# -*- coding: utf-8 -*-
"""Regenerates res/raw/audio_manifest.txt from the strings and the story catalog."""
import re, io, os, sys

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'app/src/main')
strings = {}
for m in re.finditer(r'<string name="([^"]+)">(.*?)</string>',
                     io.open(ROOT + '/res/values/strings.xml', encoding='utf-8').read(), re.S):
    strings[m.group(1)] = m.group(2).replace('\\n', ' ').replace("\\'", "'").strip()

def S(key, *subs):
    text = strings[key]
    for i, sub in enumerate(subs):
        text = text.replace('%%%d$s' % (i + 1), sub)
    return text

out = []
W = out.append

W('Pashmak Game — audio drop-in list')
W('=================================')
W('Put the recordings in app/src/main/res/raw/ using exactly these file names.')
W('Any missing file is skipped silently; the game stays playable and the words')
W('still appear in the speech bubble. Use .mp3 or .ogg, mono, 44.1 kHz.')
W('')
W('File names must be lowercase with letters, digits and underscores only —')
W('aapt rejects anything else. Do not write the extension in this list; the')
W('app looks the file up by name, so welcome.mp3 and welcome.ogg both work.')
W('')
W('Every line Pashmak speaks in the app is listed below with the exact file it')
W('looks for. Nothing speaks without a name: MascotController.say() requires a')
W('clip, so this list is complete.')
W('')

def section(title):
    W('')
    W(title)
    W('-' * len(title))

def row(name, text):
    W('  %-22s %s' % (name, text))

section('1. BACKGROUND MUSIC  (looping, instrumental, no singing)')
W('These are the three files to drop in for background music:')
W('')
row('bgm_menu', 'Splash, home, the mission list, the games menu,')
W('  %-22s %s' % ('', 'the gallery and settings.'))
row('bgm_play', 'Every mini-game and the whole live-drawing flow.')
row('bgm_story', 'The story screen — softer than the games loop.')
W('')
W('  Make each one loop seamlessly; the app loops the file end-to-start with no')
W('  gap. 60-120 seconds is plenty. The music ducks to a whisper automatically')
W('  whenever Pashmak speaks and comes back up afterwards.')
W('')
W('  If no file is present the app synthesises a soft wordless loop instead, so')
W('  music plays either way. The bedtime screen plays no background music at')
W('  all — the lullaby is the sound there.')

section('2. MASCOT GREETING AND REACTIONS')
row('welcome', S('ms_welcome', 'پشمک'))
row('win1', S('ms_win_1'))
row('win2', S('ms_win_2'))
row('win3', S('ms_win_3'))
row('try_again1', S('ms_try_1'))
row('try_again2', S('ms_try_2'))
row('giggle', S('ms_poke_1'))
row('poke', S('ms_poke_2'))
W('')
W('  win1-3 play at random on any success, try_again1-2 on any miss, and')
W('  giggle/poke when the child taps Pashmak himself.')

section('3. SCREEN HINTS  (spoken once when a screen opens)')
row('help_default', S('ms_help_default'))
row('help_paint', S('ms_help_paint'))
row('help_trace', S('ms_help_trace'))
row('help_mission', S('ms_help_mission', '…', '…'))
W('')
W('  help_mission is shown with the mission and item names filled in. Record the')
W('  sentence without them — for example: «بریم سراغ ماموریت! اول این رو روی')
W('  کاغذ بکش.» The written line on screen still names them.')

section('4. نقاشی زنده — LIVE DRAWING')
row('mission_start', S('mission_start_line', '…'))
row('item_arrived', S('alive_arrived', '…'))
row('mission_done', S('ms_mission_done'))
W('')
W('  mission_start and item_arrived also carry a name on screen. Record them')
W('  generically, e.g. «بریم سراغ ماموریت بعدی!» and «وااای! نقاشیت زنده شد و')
W('  اومد تو صحنه!»')

section('5. RANG-AMIZI — COLORING')
row('paint_right', S('paint_right', '…'))
row('paint_wrong', S('paint_wrong', '…'))
row('paint_done', S('paint_all_right'))

