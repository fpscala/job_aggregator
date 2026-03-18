# Operations

## Muhit talablari

- Tavsiya etilgan Python runtime: `Python 3.11`
- Scala backend runtime: `Java 17` + `sbt`
- Telegram API credentials: `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`
- Authorized Telethon session fayli kerak
- Lokal Kafka broker kerak
- Lokal PostgreSQL kerak

## Environment variables

Asosiy Python envlar `.env.example` ichida bor.

Minimal Python env:

```env
TELEGRAM_API_ID=123456
TELEGRAM_API_HASH=replace_me
TELEGRAM_SESSION=job_aggregator.session
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC=jobs.raw
```

Qo'shimcha foydali Python envlar:

- `TELEGRAM_PHONE` - auth script uchun telefon raqam
- `TELEGRAM_LOGIN_METHOD=code|qr` - login usuli
- `TELEGRAM_FORCE_SMS=true|false` - mumkin bo'lsa SMS so'rash
- `PARSER_CONCURRENCY` - bir vaqtning o'zida parse qilinadigan message soni
- `KAFKA_WORKER_COUNT` - producer workerlar soni
- `KAFKA_QUEUE_SIZE` - producer queue hajmi
- `LOG_LEVEL` - log darajasi
- `KAFKA_LOG_LEVEL` - `kafka-python` log darajasi, default `WARNING`

Minimal Scala backend env:

```env
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DATABASE=jobs
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Telegram publish uchun backend env:

```env
TELEGRAM_ENABLED=true
TELEGRAM_BOT_TOKEN=replace_me
TELEGRAM_PUBLISH_ENABLED=true
TELEGRAM_PUBLISH_CHANNEL_CHAT_ID=@your_channel
TELEGRAM_PUBLISH_BANNER_IMAGE_PATH=/absolute/path/banner.png
```

## Virtual environment

```bash
cd /Users/prince/IdeaProjects/job_aggregator
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
```

## Telegram session yaratish

### Code login

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
python scripts/create_telegram_session.py
```

### QR login

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
export TELEGRAM_LOGIN_METHOD=qr
python scripts/create_telegram_session.py
```

QR image shu joyga saqlanadi:

```text
data/telegram_login_qr.png
```

## PostgreSQL'ni ko'tarish

```bash
docker compose -f infra/docker-compose.postgres.yml up -d
```

- PostgreSQL: `localhost:5432`
- database: `jobs`
- user: `postgres`
- password: `postgres`

## Kafka'ni ko'tarish

```bash
docker compose -f infra/docker-compose.kafka.yml up -d
```

Natija:

- Kafka broker: `localhost:9092`
- Kafka UI: `http://localhost:8080`
- Topic: `jobs.raw`

To'xtatish:

```bash
docker compose -f infra/docker-compose.postgres.yml down
docker compose -f infra/docker-compose.kafka.yml down
```

## Python ingestion servisni ishga tushirish

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
python -m job_aggregator.app
```

Live flow:

1. Telegram client channelga subscribe bo'ladi.
2. Yangi message keladi.
3. Mos parser ishlaydi.
4. `Job` qaytsa Kafka'ga publish bo'ladi.

## Python ingestion Docker orqali

Tavsiya etilgan volume:

```text
data/telegram-session/job_aggregator.session
```

Container ichidagi default session path:

```text
/app/runtime/job_aggregator.session
```

Docker build + run:

```bash
cd /Users/prince/IdeaProjects/job_aggregator
docker compose \
  --env-file .env \
  -f infra/docker-compose.kafka.yml \
  -f infra/docker-compose.ingestion.yml \
  up -d --build
