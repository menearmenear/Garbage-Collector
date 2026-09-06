#!/usr/bin/env bash
set -uo pipefail

# ---------------------------------------------------------------
#  Garbage-Collector build script
#  - checks env, cleans, builds, reports jar + timing
# ---------------------------------------------------------------

# ---- colors (auto-disabled when not a tty) ----------------------
if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
    C_RESET=$'\033[0m'
    C_BOLD=$'\033[1m'
    C_DIM=$'\033[2m'
    C_CYAN=$'\033[36m'
    C_GREEN=$'\033[32m'
    C_YELLOW=$'\033[33m'
    C_RED=$'\033[31m'
    C_BLUE=$'\033[34m'
else
    C_RESET=""; C_BOLD=""; C_DIM=""; C_CYAN=""; C_GREEN=""
    C_YELLOW=""; C_RED=""; C_BLUE=""
fi

# ---- helpers -----------------------------------------------------
TOTAL_START=$(date +%s)
LOG_FILE="$(dirname "$0")/build.log"

step() {
    printf '\n%b[%s/%s] %b%s%b\n' \
        "$C_CYAN" "$1" "$2" "$C_BOLD" "$3" "$C_RESET"
}

ok()   { printf '  %b[ OK ]%b %s\n' "$C_GREEN" "$C_RESET" "$1"; }
info() { printf '  %b..%b %s\n' "$C_DIM" "$C_RESET" "$1"; }
warn() { printf '  %b[WARN]%b %s\n' "$C_YELLOW" "$C_RESET" "$1"; }
fail() { printf '  %b[FAIL]%b %s\n' "$C_RED" "$C_RESET" "$1"; }

hr() {
    printf '%b' "$C_DIM"
    printf '========================================================\n'
    printf '%b' "$C_RESET"
}

elapsed() {
    local now secs m s
    now=$(date +%s)
    secs=$((now - TOTAL_START))
    m=$((secs / 60)); s=$((secs % 60))
    printf '%dm %02ds' "$m" "$s"
}

sep_line() {
    printf '%b%s%b\n' "$C_DIM" "-----------------------------------------------------" "$C_RESET"
}

# ---- header ------------------------------------------------------
hr
printf '%b%24s%b\n' "$C_BOLD" "Garbage-Collector Build" "$C_RESET"
printf '%b%s%b\n' "$C_DIM" "  branch : $(git branch --show-current 2>/dev/null || echo '?')" "$C_RESET"
hr

# ---- 1. environment ---------------------------------------------
step 1 6 "Environment"

if ! command -v java >/dev/null 2>&1; then
    fail "Java not found. Install with: pkg install openjdk-21"
    exit 1
fi
if ! command -v gradle >/dev/null 2>&1; then
    fail "Gradle not found. Install with: pkg install gradle"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/.*version "//; s/".*//')
GRADLE_VER=$(gradle --version 2>/dev/null | sed -n 's/^Gradle //p')
ok "java   : $JAVA_VER"
ok "gradle : $GRADLE_VER"
ok "dir    : $(pwd)"

# ---- 2. clean ----------------------------------------------------
step 2 6 "Cleaning previous build"
rm -rf build
ok "removed build/"

# ---- 3. build ----------------------------------------------------
step 3 6 "Building (started $(date +%H:%M:%S))"
BUILD_START=$(date +%s)
info "logging to: $LOG_FILE"

BUILD_OUTPUT=$(gradle clean build --no-daemon 2>&1)
BUILD_STATUS=$?
printf '%s\n' "$BUILD_OUTPUT" > "$LOG_FILE"
BUILD_SECS=$(($(date +%s) - BUILD_START))

if [ "$BUILD_STATUS" -ne 0 ]; then
    printf '\n%b'
    printf '  BUILD FAILED after %dm %02ds\n' "$((BUILD_SECS / 60))" "$((BUILD_SECS % 60))"
    printf '%b' "$C_RESET"
    sep_line
    printf '%s\n' "$BUILD_OUTPUT" | tail -30
    sep_line
    fail "troubleshoot: check build.log or run: gradle --stacktrace"
    exit 1
fi

ok "build finished in $((BUILD_SECS / 60))m $((BUILD_SECS % 60))s"

# ---- 4. result ---------------------------------------------------
step 4 6 "Build result"
JAR=$(find build/libs -name '*.jar' ! -name '*sources*' 2>/dev/null | head -1)
JAR_COUNT=$(find build/libs -name '*.jar' ! -name '*sources*' 2>/dev/null | wc -l)

if [ -n "$JAR" ]; then
    SIZE=$(du -h "$JAR" | cut -f1)
    ok "$(basename "$JAR") ($SIZE)"
else
    FAILED_TESTS=$(printf '%s\n' "$BUILD_OUTPUT" | grep -c 'FAILED' || true)
    if [ "$FAILED_TESTS" -gt 0 ]; then
        warn "no jar produced (tests failed?)"
    else
        fail "no jar produced"
    fi
    [ "$JAR_COUNT" -eq 0 ] && exit 1
fi

# ---- 5. deploy ----------------------------------------------------
step 5 6 "Deploy to server"
SERVER_PLUGINS="$(dirname "$0")/server/plugins"
if [ -n "$JAR" ] && [ -d "$SERVER_PLUGINS" ]; then
    cp "$JAR" "$SERVER_PLUGINS/"
    ok "deployed $(basename "$JAR") -> server/plugins/"
else
    warn "no server/plugins directory; skipping deploy"
fi

# ---- 6. summary --------------------------------------------------
step 6 6 "Summary"
printf '%b%12s%b : %s\n' "$C_BOLD" "branch" "$C_RESET" "$(git branch --show-current 2>/dev/null || echo '?')"
printf '%b%12s%b : %s\n' "$C_BOLD" "commit" "$C_RESET" "$(git rev-parse --short HEAD 2>/dev/null || echo '?')"
printf '%b%12s%b : %s\n' "$C_BOLD" "time" "$C_RESET" "$(elapsed)"
sep_line
printf '  %bArtifacts:%b\n' "$C_BOLD" "$C_RESET"
printf '%b%s%b\n' "$C_DIM" "$JAR" "$C_RESET"
sep_line

printf '\n%bDone. Copy the jar into your server plugins/ folder.%b\n' "$C_GREEN" "$C_RESET"
exit 0