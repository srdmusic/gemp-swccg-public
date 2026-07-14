# Trace Stage 4A2a: outer tracker lifecycle packet

Date: 2026-07-13
Architecture owner: Codex/Alfred
Java implementer: K-2/Claude
Frozen parent: `978b58e1d5892590ce25e4a8ba7647455f4e34f0`
Status: implementation authorized after K-2 source and council preflight
Deployment: held
Push: held

## Goal

Observe the two existing outer `DecisionTracker` lifecycle owners without changing their behavior:

1. The one `decisionTracker.updateState(...)` call in each bot's
   `updateDecisionTrackerState(...)` helper.
2. The one new-game `decisionTracker.clear()` call in each bot's `trackGameState(...)` helper.

Each legacy mutator must remain authoritative, retain its source order, and run exactly once.
Tracing records one immutable post-mutation event only when a `TraceSession` is active. Production
capture remains disabled.

Current source cardinality is exact: two outer `updateState(...)` calls and two outer new-game
`clear()` calls are in scope. The third `updateState(...)` call is the distinct inherited
`HeuristicAiBase` owner and is explicitly reserved for 4A2b.

## Typed model

Add these records under `ai/models/common/trace/state`:

- `DecisionTrackerLifecycleSnapshot`: the existing complete `DecisionTrackerSnapshot` plus exact
  `lastTurn` and `lastStateHash`. It is immutable and null-rejecting. These are outer
  `updateState(...)` owner fields, not asserted board truth. Do not include `lastPhase`: only
  `onPhaseChange(...)` writes it, and that call is reachable through the inherited
  `HeuristicAiBase` tracker path reserved for 4A2b, not either outer tracker lifecycle owner.
- `TrackerUpdateStateEvent`: outer owner, exact legacy call arguments (`handSize`, `forcePile`,
  `reserveDeck`, `turn`, `cardsInPlay`), complete lifecycle before/after snapshots, and a derived
  `MutationOutcome`.
- `TrackerClearEvent`: outer owner, closed `ClearCause.NEW_GAME_RESET`, complete lifecycle
  before/after snapshots, and a derived `MutationOutcome`.

Use separate event records. Do not create one nullable Cartesian lifecycle record. Both event
constructors reject `TrackerOwner.HEURISTIC_SHARED`; that owner lands in 4A2b. Factory methods
derive `CHANGED` versus `NO_OP` from exact snapshot equality, and public constructors reject an
inconsistent supplied outcome.

Extend `TraceStateEvent` to permit the two new event records. This completes more of schema 3's
typed state-event union; it does not change envelope fields or bump the schema again.

## Recording API

Add to `TraceSession`:

- `recordTrackerUpdateState(...)`
- `recordTrackerClear(...)`

Each method checks `CURRENT` before constructing an event, appends through
`TraceCollector.recordStateEvent(...)`, and converts any construction/append failure into
`TraceCaptureFailure.Stage.STATE_EVENT` without throwing into gameplay.

In both outer `DecisionTracker.java` files, add one package-local pure
`traceLifecycleSnapshot()` seam. It delegates the already accepted decision-affecting state to
`traceSnapshot()` and adds `lastTurn` and `lastStateHash`. It reads only and never mutates.
`lastPhase` remains excluded by the accepted preflight correction because its writer belongs to
the inherited 4A2b owner.

## Hook law

In both bots, bracket only the existing legacy call:

1. When `TraceSession.isActive()`, build the before snapshot under an instrumentation-only
   `try/catch`.
2. Run the existing legacy mutator exactly once, outside the instrumentation failure path.
3. If the before snapshot exists, build the after snapshot and record the typed event under an
   instrumentation-only `try/catch`.
4. On snapshot failure, call `TraceSession.markCaptureFailure(STATE_EVENT, ...)`; never skip,
   repeat, or alter the legacy mutator.

For `UPDATE_STATE`, record the exact already-computed call arguments. The helper currently leaves
zero defaults after a caught game-state getter exception; describe these as legacy call arguments,
not authoritative observations.

For `CLEAR`, use only `NEW_GAME_RESET`. Preserve current source order: pending-concede clear, outer
new-game writes, chat reset, tracker clear, seen-set clears, then strategy-component resets.

## Exact production files

- New `DecisionTrackerLifecycleSnapshot.java`
- New `TrackerUpdateStateEvent.java`
- New `TrackerClearEvent.java`
- Existing `TraceStateEvent.java`
- Existing `TraceSession.java`
- Mirrored `rando/DecisionTracker.java` and `chosenone/DecisionTracker.java`
- Mirrored `rando/RandoCalAi.java` and `chosenone/TheChosenOneAi.java`
- Required changelog/history bookkeeping

No evaluator, engine, mediator, objective, strategy, deploy, or card source file belongs in this
commit.

## Acceptance fixtures

Pure common-state tests:

- lifecycle snapshot null rejection and immutable nested state;
- both events derive `CHANGED` and `NO_OP` correctly;
- inconsistent outcomes and `HEURISTIC_SHARED` are rejected;
- clear accepts only `NEW_GAME_RESET`.

Real tracker fixtures in both bot packages:

- identical repeated update produces `NO_OP`;
- changed state hash resets repeat/loop counts according to the existing owner;
- turn change clears canonical turn-block rows;
- new-game clear resets every field included in the lifecycle snapshot;
- Rando and ChosenOne snapshots/events match after package, class, and owner normalization.

Bot-boundary fixtures:

- traced and untraced responses remain identical;
- first non-null-game decision records outer tracker clear before outer tracker update;
- a same-game repeat records no second clear and an update event whose outcome follows snapshot
  equality;
- a direct-return interceptor records no final `RECORD_RESPONSE`, preserving current control flow;
- production default opens no session and records no lifecycle event.

Gate:

- exact source count remains one reachable outer `updateState` call and one new-game outer `clear`
  call per bot;
- normalized bot edits and fixtures match;
- affected-module package passes;
- existing expanded trace/tie/finalizer suite plus new fixtures passes;
- `git diff --check` passes;
- no deployment and no push.

## Non-goals

- No inherited `HeuristicAiBase` tracker hooks. Those are 4A2b.
- No event for the separate outer-bot `lastTurn`/`lastPhase` fields; this slice includes only the
  fields owned by each `DecisionTracker` instance.
- No F1 `MultipleChoiceAwaitingDecision` or F2 `SwccgGameMediator` edits.
- No CONTROL, ACTIVATE, evaluator, objective, deploy, finalizer, RNG, scoring, routing, or behavior
  repair.
- No capture enablement, owner consolidation, schema bump, retirement, deployment, or push.

## K-2 preflight

Before editing, K-2 must verify the four call sites against the current source and ask the council
one narrow question: does the separate `UPDATE_STATE`/`CLEAR` record design preserve the 4A0
bijection without duplicating direct-write ownership? Report any correction before implementation;
otherwise implement this packet as one isolated commit.

K-2 preflight correction accepted before implementation: the outer lifecycle snapshot excludes
`lastPhase`. Consecutive direct outer `updateState(...)` and `clear()` calls must still yield exactly
one corresponding event per legacy call when a trace session is active.
