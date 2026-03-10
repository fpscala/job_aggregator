package uz.scala.telegram

final case class TelegramConfig(
    enabled: Boolean = false,
    bot: TelegramConfig.Bot = TelegramConfig.Bot(),
    publish: TelegramConfig.Publish = TelegramConfig.Publish(),
  )

object TelegramConfig {
  final case class Bot(
      token: Option[String] = None,
      apiBaseUrl: String = "https://api.telegram.org",
    )

  final case class Publish(
      enabled: Boolean = false,
      channelChatId: Option[String] = None,
      batchSize: Int = 10,
      bannerImagePath: Option[String] = None,
      footerHandle: Option[String] = None,
    )
}
