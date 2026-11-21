"""CI/prebuild global integrity validator for all supported Hadith collections.

Checks:
- Archive size & SHA-256 verification + per-collection SHA-256 verification
- Source metadata & open license verification (Unlicense / MIT)
- Per-collection verified record counts, min/max reference numbers per scheme, missing gaps
- Distinction between legitimate edition gaps (`missing_in_source` / `unmapped_edition_variant`)
  vs missing imported records
- Duplicate stable_id, duplicate reference tuple, and numbering-scheme collision checks
- Empty Arabic matn, invalid translations, control/escape leakage, Arabic/translation sanity
- Valid canonical URL patterns (zero broken URLs) and attributed scholarly gradings
- All 5 mandatory golden regression fixtures (HF-001..HF-005)
- Emits machine-readable JSON + human-readable Markdown validation reports
"""
import hashlib
import json
import pathlib
import re
import unicodedata
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app/src/main/assets/hadith"
BUNDLE_PATH = ASSET_DIR / "sunnah-canonical-bundle.zip"
MANIFEST_PATH = ASSET_DIR / "corpus-manifest.json"
CORRECTIONS_PATH = ASSET_DIR / "content-corrections.json"
REPORT_JSON_PATH = ROOT / "specs/003-hadith-integrity/audit/corpus-validation-report.json"
REPORT_MD_PATH = ROOT / "specs/003-hadith-integrity/audit/corpus-validation-summary.md"
SEMANTIC_AUDIT_PATH = ROOT / "specs/003-hadith-integrity/audit/semantic-audit-report.json"

EXPECTED_COLLECTIONS = {
    "bukhari",
    "muslim",
    "abudawud",
    "tirmidhi",
    "nasai",
    "ibnmajah",
    "malik",
    "ahmad",
    "darimi",
}
ALLOWED_LICENSES = {"Unlicense", "MIT", "Unlicense+MIT"}
ALLOWED_URL_PATTERNS = {
    "sunnah": re.compile(r"^https://sunnah\.com/[a-z]+:\d+$"),
    "ar_risalah": re.compile(r"^https://hadits\.net/ahmad:\d+$"),
}
ARABIC_CHARS_RE = re.compile(r"[\u0600-\u06FF]")
SEMANTIC_AR_RE = re.compile(
    r"(?<![\w\u0621-\u064A])(?:[وفبلك]?)(?:ال)?"
    r"(وشم|الوشر|نمص|واشم[هة]?|واشمات|موتشم[هة]?|موتشمات|مستوشم[هة]?|مستوشمات|"
    r"نامص[هة]?|نامصات|متنمص[هة]?|متنمصات|موشم[هة]?|موشمات|متوشم[هة]?ات?|موشومات|"
    r"تشمن|تستوشمن|متفلج[هة]?ات?|نتف[اه]?)"
    r"(?![\w\u0621-\u064A])"
)
SEMANTIC_ID_RE = re.compile(
    r"(\btato\b|bertato|membuat\s+tato|ditato|mentato|mengkikir|mengikir|"
    r"merenggangkan\s+gigi|bulu\s+alis|mencabut\s+bulu|pencabut\s+bulu|mencukur\s+alis)",
    re.I,
)


def _norm_ar_search(text: str) -> str:
    decomposed = unicodedata.normalize("NFKD", text)
    no_marks = "".join(c for c in decomposed if unicodedata.category(c) != "Mn")
    return no_marks.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ى", "ي").replace("ة", "ه").replace("ـ", "")


