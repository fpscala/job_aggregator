package uz.scala.etl

import java.time.ZoneId
import java.time.ZonedDateTime

import weaver.SimpleIOSuite

import uz.scala.domain.events.RawJob

object StructuredPostParserTest extends SimpleIOSuite {
  private val postedAt =
    ZonedDateTime.of(2026, 3, 12, 10, 0, 0, 0, ZoneId.of("Asia/Tashkent"))

  pureTest("parses real labeled structured post 26617") {
    val parsed =
      expectParsed(
        rawJob(
          source = "Xorazm_ish_bor_elonlar",
          url = "https://t.me/Xorazm_ish_bor_elonlar/26617",
          description =
            """📣📣📣📣
              |💼 Ish lavozimi: Kassir
              |
              |🏢 Ish joyi: “XON BOZOR”
              |
              |📍 Manzil: Urganch shahri, Xiva krug, “Xon Bozor” majmuasi
              |
              |👩‍💼 Talablar:
              |• Faqat 18–25 yosh oralig‘idagi qizlar qabul qilinadi
              |• Kompyuterda ishlash bo‘yicha boshlang‘ich ko‘nikmaga ega bo‘lishi kerak
              |• Xushmuomala va mas’uliyatli bo‘lishi kerak
              |• Hisob-kitobni bilishi zarur
              |• Talabalar bezoda qilmasin
              |
              |⏰ Ish vaqti: 09:00 dan 23:00 gacha
              |
              |💰 Ish haqi: Suhbat asosida
              |
              |📞 Bog‘lanish uchun:
              |📱 +998 97 666 44 22
              |
              |👉 @Xorazm_ish_bor_elonlar""".stripMargin
        )
      )

    expect.same("Kassir", parsed.title) &&
    expect.same(Some("XON BOZOR"), parsed.company) &&
    expect.same(Some("Suhbat asosida"), parsed.salary) &&
    expect.same(Some("Urganch shahri, Xiva krug, “Xon Bozor” majmuasi"), parsed.location) &&
    expect.same(Some("09:00 dan 23:00 gacha"), parsed.details.workSchedule) &&
    expect.same(List("+998976664422"), parsed.details.contactPhoneNumbers) &&
    expect.same(Nil, parsed.details.contactTelegramUsernames)
  }

  pureTest("parses real labeled structured post 26598 with region and workday labels") {
    val parsed =
      expectParsed(
        rawJob(
          source = "Xorazm_ish_bor_elonlar",
          url = "https://t.me/Xorazm_ish_bor_elonlar/26598",
          description =
            """📣📣📣📣
              |💼 Ish lavozimi: Savdo xodimi
              |
              |🏢 Ish beruvchi: PORT Foods MChJ
              |
              |📍 Hudud: Turtkul, Beruniy, Boston, Ellikkala
              |
              |⏰ Ish vaqti: 09:00 dan 17:00 gacha
              |🗓 Ish kuni: 7/6
              |
              |💰 Ish haqi: 6 000 000 – 10 000 000 so‘m
              |
              |📝 Talablar:
              |• Erkak va ayol nomzodlar
              |• Yosh: 18–35 yosh
              |• Savdo sohasida ishlagan bo‘lishi shart
              |• Shaxsiy avtomashina bo‘lsa — ustunlik
              |
              |📞 Bog‘lanish:
              |📱 93 587 95 25
              |
              |👉 @Xorazm_ish_bor_elonlar""".stripMargin
        )
      )

    expect.same("Savdo xodimi", parsed.title) &&
    expect.same(Some("PORT Foods MChJ"), parsed.company) &&
    expect.same(Some("Turtkul, Beruniy, Boston, Ellikkala"), parsed.location) &&
    expect.same(Some("09:00 dan 17:00 gacha\n7/6"), parsed.details.workSchedule) &&
    expect.same(Some("6 000 000 – 10 000 000 so'm"), parsed.salary) &&
    expect.same(List("935879525"), parsed.details.contactPhoneNumbers)
  }

