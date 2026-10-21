extends RazinoScreen
## UI-02 — معرفی اولیه. A short, skippable explanation of the controls, then
## on to making a character. Nothing here is a quiz; it is one screen a child
## reads once.

const STEPS := [
	{"title": "به رازینو خوش آمدی", "body": "شهر رازینو ده راز دارد. تو عضو تازهٔ گروهی هستی که دنبالشان می‌گردد: آراد، رها، مانی و فندق."},
	{"title": "بگرد و بررسی کن", "body": "هر چیزی که عجیب به نظر می‌رسد را لمس کن. چیزهایی که پیدا می‌کنی در دفترچهٔ سرنخ‌ها می‌نشینند."},
	{"title": "انتخاب کن", "body": "گاهی باید تصمیم بگیری. هیچ تصمیمی تو را به بن‌بست نمی‌برد، ولی داستان را عوض می‌کند."},
	{"title": "سرنخ‌ها را کنار هم بگذار", "body": "دو سرنخ که کنار هم بنشینند، گاهی جواب سومی می‌سازند. اشتباه‌زدن هیچ هزینه‌ای ندارد."},
]

var _step: int = 0
var _title: Label
var _body: Label
var _dots: HBoxContainer
var _next: Button

func _ready() -> void:
	configure("")
	super._ready()

func build_content() -> void:
	var card := Style.make_card()
	card.size_flags_vertical = Control.SIZE_EXPAND_FILL
	content.add_child(card)

	var col := VBoxContainer.new()
	col.add_theme_constant_override("separation", Palette.PAD_MD)
	card.add_child(col)

	var art := PlaceholderArt.new()
	art.background_id = "BG_INTRO_RAZINO"
	art.custom_minimum_size = Vector2(0, 220)
	art.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	col.add_child(art)

	_title = Style.make_title("", Palette.FS_HEADING, Palette.INK)
	col.add_child(_title)
	_body = Style.make_body("", Palette.FS_BODY, Palette.INK_SOFT)
	_body.size_flags_vertical = Control.SIZE_EXPAND_FILL
	col.add_child(_body)

	_dots = HBoxContainer.new()
	_dots.alignment = BoxContainer.ALIGNMENT_CENTER
	_dots.add_theme_constant_override("separation", Palette.PAD_SM)
	col.add_child(_dots)

	var buttons := HBoxContainer.new()
	buttons.add_theme_constant_override("separation", Palette.PAD_MD)
	content.add_child(buttons)

	_next = Style.make_button("بعدی")
	_next.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	_next.pressed.connect(_advance)
	buttons.add_child(_next)

	var skip := Style.make_button("رد کردن", "quiet")
	skip.custom_minimum_size.x = 180
	skip.pressed.connect(_finish)
	buttons.add_child(skip)

	_render()

func _render() -> void:
	var s: Dictionary = STEPS[_step]
	_title.text = String(s["title"])
	_body.text = String(s["body"])
	_next.text = "بعدی" if _step < STEPS.size() - 1 else "بساز و شروع کن"
	for c in _dots.get_children():
		c.queue_free()
	for i in STEPS.size():
		var dot := ColorRect.new()
		dot.custom_minimum_size = Vector2(12, 12)
		dot.color = Palette.BRASS if i == _step else Palette.PARCHMENT_DIM
		_dots.add_child(dot)

func _advance() -> void:
	AudioManager.play_sfx("SFX_UI_TAP")
	if _step < STEPS.size() - 1:
		_step += 1
		_render()
	else:
		_finish()

func _finish() -> void:
	GameState.set_flag("intro_seen", true)
	SaveManager.mark_dirty()
	Router.go("UI-03")
