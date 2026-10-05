#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks every line with Google Cloud Text-to-Speech — the voice that was approved.

Eight models were tried and every one failed on the same thing, and it was never
the timbre: Persian script leaves the short vowels out, so a synthesiser has to
supply them. Every local model tried here phonemises with espeak, which guesses,
and a wrong guess is exactly what bad pronunciation sounds like.

One voice in the whole search was approved by ear: Umbriel, heard through AI
Studio. It was dropped for a reason that was never about quality — the free tier
of generativelanguage allows ten calls a day per model. The same voice is served
by Cloud Text-to-Speech, where the free tier is a million characters a month and
the whole app is 59,060 of them.

    set GOOGLE_API_KEY=...                     (or GCLOUD_TTS_KEY)
    python3 tools/gen_voice_gcloud.py --check      # what the key can do, and the price
    python3 tools/gen_voice_gcloud.py --probe      # which request shape this model takes
    python3 tools/gen_voice_gcloud.py --voices     # one line in each male voice
    python3 tools/gen_voice_gcloud.py --sample     # one line, plain and shaped
    python3 tools/gen_voice_gcloud.py --fresh      # all 1,136 into res/raw

The key comes from the environment and nowhere else: never a file in the
repository, never printed, never written to the ledger, never committed. Make one
in a Google Cloud project with billing enabled and the Text-to-Speech API turned
on — https://console.cloud.google.com/apis/credentials.

Two kinds of voice live behind this one endpoint and they are directed
differently, which is why --probe exists:

  gemini-2.5-flash-tts   takes a sentence of direction, in words. «Sound
                         genuinely delighted and proud of the child» is the
                         actual instruction, and the feeling comes from the model
                         rather than from bending the pitch of a flat reading.
                         This is what made the approved clips good.
  fa-IR-Chirp3-HD-*      takes a speaking rate and no feeling, so the six moods
                         become six paces and nothing more.

The vowel marks stay on here. For this family of model the hand-vowelised
manifest is an asset rather than poison — it is what the approved clips were made
from, and it is the difference between «شب شده بود» and «شَب شُدِه بُود».
"""
import argparse
import base64
import io
import json
import os
import ssl
import sys
import time
import urllib.error
import urllib.request
import wave

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import PACE, direction, mood_for, tally      # noqa: E402
from voice_text import bare, polish                           # noqa: E402
import voice_output                                           # noqa: E402


def _utf8_console():
    """Let Persian reach a Windows console, which defaults to cp1252."""
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding='utf-8', errors='replace')
        except (AttributeError, ValueError):
            pass


_utf8_console()

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.normpath(os.path.join(ROOT, 'app/src/main/res/raw'))
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
LEDGER = os.path.join(ROOT, 'tools/voice_done_gcloud.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
SCRATCH = os.path.join(ROOT, 'tools/gcloud')

SYNTH = 'https://texttospeech.googleapis.com/v1/text:synthesize'
LIST = 'https://texttospeech.googleapis.com/v1/voices?languageCode=%s'
LANGUAGE = 'fa-IR'
MODEL = 'gemini-2.5-flash-tts'
CHIRP = 'fa-IR-Chirp3-HD-%s'
VOICE = 'Umbriel'
RATE = 24000
MIN_BYTES = 900

# The male voices in the middle of the range, Pashmak being a boy bear. Umbriel
# is first because it is the one that was approved; the rest are here so one line
# can be heard in all of them before 1,136 are committed to any.
MALE = ('Umbriel', 'Algieba', 'Achird', 'Iapetus', 'Charon', 'Fenrir', 'Orus')

# $30 per million characters, with the first million each month free. The Gemini
# model is billed per token instead, so for that one the character count is an
# indication of size and not a price.
PER_MILLION = 30.0


def api_key():
    for name in ('GOOGLE_API_KEY', 'GCLOUD_TTS_KEY'):
        if os.environ.get(name):
            return os.environ[name]
    sys.exit(
        'no key. Set one in this shell and nowhere else:\n'
        '\n  set GOOGLE_API_KEY=...\n'
        '\nIt has to come from a Google Cloud project with billing enabled and\n'
        'the Cloud Text-to-Speech API turned on:\n'
        '  https://console.cloud.google.com/apis/library/texttospeech.googleapis.com\n'
        '  https://console.cloud.google.com/apis/credentials\n'
        '\nAn AI Studio key is a different thing and will not work here.')


def _context():
    """Trust whatever CA bundle this machine was told to use."""
    bundle = os.environ.get('SSL_CERT_FILE') or os.environ.get('REQUESTS_CA_BUNDLE')
    if bundle and os.path.exists(bundle):
        return ssl.create_default_context(cafile=bundle)
    return ssl.create_default_context()


def call(url, key, body=None, tries=7):
    """One request, with rate limits waited out and refusals explained.

    A 429 or a 5xx is worth waiting for; a 400 or a 403 will never get better by
    asking again, and the server says why in the body — which is printed, because
    with an API key the answer is usually one of three things and the message
    names which.
    """
    data = json.dumps(body).encode('utf-8') if body is not None else None
    delay = 2.0
    for attempt in range(tries):
        request = urllib.request.Request(
            url, data=data,
            headers={'Content-Type': 'application/json', 'x-goog-api-key': key})
        try:
            with urllib.request.urlopen(request, timeout=180,
                                        context=_context()) as answer:
                return json.loads(answer.read().decode('utf-8'))
        except urllib.error.HTTPError as problem:
            detail = problem.read().decode('utf-8', 'replace')
            if problem.code in (429, 500, 502, 503, 504) and attempt < tries - 1:
                time.sleep(delay)
                delay = min(delay * 2, 60.0)
                continue
            raise RuntimeError('HTTP %d — %s%s'
                               % (problem.code, _said(detail), _advice(problem.code)))
        except urllib.error.URLError as problem:
            if attempt < tries - 1:
                time.sleep(delay)
                delay = min(delay * 2, 60.0)
                continue
            raise RuntimeError(str(problem))
    raise RuntimeError('gave up after %d attempts' % tries)


def _said(detail):
    """The server's own message, which is more use than the status line."""
    try:
        return json.loads(detail)['error']['message']
    except Exception:                                      # noqa: BLE001
        return ' '.join(detail.split())[:300]


