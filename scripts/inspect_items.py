import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Let's find all DzikirItem blocks
# Match DzikirItem( ... ),
items = []
pattern = re.compile(r'DzikirItem\s*\((.*?)\n\s*\),?', re.DOTALL)
matches = list(pattern.finditer(text))

print(f"Total matched items: {len(matches)}")
for idx, m in enumerate(matches):
    content = m.group(1)
    # find id
    id_m = re.search(r'id\s*=\s*"([^"]+)"', content)
    cat_m = re.search(r'category\s*=\s*DzikirCategory\.([A-Z_]+)', content)
    item_id = id_m.group(1) if id_m else "UNKNOWN"
    cat = cat_m.group(1) if cat_m else "UNKNOWN"
    if idx < 5 or idx > len(matches) - 5 or 'latif_09' in item_id or 'latif_10' in item_id or 'latif_11' in item_id:
        print(f"Item {idx}: id={item_id}, cat={cat}")
