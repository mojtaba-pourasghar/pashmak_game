class_name Fa
extends RefCounted
## Persian number formatting.
##
## A Persian game for children should not show Latin digits next to Persian
## words — a ten-year-old reading «۱۲ صحنه» should not have to switch scripts
## mid-sentence. Every number the player sees goes through here.

const LATIN := ["0", "1", "2", "3", "4", "5", "6", "7", "8", "9"]
const PERSIAN := ["۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹"]

## Converts the digits in a string, leaving everything else alone.
static func digits(text: String) -> String:
	var out := text
	for i in 10:
		out = out.replace(LATIN[i], PERSIAN[i])
	return out

static func num(value: int) -> String:
	return digits(str(value))

static func percent(ratio: float) -> String:
	return digits("%d" % int(round(clampf(ratio, 0.0, 1.0) * 100.0))) + "٪"

## One decimal place, for storage sizes.
static func decimal(value: float, places: int = 1) -> String:
	return digits(String.num(value, places))
