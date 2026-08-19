package com.quranplus.app.features.gharib.domain

data class GharibReading(
    val id: Int,
    val categoryId: String,
    val categoryTitle: String,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val pageNumber: Int,
    val wordSnippetArabic: String,
    val fullAyahArabic: String,
    val pronunciationGuide: String,
    val writtenVsSpoken: String,
    val explanation: String,
    val ruleType: GharibType
)

enum class GharibType {
    NUN_WIQAYAH,
    AYAT_SAJDAH,
    SIFIR_MUSTADIR,
    SIFIR_MUSTATIL,
    SAKTAH,
    IMALAH,
    ISYMAM,
    TASHIL,
    NAQL
}

object GharibDataRepository {

    val ALL_GHARIB_READINGS: List<GharibReading> = listOf(
        // 1. Imalah
        GharibReading(
            id = 1,
            categoryId = "imalah",
            categoryTitle = "Imalah (إمالة)",
            surahNumber = 11,
            surahName = "Hud",
            ayahNumber = 41,
            pageNumber = 226,
            wordSnippetArabic = "مَجْرٜىٰهَا",
            fullAyahArabic = "وَقَالَ ارْكَبُوا فِيهَا بِسْمِ اللَّهِ مَجْرٜىٰهَا وَمُرْسَاهَا ۚ إِنَّ رَبِّي لَغَفُورٌ رَحِيمٌ",
            pronunciationGuide = "Dibaca: 'Majree-haa' (vokal 'a' dimiringkan condong ke 'e' seperti kata 'sate')",
            writtenVsSpoken = "Tulisan: مَجْرَاهَا ➔ Pelafalan: Majreehaa",
            explanation = "Imalah adalah memiringkan bunyi fathah ke arah kasrah, serta memiringkan alif ke arah ya. Dalam qira'at Imam Ashim riwayat Hafs hanya terdapat pada kata 'Majraha' di QS. Hud: 41.",
            ruleType = GharibType.IMALAH
        ),
        // 2. Isymam
        GharibReading(
            id = 2,
            categoryId = "isymam",
            categoryTitle = "Isymam (إشمام)",
            surahNumber = 12,
            surahName = "Yusuf",
            ayahNumber = 11,
            pageNumber = 236,
            wordSnippetArabic = "لَا تَأْمَ۫نَّا",
            fullAyahArabic = "قَالُوا يَا أَبَانَا مَا لَكَ لَا تَأْمَ۫نَّا عَلَىٰ يُوسُفَ وَإِنَّا لَهُ لَنَاصِحُونَ",
            pronunciationGuide = "Memoncongkan kedua bibir ke depan tanpa bersuara di tengah-tengah dengung huruf Nun",
            writtenVsSpoken = "Asal Kata: لَا تَأْمَنُنَا ➔ Isyarat Bibir: Laa Ta'mannaa",
            explanation = "Isymam adalah mengisyaratkan harakat dhammah yang dibuang dengan memoncongkan kedua bibir ke depan tanpa mengeluarkan suara saat melafalkan nun tasydid berdengung.",
            ruleType = GharibType.ISYMAM
        ),
        // 3. Tashil
        GharibReading(
            id = 3,
            categoryId = "tashil",
            categoryTitle = "Tashil (تسهيل)",
            surahNumber = 41,
            surahName = "Fussilat",
            ayahNumber = 44,
            pageNumber = 481,
            wordSnippetArabic = "ءَا۬عْجَمِيٌّ",
            fullAyahArabic = "وَلَوْ جَعَلْنَاهُ قُرْآنًا أَعْجَمِيًّا لَقَالُوا لَوْلَا فُصِّلَتْ آيَاتُهُ ۖ ءَا۬عْجَمِيٌّ وَعَرَبِيٌّ",
            pronunciationGuide = "Hamzah pertama dibaca tahqiq (jelas 'A'), hamzah kedua dibaca tashil (ringan antara hamzah & alif)",
            writtenVsSpoken = "Tulisan: أَأَعْجَمِيٌّ ➔ Pelafalan: A-a'jamiyyun (ringan)",
            explanation = "Tashil adalah membunyikan hamzah kedua dengan suara lunak dan diringankan di antara suara hamzah dan alif tanpa mad.",
            ruleType = GharibType.TASHIL
        ),
        // 4. Naql
        GharibReading(
            id = 4,
            categoryId = "naql",
            categoryTitle = "Naql (نقل)",
            surahNumber = 49,
            surahName = "Al-Hujurat",
            ayahNumber = 11,
            pageNumber = 516,
            wordSnippetArabic = "بِئْسَ الِاسْمُ",
            fullAyahArabic = "وَلَا تَنَابَزُوا بِالْأَلْقَابِ ۖ بِئْسَ الِاسْمُ الْفُسُوقُ بَعْدَ الْإِيمَانِ",
            pronunciationGuide = "Harakat kasrah hamzah dipindahkan ke huruf lam sukun sebelumnya: dibaca 'Bi'salismu'",
            writtenVsSpoken = "Tulisan: بِئْسَ الاِسْمُ ➔ Pelafalan: Bi'sa-lismu",
            explanation = "Naql adalah memindahkan harakat hamzah ke huruf mati sebelumnya, kemudian hamzahnya dihilangkan dalam bacaan.",
            ruleType = GharibType.NAQL
        ),
        // 5. Saktah (4 Ayat)
        GharibReading(
            id = 5,
            categoryId = "saktah",
            categoryTitle = "Saktah 1 (سكتة)",
            surahNumber = 18,
            surahName = "Al-Kahf",
            ayahNumber = 1,
            pageNumber = 293,
            wordSnippetArabic = "عِوَجًا ۜ قَيِّمًا",
            fullAyahArabic = "الْحَمْدُ لِلَّهِ الَّذِي أَنْزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَلْ لَهُ عِوَجًا ۜ * قَيِّمًا لِيُنْذِرَ بَأْسًا",
            pronunciationGuide = "Berhenti sejenak selama 2 harakat pada 'Iwajaa' tanpa bernafas, lalu lanjut 'Qayyimaa'",
            writtenVsSpoken = "Berhenti tanpa nafas ± 2 harakat",
            explanation = "Saktah adalah berhenti sejenak tanpa bernafas dengan niat melanjutkan bacaan. Bertujuan memisahkan makna agar tidak disangka 'Iwajan' menyifati 'Qayyiman'.",
            ruleType = GharibType.SAKTAH
        ),
        GharibReading(
            id = 6,
            categoryId = "saktah",
            categoryTitle = "Saktah 2 (سكتة)",
            surahNumber = 36,
            surahName = "Yasin",
            ayahNumber = 52,
            pageNumber = 443,
            wordSnippetArabic = "مَرْقَدِنَا ۜ هَٰذَا",
            fullAyahArabic = "قَالُوا يَا وَيْلَنَا مَنْ بَعَثَنَا مِنْ مَرْقَدِنَا ۜ هَٰذَا مَا وَعَدَ الرَّحْمَٰنُ وَصَدَقَ الْمُرْسَلُونَ",
            pronunciationGuide = "Berhenti sejenak pada 'Marqadinaa' tanpa bernafas, lalu lanjut 'Haadzaa'",
            writtenVsSpoken = "Berhenti tanpa nafas ± 2 harakat",
            explanation = "Memisahkan ucapan orang kafir 'Marqadina' dengan jawaban orang beriman / malaikat 'Haadzaa'.",
            ruleType = GharibType.SAKTAH
        ),
        GharibReading(
            id = 7,
            categoryId = "saktah",
            categoryTitle = "Saktah 3 (سكتة)",
            surahNumber = 75,
            surahName = "Al-Qiyamah",
            ayahNumber = 27,
            pageNumber = 578,
            wordSnippetArabic = "مَنْ ۜ رَاقٍ",
            fullAyahArabic = "وَقِيلَ مَنْ ۜ رَاقٍ",
            pronunciationGuide = "Berhenti sejenak pada 'Man' tanpa bernafas, lalu lanjut 'Raaq' (nun tidak diidghamkan ke ra)",
            writtenVsSpoken = "Man (jeda) Raaq",
            explanation = "Saktah mencegah terjadinya Idgham Bilaghunnah agar tidak terdengar seperti kata 'Marraaq' (penjual kuah).",
            ruleType = GharibType.SAKTAH
        ),
        GharibReading(
            id = 8,
            categoryId = "saktah",
            categoryTitle = "Saktah 4 (سكتة)",
            surahNumber = 83,
            surahName = "Al-Mutaffifin",
            ayahNumber = 14,
            pageNumber = 588,
            wordSnippetArabic = "كَلَّا ۖ بَلْ ۜ رَانَ",
            fullAyahArabic = "كَلَّا ۖ بَلْ ۜ رَانَ عَلَىٰ قُلُوبِهِمْ مَا كَانُوا يَكْسِبُونَ",
            pronunciationGuide = "Berhenti sejenak pada 'Bal' tanpa bernafas, lalu lanjut 'Raana'",
            writtenVsSpoken = "Bal (jeda) Raana",
            explanation = "Saktah mencegah terjadinya Idgham Lam ke Ra agar tidak terdengar seperti kata 'Barraana'.",
            ruleType = GharibType.SAKTAH
        ),
        // 6. Sifir Mustatil (Bulatan Lonjong)
        GharibReading(
            id = 9,
            categoryId = "sifir_mustatil",
            categoryTitle = "Sifir Mustatil (سفر مستطيل)",
            surahNumber = 109,
            surahName = "Al-Kafirun",
            ayahNumber = 4,
            pageNumber = 603,
            wordSnippetArabic = "أَنَا۟",
            fullAyahArabic = "وَلَا أَنَا۠ عَابِدٌ مَا عَبَدْتُمْ",
            pronunciationGuide = "Jika Waqaf (berhenti): dibaca panjang 2 harakat ('Anaa'). Jika Wasal (lanjut): dibaca pendek ('Ana 'aabidun')",
            writtenVsSpoken = "Waqaf ➔ Anaa (2 harakat) | Wasal ➔ Ana (1 harakat)",
            explanation = "Tanda bulatan lonjong di atas alif. Alif dibaca panjang saat waqaf, namun gugur / dibaca pendek saat disambung (wasal).",
            ruleType = GharibType.SIFIR_MUSTATIL
        ),
        GharibReading(
            id = 10,
            categoryId = "sifir_mustatil",
            categoryTitle = "Sifir Mustatil (سفر مستطيل)",
            surahNumber = 18,
            surahName = "Al-Kahf",
            ayahNumber = 38,
            pageNumber = 298,
            wordSnippetArabic = "لَّٰكِنَّا۟",
            fullAyahArabic = "لَّٰكِنَّا۠ هُوَ اللَّهُ رَبِّي وَلَا أُشْرِكُ بِرَبِّي أَحَدًا",
            pronunciationGuide = "Jika Waqaf: dibaca 'Laakinnaa' (2 harakat). Jika Wasal: dibaca 'Laakinnahuvallah'",
            writtenVsSpoken = "Waqaf ➔ Laakinnaa | Wasal ➔ Laakinnah",
            explanation = "Sifir Mustatil pada kata Laakinnaa (asalnya Laakin Ana).",
            ruleType = GharibType.SIFIR_MUSTATIL
        ),
        // 7. Sifir Mustadir (Bulatan Bulat)
        GharibReading(
            id = 11,
            categoryId = "sifir_mustadir",
            categoryTitle = "Sifir Mustadir (سفر مستدير)",
            surahNumber = 76,
            surahName = "Al-Insan",
            ayahNumber = 4,
            pageNumber = 578,
            wordSnippetArabic = "سَلَٰسِلَا۟",
            fullAyahArabic = "إِنَّا أَعْتَدْنَا لِلْكَافِرِينَ سَلَاسِلَا۟ وَأَغْلَالًا وَسَعِيرًا",
            pronunciationGuide = "Huruf yang bertanda sifir bulat tidak dibaca panjang baik saat waqaf maupun wasal",
            writtenVsSpoken = "Tidak dibaca panjang dalam segala kondisi",
            explanation = "Tanda bulatan bulat sempurna di atas huruf 'Illat (Alif, Waw, Ya). Menandakan huruf tersebut tidak berfungsi sebagai huruf mad.",
            ruleType = GharibType.SIFIR_MUSTADIR
        ),
        // 8. Nun Wiqayah / Nun Wasal
        GharibReading(
            id = 12,
            categoryId = "nun_wiqayah",
            categoryTitle = "Nun Wiqayah / Nun Wasal (نون الوقاية)",
            surahNumber = 2,
            surahName = "Al-Baqarah",
            ayahNumber = 180,
            pageNumber = 27,
            wordSnippetArabic = "خَيْرًا ۨالْوَصِيَّةُ",
            fullAyahArabic = "كُتِبَ عَلَيْكُمْ إِذَا حَضَرَ أَحَدَكُمُ الْمَوْتُ إِنْ تَرَكَ خَيْرًا ۨالْوَصِيَّةُ لِلْوَالِدَيْنِ",
            pronunciationGuide = "Tanwin bertemu Hamzah Wasal dibaca dengan menambahkan nun berharakat kasrah: 'Khayrani-lwashiyyah'",
            writtenVsSpoken = "Khayran + Al-Washiyyah ➔ Khayranil-washiyyah",
            explanation = "Ketika tanwin bertemu dengan hamzah wasal, tanwin dipecah menjadi bunyi nun berharakat kasrah untuk menghindari bertemunya dua sukun.",
            ruleType = GharibType.NUN_WIQAYAH
        ),
        GharibReading(
            id = 13,
            categoryId = "nun_wiqayah",
            categoryTitle = "Nun Wiqayah / Nun Wasal (نون الوقاية)",
            surahNumber = 62,
            surahName = "Al-Jumu'ah",
            ayahNumber = 11,
            pageNumber = 554,
            wordSnippetArabic = "لَهْوًا ۨانْفَضُّوا",
            fullAyahArabic = "وَإِذَا رَأَوْا تِجَارَةً أَوْ لَهْوًا ۨانْفَضُّوا إِلَيْهَا وَتَرَكُوكَ قَائِمًا",
            pronunciationGuide = "Dibaca menyambung: 'Lahwani-nfadh-dhuu'",
            writtenVsSpoken = "Lahwan + Infadh-dhuu ➔ Lahwaninfadh-dhuu",
            explanation = "Nun Wiqayah pada surah Al-Jumu'ah ayat 11.",
            ruleType = GharibType.NUN_WIQAYAH
        ),
        // 9. Ayat-Ayat Sajdah
        GharibReading(
            id = 14,
            categoryId = "ayat_sajdah",
            categoryTitle = "Ayat Sajdah (سجدة التلاوة)",
            surahNumber = 32,
            surahName = "As-Sajdah",
            ayahNumber = 15,
            pageNumber = 416,
            wordSnippetArabic = "وَهُمْ لَا يَسْتَكْبِرُونَ ۩",
            fullAyahArabic = "إِنَّمَا يُؤْمِنُ بِآيَاتِنَا الَّذِينَ إِذَا ذُكِّرُوا بِهَا خَرُّوا سُجَّدًا وَسَبَّحُوا بِحَمْدِ رَبِّهِمْ وَهُمْ لَا يَسْتَكْبِرُونَ ۩",
            pronunciationGuide = "Disunnahkan melakukan Sujud Tilawah sebanyak 1 kali saat membaca atau mendengar ayat ini",
            writtenVsSpoken = "Terdapat simbol kubah mihrab ۩",
            explanation = "Ayat yang memuat perintah sujud dan ketundukan kepada Allah. Sunnah muakkadah sujud tilawah dengan membaca doa: سَجَدَ وَجْهِي لِلَّذِي خَلَقَهُ وَصَوَّرَهُ وَشَقَّ سَمْعَهُ وَبَصَرَهُ بِحَوْلِهِ وَقُوَّتِهِ",
            ruleType = GharibType.AYAT_SAJDAH
        ),
        GharibReading(
            id = 15,
            categoryId = "ayat_sajdah",
            categoryTitle = "Ayat Sajdah (سجدة التلاوة)",
            surahNumber = 96,
            surahName = "Al-'Alaq",
            ayahNumber = 19,
            pageNumber = 597,
            wordSnippetArabic = "وَاسْجُدْ وَاقْتَرِبْ ۩",
            fullAyahArabic = "كَلَّا لَا تُطِعْهُ وَاسْجُدْ وَاقْتَرِبْ ۩",
            pronunciationGuide = "Disunnahkan sujud tilawah pada akhir ayat Al-'Alaq",
            writtenVsSpoken = "Simbol ۩ di akhir ayat",
            explanation = "Penutup surah pertama yang diwahyukan, memerintahkan sujud dan mendekatkan diri kepada Allah SWT.",
            ruleType = GharibType.AYAT_SAJDAH
        )
    )
}
