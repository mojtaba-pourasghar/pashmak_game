# -*- coding: utf-8 -*-
"""Estimates the minimum height each screen needs, per dimension bucket.

The app is landscape-locked, so a device's smallestWidth IS its height and height
is the scarce dimension. This walks each layout and works out how much vertical
space it cannot do without, so a screen that runs off the bottom shows up here
instead of on a child's phone.

Understands the three things that decide whether a screen fits:
  * ScrollView      - absorbs any overflow, so it needs only its own minimum
  * layout_weight   - a weighted child yields its space, so it needs only minHeight
  * ConstraintLayout - children chained top-to-bottom accumulate; the tallest
                       chain is what the screen needs
  * GridLayout      - cells wrap into rows, so it costs rows x cell, not one cell
"""
import glob, os, re, sys
import xml.etree.ElementTree as ET

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'app/src/main/res')
A = '{http://schemas.android.com/apk/res/android}'
APP = '{http://schemas.android.com/apk/res-auto}'

def load_values(dirs, kind='dimen'):
    out = {}
    for d in dirs:
        for fname in ('dimens.xml', 'integers.xml', 'bools.xml'):
            p = os.path.join(RES, d, fname)
            if not os.path.exists(p):
                continue
            text = open(p, encoding='utf-8').read()
            for m in re.finditer(r'<dimen name="([^"]+)">([0-9.]+)(dp|sp)</dimen>', text):
                out[m.group(1)] = float(m.group(2))
    return out

STYLE_SIZE = {
    'Text.Display': 'text_display', 'Text.Title': 'text_title',
    'Text.Section': 'text_section', 'Text.Body': 'text_body',
    'Text.Body.Bold': 'text_body', 'Text.Sub': 'text_sub',
    'Text.Chip': 'text_chip', 'Button.Chunky': 'text_button',
    'Button.Chunky.Large': 'text_button_lg',
}
STYLE_PAD = {'Button.Chunky': 30, 'Button.Chunky.Large': 35, 'Button.Pill': 16}
STYLE_FIXED = {'Button.Round': 'back_button'}

def dp(value, dims, default=0.0):
    if not value:
        return default
    value = value.strip()
    m = re.match(r'^(-?[0-9.]+)(dp|sp|px)?$', value)
    if m:
        return float(m.group(1))
    m = re.match(r'^@dimen/(.+)$', value)
    if m:
        return dims.get(m.group(1), default)
    return default

CACHE = {}

def layout_path(name):
    for d in ('layout', 'layout-sw600dp'):
        p = os.path.join(RES, d, name + '.xml')
        if os.path.exists(p):
            return p
    return None

def layout_path_for(name, bucket_dirs):
    """Honours layout-sw600dp/ for the tablet buckets, layout/ otherwise."""
    if any('sw600' in d or 'sw720' in d for d in bucket_dirs):
        p = os.path.join(RES, 'layout-sw600dp', name + '.xml')
        if os.path.exists(p):
            return p
    p = os.path.join(RES, 'layout', name + '.xml')
    return p if os.path.exists(p) else None

