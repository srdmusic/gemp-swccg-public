# K-2 HANDOFF — 2026-07-02 — Fable 5 onboarding

You are K-2 (see `.claude/CLAUDE.md` for persona + comm rules: concise, no em-dashes in prose,
single-layer bullets, push back when Steve is wrong, tables for Steve — he has ADHD + dyslexia).

## First reads, in order (non-negotiable)
1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — standing rules. The
   `feedback_*` entries are law. Especially: one-change-at-a-time, breadcrumbs-every-fix,
   check-rule-is-live-before-editing, update-old-rule-not-new-version, card-search-by-type-not-text.
2. `Handoffs/K2_MASTER_HANDOFF_2026-07-02.md` — THE master router (verified state, both queues, landmines).
   (POINTER UPDATED 2026-07-01 by the Master K-2; this line used to send you to the bannered 06-23 master, now archived.)
3. `resources/k2-resources/distilled/00-START-HERE.md` — project overview, build/deploy, Rando architecture (stale past ~V186).
4. THIS FILE — what happened 2026-06-28 → 07-02 and what is queued next.

## The one discipline you cannot break
Rando scoring is ADDITIVE. Old rules do not go missing — they get DOMINATED by bigger new numbers.
Do the boundary math at the boundary cases BEFORE writing code. Four incidents so far.

## State of the branch (local only, NOTHING pushed to GitHub — Steve's standing order)
Branch: `rando-consolidation-2026-06-23`. Committed, deployed to the running jar, verified live:

| Commit | What | Status |
|---|---|---|
| `d72ced949` | V188: Set Your Course For Alderaan — ability chars get -900 at Death Star sites (front side only) | live, awaits a live Alderaan game |
| `f664cc2ba` | V120 fix: "Deploy Vader from Reserve Deck" no longer mis-blocked as a weapon pull ("vader" ⊂ "Darth Vader's Lightsaber") | live, awaits a live Hunt Down game (watch Vader deploy) |
| `37c352d87` | V179 fix: a [download] naming a location the bot HOLDS no longer outranks deploying that location from hand (A Good Friend / Ahch-To / Be With Me) | live, awaits a live Saga game (Ahch-To first, then Be With Me pulls) |

