#!/usr/bin/env python3
"""voicesync — does the app's sense of time match the voice it actually ships?

Every clip in res/raw has an exact length. Before this check existed the app did
not ask for it: it counted the words of a line, assumed 110 of them a minute,
and used that to pace the read-along and to decide when Pashmak had finished
talking. Umbriel reads at 87. So seven lines in ten ran past their own hold
timer, which calls idle(), which stops the player — the voice was cut off
mid-sentence on nearly every line in the app, worst on the long story passages
and on the choice beats, where the question was lost before the child heard it.

Four things are checked, all against the files themselves:

  1. the read-along can keep up — the typewriter's slowest allowed step is at
     least what the slowest clip needs per character;
  2. the word-count fallback, which is all a device with no recording has, is
     the measured pace and not a guess;
  3. the hold is still derived from the clip's real length, so the mechanism
     cannot be quietly removed;
  4. nothing that leads to Pashmak speaking is scheduled on a hand-typed
     number — that is the shape the fault took (1500L, 1600L, 900L, 2400L).

Check 4 reads the Java as text, so a number moved into a named constant would
slip past it. It catches the literal, which is what was there.

Needs ffprobe. No compiler, no device.
"""

import os
import re
import subprocess
import statistics
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, 'app/src/main/res/raw')
JAVA = os.path.join(ROOT, 'app/src/main/java/com/pashmak/app')
NOT_SPEECH = ('bgm_', 'sfx_', 'lullaby_')
HARAKAT = set('ًٌٍَُِّْ'
              'ٰٕٓٔ')
# Everything that puts words in Pashmak's mouth.
SPEAKS = ('.say(', 'onCorrect(', 'onWrong(', '.cheer(', '.encourage(',
          '.poke(', '.help(', '.speak(', '.welcome(')


def bare(text):
    return ''.join(c for c in text if c not in HARAKAT)


def manifest_text():
    """The line behind each clip name, as the manifest in res/raw lists it."""
    said = {}
    path = os.path.join(RAW, 'audio_manifest.txt')
    for line in open(path, encoding='utf-8'):
        found = re.match(r'^ {2,}([a-z][a-z0-9_]*) {2,}(\S.*)$', line.rstrip())
        if found and found.group(1) not in said:
            said[found.group(1)] = bare(found.group(2)).strip()
    return said


def length_of(path):
    """The clip's real length in milliseconds, straight from the container."""
    try:
        out = subprocess.run(
            ['ffprobe', '-v', 'error', '-show_entries', 'format=duration',
             '-of', 'csv=p=0', path],
            capture_output=True, text=True).stdout.strip()
        return float(out) * 1000.0
    except (OSError, ValueError):
        return 0.0


def clips():
    """Every spoken clip: its name, its real length, and the words in it."""
    said = manifest_text()
    found = []
    for entry in sorted(os.listdir(RAW)):
        name, ext = os.path.splitext(entry)
        if ext not in ('.ogg', '.mp3', '.wav') or name.startswith(NOT_SPEECH):
            continue
        millis = length_of(os.path.join(RAW, entry))
        if millis <= 0:
            continue
        found.append((name, millis, said.get(name, '')))
    return found


def source(path):
    return open(os.path.join(JAVA, path), encoding='utf-8').read()


def number_in(text, pattern, label, problems):
    found = re.search(pattern, text)
    if not found:
        problems.append('%s: not found at all' % label)
        return None
    return float(found.group(1))


# --------------------------------------------------------------- the checks

def read_along_keeps_up(spoken, problems):
    """The typewriter's slowest step must cover the slowest clip."""
    controller = source('mascot/MascotController.java')
    ceiling = number_in(controller,
                        r'TYPEWRITER_MAX_MS\s*=\s*(\d+)L',
                        'MascotController.TYPEWRITER_MAX_MS', problems)
    worst, name = 0.0, ''
    for clip, millis, text in spoken:
        if len(text) > 3 and millis / len(text) > worst:
            worst, name = millis / len(text), clip
    print('  slowest clip spends %.0fms on a character (%s)' % (worst, name))
    if ceiling is None:
        return
    print('  the typewriter is allowed up to %.0fms' % ceiling)
    if ceiling < worst:
        problems.append(
            'the read-along cannot keep up with %s: it needs %.0fms a character '
            'and TYPEWRITER_MAX_MS is %.0fms, so the words finish before the '
            'voice does' % (name, worst, ceiling))


def fallback_is_measured(spoken, problems):
    """The word-count estimate must be the pace that was measured."""
    per_word = sorted(millis / len(text.split())
                      for _, millis, text in spoken if len(text.split()) > 1)
    measured = 60000.0 / statistics.median(per_word)
    print('  the voice reads at %.0f words a minute (median of %d lines)'
          % (measured, len(per_word)))
    for path, pattern, label in (
            ('mascot/MascotController.java', r'words / (\d+)f \* 60000f',
             'MascotController.spokenMs'),
            ('data/catalog/Tale.java', r'words / (\d+)f \* 60_000f',
             'Tale.Moment.durationMs')):
        assumed = number_in(source(path), pattern, label, problems)
        if assumed is None:
            continue
        print('  %-32s assumes %.0f' % (label, assumed))
        if abs(assumed - measured) > 0.05 * measured:
            problems.append(
                '%s assumes %.0f words a minute; the voice reads at %.0f, which '
                'is %.0f%% out' % (label, assumed, measured,
                                   100 * abs(assumed - measured) / measured))


