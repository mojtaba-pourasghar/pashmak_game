class_name SceneStage
extends Control
## The picture behind a scene: background, parallax layers and the characters
## standing in it (GDD §4, "حرکت دوربین و عمق محیط").
##
## Backgrounds are placeholders until the paintings exist; the character
## figures are drawn from the same vector routine as the player's avatar, so
## the cast looks like one family rather than a set of boxes.

const SLOTS := {
	"far_left": 0.16, "left": 0.32, "center": 0.5, "right": 0.68, "far_right": 0.84,
}

var _bg: PlaceholderArt
var _figures: Control
var _drift: float = 0.0

func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_PASS
	_bg = PlaceholderArt.new()
	add_child(_bg)
	_figures = Control.new()
	_figures.set_anchors_preset(Control.PRESET_FULL_RECT)
	_figures.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_figures)
	set_process(not Settings.reduce_motion)

func _process(delta: float) -> void:
	# A very slow horizontal drift reads as depth without ever moving enough
	# to distract from the text.
	_drift += delta * 0.12
	_bg.position.x = sin(_drift) * 8.0

func show_scene(scene: Dictionary) -> void:
	_bg.background_id = String(scene.get("background_id", ""))
	for child in _figures.get_children():
		child.queue_free()
	for c in scene.get("characters", []):
		_add_figure(c as Dictionary)

func _add_figure(entry: Dictionary) -> void:
	var fig := CharacterFigure.new()
	fig.character_id = String(entry.get("character_id", ""))
	fig.pose = String(entry.get("animation", "idle"))
	var slot := String(entry.get("position", "center"))
	var x := float(SLOTS.get(slot, 0.5))
	fig.set_anchors_preset(Control.PRESET_BOTTOM_LEFT)
	fig.anchor_left = x
	fig.anchor_right = x
	fig.anchor_top = 1.0
	fig.anchor_bottom = 1.0
	fig.offset_left = -90
	fig.offset_right = 90
	fig.offset_top = -340
	fig.offset_bottom = -20
	_figures.add_child(fig)

func highlight_speaker(speaker_id: String) -> void:
	for child in _figures.get_children():
		if child is CharacterFigure:
			(child as CharacterFigure).set_speaking((child as CharacterFigure).character_id == speaker_id)
