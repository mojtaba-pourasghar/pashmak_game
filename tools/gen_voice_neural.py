#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks every line in the app with a neural Persian voice.

What this replaces. The clips in res/raw were made with eSpeak, which speaks
Persian but sounds like a synthesiser. This uses a neural voice instead —
warm, natural, and with the pace and lift changed line by line so Pashmak
sounds pleased when the child gets something right and gentle when they do not.

Where the words come from. tools/all_game_lines.json, built by
tools/build_voice_lines.py out of res/raw/audio_manifest.txt, where the text has
been vowelised by hand. Persian leaves the short vowels out and a synthesiser has
to guess, so «شب شده بود» and «شَب شُدِه بُود» are the same words and very
different readings. Never feed this script the catalogue text directly; it is the
plain spelling, which is right for the screen and wrong for the ear.

This cannot run inside Claude's sandbox: edge-tts talks over a WebSocket and the
agent proxy does not carry WebSocket upgrades. Run it on your own machine.

    pip install edge-tts
    # ffmpeg on PATH, or mpg123/lame plus oggenc
    python3 tools/build_voice_lines.py
    python3 tools/gen_voice_neural.py

    python3 tools/gen_voice_neural.py --only tale_       # just one family
    python3 tools/gen_voice_neural.py --force            # redo everything
    python3 tools/gen_voice_neural.py --list-voices      # what else is available
"""
import argparse
import asyncio
import io
import json
import os
import shutil
import subprocess
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for as mood_key   # noqa: E402  (after sys.path)
import voice_output                           # noqa: E402


def _utf8_console():
    """Let Persian reach a Windows console.

    cmd defaults to cp1252, which cannot encode a single Persian letter, so a
    tool that prints the line it is about to speak dies on the print and not on
    the work — and the traceback points at codecs, which is no help at all.
    """
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding='utf-8', errors='replace')
        except (AttributeError, ValueError):               # pre-3.7, or a pipe
            pass


_utf8_console()

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, 'app/src/main/res/raw')
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')

VOICE = 'fa-IR-FaridNeural'

# A clip smaller than this is not speech — it is a failed encode. Writing one over
# a good file is how a working line goes silent with nothing to show for it, so a
# clip that comes out this small is thrown away and the old one kept.
MIN_BYTES = 1200


# --- how each kind of line is said -------------------------------------------
#
# Persian neural voices do not take Azure's express-as style tags, so the feeling
# is carried by pace, lift and loudness instead — which is most of what a listener
# actually hears. Ordered: the first pattern that matches a clip name wins.
# How each mood is bent out of a flat reading. Which line is in which mood is
# not decided here — tools/voice_moods.py holds that, so this script and
# tools/gen_voice_aistudio.py cannot drift apart on it.
MOODS = {
    'delighted': dict(rate='-2%', pitch='+48Hz', volume='+8%'),
    'kind':      dict(rate='-12%', pitch='+30Hz', volume='-6%'),
    'bedtime':   dict(rate='-20%', pitch='+26Hz', volume='-12%'),
    'glyph':     dict(rate='-22%', pitch='+38Hz', volume='+0%'),
    'story':     dict(rate='-14%', pitch='+36Hz', volume='+0%'),
    'game':      dict(rate='-6%', pitch='+42Hz', volume='+0%'),
}


def mood_for(name):
    return MOODS[mood_key(name)]


def polish(text):
    """Small repairs that make a neural voice read Persian the way a person would."""
    t = text.strip()
    t = t.replace('ي', 'ی').replace('ك', 'ک')          # Arabic forms of two letters
    # A breath where someone starts speaking, so quoted dialogue does not run on.
    for verb in ('گفت:', 'پرسید:', 'گفتن:', 'می‌گفت:'):
        t = t.replace(verb, verb[:-1] + '، ')
    # The half-space joins words in print and means nothing aloud; left in, some
    # engines read it as a break in the middle of a word.
    t = t.replace('‌', ' ')
    return ' '.join(t.split())


# --- turning the engine's mp3 into something Android will play ----------------

def encoder():
    """ffmpeg if it is here, otherwise an mp3 decoder plus oggenc."""
    if shutil.which('ffmpeg'):
        return 'ffmpeg'
    if shutil.which('oggenc') and (shutil.which('mpg123') or shutil.which('lame')):
        return 'oggenc'
    return None


def to_ogg(mp3, ogg, how):
    """Mono 24 kHz Vorbis: what Android's MediaPlayer is happiest with."""
    if how == 'ffmpeg':
        subprocess.run(['ffmpeg', '-y', '-i', mp3, '-ac', '1', '-ar', '24000',
                        '-c:a', 'libvorbis', '-q:a', '4', ogg],
                       check=True, capture_output=True)
        return
    wav = mp3 + '.wav'
    try:
        if shutil.which('mpg123'):
            subprocess.run(['mpg123', '-q', '-m', '-r', '24000', '-w', wav, mp3],
                           check=True, capture_output=True)
        else:
            subprocess.run(['lame', '--quiet', '--decode', '-m', 'm', mp3, wav],
                           check=True, capture_output=True)
        subprocess.run(['oggenc', '-Q', '-q', '4', '--resample', '24000',
                        '-o', ogg, wav], check=True, capture_output=True)
    finally:
        if os.path.exists(wav):
            os.remove(wav)


