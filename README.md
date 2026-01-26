

### Docker
```
docker compose up -d
docker ps
```


### Kafka
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
