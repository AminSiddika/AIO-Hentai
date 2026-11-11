#!/usr/bin/env bash
# Provider-level smoke test: runs CloudStream's MainAPI entry points through the adapter.
#   ./scripts/provider_smoke.sh WatchHentai HentaiCity
#   ./scripts/provider_smoke.sh --genres
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${CS3_CACHE:-$HOME/.cache/cs3}"
CLASSES="$CACHE/typecheck-out"
STUB_JAR="$CACHE/android-stub.jar"
KOTLINC="${KOTLINC:-$(command -v kotlinc || true)}"

[ -d "$CLASSES" ] || { echo "run scripts/typecheck.sh first" >&2; exit 1; }
[ -n "$KOTLINC" ] || { echo "kotlinc not found; install Kotlin or set KOTLINC" >&2; exit 1; }

python3 "$ROOT/scripts/gen_smoke.py"

CP="$CLASSES:$CACHE/cloudstream-classes.jar:$STUB_JAR"
for jar in "$CACHE"/libs/*.jar; do CP="$CP:$jar"; done
CP="$CP:$(bash "$ROOT/scripts/print_kotlin_lib.sh")"

mkdir -p "$CACHE/provider-out"
"$KOTLINC" -nowarn -jvm-target 1.8 -classpath "$CP" -d "$CACHE/provider-out" \
  "$ROOT"/scripts/ProviderSmoke.kt "$ROOT"/scripts/SmokeSites.kt 2>&1 | grep -v "^warning:" || true

java -cp "$CACHE/provider-out:$CP" com.aiohentai.cs.smoke.ProviderSmokeKt "$@"
