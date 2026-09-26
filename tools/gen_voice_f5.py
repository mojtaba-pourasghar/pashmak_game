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
import shutil
import subprocess
import sys
import tempfile
import wave

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for, tally                  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.normpath(os.path.join(ROOT, 'app/src/main/res/raw'))
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


def preflight(args):
    """Every environment problem at once, in words, before anything slow starts.

    Each of these has already cost a round trip on its own, and each arrived as a
    traceback rather than as a thing to do. Note that a broken torch raises
    OSError, not ImportError — on Windows it is WinError 1114 out of c10.dll —
    so catching ImportError alone lets the ugliest of them through.
    """
    problems = []

    if sys.version_info[:2] >= (3, 14):
        problems.append(
            'Python %d.%d is newer than PyTorch builds for. That is what a DLL\n'
            '    initialization failure in c10.dll means. Install Python 3.12 and\n'
            '    make a venv with it:\n'
            '      py -3.12 -m venv .venv\n'
            '      .venv\\Scripts\\activate      (Windows)\n'
            '      python -m pip install f5-tts soundfile torch'
            % sys.version_info[:2])

    for module, why in (('torch', 'the model runs on it'),
                        ('soundfile', 'the audio is written with it'),
                        ('f5_tts', 'the model itself')):
        try:
            __import__(module)
        except ImportError:
            problems.append('%s is not installed (%s):\n      %s -m pip install %s'
                            % (module, why, sys.executable,
                               'f5-tts' if module == 'f5_tts' else module))
        except OSError as broken:
            problems.append('%s is installed but will not load (%s):\n    %s'
                            % (module, why, ' '.join(str(broken).split())[:200]))

    if not shutil.which('oggenc'):
        if args.sample:
            print('oggenc is not on PATH, so this sample is written as .wav. That is\n'
                  'fine to listen to; the full run needs it, because 104 minutes of\n'
                  'wav is about 300 MB and will not ship in an apk. vorbis-tools.\n',
                  file=sys.stderr)
        else:
            problems.append(
                'oggenc is not on PATH. The whole set is 104 minutes of speech, which\n'
                '    is about 300 MB as wav and 25 MB as ogg, so this is not optional\n'
                '    for the full run. Install vorbis-tools. (--sample works without\n'
                '    it and writes .wav instead.)')

    if not os.path.exists(args.ref):
        problems.append('no reference clip at %s' % args.ref)

    if problems:
        print('\nthis machine is not ready yet:\n', file=sys.stderr)
        for i, problem in enumerate(problems, 1):
            print('  %d. %s\n' % (i, problem), file=sys.stderr)
        sys.exit(1)


def engine(args):
    """The model, loaded once. Imported here so --help works without torch.

    f5-tts has changed how a model is named more than once, and a community model
    is not one of its built-ins, so three ways are tried in turn rather than
    betting on one: the repo id straight in, the same as the older `model_type`
    keyword, and — what usually does it — fetching the checkpoint and the vocab
    out of the repo and passing those as files.
    """
    preflight(args)
    from f5_tts.api import F5TTS

    tried = []
    for attempt in ('model', 'model_type'):
        try:
            return F5TTS(**{attempt: args.model}, device=args.device)
        except Exception as problem:                       # noqa: BLE001
            tried.append('%s= -> %s' % (attempt, ' '.join(str(problem).split())[:90]))

    try:
        from huggingface_hub import list_repo_files, hf_hub_download
    except ImportError:
        sys.exit('pip install huggingface_hub\n  ' + '\n  '.join(tried))
    try:
        files = list_repo_files(args.model)
    except Exception as problem:                           # noqa: BLE001
        sys.exit('cannot reach %s: %s\n  %s'
                 % (args.model, problem, '\n  '.join(tried)))

    ckpt = args.ckpt or _pick(files, ('.safetensors', '.pt'))
    vocab = args.vocab or _pick(files, ('vocab.txt',))
    if not ckpt:
        sys.exit('no checkpoint in %s — its files are:\n  %s'
                 % (args.model, '\n  '.join(files)))
    print('loading %s (%s)' % (args.model, ckpt))
    kwargs = dict(ckpt_file=hf_hub_download(args.model, ckpt), device=args.device)
    if vocab:
        kwargs['vocab_file'] = hf_hub_download(args.model, vocab)
    return F5TTS(**kwargs)


def _pick(files, endings):
    """The likeliest file, preferring a plain name over a checkpoint-N one."""
    hits = [f for f in files if f.endswith(endings)]
    return sorted(hits, key=lambda f: (len(f), f))[0] if hits else None


def to_ogg(wav, path):
    """Vorbis when oggenc is here; otherwise the wav itself, renamed.

    Android resolves R.raw.<name> by name and does not care about the extension,
    so a .wav plays as happily as a .ogg — it is only far larger, which is why
    the full run insists on the encoder and a single sample does not.
    """
    if shutil.which('oggenc'):
        subprocess.check_call(['oggenc', '-Q', '-q', '3', '-o', path, wav])
        return path
    fallback = os.path.splitext(path)[0] + '.wav'
    shutil.copyfile(wav, fallback)
    return fallback


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
        written = to_ogg(wav, staged)
        if os.path.getsize(written) < MIN_BYTES:
            raise RuntimeError('encoded to almost nothing')
        final = path if written == staged else os.path.splitext(path)[0] + '.wav'
        os.replace(written, final)
    finally:
        for leftover in (wav, staged, staged + '.wav',
                         os.path.splitext(staged)[0] + '.wav'):
            if os.path.exists(leftover):
                os.unlink(leftover)
    return final, os.path.getsize(final)


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
    written, size = write_clip(tts, ipa[name], mood, args, path)
    print('wrote %s  (%.1f KB)' % (os.path.relpath(written, ROOT), size / 1024.0))


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
            _written, size = write_clip(tts, ipa[name], mood_for(name), args, path)
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
    # The Umbriel greeting is committed as res/raw/welcome.ogg, so the voice you
    # already chose is the default reference and --sample needs no arguments.
    parser.add_argument('--ref', default=os.path.join(RAW, 'welcome.ogg'),
                        help='a few seconds of the voice to clone')
    parser.add_argument('--ref-text', default=None,
                        help='what the reference clip says; taken from the '
                             'manifest when the reference is welcome.ogg')
    parser.add_argument('--ckpt', help='checkpoint inside the model repo')
    parser.add_argument('--vocab', help='vocab file inside the model repo')
    parser.add_argument('--seed', type=int, default=1234,
                        help='fixed, so every line sounds like the same bear')
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--sample', nargs='?', const='welcome')
    args = parser.parse_args()
    if args.ref_text is None:
        same = os.path.abspath(args.ref) == os.path.abspath(
            os.path.join(RAW, 'welcome.ogg'))
        args.ref_text = load(TEXT, 'build_voice_lines.py')['welcome'] if same else ''
    if not os.path.exists(args.ref):
        sys.exit('no reference clip at %s' % args.ref)
    if args.sample:
        do_sample(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
