#!/usr/bin/env python3
"""
Quran Plus - SQLite Database Builder
Generates and packages the complete, authentic SQLite database asset:
1. 114 Surahs (Surah number, Arabic name, Latin name, English translation, Revelation type, Ayah count)
2. 6,236 Authentic Ayahs with Uthmani Arabic, Latin transliteration, Indonesian translation, English translation, Juz, Page, and Tajwid tags
3. FTS virtual table for fast full-text search across Arabic, Indonesian, English, and Transliteration
4. Authentic Hadith knowledge base (Nawawi 40 + selections from Malik, Ahmad, Darimi)
5. Structured Tahsin curriculum (17 Makharij, Sifat al-Huruf, Complete Tajwid Rules, Waqaf)
6. Knowledge chunks for on-device RAG vector search
7. Bookmarks, Last Read, and Chat tables compatible with Room entities
"""

import os
import sys
import json
import sqlite3
import urllib.request
import time
from pathlib import Path

# Import custom data modules
sys.path.append(str(Path(__file__).parent))
from tahsin_data import TAHSIN_LESSONS
from hadith_data import NAWAWI_METADATA, FOUNDATIONAL_SELECTIONS
from knowledge_chunks_data import KNOWLEDGE_CHUNKS

BASE_DIR = Path(__file__).resolve().parent.parent
CACHE_DIR = BASE_DIR / "scripts" / "cache"
ASSETS_DIR = BASE_DIR / "app" / "src" / "main" / "assets"
DB_OUTPUT_DIR = ASSETS_DIR / "databases"
SEEDS_OUTPUT_DIR = ASSETS_DIR / "seeds"
DB_FILE = DB_OUTPUT_DIR / "quranplus.db"

CACHE_DIR.mkdir(parents=True, exist_ok=True)
DB_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
SEEDS_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

def fetch_or_load_json(url: str, cache_name: str) -> dict:
    """Fetch JSON from URL or load from cache if available."""
    cache_path = CACHE_DIR / f"{cache_name}.json"
    if cache_path.exists():
        print(f"  [CACHE] Loading {cache_name} from local cache...")
        with open(cache_path, "r", encoding="utf-8") as f:
            return json.load(f)

    print(f"  [DOWNLOAD] Fetching {cache_name} from {url}...")
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "QuranPlus-DataEngine/1.0 (Android; Kotlin)"}
    )
    with urllib.request.urlopen(req, timeout=45) as response:
        content = response.read().decode("utf-8")
        data = json.loads(content)
        with open(cache_path, "w", encoding="utf-8") as f:
            f.write(content)
        return data

