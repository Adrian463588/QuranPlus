"""Read-only regressions for reviewed Arabic and unchanged Latin transliteration.

Letter-name reference: https://quran.nu.or.id/wirid/hizib/hizib-bahr
Quran reference: the same bundled Mushaf used by the Quran reader.
"""
import hashlib
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt"
DATABASE = ROOT / "app/src/main/assets/databases/quranplus.db"
TEXT_FIELD = r'((?:\\.|[^"\\])*)'


def validate() -> None:
    text = CATALOG.read_text(encoding="utf-8")
    items = dict(re.findall(r'id = "([^"]+)".*?arabicText = "' + TEXT_FIELD + r'"', text, re.S))
    with sqlite3.connect(f"file:{DATABASE.as_posix()}?mode=ro", uri=True) as db:
        for item_id, surah, end in [("matsurat_02", 2, 5), ("bahr_05", 36, 9)]:
            verses = db.execute(
                "SELECT text_arabic FROM ayahs WHERE surah_id=? AND ayah_number "
                "BETWEEN 1 AND ? ORDER BY ayah_number", (surah, end)
            ).fetchall()
            assert len(verses) == end, f"Missing canonical verses: {surah}:1-{end}"
            prefix = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\\n" if surah == 2 else ""
            expected = prefix + " ۝ ".join(row[0] for row in verses)
            assert items[item_id] == expected, f"Arabic/harakat/waqaf drift: {item_id}"

    bare_letters = re.compile(r"(?<![\u0621-\u06ff])(?:كهيعص|يس|حم|عسق|طس|الم)(?![\u0621-\u06ff])")
    for item_id, arabic in items.items():
        assert not bare_letters.search(arabic), f"Missing letter-name mad: {item_id}"
    latin = re.findall(r'transliteration = "' + TEXT_FIELD + r'"', text)
    # Reviewed Latin is outside this repair's scope. Update only after explicit review.
    assert hashlib.sha256("\n".join(latin).encode()).hexdigest() == LATIN_SHA256, "Latin drift"
    print(f"PASS: {len(items)} entries; canonical Quran harakat/waqaf; Latin unchanged")


LATIN_SHA256 = "18edf2e93dcfbdc69d365ddfe9a2e6f42663c1da66be00039b450f50ed8d8c11"

if __name__ == "__main__":
    validate()
