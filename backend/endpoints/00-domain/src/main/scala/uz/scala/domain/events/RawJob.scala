package uz.scala.domain.events

import java.time.OffsetDateTime

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
    postedAt: OffsetDateTime,
  ) extends KafkaEvent {
  override def eventType: String = "jobs.raw"
}

object RawJob {
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames
}
