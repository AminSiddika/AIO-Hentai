#!/data/data/com.termux/files/usr/bin/bash
# Local typecheck of the plugin sources without the Android SDK.
# Compiles against CloudStream's classes.jar + the runtime deps + desktop Android stubs.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${CS3_CACHE:-$HOME/.cache/cs3}"
STUB_SRC="/data/data/com.termux/files/usr/tmp/opencode/android-stub"
STUB_JAR="$CACHE/android-stub.jar"

if [ ! -f "$CACHE/cloudstream-classes.jar" ]; then
  echo "downloading test environment into $CACHE"
  "$(dirname "$0")/setup_env.sh"
fi

if [ ! -f "$STUB_JAR" ] || [ -n "$(find "$STUB_SRC" -newer "$STUB_JAR" -name '*.java' 2>/dev/null)" ]; then
  rm -rf "$CACHE/stub-classes"
  mkdir -p "$CACHE/stub-classes"
  javac -d "$CACHE/stub-classes" $(find "$STUB_SRC" -name '*.java')
  (cd "$CACHE/stub-classes" && jar cf "$STUB_JAR" android)
fi

CP="$CACHE/cloudstream-classes.jar:$STUB_JAR"
for jar in "$CACHE"/libs/*.jar; do CP="$CP:$jar"; done

SRC="$ROOT/AIO-Hentai/src/main/kotlin"
OUT="$CACHE/typecheck-out"
LOG="$CACHE/typecheck.log"
rm -rf "$OUT"
mkdir -p "$OUT"

set +e
kotlinc -nowarn -jvm-target 1.8 -classpath "$CP" -d "$OUT" \
  $(find "$SRC" -name '*.kt' | sort) >"$LOG" 2>&1
status=$?
set -e

if [ $status -ne 0 ]; then
  echo "typecheck FAILED"
  grep -E "error:" "$LOG" | head -40
  echo
  echo "full log: $LOG"
  exit $status
fi

echo "typecheck ok -> $OUT"
