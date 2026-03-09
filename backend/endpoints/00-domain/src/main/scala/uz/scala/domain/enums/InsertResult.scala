package uz.scala.domain.enums

sealed trait InsertResult

object InsertResult {
  case object Inserted extends InsertResult
  case object Duplicate extends InsertResult
}
