with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

paren_stack = []
for idx, line in enumerate(lines, 1):
    in_str = False
    esc = False
    for col, char in enumerate(line, 1):
        if in_str:
            if esc:
                esc = False
            elif char == '\\':
                esc = True
            elif char == '"':
                in_str = False
        else:
            if char == '"':
                in_str = True
            elif char in '([{':
                paren_stack.append((char, idx, col))
            elif char in ')]}':
                top, l, c = paren_stack.pop()
                if l == 8:
                    print(f"listOf( at line 8:col {c} was closed at line {idx}:col {col}")
