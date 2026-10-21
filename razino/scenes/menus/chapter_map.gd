extends RazinoScreen
## UI-10 — نقشهٔ فصل‌ها.
##
## GDD §5 screen 4 is explicit that locking must be honest: a locked chapter
## says plainly that it is locked and why, and never dangles a promise to push
## a child towards buying.

func _ready() -> void:
	configure("فصل‌ها", "UI-04")
	super._ready()

func build_content() -> void:
	var list := make_scroll()
	for entry in ContentManager.catalog:
		list.add_child(_chapter_card(entry))

func _chapter_card(entry: Dictionary) -> Control:
	var cid := String(entry.get("chapter_id", ""))
	var unlocked := GameState.is_chapter_unlocked(cid)
	var installed := ContentManager.is_installed(cid)
	var complete := GameState.is_chapter_complete(cid)
	var free := String(entry.get("access_type", "paid")) == "free"

	var card := Style.make_night_card()
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", Palette.PAD_MD)
	card.add_child(row)

	var thumb := PlaceholderArt.new()
	thumb.background_id = cid
	thumb.show_badge = false
	thumb.custom_minimum_size = Vector2(150, 100)
	row.add_child(thumb)

	var col := VBoxContainer.new()
	col.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	row.add_child(col)
	col.add_child(Style.make_title(String(entry.get("title", cid)), Palette.FS_HEADING))
	col.add_child(Style.make_body(String(entry.get("synopsis", "")).left(130) + "…",
		Palette.FS_CAPTION, Palette.SNOW_SOFT))

	var facts := HBoxContainer.new()
	facts.add_theme_constant_override("separation", Palette.PAD_MD)
	col.add_child(facts)
	facts.add_child(_chip("%s صحنه" % Fa.num(int(entry.get("scene_count", 0))), Palette.NIGHT_LIFT))
	facts.add_child(_chip("%s قسمت" % Fa.num(int(entry.get("episode_count", 0))), Palette.NIGHT_LIFT))
	facts.add_child(_chip("رایگان" if free else "خریدنی", Palette.TEAL if free else Palette.BRASS))
	if complete:
		facts.add_child(_chip("تمام شد", Palette.OK))

	var action := VBoxContainer.new()
	action.custom_minimum_size.x = 220
	row.add_child(action)

	if not unlocked:
		action.add_child(Style.make_body("هنوز باز نشده", Palette.FS_CAPTION, Palette.SNOW_SOFT))
		var why := Style.make_body(
			"با تمام‌کردن فصل قبلی باز می‌شود." if free else "این فصل جداگانه عرضه می‌شود.",
			Palette.FS_CAPTION, Palette.LOCKED)
		action.add_child(why)
		if not free:
			var store := Style.make_button("دیدن در فروشگاه", "quiet")
			store.pressed.connect(func(): Router.go("UI-11"))
			action.add_child(store)
	elif not installed:
		action.add_child(Style.make_body("روی دستگاه نیست", Palette.FS_CAPTION, Palette.SNOW_SOFT))
		var get_btn := Style.make_button("دریافت", "secondary")
		get_btn.pressed.connect(func(): Router.go("UI-15", {"chapter_id": cid}))
		action.add_child(get_btn)
	else:
		var play := Style.make_button("ادامه" if GameState.current_chapter == cid else "بازی")
		play.pressed.connect(func(): Router.go("UI-06", {"chapter_id": cid, "resume": GameState.current_chapter == cid}))
		action.add_child(play)

	return card

func _chip(text: String, colour: Color) -> Control:
	var p := PanelContainer.new()
	var sb := StyleBoxFlat.new()
	sb.bg_color = colour
	sb.set_corner_radius_all(int(Palette.R_SM))
	sb.content_margin_left = Palette.PAD_SM
	sb.content_margin_right = Palette.PAD_SM
	sb.content_margin_top = 4
	sb.content_margin_bottom = 4
	p.add_theme_stylebox_override("panel", sb)
	p.add_child(Style.make_body(text, Palette.FS_CAPTION, Palette.SNOW, false))
	return p
