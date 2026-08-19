"""Import reviewed word-by-word Quran data into the app asset.

The optional API source supplies real per-word transliteration and English
meaning. Every API position must align to the reviewed Arabic source before
the asset is replaced.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import time
import unicodedata
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from pathlib import Path

EXPECTED_WORDS = 77_430
EXPECTED_API_WORDS = 77_429
EXPECTED_AYAHS = 6_236
SOURCE_REVISION = "wordbyword.db:source-gated-v4"
API_SOURCE_REVISION = "islamic.app:v1-words"


def normalize_english(value: str | None) -> str:
    """Remove the source DB's serialized quote escapes, not content."""
    return (value or "").replace('\\"', '"')


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def normalize_arabic(value: str) -> str:
    """Compare source words without treating Quranic marks as letters."""
    value = value.replace("ىٰ", "ا").replace("ـٰ", "ا").replace("ٰ", "ا")
    normalized = unicodedata.normalize("NFKD", value or "")
    return "".join(
        {
            "ٱ": "ا",
            "أ": "ا",
            "إ": "ا",
            "آ": "ا",
            "ى": "ا",
            "ؤ": "و",
            "ئ": "ي",
            "ء": "",
        }.get(char, char)
        for char in normalized
        if unicodedata.category(char)[0] != "M"
        and unicodedata.category(char) != "Cf"
        and char not in {
            "ـ", "۞", "۩", "ۘ", "ۙ", "ۚ", "ۖ", "ۗ", "ۛ", "ۜ", "ۦ", "ۧ"
        }
        and not char.isspace()
    )


def fetch_json(url: str) -> dict[str, object]:
    request = Request(
        url,
        headers={"Accept": "application/json", "User-Agent": "QuranPlus-data-import/1.0"},
    )
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            with urlopen(request, timeout=30) as response:
                return json.load(response)
        except (HTTPError, URLError, TimeoutError) as error:
            last_error = error
            if attempt < 2:
                time.sleep(2**attempt)
    raise RuntimeError(f"Word API request failed: {url}") from last_error


def fetch_api_rows(base_url: str, cache_path: Path | None = None) -> list[dict[str, object]]:
    """Fetch the documented word source, keeping one row per ayah position."""
    cache: dict[str, dict[str, object]] = {}
    if cache_path and cache_path.is_file():
        cache = json.loads(cache_path.read_text(encoding="utf-8"))

    def fetch_page(surah_id: int, page: int) -> dict[str, object]:
        key = f"{surah_id}:{page}"
        if key in cache:
            return cache[key]
        payload = fetch_json(
            f"{base_url.rstrip('/')}/{surah_id}?{urlencode({'page': page, 'per_page': 50})}"
        )
        cache[key] = payload
        if cache_path:
            cache_path.write_text(
                json.dumps(cache, ensure_ascii=False, separators=(",", ":")),
                encoding="utf-8",
            )
        return payload

    rows: list[dict[str, object]] = []
    for surah_id in range(1, 115):
        first_payload = fetch_page(surah_id, 1)
        data = first_payload.get("data")
        if not isinstance(data, dict):
            raise RuntimeError(f"Word API returned no data for surah {surah_id}")
        pagination = data.get("pagination")
        if not isinstance(pagination, dict):
            raise RuntimeError(f"Word API returned no pagination for surah {surah_id}")
        total_pages = int(pagination.get("total_pages", 0))
        if total_pages < 1:
            raise RuntimeError(f"Word API returned no pages for surah {surah_id}")
        for page in range(1, total_pages + 1):
            payload = first_payload if page == 1 else fetch_page(surah_id, page)
            page_data = payload.get("data")
            ayahs = page_data.get("ayahs") if isinstance(page_data, dict) else None
            if not isinstance(ayahs, list):
                raise RuntimeError(f"Word API returned no ayahs for {surah_id}, page {page}")
            for ayah in ayahs:
                if not isinstance(ayah, dict):
                    raise RuntimeError(f"Malformed ayah for {surah_id}, page {page}")
                ayah_number = int(ayah["ayah_number"])
                words = ayah.get("words")
                if not isinstance(words, list):
                    raise RuntimeError(f"Malformed words for {surah_id}:{ayah_number}")
                for word in words:
                    if not isinstance(word, dict):
                        raise RuntimeError(f"Malformed word for {surah_id}:{ayah_number}")
                    transliteration = str(word.get("transliteration") or "").strip()
                    translation = normalize_english(str(word.get("translation") or "").strip())
                    arabic = str(word.get("text_uthmani") or "").strip()
                    if not arabic or not transliteration or not translation:
                        raise RuntimeError(
                            f"Incomplete word source for {surah_id}:{ayah_number}:{word.get('position')}"
                        )
                    rows.append(
                        {
                            "surah_id": surah_id,
                            "ayah_number": ayah_number,
                            "word_index": int(word["position"]),
                            "text_arabic": arabic,
                            "transliteration": transliteration,
                            "translation_en": translation,
                        }
                    )
    return rows


