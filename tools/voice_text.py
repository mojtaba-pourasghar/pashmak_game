# -*- coding: utf-8 -*-
"""The text the models are given, where it is the same for all of them.

The manifest is hand-vowelised because espeak and the IPA builder need it. A
model that learned from ordinary Persian prose has never seen a fatha, so for
those the marks have to come off — and that had started to be written out once
per generator, which is how two of them ended up disagreeing about it.
"""

import re

HARAKAT = ''.join(chr(c) for c in list(range(0x64b, 0x653)) + [0x670])


def bare(text):
    """The line without its vowel marks.

    A character the model cannot place is either dropped or guessed at, and a
    line full of them is mispronounced in a way that sounds like an accent
    rather than a bug — which is how it was first heard.
    """
    return ''.join(c for c in text if c not in HARAKAT)


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


GLOSSED = re.compile(r'^(\S{1,2})\s*\((.+)\)$')


def glyph(text):
    """A letter or digit line reduced to the word that is actually said.

    The manifest writes these as the character and its name together — «۶ (شِش)»,
    «ا (الف)» — which is right on the page and wrong in the ear: a synthesiser
    either reads the brackets aloud or says the thing twice. The name is what a
    child should hear, so the name is what is sent. Anything longer than two
    characters before the bracket is an ordinary line that happens to contain a
    parenthesis, and is left alone.
    """
    found = GLOSSED.match(text.strip())
    return found.group(2).strip() if found else text


def polish(text):
    """Small repairs so the reading is of Persian and not of its typography."""
    t = glyph(text).strip()
    t = t.replace('ي', 'ی').replace('ك', 'ک')      # the Arabic forms of two letters
    # A breath where someone starts speaking, so quoted dialogue does not run on.
    for verb in ('گفت:', 'پرسید:', 'گفتن:', 'می‌گفت:'):
        t = t.replace(verb, verb[:-1] + '، ')
    return ' '.join(t.split())
