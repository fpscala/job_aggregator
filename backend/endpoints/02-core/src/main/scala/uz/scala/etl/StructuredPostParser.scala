package uz.scala.etl

import java.util.Locale
import java.util.regex.Pattern

import scala.util.matching.Regex

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails

object StructuredPostParser {
  final case class Parsed(
      title: String,
      company: String,
      salary: Option[String],
      location: Option[String],
      details: JobDetails,
    )

  final case class Rejected(reason: RejectionReason)

  sealed trait RejectionReason {
    def code: String
  }

  object RejectionReason {
    case object MissingStructuredLabels extends RejectionReason {
      override val code: String = "missing_structured_labels"
    }

    case object MissingTitle extends RejectionReason {
      override val code: String = "missing_title"
    }

    case object NoisyHeader extends RejectionReason {
      override val code: String = "noisy_header"
    }

    case object MultipleRolesDetected extends RejectionReason {
      override val code: String = "multiple_roles_detected"
    }

    case object MissingCompany extends RejectionReason {
      override val code: String = "missing_company"
    }

    case object MissingPhone extends RejectionReason {
      override val code: String = "missing_phone"
    }

    case object TooFewOptionalFields extends RejectionReason {
      override val code: String = "too_few_optional_fields"
    }
  }

  private sealed trait LabelKind {
    def key: String
    def countsAsStructured: Boolean
    def countsAsOptional: Boolean
  }

  private object LabelKind {
    case object Company extends LabelKind {
      override val key: String = "company"
      override val countsAsStructured: Boolean = true
      override val countsAsOptional: Boolean = false
    }

    case object Salary extends LabelKind {
      override val key: String = "salary"
      override val countsAsStructured: Boolean = true
      override val countsAsOptional: Boolean = true
    }

    case object Address extends LabelKind {
      override val key: String = "address"
      override val countsAsStructured: Boolean = true
      override val countsAsOptional: Boolean = true
    }

    case object WorkTime extends LabelKind {
      override val key: String = "work_time"
      override val countsAsStructured: Boolean = true
      override val countsAsOptional: Boolean = true
    }

    case object Requirements extends LabelKind {
      override val key: String = "requirements"
      override val countsAsStructured: Boolean = true
      override val countsAsOptional: Boolean = true
    }

    case object Benefits extends LabelKind {
      override val key: String = "benefits"
      override val countsAsStructured: Boolean = true
      override val countsAsOptional: Boolean = true
    }

    case object Phone extends LabelKind {
      override val key: String = "phone"
      override val countsAsStructured: Boolean = true
      override val countsAsOptional: Boolean = false
    }

    case object Application extends LabelKind {
      override val key: String = "application"
      override val countsAsStructured: Boolean = true
      override val countsAsOptional: Boolean = true
    }

    case object Responsibilities extends LabelKind {
      override val key: String = "responsibilities"
      override val countsAsStructured: Boolean = false
      override val countsAsOptional: Boolean = false
    }

    case object Additional extends LabelKind {
      override val key: String = "additional"
      override val countsAsStructured: Boolean = false
      override val countsAsOptional: Boolean = false
    }
  }

  private final case class LabelAlias(kind: LabelKind, alias: String) {
    private val quotedAlias = Pattern.quote(alias)

    private val withDelimiter =
      (s"(?iu)^$quotedAlias\\s*[:\\-–—]\\s*(.*)$$").r

    private val standalone =
      (s"(?iu)^$quotedAlias\\s*$$").r

    def detect(line: String): Option[DetectedLabel] =
      line match {
        case withDelimiter(value) =>
          Some(DetectedLabel(kind = kind, inlineValue = normalizeWhitespace(value)))
        case standalone() =>
          Some(DetectedLabel(kind = kind, inlineValue = ""))
        case _ =>
          None
      }
  }

  private final case class DetectedLabel(
      kind: LabelKind,
      inlineValue: String,
    )

  private final case class Section(
      kind: LabelKind,
      lines: List[String],
    )

