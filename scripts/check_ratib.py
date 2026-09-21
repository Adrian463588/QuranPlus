import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

pattern = re.compile(r'DzikirItem\s*\(\s*id\s*=\s*"(ratib_\d+)",\s*category\s*=\s*DzikirCategory\.RATIB_AL_HADDAD,\s*orderNumber\s*=\s*(\d+),\s*title\s*=\s*"([^"]+)",\s*arabicText\s*=\s*"(.*?)"\s*,\s*tajwidTags', re.DOTALL)
for item_id, order, title, arab in pattern.findall(text):
    clean_arab = arab.replace('\\n', ' ').replace('\\"', '"')
    print(f"[{item_id}] #{order} {title}: {clean_arab[:70]}...")
