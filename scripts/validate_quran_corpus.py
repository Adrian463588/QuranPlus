"""Fail-closed validation for the bundled Quran and word-by-word corpus."""

from __future__ import annotations

import argparse
import json
import re
import sqlite3
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path


EXPECTED_AYAHS = 6236
EXPECTED_WORDS = 77429
KNOWN_TAGS = set("hsl npmqocfwi audbg".replace(" ", ""))
WAQAF_MARKERS = {
    "ۘ": "م",
    "ۙ": "لا",
    "ۚ": "ج",
    "ۖ": "صلى",
    "ۗ": "قلى",
    "ۛ": "muanaqah",
    "ۜ": "saktah",
}
TAG_START = re.compile(r"\[([a-zA-Z])(?::([0-9]+))?\[")


def normalize_source_content(value: str) -> str:
    return (
        value.replace("ـ", "")
        .replace("\u200c", "")
        .replace("\u0672", "\u0670")
        .replace("\u066e", "\u0649")
        .replace("\u06e7", "\u06e6")
    )


def normalize_for_alignment(value: str) -> str:
    value = normalize_source_content(value)
    value = value.replace("أ", "ءا").replace("ئ", "ى").replace("ؤ", "و")
    return "".join(
        char
        for char in value
        if unicodedata.category(char)[0] != "M"
        and char not in {" ", "ى", "۝", "ۘ", "ۙ", "ۚ", "ۖ", "ۗ", "ۛ", "ۜ"}
    )


def remove_known_bismillah_prefix(value: str) -> str:
    prefix = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ "
    return value[len(prefix) :] if value.startswith(prefix) else value


def parse_markup(
    value: str,
) -> tuple[str, set[str], bool, Counter[str], list[tuple[str, int, int]]]:
    plain: list[str] = []
    tags: set[str] = set()
    counts: Counter[str] = Counter()
    open_tags: list[tuple[str, int]] = []
    spans: list[tuple[str, int, int]] = []
    cursor = 0
    malformed = False
    while cursor < len(value):
        match = TAG_START.search(value, cursor)
        if match is not None and match.start() == cursor:
            tag = match.group(1).lower()
            open_tags.append((tag, len("".join(plain))))
            tags.add(tag)
            counts[tag] += 1
            cursor = match.end()
        elif value[cursor] == "[" and value.find("]", cursor + 1) > cursor:
            literal_end = value.find("]", cursor + 1)
            literal = value[cursor + 1 : literal_end]
            if literal == "ٮٰ":
                plain.append(normalize_source_content(literal))
                cursor = literal_end + 1
            else:
                malformed = True
                plain.append(value[cursor])
                cursor += 1
        elif value[cursor] == "]":
            if not open_tags:
                malformed = True
            else:
                tag, start = open_tags.pop()
                spans.append((tag, start, len("".join(plain))))
            cursor += 1
        else:
            plain.append(normalize_source_content(value[cursor]))
            cursor += 1
    if open_tags:
        malformed = True
    return "".join(plain), tags, malformed, counts, spans


def is_alignment_gap(value: str) -> bool:
    return all(
        char.isspace()
        or char in {"ـ", "\u200c", "۞", "۝", "۩", "ۘ", "ۙ", "ۚ", "ۖ", "ۗ", "ۛ", "ۜ"}
        or "٠" <= char <= "٩"
        or "0" <= char <= "9"
        for char in value
    )


WORD_MARKERS = set("ۘۙۚۖۗۛۜ")


def normalize_word_char(char: str) -> str:
    if char in {"ـ", "۞", "۝", "۩"} or char in WORD_MARKERS:
        return ""
    if unicodedata.category(char)[0] == "M":
        return ""
    return {"ٱ": "ا", "أ": "ا", "إ": "ا", "آ": "ا", "ئ": "ى", "ؤ": "و"}.get(char, char)


def normalize_word(value: str) -> str:
    return "".join(normalize_word_char(char) for char in value)


def is_word_character(char: str) -> bool:
    return (
        not char.isspace()
        and char not in {"\u200c", "\u06e9", "۞", "۝", "۩"}
        and char not in WORD_MARKERS
        and not ("٠" <= char <= "٩" or "0" <= char <= "9")
    )


