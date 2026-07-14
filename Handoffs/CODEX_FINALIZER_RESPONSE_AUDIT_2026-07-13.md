# Codex finalizer, PASS, and RESPONSE route audit

Date: 2026-07-13
Owner: Codex audits and gates; K-2 implements production Java
Baseline: clean commit `21dda1a67367da689e6f610f7111b6dbdfee0c2e`
Scope: Rando and chosenone response finalization, pass legality, direct response routes,
mediator rejection handling, tracker mutation, and parity. K-2's later Trace 2b worktree
changes were excluded from this audit.

## Verdict

| Boundary | Verdict | Reason |
|---|---|---|
| Typed response facts and a pure finalizer in shadow mode | ADVANCE | This is the smallest seam that can expose current contradictions without changing play. |
| Shared response contract fixtures against real engine decisions | ADVANCE | The engine decision classes, not prompt text, define valid wire responses. |
| Mediator bounded retry with visible terminal failure | ADVANCE after fixtures | Invalid AI output currently requeues a decision but does not restart AI scheduling. |
| Direct interceptor migration into the shared finalizer | HOLD | Current V45, V44/V67j, V170, V61, and Rando-only V79b returns bypass safety and tracking. |
| Retire either `DecisionSafety` copy | HOLD | Both bots still depend on duplicated emergency and correction behavior. |
| Retire outer or base finalization/tracker code | HOLD | The same fallback response is finalized and tracked more than once. |
| Phase-owner cutovers | HOLD | Final response legality must have one owner before phase routes can safely replace legacy owners. |

## P0 findings

### 1. A rejected AI answer can strand the game

Current mediator order:

1. Obtain the pending decision.
2. Ask the AI for an answer.
3. Remove the pending decision with `participantDecided`.
4. Call the engine's `decisionMade(answer)` validator.
5. On `DecisionResultInvalidException`, reinsert the decision only.

The valid path alone calls `carryOutPendingActionsUntilDecisionNeeded()` and
`startClocksForUsersPendingDecision()`. The invalid path does not call either one and does
not invoke `maybeLetAiPlay` again. The global chain counter also returns silently after 50.
`MultipleChoiceAwaitingDecision` parses an integer but does not bounds-check it before indexing
the result array, so an out-of-range AI ordinal throws an unchecked exception outside the
mediator's `DecisionResultInvalidException` catch.

Evidence:

| Source | Evidence |
|---|---|
| `SwccgGameMediator.java:1278-1329` | `participantDecided` precedes engine validation; catch only calls `sendAwaitingDecision`. |
| `DefaultUserFeedback.java:22-24` | `participantDecided` removes the awaiting decision and marks it finished. |
| `SwccgGameMediator.java:1285-1287` | `MAX_AI_CHAIN` overflow returns without a visible failure or recovery action. |
| `MultipleChoiceAwaitingDecision.java:59-70` | Parsed ordinals index `_possibleResults` without a range guard. |

Required behavior: one deterministic immediate retry keyed by player plus decision id,
then a visible terminal failure that preserves the pending decision for diagnosis. No silent
return and no unbounded recursion.

### 2. Pass legality has incompatible owners

| Owner | Current rule | Conflict |
|---|---|---|
| `DecisionContext` | Missing params default to `noPass=true`, `min=0`, `max=1`. | Fabricated defaults can be mistaken for engine facts. |
| `PassEvaluator` | Pass only when `!noPass && min==0`, with an explicit cancel lookup for required prompts. | Treats policy `noPass` as universal empty-wire legality. |
| `CombinedEvaluator` | Synthesizes empty passes for DPS, all-veto, and V148 paths using separate prompt-text rules. | Creates pass responses outside `PassEvaluator`. |
| Rando/chosenone outer bots | Empty plus raw `noPass=true` triggers emergency selection. | Can overwrite a legal empty response for some engine decision types. |
| `DecisionSafety.mustChoose` | Prompt Done/Cancel text can override raw `noPass`; otherwise raw `noPass` returns before cardinality checks. | Text heuristics and parameter precedence differ from evaluator rules. |
| Engine decision classes | Legality depends on concrete decision type and fields. | Engine truth does not map to one `noPass` rule. |

