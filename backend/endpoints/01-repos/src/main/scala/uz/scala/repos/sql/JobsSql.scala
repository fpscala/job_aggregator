package uz.scala.repos.sql

import java.time.ZonedDateTime
import java.util.UUID

import doobie.Query0
import doobie.Update
import doobie.Update0
import doobie.implicits.toSqlInterpolator

import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto.Job

object JobsSql extends Sql[Job] {

  val insert: Update[Job] = Update[Job](
    sql"""INSERT INTO $table ($columns) VALUES ($values) ON CONFLICT DO NOTHING""".internals.sql
  )

  def findById(id: UUID): Query0[Job] =
    sql"""SELECT $columns FROM $table WHERE id = $id LIMIT 1""".query[Job]

  def findReadyForPublication(channelChatId: String, limit: Int): Query0[Job] =
    sql"""
      SELECT $columns
      FROM $table AS jobs
      WHERE NOT EXISTS (
        SELECT 1
        FROM job_channel_posts AS job_channel_posts
        WHERE job_channel_posts.job_id = jobs.id
          AND job_channel_posts.channel_chat_id = $channelChatId
      )
      ORDER BY jobs.posted_at ASC, jobs.created_at ASC
      LIMIT $limit
    """.query[Job]

  def markTelegramPublished(id: UUID, publishedAt: ZonedDateTime): Update0 =
    sql"""
      UPDATE $table
      SET telegram_published_at = $publishedAt
      WHERE id = $id
        AND (telegram_published_at IS NULL OR telegram_published_at < $publishedAt)
    """.update
}
