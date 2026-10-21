class_name ContentEndpoints
extends RefCounted
## Where the game looks for its backend, and how it decides what it may trust.
##
## No URL, key or token is hard-coded. Configuration is read from, in order:
##   1. user://endpoints.cfg  — written by the parent/dev settings screen
##   2. res://resources/shared_assets/endpoints.default.cfg — shipped defaults
##
## Secrets never live in either: the game only ever holds PUBLIC endpoints and
## a PUBLIC signing key. See docs/BACKEND.md and env.example.

const USER_CONFIG := "user://endpoints.cfg"
const BUNDLED_CONFIG := "res://resources/shared_assets/endpoints.default.cfg"

static func _cfg() -> ConfigFile:
	var cfg := ConfigFile.new()
	if cfg.load(USER_CONFIG) == OK:
		return cfg
	var bundled := ConfigFile.new()
	bundled.load(BUNDLED_CONFIG)
	return bundled

static func content_base_url() -> String:
	return String(_cfg().get_value("content", "base_url", ""))

static func api_base_url() -> String:
	return String(_cfg().get_value("api", "base_url", ""))

static func is_configured() -> bool:
	return not content_base_url().is_empty()

## True only for a loopback / LAN development server. A production host may
## never serve unsigned content.
static func allow_unsigned_content() -> bool:
	if not bool(_cfg().get_value("content", "allow_unsigned", false)):
		return false
	var url := content_base_url()
	return url.begins_with("http://127.0.0.1") \
		or url.begins_with("http://localhost") \
		or url.begins_with("file://")
