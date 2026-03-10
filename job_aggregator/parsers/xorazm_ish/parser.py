from __future__ import annotations

import re
from typing import TYPE_CHECKING

from job_aggregator.core.parser_base import BaseParser
from job_aggregator.models.job import Job

if TYPE_CHECKING:
    from telethon.tl.custom.message import Message

SECTION_PREFIXES = (
    "talablar",
    "umumiy talablar",
    "vazifasi",
    "vazifalar",
    "asosiy vazifalar",
    "ish vaqti",
    "ish tartibi",
    "ish grafigi",
    "oylik",
    "kunlik",
    "maosh",
    "manzil",
    "mo'ljal",
    "murojaat",
    "tel",
    "telefon",
    "aloqa",
    "rezyume",
    "anketa",
    "biz taklif qilamiz",
    "qulayliklar",
    "yosh chegarasi",
    "qo'shimcha",
    "общие требования",
    "требования",
    "обязанности",
    "рабочее время",
    "график работы",
    "адрес",
    "ориентир",
    "заработная плата",
    "зарплата",
)
NOISE_PREFIXES = (
    "#ish",
    "@xorazm_ish",
    "xorazm ish",
    "kanalga obuna",
)
NON_JOB_TAGS = {
    "#yangilik",
    "#fikr",
    "#mulohaza",
    "#foydali",
    "#tabrik",
    "#reklama",
    "#eslatma",
    "#malumot",
    "#ishchi_topildi",
    "#ish_kerak",
}
NON_JOB_PHRASES = (
    "ushbu reklama egasi ishchi topganini ma'lum qildi",
    "o'zingizga zarur ishchini biz orqali toping",
    "ishchi topganini ma'lum qildi",
)
ROLE_BULLET_CHARS = "-•▪*●◦"
ROLE_BULLET_PATTERN = re.compile(r"^[\s\-•▪*●◦\u25aa\ufe0f]+")
HASHTAG_PATTERN = re.compile(r"#[\w\u0400-\u04FF\u2019\u02BB\u02BC]+", re.IGNORECASE)
JOB_SIGNAL_PATTERNS = [
    re.compile(r"ishga\s+taklif\s+qilin", re.IGNORECASE),
    re.compile(r"\b(?:vakansiya|bo'sh\s+ish\s+o'rni|lavozim|xodim\s+kerak|ishchi\s+kerak)\b", re.IGNORECASE),
    re.compile(r"\b(?:oylik|kunlik|maosh)\b", re.IGNORECASE),
    re.compile(r"приглашает\s+на\s+работу", re.IGNORECASE),
    re.compile(r"\b(?:должность|заработная\s+плата|зарплата|адрес)\b", re.IGNORECASE),
]
HEADING_ROLE_PATTERN = re.compile(
    r"^(?P<company>.+?)(?:ga|ka|qa)\s+(?P<title>.+?)\s+ishga\s+taklif\s+qilin(?:adi|adi!|adi\.|moqda|gan)",
    re.IGNORECASE,
)
GENERIC_ROLE_PATTERN = re.compile(
    r"^(?P<title>.+?)\s+ishga\s+taklif\s+qilin(?:adi|adi!|adi\.|moqda|gan)",
    re.IGNORECASE,
)
NEEDS_ROLE_PATTERN = re.compile(
    r"^(?P<title>.+?)\s+(?:kerak|talab\s+etiladi)",
    re.IGNORECASE,
)
RUSSIAN_HEADING_PATTERN = re.compile(
    r"^(?P<company>.+?)\s+приглашает\s+на\s+работу(?:.*?)(?:(?:на\s+должность|на\s+следующие\s+должности)\.?\s*)?$",
    re.IGNORECASE,
)
QUOTED_COMPANY_PATTERN = re.compile(r"[\"'“«](?P<company>[^\"'”»]+)[\"'”»]")
INLINE_FIELD_PATTERNS = {
    "company": re.compile(r"^(?:kompaniya|ish\s+joyi|korxona)\s*[:\-]\s*(?P<value>.+)$", re.IGNORECASE),
    "location": re.compile(r"^(?:manzil|адрес)\s*[:\-]?\s*(?P<value>.+)$", re.IGNORECASE),
    "landmark": re.compile(r"^(?:mo'ljal|ориентир)\s*[:\-]?\s*(?P<value>.+)$", re.IGNORECASE),
    "salary": re.compile(r"^(?:maosh|oylik|kunlik|заработная\s+плата|зарплата)\s*[:\-]?\s*(?P<value>.+)$", re.IGNORECASE),
}
HEADING_LOCATION_PATTERN = re.compile(
    r"(?P<value>[A-Za-zА-Яа-яЁёЎўҚқҒғҲҳʼʻ‘’\"'.,\-\s]+?)\s+filial(?:iga|lariga)\b",
    re.IGNORECASE,
)
HEADING_LOCATION_RU_PATTERN = re.compile(
    r"(?:город|г\.)\s*(?P<value>[A-Za-zА-Яа-яЁёЎўҚқҒғҲҳʼʻ‘’\"'.,\-\s]+)",
    re.IGNORECASE,
)
SALARY_HINT_PATTERN = re.compile(
    r"\b(?:maosh|oylik|kunlik|fiksa|bonus|foiz|suhbat\s+asosida|kelishiladi|so'm|ming|mln|million|\$|зарплата|заработная\s+плата|собеседован)\b",
    re.IGNORECASE,
)
BENEFIT_PREFIXES = ("✓", "✔", "✅")
HEADING_TITLE_PREFIXES = (
    "o‘quv markazining",
    "o'quv markazining",
    "xususiy maktabning",
    "nodavlat ta’lim muassasasiga",
    "nodavlat ta'lim muassasasiga",
    "mchj",
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
        if not lines:
            return None
        if self._has_blocked_signal(lines, cleaned_text):
            return None

        heading_index, heading = self._extract_heading(lines)
        role_lines = self._extract_role_lines(lines, heading_index)
        company = self._extract_company(lines, heading)
        title = self._extract_title(heading, role_lines)
        location = self._extract_location(lines, heading, company)
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
        return any(pattern.search(lowered) for pattern in JOB_SIGNAL_PATTERNS)

    def _has_blocked_signal(self, lines: list[str], text: str) -> bool:
        lowered_text = text.lower()
        if any(phrase in lowered_text for phrase in NON_JOB_PHRASES):
            return True

        for line in lines[:3]:
            tags = {tag.lower() for tag in HASHTAG_PATTERN.findall(line)}
            if tags & NON_JOB_TAGS:
                return True
        return False

    def _cleanup_text(self, text: str) -> str:
        normalized = text.replace("\xa0", " ").replace("\u200b", " ")
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
            if self._is_noise_line(line):
                continue
            return index, line
        return 0, lines[0] if lines else ""

    def _extract_role_lines(self, lines: list[str], heading_index: int) -> list[str]:
        heading = lines[heading_index] if 0 <= heading_index < len(lines) else ""
        lowered_heading = heading.lower()
        collect_roles = any(
            marker in lowered_heading
            for marker in ("quyidagi lavozim", "lavozimlar", "должност", "на должность", "на следующие должности")
        )

        roles: list[str] = []
        for line in lines[heading_index + 1 :]:
            if self._is_section_line(line):
                break
            if not collect_roles and line and line[0] not in ROLE_BULLET_CHARS:
                break
            if not self._is_role_candidate(line):
                if collect_roles and roles:
                    break
                continue
            collect_roles = True
            roles.append(self._strip_bullet(line))
        return roles if collect_roles else []

    def _extract_company(self, lines: list[str], heading: str) -> str | None:
        inline_company = self._extract_inline_field(lines, "company")
        if inline_company:
            return inline_company

        quoted_match = QUOTED_COMPANY_PATTERN.search(heading)
        if quoted_match:
            return quoted_match.group("company").strip()

        heading_match = HEADING_ROLE_PATTERN.search(heading)
        if heading_match:
            return self._cleanup_company(heading_match.group("company"))

        russian_match = RUSSIAN_HEADING_PATTERN.search(heading)
        if russian_match:
            return self._cleanup_company(russian_match.group("company"))

        return None

    def _extract_title(self, heading: str, role_lines: list[str]) -> str | None:
        if role_lines:
            compact_roles = [self._compact_title(role) for role in role_lines if role]
            return self._join_roles(compact_roles)

        for pattern in (HEADING_ROLE_PATTERN, GENERIC_ROLE_PATTERN, NEEDS_ROLE_PATTERN):
            match = pattern.search(heading)
            if match:
                return self._cleanup_title(match.group("title"))

        if RUSSIAN_HEADING_PATTERN.search(heading) and role_lines:
            compact_roles = [self._compact_title(role) for role in role_lines if role]
            return self._join_roles(compact_roles)

        if self._is_noise_line(heading):
            return None

        cleaned_heading = self._cleanup_title(self._remove_invitation_suffix(heading))
        return cleaned_heading or None

    def _extract_location(self, lines: list[str], heading: str = "", company: str | None = None) -> str | None:
        location = self._extract_inline_field(lines, "location")
        landmark = self._extract_inline_field(lines, "landmark")
        if location and landmark:
            return f"{location} ({landmark})"
        if location or landmark:
            return location or landmark

        heading_location = self._extract_heading_location(heading, company)
        return heading_location

    def _extract_salary(self, lines: list[str]) -> str | None:
        explicit_salary = self._extract_inline_field(lines, "salary")
        if explicit_salary:
            return explicit_salary

        salary_lines: list[str] = []
        for line in lines:
            lowered = line.lower()
            if lowered.startswith(("oylik", "kunlik", "maosh")) or SALARY_HINT_PATTERN.search(line):
                if self._is_contact_line(line):
                    continue
                if line.startswith(BENEFIT_PREFIXES):
                    continue
                salary_lines.append(self._strip_bullet(line))

        if not salary_lines:
            return None

        return "; ".join(dict.fromkeys(salary_lines))

    def _extract_inline_field(self, lines: list[str], field_name: str) -> str | None:
        pattern = INLINE_FIELD_PATTERNS[field_name]
        for line in lines:
            match = pattern.search(line)
            if match:
                return match.group("value").strip().rstrip(".")
        return None

    def _is_noise_line(self, line: str) -> bool:
        lowered = line.lower()
        if lowered in {"#ish", "ish", "vakansiya"}:
            return True
        return any(lowered.startswith(prefix) for prefix in NOISE_PREFIXES)

    def _is_section_line(self, line: str) -> bool:
        lowered = line.lower()
        return any(lowered.startswith(prefix) for prefix in SECTION_PREFIXES)

    def _is_role_candidate(self, line: str) -> bool:
        stripped = self._strip_bullet(line)
        lowered = stripped.lower()
        if not stripped or self._is_section_line(stripped) or self._is_contact_line(stripped):
            return False
        return line[0] in ROLE_BULLET_CHARS or any(keyword in lowered for keyword in ("menejer", "sotuvchi", "administrator", "tikuvchi", "oshpaz", "operator", "kassir", "yuk", "agent", "marketolog", "barista", "ofitsiant", "qorovul", "воспитатель", "учитель", "продав", "оператор"))

    def _is_contact_line(self, line: str) -> bool:
        lowered = line.lower()
        return lowered.startswith(("tel", "telefon", "aloqa", "murojaat", "rezyume", "тел", "контакт"))

    def _normalize_line(self, line: str) -> str:
        compact = re.sub(r"\s+", " ", line.strip())
        return compact.strip()

    def _strip_bullet(self, line: str) -> str:
        return ROLE_BULLET_PATTERN.sub("", line).strip()

    def _compact_title(self, value: str) -> str:
        cleaned = self._strip_bullet(value)
        cleaned = cleaned.strip("-:;,. ")
        cleaned = re.sub(r"\s{2,}", " ", cleaned)
        return cleaned

    def _cleanup_title(self, value: str) -> str:
        cleaned = self._compact_title(value)
        cleaned = re.sub(r"^(?:va\s+)?(?:[\wʼʻ‘’\"'().,-]+\s+filiallariga\s+)+", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"^(?:quyidagi\s+lavozim(?:lar)?(?:ga|bo'yicha)?\s*)", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"\blavozimiga\b", "", cleaned, flags=re.IGNORECASE)
        for prefix in HEADING_TITLE_PREFIXES:
            cleaned = re.sub(rf"^{re.escape(prefix)}\s+", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"\s{2,}", " ", cleaned)
        return cleaned.strip(" -:;,.")

    def _join_roles(self, roles: list[str]) -> str | None:
        unique_roles = [role for role in dict.fromkeys(roles) if role]
        if not unique_roles:
            return None
        if len(unique_roles) <= 4:
            return " / ".join(unique_roles)
        visible = " / ".join(unique_roles[:4])
        return f"{visible} +{len(unique_roles) - 4}"

    def _cleanup_company(self, company: str) -> str | None:
        normalized = self._compact_title(company)
        normalized = re.sub(r"\b(?:ga|ka|qa)$", "", normalized, flags=re.IGNORECASE).strip()
        normalized = re.sub(r"\b(?:mchj|xususiy\s+maktab(?:ning)?|o['’‘`]?quv\s+markaz(?:i|ining))\b", "", normalized, flags=re.IGNORECASE)
        normalized = re.sub(r"\s{2,}", " ", normalized).strip(" -:;,.")
        return normalized or None

    def _remove_invitation_suffix(self, heading: str) -> str:
        cleaned = re.sub(r"ishga\s+taklif\s+qilin(?:adi|adi!|adi\.|moqda|gan).*", "", heading, flags=re.IGNORECASE)
        cleaned = re.sub(r"\b(?:kerak|talab\s+etiladi)\b.*", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"приглашает\s+на\s+работу.*", "", cleaned, flags=re.IGNORECASE)
        return cleaned.strip()

    def _extract_heading_location(self, heading: str, company: str | None) -> str | None:
        if not heading:
            return None

        match = HEADING_LOCATION_PATTERN.search(heading)
        if match:
            value = match.group("value")
            if company:
                value = value.replace(company, "").strip()
            value = value.strip("\"'“«»” ")
            value = re.sub(r"^(?:o['’‘`]?quv\s+markazining|xususiy\s+maktabning|mchj)\s+", "", value, flags=re.IGNORECASE)
            value = re.sub(r"\s{2,}", " ", value).strip(" -:;,.")
            if value:
                return value

        match = HEADING_LOCATION_RU_PATTERN.search(heading)
        if match:
            value = match.group("value")
            value = re.sub(r"\s{2,}", " ", value).strip(" -:;,.")
            if value:
                return value

        return None
