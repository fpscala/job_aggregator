package uz.scala.etl.sources

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails
import uz.scala.etl.SourceJobEtl

object IshborToshkentKerakIshchiBorSourceJobEtl extends SourceJobEtl {
  override val sources: Set[String] =
    Set("ishbor_toshkent_kerak_ishchi_bor")

  override def enrich(rawJob: RawJob): JobDetails =
    XorazmIshSourceJobEtl.enrich(rawJob)
}
