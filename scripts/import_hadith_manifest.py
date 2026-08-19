"""Import hadith provenance metadata without bundling hadith text.

The reference contains 17 collections, but its data license, grading coverage,
and completeness are not release evidence. This script therefore writes only
collection metadata and marks every collection as bundle_allowed=false.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
from pathlib import Path


EXPECTED_COLLECTIONS = 17


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def validate_collection(path: Path) -> dict[str, object]:
    document = json.loads(path.read_text(encoding="utf-8"))
    hadiths = document.get("hadiths")
    chapters = document.get("chapters")
    metadata = document.get("metadata", {})
    if not isinstance(hadiths, list) or not isinstance(chapters, list):
        raise ValueError(f"Invalid hadith schema: {path}")

    missing = 0
    for record in hadiths:
        english = record.get("english") or {}
        if not record.get("arabic") or not english.get("text"):
            missing += 1

    english_metadata = metadata.get("english", {})
    arabic_metadata = metadata.get("arabic", {})
    return {
        "collection_id": path.stem,
        "title_arabic": arabic_metadata.get("title", ""),
        "title_english": english_metadata.get("title", path.stem),
        "source_revision": "1.3.0",
        "source_sha256": sha256(path),
        "license_status": "review_required",
        "grade_status": "not_present_in_source",
        "record_count": len(hadiths),
        "chapter_count": len(chapters),
        "is_complete": missing == 0,
        "missing_arabic_or_english_text": missing,
        "bundle_allowed": False,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--source-root",
        type=Path,
        default=Path("docs/HadistReference/reference2"),
    )
    parser.add_argument(
        "--target",
        type=Path,
        default=Path("app/src/main/assets/databases/quranplus.db"),
    )
    parser.add_argument(
        "--manifest",
        type=Path,
        default=None,
        help="Optional tracked provenance manifest output path",
    )
    args = parser.parse_args()

    source_root = args.source_root.resolve()
    target = args.target.resolve()
    files = sorted((source_root / "db" / "by_book").glob("**/*.json"))
    if len(files) != EXPECTED_COLLECTIONS:
        raise SystemExit(f"Expected {EXPECTED_COLLECTIONS} collections, found {len(files)}")
    if not target.is_file():
        raise SystemExit(f"Target database does not exist: {target}")

    manifests = [validate_collection(path) for path in files]
    with sqlite3.connect(target) as database:
        database.execute(
            """
            CREATE TABLE IF NOT EXISTS hadith_collections (
                id TEXT NOT NULL PRIMARY KEY,
                title_arabic TEXT NOT NULL,
                title_english TEXT NOT NULL,
                source_revision TEXT NOT NULL,
                source_sha256 TEXT NOT NULL,
                license_status TEXT NOT NULL,
                grade_status TEXT NOT NULL,
                record_count INTEGER NOT NULL,
                chapter_count INTEGER NOT NULL,
                is_complete INTEGER NOT NULL,
                bundle_allowed INTEGER NOT NULL
            )
            """
        )
        database.execute(
            """
            CREATE TABLE IF NOT EXISTS hadith_chapters (
                collection_id TEXT NOT NULL,
                chapter_id TEXT NOT NULL,
                chapter_number INTEGER NOT NULL,
                title_arabic TEXT NOT NULL,
                title_english TEXT NOT NULL,
                PRIMARY KEY(collection_id, chapter_id)
            )
            """
        )
        for manifest in manifests:
            database.execute(
                """
                INSERT OR REPLACE INTO hadith_collections (
                    id, title_arabic, title_english, source_revision,
                    source_sha256, license_status, grade_status, record_count,
                    chapter_count, is_complete, bundle_allowed
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    manifest["collection_id"],
                    manifest["title_arabic"],
                    manifest["title_english"],
                    manifest["source_revision"],
                    manifest["source_sha256"],
                    manifest["license_status"],
                    manifest["grade_status"],
                    manifest["record_count"],
                    manifest["chapter_count"],
                    int(manifest["is_complete"]),
                    int(manifest["bundle_allowed"]),
                ),
            )

    payload = {
        "status": "reference-reviewed-not-bundled",
        "bundled_record_count": 0,
        "source": {
            "repository": "https://github.com/AhmedBaset/hadith-json",
            "revision": "1.3.0-reference2",
            "path": "docs/HadistReference/reference2/db/by_book",
            "collections": manifests,
            "total_record_count": sum(int(item["record_count"]) for item in manifests),
            "incomplete_record_count": sum(
                int(item["missing_arabic_or_english_text"]) for item in manifests
            ),
            "license_status": "review_required",
            "record_grade_policy": "not_present_in_source",
        },
        "runtime_policy": {
            "bundled_corpus": False,
            "bundled_raw_reference": False,
            "required_before_import": [
                "source revision is pinned to a reviewed tag or reconciled full commit",
                "source license and scraped-data permission verified",
                "record-level grading preserved",
                "complete Arabic and English corpus validated",
                "embedding model and tokenizer SHA-256 match runtime",
                "vector dimension and coverage verified",
            ],
        },
    }
    if args.manifest is not None:
        args.manifest.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    print(json.dumps({"collection_count": len(manifests), "manifests": manifests}, ensure_ascii=False))


if __name__ == "__main__":
    main()
