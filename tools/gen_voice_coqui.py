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
    python tools/gen_voice_coqui.py --fresh      # the same, over another voice

The voice that was chosen by ear is male1, the Coqui VITS one. It is its own
repository, which is the default above, and it is also a folder in the eight-voice
shelf whose demo clips were the ones listened to. --folder takes that route, and
is the way to be sure the weights are the very ones behind the clip:

    python tools/gen_voice_coqui.py --check --model karim23657/persian-tts-vits \
        --folder persian-tts-male1-vits-coqui

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
import voice_output                                        # noqa: E402


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


def repo_files(model, token=None, folder=None):
    """What the repository holds, biggest first, or just one folder of it.

    Some repositories are a shelf rather than a model: karim23657/persian-tts-vits
    is eight Persian voices, each in its own folder with its own config. Keeping
    the whole listing would let pick() choose a checkpoint from one voice and the
    config of another, which starts and then sounds wrong, so the folder is cut
    down to here where it still means something.
    """
    from huggingface_hub import HfApi
    try:
        info = HfApi().model_info(model, files_metadata=True,
                                 token=token or _token())
    except Exception as problem:                           # noqa: BLE001
        _network_or_raise(problem)
        raise
    files = sorted(((f.rfilename, f.size or 0) for f in info.siblings),
                   key=lambda row: -row[1])
    if not folder:
        return files
    prefix = folder.strip('/') + '/'
    inside = [row for row in files if row[0].startswith(prefix)]
    if not inside:
        sys.exit('%s has no folder called %s. Its folders are:\n  %s'
                 % (model, folder, '\n  '.join(sorted(
                     {n.split('/')[0] for n, _ in files if '/' in n}) or
                     ['(none — the files sit at the top level)'])))
    return inside


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
    files = repo_files(args.model, args.token, args.folder)
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


# XTTS's text preprocessing has a hardcoded list of languages, and a fine-tune to
# a new one extends the model and the vocabulary without touching it. So the model
# knows Persian, the library does not, and inference stops at
# NotImplementedError: Language 'fa' is not supported.
TAUGHT = ('fa',)


def teach(runtime, language):
    """Let the installed tokenizer accept a language the fine-tune added.

    The fine-tune's vocab.json is a BPE trained on Persian, so the text wants
    passing through rather than running through cleaners built for other
    alphabets: collapsing whitespace is the whole job. What was patched is
    printed, because a silent monkeypatch that misses a second gate is worse than
    none at all.
    """
    import importlib
    if language.split('-')[0] not in TAUGHT:
        return
    module = importlib.import_module('%s.tts.layers.xtts.tokenizer' % runtime)
    tokenizer = getattr(module, 'VoiceBpeTokenizer', None)
    if tokenizer is None:
        print('warning: no VoiceBpeTokenizer to teach; inference may still refuse '
              '%s' % language, file=sys.stderr)
        return

    done = []
    original = tokenizer.preprocess_text

    def preprocess_text(self, txt, lang):
        if str(lang).split('-')[0] in TAUGHT:
            return ' '.join(str(txt).split())
        return original(self, txt, lang)

    tokenizer.preprocess_text = preprocess_text
    done.append('preprocess_text')

    # A per-language length cap sits in a dict somewhere nearby and raises a
    # KeyError on an unknown key, which would be the next wall along.
    for holder, label in ((module, 'module'), (tokenizer, 'class')):
        for name in dir(holder):
            if 'limit' not in name.lower():
                continue
            value = getattr(holder, name, None)
            if isinstance(value, dict) and 'en' in value:
                for code in TAUGHT:
                    value.setdefault(code, value['en'])
                done.append('%s.%s' % (label, name))

    print('taught the tokenizer %s: %s' % (', '.join(TAUGHT), ', '.join(done)))


