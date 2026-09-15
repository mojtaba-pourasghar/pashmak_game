# -*- coding: utf-8 -*-
"""Renders every story scene to a contact sheet, so the drawn places get looked at.

Mirrors ScenePainter's geometry (fractions of the scene, sky/ground bands, the five
shape kinds) and reuses the VectorDrawable -> SVG pass for the decor icons.
"""
import io, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cairosvg
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app/src/main/res')
SRC = os.path.join(ROOT, 'app/src/main/java/ir/brandimo/pashmak/data/catalog')
W, H = 300, 190

sys.path.insert(0, os.path.join(ROOT, 'tools'))
from sheet import convert as vector_to_svg           # noqa: E402

def rgb(h):
    h = h.strip('"#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))

def parse(path):
    """Pulls the scene(...) calls out of the catalog as plain data."""
    text = open(path, encoding='utf-8').read()
    body = text[text.index('return new MissionScene[]{'):]
    scenes = []
    for m in re.finditer(r'//\s*(\d+)\s+(.+?)\n\s*scene\("(#\w+)",\s*"(#\w+)",\s*([\d.]+)f,',
                         body):
        start = m.end()
        depth, i = 1, start
        while depth:
            if body[i] == '(':
                depth += 1
            elif body[i] == ')':
                depth -= 1
            i += 1
        chunk = body[start:i]
        shapes = []
        for s in re.finditer(r'\b(rect|box|oval|triUp|triDown)\("(#\w+)",\s*'
                             r'([-\d.]+)f,\s*([-\d.]+)f,\s*([-\d.]+)f,\s*([-\d.]+)f'
                             r'(?:,\s*([-\d.]+)f)?\)', chunk):
            shapes.append((s.group(1), s.group(2)) + tuple(
                float(x) for x in s.groups()[2:6]))
        decor = []
        for d in re.finditer(r'at\(R\.drawable\.(\w+),\s*([-\d.]+)f,\s*'
                             r'([-\d.]+)f,\s*([-\d.]+)f\)', chunk):
            decor.append((d.group(1), float(d.group(2)), float(d.group(3)),
                          float(d.group(4))))
        scenes.append(dict(n=m.group(1), title=m.group(2).strip(), sky=m.group(3),
                           ground=m.group(4), horizon=float(m.group(5)),
                           shapes=shapes, decor=decor))
    return scenes

ICONS = {}
def icon(name, size):
    key = (name, size)
    if key not in ICONS:
        png = cairosvg.svg2png(
            bytestring=vector_to_svg(os.path.join(RES, 'drawable', name + '.xml')).encode(),
            output_width=size, output_height=size)
        ICONS[key] = Image.open(io.BytesIO(png)).convert('RGBA')
    return ICONS[key]

def render(s):
    img = Image.new('RGBA', (W, H), rgb(s['sky']))
    d = ImageDraw.Draw(img)
    horizon = s['horizon'] * H
    d.rectangle([0, horizon, W, H], fill=rgb(s['ground']))
    for kind, color, x, y, w, h in s['shapes']:
        box = [x * W, y * H, (x + w) * W, (y + h) * H]
        c = rgb(color)
        if kind == 'oval':
            d.ellipse(box, fill=c)
        elif kind == 'box':
            d.rounded_rectangle(box, radius=min(w * W, h * H) * 0.3, fill=c)
        elif kind == 'triUp':
            d.polygon([((box[0] + box[2]) / 2, box[1]), (box[2], box[3]),
                       (box[0], box[3])], fill=c)
        elif kind == 'triDown':
            d.polygon([(box[0], box[1]), (box[2], box[1]),
                       ((box[0] + box[2]) / 2, box[3])], fill=c)
        else:
            d.rectangle(box, fill=c)
    shortest = min(W, H)
    for name, x, y, size in s['decor']:
        px = max(8, int(size * shortest))
        ic = icon(name, px)
        img.paste(ic, (int(x * W - px / 2), int(y * H - px / 2)), ic)
    return img

scenes = parse(os.path.join(SRC, 'StorySceneCatalog.java'))
cols = 4
rows = (len(scenes) + cols - 1) // cols
sheet = Image.new('RGB', (cols * (W + 8) + 8, rows * (H + 8) + 8), (40, 46, 54))
for i, s in enumerate(scenes):
    x, y = (i % cols) * (W + 8) + 8, (i // cols) * (H + 8) + 8
    sheet.paste(render(s).convert('RGB'), (x, y))
sheet.save(os.path.join(ROOT, 'tools', 'story_scenes.png'))
print('%d scenes rendered' % len(scenes))
for i, s in enumerate(scenes):
    print('  %2s %-28s %d shapes, %d decor' % (s['n'], s['title'],
                                               len(s['shapes']), len(s['decor'])))
