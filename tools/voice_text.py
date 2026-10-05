# -*- coding: utf-8 -*-
"""The text the models are given, where it is the same for all of them.

The manifest is hand-vowelised because espeak and the IPA builder need it. A
model that learned from ordinary Persian prose has never seen a fatha, so for
those the marks have to come off — and that had started to be written out once
per generator, which is how two of them ended up disagreeing about it.
"""

HARAKAT = ''.join(chr(c) for c in list(range(0x64b, 0x653)) + [0x670])


def bare(text):
    """The line without its vowel marks.

    A character the model cannot place is either dropped or guessed at, and a
    line full of them is mispronounced in a way that sounds like an accent
    rather than a bug — which is how it was first heard.
    """
    return ''.join(c for c in text if c not in HARAKAT)
