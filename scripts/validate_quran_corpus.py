"""Fail-closed validation for the bundled Quran and word-by-word corpus."""

from __future__ import annotations

import argparse
import json
import re
import sqlite3
import unicodedata
from collections import Counter
from pathlib import Path


EXPECTED_AYAHS = 6236
EXPECTED_WORDS = 77430
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


def parse_markup(value: str) -> tuple[str, set[str], bool, Counter[str]]:
    plain: list[str] = []
    tags: set[str] = set()
    counts: Counter[str] = Counter()
    open_tags: list[str] = []
    cursor = 0
    malformed = False
    while cursor < len(value):
        match = TAG_START.search(value, cursor)
        if match is not None and match.start() == cursor:
            tag = match.group(1).lower()
            open_tags.append(tag)
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
                open_tags.pop()
            cursor += 1
        else:
            plain.append(normalize_source_content(value[cursor]))
            cursor += 1
    if open_tags:
        malformed = True
    return "".join(plain), tags, malformed, counts


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
    ayah_keys = {(row["surah_id"], row["ayah_number"]) for row in ayahs}

    for row in ayahs:
        if not row["text_arabic"] or not row["tajwid_tags"]:
            issues.append({"type": "empty_ayah_field", "surah": row["surah_id"], "ayah": row["ayah_number"]})
            continue
        parsed, tags, malformed, counts = parse_markup(row["tajwid_tags"])
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
        and sequence_issues == 0
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
        "waqaf_marker_counts": dict(sorted(waqaf_counts.items())),
        "ayah_end_marker_source_count": sum("۝" in row["text_arabic"] for row in ayahs),
        "ayah_end_marker_rendered_count": len(ayahs),
        "alignment_failures": alignment_failures,
        "sequence_issues": sequence_issues,
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
