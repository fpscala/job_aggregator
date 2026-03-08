package uz.scala.kafka

/** Topic - consume va publish qilish uchun
 */
trait Topic[F[_], A] extends Source[F, A] {
  def publish(event: A): F[Unit]
}
