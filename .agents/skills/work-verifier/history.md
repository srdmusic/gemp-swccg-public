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

## 2026-08-25 16:49 PT - AI evaluator edit (V301 strict battle-loss order) -> FAIL

Operation: independent source verification of the uncommitted V301 diff in
`/Users/steve/gemp-objective-score-cap-2026-08-15`, branch
`codex/rando-space-ability-four-2026-08-22`, exact base
`095832d2b1b39fafb9ddc43f789249ef520cd79a`. No package, deploy, runtime,
database, deck, card-source, engine-source, commit, or push action was run.

FAIL blocker - DAMAGE tier retains stale positive attrition for scoring:

- `BattleForfeitPolicy.planCombinedResolution` correctly transitions to
  `ResolutionTier.DAMAGE` when no selectable HIT or attrition-capable
  participant exists (`BattleForfeitPolicy.java:130-159`).
- Both mirrored adapters nevertheless build `DecisionFacts` with the raw
  `attritionRemaining` (`rando/.../CardSelectionEvaluator.java:7090-7092` and
  `chosenone/.../CardSelectionEvaluator.java:7090-7092`) and pass that object
  unchanged to `evaluateCombined` at lines 7137-7139.
- The stale value activates V150 for Force loss
  (`BattleForfeitPolicy.java:441-446`) and the attrition/immunity arms of V159
  (`BattleForfeitPolicy.java:491-558`) even though the active category is now
  remaining battle damage.
- Independent compiled-policy probe: with effective DAMAGE facts of attrition
  5 and damage 10, a Force-loss candidate receives `V150 -500`; the same facts
  with effective attrition zero receive `V22.3 -80`. A nonimmune
  `cannotSatisfyAttrition` vehicle with forfeit 1 receives
  `V159-attrition +1800` under the stale facts, versus
  `V159-pure-low-coverage -800` with effective attrition zero. This is a
  2600-point ranking inversion inside tier 3, not a cosmetic reason string.
- Actionable fix: after the plan is known, construct the scoring
  `DecisionFacts` with effective attrition zero when
  `resolutionPlan.tier() == DAMAGE` (or make the plan own that effective
  value), while retaining raw/frozen attrition only for tier selection and
  diagnostics. Add assertions that all-immune and all-cannot-satisfy DAMAGE
  sets emit no V150 and use V159 pure-damage arms.

Test-quality gap that allowed the blocker:

- `allImmuneParticipantsMoveToRemainingDamageTier`
  (`ForceLossCardSelectionPolicyParityTest.java:537-562`) asserts only that
  both actions are present. It does not assert their scores, reasons, or the
  absence of V150.
- `residualAttritionDisappearsWhenNoBattleParticipantRemains` at lines
  655-678 covers only the different case where `GuiUtils` already reports
  zero because no participant remains. It does not cover DAMAGE selection
  while immune or cannot-satisfy participants remain on table.

Checks that passed on this snapshot:

- Fresh server-reactor compile: PASS, Maven exit 0.
- Independent bounded ring: PASS, 13 classes, 97 tests, 0 failures,
  0 errors, 0 skipped.
- Compiled V301 adapter marker: rando 1, chosenone 1.
  Class sizes were 380867 and 381299 bytes, respectively.
- Exact adapter mirror after package normalization: PASS, zero diff lines.
- `git diff --check`: PASS, zero findings.
- Type-by-API forbidden title-substring scan: PASS, zero findings. New typing
  uses `CardCategory`, zone APIs, frozen immunity APIs, and
  `cannotSatisfyAttrition`.
- Scope before this required history append: PASS, 10 modified files,
  624 insertions and 133 deletions. Breakdown: 4 production AI files,
  3 tests, and 3 records. No card Java, engine Java, deck, or DB file changed.

Claim-by-claim semantic audit:

- Selectable HIT character/vehicle/starship tier: PASS. Only selectable facts
  enter the plan at both adapters' lines 7065-7085; hit participants are
  selected at `BattleForfeitPolicy.java:136-152`.
- Attrition tier: PASS for exposure. It requires positive remaining attrition,
  participant category, `canSatisfyAttrition`, and nonimmunity to frozen total
  attrition (`BattleForfeitFacts.java:112-130`,
  `BattleForfeitPolicy.java:144-157`). Mixed-immunity and nonselectable tests
  passed.
