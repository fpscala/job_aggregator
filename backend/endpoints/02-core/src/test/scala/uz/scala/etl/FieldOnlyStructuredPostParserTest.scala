package uz.scala.etl

import java.time.ZoneId
import java.time.ZonedDateTime

import weaver.SimpleIOSuite

import uz.scala.domain.events.RawJob

object FieldOnlyStructuredPostParserTest extends SimpleIOSuite {
  private val postedAt =
    ZonedDateTime.of(2026, 3, 14, 16, 0, 0, 0, ZoneId.of("Asia/Tashkent"))

  pureTest("parses field-only structured post without company") {
    val parsed =
      expectParsed(
        RawJob(
          title = "placeholder",
          company = None,
          description =
            """YANGI ISH
              |💼 Ish lavozimi: Yordamchi (natejnoy potolok sexiga)
              |
              |📍 Manzil: Urganch shahar, Olimpia stadion yonida
              |
              |⏰ Ish vaqti: 09:00 dan 19:00 gacha
              |
              |💰 Ish haqi: 3 000 000 so'm + premiyalar (ishga qarab)
              |
              |📝 Talablar:
              |• Chaqqon bo'lishi kerak
              |• Erkak kishi
              |• Yosh: 20-30 yosh
              |• Tajriba shart emas
              |
              |📞 Telefon: +998 91 994 91 11
              |
              |👉 @Xorazm_ish_bor_elonlar""".stripMargin,
          salary = None,
          location = None,
          source = "xorazm_ish_bor_elonlar",
          url = "https://t.me/Xorazm_ish_bor_elonlar/27841",
          postedAt = postedAt,
          contactLinks = None,
        )
      )

    expect.same("Yordamchi (natejnoy potolok sexiga)", parsed.title) &&
    expect.same(None, parsed.company) &&
    expect.same(Some("3 000 000 so'm + premiyalar (ishga qarab)"), parsed.salary) &&
    expect.same(Some("Urganch shahar, Olimpia stadion yonida"), parsed.location) &&
    expect.same(Some("09:00 dan 19:00 gacha"), parsed.details.workSchedule) &&
    expect.same(
      Some("Chaqqon bo'lishi kerak\nErkak kishi\nYosh: 20-30 yosh\nTajriba shart emas"),
      parsed.details.requirements,
    ) &&
    expect.same(List("+998919949111"), parsed.details.contactPhoneNumbers)
  }

  private def expectParsed(rawJob: RawJob): StructuredPostParser.Parsed =
    FieldOnlyStructuredPostParser.parse(rawJob) match {
      case Right(value) => value
      case Left(rejected) =>
        throw new AssertionError(s"expected parsed field-only structured post, got ${rejected.reason.code}")
    }
}
