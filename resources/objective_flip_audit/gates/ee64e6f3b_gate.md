# Retroactive GATE — ee64e6f3b

- **Commit:** ee64e6f3b83d69ecb41d301d4475935084947cc9
- **Subject:** fix(ai): prove Endor Operations objective behavior
- **Reviewer:** K-2 (independent retroactive gate)
- **Tree:** rando-consolidation-2026-06-23 @ ee64e6f3b (HEAD)
- **Verdict:** **PASS-WITH-CONCERNS**

Objective: **8_167** Endor Operations. The headline check is whether the commit CORRECTED the previously-WRONG Bunker/location-control classification rather than building on it. It did.

---

## 1. SCOPE — PASS
Every touched path is AI-only (full `--name-status` reviewed): `resources/AI_CHANGELOG.md`, `AI_VERSION_HISTORY.md`, `resources/objective_flip_audit/records/8_167.json`, `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/**` (incl. RandoCalAi.java / TheChosenOneAi.java, both under `ai/models/`), `objective_playbooks.json`, and AI tests. No engine/card java. Clean.

## 2. RULE IDS — PASS (code) / CONCERN (commit message)
New arms carry dotted semantic ids: `BATTLE.OBJECTIVE.HARD_LOSS_LOCATION_DEFENSE`, `.GLOBAL_BLOCKER_REMOVAL`, `.REQUIRED_CARD_CONTROL_ENABLER`, `.REQUIRED_CARD_RETENTION`, `.REQUIRED_LOCATION_CONTEST` (ObjectiveBattlePolicy); `DEPLOY.BUDGET.OBJECTIVE_REQUIRED_CARD_RESERVE` (DeployBudgetPolicy.java:118); `PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD*`, `.REQUIRED_CARD_ENABLER_*` (PullSelectionCandidatePolicy). No new V-numbers. **Concern:** bare one-liner commit message, **no `RULES:` line**.

## 3. BOUNDARY MATH — PASS (verified by reviewer against code)
- **Battle arms fail-closed:** ObjectiveBattlePolicy.evaluate (lines 144-157) returns empty operations unless every veto passes (formationSafetyVeto, predictorSafe, effectiveDiff, reserveReady, v25Suicide). HARD_LOSS_LOCATION_DEFENSE and GLOBAL_BLOCKER_REMOVAL (+250) cannot override a safety veto.
- **Deploy reserve arms mirror the established idiom:** `DEPLOY.BUDGET.OBJECTIVE_REQUIRED_CARD_RESERVE` and `OBJECTIVE_FORMATION_RESERVE` are **-500.0f** hold-back penalties applied only when an off-plan deploy would leave insufficient Force (DeployBudgetPolicy.java:108-123) — the exact magnitude and shape of the existing `V48` Vader-move-reserve arm two blocks down. No new dominating magnitude introduced.
- No written dominance analysis in the commit; verified directly.

## 4. OLD RULES — PASS-WITH-CONCERNS
- The old hardcoded V193 Endor block (which set `flipCriticalControlSite = "endor: bunker"` as a flip proxy) is compiled out with `if (false /* SUPERSEDED 2026-07-08 — Endor now JSON-hydrated */)` and the V193 tag preserved for grep (ObjectiveAnalyzer.java ~9989-10023). Correct comment-out discipline.
- The surviving `flipGateSite "endor: bunker"` in the JSON profile is **correctly demoted**: it hydrates `flipCriticalControlSite`, whose only consumer, `isActiveRequiredCardControlEnablerLocation` (ObjectiveAnalyzer.java:5409-5435), is documented and scoped as *"a deploy enabler, not the objective's flip condition,"* gated to the exact Bunker-gated Establish Secret Base ids `{207_25,207_025,601_260}`. It does **not** feed flip detection.
- **Concern:** ObjectiveAnalyzer.java is a ~6892-line change with heavy relocation; a static gate cannot fully certify no scoring arm was silently dropped. Regression reliance is on the in-commit golden/behavior/parity suite.

## 5. PARITY — PASS
rando/ and chosenone/ evaluators changed symmetrically; RandoCalAi.java and TheChosenOneAi.java both +75 lines. Endor tests assert matching Rando/Chosen One action ids, raw score bits, reasons, veto/defer state, and winners (per record `testEvidence.parity`); `forceLossProtectsSoleRequiredEffectAcrossRecyclableZones` and the Force-loss parity tests table-drive both bots.

