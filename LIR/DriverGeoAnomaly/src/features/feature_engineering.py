import pandas as pd
from src.utils.geo_utils import calculate_distance

def create_features(df):
    df = df.copy()

    # Speed change
    df['speed_change'] = df['speed'].diff().fillna(0)

    # Distance between points
    distances = [0]
    for i in range(1, len(df)):
        prev = (df.iloc[i-1]['lat'], df.iloc[i-1]['lng'])
        curr = (df.iloc[i]['lat'], df.iloc[i]['lng'])
        distances.append(calculate_distance(prev, curr))

    df['distance'] = distances

    # Acceleration (important)
    df['acceleration'] = df['speed_change']

    # Stop flag
    df['is_stop'] = (df['speed'] < 5).astype(int)

    # High speed flag
    df['is_high_speed'] = (df['speed'] > 90).astype(int)

    # Sudden change detection
    df['sudden_change'] = (abs(df['speed_change']) > 30).astype(int)

    return df[[
        'speed',
        'speed_change',
        'distance',
        'acceleration',
        'is_stop',
        'is_high_speed',
        'sudden_change'
    ]]