
## Set up
### 1. Docker
Create kafka container
```
docker compose up -d
docker ps
```

### 2. Kafka
Create kafka topic
```bash
docker exec kafka \
  /usr/bin/kafka-topics \
  --create \
  --topic property_events \
  --bootstrap-server localhost:9092 \
  --partitions 6 \
  --replication-factor 1
```

Check topic is created in kafka cluster
```bash
docker exec kafka \
  /usr/bin/kafka-topics \
  --list \
  --bootstrap-server localhost:9092
```

Expected output
```bash
__consumer_offsets
_schemas
property_events
```

### Python
```python
cd producer
source venv/bin/activate
which pip
pip install -r requeriments.txt
python3 producer.py
```

Expected result:
```bash
python3 producer.py
✅ Produced to property_events [partition=2 offset=0]
✅ Produced to property_events [partition=3 offset=0]
✅ Produced to property_events [partition=4 offset=0]
✅ Produced to property_events [partition=4 offset=1]
✅ Produced to property_events [partition=0 offset=0]
```

Subject URL: `http://localhost:8081/subjects/`

Schema URL: `http://localhost:8081/subjects/property_events-value/versions/latest`

Check messages are flowing
```
kafka-console-consumer \
--bootstrap-server localhost:9092 \
--topic property_events \
--from-beginning
```
Expected output
```bash
Hecc62d6f-3f28-4aaa-b5a7-267f1a561f22LISTING_UPDATEDMLS_MOCK�����fproperty_4��@
                                                                               ACTIVE
Hee8775d2-fe2b-4a5a-b684-02b2f729f3b6LISTING_UPDATEDMLS_MOCK�����fproperty_1L�@
                                                                               ACTIVE
H3eebaf46-ff2b-4976-81e3-ad14bf54cc1dLISTING_UPDATEDMLS_MOCK�����fproperty_3��@
                                                                               ACTIVE
H17bdb99c-c2a4-4496-a256-70105e421631LISTING_UPDATEDMLS_MOCKΕ���fproperty_0j�@
                                                                              ACTIVE
H21709401-a67c-4fda-8988-73d1e92e9bccLISTING_UPDATEDMLS_MOCK�����fproperty_2�@
                                                                              ACTIVE
```

Check actual messages in JSON format (if available)
```bash
/usr/bin/kafka-avro-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic property_events \
  --from-beginning \
  --property schema.registry.url=http://schema-registry:8081
```

### Flink
Flink URL: http://localhost:8082

Check Flink and Kafka connectivity
```bash
docker exec -it flink-jobmanager bash
getent hosts kafka
getent hosts schema-registry
```
Expected output
```bash
172.19.0.3      kafka
172.19.0.4      schema-registry
```

Compile Flink code
```bash
cd flink-consumer
mvn clean package -U
```
Expected output
```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  13.898 s
[INFO] Finished at: 2026-01-27T15:16:48-06:00
[INFO] ------------------------------------------------------------------------
```
Expected Files
```
target/
├── classes/
│   └── com/example/flink/PropertyEventConsumer.class
└── property-flink-consumer-1.0-SNAPSHOT.jar

```

Now copy .jar file to Flink container
```bash
docker exec -it flink-jobmanager \
mkdir -p /opt/flink/jobs/  \

docker cp ./target/property-flink-consumer-1.0-SNAPSHOT.jar flink-jobmanager:/opt/flink/jobs/
````
Run the Flink job
```bash
docker exec -it flink-jobmanager \
  flink run /opt/flink/jobs/property-flink-consumer-1.0-SNAPSHOT.jar
```
Expected output
```bash
Job has been submitted with JobID a522df5b5c31d471b8ae6d64529dae3f
```
Check actually messages are being processed by the Flink Task Manager
```bash
docker logs flink-taskmanager
```
Expected output
```bash
{"event_id": "d9812664-60f1-4478-89d9-949b1730ea70", "event_type": "LISTING_UPDATED", "source_system": "MLS_MOCK", "event_time": 1769552783160, "payload": {"property_id": "property_12", "price": 170000.0, "status": "ACTIVE"}}
{"event_id": "348c4b2a-e006-4c4e-917e-02d1f1031e16", "event_type": "LISTING_UPDATED", "source_system": "MLS_MOCK", "event_time": 1769552783160, "payload": {"property_id": "property_13", "price": 180000.0, "status": "ACTIVE"}}
{"event_id": "bfe327c4-30c1-4848-8a6b-f27c77e8aa81", "event_type": "LISTING_UPDATED", "source_system": "MLS_MOCK", "event_time": 1769552783160, "payload": {"property_id": "property_14", "price": 190000.0, "status": "ACTIVE"}}
```

