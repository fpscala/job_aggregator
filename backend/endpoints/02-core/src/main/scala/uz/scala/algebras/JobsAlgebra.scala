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
import uz.scala.etl.SemiStructuredPostParser
import uz.scala.etl.StructuredPostParser
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
      private final case class ParsedCandidate(
          rawJob: RawJob,
          parsed: StructuredPostParser.Parsed,
        )

      override def ingest(rawJob: RawJob): F[InsertResult] = {
        val parsedResult =
          StructuredPostParser.parse(rawJob) match {
            case Right(parsed) =>
              Right(List(ParsedCandidate(rawJob, parsed)))
            case Left(_) =>
              SemiStructuredPostParser
                .parseMany(rawJob)
                .map(_.map(candidate => ParsedCandidate(candidate.rawJob, candidate.parsed)))
          }

        parsedResult match {
          case Left(rejected) =>
            Logger[F].info(
              s"Rejected raw job source=${rawJob.source} url=${rawJob.url} reason=${rejected.reason.code}"
            ) *>
              MonadCancelThrow[F].pure(InsertResult.Invalid)

          case Right(parsedCandidates) =>
            for {
              createdAt <- Calendar[F].currentZonedDateTime
              ids <- parsedCandidates.traverse(_ => GenUUID[F].make)
              inserted <- parsedCandidates
                .zip(ids)
                .traverse { case (candidate, id) =>
                  repository.insert(
                    dto.Job.fromEvent(
                      input = candidate.rawJob,
                      id = id,
                      createdAt = createdAt,
                      details = candidate.parsed.details,
                      titleOverride = Some(candidate.parsed.title),
                      companyOverride = Some(candidate.parsed.company),
                      salaryOverride = candidate.parsed.salary,
                      locationOverride = candidate.parsed.location,
                    )
                  )
                }
                .transact(xa)
            } yield if (inserted.exists(identity)) InsertResult.Inserted else InsertResult.Duplicate
        }
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
