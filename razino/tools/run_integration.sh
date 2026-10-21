#!/usr/bin/env bash
# End-to-end content delivery test.
#
#   tools/run_integration.sh [path-to-signing-key]
#
# Packs chapters, builds a deliberately tampered copy alongside them, serves
# both over loopback, and runs the Godot integration scene against it.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEY="${1:-${RAZINO_CONTENT_SIGNING_KEY:-}}"
DIST="${RAZINO_DIST:-$HERE/dist}"
PORT="${RAZINO_PORT:-8777}"

if [[ -z "$KEY" || ! -f "$KEY" ]]; then
  echo "error: pass the RSA signing key as \$1 or set RAZINO_CONTENT_SIGNING_KEY" >&2
  exit 2
fi

rm -rf "$DIST"
python3 "$HERE/content_tools/pack_chapter.py" season_02 season_03 --key "$KEY" --out "$DIST"

# A copy with one byte of story.json changed, manifest left as it was. Its
# recorded hash no longer matches, which is exactly the corruption the
# validator has to catch.
mkdir -p "$DIST/tampered"
cp -r "$DIST/season_02" "$DIST/tampered/season_02"
printf ' ' >> "$DIST/tampered/season_02/story.json"
echo "prepared a tampered copy at $DIST/tampered/season_02"

python3 "$HERE/content_tools/serve_content.py" --dir "$DIST" --port "$PORT" &
SERVER_PID=$!
trap 'kill "$SERVER_PID" 2>/dev/null || true' EXIT

for _ in $(seq 1 40); do
  if curl -sf "http://127.0.0.1:$PORT/season_02/manifest.json" -o /dev/null --noproxy '*'; then break; fi
  sleep 0.25
done

godot --headless --path "$HERE" res://tests/integration_download.tscn
