package uz.scala.repos

import doobie.ConnectionIO

import uz.scala.repos.dto.JobChannelPost
import uz.scala.repos.sql.JobChannelPostsSql

trait JobChannelPostRepository[F[_]] {
  def insert(jobChannelPost: JobChannelPost): F[Boolean]
}

object JobChannelPostRepository {
  def make: JobChannelPostRepository[ConnectionIO] =
    new JobChannelPostRepository[ConnectionIO] {
      override def insert(jobChannelPost: JobChannelPost): ConnectionIO[Boolean] =
        JobChannelPostsSql.insert.run(jobChannelPost).map(_ > 0)
    }
}
