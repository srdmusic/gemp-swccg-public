# Verify Docker nuke rebuild

## When to run

After Claude has done a full Docker nuke rebuild:
```
docker compose down -v
docker compose build --no-cache build
docker compose up -d
[restart for DB race]
unzip web bundle into container
```

## Prompt template Claude should use

```
You are the work-verifier agent for the GEMP-SWCCG project. Steve just
ran a full Docker nuke + rebuild. Verify the deploy end-to-end.

CONTEXT (filled by Claude):
- Expected V-version(s) added in this rebuild: <e.g., V88, V88-CardSelection>
- Files modified in this rebuild: <comma-separated list>
- Web bundle expected to include: <yes/no for newgui StreamingAssets>

VERIFICATION STEPS (run all):

1. Container health:
     cd /Users/steve/gemp-swccg-public/src && docker compose ps
   Both gemp_swccg_app_1 and gemp_swccg_db_1 must be "Up".
   If either is missing/Exited → FAIL.

2. App startup completed:
     docker exec gemp_swccg_app_1 grep "GempukkuServer startup complete" /root/nohup.out | tail -3
   Must have at least one line. If empty → FAIL.

3. No fatal startup exceptions:
     docker exec gemp_swccg_app_1 grep -E "Exception in thread \"main\"|Communications link failure" /root/nohup.out | tail -5
   Should be empty OR followed by a successful restart. If the LAST
   startup attempt failed without a restart, FAIL.

4. GEMP serves a real response:
     curl -sf http://localhost:17001/gemp-swccg-server/ -o /tmp/gemp-health.html
     wc -c /tmp/gemp-health.html
   Must return > 100 bytes (more than just "OK"). If 0 or 404 → FAIL.

5. Cache-Control header active (V84):
     curl -sI http://localhost:17001/gemp-swccg/js/gemp-016/gameUi.js | grep -i "cache-control"
   Must show "Cache-Control: no-cache". If missing → WARN.

6. Web bundle deployed (epic-duel newgui StreamingAssets check):
     docker exec gemp_swccg_app_1 ls -la /opt/gemp-swccg/web/epic-duel/StreamingAssets/cardImages.json
   Must exist with size > 300000 bytes. If missing → FAIL with
   "MUST also unzip web bundle".

7. Each expected V-version is in the deployed JARs:
   For each V<n> Claude expects:
     docker exec gemp_swccg_app_1 sh -c 'grep -c "V<n>" /opt/gemp-swccg/src/gemp-swccg-server/target/classes/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.class'
     [also MoveEvaluator, CardSelectionEvaluator, ActionTextEvaluator as appropriate]
     [also chosenone variant — must match Rando count within 1]
   If any expected V-version returns 0 → FAIL.

8. Rando and Chosen One mirror:
   For each V-version, the count in rando/ classes should equal the
   count in chosenone/ classes within 1. Asymmetric mirroring →
   WARN with which side is missing.

KNOWN PAST FAILURES (cross-check):
- May 17 2026: docker compose down -v + up resulted in MySQL race —
  app started before DB ready, threw Communications link failure.
  Fix: restart the app container.
  → Always check for this exception in nohup.out and confirm the
    NEXT startup succeeded.
- May 19 2026: unzip -oq silently skipped StreamingAssets directory
  because an empty placeholder dir already existed. cardImages.json
  (379890 bytes) was missing. Unity newgui showed blank card images.
  → Step 6 above checks this specific path.

REPORT FORMAT:
PASS / WARN / FAIL: <one-line summary>

Details (always include):
- Containers: app=<status>, db=<status>
- Last startup line: <exact line>
- Recent exceptions: <count> (none / list if found)
- /gemp-swccg-server/ size: <bytes>
- Cache-Control: <value or MISSING>
- cardImages.json: <bytes or MISSING>
- V-version class string counts (per evaluator, per bot):
    V<n> rando.DeployEvaluator: <n>  chosenone.DeployEvaluator: <n>
    [etc]

If FAIL, list exact recovery steps Claude should run.

Append this report to:
  /Users/steve/gemp-swccg-public/.claude/skills/work-verifier/history.md
with a heading line like:
  ## 2026-MM-DD HH:MM — docker rebuild → <PASS|WARN|FAIL>
```
