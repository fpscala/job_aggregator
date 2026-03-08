package uz.scala.repos

import java.time.OffsetDateTime
import java.util.UUID

import doobie.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._

import uz.scala.domain.jobs.Job

trait JobRepository[F[_]] {
  def insert(job: Job): F[Boolean]
  def findById(id: UUID): F[Option[Job]]
  def findLatest(limit: Int): F[List[Job]]
  def search(query: String, limit: Int): F[List[Job]]
}

object JobRepository {
  def make: JobRepository[ConnectionIO] =
    new JobRepository[ConnectionIO] {
      override def insert(job: Job): ConnectionIO[Boolean] = {
        val dedupHash = Job.deduplicationHash(job)

        sql"""
          INSERT INTO jobs (
            id,
            title,
            company,
            description,
            salary,
            location,
            source,
            source_url,
            posted_at,
            created_at,
            dedup_hash
          ) VALUES (
            ${job.id},
            ${job.title},
            ${job.company},
            ${job.description},
            ${job.salary},
            ${job.location},
            ${job.source},
            ${job.sourceUrl},
            ${job.postedAt},
            ${job.createdAt},
            $dedupHash
          )
          ON CONFLICT (dedup_hash) DO NOTHING
        """.update.run.map(_ > 0)
      }

      override def findById(id: UUID): ConnectionIO[Option[Job]] =
        sql"""
          SELECT
            id,
            title,
            company,
            description,
            salary,
            location,
            source,
            source_url,
            posted_at,
            created_at
          FROM jobs
          WHERE id = $id
        """.query[Job].option

      override def findLatest(limit: Int): ConnectionIO[List[Job]] =
        sql"""
          SELECT
            id,
            title,
            company,
            description,
            salary,
            location,
            source,
            source_url,
            posted_at,
            created_at
          FROM jobs
          ORDER BY posted_at DESC
          LIMIT $limit
        """.query[Job].to[List]

      override def search(query: String, limit: Int): ConnectionIO[List[Job]] = {
        val pattern = s"%${query.trim}%"

        sql"""
          SELECT
            id,
            title,
            company,
            description,
            salary,
            location,
            source,
            source_url,
            posted_at,
            created_at
          FROM jobs
          WHERE
            title ILIKE $pattern OR
            company ILIKE $pattern OR
            description ILIKE $pattern OR
            location ILIKE $pattern OR
            source ILIKE $pattern
          ORDER BY posted_at DESC
          LIMIT $limit
        """.query[Job].to[List]
      }
    }
}
