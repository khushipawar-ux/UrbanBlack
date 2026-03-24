"""
config.py — Central configuration for Urban Black Route Optimization Engine
All secrets are loaded from environment variables / .env file.
"""

import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    # ── App ────────────────────────────────────────────────────────────────
    APP_NAME: str         = "Urban Black Route Optimization Engine"
    APP_VERSION: str      = "1.0.0"
    ENV: str              = os.getenv("ENV", "development")        # development | production
    DEBUG: bool           = os.getenv("DEBUG", "true").lower() == "true"
    PORT: int             = int(os.getenv("PORT", 8000))
    HOST: str             = os.getenv("HOST", "0.0.0.0")

    # ── PostgreSQL ──────────────────────────────────────────────────────────
    DB_HOST: str          = os.getenv("DB_HOST", "localhost")
    DB_PORT: int          = int(os.getenv("DB_PORT", 5432))
    DB_NAME: str          = os.getenv("DB_NAME", "urban_black_db")
    DB_USER: str          = os.getenv("DB_USER", "postgres")
    DB_PASSWORD: str      = os.getenv("DB_PASSWORD", "postgres")
    DB_POOL_MIN: int      = int(os.getenv("DB_POOL_MIN", 2))
    DB_POOL_MAX: int      = int(os.getenv("DB_POOL_MAX", 10))

    @property
    def DATABASE_URL(self) -> str:
        return (
            f"postgresql+asyncpg://{self.DB_USER}:{self.DB_PASSWORD}"
            f"@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"
        )

    @property
    def DATABASE_URL_SYNC(self) -> str:
        return (
            f"postgresql://{self.DB_USER}:{self.DB_PASSWORD}"
            f"@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"
        )

    # ── Redis ───────────────────────────────────────────────────────────────
    REDIS_HOST: str       = os.getenv("REDIS_HOST", "localhost")
    REDIS_PORT: int       = int(os.getenv("REDIS_PORT", 6379))
    REDIS_DB: int         = int(os.getenv("REDIS_DB", 0))
    REDIS_PASSWORD: str   = os.getenv("REDIS_PASSWORD", "")
    CACHE_TTL_SECONDS: int = int(os.getenv("CACHE_TTL_SECONDS", 300))   # 5 minutes

    # ── Google Maps ─────────────────────────────────────────────────────────
    GOOGLE_MAPS_API_KEY: str         = os.getenv("GOOGLE_MAPS_API_KEY", "YOUR_API_KEY_HERE")
    GOOGLE_DIRECTIONS_KEY: str       = os.getenv("GOOGLE_DIRECTIONS_KEY", "")    # separate key if needed
    GOOGLE_DISTANCE_MATRIX_KEY: str  = os.getenv("GOOGLE_DISTANCE_MATRIX_KEY", "")
    GOOGLE_MAPS_TIMEOUT: int         = int(os.getenv("GOOGLE_MAPS_TIMEOUT", 5))  # seconds

    # ── ML Model ────────────────────────────────────────────────────────────
    MODEL_PATH: str       = os.getenv("MODEL_PATH", "ml/saved_models/eta_lgbm_v1.pkl")
    MODEL_VERSION: str    = os.getenv("MODEL_VERSION", "lgbm_v1.0")
    TRAINING_DATA_PATH: str = os.getenv("TRAINING_DATA_PATH", "data/rides_training.csv")
    MIN_TRAINING_ROWS: int = int(os.getenv("MIN_TRAINING_ROWS", 100))

    # ── Route Optimization ──────────────────────────────────────────────────
    REROUTE_DEVIATION_METERS: int    = int(os.getenv("REROUTE_DEVIATION_METERS", 200))
    REROUTE_POLL_SECONDS: int        = int(os.getenv("REROUTE_POLL_SECONDS", 30))
    ML_IMPROVEMENT_THRESHOLD: float  = float(os.getenv("ML_IMPROVEMENT_THRESHOLD", 0.05))  # 5% better to suggest alt
    MAX_ROUTE_RESPONSE_MS: int       = int(os.getenv("MAX_ROUTE_RESPONSE_MS", 200))

    # ── Pune City Defaults ──────────────────────────────────────────────────
    CITY_NAME: str        = "Pune"
    CITY_LAT: float       = 18.5204
    CITY_LNG: float       = 73.8567
    CITY_TIMEZONE: str    = "Asia/Kolkata"

    # ── Peak Hours (Pune) ───────────────────────────────────────────────────
    MORNING_PEAK_START: int   = 8
    MORNING_PEAK_END: int     = 10
    EVENING_PEAK_START: int   = 18
    EVENING_PEAK_END: int     = 20


settings = Settings()
