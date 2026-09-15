# -*- coding: utf-8 -*-
"""Checks every grid still has readable columns after the rail takes its width.

Height is the scarce dimension in this landscape-locked app, so the companion was
moved into a side rail — which spends width instead. Width is plentiful, but not
unlimited: a stage row still has to fit its chip, its lock and its progress mark
beside the title, and a game card its icon tile beside two lines of Persian. This
works out what each column actually gets and fails if one drops below that.
"""
import os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vfit

# Landscape width that goes with each bucket's shortest screen.
WIDTHS = {320: 480, 360: 640, 600: 960, 720: 1280}

# screen, span resource, does it sit beside the rail, floor per column, why
GRIDS = [
    ('stage picker', 'missions_span', True, 150,
     'chip 46 + lock 20 + progress + a readable title'),
    ('missions',     'missions_span', True, 150, 'same row shape as the picker'),
    ('gallery',      'gallery_span',  True,  96, 'a square thumbnail and its caption'),
    # The floor is what the card itself needs: its own side padding, the icon tile,
    # the gap, and room for a short title. The longest title ("دنیای بازی پشمک")
    # ellipsizes on a 480dp phone by design — three columns of a wide horizontal card
    # simply do not fit otherwise, and all eight cards fitting on screen matters more.
    ('games menu',   'games_span',    False, 132,
     'card padding + icon tile + a short title'),
]

def integer(name, dirs):
    for d in reversed(dirs):
        p = os.path.join(vfit.RES, d, 'integers.xml')
        if os.path.exists(p):
            m = re.search(r'%s">(\d+)' % name, open(p, encoding='utf-8').read())
            if m:
                return int(m.group(1))
    return 1

def run():
    failures = 0
    for label, dirs, viewport in vfit.BUCKETS:
        d = vfit.load_values(dirs)
        screen = WIDTHS[viewport]
        print('%s  (%ddp wide):' % (label, screen))
        for name, span_res, beside_rail, floor, why in GRIDS:
            span = integer(span_res, dirs)
            width = screen - 2 * d['screen_padding'] - 2 * d['list_box_padding']
            if beside_rail:
                width -= d['rail_width'] + d['gap']
            # Each cell carries a 5dp margin on both sides.
            column = width / span - 10
            ok = column >= floor
            failures += 0 if ok else 1
            print('    %-14s span %d -> %5.0fdp/column  (needs %ddp: %s)  %s'
                  % (name, span, column, floor, why, 'ok' if ok else 'TOO NARROW'))
    print('\n%s' % ('FAIL: %d grid/bucket combinations are too narrow' % failures
                    if failures else 'PASS: every grid column stays readable'))
    return 1 if failures else 0

if __name__ == '__main__':
    sys.exit(run())
