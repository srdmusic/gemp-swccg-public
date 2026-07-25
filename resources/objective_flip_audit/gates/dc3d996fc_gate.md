# Retroactive GATE — dc3d996fc

- **Commit:** dc3d996fcce4ff25fa9b09e58413edd804143a61
- **Subject:** fix(ai): prove Hunt Down objective behavior
- **Reviewer:** K-2 (independent retroactive gate)
- **Tree:** rando-consolidation-2026-06-23 @ ee64e6f3b (HEAD)
- **Verdict:** **PASS-WITH-CONCERNS**

Covers both Hunt Down variants: **7_297** (classic) and **213_31** (V / virtual). Legacy 601_87 is NOT touched by this commit.

---

## 1. SCOPE — PASS
Every touched path is AI-only. Full `--name-status` reviewed: `resources/AI_CHANGELOG.md`, `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`, `resources/objective_flip_audit/records/{7_297,213_31}.json`, `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/**`, `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`, and AI tests under `.../ai/**`. No `src/gemp-swccg-cards/**`, no `src/gemp-swccg-logic/**`, no `Card*.java`. New file `ObjectiveHardLossPolicy.java` is under `ai/models/common/phase/`. Clean.

## 2. RULE IDS — PASS (code) / CONCERN (commit message)
New/changed scoring arms carry dotted semantic ids, no new V-numbers:
- `MOVE.OBJECTIVE.ACTOR_ROUTE_START/DESTINATION`, `.REQUIRED_CARD_ENABLER_START/DESTINATION`, `.ACTOR_LOCATION_START/DESTINATION`, `.BLOCKER_CHASE_START/DESTINATION` (MoveDestinationPolicy.java:121-220).
- `BATTLE.OBJECTIVE.GLOBAL_BLOCKER_REMOVAL` (ObjectiveBattlePolicy.java:18-20).
- `PULL.OBJECTIVE.*` (PullSelectionCandidatePolicy.java:55-148).
- `ObjectiveHardLossPolicy` emits via `TraceRuleId.of(ruleId)` (lines 60-77).
**Concern:** commit message is a bare one-liner with **no `RULES:` line** naming touched ids. Documentation gap, not a behavior gap.

## 3. BOUNDARY MATH — PASS (verified by reviewer against code)
No dominance/sandwich analysis is written in the commit message or comments, so I verified the arithmetic directly:
- **Battle arms are fail-closed, not merely small.** ObjectiveBattlePolicy.evaluate (lines 144-157) returns EMPTY operations if `!bothSidesPresent || formationSafetyVeto || !predictorSafe || effectiveDiff < MIN || !reserveReady || v25Suicide`. GLOBAL_BLOCKER_REMOVAL (+250) and the other objective battle bonuses can **never** be emitted over an active safety veto. Correct.
- **Move arms are FormationSafety-gated at the source.** MoveEvaluator (rando) lines 636-648 set `safeAdvancingHop=true` only when both `FormationSafety.vetoMoveDestination` and `.vetoMoveOrigin` return null; the +600 start / +1000 destination arms return `Contribution.none()` when the gate is false (MoveDestinationPolicy.java:116-227). A veto zeroes the bonus rather than being out-scored by it.
- **+1000 destination is intentionally large** to dominate ordinary move scoring (icons ~15/icon) and steer the actor to the flip gate; it cannot override a hard veto. Low-risk residual: a non-veto *soft* penalty steering away from a destination would be overridden by +1000 — acceptable as designed steering.

## 4. OLD RULES — PASS-WITH-CONCERNS
No silent scoring deletion found in the sampled diff. The superseded hardcoded Endor V193 block is compiled out with `if (false /* SUPERSEDED */)` per discipline (visible in the sibling ObjectiveAnalyzer region). **Concern:** ObjectiveAnalyzer.java is a ~1992-line change here (and ~6892 in the sibling Endor commit) with heavy method relocation (VaderCastle* assessments). A static gate cannot certify the absence of a silently-dropped arm inside a rewrite of this size. Regression reliance is on the in-commit golden + behavior + parity suite (see items 5, 7). Golden anchor `ObjectiveAnalyzerSharedGoldenTest` moved `16→18` enabled profiles — exactly the two new Hunt Down loader profiles, a legitimate minimal update.

