"""
ml/predictor.py — Load the trained LightGBM model and serve ETA predictions.

This is a singleton — the model is loaded once at startup and reused for all
requests. Loading from disk is ~100ms; inference is ~1ms.
"""

import os
import joblib
import numpy as np
import structlog
from typing import Optional, Dict, Any, Tuple
from pathlib import Path

from ml.feature_engineering import (
    extract_features,
    features_to_dataframe,
    get_traffic_condition_label,
)
from config import settings

log = structlog.get_logger(__name__)


class ETAPredictor:
    """
    Singleton wrapper around the trained LightGBM model.

    Usage:
        predictor = ETAPredictor()
        predictor.load()
        result = predictor.predict(distance_km=10.5, google_eta_sec=1800, ...)
    """

    def __init__(self):
        self.model           = None
        self.feature_columns = []
        self.model_version   = settings.MODEL_VERSION
        self.is_loaded       = False

    # ─────────────────────────────────────────────────────────────────────
    # LOADING
    # ─────────────────────────────────────────────────────────────────────

    def load(self, model_path: Optional[str] = None) -> bool:
        """Load model from disk. Returns True on success."""
        path = model_path or settings.MODEL_PATH

        # Try the specified path first, then fallback to "latest"
        candidates = [
            path,
            "ml/saved_models/eta_lgbm_latest.pkl",
            "ml/saved_models/eta_lgbm_v1.pkl",
        ]

        for candidate in candidates:
            if os.path.exists(candidate):
                try:
                    artifact = joblib.load(candidate)
                    self.model           = artifact["model"]
                    self.feature_columns = artifact["feature_columns"]
                    self.is_loaded       = True
                    log.info("ETA model loaded",
                             path=candidate,
                             features=len(self.feature_columns),
                             best_iteration=getattr(self.model, "best_iteration", "N/A"))
                    return True
                except Exception as e:
                    log.warning("Failed to load model from path",
                                path=candidate, error=str(e))

        log.warning("No model file found — predictions will use Google ETA only")
        self.is_loaded = False
        return False

    # ─────────────────────────────────────────────────────────────────────
    # PREDICTION
    # ─────────────────────────────────────────────────────────────────────

    def predict(
        self,
        distance_km: float,
        google_eta_sec: int,
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
        Predict ETA in minutes for a given trip.

        Returns a dict with:
          - ml_eta_min        : ML-corrected ETA (main output)
          - google_eta_min    : Raw Google ETA
          - eta_correction_min: Difference (positive = ML thinks it'll take longer)
          - confidence_score  : Rough model confidence (0.0–1.0)
          - traffic_condition : human-readable label
          - is_peak_hour      : bool
          - features          : the feature dict used
        """
        google_eta_min = round(google_eta_sec / 60.0, 2)

        # ── Extract features ──────────────────────────────────────────────
        features = extract_features(
            distance_km=distance_km,
            google_eta_sec=google_eta_sec,
            hour_of_day=hour_of_day,
            day_of_week=day_of_week,
            is_holiday=is_holiday,
            weather_condition=weather_condition,
            rainfall_mm=rainfall_mm,
            traffic_congestion_score=traffic_congestion_score,
            total_turns=total_turns,
            total_signals=total_signals,
        )

        # ── Inference ─────────────────────────────────────────────────────
        if self.is_loaded and self.model is not None:
            try:
                X = features_to_dataframe(features)
                # Align to training columns
                for col in self.feature_columns:
                    if col not in X.columns:
                        X[col] = 0
                X = X[self.feature_columns]

                # Support both LightGBM and sklearn backends
                lgbm_iter = getattr(self.model, "best_iteration_", None)
                if lgbm_iter is not None:
                    raw_pred = self.model.predict(X, num_iteration=lgbm_iter)[0]
                else:
                    raw_pred = self.model.predict(X)[0]

                ml_eta_min = round(max(float(raw_pred), 1.0), 2)

                # Rough confidence: higher when prediction is close to Google
                deviation = abs(ml_eta_min - google_eta_min) / max(google_eta_min, 1)
                confidence = round(max(0.5, 1.0 - deviation * 0.5), 3)

            except Exception as e:
                log.warning("Model inference failed, falling back to Google ETA",
                            error=str(e))
                ml_eta_min = google_eta_min
                confidence = 0.5
        else:
            # No model loaded — return Google ETA with a heuristic correction
            ml_eta_min = self._heuristic_correction(
                google_eta_min,
                is_peak=bool(features.get("is_peak_hour")),
                weather=weather_condition,
                rainfall=float(features.get("rainfall_mm", 0)),
            )
            confidence = 0.6

        correction = round(ml_eta_min - google_eta_min, 2)

        return {
            "ml_eta_min":          ml_eta_min,
            "google_eta_min":      google_eta_min,
            "eta_correction_min":  correction,
            "confidence_score":    confidence,
            "traffic_condition":   get_traffic_condition_label(traffic_congestion_score),
            "is_peak_hour":        bool(features.get("is_peak_hour", False)),
            "model_version":       self.model_version if self.is_loaded else "heuristic",
            "features":            features,
        }

    # ─────────────────────────────────────────────────────────────────────
    # HEURISTIC FALLBACK (when model not loaded)
    # ─────────────────────────────────────────────────────────────────────

    def _heuristic_correction(
        self,
        google_eta_min: float,
        is_peak: bool,
        weather: str,
        rainfall: float,
    ) -> float:
        """Simple rule-based correction when model is unavailable."""
        correction = 0.0
        if is_peak:
            correction += google_eta_min * 0.15   # 15% more in peak hour
        if weather == "rainy":
            correction += google_eta_min * 0.20
        elif weather == "stormy":
            correction += google_eta_min * 0.40
        return round(max(google_eta_min + correction, 1.0), 2)

    # ─────────────────────────────────────────────────────────────────────
    # RELOAD (for hot-swap without restart)
    # ─────────────────────────────────────────────────────────────────────

    def reload(self, model_path: Optional[str] = None) -> bool:
        """Hot-reload model without restarting the server."""
        log.info("Reloading ETA model...")
        old_model   = self.model
        old_version = self.model_version
        success = self.load(model_path)
        if not success:
            self.model         = old_model
            self.model_version = old_version
            log.warning("Model reload failed, kept old model")
        return success


# Global singleton — imported and used by route_optimizer
_predictor: Optional[ETAPredictor] = None


def get_predictor() -> ETAPredictor:
    """Return the global ETAPredictor instance, creating it if needed."""
    global _predictor
    if _predictor is None:
        _predictor = ETAPredictor()
        _predictor.load()
    return _predictor
