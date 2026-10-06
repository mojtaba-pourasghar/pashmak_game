#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks every line in the app with Google AI Studio's voice (Gemini TTS).

Why this one and not tools/gen_voice_neural.py. That script uses edge-tts, which
talks over a WebSocket, and the agent proxy here does not carry WebSocket
upgrades — so it can only ever run on your own machine. This one is plain HTTPS
to generativelanguage.googleapis.com, which *is* reachable, so the audio can be
made right here as soon as there is a key. It is also the better instrument: the
voice is directed in words rather than in percentages, so «sound genuinely
delighted and proud of the child» is the actual instruction, and the emotion
comes from the model rather than from bending the pitch of a flat reading.

The key. Make one at https://aistudio.google.com/apikey and put it in the
environment as GEMINI_API_KEY. It is read from the environment and never
written to a file, printed, or committed.

Where the words come from. tools/all_game_lines.json, built by
tools/build_voice_lines.py out of res/raw/audio_manifest.txt, where the Persian
has been vowelised by hand. Persian leaves the short vowels out and a
synthesiser has to guess; «شب شده بود» and «شَب شُدِه بُود» are the same words
and two different readings. Never feed this the catalogue text instead — that is
the plain spelling, right for the screen and wrong for the ear.

    export GEMINI_API_KEY=...
    python3 tools/build_voice_lines.py
    python3 tools/gen_voice_aistudio.py --sample        # hear one line first
    python3 tools/gen_voice_aistudio.py                 # then all 1,136

    python3 tools/gen_voice_aistudio.py --only tale_    # one family
    python3 tools/gen_voice_aistudio.py --force         # redo what is there
    python3 tools/gen_voice_aistudio.py --list-voices

