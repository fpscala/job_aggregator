from __future__ import annotations

import unittest
from datetime import datetime, timezone
from types import SimpleNamespace

from telethon.tl.types import MessageEntityTextUrl

from job_aggregator.core.parser_base import ParserConfig
from job_aggregator.parsers.xorazm_ish_bor_elonlar.parser import Parser


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
        self.chat = SimpleNamespace(username="Xorazm_ish_bor_elonlar")
        self.chat_id = -1001234567890
        self.entities = entities or []
        self.contact_links = contact_links


class XorazmIshBorElonlarParserTestCase(unittest.TestCase):
    def setUp(self) -> None:
        self.parser = Parser(
            config=ParserConfig(channel="Xorazm_ish_bor_elonlar", source="xorazm_ish_bor_elonlar"),
            plugin_name="xorazm_ish_bor_elonlar",
        )

    def test_parses_structured_single_role_post(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            💼 Ish lavozimi: Savdo agenti (3 ta o‘rin)

            🏢 Ish beruvchi: Parfyumeriya va yuvish vositalari savdosi (200 dan ortiq assortiment)

            📍 Manzil: Urganch shahri, Olimpiya stadion atrofida
            ⏰ Ish vaqti: 09:00 - 18:00
            💰 Maosh: 3 500 000 so‘mdan 7 000 000 so‘mgacha

            📞 Aloqa uchun: +998 93 467 50 01
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Savdo agenti (3 ta o‘rin)")
        self.assertEqual(job.company, "Parfyumeriya va yuvish vositalari savdosi (200 dan ortiq assortiment)")
        self.assertEqual(job.location, "Urganch shahri, Olimpiya stadion atrofida")
        self.assertEqual(job.salary, "3 500 000 so‘mdan 7 000 000 so‘mgacha")

    def test_parses_structured_multi_role_post(self) -> None:
        message = FakeMessage(
            """
            💼 Ish lavozimlari:
            • Xodim (yigitlar)
            • Kassir qiz
            • Ofitsiant qizlar

            🏢 Ish beruvchi: UNNA KAFE
            📍 Manzil: Urganch shahri, 8-blok

            💰 Ish haqi:
            • Yigitlar: kunlik 150 000 - 250 000 so‘m
            • Kassir qiz: kunlik 100 000 - 150 000 so‘m

            📞 Telefon:
            +998 91 279 71 76
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Xodim (yigitlar) / Kassir qiz / Ofitsiant qizlar")
        self.assertEqual(job.company, "UNNA KAFE")
        self.assertEqual(job.location, "Urganch shahri, 8-blok")

    def test_parses_promo_heading_with_bulleted_roles(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            Ish qidiryapsizmi? Olimp restoraniga qo‘shiling - ahil jamoa va yuqori maosh sizni kutmoqda

            ▫️Sotuv menejeri
            ▫️Ofitsant
            ▫️Call sentra operator

            🕰️ Ish vaqti: 12 soat
            💰OYLIK MAOSH: 3 mlndan 15 mlngacha
            🚗MANZIL: Urganch Roysentr

            @Olimp_kafe_bot
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Sotuv menejeri / Ofitsant / Call sentra operator")
        self.assertEqual(job.location, "Urganch Roysentr")
        self.assertEqual(job.salary, "3 mlndan 15 mlngacha")

    def test_parses_freeform_company_role_heading(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            Distributsiya bilan shug'ullanadigan korxonaga tajribali buxgalter ishga taflik qilinadi.

            • Ish vaqti: 6/1, 09:00-18:00
            • Maosh: 6 000 000-10 000 000 so'm
            🗺Manzil: Urganch shahri, "Xonqa yo'li"

            📞 Aloqa: @HRxodimagency rezyume bilan murojaat qiling
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Distributsiya bilan shug'ullanadigan korxonaga tajribali buxgalter ishga taflik qilinadi")
        self.assertEqual(job.location, 'Urganch shahri, "Xonqa yo\'li"')
        self.assertEqual(job.salary, "6 000 000-10 000 000 so'm")

    def test_parses_invitation_after_leading_meta_lines(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            📍 Hudud: #Xorazm Bog'ot tumani
            🚹 #Ayollar #Erkaklar
            🆔 Yosh: 18 dan 35 yoshgacha
            💲Oylik: Suhbat orqali

            🔍GARANT SAVDO MARKAZI
            ishga taklif qiladi

            🔸Marketing (Shaxsiy avtomobili bilan)
            🔸Shartnoma bo'limi
            🔸Yuk tashuvchi

            📍 Manzil: Xorazm viloyati Bog'ot tumani O'zbekiston ko'chasi
            🕹 Mo'ljal: Tuman hokimligi yonida
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Marketing (Shaxsiy avtomobili bilan) / Shartnoma bo'limi / Yuk tashuvchi")
        self.assertEqual(job.location, "Xorazm viloyati Bog'ot tumani O'zbekiston ko'chasi (Tuman hokimligi yonida)")

    def test_parses_followup_role_blocks_in_multi_role_post(self) -> None:
        message = FakeMessage(
            """
            #ish
            "Texno bozor" jamoasiga quyidagi lavozimlarga ishga taklif qilinadi.

            ▪ Brend Face
            Talablar:
            • Faqat ayol-qizlar
            • 18-30 yosh
            • O'zbek tilida ravon va chiroyli gapira olish
            • Kamera oldida erkin chiqish qila olish

            Qulayliklar:
            ✓ Ahil va professional jamoa
            ✓ Qulay ish muhiti

            Ish vaqti va oylik suhbat asosida kelishiladi

            ▪ Bosh buxgalter
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

    def test_parses_department_invitation_without_falling_back_to_requirements(self) -> None:
        message = FakeMessage(
            """
            🔉 ISHONCH do'konlar tarmog'i Sizga bo'sh ish o'rinlarini taklif etadi.

            Urganch filialiga:

            🧩 Mijozlar bilan ishlash bo'limiga ishga taklif etadi

            ISH HAQI - 8 mlndan 15 mlngacha

            Talablar:
            ✅ Qarzdorliklarni undirish sohasida ish tajriba
            ✅ Bank yoki MIB sohasida kamida 1 yillik mehnat faoliyati ustunlik beradi

            Tel: +998991114090
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Mijozlar bilan ishlash bo'limi")
        self.assertEqual(job.company, "ISHONCH do'konlar tarmog'i")
        self.assertEqual(job.location, "Urganch")
        self.assertEqual(job.salary, "8 mlndan 15 mlngacha")

    def test_cleans_company_suffix_and_trailing_role_emojis(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            RETPEN PROFILLARI
            RETPEN oyna-eshik savdo offisiga ishga taklif qilamiz ✅

            💁🏻‍♂ Bo'sh ish o'rinlari ⬇
            1. Savdo bo'limi menedjeri 👨🏻‍💻
            2. Skladga ( Ombor mudiri ) 👷🏻‍♂

            💰 Oylik kelishuv asosida:
            2 500 000 dan 5 000 000 gacha

            📍 Manzil: Urganch shahar, P.Mahmud ko'chasi 6/1-A uy
            Mo'ljal: Sud med ekspertiza ro'parasi
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Savdo bo'limi menedjeri / Sklad ( Ombor mudiri )")
        self.assertEqual(job.company, "RETPEN oyna-eshik savdo offisi")
        self.assertEqual(job.location, "Urganch shahar, P.Mahmud ko'chasi 6/1-A uy (Sud med ekspertiza ro'parasi)")
        self.assertEqual(job.salary, "kelishuv asosida; 2 500 000 dan 5 000 000 gacha")

    def test_salary_does_not_swallow_notes_or_phone(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            📢 ИШГА ТАКЛИФ!

            🏢 Вакансиялар: Хужалик ишларига
            Иш вакти: (душанба-шанба) (09:00 - 17:00)

            Талаблар:
            ✔ (фақат йигитлар)
            ✔ Маъсулиятни ҳис қилиш к.
            ✔ Ёш чегараси (30) ёшгача
            💰 Маош: 2.5 млн сўмдан бошланади

            👉 Фақат Урганч шаҳри ёки Урганч туманида яшовчилар учун!

            📞 +99877 795 7759
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Хужалик ишлари")
        self.assertEqual(job.salary, "2.5 млн сўмдан бошланади")

    def test_blank_address_falls_back_to_landmark_without_truncating_company(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            💼 Ish lavozimi: Tozalik xodimi (sanitarka)

            🏢 Ish beruvchi: Ishonchmed klinika

            📍 Manzil:
            🎯 Mo‘ljal: Media Park

            ⏰ Ish vaqti: 08:00 – 17:00

            💰 Oylik maoshi: 1 300 000 so‘m
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.company, "Ishonchmed klinika")
        self.assertEqual(job.location, "Media Park")
        self.assertEqual(job.salary, "1 300 000 so‘m")

    def test_preserves_heading_when_invitation_has_no_clear_role(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            🔹 Urganch shaxridagi avto exiyot qismlari do'koniga yigitlarni ishga taklif qilamiz

            📌 Faqat Urganch shaxridan ishga qabul qilamiz
            • Xushmuomala va mijozlar bilan muloyim muloqot qila oladigan bo‘lishi
            • Oldin savdo sohasida ishlagan bo'lishi
            • Jamoa bilan ishlay olishi va topshiriqlarni vaqtida bajarishi kerak
            • Shu soxa bo’yicha tajriba bo’lishi (+6 oy-1 yil)

            🔹 Yoshi: 18 yoshdan 30 yoshgacha

            💰 Oylik maoshi: Kelishilgan xolda
            📞 +998999640055

            📍 Manzil: Urganch shaxri
            📍 Mo'ljal: Avto extiyot qismlari bozori (запчаст бозор)
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Urganch shaxridagi avto exiyot qismlari do'koniga yigitlarni ishga taklif qilamiz")
        self.assertIsNone(job.company)
        self.assertEqual(job.location, "Urganch shaxri (Avto extiyot qismlari bozori (запчаст бозор))")
        self.assertEqual(job.salary, "Kelishilgan xolda")

    def test_inline_company_keeps_full_klinika_word(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            💼 Ish lavozimi: Oshpaz (qizlar)

            🏢 Ish joyi: Klinika

            ⏰ Ish vaqti: 3 mahal ovqat tayyorlash
            💰 Ish haqi: 3 000 000 so‘m
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.company, "Klinika")

    def test_keeps_full_heading_for_sentence_style_post(self) -> None:
        message = FakeMessage(
            """
            "Dr Omon Premium" stomatologiya markaziga sanitar xodimi ishga taklif qilinadi

            • Ish vaqti: 6/1, 08:00-17:00
            • Maosh: 2 000 000 so'm

            🗺 Manzil: Urganch shahri, Markaziy dehkon bozor yoni
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "\"Dr Omon Premium\" stomatologiya markaziga sanitar xodimi ishga taklif qilinadi")
        self.assertEqual(job.company, "Dr Omon Premium")
        self.assertEqual(job.location, "Urganch shahri, Markaziy dehkon bozor yoni")
        self.assertEqual(job.salary, "2 000 000 so'm")

    def test_preserves_full_heading_when_role_is_not_clear(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            SH&B Murskoy kiyim dokiniga ishga taklif qilinadi

            ⏰ISH VAQTI: 13:00dan 00:00gaca
            💰OYLIK MAOSH: kunlik 80:000. 130:000 ming
            📍MANZIL: Stadion tog'risi, bir zumdaning yoninda
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "SH&B Murskoy kiyim dokiniga ishga taklif qilinadi")
        self.assertEqual(job.location, "Stadion tog'risi, bir zumdaning yoninda")

    def test_skips_non_job_course_ad(self) -> None:
        message = FakeMessage("💼 Taklif: Savdo vakillari (agentlar) tayyorlash o‘quv kursi")
        self.assertIsNone(self.parser.parse(message))

    def test_skips_non_job_sale_ad(self) -> None:
        message = FakeMessage("🧊 Музқаймоқ аппарати сотилади!\n📞 Телефон: +998941898686")
        self.assertIsNone(self.parser.parse(message))

    def test_parses_cyrillic_invitation_post(self) -> None:
        message = FakeMessage(
            """
            📣📣📣📣
            - КРЕМБЕР Кандолат махсулотлари Фирмасига Турткуль Беруний Бустон буйича иш тажрибасига эга СУПЕРВАЙЗЕР ишга таклиф килади!

            • Ойлиги(юкори) сухбатдан кейин, фикса+бонус
            Тел: +998930950076
            Манзил: Турткуль Шахри Д.Матчанов 9
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "КРЕМБЕР Кандолат махсулотлари Фирмасига Турткуль Беруний Бустон буйича иш тажрибасига эга СУПЕРВАЙЗЕР ишга таклиф килади")
        self.assertEqual(job.location, "Турткуль Шахри Д.Матчанов 9")

    def test_parses_english_hiring_post(self) -> None:
        message = FakeMessage(
            """
            📢 We Are Hiring Full-Time Support Teachers!

            ✅ Requirements:
            • English proficiency 6+
            • Knowledge of Russian

            📩 Contact: @uprofilialrahbari
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "We Are Hiring Full-Time Support Teachers")

    def test_keeps_hidden_form_link(self) -> None:
        text = """
            Xususiy firmaga, quyidagi lavozimlar bo'yicha ishga taklif qilamiz !!!

            📲 Savdo bo'limi operatori
            📝 Xo'jalik bo'limi xodimi

            Anketa to'ldirish
            """
        offset = text.index("Anketa to'ldirish")
        message = FakeMessage(
            text,
            entities=[MessageEntityTextUrl(offset=offset, length=len("Anketa to'ldirish"), url="https://forms.gle/example")],
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Savdo bo'limi operatori / Xo'jalik bo'limi xodimi")
        self.assertEqual(job.contact_links, ["https://forms.gle/example"])

    def test_prefers_summary_title_for_marketing_skill_list_post(self) -> None:
        message = FakeMessage(
            """
            YANGI ISH
            ***YANGI***Gold Restoranga
            • POVR PROFESSIONAL 7-10
            • Turetski kuhnya
            • Tandirda Lavash avgan non
            • Gosht steak
            • Barista cofe maxito koktell
            • Salat opitni
            • Desert
            Tayyorlab biladigan povrlarga ish bor ✅

            Paspurt
            Med qnishka
            Yashash joyi Urganch shahar

            💰 OYLIK MAOSH:
            POVR ***150-300 som***

            📍 MANZIL: GOLD Resto bar
            🎯 MO'LJAL: oblasnoy Raddom
            """
        )

        job = self.parser.parse(message)

        self.assertIsNotNone(job)
        assert job is not None
        self.assertEqual(job.title, "Gold Restoranga Tayyorlab biladigan povrlarga ish bor")
        self.assertEqual(job.company, "Gold Restoran")
        self.assertEqual(job.location, "GOLD Resto bar (oblasnoy Raddom)")
        self.assertEqual(job.salary, "150-300 som")


if __name__ == "__main__":
    unittest.main()