  pureTest("parses real original structured post 40610 with intro company and separate role line") {
    val parsed =
      expectParsed(
        rawJob(
          source = "Xorazm_ish",
          url = "https://t.me/Xorazm_ish/40610",
          description =
            """#ish
              |"Candy Gold" konditer firmasiga quyidagi lavozimga ishga taklif qilinadi.
              |
              |▪️Savdo agenti
              |Talablar:
              |• Yigit-qizlar
              |• Kamida 6 oylik ish tajribasi bo'lishi
              |• Shaxsiy avtomobiliga ega bo'lishi
              |• Sotuv qobiliyatiga ega bo'lishi
              |• Muammolarni hal qila olishi
              |• Belgilangan hududda faol mijozlar bazasini yaratish
              |• Mas'uliyatli va chaqqon bo'lishi kerak
              |
              |Oylik: 4.000.000 - 7.000.000 so'm (fiksa + KPI + bonus)
              |
              |Manzil: Urganch shahri, Xonqa ko'chasi
              |Mo'ljal: Eco Metan
              |
              |❗️10:00 - 17:00 oralig'ida bog'laning
              |Tel: +998919120201
              |
              |👉 @Xorazm_ish""".stripMargin
        )
      )

    expect.same("Savdo agenti", parsed.title) &&
    expect.same(Some("Candy Gold konditer firmasi"), parsed.company) &&
    expect.same(Some("4.000.000 - 7.000.000 so'm (fiksa + KPI + bonus)"), parsed.salary) &&
    expect.same(Some("Urganch shahri, Xonqa ko'chasi (Eco Metan)"), parsed.location) &&
    expect.same(None, parsed.details.workSchedule) &&
    expect.same(List("+998919120201"), parsed.details.contactPhoneNumbers)
  }

  pureTest("parses real original structured post 40608 with inline title in intro line") {
    val parsed =
      expectParsed(
        rawJob(
          source = "Xorazm_ish",
          url = "https://t.me/Xorazm_ish/40608",
          description =
            """#ish
              |"JASMIN Cake bakery" jamoasi kengayotganligi munosabati bilan qandolatchilar ishga taklif qilinadi.
              |
              |Talablar:
              |• 20 yoshdan katta bolishi
              |• Shaxsiy fazilatlar: mas'uliyat, odoblilik, moslashuvchanlik
              |• Yashash joyi Urganch shaharda bo'lsa yaxshi
              |
              |Ish vaqti: 09:00 - 19:00
              |Oylik: (ishbay) 120.000 so'mdan boshlanadi
              |
              |Manzil: Urganch shahri
              |Mo'ljal: Amina do'koni
              |
              |Tel: +998914316633
              |
              |👉 @Xorazm_ish""".stripMargin
        )
      )

    expect.same("qandolatchilar", parsed.title) &&
    expect.same(Some("JASMIN Cake bakery jamoasi"), parsed.company) &&
    expect.same(Some("(ishbay) 120.000 so'mdan boshlanadi"), parsed.salary) &&
    expect.same(Some("Urganch shahri (Amina do'koni)"), parsed.location) &&
    expect.same(Some("09:00 - 19:00"), parsed.details.workSchedule) &&
    expect.same(List("+998914316633"), parsed.details.contactPhoneNumbers)
  }

  pureTest("parses normalized structured variant derived from real dataset pattern") {
    val parsed =
      expectParsed(
        rawJob(
          source = "Xorazm_ish",
          url = "https://t.me/Xorazm_ish/40421",
          description =
            """Savdo xodimi
              |Kompaniya: PORT Foods MChJ
              |Maosh: 6 000 000 - 10 000 000 so'm
              |Manzil: Turtkul, Beruniy, Boston, Ellikkala
              |Ish vaqti:
              |09:00 dan 17:00 gacha
              |7/6
              |Talablar:
              |- Erkak va ayol nomzodlar
              |- Yosh: 18-35 yosh
              |- Savdo sohasida ishlagan bo'lishi shart
              |- Shaxsiy avtomashina bo'lsa ustunlik
              |Telefon: 93 587 95 25""".stripMargin
        )
      )

    expect.same("Savdo xodimi", parsed.title) &&
    expect.same(Some("PORT Foods MChJ"), parsed.company) &&
    expect.same(Some("6 000 000 - 10 000 000 so'm"), parsed.salary) &&
    expect.same(Some("Turtkul, Beruniy, Boston, Ellikkala"), parsed.location) &&
    expect.same(Some("09:00 dan 17:00 gacha\n7/6"), parsed.details.workSchedule) &&
    expect.same(List("935879525"), parsed.details.contactPhoneNumbers)
  }

