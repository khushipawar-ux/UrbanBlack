import math

def calculate_bearing(lat1, lon1, lat2, lon2):
    """
    Calculate angle (bearing) between two points
    """
    lat1 = math.radians(lat1)
    lat2 = math.radians(lat2)
    diff_lon = math.radians(lon2 - lon1)

    x = math.sin(diff_lon) * math.cos(lat2)
    y = math.cos(lat1) * math.sin(lat2) - (
        math.sin(lat1) * math.cos(lat2) * math.cos(diff_lon)
    )

    initial_bearing = math.atan2(x, y)
    bearing = math.degrees(initial_bearing)

    return (bearing + 360) % 360


def direction_score(pickup_lat, pickup_lng, drop_lat, drop_lng, depot_lat, depot_lng):
    """
    Compare ride direction with depot direction
    """
    ride_direction = calculate_bearing(pickup_lat, pickup_lng, drop_lat, drop_lng)
    depot_direction = calculate_bearing(pickup_lat, pickup_lng, depot_lat, depot_lng)

    angle_diff = abs(ride_direction - depot_direction)

    if angle_diff > 180:
        angle_diff = 360 - angle_diff

    # Convert to score (1 = same direction, 0 = opposite)
    return 1 - (angle_diff / 180)