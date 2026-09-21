# -*- coding: utf-8 -*-
"""
Generator and Updater for DzikirDataCatalog.kt
"""
import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    original_code = f.read()

# -------------------------------------------------------------
# 1. SHALAT (12 Items)
# -------------------------------------------------------------
shalat_12_items = """            DzikirItem(
                id = "shalat_01",
                category = DzikirCategory.SHALAT,
                orderNumber = 1,
                title = "Istighfar Pembuka & Taubat (3x)",
                arabicText = "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ الْحَيَّ الْقَيُّومَ وَأَتُوبُ إِلَيْهِ",
                tajwidTags = "[l[أَسْ]]تَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي [o[لَا]] [o[إِلَٰهَ]] [o[إِلَّا]] هُوَ الْحَيَّ [p[الْقَيُّومَ]] وَ[o[أَتُوبُ]] [p[إِلَيْهِ]]",
                transliteration = "Astaghfirullāhal-‘aẓīm, alladzī lā ilāha illā huwal-ḥayyul-qayyūma wa atūbu ilaih (3x).",
                translationId = "Aku memohon ampun kepada Allah Yang Mahaagung, yang tiada tuhan selain Dia, Yang Mahahidup lagi terus-menerus mengurus makhluk-Nya, dan aku bertaubat kepada-Nya (3 kali).",
                translationEn = "I seek forgiveness from Allah the Magnificent, whom there is no deity except Him, the Ever-Living, the Sustainer, and I repent unto Him (3 times).",
                repeatCount = 3,
                sourceNote = "HR. Abu Dawud no. 1517, At-Tirmidzi no. 3577",
                fadhilah = "Menghapuskan dosa-dosa dan kekurangan yang terjadi selama mendirikan shalat."
            ),
            DzikirItem(
                id = "shalat_02",
                category = DzikirCategory.SHALAT,
                orderNumber = 2,
                title = "Doa Perlindungan Siksa Neraka (7x)",
                arabicText = "اللَّهُمَّ أَجِرْنَا مِنَ النَّارِ",
                tajwidTags = "[g[اللَّ]]هُمَّ [q[أَجِ]]رْنَا مِنَ [l[النَّ]][p[ارِ]]",
                transliteration = "Allāhumma ajirnā minan-nār (7x).",
                translationId = "Ya Allah, selamatkanlah kami dari siksa api neraka (dibaca 7 kali seusai Subuh dan Maghrib).",
                translationEn = "O Allah, save us from the punishment of the Hellfire (7 times).",
                repeatCount = 7,
                sourceNote = "HR. Abu Dawud no. 5079, Ahmad no. 17362",
                fadhilah = "Jika dibaca 7 kali seusai shalat Shubuh dan Maghrib, Allah tetapkan perlindungan baginya dari siksa neraka."
            ),
            DzikirItem(
                id = "shalat_03",
                category = DzikirCategory.SHALAT,
                orderNumber = 3,
                title = "Kalimat Tauhid & Pengagungan",
                arabicText = "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ يُحْيِي وَيُمِيتُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
                tajwidTags = "[o[لَا]] [o[إِلَٰهَ]] [o[إِلَّا]] اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ يُحْيِي وَيُمِيتُ وَهُوَ عَلَىٰ كُلِّ شَيْ[f[ءٍ]] [f[قَ]]دِيرٌ",
                transliteration = "Lā ilāha illallāhu waḥdahū lā syarīka lah, lahul-mulku wa lahul-ḥamdu yuḥyī wa yumītu wa huwa ‘alā kulli syai’ing qadīr.",
                translationId = "Tidak ada tuhan selain Allah semata, tidak ada sekutu bagi-Nya. Milik-Nyalah kerajaan dan segala pujian, Dia yang menghidupkan dan mematikan, dan Dia Mahakuasa atas segala sesuatu.",
                translationEn = "None has the right to be worshipped except Allah alone, without partner. To Him belongs sovereignty and praise, He gives life and causes death, and He is over all things omnipotent.",
                repeatCount = 3,
                sourceNote = "HR. Al-Bukhari no. 844, Muslim no. 593",
                fadhilah = "Peneguh tauhid pembuka keselamatan setelah shalat fardhu."
            ),
            DzikirItem(
                id = "shalat_04",
                category = DzikirCategory.SHALAT,
                orderNumber = 4,
                title = "Doa Keselamatan & Kemuliaan Lengkap",
                arabicText = "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، وَإِلَيْكَ يَعُودُ السَّلَامُ، فَحَيِّنَا رَبَّنَا بِالسَّلَامِ، وَأَدْخِلْنَا الْجَنَّةَ دَارَ السَّلَامِ، تَبَارَكْتَ رَبَّنَا وَتَعَالَيْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
                tajwidTags = "[g[اللَّ]]هُمَّ [f[أَنتَ]] [l[السَّ]]لَامُ [f[وَمِنم]]كَ [l[السَّ]]لَامُ، [o[وَإِلَيْكَ]] يَعُودُ [l[السَّ]]لَامُ، فَحَيِّنَا رَبَّنَا بِ[l[السَّ]]لَامِ، [o[وَأَدْ]][q[خِ]]لْنَا الْجَ[g[نَّةَ]] دَارَ [l[السَّ]]لَامِ، تَبَارَكْتَ رَبَّنَا وَتَعَالَيْتَ يَا ذَا الْجَلَالِ [p[وَالْإِكْرَامِ]]",
                transliteration = "Allāhumma angtas-salāmu wa mingkas-salām, wa ilaika ya‘ūdus-salām, fa ḥayyinā rabbanā bis-salām, wa adkhilnal-jannata dāras-salām, tabārakta rabbanā wa ta‘ālaita yā dzal-jalāli wal-ikrām.",
                translationId = "Ya Allah, Engkaulah Dzat Yang Memberi Keselamatan, dari-Mulah keselamatan, dan kepada-Mulah keselamatan akan kembali, maka sambutlah kami wahai Tuhan kami dengan keselamatan, dan masukkanlah kami ke surga tempat tinggal keselamatan. Mahaberkah Engkau wahai Tuhan kami dan Mahatinggi Engkau, wahai Dzat Pemilik Keagungan dan Kemuliaan.",
                translationEn = "O Allah, You are Peace, from You comes peace, and to You peace returns. Revive us, our Lord, with peace, and admit us into Paradise, the Abode of Peace...",
                repeatCount = 1,
                sourceNote = "HR. Muslim no. 591, Kitab Al-Adzkar An-Nawawiyyah",
                fadhilah = "Menyucikan hati dan menyambungkan permohonan keselamatan dunia dan akhirat."
            ),
            DzikirItem(
                id = "shalat_05",
                category = DzikirCategory.SHALAT,
                orderNumber = 5,
                title = "Doa Penyerahan & Tiada Penolak Takdir (HR. Bukhari & Muslim)",
                arabicText = "اللَّهُمَّ لَا مَانِعَ لِمَا أَعْطَيْتَ، وَلَا مُعْطِيَ لِمَا مَنَعْتَ، وَلَا رَادَّ لِمَا قَضَيْتَ، وَلَا يَنْفَعُ ذَا الْجَدِّ مِنْكَ الْجَدُّ",
                tajwidTags = "[g[اللَّ]]هُمَّ [o[لَا]] مَانِعَ لِمَا [o[أَعْ]][q[طَ]]يْتَ، [o[وَلَا]] مُ[q[عْ]][q[طِ]]يَ لِمَا مَنَعْتَ، [o[وَلَا]] [o[رَا]][g[دَّ]] لِمَا [q[قَ]]ضَيْتَ، [o[وَلَا]] [f[يَنفَ]]عُ ذَا [p[الْجَدِّ]] [f[مِنم]]كَ [p[الْجَدُّ]]",
                transliteration = "Allāhumma lā māni‘a limā a‘ṭhaita, wa lā mu‘ṭhiya limā mana‘ta, wa lā rādda limā qaḍaita, wa lā yamfa‘u dzal-jaddi mingkal-jadd.",
                translationId = "Ya Allah, tiada yang dapat mencegah apa yang Engkau berikan, tiada yang dapat memberi apa yang Engkau tahan, tiada yang dapat menolak apa yang telah Engkau tetapkan, dan tidak berguna kekayaan serta kemuliaan bagi orang yang memilikinya dari siksa-Mu.",
                translationEn = "O Allah, none can withhold what You have given, none can give what You have withheld, none can reverse what You have decreed, and no fortune can benefit its possessor against You.",
                repeatCount = 1,
                sourceNote = "HR. Al-Bukhari no. 844, Muslim no. 593 dari Al-Mughirah bin Syu'bah",
                fadhilah = "Kepasrahan total atas takdir dan rezeki Allah, memurnikan tauhid dari kesombongan duniawi."
            ),
            DzikirItem(
                id = "shalat_06",
                category = DzikirCategory.SHALAT,
                orderNumber = 6,
                title = "Surat Al-Fatihah Pembuka Keberkahan",
                arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۝ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝ الرَّحْمَٰنِ الرَّحِيمِ ۝ مَالِكِ يَوْمِ الدِّينِ ۝ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۝ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۝ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
                tajwidTags = "بِسْمِ اللَّهِ [l[ال]][l[رَّ]]حْمَٰنِ [l[ال]][p[رَّحِيمِ]] ۝ الْحَمْدُ لِلَّهِ رَبِّ [p[الْعَالَمِينَ]] ۝ [l[ال]][l[رَّ]]حْمَٰنِ [p[الرَّحِيمِ]] ۝ مَالِكِ يَوْمِ [l[الدِّ]][p[ينِ]] ۝ [o[إِيَّاكَ]] [l[نَعْ]][q[بُ]]دُ [o[وَإِيَّاكَ]] [p[نَسْتَعِينُ]] ۝ [l[اهْ]]دِنَا [l[الصِّ]]رَا[q[طَ]] [p[الْمُسْتَقِيمَ]] ۝ صِرَا[q[طَ]] الَّذِينَ [o[أَنْعَمْتَ]] عَلَيْهِمْ غَيْرِ الْمَ[q[غْ]][q[ضُ]]وبِ عَلَيْهِمْ وَلَا [o[الضَّ]][g[الِّينَ]]",
                transliteration = "Bismillāhir-raḥmānir-raḥīm. Al-ḥamdu lillāhi rabbil-‘ālamīn. Ar-raḥmānir-raḥīm. Māliki yaumid-dīn. Iyyāka na‘budu wa iyyāka nasta‘īn. Ihdinaṣ-ṣirāṭhal-mustaqīm. Ṣirāṭhal-ladzīna an‘amta ‘alaihim ghairil-maghdūbi ‘alaihim walāḍ-ḍāllīn.",
                translationId = "Dengan nama Allah Yang Maha Pengasih lagi Maha Penyayang. Segala puji bagi Allah, Tuhan semesta alam. Maha Pengasih lagi Maha Penyayang. Pemilik hari pembalasan. Hanya kepada Engkaulah kami menyembah dan hanya kepada Engkaulah kami memohon pertolongan. Tunjukilah kami jalan yang lurus, (yaitu) jalan orang-orang yang telah Engkau beri nikmat kepadanya; bukan (jalan) mereka yang dimurkai dan bukan (pula jalan) mereka yang sesat.",
                translationEn = "In the name of Allah, the Entirely Merciful, the Especially Merciful. Praise be to Allah, Lord of the worlds... Guide us to the straight path...",
                repeatCount = 1,
                sourceNote = "Tradisi Pembacaan Wirid Nusantara, Kitab Majmu' Syarif",
                fadhilah = "Ummul Kitab pembuka ijabah doa, pengikat keberkahan ibadah shalat fardhu."
            ),
            DzikirItem(
                id = "shalat_07",
                category = DzikirCategory.SHALAT,
                orderNumber = 7,
                title = "Ayat Kursi Ba'da Shalat (QS. Al-Baqarah: 255)",
                arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
                tajwidTags = "[g[اللَّ]]هُ [o[لَا]] [o[إِلَٰهَ]] [o[إِلَّا]] هُوَ الْحَيُّ [p[الْقَيُّومُ]] ۚ لَا تَأْخُذُهُ سِنَ[a[ةٌ]] [a[وَ]]لَا [p[نَوْمٌ]] ۚ لَّهُ مَا فِي [l[السَّ]]مَاوَاتِ وَمَا فِي الْأَرْضِ ۗ [f[مَن]] [f[ذَ]]ا الَّذِي يَشْفَعُ [f[عِندَ]]هُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْ[a[ءٍ]] [a[مِّنْ]] عِلْمِهِ إِلَّا بِمَا [o[شَاءَ]] ۚ وَسِعَ كُرْسِيُّهُ [l[السَّ]]مَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ [p[الْعَلِيُّ]] [p[الْعَظِيمُ]]",
                transliteration = "Allāhu lā ilāha illā huwal-ḥayyul-qayyūm, lā ta’khudzuhū sinatuw wa lā naūm, lahū mā fis-samāwāti wa mā fil-arḍ, mang dzal-ladzī yasyfa‘u ‘ingdahū illā bi’idznih, ya‘lamu مَا baina aidīhim wa mā khalfahum, wa lā yuḥīṭūna bi syai’im min ‘ilmihī illā bimā syā’, wasi‘a kursiyyuhus-samāwāti wal-arḍ, wa lā ya’ūduhū ḥifẓuhumā, wa huwal-‘aliyyul-‘aẓīm.",
                translationId = "Allah, tidak ada tuhan selain Dia, Yang Mahahidup lagi terus-menerus mengurus (makhluk-Nya)...",
                translationEn = "Allah! There is no deity except Him, the Ever-Living, the Sustainer of existence...",
                repeatCount = 1,
                sourceNote = "HR. An-Nasa'i dalam As-Sunan Al-Kubra no. 9928, dishahihkan Ibnu Hibban",
                fadhilah = "Barangsiapa membaca Ayat Kursi setiap selesai shalat fardhu, tidak ada yang menghalanginya masuk surga selain kematian."
            ),
            DzikirItem(
                id = "shalat_08",
                category = DzikirCategory.SHALAT,
                orderNumber = 8,
                title = "Tasbih, Tahmid, Takbir (33x) & Tahlil ke-100",
                arabicText = "سُبْحَانَ اللَّهِ (٣٣×)\\nالْحَمْدُ لِلَّهِ (٣٣×)\\nاللَّهُ أَكْبَرُ (٣٣×)\\n\\nتَمَامَ الْمِائَةِ:\\nلَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
                tajwidTags = "[q[سُبْ]]حَانَ اللَّهِ (٣٣×)\\nالْحَمْدُ لِلَّهِ (٣٣×)\\nاللَّهُ [q[أَكْ]]بَرُ (٣٣×)\\n\\nتَمَامَ الْمِ[o[ائَةِ]]:\\n[o[لَا]] [o[إِلَٰهَ]] [o[إِلَّا]] اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَىٰ كُلِّ شَيْ[f[ءٍ]] [f[قَ]][p[دِيرٌ]]",
                transliteration = "Subḥānallāh (33x)\\nAl-ḥamdu lillāh (33x)\\nAllāhu akbar (33x)\\n\\nPenutup ke-100:\\nLā ilāha illallāhu waḥdahū lā syarīka lah, lahul-mulku wa lahul-ḥamdu wa huwa ‘alā kulli syai’ing qadīr.",
                translationId = "Mahasuci Allah (33x), Segala puji bagi Allah (33x), Allah Mahabesar (33x). Penyempurna ke-100: Tidak ada tuhan selain Allah semata, tiada sekutu bagi-Nya. Milik-Nyalah kerajaan dan pujian, dan Dia Mahakuasa atas segala sesuatu.",
                translationEn = "Glory be to Allah (33x), Praise be to Allah (33x), Allah is the Greatest (33x). Completing the 100th: None has the right to be worshipped except Allah alone...",
                repeatCount = 33,
                sourceNote = "HR. Muslim no. 597",
                fadhilah = "Diampuni kesalahan-kesalahannya walaupun sebanyak buih di lautan."
            ),
            DzikirItem(
                id = "shalat_09",
                category = DzikirCategory.SHALAT,
                orderNumber = 9,
                title = "Surat Pendek Perlindungan (Al-Mu'awwidzat)",
                arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\\nقُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ\\n\\nبِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ\\n\\nبِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",
                tajwidTags = "قُلْ هُوَ اللَّهُ [q[أَحَدٌ]] ۝ اللَّهُ [l[الصَّ]]مَدُ ۝ لَمْ [q[يَلِدْ]] وَلَمْ [q[يُولَدْ]] ۝ وَلَمْ يَكُ[u[ن]] [u[لَّ]]هُ كُفُ[a[وًا]] [a[أَ]]حَدٌ\\n\\nقُلْ أَعُوذُ بِرَبِّ [q[الْفَلَقِ]] ۝ [f[مِن]] [f[شَ]]رِّ مَا [q[خَلَقَ]] ۝ وَ[f[مِن]] [f[شَ]]رِّ غَاسِقٍ إِذَا [q[وَقَبَ]] ۝ وَ[f[مِن]] [f[شَ]]رِّ [l[النَّ]]فَّاثَاتِ فِي [q[الْعُقَدِ]] ۝ وَ[f[مِن]] [f[شَ]]رِّ حَاسِدٍ إِذَا [q[حَسَدَ]]\\n\\nقُلْ أَعُوذُ بِرَبِّ [l[النَّ]]اسِ ۝ مَلِكِ [l[النَّ]]اسِ ۝ إِلَٰهِ [l[النَّ]]اسِ ۝ [f[مِن]] [f[شَ]]رِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ [l[النَّ]]اسِ ۝ مِنَ الْجِنَّةِ وَ[l[النَّ]]اسِ",
                transliteration = "Surat Al-Ikhlas, Al-Falaq, dan An-Nas seusai shalat fardhu.",
                translationId = "Membaca Surah Al-Ikhlas, Al-Falaq, dan An-Nas setiap selesai shalat fardhu (dibaca 3x saat Shubuh & Maghrib).",
                translationEn = "Reciting Surah Al-Ikhlas, Al-Falaq, and An-Nas after obligatory prayers.",
                repeatCount = 1,
                sourceNote = "HR. Abu Dawud no. 1523, An-Nasa'i 3/68",
                fadhilah = "Membentengi diri dari segala godaan sihir, bisikan setan, dan kedengkian."
            ),
            DzikirItem(
                id = "shalat_10",
                category = DzikirCategory.SHALAT,
                orderNumber = 10,
                title = "Doa Wasiat Rasulullah SAW (Kemudahan Dzikir & Syukur)",
                arabicText = "اللَّهُمَّ أَعِنِّي عَلَىٰ ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ",
                tajwidTags = "[g[اللَّ]]هُمَّ [o[أَعِ]][g[نِّي]] عَلَىٰ ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ [p[عِبَادَتِكَ]]",
                transliteration = "Allāhumma a‘innī ‘alā dzikrika, wa syukrika, wa ḥusni ‘ibādatik.",
                translationId = "Ya Allah, tolonglah aku untuk senantiasa mengingat-Mu, bersyukur kepada-Mu, dan beribadah dengan baik kepada-Mu.",
                translationEn = "O Allah, help me to remember You, to thank You, and to worship You in an excellent manner.",
                repeatCount = 1,
                sourceNote = "HR. Abu Dawud no. 1522, An-Nasa'i no. 1303",
                fadhilah = "Wasiat istimewa Rasulullah SAW kepada Mu'adz bin Jabal agar tidak pernah meninggalkannya seusai shalat."
            ),
            DzikirItem(
                id = "shalat_11",
                category = DzikirCategory.SHALAT,
                orderNumber = 11,
                title = "Doa Birrul Walidain untuk Kedua Orang Tua",
                arabicText = "رَبَّنَا اغْفِرْ لَنَا وَلِوَالِدَيْنَا وَارْحَمْهُمْ كَمَا رَبَّوْنَا صِغَارًا",
                tajwidTags = "رَ[g[بَّ]]نَا [l[ا]][q[غْ]]فِرْ لَنَا وَلِوَالِدَيْنَا وَ[l[ارْ]]حَمْهُم كَمَا رَ[g[بَّ]][o[وْنَا]] [p[صِغَارًا]]",
                transliteration = "Rabbanaghfir lanā wa li-wālidinā warḥamhum kamā rabbaynā ṣighārā.",
                translationId = "Wahai Tuhan kami, ampunilah dosa kami dan dosa kedua orang tua kami, dan kasihanilah mereka berdua sebagaimana mereka telah mendidik kami sewaktu kecil.",
                translationEn = "Our Lord, forgive us and our parents, and have mercy upon them as they brought us up when we were small.",
                repeatCount = 1,
                sourceNote = "QS. Al-Isra: 24, Tradisi Doa Shalat Harian",
                fadhilah = "Bakti tertinggi kepada orang tua setiap selesai shalat fardhu mengalirkan rahmat dan ampunan bagi mereka."
            ),
            DzikirItem(
                id = "shalat_12",
                category = DzikirCategory.SHALAT,
                orderNumber = 12,
                title = "Doa Penutup Ba'da Shalat Ma'tsur Lengkap",
                arabicText = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ حَمْدًا يُوَافِي نِعَمَهُ وَيُكَافِئُ مَزِيدَهُ، يَا رَبَّنَا لَكَ الْحَمْدُ كَمَا يَنْبَغِي لِجَلَالِ وَجْهِكَ وَلِعَظِيمِ سُلْطَانِكَ، رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ، وَصَلَّى اللَّهُ عَلَىٰ سَيِّدِنَا مُحَمَّدٍ وَعَلَىٰ آلِهِ وَصَحْبِهِ وَسَلَّمَ",
                tajwidTags = "الْحَمْدُ لِلَّهِ رَبِّ [p[الْعَالَمِينَ]] حَمْ[a[دًا]] [a[يُ]]وَافِي نِعَمَهُ [a[وَيُكَافِئُ]] مَزِيدَهُ، يَا رَبَّنَا لَكَ الْحَمْدُ كَمَا [f[يَنبَ]]غِي لِجَلَالِ وَ[q[جْ]]هِكَ وَلِعَظِيمِ سُلْطَانِكَ، رَبَّنَا [o[آتِنَا]] فِي [l[الدُّ]]نْيَا حَسَنَ[a[ةً]] [a[وَ]]فِي الْآخِرَةِ حَسَنَ[a[ةً]] [a[وَ]][q[قِ]]نَا عَذَابَ [l[النَّ]]ارِ، وَصَلَّى اللَّهُ عَلَىٰ سَيِّدِنَا مُحَ[g[مَّ]]دٍ [o[وَعَلَىٰ]] [o[آلِهِ]] وَصَحْبِهِ [p[وَسَلَّمَ]]",
                transliteration = "Al-ḥamdu lillāhi rabbil-‘ālamīn, ḥamday yuwāfī ni‘amahū wa yukāfi’u mazīdah, yā rabbanā lakal-ḥamdu kamā yambaghī li jalāli wajhika wa li‘aẓīmi sulṭānik. Rabbanā ātinā fid-dunyā ḥasanataw wa fil-ākhirati ḥasanataw wa qinā ‘adzāban-nār. Wa ṣallallāhu ‘alā sayyidinā Muḥammadiw wa ‘alā ālihī wa ṣaḥbihī wa sallam.",
                translationId = "Segala puji bagi Allah Tuhan semesta alam, pujian yang sebanding dengan nikmat-nikmat-Nya dan menjamin tambahannya. Wahai Tuhan kami, bagi-Mu segala puji sebagaimana layak bagi keagungan wajah-Mu dan kebesaran kekuasaan-Mu. Ya Tuhan kami, berilah kami kebaikan di dunia dan kebaikan di akhirat, dan lindungilah kami dari azab neraka. Dan semoga Allah melimpahkan shalawat serta salam kepada junjungan kami Nabi Muhammad beserta keluarga dan para sahabatnya.",
                translationEn = "All praise is due to Allah, Lord of the worlds, a praise matching His blessings and compensating for His abundance... Our Lord, give us in this world that which is good and in the Hereafter that which is good and save us from the punishment of the Fire...",
                repeatCount = 1,
                sourceNote = "Kumpulan Doa Ma'tsur Ba'da Shalat, Majmu' Syarif",
                fadhilah = "Doa penutup shalat yang merangkum segala kebaikan dunia dan keselamatan di akhirat."
            ),"""

print("SHALAT items defined successfully.")