def build_quran_dataset():
    """Fetch and merge all 5 Quran editions into unified datasets."""
    print("\n--- 1. Fetching Quran Editions ---")
    
    surahs_meta_raw = fetch_or_load_json(
        "https://api.alquran.cloud/v1/surah", "surahs_metadata"
    )
    uthmani_raw = fetch_or_load_json(
        "https://api.alquran.cloud/v1/quran/quran-uthmani", "quran_uthmani"
    )
    tajweed_raw = fetch_or_load_json(
        "https://api.alquran.cloud/v1/quran/quran-tajweed", "quran_tajweed"
    )
    indonesian_raw = fetch_or_load_json(
        "https://api.alquran.cloud/v1/quran/id.indonesian", "quran_indonesian"
    )
    english_raw = fetch_or_load_json(
        "https://api.alquran.cloud/v1/quran/en.sahih", "quran_english"
    )
    transliteration_raw = fetch_or_load_json(
        "https://api.alquran.cloud/v1/quran/en.transliteration", "quran_transliteration"
    )

    surah_meta_list = surahs_meta_raw.get("data", [])
    uthmani_surahs = uthmani_raw.get("data", {}).get("surahs", [])
    tajweed_surahs = tajweed_raw.get("data", {}).get("surahs", [])
    indonesian_surahs = indonesian_raw.get("data", {}).get("surahs", [])
    english_surahs = english_raw.get("data", {}).get("surahs", [])
    transliteration_surahs = transliteration_raw.get("data", {}).get("surahs", [])

    surahs_records = []
    ayahs_records = []
    
    bismillah_prefix = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ "

    for s_idx in range(len(surah_meta_list)):
        s_meta = surah_meta_list[s_idx]
        s_num = s_meta["number"]
        
        # Standardize revelation type
        rev_type = s_meta.get("revelationType", "Meccan")
        rev_type_formatted = "Makkiyyah" if rev_type.lower() in ["meccan", "makkiyyah"] else "Madaniyyah"

        surah_row = {
            "id": s_num,
            "number": s_num,
            "name_arabic": s_meta.get("name", "").strip(),
            "name_latin": s_meta.get("englishName", "").strip(),
            "name_english": s_meta.get("englishNameTranslation", "").strip(),
            "revelation_type": rev_type_formatted,
            "ayah_count": s_meta.get("numberOfAyahs", 0)
        }
        surahs_records.append(surah_row)

        u_ayahs = uthmani_surahs[s_idx].get("ayahs", [])
        t_ayahs = tajweed_surahs[s_idx].get("ayahs", [])
        id_ayahs = indonesian_surahs[s_idx].get("ayahs", [])
        en_ayahs = english_surahs[s_idx].get("ayahs", [])
        tr_ayahs = transliteration_surahs[s_idx].get("ayahs", [])

        num_ayahs = len(u_ayahs)
        for a_idx in range(num_ayahs):
            u_ayah = u_ayahs[a_idx]
            global_id = u_ayah["number"] # 1..6236
            ayah_num = u_ayah["numberInSurah"]
            juz_num = u_ayah.get("juz", 1)
            page_num = u_ayah.get("page", 1)

            # Clean Arabic Uthmani text
            text_ar = u_ayah.get("text", "").strip().lstrip("\ufeff")
            # Strip prepended Bismillah for surah > 1, surah != 9, ayah == 1
            if s_num > 1 and s_num != 9 and ayah_num == 1:
                if text_ar.startswith(bismillah_prefix):
                    text_ar = text_ar[len(bismillah_prefix):].strip()

            # Tajweed tagged string
            tajwid_tag = t_ayahs[a_idx].get("text", "").strip() if a_idx < len(t_ayahs) else ""
            
            # Indonesian translation
            trans_id = id_ayahs[a_idx].get("text", "").strip() if a_idx < len(id_ayahs) else ""
            
            # English translation
            trans_en = en_ayahs[a_idx].get("text", "").strip() if a_idx < len(en_ayahs) else ""
            
            # Transliteration
            trans_lat = tr_ayahs[a_idx].get("text", "").strip() if a_idx < len(tr_ayahs) else ""

            ayah_row = {
                "id": global_id,
                "surah_id": s_num,
                "ayah_number": ayah_num,
                "text_arabic": text_ar,
                "transliteration": trans_lat,
                "translation_id": trans_id,
                "translation_en": trans_en,
                "juz": juz_num,
                "page": page_num,
                "tajwid_tags": tajwid_tag
            }
            ayahs_records.append(ayah_row)

    print(f"  [DONE] Processed {len(surahs_records)} Surahs and {len(ayahs_records)} Ayahs.")
    return surahs_records, ayahs_records

