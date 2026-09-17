# دنیای بازی پشمک — Pashmak Game

An Osmo-inspired Persian (RTL) learning game for children aged 3–8, built as a native
Android app in Java. The child draws on real paper, scans the drawing with the tablet
camera, and watches it come to life beside Pashmak — an original orange monster who
talks, cheers and never scolds.

Ported from an HTML/CSS prototype produced in Claude Design; the prototype's palette,
copy, mission data and mascot geometry are reproduced here, and everything it only
simulated (camera, extraction, persistence, gallery) is implemented for real.

---

## Opening the project

1. Android Studio → **Open** → select this folder.
2. Let Gradle sync. The Gradle **wrapper JAR is not committed** (binary), so on first
   open either let Android Studio generate it, or run `gradle wrapper` once if you have
   Gradle installed. `gradle/wrapper/gradle-wrapper.properties` already pins 8.7.
3. Build → **Build APK**, or `./gradlew assembleDebug` once the wrapper exists.

| | |
|---|---|
| Language | 100% Java (17 source/target, core library desugaring on) |
| minSdk / targetSdk | **21** / 35 |
| Orientation | Both, on phone and tablet; RTL-first |
| Architecture | MVVM — ViewModel + LiveData + ViewBinding |
| Package | `ir.brandimo.pashmak` |

Key dependencies: AppCompat, Material, ConstraintLayout, RecyclerView, Lifecycle
(ViewModel/LiveData), Room, CameraX (core/camera2/lifecycle/view), Glide, ExifInterface.

---

## What's in it

**نقاشی زنده — Live Drawing.** 20 themed missions, 4 hand-drawn items each. The flow is
brief → camera → extraction → the drawing arriving in the mission's own world → repeat.
Every mission has its own scene — a bedroom, the seabed, deep space — and each item has
a place reserved in it, so a scanned bed lands on the floor and a scanned window on the
wall. The cut-out flies in along an arc, settles with a bounce, then breathes gently
while Pashmak cheers it by name. Finishing a collection fires confetti, awards stars and
saves the composed scene to the gallery.
Progress auto-saves after every scan; a child can leave mid-mission and come back, or
tap a filled slot to re-scan just that item.

**Mini-games, all staged.** Coloring, tracing and memory each open on a named stage
list built the same way the missions list is, so the child picks a stage by its name
and its picture rather than hunting through chips inside the game: 60 coloring pages
across themed packs, one stage per Persian letter and per digit (42 in all), and 30
memory boards — six themed decks, five levels each, unlocked in order, plus a seventh
deck dealt from the child's own scanned drawings once they have made a few. Alongside them:
free drawing with real pencils, and bubble pop — which now plays in rounds, each with
one rule announced before it starts: pop only «چ», only «۷», only the letters, only the
digits. The hunted glyph is salted into the field so it is always there to find, and
popping the wrong bubble costs nothing — it is a small child's game, not a test.

**قصه‌ی زنده — Live stories.** Twenty of them, each its own stage, each happening in a
drawn place — a seabed, a bakery, the surface of the moon — painted by the same
`ScenePainter` the live-drawing missions use, so a story kitchen and a mission kitchen
are the same kitchen. Each runs a minute or two and stops six times: three things to
find in the picture and two decisions that really change what happens next, so it is a
conversation rather than a page of text. Pashmak reads every passage aloud and the words
appear in step with his voice rather than all at once, so a child who is still learning
to read can follow the line being spoken.

The branch targets are beat indices, and a story that jumps into the middle of the wrong
scene is exactly the fault a read-through misses, so the beats are generated from a spec
in which a branch names a label: every jump, every prop a beat asks the child to find,
and every reachable beat is checked when `StoryCatalog.java` is written, and
`tools/storyfit.py` then checks the passages actually fit the box that prints them.

**قصه‌های پشمک — Told tales.** Forty more stories, 80 to 108 seconds each, that ask
nothing of the child: Pashmak simply tells them, a passage at a time, and the scene
changes under him as the tale moves — a river at night, a meadow at noon, a room by
lamplight, cross-faded rather than cut. There is play/pause, back and forward a passage,
and the next tale at the end. Nothing is locked; a tired child should not have to earn a
bedtime story. 578 passages, 5,740 words, about an hour of telling in all, and the ten
places are built by `TaleSceneKit` from a handful of moods, so each tale gets its own set
rather than sharing one generic backdrop.

