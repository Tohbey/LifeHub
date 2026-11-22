#!/usr/bin/env bash
# Creates LifeHub's Kafka topics with explicit partitions and retention (deploy/docs/events.md, "Topics").
# Safe to run again: existing topics are left alone.
#
# Usage: deploy/kafka/create-topics.sh
#   KAFKA_CONTAINER   the Kafka container's name (default: kafka)
#   BOOTSTRAP         a listener reachable from inside that container (default: kafka:29092, the INTERNAL one)
set -euo pipefail

KAFKA_CONTAINER="${KAFKA_CONTAINER:-kafka}"
BOOTSTRAP="${BOOTSTRAP:-kafka:29092}"
DAY_MS=86400000

# One event topic per producer, 3 partitions, 14 days. Each gets a dead-letter topic, 1 partition, 30 days.
EVENT_TOPICS=(identity.events finance.events task.events calendar.events shopping.events)

create() {
    local topic="$1" partitions="$2" retention_days="$3"
    docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" \
        --create --if-not-exists --topic "$topic" --partitions "$partitions" --replication-factor 1 \
        --config "retention.ms=$((retention_days * DAY_MS))"
}

for topic in "${EVENT_TOPICS[@]}"; do
    create "$topic" 3 14
    create "$topic.dlt" 1 30
done

docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --list
