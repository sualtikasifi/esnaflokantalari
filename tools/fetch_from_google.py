#!/usr/bin/env python3
"""
TEK SEFERLİK veri toplayıcı — Google Places API (New).

Bu betik uygulamanın parçası DEĞİLDİR. Uygulama çalışırken hiçbir ağ isteği
yapmaz. Bu araç sadece senin bilgisayarında, ayda bir çalıştırılır ve
tools/data/restaurants.csv dosyasını üretir.

Kullanım:
    export MAPS_API_KEY="anahtarin"
    python3 tools/fetch_from_google.py                # tüm iller
    python3 tools/fetch_from_google.py Adana Ankara   # sadece belirli iller
    python3 tools/fetch_from_google.py --dry-run      # kaç istek atılacağını göster

Sonra:
    python3 tools/build_dataset.py
"""

import csv
import json
import math
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CITIES_CSV = ROOT / "tools" / "data" / "cities.csv"
OUTPUT_CSV = ROOT / "tools" / "data" / "restaurants.csv"

ENDPOINT = "https://places.googleapis.com/v1/places:searchText"


class QuotaExceeded(Exception):
    """Google'ın günlük istek kotası doldu — kalan iller yarın çekilmeli."""

FIELD_MASK = ",".join([
    "places.id",
    "places.displayName",
    "places.formattedAddress",
    "places.location",
    "places.rating",
    "places.userRatingCount",
    "places.priceLevel",
    "places.primaryTypeDisplayName",
    "places.types",
    "places.businessStatus",
    "places.internationalPhoneNumber",
    "places.googleMapsUri",
])

# Her il için denenecek aramalar. Sadece "restoran" demiyoruz —
# pizzacı/kafe/zincir gelmesin diye esnaf lokantasına özgü terimler kullanıyoruz.
QUERIES = [
    "esnaf lokantası {city}",
    "ev yemekleri lokantası {city}",
    "sulu yemek lokantası {city}",
    "esnaf kebap salonu {city}",
    "çorbacı {city}",
    "en iyi lokanta {city}",
]

MIN_RATING = 4.3
TOP_N_PER_CITY = 25

# Her ilde en az bu kadar mekan çıkmasını hedefliyoruz. Hedefe ulaşılmazsa
# yorum sayısı eşiği kademeli olarak gevşetilir (puan eşiği asla düşmez).
TARGET_PER_CITY = 20

# Yorum sayısı eşiği: büyük şehirlerde yüksek, küçük illerde düşük olmalı;
# yoksa küçük iller tamamen boş kalır.
BIG_CITIES = {
    "İstanbul", "Ankara", "İzmir", "Bursa", "Antalya",
    "Adana", "Konya", "Gaziantep",
}
MID_CITIES = {
    "Mersin", "Kayseri", "Eskişehir", "Diyarbakır", "Samsun", "Denizli",
    "Şanlıurfa", "Malatya", "Trabzon", "Erzurum", "Van", "Kocaeli",
    "Sakarya", "Hatay", "Manisa", "Balıkesir", "Kahramanmaraş", "Aydın",
}


def min_reviews_for(city: str) -> int:
    if city in BIG_CITIES:
        return 300
    if city in MID_CITIES:
        return 120
    return 30


# Zincirler esnaf lokantası değil — elenir.
CHAIN_KEYWORDS = [
    "burger king", "mcdonald", "domino", "pizza hut", "popeyes", "kfc",
    "komagene", "baydöner", "bay döner", "tavuk dünyası", "simit sarayı",
    "starbucks", "little caesars", "sbarro", "arby", "subway", "usta dönerci",
    "big chefs", "midpoint", "kahve dünyası", "çiğköftem", "cigkoftem",
    "burger king", "carl's jr", "tostçu", "waffle",
]

# Google'ın kategorisi "Restoran" dese bile adında bunlar geçen yerler
# esnaf lokantası değil.
EXCLUDED_NAME_WORDS = [
    "kafe", "cafe", "coffee", "pastane", "patisserie", "tatlıcı",
    "dondurma", "kokteyl", "cocktail", "bistro", "lounge", "pub",
]

# Bu türler birincil tür olarak gelirse esnaf lokantası saymıyoruz.
EXCLUDED_TYPES = {
    "cafe", "coffee_shop", "bar", "bakery", "fast_food_restaurant",
    "meal_delivery", "meal_takeaway", "ice_cream_shop", "night_club",
    "sandwich_shop", "pizza_restaurant", "hamburger_restaurant",
    "dessert_shop", "juice_shop", "tea_house",
}

# Google'ın kategorisi bunlardan biriyse esnaf lokantası saymıyoruz
# (tatlıcı, kafe, pastane vb. `types` alanında her zaman yakalanmıyor).
EXCLUDED_CATEGORIES = {
    "tatlıcı", "pastane", "kafe", "kahveci", "dondurmacı",
    "börekçi", "simitçi", "fırın", "bar", "pub",
}

