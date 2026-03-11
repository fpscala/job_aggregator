package uz.scala.etl.sources

import scala.util.matching.Regex

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails
import uz.scala.etl.SourceJobEtl

object XorazmIshSourceJobEtl extends SourceJobEtl {
  override val sources: Set[String] =
    Set("xorazm_ish", "xorazm_ish_bor_elonlar")

  private final case class ExtractedSection(
      lines: List[String],
      consumedIndices: Set[Int],
    )

  private val RequirementsMarkers =
    List(
      "talablar",
      "umumiy talablar",
      "требования",
      "общие требования",
      "талаблар",
      "умумий талаблар",
    )

  private val ResponsibilitiesMarkers =
    List(
      "vazifasi",
      "vazifalar",
      "asosiy vazifalar",
      "обязанности",
      "вазифаси",
      "вазифалар",
      "асосий вазифалар",
    )

  private val BenefitsMarkers =
    List(
      "qulayliklar",
      "biz taklif qilamiz",
      "мы предлагаем",
      "қулайликлар",
      "биз таклиф қиламиз",
      "биз нималарни таклиф қиламиз",
    )

  private val WorkScheduleMarkers =
    List(
      "ish vaqti",
      "ish tartibi",
      "ish grafigi",
      "рабочее время",
      "график работы",
      "иш вақти",
      "иш тартиби",
      "иш графиги",
    )

  private val ContactSectionMarkers =
    List(
      "murojaat uchun",
      "murojaat",
      "aloqa uchun",
      "aloqa",
      "bog'lanish",
      "boglanish",
      "ariza topshirish",
      "kontak",
      "контакты",
      "мурожаат учун",
      "мурожаат",
      "алоқа учун",
      "алоқа",
      "боғланиш",
      "ариза топшириш",
    )

  private val AdditionalSectionMarkers =
    List(
      "eslatma",
      "qo'shimcha",
      "qo'shimcha ma'lumot",
      "qo'shimcha ma'lumotlar",
      "qo‘shimcha",
      "qo‘shimcha ma’lumot",
      "qo‘shimcha ma’lumotlar",
      "izoh",
      "transport yo'nalishlari",
      "transport yonalishlari",
      "transport yunalishlari",
      "эслатма",
      "қўшимча",
      "қўшимча маълумот",
      "қўшимча маълумотлар",
      "транспорт йўналишлари",
    )

  private val ContactInstructionMarkers =
    List(
      "telegram orqali",
      "telegramdan",
      "to'ldirish uchun anketa",
      "anketa orqali",
      "anketa",
      "rezyumengizni",
      "rezumengizni",
      "rezyume",
      "rezume",
      "resume",
      "quyidagi havola orqali",
      "quyidagi profilga yuboring",
      "havola orqali anketani",
      "телеграм орқали",
      "тўлдириш учун анкета",
      "анкета орқали",
      "резюменгизни",
      "резумеларингизни",
      "резюме",
      "резуме",
      "қуйидаги ҳавола орқали",
      "қуйидаги профилга юборинг",
      "ҳавола орқали анкетани",
    )

  private val MetadataMarkers =
    List(
      "oylik",
      "maosh",
      "ish haqi",
      "kunlik",
      "manzil",
      "mo'ljal",
      "ish joyi",
      "korxona",
      "kompaniya",
      "зарплата",
      "заработная плата",
      "адрес",
      "ориентир",
      "ойлик",
      "маош",
      "иш ҳақи",
      "кунлик",
      "манзил",
      "мўлжал",
      "иш жойи",
      "корхона",
      "компания",
    )

  private val SalaryMetadataMarkers =
    List(
      "oylik",
      "maosh",
      "ish haqi",
      "kunlik",
      "зарплата",
      "заработная плата",
      "ойлик",
      "маош",
      "иш ҳақи",
      "кунлик",
    )

  private val SectionMarkers =
    RequirementsMarkers ++
      ResponsibilitiesMarkers ++
      BenefitsMarkers ++
      AdditionalSectionMarkers ++
      WorkScheduleMarkers ++
      ContactSectionMarkers ++
      MetadataMarkers