def adjacent_word_character(value: str, index: int, direction: int) -> str | None:
    cursor = index + direction
    while 0 <= cursor < len(value):
        char = value[cursor]
        if unicodedata.category(char)[0] == "M" or char == "ـ":
            cursor += direction
            continue
        return char if is_word_character(char) else None
    return None


def is_word_separator_at(value: str, index: int) -> bool:
    char = value[index]
    if char in WORD_MARKERS:
        return adjacent_word_character(value, index, -1) is None or adjacent_word_character(value, index, 1) is None
    return (
        char.isspace()
        or char in {"\u200c", "\u06e9", "۞", "۝", "۩"}
        or "٠" <= char <= "٩"
        or "0" <= char <= "9"
    )


def skip_known_bismillah_prefix(value: str) -> int:
    prefix = "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    return len(prefix) if value.startswith(prefix) and (
        len(value) == len(prefix) or value[len(prefix)].isspace()
    ) else 0


def align_word_rows(display_text: str, words: list[str]) -> bool:
    if not display_text or not words:
        return False
    cursor = skip_known_bismillah_prefix(display_text)
    for source_word in words:
        source_parts = source_word.split()
        if not source_parts:
            return False
        for source_part in source_parts:
            while cursor < len(display_text) and is_word_separator_at(display_text, cursor):
                cursor += 1
            candidate_end = cursor
            while candidate_end < len(display_text) and not is_word_separator_at(display_text, candidate_end):
                candidate_end += 1
            if normalize_word(display_text[cursor:candidate_end]) != normalize_word(source_part):
                return False
            cursor = candidate_end
    return is_alignment_gap(display_text[cursor:])


def utf16_span_is_safe(value: str, start: int, end: int) -> bool:
    if not 0 <= start < end <= len(value):
        return False
    utf16_start = len(value[:start].encode("utf-16-le")) // 2
    utf16_end = len(value[:end].encode("utf-16-le")) // 2
    utf16_length = len(value.encode("utf-16-le")) // 2
    return 0 <= utf16_start < utf16_end <= utf16_length