And the thing the tale is about is *in* the picture, moving. That was the fault worth
fixing: a place with nothing in it meant the brave snail's story showed an empty meadow,
and half the cards in the picker were a plain grey circle. Each tale names its subject
once — one of 23 drawings added for them, a snail, a sheep, a turtle, a teapot, a loaf —
and it becomes both the card and the figure walking, drifting, bobbing or turning in
every scene of that tale. The motion is computed from elapsed time rather than stored, so
a dropped frame or a rotation changes nothing.

**لالایی شبانه — Bedtime.** A night screen with its own sky: ten lullabies, repeat-one,
automatic advance to the next, and a sleep timer (۱۵/۳۰/۶۰ minutes) that fades the last
minute out rather than cutting the song off. The background loop is silenced here — the
lullaby is the sound on this screen.

**Gallery.** Every finished scene, drawing and coloring page, with a full-screen
viewer, share, delete, and one-tap export to `Pictures/PashmakGame` in the device
gallery.

**Parent gate.** An arithmetic question written out in Persian words guards the
settings (difficulty, voice, music, reset stars). Regenerated every attempt.

---

## How a few things work

**Drawing extraction** (`vision/DrawingExtractor.java`) is pure Java on `Bitmap` — no
OpenCV, nothing native to ship. A photo of paper on a desk is never evenly lit, so a
global threshold is useless; instead each pixel is compared against the mean of its own
neighbourhood (Bradley-Roth, via an integral image), which is invariant to a smooth
lighting gradient, with a second absolute-contrast test so blank paper texture isn't
amplified into speckle. Colour is judged on an ink channel of `255 - min(R,G,B)` rather
than luma, so a yellow marker survives as strongly as a black one. Then morphological
open/close, connected components, paper-edge rejection, and a soft alpha ramp for
anti-aliased strokes. Roughly a quarter-second on a mid-range tablet.

**The mascot** (`mascot/`) is a custom Canvas `View`, not Lottie — there were no
animation files in the design. It draws directly in the prototype's 200×224 SVG
viewBox and scales with a single matrix, so one set of shapes serves every size from
the splash hero down to the docked companion. His mouth is driven by the speech bubble's
typewriter rather than by the pose, so the lips move for exactly as long as words are
appearing. `MascotAnims` transcribes the original CSS `@keyframes` stop for stop, and
every body part samples one shared clock, which is what lets them each run at their own
period the way independent CSS animations do. Seven states: idle, wave, talk, cheer,
encourage, tickle, enter. The system "remove animations" setting is honoured.

**The bedtime player** (`audio/LullabyPlayer.java`) owns one `MediaPlayer` and a single
500 ms tick that drives the progress bar, the sleep timer and the fade together. The
recordings are dropped into `res/raw` by hand, so every lookup goes through
`Resources#getIdentifier`: a lullaby with no file yet is skipped rather than thrown, and
Pashmak mentions it once instead of on every skip — an empty `res/raw` leaves the screen
usable. Repeat, auto-advance and the chosen timer are remembered between nights.

**Persistence.** Room stores only what the child makes — captured items and gallery
entries, as file paths, never blobs. The 20 missions are immutable content and live in
`res/values/arrays.xml`; mission progress is *derived* (`SELECT missionIndex, COUNT(*)
… GROUP BY missionIndex`) so it can never drift out of step with the files on disk.
Stars, mute, difficulty and the welcome flag are SharedPreferences.

---

## Audio

**Pashmak carries his own voice.** Every line he has — 1,136 of them, from the
greeting to all 578 passages of the told tales — is a file in `res/raw`, synthesised
at build time by `tools/gen_voice.py`. The app talks on any device, offline, with
nothing to install and nothing to download.

It got there the long way round, and the short version is worth knowing. The first
attempt used the device's own text-to-speech, which is the obvious thing to do and
does not work: Google's engine has no Persian voice, and neither do Samsung's or
Xiaomi's. Asking every installed engine instead of just the default helped some
devices and not the one in front of the child. Offering to install eSpeak NG helped
nobody who did not want a second app. eSpeak NG *does* speak Persian, so the third
answer was to stop asking the phone for a voice and carry one: run eSpeak here, once,
and ship the result.

It is a synthesiser and it sounds like one. That was the trade — a plain voice that is
always there is worth more to a three-year-old than a better one that is usually
absent — and it is reversible line by line, because a file dropped in under the same
name wins over the generated one.

The device's speech engine is still there behind it (`audio/SpeechEngine`, pitch 1.2,
rate 0.92, Google's engine asked first by name, then the default, then everything
else) but it is now only a fallback for a line whose clip is missing.

