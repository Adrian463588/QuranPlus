import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

pattern = re.compile(r'(\s*DzikirItem\s*\(.*?\n\s*\)),?', re.DOTALL)
matches = list(pattern.finditer(text))

categories = {
    'PAGI': [],
    'PETANG': [],
    'SHALAT': [],
    'AL_MATSURAT': [],
    'RATIB_AL_HADDAD': [],
    'WIRDUL_LATIF': [],
    'HIZIB_BAHR': [],
    'HIZIB_NASHR': [],
    'HIZIB_NASHR_HADDAD': [],
    'HIZIB_NAWAWI': []
}

for m in matches:
    raw_item = m.group(1).strip()
    cat_m = re.search(r'category\s*=\s*DzikirCategory\.([A-Z_]+)', raw_item)
    assert cat_m, f"Could not find category in item:\n{raw_item[:100]}"
    cat = cat_m.group(1)
    assert cat in categories, f"Unknown category {cat}"
    categories[cat].append(raw_item)

for cat, itms in categories.items():
    print(f"Category {cat}: {len(itms)} items")

output_code = """package com.quranplus.app.features.dzikir.data

import com.quranplus.app.features.dzikir.domain.DzikirCategory
import com.quranplus.app.features.dzikir.domain.DzikirItem

object DzikirDataCatalog {
"""

cat_var_names = []
for cat, itms in categories.items():
    var_name = f"{cat}_ITEMS"
    cat_var_names.append(var_name)
    output_code += f"    val {var_name}: List<DzikirItem> by lazy {{\n        listOf(\n"
    for i, itm in enumerate(itms):
        # indent item with 12 spaces
        indented = "\n".join("            " + line.strip() for line in itm.splitlines())
        comma = "," if i < len(itms) - 1 else ""
        output_code += f"{indented}{comma}\n"
    output_code += "        )\n    }\n\n"

output_code += "    val ALL_ITEMS: List<DzikirItem> by lazy {\n        "
output_code += " +\n        ".join(cat_var_names) + "\n    }\n}\n"

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'w', encoding='utf-8') as f:
    f.write(output_code)

print("Regenerated DzikirDataCatalog.kt successfully with modular lists!")
