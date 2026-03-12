package uz.scala.etl

import java.time.ZoneId
import java.time.ZonedDateTime

import weaver.SimpleIOSuite

import uz.scala.domain.events.RawJob

object StructuredPostParserTest extends SimpleIOSuite {
  private val postedAt =
    ZonedDateTime.of(2026, 3, 12, 10, 0, 0, 0, ZoneId.of("Asia/Tashkent"))

  pureTest("parses ideal structured post") {
    val parsed =
      expectParsed(
        rawJob(
          description =
            """🔥 Ofitsiant
              |🏢 Kompaniya: Burger House
              |💰 Maosh: 3 000 000 so'm
              |📍 Manzil: Urganch
              |⏰ Ish vaqti: 09:00-18:00
              |Talablar:
              |- Tajriba
              |- Mas'uliyat
              |📞 Telefon: +998 90 123 45 67
              |📨 Murojaat:
              |- Onlayn anketa
              |@structured_channel""".stripMargin,
          contactLinks = Some(List("https://forms.gle/apply")),
        )
      )

    expect.same("Ofitsiant", parsed.title) &&
    expect.same("Burger House", parsed.company) &&
    expect.same(Some("3 000 000 so'm"), parsed.salary) &&
    expect.same(Some("Urganch"), parsed.location) &&
    expect.same(Some("09:00-18:00"), parsed.details.workSchedule) &&
    expect.same(Some("Tajriba\nMas'uliyat"), parsed.details.requirements) &&
    expect.same(Some("Onlayn anketa"), parsed.details.contactText) &&
    expect.same(List("+998901234567"), parsed.details.contactPhoneNumbers) &&
    expect.same(List("https://forms.gle/apply"), parsed.details.contactLinks)
  }

  pureTest("parses when label order changes") {
    val parsed =
      expectParsed(
        rawJob(
          description =
            """Barista
              |Telefon: 99890 555 44 33
              |Talablar:
              |• Xushmuomala
              |Kompaniya: Coffee Lab
              |Manzil: Urganch""".stripMargin
        )
      )

    expect.same("Barista", parsed.title) &&
    expect.same("Coffee Lab", parsed.company) &&
    expect.same(Some("Urganch"), parsed.location) &&
    expect.same(Some("Xushmuomala"), parsed.details.requirements) &&
    expect.same(List("+998905554433"), parsed.details.contactPhoneNumbers)
  }

  pureTest("keeps multiline requirements") {
    val parsed =
      expectParsed(
        rawJob(
          description =
            """Sotuvchi
              |Kompaniya: Optovik
              |Telefon:
              |+998 91 111 22 33
              |Maosh: 4 000 000 so'm
              |Talablar:
              |- Savdo tajribasi
              |- Rus tilini bilish
              |- Dam olish kunlari ishlay olish
              |Murojaat:
              |- Rezyumeni yuboring""".stripMargin
        )
      )

    expect.same(
      Some("Savdo tajribasi\nRus tilini bilish\nDam olish kunlari ishlay olish"),
      parsed.details.requirements,
    ) &&
    expect.same(Some("Rezyumeni yuboring"), parsed.details.contactText)
  }

  pureTest("accepts posts with only two optional fields present") {
    val parsed =
      expectParsed(
        rawJob(
          description =
            """Kassir
              |Kompaniya: Market
              |Telefon: +998901112233
              |Maosh: 3 500 000 so'm
              |Ish vaqti: 2/2 smena""".stripMargin
        )
      )

    expect.same(Some("3 500 000 so'm"), parsed.salary) &&
    expect.same(Some("2/2 smena"), parsed.details.workSchedule) &&
    expect.same(List("+998901112233"), parsed.details.contactPhoneNumbers)
  }

  pureTest("normalizes multiple phone formats") {
    val parsed =
      expectParsed(
        rawJob(
          description =
            """Operator
              |Kompaniya: Call Center
              |Telefon:
              |+998 (90) 123-45-67
              |99891 222 33 44
              |90 765 43 21
              |Manzil: Urganch
              |Ish vaqti: 09:00-18:00""".stripMargin
        )
      )

    expect.same(
      List("+998901234567", "+998912223344", "907654321"),
      parsed.details.contactPhoneNumbers,
    )
  }

  pureTest("starts a new contact section at Bog'lanish and keeps requirements clean") {
    val originalPost =
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

    val parsed = expectParsed(rawJob(description = originalPost))

    expect.same(Some("Xushmuomala\nMas'uliyatli"), parsed.details.requirements) &&
    expect.same(List("+998901234567"), parsed.details.contactPhoneNumbers) &&
    expect.same(List("test_hr"), parsed.details.contactTelegramUsernames) &&
    expect.same(None, parsed.details.contactText)
  }

  pureTest("rejects non structured posts") {
    val rejected =
      expectRejected(
        rawJob(
          description =
            """Burger House jamoasiga ofitsiant kerak.
              |Ish tajribasi bo'lsa yaxshi.
              |Bog'lanish uchun +998901234567""".stripMargin
        )
      )

    expect.same(StructuredPostParser.RejectionReason.MissingStructuredLabels, rejected.reason)
  }

  pureTest("rejects multi role titles") {
    val rejected =
      expectRejected(
        rawJob(
          description =
            """Ofitsiant / Kassir
              |Kompaniya: Burger House
              |Telefon: +998901234567
              |Manzil: Urganch
              |Ish vaqti: 09:00-18:00""".stripMargin
        )
      )

    expect.same(StructuredPostParser.RejectionReason.MultipleRolesDetected, rejected.reason)
  }

  pureTest("rejects posts without company") {
    val rejected =
      expectRejected(
        rawJob(
          description =
            """Ofitsiant
              |Telefon: +998901234567
              |Maosh: 3 000 000 so'm
              |Manzil: Urganch""".stripMargin
        )
      )

    expect.same(StructuredPostParser.RejectionReason.MissingCompany, rejected.reason)
  }

  pureTest("rejects posts without phone") {
    val rejected =
      expectRejected(
        rawJob(
          description =
            """Ofitsiant
              |Kompaniya: Burger House
              |Maosh: 3 000 000 so'm
              |Manzil: Urganch
              |Talablar:
              |- Tajriba""".stripMargin
        )
      )

    expect.same(StructuredPostParser.RejectionReason.MissingPhone, rejected.reason)
  }

  private def rawJob(
      description: String,
      contactLinks: Option[List[String]] = None,
    ): RawJob =
    RawJob(
      title = "placeholder",
      company = None,
      description = description,
      salary = None,
      location = None,
      source = "structured_channel",
      url = "https://t.me/structured_channel/42",
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
