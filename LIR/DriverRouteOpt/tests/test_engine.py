"""
tests/test_engine.py — Unit & integration tests for the Route Optimization Engine.

Run:
    pytest tests/ -v

For integration tests (needs real DB + Redis):
    pytest tests/ -v --integration
"""

import sys
import os
import pytest
import asyncio
from unittest.mock import MagicMock, patch, AsyncMock
from pathlib import Path

# Add project root
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from ml.feature_engineering import (
    extract_features,
    prepare_training_dataframe,
    is_peak_hour,
    encode_cyclical,
    FEATURE_COLUMNS,
)
from services.cache import CacheService


# ─────────────────────────────────────────────────────────────────────────────
# FEATURE ENGINEERING TESTS
# ─────────────────────────────────────────────────────────────────────────────

class TestFeatureEngineering:

    def test_peak_hour_detection_weekday_morning(self):
        assert is_peak_hour(8,  is_weekend=False) is True
        assert is_peak_hour(9,  is_weekend=False) is True
        assert is_peak_hour(10, is_weekend=False) is True

    def test_peak_hour_detection_weekday_evening(self):
        assert is_peak_hour(18, is_weekend=False) is True
        assert is_peak_hour(19, is_weekend=False) is True
        assert is_peak_hour(20, is_weekend=False) is True

    def test_peak_hour_off_peak(self):
        assert is_peak_hour(11, is_weekend=False) is False
        assert is_peak_hour(14, is_weekend=False) is False
        assert is_peak_hour(2,  is_weekend=False) is False

    def test_peak_hour_weekend_always_false(self):
        assert is_peak_hour(9,  is_weekend=True) is False
        assert is_peak_hour(18, is_weekend=True) is False

    def test_peak_hour_holiday(self):
        assert is_peak_hour(9,  is_weekend=False, is_holiday=True) is False

    def test_cyclical_encoding_hour(self):
        sin_val, cos_val = encode_cyclical(0, 24)
        assert abs(sin_val) < 1e-5    # sin(0) = 0
        assert abs(cos_val - 1.0) < 1e-5   # cos(0) = 1

        sin_12, cos_12 = encode_cyclical(12, 24)
        assert abs(sin_12) < 1e-5    # sin(pi) ≈ 0
        assert abs(cos_12 + 1.0) < 1e-5   # cos(pi) = -1

    def test_extract_features_structure(self):
        features = extract_features(
            distance_km=10.5,
            google_eta_sec=1800,
            hour_of_day=9,
            day_of_week=1,
        )
        # All required feature columns should be present
        for col in FEATURE_COLUMNS:
            assert col in features, f"Missing feature: {col}"

    def test_extract_features_peak_hour(self):
        features = extract_features(
            distance_km=5.0,
            google_eta_sec=900,
            hour_of_day=9,
            day_of_week=1,    # Monday
        )
        assert features["is_peak_hour"] == 1
        assert features["is_weekend"] == 0

    def test_extract_features_weekend(self):
        features = extract_features(
            distance_km=5.0,
            google_eta_sec=900,
            hour_of_day=9,
            day_of_week=6,    # Sunday
        )
        assert features["is_weekend"] == 1
        assert features["is_peak_hour"] == 0

    def test_extract_features_rainy_weather(self):
        features = extract_features(
            distance_km=5.0,
            google_eta_sec=900,
            hour_of_day=12,
            day_of_week=2,
            weather_condition="rainy",
            rainfall_mm=8.0,
        )
        assert features["is_raining"] == 1
        assert features["rainfall_mm"] == 8.0
        assert features["rain_x_traffic"] > 0

    def test_interaction_features(self):
        features = extract_features(
            distance_km=10.0,
            google_eta_sec=1800,
            hour_of_day=9,
            day_of_week=1,
            traffic_congestion_score=3.0,
        )
        assert features["distance_x_traffic"] == pytest.approx(30.0, rel=0.01)
        assert features["peak_x_distance"]    == pytest.approx(10.0, rel=0.01)

    def test_prepare_training_dataframe(self):
        import pandas as pd
        import numpy as np

        # Create minimal dummy dataframe
        df = pd.DataFrame({
            "ride_id":                  ["r1", "r2", "r3", "r4"],
            "distance_km":              [2.5, 0.5, 15.0, 8.0],     # 0.5km should be dropped
            "google_eta_seconds":       [900, 300, 2700, 1440],
            "hour_of_day":              [9, 10, 14, 18],
            "day_of_week":              [1, 2, 3, 1],
            "is_peak_hour":             [1, 1, 0, 1],
            "is_weekend":               [0, 0, 0, 0],
            "actual_duration_min":      [18.5, 8.0, 42.0, 35.0],
            "traffic_congestion_score": [2.5, 1.2, 1.8, 3.2],
        })

        processed = prepare_training_dataframe(df)
        # 0.5km ride should be dropped
        assert len(processed) == 3
        assert "hour_sin" in processed.columns
        assert "hour_cos" in processed.columns
        assert "distance_x_traffic" in processed.columns
        assert "actual_duration_min" in processed.columns


