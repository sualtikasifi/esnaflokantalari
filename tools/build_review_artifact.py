#!/usr/bin/env python3
"""
tools/fetch_photo_candidates.py'nin topladığı aday fotoğraflardan, tarayıcıda
tek tıkla seçim yapılabilen bir HTML sayfası üretir (Claude Artifact olarak
yayımlanır). Görseller sayfanın içine gömülür (data URI) — dışarıya hiçbir
istek atılmaz.

Kullanım:
    python3 tools/build_review_artifact.py [--out DOSYA] [--max-bytes N]

Varsayılan çıktı: tools/data/review_artifact.html
"""

import base64
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CANDIDATES_DIR = ROOT / "tools" / "data" / "photo_candidates"
MANIFEST_PATH = CANDIDATES_DIR / "manifest.json"
DEFAULT_OUT = ROOT / "tools" / "data" / "review_artifact.html"

# Base64 gömme + HTML iskeleti için pay bırakarak hedef bütçe.
DEFAULT_MAX_BYTES = 10 * 1024 * 1024


def load_manifest() -> dict:
    if not MANIFEST_PATH.exists():
        return {}
    return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))


def data_uri(path: Path) -> str:
    raw = path.read_bytes()
    return "data:image/jpeg;base64," + base64.b64encode(raw).decode("ascii")


