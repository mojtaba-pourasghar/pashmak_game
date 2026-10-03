#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks every line with a Persian Chatterbox model, in Umbriel's voice.

Why this one is the best fit so far. Two things are wanted at once: the timbre of
Umbriel, and a different feeling per line. Chatterbox gives both. It clones the
voice from a reference clip, so Umbriel is reachable at all — no pretrained
Persian model can be Umbriel, only a cloning model can — and it has an
`exaggeration` control, which is a real knob on how strongly a line is felt.
Everything before this was worse on one axis or the other: Piper has one fixed
voice, F5 clones but its only lever is speed, and edge-tts and Gemini cannot
clone at all.

It also takes plain Persian, so tools/all_game_ipa.json is not needed here — the
vowelised text from the manifest goes straight in, shadda and all, because a
neural model reads the mark instead of announcing its name.

    python3 tools/gen_voice_chatterbox.py --sample      # one line, five feelings
    python3 tools/gen_voice_chatterbox.py               # all of them into res/raw

The reference is tools/reference/umbriel_welcome.wav: 7.3 seconds of the real
Umbriel, kept outside res/raw so no later run overwrites it.

Two things about this file are unverified, and it is better to say so than to let
them look tested. Hugging Face is refused by the egress policy where this was
written, so the model could not be loaded even once: the loader tries several
shapes and, failing all of them, prints what the repository actually holds so the
right one can be named. And the numbers below are a considered starting point for
the six moods, not a measured result — --sample renders one line at all of them
so the choice is made by ear.
"""
import argparse
import io
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for, tally                   # noqa: E402


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
RAW = os.path.normpath(os.path.join(ROOT, 'app/src/main/res/raw'))
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
LEDGER = os.path.join(ROOT, 'tools/voice_done_chatterbox.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
REFERENCE = os.path.join(ROOT, 'tools/reference/umbriel_welcome.wav')
MODEL = 'Thomcles/Chatterbox-TTS-Persian-Farsi'
MIN_BYTES = 900

# exaggeration is how strongly the line is felt; cfg_weight paces it, and lower
# is slower and more deliberate. A starting point, to be settled by ear.
MOODS = {
    #              exaggeration  cfg_weight
    'delighted': (0.70, 0.45),   # genuinely pleased, lifting at the end
    'kind':      (0.40, 0.35),   # warm, and with no trace of disappointment
    'bedtime':   (0.25, 0.25),   # almost a whisper, the slowest thing here
    'glyph':     (0.35, 0.28),   # one letter, said once, with room around it
    'story':     (0.45, 0.35),   # measured, with time to picture it
    'game':      (0.55, 0.45),   # warm and companionable
}


def preflight(args):
    """Every environment problem at once, before anything slow starts."""
    problems = []
    if sys.version_info[:2] >= (3, 13):
        problems.append(
            'chatterbox-tts pins torch==2.6.0 for Python below 3.13, and this is\n'
            '    %d.%d. Use a 3.12 venv:\n'
            '      py -3.12 -m venv .venv\n'
            '      .venv\\Scripts\\activate\n'
            '      python -m pip install chatterbox-tts' % sys.version_info[:2])
    for module, why in (('torch', 'the model runs on it'),
                        ('torchaudio', 'the audio is written with it'),
                        ('chatterbox', 'the model itself')):
        try:
            __import__(module)
        except ImportError:
            problems.append('%s is not installed (%s):\n      %s -m pip install %s'
                            % (module, why, sys.executable,
                               'chatterbox-tts' if module == 'chatterbox' else module))
        except OSError as broken:
            # A broken torch raises OSError, not ImportError — on Windows it is
            # WinError 1114 out of c10.dll — so catching ImportError alone lets
            # the ugliest failure through as a stack trace.
            problems.append('%s is installed but will not load (%s):\n    %s'
                            % (module, why, ' '.join(str(broken).split())[:200]))
    if not shutil.which('oggenc'):
        if args.sample:
            print('oggenc is not on PATH, so this sample is written as .wav.\n',
                  file=sys.stderr)
        else:
            problems.append('oggenc is not on PATH. The whole set is over an hour of\n'
                            '    speech, which does not ship as wav. Install vorbis-tools.')
    if not os.path.exists(args.ref):
        problems.append('no reference clip at %s' % args.ref)
    if problems:
        print('\nthis machine is not ready yet:\n', file=sys.stderr)
        for i, problem in enumerate(problems, 1):
            print('  %d. %s\n' % (i, problem), file=sys.stderr)
        sys.exit(1)


# What a Chatterbox checkpoint is made of. Everything else in a fine-tune's
# repository — optimizer states, intermediate steps, training logs — is weight
# without value here.
CHECKPOINT = ('.safetensors', '.pt', '.json', '.txt')
TRAINING = ('optimizer', 'scheduler', 'global_step', 'checkpoint-', 'runs/',
            'training_args', 'rng_state', 'trainer_state', '.ckpt')


def repo_files(model):
    """Every file in the repository with its size, from the API alone.

    This costs nothing to run and is what should happen before any download.
    """
    from huggingface_hub import HfApi
    info = HfApi().model_info(model, files_metadata=True)
    return sorted(((f.rfilename, f.size or 0) for f in info.siblings),
                  key=lambda row: -row[1])


def weigh(args):
    """The files worth fetching: a checkpoint, without its training."""
    if args.files:
        wanted = set(n.strip() for n in args.files.split(',') if n.strip())
        return [(n, s) for n, s in repo_files(args.model) if n in wanted]
    plan = []
    for name, size in repo_files(args.model):
        lower = name.lower()
        if any(mark in lower for mark in TRAINING):
            continue
        if lower.endswith(CHECKPOINT):
            plan.append((name, size))
    return plan


def do_list(args):
    rows = repo_files(args.model)
    print('%s holds %d file(s), %.2f GB in all:\n'
          % (args.model, len(rows), sum(s for _, s in rows) / 1e9))
    for name, size in rows:
        print('  %9.3f GB  %s' % (size / 1e9, name))
    plan = weigh(args)
    print('\nwhat this tool would fetch: %d file(s), %.2f GB'
          % (len(plan), sum(s for _, s in plan) / 1e9))


def engine(args):
    """The model, loaded once.

    A community fine-tune is not one of the package's built-ins, and the loaders
    have moved between releases, so several shapes are tried rather than betting
    on one. When all of them fail the repository's own file list is printed,
    because that is what says which loader it wants.
    """
    preflight(args)
    import torch
    device = args.device or ('cuda' if torch.cuda.is_available() else 'cpu')
    print('device: %s' % device)

    tried = []
    try:
        from chatterbox.tts import ChatterboxTTS
    except ImportError as missing:
        sys.exit('chatterbox.tts will not import: %s' % missing)

    for how, call in (
            ('from_pretrained(repo)', lambda: ChatterboxTTS.from_pretrained(
                args.model, device=device)),
            ('from_pretrained(device only)', lambda: ChatterboxTTS.from_pretrained(
                device=device)),
    ):
        try:
            return ChatterboxVoice(call(), device)
        except Exception as problem:                       # noqa: BLE001
            tried.append('%s -> %s' % (how, ' '.join(str(problem).split())[:100]))

    # The usual route for a fine-tune: fetch the repository, then load the folder.
    #
    # With a filter. An unfiltered snapshot_download takes everything the
    # repository holds, and a fine-tune's repository holds its training as well
    # as its result — which is how this pulled 20 GB before anyone could stop
    # it. Only the files a checkpoint is made of are fetched, the plan is printed
    # first, and anything over --max-gb stops rather than starts.
    try:
        from huggingface_hub import snapshot_download
    except ImportError:
        sys.exit('pip install huggingface_hub\n  ' + '\n  '.join(tried))
    plan = weigh(args)
    if not plan:
        sys.exit('nothing in %s looks like a checkpoint. run --list and send me '
                 'what it prints.' % args.model)
    total = sum(size for _, size in plan) / 1e9
    print('\nfetching %d file(s), %.2f GB:' % (len(plan), total))
    for name, size in plan:
        print('   %8.2f GB  %s' % (size / 1e9, name))
    if total > args.max_gb:
        sys.exit('that is %.2f GB, over the --max-gb limit of %.2f. run --list to '
                 'see everything, then pass --files with the ones you want.'
                 % (total, args.max_gb))
    try:
        folder = snapshot_download(args.model,
                                   allow_patterns=[name for name, _ in plan])
    except Exception as problem:                           # noqa: BLE001
        sys.exit('cannot fetch %s: %s\n  %s'
                 % (args.model, problem, '\n  '.join(tried)))
    try:
        return ChatterboxVoice(ChatterboxTTS.from_local(folder, device), device)
    except Exception as problem:                           # noqa: BLE001
        tried.append('from_local(%s) -> %s' % (folder, ' '.join(str(problem).split())[:100]))

    print('none of the loaders worked:', file=sys.stderr)
    for line in tried:
        print('  ' + line, file=sys.stderr)
    try:
        print('\nthe repository holds:', file=sys.stderr)
        for name in list_repo_files(args.model):
            print('  ' + name, file=sys.stderr)
    except Exception:                                      # noqa: BLE001
        pass
    sys.exit('send me that list and I will name the right loader')


class ChatterboxVoice(object):
    """Thin wrapper, so generate()'s argument names live in exactly one place."""

    def __init__(self, model, device):
        self.model = model
        self.device = device
        self.rate = getattr(model, 'sr', 24000)

    def say(self, text, reference, exaggeration, cfg_weight):
        return self.model.generate(text, audio_prompt_path=reference,
                                   exaggeration=exaggeration, cfg_weight=cfg_weight)


