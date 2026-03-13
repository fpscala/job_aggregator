package uz.scala.etl

import java.time.ZoneId
import java.time.ZonedDateTime

import weaver.SimpleIOSuite

import uz.scala.domain.events.RawJob

object SemiStructuredPostParserTest extends SimpleIOSuite {
  private val postedAt =
    ZonedDateTime.of(2026, 3, 13, 11, 0, 0, 0, ZoneId.of("Asia/Tashkent"))

  pureTest("parses real semi-structured post 40600 with combined work and application sections") {
    val parsed =
      expectParsed(
        rawJob(
          source = "xorazm_ish",
          url = "https://t.me/Xorazm_ish/40600",
          description =
            """#ish
              |''INFINITY IMPEX'' MCHJ Urganch filialiga ta'minotchilar ishga taklif qilinadi.
              |Kompaniya savdo va chet eldan mahsulot olib kelish bilan shug'ullanadi.
              |
              |Talablar:
              |• Faqat yigitlar
              |• 25-35 yosh
              |• Iqtisodiyot, marketing, logistika yoki shunga yaqin sohalarda oliy ma'lumotli bo'lishi
              |• Kompyuter va ofis dasturlarida ishlay olishi
              |• Savdk sohasida kamida 1 yillik tajribasi bo'lishi
              |• Xitoy tilini bilishi kerak. Rus va ingliz tillarini bilishi ustunlik beradi
              |
              |Ish vaqti va oylik suhbat asosida kelishiladi
              |✓ Xizmat safarlari
              |✓ Ofis va kerakli anjomlar
              |✓ Doimiy rivojlanish uchun treninglar
              |
              |Murojaat uchun:
              |👤 @infinityimpex1 (rezyumengizni shu manzilga yuboring)
              |
              |👉 @Xorazm_ish""".stripMargin
        )
      )

    expect.same("ta'minotchilar", parsed.title) &&
    expect.same("INFINITY IMPEX MCHJ Urganch filiali", parsed.company) &&
    expect.same(None, parsed.salary) &&
    expect.same(Some("Ish vaqti va oylik suhbat asosida kelishiladi"), parsed.details.workSchedule) &&
    expect.same(List("infinityimpex1"), parsed.details.contactTelegramUsernames) &&
    expect.same(Some("rezyumengizni shu manzilga yuboring"), parsed.details.contactText)
  }

  pureTest("parses real semi-structured post 40597 with salary continuation and telegram contact") {
    val parsed =
      expectParsed(
        rawJob(
          source = "xorazm_ish",
          url = "https://t.me/Xorazm_ish/40597",
          description =
            """#ish
              |Avikassa va tur firmaga ishchilar ishga taklif qilinadi.
              |
              |Talablar:
              |• Faqat yigitlar
              |• 1 yillil ish tajribasi bo'lishi
              |• Nutqi ravon va chiroyli bo'lishi
              |• Xushmuomala va mas'uliyatli bo'lishi kerak
              |• Rus tilini bilishi va oliy ma'lumotli bo'lishi ustunlik beradi
              |
              |Oylik suhbat asosida kelishiladi
              |Savdo hajmiga qarab belgilanadi
              |
              |Manzil: Urganch shahri
              |
              |Rezyumengizni quyidagi profilga yuboring:
              |👤 @shovshuvtravel
              |
              |👉 @Xorazm_ish""".stripMargin
        )
      )

    expect.same("ishchilar", parsed.title) &&
    expect.same("Avikassa va tur firma", parsed.company) &&
    expect.same(Some("suhbat asosida kelishiladi\nSavdo hajmiga qarab belgilanadi"), parsed.salary) &&
    expect.same(Some("Urganch shahri"), parsed.location) &&
    expect.same(List("shovshuvtravel"), parsed.details.contactTelegramUsernames) &&
    expect.same(Some("Rezyumengizni quyidagi profilga yuboring"), parsed.details.contactText)
  }

  pureTest("rejects real semi-structured vacancy list 27663 as multi-role") {
    val rejected =
      expectRejected(
        rawJob(
          source = "xorazm_ish_bor_elonlar",
          url = "https://t.me/Xorazm_ish_bor_elonlar/27663",
          description =
            """📣📣📣📣
              |RETPEN PROFILLARI
              |RETPEN oyna-eshik savdo offisiga ishga taklif qilamiz ✅
              |
              |💁🏻‍♂️ Bo’sh ish o’rinlari ⬇️
              | 1. Savdo bo’limi menedjeri 👨🏻‍💻
              | 2. Skladga ( Ombor mudiri ) 👷🏻‍♂️
              |
              |⚠️ Talablar
              |• Faqat erkaklar
              |
              |💰Oylik kelishuv asosida :
              |2 500 000 dan 5 000 000 gacha
              |
              |☎️ +998992126555
              |+998979639999
              |
              |👉 @Xorazm_ish_bor_elonlar""".stripMargin
        )
      )

    expect.same(StructuredPostParser.RejectionReason.MultipleRolesDetected, rejected.reason)
  }

  private def rawJob(
      description: String,
      source: String,
      url: String,
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
      contactLinks = None,
    )

  private def expectParsed(rawJob: RawJob): StructuredPostParser.Parsed =
    SemiStructuredPostParser.parse(rawJob) match {
      case Right(value) => value
      case Left(rejected) =>
        throw new AssertionError(s"expected parsed semi-structured post, got ${rejected.reason.code}")
    }

  private def expectRejected(rawJob: RawJob): StructuredPostParser.Rejected =
    SemiStructuredPostParser.parse(rawJob) match {
      case Left(value) => value
      case Right(parsed) =>
        throw new AssertionError(s"expected rejection, got parsed title=${parsed.title}")
    }
}