def do_notebook(args):
    """Print the inference notebook the model's own author shipped.

    When a guess about a library's internals is needed, the author's own code is
    the place to look, and it is a few kilobytes next to a 5.6 GB checkpoint.
    """
    files = repo_files(args.model, args.token, args.folder)
    books = [n for n, _ in files if n.endswith('.ipynb')]
    if not books:
        sys.exit('no notebook in %s' % args.model)
    local = fetch(args.model, books, args.token)
    for name in books:
        spec = json.load(io.open(local[name], encoding='utf-8'))
        print('=== %s ===\n' % name)
        for cell in spec.get('cells', []):
            if cell.get('cell_type') != 'code':
                continue
            body = ''.join(cell.get('source', [])).strip()
            if body:
                print(body + '\n' + '-' * 60)



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


def _output_rate(config):
    """The rate XTTS actually emits at, looked for where it really lives."""
    for holder, field in ((getattr(config, 'model_args', None), 'output_sample_rate'),
                          (getattr(config, 'audio', None), 'output_sample_rate'),
                          (getattr(config, 'audio', None), 'sample_rate')):
        value = getattr(holder, field, None) if holder is not None else None
        if value:
            return int(value)
    return 24000



class XttsVoice(object):
    """XTTS, driven the way ParsVoice's own inference notebook drives it.

    Five things here come from that notebook rather than from guessing, and each
    one was wrong in the first version of this file:

    The checkpoint and the vocabulary are named separately. A fine-tune's
    vocab.json is the thing that makes it speak a new language at all, and
    pointing load_checkpoint at a directory lets it find some other vocabulary —
    or none.

    The reference clip is normalised first: mono, 24 kHz, RMS brought to about
    -20 dBFS, peak-limited. Cloning reads level as part of the voice, so an
    un-normalised reference is a different speaker.

    Persian punctuation is folded to ASCII. The tokenizer was trained that way,
    so «؟» unfolded is an unknown token in the middle of every question.

    Long text is split into sentences and synthesised one at a time. XTTS has a
    length limit per call, and a story passage is well past it.

    And the sampling is the author's: temperature 0.1 with repetition_penalty 10
    is a deliberately tight, repetition-averse setting, nothing like the defaults.
    """

    # The notebook's values. Low temperature and a heavy repetition penalty are
    # what keep a long Persian line from wandering.
    SAMPLING = dict(temperature=0.1, length_penalty=1.0, repetition_penalty=10.0,
                    top_k=10, top_p=0.3)

    # Folded because the tokenizer was trained on the folded forms.
    PUNCTUATION = {'؟': '?', '،': ',', '؛': ';', '«': '"', '»': '"',
                   '٬': ',', 'ـ': '-', '…': '...'}

    # The short vowels, and the shadda. Hand-written into the manifest because
    # espeak and an IPA model cannot read Persian without them — and very likely
    # poison here, because this model's vocabulary is a BPE trained on ordinary
    # Persian, where they simply do not appear. An unknown token in the middle of
    # every other word is exactly what a ruined reading sounds like.
    HARAKAT = '\u064b\u064c\u064d\u064e\u064f\u0650\u0651\u0652\u0670'

    def __init__(self, model, config, reference, language, rate,
                 vowels=False, speed=True):
        self.vowels = vowels
        self.speed = speed
        self.model = model
        self.config = config
        self.language = language
        self.rate = rate
        self.latent, self.embedding = model.get_conditioning_latents(
            audio_path=self._normalised(reference),
            gpt_cond_len=getattr(config, 'gpt_cond_len', 30),
            max_ref_length=getattr(config, 'max_ref_len', 10),
            sound_norm_refs=getattr(config, 'sound_norm_refs', False))

    def _normalised(self, path):
        """Mono, 24 kHz, about -20 dBFS: the reference as the model expects it."""
        import torch
        import torchaudio
        import torchaudio.functional as F
        wav, rate = torchaudio.load(path)
        if wav.shape[0] > 1:
            wav = wav.mean(dim=0, keepdim=True)
        if rate != self.rate:
            wav = F.resample(wav, orig_freq=rate, new_freq=self.rate)
        rms = wav.pow(2).mean().sqrt()
        wav = wav * ((10 ** (-20 / 20.0)) / (rms + 1e-9))
        peak = wav.abs().max()
        if peak > 1.0:
            wav = wav / peak
        out = os.path.join(SAMPLES, '.reference_24k.wav')
        if not os.path.isdir(SAMPLES):
            os.makedirs(SAMPLES)
        torchaudio.save(out, wav, self.rate)
        print('reference normalised to %d Hz mono at about -20 dBFS' % self.rate)
        return out

    def _fold(self, text):
        for persian, plain in self.PUNCTUATION.items():
            text = text.replace(persian, plain)
        if not self.vowels:
            text = ''.join(c for c in text if c not in self.HARAKAT)
        return ' '.join(text.split())

    def say(self, text, pace, _reference):
        import torch
        pieces = [p.strip() for p in re.split(r'(?<=[.!?])\s+', self._fold(text))
                  if p.strip()]
        if not pieces:
            raise RuntimeError('nothing to say')
        chunks = []
        with torch.inference_mode():
            for piece in pieces:
                how = dict(self.SAMPLING)
                # speed is this file's addition, not the author's. It is cheap to
                # ask for and it does cost quality, so it is switchable.
                if self.speed:
                    how['speed'] = pace
                try:
                    result = self.model.inference(
                        text=piece, language=self.language,
                        gpt_cond_latent=self.latent,
                        speaker_embedding=self.embedding, **how)
                except TypeError:
                    how.pop('speed', None)
                    result = self.model.inference(
                        text=piece, language=self.language,
                        gpt_cond_latent=self.latent,
                        speaker_embedding=self.embedding, **how)
                wav = result['wav'] if isinstance(result, dict) else result
                chunks.append(torch.as_tensor(wav).flatten())
        return torch.cat(chunks).cpu().numpy()


