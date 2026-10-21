extends Node
## Save, load and migrate player progress (GDD §9).
##
## Three rules this file exists to keep:
##   * a save is written atomically — a crash mid-write must not destroy the
##     previous good save;
##   * an older save is migrated forward, never discarded;
##   * nothing here needs the network. Progress is local first.

signal saved()
signal loaded()
signal save_failed(reason: String)

const SAVE_PATH := "user://save_01.json"
const BACKUP_PATH := "user://save_01.bak.json"
const TMP_PATH := "user://save_01.tmp.json"
const AUTOSAVE_SECONDS := 20.0

var _dirty: bool = false
var _timer: float = 0.0
var last_error: String = ""

func _ready() -> void:
	set_process(true)

func _process(delta: float) -> void:
	if not _dirty:
		return
	_timer += delta
	if _timer >= AUTOSAVE_SECONDS:
		save()

func mark_dirty() -> void:
	_dirty = true

func has_save() -> bool:
	return FileAccess.file_exists(SAVE_PATH) or FileAccess.file_exists(BACKUP_PATH)

func save() -> bool:
	_timer = 0.0
	var payload := {
		"schema": GameState.SAVE_SCHEMA_VERSION,
		"saved_at": Time.get_unix_time_from_system(),
		"game_version": ProjectSettings.get_setting("application/config/version", "0"),
		"state": GameState.to_dict(),
	}
	var text := JSON.stringify(payload, "  ")

	# Write to a temp file, verify it reads back, then swap. An interrupted
	# write leaves the previous save untouched.
	var f := FileAccess.open(TMP_PATH, FileAccess.WRITE)
	if f == null:
		return _fail("could not open temp save (%d)" % FileAccess.get_open_error())
	f.store_string(text)
	f.close()

	var verify := FileAccess.open(TMP_PATH, FileAccess.READ)
	if verify == null:
		return _fail("temp save unreadable")
	var check = JSON.parse_string(verify.get_as_text())
	verify.close()
	if typeof(check) != TYPE_DICTIONARY:
		return _fail("temp save did not parse back")

	if FileAccess.file_exists(SAVE_PATH):
		DirAccess.remove_absolute(BACKUP_PATH)
		DirAccess.rename_absolute(SAVE_PATH, BACKUP_PATH)
	var err := DirAccess.rename_absolute(TMP_PATH, SAVE_PATH)
	if err != OK:
		return _fail("could not move temp save into place (%d)" % err)

	_dirty = false
	last_error = ""
	saved.emit()
	Log.info("Save", "saved at %s / %s" % [GameState.current_chapter, GameState.current_scene])
	return true

func load_game() -> bool:
	if _try_load(SAVE_PATH):
		return true
	Log.warn("Save", "primary save unusable; trying backup")
	if _try_load(BACKUP_PATH):
		return true
	return false

func _try_load(path: String) -> bool:
	if not FileAccess.file_exists(path):
		return false
	var f := FileAccess.open(path, FileAccess.READ)
	if f == null:
		return false
	var raw := f.get_as_text()
	f.close()
	var parsed = JSON.parse_string(raw)
	if typeof(parsed) != TYPE_DICTIONARY:
		Log.error("Save", "%s is not valid JSON" % path)
		return false
	var d: Dictionary = parsed
	var schema := int(d.get("schema", 0))
	var state = d.get("state", {})
	if typeof(state) != TYPE_DICTIONARY:
		Log.error("Save", "%s has no state block" % path)
		return false
	state = _migrate(state, schema)
	GameState.from_dict(state)
	_dirty = false
	loaded.emit()
	Log.info("Save", "loaded schema %d from %s" % [schema, path])
	return true

func _migrate(state: Dictionary, from_schema: int) -> Dictionary:
	## Each step moves the dictionary forward exactly one version. Adding a
	## version means adding one branch here and bumping SAVE_SCHEMA_VERSION.
	var s := state.duplicate(true)
	var v := from_schema
	if v < 1:
		# Pre-versioned saves: trust used to be a single integer.
		if typeof(s.get("trust")) != TYPE_DICTIONARY:
			var old := int(s.get("trust", 0))
			s["trust"] = {"raad": old, "raha": 0, "mani": 0, "fandogh": 0}
		v = 1
	if v > GameState.SAVE_SCHEMA_VERSION:
		Log.warn("Save", "save is newer (schema %d) than this build (%d)" % [v, GameState.SAVE_SCHEMA_VERSION])
	return s

func delete_save() -> void:
	for p in [SAVE_PATH, BACKUP_PATH, TMP_PATH]:
		if FileAccess.file_exists(p):
			DirAccess.remove_absolute(p)
	GameState.reset()
	Log.info("Save", "save deleted")

func _fail(reason: String) -> bool:
	last_error = reason
	Log.error("Save", reason)
	save_failed.emit(reason)
	return false
