package uz.scala.telegram

import uz.scala.domain.jobs.Job

object TelegramCaptionRenderer {
  private val MaxCaptionLength = 1024
  private val SectionSeparator = "\n\n"
  private val LineSeparator = "\n"
  private val LowValueContactPhrases = Set(
    "murojaat",
    "murojaat uchun",
    "aloqa",
    "telefon",
    "tel",
    "telegram",
    "telegram orqali",
    "onlayn anketa",
    "anketa",
    "bot",
  )
  private val DecorativePrefixChars = Set(
    '•',
    '-',
    '–',
    '—',
    '✓',
    '✔',
    '☑',
    '▪',
    '▫',
    '◆',
    '◇',
    '★',
    '☆',
    '▶',
    '►',
    '\uFE0F',
    '\uFEFF',
    ':',
    ';',
    ',',
    '.',
  )

  def render(job: Job, footerHandle: Option[String] = None): String = {
    val footerBlock = renderFooter(footerHandle)
    val footerBudget =
      footerBlock.map(blockLength).map(_ + SectionSeparator.length).getOrElse(0)

    val body = fitWithinLimit(
      blocks =
        List(
          renderHeadline(job.title),
          renderMeta(job),
          renderSection("📋", "Talablar", job.requirements),
          renderSection("🛠", "Vazifalar", job.responsibilities),
          renderSection("🎁", "Qulayliklar", job.benefits),
          renderContactSection(job),
          renderList("☎️", "Telefon", job.contactPhoneNumbers),
          renderList(
            "💬",
            "Telegram",
            job.contactTelegramUsernames.map(username => s"@$username"),
          ),
          renderLinks(job.contactLinks),
          renderSection("✨", "Qo'shimcha", job.additional),
        ).flatten,
      maxLength = MaxCaptionLength - footerBudget,
    )

    footerBlock match {
      case Some(footer) if body.nonEmpty => s"$body$SectionSeparator${renderBlock(footer)}"
      case Some(footer) => renderBlock(footer)
      case None => body
    }
  }

  private def renderHeadline(title: String): Option[RenderBlock] =
    Option(normalize(title)).filter(_.nonEmpty).map { value =>
      RenderBlock(
        lines = List(s"🔥 <b>${escape(value)}</b>"),
        plainLines = List(s"🔥 $value"),
        allowPartial = false,
      )
    }

  private def renderMeta(job: Job): Option[RenderBlock] =
    List(
      renderInlineField("🏢", "Kompaniya", job.company),
      renderInlineField("💸", "Maosh", job.salary),
      renderInlineField("📍", "Manzil", job.location),
      renderInlineField("🕒", "Ish vaqti", job.workSchedule),
    ).flatten match {
      case Nil => None
      case lines =>
        Some(
          RenderBlock(
            lines = lines.map(_._1),
            plainLines = lines.map(_._2),
            allowPartial = false,
          )
        )
    }

  private def renderInlineField(
      icon: String,
      label: String,
      value: Option[String],
    ): Option[(String, String)] =
    value
      .map(normalizeDecorated)
      .filter(_.nonEmpty)
      .map(content => s"$icon <b>$label:</b> ${escape(content)}" -> s"$icon $label: $content")

  private def renderSection(icon: String, label: String, value: Option[String]): Option[RenderBlock] =
    renderBulletBlock(icon, label, splitMeaningfulLines(value))

  private def renderContactSection(job: Job): Option[RenderBlock] =
    renderBulletBlock(
      "📨",
      "Murojaat",
      sanitizeContactLines(
        value = job.contactText,
        usernames = job.contactTelegramUsernames,
        phoneNumbers = job.contactPhoneNumbers,
        links = job.contactLinks,
      ),
    )

  private def renderList(icon: String, label: String, values: List[String]): Option[RenderBlock] =
    renderBulletBlock(icon, label, values.map(normalizeDecorated).filter(_.nonEmpty).distinct)

  private def renderLinks(values: List[String]): Option[RenderBlock] =
    values
      .map(normalizeDecorated)
      .filter(_.nonEmpty)
      .distinct
      .zipWithIndex match {
      case Nil => None
      case links =>
        Some(
          RenderBlock(
            lines =
              "🔗 <b>Havolalar:</b>" +:
                links.map {
                  case (link, index) =>
                    s"""• <a href="${escapeAttribute(link)}">Murojaat ${index + 1}</a>"""
                },
            plainLines =
              "🔗 Havolalar:" +:
                links.map {
                  case (_, index) => s"• Murojaat ${index + 1}"
                },
            allowPartial = false,
          )
        )
    }

  private def renderFooter(value: Option[String]): Option[RenderBlock] =
    value
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { handle =>
        val normalized = if (handle.startsWith("@")) handle else s"@$handle"
        RenderBlock(
          lines = List(s"🤖 <b>${escape(normalized)}</b>"),
          plainLines = List(s"🤖 $normalized"),
          allowPartial = false,
        )
      }

  private def renderBulletBlock(
      icon: String,
      label: String,
      values: List[String],
    ): Option[RenderBlock] =
    values.map(normalizeDecorated).filter(_.nonEmpty).distinct match {
      case Nil => None
      case list =>
        Some(
          RenderBlock(
            lines =
              s"$icon <b>$label:</b>" +:
                list.map(item => s"• ${escape(item)}"),
            plainLines =
              s"$icon $label:" +:
                list.map(item => s"• $item"),
            allowPartial = true,
          )
        )
    }

