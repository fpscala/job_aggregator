package uz.scala.domain.events

import java.time.ZonedDateTime

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec

import uz.scala.kafka.KafkaEvent

@ConfiguredJsonCodec
final case class RawJob(
    title: String,
    company: Option[String],
    description: String,
    salary: Option[String],
    location: Option[String],
    source: String,
    url: String,
    postedAt: ZonedDateTime,
    contactLinks: Option[List[String]],
  ) extends KafkaEvent {
  override def eventType: String = "jobs.raw"
}

object RawJob {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}
