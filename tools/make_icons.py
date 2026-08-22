#!/usr/bin/env python3
"""
Uygulama simgelerini üretir.

Android 8 öncesi cihazlar uyarlanabilir (adaptive) simgeyi okuyamaz, bu yüzden
klasik PNG simgeler de gerekir. Bu betik ikisini de üretir.

Kullanım:
    python3 tools/make_icons.py
"""

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"

BACKGROUND = (182, 57, 44)      # Kiremit kırmızısı
PLATE = (251, 243, 234)         # Krem
BROTH = (224, 162, 42)          # Çorba/altın

# mipmap klasörü -> kenar uzunluğu (px)
DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def draw_icon(size: int, rounded: bool) -> Image.Image:
    scale = 4
    canvas = size * scale
    image = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    if rounded:
        draw.ellipse([0, 0, canvas - 1, canvas - 1], fill=BACKGROUND)
    else:
        radius = int(canvas * 0.18)
        draw.rounded_rectangle([0, 0, canvas - 1, canvas - 1], radius=radius, fill=BACKGROUND)

    # Tabak
    pad = canvas * 0.22
    draw.ellipse([pad, pad, canvas - pad, canvas - pad], fill=PLATE)

    # İçindeki çorba
    inner = canvas * 0.34
    draw.ellipse([inner, inner, canvas - inner, canvas - inner], fill=BROTH)

    return image.resize((size, size), Image.LANCZOS)


def main() -> int:
    for folder, size in DENSITIES.items():
        target = RES / folder
        target.mkdir(parents=True, exist_ok=True)
        draw_icon(size, rounded=False).save(target / "ic_launcher.png")
        draw_icon(size, rounded=True).save(target / "ic_launcher_round.png")
        print(f"  {folder}: {size}x{size}")

    print("✓ Simgeler üretildi")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