```

Muhim:

- `TELEGRAM_API_ID` va `TELEGRAM_API_HASH` `.env` yoki shell envda bo'lishi shart.
- Authorized Telethon session fayli `data/telegram-session/` ichida bo'lishi kerak.
- Container ichidagi default broker `kafka:19092`, override uchun `INGESTION_KAFKA_BOOTSTRAP_SERVERS` ishlating.
- Container ichidagi default session path `/app/runtime/job_aggregator.session`, override uchun `INGESTION_TELEGRAM_SESSION` ishlating.

To'xtatish:

```bash
docker compose \
  -f infra/docker-compose.kafka.yml \
  -f infra/docker-compose.ingestion.yml \
  down
```

## Scala backendni ishga tushirish

```bash
cd /Users/prince/IdeaProjects/job_aggregator/backend
nix develop ./nix#java17 -c sbt runServer
```

Nima bo'ladi:

1. Flyway migrationlar apply qilinadi.
2. Kafka `jobs.raw` listener ishga tushadi.
3. Valid joblar PostgreSQL `jobs` jadvaliga yoziladi.
4. API `8000` portda ishga tushadi.
5. Telegram publish job banner image bilan channelga post qiladi, agar Telegram publish envlari berilgan bo'lsa.

Muhim:

- `TELEGRAM_PUBLISH_BANNER_IMAGE_PATH` absolute path bo'lishi kerak.
- Bot target channelda admin bo'lishi kerak.
- Publish metadata `job_channel_posts` jadvaliga yoziladi.

## End-to-end local tartib

1. `docker compose -f infra/docker-compose.postgres.yml up -d`
2. `docker compose -f infra/docker-compose.kafka.yml up -d`
3. `python scripts/create_telegram_session.py`
4. `python -m job_aggregator.app`
5. `cd backend && nix develop ./nix#java17 -c sbt runServer`
6. Source channelga yangi vacancy post yuboriladi.
7. `jobs` va `job_channel_posts` jadvallari tekshiriladi.

## Kafka eventlarini tekshirish

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
python scripts/consume_kafka_messages.py --from-beginning --max-messages 5
```

Agar faqat yangi message'larni ko'rmoqchi bo'lsangiz:

```bash
python scripts/consume_kafka_messages.py --max-messages 5
```

## PostgreSQL tekshiruv so'rovlari

```sql
select id, title, source, source_url, posted_at
from jobs
order by created_at desc
limit 20;
```

```sql
select job_id, channel_chat_id, telegram_message_id, published_at
from job_channel_posts
order by published_at desc
limit 20;
```

## Parser tuning workflow

### 1. Kanal history'sini eksport qilish

```bash
cd /Users/prince/IdeaProjects/job_aggregator
source .venv/bin/activate
python scripts/export_channel_history.py --channel Xorazm_ish --limit 1000
```

### 2. Parser coverage'ni tahlil qilish

```bash
python scripts/analyze_channel_history.py --plugin xorazm_ish --input data/exports/xorazm_ish.jsonl
```

### 3. Kerak bo'lsa replay/backfill qilish

```bash
python scripts/replay_export_to_kafka.py --plugin xorazm_ish --input data/exports/xorazm_ish.jsonl
```

Muhim: replay script backfill uchun. Real-time ingest `python -m job_aggregator.app` orqali ishlaydi.

## Yangi parser qo'shish bo'yicha amaliy qadamlar

1. `job_aggregator/parsers/<new_plugin>/` papkasini yarating.
2. `config.yaml` ichiga `channel`, `source`, `enabled` yozing.
3. `parser.py` ichida `Parser(BaseParser)` klassini yozing.
4. Servisni restart qiling.
5. Kanal history'sini eksport qilib parserni tahlil qiling.

## Operatsion eslatmalar

- `job_aggregator.session` maxfiy fayl; repository'ga commit qilinmasligi kerak.
- Parser exception butun servisni yiqitmaydi, lekin loglarda ko'rinadi.
- Kafka publish exception ham log qilinadi, worker loop davom etadi.
- Job bo'lmagan postlar normal holatda skip qilinadi.
- Telegram publish default holatda o'chirilgan; env orqali yoqiladi.
