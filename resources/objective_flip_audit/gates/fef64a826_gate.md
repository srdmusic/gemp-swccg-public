# Retroactive GATE — fef64a826

- Commit: `fef64a826811d42c6d2bfd075b6cf25b16c1f862`
- Subject: fix(ai): prove regional objective flip chains
- Reviewer: K-2 (independent retroactive gate; static review, no build run)
- Objectives covered: Dantooine Base Operations (7_135, LIGHT), Ralltiir Operations (7_300, DARK), Zero Hour (219_48, LIGHT)

## VERDICT: PASS-WITH-CONCERNS

Three regional/counted objective profiles transcribed faithfully from SOURCE_VERIFIED law; mirrored logic and tests are clean. Concerns are documentation-form and one unverified law-adjacent detail (Ralltiir place-out-of-play wiring).

---

## Per-item findings

### 1. SCOPE — PASS
All 30 touched paths AI-only: shared `.../ai/models/common/**`, mirrored `rando/**` + `chosenone/**`, `objective_playbooks.json`, both changelogs, and tests under `.../ai/models/**`. No cards/logic/Card*.java.

### 2. RULE IDS — PASS (with commit-message concern)
New profile rule ids are dotted and descriptive: `dantooine-base-operations-front-control` / `-back-control`, `ralltiir-operations-front-control` / `-back-control`, `zero-hour-front-control-or-occupy`, `zero-hour-back-control-margin`. New analyzer/policy arms reuse the shared `MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD`, `DEPLOY.OBJECTIVE.*`, and pull rule ids. No new V-numbers. CONCERN: bare one-line commit message, no `RULES:` line; magnitudes/ids documented only in changelogs.

### 3. BOUNDARY MATH — PASS
Changelog states and code confirms: Reserve-Deck actor pull `+400`, location pull `+300`, exact missing-location deploy `+600`. These are PULL/DEPLOY-domain arms; they sit above the deploy-plan planned-target arm (+200/-100 in DeployPlanPolicy) so an objective formation correctly outranks generic planned-target following — intended. Move holds (`MoveObjectiveGateHoldPolicy.evaluateCountedFormation`, `evaluatePostFlipBlocker`) are `hardVeto`→ ladder `-100000`, each released when `opponentPower > friendlyPower + 6` (RETREATABLE_POWER_GAP). Redundant fourth qualified location is explicitly released (evaluateCountedFormation returns none for non LAST_REQUIRED_ACTOR/BUDDY roles). Zero-Hour back uses count comparator `>` with `referenceController: self` (strict opponent>self) so ties stay flipped — matches law, no magnitude collision. No new arm silently dominates.

### 4. OLD RULES — PASS
Removed-line scan over production Java: zero deletions of scoring/veto magnitudes or `PolicyOperation.add`. Additive-only changes plus the new counted-formation branches.

### 5. PARITY — PASS
Normalized mirror diffs byte-identical: DeployPhasePlanner (28 each), MoveEvaluator (73), CardSelectionEvaluator (118), DeployEvaluator (8). Parity/ownership tests in-commit: DeployPlanRankingAdapterParityTest, DeployPlanRankingSourceOwnershipTest, ForceLossCardSelectionPolicyParityTest, MoveObjectiveConsolidationSourceParityTest, MovePostFlipConsolidationSourceParityTest.

### 6. TRUTH CONFORMANCE — PASS (each objective vs its SOURCE_VERIFIED record)
- **Dantooine (7_135):** profile front = controlWith `Dantooine_site` >=3, actor `Rebel`, opponent control `Dantooine_location` ==0, includeExcludedFromBattle. Back = opponent control `Dantooine_location` >=2 with `LEGACY__MORE_DANGEROUS_THAN_YOU_REALIZE__REQUIRES_THREE_SITES_TO_FLIP_BACK` modifiedValue 3. Matches record exactly, including the site-vs-location asymmetry and the legacy-mod threshold bump.
- **Ralltiir (7_300):** front = controlWith `Ralltiir_site` >=3, actor `Imperial`, opponent control `Ralltiir_location` ==0. Back = opponent control `Ralltiir_location` >=2. Matches record's site/location asymmetry and SpotOverride semantics.
- **Zero Hour (219_48):** front `anyOf` = (controlWith `Lothal_location` >=3 Rebel) OR (occupyWith `Lothal_location` >=3 `Phoenix_Squadron_character`), each with opponent control ==0, `includeExcludedFromBattle: false` (matches record's "front blocker is opponent CONTROL with NO SpotOverride"). Back = opponent control `Lothal_location` strictly `>` self, `includeExcludedFromBattle: true`, tie-safe. Matches record's strict count-comparison flip-back.

### 7. TEST HONESTY — PASS
RegionalCountedObjectiveBehaviorTest runs all three families with positive and negative assertions: `frontRuleRequiresThreeActorQualifiedControlledSitesAndNoOpponentControl` (assertFalse on 2/3, on opponent control), `backRuleRequiresOpponentControlOfTwoRegionalLocations`, `actorAdvanceIsExactToActorTypeSiteAndUnmetThreshold` (Chopper-at-empty-site rejected; occupyWith needs an existing occupier), `formationProtectionPreservesPartialAndExactProgressButReleasesRedundancy` (asserts hardVeto and redundancy release), `battleBonusTargetsOnlyTheSafeMissingQualifiedSite`. RegionalObjectiveEngineContractTest / ZeroHourObjectiveEngineContractTest / ObjectiveAnalyzerPostFlipLocationRiskTest exercise real card triggers.

### 8. KNOWN DEFECT INTERACTION (V201 DEFER) — PASS
`DeployPlanPolicy` DEFER untouched. Both planners "invalidate a same-turn cached plan when structured progress changes, while stable progress retains the cached plan" (changelog), same replan mechanism as the Invasion commit. DEFER continues to protect plan integrity for non-objective cases.

---

## Follow-ups for the lead
1. Add a `RULES:` trailer to the commit message.
2. **Ralltiir place-out-of-play:** truth record 7_300 notes both sides carry `isBlownAwayLastStep(SYSTEM + Ralltiir)` → objective placed out of play. The generic `HARD_LOSS_LOCATION` defense logic exists (from Invasion commit), but this gate did **not** confirm the Ralltiir profile wires the Ralltiir system as a hard-loss location. Recommend a targeted confirmation. Not a flip-law error; the front/back flip transcription is correct.
3. Jar hashes and reactor counts (2156/0/0/26) unverified in static gate.
