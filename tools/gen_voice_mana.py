#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks the app's lines with Persian-Tacotron2-on-ManaTTS, cloning a reference voice.

This is the one whose natural reference clip sounded right. It is also the most
assembled of them all: the author's notebook is written for Colab and pulls four
things from three places, none of which is a single pip install.

  the inference code   github.com/MahtaFetrat/Persian-MultiSpeaker-Tacotron2
  the speaker encoder  saved_models/default/encoder.pt, inside that repo
  the synthesiser      synthesizer.pt, 371 MB, from the Hugging Face repo
  the vocoder          vctk_hifigan.v1, fetched by parallel-wavegan

Tacotron2 makes a spectrogram, not sound, which is why the vocoder is not
optional and why the model repo on its own cannot speak a word. --setup puts all
four in the places inference.py expects.

    python3 tools/gen_voice_mana.py --setup
    python3 tools/gen_voice_mana.py --sample --ref <a clean wav of the voice>
    python3 tools/gen_voice_mana.py --ref <same wav>

Use a separate virtual environment for this. The author pins scipy 1.12, and the
environment that runs coqui-tts here has 1.18; installing one over the other
breaks whichever was there first.

    py -3.12 -m venv .venv-mana
    .venv-mana\\Scripts\\activate
    python -m pip install "scipy==1.12.0" parallel-wavegan torch soundfile huggingface_hub

