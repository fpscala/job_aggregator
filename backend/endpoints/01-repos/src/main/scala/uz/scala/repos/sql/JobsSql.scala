package uz.scala.repos.sql

import java.util.UUID

import doobie.Query0
import doobie.Update
import doobie.implicits.toSqlInterpolator

import uz.scala.doobie.Sql
import uz.scala.doobie.syntax.all._
import uz.scala.repos.dto.Job

object JobsSql extends Sql[Job] {
  val insert: Update[Job] = Update[Job](
    sql"""INSERT INTO $table ($columns) VALUES ($values)""".internals.sql
  )

  def findById(id: UUID): Query0[Job] =
    sql"""SELECT $columns FROM $table WHERE id = $id LIMIT 1""".query[Job]
}
