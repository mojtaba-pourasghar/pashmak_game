extends RazinoScreen
## UI-14 — بخش والدین.
##
## GDD §14 is clear that this is part of the product, not an extra. It sits
## behind a PIN rather than behind one arithmetic question, and it is the only
## place that can turn on purchases, the camera, or server processing of a
## photo. It also has to be able to delete data and prove it.

var _body: VBoxContainer
var _pin_field: LineEdit
var _message: Label

func _ready() -> void:
	configure("بخش والدین", "UI-04")
	super._ready()

func build_content() -> void:
	_body = VBoxContainer.new()
	_body.size_flags_vertical = Control.SIZE_EXPAND_FILL
	_body.add_theme_constant_override("separation", Palette.PAD_MD)
	content.add_child(_body)
	_show_gate()

# — gate ———————————————————————————————————————————————————

func _show_gate() -> void:
	_clear()
	var card := Style.make_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	_body.add_child(card)

	var first_time := not ParentalSettings.has_pin()
	col.add_child(Style.make_title(
		"این بخش برای بزرگ‌ترهاست" if not first_time else "یک رمز چهاررقمی بگذارید",
		Palette.FS_HEADING, Palette.INK))
	col.add_child(Style.make_body(
		"خرید، دوربین و حذف داده از اینجا کنترل می‌شوند." if not first_time
		else "این رمز جلوی خرید و تغییرات ناخواسته را می‌گیرد. آن را جایی بنویسید؛ راهی برای بازیابی‌اش در خود بازی نیست.",
		Palette.FS_CAPTION, Palette.INK_SOFT))

	_pin_field = LineEdit.new()
	_pin_field.secret = true
	_pin_field.max_length = 4
	_pin_field.placeholder_text = "۴ رقم"
	_pin_field.custom_minimum_size.y = Palette.TOUCH_MIN
	col.add_child(_pin_field)

	_message = Style.make_body("", Palette.FS_CAPTION, Palette.ROSE)
	col.add_child(_message)

	var go := Style.make_button("ورود" if not first_time else "ثبت رمز")
	go.pressed.connect(func(): _submit(first_time))
	col.add_child(go)

	if ParentalSettings.is_locked_out():
		_message.text = "به دلیل تلاش‌های ناموفق، چند لحظه صبر کنید."

func _submit(first_time: bool) -> void:
	var pin := _pin_field.text.strip_edges()
	if first_time:
		if ParentalSettings.set_pin(pin):
			_show_panel()
		else:
			_message.text = "رمز باید دقیقاً چهار رقم باشد."
		return
	if ParentalSettings.is_locked_out():
		_message.text = "هنوز قفل است. %d ثانیه دیگر دوباره امتحان کنید." % int(ParentalSettings.seconds_until_unlock())
		return
	if ParentalSettings.verify(pin):
		_show_panel()
	else:
		_message.text = "رمز درست نیست."

# — panel ——————————————————————————————————————————————————

func _show_panel() -> void:
	_clear()
	var list := VBoxContainer.new()
	list.add_theme_constant_override("separation", Palette.PAD_MD)
	var scroll := ScrollContainer.new()
	scroll.size_flags_vertical = Control.SIZE_EXPAND_FILL
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	scroll.add_child(list)
	_body.add_child(scroll)

	list.add_child(_consent_card())
	list.add_child(_purchases_card())
	list.add_child(_storage_card())
	list.add_child(_privacy_card())
	list.add_child(_data_card())

func _consent_card() -> Control:
	var card := Style.make_night_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title("اجازه‌ها", Palette.FS_HEADING))
	col.add_child(Style.make_body(
		"همهٔ این‌ها به‌صورت پیش‌فرض خاموش‌اند و بازی بدون هیچ‌کدامشان کامل کار می‌کند.",
		Palette.FS_CAPTION, Palette.SNOW_SOFT))
	col.add_child(_consent_row("اجازهٔ خرید درون‌برنامه‌ای", "allow_purchases", ParentalSettings.allow_purchases))
	col.add_child(_consent_row("اجازهٔ استفاده از دوربین برای ساخت آواتار", "allow_camera_avatar", ParentalSettings.allow_camera_avatar))
	var server_row := _consent_row("اجازهٔ پردازش عکس روی سرور", "allow_server_photo_processing", ParentalSettings.allow_server_photo_processing)
	col.add_child(server_row)
	col.add_child(Style.make_body(
		"پردازش سروری فعلاً پیاده‌سازی نشده است؛ تمام ساخت آواتار روی همین دستگاه انجام می‌شود و هیچ عکسی ارسال نمی‌شود.",
		Palette.FS_CAPTION, Palette.BRASS))
	col.add_child(_consent_row("اجازهٔ دانلود با اینترنت همراه", "allow_content_download_on_cellular", ParentalSettings.allow_content_download_on_cellular))
	return card

