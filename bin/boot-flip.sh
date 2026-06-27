#!/usr/bin/env bash
# Auto-flips the GEMP server operational + gameplay switches ON, on every boot.
# Launched in the background by the app container's docker-compose command, right
# before java is exec'd. Runs INSIDE the app container, so it talks to localhost.
#
# Waits for the server to answer, then logs in as asdf and flips:
#   shutdown     -> enabled=false  (operational; lets games/tables start)
#   aitables     -> enabled=true   (bot / Rando tables)
#   privategames -> enabled=true
#   stattracking -> enabled=true
#   newaccounts  -> enabled=true
# bonusabilities (the April Fool's Day abilities toggle) is left OFF on purpose.

U="${1:-http://localhost:80}"
LOG=/logs/boot-flip.log

until curl -sf -o /dev/null "$U/gemp-swccg/" 2>/dev/null; do sleep 3; done
sleep 2

cj=$(mktemp)
curl -s -c "$cj"          -d 'login=asdf&password=asdf'  "$U/gemp-swccg-server/login"             >/dev/null 2>&1
curl -s -c "$cj" -b "$cj" -X POST -d 'enabled=false'     "$U/gemp-swccg-server/admin/shutdown"    >/dev/null 2>&1
for s in aitables privategames stattracking newaccounts; do
  curl -s -c "$cj" -b "$cj" -X POST -d 'enabled=true'    "$U/gemp-swccg-server/admin/settings/$s" >/dev/null 2>&1
done
rm -f "$cj"
echo "[boot-flip] $(date '+%Y-%m-%d %H:%M:%S'): operational + aitables/privategames/stattracking/newaccounts ON (April Fool's bonusabilities left off)" >> "$LOG" 2>/dev/null