- Lower-tier omission and fallback isolation: PASS. The adapter emits only
  `resolutionPlan.includes(cardId)` at lines 7114-7117. CombinedEvaluator can
  merge and mandatory-fallback only emitted action IDs, so omitted lower tiers
  cannot reappear through additive sums or the all-veto fallback. The engine's
  combined prompt has minimum selection 1.
- V48 and flip-gate same-tier alternatives: PASS. Both consume the active ID
  set only. Sole loaded HIT and sole loaded attrition carriers remain legal;
  peer alternatives still protect the carrier.
- Off-table/Reserve weapon routing: PASS. Combined reads short-circuit
  Force-loss-zone candidates before battle-weapon facts
  (`BattleForfeitFacts.java:331-335`), and V154 requires
  `battleWeapon()` (`BattleForfeitPolicy.java:401-411`). Focused tests confirm
  no V154 on a Reserve weapon.
- All-immune/cannot-satisfy DAMAGE scoring: FAIL for the stale-attrition reason
  above. Exposure is correct, within-tier scoring is not.
- Replay arithmetic: PASS against engine source. A table forfeit independently
  satisfies `min(remaining, forfeit)` for battle damage and attrition
  (`ForfeitCardsFromTableSimultaneouslyEffect.java:133-175`), so forfeit 8
  against attrition 11 and damage 22 leaves attrition 3 and damage 14. Once no
  forfeitable participant remains, `GuiUtils.java:125-130` reports effective
  attrition zero.

Records truthfulness: WARN under the blocker. `resources/AI_CHANGELOG.md:2700`
labels V301 "source-tested" while line 2705 says proof remains below
`SOURCE_TESTED`. Lines 2703-2705 and
`AI_VERSION_HISTORY.md:11608-11639` also overstate completed tier-3 semantics
until effective attrition is corrected and independently retested. The mailbox
correctly says review pending, but its design claim is incomplete for scoring.

Verdict: FAIL. The categorical action exposure works, but tier 3 still scores
as attrition in all-immune/cannot-satisfy transitions. Fix both mirrored
adapters through their shared effective facts, add the missing score/reason
regressions, correct the records, and re-invoke the verifier.

## 2026-08-25 16:54 PT - AI evaluator edit re-verification (V301 corrected) -> PASS

Scope verified:

- Base `095832d2b1b39fafb9ddc43f789249ef520cd79a`, branch
  `codex/rando-space-ability-four-2026-08-22`.
- Production scope excluding this required verifier-history append is exactly
  four files: shared `BattleForfeitFacts.java`, shared
  `BattleForfeitPolicy.java`, and the Rando and Chosen One
  `CardSelectionEvaluator.java` adapters. The remaining six files are three
  focused tests and three project records. Total: 10 files, 676 insertions,
  135 deletions.
- No card Java, engine Java, deck data, database, package, jar, deployment,
  runtime, game mutation, commit, or push action occurred.

Prior FAIL correction:

- PASS. Both adapters now derive `scoringAttritionRemaining` from the selected
  resolution tier and set it to zero for `DAMAGE`
  (`rando/.../CardSelectionEvaluator.java:7090-7100` and the exact normalized
  Chosen One mirror). The corrected `DecisionFacts` is passed unchanged to
  `evaluateCombined` at lines 7141-7143. Raw remaining attrition is retained
  only for tier selection and formation facts.
- PASS. The all-immune regression asserts V159 receives `attr=0`, Force loss
  receives no V150, and its reason reports 10 damage left
  (`ForceLossCardSelectionPolicyParityTest.java:537-566`). The parallel
  all-`cannotSatisfyAttrition` regression makes the same pure-damage assertions
  at lines 569-597. The nonselectable HIT regression also pins no V150 and 10
  damage left at lines 630-659.
- PASS. V150 can now activate only when the effective scoring facts retain
  positive attrition (`BattleForfeitPolicy.java:441-446`), while V159 reaches
  its pure-damage arms after the categorical transition
  (`BattleForfeitPolicy.java:477-565`). The 2600-point stale-attrition ranking
  inversion identified by the first audit is closed.

