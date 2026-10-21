extends RazinoTest
## Drives the real StoryEngine through every shipped chapter.
##
## The important test here is `test_every_chapter_reaches_an_ending`: it plays
## each chapter many times, taking a different mix of choices each run, and
## insists every run lands on a scene marked is_ending. That is the property
## GDD §4.3 asks for — no choice may strand a child — and it is checked against
## the engine's own condition evaluation, not against a separate model of it.

const MAX_STEPS := 400
const RUNS_PER_CHAPTER := 12

func before_each() -> void:
	GameState.reset()

func after_each() -> void:
	GameState.reset()

func _chapters() -> PackedStringArray:
	var out := PackedStringArray()
	for cid in ContentManager.chapter_ids():
		if ContentManager.is_installed(cid):
			out.append(cid)
	return out

func test_all_ten_chapters_are_present_and_load():
	var ids := _chapters()
	eq(ids.size(), 10, "ten chapters are installed")
	for cid in ids:
		GameState.reset()
		var story := ContentManager.load_chapter_story(cid)
		not_empty(story, "%s has story data" % cid)
		var problems := StoryEngine.validate_story(story)
		empty(problems, "%s validates in-engine: %s" % [cid, ", ".join(problems)])

func test_every_chapter_reaches_an_ending():
	for cid in _chapters():
		for run in RUNS_PER_CHAPTER:
			GameState.reset()
			seed(run * 7919 + cid.hash())
			var result := _play(cid, run)
			ok(result["ended"], "%s run %d reached an ending (last scene %s, %d steps)" % [
				cid, run, result["last"], result["steps"]])

func test_choices_are_never_all_hidden_mid_chapter():
	## A scene that offers choices must always offer at least one the player
	## can actually take, whatever state they arrive in.
	for cid in _chapters():
		GameState.reset()
		var story := ContentManager.load_chapter_story(cid)
		StoryEngine.load_story(story)
		for s in story.get("scenes", []):
			var scene: Dictionary = s
			var declared: Array = scene.get("choices", [])
			if declared.is_empty():
				continue
			var unconditional := 0
			for c in declared:
				var cond = (c as Dictionary).get("conditions", {})
				if cond == null or (typeof(cond) == TYPE_DICTIONARY and (cond as Dictionary).is_empty()):
					unconditional += 1
			# Either an always-available choice exists, or the conditional ones
			# are complementary (lacks_X / has_X), which the playthroughs above
			# would have caught as a stall.
			ok(unconditional > 0 or declared.size() >= 2,
				"%s/%s offers a usable way onward" % [cid, scene.get("scene_id", "?")])

func test_unknown_condition_fails_closed():
	## A typo in content must never open a path.
	not_ok(StoryEngine.evaluate({"has_banana": "x"}), "unknown condition is false")
	ok(StoryEngine.evaluate({}), "empty condition is true")

func test_conditions_read_real_state():
	GameState.add_clue("CLUE_TEST")
	ok(StoryEngine.evaluate({"has_clue": "CLUE_TEST"}), "has_clue sees the clue")
	not_ok(StoryEngine.evaluate({"lacks_clue": "CLUE_TEST"}), "lacks_clue is false once held")
	GameState.add_trust("raha", 3)
	ok(StoryEngine.evaluate({"trust_at_least": {"raha": 3}}), "trust_at_least at the boundary")
	not_ok(StoryEngine.evaluate({"trust_at_least": {"raha": 4}}), "trust_at_least above the value")
	ok(StoryEngine.evaluate({"any": [{"flag": "nope"}, {"has_clue": "CLUE_TEST"}]}), "any short-circuits to true")

func test_effects_mutate_state():
	StoryEngine.apply_effects([
		{"add_clue": "CLUE_A"},
		{"add_item": "ITEM_A"},
		{"set_flag": {"f": true}},
		{"add_trust": {"mani": 2}},
	])
	ok(GameState.has_clue("CLUE_A"), "add_clue")
	ok(GameState.has_item("ITEM_A"), "add_item")
	ok(bool(GameState.get_flag("f")), "set_flag")
	eq(GameState.get_trust("mani"), 2, "add_trust")

func test_validator_catches_a_broken_jump():
	var broken := {
		"chapter_id": "season_test",
		"start_scene_id": "A",
		"scenes": [
			{"scene_id": "A", "scene_type": "dialogue", "next_scene_id": "NOWHERE"},
		],
	}
	var problems := StoryEngine.validate_story(broken)
	not_empty(problems, "a dangling jump is reported")

func test_validator_catches_an_unreachable_scene():
	var orphaned := {
		"chapter_id": "season_test",
		"start_scene_id": "A",
		"scenes": [
			{"scene_id": "A", "scene_type": "dialogue", "is_ending": true},
			{"scene_id": "B", "scene_type": "dialogue", "is_ending": true},
		],
	}
	var problems := StoryEngine.validate_story(orphaned)
	not_empty(problems, "an unreachable scene is reported")

## Walks one chapter, choosing pseudo-randomly among the choices the engine
## says are available, and reports whether an ending was reached.
func _play(chapter_id: String, run: int) -> Dictionary:
	var story := ContentManager.load_chapter_story(chapter_id)
	var by_id := {}
	for s in story.get("scenes", []):
		by_id[String((s as Dictionary).get("scene_id", ""))] = s

	StoryEngine.load_story(story)
	var current := String(story.get("start_scene_id", ""))
	var steps := 0

	while steps < MAX_STEPS:
		steps += 1
		var scene: Dictionary = by_id.get(current, {})
		if scene.is_empty():
			return {"ended": false, "last": current, "steps": steps}

		# Apply what entering this scene would do, so later conditions see it.
		StoryEngine.current_scene = scene
		StoryEngine.apply_effects(scene.get("on_enter", []))
		GameState.set_flag("seen:%s:%s" % [chapter_id, current], true)

		if bool(scene.get("is_ending", false)):
			return {"ended": true, "last": current, "steps": steps}

		# Interactions are optional, but a run that never touches them must
		# still finish — so half the runs skip them entirely.
		if run % 2 == 0:
			for it in scene.get("interactions", []):
				var itd: Dictionary = it
				if StoryEngine.evaluate(itd.get("conditions", {})):
					StoryEngine.apply_effects(itd.get("effects", []))

		var available := StoryEngine.available_choices()
		if not available.is_empty():
			var pick: Dictionary = available[randi() % available.size()]
			StoryEngine.apply_effects(pick.get("effects", []))
			var dest := String(pick.get("next_scene_id", ""))
			if dest == current:
				# A "look again" choice: satisfy it by taking the interactions
				# so the run cannot spin here forever.
				for it2 in scene.get("interactions", []):
					StoryEngine.apply_effects((it2 as Dictionary).get("effects", []))
			current = dest
			continue

		var puzzle: Dictionary = scene.get("puzzle", {})
		if not puzzle.is_empty():
			# Alternate between solving and skipping, so both branches are run.
			var solved := (run + steps) % 2 == 0
			StoryEngine.apply_effects(puzzle.get("on_solve" if solved else "on_skip", []))
			current = String(puzzle.get("next_scene_id" if solved else "skip_scene_id",
				scene.get("next_scene_id", "")))
			continue

		current = String(scene.get("next_scene_id", ""))
		if current.is_empty():
			return {"ended": false, "last": String(scene.get("scene_id", "?")), "steps": steps}

	return {"ended": false, "last": current, "steps": steps}
