import os
from dotenv import load_dotenv

load_dotenv()

# ================= DATABASE =================
DB_CONFIG = {
    "host": os.getenv("DB_HOST"),
    "port": os.getenv("DB_PORT"),
    "dbname": os.getenv("DB_NAME"),
    "user": os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
}

# ================= GOOGLE MAPS =================
GOOGLE_MAPS_API_KEY = os.getenv("GOOGLE_MAPS_API_KEY")

# ================= MODEL =================
MODEL_PATH = os.getenv("MODEL_PATH", "models/xgb_model.pkl")

# APP CONFIG
AVG_SPEED = int(os.getenv("AVG_SPEED", 30))

# DEPOT LOCATION (IMPORTANT)
DEPOT_LAT = 18.5204
DEPOT_LNG = 73.8567

# ================= SHIFT LOGIC =================
SHIFT_THRESHOLD = 120   # 2 hours (strict return mode)
MID_SHIFT_THRESHOLD = 300  # 5 hours