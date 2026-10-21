extends RazinoScreen
## UI-15 — دانلودها.
##
## Shows what is on the device, what can be fetched, and — when something goes
## wrong — exactly what went wrong in words a parent can act on. A failed or
## interrupted download never removes the version already installed.

var _list: VBoxContainer
var _status: Label

func _ready() -> void:
	configure("دانلودها", "UI-04")
	super._ready()

func build_content() -> void:
	var payload := Router.take_payload()

	if not ContentEndpoints.is_configured():
		content.add_child(make_notice(
			"سرور محتوا هنوز تنظیم نشده است، بنابراین فصل تازه‌ای دریافت نمی‌شود. فصل‌هایی که روی دستگاه هستند بدون اینترنت کار می‌کنند.",
			Palette.BRASS))

	_status = Style.make_body("", Palette.FS_LABEL, Palette.SNOW)
	content.add_child(_status)

	_list = make_scroll()
	_refresh()

	ContentManager.download_progress.connect(func(cid, ratio):
		_status.text = "در حال دریافت %s — %d٪" % [cid, int(ratio * 100.0)])
	ContentManager.download_completed.connect(func(cid):
		_status.text = "«%s» دریافت شد." % cid
		_refresh())
	ContentManager.download_failed.connect(func(_cid, reason):
		_status.text = reason)

	var wanted := String(payload.get("chapter_id", ""))
	if not wanted.is_empty():
		_status.text = "برای دریافت «%s» دکمهٔ آن را بزنید." % wanted

func _refresh() -> void:
	for c in _list.get_children():
		c.queue_free()

	var used_mb := float(ContentManager.storage_bytes_used()) / (1024.0 * 1024.0)
	_list.add_child(Style.make_body("فضای استفاده‌شده: %.1f مگابایت" % used_mb,
		Palette.FS_CAPTION, Palette.SNOW_SOFT))

	for entry in ContentManager.catalog:
		_list.add_child(_row(entry as Dictionary))

func _row(entry: Dictionary) -> Control:
	var cid := String(entry.get("chapter_id", ""))
	var installed := ContentManager.is_installed(cid)
	var bundled := ContentManager.is_bundled(cid)
	var needs_update := ContentManager.needs_update(cid)

	var card := Style.make_night_card()
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", Palette.PAD_MD)
	card.add_child(row)

	var col := VBoxContainer.new()
	col.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	row.add_child(col)
	col.add_child(Style.make_title(String(entry.get("title", cid)), Palette.FS_LABEL))

	var state := ""
	if bundled:
		state = "همراه بازی نصب شده — بدون اینترنت کار می‌کند"
	elif installed and needs_update:
		state = "نسخهٔ تازه‌تری موجود است (نسخهٔ %d)" % int(entry.get("version", 1))
	elif installed:
		state = "روی دستگاه است — بدون اینترنت کار می‌کند"
	else:
		state = "روی دستگاه نیست"
	col.add_child(Style.make_body(state, Palette.FS_CAPTION,
		Palette.OK if installed and not needs_update else Palette.SNOW_SOFT))

	var actions := VBoxContainer.new()
	actions.custom_minimum_size.x = 210
	row.add_child(actions)

	if not bundled:
		if not installed or needs_update:
			var get_btn := Style.make_button("دریافت" if not installed else "به‌روزرسانی", "secondary")
			get_btn.disabled = not ContentEndpoints.is_configured()
			get_btn.pressed.connect(func():
				_status.text = "در حال شروع دریافت…"
				await ContentManager.download_chapter(cid))
			actions.add_child(get_btn)
		if installed:
			var remove := Style.make_button("حذف از دستگاه", "quiet")
			remove.pressed.connect(func():
				ContentManager.uninstall_chapter(cid)
				_status.text = "«%s» از دستگاه حذف شد. خریدش محفوظ است." % String(entry.get("title", cid))
				_refresh())
			actions.add_child(remove)
	return card
