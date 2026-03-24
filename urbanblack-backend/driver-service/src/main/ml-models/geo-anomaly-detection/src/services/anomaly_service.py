from src.models.predict_model import predict_with_score
from src.utils.geo_utils import deviation_from_route
from src.config.config import (
    SPEED_THRESHOLD,
    DETOUR_DISTANCE_THRESHOLD,
    STOP_THRESHOLD
)
from src.db.db_queries import save_anomaly
from src.utils.logger import logger


def detect_anomaly(data, route_points=None):

    # ✅ Input validation
    required_fields = ["lat", "lng", "speed"]
    for field in required_fields:
        if field not in data:
            raise ValueError(f"{field} is required")

    # 🤖 ML prediction
    prediction, score = predict_with_score(data)

    # 🎯 Decision logic (ALL INSIDE FUNCTION)
    if data["speed"] > SPEED_THRESHOLD:
        anomaly = "SPEED"
        severity = "HIGH"

    elif data["speed"] < STOP_THRESHOLD:
        anomaly = "STOP"
        severity = "MEDIUM"

    elif route_points:
        deviation = deviation_from_route(
            (data["lat"], data["lng"]),
            route_points
        )
        if deviation > DETOUR_DISTANCE_THRESHOLD:
            anomaly = "DETOUR"
            severity = "HIGH"
        else:
            anomaly = "NORMAL"
            severity = "LOW"

    elif prediction == -1:
        anomaly = "ML_ANOMALY"
        severity = "MEDIUM"

    else:
        anomaly = "NORMAL"
        severity = "LOW"

    result = {
        "anomaly": anomaly,
        "severity": severity,
        "score": float(score)
    }

    # 💾 Save to DB
    try:
        save_anomaly(result)
        logger.info(f"Anomaly saved: {result}")
    except Exception as e:
        logger.error(f"DB error: {e}")

    return result