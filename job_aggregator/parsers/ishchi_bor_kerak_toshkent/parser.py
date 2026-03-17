from __future__ import annotations

from job_aggregator.parsers.toshkent_common import BaseToshkentParser


class Parser(BaseToshkentParser):
    extra_non_job_phrases = (
        "android va ios uchun o'z dasturimizni chiqardik",
        "playmarket va appstore orqali yuklab oling",
        "ish beruvchimiz o'zlariga kereli bo'gan ishchilarni",
        "ish beruvchimiz o'zlariga kereli bo'lgan ishchilarni",
    )

    def _drop_contact_link(self, link: str, text: str) -> bool:
        lowered = text.lower()
        return link.startswith("https://t.me/+") and "asosiy kanal" in lowered