# ─────────────────────────────────────────────────────────────────────────────
# CACHE TESTS
# ─────────────────────────────────────────────────────────────────────────────

class TestCacheService:

    def test_route_cache_key_format(self):
        key = CacheService.make_route_key(18.5362, 73.8935, 18.5912, 73.7389)
        assert key.startswith("route:")
        assert "18.536" in key
        assert "73.894" in key

    def test_route_cache_key_rounding(self):
        """Nearby coordinates should produce the same cache key."""
        key1 = CacheService.make_route_key(18.53621, 73.89351, 18.59121, 73.73891)
        key2 = CacheService.make_route_key(18.53629, 73.89349, 18.59129, 73.73899)
        assert key1 == key2   # rounds to 3dp

    @pytest.mark.asyncio
    async def test_in_memory_cache_set_get(self):
        cache = CacheService()
        cache._use_memory = True

        await cache.set("test_key", {"value": 42})
        result = await cache.get("test_key")
        assert result == {"value": 42}

    @pytest.mark.asyncio
    async def test_in_memory_cache_miss(self):
        cache = CacheService()
        cache._use_memory = True

        result = await cache.get("nonexistent_key")
        assert result is None

    @pytest.mark.asyncio
    async def test_in_memory_cache_delete(self):
        cache = CacheService()
        cache._use_memory = True

        await cache.set("del_key", {"x": 1})
        await cache.delete("del_key")
        result = await cache.get("del_key")
        assert result is None


# ─────────────────────────────────────────────────────────────────────────────
# PREDICTOR TESTS (with mocked model)
# ─────────────────────────────────────────────────────────────────────────────

class TestETAPredictor:

    def test_heuristic_correction_peak_hour(self):
        from ml.predictor import ETAPredictor
        p = ETAPredictor()
        p.is_loaded = False

        result = p._heuristic_correction(30.0, is_peak=True, weather="clear", rainfall=0.0)
        assert result > 30.0    # should be higher in peak hour

    def test_heuristic_correction_rainy(self):
        from ml.predictor import ETAPredictor
        p = ETAPredictor()
        result = p._heuristic_correction(30.0, is_peak=False, weather="rainy", rainfall=5.0)
        assert result > 30.0

    def test_heuristic_correction_stormy(self):
        from ml.predictor import ETAPredictor
        p = ETAPredictor()
        r_rainy  = p._heuristic_correction(30.0, is_peak=False, weather="rainy",  rainfall=5.0)
        r_stormy = p._heuristic_correction(30.0, is_peak=False, weather="stormy", rainfall=25.0)
        assert r_stormy > r_rainy   # stormy should be worse

    def test_predict_without_model_returns_reasonable_eta(self):
        from ml.predictor import ETAPredictor
        p = ETAPredictor()
        p.is_loaded = False

        result = p.predict(
            distance_km=10.0,
            google_eta_sec=1800,    # 30 minutes
            hour_of_day=9,
            day_of_week=1,
        )
        assert result["google_eta_min"]  == pytest.approx(30.0, rel=0.01)
        assert result["ml_eta_min"]      > 0
        assert "traffic_condition"       in result
        assert "is_peak_hour"            in result
        assert result["is_peak_hour"]    is True