**The background music and the sound effects ship too.** Three seamless loops from
`tools/gen_bgm.py` — the join is made by folding the ringing tail back over the
opening, and the tool fails if the step across the join is audible — and ten short
effects from `tools/gen_sfx.py`. Background music was also *off by default*, which is
why a fresh install opened in silence whatever else was fixed; it is on now, like the
sound effects and the speech always were.

**Every sound still has a file name, and they are all in one list.** Nothing in the app
speaks anonymously — `MascotController.say()` requires a clip name, so a new screen
cannot ship a mouth moving over silence. The complete list, each name with the exact
Persian line it reads and where it is heard, is
[`app/src/main/res/raw/audio_manifest.txt`](app/src/main/res/raw/audio_manifest.txt).
Names are lowercase with underscores or aapt will reject them; `.mp3` or `.ogg`.

**Background music** is three files:

| file | where it plays |
|---|---|
| `bgm_menu` | splash, home, missions list, games menu, gallery, settings |
| `bgm_play` | every mini-game and the whole live-drawing flow |
| `bgm_story` | the story screen, which wants something softer |

Loop them seamlessly; 60–120 seconds each is plenty. **Music plays out of the box even
with none of them present** — `audio/MusicEngine` synthesises a soft wordless loop with
`AudioTrack`, a pentatonic phrase over a slow drone, rendered once and looped. Drop a
real file in and it is used instead, with no code change. Either way the music ducks to
a whisper whenever Pashmak speaks and comes back up afterwards. The bedtime screen asks
for silence — the lullaby is the sound there.

**Checking your files landed.** The parent screen (behind the gate) reports the music
and the voice clips separately, naming a few it cannot find, so a wrong file name shows
up immediately rather than as a character who goes quiet in one place.

`tools/voicefit.py` is the check that decides whether Pashmak speaks at all: a clip is
resolved by name through `Resources#getIdentifier`, and a name that is not there is a
silent no-op — no crash, no log, nothing. So it works out every name the app can ask
for, the same way the app works them out, and matches them against the files on disk.
It found `tale_done` missing on the first run.

---

## Layout notes

The app forces the `fa-IR` locale in `attachBaseContext`, so it lays out right-to-left on
any device rather than inheriting the phone's language — without that, an English-locale
device mirrors the whole design the wrong way.

Pashmak is placed in the control column on screens that have one, and pinned to the free
corner elsewhere, so he can never end up sitting on a button.

The prototype was a 392×820 portrait phone mock. Landscape is the app's home ground —
a game a child props up on a table — so that is the design that was drawn first: the
recurring portrait pattern of header, flexible body and pinned footer becomes a
three-band layout of a start rail, the centre stage and an end column of actions. But a
child turns the tablet, so the app turns with them.

**Whichever edge is short is the one to spend carefully.** `smallestWidth` is the
device's shorter edge and does not change when the screen is turned, so the four buckets
— `values/` (tuned for the smallest phone, since it is also the fallback),
`values-sw360dp`, `values-sw600dp`, `values-sw720dp` — are device sizes, not
orientations. What flips is which axis is scarce: on its side a small phone has about
320dp of height and plenty of width; upright it has 320dp of width and plenty of height.
The same number serves both. Anything added to one bucket belongs in all four.

**Portrait turns the end column into a bottom band.** `res/layout-port/` holds a variant
of every screen that has a side column: the stage takes the full width, and Pashmak, the
text and the buttons run across the bottom. On a phone that band is two rows rather than
one — Pashmak and the words on top, the buttons full width beneath — because three
things across 300dp left the line of Persian 78dp wide, narrower than the column it was
replacing. `tools/portfit.py` is what said so.

Two things about that folder are easy to get wrong, so both are checked rather than
remembered. `smallestWidth` outranks orientation in the resource qualifier order, so on
a tablet held upright `layout-sw600dp/` beats `layout-port/` — any screen with a sw600
variant needs a `layout-sw600dp-port/` twin or the tablet gets the sideways design.
And ViewBinding merges every variant of a layout into one class, where an id missing
from any one of them becomes a nullable field: the screen still compiles and then throws
the first time it is opened the other way up. So the variants must carry exactly the
same ids, and `portfit` fails if they do not.

