extends RazinoTest
## Proves the harness itself works and the autoloads are reachable from a test.

func test_harness_runs():
	ok(true, "harness executes a test")
	eq(1 + 1, 2, "arithmetic")

func test_autoloads_present():
	ok(Engine.has_singleton("Log") or Log != null, "Log autoload")
	ok(GameState != null, "GameState autoload")
	ok(StoryEngine != null, "StoryEngine autoload")
	ok(ContentManager != null, "ContentManager autoload")
