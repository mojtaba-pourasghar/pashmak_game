# -*- coding: utf-8 -*-
"""Checks the portrait layouts against the landscape ones they stand in for.

Two different things can go wrong when a screen gains a second layout, and only
one of them is visible by reading the file.

1. ID PARITY. ViewBinding merges the variants of a layout into one binding class,
   and any id that is missing from one of them becomes a nullable field. Nothing
   fails to compile; the screen just throws the first time that view is touched in
   the orientation that lacks it. So a portrait layout must carry exactly the ids
   its landscape counterpart does — no more, no fewer.

2. THE WIDTH BUDGET. smallestWidth does not change when the screen is turned, so
   the buckets stay the same, but which edge is scarce flips: in portrait the width
   IS the smallestWidth, and the bottom band has to fit Pashmak, whatever text the
   screen shows and its buttons across that width.
"""
import glob, os, re, sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app/src/main/res')
A = '{http://schemas.android.com/apk/res/android}'

BUCKETS = [
    ('small phone  sw320', ['values'], 320),
    ('common phone sw360', ['values', 'values-sw360dp'], 360),
    ('7in tablet   sw600', ['values', 'values-sw360dp', 'values-sw600dp'], 600),
    ('10in tablet  sw720', ['values', 'values-sw360dp', 'values-sw600dp',
                            'values-sw720dp'], 720),
]


def dims(dirs):
    out = {}
    for d in dirs:
        p = os.path.join(RES, d, 'dimens.xml')
        if not os.path.exists(p):
            continue
        for m in re.finditer(r'<dimen name="([^"]+)">([0-9.]+)(?:dp|sp)</dimen>',
                             open(p, encoding='utf-8').read()):
            out[m.group(1)] = float(m.group(2))
    return out


def ids(path):
    found = set()
    for node in ET.parse(path).iter():
        value = node.get(A + 'id')
        if value:
            found.add(value.split('/')[-1])
    return found


def parity():
    """Every variant of a layout must name the same ids.

    ViewBinding generates one class per layout NAME, merging every variant of it.
    An id that is missing from any one of them becomes a nullable field on the
    single generated class — so the hazard is the union across all variants, not a
    comparison against one particular file.
    """
    problems = []
    variants = {}
    for folder in ('layout', 'layout-sw600dp', 'layout-port', 'layout-sw600dp-port'):
        for path in sorted(glob.glob(os.path.join(RES, folder, '*.xml'))):
            variants.setdefault(os.path.basename(path), []).append((folder, path))
    for name, files in sorted(variants.items()):
        if len(files) < 2:
            continue
        everywhere = [(folder, ids(path)) for folder, path in files]
        union = set()
        for _, found in everywhere:
            union |= found
        for folder, found in everywhere:
            for missing in sorted(union - found):
                problems.append('%s/%s does not have %s, which another variant does '
                                '— ViewBinding will make that field nullable'
                                % (folder, name, missing))
    return problems


def coverage():
    """A landscape layout with a side column needs a portrait twin."""
    problems = []
    for land in sorted(glob.glob(os.path.join(RES, 'layout', 'activity_*.xml'))):
        name = os.path.basename(land)
        text = open(land, encoding='utf-8').read()
        sided = ('@dimen/end_column_width' in text or '@dimen/rail_width' in text
                 or '@dimen/lullaby_column_width' in text)
        if sided and not os.path.exists(os.path.join(RES, 'layout-port', name)):
            problems.append('%s lays out a side column but has no portrait variant'
                            % name)
    # A tablet in portrait resolves layout-sw600dp before layout-port, because
    # smallestWidth outranks orientation — so a sw600 variant needs a -port of its own.
    for land in sorted(glob.glob(os.path.join(RES, 'layout-sw600dp', '*.xml'))):
        name = os.path.basename(land)
        if not os.path.exists(os.path.join(RES, 'layout-sw600dp-port', name)):
            problems.append('layout-sw600dp/%s wins over layout-port on a tablet in '
                            'portrait; it needs a layout-sw600dp-port twin' % name)
    return problems


