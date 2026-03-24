"""
ml/train.py — Train, evaluate, and save the LightGBM ETA prediction model.

Usage:
    python ml/train.py --data data/rides_training.csv --output ml/saved_models/

The trained model is saved as a .pkl file and can be loaded by predictor.py.
"""

import os
import sys
import argparse
import joblib
import json
import numpy as np
import pandas as pd
import structlog
from datetime import datetime
from pathlib import Path

from sklearn.model_selection import train_test_split, KFold
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.preprocessing import LabelEncoder

import lightgbm as lgb

# Add project root to path
sys.path.append(str(Path(__file__).resolve().parent.parent))
from ml.feature_engineering import (
    prepare_training_dataframe,
    FEATURE_COLUMNS,
    TARGET_COLUMN,
)

log = structlog.get_logger(__name__)


# ─────────────────────────────────────────────────────────────────────────────
# LIGHTGBM HYPERPARAMETERS
# ─────────────────────────────────────────────────────────────────────────────

LGBM_PARAMS = {
    "objective":        "regression",
    "metric":           ["mae", "mse"],
    "boosting_type":    "gbdt",
    "num_leaves":       63,
    "max_depth":        -1,
    "learning_rate":    0.05,
    "n_estimators":     1000,
    "min_child_samples": 20,
    "subsample":        0.8,
    "subsample_freq":   1,
    "colsample_bytree": 0.8,
    "reg_alpha":        0.1,
    "reg_lambda":       0.1,
    "random_state":     42,
    "n_jobs":           -1,
    "verbose":          -1,
}

EARLY_STOPPING_ROUNDS = 50


# ─────────────────────────────────────────────────────────────────────────────
# EVALUATION HELPERS
# ─────────────────────────────────────────────────────────────────────────────

def mean_absolute_percentage_error(y_true, y_pred):
    y_true = np.array(y_true)
    y_pred = np.array(y_pred)
    mask = y_true != 0
    return np.mean(np.abs((y_true[mask] - y_pred[mask]) / y_true[mask])) * 100


def evaluate_model(model, X_test, y_test):
    y_pred = model.predict(X_test, num_iteration=model.best_iteration_)
    y_pred = np.maximum(y_pred, 1.0)   # ETA can't be negative

    mae   = round(mean_absolute_error(y_test, y_pred), 4)
    rmse  = round(np.sqrt(mean_squared_error(y_test, y_pred)), 4)
    mape  = round(mean_absolute_percentage_error(y_test, y_pred), 4)
    r2    = round(r2_score(y_test, y_pred), 4)
    within_10pct = round(np.mean(np.abs(y_test - y_pred) / y_test <= 0.10) * 100, 2)

    metrics = {
        "mae_minutes":      mae,
        "rmse_minutes":     rmse,
        "mape_pct":         mape,
        "r2_score":         r2,
        "within_10pct_pct": within_10pct,
        "test_samples":     len(y_test),
    }
    return metrics, y_pred


# ─────────────────────────────────────────────────────────────────────────────
# TRAINING PIPELINE
# ─────────────────────────────────────────────────────────────────────────────

