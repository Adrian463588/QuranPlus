# Knowledge Chunks for RAG Vector Search in Quran Plus
# Ground truth knowledge base chunks indexing Quran, Hadith, Tahsin, and Core Islamic guidance.

KNOWLEDGE_CHUNKS = [
    # --- A. QURANIC THEMATIC CHUNKS ---
    {
        "source_type": "quran_theme",
        "source_id": "tauhid_aqidah_01",
        "title": "Tauhid Uluhiyyah dan Keagungan Allah (Ayat Kursi & Al-Ikhlas)",
        "text_content": "Tauhid adalah fondasi utama dalam Islam, yaitu mengesakan Allah SWT dalam Rububiyyah, Uluhiyyah, dan Asma' wa Sifat. Ayat Kursi (QS. Al-Baqarah: 255) menegaskan bahwa Allah adalah Al-Hayyu (Maha Hidup) dan Al-Qayyum (Maha Berdiri Sendiri), tidak mengantuk dan tidak tidur, pemilik langit dan bumi. Surat Al-Ikhlas (QS. 112: 1-4) menyatakan: 'Qul Huwallahu Ahad, Allahush Shamad, Lam Yalid wa Lam Yulad, wa Lam Yakul Lahu Kufuwan Ahad' (Katakanlah: Dialah Allah Yang Maha Esa, Allah tempat bergantung segala sesuatu, Dia tidak beranak dan tidak diperanakkan, dan tidak ada sesuatu yang setara dengan Dia). Dalil ini menjadi dasar keyakinan bahwa tiada sesembahan yang berhak diibadahi selain Allah semata."
    },
    {
        "source_type": "quran_theme",
        "source_id": "shalat_ibadah_02",
        "title": "Kewajiban Shalat, Keutamaan Khusyu', dan Waktu-waktu Shalat",
        "text_content": "Shalat adalah rukun Islam kedua dan tiang agama. Al-Quran menegaskan: 'Innash shalaata kaanat 'alal mu'miniina kitaaban mauquuta' (Sesungguhnya shalat itu adalah fardhu yang ditentukan waktunya atas orang-orang yang beriman, QS. An-Nisa: 103). Shalat berfungsi mencegah perbuatan keji dan munkar ('Innash shalaata tan-haa 'anil fahsyaa-i wal munkar', QS. Al-Ankabut: 45). Orang-orang beriman yang beruntung adalah mereka yang khusyu' dalam shalatnya ('Alladziina hum fii shalaatihim khaasyi'uun', QS. Al-Mu'minun: 2). Shalat lima waktu wajib dijaga secara konsisten, terutama Shalat Wustha (QS. Al-Baqarah: 238)."
    },
    {
        "source_type": "quran_theme",
        "source_id": "zakat_infaq_03",
        "title": "Kewajiban Zakat, Infaq, dan Keberkahan Sedekah",
        "text_content": "Zakat dan sedekah adalah instrumen pensucian jiwa dan harta dalam Islam. Allah berfirman: 'Khudz min amwaalihim shadaqatan tuthohhiruhum wa tuzakkiihim bihaa' (Ambillah zakat dari sebagian harta mereka, dengan zakat itu kamu membersihkan dan mensucikan mereka, QS. At-Taubah: 103). Perumpamaan orang yang menafkahkan hartanya di jalan Allah adalah seperti sebutir benih yang menumbuhkan tujuh bulir, pada tiap-tiap bulir ada seratus biji, dan Allah melipatgandakan bagi siapa yang Dia kehendaki (QS. Al-Baqarah: 261). Delapan asnaf penerima zakat dijelaskan secara rinci dalam QS. At-Taubah: 60 (Fakir, Miskin, Amil, Muallaf, Riqab, Gharimin, Fisabilillah, Ibnu Sabil)."
    },
    {
        "source_type": "quran_theme",
        "source_id": "puasa_ramadhan_04",
        "title": "Kewajiban Puasa Ramadhan dan Pembentukan Ketakwaan",
        "text_content": "Puasa di bulan Ramadhan diwajibkan bagi setiap muslim yang baligh dan berakal sehat. Firman Allah SWT: 'Yaa ayyuhalladziina aamanuu kutiba 'alaikumush shiyaamu kamaa kutiba 'alalladziina min qablikum la'allakum tattaquun' (Wahai orang-orang yang beriman, diwajibkan atas kamu berpuasa sebagaimana diwajibkan atas orang-orang sebelum kamu agar kamu bertakwa, QS. Al-Baqarah: 183). Bulan Ramadhan adalah bulan diturunkannya Al-Quran sebagai petunjuk bagi manusia (QS. Al-Baqarah: 185). Puasa melatih pengendalian hawa nafsu, empati terhadap kaum dhuafa, dan mendekatkan diri kepada Allah."
    },
    {
        "source_type": "quran_theme",
        "source_id": "akhlak_budi_pekerti_05",
        "title": "Akhlak Mulia, Berbakti kepada Orang Tua (Birrul Walidain), dan Menjaga Lisan",
        "text_content": "Islam sangat menekankan kemuliaan akhlak. Allah memuji Rasulullah: 'Wa innaka la'alaa khuluqin 'azhiim' (Dan sesungguhnya engkau benar-benar berbudi pekerti yang luhur, QS. Al-Qalam: 4). Berbakti kepada orang tua ditempatkan langsung setelah larangan menyekutukan Allah: 'Wa qadhaa rabbuka allaa ta'buduu illaa iyyaahu wa bil waalidaini ihsaanaa' (Dan Rabbmu telah memerintahkan supaya kamu jangan menyembah selain Dia dan hendaklah berbuat baik pada ibu bapakmu dengan sebaik-baiknya, janganlah mengatakan perkataan 'ah' / 'uf' dan jangan membentak mereka, QS. Al-Isra: 23-24). Mukmin sejati juga senantiasa menjauhi ghibah, prasangka buruk, dan fitnah (QS. Al-Hujurat: 12)."
    },
    {
        "source_type": "quran_theme",
        "source_id": "doa_pilihan_quran_06",
        "title": "Doa-doa Mustajab dan Robbana dalam Al-Quran",
        "text_content": "Al-Quran memuat banyak doa para Nabi dan orang-orang shalih: 1. Doa kebaikan dunia akhirat: 'Rabbanaa aatinaa fid dunyaa hasanah wa fil aakhirati hasanah wa qinaa 'adzaaban naar' (QS. Al-Baqarah: 201). 2. Doa ketetapan hati dalam iman: 'Rabbanaa laa tuzigh quluubanaa ba'da idz hadaitanaa wa hab lanaa min ladunka rahmah innaka antal wahhaab' (QS. Ali 'Imran: 8). 3. Doa keluarga sakinah dan keturunan shalih: 'Rabbanaa hab lanaa min azwaajinaa wa dzurriyyatinaa qurrata a'yuniw waj'alnaa lil muttaqiina imaamaa' (QS. Al-Furqan: 74). 4. Doa pengampunan dosa (Nabi Adam): 'Rabbanaa zhalamnaa anfusanaa wa in lam taghfir lanaa wa tarhamnaa lanakuunanna minal khaasiriin' (QS. Al-A'raf: 23)."
    },
    {
        "source_type": "quran_theme",
        "source_id": "sabar_tawakkal_07",
        "title": "Hakikat Sabar, Syukur, dan Tawakkal Menghadapi Ujian Hidup",
        "text_content": "Ujian adalah sunnatullah dalam kehidupan seorang mukmin. Firman Allah: 'Wa lanabluwannakum bisyai-im minal khaufi wal juu'i wa naqshim minal amwaali wal anfusi wats tsamaraat, wa basysyirish shaabiriin' (Dan Kami pasti akan menguji kalian dengan sedikit ketakutan, kelaparan, kekurangan harta, jiwa, dan buah-buahan; dan sampaikanlah kabar gembira kepada orang-orang yang sabar, QS. Al-Baqarah: 155). Orang sabar mengucapkan: 'Inna lillahi wa inna ilaihi raaji'uun'. Bersamaan dengan ikhtiar dan sabar, seorang hamba bertawakkal sepenuhnya kepada Allah ('Wa may yatawakkal 'alallaahi fahuwa hasbuh', QS. At-Thalaq: 3)."
    },
    {
        "source_type": "quran_theme",
        "source_id": "muamalah_halal_haram_08",
        "title": "Prinsip Muamalah Islam, Larangan Riba, dan Keadilan Berniaga",
        "text_content": "Islam menghalalkan jual beli yang adil dan mengharamkan segala bentuk riba serta penipuan: 'Wa ahallallaahul bai'a wa harramar ribaa' (Allah telah menghalalkan jual beli dan mengharamkan riba, QS. Al-Baqarah: 275). Perniagaan harus didasari atas dasar saling ridha ('antaradhin minkum, QS. An-Nisa: 29). Transaksi hutang piutang wajib dicatat dengan adil dan transparan (QS. Al-Baqarah: 282 - Ayat Terpanjang dalam Al-Quran). Larangan mengurangi takaran dan timbangan ditegaskan dalam QS. Al-Muthaffifin: 1-3."
    },

    # --- B. HADITH FOUNDATIONS CHUNKS ---
    {
        "source_type": "hadith_theme",
        "source_id": "hadith_niat_ikhlas_01",
        "title": "Pentingnya Niat dan Keikhlasan (HR. Bukhari No. 1 & Hadits Arbain 1)",
        "text_content": "Hadits 'Innamal a'maalu bin niyyaat' (Setiap amalan tergantung pada niatnya) adalah kaidah pokok syariat Islam. Menurut Imam Asy-Syafi'i dan Imam Ahmad, hadits ini mencakup sepertiga ilmu agama. Niat membedakan antara perbuatan ibadah dan kebiasaan adat (misal: mandi junub vs mandi biasa), serta membedakan tujuan ibadah apakah semata-mata karena Allah (ikhlas) atau mencari pujian manusia (riya'). Amal yang besar bisa menjadi bernilai kecil di sisi Allah jika salah niatnya, dan amal yang tampak kecil bisa bernilai agung karena ketulusan niatnya."
    },
    {
        "source_type": "hadith_theme",
        "source_id": "hadith_keutamaan_quran_02",
        "title": "Keutamaan Membaca, Menghafal, dan Mengamalkan Al-Quran (Hadits Bukhari & Muslim)",
        "text_content": "Rasulullah shallallahu 'alaihi wa sallam bersabda: 'Sebaik-baik kalian adalah orang yang belajar Al-Quran dan mengajarkannya' (HR. Bukhari). Membaca satu huruf dari Al-Quran diganjar sepuluh kebaikan, di mana Alif satu huruf, Lam satu huruf, dan Mim satu huruf (HR. Tirmidzi). Pada hari kiamat, Al-Quran dan puasa akan datang memberikan syafaat (pembelaan) bagi orang yang rajin membacanya (HR. Muslim & Ahmad). Orang yang mahir membaca Al-Quran akan ditempatkan bersama malaikat-malaikat yang mulia dan taat, sedangkan yang terbata-bata dan susah membacanya mendapatkan dua pahala (pahala membaca dan pahala perjuangan belajarnya)."
    },
    {
        "source_type": "hadith_theme",
        "source_id": "hadith_hak_sesama_muslim_03",
        "title": "Enam Hak dan Kewajiban Sesama Muslim (HR. Muslim No. 2162)",
        "text_content": "Rasulullah shallallahu 'alaihi wa sallam bersabda bahwa hak seorang muslim atas muslim lainnya ada enam: 1. Apabila engkau bertemu dengannya, ucapkanlah salam (Assalamu'alaikum). 2. Apabila ia mengundangmu, penuhilah undangannya. 3. Apabila ia meminta nasihat kepadamu, berilah nasihat yang tulus. 4. Apabila ia bersin lalu memuji Allah (Alhamdulillah), doakanlah dia (Yarhamukallah). 5. Apabila ia sakit, jenguklah dia. 6. Apabila ia meninggal dunia, iringilah jenazahnya hingga pemakaman. Hadits ini meneguhkan jalinan persaudaraan (Ukhuwah Islamiyyah) dan kepedulian sosial."
    },
    {
        "source_type": "hadith_theme",
        "source_id": "hadith_menuntut_ilmu_04",
        "title": "Kewajiban Menuntut Ilmu dan Jalan Menuju Surga (HR. Muslim & Darimi)",
        "text_content": "Menuntut ilmu agama adalah kewajiban bagi setiap muslim ('Thalabul 'ilmi fariidhatun 'alaa kulli muslim', HR. Ibnu Majah). Rasulullah bersabda: 'Barangsiapa menempuh jalan untuk menuntut ilmu, Allah akan memudahkan baginya jalan menuju surga' (HR. Muslim). Para malaikat merendahkan sayapnya sebagai tanda ridha terhadap penuntut ilmu, dan seluruh makhluk bahkan ikan di lautan memohonkan ampunan bagi orang yang berilmu. Ilmu yang bermanfaat adalah salah satu dari tiga amalan yang pahalanya tidak terputus setelah kematian (sedekah jariyah, ilmu bermanfaat, dan anak shalih yang mendoakannya, HR. Muslim)."
    },

    # --- C. TAHSIN & TAJWID CHUNKS ---
    {
        "source_type": "tahsin_tajwid",
        "source_id": "tahsin_makharij_overview_01",
        "title": "17 Titik Artikulasi Huruf Hijaiyah (Makharij al-Huruf) Menurut Imam Ibnu Al-Jazari",
        "text_content": "Makharij al-Huruf adalah tempat keluarnya huruf hijaiyah saat dilafalkan. Terdapat 17 makhraj spesifik yang terbagi ke dalam 5 daerah utama: 1. Al-Jawf (Rongga mulut dan tenggorokan) untuk huruf Mad (ا و ي). 2. Al-Halq (Tenggorokan: pangkal ء هـ, tengah ع ح, ujung غ خ). 3. Al-Lisan (Lidah: pangkal ق ك, tengah ج ش ي, sisi tepi ض, ujung tepi ل ن ر, ujung lidah bertemu gigi seri ط د ت, ص ز س, ظ ذ ث). 4. Asy-Syafatain (Dua bibir: ف pada bibir bawah & gigi atas; ب م و pada kedua bibir). 5. Al-Khaisyum (Rongga hidung) untuk suara dengung Ghunnah (نّ مّ). Memahami makhraj sangat penting agar huruf tidak tertukar dan makna ayat tetap terjaga."
    },
    {
        "source_type": "tahsin_tajwid",
        "source_id": "tahsin_sifat_overview_02",
        "title": "Sifat-sifat Huruf Hijaiyah (Sifat yang Memiliki Lawan dan Sifat Tunggal)",
        "text_content": "Sifat al-Huruf adalah karakteristik suara yang menyertai huruf saat keluar dari makhrajnya. Terbagi menjadi dua kategori: A. Sifat Berlawanan (5 pasang/10 sifat): 1. Hams (nafas berhembus: فَحَثَّهُ شَخْصٌ سَكَتَ) vs Jahr (nafas tertahan). 2. Syiddah (suara tertahan kuat: أَجِدْ قَطٍ بَكَتْ) & Tawassuth (sedang: لِنْ عُمَرْ) vs Rakhawah (suara lepas mengalir). 3. Isti'la (pangkal lidah naik/tebal: خُصَّ ضَغْطٍ قِظْ) vs Istifal (pangkal lidah turun/tipis). 4. Ithbaq (lidah menempel langit-langit: ص ض ط ظ) vs Infitah (lidah terbuka). 5. Idzlaq (ringan di bibir/ujung lidah: فَرَّ مِنْ لُبٍّ) vs Ishmat. B. Sifat Tunggal (tanpa lawan): Qalqalah (pantulan suara: ق ط ب ج د), Shafir (desis burung: ص ز س), Lien (lembut: وْ يْ setelah fathah), Inhiraf (penyimpangan suara: ل ر), Takrir (getaran lidah terkendali: ر), Tafasysyi (angin menyebar luas: ش), Istithalah (suara memanjang tepi lidah: ض), dan Ghunnah (dengung rongga hidung: ن م)."
    },
    {
        "source_type": "tahsin_tajwid",
        "source_id": "tahsin_hukum_nun_mim_03",
        "title": "Hukum Lengkap Nun Sukun/Tanwin dan Mim Sukun",
        "text_content": "Hukum Nun Sukun & Tanwin ada 5: 1. Izhar Halqi (ء هـ ع ح غ خ - dibaca jelas tanpa dengung). 2. Idgham Bighunnah (ي ن م و - dilebur disertai dengung 2 harakat). 3. Idgham Bilaghunnah (ل ر - dilebur sempurna tanpa dengung). 4. Iqlab (ب - suara nun diubah menjadi mim disertai dengung). 5. Ikhfa Haqiqi (15 huruf - suara disamarkan antara izhar dan idgham disertai dengung 2 harakat). Hukum Mim Sukun ada 3: 1. Ikhfa Syafawi (bertemu ب - samar dengan bibir lembut disertai dengung). 2. Idgham Mimi/Mutamatsilain (bertemu م - dilebur disertai dengung). 3. Izhar Syafawi (bertemu huruf selain ب dan م - dibaca jelas di bibir tanpa dengung)."
    },
    {
        "source_type": "tahsin_tajwid",
        "source_id": "tahsin_hukum_mad_04",
        "title": "Hukum Mad (Panjang Bacaan) dalam Tajwid Al-Quran",
        "text_content": "Mad secara bahasa artinya memanjangkan suara. Terbagi menjadi: 1. Mad Thabi'i / Mad Ashli (panjang 2 harakat: alif setelah fathah, waw setelah dhammah, ya setelah kasrah). 2. Mad Wajib Muttashil (mad bertemu hamzah dalam satu kata, panjang 4-5 harakat). 3. Mad Jaiz Munfashil (mad bertemu hamzah di kata berikutnya, panjang 2, 4, atau 5 harakat). 4. Mad 'Aridh Lissukun (mad bertemu huruf sukun karena waqaf di akhir kalimat, panjang 2, 4, atau 6 harakat). 5. Mad Lazim (Mad bertemu sukun asli atau tasydid, wajib 6 harakat penuh, baik Kilmi seperti 'Adh-Dhaallin' maupun Harfi seperti 'Alif Laam Miim'). 6. Mad Shilah (Qashirah 2 harakat, Thawilah 4-5 harakat pada ha dhamir). 7. Mad 'Iwad (2 harakat pengganti tanwin fathah waqaf)."
    }
]
