# Rando Facts and Assessments Contract

Date: 2026-07-13
Owner: Codex/Alfred
Implementer: K-2/Claude
Status: required before Batch 2 production wiring

## Boundary

Build one immutable, ordered snapshot per decision. The snapshot records what the engine offered
and what can be observed or measured from game state. It does not decide whether an action is good.

The data flow is:

`raw decision -> route -> immutable snapshot -> domain assessments -> typed contributions -> merge`

No producer may call a later stage. In particular, facts cannot contain scores, ranks, vetoes, or
policy verdicts.

## Current-state evidence

- Rando and ChosenOne `DecisionContext` are structural mirrors that differ only by package imports,
  so the new snapshot belongs in shared `models/common`, not in two copied packages.
- `DecisionContext` currently builds mutable candidate lists in stages and exposes an untyped
  `Map<String,Object> extra`. New code must not add another key-value escape hatch.
- `ForceReserveService.Facts` proves one immutable per-decision computation is practical, but its
  empty sentinel maps missing input to false and zero. The general facts model must preserve unknown.
- `ObjectiveAnalyzer.getDeployObjectiveAdjustments()` currently mixes objective detection/data with
  deploy scoring. The target split keeps objective facts and plan data in the analyzer, while a
  deploy objective adapter owns contributions.
- `CombinedEvaluator` now merges through `LinkedHashMap` and retains the first-seen candidate on an
  exact raw-float tie. This was an explicit intentional delta, not baseline behavior. Commit
  `5240f36c6` adds the focused normal, duplicate-id, deploy-bucket, and all-veto fixtures in both
  bots; the aggregate deployment gate remains separate.

## Minimal shared model

Use final immutable classes or Java 21 records under a shared decision package. Keep the first batch
small:

### `FactValue<T>`

- State: `KNOWN` or `UNKNOWN`.
- A known value, including known Boolean `false` or numeric zero.
- Producer id.
- Source evidence or stable provenance key.
- Unknown reason when state is `UNKNOWN`.

`Optional<T>` may be an implementation detail inside this type. It is not the public fact contract
because it cannot explain who produced the value or why it is absent.

### `DecisionFacts`

- Decision id/type/text, phase/window, turn, current player, side, and obligation flags.
- `noPass`, minimum, maximum, and blocked responses.
- Force, life-force, hand, pile, objective identity, and objective flip observations when known.
- The selected route and the evidence used to select it.

### `ActionFacts`

- Original ordinal, action id, action text, card id, blueprint id, testing text, and selectable flag.
- Typed action/card/source/destination references when resolution succeeds.
- Rule-independent measurements such as cost, power, ability, presence counts, icons, and
  weapon-adjusted power components.
- Unknown resolution results with provenance. Candidate order is never sorted or rebuilt.

### `DecisionSnapshot`

- One `DecisionFacts` instance.
- One ordered `ActionFacts` entry per original candidate ordinal.
- Immutable shared service facts that are truly observational, such as force obligations.
- A snapshot version used by fixture traces.

The snapshot does not carry mutable game objects as its public contract. A compatibility layer may
retain engine references while an arm is unmigrated, but frozen fixtures serialize stable ids and
values rather than object identity.

## Domain assessments

These are separate immutable outputs with one producer each:

- Formation safety and its exact law/reason.
- Battle feasibility and expected battle measurements.
- Force budget and maintenance obligations.
- Objective plan requirement or preference for this action.
- Pull/search viability and parsed target identity.

Do not add scalar `threatLevel`, `survivabilityEstimate`, `economicImpact`, or
`objectiveAlignment` fields to `ActionFacts`. Those names hide policy thresholds and recreate the
same cross-talk being removed. Store their observable components in facts, then let the owning
domain produce a typed assessment.

An assessment may be cached once per snapshot and action ordinal. It cannot mutate facts or invoke
an evaluator.

### Cross-prompt deploy formation plan

Parent `CARD_ACTION_CHOICE` and child deploy-destination prompts are one mediated route. They cannot
produce independent answers to whether a character will finish in a safe formation.

The formation producer returns one typed `DeployRouteAssessment` per parent deploy action:

- `SAFE_SOLO`: at least one ordered destination is safe without a follow-up.
- `SAFE_SEQUENCE`: identifies the first card, buddy card, ordered destination, both costs, reserved
  force obligations, and the legal same-phase sequence.
- `ALL_DESTINATIONS_BLOCKED`: every solo destination and complete sequence is blocked, with the
  ordered destination assessments and exact laws retained.
