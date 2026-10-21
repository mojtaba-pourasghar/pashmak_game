extends RazinoScreen
## UI-09 — معما, as a screen in its own right.
##
## As with dialogue, puzzles normally run as a panel inside UI-06. This entry
## lets a puzzle be opened on its own — for a content preview, or to re-try one
## from the journal — and shares the same PuzzlePanel implementation.

var _panel: PuzzlePanel

func _ready() -> void:
	configure("معما", "UI-04")
	super._ready()

func build_content() -> void:
	var payload := Router.take_payload()
	var scroll := make_scroll()
	_panel = PuzzlePanel.new()
	_panel.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	scroll.add_child(_panel)

	var puzzle: Dictionary = payload.get("puzzle", {})
	if puzzle.is_empty():
		var chapter_id := String(payload.get("chapter_id", GameState.current_chapter))
		var scene_id := String(payload.get("scene_id", ""))
		if not chapter_id.is_empty() and not scene_id.is_empty():
			var story := ContentManager.load_chapter_story(chapter_id)
			for s in story.get("scenes", []):
				if String((s as Dictionary).get("scene_id", "")) == scene_id:
					puzzle = (s as Dictionary).get("puzzle", {})
					break
	if puzzle.is_empty():
		scroll.add_child(make_notice("معمایی برای نمایش انتخاب نشده است.", Palette.BRASS))
		return

	_panel.load_puzzle(puzzle)
	_panel.solved.connect(func(): Router.go(String(payload.get("return_to", "UI-04"))))
	_panel.skipped.connect(func(): Router.go(String(payload.get("return_to", "UI-04"))))
