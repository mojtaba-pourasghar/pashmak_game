#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks every line with Piper and the Persian fa_IR-mana voice.

This is the one that works everywhere. No key, no quota, no service: Piper is a
small ONNX model that runs on the processor, and the model itself is in this
repository, in tools/textToSpeech - Persian.zip, so there is nothing to fetch.
That is why it beat the alternatives — Gemini allows ten calls a day on the free
tier, edge-tts needs a WebSocket the sandbox proxy will not carry, and F5 wants
torch and a Hugging Face download.

Two things are done to the model's own output, and both were measured rather
than guessed.

The voice is female. fa_IR-mana reads at about 235 Hz, which is a woman, and
Pashmak is a small boy bear. So each clip is stretched, which lowers the pitch
and the formants together — a short vocal tract is what makes a small creature
sound small — and Piper's length_scale is divided by the same amount so the line
still lands at the length it should. At the drop used here it comes out near
180 Hz, against roughly 110 for a grown man and 280 for a small child.

And the model phonemises with espeak, which does not know what a shadda is:
handed «غُصِّه» it says the *name* of the mark, so the line becomes
"ghosse-tashdid". 76 of the 1,136 lines carry one. They go through the same
repair the IPA builder uses — the consonant written twice, which is what the mark
means.

After that comes the warmth: the shelf under 260 Hz, the roll-off above 7 kHz,
soft compression and one level for the whole app, all from tools/voice_bear.py.

    python3 tools/gen_voice_piper.py --sample      # one line, five pitches
    python3 tools/gen_voice_piper.py               # all of them into res/raw
    python3 tools/gen_voice_piper.py --only tale_
