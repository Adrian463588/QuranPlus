import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

items = re.findall(rf'id\s*=\s*"(nashr_\d+)".*?title\s*=\s*"([^"]+)".*?arabicText\s*=\s*"(.*?)",\s*tajwidTags', text, re.DOTALL)
for item_id, title, arab in items:
    clean_arab = arab.replace('\\n', '\n').replace('\\"', '"')
    print(f"[{item_id}] {title}")
    print(clean_arab)
    print("-" * 50)
