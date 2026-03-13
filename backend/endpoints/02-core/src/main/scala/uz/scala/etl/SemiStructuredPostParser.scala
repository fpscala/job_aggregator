package uz.scala.etl

import java.util.Locale

import scala.util.matching.Regex

import uz.scala.domain.events.RawJob
import uz.scala.etl.sources.XorazmIshSourceJobEtl

object SemiStructuredPostParser {
  import StructuredPostParser.{Parsed, Rejected}
  import StructuredPostParser.RejectionReason

  private final case class IntroPattern(
      pattern: Regex,
      companyBuilder: Regex.Match => String,
      titleGroup: Option[Int],
    )

  private val TitleMarkers =
    List("ish lavozimi", "lavozim", "vakansiya")

  private val CompanyMarkers =
    List("kompaniya", "korxona", "ish beruvchi", "ish joyi")

  private val SalaryMarkers =
    List("oylik maosh", "ish haqi va motivatsiya", "oylik maoshi", "ish haqi", "maosh", "oylik", "kunlik")

  private val AddressMarkers =
    List("manzil", "adres", "hudud")

  private val LandmarkMarkers =
    List("mo'ljal", "mo‘ljal")

  private val WorkScheduleMarkers =
    List("ish vaqti", "ish vakti", "ish tartibi", "ish grafigi", "ish kuni")

  private val RequirementsMarkers =
    List(
      "talablar",
      "talab etiladi",
      "talab qilinadi",
      "nomzodga talablar",
      "biz kimni qidirmoqdamiz",
      "biz kimni izlayapmiz",
    )

  private val ResponsibilitiesMarkers =
    List(
      "majburiyatlar",
      "vazifalar",
      "vazifasi",
      "lavozim majburiyatlari",
      "asosiy vazifalar",
    )

  private val BenefitsMarkers =
    List(
      "biz taklif qilamiz",
      "bizdan taklif",
      "imkoniyatlar",
      "qulayliklar",
      "sharoitlar",
      "ish sharoitlari",
    )

  private val AdditionalMarkers =
    List("qo'shimcha", "qo'shimcha ma'lumot", "eslatma", "izoh")

  private val ContactMarkers =
    List(
      "murojaat uchun",
      "murojaat",
      "aloqa uchun",
      "aloqa",
      "bog'lanish uchun",
      "bog'lanish",
      "boglanish",
      "telegram",
      "ariza topshirish",
      "qo'shimcha ma'lumot uchun",
      "telefon",
      "tel",
    )

  private val VacancyListMarkers =
    List("bo'sh ish o'rinlari", "vakansiyalar", "vakansiya lar", "ish o'rinlari")

  private val HeaderNoiseMarkers =
    List(
      "ish",
      "vakansiya",
      "ishga taklif",
      "ishga qabul boshlandi",
      "ishga qabul",
      "toshkentdan gapiramiz",
    )

  private val InlineTitleIntroPrefixes =
    List(
      "quyidagi lavozimga",
      "quyidagi lavozimlarga",
      "quyidagi ishchilar",
      "bo'sh ish o'rinlari",
      "vakansiyalar",
    )

  private val MultiRoleSeparators =
    List("/", ";", ",", " va ", " hamda ", " yoki ", " & ")

  private val TimeValuePattern: Regex =
    """\b\d{1,2}[:.]\d{2}\b""".r

  private val ScheduleRatioPattern: Regex =
    """\b\d/\d\b""".r

  private val TelegramUsernamePattern: Regex =
    """(?<![\w@])@([A-Za-z0-9_]{4,})""".r

  private val IntroActionPattern =
    """(?:ishga\s+taklif\s+qilinadi|ishga\s+qabul\s+qilinadi|tanlov\s+asosida\s+ishga\s+qabul\s+qilinadi|ishga\s+taklif\s+etadi|ishga\s+taklif\s+qilamiz|ishga\s+qabul\s+qilamiz|taklif\s+etadi|taklif\s+qilamiz)"""

