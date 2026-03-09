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
import uz.scala.repos.dto
import uz.scala.effects.GenUUID
import uz.scala.repos.JobRepository

trait JobsAlgebra[F[_]] {
  def ingest(rawJob: RawJob): F[InsertResult]
  def findById(id: UUID): F[Option[Job]]
}

object JobsAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger](
      repository: JobRepository[ConnectionIO]
    )(implicit
      xa: Transactor[F]
    ): JobsAlgebra[F] =
    new JobsAlgebra[F] {
      override def ingest(rawJob: RawJob): F[InsertResult] =
        for {
          id <- GenUUID[F].make
          createdAt <- Calendar[F].currentZonedDateTime
          inserted <- repository
            .insert(dto.Job.fromEvent(rawJob, id, createdAt))
            .transact(xa)
        } yield if (inserted) InsertResult.Inserted else InsertResult.Duplicate

      override def findById(id: UUID): F[Option[Job]] =
        repository.findById(id).map(_.map(_.toDomain)).transact(xa)
    }
}
