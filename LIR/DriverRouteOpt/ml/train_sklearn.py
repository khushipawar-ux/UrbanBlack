"""
ml/train_sklearn.py — Fallback ETA trainer using scikit-learn GradientBoosting.

Use this if LightGBM is not installed.
For production, use ml/train.py (LightGBM) which gives better accuracy.

Usage:
    python ml/train_sklearn.py --data data/rides_training.csv --output ml/saved_models/
"""

import os
import sys
import argparse
import joblib
import json
import numpy as np
import pandas as pd
from datetime import datetime
from pathlib import Path

from sklearn.ensemble import GradientBoostingRegressor, RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

sys.path.append(str(Path(__file__).resolve().parent.parent))
from ml.feature_engineering import (
    prepare_training_dataframe,
    FEATURE_COLUMNS,
    TARGET_COLUMN,
)


def mape(y_true, y_pred):
    y_true, y_pred = np.array(y_true), np.array(y_pred)
    mask = y_true != 0
    return float(np.mean(np.abs((y_true[mask] - y_pred[mask]) / y_true[mask])) * 100)


def train(data_path: str, output_dir: str, version_tag: str = None):
    print("=" * 55)
    print("  Urban Black — ETA Model Training (sklearn fallback)")
    print("=" * 55)

    # ── Load + prepare ────────────────────────────────────────────────────
    df_raw = pd.read_csv(data_path)
    print(f"\nRaw data    : {len(df_raw)} rows")

    df = prepare_training_dataframe(df_raw)
    feature_cols = [c for c in FEATURE_COLUMNS if c in df.columns]
    X = df[feature_cols].values
    y = df[TARGET_COLUMN].values
    print(f"After clean : {len(df)} rows  |  {len(feature_cols)} features")
    print(f"Target      : min={y.min():.1f}  max={y.max():.1f}  mean={y.mean():.1f} min")

    # ── Split ─────────────────────────────────────────────────────────────
    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=0.20, random_state=42
    )
    print(f"\nTrain: {len(X_train)}  |  Val: {len(X_val)}")

    # ── Model: GradientBoostingRegressor ─────────────────────────────────
    print("\nFitting GradientBoostingRegressor ...")
    model = GradientBoostingRegressor(
        n_estimators=500,
        learning_rate=0.05,
        max_depth=5,
        min_samples_leaf=10,
        subsample=0.8,
        random_state=42,
        verbose=0,
    )
    model.fit(X_train, y_train)

    # ── Evaluate ─────────────────────────────────────────────────────────
    y_pred = np.maximum(model.predict(X_val), 1.0)
    mae_v   = round(mean_absolute_error(y_val, y_pred), 3)
    rmse_v  = round(np.sqrt(mean_squared_error(y_val, y_pred)), 3)
    mape_v  = round(mape(y_val, y_pred), 3)
    r2_v    = round(r2_score(y_val, y_pred), 4)
    w10     = round(np.mean(np.abs(y_val - y_pred) / y_val <= 0.10) * 100, 2)

    print("\n" + "=" * 55)
    print("  MODEL EVALUATION RESULTS")
    print("=" * 55)
    print(f"  MAE         : {mae_v} minutes")
    print(f"  RMSE        : {rmse_v} minutes")
    print(f"  MAPE        : {mape_v} %")
    print(f"  R²          : {r2_v}")
    print(f"  Within 10%  : {w10} % of predictions")
    if mape_v <= 15.0:
        print("  ✓ Model trained successfully")
    else:
        print("  ⚠  MAPE high — use LightGBM trainer for better accuracy")
    print("=" * 55)

    # ── Feature importance ────────────────────────────────────────────────
    importance = pd.DataFrame({
        "feature":    feature_cols,
        "importance": model.feature_importances_,
    }).sort_values("importance", ascending=False)
    print("\nTop 10 Feature Importances:")
    print(importance.head(10).to_string(index=False))

    # ── Save ─────────────────────────────────────────────────────────────
    os.makedirs(output_dir, exist_ok=True)
    tag         = version_tag or f"sklearn_v{datetime.now().strftime('%Y%m%d_%H%M')}"
    model_path  = os.path.join(output_dir, f"eta_{tag}.pkl")
    latest_path = os.path.join(output_dir, "eta_lgbm_latest.pkl")
    meta_path   = os.path.join(output_dir, f"eta_{tag}_metadata.json")
    imp_path    = os.path.join(output_dir, f"eta_{tag}_importance.csv")

    artifact = {"model": model, "feature_columns": feature_cols, "backend": "sklearn"}
    joblib.dump(artifact, model_path)
    joblib.dump(artifact, latest_path)

    metadata = {
        "version_tag":     tag,
        "algorithm":       "GradientBoostingRegressor (sklearn)",
        "trained_at":      datetime.now().isoformat(),
        "training_rows":   int(len(X_train)),
        "validation_rows": int(len(X_val)),
        "features":        feature_cols,
        "target":          TARGET_COLUMN,
        "n_estimators":    500,
        "metrics": {
            "mae_minutes":      mae_v,
            "rmse_minutes":     rmse_v,
            "mape_pct":         mape_v,
            "r2_score":         r2_v,
            "within_10pct_pct": w10,
        },
        "note": "Use ml/train.py (LightGBM) for production — better accuracy",
    }
    with open(meta_path, "w") as f:
        json.dump(metadata, f, indent=2)
    importance.to_csv(imp_path, index=False)

    print(f"\n  Model saved  → {model_path}")
    print(f"  Latest link  → {latest_path}")
    print(f"  Metadata     → {meta_path}")
    print(f"  Importance   → {imp_path}")
    return model, metadata, feature_cols


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train Urban Black ETA Model (sklearn fallback)")
    parser.add_argument("--data",    default="data/rides_training.csv")
    parser.add_argument("--output",  default="ml/saved_models/")
    parser.add_argument("--version", default=None)
    args = parser.parse_args()
    train(args.data, args.output, args.version)