async def speak(name, text, how, sem, force, stats):
    import edge_tts

    final = os.path.join(RAW, name + '.ogg')
    if not force and os.path.exists(final) and os.path.getsize(final) >= MIN_BYTES:
        stats['kept'] += 1
        return True

    mood = mood_for(name)
    words = polish(text)
    async with sem:
        for attempt in range(3):
            mp3 = tempfile.mktemp(suffix='.mp3')
            staged = final + '.new'
            try:
                talk = edge_tts.Communicate(words, VOICE, rate=mood['rate'],
                                            pitch=mood['pitch'],
                                            volume=mood['volume'])
                await talk.save(mp3)
                if os.path.getsize(mp3) < MIN_BYTES:
                    raise RuntimeError('engine returned almost nothing')
                to_ogg(mp3, staged, how)
                if os.path.getsize(staged) < MIN_BYTES:
                    raise RuntimeError('encode produced almost nothing')
                # Only now is the old clip replaced, so a failure never leaves the
                # app with a file that exists and says nothing.
                os.replace(staged, final)
                stats['made'] += 1
                return True
            except Exception as err:
                if attempt == 2:
                    stats['failed'].append('%s (%s)' % (name, err))
                else:
                    await asyncio.sleep(1.5 * (attempt + 1))
            finally:
                for path in (mp3, staged):
                    if os.path.exists(path):
                        os.remove(path)
    return False


async def one(args, text, name, stem):
    """Speak a single line into tools/voice_samples, never into res/raw.

    A sample has to be listened to before 1,136 clips are committed to a voice,
    and it must not touch what the app loads while it is being judged.
    """
    import edge_tts
    how = encoder()
    if how is None:
        print('No encoder. Install ffmpeg, or mpg123/lame together with oggenc.')
        return 1
    out = os.path.join(ROOT, 'tools/voice_samples')
    os.makedirs(out, exist_ok=True)
    mood = mood_for(name)
    words = polish(text)
    print('voice : %s' % VOICE)
    print('mood  : %-10s rate %s  pitch %s  volume %s'
          % (mood_key(name), mood['rate'], mood['pitch'], mood['volume']))
    print('text  : %s' % words)
    mp3 = os.path.join(out, 'neural_%s.mp3' % stem)
    ogg = os.path.join(out, 'neural_%s.ogg' % stem)
    talk = edge_tts.Communicate(words, VOICE, rate=mood['rate'],
                                pitch=mood['pitch'], volume=mood['volume'])
    await talk.save(mp3)
    to_ogg(mp3, ogg, how)
    os.remove(mp3)
    print('\nwrote %s  (%.1f KB)'
          % (os.path.relpath(ogg, ROOT), os.path.getsize(ogg) / 1024.0))
    return 0


