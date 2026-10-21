extends Node
## End-to-end content delivery test. Needs a local server, so it is kept out
## of the hermetic unit suite and run by tools/run_integration.sh.
##
## It exercises the whole chain the GDD asks for: fetch a signed package,
## verify every file's size and hash, verify the signature, activate it
## atomically, play it offline, and — the part that matters most — prove that a
## tampered or interrupted package leaves the previously installed version
## alone rather than destroying it.

const BASE_URL := "http://127.0.0.1:8777"

var _failures: Array = []
var _checks := 0

func _ready() -> void:
	await get_tree().process_frame
	print("\n── Razino content delivery integration ──")
	_configure_endpoint()
	await _test_download_and_activate()
	await _test_offline_play()
	await _test_tampered_package_is_refused()
	await _test_unknown_chapter_fails_cleanly()
	await _test_uninstall_keeps_entitlement()
	_report()

func _check(condition: bool, label: String) -> void:
	_checks += 1
	if condition:
		print("  ✓ %s" % label)
	else:
		print("  ✗ %s" % label)
		_failures.append(label)

func _configure_endpoint() -> void:
	var cfg := ConfigFile.new()
	cfg.set_value("content", "base_url", BASE_URL)
	# Packages served here are signed; unsigned ones are NOT accepted.
	cfg.set_value("content", "allow_unsigned", false)
	cfg.set_value("api", "base_url", "")
	cfg.save(ContentEndpoints.USER_CONFIG)
	print("endpoint: %s" % ContentEndpoints.content_base_url())

func _test_download_and_activate() -> void:
	print("\n[1] download, verify and activate a signed package")
	ContentManager.uninstall_chapter("season_02")
	ParentalSettings.set_consent("allow_purchases", true)
	await PurchaseManager.purchase("season_02")
	_check(PurchaseManager.has_entitlement("season_02"), "entitlement granted (sandbox)")

	var ok := await ContentManager.download_chapter("season_02")
	_check(ok, "download reported success (%s)" % ContentManager.last_error)
	_check(ContentManager.is_installed("season_02"), "chapter is installed")

	var story := ContentManager.load_chapter_story("season_02")
	_check(not story.is_empty(), "story.json loads from the installed package")
	_check(int(story.get("scenes", []).size()) > 10, "it has the expected scenes")
	_check(StoryEngine.load_chapter("season_02"), "the engine accepts the downloaded chapter")

func _test_offline_play() -> void:
	print("\n[2] the installed chapter plays with the server unreachable")
	var cfg := ConfigFile.new()
	cfg.set_value("content", "base_url", "http://127.0.0.1:9")  # nothing listens here
	cfg.save(ContentEndpoints.USER_CONFIG)

	var story := ContentManager.load_chapter_story("season_02")
	_check(not story.is_empty(), "story still loads with no reachable server")
	GameState.reset()
	_check(StoryEngine.load_story(story), "the engine runs it offline")
	StoryEngine.start()
	_check(not StoryEngine.current_scene.is_empty(), "the first scene opens offline")

	_configure_endpoint()

func _test_tampered_package_is_refused() -> void:
	print("\n[3] a tampered package is refused and the good version survives")
	var before := ContentManager.load_chapter_story("season_02")
	_check(not before.is_empty(), "a good version is installed to begin with")

	# Point at the tampered tree the runner script prepared.
	var cfg := ConfigFile.new()
	cfg.set_value("content", "base_url", BASE_URL + "/tampered")
	cfg.save(ContentEndpoints.USER_CONFIG)

	var ok := await ContentManager.download_chapter("season_02")
	_check(not ok, "the tampered package was refused")
	_check(ContentManager.last_error != "", "with a reason: %s" % ContentManager.last_error)

	var after := ContentManager.load_chapter_story("season_02")
	_check(after == before, "the previously installed version is untouched")
	_check(StoryEngine.load_story(after), "and is still playable")

	_configure_endpoint()

func _test_unknown_chapter_fails_cleanly() -> void:
	print("\n[4] a chapter that is not in the catalogue fails cleanly")
	var ok := await ContentManager.download_chapter("season_99")
	_check(not ok, "refused")
	_check(not ContentManager.is_installed("season_99"), "nothing was installed")

func _test_uninstall_keeps_entitlement() -> void:
	print("\n[5] freeing storage does not take away what was paid for")
	_check(ContentManager.uninstall_chapter("season_02"), "chapter removed from storage")
	_check(not ContentManager.is_installed("season_02"), "it is gone from disk")
	_check(PurchaseManager.has_entitlement("season_02"), "the entitlement is still held")

func _report() -> void:
	print("\n──────────────────────────────────────────")
	print(" %d checks — %d passed, %d failed" % [_checks, _checks - _failures.size(), _failures.size()])
	print("──────────────────────────────────────────\n")
	get_tree().quit(1 if _failures.size() > 0 else 0)