## 6. TRUTH CONFORMANCE — PASS (the critical item)
The 8_167 record and the code both now encode the **pure card-on-table** flip law, correcting the prior WRONG Bunker-proxy classification:
- **Record corrected:** the old `profileGaps` (flipGateSite "endor: bunker" proxy; Ominous Rumors missing from flipGateCardIds; biker_scout mis-flagged as a flip actor) are **removed**. New `frontStructuredRule` = allOf {Ominous Rumors onTable, Establish Secret Base onTable, active-spotted, include-excluded-from-battle}; `backStructuredRule` = anyOf {either count == 0}. `contradictions` explicitly states *"Bunker control is not the objective's flip law. It is only a deploy prerequisite for specific Establish Secret Base printings."*
- **Code matches:** playbook `endor-operations-front-required-effects` (allOf, `actorFilterKey` Ominous_Rumors + Establish_Secret_Base, `relation: onTable`) and `-back-required-effects` (anyOf, count==0). `assessFlipLocationRules` (ObjectiveAnalyzer.java:389-430) evaluates allOf/anyOf on-table state. **Ominous Rumors is now represented** (it was the missing card). Registry maps both titles to engine `Filters.Ominous_Rumors` / `Filters.Establish_Secret_Base` (lines 7964-7966) — no loose matching.
- Printing-specific deploy routes are correctly separated as **enablers, not flip requirements** (classic 8_124 = 3 controlled Endor sites; V/Legacy ESB = Bunker control; classic Ominous Rumors 8_127 = opponent controls zero Endor sites; V/Legacy Ominous Rumors = direct on Bunker). Reactor Core 9_146 suspension → real flip-back is modeled as `requiredCardInactivationRules`.

## 7. TEST HONESTY — PASS
Six new Endor test classes with high assertion density (e.g. EndorOperationsCombinedEvaluatorDecisionTest ~222 asserts, EndorOperationsDecisionPolicyTest ~42). Coverage per record and spot-check includes: one-of-two advancement vs two-of-two completion, post-flip sole-blocker, Reactor Core suspension-driven flip-back, Colonel Dyer cancellation prevention + safe relocation, a real Deactivate The Shield Generator destiny-13 Bunker blow-away placing the objective out of play, Force-loss protection across recyclable zones, and `forceLossMaySpendOfferedUsedCopyWhenBetterCopySurvives` (a genuine boundary/negative control). EndorOperationsObjectiveEngineContractTest uses unchanged card/engine code.

## 8. KNOWN DEFECT (V201 non-additive DEFER) — CONCERN
DeployPlanPolicy.java is not touched. The new Endor objective deploy scoring (DeployObjectiveSitingPolicy, DeployBudgetPolicy) flows through the deploy adapter that consults the plan DEFER. The plan-vs-objective destination conflict is **not reconciled** and remains an untested boundary. (Note: the `V25-CC-SPREAD-DEFER` seen in DeployObjectiveSitingPolicy is a separate Cloud City spread mechanism, not the V201 plan DEFER.) Pre-existing, not introduced here.

---

## Follow-ups for the lead
1. Add a `RULES:` line to the commit enumerating new ids (BATTLE.OBJECTIVE.HARD_LOSS_LOCATION_DEFENSE/GLOBAL_BLOCKER_REMOVAL/REQUIRED_*; DEPLOY.BUDGET.OBJECTIVE_REQUIRED_CARD_RESERVE; PULL.OBJECTIVE.*).
2. Consider deleting or NULL-ing the legacy `flipGateSite "endor: bunker"` / `flipGateCardIds` in the Endor JSON profile now that the structured on-table law owns the flip, OR add a comment in the profile itself that these fields are enabler-only — today the "not a flip proxy" intent lives only in Java, and a future reader of the JSON could reintroduce the wrong assumption.
3. Point to (or add) a test for the V201 plan-DEFER vs. objective-deploy-destination conflict, or document it as an accepted deferred limitation.
4. Nice-to-have: golden coverage that fails if a specific relocated ObjectiveAnalyzer arm is dropped in the 6892-line rewrite, beyond the profile-count anchor.
