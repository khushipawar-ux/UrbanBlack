import asyncio
import json
import logging
import os
from services.feature_store import feature_store

# Note: library expects to run in async context
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
TOPIC_NAME = 'live_traffic_events'

logger = logging.getLogger(__name__)

async def start_kafka_consumer():
    try:
        from aiokafka import AIOKafkaConsumer
    except ImportError:
        logger.warning("aiokafka not installed, skipping kafka consumer")
        return

    consumer = AIOKafkaConsumer(
        TOPIC_NAME,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_deserializer=lambda x: json.loads(x.decode('utf-8'))
    )
    
    try:
        await consumer.start()
        logger.info("Kafka consumer started listening for traffic events...")
        async for msg in consumer:
            event = msg.value
            lat = event.get('lat')
            lng = event.get('lng')
            score = event.get('congestion_score')
            if lat and lng and score:
                # Update the segment weights dynamically during the trip
                # enables real-time re-routing without calling full Directions API 
                feature_store.update_congestion_score(lat, lng, score)
                logger.debug(f"Updated congestion score for {lat},{lng} to {score}")
    except Exception as e:
        logger.error(f"Kafka consumer error: {e}")
    finally:
        await consumer.stop()
