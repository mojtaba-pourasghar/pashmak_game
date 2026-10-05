#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks every line with the male1 Persian VITS voice, through sherpa-onnx.

This is the voice chosen by ear from the eight demo clips, and --check found
something better than expected about it: the folder is not a Coqui checkpoint.

    persian-tts-male1-vits-coqui/model.onnx    114.3 MB
    persian-tts-male1-vits-coqui/tokens.txt
    persian-tts-male1-vits-coqui/test.wav

"coqui" is where the voice was trained; what is published is the ONNX export.
That means no torch, no coqui-tts, no 2 GB of dependencies — one wheel that runs
the model on the processor:

    python -m pip install sherpa-onnx espeakng-loader numpy scipy
    python tools/gen_voice_sherpa.py --check     # the vocabulary, and the text against it
    python tools/gen_voice_sherpa.py --sample    # one line, plain and shaped
    python tools/gen_voice_sherpa.py --fresh     # all 1,136 into res/raw

--check is the one to run first, and it earned its keep immediately: this voice
turned out to have 152 tokens of IPA, not Persian letters. That one fact decides
everything. A letter model takes the text as it is; a phoneme model needs
espeak-ng's data beside it and, without it, produces silence rather than an
error. The data is found by itself in any of four places, and the espeakng-loader
package above is the one that needs no administrator — it carries a complete
copy, Persian voice included. Then it holds every character of the app's 1,136 lines against that
vocabulary and names the ones the model has no token for, which is the difference
between knowing and finding out after an hour of synthesis.
"""
import argparse
import io
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import PACE, mood_for, tally                # noqa: E402
from voice_text import HARAKAT, bare                         # noqa: E402
import voice_output                                          # noqa: E402


def _utf8_console():
    """Let Persian reach a Windows console, which defaults to cp1252."""
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding='utf-8', errors='replace')
        except (AttributeError, ValueError):
            pass


_utf8_console()

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.normpath(os.path.join(ROOT, 'app/src/main/res/raw'))
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
LEDGER = os.path.join(ROOT, 'tools/voice_done_sherpa.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
VOICES = os.path.join(ROOT, 'tools/voices')
MODEL = 'karim23657/persian-tts-vits'
FOLDER = 'persian-tts-male1-vits-coqui'
WANTED = ('model.onnx', 'tokens.txt')
MIN_BYTES = 900

# Plain Persian, no vowel marks, for the first listen. This is the welcome line
# as a person would write it, which is what this model was trained on.
PLAIN = 'سلام قند عسلم! من پشمکم، دوست جدیدت! آماده‌ای باهم کلی بازی کنیم؟'

# Where espeak-ng's data can be found, best first. The pip package carries a
# complete copy, Persian voice included, and needs no administrator and no PATH,
# which is why it is tried before the installed program.
ESPEAK_ENV = ('ESPEAK_DATA_PATH', 'ESPEAKNG_DATA_PATH', 'PHONEMIZER_ESPEAK_DATA')
ESPEAK_GUESSES = (
    r'C:\Program Files\eSpeak NG\espeak-ng-data',
    r'C:\Program Files (x86)\eSpeak NG\espeak-ng-data',
    '/usr/share/espeak-ng-data',
    '/usr/local/share/espeak-ng-data',
)
# The Persian voice inside that folder. espeak keeps Iranian languages under ira.
FARSI_VOICE = os.path.join('lang', 'ira', 'fa')


def need(module, why, install=None):
    try:
        return __import__(module)
    except ImportError as problem:
        sys.exit('%s is needed (%s).\n  %s\n\n  %s -m pip install %s'
                 % (module, why, ' '.join(str(problem).split())[:160],
                    sys.executable, install or module.replace('_', '-')))


def home(args):
    return os.path.join(VOICES, args.folder or 'voice')


def grab(args):
    """The two files that matter, fetched once into tools/voices/<folder>/."""
    folder = home(args)
    if not os.path.isdir(folder):
        os.makedirs(folder)
    paths = {}
    for name in WANTED:
        local = os.path.join(folder, name)
        if not os.path.exists(local):
            hub = need('huggingface_hub', 'the voice is published there')
            inside = '%s/%s' % (args.folder, name) if args.folder else name
            print('fetching %s' % inside, flush=True)
            got = hub.hf_hub_download(args.model, inside, token=args.token)
            with io.open(got, 'rb') as source, io.open(local, 'wb') as sink:
                sink.write(source.read())
        paths[name] = local
    return paths


def tokens_of(path):
    """The vocabulary, as the set of symbols the model has a token for."""
    symbols = set()
    for row in io.open(path, encoding='utf-8'):
        if not row.strip():
            continue
        # Each line is "<symbol> <id>", and the symbol may itself be a space.
        symbols.add(row.rstrip('\n').rsplit(' ', 1)[0])
    return symbols


PERSIAN = set('ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهیءآأإئؤةيك')


def phonemic(symbols):
    """Whether this vocabulary is phonemes rather than Persian letters.

    A model exported from a Coqui recipe that phonemised with espeak has IPA in
    its tokens. Handed Persian letters it has no token for any of them, which is
    not an error anywhere — it is a clip of silence, 1,136 times over.
    """
    return len(PERSIAN & symbols) < 8


def looks_like_espeak(folder):
    """Whether this folder really is espeak's data, and knows Persian.

    Pointing at the wrong folder is not an error either — it is the same silence
    as having none, so the two files that must be in a real one are checked: the
    phoneme tables, and the Persian voice itself.
    """
    if not folder or not os.path.isdir(folder):
        return False
    return (os.path.exists(os.path.join(folder, 'phontab'))
            and os.path.exists(os.path.join(folder, FARSI_VOICE)))


def found_espeak():
    """Every place worth looking, in order, with what it was."""
    for key in ESPEAK_ENV:
        if os.environ.get(key):
            yield os.environ[key], key
    try:
        import espeakng_loader
        yield str(espeakng_loader.get_data_path()), 'the espeakng-loader package'
    except Exception:                                      # noqa: BLE001
        pass
    try:
        import piper_phonemize
        yield (os.path.join(os.path.dirname(piper_phonemize.__file__),
                            'espeak-ng-data'), 'the piper-phonemize package')
    except Exception:                                      # noqa: BLE001
        pass
    for guess in ESPEAK_GUESSES:
        yield guess, 'the installed espeak-ng'


NEED_ESPEAK = (
    'this model speaks phonemes, so it needs espeak-ng\'s data folder — the\n'
    'phoneme tables and the Persian voice — and nothing here has one.\n'
    '\nThe easiest way needs no administrator and no PATH: a pip package carries\n'
    'a complete copy, Persian included.\n'
    '\n  %s -m pip install espeakng-loader\n'
    '\nThen run the same command again; it is found by itself. An installed\n'
    'espeak-ng works too (winget install espeak-ng), and so does pointing at any\n'
    'copy of the folder:\n'
    '\n  --data-dir "C:\\Program Files\\eSpeak NG\\espeak-ng-data"\n'
    '\nWithout it the model is handed characters it has no token for, and the\n'
    'result is silence rather than a complaint.')


def espeak_data(args, symbols):
    """The data folder this model needs, or nothing when it needs none."""
    if not phonemic(symbols):
        return ''
    if args.data_dir:
        if not looks_like_espeak(args.data_dir):
            sys.exit('%s is not espeak-ng data: a real one holds phontab and %s.'
                     % (args.data_dir, FARSI_VOICE))
        return args.data_dir
    for folder, source in found_espeak():
        if looks_like_espeak(folder):
            print('espeak data : %s\n              (%s)' % (folder, source))
            return folder
    sys.exit(NEED_ESPEAK % sys.executable)


def engine(args):
    sherpa = need('sherpa_onnx', 'it runs the ONNX voice', 'sherpa-onnx')
    paths = grab(args)
    symbols = tokens_of(paths['tokens.txt'])
    vits = sherpa.OfflineTtsVitsModelConfig(
        model=paths['model.onnx'],
        tokens=paths['tokens.txt'],
        data_dir=espeak_data(args, symbols),
        length_scale=1.0)
    config = sherpa.OfflineTtsConfig(
        model=sherpa.OfflineTtsModelConfig(vits=vits, num_threads=args.threads,
                                           provider='cpu'),
        # One sentence at a time. The lines are short and this keeps the pace
        # asked for from being applied to a batch instead of a line.
        max_num_sentences=1)
    config.validate()
    try:
        return sherpa.OfflineTts(config)
    except Exception as problem:                           # noqa: BLE001
        text = ' '.join(str(problem).split())
        hint = ''
        if 'data_dir' in text or 'espeak' in text.lower():
            hint = '\n\n' + NEED_ESPEAK % sys.executable
        elif 'voice' in text.lower() or 'language' in text.lower():
            hint = ('\n\n  The model has to say which language it was trained on,'
                    ' in its own\n  metadata, for espeak to phonemise for it. If'
                    ' it does not, this export\n  cannot be driven this way — send'
                    ' me the message above.')
        sys.exit('sherpa-onnx would not load the voice:\n  %s%s' % (text[:400], hint))


def speak(tts, text, pace, vowels=False):
    """One line. sherpa's speed is the pace the other way up: larger is faster."""
    said = tts.generate(text if vowels else bare(text), sid=0, speed=1.0 / pace)
    return said.samples, said.sample_rate