  private val LabelAliases: List[LabelAlias] =
    List(
      LabelAlias(LabelKind.Company, "kompaniya"),
      LabelAlias(LabelKind.Company, "korxona"),
      LabelAlias(LabelKind.Company, "компания"),
      LabelAlias(LabelKind.Company, "корхона"),
      LabelAlias(LabelKind.Salary, "maosh"),
      LabelAlias(LabelKind.Salary, "oylik"),
      LabelAlias(LabelKind.Salary, "ish haqi"),
      LabelAlias(LabelKind.Salary, "иш ҳақи"),
      LabelAlias(LabelKind.Salary, "маош"),
      LabelAlias(LabelKind.Salary, "ойлик"),
      LabelAlias(LabelKind.Salary, "зарплата"),
      LabelAlias(LabelKind.Address, "manzil"),
      LabelAlias(LabelKind.Address, "adres"),
      LabelAlias(LabelKind.Address, "манзил"),
      LabelAlias(LabelKind.Address, "адрес"),
      LabelAlias(LabelKind.WorkTime, "ish vaqti"),
      LabelAlias(LabelKind.WorkTime, "ish tartibi"),
      LabelAlias(LabelKind.WorkTime, "ish grafigi"),
      LabelAlias(LabelKind.WorkTime, "иш вақти"),
      LabelAlias(LabelKind.WorkTime, "иш тартиби"),
      LabelAlias(LabelKind.WorkTime, "иш графиги"),
      LabelAlias(LabelKind.Requirements, "talablar"),
      LabelAlias(LabelKind.Requirements, "umumiy talablar"),
      LabelAlias(LabelKind.Requirements, "талаблар"),
      LabelAlias(LabelKind.Requirements, "умумий талаблар"),
      LabelAlias(LabelKind.Requirements, "требования"),
      LabelAlias(LabelKind.Benefits, "qulayliklar"),
      LabelAlias(LabelKind.Benefits, "sharoitlar"),
      LabelAlias(LabelKind.Benefits, "biz taklif qilamiz"),
      LabelAlias(LabelKind.Benefits, "қулайликлар"),
      LabelAlias(LabelKind.Benefits, "шароитлар"),
      LabelAlias(LabelKind.Benefits, "биз таклиф қиламиз"),
      LabelAlias(LabelKind.Benefits, "мы предлагаем"),
      LabelAlias(LabelKind.Phone, "telefon raqami"),
      LabelAlias(LabelKind.Phone, "telefon"),
      LabelAlias(LabelKind.Phone, "tel"),
      LabelAlias(LabelKind.Phone, "телефон рақами"),
      LabelAlias(LabelKind.Phone, "телефон"),
      LabelAlias(LabelKind.Phone, "тел"),
      LabelAlias(LabelKind.Application, "murojaat uchun"),
      LabelAlias(LabelKind.Application, "murojaat"),
      LabelAlias(LabelKind.Application, "aloqa uchun"),
      LabelAlias(LabelKind.Application, "aloqa"),
      LabelAlias(LabelKind.Application, "bog'lanish"),
      LabelAlias(LabelKind.Application, "boglanish"),
      LabelAlias(LabelKind.Application, "ariza topshirish"),
      LabelAlias(LabelKind.Application, "мурожаат учун"),
      LabelAlias(LabelKind.Application, "мурожаат"),
      LabelAlias(LabelKind.Application, "алоқа учун"),
      LabelAlias(LabelKind.Application, "алоқа"),
      LabelAlias(LabelKind.Application, "боғланиш"),
      LabelAlias(LabelKind.Application, "ариза топшириш"),
      LabelAlias(LabelKind.Responsibilities, "vazifalar"),
      LabelAlias(LabelKind.Responsibilities, "vazifasi"),
      LabelAlias(LabelKind.Responsibilities, "вазифалар"),
      LabelAlias(LabelKind.Responsibilities, "вазифаси"),
      LabelAlias(LabelKind.Responsibilities, "обязанности"),
      LabelAlias(LabelKind.Additional, "qo'shimcha"),
      LabelAlias(LabelKind.Additional, "qo‘shimcha"),
      LabelAlias(LabelKind.Additional, "eslatma"),
      LabelAlias(LabelKind.Additional, "қўшимча"),
      LabelAlias(LabelKind.Additional, "эслатма"),
    ).sortBy(alias => -alias.alias.length)