  private val TrailingSourceUrlPattern: Regex =
    """https?://t\.me/([^/\s]+)/\d+""".r

  private val TelegramUsernamePattern: Regex =
    """(?<![\w@])@([A-Za-z0-9_]{4,})""".r

  private val UzbekPhonePattern: Regex =
    """\+?998(?:[\s\-()]?\d){9}""".r

  private val VisibleUrlPattern: Regex =
    """(?i)\b(?:https?://|t\.me/)[^\s<>()]+""".r

  private val TimeValuePattern: Regex =
    """\b\d{1,2}[:.]\d{2}\b""".r

  private val ScheduleRatioPattern: Regex =
    """\b\d/\d\b""".r

  private val LeadingDecorationPattern: Regex =
    """^[\s\p{Punct}\p{So}•▪◦●✔✅❗👤👉➤▶✓]+""".r

  private val TrailingDecorationPattern: Regex =
    """[\s\p{Punct}\p{So}•▪◦●✔✅❗👤👉➤▶✓]+$""".r

  private val BenefitBulletPattern: Regex =
    """(?u)^\s*[✓✔✅]+\s*""".r

  override def enrich(rawJob: RawJob): JobDetails = {
    val ignoredUsernames = ignoredTelegramUsernames(rawJob)
    val ignoredUrls = ignoredSourceUrls(rawJob, ignoredUsernames)
    val lines =
      normalizeLines(rawJob.description)
        .filterNot(isSourceHandleLine(_, ignoredUsernames))
        .toVector

    val requirements = extractSection(
      lines = lines,
      start =
        line =>
          startsWithAny(line, RequirementsMarkers) ||
            normalized(line).contains("talablari bor") ||
            normalized(line).contains("талаблари бор"),
      stripLabels = RequirementsMarkers,
      resetFirstLine =
        line =>
          normalized(line).contains("talablari bor") ||
            normalized(line).contains("талаблари бор"),
    )
    val responsibilities =
      extractSection(
        lines = lines,
        start = startsWithAny(_, ResponsibilitiesMarkers),
        stripLabels = ResponsibilitiesMarkers,
      )
    val benefits =
      extractSection(
        lines = lines,
        start = startsWithAny(_, BenefitsMarkers),
        stripLabels = BenefitsMarkers,
      )
    val labeledAdditional =
      extractSection(
        lines = lines,
        start = startsWithAny(_, AdditionalSectionMarkers),
        stripLabels = Nil,
      )
    val workSchedule =
      extractWorkSchedule(lines)

    val extractedPhoneNumbers =
      extractPhoneNumbers(lines.mkString("\n"))

    val extractedTelegramUsernames =
      extractTelegramUsernames(lines.mkString("\n")).filterNot(ignoredUsernames)

    val extractedLinks =
      extractLinks(rawJob, lines.mkString("\n"), ignoredUsernames, ignoredUrls)

    val contact =
      extractContact(
        lines = lines,
        ignoredUsernames = ignoredUsernames,
        ignoredUrls = ignoredUrls,
      )

    val consumedIndices =
      requirements.consumedIndices ++
        responsibilities.consumedIndices ++
        benefits.consumedIndices ++
        labeledAdditional.consumedIndices ++
        workSchedule.consumedIndices ++
        contact.consumedIndices ++
        metadataIndices(lines)

    JobDetails(
      requirements = compact(requirements.lines),
      responsibilities = compact(responsibilities.lines),
      benefits = compact(benefits.lines),
      additional =
        compact(
          (labeledAdditional.lines ++
            extractAdditional(lines, consumedIndices, ignoredUsernames, ignoredUrls)).distinct
        ),
      workSchedule = compact(workSchedule.lines),
      contactText = compact(contact.lines),
      contactPhoneNumbers = extractedPhoneNumbers,
      contactTelegramUsernames = extractedTelegramUsernames,
      contactLinks = extractedLinks,
    )
  }

