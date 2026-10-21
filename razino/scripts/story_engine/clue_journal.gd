extends Node
## دفترچهٔ سرنخ‌ها (UI-08) — GDD §5, screen 3.
##
## More than a list: clues carry a category and can be put side by side. A
## correct pairing is itself a discovery, which is how several chapters open
## their next path without the player being told the answer.

signal clue_added(clue_id: String)
signal combination_found(result_clue_id: String)
signal combination_rejected(a: String, b: String)

## clue_id -> definition, loaded from the active chapter's story data.
var definitions: Dictionary = {}
## Pairs that produce a new clue: "a|b" -> result clue id.
var combinations: Dictionary = {}

func _ready() -> void:
	GameState.clue_discovered.connect(_on_clue_discovered)

func load_chapter_clues(story: Dictionary) -> void:
	definitions.clear()
	combinations.clear()
	for c in story.get("clues", []):
		var cd: Dictionary = c
		definitions[String(cd.get("clue_id", ""))] = cd
	for combo in story.get("clue_combinations", []):
		var kd: Dictionary = combo
		var a := String(kd.get("a", ""))
		var b := String(kd.get("b", ""))
		var result := String(kd.get("result", ""))
		if a.is_empty() or b.is_empty() or result.is_empty():
			continue
		combinations[_pair_key(a, b)] = result
	Log.info("Journal", "%d clues, %d combinations" % [definitions.size(), combinations.size()])

func definition(clue_id: String) -> Dictionary:
	return definitions.get(clue_id, {})

func title_of(clue_id: String) -> String:
	return String(definition(clue_id).get("title", clue_id))

func body_of(clue_id: String) -> String:
	return String(definition(clue_id).get("body", ""))

func category_of(clue_id: String) -> String:
	return String(definition(clue_id).get("category", "general"))

## Clues the player holds, newest last, filtered to the current chapter's set.
func discovered() -> PackedStringArray:
	var out := PackedStringArray()
	for clue_id in GameState.clues.keys():
		if definitions.has(clue_id):
			out.append(String(clue_id))
	return out

func discovered_in_category(category: String) -> PackedStringArray:
	var out := PackedStringArray()
	for clue_id in discovered():
		if category_of(clue_id) == category:
			out.append(clue_id)
	return out

func categories() -> PackedStringArray:
	var seen := {}
	for clue_id in discovered():
		seen[category_of(clue_id)] = true
	var out := PackedStringArray()
	for k in seen.keys():
		out.append(String(k))
	out.sort()
	return out

## Puts two clues side by side. Returns the new clue id, or "" if the pair
## says nothing — which is never punished, only answered.
func combine(a: String, b: String) -> String:
	if a == b or not GameState.has_clue(a) or not GameState.has_clue(b):
		combination_rejected.emit(a, b)
		return ""
	var result := String(combinations.get(_pair_key(a, b), ""))
	if result.is_empty():
		combination_rejected.emit(a, b)
		AudioManager.play_sfx("SFX_UI_SOFT_NO")
		return ""
	if GameState.has_clue(result):
		return result
	GameState.add_clue(result)
	SaveManager.mark_dirty()
	combination_found.emit(result)
	AudioManager.play_sfx("SFX_CLUE_COMBINE")
	Log.info("Journal", "%s + %s -> %s" % [a, b, result])
	return result

## Unsolved threads shown in the journal, so a stuck player can see what is
## still open without being handed the answer (GDD §5, screen 3).
func open_questions() -> Array:
	var out: Array = []
	for clue_id in definitions.keys():
		var d: Dictionary = definitions[clue_id]
		if not bool(d.get("is_question", false)):
			continue
		var resolved_by := String(d.get("resolved_by", ""))
		if resolved_by.is_empty() or not GameState.has_clue(resolved_by):
			if _question_is_visible(d):
				out.append(d)
	return out

func _question_is_visible(d: Dictionary) -> bool:
	var needs := String(d.get("visible_after", ""))
	return needs.is_empty() or GameState.has_clue(needs)

func _pair_key(a: String, b: String) -> String:
	# Order-independent: combining is a question, not a sequence.
	return "%s|%s" % [a, b] if a < b else "%s|%s" % [b, a]

func _on_clue_discovered(clue_id: String) -> void:
	clue_added.emit(clue_id)
