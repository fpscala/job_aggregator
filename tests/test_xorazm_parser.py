from __future__ import annotations

import unittest
from datetime import datetime, timezone
from types import SimpleNamespace

from telethon.tl.types import MessageEntityTextUrl

from job_aggregator.core.parser_base import ParserConfig
from job_aggregator.parsers.xorazm_ish.parser import Parser


class FakeMessage:
    def __init__(
        self,
        text: str,
        message_id: int = 1,
        entities: list[object] | None = None,
        contact_links: list[str] | None = None,
    ) -> None:
        self.raw_text = text
        self.message = text
        self.id = message_id
        self.date = datetime(2026, 3, 1, 10, 0, tzinfo=timezone.utc)
        self.chat = SimpleNamespace(username="Xorazm_ish")
        self.chat_id = -1001234567890
        self.entities = entities or []
        self.contact_links = contact_links


class XorazmParserTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.parser = Parser(config=ParserConfig(channel="Xorazm_ish", source="xorazm_ish"), plugin_name="xorazm_ish")

    def test_parses_single_role_post(self) -> None:
        message = FakeMessage(
            """
            #ish
            \"Lider\" parfyumeriya mahsulotlari kompaniyasiga sotuv menejerlari ishga taklif qilinadi.
            Talablar: xushmuomalalik, chaqqonlik.
            Oylik: 3 000 000 + bonus
            Manzil: Urganch shahri
            Murojaat uchun: +998901234567
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "sotuv menejerlari")
        self.assertEqual(job.company, "Lider")
        self.assertEqual(job.location, "Urganch shahri")
        self.assertEqual(job.salary, "3 000 000 + bonus")
        self.assertEqual(job.source, "xorazm_ish")
        self.assertEqual(job.url, "https://t.me/Xorazm_ish/1")
        self.assertIsNone(job.contact_links)

    def test_parses_multi_role_post(self) -> None:
        message = FakeMessage(
            """
            Avtosalonga quyidagi lavozimlarga ishga taklif qilinadi.
            ▪️ Kutib olish menejeri
            ▪️ Sotuv menejeri
            ▪️ Administrator
            Talablar: mas'uliyatlilik.
            Oylik: suhbat asosida kelishiladi
            Manzil: Urganch shahri
            Mo'ljal: Yoshlar markazi yonida
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Kutib olish menejeri / Sotuv menejeri / Administrator")
        self.assertEqual(job.company, "Avtosalon")
        self.assertEqual(job.location, "Urganch shahri (Yoshlar markazi yonida)")
        self.assertEqual(job.salary, "suhbat asosida kelishiladi")

    def test_extracts_branch_location_from_heading(self) -> None:
        message = FakeMessage(
            """
            #ish
            "HDP" O‘quv Markazining Shovot, Xonqa va Yangiariq filiallariga Ingliz tili fanidan Asosiy va yordamchi o'qituvchilar ishga taklif qilinadi.

            Talablar:
            • Tajribali bo'lishi kerak

            Oylik: O'quvchi boshidan to'lov, O'qituvchini salohiyatiga qarab.
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.company, "HDP")
        self.assertEqual(job.location, "Shovot, Xonqa va Yangiariq")
        self.assertEqual(job.title, "Ingliz tili fanidan Asosiy va yordamchi o'qituvchilar")

    def test_parses_russian_post(self) -> None:
        message = FakeMessage(
            """
            #ish
            Частный детский сад приглашает на работу женщин на должность.

            ▪️Воспитательница

            Общие требования:
            • Только женщины
            • Совершенное владение русским языком

            Рабочее время: 08:00 - 18:00
            Заработная плата согласовывается на основе собеседования

            Адрес: город Ургенч, улица Галаба, дом 11/1
            Ориентир: Электросеть г. Ургенч
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.company, "Частный детский сад")
        self.assertEqual(job.title, "Воспитательница")
        self.assertEqual(job.location, "город Ургенч, улица Галаба, дом 11/1 (Электросеть г. Ургенч)")
        self.assertEqual(job.salary, "согласовывается на основе собеседования")

    def test_skips_non_job_post(self) -> None:
        message = FakeMessage("Kanalimizga obuna bo'ling va do'stlaringizga ulashing")
        self.assertIsNone(self.parser.parse(message))

    def test_extracts_hidden_contact_link_and_skips_source_channel_link(self) -> None:
        text = """
            #ish
            Test kompaniyasiga operator ishga taklif qilinadi.
            Ariza topshirish:
            👤 Onlayn anketa
            """
        offset = text.index("Onlayn anketa")
        message = FakeMessage(
            text,
            entities=[
                MessageEntityTextUrl(offset=offset, length=len("Onlayn anketa"), url="https://forms.gle/example"),
                MessageEntityTextUrl(offset=0, length=4, url="https://t.me/Xorazm_ish"),
            ],
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.contact_links, ["https://forms.gle/example"])


if __name__ == "__main__":
    unittest.main()
