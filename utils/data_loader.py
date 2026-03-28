import pandas as pd


def load_excel_data(driver_id):
    try:
        df = pd.read_excel("data/LIR_DB_Schema.xlsx")

        # ✅ Normalize columns
        df.columns = df.columns.str.strip().str.lower()

        # ✅ Rename to API-compatible names
        df.rename(columns={
            "driverid": "driver_id",
            "ridekm": "ride_km",
            "durationmin": "duration_min",
            "requestedat": "requested_at",
            "pickuplat": "pickup_lat",
            "pickuplng": "pickup_lng",
            "droplat": "drop_lat",
            "droplng": "drop_lng"
        }, inplace=True)

        # ✅ Required columns check
        required_cols = [
            "driver_id", "ride_km", "duration_min",
            "pickup_lat", "pickup_lng", "drop_lat", "drop_lng"
        ]

        for col in required_cols:
            if col not in df.columns:
                print(f"❌ Missing column: {col}")
                return pd.DataFrame()

        # ✅ Filter driver
        df = df[df["driver_id"].astype(str) == str(driver_id)]

        if df.empty:
            return pd.DataFrame()

        # ✅ Handle datetime
        if "requested_at" in df.columns:
            df["requested_at"] = pd.to_datetime(df["requested_at"], errors="coerce")
        else:
            df["requested_at"] = pd.Timestamp.now()

        # ✅ Add shift_end
        df["shift_end"] = pd.Timestamp.now() + pd.Timedelta(hours=5)

        return df

    except Exception as e:
        print("❌ Excel Load Error:", e)
        return pd.DataFrame()