#!/data/data/com.termux/files/usr/bin/bash
# Prints the kotlin stdlib jar that ships with the local kotlinc install.
set -euo pipefail
KOTLINC="$(command -v kotlinc)"
HOME_DIR="$(cd "$(dirname "$(readlink -f "$KOTLINC")")/.." && pwd)"
if [ -f "$HOME_DIR/lib/kotlin-stdlib.jar" ]; then
  echo "$HOME_DIR/lib/kotlin-stdlib.jar"
else
  find "$HOME_DIR" -maxdepth 3 -name 'kotlin-stdlib*.jar' ! -name '*sources*' 2>/dev/null | head -1
fi
