package uz.scala.telegram

import java.util.Locale

import uz.scala.domain.jobs.Job

object TelegramPublishEligibility {
  private val BlockedTitles =
    Set("raw_post", "placeholder")

  def isEligible(job: Job): Boolean = {
    val normalizedTitle =
      job.title.trim.toLowerCase(Locale.ROOT)

    normalizedTitle.nonEmpty && !BlockedTitles.contains(normalizedTitle)
  }
}
