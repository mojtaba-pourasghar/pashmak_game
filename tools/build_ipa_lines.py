#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Turns every line the app speaks into IPA, for a phoneme-driven voice.

Some good Persian TTS models take IPA rather than Persian script — for example
KiaBush/Persian-IPA-to-Speech-F5 on Hugging Face. That moves the hard part of
Persian off the model and onto whatever produces the phonemes, and it is where
the hand-vowelised manifest earns its keep, because Persian script leaves the
short vowels out and a transcriber has to guess at them. Compare:

    سلام قندعسلم  ->  salˈɑm ɢˌanadʔasˈalam      the plain spelling
    سَلام قَندِعَسَلَم  ->  salˈɑm ɢˌandeʔasˈalam      vowelised, with the ezafe

The second is right. «دوستِ جَدیدت» likewise comes out dˈuːste dʒadˈidat rather
than dˈuːst dʒadˈidat — the ezafe /e/ that joins the two words only exists in the
text because it was written in by hand.

Two things here are not decoration:

espeak is called once per line. Handing it a file of 1,136 lines looks faster and
is wrong: it splits its output at every «!» and «،», so 1,136 lines come back as
1,909 and every name after the first multi-sentence line is matched to somebody
else's phonemes. Nothing would look broken; the app would simply say the wrong
words for the rest of its life.

And q1 is repaired. espeak leaks its own internal name for ق into what it calls
IPA — 526 times across this corpus, which is nearly half the lines — so «قند»
arrives as q1and. It is written as ɢ, which is what that letter is in Persian.

    python3 tools/build_ipa_lines.py
"""
import collections
import io
import json
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
OUT = os.path.join(ROOT, 'tools/all_game_ipa.json')

# espeak's internal phoneme names, as they leak into its --ipa output.
REPAIRS = [
    ('q1', 'ɢ'),        # ق — a voiced uvular stop in Persian, not a q
]

# What is allowed through. Anything else is reported rather than shipped.
ALLOWED = set(
    'abdefghijklmnoprstuvwxyz'          # plain letters espeak uses
    'æɑɒəɛɜɡɣɢħɪʁʃʒʔʕθðŋɲʊʌχ'          # the IPA it needs for Persian
    'ːˈˌ̃'                              # length, primary and secondary stress
    ' '
)


HARAKAT = '\u064b\u064c\u064d\u064e\u064f\u0650\u0652\u0670'
SHADDA = '\u0651'


def degeminate(text):
    """Writes a shadda out as the doubled consonant it stands for.

    espeak does not understand it: given «غُصِّه» it says the *name* of the mark,
    so the line comes back as ɢˌosetˈaʃdidh — Pashmak announcing "ghosse-tashdid".
    76 of the 1,136 lines were doing that. Persian gemination is just the
    consonant twice, so that is what espeak is handed; the manifest itself is
    never touched, because the shadda is correct there and a neural voice reads
    it properly.
    """
    if SHADDA not in text:
        return text
    out = list(text)
    i = len(out) - 1
    while i >= 0:
        if out[i] == SHADDA:
            # Back up over this consonant's vowels to reach the consonant itself.
            j = i - 1
            while j >= 0 and out[j] in HARAKAT:
                j -= 1
            del out[i]
            if j >= 0:
                out.insert(j + 1, out[j])
        i -= 1
    return ''.join(out)


def ipa_for(text):
    """One line in, one line of IPA out — espeak's own sentence splits folded."""
    done = subprocess.run(
        ['espeak-ng', '-v', 'fa', '--ipa', '-q', '--', degeminate(text)],
        capture_output=True, text=True)
    if done.returncode != 0:
        raise RuntimeError(done.stderr.strip() or 'espeak failed')
    phonemes = ' '.join(done.stdout.split())
    for bad, good in REPAIRS:
        phonemes = phonemes.replace(bad, good)
    return phonemes


def main():
    if not os.path.exists(LINES):
        sys.exit('run tools/build_voice_lines.py first')
    lines = json.load(io.open(LINES, encoding='utf-8'))

    ipa, switched, strange = {}, [], collections.Counter()
    for name in sorted(lines):
        phonemes = ipa_for(' '.join(lines[name].split()))
        # espeak brackets a run it decided was another language, which for this
        # corpus means it met something it could not read as Persian.
        if '(' in phonemes:
            switched.append(name)
            phonemes = re.sub(r'\([^)]*\)', ' ', phonemes)
            phonemes = ' '.join(phonemes.split())
        for c in phonemes:
            if c not in ALLOWED:
                strange[c] += 1
        ipa[name] = phonemes

    with io.open(OUT, 'w', encoding='utf-8') as out:
        out.write(json.dumps(ipa, ensure_ascii=False, indent=1, sort_keys=True))

    print('lines transcribed : %d' % len(ipa))
    print('empty (a problem) : %d' % sum(1 for v in ipa.values() if not v))
    if switched:
        print('espeak switched language on %d line(s), now stripped: %s'
              % (len(switched), ', '.join(switched[:8])))
    if strange:
        print('symbols outside the expected set, worth a look before use:')
        for c, n in strange.most_common():
            print('   %r  U+%04X  %d' % (c, ord(c), n))
    else:
        print('every symbol is one the model should know')
    print('\nwrote %s' % os.path.relpath(OUT, ROOT))


if __name__ == '__main__':
    main()
