# -*- coding: utf-8 -*-
"""Checks that every clip the app looks for is actually in res/raw.

This is the check that decides whether Pashmak speaks. VoicePlayer resolves a clip
by name through Resources#getIdentifier, and a name that is not there is a silent
no-op — no crash, no log, nothing. So one wrong name is a line that never sounds and
nothing anywhere says so. The names are worked out the same way the app works them
out, from AudioManifest and the catalogues, and matched against the files on disk.
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(ROOT, 'app/src/main')
RAW = os.path.join(MAIN, 'res/raw')
JAVA = os.path.join(MAIN, 'java/ir/brandimo/pashmak')
CATALOG = os.path.join(JAVA, 'data/catalog')


def present():
    """Clip names in res/raw, without their extension."""
    out = set()
    for entry in os.listdir(RAW):
        stem, dot, ext = entry.rpartition('.')
        if ext in ('ogg', 'mp3', 'wav', 'm4a'):
            out.add(stem)
    return out


def wanted():
    """Every name the app can ask for, split into what must sound and what may not."""
    manifest = io.open(os.path.join(JAVA, 'audio/AudioManifest.java'),
                       encoding='utf-8').read()
    speech, effects, music = set(), set(), set()
    for kind, value in re.findall(
            r'public static final String (\w+) = "([^"]+)"', manifest):
        if kind.startswith('BGM_'):
            music.add(value)
        elif kind.startswith('SFX_'):
            effects.add(value)
        else:
            speech.add(value)

    arrays = io.open(os.path.join(MAIN, 'res/values/arrays.xml'), encoding='utf-8').read()
    letters = re.search(r'<string-array name="trace_letters">(.*?)</string-array>',
                        arrays, re.S)
    if letters:
        for glyph in re.findall(r'<item>(.*?)</item>', letters.group(1)):
            speech.add('letter_%x' % ord(glyph[0]))
    for i in range(10):
        speech.add('digit_%d' % i)

    story = io.open(os.path.join(CATALOG, 'StoryCatalog.java'), encoding='utf-8').read()
    for block in re.split(r'Story \w+ = new Story\(', story)[1:]:
        sid = re.match(r'"([^"]+)"', block).group(1)
        beats = re.findall(
            r'StoryBeat\.(narrate|narrateTo|ask|choice|celebrate)\(', block)
        for i, kind in enumerate(beats):
            speech.add('story_%s_%d' % (sid, i))
            if kind == 'ask':
                speech.add('story_%s_%d_yes' % (sid, i))

    tales = io.open(os.path.join(CATALOG, 'TaleCatalog.java'), encoding='utf-8').read()
    for block in re.split(r'new Tale\(', tales)[1:]:
        tid = re.match(r'"([^"]+)"', block).group(1)
        for i, _ in enumerate(re.findall(r'\bat\(\d+, "', block)):
            speech.add('tale_%s_%d' % (tid, i))

    return speech, effects, music


def main():
    have = present()
    speech, effects, music = wanted()

    missing = sorted(speech - have)
    spare = sorted(have - speech - effects - music)

    print('clips in res/raw: %d' % len(have))
    print('lines the app speaks: %d' % len(speech))
    print('')
    if missing:
        print('SILENT — the app asks for these and they are not there:')
        for name in missing[:12]:
            print('    %s' % name)
        if len(missing) > 12:
            print('    ...and %d more' % (len(missing) - 12))
    else:
        print('every spoken line has a clip')

    print('sound effects present: %d of %d  (optional; taps and pops, not speech)'
          % (len(effects & have), len(effects)))
    print('background music present: %d of %d  (the app synthesises a loop without them)'
          % (len(music & have), len(music)))
    if spare:
        print('in res/raw but never asked for: %d  e.g. %s'
              % (len(spare), ', '.join(spare[:4])))

    # aapt refuses anything but lowercase letters, digits and underscores.
    illegal = sorted(n for n in have if not re.fullmatch(r'[a-z0-9_]+', n))
    if illegal:
        print('names aapt will refuse to build: %s' % illegal[:6])

    bad = len(missing) + len(illegal)
    print('\n%s' % ('FAIL: %d problem(s)' % bad if bad
                    else 'PASS: Pashmak has a voice for every line he has'))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
