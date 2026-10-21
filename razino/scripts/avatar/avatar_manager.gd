extends Node
## ساخت آواتار (UI-03 / UI-12) — GDD §10.
##
## Privacy is the design, not a setting bolted on afterwards:
##   * The whole game is playable with no camera and no photo. Photo use is
##     an extra, never a step on the way in.
##   * The original photo is NEVER stored by default and NEVER uploaded by
##     default. Server processing needs an explicit, separate adult consent.
##   * Any temporary image file is deleted immediately after processing, and
##     `temp_file_exists()` makes that deletion checkable by a test.
##   * There is NO face recognition, NO identity matching, NO biometric
##     template, and nothing is used to train anything.
##
## What the optional photo path actually does: estimates a few coarse colour
## hints (overall skin/hair/clothing tone) to pre-select among hand-drawn
## cartoon parts. It cannot identify anyone, and its output is just a handful
## of part indices.

signal avatar_changed(avatar_id: String)
signal photo_processing_failed(reason: String)

const TEMP_PHOTO := "user://avatar_tmp_capture.png"
const AVATAR_PATH := "user://avatar.json"

## Hand-drawn part sets. Indices into these are the entire avatar.
const SKIN_TONES := [
	Color("F2D6BE"), Color("E8C19C"), Color("D2A077"), Color("B2784F"),
	Color("8D5A38"), Color("5F3B24"),
]
const HAIR_COLORS := [
	Color("1C1A1A"), Color("3B2A1E"), Color("6B4327"), Color("A9703E"),
	Color("D9A45B"), Color("8A8FA3"),
]
const HAIR_STYLES := ["short", "wavy", "curly", "braid", "ponytail", "cap"]
const EYE_SHAPES := ["round", "almond", "wide", "soft"]
const OUTFITS := ["explorer", "scholar", "inventor", "festival", "night"]
const ACCESSORIES := ["none", "glasses", "scarf", "satchel", "pocket_watch"]

var config: Dictionary = {
	"skin": 1, "hair_color": 1, "hair_style": 0,
	"eyes": 0, "outfit": 0, "accessory": 0,
	"from_photo": false,
}

func _ready() -> void:
	load_avatar()
	# A temp capture left behind by a crash is cleaned up at startup.
	_delete_temp_photo()

func avatar_id() -> String:
	return "AV-%d%d%d%d%d%d" % [
		config.get("skin", 0), config.get("hair_color", 0), config.get("hair_style", 0),
		config.get("eyes", 0), config.get("outfit", 0), config.get("accessory", 0),
	]

func set_part(part: String, index: int) -> void:
	var count := part_count(part)
	if count == 0:
		Log.warn("Avatar", "unknown part '%s'" % part)
		return
	config[part] = posmod(index, count)
	_commit()

func cycle_part(part: String, direction: int = 1) -> void:
	set_part(part, int(config.get(part, 0)) + direction)

func part_count(part: String) -> int:
	match part:
		"skin": return SKIN_TONES.size()
		"hair_color": return HAIR_COLORS.size()
		"hair_style": return HAIR_STYLES.size()
		"eyes": return EYE_SHAPES.size()
		"outfit": return OUTFITS.size()
		"accessory": return ACCESSORIES.size()
	return 0

func skin_color() -> Color:
	return SKIN_TONES[int(config.get("skin", 0)) % SKIN_TONES.size()]

func hair_color() -> Color:
	return HAIR_COLORS[int(config.get("hair_color", 0)) % HAIR_COLORS.size()]

func hair_style() -> String:
	return HAIR_STYLES[int(config.get("hair_style", 0)) % HAIR_STYLES.size()]

func eye_shape() -> String:
	return EYE_SHAPES[int(config.get("eyes", 0)) % EYE_SHAPES.size()]

func outfit() -> String:
	return OUTFITS[int(config.get("outfit", 0)) % OUTFITS.size()]

func accessory() -> String:
	return ACCESSORIES[int(config.get("accessory", 0)) % ACCESSORIES.size()]

func randomize_avatar() -> void:
	for part in ["skin", "hair_color", "hair_style", "eyes", "outfit", "accessory"]:
		config[part] = randi() % part_count(part)
	config["from_photo"] = false
	_commit()

# — optional photo assist ————————————————————————————————