## 5. PARITY — PASS
Both bots changed symmetrically (rando/ and chosenone/ ActionText, CardSelection, Combined, Deploy, Move, PullPolicyAdapter). `HuntDownObjectiveAdapterSourceParityTest` asserts the two bots' evaluator **source is byte-identical after namespace normalization** (`models.rando`/`models.chosenone` → `models.BOT`, lines 24-26, 219-220) — parity is structural, not just behavioral. Plus HuntDownCastleMoveSourceParityTest and HuntDownLocationDownloadActionTextParityTest.

## 6. TRUTH CONFORMANCE — PASS
- **7_297 (classic):** playbook `hunt-down-classic-front` = allOf {Vader `at` battleground_site (controller any), zero opponent `Jedi_or_Luke` at any battleground} ; back = anyOf {Vader `onTable` count==0, opponent Jedi_or_Luke at battleground}. **Padawan is correctly EXCLUDED** from the classic blocker.
- **213_31 (V):** identical shape but blocker = `Jedi_Padawan_or_Luke` — padawan screening present, matching the variant delta.
- **No loose title matching.** Actor registry (ObjectiveAnalyzer.java:7944-7969) maps `"Vader"→Filters.Vader` (Persona), `"Jedi_or_Luke"→or(Jedi,Luke)`, `"Jedi_Padawan_or_Luke"→or(Jedi,padawan,Luke)` — all engine `Filters`, no substring match. The record explicitly encodes "a title containing Vader without Persona.VADER is not the actor."
- Both records upgraded `SOURCE_VERIFIED → FLIP_OBSERVED` with corrected structured front/back laws, controller-any actor, and on-table back semantics.

## 7. TEST HONESTY — PASS
Real decision/rule-id assertions with negative controls:
- HuntDownObjectiveBehaviorTest (106 asserts): `impostorVader` fixture (title-contains-Vader, not Persona) asserts front NOT satisfied (lines 60-64); `PadawanBlocksOnlyTheVirtualObjective` (line 231) proves Padawan blocks the V objective but NOT the classic — the exact variant delta as a paired positive/negative control.
- HuntDownObjectiveEngineContractTest uses unchanged card/engine code for real flip/flip-back sequences.
- HuntDownObjectiveDecisionPolicyTest locks exact rule ids + magnitudes.

## 8. KNOWN DEFECT (V201 non-additive DEFER) — CONCERN
DeployEvaluator still routes through `DeployPlanPolicy.evaluate` / `AdapterStep.CONTINUE_ACTION` (rando DeployEvaluator.java:1482-1507); neither DeployPlanPolicy.java nor the CombinedEvaluator DEFER enforcement is modified by this commit (CombinedEvaluator diff contains no DEFER/plan changes). Therefore the V201 non-additive plan DEFER is **unchanged and not reconciled** with the new objective deploy destinations: if the plan latches a different destination, the new objective deploy scoring can still be deferred. Pre-existing defect, not introduced here, but it is an **untested boundary** for the new Hunt Down deploy behavior.

---

## Follow-ups for the lead
1. Add a `RULES:` line to the commit (or an amend note) enumerating the new dotted ids (MOVE.OBJECTIVE.*, BATTLE.OBJECTIVE.GLOBAL_BLOCKER_REMOVAL, PULL.OBJECTIVE.*, DEPLOY.BUDGET.*).
2. Record the boundary/dominance analysis for the +600/+1000 move arms and +250 battle arm somewhere durable (comment or changelog) — the fail-closed gating is correct but only provable by reading two files.
3. Add or point to a test that exercises the V201 plan-DEFER vs. objective-deploy-destination conflict for Hunt Down, or explicitly document it as a known deferred limitation.
4. Nice-to-have: a behavior test that would fail if any relocated ObjectiveAnalyzer scoring arm were dropped in the rewrite (golden coverage of the specific arms, not just profile counts).
