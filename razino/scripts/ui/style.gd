extends Node
## Builds Razino's theme once at startup and applies it to the whole tree.
##
## Doing this in code rather than a .tres keeps every colour, radius and font
## size traceable to Palette, so the sixteen screens cannot drift apart. It
## also lets the theme react to the player's text-size setting without
## shipping several theme files.

signal theme_rebuilt()

const FONT_REGULAR := "res://assets/fonts/Vazirmatn-Regular.ttf"
const FONT_MEDIUM := "res://assets/fonts/Vazirmatn-Medium.ttf"
const FONT_BOLD := "res://assets/fonts/Vazirmatn-Bold.ttf"

var theme: Theme
var font_regular: Font
var font_medium: Font
var font_bold: Font

func _ready() -> void:
	_load_fonts()
	rebuild()
	Settings.changed.connect(func(key: String):
		if key == "text_scale":
			rebuild()
	)

func _load_fonts() -> void:
	font_regular = _load_font(FONT_REGULAR)
	font_medium = _load_font(FONT_MEDIUM)
	font_bold = _load_font(FONT_BOLD)
	if font_regular == null:
		Log.error("Style", "Persian font missing — text will fall back to the engine default and will not shape correctly")

func _load_font(path: String) -> Font:
	if not ResourceLoader.exists(path):
		return null
	var res := ResourceLoader.load(path)
	return res as Font

func scaled(size: int) -> int:
	return int(round(size * Settings.text_scale))

func rebuild() -> void:
	theme = Theme.new()
	if font_regular != null:
		theme.default_font = font_regular
	theme.default_font_size = scaled(Palette.FS_BODY)

	_style_buttons()
	_style_panels()
	_style_labels()
	_style_inputs()
	_style_misc()

	var root := get_tree().root
	if root != null:
		root.theme = theme
	theme_rebuilt.emit()
	Log.info("Style", "theme built (text scale %.2f)" % Settings.text_scale)

func _flat(bg: Color, radius: float, border: Color = Color.TRANSPARENT, border_w: int = 0) -> StyleBoxFlat:
	var sb := StyleBoxFlat.new()
	sb.bg_color = bg
	sb.set_corner_radius_all(int(radius))
	sb.content_margin_left = Palette.PAD_MD
	sb.content_margin_right = Palette.PAD_MD
	sb.content_margin_top = Palette.PAD_SM
	sb.content_margin_bottom = Palette.PAD_SM
	if border_w > 0:
		sb.set_border_width_all(border_w)
		sb.border_color = border
	return sb

func _style_buttons() -> void:
	# Primary action — brass, the colour of the clock the whole story turns on.
	theme.set_stylebox("normal", "Button", _flat(Palette.BRASS, Palette.R_MD))
	theme.set_stylebox("hover", "Button", _flat(Palette.BRASS.lightened(0.08), Palette.R_MD))
	theme.set_stylebox("pressed", "Button", _flat(Palette.BRASS_DEEP, Palette.R_MD))
	theme.set_stylebox("disabled", "Button", _flat(Palette.LOCKED, Palette.R_MD))
	theme.set_stylebox("focus", "Button", _flat(Color.TRANSPARENT, Palette.R_MD, Palette.SNOW, 3))
	theme.set_color("font_color", "Button", Palette.INK)
	theme.set_color("font_hover_color", "Button", Palette.INK)
	theme.set_color("font_pressed_color", "Button", Palette.INK)
	theme.set_color("font_disabled_color", "Button", Palette.SNOW_SOFT)
	theme.set_font_size("font_size", "Button", scaled(Palette.FS_LABEL))
	if font_medium != null:
		theme.set_font("font", "Button", font_medium)

	theme.set_stylebox("normal", "CheckButton", _flat(Palette.NIGHT_SOFT, Palette.R_MD))
	theme.set_color("font_color", "CheckButton", Palette.SNOW)
	theme.set_font_size("font_size", "CheckButton", scaled(Palette.FS_LABEL))

func _style_panels() -> void:
	theme.set_stylebox("panel", "PanelContainer", _flat(Palette.NIGHT_SOFT, Palette.R_LG))
	theme.set_stylebox("panel", "Panel", _flat(Palette.NIGHT_SOFT, Palette.R_LG))
	theme.set_stylebox("panel", "PopupPanel", _flat(Palette.NIGHT_SOFT, Palette.R_LG, Palette.BRASS, 2))

func _style_labels() -> void:
	theme.set_color("font_color", "Label", Palette.SNOW)
	theme.set_font_size("font_size", "Label", scaled(Palette.FS_BODY))
	theme.set_color("default_color", "RichTextLabel", Palette.SNOW)
	theme.set_font_size("normal_font_size", "RichTextLabel", scaled(Palette.FS_BODY))
	if font_regular != null:
		theme.set_font("normal_font", "RichTextLabel", font_regular)
	if font_bold != null:
		theme.set_font("bold_font", "RichTextLabel", font_bold)

func _style_inputs() -> void:
	# A field has to read as a field on parchment as well as on night blue, so
	# it is near-white with a standing border rather than the card colour.
	theme.set_stylebox("normal", "LineEdit", _flat(Color("FDFBF7"), Palette.R_SM, Palette.INK_SOFT, 2))
	theme.set_stylebox("focus", "LineEdit", _flat(Color("FDFBF7"), Palette.R_SM, Palette.BRASS_DEEP, 3))
	theme.set_color("font_color", "LineEdit", Palette.INK)
	theme.set_color("font_placeholder_color", "LineEdit", Palette.INK_SOFT)
	theme.set_color("caret_color", "LineEdit", Palette.INK)
	theme.set_font_size("font_size", "LineEdit", scaled(Palette.FS_BODY))

	var grabber := _flat(Palette.BRASS, 12.0)
	theme.set_stylebox("grabber_area", "HSlider", grabber)
	theme.set_stylebox("grabber_area_highlight", "HSlider", _flat(Palette.BRASS.lightened(0.1), 12.0))
	theme.set_stylebox("slider", "HSlider", _flat(Palette.NIGHT_LIFT, 12.0))

