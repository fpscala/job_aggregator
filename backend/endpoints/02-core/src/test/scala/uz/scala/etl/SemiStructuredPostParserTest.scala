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

  pureTest("parseMany splits shared-body vacancy list from screenshot into three jobs") {
    val parsed =
      expectParsedMany(
        rawJob(
          source = "xorazm_ish_bor_elonlar",
          url = "https://t.me/Xorazm_ish_bor_elonlar/99999",
          description =
            """ISH BOR
              |📦 Bo'sh ish o'rinlari:
              |• Ofitsiant (yigit-qizlar)
              |• Uborchitsa
              |• Posuda moykachi
              |
              |🏢 Ish joyi: Sulton Maram restorani
              |📍 Manzil: Raysentr
              |🎯 Mo'ljal: Abbos apteka yonida
              |⏰ Ish vaqti: 11:00 - 23:00
              |💰 Ish haqi: Kunlik 150 000 so'm
              |
              |📋 Talablar:
              |• Chaqqon va xushmuomala
              |• Ish tajribasiga ega bo'lishi kerak
              |• Mas'uliyatli va tartibli
              |
              |📞 Bog'lanish:
              |📱 +998 95 984 55 00
              |
              |👉 @Xorazm_ish_bor_elonlar""".stripMargin
        )
      )

    val titles = parsed.map(_.parsed.title)

    expect.same(List("Ofitsiant (yigit-qizlar)", "Uborchitsa", "Posuda moykachi"), titles) &&
    expect(parsed.forall(candidate => candidate.parsed.company == "Sulton Maram restorani")) &&
    expect(parsed.forall(candidate => candidate.parsed.salary.exists(_.contains("Kunlik 150 000 so'm")))) &&
    expect(parsed.forall(candidate => candidate.parsed.location.contains("Raysentr (Abbos apteka yonida)"))) &&
    expect(parsed.forall(candidate => candidate.parsed.details.workSchedule.contains("11:00 - 23:00"))) &&
    expect(
      parsed.forall(
        _.parsed.details.requirements.contains(
          "Chaqqon va xushmuomala\nIsh tajribasiga ega bo'lishi kerak\nMas'uliyatli va tartibli"
        )
      )
    ) &&
    expect(parsed.forall(_.parsed.details.contactPhoneNumbers == List("+998959845500"))) &&
    expect(parsed.map(_.rawJob.url).zipWithIndex.forall { case (url, index) =>
      url.endsWith(s"#role-${index + 1}")
    })
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

  private def expectParsedMany(rawJob: RawJob): List[SemiStructuredPostParser.ParsedCandidate] =
    SemiStructuredPostParser.parseMany(rawJob) match {
      case Right(value) => value
      case Left(rejected) =>
        throw new AssertionError(s"expected parsed semi-structured jobs, got ${rejected.reason.code}")
    }

  private def expectRejected(rawJob: RawJob): StructuredPostParser.Rejected =
    SemiStructuredPostParser.parse(rawJob) match {
      case Left(value) => value
      case Right(parsed) =>
        throw new AssertionError(s"expected rejection, got parsed title=${parsed.title}")
    }
}
