extends RazinoScreen
## UI-04 — خانه.
##
## GDD §5 screen 1: within a couple of seconds a child should see where they
## are and where they can carry on. So the big button is always "continue",
## and the current chapter and its progress sit right above it.

func _ready() -> void:
	configure("")
	super._ready()

func build_content() -> void:
	var top := HBoxContainer.new()
	top.add_theme_constant_override("separation", Palette.PAD_LG)
	top.size_flags_vertical = Control.SIZE_EXPAND_FILL
	content.add_child(top)

	# — who you are —
	var who := Style.make_night_card()
	who.custom_minimum_size.x = 280
	top.add_child(who)
	var wcol := VBoxContainer.new()
	wcol.alignment = BoxContainer.ALIGNMENT_CENTER
	who.add_child(wcol)
	var portrait := AvatarPortrait.new()
	portrait.custom_minimum_size = Vector2(180, 240)
	wcol.add_child(portrait)
	var name_text := GameState.player_name if not GameState.player_name.is_empty() else "ماجراجو"
	var name_label := Style.make_title(name_text, Palette.FS_HEADING)
	name_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	wcol.add_child(name_label)

	# — where you are —
	var right := VBoxContainer.new()
	right.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	right.add_theme_constant_override("separation", Palette.PAD_MD)
	top.add_child(right)

	right.add_child(_current_chapter_card())

	var big := Style.make_button(_continue_label())
	big.custom_minimum_size.y = 84
	big.add_theme_font_size_override("font_size", Style.scaled(Palette.FS_HEADING))
	big.pressed.connect(_continue)
	right.add_child(big)

	var grid := GridContainer.new()
	grid.columns = 2
	grid.add_theme_constant_override("h_separation", Palette.PAD_MD)
	grid.add_theme_constant_override("v_separation", Palette.PAD_MD)
	right.add_child(grid)
	grid.add_child(_tile("داستان‌ها", "UI-10", "secondary"))
	grid.add_child(_tile("دفتر سرنخ‌ها", "UI-08", "secondary"))
	grid.add_child(_tile("لباس‌ها", "UI-12", "quiet"))
	grid.add_child(_tile("نقشهٔ جهان", "UI-05", "quiet"))

	# — the adult / system row, deliberately out of the child's main path —
	var bottom := HBoxContainer.new()
	bottom.add_theme_constant_override("separation", Palette.PAD_SM)
	content.add_child(bottom)
	bottom.add_child(_tile("تنظیمات", "UI-13", "quiet"))
	bottom.add_child(_tile("دانلودها", "UI-15", "quiet"))
	bottom.add_child(_tile("فروشگاه", "UI-11", "quiet"))
	bottom.add_child(_tile("بخش والدین", "UI-14", "quiet"))

func _current_chapter_card() -> Control:
	var card := Style.make_card()
	var col := VBoxContainer.new()
	card.add_child(col)

	var chapter_id := GameState.current_chapter
	if chapter_id.is_empty():
		chapter_id = "season_01"
	var entry := ContentManager.catalog_entry(chapter_id)
	var title := String(entry.get("title", "راز ساعت خاموش"))
	var scene_total := int(entry.get("scene_count", 1))

	col.add_child(Style.make_body("فصل فعلی", Palette.FS_CAPTION, Palette.INK_SOFT))
	col.add_child(Style.make_title(title, Palette.FS_HEADING, Palette.INK))

	var percent := GameState.chapter_percent(chapter_id, scene_total)
	var bar := ProgressBar.new()
	bar.min_value = 0.0
	bar.max_value = 1.0
	bar.value = percent
	bar.show_percentage = false
	bar.custom_minimum_size.y = 18
	col.add_child(bar)
	col.add_child(Style.make_body("%d٪ از این فصل را دیده‌ای" % int(round(percent * 100.0)),
		Palette.FS_CAPTION, Palette.INK_SOFT))
	return card

func _continue_label() -> String:
	return "ادامهٔ ماجرا" if not GameState.current_scene.is_empty() else "شروع ماجرا"

func _continue() -> void:
	var chapter_id := GameState.current_chapter
	if chapter_id.is_empty():
		chapter_id = "season_01"
	Router.go("UI-06", {"chapter_id": chapter_id, "resume": true})

func _tile(label: String, ui_id: String, variant: String) -> Button:
	var b := Style.make_button(label, variant)
	b.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	b.pressed.connect(func():
		AudioManager.play_sfx("SFX_UI_TAP")
		Router.go(ui_id))
	return b
