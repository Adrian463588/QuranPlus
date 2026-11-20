# Data Model — Sprint 3 Hadith Integrity

## Tables (Room v15)

1. **`hadiths`** (additive columns on v14 table)
   - `id INTEGER PRIMARY KEY`
   - `collection_id TEXT`, `hadith_number INTEGER`, `title TEXT`, `text_arabic TEXT`, `translation_id TEXT`, `translation_en TEXT`, `reference TEXT`, `chapter_id TEXT`
   - `source_revision TEXT`, `source_sha256 TEXT`, `license_status TEXT`, `grade TEXT`, `language TEXT`, `is_complete INTEGER`
   - NEW: `stable_id TEXT NOT NULL DEFAULT ''`, `numbering_scheme TEXT NOT NULL DEFAULT 'archive_gading'`, `canonical_url TEXT`, `content_sha256 TEXT NOT NULL DEFAULT ''`, `verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED'`

2. **`hadith_references`**
   - `id INTEGER PRIMARY KEY AUTOINCREMENT`
   - `hadith_id INTEGER NOT NULL`, `stable_id TEXT NOT NULL`, `collection_id TEXT NOT NULL`
   - `scheme_id TEXT NOT NULL` (`"sunnah"`, `"archive_gading"`, `"sunnah_ordinal"`)
   - `reference_number INTEGER NOT NULL`, `variant_suffix TEXT NOT NULL DEFAULT ''`
   - `book_number INTEGER NOT NULL DEFAULT 0`, `hadith_in_book INTEGER NOT NULL DEFAULT 0`
   - `canonical_url TEXT`, `mapping_confidence TEXT NOT NULL`, `review_status TEXT NOT NULL`
   - `UNIQUE(collection_id, scheme_id, reference_number, variant_suffix)`

3. **`hadith_translations`**
   - `id INTEGER PRIMARY KEY AUTOINCREMENT`
   - `hadith_id INTEGER NOT NULL`, `stable_id TEXT NOT NULL`, `language TEXT NOT NULL` (`"ar"`, `"en"`, `"id"`)
   - `text TEXT NOT NULL`, `narrator TEXT NOT NULL DEFAULT ''`, `translator TEXT NOT NULL DEFAULT ''`
   - `source_id TEXT NOT NULL`, `license TEXT NOT NULL`, `sha256 TEXT NOT NULL`
   - `UNIQUE(stable_id, language)`

4. **`hadith_grades`**
   - `id INTEGER PRIMARY KEY AUTOINCREMENT`
   - `hadith_id INTEGER NOT NULL`, `stable_id TEXT NOT NULL`, `authority TEXT NOT NULL`, `grade TEXT NOT NULL`, `source_url TEXT NOT NULL DEFAULT ''`
   - `UNIQUE(stable_id, authority)`

5. **`hadith_source_manifests`**
   - `source_id TEXT PRIMARY KEY`, `source_uri TEXT`, `revision TEXT`, `archive_sha256 TEXT`, `license_id TEXT`, `active_record_count INTEGER`, `missing_slot_count INTEGER`, `status TEXT`, `updated_at_epoch_ms INTEGER`

6. **`hadith_scheme_stats`** *(Room v16)*
   - `collection_id TEXT NOT NULL`, `scheme_id TEXT NOT NULL` — `PRIMARY KEY(collection_id, scheme_id)`
   - `scheme_label TEXT NOT NULL`
   - `record_count INTEGER NOT NULL`
   - `min_reference_number INTEGER NOT NULL`
   - `max_reference_number INTEGER NOT NULL`
   - `missing_slot_count INTEGER NOT NULL DEFAULT 0`
   - `is_default INTEGER NOT NULL DEFAULT 0`
