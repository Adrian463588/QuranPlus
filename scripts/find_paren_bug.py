with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Let's count parentheses from listOf(
start_counting = False
depth = 0
for line_no, line in enumerate(text.splitlines(), start=1):
    if 'val ALL_ITEMS: List<DzikirItem> by lazy {' in line:
        continue
    if 'listOf(' in line and not start_counting:
        start_counting = True
        depth = 1
        continue
    if start_counting:
        # Avoid counting parens inside strings
        in_str = False
        escaped = False
        for ch in line:
            if escaped:
                escaped = False
                continue
            if ch == '\\':
                escaped = True
                continue
            if ch == '"':
                in_str = not in_str
                continue
            if not in_str:
                if ch == '(':
                    depth += 1
                elif ch == ')':
                    depth -= 1
                    if depth <= 0:
                        print(f"Depth reached {depth} at line {line_no}: {line}")