  private val VisibleUrlPattern: Regex =
    """(?i)\b(?:https?://|t\.me/)[^\s<>()]+""".r

  private val TelegramUsernamePattern: Regex =
    """(?<![\w@])@([A-Za-z0-9_]{4,})""".r

  private val PhonePattern: Regex =
    """(?iu)(?:\+?\d[\d \t()\-]{6,}\d)""".r

  private val SourceUrlPattern: Regex =
    """https?://t\.me/([^/\s]+)/\d+""".r

  private val LeadingDecorationPattern: Regex =
    """^[\s\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾]+""".r

  private val TrailingDecorationPattern: Regex =
    """[\s\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾]+$""".r

  private val BulletPrefixPattern: Regex =
    """^\s*(?:[-–—•*▪◦●✔✅☑➤▶✓📌📍📞📋📨📱✨]+|\d+[.)])\s*""".r

  private val SeparatorLinePattern: Regex =
    """^\s*[-=_~]{3,}\s*$""".r

  private val HashtagNoisePattern: Regex =
    """(?iu)^#\S+$""".r

  private val MultiRoleSeparators =
    List("/", ";", ",", " va ", " hamda ", " yoki ", " & ")

  def parse(rawJob: RawJob): Either[Rejected, Parsed] = {
    val ignoredUsernames = ignoredTelegramUsernames(rawJob)
    val lines = normalizedLines(rawJob.description)
    val trimmedLines = trimIgnorableEdges(lines, ignoredUsernames, rawJob.url)

    if (trimmedLines.isEmpty)
      Left(Rejected(RejectionReason.MissingStructuredLabels))
    else {
      val labeledLines =
        trimmedLines.zipWithIndex.flatMap {
          case (line, index) =>
            detectLabel(line).map(index -> _)
        }

      val structuredLabelCount =
        labeledLines
          .collect { case (_, detected) if detected.kind.countsAsStructured => detected.kind.key }
          .distinct
          .size

      if (structuredLabelCount < 3)
        Left(Rejected(RejectionReason.MissingStructuredLabels))
      else {
        val firstLabelIndex = labeledLines.map(_._1).min
        val headerLines = trimmedLines.take(firstLabelIndex).filterNot(isIgnorableLine(_, ignoredUsernames, rawJob.url))

        if (headerLines.isEmpty)
          Left(Rejected(RejectionReason.MissingTitle))
        else if (headerLines.size > 1)
          Left(Rejected(RejectionReason.NoisyHeader))
        else {
          val title = cleanTitle(headerLines.head)

          if (title.isEmpty)
            Left(Rejected(RejectionReason.MissingTitle))
          else if (looksLikeMultiRole(title))
            Left(Rejected(RejectionReason.MultipleRolesDetected))
          else {
            val sections = buildSections(trimmedLines, labeledLines)

            val company = compactValue(sectionLines(sections, LabelKind.Company))
            val salary = compactValue(sectionLines(sections, LabelKind.Salary))
            val location = compactValue(sectionLines(sections, LabelKind.Address))
            val workSchedule = multilineValue(sectionLines(sections, LabelKind.WorkTime))
            val requirements = multilineValue(sectionLines(sections, LabelKind.Requirements))
            val benefits = multilineValue(sectionLines(sections, LabelKind.Benefits))
            val responsibilities = multilineValue(sectionLines(sections, LabelKind.Responsibilities))
            val additional = multilineValue(sectionLines(sections, LabelKind.Additional))

            val applicationLines = sectionLines(sections, LabelKind.Application)
            val phoneLines = sectionLines(sections, LabelKind.Phone)
            val contactSourceText = (applicationLines ++ phoneLines).mkString("\n")
            val hasApplication = applicationLines.map(cleanContentLine).exists(_.nonEmpty)
            val phoneNumbers = extractPhoneNumbers(contactSourceText)
            val usernames = extractTelegramUsernames(contactSourceText, ignoredUsernames)
            val visibleLinks =
              extractVisibleLinks(
                value = contactSourceText,
                ignoredUsernames = ignoredUsernames,
                messageUrl = rawJob.url,
              )
            val hiddenLinks =
              if (hasApplication) rawJob.contactLinks.getOrElse(Nil)
              else Nil
            val contactLinks = distinctPreservingOrder(hiddenLinks ++ visibleLinks)
            val applicationText =
              sanitizeContactText(multilineValue(applicationLines), phoneNumbers, usernames, contactLinks)

            val optionalFieldCount =
              List(salary, location, workSchedule, requirements, benefits).count(_.nonEmpty) +
                (if (hasApplication) 1 else 0)

            if (company.isEmpty)
              Left(Rejected(RejectionReason.MissingCompany))
            else if (phoneNumbers.isEmpty)
              Left(Rejected(RejectionReason.MissingPhone))
            else if (optionalFieldCount < 2)
              Left(Rejected(RejectionReason.TooFewOptionalFields))
            else
              Right(
                Parsed(
                  title = title,
                  company = company.get,
                  salary = salary,
                  location = location,
                  details =
                    JobDetails(
                      requirements = requirements,
                      responsibilities = responsibilities,
                      benefits = benefits,
                      additional = additional,
                      workSchedule = workSchedule,
                      contactText = applicationText,
                      contactPhoneNumbers = phoneNumbers,
                      contactTelegramUsernames = usernames,
                      contactLinks = contactLinks,
                    ),
                )
              )
          }
        }
      }
    }
  }

