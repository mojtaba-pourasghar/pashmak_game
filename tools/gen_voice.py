# -*- coding: utf-8 -*-
"""Speaks every line in the app and puts the result in res/raw.

Why this exists. Pashmak used to rely on the device's own text-to-speech, and most
Android phones cannot say a word of Persian: Google's engine has no Persian voice
and the manufacturers' engines have none either. Everything built around that —
asking each engine in turn, offering to install one — still left the app silent on
the phone in front of the child, which is the only test that counts.

So the voice is not asked for any more, it is carried. eSpeak NG does speak Persian,
and it runs here, so every line is synthesised once at build time and shipped inside
the app. The result works on any device, offline, with nothing to install.

It is a synthesiser and it sounds like one. That was the trade: a plain voice that
is always there beats a better one that is usually absent. The moment anybody records
a real voice under the same file name it wins — VoicePlayer prefers res/raw over
synthesis, and these files ARE res/raw, so a recording simply replaces one.

Run: python3 tools/gen_voice.py [--only NAME] [--dry]
"""
import io, os, re, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN = os.path.join(ROOT, 'app/src/main')
RAW = os.path.join(MAIN, 'res/raw')
CATALOG = os.path.join(MAIN, 'java/ir/brandimo/pashmak/data/catalog')

# A small warm creature, not a newsreader. eSpeak's pitch runs 0-99 (50 is neutral)
# and its speed is words a minute (175 is neutral), so these are the same choice the
# live engine makes with pitch 1.2 and rate 0.92, in eSpeak's own units.
VOICE = ['-v', 'fa', '-p', '68', '-s', '150']
QUALITY = ['-Q', '-q', '0', '--resample', '22050']


def strings():
    text = io.open(os.path.join(MAIN, 'res/values/strings.xml'), encoding='utf-8').read()
    out = {}
    for m in re.finditer(r'<string name="([^"]+)">(.*?)</string>', text, re.S):
        out[m.group(1)] = m.group(2)
    return out


# What a placeholder becomes when the line is spoken. A name that is fixed is filled
# in; a name that changes with the mission or the colour becomes a word that keeps the
# sentence upright, because «%1$s» read aloud is noise and a gap is worse — the first
# attempt turned «من %1$sم» into «من م».
FILLED = {
    'ms_welcome': ['پشمک'],
    'ms_help_mission': ['ماموریت', 'این'],
    'mission_start_line': ['ماموریت تازه'],
    'alive_arrived': ['نقاشیت'],
    'paint_right': ['این'],
    'paint_wrong': ['اینجا'],
    'bubbles_not_that': [''],
    'lullaby_missing': ['این'],
    'trace_hint': ['این حرف'],
    'memory_counter': ['', ''],
}


def clean(text, key=None):
    """Turns a string resource into something worth saying out loud."""
    for i, word in enumerate(FILLED.get(key, [])):
        text = text.replace('%%%d$s' % (i + 1), word)
    text = re.sub(r'%\d+\$[sd]', '', text)
    text = text.replace('\\n', ' ').replace("\\'", "'").replace('\\"', '"')
    text = re.sub(r'<[^>]+>', '', text)
    text = text.replace('&amp;', 'و').replace('&lt;', '').replace('&gt;', '')
    # The zero-width non-joiner holds Persian words together in print; spoken, it is
    # nothing, and eSpeak reads it as a pause in the middle of a word.
    text = text.replace('‌', ' ')
    return re.sub(r'\s+', ' ', text).strip()


