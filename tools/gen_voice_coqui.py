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
import re
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


# Breaks that are not this project's fault and have a known answer. Worth naming,
# because the exception alone sends you reading an unrelated traceback.
KNOWN = (
    (('isin_mps_friendly', 'transformers.pytorch_utils'),
     'transformers 5 removed what coqui-tts imports from it. Pin it back:\n'
     '      %(python)s -m pip install "transformers<5"'),
    (('torchcodec',),
     'PyTorch 2.9 moved audio IO onto torchcodec, which coqui-tts keeps behind\n'
     '    an extra rather than a dependency:\n'
     '      %(python)s -m pip install "coqui-tts[codec]"'),
    (('torchaudio',),
     'coqui-tts imports torchaudio without depending on it:\n'
     '      %(python)s -m pip install torchaudio --index-url '
     'https://download.pytorch.org/whl/cpu'),
)


def _known(detail):
    """A line of actual advice when the failure is one that has an answer."""
    for marks, advice in KNOWN:
        if all(mark in detail for mark in marks):
            return '\n\n    ' + advice % {'python': sys.executable}
    return ''


def preflight():
    problems = []

    for module, why, install in (('torch', 'the model runs on it', 'torch'),
                                 # coqui-tts imports torchaudio and does not
                                 # depend on it, so it goes missing quietly and
                                 # surfaces as the runtime failing to import.
                                 ('torchaudio', 'coqui-tts imports it',
                                  'torchaudio --index-url '
                                  'https://download.pytorch.org/whl/cpu'),
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
                        '    real error, and it is not a missing package:\n    %s%s'
                        % (name, detail, _known(detail)))

    if problems:
        print('\nthis machine is not ready yet:\n', file=sys.stderr)
        for i, problem in enumerate(problems, 1):
            print('  %d. %s\n' % (i, problem), file=sys.stderr)
        sys.exit(1)


def repo_files(model, token=None):
    from huggingface_hub import HfApi
    try:
        info = HfApi().model_info(model, files_metadata=True,
                                 token=token or _token())
    except Exception as problem:                           # noqa: BLE001
        _network_or_raise(problem)
        raise
    return sorted(((f.rfilename, f.size or 0) for f in info.siblings),
                  key=lambda row: -row[1])


def _network_or_raise(problem):
    """Exit with advice when the connection is being cut, rather than a traceback.

    SSLEOFError is not a certificate problem and not an authentication problem —
    it is the connection dying mid-handshake, which is what interference looks
    like from inside. A token cannot fix it and neither can retrying in a loop,
    so the remedies worth naming are a mirror or a different route out.
    """
    text = ' '.join(str(problem).split())
    marks = ('UNEXPECTED_EOF_WHILE_READING', 'SSLEOFError', 'MaxRetryError',
             'ConnectionResetError', 'ConnectionError', 'EOF occurred in violation')
    if not any(mark in text for mark in marks):
        return
    sys.exit(
        '\nthe connection to Hugging Face was cut mid-handshake:\n  %s\n'
        '\nThat is not the token and not this script. It is what interference on\n'
        'the way out looks like from here, and no amount of retrying in a loop\n'
        'fixes it. Three things do, in order of least effort:\n'
        '\n  1. just run it again — it is often intermittent\n'
        '  2. point huggingface_hub at a mirror, in this same shell:\n'
        '       set HF_ENDPOINT=https://hf-mirror.com\n'
        '     (a gated model may not be served by a mirror; if it refuses,\n'
        '      that is the gate rather than the network)\n'
        '  3. the same route you used when pypi was doing this\n'
        '\nOne other thing worth checking: the login said HF_TOKEN is already set\n'
        'in your environment and takes precedence over the token it just saved.\n'
        'If that older one is stale, `set HF_TOKEN=` clears it for this shell and\n'
        'the saved login is used instead.' % text[:200])


# XTTS does not load from one checkpoint: the vocabulary and the speaker files
# sit beside it and it will not start without them.
XTTS_WANTED = ('model.pth', 'config.json', 'vocab.json', 'speakers_xtts.pth',
               'dvae.pth', 'mel_stats.pth')


def pick(files, kind='vits'):
    """The files worth fetching, and the config, for this kind of model.

    A training repository holds every checkpoint it ever wrote — the Kamtera one
    has twelve, a gigabyte each. Two rules sort them: a file named best_model is
    the one the trainer kept because it scored best, so those beat a plain
    checkpoint, and among equals the highest step is the latest. Taking whichever
    came first meant downloading a gigabyte of an earlier, worse model.

    XTTS is different. It is a set — weights, vocabulary, speaker statistics —
    and missing any of them is a failure to start rather than a worse voice.
    """
    names = [n for n, _ in files]
    config = next((n for n in names if os.path.basename(n) == 'config.json'), None)
    if not config:
        config = next((n for n in names if n.endswith('.json')), None)

    if kind == 'xtts':
        wanted = [n for n in names if os.path.basename(n) in XTTS_WANTED]
        return wanted, config

    weights = [n for n in names
               if n.endswith(('.pth', '.pt')) and 'speaker' not in n.lower()]

    def rank(name):
        base = os.path.basename(name).lower()
        digits = re.findall(r'\d+', base)
        step = int(digits[-1]) if digits else -1
        return (1 if base.startswith('best_model') else 0, step)

    return ([max(weights, key=rank)] if weights else []), config


