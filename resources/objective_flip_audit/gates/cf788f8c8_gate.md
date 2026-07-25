# Retroactive GATE — cf788f8c8

- Commit: `cf788f8c838686eed48d1e4c19082852becc6d74`
- Subject: fix(ai): prove We Have A Plan flip route
- Reviewer: K-2 (independent retroactive gate; static review, no build run)
- Objective covered: We Have A Plan (14_52, LIGHT); flip gate = Naboo: Theed Palace Throne Room (shared with Invasion 14_113)

## VERDICT: PASS-WITH-CONCERNS

Faithful transcription and a genuinely good de-duplication catch (the shared Throne Room gate is not double-scored across the two set-14 objectives). Concerns: bare commit message, and a truth-record edit that escalates proofGate on claims a static gate cannot verify.

---

## Per-item findings

### 1. SCOPE — PASS
All 33 touched paths AI-only: shared `.../ai/models/common/**`, mirrored `rando/**` + `chosenone/**`, `objective_playbooks.json`, both changelogs, tests under `.../ai/models/**`, plus `resources/objective_flip_audit/records/14_52.json` (a `resources/**` doc — permitted). No cards/logic/Card*.java.

### 2. RULE IDS — PASS (with commit-message concern)
New dotted ids in the shared analyzer/policies: `DEPLOY.OBJECTIVE.ACTOR_ROUTE_STAGING`, `MOVE.OBJECTIVE.ACTOR_ROUTE_START`, `.ACTOR_ROUTE_DESTINATION`, `MOVE.OBJECTIVE.FLIP_BACK_BLOCKER_HOLD`, `BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD`, `BATTLE.OBJECTIVE.REQUIRED_LOCATION_CONTEST`, plus the shared activation matcher recognizing exact `Activate 1 Force`. No new V-numbers. CONCERN: bare one-line commit message, no `RULES:` line.

### 3. BOUNDARY MATH — PASS (and a de-dup fix in the right direction)
Key catch: because WHAP (14_52, LIGHT) and Invasion (14_113, DARK) both key off Theed Palace Throne Room, the commit stops the generic required-location score from double-stacking the dedicated Invasion gate score ("the generic required-location score no longer double-stacks the dedicated Invasion gate score", changelog). Verified this is an additive-suppression guard, not a magnitude deletion (see item 4). Move/forfeit protections reuse the established `-9999` forfeit veto and `-100000` ladder move-veto, both released at the `+6` power gap; changelog confirms "That protection releases when opponent effective power leads by more than six." Formation Safety still hard-vetoes a doomed route ahead of the objective route bonus (record `contradictions`: "Formation Safety remains absolute before the parent +600 or destination +1000 route contribution"). No new arm silently dominates.

### 4. OLD RULES — PASS
Removed-line scan over production Java: no deletion of any scoring magnitude or `PolicyOperation.add`. The double-stack fix is implemented as a guard, not by deleting an arm.

### 5. PARITY — PASS
Normalized mirror diffs byte-identical: MoveEvaluator (118 each), CardSelectionEvaluator (72), ActionTextEvaluator (7). Parity tests in-commit: ActivateActionTextPolicyParityTest, BattleForfeitAdapterParityTest, MoveDestinationSourceParityTest, MoveObjectiveGateHoldSourceParityTest, DeployObjectiveSitingResidualSourceParityTest, ResponsePolicySourceParityTest.

### 6. TRUTH CONFORMANCE — PASS (vs 14_52.json)
Profile: front `controlsWith` exact Theed_Palace_Throne_Room + actor Amidala, count 1, includeExcludedFromBattle; back opponent control exact Throne Room, count 1. Matches 14_52 flipToBack/flipToFront law. The commit's edit to 14_52.json changes **only the profile section** (`loaderEnabled` false→true, added structured-rule descriptions, added missingState/flipPreservation/runtimeOwners/testEvidence blocks, `proofGate` SOURCE_VERIFIED→FLIP_OBSERVED). The flip LAW itself (flipToBack/flipToFront expressions, citations, requirementSemantics) is **unchanged** — the code was not built to a bent record. Route legality (Amidala must MOVE into the interior Throne Room, cannot deploy there; boosted Courtyard→Throne allowed) matches record semantics and contradictions.

### 7. TEST HONESTY — PASS
WeHaveAPlanCombinedEvaluatorDecisionTest: 36 asserts, 35 negative-control hits — production CombinedEvaluator (both bots) chooses Padme deploy over Pass, Courtyard over an exterior distractor, Hallway/Throne over wrong-way moves, a prohibited closer site gets no doctrine score, and Formation Safety still vetoes a doomed route. WeHaveAPlanObjectiveEngineContractTest fires the real front flip and the real flip-back (Vader battles Padme away, takes Throne control). Asserts decisions/rule-ids with distractors, not bare non-zero scores.

### 8. KNOWN DEFECT INTERACTION (V201 DEFER) — PASS
`DeployPlanPolicy` DEFER untouched. WHAP uses the exact-setup route staging (DEPLOY.OBJECTIVE.ACTOR_ROUTE_STAGING) available "only when the engine offers a legal exterior route stage," so it never invents an illegal direct interior deploy — consistent with the DEFER-protected plan flow. Plan integrity for non-objective cases unchanged.

---

## Follow-ups for the lead
1. Add a `RULES:` trailer to the commit message.
2. **proofGate escalation is unverifiable statically:** 14_52.json now asserts `FLIP_OBSERVED` with server/web jar SHA-256 values and reactor counts (2187/0/0/26) that this gate did not build or hash. The escalation rests on a deterministic engine test (WeHaveAPlanObjectiveEngineContractTest), which is a reasonable basis, but the jar-hash and package claims must be re-confirmed at the build/deploy gate before treating FLIP_OBSERVED as fully proven.
3. No behavioral fix required.
