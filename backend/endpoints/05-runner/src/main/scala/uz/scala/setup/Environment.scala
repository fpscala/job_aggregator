package uz.scala.setup

import cats.effect.Async
import cats.effect.Resource
import cats.effect.std.Random
import cats.~>
import doobie.ConnectionIO
import doobie.Transactor
import doobie.WeakAsync
import eu.timepit.refined.pureconfig._
import org.typelevel.log4cats.Logger
import pureconfig.generic.auto._
import pureconfig.module.cron4s._

import uz.scala.Algebras
import uz.scala.JobsEnvironment
import uz.scala.Listeners
import uz.scala.Repositories
import uz.scala.doobie.DoobieTransaction
import uz.scala.flyway.Migrations
import uz.scala.http.{ Environment => HttpEnvironment }
import uz.scala.kafka.Topics
import uz.scala.telegram.TelegramChannelPublisher
import uz.scala.utils.ConfigLoader

final case class Environment[F[_]](
    config: Config,
    repositories: Repositories[ConnectionIO],
    algebras: Algebras[F],
    listeners: Listeners[F],
    telegramPublisher: TelegramChannelPublisher[F],
  )(implicit
    val async: Async[F],
    val xa: Transactor[F],
  ) {
  lazy val toServer: HttpEnvironment[F] =
    HttpEnvironment(
      config = config.http,
      algebras = algebras,
      defaultLimit = config.api.defaultLimit,
    )

  lazy val toJobs: JobsEnvironment[F] =
    JobsEnvironment(
      algebras = algebras,
      telegramPublisher = telegramPublisher,
      telegram = config.telegram,
    )
}

object Environment {
  def make[F[_]: Async: Logger]: Resource[F, Environment[F]] =
    for {
      config <- Resource.eval(ConfigLoader.load[F, Config])
      _ <- Resource.eval(Migrations.run[F](config.migrations))
      implicit0(xa: Transactor[F]) = DoobieTransaction.make[F](config.database)
      repositories = Repositories.make

      implicit0(random: Random[F]) <- Resource.eval(Random.scalaUtilRandom[F])
      implicit0(lifter: (F ~> ConnectionIO)) <- WeakAsync.liftK[F, ConnectionIO]
      topics <- Topics.make[F](config.kafka)
      telegramPublisher <- TelegramChannelPublisher.resource[F](config.telegram)
      algebras = Algebras.make[F](repositories, config.api.maxLimit)
      listeners = Listeners.make[F](topics, algebras.jobs)
      env = Environment[F](
        config = config,
        repositories = repositories,
        algebras = algebras,
        listeners = listeners,
        telegramPublisher = telegramPublisher,
      )
    } yield env
}
