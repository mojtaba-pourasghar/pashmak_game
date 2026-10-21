extends RazinoTest
## Package validation and the offline guarantee (GDD §7, QA checklist items on
## download, incomplete packages, invalid hashes and offline play).

func _manifest(overrides: Dictionary = {}) -> Dictionary:
	var base := {
		"chapter_id": "season_02",
		"schema_version": 1,
		"version": 2,
		"minimum_game_version": "0.1.0",
		"files": [
			{"path": "story.json", "size": 10, "sha256": "ab" .repeat(32)},
		],
		"dependencies": [],
	}
	for k in overrides.keys():
		base[k] = overrides[k]
	return base

func test_a_good_manifest_passes():
	eq(ContentManager.validate_manifest(_manifest(), "season_02"), "", "a well-formed manifest is accepted")

func test_wrong_chapter_id_is_refused():
	ne(ContentManager.validate_manifest(_manifest(), "season_03"), "", "a manifest for another chapter is refused")

func test_future_schema_is_refused_with_a_readable_reason():
	var reason := ContentManager.validate_manifest(_manifest({"schema_version": 99}), "season_02")
	ne(reason, "", "a newer schema is refused")
	contains(reason, "بازی", "and the reason is in Persian and mentions updating the game")

func test_newer_minimum_game_version_is_refused():
	ne(ContentManager.validate_manifest(_manifest({"minimum_game_version": "9.0.0"}), "season_02"), "",
		"a package needing a newer build is refused")

func test_empty_file_list_is_refused():
	ne(ContentManager.validate_manifest(_manifest({"files": []}), "season_02"), "", "an empty package is refused")

func test_executable_payloads_are_refused():
	## Nothing downloaded may be code. This is the rule that keeps a content
	## package from becoming a code-delivery channel.
	for ext in ["gd", "so", "dll", "pck", "sh", "jar", "exe"]:
		var m := _manifest({"files": [{"path": "evil." + ext, "size": 1, "sha256": "aa" .repeat(32)}]})
		ne(ContentManager.validate_manifest(m, "season_02"), "", "a .%s payload is refused" % ext)

func test_missing_file_metadata_is_refused():
	var m := _manifest({"files": [{"path": "story.json"}]})
	ne(ContentManager.validate_manifest(m, "season_02"), "", "a file without size and hash is refused")

func test_unmet_dependency_is_refused():
	var m := _manifest({"dependencies": [{"chapter_id": "season_99", "min_version": 1}]})
	ne(ContentManager.validate_manifest(m, "season_02"), "", "a missing dependency blocks install")

func test_unsigned_package_is_refused_without_a_dev_endpoint():
	not_ok(ContentManager.verify_manifest_signature(_manifest()),
		"an unsigned package is refused when no development endpoint is configured")

func test_path_traversal_is_refused():
	for bad in ["../escape.json", "/etc/passwd", "a\\b.json", "c:/x.json", "ok/../../x"]:
		not_ok(ContentManager._is_safe_relative_path(bad), "'%s' is rejected as a package path" % bad)
	ok(ContentManager._is_safe_relative_path("audio/vo/line.ogg"), "a normal relative path is fine")

func test_bundled_chapter_plays_with_no_network():
	## The offline guarantee: chapter 1 ships inside the app and must be
	## readable without any endpoint configured.
	ok(ContentManager.is_bundled("season_01"), "chapter 1 is bundled")
	ok(ContentManager.is_installed("season_01"), "and counts as installed")
	var story := ContentManager.load_chapter_story("season_01")
	not_empty(story, "its story data loads with no server")
	not_empty(story.get("scenes", []), "and it has scenes")

func test_download_fails_cleanly_with_no_endpoint():
	var story_before := ContentManager.load_chapter_story("season_01")
	var ok_result := await ContentManager.download_chapter("season_99")
	not_ok(ok_result, "downloading an unknown chapter fails")
	not_empty(ContentManager.last_error, "and says why, in Persian")
	eq(ContentManager.load_chapter_story("season_01"), story_before,
		"an installed chapter is untouched by a failed download")

func test_catalog_lists_all_ten_chapters():
	eq(ContentManager.chapter_ids().size(), 10, "the catalogue has ten chapters")
	var free_count := 0
	for c in ContentManager.catalog:
		if String((c as Dictionary).get("access_type", "")) == "free":
			free_count += 1
	eq(free_count, 1, "exactly one chapter is free, as the pricing model says")
