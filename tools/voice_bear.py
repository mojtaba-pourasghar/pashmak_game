#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Pashmak's voice, made here, with nothing but what is on this machine.

No service, no key, no quota. espeak-ng drives MBROLA's ir1 Farsi database,
which is not a buzzer approximating speech but a recording of a real person cut
into diphones and re-joined — so the grain of the voice is human. espeak on its
own formant-synthesises, and that is the thin electronic sound the app ships
today.

What espeak hands back is still an adult man reading briskly, which is not a
small bear talking to a four-year-old. So the rest of this file is the part that
makes it one, and it is plain arithmetic over the samples — no numpy, because
there is none here:

  warmth      a one-pole low shelf lifting what is under ~260 Hz, which is the
              chest of the voice, and a gentle roll-off above ~7 kHz, which is
              where the diphone joins hiss.
  body        the whole thing resampled slightly long, dropping the formants a
              little. A bear is round; round things have lower formants. The
              pitch that this drops with it is given back in espeak's -p.
  evenness    soft-knee compression, so the loud syllables do not startle and
              the quiet ones do not disappear under a car going past.
  level       normalised to a fixed peak, so no line in the app is louder than
              any other line.
  edges       an eight-millisecond fade each end, because a waveform that starts
              at full amplitude clicks.

Nothing here is a matter of taste that cannot be checked: --measure prints the
fundamental the result actually came out at, found by autocorrelation. An adult
man sits near 110 Hz and a small child near 280. Pashmak should land in the
middle — warm, not deep, and nowhere near a chipmunk.

    python3 tools/voice_bear.py --sample                 # one line, five settings
    python3 tools/voice_bear.py                          # all 1,136 into res/raw
    python3 tools/voice_bear.py --only tale_ --measure
