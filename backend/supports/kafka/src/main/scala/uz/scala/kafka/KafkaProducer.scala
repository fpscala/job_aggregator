package uz.scala.kafka

import scala.concurrent.duration._

import cats.effect.Async
import cats.effect.Resource
import cats.implicits._
import fs2.Stream
import fs2.kafka._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.parser.decode
import io.circe.syntax._
import org.typelevel.log4cats.Logger



object KafkaProducer {
  /** Create a Topic with both publish and consume capabilities
    */
  def makeTopic[F[_]: Async: Logger, A <: KafkaEvent: Encoder: Decoder](
      config: KafkaConfig,
      topicName: String,
    ): Resource[F, Topic[F, A]] = {
    val producerSettings = ProducerSettings[F, String, String]
      .withBootstrapServers(config.bootstrapServers)
      .withAcks(Acks.All)
      .withRetries(config.producer.retries)
      .withBatchSize(config.producer.batchSize)
      .withLinger(config.producer.lingerMs.millis)
      .withClientId("job_aggregator")

    fs2
      .kafka
      .KafkaProducer
      .resource(producerSettings)
      .map { producer =>
        new Topic[F, A] {
          override def publish(event: A): F[Unit] = {
            val record = ProducerRecord(topicName, event.eventType, event.asJson.noSpaces)
            producer
              .produce(ProducerRecords.one(record))
              .flatten
              .void
              .handleErrorWith { error =>
                Logger[F].error(
                  s"Failed to publish event to topic $topicName: ${error.getMessage}"
                ) *>
                  error.raiseError[F, Unit]
              }
          }

          override def stream(groupId: String): Stream[F, A] = {
            val consumerSettings = ConsumerSettings[F, Array[Byte], String]
              .withBootstrapServers(config.bootstrapServers)
              .withGroupId(s"${config.consumer.groupIdPrefix}-$groupId")
              .withAutoOffsetReset(AutoOffsetReset.Earliest)
              .withEnableAutoCommit(config.consumer.enableAutoCommit)
              .withMaxPollRecords(config.consumer.maxPollRecords)

            KafkaConsumer
              .stream(consumerSettings)
              .subscribeTo(topicName)
              .records
              .evalMap { committable =>
                val record = committable.record
                decode[A](record.value) match {
                  case Right(event) =>
                    committable.offset.commit.as(Some(event): Option[A])
                  case Left(error) =>
                    Logger[F].error(
                      s"Failed to decode event from topic $topicName: ${error.getMessage}"
                    ) *>
                      committable.offset.commit.as(None: Option[A])
                }
              }
              .collect { case Some(event) => event }
          }
        }
      }
  }

  /** Create a Source (read-only) for consuming events
    */
  def makeSource[F[_]: Async: Logger, A <: KafkaEvent: Decoder](
      config: KafkaConfig,
      topicName: String,
    ): Source[F, A] =
    new Source[F, A] {
      override def stream(groupId: String): Stream[F, A] = {
        val consumerSettings = ConsumerSettings[F, Array[Byte], String]
          .withBootstrapServers(config.bootstrapServers)
          .withGroupId(s"${config.consumer.groupIdPrefix}-$groupId")
          .withAutoOffsetReset(AutoOffsetReset.Earliest)
          .withEnableAutoCommit(config.consumer.enableAutoCommit)
          .withMaxPollRecords(config.consumer.maxPollRecords)
          .withProperty("session.timeout.ms", config.consumer.sessionTimeout.toMillis.toString)
          .withProperty(
            "heartbeat.interval.ms",
            config.consumer.heartbeatInterval.toMillis.toString,
          )
          .withMaxPollInterval(5.minutes)

        KafkaConsumer
          .stream(consumerSettings)
          .subscribeTo(topicName)
          .records
          .evalMap { committable =>
            val record = committable.record
            decode[A](record.value) match {
              case Right(event) =>
                committable.offset.commit.as(Some(event): Option[A])
              case Left(error) =>
                Logger[F].error(
                  s"Failed to decode event from topic $topicName: ${error.getMessage}"
                ) *>
                  committable.offset.commit.as(None: Option[A])
            }
          }
          .collect { case Some(event) => event }
      }
    }
}
