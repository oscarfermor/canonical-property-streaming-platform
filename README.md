



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