def read_ledger():
    if not os.path.exists(LEDGER):
        return {'done': []}
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


def do_check(args):
    paths = grab(args)
    print('%s/%s' % (args.model, args.folder or ''))
    for name in WANTED:
        print('  %8.1f MB  %s' % (os.path.getsize(paths[name]) / 1e6, name))

    symbols = tokens_of(paths['tokens.txt'])
    print('\n%d tokens. %s' % (len(symbols),
          'IPA phonemes — espeak-ng data needed' if phonemic(symbols)
          else 'Persian letters — the text goes in as it is'))
    print('  %s' % ' '.join(sorted(s for s in symbols if s.strip())[:48]))

    lines = load_lines()
    used = set()
    for text in lines.values():
        used |= set(bare(text))
    if not phonemic(symbols):
        absent = sorted(c for c in used if c not in symbols and c.strip())
        print('\nthe app uses %d distinct characters; %d have no token:\n  %s'
              % (len(used), len(absent), ' '.join(absent) or '(none)'))
        if absent:
            print('  Those are either read as nothing or guessed at. Punctuation\n'
                  '  missing from the list is only a pause lost; a letter missing\n'
                  '  from it is a word said wrong.')
    else:
        print('\nthe text is phonemised by espeak, so its characters are not\n'
              'matched against these tokens.')

    tts = engine(args)
    print('\nit loads: %d Hz, %d speaker(s)' % (tts.sample_rate, tts.num_speakers))


