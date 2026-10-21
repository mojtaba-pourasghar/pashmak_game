class_name DialoguePanel
extends PanelContainer
## UI-07 — کادر دیالوگ و انتخاب.
##
## GDD §5 screen 2 asks for: a readable Persian dialogue box that does not sit
## over the important part of the picture, a way to replay the line, and two
## or three clearly tappable choices at decision points.
##
## Subtitles appear in step with the recorded line rather than all at once, so
## a child still learning to read can follow the voice. When no recording
## exists yet, the pacing falls back to a reading estimate — and the panel
## says so, rather than pretending a line was voiced.

signal continue_pressed()
signal choice_selected(choice_id: String)

const REVEAL_CHARS_PER_SECOND := 34.0

var _speaker: Label
var _text: RichTextLabel
var _choices: VBoxContainer
var _continue: Button
var _replay: Button
var _missing_vo: Label

var _full_text: String = ""
var _revealed: float = 0.0
var _revealing: bool = false
var _current_audio: String = ""

func _ready() -> void:
	var sb := StyleBoxFlat.new()
	sb.bg_color = Palette.PARCHMENT
	sb.set_corner_radius_all(int(Palette.R_LG))
	sb.content_margin_left = Palette.PAD_LG
	sb.content_margin_right = Palette.PAD_LG
	sb.content_margin_top = Palette.PAD_MD
	sb.content_margin_bottom = Palette.PAD_MD
	sb.shadow_color = Color(0, 0, 0, 0.45)
	sb.shadow_size = 14
	sb.shadow_offset = Vector2(0, 6)
	add_theme_stylebox_override("panel", sb)

	var col := VBoxContainer.new()
	col.add_theme_constant_override("separation", Palette.PAD_SM)
	add_child(col)

	var top := HBoxContainer.new()
	col.add_child(top)
	_speaker = Style.make_title("", Palette.FS_LABEL, Palette.BRASS_DEEP)
	_speaker.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	top.add_child(_speaker)

	_replay = Style.make_button("پخش دوباره", "quiet")
	_replay.custom_minimum_size = Vector2(170, Palette.TOUCH_MIN)
	_replay.pressed.connect(_replay_line)
	top.add_child(_replay)

	_text = RichTextLabel.new()
	_text.bbcode_enabled = false
	_text.fit_content = true
	_text.custom_minimum_size.y = 108
	_text.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_text.add_theme_color_override("default_color", Palette.INK)
	_text.add_theme_font_size_override("normal_font_size", Style.scaled(Palette.FS_BODY))
	if Style.font_regular != null:
		_text.add_theme_font_override("normal_font", Style.font_regular)
	# RichTextLabel has no horizontal_alignment; it follows the layout and
	# text direction instead, which is what makes Persian read correctly here.
	_text.layout_direction = Control.LAYOUT_DIRECTION_RTL
	_text.text_direction = Control.TEXT_DIRECTION_RTL
	col.add_child(_text)

	_missing_vo = Style.make_body("", Palette.FS_CAPTION, Palette.ROSE)
	col.add_child(_missing_vo)

	_choices = VBoxContainer.new()
	_choices.add_theme_constant_override("separation", Palette.PAD_SM)
	col.add_child(_choices)

	_continue = Style.make_button("ادامه")
	_continue.pressed.connect(_on_continue)
	col.add_child(_continue)

	AudioManager.voice_finished.connect(_on_voice_finished)
	set_process(false)

func _process(delta: float) -> void:
	_revealed += delta * REVEAL_CHARS_PER_SECOND * (1.6 if Settings.reduce_motion else 1.0)
	var shown := int(_revealed)
	if shown >= _full_text.length():
		_finish_reveal()
		return
	_text.text = _full_text.substr(0, shown)

func show_line(line: Dictionary, speaker_name: String) -> void:
	_choices.visible = false
	for c in _choices.get_children():
		c.queue_free()
	_continue.visible = true
	_continue.text = "ادامه"
	_speaker.text = speaker_name
	_full_text = String(line.get("text", ""))
	_current_audio = String(line.get("audio_id", ""))

	var has_clip := AudioManager.missing_audio.has(_current_audio) == false and not _current_audio.is_empty()
	_replay.visible = not _current_audio.is_empty()
	_missing_vo.text = "" if has_clip else "صدای این خط هنوز ضبط نشده است."

	if Settings.subtitles_enabled:
		_revealed = 0.0
		_revealing = true
		_text.text = ""
		set_process(true)
	else:
		_text.text = _full_text
		_finish_reveal()

func show_choices(choices: Array, texts_by_id: Dictionary = {}) -> void:
	_finish_reveal()
	_continue.visible = false
	_choices.visible = true
	for c in _choices.get_children():
		c.queue_free()
	for entry in choices:
		var cd: Dictionary = entry
		var cid := String(cd.get("choice_id", ""))
		var label := String(texts_by_id.get(cid, cd.get("text", "")))
		var b := Style.make_button(label, "secondary")
		b.custom_minimum_size.y = maxf(Palette.TOUCH_MIN, 64)
		b.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		var hint := String(cd.get("hint", ""))
		if not hint.is_empty():
			b.tooltip_text = hint
		b.pressed.connect(func():
			AudioManager.play_sfx("SFX_UI_CHOICE")
			choice_selected.emit(cid))
		_choices.add_child(b)

func is_revealing() -> bool:
	return _revealing

## Tapping while text is still appearing shows all of it at once, which is
## what a fast reader expects, rather than being made to wait.
func _on_continue() -> void:
	if _revealing:
		_finish_reveal()
		return
	AudioManager.play_sfx("SFX_UI_TAP")
	continue_pressed.emit()

func _finish_reveal() -> void:
	set_process(false)
	_revealing = false
	_text.text = _full_text

func _replay_line() -> void:
	if _current_audio.is_empty():
		return
	AudioManager.play_voice(_current_audio, _full_text)

func _on_voice_finished(_audio_id: String) -> void:
	_finish_reveal()
	if Settings.auto_advance and _continue.visible:
		continue_pressed.emit()
