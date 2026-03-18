package uz.scala.etl.sources

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

import uz.scala.domain.events.RawJob
import uz.scala.domain.jobs.JobDetails
import uz.scala.etl.SourceJobEtl

object IshchiBorKerakToshkentSourceJobEtl extends SourceJobEtl {
  override val sources: Set[String] =
    Set("ishchi_bor_kerak_toshkent")

  private final case class ClassifiedAboutSection(
      requirements: List[String],
      responsibilities: List[String],
      benefits: List[String],
      additional: List[String],
      phoneNumbers: List[String],
      telegramUsernames: List[String],
      links: List[String],
    )

  private object ClassifiedAboutSection {
    val empty: ClassifiedAboutSection =
      ClassifiedAboutSection(
        requirements = Nil,
        responsibilities = Nil,
        benefits = Nil,
        additional = Nil,
        phoneNumbers = Nil,
        telegramUsernames = Nil,
        links = Nil,
      )
  }

  private final case class PreparedDescription(
      baseDescription: String,
      about: ClassifiedAboutSection,
    )

  private val AboutMarkers =
    List(
      "ish haqida",
      "ish haqida ma'lumot",
      "ish haqida malumot",
      "о работе",
    )

  private val SectionBoundaryMarkers =
    List(
      "talablar",
      "требования",
      "biz taklif qilamiz",
      "мы предлагаем",
      "qulayliklar",
      "afzalliklar",
      "ish sharoitlari",
      "sharoitlar",
      "ish vaqti",
      "ish jadvali",
      "ish tartibi",
      "ish grafigi",
      "ish kuni",
      "рабочее время",
      "график работы",
      "manzil",
      "адрес",
      "mo'ljal",
      "orientir",
      "ориентир",
      "hudud",
      "hududlar",
      "region",
      "регион",
      "bog'lanish",
      "boglanish",
      "aloqa",
      "aloqa uchun",
      "murojaat",
      "murojaat uchun",
      "telegram",
      "kontak",
      "контакты",
      "kompaniya",
      "корхона",
      "komпания",
      "korxona",
      "maosh",
      "oylik",
      "ish haqi",
      "зарплата",
      "заработная плата",
    )

  private val BenefitHeaderMarkers =
    List(
      "takliflarimiz",
      "taklif qilinadi",
      "ish sharoitlari",
      "nega aynan biz",
    )

  private val RequirementHeaderMarkers =
    List(
      "biz kimlarni kutamiz",
      "biz kimni qidirmoqdamiz",
      "biz kimni izlayapmiz",
    )

  private val ContactHeaderMarkers =
    List(
      "telegram",
      "rezume yuborish",
      "rezyume yuborish",
      "resume yuborish",
      "cv yuborish",
      "cv yuboring",
      "cv yuboring iltimos",
    )

  private val LocationHeaderMarkers =
    List(
      "hudud",
      "hududlar",
      "region",
      "регион",
    )

  private val ScheduleHeaderMarkers =
    List(
      "ish jadvali",
    )

  private val PhoneHeaderMarkers =
    List(
      "telefon",
      "tel",
      "nomer",
    )

  private val RequirementLineMarkers =
    List(
      "yosh",
      "yoshi",
      "jinsi",
      "tajriba",
      "ish tajribasi",
      "malumoti",
      "ma'lumoti",
      "ma’lumoti",
      "til bilish",
    )

  private val GenderMarkers =
    List(
      "erkak",
      "erkaklar",
      "ayol",
      "ayollar",
      "qiz",
      "qizlar",
      "yigit",
      "yigitlar",
      "faqat ayol-qizlar",
      "faqat ayollar",
      "faqat erkaklar",
    )

  private val BenefitKeywords =
    List(
      "bonus",
      "kpi",
      "premiya",
      "ovqat",
      "tushlik",
      "abed",
      "obed",
      "yotoq joy",
      "yotoqxona",
      "turar joy",
      "yashash joyi",
      "rasmiy ish",
      "ahill jamoa",
      "ahil jamoa",
      "jamoa",
      "ofis",
      "karyera",
      "osish imkoniyati",
      "o'sish imkoniyati",
      "imkoniyat",
      "qulay sharoit",
      "sharoit",
      "o'rgatiladi",
      "urgatiladi",
      "o'rgatamiz",
      "urgatamiz",
      "fiksa",
      "oylik",
      "maosh",
      "barqaror ish",
      "doimiy ish",
      "suhbat asosida",
      "sinov muddati",
      "lavozim ko'tarilishi",
      "lavozim ko‘tarilishi",
      "transport bilan",
    )

  private val ApplicationKeywords =
    List(
      "anketa",
      "rezyume",
      "rezume",
      "resume",
      "cv",
      "havola",
      "telegram orqali",
      "telegramdan yozing",
      "bog'laning",
      "boglaning",
      "murojaat qiling",
    )

