import joblib
import pandas as pd
import os
from src.features.feature_engineering import create_features

model = None
scaler = None

if os.path.exists("model/anomaly_model.pkl"):
    model = joblib.load("model/anomaly_model.pkl")

if os.path.exists("model/scaler.pkl"):
    scaler = joblib.load("model/scaler.pkl")


def predict_with_score(data):
    if model is None or scaler is None:
        return 1, 0.0

    df = pd.DataFrame([data])
    features = create_features(df)

    scaled = scaler.transform(features)

    prediction = model.predict(scaled)[0]
    score = model.decision_function(scaled)[0]

    return prediction, score