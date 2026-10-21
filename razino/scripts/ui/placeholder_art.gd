class_name PlaceholderArt
extends Control
## A clearly-marked stand-in for a background that has not been painted yet.
##
## This is NOT final art and must never be presented as such. It draws a
## layered dusk-to-night wash with a simple skyline derived from the
## background id, so scenes are distinguishable while playing, and it carries
## a visible badge saying it is temporary. docs/ASSETS.md lists every id that
## still needs a real painting.

const BADGE_TEXT := "دارایی موقت"

@export var background_id: String = "":
	set(value):
		background_id = value
		queue_redraw()

## Set false for screenshots of real art once it lands.
@export var show_badge: bool = true

## Horizontal parallax offset, in pixels, set by SceneStage.
##
## It is applied inside _draw rather than by moving this control: writing
## `position` on a control anchored to a full rect converts those anchors into
## fixed offsets at whatever size it happened to have that frame, which silently
## froze the background at the wrong height.
var drift: float = 0.0:
	set(value):
		drift = value
		queue_redraw()

func _ready() -> void:
	set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	resized.connect(queue_redraw)

func _draw() -> void:
	var r := size
	var h := _hue()

	# Sky: three bands, darkest at the top. These are deliberately well clear
	# of the UI's night blue — a placeholder that reads as "the background did
	# not load" is useless for telling two scenes apart while playing.
	var top := Color.from_hsv(h, 0.52, 0.34)
	var mid := Color.from_hsv(h, 0.46, 0.50)
	var low := Color.from_hsv(fposmod(h + 0.06, 1.0), 0.40, 0.68)
	draw_rect(Rect2(0, 0, r.x, r.y * 0.45), top)
	draw_rect(Rect2(0, r.y * 0.45, r.x, r.y * 0.22), mid)
	draw_rect(Rect2(0, r.y * 0.67, r.x, r.y * 0.33), low)

	# Far silhouette, then near silhouette. The nearer layer drifts further,
	# which is what reads as depth.
	_draw_skyline(r, 0.62, Color.from_hsv(h, 0.58, 0.26), 7, drift * 0.4)
	_draw_skyline(r, 0.78, Color.from_hsv(h, 0.62, 0.17), 5, drift)

	# Ground.
	draw_rect(Rect2(0, r.y * 0.86, r.x, r.y * 0.14), Color.from_hsv(h, 0.55, 0.12))

	if show_badge:
		_draw_badge(r)

func _draw_skyline(r: Vector2, baseline: float, colour: Color, count: int, offset: float = 0.0) -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = hash(background_id + str(count))
	var x := offset - 40.0
	var step := (r.x + 80.0) / float(count)
	for i in count:
		var w := step * rng.randf_range(0.6, 1.1)
		var hgt := r.y * rng.randf_range(0.07, 0.20)
		draw_rect(Rect2(x, r.y * baseline - hgt, w, hgt + r.y * 0.2), colour)
		x += step
	draw_rect(Rect2(0, r.y * baseline, r.x, r.y * (1.0 - baseline)), colour)

func _draw_badge(r: Vector2) -> void:
	var font := Style.font_medium if Style.font_medium != null else ThemeDB.fallback_font
	var fs := 18
	var text := "%s — %s" % [BADGE_TEXT, background_id]
	var text_size := font.get_string_size(text, HORIZONTAL_ALIGNMENT_RIGHT, -1, fs)
	var pad := 10.0
	# Bottom-left, clear of the gameplay HUD in the top corners.
	var box := Rect2(
		16, r.y - text_size.y - pad - 16,
		text_size.x + pad * 2, text_size.y + pad
	)
	draw_rect(box, Color(0, 0, 0, 0.55), true)
	draw_rect(box, Palette.ROSE, false, 2.0)
	draw_string(font, Vector2(box.position.x + pad, box.position.y + text_size.y + pad * 0.25),
		text, HORIZONTAL_ALIGNMENT_RIGHT, -1, fs, Palette.SNOW)

## Each background id gets its own stable hue, so a scene looks the same every
## time and two scenes rarely collide.
func _hue() -> float:
	if background_id.is_empty():
		return 0.62
	return fposmod(float(hash(background_id) % 1000) / 1000.0, 1.0)
