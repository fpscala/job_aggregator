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
}