Independent checks:

- Fresh server-reactor compile: PASS, Maven exit 0.
- Independent bounded ring: PASS, 13 classes, 98 tests, 0 failures, 0 errors,
  0 skipped. The focused `ForceLossCardSelectionPolicyParityTest`,
  `BattleForfeitPolicyTest`, and `BattleForfeitFactsTest` subset accounts for
  54 tests, also 0 failures, 0 errors, 0 skipped. The ring additionally covered
  adapter parity, source ownership, capture retention, battle residuals,
  Force-loss policy/action-text/facts, Tdigwatt behavior, and the battle-engine
  contract.
- Compiled V301 marker: PASS, exactly 1 occurrence in each adapter class.
  Fresh class sizes are 381060 bytes for Rando and 381492 bytes for Chosen One.
- Exact Rando/Chosen One mirror after package normalization: PASS, zero diff
  lines.
- `git diff --check`: PASS, zero findings.
- Type-by-API discipline: PASS, zero forbidden title-substring matches. New
  logic uses card categories, engine zones, frozen immunity APIs, selectability,
  and `cannotSatisfyAttrition`.

Claim-by-claim semantic audit:

- Tier 1: PASS. Only selectable HIT character, vehicle, and starship facts can
  establish the tier. Nonselectable and unavailable candidates never enter the
  plan (`CardSelectionEvaluator.java:7063-7085`,
  `BattleForfeitPolicy.java:136-152`).
- Tier 2: PASS. Only selectable character, vehicle, and starship participants
  that can satisfy attrition and are nonimmune to frozen total attrition can
  establish the tier (`BattleForfeitPolicy.java:144-157` and the shared facts
  reader). Mixed immunity, all immunity, all cannot-satisfy, and total-versus-
  remaining immunity regressions pass.
- Tier 3: PASS. When no earlier-tier action exists, every remaining selectable
  legal damage action is exposed, with effective scoring attrition zero. The
  all-immune, all-cannot-satisfy, and nonselectable boundaries are pinned by
  score and reason, not merely action presence.
- Lower-tier isolation: PASS. The adapters emit only IDs included by the active
  `ResolutionPlan` (`CardSelectionEvaluator.java:7118-7120`). Additive scoring
  and mandatory all-veto fallback operate only on that emitted map, so they
  cannot restore a lower-tier choice. The engine combined prompt requires at
  least one selection.
- Same-tier protections: PASS. V48 loaded-carrier and flip-gate formation
  alternatives consume the active-tier ID set only. Sole loaded HIT and sole
  loaded attrition carriers remain legal; peer alternatives in the same tier
  still protect them.
- Weapon routing: PASS. Hand, Reserve Deck, and Force Pile weapons short-circuit
  through Force-loss facts, while V154 requires a battlefield weapon
  (`BattleForfeitFacts.java:331-335`,
  `BattleForfeitPolicy.java:401-411`). The Reserve-weapon regression passes.
- Replay arithmetic: PASS against engine source. A table forfeit independently
  satisfies battle damage and attrition, so forfeit 8 against attrition 11 and
  damage 22 leaves attrition 3 and damage 14. When no forfeitable participant
  remains, the engine reports effective attrition zero.
- Rando/Chosen parity: PASS in source normalization, compiled markers, and all
  adapter parity assertions. No alternate selection or fallback path bypassing
  the active tier was found.

Records and proof:

- PASS. `resources/AI_CHANGELOG.md:2700-2705` and
  `AI_VERSION_HISTORY.md:11631-11652` accurately describe the corrected
  DAMAGE-tier boundary, the first verifier failure, final 54/98 counts, exact
  scope, and the pending independent gate. The mailbox entry at
  `Handoffs/AI_MAILBOX.md:555-565` remains a truthful historical pending
  snapshot and can be closed by a later append without rewriting history.
- Test quality is sufficient for this source claim. The corrected tests assert
  action exposure, exact scoring-path reasons, absence of stale V150, both-bot
  parity, frozen-total immunity, selectability, same-tier alternatives, Reserve
  routing, and the engine arithmetic contract.