Engine truth verified from source:

| Decision | Empty wire response | Source truth |
|---|---|---|
| `ACTION_CHOICE` | Rejected | `ActionSelectionDecision.java:130-142` throws when result is empty. |
| `CARD_ACTION_CHOICE` | Accepted as no selected action | `CardActionSelectionDecision.java:167-179` returns `null` for empty, regardless of `noPass`. |
| `CARD_SELECTION` | Accepted only when cardinality permits zero | `CardsSelectionDecision.java:64-71` validates `minimum` and `maximum`. |
| `ARBITRARY_CARDS` | Cardinality can be bypassed when `returnAnyChange=true` | `ArbitraryCardsSelectionDecision.java:248` and `:281`. |
| `MULTIPLE_CHOICE` | Must parse to an in-range result index | `MultipleChoiceAwaitingDecision.java:60-70`; current source lacks the range guard and can throw unchecked. |
| `INTEGER` | Must parse and satisfy engine bounds | `IntegerAwaitingDecision.java:35-51`. |

The final contract must therefore separate:

| Fact | Meaning |
|---|---|
| `policyPassAllowed` | Whether strategy may intentionally decline. |
| `emptyWireAccepted` | Whether the concrete engine decision accepts `""`. |
| `minimum` / `maximum` | Selection cardinality enforced by the engine. |
| `preselected` | Existing selected state for iterative arbitrary-card decisions. |
| `returnAnyChange` | Whether one valid change may return before normal cardinality. |
| `defaultIndex` and bounds | Concrete multiple-choice and integer defaults, not guessed index positions. |

### 3. Direct routes bypass the common final boundary

Rando has five direct interceptor families. Chosenone has four because V79b exists only in
Rando. They return before emergency handling, `DecisionSafety`, tracker recording, and
strategic-event recording.

| Route | Risk |
|---|---|
| V45 optional forfeit | It has no decision-type guard and returns empty from prompt text alone. |
| V44/V67j revert | It defaults to index 0 before proving a positive option exists. |
| V170 undercover | It initializes Yes=0 and No=1 before validating result shape or labels. |
| V61 saga | It chooses a raw result ordinal and returns before shared validation/tracking. |
| V79b parsec, Rando only | It returns from several branches and has no chosenone parity route. |

Clean-baseline anchors: Rando `RandoCalAi.java:614-934`; chosenone
`TheChosenOneAi.java:613-789`. The common Rando finalizer begins at
`RandoCalAi.java:962`, records the outer tracker at `:1009`, and records strategic events at
`:1019`.

### 4. Several safety rules are additive pseudo-vetoes

The engine selects the highest total score. A large negative number is not an absolute veto
unless it sets the `EvaluatedAction` hard-veto flag or the finalizer rejects the intent.

| Rule family | Current form | Required final form |
|---|---|---|
| V163/V169 loop handling | `-100000` or `-250` score mutation plus retry counters | Typed rejection history plus explicit ALLOW/DEFER/BLOCK assessment. |
| MOVE ladder | `-100000` score mutation for several named hard-veto branches | Typed veto carried to final selection. |
| V38.3 harmful self-target | `-9999` score mutation | Unoutvotable finalizer constraint. |
| V67af self-bounce | `-9999` score mutation | Unoutvotable finalizer constraint. |

Formation Safety is the useful model: its hard-veto flag is filtered before score ranking,
with an explicit all-veto forced-choice fallback.

## P1 findings

