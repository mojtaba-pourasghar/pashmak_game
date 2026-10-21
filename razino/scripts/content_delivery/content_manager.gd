extends Node
## Chapter catalogue, download, validation and activation (GDD §7, §9).
##
## The contract this file enforces, in order:
##   1. A chapter is a versioned DATA package. It never contains executable
##      code, and nothing downloaded is ever `load()`-ed as a script or scene.
##   2. A download lands in a temporary directory. It becomes the live chapter
##      only after its manifest, file list, sizes, SHA-256 hashes and signature
##      all check out — and then by one atomic directory swap.
##   3. If anything fails, the previously installed version stays live and
##      playable. A half-downloaded chapter is never run.
##   4. Once installed, a chapter plays with no network at all.

signal catalog_updated()
signal download_progress(chapter_id: String, ratio: float)
signal download_completed(chapter_id: String)
signal download_failed(chapter_id: String, reason: String)
signal content_activated(chapter_id: String, version: int)

const BUNDLED_ROOT := "res://content"
const INSTALL_ROOT := "user://content"
const STAGING_ROOT := "user://content_staging"
const INSTALLED_INDEX := "user://content/installed.json"
const PUBLIC_KEY_PATH := "res://resources/shared_assets/content_signing_public.key"

## The engine feature level this build can run. A package demanding more is
## refused with a clear message rather than loaded half-understood.
const SUPPORTED_SCHEMA_VERSION := 1
const GAME_VERSION := "0.1.0"

var catalog: Array = []
var installed: Dictionary = {}      # chapter_id -> {"version": int, "path": String}
var last_error: String = ""

var _http: HTTPRequest
var _public_key: CryptoKey = null
var _crypto := Crypto.new()
var _active_download: String = ""

func _ready() -> void:
	DirAccess.make_dir_recursive_absolute(INSTALL_ROOT)
	DirAccess.make_dir_recursive_absolute(STAGING_ROOT)
	_http = HTTPRequest.new()
	_http.use_threads = true
	add_child(_http)
	_load_public_key()
	_load_installed_index()
	load_local_catalog()

func _load_public_key() -> void:
	if not FileAccess.file_exists(PUBLIC_KEY_PATH):
		Log.warn("Content", "no signing public key bundled; signed packages cannot be verified")
		return
	var key := CryptoKey.new()
	# `true` = public-only key. The private half never ships with the game.
	if key.load(PUBLIC_KEY_PATH, true) == OK:
		_public_key = key
		Log.info("Content", "content signing key loaded")
	else:
		Log.error("Content", "signing public key failed to load")

# — catalogue ————————————————————————————————————————————

func load_local_catalog() -> void:
	var path := BUNDLED_ROOT + "/manifest/catalog.json"
	var d := _read_json(path)
	if d.is_empty():
		Log.error("Content", "bundled catalog missing at %s" % path)
		return
	catalog = d.get("chapters", [])
	catalog_updated.emit()
	Log.info("Content", "catalog: %d chapters" % catalog.size())

func catalog_entry(chapter_id: String) -> Dictionary:
	for c in catalog:
		if String((c as Dictionary).get("chapter_id", "")) == chapter_id:
			return c
	return {}

func chapter_ids() -> PackedStringArray:
	var out := PackedStringArray()
	for c in catalog:
		out.append(String((c as Dictionary).get("chapter_id", "")))
	return out

# — availability —————————————————————————————————————————

## Bundled chapters ship inside the APK; installed ones were downloaded.
func is_bundled(chapter_id: String) -> bool:
	return FileAccess.file_exists("%s/%s/story.json" % [BUNDLED_ROOT, chapter_id])

func is_installed(chapter_id: String) -> bool:
	return installed.has(chapter_id) or is_bundled(chapter_id)

func installed_version(chapter_id: String) -> int:
	if installed.has(chapter_id):
		return int((installed[chapter_id] as Dictionary).get("version", 0))
	if is_bundled(chapter_id):
		return int(catalog_entry(chapter_id).get("version", 1))
	return 0

func needs_update(chapter_id: String) -> bool:
	var entry := catalog_entry(chapter_id)
	if entry.is_empty():
		return false
	return int(entry.get("version", 1)) > installed_version(chapter_id)

## Where a chapter's files actually live right now.
func chapter_root(chapter_id: String) -> String:
	if installed.has(chapter_id):
		return String((installed[chapter_id] as Dictionary).get("path", ""))
	if is_bundled(chapter_id):
		return "%s/%s" % [BUNDLED_ROOT, chapter_id]
	return ""