def merge_source_with_api(
    source_rows: list[tuple[object, ...]],
    api_rows: list[dict[str, object]],
    include_indonesian: bool,
) -> list[tuple[object, ...]]:
    """Keep API word boundaries and consume only explicitly matching source words."""
    source_by_key = {
        (int(row[1]), int(row[2]), int(row[3])): row
        for row in source_rows
    }
    consumed: set[tuple[int, int, int]] = set()
    merged: list[tuple[object, ...]] = []
    for api_word in api_rows:
        surah_id = int(api_word["surah_id"])
        ayah_number = int(api_word["ayah_number"])
        word_index = int(api_word["word_index"])
        key = (surah_id, ayah_number, word_index)
        source_row = source_by_key.get(key)
        if source_row is None:
            raise RuntimeError(f"Word API has no reviewed source row at {key}")

        source_parts = [source_row]
        combined = str(source_row[4])
        target_arabic = str(api_word["text_arabic"])
        while normalize_arabic(combined) != normalize_arabic(target_arabic):
            if not normalize_arabic(target_arabic).startswith(normalize_arabic(combined)):
                raise RuntimeError(
                    f"Word API Arabic alignment failed at {key}: "
                    f"{combined!r} != {target_arabic!r}"
                )
            next_key = (surah_id, ayah_number, word_index + len(source_parts))
            next_row = source_by_key.get(next_key)
            if next_row is None:
                raise RuntimeError(
                    f"Word API composite word has no complete reviewed source at {key}"
                )
            source_parts.append(next_row)
            combined = f"{combined} {next_row[4]}"
        source_keys = {
            (int(row[1]), int(row[2]), int(row[3])) for row in source_parts
        }
        if consumed.intersection(source_keys):
            raise RuntimeError(f"Word API reuses a reviewed source row at {key}")
        consumed.update(source_keys)
        if include_indonesian and len(source_parts) != 1:
            raise RuntimeError(
                f"Indonesian translation cannot be aligned to composite word at {key}"
            )
        merged.append(
            (
                source_row[0],
                surah_id,
                ayah_number,
                word_index,
                combined,
                str(api_word["transliteration"]),
                str(api_word["translation_en"]),
                normalize_english(str(source_row[6])) if include_indonesian else "",
            )
        )

    all_source_keys = set(source_by_key)
    if consumed != all_source_keys:
        leftovers = sorted(all_source_keys - consumed)
        raise RuntimeError(
            "Reviewed source has words not represented by the API: "
            f"{leftovers[:3]} (count={len(leftovers)})"
        )
    return merged


