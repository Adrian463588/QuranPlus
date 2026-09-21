import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

pattern = re.compile(r'DzikirItem\s*\(\s*id\s*=\s*"(matsurat_\d+)",.*?orderNumber\s*=\s*(\d+),\s*title\s*=\s*"([^"]+)"', re.DOTALL)
for item_id, order, title in pattern.findall(text):
    print(f"[{item_id}] #{order}: {title}")
