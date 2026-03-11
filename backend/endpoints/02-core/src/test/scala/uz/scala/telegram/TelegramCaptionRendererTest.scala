package uz.scala.telegram

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import uz.scala.domain.jobs.Job

object TelegramCaptionRendererTest extends SimpleIOSuite with Checkers {
  private val job =
    Job(
      id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      title = "Savdo agenti",
      company = Some("Fin Lite"),
      description = "Raw description",
      salary = Some("4 000 000 so'm"),
      location = Some("Urganch"),
      source = "xorazm_ish",
      sourceUrl = "https://t.me/Xorazm_ish/40585",
      postedAt = ZonedDateTime.of(2026, 3, 9, 12, 0, 0, 0, ZoneId.of("Asia/Samarkand")),
      createdAt = ZonedDateTime.of(2026, 3, 9, 12, 5, 0, 0, ZoneId.of("Asia/Samarkand")),
      requirements = Some("Tajriba\nMas'uliyat"),
      responsibilities = Some("Mijozlar bilan ishlash"),
      benefits = Some("Rasmiy ishga kirish"),
      additional = Some("Tushlik ishxona hisobidan"),
      workSchedule = Some("08:00 - 17:00"),
      contactText = Some("Rezyume yuboring"),
      contactPhoneNumbers = List("+998901234567"),
      contactTelegramUsernames = List("hr_manager"),
      contactLinks = List("https://example.com/apply"),
      telegramPublishedAt = None,
    )

  pureTest("renders rich telegram post format") {
    val rendered = TelegramCaptionRenderer.render(job)

    expect(rendered.contains("🔥 <b>Savdo agenti</b>")) &&
    expect(rendered.contains("🏢 <b>Kompaniya:</b> Fin Lite")) &&
    expect(rendered.contains("📋 <b>Talablar:</b>")) &&
    expect(rendered.contains("• Tajriba")) &&
    expect(rendered.contains("• Mas'uliyat")) &&
    expect(rendered.contains("💬 <b>Telegram:</b>")) &&
    expect(rendered.contains("""<a href="https://example.com/apply">Murojaat 1</a>""")) &&
    expect(!rendered.contains("📡")) &&
    expect(!rendered.contains("Manba posti"))
  }

  pureTest("renders multi-line sections as separate bullet lines") {
    val rendered =
      TelegramCaptionRenderer.render(
        job.copy(
          requirements = Some("Sotuv tajribasi\nMas'uliyat\nXushmuomalalik"),
          benefits = Some("Rasmiy ishga kirish\nTushlik ishxona hisobidan"),
        )
      )

    expect(rendered.contains("📋 <b>Talablar:</b>\n• Sotuv tajribasi\n• Mas'uliyat\n• Xushmuomalalik")) &&
    expect(rendered.contains("🎁 <b>Qulayliklar:</b>\n• Rasmiy ishga kirish\n• Tushlik ishxona hisobidan"))
  }

  pureTest("drops low-value contact text lines and keeps meaningful instruction") {
    val rendered =
      TelegramCaptionRenderer.render(
        job.copy(
          contactText = Some(
            "FDC_HRBOT\nOnlayn anketa\nTel\nhdp_omonschool_hr\nO'z rezumelaringizni quyidagi telefon raqamiga telegram orqali yuborishingiz mumkin"
          ),
          contactTelegramUsernames = List("fdc_hrbot", "hdp_omonschool_hr"),
          contactLinks = Nil,
        )
      )

    expect(!rendered.contains("FDC_HRBOT")) &&
    expect(!rendered.contains("Onlayn anketa")) &&
    expect(!rendered.contains("\n• Tel")) &&
    expect(!rendered.contains("📨 <b>Murojaat:</b>\n• hdp_omonschool_hr")) &&
    expect(rendered.contains("O'z rezumelaringizni quyidagi telefon raqamiga telegram orqali yuborishingiz mumkin"))
  }

  pureTest("appends footer handle when configured") {
    val rendered = TelegramCaptionRenderer.render(job, Some("@top_is_bot"))

    expect(rendered.endsWith("@top_is_bot"))
  }

