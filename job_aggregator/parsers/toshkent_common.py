from __future__ import annotations

import re
from typing import TYPE_CHECKING

from job_aggregator.parsers.xorazm_ish_bor_elonlar.parser import (
    COMPANY_ONLY_INVITATION_PATTERN,
    COMPANY_ROLE_PATTERN,
    EXPLICIT_TITLE_PATTERNS,
    GENERIC_ROLE_PATTERN,
    INLINE_ADDRESS_PATTERNS,
    INLINE_COMPANY_PATTERNS,
    INLINE_LANDMARK_PATTERNS,
    INLINE_REGION_PATTERNS,
    INLINE_SALARY_PATTERNS,
    INVITATION_ONLY_PATTERN,
    PROMO_COMPANY_PATTERN,
    QUOTED_COMPANY_PATTERN,
    Parser as BaseStructuredParser,
)

if TYPE_CHECKING:
    from telethon.tl.custom.message import Message


APOSTROPHE_TRANSLATION = str.maketrans(
    {
        "‘": "'",
        "’": "'",
        "ʻ": "'",
        "ʼ": "'",
        "`": "'",
    }
)
COMPACT_ROLE_SPLIT_PATTERN = re.compile(r"\s*[📍🔹🔸▪▫•]+\s*")
DIRECT_COMPANY_INVITATION_PATTERN = re.compile(
    r"^(?P<company>.+?(?:kompaniyasi|kompaniya|firmasi|korxonasi|zavodi|markazi|tsexi|sexi|skladi|ombori|restorani|kafesi|kafe|hotel|trading|logistics|logistic|taxi|market|mchj))\s*(?P<title>.+?)?\s*(?:ishga\s+|ишга\s+)?(?:taklif\s+qil(?:adi|amiz)|taklif\s+et(?:adi|amiz)|qabul\s+qil(?:adi|amiz|inadi)|qabul\s+qilin(?:adi|amiz))",
    re.IGNORECASE,
)
COMMON_COMPANY_MARKERS = (
    "trading",
    "logistics",
    "logistic",
    "tsex",
    "sex",
    "sklad",
    "ombor",
    "zavod",
    "fabrika",
    "factory",
    "ishlab chiqarish",
    "savdo markazi",
    "savdo uyi",
    "distrib",
    "distribut",
    "taxi",
    "yandex",
    "clinic",
    "hotel",
)
COMPANY_SUFFIX_REPLACEMENTS: tuple[tuple[re.Pattern[str], str], ...] = (
    (re.compile(r"\bkompaniyasiga\b", re.IGNORECASE), "kompaniyasi"),
    (re.compile(r"\bkompaniyaga\b", re.IGNORECASE), "kompaniya"),
    (re.compile(r"\bfirmasiga\b", re.IGNORECASE), "firmasi"),
    (re.compile(r"\bfirmaga\b", re.IGNORECASE), "firma"),
    (re.compile(r"\bkorxonasiga\b", re.IGNORECASE), "korxonasi"),
    (re.compile(r"\bkorxonaga\b", re.IGNORECASE), "korxona"),
    (re.compile(r"\bmarkaziga\b", re.IGNORECASE), "markazi"),
    (re.compile(r"\bmarkazga\b", re.IGNORECASE), "markaz"),
    (re.compile(r"\bfilialiga\b", re.IGNORECASE), "filiali"),
    (re.compile(r"\bfiliallariga\b", re.IGNORECASE), "filiallari"),
    (re.compile(r"\bbo'limiga\b", re.IGNORECASE), "bo'limi"),
    (re.compile(r"\btsexiga\b", re.IGNORECASE), "tsexi"),
    (re.compile(r"\bsexiga\b", re.IGNORECASE), "sexi"),
    (re.compile(r"\bzavodiga\b", re.IGNORECASE), "zavodi"),
    (re.compile(r"\bskladiga\b", re.IGNORECASE), "skladi"),
    (re.compile(r"\bofisiga\b", re.IGNORECASE), "ofisi"),
    (re.compile(r"\boffisiga\b", re.IGNORECASE), "offisi"),
    (re.compile(r"\brestoraniga\b", re.IGNORECASE), "restorani"),
    (re.compile(r"\bkafega\b", re.IGNORECASE), "kafe"),
    (re.compile(r"\bog'chasiga\b", re.IGNORECASE), "bog'chasi"),
)