def build_hadith_dataset():
    """Build the complete authentic Hadith dataset from assets and metadata."""
    print("\n--- 2. Building Hadith Dataset ---")
    
    hadiths_records = []
    hadith_id_counter = 1

    # 1. Nawawi 40 (All 42 Hadiths)
    nawawi_asset_path = ASSETS_DIR / "hadith" / "data_nawawi40.json"
    nawawi_ar_map = {}
    if nawawi_asset_path.exists():
        with open(nawawi_asset_path, "r", encoding="utf-8") as f:
            for item in json.load(f):
                nawawi_ar_map[item["hadith_number"]] = item["text_ar"]

    for meta in NAWAWI_METADATA:
        h_num = meta["hadith_number"]
        text_ar = nawawi_ar_map.get(h_num, "")
        hadith_row = {
            "id": hadith_id_counter,
            "collection_id": "nawawi40",
            "hadith_number": h_num,
            "title": meta["title"],
            "text_arabic": text_ar,
            "translation_id": meta["translation_id"],
            "translation_en": meta["translation_en"],
            "reference": meta["reference"]
        }
        hadiths_records.append(hadith_row)
        hadith_id_counter += 1

    # 2. Foundational Selections from Malik, Ahmad, Darimi
    for sel in FOUNDATIONAL_SELECTIONS:
        hadith_row = {
            "id": hadith_id_counter,
            "collection_id": sel["collection_id"],
            "hadith_number": sel["hadith_number"],
            "title": sel["title"],
            "text_arabic": sel["text_arabic"],
            "translation_id": sel["translation_id"],
            "translation_en": sel["translation_en"],
            "reference": sel["reference"]
        }
        hadiths_records.append(hadith_row)
        hadith_id_counter += 1

    print(f"  [DONE] Built {len(hadiths_records)} Hadith records.")
    return hadiths_records

def build_tahsin_dataset():
    """Load and format Tahsin lessons."""
    print("\n--- 3. Loading Tahsin Curriculum ---")
    print(f"  [DONE] Loaded {len(TAHSIN_LESSONS)} Tahsin lessons.")
    return TAHSIN_LESSONS

def build_knowledge_chunks(surahs_records, hadiths_records, tahsin_records):
    """Generate rich knowledge chunks for RAG vector search."""
    print("\n--- 4. Building RAG Knowledge Chunks ---")
    chunks = []
    chunk_id_counter = 1

    # A. Curated thematic chunks
    for item in KNOWLEDGE_CHUNKS:
        chunks.append({
            "id": chunk_id_counter,
            "source_type": item["source_type"],
            "source_id": item["source_id"],
            "title": item["title"],
            "text_content": item["text_content"],
            "embedding": None
        })
        chunk_id_counter += 1

    # B. Chunks for all 42 Nawawi Hadiths
    for h in hadiths_records:
        if h["collection_id"] == "nawawi40":
            text = f"Hadits Arbain Nawawi No. {h['hadith_number']}: {h['title']}. Riwayat: {h['reference']}.\nTerjemahan Indonesia: {h['translation_id']}\nEnglish Translation: {h['translation_en']}"
            chunks.append({
                "id": chunk_id_counter,
                "source_type": "hadith",
                "source_id": f"hadith_nawawi_{h['hadith_number']}",
                "title": f"Hadits Arbain {h['hadith_number']}: {h['title']}",
                "text_content": text,
                "embedding": None
            })
            chunk_id_counter += 1

    # C. Chunks for Tahsin Lessons
    for t in tahsin_records:
        text = f"Pelajaran Tahsin & Tajwid: {t['title']} ({t['subcategory']}).\nHuruf: {t['letter_arabic']} ({t['letter_latin']}).\nTitik Artikulasi (Makhraj/Sifat): {t['articulation_point']}.\nPenjelasan: {t['description']}.\nContoh Ayat: {t['example_ayah_text']} ({t['example_ayah_ref']})."
        chunks.append({
            "id": chunk_id_counter,
            "source_type": "tahsin",
            "source_id": f"tahsin_{t['rule_type'].lower()}",
            "title": f"Tahsin: {t['title']}",
            "text_content": text,
            "embedding": None
        })
        chunk_id_counter += 1

    print(f"  [DONE] Generated {len(chunks)} Knowledge Chunks.")
    return chunks

