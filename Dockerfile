FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

COPY pyproject.toml README.md ./
COPY job_aggregator ./job_aggregator

RUN pip install --upgrade pip && pip install .

RUN mkdir -p /app/runtime

CMD ["python", "-m", "job_aggregator.app"]
