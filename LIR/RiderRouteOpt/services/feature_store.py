import random

class FeatureStore:
    def __init__(self):
        self.cache = {}
        
    def get_segment_congestion_score(self, start_lat: float, start_lng: float, end_lat: float, end_lng: float) -> float:
        """
        Pulls pre-computed road-segment congestion scores.
        Blends historical speed profiles with current live traffic signal.
        Returns score between 0.0 (open road) and 1.0 (gridlock).
        """
        # Mock logic. In real production, this fetches from Redis/Feast feature store.
        cache_key = f"{round(start_lat,3)}:{round(start_lng,3)}"
        if cache_key not in self.cache:
            self.cache[cache_key] = random.uniform(0.1, 0.9)
        return self.cache[cache_key]
        
    def get_historical_speed_profile(self, road_class: str, current_time) -> int:
        """ Returns max expected speed on this road class at this time of day. """
        speeds_by_class = {
            'highway': 80,
            'arterial': 40,
            'residential': 20
        }
        return speeds_by_class.get(road_class.lower(), 30)
        
    def update_congestion_score(self, lat: float, lng: float, new_score: float):
        """ Dynamically updated via background Kafka traffic events. """
        cache_key = f"{round(lat,3)}:{round(lng,3)}"
        self.cache[cache_key] = new_score

feature_store = FeatureStore()