def height_of(node, dims, dirs, depth=0):
    """Vertical space this view cannot give up, margins included."""
    if depth > 16:
        return 0.0
    tag = node.tag.split('}')[-1]
    style = node.get('style', '').replace('@style/', '')
    h = node.get(A + 'layout_height', 'wrap_content')
    margin = (dp(node.get(A + 'layout_marginTop'), dims)
              + dp(node.get(A + 'layout_marginBottom'), dims))
    pad_all = dp(node.get(A + 'padding'), dims)
    pad = (pad_all * 2 if pad_all else
           dp(node.get(A + 'paddingTop'), dims) + dp(node.get(A + 'paddingBottom'), dims))
    min_h = dp(node.get(A + 'minHeight'), dims, 0)

    if node.get(A + 'visibility') == 'gone':
        return 0.0

    # A weighted child yields its space to its siblings.
    if dp(node.get(A + 'layout_weight'), dims, 0) > 0:
        return min_h + margin

    if tag == 'include':
        m = re.match(r'^@layout/(.+)$', node.get('layout') or '')
        return (required(m.group(1), dims, dirs, depth + 1) if m else 0.0) + margin

    fixed = dims.get(STYLE_FIXED[style]) if style in STYLE_FIXED else None
    if fixed is None and h not in ('wrap_content', 'match_parent', '0dp'):
        fixed = dp(h, dims, None)
    if fixed is not None:
        return max(fixed, min_h) + margin

    kids = [c for c in node if isinstance(c.tag, str)]

    # A scroll view absorbs whatever its content needs.
    if 'ScrollView' in tag:
        return min_h + margin

    if tag.endswith('GridLayout'):
        cols = int(node.get(A + 'columnCount') or 1)
        cells = [c for c in kids if c.get(A + 'visibility') != 'gone']
        rows = -(-len(cells) // max(cols, 1))
        tallest = max((height_of(c, dims, dirs, depth + 1) for c in cells), default=0.0)
        own = rows * tallest + pad
    elif tag.endswith('ConstraintLayout'):
        own = constraint_height(node, kids, dims, dirs, depth) + pad
    elif tag.endswith('LinearLayout') and node.get(A + 'orientation') == 'vertical':
        own = sum(height_of(c, dims, dirs, depth + 1) for c in kids) + pad
    elif kids:
        own = max(height_of(c, dims, dirs, depth + 1) for c in kids) + pad
    else:
        key = STYLE_SIZE.get(style)
        size = dims.get(key, 0) if key else dp(node.get(A + 'textSize'), dims, 0)
        text = (size * 1.45 + STYLE_PAD.get(style, 0)) if size else 0
        own = text + pad
    return max(own, min_h) + margin

def constraint_height(root, kids, dims, dirs, depth):
    """The tallest top-to-bottom chain, which is what the parent must be able to show."""
    by_id, heights = {}, {}
    for c in kids:
        cid = (c.get(A + 'id') or '').replace('@+id/', '').replace('@id/', '')
        if cid:
            by_id[cid] = c
        heights[id(c)] = height_of(c, dims, dirs, depth + 1)

    def ref(c, attr):
        v = c.get(APP + attr) or ''
        return v.replace('@+id/', '').replace('@id/', '') if v else None

    seen = {}
    def bottom_of(c, stack=()):
        key = id(c)
        if key in seen:
            return seen[key]
        if key in stack:
            return 0.0
        above = ref(c, 'layout_constraintTop_toBottomOf')
        start = bottom_of(by_id[above], stack + (key,)) if above in by_id else 0.0
        value = start + heights[key]
        seen[key] = value
        return value

    tallest = 0.0
    for c in kids:
        if c.get(A + 'visibility') == 'gone':
            continue
        h = c.get(A + 'layout_height', 'wrap_content')
        pinned_both = (ref(c, 'layout_constraintTop_toTopOf')
                       and ref(c, 'layout_constraintBottom_toBottomOf'))
        # 0dp pinned top and bottom just fills whatever is left.
        if h == '0dp' and pinned_both:
            continue
        tallest = max(tallest, bottom_of(c))
    return tallest

def required(name, dims, dirs, depth=0):
    key = (name, tuple(dirs))
    if key in CACHE:
        return CACHE[key]
    path = layout_path_for(name, dirs)
    if not path:
        return 0.0
    CACHE[key] = 0.0
    CACHE[key] = height_of(ET.parse(path).getroot(), dims, dirs, depth)
    return CACHE[key]

# bucket -> (value dirs, the shortest screen that bucket can be selected for)
BUCKETS = [
    ('small phone  sw320', ['values'], 320),
    ('common phone sw360', ['values', 'values-sw360dp'], 360),
    ('7in tablet   sw600', ['values', 'values-sw360dp', 'values-sw600dp'], 600),
    ('10in tablet  sw720', ['values', 'values-sw360dp', 'values-sw600dp',
                            'values-sw720dp'], 720),
]

def generated_content(dims, viewport):
    """Content built in code that still has to fit a fixed container.

    vfit reads layouts, so it cannot see views an Activity creates at runtime — which
    is how four mission item cards ended up in a column with room for two. Anything
    generated into a fixed-height parent belongs here.
    """
    problems = []
    # LiveDrawingActivity#renderSlots fills brief_slots with one card per item.
    items = 4                                   # every mission has four
    card = dims['slot_thumb'] + 16 + 8          # thumbnail + 8dp padding each side + margin
    body = dims['text_body'] * 1.45 * 2 + 18    # brief_line, two lines
    column = (viewport - 2 * dims['screen_padding_v'] - dims['back_button']
              - 6 - body - 8)
    room = (column - (dims['text_section'] * 1.45 + 8) - 8
            - (dims['text_button'] * 1.45 + 30))
    # The column scrolls, so not fitting is fine — being too small to use is not.
    visible = int(room // card)
    if visible < 2:
        problems.append('brief_slots: only %d of %d item cards visible in %.0fdp'
                        % (visible, items, room))
    return problems


def main():
    failures = 0
    names = sorted(os.path.basename(f)[:-4]
                   for f in glob.glob(os.path.join(RES, 'layout', 'activity_*.xml')))
    names.append('dialog_parent_gate')
    for label, dirs, viewport in BUCKETS:
        CACHE.clear()
        dims = load_values(dirs)
        rows = [(n, required(n, dims, dirs)) for n in names]
        over = [(n, h) for n, h in rows if h > viewport]
        worst = max(rows, key=lambda r: r[1])
        print('%s  (%ddp tall):' % (label, viewport), end=' ')
        if over:
            failures += len(over)
            print('%d OVER' % len(over))
            for n, h in sorted(over, key=lambda r: -r[1]):
                print('    %-24s needs ~%4.0fdp  (over by %3.0fdp)' % (n, h, h - viewport))
        else:
            print('all fit  (tightest: %s at ~%.0fdp, %.0fdp spare)'
                  % (worst[0], worst[1], viewport - worst[1]))
        for problem in generated_content(dims, viewport):
            failures += 1
            print('    %s  OVER' % problem)
    print('\n%s' % ('FAIL: %d screen/bucket combinations overflow' % failures
                    if failures else 'PASS: every screen fits every bucket'))
    return 1 if failures else 0

if __name__ == '__main__':
    sys.exit(main())
