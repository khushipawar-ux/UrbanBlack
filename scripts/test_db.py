from utils.db import fetch_data

query = "SELECT * FROM driver LIMIT 5;"

df = fetch_data(query)

print(df.head())