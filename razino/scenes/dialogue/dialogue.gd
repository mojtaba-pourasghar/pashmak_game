extends RazinoScreen
## UI-07 — دیالوگ و انتخاب, as a screen in its own right.
##
## In play, dialogue is a panel inside UI-06 so the picture never leaves the
## screen. This standalone entry exists for the cases where a conversation is
## the whole screen — a preview from the content tool, or a jump straight to a
## conversation from a link — and uses the same DialoguePanel, so there is one
## implementation of Persian dialogue, not two.

var _panel: DialoguePanel

func _ready() -> void:
	configure("گفت‌وگو", "UI-04")
	super._ready()

func build_content() -> void:
	var payload := Router.take_payload()
	var stage := SceneStage.new()
	stage.custom_minimum_size = Vector2(0, 320)
	stage.size_flags_vertical = Control.SIZE_EXPAND_FILL
	content.add_child(stage)

	_panel = DialoguePanel.new()
	content.add_child(_panel)

	var scene_id := String(payload.get("scene_id", ""))
	var chapter_id := String(payload.get("chapter_id", GameState.current_chapter))
	if chapter_id.is_empty() or scene_id.is_empty():
		_panel.show_line({"text": "گفت‌وگویی برای نمایش انتخاب نشده است."}, "")
		return

	if not StoryEngine.load_chapter(chapter_id):
		_panel.show_line({"text": "محتوای این فصل در دسترس نیست."}, "")
		return
	StoryEngine.line_shown.connect(func(line, _i, _t):
		stage.highlight_speaker(String(line.get("speaker_id", "")))
		_panel.show_line(line, String(line.get("speaker_id", ""))))
	StoryEngine.scene_entered.connect(func(scene): stage.show_scene(scene))
	_panel.continue_pressed.connect(func(): StoryEngine.advance())
	_panel.choice_selected.connect(func(cid): StoryEngine.choose(cid))
	StoryEngine.enter_scene(scene_id)