func load_chapter_story(chapter_id: String) -> Dictionary:
	var root := chapter_root(chapter_id)
	if root.is_empty():
		last_error = "chapter %s is not installed" % chapter_id
		return {}
	return _read_json(root + "/story.json")

## AudioManager asks here when a res:// lookup fails, so downloaded chapters
## can supply their own clips.
func resolve_external_audio(res_path: String) -> String:
	var tail := res_path.replace("res://assets/audio/", "")
	for chapter_id in installed.keys():
		var candidate := "%s/audio/%s" % [chapter_root(chapter_id), tail]
		if FileAccess.file_exists(candidate):
			return candidate
	return ""

# — download —————————————————————————————————————————————

## Fetches, validates and activates a chapter. Returns true on success.
## Safe to call with no network: it fails cleanly and leaves state untouched.
func download_chapter(chapter_id: String, base_url: String = "") -> bool:
	if _active_download != "":
		download_failed.emit(chapter_id, "یک دانلود دیگر در حال انجام است.")
		return false
	var entry := catalog_entry(chapter_id)
	if entry.is_empty():
		return _download_fail(chapter_id, "این فصل در فهرست محتوا نیست.")

	# Access check happens before a single byte moves.
	if String(entry.get("access_type", "free")) != "free" \
			and not PurchaseManager.has_entitlement(chapter_id):
		return _download_fail(chapter_id, "برای دریافت این فصل، ابتدا باید آن را تهیه کنید.")

	var url_root := base_url
	if url_root.is_empty():
		url_root = ContentEndpoints.content_base_url()
	if url_root.is_empty():
		return _download_fail(chapter_id, "آدرس سرور محتوا تنظیم نشده است.")

	_active_download = chapter_id
	var staging := "%s/%s" % [STAGING_ROOT, chapter_id]
	_wipe_dir(staging)
	DirAccess.make_dir_recursive_absolute(staging)

	# 1. manifest
	var manifest_url := "%s/%s/manifest.json" % [url_root, chapter_id]
	var manifest_bytes := await _fetch(manifest_url)
	if manifest_bytes.is_empty():
		return _download_fail(chapter_id, "دریافت فهرست فایل‌ها ناموفق بود.")
	var manifest = JSON.parse_string(manifest_bytes.get_string_from_utf8())
	if typeof(manifest) != TYPE_DICTIONARY:
		return _download_fail(chapter_id, "فهرست فایل‌ها خوانا نیست.")

	var guard := validate_manifest(manifest, chapter_id)
	if guard != "":
		return _download_fail(chapter_id, guard)

	# 2. files, each hash-checked as it lands
	var files: Array = (manifest as Dictionary).get("files", [])
	var done := 0
	for f in files:
		var fd: Dictionary = f
		var rel := String(fd.get("path", ""))
		if not _is_safe_relative_path(rel):
			return _download_fail(chapter_id, "مسیر فایل نامعتبر است: %s" % rel)
		var bytes := await _fetch("%s/%s/%s" % [url_root, chapter_id, rel])
		if bytes.is_empty():
			return _download_fail(chapter_id, "دریافت %s ناقص ماند." % rel)
		if bytes.size() != int(fd.get("size", -1)):
			return _download_fail(chapter_id, "اندازهٔ %s با فهرست نمی‌خواند." % rel)
		if _sha256_hex(bytes) != String(fd.get("sha256", "")).to_lower():
			return _download_fail(chapter_id, "درستی فایل %s تأیید نشد." % rel)
		var target := staging + "/" + rel
		DirAccess.make_dir_recursive_absolute(target.get_base_dir())
		var out := FileAccess.open(target, FileAccess.WRITE)
		if out == null:
			return _download_fail(chapter_id, "فضای ذخیره‌سازی کافی نیست.")
		out.store_buffer(bytes)
		out.close()
		done += 1
		download_progress.emit(chapter_id, float(done) / float(maxi(files.size(), 1)))

	# 3. signature over the manifest itself, which in turn pins every file hash
	if not verify_manifest_signature(manifest):
		return _download_fail(chapter_id, "امضای بستهٔ محتوا معتبر نیست.")

	# 4. atomic activation
	if not _activate(chapter_id, staging, int((manifest as Dictionary).get("version", 1))):
		return _download_fail(chapter_id, "فعال‌سازی فصل ناموفق بود؛ نسخهٔ قبلی دست‌نخورده ماند.")

	_active_download = ""
	download_completed.emit(chapter_id)
	Log.info("Content", "chapter %s installed" % chapter_id)
	return true

