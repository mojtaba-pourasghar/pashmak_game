#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Reads the app's lines with a Coqui Persian VITS voice, e.g. the Kamtera male one.

Run this on your own machine: the weights live on Hugging Face, which the sandbox
this was written in cannot reach.

    py -3.12 -m venv .venv
    .venv\\Scripts\\activate
    python -m pip install coqui-tts scipy numpy huggingface_hub
    python tools/gen_voice_coqui.py --check      # look before leaping
    python tools/gen_voice_coqui.py --sample     # one line, plain and shaped
    python tools/gen_voice_coqui.py             # all 1,136 into res/raw

--check is worth doing first and costs one small download. It prints what the
repository holds, then reads the model's own config and says whether it wants
espeak — a VITS model trained on phonemes will not speak a word without it, and
on Windows that means installing espeak-ng and pointing PHONEMIZER_ESPEAK_LIBRARY
at its dll. Finding that out from a config is pleasanter than from silence.

Every clip is written twice in --sample: as the model made it, and through
tools/voice_shape.py. The shaping is where «رسا» is made concrete — a few dB
across 2-4 kHz, where consonants live, so a line carries to a child who is not
listening carefully; plus a shelf under 240 Hz for body, the edge off above
7 kHz, soft compression and one level for the whole app. The plain file is there
because shaping is an opinion and the model's output is the reference.
"""
import argparse
import io
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for, tally                    # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.normpath(os.path.join(ROOT, 'app/src/main/res/raw'))
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
LEDGER = os.path.join(ROOT, 'tools/voice_done_coqui.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
MODEL = 'Kamtera/persian-tts-male1-vits'
MIN_BYTES = 900

# VITS paces itself with length_scale: 1.0 is the model's own speed, higher is
# slower. That is the only lever this kind of model has, so the moods are paces.
PACE = {
    'delighted': 0.95,
    'kind': 1.18,
    'bedtime': 1.40,
    'glyph': 1.45,
    'story': 1.18,
    'game': 1.08,
}


def probe(module):
    """Why a module will not import, told apart properly.

    "Not installed" and "installed but broken" are different problems with
    different fixes, and an ImportError alone does not distinguish them: an
    ImportError also comes from a failed import *inside* a package that is
    perfectly well installed. find_spec answers the question — a spec means the
    package is on disk, so any failure after that is internal and the real
    message is the useful thing to print.
    """
    import importlib
    import importlib.util
    try:
        spec = importlib.util.find_spec(module)
    except (ImportError, ValueError):
        spec = None
    if spec is None:
        return 'missing', ''
    try:
        importlib.import_module(module)
        return 'ok', ''
    except BaseException as problem:                       # noqa: BLE001
        return 'broken', '%s: %s' % (type(problem).__name__,
                                     ' '.join(str(problem).split())[:300])


# The Coqui runtime has been imported under more than one name across versions.
RUNTIME = ('TTS', 'coqui_tts')


def runtime_module():
    """Whichever name the installed Coqui package answers to."""
    for name in RUNTIME:
        state, _ = probe(name)
        if state == 'ok':
            return name
    return None


def preflight():
    problems = []

    for module, why, install in (('torch', 'the model runs on it', 'torch'),
                                 ('numpy', 'the audio maths', 'numpy'),
                                 ('scipy', 'the filters', 'scipy'),
                                 ('huggingface_hub', 'fetching the weights',
                                  'huggingface_hub')):
        state, detail = probe(module)
        if state == 'missing':
            problems.append('%s is not installed (%s):\n      %s -m pip install %s'
                            % (module, why, sys.executable, install))
        elif state == 'broken':
            problems.append('%s is installed but will not import (%s):\n    %s'
                            % (module, why, detail))

    states = [(name,) + probe(name) for name in RUNTIME]
    if not any(state == 'ok' for _n, state, _d in states):
        if all(state == 'missing' for _n, state, _d in states):
            problems.append('the Coqui runtime is not installed:\n'
                            '      %s -m pip install coqui-tts' % sys.executable)
        else:
            for name, state, detail in states:
                if state == 'broken':
                    problems.append(
                        'coqui-tts IS installed, but importing %s fails. This is the\n'
                        '    real error, and it is not a missing package:\n    %s'
                        % (name, detail))

    if problems:
        print('\nthis machine is not ready yet:\n', file=sys.stderr)
        for i, problem in enumerate(problems, 1):
            print('  %d. %s\n' % (i, problem), file=sys.stderr)
        sys.exit(1)


def repo_files(model):
    from huggingface_hub import HfApi
    info = HfApi().model_info(model, files_metadata=True)
    return sorted(((f.rfilename, f.size or 0) for f in info.siblings),
                  key=lambda row: -row[1])


def pick(files):
    """The checkpoint and the config, by the names Coqui models actually use."""
    checkpoint = next((n for n, _ in files
                       if n.endswith(('.pth', '.pt')) and 'speaker' not in n.lower()),
                      None)
    config = next((n for n, _ in files if os.path.basename(n) == 'config.json'), None)
    if not config:
        config = next((n for n, _ in files if n.endswith('.json')), None)
    return checkpoint, config


def fetch(model, names):
    from huggingface_hub import hf_hub_download
    got = {}
    for name in names:
        if name:
            print('   fetching %s' % name, flush=True)
            got[name] = hf_hub_download(model, name)
    return got


def do_check(args):
    files = repo_files(args.model)
    print('%s holds %d file(s), %.1f MB:\n'
          % (args.model, len(files), sum(s for _, s in files) / 1e6))
    for name, size in files:
        print('  %8.1f MB  %s' % (size / 1e6, name))
    checkpoint, config = pick(files)
    print('\ncheckpoint: %s\nconfig    : %s' % (checkpoint, config))
    if not checkpoint or not config:
        sys.exit('\ncannot see both a checkpoint and a config — send me that list.')

    local = fetch(args.model, [config])
    spec = json.load(io.open(local[config], encoding='utf-8'))
    phonemes = spec.get('use_phonemes')
    print('\nsample rate   : %s' % spec.get('audio', {}).get('sample_rate'))
    print('use_phonemes  : %s' % phonemes)
    print('phonemizer    : %s' % spec.get('phonemizer'))
    print('language      : %s' % spec.get('phoneme_language'))
    if phonemes:
        print('\nThis model speaks phonemes, so espeak-ng has to be on this machine\n'
              'or it will produce nothing. On Windows: install espeak-ng, then set\n'
              '  set PHONEMIZER_ESPEAK_LIBRARY=C:\\Program Files\\eSpeak NG\\libespeak-ng.dll\n'
              'in the same shell before running --sample.')
    else:
        print('\nIt reads characters directly, so no espeak is needed.')


def engine(args):
    preflight()
    files = repo_files(args.model)
    checkpoint, config = pick(files)
    if not checkpoint or not config:
        sys.exit('cannot find a checkpoint and a config — run --check')
    local = fetch(args.model, [checkpoint, config])
    import importlib
    name = runtime_module()
    Synthesizer = importlib.import_module('%s.utils.synthesizer' % name).Synthesizer
    synth = Synthesizer(tts_checkpoint=local[checkpoint],
                        tts_config_path=local[config],
                        use_cuda=bool(args.cuda))
    return synth


def say(synth, text, pace):
    """One line, at the pace this mood asks for."""
    model = getattr(synth, 'tts_model', None)
    if model is not None and hasattr(model, 'length_scale'):
        model.length_scale = pace
    return synth.tts(text)


def write(samples, rate, path, shaped=True):
    import numpy as np
    import wave
    import subprocess
    import voice_shape as shaping
    audio = np.asarray(samples, dtype=np.float64)
    if audio.size and np.max(np.abs(audio)) <= 1.5:
        audio = audio * 32767.0                            # Coqui returns floats
    pcm = shaping.shape(audio, rate) if shaped else shaping.to_pcm16(
        audio / max(np.max(np.abs(audio)), 1.0) * 0.89 / 32767.0 * 32767.0)
    wav = os.path.splitext(path)[0] + '.tmp.wav'
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(np.asarray(pcm, dtype='<i2').tobytes())
    try:
        subprocess.check_call(['oggenc', '-Q', '-q', '4', '-o', path, wav])
        os.unlink(wav)
        final = path
    except (OSError, subprocess.CalledProcessError):
        final = os.path.splitext(path)[0] + '.wav'
        os.replace(wav, final)
    return final, os.path.getsize(final), pcm


def read_ledger():
    if not os.path.exists(LEDGER):
        return {'model': MODEL, 'done': []}
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
    import voice_shape as shaping
    lines = load_lines()
    if args.sample not in lines:
        sys.exit('no line called %s' % args.sample)
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    synth = engine(args)
    rate = synth.output_sample_rate
    mood = mood_for(args.sample)
    print('\nline : %s   (mood %s, pace %.2f)' % (args.sample, mood, PACE[mood]))
    print('text : %s\n' % lines[args.sample])
    audio = say(synth, lines[args.sample], PACE[mood])
    for tag, shaped in (('plain', False), ('shaped', True)):
        path = os.path.join(SAMPLES, 'coqui_%s.ogg' % tag)
        final, size, pcm = write(audio, rate, path, shaped=shaped)
        print('  %-7s %5.2f s  %3.0f Hz  %6.1f KB  %s'
              % (tag, len(pcm) / float(rate), shaping.fundamental(pcm, rate),
                 size / 1024.0, os.path.relpath(final, ROOT)))
    print('\na grown man is near 110 Hz, a small child near 280.')


def do_all(args):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    if not shutil.which('oggenc'):
        sys.exit('oggenc is not on PATH. Over an hour of speech does not ship as\n'
                 'wav — install vorbis-tools. (--sample works without it.)')
    ledger = read_ledger()
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do'
          % (len(names), len(names) - len(todo), len(todo)))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %4d   pace %.2f' % (mood, count, PACE[mood]))
    print()
    synth = engine(args)
    rate = synth.output_sample_rate
    done = failed = 0
    for i, name in enumerate(todo, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            audio = say(synth, lines[name], PACE[mood_for(name)])
            staged = path + '.part'
            final, size, _pcm = write(audio, rate, staged, shaped=True)
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
    parser.add_argument('--model', default=MODEL)
    parser.add_argument('--cuda', action='store_true')
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--check', action='store_true',
                        help='print the repository and what the config needs')
    parser.add_argument('--sample', nargs='?', const='welcome')
    args = parser.parse_args()
    if args.check:
        preflight()
        do_check(args)
    elif args.sample:
        do_sample(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
