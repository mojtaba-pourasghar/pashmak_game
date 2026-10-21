extends RazinoTest
## Purchase rules (GDD §11.1). The point of these tests is that no path
## through this code can hand out an entitlement that a server did not verify,
## and that nothing here can be mistaken for a real purchase.

func before_each() -> void:
	PurchaseManager.clear_sandbox_entitlements()
	ParentalSettings.set_consent("allow_purchases", false)

func after_each() -> void:
	PurchaseManager.clear_sandbox_entitlements()
	ParentalSettings.set_consent("allow_purchases", false)

func test_no_store_backend_means_sandbox_not_live():
	not_ok(PurchaseManager.is_live(), "with no store SDK installed the game is NOT in live mode")
	eq(PurchaseManager.mode, PurchaseManager.Mode.SANDBOX, "it reports sandbox explicitly")

func test_purchase_is_blocked_until_a_parent_allows_it():
	var failed := []
	PurchaseManager.purchase_failed.connect(func(_p, reason): failed.append(reason))
	await PurchaseManager.purchase("season_02")
	eq(failed.size(), 1, "the purchase was refused")
	contains(failed[0], "والدین", "and the reason points at the parent section")
	not_ok(PurchaseManager.has_entitlement("season_02"), "no entitlement was granted")

func test_sandbox_purchase_is_marked_sandbox_and_kept_separate():
	ParentalSettings.set_consent("allow_purchases", true)
	await PurchaseManager.purchase("season_02")
	ok(PurchaseManager.has_entitlement("season_02"), "the sandbox flow completes")
	eq(PurchaseManager.entitlement_source("season_02"), "sandbox",
		"and it is labelled sandbox, never as a real store purchase")
	var f := FileAccess.open(PurchaseManager.LEDGER_PATH, FileAccess.READ)
	var real_ledger := f.get_as_text() if f != null else "{}"
	if f != null:
		f.close()
	not_ok(real_ledger.contains("season_02"),
		"the real entitlement ledger was NOT written by a sandbox purchase")

func test_verification_refuses_when_no_server_is_configured_in_live_mode():
	## The important negative: a missing server must read as "not verified",
	## never as success.
	PurchaseManager.mode = PurchaseManager.Mode.LIVE
	var verdict := await PurchaseManager.verify_receipt("season_02", {"token": "x"})
	not_ok(verdict.get("ok", true), "with no API configured, live verification fails")
	contains(String(verdict.get("reason", "")), "سرور", "and says the server is not configured")
	PurchaseManager.mode = PurchaseManager.Mode.SANDBOX

func test_buying_something_already_owned_does_not_charge_again():
	ParentalSettings.set_consent("allow_purchases", true)
	await PurchaseManager.purchase("season_02")
	var started := []
	PurchaseManager.purchase_started.connect(func(p): started.append(p))
	await PurchaseManager.purchase("season_02")
	eq(started.size(), 0, "a second purchase of an owned product starts no transaction")

func test_a_failed_store_transaction_grants_nothing():
	ParentalSettings.set_consent("allow_purchases", true)
	var backend := SandboxStoreBackend.new()
	backend.fail_next = true
	PurchaseManager.install_backend(backend)
	await PurchaseManager.purchase("season_03")
	not_ok(PurchaseManager.has_entitlement("season_03"), "a cancelled purchase grants nothing")
	PurchaseManager.install_backend(SandboxStoreBackend.new())

func test_unknown_product_is_refused():
	ParentalSettings.set_consent("allow_purchases", true)
	await PurchaseManager.purchase("not_a_product")
	not_ok(PurchaseManager.has_entitlement("not_a_product"), "an unknown product grants nothing")

func test_paid_chapter_cannot_be_downloaded_without_an_entitlement():
	var refusals := []
	ContentManager.download_failed.connect(func(_c, reason): refusals.append(reason))
	var result := await ContentManager.download_chapter("season_02")
	not_ok(result, "a paid chapter is not downloaded without entitlement or endpoint")
	eq(refusals.size(), 1, "and the refusal is reported")

func test_chapter_entitlement_unlocks_the_chapter():
	ParentalSettings.set_consent("allow_purchases", true)
	await PurchaseManager.purchase("season_02")
	ok(GameState.is_chapter_unlocked("season_02"), "owning a chapter unlocks it in game state")
