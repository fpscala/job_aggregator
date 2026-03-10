# Telegram Job Aggregator

Telegram job aggregation platform with two runtime parts:

- Python ingestion service: Telegram channels -> parser plugins -> Kafka `jobs.raw`
- Scala backend: Kafka `jobs.raw` -> ETL -> PostgreSQL -> HTTP API -> Telegram channel publishing

## Architecture

```text
Telegram channels
-> Python Telethon listener
-> parser plugins
-> Kafka topic jobs.raw
-> Scala ETL consumer
-> PostgreSQL
-> http4s API
-> Telegram channel publisher
```

## Features

- Dynamic parser discovery from `job_aggregator/parsers/*`
- One parser per Telegram source folder
- Real-time publish of normalized raw jobs into Kafka
- Source-specific Scala ETL and structured enrichment
- PostgreSQL persistence with deduplication
- http4s API for job retrieval
- Telegram banner-based channel publishing from stored jobs
- Standard logging for ingestion, ETL, Kafka, DB, and publish failures

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
  infra/
  docs/
  backend/
  job_aggregator/
    app.py
    config.py
    core/
    telegram_client/
    kafka/
    models/
    parsers/
```

## Infra

Start PostgreSQL and Kafka:

```bash
docker compose -f infra/docker-compose.postgres.yml up -d
docker compose -f infra/docker-compose.kafka.yml up -d
```

Infra endpoints:

- PostgreSQL: `localhost:5432`
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8080`

Stop:

```bash
docker compose -f infra/docker-compose.kafka.yml down
docker compose -f infra/docker-compose.postgres.yml down
```

## Python ingestion setup

Create virtualenv and install:

```bash
cd /Users/prince/IdeaProjects/job_aggregator
python -m venv .venv
source .venv/bin/activate
pip install -e .
```

Minimal Python env:

```bash
export TELEGRAM_API_ID=123456
export TELEGRAM_API_HASH=replace_me
export TELEGRAM_SESSION=job_aggregator.session
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export KAFKA_LOG_LEVEL=WARNING
```

Run ingestion:

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
python -m job_aggregator.app
```

What it does:

1. Connects to Telegram via Telethon.
2. Resolves enabled parser channels.
3. Parses new posts into normalized jobs.
4. Publishes raw JSON events into Kafka topic `jobs.raw`.

## Telegram authentication

Python service expects an existing authorized Telethon session file.

Create a session:

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
python scripts/create_telegram_session.py
```

Useful auth envs:

- `TELEGRAM_PHONE` - phone number for login script
- `TELEGRAM_LOGIN_METHOD=code|qr` - login method
- `TELEGRAM_FORCE_SMS=true|false` - ask Telegram for SMS when possible

If QR login is used, QR image is generated here:

```text
data/telegram_login_qr.png
```

## Scala backend setup

Backend runtime requires Java 17 and `sbt` through `nix develop`.

Minimal backend env:

```bash
export POSTGRES_HOST=127.0.0.1
export POSTGRES_PORT=5432
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres
export POSTGRES_DATABASE=jobs
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Run backend:

```bash
cd /Users/prince/IdeaProjects/job_aggregator/backend
nix develop ./nix#java17 -c sbt runServer
```

What it does:

1. Applies Flyway migrations.
2. Starts Kafka ETL listener for `jobs.raw`.
3. Inserts valid jobs into PostgreSQL.
4. Starts the http4s API.
5. Starts the background Telegram publish job.

Default API port:

- `8000`

API routes:

- `GET /jobs`
- `GET /jobs/{id}`
- `GET /jobs/latest`
- `GET /jobs/search?q=...`

## Telegram channel publishing

Publishing is handled by the Scala backend, not the Python listener.

Required env:

```bash
export TELEGRAM_ENABLED=true
export TELEGRAM_BOT_TOKEN=replace_me
export TELEGRAM_PUBLISH_ENABLED=true
export TELEGRAM_PUBLISH_CHANNEL_CHAT_ID=@your_channel
export TELEGRAM_PUBLISH_BANNER_IMAGE_PATH=/absolute/path/banner.png
```

Rules:

- `TELEGRAM_PUBLISH_BANNER_IMAGE_PATH` must be an absolute file path.
- The bot must already be admin in the target channel.
- Publish metadata is stored in PostgreSQL table `job_channel_posts`.
- `(job_id, channel_chat_id)` uniqueness prevents reposting the same job to the same channel.

Publish flow:

1. Python writes raw jobs to Kafka.
2. Scala ETL stores valid jobs in PostgreSQL.
3. Scala background job selects unpublished jobs.
4. Bot posts each job to the configured channel with the configured banner image.
5. Publish metadata is stored in `job_channel_posts`.

## End-to-end local run

1. Start PostgreSQL and Kafka.
2. Create the Telethon session with `python scripts/create_telegram_session.py`.
3. Start Python ingestion with `python -m job_aggregator.app`.
4. Start Scala backend with `cd backend && nix develop ./nix#java17 -c sbt runServer`.
5. Send a new vacancy post into the watched source channel.
6. Check Kafka UI, PostgreSQL `jobs`, and PostgreSQL `job_channel_posts`.

Useful checks:

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
python scripts/consume_kafka_messages.py --max-messages 5
```

```sql
select count(*) from jobs;
select count(*) from job_channel_posts;
select id, title, source, posted_at from jobs order by created_at desc limit 20;
```

## Parser tuning workflow

Export channel history:

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
python scripts/export_channel_history.py --channel Xorazm_ish --limit 1000
```

Analyze parser coverage:

```bash
python scripts/analyze_channel_history.py --plugin xorazm_ish --input data/exports/xorazm_ish.jsonl
```

Replay exported jobs into Kafka for backfill only:

```bash
docker compose -f infra/docker-compose.kafka.yml up -d
python scripts/replay_export_to_kafka.py --plugin xorazm_ish --input data/exports/xorazm_ish.jsonl
python scripts/consume_kafka_messages.py --from-beginning --max-messages 5
```

Important: replay is for backfill. Real-time ingest is `python -m job_aggregator.app`.

## Adding a new parser

1. Create a new folder under `job_aggregator/parsers/`.
2. Add `config.yaml` with the Telegram channel username or identifier.
3. Add `parser.py` with a `Parser(BaseParser)` implementation.
4. Restart the service.

No core code changes are required.