def export_json_seeds(surahs, ayahs, hadiths, tahsin, chunks):
    """Export clean JSON seed files to assets/seeds directory."""
    print("\n--- 5. Exporting JSON Seed Files ---")
    
    seeds = {
        "surahs_seed.json": surahs,
        "hadiths_seed.json": hadiths,
        "tahsin_seed.json": tahsin,
        "knowledge_chunks_seed.json": chunks,
    }
    
    for filename, data in seeds.items():
        out_path = SEEDS_OUTPUT_DIR / filename
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"  [SEED] Saved {filename} ({len(data)} items)")

def initialize_and_populate_sqlite(surahs, ayahs, hadiths, tahsin, chunks):
    """Create SQLite database with all tables, indexes, and FTS virtual table."""
    print(f"\n--- 6. Creating SQLite Database: {DB_FILE} ---")
    
    # Clean up any existing DB and journal/WAL files
    for ext in ["", "-wal", "-shm", "-journal"]:
        p = Path(str(DB_FILE) + ext)
        if p.exists():
            try:
                p.unlink()
            except Exception:
                pass

    conn = sqlite3.connect(str(DB_FILE))
    cursor = conn.cursor()

    # Set PRAGMAs for standalone packaged DB
    cursor.execute("PRAGMA foreign_keys = OFF;")
    cursor.execute("PRAGMA journal_mode = DELETE;")
    cursor.execute("PRAGMA synchronous = OFF;")
    cursor.execute("PRAGMA user_version = 1;")

    # 1. Surahs Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS surahs (
        id INTEGER PRIMARY KEY NOT NULL,
        number INTEGER NOT NULL,
        name_arabic TEXT NOT NULL,
        name_latin TEXT NOT NULL,
        name_english TEXT NOT NULL,
        revelation_type TEXT NOT NULL,
        ayah_count INTEGER NOT NULL
    );
    """)

    # 2. Ayahs Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS ayahs (
        id INTEGER PRIMARY KEY NOT NULL,
        surah_id INTEGER NOT NULL,
        ayah_number INTEGER NOT NULL,
        text_arabic TEXT NOT NULL,
        transliteration TEXT NOT NULL,
        translation_id TEXT NOT NULL,
        translation_en TEXT NOT NULL,
        juz INTEGER NOT NULL,
        page INTEGER NOT NULL,
        tajwid_tags TEXT
    );
    """)

    # 3. Bookmarks Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS bookmarks (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        surah_id INTEGER NOT NULL,
        surah_name TEXT NOT NULL,
        ayah_number INTEGER NOT NULL,
        ayah_text_arabic TEXT NOT NULL,
        ayah_translation TEXT NOT NULL,
        note TEXT,
        timestamp INTEGER NOT NULL
    );
    """)

    # 4. Last Read Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS last_read (
        id INTEGER PRIMARY KEY NOT NULL,
        surah_id INTEGER NOT NULL,
        surah_name TEXT NOT NULL,
        ayah_number INTEGER NOT NULL,
        timestamp INTEGER NOT NULL
    );
    """)

    # 5. Tahsin Lessons Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS tahsin_lessons (
        id INTEGER PRIMARY KEY NOT NULL,
        category TEXT NOT NULL,
        subcategory TEXT NOT NULL,
        title TEXT NOT NULL,
        letter_arabic TEXT NOT NULL,
        letter_latin TEXT NOT NULL,
        description TEXT NOT NULL,
        articulation_point TEXT NOT NULL,
        audio_sample TEXT,
        example_ayah_text TEXT NOT NULL,
        example_ayah_ref TEXT NOT NULL,
        rule_type TEXT NOT NULL,
        order_index INTEGER NOT NULL,
        is_completed INTEGER NOT NULL DEFAULT 0
    );
    """)

    # 6. Hadiths Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS hadiths (
        id INTEGER PRIMARY KEY NOT NULL,
        collection_id TEXT NOT NULL,
        hadith_number INTEGER NOT NULL,
        title TEXT NOT NULL,
        text_arabic TEXT NOT NULL,
        translation_id TEXT NOT NULL,
        translation_en TEXT NOT NULL,
        reference TEXT NOT NULL
    );
    """)

    # 7. Knowledge Chunks Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS knowledge_chunks (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        source_type TEXT NOT NULL,
        source_id TEXT NOT NULL,
        title TEXT NOT NULL,
        text_content TEXT NOT NULL,
        embedding BLOB
    );
    """)

    # 8. Chat Messages Table
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS chat_messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        conversation_id TEXT NOT NULL,
        role TEXT NOT NULL,
        content TEXT NOT NULL,
        citations_json TEXT,
        timestamp INTEGER NOT NULL
    );
    """)

    # Insert Data into Tables
    print("  [INSERT] Inserting Surahs...")
    cursor.executemany("""
    INSERT INTO surahs (id, number, name_arabic, name_latin, name_english, revelation_type, ayah_count)
    VALUES (:id, :number, :name_arabic, :name_latin, :name_english, :revelation_type, :ayah_count);
    """, surahs)

    print("  [INSERT] Inserting Ayahs...")
    cursor.executemany("""
    INSERT INTO ayahs (id, surah_id, ayah_number, text_arabic, transliteration, translation_id, translation_en, juz, page, tajwid_tags)
    VALUES (:id, :surah_id, :ayah_number, :text_arabic, :transliteration, :translation_id, :translation_en, :juz, :page, :tajwid_tags);
    """, ayahs)

    print("  [INSERT] Inserting Hadiths...")
    cursor.executemany("""
    INSERT INTO hadiths (id, collection_id, hadith_number, title, text_arabic, translation_id, translation_en, reference)
    VALUES (:id, :collection_id, :hadith_number, :title, :text_arabic, :translation_id, :translation_en, :reference);
    """, hadiths)

    print("  [INSERT] Inserting Tahsin Lessons...")
    cursor.executemany("""
    INSERT INTO tahsin_lessons (id, category, subcategory, title, letter_arabic, letter_latin, description, articulation_point, audio_sample, example_ayah_text, example_ayah_ref, rule_type, order_index, is_completed)
    VALUES (:id, :category, :subcategory, :title, :letter_arabic, :letter_latin, :description, :articulation_point, :audio_sample, :example_ayah_text, :example_ayah_ref, :rule_type, :order_index, :is_completed);
    """, tahsin)

    print("  [INSERT] Inserting Knowledge Chunks...")
    cursor.executemany("""
    INSERT INTO knowledge_chunks (id, source_type, source_id, title, text_content, embedding)
    VALUES (:id, :source_type, :source_id, :title, :text_content, :embedding);
    """, chunks)

    # 9. Ayahs FTS Table (Room-compatible standalone FTS4 virtual table)
    print("  [INDEX] Creating Ayahs FTS virtual table...")
    cursor.execute("""
    CREATE VIRTUAL TABLE IF NOT EXISTS ayahs_fts USING fts4(
        translation_id,
        translation_en,
        transliteration,
        text_arabic
    );
    """)
    cursor.execute("""
    INSERT INTO ayahs_fts (docid, translation_id, translation_en, transliteration, text_arabic)
    SELECT id, translation_id, translation_en, transliteration, text_arabic FROM ayahs;
    """)

    # Create helpful query indexes
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_ayahs_surah ON ayahs(surah_id, ayah_number);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_ayahs_juz ON ayahs(juz);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_ayahs_page ON ayahs(page);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_hadiths_col ON hadiths(collection_id, hadith_number);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_tahsin_cat ON tahsin_lessons(category, order_index);")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_chunks_src ON knowledge_chunks(source_type);")

    conn.commit()

    # VACUUM and optimize
    print("  [OPTIMIZE] Running VACUUM & PRAGMA optimize...")
    cursor.execute("PRAGMA optimize;")
    conn.commit()
    conn.close()

    print(f"  [SUCCESS] Database built at: {DB_FILE} ({os.path.getsize(DB_FILE):,} bytes)")

