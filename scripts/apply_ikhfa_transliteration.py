import sqlite3
import re
import time

DB_PATH = 'app/src/main/assets/databases/quranplus.db'

# 15 Ikhfa consonants in Latin transliteration:
# ta (t), tsa (ṡ, ts), jim (j), dal (d), dzal (ż, dz), zai (z), sin (s),
# syin (sy, sh), shad (ṣ), dhad (ḍ), tha (ṭ), zha (ẓ), fa (f), qaf (q), kaf (k)
IKHFA_REGEX_INTER = re.compile(
    r'\b([a-zA-Zāīūḍṣṭẓżṡ‘\']*)([aiuāīū])n(\s+)([tTjJdDzZsSfFqQkK]|ṡ|ts|ż|dz|sy|sh|ṣ|ḍ|ṭ|ẓ)',
    re.UNICODE
)
IKHFA_REGEX_INTRA = re.compile(
    r'\b([a-zA-Zāīūḍṣṭẓżṡ‘\']*)n([tTjJdDzZsSfFqQkK]|ṡ|ts|ż|dz|sy|sh|ṣ|ḍ|ṭ|ẓ)',
    re.UNICODE
)

def apply_ikhfa_to_text(text: str) -> str:
    if not text:
        return text
    # 1. Inter-word: tanwin or nun-sukun at end of word meeting ikhfa letter
    # e.g., "fājiran kaffārā" -> "fājirang kaffārā", "min qablik" -> "ming qablik"
    res = IKHFA_REGEX_INTER.sub(r'\1\2ng\3\4', text)
    # 2. Intra-word: nun sukun inside word meeting ikhfa letter
    # e.g., "yunfiqūn" -> "yungfiqūn", "kuntum" -> "kungtum", "unzila" -> "ungzila"
    res = IKHFA_REGEX_INTRA.sub(r'\1ng\2', res)
    return res

def apply_intra_ikhfa(text: str) -> str:
    if not text:
        return text
    return IKHFA_REGEX_INTRA.sub(r'\1ng\2', text)

def main():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    start_time = time.time()

    print(f"Opening database: {DB_PATH}")
    cursor.execute("SELECT id, surah_id, ayah_number, transliteration FROM ayahs")
    ayahs = cursor.fetchall()

    updated_ayahs = 0
    for row_id, surah_id, ayah_num, translit in ayahs:
        new_translit = apply_ikhfa_to_text(translit)
        if new_translit != translit:
            cursor.execute(
                "UPDATE ayahs SET transliteration = ? WHERE id = ?",
                (new_translit, row_id)
            )
            updated_ayahs += 1

    conn.commit()
    print(f"Updated {updated_ayahs} of {len(ayahs)} ayahs with Ikhfa 'ng' transliteration.")

    # Update word_by_word transliteration (intra-word)
    cursor.execute("SELECT id, transliteration FROM word_by_word WHERE transliteration IS NOT NULL")
    words = cursor.fetchall()
    updated_words = 0
    for word_id, word_translit in words:
        new_word_translit = apply_intra_ikhfa(word_translit)
        if new_word_translit != word_translit:
            cursor.execute(
                "UPDATE word_by_word SET transliteration = ? WHERE id = ?",
                (new_word_translit, word_id)
            )
            updated_words += 1

    conn.commit()
    print(f"Updated {updated_words} of {len(words)} word_by_word records.")

    # Rebuild FTS table if ayahs_fts exists
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='ayahs_fts'")
    if cursor.fetchone():
        print("Rebuilding ayahs_fts index...")
        try:
            cursor.execute("INSERT INTO ayahs_fts(ayahs_fts) VALUES('rebuild')")
            conn.commit()
            print("ayahs_fts rebuilt successfully.")
        except Exception as e:
            print(f"ayahs_fts rebuild notice: {e}")

    conn.close()
    print(f"Finished in {time.time() - start_time:.2f}s.")

if __name__ == '__main__':
    main()
