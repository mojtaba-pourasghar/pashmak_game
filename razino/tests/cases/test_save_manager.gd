extends RazinoTest
## Save, reload, migrate and recover (GDD §9, QA checklist "ثبت و بازیابی").

func before_each() -> void:
	SaveManager.delete_save()
	GameState.reset()

func after_each() -> void:
	SaveManager.delete_save()
	GameState.reset()

func test_round_trip_preserves_progress():
	GameState.player_name = "سارا"
	GameState.add_clue("CLUE_S01_SOUND")
	GameState.add_item("ITEM_NOTEBOOK")
	GameState.set_flag("s01_told", true)
	GameState.add_trust("raha", 3)
	GameState.mark_position("season_01", "S01-E02", "S01-E02-SC03")
	ok(SaveManager.save(), "save succeeds")

	GameState.reset()
	eq(GameState.clues.size(), 0, "state really was cleared")

	ok(SaveManager.load_game(), "load succeeds")
	eq(GameState.player_name, "سارا", "Persian name survives the round trip")
	ok(GameState.has_clue("CLUE_S01_SOUND"), "clue restored")
	ok(GameState.has_item("ITEM_NOTEBOOK"), "item restored")
	eq(GameState.get_trust("raha"), 3, "trust restored")
	eq(GameState.current_scene, "S01-E02-SC03", "resumes at the right scene")

func test_resume_lands_on_the_saved_scene():
	GameState.mark_position("season_01", "S01-E03", "S01-E03-SC03")
	SaveManager.save()
	GameState.reset()
	SaveManager.load_game()
	StoryEngine.load_chapter("season_01")
	StoryEngine.resume()
	eq(String(StoryEngine.current_scene.get("scene_id", "")), "S01-E03-SC03",
		"resume() re-enters the saved scene, not the start")

func test_corrupt_save_falls_back_to_backup():
	GameState.add_clue("CLUE_GOOD")
	SaveManager.save()
	# A second save moves the first to the backup slot.
	GameState.add_clue("CLUE_NEWER")
	SaveManager.save()

	var f := FileAccess.open(SaveManager.SAVE_PATH, FileAccess.WRITE)
	f.store_string("{ this is not json")
	f.close()

	GameState.reset()
	ok(SaveManager.load_game(), "load falls back to the backup rather than failing")
	ok(GameState.has_clue("CLUE_GOOD"), "the backup's contents came back")

func test_interrupted_write_leaves_the_previous_save_intact():
	GameState.add_clue("CLUE_V1")
	SaveManager.save()
	# Simulate a crash having left a half-written temp file behind.
	var tmp := FileAccess.open(SaveManager.TMP_PATH, FileAccess.WRITE)
	tmp.store_string("{\"schema\": 1, \"stat")
	tmp.close()

	GameState.reset()
	ok(SaveManager.load_game(), "the good save still loads")
	ok(GameState.has_clue("CLUE_V1"), "and still has the right contents")

func test_old_schema_is_migrated_not_discarded():
	# Schema 0 stored trust as one integer.
	var legacy := {
		"schema": 0,
		"state": {
			"player_name": "آراد",
			"trust": 4,
			"clues": {"CLUE_OLD": true},
			"current_chapter": "season_01",
			"current_scene": "S01-E01-SC02",
		},
	}
	var f := FileAccess.open(SaveManager.SAVE_PATH, FileAccess.WRITE)
	f.store_string(JSON.stringify(legacy))
	f.close()

	GameState.reset()
	ok(SaveManager.load_game(), "a pre-versioned save still loads")
	eq(GameState.player_name, "آراد", "name migrated")
	ok(GameState.has_clue("CLUE_OLD"), "clues migrated")
	eq(GameState.get_trust("raad"), 4, "the single trust value became raad's")
	eq(GameState.get_trust("raha"), 0, "the other companions start at zero")

func test_chapter_one_is_always_unlocked_after_load():
	var f := FileAccess.open(SaveManager.SAVE_PATH, FileAccess.WRITE)
	f.store_string(JSON.stringify({"schema": 1, "state": {"unlocked_chapters": {}}}))
	f.close()
	GameState.reset()
	SaveManager.load_game()
	ok(GameState.is_chapter_unlocked("season_01"), "the free chapter cannot be locked by a save")