def kind_of(spec):
    """Whether this config describes an XTTS model or a plain one."""
    name = str(spec.get('model', '')).lower()
    return 'xtts' if 'xtts' in name or 'gpt' in name else 'vits'


def fetch(model, names, token=None):
    """The named files, with a gated repository explained rather than raised.

    A gate is not a bug and not a network fault: the listing is public and the
    download is not, so it fails only once something is actually wanted. Saying
    what to click beats thirty lines of urllib traceback.
    """
    from huggingface_hub import hf_hub_download
    from huggingface_hub.errors import GatedRepoError, RepositoryNotFoundError
    got = {}
    for name in names:
        if not name:
            continue
        print('   fetching %s' % name, flush=True)
        try:
            got[name] = hf_hub_download(model, name, token=token or _token())
        except GatedRepoError:
            sys.exit(_gated(model))
        except RepositoryNotFoundError:
            sys.exit('%s does not exist, or is private to someone else.' % model)
        except Exception as problem:                       # noqa: BLE001
            _network_or_raise(problem)
            raise
    return got


def _token():
    """Whatever token this machine already holds, if any."""
    for key in ('HF_TOKEN', 'HUGGING_FACE_HUB_TOKEN', 'HUGGINGFACEHUB_API_TOKEN'):
        if os.environ.get(key):
            return os.environ[key]
    try:
        from huggingface_hub import HfFolder
        return HfFolder.get_token()
    except Exception:                                      # noqa: BLE001
        return None


def _gated(model):
    return (
        '\n%s is a gated model. Its file list is public; downloading is not.\n'
        '\nThree steps, and the first one has to happen in a browser:\n'
        '\n  1. open https://huggingface.co/%s and accept the access terms\n'
        '     (some repos grant it at once, some wait for the author)\n'
        '  2. make a token at https://huggingface.co/settings/tokens — "read" is\n'
        '     enough\n'
        '  3. give it to this shell, either way round:\n'
        '       set HF_TOKEN=hf_xxxxxxxx\n'
        '     or, once and for all:\n'
        '       %s -m huggingface_hub.commands.huggingface_cli login\n'
        '\nThen run the same command again. A token already in the environment or\n'
        'saved by a previous login is picked up without being asked for.'
        % (model, model, sys.executable))


def do_check(args):
    files = repo_files(args.model, args.token)
    print('%s holds %d file(s), %.1f MB:\n'
          % (args.model, len(files), sum(s for _, s in files) / 1e6))
    for name, size in files:
        print('  %8.1f MB  %s' % (size / 1e6, name))

    _unused, config = pick(files)
    if not config:
        sys.exit('\nno config.json in that repository — send me the list above.')
    local = fetch(args.model, [config], args.token)
    spec = json.load(io.open(local[config], encoding='utf-8'))
    kind = kind_of(spec)
    wanted, _c = pick(files, kind)

    print('\nmodel     : %s  (read as %s)' % (spec.get('model'), kind))
    print('sample rate: %s' % (spec.get('audio', {}).get('sample_rate')
                               or spec.get('audio', {}).get('output_sample_rate')))
    print('will fetch : %d file(s), %.0f MB'
          % (len(wanted) + 1,
             sum(sz for n, sz in files if n in wanted) / 1e6))
    for name in wanted:
        print('   %s' % name)

    if kind == 'xtts':
        languages = spec.get('languages') or spec.get('model_args', {}).get('languages')
        print('\nlanguages  : %s' % (', '.join(languages) if languages else 'not listed'))
        if languages and 'fa' not in languages:
            print('\nNote: «fa» is not in that list. XTTS only speaks the languages its\n'
                  'tokenizer was built for, so a Persian fine-tune should name fa here.\n'
                  'If it does not, the sample will come out as nonsense and the right\n'
                  'language tag has to come from the model card.')
        print('\nThis is a cloning model, so no espeak is needed — it takes a reference\n'
              'clip instead. The default is the Umbriel greeting already in the repo.')
    else:
        phonemes = spec.get('use_phonemes')
        print('\nuse_phonemes: %s\nphonemizer  : %s\nlanguage    : %s'
              % (phonemes, spec.get('phonemizer'), spec.get('phoneme_language')))
        if phonemes:
            print('\nThis model speaks phonemes, so espeak-ng has to be on this machine\n'
                  'or it will produce nothing. On Windows: install espeak-ng, then set\n'
                  '  set PHONEMIZER_ESPEAK_LIBRARY=C:\\Program Files\\eSpeak NG\\libespeak-ng.dll\n'
                  'in the same shell before running --sample.')
        else:
            print('\nIt reads characters directly, so no espeak is needed.')