  private def buildSections(
      lines: Vector[String],
      labeledLines: Vector[(Int, DetectedLabel)],
    ): List[Section] =
    labeledLines.zipWithIndex.map {
      case ((index, detected), position) =>
        val endExclusive =
          labeledLines.lift(position + 1).map(_._1).getOrElse(lines.length)
        val rawLines =
          (Option(detected.inlineValue).filter(_.nonEmpty).toList ++
            lines.slice(index + 1, endExclusive).toList)
            .map(normalizeWhitespace)
            .filter(_.nonEmpty)

        Section(kind = detected.kind, lines = rawLines)
    }.toList

  private def sectionLines(sections: List[Section], kind: LabelKind): List[String] =
    sections.collect { case Section(`kind`, lines) => lines }.flatten

  private def compactValue(lines: List[String]): Option[String] =
    Option(lines.map(cleanContentLine).filter(_.nonEmpty).mkString(" "))
      .map(normalizeWhitespace)
      .filter(_.nonEmpty)

  private def multilineValue(lines: List[String]): Option[String] =
    Option(lines.map(cleanContentLine).filter(_.nonEmpty).distinct.mkString("\n"))
      .map(normalizeMultiline)
      .filter(_.nonEmpty)

  private def sanitizeContactText(
      value: Option[String],
      phoneNumbers: List[String],
      usernames: List[String],
      links: List[String],
    ): Option[String] = {
    val normalizedPhones =
      phoneNumbers.map(phoneDigits).filter(_.nonEmpty).toSet
    val normalizedUsernames =
      usernames.map(_.trim.toLowerCase(Locale.ROOT).stripPrefix("@")).filter(_.nonEmpty).toSet
    val normalizedLinks =
      links.map(_.trim.toLowerCase(Locale.ROOT)).filter(_.nonEmpty).toSet

    value
      .map(normalizeMultiline)
      .toList
      .flatMap(_.linesIterator.toList)
      .map(cleanContentLine)
      .filter(_.nonEmpty)
      .filterNot { line =>
        val normalized = line.toLowerCase(Locale.ROOT)
        val digits = phoneDigits(line)
        normalizedLinks.contains(normalized) ||
        normalizedPhones.contains(digits) ||
        normalizedUsernames.contains(normalized.stripPrefix("@")) ||
        stripExtractedContacts(normalized, phoneNumbers, usernames, links).isEmpty
      }
      .distinct match {
      case Nil => None
      case lines => Some(lines.mkString("\n"))
    }
  }

