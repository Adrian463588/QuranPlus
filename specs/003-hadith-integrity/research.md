# Research & Audit — Hadith Translation Integrity (Nasai 5111 / 5022 & Semantic Corpus Scan)

## 1. BUG-S3-HADITH-TRANSLATION-001 Verification (`nasai:5111` / `archive 5022`)

- **Collection:** Sunan an-Nasa'i (`nasai`, Kitab 48: *The Book of Adornment*, Hadith 72).
- **Canonical Reference:** `https://sunnah.com/nasai:5111` (`stable_id = "sunnah:nasai:5111"`, Room `id = 5105117`).
- **Archive / Al-Alamiyah Reference:** `archive_gading = 5022` (verified via `numbering.json` & `gadingnst/nasai.json`).
- **Arabic Sanad & Matn Verified Against `sunnah.com/nasai:5111`:**
  `أَخْبَرَنَا أَحْمَدُ بْنُ عَمْرِو بْنِ السَّرْحِ، قَالَ حَدَّثَنَا ابْنُ وَهْبٍ، قَالَ أَخْبَرَنِي اللَّيْثُ، عَنْ يَزِيدَ بْنِ أَبِي حَبِيبٍ، عَنْ أَبِي الْحُصَيْنِ الْحِمْيَرِيِّ، عَنْ أَبِي رَيْحَانَةَ، قَالَ بَلَغَنَا أَنَّ رَسُولَ اللَّهِ صلى الله عليه وسلم نَهَى عَنِ الْوَشْرِ وَالْوَشْمِ ‏.‏`
  - Canonical `fawazahmed0` Arabic already preserves the space `قَالَ بَلَغَنَا` (SHA-256 `1c6f93681d2df2adbeb21633379d744eb50b20c91c2716bcc6101af65e75d304`).
  - Legacy `gadingnst/hadith-api` raw `#5022` (`SHA-256 75695d5624bc24137dd39f35d9d54b5f11552d159fac4edad5c74138c3d07e18`) contains the reported missing-space defect `قَالَبَلَغَنَا` — handled deterministically by `HadithContentCorrector` when SAF/legacy `gadingnst` records are processed.
- **English Parallel (`fawazahmed0`):** `"It was narrated that Abu Raihanah said:\"We heard that the Messenger of Allah [SAW] forbade filing (the teeth) and tattoos"` (matches `sunnah.com/nasai:5111`).
- **Indonesian Upstream Defect (`gadingnst #5022` & `fawazahmed0 ind-nasai #5111`):**
  - Original ID text end: `...melarang mengkikir gigi dan mencabut bulu alis."` (`db7a8841c6b5a2e497da0a995cc671e67962133a5d1012f28f51e9fb17fa7393`; `fawaz` without trailing period: `f71914c87ea21fdff1a0c3b926d4f2525359d8e71bb1a6f5d2c3f08e09f176df`).
  - Mistranslated `وَالْوَشْمِ` (*tattoos / membuat tato*) as `"mencabut bulu alis"` (*plucking eyebrows / النمص*).
- **Corrected Target Translation (Minimal Sanad-Preserving Edit):**
  `Telah mengabarkan kepada kami [Ahmad bin Amru bin As Sarh] ia berkata; telah menceritakan kepada kami [Ibnu Wahb] ia berkata; telah mengabarkan kepadaku [Al Laits] dari [Yazid bin Abu Habib] dari [Abu Al Hushain Al Himyari] dari [Abu Raihanah] ia berkata, "Telah sampai kabar kepada kami bahwa Rasulullah shallallahu 'alaihi wasallam melarang mengkikir gigi dan membuat tato."`
