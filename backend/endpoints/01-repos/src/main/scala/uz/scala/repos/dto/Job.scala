package uz.scala.repos.dto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.UUID

import io.scalaland.chimney.dsl.TransformationOps

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs
import uz.scala.domain.jobs.JobDetails

case class Job(
    id: UUID,
    title: String,
    company: Option[String],
    description: String,
    salary: Option[String],
    location: Option[String],
    source: String,
    sourceUrl: String,
    postedAt: ZonedDateTime,
    createdAt: ZonedDateTime,
    requirements: Option[String],
    responsibilities: Option[String],
    benefits: Option[String],
    additional: Option[String],
    workSchedule: Option[String],
    contactText: Option[String],
    contactPhoneNumbers: List[String],
    contactTelegramUsernames: List[String],
    contactLinks: List[String],
    telegramPublishedAt: Option[ZonedDateTime],
    dedupHash: String,
    sourcePostHash: String,
  ) {
  def toDomain: jobs.Job =
    this.into[jobs.Job].transform
}

object Job {
  def fromEvent(
      input: RawJob,
      id: UUID,
      createdAt: ZonedDateTime,
      details: JobDetails,
    ): Job =
    input
      .into[Job]
      .withFieldConst(_.id, id)
      .withFieldConst(_.createdAt, createdAt)
      .withFieldComputed(_.title, value => sentenceCaseTitle(value.title))
      .withFieldComputed(_.sourceUrl, _.url)
      .withFieldConst(_.requirements, details.requirements)
      .withFieldConst(_.responsibilities, details.responsibilities)
      .withFieldConst(_.benefits, details.benefits)
      .withFieldConst(_.additional, details.additional)
      .withFieldConst(_.workSchedule, details.workSchedule)
      .withFieldConst(_.contactText, details.contactText)
      .withFieldConst(_.contactPhoneNumbers, details.contactPhoneNumbers)
      .withFieldConst(_.contactTelegramUsernames, details.contactTelegramUsernames)
      .withFieldConst(_.contactLinks, details.contactLinks)
      .withFieldConst(_.telegramPublishedAt, Option.empty[ZonedDateTime])
      .withFieldConst(_.dedupHash, Job.deduplicationHash(input, details))
      .withFieldConst(_.sourcePostHash, Job.sourcePostHash(input))
      .enableOptionDefaultsToNone
      .transform

  def deduplicationHash(job: RawJob, details: JobDetails): String =
    sha256(
      List(
        normalize(job.source),
        normalize(job.title),
        normalize(job.company.getOrElse("")),
        normalize(job.location.getOrElse("")),
        normalize(job.salary.getOrElse("")),
        normalize(details.requirements.getOrElse("")),
        normalize(details.responsibilities.getOrElse("")),
        normalize(details.benefits.getOrElse("")),
        normalize(details.additional.getOrElse("")),
        normalize(details.workSchedule.getOrElse("")),
        normalize(details.contactText.getOrElse("")),
        details.contactPhoneNumbers.sorted.mkString("|"),
        details.contactTelegramUsernames.sorted.mkString("|"),
        details.contactLinks.sorted.mkString("|"),
        normalizedDescription(job),
      ).mkString("|")
    )

  def sourcePostHash(job: RawJob): String =
    sha256(
      List(
        normalize(job.source),
        normalize(job.url),
      ).mkString("|")
    )

  private def normalizedDescription(job: RawJob): String = {
    val ignoredHandles = (sourceHandleFromUrl(job.url).toList ++ List(job.source))
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .distinct

    val withoutIgnoredHandles =
      ignoredHandles.foldLeft(job.description) {
        case (current, handle) =>
          current.replaceAll(s"(?iu)@${java.util.regex.Pattern.quote(handle)}\\b", " ")
      }

    normalize(withoutIgnoredHandles.replace(job.url, " "))
  }

  private def sourceHandleFromUrl(url: String): Option[String] =
    """https?://t\.me/([^/\s]+)/\d+"""
      .r
      .findFirstMatchIn(url)
      .map(_.group(1))

  private def normalize(value: String): String =
    value
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .replace('’', '\'')
      .replace('ʻ', '\'')
      .replace('ʼ', '\'')
      .replaceAll("""\s+""", " ")
      .trim
      .toLowerCase

  private def sentenceCaseTitle(value: String): String = {
    val trimmed = value.trim
    val firstLetterIndex = trimmed.indexWhere(_.isLetter)

    if (firstLetterIndex < 0) trimmed
    else {
      val prefix = trimmed.take(firstLetterIndex)
      val letter = trimmed.charAt(firstLetterIndex).toUpper
      val suffix = trimmed.drop(firstLetterIndex + 1)

      s"$prefix$letter$suffix"
    }
  }

  private def sha256(value: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    digest
      .digest(value.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_))
      .mkString
  }
}