- `UNKNOWN`: required resolution failed, with provenance and the registry-declared unknown policy.

A `SAFE_SEQUENCE` is not inferred from "buddy in hand" alone. The producer must prove that the same
destination remains legal for both cards, total cost plus obligations fits available force, and the
follow-up remains available in the current phase. The decision mediator carries the selected plan
from parent to child and clears it on completion, cancel, or phase transition. Evaluator-local
mutable caches do not own this lifecycle.

The child formation guard consumes the carried assessment. It must not veto the planned first card
solely because its buddy is still in hand. Conversely, `ALL_DESTINATIONS_BLOCKED` constrains the
parent action so an optional child prompt cannot be opened and canceled repeatedly.

## Unknown handling

Unknown is data, not an automatic decision. Each rule arm declares its unknown policy in the domain
registry:

- `DEFER`: emit no contribution.
- `CONSERVATIVE_BLOCK`: safety law refuses the action until the required fact is known.
- `CONSERVATIVE_ALLOW`: continue when blocking would be more dangerous.
- `TEST_ERROR`: a fixture required the value, so unknown is a harness failure.

There is no project-wide fail-open default. Existing V61c battle-plausibility logic intentionally
fails conservatively when game state is missing or throws; replacing that with false would reopen a
known no-destiny defect.

## Construction and dependencies

1. Parse the complete raw decision and preserve every candidate array in original order.
2. Select exactly one primary route from decision type, phase/window, obligation flags, and typed
   candidate shape. Scores and assessments cannot influence route selection.
3. Build the base snapshot once, after the legacy context has received all candidate arrays.
4. Invoke only the domain producers required by that route, in a declared acyclic order.
5. Cache each producer result for the duration of the decision.
6. Emit constraints, ranks, and bounded scores only from the owning phase/domain adapter.
7. Freeze the snapshot, assessments, and ordered contribution trace for shadow comparison.

Eagerly compute the small base snapshot. Compute domain assessments route-by-route and cache them;
do not eagerly run every strategy service for every candidate.

### SETUP construction boundary

Build the SETUP shadow snapshot from the complete raw decision before strategy-service mutation.
Preserve temporary ids, blueprints, testing text, selectable flags, preselected flags, and original
ordinals even where the legacy context drops an array. A typed `SetupRoute` must distinguish
terminal decisions and terminal candidates so early V21 returns and later V22/V70 suppression do
not change accidentally. Direct saga responses, fallback routing, and their skipped finalizer and
tracker effects remain explicit legacy routes until separately cut over.

### DRAW construction boundary

Build the DRAW shadow snapshot after the legacy context has parsed its inputs but before
`CombinedEvaluator.canHandle`. The builder may read already-parsed fields only. It cannot call
evaluators, strategy services, or `getForceReserveFacts()`, because those calls can mutate caches or
recompute on soak boundaries. The first typed producer is a pure `DrawReserveAssessment` that
preserves the current arithmetic order, including cap-before-V67z, ownership-only contested
detection, all-character corridor counting, and the legacy error fallback.

## Objective split

`ObjectiveAnalyzer` remains the compatibility source for objective identity, flip state, parsed
requirements, active playbook data, and named objective facts. It stops being the final owner of
deploy scores as arms migrate.

The phase adapter owns each contribution:

- Objective deploy facts -> Deploy objective adapter.
- Objective movement facts -> Move objective adapter.
- Objective battle facts -> Battle objective adapter.
- Pull requirements -> Pull engine adapter.

One objective fact may feed several phase adapters. The same semantic contribution may emit from
only one adapter on one route.

## Incremental landing order

1. Add shared immutable value/snapshot types and pure construction tests. No production consumer.
2. Build snapshots in shadow mode beside both legacy contexts and compare Rando/ChosenOne output.
3. Add deterministic trace serialization and fixture capture.
4. Migrate one observational service, with no score movement.
5. Add one typed domain assessment and compare it against the legacy predicate.
6. Move one contribution owner only after its named fixtures pass.
7. Remove the corresponding legacy arm only after shadow traces prove it no longer contributes.

## Gate

Batch 2 foundation advances only when:

- Rando and ChosenOne receive equal snapshots for the same frozen input.
- Candidate arrays and ordinals are unchanged.
- Unknown values preserve producer, provenance, and reason.
- No snapshot or fact class exposes mutators or an untyped extension map.
- No score, veto, rank, winner, response, or V191 trace changes before an approved owner move.
- Focused fixtures, mirrored-source checks, and the complete affected-module build pass.