def do_sample(args):
    tts = engine(args)
    lines = load_lines()
    if args.sample not in lines:
        sys.exit('no line called %s' % args.sample)
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    import voice_shape as shaping
    mood = mood_for(args.sample)
    print('line : %s   (mood %s, pace %.2f)' % (args.sample, mood, PACE[mood]))
    print('text : %s\n' % lines[args.sample])
    samples, rate = speak(tts, lines[args.sample], PACE[mood], args.vowels)
    for tag, shaped in (('plain', False), ('shaped', True)):
        path = os.path.join(SAMPLES, 'sherpa_%s.ogg' % tag)
        final, size, pcm = voice_output.write_clip(samples, rate, path, shaped)
        print('  %-7s %5.2f s  %3.0f Hz  %6.1f KB  %s'
              % (tag, len(pcm) / float(rate), shaping.fundamental(pcm, rate),
                 size / 1024.0, os.path.relpath(final, ROOT)))
    print('\na grown man is near 110 Hz, a small child near 280.')


def do_text(args):
    """Speak a sentence given on the command line, and nothing else.

    Judging a voice from the app's own lines means judging two things at once,
    because those lines are vowelised and this model never saw a vowel mark. A
    plain sentence typed in separates them: if this sounds right, the voice is
    right and only the text needs work.
    """
    tts = engine(args)
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    import voice_shape as shaping
    text = args.text if isinstance(args.text, str) and args.text.strip() else PLAIN
    marks = sum(1 for c in text if c in HARAKAT)
    print('text : %s' % text)
    print('       %d characters%s, pace %.2f'
          % (len(text), ', %d vowel marks (removed)' % marks if marks else
             ', plain already', args.pace))
    samples, rate = speak(tts, text, args.pace, args.vowels)
    for tag, shaped in (('plain', False), ('shaped', True)):
        path = os.path.join(SAMPLES, 'sherpa_text_%s.ogg' % tag)
        final, size, pcm = voice_output.write_clip(samples, rate, path, shaped)
        print('  %-7s %5.2f s  %3.0f Hz  %6.1f KB  %s'
              % (tag, len(pcm) / float(rate), shaping.fundamental(pcm, rate),
                 size / 1024.0, os.path.relpath(final, ROOT)))
    print('\na grown man is near 110 Hz, a small child near 280.')