def _advice(code):
    if code == 403:
        return ('\n  A 403 with a key is almost always one of three things: the '
                'Text-to-Speech\n  API is not enabled on the project, billing is '
                'not enabled, or the key is\n  restricted to other APIs.')
    if code == 400:
        return ('\n  A 400 is the request itself — usually a voice name that does '
                'not exist for\n  this language, or a field this model does not '
                'take. --check lists the real\n  names; --probe finds the shape.')
    return ''


def gemini(model):
    return 'gemini' in model.lower()


def voice_name(args):
    """A bare name for a Gemini voice, the full locale name for a Chirp one."""
    name = args.voice
    if gemini(args.model):
        return name.split('-')[-1] if name.startswith(LANGUAGE) else name
    return name if name.startswith(LANGUAGE) else CHIRP % name


def request_for(args, text, name=None, pace=None):
    """The body, built the way the chosen model is directed."""
    said = polish(text if args.harakat == 'keep' else bare(text))
    body = {'input': {'text': said},
            'voice': {'languageCode': args.language, 'name': voice_name(args)},
            'audioConfig': {'audioEncoding': 'LINEAR16', 'sampleRateHertz': RATE}}
    if gemini(args.model):
        body['voice']['modelName'] = args.model
        # Directed in words. This is the whole reason for using this model.
        body['input']['prompt'] = (direction(name) if name
                                   else direction('welcome'))
    else:
        # Chirp has no feeling to give, so the mood is only a pace — and the API
        # counts a larger rate as faster, which is the reciprocal of our table.
        body['audioConfig']['speakingRate'] = round(1.0 / (pace or 1.0), 3)
    return body, said


def samples_of(payload):
    """The audio in the reply, as 16-bit samples and their rate.

    LINEAR16 comes back as a wav — header and all — so it is read as one rather
    than assumed to be bare PCM, and bare PCM is still handled in case that ever
    changes.
    """
    import numpy as np
    raw = base64.b64decode(payload['audioContent'])
    if raw[:4] == b'RIFF':
        with wave.open(io.BytesIO(raw), 'rb') as clip:
            rate = clip.getframerate()
            frames = clip.readframes(clip.getnframes())
        return np.frombuffer(frames, dtype='<i2'), rate
    return np.frombuffer(raw, dtype='<i2'), RATE


def say(args, key, text, name=None, pace=None):
    body, _said = request_for(args, text, name, pace)
    return samples_of(call(SYNTH, key, body))


def read_ledger():
    if not os.path.exists(LEDGER):
        return {'voice': None, 'model': None, 'done': []}
    return json.load(io.open(LEDGER, encoding='utf-8'))


def write_ledger(ledger):
    staged = LEDGER + '.part'
    with io.open(staged, 'w', encoding='utf-8') as out:
        out.write(json.dumps(ledger, ensure_ascii=False, indent=1, sort_keys=True))
    os.replace(staged, LEDGER)


def load_lines():
    if not os.path.exists(LINES):
        sys.exit('run tools/build_voice_lines.py first')
    return json.load(io.open(LINES, encoding='utf-8'))


def spoken_size(args, lines):
    """How many characters will actually be sent, which is what is charged."""
    return sum(len(polish(t if args.harakat == 'keep' else bare(t)))
               for t in lines.values())


