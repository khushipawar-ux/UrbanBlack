"""
ml/feature_engineering.py — Extract and transform features for the ETA model.
Every prediction goes through this file so features stay consistent
between training and inference.
"""

import pandas as pd
import numpy as np
from datetime import datetime, timezone, timedelta
from typing import Dict, Any, Optional

# IST = UTC+5:30
IST_OFFSET = timezone(timedelta(hours=5, minutes=30))

# ─────────────────────────────────────────────────────────────────────────────
# FEATURE COLUMNS (must match training CSV exactly)
# ─────────────────────────────────────────────────────────────────────────────

FEATURE_COLUMNS = [
    "distance_km",
    "google_eta_min",
    "hour_of_day",
    "day_of_week",
    "is_weekend",
    "is_peak_hour",
    "is_holiday",
    "traffic_congestion_score",
    "rainfall_mm",
    "total_turns",
    "total_signals",
    "is_raining",
    # Derived
    "hour_sin",
    "hour_cos",
    "dow_sin",
    "dow_cos",
    "distance_x_traffic",
    "peak_x_distance",
    "rain_x_traffic",
]

TARGET_COLUMN = "actual_duration_min"


def encode_cyclical(value: float, max_val: float):
    """Encode cyclical features (hour, day_of_week) as sin/cos pairs."""
    sin_val = np.sin(2 * np.pi * value / max_val)
    cos_val = np.cos(2 * np.pi * value / max_val)
    return round(float(sin_val), 6), round(float(cos_val), 6)


def weather_to_rainfall(condition: str, rainfall_mm: Optional[float] = None) -> float:
    """Convert weather condition string to numeric rainfall_mm if not provided."""
    if rainfall_mm is not None:
        return float(rainfall_mm)
    defaults = {
        "clear":  0.0,
        "cloudy": 0.0,
        "rainy":  5.0,
        "stormy": 25.0,
    }
    return defaults.get(condition.lower(), 0.0)


def is_peak_hour(hour: int, is_weekend: bool, is_holiday: bool = False) -> bool:
    """Return True if this is a Pune peak hour."""
    if is_weekend or is_holiday:
        return False
    return (8 <= hour <= 10) or (18 <= hour <= 20)


def get_traffic_condition_label(congestion_score: float) -> str:
    """Map numeric congestion score to human-readable label."""
    if congestion_score < 1.3:
        return "free"
    elif congestion_score < 1.6:
        return "normal"
    elif congestion_score < 1.85:
        return "heavy"
    else:
        return "severe"


def extract_features(
    distance_km: float,
    google_eta_sec: int,
    dt: Optional[datetime] = None,
    hour_of_day: Optional[int] = None,
    day_of_week: Optional[int] = None,
    is_holiday: bool = False,
    weather_condition: str = "clear",
    rainfall_mm: Optional[float] = None,
    traffic_congestion_score: float = 2.0,
    total_turns: int = 5,
    total_signals: int = 3,
) -> Dict[str, Any]:
    """
    Build the feature dict used for a single ETA prediction.

    Either pass `dt` (a datetime) OR (hour_of_day + day_of_week) explicitly.
    """
    if dt is not None:
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=IST_OFFSET)
        hour  = dt.hour
        dow   = dt.weekday()     # 0=Mon … 6=Sun
    elif hour_of_day is not None and day_of_week is not None:
        hour = hour_of_day
        dow  = day_of_week
    else:
        now   = datetime.now(IST_OFFSET)
        hour  = now.hour
        dow   = now.weekday()

    is_wknd   = dow in (5, 6)
    is_peak   = is_peak_hour(hour, is_wknd, is_holiday)
    rain_mm   = weather_to_rainfall(weather_condition, rainfall_mm)
    google_min = round(google_eta_sec / 60.0, 3)

    hour_sin, hour_cos = encode_cyclical(hour, 24)
    dow_sin,  dow_cos  = encode_cyclical(dow,  7)

    features = {
        # Raw
        "distance_km":                round(distance_km, 3),
        "google_eta_min":             google_min,
        "hour_of_day":                hour,
        "day_of_week":                dow,
        "is_weekend":                 int(is_wknd),
        "is_peak_hour":               int(is_peak),
        "is_holiday":                 int(is_holiday),
        "traffic_congestion_score":   round(float(traffic_congestion_score), 3),
        "rainfall_mm":                round(float(rain_mm), 3),
        "total_turns":                int(total_turns),
        "total_signals":              int(total_signals),
        "is_raining":                 int(rain_mm > 0),
        # Cyclical encodings
        "hour_sin":                   hour_sin,
        "hour_cos":                   hour_cos,
        "dow_sin":                    dow_sin,
        "dow_cos":                    dow_cos,
        # Interaction features
        "distance_x_traffic":         round(distance_km * traffic_congestion_score, 4),
        "peak_x_distance":            round(int(is_peak) * distance_km, 4),
        "rain_x_traffic":             round(rain_mm * traffic_congestion_score, 4),
    }
    return features


def features_to_dataframe(features: Dict[str, Any]) -> pd.DataFrame:
    """Wrap a single feature dict into a one-row DataFrame for model.predict()."""
    df = pd.DataFrame([features])
    # Keep only columns the model was trained on, in the right order
    available = [c for c in FEATURE_COLUMNS if c in df.columns]
    return df[available]


def prepare_training_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """
    Full preprocessing pipeline for the rides_training.csv dataset.
    Call this before fitting the LightGBM model.
    """
    df = df.copy()

    # Drop rows with missing target
    df = df.dropna(subset=[TARGET_COLUMN])

    # Drop GPS anomalies if column exists
    if "has_gps_anomaly" in df.columns:
        df = df[df["has_gps_anomaly"] == 0]

    # Drop rides < 1km (noise)
    df = df[df["distance_km"] >= 1.0]

    # Drop outlier ETAs (>180 min for a city ride = likely data error)
    df = df[df[TARGET_COLUMN] <= 180]
    df = df[df[TARGET_COLUMN] >= 2]

    # Ensure google_eta_min column
    if "google_eta_min" not in df.columns and "google_eta_seconds" in df.columns:
        df["google_eta_min"] = df["google_eta_seconds"] / 60.0

    # Encode cyclical features
    df[["hour_sin", "hour_cos"]] = df["hour_of_day"].apply(
        lambda h: pd.Series(encode_cyclical(h, 24))
    )
    df[["dow_sin", "dow_cos"]] = df["day_of_week"].apply(
        lambda d: pd.Series(encode_cyclical(d, 7))
    )

    # Fill missing optional columns with defaults
    if "total_turns"   not in df.columns: df["total_turns"]   = 5
    if "total_signals" not in df.columns: df["total_signals"] = 3
    if "is_holiday"    not in df.columns: df["is_holiday"]    = 0
    if "rainfall_mm"   not in df.columns: df["rainfall_mm"]   = 0.0
    if "is_raining"    not in df.columns:
        df["is_raining"] = (df["rainfall_mm"] > 0).astype(int)
    if "traffic_congestion_score" not in df.columns:
        df["traffic_congestion_score"] = 2.0

    # Interaction features
    df["distance_x_traffic"] = df["distance_km"] * df["traffic_congestion_score"]
    df["peak_x_distance"]    = df["is_peak_hour"].astype(int) * df["distance_km"]
    df["rain_x_traffic"]     = df["rainfall_mm"] * df["traffic_congestion_score"]

    # Keep only known feature columns + target
    keep_cols = [c for c in FEATURE_COLUMNS if c in df.columns] + [TARGET_COLUMN]
    df = df[keep_cols]

    return df.reset_index(drop=True)
