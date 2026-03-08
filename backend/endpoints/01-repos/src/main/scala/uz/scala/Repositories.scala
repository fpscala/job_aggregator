package uz.scala

import _root_.doobie.ConnectionIO

import uz.scala.repos.JobRepository

final case class Repositories[F[_]](
    jobs: JobRepository[F]
  )

object Repositories {
  def make: Repositories[ConnectionIO] =
    Repositories(
      jobs = JobRepository.make
    )
}
