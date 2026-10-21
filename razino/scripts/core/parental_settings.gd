extends Node
## بخش والدین (UI-14) — GDD §14.
##
## Two things live here and nowhere else:
##   1. the gate that stands in front of every sensitive action, and
##   2. the consent flags that the avatar and purchase systems must check.
##
## The gate is deliberately NOT a single arithmetic question behind a button —
## GDD §14 calls that insufficient. It asks for a four-digit PIN that the adult
## sets once; if no PIN is set yet, setting one is itself gated behind a
## multi-step prompt that a young child is unlikely to complete by accident.

signal gate_passed()
signal gate_failed()
signal consent_changed(key: String)

const PATH := "user://parental.cfg"
const LOCKOUT_SECONDS := 30.0
const MAX_ATTEMPTS := 5

var _pin_hash: String = ""
var _salt: String = ""
var _failed_attempts: int = 0
var _locked_until_ms: int = 0

## Consent flags. Every one of these defaults to false: nothing involving a
## camera, a photo leaving the device, or a purchase happens without an adult
## turning it on first.
var allow_purchases: bool = false
var allow_camera_avatar: bool = false
var allow_server_photo_processing: bool = false
var allow_content_download_on_cellular: bool = false

func _ready() -> void:
	_load()

func has_pin() -> bool:
	return _pin_hash != ""

func is_locked_out() -> bool:
	return Time.get_ticks_msec() < _locked_until_ms

func seconds_until_unlock() -> float:
	return maxf(0.0, (_locked_until_ms - Time.get_ticks_msec()) / 1000.0)

func set_pin(pin: String) -> bool:
	if not _is_valid_pin(pin):
		return false
	_salt = _make_salt()
	_pin_hash = _hash(pin, _salt)
	_failed_attempts = 0
	_save()
	Log.info("Parental", "PIN set")
	return true

func change_pin(old_pin: String, new_pin: String) -> bool:
	if has_pin() and not _matches(old_pin):
		return false
	return set_pin(new_pin)

func verify(pin: String) -> bool:
	if is_locked_out():
		gate_failed.emit()
		return false
	if _matches(pin):
		_failed_attempts = 0
		gate_passed.emit()
		return true
	_failed_attempts += 1
	if _failed_attempts >= MAX_ATTEMPTS:
		_locked_until_ms = Time.get_ticks_msec() + int(LOCKOUT_SECONDS * 1000.0)
		_failed_attempts = 0
		Log.warn("Parental", "gate locked out after repeated failures")
	gate_failed.emit()
	return false

func set_consent(key: String, value: bool) -> void:
	match key:
		"allow_purchases": allow_purchases = value
		"allow_camera_avatar": allow_camera_avatar = value
		"allow_server_photo_processing": allow_server_photo_processing = value
		"allow_content_download_on_cellular": allow_content_download_on_cellular = value
		_:
			Log.warn("Parental", "unknown consent key '%s'" % key)
			return
	# Revoking camera consent must also revoke the narrower server consent.
	if key == "allow_camera_avatar" and not value:
		allow_server_photo_processing = false
	_save()
	consent_changed.emit(key)
	Log.info("Parental", "consent '%s' = %s" % [key, str(value)])

func _is_valid_pin(pin: String) -> bool:
	if pin.length() != 4:
		return false
	for c in pin:
		if not (c >= "0" and c <= "9"):
			return false
	return true

func _matches(pin: String) -> bool:
	if not has_pin():
		return false
	return _hash(pin, _salt) == _pin_hash

func _make_salt() -> String:
	var bytes := PackedByteArray()
	for i in 16:
		bytes.append(randi() % 256)
	return Marshalls.raw_to_base64(bytes)

func _hash(pin: String, salt: String) -> String:
	# Not a password store for a server — it guards a local settings screen.
	# Salted SHA-256 keeps the PIN out of the config file in plain text.
	var ctx := HashingContext.new()
	ctx.start(HashingContext.HASH_SHA256)
	ctx.update((salt + ":" + pin).to_utf8_buffer())
	return Marshalls.raw_to_base64(ctx.finish())

func _load() -> void:
	var cfg := ConfigFile.new()
	if cfg.load(PATH) != OK:
		return
	_pin_hash = cfg.get_value("gate", "hash", "")
	_salt = cfg.get_value("gate", "salt", "")
	allow_purchases = cfg.get_value("consent", "purchases", false)
	allow_camera_avatar = cfg.get_value("consent", "camera_avatar", false)
	allow_server_photo_processing = cfg.get_value("consent", "server_photo", false)
	allow_content_download_on_cellular = cfg.get_value("consent", "cellular", false)

func _save() -> void:
	var cfg := ConfigFile.new()
	cfg.set_value("gate", "hash", _pin_hash)
	cfg.set_value("gate", "salt", _salt)
	cfg.set_value("consent", "purchases", allow_purchases)
	cfg.set_value("consent", "camera_avatar", allow_camera_avatar)
	cfg.set_value("consent", "server_photo", allow_server_photo_processing)
	cfg.set_value("consent", "cellular", allow_content_download_on_cellular)
	cfg.save(PATH)