  private def extractContact(
      lines: Vector[String],
      ignoredUsernames: Set[String],
      ignoredUrls: Set[String],
    ): ExtractedSection = {
    val contactSection =
      extractSection(
        lines = lines,
        start = startsWithAny(_, ContactSectionMarkers),
        stripLabels = ContactSectionMarkers,
      )

    val standaloneInstructionLines =
      lines.zipWithIndex.collect {
        case (line, index) if isStandaloneContactInstruction(line) =>
          index -> line
      }

    val contactSignalIndices =
      lines.zipWithIndex.collect {
        case (line, index)
            if containsPhoneNumber(line) ||
              containsTelegramUsername(line, ignoredUsernames) ||
              containsNonSourceUrl(line, ignoredUsernames, ignoredUrls) =>
          index
      }.toSet

    val cleanedLines =
      (contactSection.lines ++ standaloneInstructionLines.map(_._2))
        .map(cleanContactLine(_, ignoredUsernames, ignoredUrls))
        .filter(_.nonEmpty)
        .distinct

    ExtractedSection(
      lines = cleanedLines,
      consumedIndices =
        contactSection.consumedIndices ++
          standaloneInstructionLines.map(_._1).toSet ++
          contactSignalIndices,
    )
  }

  private def extractSection(
      lines: Vector[String],
      start: String => Boolean,
      stripLabels: List[String],
      resetFirstLine: String => Boolean = _ => false,
    ): ExtractedSection =
    lines.indexWhere(start) match {
      case -1 =>
        ExtractedSection(Nil, Set.empty)
      case index =>
        val head =
          if (resetFirstLine(lines(index))) ""
          else stripLabelsFromLine(lines(index), stripLabels)

        val tail =
          lines
            .drop(index + 1)
            .takeWhile(line => !isSectionBoundary(line))
            .toList

        ExtractedSection(
          lines =
            (head :: tail)
              .map(cleanContentLine)
              .map(stripBenefitBullet)
              .filter(_.nonEmpty)
              .distinct,
          consumedIndices = (index until (index + 1 + tail.size)).toSet,
        )
    }

  private def extractWorkSchedule(lines: Vector[String]): ExtractedSection =
    lines.indexWhere(startsWithAny(_, WorkScheduleMarkers)) match {
      case -1 =>
        ExtractedSection(Nil, Set.empty)
      case index =>
        val head = stripLabelsFromLine(lines(index), WorkScheduleMarkers)
        val tail =
          lines
            .drop(index + 1)
            .takeWhile(line => !isSectionBoundary(line) && looksLikeWorkSchedule(line))
            .toList

        val cleanedLines =
          (head :: tail)
            .map(normalizeWorkScheduleLine)
            .filter(line => line.nonEmpty && looksLikeWorkSchedule(line))
            .distinct

        ExtractedSection(
          lines = cleanedLines,
          consumedIndices = (index until (index + 1 + tail.size)).toSet,
        )
    }

  private def extractAdditional(
      lines: Vector[String],
      consumedIndices: Set[Int],
      ignoredUsernames: Set[String],
      ignoredUrls: Set[String],
    ): List[String] = {
    val firstStructuredIndex =
      lines.indexWhere(line => isSectionBoundary(line) || isStandaloneContactInstruction(line)) match {
        case -1 => 2
        case index => index
      }

    lines.zipWithIndex.collect {
      case (line, index)
          if index >= firstStructuredIndex &&
            !consumedIndices.contains(index) &&
            !isSectionBoundary(line) &&
            !isHashtagLine(line) &&
            !isMetadataLine(line) &&
            !isStandaloneContactInstruction(line) &&
            !isSourceHandleLine(line, ignoredUsernames) =>
        cleanAdditionalLine(line, ignoredUsernames, ignoredUrls)
    }.filter(_.nonEmpty).distinct.toList
  }

  private def metadataIndices(lines: Vector[String]): Set[Int] =
    lines.zipWithIndex.foldLeft((Set.empty[Int], false)) {
      case ((indices, _), (line, index)) if isMetadataLine(line) =>
        (indices + index, isSalaryMetadataLine(line))
      case ((indices, true), (line, index)) if looksLikeSalaryContinuation(line) =>
        (indices + index, true)
      case ((indices, _), (line, index)) if looksLikeMetadataContinuation(line) && index > 0 && isMetadataLine(lines(index - 1)) =>
        (indices + index, false)
      case ((indices, _), _) =>
        (indices, false)
    }._1

