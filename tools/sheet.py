# -*- coding: utf-8 -*-
"""Renders the face_*.xml icon set as a contact sheet, and lends its
VectorDrawable -> SVG conversion to the other tools.

Android VectorDrawable path data is SVG path data, so the conversion is mechanical.
"""
import glob, io, os, sys
import xml.etree.ElementTree as ET
import cairosvg
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'app/src/main/res/drawable')
A = '{http://schemas.android.com/apk/res/android}'
CELL = 96


def convert(path, size=CELL):
    root = ET.parse(path).getroot()
    vw = float(root.get(A + 'viewportWidth'))
    vh = float(root.get(A + 'viewportHeight'))
    parts = []

    def emit(node, transform=None):
        for child in node:
            tag = child.tag.split('}')[-1]
            if tag == 'group':
                px = float(child.get(A + 'pivotX', 0))
                py = float(child.get(A + 'pivotY', 0))
                rot = float(child.get(A + 'rotation', 0))
                emit(child, 'rotate(%g %g %g)' % (rot, px, py))
            elif tag == 'path':
                fc = child.get(A + 'fillColor', 'none')
                if fc in (None, '#00000000'):
                    fc = 'none'
                attrs = ['d="%s"' % child.get(A + 'pathData'), 'fill="%s"' % fc]
                fa = child.get(A + 'fillAlpha')
                if fa:
                    attrs.append('fill-opacity="%s"' % fa)
                sc = child.get(A + 'strokeColor')
                if sc:
                    attrs += ['stroke="%s"' % sc,
                              'stroke-width="%s"' % (child.get(A + 'strokeWidth') or 1),
                              'stroke-linecap="%s"' % child.get(A + 'strokeLineCap', 'butt'),
                              'stroke-linejoin="round"']
                if transform:
                    attrs.append('transform="%s"' % transform)
                parts.append('<path %s/>' % ' '.join(attrs))

    emit(root)
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %g %g" '
            'width="%d" height="%d">%s</svg>' % (vw, vh, size, size, ''.join(parts)))


def main():
    files = sorted(glob.glob(os.path.join(SRC, 'face_*.xml')))
    cols = 8
    rows = (len(files) + cols - 1) // cols
    sheet = Image.new('RGB', (cols * CELL, rows * CELL), (246, 243, 238))
    draw = ImageDraw.Draw(sheet)
    for i, f in enumerate(files):
        png = cairosvg.svg2png(bytestring=convert(f).encode('utf-8'),
                               output_width=CELL, output_height=CELL)
        icon = Image.open(io.BytesIO(png)).convert('RGBA')
        x, y = (i % cols) * CELL, (i // cols) * CELL
        draw.rectangle([x + 2, y + 2, x + CELL - 3, y + CELL - 3],
                       fill=(255, 255, 255), outline=(222, 216, 208))
        sheet.paste(icon, (x, y), icon)
    sheet.save(os.path.join(ROOT, 'tools', 'faces.png'))
    print('%d icons rendered' % len(files))


if __name__ == '__main__':
    main()
