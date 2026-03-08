from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from kafka import KafkaConsumer

from job_aggregator.config import load_dotenv, load_settings



def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Read messages from Kafka for verification")
    parser.add_argument("--topic", default=None, help="Kafka topic name")
    parser.add_argument("--max-messages", type=int, default=5, help="Maximum number of messages to print")
    parser.add_argument("--from-beginning", action="store_true", help="Consume from the beginning of the topic")
    return parser.parse_args()



def main() -> None:
    args = parse_args()
    load_dotenv(PROJECT_ROOT / ".env")
    settings = load_settings()
    topic = args.topic or settings.kafka_topic

    consumer = KafkaConsumer(
        topic,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        auto_offset_reset="earliest" if args.from_beginning else "latest",
        enable_auto_commit=False,
        consumer_timeout_ms=5000,
        value_deserializer=lambda value: json.loads(value.decode("utf-8")),
    )
    try:
        seen = 0
        for message in consumer:
            print(json.dumps(message.value, ensure_ascii=False))
            seen += 1
            if seen >= args.max_messages:
                break
        print(f"Consumed messages: {seen}")
    finally:
        consumer.close()


if __name__ == "__main__":
    main()
