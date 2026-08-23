#!/usr/bin/env python3
"""
TEK SEFERLİK fotoğraf ADAY toplayıcı — Google Places API (New).

Bu betik uygulamanın parçası DEĞİLDİR. Uygulama çalışırken hiçbir ağ isteği
yapmaz. Google'ın bir mekan için otomatik seçtiği "kapak" fotoğrafı çoğu
zaman yanlış oluyor (başka bir mekanın fotoğrafı, iç mekan yerine tabela,
vb.) — bu yüzden burada TEK bir fotoğrafı otomatik seçip kaydetmiyoruz.
Bunun yerine her lokanta için birkaç ADAY küçük önizleme indirir; hangisinin
doğru olduğuna sen tools/review_photos.py ile bakıp tek tıkla karar verirsin.

Kullanım:
    export MAPS_API_KEY="anahtarin"
    python3 tools/fetch_photo_candidates.py                # eksik olan herkes için
    python3 tools/fetch_photo_candidates.py --limit 40      # günlük kota için küçük parti
    python3 tools/fetch_photo_candidates.py --dry-run       # kaç lokanta eksik, gösterir

Sonra:
    python3 tools/review_photos.py
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
CANDIDATES_DIR = ROOT / "tools" / "data" / "photo_candidates"
MANIFEST_PATH = CANDIDATES_DIR / "manifest.json"
SKIPPED_PATH = ROOT / "tools" / "data" / "photo_review_skipped.json"

SEARCH_ENDPOINT = "https://places.googleapis.com/v1/places:searchText"
SEARCH_FIELD_MASK = "places.displayName,places.formattedAddress,places.photos"

MAX_CANDIDATES = 6
THUMB_MAX_PX = 480
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


def load_manifest() -> dict:
    if MANIFEST_PATH.exists():
        return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    return {}


def save_manifest(manifest: dict) -> None:
    CANDIDATES_DIR.mkdir(parents=True, exist_ok=True)
    MANIFEST_PATH.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )


def load_skipped() -> set:
    if SKIPPED_PATH.exists():
        return set(json.loads(SKIPPED_PATH.read_text(encoding="utf-8")))
    return set()


def pending(rows, manifest, skipped):
    return [
        r for r in rows
        if not (PHOTOS_DIR / f"{r['id']}.jpg").exists()
        and r["id"] not in manifest
        and r["id"] not in skipped
    ]


def search_photos(name: str, city: str, address: str, api_key: str):
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
        return []
    except Exception as error:  # ağ hatası
        print(f"    HATA: {error}", file=sys.stderr)
        return []

    if not places:
        return []
    return places[0].get("photos") or []


def download_thumb(photo_ref: str, api_key: str) -> bytes | None:
    url = (
        f"https://places.googleapis.com/v1/{photo_ref}/media"
        f"?maxWidthPx={THUMB_MAX_PX}&key={api_key}"
    )
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        if error.code == 429:
            raise QuotaExceeded()
        return None
    except Exception:
        return None


def save_thumb(raw: bytes, restaurant_dir: Path, index: int) -> bool:
    try:
        image = Image.open(io.BytesIO(raw))
        image = ImageOps.exif_transpose(image)
        image = image.convert("RGB")
    except Exception:
        return False
    restaurant_dir.mkdir(parents=True, exist_ok=True)
    image.save(restaurant_dir / f"{index}.jpg", "JPEG", quality=82, optimize=True)
    return True


def main() -> int:
    dry_run = "--dry-run" in sys.argv
    limit = None
    if "--limit" in sys.argv:
        idx = sys.argv.index("--limit")
        limit = int(sys.argv[idx + 1])

    rows = load_restaurants()
    manifest = load_manifest()
    skipped = load_skipped()
    todo = pending(rows, manifest, skipped)

    print(f"{len(rows)} lokanta, {len(todo)} tanesi için aday fotoğraf toplanmamış.")
    if dry_run:
        return 0
    if not todo:
        print("Eksik yok.")
        return 0

    if limit:
        todo = todo[:limit]
        print(f"Bu çalıştırmada en fazla {limit} lokanta denenecek.")

    api_key = os.environ.get("MAPS_API_KEY", "").strip()
    if not api_key:
        print("HATA: MAPS_API_KEY ortam değişkeni tanımlı değil.", file=sys.stderr)
        return 1

    collected = 0
    for index, row in enumerate(todo, start=1):
        print(f"[{index}/{len(todo)}] {row['name']} ({row['city']})...", flush=True)
        try:
            photos = search_photos(row["name"], row["city"], row["address"], api_key)
            if not photos:
                print("    aday bulunamadı")
                time.sleep(DELAY_SECONDS)
                continue

            restaurant_dir = CANDIDATES_DIR / row["id"]
            saved_candidates = []
            for candidate_index, photo in enumerate(photos[:MAX_CANDIDATES], start=1):
                photo_ref = photo.get("name")
                if not photo_ref:
                    continue
                raw = download_thumb(photo_ref, api_key)
                if raw and save_thumb(raw, restaurant_dir, candidate_index):
                    saved_candidates.append({"index": candidate_index, "ref": photo_ref})
                time.sleep(DELAY_SECONDS)

            if saved_candidates:
                manifest[row["id"]] = {
                    "name": row["name"],
                    "city": row["city"],
                    "address": row["address"],
                    "candidates": saved_candidates,
                }
                save_manifest(manifest)
                collected += 1
                print(f"    ✓ {len(saved_candidates)} aday kaydedildi")
            else:
                print("    hiçbir aday indirilemedi")
        except QuotaExceeded:
            print("\n⚠ Google'ın GÜNLÜK istek kotası doldu.", file=sys.stderr)
            print(f"  Şimdiye kadar: {collected} lokanta için aday toplandı.", file=sys.stderr)
            print("  Kota yarın sıfırlanır, betiği tekrar çalıştır.", file=sys.stderr)
            print("\nİncelemek için: python3 tools/review_photos.py")
            return 0

    print(f"\n✓ {collected} lokanta için aday fotoğraf toplandı.")
    print("İncelemek için: python3 tools/review_photos.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
