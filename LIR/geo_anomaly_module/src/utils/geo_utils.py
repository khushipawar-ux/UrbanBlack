from geopy.distance import geodesic

def calculate_distance(p1, p2):
    return geodesic(p1, p2).km


def deviation_from_route(current_point, route_points):
    """
    Calculate minimum distance from current point to route polyline
    """
    normalized_route = []
    for point in route_points:
        if isinstance(point, dict):
            normalized_route.append((point.get('lat'), point.get('lng')))
        elif isinstance(point, (list, tuple)):
            normalized_route.append((point[0], point[1]))
        else:
            raise ValueError(f"Unsupported route point type: {type(point)}")

    distances = [
        calculate_distance(current_point, p)
        for p in normalized_route
    ]
    return min(distances) if distances else 0