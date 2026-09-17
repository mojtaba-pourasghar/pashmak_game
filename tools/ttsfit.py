# -*- coding: utf-8 -*-
"""Runs SpeechEngine's engine search against made-up devices.

The search is a small state machine — ask Google's engine, then the device default,
then everything else, and decide between four endings — and it runs inside Android
where nothing here can execute it. So it is mirrored and driven against the devices
that actually matter: the Samsung and Xiaomi phones whose own engine has no Persian,
which is what made the app mute in the first place.

If the mirror and the Java drift apart this is worthless, so the mirror is written
from the Java line by line and the constants are read out of it rather than retyped.
"""
import io, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'app/src/main/java/ir/brandimo/pashmak/audio/SpeechEngine.java')

AVAILABLE, MISSING, NOT_SUPPORTED = 'available', 'missing_data', 'not_supported'


def constants():
    """Reads the values out of the Java so the two cannot disagree quietly."""
    text = io.open(SRC, encoding='utf-8').read()
    pitch = float(re.search(r'PITCH = ([\d.]+)f', text).group(1))
    rate = float(re.search(r'RATE = ([\d.]+)f', text).group(1))
    google = re.search(r'GOOGLE_TTS = "([^"]+)"', text).group(1)
    locales = re.findall(r'new Locale\("(\w+)"(?:, "(\w+)")?\)', text)
    return (pitch, rate, google,
            ['-'.join(p for p in loc if p) for loc in locales],
            queue_order(text, google))


def queue_order(text, google):
    """The order the Java asks engines in, read out of restart().

    Taken from the source rather than assumed: the whole point of this change is
    that Google is asked BEFORE the device default, and a mirror that hardcoded
    that order would go on passing after somebody deleted the line.
    """
    body = text[text.index('public void restart()'):text.index('private void tryNext')]
    order = []
    for token in re.findall(r'queue\.add\((\w+)\)', body):
        order.append(None if token == 'null'
                     else (google if token == 'GOOGLE_TTS' else token))
    return order


class Device:
    """What a phone has installed and what each engine can say."""

    def __init__(self, name, default, engines):
        self.name = name
        self.default = default          # package of the system default, or None
        self.engines = engines          # package -> {locale tag: answer}

    def start(self, wanted):
        """Mirrors `new TextToSpeech(ctx, init, engine)`.

        Naming an engine that is not installed does not reliably fail: some versions
        hand back the default instead, which is why the Java confirms the package is
        really there before it claims to be using it.
        """
        if wanted is None:
            return self.default
        if wanted in self.engines:
            return wanted
        return self.default             # silently substituted

    def installed(self, package):
        return package is not None and package in self.engines


def search(device, google, locales, order):
    """The Java, step for step."""
    queue = list(order)
    any_engine_works = False
    needs_data = None
    while queue:
        asked = queue.pop(0)
        running = device.start(asked)
        if running is None:
            continue                            # init failed; ask the next
        any_engine_works = True
        if asked is None:
            for package in device.engines:
                if package != google and package not in queue:
                    queue.append(package)
        speaks = device.engines[running]
        chosen = None
        for tag in locales:
            answer = speaks.get(tag, NOT_SUPPORTED)
            if answer == AVAILABLE:
                chosen = tag
                break
            if answer == MISSING and needs_data is None:
                needs_data = asked if device.installed(asked) else device.default
        if chosen is not None:
            using = asked if device.installed(asked) else device.default
            return 'READY', using
    if needs_data is not None:
        return 'NEEDS_DATA', needs_data
    return ('NO_PERSIAN' if any_engine_works else 'UNAVAILABLE'), None


def main():
    pitch, rate, google, locales, order = constants()
    persian = [tag for tag in locales if tag.startswith('fa')]
    problems = []

    if not (1.1 <= pitch <= 1.35):
        problems.append('pitch %.2f is not the warm, boyish bear it is meant to be'
                        % pitch)
    if not (0.85 <= rate <= 0.98):
        problems.append('rate %.2f is not a gentle pace for a three-year-old' % rate)
    for want in ('fa-IR', 'fas', 'fa'):
        if want not in persian:
            problems.append('Persian is not asked for as %s' % want)
    if not order or order[0] != google:
        problems.append("Google's engine is no longer the first one asked, so a "
                        "Samsung or Xiaomi default gets to answer first")
    if None not in order:
        problems.append('the device default is never asked, so a phone already set '
                        'to a Persian engine would be skipped')
    print('voice: pitch %.2f, rate %.2f' % (pitch, rate))
    print('engines asked in order: %s'
          % ', '.join(e if e else 'the device default' for e in order))
    print('Persian asked for as: %s' % ', '.join(persian))
    print('')

    SAMSUNG = 'com.samsung.SMT'
    ESPEAK = 'com.reecedunn.espeak'
    RHVOICE = 'com.github.olga_yakovleva.rhvoice.android'
    NONE = {tag: NOT_SUPPORTED for tag in persian}
    SPEAKS = {tag: AVAILABLE for tag in persian}
    UNDOWNLOADED = {tag: MISSING for tag in persian}

    cases = [
        ('Samsung phone, nothing Persian anywhere',
         Device('a', SAMSUNG, {SAMSUNG: NONE, google: NONE}), 'NO_PERSIAN', None),
        ('Google has Persian',
         Device('b', SAMSUNG, {SAMSUNG: NONE, google: SPEAKS}), 'READY', google),
        ('Google knows Persian but has not downloaded it',
         Device('c', SAMSUNG, {SAMSUNG: NONE, google: UNDOWNLOADED}),
         'NEEDS_DATA', google),
        ('the default engine is the one that speaks Persian',
         Device('d', RHVOICE, {RHVOICE: SPEAKS}), 'READY', RHVOICE),
        ('a third engine speaks Persian; the default and Google do not',
         Device('e', SAMSUNG, {SAMSUNG: NONE, google: NONE, ESPEAK: SPEAKS}),
         'READY', ESPEAK),
        ('no speech engine at all',
         Device('f', None, {}), 'UNAVAILABLE', None),
        ('Google is not installed, so it must not be named as the voice',
         Device('g', RHVOICE, {RHVOICE: SPEAKS}), 'READY', RHVOICE),
        ('Xiaomi phone whose own engine has no Persian, but eSpeak does',
         Device('h', 'com.xiaomi.mibrain.speech',
                {'com.xiaomi.mibrain.speech': NONE, ESPEAK: SPEAKS}),
         'READY', ESPEAK),
    ]

    for label, device, want_status, want_engine in cases:
        status, engine = search(device, google, persian, order)
        ok = status == want_status and engine == want_engine
        if not ok:
            problems.append('%s -> %s via %s, expected %s via %s'
                            % (label, status, engine, want_status, want_engine))
        print('  %-58s %-12s %s' % (label, status, 'ok' if ok else 'WRONG'))

    print('\n%s' % ('FAIL: %d problem(s)' % len(problems) if problems
                    else 'PASS: every device reaches the right answer, and the voice '
                         'is set for a small warm bear'))
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
