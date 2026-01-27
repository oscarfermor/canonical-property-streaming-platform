# Canonical Property Event Stream

A real estate event ingestion platform that generates property events and normalizes them with Apache Flink, and stores them in a Bronze table in Databricks for analytics.

## 📋 Table of Contents
- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Setup Instructions](#setup-instructions)
- [Verification Steps](#verification-steps)
- [Troubleshooting](#troubleshooting)

## 🚀 Project Overview

The platform generates property events a single Kafka topic. Apache Flink validates and normalizes the data, applying quality flags, before appending records to a Databricks Bronze table with full metadata preserved.

### Key Components
- **Kafka**: Event streaming platform for real-time data ingestion
- **Schema Registry**: Central schema management for Avro events
- **Apache Flink**: Stream processing for validation and normalization
- **Databricks**: Data lake for Bronze table storage and analytics

## 🏗️ Architecture

```
Producer (Python) 
    ↓
Kafka Topic (property_events)
    ↓
Schema Registry (Avro)
    ↓
Flink Consumer (Java/Avro)
    ↓
Databricks Bronze Table
```

## 📦 Prerequisites

- Docker & Docker Compose
- Java 11+ (for Flink compilation)
- Maven 3.6+ (for building Flink jobs)
- Python 3.8+ (for producer)
- Databricks workspace with API token (optional, for final storage)
## ⚡ Quick Start

```bash
# 1. Start Docker containers
docker compose up -d

# 2. Create Kafka topic
docker exec kafka /usr/bin/kafka-topics --create --topic property_events \
  --bootstrap-server localhost:9092 --partitions 6 --replication-factor 1

# 3. Run Python producer
cd producer && pip install -r requirements.txt && python3 producer.py

# 4. Compile and run Flink job
cd flink-consumer && mvn clean package -U
docker cp target/property-flink-consumer-1.0-SNAPSHOT.jar flink-jobmanager:/opt/flink/jobs/
docker exec flink-jobmanager flink run /opt/flink/jobs/property-flink-consumer-1.0-SNAPSHOT.jar
```

## 🔧 Setup Instructions

### 1. Start Docker Containers

Spin up Kafka, Schema Registry, and Flink using Docker Compose:

```bash
docker compose up -d
docker ps
```

**Verify containers are running:** You should see kafka, schema-registry, flink-jobmanager, and flink-taskmanager containers.

### 2. Create Kafka Topic

Create the `property_events` topic for event streaming:

```bash
docker exec kafka /usr/bin/kafka-topics --create \
  --topic property_events \
  --bootstrap-server localhost:9092 \
  --partitions 6 \
  --replication-factor 1
```

**Verify topic creation:**

```bash
docker exec kafka /usr/bin/kafka-topics --list \
  --bootstrap-server localhost:9092
```

Expected output:
```
__consumer_offsets
_schemas
property_events
```

### 3. Run Python Producer

The producer generates sample property events and publishes them to Kafka.

```bash
cd producer
python3 -m venv venv  # Create virtual environment (if needed)
source venv/bin/activate
pip install -r requirements.txt
python3 producer.py
```

**Expected result:**
```
✅ Produced to property_events [partition=2 offset=0]
✅ Produced to property_events [partition=3 offset=0]
✅ Produced to property_events [partition=4 offset=0]
✅ Produced to property_events [partition=4 offset=1]
✅ Produced to property_events [partition=0 offset=0]
```

**Schema Registry URLs:**
- Subject list: `http://localhost:8081/subjects/`
- Latest schema: `http://localhost:8081/subjects/property_events-value/versions/latest`

### 4. Verify Kafka Messages

#### View raw (Avro) messages:
```bash
docker exec kafka /usr/bin/kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic property_events \
  --from-beginning
```

#### View JSON-formatted messages (using Avro deserializer):
```bash
docker exec kafka /usr/bin/kafka-avro-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic property_events \
  --from-beginning \
  --property schema.registry.url=http://schema-registry:8081
```

Expected JSON output:
```json
{"event_id": "d9812664-60f1-4478-89d9-949b1730ea70", "event_type": "LISTING_UPDATED", "source_system": "MLS_MOCK", "event_time": 1769552783160, "payload": {"property_id": "property_12", "price": 170000.0, "status": "ACTIVE"}}
```

### 5. Build and Deploy Flink Job

Compile the Flink consumer and deploy it to the Flink cluster.

**Access Flink UI:** `http://localhost:8082`

**Compile the job:**
```bash
cd flink-consumer
mvn clean package -U
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 13.898 s
[INFO] Finished at: 2026-01-27T15:16:48-06:00
```

**Verify build artifacts:**
```
target/
├── classes/
│   └── com/example/flink/PropertyEventConsumer.class
└── property-flink-consumer-1.0-SNAPSHOT.jar
```

**Copy JAR to Flink container:**
```bash
docker exec flink-jobmanager mkdir -p /opt/flink/jobs/
docker cp ./target/property-flink-consumer-1.0-SNAPSHOT.jar \
  flink-jobmanager:/opt/flink/jobs/
```

**Submit the Flink job:**
```bash
docker exec flink-jobmanager flink run \
  /opt/flink/jobs/property-flink-consumer-1.0-SNAPSHOT.jar
```

Expected output:
```
Job has been submitted with JobID a522df5b5c31d471b8ae6d64529dae3f
```

**Monitor job processing:**
```bash
docker logs flink-taskmanager
```

Expected output (processed and normalized events):
```json
{"event_id": "d9812664-60f1-4478-89d9-949b1730ea70", "event_type": "LISTING_UPDATED", "source_system": "MLS_MOCK", "event_time": 1769552783160, "payload": {"property_id": "property_12", "price": 170000.0, "status": "ACTIVE"}}
{"event_id": "348c4b2a-e006-4c4e-917e-02d1f1031e16", "event_type": "LISTING_UPDATED", "source_system": "MLS_MOCK", "event_time": 1769552783160, "payload": {"property_id": "property_13", "price": 180000.0, "status": "ACTIVE"}}
```

## ✅ Verification Steps

### Check Flink-Kafka Connectivity

Ensure Flink and Kafka can communicate within the Docker network:

```bash
docker exec flink-jobmanager bash
getent hosts kafka
getent hosts schema-registry
```

Expected output:
```
172.19.0.3      kafka
172.19.0.4      schema-registry
```

### Monitor Docker Logs

Track real-time processing:

```bash
# Kafka broker
docker logs kafka

# Flink jobmanager
docker logs flink-jobmanager

# Flink taskmanager
docker logs flink-taskmanager

# All containers
docker compose logs -f
```

## 🐛 Troubleshooting

### Producer fails to connect to Kafka
- Verify Kafka is running: `docker ps | grep kafka`
- Check Kafka logs: `docker logs kafka`
- Ensure `KAFKA_BOOTSTRAP_SERVERS=localhost:9092` is set in producer environment

### Flink job fails to compile
```bash
# Clear Maven cache and rebuild
cd flink-consumer
rm -rf target/
mvn clean package -U
```

### No messages appearing in Flink logs
1. Verify Kafka topic has messages: Use kafka-console-consumer to check
2. Verify Flink job is running: Check Flink UI at http://localhost:8082
3. Check job logs: `docker logs flink-taskmanager | tail -100`
4. Ensure Schema Registry is accessible: `curl http://localhost:8081/subjects/`

### Docker network issues
```bash
# Rebuild and restart containers
docker compose down
docker compose up -d --build
```

### Memory/Resource issues
If containers are crashing, increase Docker memory allocation:
- Docker Desktop: Preferences → Resources → Memory (increase to 8GB+)
- Rebuild: `docker compose up -d --force-recreate`

## 📝 Project Structure

```
.
├── README.md                          # This file
├── docker-compose.yml                 # Docker services configuration
├── deploy.sh                          # Deployment script
├── producer/                          # Python Kafka producer
│   ├── producer.py                    # Main producer logic
│   ├── constants.py                   # Configuration constants
│   ├── logger.py                      # Logging utilities
│   ├── utils.py                       # Helper functions
│   └── requirements.txt                # Python dependencies
├── flink-consumer/                    # Java Flink consumer
│   ├── pom.xml                        # Maven configuration
│   ├── src/main/java/.../PropertyEventConsumer.java
│   └── src/main/resources/.../property_event_v1.avsc
├── schemas/                           # Shared Avro schemas
│   └── property_event_v1.avsc         # Property event schema
└── databricks/                        # Databricks integration (future)
```

## 🔗 Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Apache Flink Documentation](https://flink.apache.org/)
- [Confluent Schema Registry](https://docs.confluent.io/schema-registry/)
- [Databricks Documentation](https://docs.databricks.com/)

## 📄 License

TBD