  private def fitWithinLimit(blocks: List[RenderBlock], maxLength: Int): String = {
    val (selected, _) =
      blocks.foldLeft((Vector.empty[RenderBlock], 0)) {
        case ((acc, usedLength), block) =>
          val separatorLength = if (acc.isEmpty) 0 else SectionSeparator.length
          val fullLength = usedLength + separatorLength + blockLength(block)

          if (fullLength <= maxLength) (acc :+ block, fullLength)
          else if (block.allowPartial) {
            val remaining = maxLength - usedLength - separatorLength
            val partial = truncateBlock(block, remaining)

            partial.fold((acc, usedLength))(truncated => {
              val next = acc :+ truncated
              val nextLength = usedLength + separatorLength + blockLength(truncated)
              (next, nextLength)
            })
          } else (acc, usedLength)
      }

    selected.map(renderBlock).mkString(SectionSeparator)
  }

  private def truncateBlock(block: RenderBlock, maxLength: Int): Option[RenderBlock] = {
    if (maxLength <= 0) None
    else {
      val header = block.lines.head
      val plainHeader = block.plainLines.head

      if (plainHeader.length > maxLength) None
      else {
        val (_, renderedLines, plainLines, _) =
          block.lines.tail
            .zip(block.plainLines.tail)
            .foldLeft((false, Vector(header), Vector(plainHeader), plainHeader.length)) {
              case ((true, lines, plain, length), _) => (true, lines, plain, length)
              case ((false, lines, plain, length), (line, plainLine)) =>
                val nextLength = length + LineSeparator.length + plainLine.length

                if (nextLength <= maxLength)
                  (false, lines :+ line, plain :+ plainLine, nextLength)
                else (true, lines, plain, length)
            }

        if (renderedLines.size <= 1) None
        else
          Some(
            RenderBlock(
              lines = renderedLines.toList,
              plainLines = plainLines.toList,
              allowPartial = false,
            )
          )
      }
    }
  }

  private def renderBlock(block: RenderBlock): String =
    block.lines.mkString(LineSeparator)

  private def blockLength(block: RenderBlock): Int =
    block.plainLines.mkString(LineSeparator).length

  private def splitMeaningfulLines(value: Option[String]): List[String] =
    value
      .map(normalizeMultiline)
      .toList
      .flatMap(_.linesIterator.toList)
      .map(normalizeDecorated)
      .filter(_.nonEmpty)
      .distinct

  private def sanitizeContactLines(
      value: Option[String],
      usernames: List[String],
      phoneNumbers: List[String],
      links: List[String],
    ): List[String] = {
    val normalizedUsernames =
      usernames.map(_.trim.toLowerCase.stripPrefix("@")).filter(_.nonEmpty).toSet
    val normalizedPhones =
      phoneNumbers.map(phoneDigits).filter(_.nonEmpty).toSet
    val normalizedLinks =
      links.map(_.trim.toLowerCase).filter(_.nonEmpty).toSet

    value
      .map(normalizeMultiline)
      .toList
      .flatMap(_.linesIterator.toList)
      .map(normalizeDecorated)
      .filter(_.nonEmpty)
      .filterNot { line =>
        val compact = line.toLowerCase.replaceAll("""\s+""", " ").trim
        val withoutUsers =
          normalizedUsernames.foldLeft(compact) { case (current, username) =>
            current
              .replace(s"@$username", " ")
              .replace(username, " ")
          }
        val withoutLinks =
          normalizedLinks.foldLeft(withoutUsers) { case (current, link) =>
            current.replace(link, " ")
          }
        val withoutPhones =
          phoneNumbers.foldLeft(withoutLinks) { case (current, phone) =>
            current.replace(phone.toLowerCase, " ")
          }.replaceAll("""[+]?[\d\s()\-]{7,}""", " ")
        val stripped = withoutPhones.replaceAll("""\s+""", " ").trim
        val digits = phoneDigits(line)

        compact.isEmpty ||
        normalizedLinks.contains(compact) ||
        normalizedPhones.contains(digits) ||
        normalizedUsernames.contains(compact.stripPrefix("@")) ||
        LowValueContactPhrases.contains(compact) ||
        stripped.isEmpty ||
        LowValueContactPhrases.contains(stripped)
      }
      .distinct
  }

  private def normalize(value: String): String =
    value
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .replaceAll("""\s+""", " ")
      .trim

  private def normalizeMultiline(value: String): String =
    value
      .replace('\u00a0', ' ')
      .replace('\u200b', ' ')
      .linesIterator
      .map(_.replaceAll("""[ \t\r\f]+""", " ").trim)
      .filter(_.nonEmpty)
      .mkString(LineSeparator)

  private def normalizeDecorated(value: String): String =
    stripDecorativePrefix(normalize(value))

  private def stripDecorativePrefix(value: String): String =
    value.dropWhile(ch => ch.isWhitespace || DecorativePrefixChars.contains(ch)).trim

  private def phoneDigits(value: String): String =
    value.filter(_.isDigit)

  private def escape(value: String): String =
    value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")

  private def escapeAttribute(value: String): String =
    escape(value).replace("\"", "&quot;")

  private final case class RenderBlock(
      lines: List[String],
      plainLines: List[String],
      allowPartial: Boolean,
    )
}
