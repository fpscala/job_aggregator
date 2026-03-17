package uz.scala.etl.sources

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails
import uz.scala.etl.SourceJobEtl

object IshchiBorKerakToshkentSourceJobEtl extends SourceJobEtl {
  override val sources: Set[String] =
    Set("ishchi_bor_kerak_toshkent")

  override def enrich(rawJob: RawJob): JobDetails =
    XorazmIshSourceJobEtl.enrich(rawJob)
}
