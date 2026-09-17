# -*- coding: utf-8 -*-
"""Three background loops, so the app is never silent on a fresh install.

MusicEngine can synthesise a loop at runtime, but that is code that has to work on
every device, and a file is a file. These are the default; anything dropped into
res/raw under the same name replaces them, which is how a real piece of music gets
in later without touching any code.

They are written to loop without a seam: a whole number of bars, every note faded
out before the bar ends, and the drone crossfaded into itself at the join.
"""
import array, math, os, subprocess, sys, wave

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, 'app/src/main/res/raw')
RATE = 22050

# A pentatonic scale has no semitone clashes, so any two notes sound fine together
# and a phrase can wander without ever going sour. C D E G A, two octaves.
SCALE = [261.63, 293.66, 329.63, 392.00, 440.00,
         523.25, 587.33, 659.25, 784.00, 880.00]


def bell(freq, seconds, volume):
    """One soft note that has fully died away by the time the next bar starts."""
    n = int(seconds * RATE)
    out = [0.0] * n
    for i in range(n):
        t = i / n
        envelope = min(1.0, t * 30) * math.exp(-4.2 * t)
        # A touch of the octave above gives it a glassy edge without a sharp attack.
        out[i] = (math.sin(2 * math.pi * freq * i / RATE)
                  + 0.28 * math.sin(4 * math.pi * freq * i / RATE)) * envelope * volume
    return out


def drone(freq, seconds, volume):
    """A held low note under everything, faded in and out so the loop joins."""
    n = int(seconds * RATE)
    out = [0.0] * n
    edge = int(0.8 * RATE)
    for i in range(n):
        value = (math.sin(2 * math.pi * freq * i / RATE)
                 + 0.4 * math.sin(2 * math.pi * freq * 1.5 * i / RATE))
        fade = 1.0
        if i < edge:
            fade = i / edge
        elif i > n - edge:
            fade = (n - i) / edge
        out[i] = value * fade * volume
    return out


# How far the last notes are allowed to ring past the end of the loop before being
# folded back over the beginning. This is what makes the join seamless: when the file
# repeats, the tail is already sounding underneath the first note, exactly as it would
# if the music simply carried on.
RING = 1.2


def render(phrase, note_seconds, bars, root, brightness):
    seconds = note_seconds * len(phrase) * bars
    n = int(seconds * RATE)
    over = int(RING * RATE)
    out = drone(root, seconds, 0.10) + [0.0] * over
    step = int(note_seconds * RATE)
    for bar in range(bars):
        for i, degree in enumerate(phrase):
            # Each repeat of the phrase sits a little differently, so a minute of it
            # does not feel like the same four seconds twelve times over.
            shift = (bar % 3) if degree + (bar % 3) < len(SCALE) else 0
            freq = SCALE[degree + shift] * brightness
            start = (bar * len(phrase) + i) * step
            note = bell(freq, note_seconds * 1.6, 0.22)
            for j, value in enumerate(note):
                if start + j < n + over:
                    out[start + j] += value
    # Fold the ringing tail back over the opening, then cut the loop to length.
    for i in range(over):
        out[i] += out[n + i]
    out = out[:n]
    peak = max(abs(v) for v in out) or 1.0
    return [v / peak * 0.58 for v in out]


TRACKS = {
    # Home, menus, lists: warm and unhurried.
    'bgm_menu': dict(phrase=[0, 2, 4, 2, 3, 1, 3, 0], note_seconds=0.62, bars=12,
                     root=130.81, brightness=1.0),
    # Games and the drawing flow: the same world, a little brighter and quicker.
    'bgm_play': dict(phrase=[2, 4, 5, 7, 5, 4, 2, 3], note_seconds=0.52, bars=14,
                     root=146.83, brightness=1.0),
    # Stories: the softest, low and slow, meant to be under a voice.
    'bgm_story': dict(phrase=[0, 3, 2, 4, 3, 1], note_seconds=0.82, bars=10,
                      root=110.00, brightness=0.75),
}


def write(name, samples):
    wav = '/tmp/pashmak_bgm.wav'
    with wave.open(wav, 'w') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(array.array(
            'h', [max(-32767, min(32767, int(v * 32767))) for v in samples]).tobytes())
    ogg = os.path.join(RAW, name + '.ogg')
    subprocess.run(['oggenc', '-Q', '-q', '1', '-o', ogg, wav],
                   check=True, capture_output=True)
    return os.path.getsize(ogg)


def main():
    total = 0
    for name, spec in sorted(TRACKS.items()):
        samples = render(**spec)
        # A loop joins cleanly when the end runs into the start without a step —
        # not when both ends happen to be quiet. So the test is the size of the jump
        # across the join, against the loudest moment in the piece.
        peak = max(abs(v) for v in samples)
        jump = abs(samples[0] - samples[-1]) / peak
        slope_in = abs(samples[1] - samples[0])
        slope_out = abs(samples[-1] - samples[-2])
        slip = abs(slope_in - slope_out) / peak
        seam = 'ok' if jump < 0.05 and slip < 0.05 else 'CLICKS'
        size = write(name, samples)
        total += size
        print('  %-11s %5.1fs  %5.0f KB  seam %s (step %.4f, slope %.4f)'
              % (name, len(samples) / RATE, size / 1024, seam, jump, slip))
        if seam != 'ok':
            return 1
    print('wrote %d loops, %.0f KB' % (len(TRACKS), total / 1024))
    return 0


if __name__ == '__main__':
    sys.exit(main())