def lines():
    """Every (file name, words) pair the app will ever ask res/raw for."""
    S = strings()
    out = []

    def add(name, key_or_text, literal=False):
        words = key_or_text if literal else S.get(key_or_text, '')
        words = clean(words, None if literal else key_or_text)
        if words:
            out.append((name, words))

    add('welcome', 'ms_welcome')
    for i in (1, 2, 3):
        add('win%d' % i, 'ms_win_%d' % i)
    for i in (1, 2):
        add('try_again%d' % i, 'ms_try_%d' % i)
    add('giggle', 'ms_poke_1')
    add('poke', 'ms_poke_2')

    add('help_default', 'ms_help_default')
    add('help_paint', 'ms_help_paint')
    add('help_trace', 'ms_help_trace')
    add('help_mission', 'ms_help_mission')

    add('mission_start', 'mission_start_line')
    add('item_arrived', 'alive_arrived')
    add('mission_done', 'ms_mission_done')

    add('paint_right', 'paint_right')
    add('paint_wrong', 'paint_wrong')
    add('paint_done', 'paint_all_right')

    add('trace_done', 'trace_award')
    add('trace_more', 'trace_incomplete')

    add('memory_match', 'memory_match')
    add('memory_miss', 'memory_miss')
    add('memory_win', 'memory_win')
    add('memory_next', 'memory_next_level')

    add('bubble_pop', 'bubbles_pop_line')
    add('bubble_goal', 'bubbles_goal_letters')
    add('bubble_wrong', 'bubbles_not_that')
    add('bubble_round', 'bubbles_round_done')
    add('draw_empty', 'freedraw_empty')
    add('stage_locked', 'stage_locked')

    add('story_wrong', 'story_wrong')
    add('story_end', 'story_finished')
    add('tale_done', 'tale_finished')

    add('night_hello', 'lullaby_greeting')
    add('night_goodnight', 'lullaby_goodnight')
    add('night_missing', 'lullaby_missing')

    # One clip per glyph, named by its code point, said on its own.
    arrays = io.open(os.path.join(MAIN, 'res/values/arrays.xml'), encoding='utf-8').read()
    letters = re.search(r'<string-array name="trace_letters">(.*?)</string-array>',
                        arrays, re.S)
    if letters:
        for glyph in re.findall(r'<item>(.*?)</item>', letters.group(1)):
            add('letter_%x' % ord(glyph[0]), glyph, literal=True)
    digits = '۰۱۲۳۴۵۶۷۸۹'
    for i, digit in enumerate(digits):
        add('digit_%d' % i, digit, literal=True)

    # The stories, beat by beat, with the praise for finding the right thing.
    story = io.open(os.path.join(CATALOG, 'StoryCatalog.java'), encoding='utf-8').read()
    for block in re.split(r'Story \w+ = new Story\(', story)[1:]:
        sid = re.match(r'"([^"]+)"', block).group(1)
        beats = re.findall(
            r'StoryBeat\.(narrate|narrateTo|ask|choice|celebrate)\(\s*\n?\s*'
            r'"((?:[^"\\]|\\.)*)"', block)
        praises = re.findall(
            r'StoryBeat\.ask\(\s*\n?\s*"(?:[^"\\]|\\.)*",\s*\n?\s*"[^"]*",\s*'
            r'"((?:[^"\\]|\\.)*)"', block)
        praise = iter(praises)
        for i, (kind, text) in enumerate(beats):
            add('story_%s_%d' % (sid, i), text, literal=True)
            if kind == 'ask':
                try:
                    add('story_%s_%d_yes' % (sid, i), next(praise), literal=True)
                except StopIteration:
                    pass

    # The told tales, passage by passage.
    tales = io.open(os.path.join(CATALOG, 'TaleCatalog.java'), encoding='utf-8').read()
    for block in re.split(r'new Tale\(', tales)[1:]:
        tid = re.match(r'"([^"]+)"', block).group(1)
        for i, text in enumerate(re.findall(r'\bat\(\d+, "((?:[^"\\]|\\.)*)"', block)):
            add('tale_%s_%d' % (tid, i), text, literal=True)

    return out


def say(name, words):
    wav = '/tmp/pashmak_voice.wav'
    ogg = os.path.join(RAW, name + '.ogg')
    subprocess.run(['espeak-ng'] + VOICE + ['-w', wav, words],
                   check=True, capture_output=True)
    subprocess.run(['oggenc'] + QUALITY + ['-o', ogg, wav],
                   check=True, capture_output=True)
    return os.path.getsize(ogg)


def main():
    only = None
    dry = '--dry' in sys.argv
    if '--only' in sys.argv:
        only = sys.argv[sys.argv.index('--only') + 1]

    work = lines()
    if only:
        work = [(n, t) for n, t in work if only in n]

    # aapt only takes lowercase letters, digits and underscores.
    bad = [n for n, _ in work if not re.fullmatch(r'[a-z0-9_]+', n)]
    if bad:
        print('names aapt will refuse: %s' % bad[:5])
        return 1
    seen = {}
    for n, t in work:
        if n in seen and seen[n] != t:
            print('%s is asked to say two different things' % n)
            return 1
        seen[n] = t

    print('%d lines to speak' % len(work))
    if dry:
        for n, t in work[:8]:
            print('  %-24s %s' % (n, t))
        print('  ...')
        return 0

    os.makedirs(RAW, exist_ok=True)
    total = 0
    for i, (name, words) in enumerate(work):
        total += say(name, words)
        if (i + 1) % 100 == 0:
            print('  %d/%d, %.1f MB so far' % (i + 1, len(work), total / 1048576))
    print('wrote %d clips, %.1f MB' % (len(work), total / 1048576))
    return 0


if __name__ == '__main__':
    sys.exit(main())