class VitsVoice(object):
    """A plain Coqui model: text in, audio out, paced by length_scale."""

    def __init__(self, synth):
        self.synth = synth
        self.rate = synth.output_sample_rate

    def say(self, text, pace, _reference):
        model = getattr(self.synth, 'tts_model', None)
        if model is not None and hasattr(model, 'length_scale'):
            model.length_scale = pace
        return self.synth.tts(text)


class XttsVoice(object):
    """XTTS: clones a reference clip, and needs no phonemiser of its own.

    The conditioning is computed once from the reference and reused for every
    line, which is both faster and the thing that keeps 1,136 clips sounding
    like one bear rather than 1,136 cousins.
    """

    def __init__(self, model, config, reference, language):
        self.model = model
        self.language = language
        self.rate = (config.audio.output_sample_rate
                     if hasattr(config.audio, 'output_sample_rate') else 24000)
        self.latent, self.embedding = model.get_conditioning_latents(
            audio_path=[reference])

    def say(self, text, pace, _reference):
        # speed is XTTS's pace control; older builds do not take it, and a
        # TypeError here should not read as a failure to speak.
        try:
            out = self.model.inference(text, self.language, self.latent,
                                       self.embedding, speed=pace)
        except TypeError:
            out = self.model.inference(text, self.language, self.latent,
                                       self.embedding)
        return out['wav'] if isinstance(out, dict) else out


def engine(args):
    preflight()
    files = repo_files(args.model, args.token)
    _first, config = pick(files)
    if not config:
        sys.exit('no config in that repository — run --check')
    local = fetch(args.model, [config], args.token)
    spec = json.load(io.open(local[config], encoding='utf-8'))
    kind = kind_of(spec)
    wanted, _c = pick(files, kind)
    if not wanted:
        sys.exit('nothing in that repository looks like weights — run --check')
    local.update(fetch(args.model, wanted, args.token))
    folder = os.path.dirname(local[wanted[0]])

    import importlib
    runtime = runtime_module()

    if kind == 'xtts':
        xtts_config = importlib.import_module(
            '%s.tts.configs.xtts_config' % runtime).XttsConfig()
        xtts_config.load_json(local[config])
        Xtts = importlib.import_module('%s.tts.models.xtts' % runtime).Xtts
        model = Xtts.init_from_config(xtts_config)
        model.load_checkpoint(xtts_config, checkpoint_dir=folder, eval=True)
        if args.cuda:
            model.cuda()
        language = args.language or _language_for(xtts_config)
        print('cloning %s, speaking «%s»'
              % (os.path.relpath(args.ref, ROOT), language))
        return XttsVoice(model, xtts_config, args.ref, language)

    Synthesizer = importlib.import_module(
        '%s.utils.synthesizer' % runtime).Synthesizer
    return VitsVoice(Synthesizer(tts_checkpoint=local[wanted[0]],
                                 tts_config_path=local[config],
                                 use_cuda=bool(args.cuda)))


def _language_for(config):
    """Persian when the model admits to it, and said plainly when it does not."""
    languages = list(getattr(config, 'languages', None) or [])
    if 'fa' in languages:
        return 'fa'
    if languages:
        print('warning: this model lists %s and not fa. Using %s; if the result is\n'
              'nonsense, the right tag is on the model card — pass --language.'
              % (', '.join(languages), languages[0]), file=sys.stderr)
        return languages[0]
    return 'fa'


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
    voice = engine(args)
    rate = voice.rate
    mood = mood_for(args.sample)
    print('\nline : %s   (mood %s, pace %.2f)' % (args.sample, mood, PACE[mood]))
    print('text : %s\n' % lines[args.sample])
    audio = voice.say(lines[args.sample], PACE[mood], args.ref)
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
    voice = engine(args)
    rate = voice.rate
    done = failed = 0
    for i, name in enumerate(todo, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            audio = voice.say(lines[name], PACE[mood_for(name)], args.ref)
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
    parser.add_argument('--ref', default=os.path.join(ROOT,
                        'tools/reference/umbriel_welcome.wav'),
                        help='for a cloning model: the voice to copy')
    parser.add_argument('--language', help='override the tag XTTS is given')
    parser.add_argument('--token', help='a Hugging Face read token, for a '
                                        'gated model')
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
