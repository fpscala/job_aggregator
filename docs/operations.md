# Operations

## Muhit talablari

- Tavsiya etilgan runtime: `Python 3.11`
- Telegram API credentials: `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`
- Authorized Telethon session fayli kerak
- Lokal Kafka broker kerak

## Environment variables

Asosiy envlar `.env.example` ichida bor.

Minimal ishlash uchun:

```env
TELEGRAM_API_ID=123456
TELEGRAM_API_HASH=replace_me
TELEGRAM_SESSION=job_aggregator.session
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC=jobs.raw
```

Qo'shimcha foydali envlar:

- `TELEGRAM_PHONE` - auth script uchun telefon raqam
- `TELEGRAM_LOGIN_METHOD=code|qr` - login usuli
- `TELEGRAM_FORCE_SMS=true|false` - mumkin bo'lsa SMS so'rash
- `PARSER_CONCURRENCY` - bir vaqtning o'zida parse qilinadigan message soni
- `KAFKA_WORKER_COUNT` - publisher workerlar soni
- `KAFKA_QUEUE_SIZE` - producer queue hajmi
- `LOG_LEVEL` - log darajasi

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
source .venv/bin/activate
python scripts/create_telegram_session.py
```

### QR login

```bash
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

PostgreSQL defaultlari:

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

## Live servisni ishga tushirish

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

## Kafka eventlarini tekshirish

```bash
source .venv/bin/activate
python scripts/consume_kafka_messages.py --from-beginning --max-messages 5
```

Agar faqat yangi message'larni ko'rmoqchi bo'lsangiz:

```bash
python scripts/consume_kafka_messages.py --max-messages 5
```

## Parser tuning workflow

### 1. Kanal history'sini eksport qilish

```bash
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

Muhim: replay script backfill uchun. Real-time publish `python -m job_aggregator.app` orqali ishlaydi.

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
