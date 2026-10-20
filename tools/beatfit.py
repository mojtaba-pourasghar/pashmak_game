# -*- coding: utf-8 -*-
"""Checks every StoryBeat call against the factory it is calling.

This is the check that was missing. Three narrateTo calls in StoryCatalog had
been written across two lines with the jump target left out, so a StoryProp sat
where the int belongs — «error: incompatible types» at the first of them, and
the other two waiting behind it. Nothing here could see that: vfit measures
layouts, storyfit measures text, and neither reads an argument list.

The compiler is the real authority and it does not run in this sandbox, so the
signatures are read out of StoryBeat.java rather than retyped, and every call in
the catalogue is matched against them. A target that points outside its own
story is caught too: that one compiles and then walks a child off the end of a
story at run time.

    python3 tools/beatfit.py
"""
import io
import os
import re
import sys


def _utf8_console():
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding='utf-8', errors='replace')
        except (AttributeError, ValueError):
            pass


_utf8_console()

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, 'app/src/main/java/com/pashmak/app')
BEAT = os.path.join(JAVA, 'data/catalog/StoryBeat.java')
CATALOGUES = ('data/catalog/StoryCatalog.java', 'data/catalog/TaleCatalog.java')


def signatures():
    """What each factory takes, read from the class rather than assumed."""
    src = io.open(BEAT, encoding='utf-8').read()
    kinds = {'String': 'str', 'int': 'int'}
    found = {}
    for call in re.finditer(r'public static StoryBeat (\w+)\(([^)]*)\)', src, re.S):
        fixed = []
        for piece in call.group(2).split(','):
            piece = ' '.join(piece.split())
            if not piece or piece.startswith('StoryProp...'):
                continue
            fixed.append(kinds.get(piece.split()[0], 'other'))
        found[call.group(1)] = fixed
    return found


def arguments(text, open_paren):
    """The call's arguments, split at the commas that are at the top level."""
    args, cur, depth, quoted, i = [], '', 0, False, open_paren
    while i < len(text):
        c = text[i]
        if quoted:
            cur += c
            if c == '\\':
                cur += text[i + 1]
                i += 2
                continue
            if c == '"':
                quoted = False
        elif c == '"':
            quoted = True
            cur += c
        elif c in '([':
            depth += 1
            if depth > 1:
                cur += c
        elif c in ')]':
            depth -= 1
            if depth == 0:
                args.append(cur.strip())
                return [a for a in args if a]
            cur += c
        elif c == ',' and depth == 1:
            args.append(cur.strip())
            cur = ''
        else:
            cur += c
        i += 1
    raise ValueError('a StoryBeat call is never closed')


def kind(arg):
    if arg.startswith('"'):
        return 'str'
    if re.fullmatch(r'-?\d+', arg):
        return 'int'
    return 'prop'


def check(path, wanted):
    src = io.open(path, encoding='utf-8').read()
    name = os.path.basename(path)
    problems = []
    calls = 0
    for call in re.finditer(r'StoryBeat\.(\w+)\s*\(', src):
        calls += 1
        factory = call.group(1)
        line = src[:call.start()].count('\n') + 1
        if factory not in wanted:
            problems.append('%s:%d  StoryBeat.%s is not a factory'
                            % (name, line, factory))
            continue
        want = wanted[factory]
        got = [kind(a) for a in arguments(src, call.end() - 1)]
        if got[:len(want)] != want:
            problems.append('%s:%d  %s takes (%s, prop...) but was given (%s)'
                            % (name, line, factory, ', '.join(want), ', '.join(got)))
        elif any(k != 'prop' for k in got[len(want):]):
            problems.append('%s:%d  %s has something other than a prop after its '
                            'fixed arguments: (%s)'
                            % (name, line, factory, ', '.join(got)))

    # Every jump has to land on a beat of the same story.
    starts = [m.start() for m in re.finditer(r'new (?:Story|Tale)\(', src)]
    for i, start in enumerate(starts):
        end = starts[i + 1] if i + 1 < len(starts) else len(src)
        block = src[start:end]
        title = re.search(r'new (?:Story|Tale)\("([^"]+)"', block)
        title = title.group(1) if title else '?'
        beats = len(re.findall(r'StoryBeat\.\w+\s*\(', block))
        jumps = [int(t) for t in
                 re.findall(r'narrateTo\s*\(\s*"(?:[^"\\]|\\.)*"\s*,\s*(-?\d+)',
                            block, re.S)]
        jumps += [int(t) for t in re.findall(r'",\s*(-?\d+)\s*,', block)]
        for target in jumps:
            if not 0 <= target < beats:
                problems.append('%s  story %s jumps to beat %d, and it has %d'
                                % (name, title, target, beats))
    return calls, problems


def main():
    wanted = signatures()
    print('factories in StoryBeat: %s'
          % ', '.join('%s(%s, prop...)' % (n, ', '.join(a) or '')
                      for n, a in sorted(wanted.items())))
    total = 0
    problems = []
    for rel in CATALOGUES:
        path = os.path.join(JAVA, rel)
        if not os.path.exists(path):
            continue
        calls, found = check(path, wanted)
        total += calls
        problems += found
        print('  %-22s %4d calls' % (os.path.basename(path), calls))
    if problems:
        print('\nFAIL: %d call(s) do not fit' % len(problems))
        for row in problems[:20]:
            print('   %s' % row)
        return 1
    print('\nPASS: all %d beats fit their factory, and every jump lands in its '
          'own story' % total)
    return 0


if __name__ == '__main__':
    sys.exit(main())
