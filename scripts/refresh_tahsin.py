#!/usr/bin/env python3
"""Publish Tahsin lessons with Quran text resolved from the bundled database.

The lesson explanations are maintained in tahsin_data.py. This script keeps
the Quran examples factual by replacing every example with the exact Uthmani
text already shipped in quranplus.db. Missing references fail the refresh.
"""

from __future__ import annotations

import json
import runpy
import sqlite3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DATABASE = ROOT / "app/src/main/assets/databases/quranplus.db"
SEED = ROOT / "app/src/main/assets/seeds/tahsin_seed.json"
QUIZ_SEED = ROOT / "data/tahsin-quiz.json"

# A lesson can point to more than one ayah when the example is a short range.
EXAMPLES: dict[int, tuple[int, int, int]] = {
    1: (11, 49, 49),
    2: (112, 1, 1),
    3: (1, 2, 2),
    4: (1, 7, 7),
    5: (113, 1, 1),
    6: (1, 5, 5),
    7: (91, 1, 1),
    8: (1, 7, 7),
    9: (2, 3, 3),
    10: (1, 5, 5),
    11: (1, 3, 3),
    12: (1, 6, 6),
    13: (1, 7, 7),
    14: (78, 3, 3),
    15: (108, 2, 2),
    16: (111, 1, 1),
    17: (108, 1, 1),
    18: (81, 1, 1),
    19: (85, 1, 1),
    20: (4, 57, 57),
    21: (2, 19, 19),
    22: (2, 127, 127),
    23: (112, 3, 3),
    24: (2, 45, 45),
    25: (106, 1, 2),
    26: (20, 114, 114),
    27: (5, 114, 114),
    28: (113, 2, 2),
    29: (68, 10, 13),
    30: (114, 6, 6),
    31: (1, 7, 7),
    32: (99, 7, 7),
    33: (2, 2, 2),
    34: (104, 4, 4),
    35: (113, 2, 2),
    36: (105, 4, 4),
    37: (106, 4, 4),
    38: (105, 1, 1),
    39: (114, 1, 2),
    40: (4, 78, 78),
    41: (7, 189, 189),
    42: (77, 20, 20),
    43: (1, 2, 2),
    44: (1, 3, 3),
    45: (89, 10, 10),
    46: (111, 1, 1),
    47: (7, 121, 121),
    48: (110, 1, 1),
    49: (108, 1, 1),
    50: (1, 2, 2),
    51: (1, 7, 7),
    52: (84, 13, 13),
    53: (100, 1, 1),
    54: (2, 2, 2),
}

SURAH_NAMES = {
    1: "Al-Fatihah",
    2: "Al-Baqarah",
    4: "An-Nisa'",
    5: "Al-Ma'idah",
    7: "Al-A'raf",
    11: "Hud",
    20: "Taha",
    68: "Al-Qalam",
    77: "Al-Mursalat",
    78: "An-Naba'",
    81: "At-Takwir",
    84: "Al-Insyiqaq",
    85: "Al-Buruj",
    89: "Al-Fajr",
    91: "Asy-Syams",
    99: "Az-Zalzalah",
    100: "Al-'Adiyat",
    104: "Al-Humazah",
    105: "Al-Fil",
    106: "Quraisy",
    108: "Al-Kausar",
    110: "An-Nasr",
    111: "Al-Lahab",
    112: "Al-Ikhlas",
    113: "Al-Falaq",
    114: "An-Nas",
}


def resolve_examples(connection: sqlite3.Connection) -> dict[int, tuple[str, str]]:
    resolved: dict[int, tuple[str, str]] = {}
    for lesson_id, (surah, first, last) in EXAMPLES.items():
        rows = connection.execute(
            "SELECT text_arabic FROM ayahs "
            "WHERE surah_id = ? AND ayah_number BETWEEN ? AND ? "
            "ORDER BY ayah_number",
            (surah, first, last),
        ).fetchall()
        if len(rows) != last - first + 1:
            raise ValueError(f"Missing Quran example for lesson {lesson_id}: {surah}:{first}-{last}")
        text = "\n".join(row[0] for row in rows)
        suffix = str(last) if first == last else f"{first}-{last}"
        resolved[lesson_id] = (text, f"QS. {SURAH_NAMES[surah]}: {suffix}")
    return resolved


def publish_quiz_questions(connection: sqlite3.Connection) -> int:
    questions = json.loads(QUIZ_SEED.read_text(encoding="utf-8"))
    question_ids = [question["id"] for question in questions]
    if len(question_ids) != len(set(question_ids)):
        raise ValueError("Quiz question ids must be unique")

    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS quiz_questions (
            id INTEGER NOT NULL PRIMARY KEY,
            prompt TEXT NOT NULL,
            arabic_snippet TEXT NOT NULL,
            reference TEXT NOT NULL,
            options_json TEXT NOT NULL,
            correct_index INTEGER NOT NULL,
            explanation TEXT NOT NULL,
            source_id TEXT NOT NULL,
            source_revision TEXT NOT NULL
        )
        """
    )
    connection.execute("DELETE FROM quiz_questions")
    for question in questions:
        options = question["options"]
        correct_index = question["correct_index"]
        if len(options) < 2 or correct_index not in range(len(options)):
            raise ValueError(f"Invalid quiz options for question {question['id']}")
        if any(not str(question[field]).strip() for field in ("prompt", "arabic_snippet", "reference", "explanation")):
            raise ValueError(f"Quiz question {question['id']} has an empty required field")
        connection.execute(
            """
            INSERT INTO quiz_questions(
                id, prompt, arabic_snippet, reference, options_json,
                correct_index, explanation, source_id, source_revision
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                question["id"],
                question["prompt"],
                question["arabic_snippet"],
                question["reference"],
                json.dumps(options, ensure_ascii=False),
                correct_index,
                question["explanation"],
                question["source_id"],
                question["source_revision"],
            ),
        )
    return len(questions)


def main() -> None:
    lessons = runpy.run_path(str(ROOT / "scripts/tahsin_data.py"))["TAHSIN_LESSONS"]
    if len(lessons) != len(EXAMPLES):
        raise ValueError(f"Expected {len(EXAMPLES)} lessons, found {len(lessons)}")

    with sqlite3.connect(DATABASE) as connection:
        resolved = resolve_examples(connection)
        for lesson in lessons:
            example_text, example_ref = resolved[lesson["id"]]
            lesson["example_ayah_text"] = example_text
            lesson["example_ayah_ref"] = example_ref
            # No lesson audio is shipped. Null is more honest than a filename
            # that cannot be played or verified at runtime.
            lesson["audio_sample"] = None
            connection.execute(
                "UPDATE tahsin_lessons SET example_ayah_text = ?, "
                "example_ayah_ref = ?, audio_sample = ? WHERE id = ?",
                (example_text, example_ref, None, lesson["id"]),
            )
        quiz_count = publish_quiz_questions(connection)

    SEED.write_text(json.dumps(lessons, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "lessons": len(lessons),
        "examples_resolved": len(resolved),
        "quiz_questions": quiz_count,
        "audio": "unavailable",
    }))


if __name__ == "__main__":
    main()
