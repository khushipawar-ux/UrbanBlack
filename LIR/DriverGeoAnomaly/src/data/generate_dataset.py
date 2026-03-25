import pandas as pd
import random

data = []

BASE_LAT = 18.5204
BASE_LNG = 73.8567

for i in range(10000):

    # Simulate normal movement
    lat = BASE_LAT + random.uniform(-0.05, 0.05)
    lng = BASE_LNG + random.uniform(-0.05, 0.05)

    # Speed distribution
    rand = random.random()

    if rand < 0.7:
        speed = random.randint(30, 70)  # normal driving
    elif rand < 0.85:
        speed = random.randint(80, 120)  # high speed anomaly
    else:
        speed = random.randint(0, 10)  # stop anomaly

    bearing = random.randint(0, 360)

    data.append({
        "driverId": f"D{random.randint(1, 50)}",
        "lat": round(lat, 6),
        "lng": round(lng, 6),
        "speed": speed,
        "bearing": bearing
    })

df = pd.DataFrame(data)

df.to_csv("data/raw/large_driver_data.csv", index=False)

print("✅ Large dataset generated (10,000 rows)")