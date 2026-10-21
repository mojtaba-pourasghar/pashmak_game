class_name SandboxStoreBackend
extends StoreBackend
## A test double for running the purchase flow end to end with no store SDK.
##
## It is deliberately obvious about what it is: `is_live()` is false, every
## receipt it mints is marked sandbox, and PurchaseManager files those in a
## separate ledger. Nothing here can produce a real entitlement.

const CATALOG := [
	{"product_id": "season_02", "title": "فصل ۲ — کتابخانهٔ هزار در", "price_irr": 0, "kind": "chapter"},
	{"product_id": "season_03", "title": "فصل ۳ — جنگل نشانه‌ها", "price_irr": 0, "kind": "chapter"},
	{"product_id": "bundle_02_04", "title": "بستهٔ سه‌فصلی (۲ تا ۴)", "price_irr": 0, "kind": "bundle"},
]

## Set by tests to exercise the failure path.
var fail_next: bool = false

func store_name() -> String:
	return "sandbox"

func is_live() -> bool:
	return false

func products() -> Array:
	return CATALOG.duplicate(true)

func begin_purchase(product_id: String) -> Dictionary:
	if fail_next:
		fail_next = false
		return {}
	var known := false
	for p in CATALOG:
		if String((p as Dictionary)["product_id"]) == product_id:
			known = true
			break
	if not known:
		return {}
	return {
		"product_id": product_id,
		"sandbox": true,
		"token": "SANDBOX-%d" % Time.get_ticks_msec(),
		"issued_at": int(Time.get_unix_time_from_system()),
	}

func list_owned() -> Array:
	return []
