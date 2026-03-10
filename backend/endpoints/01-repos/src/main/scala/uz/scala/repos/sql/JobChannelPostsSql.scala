package uz.scala.repos.sql

import doobie.Update
import doobie.implicits.toSqlInterpolator

import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto.JobChannelPost

object JobChannelPostsSql extends Sql[JobChannelPost] {
  val insert: Update[JobChannelPost] = Update[JobChannelPost](
    sql"""INSERT INTO $table ($columns) VALUES ($values) ON CONFLICT DO NOTHING""".internals.sql
  )
}