def validate(database_path: Path) -> dict[str, object]:
    connection = sqlite3.connect(database_path)
    connection.row_factory = sqlite3.Row
    try:
        ayahs = connection.execute(
            "SELECT surah_id, ayah_number, text_arabic, tajwid_tags "
            "FROM ayahs ORDER BY surah_id, ayah_number"
        ).fetchall()
        surah_counts = dict(
            connection.execute("SELECT number, ayah_count FROM surahs ORDER BY number").fetchall()
        )
        word_count, word_ayah_count, word_english_count, word_indonesian_count = connection.execute(
            "SELECT COUNT(*), COUNT(DISTINCT surah_id || '-' || ayah_number), "
            "SUM(length(translation_en) > 0), SUM(length(translation_id) > 0) "
            "FROM word_by_word"
        ).fetchone()
        word_sequences = connection.execute(
            "SELECT surah_id, ayah_number, word_index, text_arabic "
            "FROM word_by_word ORDER BY surah_id, ayah_number, word_index"
        ).fetchall()
    finally:
        connection.close()

    issues: list[dict[str, object]] = []
    tag_counts: Counter[str] = Counter()
    waqaf_counts: Counter[str] = Counter()
    malformed_count = 0
    alignment_failures = 0
    utf16_span_failures = 0
    ayah_keys = {(row["surah_id"], row["ayah_number"]) for row in ayahs}
    word_rows_by_ayah: defaultdict[tuple[int, int], list[sqlite3.Row]] = defaultdict(list)
    for word in word_sequences:
        word_rows_by_ayah[(word["surah_id"], word["ayah_number"])].append(word)
    tajwid_source_ayah_count = 0

    for row in ayahs:
        if not row["text_arabic"] or not row["tajwid_tags"]:
            issues.append({"type": "empty_ayah_field", "surah": row["surah_id"], "ayah": row["ayah_number"]})
            continue
        parsed, tags, malformed, counts, spans = parse_markup(row["tajwid_tags"])
        if tags:
            tajwid_source_ayah_count += 1
        tag_counts.update(counts)
        unknown = sorted(tags - KNOWN_TAGS)
        if malformed or unknown:
            malformed_count += int(malformed)
            issues.append({
                "type": "invalid_tajwid_markup",
                "surah": row["surah_id"],
                "ayah": row["ayah_number"],
                "unknown_tags": unknown,
                "malformed": malformed,
            })
        for _, start, end in spans:
            if not utf16_span_is_safe(parsed, start, end):
                utf16_span_failures += 1
        display_text = remove_known_bismillah_prefix(row["text_arabic"])
        if normalize_for_alignment(parsed) != normalize_for_alignment(display_text):
            alignment_failures += 1
            if len([item for item in issues if item["type"] == "tajwid_alignment"]) < 10:
                issues.append({
                    "type": "tajwid_alignment",
                    "surah": row["surah_id"],
                    "ayah": row["ayah_number"],
                    "parsed_length": len(parsed),
                    "display_length": len(display_text),
                })
        for char in row["text_arabic"]:
            if char in WAQAF_MARKERS:
                waqaf_counts[WAQAF_MARKERS[char]] += 1

    sequence_issues = 0
    current_key: tuple[int, int] | None = None
    expected_index = 1
    for row in word_sequences:
        key = (row["surah_id"], row["ayah_number"])
        if key != current_key:
            current_key = key
            expected_index = 1
        if key not in ayah_keys or row["word_index"] != expected_index or not row["text_arabic"]:
            sequence_issues += 1
            if sequence_issues <= 10:
                issues.append({"type": "word_alignment", "surah": row["surah_id"], "ayah": row["ayah_number"], "word_index": row["word_index"], "expected_index": expected_index})
        expected_index += 1

    word_alignment_failures = 0
    for row in ayahs:
        key = (row["surah_id"], row["ayah_number"])
        words = word_rows_by_ayah.get(key, [])
        if not align_word_rows(row["text_arabic"], [word["text_arabic"] for word in words]):
            word_alignment_failures += 1
            if word_alignment_failures <= 10:
                issues.append({
                    "type": "word_text_alignment",
                    "surah": row["surah_id"],
                    "ayah": row["ayah_number"],
                })

    expected_ayah_count = sum(surah_counts.values())
    valid = (
        len(surah_counts) == 114
        and expected_ayah_count == EXPECTED_AYAHS
        and len(ayahs) == EXPECTED_AYAHS
        and len(ayah_keys) == EXPECTED_AYAHS
        and word_count == EXPECTED_WORDS
        and word_ayah_count == EXPECTED_AYAHS
        and word_english_count == EXPECTED_WORDS
        and malformed_count == 0
        and alignment_failures == 0
        and utf16_span_failures == 0
        and sequence_issues == 0
        and word_alignment_failures == 0
    )
    return {
        "status": "valid" if valid else "blocked",
        "ayah_count": len(ayahs),
        "surah_count": len(surah_counts),
        "expected_ayah_count": expected_ayah_count,
        "word_count": word_count,
        "word_ayah_count": word_ayah_count,
        "word_english_count": word_english_count,
        "word_indonesian_count": word_indonesian_count,
        "tajwid_tag_counts": dict(sorted(tag_counts.items())),
        "tajwid_source_ayah_count": tajwid_source_ayah_count,
        "ayahs_without_tajwid_source": len(ayahs) - tajwid_source_ayah_count,
        "waqaf_marker_counts": dict(sorted(waqaf_counts.items())),
        "ayah_end_marker_source_count": sum("۝" in row["text_arabic"] for row in ayahs),
        "ayah_end_marker_rendered_count": len(ayahs),
        "alignment_failures": alignment_failures,
        "utf16_span_failures": utf16_span_failures,
        "sequence_issues": sequence_issues,
        "word_alignment_failures": word_alignment_failures,
        "issues": issues[:50],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("database", type=Path, nargs="?", default=Path("app/src/main/assets/databases/quranplus.db"))
    args = parser.parse_args()
    result = validate(args.database.resolve())
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if result["status"] != "valid":
        raise SystemExit(2)


if __name__ == "__main__":
    main()
