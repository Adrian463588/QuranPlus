# -*- coding: utf-8 -*-
import re

with open('app/src/main/java/com/quranplus/app/features/dzikir/data/DzikirDataCatalog.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Extract existing matsurat item bodies
pattern = re.compile(r'DzikirItem\s*\(\s*id\s*=\s*"matsurat_(\d+)",\s*category\s*=\s*DzikirCategory\.AL_MATSURAT,\s*orderNumber\s*=\s*\d+,\s*title\s*=\s*"([^"]+)",\s*arabicText\s*=\s*"(.*?)"\s*,\s*tajwidTags\s*=\s*"(.*?)"\s*,\s*transliteration\s*=\s*"(.*?)"\s*,\s*translationId\s*=\s*"(.*?)"\s*,\s*translationEn\s*=\s*"(.*?)"\s*,\s*repeatCount\s*=\s*(\d+),\s*sourceNote\s*=\s*"(.*?)"(?:,\s*fadhilah\s*=\s*"(.*?)")?\s*\)', re.DOTALL)

matches = pattern.findall(text)
existing_by_num = {int(m[0]): m for m in matches}
print(f"Mapped {len(existing_by_num)} existing items.")

# We need to assemble 28 items in order:
# 1..12: existing 1..12
# 13: NEW - Doa 'Afiyah 3x
# 14: NEW - Hasbiyallah 7x
# 15..21: existing 13..19 (renumbered to 15..21)
# 22: existing 20 (Sayyidul Istighfar)
# 23: NEW - Tasbih 100x
# 24: NEW - Istighfar 100x
# 25: NEW - Shalawat 10x
# 26: NEW - Tahlil 10x
# 27: NEW - Ayat Mulk (Ali Imran 26-27)
# 28: existing 21 (Doa Rabithah)

new_13 = {
    'title': "Doa 'Afiyah & Perlindungan Siksa Kubur (3x)",
    'arabicText': "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَٰهَ إِلَّا أَنْتَ",
    'tajwidTags': "[g[اللَّ]]هُمَّ عَافِنِي فِي بَدَنِي، [g[اللَّ]]هُمَّ عَافِنِي فِي سَمْعِي، [g[اللَّ]]هُمَّ عَافِنِي فِي بَصَرِي، [g[اللَّ]]هُمَّ [g[إِنِّي]] [o[أَعُوذُ]] بِكَ مِنَ الْكُ[q[فْ]]رِ وَالْفَ[q[قْ]]رِ، [g[اللَّ]]هُمَّ [g[إِنِّي]] [o[أَعُوذُ]] بِكَ مِنْ عَذَابِ [p[الْقَبْرِ]]، [o[لَا]] [o[إِلَٰهَ]] [o[إِلَّا]] [p[أَنتَ]]",
    'transliteration': "Allāhumma ‘āfinī fī badanī, Allāhumma ‘āfinī fī sam‘ī, Allāhumma ‘āfinī fī baṣarī, Allāhumma innī a‘ūdzu bika minal-kufri wal-faqr, Allāhumma innī a‘ūdzu bika min ‘adzābil-qabr, lā ilāha illā angt (3x).",
    'translationId': "Ya Allah, selamatkanlah tubuhku (dari penyakit dan kelemahan). Ya Allah, selamatkanlah pendengaranku. Ya Allah, selamatkanlah penglihatanku. Ya Allah, sesungguhnya aku berlindung kepada-Mu dari kekufuran dan kefakiran. Ya Allah, sesungguhnya aku berlindung kepada-Mu dari siksa kubur. Tidak ada tuhan selain Engkau (dibaca 3 kali).",
    'translationEn': "O Allah, grant health to my body. O Allah, grant health to my hearing. O Allah, grant health to my sight. O Allah, I seek refuge in You from disbelief and poverty. O Allah, I seek refuge in You from the punishment of the grave. There is no deity except You (3 times).",
    'repeatCount': 3,
    'sourceNote': "HR. Abu Dawud no. 5090, Ahmad 5/42",
    'fadhilah': "Menjaga kesehatan fisik, pendengaran, dan penglihatan agar senantiasa berada dalam ketaatan kepada Allah."
}

new_14 = {
    'title': "Hasbiyallāh (Kecukupan dari Allah 7x)",
    'arabicText': "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ ۖ عَلَيْهِ تَوَكَّلْتُ ۖ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
    'tajwidTags': "حَسْبِيَ اللَّهُ [o[لَا]] [o[إِلَٰهَ]] [o[إِلَّا]] هُوَ ۖ عَلَيْهِ تَوَكَّلْتُ ۖ وَهُوَ رَبُّ الْعَرْشِ [p[الْعَظِيمِ]]",
    'transliteration': "Ḥasbiyallāhu lā ilāha illā huwa, ‘alaihi tawakkaltu wa huwa rabbul-‘arsyil-‘aẓīm (7x).",
    'translationId': "Cukuplah Allah bagiku; tidak ada tuhan selain Dia. Hanya kepada-Nya aku bertawakal dan Dia adalah Tuhan yang memiliki 'Arsy yang agung (dibaca 7 kali).",
    'translationEn': "Sufficient for me is Allah; there is no deity except Him. On Him I have relied, and He is the Lord of the Great Throne (7 times).",
    'repeatCount': 7,
    'sourceNote': "QS. At-Taubah: 129, HR. Abu Dawud no. 5081",
    'fadhilah': "Barangsiapa membacanya 7 kali pada pagi dan petang, Allah akan mencukupkan baginya urusan dunia dan akhirat yang menggelisahkannya."
}

new_23 = {
    'title': "Tasbih & Tahmid Penggugur Dosa (100x)",
    'arabicText': "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
    'tajwidTags': "[q[سُبْ]]حَانَ اللَّهِ [p[وَبِحَمْدِهِ]]",
    'transliteration': "Subḥānallāhi wa bi-ḥamdih (100x).",
    'translationId': "Mahasuci Allah dan segala puji bagi-Nya (dibaca 100 kali).",
    'translationEn': "Glory be to Allah and His is the praise (100 times).",
    'repeatCount': 100,
    'sourceNote': "HR. Al-Bukhari no. 6405, Muslim no. 2691",
    'fadhilah': "Diampuni kesalahan-kesalahannya walaupun sebanyak buih di lautan dan tidak ada yang datang di hari kiamat membawa amal lebih baik darinya."
}

new_24 = {
    'title': "Istighfar & Permohonan Taubat (100x)",
    'arabicText': "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
    'tajwidTags': "[l[أَسْ]]تَغْفِرُ اللَّهَ [o[وَأَتُوبُ]] [p[إِلَيْهِ]]",
    'transliteration': "Astaghfirullāha wa atūbu ilaih (100x).",
    'translationId': "Aku memohon ampun kepada Allah dan bertaubat kepada-Nya (dibaca 100 kali).",
    'translationEn': "I seek the forgiveness of Allah and repent to Him (100 times).",
    'repeatCount': 100,
    'sourceNote': "HR. Al-Bukhari no. 6307, Muslim no. 2702",
    'fadhilah': "Membuka pintu rezeki, melapangkan kesempitan hidup, dan mendatangkan ketenangan hati."
}

new_25 = {
    'title': "Shalawat Ibrahimiyyah (10x)",
    'arabicText': "اللَّهُمَّ صَلِّ عَلَىٰ سَيِّدِنَا مُحَمَّدٍ وَعَلَىٰ آلِ سَيِّدِنَا مُحَمَّدٍ كَمَا صَلَّيْتَ عَلَىٰ سَيِّدِنَا إِبْرَاهِيمَ وَعَلَىٰ آلِ سَيِّدِنَا إِبْرَاهِيمَ، وَبَارِكْ عَلَىٰ سَيِّدِنَا مُحَمَّدٍ وَعَلَىٰ آلِ سَيِّدِنَا مُحَمَّدٍ كَمَا بَارَكْتَ عَلَىٰ سَيِّدِنَا إِبْرَاهِيمَ وَعَلَىٰ آلِ سَيِّدِنَا إِبْرَاهِيمَ، فِي الْعَالَمِينَ إِنَّكَ حَمِيدٌ مَجِيدٌ",
    'tajwidTags': "[g[اللَّ]]هُمَّ صَ[g[لِّ]] عَلَىٰ سَيِّدِنَا مُحَ[g[مَّ]][a[دٍ]] [a[وَعَلَىٰ]] [o[آلِ]] سَيِّدِنَا مُحَ[g[مَّ]][f[دٍ]] [f[كَ]]مَا صَ[g[لَّ]]يْتَ عَلَىٰ سَيِّدِنَا [o[إِبْرَاهِيمَ]] [o[وَعَلَىٰ]] [o[آلِ]] سَيِّدِنَا [o[إِبْرَاهِيمَ]]، وَبَارِ[q[كْ]] عَلَىٰ سَيِّدِنَا مُحَ[g[مَّ]][a[دٍ]] [a[وَعَلَىٰ]] [o[آلِ]] سَيِّدِنَا مُحَ[g[مَّ]][f[دٍ]] [f[كَ]]مَا بَارَ[q[كْ]]تَ عَلَىٰ سَيِّدِنَا [o[إِبْرَاهِيمَ]] [o[وَعَلَىٰ]] [o[آلِ]] سَيِّدِنَا [o[إِبْرَاهِيمَ]]، فِي الْعَالَمِينَ [g[إِنَّ]]كَ حَمِي[q[دٌ]] [p[مَّجِيدٌ]]",
    'transliteration': "Allāhumma ṣalli ‘alā sayyidinā Muḥammadiw wa ‘alā āli sayyidinā Muḥammadin kamā ṣallaita ‘alā sayyidinā Ibrāhīma wa ‘alā āli sayyidinā Ibrāhīm, wa bārik ‘alā sayyidinā Muḥammadiw wa ‘alā āli sayyidinā Muḥammadin kamā bārakta ‘alā sayyidinā Ibrāhīma wa ‘alā āli sayyidinā Ibrāhīm, fil-‘ālamīna innaka ḥamīdum majīd (10x).",
    'translationId': "Ya Allah, limpahkanlah rahmat kepada junjungan kami Nabi Muhammad dan kepada keluarga junjungan kami Nabi Muhammad, sebagaimana Engkau telah melimpahkan rahmat kepada junjungan kami Nabi Ibrahim dan kepada keluarga junjungan kami Nabi Ibrahim. Dan berkahilah junjungan kami Nabi Muhammad dan keluarga junjungan kami Nabi Muhammad, sebagaimana Engkau telah memberkahi junjungan kami Nabi Ibrahim dan keluarga junjungan kami Nabi Ibrahim. Di seluruh alam semesta, sesungguhnya Engkau Maha Terpuji lagi Maha Mulia (dibaca 10 kali).",
    'translationEn': "O Allah, bestow Your blessings upon our Master Muhammad and the family of our Master Muhammad as You bestowed blessings upon our Master Abraham and the family of our Master Abraham...",
    'repeatCount': 10,
    'sourceNote': "HR. Muslim no. 405, Al-Ma'tsurat Hasan Al-Banna",
    'fadhilah': "Mendapatkan syafa'at Rasulullah SAW di hari kiamat dan Allah balas dengan sepuluh rahmat."
}

new_26 = {
    'title': "Tahlil Sempurna (10x)",
    'arabicText': "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ يُحْيِي وَيُمِيتُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
    'tajwidTags': "[o[لَا]] [o[إِلَٰهَ]] [o[إِلَّا]] اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ يُحْيِي وَيُمِيتُ وَهُوَ عَلَىٰ كُلِّ شَيْ[f[ءٍ]] [f[قَ]]دِيرٌ",
    'transliteration': "Lā ilāha illallāhu waḥdahū lā syarīka lah, lahul-mulku wa lahul-ḥamdu yuḥyī wa yumītu wa huwa ‘alā kulli syai’ing qadīr (10x).",
    'translationId': "Tidak ada tuhan selain Allah semata, tiada sekutu bagi-Nya. Milik-Nyalah segala kerajaan dan pujian, Dia yang menghidupkan dan mematikan, dan Dia Mahakuasa atas segala sesuatu (dibaca 10 kali).",
    'translationEn': "None has the right to be worshipped except Allah alone, without partner. To Him belongs sovereignty and praise, He gives life and causes death, and He is over all things omnipotent (10 times).",
    'repeatCount': 10,
    'sourceNote': "HR. An-Nasa'i dalam 'Amalul Yaum wal Lailah no. 86",
    'fadhilah': "Pahala memerdekakan empat orang budak dari keturunan Nabi Ismail AS dan perlindungan dari godaan setan."
}

new_27 = {
    'title': "Ayat Kerajaan & Kekuasaan Mutlak (QS. Ali 'Imran: 26-27)",
    'arabicText': "قُلِ اللَّهُمَّ مَالِكَ الْمُلْكِ تُؤْتِي الْمُلْكَ مَن تَشَاءُ وَتَنزِعُ الْمُلْكَ مِمَّن تَشَاءُ وَتُعِزُّ مَن تَشَاءُ وَتُذِلُّ مَن تَشَاءُ ۖ بِيَدِكَ الْخَيْرُ ۖ إِنَّكَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ ۝ تُولِجُ اللَّيْلَ فِي النَّهَارِ وَتُولِجُ النَّهَارَ فِي اللَّيْلِ ۖ وَتُخْرِجُ الْحَيَّ مِنَ الْمَيِّتِ وَتُخْرِجُ الْمَيِّتَ مِنَ الْحَيِّ ۖ وَتَرْزُقُ مَن تَشَاءُ بِغَيْرِ حِسَابٍ",
    'tajwidTags': "قُلِ [g[اللَّ]]هُمَّ مَالِكَ الْمُلْكِ تُؤْتِي الْمُلْكَ [a[مَن]] [a[تَ]]شَا[o[ءُ]] [a[وَتَنزِ]]عُ الْمُلْكَ [f[مِمَّن]] [f[تَ]]شَا[o[ءُ]] [a[وَتُعِ]][g[زُّ]] [a[مَن]] [a[تَ]]شَا[o[ءُ]] وَتُذِ[g[لُّ]] [a[مَن]] [a[تَ]]شَا[o[ءُ]] ۖ بِيَدِكَ [p[الْخَيْرُ]] ۖ [g[إِنَّ]]كَ عَلَىٰ كُ[g[لِّ]] شَيْ[f[ءٍ]] [f[قَ]][p[دِيرٌ]] ۝ تُولِجُ [l[اللَّ]]يْلَ فِي [l[النَّ]]هَارِ وَتُولِجُ [l[النَّ]]هَارَ فِي [l[اللَّ]]يْلِ ۖ وَتُ[q[خْ]]رِجُ الْحَ[g[يَّ]] مِنَ الْمَ[g[يِّ]]تِ وَتُ[q[خْ]]رِجُ الْمَ[g[يِّ]]تَ مِنَ الْحَ[g[يِّ]] ۖ وَتَ[q[رْ]]زُقُ [a[مَن]] [a[تَ]]شَا[o[ءُ]] بِغَيْرِ [p[حِسَابٍ]]",
    'transliteration': "Qulillāhumma mālikal-mulki tu’til-mulka man tasyā’u wa tanzi‘ul-mulka mimman tasyā’u wa tu‘izzu man tasyā’u wa tudzillu man tasyā’, biyadikal-khair, innaka ‘alā kulli syai’ing qadīr. Tūlijul-laila fin-nahāri wa tūlijun-nahāra fil-laili wa tukhrijul-ḥayya minal-mayyiti wa tukhrijul-mayyita minal-ḥayyi wa tarzuqu man tasyā’u bi-ghairi ḥisāb.",
    'translationId': "Katakanlah (Muhammad): 'Wahai Tuhan Pemilik kekuasaan, Engkau berikan kekuasaan kepada siapa pun yang Engkau kehendaki, dan Engkau cabut kekuasaan dari siapa pun yang Engkau kehendaki. Engkau muliakan siapa pun yang Engkau kehendaki dan Engkau hinakan siapa pun yang Engkau kehendaki. Di tangan-Mulah segala kebajikan. Sungguh, Engkau Mahakuasa atas segala sesuatu. Engkau masukkan malam ke dalam siang dan Engkau masukkan siang ke dalam malam. Dan Engkau keluarkan yang hidup dari yang mati, dan Engkau keluarkan yang mati dari yang hidup. Dan Engkau berikan rezeki kepada siapa yang Engkau kehendaki tanpa perhitungan' (QS. Ali 'Imran: 26-27).",
    'translationEn': "Say, 'O Allah, Owner of Sovereignty, You give sovereignty to whom You will and You take sovereignty away from whom You will. You honor whom You will and You humble whom You will. In Your hand is [all] good. Indeed, You are over all things omnipotent...' (QS. Ali 'Imran: 26-27).",
    'repeatCount': 1,
    'sourceNote': "QS. Ali 'Imran: 26-27, Al-Ma'tsurat Hasan Al-Banna",
    'fadhilah': "Pengakuan kekuasaan mutlak Allah atas pergantian masa, pelunasan hutang sebesar gunung, dan limpahan rezeki tak terduga."
}

def format_item(item_id, order, data):
    fadhilah_str = data.get("fadhilah", "")
    if fadhilah_str:
        fadhilah_line = f',\n                fadhilah = "{fadhilah_str}"'
    else:
        fadhilah_line = ""
    return f'''            DzikirItem(
                id = "{item_id}",
                category = DzikirCategory.AL_MATSURAT,
                orderNumber = {order},
                title = "{data["title"]}",
                arabicText = "{data["arabicText"]}",
                tajwidTags = "{data["tajwidTags"]}",
                transliteration = "{data["transliteration"]}",
                translationId = "{data["translationId"]}",
                translationEn = "{data["translationEn"]}",
                repeatCount = {data["repeatCount"]},
                sourceNote = "{data["sourceNote"]}"{fadhilah_line}
            )'''

def tuple_to_dict(t):
    return {
        'title': t[1],
        'arabicText': t[2],
        'tajwidTags': t[3],
        'transliteration': t[4],
        'translationId': t[5],
        'translationEn': t[6],
        'repeatCount': int(t[7]),
        'sourceNote': t[8],
        'fadhilah': t[9] if len(t) > 9 else ""
    }

order_map = []
# 1..12: existing 1..12
for i in range(1, 13):
    order_map.append((f"matsurat_{i:02d}", len(order_map)+1, tuple_to_dict(existing_by_num[i])))

# 13: new_13
order_map.append(("matsurat_13", 13, new_13))
# 14: new_14
order_map.append(("matsurat_14", 14, new_14))

# 15..21: existing 13..19
for i in range(13, 20):
    order_map.append((f"matsurat_{len(order_map)+1:02d}", len(order_map)+1, tuple_to_dict(existing_by_num[i])))

# 22: existing 20 (Sayyidul Istighfar)
order_map.append(("matsurat_22", 22, tuple_to_dict(existing_by_num[20])))

# 23: new_23 (Tasbih 100x)
order_map.append(("matsurat_23", 23, new_23))
# 24: new_24 (Istighfar 100x)
order_map.append(("matsurat_24", 24, new_24))
# 25: new_25 (Shalawat 10x)
order_map.append(("matsurat_25", 25, new_25))
# 26: new_26 (Tahlil 10x)
order_map.append(("matsurat_26", 26, new_26))
# 27: new_27 (Ayat Mulk)
order_map.append(("matsurat_27", 27, new_27))
# 28: existing 21 (Doa Rabithah)
order_map.append(("matsurat_28", 28, tuple_to_dict(existing_by_num[21])))

formatted_blocks = [format_item(item_id, ord_num, d) for item_id, ord_num, d in order_map]
all_matsurat_str = ",\n".join(formatted_blocks)

with open('scripts/generated_matsurat_28.txt', 'w', encoding='utf-8') as out:
    out.write(all_matsurat_str)

print(f"Generated 28 items for AL_MATSURAT. Total chars: {len(all_matsurat_str)}")