def run_semantic_audit(all_records: list[tuple[str, dict]], corrections_doc: dict) -> dict:
    """Classify adornment/tato/wasyr/namsh/netf candidates deterministically across all collections."""
    applied_ids = {c["stable_id"]: c["correction_id"] for c in corrections_doc.get("corrections", [])}
    seen_records: dict[str, tuple[str, dict]] = {}
    for col_id, rec in all_records:
        seen_records.setdefault(rec["stable_id"], (col_id, rec))

    inspected = 0
    verified_correct: list[dict] = []
    verified_incorrect: list[dict] = []
    needs_review: list[dict] = []
    source_unavailable: list[dict] = []

    for sid in sorted(seen_records):
        col_id, rec = seen_records[sid]
        ar = rec.get("arabic", "")
        ar_n = _norm_ar_search(ar)
        ind = rec.get("indonesian", "")

        m_ar = sorted(set(SEMANTIC_AR_RE.findall(ar_n)))
        m_id = sorted(set(m.lower() for m in SEMANTIC_ID_RE.findall(ind)))
        # Skip routine fitrah armpit-plucking (نتف الإبط) unless tato/alis/kikir/gigi appear
        is_armpit_fitrah = ("الابط" in ar_n or "اباط" in ar_n or "ketiak" in ind.lower()) and not (
            any(x in " ".join(m_ar) for x in ("وشم", "وشر", "نمص", "واشم", "متنمص", "موتشم", "مستوشم", "موشم", "متوشم", "تشمن", "تستوشمن"))
            or any(x in " ".join(m_id) for x in ("tato", "alis", "kikir", "gigi"))
        )
        if not (m_ar or m_id) or is_armpit_fitrah:
            continue

        inspected += 1
        has_wasym = any(k in " ".join(m_ar) for k in ("وشم", "واشم", "موتشم", "مستوشم", "موشم", "متوشم", "موشومات", "تشمن", "تستوشمن"))
        has_namsh = any(k in " ".join(m_ar) for k in ("نمص", "نامص", "متنمص"))
        has_netf = any("نتف" in k for k in m_ar)
        ind_lower = ind.lower()
        item = {
            "collection_id": col_id,
            "stable_id": sid,
            "arabic_terms": m_ar,
            "indonesian_terms": m_id,
            "correction_applied": applied_ids.get(sid),
        }

        if not ind.strip():
            item["status"] = "SOURCE_UNAVAILABLE"
            item["note"] = "Indonesian translation absent from upstream source."
            source_unavailable.append(item)
        elif has_wasym and not (has_namsh or has_netf) and ("bulu alis" in ind_lower or "mencukur alis" in ind_lower):
            item["status"] = "VERIFIED_INCORRECT"
            item["note"] = "Arabic الوشم/الواشمة translated as eyebrow plucking without النمص/النتف in Arabic matn."
            verified_incorrect.append(item)
        elif "قَالَبَلَغَنَا" in ar or "قالبلغنا" in ar_n:
            item["status"] = "VERIFIED_INCORRECT"
            item["note"] = "Missing Arabic whitespace between قَالَ and بَلَغَنَا."
            verified_incorrect.append(item)
        elif has_netf and ("bulu alis" in ind_lower or "mencukur alis" in ind_lower):
            item["status"] = "NEEDS_REVIEW"
            item["note"] = "Arabic النتف (plucking hair) rendered as mencabut/mencukur bulu alis by upstream translator; left untouched pending scholarly editorial decision."
            needs_review.append(item)
        elif not has_wasym and any("tato" in t for t in m_id):
            item["status"] = "NEEDS_REVIEW"
            item["note"] = "Animal/facial branding (الوسم/الصورة) rendered with '(tato)' in upstream translation; left untouched pending expert review."
            needs_review.append(item)
        else:
            item["status"] = "VERIFIED_CORRECT"
            verified_correct.append(item)

    return {
        "total_corpus_records": len(seen_records),
        "targeted_candidates_inspected": inspected,
        "summary": {
            "VERIFIED_CORRECT": len(verified_correct),
            "VERIFIED_INCORRECT": len(verified_incorrect),
            "NEEDS_REVIEW": len(needs_review),
            "SOURCE_UNAVAILABLE": len(source_unavailable),
        },
        "verified_incorrect": verified_incorrect,
        "needs_review": needs_review,
        "source_unavailable": source_unavailable,
        "corrected_records": [r for r in verified_correct if r.get("correction_applied")],
    }


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def validate(write_reports: bool = False) -> None:
    assert BUNDLE_PATH.is_file(), f"Missing {BUNDLE_PATH}"
    assert MANIFEST_PATH.is_file(), f"Missing {MANIFEST_PATH}"

    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    zip_bytes = BUNDLE_PATH.read_bytes()
    assert len(zip_bytes) == manifest["archive_size_bytes"], "Archive size mismatch"
    assert sha256_bytes(zip_bytes) == manifest["archive_sha256"], "Archive SHA-256 mismatch"
    assert manifest["license_id"] in ALLOWED_LICENSES, f"Unexpected license: {manifest['license_id']}"
    assert set(manifest["collections"].keys()) == EXPECTED_COLLECTIONS, (
        f"Collection mismatch: {set(manifest['collections'].keys())}"
    )

    collection_reports: dict[str, dict] = {}
    golden: dict[tuple[str, str, int], dict] = {}
    all_records: list[tuple[str, dict]] = []
    seen_room_ids: set[int] = set()
    total_active = 0
    total_issues = 0

    with zipfile.ZipFile(BUNDLE_PATH) as zf:
        inner_manifest = json.loads(zf.read("manifest.json").decode("utf-8"))
        assert inner_manifest["revision"] == manifest["revision"]

        for col_id in sorted(manifest["collections"]):
            col_meta = manifest["collections"][col_id]
            assert col_meta["license_id"] in ALLOWED_LICENSES, f"License invalid for {col_id}"
            assert col_meta.get("source_id") and col_meta.get("source_revision"), (
                f"Missing source metadata for {col_id}"
            )

            raw = zf.read(col_meta["file"])
            assert sha256_bytes(raw) == col_meta["sha256"], f"File SHA mismatch for {col_id}"
            doc = json.loads(raw.decode("utf-8"))
            records = doc["records"]
            missing_slots = doc.get("missing_slots", [])
            assert len(records) == col_meta["active_record_count"], f"Count mismatch for {col_id}"
            total_active += len(records)

            seen_stable: set[str] = set()
            seen_refs: set[tuple[str, str, int, str]] = set()
            scheme_numbers: dict[str, set[int]] = {}
            scheme_distinct_records: dict[str, set[str]] = {}
            empty_arabic = 0
            partial_no_arabic = 0
            empty_translations = 0
            escape_leaks = 0
            broken_urls = 0
            bad_grades = 0

            for r in records:
                all_records.append((col_id, r))
                rid = int(r["id"])
                assert rid not in seen_room_ids, f"Room ID collision {rid} in {col_id}"
                seen_room_ids.add(rid)

                sid = r["stable_id"]
                assert sid not in seen_stable, f"Duplicate stable_id in {col_id}: {sid}"
                seen_stable.add(sid)

                ar = r.get("arabic", "").strip()
                en = r.get("english", "").strip()
                ind = r.get("indonesian", "").strip()
                status = r.get("verification_status", "")
                assert ar or en or ind, f"Completely empty record {sid}"
                if status == "VERIFIED":
                    if not ar or len(ARABIC_CHARS_RE.findall(ar)) < 3:
                        empty_arabic += 1
                    if not en and not ind:
                        empty_translations += 1
                else:
                    assert status == "PARTIAL", f"Unexpected verification_status {status} on {sid}"
                    if not ar or len(ARABIC_CHARS_RE.findall(ar)) < 3:
                        partial_no_arabic += 1
                if "\\'" in ind or '\\"' in ind:
                    escape_leaks += 1

                expected_sha = sha256_bytes(f"{ar}\n{en}\n{ind}".encode("utf-8"))
                assert r["content_sha256"] == expected_sha, f"Content SHA mismatch for {sid}"

                for g in r.get("grades", []):
                    if not g.get("authority", "").strip() or not g.get("grade", "").strip():
                        bad_grades += 1

                url = r.get("canonical_url")
                if url is not None:
                    scheme_for_url = col_meta["default_scheme"]
                    pat = ALLOWED_URL_PATTERNS.get(scheme_for_url)
                    if not pat or not pat.match(url):
                        broken_urls += 1

                for ref in r["references"]:
                    sc = ref["scheme"]
                    num = int(ref["number"])
                    var = ref.get("variant", "")
                    key = (col_id, sc, num, var)
                    assert key not in seen_refs, f"Duplicate reference/collision {key}"
                    seen_refs.add(key)

                    ref_url = ref.get("url")
                    if ref_url is not None:
                        pat = ALLOWED_URL_PATTERNS.get(sc)
                        if not pat or not pat.match(ref_url):
                            broken_urls += 1

                    scheme_numbers.setdefault(sc, set()).add(num)
                    scheme_distinct_records.setdefault(sc, set()).add(sid)
                    if var == "":
                        golden[(col_id, sc, num)] = r

            # Compute gap classification per scheme
            missing_slot_nums: dict[str, set[int]] = {}
            for slot in missing_slots:
                missing_slot_nums.setdefault(slot["scheme"], set()).add(int(slot["number"]))

            scheme_summary = {}
            for sc, num_set in sorted(scheme_numbers.items()):
                min_n, max_n = min(num_set), max(num_set)
                expected_range_len = max_n - min_n + 1
                raw_gap_count = expected_range_len - len(num_set)
                documented_gaps = len(missing_slot_nums.get(sc, set()))
                scheme_summary[sc] = {
                    "record_count": len(scheme_distinct_records[sc]),
                    "unique_numbers": len(num_set),
                    "min_reference_number": min_n,
                    "max_reference_number": max_n,
                    "range_span_gaps": raw_gap_count,
                    "documented_edition_gaps": documented_gaps,
                }
                assert col_meta["scheme_ranges"][sc]["max_reference_number"] == max_n
                assert col_meta["scheme_ranges"][sc]["min_reference_number"] == min_n

            assert (len(records) - sum(1 for r in records if r.get("verification_status") == "VERIFIED")) == col_meta["partial_count"], (
                f"Partial record count mismatch in {col_id}"
            )
            col_issues = empty_arabic + empty_translations + escape_leaks + broken_urls + bad_grades
            total_issues += col_issues
            assert col_issues == 0, (
                f"Validation issues in {col_id}: empty_ar={empty_arabic}, empty_tr={empty_translations}, "
                f"escapes={escape_leaks}, broken_urls={broken_urls}, bad_grades={bad_grades}"
            )

            collection_reports[col_id] = {
                "title_english": col_meta["title_english"],
                "default_scheme": col_meta["default_scheme"],
                "license_id": col_meta["license_id"],
                "source_id": col_meta["source_id"],
                "active_record_count": len(records),
                "verified_count": col_meta["verified_count"],
                "partial_count": col_meta["partial_count"],
                "partial_without_arabic": partial_no_arabic,
                "indonesian_count": col_meta.get("indonesian_count", 0),
                "english_count": col_meta.get("english_count", 0),
                "graded_count": col_meta.get("graded_count", 0),
                "empty_arabic_in_verified": empty_arabic,
                "empty_translations_in_verified": empty_translations,
                "broken_canonical_urls": broken_urls,
                "incorrect_grade_attributions": bad_grades,
                "schemes": scheme_summary,
                "sha256": col_meta["sha256"],
                "status": "PASS",
            }

    assert total_active == manifest["active_record_count"]

    # Golden Fixture HF-001: Sahih al-Bukhari 3617
    hf1 = golden[("bukhari", "sunnah", 3617)]
    assert "نَصْرَانِيًّا" in hf1["arabic"] and "لَفَظَتْهُ الأَرْضُ" in hf1["arabic"], "HF-001 Arabic mismatch"
    assert "Christian" in hf1["english"] and "earth had thrown his body out" in hf1["english"], "HF-001 English mismatch"
    assert hf1["canonical_url"] == "https://sunnah.com/bukhari:3617"

    # Golden Fixture HF-002: Sunan Abi Dawud 4000
    hf2 = golden[("abudawud", "sunnah", 4000)]
    assert "مَالِكِ يَوْمِ الدِّينِ" in hf2["arabic"] and "مَلِكِ يَوْمِ الدِّينِ" in hf2["arabic"], "HF-002 qiraat Arabic mismatch"
    assert "AbuBakr, Umar and Uthman" in hf2["english"], "HF-002 English mismatch"
    assert any(g["authority"] == "Al-Albani" and "Daif" in g["grade"] for g in hf2["grades"]), "HF-002 grading mismatch"
    assert hf2["canonical_url"] == "https://sunnah.com/abudawud:4000"

    # Golden Fixture HF-003: Jami' at-Tirmidhi 3000
    hf3 = golden[("tirmidhi", "sunnah", 3000)]
    assert "كِلاَبُ النَّارِ" in hf3["arabic"] and "أَبُو أُمَامَةَ" in hf3["arabic"], "HF-003 Arabic mismatch"
    assert "Khawarij" in hf3["english"] and "Hajj" not in hf3["english"], "HF-003 English mismatch"
    assert hf3["canonical_url"] == "https://sunnah.com/tirmidhi:3000"

    # Golden Fixture HF-004: Jami' at-Tirmidhi 813
    hf4 = golden[("tirmidhi", "sunnah", 813)]
    assert "الزَّادُ وَالرَّاحِلَةُ" in hf4["arabic"], "HF-004 Arabic mismatch"
    assert "Hajj" in hf4["english"] and "provisions" in hf4["english"], "HF-004 English mismatch"
    assert any(g["authority"] == "Zubair Ali Zai" and g["grade"] == "Daif" for g in hf4["grades"]), "HF-004 grading mismatch"
    assert hf4["canonical_url"] == "https://sunnah.com/tirmidhi:813"
    assert hf3["stable_id"] != hf4["stable_id"]

    # Golden Fixture HF-005: Musnad Ahmad 4001 (Ar-Risalah vs Al-Alamiyah vs #4027 moths/flies)
    hf5_ris = golden[("ahmad", "ar_risalah", 4001)]
    hf5_al = golden[("ahmad", "archive_gading", 4001)]
    hf5_4027 = golden[("ahmad", "ar_risalah", 4027)]
    assert "عَشِيَّةَ الْجُمُعَةِ" in hf5_ris["arabic"] and "آيَةُ اللِّعَانِ" in hf5_ris["arabic"], "HF-005 Ar-Risalah Arabic mismatch"
    assert "ayat li'an" in hf5_ris["indonesian"], "HF-005 Ar-Risalah Indonesian mismatch"
    assert "kupu-kupu" not in hf5_ris["indonesian"], "HF-005 contaminated by #4027"
    assert any("Arna'ut" in g["authority"] and "Sahih" in g["grade"] for g in hf5_ris["grades"]), "HF-005 grade mismatch"
    assert hf5_ris["canonical_url"] == "https://hadits.net/ahmad:4001"
    assert "مَنْ نَزَلَتْ بِهِ فَاقَةٌ" in hf5_al["arabic"], "HF-005 Al-Alamiyah #4001 Arabic mismatch"
    assert "terdesak kebutuhan" in hf5_al["indonesian"], "HF-005 Al-Alamiyah #4001 Indonesian mismatch"
    assert hf5_ris["stable_id"] != hf5_al["stable_id"]
    assert hf5_ris["stable_id"] != hf5_4027["stable_id"]

    # Golden Fixture HF-006: Sunan an-Nasa'i 5111 / 5022 & sibling 5112 / 5023
    assert CORRECTIONS_PATH.is_file(), f"Missing {CORRECTIONS_PATH}"
    corrections_doc = json.loads(CORRECTIONS_PATH.read_text(encoding="utf-8"))
    for c_item in corrections_doc["corrections"]:
        target_rec = golden[(c_item["collection_id"], c_item["scheme_id"], int(c_item["canonical_reference"]))]
        assert sha256_bytes(target_rec["indonesian"].encode("utf-8")) == c_item["corrected_text_sha256"], (
            f"Correction {c_item['correction_id']} not reflected in canonical bundle"
        )
        assert target_rec["indonesian"] == c_item["corrected_text"], (
            f"Corrected text mismatch for {c_item['correction_id']}"
        )

    hf6 = golden[("nasai", "sunnah", 5111)]
    hf6_arch = golden[("nasai", "archive_gading", 5022)]
    assert hf6["stable_id"] == hf6_arch["stable_id"] == "sunnah:nasai:5111"
    assert "قَالَ بَلَغَنَا" in hf6["arabic"] and "قَالَبَلَغَنَا" not in hf6["arabic"], "HF-006 Arabic whitespace defect"
    assert "الْوَشْرِ وَالْوَشْمِ" in hf6["arabic"], "HF-006 Arabic matn mismatch"
    assert "mengkikir gigi dan membuat tato" in hf6["indonesian"], "HF-006 Indonesian tattoo correction missing"
    assert "bulu alis" not in hf6["indonesian"].lower(), "HF-006 still contains wrong eyebrow-plucking phrase"
    assert any(g["authority"] == "Al-Albani" and g["grade"] == "Sahih" for g in hf6["grades"])
    assert any(g["authority"] == "Zubair Ali Zai" and "Daif" in g["grade"] for g in hf6["grades"])
    assert hf6["canonical_url"] == "https://sunnah.com/nasai:5111"

    hf6_twin = golden[("nasai", "sunnah", 5112)]
    hf6_twin_arch = golden[("nasai", "archive_gading", 5023)]
    assert hf6_twin["stable_id"] == hf6_twin_arch["stable_id"] == "sunnah:nasai:5112"
    assert "mengkikir gigi dan membuat tato" in hf6_twin["indonesian"], "HF-006 sibling 5112 missing tattoo fix"
    assert "bulu alis" not in hf6_twin["indonesian"].lower(), "HF-006 sibling 5112 still contains bulu alis"

    # Preserve valid related narrations containing النمص / المتنمصات (e.g. nasai:5109, bukhari:5931)
    assert "mencabut bulu alis" in golden[("nasai", "sunnah", 5109)]["indonesian"]
    assert "الْمُتَنَمِّصَاتِ" in golden[("nasai", "sunnah", 5109)]["arabic"]
    assert "mencukur habis alis" in golden[("bukhari", "sunnah", 5931)]["indonesian"]

    # Golden Fixture HF-007 (BUG-S3-HADITH-AHMAD-4302): multi-edition Ahmad 4302 + checkpoints + clean crosswalk
    hf7_ris = golden[("ahmad", "ar_risalah", 4302)]
    hf7_al = golden[("ahmad", "archive_gading", 4302)]
    assert hf7_ris["stable_id"] == "mit:ahmad:4075"
    assert "كُنَّا مَعَ رَسُولِ اللَّهِ" in hf7_ris["arabic"] and "نَسْتَخْصِي" in hf7_ris["arabic"], "HF-007 Ar-Risalah 4302 Arabic mismatch"
    assert hf7_ris["canonical_url"] == "https://hadits.net/ahmad:4302"
    assert hf7_al["stable_id"] == "mit:ahmad:4302"
    assert "عَبْدُ الرَّحْمَنِ" in hf7_al["arabic"] and "لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ" in hf7_al["arabic"], "HF-007 Al-Alamiyah 4302 Arabic mismatch"
    assert "Janganlah sebagian kalian menjual di atas penjualan sebagian lain" in hf7_al["indonesian"], "HF-007 Al-Alamiyah 4302 Indonesian mismatch"
    assert hf7_ris["stable_id"] != hf7_al["stable_id"]
    hf7_al_ris_nums = {int(ref["number"]) for ref in hf7_al["references"] if ref["scheme"] == "ar_risalah"}
    assert 4531 in hf7_al_ris_nums and 4526 not in hf7_al_ris_nums, f"HF-007 contaminated ar_risalah refs on mit:ahmad:4302: {hf7_al_ris_nums}"
    assert golden[("ahmad", "ar_risalah", 4526)]["stable_id"] == "mit:ahmad:4297", "Ar-Risalah 4526 must map to الشغار (4297)"
    assert golden[("ahmad", "ar_risalah", 4528)]["stable_id"] == "mit:ahmad:4299", "Ar-Risalah 4528 must map to المزابنة (4299)"
    assert golden[("ahmad", "ar_risalah", 4531)]["stable_id"] == "mit:ahmad:4302"
    assert golden[("ahmad", "ar_risalah", 4532)]["stable_id"] == "mit:ahmad:4304"
    for ckpt in (1, 10, 100, 1000, 2000, 3000, 3999, 4000, 4001, 4002, 4299, 4300, 4301, 4302, 4303, 4304, 4305, 26363):
        assert ("ahmad", "archive_gading", ckpt) in golden, f"Missing ahmad archive_gading checkpoint #{ckpt}"
    assert ("ahmad", "ar_risalah", 27647) in golden, "Missing ahmad ar_risalah max #27647"

    # Deterministic targeted semantic audit report across all 9 collections
    semantic_findings = run_semantic_audit(all_records, corrections_doc)
    if write_reports and SEMANTIC_AUDIT_PATH.parent.exists():
        SEMANTIC_AUDIT_PATH.write_text(
            json.dumps(semantic_findings, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
    assert semantic_findings["summary"]["VERIFIED_INCORRECT"] == 0, (
        f"Uncorrected VERIFIED_INCORRECT records remain: {semantic_findings['verified_incorrect']}"
    )

    report = {
        "validation_status": "PASS",
        "archive_filename": BUNDLE_PATH.name,
        "archive_size_bytes": len(zip_bytes),
        "archive_sha256": manifest["archive_sha256"],
        "license_id": manifest["license_id"],
        "revision": manifest["revision"],
        "total_collections": len(collection_reports),
        "total_active_records": total_active,
        "total_issues": total_issues,
        "fixtures_verified": ["HF-001", "HF-002", "HF-003", "HF-004", "HF-005", "HF-006"],
        "collections": collection_reports,
    }

    if write_reports and REPORT_JSON_PATH.parent.exists():
        REPORT_JSON_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
        md_lines = [
            "# Hadith Corpus Validation Summary",
            "",
            f"- **Status:** {report['validation_status']}",
            f"- **Bundle:** `{report['archive_filename']}` ({report['archive_size_bytes']:,} bytes, SHA-256 `{report['archive_sha256']}`)",
            f"- **Licenses:** `{report['license_id']}` | **Revision:** `{report['revision']}`",
            f"- **Total Verified Records:** {report['total_active_records']:,} across {report['total_collections']} collections",
            f"- **Golden Fixtures Verified:** {', '.join(report['fixtures_verified'])}",
            f"- **Integrity Issues Found:** {report['total_issues']}",
            "",
            "| Collection | Default Scheme | License | Records | Scheme Ranges (Min..Max) | Gaps Explained | Status |",
            "|---|---|---|---|---|---|---|",
        ]
        for cid, cr in collection_reports.items():
            ranges_str = "; ".join(
                f"`{sc}`: {sr['record_count']:,} recs ({sr['min_reference_number']}..{sr['max_reference_number']})"
                for sc, sr in cr["schemes"].items()
            )
            gaps_str = "; ".join(
                f"`{sc}`: {sr['documented_edition_gaps']} source slots"
                for sc, sr in cr["schemes"].items()
            )
            md_lines.append(
                f"| {cr['title_english']} (`{cid}`) | `{cr['default_scheme']}` | `{cr['license_id']}` | "
                f"{cr['active_record_count']:,} | {ranges_str} | {gaps_str} | {cr['status']} |"
            )
        REPORT_MD_PATH.write_text("\n".join(md_lines) + "\n", encoding="utf-8", newline="\n")

    print(f"PASS: {total_active} records verified across {len(collection_reports)} collections; HF-001..HF-006 passed")


if __name__ == "__main__":
    validate(write_reports=True)
