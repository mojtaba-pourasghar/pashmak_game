class_name StoreBackend
extends RefCounted
## The seam a real store SDK plugs into (Cafe Bazaar, Myket, Google Play).
##
## Implement these four methods in a subclass, hand it to
## PurchaseManager.install_backend(), and nothing else in the game changes.
## docs/PURCHASES.md spells out what each store needs.

func store_name() -> String:
	return "abstract"

## Must return false for anything that cannot take real money.
func is_live() -> bool:
	return false

func products() -> Array:
	return []

## Returns an opaque receipt dictionary for the server to verify, or {} if the
## purchase did not complete. Never decide entitlement here.
func begin_purchase(_product_id: String) -> Dictionary:
	return {}

## Receipts the store says this account already owns.
func list_owned() -> Array:
	return []
