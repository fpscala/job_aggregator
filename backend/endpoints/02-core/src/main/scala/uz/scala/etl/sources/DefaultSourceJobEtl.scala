package uz.scala.etl.sources

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails
import uz.scala.etl.SourceJobEtl

object DefaultSourceJobEtl extends SourceJobEtl {
  override val sources: Set[String] = Set.empty

  override def enrich(rawJob: RawJob): JobDetails =
    JobDetails.empty
}