"""
import argparse
import array
import io
import json
import math
import os
import struct
import subprocess
import sys
import tempfile
import wave

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from voice_moods import mood_for, tally               # noqa: E402
from build_ipa_lines import degeminate                # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, 'app/src/main/res/raw')
LINES = os.path.join(ROOT, 'tools/all_game_lines.json')
SAMPLES = os.path.join(ROOT, 'tools/voice_samples')
MIN_BYTES = 900

# espeak's own voice, kept as a fallback for a machine without MBROLA.
FALLBACK = 'fa'
MBROLA = 'mb-ir1'

# How each mood is read. Pitch and speed go to espeak; 'body' is how much the
# result is stretched afterwards, which lowers the formants; 'gap' is the pause
# espeak leaves between words, in units of 10 ms.
MOODS = {
    'delighted': dict(pitch=99, speed=158, gap=2, body=0.96),
    'kind':      dict(pitch=94, speed=138, gap=4, body=0.98),
    'bedtime':   dict(pitch=90, speed=124, gap=6, body=0.99),
    'glyph':     dict(pitch=96, speed=112, gap=8, body=0.97),
    'story':     dict(pitch=95, speed=142, gap=4, body=0.98),
    'game':      dict(pitch=99, speed=150, gap=3, body=0.97),
}


def polish(text):
    """Small repairs so the reading is of Persian and not of its typography."""
    t = text.strip()
    t = t.replace('ي', 'ی').replace('ك', 'ک')       # the Arabic forms of two letters
    # A breath where someone starts speaking, so quoted dialogue does not run on.
    for verb in ('گفت:', 'پرسید:', 'گفتن:', 'می‌گفت:'):
        t = t.replace(verb, verb[:-1] + '، ')
    t = t.replace('‌', ' ')                      # the half-space is silent
    # espeak says the *name* of a shadda instead of doubling the consonant,
    # so «غُصِّه» comes out as "ghosse-tashdid". 76 lines carry one.
    t = degeminate(t)
    # «!» read flat by espeak; a comma before it gives the sentence somewhere to
    # lift, which is most of what makes a greeting sound pleased.
    return ' '.join(t.split())


# --- the arithmetic -----------------------------------------------------------

def read_wav(path):
    with wave.open(path, 'rb') as w:
        if w.getsampwidth() != 2 or w.getnchannels() != 1:
            raise RuntimeError('expected 16-bit mono from espeak')
        rate = w.getframerate()
        data = array.array('h')
        data.frombytes(w.readframes(w.getnframes()))
    return data, rate


def low_passed(samples, rate, hz):
    """One-pole. Cheap, and its gentle slope is exactly what is wanted here."""
    a = math.exp(-2.0 * math.pi * hz / rate)
    out = array.array('d', bytes(8 * len(samples)))
    running = 0.0
    for i, s in enumerate(samples):
        running = (1.0 - a) * s + a * running
        out[i] = running
    return out


def warm(samples, rate, shelf_hz=260.0, lift=0.55, top_hz=7000.0):
    """Lift the chest of the voice and take the fizz off the top."""
    body = low_passed(samples, rate, shelf_hz)
    mixed = array.array('d', bytes(8 * len(samples)))
    for i, s in enumerate(samples):
        mixed[i] = s + lift * body[i]
    # The roll-off is a one-pole blended back, so it softens rather than muffles.
    top = low_passed(mixed, rate, top_hz)
    for i in range(len(mixed)):
        mixed[i] = 0.75 * top[i] + 0.25 * mixed[i]
    return mixed


def stretch(samples, factor):
    """Linear resample. Longer means lower: formants and pitch both come down."""
    if abs(factor - 1.0) < 1e-6:
        return samples
    out_len = int(len(samples) * factor)
    out = array.array('d', bytes(8 * out_len))
    step = (len(samples) - 1) / float(out_len - 1)
    for i in range(out_len):
        x = i * step
        j = int(x)
        frac = x - j
        k = j + 1 if j + 1 < len(samples) else j
        out[i] = samples[j] * (1.0 - frac) + samples[k] * frac
    return out


def compress(samples, threshold=0.42, ratio=3.0):
    """Soft knee, so a shout and a whisper end up within reach of each other."""
    peak = max((abs(s) for s in samples), default=1.0) or 1.0
    out = array.array('d', bytes(8 * len(samples)))
    for i, s in enumerate(samples):
        x = s / peak
        m = abs(x)
        if m > threshold:
            m = threshold + (m - threshold) / ratio
        out[i] = math.copysign(m, x)
    return out


def finish(samples, rate, peak=0.89, fade_ms=8.0):
    """Normalise, fade the ends, and come back to 16-bit."""
    top = max((abs(s) for s in samples), default=1.0) or 1.0
    gain = peak / top
    fade = max(1, int(rate * fade_ms / 1000.0))
    out = array.array('h', bytes(2 * len(samples)))
    last = len(samples) - 1
    for i, s in enumerate(samples):
        v = s * gain
        if i < fade:
            v *= i / float(fade)
        elif i > last - fade:
            v *= (last - i) / float(fade)
        out[i] = int(max(-32768, min(32767, round(v * 32767))))
    return out


def fundamental(samples, rate, low=70.0, high=400.0):
    """The pitch the result actually came out at, by autocorrelation.

    Voiced frames only — silence and consonants have no pitch to find, and
    letting them vote drags the answer wherever the noise happens to sit.
    """
    frame = int(rate * 0.04)
    hop = int(rate * 0.02)
    lo, hi = int(rate / high), int(rate / low)
    found = []
    for start in range(0, max(0, len(samples) - frame), hop):
        window = samples[start:start + frame]
        energy = sum(s * s for s in window) / float(frame)
        if energy < 2.0e6:                             # not speech, or not voiced
            continue
        best, best_lag = 0.0, 0
        for lag in range(lo, min(hi, frame - 1)):
            total = 0.0
            for i in range(0, frame - lag, 2):         # every other sample is plenty
                total += window[i] * window[i + lag]
            if total > best:
                best, best_lag = total, lag
        if best_lag:
            found.append(rate / float(best_lag))
    if not found:
        return 0.0
    found.sort()
    return found[len(found) // 2]


# --- putting one line together ------------------------------------------------

def say(text, mood, voice, path_wav):
    how = MOODS[mood]
    subprocess.check_call(
        ['espeak-ng', '-v', voice, '-p', str(how['pitch']), '-s', str(how['speed']),
         '-g', str(how['gap']), '-w', path_wav, polish(text)],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def render(text, mood, voice, measure=False):
    """From words to finished 16-bit samples."""
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as scratch:
        wav = scratch.name
    try:
        say(text, mood, voice, wav)
        samples, rate = read_wav(wav)
    finally:
        os.unlink(wav)
    if not len(samples):
        raise RuntimeError('espeak produced no audio')
    worked = warm(samples, rate)
    worked = stretch(worked, MOODS[mood]['body'])
    worked = compress(worked)
    done = finish(worked, rate)
    pitch = fundamental(done, rate) if measure else 0.0
    return done, rate, pitch


def to_ogg(samples, rate, path):
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as scratch:
        wav = scratch.name
    try:
        with wave.open(wav, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(rate)
            w.writeframes(samples.tobytes())
        subprocess.check_call(['oggenc', '-Q', '-q', '3', '-o', path, wav])
    finally:
        os.unlink(wav)


def write_clip(text, mood, voice, path, measure=False):
    samples, rate, pitch = render(text, mood, voice, measure)
    staged = path + '.part'
    try:
        to_ogg(samples, rate, staged)
        if os.path.getsize(staged) < MIN_BYTES:
            raise RuntimeError('encoded to almost nothing')
        os.replace(staged, path)
    finally:
        if os.path.exists(staged):
            os.unlink(staged)
    return os.path.getsize(path), len(samples) / float(rate), pitch


# --- the run ------------------------------------------------------------------

def pick_voice(preferred):
    if preferred:
        return preferred
    probe = subprocess.run(['espeak-ng', '-v', MBROLA, '--stdout', 'a'],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if probe.returncode == 0:
        return MBROLA
    print('MBROLA is not here (apt-get install mbrola mbrola-ir1); '
          'falling back to espeak\'s own Persian, which sounds thinner')
    return FALLBACK


def load_lines():
    if not os.path.exists(LINES):
        sys.exit('run tools/build_voice_lines.py first')
    return json.load(io.open(LINES, encoding='utf-8'))


def do_sample(args, voice):
    """One line, five readings around the target, so it can be chosen by ear."""
    lines = load_lines()
    name = args.sample
    if name not in lines:
        sys.exit('no line called %s' % name)
    if not os.path.isdir(SAMPLES):
        os.makedirs(SAMPLES)
    mood = mood_for(name)
    base = dict(MOODS[mood])
    tries = [
        ('a_deeper',   dict(base, pitch=84, body=1.02)),
        ('b_warm',     dict(base, pitch=92, body=0.99)),
        ('c_middle',   dict(base)),
        ('d_smaller',  dict(base, pitch=99, speed=base['speed'] + 8, body=0.93)),
        ('e_slower',   dict(base, pitch=96, speed=base['speed'] - 18, gap=5,
                            body=0.98)),
    ]
    print('line  : %s   (mood %s)' % (name, mood))
    print('text  : %s' % lines[name])
    print('voice : %s\n' % voice)
    print('  %-11s %-6s %-6s %-5s %8s %7s' %
          ('', 'pitch', 'speed', 'body', 'length', 'F0'))
    keep = MOODS[mood]
    for tag, how in tries:
        MOODS[mood] = how
        path = os.path.join(SAMPLES, '%s__%s.ogg' % (name, tag))
        try:
            size, length, pitch = write_clip(lines[name], mood, voice, path, True)
            print('  %-11s %-6d %-6d %-5.2f %7.2fs %6.0f Hz   %s'
                  % (tag, how['pitch'], how['speed'], how['body'], length, pitch,
                     os.path.relpath(path, ROOT)))
        except (RuntimeError, subprocess.CalledProcessError) as problem:
            print('  %-11s FAILED  %s' % (tag, problem))
    MOODS[mood] = keep
    print('\nan adult man sits near 110 Hz and a small child near 280;')
    print('tell me the letter that sounds like Pashmak.')


def do_all(args, voice):
    lines = load_lines()
    names = sorted(lines)
    if args.only:
        names = [n for n in names if n.startswith(args.only)]
        if not names:
            sys.exit('nothing starts with %r' % args.only)
    if not os.path.isdir(RAW):
        sys.exit('no res/raw at %s' % RAW)
    print('%d lines, voice %s' % (len(names), voice))
    for mood, count in sorted(tally(names).items(), key=lambda kv: -kv[1]):
        print('   %-10s %d' % (mood, count))
    print()
    done = failed = 0
    for i, name in enumerate(names, 1):
        path = os.path.join(RAW, name + '.ogg')
        try:
            size, length, pitch = write_clip(lines[name], mood_for(name), voice,
                                             path, args.measure)
            done += 1
            if i % 25 == 0 or i == len(names):
                print('[%4d/%4d] %-28s %5.2fs %s'
                      % (i, len(names), name, length,
                         '%3.0f Hz' % pitch if args.measure else ''))
        except (RuntimeError, subprocess.CalledProcessError) as problem:
            failed += 1
            print('[%4d/%4d] %-28s FAILED %s' % (i, len(names), name, problem))
    print('\nwrote %d, failed %d' % (done, failed))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--voice', help='espeak voice; default mb-ir1')
    parser.add_argument('--only', help='only clips whose name starts with this')
    parser.add_argument('--sample', nargs='?', const='welcome',
                        help='render one line five ways and stop')
    parser.add_argument('--measure', action='store_true',
                        help='report the fundamental each clip came out at')
    args = parser.parse_args()
    voice = pick_voice(args.voice)
    if args.sample:
        do_sample(args, voice)
    else:
        do_all(args, voice)


if __name__ == '__main__':
    main()
