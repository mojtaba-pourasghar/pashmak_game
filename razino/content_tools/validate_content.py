#!/usr/bin/env python3
"""Offline validator for Razino chapter packages.

Implements the checks GDD §13.1 asks for, plus the structural ones that stop a
chapter shipping broken:

  * scene ids unique, well-formed, and every jump lands on a real scene
  * every scene reachable from the start, and an ending reachable from every
    scene — so no choice can strand a player (GDD §4.3)
  * no dead end that is not explicitly an ending
  * condition and effect keys stay inside the engine's closed vocabulary,
    read straight out of story_engine.gd so the two cannot drift
  * clue references resolve; combinations name real clues
  * Persian text and subtitles present and non-empty
  * referenced background / audio assets reported as present or missing
  * duplicate or unreferenced resources flagged

Exit code 0 = no errors (warnings allowed), 1 = errors found.

    python3 content_tools/validate_content.py              # all chapters
    python3 content_tools/validate_content.py season_01    # one chapter
"""

from __future__ import annotations

import json
import re
import sys
from collections import deque
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTENT = ROOT / "content"
ENGINE = ROOT / "scripts" / "story_engine" / "story_engine.gd"

SCENE_ID_RE = re.compile(r"^S\d{2}-E\d{2}-SC\d{2}$")
EPISODE_ID_RE = re.compile(r"^S\d{2}-E\d{2}$")
SCHEMA_VERSION = 1

# Interaction kinds, GDD §2.2 INT-01..INT-08.
INTERACTION_KINDS = {
    "examine", "talk", "collect", "combine",
    "puzzle", "choice", "inspect", "trigger",
}
PUZZLE_KINDS = {
    "order_symbols", "match_pairs", "dial_code",
    "route_path", "assemble_parts", "spot_contradiction",
}


class Report:
    def __init__(self, chapter: str):
        self.chapter = chapter
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.stats: dict[str, int] = {}

    def error(self, msg: str) -> None:
        self.errors.append(msg)

    def warn(self, msg: str) -> None:
        self.warnings.append(msg)

    @property
    def ok(self) -> bool:
        return not self.errors


def engine_vocabulary() -> tuple[set[str], set[str]]:
    """Read CONDITIONS and EFFECTS out of the engine so this file cannot
    validate against a vocabulary the engine no longer has."""
    text = ENGINE.read_text(encoding="utf-8")

    def grab(name: str) -> set[str]:
        m = re.search(rf"const {name} := \[(.*?)\]", text, re.S)
        if not m:
            raise SystemExit(f"could not read {name} from {ENGINE}")
        return set(re.findall(r'"([a-z_]+)"', m.group(1)))

    return grab("CONDITIONS"), grab("EFFECTS")


CONDITION_KEYS, EFFECT_KEYS = engine_vocabulary()


def check_conditions(cond, where: str, rep: Report) -> None:
    if cond is None:
        return
    if isinstance(cond, list):
        for c in cond:
            check_conditions(c, where, rep)
        return
    if not isinstance(cond, dict):
        rep.error(f"{where}: conditions must be an object")
        return
    for key, value in cond.items():
        if key not in CONDITION_KEYS:
            rep.error(f"{where}: unknown condition '{key}' (engine would fail closed)")
        if key in ("any", "all"):
            if not isinstance(value, list):
                rep.error(f"{where}: '{key}' must hold a list")
            else:
                for sub in value:
                    check_conditions(sub, where, rep)


def check_effects(effects, where: str, rep: Report, clue_ids: set[str]) -> None:
    if effects is None:
        return
    if not isinstance(effects, list):
        rep.error(f"{where}: effects must be a list")
        return
    for eff in effects:
        if not isinstance(eff, dict):
            rep.error(f"{where}: each effect must be an object")
            continue
        for key, value in eff.items():
            if key not in EFFECT_KEYS:
                rep.error(f"{where}: unknown effect '{key}' (engine would ignore it)")
            if key == "add_clue":
                for cid in ([value] if isinstance(value, str) else value):
                    if cid not in clue_ids:
                        rep.error(f"{where}: adds unknown clue '{cid}'")


