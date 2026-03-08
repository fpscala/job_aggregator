package uz.scala

import cats.effect.Async
import _root_.doobie.ConnectionIO
import _root_.doobie.Transactor

import uz.scala.algebras.JobsAlgebra

final case class Algebras[F[_]](
    jobs: JobsAlgebra[F]
  )

object Algebras {
  def make[F[_]: Async](
      repositories: Repositories[ConnectionIO],
      maxQueryLimit: Int,
    )(implicit
      xa: Transactor[F]
    ): Algebras[F] =
    Algebras(
      jobs = JobsAlgebra.make[F](repositories.jobs, maxQueryLimit)
    )
}
