from utils.geo import haversine, estimate_time
from config.settings import DEPOT_LAT, DEPOT_LNG


def is_ride_feasible(driver_lat, driver_lng,
                     drop_lat, drop_lng,
                     ride_km,
                     remaining_time_min):

    # 1. Estimate ride time
    ride_time = estimate_time(ride_km)

    # 2. Distance from drop → depot
    return_distance = haversine(drop_lat, drop_lng, DEPOT_LAT, DEPOT_LNG)

    # 3. Estimate return time
    return_time = estimate_time(return_distance)

    total_time = ride_time + return_time

    return total_time <= remaining_time_min


def return_probability(remaining_time, total_time):
    if total_time > remaining_time:
        return 0.0
    return max(0.1, 1 - (total_time / remaining_time))