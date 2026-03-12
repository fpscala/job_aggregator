from __future__ import annotations

from typing import TYPE_CHECKING

from job_aggregator.core.parser_base import BaseParser
from job_aggregator.models.job import Job

if TYPE_CHECKING:
    from telethon.tl.custom.message import Message


class RawMessageParser(BaseParser):
    RAW_TITLE = "raw_post"

    def parse(self, message: "Message") -> Job | None:
        raw_text = self.get_message_text(message)
        if not raw_text:
            return None

        return self.build_job(
            message=message,
            title=self.RAW_TITLE,
            description=raw_text,
        )