  private val IntroPatterns: List[IntroPattern] =
    List(
      IntroPattern(
        pattern =
          s"""(?iu)^(.+?)\\s+(jamoasi|jamoasiga)\\s+kengayotganligi\\s+munosabati\\s+bilan\\s+(.+?)\\s+$IntroActionPattern[\\s:!.-]*$$""".r,
        companyBuilder = matched => composeIntroCompany(matched.group(1), "jamoasiga"),
        titleGroup = Some(3),
      ),
      IntroPattern(
        pattern =
          s"""(?iu)^(.+?)\\s+(jamoasiga)\\s+(.+?)\\s+$IntroActionPattern[\\s:!.-]*$$""".r,
        companyBuilder = matched => composeIntroCompany(matched.group(1), matched.group(2)),
        titleGroup = Some(3),
      ),
      IntroPattern(
        pattern =
          s"""(?iu)^(.+?)\\s+(firmasiga|firmaga|korxonasiga|korxonaga|kompaniyasiga|kompaniyaga|do'koni'?ga|do'konga|restoraniga|restoranga|markaziga|markazga|klinikasiga|klinikaga|bog'chasiga|bog'chaga|kafega|cafega|filialiga|filiallariga|muassasasiga|tashkilotiga|mchj\\s*ga|mchjga|ofisiga|offisiga)\\s+(.+?)\\s+$IntroActionPattern[\\s:!.-]*$$""".r,
        companyBuilder = matched => composeIntroCompany(matched.group(1), matched.group(2)),
        titleGroup = Some(3),
      ),
    )

  private val IntroCompanySuffixes =
    Map(
      "firmasiga" -> "firmasi",
      "firmaga" -> "firma",
      "korxonasiga" -> "korxonasi",
      "korxonaga" -> "korxona",
      "kompaniyasiga" -> "kompaniyasi",
      "kompaniyaga" -> "kompaniya",
      "jamoasiga" -> "jamoasi",
      "do'koniga" -> "do'koni",
      "do'koniga" -> "do'koni",
      "do'konga" -> "do'kon",
      "restoraniga" -> "restorani",
      "restoranga" -> "restoran",
      "markaziga" -> "markazi",
      "markazga" -> "markaz",
      "klinikasiga" -> "klinikasi",
      "klinikaga" -> "klinika",
      "bog'chasiga" -> "bog'chasi",
      "bog'chaga" -> "bog'cha",
      "kafega" -> "kafe",
      "cafega" -> "cafe",
      "filialiga" -> "filiali",
      "filiallariga" -> "filiallari",
      "muassasasiga" -> "muassasasi",
      "tashkilotiga" -> "tashkiloti",
      "mchjga" -> "MCHJ",
      "mchj ga" -> "MCHJ",
      "ofisiga" -> "ofisi",
      "offisiga" -> "offisi",
    )

  def parse(rawJob: RawJob): Either[Rejected, Parsed] = {
    val lines = normalizeLines(rawJob.description).toVector
    val extractedDetails = XorazmIshSourceJobEtl.enrich(rawJob)
    val details =
      extractedDetails.copy(
        workSchedule = extractedDetails.workSchedule.orElse(extractWorkSchedule(lines)),
        contactText =
          sanitizeContactText(
            extractedDetails.contactText,
            extractedDetails.contactTelegramUsernames,
          ),
      )

    for {
      title <- extractTitle(lines)
      company <- extractCompany(lines)
      _ <-
        if (hasMultipleRoleSignals(lines)) Left(Rejected(RejectionReason.MultipleRolesDetected))
        else Right(())
      _ <-
        if (!details.hasContacts) Left(Rejected(RejectionReason.MissingContact))
        else Right(())
      salary = extractMetadataValue(lines, SalaryMarkers, looksLikeSalaryContinuation)
      location = extractLocation(lines)
      optionalFieldCount =
        List(
          salary,
          location,
          details.workSchedule,
          details.requirements,
          details.responsibilities,
          details.benefits,
          details.additional,
          details.contactText,
        ).count(_.nonEmpty)
      _ <-
        if (!containsJobSignal(lines)) Left(Rejected(RejectionReason.MissingStructuredLabels))
        else if (optionalFieldCount < 2) Left(Rejected(RejectionReason.TooFewOptionalFields))
        else Right(())
    } yield Parsed(
      title = title,
      company = company,
      salary = salary,
      location = location,
      details = details,
    )
  }

  private def extractTitle(lines: Vector[String]): Either[Rejected, String] = {
    val titleFromLabel =
      firstStrictMarkerValue(lines, TitleMarkers).filter(_.nonEmpty)

    titleFromLabel match {
      case Some(value) =>
        validateTitle(value)
      case None =>
        val headerLines = headerLinesOnly(lines)
        val introTitle =
          headerLines.flatMap(extractIntroFacts).flatMap(_.title.toList).headOption
        val roleCandidates = headerLines.filter(isRoleCandidate)

        roleCandidates.distinct match {
          case Vector(single) =>
            validateTitle(single)
          case Vector() if introTitle.nonEmpty =>
            validateTitle(introTitle.get)
          case values if values.size > 1 =>
            Left(Rejected(RejectionReason.MultipleRolesDetected))
          case _ =>
            Left(Rejected(RejectionReason.MissingTitle))
        }
    }
  }

