class_name AvatarPortrait
extends Control
## Draws the player's avatar from AvatarManager's parts.
##
## Original vector artwork — every shape here is drawn from primitives, so the
## avatar is consistent at any size and needs no sprite sheet. It is a real
## rendering of the chosen parts, not a placeholder box; the hand-painted
## character art in docs/ASSETS.md would replace it for the shipping look.

## Redraws automatically when the avatar changes.
@export var live: bool = true
## A gentle idle bob, disabled when the player asks for reduced motion.
@export var animate: bool = true

var _t: float = 0.0

func _ready() -> void:
	custom_minimum_size = Vector2(220, 300)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	if live:
		AvatarManager.avatar_changed.connect(func(_id): queue_redraw())
	set_process(animate and not Settings.reduce_motion)

func _process(delta: float) -> void:
	_t += delta
	queue_redraw()

func _draw() -> void:
	var w := size.x
	var h := size.y
	var unit := minf(w, h) / 10.0
	var bob := 0.0
	if animate and not Settings.reduce_motion:
		bob = sin(_t * 1.6) * unit * 0.08

	var cx := w * 0.5
	var head_r := unit * 1.5
	var head_y := h * 0.26 + bob
	var skin := AvatarManager.skin_color()
	var hair := AvatarManager.hair_color()
	var outfit_colour := _outfit_colour()

	# — body ————————————————————————————————————————————
	var torso_top := head_y + head_r * 0.85
	var torso_h := unit * 3.2
	var torso_w := unit * 2.6
	_rounded(Rect2(cx - torso_w * 0.5, torso_top, torso_w, torso_h), unit * 0.55, outfit_colour)

	# arms
	var arm_w := unit * 0.7
	_rounded(Rect2(cx - torso_w * 0.5 - arm_w * 0.85, torso_top + unit * 0.25, arm_w, torso_h * 0.78),
		arm_w * 0.5, outfit_colour.darkened(0.12))
	_rounded(Rect2(cx + torso_w * 0.5 - arm_w * 0.15, torso_top + unit * 0.25, arm_w, torso_h * 0.78),
		arm_w * 0.5, outfit_colour.darkened(0.12))
	# hands
	draw_circle(Vector2(cx - torso_w * 0.5 - arm_w * 0.35, torso_top + torso_h * 0.82), unit * 0.33, skin)
	draw_circle(Vector2(cx + torso_w * 0.5 + arm_w * 0.35, torso_top + torso_h * 0.82), unit * 0.33, skin)

	# legs
	var leg_w := unit * 0.85
	var leg_top := torso_top + torso_h
	var trousers := outfit_colour.darkened(0.35)
	_rounded(Rect2(cx - leg_w * 1.15, leg_top, leg_w, unit * 2.0), leg_w * 0.35, trousers)
	_rounded(Rect2(cx + leg_w * 0.15, leg_top, leg_w, unit * 2.0), leg_w * 0.35, trousers)
	# shoes
	_rounded(Rect2(cx - leg_w * 1.25, leg_top + unit * 1.85, leg_w * 1.2, unit * 0.5), unit * 0.2, Palette.INK)
	_rounded(Rect2(cx + leg_w * 0.05, leg_top + unit * 1.85, leg_w * 1.2, unit * 0.5), unit * 0.2, Palette.INK)

	# neck
	_rounded(Rect2(cx - unit * 0.4, head_y + head_r * 0.6, unit * 0.8, unit * 0.6), unit * 0.2, skin.darkened(0.08))

	# — head ————————————————————————————————————————————
	draw_circle(Vector2(cx, head_y), head_r, skin)
	# ears
	draw_circle(Vector2(cx - head_r * 0.95, head_y + head_r * 0.1), head_r * 0.22, skin.darkened(0.05))
	draw_circle(Vector2(cx + head_r * 0.95, head_y + head_r * 0.1), head_r * 0.22, skin.darkened(0.05))

	_draw_hair(cx, head_y, head_r, hair)
	_draw_eyes(cx, head_y, head_r)

	# nose and mouth — a small, friendly face, no expression beyond content
	draw_circle(Vector2(cx, head_y + head_r * 0.22), head_r * 0.09, skin.darkened(0.18))
	_smile(Vector2(cx, head_y + head_r * 0.52), head_r * 0.38, Palette.INK.lightened(0.1))

	_draw_accessory(cx, head_y, head_r, torso_top, torso_w, unit)

func _draw_hair(cx: float, cy: float, r: float, hair: Color) -> void:
	match AvatarManager.hair_style():
		"short":
			draw_arc(Vector2(cx, cy), r * 0.98, PI, TAU, 28, hair, r * 0.42, true)
		"wavy":
			draw_arc(Vector2(cx, cy), r * 1.0, PI * 0.9, TAU + PI * 0.1, 30, hair, r * 0.45, true)
			draw_circle(Vector2(cx - r * 0.85, cy + r * 0.25), r * 0.28, hair)
			draw_circle(Vector2(cx + r * 0.85, cy + r * 0.25), r * 0.28, hair)
		"curly":
			for i in 7:
				var a := PI + PI * (float(i) / 6.0)
				draw_circle(Vector2(cx + cos(a) * r * 0.86, cy + sin(a) * r * 0.86), r * 0.33, hair)
		"braid":
			draw_arc(Vector2(cx, cy), r * 0.98, PI, TAU, 28, hair, r * 0.40, true)
			_rounded(Rect2(cx + r * 0.72, cy + r * 0.1, r * 0.34, r * 1.5), r * 0.17, hair)
			draw_circle(Vector2(cx + r * 0.89, cy + r * 1.62), r * 0.2, hair.darkened(0.2))
		"ponytail":
			draw_arc(Vector2(cx, cy), r * 0.98, PI, TAU, 28, hair, r * 0.40, true)
			draw_circle(Vector2(cx + r * 1.0, cy - r * 0.15), r * 0.36, hair)
			_rounded(Rect2(cx + r * 0.82, cy - r * 0.1, r * 0.4, r * 1.1), r * 0.2, hair)
		"cap":
			draw_arc(Vector2(cx, cy), r * 0.98, PI, TAU, 28, hair.darkened(0.3), r * 0.42, true)
			# peak
			draw_rect(Rect2(cx - r * 1.25, cy - r * 0.42, r * 1.3, r * 0.26), Palette.BRASS)

