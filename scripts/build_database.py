#!/usr/bin/env python3
"""Fail-closed database build guard.

The former generator mixed unverified network data and manual hadith/RAG seed
text. Production data must be rebuilt only from reviewed, hash-pinned sources
with verified model/index parity. This guard prevents accidental fabrication.
"""


def main() -> None:
    raise SystemExit(
        "BLOCKED: database generation requires reviewed Quran/Tahsin provenance "
        "and verified Hadith, embedding, and sqlite-vec manifests."
    )


if __name__ == "__main__":
    main()
