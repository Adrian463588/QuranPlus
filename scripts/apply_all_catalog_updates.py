# -*- coding: utf-8 -*-
import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

from build_full_dzikir_update import shalat_12_items

with open('scripts/generated_matsurat_28.txt', 'r', encoding='utf-8') as f:
    matsurat_28_items = f.read()

from chunk_nashr_and_haddad import nashr_syadzili_and_haddad
from chunk_nawawi import nawawi_items

# A. Replace SHALAT: from shalat_01 to end of shalat_09
shalat_pattern = re.compile(r'(\s*DzikirItem\s*\(\s*id\s*=\s*"shalat_01".*?DzikirItem\s*\(\s*id\s*=\s*"shalat_09".*?\n            \),)', re.DOTALL)
m = shalat_pattern.search(text)
assert m, "SHALAT pattern not matched"
text = text[:m.start()] + "\n" + shalat_12_items + text[m.end():]
print("SHALAT replaced.")

# B. Replace AL_MATSURAT: from matsurat_01 to end of matsurat_21
matsurat_pattern = re.compile(r'(\s*DzikirItem\s*\(\s*id\s*=\s*"matsurat_01".*?DzikirItem\s*\(\s*id\s*=\s*"matsurat_21".*?\n            \),)', re.DOTALL)
m = matsurat_pattern.search(text)
assert m, "AL_MATSURAT pattern not matched"
text = text[:m.start()] + "\n" + matsurat_28_items + ",\n" + text[m.end():]
print("AL_MATSURAT replaced.")

# C. Replace HIZIB_NASHR: from nashr_01 to end of nashr_06 (and insert haddad_nashr)
nashr_pattern = re.compile(r'(\s*DzikirItem\s*\(\s*id\s*=\s*"nashr_01".*?DzikirItem\s*\(\s*id\s*=\s*"nashr_06".*?\n            \),)', re.DOTALL)
m = nashr_pattern.search(text)
assert m, "HIZIB_NASHR pattern not matched"
text = text[:m.start()] + "\n" + nashr_syadzili_and_haddad + "\n" + text[m.end():]
print("HIZIB_NASHR and HIZIB_NASHR_HADDAD inserted.")

# D. Replace HIZIB_NAWAWI: from nawawi_01 to end of file
nawawi_start = text.find('id = "nawawi_01"')
assert nawawi_start != -1, "nawawi_01 not found"
# Find start of DzikirItem for nawawi_01
dzikir_item_idx = text.rfind('DzikirItem(', 0, nawawi_start)
assert dzikir_item_idx != -1

# Find the end of ALL_ITEMS: \n        )\n    }\n}
end_catalog_idx = text.rfind(')\n    }\n}')
assert end_catalog_idx != -1

replacement_nawawi = nawawi_items + "\n        )\n    }\n}\n"
text = text[:dzikir_item_idx] + replacement_nawawi
print("HIZIB_NAWAWI replaced.")

# Write updated file
with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'w', encoding='utf-8') as out:
    out.write(text)

print("All updates successfully written to DzikirDataCatalog.kt!")
