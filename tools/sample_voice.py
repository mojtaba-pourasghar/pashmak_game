#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""One line of Pashmak, read several ways, so the voice can be chosen by ear.

Run this before tools/gen_voice_neural.py. It speaks a single line — the
greeting by default — at a handful of settings around the warm, mid-pitched
reading we are after, and writes them as mp3 next to each other. mp3, not ogg,
so there is nothing to install beyond edge-tts itself: no ffmpeg, no oggenc.
Listen, pick the one that sounds like a small boy bear, and tell me the letter.

    pip install edge-tts
    python3 tools/sample_voice.py

    python3 tools/sample_voice.py story_magic_0      # hear the story voice
    python3 tools/sample_voice.py lullaby_1          # or the bedtime one

This cannot run inside Claude's sandbox. edge-tts speaks over a WebSocket and
the proxy there answers 403 to the upgrade; Azure's REST endpoint and
ElevenLabs are both refused by the egress policy as well. So it has to be your
machine.
"""
import asyncio
import io
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for                     # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
OUT = os.path.join(ROOT, 'tools/voice_samples')
VOICE = 'fa-IR-FaridNeural'

# Five readings of the same words, bracketing the target. The middle one is what
# gen_voice_neural.py uses today for a line said during a game.
VARIANTS = [
    ('a_deeper',   '-6%',  '+30Hz', 'بم‌تر و سنگین‌تر'),
    ('b_middle',   '-6%',  '+38Hz', 'میانه — پیشنهاد من'),
    ('c_current',  '-6%',  '+42Hz', 'همین که الان در اسکریپت هست'),
    ('d_brighter', '-4%',  '+50Hz', 'زیرتر و سرزنده‌تر'),
    ('e_warmer',   '-12%', '+34Hz', 'آرام‌تر و گرم‌تر'),
]


def polish(text):
    """Small repairs so the reading is of Persian and not of its typography."""
    t = text.strip()
    t = t.replace('ي', 'ی').replace('ك', 'ک')      # the Arabic forms of two letters
    for verb in ('گفت:', 'پرسید:', 'گفتن:', 'می‌گفت:'):
        t = t.replace(verb, verb[:-1] + '، ')
    t = t.replace('‌', ' ')                    # the half-space is silent
    return ' '.join(t.split())


async def main():
    import edge_tts

    name = sys.argv[1] if len(sys.argv) > 1 else 'welcome'
    lines = json.load(io.open(LINES, encoding='utf-8'))
    if name not in lines:
        sys.exit('no line called %s — run tools/build_voice_lines.py first?' % name)
    text = polish(lines[name])

    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    print('line  : %s   (mood: %s)' % (name, mood_for(name)))
    print('text  : %s' % lines[name])
    print('voice : %s\n' % VOICE)

    for tag, rate, pitch, what in VARIANTS:
        path = os.path.join(OUT, '%s__%s.mp3' % (name, tag))
        try:
            await edge_tts.Communicate(text, VOICE, rate=rate,
                                       pitch=pitch).save(path)
            print('  %-10s %-5s %-6s  %7.1f KB   %s'
                  % (tag, rate, pitch, os.path.getsize(path) / 1024.0, what))
        except Exception as problem:                 # noqa: BLE001 — report and go on
            print('  %-10s FAILED  %s' % (tag, problem))

    print('\nin %s' % os.path.relpath(OUT, ROOT))
    print('tell me which letter and the whole set gets made at those settings.')


if __name__ == '__main__':
    asyncio.run(main())
