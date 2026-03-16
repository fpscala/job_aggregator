package uz.scala.etl

import java.util.Locale

import scala.util.matching.Regex

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails
import uz.scala.etl.sources.XorazmIshSourceJobEtl

object NarrativeRecruitmentPostParser {
  import StructuredPostParser.{Parsed, Rejected}
  import StructuredPostParser.RejectionReason

  private val NarrativeMarkers =
    List(
      "jamoamizga qo'shilishni",
      "jamoamizga qo'shilish",
      "jamoamizda ko'rsak xursand bo'lamiz",
      "qo'shilish talablari bor",
      "ishga qabul qilinganlar uchun",
    )

  private val SalaryMarkers =
    List("oylik maoshlar", "oylik maosh", "oylik", "maosh")

  private val LocationMarkers =
    List("manzil", "манзил")

  private val LandmarkMarkers =
    List("mo'ljal", "mo‘ljal", "мўлжал")

  private val ContactMarkers =
    List(
      "telefon",
      "tel",
      "aloqa",
      "murojaat",
      "bog'lanish",
      "boglanish",
      "telegram",
      "телефон",
      "тел",
      "алоқа",
      "мурожаат",
      "боғланиш",
    )

  private val BenefitKeywords =
    List(
      "premiya",
      "bonus",
      "mukofot",
      "rag'bat",
      "ragbat",
      "tushlik",
      "ovqat",
      "bepul",
      "trening",
      "имконият",
      "премия",
      "бонус",
      "рағбат",
      "тушлик",
      "овқат",
      "бепул",
    )

  private val SalaryLinePattern: Regex =
    """(?iu).*(?:\d.*(?:razryad|разряд|oylik|maosh|so'm|so‘m|сўм|сум|mln|million)|oylik\s+maoshlar).*""".r

  private val LeadingDecorationPattern: Regex =
    """^[\s\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾\uFE0F]+""".r

  private val TrailingDecorationPattern: Regex =
    """[\s\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾\uFE0F]+$""".r

  private val SourceHandlePattern: Regex =
    """(?iu)^@xorazm_ish(?:_bor_elonlar)?$""".r

  def parse(rawJob: RawJob): Either[Rejected, Parsed] = {
    val lines = normalizedLines(rawJob.description)

    if (!looksLikeNarrativeRecruitment(lines))
      Left(Rejected(RejectionReason.MissingStructuredLabels))
    else {
      val extractedDetails = XorazmIshSourceJobEtl.enrich(rawJob)
      val title = cleanTitle(rawJob.title).filter(_.nonEmpty)
      val company =
        rawJob.company.map(cleanValue).filter(_.nonEmpty).orElse(extractCompany(lines))
      val salary =
        extractNarrativeSalary(lines).orElse(rawJob.salary.map(cleanValue).filter(_.nonEmpty))
      val location =
        rawJob.location.map(cleanValue).filter(_.nonEmpty).orElse(extractLocation(lines))
      val benefitLines = extractBenefitLines(lines)
      val benefits =
        multilineValue(benefitLines).orElse(extractedDetails.benefits)
      val details =
        extractedDetails.copy(
          benefits = benefits,
          additional = subtractLines(extractedDetails.additional, benefitLines),
        )
      val optionalFieldCount =
        List(
          salary,
          location,
          details.requirements,
          details.benefits,
          details.additional,
        ).count(_.nonEmpty)

      if (title.isEmpty)
        Left(Rejected(RejectionReason.MissingTitle))
      else if (company.isEmpty)
        Left(Rejected(RejectionReason.MissingCompany))
      else if (!details.hasContacts)
        Left(Rejected(RejectionReason.MissingContact))
      else if (optionalFieldCount < 2)
        Left(Rejected(RejectionReason.TooFewOptionalFields))
      else
        Right(
          Parsed(
            title = title.get,
            company = company,
            salary = salary,
            location = location,
            details = details,
          )
        )
    }
  }

  private def looksLikeNarrativeRecruitment(lines: Vector[String]): Boolean = {
    val normalizedText = lines.map(normalize).mkString("\n")
    val hasNarrativeMarker = NarrativeMarkers.exists(normalizedText.contains)
    val hasCompanyHeader = extractCompany(lines).nonEmpty
    val hasContact = lines.exists(line => startsWithAny(line, ContactMarkers))

    hasNarrativeMarker && hasCompanyHeader && hasContact
  }

  private def extractCompany(lines: Vector[String]): Option[String] =
    lines
      .find(line => !isIgnorableLine(line))
      .map(cleanValue)
      .filter(_.nonEmpty)
      .map(stripWrappedQuotes)

