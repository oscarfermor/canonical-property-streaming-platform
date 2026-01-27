import uuid
from typing import Dict
import time


def load_schema(path: str) -> str:
    with open(path, "r") as f:
        return f.read()


def create_property_event(property_id: str, price: float, status: str) -> Dict:
    """Create a Property Event matching the Avro Schema"""
    return {
        "event_id": str(uuid.uuid4()),
        "event_type": "LISTING_UPDATED",
        "source_system": "MLS_MOCK",
        "event_time": int(time.time() * 1000),
        "payload": {"property_id": property_id, "price": price, "status": status},
    }


def property_event_to_dict(event, ctx):
    return event


def delivery_report(err, msg):
    """Delivery callback for Kafka produce requests."""
    if err is not None:
        print(f"❌ Delivery failed: {err}")
    else:
        print(
            f"✅ Produced to {msg.topic()} "
            f"[partition={msg.partition()} offset={msg.offset()}]"
        )
