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
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for, tally           # noqa: E402  (after sys.path)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, 'app/src/main/res/raw')
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
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
DEFAULT_VOICE = 'Algieba'

# Who is speaking, on every single line. This is the "middle pitch and warm"
# part, said once so it cannot drift between moods.
PERSONA = (
    'You are Pashmak, a small soft kind boy bear — a chubby, sweet cartoon mascot '
    'who talks to a Persian-speaking child aged three to eight. Speak Persian in a '
    'warm, mid-pitched voice: not high and not deep. Be natural and expressive, '
    'clear enough for a small child to catch every single word, and never shouty, '
    'never sing-song, never baby talk.'
)

# And how this particular line is said. Directed in words, which is the whole
# point of using this engine.
MOOD_DIRECTION = {
    'delighted': 'Sound genuinely delighted and proud of the child, bright and '
                 'smiling, lifting at the end.',
    'kind': 'Sound kind and reassuring. The child has just got something wrong, '
            'so there must be no trace of disappointment — only warmth and a '
            'nudge to try again.',
    'bedtime': 'Almost a whisper. Very slow and very soft, as if the child is '
               'already half asleep.',
    'glyph': 'Say this one letter slowly and very clearly, just once, with a '
             'small friendly lilt.',
    'story': 'Tell this the way a storyteller would: unhurried and measured, '
             'with room after each phrase for the child to picture it.',
    'game': 'Warm, unhurried and companionable, like a friend sitting beside them.',
}


def direction(name):
    return '%s %s' % (PERSONA, MOOD_DIRECTION[mood_for(name)])


def polish(text):
    """Small repairs so the reading is of Persian and not of its typography."""
    t = text.strip()
    t = t.replace('ي', 'ی').replace('ك', 'ک')      # the Arabic forms of two letters
    # A breath where someone starts speaking, so quoted dialogue does not run on.
    for verb in ('گفت:', 'پرسید:', 'گفتن:', 'می‌گفت:'):
        t = t.replace(verb, verb[:-1] + '، ')
    return ' '.join(t.split())


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


def speak(text, name, voice, model, key, tries=5):
    """One line of Persian, returned as raw PCM plus its sample rate."""
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

def to_ogg(pcm, rate, path):
    """16-bit mono PCM straight into Vorbis. No ffmpeg needed; oggenc reads raw."""
    with tempfile.NamedTemporaryFile(suffix='.raw', delete=False) as scratch:
        scratch.write(pcm)
        source = scratch.name
    try:
        subprocess.check_call(
            ['oggenc', '-Q', '-r', '-B', '16', '-C', '1', '-R', str(rate),
             '-q', '3', '-o', path, source])
    finally:
        os.unlink(source)


def write_clip(text, name, voice, model, key, path):
    """Staged, so an interrupted run never leaves a silent file behind."""
    pcm, rate = speak(text, name, voice, model, key)
    staged = path + '.part'
    try:
        to_ogg(pcm, rate, staged)
        if os.path.getsize(staged) < MIN_BYTES:
            raise RuntimeError('encoded to almost nothing (%d bytes)'
                               % os.path.getsize(staged))
        os.replace(staged, path)
    finally:
        if os.path.exists(staged):
            os.unlink(staged)
    return os.path.getsize(path)


# --- the run ------------------------------------------------------------------

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


def do_all(args, key):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    if not os.path.isdir(RAW):
        sys.exit('no res/raw at %s' % RAW)

    todo = [n for n in names
            if args.force or not os.path.exists(os.path.join(RAW, n + '.ogg'))]
    print('%d lines, %d to do, voice %s, model %s'
          % (len(names), len(todo), args.voice, args.model))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %d' % (mood, count))
    print()

    done = failed = 0
    for i, name in enumerate(todo, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            size = write_clip(lines[name], name, args.voice, args.model, key, path)
            done += 1
            print('[%4d/%4d] %-28s %-10s %6.1f KB'
                  % (i, len(todo), name, mood_for(name), size / 1024.0))
        except RuntimeError as problem:
            failed += 1
            print('[%4d/%4d] %-28s FAILED  %s' % (i, len(todo), name, problem))
            if failed >= 5 and done == 0:
                sys.exit('five failures and nothing written — stopping before '
                         'this burns through the quota')
        if args.pause:
            time.sleep(args.pause)
    print('\nwrote %d, failed %d' % (done, failed))
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
    args = parser.parse_args()

    if args.list_voices:
        for name, character in VOICES:
            print('  %-16s %s' % (name, character))
        return
    if not os.access('/usr/bin/oggenc', os.X_OK) and not any(
            os.access(os.path.join(d, 'oggenc'), os.X_OK)
            for d in os.environ.get('PATH', '').split(os.pathsep)):
        sys.exit('oggenc is not here (apt-get install vorbis-tools)')

    key = api_key()
    if args.sample:
        do_sample(args, key)
    else:
        do_all(args, key)


if __name__ == '__main__':
    main()
