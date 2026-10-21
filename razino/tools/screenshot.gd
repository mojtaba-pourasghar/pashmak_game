extends Node
## Renders each screen and writes a PNG, so the UI can be inspected without a
## device. Run with:
##   xvfb-run -a godot --path . res://tools/screenshot.tscn
## Output goes to user://shots/ — the console prints the absolute paths.

const SHOTS := [
	{"ui": "UI-02", "name": "02_intro"},
	{"ui": "UI-03", "name": "03_avatar_builder"},
	{"ui": "UI-04", "name": "04_home"},
	{"ui": "UI-05", "name": "05_world_map"},
	{"ui": "UI-06", "name": "06_explore", "payload": {"chapter_id": "season_01"}},
	{"ui": "UI-07", "name": "07_dialogue", "payload": {"chapter_id": "season_01", "scene_id": "S01-E01-SC02"}},
	{"ui": "UI-08", "name": "08_clue_journal"},
	{"ui": "UI-09", "name": "09_puzzle", "payload": {"chapter_id": "season_01", "scene_id": "S01-E02-SC03"}},
	{"ui": "UI-10", "name": "10_chapter_map"},
	{"ui": "UI-11", "name": "11_store"},
	{"ui": "UI-12", "name": "12_wardrobe"},
	{"ui": "UI-13", "name": "13_settings"},
	{"ui": "UI-14", "name": "14_parent_zone"},
	{"ui": "UI-15", "name": "15_downloads"},
	{"ui": "UI-16", "name": "16_error", "payload": {"reason": "chapter_missing"}},
]

const OUT_DIR := "user://shots"

func _ready() -> void:
	DirAccess.make_dir_recursive_absolute(OUT_DIR)
	# Give the player some state so the screens have something to show.
	GameState.player_name = "نگار"
	GameState.mark_position("season_01", "S01-E01", "S01-E01-SC03")
	GameState.add_clue("CLUE_S01_SOUND")
	GameState.add_clue("CLUE_S01_MAP_PIECE")
	GameState.add_clue("CLUE_S01_NOTICE")
	GameState.add_clue("CLUE_S01_DUST")
	AvatarManager.set_part("outfit", 0)
	AvatarManager.set_part("accessory", 3)
	ClueJournal.load_chapter_clues(ContentManager.load_chapter_story("season_01"))

	await get_tree().process_frame
	for shot in SHOTS:
		await _capture(shot)
	print("SCREENSHOTS WRITTEN TO: %s" % ProjectSettings.globalize_path(OUT_DIR))
	get_tree().quit(0)

func _capture(shot: Dictionary) -> void:
	var path: String = Router.SCREENS[String(shot["ui"])]
	var packed: PackedScene = load(path)
	if packed == null:
		push_error("could not load %s" % path)
		return
	Router.payload = shot.get("payload", {})
	var instance := packed.instantiate()
	add_child(instance)
	# Several frames so fonts, layout and the first draw all settle.
	for i in 8:
		await get_tree().process_frame
	await RenderingServer.frame_post_draw

	var image := get_viewport().get_texture().get_image()
	var out := "%s/%s.png" % [OUT_DIR, String(shot["name"])]
	var err := image.save_png(out)
	print("%s -> %s (%dx%d, err %d)" % [shot["ui"], out, image.get_width(), image.get_height(), err])
	instance.queue_free()
	await get_tree().process_frame
