"""Gera os arquivos SVG do logo Orbita Play (texto convertido em curvas).
Uso: python3 generate.py   (requer: pip install fonttools)"""
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

HERE = Path(__file__).parent
FONTS = HERE.parents[1] / "app/src/main/res/font"
WHITE, CYAN, NAVY = "#FFFFFF", "#6CD4FF", "#0B1020"

def mark(fg=WHITE, accent=None, bg=NAVY):
    """Símbolo (viewBox 0 0 100 100): planeta em órbita cortado por um rastro."""
    accent = accent or fg
    return f'''<defs><mask id="cut" maskUnits="userSpaceOnUse" x="0" y="0" width="100" height="100">
<rect width="100" height="100" fill="#fff"/>
<path d="M33 67 C46 50 66 30 97 10 C74 34 56 52 33 67Z" fill="#000" stroke="#000" stroke-width="6" stroke-linejoin="round"/>
</mask></defs>
<g mask="url(#cut)"><circle cx="46" cy="56" r="27" fill="none" stroke="{fg}" stroke-width="10"/>
<circle cx="46" cy="56" r="11" fill="{fg}"/></g>
<path d="M36 61 C50 46 68 30 97 10 C76 32 58 48 40 65Z" fill="{accent}"/>'''

def text_path(text, font_file, size, x, y, tracking):
    font = TTFont(FONTS / font_file)
    gs, cmap, hmtx = font.getGlyphSet(), font.getBestCmap(), font["hmtx"]
    scale = size / font["head"].unitsPerEm
    d, cx = [], x
    for ch in text:
        name = cmap[ord(ch)]
        pen = SVGPathPen(gs)
        gs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, cx, y)))
        d.append(pen.getCommands())
        cx += hmtx[name][0] * scale + tracking
    return " ".join(d), cx - tracking

def svg(w, h, body, bg=None):
    rect = f'<rect width="{w}" height="{h}" fill="{bg}"/>' if bg else ""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}">'
            f'<title>Orbita Play</title>{rect}{body}</svg>\n')

def horizontal(fg, accent, bg=None):
    orbita, end = text_path("ORBITA", "outfit_regular.ttf", 64, 128, 82, 9)
    play, end2 = text_path("PLAY", "outfit_bold.ttf", 64, end + 22, 82, 6)
    w = int(end2 + 20)
    body = (f'<g transform="translate(-4 -4) scale(1.15)">{mark(fg, accent)}</g>'
            f'<path d="{orbita}" fill="{fg}"/><path d="{play}" fill="{accent}"/>')
    return svg(w, 124, body, bg)

def stacked(fg, accent, bg=None):
    orbita, e1 = text_path("ORBITA", "outfit_regular.ttf", 44, 0, 0, 6)
    play, e2 = text_path("PLAY", "outfit_bold.ttf", 30, 0, 0, 14)
    w = 320
    body = (f'<g transform="translate(90 14) scale(1.4)">{mark(fg, accent)}</g>'
            f'<g transform="translate({(w - e1) / 2} 206)"><path d="{orbita}" fill="{fg}"/></g>'
            f'<g transform="translate({(w - e2) / 2} 250)"><path d="{play}" fill="{accent}"/></g>')
    return svg(w, 270, body, bg)

def app_icon():
    body = (f'<rect width="512" height="512" rx="112" fill="{NAVY}"/>'
            f'<g transform="translate(76 66) scale(3.6)">{mark(WHITE, CYAN)}</g>')
    return svg(512, 512, body)

files = {
    "orbita-play-logo.svg": horizontal(WHITE, CYAN),
    "orbita-play-logo-dark-bg.svg": horizontal(WHITE, CYAN, NAVY),
    "orbita-play-logo-light.svg": horizontal(NAVY, "#1E9BD7"),
    "orbita-play-logo-stacked.svg": stacked(WHITE, CYAN, NAVY),
    "orbita-play-mark.svg": svg(100, 100, mark(WHITE)),
    "orbita-play-app-icon.svg": app_icon(),
}
for name, content in files.items():
    (HERE / name).write_text(content)
    print("wrote", name)
