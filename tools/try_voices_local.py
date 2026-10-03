#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Fetches Persian Piper voices and reads one line with each, to choose by ear.

Run this on your own machine. Everything it needs is small: piper-tts is a few
megabytes, each voice is 20-80 MB, and there is no torch anywhere in it — which
is what made every other route painful.

    py -3.12 -m venv .venv
    .venv\\Scripts\\activate
    python -m pip install piper-tts scipy numpy
    python tools/try_voices_local.py --list
    python tools/try_voices_local.py fa_IR-amir-medium fa_IR-gyro-medium

--list prints every Persian voice the Piper catalogue holds, with its size and
whether it is a man or a woman, straight from the index — so the names come from
the catalogue rather than from anyone's memory. Then name the ones you want and
each is downloaded, asked to read the greeting, and written to
tools/voice_samples/.

Each voice is written twice: once exactly as the model made it, and once through
the shaping in tools/voice_shape.py — a gentle shelf under 240 Hz, the edge taken
off above 7 kHz, soft compression, one fixed level. Both, because the shaping is
an opinion and the model's own output is the reference.
"""
import argparse
import io
import json
import os
import subprocess
import sys
import urllib.request
import wave

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


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
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
VOICES = os.path.join(ROOT, 'tools/models')
OUT = os.path.join(ROOT, 'tools/voice_samples')
INDEX = 'https://huggingface.co/rhasspy/piper-voices/resolve/main/voices.json'
BASE = 'https://huggingface.co/rhasspy/piper-voices/resolve/main/'


def catalogue():
    with urllib.request.urlopen(INDEX, timeout=120) as answer:
        return json.loads(answer.read().decode('utf-8'))


def persian(index):
    """Only the Persian entries, with what is known about each."""
    found = []
    for name, entry in index.items():
        language = entry.get('language', {})
        if language.get('code', '').startswith('fa') or name.startswith('fa_'):
            size = sum(f.get('size_bytes', 0) for f in entry.get('files', {}).values())
            found.append((name, entry.get('quality', '?'),
                          ', '.join(entry.get('speaker_id_map', {}) or {}) or
                          'single speaker', size, entry))
    return sorted(found)


def fetch(entry, name):
    """The .onnx and its .json, skipped when they are already here."""
    if not os.path.isdir(VOICES):
        os.makedirs(VOICES)
    local = None
    for path in entry.get('files', {}):
        if not path.endswith(('.onnx', '.onnx.json')):
            continue
        target = os.path.join(VOICES, os.path.basename(path))
        if path.endswith('.onnx'):
            local = target
        if os.path.exists(target):
            continue
        print('   fetching %s' % os.path.basename(path), flush=True)
        urllib.request.urlretrieve(BASE + path, target)
    if not local or not os.path.exists(local):
        raise RuntimeError('no .onnx arrived for %s' % name)
    return local


def read_wav(path):
    import numpy as np
    with wave.open(path, 'rb') as w:
        rate = w.getframerate()
        audio = np.frombuffer(w.readframes(w.getnframes()),
                              dtype='<i2').astype('float64')
    return audio, rate


def write(samples, rate, path):
    import numpy as np
    wav = os.path.splitext(path)[0] + '.tmp.wav'
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(np.asarray(samples, dtype='<i2').tobytes())
    try:
        subprocess.check_call(['oggenc', '-Q', '-q', '4', '-o', path, wav])
        os.unlink(wav)
        return path
    except (OSError, subprocess.CalledProcessError):
        # No oggenc: the wav stands in. Android would play it too; it is only
        # larger, which matters for 1,136 clips and not for a sample.
        final = os.path.splitext(path)[0] + '.wav'
        os.replace(wav, final)
        return final


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('voices', nargs='*', help='voice names, from --list')
    parser.add_argument('--list', action='store_true',
                        help='print the Persian voices in the catalogue and stop')
    parser.add_argument('--line', default='welcome', help='which clip to read')
    parser.add_argument('--pace', type=float, default=1.10,
                        help='1.0 is the model\'s own speed; higher is slower')
    args = parser.parse_args()

    try:
        import numpy                                       # noqa: F401
        from piper import PiperVoice, SynthesisConfig
    except ImportError as missing:
        sys.exit('%s\n\ninstall into this interpreter:\n  %s -m pip install '
                 'piper-tts scipy numpy' % (missing, sys.executable))

    if args.list:
        rows = persian(catalogue())
        if not rows:
            sys.exit('the catalogue lists no Persian voice')
        print('Persian voices in the Piper catalogue:\n')
        for name, quality, speakers, size, _ in rows:
            print('  %-30s %-8s %6.1f MB  %s'
                  % (name, quality, size / 1e6, speakers))
        print('\nname the ones you want, e.g.:\n  python %s %s'
              % (os.path.relpath(__file__, os.getcwd()),
                 ' '.join(n for n, *_ in rows[:2])))
        return

    if not args.voices:
        sys.exit('name at least one voice, or pass --list to see them')

    lines = json.load(io.open(LINES, encoding='utf-8'))
    if args.line not in lines:
        sys.exit('no line called %s' % args.line)
    text = lines[args.line]
    index = {name: entry for name, _q, _s, _b, entry in persian(catalogue())}
    if not os.path.isdir(OUT):
        os.makedirs(OUT)

    import voice_shape as shaping
    print('line: %s\ntext: %s\n' % (args.line, text))
    for name in args.voices:
        if name not in index:
            print('  %-28s not in the catalogue — check --list' % name)
            continue
        print('  %s' % name)
        try:
            model = fetch(index[name], name)
            voice = PiperVoice.load(model)
            scratch = os.path.join(OUT, '.scratch.wav')
            with wave.open(scratch, 'wb') as w:
                voice.synthesize_wav(text, w,
                                     syn_config=SynthesisConfig(length_scale=args.pace))
            audio, rate = read_wav(scratch)
            os.unlink(scratch)

            plain = write(shaping.to_pcm16(audio / max(abs(audio).max(), 1.0) * 0.89),
                          rate, os.path.join(OUT, '%s__plain.ogg' % name))
            shaped = write(shaping.shape(audio, rate), rate,
                           os.path.join(OUT, '%s__shaped.ogg' % name))
            pitch = shaping.fundamental(shaping.to_pcm16(audio / 32768.0), rate)
            print('     %5.2f s   %3.0f Hz   %s   %s'
                  % (len(audio) / float(rate), pitch,
                     os.path.basename(plain), os.path.basename(shaped)))
        except Exception as problem:                       # noqa: BLE001
            print('     FAILED %s' % ' '.join(str(problem).split())[:160])
    print('\nin %s. a grown man is near 110 Hz, a small child near 280.'
          % os.path.relpath(OUT, ROOT))
    print('tell me the name you want and I will build all 1,136 lines with it.')


if __name__ == '__main__':
    main()
