package uz.scala.algebras

import java.time.ZoneOffset
import java.util.UUID

import cats.syntax.all._
import cats.effect.Async
import cats.effect.Clock
import cats.effect.Sync
import doobie.ConnectionIO
import doobie.Transactor
import doobie.implicits._
import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.Job
import uz.scala.repos.JobRepository

sealed trait InsertResult

object InsertResult {
  case object Inserted extends InsertResult
  case object Duplicate extends InsertResult
}

trait JobsAlgebra[F[_]] {
  def ingest(rawJob: RawJob): F[InsertResult]
  def findById(id: UUID): F[Option[Job]]
  def findLatest(limit: Int): F[List[Job]]
  def search(query: String, limit: Int): F[List[Job]]
}

object JobsAlgebra {
  def make[F[_]: Async](
      repository: JobRepository[ConnectionIO],
      maxQueryLimit: Int,
    )(implicit
      xa: Transactor[F]
    ): JobsAlgebra[F] =
    new JobsAlgebra[F] {
      override def ingest(rawJob: RawJob): F[InsertResult] =
        for {
          id <- Sync[F].delay(UUID.randomUUID())
          createdAt <- Clock[F].realTimeInstant.map(_.atOffset(ZoneOffset.UTC))
          inserted <- repository
            .insert(
              Job(
                id = id,
                title = rawJob.title,
                company = rawJob.company,
                description = rawJob.description,
                salary = rawJob.salary,
                location = rawJob.location,
                source = rawJob.source,
                sourceUrl = rawJob.url,
                postedAt = rawJob.postedAt,
                createdAt = createdAt,
              )
            )
            .transact(xa)
        } yield if (inserted) InsertResult.Inserted else InsertResult.Duplicate

      override def findById(id: UUID): F[Option[Job]] =
        repository.findById(id).transact(xa)

      override def findLatest(limit: Int): F[List[Job]] =
        repository.findLatest(sanitizeLimit(limit)).transact(xa)

      override def search(query: String, limit: Int): F[List[Job]] =
        repository.search(query.trim, sanitizeLimit(limit)).transact(xa)

      private def sanitizeLimit(limit: Int): Int = {
        val effectiveMax = math.max(1, maxQueryLimit)
        limit.max(1).min(effectiveMax)
      }
    }
}
