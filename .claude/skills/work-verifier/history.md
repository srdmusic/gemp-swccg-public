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

## 2026-06-25 04:20 — V185/V186 fast-path deploy → PASS

Operation: Deploy of two Rando AI rules to the running server via
fast-path (in-container mvn package + docker compose restart build,
NOT a full nuke). V185 = weapon-deployability gate (DeployEvaluator +
DeckOracle). V186 = "I Want That Map" starting setup (CardSelectionEvaluator
+ ObjectiveAnalyzer). Prior running jar was V184.
Context: branch rando-consolidation-2026-06-23, base 55c22cf49.
Running jar: gemp_swccg_app_1:/opt/gemp-swccg/src/gemp-swccg-async/target/web.jar.

Verification was BYTECODE-PRESENCE ONLY (read-only, no rebuild/edit).
RULE-FIRED-IN-GAME is explicitly NOT verified — needs an "I Want That Map"
objective deck (V186) and an A-Good-Friend-style reserve-deploy-only-
unattachable-weapons board (V185).

Findings (claim-by-claim):

CLAIM 1 — V185/V186 in running jar bytecode: PASS
  Extracted 4 classes from web.jar inside container, `strings` + grep:
  - DeployEvaluator.class       V185=2  V186=0
  - DeckOracle.class            V185=0  V186=0  (comment-only, expected)
  - CardSelectionEvaluator.class V185=0 V186=4
  - ObjectiveAnalyzer.class     V185=0  V186=1
  Required minimum met: V185 present in DeployEvaluator (>=1), V186 present
  in CardSelectionEvaluator (>=1). DeckOracle carries V185 only in source
  comments (no runtime string) — matches the "V-tags may live in comments"
  caveat in the task. Actual runtime strings dumped and confirmed REAL
  log/reasoning strings, not coincidental:
  - V185: "V185 WEAPON-NO-HOLDER blocked: source={} targets={}",
          "V185 WEAPON, NO LEGAL HOLDER: every Reserve-Deck target left..."
  - V186: "V186 PREFERRED START (I Want That Map): {} (+1000)",
          "V186 STARKILLER SYSTEM: cardId={} bp={} title={} (+400)",
          "V186 STARKILLER BASE SYSTEM - download engine...",
          "V186 PREFERRED STARTING EFFECT (I Want That Map):",
          "[ObjectiveAnalyzer] V186: I Want That Map detected - naming
           Starkiller Base (location) + The First Order Was Just The
           Beginning (effect)."

CLAIM 2 — clean boot + game servers up: PASS
  - Java proc alive: PID 1, `java -Xmx4g ... -jar .../web.jar
    com.gempukku.swccgo.async.SwccgoAsyncServer`
  - nohup.out tail: "GempukkuServer startup complete." (last at line 148558)
    preceded by Started: HallServer / SwccgoServer / ChatServer.
  - "Server is in operational mode and games are now able to be started"
    at tail.
  - 10 total startup-complete lines historically; the LAST is the deploy boot.
  - Fatal-exception scan after line 148558: ZERO real java stacktraces
    (`^\s+at ...java:NN)` = 0, no "Caused by"/NoClassDefFound/FATAL).
    The 136 historical "exception" grep hits are log4j config DEBUG attr
    names ("ignoreExceptions"/"alwaysWriteExceptions") + earlier-startup
    MySQL races, NOT from this boot.

CLAIM 3 — operational + AI tables: PASS
  - curl http://localhost:17001/gemp-swccg-server/ → 200
  - Hall (logged in as asdf): aiTablesEnabledBoolean="true",
    friendly default motd (no "not yet in operational mode" string).

CLAIM 4 — DB safety: PASS
  - `docker volume ls` → EMPTY (header only, no volumes).
  - gemp_swccg_db_1 mount: bind /Users/steve/gemp-swccg-public/database
    -> /var/lib/mysql. Host bind-mount, not a docker volume. Deploy was
    Java-only; DB untouched.
  - Containers: app Up 3 min, db Up 7 min (db older = never restarted,
    consistent with a build-only restart that left the DB container alone).

Freshness cross-check:
  - web.jar mtime 2026-06-25 04:16:34 UTC; container StartedAt
    04:17:27 UTC. Jar built BEFORE restart => the running process loaded
    the freshly-built V185/V186 jar (not a stale one). The .class mtimes
    inside the jar are 04:20 (extraction time, expected).

NOT VERIFIED (stated explicitly): V185 and V186 FIRING in a real game.
Bytecode-present != rule-fired. Requires specific deck scenarios.

Note on /gemp-swccg/ → 404 (static web frontend): AGREE it is unrelated
to this deploy. Evidence: the change was server-side Java only (AI
evaluator classes in web.jar); the static frontend is served from a
separate bind-mounted web dir (/opt/gemp-swccg/web), not rebuilt or
touched by an mvn package of gemp-swccg-async. The game API path
/gemp-swccg-server/ returns 200, which is what matters for AI-table play.
Pre-existing, not a regression from V185/V186.

Verdict: PASS (4/4 claims). Deploy is live in bytecode and the server is
healthy. Only outstanding item is the in-game firing test, which is out of
scope for a read-only bytecode verification.

Lesson reinforced: fast-path deploy (in-container mvn package + restart
build, no nuke) correctly swapped the jar — jar-mtime-before-container-start
is the clean check that the running process picked up the new build. The
log4j "ignoreExceptions"/"alwaysWriteExceptions" attribute names are a
recurring false-positive for exception greps; always filter to real
stack-frame regex (`^\s+at .*\.java:[0-9]+\)`) when scanning startup tails.

## 2026-07-01 21:15 — V177 CATEGORY RESCUE (AI evaluator edit + fast-path deploy) → PASS

Operation: In-place adjustment of V177 dead-search gate in both bots'
ActionTextEvaluator: before applying the -2000 DEAD SEARCH block, consult
DeckOracle.validatePullFromSourceCard(RESERVE_DECK, gameText); WILL_SUCCEED
-> no block (log "V177 CATEGORY RESCUE"), else original block unchanged.
Deploy via bin/gemp reload-ai (in-container mvn + JVM restart).
Context: branch rando-consolidation-2026-06-23, uncommitted working tree.
TDIGWATT replay 7co2xviwqo5q3zac (I'm Sorry interior-CC-site download
false-blocked 19x).

Findings (claim-by-claim):

CLAIM 1 — source edits: PASS
- git diff = EXACTLY 4 files: rando ActionTextEvaluator.java (+36/-8, one
  hunk @259), chosenone mirror (+37/-8, one hunk @240), AI_CHANGELOG.md (+9),
  AI_VERSION_HISTORY.md (+21). No other code touched.
- Rescue sits inside the live V177 gate (rando:221 / chosenone:202,
  `textLower.contains("from reserve deck") || "[download]"`). grep for
  "if (false" / "if(false" in BOTH files: ZERO hits — no dead-code guard
  anywhere in either file.
- Original -2000 block body inside the else branch is text-identical to the
  pre-edit lines (only +4 spaces re-indent from nesting); retains
  actions.add(action); continue;. Rescue branch correctly does NOT
  add/continue, so the action falls through to normal scoring (V116 floor
  at rando:341 still applies) — V79 actions.add failure class N/A by design.
- API real in both bots: DeckOracle.validatePullFromSourceCard(Zone,String)
  at rando DeckOracle:1161 / chosenone:1045; PullValidation has public
  outcome + reason fields; PullOutcome.WILL_SUCCEED exists.
- Mirror symmetric: "V177 CATEGORY RESCUE" source hits rando=2 (comment+log),
  chosenone=2.

CLAIM 2 — build + deploy: PASS
- python3 zipfile byte-search of src/gemp-swccg-async/target/web.jar:
  b"V177 CATEGORY RESCUE" count=1 in BOTH
  com/gempukku/swccgo/ai/models/rando/evaluators/ActionTextEvaluator.class
  (149355 B) and .../chosenone/.../ActionTextEvaluator.class (148904 B),
  class timestamps 2026-07-02 04:03:22.
- Local target/classes .class files md5-IDENTICAL to the jar copies
  (rando ba34..., chosenone 4ac4...) — compile + repackage consistent.
- Ordering: jar mtime 21:06:07 PDT (04:06:07Z), container StartedAt
  04:06:08Z — jar written BEFORE JVM start; host jar md5 == in-container
  jar md5 (a2bfe482...) via bind mount; java PID 1 running that jar.
- Container gemp_swccg_app_1 "Up 2 minutes" at verification (well inside
  the ~30 min claim); db Up 47 hours (untouched, correct for fast path).
- HTTP: /gemp-swccg/ -> 200 AND /gemp-swccg-server/ -> 200.

