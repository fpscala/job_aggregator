package uz.scala

import cats.effect.Concurrent
import cats.effect.ExitCode
import cats.effect.Resource
import cats.effect.implicits.genSpawnOps
import cats.implicits._
import fs2.Stream
import org.typelevel.log4cats.Logger

import uz.scala.algebras.JobsAlgebra
import uz.scala.kafka.Topics
import uz.scala.listeners.RawJobListener

trait Listeners[F[_]] {
  def start: Resource[F, ExitCode]
}

object Listeners {
  def make[F[_]: Concurrent: Logger](
      topics: Topics[F],
      jobsAlgebra: JobsAlgebra[F],
    ): Listeners[F] =
    new Listeners[F] {
      private val trafficListener =
        RawJobListener.make[F](topics.rawJobEvent, jobsAlgebra)

      override def start: Resource[F, ExitCode] =
        Stream(
          trafficListener.start()
        ).parJoin(1)
          .compile
          .drain
          .background
          .as(ExitCode.Success)
    }
}