PRICE_LEVELS = {
    "PRICE_LEVEL_INEXPENSIVE": 1,
    "PRICE_LEVEL_MODERATE": 2,
    "PRICE_LEVEL_EXPENSIVE": 3,
    "PRICE_LEVEL_VERY_EXPENSIVE": 4,
}

# Google'ın kategorisi çoğunlukla düz "Restoran" geliyor; uygulamadaki filtre
# çiplerinin işe yaraması için mekan adından anlamlı etiketler türetiyoruz.
TAG_RULES = [
    ("Kebap", ("kebap", "kebab", "ocakbaşı", "ocakbasi")),
    ("Çorba", ("çorba", "corba", "paça", "paca", "beyran", "işkembe", "iskembe", "kelle")),
    ("Lahmacun & Pide", ("lahmacun", "pide", "pidecı", "pideci")),
    ("Ciğer", ("ciğer", "ciger")),
    ("Köfte", ("köfte", "kofte")),
    ("Mantı", ("mantı", "manti")),
    ("Büryan", ("büryan", "buryan")),
    ("Döner", ("döner", "doner", "dürüm", "durum")),
    ("Tantuni", ("tantuni",)),
    ("Balık", ("balık", "balik")),
    ("Kahvaltı", ("kahvaltı", "kahvalti", "serpme")),
    ("Et", ("kasap", "et lokantası", "et restaurant", "steak")),
    ("Sulu Yemek", ("lokanta", "sofra", "ev yemek", "sulu yemek", "aşevi", "asevi", "esnaf")),
]


def derive_tags(name: str, category: str) -> str:
    """Ada ve kategoriye bakarak etiket üretir, `|` ile ayırır."""
    haystack = f"{name} {category}".lower()
    tags = [tag for tag, keywords in TAG_RULES if any(k in haystack for k in keywords)]

    if not tags:
        # Hiçbir kural tutmadıysa Google'ın kategorisine düş.
        cleaned = category.strip()
        if cleaned and cleaned.lower() != "restoran":
            tags = [cleaned]
        else:
            tags = ["Lokanta"]

    return "|".join(tags[:3])


def clean_maps_url(url: str) -> str:
    """Google'ın izleme parametrelerini (g_mp) at, sade bağlantı bırak."""
    return url.split("&g_mp=")[0] if url else ""


def read_cities():
    with CITIES_CSV.open(encoding="utf-8") as handle:
        return [row["il"].strip() for row in csv.DictReader(handle) if row["il"].strip()]