Being multi-speaker is the point: it clones from --ref, so the voice can be the
natural recording that sounded best, or any clean recording of whoever should be
Pashmak. One caution worth saying once: a real person's voice is theirs. Check what
the recording's licence allows before shipping a clone of it.
"""
import argparse
import io
import json
import os
import shutil
import subprocess
import sys


def _utf8_console():
    """Let Persian reach a Windows console, which defaults to cp1252."""
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding='utf-8', errors='replace')
        except (AttributeError, ValueError):
            pass


_utf8_console()

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'tools'))
from voice_moods import mood_for, tally                    # noqa: E402

RAW = os.path.normpath(os.path.join(ROOT, 'app/src/main/res/raw'))
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
LEDGER = os.path.join(ROOT, 'tools/voice_done_mana.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
WORK = os.path.join(ROOT, 'tools/mana')
CODE = os.path.join(WORK, 'Persian-MultiSpeaker-Tacotron2')
GITHUB = 'https://github.com/MahtaFetrat/Persian-MultiSpeaker-Tacotron2.git'
MODEL = 'MahtaFetrat/Persian-Tacotron2-on-ManaTTS'
VOCODER = 'vctk_hifigan.v1'
MIN_BYTES = 900


# Breaks with known answers, named so they are not mistaken for this tool's fault.
KNOWN = (
    (('parallel_wavegan', 'No module named'),
     'parallel-wavegan is not installed. It is an old package whose setup.py\n'
     '    imports pip, which modern build isolation does not provide, so it needs\n'
     '    building inside this environment instead:\n'
     '      %(python)s -m pip install setuptools wheel\n'
     '      %(python)s -m pip install --no-build-isolation parallel-wavegan'),
    (('scipy',),
     'scipy has to be 1.12 for this model\'s code:\n'
     '      %(python)s -m pip install "scipy==1.12.0"'),
)


def advice(detail):
    for marks, words in KNOWN:
        if all(mark in detail for mark in marks):
            return '\n\n    ' + words % {'python': sys.executable}
    return ''


def need(module, why):
    """Import something, and when it will not, say what to do about it."""
    try:
        return __import__(module)
    except (ImportError, OSError) as problem:
        text = '%s: %s' % (type(problem).__name__,
                           ' '.join(str(problem).split())[:200])
        sys.exit('%s is needed (%s).\n  %s%s'
                 % (module, why, text, advice('%s %s' % (module, text))))


def final_models():
    return os.path.join(CODE, 'saved_models', 'final_models')


def do_setup(args):
    """Put all four pieces where inference.py expects to find them."""
    if not os.path.isdir(WORK):
        os.makedirs(WORK)

    if not os.path.isdir(os.path.join(CODE, '.git')):
        print('cloning the inference code')
        subprocess.check_call(['git', 'clone', '--depth', '1', GITHUB, CODE])
    else:
        print('inference code already here')

    target = final_models()
    if not os.path.isdir(target):
        os.makedirs(target)

    encoder = os.path.join(CODE, 'saved_models', 'default', 'encoder.pt')
    if not os.path.exists(encoder):
        sys.exit('no encoder.pt in the cloned repo — it should be at\n  %s' % encoder)
    if not os.path.exists(os.path.join(target, 'encoder.pt')):
        shutil.copyfile(encoder, os.path.join(target, 'encoder.pt'))
        print('speaker encoder in place')

    if not os.path.exists(os.path.join(target, 'synthesizer.pt')):
        hf_hub_download = need('huggingface_hub',
                               'the synthesiser comes from there').hf_hub_download
        print('fetching synthesizer.pt (371 MB, once)')
        got = hf_hub_download(MODEL, 'synthesizer.pt', token=args.token)
        shutil.copyfile(got, os.path.join(target, 'synthesizer.pt'))
        print('synthesiser in place')

    if not os.path.exists(os.path.join(target, 'vocoder_HiFiGAN.pkl')):
        need('parallel_wavegan', 'it fetches and runs the vocoder')
        from parallel_wavegan.utils import download_pretrained_model
        print('fetching the vocoder')
        where = download_pretrained_model(VOCODER, WORK)
        # It returns the checkpoint; the config sits beside it under either name.
        folder = os.path.dirname(where)
        shutil.copyfile(where, os.path.join(target, 'vocoder_HiFiGAN.pkl'))
        for name in ('config.yml', 'config.yaml'):
            beside = os.path.join(folder, name)
            if os.path.exists(beside):
                shutil.copyfile(beside, os.path.join(target, 'config.yml'))
                break
        else:
            sys.exit('the vocoder came without a config.yml; it should be beside\n'
                     '  %s' % where)
        print('vocoder in place')

    print('\nready:')
    for name in sorted(os.listdir(target)):
        print('  %8.1f MB  %s'
              % (os.path.getsize(os.path.join(target, name)) / 1e6, name))


def check_ready(args):
    missing = [n for n in ('encoder.pt', 'synthesizer.pt', 'vocoder_HiFiGAN.pkl',
                           'config.yml')
               if not os.path.exists(os.path.join(final_models(), n))]
    if missing:
        sys.exit('not set up yet — missing %s\n  run --setup first'
                 % ', '.join(missing))
    if not args.ref or not os.path.exists(args.ref):
        sys.exit('--ref must point at a clean wav of the voice to clone%s'
                 % ('' if not args.ref else ' (%s is not there)' % args.ref))


def say(args, text, name):
    """One line, through the author's own inference.py.

    It writes into its own results folder and names the file after --test_name,
    so each line gets a name of its own and the finished wav is fetched from
    there rather than guessed at.
    """
    results = os.path.join(CODE, 'results')
    if not os.path.isdir(results):
        os.makedirs(results)
    out = os.path.join(results, name + '.wav')
    if os.path.exists(out):
        os.unlink(out)
    done = subprocess.run(
        [sys.executable, 'inference.py', '--vocoder', 'HiFiGAN',
         '--text', text, '--ref_wav_path', os.path.abspath(args.ref),
         '--test_name', name],
        cwd=CODE, capture_output=True, text=True)
    if not os.path.exists(out):
        tail = (done.stderr or done.stdout or '').strip().splitlines()
        raise RuntimeError('no wav came out: %s'
                           % ' '.join(tail[-3:])[:200] if tail else 'silent failure')
    return out


def write(source, path):
    if shutil.which('oggenc'):
        subprocess.check_call(['oggenc', '-Q', '-q', '4', '-o', path, source])
        return path
    final = os.path.splitext(path)[0] + '.wav'
    shutil.copyfile(source, final)
    return final


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
    check_ready(args)
    lines = load_lines()
    if args.sample not in lines:
        sys.exit('no line called %s' % args.sample)
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    print('reference : %s' % args.ref)
    print('line      : %s\ntext      : %s\n' % (args.sample, lines[args.sample]))
    made = say(args, lines[args.sample], 'pashmak_sample')
    final = write(made, os.path.join(SAMPLES, 'mana_%s.ogg' % args.sample))
    print('wrote %s  (%.1f KB)'
          % (os.path.relpath(final, ROOT), os.path.getsize(final) / 1024.0))


def do_all(args):
    check_ready(args)
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
        print('   %-10s %4d' % (mood, count))
    print('\nTacotron2 has no pace control, so the moods are only a tally here —\n'
          'the delivery is whatever the reference voice does.\n')
    done = failed = 0
    for i, name in enumerate(todo, 1):
        try:
            made = say(args, lines[name], name)
            final = write(made, os.path.join(RAW, name + '.ogg'))
            if os.path.getsize(final) < MIN_BYTES:
                raise RuntimeError('almost nothing came out')
            done += 1
            ledger['done'].append(name)
            write_ledger(ledger)
            if i % 20 == 0 or i == len(todo):
                print('[%4d/%4d] %-28s %6.1f KB'
                      % (i, len(todo), name, os.path.getsize(final) / 1024.0),
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
    parser.add_argument('--ref', help='a clean wav of the voice to clone')
    parser.add_argument('--token', help='a Hugging Face token, if needed')
    parser.add_argument('--only')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--setup', action='store_true',
                        help='assemble the four pieces; run this first')
    parser.add_argument('--sample', nargs='?', const='welcome')
    args = parser.parse_args()
    if args.setup:
        do_setup(args)
    elif args.sample:
        do_sample(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
