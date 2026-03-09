package uz.scala.repos.dto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.UUID

import io.scalaland.chimney.dsl.TransformationOps

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs

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
    dedupHash: String,
  ) {
  def toDomain: jobs.Job =
    this.into[jobs.Job].transform
}

object Job {
  def fromEvent(
      input: RawJob,
      id: UUID,
      createdAt: ZonedDateTime,
    ): Job =
    input
      .into[Job]
      .withFieldConst(_.id, id)
      .withFieldConst(_.dedupHash, Job.deduplicationHash(input))
      .withFieldConst(_.createdAt, createdAt)
      .withFieldComputed(_.sourceUrl, _.url)
      .enableOptionDefaultsToNone
      .transform

  def deduplicationHash(job: RawJob): String = {
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