def search(query: str, api_key: str):
    payload = json.dumps({
        "textQuery": query,
        "languageCode": "tr",
        "regionCode": "TR",
    }).encode("utf-8")

    request = urllib.request.Request(
        ENDPOINT,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "X-Goog-Api-Key": api_key,
            "X-Goog-FieldMask": FIELD_MASK,
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.loads(response.read().decode("utf-8")).get("places", [])
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        if error.code in (401, 403):
            print(f"    HATA {error.code}: {body[:300]}", file=sys.stderr)
            raise SystemExit(
                "\nAPI anahtarı reddedildi. Google Cloud Console'da:\n"
                "  1) 'Places API (New)' etkin mi?\n"
                "  2) Anahtarın API kısıtlamalarında 'Places API (New)' seçili mi?\n"
            )
        if error.code == 429:
            raise QuotaExceeded()
        print(f"    HATA {error.code}: {body[:300]}", file=sys.stderr)
        return []
    except Exception as error:  # ağ hatası
        print(f"    HATA: {error}", file=sys.stderr)
        return []


def is_chain(name: str) -> bool:
    lowered = name.lower()
    return any(keyword in lowered for keyword in CHAIN_KEYWORDS)


def score(rating: float, reviews: int) -> float:
    """Sitedeki sıralama skoruyla aynı: puan × log10(yorum sayısı)."""
    return rating * math.log10(max(reviews, 10))


def collect_city(city: str, api_key: str, delay: float):
    found = {}
    for template in QUERIES:
        query = template.format(city=city)
        for place in search(query, api_key):
            place_id = place.get("id")
            if place_id and place_id not in found:
                found[place_id] = place
        time.sleep(delay)

    # Hedef sayıya ulaşana kadar yorum eşiğini kademeli gevşet.
    # Puan eşiği (4.3) hiçbir zaman düşürülmez — kalite kuralı korunur.
    base_threshold = min_reviews_for(city)
    for divisor in (1, 2, 4, 10):
        threshold = max(base_threshold // divisor, 10)
        kept = filter_places(found, city, threshold)
        if len(kept) >= TARGET_PER_CITY:
            break

    kept.sort(key=lambda row: row["_skor"], reverse=True)
    return kept[:TOP_N_PER_CITY]


def filter_places(found: dict, city: str, min_reviews: int):
    kept = []
    for place in found.values():
        name = (place.get("displayName") or {}).get("text", "").strip()
        rating = place.get("rating")
        reviews = place.get("userRatingCount") or 0
        types = set(place.get("types") or [])

        if not name or rating is None:
            continue
        if place.get("businessStatus") != "OPERATIONAL":
            continue
        if rating < MIN_RATING:
            continue
        if reviews < min_reviews:
            continue
        if is_chain(name):
            continue
        if any(word in name.lower() for word in EXCLUDED_NAME_WORDS):
            continue
        if types & EXCLUDED_TYPES:
            continue
        category_raw = (place.get("primaryTypeDisplayName") or {}).get("text", "")
        if category_raw.strip().lower() in EXCLUDED_CATEGORIES:
            continue
        # Adres il adını içermiyorsa büyük ihtimalle başka ilden gelmiş.
        address = place.get("formattedAddress", "")
        if city.lower() not in address.lower():
            continue

        category = (place.get("primaryTypeDisplayName") or {}).get("text", "Lokanta")
        kept.append({
            "il": city,
            "ad": name,
            "kategori": category,
            "etiketler": derive_tags(name, category),
            "puan": rating,
            "yorum_sayisi": reviews,
            "adres": address,
            "telefon": place.get("internationalPhoneNumber", ""),
            "fiyat": PRICE_LEVELS.get(place.get("priceLevel", ""), ""),
            "enlem": (place.get("location") or {}).get("latitude", ""),
            "boylam": (place.get("location") or {}).get("longitude", ""),
            "maps_url": clean_maps_url(place.get("googleMapsUri", "")),
            "foto_url": "",
            "not": "",
            "_skor": score(rating, reviews),
        })

    return kept


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    dry_run = "--dry-run" in sys.argv

    cities = args or read_cities()

    if dry_run:
        print(f"{len(cities)} il × {len(QUERIES)} sorgu = {len(cities) * len(QUERIES)} istek")
        print("Google'ın Enterprise katmanında aylık 1.000 istek ücretsiz.")
        return 0

    api_key = os.environ.get("MAPS_API_KEY", "").strip()
    if not api_key:
        print("HATA: MAPS_API_KEY ortam değişkeni tanımlı değil.", file=sys.stderr)
        return 1

    all_rows = []
    remaining = []
    for index, city in enumerate(cities, start=1):
        if remaining:
            remaining.append(city)
            continue
        print(f"[{index}/{len(cities)}] {city}...", flush=True)
        try:
            rows = collect_city(city, api_key, delay=0.3)
        except QuotaExceeded:
            print("\n⚠ Google'ın GÜNLÜK istek kotası doldu.", file=sys.stderr)
            remaining.append(city)
            continue
        print(f"    {len(rows)} lokanta seçildi")
        all_rows.extend(rows)

    columns = [
        "il", "ad", "kategori", "etiketler", "puan", "yorum_sayisi", "adres",
        "telefon", "fiyat", "enlem", "boylam", "maps_url", "foto_url", "not",
    ]

    for row in all_rows:
        row.pop("_skor", None)

    # Mevcut CSV'yi koru: sadece bu çalıştırmada çekilen illerin satırları
    # yenilenir, diğer illerin verisi olduğu gibi kalır. Böylece kota
    # nedeniyle yarıda kalan çekimi ertesi gün tamamlayabilirsin.
    fetched_cities = {row["il"] for row in all_rows}
    preserved = []
    if OUTPUT_CSV.exists():
        with OUTPUT_CSV.open(encoding="utf-8") as handle:
            for row in csv.DictReader(handle):
                if row.get("il") and row["il"] not in fetched_cities:
                    preserved.append({key: row.get(key, "") for key in columns})

    merged = preserved + all_rows
    merged.sort(key=lambda row: (row["il"], row["ad"]))

    with OUTPUT_CSV.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        writer.writerows(merged)

    print(f"\n✓ {OUTPUT_CSV.relative_to(ROOT)} yazıldı")
    print(f"  bu çalıştırmada: {len(all_rows)} lokanta ({len(fetched_cities)} il)")
    if preserved:
        print(f"  korunan önceki veri: {len(preserved)} lokanta")
    print(f"  toplam: {len(merged)} lokanta")

    if remaining:
        print(f"\n⚠ {len(remaining)} il kota nedeniyle çekilemedi.")
        print("Kota yarın sıfırlanır. Sadece kalanları çekmek için:\n")
        print("  python3 tools/fetch_from_google.py " + " ".join(f'"{c}"' for c in remaining))
        print("\n(Mevcut veriler korunur, üzerine yazılmaz.)")

    print("\nŞimdi: python3 tools/build_dataset.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
