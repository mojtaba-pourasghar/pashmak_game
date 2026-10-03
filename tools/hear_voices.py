#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Fetches the demo clip of every voice in a collection repo, so one can be chosen.

Some Hugging Face repositories are not one model but a shelf of them, each in its
own folder with a sample beside it. karim23657/persian-tts-vits is eight Persian
voices that way — six Coqui VITS exports, two Piper, one Mimic3 — and every one
ships a test.wav of about a third of a megabyte.

That is the cheapest possible way to choose: a couple of megabytes buys every
voice in the shelf, and only the winner's 114 MB has to be downloaded at all.

    python3 tools/hear_voices.py --model karim23657/persian-tts-vits

The clips land in tools/voice_samples/heard/. Listen, pick, and only then fetch
the model that won.
"""
import argparse
import os
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
OUT = os.path.join(ROOT, 'tools/voice_samples/heard')
MODEL = 'karim23657/persian-tts-vits'
DEMOS = ('test.wav', 'sample.wav', 'demo.wav', 'example.wav')


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--model', default=MODEL)
    parser.add_argument('--token')
    args = parser.parse_args()

    try:
        from huggingface_hub import HfApi, hf_hub_download
    except ImportError:
        sys.exit('%s -m pip install huggingface_hub' % sys.executable)

    info = HfApi().model_info(args.model, files_metadata=True, token=args.token)
    files = [(f.rfilename, f.size or 0) for f in info.siblings]

    # A voice is a folder here, and its weights are what mark it as one — a demo
    # clip loose at the root belongs to nothing.
    voices = {}
    for name, size in files:
        folder = os.path.dirname(name)
        if not folder:
            continue
        base = os.path.basename(name).lower()
        entry = voices.setdefault(folder, {'weights': 0, 'demo': None})
        if base.endswith('.onnx') or base.endswith(('.pth', '.pt')):
            entry['weights'] += size
        elif base in DEMOS or (base.endswith('.wav') and not entry['demo']):
            entry['demo'] = name

    shelf = {k: v for k, v in voices.items() if v['weights']}
    if not shelf:
        sys.exit('%s does not look like a shelf of voices — no folder in it holds '
                 'weights.' % args.model)

    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    print('%d voice(s) in %s\n' % (len(shelf), args.model))
    for folder in sorted(shelf):
        entry = shelf[folder]
        line = '  %-34s %6.1f MB' % (folder, entry['weights'] / 1e6)
        if not entry['demo']:
            print(line + '   no demo clip')
            continue
        try:
            got = hf_hub_download(args.model, entry['demo'], token=args.token)
            local = os.path.join(OUT, folder.replace('/', '_') + '.wav')
            with open(got, 'rb') as source, open(local, 'wb') as target:
                target.write(source.read())
            print(line + '   %s' % os.path.basename(local))
        except Exception as problem:                       # noqa: BLE001
            print(line + '   FAILED %s'
                  % ' '.join(str(problem).split())[:80])

    print('\nin %s' % os.path.relpath(OUT, ROOT))
    print('listen to all of them, then tell me the folder name you want. Only that\n'
          'one gets downloaded — the others cost nothing.')


if __name__ == '__main__':
    main()