"""
import argparse
import io
import json
import os
import sys
import wave
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for, tally                   # noqa: E402
from build_ipa_lines import degeminate                    # noqa: E402
import voice_bear as shaping                              # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.normpath(os.path.join(ROOT, 'app/src/main/res/raw'))
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
LEDGER = os.path.join(ROOT, 'tools/voice_done_piper.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
BUNDLE = os.path.join(ROOT, 'tools/textToSpeech - Persian.zip')
MODELS = os.path.join(ROOT, 'tools/models')
MODEL = 'fa_IR-mana-medium.onnx'
MIN_BYTES = 900

# drop is how far the pitch comes down; pace is how long the line ends up,
# against the model's own speed. length_scale is pace/drop, so the two are
# independent: drop chooses the voice, pace chooses the delivery.
DROP = 1.28
MOODS = {
    #              pace  drop
    'delighted': (1.02, 1.24),   # quick and a shade brighter
    'kind':      (1.22, 1.30),   # slower and softer; never disappointed
    'bedtime':   (1.45, 1.32),   # the slowest, deepest thing in the app
    'glyph':     (1.50, 1.28),   # one letter, said once, with room around it
    'story':     (1.24, 1.28),   # measured, with time to picture it
    'game':      (1.12, 1.28),   # warm and unhurried
}


def model_path():
    """The model, unpacked out of the zip in the repo the first time it is asked for."""
    found = os.path.join(MODELS, MODEL)
    if os.path.exists(found):
        return found
    if not os.path.exists(BUNDLE):
        sys.exit('neither %s nor %s is here' % (found, BUNDLE))
    if not os.path.isdir(MODELS):
        os.makedirs(MODELS)
    with zipfile.ZipFile(BUNDLE) as bundle:
        for entry in bundle.namelist():
            if entry.endswith(MODEL) or entry.endswith(MODEL + '.json'):
                with bundle.open(entry) as source, \
                        io.open(os.path.join(MODELS, os.path.basename(entry)),
                                'wb') as target:
                    target.write(source.read())
    if not os.path.exists(found):
        sys.exit('%s holds no %s' % (BUNDLE, MODEL))
    print('unpacked %s' % os.path.relpath(found, ROOT))
    return found


def engine():
    try:
        from piper import PiperVoice
    except ImportError:
        sys.exit('%s -m pip install piper-tts' % sys.executable)
    return PiperVoice.load(model_path())


def render(voice, text, pace, drop, scratch):
    """From Persian to finished 16-bit samples."""
    from piper import SynthesisConfig
    with wave.open(scratch, 'wb') as out:
        voice.synthesize_wav(degeminate(' '.join(text.split())), out,
                             syn_config=SynthesisConfig(length_scale=pace / drop))
    samples, rate = shaping.read_wav(scratch)
    if not len(samples):
        raise RuntimeError('the model returned no audio')
    worked = shaping.warm(samples, rate)
    worked = shaping.stretch(worked, drop)
    worked = shaping.compress(worked)
    return shaping.finish(worked, rate), rate


def write_clip(voice, text, pace, drop, path, scratch, measure=False):
    """Staged, so an interrupted run never leaves a silent file behind."""
    samples, rate = render(voice, text, pace, drop, scratch)
    staged = path + '.part'
    try:
        shaping.to_ogg(samples, rate, staged)
        if os.path.getsize(staged) < MIN_BYTES:
            raise RuntimeError('encoded to almost nothing')
        os.replace(staged, path)
    finally:
        if os.path.exists(staged):
            os.unlink(staged)
    return (os.path.getsize(path), len(samples) / float(rate),
            shaping.fundamental(samples, rate) if measure else 0.0)


def read_ledger():
    if not os.path.exists(LEDGER):
        return {'voice': MODEL, 'drop': DROP, 'done': []}
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


def do_sample(args):
    lines = load_lines()
    if args.sample not in lines:
        sys.exit('no line called %s' % args.sample)
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    mood = mood_for(args.sample)
    pace, _ = MOODS[mood]
    voice = engine()
    scratch = os.path.join(SAMPLES, '.scratch.wav')
    print('line : %s   (mood %s, pace %.2f)' % (args.sample, mood, pace))
    print('text : %s\n' % lines[args.sample])
    print('  %-9s %-6s %8s %8s' % ('', 'drop', 'length', 'F0'))
    for tag, drop in (('native', 1.00), ('soft', 1.10), ('boy', 1.18),
                      ('deeper', 1.28), ('deepest', 1.38)):
        path = os.path.join(SAMPLES, 'piper_%s.ogg' % tag)
        size, length, pitch = write_clip(voice, lines[args.sample], pace, drop,
                                         path, scratch, measure=True)
        print('  %-9s %-6.2f %7.2fs %6.0f Hz   %d KB'
              % (tag, drop, length, pitch, size // 1024))
    if os.path.exists(scratch):
        os.unlink(scratch)
    print('\na grown man is near 110 Hz and a small child near 280.')


def do_all(args):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    ledger = read_ledger()
    if abs(ledger.get('drop', DROP) - args.drop) > 1e-9 and not args.force:
        sys.exit('this run was started at drop %.2f; pass --force to redo it at '
                 '%.2f' % (ledger['drop'], args.drop))
    ledger['drop'], ledger['voice'] = args.drop, MODEL
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do, drop %.2f'
          % (len(names), len(names) - len(todo), len(todo), args.drop))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %4d   pace %.2f' % (mood, count, MOODS[mood][0]))
    print()

    voice = engine()
    scratch = os.path.join(RAW, '.scratch.wav')
    done = failed = 0
    seconds = 0.0
    try:
        for i, name in enumerate(todo, 1):
            pace, drop = MOODS[mood_for(name)]
            # --drop moves every mood together, keeping their relative offsets.
            drop *= args.drop / DROP
            path = os.path.join(RAW, name + '.ogg')
            try:
                size, length, _ = write_clip(voice, lines[name], pace, drop,
                                             path, scratch)
                done += 1
                seconds += length
                ledger['done'].append(name)
                write_ledger(ledger)
                if i % 25 == 0 or i == len(todo):
                    print('[%4d/%4d] %-28s %5.2fs  %.1f min of speech so far'
                          % (i, len(todo), name, length, seconds / 60.0),
                          flush=True)
            except Exception as problem:                   # noqa: BLE001
                failed += 1
                print('[%4d/%4d] %-28s FAILED %s'
                      % (i, len(todo), name,
                         ' '.join(str(problem).split())[:110]), flush=True)
                if failed >= 5 and done == 0:
                    sys.exit('five failures and nothing written — stopping')
    finally:
        if os.path.exists(scratch):
            os.unlink(scratch)
    print('\nwrote %d, failed %d, %d of %d in all, %.1f minutes of speech'
          % (done, failed, len(ledger['done']), len(names), seconds / 60.0))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--drop', type=float, default=DROP,
                        help='how far the pitch comes down; %.2f is "deeper"' % DROP)
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--sample', nargs='?', const='welcome')
    args = parser.parse_args()
    if args.sample:
        do_sample(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
