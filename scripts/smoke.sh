#!/data/data/com.termux/files/usr/bin/bash
# JVM smoke test of the ported scrapers against the live sites.
#   ./scripts/smoke.sh                  # every site, home/details/episodes
#   ./scripts/smoke.sh --links          # also resolve video links
#   ./scripts/smoke.sh WatchHentai ...  # specific sources
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${CS3_CACHE:-$HOME/.cache/cs3}"
CLASSES="$CACHE/typecheck-out"
STUB_JAR="$CACHE/android-stub.jar"

if [ ! -d "$CLASSES" ]; then
  echo "run scripts/typecheck.sh first" >&2
  exit 1
fi

python3 "$ROOT/scripts/gen_smoke.py"

CP="$CLASSES:$CACHE/cloudstream-classes.jar:$STUB_JAR"
for jar in "$CACHE"/libs/*.jar; do CP="$CP:$jar"; done

KOTLIN_STDLIB="$("$ROOT"/scripts/print_kotlin_lib.sh 2>/dev/null || true)"
if [ -n "$KOTLIN_STDLIB" ]; then CP="$CP:$KOTLIN_STDLIB"; fi

mkdir -p "$CACHE/smoke-out"
kotlinc -nowarn -jvm-target 1.8 -classpath "$CP" -d "$CACHE/smoke-out" \
  "$ROOT"/scripts/Smoke.kt "$ROOT"/scripts/SmokeSites.kt 2>&1 | grep -v "^warning:" || true

java -cp "$CACHE/smoke-out:$CP" com.aiohentai.cs.smoke.SmokeKt "$@"