One caveat worth reading before spending a key on 1,136 calls: Persian is not on
Google's published list of languages for these TTS models. It may well read it
anyway — the underlying model knows Persian — but that is a thing to find out
from --sample, not from a full run. That is what --sample is for.
"""
import argparse
import base64
import json
import io
import os
import ssl
import sys
import time
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import voice_output                                        # noqa: E402
from voice_text import polish                              # noqa: E402
from voice_moods import (MOOD_DIRECTION, PERSONA, direction,
                         mood_for, tally)           # noqa: E402  (after sys.path)


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
# res/raw already holds a full set of clips from the old espeak run, so "skip
# what is already on disk" would skip everything. What has actually been spoken
# by this engine is written down here instead, and that is what a resume reads —
# which matters, because the free tier stops after ten calls a day and the run
# has to pick up later, on a different key.
LEDGER = os.path.join(ROOT, 'tools/voice_done.json')
ENDPOINT = ('https://generativelanguage.googleapis.com/v1beta/models/'
            '%s:generateContent')
MODEL = 'gemini-2.5-flash-preview-tts'

# Under this and it is a failed encode, not speech.
MIN_BYTES = 1200

# The prebuilt voices, with the character Google gives each one. Pashmak is a
# small soft bear, so the shortlist at the top is the warm mid-pitched end of it.
VOICES = [
    ('Algieba', 'smooth'), ('Achird', 'friendly'), ('Umbriel', 'easy-going'),
    ('Iapetus', 'clear'), ('Schedar', 'even'),
    ('Sulafat', 'warm'), ('Achernar', 'soft'), ('Vindemiatrix', 'gentle'),
    ('Despina', 'smooth'),
    ('Callirrhoe', 'easy-going'), ('Schedar', 'even'), ('Iapetus', 'clear'),
    ('Charon', 'informative'), ('Rasalgethi', 'informative'),
    ('Zephyr', 'bright'), ('Puck', 'upbeat'), ('Leda', 'youthful'),
    ('Aoede', 'breezy'), ('Kore', 'firm'), ('Orus', 'firm'),
    ('Enceladus', 'breathy'), ('Gacrux', 'mature'), ('Erinome', 'clear'),
    ('Laomedeia', 'upbeat'), ('Autonoe', 'bright'), ('Alnilam', 'firm'),
    ('Sadachbia', 'lively'), ('Zubenelgenubi', 'casual'), ('Fenrir', 'excitable'),
    ('Algenib', 'gravelly'), ('Sadaltager', 'knowledgeable'),
    ('Pulcherrima', 'forward'),
]
# Pashmak is a boy bear, so the shortlist above leads with the male voices
# that sit in the middle of the range; Sulafat is warm but reads female.
DEFAULT_VOICE = 'Umbriel'

# --- talking to the API -------------------------------------------------------

def api_key():
    key = os.environ.get('GEMINI_API_KEY') or os.environ.get('GOOGLE_API_KEY')
    if not key:
        sys.exit('set GEMINI_API_KEY first — make one at '
                 'https://aistudio.google.com/apikey')
    return key


def _context():
    """Trust whatever CA bundle this machine was told to use."""
    bundle = (os.environ.get('SSL_CERT_FILE')
              or os.environ.get('REQUESTS_CA_BUNDLE'))
    if bundle and os.path.exists(bundle):
        return ssl.create_default_context(cafile=bundle)
    return ssl.create_default_context()


def speak(text, name, voice, model, key, tries=9, empty_tries=3):
    """One line of Persian, returned as raw PCM plus its sample rate.

    A reply shaped like an answer but with no audio in it — finishReason OTHER —
    happens now and then on a short line, and it is transient: the same request
    sent again comes back with speech. So it is retried like a rate limit rather
    than counted as a failure, which is what scattered single losses through a
    run of 1,136.
    """
    for attempt in range(empty_tries):
        try:
            return _once(text, name, voice, model, key, tries)
        except RuntimeError as problem:
            if 'no audio came back' not in str(problem) or attempt == empty_tries - 1:
                raise
            time.sleep(2.0 * (attempt + 1))


def _once(text, name, voice, model, key, tries=9):
    body = json.dumps({
        'contents': [{'parts': [{'text': '%s\n\n%s' % (direction(name),
                                                       polish(text))}]}],
        'generationConfig': {
            'responseModalities': ['AUDIO'],
            'speechConfig': {
                'voiceConfig': {'prebuiltVoiceConfig': {'voiceName': voice}},
            },
        },
    }).encode('utf-8')

    delay = 2.0
    for attempt in range(tries):
        request = urllib.request.Request(
            ENDPOINT % model, data=body,
            headers={'Content-Type': 'application/json', 'x-goog-api-key': key})
        try:
            with urllib.request.urlopen(request, timeout=180,
                                        context=_context()) as answer:
                payload = json.loads(answer.read().decode('utf-8'))
            break
        except urllib.error.HTTPError as problem:
            detail = problem.read().decode('utf-8', 'replace')[:300]
            # Rate limits and server trouble are worth waiting out; a bad key or
            # a malformed request will never get better by asking again.
            if problem.code in (429, 500, 502, 503, 504) and attempt < tries - 1:
                time.sleep(delay)
                delay *= 2
                continue
            raise RuntimeError('HTTP %d: %s' % (problem.code, detail))
        except urllib.error.URLError as problem:
            if attempt < tries - 1:
                time.sleep(delay)
                delay *= 2
                continue
            raise RuntimeError(str(problem))
    else:
        raise RuntimeError('gave up after %d attempts' % tries)

    try:
        part = payload['candidates'][0]['content']['parts'][0]['inlineData']
    except (KeyError, IndexError):
        # A refusal or a safety block comes back shaped like a reply with no
        # audio in it, and saying so beats a stack trace.
        raise RuntimeError('no audio came back: %s' % json.dumps(payload)[:300])
    rate = 24000
    for bit in part.get('mimeType', '').split(';'):
        if bit.strip().startswith('rate='):
            rate = int(bit.strip()[5:])
    return base64.b64decode(part['data']), rate


# --- turning that PCM into something Android will play ------------------------

def write_clip(text, name, voice, model, key, path, shaped=True):
    """One clip, staged, so an interrupted run never leaves a silent file.

    The encoding and the shaping are voice_output's, the same as the other
    generators use: either oggenc or ffmpeg, the presence lift that makes a line
    carry to a child who is not listening carefully, and one level for the whole
    app. This used to call oggenc directly with raw-PCM flags, which meant no
    shaping and a hard dependency on vorbis-tools.
    """
    import numpy as np
    pcm, rate = speak(text, name, voice, model, key)
    samples = np.frombuffer(pcm, dtype='<i2')
    staged = path + '.part'
    final, size, _pcm = voice_output.write_clip(samples, rate, staged, shaped)
    if size < MIN_BYTES:
        os.unlink(final)
        raise RuntimeError('encoded to almost nothing (%d bytes)' % size)
    os.replace(final, path)
    return size


# --- the run ------------------------------------------------------------------

def read_ledger():
    if not os.path.exists(LEDGER):
        return {'voice': None, 'model': None, 'done': []}
    return json.load(io.open(LEDGER, encoding='utf-8'))


def write_ledger(ledger):
    """Written after every clip, and moved into place, so a crash mid-write
    cannot leave a half-file that loses the whole run's record."""
    staged = LEDGER + '.part'
    with io.open(staged, 'w', encoding='utf-8') as out:
        out.write(json.dumps(ledger, ensure_ascii=False, indent=1,
                             sort_keys=True))
    os.replace(staged, LEDGER)


def load_lines():
    if not os.path.exists(LINES):
        sys.exit('run tools/build_voice_lines.py first — %s is missing' % LINES)
    return json.load(io.open(LINES, encoding='utf-8'))


