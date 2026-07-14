# Trace Stage 4A2b: inherited shared tracker preflight

Date: 2026-07-13
Architecture/review owner: Codex/Alfred
Java implementation owner: K-2/Claude small agent
Implementation parent: `08e544f5050d6a85b2dadcb25e5cb73436ace2b6`
Status: implementation authorized by Codex `ADVANCE-INERT` gate (`m00434`)
Deployment: held
Push: held

## Scope

Observe only the private `DecisionTracker` owned by `HeuristicAiBase`. It is a third tracker
instance, distinct from both outer bot trackers. `HeuristicAiBase` imports the Rando tracker class,
so both Rando and ChosenOne use `com.gempukku.swccgo.ai.models.rando.DecisionTracker` for this
inherited owner.

Do not consolidate tracker instances in this increment. Do not instrument outer tracker calls a
second time. Every event from this slice must use `TrackerOwner.HEURISTIC_SHARED`.

## Exact reachable mutator sites

All line numbers refer to source at `08e544f50`.

| Site | Call | Required event family |
|---|---|---|
| `HeuristicAiBase.java:75` | `decisionTracker.onPhaseChange(phase.name())` | `PHASE_CHANGE` |
| `HeuristicAiBase.java:1027` | `decisionTracker.updateState(handSize, forcePile, reserveDeck, turn, cardsInPlay)` | `UPDATE_STATE` |
| `HeuristicAiBase.java:109` | `decisionTracker.blockLastActionOnCancel(decisionType, decisionText)` | `BLOCK_RESPONSE` |
| `HeuristicAiBase.java:132` | `decisionTracker.recordDecision(decisionType, decisionText, decisionId, trackingResponse)` | existing `RECORD_RESPONSE` semantic |

The same tracker also has read-only calls to `shouldForceDifferentChoice()` and
`getBlockedResponses(...)`; they are not state events. There is no reachable `clear()` or
`resetRepeatCount(...)` call on this inherited instance.

`DecisionTracker.recordDecision(...)` can internally invoke `blockLastActionOnCancel(...)` after
three cancel responses. That internal call is part of the one externally observed
`recordDecision(...)` mutation. Instrumenting it separately would create nested duplicate ownership
for one legacy public call and is out of scope.

## Exact source order

Within `HeuristicAiBase.decide(...)`:

1. Read decision parameters, type, text, and current phase.
2. If phase is non-null, call `onPhaseChange(...)` once.
3. Call `updateDecisionTrackerState(...)`. It either returns before the tracker call or invokes
   `updateState(...)` once after all game-state reads and reassignment pruning succeed.
4. Select the heuristic response.
5. For an empty CARD_SELECTION or ARBITRARY_CARDS result, call
   `blockLastActionOnCancel(...)` once.
6. Apply safety, failed-search, local-loop, recent-response, and reassignment bookkeeping.
7. Call `recordDecision(...)` once with the final `trackingResponse`.

The outer bot trace session is already active before either bot delegates to `super.decide(...)`.
Only fallback routes call `super.decide(...)`; primary evaluator routes do not mutate the inherited
tracker and must produce no `HEURISTIC_SHARED` event. On a fallback route, the expected shared event
order is `PHASE_CHANGE` when applicable, `UPDATE_STATE` when the helper reaches its tracker call,
optional `BLOCK_RESPONSE`, then `RECORD_RESPONSE`. Outer-owner events remain distinct before and
after this sequence.

## Payload ownership

`PHASE_CHANGE`:

- exact phase string passed to the call;
- before/after decision-affecting tracker snapshot;
- exact `lastPhase` before/after;
- outcome derived from snapshot equality, including `lastPhase`.

`UPDATE_STATE`:

- exact already-computed `handSize`, `forcePile`, `reserveDeck`, `turn`, and `cardsInPlay`;
- before/after `DecisionTrackerSnapshot`, `lastTurn`, and `lastStateHash`;
- no `lastPhase` claim;
- outcome derived from full lifecycle snapshot equality.

`BLOCK_RESPONSE`:

- exact call subject `decisionType` and `decisionText`;
- before/after `DecisionTrackerSnapshot`, which carries the last-action key/response and canonical
  turn-block rows;
- exact boolean returned by the legacy call;
- constructor validation that return value and snapshot delta do not contradict each other.

`RECORD_RESPONSE`:

- reuse the accepted `TrackerRecordResponseEvent` semantic with
  `TrackerOwner.HEURISTIC_SHARED`;
- exact decision type, numeric id string, exact decision key from the tracker seam, and final
  tracking response;
- before/after `DecisionTrackerSnapshot`;
- do not invent card, board, or candidate facts.

## Package-boundary constraint

`HeuristicAiBase` is in `com.gempukku.swccgo.ai.models`, while its tracker and current pure seams
are in `com.gempukku.swccgo.ai.models.rando`. The accepted 4A1 and proposed 4A2a seams are
package-local, so `HeuristicAiBase` cannot call them.

The smallest non-reflective solution is one public, read-only trace-access bridge in the Rando
tracker package. The bridge delegates to the existing package-local pure seams and any new phase
snapshot seam. It exposes no mutation and performs no reconstruction. Do not make tracker state
fields public, duplicate `decisionKey(...)`, use reflection, or move the tracker class in this
increment.

This bridge is Rando-package-only because the inherited owner is always the Rando tracker class.
The ChosenOne outer tracker remains separately mirrored and separately observed.

## One-call/one-event laws

- One event for each instrumented external boundary call while a trace session is active.
- No event when `phase == null` or when `updateDecisionTrackerState(...)` returns before its tracker
  call.
- No nested event for the internal cancel-block helper invoked by `recordDecision(...)`.
- Instrumentation failure never skips, repeats, or changes a legacy call.
- Event owner is always `HEURISTIC_SHARED`; outer events keep `OUTER_RANDO` or
  `OUTER_CHOSENONE`.
- Captured event order must match source order exactly.
- Production default keeps trace capture disabled.

## Highest overlap risk

Both outer bots call their own `updateState(...)` before the heuristic fallback calls the shared
tracker's `updateState(...)`. The arguments can be identical, but the tracker objects and owners are
not. A trace that omits owner identity, reconstructs one event from the other, or coalesces equal
payloads would hide real cross-talk. Tests must assert both events remain present and ordered under
their distinct owners.

The shared `recordDecision(...)` also occurs before the outer bot performs its final safety and
outer `recordDecision(...)`. The shared call records `trackingResponse`; the outer call records the
final `result`, so the values can differ. Both records are real current behavior. This slice
observes the duplication; it does not consolidate or retire either owner.

## Council result and source correction

The local council endorsed distinct `PHASE_CHANGE`, `UPDATE_STATE`, and `RECORD_RESPONSE` event
boundaries scoped to `HEURISTIC_SHARED`. Its suggestion to attach cards-in-play context to
`recordDecision(...)` is rejected because that is not a call argument or owner fact. Source review
also found the reachable `blockLastActionOnCancel(...)` boundary that the council omitted.

## Acceptance fixtures

- Real shared-tracker phase change: changed phase resets sequence/repeat state; repeated phase is
  `NO_OP`.
- Shared update: identical arguments are `NO_OP`; turn/hash change is `CHANGED`.
- Shared cancel block: successful block reports true and a changed canonical snapshot; ineligible
  or absent-last-action calls report false and `NO_OP`.
- Shared record response: exact decision key and tracking response, with
  `HEURISTIC_SHARED` accepted only after this slice intentionally expands the 4A1 owner invariant.
- Bot boundary: both outer and shared update events appear in exact owner order; heuristic and outer
  response-record events both appear in current source order on a fallback route.
- Primary evaluator route: no `HEURISTIC_SHARED` event appears because `super.decide(...)` was not
  called.
- Null phase and failed game-state read suppress only their corresponding unexecuted mutator event.
- Rando and ChosenOne produce normalized shared-owner parity because both inherit the same source
  and tracker implementation.

The 4A2a prerequisite is satisfied by `08e544f50` and Codex mailbox result `m00434`.

## Authorized implementation shape

Use the existing naming family and keep this as one isolated commit:

- Add `DecisionTrackerPhaseSnapshot`: complete `DecisionTrackerSnapshot` plus exact `lastPhase`.
- Add `TrackerPhaseChangeEvent`: owner fixed to `HEURISTIC_SHARED`, exact phase argument,
  before/after phase snapshots, and derived `MutationOutcome`. Its constructor rejects an after
  snapshot whose `lastPhase` differs from the exact phase argument.
