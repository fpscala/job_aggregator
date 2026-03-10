package uz.scala.telegram

import java.io.File

import cats.effect.Async
import cats.effect.Resource
import cats.syntax.all._
import org.http4s.ember.client.EmberClientBuilder
import telegramium.bots.ChatId
import telegramium.bots.ChatIntId
import telegramium.bots.ChatStrId
import telegramium.bots.Html
import telegramium.bots.InputPartFile
import telegramium.bots.high.BotApi
import telegramium.bots.high.FailedRequest
import telegramium.bots.high.Methods

trait TelegramChannelPublisher[F[_]] {
  def publishPhoto(
      channelChatId: String,
      bannerImagePath: String,
      caption: String,
    ): F[Long]
}

object TelegramChannelPublisher {
  def resource[F[_]: Async](config: TelegramConfig): Resource[F, TelegramChannelPublisher[F]] =
    config.bot.token.map(_.trim).filter(_.nonEmpty) match {
      case Some(token) if config.enabled =>
        EmberClientBuilder
          .default[F]
          .build
          .map { httpClient =>
            val api = BotApi[F](
              httpClient,
              s"${config.bot.apiBaseUrl.stripSuffix("/")}/bot$token",
            )

            new TelegramChannelPublisher[F] {
              override def publishPhoto(
                  channelChatId: String,
                  bannerImagePath: String,
                  caption: String,
                ): F[Long] =
                for {
                  photoFile <- ensureFileExists[F](bannerImagePath)
                  chatId <- parseChatId[F](channelChatId)
                  message <- sendPhotoWithFallback[F](
                    api = api,
                    chatId = chatId,
                    photoFile = photoFile,
                    caption = caption,
                  )
                } yield message.messageId.toLong
            }
          }

      case _ =>
        Resource.pure[F, TelegramChannelPublisher[F]](
          new TelegramChannelPublisher[F] {
            override def publishPhoto(
                channelChatId: String,
                bannerImagePath: String,
                caption: String,
              ): F[Long] =
              Async[F].raiseError(
                new IllegalStateException("Telegram publishing is not configured")
              )
          }
        )
    }

  private def parseChatId[F[_]: Async](value: String): F[ChatId] = {
    val normalized = value.trim

    if (normalized.startsWith("@")) (ChatStrId(normalized): ChatId).pure[F]
    else
      Async[F]
        .fromEither(
          Either
            .catchOnly[NumberFormatException](normalized.toLong)
            .leftMap(_ => new IllegalArgumentException(s"Invalid Telegram chat id: $normalized"))
        )
        .map(ChatIntId(_))
  }

  private def ensureFileExists[F[_]: Async](path: String): F[File] =
    Async[F].blocking(new File(path)).flatMap { file =>
      Async[F]
        .blocking(file.exists() && file.isFile)
        .ifM(
          file.pure[F],
          Async[F].raiseError(
            new IllegalArgumentException(s"Telegram banner image file does not exist: $path")
          ),
        )
    }

  private def sendPhotoWithFallback[F[_]: Async](
      api: BotApi[F],
      chatId: ChatId,
      photoFile: File,
      caption: String,
    ) =
    api
      .execute(
        Methods.sendPhoto(
          chatId = chatId,
          photo = InputPartFile(photoFile),
          caption = Option(caption),
          parseMode = Option(Html),
        )
      )
      .handleErrorWith {
        case error: FailedRequest[_]
            if error.description.exists(_.toLowerCase.contains("can't parse entities")) =>
          api.execute(
            Methods.sendPhoto(
              chatId = chatId,
              photo = InputPartFile(photoFile),
              caption = Option(stripHtml(caption)),
            )
          )

        case error => Async[F].raiseError(error)
      }

  private def stripHtml(value: String): String =
    value
      .replaceAll("""<a [^>]+>""", "")
      .replace("</a>", "")
      .replace("<b>", "")
      .replace("</b>", "")
      .replace("&quot;", "\"")
      .replace("&gt;", ">")
      .replace("&lt;", "<")
      .replace("&amp;", "&")
}
