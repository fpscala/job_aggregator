package uz.scala.routes

import cats.MonadThrow
import cats.effect.Async
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.typelevel.log4cats.Logger

import uz.scala.algebras.JobsAlgebra
import uz.scala.http4s.utils.Routes

final class JobRoutes[F[_]: Logger: JsonDecoder: MonadThrow](
    jobsAlgebra: JobsAlgebra[F]
  ) extends Routes[F, Unit] {
  override val path: String = "/jobs"
  override val public: HttpRoutes[F] = HttpRoutes.empty
}
