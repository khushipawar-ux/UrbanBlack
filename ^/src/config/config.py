import os
from dotenv import load_dotenv

load_dotenv()

DB_CONFIG = {
    "dbname": os.getenv("DB_NAME"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "host": os.getenv("DB_HOST"),
    "port": os.getenv("DB_PORT"),
}

MODEL_PATH = "model/anomaly_model.pkl"

SPEED_THRESHOLD = 100
STOP_THRESHOLD = 5
DETOUR_DISTANCE_THRESHOLD = 0.5