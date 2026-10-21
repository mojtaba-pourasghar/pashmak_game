class_name PuzzlePanel
extends PanelContainer
## UI-09 — معما.
##
## One panel that can render all six puzzle kinds the content schema allows,
## driven entirely by the puzzle block in the chapter data. A new puzzle needs
## new data, not new code.
##
## Two rules from the GDD shape this file:
##   * no child is ever locked out — after `allow_skip_after_failures` tries a
##     guided route opens, and the story continues either way (§4.3);
##   * a wrong answer costs nothing. There is no score, no timer, no penalty;
##     a wrong try simply offers the next hint.

signal solved()
signal skipped()

var _puzzle: Dictionary = {}
var _attempts: int = 0
var _hint_index: int = 0

var _prompt: Label
var _work_area: VBoxContainer
var _feedback: Label
var _hint_label: Label
var _check: Button
var _hint_button: Button
var _skip: Button

## Selection state, interpreted per kind.
var _ordered: Array = []
var _dials: Dictionary = {}
var _pair_first: String = ""
var _pairs_done: Array = []
var _picked: Dictionary = {}

func _ready() -> void:
	var sb := StyleBoxFlat.new()
	sb.bg_color = Palette.NIGHT_SOFT
	sb.set_corner_radius_all(int(Palette.R_LG))
	sb.set_border_width_all(2)
	sb.border_color = Palette.BRASS
	for side in ["left", "right", "top", "bottom"]:
		sb.set("content_margin_" + side, Palette.PAD_LG)
	add_theme_stylebox_override("panel", sb)

	var col := VBoxContainer.new()
	col.add_theme_constant_override("separation", Palette.PAD_MD)
	add_child(col)

	_prompt = Style.make_title("", Palette.FS_HEADING)
	col.add_child(_prompt)

	_work_area = VBoxContainer.new()
	_work_area.add_theme_constant_override("separation", Palette.PAD_SM)
	_work_area.size_flags_vertical = Control.SIZE_EXPAND_FILL
	col.add_child(_work_area)

	_feedback = Style.make_body("", Palette.FS_LABEL, Palette.BRASS)
	col.add_child(_feedback)
	_hint_label = Style.make_body("", Palette.FS_CAPTION, Palette.TEAL)
	col.add_child(_hint_label)

	var buttons := HBoxContainer.new()
	buttons.add_theme_constant_override("separation", Palette.PAD_MD)
	col.add_child(buttons)

	_check = Style.make_button("بررسی")
	_check.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	_check.pressed.connect(_on_check)
	buttons.add_child(_check)

	_hint_button = Style.make_button("راهنمایی", "secondary")
	_hint_button.custom_minimum_size.x = 190
	_hint_button.pressed.connect(_next_hint)
	buttons.add_child(_hint_button)

	_skip = Style.make_button("بگذار کمکت کنم", "quiet")
	_skip.custom_minimum_size.x = 240
	_skip.visible = false
	_skip.pressed.connect(func():
		AudioManager.play_sfx("SFX_UI_TAP")
		skipped.emit())
	buttons.add_child(_skip)

func load_puzzle(puzzle: Dictionary) -> void:
	_puzzle = puzzle
	_attempts = 0
	_hint_index = 0
	_ordered.clear()
	_dials.clear()
	_pairs_done.clear()
	_picked.clear()
	_pair_first = ""
	_feedback.text = ""
	_hint_label.text = ""
	_skip.visible = false
	_prompt.text = String(puzzle.get("prompt", ""))
	_rebuild()

func _rebuild() -> void:
	for c in _work_area.get_children():
		c.queue_free()
	match String(_puzzle.get("kind", "")):
		"order_symbols", "route_path", "assemble_parts":
			_build_ordering()
		"dial_code":
			_build_dials()
		"match_pairs":
			_build_pairs()
		"spot_contradiction":
			_build_picking()
		_:
			_work_area.add_child(Style.make_body("این معما را نمی‌شناسم.", Palette.FS_LABEL, Palette.ROSE))

# — ordering (order_symbols / route_path / assemble_parts) ————

