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
BASMALAH = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\\n"
QURAN_PASSAGES = {
    "matsurat_01": (1, 1, 7, ""), "matsurat_02": (2, 1, 5, BASMALAH),
    "matsurat_03": (2, 255, 255, ""), "matsurat_04": (2, 256, 257, ""),
    "matsurat_05": (2, 284, 286, ""),
    "matsurat_06": (112, 1, 4, BASMALAH), "matsurat_07": (113, 1, 5, BASMALAH),
    "matsurat_08": (114, 1, 6, BASMALAH),
    "ratib_02": (2, 255, 255, ""), "ratib_03": (2, 285, 286, ""),
    "latif_01": (112, 1, 4, BASMALAH), "latif_02": (113, 1, 5, BASMALAH),
    "latif_03": (114, 1, 6, BASMALAH), "latif_05": (23, 115, 118, ""),
    "latif_06": (30, 17, 19, ""),
    "latif_07": (59, 22, 24, "أَعُوذُ بِاللَّهِ السَّمِيعِ الْعَلِيمِ مِنَ الشَّيْطَانِ الرَّجِيمِ (٣ مرات)\\n\\n"),
    "latif_08": (37, 79, 81, ""), "bahr_05": (36, 1, 9, ""),
}


def base_letters(value: str) -> str:
    return ''.join(char for char in value if char.isalnum() and char not in '\u0640\u0670\u0672')


def validate() -> None:
    text = CATALOG.read_text(encoding="utf-8")
    items = dict(re.findall(r'id = "([^"]+)".*?arabicText = "' + TEXT_FIELD + r'"', text, re.S))
    tags = dict(re.findall(r'id = "([^"]+)".*?tajwidTags = "' + TEXT_FIELD + r'"', text, re.S))
    with sqlite3.connect(f"file:{DATABASE.as_posix()}?mode=ro", uri=True) as db:
        for item_id, (surah, start, end, prefix) in QURAN_PASSAGES.items():
            verses = db.execute(
                "SELECT text_arabic, tajwid_tags FROM ayahs WHERE surah_id=? AND ayah_number "
                "BETWEEN ? AND ? ORDER BY ayah_number", (surah, start, end)
            ).fetchall()
            assert len(verses) == end - start + 1, f"Missing canonical verses: {surah}:{start}-{end}"
            expected = prefix + " ۝ ".join(row[0] for row in verses)
            assert items[item_id] == expected, f"Arabic/harakat/waqaf drift: {item_id}"
            assert tags[item_id] == prefix + " ۝ ".join(row[1] for row in verses), f"Quran annotation drift: {item_id}"

    bare_letters = re.compile(r"(?<![\u0621-\u06ff])(?:كهيعص|يس|حم|عسق|طس|الم)(?![\u0621-\u06ff])")
    for item_id, arabic in items.items():
        assert not bare_letters.search(arabic), f"Missing letter-name mad: {item_id}"
    for item_id, tagged in tags.items():
        if item_id in QURAN_PASSAGES:
            continue  # Canonical annotations use equivalent Mushaf orthographies and were checked exactly above.
        plain = re.sub(r'\[[a-z]+(?::\d+)?\[', '', tagged).replace(']', '')
        # Harakat may differ in an annotation describing wasl. Base letters may not.
        assert base_letters(plain) == base_letters(items[item_id]), f"Annotation letter corruption: {item_id}"
    latin = re.findall(r'transliteration = "' + TEXT_FIELD + r'"', text)
    # Reviewed Latin is outside this repair's scope. Update only after explicit review.
    assert hashlib.sha256("\n".join(latin).encode()).hexdigest() == LATIN_SHA256, "Latin drift"
    print(f"PASS: {len(items)} entries; {len(QURAN_PASSAGES)} exact Quran passages; Latin unchanged")


LATIN_SHA256 = "18edf2e93dcfbdc69d365ddfe9a2e6f42663c1da66be00039b450f50ed8d8c11"

if __name__ == "__main__":
    validate()
