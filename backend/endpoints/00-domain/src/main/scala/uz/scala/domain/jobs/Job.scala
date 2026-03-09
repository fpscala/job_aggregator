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
  )