## Derives colour hints from a locally-held image and then deletes it.
## Returns true if hints were applied. Requires adult consent; refuses
## otherwise. The image never leaves the device on this path.
func apply_photo_hints(image: Image) -> bool:
	if not ParentalSettings.allow_camera_avatar:
		photo_processing_failed.emit("استفاده از دوربین در بخش والدین فعال نشده است.")
		return false
	if image == null or image.get_width() < 32 or image.get_height() < 32:
		photo_processing_failed.emit("عکس خوانا نبود. می‌توانی بدون عکس هم شخصیت بسازی.")
		return false

	var hints := _estimate_tone_hints(image)
	if hints.is_empty():
		photo_processing_failed.emit("نور عکس مناسب نبود. دوباره امتحان کن یا بدون عکس ادامه بده.")
		return false

	config["skin"] = _nearest(SKIN_TONES, hints["skin"])
	config["hair_color"] = _nearest(HAIR_COLORS, hints["hair"])
	config["from_photo"] = true
	_commit()

	# Nothing about the photo is kept beyond these two indices.
	_delete_temp_photo()
	Log.info("Avatar", "photo hints applied; source image discarded")
	return true

## Coarse average tones from two bands of the image. No landmarks, no
## descriptors, nothing that could identify a person.
func _estimate_tone_hints(image: Image) -> Dictionary:
	var img := image.duplicate()
	img.resize(32, 32, Image.INTERPOLATE_BILINEAR)
	var skin := _average_band(img, 12, 22)   # middle band
	var hair := _average_band(img, 0, 8)     # top band
	# Reject frames that are essentially flat — a wall, a dark room, a finger
	# over the lens. Better to say "try again" than to invent a result.
	if absf(skin.v - hair.v) < 0.03 and skin.s < 0.05:
		return {}
	return {"skin": skin, "hair": hair}

func _average_band(img: Image, y0: int, y1: int) -> Color:
	var r := 0.0
	var g := 0.0
	var b := 0.0
	var n := 0
	for y in range(y0, mini(y1, img.get_height())):
		for x in img.get_width():
			var c := img.get_pixel(x, y)
			r += c.r; g += c.g; b += c.b
			n += 1
	if n == 0:
		return Color.BLACK
	return Color(r / n, g / n, b / n)

func _nearest(palette: Array, target: Color) -> int:
	var best := 0
	var best_d := INF
	for i in palette.size():
		var c: Color = palette[i]
		var d := pow(c.r - target.r, 2.0) + pow(c.g - target.g, 2.0) + pow(c.b - target.b, 2.0)
		if d < best_d:
			best_d = d
			best = i
	return best

func temp_photo_path() -> String:
	return TEMP_PHOTO

## Exposed so the acceptance test can prove the temp file is really gone.
func temp_file_exists() -> bool:
	return FileAccess.file_exists(TEMP_PHOTO)

func _delete_temp_photo() -> void:
	if FileAccess.file_exists(TEMP_PHOTO):
		DirAccess.remove_absolute(TEMP_PHOTO)

## "Forget the photo": drops the derived flag and re-randomises nothing else.
func forget_photo_derived_data() -> void:
	config["from_photo"] = false
	_delete_temp_photo()
	_commit()

func delete_avatar() -> void:
	config = {
		"skin": 1, "hair_color": 1, "hair_style": 0,
		"eyes": 0, "outfit": 0, "accessory": 0, "from_photo": false,
	}
	_delete_temp_photo()
	if FileAccess.file_exists(AVATAR_PATH):
		DirAccess.remove_absolute(AVATAR_PATH)
	GameState.avatar_id = ""
	avatar_changed.emit("")
	Log.info("Avatar", "avatar deleted")

# — persistence ——————————————————————————————————————————

func _commit() -> void:
	GameState.avatar_id = avatar_id()
	save_avatar()
	SaveManager.mark_dirty()
	avatar_changed.emit(GameState.avatar_id)

func save_avatar() -> void:
	var f := FileAccess.open(AVATAR_PATH, FileAccess.WRITE)
	if f == null:
		Log.error("Avatar", "could not save avatar")
		return
	f.store_string(JSON.stringify(config, "  "))
	f.close()

func load_avatar() -> void:
	if not FileAccess.file_exists(AVATAR_PATH):
		return
	var f := FileAccess.open(AVATAR_PATH, FileAccess.READ)
	if f == null:
		return
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	if typeof(parsed) == TYPE_DICTIONARY:
		config = parsed
		GameState.avatar_id = avatar_id()
