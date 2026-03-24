"""
utils/ — Shared utility helpers used across the engine.

Currently available:
  - haversine_km(lat1, lng1, lat2, lng2) : straight-line distance in km
  - is_pune_coords(lat, lng)             : sanity-check coordinates are in Pune region
  - format_duration(minutes)             : e.g. 75.5 → '1 hr 15 min'
  - truncate_polyline(polyline, max_len) : shorten polyline string for logging

Add new project-wide helpers here rather than scattering them across modules.
"""

import math


def haversine_km(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    """
    Haversine great-circle distance between two GPS coordinates.
    Returns straight-line distance in kilometres.
    Note: actual road distance is typically 1.25–1.45× this value.
    """
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlng = math.radians(lng2 - lng1)
    a = (math.sin(dlat / 2) ** 2 +
         math.cos(math.radians(lat1)) *
         math.cos(math.radians(lat2)) *
         math.sin(dlng / 2) ** 2)
    return R * 2 * math.asin(math.sqrt(a))


def is_pune_coords(lat: float, lng: float) -> bool:
    """
    Return True if coordinates fall within the greater Pune metropolitan area.
    Bounding box: lat 18.35–18.75, lng 73.60–74.10
    """
    return (18.35 <= lat <= 18.75) and (73.60 <= lng <= 74.10)


def format_duration(minutes: float) -> str:
    """
    Convert a duration in minutes to a human-readable string.

    Examples:
        4.5   → '4 min'
        75.0  → '1 hr 15 min'
        120.0 → '2 hr'
    """
    minutes = max(0.0, float(minutes))
    total_min = round(minutes)
    if total_min < 60:
        return f"{total_min} min"
    hrs = total_min // 60
    mins = total_min % 60
    if mins == 0:
        return f"{hrs} hr"
    return f"{hrs} hr {mins} min"


def truncate_polyline(polyline: str, max_len: int = 80) -> str:
    """
    Shorten a Google encoded polyline string for safe use in log lines.
    Full polylines can be 1000+ characters.
    """
    if len(polyline) <= max_len:
        return polyline
    return polyline[:max_len] + f"…[{len(polyline)} chars]"


__all__ = [
    "haversine_km",
    "is_pune_coords",
    "format_duration",
    "truncate_polyline",
]
