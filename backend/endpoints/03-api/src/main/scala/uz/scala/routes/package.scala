package uz.scala

import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

package object routes {
  object LimitQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object SearchQueryParam extends OptionalQueryParamDecoderMatcher[String]("q")
}