def esc(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


CARD_TEMPLATE = """
      <section class="card" data-restaurant="{id}">
        <header class="card-head">
          <h2>{name}</h2>
          <p class="addr">{city} &middot; {address}</p>
        </header>
        <div class="grid">
{tiles}
          <label class="tile tile-none">
            <input type="radio" name="pick-{id}" value="none">
            <span class="none-mark">Hiçbiri<br>uygun değil</span>
          </label>
        </div>
      </section>
"""

TILE_TEMPLATE = """          <label class="tile">
            <input type="radio" name="pick-{id}" value="{index}" data-ref="{ref}">
            <img src="{src}" alt="{name} aday {index}" loading="lazy">
          </label>
"""

PAGE_TEMPLATE = """<!doctype html>
<title>Lokanta Fotoğrafı Seç</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  :root {{
    --bg: #faf6ee;
    --surface: #ffffff;
    --border: #e6dcc9;
    --ink: #2a1e16;
    --ink-soft: #7c6b58;
    --gold: #b08d46;
    --terracotta: #b6392c;
    --selected-bg: #fbf1de;
  }}
  @media (prefers-color-scheme: dark) {{
    :root:not([data-theme="light"]) {{
      --bg: #1c1410;
      --surface: #241a15;
      --border: #3a2c22;
      --ink: #f3ece3;
      --ink-soft: #c9b9a6;
      --gold: #d8b978;
      --terracotta: #e07a63;
      --selected-bg: #33261a;
    }}
  }}
  :root[data-theme="dark"] {{
    --bg: #1c1410;
    --surface: #241a15;
    --border: #3a2c22;
    --ink: #f3ece3;
    --ink-soft: #c9b9a6;
    --gold: #d8b978;
    --terracotta: #e07a63;
    --selected-bg: #33261a;
  }}

  * {{ box-sizing: border-box; }}
  body {{
    margin: 0;
    background: var(--bg);
    color: var(--ink);
    font-family: "IBM Plex Sans", system-ui, sans-serif;
    padding-bottom: 80px;
  }}
  h1, h2 {{ font-family: "Fraunces", Georgia, serif; text-wrap: balance; }}

  .topbar {{
    position: sticky; top: 0; z-index: 5;
    background: var(--bg);
    border-bottom: 1px solid var(--border);
    padding: 18px 24px;
  }}
  .topbar h1 {{ margin: 0 0 4px; font-size: 22px; font-weight: 600; }}
  .topbar p {{ margin: 0; color: var(--ink-soft); font-size: 14px; }}
  #progress {{ font-variant-numeric: tabular-nums; color: var(--gold); font-weight: 600; }}

  main {{ max-width: 760px; margin: 0 auto; padding: 24px; display: flex; flex-direction: column; gap: 20px; }}

  .card {{
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 16px;
    padding: 18px;
  }}
  .card.resolved {{ opacity: 0.35; }}
  .card-head h2 {{ margin: 0 0 2px; font-size: 19px; }}
  .addr {{ margin: 0 0 14px; color: var(--ink-soft); font-size: 13.5px; }}

  .grid {{
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }}
  .tile {{
    position: relative;
    display: block;
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
    border: 2.5px solid transparent;
    aspect-ratio: 4 / 3;
    background: var(--bg);
  }}
  .tile input {{ position: absolute; opacity: 0; pointer-events: none; }}
  .tile img {{ width: 100%; height: 100%; object-fit: cover; display: block; }}
  .tile:has(input:checked) {{ border-color: var(--terracotta); }}
  .tile:hover {{ border-color: var(--gold); }}

  .tile-none {{
    display: flex; align-items: center; justify-content: center;
    text-align: center; font-size: 12.5px; color: var(--ink-soft);
    background: var(--selected-bg);
    line-height: 1.35;
  }}
  .tile-none:has(input:checked) {{ border-color: var(--ink-soft); color: var(--ink); }}

  footer {{ max-width: 760px; margin: 0 auto; padding: 0 24px; color: var(--ink-soft); font-size: 13px; }}
</style>

<div class="topbar">
  <h1>Lokanta Fotoğrafı Seç</h1>
  <p><span id="progress">0 / {total}</span> seçildi — doğru fotoğrafa tıkla, emin değilsen "Hiçbiri uygun değil" işaretle.</p>
</div>

<main>
{cards}
</main>

<footer>Bu sayfa Gurme uygulamasının veri hazırlığı içindir. Seçimler otomatik kaydedilir.</footer>

<script>
  function updateProgress() {{
    var groups = {{}};
    document.querySelectorAll('main input[type=radio]').forEach(function (input) {{
      groups[input.name] = groups[input.name] || false;
      if (input.checked) groups[input.name] = true;
    }});
    var total = Object.keys(groups).length;
    var done = Object.values(groups).filter(Boolean).length;
    document.getElementById('progress').textContent = done + ' / ' + total;
    document.querySelectorAll('.card').forEach(function (card) {{
      var name = 'pick-' + card.dataset.restaurant;
      card.classList.toggle('resolved', !!groups[name]);
    }});
  }}
  document.addEventListener('change', updateProgress);
  document.addEventListener('claude:edit', updateProgress);
  updateProgress();
</script>
"""


def main() -> int:
    max_bytes = DEFAULT_MAX_BYTES
    if "--max-bytes" in sys.argv:
        max_bytes = int(sys.argv[sys.argv.index("--max-bytes") + 1])
    out_path = DEFAULT_OUT
    if "--out" in sys.argv:
        out_path = Path(sys.argv[sys.argv.index("--out") + 1])

    manifest = load_manifest()
    if not manifest:
        print("İncelenecek aday yok. Önce şunu çalıştır:")
        print("  python3 tools/fetch_photo_candidates.py")
        return 1

    cards_html = []
    included = []
    running_size = len(PAGE_TEMPLATE)

    for restaurant_id, info in manifest.items():
        tiles = []
        for candidate in info["candidates"]:
            thumb_path = CANDIDATES_DIR / restaurant_id / f"{candidate['index']}.jpg"
            if not thumb_path.exists():
                continue
            src = data_uri(thumb_path)
            tiles.append(TILE_TEMPLATE.format(
                id=esc(restaurant_id),
                index=candidate["index"],
                ref=esc(candidate["ref"]),
                src=src,
                name=esc(info["name"]),
            ))
        if not tiles:
            continue

        card = CARD_TEMPLATE.format(
            id=esc(restaurant_id),
            name=esc(info["name"]),
            city=esc(info["city"]),
            address=esc(info["address"]),
            tiles="".join(tiles),
        )

        if running_size + len(card) > max_bytes and included:
            break

        cards_html.append(card)
        included.append(restaurant_id)
        running_size += len(card)

    html = PAGE_TEMPLATE.format(
        cards="".join(cards_html),
        total=len(included),
    )

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(html, encoding="utf-8")

    try:
        label = out_path.relative_to(ROOT)
    except ValueError:
        label = out_path
    print(f"✓ {label} yazıldı")
    print(f"  {len(included)} lokanta, {len(html) / 1024 / 1024:.2f} MB")
    print(f"  toplam bekleyen: {len(manifest)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