  private def extractLinks(
      rawJob: RawJob,
      text: String,
      ignoredUsernames: Set[String],
      ignoredUrls: Set[String],
    ): List[String] = {
    val visibleLinks =
      VisibleUrlPattern
        .findAllMatchIn(text)
        .map(_.matched)
        .toList

    val hiddenLinks = rawJob.contactLinks.getOrElse(List.empty)

    (hiddenLinks ++ visibleLinks)
      .map(normalizeLink)
      .filter(_.nonEmpty)
      .filterNot(ignoredUrls)
      .filterNot(isSourceTelegramUrl(_, ignoredUsernames))
      .distinct
  }

  private def isSectionBoundary(line: String): Boolean =
    startsWithAny(line, SectionMarkers)

  private def isMetadataLine(line: String): Boolean =
    startsWithAny(line, MetadataMarkers)

  private def isSalaryMetadataLine(line: String): Boolean =
    startsWithAny(line, SalaryMetadataMarkers)

  private def isStandaloneContactInstruction(line: String): Boolean = {
    val key = normalized(line)
    ContactInstructionMarkers.exists(key.contains) ||
    key.contains("qo'shimcha ma'lumotlar uchun") ||
    key.contains("qo’shimcha ma’lumotlar uchun") ||
    key.contains("қўшимча маълумотлар учун") ||
    key.contains("havolani bosib") ||
    key.contains("ҳаволани босиб") ||
    key.contains("ustiga bosing") ||
    key.contains("устига босинг") ||
    key.contains("murojaat qiling") ||
    key.contains("мурожаат қилинг") ||
    key.contains("rezyumengizni") ||
    key.contains("rezumelaringizni") ||
    key.contains("резюменгизни") ||
    key.contains("резумеларингизни")
  }

  private def containsPhoneNumber(line: String): Boolean =
    UzbekPhonePattern.findFirstIn(line).nonEmpty

  private def containsTelegramUsername(line: String, ignoredUsernames: Set[String]): Boolean =
    extractTelegramUsernames(line).exists(username => !ignoredUsernames.contains(username))

  private def containsNonSourceUrl(
      line: String,
      ignoredUsernames: Set[String],
      ignoredUrls: Set[String],
    ): Boolean =
    VisibleUrlPattern
      .findAllMatchIn(line)
      .map(_.matched)
      .map(normalizeLink)
      .exists(url => url.nonEmpty && !ignoredUrls.contains(url) && !isSourceTelegramUrl(url, ignoredUsernames))

  private def cleanContactLine(
      line: String,
      ignoredUsernames: Set[String],
      ignoredUrls: Set[String],
    ): String = {
    val withoutIgnoredHandles =
      ignoredUsernames.foldLeft(line) { case (current, username) =>
        current.replaceAll(s"(?iu)@${Regex.quote(username)}\\b", " ")
      }

    val withoutIgnoredUrls =
      ignoredUrls.foldLeft(withoutIgnoredHandles) { case (current, url) =>
        current.replace(url, " ")
      }

    val withoutPlainIgnoredNames =
      ignoredUsernames.foldLeft(withoutIgnoredUrls) { case (current, username) =>
        current.replaceAll(s"(?iu)\\b${Regex.quote(username)}\\b", " ")
      }

    val withoutPhones =
      UzbekPhonePattern.replaceAllIn(withoutPlainIgnoredNames, " ")

    val withoutUsernames =
      TelegramUsernamePattern.replaceAllIn(withoutPhones, " ")

    val withoutUrls =
      VisibleUrlPattern.replaceAllIn(withoutUsernames, " ")

    val withoutLabels =
      stripLabelsFromLine(withoutUrls, ContactSectionMarkers)

    normalizeWhitespace(
      stripDecorations(withoutLabels)
        .replaceAll("""(?iu)\bga\s+(?=\d+\s*ta\s*xabar)""", "")
    )
  }

