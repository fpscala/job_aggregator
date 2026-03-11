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

    def test_parses_followup_role_blocks_in_multi_role_post(self) -> None:
        message = FakeMessage(
            """
            #ish
            ''Texno bozor'' jamoasiga quyidagi lavozimlarga ishga taklif qilinadi.

            ▪️ Brend Face
            Talablar:
            • Faqat ayol-qizlar
            • 18-30 yosh
            • O'zbek tilida ravon va chiroyli gapira olish
            • Kamera oldida erkin chiqish qila olish

            Qulayliklar:
            ✓ Ahil va professional jamoa
            ✓ Qulay ish muhiti

            Ish vaqti va oylik suhbat asosida kelishiladi

            ▪️Bosh buxgalter
            Talablar:
            • Buxgalteriya sohasida kamida 3-5 yil ish tajribasi
            • Retail (chakana savdo) sohasida ishlagan bo'lishi shart
            • 1C yoki boshqa buxgalteriya dasturlarida ishlay olish

            Qulayliklar:
            ✓ Barqaror ish o'rni
            ✓ Professional rivojlanish imkoniyati

            Ish vaqti: 6/1, 09:00 - 18:00
            Oylik suhbat asosida kelishiladi

            Manzil: Urganch shahri
            Tel: +998905589009
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Brend Face / Bosh buxgalter")
        self.assertEqual(job.company, "Texno bozor")
        self.assertEqual(job.location, "Urganch shahri")
        self.assertEqual(job.salary, "suhbat asosida kelishiladi")

    def test_splits_followup_role_blocks_into_multiple_jobs(self) -> None:
        message = FakeMessage(
            """
            #ish
            ''Texno bozor'' jamoasiga quyidagi lavozimlarga ishga taklif qilinadi.

            ▪️ Brend Face
            Talablar:
            • Faqat ayol-qizlar
            • 18-30 yosh
            • O'zbek tilida ravon va chiroyli gapira olish
            • Kamera oldida erkin chiqish qila olish

            Qulayliklar:
            ✓ Ahil va professional jamoa
            ✓ Qulay ish muhiti

            Ish vaqti va oylik suhbat asosida kelishiladi

            ▪️Bosh buxgalter
            Talablar:
            • Buxgalteriya sohasida kamida 3-5 yil ish tajribasi
            • Retail (chakana savdo) sohasida ishlagan bo'lishi shart
            • 1C yoki boshqa buxgalteriya dasturlarida ishlay olish

            Qulayliklar:
            ✓ Barqaror ish o'rni
            ✓ Professional rivojlanish imkoniyati

            Ish vaqti: 6/1, 09:00 - 18:00
            Oylik suhbat asosida kelishiladi

            Manzil: Urganch shahri
            Tel: +998905589009
            """
        )

        jobs = self.parser.parse_many(message)

        self.assertEqual(len(jobs), 2)
        self.assertEqual(jobs[0].title, "Brend Face")
        self.assertEqual(jobs[0].company, "Texno bozor")
        self.assertEqual(jobs[0].location, "Urganch shahri")
        self.assertIn("Brend Face", jobs[0].description)
        self.assertNotIn("Bosh buxgalter", jobs[0].description)
        self.assertTrue(jobs[0].url.endswith("#role-1"))

        self.assertEqual(jobs[1].title, "Bosh buxgalter")
        self.assertEqual(jobs[1].company, "Texno bozor")
        self.assertEqual(jobs[1].location, "Urganch shahri")
        self.assertIn("Bosh buxgalter", jobs[1].description)
        self.assertTrue(jobs[1].url.endswith("#role-2"))

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

    def test_preserves_full_heading_for_marketing_style_invitation(self) -> None:
        message = FakeMessage(
            """
            #ish
            "JASMIN Cake bakery" jamoasi kengayotganligi munosabati bilan qandolatchilar ishga taklif qilinadi.

            Talablar:
            • 20 yoshdan katta bolishi

            Ish vaqti: 09:00 - 19:00
            Oylik: (ishbay) 120.000 so'mdan boshlanadi

            Manzil: Urganch shahri
            Mo'ljal: Amina do'koni

            Tel: +998914316633
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(
            job.title,
            "\"JASMIN Cake bakery\" jamoasi kengayotganligi munosabati bilan qandolatchilar ishga taklif qilinadi",
        )
        self.assertEqual(job.company, "JASMIN Cake bakery")
        self.assertEqual(job.location, "Urganch shahri (Amina do'koni)")
        self.assertEqual(job.salary, "(ishbay) 120.000 so'mdan boshlanadi")

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
