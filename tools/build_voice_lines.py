# -*- coding: utf-8 -*-
"""Builds tools/all_game_lines.json — the exact text each clip should say.

The text comes from res/raw/audio_manifest.txt, where it has been vowelised by
hand. Persian script leaves the short vowels out and a synthesiser has to guess,
so «شب شده بود» and «شَب شُدِه بُود» are the same words and very different
readings. That hand work is the point of this file; the catalogues in Java keep
the plain spelling, because that is what a child should see on screen.

Anything the manifest does not carry falls back to the catalogue text, and the
tool says which ones did, so the gap is visible rather than silently unvowelised.

Run: python3 tools/build_voice_lines.py
"""
import io, json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'tools'))
import voicefit                                            # noqa: E402
import gen_voice                                           # noqa: E402

MANIFEST = os.path.join(ROOT, 'app/src/main/res/raw/audio_manifest.txt')
OUT = os.path.join(ROOT, 'tools/all_game_lines.json')
HARAKAT = 'ًٌٍَُِّْٰ'


def from_manifest(names):
    """Looks each clip up by name, because the file is not one single format.

    Some rows are «name: text» and some are «  name   text», and a parser written
    for one shape quietly drops the other — which is how twenty lines went missing
    the first time this was counted.
    """
    text = io.open(MANIFEST, encoding='utf-8').read().split('\n')
    found = {}
    for line in text:
        stripped = line.strip()
        for sep in (':', ' '):
            head, _, tail = stripped.partition(sep)
            head = head.strip()
            if head in names and tail.strip():
                # The longest match wins: story_x_1 must not swallow story_x_1_yes.
                if head not in found or len(tail.strip()) > len(found[head]):
                    found[head] = tail.strip()
                break
    return found


def main():
    speech, _effects, _music = voicefit.wanted()
    catalogue = dict(gen_voice.lines())          # the plain text, as a fallback

    voiced = from_manifest(speech)
    out, plain, absent = {}, [], []
    for name in sorted(speech):
        if name in voiced:
            out[name] = voiced[name]
            if not any(c in HARAKAT for c in voiced[name]):
                plain.append(name)
        elif name in catalogue:
            out[name] = catalogue[name]
            absent.append(name)
        else:
            absent.append(name)

    io.open(OUT, 'w', encoding='utf-8').write(
        json.dumps(out, ensure_ascii=False, indent=1, sort_keys=True))

    print('lines the app speaks : %d' % len(speech))
    print('taken from the manifest, with vowels : %d'
          % (len(out) - len(plain) - len([a for a in absent if a in out])))
    print('in the manifest but not vowelised    : %d' % len(plain))
    print('fell back to the catalogue           : %d' % len(absent))
    for name in absent[:12]:
        print('    %s' % name)
    if len(absent) > 12:
        print('    ...and %d more' % (len(absent) - 12))
    missing = sorted(speech - set(out))
    if missing:
        print('NO TEXT AT ALL for %d clips: %s' % (len(missing), missing[:5]))
        return 1
    print('\nwrote %s (%d lines)' % (os.path.relpath(OUT, ROOT), len(out)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
