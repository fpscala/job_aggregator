from __future__ import annotations

import unittest
from datetime import datetime, timezone
from types import SimpleNamespace

from job_aggregator.core.parser_base import ParserConfig
from job_aggregator.parsers.ishbor_toshkent_kerak_ishchi_bor.parser import Parser


class FakeMessage:
    def __init__(self, text: str, message_id: int = 1, contact_links: list[str] | None = None) -> None:
        self.raw_text = text
        self.message = text
        self.id = message_id
        self.date = datetime(2026, 3, 17, 10, 0, tzinfo=timezone.utc)
        self.chat = SimpleNamespace(username="Ishbor_Toshkent_kerak_ishchi_bor")
        self.chat_id = -1001234567890
        self.entities: list[object] = []
        self.contact_links = contact_links


class IshborToshkentKerakIshchiBorParserTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.parser = Parser(
            config=ParserConfig(
                channel="Ishbor_Toshkent_kerak_ishchi_bor",
                source="ishbor_toshkent_kerak_ishchi_bor",
            ),
            plugin_name="ishbor_toshkent_kerak_ishchi_bor",
        )

    def test_parses_yandex_eda_registration_post(self) -> None:
        message = FakeMessage(
            """
            🍔Yandex.Eda kompaniyasi kuryerlarni ishga taklif qiladi!

            🚀 Erkin ish jadvali, yuqori daromad va kunlik to‘lov!
            📢 Bo‘sh ish o‘rni: Kuryer

            📊 Yosh: 18+
            💵 Daromad: 5 000 000 – 15 000 000 so‘mgacha
            ✅ To‘lov: Kunlik!

            ⏰ Ish vaqti: O‘zingiz xohlagan paytda ishlang!

            📍 Qulay hududni o‘zingiz belgilaysiz!

            📲 Ro‘yxatdan o‘tish uchun havola:
            🔗 Ro‘yxatdan o‘tish
            """,
            contact_links=["https://2go.pro/DNuQ"],
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Kuryer")
        self.assertEqual(job.company, "Yandex.Eda kompaniyasi")
        self.assertEqual(job.salary, "5 000 000 – 15 000 000 so'mgacha")
        self.assertEqual(job.location, "Qulay hududni o'zingiz belgilaysiz")
        self.assertEqual(job.contact_links, ["https://2go.pro/DNuQ"])

    def test_parses_freeform_salary_and_address_lines(self) -> None:
        message = FakeMessage(
            """
            Ko'chada savdo qilish
            Ish vaqti 12 soat
            Ish haqqi haftalik yoki kunlik, kunlik ish xaqqi 170.000 so'm yahwi iwlasa qo'wib beriladi
            Adres chilonzor shuhrat astanovkasi
            Qogan ma'lumotla telda gaplawiladi
            +998932985566
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Ko'chada savdo qilish")
        self.assertEqual(job.salary, "haftalik yoki kunlik, kunlik ish xaqqi 170.000 so'm yahwi iwlasa qo'wib beriladi")
        self.assertEqual(job.location, "chilonzor shuhrat astanovkasi")

    def test_blocks_mlm_style_online_income_posts(self) -> None:
        message = FakeMessage(
            """
            🅰SSALOMU ALAYKUM YURTDOSHLAR!!!

            👩‍💻 Men sizni qonuniy halol va eng muhimi uyda o‘tirgan holda ishlashga taklif qilaman!
            💯 ONLINE DAROMAD
            ❌ Faberlic emas
            ❌ Oriflame emas
            """
        )

        self.assertIsNone(self.parser.parse(message))
