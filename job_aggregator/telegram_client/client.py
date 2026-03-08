from __future__ import annotations

import asyncio
import logging
from contextlib import suppress

from telethon import TelegramClient, events
from telethon.utils import get_peer_id

from job_aggregator.config import Settings
from job_aggregator.core.plugin_loader import ParserRegistration
from job_aggregator.kafka.producer import KafkaJobProducer

logger = logging.getLogger(__name__)


class TelegramJobClient:
    def __init__(
        self,
        settings: Settings,
        registrations: list[ParserRegistration],
        producer: KafkaJobProducer,
    ) -> None:
        self._settings = settings
        self._registrations = registrations
        self._producer = producer
        self._client = TelegramClient(
            settings.telegram_session,
            settings.telegram_api_id,
            settings.telegram_api_hash,
        )
        self._channel_map: dict[int, ParserRegistration] = {}
        self._entities: list[object] = []
        self._tasks: set[asyncio.Task[None]] = set()
        self._semaphore = asyncio.Semaphore(settings.parser_concurrency)

    async def start(self) -> None:
        logger.info("Connecting to Telegram")
        await self._client.connect()
        if not await self._client.is_user_authorized():
            raise RuntimeError(
                "Telegram session is not authorized. Create a Telethon session before starting the service.",
            )

        await self._resolve_channels()
        if not self._entities:
            raise RuntimeError("No parser channels could be resolved on Telegram")

        self._client.add_event_handler(self._on_new_message, events.NewMessage(chats=self._entities))
        logger.info("Telegram client subscribed to %s channel(s)", len(self._entities))

    async def run_until_disconnected(self) -> None:
        logger.info("Telegram listener is running")
        await self._client.run_until_disconnected()

    async def stop(self) -> None:
        if self._client.is_connected():
            await self._client.disconnect()
            logger.info("Telegram client disconnected")

        active_tasks = list(self._tasks)
        if not active_tasks:
            return

        done, pending = await asyncio.wait(active_tasks, timeout=30)
        for task in done:
            with suppress(asyncio.CancelledError, Exception):
                task.result()

        for task in pending:
            task.cancel()
        for task in pending:
            with suppress(asyncio.CancelledError):
                await task

        self._tasks.clear()

    async def _resolve_channels(self) -> None:
        for registration in self._registrations:
            try:
                entity = await self._client.get_entity(registration.channel)
            except Exception:
                logger.exception(
                    "Failed to resolve Telegram channel '%s' for plugin '%s'",
                    registration.channel,
                    registration.plugin_name,
                )
                continue

            peer_id = get_peer_id(entity)
            entity_id = getattr(entity, "id", None)
            self._channel_map[peer_id] = registration
            if isinstance(entity_id, int):
                self._channel_map[entity_id] = registration
            self._entities.append(entity)
            logger.info(
                "Resolved Telegram channel '%s' for parser '%s'",
                registration.channel,
                registration.plugin_name,
            )

    async def _on_new_message(self, event: events.NewMessage.Event) -> None:
        task = asyncio.create_task(self._process_event(event))
        self._tasks.add(task)
        task.add_done_callback(self._tasks.discard)

    async def _process_event(self, event: events.NewMessage.Event) -> None:
        async with self._semaphore:
            message = event.message
            registration = self._find_registration(event)
            if registration is None:
                logger.warning("Skipping message from unresolved chat_id=%s", event.chat_id)
                return

            try:
                job = registration.parser.parse(message)
            except Exception:
                logger.exception(
                    "Parser '%s' failed for channel '%s' message_id=%s",
                    registration.plugin_name,
                    registration.channel,
                    getattr(message, "id", None),
                )
                return

            if job is None:
                logger.debug(
                    "Parser '%s' skipped message_id=%s from channel '%s'",
                    registration.plugin_name,
                    getattr(message, "id", None),
                    registration.channel,
                )
                return

            logger.info(
                "Parsed job title='%s' source='%s' message_id=%s",
                job.title,
                job.source,
                getattr(message, "id", None),
            )
            await self._producer.publish(job)

    def _find_registration(self, event: events.NewMessage.Event) -> ParserRegistration | None:
        candidates = [event.chat_id]
        try:
            candidates.append(get_peer_id(event.message.peer_id))
        except Exception:
            pass

        for candidate in candidates:
            if isinstance(candidate, int) and candidate in self._channel_map:
                return self._channel_map[candidate]
        return None
