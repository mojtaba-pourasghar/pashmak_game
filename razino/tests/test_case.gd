class_name RazinoTest
extends RefCounted
## Minimal assertion base class for the headless test suite.
##
## A test file extends this and defines methods whose names start with
## `test_`. The runner calls each one on a fresh instance, so no test can
## leak state into the next. `before_each` / `after_each` hook in setup.

var failures: PackedStringArray = []
var assertions: int = 0

func before_each() -> void:
	pass

func after_each() -> void:
	pass

## A test is a RefCounted, so it has no get_tree(). This waits one frame.
func next_frame() -> void:
	await (Engine.get_main_loop() as SceneTree).process_frame

func _fail(message: String) -> void:
	failures.append(message)

func ok(condition: bool, message: String) -> void:
	assertions += 1
	if not condition:
		_fail("expected true: %s" % message)

func not_ok(condition: bool, message: String) -> void:
	assertions += 1
	if condition:
		_fail("expected false: %s" % message)

func eq(actual: Variant, expected: Variant, message: String) -> void:
	assertions += 1
	if actual != expected:
		_fail("%s — expected <%s>, got <%s>" % [message, str(expected), str(actual)])

func ne(actual: Variant, expected: Variant, message: String) -> void:
	assertions += 1
	if actual == expected:
		_fail("%s — expected anything but <%s>" % [message, str(expected)])

func gt(actual: float, threshold: float, message: String) -> void:
	assertions += 1
	if actual <= threshold:
		_fail("%s — expected > %s, got %s" % [message, str(threshold), str(actual)])

func contains(haystack: Variant, needle: Variant, message: String) -> void:
	assertions += 1
	var found := false
	match typeof(haystack):
		TYPE_STRING: found = (haystack as String).contains(String(needle))
		TYPE_ARRAY: found = (haystack as Array).has(needle)
		TYPE_PACKED_STRING_ARRAY: found = (haystack as PackedStringArray).has(String(needle))
		TYPE_DICTIONARY: found = (haystack as Dictionary).has(needle)
	if not found:
		_fail("%s — <%s> not found in %s" % [message, str(needle), str(haystack)])

func empty(value: Variant, message: String) -> void:
	assertions += 1
	var size := 0
	match typeof(value):
		TYPE_STRING: size = (value as String).length()
		TYPE_ARRAY: size = (value as Array).size()
		TYPE_PACKED_STRING_ARRAY: size = (value as PackedStringArray).size()
		TYPE_DICTIONARY: size = (value as Dictionary).size()
	if size != 0:
		_fail("%s — expected empty, got %s" % [message, str(value)])

func not_empty(value: Variant, message: String) -> void:
	assertions += 1
	var size := 0
	match typeof(value):
		TYPE_STRING: size = (value as String).length()
		TYPE_ARRAY: size = (value as Array).size()
		TYPE_PACKED_STRING_ARRAY: size = (value as PackedStringArray).size()
		TYPE_DICTIONARY: size = (value as Dictionary).size()
	if size == 0:
		_fail("%s — expected something, got empty" % message)
