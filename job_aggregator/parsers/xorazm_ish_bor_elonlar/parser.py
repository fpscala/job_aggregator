from __future__ import annotations

import re
from typing import TYPE_CHECKING

from job_aggregator.core.parser_base import BaseParser
from job_aggregator.models.job import Job

if TYPE_CHECKING:
    from telethon.tl.custom.message import Message

ROLE_BULLET_CHARS = "-•▪▫*●◦▶✓✔✅🔸🔹📲📝🚚📌"
ROLE_LIST_BULLET_CHARS = "-•▪▫*●◦▶🔸🔹📲📝🚚"
ROLE_BULLET_PATTERN = re.compile(r"^[\s\-•▪▫*●◦▶✓✔✅🔸🔹📲📝🚚📌\d().\ufe0f]+")
DECORATIVE_ONLY_PATTERN = re.compile(r"^[\W_📣🔉💥🔥✅❗❕❌➖➕👍👆👇☎📞📱🆔🗓💼🏢📍🎯⏰💰📝📋📌🚗🕰💎🕹📢🔹🔸🔴✨👥🙋‍♀🙋‍♂🙋💁🏻‍♂️🟢🔍💵⚠️✅🏠✈️👶🧾🖥️🗺🚹]+$")
PHONE_PATTERN = re.compile(r"\+?\d(?:[\d\s()\-]{6,}\d)")
HASHTAG_PATTERN = re.compile(r"#[\w\u0400-\u04FF\u2019\u02BB\u02BC]+", re.IGNORECASE)
QUOTED_COMPANY_PATTERN = re.compile(r"(?<!\w)[\"'“«](?P<company>[^\"'”»]+)[\"'”»](?!\w)")

