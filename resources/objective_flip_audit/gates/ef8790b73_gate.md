# Retroactive GATE — ef8790b73

- Commit: `ef8790b730a3b4be3f1d4ee271292d967e3ad3cd`
- Subject: fix(ai): prove Invasion objective flip chain
- Reviewer: K-2 (independent retroactive gate; static review, no build run)
- HEAD at review: ee64e6f3b, branch rando-consolidation-2026-06-23

## VERDICT: PASS-WITH-CONCERNS

Behaviorally sound and truth-conformant. The only concerns are documentation-form (bare one-line commit message) and claims a static gate cannot verify (jar hashes, reactor counts). No behavioral regression found.

---

## Per-item findings

### 1. SCOPE — PASS
All 36 touched paths are AI-only. Production Java is confined to `.../ai/models/common/**`, `.../ai/models/rando/**`, `.../ai/models/chosenone/**`; the objective profile `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`; both changelogs (`resources/AI_CHANGELOG.md`, `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`); and tests under `.../ai/models/**` (including `common/strategy/InvasionObjective*Test.java`). No `src/gemp-swccg-cards/**`, no `src/gemp-swccg-logic/**`, no `Card*.java`. Automatic-FAIL trigger not hit.

### 2. RULE IDS — PASS (with commit-message concern)
New scoring arms carry dotted semantic ids, no new V-numbers:
- `BATTLE.OBJECTIVE.REQUIRED_LOCATION_CONTEST`, `.REQUIRED_CARD_CONTROL_ENABLER`, `.REQUIRED_CARD_RETENTION`, `.HARD_LOSS_LOCATION_DEFENSE`, `.GLOBAL_BLOCKER_REMOVAL` (ObjectiveBattlePolicy.java).
- `BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD` (BattleForfeitPolicy.java:113).
- `MOVE.OBJECTIVE.REQUIRED_CONTROL_HOLD`, `.REQUIRED_CARD_ENABLER_HOLD`, `.HARD_LOSS_LOCATION_HOLD`, `.REQUIRED_CARD_RETENTION_HOLD`, `.FLIP_BACK_BLOCKER_HOLD`, `.POST_FLIP_SURVIVAL_HOLD`, `.RUNTIME_ACTOR_HOLD` (MoveObjectiveGateHoldPolicy.java).
- `OBJECTIVE.DEPLOY.REPLAN_GATE_APPEARED`, `.REPLAN_ACTOR_ARRIVED`, `.ENABLER_PLAN` (DeployPhasePlanner.java).
Legacy V-tag arms (V21/V22/V25/V139/V159...) retained verbatim. CONCERN: the commit message is a bare one-line subject with **no `RULES:` line**; the rule-id/magnitude documentation lives only in the two changelogs. Recommend a `RULES:` trailer on future commits.

### 3. BOUNDARY MATH — PASS (documented in changelog, not commit msg)
Verified magnitudes against code:
- `ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS = 80.0f` (BANDED), gated behind six safety predicates: `bothSidesPresent`, `!formationSafetyVeto`, `predictorSafe`, `effectiveDiff >= -2`, `reserveReady` (reserve>=3 OR raw overpower>=8), `!v25Suicide`. Only fires on an already-safe battle; +80 biases target selection, cannot force an unsafe battle. Hard-loss / global-blocker arms = 250.0f, same safety gate.
- `BattleForfeitPolicy` FLIP_GATE_FORMATION_HOLD = VETO `-9999.0f` (line 148), fired only when `hasUnprotectedLegalAlternative` is true (BattleForfeitPolicy.java:107, computed in BattleForfeitFacts.readFlipGateFormationSelection). Sits on the *same* -9999 veto floor as the pre-existing V21/V48 vetos — no new magnitude scale introduced; unavoidable mandatory losses stay selectable.
- MoveObjectiveGateHoldPolicy holds are applied by the adapters as ladder `HARD_VETO` = `-100000` (MoveEvaluator.java:242, :995-1001, :1318). This dominates R3 survival (+12000), but every hold releases when `opponentPowerAtLocation > friendlyPowerAtLocation + RETREATABLE_POWER_GAP(6.0f)`. That ±6 gap is the sandwich boundary: the veto binds only when the position is defensible, so it never traps a piece into death; when overpowered by >6 the ordinary retreat/survival ladder resumes.
No new arm silently dominates an older one at a realistic boundary.