  pureTest("merges source-specific Ish haqida details for ishchi_bor_kerak_toshkent") {
    val parsed =
      expectParsed(
        rawJob(
          source = "ishchi_bor_kerak_toshkent",
          url = "https://t.me/ishchi_bor_kerak_toshkent/103614",
          description =
            """⚡️ MYFXBRO Trading kompaniyasiga call-markaz operatori kerak
              |
              |🏢 Kompaniya: MYFXBRO Trading kompaniyasi
              |
              |💵 Maosh: 8.000.000 - 20.000.000
              |📍 Hudud: Toshkent
              |
              |📣 Ish haqida:
              |– Mijozlar bilan savdo jarayonlarini olib borish
              |– Telefon orqali va bevosita sotuvni amalga oshirish
              |– Ravon gapirish va ishontirish orqali sotish
              |
              |❗️ Talablar:
              |– 18–35 yosh (qat’iy)
              |
              |⏰ Ish vaqti:
              |– 09:00 dan 18:00 gacha
              |
              |☎️ Bog‘lanish:
              |– +998901234567""".stripMargin,
        )
      )

    expect.same(Some("MYFXBRO Trading kompaniyasi"), parsed.company) &&
    expect.same(
      Some(
        "Mijozlar bilan savdo jarayonlarini olib borish\nTelefon orqali va bevosita sotuvni amalga oshirish\nRavon gapirish va ishontirish orqali sotish"
      ),
      parsed.details.responsibilities,
    ) &&
    expect(parsed.details.requirements.exists(_.contains("18"))) &&
    expect.same(List("+998901234567"), parsed.details.contactPhoneNumbers)
  }

  pureTest("keeps requirements clean when Bog'lanish starts a new section") {
    val parsed =
      expectParsed(
        rawJob(
          source = "Xorazm_ish_bor_elonlar",
          url = "https://t.me/Xorazm_ish_bor_elonlar/26599",
          description =
            """Operator
              |Kompaniya: Test Call Center
              |Talablar:
              |- Xushmuomala
              |- Mas'uliyatli
              |Bog‘lanish:
              |+998 90 123 45 67
              |@test_hr
              |Manzil: Urganch
              |Ish vaqti: 09:00-18:00""".stripMargin
        )
      )

    expect.same(Some("Xushmuomala\nMas'uliyatli"), parsed.details.requirements) &&
    expect.same(List("+998901234567"), parsed.details.contactPhoneNumbers) &&
    expect.same(List("test_hr"), parsed.details.contactTelegramUsernames) &&
    expect.same(None, parsed.details.contactText)
  }

  pureTest("rejects real multi-role structured post 40421") {
    val rejected =
      expectRejected(
        rawJob(
          source = "Xorazm_ish",
          url = "https://t.me/Xorazm_ish/40602",
          description =
            """#ish
              |"JASMIN Cake bakery" jamoasi kengayotganligi munosabati bilan quyidagi lavozimlarga ishga taklif qilinadi.
              |
              |▪️Savdo agenti
              |▪️Yuk tashuvchi (доставщик)
              |
              |Talablar:
              |• Yigit-qizlar
              |• 20-35 yosh
              |
              |Vazifalar:
              |• Belgilangan hududda joylashgan do'konlarga muntazam tashriflar.
              |
              |Oylik: 8.000.000 - 10.000.000 so'm (savdo agenti)
              |5.000.000 - 6.000.000 so'm (доставщик)
              |
              |Manzil: Urganch shahri
              |Mo'ljal: Amina do'koni
              |
              |Tel: +998912767070
              |
              |👉 @Xorazm_ish""".stripMargin
        )
      )

    expect.same(StructuredPostParser.RejectionReason.MultipleRolesDetected, rejected.reason)
  }

  pureTest("rejects real structured-like post 26599 without company") {
    val rejected =
      expectRejected(
        rawJob(
          description =
            """📣📣📣📣
              |💼 Ish lavozimi: Sotuvchi-Operator (Call center)
              |
              |• Talab qilinadi: qizlar
              |• Yosh: 20-30
              |
              |⏰ Ish vaqti: 14:00-22:00
              |
              |💰 Oylik maosh: 4 500 000 so‘m
              |
              |📝 Talablar:
              |• Doimiy ishchi kerak
              |• Muloqotga usta bo‘lishi
              |• Nutqi ravon, adabiy tilda to‘g‘ri gapira olishi
              |• Xushmuomala va mas’uliyatli bo‘lishi
              |• Kompyuterni bilishi
              |• Telefonda ilovalar bilan ishlashni tushunishi
              |
              |🍴 Sharoitlar:
              |• Bepul tushlik
              |• Qulay ish muhiti
              |
              |📍 Manzil: Urganch shahar
              |📞 Aloqa: +998 93 748 00 88
              |
              |👉 @Xorazm_ish_bor_elonlar""".stripMargin
        )
      )

    expect.same(StructuredPostParser.RejectionReason.MissingCompany, rejected.reason)
  }