func _build_ordering() -> void:
	var chosen := HBoxContainer.new()
	chosen.add_theme_constant_override("separation", Palette.PAD_SM)
	_work_area.add_child(Style.make_body("ترتیب تو:", Palette.FS_CAPTION, Palette.SNOW_SOFT))
	_work_area.add_child(chosen)
	for i in _ordered.size():
		chosen.add_child(_token(String(_label_for(_ordered[i])), Palette.BRASS, Palette.INK))
	if _ordered.is_empty():
		chosen.add_child(Style.make_body("— هنوز چیزی نچیده‌ای —", Palette.FS_CAPTION, Palette.LOCKED))

	_work_area.add_child(Style.make_body("از این‌ها انتخاب کن:", Palette.FS_CAPTION, Palette.SNOW_SOFT))
	var pool := _flow()
	for item in _puzzle.get("items", []):
		var id := String((item as Dictionary).get("id", ""))
		if _ordered.has(id):
			continue
		var b := Style.make_button(String((item as Dictionary).get("label", id)), "quiet")
		b.pressed.connect(func():
			_ordered.append(id)
			AudioManager.play_sfx("SFX_UI_TAP")
			_rebuild())
		pool.add_child(b)

	if not _ordered.is_empty():
		var undo := Style.make_button("آخری را بردار", "quiet")
		undo.pressed.connect(func():
			_ordered.pop_back()
			_rebuild())
		_work_area.add_child(undo)

# — dial_code ——————————————————————————————————————————————

func _build_dials() -> void:
	for item in _puzzle.get("items", []):
		var d: Dictionary = item
		var id := String(d.get("id", ""))
		var options: Array = d.get("options", [])
		if options.is_empty():
			continue
		if not _dials.has(id):
			_dials[id] = 0
		var row := HBoxContainer.new()
		row.add_theme_constant_override("separation", Palette.PAD_SM)

		var label := Style.make_body(String(d.get("label", id)), Palette.FS_LABEL, Palette.SNOW)
		label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		row.add_child(label)

		var down := Style.make_button("‹", "quiet")
		down.custom_minimum_size = Vector2(Palette.TOUCH_MIN, Palette.TOUCH_MIN)
		down.pressed.connect(func():
			_dials[id] = posmod(int(_dials[id]) - 1, options.size())
			_rebuild())
		row.add_child(down)

		row.add_child(_token(String(options[int(_dials[id])]), Palette.PARCHMENT, Palette.INK))

		var up := Style.make_button("›", "quiet")
		up.custom_minimum_size = Vector2(Palette.TOUCH_MIN, Palette.TOUCH_MIN)
		up.pressed.connect(func():
			_dials[id] = posmod(int(_dials[id]) + 1, options.size())
			_rebuild())
		row.add_child(up)

		_work_area.add_child(row)

# — match_pairs —————————————————————————————————————————————

func _build_pairs() -> void:
	if not _pairs_done.is_empty():
		_work_area.add_child(Style.make_body("جفت‌های پیداشده:", Palette.FS_CAPTION, Palette.SNOW_SOFT))
		for pair in _pairs_done:
			_work_area.add_child(Style.make_body(
				"%s  ↔  %s" % [_label_for(pair[0]), _label_for(pair[1])],
				Palette.FS_CAPTION, Palette.OK))

	var pool := _flow()
	for item in _puzzle.get("items", []):
		var id := String((item as Dictionary).get("id", ""))
		if _in_done(id):
			continue
		var b := Style.make_button(String((item as Dictionary).get("label", id)),
			"secondary" if id == _pair_first else "quiet")
		b.pressed.connect(func(): _pick_pair(id))
		pool.add_child(b)

func _pick_pair(id: String) -> void:
	AudioManager.play_sfx("SFX_UI_TAP")
	if _pair_first.is_empty():
		_pair_first = id
		_rebuild()
		return
	if _pair_first == id:
		_pair_first = ""
		_rebuild()
		return
	var a := _pair_first
	_pair_first = ""
	if _is_valid_pair(a, id):
		_pairs_done.append([a, id])
		_feedback.text = "درست است."
		AudioManager.play_sfx("SFX_PUZZLE_OK")
	else:
		_attempts += 1
		_feedback.text = "این دو با هم جور نیستند. دوباره نگاه کن."
		_offer_help()
	_rebuild()
	if _pairs_done.size() == (_puzzle.get("solution", []) as Array).size():
		_succeed()

func _is_valid_pair(a: String, b: String) -> bool:
	for pair in _puzzle.get("solution", []):
		var p: Array = pair
		if (p[0] == a and p[1] == b) or (p[0] == b and p[1] == a):
			return true
	return false

func _in_done(id: String) -> bool:
	for pair in _pairs_done:
		if (pair as Array).has(id):
			return true
	return false

