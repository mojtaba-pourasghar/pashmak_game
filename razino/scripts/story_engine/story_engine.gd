extends Node
## StoryEngine — scene, dialogue, condition and choice execution (GDD §4, §8.3).
##
## Everything a chapter does is data. A writer adds scenes, dialogue, choices,
## clues and puzzles to a JSON file; no GDScript is written for new chapters.
## The engine only knows a fixed vocabulary of CONDITIONS and EFFECTS, listed
## below — which is also why a downloaded chapter can never run arbitrary code.

signal chapter_loaded(chapter_id: String)
signal scene_entered(scene: Dictionary)
signal line_shown(line: Dictionary, index: int, total: int)
signal choices_offered(choices: Array)
signal choice_made(choice_id: String)
signal puzzle_requested(puzzle: Dictionary)
signal cinematic_requested(cinematic: Dictionary)
signal chapter_finished(chapter_id: String, ending_id: String)
signal story_error(message: String)

## Condition keys the engine understands.
const CONDITIONS := [
	"has_clue", "lacks_clue", "has_item", "lacks_item",
	"flag", "not_flag", "trust_at_least", "trust_below",
	"chapter_complete", "any", "all",
]
## Effect keys the engine understands.
const EFFECTS := [
	"add_clue", "add_item", "set_flag", "add_trust",
	"unlock_chapter", "play_sfx", "play_music", "complete_chapter", "set_ending",
]

var story: Dictionary = {}
var chapter_id: String = ""
var scenes_by_id: Dictionary = {}

var current_scene: Dictionary = {}
var current_line_index: int = -1
var _finished: bool = false

# — loading ——————————————————————————————————————————————

func load_chapter(id: String) -> bool:
	var data := ContentManager.load_chapter_story(id)
	if data.is_empty():
		_error("محتوای فصل «%s» در دسترس نیست." % id)
		return false
	return load_story(data)

## Takes an already-parsed chapter. Used by load_chapter() and by the tests,
## which feed it fixtures directly.
func load_story(data: Dictionary) -> bool:
	var problems := validate_story(data)
	if not problems.is_empty():
		_error("داده‌های فصل ایراد دارد: %s" % problems[0])
		Log.error("Story", "validation failed: %s" % ", ".join(problems))
		return false
	story = data
	chapter_id = String(data.get("chapter_id", ""))
	scenes_by_id.clear()
	for s in data.get("scenes", []):
		scenes_by_id[String((s as Dictionary).get("scene_id", ""))] = s
	_finished = false
	ClueJournal.load_chapter_clues(data)
	chapter_loaded.emit(chapter_id)
	Log.info("Story", "chapter %s loaded — %d scenes" % [chapter_id, scenes_by_id.size()])
	return true

func start(from_scene_id: String = "") -> void:
	var target := from_scene_id
	if target.is_empty():
		target = String(story.get("start_scene_id", ""))
	if target.is_empty() or not scenes_by_id.has(target):
		_error("نقطهٔ شروع فصل پیدا نشد.")
		return
	enter_scene(target)

## Picks up exactly where the player left off (GDD §9).
func resume() -> void:
	var saved := GameState.current_scene
	if GameState.current_chapter == chapter_id and scenes_by_id.has(saved):
		enter_scene(saved)
	else:
		start()

# — scene flow ———————————————————————————————————————————

func enter_scene(scene_id: String) -> void:
	if not scenes_by_id.has(scene_id):
		_error("صحنهٔ «%s» پیدا نشد." % scene_id)
		return
	current_scene = scenes_by_id[scene_id]
	current_line_index = -1

	GameState.mark_position(chapter_id, String(current_scene.get("episode_id", "")), scene_id)
	GameState.set_flag("seen:%s:%s" % [chapter_id, scene_id], true)
	SaveManager.mark_dirty()

	apply_effects(current_scene.get("on_enter", []))

	var music := String(current_scene.get("music_id", ""))
	if not music.is_empty():
		AudioManager.play_music(music)

	scene_entered.emit(current_scene)
	Log.info("Story", "scene %s (%s)" % [scene_id, current_scene.get("scene_type", "?")])

	match String(current_scene.get("scene_type", "dialogue")):
		"puzzle":
			puzzle_requested.emit(current_scene.get("puzzle", {}))
		"cinematic":
			cinematic_requested.emit(current_scene.get("cinematic", {}))
		_:
			advance()

## Moves to the next dialogue line, or — once the lines run out — to the
## choices, or to the next scene.
func advance() -> void:
	var lines: Array = current_scene.get("dialogue", [])
	current_line_index += 1
	if current_line_index < lines.size():
		var line: Dictionary = lines[current_line_index]
		apply_effects(line.get("effects", []))
		AudioManager.play_voice(String(line.get("audio_id", "")), String(line.get("text", "")))
		line_shown.emit(line, current_line_index, lines.size())
		return
	_after_dialogue()

