# Canonical Property Event Stream update

A real estate event ingestion platform that generates property events and normalizes them with Apache Flink, and stores them in a Bronze table in Databricks for analytics.

## 📋 Table of Contents
- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Required AWS Resources](#required-aws-resources-before-running-the-flink-job)
- [Databricks Configuration](#databricks-configuration-aws-glue-integration)
- [Data in Databricks](#data-in-databricks-screenshots)
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

## ✅ REQUIRED AWS RESOURCES (before running the Flink job)

### 1️⃣ S3 bucket (MANDATORY)

You **must create this first**:

```
canonical-property-streaming-platform
```

### 2️⃣ IAM user or role with S3 access (MANDATORY)

Your Flink job (local Docker, EC2, EKS, etc.) must run with credentials that can:

- `s3:ListBucket`
- `s3:GetObject`
- `s3:PutObject`
- `s3:DeleteObject`

Policy example:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowListBucket",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::<ACCOUNT_ID>:root"
      },
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::canonical-property-streaming-platform"
    },
    {
      "Sid": "AllowObjectAccess",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::<ACCOUNT_ID>:root"
      },
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::canonical-property-streaming-platform/*"
    }
  ]
}
```

### 3️⃣ Bucket Versioning

🔶 **Recommended: ENABLE**

Why (important for Iceberg):

- Iceberg commits are metadata-driven
- Versioning helps:
    - Recover from accidental deletes
    - Debug failed commits
    - Safer experimentation

Tradeoff:

- Slightly higher storage cost

✅ **Enable it** if this is not a throwaway project.

### 4️⃣ Final recommended S3 bucket selections (TL;DR)

| Setting | Value |
| --- | --- |
| Region | us-west-2 |
| Bucket type | General purpose |
| Object ownership | Bucket owner enforced |
| Public access | Block all |
| Versioning | **Enable** |
| Encryption | SSE-S3 |
| ACLs | Disabled |

You are **100% safe** with this setup.

### 5️⃣ AWS Glue Database — **YOU must create**

Glue **does not auto-create databases**.

You must create this manually:

```
Glue Database name: db
Region: us-west-2
```

This maps to:

```java
TableIdentifier.of("db","property_events_valid")
```

📌 If this DB does not exist:

```
NoSuchDatabaseException
```

#### Required Glue Permissions

```json
{
  "Effect": "Allow",
  "Action": [
    "glue:GetDatabase",
    "glue:GetDatabases",
    "glue:GetTable",
    "glue:GetTables",
    "glue:CreateTable",
    "glue:UpdateTable"
  ],
  "Resource": "*"
}
```

#### Step 1: Create the Database

1. Go to the **AWS Console** → search for **Glue** → open it
2. In the left sidebar, click **Databases**
3. Click **Add database**
4. Enter a name (e.g., `db`)
5. Click **Create**

#### Step 2: Create the Iceberg Table

1. In the left sidebar, click **Tables**
2. Click **Add table**
3. Select the database you just created (`db`)
4. Enter the table name: `property_events_valid`
5. Choose **Data warehouse** as the data source type
6. Set the **S3 location** to:

   ```
   s3://canonical-property-streaming-platform/iceberg-warehouse/db/property_events_valid
   ```

7. Define your columns:

   | Name | Type |
   | --- | --- |
   | property_id | string |
   | price | double |
   | currency | string |
   | event_time | timestamp |

8. Under table type / format, select **Iceberg** (or set the table type to `ICEBERG` in the table parameters)
9. Click **Create**

### 6️⃣ Configure AWS credentials locally

Export your AWS credentials before running the Flink job:

```bash
export AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id)
export AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key)
```

## 🔗 Databricks Configuration (AWS Glue Integration)

### Prerequisites

- **Databricks workspace** deployed in AWS (same region as your S3 bucket)
- **Databricks personal access token** for API authentication
- AWS Glue database and table created (steps above)

### Step 1: Add AWS Glue as an External Location (Databricks)

1. Go to **Databricks workspace** → **Catalog** (sidebar)
2. Click **External Locations**
3. Click **Create Location**
4. Configure:
   - **Location name**: `canonical-property-warehouse`
   - **URL**: `s3://canonical-property-streaming-platform/iceberg-warehouse/`
   - **Access credential**: Select your AWS credentials or create new
5. Click **Create**

### Step 2: Create a Schema/Database (Databricks)

1. In **Catalog** section, click **Schemas**
2. Click **Create Schema**
3. Configure:
   - **Schema name**: `db`
   - **External location**: `canonical-property-warehouse` (from Step 1)
   - **Owner**: Your user or service principal
4. Click **Create**

### Step 3: Link AWS Glue Catalog (Optional but Recommended)

Databricks can directly sync with AWS Glue tables:

1. Go to **Admin Console** → **Catalogs**
2. Click **Create Catalog** → **AWS Glue**
3. Configure:
   - **Catalog name**: `glue_catalog`
   - **Metastore**: AWS Glue
   - **Region**: `us-west-2` (or your region)
   - **IAM role**: Select role with Glue permissions
4. Click **Create**

Your Flink job writes Iceberg tables to S3 → AWS Glue tracks metadata → Databricks reads via Glue catalog.

### Step 4: Query and Verify Data in Databricks

```sql
-- Query using Glue catalog
SELECT * FROM glue_catalog.db.property_events_valid LIMIT 10;

-- Or query directly if table is synced
SELECT * FROM db.property_events_valid LIMIT 10;
```

Expected output:
```
property_id | price    | currency | event_time
------------|----------|----------|----------------------
property_1  | 150000.0 | USD      | 2026-02-02 22:35:10
property_2  | 250000.0 | USD      | 2026-02-02 22:35:11
property_3  | 350000.0 | USD      | 2026-02-02 22:35:12
```

---

## 📊 Data in Databricks (Screenshots)

Add your Databricks screenshots here to document the data flow:

### Screenshot 1: Catalog View
![Databricks Catalog - External Location](./docs/screenshots/databricks_catalog.png)

### Screenshot 2: Query Results
![Databricks Query Results - Property Events Table](./docs/screenshots/databricks_query_results.png)

### Screenshot 3: Table Stats
![Databricks Table Stats and Metadata](./docs/screenshots/databricks_table_stats.png)

### Screenshot 4: Lineage View
![Databricks Data Lineage - Flink to Iceberg to Glue](./docs/screenshots/databricks_lineage.png)

---

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
Expected output
```bash
VALID> PropertyEvent{eventId='33875527-d194-4425-8e24-8976b207d196', eventType='LISTING_UPDATED', sourceSystem='MLS_MOCK', eventTime=1769725516379, payload=PropertyPayload{propertyId='property_4', price=90000.0, status='ACTIVE'}}
VALID> PropertyEvent{eventId='0a3b9525-da04-4f48-9dc8-ae9c1c20a341', eventType='LISTING_UPDATED', sourceSystem='MLS_MOCK', eventTime=1769725516379, payload=PropertyPayload{propertyId='property_1', price=60000.0, status='ACTIVE'}}
VALID> PropertyEvent{eventId='a8752000-5373-447c-995e-bb57ab03e73c', eventType='LISTING_UPDATED', sourceSystem='MLS_MOCK', eventTime=1769725516379, payload=PropertyPayload{propertyId='property_3', price=80000.0, status='ACTIVE'}}
VALID> PropertyEvent{eventId='8f51b2cb-5c56-448d-8b66-94f4d4638d24', eventType='LISTING_UPDATED', sourceSystem='MLS_MOCK', eventTime=1769725516026, payload=PropertyPayload{propertyId='property_0', price=50000.0, status='ACTIVE'}}
VALID> PropertyEvent{eventId='7f9a4b63-db5f-4bd8-b55a-2e3ee3f14065', eventType='LISTING_UPDATED', sourceSystem='MLS_MOCK', eventTime=1769725516379, payload=PropertyPayload{propertyId='property_2', price=70000.0, status='ACTIVE'}}
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

