extends Node
## Purchases (GDD §11) — an abstract store with server-side verification.
##
## Deliberate limits of this implementation:
##   * There is NO bundled store SDK. Cafe Bazaar and Myket integration needs
##     their official libraries and merchant credentials; `StoreBackend` is the
##     seam they plug into. See docs/PURCHASES.md.
##   * The sandbox backend never grants a real entitlement. It writes to a
##     separate sandbox ledger that the real one ignores, so a test purchase
##     can never be mistaken for a paid one.
##   * An entitlement is only written after a receipt is verified SERVER-side.
##     The client never decides on its own that something was bought.

signal purchase_started(product_id: String)
signal purchase_succeeded(product_id: String)
signal purchase_failed(product_id: String, reason: String)
signal entitlements_restored(count: int)
signal entitlement_updated(product_id: String)

const LEDGER_PATH := "user://entitlements.json"
const SANDBOX_LEDGER_PATH := "user://entitlements.sandbox.json"

enum Mode { UNCONFIGURED, SANDBOX, LIVE }

var mode: Mode = Mode.UNCONFIGURED
var _entitlements: Dictionary = {}          # product_id -> {"verified_at": int, "source": String}
var _sandbox_entitlements: Dictionary = {}
var _backend: StoreBackend = null
var _http: HTTPRequest

func _ready() -> void:
	_http = HTTPRequest.new()
	add_child(_http)
	_load_ledgers()
	_select_backend()

func _select_backend() -> void:
	# A real backend is installed by whoever integrates the store SDK:
	#   PurchaseManager.install_backend(CafeBazaarBackend.new())
	# Until then the game runs in sandbox and says so plainly.
	if _backend == null:
		_backend = SandboxStoreBackend.new()
		mode = Mode.SANDBOX
		Log.warn("Purchase", "no store backend installed — sandbox only, no real purchases")

func install_backend(backend: StoreBackend) -> void:
	_backend = backend
	mode = Mode.LIVE if backend.is_live() else Mode.SANDBOX
	Log.info("Purchase", "store backend: %s (%s)" % [backend.store_name(), "live" if mode == Mode.LIVE else "sandbox"])

func is_live() -> bool:
	return mode == Mode.LIVE

## The only question the rest of the game should ask.
func has_entitlement(product_id: String) -> bool:
	if _entitlements.has(product_id):
		return true
	# Sandbox grants apply only while an explicitly sandboxed backend is live,
	# and are never written to the real ledger.
	return mode == Mode.SANDBOX and _sandbox_entitlements.has(product_id)

func entitlement_source(product_id: String) -> String:
	if _entitlements.has(product_id):
		return String((_entitlements[product_id] as Dictionary).get("source", "unknown"))
	if _sandbox_entitlements.has(product_id):
		return "sandbox"
	return ""

func products() -> Array:
	return _backend.products() if _backend != null else []

func purchase(product_id: String) -> void:
	if not ParentalSettings.allow_purchases:
		purchase_failed.emit(product_id, "خرید در بخش والدین فعال نشده است.")
		return
	if _backend == null:
		purchase_failed.emit(product_id, "فروشگاه در دسترس نیست.")
		return
	if has_entitlement(product_id):
		# GDD §11.1: re-buying something already owned must not charge again.
		purchase_succeeded.emit(product_id)
		return

	purchase_started.emit(product_id)
	var receipt := await _backend.begin_purchase(product_id)
	if receipt.is_empty():
		purchase_failed.emit(product_id, "خرید انجام نشد.")
		return

	var verdict := await verify_receipt(product_id, receipt)
	if verdict.get("ok", false):
		_grant(product_id, String(verdict.get("source", _backend.store_name())), verdict.get("sandbox", not is_live()))
		purchase_succeeded.emit(product_id)
	else:
		purchase_failed.emit(product_id, String(verdict.get("reason", "تأیید خرید ناموفق بود.")))