func _after_dialogue() -> void:
	var choices := available_choices()
	if not choices.is_empty():
		choices_offered.emit(choices)
		return
	var next := String(current_scene.get("next_scene_id", ""))
	if next.is_empty():
		_finish_chapter()
		return
	enter_scene(next)

## Choices whose conditions are met right now. A choice the player cannot take
## is hidden rather than shown greyed-out, so no option reads as a tease.
func available_choices() -> Array:
	var out: Array = []
	for c in current_scene.get("choices", []):
		var cd: Dictionary = c
		if evaluate(cd.get("conditions", {})):
			out.append(cd)
	return out

func choose(choice_id: String) -> void:
	for c in available_choices():
		var cd: Dictionary = c
		if String(cd.get("choice_id", "")) != choice_id:
			continue
		apply_effects(cd.get("effects", []))
		choice_made.emit(choice_id)
		AudioManager.play_sfx("SFX_UI_CHOICE")
		SaveManager.mark_dirty()
		var next := String(cd.get("next_scene_id", ""))
		if next.is_empty():
			_finish_chapter()
		else:
			enter_scene(next)
		return
	_error("این انتخاب در دسترس نیست.")

## Called by the puzzle host once a puzzle is solved or skipped.
func puzzle_resolved(solved: bool) -> void:
	var puzzle: Dictionary = current_scene.get("puzzle", {})
	if solved:
		apply_effects(puzzle.get("on_solve", []))
	else:
		apply_effects(puzzle.get("on_skip", []))
	SaveManager.mark_dirty()
	var key := "next_scene_id" if solved else "skip_scene_id"
	var next := String(puzzle.get(key, current_scene.get("next_scene_id", "")))
	if next.is_empty():
		_finish_chapter()
	else:
		enter_scene(next)

func cinematic_finished() -> void:
	var next := String(current_scene.get("next_scene_id", ""))
	if next.is_empty():
		_finish_chapter()
	else:
		enter_scene(next)

func _finish_chapter() -> void:
	if _finished:
		return
	_finished = true
	var ending := GameState.ending_id
	if ending.is_empty():
		ending = String(story.get("default_ending_id", "ending_default"))
	GameState.endings_seen[ending] = true
	GameState.mark_chapter_complete(chapter_id)
	SaveManager.save()
	chapter_finished.emit(chapter_id, ending)
	Log.info("Story", "chapter %s finished with ending %s" % [chapter_id, ending])

# — conditions ———————————————————————————————————————————

## An empty condition block is always true. Unknown keys fail CLOSED and are
## logged, so a typo in content data cannot silently open a path.
func evaluate(cond: Variant) -> bool:
	if cond == null:
		return true
	if typeof(cond) == TYPE_ARRAY:
		for c in (cond as Array):
			if not evaluate(c):
				return false
		return true
	if typeof(cond) != TYPE_DICTIONARY:
		return true
	var d: Dictionary = cond
	if d.is_empty():
		return true
	for key in d.keys():
		var value = d[key]
		match String(key):
			"has_clue":
				if not _all_true(value, func(v): return GameState.has_clue(String(v))): return false
			"lacks_clue":
				if not _all_true(value, func(v): return not GameState.has_clue(String(v))): return false
			"has_item":
				if not _all_true(value, func(v): return GameState.has_item(String(v))): return false
			"lacks_item":
				if not _all_true(value, func(v): return not GameState.has_item(String(v))): return false
			"flag":
				if not _all_true(value, func(v): return bool(GameState.get_flag(String(v), false))): return false
			"not_flag":
				if not _all_true(value, func(v): return not bool(GameState.get_flag(String(v), false))): return false
			"trust_at_least":
				for who in (value as Dictionary).keys():
					if GameState.get_trust(String(who)) < int((value as Dictionary)[who]): return false
			"trust_below":
				for who in (value as Dictionary).keys():
					if GameState.get_trust(String(who)) >= int((value as Dictionary)[who]): return false
			"chapter_complete":
				if not _all_true(value, func(v): return GameState.is_chapter_complete(String(v))): return false
			"all":
				for sub in (value as Array):
					if not evaluate(sub): return false
			"any":
				var ok := false
				for sub in (value as Array):
					if evaluate(sub):
						ok = true
						break
				if not ok: return false
			_:
				Log.error("Story", "unknown condition '%s' — treated as false" % key)
				return false
	return true

func _all_true(value: Variant, predicate: Callable) -> bool:
	if typeof(value) == TYPE_ARRAY:
		for v in (value as Array):
			if not predicate.call(v):
				return false
		return true
	return predicate.call(value)

# — effects ——————————————————————————————————————————————

func apply_effects(effects: Variant) -> void:
	if typeof(effects) != TYPE_ARRAY:
		return
	for e in (effects as Array):
		if typeof(e) != TYPE_DICTIONARY:
			continue
		apply_effect(e)

