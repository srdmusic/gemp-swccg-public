# Rando Runtime Route and Shadow Contract

Date: 2026-07-13
Owner: Codex/Alfred
Source baseline: `b94af20e1`; semantic-subroute amendment checked against audits through `d558248cf`
Status: required before DecisionRouter shadow wiring

## Current route

The runtime does not have a `DecisionRouter` class. The actual order is:

1. `SwccgGameMediator` supplies `AwaitingDecision`, calls `setGame`, then `decide`.
2. Rando creates its ephemeral context, tracks game state, handles deferred concede, and updates
   the outer decision tracker.
3. V45 directly intercepts optional forfeit.
4. V44/V67j directly intercepts revert approval.
5. V170 directly intercepts undercover choice.
6. V61 directly intercepts saga choice.
7. Rando-only V79b directly intercepts Verge parsec choice. ChosenOne lacks this branch.
8. Outside deploy/battle, chaos may bypass evaluators and call the heuristic base.
9. The normal lane builds mutable `DecisionContext`, refreshes strategy/objective/deck state, and
   installs deploy-script buckets.
10. `CombinedEvaluator` runs every applicable evaluator and additively merges action ids. Hard veto
    uses OR semantics.
11. Selection walks deploy buckets, the non-bucket epilogue, legacy action filters, formation veto,
    then the V148 all-bad/pass gate.
12. The evaluator result is formatted for multi-select or returned as an action id.
13. If no evaluator handles the decision, `HeuristicAiBase.decide` runs its separate routing,
    safety, tracker, and failed-search/reassignment memories.
14. Outer emergency fallback uses raw `noPass`; final safety forces required choices and clamps card
    responses to selectable ids.
15. The outer tracker and strategic state record the result, context clears, and the mediator alone
    commits the response with `decisionMade`.

Primary source anchors: `RandoCalAi.java:505-1221`, `CombinedEvaluator.java:39-315`,
`DecisionSafety.java:59-255`, `HeuristicAiBase.java:48-107`, and
`SwccgGameMediator.java:1278-1327`.

## Bypass and state hazards

- The five direct interceptors return before outer emergency validation, decision recording, and
  strategic-event tracking. Only context cleanup still runs.
- Rando's outer tracker and `HeuristicAiBase`'s private tracker are separate loop systems.
- `trackGameState` resets strategy state, tracks cards/shields, and may mutate the actual game through
  deferred loss.
- Evaluator execution mutates deploy-plan caches, retry budgets, barrier memory, AMSD state, and
  opponent-intelligence state.
- A rejected `decisionMade` response may be sent again, but those AI-side mutations are not rolled
  back.
- Outer emergency uses raw `noPass`, while `DecisionSafety.mustChoose` exempts minimum-zero
  Done/Cancel prompts. A valid V148 pass can therefore be replaced by emergency selection.
- The shared heuristic fallback is compiled against Rando safety/tracker classes. ChosenOne's normal
  route otherwise mirrors Rando after namespace normalization.
- Only Rando is constructed by the live `RANDO` factory; ChosenOne parity is source protection, not
  proof of a live ChosenOne route.

## Shadow authority

Legacy remains the only authority and the only mutating execution during shadow mode:

- Preserve interceptor order and results.
- Consume chaos/randomness once. A shadow path cannot make a second RNG draw.
- Preserve evaluator eligibility, bucket order, additive merge, veto OR, V148 handling, multi-select
  formatting, heuristic fallback, and final response correction.
- Preserve all legacy persistent mutations and failed-search memories.
- Only the legacy final response may reach `decisionMade`.

The shadow path consumes an immutable snapshot and must be pure. Do not run legacy evaluators twice,
clone shallow strategy objects, or call mutating helpers from both lanes. Instrument the one legacy
execution to capture its trace; run the new route from frozen facts and compare after legacy final
safety correction.

## Target router stages

### 1. Normalize

Build the immutable decision snapshot once, preserving raw parameter presence/array lengths,
candidate order, selectable flags, obligation facts, phase/window, and transaction correlation.

### 2. Intercept

Evaluate the ordered policy interceptors as named routes. Initially they return the same legacy
response, but the router records route id, facts, and result. They do not directly return from
`decide` after cutover.

### 3. Semantic subroute

Select exactly one primary semantic subroute from typed, policy-free evidence. Phase is a window and
an allowed-route constraint, not a sufficient route key. Route evidence includes:

- ordered interceptor result, if any;
- wire decision shape;
- phase/window;
- obligations and pass/cancel eligibility facts;
- raw candidate-array shape and normalized candidate family;
- source/action/destination relationship and parent transaction id when the engine exposes them.

Examples are DRAW action versus optional response inside DRAW, PULL parent versus deploy/take child,
BATTLE initiation versus weapon/forfeit/finalizer choice, CONTROL drain versus unrelated responses,
and each SETUP child route. PULL/SEARCH and RESPONSE may cross phase windows. Chaos and heuristic
fallback are explicit semantic routes, not invisible side exits.

Scores, assessment verdicts, candidate ranks, title desirability, and objective policy may not
select the route. The phase window narrows legal subroutes; the semantic route selects one primary
adapter. Shared facts services and finalization remain cross-cutting and contribute no competing
route owner.

### 4. Evaluate

Produce typed constraints, ranks, and bounded scores from one semantic owner per domain. Record every
ordered contribution before merge.

### 5. Finalize

Apply multi-select formatting, mandatory-choice rules, pass/cancel semantics, selectable-id clamps,
and emergency handling exactly once for every route, including interceptors.

### 6. Record and commit

Update one decision tracker and strategic memory exactly once, then return one response. The mediator
remains the sole game-state commit point.

## Cutover order

1. Add route observation only. Do not redirect control flow.
2. Freeze interceptor, chaos, fallback, finalizer, and state-mutation traces.
3. Build a pure router shadow from the immutable snapshot.
4. Compare route id, candidates, contributions, vetoes, winner, final response, and intended state
   events without applying shadow events.
5. Cut over low-risk interceptors one at a time, while still using the common finalizer/recorder.
6. Cut over phase pipelines only after their domain-owner fixtures pass.
7. Merge the two loop trackers only as an explicit behavior change with loop fixtures.
8. Fix raw-`noPass` versus V148 only as a separate intentional delta.

## Gate

DecisionRouter work advances only when shadow execution is demonstrably non-mutating, makes no RNG
draw, never calls `decisionMade`, and matches the post-finalization legacy result. Any duplicate state
event, changed interceptor order, skipped finalizer, or second tracker update is a `HOLD`.
