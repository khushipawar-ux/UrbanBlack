import os
import numpy as np
import logging
from typing import Dict, Tuple

logger = logging.getLogger(__name__)

# Kalman filter noise parameters (tunable via .env)
_Q = float(os.getenv("KALMAN_PROCESS_NOISE", "0.01"))       # process noise
_R = float(os.getenv("KALMAN_MEASUREMENT_NOISE", "5.0"))     # measurement noise (GPS accuracy ~5m)


class KalmanFilter2D:
    """
    2D Kalman filter for GPS smoothing.

    State vector: [lat, lng, v_lat, v_lng]
      - lat, lng    : position
      - v_lat, v_lng: velocity in deg/sec

    Models uniform motion between GPS updates. Handles urban interference:
    signal multipath, tall buildings, network jitter — common in Maharashtra cities.
    """

    def __init__(self, process_noise: float = _Q, measurement_noise: float = _R):
        dt = float(os.getenv("GPS_UPDATE_INTERVAL_SEC", "3"))

        # State transition matrix (constant velocity model)
        self.F = np.array([
            [1, 0, dt, 0 ],
            [0, 1, 0,  dt],
            [0, 0, 1,  0 ],
            [0, 0, 0,  1 ],
        ], dtype=float)

        # Measurement matrix (we only observe lat, lng directly)
        self.H = np.array([
            [1, 0, 0, 0],
            [0, 1, 0, 0],
        ], dtype=float)

        # Process noise covariance
        self.Q = np.eye(4) * process_noise

        # Measurement noise covariance (GPS horizontal error ~5m → ~0.00005 deg)
        self.R = np.eye(2) * measurement_noise

        # State vector and covariance — initialised on first fix
        self.x: np.ndarray = None    # [lat, lng, v_lat, v_lng]
        self.P: np.ndarray = np.eye(4) * 1.0

    def _init_state(self, lat: float, lng: float):
        self.x = np.array([lat, lng, 0.0, 0.0], dtype=float)
        self.P = np.eye(4) * 1.0

    def update(self, lat: float, lng: float) -> Tuple[float, float]:
        """
        Feed a new raw GPS measurement.
        Returns (smoothed_lat, smoothed_lng).
        """
        if self.x is None:
            self._init_state(lat, lng)
            return lat, lng

        # --- Predict step ---
        x_pred = self.F @ self.x
        P_pred = self.F @ self.P @ self.F.T + self.Q

        # --- Update step ---
        z = np.array([lat, lng], dtype=float)
        y = z - self.H @ x_pred                                    # innovation
        S = self.H @ P_pred @ self.H.T + self.R                    # innovation covariance
        K = P_pred @ self.H.T @ np.linalg.inv(S)                   # Kalman gain
        self.x = x_pred + K @ y
        self.P = (np.eye(4) - K @ self.H) @ P_pred

        smoothed_lat = float(self.x[0])
        smoothed_lng = float(self.x[1])
        logger.debug(
            f"Kalman: raw=({lat:.6f},{lng:.6f}) → smooth=({smoothed_lat:.6f},{smoothed_lng:.6f})"
        )
        return smoothed_lat, smoothed_lng


# Registry: one filter instance per active driver, keyed by driver_id
_driver_filters: Dict[str, KalmanFilter2D] = {}


def get_filter(driver_id: str) -> KalmanFilter2D:
    """Return the Kalman filter for a driver, creating one on first call."""
    if driver_id not in _driver_filters:
        _driver_filters[driver_id] = KalmanFilter2D()
        logger.info(f"Initialised Kalman filter for driver {driver_id}")
    return _driver_filters[driver_id]


def reset_filter(driver_id: str):
    """Reset filter state at end of shift / session stop."""
    if driver_id in _driver_filters:
        del _driver_filters[driver_id]
        logger.info(f"Reset Kalman filter for driver {driver_id}")


def smooth_gps(driver_id: str, lat: float, lng: float) -> Tuple[float, float]:
    """Convenience wrapper — apply the driver's Kalman filter to a raw GPS fix."""
    kf = get_filter(driver_id)
    return kf.update(lat, lng)
