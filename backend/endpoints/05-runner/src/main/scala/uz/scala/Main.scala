package uz.scala

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import uz.scala.setup.Environment

object Main extends IOApp {
  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  // Each component is a long-running service
  private def components[F[_]: Async: Logger](
      env: Environment[F]
    ): List[Resource[F, ExitCode]] =
    List(
      env.listeners.start,
      Resource.eval(
        JobsModule.make[F](env.toJobs).startJobs(env.config.jobs)
      ),
      HttpModule.make[F](env.toServer),
    )

  override def run(args: List[String]): IO[ExitCode] = {
    val program: IO[ExitCode] = Environment
      .make[IO]
      .use { env =>
        // Test UTF-8 encoding with Russian text
        Logger[IO].info("Starting packet analyzer services...") >>
          components[IO](env)
            .traverse { resource =>
              // Each resource manages its own lifecycle and runs independently
              resource.use(_ => IO.never).start
            }
            .flatMap { fibers =>
              Logger[IO].info(s"Started ${fibers.size} independent services in parallel") >>
                // All services run in parallel, wait for all to complete
                fibers.parTraverse(_.join).flatMap { outcomes =>
                  Logger[IO].info("All services completed, shutting down...") >> {
                    outcomes.head match {
                      case Outcome.Succeeded(exitCode) => exitCode
                      case Outcome.Errored(error) =>
                        Logger[IO].error(error)("Service failed").as(ExitCode.Error)
                      case Outcome.Canceled() =>
                        Logger[IO].info("Service cancelled").as(ExitCode.Success)
                    }
                  }
                }
            }
            .handleErrorWith { error =>
              Logger[IO].error(error)("Fatal error in main").as(ExitCode.Error)
            }
      }
    program
  }
}