### 4. OLD RULES — PASS
Removed-line scan over production Java shows no deletion of any `PolicyOperation.add`, `addReasoning`, or score magnitude. The only removals are `if (actorCandidates.isEmpty()) return null;` (deliberately replaced by the funded ENABLER_PLAN branch, DeployPhasePlanner) and a `holdLocations.add(bestLoc)`→`bestLocation` rename inside MovePostFlipConsolidationPolicy whose javadoc states it "preserves the legacy strongest-two selection and strict insertion-order tie behavior." No silent scoring deletions.

### 5. PARITY — PASS
Normalized diff (model token stripped) is byte-identical between `rando/**` and `chosenone/**` mirrors: DeployPhasePlanner (97 lines each), MoveEvaluator (33), CardSelectionEvaluator (37), DeployEvaluator (26). Parity tests present in-commit: DeployPlanRankingAdapterParityTest, BattleForfeitAdapterParityTest, MoveObjectiveGateHoldSourceParityTest, MovePostFlipConsolidationSourceParityTest.

### 6. TRUTH CONFORMANCE — PASS (vs 14_113.json, SOURCE_VERIFIED)
- Front (unchanged in profile) = `allOf`: controlsWith Theed_Palace_Throne_Room + Neimoidian AND control Naboo_system. Matches 14_113 flipToBack allOf.
- Back (added by this commit) = `mode: anyOf`, opponent control Naboo_system>=1 OR opponent control Theed_Palace_Throne_Room>=1. Matches 14_113 flipToFront `Filters.or(Naboo_system, Theed_Palace_Throne_Room)` opponent-control law exactly.
- Post-flip hold protects exact Naboo system + Throne Room (not Swamp-by-substring), consistent with requirementSemantics that occupation/presence is insufficient and control is required.

### 7. TEST HONESTY — PASS
InvasionObjectiveFullChainBehaviorTest: 66 asserts, real front/back card triggers, explicit negative control ("Swamp does not substitute"; exact Throne without Naboo does not flip). ObjectiveBattlePolicyTest asserts the +80 boundary and negative controls: everyPositiveSafetyFactIsRequired, lowReserveRequiresRawOverpowerMarginOfEight, v25SuicideRemainsExcludedEvenWhenEffectiveDiffPasses. BattleForfeitPolicyTest (25 asserts) covers the -9999 hold and the legal-alternative gate. Tests assert decisions/rule-ids, not merely non-zero scores.

### 8. KNOWN DEFECT INTERACTION (V201 DEFER) — PASS
`DeployPlanPolicy.java` (the non-additive DEFER, `evaluateDestinationTarget` → `PolicyOperation.defer("deploy-plan-target-defer")`) is **not touched** by this commit. The Invasion fix does not bypass the DEFER; instead DeployPhasePlanner now invalidates a same-turn cached plan when the exact flip gate enters play (`hasActiveFlipGateLocation`) or the typed actor reaches hand (`hasFlipGateActorInHand`), forcing a rebuild so the DEFER defers to a fresh, correct planned target rather than the stale Swamp target. The changelog explicitly records that V201's non-additive DEFER disproved the earlier "V193 can just bypass V201" inference — the fix respects the defer, it does not route around it. Plan integrity for non-objective cases is unchanged (DEFER logic untouched).

---

## Follow-ups for the lead
1. Add a `RULES:` trailer (and a one-line boundary-math note) to future objective commit messages; today they are one-liners and all rationale lives in the changelogs.
2. Jar SHA-256 values and reactor pass counts (2114/0/0/26) in the changelog are **unverified by this gate** — static review only; re-confirm at deploy gate.
3. Nothing behavioral to fix.
