from __future__ import annotations

import logging
import os
from functools import lru_cache
from pathlib import Path

from pydantic import BaseModel, Field


class Settings(BaseModel):
    telegram_api_id: int
    telegram_api_hash: str
    telegram_session: str = "job_aggregator.session"
    kafka_bootstrap_servers: list[str] = Field(default_factory=lambda: ["localhost:9092"])
    kafka_topic: str = "jobs.raw"
    kafka_client_id: str = "job-aggregator"
    kafka_request_timeout_ms: int = 10000
    kafka_max_block_ms: int = 10000
    kafka_worker_count: int = 4
    kafka_queue_size: int = 1000
    parser_concurrency: int = 50
    log_level: str = "INFO"
    kafka_log_level: str = "WARNING"
    parsers_path: Path = Field(
        default_factory=lambda: Path(__file__).resolve().parent / "parsers",
    )

    @classmethod
    def from_env(cls) -> "Settings":
        telegram_api_id = os.getenv("TELEGRAM_API_ID")
        telegram_api_hash = os.getenv("TELEGRAM_API_HASH")
        if not telegram_api_id or not telegram_api_hash:
            raise RuntimeError(
                "TELEGRAM_API_ID and TELEGRAM_API_HASH must be set before starting the service.",
            )

        kafka_bootstrap_servers = [
            server.strip()
            for server in os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092").split(",")
            if server.strip()
        ]

        parsers_path_value = os.getenv("PARSERS_PATH")
        parsers_path = (
            Path(parsers_path_value).expanduser().resolve()
            if parsers_path_value
            else Path(__file__).resolve().parent / "parsers"
        )

        return cls(
            telegram_api_id=int(telegram_api_id),
            telegram_api_hash=telegram_api_hash,
            telegram_session=os.getenv("TELEGRAM_SESSION", "job_aggregator.session"),
            kafka_bootstrap_servers=kafka_bootstrap_servers,
            kafka_topic=os.getenv("KAFKA_TOPIC", "jobs.raw"),
            kafka_client_id=os.getenv("KAFKA_CLIENT_ID", "job-aggregator"),
            kafka_request_timeout_ms=int(os.getenv("KAFKA_REQUEST_TIMEOUT_MS", "10000")),
            kafka_max_block_ms=int(os.getenv("KAFKA_MAX_BLOCK_MS", "10000")),
            kafka_worker_count=max(1, int(os.getenv("KAFKA_WORKER_COUNT", "4"))),
            kafka_queue_size=max(1, int(os.getenv("KAFKA_QUEUE_SIZE", "1000"))),
            parser_concurrency=max(1, int(os.getenv("PARSER_CONCURRENCY", "50"))),
            log_level=os.getenv("LOG_LEVEL", "INFO"),
            kafka_log_level=os.getenv("KAFKA_LOG_LEVEL", "WARNING"),
            parsers_path=parsers_path,
        )


@lru_cache(maxsize=1)
def load_settings() -> Settings:
    load_dotenv()
    return Settings.from_env()


def configure_logging(level: str, kafka_level: str = "WARNING") -> None:
    logging.basicConfig(
        level=getattr(logging, level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )

    resolved_kafka_level = getattr(logging, kafka_level.upper(), logging.WARNING)
    logging.getLogger("kafka").setLevel(resolved_kafka_level)
    logging.getLogger("kafka.conn").setLevel(resolved_kafka_level)


def load_dotenv(dotenv_path: Path | None = None) -> None:
    candidate_paths = []
    if dotenv_path is not None:
        candidate_paths.append(dotenv_path)
    else:
        candidate_paths.extend(
            [
                Path.cwd() / ".env",
                Path(__file__).resolve().parent.parent / ".env",
            ],
        )

    dotenv_file = next((path for path in candidate_paths if path.exists()), None)
    if dotenv_file is None:
        return

    for raw_line in dotenv_file.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        normalized_key = key.strip()
        if not normalized_key or normalized_key in os.environ:
            continue

        os.environ[normalized_key] = _strip_quotes(value.strip())


def _strip_quotes(value: str) -> str:
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
        return value[1:-1]
    return value