UNCOMMITTED (the other K-2's work — live in the jar, code not committed). CORRECTION 2026-07-01:
this is FOUR distinct changes, not one, each now documented in AI_CHANGELOG + AI_VERSION_HISTORY
(entries backfilled 2026-07-01):
- V61c destiny buffer (keep 3 in reserve): `ForceActivationEvaluator.java` + `ActionTextEvaluator.java`
- V61b overpower battle (fight with empty reserve when overpowering by ≥8): `BattleEvaluator.java`
- V79b Death Star parsec steering (Verge → Scarif): `RandoCalAi.java` + `MoveEvaluator.java` (the
  MoveEvaluator half is a marked-INERT fallback; the RandoCalAi handler is the real fix)
- log4j mainlog appender (decision logs survive restarts): `prod-log4j.xml`
Commit as FOUR separate commits when Steve says commit. Ask Steve before committing or discarding.
git stages whole files — two prior commits accidentally bundled cross-K2 work. If two sessions run
again, ask Steve for a worktree.

## THE QUEUED WORK — TDIGWATT (Steve said "I want all four fixes"; two shipped, these two remain)
Game: 2026-07-02 02:09, Rando DARK, objective "This Deal Is Getting Worse All The Time"
(Card226_012 variant, deploys with I'm Sorry, FORBIDS deploying Executor). Symptom: Rando deployed
zero Bespin sites after the start, flooded weak solo characters, never flipped, got clobbered.
Diagnosis is workflow-verified with file:line evidence (adversarial, 3 lenses). NOT a regression
from V120/V179 (zero log hits; ruled out). Two pre-existing bugs:

### Bug A (primary) — V177 dead-search false negative starves the site downloads
- I'm Sorry (Card226_006) may [download] "an interior Cloud City site" once per turn. Cloud City:
  Dining Room + Security Tower sat in Reserve ALL GAME.
- V177 (`rando/evaluators/ActionTextEvaluator.java` ~246) calls the RAW `DeckOracle.hasTargetInZone`
  (substring + last-word ≥4 matcher, DeckOracle ~853-885). The target "interior cloud city site"
  cannot substring-match titles like "Cloud City: Dining Room", and the ≥6-char word-rescue only
  tries "interior" (no title contains it). So V177 declared DEAD and hard-blocked -2000 EVERY turn.
- Meanwhile V67h's V82.1 category fallback (DeckOracle ~1176-1187) maps type-word "site" →
  CardCategory.LOCATION and correctly logged "present in RESERVE_DECK". Two detection paths
  disagree; the weaker one blocks. This is the detection-path-mismatch class from
  `Rando_AI_Rule_Audit.xlsx` — consult it before coding.
- FIX: before V177 declares DEAD, run the same V82.1 category fallback V67h uses. If the category
  fallback finds the target in Reserve, do not block. Mirror to chosenone. This ADJUSTS V177 in
  place — no new V-tag.

### Bug B (secondary) — V29 BESPIN-FIRST demands a ship the objective forbids
- `rando/evaluators/DeployEvaluator.java` ~1137-1213: when `needsBespinSystemPresence()` is true
  (ObjectiveAnalyzer ~1027-1033, trips on bespin/cloud city flip fragments), every bare character
  deploy gets -500 "deploy Executor first" until Rando occupies Bespin space.
- THIS objective forbids deploying Executor, and the deck has no other capital ship (inferred from
  the log, verify against the decklist), so the gate never releases. Rando controlled 1 CC site all
  game vs the 3 needed to flip.
- FIX: release/skip the gate when the objective text forbids Executor (or no live path to occupy
  Bespin space exists). Mirror to chosenone. Adjusts V29 in place.

### Order + boundary notes
- Build A first (highest value), then B. ~75% confident BOTH are needed: A restores the site
  downloads, but if B keeps starving character deploys the sites sit uncontrolled.
- Boundary math: removing the -2000/-500 must not let these actions silently dominate. The site
  download should land in the LOCATIONS bucket at its natural score; character deploys return to
  their normal V38/V136 scoring. Check nothing else was leaning on those penalties.
- Evidence source: the game is at the HEAD of `logs/gemp-swccg.log` (starts 02:09:13). It rotates
  at 10MB into `logs/2026-06/app-*.log.gz` (gunzip -c to read). Key lines if rotated: V177 blocks
  ("V177 DEAD SEARCH blocked ... [interior cloud city site] (source 'I'm Sorry')"), V67h disagreeing
  ("type-word 'interior cloud city site' → category LOCATION present"), V29 fires ("V29 BESPIN-FIRST:
  BLOCKING ... deploy Executor first!").

## ALSO QUEUED — V61c battle-intent refinement (Steve approved 2026-07-01, plan only, not built)
Steve: "If Rando intends to battle that turn, he needs to save 3. If he intends to deploy and end
turn without battling he can activate all force... Let's come up with a plan for bypassing that
rule in the event that Rando wants to deploy and end turn without battling."
- Today V61c ALWAYS protects the 3-card destiny buffer (passes activation at reserve ≤ 3). The
  refinement: bypass the protection on turns Rando will NOT battle, so it can activate everything.
- Hard part: Activate phase comes BEFORE Deploy/Battle, so battle intent must be PREDICTED at
  activation time. Conservative predictor v1: at activation, scan for any contested location
  (both sides' presence) — the same scan V61b uses in BattleEvaluator (~627). Contested site
  exists → battle plausible → keep 3. Zero contested sites → deploy-and-end turn → activate all.
- Bias toward keeping 3 when unsure: a false "no battle" prediction re-opens the no-destiny bug
  for that turn; a false "battle" prediction only costs a little activation.
- Touch points: one shared helper (e.g. in DecisionContext or a strategy util) called from BOTH
  ForceActivationEvaluator (~186, the amount cap) and ActionTextEvaluator (V168 carve-out ~166 +
  V38.3 carve-out ~1342). All three sites must use the SAME predicate or they fight each other —
  that mismatch is exactly how the original V61c bug happened (V168 vs the cap).
- Boundary: does not touch V61b (battle-time overpower exception) or V67at (end-game reserve-2).

## Build / deploy / verify (full detail: `resources/BUILD_AND_DEPLOY.md`)
- Compile check: `docker exec gemp_swccg_app_1 bash -c "cd /opt/gemp-swccg/src && mvn -q -pl gemp-swccg-server -am compile"`.
- Deploy: `bin/gemp reload-ai` (builds web.jar, restarts JVM, flips switches). `rebuild`/`rebuild-fast`
  do NOT restart the JVM — trap.
- Verify live, never trust "it compiled": python3 zipfile byte-search the class inside
  `src/gemp-swccg-async/target/web.jar` for a string only your change adds, confirm the java
  process restarted (small etime), HTTP 200 on `http://localhost:17001/gemp-swccg/`.
- Before editing ANY rule: grep its enclosing `if (...)` for `if (false /* SUPERSEDED ... */)` —
  many V-tags are compiled out. A prior K-2 lost a day editing dead V67al.
- Every fix ships with breadcrumbs the SAME session: code comment + `resources/AI_CHANGELOG.md`
  + `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md` (newest entries near
  line ~3219, `════ Vxxx ════` format) + commit message. Mirror rando fixes to `chosenone/`.
- Commit scope: stage ONLY the files your fix touched + the two changelogs.

## Steve's tracker
`resources/Rando_Issues_2026-06-29.xlsx` — the four-issue table from the last session (all green).
Add TDIGWATT bugs A + B to it (or a fresh dated file) when you start; Steve reads tables, not prose.

## Suggested first message from Steve to you
"Read Handoffs/K2_HANDOFF_2026-07-02_fable5-onboarding.md and build the two TDIGWATT fixes (A then B)."
