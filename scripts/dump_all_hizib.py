import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

with open('scripts/hizib_output_utf8.txt', 'w', encoding='utf-8') as out:
    for prefix in ['nashr', 'bahr', 'nawawi']:
        out.write(f"\n============================== {prefix.upper()} ==============================\n")
        items = re.findall(rf'id\s*=\s*"({prefix}_\d+)".*?title\s*=\s*"([^"]+)".*?arabicText\s*=\s*"(.*?)",\s*tajwidTags', text, re.DOTALL)
        for item_id, title, arab in items:
            clean = arab.replace('\\n', '\n').replace('\\"', '"')
            out.write(f"\n--- {item_id}: {title} ---\n")
            out.write(clean + "\n")
print("Done writing UTF-8")