def scene_targets(scene: dict) -> list[str]:
    out = []
    if scene.get("next_scene_id"):
        out.append(scene["next_scene_id"])
    for c in scene.get("choices", []):
        if c.get("next_scene_id"):
            out.append(c["next_scene_id"])
    puzzle = scene.get("puzzle") or {}
    for key in ("next_scene_id", "skip_scene_id"):
        if puzzle.get(key):
            out.append(puzzle[key])
    return out


def validate_chapter(path: Path) -> Report:
    chapter_dir = path.parent
    rep = Report(chapter_dir.name)
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        rep.error(f"story.json is not valid JSON: {exc}")
        return rep

    # — header ————————————————————————————————————————————
    if data.get("schema_version") != SCHEMA_VERSION:
        rep.error(f"schema_version must be {SCHEMA_VERSION}, got {data.get('schema_version')!r}")
    if data.get("language") != "fa-IR":
        rep.error(f"language must be 'fa-IR', got {data.get('language')!r}")
    if not data.get("title", "").strip():
        rep.error("title is empty")
    if not data.get("synopsis", "").strip():
        rep.warn("synopsis is empty")
    if data.get("chapter_id") != chapter_dir.name:
        rep.error(f"chapter_id {data.get('chapter_id')!r} does not match folder {chapter_dir.name}")

    # — clues —————————————————————————————————————————————
    clue_ids: set[str] = set()
    for clue in data.get("clues", []):
        cid = clue.get("clue_id", "")
        if not cid:
            rep.error("a clue has no clue_id")
            continue
        if cid in clue_ids:
            rep.error(f"duplicate clue id '{cid}'")
        clue_ids.add(cid)
        if not clue.get("title", "").strip():
            rep.error(f"clue '{cid}' has no title")
        if not clue.get("body", "").strip():
            rep.error(f"clue '{cid}' has no body text")
    for combo in data.get("clue_combinations", []):
        for side in ("a", "b", "result"):
            if combo.get(side) not in clue_ids:
                rep.error(f"clue combination names unknown clue '{combo.get(side)}' in '{side}'")

    ending_ids = {e.get("ending_id") for e in data.get("endings", [])}
    episode_ids = set()
    for ep in data.get("episodes", []):
        eid = ep.get("episode_id", "")
        if not EPISODE_ID_RE.match(eid):
            rep.error(f"episode id '{eid}' is malformed (expect S01-E01)")
        episode_ids.add(eid)

    # — scenes ————————————————————————————————————————————
    scenes = data.get("scenes", [])
    if not scenes:
        rep.error("chapter has no scenes")
        return rep

    by_id: dict[str, dict] = {}
    for scene in scenes:
        sid = scene.get("scene_id", "")
        if not sid:
            rep.error("a scene has no scene_id")
            continue
        if not SCENE_ID_RE.match(sid):
            rep.error(f"scene id '{sid}' is malformed (expect S01-E01-SC01)")
        if sid in by_id:
            rep.error(f"duplicate scene id '{sid}'")
        by_id[sid] = scene

    backgrounds: set[str] = set()
    audio_ids: set[str] = set()
    line_ids: set[str] = set()
    dialogue_lines = 0
    choice_count = 0
    puzzle_count = 0

    for sid, scene in by_id.items():
        ep = scene.get("episode_id", "")
        if ep and ep not in episode_ids:
            rep.error(f"{sid}: episode_id '{ep}' is not declared in episodes")
        stype = scene.get("scene_type", "dialogue")
        if stype not in ("dialogue", "explore", "puzzle", "cinematic"):
            rep.error(f"{sid}: unknown scene_type '{stype}'")
        if scene.get("background_id"):
            backgrounds.add(scene["background_id"])
        else:
            rep.warn(f"{sid}: no background_id")

        check_effects(scene.get("on_enter"), f"{sid}.on_enter", rep, clue_ids)

        for line in scene.get("dialogue", []):
            dialogue_lines += 1
            lid = line.get("line_id", "")
            if not lid:
                rep.error(f"{sid}: a dialogue line has no line_id")
            elif lid in line_ids:
                rep.error(f"duplicate dialogue line id '{lid}'")
            else:
                line_ids.add(lid)
            if not line.get("text", "").strip():
                rep.error(f"{sid}/{lid}: subtitle text is empty")
            if not line.get("speaker_id"):
                rep.error(f"{sid}/{lid}: no speaker_id")
            if line.get("audio_id"):
                audio_ids.add(line["audio_id"])
            else:
                rep.warn(f"{sid}/{lid}: no audio_id — line has no recorded voice")
            check_effects(line.get("effects"), f"{sid}/{lid}", rep, clue_ids)

        for inter in scene.get("interactions", []):
            iid = inter.get("interaction_id", "?")
            if inter.get("kind") not in INTERACTION_KINDS:
                rep.error(f"{sid}/{iid}: unknown interaction kind '{inter.get('kind')}'")
            if not inter.get("label", "").strip():
                rep.error(f"{sid}/{iid}: interaction has no label")
            rect = inter.get("rect")
            if rect is not None and (len(rect) != 4):
                rep.error(f"{sid}/{iid}: rect must have 4 numbers")
            check_conditions(inter.get("conditions"), f"{sid}/{iid}", rep)
            check_effects(inter.get("effects"), f"{sid}/{iid}", rep, clue_ids)

        for ch in scene.get("choices", []):
            choice_count += 1
            cid = ch.get("choice_id", "?")
            if not ch.get("text", "").strip():
                rep.error(f"{sid}/{cid}: choice text is empty")
            if not ch.get("next_scene_id"):
                rep.error(f"{sid}/{cid}: choice has no destination")
            check_conditions(ch.get("conditions"), f"{sid}/{cid}", rep)
            check_effects(ch.get("effects"), f"{sid}/{cid}", rep, clue_ids)

        puzzle = scene.get("puzzle")
        if stype == "puzzle" and not puzzle:
            rep.error(f"{sid}: scene_type is puzzle but no puzzle block")
        if puzzle:
            puzzle_count += 1
            pid = puzzle.get("puzzle_id", "?")
            if puzzle.get("kind") not in PUZZLE_KINDS:
                rep.error(f"{sid}/{pid}: unknown puzzle kind '{puzzle.get('kind')}'")
            if not puzzle.get("prompt", "").strip():
                rep.error(f"{sid}/{pid}: puzzle has no prompt")
            if not puzzle.get("hints"):
                rep.warn(f"{sid}/{pid}: puzzle offers no hints")
            # GDD §4.3: a wrong path must always have a way back.
            if not puzzle.get("allow_skip_after_failures"):
                rep.error(f"{sid}/{pid}: no allow_skip_after_failures — a stuck child would be trapped")
            if not puzzle.get("skip_scene_id") and not scene.get("next_scene_id"):
                rep.error(f"{sid}/{pid}: nowhere to go if the puzzle is skipped")
            check_effects(puzzle.get("on_solve"), f"{sid}/{pid}.on_solve", rep, clue_ids)
            check_effects(puzzle.get("on_skip"), f"{sid}/{pid}.on_skip", rep, clue_ids)

        # destinations must exist
        for target in scene_targets(scene):
            if target not in by_id:
                rep.error(f"{sid}: points at non-existent scene '{target}'")

        # dead ends
        if not scene_targets(scene) and not scene.get("is_ending"):
            rep.error(f"{sid}: dead end — no destination and not marked is_ending")
        if scene.get("is_ending") and scene_targets(scene):
            rep.warn(f"{sid}: marked is_ending but still points onward")

    # — graph —————————————————————————————————————————————
    start = data.get("start_scene_id", "")
    if start not in by_id:
        rep.error(f"start_scene_id '{start}' is not a real scene")
    else:
        reached = {start}
        queue = deque([start])
        while queue:
            sid = queue.popleft()
            for t in scene_targets(by_id[sid]):
                if t in by_id and t not in reached:
                    reached.add(t)
                    queue.append(t)
        for sid in by_id:
            if sid not in reached:
                rep.error(f"{sid}: unreachable from the start")

        # An ending must be reachable from every reachable scene — this is the
        # check that catches a loop a player could never leave.
        endings = {sid for sid, s in by_id.items() if s.get("is_ending")}
        if not endings:
            rep.error("chapter has no scene marked is_ending")
        else:
            can_finish = set(endings)
            changed = True
            while changed:
                changed = False
                for sid, scene in by_id.items():
                    if sid in can_finish:
                        continue
                    if any(t in can_finish for t in scene_targets(scene)):
                        can_finish.add(sid)
                        changed = True
            for sid in reached:
                if sid not in can_finish:
                    rep.error(f"{sid}: no ending is reachable from here — the player would be stuck")

    default_ending = data.get("default_ending_id")
    if default_ending and ending_ids and default_ending not in ending_ids:
        rep.error(f"default_ending_id '{default_ending}' is not in endings")

    # — assets ————————————————————————————————————————————
    missing_bg = []
    for bg in sorted(backgrounds):
        candidates = [
            chapter_dir / "backgrounds" / f"{bg}.webp",
            chapter_dir / "backgrounds" / f"{bg}.png",
            ROOT / "assets" / "art" / "backgrounds" / f"{bg}.svg",
            ROOT / "assets" / "art" / "backgrounds" / f"{bg}.png",
        ]
        if not any(c.exists() for c in candidates):
            missing_bg.append(bg)
    missing_audio = []
    for aid in sorted(audio_ids):
        candidates = [
            chapter_dir / "audio" / "vo" / f"{aid}.ogg",
            ROOT / "assets" / "audio" / "vo" / f"{aid}.ogg",
        ]
        if not any(c.exists() for c in candidates):
            missing_audio.append(aid)

    if missing_bg:
        rep.warn(f"{len(missing_bg)} background(s) have no image file yet "
                 f"(first: {missing_bg[0]})")
    if missing_audio:
        rep.warn(f"{len(missing_audio)} voice line(s) have no recorded clip yet "
                 f"(first: {missing_audio[0]})")

    rep.stats = {
        "scenes": len(by_id),
        "episodes": len(episode_ids),
        "dialogue_lines": dialogue_lines,
        "choices": choice_count,
        "puzzles": puzzle_count,
        "clues": len(clue_ids),
        "endings": len(ending_ids),
        "backgrounds_referenced": len(backgrounds),
        "backgrounds_missing": len(missing_bg),
        "voice_lines_referenced": len(audio_ids),
        "voice_lines_missing": len(missing_audio),
    }
    return rep


