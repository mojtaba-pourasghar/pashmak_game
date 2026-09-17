# -*- coding: utf-8 -*-
"""Makes the ten little noises: a pop, a card turning, a star, a shutter.

eSpeak can say words but it cannot make a bubble burst, so these are written
sample by sample. They are deliberately soft and short — a three-year-old plays
with the volume up and the same tap sound fires hundreds of times a session, so
nothing here is sharp and nothing is longer than it needs to be.
"""
import array, io, math, os, random, struct, subprocess, sys, wave

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, 'app/src/main/res/raw')
RATE = 22050


def tone(freq, seconds, volume=0.5, shape='sine', bend=1.0, fade='out'):
    """One voice: a sine or a triangle, optionally sliding in pitch."""
    n = int(seconds * RATE)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / n
        f = freq * (bend ** t)
        phase += 2 * math.pi * f / RATE
        if shape == 'sine':
            value = math.sin(phase)
        elif shape == 'triangle':
            value = 2 * abs(2 * ((phase / (2 * math.pi)) % 1) - 1) - 1
        else:                                    # soft noise
            value = random.uniform(-1, 1)
        if fade == 'out':
            envelope = (1 - t) ** 2
        elif fade == 'in':
            envelope = t ** 2
        else:                                    # a bell: quick on, slow off
            envelope = min(1.0, t * 12) * (1 - t) ** 1.5
        out.append(value * envelope * volume)
    return out


def mix(*voices):
    length = max(len(v) for v in voices)
    out = [0.0] * length
    for voice in voices:
        for i, value in enumerate(voice):
            out[i] += value
    peak = max(abs(v) for v in out) or 1.0
    return [v / peak * 0.72 for v in out]        # leave headroom, never clip


def after(gap, voice):
    return [0.0] * int(gap * RATE) + voice


SOUNDS = {
    # A bubble bursting: a short blip that falls in pitch, plus a breath of noise.
    'sfx_pop': lambda: mix(tone(760, 0.09, 0.6, 'sine', bend=0.45),
                           tone(0, 0.05, 0.18, 'noise')),
    # A card turning over: two soft taps.
    'sfx_flip': lambda: mix(tone(320, 0.05, 0.45, 'triangle', bend=1.5),
                            after(0.07, tone(240, 0.06, 0.3, 'triangle', bend=0.7))),
    # A correct answer: a rising two-note chime.
    'sfx_match': lambda: mix(tone(660, 0.14, 0.5, 'sine', fade='bell'),
                             after(0.10, tone(880, 0.22, 0.5, 'sine', fade='bell'))),
    # A star being awarded: three notes up.
    'sfx_star': lambda: mix(tone(784, 0.12, 0.45, 'sine', fade='bell'),
                            after(0.09, tone(988, 0.12, 0.45, 'sine', fade='bell')),
                            after(0.18, tone(1319, 0.26, 0.5, 'sine', fade='bell'))),
    # A camera: a click, not a clack.
    'sfx_shutter': lambda: mix(tone(0, 0.03, 0.5, 'noise'),
                               after(0.05, tone(0, 0.025, 0.3, 'noise'))),
    # "Not that one", and never harsh: two low notes, gently down.
    'sfx_wrong': lambda: mix(tone(300, 0.13, 0.4, 'sine', fade='bell'),
                             after(0.11, tone(233, 0.22, 0.4, 'sine', fade='bell'))),
    # A pencil on paper.
    'sfx_brush': lambda: mix(tone(0, 0.13, 0.22, 'noise')),
    # Any button. The quietest thing here, because it fires all day.
    'sfx_tap': lambda: mix(tone(520, 0.045, 0.32, 'sine', bend=0.8)),
    # Something moving across the screen.
    'sfx_whoosh': lambda: mix(tone(0, 0.20, 0.26, 'noise', fade='in'),
                              tone(180, 0.20, 0.18, 'sine', bend=2.6)),
    # A mission or a story finishing: a little fanfare.
    'sfx_fanfare': lambda: mix(tone(523, 0.14, 0.42, 'triangle', fade='bell'),
                               after(0.11, tone(659, 0.14, 0.42, 'triangle', fade='bell')),
                               after(0.22, tone(784, 0.16, 0.45, 'triangle', fade='bell')),
                               after(0.36, tone(1047, 0.40, 0.5, 'sine', fade='bell'))),
}


def write(name, samples):
    wav = '/tmp/pashmak_sfx.wav'
    with wave.open(wav, 'w') as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(array.array(
            'h', [max(-32767, min(32767, int(v * 32767))) for v in samples]).tobytes())
    ogg = os.path.join(RAW, name + '.ogg')
    subprocess.run(['oggenc', '-Q', '-q', '2', '-o', ogg, wav],
                   check=True, capture_output=True)
    return os.path.getsize(ogg)


def main():
    random.seed(7)                               # the same noise every run
    total = 0
    for name, build in sorted(SOUNDS.items()):
        samples = build()
        size = write(name, samples)
        total += size
        print('  %-14s %5.2fs  %4.1f KB' % (name, len(samples) / RATE, size / 1024))
    print('wrote %d effects, %.0f KB' % (len(SOUNDS), total / 1024))
    return 0


if __name__ == '__main__':
    sys.exit(main())
