"""Cast and shared helpers used by every authored chapter."""

CAST = [
    {"character_id": "player", "name": "تو", "role": "عضو تازهٔ گروه"},
    {"character_id": "arad", "name": "آراد", "role": "کنجکاو و ماجراجو"},
    {"character_id": "raha", "name": "رها", "role": "تحلیل‌گر گروه"},
    {"character_id": "mani", "name": "مانی", "role": "مخترع کوچک"},
    {"character_id": "fandogh", "name": "فندق", "role": "راهنمای بازی"},
]


def cast_with(*extra) -> list[dict]:
    """The four companions plus this chapter's own guests."""
    return CAST + [
        {"character_id": cid, "name": name, "role": role}
        for cid, name, role in extra
    ]


def clue(cid, title, body, category, **extra):
    return (cid, title, body, category, extra) if extra else (cid, title, body, category)


def group(*ids):
    """Standard staging for the companions present in a scene."""
    slots = ["right", "left", "far_left", "far_right"]
    return [(cid, slots[i % len(slots)], "idle") for i, cid in enumerate(ids)]