- **Scholarly Grading Verified (`grades`):**
  - `Abu Ghuddah`: `Sahih`
  - `Al-Albani`: `Sahih`
  - `Zubair Ali Zai`: `Daif` (Darussalam grading; reflected in chain note `قَالَ بَلَغَنَا` *inqitha'* — preserved separately by authority without collapsing).

## 2. Twin Defect Discovered During Semantic Audit (`nasai:5112` / `archive 5023`)

- **Record:** `sunnah:nasai:5112` (`archive_gading = 5023`, `id = 5105118`, `Kitab 48, Hadis 73`).
- **Sanad & Matn:** Narrated via `[Qutaibah]` -> `[Al Laits]` -> `[Yazid bin Abu Habib]` -> `[Abu Al Hushain Al Himyari]` -> `[Abu Raihanah]`, ending with the exact same matn `قَالَ بَلَغَنَا أَنَّ رَسُولَ اللَّهِ صلى الله عليه وسلم نَهَى عَنِ الْوَشْرِ وَالْوَشْمِ ‏.‏` (Arabic SHA-256 `314f22d7d3ad1b0546239a9a227a9e5a3d95ff9ec92650c777e5461931cab131`).
- **Indonesian Defect:** Identical upstream Lidwa error (`3c2a4cd853097d7ca14572ec237606daf182bfee5e362c2b9670f09bb3bb151c`; `fawaz`: `fd821c142461cb6d1138169adf26ce93dbf6d4f5469c96e8dc0837eb92aa0b47`) ending in `"mengkikir gigi dan mencabut bulu alis."`.
- **Legacy `gadingnst #5023`:** Also contains `قَالَبَلَغَنَا` concatenation (`1d2d4710bdc7d57c18cb77ce3ba2fbb472b4857649cfb40b063fa3a918770f69`).
- **Verification:** Confirmed against `https://sunnah.com/nasai:5112` (`"We heard that the Messenger of Allah (ﷺ) forbade filing (the teeth) and tattoos"`).

## 3. Related Supporting & Comparative References Verified Preserved

- **`nasai:5110` (`archive 5021`):** Contains 3 prohibitions `الْوَشْرَ وَالْوَشْمَ وَالنَّتْفَ` (`mengikir gigi, membuat tato dan mencabut bulu alis`). Already translates `الْوَشْمَ` as `membuat tato` (`النَّتْفَ` -> *plucking hairs* flagged as `NEEDS_REVIEW` editorial precision note without unverified rewrite).
- **`nasai:5109` (`archive 5020`), `nasai:5107`, `nasai:5108`, `bukhari:5931`, `bukhari:5939`, `bukhari:5943`, `ahmad:3759`, `ahmad:3760`, `ahmad:4114`, `ahmad:4202`:** Explicitly contain `الْمُتَنَمِّصَاتِ` alongside `الْمُوتَشِمَاتِ`/`الْوَاشِمَاتِ`/`الْمُوشِمَاتِ`. Preserved untouched (`VERIFIED_CORRECT`).
- **`bukhari:5931` & `muslim:2125`:** Checked as supporting references; neither is substituted onto `nasai:5111`/`5112`.
- **`bukhari:5541`, `muslim:2116.01`, `muslim:2117`:** Animal face branding (`الْوَسْمِ` / `الصورة`) loosely rendered with `"tato"` in Lidwa; flagged `NEEDS_REVIEW` in `semantic-audit-report.json` rather than auto-mutated.
- **`bukhari:5933`, `muslim:2125.02`:** English-only `fawazahmed0` variants lacking Indonesian upstream alignment; classified `SOURCE_UNAVAILABLE` for Indonesian (surface honest UI unavailable notice).

## 4. BUG-S3-HADITH-AHMAD-4302 Verification & Crosswalk Audit (`Musnad Ahmad 4302`)

- **Why Legacy Quran Plus Failed on `ahmad:4302`:**
  - The legacy `gadingnst/hadith-api` `books/ahmad.json` archive contained **4,305 sparse records** across raw IDs `1..26363` (last 10: `26353..26363`). Numbers `100, 1000, 2000, 3000, 3999, 4000, 4001, 4002, 4299..4305` were completely absent from `gadingnst/ahmad.json` even though the UI displayed `"4305 hadist"`.
- **Canonical Verified Multi-Edition Identity for `Musnad Ahmad 4302`:**
  - **Default Edition — Mu'assasat al-Risalah (`ar_risalah:4302`, `https://hadits.net/ahmad:4302` -> `mit:ahmad:4075`, Al-Alamiyah `#4075`):**
    - Sanad/Matn: Narrated by Yahya bin Zakariya -> Isma'il -> Qais -> Ibn Mas'ud: `كُنَّا مَعَ رَسُولِ اللَّهِ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ لَيْسَ لَنَا نِسَاءٌ... أَلَا نَسْتَخْصِي` (QS Al-Ma'idah: 87).
  - **Al-Alamiyah / Archive Edition (`archive_gading:4302` -> `mit:ahmad:4302`, Mu'assasat al-Risalah `#4531`):**
    - Sanad/Matn: `حَدَّثَنَا عَبْدُ الرَّحْمَنِ حَدَّثَنَا مَالِكٌ عَنْ نَافِعٍ عَنِ ابْنِ عُمَرَ أَنَّ النَّبِيَّ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَهَى عَنْ تَلَقِّي السِّلَعِ حَتَّى يُهْبَطَ بِهَا الْأَسْوَاقُ وَنَهَى عَنْ النَّجْشِ وَقَالَ لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ`
    - Indonesian (`irsyadulibad`, MIT): `Telah menceritakan kepada kami [Abdurrahman] telah menceritakan kepada kami [Malik] dari [Nafi'] dari [Ibnu Umar], bahwa Nabi shallallahu 'alaihi wasallam melarang menghentikan barang dagangan hingga diturunkan di pasar, beliau juga melarang najasy (menambah harga untuk menipu yang lain), beliau bersabda: "Janganlah sebagian kalian menjual di atas penjualan sebagian lain."`
    - Confirmed exact match against candidate Arabic matn (`لَا يَبِعْ بَعْضُكُمْ عَلَى بَيْعِ بَعْضٍ`), distinct from Bukhari/Muslim/Tirmidhi/Nasai parallel narrations and distinct from Mu'assasat al-Risalah `#4302` (never silently substituted).
- **Crosswalk Defect Diagnosed & Repaired in `build_hadith_corpus.py` (`+mit9+corr2`):**
  - Prior n-gram window (`pos < 14`) truncated short Malik/Nafi/Ibn Umar hadiths before their distinguishing matn (`الشغار`, `المزابنة`), and asymmetric `_verify_pair` (`min(len)`) mis-linked `ar_risalah:4526` (`نهى عن الشغار`, actually `mit:ahmad:4297`) onto `mit:ahmad:4302` alongside `4531`, `ar_risalah:4528` (`المزابنة`, actually `mit:ahmad:4299`) onto `mit:ahmad:4300`, and `ar_risalah:4532` onto a distant duplicate (`4890` instead of `4304`).
- Expanding indexed 6-grams up to token 40, enforcing symmetric Dice + matn-tail verification, bounding Pass 1 positional window (`±2200`), and adding 2 monotonic bracket-refinement passes repairs all local alignments (`4526->4297`, `4527->4298`, `4528->4299`, `4529->4300`, `4530->4301`, `4531->4302`, `4532->4304`, `4533->4305`), cleanly removing spurious `4526` from `mit:ahmad:4302` while maintaining full 26,363-record coverage.

## 5. BUG-S3-HADITH-DARIMI-2841 Verification (`Sunan al-Darimi 2841` & Multi-Collection Integrity)

- **Why Legacy Quran Plus Failed on `darimi:2841` (Tablet Screenshot `2949 hadist`):**
  - Inspection of `scripts/cache/hadith_audit/hadith-indonesia-bundle.zip` (`books/darimi.json` from `gadingnst/hadith-api`, MIT) confirmed the legacy bundle held only **2,949 sparse records** across `1..3,367` (`418` missing slots), explicitly omitting `#500`, `#2839`, `#2840`, `#2841`, and `#2975`.
  - Candidate URL checks confirmed `tafsirq.com/hadits/darimi/2841` serves the TafsirQ homepage (no direct Darimi record), `api.hadith.gading.dev` is unavailable via TLS, `fawazahmed0/hadith-api` (`editions.min.json`) does not contain `darimi`, and direct `sunnah.com/darimi:2841` scraping returned `HTTP 403`. No canonical Sunnah.com or Darul Mughni/Husain Salim Asad per-hadith crosswalk is verified or bundled for `darimi`, so no `sunnah.com/darimi:*` URLs may be fabricated.
- **Verified Local Complete Corpus (`irsyadulibad/hadits-database`, MIT, Al-Alamiyah `1..3,367`):**
  - Bundled `collections/darimi.json` contains **3,367 / 3,367** complete verified Arabic + Indonesian records (`min=1`, `max=3,367`, `0` missing slots, `default_scheme = archive_gading`, `canonical_url = null`).
  - **`mit:darimi:2841` (`archive_gading: 2841`) — Matches Candidate Record A Verbatim:**
    - Arabic: `أَخْبَرَنَا مُحَمَّدُ بْنُ الْعَلَاءِ حَدَّثَنَا يَحْيَى بْنُ أَبِي بُكَيْرٍ حَدَّثَنَا إِبْرَاهِيمُ بْنُ طَهْمَانَ عَنْ سِمَاكِ بْنِ حَرْبٍ عَنْ عِكْرِمَةَ عَنْ ابْنِ عَبَّاسٍ أَنَّ قَوْمًا اخْتَصَمُوا إِلَى عَلِيٍّ رَضِيَ اللَّهُ عَنْهُ فِي وَلَدِ الْمُتَلَاعِنَيْنِ فَجَاءَ عَصَبَةُ أَبِيهِ يَطْلُبُونَ مِيرَاثَهُ فَقَالَ إِنَّ أَبَاهُ كَانَ تَبَرَّأَ مِنْهُ فَلَيْسَ لَكُمْ مِنْ مِيرَاثِهِ شَيْءٌ فَقَضَى بِمِيرَاثِهِ لِأُمِّهِ وَجَعَلَهَا عَصَبَتَهُ`
    - Indonesian (`irsyadulibad`, MIT): `Telah mengabarkan kepada kami [Muhammad bin Al 'Ala`] ... bahwa ada suatu kaum (beberapa orang) yang mengadukan masalah kepada [Ali] radlyallahu 'anhu tentang warisan anak seorang wanita yang melakukan li'an ... Lalu ia memutuskan bahwa harta warisan anak itu untuk ibunya dan menjadikan ibu sebagai ashabah.`
    - **Classification & Speaker Attribution:** **Atsar Mauquf** attributed explicitly to **Ali bin Abi Talib (رضي الله عنه)** (NOT prophetic speech of Muhammad ﷺ). Grading (`Hasan` attributed to Husain Salim Asad) and external edition numbers (`3023`, `3027`) remain **UNVERIFIED / UNMAPPED** against the local Al-Alamiyah SQL archive and are NOT attached without verified machine-readable edition proof (`mit:darimi:3023` and `mit:darimi:3027` in `archive_gading` are distinct narrations on *wala'*).
  - **`mit:darimi:2805` (`archive_gading: 2805`) — Matches Candidate Record B Verbatim:**
    - `أَطْعَمَ جَدَّةً سُدُسًا` (Hadis Marfu' to Prophet ﷺ regarding grandmother's 1/6 inheritance share); distinct from Darul Mughni `#2975` claim (`archive_gading:2975` is *walad az-zina*).
- **Code Defects Diagnosed in Shared Resolver / RAG Pipeline:**
  - `HadithReferenceRequest.kt`: `aliases["darimi"]` missed `"Sunan al-Darimi"` / `"Musnad ad-Darimi"`, `.url` returned `"https://sunnah.com/darimi:<num>"` instead of `null`, and `matches()` rejected local `(Al-Alamiyah)` / `targetSchemeId() == "archive_gading"` Darimi citations when `archive = false` (`"Hadis Sunan al-Darimi nomor 2841"`).
  - `RagPipeline.kt`: Rule 8 tightened to prohibit misattributing Sahabi/tabi'in/atsar Mauquf narrations as prophetic speech.
