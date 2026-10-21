extends Node
## Player-adjustable settings (UI-13). Stored on its own, outside the save
## game, so that wiping a save never silently resets accessibility choices
## and so that settings survive a corrupt save file.

signal changed(key: String)

const PATH := "user://settings.cfg"

## Independent volumes, GDD §12.2: گوینده / موسیقی / افکت‌ها.
var voice_volume: float = 0.9
var music_volume: float = 0.55
var sfx_volume: float = 0.8

var subtitles_enabled: bool = true
## 0.85 .. 1.4 — bumped for younger readers.
var text_scale: float = 1.0
## Honours the platform "reduce motion" intent; also disables parallax drift.
var reduce_motion: bool = false
## Auto-advance dialogue once the recorded line has finished.
var auto_advance: bool = false
var locale: String = "fa"

func _ready() -> void:
	load_from_disk()

func load_from_disk() -> void:
	var cfg := ConfigFile.new()
	if cfg.load(PATH) != OK:
		Log.info("Settings", "no settings file yet; using defaults")
		return
	voice_volume = clampf(cfg.get_value("audio", "voice", voice_volume), 0.0, 1.0)
	music_volume = clampf(cfg.get_value("audio", "music", music_volume), 0.0, 1.0)
	sfx_volume = clampf(cfg.get_value("audio", "sfx", sfx_volume), 0.0, 1.0)
	subtitles_enabled = cfg.get_value("a11y", "subtitles", subtitles_enabled)
	text_scale = clampf(cfg.get_value("a11y", "text_scale", text_scale), 0.85, 1.4)
	reduce_motion = cfg.get_value("a11y", "reduce_motion", reduce_motion)
	auto_advance = cfg.get_value("a11y", "auto_advance", auto_advance)
	locale = cfg.get_value("general", "locale", locale)
	Log.info("Settings", "loaded")

func save_to_disk() -> void:
	var cfg := ConfigFile.new()
	cfg.set_value("audio", "voice", voice_volume)
	cfg.set_value("audio", "music", music_volume)
	cfg.set_value("audio", "sfx", sfx_volume)
	cfg.set_value("a11y", "subtitles", subtitles_enabled)
	cfg.set_value("a11y", "text_scale", text_scale)
	cfg.set_value("a11y", "reduce_motion", reduce_motion)
	cfg.set_value("a11y", "auto_advance", auto_advance)
	cfg.set_value("general", "locale", locale)
	var err := cfg.save(PATH)
	if err != OK:
		Log.error("Settings", "could not save settings: %d" % err)

func set_value(key: String, value: Variant) -> void:
	match key:
		"voice_volume": voice_volume = clampf(value, 0.0, 1.0)
		"music_volume": music_volume = clampf(value, 0.0, 1.0)
		"sfx_volume": sfx_volume = clampf(value, 0.0, 1.0)
		"subtitles_enabled": subtitles_enabled = bool(value)
		"text_scale": text_scale = clampf(value, 0.85, 1.4)
		"reduce_motion": reduce_motion = bool(value)
		"auto_advance": auto_advance = bool(value)
		_:
			Log.warn("Settings", "unknown setting '%s'" % key)
			return
	save_to_disk()
	changed.emit(key)