| Finding | Evidence and impact |
|---|---|
| Fallback finalizes twice | `HeuristicAiBase.java:107-133` applies emergency, safety, loop mutation, and tracker recording. The outer bot repeats emergency, safety, and tracker work after `super.decide`. |
| Chosenone uses Rando safety in the base | `HeuristicAiBase` imports Rando `DecisionSafety`, so chosenone fallback runs through Rando safety before chosenone outer safety runs. |
| Tracker mutation has multiple choke points | Base fallback records once, outer bot records again, and direct interceptors record neither. |
| Randomness has multiple owners | `DecisionSafety` owns an unseeded static `Random`; fallback and evaluator routes also have independent random choices. Exact replay is not guaranteed. |
| INTEGER emergency can be illegal | `DecisionSafety.java:294-298` always returns `0` without reading engine minimum, maximum, or default. |
| Copy parity is not ownership | Mirrored Rando/chosenone classes can remain text-identical while both preserve the same contradictory contract. |
| Typed snapshots are incomplete for finalization | They must carry preselected state, `returnAnyChange`, concrete defaults, bounds, and multiple-choice result shape. |

## Frozen legacy precedence

Until the shared finalizer is proven, preserve this exact order as observable behavior:

1. Mediator schedules AI.
2. Outer bot checks loop state.
3. Direct interceptors may return immediately.
4. Chaos or `CombinedEvaluator` runs.
5. `HeuristicAiBase` fallback may finalize and mutate tracking.
6. Outer multi-select handling runs.
7. Raw-`noPass` emergency may replace the result.
8. Outer `DecisionSafety` may correct the result.
9. Outer tracker and strategic-event mutation run.
10. Mediator submits to the engine.
11. Valid answers recurse into scheduling; invalid answers are silently requeued.

This ordering is not endorsed. It is the compatibility oracle for shadow comparison.

## Smallest consolidation seam

Introduce one policy-free boundary:

```text
finalize(
    DecisionSnapshot snapshot,
    ResponseContract contract,
    ResponseIntent intent,
    RandomGenerator random,
    RejectionHistory history
) -> FinalizedResponse
```

`ResponseIntent` has only these variants:

| Variant | Payload |
|---|---|
| `Pass` | No payload. |
| `CandidateOrdinal` | One typed candidate ordinal. |
| `CardOrdinals` | Ordered typed card ordinals. |
| `IntegerValue` | One integer value. |
| `Acknowledge` | Explicit empty/acknowledgement intent for engine types that use it. |

`FinalizedResponse` must contain the chosen intent, exact wire response, corrections, veto
or forced-choice reason, deterministic random draw metadata, and exactly one tracker
mutation request. The finalizer is pure. The caller applies the mutation only after the
engine accepts the response.

## Fixture corpus

Engine-contract fixtures:

| Fixture | Required assertion |
|---|---|
| `FC_ACTION_CHOICE_EMPTY_REJECTED` | Empty is rejected. |
| `FC_CARD_ACTION_NOPASS_EMPTY_ACCEPTED` | Empty maps to no selected action even with raw `noPass=true`. |
| `FC_CARD_SELECTION_MIN0_EMPTY` | Zero-card response is accepted. |
| `FC_CARD_SELECTION_MIN2_EXACT` | Exactly the required card count is returned. |
| `FC_ARBITRARY_RETURN_ANY_CHANGE` | Preselected state plus one valid change is accepted. |
| `FC_MULTIPLE_CHOICE_BOUNDS` | Result index is label-derived and in range. |
| `FC_INTEGER_BOUNDS` | Value and default satisfy engine bounds. |

Direct-route and parity fixtures:

| Fixture | Required assertion |
|---|---|
| `RR_V45_OPTIONAL_FORFEIT` | Empty only when the concrete contract accepts it. |
| `RR_V44_REVERT_REORDERED` | Positive option found by label after permutation. |
| `RR_V170_YES_NO_REORDERED` | Yes/No labels determine ordinal; missing labels cannot default silently. |
| `RR_V61_SAGA_BY_DECK` | Saga intent survives result permutation. |
| `RR_V79B_RANDO_ONLY_WAIVER` | Explicit documented parity waiver until chosenone ownership is decided. |
| `RR_HEURISTIC_SINGLE_FINALIZER` | Base fallback and outer bot produce one finalization and one tracker mutation. |
| `PX_RANDO_CHOSEN_SHARED_CORPUS` | Both bots pass the same contract corpus. |

