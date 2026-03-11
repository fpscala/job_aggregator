from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime, timezone
import re
from typing import TYPE_CHECKING, Iterable
from urllib.parse import urlparse

from pydantic import BaseModel, ConfigDict

from job_aggregator.models.job import Job

try:
    from telethon.tl.types import MessageEntityTextUrl, MessageEntityUrl
except ImportError:  # pragma: no cover - Telethon is expected in runtime
    MessageEntityTextUrl = type("MessageEntityTextUrl", (), {})
    MessageEntityUrl = type("MessageEntityUrl", (), {})

if TYPE_CHECKING:
    from telethon.tl.custom.message import Message


VISIBLE_URL_PATTERN = re.compile(r"(?i)\b(?:https?://|t\.me/)[^\s<>()]+")
TRAILING_URL_PUNCTUATION = ".,;:!?)]}>\"'"
TELEGRAM_HOSTS = {"t.me", "www.t.me", "telegram.me", "www.telegram.me"}


class ParserConfig(BaseModel):
    model_config = ConfigDict(extra="allow", str_strip_whitespace=True)

    channel: str
    source: str | None = None
    enabled: bool = True


def extract_message_text(message: "Message") -> str:
    return (getattr(message, "raw_text", None) or getattr(message, "message", None) or "").strip()


def build_message_url(message: "Message", fallback_channel: str | None = None) -> str:
    chat = getattr(message, "chat", None)
    username = getattr(chat, "username", None)
    if username:
        return f"https://t.me/{username}/{message.id}"

    chat_id = getattr(message, "chat_id", None)
    if isinstance(chat_id, int) and str(chat_id).startswith("-100"):
        return f"https://t.me/c/{str(chat_id)[4:]}/{message.id}"

    channel = (fallback_channel or "unknown").strip()
    return f"tg://privatepost?channel={channel}&post={message.id}"


def extract_message_contact_links(
    message: "Message",
    *,
    source_channel_names: Iterable[str] = (),
    message_url: str | None = None,
) -> list[str]:
    text = extract_message_text(message)
    candidates: list[str] = []

    exported_links = getattr(message, "contact_links", None) or []
    candidates.extend(str(value) for value in exported_links if value)

    for entity in getattr(message, "entities", None) or []:
        if isinstance(entity, MessageEntityTextUrl):
            candidates.append(getattr(entity, "url", ""))
            continue

        if isinstance(entity, MessageEntityUrl):
            offset = max(int(getattr(entity, "offset", 0)), 0)
            length = max(int(getattr(entity, "length", 0)), 0)
            raw_value = text[offset : offset + length]
            if raw_value:
                candidates.append(raw_value)

    candidates.extend(VISIBLE_URL_PATTERN.findall(text))

    ignored_usernames = {
        value.strip().lstrip("@").lower()
        for value in source_channel_names
        if value and value.strip()
    }
    normalized_message_url = _normalize_link(message_url or "")

    links: list[str] = []
    seen: set[str] = set()
    for value in candidates:
        normalized = _normalize_link(value)
        if not normalized:
            continue
        if normalized == normalized_message_url:
            continue
        if _is_source_telegram_link(normalized, ignored_usernames):
            continue
        if normalized in seen:
            continue
        seen.add(normalized)
        links.append(normalized)

    return links


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

    def parse_many(self, message: "Message") -> list[Job]:
        job = self.parse(message)
        return [job] if job is not None else []

    def get_message_text(self, message: "Message") -> str:
        return extract_message_text(message)

    def get_message_contact_links(self, message: "Message") -> list[str]:
        chat = getattr(getattr(message, "chat", None), "username", None)
        return extract_message_contact_links(
            message,
            source_channel_names=(self.channel, self.source_name, chat or ""),
            message_url=self.build_message_url(message),
        )

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
        contact_links: list[str] | None = None,
    ) -> Job | None:
        normalized_title = title.strip()
        normalized_description = description.strip()
        if not normalized_title or not normalized_description:
            return None

        resolved_contact_links = self._optional_links(
            contact_links if contact_links is not None else self.get_message_contact_links(message)
        )

        return Job(
            title=normalized_title,
            company=self._optional(company),
            location=self._optional(location),
            salary=self._optional(salary),
            description=normalized_description,
            source=source or self.source_name,
            url=url or self.build_message_url(message),
            posted_at=posted_at or self.get_posted_at(message),
            contact_links=resolved_contact_links,
        )

    def build_message_url(self, message: "Message") -> str:
        return build_message_url(message, fallback_channel=self.channel)

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

    @staticmethod
    def _optional_links(values: list[str] | None) -> list[str] | None:
        if not values:
            return None

        normalized: list[str] = []
        seen: set[str] = set()
        for value in values:
            item = value.strip()
            if not item or item in seen:
                continue
            seen.add(item)
            normalized.append(item)

        return normalized or None


def _normalize_link(value: str) -> str:
    normalized = value.strip().strip("<>[]{}()")
    normalized = normalized.rstrip(TRAILING_URL_PUNCTUATION)
    if not normalized:
        return ""

    if normalized.lower().startswith("t.me/"):
        normalized = f"https://{normalized}"

    parsed = urlparse(normalized)
    if not parsed.scheme and parsed.path.startswith("http"):
        normalized = parsed.path
        parsed = urlparse(normalized)

    return normalized if parsed.scheme in {"http", "https"} and parsed.netloc else ""


def _is_source_telegram_link(link: str, ignored_usernames: set[str]) -> bool:
    parsed = urlparse(link)
    if parsed.netloc.lower() not in TELEGRAM_HOSTS:
        return False

    parts = [value.lower() for value in parsed.path.split("/") if value]
    if not parts:
        return False

    username = parts[1] if len(parts) > 1 and parts[0] == "s" else parts[0]

    return username in ignored_usernames
