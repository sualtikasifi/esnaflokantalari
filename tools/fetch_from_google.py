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
    "sulu yemek {city}",
    "esnaf kebap salonu {city}",
]

MIN_RATING = 4.3
TOP_N_PER_CITY = 10

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
        return 500
    if city in MID_CITIES:
        return 200
    return 50


# Zincirler esnaf lokantası değil — elenir.
CHAIN_KEYWORDS = [
    "burger king", "mcdonald", "domino", "pizza hut", "popeyes", "kfc",
    "komagene", "baydöner", "bay döner", "tavuk dünyası", "simit sarayı",
    "starbucks", "little caesars", "sbarro", "arby", "subway", "usta dönerci",
    "big chefs", "midpoint", "kahve dünyası", "çiğköftem", "cigkoftem",
    "burger king", "carl's jr", "tostçu", "waffle",
]

# Bu türler birincil tür olarak gelirse esnaf lokantası saymıyoruz.
EXCLUDED_TYPES = {
    "cafe", "coffee_shop", "bar", "bakery", "fast_food_restaurant",
    "meal_delivery", "meal_takeaway", "ice_cream_shop", "night_club",
    "sandwich_shop", "pizza_restaurant", "hamburger_restaurant",
    "dessert_shop", "juice_shop", "tea_house",
}

PRICE_LEVELS = {
    "PRICE_LEVEL_INEXPENSIVE": 1,
    "PRICE_LEVEL_MODERATE": 2,
    "PRICE_LEVEL_EXPENSIVE": 3,
    "PRICE_LEVEL_VERY_EXPENSIVE": 4,
}


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
        print(f"    HATA {error.code}: {body[:300]}", file=sys.stderr)
        if error.code in (401, 403):
            raise SystemExit(
                "\nAPI anahtarı reddedildi. Google Cloud Console'da:\n"
                "  1) 'Places API (New)' etkin mi?\n"
                "  2) Anahtarın API kısıtlamalarında 'Places API (New)' seçili mi?\n"
            )
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
        if reviews < min_reviews_for(city):
            continue
        if is_chain(name):
            continue
        if types & EXCLUDED_TYPES:
            continue
        # Adres il adını içermiyorsa büyük ihtimalle başka ilden gelmiş.
        address = place.get("formattedAddress", "")
        if city.lower() not in address.lower():
            continue

        kept.append({
            "il": city,
            "ad": name,
            "kategori": (place.get("primaryTypeDisplayName") or {}).get("text", "Lokanta"),
            "etiketler": "",
            "puan": rating,
            "yorum_sayisi": reviews,
            "adres": address,
            "telefon": place.get("internationalPhoneNumber", ""),
            "fiyat": PRICE_LEVELS.get(place.get("priceLevel", ""), ""),
            "enlem": (place.get("location") or {}).get("latitude", ""),
            "boylam": (place.get("location") or {}).get("longitude", ""),
            "maps_url": place.get("googleMapsUri", ""),
            "foto_url": "",
            "not": "",
            "_skor": score(rating, reviews),
        })

    kept.sort(key=lambda row: row["_skor"], reverse=True)
    return kept[:TOP_N_PER_CITY]


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
    for index, city in enumerate(cities, start=1):
        print(f"[{index}/{len(cities)}] {city}...", flush=True)
        rows = collect_city(city, api_key, delay=0.3)
        print(f"    {len(rows)} lokanta seçildi")
        all_rows.extend(rows)

    columns = [
        "il", "ad", "kategori", "etiketler", "puan", "yorum_sayisi", "adres",
        "telefon", "fiyat", "enlem", "boylam", "maps_url", "foto_url", "not",
    ]
    with OUTPUT_CSV.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns)
        writer.writeheader()
        for row in all_rows:
            row.pop("_skor", None)
            writer.writerow(row)

    print(f"\n✓ {OUTPUT_CSV.relative_to(ROOT)} yazıldı — {len(all_rows)} lokanta")
    print("Şimdi: python3 tools/build_dataset.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
