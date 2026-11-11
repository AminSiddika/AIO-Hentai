#!/usr/bin/env bash
# Prints the kotlin stdlib jar that ships with the local kotlinc install.
set -euo pipefail

KOTLINC="${KOTLINC:-$(command -v kotlinc || true)}"
if [ -z "$KOTLINC" ]; then
  echo "kotlinc not found; install Kotlin or set KOTLINC" >&2
  exit 1
fi

# Resolve symlinks without relying on GNU readlink -f (absent on macOS).
KOTLINC="$(python3 -c 'import os,sys; print(os.path.realpath(sys.argv[1]))' "$KOTLINC")"
HOME_DIR="$(cd "$(dirname "$KOTLINC")/.." && pwd)"

if [ -f "$HOME_DIR/lib/kotlin-stdlib.jar" ]; then
  echo "$HOME_DIR/lib/kotlin-stdlib.jar"
else
  find "$HOME_DIR" -maxdepth 3 -name 'kotlin-stdlib*.jar' ! -name '*sources*' 2>/dev/null | head -1
fi
