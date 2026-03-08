package uz.scala.kafka


import fs2.Stream
/** Source - faqat consume qilish uchun
 */
trait Source[F[_], A] {
  def stream(groupId: String): Stream[F, A]
}