def engine(args):
    preflight()
    files = repo_files(args.model, args.token, args.folder)
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
        vocab = next((local[n] for n in wanted
                      if os.path.basename(n) == 'vocab.json'), None)
        if not vocab:
            sys.exit('no vocab.json came down — a fine-tune cannot speak its new '
                     'language without it')
        # Named, not discovered: the vocabulary is what makes this model Persian.
        model.load_checkpoint(xtts_config,
                              checkpoint_path=local[
                                  next(n for n in wanted
                                       if os.path.basename(n) == 'model.pth')],
                              vocab_path=vocab, use_deepspeed=False)
        language = args.language or _language_for(xtts_config)
        import torch
        device = 'cuda:0' if (args.cuda and torch.cuda.is_available()) else 'cpu'
        model.to(device)
        teach(runtime, language)
        rate = _output_rate(xtts_config)
        print('cloning %s, speaking «%s» on %s, writing at %d Hz'
              % (os.path.relpath(args.ref, ROOT), language, device, rate))
        return XttsVoice(model, xtts_config, args.ref, language, rate,
                         vowels=bool(args.vowels), speed=not args.no_speed)

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
        voice_output.encode(wav, path)
        os.unlink(wav)
        final = path
    except (OSError, RuntimeError, subprocess.CalledProcessError):
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


def do_fresh(args):
    """Start the whole set again: the lines from the manifest, the clips deleted.

    The checks all run before the first deletion. res/raw holding a name the
    Java refers to is not optional — a clip missing from it is a compile error,
    not a silence — so nothing is removed until it is certain the run can
    replace it.
    """
    voice_output.check_encoder(os.path.join(SAMPLES, 'check'))
    voice = engine(args)               # the model loads and speaks, or we stop
    voice_output.rebuild_lines(ROOT)
    voice_output.clear_spoken(RAW, load_lines())
    if os.path.exists(LEDGER):
        os.unlink(LEDGER)
    do_all(args, voice)