Verdict: PASS, 98 tests with 0 failures, 0 errors, and 0 skipped. No blockers or
warnings remain for the V301 source gate. This supports `SOURCE_TESTED`; it is
not package, runtime-loaded, or replay-fired proof.

## 2026-08-29 15:22 PDT: V302 plus Set 27 integration candidate -> FAIL

Operation: independent source and package verification of exact commit
`b1f74cf61ff132b6c02e54e363f1c4cdd3ac5ec3` in clean worktree
`/Users/steve/gemp-v302-set27-integration-2026-08-29`. The candidate combines
V302 commit `8be676df32daacf4fefe3be29b6555cb7567614c` with the three Set 27
commits `088416ab1c62fd26f0070499a9810ffc6cc8e999`,
`23b72e9ad78eb31e9eadc62eb039011c4b0e878f`, and
`b1f74cf61ff132b6c02e54e363f1c4cdd3ac5ec3`. No live-server, database,
game-state, source-edit, deploy, restart, or push action was performed.

FAIL blocker: the sealed candidate is not a V302-only byte delta from the
stated current-live artifact
`cf36a592d6ef280ed1b9a1e386864949e96f52e156eb5e95b4565dd8eb239a23`.
Candidate versus live has 525 content deltas. Only 113
are outputs owned by the eight V302 production sources. The other 412 comprise
89 additions, 323 changes, and zero removals.

- The 412 non-V302 deltas contain 391 AI classes: 86 additions and 305 changes.
  Family counts are 307 shared/common, 42 Rando, and 42 Chosen One class
  entries.
- The remaining 21 are non-AI entries: three additions and 18 changes. They
  cover Hall/game classes (13), logic classes (3), async classes (2), one card
  evaluator class, the packaged async POM, and `objective_playbooks.json`.
- All 412 candidate bytes match the sealed V301 jar exactly at the same paths.
  Candidate removes no current-live entry. It would nevertheless overwrite
  305 current-live AI class variants that differ from sealed V301 and re-add
  86 sealed V301 AI classes absent from the current-live jar.
- The current-live artifact itself differs from sealed V301 in 1,029 entries,
  529 outside the Set 27 output allowlist, and lacks both Rando and Chosen One
  V301 adapter markers. This explains the candidate delta but does not make the
  broader replacement implicit in a V302 deployment request.

Required resolution: explicitly accept restoration of the sealed V301-derived
source baseline as part of this deployment, or produce a candidate that
preserves the intended current-live AI bytes. Re-run the deployment verifier
after that decision. The verifier does not treat unreviewed live drift as
implicitly disposable.

Checks that passed:

- Exact source identity and ancestry: PASS. HEAD and tree are exact, the
  worktree was clean before this required history append, V302 is based on
  `b9d607b973a264f46ddc5e6801f2f47fe61d8789`, and the three Set 27 commits are
  direct descendants. No V302 production, test, audit, or history file changed
  after `8be676df`.
- V302 semantics: PASS. Actual card source confirms Tracked Fleet is
  annihilated at the start of its owner's turn when that player controls its
  physical host. The implementation ranks the current host before the shortest
  admissible bridge, excludes prior hard vetoes and deferrals from the safe
  tier, limits the V41 exemption to the exact host, retains V41-rejected bridge
  deferral, holds a survivable unresolved host, releases retreatable danger,
  funds exact pre-flip Kylo Outpost setup only behind an independently piloted
  on-table chase ship and remaining move reserve, and hard-vetoes sole pending
  trigger control-source movement and forfeit.
- Both-bot parity: PASS. Fully normalized Rando and Chosen One
  `CardSelectionEvaluator` files have zero diff lines; normalized
  `MoveEvaluator` files also have zero diff lines. Shared policies own the
  bounded contributions and source-ownership tests pass.
- Independent focused tests: PASS, 165 tests, zero failures, zero errors, zero
  skipped. The nine suites report 32, 12, 11, 6, 41, 43, 16, 2, and 2 tests.
  The verifier's container Maven run exited zero.
- Clean full-build evidence: PASS. There are 436 server reports totaling 3,535
  tests, zero failures, zero errors, and 26 skipped, plus one async report with
  10 tests and no failures, errors, or skips. Compilation and packaging had
  completed in the same clean worktree.