## Returns "" when the manifest is acceptable, otherwise a Persian reason.
func validate_manifest(manifest: Dictionary, expect_id: String) -> String:
	if String(manifest.get("chapter_id", "")) != expect_id:
		return "شناسهٔ بسته با فصل درخواستی یکی نیست."
	var schema := int(manifest.get("schema_version", 0))
	if schema <= 0:
		return "نسخهٔ قالب بسته مشخص نیست."
	if schema > SUPPORTED_SCHEMA_VERSION:
		return "این فصل به نسخهٔ تازه‌تری از بازی نیاز دارد."
	var min_game := String(manifest.get("minimum_game_version", "0"))
	if _version_less_than(GAME_VERSION, min_game):
		return "برای این فصل باید بازی را به‌روز کنید."
	var files = manifest.get("files", [])
	if typeof(files) != TYPE_ARRAY or (files as Array).is_empty():
		return "فهرست فایل‌های بسته خالی است."
	for f in files:
		if typeof(f) != TYPE_DICTIONARY:
			return "فهرست فایل‌ها خراب است."
		var fd: Dictionary = f
		if not fd.has("path") or not fd.has("sha256") or not fd.has("size"):
			return "اطلاعات یکی از فایل‌ها ناقص است."
		# No downloaded package may carry executable content.
		var ext := String(fd["path"]).get_extension().to_lower()
		if ext in ["gd", "gdc", "gdns", "so", "dll", "dylib", "pck", "exe", "sh", "jar"]:
			return "بسته شامل فایل اجرایی است و پذیرفته نمی‌شود."
	for dep in manifest.get("dependencies", []):
		var dd: Dictionary = dep
		var dep_id := String(dd.get("chapter_id", ""))
		if not is_installed(dep_id):
			return "ابتدا باید فصل «%s» را دریافت کنید." % dep_id
		if installed_version(dep_id) < int(dd.get("min_version", 1)):
			return "فصل «%s» باید به‌روز شود." % dep_id
	return ""

func verify_manifest_signature(manifest: Dictionary) -> bool:
	var sig_b64 := String(manifest.get("signature", ""))
	if sig_b64.is_empty():
		# Unsigned packages are only tolerated for locally-served development
		# content, never for anything fetched from a production URL.
		if ContentEndpoints.allow_unsigned_content():
			Log.warn("Content", "accepting unsigned package (development mode)")
			return true
		Log.error("Content", "package has no signature")
		return false
	if _public_key == null:
		Log.error("Content", "cannot verify signature: no public key")
		return false
	var body := manifest.duplicate()
	body.erase("signature")
	var canonical := _canonical_json(body)
	var ctx := HashingContext.new()
	ctx.start(HashingContext.HASH_SHA256)
	ctx.update(canonical.to_utf8_buffer())
	var digest := ctx.finish()
	var sig := Marshalls.base64_to_raw(sig_b64)
	return _crypto.verify(HashingContext.HASH_SHA256, digest, sig, _public_key)

func _activate(chapter_id: String, staging: String, version: int) -> bool:
	var final_dir := "%s/%s" % [INSTALL_ROOT, chapter_id]
	var retired := "%s/%s.retired" % [INSTALL_ROOT, chapter_id]
	_wipe_dir(retired)
	# Move the old version aside rather than deleting it, so a failed rename
	# of the new one can be undone.
	var had_old := DirAccess.dir_exists_absolute(final_dir)
	if had_old and DirAccess.rename_absolute(final_dir, retired) != OK:
		return false
	if DirAccess.rename_absolute(staging, final_dir) != OK:
		if had_old:
			DirAccess.rename_absolute(retired, final_dir)
		return false
	_wipe_dir(retired)
	installed[chapter_id] = {"version": version, "path": final_dir}
	_save_installed_index()
	content_activated.emit(chapter_id, version)
	return true

func uninstall_chapter(chapter_id: String) -> bool:
	## Frees storage without touching entitlements: a paid chapter stays paid
	## for and can be fetched again (GDD §9.4).
	if not installed.has(chapter_id):
		return false
	_wipe_dir("%s/%s" % [INSTALL_ROOT, chapter_id])
	installed.erase(chapter_id)
	_save_installed_index()
	Log.info("Content", "chapter %s removed from storage" % chapter_id)
	return true

