

```bash
docker compose -f docker-compose.kafka.yaml up -d   # start
```
```bash
docker compose -f docker-compose.kafka.yaml down    # stop & delete state
```
### # Create an explicit topic (useful when auto‑create is off)
```bash
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic poc.events --replication-factor 1
```
### # List topics
```bash
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```