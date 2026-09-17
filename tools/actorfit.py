# -*- coding: utf-8 -*-
"""Mirrors TaleSceneView.drawActor so the subject's placement can be looked at.

The maths lives in Java where nothing can render it, so it is repeated here and the
result drawn: a frame per motion, the subject at several moments in its cycle. What
this is checking is simple and easy to get wrong by arithmetic alone — that the
thing is on the ground when it should be, in the sky when it should be, the right
size, and never off the edge of the picture.
"""
import io, math, sys
sys.path.insert(0, 'tools')
import sheet, cairosvg
from PIL import Image, ImageDraw

W, H = 300, 200
HORIZON = 0.62
STEPS = [0.0, 4.0, 9.0, 14.0, 19.0]


def place(motion, seconds, w, h):
    size = min(w, h) * 0.42
    ground = h * HORIZON
    standing = h - size * 0.60
    turn = 0.0
    if motion == 'WALK':
        cycle = (seconds % 40.0) / 20.0
        along = cycle if cycle <= 1 else 2 - cycle
        x = w * 0.18 + (w * 0.64) * along
        y = standing + math.sin(seconds * 3.1) * size * 0.03
    elif motion == 'DRIFT':
        cycle = (seconds % 48.0) / 24.0
        along = cycle if cycle <= 1 else 2 - cycle
        x = w * 0.80 - (w * 0.60) * along
        y = max(size * 0.55, ground * 0.42 + math.sin(seconds * 0.8) * size * 0.12)
    elif motion == 'RISE':
        x = w * 0.5 + w * 0.05 * math.sin(seconds * 0.5)
        lift = (1 - math.cos(seconds * 0.45)) / 2
        y = standing - (standing - ground * 0.5) * lift
    elif motion == 'SPIN':
        x, y = w * 0.5, standing
        turn = (seconds * 42.0) % 360.0
    else:
        x, y = w * 0.5, standing
        if motion == 'BOB':
            y -= abs(math.sin(seconds * 1.9)) * size * 0.16
    return x, y, size, turn


MIN_SHARE = 0.34   # of the shorter edge: below this it stops being the subject


CASES = [('WALK', 'snail'), ('DRIFT', 'kite'), ('BOB', 'candle'),
         ('SPIN', 'clock'), ('RISE', 'drop'), ('STILL', 'door')]

sheetimg = Image.new('RGB', (W * len(STEPS), (H + 20) * len(CASES)), '#FFF6E8')
draw = ImageDraw.Draw(sheetimg)
problems = []
for row, (motion, art) in enumerate(CASES):
    svg = sheet.convert('app/src/main/res/drawable/face_%s.xml' % art, 256)
    for col, t in enumerate(STEPS):
        x0, y0 = col * W, row * (H + 20)
        draw.rectangle([x0, y0, x0 + W - 2, y0 + H], fill='#BFE7F7')
        draw.rectangle([x0, y0 + H * HORIZON, x0 + W - 2, y0 + H], fill='#A8DC85')
        x, y, size, turn = place(motion, t, W, H)
        png = cairosvg.svg2png(bytestring=svg.encode('utf-8'),
                               output_width=int(size), output_height=int(size))
        tile = Image.open(io.BytesIO(png)).convert('RGBA')
        if turn:
            tile = tile.rotate(-turn, expand=False, resample=Image.BICUBIC)
        sheetimg.paste(tile, (int(x0 + x - size / 2), int(y0 + y - size / 2)), tile)
        if x - size / 2 < 0 or x + size / 2 > W:
            problems.append('%s at %gs runs off the side' % (motion, t))
        if y + size / 2 > H or y - size / 2 < 0:
            problems.append('%s at %gs leaves the frame' % (motion, t))
        if size < min(W, H) * MIN_SHARE:
            problems.append('%s is too small to be the subject' % motion)
    draw.text((row and 4 or 4, y0 + H + 4), '%s (%s)' % (motion, art), fill='#2B3742')
sheetimg.save('/tmp/actors.png')
print('\n'.join(problems) if problems else 'placement stays inside the frame')
