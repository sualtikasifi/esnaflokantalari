#!/usr/bin/env python3
"""
Google Play mağaza görsellerini üretir.

Üretilenler (store/ klasörüne):
  - icon-512.png          Uygulama simgesi (Play Console zorunlu, 512x512)
  - feature-graphic.png   Öne çıkan görsel (Play Console zorunlu, 1024x500)

Kullanım:
    python3 tools/make_store_assets.py
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
LOGO = ROOT / "tools" / "data" / "logo.png"
OUT = ROOT / "store"

CREAM = (250, 247, 240)
GOLD = (176, 141, 70)
DARK = (36, 26, 21)

FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf",
]
BODY_FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
]


def font(paths, size):
    for path in paths:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    raise SystemExit("Uygun yazı tipi bulunamadı.")


def transparent_logo() -> Image.Image:
    image = Image.open(LOGO).convert("RGBA")
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, _ = pixels[x, y]
            brightness = (r + g + b) / 3
            if brightness > 225:
                pixels[x, y] = (r, g, b, 0)
            elif brightness > 195:
                pixels[x, y] = (r, g, b, int((225 - brightness) / 30 * 255))
    return image.crop(image.getbbox())


def scaled(logo: Image.Image, target_width: int) -> Image.Image:
    factor = target_width / logo.width
    return logo.resize((target_width, max(int(logo.height * factor), 1)), Image.LANCZOS)


def make_icon(logo: Image.Image) -> Image.Image:
    size = 512
    canvas = Image.new("RGBA", (size, size), CREAM + (255,))
    art = scaled(logo, int(size * 0.62))
    canvas.alpha_composite(art, ((size - art.width) // 2, (size - art.height) // 2))
    return canvas.convert("RGB")


def make_feature_graphic(logo: Image.Image) -> Image.Image:
    width, height = 1024, 500
    canvas = Image.new("RGB", (width, height), CREAM)
    draw = ImageDraw.Draw(canvas)

    # İnce altın çerçeve
    draw.rectangle([12, 12, width - 13, height - 13], outline=GOLD, width=2)

    art = scaled(logo, 250)
    canvas.paste(art, (95, (height - art.height) // 2), art)

    title = font(FONT_CANDIDATES, 62)
    body = font(BODY_FONT_CANDIDATES, 30)

    text_x = 400
    draw.text((text_x, 220), "GURME", font=title, fill=GOLD)
    draw.text((text_x, 340), "81 ilde gerçek esnaf lezzetleri", font=body, fill=DARK)
    draw.text((text_x, 380), "İnternetsiz çalışır · Ücretsiz", font=body, fill=(120, 105, 92))

    return canvas


def main() -> int:
    if not LOGO.exists():
        print(f"HATA: {LOGO.relative_to(ROOT)} bulunamadı.", file=sys.stderr)
        return 1

    OUT.mkdir(parents=True, exist_ok=True)
    logo = transparent_logo()

    make_icon(logo).save(OUT / "icon-512.png")
    make_feature_graphic(logo).save(OUT / "feature-graphic.png")

    print(f"✓ {(OUT / 'icon-512.png').relative_to(ROOT)} (512x512)")
    print(f"✓ {(OUT / 'feature-graphic.png').relative_to(ROOT)} (1024x500)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
