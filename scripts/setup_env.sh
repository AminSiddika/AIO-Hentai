#!/usr/bin/env bash
# Downloads everything the offline typecheck / smoke tests need:
#   - CloudStream's plugin API (classes.jar from the pre-release build)
#   - the runtime libraries the scrapers use
#   - desktop stubs for the Android classes they touch (see scripts/android-stub)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${CS3_CACHE:-$HOME/.cache/cs3}"
mkdir -p "$CACHE/libs"

fetch() {
  local url="$1" out="$2"
  if [ -s "$out" ]; then
    return
  fi
  echo "fetch $(basename "$out")"
  if command -v curl >/dev/null 2>&1 && curl -sfL --connect-timeout 20 -o "$out.part" "$url"; then
    mv "$out.part" "$out"
    return
  fi
  # Some environments resolve DNS for Python but not for curl.
  python3 - "$url" "$out" <<'PY'
import os, sys, urllib.request
url, out = sys.argv[1], sys.argv[2]
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req, timeout=120) as r, open(out + ".part", "wb") as f:
    f.write(r.read())
os.replace(out + ".part", out)
PY
}

fetch "https://github.com/recloudstream/cloudstream/releases/download/pre-release/classes.jar" \
  "$CACHE/cloudstream-classes.jar"

M="https://repo1.maven.org/maven2"
fetch "$M/org/jsoup/jsoup/1.22.2/jsoup-1.22.2.jar" "$CACHE/libs/jsoup-1.22.2.jar"
fetch "$M/org/jspecify/jspecify/1.0.0/jspecify-1.0.0.jar" "$CACHE/libs/jspecify-1.0.0.jar"
fetch "$M/com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.jar" "$CACHE/libs/okhttp-4.12.0.jar"
fetch "$M/com/squareup/okio/okio-jvm/3.6.0/okio-jvm-3.6.0.jar" "$CACHE/libs/okio-jvm-3.6.0.jar"
fetch "$M/org/nanohttpd/nanohttpd/2.3.1/nanohttpd-2.3.1.jar" "$CACHE/libs/nanohttpd-2.3.1.jar"
fetch "$M/org/json/json/20240303/json-20240303.jar" "$CACHE/libs/json-20240303.jar"
fetch "$M/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.10.2/kotlinx-coroutines-core-jvm-1.10.2.jar" \
  "$CACHE/libs/kotlinx-coroutines-core-jvm-1.10.2.jar"

# CloudStream's own runtime deps, needed to run providers on the JVM (not to typecheck)
fetch "$M/com/fasterxml/jackson/core/jackson-databind/2.17.2/jackson-databind-2.17.2.jar" "$CACHE/libs/jackson-databind-2.17.2.jar"
fetch "$M/com/fasterxml/jackson/core/jackson-core/2.17.2/jackson-core-2.17.2.jar" "$CACHE/libs/jackson-core-2.17.2.jar"
fetch "$M/com/fasterxml/jackson/core/jackson-annotations/2.17.2/jackson-annotations-2.17.2.jar" "$CACHE/libs/jackson-annotations-2.17.2.jar"
fetch "$M/com/fasterxml/jackson/module/jackson-module-kotlin/2.17.2/jackson-module-kotlin-2.17.2.jar" "$CACHE/libs/jackson-module-kotlin-2.17.2.jar"
fetch "$M/org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/1.7.3/kotlinx-serialization-json-jvm-1.7.3.jar" "$CACHE/libs/kotlinx-serialization-json-jvm-1.7.3.jar"
fetch "$M/org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/1.7.3/kotlinx-serialization-core-jvm-1.7.3.jar" "$CACHE/libs/kotlinx-serialization-core-jvm-1.7.3.jar"
fetch "$M/io/ktor/ktor-http-jvm/3.0.3/ktor-http-jvm-3.0.3.jar" "$CACHE/libs/ktor-http-jvm-3.0.3.jar"
fetch "$M/io/ktor/ktor-utils-jvm/3.0.3/ktor-utils-jvm-3.0.3.jar" "$CACHE/libs/ktor-utils-jvm-3.0.3.jar"
fetch "$M/io/ktor/ktor-io-jvm/3.0.3/ktor-io-jvm-3.0.3.jar" "$CACHE/libs/ktor-io-jvm-3.0.3.jar"
fetch "$M/org/jetbrains/kotlinx/kotlinx-io-core-jvm/0.5.4/kotlinx-io-core-jvm-0.5.4.jar" "$CACHE/libs/kotlinx-io-core-jvm-0.5.4.jar"
fetch "$M/org/jetbrains/kotlinx/kotlinx-datetime-jvm/0.6.1/kotlinx-datetime-jvm-0.6.1.jar" "$CACHE/libs/kotlinx-datetime-jvm-0.6.1.jar"
fetch "$M/dev/whyoleg/cryptography/cryptography-core-jvm/0.4.0/cryptography-core-jvm-0.4.0.jar" "$CACHE/libs/cryptography-core-jvm-0.4.0.jar"
fetch "$M/dev/whyoleg/cryptography/cryptography-random-jvm/0.4.0/cryptography-random-jvm-0.4.0.jar" "$CACHE/libs/cryptography-random-jvm-0.4.0.jar"
fetch "$M/dev/whyoleg/cryptography/cryptography-provider-base-jvm/0.4.0/cryptography-provider-base-jvm-0.4.0.jar" "$CACHE/libs/cryptography-provider-base-jvm-0.4.0.jar"
fetch "$M/dev/whyoleg/cryptography/cryptography-provider-jdk-jvm/0.4.0/cryptography-provider-jdk-jvm-0.4.0.jar" "$CACHE/libs/cryptography-provider-jdk-jvm-0.4.0.jar"

KOTLIN_LIB="$(bash "$ROOT/scripts/print_kotlin_lib.sh")"
cp -n "$KOTLIN_LIB" "$CACHE/libs/kotlin-stdlib.jar" 2>/dev/null || true
cp -n "$(dirname "$KOTLIN_LIB")/kotlin-reflect.jar" "$CACHE/libs/kotlin-reflect.jar" 2>/dev/null || true

echo "cache ready: $CACHE"
