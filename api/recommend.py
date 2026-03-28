from fastapi import APIRouter
import pandas as pd
import joblib
import googlemaps
import os

from utils.db import fetch_data
from config.settings import (
    GOOGLE_MAPS_API_KEY,
    MODEL_PATH,
    AVG_SPEED,
    DEPOT_LAT,
    DEPOT_LNG,
    SHIFT_THRESHOLD,
    MID_SHIFT_THRESHOLD
)

from utils.geo import haversine, estimate_time
from utils.direction import direction_score

router = APIRouter()

model = joblib.load(MODEL_PATH)
gmaps = googlemaps.Client(key=GOOGLE_MAPS_API_KEY)


@router.get("/")
def home():
    return {"message": "API Running 🚀"}


@router.get("/recommend/{driver_id}")
def recommend(driver_id: str):

    try:
        # ================= 1. FETCH FROM DATABASE =================
        query = """
        SELECT 
            r.id AS ride_id,
            r.driver_id,
            r.ride_km,
            r.duration_min,
            r.fare,
            r.requested_at,
            ds.shift_end,
            r.pickup_lat,
            r.pickup_lng,
            r.drop_lat,
            r.drop_lng
        FROM rides r
        LEFT JOIN driver_shifts ds ON r.driver_id = ds.driver_id
        WHERE r.driver_id = %s
        LIMIT 100
        """

        df = fetch_data(query, (driver_id,))

        # ================= 2. FALLBACK TO EXCEL =================
        if df.empty:
            try:
                path = os.path.join("data", "PMPML_LIR_ML_Dataset.xlsx")
                df = pd.read_excel(path, sheet_name="rides")

                df.rename(columns={
                    "id": "ride_id",
                    "rideKm": "ride_km",
                    "durationMin": "duration_min",
                    "requestedAt": "requested_at",
                    "pickupLat": "pickup_lat",
                    "pickupLng": "pickup_lng",
                    "dropLat": "drop_lat",
                    "dropLng": "drop_lng",
                    "driverId": "driver_id"
                }, inplace=True)

                df["driver_id"] = df["driver_id"].astype(str).str.strip()
                driver_id = str(driver_id).strip()

                df = df[df["driver_id"] == driver_id]

                if df.empty:
                    return {"message": "Driver not found in dataset"}

                # fallback shift
                df["shift_end"] = pd.Timestamp.now() + pd.Timedelta(hours=6)

            except Exception as e:
                return {"error": f"Data loading failed: {str(e)}"}

        # ================= 3. CLEANING =================
        df["requested_at"] = pd.to_datetime(df["requested_at"], errors="coerce")
        df["shift_end"] = pd.to_datetime(df["shift_end"], errors="coerce")

        df["hour"] = df["requested_at"].dt.hour.fillna(0)

        df["remaining_time"] = (
            (df["shift_end"] - df["requested_at"]).dt.total_seconds() / 60
        ).fillna(0)

        df["distance_proxy"] = df["ride_km"].fillna(0)

        # ================= 🔥 FEATURE ENGINEERING =================
        df["speed"] = df["ride_km"] / (df["duration_min"].replace(0, 1))
        df["fare_per_km"] = df["fare"] / (df["ride_km"].replace(0, 1))

        # ================= 4. MODEL =================
        features = [
            "ride_km",
            "duration_min",
            "fare",
            "hour",
            "remaining_time",
            "distance_proxy",
            "speed",
            "fare_per_km"
        ]

        df = df.dropna(subset=features)

        if df.empty:
            return {"message": "Insufficient data for prediction"}

        df["model_score"] = model.predict(df[features])

        # ================= 5. DISTANCE =================
        df["distance_to_depot"] = df.apply(
            lambda r: haversine(
                r["drop_lat"],
                r["drop_lng"],
                DEPOT_LAT,
                DEPOT_LNG
            ),
            axis=1
        )

        # ================= 6. FEASIBILITY =================
        def check(row):
            ride_time = estimate_time(row["ride_km"], AVG_SPEED)
            return_time = estimate_time(row["distance_to_depot"], AVG_SPEED)
            total_time = ride_time + return_time
            return total_time <= row["remaining_time"], total_time

        temp = df.apply(check, axis=1)

        df["is_feasible"] = [x[0] for x in temp]
        df["total_time"] = [x[1] for x in temp]

        df = df[df["is_feasible"]]

        if df.empty:
            return {"message": "No feasible rides"}

        # ================= 7. SCORES =================
        df["return_prob"] = df.apply(
            lambda r: max(0.1, 1 - (r["total_time"] / max(r["remaining_time"], 1))),
            axis=1
        )

        df["direction_score"] = df.apply(
            lambda r: direction_score(
                r["pickup_lat"], r["pickup_lng"],
                r["drop_lat"], r["drop_lng"],
                DEPOT_LAT, DEPOT_LNG
            ),
            axis=1
        )

        df["proximity_score"] = 1 / (1 + df["distance_to_depot"])

        # ================= 8. FINAL SCORE =================
        def final(r):
            if r["remaining_time"] <= SHIFT_THRESHOLD:
                return 0.5*r["model_score"] + 0.3*r["return_prob"] + 0.2*r["direction_score"]
            elif r["remaining_time"] <= MID_SHIFT_THRESHOLD:
                return 0.7*r["model_score"] + 0.2*r["return_prob"] + 0.1*r["direction_score"]
            else:
                return r["model_score"]

        df["final_score"] = df.apply(final, axis=1)

        df = df.sort_values(by="final_score", ascending=False)

        # ================= 🔥 GOOGLE MAPS (USED) =================
        results = []

        for _, r in df.head(5).iterrows():
            try:
                origin = f"{r['pickup_lat']},{r['pickup_lng']}"
                destination = f"{r['drop_lat']},{r['drop_lng']}"

                directions = gmaps.directions(origin, destination)

                if directions:
                    leg = directions[0]["legs"][0]
                    distance = leg["distance"]["text"]
                    duration = leg["duration"]["text"]
                else:
                    distance, duration = None, None

            except Exception:
                distance, duration = None, None

            results.append({
                "ride_id": str(r["ride_id"]),
                "driver_id": str(r["driver_id"]),
                "final_score": round(float(r["final_score"]), 4),
                "model_score": round(float(r["model_score"]), 4),
                "efficiency": round(float(r["model_score"]), 4),
                "fare": float(r["fare"]),
                "ride_km": float(r["ride_km"]),
                "remaining_time": round(float(r["remaining_time"]), 2),
                "estimated_distance": distance,
                "estimated_time": duration,
                "explanation": f"High efficiency ({r['model_score']:.2f}), fare/km ({r['fare_per_km']:.2f}), feasible return"
            })

        return results

    except Exception as e:
        return {"error": str(e)}