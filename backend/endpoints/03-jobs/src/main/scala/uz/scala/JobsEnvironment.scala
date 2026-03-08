package uz.scala

case class JobsEnvironment[F[_]](
    algebras: uz.scala.Algebras[F]
  )