- Sealed artifact integrity: PASS. Path
  `/Users/steve/gemp-deploy-artifacts/first-order-route-set27-2026-08-29/`
  `b1f74cf6-947ee96c/web.jar`, SHA-256
  `947ee96c3f3e3b6b5813dbb0ab784ec17ebe1052d36d97c49276a7e1f89cbbdb`,
  46,208,218 bytes, 27,053 entries, zero duplicate names, and a clean full ZIP
  test. The sealed bytes exactly match the worktree target jar.
- Package source-byte ownership: PASS. The 22 changed production Java sources
  produce 696 top-level and nested class files. All 696 match their module jars
  and the sealed web jar byte-for-byte, with zero missing outputs or mismatches.
- Marker presence: PASS in both bot adapters and shared policies for current
  host, shortest safe bridge, V41 route hold and exact-host exemption, Kylo
  Outpost setup, unresolved-host hold, and pending-trigger hold.
- Strict known-baseline deltas: PASS. Candidate versus isolated V302 artifact
  `ed7d0312c19979b57ed7d99a6d187ad789b9f19ac0a949def1f233209ab23e7f`
  has exactly 500 deltas, all inside the 572-entry Set 27 class/resource
  allowlist. Candidate versus sealed V301 has exactly 610 deltas, all inside
  the 697-entry V302-plus-Set-27 allowlist. All 572 Set 27 output entries match
  current live byte-for-byte.
- Hygiene: PASS. `git diff --check`, objective JSON parsing, type-by-API scan,
  card-source truth, changelog description parity, and normalized adapter
  parity all passed.

Verdict: FAIL for deployment, with one exact scope blocker. Source, tests,
package integrity, V302 semantics, Set 27 preservation, and known-baseline
union are verified. The unresolved issue is the additional restoration of 412
non-V302 live entries, including replacement of 305 current-live AI byte
variants. This supports the candidate as an exact clean source build, but not
as an authorized narrow live replacement.

## 2026-08-29 20:29 PDT: V301 plus V302 plus Set 27 live runtime -> PASS

Operation: independent read-only post-deployment verification of expected live
artifact
`947ee96c3f3e3b6b5813dbb0ab784ec17ebe1052d36d97c49276a7e1f89cbbdb`.
No server setting, database row, game state, container, source, artifact, or
repository state was changed. Authentication created only a temporary local
session for the Hall read.

- Artifact identity: PASS. The sealed candidate, live host jar, and running
  container jar have the exact expected SHA-256 and are each 46,208,218 bytes.
  The jar mtime is `2026-08-29T22:15:18Z`; the fresh app started later at
  `2026-08-30T03:25:19.011669461Z`.
- App-only recreation: PASS. Fresh app container is
  `d4d4fb2ebfb729392cc67539cc6d1db65f523888d5d570ea0767ab81c206e281`.
  Database container
  `46a8397072d34ed7927676aa8eaf870e6ead6d3ee9332fa1ca602526de84faa6`
  retained its identity and `2026-08-29T20:44:46.847479292Z` start time.
- Runtime process: PASS. Java is direct PID 1 with the expected `-jar` path.
  At the final `2026-08-30T03:29:25Z` check the app and database were running,
  each had restart count zero, and neither was OOM-killed.
- HTTP: PASS. Three consecutive probes to `/gemp-swccg-server/` returned 200
  with 300-byte bodies.
- Gameplay settings: PASS. Direct read-only database query found exactly the
  five expected settings enabled: AI tables, bonus abilities, in-game
  statistics, new-account registration, and private games.
- Hall safety: PASS. Local `asdf` authentication and authenticated Hall GET
  both returned 200. The Hall XML had root `hall`, zero tables, zero games,
  zero player attribute values, and zero error elements.
- Live markers: PASS. Both Rando and Chosen One contain the V301 marker and all
  V302 current-host, shortest-bridge, V41, Kylo Outpost, unresolved-host, and
  pending-trigger markers. All five new Set 27 top-level card classes and
  `set227-rarity.txt` are present with nonzero bytes in the running jar.
