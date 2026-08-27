#!/usr/bin/env python3
"""
TEK SEFERLİK ilçe bazlı veri toplayıcı — Google Places API (New).

tools/fetch_from_google.py il genelinde arama yapıyor; büyük illerde bu,
sonuçları birkaç merkez ilçeye yığıyor (ör. İstanbul'da hep Fatih/Beyoğlu
çıkıyor, Silivri/Şile hiç çıkmıyor). Bu betik ilçe ilçe arayarak boşlukları
dolduruyor — ama önce mevcut veride o ilçede zaten yeterli lokanta var mı
diye bakıyor, varsa hiç API isteği atmadan atlıyor (kota israf etmemek için).

Bu betik uygulamanın parçası DEĞİLDİR. Uygulama çalışırken hiçbir ağ isteği
yapmaz.

Kullanım:
    export MAPS_API_KEY="anahtarin"
    python3 tools/fetch_districts.py İstanbul
    python3 tools/fetch_districts.py İstanbul Bursa
    python3 tools/fetch_districts.py --dry-run İstanbul   # hangi ilçeler eksik, gösterir

Sonra:
    python3 tools/build_dataset.py
"""

import csv
import re
import sys
import time
from pathlib import Path

import fetch_from_google as base

ROOT = Path(__file__).resolve().parent.parent
RESTAURANTS_CSV = ROOT / "tools" / "data" / "restaurants.csv"

TARGET_PER_DISTRICT = 3
MAX_PER_DISTRICT = 5
BASE_MIN_REVIEWS = 50
DELAY_SECONDS = 0.3

DISTRICT_QUERIES = [
    "esnaf lokantası {district} {city}",
    "ev yemekleri lokantası {district} {city}",
]

DISTRICT_SUFFIX = re.compile(r"([^\W\d_]+)/[^\W\d_]+\s*$", re.UNICODE)

