import os
import json
import logging
from datetime import datetime, timezone

logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
TOPIC_LOCATION_UPDATES = os.getenv("KAFKA_TOPIC_LOCATION_UPDATES", "driver_location_updates")

# Module-level producer instance (initialised in lifespan)
_producer = None


async def start_kafka_producer():
    """Initialise and start the AIOKafka producer. Called at app startup."""
    global _producer
    try:
        from aiokafka import AIOKafkaProducer
    except ImportError:
        logger.warning("aiokafka not installed — Kafka publishing disabled.")
        return

    _producer = AIOKafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )
    try:
        await _producer.start()
        logger.info(f"Kafka producer started → {KAFKA_BOOTSTRAP_SERVERS} / topic: {TOPIC_LOCATION_UPDATES}")
    except Exception as e:
        logger.warning(
            f"Kafka broker unreachable at {KAFKA_BOOTSTRAP_SERVERS}: {e}. "
            "Location publishing disabled — DB persistence still active."
        )
        await _producer.stop()
        _producer = None


async def stop_kafka_producer():
    """Flush and close the producer. Called at app shutdown."""
    global _producer
    if _producer:
        await _producer.stop()
        _producer = None
        logger.info("Kafka producer stopped.")


async def publish_location(
    driver_id: str,
    smoothed_lat: float,
    smoothed_lng: float,
    bearing: float | None,
    speed_kmh: float | None,
    ride_id: str | None,
    is_on_trip: bool,
    location_id: str,
):
    """
    Publish a smoothed driver location update to the `driver_location_updates` Kafka topic.

    Downstream consumers:
      - Rider live map (shows stable driver pin)
      - Geo-Spatial Anomaly Detection model (LGAD)
      - Feature Store (real-time driver location features)
    """
    if _producer is None:
        logger.debug("Kafka producer not available, skipping publish.")
        return

    message = {
        "driver_id": driver_id,
        "smoothed_lat": smoothed_lat,
        "smoothed_lng": smoothed_lng,
        "bearing": bearing,
        "speed_kmh": speed_kmh,
        "is_on_trip": is_on_trip,
        "ride_id": ride_id,
        "location_id": location_id,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }

    try:
        await _producer.send_and_wait(TOPIC_LOCATION_UPDATES, message)
        logger.debug(f"Published location for driver {driver_id} to Kafka.")
    except Exception as e:
        logger.error(f"Failed to publish location for driver {driver_id}: {e}")