SECTION_PREFIXES = (
    "talablar",
    "vazifalar",
    "vazifalari",
    "majburiyatlar",
    "mas'uliyatlar",
    "masuliyatlar",
    "sharoitlar",
    "qulayliklar",
    "qo'shimcha",
    "qo'shimcha ma'lumot",
    "qo'shimcha ma'lumotlar",
    "kompaniya tomonidan",
    "biz bilan bularga ega bo'lasiz",
    "biz taklif qilamiz",
    "bizdan taklif",
    "ish vaqti",
    "ish vaqti:",
    "ish jadvali",
    "ish grafigi",
    "ish tartibi",
    "yashash",
    "bolalar",
    "telefon",
    "tel",
    "aloqa",
    "bog'lanish",
    "murojaat",
    "anketa",
    "rezyume",
    "eslatma",
    "malaka",
    "afzallik",
    "afzalliklar",
    "ish shartlari",
    "talab va takliflar bilan",
    "nimalar taklif qilinadi",
    "kimlar uchun",
    "вакансиялар",
    "требования",
    "обязанности",
    "условия",
    "зарплата",
    "адрес",
    "талаблар",
    "вазифалар",
    "мажбуриятлари",
    "иш шартлари",
    "иш вақти",
    "иш графиги",
    "ойлик",
    "ойлик маош",
    "маош",
    "манзил",
    "мулжал",
    "мурожаат учун",
    "богланиш учун",
    "боғланиш учун",
    "телефон",
    "тел",
)
FIELD_ONLY_PREFIXES = (
    "hudud",
    "yoshi",
    "jinsi",
    "malumoti",
    "ma'lumoti",
    "oylik",
    "maosh",
    "ish haqi",
    "manzil",
    "mo'ljal",
    "murojaat uchun",
    "aloqa uchun",
    "худуд",
    "ёши",
    "жинси",
    "ойлик",
    "маош",
    "манзил",
    "мулжал",
    "мурожаат учун",
)
NOISE_PREFIXES = (
    "#ish",
    "@xorazm_ish_bor_elonlar",
    "xorazm ish bor elonlar",
)
NON_JOB_TAGS = {
    "#reklama",
    "#kurs",
    "#dars",
    "#telefon",
}
NON_JOB_PHRASES = (
    "o'quv kursi",
    "o‘quv kursi",
    "telefon olmoqchimisiz",
    "telefon sotmoqchimisiz",
    "rus tili tez va oson",
    "ayollarimizga maxsus taklif",
    "onlayn dars",
    "savdo vakillari (agentlar) tayyorlash",
    "сотилади",
    "аксия",
    "0-0-8 aksiya",
    "grant",
    "vakansya kanal",
    "vakansiya kanal",
    "obuna bo'ling",
)
GENERIC_HEADING_PATTERNS = [
    re.compile(r"^(?:📢\s*)?(?:ishga|ишга)\s+taklif!?$", re.IGNORECASE),
    re.compile(r"^(?:📢\s*)?(?:ishga|ишга)\s+taklif\s+qil(?:inadi|amiz|adi)?!?$", re.IGNORECASE),
    re.compile(r"^(?:🔴\s*)?(?:ishga|ишга)\s+taklif!?$", re.IGNORECASE),
    re.compile(r"^(?:📢\s*)?вакансия!?$", re.IGNORECASE),
    re.compile(r"^ish\s+qidiryapsizmi", re.IGNORECASE),
    re.compile(r"^urganch\s+shaxrida$", re.IGNORECASE),
    re.compile(r"^toshkentdan\s+gapiramiz$", re.IGNORECASE),
]
JOB_SIGNAL_PATTERNS = [
    re.compile(r"\b(?:ish\s+lavozimi|ish\s+lavozimlari|vakansiya|вакансиялар)\b", re.IGNORECASE),
    re.compile(r"(?:ishga|ишга)\s+(?:taklif|qabul)", re.IGNORECASE),
    re.compile(r"ишга\s+(?:таклиф|қабул|килади|қилади)", re.IGNORECASE),
    re.compile(r"приглашает\s+на\s+работу", re.IGNORECASE),
    re.compile(r"we\s+are\s+hiring", re.IGNORECASE),
    re.compile(r"\b(?:kerak|talab\s+etiladi|qidirmoqda|izlayapmiz)\b", re.IGNORECASE),
    re.compile(r"qabul\s+qilmoqda", re.IGNORECASE),
    re.compile(r"\b(?:керак|лавозим|маош|манзил|мурожаат|телефон|тел)\b", re.IGNORECASE),
    re.compile(r"\b(?:талаблар|иш\s+тартиби|иш\s+ўринлари|иш\s+уринлари)\b", re.IGNORECASE),
    re.compile(r"\b(?:требования|контакт|контакты|заработанная\s+плата|адрес)\b", re.IGNORECASE),
    re.compile(r"\b(?:requirements|salary|contact|address)\b", re.IGNORECASE),
    re.compile(r"\b(?:maosh|oylik|ish\s+haqi|kunlik)\b", re.IGNORECASE),
    re.compile(r"\b(?:manzil|hudud|murojaat|telefon|tel|aloqa)\b", re.IGNORECASE),
]
EXPLICIT_TITLE_PATTERNS = [
    re.compile(
        r"^(?:[\W_]+\s*)?(?:ish\s+lavozimi|vakansiya|ish\s+o['’`]?rni|bo['’`]?sh\s+ish\s+o['’`]?rni)\s*[:\-]\s*(?P<value>.*)$",
        re.IGNORECASE,
    ),
    re.compile(r"^(?:[\W_]+\s*)?вакансиялар?\s*[:\-]\s*(?P<value>.*)$", re.IGNORECASE),
    re.compile(r"^(?:[\W_]+\s*)?(?:лавозим|должность)\s*[:\-]\s*(?P<value>.*)$", re.IGNORECASE),
]
EXPLICIT_MULTI_PATTERNS = [
    re.compile(
        r"^(?:[\W_]+\s*)?(?:ish\s+lavozimlari|bo['’`]?sh\s+ish\s+o['’`]?rinlari|vakansiyalar)\s*[:\-]?\s*(?P<value>.*)$",
        re.IGNORECASE,
    ),
    re.compile(r"^(?:[\W_]+\s*)?bo['’`]?sh\s+ish\s+o['’`]?rinlari", re.IGNORECASE),
    re.compile(r"^(?:[\W_]+\s*)?вакансиялар", re.IGNORECASE),
    re.compile(r"по\s+следующим\s+вакансиям", re.IGNORECASE),
]
INLINE_COMPANY_PATTERNS = [
    re.compile(
        r"^(?:[\W_]+\s*)?(?:ish\s+beruvchi|kompaniya|kompaniyamiz|korxona|brend)\s*[:\-]\s*(?P<value>.+)$",
        re.IGNORECASE,
    ),
    re.compile(r"^(?:[\W_]+\s*)?ish\s+joyi\s*[:\-]\s*(?P<value>.+)$", re.IGNORECASE),
    re.compile(r"^(?:[\W_]+\s*)?(?:компания|бренд)\s*[:\-]\s*(?P<value>.+)$", re.IGNORECASE),
]
COMPANY_ONLY_INVITATION_PATTERN = re.compile(
    r"^(?P<company>.+?(?:ga|ka|qa|iga|lariga|siga|га|ка|қа|ига|ларга|сига))\s+(?:ishga\s+|ишга\s+)?(?:taklif\s+et(?:adi|amiz)|taklif\s+qil(?:adi|amiz|inadi|moqda)|qabul\s+qil(?:adi|moqda|inadi)|qabul\s+qilin(?:adi|amiz|moqda))\b",
    re.IGNORECASE,
)
PROMO_COMPANY_PATTERN = re.compile(
    r"^(?P<company>.+?)\s+(?:sizga\s+)?bo['’`]sh\s+ish\s+o['’`]rinlarini\s+taklif\s+etadi\b",
    re.IGNORECASE,
)
LEADING_COMPANY_CONTEXT_PATTERN = re.compile(
    r"^(?:yangi\s+)?(?P<company>.+?\b(?:restoran|restorant|kafe|cafe|bar|resto\s*bar|club|pub|do['’`]?kon|market|gipermarket|klinika|markaz|bog['’`]cha|maktab|korxona))(?:ga|ka|qa|iga|siga|га|ка|қа|ига|сига)?$",
    re.IGNORECASE,
)
INLINE_ADDRESS_PATTERNS = [
    re.compile(r"^(?:[\W_]+\s*)?(?:manzil|офис\s+манзили|адрес)\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
    re.compile(r"^(?:[\W_]+\s*)?ish\s+joyi\s*[:\-]\s*(?P<value>.*)$", re.IGNORECASE),
    re.compile(r"^(?:[\W_]+\s*)?манзил\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
]
INLINE_REGION_PATTERNS = [
    re.compile(r"^(?:[\W_]+\s*)?hudud\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
    re.compile(r"^(?:[\W_]+\s*)?худуд\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
]
FILIAL_LOCATION_PATTERN = re.compile(r"^(?P<value>.+?)\s+filialiga\s*:?\s*$", re.IGNORECASE)
INLINE_LANDMARK_PATTERNS = [
    re.compile(r"^(?:[\W_]+\s*)?(?:mo['’‘`]?ljal|marshrut)\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
    re.compile(r"^(?:[\W_]+\s*)?мулжал\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
]
INLINE_SALARY_PATTERNS = [
    re.compile(
        r"^(?:[\W_]+\s*)?(?:maosh|oylik(?:\s+maosh(?:i)?)?|ish\s+haqi(?:\s+va\s+motivatsiya)?|ish\s+haqi|kunlik)\s*[:\-]?\s*(?P<value>.*)$",
        re.IGNORECASE,
    ),
    re.compile(r"^(?:[\W_]+\s*)?(?:маош|ойлик(?:\s+маош(?:и)?)?|иш\s+хаки|иш\s+ҳақи|заработанная\s+плата)\s*[:\-]?\s*(?P<value>.*)$", re.IGNORECASE),
]
INVITATION_ONLY_PATTERN = re.compile(
    r"^(?:📢\s*)?(?:(?:ishga|ишга)\s+taklif(?:\s+qilin(?:adi|amiz|moqda)|\s+qil(?:adi|amiz)|\s+etamiz)?|(?:ishga|ишга)\s+qabul\s+qilin(?:adi|amiz|moqda))\W*$",
    re.IGNORECASE,
)
COMPANY_ROLE_PATTERN = re.compile(
    r"^(?P<company>.+?)(?:ga|ka|qa|iga|lariga|siga|га|ка|қа|ига|ларга|сига)\s+(?P<title>.+?)\s+(?:ishga\s+|ишга\s+)?(?:taklif\s+qilin(?:adi|amiz|moqda|gan)|taflik\s+qilin(?:adi|amiz|moqda|gan)|taklif\s+qil(?:adi|amiz)|taflik\s+qil(?:adi|amiz)|taklif\s+etamiz|qabul\s+qilin(?:adi|amiz|moqda)|qabul\s+qilmoqda|таклиф\s+қил(?:ади|амиз)|таклиф\s+кил(?:ади|амиз)|қабул\s+қилин(?:ади|амиз)|қабул\s+килин(?:ади|амиз)|kerak|керак|qidirmoqda|izlayapmiz)",
    re.IGNORECASE,
)
GENERIC_ROLE_PATTERN = re.compile(
    r"^(?P<title>.+?)\s+(?:ishga\s+|ишга\s+)?(?:taklif\s+qilin(?:adi|amiz|moqda|gan)|taflik\s+qilin(?:adi|amiz|moqda|gan)|taklif\s+qil(?:adi|amiz)|taflik\s+qil(?:adi|amiz)|taklif\s+etamiz|qabul\s+qilin(?:adi|amiz|moqda)|qabul\s+qilmoqda|таклиф\s+қил(?:ади|амиз)|таклиф\s+кил(?:ади|амиз)|қабул\s+қилин(?:ади|амиз)|қабул\s+килин(?:ади|амиз)|kerak|керак|talab\s+etiladi|qidirmoqda|izlayapmiz)",
    re.IGNORECASE,
)
RUSSIAN_RECRUITMENT_PATTERN = re.compile(
    r"^(?P<company>.+?)\s+(?:с[^\s]*\s+)?(?:радостью\s+сообщает\s+о\s+наборе|ищет|приглашает\s+на\s+работу|қидиряпди|қидирмокда)\s+(?P<title>.+?)(?:[.!]|$)",
    re.IGNORECASE,
)
ENGLISH_HIRING_PATTERN = re.compile(
    r"^we\s+are\s+hiring(?:\s+full[- ]time)?\s+(?P<title>.+?)(?:[.!]|$)",
    re.IGNORECASE,
)
MARKETING_CONTEXT_PATTERN = re.compile(
    r"\b(?:jamoa\w*|munosabati|kengay\w*|qo['’`]shiling|sizni\s+kutmoqda|hamkorlikka\s+chorlaymiz|жамоамизга|кенгая)\b",
    re.IGNORECASE,
)
LOCATION_HINT_PATTERN = re.compile(
    r"\b(?:shahar|shahri|tumani|viloyat|ko['’`]cha|ko['’`]chasi|yo['’`]li|yo'li|yoni|ro['’`]parasi|market|baza|mahalla|blok|massiv|gaz|stadion|maktab|bog['’`]?cha|bog['’`]ot|urganch|xonqa|gurlan|hazorasp|shovot|to['’`]rtko['’`]l|beruniy|toshkent|шахар|туман|вилоят|кучаси|йони|ропараси|манзил|турткуль)\b",
    re.IGNORECASE,
)
ROLE_HINT_PATTERN = re.compile(
    r"\b(?:menejer|menedjer|operator|kassir|ofitsiant|oshpaz|barista|povar\w*|povr\w*|xodim|ishchi|ishchilar|agent|savdo|sotuvchi|supervayzer|buxgalter|dizayner|xostes|yordamchi|marketolog|marketing|tarbiyachi|o['’`]qituvchi|psixolog|shartnoma|xo['’`]jalik|bo['’`]lim(?:i|iga)?|mijozlar\s+bilan\s+ishlash|omborchi|ombor\s+mudiri|sklad|moykachi|uborkachi|kliner|tozalovchi|xamirchi|haydovchi|dastavshik|dostavshik|tur\s+agent|kompyuterchi|mebelchi|enaga|reklama\s+tarqatish\s+mutaxassisi|servis\s+menedjeri|call\s+sentra\s+operator|yuk\s+tashuvchi|support\s+teachers?|teacher|logist|dispatch|dispatcher|hr-?specialist|assistant|супервайзер|бухгалтер|хисобчи|ҳисобчи|оператор|кассир|повар|бариста|официант|кондитер|мойщица|доставшик|логист|диспетчир|ассистент|hr-специалист|савдо\s+вакили|менежер|ходими|учител)\b",
    re.IGNORECASE,
)
SUMMARY_TITLE_SIGNAL_PATTERN = re.compile(
    r"\b(?:ish\s+bor|ishga\s+taklif|ishga\s+qabul|kerak|qidirmoqda|izlayapmiz|талаб\s+этилади|иш\s+бор)\b",
    re.IGNORECASE,
)
CONTACT_PREFIXES = (
    "tel",
    "telefon",
    "aloqa",
    "bog'lanish",
    "murojaat",
    "anketa",
    "telegram",
    "qo'shimcha ma'lumot olish uchun",
    "тел",
    "телефон",
    "контакт",
    "мурожаат",
    "боғланиш учун",
    "богланиш учун",
)
GENERIC_TARGET_PATTERN = re.compile(
    r"\b(?:yigit(?:lar|larni)?|qiz(?:lar|larni)?|ayol(?:lar|larni)?|erkak(?:lar|larni)?|talaba(?:lar|larni)?|xodim(?:lar|larni)?)\b",
    re.IGNORECASE,
)


class Parser(BaseParser):
    def parse(self, message: "Message") -> Job | None:
        text = self.get_message_text(message)
        if not text:
            return None

        cleaned_text = self._cleanup_text(text)
        if not self._looks_like_job_post(cleaned_text):
            return None

        lines = self._split_lines(cleaned_text)
        if not lines or self._has_blocked_signal(lines, cleaned_text):
            return None

        heading_index, heading = self._extract_heading(lines)
        role_lines = self._extract_role_lines(lines, heading_index)
        company = self._extract_company(lines, heading)
        title = self._extract_title(lines, heading_index, heading, role_lines)
        location = self._extract_location(lines)
        salary = self._extract_salary(lines)

        if not title:
            return None

        return self.build_job(
            message=message,
            title=title,
            company=company,
            location=location,
            salary=salary,
            description=cleaned_text,
        )

    def _looks_like_job_post(self, text: str) -> bool:
        lowered = text.lower()
        if any(phrase in lowered for phrase in NON_JOB_PHRASES):
            return False
        if any(pattern.search(text) for pattern in JOB_SIGNAL_PATTERNS):
            return True

        signals = 0
        for marker in ("manzil", "hudud", "mo'ljal", "maosh", "oylik", "ish haqi", "telefon", "aloqa", "murojaat"):
            if marker in lowered:
                signals += 1
        for marker in ("манзил", "маош", "ойлик", "тел", "мурожаат", "лавозим", "керак"):
            if marker in lowered:
                signals += 1
        return signals >= 3

    def _has_blocked_signal(self, lines: list[str], text: str) -> bool:
        lowered = text.lower()
        if any(phrase in lowered for phrase in NON_JOB_PHRASES):
            return True

        for line in lines[:4]:
            tags = {tag.lower() for tag in HASHTAG_PATTERN.findall(line)}
            if tags & NON_JOB_TAGS:
                return True
        return False

    def _cleanup_text(self, text: str) -> str:
        normalized = text.replace("\xa0", " ").replace("\u200b", " ")
        normalized = normalized.replace("\ufe0f", "")
        normalized = normalized.replace("\r\n", "\n").replace("\r", "\n")
        normalized = re.sub(r"\n{3,}", "\n\n", normalized)
        return normalized.strip()

    def _split_lines(self, text: str) -> list[str]:
        lines: list[str] = []
        for raw_line in text.splitlines():
            line = self._normalize_line(raw_line)
            if line:
                lines.append(line)
        return lines

    def _extract_heading(self, lines: list[str]) -> tuple[int, str]:
        for index, line in enumerate(lines):
            if self._is_heading_noise(line):
                continue

            if index + 1 < len(lines) and INVITATION_ONLY_PATTERN.search(lines[index + 1]):
                return index, line

            return index, line

        return 0, lines[0] if lines else ""

    def _extract_role_lines(self, lines: list[str], heading_index: int) -> list[str]:
        explicit_marker_index = self._find_explicit_multi_marker(lines)
        if explicit_marker_index is not None:
            return self._collect_explicit_role_lines(lines, explicit_marker_index + 1)

        invitation_marker = self._find_invitation_role_marker(lines)
        if invitation_marker is not None:
            marker_index, explicit_context = invitation_marker
            if explicit_context:
                return self._collect_explicit_role_lines(lines, marker_index + 1)
            return self._collect_role_lines(lines, marker_index + 1, explicit_context=False)

        if 0 <= heading_index < len(lines):
            return self._collect_role_lines(lines, heading_index + 1, explicit_context=False)

        return []

    def _extract_company(self, lines: list[str], heading: str) -> str | None:
        inline_company = self._extract_inline_company(lines)
        if inline_company:
            return inline_company

        for candidate in self._leading_context_lines(lines, heading):
            if self._starts_labeled_field(candidate):
                continue

            quoted = QUOTED_COMPANY_PATTERN.search(candidate)
            if quoted:
                return self._cleanup_company(quoted.group("company"))

            promo_match = PROMO_COMPANY_PATTERN.search(candidate)
            if promo_match:
                return self._cleanup_company(promo_match.group("company"))

            match = COMPANY_ROLE_PATTERN.search(candidate)
            if match:
                extracted_title = self._cleanup_title(match.group("title"))
                if not self._is_unreliable_extracted_title(extracted_title):
                    return self._cleanup_company(match.group("company"))

            company_only_match = COMPANY_ONLY_INVITATION_PATTERN.search(candidate)
            if company_only_match:
                cleaned_company = self._cleanup_company(company_only_match.group("company"))
                if cleaned_company and not self._is_unreliable_company_candidate(cleaned_company):
                    return cleaned_company

            context_company = self._extract_heading_company(candidate)
            if context_company:
                return context_company

        return None

    def _extract_title(self, lines: list[str], heading_index: int, heading: str, role_lines: list[str]) -> str | None:
        explicit_title = self._extract_explicit_title(lines)
        if explicit_title:
            return explicit_title

        summary_title = self._extract_summary_title(lines, heading_index)
        if role_lines:
            if len(role_lines) == 1 and summary_title:
                return summary_title
            return self._join_roles(role_lines)

        if summary_title:
            return summary_title

        next_line = lines[heading_index + 1] if 0 <= heading_index + 1 < len(lines) else ""
        if heading and next_line and INVITATION_ONLY_PATTERN.search(next_line):
            return self._cleanup_title(heading)

        for candidate in self._leading_context_lines(lines, heading):
            if self._is_section_line(candidate) or self._starts_labeled_field(candidate):
                continue

            match = COMPANY_ROLE_PATTERN.search(candidate)
            if match:
                extracted_title = self._cleanup_title(match.group("title"))
                if self._should_preserve_full_heading(extracted_title) or extracted_title.lower() in {"ish", "ishga", "taklif"}:
                    return self._cleanup_heading(candidate)
                return self._cleanup_heading(candidate)

            match = GENERIC_ROLE_PATTERN.search(candidate)
            if match:
                extracted_title = self._cleanup_title(match.group("title"))
                if self._should_preserve_full_heading(extracted_title) or extracted_title.lower() in {"ish", "ishga", "taklif"}:
                    return self._cleanup_heading(candidate)
                return self._cleanup_heading(candidate)

            quoted = QUOTED_COMPANY_PATTERN.search(candidate)
            if quoted and ROLE_HINT_PATTERN.search(quoted.group("company")):
                return self._cleanup_title(quoted.group("company"))

            russian_match = RUSSIAN_RECRUITMENT_PATTERN.search(candidate)
            if russian_match:
                return self._cleanup_heading(candidate)

            english_match = ENGLISH_HIRING_PATTERN.search(self._strip_leading_symbols(candidate))
            if english_match:
                return self._cleanup_heading(candidate)

        if self._is_heading_noise(heading):
            return None

        return self._cleanup_title(heading) or None

    def _extract_location(self, lines: list[str]) -> str | None:
        location = self._extract_labeled_field(lines, INLINE_ADDRESS_PATTERNS)
        if not location:
            location = self._extract_labeled_field(lines, INLINE_REGION_PATTERNS)
        if not location:
            location = self._extract_filial_location(lines)
        landmark = self._extract_labeled_field(lines, INLINE_LANDMARK_PATTERNS)

        if location and self._looks_like_company(location) and not self._looks_like_location(location):
            location = None

        if location and landmark:
            return f"{location} ({landmark})"
        if location or landmark:
            return location or landmark
        return None

    def _extract_filial_location(self, lines: list[str]) -> str | None:
        for line in lines[:6]:
            match = FILIAL_LOCATION_PATTERN.search(self._strip_leading_symbols(line))
            if not match:
                continue
            value = self._cleanup_field_value(match.group("value"))
            if value and not self._looks_like_company(value):
                return value
        return None

    def _extract_salary(self, lines: list[str]) -> str | None:
        return self._extract_labeled_field(lines, INLINE_SALARY_PATTERNS, allow_multiline=True)

    def _extract_summary_title(self, lines: list[str], heading_index: int) -> str | None:
        if heading_index < 0:
            return None

        boundary_index = len(lines)
        for index in range(heading_index + 1, len(lines)):
            candidate = lines[index]
            if self._starts_labeled_field(candidate) or self._is_section_line(candidate) or self._is_contact_line(candidate):
                boundary_index = index
                break

        summary_index: int | None = None
        for index in range(heading_index + 1, boundary_index):
            candidate = lines[index]
            cleaned = self._cleanup_heading(candidate)
            if not cleaned or self._is_noise_line(candidate):
                continue
            if SUMMARY_TITLE_SIGNAL_PATTERN.search(cleaned) and ROLE_HINT_PATTERN.search(cleaned):
                summary_index = index
                break

        if summary_index is None:
            return None

        bullet_count = 0
        for index in range(heading_index + 1, summary_index):
            line = lines[index]
            if line and (line[0] in ROLE_LIST_BULLET_CHARS or re.match(r"^\s*\d+[.)]", line)):
                bullet_count += 1

        if bullet_count < 3:
            return None

        summary = self._cleanup_heading(lines[summary_index])
        context = self._extract_summary_context(lines, heading_index, summary_index)
        if context:
            return self._cleanup_heading(f"{context} {summary}")
        return summary

    def _extract_summary_context(self, lines: list[str], heading_index: int, summary_index: int) -> str | None:
        for index in range(heading_index, summary_index):
            candidate = lines[index]
            if self._is_noise_line(candidate) or self._starts_labeled_field(candidate) or self._is_section_line(candidate):
                continue
            cleaned = self._cleanup_heading(candidate)
            if not cleaned or cleaned.lower() in {"yangi ish", "ish bor"}:
                continue
            if cleaned.startswith(("•", "-", "▪", "▫")):
                continue
            if self._extract_heading_company(cleaned):
                return self._cleanup_heading(re.sub(r"(?i)^yangi\s+", "", cleaned))
        return None

    def _extract_heading_company(self, value: str) -> str | None:
        cleaned = self._cleanup_heading(value)
        if not cleaned:
            return None
        cleaned = re.sub(r"(?i)^yangi\s+", "", cleaned).strip()
        match = LEADING_COMPANY_CONTEXT_PATTERN.search(cleaned)
        if not match:
            return None
        return self._cleanup_company(match.group("company"))

    def _extract_explicit_title(self, lines: list[str]) -> str | None:
        for index, line in enumerate(lines):
            for pattern in EXPLICIT_TITLE_PATTERNS:
                match = pattern.search(line)
                if not match:
                    continue

                value = self._cleanup_title(match.group("value"))
                if value:
                    return value

                if index + 1 < len(lines):
                    candidate = self._cleanup_title(lines[index + 1])
                    if candidate and not self._starts_labeled_field(candidate):
                        return candidate
        return None

    def _extract_inline_company(self, lines: list[str]) -> str | None:
        for line in lines:
            for pattern in INLINE_COMPANY_PATTERNS:
                match = pattern.search(line)
                if not match:
                    continue

                value = self._cleanup_inline_company(match.group("value"))
                if not value:
                    continue

                if self._looks_like_location(value):
                    continue
                return value
        return None

    def _extract_labeled_field(
        self,
        lines: list[str],
        patterns: list[re.Pattern[str]],
        *,
        allow_multiline: bool = False,
    ) -> str | None:
        for index, line in enumerate(lines):
            for pattern in patterns:
                match = pattern.search(line)
                if not match:
                    continue

                values: list[str] = []
                initial = self._cleanup_field_value(match.group("value"))
                if initial:
                    values.append(initial)

                should_consume_next = allow_multiline or not initial
                next_index = index + 1
                while should_consume_next and next_index < len(lines):
                    candidate = lines[next_index]
                    if self._is_noise_line(candidate):
                        next_index += 1
                        continue
                    if self._starts_labeled_field(candidate):
                        break
                    if values and self._is_section_line(candidate):
                        break
                    if self._is_contact_line(candidate) and pattern in INLINE_SALARY_PATTERNS:
                        break
                    if pattern in INLINE_SALARY_PATTERNS and values and not self._looks_like_salary_continuation(candidate):
                        break

                    cleaned = self._cleanup_field_value(candidate)
                    if not cleaned:
                        next_index += 1
                        continue

                    values.append(cleaned)
                    next_index += 1
                    if not allow_multiline:
                        break

                if not values:
                    continue

                joined = "; ".join(dict.fromkeys(values))
                if pattern in INLINE_SALARY_PATTERNS:
                    joined = self._cleanup_salary_value(joined)
                return joined.strip(" -:;,.")
        return None

    def _find_explicit_multi_marker(self, lines: list[str]) -> int | None:
        for index, line in enumerate(lines[:8]):
            if any(pattern.search(line) for pattern in EXPLICIT_MULTI_PATTERNS):
                return index
        return None

    def _find_invitation_role_marker(self, lines: list[str]) -> tuple[int, bool] | None:
        for index, line in enumerate(lines[:8]):
            lowered = line.lower()
            if "quyidagi" in lowered and ("ishga taklif" in lowered or "ishga qabul" in lowered):
                return index, True
            if self._is_promo_heading(line):
                return index, True
            if INVITATION_ONLY_PATTERN.search(line):
                previous = lines[index - 1] if index > 0 else ""
                if previous and not self._is_heading_noise(previous) and self._looks_like_company(previous):
                    return index, True
                return index, False
            if ("ishga taklif" in lowered or "ishga qabul" in lowered) and index + 1 < len(lines) and self._is_role_candidate(lines[index + 1], explicit_context=False):
                return index, True
        return None

    def _collect_role_lines(self, lines: list[str], start_index: int, *, explicit_context: bool) -> list[str]:
        roles: list[str] = []
        for line in lines[start_index:]:
            if self._is_section_line(line) or self._starts_labeled_field(line):
                if roles:
                    break
                continue
            if self._is_noise_line(line) or INVITATION_ONLY_PATTERN.search(line):
                continue

            if self._is_role_candidate(line, explicit_context=explicit_context):
                roles.append(self._cleanup_title(line))
                continue

            if roles:
                break

            if explicit_context:
                continue

            # Generic promo headings can have a short intro before bullet roles.
            if len(line.split()) <= 6 and not self._looks_like_location(line):
                continue
            break

        return [role for role in dict.fromkeys(roles) if role]

    def _collect_explicit_role_lines(self, lines: list[str], start_index: int) -> list[str]:
        roles = self._collect_role_lines(lines, start_index, explicit_context=True)
        for index in range(start_index, len(lines)):
            if not self._is_followup_role_heading(lines, index):
                continue
            roles.append(self._cleanup_title(lines[index]))
        return [role for role in dict.fromkeys(roles) if role]

    def _leading_context_lines(self, lines: list[str], heading: str) -> list[str]:
        candidates: list[str] = []
        if heading:
            candidates.append(heading)

        for line in lines[:6]:
            if line not in candidates and not self._is_noise_line(line):
                candidates.append(line)
        return candidates

    def _is_heading_noise(self, line: str) -> bool:
        lowered = self._strip_leading_symbols(line).lower()
        if self._is_noise_line(line):
            return True
        if any(lowered.startswith(prefix) for prefix in NOISE_PREFIXES):
            return True
        if any(pattern.search(self._strip_leading_symbols(line)) for pattern in GENERIC_HEADING_PATTERNS):
            return True
        if any(lowered.startswith(prefix) for prefix in FIELD_ONLY_PREFIXES):
            return True
        return False

    def _is_noise_line(self, line: str) -> bool:
        lowered = line.lower()
        if not line:
            return True
        if line.startswith("👉 @"):
            return True
        if lowered in {"#ish", "ish", "vakansiya"}:
            return True
        return DECORATIVE_ONLY_PATTERN.fullmatch(line) is not None

    def _is_section_line(self, line: str) -> bool:
        lowered = self._strip_leading_symbols(line).lower()
        return any(lowered.startswith(prefix) for prefix in SECTION_PREFIXES)

    def _starts_labeled_field(self, line: str) -> bool:
        return any(
            pattern.search(line)
            for pattern in INLINE_COMPANY_PATTERNS
            + INLINE_ADDRESS_PATTERNS
            + INLINE_REGION_PATTERNS
            + INLINE_LANDMARK_PATTERNS
            + INLINE_SALARY_PATTERNS
            + EXPLICIT_TITLE_PATTERNS
            + EXPLICIT_MULTI_PATTERNS
        )

    def _is_contact_line(self, line: str) -> bool:
        lowered = self._strip_leading_symbols(line).lower()
        if any(lowered.startswith(prefix) for prefix in CONTACT_PREFIXES):
            return True
        if self._looks_like_phone_line(lowered):
            return True
        return lowered.startswith("@") or "t.me/" in lowered or lowered.startswith("http")

    def _is_promo_heading(self, line: str) -> bool:
        lowered = line.lower()
        return (
            "ish qidiryapsizmi" in lowered
            or "sizni kutmoqda" in lowered
            or "bo'sh ish o'rinlarini taklif etadi" in lowered
            or "bo‘sh ish o‘rinlarini taklif etadi" in lowered
        )

    def _is_role_candidate(self, line: str, *, explicit_context: bool) -> bool:
        cleaned = self._cleanup_title(line)
        lowered = cleaned.lower()
        if not cleaned or self._is_section_line(cleaned) or self._is_contact_line(cleaned):
            return False
        if self._looks_like_location(cleaned):
            return False
        if len(cleaned) > 80:
            return False
        if any(token in lowered for token in ("ahil jamoa", "zo'r atmosfera", "o'sish imkoniyati", "ish formasi", "issiq ovqat", "mavsum vaqtida")):
            return False

        has_bullet = line[0] in ROLE_LIST_BULLET_CHARS or bool(re.match(r"^\s*\d+[.)]", line))
        has_role_hint = ROLE_HINT_PATTERN.search(cleaned) is not None
        if explicit_context:
            return has_bullet or has_role_hint
        return has_role_hint and (has_bullet or len(cleaned.split()) <= 6 or cleaned.isupper())

    def _is_followup_role_heading(self, lines: list[str], index: int) -> bool:
        line = lines[index]
        cleaned = self._cleanup_title(line)
        lowered = cleaned.lower()
        if not cleaned or self._is_section_line(cleaned) or self._starts_labeled_field(cleaned) or self._is_contact_line(cleaned):
            return False
        if self._looks_like_location(cleaned):
            return False
        if len(cleaned) > 60:
            return False
        if any(
            lowered.startswith(prefix)
            for prefix in (
                "faqat ",
                "kamida ",
                "tajriba ",
                "mas'uliyat",
                "masuliyat",
                "ijtimoiy ",
                "tashqi ",
                "kreativ ",
                "barqaror ",
                "ahil ",
                "professional ",
                "qulay ",
                "shaxsiy ",
                "oylik ",
                "maosh ",
                "ish vaqti",
                "manzil",
                "hudud",
                "telefon",
                "tel",
                "aloqa",
            )
        ):
            return False
        if ":" in cleaned and (
            any(marker in lowered for marker in ("kunlik", "so'm", "so‘m", "mln", "maosh", "oylik", "ish haqi"))
            or any(char.isdigit() for char in cleaned)
        ):
            return False

        has_bullet = line[0] in ROLE_LIST_BULLET_CHARS or bool(re.match(r"^\s*\d+[.)]", line))
        has_role_hint = ROLE_HINT_PATTERN.search(cleaned) is not None
        if not has_bullet and not has_role_hint:
            return False
        if not has_role_hint and not self._looks_like_title_case_role(cleaned):
            return False

        next_line = self._next_significant_line(lines, index)
        if not next_line:
            return False
        return self._is_section_line(next_line) or self._starts_labeled_field(next_line) or self._is_contact_line(next_line)

    def _should_preserve_full_heading(self, extracted_title: str) -> bool:
        return MARKETING_CONTEXT_PATTERN.search(extracted_title) is not None or self._is_unreliable_extracted_title(extracted_title)

    def _looks_like_location(self, value: str) -> bool:
        return LOCATION_HINT_PATTERN.search(value) is not None

    def _looks_like_company(self, value: str) -> bool:
        lowered = value.lower()
        return any(
            marker in lowered
            for marker in (
                "mchj",
                "kompaniya",
                "firmasi",
                "restoran",
                "kafe",
                "club",
                "klinika",
                "markaz",
                "do'kon",
                "bog'cha",
                "maktab",
                "offis",
                "ofis",
                "компания",
                "фирма",
                "кафе",
                "групп",
                "дилери",
                "корхона",
            )
        )

    def _cleanup_title(self, value: str) -> str:
        cleaned = self._strip_leading_symbols(value)
        cleaned = self._strip_bullet(cleaned)
        cleaned = re.sub(r"\*{2,}", " ", cleaned)
        cleaned = re.sub(r"^(?:bo['’`]?sh\s+ish\s+o['’`]?rinlari\s*[⬇️:]*)", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"^(?:ish\s+lavozimlari?\s*[:\-]?)", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"^(?:vakansiya\s*[:\-]?)", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"^(?:бизнинг\s+жамоамизга\s+)", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"^(?:корхонамиз\s+кенгая[^\s]*\s+сабабли\s+)", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"^(?:we\s+are\s+hiring(?:\s+full[- ]time)?\s+)", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"^.+?\bбуйича\s+иш\s+тажрибасига\s+эга\s+", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(
            r"\s+(?:ishga\s+|ишга\s+)?(?:taklif\s+et(?:adi|amiz)|taklif\s+qil(?:adi|amiz|inadi|moqda)|qabul\s+qil(?:adi|moqda|inadi)|qabul\s+qilin(?:adi|amiz|moqda))\b.*$",
            "",
            cleaned,
            flags=re.IGNORECASE,
        )
        cleaned = re.sub(r"(?:ga|ka|qa|га|ка|қа)(?=\s*\()", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"(?:ga|ka|qa|га|ка|қа)$", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"\s*[^\w\u0400-\u04FF()]+$", "", cleaned)
        cleaned = re.sub(r"\s{2,}", " ", cleaned)
        return cleaned.strip(" -:;,.!")

    def _cleanup_heading(self, value: str) -> str:
        cleaned = self._strip_leading_symbols(value)
        cleaned = self._strip_bullet(cleaned)
        cleaned = re.sub(r"\*{2,}", " ", cleaned)
        cleaned = re.sub(r"\s*[^\w\u0400-\u04FF()]+$", "", cleaned)
        cleaned = re.sub(r"\s{2,}", " ", cleaned)
        return cleaned.strip(" -:;,.!")

    def _cleanup_company(self, value: str) -> str | None:
        cleaned = self._cleanup_heading(value)
        cleaned = re.sub(r"(?:ga|ka|qa|га|ка|қа)$", "", cleaned, flags=re.IGNORECASE).strip()
        cleaned = re.sub(r"\s{2,}", " ", cleaned)
        return cleaned.strip(" -:;,.") or None

    def _cleanup_inline_company(self, value: str) -> str | None:
        cleaned = self._cleanup_heading(value)
        cleaned = re.sub(r"\s{2,}", " ", cleaned)
        return cleaned.strip(" -:;,.") or None

    def _cleanup_field_value(self, value: str) -> str:
        cleaned = self._strip_leading_symbols(value)
        cleaned = re.sub(r"\*{2,}", " ", cleaned)
        cleaned = re.sub(r"\s{2,}", " ", cleaned)
        return cleaned.strip(" -:;,.")

    def _cleanup_salary_value(self, value: str) -> str:
        cleaned = value.replace("*", "")
        cleaned = re.sub(r"\s{2,}", " ", cleaned).strip(" -:;,.")
        lowered = cleaned.lower()
        if any(marker in lowered for marker in ("so'm", "so‘m", "som", "mln", "ming", "million")):
            cleaned = re.sub(r"^(?:povr|povar|oshpaz|barista)\s+", "", cleaned, flags=re.IGNORECASE)
        return cleaned.strip(" -:;,.")

    def _looks_like_salary_continuation(self, value: str) -> bool:
        cleaned = self._cleanup_field_value(value)
        lowered = cleaned.lower()
        if not cleaned:
            return False
        if self._looks_like_phone_line(cleaned):
            return False
        return any(
            marker in lowered
            for marker in (
                "so'm",
                "so‘m",
                "mln",
                "million",
                "ming",
                "bonus",
                "fiksa",
                "foiz",
                "kpi",
                "suhbat",
                "kelish",
            )
        ) or bool(re.search(r"\d", cleaned))

    def _looks_like_phone_line(self, value: str) -> bool:
        cleaned = self._cleanup_field_value(value)
        if not cleaned:
            return False
        if re.search(r"[A-Za-z\u0400-\u04FF]", cleaned):
            return False
        digit_count = sum(char.isdigit() for char in cleaned)
        if cleaned.startswith("+"):
            return digit_count >= 7
        return digit_count >= 9

    def _is_unreliable_extracted_title(self, value: str) -> bool:
        lowered = value.lower()
        if lowered in {"ish", "ishga", "taklif"}:
            return True
        return ROLE_HINT_PATTERN.search(value) is None and GENERIC_TARGET_PATTERN.search(value) is not None

    def _is_unreliable_company_candidate(self, value: str) -> bool:
        lowered = value.lower()
        return GENERIC_TARGET_PATTERN.search(value) is not None or "ishga" in lowered or "taklif" in lowered

    def _next_significant_line(self, lines: list[str], index: int) -> str | None:
        next_index = index + 1
        while next_index < len(lines):
            candidate = lines[next_index]
            if self._is_noise_line(candidate):
                next_index += 1
                continue
            return candidate
        return None

    def _looks_like_title_case_role(self, value: str) -> bool:
        tokens = re.findall(r"[A-Za-z\u0400-\u04FFʻ’'`-]+", value)
        if not tokens or len(tokens) > 4:
            return False
        return all(token[:1].isupper() or token.isupper() for token in tokens)

    def _join_roles(self, roles: list[str]) -> str | None:
        unique_roles = [role for role in dict.fromkeys(roles) if role]
        if not unique_roles:
            return None
        if len(unique_roles) <= 5:
            return " / ".join(unique_roles)
        visible = " / ".join(unique_roles[:5])
        return f"{visible} +{len(unique_roles) - 5}"

    def _normalize_line(self, line: str) -> str:
        compact = re.sub(r"\s+", " ", line.strip())
        return compact.strip()

    def _strip_leading_symbols(self, value: str) -> str:
        return re.sub(r"^[\s📣🔉💥🔥✅❗❕❌➖➕👍👆👇☎📞📱🆔🗓💼🏢📍🎯⏰💰📝📋📌🚗🕰💎🕹📢🔹🔸🔴✨👥🙋🟢🔍💵⚠️🏠✈️👶🧾🖥️🗺🚹🧩▫▪\ufe0f]+", "", value).strip()

    def _strip_bullet(self, value: str) -> str:
        return ROLE_BULLET_PATTERN.sub("", value).strip()