  private val RoleHeadingKeywords =
    List(
      "hamshira",
      "laborant",
      "lobarant",
      "labarant",
      "reception",
      "reseption",
      "receptionist",
      "reception qizlar",
      "tozalik xodimi",
      "tozalovchi",
      "gornichnaya",
      "administrator",
      "admin",
      "kassir",
      "sotuvchi",
      "boshqaruvchi",
      "operator",
    )

  private val PromoNoiseKeywords =
    List(
      "asosiy kanal",
      "1mln",
      "1 mln",
    )

  private val VisibleUrlPattern: Regex =
    """(?i)\b(?:https?://|t\.me/)[^\s<>()]+""".r

  private val TelegramUsernamePattern: Regex =
    """(?<![\w@])@([A-Za-z0-9_]{4,})""".r

  private val UzbekPhonePattern: Regex =
    """(?iu)(?:\+?998(?:[\s\-()]?\d){9}|\+?\d[\d \t()\-]{6,}\d)""".r

  private val LeadingDecorationPattern: Regex =
    """^[\s\p{Punct}\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾]+""".r

  private val TrailingDecorationPattern: Regex =
    """[\s\p{Punct}\p{So}•▪◦●✔✅❗👤👉➤▶✓🔥💼🏢📍💰📞📋⏰📨📌📱✨🧾]+$""".r

  override def enrich(rawJob: RawJob): JobDetails = {
    val prepared = prepareDescription(rawJob)
    val base = XorazmIshSourceJobEtl.enrich(rawJob.copy(description = prepared.baseDescription))
    val mergedRequirements =
      normalizeMultiRoleRequirements(
        mergeMultilineValues(base.requirements, compact(prepared.about.requirements))
      )

    JobDetails(
      requirements = mergedRequirements,
      responsibilities =
        sanitizeStructuredSection(
          mergeMultilineValues(base.responsibilities, compact(prepared.about.responsibilities))
        ),
      benefits =
        sanitizeStructuredSection(
          mergeMultilineValues(base.benefits, compact(prepared.about.benefits))
        ),
      additional =
        sanitizeStructuredSection(
          mergeMultilineValues(base.additional, compact(prepared.about.additional))
        ),
      workSchedule = base.workSchedule,
      contactText = sanitizeContactText(base.contactText),
      contactPhoneNumbers =
        distinctPreservingOrder(base.contactPhoneNumbers ++ prepared.about.phoneNumbers),
      contactTelegramUsernames =
        distinctPreservingOrder(base.contactTelegramUsernames ++ prepared.about.telegramUsernames),
      contactLinks =
        distinctPreservingOrder(base.contactLinks ++ prepared.about.links),
    )
  }

  private def prepareDescription(rawJob: RawJob): PreparedDescription = {
    val lines = normalizeLines(rawJob.description)
    val baseLines = ListBuffer.empty[String]
    val aboutLines = ListBuffer.empty[String]

    var index = 0
    while (index < lines.length) {
      val line = lines(index)

      if (isPromoNoiseLine(line)) {
        index += 1
      } else if (startsWithAny(line, AboutMarkers)) {
        val extracted = extractSectionLines(lines, index, AboutMarkers)
        aboutLines ++= extracted._1
        index = extracted._2
      } else {
        val rewritten = rewriteLine(line)
        if (rewritten.nonEmpty && !isPromoNoiseLine(rewritten))
          baseLines += rewritten
        index += 1
      }
    }

    PreparedDescription(
      baseDescription = baseLines.mkString("\n"),
      about = classifyAboutSection(aboutLines.toList, rawJob),
    )
  }

  private def extractSectionLines(
      lines: Vector[String],
      startIndex: Int,
      markers: List[String],
    ): (List[String], Int) = {
    val collected = ListBuffer.empty[String]
    val head = stripLabelFromLine(lines(startIndex), markers)
    if (head.nonEmpty)
      collected += head

    var index = startIndex + 1
    while (index < lines.length && !isSectionBoundary(lines(index))) {
      val line = lines(index)
      if (!isPromoNoiseLine(line))
        collected += line
      index += 1
    }

    collected.toList -> index
  }

  private def classifyAboutSection(lines: List[String], rawJob: RawJob): ClassifiedAboutSection = {
    val cleanedLines =
      lines
        .map(cleanContentLine)
        .filter(_.nonEmpty)
        .filterNot(isPromoNoiseLine)

    val requirements = ListBuffer.empty[String]
    val responsibilities = ListBuffer.empty[String]
    val benefits = ListBuffer.empty[String]
    val additional = ListBuffer.empty[String]

    cleanedLines.foreach { line =>
      if (isDuplicateHeaderLine(line, rawJob)) ()
      else if (isApplicationLike(line)) additional += line
      else if (isRequirementLike(line)) requirements += line
      else if (isBenefitLike(line)) benefits += line
      else responsibilities += line
    }

    val text = cleanedLines.mkString("\n")

    ClassifiedAboutSection(
      requirements = requirements.toList.distinct,
      responsibilities = responsibilities.toList.distinct,
      benefits = benefits.toList.distinct,
      additional = additional.toList.distinct,
      phoneNumbers = extractPhoneNumbers(text),
      telegramUsernames = extractTelegramUsernames(text),
      links = extractLinks(text),
    )
  }