def references():
    """Every resource the new layouts name has to exist.

    /tmp-style audits only walk res/layout, so the portrait folders are unseen by
    them; a mistyped drawable in here fails at build time with nothing pointing at
    which file. This is how bg_screen_draw — which never existed — was caught.
    """
    have = {'drawable': set(), 'string': set(), 'dimen': set(), 'style': set(),
            'color': set(), 'integer': set(), 'bool': set(), 'array': set(),
            'layout': set(), 'font': set(), 'attr': set(), 'id': set()}
    for path in glob.glob(os.path.join(RES, 'drawable*', '*')):
        have['drawable'].add(os.path.basename(path).rsplit('.', 1)[0])
    for path in glob.glob(os.path.join(RES, 'font', '*')):
        have['font'].add(os.path.basename(path).rsplit('.', 1)[0])
    for folder in ('layout', 'layout-sw600dp', 'layout-port', 'layout-sw600dp-port'):
        for path in glob.glob(os.path.join(RES, folder, '*.xml')):
            have['layout'].add(os.path.basename(path)[:-4])
    for path in glob.glob(os.path.join(RES, 'values*', '*.xml')):
        text = open(path, encoding='utf-8').read()
        for kind, name in re.findall(
                r'<(dimen|string|color|style|integer|bool|string-array|integer-array|'
                r'array) name="([^"]+)"', text):
            key = 'array' if kind.endswith('array') else kind
            have[key].add(name)
    problems = []
    for folder in ('layout-port', 'layout-sw600dp-port'):
        for path in sorted(glob.glob(os.path.join(RES, folder, '*.xml'))):
            text = open(path, encoding='utf-8').read()
            declared = {m for m in re.findall(r'@\+id/([A-Za-z0-9_]+)', text)}
            for kind, name in re.findall(r'@(?:android:)?(\w[\w-]*)/([A-Za-z0-9_.]+)',
                                         text):
                if kind in ('android', 'style') and kind == 'android':
                    continue
                if kind == 'id':
                    if name not in declared:
                        problems.append('%s/%s refers to @id/%s, which it never '
                                        'declares' % (folder,
                                                      os.path.basename(path), name))
                    continue
                key = kind.replace('-', '')
                if key in have and name not in have[key]:
                    problems.append('%s/%s refers to @%s/%s, which does not exist'
                                    % (folder, os.path.basename(path), kind, name))
    return problems


def bands():
    """Every row of a portrait bottom band, with what it has to fit across it."""
    rows = []
    for port in sorted(glob.glob(os.path.join(RES, 'layout-port', '*.xml'))):
        for node in ET.parse(port).iter():
            if node.tag.split('}')[-1] != 'LinearLayout':
                continue
            if node.get(A + 'minHeight') != '@dimen/band_min':
                continue
            if node.get(A + 'orientation') == 'horizontal':
                rows.append((os.path.basename(port), [node]))
            else:
                # A band that stacks: each row inside it is measured on its own.
                rows.append((os.path.basename(port),
                             [c for c in node
                              if c.get(A + 'orientation') == 'horizontal']))
    return rows


# A line of Persian in a band needs room to be a line, not a word per row. This is
# about four short words at body size on the smallest phone.
TEXT_MIN_DP = 150


def main():
    problems = parity() + coverage() + references()
    for problem in problems:
        print('  %s' % problem)
    print('')
    for label, dirs, sw in BUCKETS:
        d = dims(dirs)
        usable = sw - 2 * d['screen_padding']
        for name, rows in bands():
            for row in rows:
                fixed = 0.0
                flexible = 0
                for child in row:
                    width = child.get(A + 'layout_width')
                    if width == '0dp':
                        flexible += 1
                        continue
                    if width and width.startswith('@dimen/'):
                        fixed += d.get(width.split('/')[1], 0.0)
                    elif child.tag.split('}')[-1] == 'include':
                        fixed += d['mascot_dock']
                    fixed += d['gap']
                left = usable - fixed
                if flexible and left < TEXT_MIN_DP:
                    problems.append('%s %s' % (label, name))
                    print('  %s  %-28s a row leaves %3.0fdp for its text, under the '
                          '%ddp a line needs' % (label, name, left, TEXT_MIN_DP))
    print('\n%s' % ('FAIL: %d problem(s)' % len(problems) if problems
                    else 'PASS: portrait matches landscape id for id, and every '
                         'bottom band fits the short edge'))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
