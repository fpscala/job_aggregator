from __future__ import annotations

import asyncio
import json
import logging
from contextlib import suppress
from typing import Any

from kafka import KafkaProducer
from kafka.errors import KafkaError

from job_aggregator.config import Settings
from job_aggregator.models.job import Job

logger = logging.getLogger(__name__)


class KafkaJobProducer:
    def __init__(self, settings: Settings) -> None:
        self._settings = settings
        self._topic = settings.kafka_topic
        self._queue: asyncio.Queue[Job] = asyncio.Queue(maxsize=settings.kafka_queue_size)
        self._producer: KafkaProducer | None = None
        self._worker_tasks: list[asyncio.Task[None]] = []

    async def start(self) -> None:
        loop = asyncio.get_running_loop()
        self._producer = await loop.run_in_executor(None, self._build_producer)
        self._worker_tasks = [
            asyncio.create_task(self._worker(index), name=f"kafka-publisher-{index}")
            for index in range(self._settings.kafka_worker_count)
        ]
        logger.info(
            "Kafka producer started for topic '%s' with %s worker(s)",
            self._topic,
            self._settings.kafka_worker_count,
        )

    async def publish(self, job: Job) -> None:
        await self._queue.put(job)

    async def stop(self) -> None:
        if self._worker_tasks:
            await self._queue.join()
            for task in self._worker_tasks:
                task.cancel()
            for task in self._worker_tasks:
                with suppress(asyncio.CancelledError):
                    await task
            self._worker_tasks.clear()

        if self._producer is not None:
            loop = asyncio.get_running_loop()
            await loop.run_in_executor(None, self._shutdown_sync)
            self._producer = None
            logger.info("Kafka producer stopped")

    def _build_producer(self) -> KafkaProducer:
        return KafkaProducer(
            bootstrap_servers=self._settings.kafka_bootstrap_servers,
            client_id=self._settings.kafka_client_id,
            request_timeout_ms=self._settings.kafka_request_timeout_ms,
            max_block_ms=self._settings.kafka_max_block_ms,
            value_serializer=self._serialize,
            retries=5,
            linger_ms=50,
        )

    async def _worker(self, worker_index: int) -> None:
        logger.debug("Kafka worker %s started", worker_index)
        while True:
            job = await self._queue.get()
            try:
                await self._send(job)
            except KafkaError:
                logger.exception(
                    "Kafka publish failed for source='%s' title='%s'",
                    job.source,
                    job.title,
                )
            except Exception:
                logger.exception(
                    "Unexpected producer failure for source='%s' title='%s'",
                    job.source,
                    job.title,
                )
            finally:
                self._queue.task_done()

    async def _send(self, job: Job) -> None:
        if self._producer is None:
            raise RuntimeError("Kafka producer has not been started")

        payload = job.model_dump(mode="json", exclude_none=True)
        loop = asyncio.get_running_loop()
        metadata = await loop.run_in_executor(None, self._send_sync, payload)
        logger.info(
            "Published job to Kafka topic='%s' partition=%s offset=%s source='%s'",
            metadata.topic,
            metadata.partition,
            metadata.offset,
            job.source,
        )

    def _send_sync(self, payload: dict[str, Any]) -> Any:
        assert self._producer is not None
        future = self._producer.send(self._topic, value=payload)
        return future.get(timeout=10)

    def _shutdown_sync(self) -> None:
        assert self._producer is not None
        self._producer.flush(timeout=10)
        self._producer.close(timeout=10)

    @staticmethod
    def _serialize(payload: dict[str, Any]) -> bytes:
        return json.dumps(payload, ensure_ascii=False).encode("utf-8")
