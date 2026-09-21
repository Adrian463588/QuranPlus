import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    m = re.search(r'category\s*=\s*DzikirCategory\.([A-Z_]+)', line)
    if m:
        id_m = re.search(r'id\s*=\s*"([^"]+)"', lines[i-1] if i > 0 else "")
        id_val = id_m.group(1) if id_m else "?"
        if id_val.endswith("_01"):
            print(f"Category {m.group(1)} starts near line {i}: id={id_val}")
print(f"Total lines: {len(lines)}")