# Öncelikli illerin resmi ilçe listeleri (bkz. tools/fetch_from_google.py'deki
# PRIORITY_CITIES). Sırayla en kalabalık/turistik illerden başlanıyor.
DISTRICTS = {
    "İstanbul": ["Adalar", "Arnavutköy", "Ataşehir", "Avcılar", "Bağcılar", "Bahçelievler", "Bakırköy", "Başakşehir", "Bayrampaşa", "Beşiktaş", "Beykoz", "Beylikdüzü", "Beyoğlu", "Büyükçekmece", "Çatalca", "Çekmeköy", "Esenler", "Esenyurt", "Eyüpsultan", "Fatih", "Gaziosmanpaşa", "Güngören", "Kadıköy", "Kağıthane", "Kartal", "Küçükçekmece", "Maltepe", "Pendik", "Sancaktepe", "Sarıyer", "Silivri", "Sultanbeyli", "Sultangazi", "Şile", "Şişli", "Tuzla", "Ümraniye", "Üsküdar", "Zeytinburnu"],
    "Ankara": ["Akyurt", "Altındağ", "Ayaş", "Bala", "Beypazarı", "Çamlıdere", "Çankaya", "Çubuk", "Elmadağ", "Etimesgut", "Evren", "Gölbaşı", "Güdül", "Haymana", "Kalecik", "Kahramankazan", "Keçiören", "Kızılcahamam", "Mamak", "Nallıhan", "Polatlı", "Pursaklar", "Sincan", "Şereflikoçhisar", "Yenimahalle"],
    "İzmir": ["Aliağa", "Balçova", "Bayındır", "Bayraklı", "Bergama", "Beydağ", "Bornova", "Buca", "Çeşme", "Çiğli", "Dikili", "Foça", "Gaziemir", "Güzelbahçe", "Karabağlar", "Karaburun", "Karşıyaka", "Kemalpaşa", "Kınık", "Kiraz", "Konak", "Menderes", "Menemen", "Narlıdere", "Ödemiş", "Seferihisar", "Selçuk", "Tire", "Torbalı", "Urla"],
    "Bursa": ["Büyükorhan", "Gemlik", "Gürsu", "Harmancık", "İnegöl", "İznik", "Karacabey", "Keles", "Kestel", "Mudanya", "Mustafakemalpaşa", "Nilüfer", "Orhaneli", "Orhangazi", "Osmangazi", "Yenişehir", "Yıldırım"],
    "Antalya": ["Akseki", "Aksu", "Alanya", "Demre", "Döşemealtı", "Elmalı", "Finike", "Gazipaşa", "Gündoğmuş", "İbradı", "Kaş", "Kemer", "Kepez", "Konyaaltı", "Korkuteli", "Kumluca", "Manavgat", "Muratpaşa", "Serik"],
    "Adana": ["Aladağ", "Ceyhan", "Çukurova", "Feke", "İmamoğlu", "Karaisalı", "Karataş", "Kozan", "Pozantı", "Saimbeyli", "Sarıçam", "Seyhan", "Tufanbeyli", "Yumurtalık", "Yüreğir"],
    "Konya": ["Ahırlı", "Akören", "Akşehir", "Altınekin", "Beyşehir", "Bozkır", "Cihanbeyli", "Çeltik", "Çumra", "Derbent", "Derebucak", "Doğanhisar", "Emirgazi", "Ereğli", "Güneysınır", "Hadim", "Halkapınar", "Hüyük", "Ilgın", "Kadınhanı", "Karapınar", "Karatay", "Kulu", "Meram", "Sarayönü", "Selçuklu", "Seydişehir", "Taşkent", "Tuzlukçu", "Yalıhüyük", "Yunak"],
    "Gaziantep": ["Araban", "İslahiye", "Karkamış", "Nizip", "Nurdağı", "Oğuzeli", "Şahinbey", "Şehitkamil", "Yavuzeli"],
    "Muğla": ["Bodrum", "Dalaman", "Datça", "Fethiye", "Kavaklıdere", "Köyceğiz", "Marmaris", "Menteşe", "Milas", "Ortaca", "Seydikemer", "Ula", "Yatağan"],
    "Nevşehir": ["Acıgöl", "Avanos", "Derinkuyu", "Gülşehir", "Hacıbektaş", "Kozaklı", "Merkez", "Ürgüp"],
    "Trabzon": ["Akçaabat", "Araklı", "Arsin", "Beşikdüzü", "Çarşıbaşı", "Çaykara", "Dernekpazarı", "Düzköy", "Hayrat", "Köprübaşı", "Maçka", "Of", "Ortahisar", "Sürmene", "Şalpazarı", "Tonya", "Vakfıkebir", "Yomra"],
    "Mersin": ["Akdeniz", "Anamur", "Aydıncık", "Bozyazı", "Çamlıyayla", "Erdemli", "Gülnar", "Mezitli", "Mut", "Silifke", "Tarsus", "Toroslar", "Yenişehir"],
    "Kayseri": ["Akkışla", "Bünyan", "Develi", "Felahiye", "Hacılar", "İncesu", "Kocasinan", "Melikgazi", "Özvatan", "Pınarbaşı", "Sarıoğlan", "Sarız", "Talas", "Tomarza", "Yahyalı", "Yeşilhisar"],
}


def extract_district(address: str):
    match = DISTRICT_SUFFIX.search(address.strip())
    return match.group(1) if match else None


