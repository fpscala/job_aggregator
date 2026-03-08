package uz.scala

import cats.data.NonEmptyList
import cats.effect.Async
import cats.effect.ExitCode
import cats.effect.kernel.Resource
import cats.implicits.toFunctorOps
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import uz.scala.http.Environment
import uz.scala.http4s.HttpServer
import uz.scala.http4s.utils.Routes
import uz.scala.routes.JobRoutes

object HttpModule {
  private def allRoutes[F[_]: Async: JsonDecoder: Logger](
      env: Environment[F]
    ): NonEmptyList[HttpRoutes[F]] =
    NonEmptyList
      .of[Routes[F, Unit]](
        new JobRoutes[F](env.algebras.jobs)
      )
      .map { r =>
        Router(
          r.path -> (r.public)
        )
      }

  def make[F[_]: Async](
      env: Environment[F]
    )(implicit
      logger: Logger[F]
    ): Resource[F, ExitCode] =
    HttpServer
      .make[F](env.config, _ => allRoutes[F](env))
      .evalMap { _ =>
        logger.info(s"HTTP server is started").as(ExitCode.Success)
      }
}
