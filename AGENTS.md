# Repository Guidelines

## Project Structure & Module Organization
`job_aggregator/` contains the Python ingestion service: Telethon client, Kafka producer, shared models, and parser plugins under `job_aggregator/parsers/<source_name>/`. Keep parser logic in `parser.py` and channel metadata in `config.yaml`. `tests/` holds Python parser regressions. `scripts/` covers Telegram auth, export/replay, and Kafka inspection. `backend/` is the Scala service: `common/`, `supports/`, and `endpoints/00-domain` through `05-runner`. Infra files live in `infra/`; design and runbook notes live in `docs/`.

## Build, Test, and Development Commands
- `python -m venv .venv && source .venv/bin/activate && pip install -e .` installs the ingestion service.
- `docker compose -f infra/docker-compose.postgres.yml up -d` and `docker compose -f infra/docker-compose.kafka.yml up -d` start local dependencies.
- `source .venv/bin/activate && python -m job_aggregator.app` runs the Telegram-to-Kafka listener.
- `source .venv/bin/activate && python -m unittest discover -s tests` runs Python tests.
- `nix develop ./nix#java17 -c sbt runServer` starts migrations, Kafka ETL, API, and Telegram publishing.
- `nix develop ./nix#java17 -c sbt test` runs Scala tests.
- `nix develop ./nix#java17 -c sbt styleCheck` verifies formatting, compile health, and scalafix rules.

## Coding Style & Naming Conventions
Python uses 4-space indentation, type hints, `snake_case` modules/functions, and `PascalCase` classes. Keep plugin folders and source identifiers lowercase with underscores, for example `xorazm_ish_bor_elonlar`. Scala formatting is enforced by `backend/.scalafmt.conf`: 100-column lines, sorted imports, and trailing commas in multiline definitions. Use `UpperCamelCase` for Scala types/objects, `lowerCamelCase` for methods/vals, and version SQL migrations as `VNNN__description.sql`.

## Testing Guidelines
Add Python regressions in `tests/test_<plugin>.py` using focused `unittest` cases that exercise both `parse()` and `parse_many()` when posts can split into multiple jobs. Add Scala tests under `backend/**/src/test/scala`, and prefer `sbt testOnly <SuiteName>` while iterating. No numeric coverage gate is configured; add a regression test for each new parser rule, ETL branch, or migration behavior.

## Commit & Pull Request Guidelines
Recent commits follow Conventional Commit style with scopes, for example `fix(xorazm-ish): recognized new benefits markers`. Keep subjects imperative and scope them to the parser, ETL, API, or infra area you changed. PRs should summarize user-visible impact, list the commands you ran, mention env or migration changes, and include sample payloads or screenshots when output formatting changes.

## Security & Configuration Tips
Do not commit `.env`, Telethon session files, exported channel history, or generated QR images. Start from `.env.example`, keep `TELEGRAM_PUBLISH_BANNER_IMAGE_PATH` absolute, and treat `data/` as local development state unless a file is intentionally tracked.
