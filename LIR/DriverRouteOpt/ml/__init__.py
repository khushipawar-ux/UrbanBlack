"""
ml/ — Machine Learning package for Urban Black Route Optimization Engine.

Exposes:
  - ETAPredictor     : load model + run inference
  - get_predictor()  : return global singleton predictor
  - extract_features : build feature dict for a single prediction
  - prepare_training_dataframe : full preprocessing for rides_training.csv
  - FEATURE_COLUMNS  : ordered list of feature names (train == infer)
  - TARGET_COLUMN    : 'actual_duration_min'
"""

from ml.predictor import ETAPredictor, get_predictor
from ml.feature_engineering import (
    extract_features,
    features_to_dataframe,
    prepare_training_dataframe,
    is_peak_hour,
    get_traffic_condition_label,
    FEATURE_COLUMNS,
    TARGET_COLUMN,
)

__all__ = [
    "ETAPredictor",
    "get_predictor",
    "extract_features",
    "features_to_dataframe",
    "prepare_training_dataframe",
    "is_peak_hour",
    "get_traffic_condition_label",
    "FEATURE_COLUMNS",
    "TARGET_COLUMN",
]