  private def rewriteLine(line: String): String =
    if (startsWithAny(line, LocationHeaderMarkers))
      replaceLeadingLabel(line, LocationHeaderMarkers, "Manzil")
    else if (startsWithAny(line, ScheduleHeaderMarkers))
      replaceLeadingLabel(line, ScheduleHeaderMarkers, "Ish vaqti")
    else if (startsWithAny(line, BenefitHeaderMarkers))
      replaceLeadingLabel(line, BenefitHeaderMarkers, "Biz taklif qilamiz")
    else if (startsWithAny(line, RequirementHeaderMarkers))
      replaceLeadingLabel(line, RequirementHeaderMarkers, "Talablar")
    else if (startsWithAny(line, ContactHeaderMarkers))
      replaceLeadingLabel(line, ContactHeaderMarkers, "Bog'lanish")
    else if (startsWithAny(line, PhoneHeaderMarkers))
      replaceLeadingLabel(line, PhoneHeaderMarkers, "Telefon")
    else if (startsWithAny(line, List("telegram")))
      replaceLeadingLabel(line, List("telegram"), "Bog'lanish")
    else line

  private def isSectionBoundary(line: String): Boolean =
    startsWithAny(line, SectionBoundaryMarkers) || startsWithPhoneHeader(line)

  private def isRequirementLike(line: String): Boolean = {
    val key = normalized(line)

    startsWithAny(line, RequirementLineMarkers) ||
    GenderMarkers.exists(marker => key == marker || key.startsWith(s"$marker ")) ||
    key.contains("talabalar") ||
    key.contains("student") ||
    key.matches("""^\d{1,2}\s*[-–]\s*\d{1,2}.*yosh.*$""")
  }

  private def isBenefitLike(line: String): Boolean = {
    val key = normalized(line)

    BenefitKeywords.exists(key.contains) ||
    key.contains("korxona hisobidan") ||
    key.contains("kompaniya hisobidan") ||
    key.contains("ishga qarab ko'tar") ||
    key.contains("ishga qarab ko‘tar") ||
    key.contains("bo'sh ish o'rni") ||
    key.contains("bo‘sh ish o‘rni") ||
    key.contains("haftalik maosh") ||
    key.contains("kunlik naqd")
  }

  private def isApplicationLike(line: String): Boolean = {
    val key = normalized(line)
    ApplicationKeywords.exists(key.contains) ||
    VisibleUrlPattern.findFirstIn(line).nonEmpty ||
    TelegramUsernamePattern.findFirstIn(line).nonEmpty
  }

  private def isDuplicateHeaderLine(line: String, rawJob: RawJob): Boolean = {
    val key = normalized(line)
    val title = normalized(rawJob.title)
    val company = rawJob.company.map(normalized).getOrElse("")

    key.nonEmpty && (
      key == title ||
        key == company ||
        (title.nonEmpty && key.contains(title) && company.nonEmpty && key.contains(company.stripSuffix("si")))
    )
  }

  private def sanitizeContactText(value: Option[String]): Option[String] =
    value.flatMap { current =>
      compact(
        current
          .split('\n')
          .toList
          .map(cleanContentLine)
          .filter(_.nonEmpty)
          .filterNot(line => {
            val key = normalized(line)
            key == "telegram" || key == "telefon" || key == "tel"
          })
      )
    }

  private def sanitizeStructuredSection(value: Option[String]): Option[String] =
    value.flatMap(current => compact(splitMultiline(current).filterNot(isLeakedSectionLabel)))

  private def normalizeMultiRoleRequirements(value: Option[String]): Option[String] =
    value.flatMap { current =>
      val lines = splitMultiline(current)
      val roleHeadingCount = lines.count(isStandaloneRoleHeading)
      val hasRolePrefixedRequirements = lines.exists(line => stripRolePrefix(line).exists(_.nonEmpty))

      if (roleHeadingCount < 2 && !hasRolePrefixedRequirements)
        compact(lines)
      else
        compact(
          lines.flatMap { line =>
            stripRolePrefix(line) match {
              case Some(stripped) if stripped.nonEmpty =>
                Some(stripped)
              case _ if isStandaloneRoleHeading(line) =>
                None
              case _ =>
                Some(line)
            }
          }
        )
    }