def verify_database():
    """Verify data integrity, row counts, and test queries."""
    print("\n--- 7. Verifying Database Integrity & Queries ---")
    conn = sqlite3.connect(str(DB_FILE))
    cursor = conn.cursor()

    # Integrity Check
    cursor.execute("PRAGMA integrity_check;")
    check = cursor.fetchone()[0]
    print(f"  PRAGMA integrity_check: {check}")
    assert check == "ok", f"Integrity check failed: {check}"

    # Verify Table Counts
    counts = {}
    for table in ["surahs", "ayahs", "hadiths", "tahsin_lessons", "knowledge_chunks"]:
        cursor.execute(f"SELECT COUNT(*) FROM {table};")
        cnt = cursor.fetchone()[0]
        counts[table] = cnt
        print(f"  Table '{table}': {cnt:,} records")

    assert counts["surahs"] == 114, f"Expected 114 surahs, got {counts['surahs']}"
    assert counts["ayahs"] == 6236, f"Expected 6236 ayahs, got {counts['ayahs']}"
    assert counts["tahsin_lessons"] >= 50, f"Expected >=50 tahsin lessons, got {counts['tahsin_lessons']}"
    assert counts["hadiths"] >= 46, f"Expected >=46 hadiths, got {counts['hadiths']}"
    assert counts["knowledge_chunks"] >= 100, f"Expected >=100 chunks, got {counts['knowledge_chunks']}"

    # Test FTS Search
    print("\n  [TEST QUERY] FTS Search on Indonesian translation ('rezeki'):")
    cursor.execute("""
    SELECT ayahs.id, surahs.name_latin, ayahs.ayah_number, ayahs.translation_id
    FROM ayahs
    JOIN surahs ON ayahs.surah_id = surahs.number
    JOIN ayahs_fts ON ayahs.id = ayahs_fts.rowid
    WHERE ayahs_fts MATCH ?
    LIMIT 3;
    """, ("rezeki*",))
    fts_results = cursor.fetchall()
    for r in fts_results:
        print(f"    - QS. {r[1]} [{r[2]}]: {r[3][:60]}...")

    # Test Arabic Text & Tajwid Tags for Al-Fatihah 1
    cursor.execute("SELECT text_arabic, tajwid_tags, translation_id, transliteration FROM ayahs WHERE id = 1;")
    sample = cursor.fetchone()
    print("\n  [SAMPLE] Ayah 1:1:")
    print(f"    Arabic       : {sample[0]}")
    print(f"    Tajwid Tags  : {sample[1]}")
    print(f"    Translation  : {sample[2]}")
    print(f"    Translit     : {sample[3]}")

    # Test Tahsin Categories
    cursor.execute("SELECT category, COUNT(*) FROM tahsin_lessons GROUP BY category;")
    tahsin_cats = cursor.fetchall()
    print("\n  [TAHSIN CATEGORIES]")
    for cat, count in tahsin_cats:
        print(f"    - {cat}: {count} lessons")

    conn.close()
    print("\n  [ALL VERIFICATIONS PASSED SUCCESSFULLY!]")

def main():
    t_start = time.time()
    print("===================================================================")
    print("           Quran Plus — Authentic Database Builder                 ")
    print("===================================================================")
    
    surahs, ayahs = build_quran_dataset()
    hadiths = build_hadith_dataset()
    tahsin = build_tahsin_dataset()
    chunks = build_knowledge_chunks(surahs, hadiths, tahsin)
    
    export_json_seeds(surahs, ayahs, hadiths, tahsin, chunks)
    initialize_and_populate_sqlite(surahs, ayahs, hadiths, tahsin, chunks)
    verify_database()
    
    print(f"\nTotal elapsed time: {time.time() - t_start:.2f} seconds.")
    print("Database build completed successfully!")

if __name__ == "__main__":
    main()
