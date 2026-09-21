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
                if not paren_stack:
                    print(f"Extra closing {char} at line {idx}:{col}")
                else:
                    top, l, c = paren_stack.pop()
                    match = {'(': ')', '[': ']', '{': '}'}[top]
                    if match != char:
                        print(f"Mismatched {char} at line {idx}:{col}, expected {match} from line {l}:{c}")

print(f"Remaining open brackets: {len(paren_stack)}")
for item in paren_stack[-10:]:
    print(item)
