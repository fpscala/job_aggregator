package uz.scala.etl

import java.time.ZoneId
import java.time.ZonedDateTime

import weaver.SimpleIOSuite

import uz.scala.domain.events.RawJob

object SourceJobEtlsTest extends SimpleIOSuite {
  private val postedAt =
    ZonedDateTime.of(2026, 3, 17, 10, 0, 0, 0, ZoneId.of("Asia/Tashkent"))

  pureTest("dispatches ishchi_bor_kerak_toshkent through source-specific etl") {
    val rawJob =
      RawJob(
        title = "call-markaz operatori",
        company = Some("MYFXBRO Trading kompaniyasi"),
        description =
          """⚡ MYFXBRO Trading kompaniyasiga call-markaz operatori kerak
            |
            |❗ Talablar:
            |– Chiroyli ovoz va korinishga ega bolish
            |
            |⏰ Ish vaqti:
            |– 09:00 dan 20:00 gacha
            |
            |☎ Bog'lanish:
            |– +998903187572
            |– @ziyodamamatova""".stripMargin,
        salary = Some("8.000.000 - 20.000.000"),
        location = Some("Toshkent shahar, Chilonzor tumani"),
        source = "ishchi_bor_kerak_toshkent",
        url = "https://t.me/ishchi_bor_kerak_toshkent/103623",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = SourceJobEtls.enrich(rawJob)

    expect.same(Some("– Chiroyli ovoz va korinishga ega bolish"), details.requirements) &&
    expect.same(Some("– 09:00 dan 20:00 gacha"), details.workSchedule) &&
    expect.same(List("+998903187572"), details.contactPhoneNumbers) &&
    expect.same(List("ziyodamamatova"), details.contactTelegramUsernames)
  }

  pureTest("dispatches ishbor_toshkent_kerak_ishchi_bor through source-specific etl") {
    val rawJob =
      RawJob(
        title = "Kuryer",
        company = Some("Yandex.Eda kompaniyasi"),
        description =
          """🍔Yandex.Eda kompaniyasi kuryerlarni ishga taklif qiladi!
            |
            |💵 Daromad: 5 000 000 – 15 000 000 so'mgacha
            |⏰ Ish vaqti: O'zingiz xohlagan paytda ishlang!
            |📲 Ro'yxatdan o'tish uchun havola:""".stripMargin,
        salary = Some("5 000 000 – 15 000 000 so'mgacha"),
        location = Some("Qulay hududni o'zingiz belgilaysiz"),
        source = "ishbor_toshkent_kerak_ishchi_bor",
        url = "https://t.me/Ishbor_Toshkent_kerak_ishchi_bor/3687",
        postedAt = postedAt,
        contactLinks = Some(List("https://2go.pro/DNuQ")),
      )

    val details = SourceJobEtls.enrich(rawJob)

    expect.same(None, details.workSchedule) &&
    expect.same(List("https://2go.pro/DNuQ"), details.contactLinks) &&
    expect(details.hasContacts)
  }
}
