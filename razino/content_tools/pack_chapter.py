#!/usr/bin/env python3
"""Packs a chapter into a signed, downloadable content package.

    python3 content_tools/pack_chapter.py season_02 [--out dist] [--key PATH]

Produces, under <out>/<chapter_id>/:
    story.json        the chapter data
    <assets...>       whatever else the chapter folder holds
    manifest.json     file list with size and SHA-256 for every file,
                      plus an RSA-SHA256 signature over the manifest itself

The signature covers a canonical form of the manifest with the signature field
removed: keys sorted, no whitespace, whole numbers as integers. ContentManager
rebuilds exactly that string before verifying, which is why both sides have to
agree on how numbers are written.

The PRIVATE key never belongs in the repository. Pass --key, or set
RAZINO_CONTENT_SIGNING_KEY. See docs/BACKEND.md and env.example.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCHEMA_VERSION = 1
MIN_GAME_VERSION = "0.1.0"

# Anything that could be executed must never enter a package.
FORBIDDEN_SUFFIXES = {
    ".gd", ".gdc", ".gdns", ".so", ".dll", ".dylib",
    ".pck", ".exe", ".sh", ".jar", ".bat", ".py",
}


def canonical(value) -> str:
    """Must match ContentManager._canonical_json byte for byte."""
    if isinstance(value, dict):
        parts = [f"{json.dumps(str(k), ensure_ascii=False)}:{canonical(v)}"
                 for k, v in sorted(value.items())]
        return "{" + ",".join(parts) + "}"
    if isinstance(value, list):
        return "[" + ",".join(canonical(v) for v in value) + "]"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, float) and value.is_integer():
        return str(int(value))
    if value is None:
        return "null"
    return json.dumps(value, ensure_ascii=False)


def sign(payload: str, key_path: Path) -> str:
    """RSA-SHA256 over the canonical manifest, via openssl."""
    with tempfile.TemporaryDirectory() as tmp:
        data = Path(tmp) / "payload"
        data.write_text(payload, encoding="utf-8")
        sig = Path(tmp) / "sig"
        subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", str(key_path),
             "-out", str(sig), str(data)],
            check=True, capture_output=True,
        )
        return base64.b64encode(sig.read_bytes()).decode("ascii")


def collect(chapter_dir: Path) -> list[Path]:
    files = []
    for p in sorted(chapter_dir.rglob("*")):
        if not p.is_file() or p.name == "manifest.json":
            continue
        if p.suffix.lower() in FORBIDDEN_SUFFIXES:
            raise SystemExit(f"refusing to package executable file: {p}")
        files.append(p)
    return files


def pack(chapter_id: str, out_root: Path, key_path: Path | None,
         dependencies: list[dict] | None = None) -> Path:
    chapter_dir = ROOT / "content" / chapter_id
    if not (chapter_dir / "story.json").exists():
        raise SystemExit(f"{chapter_id} has no story.json")

    story = json.loads((chapter_dir / "story.json").read_text(encoding="utf-8"))
    out_dir = out_root / chapter_id
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)

    entries = []
    for src in collect(chapter_dir):
        rel = src.relative_to(chapter_dir).as_posix()
        dst = out_dir / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        blob = dst.read_bytes()
        entries.append({
            "path": rel,
            "size": len(blob),
            "sha256": hashlib.sha256(blob).hexdigest(),
        })

    manifest = {
        "chapter_id": chapter_id,
        "schema_version": SCHEMA_VERSION,
        "version": int(story.get("version", 1)),
        "minimum_game_version": story.get("minimum_game_version", MIN_GAME_VERSION),
        "title": story.get("title", chapter_id),
        "language": story.get("language", "fa-IR"),
        "dependencies": dependencies or [],
        "files": entries,
    }

    if key_path is not None:
        manifest["signature"] = sign(canonical(manifest), key_path)
    else:
        print(f"  ! {chapter_id} packaged UNSIGNED — only a local development "
              f"server may serve this", file=sys.stderr)

    (out_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    total = sum(e["size"] for e in entries)
    print(f"  {chapter_id}: {len(entries)} file(s), {total / 1024:.1f} KiB"
          f"{' , signed' if key_path else ' , UNSIGNED'}")
    return out_dir


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("chapters", nargs="*", help="chapter ids; default is every paid chapter")
    ap.add_argument("--out", default="dist", help="output directory (default: dist)")
    ap.add_argument("--key", default=os.environ.get("RAZINO_CONTENT_SIGNING_KEY"),
                    help="RSA private key PEM; or set RAZINO_CONTENT_SIGNING_KEY")
    args = ap.parse_args()

    key_path = Path(args.key) if args.key else None
    if key_path is not None and not key_path.exists():
        raise SystemExit(f"signing key not found: {key_path}")

    chapters = args.chapters
    if not chapters:
        chapters = [d.name for d in sorted((ROOT / "content").glob("season_*"))
                    if d.name != "season_01"]

    out_root = Path(args.out)
    if not out_root.is_absolute():
        out_root = ROOT / out_root
    print(f"packing {len(chapters)} chapter(s) into {out_root}")
    for cid in chapters:
        pack(cid, out_root, key_path)
    print("done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
