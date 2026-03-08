package uz.scala.domain.jobs

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.UUID

import io.circe.Encoder
import io.circe.Json

final case class Job(
    id: UUID,
    title: String,
    company: Option[String],
    description: String,
    salary: Option[String],
    location: Option[String],
    source: String,
    sourceUrl: String,
    postedAt: OffsetDateTime,
    createdAt: OffsetDateTime,
  )

object Job {
  implicit val encoder: Encoder[Job] = Encoder.instance { job =>
    Json.obj(
      "id" -> Json.fromString(job.id.toString),
      "title" -> Json.fromString(job.title),
      "company" -> job.company.fold(Json.Null)(Json.fromString),
      "description" -> Json.fromString(job.description),
      "salary" -> job.salary.fold(Json.Null)(Json.fromString),
      "location" -> job.location.fold(Json.Null)(Json.fromString),
      "source" -> Json.fromString(job.source),
      "sourceUrl" -> Json.fromString(job.sourceUrl),
      "postedAt" -> Json.fromString(job.postedAt.toString),
      "createdAt" -> Json.fromString(job.createdAt.toString),
    )
  }

  def deduplicationHash(job: Job): String = {
    val normalized = List(job.title, job.company.getOrElse(""), job.location.getOrElse(""))
      .map(_.trim.toLowerCase)
      .mkString("|")

    val digest = MessageDigest.getInstance("SHA-256")
    digest
      .digest(normalized.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_))
      .mkString
  }
}