class BaseToshkentParser(BaseStructuredParser):
    extra_non_job_phrases: tuple[str, ...] = ()
    extra_company_markers: tuple[str, ...] = COMMON_COMPANY_MARKERS
    extra_salary_patterns: tuple[re.Pattern[str], ...] = ()
    extra_address_patterns: tuple[re.Pattern[str], ...] = ()

    def get_message_contact_links(self, message: "Message") -> list[str]:
        links = super().get_message_contact_links(message)
        if not links:
            return links

        text = self.get_message_text(message)
        return [link for link in links if not self._drop_contact_link(link, text)]

    def _drop_contact_link(self, link: str, text: str) -> bool:
        return False

    def _cleanup_text(self, text: str) -> str:
        return super()._cleanup_text(text.translate(APOSTROPHE_TRANSLATION))

    def _normalize_line(self, line: str) -> str:
        return super()._normalize_line(line.translate(APOSTROPHE_TRANSLATION))

    def _has_blocked_signal(self, lines: list[str], text: str) -> bool:
        lowered = text.lower()
        if any(phrase in lowered for phrase in self.extra_non_job_phrases):
            return True
        return super()._has_blocked_signal(lines, text)

    def _extract_role_lines(self, lines: list[str], heading_index: int) -> list[str]:
        roles = super()._extract_role_lines(lines, heading_index)
        expanded = self._expand_compact_roles(roles)
        if expanded:
            return expanded

        explicit_marker_index = self._find_explicit_multi_marker(lines)
        if explicit_marker_index is None:
            return roles

        compact_roles: list[str] = []
        for line in lines[explicit_marker_index + 1 : explicit_marker_index + 4]:
            compact_roles.extend(self._split_compact_roles(line))

        return [role for role in dict.fromkeys(compact_roles) if role] or roles

    def _extract_company(self, lines: list[str], heading: str) -> str | None:
        inline_company = self._extract_inline_company(lines)
        if inline_company:
            return inline_company

        candidates = self._leading_context_lines(lines, heading)
        candidates.extend(line for line in lines[:8] if line not in candidates and not self._is_noise_line(line))

        for candidate in candidates:
            if self._starts_labeled_field(candidate):
                continue

            quoted = QUOTED_COMPANY_PATTERN.search(candidate)
            if quoted:
                company = self._cleanup_company(quoted.group("company"))
                if company:
                    return company

            promo_match = PROMO_COMPANY_PATTERN.search(candidate)
            if promo_match:
                company = self._cleanup_company(promo_match.group("company"))
                if company:
                    return company

            direct_match = DIRECT_COMPANY_INVITATION_PATTERN.search(candidate)
            if direct_match:
                company = self._normalize_company_from_candidate(candidate, direct_match.group("company"))
                title = self._cleanup_title(direct_match.group("title") or "")
                if company and self._is_reliable_company_candidate(company, title):
                    return company

            match = COMPANY_ROLE_PATTERN.search(candidate)
            if match:
                company = self._normalize_company_from_candidate(candidate, match.group("company"))
                title = self._cleanup_title(match.group("title"))
                if company and self._is_reliable_company_candidate(company, title):
                    return company

            company_only_match = COMPANY_ONLY_INVITATION_PATTERN.search(candidate)
            if company_only_match:
                company = self._normalize_company_from_candidate(candidate, company_only_match.group("company"))
                if company and self._is_reliable_company_candidate(company):
                    return company

            context_company = self._extract_heading_company(candidate)
            if context_company and self._is_reliable_company_candidate(context_company):
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

            company_match = COMPANY_ROLE_PATTERN.search(candidate)
            if company_match:
                extracted_title = self._cleanup_title(company_match.group("title"))
                if extracted_title and not self._is_unreliable_extracted_title(extracted_title):
                    return extracted_title
                return self._cleanup_heading(candidate)

            generic_match = GENERIC_ROLE_PATTERN.search(candidate)
            if generic_match:
                extracted_title = self._cleanup_title(generic_match.group("title"))
                if extracted_title and not self._is_unreliable_extracted_title(extracted_title):
                    return extracted_title
                return self._cleanup_heading(candidate)

        if self._is_heading_noise(heading):
            return None

        return self._cleanup_title(heading) or None

    def _extract_location(self, lines: list[str]) -> str | None:
        address_patterns = [*self.extra_address_patterns, *INLINE_ADDRESS_PATTERNS]
        location = self._extract_multiline_location(lines, address_patterns)
        if not location:
            location = self._extract_multiline_location(lines, INLINE_REGION_PATTERNS)
        if not location:
            location = self._extract_filial_location(lines)
        landmark = self._extract_multiline_location(lines, INLINE_LANDMARK_PATTERNS)

        if location and self._looks_like_company(location) and not self._looks_like_location(location):
            location = None

        if location and landmark:
            return f"{location} ({landmark})"
        if location or landmark:
            return location or landmark
        return None

    def _extract_salary(self, lines: list[str]) -> str | None:
        salary_patterns = [*self.extra_salary_patterns, *INLINE_SALARY_PATTERNS]
        return self._extract_labeled_field(lines, salary_patterns, allow_multiline=True)

    def _looks_like_company(self, value: str) -> bool:
        lowered = value.lower()
        return super()._looks_like_company(value) or any(marker in lowered for marker in self.extra_company_markers)

    def _cleanup_company(self, value: str) -> str | None:
        lowered_original = value.lower()
        cleaned = self._cleanup_heading(value)
        for pattern, replacement in COMPANY_SUFFIX_REPLACEMENTS:
            cleaned = pattern.sub(replacement, cleaned)
        if "kompaniyasiga" in lowered_original and cleaned.lower().endswith("kompaniya"):
            cleaned = f"{cleaned}si"
        if "firmasiga" in lowered_original and cleaned.lower().endswith("firma"):
            cleaned = f"{cleaned}si"
        if "korxonasiga" in lowered_original and cleaned.lower().endswith("korxona"):
            cleaned = f"{cleaned}si"
        if "markaziga" in lowered_original and cleaned.lower().endswith("markaz"):
            cleaned = f"{cleaned}i"
        if "zavodiga" in lowered_original and cleaned.lower().endswith("zavod"):
            cleaned = f"{cleaned}i"
        if "tsexiga" in lowered_original and cleaned.lower().endswith("tsex"):
            cleaned = f"{cleaned}i"
        if "sexiga" in lowered_original and cleaned.lower().endswith("sex"):
            cleaned = f"{cleaned}i"
        if "skladiga" in lowered_original and cleaned.lower().endswith("sklad"):
            cleaned = f"{cleaned}i"
        if "ofisiga" in lowered_original and cleaned.lower().endswith("ofis"):
            cleaned = f"{cleaned}i"
        cleaned = re.sub(r"(?:ga|ka|qa|iga|lariga|siga|га|ка|қа|ига|ларга|сига)$", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(r"\s{2,}", " ", cleaned)
        return cleaned.strip(" -:;,.") or None

    def _extract_explicit_title(self, lines: list[str]) -> str | None:
        patterns = list(EXPLICIT_TITLE_PATTERNS) + [
            re.compile(
                r"^(?:[\W_]+\s*)?(?:bo['’`]?sh\s+ish\s+o['’`]?rni|rol|position)\s*[:\-]?\s*(?P<value>.*)$",
                re.IGNORECASE,
            )
        ]
        for index, line in enumerate(lines):
            for pattern in patterns:
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

    def _is_reliable_company_candidate(self, company: str, title: str | None = None) -> bool:
        if not company or self._is_unreliable_company_candidate(company):
            return False
        if self._looks_like_company(company):
            return True
        if "," in company or len(company) > 60:
            return False
        if title and company.lower().startswith(title.lower()):
            return False

        word_count = len(company.split())
        if word_count >= 3 and title and company.lower() != title.lower():
            return True

        return False

    def _expand_compact_roles(self, roles: list[str]) -> list[str]:
        expanded: list[str] = []
        for role in roles:
            parts = self._split_compact_roles(role)
            if parts:
                expanded.extend(parts)
        return [role for role in dict.fromkeys(expanded) if role]

    def _split_compact_roles(self, value: str) -> list[str]:
        if len(COMPACT_ROLE_SPLIT_PATTERN.findall(value)) < 2:
            cleaned = self._cleanup_title(value)
            return [cleaned] if cleaned else []

        parts = [self._cleanup_title(part) for part in COMPACT_ROLE_SPLIT_PATTERN.split(value) if part.strip()]
        return [part for part in parts if part]

    def _extract_multiline_location(
        self,
        lines: list[str],
        patterns: list[re.Pattern[str]],
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

                next_index = index + 1
                while next_index < len(lines):
                    candidate = lines[next_index]
                    if self._is_noise_line(candidate):
                        next_index += 1
                        continue
                    if self._starts_labeled_field(candidate) or self._is_section_line(candidate) or self._is_contact_line(candidate):
                        break

                    cleaned = self._cleanup_field_value(candidate)
                    if not cleaned:
                        next_index += 1
                        continue
                    lowered = cleaned.lower()
                    if values and ("tel" in lowered or "telegram" in lowered or "murojaat" in lowered or "aloqa" in lowered):
                        break

                    values.append(cleaned)
                    next_index += 1

                if values:
                    return "; ".join(dict.fromkeys(values)).strip(" -:;,.")
        return None

    def _starts_labeled_field(self, line: str) -> bool:
        patterns = (
            list(self.extra_address_patterns)
            + list(self.extra_salary_patterns)
            + INLINE_COMPANY_PATTERNS
            + INLINE_ADDRESS_PATTERNS
            + INLINE_REGION_PATTERNS
            + INLINE_LANDMARK_PATTERNS
            + INLINE_SALARY_PATTERNS
            + EXPLICIT_TITLE_PATTERNS
        )
        return any(pattern.search(line) for pattern in patterns)

    def _strip_leading_symbols(self, value: str) -> str:
        cleaned = value.translate(APOSTROPHE_TRANSLATION)
        cleaned = re.sub(r"^[\s\W_]+", "", cleaned)
        return super()._strip_leading_symbols(cleaned)

    def _normalize_company_from_candidate(self, candidate: str, fragment: str) -> str | None:
        company = self._cleanup_company(fragment)
        if not company:
            return None

        lowered_candidate = candidate.lower()
        lowered_company = company.lower()

        if "kompaniyasiga" in lowered_candidate and lowered_company.endswith("kompaniya"):
            return f"{company}si"
        if "firmasiga" in lowered_candidate and lowered_company.endswith("firma"):
            return f"{company}si"
        if "korxonasiga" in lowered_candidate and lowered_company.endswith("korxona"):
            return f"{company}si"
        if "markaziga" in lowered_candidate and lowered_company.endswith("markaz"):
            return f"{company}i"
        if "zavodiga" in lowered_candidate and lowered_company.endswith("zavod"):
            return f"{company}i"
        if "tsexiga" in lowered_candidate and lowered_company.endswith("tsex"):
            return f"{company}i"

        return company
