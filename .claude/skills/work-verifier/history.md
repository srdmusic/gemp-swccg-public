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

## 2026-05-25 20:42 — V130/V131/V134/V135 AI evaluator edit → WARN

Operation: Multi-file edit across rando + chosenone evaluators adding
V130 (DeckOracle helpers), V131 (deck-aware pull detection three-tier),
V134 (Odin Nesloor 5-force floor), V135 (self-move-to-friend gate).
V132 reverted (allow-opponent-activate baseline restored to 50.0f).
V133 dropped (deferred to consolidated V136). Deploy via mvn package +
./bin/gemp restart only — no full Docker nuke.

Context: branch ai-improvements-v91, 10 source files + 2 changelog
files modified. All edits local; not pushed.

Findings:
- Compile: PASS (mvn -pl gemp-swccg-server compile = BUILD SUCCESS,
  exit 0, "Nothing to compile - all classes are up to date" — already
  built fresh)
- Class file mtimes: all 10 .class files freshly compiled May 25 20:36
- web.jar mtime: May 26 03:36 UTC = 20:36 PT, freshly repackaged
- web.jar internal class mtime: May 26 03:42 UTC = 20:42 PT, slightly
  later (jar repackage step after compile)
- Container: gemp_swccg_app_1 "Up 4 minutes" at verification time —
  consistent with restart picking up fresh web.jar
- Server startup: "GempukkuServer startup complete." at 2026-05-26
  T03:37:54Z (= 20:37 PT). Last startup completed cleanly. 4 successful
  startups total in /root/nohup.out. ONE earlier 02:11 UTC startup
  attempt has historical NoClassDefFoundError/ClassNotFoundException
  trace (log4j ThrowableStackTraceRenderer + GameEndReason) — NOT from
  the V130-V135 rebuild, recovered by the 03:37 startup. Not blocking.
- HTTP /gemp-swccg/: 200
- Hall connection at 03:37:54 working (gemp-swccg.log shows "Welcome
  to room: Game Hall")

V-tag string counts in deployed web.jar (canonical artifact loaded by
JVM via -jar /opt/gemp-swccg/src/gemp-swccg-async/target/web.jar):
- V130 (comments only — no runtime strings expected): 0/0 — PASS
- V131 in ActionTextEvaluator: rando=5, chosenone=5 — MIRROR PASS
- V134 in ActionTextEvaluator: rando=3, chosenone=3 — MIRROR PASS
- V135 in MoveEvaluator:        rando=2, chosenone=2 — MIRROR PASS

Negative-presence checks (V132 reverted, V133 dropped):
- V132 in ForceActivationEvaluator: rando=0, chosenone=0 — PASS
  Both source files: only comment "V132 mirror reverted" / "V132 (which
  had dropped this to 10) reverted". Baseline 50.0f confirmed at
  lines 64, 77, 108 (rando) and corresponding chosenone.
- V133 in CardSelectionEvaluator: rando=0, chosenone=0 — PASS
  Both source files: only the "// === V133 DROPPED" comment block at
  rando:1703-1710 / chosenone:1329-1336. No executable logic.

Type-by-API discipline (Step 4):
- Zero generic-noun .contains() violations on title strings across all
  10 edited files. The ONLY title-substring match is V134's
  contains("odin nesloor") — proper-noun match flagged by Steve as
  acceptable per §2B (specific persona name, not a card-type noun).
- V131's v131LocNouns/v131WeaponNouns Sets contain substrings like
  "site", "system", "weapon", "lightsaber" — these match against PARSED
  PULL-TARGET NOUNS from card game text (variable v131Tl), NOT against
  card titles. Not a §2B violation. Confirmed as intended at
  ActionTextEvaluator.java:3753-3766 (rando) + corresponding chosenone.

action.addReasoning() pattern (V79 past-failure check):
- V134 and V135 augment an EXISTING action via addReasoning(reason,
  score) — no new action created, so no actions.add() needed.
  Pattern matches surrounding V37/V38.3/V67ai/V95 code.

Changelog parity (Steps 7, 8):
- AI_CHANGELOG.md: V130-V135 section at lines 782-849 — present,
  matches code comment headers verbatim.
- AI_VERSION_HISTORY.md: V130-V135 family at lines 2485-2571 —
  present, matches code comment headers verbatim.
- V130 changelog description ↔ code comment: MATCH
- V131 changelog description ↔ code comment: MATCH
- V132 DROPPED description ↔ code comment: MATCH
- V133 DROPPED description ↔ code comment: MATCH
- V134 changelog description ↔ code comment: MATCH
- V135 changelog description ↔ code comment: MATCH

No-fabrication check (Step 10):
- Odin Nesloor: EXISTS in card_blueprint_database — TWO cards:
    "12_65"  "Odin Nesloor"           side=LIGHT  expansion=CORUSCANT
    "209_21" "Odin Nesloor & First Aid" side=LIGHT  expansion=SET_9
  ZERO dark-side entries.
- "Bug 7a" referenced in V135 comments: cannot verify outside this
  bundle, presumably internal Steve-numbering for a recent game incident.
  No SWCCG content claim — just narrative.

WARN: SIDE LABEL REVERSED on Odin Nesloor in V134 documentation
  Three locations claim "Odin Nesloor is dark-side" — this is FACTUALLY
  WRONG. Card database confirms LIGHT side for both Odin Nesloor cards.
    1. src/.../chosenone/evaluators/ActionTextEvaluator.java:172-174
       "Note: Odin Nesloor is a dark-side card so this mirror is
        effectively dead code for the light-side bot"
    2. AI_CHANGELOG.md:829-830
       "Odin Nesloor is dark-side only; chosenone mirror is dead code
        by design"
    3. AI_VERSION_HISTORY.md:2542-2543
       "Odin Nesloor is dark-side; chosenone mirror is dead code by
        design — kept for V-tag symmetry"
  CORRECT polarity:
    - Odin Nesloor is LIGHT side.
    - chosenone (light-side bot) IS the side that will actually fire
      V134 in real games.
    - rando (dark-side bot) version is the DEAD CODE one — kept for
      V-tag symmetry.
  Impact: V134 LOGIC is correct in both files (5-force floor in MOVE).
  Risk is documentation-only — future maintenance reading the comment
  will misunderstand which bot uses this rule. Recommend fixing the
  comment and the two changelog lines before push.

Outcome: All 10 source/class files compiled and deployed via web.jar.
All V-tag mirrors symmetric across rando/chosenone. V132 revert and
V133 drop confirmed at source AND bytecode level. No type-by-API
violations on edited files. Changelog descriptions match code comment
headers. Server startup completed; HTTP 200; hall responsive.

Single WARN: Odin Nesloor side label reversed in 3 places (comment
+ 2 changelogs). Pure-doc bug; logic unaffected.

Verdict: WARN (one actionable doc-correctness issue).

Lesson added: When mirroring a rule across both bots, explicitly
state in the comment which side ACTUALLY uses the rule and which
side is the symmetric placeholder — verify against
card_blueprint_database before claiming "dead code on this side".
The side-reversal failure mode (calling LIGHT-side cards dark-side
or vice versa) joins the "no fabrication" rule family. Add to
references/verify-evaluator-edit.md as a sub-check under Step 10c.