def to_ogg(wav, path):
    """Vorbis when oggenc is here; otherwise the wav itself.

    Android resolves R.raw.<name> by name and ignores the extension, so a .wav
    plays as happily — it is only far larger, which is why the full run insists
    on the encoder and a single sample does not.
    """
    import subprocess
    if shutil.which('oggenc'):
        subprocess.check_call(['oggenc', '-Q', '-q', '3', '-o', path, wav])
        return path
    fallback = os.path.splitext(path)[0] + '.wav'
    shutil.copyfile(wav, fallback)
    return fallback


def write_clip(voice, text, exaggeration, cfg_weight, args, path):
    """Staged, so an interrupted run never leaves a silent file behind."""
    import tempfile
    import torchaudio
    audio = voice.say(text, args.ref, exaggeration, cfg_weight)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as scratch:
        wav = scratch.name
    staged = path + '.part'
    try:
        torchaudio.save(wav, audio.cpu() if hasattr(audio, 'cpu') else audio,
                        voice.rate)
        written = to_ogg(wav, staged)
        if os.path.getsize(written) < MIN_BYTES:
            raise RuntimeError('encoded to almost nothing')
        final = path if written == staged else os.path.splitext(path)[0] + '.wav'
        os.replace(written, final)
    finally:
        for leftover in (wav, staged, os.path.splitext(staged)[0] + '.wav'):
            if os.path.exists(leftover):
                os.unlink(leftover)
    return final, os.path.getsize(final)


