package uz.scala.etl

import java.util.Locale

import scala.util.matching.Regex

import uz.scala.domain.events.RawJob

object FieldOnlyStructuredPostParser {
  import StructuredPostParser.{Parsed, Rejected}
  import StructuredPostParser.RejectionReason

  private val TitleMarkers =
    List("ish lavozimi", "lavozim", "vakansiya", "иш лавозими", "лавозим", "вакансия")

  private val SalaryMarkers =
    List("ish haqi", "maosh", "oylik", "kunlik", "иш ҳақи", "маош", "ойлик", "зарплата")

  private val LocationMarkers =
    List("manzil", "hudud", "адрес", "манзил", "худуд")

  private val LandmarkMarkers =
    List("mo'ljal", "mo‘ljal", "mўлжал", "ориентир")

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

  private val StructureMarkers =
    TitleMarkers ++
      SalaryMarkers ++
      LocationMarkers ++
      LandmarkMarkers ++
      ContactMarkers ++
      List(
        "talablar",
        "vazifalar",
        "qulayliklar",
        "qo'shimcha",
        "ish vaqti",
        "талаблар",
        "вазифалар",
        "қулайликлар",
        "қўшимча",
        "иш вақти",
      )

  private val LeadingDecorationPattern: Regex =
    """^[\s\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾\uFE0F]+""".r

  private val TrailingDecorationPattern: Regex =
    """[\s\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾\uFE0F]+$""".r

  def parse(rawJob: RawJob): Either[Rejected, Parsed] = {
    val lines = normalizedLines(rawJob.description)
    val extractedDetails = SourceJobEtls.enrich(rawJob)
    val labelCount =
      lines.count(line => startsWithAny(line, StructureMarkers))

    val title =
      extractLabeledValue(lines, TitleMarkers)
        .orElse(cleanTitle(rawJob.title))
    val salary =
      rawJob.salary.map(cleanValue).filter(_.nonEmpty).orElse(extractLabeledValue(lines, SalaryMarkers))
    val location =
      rawJob.location.map(cleanValue).filter(_.nonEmpty).orElse(extractLocation(lines))
    val optionalFieldCount =
      List(
        salary,
        location,
        extractedDetails.workSchedule,
        extractedDetails.requirements,
        extractedDetails.responsibilities,
        extractedDetails.benefits,
        extractedDetails.additional,
      ).count(_.nonEmpty)

    if (labelCount < 4)
      Left(Rejected(RejectionReason.MissingStructuredLabels))
    else if (title.isEmpty)
      Left(Rejected(RejectionReason.MissingTitle))
    else if (!extractedDetails.hasContacts)
      Left(Rejected(RejectionReason.MissingContact))
    else if (optionalFieldCount < 3)
      Left(Rejected(RejectionReason.TooFewOptionalFields))
    else
      Right(
        Parsed(
          title = title.get,
          company = None,
          salary = salary,
          location = location,
          details = extractedDetails,
        )
      )
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