func _style_misc() -> void:
	theme.set_stylebox("background", "ProgressBar", _flat(Palette.NIGHT_LIFT, Palette.R_SM))
	theme.set_stylebox("fill", "ProgressBar", _flat(Palette.TEAL, Palette.R_SM))
	theme.set_color("font_color", "ProgressBar", Palette.SNOW)
	theme.set_constant("separation", "VBoxContainer", Palette.PAD_SM)
	theme.set_constant("separation", "HBoxContainer", Palette.PAD_SM)
	theme.set_stylebox("panel", "ScrollContainer", _flat(Color.TRANSPARENT, 0.0))

## Assigns the theme directly to a control.
##
## Godot propagates a theme down through Control and Window nodes, but a plain
## Node in the chain breaks that walk — so a screen parented under one would
## silently fall back to the engine's default grey. Every root UI node calls
## this instead of trusting its ancestry.
func attach(control: Control) -> void:
	if control != null and theme != null:
		control.theme = theme


# — small helpers shared by the screens ———————————————————

## A heading that reads as a heading on every screen.
func make_title(text: String, size: int = Palette.FS_TITLE, color: Color = Palette.SNOW) -> Label:
	var l := Label.new()
	l.text = text
	l.add_theme_font_size_override("font_size", scaled(size))
	l.add_theme_color_override("font_color", color)
	if font_bold != null:
		l.add_theme_font_override("font", font_bold)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	return l

func make_body(text: String, size: int = Palette.FS_BODY, color: Color = Palette.SNOW_SOFT,
		wrap: bool = true) -> Label:
	var l := Label.new()
	l.text = text
	l.add_theme_font_size_override("font_size", scaled(size))
	l.add_theme_color_override("font_color", color)
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	# A wrapping label inside a row can shrink to a single glyph per line, so
	# anything laid out horizontally asks for wrap = false.
	l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART if wrap else TextServer.AUTOWRAP_OFF
	return l

## Buttons are at least Palette.TOUCH_MIN tall, per GDD §7.2.
func make_button(text: String, variant: String = "primary") -> Button:
	var b := Button.new()
	b.text = text
	# Wide enough to read and to hit; height is the GDD's touch minimum.
	b.custom_minimum_size = Vector2(120, Palette.TOUCH_MIN)
	b.focus_mode = Control.FOCUS_ALL
	match variant:
		"secondary":
			b.add_theme_stylebox_override("normal", _flat(Palette.TEAL, Palette.R_MD))
			b.add_theme_stylebox_override("hover", _flat(Palette.TEAL.lightened(0.08), Palette.R_MD))
			b.add_theme_stylebox_override("pressed", _flat(Palette.TEAL_DEEP, Palette.R_MD))
			b.add_theme_color_override("font_color", Palette.SNOW)
			b.add_theme_color_override("font_hover_color", Palette.SNOW)
			b.add_theme_color_override("font_pressed_color", Palette.SNOW)
		"quiet":
			b.add_theme_stylebox_override("normal", _flat(Palette.NIGHT_LIFT, Palette.R_MD))
			b.add_theme_stylebox_override("hover", _flat(Palette.NIGHT_LIFT.lightened(0.08), Palette.R_MD))
			b.add_theme_stylebox_override("pressed", _flat(Palette.NIGHT, Palette.R_MD))
			b.add_theme_color_override("font_color", Palette.SNOW)
			b.add_theme_color_override("font_hover_color", Palette.SNOW)
			b.add_theme_color_override("font_pressed_color", Palette.SNOW)
		"danger":
			b.add_theme_stylebox_override("normal", _flat(Palette.ROSE, Palette.R_MD))
			b.add_theme_stylebox_override("hover", _flat(Palette.ROSE.lightened(0.08), Palette.R_MD))
			b.add_theme_color_override("font_color", Palette.SNOW)
			b.add_theme_color_override("font_hover_color", Palette.SNOW)
	return b

## A parchment card — used for anything the player reads closely.
func make_card(radius: float = Palette.R_LG) -> PanelContainer:
	var p := PanelContainer.new()
	var sb := _flat(Palette.PARCHMENT, radius)
	sb.content_margin_left = Palette.PAD_MD
	sb.content_margin_right = Palette.PAD_MD
	sb.content_margin_top = Palette.PAD_MD
	sb.content_margin_bottom = Palette.PAD_MD
	sb.shadow_color = Color(0, 0, 0, 0.35)
	sb.shadow_size = 10
	sb.shadow_offset = Vector2(0, 4)
	p.add_theme_stylebox_override("panel", sb)
	return p

func make_night_card(radius: float = Palette.R_LG) -> PanelContainer:
	var p := PanelContainer.new()
	var sb := _flat(Palette.NIGHT_SOFT, radius)
	sb.content_margin_left = Palette.PAD_MD
	sb.content_margin_right = Palette.PAD_MD
	sb.content_margin_top = Palette.PAD_MD
	sb.content_margin_bottom = Palette.PAD_MD
	sb.set_border_width_all(1)
	sb.border_color = Palette.NIGHT_LIFT
	p.add_theme_stylebox_override("panel", sb)
	return p
