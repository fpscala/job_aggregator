package uz.scala.telegram

import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

import weaver.SimpleIOSuite

import uz.scala.domain.jobs.Job

object TelegramPublishEligibilityTest extends SimpleIOSuite {
  private val timestamp =
    ZonedDateTime.of(2026, 3, 14, 20, 0, 0, 0, ZoneId.of("Asia/Tashkent"))

  private def job(title: String): Job =
    Job(
      id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      title = title,
      company = None,
      description = "Raw description",
      salary = None,
      location = None,
      source = "xorazm_ish",
      sourceUrl = "https://t.me/Xorazm_ish/999",
      postedAt = timestamp,
      createdAt = timestamp,
      requirements = None,
      responsibilities = None,
      benefits = None,
      additional = None,
      workSchedule = None,
      contactText = None,
      contactPhoneNumbers = List.empty,
      contactTelegramUsernames = List.empty,
      contactLinks = List.empty,
      telegramPublishedAt = None,
    )

  pureTest("blocks raw placeholder titles from telegram publication") {
    expect(!TelegramPublishEligibility.isEligible(job("raw_post"))) &&
    expect(!TelegramPublishEligibility.isEligible(job("placeholder"))) &&
    expect(TelegramPublishEligibility.isEligible(job("Savdo agenti")))
  }
}
