package uz.scala.kafka

import cats.effect.Async
import cats.effect.Resource
import org.typelevel.log4cats.Logger

import uz.scala.domain.events.RawJob

case class Topics[F[_]](
    rawJobEvent: Topic[F, RawJob]
  )

object Topics {
  def make[F[_]: Async: Logger](config: KafkaConfig): Resource[F, Topics[F]] =
    for {
      rawJobEventTopic <- KafkaProducer.makeTopic[F, RawJob](
        config,
        config.topics.rawJob,
      )
    } yield Topics(
      rawJobEvent = rawJobEventTopic
    )
}
