package uz.scala.domain.jobs

import java.time.ZonedDateTime
import java.util.UUID

import io.circe.generic.JsonCodec

@JsonCodec
final case class Job(
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
  )