  pureTest("rejects real structured-like post 40602 without phone") {
    val rejected =
      expectRejected(
        rawJob(
          description =
            """#ish
              |Stomatologiya klinikasiga reseption (adminstrator) ishga taklif qilinadi.
              |
              |Talablar:
              |• Faqat qizlar
              |• Ma'lumoti Oliy (yo'nalish ahamiyatga ega emas, lekin madaniyat va savodxonlik darajasi yuqori bo'lishi shart)
              |• Kompyuter (word, Excel) da ishlay olishi
              |• Sun'iy intelekt bilan ishlay olishi
              |• Mijozlar bilan muloqot qobiliyati bo'lishi
              |• Punktual va jamoa bilan ishlay olishi
              |• Xushmuomala va kirishimli bo'lishi kerak
              |
              |Vazifalar:
              |• Bemorlarni kutib olish va ro'yxatga olish
              |• Telefon qo'ng'iroqlariga javob berish va qabul vaqtlarini belgilash
              |• Klinika xizmatlari haqida ma'lumot berish
              |• Hujjatlar bilan ishlash
              |
              |Ish vaqti va oylik suhbat asosida kelishiladi
              |✓ Ahil jamoa
              |✓ Zamonaviy va shinam ofis
              |✓ Raqobatbardosh ish haqi
              |✓ Kasbiy rivojlanish imkoniyati
              |
              |Manzil: Urganch shahri
              |
              |Murojaat uchun:
              |👤 @FDC_HRBOT
              |
              |👉 @Xorazm_ish""".stripMargin
        )
      )

    expect.same(StructuredPostParser.RejectionReason.MissingPhone, rejected.reason)
  }

  pureTest("parses Russian-language structured post with Cyrillic labels") {
    val parsed = expectParsed(
      rawJob(
        source = "Xorazm_ish",
        url = "https://t.me/Xorazm_ish/40113",
        description =
          """#ish
            |Частный детский сад приглашает на работу женщин на должность.
            |
            |▪️Воспитательница
            |
            |Общие требования:
            |• Только женщины
            |• Совершенное владение русским языком
            |• Пунктуальная, дисциплинированная
            |
            |Рабочее время: 08:00 - 18:00.
            |Заработная плата согласовывается на основе собеседования
            |
            |Адрес: город Ургенч, улица Галаба, дом 11/1.
            |Ориентир: Электросеть г. Ургенч.
            |
            |Tел: +998997762555
            |
            |👉 @Xorazm_ish""".stripMargin,
      )
    )

    expect.same("Воспитательница", parsed.title) &&
    expect.same(Some("Частный детский сад"), parsed.company) &&
    expect.same(Some("согласовывается на основе собеседования"), parsed.salary) &&
    expect.same(
      Some("город Ургенч, улица Галаба, дом 11/1. (Электросеть г. Ургенч.)"),
      parsed.location,
    ) &&
    expect.same(Some("08:00 - 18:00."), parsed.details.workSchedule) &&
    expect.same(List("+998997762555"), parsed.details.contactPhoneNumbers)
  }

  pureTest("rejects Russian-language post that is missing a phone number") {
    val rejected = expectRejected(
      rawJob(
        source = "Xorazm_ish",
        url = "https://t.me/Xorazm_ish/40114",
        description =
          """#ish
            |Частный детский сад приглашает на работу женщин на должность.
            |
            |▪️Воспитательница
            |
            |Общие требования:
            |• Только женщины
            |• Совершенное владение русским языком
            |
            |Рабочее время: 08:00 - 18:00.
            |Заработная плата согласовывается на основе собеседования
            |
            |Адрес: город Ургенч, улица Галаба, дом 11/1.
            |Ориентир: Электросеть г. Ургенч.
            |
            |👉 @Xorazm_ish""".stripMargin,
      )
    )

    expect.same(StructuredPostParser.RejectionReason.MissingPhone, rejected.reason)
  }

  private def rawJob(
      description: String,
      contactLinks: Option[List[String]] = None,
      source: String = "structured_channel",
      url: String = "https://t.me/structured_channel/42",
    ): RawJob =
    RawJob(
      title = "placeholder",
      company = None,
      description = description,
      salary = None,
      location = None,
      source = source,
      url = url,
      postedAt = postedAt,
      contactLinks = contactLinks,
    )

  private def expectParsed(rawJob: RawJob): StructuredPostParser.Parsed =
    StructuredPostParser.parse(rawJob) match {
      case Right(value) => value
      case Left(rejected) =>
        throw new AssertionError(s"expected parsed structured post, got ${rejected.reason.code}")
    }

  private def expectRejected(rawJob: RawJob): StructuredPostParser.Rejected =
    StructuredPostParser.parse(rawJob) match {
      case Left(value) => value
      case Right(parsed) =>
        throw new AssertionError(s"expected rejection, got parsed title=${parsed.title}")
    }
}