  private def cleanAdditionalLine(
      line: String,
      ignoredUsernames: Set[String],
      ignoredUrls: Set[String],
    ): String = {
    val withoutIgnoredHandles =
      ignoredUsernames.foldLeft(line) { case (current, username) =>
        current.replaceAll(s"(?iu)@${Regex.quote(username)}\\b", " ")
      }

    val withoutIgnoredUrls =
      ignoredUrls.foldLeft(withoutIgnoredHandles) { case (current, url) =>
        current.replace(url, " ")
      }

    val cleaned =
      stripBenefitBullet(
        stripDecorations(
          stripBullet(
            VisibleUrlPattern.replaceAllIn(withoutIgnoredUrls, " ")
          )
        )
      )

    val normalized = normalizeWhitespace(cleaned)
    val key = normalized.toLowerCase
    if (
      key.contains("biz bilan bog'lan") ||
      key.contains("murojaat qiling") ||
      key.contains("биз билан боғлан") ||
      key.contains("мурожаат қилинг")
    ) ""
    else normalized
  }

  private def cleanContentLine(line: String): String =
    normalizeWhitespace(stripDecorations(stripBullet(line)))

  private def stripLabelsFromLine(line: String, labels: List[String]): String =
    labels.foldLeft(line) { case (current, label) =>
      LeadingDecorationPattern
        .replaceFirstIn(current, "")
        .replaceFirst(
          s"(?iu)^\\Q$label\\E\\s*[:\\-–]?\\s*",
          "",
        )
    }

  private def stripBenefitBullet(line: String): String =
    BenefitBulletPattern.replaceFirstIn(line, "")

  private def normalizeWorkScheduleLine(line: String): String =
    normalizeWhitespace(stripBullet(LeadingDecorationPattern.replaceFirstIn(line, "")))
      .replaceAll("""(?i)(\b\d{1,2}[:.]\d{2})\s*g\b""", "$1")
      .replaceAll("""(?<=\d)\(""", " (")
      .replaceAll("""\(\s+""", "(")
      .replaceAll("""\s+\)""", ")")

  private def looksLikeMetadataContinuation(line: String): Boolean = {
    val trimmed = line.trim
    trimmed.startsWith("(") || trimmed.startsWith("（")
  }

  private def looksLikeSalaryContinuation(line: String): Boolean = {
    val key = normalized(line)
    if (key.isEmpty || isSectionBoundary(line) || containsPhoneNumber(line)) false
    else {
      key.exists(_.isDigit) ||
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
      key.contains("opit") ||
      key.contains("qarab") ||
      key.contains("belgilanadi") ||
      key.contains("сўм") ||
      key.contains("сум") ||
      key.contains("қараб") ||
      key.contains("белгиланади")
    }
  }

  private def looksLikeWorkSchedule(line: String): Boolean = {
    val key = normalized(line)

    TimeValuePattern.findFirstIn(line).nonEmpty ||
    ScheduleRatioPattern.findFirstIn(key).nonEmpty ||
    key.contains("ҳафтада") ||
    key.contains("haftada") ||
    key.contains("якшанба") ||
    key.contains("smena") ||
    key.contains("смена") ||
    key.contains("yakshanba") ||
    key.contains("dam olish") ||
    key.contains("дам олиш") ||
    key.contains("qulay grafik") ||
    key.contains("қулай график") ||
    key.contains("faslga qarab") ||
    key.contains("фаслга қараб") ||
    key.contains("to'liq stavka") ||
    key.contains("to‘liq stavka") ||
    key.contains("тўлиқ ставка") ||
    key.contains("grafik") ||
    key.contains("график")
  }

  private def isBulletLikeLine(line: String): Boolean = {
    val trimmed = line.trim
    trimmed.startsWith("•") ||
    trimmed.startsWith("▪") ||
    trimmed.startsWith("◦") ||
    trimmed.startsWith("●") ||
    trimmed.startsWith("✓") ||
    trimmed.startsWith("✔") ||
    trimmed.startsWith("✅") ||
    trimmed.startsWith("-")
  }