  private def stripExtractedContacts(
      value: String,
      phoneNumbers: List[String],
      usernames: List[String],
      links: List[String],
    ): String = {
    val withoutLinks =
      links.foldLeft(value) { case (current, link) =>
        current.replace(link.toLowerCase(Locale.ROOT), " ")
      }
    val withoutUsernames =
      usernames.foldLeft(withoutLinks) { case (current, username) =>
        current
          .replace(s"@${username.toLowerCase(Locale.ROOT)}", " ")
          .replace(username.toLowerCase(Locale.ROOT), " ")
      }
    val withoutPhones =
      phoneNumbers.foldLeft(withoutUsernames) { case (current, phone) =>
        current.replace(phone.toLowerCase(Locale.ROOT), " ")
      }

    normalizeWhitespace(withoutPhones.replaceAll("""[+]?[\d\s()\-]{7,}""", " "))
  }

  private def extractPhoneNumbers(value: String): List[String] =
    distinctPreservingOrder(
      PhonePattern
        .findAllMatchIn(value)
        .flatMap(matchResult => normalizePhoneNumber(matchResult.matched))
        .toList
    )

  private def normalizePhoneNumber(value: String): Option[String] = {
    val trimmed = normalizeWhitespace(value)
    val digits = trimmed.filter(_.isDigit)

    if (digits.length < 7) None
    else if (trimmed.startsWith("+")) Some(s"+$digits")
    else if (digits.startsWith("998") && digits.length == 12) Some(s"+$digits")
    else Some(digits)
  }

  private def extractTelegramUsernames(value: String, ignoredUsernames: Set[String]): List[String] =
    distinctPreservingOrder(
      TelegramUsernamePattern
        .findAllMatchIn(value)
        .map(_.group(1))
        .map(_.trim)
        .filter(_.nonEmpty)
        .filterNot(username => ignoredUsernames.contains(username.toLowerCase(Locale.ROOT)))
        .toList
    )

  private def extractVisibleLinks(
      value: String,
      ignoredUsernames: Set[String],
      messageUrl: String,
    ): List[String] =
    {
      val normalizedMessageUrl = normalizeLink(messageUrl).getOrElse("")

      distinctPreservingOrder(
        VisibleUrlPattern
          .findAllMatchIn(value)
          .flatMap(matchResult => normalizeLink(matchResult.matched))
          .filterNot(_ == normalizedMessageUrl)
          .filterNot(link => isSourceTelegramLink(link, ignoredUsernames))
          .toList
      )
    }

  private def normalizeLink(value: String): Option[String] = {
    val trimmed =
      value.trim
        .stripPrefix("<")
        .stripSuffix(">")
        .stripSuffix(")")
        .stripSuffix("]")
        .replaceAll("""[.,;:!?]+$""", "")
    val normalized =
      if (trimmed.toLowerCase(Locale.ROOT).startsWith("t.me/")) s"https://$trimmed"
      else trimmed

    if (normalized.startsWith("http://") || normalized.startsWith("https://")) Some(normalized)
    else None
  }

  private def isSourceTelegramLink(link: String, ignoredUsernames: Set[String]): Boolean =
    """(?iu)^https?://(?:www\.)?(?:t\.me|telegram\.me)/([^/\s?#]+)/?.*$"""
      .r
      .findFirstMatchIn(link)
      .exists(matchResult => ignoredUsernames.contains(matchResult.group(1).toLowerCase(Locale.ROOT)))

  private def detectLabel(line: String): Option[DetectedLabel] = {
    val stripped = stripDecorations(line)

    LabelAliases.view.flatMap(_.detect(stripped)).headOption
  }

