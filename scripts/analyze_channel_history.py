from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from types import SimpleNamespace

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from job_aggregator.core.plugin_loader import PluginLoader
from job_aggregator.models.job import Job


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
    parser = argparse.ArgumentParser(description="Analyze exported Telegram posts with a parser plugin")
    parser.add_argument("--plugin", required=True, help="Parser plugin folder name, e.g. xorazm_ish")
    parser.add_argument("--input", required=True, type=Path, help="Path to exported JSONL file")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    loader = PluginLoader(PROJECT_ROOT / "job_aggregator" / "parsers")
    registrations = {item.plugin_name: item for item in loader.load()}
    if args.plugin not in registrations:
        available = ", ".join(sorted(registrations))
        raise RuntimeError(f"Plugin '{args.plugin}' not found. Available: {available}")

    parser = registrations[args.plugin].parser
    total = 0
    parsed_messages = 0
    parsed_jobs = 0
    missing_salary = 0
    missing_location = 0
    failures: list[dict[str, object]] = []
    examples: list[Job] = []

    with args.input.open("r", encoding="utf-8") as handle:
        for raw_line in handle:
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
                if len(failures) < 20:
                    failures.append({"id": message.id, "text": message.raw_text[:400]})
                continue

            parsed_messages += 1
            parsed_jobs += len(jobs)
            for job in jobs:
                if not job.salary:
                    missing_salary += 1
                if not job.location:
                    missing_location += 1
                if len(examples) < 5:
                    examples.append(job)

    success_rate = (parsed_messages / total * 100) if total else 0.0
    print(f"Total messages: {total}")
    print(f"Messages with parsed jobs: {parsed_messages}")
    print(f"Parsed jobs: {parsed_jobs}")
    print(f"Message success rate: {success_rate:.2f}%")
    print(f"Missing salary: {missing_salary}")
    print(f"Missing location: {missing_location}")

    if examples:
        print("\nSample parsed jobs:")
        for job in examples:
            print(json.dumps(job.model_dump(mode="json"), ensure_ascii=False))

    if failures:
        failure_path = PROJECT_ROOT / "data" / "analysis" / f"{args.plugin}_failures.json"
        failure_path.parent.mkdir(parents=True, exist_ok=True)
        failure_path.write_text(json.dumps(failures, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"\nSaved failure samples to {failure_path}")


def _parse_datetime(value: str | None) -> datetime:
    if not value:
        return datetime.now(timezone.utc)
    parsed = datetime.fromisoformat(value)
    if parsed.tzinfo is None:
        return parsed.replace(tzinfo=timezone.utc)
    return parsed


if __name__ == "__main__":
    main()
