package uz.scala.etl

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails

trait SourceJobEtl {
  def sources: Set[String]
  def enrich(rawJob: RawJob): JobDetails

  final def supports(source: String): Boolean =
    sources.contains(SourceJobEtl.normalizeSource(source))
}

object SourceJobEtl {
  def normalizeSource(source: String): String =
    source.trim.toLowerCase
}
