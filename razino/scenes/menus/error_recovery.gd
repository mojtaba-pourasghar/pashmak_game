extends RazinoScreen
## UI-16 — خطا و بازیابی.
##
## Every failure the game can hit arrives here with a cause and something the
## player or parent can actually do. GDD §11 asks that an incomplete download
## or an incompatible version shows a clear message — not a silent stall and
## not a stack trace.

const EXPLANATIONS := {
	"catalog_missing": {
		"title": "فهرست محتوا خوانده نشد",
		"body": "فایل فهرست فصل‌ها پیدا نشد یا خوانا نبود. این یعنی نصب بازی ناقص است.",
		"fix": "بازی را ببندید و دوباره باز کنید. اگر درست نشد، برنامه را دوباره نصب کنید؛ پیشرفت شما جداگانه ذخیره شده است.",
	},
	"save_unreadable": {
		"title": "پروندهٔ ذخیره خوانده نشد",
		"body": "فایل پیشرفت آسیب دیده بود و نسخهٔ پشتیبان هم قابل خواندن نبود.",
		"fix": "می‌توانید از ابتدا شروع کنید. فصل‌های خریداری‌شده از بین نمی‌روند.",
	},
	"chapter_missing": {
		"title": "این فصل روی دستگاه نیست",
		"body": "محتوای این فصل هنوز دریافت نشده یا پاک شده است.",
		"fix": "به بخش دانلودها بروید و فصل را دریافت کنید.",
	},
	"story_unreadable": {
		"title": "محتوای فصل خوانده نشد",
		"body": "فایل‌های این فصل ناقص یا ناسازگارند. نسخهٔ سالم قبلی دست‌نخورده مانده است.",
		"fix": "فصل را از بخش دانلودها دوباره دریافت کنید.",
	},
	"story_error": {
		"title": "مشکلی در داستان پیش آمد",
		"body": "بازی نتوانست صحنهٔ بعدی را پیدا کند.",
		"fix": "به خانه برگردید و دوباره ادامه بدهید؛ پیشرفت شما ذخیره شده است.",
	},
	"network": {
		"title": "اینترنت در دسترس نیست",
		"body": "برای دریافت فصل تازه به اینترنت نیاز است.",
		"fix": "فصل‌هایی که روی دستگاه هستند بدون اینترنت کار می‌کنند؛ می‌توانید همان‌ها را ادامه بدهید.",
	},
}

func _ready() -> void:
	configure("")
	super._ready()

func build_content() -> void:
	var payload := Router.take_payload()
	var reason := String(payload.get("reason", "story_error"))
	var info: Dictionary = EXPLANATIONS.get(reason, EXPLANATIONS["story_error"])

	var card := Style.make_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	content.add_child(card)

	col.add_child(Style.make_title(String(info["title"]), Palette.FS_TITLE, Palette.INK))
	col.add_child(Style.make_body(String(info["body"]), Palette.FS_BODY, Palette.INK_SOFT))
	col.add_child(Style.make_title("چه کار کنیم؟", Palette.FS_LABEL, Palette.BRASS_DEEP))
	col.add_child(Style.make_body(String(info["fix"]), Palette.FS_BODY, Palette.INK_SOFT))

	var detail := String(payload.get("message", ""))
	if not detail.is_empty():
		col.add_child(Style.make_body("جزئیات: " + detail, Palette.FS_CAPTION, Palette.INK_SOFT))

	var buttons := HBoxContainer.new()
	buttons.add_theme_constant_override("separation", Palette.PAD_MD)
	content.add_child(buttons)

	var home := Style.make_button("بازگشت به خانه")
	home.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	home.pressed.connect(func(): Router.go("UI-04"))
	buttons.add_child(home)

	if reason in ["chapter_missing", "story_unreadable", "network"]:
		var dl := Style.make_button("دانلودها", "secondary")
		dl.pressed.connect(func(): Router.go("UI-15", payload))
		buttons.add_child(dl)

	if reason == "save_unreadable":
		var fresh := Style.make_button("شروع تازه", "danger")
		fresh.pressed.connect(func():
			SaveManager.delete_save()
			Router.go("UI-02"))
		buttons.add_child(fresh)

	content.add_child(_diagnostics())

## A collapsed technical log for a parent who is reporting a problem. It holds
## no personal data — see scripts/core/log.gd.
func _diagnostics() -> Control:
	var card := Style.make_night_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	var toggle := Style.make_button("نمایش اطلاعات فنی", "quiet")
	col.add_child(toggle)
	var box := VBoxContainer.new()
	box.visible = false
	col.add_child(box)
	toggle.pressed.connect(func():
		box.visible = not box.visible
		toggle.text = "پنهان‌کردن اطلاعات فنی" if box.visible else "نمایش اطلاعات فنی"
		if box.visible and box.get_child_count() == 0:
			for line in Log.read_tail(30):
				box.add_child(Style.make_body(line, Palette.FS_CAPTION, Palette.SNOW_SOFT)))
	return card