  private def startsWithAny(line: String, markers: List[String]): Boolean = {
    val key = normalized(line)
    markers.exists(marker => key.startsWith(marker))
  }

  private def normalizeLines(text: String): List[String] =
    text
      .replace("\r\n", "\n")
      .replace('\r', '\n')
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .split('\n')
      .toList
      .map(normalizeWhitespace)
      .filter(_.nonEmpty)

  private def normalized(line: String): String =
    normalizeWhitespace(
      stripDecorations(
        line
          .replace('’', '\'')
          .replace('ʻ', '\'')
          .replace('ʼ', '\'')
      )
    ).toLowerCase

  private def stripDecorations(line: String): String =
    TrailingDecorationPattern.replaceFirstIn(LeadingDecorationPattern.replaceFirstIn(line, ""), "")

  private def stripBullet(line: String): String =
    line.replaceFirst("""(?u)^\s*[•▪◦●✔✅❗👤👉➤▶✓\-]+\s*""", "")

  private def normalizeWhitespace(line: String): String =
    line.replaceAll("""\s+""", " ").trim

  private def compact(lines: List[String]): Option[String] =
    lines match {
      case Nil => None
      case values =>
        Some(values.distinct.mkString("\n"))
    }

  private def extractTelegramUsernames(text: String): List[String] =
    TelegramUsernamePattern
      .findAllMatchIn(text)
      .map(_.group(1).toLowerCase)
      .toList
      .distinct

  private def extractPhoneNumbers(text: String): List[String] =
    UzbekPhonePattern
      .findAllMatchIn(text)
      .map(_.matched)
      .map(normalizePhoneNumber)
      .filter(_.nonEmpty)
      .toList
      .distinct

  private def normalizePhoneNumber(value: String): String = {
    val digits = value.filter(_.isDigit)

    if (digits.startsWith("998") && digits.length == 12) s"+$digits"
    else if (digits.nonEmpty) digits
    else ""
  }

  private def normalizeLink(value: String): String = {
    val trimmed =
      value
        .trim
        .stripPrefix("<")
        .stripSuffix(">")
        .stripSuffix(".")
        .stripSuffix(",")
        .stripSuffix(";")
        .stripSuffix(":")
        .stripSuffix(")")

    if (trimmed.toLowerCase.startsWith("t.me/")) s"https://$trimmed"
    else trimmed
  }

  private def ignoredTelegramUsernames(rawJob: RawJob): Set[String] = {
    val fromSource =
      Set(rawJob.source.trim.toLowerCase).filter(_.nonEmpty)

    val fromUrl =
      rawJob.url match {
        case TrailingSourceUrlPattern(username) => Set(username.trim.toLowerCase)
        case _                                  => Set.empty[String]
      }

    fromSource ++ fromUrl
  }

  private def ignoredSourceUrls(rawJob: RawJob, ignoredUsernames: Set[String]): Set[String] =
    (Set(normalizeLink(rawJob.url)) ++ ignoredUsernames.map(username => s"https://t.me/$username")).filter(_.nonEmpty)

  private def isSourceTelegramUrl(url: String, ignoredUsernames: Set[String]): Boolean =
    url match {
      case TrailingSourceUrlPattern(username) =>
        ignoredUsernames.contains(username.trim.toLowerCase)
      case _ =>
        """(?i)^https?://t\.me/([^/\s?#]+)""".r
          .findFirstMatchIn(url)
          .map(_.group(1).trim.toLowerCase)
          .exists(ignoredUsernames.contains)
    }

  private def isSourceHandleLine(line: String, ignoredUsernames: Set[String]): Boolean = {
    val usernames = extractTelegramUsernames(line).toSet
    val stripped =
      normalizeWhitespace(
        TelegramUsernamePattern.replaceAllIn(stripDecorations(line), " ")
      )

    usernames.nonEmpty &&
    usernames.subsetOf(ignoredUsernames) &&
    stripped.isEmpty
  }

  private def isHashtagLine(line: String): Boolean =
    normalized(line).startsWith("#")
}
