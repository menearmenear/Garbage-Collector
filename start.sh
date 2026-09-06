#!/usr/bin/env bash
set -uo pipefail

# ---------------------------------------------------------------
#  Garbage-Collector local test server launcher
#  - starts the Paper server in server/ with one click
#  - auto-accepts the EULA (local dev/testing only)
#  - memory override: MEM=3G ./start.sh
# ---------------------------------------------------------------

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$ROOT/server"

# ---- colors ------------------------------------------------------
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
    C_RESET=$'\033[0m'; C_BOLD=$'\033[1m'; C_DIM=$'\033[2m'
    C_CYAN=$'\033[36m'; C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'; C_RED=$'\033[31m'
else
    C_RESET=""; C_BOLD=""; C_DIM=""; C_CYAN=""; C_GREEN=""; C_YELLOW=""; C_RED=""
fi

fail() { printf '%b[FAIL]%b %s\n' "$C_RED" "$C_RESET" "$1"; exit 1; }
info() { printf '  %b..%b %s\n' "$C_DIM" "$C_RESET" "$1"; }
ok()   { printf '  %b[ OK ]%b %s\n' "$C_GREEN" "$C_RESET" "$1"; }

# ---- sanity checks ----------------------------------------------
command -v java >/dev/null 2>&1 || fail "Java not found. Install openjdk-25 (see README)."
JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*version "//; s/".*//')

if [ ! -d "$SERVER_DIR" ]; then
    fail "No server/ directory. Move a Paper jar into server/ first."
fi
cd "$SERVER_DIR"

JAR=$(find . -maxdepth 1 -name 'paper-*.jar' -o -maxdepth 1 -name 'server.jar' 2>/dev/null | head -1)
[ -n "$JAR" ] || fail "No paper-*.jar found in server/. See README for setup."

JAR_NAME=$(basename "$JAR")

# ---- EULA --------------------------------------------------------
if [ ! -f eula.txt ] || ! grep -q '^eula=true' eula.txt 2>/dev/null; then
    printf '\n%bWARNING: accepting Minecraft EULA (local test server only)%b\n' "$C_YELLOW" "$C_RESET"
    printf 'eula=true\n' > eula.txt
fi

# ---- memory ------------------------------------------------------
MEM="${MEM:-2G}"

# ---- header ------------------------------------------------------
printf '%b%s%b\n' "$C_DIM" "========================================================" "$C_RESET"
printf '%b%30s%b\n' "$C_BOLD" "Garbage-Collector Test Server" "$C_RESET"
printf '%b%30s%b\n' "$C_CYAN" "$JAR_NAME" "$C_RESET"
printf '  %b%-18s%b %s\n' "$C_DIM" "java" "$C_RESET" "$JAVA_VER"
printf '  %b%-18s%b %s\n' "$C_DIM" "memory" "$C_RESET" "$MEM"
printf '  %b%-18s%b %s\n' "$C_DIM" "plugins" "$C_RESET" "$(ls plugins/*.jar 2>/dev/null | wc -l | tr -d ' ') loaded"
printf '%b%s%b\n' "$C_DIM" "========================================================" "$C_RESET"
printf '  Type %bstop%b in the console to shut down cleanly.\n\n' "$C_BOLD" "$C_RESET"

# ---- launch ------------------------------------------------------
exec java -Xms${MEM} -Xmx${MEM} -jar "$JAR" nogui