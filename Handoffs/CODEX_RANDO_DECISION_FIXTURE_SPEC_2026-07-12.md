# Rando Decision Fixture Contract

Date: 2026-07-12
Owner: Codex/Alfred (independent gate)
Implementer: K-2/Claude
Status: required before phase-reorganization Batch 2

## Purpose

The reorganization may change ownership and structure, but it may not silently change a
decision. A fixture records the inputs, route, evaluator contributions, merged score, veto
state, and final response for one decision. Reorganization batches pass only when the new
run equals the frozen fixture or carries an explicitly reviewed intentional delta.

Replay and game-log extracts are evidence used to build fixtures. They are not the score
oracle because they do not expose every contribution or every rejected candidate.

## Fixture identity

Each fixture must record:

- `fixtureId`: stable descriptive id, including the V-tag or defect when practical.
- `sourceEvidence`: replay id/path, log path and line range, or source-only boundary case.
- `baselineCommit`: exact pre-batch commit.
- `bot`: `rando`, `chosenone`, or `both` when byte-for-byte behavior is required.
- `decisionWindow`: setup, start, activate, control, deploy, battle, move, draw, end, or response.
- `intent`: one sentence describing the behavior protected by the fixture.

## Frozen input

The harness must preserve candidate order. It must not sort or normalize away order before
comparison.

- Decision type, decision text, decision id, phase, current player, turn number, side.
- `noPass`, minimum, maximum, and any other decision obligation flags.
- Action ids and action texts in their original ordinal order.
- Card ids, blueprint ids, selectable ids, and testing texts in original ordinal order.
- The minimum game-state facts required by the exercised code.
- Every derived fact used by a rule, with its producer and provenance.

Facts use `TRUE`, `FALSE`, or `UNKNOWN`. Missing data is not silently converted to false.

## Frozen trace

The trace is append-only for one decision and records:

- Selected route: interceptor, evaluator, fallback, or emergency.
- Applicable evaluators in invocation order.
- Every pre-merge contribution in invocation order:
  - action ordinal and action id;
  - evaluator, rule id/V-tag, domain id, and rule kind;
  - exact float delta as raw IEEE-754 bits plus a human-readable decimal;
  - veto flag and reason.
- Merged action score as raw float bits, veto state, bucket/rank, and pass eligibility.
- First-seen ordinal, winner ordinal, and action id.
- Final response, including any safety correction after the first winner selection.

The raw float representation is authoritative. Formatted decimal strings are diagnostic.

## Comparator

The gate compares, in this order:

1. Candidate arrays and ordinals exactly.
2. Route and evaluator invocation order exactly.
3. Ordered contribution sequence exactly, including raw float bits.
4. Merged score bits and veto set exactly.
5. Bucket/rank and pass eligibility exactly.
6. Winner ordinal, action id, and final response exactly.

An `intentionalDelta` waiver is the only exception. It must name the owner, reason, old and
new expectation, V-tag/domain, boundary math, and Steve approval when strategy changes.
Formatting, reordered candidates, or an equal winner with changed scores do not qualify as
score-neutral.

## Harness layers

Use two layers:

- Pure JUnit fixtures with scripted evaluators for deterministic merge/router/boundary behavior.
- `VirtualTableScenario` integration tests only where engine state construction or card
  effects cannot be represented faithfully by the pure harness.

Proposed homes:

- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/rando/evaluators/CombinedEvaluatorFixtureTests.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/framework/AiDecisionFixtureHarness.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/rando/RandoDecisionScenarioTests.java`

Do not add JSON, a replay parser, a database, or server/log dependencies to the pure layer.
Production code must not read test fixtures. Instrumentation may expose package-private or
test-only trace data, but may not change routing or scores.

## Deterministic tie contract

The current `CombinedEvaluator` accumulates actions in a `HashMap` and final selection compares
only score. Exact ties therefore inherit unspecified map iteration rather than candidate order.
Baseline exact-tie winners must be marked `UNSTABLE`, not frozen as behavior.

Before router shadow comparison, both bots must deliberately stabilize ties:

- Preserve first-seen candidate order during merge.
- Compare with `Float.compare(candidate, best) > 0`.
- On equal raw scores, retain the lower first-seen ordinal.

This is an explicit tie-only `intentionalDelta`, not a score-neutral refactor. Once it lands,
all fixtures require deterministic first-seen tie behavior.

## Required boundary fixtures

These are required before the corresponding owner moves:

- Deploy-phase-selection bucket: score `-100` remains viable; below `-100` falls through.
- Non-bucket deploy selection: score `+50` is selected; below `+50` passes.
- General cancellable decision: exactly `-100` is selected; below `-100` passes.
- `where to deploy`: exactly `0` is selected; below `0` passes.
- All actions vetoed and cancellable returns pass.
- All actions vetoed and mandatory returns the least-bad candidate without clearing veto facts.
- Exact-score ties select the first-seen candidate after the intentional tie-stability cutover.
- A hard veto remains a veto even if another rule adds `+20000`.
- V25 battle initiation preserves its existing summed contributions, not merely its winner.
- Every legacy `+9999`, `-9999`, or doubled variant is classified as a veto, ordering rule,
  or bounded score before it moves.
- V192 clamp boundaries retain their exact behavior.
- Krennic pull parsing removes the location suffix centrally and recognizes every typed
  Krennic persona, including `Card207_020` and `Card209_036`.
- Chiraneau split prevention applies the empty-destination penalty and does not apply it
  when the mover joins a friendly character.
- V47 Reserve Solo Block is absent, while V47 Lando stay and Lando pull behavior remain.

## Named regression corpus

These fixture ids are mandatory. Replay and source references identify the evidence used to
construct the frozen input; the pure harness remains the score oracle.

| Fixture id | Protected result | Primary evidence |
|---|---|---|
| `B0_L3_Greedo_PairBudgetVeto` | Force 3 cannot fund Greedo plus the cost-4 buddy. The deploy is vetoed and `Done` wins despite raw score `1420`. | `CODEX_SOLO_ABILITY_ROOT_CAUSE_AUDIT_2026-07-11.md`; July 12 log lines 38063, 38265, 38327-38362 |
| `B0_L3_PairFormable_Allowed` | A weak solo deploy with a payable buddy remains legal and receives no formation penalty. | `FormationSafety.java` pair-budget branch; `CODEX_VERIFY_107927F8E_2026-07-12.md` |
| `B0_L3_ParentDeploy_AllDestinationsVeto_NoReopen` | When every destination and complete buddy sequence is vetoed, the parent source action is constrained and cannot reopen the same optional child prompt. | `CODEX_PARENT_CHILD_DEPLOY_PLAN_AUDIT_2026-07-13.md`; July 12 log lines 62933-64243 |
| `B0_L3_ParentDeploy_ValidBuddySequence_Allowed` | A verified affordable same-destination buddy sequence permits its first deploy and carries the explicit follow-up plan into the child prompt. | `CODEX_PARENT_CHILD_DEPLOY_PLAN_AUDIT_2026-07-13.md`; July 12 log lines 62869-62910 |
| `B0_L3_NoBuddy_Raw350_Soft800` | No buddy and raw `350` receives `-800`, finishes at `-450`, and loses to Pass. | `FormationSafety.java`; `CardSelectionEvaluator.java` |
| `B0_L2_FirstLight_NoDestiny` | Power `5:6`, ability `3:3`, and no battle destiny is a hard veto even when battle score is `130`. | `CODEX_SOLO_ABILITY_ROOT_CAUSE_AUDIT_2026-07-11.md`; July 12 log lines 38947-38970 |
| `B0_L1_ContestedOrigin_WeakRemainder` | Moving away from a contested origin and leaving an unsafe remainder is vetoed. | `FormationSafety.java`; `CODEX_CHIRANEAU_EMPTY_SITE_SPLIT_AUDIT_2026-07-12.md` |
| `B0_L4_Tarkin_IntoArmedRey` | A destination with weapon-adjusted deficit at least 6 is vetoed even with raw score `777.5`. | `CODEX_SOLO_ABILITY_ROOT_CAUSE_AUDIT_2026-07-11.md` |
| `B0_L4_Hondo_IntoSaberPair` | Moving Hondo into the armed Anakin/Yoda threat is vetoed even with raw score `1045`. | `CODEX_SOLO_ABILITY_ROOT_CAUSE_AUDIT_2026-07-11.md` |
| `B0_Dominance_Tyranus8_Leia3` | Effective power 8 versus 3 satisfies the two-times dominance exception and retains V172 `+600`. | `CODEX_VERIFY_107927F8E_2026-07-12.md` |
| `B0_FlipGate_V193_Bunker` | The exact unflipped objective gate is formation-exempt and retains the existing Bunker priority. | `CardSelectionEvaluator.java`; `CODEX_VERIFY_107927F8E_2026-07-12.md` |
| `B0_Undercover_Jyn_LowerCorridor` | Jyn deploying undercover to Lower Corridor remains legal and keeps the observed `1805` route. | July 11 log lines 8176-8190 |
| `B0_V25_UpperWalkway_170` | Existing V25 summed contributions produce `170`, and battle beats Pass `-5`. | July 12 log lines 27450-27461 |
| `B0_170_HardVeto_Epilogue` | A vetoed non-bucket action scored `170` is excluded and Pass wins. | `CODEX_A250_FORMATION_SAFETY_REVIEW_2026-07-12.md`; `CombinedEvaluator.java` |
| `B0_AllVeto_OptionalDone` | When every candidate is vetoed and minimum is zero, synthetic Pass returns `Done` or `Cancel`. | `CODEX_A250_FORMATION_SAFETY_REVIEW_2026-07-12.md`; `CombinedEvaluator.java` |
| `B0_AllVeto_ForcedLeastBad` | When every candidate is vetoed and minimum is one, no Pass is invented; the least-bad legal candidate is returned. | `CombinedEvaluator.java`; `DecisionSafety.java` |
| `B0_MergedAction_VetoOR` | A veto from any evaluator survives merging with positive contributions from another evaluator. | `EvaluatedAction.java`; `CombinedEvaluator.java` |

### Batch 1 fixtures

| Fixture id | Protected result | Primary evidence |
|---|---|---|
| `B1a_KrennicHere_SelectorNormalize` | Side-aware source text parses `krennic here`, central normalization produces `krennic`, and typed persona resolution finds all Krennic printings. | `CODEX_VERIFY_D92BC3A3C_2026-07-12.md`; `Card216_016.java` |
| `B1c_Krennic_FirstPull_FlipExempt` | The unflipped first Krennic pull is formation-exempt and can win. | replay `ocffe8duo7yxh7fh.xml.gz`, events 3436-3442 |
| `B1c_Krennic_PostFlip_RePull` | The same unsafe pull after flip is not exempt, receives `-800`, and loses to Pass. | replay `ocffe8duo7yxh7fh.xml.gz`, events 3607-3624 |
| `B1c_FirstName_NoDeathStarOverlap` | Merely containing `Death Star` does not create a Krennic first-pull exemption. | `CODEX_VERIFY_D92BC3A3C_2026-07-12.md` |
| `B1b_Chiraneau_Ozzel_EmptySplit` | Empty destination with Ozzel left behind is a soft `-800`, not L1/L4 veto: `327.5` becomes `-472.5`, then `Done` wins. | replay `95s10zqy7sl0c177.xml.gz`, events 6501-6765 |
| `B1d_NoV47_Krennic_Praji_Snoke` | A forced Reserve child prompt no longer applies V47 Reserve Solo Block; the sole legal character remains selectable. | `CODEX_V47_WRONG_FACTS_AUDIT_2026-07-12.md`; July 12 log lines 74877-74905, 77721-77765, 78095-78149 |
| `B1d_DiningRoom_Lando_TypedRoute` | The source action carries Dining Room destination facts; the child selection does not guess a location. | `CODEX_V47_WRONG_FACTS_AUDIT_2026-07-12.md`; `Card226_001.java` |

### Required V82 red corpus

| Fixture id | Required result |
|---|---|
| `V82_SonOfSkywalker_AnakinsLightsaberPersona` | `WILL_SUCCEED`; no V67h veto. |
| `V82_Cell2187_SpyR2D2_RejectNonSpyArtoo` | `WILL_FAIL`; V67h veto applies. |
| `V82_CunningWarrior_CC_Corridor_RejectHoth` | `WILL_FAIL`; a Hoth corridor cannot satisfy a Cloud City corridor qualifier. |
| `V82_Apprentice_CoruscantNaboo_RejectHoth` | `WILL_FAIL`; a Hoth site cannot satisfy Coruscant or Naboo. |

Run every applicable route fixture against Rando and ChosenOne. Shared formation helpers need one
shared unit fixture plus both-bot route fixtures proving the helper is reached.

## Domain registry contract

Batch 0's single-owner registry must provide one row per rule arm:

- `domainId` and decision window.
- Single production owner.
- Rule kind: veto, ordering, or banded score.
- V-tag and arm label.
- Trigger and required typed facts.
- Output contract, including score band or rank.
- Cross-phase callers.
- Rando/chosenone parity pair, when applicable.
- Replacement status: active, delegated, superseded, or retired.
- Retirement gate and fixture ids proving removal is safe.

A multi-arm V-tag receives separate rows. A rule cannot be retired because a similarly named
rule exists elsewhere. The registry must identify the exact consumer and prove equivalent or
deliberately changed output.

## Batch gate

For every batch:

1. Freeze or add the relevant fixtures on the pre-batch commit.
2. Run the baseline and retain the machine-readable result.
3. Apply one owner/domain batch to both bots where parity is required.
4. Compile and run the focused fixture suite.
5. Run the full AI test suite and source/bytecode parity checks.
6. Codex issues `ADVANCE`, `HOLD`, or `ROLLBACK` with evidence.
7. Deploy only after `ADVANCE` and the normal idle-game/build/load/fire gates.

Failure to preserve a score, route, guard, mandatory-action rule, or hard veto is a `HOLD`.
Failure discovered after deployment is a `ROLLBACK` unless a smaller verified correction can
be made before another live game starts.