- Logs: PASS. The 365 post-start log lines contain one startup-complete marker,
  one operational-mode marker, Set 227 loading, zero ERROR/FATAL lines, zero
  Java stack frames, and zero `Caused by:` lines.

Verdict: PASS. The exact combined V301 plus V302 plus Set 27 artifact is loaded
and the runtime is healthy with an empty authenticated Hall. Proof ceiling is
`RUNTIME_LOADED`, not `REPLAY_FIRED`; no gameplay branch firing was attempted.

## 2026-09-11: V312 evaluator edits, PASS at SOURCE_TESTED

Independent verifier reviewed the V312 diff against `5d5276eea` in the maintained
`gemp-v304-map-buddy-drain-2026-09-06` worktree. Eight production classes changed:
both bot owners, both deployment planners, both card-selection evaluators, and
the shared ForceLossPolicy and DeployObjectiveSitingPolicy.

- Compile and focused tests: PASS. Offline native Maven, JDK 25.0.2 targeting
  Java 21. Ten fresh Surefire reports total 123 tests, zero failures, errors,
  or skips. Independent review re-read those reports.
- Mirrors: PASS. All three changed bot-pair diffs match exactly.
- Compiled output: PASS. All eight changed production classes have the expected
  marker or method; class major version 65. Zero new type-by-title heuristics.
- Behavior boundaries: actual Light/Dark Lando printings, duplicate and ordinary
  turn-4 hand characters, both loss routes, mandatory negative-score selection,
  legal funded Hoth packet, zero-icon spy exception, exact missing site target,
  and committed buddy/count packet protections are covered.
- Documentation and diff: PASS. Both changelogs match source; diff check clean.
- Limit: the deployment regression mocks the engine legality boundary. No new
  speculative deployment sequencing, package, restart, or replay firing was
  performed. Docker was already stopped and was not started. The installed V311
  jar remains SHA-256 `4c8d526f5f3c9c0054c8c45d70236384f92f452d6235d9893a75e793a9b57253`.

## 2026-09-13: V313 evaluator edits, PASS at SOURCE_TESTED

Independent bounded review against `c52a2d53a` in the maintained
`gemp-v304-map-buddy-drain-2026-09-06` worktree covered the reserve-weapon
precheck, permanent-weapon host protection, and solo hit-removal deferral.

- Compile and focused tests: PASS. Root's final offline native Maven reactor
  test command exited zero. The verifier independently parsed all 13 fresh
  Surefire reports: 95 tests, zero failures, errors, or skips. No concurrent
  Maven execution was started by the verifier.
- Compiled output: PASS. All 15 changed production classes, including the
  nested KillShotFacts record, contain the expected V313 strings or callable
  hooks. All inspected outputs have Java 21 class major version 65.
- Mirrors: PASS. Changed lines match exactly across all five bot pairs:
  owners, ActionTextEvaluator, CardSelectionEvaluator, DeployEvaluator, and
  PullPolicyAdapter. Both adapters publish the reserve-parent deferral before
  ordinary text bonuses. The focused tests prove that an added one-million
  score cannot revive a deferred reserve parent when Pass is legal.
- Boundaries: actual action-ID reflection, unarmed legal targets, unrelated
  unarmed characters, mixed hallway/weapon routes, attached and permanent
  weapons, and non-mutating effect inspection are covered. Review identified
  and resolved a direct-hand permanent-weapon counting gap. Multi-result,
  named-system, alternate-target, react, and unsupported exact-card/free
  previews remain unknown instead of vetoing on speculative future state.
- Sniper source checks: PASS. All four removal modes match real card Java.
  The shared guard uses hit state, actual battle participants, exact target
  full name, and pre-damage timing. Additional participating characters,
  starships, or vehicles preserve existing kill-shot scoring. Both bot chains
  select Pass for the last hit enemy character.
- Hygiene and documentation: PASS. No new forbidden type-by-title tests;
  `git diff --check` is clean. Both V313 changelog descriptions match the
  bounded source implementation. Earlier unrelated deployment-planner
  behavior is explicitly not claimed fixed by V313.

Verdict: PASS for SOURCE_TESTED only. Engine deployment legality is mocked in
the focused reserve-route tests; this is not replay-fired proof or a guarantee
for unmaterialized custom card effects. No package, deployment, restart, live
game mutation, database change, or push was performed by this verification.

