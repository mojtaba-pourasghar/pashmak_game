#!/usr/bin/env bash
# One daily visit to the Gemini TTS quota.
#
# The free tier allows ten calls a day per project, so a run of 1,136 clips is
# not one sitting — it is a visit a day, taking whatever each key has freed up
# and moving on to the next the moment one refuses. That is what --drip is: no
# backoff ladder, because the next firing is tomorrow, not in eight seconds.
#
# Keys come from the environment and are never written down here:
#
#   export GEMINI_API_KEYS="key1 key2 key3"
#   bash tools/voice_drip.sh
#
# It commits and pushes only when a clip was actually written.
set -u
cd "$(dirname "$0")/.."

BRANCH="${VOICE_BRANCH:-main}"

if [ -z "${GEMINI_API_KEYS:-}${GEMINI_API_KEY:-}" ]; then
    echo "no key in the environment — set GEMINI_API_KEYS" >&2
    exit 2
fi

# scipy does the shaping, numpy the samples; a fresh container has neither.
python3 - <<'PY' || pip install --quiet numpy scipy
import numpy, scipy  # noqa: F401
PY

before=$(python3 -c "import json,io;print(len(json.load(io.open('tools/voice_done.json',encoding='utf-8'))['done']))")
python3 tools/gen_voice_aistudio.py --drip
after=$(python3 -c "import json,io;print(len(json.load(io.open('tools/voice_done.json',encoding='utf-8'))['done']))")

echo "spoken this visit: $((after - before));  $after of 1136 in all"

if [ "$after" -le "$before" ]; then
    echo "nothing new — leaving the tree alone"
    exit 0
fi

git add -A app/src/main/res/raw tools/voice_done.json
git -c user.name="Claude" -c user.email="noreply@anthropic.com" commit -q -m "$(cat <<MSG
$after of 1136 clips in Umbriel's voice

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
MSG
)"
for wait in 2 4 8 16 0; do
    git push -u origin "$BRANCH" && break
    [ "$wait" -eq 0 ] && { echo "push failed" >&2; exit 1; }
    sleep "$wait"
done
