extends RazinoTest
## Instantiates every screen in GDD §7.1 and checks it really builds.
##
## A screen that parses but throws on _ready is the classic way a UI "exists"
## without working, so each one is added to the tree, given a frame, and
## inspected for the things it is supposed to contain.

var _root: Node = null

func before_each() -> void:
	_root = Node.new()
	(Engine.get_main_loop() as SceneTree).root.add_child(_root)

func after_each() -> void:
	if _root != null and is_instance_valid(_root):
		_root.queue_free()
		_root = null

func _instantiate(ui_id: String) -> Control:
	var path: String = Router.SCREENS[ui_id]
	var packed: PackedScene = load(path)
	if packed == null:
		return null
	var instance := packed.instantiate()
	_root.add_child(instance)
	await next_frame()
	return instance as Control

func test_every_screen_id_resolves_to_a_real_scene():
	for ui_id in Router.SCREENS.keys():
		var path: String = Router.SCREENS[ui_id]
		ok(ResourceLoader.exists(path), "%s -> %s exists" % [ui_id, path])

func test_all_sixteen_screens_are_registered():
	eq(Router.SCREENS.size(), 16, "all sixteen GDD screens are routed")

func test_every_screen_builds_without_error():
	for ui_id in Router.SCREENS.keys():
		var screen := await _instantiate(ui_id)
		ok(screen != null, "%s instantiates" % ui_id)
		if screen == null:
			continue
		gt(float(screen.get_child_count()), 0.0, "%s built some content" % ui_id)
		screen.queue_free()
		await next_frame()

func test_screens_lay_out_right_to_left():
	for ui_id in ["UI-04", "UI-08", "UI-10", "UI-13", "UI-14"]:
		var screen := await _instantiate(ui_id)
		if screen is RazinoScreen:
			eq(screen.layout_direction, Control.LAYOUT_DIRECTION_RTL, "%s is RTL" % ui_id)
		screen.queue_free()
		await next_frame()

func test_home_offers_a_way_into_the_story():
	var home := await _instantiate("UI-04")
	ok(_find_button_containing(home, "ماجرا") != null,
		"the home screen has a continue/start button")
	home.queue_free()

func test_store_refuses_to_buy_without_parental_consent():
	ParentalSettings.set_consent("allow_purchases", false)
	var store := await _instantiate("UI-11")
	var buy := _find_button_containing(store, "تهیهٔ این فصل")
	ok(buy != null, "the store lists a purchasable chapter")
	if buy != null:
		ok(buy.disabled, "and the buy button is disabled until a parent allows purchases")
	store.queue_free()

func test_parent_zone_starts_behind_the_gate():
	var zone := await _instantiate("UI-14")
	var found_pin := _find_node_of_type(zone, "LineEdit") != null
	ok(found_pin, "the parent section opens on the PIN gate, not on the settings")
	ok(_find_button_containing(zone, "حذف کامل پیشرفت بازی") == null,
		"and the destructive controls are not reachable before the gate is passed")
	zone.queue_free()

func test_error_screen_explains_each_known_failure():
	for reason in ["catalog_missing", "save_unreadable", "chapter_missing", "story_unreadable", "network"]:
		Router.payload = {"reason": reason}
		var screen := await _instantiate("UI-16")
		var text := _collect_text(screen)
		not_empty(text, "the error screen for '%s' says something" % reason)
		contains(text, "چه کار کنیم؟", "and tells the player what to do about '%s'" % reason)
		screen.queue_free()
		await next_frame()

func test_puzzle_panel_handles_every_puzzle_kind():
	## Every kind the schema allows must render something a player can act on.
	var kinds := {}
	for cid in ContentManager.chapter_ids():
		var story := ContentManager.load_chapter_story(cid)
		for s in story.get("scenes", []):
			var p: Dictionary = (s as Dictionary).get("puzzle", {})
			if not p.is_empty():
				kinds[String(p.get("kind", ""))] = p
	gt(float(kinds.size()), 4.0, "the chapters between them use most puzzle kinds (%d)" % kinds.size())

	for kind in kinds.keys():
		var panel := PuzzlePanel.new()
		_root.add_child(panel)
		await next_frame()
		panel.load_puzzle(kinds[kind])
		await next_frame()
		var text := _collect_text(panel)
		not_empty(text, "the '%s' puzzle renders a prompt and controls" % kind)
		panel.queue_free()
		await next_frame()

func test_dialogue_panel_shows_a_persian_line():
	var panel := DialoguePanel.new()
	_root.add_child(panel)
	await next_frame()
	panel.show_line({"text": "صدای ساعت را شنیدی؟", "audio_id": ""}, "آراد")
	await next_frame()
	var text := _collect_text(panel)
	contains(text, "آراد", "the speaker's name is shown")
	panel.queue_free()

# — helpers ——————————————————————————————————————————————

func _find_button_containing(node: Node, needle: String) -> Button:
	if node is Button and (node as Button).text.contains(needle):
		return node
	for child in node.get_children():
		var found := _find_button_containing(child, needle)
		if found != null:
			return found
	return null

func _find_node_of_type(node: Node, type_name: String) -> Node:
	if node.is_class(type_name):
		return node
	for child in node.get_children():
		var found := _find_node_of_type(child, type_name)
		if found != null:
			return found
	return null

func _collect_text(node: Node) -> String:
	var out := ""
	if node is Label:
		out += (node as Label).text + " "
	elif node is Button:
		out += (node as Button).text + " "
	elif node is RichTextLabel:
		out += (node as RichTextLabel).text + " "
	for child in node.get_children():
		out += _collect_text(child)
	return out
