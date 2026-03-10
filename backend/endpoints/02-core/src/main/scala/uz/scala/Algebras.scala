package uz.scala

import _root_.doobie.ConnectionIO
import _root_.doobie.Transactor
import cats.effect.Async
import cats.effect.std.Random
import org.typelevel.log4cats.Logger

import uz.scala.algebras.JobsAlgebra
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID

final case class Algebras[F[_]](
    jobs: JobsAlgebra[F]
  )

object Algebras {
  def make[F[_]: Async: Calendar: GenUUID: Logger: Random](
      repositories: Repositories[ConnectionIO],
      maxQueryLimit: Int,
    )(implicit
      xa: Transactor[F]
    ): Algebras[F] =
    Algebras(
      jobs = JobsAlgebra.make[F](repositories.jobs, repositories.jobChannelPosts)
    )
}
