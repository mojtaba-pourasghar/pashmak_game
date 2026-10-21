class_name RazinoScreen
extends Control
## Base for every screen in GDD §7.1.
##
## Gives each screen the same bones: a full-bleed background, a safe-area
## margin that keeps content off notches and rounded corners, a header with a
## title and an optional back button, and a content column built right to left.
## Screens override `build_content()` and nothing else.

var header: HBoxContainer
var content: VBoxContainer
var background: Control

var _title_text: String = ""
var _back_target: String = ""

## Keeps the layout usable on a narrow phone without shrinking text below
## readability (GDD §7.2, last bullet).
const SAFE_MARGIN := 28
const MAX_CONTENT_WIDTH := 1100

func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	layout_direction = Control.LAYOUT_DIRECTION_RTL
	_build_shell()
	build_content()

## Overridden by each screen.
func build_content() -> void:
	pass

## Called by subclasses before build_content() runs, from configure().
func configure(title: String, back_to: String = "") -> void:
	_title_text = title
	_back_target = back_to

func _build_shell() -> void:
	background = ColorRect.new()
	(background as ColorRect).color = Palette.NIGHT
	background.set_anchors_preset(Control.PRESET_FULL_RECT)
	background.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(background)

	var margin := MarginContainer.new()
	margin.set_anchors_preset(Control.PRESET_FULL_RECT)
	for side in ["left", "right", "top", "bottom"]:
		margin.add_theme_constant_override("margin_" + side, SAFE_MARGIN)
	add_child(margin)

	var centre := CenterContainer.new()
	centre.set_anchors_preset(Control.PRESET_FULL_RECT)
	margin.add_child(centre)

	var column := VBoxContainer.new()
	column.custom_minimum_size = Vector2(0, 0)
	column.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	column.size_flags_vertical = Control.SIZE_EXPAND_FILL
	column.add_theme_constant_override("separation", Palette.PAD_MD)
	centre.add_child(column)

	header = HBoxContainer.new()
	header.add_theme_constant_override("separation", Palette.PAD_SM)
	column.add_child(header)

	var title := Style.make_title(_title_text, Palette.FS_TITLE)
	title.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	header.add_child(title)

	if not _back_target.is_empty():
		var back := Style.make_button("بازگشت", "quiet")
		back.custom_minimum_size.x = 150
		back.pressed.connect(func(): Router.go(_back_target))
		header.add_child(back)

	content = VBoxContainer.new()
	content.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	content.size_flags_vertical = Control.SIZE_EXPAND_FILL
	content.add_theme_constant_override("separation", Palette.PAD_MD)
	column.add_child(content)

## A scrollable area for screens with more content than fits.
func make_scroll() -> VBoxContainer:
	var scroll := ScrollContainer.new()
	scroll.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	content.add_child(scroll)
	var inner := VBoxContainer.new()
	inner.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	inner.add_theme_constant_override("separation", Palette.PAD_MD)
	scroll.add_child(inner)
	return inner

## One row of label + control, laid out right to left.
func make_row(label_text: String, control: Control) -> HBoxContainer:
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", Palette.PAD_MD)
	var l := Style.make_body(label_text, Palette.FS_LABEL, Palette.SNOW)
	l.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	row.add_child(l)
	control.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	row.add_child(control)
	return row

## A short message bar used for errors and confirmations.
func make_notice(text: String, tone: Color = Palette.ROSE) -> PanelContainer:
	var panel := PanelContainer.new()
	var sb := StyleBoxFlat.new()
	sb.bg_color = tone.darkened(0.55)
	sb.set_corner_radius_all(int(Palette.R_MD))
	sb.set_border_width_all(2)
	sb.border_color = tone
	sb.content_margin_left = Palette.PAD_MD
	sb.content_margin_right = Palette.PAD_MD
	sb.content_margin_top = Palette.PAD_SM
	sb.content_margin_bottom = Palette.PAD_SM
	panel.add_theme_stylebox_override("panel", sb)
	panel.add_child(Style.make_body(text, Palette.FS_LABEL, Palette.SNOW))
	return panel
