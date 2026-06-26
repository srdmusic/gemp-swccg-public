# Verification History

Append-only log. Every verifier invocation writes a record here so the
agent (and Claude) can learn patterns of past failures.

Format per entry:

```
## YYYY-MM-DD HH:MM — <operation type> → <PASS|WARN|FAIL>

Operation: <brief description>
Context: <relevant context — branch name, V-versions, paths>

Findings:
- <check>: <result>
- <check>: <result>

Outcome: <if FAIL, what was fixed; if WARN, what was noted>
```

---

## 2026-05-19 22:00 — git push v66-memory-audit → INITIAL PUSH (no verifier ran)

Operation: First push of AI improvements to srdmusic/gemp-swccg.
Context: 10 files, +1176/-37 lines on branch v66-memory-audit.
Outcome: Successful push but branch name was stale (v66 not current).
Lesson: Verifier should warn when branch name doesn't reflect content.

---

## 2026-05-20 02:00 — git push ai-improvements-v91 → FAIL (PR #3260)

Operation: New focused branch + PR to PlayersCommittee/gemp-swccg.
Context: Branch based on origin/master (PUBLIC mirror), PR target was
PlayersCommittee/gemp-swccg (PRIVATE).
Findings:
- Local branch HEAD: matches remote ✓
- Diff vs PR base: 933 commits, 3000+ files, +648K/-78K — MASSIVE
- Cause: origin/master and pc-private/master have diverged by ~8000 commits
Outcome: Maintainer Gergall flagged the bloat within an hour. Fixed by
rebasing onto pc-private/master, force-push, comment-explain. Single
commit now: 72 files, +75146/-633.
Lesson: ALWAYS compute diff against the actual PR base remote, not
origin/master, before reporting PR success.

---

## 2026-05-20 03:30 — docker rebuild → FAIL (silent)

Operation: Full nuke + rebuild for V88/V90/V91.
Context: docker compose down -v, build --no-cache, up, restart, unzip web.
Findings:
- Containers up ✓
- GEMP /gemp-swccg-server/ → 200 ✓
- BUT: epic-duel/StreamingAssets/cardImages.json was MISSING from container
- Cause: previous unzip -oq left an empty StreamingAssets/ placeholder dir,
  the -o flag declined to recreate it on the next unzip
- Visible only as blank card images in Unity newgui
Outcome: Re-extracted just StreamingAssets path. 379890-byte file restored.
Steve had to point out the bug after Claude declared "fixed".
Lesson: After unzip -oq, ALWAYS find . | wc -l the destination and
compare against unzip -l | wc -l of the source. Quiet mode hides
warnings that would catch this.

---

## 2026-05-20 09:00 — AI_CHANGELOG.md verification → 6 WARN/FAIL caught, all fixed

