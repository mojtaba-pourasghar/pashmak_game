#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks every line with KiaBush/Persian-IPA-to-Speech-F5.

That model takes IPA rather than Persian script, which is the right way round for
this app: the hardest part of Persian is the short vowels the writing leaves out,
and those have already been written in by hand in res/raw/audio_manifest.txt. So
the phonemes are not guessed at run time — they are built once, from the
vowelised text, by tools/build_ipa_lines.py, and they live in
tools/all_game_ipa.json. That file is this script's input and it is already in
the repository, so nothing here has to re-derive it.

What the hand-vowelising buys, in the phonemes themselves:

    سلام قندعسلم    ->  salˈɑm ɢˌanadʔasˈalam      the plain spelling
    سَلام قَندِعَسَلَم  ->  salˈɑm ɢˌandeʔasˈalam      vowelised: the ezafe is there

This cannot run in Claude's sandbox. Every Hugging Face host — huggingface.co,
hf.co, the CDN and the Xet transfer endpoint — is refused by the egress policy
there, so the weights cannot be fetched. It runs anywhere else.

    pip install f5-tts soundfile torch
    python3 tools/gen_voice_f5.py --sample          # one line, then listen
    python3 tools/gen_voice_f5.py                   # all 1,136 into res/raw

A reference clip is what gives F5 its voice — it clones the one you hand it. Pass
the Umbriel greeting already in the repo, or any few seconds of the voice you
want Pashmak to have:

    python3 tools/gen_voice_f5.py --ref tools/voice_samples/welcome__Umbriel.ogg \\
        --ref-text "سَلام قَندِعَسَلَم! مَن پَشمَکَم، دوستِ جَدیدت! آمادِه‌ای باهَم کلی بازی کُنیم؟"

Speed stands in for the moods here. F5 has no pitch or emotion dial, so what it
gets is the pace each mood asks for, and the reference clip carries the warmth.
"""
import argparse
import io
import json
import os
import subprocess
import sys
import tempfile
import wave

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for, tally                  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, 'app/src/main/res/raw')
IPA = os.path.join(ROOT, 'tools/all_game_ipa.json')
TEXT = os.path.join(ROOT, 'tools/all_game_lines.json')
LEDGER = os.path.join(ROOT, 'tools/voice_done_f5.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
MODEL = 'KiaBush/Persian-IPA-to-Speech-F5'
MIN_BYTES = 900

# F5 has no pitch or emotion control, so a mood is a pace and nothing more; the
# warmth comes from the reference clip. 1.0 is the model's own speed.
PACE = {
    'delighted': 1.05,
    'kind': 0.88,
    'bedtime': 0.78,
    'glyph': 0.72,
    'story': 0.90,
    'game': 0.96,
}


def read_ledger():
    if not os.path.exists(LEDGER):
        return {'ref': None, 'done': []}
    return json.load(io.open(LEDGER, encoding='utf-8'))


def write_ledger(ledger):
    staged = LEDGER + '.part'
    with io.open(staged, 'w', encoding='utf-8') as out:
        out.write(json.dumps(ledger, ensure_ascii=False, indent=1, sort_keys=True))
    os.replace(staged, LEDGER)


def load(path, what):
    if not os.path.exists(path):
        sys.exit('%s is missing — run tools/%s' % (path, what))
    return json.load(io.open(path, encoding='utf-8'))


def engine(args):
    """The model, loaded once. Importing here keeps --help working without torch."""
    try:
        from f5_tts.api import F5TTS
    except ImportError:
        sys.exit('pip install f5-tts soundfile torch')
    return F5TTS(model=args.model, device=args.device)


def to_ogg(wav, path):
    subprocess.check_call(['oggenc', '-Q', '-q', '3', '-o', path, wav])


def write_clip(tts, phonemes, mood, args, path):
    """Staged, so an interrupted run never leaves a silent file behind."""
    import soundfile
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as scratch:
        wav = scratch.name
    staged = path + '.part'
    try:
        audio, rate, _ = tts.infer(
            ref_file=args.ref, ref_text=args.ref_text, gen_text=phonemes,
            speed=PACE[mood], remove_silence=True, seed=args.seed)
        soundfile.write(wav, audio, rate)
        with wave.open(wav, 'rb') as w:
            if not w.getnframes():
                raise RuntimeError('the model returned no audio')
        to_ogg(wav, staged)
        if os.path.getsize(staged) < MIN_BYTES:
            raise RuntimeError('encoded to almost nothing')
        os.replace(staged, path)
    finally:
        for leftover in (wav, staged):
            if os.path.exists(leftover):
                os.unlink(leftover)
    return os.path.getsize(path)


def do_sample(args):
    ipa, text = load(IPA, 'build_ipa_lines.py'), load(TEXT, 'build_voice_lines.py')
    name = args.sample
    if name not in ipa:
        sys.exit('no line called %s' % name)
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    mood = mood_for(name)
    print('line     : %s   (mood %s, pace %.2f)' % (name, mood, PACE[mood]))
    print('text     : %s' % text[name])
    print('phonemes : %s' % ipa[name])
    print('reference: %s\n' % (args.ref or '— none given, the voice will be the '
                               'model\'s own'))
    tts = engine(args)
    path = os.path.join(SAMPLES, '%s__f5.ogg' % name)
    size = write_clip(tts, ipa[name], mood, args, path)
    print('wrote %s  (%.1f KB)' % (os.path.relpath(path, ROOT), size / 1024.0))


def do_all(args):
    ipa = load(IPA, 'build_ipa_lines.py')
    names = sorted(ipa)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    ledger = read_ledger()
    if ledger['ref'] and ledger['ref'] != args.ref and not args.force:
        sys.exit('this run was started against %s; pass --force to redo it with a '
                 'different reference clip' % ledger['ref'])
    ledger['ref'] = args.ref
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do' % (len(names), len(names) - len(todo), len(todo)))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %d  (pace %.2f)' % (mood, count, PACE[mood]))
    print()
    tts = engine(args)
    done = failed = 0
    for i, name in enumerate(todo, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            size = write_clip(tts, ipa[name], mood_for(name), args, path)
            done += 1
            ledger['done'].append(name)
            write_ledger(ledger)
            print('[%4d/%4d] %-28s %-10s %6.1f KB'
                  % (i, len(todo), name, mood_for(name), size / 1024.0), flush=True)
        except Exception as problem:                      # noqa: BLE001
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
    parser.add_argument('--model', default=MODEL)
    parser.add_argument('--device', default=None, help='cuda, mps or cpu')
    parser.add_argument('--ref', help='a few seconds of the voice to clone')
    parser.add_argument('--ref-text', default='',
                        help='what the reference clip says; F5 can guess it')
    parser.add_argument('--seed', type=int, default=1234,
                        help='fixed, so every line sounds like the same bear')
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
