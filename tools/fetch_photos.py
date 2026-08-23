#!/usr/bin/env python3
"""
TEK SEFERLİK fotoğraf toplayıcı — Google Places API (New).

Bu betik uygulamanın parçası DEĞİLDİR. Uygulama çalışırken hiçbir ağ isteği
yapmaz. Bu araç sadece senin bilgisayarında çalışır: henüz fotoğrafı olmayan
her lokanta için Google'ın kendi Maps verisindeki kapak fotoğrafını indirir,
sıkıştırır ve app/src/main/assets/photos/ içine kaydeder.

Artık haritada tek tek arayıp ekran görüntüsü almana gerek yok.

Kullanım:
    export MAPS_API_KEY="anahtarin"
    python3 tools/fetch_photos.py                 # sırayla tüm eksikleri dener
    python3 tools/fetch_photos.py --limit 30       # günlük kota için küçük parti
    python3 tools/fetch_photos.py --dry-run        # kaç lokantanın fotoğrafı eksik, gösterir

Kota dolarsa betik durur; zaten indirilenler kalıcıdır, ertesi gün tekrar
çalıştırınca sadece eksik kalanlar denenir.
"""

import io
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parent.parent
DATASET = ROOT / "app" / "src" / "main" / "assets" / "restaurants.json"
PHOTOS_DIR = ROOT / "app" / "src" / "main" / "assets" / "photos"

SEARCH_ENDPOINT = "https://places.googleapis.com/v1/places:searchText"
SEARCH_FIELD_MASK = "places.displayName,places.formattedAddress,places.photos"

MAX_EDGE_PX = 1440
JPEG_QUALITY = 88
DELAY_SECONDS = 0.3


class QuotaExceeded(Exception):
    """Google'ın günlük istek kotası doldu — kalanlar yarın denenmeli."""


def load_restaurants():
    data = json.loads(DATASET.read_text(encoding="utf-8"))
    rows = []
    for city in data["cities"]:
        for restaurant in city["restaurants"]:
            rows.append({
                "id": restaurant["id"],
                "name": restaurant["name"],
                "city": city["name"],
                "address": restaurant.get("address", ""),
            })
    return rows


def missing_photos(rows):
    return [r for r in rows if not (PHOTOS_DIR / f"{r['id']}.jpg").exists()]


def search_photo_ref(name: str, city: str, address: str, api_key: str) -> str | None:
    query = f"{name}, {address}" if address else f"{name} {city}"
    payload = json.dumps({
        "textQuery": query,
        "languageCode": "tr",
        "regionCode": "TR",
    }).encode("utf-8")

    request = urllib.request.Request(
        SEARCH_ENDPOINT,
        data=payload,
        headers={
            "Content-Type": "application/json",
            "X-Goog-Api-Key": api_key,
            "X-Goog-FieldMask": SEARCH_FIELD_MASK,
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            places = json.loads(response.read().decode("utf-8")).get("places", [])
    except urllib.error.HTTPError as error:
        if error.code == 429:
            raise QuotaExceeded()
        body = error.read().decode("utf-8", errors="replace")
        print(f"    HATA {error.code}: {body[:200]}", file=sys.stderr)
        return None
    except Exception as error:  # ağ hatası
        print(f"    HATA: {error}", file=sys.stderr)
        return None

    if not places:
        return None
    photos = places[0].get("photos") or []
    if not photos:
        return None
    return photos[0].get("name")  # "places/XXXX/photos/YYYY"


def download_photo(photo_ref: str, api_key: str) -> bytes | None:
    url = (
        f"https://places.googleapis.com/v1/{photo_ref}/media"
        f"?maxWidthPx=1440&key={api_key}"
    )
    request = urllib.request.Request(url, method="GET")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        if error.code == 429:
            raise QuotaExceeded()
        print(f"    HATA (fotoğraf indirme) {error.code}", file=sys.stderr)
        return None
    except Exception as error:
        print(f"    HATA (fotoğraf indirme): {error}", file=sys.stderr)
        return None


def save_compressed(raw: bytes, restaurant_id: str) -> bool:
    try:
        image = Image.open(io.BytesIO(raw))
        image = ImageOps.exif_transpose(image)
        image = image.convert("RGB")
    except Exception as error:
        print(f"    HATA (görsel açılamadı): {error}", file=sys.stderr)
        return False

    width, height = image.size
    if max(width, height) > MAX_EDGE_PX:
        factor = MAX_EDGE_PX / max(width, height)
        image = image.resize(
            (max(int(width * factor), 1), max(int(height * factor), 1)),
            Image.LANCZOS,
        )

    PHOTOS_DIR.mkdir(parents=True, exist_ok=True)
    image.save(PHOTOS_DIR / f"{restaurant_id}.jpg", "JPEG", quality=JPEG_QUALITY, optimize=True)
    return True


def main() -> int:
    dry_run = "--dry-run" in sys.argv
    limit = None
    if "--limit" in sys.argv:
        idx = sys.argv.index("--limit")
        limit = int(sys.argv[idx + 1])

    rows = load_restaurants()
    todo = missing_photos(rows)

    print(f"{len(rows)} lokanta, {len(todo)} tanesinin fotoğrafı eksik.")
    if dry_run:
        return 0
    if not todo:
        print("Eksik yok — hepsinin fotoğrafı var.")
        return 0

    if limit:
        todo = todo[:limit]
        print(f"Bu çalıştırmada en fazla {limit} lokanta denenecek.")

    api_key = os.environ.get("MAPS_API_KEY", "").strip()
    if not api_key:
        print("HATA: MAPS_API_KEY ortam değişkeni tanımlı değil.", file=sys.stderr)
        return 1

    found = 0
    not_found = 0
    for index, row in enumerate(todo, start=1):
        print(f"[{index}/{len(todo)}] {row['name']} ({row['city']})...", flush=True)
        try:
            photo_ref = search_photo_ref(row["name"], row["city"], row["address"], api_key)
            if not photo_ref:
                print("    fotoğraf bulunamadı, atlandı")
                not_found += 1
                time.sleep(DELAY_SECONDS)
                continue

            raw = download_photo(photo_ref, api_key)
            if not raw:
                not_found += 1
                time.sleep(DELAY_SECONDS)
                continue

            if save_compressed(raw, row["id"]):
                found += 1
                print("    ✓ kaydedildi")
        except QuotaExceeded:
            print("\n⚠ Google'ın GÜNLÜK istek kotası doldu.", file=sys.stderr)
            print(f"  Şimdiye kadar: {found} fotoğraf eklendi.", file=sys.stderr)
            print("  Kota yarın sıfırlanır, betiği tekrar çalıştır — kalanlar otomatik denenir.", file=sys.stderr)
            return 0

        time.sleep(DELAY_SECONDS)

    print(f"\n✓ {found} fotoğraf eklendi, {not_found} lokanta için bulunamadı.")
    remaining = len(missing_photos(load_restaurants()))
    print(f"  Kalan eksik: {remaining}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