  private def mergeMultilineValues(primary: Option[String], secondary: Option[String]): Option[String] =
    compact(
      distinctPreservingOrder(
        primary.toList.flatMap(splitMultiline) ++ secondary.toList.flatMap(splitMultiline)
      )
    )

  private def splitMultiline(value: String): List[String] =
    value
      .split('\n')
      .toList
      .map(cleanContentLine)
      .filter(_.nonEmpty)

  private def compact(lines: List[String]): Option[String] =
    lines match {
      case Nil => None
      case values => Some(values.distinct.mkString("\n"))
    }

  private def distinctPreservingOrder(values: List[String]): List[String] =
    values.foldLeft(List.empty[String]) { (acc, value) =>
      if (value.nonEmpty && !acc.contains(value)) acc :+ value else acc
    }

  private def cleanContentLine(line: String): String =
    normalizeWhitespace(
      stripBulletPrefix(
        TrailingDecorationPattern.replaceFirstIn(
          LeadingDecorationPattern.replaceFirstIn(line.replace("\uFE0F", "").replace("\u200D", ""), ""),
          "",
        )
      )
    )

  private def stripLabelFromLine(line: String, markers: List[String]): String =
    markers.sortBy(-_.length).foldLeft(line) { case (current, marker) =>
      val trimmed = LeadingDecorationPattern.replaceFirstIn(current, "")
      if (startsWithAny(trimmed, List(marker)))
        trimmed.replaceFirst(
          s"(?iu)^\\Q$marker\\E(?:\\s*[:\\-–—]\\s*|\\s+)?",
          "",
        )
      else trimmed
    }.pipe(cleanContentLine)

  private def replaceLeadingLabel(line: String, markers: List[String], replacement: String): String = {
    val stripped = stripLabelFromLine(line, markers)
    if (stripped.nonEmpty) s"$replacement: $stripped" else s"$replacement:"
  }

  private def normalizeLines(text: String): Vector[String] =
    text
      .replace("\r\n", "\n")
      .replace('\r', '\n')
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .split('\n')
      .toVector
      .map(normalizeWhitespace)
      .filter(_.nonEmpty)

  private def normalized(line: String): String =
    normalizeWhitespace(
      line
        .replace('’', '\'')
        .replace('ʻ', '\'')
        .replace('ʼ', '\'')
    ).toLowerCase

  private def normalizeWhitespace(line: String): String =
    line.replaceAll("""\s+""", " ").trim

  private def startsWithAny(line: String, markers: List[String]): Boolean = {
    val key = normalized(cleanContentLine(line))

    markers.sortBy(-_.length).exists { marker =>
      key.startsWith(marker) && {
        val rest = key.drop(marker.length)
        rest.isEmpty || rest.startsWith(" ") || rest.startsWith(":") || rest.startsWith("-") || rest.startsWith("–")
      }
    }
  }

  private def startsWithPhoneHeader(line: String): Boolean = {
    val key = normalized(cleanContentLine(line))

    PhoneHeaderMarkers.exists { marker =>
      key == marker ||
      key.startsWith(s"$marker:") ||
      key.startsWith(s"$marker -") ||
      key.startsWith(s"$marker –")
    }
  }

  private def isStandaloneRoleHeading(line: String): Boolean = {
    val key = normalized(line)

    RoleHeadingKeywords.exists(marker => key == marker || key == s"$marker uchun")
  }

  private def stripRolePrefix(line: String): Option[String] =
    RoleHeadingKeywords
      .sortBy(-_.length)
      .collectFirst {
        case marker if normalized(line).matches(s"(?iu)^\\Q$marker\\E(?:\\s*\\([^)]*\\))?\\s*[:\\-–—]\\s*.+$$") =>
          cleanContentLine(
            line.replaceFirst(
              s"(?iu)^\\Q$marker\\E(?:\\s*\\([^)]*\\))?\\s*[:\\-–—]\\s*",
              "",
            )
          )
      }

  private def isPromoNoiseLine(line: String): Boolean = {
    val key = normalized(line)
    PromoNoiseKeywords.exists(key.contains)
  }

  private def isLeakedSectionLabel(line: String): Boolean = {
    val key = normalized(line)
    key == "bog'lanish" ||
    key == "boglanish" ||
    key == "talablar" ||
    key == "biz taklif qilamiz" ||
    key == "ish vaqti" ||
    key == "manzil"
  }

  private def stripBulletPrefix(line: String): String =
    line.replaceFirst("""(?u)^\s*[•▪◦●✔✅❗👤👉➤▶✓\-–—]+\s*""", "")

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

  private def extractLinks(text: String): List[String] =
    VisibleUrlPattern
      .findAllMatchIn(text)
      .map(_.matched)
      .map(normalizeLink)
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

  implicit private final class PipeOps[A](private val value: A) extends AnyVal {
    def pipe[B](f: A => B): B = f(value)
  }
}
