# Telegram Job Aggregator

Production-ready Telegram job aggregator service for Python 3.11.

## Architecture

Telegram channels -> Telethon listener -> parser plugins -> normalized `Job` model -> Kafka topic `jobs.raw`

## Features

- Dynamic parser discovery from `job_aggregator/parsers/*`
- One parser per channel folder
- No channel names hardcoded in the application bootstrap
- Async Telegram listener with isolated parser error handling
- Async-friendly Kafka publisher backed by worker tasks
- Pydantic job normalization
- Standard logging for operations and failures

## Docs

Structured project documentation lives in `docs/`:

- `docs/README.md` - entry point
- `docs/architecture.md` - system design and plugin contract
- `docs/status.md` - implemented work and verification snapshot
- `docs/operations.md` - local runbook and operational commands
- `docs/todo.md` - prioritized next steps

## Project layout

```text
job_aggregator/
  pyproject.toml
  README.md
  .env.example
  job_aggregator/
    app.py
    config.py
    core/
    telegram_client/
    kafka/
    models/
    parsers/
```

## Environment variables

See `.env.example`.

Required values:

- `TELEGRAM_API_ID`
- `TELEGRAM_API_HASH`
- `KAFKA_BOOTSTRAP_SERVERS`

## Run

```bash
python -m venv .venv
source .venv/bin/activate
pip install -e .
export TELEGRAM_API_ID=123456
export TELEGRAM_API_HASH=your_hash
export TELEGRAM_PHONE=+998901234567
export TELEGRAM_LOGIN_METHOD=code
export TELEGRAM_SESSION=job_aggregator.session
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export KAFKA_LOG_LEVEL=WARNING
python -m job_aggregator.app
```

## Telegram authentication

This service expects an existing authorized Telethon session file. It does not prompt for interactive login on startup.

Create a session:

```bash
source .venv/bin/activate
python scripts/create_telegram_session.py
```

Optional:

- set `TELEGRAM_PHONE` in `.env` to avoid typing the phone number every time
- set `TELEGRAM_FORCE_SMS=true` if you want to ask Telegram for SMS delivery instead of in-app delivery when possible
- set `TELEGRAM_LOGIN_METHOD=qr` if in-app codes do not arrive; the script will generate `data/telegram_login_qr.png`

Export channel history for parser tuning:

```bash
source .venv/bin/activate
python scripts/export_channel_history.py --channel Xorazm_ish --limit 1000
python scripts/analyze_channel_history.py --plugin xorazm_ish --input data/exports/xorazm_ish.jsonl
```

Replay exported jobs into Kafka:

```bash
source .venv/bin/activate
docker compose -f infra/docker-compose.kafka.yml up -d
python scripts/replay_export_to_kafka.py --plugin xorazm_ish --input data/exports/xorazm_ish.jsonl
python scripts/consume_kafka_messages.py --from-beginning --max-messages 5
```

## Adding a new parser

1. Create a new folder under `job_aggregator/parsers/`.
2. Add `config.yaml` with the Telegram channel username or identifier.
3. Add `parser.py` with a `Parser(BaseParser)` implementation.
4. Restart the service.

No core code changes are required.
