# Infra

## Start PostgreSQL

```bash
docker compose -f infra/docker-compose.postgres.yml up -d
```

- PostgreSQL: `localhost:5432`

PostgreSQL defaults:

- database: `jobs`
- user: `postgres`
- password: `postgres`

## Start Kafka

```bash
docker compose -f infra/docker-compose.kafka.yml up -d
```

Services:

- Kafka broker: `localhost:9092`
- Kafka UI: `http://localhost:8080`
- Topic bootstrap: `jobs.raw`

## Stop

```bash
docker compose -f infra/docker-compose.kafka.yml down
docker compose -f infra/docker-compose.postgres.yml down
```

To remove persisted data too:

```bash
docker compose -f infra/docker-compose.kafka.yml down -v
docker compose -f infra/docker-compose.postgres.yml down -v
```