Phone and tablet share layouts except where the structure genuinely has to change, which
so far is one screen: the home screen's five buttons are two-up on a phone and a single
tall column in `layout-sw600dp/`. The games menu adapts through a resource instead — a
phone drops the headline card's full-width span (`@bool/games_wide_headline`) so all
nine cards land in three rows and nothing scrolls. Grid columns are no longer a fixed
number per bucket: that was sound while the width was always the long edge, but upright
the same device has half of it, so a grid now takes the span it was designed for or
fewer, whichever the measured width will actually hold at a readable column. The story screen does something
similar: on a phone its side column has to hold Pashmak, the passage being read and two
option buttons in 266dp, so `@bool/story_dock_small` gives him the smaller dock there
and the passage gets the room instead.

**Rotation rebuilds the screen, which is its own kind of bug.** Android destroys and
recreates an Activity when the device turns, and several screens kept what the child was
doing in Activity fields — so a story would start again from its first line and a bubble
game would reshuffle its rules and reset the score. Those now go in the instance state,
and the bubble rounds are rebuilt from a saved seed so they come back the same.

Three faults of a sharper kind came out of the same change: a screen that awards stars
from a LiveData observer is handed the value it was already showing when it is rebuilt,
so a finished coloring page, a finished memory board and a finished mission all paid out
again on every rotation — and the mission wrote a second copy of its scene to the gallery
each time. Those claims moved into the ViewModels, which survive the rebuild. The
bedtime player moved there too: the screen is explicit that a lullaby keeps playing when
the screen dims, and rotating used to stop it. And because `PaintCanvasView` paints into
a bitmap rather than keeping a list of strokes, a drawing cannot travel in a Bundle;
`CanvasKeeper` holds it across the rebuild and the drawing is fitted into the new shape
of the paper instead of being cropped.

Because there is no way to eyeball every screen on every device, `tools/vfit.py`
walks each layout and works out the height it cannot do without — reading `ScrollView`,
`layout_weight`, `GridLayout` wrapping and ConstraintLayout's vertical chains — and
fails if any screen needs more than the shortest device in its bucket has. It resolves
layouts the way the platform does, so it measures each screen both ways up.
`tools/gridfit.py` does the same for the games grid, which `vfit` cannot judge
because the grid is a flexible `0dp` RecyclerView by construction. `tools/roi_check.py` covers the camera's
region-of-interest arithmetic, the piece that file itself calls most likely to be
subtly wrong. `tools/hfit.py` guards the
other axis — what each grid column is left with once the rail has taken its share.
`tools/storyfit.py` reads the real story passages out of the catalogue and checks they
fit the narration box — a check for content against its container, which none of the
others do, and which caught the box collapsing below its own minimum on a choice beat.
`tools/gen_faces.py` draws the icon set and `tools/actorfit.py` mirrors the placement
maths and renders it, because neither can be judged by reading the code. Between them
they caught a turtle whose head and legs were buried under its own shell, a spider with
dark eyes on a dark head, a spoon that read as a maraca, a subject drawn at a fifth of
the frame — a sticker stuck on a landscape rather than what the story is about — and a
kite that began its drift off the edge, so the first seconds of those tales showed an
empty sky. `tools/portfit.py` covers what only portrait can break: that the variants match id for
id, that every resource the new files name exists, and that a bottom band leaves its
line of Persian a width worth reading. `tools/scenes.py` and `tools/sheet.py` render the
story scenes and the icon set to contact sheets, so artwork gets looked at rather than
assumed.

Every scrolling list sits in a box of its own — a translucent panel the cards are
clipped to. The list screens give Pashmak a rail beside that box rather than the bottom
corner: on its side, height is the scarce dimension and width is not, so the rail spends
the plentiful one — and upright, where that is reversed, the rail lies down and becomes a
band along the bottom instead. It also removes a whole class of bug — the lists used to
pair `clipToPadding="false"` with a bottom padding that was meant to reserve space for
him, which does the opposite of reserving it: the padding becomes scroll room the list
still paints into, so cards slid straight across him.

Pashmak sits at the top of the tool column on the screens that have one, in that rail on
the list screens, and in the free bottom corner on the two screens with neither. His speech bubble deliberately lives in the
screen's own root rather than inside that column: a bubble inside a column grows with
the length of the line and squeezes everything under it, which is exactly what used to
break the story screen.

---

## Attribution

Fonts: [Lalezar](https://github.com/BornaIz/Lalezar) and
[Vazirmatn](https://github.com/rastikerdar/vazirmatn), both SIL Open Font License.

Pashmak is an original character. The design brief referenced Monsters University, but
that character belongs to Disney/Pixar and is not reproduced here.
