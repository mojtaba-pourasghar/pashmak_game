class_name Palette
extends RefCounted
## Razino's visual identity. Every colour, radius and spacing used by the UI
## comes from here, so the look stays one system across all sixteen screens.
##
## The palette is original to Razino: a lamplit night-blue city, warm brass
## clockwork, and parchment for anything the player reads.

# — surfaces —————————————————————————————————————————————
const NIGHT        := Color("1B1F3B")  # deepest background
const NIGHT_SOFT   := Color("262B4D")  # raised panel
const NIGHT_LIFT   := Color("333A63")  # hovered / selected panel
const PARCHMENT    := Color("F4E9D8")  # journal pages, dialogue box
const PARCHMENT_DIM:= Color("E2D4BC")

# — brand —————————————————————————————————————————————————
const BRASS        := Color("D9A45B")  # primary action, clock motifs
const BRASS_DEEP   := Color("B3813C")
const TEAL         := Color("2E8B8B")  # secondary action, discovery
const TEAL_DEEP    := Color("1F6565")
const ROSE         := Color("C96A6A")  # unsolved clue, gentle warning

# — text ——————————————————————————————————————————————————
const INK          := Color("22223A")  # on parchment
const INK_SOFT     := Color("4A4A63")
const SNOW         := Color("F7F4EE")  # on night
const SNOW_SOFT    := Color("BFC2D4")

# — state —————————————————————————————————————————————————
const OK           := Color("5FA86A")
const LOCKED       := Color("5A5F7D")

# — geometry ——————————————————————————————————————————————
const R_SM := 8.0
const R_MD := 16.0
const R_LG := 24.0

## Minimum touch target in logical units. GDD §7.2 asks for ~48.
const TOUCH_MIN := 56.0

const PAD_XS := 6
const PAD_SM := 12
const PAD_MD := 20
const PAD_LG := 32

# — type scale (logical units at 1280x720) ————————————————
const FS_DISPLAY := 52
const FS_TITLE   := 36
const FS_HEADING := 28
const FS_BODY    := 24
const FS_LABEL   := 20
const FS_CAPTION := 17
