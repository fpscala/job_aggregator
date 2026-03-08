package uz.scala.setup

import uz.scala.doobie.DataBaseConfig
import uz.scala.flyway.MigrationsConfig
import uz.scala.http4s.HttpServerConfig
import uz.scala.jobs.JobsRunnerConfig
import uz.scala.kafka.KafkaConfig
import uz.scala.mailer.MailerConfig
import uz.scala.redis.RedisConfig

case class Config(
    http: HttpServerConfig,
    redis: RedisConfig,
    database: DataBaseConfig,
    jobs: JobsRunnerConfig,
    mailer: MailerConfig,
    kafka: KafkaConfig,
    api: Config.Api,
  ) {
  lazy val migrations: MigrationsConfig = MigrationsConfig(
    hostname = database.host.value,
    port = database.port.value,
    database = database.database.value,
    username = database.user.value,
    password = database.password.value,
    schema = database.schema.fold("public")(_.value),
    location = "db/migration",
  )
}

object Config {
  case class Api(
      defaultLimit: Int,
      maxLimit: Int,
    )
}