# — spot_contradiction ————————————————————————————————————

func _build_picking() -> void:
	var needed := (_puzzle.get("solution", []) as Array).size()
	_work_area.add_child(Style.make_body(
		"یکی را انتخاب کن." if needed == 1 else "%d تا را انتخاب کن." % needed,
		Palette.FS_CAPTION, Palette.SNOW_SOFT))
	for item in _puzzle.get("items", []):
		var id := String((item as Dictionary).get("id", ""))
		var b := Style.make_button(String((item as Dictionary).get("label", id)),
			"secondary" if _picked.has(id) else "quiet")
		b.custom_minimum_size.y = maxf(Palette.TOUCH_MIN, 60)
		b.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
		b.pressed.connect(func():
			if _picked.has(id):
				_picked.erase(id)
			else:
				_picked[id] = true
			AudioManager.play_sfx("SFX_UI_TAP")
			_rebuild())
		_work_area.add_child(b)

# — checking ——————————————————————————————————————————————

func _on_check() -> void:
	if _is_correct():
		_succeed()
		return
	_attempts += 1
	_feedback.text = "هنوز نه. اشکالی ندارد — دوباره امتحان کن."
	AudioManager.play_sfx("SFX_UI_SOFT_NO")
	_offer_help()

func _is_correct() -> bool:
	var solution: Array = _puzzle.get("solution", [])
	match String(_puzzle.get("kind", "")):
		"order_symbols", "route_path", "assemble_parts":
			if _ordered.size() != solution.size():
				return false
			for i in solution.size():
				if String(solution[i]) != String(_ordered[i]):
					return false
			return true
		"dial_code":
			var items: Array = _puzzle.get("items", [])
			if items.size() != solution.size():
				return false
			for i in items.size():
				var d: Dictionary = items[i]
				var options: Array = d.get("options", [])
				var chosen := String(options[int(_dials.get(String(d.get("id", "")), 0))])
				if chosen != String(solution[i]):
					return false
			return true
		"match_pairs":
			return _pairs_done.size() == solution.size()
		"spot_contradiction":
			if _picked.size() != solution.size():
				return false
			for id in solution:
				if not _picked.has(String(id)):
					return false
			return true
	return false

func _succeed() -> void:
	_feedback.text = "آفرین."
	AudioManager.play_sfx("SFX_PUZZLE_OK")
	_check.disabled = true
	solved.emit()

## Hints come one at a time, and the last one effectively gives the answer —
## because being stuck should end in understanding, not in defeat.
func _next_hint() -> void:
	var hints: Array = _puzzle.get("hints", [])
	if hints.is_empty():
		_hint_label.text = "راهنمایی‌ای برای این معما نوشته نشده است."
		return
	_hint_label.text = String(hints[mini(_hint_index, hints.size() - 1)])
	_hint_index = mini(_hint_index + 1, hints.size() - 1)
	AudioManager.play_sfx("SFX_UI_TAP")

func _offer_help() -> void:
	var limit := int(_puzzle.get("allow_skip_after_failures", 4))
	if _attempts >= 2 and _hint_index == 0:
		_next_hint()
	if _attempts >= limit:
		_skip.visible = true
		_hint_label.text = "می‌توانی با کمک ادامه بدهی — داستان همان‌طور جلو می‌رود."

# — small helpers ——————————————————————————————————————————

func _flow() -> Control:
	var box := HFlowContainer.new()
	box.add_theme_constant_override("h_separation", Palette.PAD_SM)
	box.add_theme_constant_override("v_separation", Palette.PAD_SM)
	_work_area.add_child(box)
	return box

func _label_for(id: Variant) -> String:
	for item in _puzzle.get("items", []):
		if String((item as Dictionary).get("id", "")) == String(id):
			return String((item as Dictionary).get("label", id))
	return String(id)

func _token(text: String, bg: Color, fg: Color) -> Control:
	var p := PanelContainer.new()
	var sb := StyleBoxFlat.new()
	sb.bg_color = bg
	sb.set_corner_radius_all(int(Palette.R_SM))
	sb.content_margin_left = Palette.PAD_MD
	sb.content_margin_right = Palette.PAD_MD
	sb.content_margin_top = Palette.PAD_XS
	sb.content_margin_bottom = Palette.PAD_XS
	p.add_theme_stylebox_override("panel", sb)
	p.add_child(Style.make_body(text, Palette.FS_LABEL, fg))
	return p
