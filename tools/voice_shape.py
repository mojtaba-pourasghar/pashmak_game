# -*- coding: utf-8 -*-
"""Shaping a synthesised voice without making it sound synthesised.

This replaces the hand-rolled arithmetic in voice_bear.py, which had to work
with no numpy at all and shows it. Two things in there were actively hurting the
result:

  * pitch was lowered by linear interpolation between samples. That is a
    resampler with no anti-aliasing, so every change of rate folded high
    frequencies back down as a metallic edge — the single biggest reason the
    output sounded artificial. scipy.signal.resample_poly does the same job
    properly, filtering before it decimates.

  * the shelves were one-pole filters applied forwards only, which smears phase
    and muddies consonants. sosfiltfilt runs a Butterworth section forwards and
    backwards, so the frequency shaping lands with no phase shift at all.

The chain, in order: warmth, then the pitch move, then levelling, then the
edges. Pitch last among the spectral steps, so the filters act on the voice as
the model made it.
"""
import math

import numpy as np
from scipy.signal import butter, resample_poly, sosfiltfilt


def warm(audio, rate, shelf_hz=240.0, lift_db=3.5, top_hz=7200.0, tame_db=-2.5):
    """Lift the chest of the voice, take the edge off the top.

    Both are gentle shelves built from a Butterworth section and applied with no
    phase shift, which is what keeps consonants crisp while the body fills out.
    """
    audio = np.asarray(audio, dtype=np.float64)
    nyquist = rate / 2.0

    low = sosfiltfilt(butter(2, shelf_hz / nyquist, btype='low', output='sos'), audio)
    audio = audio + low * (10.0 ** (lift_db / 20.0) - 1.0)

    high = sosfiltfilt(butter(2, min(top_hz / nyquist, 0.99), btype='high',
                              output='sos'), audio)
    audio = audio + high * (10.0 ** (tame_db / 20.0) - 1.0)
    return audio


def drop_pitch(audio, drop):
    """Lower pitch and formants together by `drop`, properly resampled.

    Stretching the waveform lowers everything in it — the pitch and the formants
    with it, which is what makes a voice sound like it came from a smaller or
    larger body. The point here is that resample_poly filters before it
    resamples; the version of this that interpolated by hand aliased, and that
    aliasing was most of what sounded wrong.

    The caller gives the time back by asking the engine for a faster reading, so
    the line still lands at the right length.
    """
    if abs(drop - 1.0) < 1e-6:
        return np.asarray(audio, dtype=np.float64)
    up, down = _ratio(drop)
    return resample_poly(np.asarray(audio, dtype=np.float64), up, down)


def _ratio(value, limit=240):
    """A small integer fraction for `value`, because resample_poly wants one."""
    best, error = (1, 1), float('inf')
    for down in range(1, limit + 1):
        up = int(round(value * down))
        if up < 1:
            continue
        near = abs(up / down - value)
        if near < error:
            best, error = (up, down), near
            if near < 1e-6:
                break
    return best


def level(audio, threshold=0.45, ratio=3.0, peak=0.89):
    """Soft-knee compression, then one fixed peak for every line in the app.

    A loud syllable should not startle a four-year-old and a quiet one should not
    vanish under a passing car.
    """
    audio = np.asarray(audio, dtype=np.float64)
    top = np.max(np.abs(audio))
    if top <= 0:
        return audio
    x = audio / top
    over = np.abs(x) > threshold
    shaped = x.copy()
    shaped[over] = np.sign(x[over]) * (
        threshold + (np.abs(x[over]) - threshold) / ratio)
    top = np.max(np.abs(shaped)) or 1.0
    return shaped / top * peak


def edges(audio, rate, fade_ms=10.0):
    """A short fade each end, because a waveform starting at full amplitude clicks."""
    audio = np.asarray(audio, dtype=np.float64)
    n = min(int(rate * fade_ms / 1000.0), len(audio) // 2)
    if n > 1:
        ramp = np.linspace(0.0, 1.0, n)
        audio[:n] *= ramp
        audio[-n:] *= ramp[::-1]
    return audio


def to_pcm16(audio):
    return np.clip(np.asarray(audio) * 32767.0, -32768, 32767).astype(np.int16)


def shape(audio, rate, drop=1.0):
    """The whole chain. Returns 16-bit samples ready to encode."""
    worked = warm(audio, rate)
    worked = drop_pitch(worked, drop)
    worked = level(worked)
    worked = edges(worked, rate)
    return to_pcm16(worked)


def fundamental(samples, rate, low=70.0, high=400.0):
    """The pitch the result actually came out at, by autocorrelation.

    Voiced frames only: silence and consonants have no pitch to find, and letting
    them vote drags the answer wherever the noise happens to sit.
    """
    x = np.asarray(samples, dtype=np.float64)
    frame, hop = int(rate * 0.04), int(rate * 0.02)
    lo, hi = int(rate / high), int(rate / low)
    found = []
    for start in range(0, max(0, len(x) - frame), hop):
        window = x[start:start + frame]
        if np.mean(window * window) < 2.0e6:
            continue
        window = window - window.mean()
        corr = np.correlate(window, window, mode='full')[frame - 1:]
        if hi >= len(corr):
            continue
        lag = int(np.argmax(corr[lo:hi])) + lo
        if lag:
            found.append(rate / float(lag))
    return float(np.median(found)) if found else 0.0
