import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Match DzikirItem blocks
items = re.findall(r'DzikirItem\s*\(\s*id\s*=\s*"([^"]+)",\s*category\s*=\s*DzikirCategory\.([A-Z_]+),\s*orderNumber\s*=\s*(\d+),\s*title\s*=\s*"([^"]+)"', text)

print(f"Total items found: {len(items)}")
by_cat = {}
for item_id, cat, order, title in items:
    by_cat.setdefault(cat, []).append((int(order), item_id, title))

for cat in sorted(by_cat.keys()):
    itemList = sorted(by_cat[cat], key=lambda x: x[0])
    print(f"\n================ CATEGORY: {cat} ({len(itemList)} items) ================")
    for order, item_id, title in itemList:
        print(f"  {order:02d}. [{item_id}] {title}")
