#!/usr/bin/env python3
"""Rebuilds every generated chapter and the content catalogue.

    python3 content_tools/build_all.py

Chapter 1 is hand-written JSON and is not regenerated — it is the reference
a chapter is checked against. Chapters 2–10 are emitted from the authored
outlines in content_tools/chapters/.
"""

from __future__ import annotations

import importlib
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "content_tools"))

from build_chapter import write_chapter  # noqa: E402

GENERATED = [f"ch{n:02d}" for n in range(2, 11)]

# Chapter 1 is free; everything after it is a purchase, per GDD §11.
FREE_CHAPTERS = {"season_01"}


def build_chapters() -> list[Path]:
    written = []
    for mod_name in GENERATED:
        mod = importlib.import_module(f"chapters.{mod_name}")
        written.append(write_chapter(mod.SPEC))
    return written


def build_catalog() -> Path:
    chapters = []
    for d in sorted((ROOT / "content").glob("season_*")):
        story = d / "story.json"
        if not story.exists():
            continue
        data = json.loads(story.read_text(encoding="utf-8"))
        cid = data["chapter_id"]
        chapters.append({
            "chapter_id": cid,
            "version": data.get("version", 1),
            "title": data["title"],
            "subtitle": data.get("subtitle", ""),
            "synopsis": data.get("synopsis", ""),
            "language": data["language"],
            "minimum_game_version": data.get("minimum_game_version", "0.1.0"),
            "access_type": "free" if cid in FREE_CHAPTERS else "paid",
            "primary_environment": data.get("primary_environment", ""),
            "core_mechanic": data.get("core_mechanic", ""),
            "macro_clue": data.get("macro_clue", ""),
            "scene_count": len(data.get("scenes", [])),
            "episode_count": len(data.get("episodes", [])),
            "content_manifest": f"{cid}/manifest.json",
            # Bundled chapters ship inside the APK; the rest are fetched.
            "delivery": "bundled" if cid == "season_01" else "download",
        })
    out = ROOT / "content" / "manifest" / "catalog.json"
    out.write_text(json.dumps({
        "catalog_version": 1,
        "schema_version": 1,
        "generated_for": "razino 0.1.0",
        "chapters": chapters,
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return out


def main() -> int:
    written = build_chapters()
    print(f"rebuilt {len(written)} chapter(s)")
    cat = build_catalog()
    data = json.loads(cat.read_text(encoding="utf-8"))
    print(f"catalog: {len(data['chapters'])} chapters -> {cat.relative_to(ROOT)}")
    total_scenes = sum(c["scene_count"] for c in data["chapters"])
    print(f"total scenes across all chapters: {total_scenes}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