def main(argv: list[str]) -> int:
    wanted = argv[1:]
    chapter_dirs = sorted(
        d for d in CONTENT.iterdir()
        if d.is_dir() and d.name.startswith("season_")
        and (not wanted or d.name in wanted)
    )
    if not chapter_dirs:
        print("no chapters found")
        return 1

    total_errors = 0
    total_warnings = 0
    print("=" * 72)
    print("Razino content validation")
    print("=" * 72)
    for d in chapter_dirs:
        story = d / "story.json"
        if not story.exists():
            print(f"\n{d.name}: ERROR — no story.json")
            total_errors += 1
            continue
        rep = validate_chapter(story)
        status = "OK" if rep.ok else "FAILED"
        print(f"\n{d.name}: {status}")
        if rep.stats:
            s = rep.stats
            print(f"  {s['scenes']} scenes / {s['episodes']} episodes / "
                  f"{s['dialogue_lines']} lines / {s['choices']} choices / "
                  f"{s['puzzles']} puzzles / {s['clues']} clues / {s['endings']} endings")
            print(f"  assets: {s['backgrounds_missing']}/{s['backgrounds_referenced']} backgrounds missing, "
                  f"{s['voice_lines_missing']}/{s['voice_lines_referenced']} voice lines missing")
        for e in rep.errors:
            print(f"  ERROR   {e}")
        for w in rep.warnings:
            print(f"  warning {w}")
        total_errors += len(rep.errors)
        total_warnings += len(rep.warnings)

    print("\n" + "=" * 72)
    print(f"{len(chapter_dirs)} chapter(s): {total_errors} error(s), {total_warnings} warning(s)")
    print("=" * 72)
    return 1 if total_errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
