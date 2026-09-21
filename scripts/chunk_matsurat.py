# -*- coding: utf-8 -*-

# Let's read the existing matsurat items from DzikirDataCatalog.kt to reuse the perfectly verified existing ones and interleave/append the new ones with accurate order numbers!
with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

items = re.findall(r'(DzikirItem\s*\(\s*id\s*=\s*"matsurat_\d+".*?fadhilah\s*=\s*"[^"]*"\s*\))', text, re.DOTALL)
print(f"Found {len(items)} existing matsurat items.")
