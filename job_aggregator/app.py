from __future__ import annotations

import asyncio
import logging
import signal
from contextlib import suppress

from job_aggregator.config import configure_logging, load_settings
from job_aggregator.core.plugin_loader import PluginLoader
from job_aggregator.kafka.producer import KafkaJobProducer
from job_aggregator.telegram_client.client import TelegramJobClient

logger = logging.getLogger(__name__)


async def main() -> None:
    settings = load_settings()
    configure_logging(settings.log_level, settings.kafka_log_level)

    loader = PluginLoader(settings.parsers_path)
    registrations = loader.load()
    if not registrations:
        raise RuntimeError(f"No parser plugins were loaded from {settings.parsers_path}")

    producer = KafkaJobProducer(settings)
    client = TelegramJobClient(settings=settings, registrations=registrations, producer=producer)
    stop_event = asyncio.Event()

    for sig in (signal.SIGINT, signal.SIGTERM):
        with suppress(NotImplementedError):
            asyncio.get_running_loop().add_signal_handler(sig, stop_event.set)

    try:
        await producer.start()
        await client.start()

        listener_task = asyncio.create_task(client.run_until_disconnected(), name="telegram-listener")
        stopper_task = asyncio.create_task(stop_event.wait(), name="shutdown-listener")

        done, pending = await asyncio.wait(
            {listener_task, stopper_task},
            return_when=asyncio.FIRST_COMPLETED,
        )

        for task in pending:
            task.cancel()
            with suppress(asyncio.CancelledError):
                await task

        for task in done:
            exception = task.exception()
            if exception is not None:
                raise exception
    finally:
        await client.stop()
        await producer.stop()
        logger.info("Service shutdown complete")


def cli() -> None:
    asyncio.run(main())


if __name__ == "__main__":
    cli()
