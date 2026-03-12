from __future__ import annotations

import argparse
import asyncio
import json
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from types import SimpleNamespace

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from job_aggregator.config import configure_logging, load_dotenv, load_settings
from job_aggregator.core.plugin_loader import PluginLoader
from job_aggregator.kafka.producer import KafkaJobProducer


@dataclass
class ExportedMessage:
    id: int
    raw_text: str
    date: datetime
    channel: str
    contact_links: list[str] | None = None

    @property
    def message(self) -> str:
        return self.raw_text

    @property
    def chat(self) -> SimpleNamespace:
        return SimpleNamespace(username=self.channel)

    @property
    def chat_id(self) -> None:
        return None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Replay exported Telegram posts into Kafka as raw collector events")
    parser.add_argument("--plugin", required=True, help="Source config folder name, e.g. xorazm_ish")
    parser.add_argument("--input", required=True, type=Path, help="Path to exported JSONL file")
    parser.add_argument("--limit", type=int, default=None, help="Optional limit of exported records to read")
    parser.add_argument("--topic", default=None, help="Override Kafka topic name")
    return parser.parse_args()


async def main() -> None:
    args = parse_args()
    load_dotenv(PROJECT_ROOT / ".env")
    settings = load_settings()
    configure_logging(settings.log_level)
    if args.topic:
        settings.kafka_topic = args.topic

    loader = PluginLoader(settings.parsers_path)
    registrations = {item.plugin_name: item for item in loader.load()}
    if args.plugin not in registrations:
        available = ", ".join(sorted(registrations))
        raise RuntimeError(f"Plugin '{args.plugin}' not found. Available: {available}")

    producer = KafkaJobProducer(settings)
    parser = registrations[args.plugin].parser

    parsed = 0
    skipped = 0
    total = 0

    await producer.start()
    try:
        with args.input.open("r", encoding="utf-8") as handle:
            for raw_line in handle:
                if args.limit is not None and total >= args.limit:
                    break
                record = json.loads(raw_line)
                total += 1
                message = ExportedMessage(
                    id=record["id"],
                    raw_text=(record.get("text") or "").strip(),
                    date=_parse_datetime(record.get("date")),
                    channel=record.get("channel") or registrations[args.plugin].channel,
                    contact_links=record.get("contact_links"),
                )
                jobs = parser.parse_many(message)
                if not jobs:
                    skipped += 1
                    continue
                for job in jobs:
                    await producer.publish(job)
                    parsed += 1
    finally:
        await producer.stop()

    print(f"Read records: {total}")
    print(f"Published jobs: {parsed}")
    print(f"Skipped records: {skipped}")
    print(f"Kafka topic: {settings.kafka_topic}")



def _parse_datetime(value: str | None) -> datetime:
    if not value:
        return datetime.now(timezone.utc)
    parsed = datetime.fromisoformat(value)
    if parsed.tzinfo is None:
        return parsed.replace(tzinfo=timezone.utc)
    return parsed


if __name__ == "__main__":
    asyncio.run(main())
