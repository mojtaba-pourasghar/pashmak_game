#!/usr/bin/env python3
"""Turns an authored chapter outline into a valid story.json.

The prose — every line of dialogue, every choice, every clue — is written by
hand in `chapters/*.py`. What this module generates is only the PLUMBING:
scene ids, line ids, audio ids, episode wiring and the next/branch links. That
split is deliberate: link errors are the failure a read-through misses, so
links are computed rather than typed, while nothing a player reads is machine
written.

Shape of an authored chapter, and the scene shorthands it may use, are
documented in docs/CONTENT_AUTHORING.md.
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


class ChapterBuilder:
    def __init__(self, spec: dict):
        self.spec = spec
        self.num = int(spec["id"].split("_")[1])
        self.sid = f"S{self.num:02d}"
        self.scenes: list[dict] = []
        self._line_counter: dict[str, int] = {}

    # — ids ———————————————————————————————————————————————

    def scene_id(self, ep: int, sc: int) -> str:
        return f"{self.sid}-E{ep:02d}-SC{sc:02d}"

    def episode_id(self, ep: int) -> str:
        return f"{self.sid}-E{ep:02d}"

    def next_line_id(self, ep: int) -> str:
        key = f"{self.sid}-E{ep:02d}"
        self._line_counter[key] = self._line_counter.get(key, 0) + 1
        return f"{key}-L{self._line_counter[key]:03d}"

    def audio_id(self, ep: int, speaker: str, n: int) -> str:
        return f"VO_{self.sid}_E{ep:02d}_{speaker.upper()}_{n:03d}"

    # — build —————————————————————————————————————————————

    def build(self) -> dict:
        spec = self.spec
        episodes = spec["episodes"]

        # Flat list of (episode_index, scene_index_within_episode, authored)
        plan: list[tuple[int, int, dict]] = []
        for ei, ep in enumerate(episodes, start=1):
            for si, sc in enumerate(ep["scenes"], start=1):
                plan.append((ei, si, sc))

        # Resolve the linear spine first so branches can name "next".
        ids = [self.scene_id(ei, si) for ei, si, _ in plan]
        ending_ids = [e[0] for e in spec["endings"]]

        # The ending scenes are the trailing run of scenes marked is_ending;
        # "ending:N" in an authored choice indexes into them.
        trailing = 0
        for _, _, authored in reversed(plan):
            if authored.get("is_ending"):
                trailing += 1
            else:
                break
        ending_scene_ids = ids[-trailing:] if trailing else []

        speaker_counts: dict[str, int] = {}

        for index, (ei, si, authored) in enumerate(plan):
            this_id = ids[index]
            follow = ids[index + 1] if index + 1 < len(ids) else ""
            scene = {
                "scene_id": this_id,
                "episode_id": self.episode_id(ei),
                "scene_type": authored["type"],
                "title": authored["title"],
                "background_id": authored["bg"],
                "music_id": authored.get("music", spec.get("default_music", "MUS_TOWN_DAY")),
            }

            if authored.get("on_enter"):
                scene["on_enter"] = authored["on_enter"]
            if authored.get("characters"):
                scene["characters"] = [
                    {"character_id": c[0], "position": c[1], "animation": c[2]}
                    for c in authored["characters"]
                ]

            if authored.get("lines"):
                dialogue = []
                for speaker, text, anim in authored["lines"]:
                    speaker_counts[speaker] = speaker_counts.get(speaker, 0) + 1
                    dialogue.append({
                        "line_id": self.next_line_id(ei),
                        "speaker_id": speaker,
                        "text": text,
                        "audio_id": self.audio_id(ei, speaker, speaker_counts[speaker]),
                        "animation": anim,
                    })
                scene["dialogue"] = dialogue

            if authored.get("interactions"):
                scene["interactions"] = []
                for it in authored["interactions"]:
                    entry = {
                        "interaction_id": it["id"],
                        "kind": it["kind"],
                        "label": it["label"],
                        "description": it.get("desc", ""),
                        "rect": it.get("rect", [0.4, 0.45, 0.2, 0.24]),
                    }
                    if it.get("once", True):
                        entry["once"] = True
                    if it.get("conditions"):
                        entry["conditions"] = it["conditions"]
                    if it.get("effects"):
                        entry["effects"] = it["effects"]
                    scene["interactions"].append(entry)

            if authored.get("puzzle"):
                p = dict(authored["puzzle"])
                p.setdefault("allow_skip_after_failures", 4)
                p["next_scene_id"] = follow
                p["skip_scene_id"] = follow
                scene["puzzle"] = p

            if authored.get("cinematic"):
                scene["cinematic"] = {
                    "cinematic_id": f"CIN_{self.sid}_E{ei:02d}",
                    "skippable": True,
                    "beats": [
                        {
                            "text": t,
                            "audio_id": self.audio_id(ei, "nar", bi + 1),
                            "background_id": authored["bg"],
                            "seconds": 4.5,
                        }
                        for bi, t in enumerate(authored["cinematic"])
                    ],
                }

            if authored.get("choices"):
                scene["choices"] = []
                for ci, ch in enumerate(authored["choices"], start=1):
                    target = ch["to"]
                    if target == "next":
                        dest = follow
                    elif target == "self":
                        dest = this_id
                    elif target.startswith("ending:"):
                        dest = ending_scene_ids[int(target.split(":")[1])]
                    elif target.startswith("@"):
                        # "@ep.scene" — an explicit jump inside this chapter.
                        ep_s, sc_s = target[1:].split(".")
                        dest = self.scene_id(int(ep_s), int(sc_s))
                    else:
                        dest = target
                    entry = {
                        "choice_id": f"CH_{self.sid}_E{ei:02d}S{si:02d}_{ci}",
                        "text": ch["text"],
                        "next_scene_id": dest,
                    }
                    if ch.get("hint"):
                        entry["hint"] = ch["hint"]
                    if ch.get("conditions"):
                        entry["conditions"] = ch["conditions"]
                    if ch.get("effects"):
                        entry["effects"] = ch["effects"]
                    scene["choices"].append(entry)
            elif authored.get("is_ending"):
                scene["is_ending"] = True
            elif authored["type"] not in ("puzzle",):
                if follow:
                    scene["next_scene_id"] = follow

            if authored.get("is_ending"):
                scene["is_ending"] = True
                scene.pop("next_scene_id", None)

            self.scenes.append(scene)

        return {
            "schema_version": 1,
            "chapter_id": spec["id"],
            "version": 1,
            "title": spec["title"],
            "subtitle": spec["subtitle"],
            "language": "fa-IR",
            "minimum_game_version": "0.1.0",
            "synopsis": spec["synopsis"],
            "narrative_goal": spec["goal"],
            "macro_clue": spec["macro_clue"],
            "primary_environment": spec["environment"],
            "core_mechanic": spec["mechanic"],
            "start_scene_id": ids[0],
            "default_ending_id": ending_ids[0],
            "episodes": [
                {
                    "episode_id": self.episode_id(i),
                    "title": ep["title"],
                    "summary": ep["summary"],
                }
                for i, ep in enumerate(episodes, start=1)
            ],
            "characters": spec["characters"],
            "clues": [
                dict(zip(("clue_id", "title", "body", "category"), c))
                if len(c) == 4 else
                {**dict(zip(("clue_id", "title", "body", "category"), c[:4])), **c[4]}
                for c in spec["clues"]
            ],
            "clue_combinations": [
                {"a": a, "b": b, "result": r} for a, b, r in spec.get("combos", [])
            ],
            "endings": [
                {"ending_id": e[0], "title": e[1], "body": e[2]} for e in spec["endings"]
            ],
            "scenes": self.scenes,
        }


def write_chapter(spec: dict) -> Path:
    data = ChapterBuilder(spec).build()
    out = ROOT / "content" / spec["id"] / "story.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return out