def do_sample(args, key):
    """One line, in several voices, so it can be chosen by ear before the rest."""
    lines = load_lines()
    name = args.sample if args.sample != 'welcome' or 'welcome' in lines \
        else sorted(lines)[0]
    if name not in lines:
        sys.exit('no line called %s' % name)
    voices = args.voices.split(',') if args.voices else \
        [v for v, _ in VOICES[:5]]
    out = os.path.join(ROOT, 'tools/voice_samples')
    if not os.path.isdir(out):
        os.makedirs(out)
    print('line   : %s  (%s)' % (name, mood_for(name)))
    print('text   : %s' % lines[name])
    print('model  : %s' % args.model)
    print()
    for voice in voices:
        path = os.path.join(out, '%s__%s.ogg' % (name, voice))
        try:
            size = write_clip(lines[name], name, voice, args.model, key, path)
            print('  %-14s %6.1f KB  %s' % (voice, size / 1024.0,
                                            os.path.relpath(path, ROOT)))
        except RuntimeError as problem:
            print('  %-14s FAILED  %s' % (voice, problem))
    print('\nlisten to these and tell me which voice; then the same command '
          'without --sample does all of them.')


def do_fresh(args, key):
    """Start the whole set again: nothing deleted until it can be replaced.

    The encoder is proved and the API has actually answered with audio before a
    single clip goes, because a name missing from res/raw is not a silence — the
    Java refers to each one by name, so it is a compile error.
    """
    voice_output.check_encoder(os.path.join(ROOT, 'tools/aistudio'))
    lines = load_lines()
    print('one test call before anything is deleted ... ', end='', flush=True)
    pcm, rate = speak(lines.get('welcome', 'سَلام!'), 'welcome', args.voice,
                      args.model, key)
    print('%d bytes of audio at %d Hz' % (len(pcm), rate))
    voice_output.rebuild_lines(ROOT)
    voice_output.clear_spoken(RAW, load_lines())
    if os.path.exists(LEDGER):
        os.unlink(LEDGER)
    do_all(args, key)


def do_all(args, key):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    if not os.path.isdir(RAW):
        sys.exit('no res/raw at %s' % RAW)

    ledger = read_ledger()
    if ledger['voice'] and ledger['voice'] != args.voice and not args.force:
        sys.exit('the ledger is half a run in %s; pass --force to start over in %s'
                 % (ledger['voice'], args.voice))
    ledger['voice'], ledger['model'] = args.voice, args.model
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do, voice %s, model %s'
          % (len(names), len(names) - len(todo), len(todo), args.voice, args.model))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %d' % (mood, count))
    print()

    done = failed = starved = 0
    for i, name in enumerate(todo, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            size = write_clip(lines[name], name, args.voice, args.model,
                              key, path, shaped=not args.plain)
            done += 1
            ledger['done'].append(name)
            write_ledger(ledger)
            starved = 0
            if i % 20 == 0 or i == len(todo):
                print('[%4d/%4d] %-28s %-10s %6.1f KB'
                      % (i, len(todo), name, mood_for(name), size / 1024.0),
                      flush=True)
        except RuntimeError as problem:
            failed += 1
            short = ' '.join(str(problem).split())[:110]
            print('[%4d/%4d] %-28s FAILED  %s' % (i, len(todo), name, short),
                  flush=True)
            # A single quota error is not the end any more. On the free tier it
            # was, but a paid key hits a per-minute ceiling constantly and that
            # is a pause, not a wall — speak() already waits it out. Only a run
            # of them in a row, after all that waiting, means the key is spent.
            if 'quota' in str(problem).lower() or 'RESOURCE_EXHAUSTED' in str(problem):
                starved += 1
                if starved >= 3:
                    print('\nthe key is out of quota. %d spoken this run, %d in '
                          'all.' % (done, len(ledger['done'])))
                    print('give me another key and the same command carries on '
                          'from %s.' % name)
                    return
            else:
                starved = 0
            if failed >= 5 and done == 0:
                sys.exit('five failures and nothing written — stopping before '
                         'this burns through the quota')
        if args.pause:
            time.sleep(args.pause)
    print('\nwrote %d, failed %d, %d of %d done in all'
          % (done, failed, len(ledger['done']), len(names)))
    if failed:
        print('re-run to pick up what is missing; finished clips are skipped')


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--voice', default=DEFAULT_VOICE)
    parser.add_argument('--model', default=MODEL)
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true',
                        help='redo clips that are already there')
    parser.add_argument('--pause', type=float, default=0.0,
                        help='seconds between calls, if the quota is tight')
    parser.add_argument('--sample', nargs='?', const='welcome',
                        help='render one line in several voices and stop')
    parser.add_argument('--voices', help='comma-separated, for --sample')
    parser.add_argument('--list-voices', action='store_true')
    parser.add_argument('--plain', action='store_true',
                        help='the model\'s own sound, unshaped')
    parser.add_argument('--fresh', action='store_true',
                        help='rebuild the lines from the manifest, delete the '
                             'clips of the last voice, and speak them all again')
    args = parser.parse_args()

    if args.list_voices:
        for name, character in VOICES:
            print('  %-16s %s' % (name, character))
        return
    key = api_key()
    if args.sample:
        do_sample(args, key)
    elif args.fresh:
        do_fresh(args, key)
    else:
        voice_output.check_encoder(os.path.join(ROOT, 'tools/aistudio'))
        do_all(args, key)


if __name__ == '__main__':
    main()
