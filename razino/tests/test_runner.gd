extends Node
## Headless test runner.
##
##   godot --headless res://tests/test_runner.tscn
##
## Exits 0 when every test passes, 1 otherwise, so CI can gate on it.

const CASES_DIR := "res://tests/cases"

var _total := 0
var _passed := 0
var _failed := 0
var _assertions := 0
var _failures: Array = []

func _ready() -> void:
	await get_tree().process_frame
	print("")
	print("──────────────────────────────────────────────")
	print(" Razino test suite")
	print("──────────────────────────────────────────────")
	var files := _discover()
	if files.is_empty():
		push_error("no test files found in %s" % CASES_DIR)
		get_tree().quit(1)
		return
	for path in files:
		await _run_file(path)
	_report()

func _discover() -> PackedStringArray:
	var out := PackedStringArray()
	var d := DirAccess.open(CASES_DIR)
	if d == null:
		return out
	d.list_dir_begin()
	var name := d.get_next()
	while name != "":
		if not d.current_is_dir() and name.begins_with("test_") and name.ends_with(".gd"):
			out.append(CASES_DIR + "/" + name)
		name = d.get_next()
	d.list_dir_end()
	out.sort()
	return out

func _run_file(path: String) -> void:
	var script: GDScript = load(path)
	if script == null or not script.can_instantiate():
		_failed += 1
		_total += 1
		print("\n  %s" % path.get_file())
		print("    ✗ could not load (parse error?)")
		_failures.append("%s — could not load" % path)
		return
	var probe = script.new()
	var methods := []
	for m in probe.get_method_list():
		var n: String = m["name"]
		if n.begins_with("test_") and not methods.has(n):
			methods.append(n)
	methods.sort()
	print("\n  %s" % path.get_file())
	for method_name in methods:
		# Fresh instance per test: no shared state between cases.
		var case = script.new()
		_total += 1
		case.before_each()
		await case.call(method_name)
		case.after_each()
		_assertions += case.assertions
		if case.failures.is_empty():
			_passed += 1
			print("    ✓ %s" % method_name)
		else:
			_failed += 1
			print("    ✗ %s" % method_name)
			for f in case.failures:
				print("        %s" % f)
				_failures.append("%s::%s — %s" % [path.get_file(), method_name, f])

func _report() -> void:
	print("")
	print("──────────────────────────────────────────────")
	print(" %d tests, %d assertions — %d passed, %d failed" % [_total, _assertions, _passed, _failed])
	print("──────────────────────────────────────────────")
	if _failed > 0:
		print("\nFailures:")
		for f in _failures:
			print("  • %s" % f)
	print("")
	get_tree().quit(1 if _failed > 0 else 0)
