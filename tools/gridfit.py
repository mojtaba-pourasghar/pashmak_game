# -*- coding: utf-8 -*-
"""Checks the games menu really shows all nine cards without scrolling.

vfit.py can only say the screen fits — the grid is a 0dp RecyclerView, so it is
"flexible" by construction and would pass even while cards fall off the bottom.
This works out the height the grid actually gets and how much the cards want.
"""
import math, os, re, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import vfit

CARDS = 9

def run():
    failures = 0
    for label, dirs, viewport in vfit.BUCKETS:
        d = vfit.load_values(dirs)
        wide = any(os.path.exists(os.path.join(vfit.RES, x, 'bools.xml'))
                   and 'true' in open(os.path.join(vfit.RES, x, 'bools.xml'),
                                      encoding='utf-8').read()
                   for x in dirs)
        span = 3
        for x in reversed(dirs):
            p = os.path.join(vfit.RES, x, 'integers.xml')
            if os.path.exists(p):
                m = re.search(r'games_span">(\d+)', open(p, encoding='utf-8').read())
                if m:
                    span = int(m.group(1))
                    break
        # The headline card eats a whole row when it is wide.
        rows = (1 + math.ceil((CARDS - 1) / span)) if wide else math.ceil(CARDS / span)

        header = d['back_button']
        settings = d['text_chip'] * 1.45 + 16          # Button.Pill, 8dp padding each side
        grid = (viewport - 2 * d['screen_padding_v'] - header - 6 - settings - 8
                - 2 * d['list_box_padding'])           # the grid sits inside a box now

        card = max(d['card_min_height'],
                   d['card_icon_tile'] + d['card_padding_top']
                   + d['card_padding_bottom'])         # icon tile + the card's padding
        needed = rows * (card + 10)                    # 5dp margin each side

        ok = needed <= grid
        failures += 0 if ok else 1
        print('%s  span %d, %s headline -> %d rows: needs %.0fdp of %.0fdp  %s'
              % (label, span, 'wide' if wide else 'flat', rows, needed, grid,
                 'ok' if ok else 'SCROLLS'))
    print('\n%s' % ('FAIL: the games menu scrolls in %d bucket(s)' % failures
                    if failures else 'PASS: all nine game cards fit in every bucket'))
    return 1 if failures else 0

if __name__ == '__main__':
    sys.exit(run())
