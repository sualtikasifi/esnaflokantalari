#!/usr/bin/env python3
"""
Açılış ekranının alt kısmında görünen marka yazısını üretir.

Kullanım:
    python3 tools/make_branding.py
"""

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app" / "src" / "main" / "res"

TEXT = "GURME"
GOLD = (176, 141, 70, 255)

FONT_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]


def load_font(size: int) -> ImageFont.FreeTypeFont:
    for path in FONT_CANDIDATES:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    raise SystemExit("Uygun yazı tipi bulunamadı.")


def main() -> int:
    font_size = 58
    font = load_font(font_size)
    # Harfler arası boşlukla daha zarif dursun.
    tracking = 8

    widths = [font.getbbox(ch)[2] - font.getbbox(ch)[0] for ch in TEXT]
    total_width = sum(widths) + tracking * (len(TEXT) - 1)
    height = font_size * 2

    image = Image.new("RGBA", (total_width + 40, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    x = 20
    for index, char in enumerate(TEXT):
        draw.text((x, height // 2), char, font=font, fill=GOLD, anchor="lm")
        x += widths[index] + tracking

    target = RES / "drawable" / "ic_branding.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target)
    print(f"✓ {target.relative_to(ROOT)} ({image.width}x{image.height})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
