from __future__ import annotations

import unittest
from datetime import datetime, timezone
from types import SimpleNamespace

from job_aggregator.core.parser_base import ParserConfig
from job_aggregator.parsers.ishchi_bor_kerak_toshkent.parser import Parser


class FakeMessage:
    def __init__(self, text: str, message_id: int = 1, contact_links: list[str] | None = None) -> None:
        self.raw_text = text
        self.message = text
        self.id = message_id
        self.date = datetime(2026, 3, 17, 10, 0, tzinfo=timezone.utc)
        self.chat = SimpleNamespace(username="ishchi_bor_kerak_toshkent")
        self.chat_id = -1001234567890
        self.entities: list[object] = []
        self.contact_links = contact_links


class IshchiBorKerakToshkentParserTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.parser = Parser(
            config=ParserConfig(channel="ishchi_bor_kerak_toshkent", source="ishchi_bor_kerak_toshkent"),
            plugin_name="ishchi_bor_kerak_toshkent",
        )

    def test_extracts_title_company_and_filters_channel_invite_link(self) -> None:
        message = FakeMessage(
            """
            ⚡️ MYFXBRO Trading kompaniyasiga call-markaz operatori kerak

            💵 Maosh: 8.000.000 - 20.000.000
            📍 Hudud: Toshkent

            📣 Ish haqida:
            – MYFXBRO Trading kompaniyasiga call-markaz operatori kerak

            ⏰ Ish vaqti:
            – 09:00 dan 20:00 gacha

            📌 Manzil:
            – Toshkent shahar, Chilonzor tumani, 16-gorbolnitsa roparasi

            ☎️ Bog‘lanish:
            – +998901234567

            👍 ASOSIY KANAL 1MLN 👈
            """,
            contact_links=["https://t.me/+hm6Ihq9d7okxNTZi", "https://forms.gle/apply"],
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "call-markaz operatori")
        self.assertEqual(job.company, "MYFXBRO Trading kompaniyasi")
        self.assertEqual(job.location, "Toshkent shahar, Chilonzor tumani, 16-gorbolnitsa roparasi")
        self.assertEqual(job.salary, "8.000.000 - 20.000.000")
        self.assertEqual(job.contact_links, ["https://forms.gle/apply"])

    def test_blocks_channel_app_promo_posts(self) -> None:
        message = FakeMessage(
            """
            #Diqqat
            👍 ISHBOR oilasiga yanada ko'proq qulaylik yaratish maqsadida Android va IOS uchun o'z dasturimizni chiqardik!

            PlayMarket va AppStore orqali yuklab oling👇
            """
        )

        self.assertIsNone(self.parser.parse(message))

    def test_splits_compact_multi_role_lines(self) -> None:
        message = FakeMessage(
            """
            👍👍👍👍👍👍
            📍  Hudud: #Jizzax
            🚹 #Ayollar #Erkaklar
            🆔 Yosh: 18-35
            💲Oylik: Suhbat orqali
            ☎️ Tel: +998777075397; +998770390431

            🔍 Maishiy texnika ishlab chiqarish zavodi ishga taklif qiladi:

            Bo'sh ish o'rinlari:
            📍 Stanok operatori 📍 Qadoqlovchi
            📍 Yig'uvchi 📍 Yuklovchi 📍 Mexanik 📍 Elektrik
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(
            job.title,
            "Stanok operatori / Qadoqlovchi / Yig'uvchi / Yuklovchi / Mexanik +1",
        )
        self.assertEqual(job.company, "Maishiy texnika ishlab chiqarish zavodi")
