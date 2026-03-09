package uz.scala.repos

import java.util.UUID

import doobie.ConnectionIO

import uz.scala.repos.dto.Job
import uz.scala.repos.sql.JobsSql

trait JobRepository[F[_]] {
  def insert(job: Job): F[Boolean]
  def findById(id: UUID): F[Option[Job]]
}

object JobRepository {
  def make: JobRepository[ConnectionIO] =
    new JobRepository[ConnectionIO] {
      override def insert(job: Job): ConnectionIO[Boolean] =
        JobsSql.insert.run(job).map(_ > 0)

      override def findById(id: UUID): ConnectionIO[Option[Job]] =
        JobsSql.findById(id).option
    }
}
