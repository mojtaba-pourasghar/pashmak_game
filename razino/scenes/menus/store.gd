extends RazinoScreen
## UI-11 — فروشگاه.
##
## GDD §11.1 sets the rules this screen has to keep: every price and exactly
## what it contains is shown before buying, nothing already owned can be
## bought twice, and no puzzle is ever solvable by paying. There are no ads,
## no countdowns and no "limited offer" pressure — this is a store for a
## ten-year-old's parent, not a funnel.

var _status: Label

func _ready() -> void:
	configure("فروشگاه", "UI-04")
	super._ready()

func build_content() -> void:
	if not PurchaseManager.is_live():
		content.add_child(make_notice(
			"حالت آزمایشی: اتصال به فروشگاه هنوز انجام نشده و هیچ پرداخت واقعی‌ای ممکن نیست. آنچه اینجا «خریده» می‌شود فقط برای آزمایش است.",
			Palette.BRASS))
	if not ParentalSettings.allow_purchases:
		var notice := make_notice("خرید در بخش والدین فعال نشده است.", Palette.ROSE)
		content.add_child(notice)
		var go := Style.make_button("رفتن به بخش والدین", "quiet")
		go.pressed.connect(func(): Router.go("UI-14"))
		content.add_child(go)

	_status = Style.make_body("", Palette.FS_LABEL, Palette.SNOW)
	content.add_child(_status)

	PurchaseManager.purchase_succeeded.connect(func(pid):
		_status.text = "«%s» فعال شد." % pid
		build_refresh())
	PurchaseManager.purchase_failed.connect(func(_pid, reason): _status.text = reason)

	var list := make_scroll()
	list.name = "StoreList"
	_fill(list)

func build_refresh() -> void:
	var list := content.find_child("StoreList", true, false)
	if list is VBoxContainer:
		for c in (list as VBoxContainer).get_children():
			c.queue_free()
		_fill(list as VBoxContainer)

func _fill(list: VBoxContainer) -> void:
	list.add_child(Style.make_body(
		"فصل اول رایگان است و برای فهمیدن اینکه بازی را دوست دارید یا نه کافی است. هیچ معمایی با پرداخت حل نمی‌شود.",
		Palette.FS_CAPTION, Palette.SNOW_SOFT))

	for entry in ContentManager.catalog:
		var e: Dictionary = entry
		if String(e.get("access_type", "paid")) == "free":
			continue
		list.add_child(_product_card(e))

func _product_card(entry: Dictionary) -> Control:
	var cid := String(entry.get("chapter_id", ""))
	var owned := PurchaseManager.has_entitlement(cid)

	var card := Style.make_night_card()
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", Palette.PAD_MD)
	card.add_child(row)

	var thumb := PlaceholderArt.new()
	thumb.background_id = cid
	thumb.show_badge = false
	thumb.custom_minimum_size = Vector2(130, 90)
	row.add_child(thumb)

	var col := VBoxContainer.new()
	col.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	row.add_child(col)
	col.add_child(Style.make_title(String(entry.get("title", cid)), Palette.FS_HEADING))
	# What you get, stated before any price — GDD §11.1.
	col.add_child(Style.make_body("%s صحنه در %s قسمت، با ذخیرهٔ کامل پیشرفت و اجرای آفلاین." % [
		Fa.num(int(entry.get("scene_count", 0))), Fa.num(int(entry.get("episode_count", 0)))],
		Palette.FS_CAPTION, Palette.SNOW_SOFT))
	col.add_child(Style.make_body(String(entry.get("synopsis", "")).left(150) + "…",
		Palette.FS_CAPTION, Palette.SNOW_SOFT))

	var actions := VBoxContainer.new()
	actions.custom_minimum_size.x = 230
	row.add_child(actions)

	if owned:
		actions.add_child(Style.make_body("تهیه شده", Palette.FS_LABEL, Palette.OK))
		var go := Style.make_button("رفتن به دانلودها", "quiet")
		go.pressed.connect(func(): Router.go("UI-15", {"chapter_id": cid}))
		actions.add_child(go)
		return card

	# Prices are set by the server, not compiled into the app, so they can be
	# adjusted without a store release (GDD §6). Until that endpoint exists we
	# say so rather than showing an invented number.
	actions.add_child(Style.make_body("قیمت از سرور خوانده می‌شود", Palette.FS_CAPTION, Palette.SNOW_SOFT))
	actions.add_child(Style.make_body("هنوز تنظیم نشده", Palette.FS_LABEL, Palette.BRASS))

	var buy := Style.make_button("تهیهٔ این فصل")
	buy.disabled = not ParentalSettings.allow_purchases
	buy.pressed.connect(func():
		_status.text = "در حال اتصال به فروشگاه…"
		await PurchaseManager.purchase(cid))
	actions.add_child(buy)
	return card
