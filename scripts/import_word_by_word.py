"""Import the reviewed word-by-word Quran database into the app asset.

This script copies only source rows that exist in the reference database. It
fails closed on unexpected coverage and records the source checksum on every
row so the bundled data remains auditable.
"""

from __future__ import annotations

import argparse
import hashlib
import sqlite3
from pathlib import Path

EXPECTED_WORDS = 77_430
EXPECTED_AYAHS = 6_236
SOURCE_REVISION = "wordbyword.db:english-alignment-only-v3"


def normalize_english(value: str | None) -> str:
    """Remove the source DB's serialized quote escapes, not content."""
    return (value or "").replace('\\"', '"')


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def import_rows(source: Path, target: Path) -> tuple[int, int, str]:
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

        target_db.execute(
            """
            CREATE TABLE IF NOT EXISTS word_by_word (
                id INTEGER NOT NULL PRIMARY KEY,
                surah_id INTEGER NOT NULL,
                ayah_number INTEGER NOT NULL,
                word_index INTEGER NOT NULL,
                text_arabic TEXT NOT NULL,
                translation_en TEXT NOT NULL,
                translation_id TEXT NOT NULL,
                source_revision TEXT NOT NULL,
                source_sha256 TEXT NOT NULL
            )
            """
        )
        target_db.execute("DELETE FROM word_by_word")
        target_db.executemany(
            """
            INSERT INTO word_by_word(
                id, surah_id, ayah_number, word_index, text_arabic,
                translation_en, translation_id, source_revision, source_sha256
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            [
                (
                    row_id,
                    surah_id,
                    verse_id,
                    words_id,
                    arabic or "",
                    normalize_english(english),
                    "",
                    SOURCE_REVISION,
                    source_hash,
                )
                for row_id, surah_id, verse_id, words_id, arabic, english, indonesian in rows
            ],
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
        return word_count, ayah_count, source_hash
    finally:
        source_db.close()
        target_db.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    args = parser.parse_args()
    words, ayahs, source_hash = import_rows(args.source, args.target)
    print(f"Imported {words} words across {ayahs} ayahs")
    print(f"source_sha256={source_hash}")


if __name__ == "__main__":
    main()
