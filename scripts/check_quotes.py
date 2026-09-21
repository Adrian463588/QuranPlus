with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

in_string = False
escape = False
for idx, line in enumerate(lines, 1):
    for char in line:
        if in_string:
            if escape:
                escape = False
            elif char == '\\':
                escape = True
            elif char == '"':
                in_string = False
        else:
            if char == '"':
                in_string = True
    if in_string:
        print(f"Line {idx} left string open: {line.strip()[:60]}")
        break
