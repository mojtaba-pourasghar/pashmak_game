#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Speaks the app's lines by calling a Hugging Face Space, instead of running a model.

The lightest route there is. Nothing is downloaded and nothing is installed beyond
a small client: the model runs on Hugging Face's hardware and hands back audio.
After torch that would not load, a 20 GB fetch and a 5.6 GB checkpoint, that is
worth something.

    python3 -m pip install gradio_client
    python3 tools/gen_voice_space.py --api        # what the Space actually accepts
    python3 tools/gen_voice_space.py --sample
    python3 tools/gen_voice_space.py             # all of them into res/raw

--api comes first and is not optional advice. A Space is somebody's Gradio app,
its endpoint and parameter names are whatever they chose, and they are readable
rather than guessable. Everything after it is filled in from what it prints.

One courtesy: a free Space is somebody's hosting bill and usually one GPU shared
by everyone. Sampling from it is what it is for; firing 1,136 requests at it is
not. The full run pauses between lines by default, and if the voice turns out to
be the one, the decent thing is to run its model locally for the bulk and leave
the Space to the people trying it out.
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
LEDGER = os.path.join(ROOT, 'tools/voice_done_space.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
SPACE = 'MaziarAshtari/Persian-TTS-Comparison'
VOICE = 'Chatterbox-TTS-Persian-Farsi'
MIN_BYTES = 900


def client(args):
    try:
        from gradio_client import Client
    except ImportError:
        sys.exit('%s -m pip install gradio_client' % sys.executable)
    # gradio_client has renamed the token argument between majors — 1.x takes
    # hf_token, 2.x does not — so the signature is read rather than guessed.
    extra = {}
    if args.token:
        import inspect
        accepted = inspect.signature(Client.__init__).parameters
        for name in ('hf_token', 'token', 'auth'):
            if name in accepted:
                extra[name] = args.token
                break
        else:
            print('this gradio_client takes no token argument (%s); continuing '
                  'without one' % ', '.join(sorted(accepted))[:120], file=sys.stderr)
    try:
        return Client(args.space, **extra)
    except Exception as problem:                           # noqa: BLE001
        text = ' '.join(str(problem).split())
        if 'sleep' in text.lower() or 'not found' in text.lower():
            sys.exit('%s would not open: %s\n\nA Space that has gone to sleep wakes '
                     'when you open its page in a browser. Try that, wait for it to '
                     'load, then run this again.' % (args.space, text[:200]))
        sys.exit('%s would not open: %s' % (args.space, text[:300]))


def do_api(args):
    """Print what the Space accepts. Everything else is built on this."""
    link = client(args)
    print('--- %s ---\n' % args.space)
    try:
        print(link.view_api(return_format='str', print_info=False))
    except TypeError:
        link.view_api()
    print('\nSend me that. The endpoint name and the parameter order are whatever\n'
          'its author chose, and the voice is probably a dropdown — I need to see\n'
          'the exact spelling of «%s» among its choices.' % VOICE)


def speak(link, args, text):
    """One line. The call shape is deliberately overridable from the command line."""
    kwargs = {}
    if args.text_arg:
        kwargs[args.text_arg] = text
    if args.voice_arg:
        kwargs[args.voice_arg] = args.voice
    if kwargs:
        return link.predict(api_name=args.endpoint, **kwargs)
    # Positional, in the order --order names: t for the text, v for the voice.
    values = [text if slot == 't' else args.voice for slot in args.order]
    return link.predict(*values, api_name=args.endpoint)


def collect(result):
    """A Gradio audio output is a path, or something holding one."""
    if isinstance(result, (list, tuple)):
        for item in result:
            found = collect(item)
            if found:
                return found
        return None
    if isinstance(result, dict):
        for key in ('path', 'name', 'value', 'url'):
            if result.get(key):
                return collect(result[key])
        return None
    if isinstance(result, str) and os.path.exists(result):
        return result
    return None


def write(source, path):
    """Into the ogg the app plays, or the wav itself when there is no encoder."""
    if shutil.which('oggenc'):
        subprocess.check_call(['oggenc', '-Q', '-q', '4', '-o', path, source])
        return path
    final = os.path.splitext(path)[0] + os.path.splitext(source)[1]
    shutil.copyfile(source, final)
    return final


def read_ledger():
    if not os.path.exists(LEDGER):
        return {'space': SPACE, 'voice': VOICE, 'done': []}
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
    link = client(args)
    print('space : %s\nvoice : %s\nline  : %s\ntext  : %s\n'
          % (args.space, args.voice, args.sample, lines[args.sample]))
    try:
        result = speak(link, args, lines[args.sample])
    except Exception as problem:                           # noqa: BLE001
        sys.exit('the call failed: %s\n\nRun --api and send me what it prints — the\n'
                 'endpoint or the argument names are probably not what I guessed.'
                 % ' '.join(str(problem).split())[:300])
    audio = collect(result)
    if not audio:
        sys.exit('no audio came back. What it returned was:\n  %s\n\nRun --api so I '
                 'can see which output is the sound.' % repr(result)[:300])
    final = write(audio, os.path.join(SAMPLES, 'space_%s.ogg' % args.sample))
    print('wrote %s  (%.1f KB)' % (os.path.relpath(final, ROOT),
                                   os.path.getsize(final) / 1024.0))


def do_all(args):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    ledger = read_ledger()
    spoken = set() if args.force else set(ledger['done'])
    todo = [n for n in names if n not in spoken]
    print('%d lines, %d already spoken, %d to do, %.1fs between each'
          % (len(names), len(names) - len(todo), len(todo), args.pause))
    for mood, count in sorted(tally(todo).items(), key=lambda kv: -kv[1]):
        print('   %-10s %4d' % (mood, count))
    print()
    link = client(args)
    import time
    done = failed = 0
    for i, name in enumerate(todo, 1):
        try:
            audio = collect(speak(link, args, lines[name]))
            if not audio:
                raise RuntimeError('no audio came back')
            final = write(audio, os.path.join(RAW, name + '.ogg'))
            if os.path.getsize(final) < MIN_BYTES:
                raise RuntimeError('almost nothing came back')
            done += 1
            ledger['done'].append(name)
            write_ledger(ledger)
            if i % 20 == 0 or i == len(todo):
                print('[%4d/%4d] %-28s %-10s %6.1f KB'
                      % (i, len(todo), name, mood_for(name),
                         os.path.getsize(final) / 1024.0), flush=True)
        except Exception as problem:                       # noqa: BLE001
            failed += 1
            print('[%4d/%4d] %-28s FAILED %s'
                  % (i, len(todo), name, ' '.join(str(problem).split())[:110]),
                  flush=True)
            if failed >= 5 and done == 0:
                sys.exit('five failures and nothing written — run --api')
        if args.pause:
            time.sleep(args.pause)
    print('\nwrote %d, failed %d, %d of %d in all'
          % (done, failed, len(ledger['done']), len(names)))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--space', default=SPACE)
    parser.add_argument('--voice', default=VOICE,
                        help='the voice as the Space spells it')
    parser.add_argument('--endpoint', default='/predict',
                        help='the api_name --api printed')
    parser.add_argument('--text-arg', help='keyword for the text, if it takes one')
    parser.add_argument('--voice-arg', help='keyword for the voice choice')
    parser.add_argument('--order', default='tv',
                        help='positional order: t for text, v for voice')
    parser.add_argument('--token', help='a Hugging Face token, for a private Space')
    parser.add_argument('--pause', type=float, default=1.5,
                        help='seconds between lines; a free Space is shared')
    parser.add_argument('--only')
    parser.add_argument('--force', action='store_true')
    parser.add_argument('--api', action='store_true',
                        help='print what the Space accepts, and stop')
    parser.add_argument('--sample', nargs='?', const='welcome')
    args = parser.parse_args()
    if args.api:
        do_api(args)
    elif args.sample:
        do_sample(args)
    else:
        do_all(args)


if __name__ == '__main__':
    main()