Safety, pass, and rejection fixtures:

| Fixture | Required assertion |
|---|---|
| `B0_MergedAction_VetoOR` | Any hard veto survives merged score contributors. |
| `B0_170_HardVeto_Epilogue` | Late positive scores cannot revive a vetoed candidate. |
| `B0_AllVeto_OptionalDone` | Optional all-veto decision returns legal Done/pass. |
| `B0_AllVeto_ForcedLeastBad` | Forced all-veto decision returns deterministic least-bad legal intent. |
| `SV_V163_UNOUTVOTABLE` | Loop block cannot be outscored. |
| `SV_V383_SELF_TARGET_UNOUTVOTABLE` | Harmful own-card target cannot be outscored. |
| `SV_V53_OWN_GRAB_UNOUTVOTABLE` | Own grab protection cannot be outscored. |
| `SV_V67AF_SELF_BOUNCE_UNOUTVOTABLE` | Wasteful self-bounce cannot be outscored. |
| `PC_DPS_FORCED_NO_SYNTHETIC_PASS` | A forced DPS step cannot invent an illegal empty response. |
| `PC_V148_OPTIONAL_EMPTY` | V148 cancellation is legal for the concrete child type. |
| `SF_SELECTABLE_PRESELECTED_CLAMP` | Clamp preserves preselected cards and adds only selectable cards. |
| `SF_SEEDED_FORCED_CHOICE` | Forced fallback is repeatable with fixed RNG. |
| `ST_V169_RETRY_1_2_3_4` | Typed rejection history reproduces the retry boundary. |
| `SM_INVALID_ONCE_BOUNDED_RETRY` | One invalid answer triggers one deterministic retry. |
| `SM_AI_CHAIN_LIMIT_VISIBLE` | Chain exhaustion produces a visible terminal state. |

Each fixture runs twice from clean state with fixed RNG. It invokes the real engine
`decisionMade` implementation and compares route, wire response, exact score bits, veto,
corrections, and tracker mutation count.

## Replay and log grounding

| Evidence | What it proves | What it does not prove |
|---|---|---|
| `replays/asdf/f27ws5lgy0g58k5p.xml.gz` and `logs/gemp-swccg.log:29953-30302` | V148 can cancel an all-negative deploy child, but the parent repeats three times before `DecisionTracker` blocks it. The final response boundary and rejection history are coupled. | It does not prove a single-cancel parent block or shared finalizer. |
| `replays/asdf/2jg1sj0l3qrlgy6a.xml.gz` and `logs/gemp-swccg.log:16497-16511` | The classic AMN path reached an `ARBITRARY_CARDS` decision where the old selection path chose `temp33` from a single-selectable-card state. | There is still no post-fix runtime `SAFETY CLAMP` firing proof. |
| `Handoffs/CODEX_V148_DEPLOY_CANCEL_LOOP_2026-07-11.md` | Exact parent, child, three-Done, and eventual blocker sequence. | This remains Rando-only behavior until shared fixtures prove parity. |
| `Handoffs/CODEX_VERIFY_c20e09e10_AMN_CLAMP_2026-07-11.md` | Compile, class, and Rando/chosenone clamp parity for the AMN safety change. | Static parity cannot prove runtime finalizer ownership. |

## Execution gate

The shared finalizer and mediator retry are prerequisites for interceptor migration and all
phase-owner cutovers. K-2 may implement in this order:

1. Add engine-contract fixtures against real decision classes.
2. Extend typed snapshots with missing response-contract fields.
3. Add the pure finalizer in shadow mode with fixed RNG injection.
4. Compare old wire response and shadow wire response without changing behavior.
5. Add bounded mediator retry and visible terminal failure fixtures.
6. Migrate one direct interceptor at a time through the finalizer.
7. Prove one accepted response produces exactly one tracker mutation.
8. Retire duplicated outer/base safety only after clean parity and replay gates.

No deployment is authorized by this audit.
