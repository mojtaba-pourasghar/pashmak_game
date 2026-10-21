class_name CharacterFigure
extends Control
## One cast member standing in a scene.
##
## Drawn with the same vector approach as the player's avatar so the group
## reads as one art style. Each character has a fixed palette and silhouette
## so they stay recognisable across all ten chapters; `pose` tilts and shifts
## the figure for the handful of states the content asks for.
##
## These are original designs for Razino. The hand-painted sheets described in
## docs/ASSETS.md would replace this renderer for the shipping look.

const LOOKS := {
	"arad":    {"skin": "E8C19C", "hair": "3B2A1E", "outfit": "4E7C59", "height": 1.00},
	"raha":    {"skin": "D2A077", "hair": "1C1A1A", "outfit": "A8477C", "height": 0.99},
	"mani":    {"skin": "F2D6BE", "hair": "6B4327", "outfit": "8A5A2B", "height": 0.92},
	"azimi":   {"skin": "E8C19C", "hair": "8A8FA3", "outfit": "3C5A99", "height": 1.04},
	"modir":   {"skin": "D2A077", "hair": "5A5F7D", "outfit": "2E3360", "height": 1.06},
	"sharifi": {"skin": "E8C19C", "hair": "BFC2D4", "outfit": "5F6B4A", "height": 1.02},
	"banoo":   {"skin": "B2784F", "hair": "3B2A1E", "outfit": "3F6B4F", "height": 1.01},
	"ostad":   {"skin": "B2784F", "hair": "4A4A4A", "outfit": "7A4B2A", "height": 1.03},
	"parisa":  {"skin": "E8C19C", "hair": "2A2320", "outfit": "2E8B8B", "height": 1.01},
	"gholami": {"skin": "D2A077", "hair": "9A9A9A", "outfit": "4A5260", "height": 1.02},
	"sara":    {"skin": "D2A077", "hair": "5A3A22", "outfit": "C96A6A", "height": 0.99},
	"navid":   {"skin": "E8C19C", "hair": "2B2B2B", "outfit": "D9A45B", "height": 0.98},
	"hamid":   {"skin": "E8C19C", "hair": "7A7A7A", "outfit": "3A4A5A", "height": 1.02},
	"sohrab":  {"skin": "B2784F", "hair": "2B2B2B", "outfit": "C87A33", "height": 1.03},
	"delaram": {"skin": "E8C19C", "hair": "4A3426", "outfit": "6B4E8C", "height": 1.01},
}

@export var character_id: String = "":
	set(value):
		character_id = value
		queue_redraw()

@export var pose: String = "idle":
	set(value):
		pose = value
		queue_redraw()

var _speaking: bool = false
var _t: float = 0.0

func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	set_process(not Settings.reduce_motion)

func _process(delta: float) -> void:
	_t += delta
	queue_redraw()

func set_speaking(value: bool) -> void:
	if _speaking != value:
		_speaking = value
		queue_redraw()

func _draw() -> void:
	# فندق is the group's fennec guide, not a child — drawn differently.
	if character_id == "fandogh":
		_draw_fennec()
		return
	if character_id == "player":
		return  # the player is represented by their own avatar elsewhere

	var look: Dictionary = LOOKS.get(character_id, {"skin": "E8C19C", "hair": "3B2A1E", "outfit": "5A5F7D", "height": 1.0})
	var skin := Color(String(look["skin"]))
	var hair := Color(String(look["hair"]))
	var cloth := Color(String(look["outfit"]))

	var h := size.y * float(look.get("height", 1.0))
	var unit := h / 10.0
	var cx := size.x * 0.5
	var base := size.y

	var bob := 0.0
	var lean := 0.0
	match pose:
		"talk", "happy":
			bob = sin(_t * 3.0) * unit * 0.05 if _speaking else 0.0
		"think":
			lean = -unit * 0.12
		"surprised":
			bob = -unit * 0.10
	if Settings.reduce_motion:
		bob = 0.0

	var head_r := unit * 1.25
	var head_y := base - h + head_r + bob
	var torso_top := head_y + head_r * 0.8
	var torso_h := unit * 3.0
	var torso_w := unit * 2.2

	# legs
	var leg_w := unit * 0.75
	var leg_top := torso_top + torso_h
	_rounded(Rect2(cx - leg_w * 1.1, leg_top, leg_w, unit * 2.2), leg_w * 0.3, cloth.darkened(0.4))
	_rounded(Rect2(cx + leg_w * 0.1, leg_top, leg_w, unit * 2.2), leg_w * 0.3, cloth.darkened(0.4))
	_rounded(Rect2(cx - leg_w * 1.2, leg_top + unit * 2.05, leg_w * 1.15, unit * 0.45), unit * 0.18, Palette.INK)
	_rounded(Rect2(cx + leg_w * 0.05, leg_top + unit * 2.05, leg_w * 1.15, unit * 0.45), unit * 0.18, Palette.INK)

	# torso and arms
	_rounded(Rect2(cx - torso_w * 0.5 + lean, torso_top, torso_w, torso_h), unit * 0.5, cloth)
	var arm_w := unit * 0.6
	var arm_lift := -unit * 0.5 if pose == "surprised" else 0.0
	_rounded(Rect2(cx - torso_w * 0.5 - arm_w * 0.8 + lean, torso_top + unit * 0.2 + arm_lift, arm_w, torso_h * 0.75),
		arm_w * 0.5, cloth.darkened(0.14))
	_rounded(Rect2(cx + torso_w * 0.5 - arm_w * 0.2 + lean, torso_top + unit * 0.2 + arm_lift, arm_w, torso_h * 0.75),
		arm_w * 0.5, cloth.darkened(0.14))

	# head
	draw_circle(Vector2(cx + lean, head_y), head_r, skin)
	draw_arc(Vector2(cx + lean, head_y), head_r * 0.98, PI, TAU, 24, hair, head_r * 0.42, true)

	# eyes — wider when surprised, which is the whole expression budget here
	var eye_r := head_r * (0.19 if pose == "surprised" else 0.14)
	for s in [-1.0, 1.0]:
		var p := Vector2(cx + lean + head_r * 0.34 * s, head_y - head_r * 0.04)
		draw_circle(p, eye_r, Color(0.99, 0.98, 0.96))
		draw_circle(p, eye_r * 0.55, Palette.INK)

	# mouth
	var mouth := Vector2(cx + lean, head_y + head_r * 0.48)
	if pose == "surprised":
		draw_circle(mouth, head_r * 0.14, Palette.INK)
	elif _speaking:
		# A simple open/closed cycle in step with the voice line.
		var open := (sin(_t * 9.0) * 0.5 + 0.5) * head_r * 0.16 + head_r * 0.04
		_rounded(Rect2(mouth.x - head_r * 0.16, mouth.y - open * 0.5, head_r * 0.32, open), open * 0.5, Palette.INK)
	else:
		draw_arc(mouth, head_r * 0.3, PI * 0.2, PI * 0.8, 12, Palette.INK, head_r * 0.07)

	if _speaking:
		# A soft ring so a child always knows who is talking.
		draw_arc(Vector2(cx + lean, head_y), head_r * 1.35, 0, TAU, 32, Palette.BRASS, 3.0)