- Add `TrackerBlockResponseEvent`: owner fixed to `HEURISTIC_SHARED`, exact decision type/text,
  exact legacy boolean return, before/after `DecisionTrackerSnapshot`, and constructor validation
  that `blocked == (outcome == CHANGED)`. The direct legacy call returns false before every
  mutation, so both `true + NO_OP` and `false + CHANGED` are impossible.
- Extend `TraceStateEvent` only for those two new records. Keep schema version 3.
- Extend `TraceSession` with `recordTrackerPhaseChange(...)` and
  `recordTrackerBlockResponse(...)`. Reuse the existing update and record-response methods.
- Intentionally expand `TrackerUpdateStateEvent` and `TrackerRecordResponseEvent` to accept
  `HEURISTIC_SHARED`. `TrackerClearEvent` must continue rejecting it because no shared clear call
  exists. Remove the superseded rejection rather than leaving a new commented-out code block; git
  history is the revert path.
- Add one public final, read-only `DecisionTrackerTraceAccess` bridge in the Rando tracker package.
  Its static methods delegate to package-local pure snapshot/key seams. Do not expose fields,
  mutation, reflection, or reconstructed decision keys.
- Instrument only the four exact `HeuristicAiBase` call boundaries listed above. Each legacy call
  runs once outside the instrumentation failure path.

Exact production write set:

- `ai/models/HeuristicAiBase.java`
- `ai/models/rando/DecisionTracker.java`
- new `ai/models/rando/DecisionTrackerTraceAccess.java`
- new `ai/models/common/trace/state/DecisionTrackerPhaseSnapshot.java`
- new `ai/models/common/trace/state/TrackerPhaseChangeEvent.java`
- new `ai/models/common/trace/state/TrackerBlockResponseEvent.java`
- existing `TraceStateEvent.java`, `TrackerUpdateStateEvent.java`,
  `TrackerRecordResponseEvent.java`, and `TraceSession.java`
- focused common-state, bridge, shared-owner, and mirrored bot-boundary tests
- required changelog/history bookkeeping

No outer bot hook, ChosenOne `DecisionTracker`, evaluator, engine, objective, deploy, score,
finalizer, or capture-default change belongs in this commit.

## Gate additions

- Source count remains exactly one direct `HeuristicAiBase` call site for each of `onPhaseChange`,
  `updateState`, `blockLastActionOnCancel`, and `recordDecision`. The internal
  `recordDecision(...)` call to `blockLastActionOnCancel(...)` remains folded into its single
  `RECORD_RESPONSE` event and receives no nested hook.
- Parent/current bytecode for all four legacy shared tracker mutators is identical after normalized
  `javap` comparison.
- A same-package test uses `TraceSession.openForTesting(...)` with a prepared `TraceCollector`
  whose `recordStateEvent(...)` throws. This proves append failure marks the trace incomplete
  without skipping or repeating the already-executed legacy mutator. A throwing `TraceSink` is not
  this fixture because sinks receive only finalized envelopes. This contributes evidence toward
  the 4A2a fault-injection debt, but capture enablement remains closed until its gate explicitly
  clears that debt.
- Fallback fixtures assert exact shared order and distinct outer/shared owners. Primary evaluator
  fixtures assert zero `HEURISTIC_SHARED` events.
- Affected-module package, expanded trace/tie/finalizer suite, `git diff --check`, no deployment,
  and no push remain mandatory.

Frozen `08e544f50` parent evidence:

- `HeuristicAiBase` source cardinality is `1/1/1/1` for phase change, update, explicit cancel
  block, and record response respectively.
- Normalized Rando `DecisionTracker.updateState` bytecode hash:
  `f26e1bcfc200a4f0883ad3e13e81aa8f4455bcfc8e4d51cb300068b269a933c4`.
- Normalized Rando `DecisionTracker.recordDecision` bytecode hash:
  `0404f900e4965264efd651fec8b669e725583453a8ccfe3b52422fb68d2cb3f5`.
- Normalized Rando `DecisionTracker.onPhaseChange` bytecode hash:
  `9fbf2944ba9df6eff29ba7a4c5cd261ba56310dc9c6c6bc5abdbebdfa8147e9d`.
- Normalized Rando `DecisionTracker.blockLastActionOnCancel` bytecode hash:
  `6488bcd3febce7f3d2c1eabcbd7f85eca1c250108db0b94d0fbaf0b14692819c`.
