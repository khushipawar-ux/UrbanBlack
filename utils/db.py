import psycopg2
import pandas as pd
from config.settings import DB_CONFIG


def get_connection():
    return psycopg2.connect(**DB_CONFIG)


def fetch_data(query, params=None):
    conn = get_connection()
    df = pd.read_sql(query, conn, params=params)
    conn.close()
    return df