func _draw_fennec() -> void:
	## فندق — the group's fennec guide. Large ears, small body, warm sand
	## colours. An original design, not modelled on any existing character.
	var unit := size.y / 10.0
	var cx := size.x * 0.5
	var base := size.y
	var body := Color("D9A45B")
	var cream := Color("F4E9D8")

	var body_c := Vector2(cx, base - unit * 2.2)
	draw_circle(body_c, unit * 1.5, body)
	draw_circle(body_c + Vector2(0, unit * 0.3), unit * 1.0, cream)
	# tail
	draw_circle(body_c + Vector2(-unit * 1.6, unit * 0.2), unit * 0.7, body.darkened(0.08))
	draw_circle(body_c + Vector2(-unit * 2.1, -unit * 0.1), unit * 0.45, cream)

	var head_c := body_c + Vector2(0, -unit * 2.0)
	var head_r := unit * 1.15
	# ears — the fennec's signature
	for s in [-1.0, 1.0]:
		var tip := head_c + Vector2(s * head_r * 0.75, -head_r * 2.0)
		draw_colored_polygon(PackedVector2Array([
			head_c + Vector2(s * head_r * 0.25, -head_r * 0.6),
			head_c + Vector2(s * head_r * 1.05, -head_r * 0.5),
			tip,
		]), body)
		draw_colored_polygon(PackedVector2Array([
			head_c + Vector2(s * head_r * 0.38, -head_r * 0.62),
			head_c + Vector2(s * head_r * 0.88, -head_r * 0.56),
			tip * 0.92 + head_c * 0.08,
		]), Color("E8C7A0"))
	draw_circle(head_c, head_r, body)
	draw_circle(head_c + Vector2(0, head_r * 0.35), head_r * 0.6, cream)
	for s in [-1.0, 1.0]:
		draw_circle(head_c + Vector2(s * head_r * 0.36, -head_r * 0.08), head_r * 0.16, Palette.INK)
		draw_circle(head_c + Vector2(s * head_r * 0.36 + head_r * 0.05, -head_r * 0.13), head_r * 0.05, Color.WHITE)
	draw_circle(head_c + Vector2(0, head_r * 0.42), head_r * 0.12, Palette.INK)
	if _speaking:
		draw_arc(head_c, head_r * 1.5, 0, TAU, 32, Palette.BRASS, 3.0)

func _rounded(rect: Rect2, radius: float, colour: Color) -> void:
	radius = minf(radius, minf(rect.size.x, rect.size.y) * 0.5)
	if rect.size.x <= 0.0 or rect.size.y <= 0.0:
		return
	draw_rect(Rect2(rect.position + Vector2(radius, 0), Vector2(rect.size.x - radius * 2.0, rect.size.y)), colour)
	draw_rect(Rect2(rect.position + Vector2(0, radius), Vector2(rect.size.x, rect.size.y - radius * 2.0)), colour)
	for corner in [
		rect.position + Vector2(radius, radius),
		rect.position + Vector2(rect.size.x - radius, radius),
		rect.position + Vector2(radius, rect.size.y - radius),
		rect.position + Vector2(rect.size.x - radius, rect.size.y - radius),
	]:
		draw_circle(corner, radius, colour)
