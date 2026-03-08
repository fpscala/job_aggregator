package uz.scala.http

import cats.effect.Async

import uz.scala.Algebras
import uz.scala.http4s.HttpServerConfig

final case class Environment[F[_]: Async](
    config: HttpServerConfig,
    algebras: Algebras[F],
    defaultLimit: Int,
  )
