package uz.scala.domain.jobs

final case class JobDetails(
    requirements: Option[String],
    responsibilities: Option[String],
    benefits: Option[String],
    additional: Option[String],
    workSchedule: Option[String],
    contactText: Option[String],
    contactPhoneNumbers: List[String],
    contactTelegramUsernames: List[String],
    contactLinks: List[String],
  ) {
  def hasContacts: Boolean =
    contactPhoneNumbers.nonEmpty || contactTelegramUsernames.nonEmpty || contactLinks.nonEmpty
}

object JobDetails {
  val empty: JobDetails =
    JobDetails(
      requirements = None,
      responsibilities = None,
      benefits = None,
      additional = None,
      workSchedule = None,
      contactText = None,
      contactPhoneNumbers = List.empty,
      contactTelegramUsernames = List.empty,
      contactLinks = List.empty,
    )
}
