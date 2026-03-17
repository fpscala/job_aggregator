from __future__ import annotations

import re

from job_aggregator.parsers.toshkent_common import BaseToshkentParser


class Parser(BaseToshkentParser):
    extra_non_job_phrases = (
        "uyda o'tirgan holda ishlashga taklif qilaman",
        "uyda o‘tirgan holda ishlashga taklif qilaman",
        "faberlic emas",
        "oriflame emas",
        "oriflame emas",
        "online daromad",
        "kapital depazit kiritmaysiz",
        "telegram orqali yuklarni bog'laysiz",
        "telegram orqali yuklarni bog’laysiz",
        "kursimga 70% chegirma",
    )
    extra_salary_patterns = (
        re.compile(r"^(?:[\W_]+\s*)?(?:daromad|to['’`]?lov|tolov|ish\s+haqqi|ish\s+xaqqi)\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
    )
    extra_address_patterns = (
        re.compile(r"^(?:[\W_]+\s*)?(?:adres|address)\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
    )

    def _extract_location(self, lines: list[str]) -> str | None:
        location = super()._extract_location(lines)
        if location:
            return location

        for line in lines:
            cleaned = self._cleanup_field_value(line)
            lowered = cleaned.lower()
            if "hududni o'zingiz belgilaysiz" in lowered or "hududni o'zingiz tanlaysiz" in lowered:
                return cleaned.rstrip("!.,")

        return None