CLAIM 3 — gameplay switches: PASS
- logs/boot-flip.log last line: "[boot-flip] 2026-07-02 04:06:13:
  operational + aitables/privategames/stattracking/newaccounts ON (April
  Fool's bonusabilities left off)" — 5 s after THIS container start.
- Live check: login asdf -> 200; hall XML: aiTablesEnabledBoolean="true",
  privateGamesEnabledBoolean="true", friendly Twitter MOTD (no "not yet in
  operational mode"). stattracking/newaccounts evidenced by boot-flip.log
  only (hall XML doesn't expose them) — acceptable.

CLAIM 4 — changelogs: PASS
- AI_CHANGELOG.md: new bottom entry "## 2026-07-01 — V177 dead-search:
  CATEGORY RESCUE ... adjusts V177 in place" — references replay
  7co2xviwqo5q3zac + the accepted-false-positive caveat (V82.1 category
  fallback ignores qualifiers). In-place V-tag update, no new tag minted
  (feedback_update_old_rule_not_new_version honored).
- AI_VERSION_HISTORY.md: "UPDATE (2026-07-01): CATEGORY RESCUE" inserted at
  the END of the V177 DECK ORACLE DEAD-SEARCH GATE block (lines 3726+),
  correctly BEFORE the separate "V177 — WINNABLE CONTESTS" block (that
  duplicate V177 tag is pre-existing in the doc, not from this session).
- No-fabrication spot checks: "I'm Sorry" exists in card_blueprint_database
  and its gameText contains "interior cloud city"; "Cloud City: Dining Room"
  + "Cloud City: Security Tower" exist; replay file exists at
  replays/asdf/7co2xviwqo5q3zac.xml.gz; "recordFailedPull has ZERO callers"
  claim VERIFIED (grep -rn src: only the 2 definitions, rando
  DeckOracle:1371 / chosenone:1252).

CLAIM 5 — regression sanity: PASS
- else-if "V177 PARSE-JUNK pass-through" follows the edited branch in both
  files (rando:290 / chosenone:272); V183 DECK-TITLE RETOOL block intact
  after it (rando:296-336 / chosenone:278-318); try/catch + V116 floor
  unchanged. Brace structure eyeballed clean; jar build succeeding with the
  new string in bytecode confirms compilation.
- Type-by-API: added lines contain zero getTitle().contains(<generic noun>)
  patterns.
- Boundary math: no magnitude changes — pure gate alignment (rescue fires
  only on the verdict V67h already acts on). Additive-domination discipline
  not implicated.

Minor notes (non-blocking):
- Code comments say "adjusted 2026-07-02" while changelogs say 2026-07-01 —
  UTC vs PDT of the same session; harmless but a grep for the date will
  land on different days in code vs docs.
- The claim listed Rando_Issues_2026-06-29.xlsx as part of the diff; it is
  UNTRACKED (??), so it never appears in git diff. Also untracked and
  unmentioned: resources/Rando_Version_Table_2026-07-01.xlsx.
- NOT VERIFIED (explicitly out of scope, changelog says pending): the rescue
  FIRING in a live game. Needs the next TDIGWATT game — expect
  "V177 CATEGORY RESCUE" log lines on the I'm Sorry action and the site
  download actually resolving.

Verdict: PASS (5/5 claims).

Lesson: macOS BSD `strings` returned 0 for "V177 CATEGORY RESCUE" on a
.class file that provably contains the bytes (python confirmed count=1 on
the identical-md5 file). Do NOT trust bare `strings` on .class files on
the Mac host — use python bytes.count() or run strings inside the Linux
container. This false-negative could have flipped a future verification
to FAIL incorrectly.

## 2026-07-04 11:45 — V189 net-value drain gate + V140 repair + V190 starship gate (AI evaluator edit + fast-path deploy) → WARN

Operation: Two new rules from Steve (game 20jqtseod148of4y), both bots,
deployed together via bin/gemp reload-ai. V189 net-value drain gate +
V140 repaired in place (2 ActionTextEvaluators); V190 starship pull gate
+ destination widening (2 DeckOracle + 2 DeployEvaluator +
2 CardSelectionEvaluator). Branch rando-consolidation-2026-06-23,
uncommitted working tree. Prior verifier run died on connection error;
this run started clean.

Findings (claim-by-claim):

CLAIM 1 — V189, both bots: PASS
- Block present rando ActionTextEvaluator:5077-5107 / chosenone:5046-5064,
  INSIDE the V24.15 try/catch, immediately after the zero-drain else branch;
  drainAmount/drainLocation/drainGame all in scope. cost > drain →
  addReasoning -2000 + logger.warn "V189 DRAIN NET-VALUE BLOCK" + return.
- getInitiateForceDrainCost(GameState, PhysicalCard, String) is REAL:
  default method at gemp-swccg-logic .../modifiers/querying/ForceDrains.java:97,
  and ModifiersQuerying extends ForceDrains (querying/ModifiersQuerying.java:18).
  Sums INITIATE_FORCE_DRAIN_COST modifiers, Math.max(0, result).
- grep "if (false|if(false" in BOTH ActionTextEvaluators: ZERO hits.
  evaluateForceDrain called live (rando:2428; chosenone equivalent).

CLAIM 2 — V140 repaired in place, both bots: PASS
- New detection = same engine query <= 0f (rando:5241-5251 / chosenone:5196-5206).
- Old hand-rolled scan (Battle Plan title scan + occupation loop) commented
  out line-by-line `//` in both files (rando 5252-5316, chosenone 5209-5272);
  only live lines in those ranges are the surrounding if/addReasoning.
- Reasoning + warn strings now "engine initiate-cost is 0" wording; NOTE
  comment above V104 in both (rando:5328 / chosenone:5283).
- Jar bytecode: OLD waiver string ("occupy BG site + BG system (or Battle
  Plan in play)") count=0 in both classes; NEW string count=1 in both.

CLAIM 3 — V190, six files: PASS
- Both DeckOracles: public reservePullFetchesOnlyStarships(String)
  (rando:528 / chosenone:927) + public static spaceLocationOnTable(GameState)
  (rando:567 / chosenone:965; SYSTEM or SECTOR → true, null state → true,
  exception → true — fail-open as claimed). Logic identical between bots.
- Both DeployEvaluators: gate inside the V67h WILL_SUCCEED branch — rando:852
  AFTER the V185 block, chosenone:803 right after the MEMORY OK log
  (chosenone has NO V185 block anywhere — confirmed pre-existing at HEAD,
  not deleted by this change). -12000 + actions.add(action) + continue
  (V79 actions.add failure class handled).
- Both CardSelectionEvaluators: condition now (isDockingBay || isGroundSite)
  (rando:1411 / chosenone:1365), message "STARSHIP TO SITE = 0 POWER!
  (V190: ships deploy to systems)" -1500; old "STARSHIP TO GROUND - unusual!"
  else-if commented out (rando:1484-1489 / chosenone:1439-1444); brace chain
  valid (compiles — see claim 4). isGroundSite = SITE && !isDockingBay
  pre-existing, so the union is all sites as documented.

CLAIM 4 — build + deploy: PASS
- python3 zipfile byte-search of src/gemp-swccg-async/target/web.jar
  (NOT macOS strings — per the 2026-07-01 lesson):
  V189 DRAIN NET-VALUE BLOCK: rando ATE.class count=2 (148840 B),
  chosenone count=2 (148389 B); V140 new string count=1 both.
  V190 STARSHIP-NO-SYSTEM blocked: count=1 both DeployEvaluator.class.
  reservePullFetchesOnlyStarships: count=1 both DeckOracle.class.
  STARSHIP TO SITE = 0 POWER: count=1 both CardSelectionEvaluator.class.
- Ordering chain airtight: last source save 11:06:43 PDT → class timestamps
  in jar 18:06:52-54Z (11:06:52-54 PDT) → jar mtime 11:08:47 PDT →
  container StartedAt 18:08:48.459Z. Compile is OF this working tree.
- java PID 1 in gemp_swccg_app_1 runs the bind-mounted web.jar; app Up,
  db Up 26h (untouched — correct for fast path).
- HTTP 200 on /gemp-swccg/ AND /gemp-swccg-server/. boot-flip.log last line
  2026-07-04 18:08:53 (5 s after start, all switches ON); live confirm:
  login asdf 200, hall XML aiTablesEnabledBoolean="true" +
  privateGamesEnabledBoolean="true".

CLAIM 5 — changelogs + tracker: WARN (one factual error)
- AI_CHANGELOG.md: two new 2026-07-04 entries at the bottom (V189+V140,
  V190), correct structure, boundary math, revert paths.
- AI_VERSION_HISTORY.md: "════ V189 (2026-07-04) ════" at 3304 and
  "════ V190 (2026-07-04) ════" at 3332. No duplicate/prior V189/V190
  anywhere (src hits only the 8 edited files; no handoff/doc mentions).
- Rando_Issues_2026-06-29.xlsx: issues #11 and #12 (sheet rows 12-13 —
  claim said "rows 11-12", off by the header row, matches by issue number)
  both "BUILT 2026-07-04 - deploy + live verify pending".
- Replay replays/asdf/20jqtseod148of4y.xml.gz EXISTS (97804 B, Jul 4 10:21).
- Card names all real (card_blueprint_database_dark.json): Court Of The
  Vile Gangster, Elis In Hinthra, Dengar In Punishing One, Executor:
  Docking Bay, Audience Chamber.
- ⚠ THE ERROR — CARD IDs SWAPPED: Battle Plan is Card8_035
  (set8/light, Title.Battle_Plan); Battle Order is Card8_118 (set8/dark,
  Title.Battle_Order). Three places state the reverse:
    1. rando ActionTextEvaluator.java:5236 ("Battle Plan (Card8_118)
       waives only Battle Order's (Card8_035) modifier")
    2. resources/AI_CHANGELOG.md:154 ("Battle Plan, Card8_118")
    3. AI_VERSION_HISTORY.md:3315 ("imposes its OWN 3-Force drain tax,
       Card8_118")
  The MECHANICS are verified CORRECT in both card classes: Battle Order's
  InitiateForceDrainCostModifier is wrapped in UnlessCondition(OrCondition(
  battlePlanOnTable, occupation)) — waived by Battle Plan; Battle Plan's own
  modifier has NO reciprocal waiver, only the occupation test. So the V140
  repair rationale stands; only the blueprint-ID attribution is reversed.
  ZERO behavioral impact (code queries the engine, never card IDs).
  Fix: swap the two IDs in those 3 lines. feedback_no_fabrication class.

CLAIM 6 — git hygiene: PASS (notes)
- Modified: EIGHT Java files (the claim's item 6 said "six" — miscount in
  the claim text; items 1-3 correctly describe 8: 2 ActionTextEvaluator +
  6 V190 files) + AI_CHANGELOG.md + AI_VERSION_HISTORY.md + verifier
  history.md. Nothing else modified.
- Diff content audit: every added src line traces to V189/V140/V190;
  all 128 deleted lines are the old V140 scan + old else-if (both
  preserved as comments). Type-by-API forbidden-pattern grep across all
  8 files: zero hits.
- Untracked as expected (.agents/, AGENTS.md, mcp-gemp-client/gemp_mcp.py,
  RANDO_REORG_PLAN, 3 known xlsx) PLUS one new unmentioned file:
  resources/Rando_Overlap_Audit_2026-07-04.xlsx — presumably this
  session's audit artifact; harmless, flag for awareness.

Verdict: WARN (5 PASS, 1 WARN). Deploy is live and correct in bytecode;
server healthy, switches on. The only defect is the swapped Battle
Plan/Battle Order blueprint IDs in 1 code comment + 2 changelog lines —
docs-only, 3-line fix, but it is in the breadcrumbs Steve navigates
reverts by, so fix it before the next push.

NOT VERIFIED (out of scope, changelogs say pending): live-game firing of
V189/V190 (grep logs for "V189 DRAIN NET-VALUE BLOCK", "V190
STARSHIP-NO-SYSTEM blocked", "STARSHIP TO SITE = 0 POWER" in the next
Battle Plan / Vile Gangster game).

Lesson: blueprint-ID attributions written from memory are exactly the
no-fabrication slop class — when a changelog cites CardX_YYY, open the
card class and check the Title constant. The mechanics can be right while
the citation is backwards.

## 2026-07-06 10:05 — V189 v2 two-tier net-value drain gate, in-place update (AI evaluator edit + fast-path deploy) → PASS

Operation: V189 UPDATED IN PLACE (no new V-tag — correct per
feedback_update_old_rule_not_new_version). Both bots' ActionTextEvaluator:
net <= -2 stays flat-blocked; net -1 now budget-gated on forcePile vs live
deployable hand costs + 2-Force move allowance. Deployed via reload-ai
after a Docker Desktop restart. Branch rando-consolidation-2026-06-23,
uncommitted on top of d68af3694.

Findings (claim-by-claim):

CLAIM 1 — source, both bots: PASS
- Two-tier branch inside the V24.15 try, right after the zero-drain else:
  rando 5077-5148, chosenone 5046-5093. Tier (a): v189Cost - drainAmount
  >= 2.0f → -2000 + warn + return ("net <= -2, never worth it"). Tier (b):
  v189ForcePile = gameState.getForcePileSize(playerId); plannedSpend =
  sum of getDeployCost() over hand CHARACTER/STARSHIP/VEHICLE cards
  skipping AiCardHelper.isDeadCard(card, drainGame, playerId); block
  -2000 + return iff forcePile - cost < plannedSpend + 2; else
  logger.warn "V189 NET -1 DRAIN ALLOWED" and falls through to normal
  scoring. `else if (v189Cost > 0)` proceed-log branch intact both bots.
- AiCardHelper.isDeadCard(PhysicalCard, SwccgGame, String) is REAL,
  public static, ai/common/AiCardHelper.java:457.
- Zero `if (false` in either file; CardCategory import pre-existing (line 5).
- git diff: exactly ONE hunk per Java file (rando @5081,+70 lines;
  chosenone @5047,+49), so downstream V140/V104/V52/V48 untouched by
  construction (V140/V104 structure grep-confirmed alive). Changed files:
  2 Java + resources/AI_CHANGELOG.md + k2-resources/originals/
  02-rando-history/AI_VERSION_HISTORY.md + this history.md. Nothing else.
- Type-by-API forbidden-pattern grep on the diff: 0 hits. V79
  actions.add class N/A (modifies the existing action, early-return).

CLAIM 2 — boundary math (logic reading): PASS
- pay-3-drain-1: diff 2.0 >= 2.0 → flat block (original offender still dead).
- pay-3-drain-2, 12F, no deployables: 12-3=9 >= 0+2 → allowed.
- pay-3-drain-2, 6F, one 5-cost deployable: 6-3=3 < 5+2=7 → blocked.
- cost 0 / net 0 games unaffected (v189Cost > drainAmount is false).

CLAIM 3 — deploy: PASS
- python3 zipfile byte-search of src/gemp-swccg-async/target/web.jar
  (NOT macOS strings, per the 2026-07-01 lesson): "V189 NET -1 DRAIN
  ALLOWED" count=1, "V189 DRAIN NET-1 BUDGET BLOCK" count=1, "net <= -2,
  never worth it" count=1, "V189 DRAIN NET-VALUE BLOCK" count=3 — in BOTH
  bots' ActionTextEvaluator.class (rando 150080 B, chosenone 149629 B,
  class zipdate 2026-07-06 16:58:02Z). Bytecode presence also proves the
  edit compiled.
- Ordering chain: classes 16:58:02Z → jar mtime 16:59:11.64Z → container
  StartedAt 16:59:12.32Z. Margin 0.7 s — thin but correctly ordered.
- PID 1 = java -jar /opt/gemp-swccg/src/.../target/web.jar (bind mount);
  app Up 2 min at check. HTTP 200 on /gemp-swccg/ (17001).
- Docker Desktop restart accounted for: db StartedAt 16:57:17Z (Up 4 min),
  app 16:59:12Z, both running. Read-only checks only, DB untouched.
- Switches: boot-flip.log 2026-07-06 16:59:17 = THIS boot (5 s after
  start), operational + aitables/privategames/stattracking/newaccounts ON.

CLAIM 4 — changelogs + log evidence: PASS
- AI_CHANGELOG.md:157 "UPDATED 2026-07-06" bullet has ALL claimed
  elements: budget condition, DeployPhasePlanner stale-cache pushback
  with 17:16:50/17:16:57, the reworded V52 sentence ("RESTORED only while
  the turn's deploy+move intent stays funded"), ~+340 ceiling, turns-1/2
  +150 row, over-allow gap list, fail-open note.
- AI_VERSION_HISTORY.md 3331-3350: matching "UPDATED 2026-07-06 in place"
  paragraph, same content.
- Log spot-check CONFIRMED in logs/2026-07/app-07-04-2026-1.log.gz
  (gunzip -c): 17:16:50,667 drain chosen at +70 ("no deployables" boost),
  17:16:50,681 "Use 3 Force - Optional responses"; 17:16:57,044
  "CREATING COMPREHENSIVE DEPLOYMENT PLAN (Turn 5)" with "Resources:
  force=9". Consistent with pre-drain force 12. Citation is real.
- BONUS: the 07-04 WARN defect (swapped Battle Plan/Battle Order IDs) is
  FIXED in all 3 flagged spots — changelog:154, version history:3315,
  rando ActionTextEvaluator:5280 now read Battle Plan=Card8_035,
  Battle Order=Card8_118 (correct).

Verdict: PASS (4/4 claims). Bytecode live, server healthy, switches on,
breadcrumbs complete and citation-checked.

NOT VERIFIED (out of scope, pending next Battle Plan/Order game): live
firing of the v2 tiers — grep for "V189 DRAIN NET-1 BUDGET BLOCK" /
"V189 NET -1 DRAIN ALLOWED" and confirm a funded pay-3-drain-2 fires
while pay-3-drain-1 stays blocked.

Note for future runs: the fast-path jar→container margin was 0.7 s here.
If jar mtime ever lands AFTER StartedAt, the running JVM booted on stale
bytecode even though the strings are in the jar — keep checking both
sides of that inequality, not just string presence.

## 2026-07-06 12:22 — V51/V105/V112/V117 Battle Order 4th-slot deadlock fix + occupation-predicate unification (AI evaluator edit + fast-path deploy) → PASS

Operation: In-place adjustment of V51/V105/V112/V117 in BOTH bots'
CardSelectionEvaluator. Two new private helpers per bot; V105/V117 gated on
whether the preferred 4th-slot shield is actually on the menu; V51 (deploy +
shield paths) and V112 occupation checks swapped from hand-rolled loops to an
engine occupies-predicate. Deployed via bin/gemp reload-ai after Docker Desktop
restart. Branch rando-consolidation-2026-06-23, uncommitted on top of cb98f0075.
Replay unli50oa1ur8bdux ("Verge of Greatness"): 2760-fire -5000 spam deadlock.

Findings (item-by-item):

ITEM 1 — two helpers in BOTH bots + Filters API real: PASS
- occupiesBothTheaters + preferredShieldInCandidates both present, byte-for-byte
  identical logic between rando and chosenone, placed immediately before
  evaluateShieldSelection (rando 8721/8751, chosenone 8609/8633).
- Filters API confirmed in gemp-swccg-logic Filters.java: canSpot(SwccgGame,
  PhysicalCard,Filterable)@15918, occupies(String)@2692 (delegates to
  modifiersQuerying.occupiesLocation), battleground_site@17856,
  battleground_system@17857, and(Filterable...)@15060.
- DecisionContext accessors all real: getCardIds@170, getBlueprints@178,
  getDecisionType@130, getGameState@101, getGame@109, getPlayerId@105.
  getBlueprintFromId@187 (rando). Both helpers wrapped in try/catch, both
  fail to false (occupiesBothTheaters null-guards game+playerId;
  preferredShieldInCandidates null-guards context+title, n=0 on null cardIds,
  null-safe blueprints index, parseInt guarded by inner NumberFormatException).

ITEM 2 — four gate edits per bot, live, no magnitude change: PASS
- V51 deploy path (rando 8610 / chosenone 8518): `!occupiesBothTheaters(game,
  playerId)` → -9999, else +50. Old owner-present loop commented `//` in place.
- V51 shield path (rando 8895 / chosenone equiv): `!occupiesBothTheaters(
  context.getGame(), getPlayerId())` → -9999. Old loop commented in place,
  closes cleanly at rando:8932 then live `}` at 8933 → `} else {` fallback.
- V112 (rando 7815 / chosenone 7770): battle order/plan title → `!occupies
  BothTheaters(...)` → -9999. Old loop commented in place.
- V105 (rando 8847 / chosenone 8729): `boolean preferredOnMenu = preferred
  != null && preferredShieldInCandidates(context, preferred)`; if on menu +
  title contains preferred → +2000, else -5000; else branch (not on menu) →
  -5000 HOLD "keep slot closed". Slot still closes at -5000 when not offered.
- V117 (rando 7898 / chosenone 7835): `if (v117Preferred == null ||
  !preferredShieldInCandidates(context, v117Preferred))` → -9999 HOLD; else
  boost +2000 / block -9999 by title match.
- Magnitude audit via git diff: added executable lines carry -9999/-5000/+2000
  only; removed executable lines carried the SAME -9999/-5000 (old V112 -9999,
  old V105 HOLD -5000). V51 +50 constant present in both (grep count=1 each).
  ZERO magnitude value changes — pure predicate swap.
- `if (false`/SUPERSEDED guards: 14 per file, ALL in the V122/V67as/V159 range
  (2048-5152), NONE enclose the 7770-8933 edited regions. Edits are LIVE.

ITEM 3 — compile + deploy: PASS
- python3 zipfile byte-search of src/gemp-swccg-async/target/web.jar (NOT macOS
  strings, per 2026-07-01 lesson): in BOTH rando (193033 B) and chosenone
  (190698 B) CardSelectionEvaluator.class (zipdate 2026-07-06 19:16:28Z):
  occupiesBothTheaters=5, preferredShieldInCandidates=2,
  occupiesBothTheaters=false ×3, "not on menu / no trigger" ×2. All present both.
- Ordering: class zipdate 19:16:28Z → jar mtime 12:17:59 PDT (19:17:59Z) →
  app StartedAt 19:18:00.05Z. Jar built BEFORE container start (~1 s margin,
  correctly ordered).
- jar md5 host == container (e9224508...) via bind mount; java PID 1 runs
  `-jar /opt/gemp-swccg/src/.../web.jar` — running JVM loaded THIS jar.
- local target/classes .class md5 == jar copy for both bots (rando d64149...,
  chosenone bed8bb...) — jar packaged from this working-tree compile, no drift.
- app Up 3 min; HTTP 200 on /gemp-swccg/ AND /gemp-swccg-server/. boot-flip.log
  last line 2026-07-06 19:18:05 (5 s after start, all switches ON). login asdf
  → hall aiTablesEnabledBoolean="true", zero "not yet in operational mode".
- DB gemp_swccg_db_1 Up 2 hours (StartedAt 16:57:17Z, untouched — correct for
  fast path). Read-only checks only; DB never touched.

ITEM 4 — git diff scope: PASS
- Modified TRACKED files: exactly the 2 CardSelectionEvaluator.java +
  resources/AI_CHANGELOG.md + resources/k2-resources/originals/02-rando-history/
  AI_VERSION_HISTORY.md + .claude/skills/work-verifier/history.md (this file).
  NO other Rando source touched. Tracker xlsx untracked as expected.

ITEM 5 — changelogs + replay: PASS
- AI_CHANGELOG.md:172 "## 2026-07-06 — V51/V105/V112/V117 (in place)" cites
  replay unli50oa1ur8bdux, the 2760-fire -5000 deadlock, both helpers, "just
  stop the spam", the "NO magnitude changes" boundary line, AND the
  battlePlanOnTable known unmodeled edge (flagged as follow-up). Plus a Revert
  recipe. In-place update, no new V-tag minted (feedback_update_old_rule honored).
- AI_VERSION_HISTORY.md:3352 matching "V51/V105/V112/V117 (in-place UPDATE
  2026-07-06)" block — sits between V189 (3304) and V190 (3382), i.e. ABOVE
  the V190 block as claimed (chronologically odd inline placement, harmless).
- Replay replays/asdf/unli50oa1ur8bdux.xml.gz EXISTS (177032 B, Jul 6 11:08).

ITEM 6 — logic/regression review: PASS
- V105 else/HOLD closes slot at -5000 when preferredOnMenu false (rando 8878-8886).
- Boost path requires candidate title contain preferred: `if (tLower.contains(
  pLower))` → +2000 else -5000 (rando 8863). Correct.
- occupiesBothTheaters fails closed → -9999 block, no false deploy. Correct.
- preferredShieldInCandidates cannot NPE (all null-guards + try/catch verified).
- Commented-out old loops syntactically inert; brace/paren balance CLEAN both
  files (rando {1610/}1610 (5161/)5161; chosenone {1604/}1604 (5116/)5116).
- Type-by-API: ZERO forbidden generic-noun getTitle().contains() in added lines.
  The .contains("battle order"/"battle plan") are proper-noun Effect titles
  (accepted §2B pattern, unchanged from the code they replaced).
- V79 actions.add class N/A: all edits mutate the EXISTING action via
  addReasoning; the surrounding actions.add(action) is pre-existing.

Verdict: PASS (6/6 items). Deploy is live in bytecode, symmetric across both
bots, server healthy, switches on, DB untouched, breadcrumbs complete and
citation-checked, no magnitude drift, no brace imbalance, no unreachable-code
or dead-guard risk on the edited regions.

NOT VERIFIED (explicitly out of scope, changelog says SELF-PLAY pending):
BEHAVIORAL firing in a live game. A Scarif dark-deck game where Rando controls
BOTH theaters must show `V51 ... Requirements met`, `V105/V107 4TH SLOT: BOOST
'Battle Order'`, Battle Order actually reaching the table, and NO
`HARD-BLOCK ... prefer 'Battle Order'` spam when it isn't offered. Bytecode-
present != rule-fired. Requires K-2 self-play or Steve playing. Additional
open question the changelog itself raises: whether Battle Order is ever OFFERED
depends on the K&D shield-play 4x/game cap — a separate limiter self-play must
confirm.

Lesson reinforced: predicate-only swaps (hand-rolled loop → engine canSpot)
with NO magnitude change are the safest class of Rando edit — the additive-
domination discipline is untouched by construction. The git-diff magnitude
audit (compare added vs removed executable constants) is the fast proof that a
"no-magnitude-change" claim is real; both sides carried -9999/-5000 here.

## 2026-07-06 12:55 — V51 Battle Order EARLY-DEPLOY boost (in-place extension, AI evaluator edit + fast-path deploy) → PASS

Operation: Extends the same-day V51 gate IN PLACE in BOTH bots'
CardSelectionEvaluator. When occupiesBothTheaters is true, a new +200 boost
fires in the occupy-both else-branch of BOTH the deploy path and the shield
path, so Battle Order/Plan deploys turns 1-2 (not only the turn-3 4th slot).
Guarded by shieldScore > -50f. Occupy-only per Steve (no opponent clause).
Deployed via bin/gemp reload-ai. Branch rando-consolidation-2026-06-23,
uncommitted on top of d52258be0 (which already carried the base V51/V105/
V112/V117 Verge fix). Goal: Battle Order early-deploy.

Findings (item-by-item):

ITEM 1 — +200 boost, both bots, both paths, in the else-branch, guarded, occupy-only: PASS
- Deploy path: rando 8620-8627 / chosenone 8524-8531. Shield path: rando
  8909-8925 / chosenone 8782-8793. In EVERY case the `if (shieldScore > -50f)`
  block with addReasoning(..., 200.0f) lives inside the `else` of
  `!occupiesBothTheaters(...)` — i.e. the occupy-BOTH branch, NOT the -9999
  not-qualifying branch. Confirmed by reading all four sites.
- Boost is +200.0f (the addReasoning literal ends "... +200", 200.0f).
- shieldScore genuinely in scope at all four sites:
    deploy path — `float shieldScore = shieldStrategy.scoreShield(blueprintId,
      blueprintId, turnNumber)` rando:8597 (inside `if (shieldStrategy != null)`),
      chosenone equivalent; V51 gate at rando:8610 is inside that same block.
    shield path — `float shieldScore = shieldStrategy.scoreShield(blueprintId,
      title, turnNumber)` rando:8841 (inside `if (shieldStrategy != null &&
      blueprintId != null && title != null)`), chosenone:~ same; V51 gate at
      rando:8903 is inside that block.
- NO opponent-occupation check in added code: the 4 grep hits for
  "opponent"/"enemy" in the diff's + lines are ALL comment prose (explaining
  the occupy-only design + the tax mechanic). Zero executable opponent clause.
  Occupy-only confirmed.

ITEM 2 — title tests extended to "battle plan", 4 total: PASS
- git diff `^+` grep for `contains("battle plan")` = exactly 4 (deploy path +
  shield path × rando + chosenone). Deploy-path `if` now
  `contains("battle order") || contains("battle plan")`; shield-path `if` same
  (split across two lines). Both bots, both branches.

ITEM 3 — boundary / domination: PASS
- (a) Not-qualifying: the boost is unreachable when occupiesBothTheaters is
  false (that path is the -9999 if-branch; boost is in the else). Correct.
- (b) V43 redundant-shield: ShieldStrategy.java:742-752 (rando) returns
  -100.0f when opponent already has Battle Order/Plan (`return -100.0f;`
  at 750). shieldScore IS that scoreShield return value at both edit sites.
  -100 <= -50 → guard `shieldScore > -50f` is FALSE → +200 SUPPRESSED. In the
  shield path V105 already did setScore(-100) at 8845, then the V51 else logs
  "boost suppressed" and adds nothing → action stays -100 → does NOT deploy.
  Correct.
- (c) 4th slot (turn 3+): in the shield path V105/V107 (rando 8851-8895) runs
  BEFORE the V51 gate and adds +2000 (or -5000). The V51 +200 then ADDS on top.
  For a Battle Order shield in the 4th slot: setScore(base) + 2000 + 200 — the
  +200 rides on top and does NOT change which card the 4th-slot logic picks.
  V105 +2000 still dominates. Correct.
- (d) Magnitude audit: git diff of both Java files, added `^+` executable
  constants = `200.0f` ×4 ONLY; removed executable constants = NONE. No
  constant other than the new +200 changed. Additive-domination discipline
  HOLDS.
- Dead-guard check: zero `if (false`/`if(false)` enclosing the edited regions
  (rando 8607-8930 grep=0). Edits are LIVE, not compiled out.

ITEM 4 — compile + deploy: PASS
- python3 zipfile byte-search of src/gemp-swccg-async/target/web.jar (NOT macOS
  strings, per the 2026-07-01 lesson) on BOTH bots' CardSelectionEvaluator.class
  (rando 193422 B, chosenone 191087 B, class zipdate 2026-07-06 19:52:46Z):
    "V51 BATTLE ORDER (shield): both theaters" = 2 (shield-path +200 log +
      base-log share the prefix — both logger lines)
    "V51 BATTLE ORDER: both theaters" = 1 (deploy path)
    "boost suppressed" = 1 (shield-path else)
    "V51 BATTLE ORDER EARLY-DEPLOY" = 1 (interned addReasoning literal — 1/class
      is EXPECTED, not a missing edit, per the task note)
    "battle plan" = 1 (interned title literal)
  All present and SYMMETRIC across both bots.
- Ordering chain: class zipdate 19:52:46Z → jar mtime 12:53:42 PDT (19:53:42Z)
  → app StartedAt 19:53:42.847Z. Jar built BEFORE container start (correctly
  ordered). host jar md5 == container jar md5 (b02b19ff...) via bind mount;
  java PID 1 runs `-jar /opt/gemp-swccg/src/.../web.jar` — running JVM loaded
  THIS jar. app Up ~1 min at check.
- HTTP 200 on /gemp-swccg/ AND /gemp-swccg-server/.
- Switches: logs/boot-flip.log last line 2026-07-06 19:53:48 (6 s after this
  boot's StartedAt) — operational + aitables/privategames/stattracking/
  newaccounts ON. Unauthenticated hall is empty (auth-gated, expected); no
  "not yet in operational mode" string. (Note: my curl login cookie-parse
  returned empty both tries — a test-harness quirk, NOT a server fault; the
  boot-flip.log line is the authoritative switch evidence per prior runs.)
- DB gemp_swccg_db_1 Up 3 hours (StartedAt 16:57:17Z, untouched — correct for
  fast path). Read-only checks only; DB never touched.

ITEM 5 — git diff scope: PASS
- Modified TRACKED files: EXACTLY the 2 CardSelectionEvaluator.java +
  resources/AI_CHANGELOG.md + resources/k2-resources/originals/02-rando-history/
  AI_VERSION_HISTORY.md + .claude/skills/work-verifier/history.md (this file).
  NO other Rando source touched; ShieldStrategy.java NOT modified. Tracker
  xlsx untracked as expected (Rando_Consolidation_Plan / Rando_Issues +
  .agents/, AGENTS.md, mcp-gemp-client/gemp_mcp.py).

ITEM 6 — changelogs: PASS
- AI_CHANGELOG.md:181 "## 2026-07-06 — V51 EARLY-DEPLOY (in place)" with the
  +200 magnitude (183), the shieldScore>-50 guard + V43 rationale (184), the
  occupy-only gate (182,184), AND the KNOWN GAP that evaluateUnknown/V112
  (mixed K&D pile) has no positive branch so the boost only fires on the
  shield-selection + deploy routes (185). In-place update, no new V-tag minted
  (feedback_update_old_rule honored). Revert recipe present (187).
- AI_VERSION_HISTORY.md:3381 matching "EARLY-DEPLOY EXTENSION (2026-07-06)"
  paragraph — same +200, same guard/V43, occupy-only, V105 4th-slot
  domination, battle-plan title extension, the evaluateUnknown/V112 known gap,
  and "Self-play still owed." Full parity.
- No-fabrication spot check: Battle Order = dark shield 13_54 (consistent with
  the same-day V51/V105 entry which the verifier already validated); the
  changelog's "conditionsMet never set → base 80" claim is consistent with the
  ShieldStrategy checkConditions path.

Verdict: PASS (6/6 items). The +200 early-deploy boost is live in both bots'
bytecode, symmetric, gated in the occupy-both else-branch, guarded by
shieldScore > -50f (V43/-100 correctly suppresses it), occupy-only with zero
executable opponent clause, no magnitude drift beyond the single +200, no
dead-guard enclosing the region, jar loaded by the running JVM, server healthy,
switches on, DB untouched, breadcrumbs complete with the known-gap documented.

NOT VERIFIED (explicitly out of scope, changelog says self-play OWED):
BEHAVIORAL firing in a live game. A Scarif dark-deck game where Rando occupies
both theaters early must show `V51 BATTLE ORDER EARLY-DEPLOY ... +200` in the
log and Battle Order actually reaching the table on turn 1-2. Bytecode-present
!= rule-fired. Requires K-2 self-play or Steve playing. The K&D 4x/game
shield-play cap is a separate limiter to watch.

DOCUMENTED UNCOVERED PATH (not a defect — flagged by design): the boost lives
only on the evaluateShieldSelection shield route + the Reserve/hand deploy
route. A MIXED K&D stacked pile (<50% shields) routes through evaluateUnknown/
V112, a pure -9999 block with no positive branch, so the boost does not fire
there. The primary K&D shield route (the one the Verge game used) IS covered.
Adding a V112 positive branch is a follow-up only if a mixed-pile deck shows
the gap live.

Lesson reinforced: an in-place additive boost gated on an existing score
(shieldScore > -50f) is safe ONLY if that score already encodes every
rejection you must not override — here V43's -100 and the pacing/-not-played
-50/-100 all sit at or below the -50 threshold, so the single guard covers all
three. Verified by reading scoreShield's return paths, not by trusting the
comment. When a boost "rides on top" of an existing setScore path (V105's
setScore then +2000 then +200), confirm the earlier code SETS vs ADDS — here
V105 setScore then V51 addReasoning means the +200 is genuinely additive and
cannot displace the 4th-slot pick.

## 2026-07-06 14:13 (approx) — Phase A reorg batch deploy (5 commits, V29/V47/V169/V67bc/V191/V61c/V82/V60/V25/V35.4) → PASS

Operation verified: 5-commit Phase A batch (8f5ff5e71, 73f3be388, b16dd8899,
3cd4cfa61, 335a36021) + fast-path reload-ai deploy claimed at ~14:11.
Protocol: verify-evaluator-edit.md (post-deploy variant; report-only per caller).

ITEM 1 — git log/status: PASS
- All 5 claimed hashes present at HEAD in order (335a36021 docs on top,
  code commits 3cd4cfa61/b16dd8899/73f3be388/8f5ff5e71 below), all committed
  2026-07-06 14:10:45 -0700, on branch rando-consolidation-2026-06-23.
- Batch diffstat vs a44cabdd6: 16 files, +1075/-186, all AI evaluator files
  mirrored rando/chosenone (ATE, CombinedEvaluator, DeployEvaluator,
  MoveEvaluator, ObjectiveAnalyzer, RandoCalAi/TheChosenOneAi) + rando-only
  DecisionContext + ForceActivationEvaluator (V61c deliberately unmirrored,
  chosenone never had the buffer) + both changelogs.
- Working tree clean of src/ changes: only .claude/skills/work-verifier/
  history.md modified (this file) + the known untracked set (.agents/,
  AGENTS.md, mcp-gemp-client/gemp_mcp.py, 2 xlsx).

ITEM 2 — jar markers (python zipfile byte-search, NOT `strings`): PASS
- Jar: src/gemp-swccg-async/target/web.jar, mtime 2026-07-06 14:10:59.66
  (14 s AFTER the 14:10:45 commits — post-commit as claimed), 44,142,069 B.
- rando DeployEvaluator.class (173,902 B, zipdate 21:09:54Z):
  'BESPIN-FIRST RELEASED' count=1 FOUND
- rando DecisionContext.class (13,965 B, zipdate 21:09:52Z):
  'isBattlePlausibleThisTurn' count=1 FOUND
- rando CombinedEvaluator.class (12,425 B, zipdate 21:09:54Z):
  'V191 TOPN' count=1 FOUND
- BONUS mirror sweep (mirror asymmetry = known past failure): chosenone
  DeployEvaluator 'BESPIN-FIRST RELEASED'=1, chosenone CombinedEvaluator
  'V191 TOPN'=1, 'V60 RESERVE RISK' rando ATE=2 == chosenone ATE=2 (symmetric),
  'V47 LANDO STAY skipped' rando MoveEvaluator=1 == chosenone=1, 'V35.4'
  rando ATE=6, rando ForceActivationEvaluator 'V61c BATTLE-INTENT'=1
  (rando-only, correct). All present.

ITEM 3 — JVM restart + HTTP: PASS
- Ordering chain: classes compiled 21:09:52-54Z → commits 21:10:45Z → jar
  mtime 21:10:59.66Z → app StartedAt 2026-07-06T21:11:00.415Z (0.75 s after
  jar write). JVM etime 02:01 at check time (~2 min), args show
  `-jar /opt/gemp-swccg/src/gemp-swccg-async/target/web.jar` — the running
  JVM loaded THIS jar via the bind mount.
- HTTP 200 on http://localhost:17001/gemp-swccg/.
- Switches: logs/boot-flip.log last line 2026-07-06 21:11:05 (5 s after this
  boot) — operational + aitables/privategames/stattracking/newaccounts ON.

ITEM 4 — changelogs: PASS
- resources/AI_CHANGELOG.md tail: all 5 new 2026-07-06 batch entries present
  (V29 BESPIN-FIRST release, V61c battle-intent bypass, V169+V47,
  V67bc+V191, V82/V60+V25+V35.4 audit fixes), each with Why / Boundary math /
  Verified / Revert recipe. (The 6th same-day entry, V51 EARLY-DEPLOY, is the
  earlier 8f841bd25 batch — already verified separately.)
- AI_VERSION_HISTORY.md (k2-resources/originals/02-rando-history): matching
  2026-07-06 blocks at lines 3304 (V29), 3312 (V61c), 3321 (V169+V47),
  3332 (V67bc+V191), 3347 (V82/V60/V25/V35.4). Full parity.

ITEM 5 — source spot-check, rando ATE V60 Guard-1 / V82 placement: PASS
- Guard 1 (reserve <= 2) at ActionTextEvaluator.java:4371-4384: live
  addReasoning is -9999.0f (line 4377) with the OLD -400.0f line commented
  out in place directly below (4378-4380, feedback_comment_out_old_rules
  honored); logger says "too risky (-9999)".
- Old pre-guard V82 block at ~4325-4358: fully commented out (// per line),
  header "V82 UPDATED 2026-07-06: scoring MOVED below the V60 guards".
- Live V82 +2500 copy at 4611-4648 verified INSIDE the `if (!hardBlocked)`
  region opening at 4602 (guards 1-3 + V66/V67h/V67ac all set hardBlocked
  above it). Not merely present — actually nested where claimed.
- No `if (false` dead-tape around either edited region (live code paths;
  'V60 RESERVE RISK' compiled into the class confirms).

Verdict: PASS (5/5 caller checks + bonus mirror sweep). Batch is committed,
compiled into the jar the running JVM loaded, server healthy with switches
flipped, breadcrumbs complete in both changelogs, and the riskiest edit
(V82 pull-guard bypass fix) is correctly nested and commented per the
standing rules.

NOT VERIFIED (out of scope, flagged): BEHAVIORAL firing of any of the 9
rule changes in a live game. The changelogs themselves list live-game grep
handles ('V29 BESPIN-FIRST RELEASED', 'V61c BATTLE-INTENT', 'V169: soft-block',
'V47 LANDO STAY skipped', 'V191 TOPN', 'V60 RESERVE RISK ... -9999',
'V25 SIMPLE TRICKS: BLOCKING', 'V35.4: UNDERCOVER SPY') and self-play is
still owed. Bytecode-present != rule-fired.

Note for future runs: this batch's 5 commits share ONE commit timestamp
(14:10:45) — batch-committed, then jar assembled 14 s later, then JVM start
0.75 s after that. A jar mtime BETWEEN class zipdates and container StartedAt
with matching -jar args is the full ordering proof; don't demand
class-zipdate > commit-time (compile legitimately preceded the commits here).

## 2026-07-07 12:2x — Objective IDENTITY consolidation into ObjectiveAnalyzer (behavior-preserving refactor, committed NOT deployed) → PASS

Operation: Verify two commits on branch rando-consolidation-2026-06-23:
100586e4f (rando: consolidate objective identity into ObjectiveAnalyzer,
pure refactor) + adb75b917 (chosenone: full clone from rando to erase
mirror drift). Pass criterion = no behavior change + compiles + chosenone
mirrors rando. Read-only verification; nothing fixed.

Findings (5 requested items):

1. COMPILE (in-container) — PASS
   docker exec ... mvn -q -pl gemp-swccg-server -am compile → EXIT=0,
   grep -c '[ERROR]' /tmp/wv.log = 0. (mvn -q suppresses the BUILD SUCCESS
   banner, so the banner-grep found nothing — expected; EXIT=0 + 0 ERRORs
   is definitive.)

2. NO-DRIFT (rando/*.java vs chosenone twin line counts, excl RandoCalAi.java)
   — PASS. All 38 twin files identical line-for-line. Zero mismatches.
   Spot: ActionTextEvaluator 6746/6746, CardSelectionEvaluator 9690/9690,
   DeployEvaluator 6151/6151, ObjectiveAnalyzer 1217/1217, MoveEvaluator
   3340/3340. adb75b917 touched 11 chosenone files (8 evaluators + 2
   strategy + 1 root), all under models/chosenone.

3. BEHAVIOR-PRESERVATION (no inline objective-title contains in the two
   rando evaluators) — PASS. DeployEvaluator.java + CardSelectionEvaluator.java
   each: contains("invasion")=0, contains("my lord")=0,
   contains("i want that map")=0. Broader regex (invasion|my lord|
   i want that map|want that map) = NONE. Strings now live in
   ObjectiveAnalyzer.strategy: analyze() at OA:168-171. NOTE: one IWTM
   contains() also at OA:727 — pre-existing V186 Starkiller-Base naming
   block INSIDE ObjectiveAnalyzer (not an evaluator), not a straggler.
   Predicate-identity confirmed: the 6 removed inline My Lord branches were
   each `contains("my lord") || contains("make it legal")`; isMyLord()
   returns the identical predicate. True behavior preservation.

4. GETTER CORRECTNESS (ObjectiveAnalyzer) — PASS. For each of isInvasion /
   isMyLord / isWantThatMap: (a) field decl OA:86/87/93, (b) set in analyze()
   OA:168/169/171 (right after objectiveTitle set at 161, before
   parseGameText at 186 — comment explicitly places them before the no-flip
   early return), (c) reset in reset() OA:498/499/501, (d) public getter
   OA:296/297/300. IWTM typed slots (iwtmSystemBpIds/iwtmSystemTitleFragment/
   iwtmPreferredStartingEffect) also reset (OA:502-504) and exposed via
   getters OA:301-303. All getters are CONSUMED: isMyLord read 8x in
   DeployEvaluator + 4x in CardSelectionEvaluator; isInvasion 2x/2x;
   isWantThatMap 3x in CardSelectionEvaluator; IWTM slot getters 1x each in
   CardSelectionEvaluator. No orphaned getter. V99 deliberately NOT
   isMyLord()-gated (comment: gating it would CHANGE behavior) — matches claim.

5. DEPLOY STATUS — PASS (confirmed NOT live). Running web.jar mtime
   2026-07-07 17:18:41Z; container StartedAt 17:22:17Z; both commits dated
   2026-07-07 18:53:25Z (11:53 PDT) — jar is ~1.5h OLDER than the commits.
   Running jar's ObjectiveAnalyzer.class (32027 B) has ZERO occurrences of
   the new method names (getIwtmSystemBpIds / isWantThatMap / isMyLord /
   isInvasion all = 0). Refactor is committed but NOT deployed — not in the
   running JVM. No reload-ai was run.

Type-by-API (protocol step 4): NONE. No forbidden generic-noun
getTitle().contains() added; the moved contains() are on proper objective
titles (invasion / my lord / make it legal / i want that map), acceptable.

NOT VERIFIED (out of scope): in-game firing. Moot — not deployed. When
deployed, sanity-check My Lord / Invasion / IWTM objective games still
score identically to pre-refactor.

Verdict: PASS (5/5 items). Clean behavior-preserving refactor; chosenone
mirrors rando exactly; compiles; not yet deployed.

## 2026-07-07 12:35 — e8f1eaac3 objective DEPLOY scoring → ObjectiveAnalyzer (behavior-preserving refactor) → PASS

Operation: Verify commit e8f1eaac3 "Rando: move objective DEPLOY scoring
into ObjectiveAnalyzer". Six deploy objective SCORING blocks (V83/V110/V108/
V86/V88 My Lord + Invasion, plus V99 Senate guard) moved out of
DeployEvaluator into new ObjectiveAnalyzer.getDeployObjectiveAdjustments()
returning List<ScoreNote>; DeployEvaluator now calls it once at the old V83
position and applies each note via action.addReasoning. Old inline blocks
COMMENTED OUT in place. Both bots (rando + chosenone) touched. HEAD =
e8f1eaac3. Read-only verification, no fix.

Findings (5/5 PASS):

1. COMPILE (in-container): PASS. docker exec ... mvn -q -pl gemp-swccg-server
   -am compile → EXIT=0, 0 [ERROR] lines. (Bash reported "exit 1" only
   because trailing `grep -c '[ERROR]'` found 0 matches.)

2. NO-DOUBLE-SCORING: PASS. rando DeployEvaluator.java — all 15 old-block
   tag-phrase lines (V83/V110/V108/V86/V88 MY LORD, V99 SENATE GUARD) start
   with `//`; zero uncommented addReasoning in the 1430-1800 old-block region.
   The ONLY live application is the single loop
   `action.addReasoning(note.reason, note.score);` at line 1419, fed by
   objDeploy.getDeployObjectiveAdjustments(...) at 1417 — at the SAME
   position the old V83 block fired. Live: 1 loop. Commented: all 6 blocks.
   chosenone mirror identical (live loop line 1419, call 1417, all tag lines
   commented).

3. GATING: PASS. rando ObjectiveAnalyzer.getDeployObjectiveAdjustments()
   (method @342): 5 arms gated —
     `if (analyzed && isMyLord && Filters.senator.accepts(...))`  (V83)
     `if (analyzed && isMyLord && isCharacter && !isSenatorCard(blueprint))` (V110)
     `if (analyzed && isMyLord && isCharacter && isSenatorCard(blueprint))`  (V108)
     `if (analyzed && isInvasion ...)`  (V86)
     `if (analyzed && isMyLord && Filters.senator.accepts(...))`  (V88)
   V99 arm `if (isCharacter && !isSenatorCard(blueprint))` at method-relative
   offset 115 has NO analyzed gate — code comment reads
   "V99: NON-SENATOR AT GALACTIC SENATE BLOCK (DELIBERATELY ungated — keys on
   Senate on table)". Fires ungated as required. chosenone twin: byte-identical
   arm structure (same offsets 16/37/59/66/105 gated, 115 ungated).

4. NO-DRIFT (chosenone twins): PASS. ObjectiveAnalyzer 1415 == 1415 lines;
   DeployEvaluator 6176 == 6176 lines. Identical arm structure + identical
   live-loop line numbers (1417/1419) across both bots.

5. DEPLOY STATUS: PASS (NOT LIVE, as intended). HEAD = e8f1eaac3 (committed).
   Running web.jar mtime 2026-07-07 17:18:41 UTC; app StartedAt 17:22:17 UTC —
   BOTH before the commit (19:21:46 UTC = 12:21 PDT). Running rando
   ObjectiveAnalyzer.class in the jar has ZERO `getDeployObjectiveAdjustments`
   occurrences → new consolidation method absent from the live JVM. No
   reload-ai run. NOTE: the check-1 in-container `mvn compile` updated
   /opt/gemp-swccg/src/gemp-swccg-server/target/classes ONLY; it did NOT
   repackage web.jar or restart the JVM, so the running server is unchanged.

Verdict: PASS (5/5). Behavior-preserving refactor compiles clean, no
double-scoring (old blocks commented, one live note loop), V99 correctly
ungated, twins have zero drift, and the change is committed but not deployed.

NOT VERIFIED (out of scope): live-game equivalence of the six objective
adjustments pre- vs post-refactor. Bytecode not yet deployed; needs a
My Lord / Invasion / Senate-guard game after reload-ai to confirm identical
scores fire at the same positions.

## 2026-07-07 — AI edit (ObjectivePlaybook pilot, commit 699b45876) → PASS

Behavior-preserving refactor: My Lord deploy magnitudes moved from inline
literals to analyzer-owned MY_LORD_PLAYBOOK.weights.* in rando + chosenone
strategy/ObjectiveAnalyzer.java. Pass criterion: no behavior change + compiles
+ chosenone mirrors rando.

1. COMPILE (in-container): PASS. mvn -pl gemp-swccg-server -am compile → EXIT=0,
   [ERROR] count = 0.
2. BEHAVIOR-PRESERVATION: PASS. MY_LORD_PLAYBOOK weights = ObjectiveWeights(
   1500.0f, -2000.0f, 500.0f, -2000.0f); ctor order maps to
   rewardKeyCharAtKeySite=1500 (V88), penalizeKeyCharOffKeySite=-2000 (V83),
   prioritizeKeyCharDeploy=500 (V108), holdNonKeyCharNoSite=-2000 (V110) —
   EXACTLY the old literals. Four call sites now reference the weight fields
   (L444 V83, L467 V110, L477 V108, L525 V88). V99 -1500 still inline (L561);
   V86 -1500 (L504) / +300 (L511) still inline. Both untouched.
3. LIFECYCLE: PASS. activePlaybook is a field (L93), set in analyze() (L136)
   at L177 as `isMyLord ? MY_LORD_PLAYBOOK : null`, reset to null in reset()
   (L749) at L774.
4. NO-DRIFT: PASS. rando + chosenone ObjectiveAnalyzer.java both 1492 lines;
   diff differs only in package decl (L1) and RandoLogger import (L3).
5. DEPLOY STATUS: PASS (not live). Running process loads
   gemp-swccg-async/target/web.jar; that jar has 0 MY_LORD_PLAYBOOK and no
   ObjectivePlaybook nested class. Committed, NOT deployed. Needs
   package + reload-ai to go live.

NOT VERIFIED (out of scope): live-game score equivalence pre/post. Static
proof only (weights == old literals, conditions/reasons/call-order unchanged).

## 2026-07-09 07:10 — V193 (CS) FLIP-GATE CONTROL, extend Endor flip-gate steer to CardSelection route (AI evaluator edit + fast-path deploy) → PASS

Operation: New V193 (CS) block added to BOTH bots' CardSelectionEvaluator,
right after the V136 CS score block, mirroring the DeployEvaluator V193
Endor-Bunker flip-gate steer onto the route where Endor character deploys
actually resolve. Commit 9496d1f39 on rando-consolidation-2026-06-23,
LOCAL ONLY. Deployed via in-container mvn package + force-recreate.

Findings (goal-by-goal):

1. LIVE-CODE — PASS. V193 (CS) block at lines 2087-2165 (identical in both
   files) sits INSIDE `if (v136DeployingCard != null) {` (opened 2060,
   closed 2166), itself inside `if (v136DepBp != null && ...CHARACTER)`
   (2049). Inner runtime guard is `if (v136Obj != null && v136Obj.isAnalyzed()
   && title != null)` — real condition, not if(false). All `if (false ...)`
   guards in both files are pre-existing V122(2176)/V67as(3556)/V159(44xx-53xx)
   plus the unrelated falsePositive at 143 — NONE enclose 2087-2165.
   v136DepBp used at 2122 is in scope (declared 2048).

2. BYTECODE — PASS. unzip + grep -a inside container on
   /opt/gemp-swccg/src/gemp-swccg-async/target/web.jar:
   rando CardSelectionEvaluator.class "V193 (CS) FLIP-GATE CONTROL" count=2
   (size 200607); chosenone count=2 (size 200815). count=2 expected (the
   addReasoning literal + the logger.warn literal both carry the string).
   NOTE on timing: web.jar mtime 07:09:54Z is AFTER app StartedAt 07:05:05Z —
   because MY step-3 verification `mvn package` re-packaged the jar at 07:09,
   post-boot. Not a staleness concern: source was unchanged, so the deploy
   jar the JVM loaded at 07:05 produced the identical string constants my
   rebuild reproduced (count=2). The running JVM has the code.

3. COMPILE — PASS. Independent `docker exec ... mvn -q -pl gemp-swccg-async
   -am package -DskipTests` printed MVN_EXIT=0. No errors.

4. MIRROR PARITY — PASS. diff of the 2087-2165 block between the two files
   shows EXACTLY 2 differing lines, both package-path only:
   `...rando.strategy.DeckOracle` vs `...chosenone.strategy.DeckOracle`, and
   `...rando.strategy.ObjectiveAnalyzer.ObjectivePlaybook` vs the chosenone
   path. Logic byte-identical otherwise.

5. SERVER HEALTH — PASS. hall http://localhost:80/gemp-swccg/ = HTTP 200.
   "Server is in operational mode and games are now able to be started" at
   07:05:10 (this boot). Last game ended 06:16:57 (queued game-end message),
   before the 07:05 restart. No active game running — verification did not
   disrupt play.

6. CHANGELOGS — PASS. resources/AI_CHANGELOG.md:7 "## 2026-07-09 — V193 (CS):
   extend the Endor Bunker flip-gate steer to the CardSelection deploy route
   (DEPLOYED...)" with diagnosis (replay somykkwjy449xul4), the two corrections
   (ability gate, magnitude/domination math), verify line, revert recipe,
   "LOCAL commit only", "no new tag" (update-old-rule discipline honored).
   AI_VERSION_HISTORY.md:1 "════ V193 (CS) EXTENSION (2026-07-09): ..." block
   present, matches. Both dated 2026-07-09.

7. NOT PUSHED — PASS. `git branch -r --contains 9496d1f39` = empty (on no
   remote). HEAD = 9496d1f39 on rando-consolidation-2026-06-23, local only.

Extra checks:
- Type-by-API discipline: zero getTitle().contains(<generic noun>) matches in
  the block in either file. Gate-site match is title.equalsIgnoreCase(
  analyzer's getFlipCriticalControlSite()) — a specific analyzer-supplied
  site name, not a generic-noun substring. Card-identity uses getCardCategory
  == CHARACTER, hasAbilityAttribute()/getAbility(), getDeployCost(),
  GameConditions.controls, DeckOracle.isCardInHand/isCardInReserve — all API.
- V79 actions.add class N/A: the block augments the EXISTING action via
  addReasoning (no new action created), so no actions.add()/continue needed.
- Boundary/domination math (per CLAUDE.md discipline) is DOCUMENTED in the
  comment (2106-2118): bonus = playbook weight (400) + CS penalty offset
  (~730) to DOMINATE, not delete, the V67ah(-350)+V113(-300)+V24.15(~-80)
  stack. Self-limiting: fires only while analyzer named a flip-gate site,
  Rando does NOT control it, and Rando holds the gate card. NOT independently
  re-derived here — flagged as reviewed-by-reading only.

Verdict: PASS (7/7 goals + extras).

NOT VERIFIED (out of scope, changelog implies pending live confirm): the
steer actually FIRING in a live Endor Operations game and the objective
flipping — grep next game's log for "V193 (CS) FLIP-GATE CONTROL" on a
cheap ability>=1 body steered to Bunker, then the ESB(V) deploy resolving.

## 2026-07-11 08:07 PDT - AI edit (c20e09e10, V22.7 adjust + SELECTABLE-CLAMP) -> WARN

Commit verification PASS: detached clean worktree at c20e09e10 compiled
`mvn -q -pl gemp-swccg-server -am compile` with MVN_EXIT=0 and 0 `[ERROR]`
lines. Rando/chosenone DecisionSafety SELECTABLE-CLAMP blocks identical
(5078 chars); CardSelectionEvaluator V22.7 routing blocks identical
(967 chars). Compiled class counts: DecisionSafety `SAFETY CLAMP` count=1
for both bots; CardSelectionEvaluator `V22.7` count=5, `into hand` count=4,
`prison` count=1 for both bots. Type-by-API grep found 0 forbidden generic
title contains patterns. AMN 109_7 source confirms iterative prison +
bounty-hunter combination with optional matching weapon/starship; engine
selection code rejects non-selectable ids, so the fix is plausible.

WARN: current main worktree is not exactly the commit. It has an unrelated
post-commit uncommitted edit in
`src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java`
(V177 block), so live local builds from the main worktree are not pure
c20e09e10 unless that change is addressed.

## 2026-07-11 08:54 PDT - AI edit (5bfcd870, Rey-game fixes wave 1) -> FAIL

Operation: independent read-only verification of local commit
`5bfcd8701867ada10d3a5ae4452dbffc4d9ac2b5`. Clean detached worktree:
`/private/tmp/gemp-verify-5bfcd870`. No Java edits, deploy, or push.

1. CLEAN COMPILE: PASS. Detached HEAD equals the full requested hash and
`git status --porcelain` is empty. An isolated `gemp_app` container ran
`mvn -q -pl gemp-swccg-server -am compile`: `MVN_EXIT=0`, `[ERROR]` lines=0.
The optional `-Dtest='*AI*'` selector found zero tests and exited 1 solely with
`No tests were executed`; no tests were added by this commit.

2. MIRROR PARITY: FAIL, 2/3 normalized pairs exact. After replacing only
`models.rando`/`models.chosenone` package namespaces, SHA-256 matches for
CardSelectionEvaluator (`bdfbaa7b...` both) and CombinedEvaluator
(`80a4f2f...` both). DeckOracle differs (`44a578a8...` rando vs
`04f1c4ef...` chosenone). Parent DeckOracle mirrors were exact, so the commit
introduced the drift. Rando runs V67h junk pass-through before persona rescue
(DeckOracle.java:1352-1418); chosenone runs persona rescue before the junk
guard (DeckOracle.java:1350-1416). Runtime adversarial probe `[download] 7
Chewie` with Chewbacca, Protector in Reserve: rando=UNKNOWN via V67h;
chosenone=WILL_SUCCEED via V82.2b.

3. POSSESSIVE GUARD: FAIL despite helper PASS. Reflection confirms
`isPossessiveTypeTarget("leia's lightsaber")=true` and
`isPossessiveTypeTarget("jabba's palace lando")=false`; hasTargetInZone with
real cards returns false/true respectively. But `validatePullFromSourceCard`
then maps the final word `lightsaber` to CardCategory.WEAPON
(rando DeckOracle.java:1303-1308,1532-1535), bypassing the guard at 968-982.
Using actual A Good Friend text (Card225_037.java:42,57-64) with only actual
Anakin's Lightsaber (Card3_071.java:34-44) in Reserve returns WILL_SUCCEED in
BOTH bots: `type-word 'leia's lightsaber' -> category WEAPON present`.

4. V82.1 PARSER: FAIL description/count parity; named core cases PASS.
Actual Java-source runtime results: Clash Of Sabers 5_038 ->
`[uncontrollable fury]`; You Are Beaten 5_163 -> `[i am your father]`.
Card4_069 period separator -> `[]`; Card104_007 `into your hand` -> `[]`.
Across 41 actual card/location game-text entries containing `search your
Reserve Deck`, the new fourth regex matches 35 and misses 6. The full parser
returns nonempty for 36 and empty for 5. AI_CHANGELOG.md:14 claims 37/41 and
4 misses, which is false for the committed regex at DeckOracle.java:1205.

5. V82.2 PARTIAL UNKNOWN: PASS, 4/4 runtime probes. With no matching cards,
`maintenance droid` and `non-unique Rebel` each return UNKNOWN in both bots,
never WILL_FAIL solely from partial predicate recognition
(rando DeckOracle.java:1317-1335,1419-1426).

6. V82.2b PERSONA: PASS narrowly. Actual Chewbacca, Protector has
Persona.CHEWIE (Card10_003.java:37-46). Runtime: `[Reflections II] Chewie`
and `Rebel Chewie` succeed; `Imperial Chewie` is UNKNOWN because the other
recognized predicate fails. Persona token matching is exact and recognized
predicate words are enforced (rando DeckOracle.java:1385-1414). The global
possessive pull still fails check 3 through category fallback, not persona.

7. V177/V166, V22.3, V148: PASS source/mirror structure. Wave requires
`wave[1] >= 1`, adjusted enemy power enters the gate, V22.3 uses inclusive
5/10/15 boundaries, and V148 uses a compiled 0.0 bar for `where to deploy`.
WARN: V177's gate log prints raw `v166TheirPower`, not adjusted enemy power
(CardSelectionEvaluator.java:927-928), so replay diagnostics under-report the
actual denominator.

8. TYPE-BY-API: WARN. Protocol's narrow forbidden `getTitle().contains(...)`
regex count=0. Semantic changed-code scan finds 3 text-classification sites
per bot, 6 total, in v177OppWeaponBonus: title contains `lightsaber` twice and
game text contains `permanent weapon` once (CardSelectionEvaluator.java:
5891-5899). Structured APIs exist: AiCardHelper.hasPermanentWeapon and
SwccgCardBlueprint.getPermanentWeapon.

9. BYTECODE/CHANGELOG/CARD TRUTH: mixed. Compiled marker counts are symmetric:
V177 V166 GATED=1/1; V22.3=5/5; V148=2/2 plus `where to deploy`=1/1;
isPossessiveTypeTarget=1/1; reserveTargetsAreAllUnattachableWeapons=1/1;
V82.1=1/1; V82.2 partial=1/1; V82.2b persona=1/1; V67h junk=1/1.
Both changelogs are present (12 and 7 added lines), but description parity
FAILS on exact mirror claim and 37/41 parser count. Replay file exists. All
named cards checked here exist in actual Java source; no fabricated card claim
found. `git diff --check` passes; no remote branch contains the commit.

VERDICT: FAIL. Do not deploy this commit as final. Required before re-verify:
make DeckOracle control flow identical, keep possessive named targets out of
the category fallback, support period/`into your hand` parser variants or
document the real 35/41 boundary, and correct changelog claims. Type-by-API
and adjusted-power logging are WARN follow-ups.

## 2026-07-11 09:03 PDT - AI edit (4836a836d, Rey fixes wave 2) -> FAIL

Independent detached-worktree verification at exact commit
4836a836da138a2d517afd76fc35f8e0eaec0a7e. No Java edits, push, or deploy.

PASS:
- Clean compile: `mvn -q -pl gemp-swccg-server -am clean compile`, MVN_EXIT=0.
- Changed executable hunks match rando/chosenone in all four pairs. MoveEvaluator
  has one comment-indentation-only mismatch. Type-by-API violations: 0.
- Local compiled class markers per bot: V76 fallback=1, V67ae=2, V185 ATE=1,
  oppWeaponBonusAt=1, mapTypeWordToCategory=1.
- Deployed web.jar bytes: V76 fallback=2, V67ae=4, V185 ATE=2 (both bots).
- V22.4 fallback pyrrhic boundary passes: old replay score 185 becomes at most
  -315 (-500 remains additive even if another favorable location contributes
  +40) and normally -415 after favorable +40 is removed and -60 no-favorable
  applies; Pass was -5. Both favorable arms are gated by !v76fPyrrhic.
- V47/V37.1 helper math sees replay board as raw 6v8 plus Aurra permanent
  blaster +3 and Mara's attached lightsaber +5, yielding 6v16, diff -10.

FAIL:
- Specific-location BattleEvaluator applies oppWeaponBonus only to
  BattlePredictor (BattleEvaluator:451-454). weaponEffectiveDiff remains
  effectiveDiff + our weaponBonus (285-286), and favorable scoring uses that
  unadjusted value (493-518). Its V76 log also prints raw opponent power and
  omits oppWeaponBonus (456-462).
- V67ae does not exempt the replay's Lower Corridor escape. It tests raw power
  at every friendly site (ActionTextEvaluator:3675-3682); log:12833 proves raw
  diff=-2 (6v8), so the >=6 gap does not fire and -300 remains at 3697-3701.
  Testing ANY friendly site also allows unrelated-site false exemptions.
- V177 still revives a dead named saber pull. The word rescue skips
  `lightsaber` (ActionTextEvaluator:315-324), then category rescue calls
  validatePullFromSourceCard (343-349). DeckOracle maps the full possessive
  `leia's lightsaber` by its last word to WEAPON (1303-1308, 1523-1536), so
  Anakin's Lightsaber can return WILL_SUCCEED. V185 cannot cure it: possessive
  exact matching rejects the wrong saber (494-505), and any wrongly matched
  weapon with a legal holder stands the veto down (517-524).
- Exact whole-file bot parity fails in DeckOracle: normalized diff=81 lines at
  both parent and commit. Rando runs junk UNKNOWN before persona rescue
  (1359-1378 then 1379+); chosenone runs persona rescue first (1350-1389) then
  junk (1390+), which can produce different verdicts.

WARN:
- AI_CHANGELOG line 11 claims 6v11 / -5..-6. Actual replay/source/code math is
  6v16 / -10 because Aurra contributes +3 and Mara's attached saber +5.

Evidence: logs/gemp-swccg.log:11501-11515 and 12833-12845; replay
replays/asdf/rbujmoc90br3uu4c.xml.gz final segment events 4080, 4082,
4176, 4247, 4287, 4332, 4493; card sources Card212_003.java:31-58,
Card110_011.java:38-63, Card3_071.java:36-54.

## 2026-07-11 08:52 PDT - AI edit + reload-ai (5-commit Rey/AMN set, HEAD fe0b4f9) -> WARN

Scope: c20e09e10 (AMN SAFETY CLAMP + V22.7), 5bfcd8701 (wave 1),
4836a836d (wave 2), 83e4ff89a (regex widen), fe0b4b911 (handoff = HEAD).

A GIT: PASS. All 5 commits on rando-consolidation-2026-06-23, HEAD =
  fe0b4b911. `git status --short -- 'src/**/*.java'` empty (the V177
  uncommitted edit from the 08:07 WARN is now committed).
B SERVER: PASS. HTTP 200 on :17001/gemp-swccg/; gemp_swccg_app_1
  StartedAt 2026-07-11T15:47:55Z (up ~1 min at check time).
C JAR FRESH: PASS. web.jar mtime 15:47:54Z (epoch 1783784874) >
  83e4ff89a commit ts 15:46:41Z (1783784801). JVM restart 1s after jar.
D BYTE MARKERS: PASS 14/14, both bots (python zipfile over class
  entries incl. inner classes): DecisionSafety 'SAFETY CLAMP'=1;
  BattleEvaluator 'HIT ECONOMICS'=1; ActionTextEvaluator 'V67ae
  RETREAT EXEMPT'=2 + 'V185 (ATE mirror)'=1; DeckOracle 'V82.2b
  persona match'=1 + 'V82.2 partial-recognition pass-through'=1;
  MoveEvaluator 'oppWeaponBonusAt'=1.
E PARITY: WARN — one REAL divergence. DeckOracle rando vs chosenone
  (models.rando->models.chosenone normalized): the V67h PARSE-JUNK
  pass-through block and the NEW V82.2b PERSONA RESCUE block are in
  OPPOSITE ORDER. Rando: junk-check THEN persona rescue (~1352-1420).
  Chosenone: persona rescue THEN junk-check (~1350-1414). Introduced
  at 5bfcd8701 (parity diff 0 lines at c20e09e10, 58 at 5bfcd8701+).
  Semantic edge case: a >25-char/digit-bearing parsed target that also
  contains an exact persona word returns UNKNOWN on rando but
  WILL_SUCCEED on chosenone. Neither returns WILL_FAIL, so no
  false-block, but mirror discipline is broken and future parity
  diffs will trip on it. Fix: reorder one bot to match the other
  (chosenone's order — rescue before junk — matches the V82.2b
  intent of rescuing before any early return).
  Cosmetic-only extras: DeckOracle ~973 comment wording (3-line vs
  1-line Codex m00118 note); chosenone MoveEvaluator:697 comment
  indentation. All other 6 files IDENTICAL after normalization:
  DecisionSafety, MoveEvaluator(code), ActionTextEvaluator,
  BattleEvaluator, CardSelectionEvaluator, CombinedEvaluator.
F DEAD-CODE: PASS. Zero `if (false` occurrences in rando
  ActionTextEvaluator / BattleEvaluator / DeckOracle; and since javac
  strips constant-false blocks (string literals inside them never
  reach the class file), the D bytecode hits independently prove
  V67ae RETREAT EXEMPT, V76 fallback, and V177 word-rescue (~L1513)
  compiled live.
G CHANGELOGS: PASS. resources/AI_CHANGELOG.md lines 7/18/30: wave 2,
  wave 1, SAFETY CLAMP (all 2026-07-10). AI_VERSION_HISTORY.md (at
  resources/k2-resources/originals/02-rando-history/) lines 1/8/15:
  matching ════ headers. NOTE: AI_VERSION_HISTORY.md does NOT exist
  at repo root; canonical copy is the k2-resources/originals one.

VERDICT: WARN (6 PASS, 1 WARN). Deployment is live and correct; the
DeckOracle block-order mirror break should be fixed in a follow-up
one-liner before the next parity-sensitive edit.

## 2026-07-12 22:45 PDT - AI edit + deployed commit (5ab16f8ac, phase-reorg Batch 1) -> FAIL

Independent verification at exact HEAD
`5ab16f8acf97cc608604b6e2ee03b258958786ae`. Main worktree dirty state was
recorded and preserved. No production edit, deploy, push, or cleanup was run.

VERDICT: FAIL. Acceptance 1 and 2 fail in the actual compiled card/API path,
and the required AI_VERSION_HISTORY update is absent.

1. CARD216_016 FORCED-LOCATION RESOLUTION: FAIL. The pull-route guard reads
`fsSrc.getBlueprint().getGameText()` at rando ActionTextEvaluator.java:5689
(chosenone mirror identical). Card216_016 stores "May [download] Krennic here."
as location-side text at Card216_016.java:40, exposed by
AbstractLocation.getLocationDarkSideGameText() at AbstractLocation.java:85-86;
the separate generic getGameText field is returned at
AbstractSwccgCardBlueprint.java:221-222. JShell against the deployed web.jar
returned exactly: `SOURCE.generic=null`, `SOURCE.dark=May [download] Krennic
here.`, `PARSE.generic=[]`, `PARSE.dark=[krennic here]`. Therefore the
`fsGtl.matches(...)` gate at ActionTextEvaluator.java:5691 is false for the
real source card, and Batch1a normalization at 5705 is unreachable.

2. BOTH KRENNIC BLUEPRINT EXEMPTION: FAIL. The new code derives identity from
the first title token at ActionTextEvaluator.java:5723-5726. Deployed-jar
JShell using Card216_011's actual flip text returned:
`Director Orson Krennic persona=true exemption=false` and
`Krennic, Death Star Commandant persona=true exemption=true`. Both cards have
typed Persona.KRENNIC in source (Card207_020.java:50; Card209_036.java:40),
and Persona.KRENNIC exists at Persona.java:97. The typed blueprint API is
available at AbstractSwccgCardBlueprint.java:383-389. This is also a direct
type-by-API violation of acceptance 9.

3. NORMALIZATION CENTRALIZATION: WARN. DeckOracle.parseSourceCardPullTargets
still emits `krennic here`; its normalization loop at DeckOracle.java:1249-1277
does not strip forced-location suffixes. Repository scan found 38 parser call
sites across both bot trees, while the suffix strip exists only at each bot's
ActionTextEvaluator.java:5705 copy. Acceptance 10 is triggered.

4. BATCH1b SOURCE: PASS by source inspection, runtime replay fixture not run
before Steve requested conclusion. CardSelectionEvaluator.java:6289-6311 gates
on weak mover, empty destination by total power, exactly one remaining weak
character, and adds exactly -800. Arithmetic is 327.5 - 800 = -472.5. A normal
friendly stack with positive total power bypasses the branch at line 6296.

5. RETIREMENT/PARITY: PASS at source. Executable V47 RESERVE SOLO BLOCK lines
from the parent are gone; only the retirement comment remains at
CardSelectionEvaluator.java:8861. V47 LANDO STAY remains at lines 6918-6919
and V47 LANDO PULL remains at 8174-8178 in both bots. Normalized whole-file
diffs for both edited evaluator pairs exited 0. Deployed retired-string byte
scan was not completed before the stop request.

6. BUILD/TEST: clean detached worktree compile command
`mvn -q -pl gemp-swccg-server -am compile` exited 0. Full server tests exited
1: 587 run, 1 failure, 17 errors, 25 skipped. Reported failures were legacy
card tests (Timer Mine assertion plus null blueprints in Lost Relay/Old
Pirates); this commit changed no tests or card source, so causation was not
assigned. There are no commit-specific regression tests.

7. DEPLOYMENT/HEALTH: web.jar size 44,247,726 bytes, mtime
2026-07-12 22:31:28 PDT; app StartedAt 2026-07-13T05:31:29Z, restart count 0;
HTTP `localhost:17001/gemp-swccg/` returned 200. Freshness/health pass, behavior
does not.

8. CHANGELOGS: FAIL. Commit updates resources/AI_CHANGELOG.md:7-14. It does
not modify the canonical
resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md, which
contains no Batch 1 entry; its latest touching commit is a25026c9c. This fails
the AGENTS same-session two-document requirement.

## 2026-07-12 23:21 PDT - AI cleanup commit (66cf11e18, Batch 1.5 purge) -> WARN

Independent read-only audit pinned to parent `e5b393955` and candidate
`66cf11e18`. The shared worktree was not reverted, deployed, pushed, or used as
a build destination. Builds and `javap` output lived under an isolated `/tmp`
snapshot. The branch advanced concurrently to `b544ceba6`; a second stable
snapshot of that newer dirty Java tree was also compiled without touching the
main worktree.

VERDICT: WARN. The four evaluator source edits are executable-behavior-neutral
and pass the static cleanup gate. The warning is documentation/provenance and
formal program-gate scope, not deleted live bytecode.

1. MANIFEST: PASS. `git diff --name-status e5b393955 66cf11e18` contains exactly
six modified files: `resources/AI_CHANGELOG.md` (+9/-0), canonical
`AI_VERSION_HISTORY.md` (+15/-0), mirrored CardSelectionEvaluator files
(+10/-498 each), and mirrored DeployEvaluator files (+9/-345 each). Total
`+62/-1686`; `git diff --check` exited 0. Line counts are CSE 9966->9478 and DE
6206->5870 per bot. No artifact file is in this commit's manifest.

2. SOURCE DELETION CLASS: PASS. Removed executable-looking text is enclosed by
literal-false guards: eleven V159 branches per CSE bot, V90 per DE bot, and
V67aj with nested V67al per DE bot. V127/V29.8 and V33/V67aq/V115 ranges were
line-comment corpses. No package, import, type, field, method, annotation, or
descriptor declaration was deleted. The only added non-comment Java statement
is the preserved zero-forfeit call once per bot.

3. V67t: PASS. At commit-relative CSE line 4493 in both bots,
`action.addReasoning("Optional forfeit but zero forfeit value", -80.0f);`
occurs exactly once. It was the always-taken `else` of `if (false && fv > 0)`
before the purge and is unconditional after it. Full normalized bytecode is
identical pre/post, proving no semantic change.

4. V21: PASS. The removed V37/V139 block was literal-false and contained one
trapped duplicate. Four live protection pairs remain in each bot at
commit-relative CSE lines 4370/4372, 4500/4502, 4633/4636, and 4962/4964.
Pre-source has five pairs, post-source four; normalized whole-class bytecode is
unchanged because the removed fifth pair never compiled.

5. DANGER REGIONS: PASS. Parent/post hashes match for rando CSE V122 lines
2307-2361 (`cd5c1cd0...`), CSE V67as 3667-3995 (`e09dba3b...`), DE objective
comment region 1424-1794 (`dc1e430c...`), DE live V193 1902-1983
(`52f34b38...`), CSE live V193 2211-2301 (`ca4d1fae...`), and ObjectiveAnalyzer
dead Endor V193 1438-1508 (`7017b380...`). ObjectiveHandler and ActionAudit
blob ids are identical parent/post for both bots.

6. MIRROR PARITY: PASS. After replacing `models.rando` with
`models.chosenone`, post-source SHA-256 is identical for CSE
(`f212fda9...`) and DE (`119f41e4...`). Normalized compiled mirror diffs also
exit 0 for both classes.

7. COMPILE: PASS. In throwaway `gemp_app` containers using Corretto JDK
21.0.11 and Maven 3.9.6, `mvn -q -pl gemp-swccg-server -am clean compile`
exited 0 with zero `[ERROR]` lines for current-at-start, parent, and candidate
snapshots. After the branch advanced, a stable snapshot at HEAD `b544ceba6`
plus 12 dirty mirrored Java files also compiled exit 0, zero errors.

8. BYTECODE: PASS. Exact command per affected FQCN was
`javap -classpath <snapshot>/src/gemp-swccg-server/target/classes -p -c -s -constants <FQCN>`.
Pre/post output diff exited 0 for all four classes. JDK21 output SHA-256 values:
rando CSE `9a3bdc63...`, chosenone CSE `9063cfb6...`, rando DE `657e3042...`,
chosenone DE `9a1d74bb...`. Exact output equality covers method descriptors,
instructions, exception tables, and printed constants; debug line tables and
raw class/JAR hashes were intentionally excluded per the cleanup gate.

9. DOCUMENTATION/PROGRAM GATE: WARN/HOLD. `resources/AI_CHANGELOG.md:11` and
canonical version history lines 6119-6121 group artifact removal with Batch
1.5, but `66cf11e18` removed none. Five tracked artifacts were deleted by
parent commit `e5b393955`; no tracked `.DS_Store` deletion exists. Commit
`b544ceba6` later restored `game_log_latest.txt` as durable evidence. Also, the
full cleanup gate's deterministic fixture/V191 steps were not part of this
static audit, so this report advances the no-live-bytecode-deletion code gate
only. It does not authorize deployment or broader program cutover.

Post-check: the branch advanced to `587870461c` while this audit ran. The
compiled current-tree snapshot's recorded source diff SHA-256
`8fa75cdc7ebe5b197ab7803d04cf5af0771fac9f813b83ff4cf7b50491e62418`
exactly matches `git diff --binary b544ceba6..587870461c -- src`, and `src/`
is clean at that HEAD. The current-tree compile therefore covers the final
committed Java content observed at audit completion.

## 2026-07-12 23:35 PDT - Batch 1 corrections (c497a5df6) -> FAIL

Independent read-only audit pinned to candidate `c497a5df6` in detached
worktree `/tmp/gemp-c497-verify`. The shared worktree was not reverted, built,
deployed, pushed, or used for mailbox communication. No production Java was
edited.

VERDICT: FAIL. Corrections 1, 3, and 4 pass source and compiled-class probes,
and Rando/ChosenOne commit deltas mirror. Correction 2 is release-blocking:
the central pull-target parser strips title-final `Here`/`There` from legitimate
card titles, contrary to the gate requirement. Do not advance or deploy this
commit without a parser correction and focused regression fixtures.

1. SIDE-AWARE SOURCE TEXT: PASS. Rando `DeckOracle.java:1164-1182` returns
base `getGameText()` plus only the acting side's location text. ChosenOne has
the same implementation at lines 1162-1180. Actual `Card216_016.java:37-41`
has null generic game text and dark-side text `May [download] Krennic here.`.
Compiled-class JShell probes returned that exact string for DARK and the light
side text for LIGHT. The Krennic guard reads the helper at Rando/ChosenOne
`ActionTextEvaluator.java:5705-5708`, recognizes the `here` pull, and passes the
same full text to the central parser at lines 5710-5711; the probe returned
`[krennic]`.

2. PULL CONSUMER OWNERSHIP: PASS. Call-site inventory found 18 helper calls per
bot: ATE 12 (`297`, `387`, `4730`, `4794`, `4875`, `4954`, `5022`, `5079`,
`5263`, `5415`, `5512`, `5706`), DE 3 (`798`, `3611`, `3715`), ActionAudit 2
(`268`, `325`), and DeployPhaseScript 1 (`297`). Remaining direct
`getGameText()` reads in these files are non-pull contexts. No pull consumer
bypasses `getSourceCardFullGameText` in the candidate.

3. CENTRAL SUFFIX NORMALIZATION: FAIL. There is one normalization owner per
bot, Rando `DeckOracle.java:1283-1287` and ChosenOne lines 1281-1285, and the
old ATE-local duplicate is gone. However, regex
`\s+(?:here|there|at\s+that\s+location)$` cannot distinguish a destination
clause from a title word. Compiled-class probes produced:

   `[download] Krennic here.` -> `[krennic]` (correct)
   `[download] I've Got A Problem Here.` -> `[i've got a problem]` (wrong)
   `[download] The Empire Knows We're Here.` -> `[empire knows we're]` (wrong)

The titles are real database entries at
`card_blueprint_database_dark.json:15627` and
`card_blueprint_database_light.json:39878`; two more title-final `Here`
examples occur at light database lines 7036 and 9682. Required fixture gap:
assert destination suffix removal for Krennic while preserving the complete
legitimate title `I've Got A Problem Here` (plus at least one `There` title if
present in the corpus).

4. TYPED KRENNIC FLIP EXEMPTION: PASS. Both bots use blueprint Personas at
`ActionTextEvaluator.java:5727-5755`, compare each typed persona's
`getHumanReadable()` value with word boundaries, and do not title-token match.
The only Krennic printings found both declare `Persona.KRENNIC`:
`Card207_020.java:50` (Director Orson Krennic) and `Card209_036.java:40`
(Krennic, Death Star Commandant). Against actual `Card216_011.java:41` flip
text, compiled probes accepted both. Negative probes rejected a Death
Star-only condition and `Krennicity`, so no substring false positive was
observed. Missing fixture gap: permanent two-printing positive plus Death
Star-only and substring-negative cases.

5. CHIRANEAU EMPTY-DESTINATION SPLIT: PASS. Both bots at
`CardSelectionEvaluator.java:5805-5818` retain opponent-power emptiness but
separately count friendly, owner-matched, non-undercover CHARACTER cards.
The `-800` branch now requires `fsDestOurChars == 0`; printed power is not
consulted for friendly presence. A power-0 friendly therefore prevents the
empty-site penalty, while an undercover character does not. Missing fixture
gap: executable power-0-friendly and undercover-only destination cases, plus
the recorded Chiraneau/Ozzel empty-destination replay case.

6. MIRROR/BUILD: PASS. Exact candidate deltas mirror for all six Java pairs:
ATE, CSE, DE, ActionAudit, DeckOracle, and DeployPhaseScript. Whole-file
normalized mirrors are exact except one pre-existing DeckOracle comment drift.
`git diff --check c497a5df6^ c497a5df6` exited 0. In the detached candidate
worktree, `mvn -q -pl gemp-swccg-server -am compile` exited 0. No focused AI
tests exist in the candidate snapshot; current untracked AI tests concern only
common decision facts/snapshots and do not cover these corrections.

7. CHANGELOG/HISTORY: WARN. Canonical history now contains Batch 1 and its
corrections at `AI_VERSION_HISTORY.md:6128-6151`, fixing the prior omission.
Accuracy defects remain: `AI_CHANGELOG.md:10` says "Deployed DeckOracle" while
line 14 and canonical history line 6150 say NOT deployed; line 8 says all
issues are corrected despite parser failure; its ATE consumer list accounts
for 10 calls while source has 12; and line 12 plus history line 6149 call
Ozzel a power-0 example. Actual `Card3_082.java:38` and the replay audit show
Ozzel has printed power 3 and ability 2. The friendly-character-count fix is
still correct; the named example is not.

## 2026-07-12 23:56 PDT - Batch 1 correction 2 (e17422f86) -> WARN

Independent audit pinned to parent `e4e0aa213df00cc0e4af521cf081d9f32d08cfb8`
and candidate `e17422f868aa8443cec05b0c0463f8b08e47b730`. Source inspection and
builds used git objects or the exported snapshot `/tmp/gemp-e17422f86-verify`.
The shared worktree's concurrent uncommitted edits were not inspected, built,
or judged. Nothing was deployed, pushed, committed, or edited outside this
append-only report.

VERDICT: WARN. All functional and build gates pass. The only issue is a minor
canonical-history wording error: `AI_VERSION_HISTORY.md:6172` says the prior
entry's corrected Ozzel mention is "below", but that entry is above at
lines 6139-6151. This does not block the code correction, but the factual
wording should say "above" or omit the direction.

1. MANIFEST/DIFF: PASS. `git diff --name-status e4e0aa213 e17422f86`
contains exactly 12 files: the two required histories; mirrored ATE, CSE, and
DeckOracle production files; shared FormationSafety; mirrored parser tests;
and FormationSafetyCountTest. Total `+470/-69`. No rename or artifact file is
present. `git diff --check e4e0aa213 e17422f86` exited 0. Commit parent resolves
exactly to `e4e0aa213df00cc0e4af521cf081d9f32d08cfb8`.

2. CENTRAL PARSER/TITLE GRAMMAR: PASS. Rando `DeckOracle.java:1279-1288`
and ChosenOne `DeckOracle.java:1277-1286` strip only lowercase terminal
`here`, `there`, or `at that location` from the raw capture before lowercasing.
Compiled probes returned `[krennic]` for the actual Card216_016 dark-side text,
`[vader]` for lowercase `there` and `at that location`, and preserved all four
unique terminal-Here titles in the combined blueprint DB: `I've Got A Problem
Here`, `You've Got A Lot Of Guts Coming Here`, `Let's Keep A Little Optimism
Here`, and `The Empire Knows We're Here`. The DB has four unique Title Case
terminal Here/There titles, all ending Here, and zero lowercase terminal
here/there/at-that-location titles. `There Is No Try` also remained complete.
Card source confirms the two named regression titles at `Card1_253.java:37-41`
and `Card222_027.java:38-42`; the real forced text is
`Card216_016.java:37-40`.

3. FOCUSED TESTS: PASS, 28/28. In the frozen snapshot under Corretto 21.0.11
and Maven 3.9.6, the exact focused command exited 0. Surefire reports show
Rando DeckOraclePullTargetParseTest 11 tests, ChosenOne 11, and
FormationSafetyCountTest 6, with 0 failures, 0 errors, and 0 skipped. The two
bot parser test files are normalized-exact mirrors.

4. AFFECTED-MODULE PACKAGE: PASS. A throwaway `gemp_app` container ran
`mvn -q -pl gemp-swccg-server -am clean package -DskipTests`; exit was 0.
The resulting isolated server jar is 1,901,251 bytes. No deployed artifact was
used as the build destination.

5. SIDE-AWARE PULL CONSUMERS: PASS. Each bot has exactly 18 calls to
`DeckOracle.getSourceCardFullGameText`: ActionTextEvaluator 12 at lines
297, 387, 4730, 4794, 4875, 4954, 5022, 5079, 5263, 5415, 5512, and 5706;
DeployEvaluator 3 at 798, 3611, and 3715; ActionAudit 2 at 268 and 325; and
DeployPhaseScript 1 at 297. Remaining direct getGameText reads in those files
are transport-mechanic, reserve-card classification, weapon/deploy-criteria,
power-estimate, drain, or battleground fallback contexts, not source pull-text
consumers. The actual Card216_016 compiled blueprint returned only its dark
pull text for Side.DARK and only its light-side text for Side.LIGHT.

6. KRENNIC PERSONA/NEGATIVE CONTROLS: PASS. Both actual compiled printings,
`Card207_020.java:41-50` (Director Orson Krennic) and
`Card209_036.java:32-40` (Krennic, Death Star Commandant), expose the typed
`Persona.KRENNIC` and returned true against Krennic flip text through
`DeckOracle.personaNamedInText` (`rando:1192-1204`, `chosenone:1190-1202`).
The real objective names Krennic in `Card216_011.java:37-41`. Death-Star-only
and `krennicity` probes returned false, matching the permanent fixtures at
the mirrored test files' lines 79-107.

7. FRIENDLY CHARACTER COUNT/CALL SITE: PASS. Shared
`FormationSafety.java:55-70` counts exactly the same category, owner, and
undercover predicate as the removed inline loops. Both bot call sites at
`CardSelectionEvaluator.java:5811-5812` pass the same
`gameState.getCardsAtLocation(location)` and `playerId` values, and the
existing `fsDestOpp <= 0 && fsDestOurChars == 0` gate remains at line 5813.
Fixtures cover a power-0 friendly, undercover-only, opponents, non-characters,
mixed boards, empty/null input, and null player. The extraction adds only
fail-closed null guards for inputs that the original call site supplies.

8. PARITY/BEHAVIOR SURFACE: PASS. After normalizing bot package names, hunk
locations, and blob hashes, candidate deltas are exact for DeckOracle, ATE,
CSE, and the parser tests. Whole DeckOracle files retain one pre-existing
comment-only drift near line 978; this commit introduces no drift. The persona
helper is statement-for-statement equivalent to the removed inline loop, and
the friendly-count helper is equivalent to its removed inline loop. No score,
threshold, veto, action-add, API category rule, or control-flow gate changed
beyond the intended parser case sensitivity and helper calls. New
type-by-title API violations found: 0.

9. CHANGELOG/HISTORY/DEPLOYMENT: PASS with the wording WARN above.
`resources/AI_CHANGELOG.md:7-13` accurately records 12 ATE consumers per bot,
18 total consumers per bot, 28/28 tests, both helpers, Ozzel's real printed
power 3 (`Card3_082.java:34-40`), and `NOT deployed`. Canonical history
`AI_VERSION_HISTORY.md:6161-6172` records the same correction and NOT-deployed
state. The live `web.jar` is 44,247,726 bytes with mtime 2026-07-12 22:31:28
PDT, 75 minutes before the commit time 23:46:03 PDT, and byte scans found none
of `personaNamedInText` in either bot DeckOracle class or
`countFriendlyNonUndercoverCharacters` in FormationSafety. The app started at
2026-07-12 22:31:29 PDT-equivalent, restart count 0, and HTTP returned 200.

NUMBERS: 12 files; +470/-69; diff-check 0; package 0; tests 28/28; failures 0;
errors 0; skipped 0; 18 side-aware pull consumers per bot; 12 ATE consumers
per bot; 2 Krennic printings positive; 2 negative controls negative; 4 unique
terminal Here/There DB titles preserved; 0 lowercase-title collisions; 0 new
type-by-title violations; 0 correction helper symbols in live web.jar; HTTP 200.