def do_all(args, voice=None):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    voice_output.check_encoder(os.path.join(SAMPLES, 'check'))
    ledger = read_ledger()
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do'
          % (len(names), len(names) - len(todo), len(todo)))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %4d   pace %.2f' % (mood, count, PACE[mood]))
    print()
    voice = voice or engine(args)
    rate = voice.rate
    done = failed = 0
    for i, name in enumerate(todo, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            audio = voice.say(lines[name], PACE[mood_for(name)], args.ref)
            staged = path + '.part'
            final, size, _pcm = write(audio, rate, staged,
                                      shaped=not args.plain)
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


def do_probe(args):
    """Four readings of one line, to find out which of my guesses was wrong.

    A bad result from a cloning model has a small number of likely causes, and
    arguing about them one round trip at a time is slower than hearing all four:

      vowels   the manifest's harakat, which this model's vocabulary has never
               seen — the first suspect
      speed    XTTS's pace control, which this file asked for and the author's
               notebook does not
    """
    import voice_shape as shaping
    lines = load_lines()
    if args.probe not in lines:
        sys.exit('no line called %s' % args.probe)
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    mood = mood_for(args.probe)
    print('\nline : %s\ntext : %s\n' % (args.probe, lines[args.probe]))
    voice = engine(args)
    for vowels in (False, True):
        for speed in (False, True):
            voice.vowels, voice.speed = vowels, speed
            tag = '%s_%s' % ('vowels' if vowels else 'plain',
                             'paced' if speed else 'natural')
            path = os.path.join(SAMPLES, 'xtts_%s.ogg' % tag)
            try:
                audio = voice.say(lines[args.probe], PACE[mood], args.ref)
                final, size, pcm = write(audio, voice.rate, path, shaped=False)
                print('  %-16s %5.2f s  %3.0f Hz  %6.1f KB  %s'
                      % (tag, len(pcm) / float(voice.rate),
                         shaping.fundamental(pcm, voice.rate), size / 1024.0,
                         os.path.basename(final)))
            except Exception as problem:                   # noqa: BLE001
                print('  %-16s FAILED %s'
                      % (tag, ' '.join(str(problem).split())[:120]))
    print('\nplain_natural is the author\'s own recipe exactly. If that one is\n'
          'good and the others are not, the cause is named.')



def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--model', default=MODEL)
    parser.add_argument('--folder', help='one voice folder inside a repo '
                                         'that holds several')
    parser.add_argument('--cuda', action='store_true')
    parser.add_argument('--ref', default=os.path.join(ROOT,
                        'tools/reference/umbriel_welcome.wav'),
                        help='for a cloning model: the voice to copy')
    parser.add_argument('--language', help='override the tag XTTS is given')
    parser.add_argument('--token', help='a Hugging Face read token, for a '
                                        'gated model')
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--probe', nargs='?', const='welcome',
                        help='one line, four ways, to find what is spoiling it')
    parser.add_argument('--vowels', action='store_true',
                        help='keep the harakat (off by default for this model)')
    parser.add_argument('--fresh', action='store_true',
                        help='rebuild the lines from the manifest, delete the '
                             'clips of the last voice, and speak them all again')
    parser.add_argument('--plain', action='store_true',
                        help='write the model\'s own sound, unshaped — pick this\n'
                             'if the plain sample from --sample was the better one')
    parser.add_argument('--no-speed', action='store_true',
                        help='do not ask XTTS to change pace')
    parser.add_argument('--notebook', action='store_true',
                        help='print the author\'s own inference notebook')
    parser.add_argument('--check', action='store_true',
                        help='print the repository and what the config needs')
    parser.add_argument('--sample', nargs='?', const='welcome')
    args = parser.parse_args()
    if args.probe:
        do_probe(args)
    elif args.notebook:
        do_notebook(args)
    elif args.check:
        preflight()
        do_check(args)
    elif args.sample:
        do_sample(args)
    elif args.fresh:
        do_fresh(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