func _draw_eyes(cx: float, cy: float, r: float) -> void:
	var eye_y := cy - r * 0.05
	var dx := r * 0.38
	var shape := AvatarManager.eye_shape()
	var white := Color(0.99, 0.98, 0.96)
	var iris := Palette.INK
	for s in [-1.0, 1.0]:
		var p := Vector2(cx + dx * s, eye_y)
		match shape:
			"round":
				draw_circle(p, r * 0.17, white)
				draw_circle(p, r * 0.095, iris)
			"almond":
				_rounded(Rect2(p.x - r * 0.19, p.y - r * 0.11, r * 0.38, r * 0.22), r * 0.11, white)
				draw_circle(p, r * 0.085, iris)
			"wide":
				draw_circle(p, r * 0.21, white)
				draw_circle(p, r * 0.10, iris)
			"soft":
				_rounded(Rect2(p.x - r * 0.17, p.y - r * 0.13, r * 0.34, r * 0.26), r * 0.13, white)
				draw_circle(p, r * 0.09, iris)
		# catchlight
		draw_circle(p + Vector2(r * 0.04, -r * 0.04), r * 0.03, Color.WHITE)
		# brow
		draw_line(Vector2(p.x - r * 0.18, p.y - r * 0.28), Vector2(p.x + r * 0.18, p.y - r * 0.32),
			AvatarManager.hair_color().darkened(0.15), r * 0.07)

func _draw_accessory(cx: float, head_y: float, head_r: float, torso_top: float, torso_w: float, unit: float) -> void:
	match AvatarManager.accessory():
		"glasses":
			var c := Palette.BRASS
			for s in [-1.0, 1.0]:
				draw_arc(Vector2(cx + head_r * 0.38 * s, head_y - head_r * 0.05), head_r * 0.26, 0, TAU, 20, c, head_r * 0.055)
			draw_line(Vector2(cx - head_r * 0.12, head_y - head_r * 0.05),
				Vector2(cx + head_r * 0.12, head_y - head_r * 0.05), c, head_r * 0.05)
		"scarf":
			_rounded(Rect2(cx - unit * 0.95, torso_top - unit * 0.12, unit * 1.9, unit * 0.52), unit * 0.22, Palette.ROSE)
			_rounded(Rect2(cx + unit * 0.45, torso_top + unit * 0.3, unit * 0.42, unit * 1.2), unit * 0.18, Palette.ROSE.darkened(0.1))
		"satchel":
			draw_line(Vector2(cx - torso_w * 0.45, torso_top + unit * 0.1),
				Vector2(cx + torso_w * 0.42, torso_top + unit * 1.6), Palette.BRASS_DEEP, unit * 0.22)
			_rounded(Rect2(cx + torso_w * 0.26, torso_top + unit * 1.45, unit * 1.0, unit * 0.85), unit * 0.18, Palette.BRASS_DEEP)
		"pocket_watch":
			draw_line(Vector2(cx + unit * 0.2, torso_top + unit * 0.5),
				Vector2(cx + unit * 0.75, torso_top + unit * 1.3), Palette.BRASS, unit * 0.09)
			draw_circle(Vector2(cx + unit * 0.78, torso_top + unit * 1.48), unit * 0.3, Palette.BRASS)
			draw_circle(Vector2(cx + unit * 0.78, torso_top + unit * 1.48), unit * 0.22, Palette.PARCHMENT)

func _outfit_colour() -> Color:
	match AvatarManager.outfit():
		"explorer": return Color("4E7C59")
		"scholar": return Color("3C5A99")
		"inventor": return Color("8A5A2B")
		"festival": return Color("A8477C")
		"night": return Color("2E3360")
	return Palette.TEAL

func _rounded(rect: Rect2, radius: float, colour: Color) -> void:
	radius = minf(radius, minf(rect.size.x, rect.size.y) * 0.5)
	draw_rect(Rect2(rect.position + Vector2(radius, 0), Vector2(rect.size.x - radius * 2.0, rect.size.y)), colour)
	draw_rect(Rect2(rect.position + Vector2(0, radius), Vector2(rect.size.x, rect.size.y - radius * 2.0)), colour)
	for corner in [
		rect.position + Vector2(radius, radius),
		rect.position + Vector2(rect.size.x - radius, radius),
		rect.position + Vector2(radius, rect.size.y - radius),
		rect.position + Vector2(rect.size.x - radius, rect.size.y - radius),
	]:
		draw_circle(corner, radius, colour)

func _smile(centre: Vector2, radius: float, colour: Color) -> void:
	draw_arc(centre, radius, PI * 0.15, PI * 0.85, 16, colour, radius * 0.16)
