extends Control
## UI-01 — Splash and first-run routing.
##
## Also the one place that decides where a returning player lands: straight
## back into their chapter if they have progress, the intro if they do not.

const MIN_SPLASH_SECONDS := 0.9

@onready var _status: Label = $Center/Column/Status

func _ready() -> void:
	Log.info("Boot", "starting")
	_status.text = "در حال آماده‌سازی…"
	await get_tree().process_frame
	await _warm_up()
	await get_tree().create_timer(MIN_SPLASH_SECONDS).timeout
	_route_onward()

func _warm_up() -> void:
	# Catalogue first: the home and chapter-map screens both need it.
	if ContentManager.catalog.is_empty():
		ContentManager.load_local_catalog()
	if ContentManager.catalog.is_empty():
		_status.text = "فهرست محتوا خوانده نشد."
		await get_tree().create_timer(1.2).timeout
		Router.go("UI-16", {"reason": "catalog_missing"})
		return
	if SaveManager.has_save():
		if not SaveManager.load_game():
			Log.error("Boot", "save present but unreadable")
			_status.text = "پروندهٔ ذخیره خوانده نشد."
			await get_tree().create_timer(1.2).timeout
			Router.go("UI-16", {"reason": "save_unreadable"})
			return
	_status.text = "آماده است."

func _route_onward() -> void:
	if Router.current_ui_id == "UI-16":
		return
	if SaveManager.has_save() and not GameState.current_chapter.is_empty():
		Router.go("UI-04")
	else:
		Router.go("UI-02")
