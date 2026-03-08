package uz.scala.listeners

import cats.effect.MonadCancelThrow
import cats.implicits._
import fs2.Stream
import org.typelevel.log4cats.Logger

import uz.scala.algebras.InsertResult
import uz.scala.algebras.JobsAlgebra
import uz.scala.domain.events.RawJob
import uz.scala.kafka.Source

trait RawJobListener[F[_]] {
  def start(): Stream[F, Unit]
}

object RawJobListener {
  def make[F[_]: MonadCancelThrow: Logger](
      source: Source[F, RawJob],
      jobsAlgebra: JobsAlgebra[F],
    ): RawJobListener[F] =
    new RawJobListener[F] {
      private val groupId = "raw-event-listener"

      override def start(): Stream[F, Unit] =
        source
          .stream(groupId)
          .evalMap { rawJobEvent =>
            (for {
              result <- jobsAlgebra.ingest(rawJobEvent)
              _ <- result match {
                case InsertResult.Inserted =>
                  Logger[F].info(
                    s"Stored job from source=${rawJobEvent.source} title=${rawJobEvent.title}"
                  )
                case InsertResult.Duplicate =>
                  Logger[F].info(
                    s"Skipped duplicate job from source=${rawJobEvent.source} title=${rawJobEvent.title}"
                  )
              }
            } yield ())
              .handleErrorWith { error =>
                Logger[F].error(error)(
                  s"Failed to process job event from source=${rawJobEvent.source}: ${error.getMessage}"
                )
              }
          }
    }
}