func storage_bytes_used() -> int:
	var total := 0
	for chapter_id in installed.keys():
		total += _dir_size(chapter_root(chapter_id))
	return total

# — helpers ——————————————————————————————————————————————

func _download_fail(chapter_id: String, reason: String) -> bool:
	last_error = reason
	_active_download = ""
	_wipe_dir("%s/%s" % [STAGING_ROOT, chapter_id])
	Log.error("Content", "%s: %s" % [chapter_id, reason])
	download_failed.emit(chapter_id, reason)
	return false

func _fetch(url: String) -> PackedByteArray:
	if _http.get_http_client_status() != HTTPClient.STATUS_DISCONNECTED:
		_http.cancel_request()
	var err := _http.request(url)
	if err != OK:
		Log.error("Content", "request(%s) failed: %d" % [url, err])
		return PackedByteArray()
	var result = await _http.request_completed
	var res_code: int = result[0]
	var http_code: int = result[1]
	var body: PackedByteArray = result[3]
	if res_code != HTTPRequest.RESULT_SUCCESS or http_code < 200 or http_code >= 300:
		Log.error("Content", "fetch %s -> result %d / http %d" % [url, res_code, http_code])
		return PackedByteArray()
	return body

func _sha256_hex(bytes: PackedByteArray) -> String:
	var ctx := HashingContext.new()
	ctx.start(HashingContext.HASH_SHA256)
	ctx.update(bytes)
	return ctx.finish().hex_encode()

## Key-sorted, separator-free JSON so that signing and verification agree
## byte-for-byte across languages.
func _canonical_json(value: Variant) -> String:
	match typeof(value):
		TYPE_DICTIONARY:
			var d: Dictionary = value
			var keys := d.keys()
			keys.sort()
			var parts := PackedStringArray()
			for k in keys:
				parts.append("%s:%s" % [JSON.stringify(String(k)), _canonical_json(d[k])])
			return "{" + ",".join(parts) + "}"
		TYPE_ARRAY:
			var parts2 := PackedStringArray()
			for v in (value as Array):
				parts2.append(_canonical_json(v))
			return "[" + ",".join(parts2) + "]"
		_:
			return JSON.stringify(value)

func _is_safe_relative_path(p: String) -> bool:
	if p.is_empty() or p.begins_with("/") or p.contains("\\"):
		return false
	if p.contains("..") or p.contains(":"):
		return false
	return true

func _version_less_than(a: String, b: String) -> bool:
	var pa := a.split(".")
	var pb := b.split(".")
	for i in maxi(pa.size(), pb.size()):
		var va := int(pa[i]) if i < pa.size() else 0
		var vb := int(pb[i]) if i < pb.size() else 0
		if va != vb:
			return va < vb
	return false

func _read_json(path: String) -> Dictionary:
	if not FileAccess.file_exists(path):
		return {}
	var f := FileAccess.open(path, FileAccess.READ)
	if f == null:
		return {}
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	return parsed if typeof(parsed) == TYPE_DICTIONARY else {}

func _load_installed_index() -> void:
	var d := _read_json(INSTALLED_INDEX)
	installed = d.get("chapters", {})

func _save_installed_index() -> void:
	var f := FileAccess.open(INSTALLED_INDEX, FileAccess.WRITE)
	if f == null:
		Log.error("Content", "could not write installed index")
		return
	f.store_string(JSON.stringify({"chapters": installed}, "  "))
	f.close()

func _wipe_dir(path: String) -> void:
	if not DirAccess.dir_exists_absolute(path):
		return
	var d := DirAccess.open(path)
	if d == null:
		return
	d.list_dir_begin()
	var name := d.get_next()
	while name != "":
		var child := path + "/" + name
		if d.current_is_dir():
			_wipe_dir(child)
		else:
			DirAccess.remove_absolute(child)
		name = d.get_next()
	d.list_dir_end()
	DirAccess.remove_absolute(path)

func _dir_size(path: String) -> int:
	var total := 0
	var d := DirAccess.open(path)
	if d == null:
		return 0
	d.list_dir_begin()
	var name := d.get_next()
	while name != "":
		var child := path + "/" + name
		if d.current_is_dir():
			total += _dir_size(child)
		else:
			var f := FileAccess.open(child, FileAccess.READ)
			if f != null:
				total += f.get_length()
				f.close()
		name = d.get_next()
	d.list_dir_end()
	return total
