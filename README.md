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
| Orientation | Landscape only, RTL-first |
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
free drawing with real pencils, bubble pop, and an interactive story whose props answer
back when poked.

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

**Every sound has a file name, and they are all in one list.** Nothing in the app
speaks anonymously — `MascotController.say()` requires a clip name, so a new screen
cannot ship a mouth moving over silence. The complete list of 146 file names, each with
the exact Persian line it reads and where it is heard, is
[`app/src/main/res/raw/audio_manifest.txt`](app/src/main/res/raw/audio_manifest.txt).
Drop the recordings into `app/src/main/res/raw/` under those names — `.mp3` or `.ogg`,
lowercase with underscores or aapt will reject them. **No recordings ship with this
repo and none is required**: every clip is resolved by name through
`Resources#getIdentifier`, so a missing file is a silent no-op and the words still
appear in the speech bubble.

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

**Checking your files landed.** The parent screen (behind the gate) reports how many of
the 146 clips it can actually find and names a few of the ones it cannot, so a wrong
file name shows up immediately instead of as a character that never speaks.

---

## Layout notes

The app forces the `fa-IR` locale in `attachBaseContext`, so it lays out right-to-left on
any device rather than inheriting the phone's language — without that, an English-locale
device mirrors the whole design the wrong way.

Pashmak is placed in the control column on screens that have one, and pinned to the free
corner elsewhere, so he can never end up sitting on a button.

The prototype was a 392×820 portrait phone mock; this app is landscape-locked. The
recurring portrait pattern — header, flexible body, pinned footer — becomes a three-band
layout: a start rail, the centre stage, and an end column of actions.

**Height is the scarce dimension.** Landscape-locked means a device's `smallestWidth`
*is* its height: about 320dp on a small phone, 360dp on a common one, 600dp on a 7″
tablet and 800dp on a 10″. So there are four dimension buckets — `values/` (tuned for
the smallest phone, since it is also the fallback), `values-sw360dp`, `values-sw600dp`
and `values-sw720dp` — covering type scale, touch targets, mascot sizes and grid spans.
Anything added to one belongs in all four.

Phone and tablet share layouts except where the structure genuinely has to change, which
so far is one screen: the home screen's five buttons are two-up on a phone and a single
tall column in `layout-sw600dp/`. The games menu adapts through a resource instead — a
phone drops the headline card's full-width span (`@bool/games_wide_headline`) so all
eight cards land in three rows and nothing scrolls.

Because there is no way to eyeball every screen on every device, `tools/vfit.py`
walks each layout and works out the height it cannot do without — reading `ScrollView`,
`layout_weight`, `GridLayout` wrapping and ConstraintLayout's vertical chains — and
fails if any screen needs more than the shortest device in its bucket has.
`tools/gridfit.py` does the same for the games grid, which `vfit` cannot judge
because the grid is a flexible `0dp` RecyclerView by construction.

---

## Attribution

Fonts: [Lalezar](https://github.com/BornaIz/Lalezar) and
[Vazirmatn](https://github.com/rastikerdar/vazirmatn), both SIL Open Font License.

Pashmak is an original character. The design brief referenced Monsters University, but
that character belongs to Disney/Pixar and is not reproduced here.
