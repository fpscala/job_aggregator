package uz.scala.etl.sources

import java.time.ZoneId
import java.time.ZonedDateTime

import weaver.SimpleIOSuite

import uz.scala.domain.events.RawJob

object IshchiBorKerakToshkentSourceJobEtlTest extends SimpleIOSuite {
  private val postedAt =
    ZonedDateTime.of(2026, 3, 18, 10, 0, 0, 0, ZoneId.of("Asia/Tashkent"))

  pureTest("classifies Ish haqida block without leaking promo noise") {
    val rawJob =
      RawJob(
        title = "Sotuvchi menejer",
        company = Some("Consulting firma"),
        description =
          """⚡️ SOTUVCHI MENEDJER (call centerda ishlagan yoki ishlayotgan Sotuvchi operator)
            |Consulting firma sotuv bo'limaga call sentr mutaxasislari sotuv menejerlari olamiz
            |
            |💵 Maosh: 6 000 000 – 20 000 000 so'm
            |📍 Hudud: Toshkent
            |
            |📣 Ish haqida:
            |– Mijozlar bilan savdo jarayonlarini olib borish
            |– Telefon orqali va bevosita sotuvni amalga oshirish
            |– Ravon gapirish va ishontirish orqali sotish
            |– Yosh: 18-35
            |
            |❗️ Talablar:
            |– 18–35 yosh (qat’iy)
            |– Ish tajribasi call center va sotuvda 2 oy va undan yuqori
            |– Mas’uliyatli va faol bo‘lish
            |
            |⏰ Ish vaqti:
            |– 6/1
            |– 9:00-18:00 (ish vaqti qat’iy)
            |
            |✅ Biz taklif qilamiz:
            |– Shinam ofis
            |– Yuqori oylik maosh: oklad + sotuvdan bonus
            |– Barqaror ish va o‘sish imkoniyati
            |
            |☎️ Bog‘lanish:
            |Cv yuboring iltimos rasm bolsin anketada
            |– Telegram: @iwsagency
            |
            |👍 ASOSIY KANAL 1MLN 👈""".stripMargin,
        salary = Some("6 000 000 – 20 000 000 so'm"),
        location = Some("Toshkent"),
        source = "ishchi_bor_kerak_toshkent",
        url = "https://t.me/ishchi_bor_kerak_toshkent/103614",
        postedAt = postedAt,
        contactLinks = None,
      )

    val details = IshchiBorKerakToshkentSourceJobEtl.enrich(rawJob)
    val requirements = details.requirements.getOrElse("")
    val contactText = details.contactText.getOrElse("")
    val benefits = details.benefits.getOrElse("")

    expect.same(
      Some(
        "Mijozlar bilan savdo jarayonlarini olib borish\nTelefon orqali va bevosita sotuvni amalga oshirish\nRavon gapirish va ishontirish orqali sotish"
      ),
      details.responsibilities,
    ) &&
    expect(requirements.contains("18")) &&
    expect(benefits.contains("Shinam ofis")) &&
    expect(benefits.contains("Yuqori oylik maosh: oklad + sotuvdan bonus")) &&
    expect(benefits.contains("Barqaror ish va o")) &&
    expect(!contactText.contains("ASOSIY KANAL")) &&
    expect.same(List("iwsagency"), details.contactTelegramUsernames)
  }

  pureTest("drops standalone role headings and strips role prefixes from multi-role requirements") {
    val rawJob =
      RawJob(
        title = "Hamshira / Tozalik xodimi / Reception",
        company = Some("Doctor Sobirov klinikasi"),
        description =
          """⚡️ Doctor Sobirov klinikasiga bir nechta lavozimga ish taklifi
            |
            |💵 Maosh: 1 500 000 – 2 000 000 so'm
            |📍 Hudud: Urganch
            |
            |❗️ Talablar:
            |• Faqat ayol-qizlar
            |• Urganch shahridan bo'lishi
            |• Oldin laboratoriyada ishlagan bo'lishi
            |• Kompyuterdan foydalana olishi
            |• Hamshira
            |• Avtoklov dasturida ishlay olishi
            |• Xirurgiya bo'limida ishlagan bo'lishi kerak
            |• Tozalik xodimi
            |• Tozalikka e'tiborli bo'lishi
            |• Mas'uliyatli va chaqqon bo'lishi kerak
            |• Reception
            |• 20-30 yosh
            |• Kompyuterda ishlay olishi
            |• Oldin shu sohada ishlagan bo'lishi
            |• Xushmuomala va mas'uliyatli bo'lishi kerak
            |
            |☎️ Bog‘lanish:
            |• +998975121511""".stripMargin,
        salary = Some("1 500 000 – 2 000 000 so'm"),
        location = Some("Urganch"),
        source = "ishchi_bor_kerak_toshkent",
        url = "https://t.me/ishchi_bor_kerak_toshkent/999001",
        postedAt = postedAt,
        contactLinks = None,
      )

    val requirements = IshchiBorKerakToshkentSourceJobEtl.enrich(rawJob).requirements.getOrElse("")

    expect(!requirements.contains("\nHamshira\n")) &&
    expect(!requirements.contains("\nTozalik xodimi\n")) &&
    expect(!requirements.contains("\nReception\n")) &&
    expect(requirements.contains("Avtoklov dasturida ishlay olishi")) &&
    expect(requirements.contains("20-30 yosh"))
  }
}