func _consent_row(label: String, key: String, value: bool) -> Control:
	var b := CheckButton.new()
	b.button_pressed = value
	b.custom_minimum_size.y = Palette.TOUCH_MIN
	b.toggled.connect(func(on): ParentalSettings.set_consent(key, on))
	ParentalSettings.consent_changed.connect(func(changed):
		if changed == "allow_camera_avatar" and key == "allow_server_photo_processing":
			b.button_pressed = ParentalSettings.allow_server_photo_processing)
	return make_row(label, b)

func _purchases_card() -> Control:
	var card := Style.make_night_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title("خریدها", Palette.FS_HEADING))
	var mode_text := "حالت آزمایشی — هیچ پرداخت واقعی‌ای انجام نمی‌شود." if not PurchaseManager.is_live() \
		else "متصل به فروشگاه."
	col.add_child(Style.make_body(mode_text, Palette.FS_CAPTION,
		Palette.BRASS if not PurchaseManager.is_live() else Palette.OK))

	var owned := 0
	for entry in ContentManager.catalog:
		var cid := String((entry as Dictionary).get("chapter_id", ""))
		if PurchaseManager.has_entitlement(cid):
			owned += 1
			col.add_child(Style.make_body("• %s (%s)" % [
				String((entry as Dictionary).get("title", cid)),
				PurchaseManager.entitlement_source(cid)], Palette.FS_CAPTION, Palette.SNOW_SOFT))
	if owned == 0:
		col.add_child(Style.make_body("هنوز چیزی خریداری نشده است.", Palette.FS_CAPTION, Palette.SNOW_SOFT))

	var restore := Style.make_button("بازیابی خریدها", "quiet")
	restore.pressed.connect(func():
		var n := await PurchaseManager.restore_purchases()
		restore.text = "بازیابی شد: %d مورد" % n)
	col.add_child(restore)
	return card

func _storage_card() -> Control:
	var card := Style.make_night_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title("فضای ذخیره‌سازی", Palette.FS_HEADING))
	var mb := float(ContentManager.storage_bytes_used()) / (1024.0 * 1024.0)
	col.add_child(Style.make_body("محتوای دانلودشده: %.1f مگابایت" % mb, Palette.FS_CAPTION, Palette.SNOW_SOFT))
	var manage := Style.make_button("مدیریت دانلودها", "quiet")
	manage.pressed.connect(func(): Router.go("UI-15"))
	col.add_child(manage)
	return card

func _privacy_card() -> Control:
	var card := Style.make_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title("حریم خصوصی", Palette.FS_HEADING, Palette.INK))
	for line in [
		"این بازی حساب کاربری نمی‌خواهد و بدون ثبت‌نام کامل کار می‌کند.",
		"پیشرفت بازی فقط روی همین دستگاه ذخیره می‌شود.",
		"اگر از دوربین استفاده شود، عکس اصلی ذخیره یا ارسال نمی‌شود و بلافاصله پس از ساخت آواتار پاک می‌شود.",
		"هیچ قابلیت تشخیص چهره یا شناسایی هویت در بازی وجود ندارد.",
		"هیچ تبلیغی در بازی نمایش داده نمی‌شود.",
	]:
		col.add_child(Style.make_body("• " + line, Palette.FS_CAPTION, Palette.INK_SOFT))
	return card

func _data_card() -> Control:
	var card := Style.make_night_card()
	var col := VBoxContainer.new()
	card.add_child(col)
	col.add_child(Style.make_title("حذف داده", Palette.FS_HEADING))
	var status := Style.make_body("", Palette.FS_CAPTION, Palette.OK)

	var del_avatar := Style.make_button("حذف آواتار و داده‌های مرتبط", "danger")
	del_avatar.pressed.connect(func():
		AvatarManager.delete_avatar()
		status.text = "آواتار حذف شد. فایل موقت عکس: %s" % (
			"باقی نمانده" if not AvatarManager.temp_file_exists() else "هنوز هست — گزارش دهید"))
	col.add_child(del_avatar)

	var del_save := Style.make_button("حذف کامل پیشرفت بازی", "danger")
	var confirm := false
	del_save.pressed.connect(func():
		if not confirm:
			confirm = true
			del_save.text = "مطمئنید؟ دوباره بزنید"
			return
		SaveManager.delete_save()
		AvatarManager.delete_avatar()
		status.text = "همهٔ پیشرفت حذف شد."
		del_save.text = "حذف کامل پیشرفت بازی"
		confirm = false)
	col.add_child(del_save)
	col.add_child(status)
	return card

func _clear() -> void:
	for c in _body.get_children():
		c.queue_free()