  private def extractNarrativeSalary(lines: Vector[String]): Option[String] =
    lines.indexWhere(startsWithAny(_, SalaryMarkers)) match {
      case -1 =>
        None
      case index =>
        val head = cleanValue(lines(index))
        val tail =
          lines
            .drop(index + 1)
            .takeWhile(line => !startsWithAny(line, LocationMarkers ++ LandmarkMarkers ++ ContactMarkers))
            .filter(looksLikeSalaryLine)
            .toList

        val values =
          (Option(head).filter(_.nonEmpty).toList ++ tail)
            .map(cleanValue)
            .filter(_.nonEmpty)

        multilineValue(values)
    }

  private def extractLocation(lines: Vector[String]): Option[String] = {
    val address = extractLabeledValue(lines, LocationMarkers)
    val landmark = extractLabeledValue(lines, LandmarkMarkers)

    (address, landmark) match {
      case (Some(value), Some(marker)) if !normalize(value).contains(normalize(marker)) =>
        Some(s"$value ($marker)")
      case (Some(value), _) =>
        Some(value)
      case (None, Some(marker)) =>
        Some(marker)
      case _ =>
        None
    }
  }

  private def extractBenefitLines(lines: Vector[String]): List[String] =
    lines
      .filter(line => isBenefitLine(line))
      .map(cleanValue)
      .filter(_.nonEmpty)
      .distinct
      .toList

  private def subtractLines(
      additional: Option[String],
      removed: List[String],
    ): Option[String] = {
    val removedKeys = removed.map(normalize).toSet

    additional
      .map(_.split('\n').toList)
      .getOrElse(Nil)
      .map(cleanValue)
      .filter(_.nonEmpty)
      .filterNot(line => removedKeys.contains(normalize(line)))
      .distinct match {
      case Nil => None
      case values => Some(values.mkString("\n"))
    }
  }

  private def extractLabeledValue(lines: Vector[String], markers: List[String]): Option[String] =
    lines
      .collectFirst {
        case line if startsWithAny(line, markers) =>
          stripMarkersFromLine(line, markers)
      }
      .map(cleanValue)
      .filter(_.nonEmpty)

  private def startsWithAny(line: String, markers: List[String]): Boolean = {
    val key = normalize(line)
    markers.exists(marker => key.startsWith(marker))
  }

  private def stripMarkersFromLine(line: String, markers: List[String]): String =
    markers.foldLeft(line) { case (current, marker) =>
      LeadingDecorationPattern
        .replaceFirstIn(current, "")
        .replaceFirst(
          s"(?iu)^\\Q$marker\\E\\s*[:\\-–—]?\\s*",
          "",
        )
    }

  private def looksLikeSalaryLine(line: String): Boolean =
    SalaryLinePattern.matches(normalize(line))

  private def isBenefitLine(line: String): Boolean = {
    val trimmed = line.trim
    val key = normalize(line)

    trimmed.startsWith("✓") ||
    trimmed.startsWith("✔") ||
    trimmed.startsWith("✅") ||
    BenefitKeywords.exists(key.contains)
  }

  private def isIgnorableLine(line: String): Boolean = {
    val trimmed = line.trim
    trimmed.isEmpty ||
    trimmed.startsWith("#") ||
    SourceHandlePattern.matches(trimmed) ||
    normalize(trimmed) == "ish"
  }

  private def normalizedLines(text: String): Vector[String] =
    text
      .replace("\r\n", "\n")
      .replace('\r', '\n')
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .split('\n')
      .toVector
      .map(cleanValue)
      .filter(_.nonEmpty)

  private def multilineValue(lines: List[String]): Option[String] =
    lines.distinct match {
      case Nil => None
      case values => Some(values.mkString("\n"))
    }

  private def cleanTitle(value: String): Option[String] = {
    val cleaned = cleanValue(value)
    Option(cleaned).filter(value => value.nonEmpty && normalize(value) != "placeholder")
  }

  private def cleanValue(value: String): String =
    normalizeWhitespace(
      TrailingDecorationPattern.replaceFirstIn(
        LeadingDecorationPattern.replaceFirstIn(value, ""),
        "",
      )
    )

  private def stripWrappedQuotes(value: String): String =
    value.replace("\"", "").replace("“", "").replace("”", "").replace("«", "").replace("»", "").trim

  private def normalize(value: String): String =
    normalizeWhitespace(
      value
        .replace('’', '\'')
        .replace('ʻ', '\'')
        .replace('ʼ', '\'')
    ).toLowerCase(Locale.ROOT)

  private def normalizeWhitespace(value: String): String =
    value.replaceAll("""\s+""", " ").trim
}
