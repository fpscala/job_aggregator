package uz.scala.etl

import java.time.ZoneId
import java.time.ZonedDateTime

import weaver.SimpleIOSuite

import uz.scala.domain.events.RawJob

object NarrativeRecruitmentPostParserTest extends SimpleIOSuite {
  private val postedAt =
    ZonedDateTime.of(2026, 3, 14, 10, 0, 0, 0, ZoneId.of("Asia/Tashkent"))

  pureTest("parses narrative recruitment post with salary ladder and benefits") {
    val parsed =
      expectParsed(
        RawJob(
          title = "Ishchilar",
          company = None,
          description =
            """#ish
              |"ISTIQLOL LAZZATI" Qandolatchilik fabrikasi.
              |Jamoamizga qo'shilishni xoxlaysizmi ?
              |
              |Sizni jamoamizda ko'rsak xursand bo'lamiz.
              |Faqat jamoamizga qo'shilish talablari bor.
              |• Qizlar: 18-30 yosh
              |• Yigitlar: 18-30 yoshdan qabul qilinadi.
              |• Mas'uliyatli va chaqqon bo'lishi
              |
              |Oylik maoshlar razryadga qarab belgilanadi:
              |3-razryad (Yangi ishga qabul qilinganlar uchun)
              |Oylik: 2.500.000 - 3.000.000 so'm
              |2-razryad: 2.600.000 - 3.500.000 so'm
              |1-razryad: 3.500.000 - 4.500.000 so'm
              |Oliy razryad: 5.000.000 - 10.000.000 so'mgacha.
              |
              |❗️Oy davomida ishdan qolmay, muntazam ravishda ishlagan har bir xodimga premiyalar beriladi.
              |✓ Tushlik 30 kunga mo'ljallangan 30 xil ovqatlar (korxona hisobidan)
              |
              |Manzil: Urganch shahar
              |Mo'ljal: "Indaba" kafesi yon tomoni
              |
              |Tel: +998886000066
              |
              |👉 @Xorazm_ish""".stripMargin,
          salary = None,
          location = None,
          source = "xorazm_ish",
          url = "https://t.me/Xorazm_ish/40580",
          postedAt = postedAt,
          contactLinks = None,
        )
      )

    expect.same("Ishchilar", parsed.title) &&
    expect.same(Some("ISTIQLOL LAZZATI Qandolatchilik fabrikasi."), parsed.company) &&
    expect.same(
      Some(
        "Oylik maoshlar razryadga qarab belgilanadi:\n3-razryad (Yangi ishga qabul qilinganlar uchun)\nOylik: 2.500.000 - 3.000.000 so'm\n2-razryad: 2.600.000 - 3.500.000 so'm\n1-razryad: 3.500.000 - 4.500.000 so'm\nOliy razryad: 5.000.000 - 10.000.000 so'mgacha."
      ),
      parsed.salary,
    ) &&
    expect.same(
      Some("Urganch shahar (\"Indaba\" kafesi yon tomoni)"),
      parsed.location,
    ) &&
    expect.same(
      Some("Qizlar: 18-30 yosh\nYigitlar: 18-30 yoshdan qabul qilinadi\nMas'uliyatli va chaqqon bo'lishi"),
      parsed.details.requirements,
    ) &&
    expect.same(
      Some(
        "Oy davomida ishdan qolmay, muntazam ravishda ishlagan har bir xodimga premiyalar beriladi.\nTushlik 30 kunga mo'ljallangan 30 xil ovqatlar (korxona hisobidan)"
      ),
      parsed.details.benefits,
    ) &&
    expect.same(List("+998886000066"), parsed.details.contactPhoneNumbers)
  }

  private def expectParsed(rawJob: RawJob): StructuredPostParser.Parsed =
    NarrativeRecruitmentPostParser.parse(rawJob) match {
      case Right(value) => value
      case Left(rejected) =>
        throw new AssertionError(s"expected parsed narrative recruitment post, got ${rejected.reason.code}")
    }
}