def read_existing_rows():
    if not RESTAURANTS_CSV.exists():
        return []
    with RESTAURANTS_CSV.open(encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def covered_districts(rows, city: str) -> dict:
    """{ilçe: kaç lokanta var} — sadece verilen il için."""
    counts = {}
    for row in rows:
        if row.get("il") != city:
            continue
        district = extract_district(row.get("adres", ""))
        if district:
            counts[district] = counts.get(district, 0) + 1
    return counts


def filter_district_places(found: dict, city: str, district: str, min_reviews: int):
    kept = []
    for place in found.values():
        name = base.clean_text((place.get("displayName") or {}).get("text", "").strip())
        rating = place.get("rating")
        reviews = place.get("userRatingCount") or 0
        types = set(place.get("types") or [])

        if not name or rating is None:
            continue
        if place.get("businessStatus") != "OPERATIONAL":
            continue
        if rating < base.MIN_RATING:
            continue
        if reviews < min_reviews:
            continue
        if base.is_chain(name):
            continue
        if any(word in name.lower() for word in base.EXCLUDED_NAME_WORDS):
            continue
        if types & base.EXCLUDED_TYPES:
            continue
        category_raw = (place.get("primaryTypeDisplayName") or {}).get("text", "")
        if category_raw.strip().lower() in base.EXCLUDED_CATEGORIES:
            continue

        address = base.clean_text(place.get("formattedAddress", ""))
        if city.lower() not in address.lower():
            continue
        if district.lower() not in address.lower():
            continue

        category = base.clean_text((place.get("primaryTypeDisplayName") or {}).get("text", "Lokanta"))
        kept.append({
            "il": city,
            "ad": name,
            "kategori": category,
            "etiketler": base.derive_tags(name, category),
            "puan": rating,
            "yorum_sayisi": reviews,
            "adres": address,
            "telefon": place.get("internationalPhoneNumber", ""),
            "fiyat": base.PRICE_LEVELS.get(place.get("priceLevel", ""), ""),
            "enlem": (place.get("location") or {}).get("latitude", ""),
            "boylam": (place.get("location") or {}).get("longitude", ""),
            "maps_url": base.clean_maps_url(place.get("googleMapsUri", "")),
            "foto_url": "",
            "not": "",
            "_skor": base.score(rating, reviews),
        })
    return kept


def collect_district(city: str, district: str, api_key: str):
    found = {}
    for template in DISTRICT_QUERIES:
        query = template.format(district=district, city=city)
        for place in base.search(query, api_key):
            place_id = place.get("id")
            if place_id and place_id not in found:
                found[place_id] = place
        time.sleep(DELAY_SECONDS)

    kept = []
    for divisor in (1, 2, 5, 10):
        threshold = max(BASE_MIN_REVIEWS // divisor, 5)
        kept = filter_district_places(found, city, district, threshold)
        if len(kept) >= TARGET_PER_DISTRICT:
            break

    kept.sort(key=lambda row: row["_skor"], reverse=True)
    return kept[:MAX_PER_DISTRICT]


def main() -> int:
    dry_run = "--dry-run" in sys.argv
    cities = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not cities:
        print("Kullanım: python3 tools/fetch_districts.py İl1 [İl2 ...]", file=sys.stderr)
        return 1

    unknown = [c for c in cities if c not in DISTRICTS]
    if unknown:
        print(f"HATA: ilçe listesi olmayan il(ler): {', '.join(unknown)}", file=sys.stderr)
        return 1

    existing_rows = read_existing_rows()
    existing_pairs = {(row.get("il"), row.get("ad")) for row in existing_rows}

    todo = []
    for city in cities:
        counts = covered_districts(existing_rows, city)
        for district in DISTRICTS[city]:
            have = counts.get(district, 0)
            if have < TARGET_PER_DISTRICT:
                todo.append((city, district, have))

    total_in_scope = sum(len(DISTRICTS[c]) for c in cities)
    print(f"{total_in_scope} ilçe listede ({', '.join(cities)}); "
          f"{len(todo)} tanesi eksik (< {TARGET_PER_DISTRICT} lokanta).")
    for city, district, have in todo:
        print(f"  eksik: {city} / {district} (şu an {have})")

    if dry_run or not todo:
        return 0

    api_key = __import__("os").environ.get("MAPS_API_KEY", "").strip()
    if not api_key:
        print("HATA: MAPS_API_KEY ortam değişkeni tanımlı değil.", file=sys.stderr)
        return 1

    new_rows = []
    remaining = []
    for index, (city, district, have) in enumerate(todo, start=1):
        print(f"[{index}/{len(todo)}] {city} / {district}...", flush=True)
        try:
            found = collect_district(city, district, api_key)
        except base.QuotaExceeded:
            print("\n⚠ Google'ın GÜNLÜK istek kotası doldu.", file=sys.stderr)
            remaining = todo[index - 1:]
            break

        added = 0
        for row in found:
            row.pop("_skor", None)
            key = (row["il"], row["ad"])
            if key in existing_pairs:
                continue
            existing_pairs.add(key)
            new_rows.append(row)
            added += 1
        print(f"    {len(found)} bulundu, {added} yeni eklendi")

    if new_rows:
        columns = [
            "il", "ad", "kategori", "etiketler", "puan", "yorum_sayisi", "adres",
            "telefon", "fiyat", "enlem", "boylam", "maps_url", "foto_url", "not",
        ]
        write_header = not RESTAURANTS_CSV.exists()
        with RESTAURANTS_CSV.open("a", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=columns)
            if write_header:
                writer.writeheader()
            writer.writerows(new_rows)

    print(f"\n✓ {len(new_rows)} yeni lokanta eklendi (tools/data/restaurants.csv'ye eklendi)")
    if remaining:
        print(f"\n⚠ {len(remaining)} ilçe kota nedeniyle çekilemedi.")
        print("Kota yarın sıfırlanır. Betiği aynı illerle tekrar çalıştır — kalan ilçeler otomatik denenir.")

    print("\nŞimdi: python3 tools/build_dataset.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
