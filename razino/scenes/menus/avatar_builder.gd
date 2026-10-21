extends RazinoScreen
## UI-03 — ساخت شخصیت.
##
## Everything here works with no camera. The photo button exists only when an
## adult has turned it on in the parent section, and even then it says plainly
## what it does and does not do with the picture.

const PARTS := [
	{"key": "skin", "label": "رنگ پوست"},
	{"key": "hair_style", "label": "مدل مو"},
	{"key": "hair_color", "label": "رنگ مو"},
	{"key": "eyes", "label": "چشم‌ها"},
	{"key": "outfit", "label": "لباس"},
	{"key": "accessory", "label": "وسیله"},
]

var _name_field: LineEdit
var _portrait: AvatarPortrait

func _ready() -> void:
	configure("شخصیتت را بساز")
	super._ready()

func build_content() -> void:
	var columns := HBoxContainer.new()
	columns.add_theme_constant_override("separation", Palette.PAD_LG)
	columns.size_flags_vertical = Control.SIZE_EXPAND_FILL
	content.add_child(columns)

	# — preview —
	var preview := Style.make_night_card()
	preview.custom_minimum_size.x = 300
	columns.add_child(preview)
	var pcol := VBoxContainer.new()
	pcol.alignment = BoxContainer.ALIGNMENT_CENTER
	preview.add_child(pcol)
	_portrait = AvatarPortrait.new()
	_portrait.size_flags_vertical = Control.SIZE_EXPAND_FILL
	pcol.add_child(_portrait)

	# — controls —
	var right := VBoxContainer.new()
	right.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	right.add_theme_constant_override("separation", Palette.PAD_SM)
	columns.add_child(right)

	_name_field = LineEdit.new()
	_name_field.placeholder_text = "اسمت را بنویس (اختیاری)"
	_name_field.text = GameState.player_name
	_name_field.max_length = 20
	_name_field.custom_minimum_size.y = Palette.TOUCH_MIN
	_name_field.text_direction = Control.TEXT_DIRECTION_RTL
	right.add_child(make_row("نام", _name_field))

	for part in PARTS:
		right.add_child(_part_row(String(part["key"]), String(part["label"])))

	var shuffle := Style.make_button("یکی تصادفی بساز", "secondary")
	shuffle.pressed.connect(func():
		AvatarManager.randomize_avatar()
		AudioManager.play_sfx("SFX_UI_TAP"))
	right.add_child(shuffle)

	if ParentalSettings.allow_camera_avatar:
		right.add_child(_photo_section())
	else:
		var note := Style.make_body(
			"اگر دوست داری شخصیت را از روی عکس خودت بسازی، یکی از بزرگ‌ترها باید اول در «بخش والدین» اجازه بدهد. بازی بدون عکس هم کامل است.",
			Palette.FS_CAPTION, Palette.SNOW_SOFT)
		right.add_child(note)

	var start := Style.make_button("شروع ماجرا")
	start.pressed.connect(_start)
	content.add_child(start)

func _part_row(key: String, label: String) -> Control:
	var box := HBoxContainer.new()
	box.add_theme_constant_override("separation", Palette.PAD_SM)

	var prev := Style.make_button("‹", "quiet")
	prev.custom_minimum_size = Vector2(Palette.TOUCH_MIN, Palette.TOUCH_MIN)
	prev.pressed.connect(func(): AvatarManager.cycle_part(key, -1))
	box.add_child(prev)

	var value := Style.make_body("", Palette.FS_LABEL, Palette.SNOW)
	value.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	value.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	box.add_child(value)

	var next := Style.make_button("›", "quiet")
	next.custom_minimum_size = Vector2(Palette.TOUCH_MIN, Palette.TOUCH_MIN)
	next.pressed.connect(func(): AvatarManager.cycle_part(key, 1))
	box.add_child(next)

	var refresh := func(_id = ""):
		var index := int(AvatarManager.config.get(key, 0)) + 1
		value.text = "%s از %s" % [Fa.num(index), Fa.num(AvatarManager.part_count(key))]
	refresh.call()
	AvatarManager.avatar_changed.connect(refresh)

	return make_row(label, box)

func _photo_section() -> Control:
	var card := Style.make_night_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title("ساخت از روی عکس", Palette.FS_LABEL, Palette.BRASS))
	col.add_child(Style.make_body(
		"عکس فقط روی همین دستگاه بررسی می‌شود تا رنگ پوست و مو حدس زده شود، و بلافاصله پاک می‌شود. هیچ عکسی ذخیره یا ارسال نمی‌شود و بازی چهرهٔ کسی را نمی‌شناسد.",
		Palette.FS_CAPTION, Palette.SNOW_SOFT))
	var note := Style.make_body("", Palette.FS_CAPTION, Palette.ROSE)
	col.add_child(note)
	AvatarManager.photo_processing_failed.connect(func(reason): note.text = reason)

	# The camera itself is a platform integration; see docs/ASSETS.md.
	var btn := Style.make_button("دوربین هنوز وصل نشده است", "quiet")
	btn.disabled = true
	btn.tooltip_text = "اتصال دوربین در نسخهٔ اندروید انجام می‌شود."
	col.add_child(btn)
	return card

func _start() -> void:
	GameState.player_name = _name_field.text.strip_edges()
	GameState.avatar_id = AvatarManager.avatar_id()
	if GameState.current_chapter.is_empty():
		GameState.mark_position("season_01", "S01-E01", "")
	SaveManager.save()
	Router.go("UI-04")
