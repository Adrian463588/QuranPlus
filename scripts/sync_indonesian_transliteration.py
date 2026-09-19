import urllib.request
import json
import sqlite3
import time
import sys

def main():
    db_path = 'app/src/main/assets/databases/quranplus.db'
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    total_updated = 0
    start_time = time.time()

    print("Fetching and updating standard Indonesian Kemenag transliteration for 114 Surahs...")

    for surah_num in range(1, 115):
        url = f'https://equran.id/api/v2/surat/{surah_num}'
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        
        retries = 3
        data = None
        for attempt in range(retries):
            try:
                with urllib.request.urlopen(req, timeout=15) as resp:
                    data = json.loads(resp.read().decode('utf-8'))
                break
            except Exception as e:
                print(f"Error fetching Surah {surah_num} (attempt {attempt+1}): {e}")
                time.sleep(1)

        if not data or 'data' not in data or 'ayat' not in data['data']:
            print(f"FAILED to fetch Surah {surah_num}!")
            sys.exit(1)

        surah_data = data['data']
        ayahs = surah_data['ayat']
        surah_name = surah_data.get('namaLatin', f'Surah {surah_num}')

        for ayah in ayahs:
            ayah_num = ayah['nomorAyat']
            latin = ayah['teksLatin'].strip()
            
            cursor.execute(
                "UPDATE ayahs SET transliteration = ? WHERE surah_id = ? AND ayah_number = ?",
                (latin, surah_num, ayah_num)
            )
            total_updated += 1

        if surah_num % 10 == 0 or surah_num == 114:
            conn.commit()
            print(f"[{surah_num}/114] {surah_name} updated ({len(ayahs)} ayahs). Total so far: {total_updated}")

    # Rebuild FTS table if needed
    print("Updating FTS index with new transliterations...")
    try:
        cursor.execute("INSERT INTO ayahs_fts(ayahs_fts) VALUES('rebuild')")
    except Exception as e:
        print(f"FTS rebuild note: {e}")

    conn.commit()
    conn.close()
    print(f"SUCCESS: {total_updated} ayahs updated in {time.time() - start_time:.2f} seconds.")

if __name__ == '__main__':
    main()
