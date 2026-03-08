from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import TYPE_CHECKING

from pydantic import BaseModel, ConfigDict

from job_aggregator.models.job import Job

if TYPE_CHECKING:
    from telethon.tl.custom.message import Message


class ParserConfig(BaseModel):
    model_config = ConfigDict(extra="allow", str_strip_whitespace=True)

    channel: str
    source: str | None = None
    enabled: bool = True


class BaseParser(ABC):
    def __init__(self, config: ParserConfig, plugin_name: str) -> None:
        self.config = config
        self.plugin_name = plugin_name

    @property
    def channel(self) -> str:
        return self.config.channel

    @property
    def source_name(self) -> str:
        return self.config.source or self.config.channel

    @abstractmethod
    def parse(self, message: "Message") -> Job | None:
        raise NotImplementedError

    def get_message_text(self, message: "Message") -> str:
        return (getattr(message, "raw_text", None) or getattr(message, "message", None) or "").strip()

    def build_job(
        self,
        *,
        message: "Message",
        title: str,
        description: str,
        company: str | None = None,
        location: str | None = None,
        salary: str | None = None,
        source: str | None = None,
        url: str | None = None,
        posted_at: datetime | None = None,
    ) -> Job | None:
        normalized_title = title.strip()
        normalized_description = description.strip()
        if not normalized_title or not normalized_description:
            return None

        return Job(
            title=normalized_title,
            company=self._optional(company),
            location=self._optional(location),
            salary=self._optional(salary),
            description=normalized_description,
            source=source or self.source_name,
            url=url or self.build_message_url(message),
            posted_at=posted_at or self.get_posted_at(message),
        )

    def build_message_url(self, message: "Message") -> str:
        chat = getattr(message, "chat", None)
        username = getattr(chat, "username", None)
        if username:
            return f"https://t.me/{username}/{message.id}"

        chat_id = getattr(message, "chat_id", None)
        if isinstance(chat_id, int) and str(chat_id).startswith("-100"):
            return f"https://t.me/c/{str(chat_id)[4:]}/{message.id}"

        return f"tg://privatepost?channel={self.channel}&post={message.id}"

    def get_posted_at(self, message: "Message") -> datetime:
        date = getattr(message, "date", None)
        if date is None:
            return datetime.now(timezone.utc)
        if date.tzinfo is None:
            return date.replace(tzinfo=timezone.utc)
        return date

    @staticmethod
    def _optional(value: str | None) -> str | None:
        if value is None:
            return None
        normalized = value.strip()
        return normalized or None
