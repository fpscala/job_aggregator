package uz.scala.algebras

import java.util.UUID

import cats.effect.MonadCancelThrow
import cats.syntax.all._
import doobie.ConnectionIO
import doobie.Transactor
import doobie.implicits._
import org.typelevel.log4cats.Logger
import uz.scala.domain.enums.InsertResult
import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.Job
import uz.scala.effects.Calendar
import uz.scala.etl.SourceJobEtls
import uz.scala.repos.dto
import uz.scala.repos.JobChannelPostRepository
import uz.scala.effects.GenUUID
import uz.scala.repos.JobRepository

trait JobsAlgebra[F[_]] {
  def ingest(rawJob: RawJob): F[InsertResult]
  def findById(id: UUID): F[Option[Job]]
  def findReadyForPublication(channelChatId: String, limit: Int): F[List[Job]]
  def markPublished(
      jobId: UUID,
      channelChatId: String,
      telegramMessageId: Long,
      caption: String,
      bannerImagePath: String,
    ): F[Boolean]
}

object JobsAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger](
      repository: JobRepository[ConnectionIO],
      jobChannelPostRepository: JobChannelPostRepository[ConnectionIO],
    )(implicit
      xa: Transactor[F]
    ): JobsAlgebra[F] =
    new JobsAlgebra[F] {
      override def ingest(rawJob: RawJob): F[InsertResult] = {
        val details = SourceJobEtls.enrich(rawJob)

        if (details.hasContacts)
          for {
            id <- GenUUID[F].make
            createdAt <- Calendar[F].currentZonedDateTime
            inserted <- repository
              .insert(dto.Job.fromEvent(rawJob, id, createdAt, details))
              .transact(xa)
          } yield if (inserted) InsertResult.Inserted else InsertResult.Duplicate
        else MonadCancelThrow[F].pure(InsertResult.Invalid)
      }

      override def findById(id: UUID): F[Option[Job]] =
        repository.findById(id).map(_.map(_.toDomain)).transact(xa)

      override def findReadyForPublication(
          channelChatId: String,
          limit: Int,
        ): F[List[Job]] =
        repository
          .findReadyForPublication(channelChatId, limit.max(1))
          .map(_.map(_.toDomain))
          .transact(xa)

      override def markPublished(
          jobId: UUID,
          channelChatId: String,
          telegramMessageId: Long,
          caption: String,
          bannerImagePath: String,
        ): F[Boolean] =
        for {
          id <- GenUUID[F].make
          publishedAt <- Calendar[F].currentZonedDateTime
          inserted <- (
            for {
              inserted <- jobChannelPostRepository.insert(
                dto.JobChannelPost(
                  id = id,
                  jobId = jobId,
                  channelChatId = channelChatId,
                  telegramMessageId = telegramMessageId,
                  caption = caption,
                  bannerImagePath = bannerImagePath,
                  publishedAt = publishedAt,
                  createdAt = publishedAt,
                )
              )
              _ <- if (inserted)
                repository.markTelegramPublished(jobId, publishedAt).void
              else MonadCancelThrow[ConnectionIO].unit
            } yield inserted
          ).transact(xa)
        } yield inserted
    }
}