func apply_effect(effect: Dictionary) -> void:
	for key in effect.keys():
		var value = effect[key]
		match String(key):
			"add_clue":
				_for_each(value, func(v): GameState.add_clue(String(v)))
			"add_item":
				_for_each(value, func(v): GameState.add_item(String(v)))
			"set_flag":
				for flag in (value as Dictionary).keys():
					GameState.set_flag(String(flag), (value as Dictionary)[flag])
			"add_trust":
				for who in (value as Dictionary).keys():
					GameState.add_trust(String(who), int((value as Dictionary)[who]))
			"unlock_chapter":
				_for_each(value, func(v): GameState.unlock_chapter(String(v)))
			"play_sfx":
				AudioManager.play_sfx(String(value))
			"play_music":
				AudioManager.play_music(String(value))
			"set_ending":
				GameState.ending_id = String(value)
			"complete_chapter":
				GameState.mark_chapter_complete(String(value) if typeof(value) == TYPE_STRING else chapter_id)
			_:
				Log.error("Story", "unknown effect '%s' — ignored" % key)
	SaveManager.mark_dirty()

func _for_each(value: Variant, action: Callable) -> void:
	if typeof(value) == TYPE_ARRAY:
		for v in (value as Array):
			action.call(v)
	else:
		action.call(value)

# — validation ———————————————————————————————————————————
# Mirrors content_tools/validate_content.py so that a package which passed the
# offline validator also passes at runtime, and vice versa.

func validate_story(data: Dictionary) -> PackedStringArray:
	var problems := PackedStringArray()
	if String(data.get("chapter_id", "")).is_empty():
		problems.append("chapter_id خالی است")
	var scenes = data.get("scenes", [])
	if typeof(scenes) != TYPE_ARRAY or (scenes as Array).is_empty():
		problems.append("فصل هیچ صحنه‌ای ندارد")
		return problems

	var ids := {}
	for s in (scenes as Array):
		if typeof(s) != TYPE_DICTIONARY:
			problems.append("یک صحنه دیکشنری نیست")
			continue
		var sid := String((s as Dictionary).get("scene_id", ""))
		if sid.is_empty():
			problems.append("یک صحنه بدون scene_id است")
		elif ids.has(sid):
			problems.append("شناسهٔ صحنهٔ تکراری: %s" % sid)
		else:
			ids[sid] = s

	var start_id := String(data.get("start_scene_id", ""))
	if not ids.has(start_id):
		problems.append("start_scene_id نامعتبر: %s" % start_id)

	# Every jump must land somewhere real.
	for sid in ids.keys():
		var s: Dictionary = ids[sid]
		for target in _scene_targets(s):
			if not ids.has(target):
				problems.append("صحنهٔ %s به مقصد نامعتبر «%s» اشاره می‌کند" % [sid, target])
		# A scene that is not an ending must offer a way onward.
		if not bool(s.get("is_ending", false)) and _scene_targets(s).is_empty() \
				and String(s.get("scene_type", "dialogue")) != "cinematic":
			if (s.get("choices", []) as Array).is_empty() and String(s.get("next_scene_id", "")).is_empty():
				problems.append("صحنهٔ %s بن‌بست است" % sid)
		# No choice may be impossible to ever satisfy by construction.
		for c in s.get("choices", []):
			var cd: Dictionary = c
			if String(cd.get("choice_id", "")).is_empty():
				problems.append("انتخاب بدون شناسه در صحنهٔ %s" % sid)
			if String(cd.get("text", "")).strip_edges().is_empty():
				problems.append("متن انتخاب خالی در صحنهٔ %s" % sid)

	# Reachability from the start.
	if ids.has(start_id):
		var reached := _reachable_from(start_id, ids)
		for sid in ids.keys():
			if not reached.has(sid):
				problems.append("صحنهٔ %s از شروع قابل دسترسی نیست" % sid)
	return problems

func _scene_targets(s: Dictionary) -> PackedStringArray:
	var out := PackedStringArray()
	var n := String(s.get("next_scene_id", ""))
	if not n.is_empty():
		out.append(n)
	for c in s.get("choices", []):
		var t := String((c as Dictionary).get("next_scene_id", ""))
		if not t.is_empty() and not out.has(t):
			out.append(t)
	var p: Dictionary = s.get("puzzle", {})
	for key in ["next_scene_id", "skip_scene_id"]:
		var t2 := String(p.get(key, ""))
		if not t2.is_empty() and not out.has(t2):
			out.append(t2)
	return out

func _reachable_from(start_id: String, ids: Dictionary) -> Dictionary:
	var seen := {start_id: true}
	var queue := [start_id]
	while not queue.is_empty():
		var sid: String = queue.pop_front()
		for t in _scene_targets(ids.get(sid, {})):
			if not seen.has(t) and ids.has(t):
				seen[t] = true
				queue.append(t)
	return seen

func _error(message: String) -> void:
	Log.error("Story", message)
	story_error.emit(message)
