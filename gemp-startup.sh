#!/bin/bash
# =============================================================================
# gemp-startup.sh
#
# Automates the GEMP admin panel "startup" clicks after every server restart.
# Calls the same HTTP endpoints that the admin panel buttons use.
#
# Usage:
#   ./gemp-startup.sh
#   ./gemp-startup.sh --host http://localhost:17001 --user admin --pass yourpassword
#
# Or set environment variables:
#   GEMP_HOST, GEMP_USER, GEMP_PASS
# =============================================================================

set -euo pipefail

# ---------- defaults (override via args or env vars) -------------------------
HOST="${GEMP_HOST:-http://localhost:17001}"
USER="${GEMP_USER:-asdf}"
PASS="${GEMP_PASS:-asdf}"

# ---------- parse optional CLI args ------------------------------------------
while [[ $# -gt 0 ]]; do
    case "$1" in
        --host) HOST="$2"; shift 2 ;;
        --user) USER="$2"; shift 2 ;;
        --pass) PASS="$2"; shift 2 ;;
        *) echo "Unknown argument: $1"; exit 1 ;;
    esac
done

# ---------- prompt for password if not set -----------------------------------
if [[ -z "$PASS" ]]; then
    read -rsp "GEMP admin password for '$USER': " PASS
    echo
fi

BASE="${HOST}/gemp-swccg/server"
COOKIE_JAR="$(mktemp /tmp/gemp-cookies-XXXXXX.txt)"
trap 'rm -f "$COOKIE_JAR"' EXIT

echo ""
echo "🚀  GEMP Startup Automation"
echo "    Host : $HOST"
echo "    User : $USER"
echo ""

# ---------- helper -----------------------------------------------------------
call() {
    local label="$1"
    local endpoint="$2"
    shift 2
    local response
    response=$(curl -s -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
        -X POST "${BASE}${endpoint}" \
        "$@" \
        --max-time 10) || { echo "  ✗  $label  (curl error)"; return 1; }
    echo "  ✓  $label  →  ${response:0:80}"
}

# ---------- 1. log in --------------------------------------------------------
echo "[ 1/6 ] Logging in..."
LOGIN_RESP=$(curl -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
    -X POST "${BASE}/login" \
    -d "login=${USER}&password=${PASS}" \
    --max-time 10) || { echo "ERROR: Login request failed"; exit 1; }

if echo "$LOGIN_RESP" | grep -qi "error\|invalid\|fail\|unauthorized"; then
    echo "ERROR: Login failed — check username/password."
    echo "Server response: $LOGIN_RESP"
    exit 1
fi
echo "  ✓  Logged in as '$USER'"
echo ""

# ---------- 2. Enter Startup Mode --------------------------------------------
# This resets on every restart — MUST be clicked for tables to work.
echo "[ 2/6 ] Entering Startup Mode..."
call "Startup Mode (cancel shutdown)" "/admin/shutdown" \
    -d "enabled=false"
echo ""

# ---------- 3. Enable bot tables ---------------------------------------------
echo "[ 3/6 ] Enabling bot tables..."
call "Bot tables enabled" "/admin/settings/aitables" \
    -d "enabled=true"
echo ""

# ---------- 4. Enable private tables -----------------------------------------
echo "[ 4/6 ] Enabling private tables..."
call "Private tables enabled" "/admin/settings/privategames" \
    -d "enabled=true"
echo ""

# ---------- 5. Enable new account registration --------------------------------
echo "[ 5/6 ] Enabling new account registration..."
call "New account registration enabled" "/admin/settings/newaccounts" \
    -d "enabled=true"
echo ""

# ---------- 6. Enable in-game stat tracking -----------------------------------
echo "[ 6/6 ] Enabling in-game stat tracking..."
call "Stat tracking enabled" "/admin/settings/stattracking" \
    -d "enabled=true"
echo ""

echo "✅  All startup settings applied. GEMP is ready."
echo ""
