"""
tests/ — Test suite for the Urban Black Route Optimization Engine.

Test groups (all in test_engine.py):
  1. Feature engineering  — peak hour, cyclical encoding, feature extraction, df prep
  2. Cache service        — key rounding, in-memory set/get/delete
  3. ETA predictor        — heuristic fallback, model inference, rainy > clear
  4. Google Maps service  — mock mode directions, haversine distance
  5. Route optimizer      — cache-hit flow, reroute with/without improvement
  6. API endpoints        — FastAPI TestClient, 422 on missing params

Run all tests:
    pytest tests/ -v

Run a single group:
    pytest tests/test_engine.py::TestFeatureEngineering -v

Run with coverage:
    pytest tests/ --cov=. --cov-report=term-missing
"""
