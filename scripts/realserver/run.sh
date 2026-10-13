#!/usr/bin/env bash
# Bloom real-server harness.
#
# Downloads an official Paper (or legacy PaperSpigot) build from PaperMC's API, verifies its SHA-256, boots it
# headless with the Bloom self-test plugin, and reports the self-test result. Nothing is committed to the
# repository: downloads are cached in .cache/servers/ and servers run in build/realserver/<version>/.
#
# Usage:   BLOOM_ACCEPT_MINECRAFT_EULA=true scripts/realserver/run.sh <minecraft-version> [build]
# Example: BLOOM_ACCEPT_MINECRAFT_EULA=true scripts/realserver/run.sh 1.8.8
#
# Running a Minecraft server requires accepting the Minecraft EULA (https://aka.ms/MinecraftEULA). Setting
# BLOOM_ACCEPT_MINECRAFT_EULA=true states that YOU accept it; the script never accepts it on your behalf.
#
# Java selection: set JAVA8_HOME / JAVA21_HOME / JAVA25_HOME, or the script searches common install locations.
set -euo pipefail

VERSION="${1:?usage: run.sh <minecraft-version> [build]}"
BUILD="${2:-latest}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CACHE="$ROOT/.cache/servers"
WORK="$ROOT/build/realserver/$VERSION"
TIMEOUT="${BLOOM_SERVER_TIMEOUT:-600}"

if [ "${BLOOM_ACCEPT_MINECRAFT_EULA:-}" != "true" ]; then
  echo "Refusing to start: running a Minecraft server requires accepting the Minecraft EULA" >&2
  echo "(https://aka.ms/MinecraftEULA). If you accept it, re-run with BLOOM_ACCEPT_MINECRAFT_EULA=true." >&2
  exit 2
fi

PYTHON="$(command -v python3 || command -v python || true)"
[ -n "$PYTHON" ] || { echo "python is required to parse the PaperMC API response" >&2; exit 1; }
# Windows Python prints CRLF; strip the CR so values compare correctly under Git Bash.
py() { "$PYTHON" "$@" | tr -d '\r'; }

# ---- Java selection -------------------------------------------------------------------------------------------
major="${VERSION%%.*}"
minor="$(echo "$VERSION" | cut -d. -f2)"
if [ "$major" = "1" ] && [ "$minor" -le 16 ]; then JAVA_FEATURE=8
elif [ "$major" = "1" ]; then JAVA_FEATURE=21
else JAVA_FEATURE=25
fi
find_java() {
  local feature="$1" var="JAVA${1}_HOME" candidates=()
  if [ -n "${!var:-}" ]; then echo "${!var}/bin/java"; return; fi
  for base in "/c/Program Files/Eclipse Adoptium" "/c/Program Files/Java" "/usr/lib/jvm" "/Library/Java/JavaVirtualMachines"; do
    [ -d "$base" ] || continue
    while IFS= read -r dir; do candidates+=("$dir"); done < <(ls -d "$base"/*/ 2>/dev/null || true)
  done
  for dir in "${candidates[@]}"; do
    for java in "$dir/bin/java" "$dir/Contents/Home/bin/java"; do
      [ -x "$java" ] || [ -x "$java.exe" ] || continue
      local spec
      spec="$("$java" -XshowSettings:properties -version 2>&1 | grep 'java.specification.version' | awk '{print $3}')"
      spec="${spec#1.}"
      if [ "$spec" = "$feature" ]; then echo "$java"; return; fi
    done
  done
}
JAVA="$(find_java "$JAVA_FEATURE")"
[ -n "$JAVA" ] || { echo "No Java $JAVA_FEATURE found; set JAVA${JAVA_FEATURE}_HOME" >&2; exit 1; }
echo "Using Java $JAVA_FEATURE: $JAVA"

# ---- Download ---------------------------------------------------------------------------------------------------
mkdir -p "$CACHE"
API="https://fill.papermc.io/v3/projects/paper/versions/$VERSION/builds/$BUILD"
META="$(curl -fsSL -H "User-Agent: bloom-realserver-harness (github.com/crownalonedev/bloom)" "$API")"
read -r JAR_NAME JAR_URL JAR_SHA < <(echo "$META" | py -c '
import json, sys
d = json.load(sys.stdin)["downloads"]["server:default"]
print(d["name"], d["url"], d["checksums"]["sha256"])')
JAR="$CACHE/$JAR_NAME"
if [ ! -f "$JAR" ]; then
  echo "Downloading $JAR_NAME"
  curl -fsSL -o "$JAR.part" "$JAR_URL"
  mv "$JAR.part" "$JAR"
fi
ACTUAL_SHA="$(py -c "import hashlib,sys;print(hashlib.sha256(open(sys.argv[1],'rb').read()).hexdigest())" "$JAR")"
if [ "$ACTUAL_SHA" != "$JAR_SHA" ]; then
  echo "SHA-256 mismatch for $JAR_NAME (expected $JAR_SHA, got $ACTUAL_SHA); deleting it" >&2
  rm -f "$JAR"; exit 1
fi

# ---- Server directory ---------------------------------------------------------------------------------------------
(cd "$ROOT" && ./gradlew :bloom-selftest:shadowJar -q --console=plain)
PLUGIN="$(ls "$ROOT"/bloom-selftest/build/libs/bloom-selftest-*.jar | grep -v plain | head -1)"
rm -rf "$WORK"
mkdir -p "$WORK/plugins"
cp "$JAR" "$WORK/server.jar"
cp "$PLUGIN" "$WORK/plugins/"
echo "eula=true" > "$WORK/eula.txt"
PORT=$(( 25600 + RANDOM % 300 ))
if [ "$major" = "1" ] && [ "$minor" -le 12 ]; then LEVEL_TYPE="FLAT"; else LEVEL_TYPE="minecraft\\:flat"; fi
cat > "$WORK/server.properties" <<PROPS
online-mode=false
server-port=$PORT
level-type=$LEVEL_TYPE
generate-structures=false
spawn-protection=0
view-distance=3
simulation-distance=3
max-players=2
motd=Bloom real-server harness
PROPS

# ---- Run ----------------------------------------------------------------------------------------------------------
echo "Starting $JAR_NAME on port $PORT (timeout ${TIMEOUT}s)"
cd "$WORK"
set +e
if command -v timeout >/dev/null 2>&1; then
  # No -Dcom.mojang.eula.agree: on PaperSpigot 1.7.10 that flag makes startup return early and crash with an NPE.
  # eula.txt (written above) covers every version.
  timeout "$TIMEOUT" "$JAVA" -Xms512M -Xmx1536M -jar server.jar nogui < /dev/null > server.log 2>&1
else
  "$JAVA" -Xms512M -Xmx1536M -jar server.jar nogui < /dev/null > server.log 2>&1
fi
STATUS=$?
set -e
grep -E "BLOOM-SELFTEST|Bloom [0-9]" server.log || true
RESULTS="$WORK/plugins/BloomSelfTest/results.json"
if [ ! -f "$RESULTS" ]; then
  echo "No results.json (server exit $STATUS). Last log lines:" >&2
  tail -40 server.log >&2
  exit 1
fi
cp "$RESULTS" "$ROOT/build/realserver/results-$VERSION.json"
if grep -q '"result": "PASS"' "$RESULTS"; then
  echo "Bloom self-test PASSED on $VERSION ($JAR_NAME). Results: build/realserver/results-$VERSION.json"
else
  echo "Bloom self-test FAILED on $VERSION. See $RESULTS and $WORK/server.log" >&2
  exit 1
fi
