from src.db.db_connection import get_connection
from src.utils.logger import logger


def save_anomaly(data):
    conn = get_connection()

    if conn is None:
        logger.error("DB connection failed, skipping insert")
        return

    try:
        cur = conn.cursor()

        cur.execute("""
            INSERT INTO lir_geo_anomaly_events 
            (anomaly_type, severity)
            VALUES (%s, %s)
        """, (data["anomaly"], data["severity"]))

        conn.commit()
        cur.close()
        conn.close()

        logger.info("Anomaly saved to DB")

    except Exception as e:
        logger.error(f"DB Insert Error: {e}")