def hold_waits_for_the_clip(problems):
    """The hold must still be taken from the clip's own length."""
    controller = source('mascot/MascotController.java')
    player = source('audio/VoicePlayer.java')
    for text, needle, why in (
            (player, 'voice.getDuration()',
             'VoicePlayer never asks the player how long the clip is'),
            (controller, 'voice.soundingMs()',
             'MascotController never asks for the clip\'s real length'),
            (controller, 'hold = Math.max(hold, voiced + VOICE_TAIL_MS)',
             'the hold is no longer stretched to cover the voice, so the timer '
             'can fire while Pashmak is still talking')):
        if needle not in text:
            problems.append(why)
    print('  the hold is taken from the clip and the clip from the player')


def speaking_methods(text):
    """Methods in this file that end up speaking, following calls one level on."""
    bodies = {}
    for found in re.finditer(
            r'\n    (?:@\w+\s+)*(?:public|private|protected)[\w\s<>,\[\]]*?'
            r'\b(\w+)\s*\([^;{]*?\)\s*\{', text):
        start = text.index('{', found.end() - 1)
        depth, at = 0, start
        while at < len(text):
            if text[at] == '{':
                depth += 1
            elif text[at] == '}':
                depth -= 1
                if depth == 0:
                    break
            at += 1
        bodies[found.group(1)] = text[start:at]
    speaks = {name for name, body in bodies.items()
              if any(s in body for s in SPEAKS)}
    for _ in range(len(bodies)):          # follow calls until nothing new turns up
        grown = {name for name, body in bodies.items()
                 if any(re.search(r'\b%s\s*\(' % other, body)
                        for other in speaks)}
        if grown <= speaks:
            break
        speaks |= grown
    return speaks, bodies


def runnable_fields(text):
    """Runnable fields that stand for a method, so postDelayed(field, …) resolves."""
    return dict(re.findall(r'Runnable\s+(\w+)\s*=\s*this::(\w+)\s*;', text))


def no_typed_stopwatches(problems):
    """A delay that leads to speech must not be a number somebody typed."""
    checked, typed = 0, 0
    for here, _, files in os.walk(os.path.join(JAVA, 'ui')):
        for entry in sorted(files):
            if not entry.endswith('.java'):
                continue
            path = os.path.join(here, entry)
            text = open(path, encoding='utf-8').read()
            speaks, _ = speaking_methods(text)
            fields = runnable_fields(text)
            for found in re.finditer(r'postDelayed\(', text):
                start = found.end()
                depth, at = 1, start
                while at < len(text) and depth:
                    depth += (text[at] == '(') - (text[at] == ')')
                    at += 1
                call = text[start:at - 1]
                split = -1
                depth = 0
                for i, ch in enumerate(call):
                    depth += (ch in '([{') - (ch in ')]}')
                    if ch == ',' and depth == 0:
                        split = i
                if split < 0:
                    continue
                action, delay = call[:split], call[split + 1:].strip()
                target = action.strip()
                if target in fields:
                    action = fields[target]
                leads_to_speech = (
                    any(s in action for s in SPEAKS)
                    or any(re.search(r'\b%s\s*\(' % name, action)
                           or action.strip() == name for name in speaks)
                    or (target in fields and fields[target] in speaks))
                if not leads_to_speech:
                    continue
                checked += 1
                if re.fullmatch(r'\d+L?', delay):
                    typed += 1
                    line = text[:found.start()].count('\n') + 1
                    problems.append(
                        '%s:%d waits %s before Pashmak speaks again. A typed '
                        'number cannot know how long the clip is; ask say() '
                        'for the length it returns.'
                        % (os.path.relpath(path, ROOT), line, delay))
    print('  %d delays lead to Pashmak speaking, %d of them on a typed number'
          % (checked, typed))


def main():
    spoken = clips()
    if not spoken:
        print('no clips in res/raw — nothing to check against')
        return 1
    lengths = [millis for _, millis, _ in spoken]
    print('%d spoken clips, %.1fs the longest, %.0fs in all'
          % (len(spoken), max(lengths) / 1000, sum(lengths) / 1000))
    problems = []
    read_along_keeps_up(spoken, problems)
    fallback_is_measured(spoken, problems)
    hold_waits_for_the_clip(problems)
    no_typed_stopwatches(problems)
    print()
    if problems:
        print('FAIL')
        for problem in problems:
            print('  - %s' % problem)
        return 1
    print('PASS: the read-along keeps pace with the voice, and nothing cuts it off')
    return 0


if __name__ == '__main__':
    sys.exit(main())
