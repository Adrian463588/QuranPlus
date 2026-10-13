"""Validate/reproduce content-based numbering without downloading data in CI.

Reproduce with --source-dir pointing to the pinned source files recorded below.
Only a unique exact Indonesian text match (ignoring case/punctuation/spacing)
is mapped. No offsets, fuzzy matches, or model-generated references are used.
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / "app/src/main/assets/hadith/numbering.json"
BUNDLE_REVISION = "8e15b4f9e7585822426a0d470e8dfec27e2a1707"
BUNDLE_SHA256 = "1f7ffe09ccf0be44293d3b68de036a37d4c54fc36d7f81390d8e5faeed547b1c"
REFERENCE_REVISION = "df57907be35291c91ad6a6691180e22ca9920784"
BOOKS = {
    "abudawud": "abu-daud", "bukhari": "bukhari", "ibnmajah": "ibnu-majah",
    "muslim": "muslim", "nasai": "nasai", "tirmidhi": "tirmidzi",
}
REFERENCE_SHA256 = {
    "abudawud": "b00cc9b74b012f3da67c7f3fe8275b61cd223707f9dba7ff68cab38d0b89a69f",
    "bukhari": "2222688acca11b122916e302c4a409cfd22b9d4163b0641ac004c88fbc3c2661",
    "ibnmajah": "9fa89db8865c4bd208db66d5f78ca220c797978ac54ab6a8b67eb8a6f96540f8",
    "muslim": "97a76412987c7f207d02bee4c36e752fa50a5e1e46ad6769892ed7c1491b9636",
    "nasai": "24c6f23371de8bef3fa7670e8bb8aa271ca53922df35513f8f373ceaf7dc3963",
    "tirmidhi": "85ecdb090a40630cb9b7ed57b0d67f78d21479c331ea397a61a314ae78256ea8",
}
ASSET_SHA256 = "ad1d8922b8449290361d3746b91718f50dc40f952d0f13cae2d256ed37398137"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def normalize(text: str) -> str:
    return "".join(char for char in text.lower() if char.isalnum())


def build(source_dir: Path) -> dict:
    import re
    manifest = (ROOT / "app/src/main/java/com/quranplus/app/features/hadith/data/HadithBundleManifest.kt").read_text()
    book_hashes = dict(re.findall(r'"([a-z-]+\.json)" to "([a-f0-9]{64})"', manifest))
    result = {
        "bundle_revision": BUNDLE_REVISION, "bundle_sha256": BUNDLE_SHA256,
        "reference_revision": REFERENCE_REVISION,
        "reference_source": f"https://github.com/fawazahmed0/hadith-api/tree/{REFERENCE_REVISION}",
        "reference_license": "Unlicense", "method": "unique_exact_normalized_translation",
        "columns": ["archive_number", "reference_number", "book", "hadith", "content_sha256"],
        "sources": {}, "collections": {},
    }
    for collection, filename in BOOKS.items():
        source = (source_dir / f"{filename}.json").read_bytes()
        reference = (source_dir / f"canonical-ind-{collection}.min.json").read_bytes()
        assert sha256(source) == book_hashes[f"{filename}.json"], f"Foreign archive book: {collection}"
        assert sha256(reference) == REFERENCE_SHA256[collection], f"Foreign reference book: {collection}"
        result["sources"][collection] = {
            "archive_book_sha256": sha256(source), "reference_book_sha256": sha256(reference),
        }
        candidates = collections.defaultdict(list)
        for record in json.loads(reference)["hadiths"]:
            if normalize(record["text"]):
                candidates[normalize(record["text"])].append(record)
        rows = []
        for record in json.loads(source):
            matches = candidates[normalize(record["id"])]
            if len(matches) != 1:
                continue
            matched = matches[0]
            number = matched["hadithnumber"]
            if collection == "muslim":
                # Muslim's continuous API ordinal is NOT its Arabic reference.
                # Keep the Arabic base number and identify each variant by the
                # real in-book reference, without guessing letter suffixes.
                number = str(matched.get("arabicnumber", "")).split(".")[0]
                if not number.isdecimal() or matched["reference"]["book"] <= 0:
                    continue
                number = int(number)
            if not isinstance(number, int) or number <= 0:
                continue
            content_hash = sha256((record["arab"] + "\n" + record["id"]).encode())
            ref = matched["reference"]
            rows.append([record["number"], number, ref["book"], ref["hadith"], content_hash])
        result["collections"][collection] = sorted(rows)
    return result


def validate(document: dict) -> None:
    assert document["bundle_revision"] == BUNDLE_REVISION
    assert document["bundle_sha256"] == BUNDLE_SHA256
    assert document["reference_revision"] == REFERENCE_REVISION
    assert set(document["collections"]) == set(BOOKS)
    for book, rows in document["collections"].items():
        assert rows and rows == sorted(rows), book
        assert len({row[0] for row in rows}) == len(rows), book
        assert all(len(row) == 5 and row[0] > 0 and row[1] > 0 and len(row[4]) == 64 for row in rows), book
        if book != "muslim":
            assert len({row[1] for row in rows}) == len(rows), book
    abu = {row[0]: row[1] for row in document["collections"]["abudawud"]}
    assert abu[3340] == 3837 and abu[3837] == 4420, "Abu Dawud numbering regression"
    total = sum(len(rows) for rows in document["collections"].values())
    print(f"PASS: {total} content-matched references; Abu Dawud 3340 -> 3837; ambiguous matches excluded")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", type=Path)
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    document = json.loads(ASSET.read_text(encoding="utf-8")) if ASSET.exists() else None
    if args.source_dir:
        rebuilt = build(args.source_dir)
        validate(rebuilt)
        if args.write:
            ASSET.parent.mkdir(parents=True, exist_ok=True)
            # One reviewed row per line keeps future reference diffs reviewable.
            serialized = json.dumps(rebuilt, ensure_ascii=False, indent=2)
            import re
            serialized = re.sub(r'\[\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*"([a-f0-9]{64})"\s*\]',
                                r'[\1, \2, \3, \4, "\5"]', serialized)
            ASSET.write_text(serialized + "\n", encoding="utf-8")
            document = rebuilt
        else:
            assert document == rebuilt, "Numbering asset differs from pinned source content"
    assert document is not None, "Missing numbering asset"
    assert sha256(ASSET.read_bytes()) == ASSET_SHA256, "Reviewed numbering asset drift"
    validate(document)
