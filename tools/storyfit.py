# -*- coding: utf-8 -*-
"""Checks the story narration box can actually hold a story.

The live stories run to a minute or two now, so their passages are no longer one
short line — and the box they are printed in is a fixed slot in a narrow side
column, not a scrolling page. vfit measures layouts but cannot see how much text
goes into them, so this reads the real passages out of StoryCatalog.java and works
out whether the longest one fits the box on each screen size.

Two cases matter in landscape and they differ: on a choice beat the two option
buttons appear and the Next button goes away, which leaves the box much shorter
than usual. Held upright the box is a row of a band instead of a slot in a column,
so it is the width that is bounded and the height that is free — and the same
passage that runs to five lines on its side comes out in three.
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app/src/main/res')

BUCKETS = [
    ('small phone  sw320', ['values'], 320),
    ('common phone sw360', ['values', 'values-sw360dp'], 360),
    ('7in tablet   sw600', ['values', 'values-sw360dp', 'values-sw600dp'], 600),
    ('10in tablet  sw720', ['values', 'values-sw360dp', 'values-sw600dp',
                            'values-sw720dp'], 720),
]

# Persian sets narrower than Latin; the same 0.52em estimate hfit.py uses.
EM = 0.52
LINE = 1.45          # line height as a multiple of the text size
LINE_SP = 15.0       # story_line sets textSize explicitly
NEXT_SP = 18.0       # the Next button
CHUNKY_PAD = 30.0    # Button.Chunky padding, as vfit models it
CHOICE_PAD = 24.0    # Button.Chunky.Choice is slimmer: 9dp over, 15dp under
CHOICE_SIDE = 20.0   # ...and 10dp each side, which is what a label has to fit in


def dims(dirs):
    out = {}
    for d in dirs:
        p = os.path.join(RES, d, 'dimens.xml')
        if not os.path.exists(p):
            continue
        for m in re.finditer(r'<dimen name="([^"]+)">([0-9.]+)(?:dp|sp)</dimen>',
                             io.open(p, encoding='utf-8').read()):
            out[m.group(1)] = float(m.group(2))
    return out


def passages():
    """Every line the box ever shows, longest first."""
    src = io.open(os.path.join(ROOT, 'app/src/main/java/ir/brandimo/pashmak/'
                                     'data/catalog/StoryCatalog.java'),
                  encoding='utf-8').read()
    out = []
    for block in re.split(r'Story \w+ = new Story\(', src)[1:]:
        sid = re.match(r'"([^"]+)"', block).group(1)
        for kind, text in re.findall(
                r'StoryBeat\.(narrate|narrateTo|ask|choice|celebrate)\(\s*\n?\s*'
                r'"((?:[^"\\]|\\.)*)"', block):
            out.append((sid, kind, text))
    return out


def box_height(d, viewport, choosing, dock_small):
    """What story_line is left with once the column's fixed parts have taken theirs."""
    column = viewport - 2 * d['screen_padding_v'] - d['back_button']
    dock = d['mascot_dock_small'] if dock_small else d['mascot_dock']
    used = dock + 8                      # the dock and its bottom margin
    used += 8                            # story_line's top margin
    if choosing:                         # the two options; Next is gone on these beats
        used += 8 + 2 * (d['text_choice'] * LINE + CHOICE_PAD) + 6
    else:
        used += 8 + NEXT_SP * LINE + CHUNKY_PAD
    return column - used


def text_height(d, text):
    inner = d['end_column_width'] - 24            # 12dp padding each side
    per_line = max(1, int(inner // (EM * LINE_SP)))
    lines = -(-len(text) // per_line)             # ceil
    return lines * LINE_SP * LINE + 24, lines     # 12dp padding top and bottom


def options():
    """The two option labels on every choice beat — they sit on one-line buttons."""
    src = io.open(os.path.join(ROOT, 'app/src/main/java/ir/brandimo/pashmak/'
                                     'data/catalog/StoryCatalog.java'),
                  encoding='utf-8').read()
    out = []
    for m in re.finditer(r'StoryBeat\.choice\("[^"]+",\s*\n\s*"([^"]+)", \d+, '
                         r'"([^"]+)", \d+', src):
        out.extend([m.group(1), m.group(2)])
    return out


def portrait_box(d):
    """The width the passage gets in the bottom band, and what it needs.

    The band's first row is Pashmak and the passage; the buttons are on the row
    under it, which is what makes the width workable at all. On a phone the story
    screen gives him the smaller dock (@bool/story_dock_small), so that is the one
    measured here.
    """
    phone = d['end_column_width'] < 264
    dock = d['mascot_dock_small'] if phone else d['mascot_dock']
    return d['__sw'] - 2 * d['screen_padding'] - dock - d['gap']


def main():
    lines = passages()
    failures = 0
    for label, dirs, viewport in BUCKETS:
        d = dims(dirs)
        d['__sw'] = viewport
        dock_small = 'values-sw600dp' not in dirs
        # An option label that needs a third line pushes the box out of the column,
        # so the labels are held to what two lines of a button can show.
        room_per_line = int((d['end_column_width'] - CHOICE_SIDE)
                            // (EM * d['text_choice']))
        worst = max(options(), key=len)
        if len(worst) > 2 * room_per_line:
            failures += 1
            print('%s  option label %r needs %d lines of %d characters'
                  % (label, worst, -(-len(worst) // room_per_line), room_per_line))
        for choosing in (False, True):
            room = box_height(d, viewport, choosing, dock_small)
            kind = 'choice beat ' if choosing else 'ordinary beat'
            if room < d['story_line_min']:
                failures += 1
                print('%s  %s: the box collapses to %.0fdp, under its own '
                      'minimum of %.0fdp' % (label, kind, room, d['story_line_min']))
                continue
            worst = max(((text_height(d, t)[0], s, t)
                         for s, k, t in lines
                         if (k == 'choice') == choosing), key=lambda r: r[0])
            need, count = text_height(d, worst[2])
            fits = need <= room
            # An ordinary passage may run past the box; the child scrolls, or simply
            # listens. A choice question may not: it is what the two buttons under it
            # are answering, and a three-year-old will not think to scroll for it.
            if not fits and choosing:
                failures += 1
            print('%s  %s: box %3.0fdp, longest passage (%s) wants %3.0fdp '
                  'in %d lines  %s'
                  % (label, kind, room, worst[1], need, count,
                     'ok' if fits else ('DOES NOT FIT' if choosing else 'scrolls')))
    print('')
    print('Held upright — the passage is a row of the bottom band, so the width is')
    print('what is bounded. Height is free there: the band grows to fit the words.')
    for label, dirs, viewport in BUCKETS:
        d = dims(dirs)
        d['__sw'] = viewport
        inner = portrait_box(d) - 24                  # 12dp padding each side
        per_line = max(1, int(inner // (EM * LINE_SP)))
        worst = max((t for _, _, t in lines), key=len)
        rows = -(-len(worst) // per_line)
        # Nothing to fail on: the band is wrap_content and the scene above it gives
        # up the room. This is reported so the shape is visible, not guessed at.
        print('%s  passage box %3.0fdp wide, %2d characters a line, longest passage '
              'in %d lines' % (label, portrait_box(d), per_line, rows))

    print('\n%s' % ('FAIL: %d places where the story does not fit its box' % failures
                    if failures else
                    'PASS: every choice question fits whole; longer passages scroll'))
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())