def train(data_path: str, output_dir: str, version_tag: str = None):
    """
    Full training pipeline:
      1. Load CSV
      2. Feature engineering
      3. Train / val split
      4. Fit LightGBM with early stopping
      5. Evaluate
      6. Save model + metadata
    """
    log.info("Starting training", data_path=data_path)

    # ── 1. Load data ──────────────────────────────────────────────────────
    df_raw = pd.read_csv(data_path)
    log.info("Raw data loaded", rows=len(df_raw), cols=list(df_raw.columns))

    # ── 2. Feature engineering ────────────────────────────────────────────
    df = prepare_training_dataframe(df_raw)
    log.info("Preprocessed data", rows=len(df), features=FEATURE_COLUMNS)

    feature_cols = [c for c in FEATURE_COLUMNS if c in df.columns]
    X = df[feature_cols]
    y = df[TARGET_COLUMN]

    log.info("Feature matrix ready",
             feature_count=len(feature_cols),
             target_min=round(y.min(), 2),
             target_max=round(y.max(), 2),
             target_mean=round(y.mean(), 2))

    # ── 3. Train / validation split ───────────────────────────────────────
    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=0.20, random_state=42
    )
    log.info("Data split", train=len(X_train), val=len(X_val))

    # ── 4. Build LightGBM datasets ────────────────────────────────────────
    dtrain = lgb.Dataset(X_train, label=y_train, feature_name=feature_cols)
    dval   = lgb.Dataset(X_val,   label=y_val,   reference=dtrain, free_raw_data=False)

    # ── 5. Train with early stopping ──────────────────────────────────────
    callbacks = [
        lgb.early_stopping(stopping_rounds=EARLY_STOPPING_ROUNDS, verbose=True),
        lgb.log_evaluation(period=100),
    ]

    model = lgb.train(
        params=LGBM_PARAMS,
        train_set=dtrain,
        valid_sets=[dtrain, dval],
        valid_names=["train", "val"],
        callbacks=callbacks,
    )

    log.info("Training complete", best_iteration=model.best_iteration)

    # ── 6. Evaluate ───────────────────────────────────────────────────────
    metrics, y_pred = evaluate_model(model, X_val, y_val)

    print("\n" + "="*55)
    print("  MODEL EVALUATION RESULTS")
    print("="*55)
    print(f"  MAE         : {metrics['mae_minutes']} minutes")
    print(f"  RMSE        : {metrics['rmse_minutes']} minutes")
    print(f"  MAPE        : {metrics['mape_pct']} %")
    print(f"  R²          : {metrics['r2_score']}")
    print(f"  Within 10%  : {metrics['within_10pct_pct']} % of predictions")
    print(f"  Test samples: {metrics['test_samples']}")
    print("="*55)

    if metrics["mape_pct"] <= 10.0:
        print("  ✓ TARGET MET: MAPE ≤ 10% (requirement achieved)")
    else:
        print("  ✗ MAPE > 10% — need more data or tuning")

    # ── 7. Feature importance ─────────────────────────────────────────────
    importance = pd.DataFrame({
        "feature":    model.feature_name(),
        "importance": model.feature_importance(importance_type="gain"),
    }).sort_values("importance", ascending=False)

    print("\nTop 10 Feature Importances:")
    print(importance.head(10).to_string(index=False))

    # ── 8. Save model + artifacts ─────────────────────────────────────────
    os.makedirs(output_dir, exist_ok=True)

    tag = version_tag or f"lgbm_v{datetime.now().strftime('%Y%m%d_%H%M')}"
    model_path      = os.path.join(output_dir, f"eta_{tag}.pkl")
    metadata_path   = os.path.join(output_dir, f"eta_{tag}_metadata.json")
    importance_path = os.path.join(output_dir, f"eta_{tag}_importance.csv")

    # Save using joblib (includes model + feature names)
    joblib.dump({"model": model, "feature_columns": feature_cols}, model_path)

    # Save metadata
    metadata = {
        "version_tag":      tag,
        "algorithm":        "LightGBM",
        "trained_at":       datetime.now().isoformat(),
        "training_rows":    len(X_train),
        "validation_rows":  len(X_val),
        "features":         feature_cols,
        "target":           TARGET_COLUMN,
        "best_iteration":   int(model.best_iteration),
        "hyperparameters":  LGBM_PARAMS,
        "metrics":          metrics,
        "data_path":        data_path,
    }
    with open(metadata_path, "w") as f:
        json.dump(metadata, f, indent=2)

    importance.to_csv(importance_path, index=False)

    # Also save a "latest" symlink-style copy for easy loading
    latest_path = os.path.join(output_dir, "eta_lgbm_latest.pkl")
    joblib.dump({"model": model, "feature_columns": feature_cols}, latest_path)

    log.info("Model saved",
             model_path=model_path,
             metadata_path=metadata_path,
             tag=tag)

    print(f"\n  Model saved → {model_path}")
    print(f"  Metadata   → {metadata_path}")
    print(f"  Latest     → {latest_path}")
    return model, metrics, feature_cols


# ─────────────────────────────────────────────────────────────────────────────
# CLI
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train Urban Black ETA Model")
    parser.add_argument("--data",    default="data/rides_training.csv",  help="Path to rides_training.csv")
    parser.add_argument("--output",  default="ml/saved_models/",         help="Output directory for model files")
    parser.add_argument("--version", default=None,                        help="Version tag e.g. lgbm_v1.0")
    args = parser.parse_args()

    train(args.data, args.output, args.version)