def do_fresh(args):
    """Start the whole set again, checking everything before deleting anything.

    A clip missing from res/raw is worse than a silence: the Java refers to each
    one by name, so it is a compile error. Nothing is removed until the encoder
    has been proved and the model has loaded.
    """
    voice_output.check_encoder(os.path.join(VOICES, 'check'))
    tts = engine(args)
    voice_output.rebuild_lines(ROOT)
    voice_output.clear_spoken(RAW, load_lines())
    if os.path.exists(LEDGER):
        os.unlink(LEDGER)
    do_all(args, tts)


def do_all(args, tts=None):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    voice_output.check_encoder(os.path.join(VOICES, 'check'))
    ledger = read_ledger()
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do'
          % (len(names), len(names) - len(todo), len(todo)))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %4d   pace %.2f' % (mood, count, PACE[mood]))
    print()
    tts = tts or engine(args)
    done = failed = 0
    for i, name in enumerate(todo, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            samples, rate = speak(tts, lines[name], PACE[mood_for(name)],
                                  args.vowels)
            staged = path + '.part'
            final, size, _pcm = voice_output.write_clip(
                samples, rate, staged, shaped=not args.plain)
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
    parser.add_argument('--folder', default=FOLDER,
                        help='the voice\'s folder inside that repository')
    parser.add_argument('--token', help='a Hugging Face token, if needed')
    parser.add_argument('--data-dir', dest='data_dir',
                        help='espeak-ng-data, for a model that speaks phonemes')
    parser.add_argument('--threads', type=int, default=4)
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--vowels', action='store_true',
                        help='keep the harakat (off by default)')
    parser.add_argument('--plain', action='store_true',
                        help='the model\'s own sound, unshaped')
    parser.add_argument('--check', action='store_true',
                        help='the vocabulary, and the app\'s text against it')
    parser.add_argument('--sample', nargs='?', const='welcome')
    parser.add_argument('--text', nargs='?', const=PLAIN,
                        help='speak this sentence instead of a line of the app; '
                             'with no sentence, a plain Persian one')
    parser.add_argument('--pace', type=float, default=1.08,
                        help='length scale: larger is slower (default 1.08, the '
                             'pace the mascot talks at in a game)')
    parser.add_argument('--fresh', action='store_true',
                        help='rebuild the lines from the manifest, delete the '
                             'clips of the last voice, and speak them all again')
    args = parser.parse_args()
    if args.check:
        do_check(args)
    elif args.text:
        do_text(args)
    elif args.sample:
        do_sample(args)
    elif args.fresh:
        do_fresh(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
