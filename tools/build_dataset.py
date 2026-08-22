#!/usr/bin/env python3
"""
Esnaf Lokantaları - veri derleyici.

tools/data/cities.csv + tools/data/restaurants.csv dosyalarını okur ve
uygulamanın içine gömülen app/src/main/assets/restaurants.json dosyasını üretir.

Kullanım:
    python3 tools/build_dataset.py

Ayda bir güncelleme yapmak için:
    1. tools/data/restaurants.csv dosyasını Excel/Numbers ile aç, satır ekle.
    2. Bu betiği çalıştır.
    3. Uygulamayı yeniden derle.

Hiçbir internet bağlantısı veya API anahtarı gerekmez.
"""

import csv
import json
import re
import sys
import unicodedata
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CITIES_CSV = ROOT / "tools" / "data" / "cities.csv"
RESTAURANTS_CSV = ROOT / "tools" / "data" / "restaurants.csv"
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "restaurants.json"

# Sitede olduğu gibi: bu puanın altındaki mekanlar listeye girmez.
MIN_RATING = 4.3

TR_MAP = str.maketrans({
    "ç": "c", "Ç": "c", "ğ": "g", "Ğ": "g", "ı": "i", "I": "i",
    "İ": "i", "i": "i", "ö": "o", "Ö": "o", "ş": "s", "Ş": "s",
    "ü": "u", "Ü": "u",
})


def slugify(value: str) -> str:
    value = value.translate(TR_MAP).lower()
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode()
    value = re.sub(r"[^a-z0-9]+", "-", value).strip("-")
    return value


def parse_float(value: str):
    value = (value or "").strip().replace(",", ".")
    if not value:
        return None
    try:
        return float(value)
    except ValueError:
        return None


def parse_int(value: str):
    value = (value or "").strip().replace(".", "").replace(" ", "")
    if not value:
        return None
    try:
        return int(value)
    except ValueError:
        return None


def read_cities():
    cities = []
    with CITIES_CSV.open(encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            name = row["il"].strip()
            if not name:
                continue
            cities.append({
                "name": name,
                "slug": (row.get("slug") or "").strip() or slugify(name),
                "plate": parse_int(row.get("plaka")),
                "tagline": (row.get("tanitim") or "").strip(),
                "restaurants": [],
            })
    return cities


def read_restaurants():
    rows = []
    with RESTAURANTS_CSV.open(encoding="utf-8") as handle:
        for line_no, row in enumerate(csv.DictReader(handle), start=2):
            name = (row.get("ad") or "").strip()
            city = (row.get("il") or "").strip()
            if not name or not city:
                continue

            rating = parse_float(row.get("puan"))
            if rating is not None and rating < MIN_RATING:
                print(
                    f"  atlandı (puan {rating} < {MIN_RATING}): {name} — satır {line_no}",
                    file=sys.stderr,
                )
                continue

            tags = [t.strip() for t in (row.get("etiketler") or "").split("|") if t.strip()]

            rows.append({
                "city": city,
                "data": {
                    "id": f"{slugify(city)}-{slugify(name)}",
                    "name": name,
                    "category": (row.get("kategori") or "Lokanta").strip(),
                    "tags": tags,
                    "rating": rating,
                    "reviewCount": parse_int(row.get("yorum_sayisi")),
                    "address": (row.get("adres") or "").strip(),
                    "phone": (row.get("telefon") or "").strip() or None,
                    "priceLevel": parse_int(row.get("fiyat")),
                    "latitude": parse_float(row.get("enlem")),
                    "longitude": parse_float(row.get("boylam")),
                    "mapsUrl": (row.get("maps_url") or "").strip() or None,
                    "photoUrl": (row.get("foto_url") or "").strip() or None,
                    "note": (row.get("not") or "").strip() or None,
                },
            })
    return rows


def main() -> int:
    if not CITIES_CSV.exists() or not RESTAURANTS_CSV.exists():
        print("HATA: tools/data içindeki CSV dosyaları bulunamadı.", file=sys.stderr)
        return 1

    cities = read_cities()
    by_name = {c["name"]: c for c in cities}

    unknown = set()
    for row in read_restaurants():
        city = by_name.get(row["city"])
        if city is None:
            unknown.add(row["city"])
            continue
        city["restaurants"].append(row["data"])

    for city in cities:
        # Site ile aynı mantık: önce puan, sonra yorum sayısı.
        city["restaurants"].sort(
            key=lambda r: (r["rating"] or 0, r["reviewCount"] or 0),
            reverse=True,
        )

    payload = {
        "version": 1,
        "updatedAt": date.today().isoformat(),
        "minRating": MIN_RATING,
        "cities": cities,
    }

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")

    total = sum(len(c["restaurants"]) for c in cities)
    filled = sum(1 for c in cities if c["restaurants"])

    if unknown:
        print(f"UYARI: cities.csv'de olmayan iller atlandı: {', '.join(sorted(unknown))}", file=sys.stderr)

    print(f"✓ {OUTPUT.relative_to(ROOT)} yazıldı")
    print(f"  {len(cities)} il, {total} lokanta ({filled} ilde veri var, {len(cities) - filled} il boş)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