section('6. BAZI-E HOROOF — TRACING')
row('trace_done', S('trace_award'))
row('trace_more', S('trace_incomplete'))
W('')
W('  Plus one clip per glyph, named by its Unicode code point, spoken when the')
W('  stage opens:')
letters = re.search(r'<string-array name="trace_letters">(.*?)</string-array>',
                    io.open(ROOT + '/res/values/arrays.xml', encoding='utf-8').read(), re.S)
glyphs = re.findall(r'<item>(.*?)</item>', letters.group(1)) if letters else []
line = '    '
for g in glyphs:
    line += 'letter_%x (%s)  ' % (ord(g[0]), g)
    if len(line) > 66:
        W(line.rstrip())
        line = '    '
if line.strip():
    W(line.rstrip())
W('    digit_0  digit_1  digit_2  digit_3  digit_4')
W('    digit_5  digit_6  digit_7  digit_8  digit_9')

section('7. BAZI-E HAFEZE — MEMORY')
row('memory_match', S('memory_match'))
row('memory_miss', S('memory_miss'))
row('memory_win', S('memory_win'))
row('memory_next', S('memory_next_level'))

section('8. BUBBLES AND FREE DRAWING')
row('bubble_pop', S('bubbles_pop_line'))
row('draw_empty', S('freedraw_empty'))

section('9. STAGE LISTS')
row('stage_locked', S('stage_locked'))

section('10. BEDTIME — لالایی شبانه')
row('night_hello', S('lullaby_greeting'))
row('night_goodnight', S('lullaby_goodnight'))
row('night_missing', S('lullaby_missing', '…'))
W('')
W('  And the lullabies themselves — full songs, any length:')
cat = io.open(ROOT + '/java/ir/brandimo/pashmak/data/catalog/LullabyCatalog.java',
              encoding='utf-8').read()
for clip, title in re.findall(r'new Lullaby\("([^"]+)", "([^"]+)"', cat):
    row(clip, title)
W('')
W('  The titles and the verse under each one live in')
W('  data/catalog/LullabyCatalog.java — change them there if you record')
W('  different songs.')

section('11. STORIES  (one clip per line Pashmak narrates)')
W('  Optional: a story reads perfectly well from the bubble alone. Record these')
W('  only if you want the stories narrated aloud.')
story_src = io.open(ROOT + '/java/ir/brandimo/pashmak/data/catalog/StoryCatalog.java',
                    encoding='utf-8').read()
blocks = re.split(r'Story \w+ = new Story\(', story_src)[1:]
for block in blocks:
    head = re.match(r'"([^"]+)", "([^"]+)"', block)
    sid, title = head.group(1), head.group(2)
    W('')
    W('  %s — %s' % (title, sid))
    beats = re.findall(
        r'StoryBeat\.(narrate|narrateTo|ask|choice|celebrate)\(\s*\n?\s*"((?:[^"\\]|\\.)*)"',
        block)
    praises = re.findall(
        r'StoryBeat\.ask\(\s*\n?\s*"(?:[^"\\]|\\.)*",\s*\n?\s*"[^"]*",\s*"((?:[^"\\]|\\.)*)"',
        block)
    praise_iter = iter(praises)
    for i, (kind, text) in enumerate(beats):
        W('    %-20s %s' % ('story_%s_%d' % (sid, i), text))
        if kind == 'ask':
            try:
                W('    %-20s %s' % ('story_%s_%d_yes' % (sid, i), next(praise_iter)))
            except StopIteration:
                pass
W('')
row('story_wrong', S('story_wrong'))
row('story_end', S('story_finished'))

section('12. SOUND EFFECTS  (short, under a second)')
W('  sfx_pop       a bubble bursting')
W('  sfx_flip      a memory card turning over')
W('  sfx_match     a correct answer')
W('  sfx_star      a star being awarded')
W('  sfx_shutter   the camera taking a photo')
W('  sfx_wrong     a gentle "not that one" — never harsh')
W('  sfx_brush     a pencil stroke on paper')
W('  sfx_tap       any button')
W('  sfx_whoosh    a screen or an item moving')
W('  sfx_fanfare   a mission or a story finishing')

W('')
io.open(ROOT + '/res/raw/audio_manifest.txt', 'w', encoding='utf-8').write('\n'.join(out))
print('wrote', len(out), 'lines')