def do_check(args):
    key = api_key()
    print('asking what %s voices this key can use\n' % args.language)
    try:
        answer = call(LIST % args.language, key)
    except RuntimeError as problem:
        sys.exit(str(problem))
    voices = answer.get('voices') or []
    if not voices:
        print('  none listed for %s.' % args.language)
    for voice in sorted(voices, key=lambda v: v.get('name', '')):
        print('  %-34s %-7s %5d Hz'
              % (voice.get('name', '?'), voice.get('ssmlGender', '?'),
                 voice.get('naturalSampleRateHertz', 0)))

    lines = load_lines()
    chars = spoken_size(args, lines)
    print('\n%d lines, %d characters to send (%s the vowel marks).'
          % (len(lines), chars, 'with' if args.harakat == 'keep' else 'without'))
    print('A Chirp 3 HD run of that size costs $%.2f at $%.0f per million, and\n'
          'the first million characters each month are free. The Gemini model is\n'
          'billed per token instead, so treat the number above as the size of the\n'
          'job and not as its price.' % (chars / 1e6 * PER_MILLION, PER_MILLION))
    print('\nmoods:')
    for mood, count in sorted(tally(lines).items(), key=lambda kv: -kv[1]):
        print('   %-10s %4d   pace %.2f' % (mood, count, PACE[mood]))


def do_probe(args):
    """Which request shape this model takes, found out rather than assumed.

    The two kinds of voice behind this endpoint are directed differently, and the
    documentation for it is not reachable from here. Two calls settle it, and the
    server's own refusal says what is wrong with the other one.
    """
    key = api_key()
    lines = load_lines()
    text = lines.get('welcome', 'سَلام!')
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    for model, voice in ((MODEL, VOICE), ('chirp3-hd', VOICE)):
        args.model, args.voice = model, voice
        shape = 'prompt' if gemini(model) else 'speakingRate'
        print('%-22s voice %-28s (%s) ... '
              % (model, voice_name(args), shape), end='', flush=True)
        try:
            samples, rate = say(args, key, text, 'welcome', PACE['game'])
        except RuntimeError as problem:
            print('no\n    %s' % str(problem).replace('\n', '\n    '))
            continue
        path = os.path.join(SAMPLES, 'gcloud_probe_%s.ogg'
                            % model.replace('.', '').replace('-', '_'))
        final, size, _pcm = voice_output.write_clip(samples, rate, path)
        print('yes  %6.1f KB  %s' % (size / 1024.0, os.path.relpath(final, ROOT)))
    print('\nWhichever answered is the one to use; listen to both if both did.')


def do_voices(args):
    """One line in each candidate voice, which is how Umbriel was chosen."""
    key = api_key()
    lines = load_lines()
    name = args.sample or 'welcome'
    text = lines.get(name) or name
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    import voice_shape as shaping
    wanted = [v.strip() for v in (args.voices or ','.join(MALE)).split(',')
              if v.strip()]
    print('line : %s\ntext : %s\n' % (name, text))
    for voice in wanted:
        args.voice = voice
        print('  %-14s ' % voice, end='', flush=True)
        try:
            samples, rate = say(args, key, text, name, PACE[mood_for(name)])
        except RuntimeError as problem:
            print('FAILED %s' % ' '.join(str(problem).split())[:90])
            continue
        path = os.path.join(SAMPLES, 'gcloud_%s.ogg' % voice.lower())
        final, size, pcm = voice_output.write_clip(samples, rate, path)
        print('%5.2f s  %3.0f Hz  %6.1f KB  %s'
              % (len(pcm) / float(rate), shaping.fundamental(pcm, rate),
                 size / 1024.0, os.path.relpath(final, ROOT)))
    print('\na grown man is near 110 Hz, a small child near 280.')


def do_one(args, text, name, stem):
    """One clip, written both ways, with what was sent printed above it."""
    key = api_key()
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    import voice_shape as shaping
    body, said = request_for(args, text, name, PACE[mood_for(name)])
    print('voice : %s   model %s' % (voice_name(args), args.model))
    print('mood  : %s   pace %.2f' % (mood_for(name), PACE[mood_for(name)]))
    print('sent  : %s' % said)
    if 'prompt' in body['input']:
        print('as    : %s\n' % body['input']['prompt'])
    samples, rate = say(args, key, text, name, PACE[mood_for(name)])
    for tag, shaped in (('plain', False), ('shaped', True)):
        path = os.path.join(SAMPLES, 'gcloud_%s_%s.ogg' % (stem, tag))
        final, size, pcm = voice_output.write_clip(samples, rate, path, shaped)
        print('  %-7s %5.2f s  %3.0f Hz  %6.1f KB  %s'
              % (tag, len(pcm) / float(rate), shaping.fundamental(pcm, rate),
                 size / 1024.0, os.path.relpath(final, ROOT)))
    print('\na grown man is near 110 Hz, a small child near 280.')