# ─────────────────────────────────────────────────────────────────────────────
# GOOGLE MAPS SERVICE TESTS (mock mode)
# ─────────────────────────────────────────────────────────────────────────────

class TestGoogleMapsService:

    def test_mock_mode_directions(self):
        """Mock mode should return valid-shaped response."""
        from services.google_maps import GoogleMapsService
        svc = GoogleMapsService()
        svc._mock_mode = True

        result = svc.get_directions(18.5362, 73.8935, 18.5912, 73.7389)
        assert result is not None
        assert "polyline"    in result
        assert "distance_km" in result
        assert "steps"       in result
        assert result["distance_km"] > 0
        assert result["duration_sec"] > 0

    def test_haversine_known_distance(self):
        """Koregaon Park to Hinjewadi is approx 12-15 km straight line."""
        from services.google_maps import GoogleMapsService
        dist = GoogleMapsService._haversine_km(18.5362, 73.8935, 18.5912, 73.7389)
        assert 10.0 < dist < 18.0


# ─────────────────────────────────────────────────────────────────────────────
# ROUTE OPTIMIZER TESTS (full mocked integration)
# ─────────────────────────────────────────────────────────────────────────────

class TestRouteOptimizer:

    def _make_mock_directions(self):
        return {
            "polyline":                "test_polyline_abc",
            "distance_m":              10000,
            "distance_km":             10.0,
            "duration_sec":            1200,
            "duration_in_traffic_sec": 1500,
            "congestion_score":        1.25,
            "steps": [
                {"instruction": "Head north on MG Road", "distance_meters": 500,
                 "duration_seconds": 60, "road_name": "MG Road"},
                {"instruction": "Turn left at FC Road",  "distance_meters": 300,
                 "duration_seconds": 40, "road_name": "FC Road"},
            ],
            "total_turns": 1,
        }

    @pytest.mark.asyncio
    async def test_get_route_cache_hit(self):
        from services.route_optimizer import RouteOptimizer

        mock_cache = MagicMock()
        mock_cache.get_route = AsyncMock(return_value={
            **self._make_mock_directions(),
            "served_from_cache": True,
        })
        mock_cache.make_eta_key = CacheService.make_eta_key
        mock_cache.set_route = AsyncMock(return_value=True)

        mock_predictor = MagicMock()
        mock_predictor.predict.return_value = {
            "ml_eta_min": 28.0,
            "google_eta_min": 25.0,
            "eta_correction_min": 3.0,
            "confidence_score": 0.88,
            "traffic_condition": "heavy",
            "is_peak_hour": True,
            "model_version": "lgbm_v1.0",
            "features": {"weather_condition": "clear", "rainfall_mm": 0.0,
                        "traffic_congestion_score": 2.0, "is_peak_hour": 1},
        }

        optimizer = RouteOptimizer(
            gmaps=MagicMock(),
            cache=mock_cache,
            predictor=mock_predictor,
        )

        with patch("services.route_optimizer.log_eta_prediction", AsyncMock(return_value=1)):
            result = await optimizer.get_route(
                18.5362, 73.8935, 18.5309, 73.8474, 18.5912, 73.7389,
                driver_id="test-driver", ride_id="test-ride",
            )

        assert result.polyline       == "test_polyline_abc"
        assert result.ml_eta_min     == 28.0
        assert result.served_from_cache is True

    @pytest.mark.asyncio
    async def test_reroute_no_improvement(self):
        from services.route_optimizer import RouteOptimizer

        mock_directions = self._make_mock_directions()
        mock_gmaps = MagicMock()
        mock_gmaps.get_directions.return_value = mock_directions

        mock_predictor = MagicMock()
        # New ETA is 29 min, current is 30 min — only 3.3% improvement (below 5% threshold)
        mock_predictor.predict.return_value = {
            "ml_eta_min": 29.0,
            "google_eta_min": 25.0,
            "eta_correction_min": 4.0,
            "confidence_score": 0.88,
            "traffic_condition": "normal",
            "is_peak_hour": False,
            "model_version": "lgbm_v1.0",
            "features": {"weather_condition": "clear", "rainfall_mm": 0.0,
                        "traffic_congestion_score": 1.5, "is_peak_hour": 0},
        }

        optimizer = RouteOptimizer(
            gmaps=mock_gmaps,
            cache=MagicMock(),
            predictor=mock_predictor,
        )

        with patch("services.route_optimizer.log_reroute_event", AsyncMock(return_value=1)):
            result = await optimizer.reroute(
                ride_id="test-ride",
                driver_id="test-driver",
                current_lat=18.5550,
                current_lng=73.8200,
                drop_lat=18.5912,
                drop_lng=73.7389,
                current_eta_min=30.0,
                trigger="app_poll",
            )

        assert result.rerouted is False

    @pytest.mark.asyncio
    async def test_reroute_with_improvement(self):
        from services.route_optimizer import RouteOptimizer

        mock_directions = self._make_mock_directions()
        mock_gmaps = MagicMock()
        mock_gmaps.get_directions.return_value = mock_directions

        mock_predictor = MagicMock()
        # New ETA is 24 min, current is 35 min — 31% improvement
        mock_predictor.predict.return_value = {
            "ml_eta_min": 24.0,
            "google_eta_min": 20.0,
            "eta_correction_min": 4.0,
            "confidence_score": 0.90,
            "traffic_condition": "normal",
            "is_peak_hour": False,
            "model_version": "lgbm_v1.0",
            "features": {"weather_condition": "clear", "rainfall_mm": 0.0,
                        "traffic_congestion_score": 1.5, "is_peak_hour": 0},
        }

        optimizer = RouteOptimizer(
            gmaps=mock_gmaps,
            cache=MagicMock(),
            predictor=mock_predictor,
        )

        with patch("services.route_optimizer.log_reroute_event", AsyncMock(return_value=1)):
            with patch("services.route_optimizer.log_eta_prediction", AsyncMock(return_value=1)):
                result = await optimizer.reroute(
                    ride_id="test-ride",
                    driver_id="test-driver",
                    current_lat=18.5550,
                    current_lng=73.8200,
                    drop_lat=18.5912,
                    drop_lng=73.7389,
                    current_eta_min=35.0,
                    trigger="traffic_change",
                )

        assert result.rerouted    is True
        assert result.new_eta_min == 24.0
        assert result.time_saved_min == pytest.approx(11.0, rel=0.01)


# ─────────────────────────────────────────────────────────────────────────────
# API ENDPOINT TESTS
# ─────────────────────────────────────────────────────────────────────────────

class TestAPIEndpoints:

    @pytest.fixture
    def client(self):
        from fastapi.testclient import TestClient
        from main import app
        return TestClient(app)

    def test_root_returns_200(self, client):
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert data["service"] == "Urban Black Route Optimization Engine"
        assert data["status"]  == "running"

    def test_health_endpoint(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert "status"       in data
        assert "model_loaded" in data
        assert "version"      in data

    def test_docs_accessible(self, client):
        response = client.get("/docs")
        assert response.status_code == 200

    def test_route_endpoint_missing_params(self, client):
        """Should return 422 when required query params are missing."""
        response = client.get("/driver/route")
        assert response.status_code == 422

    def test_eta_endpoint_missing_params(self, client):
        response = client.get("/driver/eta")
        assert response.status_code == 422


# ─────────────────────────────────────────────────────────────────────────────
# RUN
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
