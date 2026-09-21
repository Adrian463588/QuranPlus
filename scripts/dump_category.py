import re
import sys

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

def dump_category(cat_name):
    print(f"\n==================================================================")
    print(f"CATEGORY: {cat_name}")
    print(f"==================================================================")
    # Find all DzikirItem blocks for this category
    blocks = re.findall(r'DzikirItem\s*\(\s*id\s*=\s*"([^"]+)",\s*category\s*=\s*DzikirCategory\.' + cat_name + r',.*?fadhilah\s*=\s*.*?\n            \)', text, re.DOTALL)
    
    # Or simpler regex matching DzikirItem
    pattern = re.compile(r'DzikirItem\s*\(\s*id\s*=\s*"([^"]+)",\s*category\s*=\s*DzikirCategory\.' + cat_name + r',\s*orderNumber\s*=\s*(\d+),\s*title\s*=\s*"([^"]+)",\s*arabicText\s*=\s*"(.*?)"\s*,\s*tajwidTags\s*=\s*"(.*?)"\s*,\s*transliteration\s*=\s*"(.*?)"\s*,\s*translationId\s*=\s*"(.*?)"\s*,\s*translationEn\s*=\s*"(.*?)"\s*,\s*repeatCount\s*=\s*(\d+)', re.DOTALL)
    matches = pattern.findall(text)
    for item_id, order, title, arab, tajwid, trans, tr_id, tr_en, repeat in matches:
        clean_arab = arab.replace('\\n', '\n').replace('\\"', '"')
        clean_trans = trans.replace('\\n', '\n').replace('\\"', '"')
        clean_id = tr_id.replace('\\n', '\n').replace('\\"', '"')
        print(f"\n--- [{item_id}] Order {order}: {title} (Repeat: {repeat}x) ---")
        print(f"ARAB:\n{clean_arab}")
        print(f"LATIN: {clean_trans[:100]}...")
        print(f"ARTI: {clean_id[:100]}...")

if len(sys.argv) > 1:
    for cat in sys.argv[1:]:
        dump_category(cat)
else:
    dump_category("SHALAT")
