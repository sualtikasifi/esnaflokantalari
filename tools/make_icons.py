#!/usr/bin/env python3
"""
Uygulama simgelerini ve uygulama içi logoyu üretir.

Kaynak: tools/data/logo.png (kare, tercihen 512x512+)

Üretilenler:
  - mipmap-*/ic_launcher.png ve ic_launcher_round.png   (Android 8 öncesi)
  - drawable/ic_logo_foreground.png                      (uyarlanabilir simge katmanı)
  - drawable/ic_logo.png                                 (uygulama içinde kullanılan logo)

Kullanım:
    python3 tools/make_icons.py
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"
SOURCE = ROOT / "tools" / "data" / "logo.png"

# Logonun krem/beyaz zemini — uyarlanabilir simgenin arka plan katmanı
BACKGROUND = (250, 247, 240)

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def load_logo() -> Image.Image:
    """Logoyu yükler ve açık zeminini saydam yapar."""
    image = Image.open(SOURCE).convert("RGBA")
    width, height = image.size
    pixels = image.load()

    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            # Açık gri/krem tonları saydamlaştır, çizimi koru.
            brightness = (r + g + b) / 3
            if brightness > 225:
                pixels[x, y] = (r, g, b, 0)
            elif brightness > 195:
                # Kenar yumuşatma: yarı saydam geçiş
                alpha = int((225 - brightness) / 30 * 255)
                pixels[x, y] = (r, g, b, alpha)

    return image.crop(image.getbbox() or (0, 0, width, height))


def fit(logo: Image.Image, canvas: int, ratio: float) -> Image.Image:
    """Logoyu kare tuvalin ortasına, verilen oranda yerleştirir."""
    target = int(canvas * ratio)

    # thumbnail() küçültür ama büyütmez; logo küçük kaldığında simge de küçük
    # kalıyordu. Bu yüzden en uzun kenarı hedefe oturtup elle ölçekliyoruz.
    factor = target / max(logo.width, logo.height)
    scaled = logo.resize(
        (max(int(logo.width * factor), 1), max(int(logo.height * factor), 1)),
        Image.LANCZOS,
    )

    layer = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    layer.paste(
        scaled,
        ((canvas - scaled.width) // 2, (canvas - scaled.height) // 2),
        scaled,
    )
    return layer


def launcher_icon(logo: Image.Image, size: int, rounded: bool) -> Image.Image:
    scale = 4
    canvas = size * scale

    base = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    shape = Image.new("L", (canvas, canvas), 0)
    draw = ImageDraw.Draw(shape)
    if rounded:
        draw.ellipse([0, 0, canvas - 1, canvas - 1], fill=255)
    else:
        draw.rounded_rectangle([0, 0, canvas - 1, canvas - 1], radius=int(canvas * 0.22), fill=255)

    background = Image.new("RGBA", (canvas, canvas), BACKGROUND + (255,))
    base.paste(background, (0, 0), shape)
    base.alpha_composite(fit(logo, canvas, 0.66))

    return base.resize((size, size), Image.LANCZOS)


def main() -> int:
    if not SOURCE.exists():
        print(f"HATA: {SOURCE.relative_to(ROOT)} bulunamadı.", file=sys.stderr)
        print("Logo dosyasını oraya koyup tekrar çalıştır.", file=sys.stderr)
        return 1

    logo = load_logo()
    print(f"Logo yüklendi: {logo.size[0]}x{logo.size[1]}")

    for folder, size in DENSITIES.items():
        target = RES / folder
        target.mkdir(parents=True, exist_ok=True)
        launcher_icon(logo, size, rounded=False).save(target / "ic_launcher.png")
        launcher_icon(logo, size, rounded=True).save(target / "ic_launcher_round.png")
        print(f"  {folder}: {size}x{size}")

    drawable = RES / "drawable"
    drawable.mkdir(parents=True, exist_ok=True)

    # Uyarlanabilir simge katmanı.
    # Android 108dp tuvalin dış kenarlarını kırpar; güvenli alan içteki 72dp'dir.
    # Launcher'lar ayrıca kendi maskesini uyguladığı için %48'de tutuyoruz —
    # aksi halde simge ekranda taşmış gibi görünüyor.
    fit(logo, 432, 0.48).save(drawable / "ic_logo_foreground.png")

    # Açılış (splash) ekranı ikonu: 288dp tuval, içteki 192dp görünür alan.
    fit(logo, 768, 0.50).save(drawable / "ic_splash_logo.png")

    # Uygulama içinde (başlık, boş durumlar) kullanılacak sade logo
    fit(logo, 384, 0.96).save(drawable / "ic_logo.png")

    print("✓ Simgeler ve logo üretildi")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
