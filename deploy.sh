#!/bin/bash
set -euo pipefail

export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)
# Start Docker services
docker-compose up -d

# Wait for services to be ready
sleep 10

# Create Kafka topic if it doesn't exist
docker exec kafka /usr/bin/kafka-topics \
  --create \
  --topic property_events \
  --bootstrap-server localhost:9092 \
  --partitions 6 \
  --replication-factor 1 || true

# Build and Deploy Flink job
cd flink-consumer
mvn clean package -U
FLINK_JAR=./target/property-flink-consumer-1.0-SNAPSHOT.jar

docker exec -it flink-jobmanager mkdir -p /opt/flink/jobs/
docker cp $FLINK_JAR flink-jobmanager:/opt/flink/jobs/
docker exec -it flink-jobmanager flink run /opt/flink/jobs/property-flink-consumer-1.0-SNAPSHOT.jar

# Run Python producer
cd ../producer
source venv/bin/activate
pip install -r requirements.txt
python producer.py