Operation: Claude wrote AI_CHANGELOG.md from working memory after long session.
Steve caught one fabricated line ("V79 follow-up: Hidden Path Jakku/Crait
awareness" — V79 is actually Verge of Greatness). Verifier run on the full
file caught 5 more:
- V51 buddy-path miscitation (buddy-path is V38, not V51)
- V61 wrong description (it's a choose-option pick, not starting location)
- V64 oversimplified (it's tighter maintenance with opponent drain buffer)
- V29.13 vs V73 swap (drain-comparison-on-moves is V73; V29.13 is Hunt Down grouping)
- V84 not in PR diff (the WebRequestHandler edit got dropped during rebase
  to AI-only branch; cache behavior the user sees comes from upstream
  SwccgoHttpRequestHandler)

Lessons added to the verify-evaluator-edit protocol implicitly:
- AFTER writing any user-facing summary/changelog/notes that cite V-tags,
  run a parity check: every V-tag claim must match the code comment header
  for that V-tag. Don't trust working memory after long sessions.
- When citing a rule that touches code outside AI evaluators (UI, server
  handlers), confirm the code edit is actually IN the PR branch's diff,
  not just in some other local branch.

Outcome: changelog cleaned up before posting to PR.

## 2026-05-20 17:00 — git push ai-improvements-v91 (changelogs) → WARN
Fork intact (5 branches, parent=PlayersCommittee). Commit 5437e73d7 landed
on dev-fork with exactly AI_CHANGELOG.md + AI_VERSION_HISTORY.md (18026 +
100710 bytes). Diff vs pc-private/master = 74 files (72 AI + 2 docs).
WARN: PR #3260 already MERGED at 2026-05-20T16:40:08Z with headRefOid
b8d95d27e (pre-changelog). Push landed AFTER merge, so the changelogs
are on the fork branch but never reached upstream via this PR. A new PR
is needed if changelogs should land in PlayersCommittee/master.

---

## 2026-05-22 ~16:00 — HallServer operational-default fix → PASS

Operation: One-line fix to HallServer.java defaulting `_operational = true`
at construction to eliminate the "Server is not yet in operational mode"
MOTD warning on every Mac/Docker restart.
Context: src/gemp-swccg-server/.../hall/HallServer.java line 71 change.
Deploy path: mvn clean install -DskipTests, docker compose build --no-cache,
up -d, restart, unzip web.zip, then cold restart to simulate fresh boot.

Findings:
- Containers: app=Up ~1m (after cold restart), db=Up 21m ✓
- App startup completed 4 times total, last one clean after cold restart ✓
- HallServer.class mtime: May 22 22:47 (fresh, ~3min before verification) ✓
- "operational mode" strings still present in deployed bytecode: 2 (the
  two warning messages remain compiled in, just unreachable at boot) ✓
- Cold restart + login: HTTP 200 ✓
- MOTD after cold start (NO admin POST made):
  "Follow the PC on Twitter @swccg to stay informed of Star Wars CCG
  news and events." (friendly default, NOT the operational warning) ✓
- aiTablesEnabledBoolean="true" ✓
- cardImages.json: 379890 bytes (May 20 mtime, untouched by this deploy) ✓
- Bridge health: ok (Ollama reachable, 5 models available) ✓

Historical exception in nohup.out: 1 MySQL race ("privateGamesEnabled"
Communications link failure) from an earlier startup attempt. NOT from
the cold restart in step 4 — subsequent startups completed successfully
(4 "GempukkuServer startup complete" lines total in log, including one
after the verification cold restart).

Outcome: Fix is fully deployed and behaving as designed. The motd default
"Server is not yet in operational mode" no longer fires at boot, and
Steve no longer needs to manually POST /admin/shutdown after every
restart. The two warning strings are preserved in bytecode so the
mechanism still works if `setOperational(false)` is ever called at
runtime — only the BOOT-TIME default flipped.

Lesson reinforced: K-2's verification claims matched reality this time
(MOTD friendly default + aiTablesEnabledBoolean true + no admin POST
needed). The mvn + docker fast-path deploy worked correctly for a
single-file server change without requiring a full nuke rebuild.

## 2026-05-24 — V127-V129 + HallServer fix → PASS

PASS: All 8 verification steps green. V127, V128, V129, and HallServer
operational default are deployed and behaving correctly. Symmetry holds
across both Rando and ChosenOne ActionTextEvaluator bytecode.

Details:
- Containers: app=Up 5 minutes, db=Up 48 minutes
- Last startup line: "GempukkuServer startup complete." (3 successive,
  most recent at log line 110125). Pre-existing MySQL race exception at
  line 82498 was followed by a successful startup at 82958, so the
  failure is historical and recovered. No new fatal exception from this
  rebuild.
- HTTP health: 200 on /gemp-swccg/
- V128 GameRequestHandler.java Phase.add lines (verbatim, only 4 of the
  original 6 remain — DEPLOY and BATTLE intentionally absent):
    _autoPassDefault.add(Phase.ACTIVATE);
    _autoPassDefault.add(Phase.CONTROL);
    _autoPassDefault.add(Phase.MOVE);
    _autoPassDefault.add(Phase.DRAW);
  Source file lines 72-75. The two strings "Phase.DEPLOY" /
  "Phase.BATTLE" still appear in the comment block at lines 63-64 as a
  documented revert recipe. Bytecode `strings` confirms only ACTIVATE /
  CONTROL / MOVE / DRAW remain as enum-referenced phases for the
  auto-pass set (DEPLOY/BATTLE not in the sorted output). Matches V128
  intent.
- V129 AFA strings: rando=1, chosenone=1 (mirror present in both bots'
  ActionTextEvaluator bytecode — past "missing chosenone mirror" failure
  class avoided)
- V127 duplicate +800 string in Rando CardSelectionEvaluator: 1 match
- HEAD on ai-improvements-v91: a9b36f0d0 "V127-V129 + HallServer
  operational default fix" (matches expected hash)
- LOGIN: 200 (cookie obtained as asdf)
- MOTD post-rebuild:
    motd="Follow the PC on Twitter @swccg to stay informed of Star Wars
          CCG news and events."
    aiTablesEnabledBoolean="true"
  No "Server is not yet in operational mode" string anywhere in the hall
  response. HallServer operational-default-true fix continues to work
  WITHOUT a manual POST to /admin/shutdown after the rebuild. Confirms
  the V128 game-handler change and the HallServer infrastructure change
  do not conflict.

Recovery steps if any future regression flips this back:
- N/A — all checks PASS on this run.

Cross-check notes:
- "Code that compiles is not code that works": bytecode-string checks
  for V127 / V128 / V129 all positive. No code-vs-bytecode drift.
- chosenone mirror present (V129 AFA string count = 1, matches rando).
- Both V101 / V119 removals (subsumed into V127) leave the file
  syntactically valid because mvn build completed without errors and
  the server started successfully (would not have produced the "startup
  complete" log line if a compile error had broken either evaluator).

Lesson reinforced: parallel multi-file mirror edits (V129 in two
ActionTextEvaluator subclasses) cleared the symmetry check on first
try. Continuing to enforce "ALWAYS edit both bots' evaluators together"
keeps the mirror-missing failure class extinct.
