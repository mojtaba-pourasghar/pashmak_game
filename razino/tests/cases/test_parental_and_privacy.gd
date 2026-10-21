extends RazinoTest
## The parent gate, consent defaults and the avatar privacy rules (GDD §10, §14).

func before_each() -> void:
	ParentalSettings.set_consent("allow_purchases", false)
	ParentalSettings.set_consent("allow_camera_avatar", false)
	ParentalSettings.set_consent("allow_server_photo_processing", false)

func test_consent_defaults_to_off():
	not_ok(ParentalSettings.allow_purchases, "purchases are off until an adult turns them on")
	not_ok(ParentalSettings.allow_camera_avatar, "camera is off by default")
	not_ok(ParentalSettings.allow_server_photo_processing, "server photo processing is off by default")

func test_revoking_camera_also_revokes_server_processing():
	ParentalSettings.set_consent("allow_camera_avatar", true)
	ParentalSettings.set_consent("allow_server_photo_processing", true)
	ok(ParentalSettings.allow_server_photo_processing, "both on")
	ParentalSettings.set_consent("allow_camera_avatar", false)
	not_ok(ParentalSettings.allow_server_photo_processing,
		"turning the camera off also turns off the narrower server consent")

func test_pin_must_be_four_digits():
	not_ok(ParentalSettings.set_pin("12"), "too short is refused")
	not_ok(ParentalSettings.set_pin("12345"), "too long is refused")
	not_ok(ParentalSettings.set_pin("12a4"), "non-digits are refused")
	ok(ParentalSettings.set_pin("4821"), "a four-digit PIN is accepted")

func test_pin_is_not_stored_in_plain_text():
	ParentalSettings.set_pin("4821")
	var f := FileAccess.open(ParentalSettings.PATH, FileAccess.READ)
	var text := f.get_as_text()
	f.close()
	not_ok(text.contains("4821"), "the PIN itself does not appear in the config file")

func test_gate_accepts_the_right_pin_and_rejects_others():
	ParentalSettings.set_pin("4821")
	ok(ParentalSettings.verify("4821"), "the right PIN passes")
	not_ok(ParentalSettings.verify("0000"), "a wrong PIN fails")

func test_repeated_failures_lock_the_gate():
	ParentalSettings.set_pin("4821")
	for i in ParentalSettings.MAX_ATTEMPTS:
		ParentalSettings.verify("0000")
	ok(ParentalSettings.is_locked_out(), "the gate locks after repeated wrong tries")
	not_ok(ParentalSettings.verify("4821"), "and even the right PIN is refused while locked")
	gt(ParentalSettings.seconds_until_unlock(), 0.0, "the lockout reports time remaining")

# — avatar ———————————————————————————————————————————————

func test_the_game_is_fully_playable_with_no_photo():
	AvatarManager.delete_avatar()
	AvatarManager.randomize_avatar()
	not_empty(AvatarManager.avatar_id(), "an avatar exists without any camera use")
	not_ok(bool(AvatarManager.config.get("from_photo", true)), "and it is not marked as photo-derived")
	ok(AvatarManager.part_count("skin") > 1, "there are real choices to make by hand")

func test_photo_hints_are_refused_without_consent():
	ParentalSettings.set_consent("allow_camera_avatar", false)
	var img := Image.create(64, 64, false, Image.FORMAT_RGB8)
	img.fill(Color(0.8, 0.6, 0.5))
	not_ok(AvatarManager.apply_photo_hints(img), "no consent means no photo processing")

func test_photo_source_is_discarded_after_processing():
	ParentalSettings.set_consent("allow_camera_avatar", true)
	# Write a temp capture, as the camera path would.
	var img := Image.create(64, 64, false, Image.FORMAT_RGB8)
	for y in 64:
		for x in 64:
			img.set_pixel(x, y, Color(0.2, 0.15, 0.1) if y < 20 else Color(0.85, 0.68, 0.55))
	img.save_png(AvatarManager.temp_photo_path())
	ok(AvatarManager.temp_file_exists(), "the temp capture exists before processing")

	ok(AvatarManager.apply_photo_hints(img), "hints are applied with consent")
	not_ok(AvatarManager.temp_file_exists(), "and the source image is gone afterwards")

func test_forgetting_a_photo_clears_the_derived_flag():
	ParentalSettings.set_consent("allow_camera_avatar", true)
	AvatarManager.config["from_photo"] = true
	AvatarManager.forget_photo_derived_data()
	not_ok(bool(AvatarManager.config.get("from_photo", true)), "the photo-derived marker is cleared")
	not_ok(AvatarManager.temp_file_exists(), "and no capture is left behind")

func test_a_flat_image_is_refused_rather_than_guessed():
	ParentalSettings.set_consent("allow_camera_avatar", true)
	var flat := Image.create(64, 64, false, Image.FORMAT_RGB8)
	flat.fill(Color(0.5, 0.5, 0.5))
	not_ok(AvatarManager.apply_photo_hints(flat),
		"a featureless frame is refused with a message rather than producing an invented result")

func test_avatar_survives_a_restart():
	AvatarManager.set_part("hair_style", 3)
	AvatarManager.set_part("outfit", 2)
	var before := AvatarManager.avatar_id()
	AvatarManager.config = {}
	AvatarManager.load_avatar()
	eq(AvatarManager.avatar_id(), before, "the avatar comes back the same after a reload")

func test_deleting_the_avatar_removes_its_file():
	AvatarManager.set_part("hair_style", 2)
	AvatarManager.delete_avatar()
	not_ok(FileAccess.file_exists(AvatarManager.AVATAR_PATH), "the avatar file is really deleted")
	eq(GameState.avatar_id, "", "and the game state no longer references it")
