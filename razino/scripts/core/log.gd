extends Node
## Leveled logger. Writes to stdout and to a rotating file under user://logs/
## so that failures on a real device can be inspected after the fact.
##
## Nothing written here may contain personal data: no photo paths, no child
## name, no purchase receipts. See docs/PRIVACY.md.

enum Level { DEBUG, INFO, WARN, ERROR }

const LOG_DIR := "user://logs"
const MAX_LOG_BYTES := 256 * 1024

var min_level: Level = Level.DEBUG
var _file: FileAccess = null

func _ready() -> void:
	DirAccess.make_dir_recursive_absolute(LOG_DIR)
	_rotate_if_needed()
	_file = FileAccess.open(_log_path(), FileAccess.READ_WRITE)
	if _file == null:
		_file = FileAccess.open(_log_path(), FileAccess.WRITE)
	if _file != null:
		_file.seek_end()
	info("Log", "session started — Razino %s / Godot %s" % [
		ProjectSettings.get_setting("application/config/version", "?"),
		Engine.get_version_info().get("string", "?"),
	])

func _log_path() -> String:
	return LOG_DIR + "/razino.log"

func _rotate_if_needed() -> void:
	if not FileAccess.file_exists(_log_path()):
		return
	var f := FileAccess.open(_log_path(), FileAccess.READ)
	if f == null:
		return
	var size := f.get_length()
	f.close()
	if size > MAX_LOG_BYTES:
		DirAccess.rename_absolute(_log_path(), LOG_DIR + "/razino.prev.log")

func _write(level: Level, tag: String, message: String) -> void:
	if level < min_level:
		return
	var names := ["DEBUG", "INFO", "WARN", "ERROR"]
	var line := "[%s] %-5s %-18s %s" % [
		Time.get_datetime_string_from_system(false, true),
		names[level], tag, message,
	]
	print(line)
	if _file != null:
		_file.store_line(line)
		_file.flush()

func debug(tag: String, message: String) -> void: _write(Level.DEBUG, tag, message)
func info(tag: String, message: String) -> void: _write(Level.INFO, tag, message)
func warn(tag: String, message: String) -> void: _write(Level.WARN, tag, message)
func error(tag: String, message: String) -> void: _write(Level.ERROR, tag, message)

func read_tail(max_lines: int = 200) -> PackedStringArray:
	## Used by the parent section's diagnostics view (UI-16).
	var out := PackedStringArray()
	if not FileAccess.file_exists(_log_path()):
		return out
	var f := FileAccess.open(_log_path(), FileAccess.READ)
	if f == null:
		return out
	var all := f.get_as_text().split("\n", false)
	f.close()
	var start := maxi(0, all.size() - max_lines)
	for i in range(start, all.size()):
		out.append(all[i])
	return out
