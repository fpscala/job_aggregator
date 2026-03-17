package uz.scala.etl

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails
import uz.scala.etl.sources.DefaultSourceJobEtl
import uz.scala.etl.sources.IshborToshkentKerakIshchiBorSourceJobEtl
import uz.scala.etl.sources.IshchiBorKerakToshkentSourceJobEtl
import uz.scala.etl.sources.XorazmIshSourceJobEtl

object SourceJobEtls {
  private val all: List[SourceJobEtl] =
    List(
      IshchiBorKerakToshkentSourceJobEtl,
      IshborToshkentKerakIshchiBorSourceJobEtl,
      XorazmIshSourceJobEtl,
      DefaultSourceJobEtl,
    )

  def enrich(rawJob: RawJob): JobDetails =
    all
      .find(_.supports(rawJob.source))
      .getOrElse(DefaultSourceJobEtl)
      .enrich(rawJob)
}