def read_ledger():
    if not os.path.exists(LEDGER):
        return {'ref': None, 'done': []}
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
    voice = engine(args)
    print('\nline      : %s   (its own mood is %s)' % (args.sample, mood_for(args.sample)))
    print('text      : %s' % lines[args.sample])
    print('reference : %s\n' % os.path.relpath(args.ref, ROOT))
    print('  %-11s %-6s %-6s' % ('', 'exagg', 'cfg'))
    for mood, (exaggeration, cfg_weight) in sorted(MOODS.items()):
        path = os.path.join(SAMPLES, 'chatterbox_%s.ogg' % mood)
        try:
            written, size = write_clip(voice, lines[args.sample], exaggeration,
                                       cfg_weight, args, path)
            print('  %-11s %-6.2f %-6.2f %6.1f KB   %s'
                  % (mood, exaggeration, cfg_weight, size / 1024.0,
                     os.path.relpath(written, ROOT)))
        except Exception as problem:                       # noqa: BLE001
            print('  %-11s FAILED %s'
                  % (mood, ' '.join(str(problem).split())[:100]))
    print('\nsame words, six feelings. tell me whether they read right.')


def do_all(args):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    ledger = read_ledger()
    if ledger['ref'] and ledger['ref'] != args.ref and not args.force:
        sys.exit('this run was started against %s; pass --force to redo it with a '
                 'different reference' % ledger['ref'])
    ledger['ref'] = args.ref
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do'
          % (len(names), len(names) - len(todo), len(todo)))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %4d   exagg %.2f  cfg %.2f'
              % (mood, count, MOODS[mood][0], MOODS[mood][1]))
    print()
    voice = engine(args)
    done = failed = 0
    for i, name in enumerate(todo, 1):
        exaggeration, cfg_weight = MOODS[mood_for(name)]
        path = os.path.join(RAW, name + '.ogg')
        try:
            _written, size = write_clip(voice, lines[name], exaggeration,
                                        cfg_weight, args, path)
            done += 1
            ledger['done'].append(name)
            write_ledger(ledger)
            if i % 20 == 0 or i == len(todo):
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
    parser.add_argument('--model', default=MODEL)
    parser.add_argument('--device', help='cuda or cpu; detected when left out')
    parser.add_argument('--ref', default=REFERENCE,
                        help='the voice to clone; Umbriel by default')
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--sample', nargs='?', const='welcome')
    parser.add_argument('--list', action='store_true',
                        help='print the repository\'s files and sizes, download '
                             'nothing — do this first')
    parser.add_argument('--files', help='comma-separated, exactly which to fetch')
    parser.add_argument('--max-gb', type=float, default=4.0,
                        help='stop rather than start above this (default 4)')
    args = parser.parse_args()
    if args.list:
        do_list(args)
        return
    if args.sample:
        do_sample(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
