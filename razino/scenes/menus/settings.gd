extends RazinoScreen
## UI-13 — تنظیمات.
##
## Three independent volumes, as GDD §12.2 requires, plus the accessibility
## switches that make the game readable for a child who is still learning:
## subtitle size, keeping subtitles on, reduced motion and auto-advance.

func _ready() -> void:
	configure("تنظیمات", "UI-04")
	super._ready()

func build_content() -> void:
	var list := make_scroll()

	var audio := Style.make_night_card()
	var acol := VBoxContainer.new()
	audio.add_child(acol)
	acol.add_child(Style.make_title("صدا", Palette.FS_HEADING))
	acol.add_child(_slider("صدای گوینده", "voice_volume", Settings.voice_volume))
	acol.add_child(_slider("موسیقی", "music_volume", Settings.music_volume))
	acol.add_child(_slider("افکت‌ها", "sfx_volume", Settings.sfx_volume))
	list.add_child(audio)

	var reading := Style.make_night_card()
	var rcol := VBoxContainer.new()
	reading.add_child(rcol)
	rcol.add_child(Style.make_title("خواندن و دیدن", Palette.FS_HEADING))
	rcol.add_child(_toggle("نمایش زیرنویس", "subtitles_enabled", Settings.subtitles_enabled))
	rcol.add_child(_slider("اندازهٔ متن", "text_scale", Settings.text_scale, 0.85, 1.4))
	rcol.add_child(_toggle("کم‌کردن حرکت تصویر", "reduce_motion", Settings.reduce_motion))
	rcol.add_child(_toggle("ادامهٔ خودکار بعد از هر جمله", "auto_advance", Settings.auto_advance))
	var sample := Style.make_body("نمونهٔ متن: راز ساعت خاموش — ۱۲۳۴", Palette.FS_BODY, Palette.SNOW)
	rcol.add_child(sample)
	Style.theme_rebuilt.connect(func():
		sample.add_theme_font_size_override("font_size", Style.scaled(Palette.FS_BODY)))
	list.add_child(reading)

	var about := Style.make_night_card()
	var bcol := VBoxContainer.new()
	about.add_child(bcol)
	bcol.add_child(Style.make_title("دربارهٔ بازی", Palette.FS_HEADING))
	bcol.add_child(Style.make_body("رازینو — نسخهٔ %s" % Fa.digits(String(ProjectSettings.get_setting("application/config/version", "?"))),
		Palette.FS_CAPTION, Palette.SNOW_SOFT))
	bcol.add_child(Style.make_body("فونت وزیرمتن، با مجوز SIL OFL.", Palette.FS_CAPTION, Palette.SNOW_SOFT))
	var parent_btn := Style.make_button("بخش والدین", "quiet")
	parent_btn.pressed.connect(func(): Router.go("UI-14"))
	bcol.add_child(parent_btn)
	list.add_child(about)

func _slider(label: String, key: String, value: float, lo: float = 0.0, hi: float = 1.0) -> Control:
	var s := HSlider.new()
	s.min_value = lo
	s.max_value = hi
	s.step = 0.05
	s.value = value
	s.custom_minimum_size.y = Palette.TOUCH_MIN
	var readout := Style.make_body("", Palette.FS_CAPTION, Palette.SNOW_SOFT)
	var update := func(v: float):
		readout.text = Fa.percent((v - lo) / maxf(hi - lo, 0.001))
	update.call(value)
	s.value_changed.connect(func(v):
		Settings.set_value(key, v)
		update.call(v))
	var box := HBoxContainer.new()
	box.add_theme_constant_override("separation", Palette.PAD_SM)
	s.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	box.add_child(s)
	readout.custom_minimum_size.x = 70
	box.add_child(readout)
	return make_row(label, box)

func _toggle(label: String, key: String, value: bool) -> Control:
	var b := CheckButton.new()
	b.button_pressed = value
	b.custom_minimum_size.y = Palette.TOUCH_MIN
	b.toggled.connect(func(on): Settings.set_value(key, on))
	return make_row(label, b)
