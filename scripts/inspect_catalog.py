import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

items = re.findall(r'id\s*=\s*"([^"]+)",\s*category\s*=\s*DzikirCategory\.([A-Z_]+)', text)
print(f'Total items found: {len(items)}')
categories = {}
for item_id, cat in items:
    categories[cat] = categories.get(cat, 0) + 1
for cat, count in sorted(categories.items()):
    print(f'{cat}: {count} items')

# Print titles for HIZIB_NASHR, HIZIB_BAHR, HIZIB_NAWAWI
for cat in ['HIZIB_NASHR', 'HIZIB_BAHR', 'HIZIB_NAWAWI']:
    print(f'\n--- {cat} ---')
    pattern = rf'id\s*=\s*"([^"]+)",\s*category\s*=\s*DzikirCategory\.{cat}.*?title\s*=\s*"([^"]+)"'
    for item_id, title in re.findall(pattern, text, re.DOTALL):
        print(f'{item_id}: {title}')
