extends RazinoTest
## دفترچهٔ سرنخ‌ها — collecting, categorising and combining (GDD §5 screen 3).

func before_each() -> void:
	GameState.reset()
	ClueJournal.load_chapter_clues(ContentManager.load_chapter_story("season_01"))

func after_each() -> void:
	GameState.reset()

func test_clue_definitions_load_from_chapter_data():
	gt(float(ClueJournal.definitions.size()), 10.0, "chapter 1's clues loaded")
	not_empty(ClueJournal.title_of("CLUE_S01_SOUND"), "a clue has a title")
	not_empty(ClueJournal.body_of("CLUE_S01_SOUND"), "and body text")

func test_only_discovered_clues_are_listed():
	empty(ClueJournal.discovered(), "the journal starts empty")
	GameState.add_clue("CLUE_S01_SOUND")
	eq(ClueJournal.discovered().size(), 1, "a discovered clue appears")
	contains(ClueJournal.discovered(), "CLUE_S01_SOUND", "and it is the right one")

func test_clues_are_grouped_by_category():
	GameState.add_clue("CLUE_S01_SOUND")       # observation
	GameState.add_clue("CLUE_S01_NOTEBOOK")    # document
	contains(ClueJournal.categories(), "observation", "observation category appears")
	contains(ClueJournal.categories(), "document", "document category appears")
	eq(ClueJournal.discovered_in_category("document").size(), 1, "one document clue")

func test_combining_two_right_clues_yields_a_third():
	GameState.add_clue("CLUE_S01_INITIALS")
	GameState.add_clue("CLUE_S01_LIBRARIAN")
	var result := ClueJournal.combine("CLUE_S01_INITIALS", "CLUE_S01_LIBRARIAN")
	eq(result, "CLUE_S01_NAME", "the pair produces the inventor's name")
	ok(GameState.has_clue("CLUE_S01_NAME"), "and it lands in the journal")

func test_combining_is_order_independent():
	GameState.add_clue("CLUE_S01_INITIALS")
	GameState.add_clue("CLUE_S01_LIBRARIAN")
	eq(ClueJournal.combine("CLUE_S01_LIBRARIAN", "CLUE_S01_INITIALS"), "CLUE_S01_NAME",
		"the reverse order works too — combining is a question, not a sequence")

func test_a_wrong_pairing_costs_nothing():
	GameState.add_clue("CLUE_S01_SOUND")
	GameState.add_clue("CLUE_S01_DUST")
	var rejected := []
	ClueJournal.combination_rejected.connect(func(a, b): rejected.append([a, b]))
	eq(ClueJournal.combine("CLUE_S01_SOUND", "CLUE_S01_DUST"), "", "an unrelated pair yields nothing")
	eq(rejected.size(), 1, "the journal answers gently rather than silently")
	eq(GameState.clues.size(), 2, "and nothing is taken away for guessing")

func test_cannot_combine_clues_you_do_not_hold():
	eq(ClueJournal.combine("CLUE_S01_INITIALS", "CLUE_S01_LIBRARIAN"), "",
		"clues you have not found cannot be combined")

func test_open_questions_appear_and_then_resolve():
	GameState.add_clue("CLUE_S01_MAP_PIECE")
	var open_now := ClueJournal.open_questions()
	gt(float(open_now.size()), 0.0, "an open question is shown once its trigger clue is found")
	GameState.add_clue("CLUE_S01_NAME")
	var after := ClueJournal.open_questions()
	var still_open := false
	for q in after:
		if String((q as Dictionary).get("clue_id", "")) == "Q_S01_WHO":
			still_open = true
	not_ok(still_open, "and it closes once the answering clue is found")