## 2026-10-14: V314 exact objective progress and Shield route, PASS at SOURCE_TESTED

Independent verification reviewed the complete V314 diff from
`14bbf321cd77c6afa227dca1cc87b18802d5e5f8` through documentation HEAD
`3a070243950750b57c479cf518e8513d7d6a28ed` on branch
`codex/walker-objective-progress-2026-10-14`. Production commits are
`a92047204` and boundary correction `e9fd061f6`. The worktree was clean before
this required history append, ancestry is exact, and `git diff --check` passes.
The bounded diff contains 19 production files, 13 tests, and three histories or
mailbox documents.

- Review correction: the first source pass found that the original exact deploy
  predicate also admitted Old Allies' planned Falcon pilot, an ordinary route
  enabler. A failure-first regression reproduced the leak. The correction adds
  a shared exact-location predicate that admits missing counted locations and
  the selected Shield-generator package, while excluding the Falcon pilot and
  First Order's ordinary actor route. Fresh Old Allies coverage proves the
  Falcon pilot is not exact and a missing counted Jakku ground body is exact on
  both bots. The four-suite correction boundary passed `105/0/0/0`.
- Compile and focused tests: PASS. A clean offline server-reactor compile exited
  zero. The exact 14-suite focused selector produced `238/0/0/0`. Coverage
  includes deploy choice and plan ranking, objective strength and merge order,
  policy transport, the Shield twin and Massassi engine contracts, the Hoth
  replay boundary, move destination and drain routing, source parity, and the
  one changed deploy-planner parity method.
- Impacted tests: BASELINE-ONLY RED. The documented selector
  `*Objective*Test,*CombinedEvaluatorDecisionTest,*SourceParityTest,*AdapterParityTest`
  produced `1324/14/14/0`. All 28 red results exactly match the September 15
  baseline classification: six pre-existing deploy-planner fixture results and
  22 upstream-sync fixture results. There is no new V314 red class or method.
- Score semantics: PASS. Ordinary objective relevance remains exactly `+300`.
  Exact progress is exactly `+1000`. The strongest positive tier wins, so
  ordinary plus exact remains `+1000` in either order and across merges.
  Repeated exact signals do not stack. Negative influence is accumulated on its
  independent channel and bounded at `-300`. Hard veto and deferred status
  remain categorical.
- Route boundaries: PASS. Only the source-proven Shield main-generator parent
  and destination receive the narrow V41 and V67g exemptions. Positive drain
  scoring still precedes the zero-drain exemption. FormationSafety, legality,
  package identity, piloting, cannon retention, terminal defense, and
  backward-route holds remain dominant or categorical. No non-Shield objective
  movement receives exact strength.
- Mirrors and compiled output: PASS. Fully normalized Rando and Chosen One
  versions of `BattleEvaluator`, `CardSelectionEvaluator`, `EvaluatedAction`,
  `MoveEvaluator`, and `PolicyOperationAdapter` have zero body or import
  differences. All changed common and mirrored production classes compile to
  Java 21 class major version 65. Compiled classes contain the new exact tier,
  exact-location method, and both-bot V41/V67g route markers.
- Static safety: PASS. The new exact-location method and score transport have
  live production and test callers. No stale score-field ownership remains.
  Scans found zero new title-substring type checks, debug printing, TODO/FIXME
  markers, constant-false branches, or dead exact-progress hooks.
- Card and documentation truth: PASS. Actual `Card222_013` source proves Ice
  Plains deployment, controlled-vehicle marker following, marker-3-or-lower
  firing, and the greater-than-eight generator result. Actual `Card222_014`
  and `Card222_030` source proves the objective twins flip when Main Power
  Generators are blown away. Both required histories and the mailbox describe
  the corrected source, exact magnitudes, baseline-only impacted reds, and the
  absence of packaging or runtime claims.

Verdict: PASS at `SOURCE_TESTED`. This proves source behavior, compilation,
focused and impacted regression boundaries, both-bot parity, and card-source
alignment. It does not prove packaged bytes, runtime loading, or replay firing.
No package, deployment, restart, database mutation, game mutation, or push was
performed by this verification.