  pureTest("keeps caption within telegram caption limit") {
    val oversizedJob =
      job.copy(
        requirements = Some(List.fill(100)("Uzoq requirement satri").mkString("\n")),
        responsibilities = Some(List.fill(100)("Uzoq responsibility satri").mkString("\n")),
        benefits = Some(List.fill(100)("Uzoq benefits satri").mkString("\n")),
      )

    val rendered = TelegramCaptionRenderer.render(oversizedJob, Some("@top_is_bot"))

    expect(rendered.length <= 1024)
  }

  pureTest("drops duplicate contact and metadata lines from additional and keeps phone last") {
    val rendered =
      TelegramCaptionRenderer.render(
        job.copy(
          location = Some("Hamid Olimjon 226 (Ekskavator zavodi yonida)"),
          additional =
            Some(
              "Eslatma\nUrganch yoki G'oybu mahallasida yashovchilar murojaat qilsin\nTalabalar qabul qilinmaydi\nUzoqdan keladiganlar bezovta qilmasin\nBog'lanish\nTelefon raqam\n998 88 299 12 18\n998 91 868 22 10\nMo'ljal: Ekskavator zavodi yonida\nAGAR TALABLAR SIZGA MAQUL KELGAN BO'LSA BIZ BILAN BOG'LANING!\nPOVR 150-300 som"
            ),
          contactPhoneNumbers = List("+998882991218", "+998918682210"),
          contactText = None,
          contactTelegramUsernames = Nil,
          contactLinks = Nil,
        )
      )

    val additionalIndex = rendered.indexOf("✨ <b>Qo'shimcha:</b>")
    val phoneIndex = rendered.indexOf("☎️ <b>Telefon:</b>")

    expect(rendered.contains("• Eslatma")) &&
    expect(rendered.contains("• Urganch yoki G'oybu mahallasida yashovchilar murojaat qilsin")) &&
    expect(!rendered.contains("• Bog'lanish")) &&
    expect(!rendered.contains("• Telefon raqam")) &&
    expect(!rendered.contains("• 998 88 299 12 18")) &&
    expect(!rendered.contains("• 998 91 868 22 10")) &&
    expect(!rendered.contains("• Mo'ljal: Ekskavator zavodi yonida")) &&
    expect(!rendered.contains("• AGAR TALABLAR SIZGA MAQUL KELGAN BO'LSA BIZ BILAN BOG'LANING!")) &&
    expect(!rendered.contains("• POVR 150-300 som")) &&
    expect(additionalIndex >= 0) &&
    expect(phoneIndex > additionalIndex)
  }

  pureTest("renders cyrillic labels for cyrillic jobs") {
    val rendered =
      TelegramCaptionRenderer.render(
        job.copy(
          title = "Муҳандис",
          description = "Кирилл матн",
          company = Some("Матбаа"),
          salary = Some("5 000 000 сўм"),
          location = Some("Урганч"),
          requirements = Some("Рус тилини билиши"),
          responsibilities = Some("Назорат қилиш"),
          benefits = Some("Бепул тушлик"),
          additional = Some("Карьерада янги босқич"),
          workSchedule = Some("08:00 - 17:00"),
          contactText = Some("Анкета ни тўлдиринг"),
        )
      )

    expect(rendered.contains("🏢 <b>Компания:</b> Матбаа")) &&
    expect(rendered.contains("🕒 <b>Иш вақти:</b> 08:00 - 17:00")) &&
    expect(rendered.contains("📋 <b>Талаблар:</b>")) &&
    expect(rendered.contains("🎁 <b>Қулайликлар:</b>")) &&
    expect(rendered.contains("✨ <b>Қўшимча:</b>")) &&
    expect(rendered.contains("📨 <b>Мурожаат:</b>")) &&
    expect(rendered.contains("🔗 <b>Ҳаволалар:</b>")) &&
    expect(rendered.contains("""<a href="https://example.com/apply">Мурожаат 1</a>"""))
  }
}