def do_sample(args):
    lines = load_lines()
    name = args.sample
    if name not in lines:
        sys.exit('no line called %s' % name)
    do_one(args, lines[name], name, 'sample')


def do_text(args):
    lines = load_lines()
    text = (args.text if isinstance(args.text, str) and args.text.strip()
            else lines.get('welcome', 'سَلام!'))
    do_one(args, text, 'welcome', 'text')


def do_fresh(args):
    """Start the whole set again, checking everything before deleting anything.

    A clip missing from res/raw is worse than a silence: the Java refers to each
    one by name, so it is a compile error. Nothing is removed until the encoder
    is proved and the API has actually answered with audio.
    """
    key = api_key()
    voice_output.check_encoder(SCRATCH)
    lines = load_lines()
    print('one test call before anything is deleted ... ', end='', flush=True)
    samples, rate = say(args, key, lines.get('welcome', 'سَلام!'), 'welcome',
                        PACE['game'])
    print('%d samples at %d Hz' % (len(samples), rate))
    voice_output.rebuild_lines(ROOT)
    voice_output.clear_spoken(RAW, load_lines())
    if os.path.exists(LEDGER):
        os.unlink(LEDGER)
    do_all(args)


def do_all(args):
    key = api_key()
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    voice_output.check_encoder(SCRATCH)
    ledger = read_ledger()
    here = '%s/%s' % (voice_name(args), args.model)
    if ledger['done'] and ledger.get('voice') and ledger['voice'] != here \
            and not args.force:
        sys.exit('this set was started with %s; pass --force to redo it with %s'
                 % (ledger['voice'], here))
    ledger['voice'], ledger['model'] = here, args.model
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do  —  %s, %s'
          % (len(names), len(names) - len(todo), len(todo), voice_name(args),
             args.model))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %4d   pace %.2f' % (mood, count, PACE[mood]))
    print()
    done = failed = 0
    for i, name in enumerate(todo, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            samples, rate = say(args, key, lines[name], name,
                                PACE[mood_for(name)])
            staged = path + '.part'
            final, size, _pcm = voice_output.write_clip(
                samples, rate, staged, shaped=not args.plain)
            if size < MIN_BYTES:
                raise RuntimeError('encoded to almost nothing')
            os.replace(final, path)
            done += 1
            ledger['done'].append(name)
            write_ledger(ledger)
            if i % 25 == 0 or i == len(todo):
                print('[%4d/%4d] %-28s %-10s %6.1f KB'
                      % (i, len(todo), name, mood_for(name), size / 1024.0),
                      flush=True)
        except Exception as problem:                       # noqa: BLE001
            failed += 1
            print('[%4d/%4d] %-28s FAILED %s'
                  % (i, len(todo), name, ' '.join(str(problem).split())[:110]),
                  flush=True)
            if failed >= 5 and done == 0:
                sys.exit('five failures and nothing written — stopping')
    print('\nwrote %d, failed %d, %d of %d in all'
          % (done, failed, len(ledger['done']), len(names)))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--voice', default=VOICE,
                        help='a voice name: bare for a Gemini model (Umbriel), '
                             'or the full fa-IR-Chirp3-HD-Umbriel')
    parser.add_argument('--model', default=MODEL,
                        help='gemini-2.5-flash-tts, or chirp3-hd for the other kind')
    parser.add_argument('--language', default=LANGUAGE)
    parser.add_argument('--harakat', choices=('keep', 'strip'), default='keep',
                        help='the vowel marks. Kept by default: this family of '
                             'model reads them, and they are what the approved '
                             'clips were made from')
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--plain', action='store_true',
                        help='the model\'s own sound, unshaped')
    parser.add_argument('--check', action='store_true',
                        help='what the key can do, and the size of the job')
    parser.add_argument('--probe', action='store_true',
                        help='which request shape each model takes')
    parser.add_argument('--voices', nargs='?', const='',
                        help='one line in each of these voices (default: the '
                             'male shortlist)')
    parser.add_argument('--sample', nargs='?', const='welcome')
    parser.add_argument('--text', nargs='?', const='')
    parser.add_argument('--fresh', action='store_true',
                        help='rebuild the lines from the manifest, delete the '
                             'clips of the last voice, and speak them all again')
    args = parser.parse_args()
    if args.check:
        do_check(args)
    elif args.probe:
        do_probe(args)
    elif args.voices is not None:
        do_voices(args)
    elif args.text is not None:
        do_text(args)
    elif args.sample:
        do_sample(args)
    elif args.fresh:
        do_fresh(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
