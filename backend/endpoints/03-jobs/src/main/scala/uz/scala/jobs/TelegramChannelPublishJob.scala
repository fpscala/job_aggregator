package uz.scala.jobs

import scala.concurrent.duration._

import cats.effect.IO
import cats.implicits._

import uz.scala.JobsEnvironment
import uz.scala.domain.jobs.Job
import uz.scala.jobs.Job.AutoName
import uz.scala.telegram.TelegramCaptionRenderer

object TelegramChannelPublishJob
    extends AutoName[IO]
       with PeriodicJob[IO, JobsEnvironment[IO]] {
  override val interval: FiniteDuration = 30.seconds

  override def run(implicit env: JobsEnvironment[IO]): IO[Unit] =
    publishConfig(env) match {
      case None => IO.unit
      case Some(config) =>
        for {
          jobs <- env.algebras.jobs.findReadyForPublication(
            config.channelChatId,
            config.batchSize,
          )
          _ <- logger.info(s"Found ${jobs.size} jobs ready for Telegram publishing")
          _ <- jobs.traverse_(publishOne(_, config))
        } yield ()
    }

  private def publishOne(
      job: Job,
      config: PublishConfig,
    )(implicit env: JobsEnvironment[IO]): IO[Unit] = {
    val caption = TelegramCaptionRenderer.render(job, config.footerHandle)

    (for {
      messageId <- env.telegramPublisher.publishPhoto(
        config.channelChatId,
        config.bannerImagePath,
        caption,
      )
      inserted <- env.algebras.jobs.markPublished(
        jobId = job.id,
        channelChatId = config.channelChatId,
        telegramMessageId = messageId,
        caption = caption,
        bannerImagePath = config.bannerImagePath,
      )
      _ <- if (inserted)
        logger.info(
          s"Published job id=${job.id} to Telegram channel=${config.channelChatId} messageId=$messageId"
        )
      else
        logger.warn(
          s"Skipped storing Telegram publication metadata for job id=${job.id} channel=${config.channelChatId}"
        )
    } yield ()).handleErrorWith { error =>
      logger.error(error)(
        s"Failed to publish job id=${job.id} to Telegram channel=${config.channelChatId}"
      )
    }
  }

  private def publishConfig(env: JobsEnvironment[IO]): Option[PublishConfig] =
    if (!env.telegram.enabled || !env.telegram.publish.enabled)
      None
    else
      (
        env.telegram.publish.channelChatId.map(_.trim).filter(_.nonEmpty),
        env.telegram.publish.bannerImagePath.map(_.trim).filter(_.nonEmpty),
      ).mapN { (channelChatId, bannerImagePath) =>
        PublishConfig(
          channelChatId = channelChatId,
          batchSize = env.telegram.publish.batchSize.max(1),
          bannerImagePath = bannerImagePath,
          footerHandle =
            env.telegram.publish.footerHandle
              .map(_.trim)
              .filter(_.nonEmpty)
              .orElse(Option(channelChatId).filter(_.startsWith("@"))),
        )
      }

  private final case class PublishConfig(
      channelChatId: String,
      batchSize: Int,
      bannerImagePath: String,
      footerHandle: Option[String],
    )
}