def import_rows(
    source: Path,
    target: Path,
    include_indonesian: bool = False,
    indonesian_source: str = "",
    indonesian_license: str = "",
    api_base_url: str = "",
    api_cache: Path | None = None,
) -> tuple[int, int, str]:
    if include_indonesian and (not indonesian_source or indonesian_license != "verified"):
        raise RuntimeError(
            "Indonesian word translation requires a verified source URL and license status"
        )
    source_hash = sha256(source)
    source_db = sqlite3.connect(source)
    target_db = sqlite3.connect(target)
    try:
        rows = source_db.execute(
            """
            SELECT _id, surah_id, verse_id, words_id, words_ar,
                   translate_en, translate_indo
            FROM bywords
            ORDER BY surah_id, verse_id, words_id
            """
        ).fetchall()
        word_count = len(rows)
        ayah_count = source_db.execute(
            "SELECT COUNT(DISTINCT surah_id || ':' || verse_id) FROM bywords"
        ).fetchone()[0]
        if word_count != EXPECTED_WORDS or ayah_count != EXPECTED_AYAHS:
            raise RuntimeError(
                f"Unexpected word-by-word coverage: words={word_count}, ayahs={ayah_count}"
            )

        api_rows = fetch_api_rows(api_base_url, api_cache) if api_base_url else []
        if api_base_url and len(api_rows) != EXPECTED_API_WORDS:
            raise RuntimeError(
                f"Unexpected word API coverage: words={len(api_rows)}, "
                f"expected={EXPECTED_API_WORDS}"
            )
        if api_base_url:
            output_rows = merge_source_with_api(rows, api_rows, include_indonesian)
        else:
            output_rows = [
                (
                    row_id,
                    surah_id,
                    verse_id,
                    words_id,
                    arabic or "",
                    None,
                    normalize_english(english),
                    normalize_english(indonesian) if include_indonesian else "",
                )
                for row_id, surah_id, verse_id, words_id, arabic, english, indonesian in rows
            ]

        source_revision = API_SOURCE_REVISION if api_base_url else SOURCE_REVISION
        source_hash = (
            hashlib.sha256(
                json.dumps(api_rows, ensure_ascii=False, sort_keys=True).encode("utf-8")
            ).hexdigest()
            if api_base_url
            else sha256(source)
        )

        target_db.execute(
            """
            CREATE TABLE IF NOT EXISTS word_by_word (
                id INTEGER NOT NULL PRIMARY KEY,
                surah_id INTEGER NOT NULL,
                ayah_number INTEGER NOT NULL,
                word_index INTEGER NOT NULL,
                text_arabic TEXT NOT NULL,
                transliteration TEXT,
                translation_en TEXT NOT NULL,
                translation_id TEXT NOT NULL,
                source_revision TEXT NOT NULL,
                source_sha256 TEXT NOT NULL
            )
            """
        )
        target_columns = {
            row[1] for row in target_db.execute("PRAGMA table_info(word_by_word)")
        }
        if "transliteration" not in target_columns:
            target_db.execute(
                "ALTER TABLE word_by_word ADD COLUMN transliteration TEXT"
            )
        target_db.execute("DELETE FROM word_by_word")
        target_db.executemany(
            """
            INSERT INTO word_by_word(
                id, surah_id, ayah_number, word_index, text_arabic, transliteration,
                translation_en, translation_id, source_revision, source_sha256
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            [row + (source_revision, source_hash) for row in output_rows],
        )
        target_db.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS "
            "index_word_by_word_surah_id_ayah_number_word_index "
            "ON word_by_word(surah_id, ayah_number, word_index)"
        )
        target_db.execute(
            "CREATE INDEX IF NOT EXISTS idx_word_by_word_ayah "
            "ON word_by_word(surah_id, ayah_number)"
        )
        target_db.commit()
        return len(output_rows), ayah_count, source_hash
    finally:
        source_db.close()
        target_db.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--include-indonesian", action="store_true")
    parser.add_argument("--indonesian-source", default="")
    parser.add_argument("--indonesian-license", default="")
    parser.add_argument(
        "--api-base-url",
        default="",
        help="Fetch real word transliteration and English meaning from a documented API.",
    )
    parser.add_argument(
        "--api-cache",
        type=Path,
        help="Optional cache outside the repository so interrupted imports can resume.",
    )
    args = parser.parse_args()
    words, ayahs, source_hash = import_rows(
        args.source,
        args.target,
        include_indonesian=args.include_indonesian,
        indonesian_source=args.indonesian_source,
        indonesian_license=args.indonesian_license,
        api_base_url=args.api_base_url,
        api_cache=args.api_cache,
    )
    print(f"Imported {words} words across {ayahs} ayahs")
    print(f"source_sha256={source_hash}")


if __name__ == "__main__":
    main()
