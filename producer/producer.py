from confluent_kafka import SerializingProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from constants import KAFKA_BOOTSTRAP_SERVERS, SCHEMA_REGISTRY_URL, TOPIC, SCHEMA_PATH
from utils import (
    load_schema,
    create_property_event,
    delivery_report,
    property_event_to_dict,
)


def main():
    schema_registry_conf = {"url": SCHEMA_REGISTRY_URL}
    schema_registry_client = SchemaRegistryClient(schema_registry_conf)

    # Load Avro
    avro_schema = load_schema(SCHEMA_PATH)

    # Avro Serializer
    avro_serializer = AvroSerializer(
        schema_registry_client, avro_schema, to_dict=property_event_to_dict
    )

    # Kafka Producer configuration
    producer_conf = {
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
        "key.serializer": lambda v, ctx: v.encode("utf-8"),
        "value.serializer": avro_serializer,
    }

    # Kafka producer
    producer = SerializingProducer(producer_conf)

    for i in range(5):
        event = create_property_event(
            property_id=f"property_{i}", price=50000 + i * 10000, status="ACTIVE"
        )

        producer.produce(
            topic=TOPIC,
            key=event["payload"]["property_id"],
            value=event,
            on_delivery=delivery_report,
        )

        producer.poll(0)

    producer.flush()


if __name__ == "__main__":
    main()