async def fresh(args):
    """Start the whole set again, checking everything before deleting anything.

    The encoder is proved and the engine has actually answered with audio before
    a single clip goes, because a name missing from res/raw is not a silence —
    the Java refers to each one by name, so it is a compile error.
    """
    import edge_tts
    if encoder() is None:
        print('No encoder. Install ffmpeg, or mpg123/lame together with oggenc.')
        return 1
    with io.open(LINES, encoding='utf-8') as handle:
        lines = json.load(handle)
    print('one test call before anything is deleted ... ', end='', flush=True)
    probe = os.path.join(ROOT, 'tools/voice_samples')
    os.makedirs(probe, exist_ok=True)
    mp3 = os.path.join(probe, 'neural_probe.mp3')
    talk = edge_tts.Communicate(polish(lines.get('welcome', 'سلام')), VOICE)
    await talk.save(mp3)
    size = os.path.getsize(mp3)
    os.remove(mp3)
    if size < MIN_BYTES:
        print('the engine returned almost nothing (%d bytes) — stopping' % size)
        return 1
    print('%d bytes of mp3' % size)
    voice_output.rebuild_lines(ROOT)
    voice_output.clear_spoken(RAW, lines)
    args.force = True
    return await run(args)


async def run(args):
    with open(LINES, encoding='utf-8') as f:
        lines = json.load(f)
    if args.only:
        lines = {n: t for n, t in lines.items() if n.startswith(args.only)}
    if not lines:
        print('nothing matches --only %s' % args.only)
        return 1

    how = encoder()
    if how is None:
        print('No encoder. Install ffmpeg, or mpg123/lame together with oggenc.')
        return 1

    os.makedirs(RAW, exist_ok=True)
    print('%d lines, voice %s, encoding with %s' % (len(lines), VOICE, how))
    print('re-doing clips that already exist' if args.force
          else 'keeping clips that are already there (use --force to redo)')

    stats = {'made': 0, 'kept': 0, 'failed': []}
    sem = asyncio.Semaphore(args.jobs)
    tasks = [speak(n, t, how, sem, args.force, stats) for n, t in sorted(lines.items())]
    done = 0
    for future in asyncio.as_completed(tasks):
        await future
        done += 1
        if done % 50 == 0 or done == len(tasks):
            print('  %d/%d — %d new, %d kept, %d failed'
                  % (done, len(tasks), stats['made'], stats['kept'],
                     len(stats['failed'])))

    print('\n%d spoken, %d already there, %d failed'
          % (stats['made'], stats['kept'], len(stats['failed'])))
    for line in stats['failed'][:20]:
        print('    %s' % line)
    if stats['failed']:
        print('Run again to retry only those; what worked is kept.')
        return 1
    print('\nNow check nothing is missing:  python3 tools/voicefit.py')
    return 0


async def show_voices():
    import edge_tts
    for voice in await edge_tts.list_voices():
        if voice['Locale'].startswith('fa'):
            print('%-26s %-8s %s' % (voice['ShortName'], voice['Gender'],
                                     voice.get('FriendlyName', '')))
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true',
                        help='redo clips that already exist')
    parser.add_argument('--jobs', type=int, default=8,
                        help='how many lines to speak at once (default 8)')
    parser.add_argument('--list-voices', action='store_true',
                        help='show the Persian voices the engine offers')
    parser.add_argument('--sample', nargs='?', const='welcome',
                        help='speak one line into tools/voice_samples and stop')
    parser.add_argument('--text', nargs='?', const='',
                        help='speak this sentence instead, and stop')
    parser.add_argument('--fresh', action='store_true',
                        help='rebuild the lines from the manifest, delete the '
                             'clips of the last voice, and speak them all again')
    args = parser.parse_args()

    try:
        import edge_tts                                    # noqa: F401
    except ImportError:
        print('edge-tts is not installed:  pip install edge-tts')
        return 1
    if args.list_voices:
        return asyncio.run(show_voices())
    if not os.path.exists(LINES):
        print('%s is missing. Build it first:\n'
              '    python3 tools/build_voice_lines.py'
              % os.path.relpath(LINES, ROOT))
        return 1
    with io.open(LINES, encoding='utf-8') as handle:
        lines = json.load(handle)
    if args.text is not None:
        text = args.text.strip() or lines.get('welcome', 'سلام')
        return asyncio.run(one(args, text, 'welcome', 'text'))
    if args.sample:
        if args.sample not in lines:
            print('no line called %s' % args.sample)
            return 1
        return asyncio.run(one(args, lines[args.sample], args.sample, 'sample'))
    if args.fresh:
        return asyncio.run(fresh(args))
    return asyncio.run(run(args))


if __name__ == '__main__':
    sys.exit(main())
