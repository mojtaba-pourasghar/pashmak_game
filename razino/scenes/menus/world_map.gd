extends RazinoScreen
## UI-05 — نقشهٔ جهان.
##
## GDD §3 asks for one world rather than ten separate scenes: places the
## player has already been stay open, and each chapter opens a new part of the
## same map. The regions below are the five from the GDD's world map.

const REGIONS := [
	{"id": "centre", "name": "منطقهٔ مرکزی شهر", "desc": "مدرسه، میدان ساعت، کتابخانه و خانهٔ شخصیت‌ها. نقطهٔ شروع و بازگشت.", "opens_with": "season_01"},
	{"id": "nature", "name": "منطقهٔ طبیعت", "desc": "جنگل نشانه‌ها، آبشار، رودخانه و مسیرهای کوهستانی.", "opens_with": "season_03"},
	{"id": "invent", "name": "منطقهٔ اختراع‌ها", "desc": "کارگاه مانی، آزمایشگاه علمی و انبار قطعه‌ها.", "opens_with": "season_04"},
	{"id": "far", "name": "منطقهٔ دوردست", "desc": "رصدخانه، دریاچه و ایستگاه پژوهشی.", "opens_with": "season_05"},
	{"id": "under", "name": "منطقهٔ زیرزمینی", "desc": "مسیرهای مخفی، تالار نمادها و دستگاه مرکزی.", "opens_with": "season_08"},
]

func _ready() -> void:
	configure("نقشهٔ جهان", "UI-04")
	super._ready()

func build_content() -> void:
	var art := PlaceholderArt.new()
	art.background_id = "BG_WORLD_MAP"
	art.custom_minimum_size = Vector2(0, 200)
	content.add_child(art)

	var list := make_scroll()
	for region in REGIONS:
		list.add_child(_region_card(region))

func _region_card(region: Dictionary) -> Control:
	var needs := String(region["opens_with"])
	# A region opens once its chapter has been reached, not only finished, so
	# the map grows with the story rather than lagging behind it.
	var open := GameState.is_chapter_unlocked(needs)

	var card := Style.make_night_card()
	var row := HBoxContainer.new()
	row.add_theme_constant_override("separation", Palette.PAD_MD)
	card.add_child(row)

	var swatch := ColorRect.new()
	swatch.custom_minimum_size = Vector2(90, 90)
	swatch.color = Palette.TEAL if open else Palette.LOCKED
	row.add_child(swatch)

	var col := VBoxContainer.new()
	col.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	row.add_child(col)
	col.add_child(Style.make_title(String(region["name"]), Palette.FS_HEADING,
		Palette.SNOW if open else Palette.SNOW_SOFT))
	col.add_child(Style.make_body(String(region["desc"]), Palette.FS_CAPTION, Palette.SNOW_SOFT))
	if not open:
		var entry := ContentManager.catalog_entry(needs)
		col.add_child(Style.make_body(
			"با رسیدن به «%s» باز می‌شود." % String(entry.get("title", needs)),
			Palette.FS_CAPTION, Palette.LOCKED))
	return card