  private def looksLikeMultiRole(title: String): Boolean = {
    val normalized = s" ${normalizeWhitespace(title).toLowerCase(Locale.ROOT)} "

    MultiRoleSeparators.exists(normalized.contains)
  }

  private def cleanTitle(value: String): String =
    stripDecorations(value)
      .replaceAll("""(?iu)^lavozim\s*[:\-–—]\s*""", "")
      .replaceAll("""(?iu)^ish\s+lavozimi\s*[:\-–—]\s*""", "")
      .trim

  private def cleanContentLine(value: String): String =
    normalizeWhitespace(
      TrailingDecorationPattern.replaceAllIn(
        BulletPrefixPattern.replaceFirstIn(value, ""),
        "",
      )
    )

  private def normalizedLines(value: String): Vector[String] =
    value
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .replace('\r', '\n')
      .linesIterator
      .map(normalizeWhitespace)
      .filter(_.nonEmpty)
      .toVector

  private def trimIgnorableEdges(
      lines: Vector[String],
      ignoredUsernames: Set[String],
      messageUrl: String,
    ): Vector[String] = {
    val start = lines.indexWhere(line => !isIgnorableLine(line, ignoredUsernames, messageUrl)) match {
      case -1 => lines.length
      case index => index
    }
    val reversedIndex =
      lines.reverseIterator.indexWhere(line => !isIgnorableLine(line, ignoredUsernames, messageUrl)) match {
        case -1 => lines.length
        case index => index
      }
    val endExclusive = lines.length - reversedIndex

    if (start >= endExclusive) Vector.empty
    else lines.slice(start, endExclusive)
  }

  private def isIgnorableLine(
      value: String,
      ignoredUsernames: Set[String],
      messageUrl: String,
    ): Boolean = {
    val compact = normalizeWhitespace(value)
    val stripped = stripDecorations(compact)
    val lowered = stripped.toLowerCase(Locale.ROOT)
    val normalizedMessageUrl = normalizeLink(messageUrl).getOrElse(messageUrl)

    compact.isEmpty ||
    SeparatorLinePattern.pattern.matcher(compact).matches() ||
    HashtagNoisePattern.pattern.matcher(stripped).matches() ||
    ignoredUsernames.contains(lowered.stripPrefix("@")) ||
    normalizeLink(compact).contains(normalizedMessageUrl) ||
    stripped.forall(ch => !ch.isLetterOrDigit)
  }

  private def ignoredTelegramUsernames(rawJob: RawJob): Set[String] =
    (Set(rawJob.source) ++ sourceHandleFromUrl(rawJob.url).toSet)
      .map(_.trim.stripPrefix("@").toLowerCase(Locale.ROOT))
      .filter(_.nonEmpty)

  private def sourceHandleFromUrl(url: String): Option[String] =
    SourceUrlPattern.findFirstMatchIn(url).map(_.group(1))

  private def stripDecorations(value: String): String =
    TrailingDecorationPattern
      .replaceAllIn(LeadingDecorationPattern.replaceAllIn(normalizeApostrophes(value), ""), "")
      .trim

  private def phoneDigits(value: String): String =
    value.filter(_.isDigit)

  private def normalizeWhitespace(value: String): String =
    normalizeApostrophes(value)
      .replaceAll("""[ \t\r\f]+""", " ")
      .trim

  private def normalizeMultiline(value: String): String =
    value
      .linesIterator
      .map(normalizeWhitespace)
      .filter(_.nonEmpty)
      .mkString("\n")

  private def normalizeApostrophes(value: String): String =
    value
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .replace('’', '\'')
      .replace('ʻ', '\'')
      .replace('ʼ', '\'')
      .replace('`', '\'')

  private def distinctPreservingOrder(values: List[String]): List[String] = {
    val (_, deduplicated) =
      values.foldLeft((Set.empty[String], List.empty[String])) {
        case ((seen, acc), value) if seen.contains(value) => (seen, acc)
        case ((seen, acc), value) => (seen + value, acc :+ value)
      }

    deduplicated
  }
}