## Receipts are verified by the server. A client-side "looks fine to me" is
## not an entitlement. With no API configured this returns a refusal, not a
## success — a missing server must never read as a completed purchase.
func verify_receipt(product_id: String, receipt: Dictionary) -> Dictionary:
	var api := ContentEndpoints.api_base_url()
	if api.is_empty():
		if mode == Mode.SANDBOX:
			# Sandbox: grant a clearly-marked sandbox entitlement so the flow
			# can be exercised end to end, without touching the real ledger.
			return {"ok": true, "source": "sandbox", "sandbox": true}
		return {"ok": false, "reason": "سرور تأیید خرید تنظیم نشده است."}

	var body := JSON.stringify({
		"product_id": product_id,
		"store": _backend.store_name(),
		"receipt": receipt,
	})
	var headers := ["Content-Type: application/json"]
	var err := _http.request(api + "/v1/purchases/verify/", headers, HTTPClient.METHOD_POST, body)
	if err != OK:
		return {"ok": false, "reason": "ارتباط با سرور برقرار نشد."}
	var result = await _http.request_completed
	if int(result[1]) != 200:
		return {"ok": false, "reason": "سرور خرید را تأیید نکرد."}
	var parsed = JSON.parse_string((result[3] as PackedByteArray).get_string_from_utf8())
	if typeof(parsed) != TYPE_DICTIONARY:
		return {"ok": false, "reason": "پاسخ سرور خوانا نبود."}
	return {
		"ok": bool((parsed as Dictionary).get("valid", false)),
		"source": String((parsed as Dictionary).get("store", "server")),
		"sandbox": bool((parsed as Dictionary).get("sandbox", false)),
		"reason": String((parsed as Dictionary).get("reason", "خرید تأیید نشد.")),
	}

## GDD §9.4: after a reinstall the player must be able to get back what they
## paid for, through the store's own records.
func restore_purchases() -> int:
	if _backend == null:
		return 0
	var restored := 0
	for r in await _backend.list_owned():
		var rd: Dictionary = r
		var pid := String(rd.get("product_id", ""))
		if pid.is_empty() or has_entitlement(pid):
			continue
		var verdict := await verify_receipt(pid, rd)
		if verdict.get("ok", false):
			_grant(pid, String(verdict.get("source", "restore")), verdict.get("sandbox", not is_live()))
			restored += 1
	entitlements_restored.emit(restored)
	Log.info("Purchase", "restored %d entitlement(s)" % restored)
	return restored

func _grant(product_id: String, source: String, sandbox: bool) -> void:
	var record := {"verified_at": int(Time.get_unix_time_from_system()), "source": source}
	if sandbox:
		_sandbox_entitlements[product_id] = record
	else:
		_entitlements[product_id] = record
	_save_ledgers()
	# A chapter entitlement unlocks the chapter itself.
	if product_id.begins_with("season_"):
		GameState.unlock_chapter(product_id)
		SaveManager.mark_dirty()
	entitlement_updated.emit(product_id)

func clear_sandbox_entitlements() -> void:
	_sandbox_entitlements.clear()
	_save_ledgers()

func _load_ledgers() -> void:
	_entitlements = _read(LEDGER_PATH)
	_sandbox_entitlements = _read(SANDBOX_LEDGER_PATH)

func _read(path: String) -> Dictionary:
	if not FileAccess.file_exists(path):
		return {}
	var f := FileAccess.open(path, FileAccess.READ)
	if f == null:
		return {}
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	return parsed if typeof(parsed) == TYPE_DICTIONARY else {}

func _save_ledgers() -> void:
	_write(LEDGER_PATH, _entitlements)
	_write(SANDBOX_LEDGER_PATH, _sandbox_entitlements)

func _write(path: String, d: Dictionary) -> void:
	var f := FileAccess.open(path, FileAccess.WRITE)
	if f == null:
		Log.error("Purchase", "could not write %s" % path)
		return
	f.store_string(JSON.stringify(d, "  "))
	f.close()
