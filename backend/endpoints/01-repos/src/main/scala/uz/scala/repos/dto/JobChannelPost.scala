package uz.scala.repos.dto

import java.time.ZonedDateTime
import java.util.UUID

case class JobChannelPost(
    id: UUID,
    jobId: UUID,
    channelChatId: String,
    telegramMessageId: Long,
    caption: String,
    bannerImagePath: String,
    publishedAt: ZonedDateTime,
    createdAt: ZonedDateTime,
  )
