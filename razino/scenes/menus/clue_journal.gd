extends RazinoScreen
## UI-08 — دفترچهٔ سرنخ‌ها.
##
## GDD §5 calls this one of the things that separates Razino from an ordinary
## storybook, so it is not a read-only list: clues can be put side by side, and
## a correct pairing is itself a discovery. Open questions are shown so a stuck
## player can see what is still unanswered without being handed the answer.

var _return_to: String = "UI-04"
var _selected: PackedStringArray = []
var _grid: VBoxContainer
var _result: Label
var _combine_button: Button

func _ready() -> void:
	configure("دفترچهٔ سرنخ‌ها")
	super._ready()

func build_content() -> void:
	var payload := Router.take_payload()
	_return_to = String(payload.get("return_to", "UI-04"))
	var chapter_id := String(payload.get("chapter_id", GameState.current_chapter))
	if chapter_id.is_empty():
		chapter_id = "season_01"
	if ClueJournal.definitions.is_empty():
		ClueJournal.load_chapter_clues(ContentManager.load_chapter_story(chapter_id))

	var back := Style.make_button("بازگشت", "quiet")
	back.custom_minimum_size.x = 150
	back.pressed.connect(func(): Router.go(_return_to))
	header.add_child(back)

	var columns := HBoxContainer.new()
	columns.add_theme_constant_override("separation", Palette.PAD_LG)
	columns.size_flags_vertical = Control.SIZE_EXPAND_FILL
	content.add_child(columns)

	var left := VBoxContainer.new()
	left.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	left.add_theme_constant_override("separation", Palette.PAD_SM)
	columns.add_child(left)

	var scroll := ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	left.add_child(scroll)
	_grid = VBoxContainer.new()
	_grid.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	_grid.add_theme_constant_override("separation", Palette.PAD_SM)
	scroll.add_child(_grid)

	var side := VBoxContainer.new()
	side.custom_minimum_size.x = 340
	side.add_theme_constant_override("separation", Palette.PAD_MD)
	columns.add_child(side)
	side.add_child(_combine_card())
	side.add_child(_questions_card())

	_refresh()

func _combine_card() -> Control:
	var card := Style.make_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title("کنار هم بگذار", Palette.FS_HEADING, Palette.INK))
	col.add_child(Style.make_body(
		"دو سرنخ را انتخاب کن و ببین با هم چه می‌گویند. اگر جور نبودند، هیچ اتفاقی نمی‌افتد.",
		Palette.FS_CAPTION, Palette.INK_SOFT))
	_result = Style.make_body("", Palette.FS_LABEL, Palette.TEAL_DEEP)
	col.add_child(_result)
	_combine_button = Style.make_button("امتحان کن")
	_combine_button.disabled = true
	_combine_button.pressed.connect(_try_combine)
	col.add_child(_combine_button)
	return card

func _questions_card() -> Control:
	var card := Style.make_night_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title("هنوز نمی‌دانیم", Palette.FS_LABEL, Palette.BRASS))
	var open := ClueJournal.open_questions()
	if open.is_empty():
		col.add_child(Style.make_body("فعلاً سؤال بازی نداری.", Palette.FS_CAPTION, Palette.SNOW_SOFT))
	for q in open:
		var qd: Dictionary = q
		col.add_child(Style.make_body("• " + String(qd.get("title", "")), Palette.FS_CAPTION, Palette.SNOW))
	return card

func _refresh() -> void:
	for c in _grid.get_children():
		c.queue_free()
	var found := ClueJournal.discovered()
	if found.is_empty():
		_grid.add_child(Style.make_body(
			"هنوز سرنخی پیدا نکرده‌ای. در صحنه‌ها هر چیزی که عجیب است را لمس کن.",
			Palette.FS_BODY, Palette.SNOW_SOFT))
		return
	for category in ClueJournal.categories():
		_grid.add_child(Style.make_title(_category_name(category), Palette.FS_LABEL, Palette.BRASS))
		for clue_id in ClueJournal.discovered_in_category(category):
			_grid.add_child(_clue_card(clue_id))

func _clue_card(clue_id: String) -> Control:
	var chosen := _selected.has(clue_id)
	var card := PanelContainer.new()
	var sb := StyleBoxFlat.new()
	sb.bg_color = Palette.PARCHMENT if not chosen else Palette.BRASS
	sb.set_corner_radius_all(int(Palette.R_MD))
	for side in ["left", "right", "top", "bottom"]:
		sb.set("content_margin_" + side, Palette.PAD_MD)
	if chosen:
		sb.set_border_width_all(3)
		sb.border_color = Palette.TEAL
	card.add_theme_stylebox_override("panel", sb)

	# The card must grow with its text, so the content is a normal child of the
	# panel and the whole card takes the tap — anchoring the text inside a
	# fixed-height Button clipped longer clues.
	var col := VBoxContainer.new()
	col.mouse_filter = Control.MOUSE_FILTER_IGNORE
	col.custom_minimum_size.y = Palette.TOUCH_MIN
	card.add_child(col)
	col.add_child(Style.make_title(ClueJournal.title_of(clue_id), Palette.FS_LABEL, Palette.INK))
	col.add_child(Style.make_body(ClueJournal.body_of(clue_id), Palette.FS_CAPTION, Palette.INK_SOFT))

	card.mouse_filter = Control.MOUSE_FILTER_STOP
	card.gui_input.connect(func(event: InputEvent):
		if event is InputEventMouseButton and (event as InputEventMouseButton).pressed \
				and (event as InputEventMouseButton).button_index == MOUSE_BUTTON_LEFT:
			_toggle(clue_id)
		elif event is InputEventScreenTouch and (event as InputEventScreenTouch).pressed:
			_toggle(clue_id))
	card.tooltip_text = ClueJournal.body_of(clue_id)
	return card

func _toggle(clue_id: String) -> void:
	AudioManager.play_sfx("SFX_UI_TAP")
	if _selected.has(clue_id):
		var idx := _selected.find(clue_id)
		_selected.remove_at(idx)
	else:
		if _selected.size() >= 2:
			_selected.remove_at(0)
		_selected.append(clue_id)
	_combine_button.disabled = _selected.size() != 2
	_result.text = ""
	_refresh()

func _try_combine() -> void:
	if _selected.size() != 2:
		return
	var result := ClueJournal.combine(_selected[0], _selected[1])
	if result.is_empty():
		_result.text = "این دو با هم چیزی نمی‌گویند. چیزی از دست ندادی."
	else:
		_result.text = "چیز تازه‌ای فهمیدی: %s" % ClueJournal.title_of(result)
		_selected = PackedStringArray()
		_combine_button.disabled = true
	_refresh()

func _category_name(category: String) -> String:
	match category:
		"observation": return "چیزهایی که دیدی"
		"object": return "چیزهایی که برداشتی"
		"document": return "نوشته‌ها"
		"symbol": return "نشانه‌ها"
		"testimony": return "حرف‌های آدم‌ها"
		"conclusion": return "نتیجه‌ها"
		"question": return "سؤال‌ها"
	return category
