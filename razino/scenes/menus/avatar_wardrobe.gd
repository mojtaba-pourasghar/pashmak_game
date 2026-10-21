extends RazinoScreen
## UI-12 — آواتار و لباس‌ها.
##
## GDD §5 screen 5: changing how you look is optional and never required to
## get through a story. Nothing here is behind a price, and nothing here
## changes the difficulty of anything.

var _portrait: AvatarPortrait

func _ready() -> void:
	configure("لباس‌ها و ظاهر", "UI-04")
	super._ready()

func build_content() -> void:
	content.add_child(Style.make_body(
		"هیچ‌کدام از این‌ها برای پیش‌بردن داستان لازم نیست. فقط برای این است که شخصیتت شبیه خودت باشد.",
		Palette.FS_CAPTION, Palette.SNOW_SOFT))

	var columns := HBoxContainer.new()
	columns.add_theme_constant_override("separation", Palette.PAD_LG)
	columns.size_flags_vertical = Control.SIZE_EXPAND_FILL
	content.add_child(columns)

	var preview := Style.make_night_card()
	preview.custom_minimum_size.x = 300
	columns.add_child(preview)
	_portrait = AvatarPortrait.new()
	_portrait.size_flags_vertical = Control.SIZE_EXPAND_FILL
	preview.add_child(_portrait)

	var right := VBoxContainer.new()
	right.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	right.add_theme_constant_override("separation", Palette.PAD_MD)
	columns.add_child(right)

	right.add_child(_picker("لباس", "outfit", AvatarManager.OUTFITS, _outfit_names()))
	right.add_child(_picker("وسیله", "accessory", AvatarManager.ACCESSORIES, _accessory_names()))
	right.add_child(_picker("مدل مو", "hair_style", AvatarManager.HAIR_STYLES, _hair_names()))

	var save := Style.make_button("ذخیره و بازگشت")
	save.pressed.connect(func():
		AvatarManager.save_avatar()
		SaveManager.save()
		Router.go("UI-04"))
	content.add_child(save)

func _picker(label: String, key: String, values: Array, names: Array) -> Control:
	var card := Style.make_night_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title(label, Palette.FS_LABEL, Palette.BRASS))
	var flow := HFlowContainer.new()
	flow.add_theme_constant_override("h_separation", Palette.PAD_SM)
	flow.add_theme_constant_override("v_separation", Palette.PAD_SM)
	col.add_child(flow)
	for i in values.size():
		var b := Style.make_button(String(names[i]) if i < names.size() else String(values[i]),
			"secondary" if int(AvatarManager.config.get(key, 0)) == i else "quiet")
		b.pressed.connect(func():
			AvatarManager.set_part(key, i)
			AudioManager.play_sfx("SFX_UI_TAP")
			_refresh_picker(flow, key, names, values))
		flow.add_child(b)
	return card

func _refresh_picker(flow: HFlowContainer, key: String, names: Array, values: Array) -> void:
	var i := 0
	for child in flow.get_children():
		if child is Button:
			var variant := "secondary" if int(AvatarManager.config.get(key, 0)) == i else "quiet"
			var fresh := Style.make_button(String(names[i]) if i < names.size() else String(values[i]), variant)
			(child as Button).add_theme_stylebox_override("normal",
				fresh.get_theme_stylebox("normal"))
			i += 1

func _outfit_names() -> Array:
	return ["کاوشگر", "کتاب‌خوان", "مخترع", "جشن", "شب"]

func _accessory_names() -> Array:
	return ["بدون وسیله", "عینک", "شال‌گردن", "کیف دوشی", "ساعت جیبی"]

func _hair_names() -> Array:
	return ["کوتاه", "موج‌دار", "فرفری", "بافته", "دم‌اسبی", "کلاه"]
