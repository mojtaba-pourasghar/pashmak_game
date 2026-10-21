extends RazinoTest
## Persian rendering (GDD §7.2, QA checklist "نمایش صحیح فارسی و راست‌به‌چپ").
##
## These check the things that actually break Persian in a game engine:
## the font is really there, the glyphs exist, the text shapes (joins) rather
## than falling back to tofu boxes, and the layout runs right to left.

const SAMPLE := "راز ساعت خاموش"
const JOINING := "سلام"

func test_rtl_is_forced_project_wide():
	ok(bool(ProjectSettings.get_setting("internationalization/rendering/force_right_to_left_layout_direction", false)),
		"the project forces RTL layout")
	eq(int(ProjectSettings.get_setting("internationalization/rendering/root_node_layout_direction", 0)), 2,
		"the root layout direction is set to RTL")

func test_persian_font_is_bundled_and_loads():
	ok(Style.font_regular != null, "the regular Persian face loaded")
	ok(Style.font_bold != null, "the bold face loaded")
	ok(FileAccess.file_exists("res://assets/fonts/Vazirmatn-OFL.txt"),
		"the font's licence ships with it")

func test_font_has_persian_glyphs():
	var f := Style.font_regular
	# Every letter of the sample must exist in the face: a missing glyph is
	# what produces the empty boxes you see in badly-set Persian.
	for ch in SAMPLE:
		if ch == " ":
			continue
		var size := f.get_char_size(ch.unicode_at(0), 32)
		gt(size.x, 0.0, "glyph exists for '%s'" % ch)

func test_persian_text_shapes_and_joins():
	## Persian letters join. If shaping is off, the rendered width equals the
	## sum of the isolated forms; with shaping it is narrower.
	var f := Style.font_regular
	var shaped := f.get_string_size(JOINING, HORIZONTAL_ALIGNMENT_RIGHT, -1, 32)
	gt(shaped.x, 0.0, "the shaped string has width")

	var isolated := 0.0
	for ch in JOINING:
		isolated += f.get_char_size(ch.unicode_at(0), 32).x
	ok(shaped.x < isolated,
		"joined text (%.1f) is narrower than isolated letters (%.1f) — shaping is working" % [shaped.x, isolated])

func test_text_server_supports_the_arabic_script():
	var ts := TextServerManager.get_primary_interface()
	ok(ts.has_feature(TextServer.FEATURE_SHAPING), "the text server can shape complex scripts")
	ok(ts.has_feature(TextServer.FEATURE_BIDI_LAYOUT), "and can lay out bidirectional text")

func test_a_label_lays_out_right_to_left():
	var label := Style.make_title(SAMPLE)
	eq(label.horizontal_alignment, HORIZONTAL_ALIGNMENT_RIGHT, "headings align right")

func test_buttons_meet_the_touch_target_minimum():
	var b := Style.make_button("ادامهٔ ماجرا")
	gt(b.custom_minimum_size.y, 47.0,
		"a button is at least the GDD's ~48-unit touch target (got %.0f)" % b.custom_minimum_size.y)

func test_text_scale_changes_the_theme():
	var before := Style.scaled(Palette.FS_BODY)
	Settings.set_value("text_scale", 1.4)
	var after := Style.scaled(Palette.FS_BODY)
	gt(float(after), float(before), "raising the text size really enlarges body text")
	Settings.set_value("text_scale", 1.0)

func test_all_shipped_dialogue_is_non_empty_persian():
	## A blank subtitle is a bug the player sees. Check every line in every
	## chapter, not a sample.
	var checked := 0
	for cid in ContentManager.chapter_ids():
		var story := ContentManager.load_chapter_story(cid)
		for s in story.get("scenes", []):
			for line in (s as Dictionary).get("dialogue", []):
				var text := String((line as Dictionary).get("text", ""))
				checked += 1
				not_empty(text.strip_edges(), "%s/%s has subtitle text" % [cid, (line as Dictionary).get("line_id", "?")])
				ok(_has_persian(text), "%s is written in Persian" % (line as Dictionary).get("line_id", "?"))
	gt(float(checked), 200.0, "checked every shipped line (%d)" % checked)

func _has_persian(s: String) -> bool:
	for ch in s:
		var c := ch.unicode_at(0)
		# Arabic block, plus the Persian-specific letters.
		if (c >= 0x0600 and c <= 0x06FF) or (c >= 0xFB50 and c <= 0xFDFF):
			return true
	return false
