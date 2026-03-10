package uz.scala

import uz.scala.telegram.TelegramChannelPublisher
import uz.scala.telegram.TelegramConfig

case class JobsEnvironment[F[_]](
    algebras: uz.scala.Algebras[F],
    telegramPublisher: TelegramChannelPublisher[F],
    telegram: TelegramConfig,
  )
