import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Replace "\n                fadhilah = " with ",\n                fadhilah = "
new_text = re.sub(r'"\\n(\s*fadhilah\s*=)', r'",\n\1', text)

print(f"Substitutions made: {text != new_text}")

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'w', encoding='utf-8') as f:
    f.write(new_text)

print("Updated DzikirDataCatalog.kt!")
