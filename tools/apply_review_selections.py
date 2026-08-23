#!/usr/bin/env python3
"""
tools/build_review_artifact.py ile yayımlanan sayfada yapılan seçimleri
uygulamaya işler: her "checked" radyo için tam çözünürlüklü fotoğrafı indirir,
sıkıştırır, app/src/main/assets/photos/ içine kaydeder. "Hiçbiri uygun değil"
seçilenler tools/data/photo_review_skipped.json listesine eklenir. İşlenen
lokantalar manifest.json'dan ve aday küçük resimlerinden temizlenir.

Kullanım:
    export MAPS_API_KEY="anahtarin"   # local.properties'te varsa gerekmez
    python3 tools/apply_review_selections.py SAYFA.html
"""

import io
import json
import os
import sys
import urllib.error
import urllib.request
from html.parser import HTMLParser
from pathlib import Path

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parent.parent
PHOTOS_DIR = ROOT / "app" / "src" / "main" / "assets" / "photos"
CANDIDATES_DIR = ROOT / "tools" / "data" / "photo_candidates"
MANIFEST_PATH = CANDIDATES_DIR / "manifest.json"
SKIPPED_PATH = ROOT / "tools" / "data" / "photo_review_skipped.json"
LOCAL_PROPERTIES = ROOT / "local.properties"

MAX_EDGE_PX = 1440
JPEG_QUALITY = 88


def resolve_api_key() -> str:
    key = os.environ.get("MAPS_API_KEY", "").strip()
    if key:
        return key
    if LOCAL_PROPERTIES.exists():
        for line in LOCAL_PROPERTIES.read_text(encoding="utf-8").splitlines():
            if line.strip().startswith("MAPS_API_KEY="):
                return line.split("=", 1)[1].strip()
    return ""


class RadioParser(HTMLParser):
    """<input name="pick-{id}" value="{index|none}" data-ref="..." checked> yakalar."""

    def __init__(self):
        super().__init__()
        self.picks: dict[str, dict] = {}

    def handle_starttag(self, tag, attrs):
        if tag != "input":
            return
        attrs = dict(attrs)
        name = attrs.get("name", "")
        if not name.startswith("pick-"):
            return
        if "checked" not in attrs:
            return
        restaurant_id = name[len("pick-"):]
        self.picks[restaurant_id] = {
            "value": attrs.get("value", ""),
            "ref": attrs.get("data-ref", ""),
        }


def load_manifest() -> dict:
    if MANIFEST_PATH.exists():
        return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    return {}


def save_manifest(manifest: dict) -> None:
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")


def load_skipped() -> list:
    if SKIPPED_PATH.exists():
        return json.loads(SKIPPED_PATH.read_text(encoding="utf-8"))
    return []


def save_skipped(items: list) -> None:
    SKIPPED_PATH.write_text(json.dumps(items, ensure_ascii=False, indent=2), encoding="utf-8")


def remove_candidate_files(restaurant_id: str) -> None:
    restaurant_dir = CANDIDATES_DIR / restaurant_id
    if restaurant_dir.exists():
        for f in restaurant_dir.glob("*.jpg"):
            f.unlink()
        restaurant_dir.rmdir()


def download_full(photo_ref: str, api_key: str) -> bytes:
    url = f"https://places.googleapis.com/v1/{photo_ref}/media?maxWidthPx={MAX_EDGE_PX}&key={api_key}"
    with urllib.request.urlopen(url, timeout=30) as response:
        return response.read()


def save_final_photo(raw: bytes, restaurant_id: str) -> None:
    image = Image.open(io.BytesIO(raw))
    image = ImageOps.exif_transpose(image)
    image = image.convert("RGB")
    width, height = image.size
    if max(width, height) > MAX_EDGE_PX:
        factor = MAX_EDGE_PX / max(width, height)
        image = image.resize(
            (max(int(width * factor), 1), max(int(height * factor), 1)), Image.LANCZOS
        )
    PHOTOS_DIR.mkdir(parents=True, exist_ok=True)
    image.save(PHOTOS_DIR / f"{restaurant_id}.jpg", "JPEG", quality=JPEG_QUALITY, optimize=True)


def main() -> int:
    if len(sys.argv) < 2:
        print("Kullanım: python3 tools/apply_review_selections.py SAYFA.html", file=sys.stderr)
        return 1

    html_path = Path(sys.argv[1])
    if not html_path.exists():
        print(f"HATA: {html_path} bulunamadı.", file=sys.stderr)
        return 1

    api_key = resolve_api_key()
    if not api_key:
        print("HATA: MAPS_API_KEY tanımlı değil (env veya local.properties).", file=sys.stderr)
        return 1

    parser = RadioParser()
    parser.feed(html_path.read_text(encoding="utf-8"))

    if not parser.picks:
        print("Hiçbir seçim bulunamadı (henüz kimse tıklamamış olabilir).")
        return 0

    manifest = load_manifest()
    skipped = load_skipped()

    saved, marked_none, errors = 0, 0, 0
    for restaurant_id, pick in parser.picks.items():
        info = manifest.get(restaurant_id)
        name = info["name"] if info else restaurant_id

        if pick["value"] == "none":
            skipped.append(restaurant_id)
            marked_none += 1
            print(f"⨯ hiçbiri uygun değil: {name}")
        elif pick["ref"]:
            try:
                raw = download_full(pick["ref"], api_key)
                save_final_photo(raw, restaurant_id)
                saved += 1
                print(f"✓ kaydedildi: {name}")
            except Exception as error:
                errors += 1
                print(f"HATA: {name} kaydedilemedi: {error}", file=sys.stderr)
                continue
        else:
            continue

        remove_candidate_files(restaurant_id)
        manifest.pop(restaurant_id, None)

    save_manifest(manifest)
    save_skipped(skipped)

    print(f"\n✓ {saved} fotoğraf kaydedildi, {marked_none} 'hiçbiri' işaretlendi, {errors} hata.")
    print(f"  manifest'te kalan (henüz seçilmemiş): {len(manifest)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