  private def extractCompany(lines: Vector[String]): Either[Rejected, String] = {
    val labeled = firstStrictMarkerValue(lines, CompanyMarkers).map(stripWrappedQuotes)
    val intro =
      headerLinesOnly(lines)
        .flatMap(extractIntroFacts)
        .flatMap(_.company.toList)
        .headOption

    labeled.orElse(intro).filter(_.nonEmpty).toRight(Rejected(RejectionReason.MissingCompany))
  }

  private def extractLocation(lines: Vector[String]): Option[String] = {
    val address = extractMetadataValue(lines, AddressMarkers, looksLikeMetadataContinuation)
    val landmark = extractMetadataValue(lines, LandmarkMarkers, _ => false)

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

  private def extractMetadataValue(
      lines: Vector[String],
      markers: List[String],
      continuation: String => Boolean,
    ): Option[String] =
    lines.indexWhere(startsWithAny(_, markers)) match {
      case -1 =>
        None
      case index =>
        val head = stripMarkersFromLine(lines(index), markers)
        val tail =
          lines
            .drop(index + 1)
            .takeWhile(line => !isBodyBoundary(line) && continuation(line))
            .toList

        val values =
          (head :: tail)
            .map(cleanContentLine)
            .filter(_.nonEmpty)

        Option(values.mkString("\n")).filter(_.nonEmpty)
    }

  private def firstStrictMarkerValue(lines: Vector[String], markers: List[String]): Option[String] =
    lines.zipWithIndex.collectFirst {
      case (line, index) if startsWithStrictLabel(line, markers) =>
        val value = stripMarkersFromLine(line, markers)
        if (value.nonEmpty) value
        else lines.lift(index + 1).map(cleanContentLine).getOrElse("")
    }.map(normalizeWhitespace).filter(_.nonEmpty)

  private def containsJobSignal(lines: Vector[String]): Boolean = {
    val text = lines.mkString("\n").toLowerCase(Locale.ROOT)

    TitleMarkers.exists(marker => text.contains(marker)) ||
    text.contains("ishga taklif") ||
    text.contains("ishga qabul") ||
    text.contains("vakansiya")
  }

  private def hasMultipleRoleSignals(lines: Vector[String]): Boolean = {
    val headerRoleCandidates = headerLinesOnly(lines).filter(isRoleCandidate).distinct
    val vacancyListCount =
      lines.indexWhere(startsWithAny(_, VacancyListMarkers)) match {
        case -1 => 0
        case index =>
          lines
            .drop(index + 1)
            .takeWhile(line => !isBodyBoundary(line))
            .count(isVacancyRoleLine)
      }

    headerRoleCandidates.size > 1 ||
    vacancyListCount > 1 ||
    headerRoleCandidates.exists(looksLikeMultiRole)
  }

  private def headerLinesOnly(lines: Vector[String]): Vector[String] = {
    val boundaryIndex =
      lines.indexWhere(isBodyBoundary) match {
        case -1 => lines.length
        case index => index
      }

    lines
      .take(boundaryIndex)
      .map(cleanContentLine)
      .filterNot(isHeaderNoiseLine)
      .filter(_.nonEmpty)
  }

  private def isHeaderNoiseLine(value: String): Boolean = {
    val normalized = normalize(value)

    HeaderNoiseMarkers.contains(normalized) ||
    normalized.forall(ch => !ch.isLetterOrDigit)
  }

  private def isRoleCandidate(value: String): Boolean = {
    val cleaned = cleanContentLine(value)
    val normalized = normalize(cleaned)

    cleaned.nonEmpty &&
    !isHeaderNoiseLine(cleaned) &&
    !looksLikeIntroLine(cleaned) &&
    !isBodyBoundary(cleaned) &&
    !cleaned.endsWith(".") &&
    cleaned.split("\\s+").length <= 8
  }

  private def isVacancyRoleLine(value: String): Boolean = {
    val cleaned = cleanContentLine(value)

    cleaned.matches("""^\d+[.)].+""") ||
    cleaned.startsWith("•") ||
    cleaned.startsWith("▪") ||
    cleaned.startsWith("✍") ||
    cleaned.startsWith("⬛") ||
    cleaned.startsWith("🔹")
  }

  private def looksLikeIntroLine(value: String): Boolean = {
    val normalized = normalize(value)

    normalized.contains("ishga taklif") ||
    normalized.contains("ishga qabul") ||
    normalized.contains("taklif etadi") ||
    normalized.contains("taklif qilamiz") ||
    normalized.contains("qidirmoqdamiz")
  }

  private def extractIntroFacts(value: String): Option[IntroFacts] = {
    val normalizedLine =
      stripWrappedQuotes(cleanContentLine(value))
        .replaceAll("""(?iu)(\p{L})'ga\b""", "$1ga")

    IntroPatterns.view.flatMap { introPattern =>
      introPattern.pattern.findFirstMatchIn(normalizedLine).map { matched =>
        val company = introPattern.companyBuilder(matched)
        val title =
          introPattern.titleGroup
            .flatMap(groupIndex => Option(matched.group(groupIndex)))
            .map(cleanContentLine)
            .map(stripWrappedQuotes)
            .filterNot(isInlineTitlePrefix)

        IntroFacts(
          company = Option(company).map(normalizeWhitespace).filter(_.nonEmpty),
          title = title.filter(_.nonEmpty),
        )
      }
    }.headOption
  }

  private final case class IntroFacts(
      company: Option[String],
      title: Option[String],
    )

  private def composeIntroCompany(prefix: String, suffix: String): String = {
    val normalizedPrefix =
      stripWrappedQuotes(cleanContentLine(prefix))
        .replaceAll("""[\"“”«»]+""", "")
        .replaceAll("""'{2,}""", " ")
        .trim
    val normalizedSuffix =
      IntroCompanySuffixes.getOrElse(normalize(suffix), cleanContentLine(suffix))

    normalizeWhitespace(s"$normalizedPrefix $normalizedSuffix")
  }

  private def isInlineTitlePrefix(value: String): Boolean = {
    val normalized = normalize(value)

    InlineTitleIntroPrefixes.exists(normalized.startsWith)
  }

  private def validateTitle(value: String): Either[Rejected, String] = {
    val normalizedTitle = stripWrappedQuotes(cleanContentLine(value))

    if (normalizedTitle.isEmpty) Left(Rejected(RejectionReason.MissingTitle))
    else if (looksLikeMultiRole(normalizedTitle)) Left(Rejected(RejectionReason.MultipleRolesDetected))
    else Right(normalizedTitle)
  }

  private def looksLikeMultiRole(value: String): Boolean = {
    val normalized = s" ${normalize(value)} "

    MultiRoleSeparators.exists(normalized.contains)
  }

  private def stripMarkersFromLine(value: String, markers: List[String]): String =
    markers.sortBy(-_.length).foldLeft(cleanContentLine(value)) { case (current, marker) =>
      if (startsWithAny(current, List(marker)))
        current.replaceFirst(s"(?iu)^\\Q$marker\\E(?:\\s*[:\\-–—]\\s*|\\s+)?", "")
      else current
    }.trim

  private def extractWorkSchedule(lines: Vector[String]): Option[String] =
    lines.indexWhere(startsWithAny(_, WorkScheduleMarkers)) match {
      case -1 =>
        None
      case index =>
        val rawHead = cleanContentLine(lines(index))
        val head =
          if (normalize(rawHead).contains("oylik")) rawHead
          else stripMarkersFromLine(rawHead, WorkScheduleMarkers)
        val tail =
          lines
            .drop(index + 1)
            .takeWhile(line => !isBodyBoundary(line) && looksLikeWorkSchedule(line))
            .toList

        val values =
          (head :: tail)
            .map(cleanContentLine)
            .filter(line => line.nonEmpty && looksLikeWorkSchedule(line))
            .distinct

        Option(values.mkString("\n")).filter(_.nonEmpty)
    }

  private def sanitizeContactText(
      value: Option[String],
      usernames: List[String],
    ): Option[String] =
    value
      .toList
      .flatMap(_.linesIterator.toList)
      .map { line =>
        usernames.foldLeft(line) { case (current, username) =>
          current
            .replace(s"@$username", " ")
            .replace(username, " ")
        }
      }
      .map(normalizeWhitespace)
      .map(line => normalizeWhitespace(line.replaceAll("""^[()\[\]]+|[()\[\]]+$""", "")))
      .filter(_.nonEmpty)
      .distinct match {
      case Nil => None
      case lines => Some(lines.mkString("\n"))
    }

  private def looksLikeMetadataContinuation(value: String): Boolean = {
    val trimmed = cleanContentLine(value)

    trimmed.startsWith("(") || trimmed.startsWith("（")
  }

  private def looksLikeSalaryContinuation(value: String): Boolean = {
    val key = normalize(value)

    key.nonEmpty &&
    !isBodyBoundary(value) &&
    (key.exists(_.isDigit) ||
      key.contains("som") ||
      key.contains("so'm") ||
      key.contains("so‘m") ||
      key.contains("mln") ||
      key.contains("million") ||
      key.contains("ming") ||
      key.contains("suhbat") ||
      key.contains("kelish") ||
      key.contains("bonus") ||
      key.contains("foiz") ||
      key.contains("kpi") ||
      key.contains("qarab") ||
      key.contains("belgilanadi"))
  }

  private def isBodyBoundary(value: String): Boolean =
    startsWithAny(
      value,
      TitleMarkers ++
        CompanyMarkers ++
        SalaryMarkers ++
        AddressMarkers ++
        LandmarkMarkers ++
        WorkScheduleMarkers ++
        RequirementsMarkers ++
        ResponsibilitiesMarkers ++
        BenefitsMarkers ++
        AdditionalMarkers ++
        ContactMarkers,
    ) || containsBareContact(value)

  private def containsBareContact(value: String): Boolean = {
    val cleaned = cleanContentLine(value)

    cleaned.matches("""^(?:[☎📞📱\s]+)?\+?\d[\d\s\-()]{6,}\d$""") ||
    TelegramUsernamePattern.findFirstIn(cleaned).nonEmpty
  }

  private def startsWithAny(value: String, markers: List[String]): Boolean = {
    val key = normalize(value)

    markers.sortBy(-_.length).exists { marker =>
      val normalizedMarker = normalize(marker)
      key.startsWith(normalizedMarker) && {
        val rest = key.drop(normalizedMarker.length)
        rest.isEmpty || rest.startsWith(" ") || rest.startsWith(":") || rest.startsWith("-") || rest.startsWith("–")
      }
    }
  }

  private def startsWithStrictLabel(value: String, markers: List[String]): Boolean = {
    val key = normalize(value)

    markers.sortBy(-_.length).exists { marker =>
      val normalizedMarker = normalize(marker)
      key == normalizedMarker ||
      key.startsWith(s"$normalizedMarker:") ||
      key.startsWith(s"$normalizedMarker -") ||
      key.startsWith(s"$normalizedMarker –")
    }
  }

  private def cleanContentLine(value: String): String =
    normalizeWhitespace(
      stripDecorations(
        stripBullet(value)
      )
    )

  private def stripDecorations(value: String): String =
    value
      .replace("\uFE0F", "")
      .replace("\u200D", "")
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .replaceAll("""^[\s\p{Punct}\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾🗺🧩✍️☝️⬇️⚠️]+""", "")
      .replaceAll("""[\s\p{Punct}\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾🗺🧩✍️☝️⬇️⚠️]+$""", "")

  private def stripBullet(value: String): String =
    value.replaceFirst("""(?u)^\s*(?:[•▪◦●✔✅❗👤👉➤▶✓\-✍️🔹⬛]+|\d+[.)])\s*""", "")

  private def stripWrappedQuotes(value: String): String =
    value.trim.replaceAll("""^[\"'“”«»„‟]+|[\"'“”«»„‟]+$""", "").trim

  private def normalizeWhitespace(value: String): String =
    value
      .replace('’', '\'')
      .replace('‘', '\'')
      .replace('ʻ', '\'')
      .replace('ʼ', '\'')
      .replace('`', '\'')
      .replaceAll("""\s+""", " ")
      .trim

  private def looksLikeWorkSchedule(value: String): Boolean = {
    val key = normalize(value)

    TimeValuePattern.findFirstIn(value).nonEmpty ||
    ScheduleRatioPattern.findFirstIn(key).nonEmpty ||
    key.contains("haftada") ||
    key.contains("yakshanba") ||
    key.contains("smena") ||
    key.contains("dam olish") ||
    key.contains("grafik") ||
    key.contains("to'liq stavka") ||
    key.contains("to‘liq stavka") ||
    key.contains("oylik")
  }

  private def normalize(value: String): String =
    normalizeWhitespace(value).toLowerCase(Locale.ROOT)

  private def normalizeLines(text: String): List[String] =
    text
      .replace("\r\n", "\n")
      .replace('\r', '\n')
      .split('\n')
      .toList
      .map(normalizeWhitespace)
      .filter(_.nonEmpty)
}
