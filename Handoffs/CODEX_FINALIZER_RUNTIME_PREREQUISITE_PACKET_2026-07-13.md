# Finalizer Runtime And Accepted-Response Lifecycle Prerequisite

Date: 2026-07-13
Architect/gate: Codex/Alfred
Implementer: K-2/Claude small agents
Baseline: `ad8f593857c443aeecce37b9e397a792e68dc914`
Status: `CORRECTED AND RELEASED FOR JAVA: m00585, 2026-07-13`
Approval brief: `Handoffs/CODEX_FINALIZER_RUNTIME_APPROVAL_BRIEF_2026-07-13.md`

Read-only review: K-2 source review plus council `AGREE`, mailbox `m00568`, with Curator lifecycle
addendum verified in `m00571`. Codex's independent source gate then found four load-bearing runtime
contract gaps and issued the technical hold in `m00579`; K-2 preserved the worker edits pre-test and
pre-commit in `m00580`. Steve's approval remains in force. This corrected packet requires K-2 and
council reconciliation before the preserved edit resumes.

## Why This Phase Exists

The pure response finalizer cannot safely own a live phase route yet:

- `SwccgAiController.decide()` returns only a raw `String`
- `ResponseFinalizer` explicitly has no production consumer
- `FinalizedResponse` says its tracker mutation is applied only after engine acceptance
- Rando and ChosenOne currently run outer safety and tracker mutation inside `decide()`
- `SwccgGameMediator` validates the returned string only after those mutations have occurred
- a checked engine rejection retries the AI after its tracker was already changed

This is a global runtime/API boundary, not ACTIVATE or CONTROL policy. It must land and gate as its
own major phase before any phase owner becomes authoritative.

## Approval Boundary

This phase changes production files including:

- `SwccgAiController.java`
- `SwccgGameMediator.java`
- Rando and ChosenOne decision entry points
- Curator lifecycle forwarding

Those are engine/API changes with cross-AI impact. Steve explicitly approved this packet and the
remaining planned phases on 2026-07-13. K-2 may implement this packet as one coherent Java phase.
Deployment and game execution remain gated until the complete code and verification sequence passes.

## Objective

Add one typed AI result envelope, immutable loop-local rejection history, and synchronous
engine-disposition callbacks. Preserve every legacy wire response. Move only the outer bot's common
accepted-response mutations from before engine validation to the acceptance callback. Keep the
decision trace open until the mediator reports acceptance, rejection, typed rejection, or attempt
failure.

This prerequisite does not cut over an ACTIVATE, CONTROL, or other phase owner. It does not make
the pure finalizer authoritative by itself. It creates the exact runtime boundary the later owner
uses to submit `FinalizedResponse` safely.

## Success Criteria

- mediator-facing AI calls return a typed response or typed pre-engine rejection
- every accepted wire response receives exactly one acceptance callback
- every engine-rejected response receives exactly one rejection callback and zero accepted commits
- typed pre-engine rejection remains pending and becomes visibly terminal without calling
  `decisionMade`
- the mediator owns one immutable rejection history per decision loop; the first attempt receives
  count 0 and the single retry receives count 1 with the exact rejected wire and typed engine reason
- the outer Rando/Chosen tracker and strategic-event commit occur only after engine acceptance
- direct interceptor routes retain their existing no-outer-tracker behavior
- legacy heuristic fallback retains its existing shared-base mutations and receives its existing
  outer mutation once, but the outer mutation moves post-acceptance
- every mediator-facing Rando/Chosen route defers trace close to disposition, including mutation
  mode `NONE`; mutation ownership never decides trace ownership
- trace emission for mediator-facing calls occurs after engine disposition and contains the exact
  submitted wire, closed disposition, and mutation outcome without fabricating an accepted response
- Curator forwards the actual accepted override response to the wrapped Rando lifecycle
- engine acceptance latches before the accepted callback; callback failure cannot trigger rejection
  disposition, a second response, or a retry
- direct callers of the existing `decide()` API remain backward compatible
- no wire response, evaluator score, route, candidate order, RNG draw, retry count, or game policy
  changes

## Non-Negotiable Boundaries

- One coherent phase edit, one verification pass, one commit.
- No test runs while editing.
- No games, `VirtualTableScenario`, sandbox scenarios, replays, or live-server checks.
- No deploy, reload, restart, push, or live-game interaction.
- No ACTIVATE, CONTROL, PULL, DEPLOY, BATTLE, MOVE, DRAW, SETUP, objective, or scoring edits.
- Do not retire either legacy `DecisionSafety` copy in this phase.
- Do not move shared `HeuristicAiBase` loop-memory mutations in this phase. Freeze their existing
  pre-accept behavior as a declared residual for unowned fallback routes.
- Do not use mutable pending state keyed only by numeric decision id. Several decisions reuse id 1.
- Do not persist rejection history in a map, bot field, numeric-id key, or `ThreadLocal`. It is an
  immutable local value owned only by the mediator retry loop.
- Do not use `TraceSession.abandon()` as a normal wrapper-failure path. It discards the evidence this
  phase exists to preserve.
- Preserve unrelated dirty and untracked files. Stage exact owned paths and changelog hunks only.

## 1. Typed Mediator Result

Add one small immutable `AiDecisionResult` in the server AI package. Use a closed status:

- `WIRE_RESPONSE`
- `TYPED_REJECTION`

The result contains:

- exact wire response, non-null only for `WIRE_RESPONSE`; Pass remains `""`
- typed rejection code and nonblank detail, present only for `TYPED_REJECTION`
- accepted-mutation mode: `NONE` or `OUTER_COMMON`
- optional immutable `FinalizedResponse.TrackerMutationRequest`, present only when a typed finalizer
  produced the response
- immutable diagnostic decision id; object identity remains the mediator's retry/terminal key
- whether the result came from legacy raw-string compatibility or a typed finalizer

Construction rejects contradictory states. Do not represent rejection as null, empty Pass, ordinal
zero, an exception message, or an optional that can be mistaken for an unowned route.

The accepted-mutation mode is data, not a callback closure. It must not expose `Runnable`,
`Consumer`, mutable engine objects, or arbitrary extension maps.

The accepted-mutation mode describes only post-acceptance mutation ownership. It does not describe
trace ownership. All mediator-facing Rando and ChosenOne results, both `NONE` and `OUTER_COMMON`,
retain a deferred trace lifecycle until one disposition callback closes it. This is required because
Curator can replace a wrapped Rando `NONE` response, including the V45 forfeit route.

A typed-finalizer wire result requires a non-null tracker mutation request whose decision id and
wire response match the result. A legacy compatibility wire result may use `OUTER_COMMON` without
that finalizer descriptor. A typed rejection carries neither an accepted mutation mode nor a
tracker mutation request.

## 2. Backward-Compatible Controller API

Keep the existing method for direct callers:

```java
String decide(String playerId, AwaitingDecision decision, GameState gameState);
```

Add a default mediator-facing method whose default implementation wraps `decide()` as a legacy
`WIRE_RESPONSE` with mutation mode `NONE`. Keep that three-argument method and add a
backward-compatible overload that accepts immutable `RejectionHistory`. The four-argument default
delegates to the three-argument method so an existing controller override is not bypassed. Rando,
ChosenOne, and Curator override the history-aware method; their three-argument methods delegate with
`RejectionHistory.empty()`.

Add default no-op disposition callbacks. Rejection disposition uses a closed kind:
`ENGINE_REJECTED`, `TYPED_REJECTION`, or `ATTEMPT_FAILED`. The failure callback covers a computation
or wrapper exception that escapes before an `AiDecisionResult` exists.

```java
AiDecisionResult decideForEngine(String playerId, AwaitingDecision decision, GameState gameState);
AiDecisionResult decideForEngine(String playerId, AwaitingDecision decision,
                                 GameState gameState, RejectionHistory history);
void onDecisionAccepted(String playerId, AwaitingDecision decision,
                        GameState gameState, AiDecisionResult result);
void onDecisionRejected(String playerId, AwaitingDecision decision,
                        GameState gameState, AiDecisionResult result,
                        DecisionRejectionKind kind, String detail);
void onDecisionAttemptFailed(String playerId, AwaitingDecision decision,
                             GameState gameState, String detail);
```

Exact names may change only before packet release. The contract may not change after release without
a hard-stop return to Codex.

The callbacks execute synchronously on the same mediator thread as `decideForEngine`. They cannot
schedule, recurse, call `decisionMade`, or carry pending actions. Callback detail is nonblank. The
typed-rejection callback receives its original result; the attempt-failed callback receives no
fabricated result.

Extend the typed reason used by `RejectionHistory.Attempt` with
`ENGINE_DECISION_INVALID`. `ResponseFinalizer` never produces this engine-owned reason. Add one
immutable append operation that returns a new history and rejects blank detail; do not mutate the
existing list.

## 3. Mediator Ordering

Change only the AI path. The human path remains byte-for-byte unchanged.

Create `RejectionHistory.empty()` once inside the existing F2 retry loop, outside its attempt body.
Pass that immutable value to every history-aware `decideForEngine` call. Do not retain it after the
loop exits.

For each F2 attempt:

1. call history-aware `decideForEngine`; first-attempt history count is 0
2. if computation or a wrapper throws before returning a result, call `onDecisionAttemptFailed`
   once with nonblank detail, leave no active trace, and follow the existing runtime-fault path with
   no AI retry
3. if `TYPED_REJECTION`, keep the same pending decision and timer, call `onDecisionRejected` once
   with `TYPED_REJECTION`, report visible terminal failure once, and return without
   `participantDecided` or `decisionMade`
4. for `WIRE_RESPONSE`, preserve remove-before-callback by calling `participantDecided` before
   `decisionMade`
5. on checked `DecisionResultInvalidException`, requeue the same object immediately, call
   `onDecisionRejected` once with `ENGINE_REJECTED`, then immutably append the exact returned wire,
   `ENGINE_DECISION_INVALID`, and nonblank exception detail before the existing single retry; the
   second attempt receives history count 1
6. if `decisionMade` throws an unchecked exception, call `onDecisionRejected` once with
   `ATTEMPT_FAILED`, leave no active trace, and follow the existing runtime-fault path with no retry
7. after `decisionMade` returns, latch engine acceptance before invoking `onDecisionAccepted`
8. call `onDecisionAccepted` exactly once before clock credit, chat, pending-action continuation,
   and new clock scheduling
9. preserve the existing F2 attempt count, object-identity terminal guard, timer behavior,
   `aiChainCounter`, checked-exception boundary, and visible reporter

An accepted callback owns its mutation and trace close in `try/finally`. If it throws after the
acceptance latch, the mediator logs the callback fault and continues the already-accepted engine
path. It must not call a rejection callback, submit a second response, append rejection history, or
retry.

The attempt owner guarantees exactly one terminal disposition callback when a trace-capable result
exists, or exactly one attempt-failed callback when no result exists. No arbitrary
`RuntimeException` becomes a retry. A wrapper failure must close and emit typed failure evidence
through its controller callback; a generic mediator `TraceSession.abandon()` is forbidden because
it would silently discard the attempt.

## 4. Rando And ChosenOne Lifecycle

Refactor the mirrored entry points around one internal decision computation. Preserve direct-call
behavior:

- `decide()` computes and applies the current outer common mutation before returning, exactly as
  direct callers observe today
- `decideForEngine()` computes the same wire response but returns `OUTER_COMMON` without applying
  the outer common mutation
- direct interceptors return mutation mode `NONE`, preserving their existing bypass of outer safety,
  tracker, and strategic-event handling
- chaos, CombinedEvaluator, and heuristic-fallback routes that currently reach the outer common
  boundary return `OUTER_COMMON`
- the history-aware method receives the mediator's immutable history and does not modify or persist
  it; until a phase owner calls `ResponseFinalizer`, legacy routes only carry it forward

The accepted callback applies the existing outer common mutation exactly once:

1. record the exact submitted wire and `ENGINE_ACCEPTED` disposition
2. outer `DecisionTracker.recordDecision`
3. `trackStrategicEvents`
4. record accepted-mutation outcome and the exact accepted final response
5. trace close and emit

For mutation mode `NONE`, steps 2 and 3 are skipped by contract, the accepted-mutation outcome says
no mutation executed, and the exact accepted response is still recorded before close. If an outer
mutation throws, mark the accepted trace incomplete with mutation outcome false, close it in
`finally`, and rethrow for mediator logging. The engine disposition remains accepted.

Move the outer tracker's before-snapshot into the callback together with `recordDecision`. Taking
the snapshot during computation and recording later would create a false before/after delta.

Use the actual accepted wire response in the callback. Do not rely on a response cached separately
from `AiDecisionResult`.

The rejection callback performs no tracker or strategic mutation. It records the actual submitted
wire when one exists, the typed disposition and detail, closes the attempt trace, and clears every
thread-local or pending lifecycle handle. The attempt-failed callback records `ATTEMPT_FAILED`,
closes and emits any active attempt, and fabricates neither a wire result nor an accepted response.

Trace closing is call-path-aware, never mutation-mode-aware:

- direct `decide()` closes inline exactly as today
- every mediator-facing result, including `NONE` interceptors, skips the computation-finally close
  and closes only in one synchronous disposition callback
- a mediator-facing computation exception closes through `onDecisionAttemptFailed`; a direct-call
  computation exception closes inline

Never close a mediator-facing trace in both locations, never close it before Curator can replace the
wire response, and never leave it to a child-decision loop.

The existing outer emergency and `DecisionSafety.ensureValidResponse` remain in the computation for
legacy routes. A later phase owner may bypass them only after producing a typed finalized response.

## 5. Heuristic Fallback Residual

`HeuristicAiBase.decide()` currently mutates its own distinct shared tracker, recent-response
memory, reassignment memory, and failed-search state before returning. Do not pretend those writes
have moved in this phase.

For an accepted fallback attempt, preserve current behavior:

- shared-base mutations happen during computation
- outer Rando/Chosen mutation happens once after engine acceptance

For an engine-rejected fallback attempt, the shared-base mutations remain the declared residual,
while the outer mutation does not occur. Record this exact asymmetry in fixtures and changelogs.
The fallback owner is migrated separately when its private scoring and state contract is frozen.

## 6. Curator Compatibility

Curator wraps a real `RandoCalAi`, then may replace Rando's wire response.

- its existing direct `decide()` behavior stays unchanged
- history-aware `decideForEngine()` calls the wrapped Rando history-aware mediator method with the
  same immutable history value, not raw `decide()` and not `RejectionHistory.empty()`
- typed rejection from wrapped Rando bypasses model consultation and is returned unchanged
- a Curator override replaces only the result wire response; it preserves the wrapped lifecycle
  metadata and changes the accepted mutation's response to the actual Curator choice
- Curator's accepted and rejected callbacks forward exactly once to the wrapped Rando
- Curator's attempt-failed callback forwards exactly once when failure occurs after wrapped Rando
  opened a deferred lifecycle but before Curator returned an `AiDecisionResult`
- the accepted tracker records the actual engine-accepted Curator response, not the advisory
  Rando pick

Do not invoke the local model in tests. Use a trivial non-consulting decision for forwarding tests
and the deterministic seam below for the override fixture. The currently required minimal seam is
explicit:

- a package-private constructor that accepts the wrapped `RandoCalAi` for lifecycle-forwarding
  tests; the public constructor still creates the normal Rando instance
- one pure package-private override-application helper that replaces the wire response while
  preserving lifecycle metadata; production consultation uses this helper

Do not add a general HTTP, Ollama, executor, or network abstraction. The pure helper proves the
override transformation without contacting Ollama, whose current synchronous consult can otherwise
run for up to 300 seconds.

The wrapped Rando response is advisory until Curator returns. Trace proposed-wire evidence is
recorded from the outer result inside the disposition callback, so a Curator override records the
actual wire submitted to the engine. If consultation throws before Curator returns, no wire was
submitted; the attempt-failed callback records that fact without inventing a proposed or accepted
response.

## 7. Trace Lifecycle

Mediator-facing Rando and ChosenOne calls must not close the trace at the end of computation. The
same synchronous thread-local session remains active until one disposition callback closes it.
Mutation mode `NONE` does not change this ownership.

Extend the immutable trace finalization evidence with the minimum closed lifecycle fields:

- proposed wire response plus an explicit recorded flag so Pass `""` differs from no submitted wire
- one closed disposition: `ENGINE_ACCEPTED`, `ENGINE_REJECTED`, `TYPED_REJECTION`, or
  `ATTEMPT_FAILED`
- nonblank detail for every non-accepted disposition
- accepted mutation mode and whether that mutation completed

`finalResponse` keeps its existing meaning: the exact response accepted by the engine or returned by
a direct call. A checked engine rejection, typed rejection, or failed attempt must never populate it.
The collector completeness law becomes disposition-aware:

- `ENGINE_ACCEPTED` requires proposed wire, final response, and mutation outcome
- `ENGINE_REJECTED` requires proposed wire and nonblank detail, but no final response
- `TYPED_REJECTION` requires nonblank detail and no proposed or final response
- `ATTEMPT_FAILED` requires nonblank detail and no fabricated final response; a proposed wire is
  present only when a real result existed before the failure

A rejected or failed attempt can therefore be `COMPLETE` without a final response when all facts
required by its route and disposition were captured. A genuine capture or mutation-recording fault
remains `INCOMPLETE` with its typed failure. Do not relabel a clean engine rejection as a capture
failure merely because no accepted response exists.

One attempt emits one trace. A checked rejection followed by the single retry emits two traces in
attempt order. Every accepted, rejected, exceptional, and typed-rejection path must clear
`TraceSession` in a callback-owned `finally`. Disabled trace remains `NoOpTraceSink.INSTANCE` and
builds no snapshots.

`decisionMade` does not re-enter the AI. The only child-AI scheduling path is
`startClocksForUsersPendingDecision`, after pending-action continuation. The acceptance callback
must therefore run and close the parent trace before both operations. Do not keep a trace open
across pending-action continuation or child-decision scheduling.

Curator's synchronous model consultation may intentionally hold the wrapped Rando trace open for up
to 300 seconds after Rando returns its deferred-close result. This is sequential on the mediator
thread. A caught timeout follows the existing Rando-fallback path and closes through normal engine
disposition. An unchecked consult failure that escapes Curator closes and emits through Curator's
forwarded attempt-failed callback. No mediator path may silently abandon that trace.

## 8. Finalizer Adapter Seam

Add one pure adapter from `FinalizedResponse` to `AiDecisionResult`:

- `ACCEPTED`, `CORRECTED`, and `FORCED` with a non-null wire become `WIRE_RESPONSE`
- `REJECTED` becomes `TYPED_REJECTION` with exact typed reason and detail
- the adapter copies the finalizer's tracker mutation request into the closed
  `OUTER_COMMON` lifecycle descriptor; it never applies it

The adapter is a production seam with focused tests, but no phase owner calls `ResponseFinalizer`
yet. Update stale finalizer comments that still say the engine lacks MULTIPLE_CHOICE bounds checking;
F1 now supplies that guard. Do not change finalizer behavior.

## 9. Fixture Matrix

Use pure or mocked server tests only. No VTS or game execution.

### Controller And Mediator

- legacy default controller returns one wire response and receives no callback mutation
- valid typed response: one AI call, one `decisionMade`, one accepted callback, zero rejected
  callbacks, clock credited once, pending actions continue once
- checked invalid then valid: two results with prior-rejection counts 0 then 1, one rejected callback,
  one accepted callback, one final accepted mutation, and immutable history containing the exact
  first wire plus `ENGINE_DECISION_INVALID`
- checked invalid twice: two rejected callbacks, zero accepted callback, same decision pending,
  timer retained, one visible terminal report
- typed pre-engine rejection: zero `participantDecided`, zero `decisionMade`, one rejected callback,
  same decision pending, timer retained, one visible terminal report, no retry
- `decisionMade` runtime fault: one `ATTEMPT_FAILED` rejection disposition and no retry
- accepted-callback fault: acceptance was latched first, zero rejected callbacks, zero retry, and
  accepted engine continuation occurs once
- failure before a result: one attempt-failed callback, zero result callbacks, and no retry
- chain-limit terminal path preserves its existing behavior and emits no fabricated callback

### Rando And ChosenOne

- direct `decide()` preserves exact current response and outer state-event sequence
- mediator computation produces the same response but no outer tracker or strategic mutation before
  acceptance
- acceptance applies exact outer tracker and strategic events once
- rejection applies neither and clears the trace session
- direct interceptors retain mutation mode `NONE`; mediator-facing V45, V44/V67j, V170, V61, and
  Rando-only V79b fixtures or branch-by-branch static proof show deferred trace close
- normal CombinedEvaluator, chaos, and heuristic fallback use `OUTER_COMMON`
- chaos receives branch-by-branch static parity proof when its zero-percent production constant makes
  a runtime fixture unreachable; do not add behavior merely to force coverage
- fallback fixture freezes the declared shared-base pre-accept residual
- injected outer tracker mutation failure proves accepted disposition remains latched, mutation is
  not reported complete, the trace closes as incomplete, and no retry occurs
- normalized bot parity is exact except typed owner labels

### Curator

- passthrough forwards accepted callback once
- injected wrapped-Rando fixture proves disposition forwarding without network access
- pure override-application helper records the actual accepted override response while preserving
  lifecycle metadata
- a `NONE` V45 wrapped result overridden by Curator records the override as the accepted final
  response and still applies no outer mutation
- Curator forwards the identical immutable rejection history to wrapped Rando
- rejection bypasses consultation and forwards rejection once
- caught consultation timeout preserves the existing Rando fallback and closes through its eventual
  engine disposition
- unchecked consultation failure after wrapped Rando returns emits one `ATTEMPT_FAILED` trace,
  applies no accepted mutation, and cannot leak lifecycle state into the next mediator attempt

### Finalizer Adapter

- accepted/corrected/forced statuses map exact wire and mutation descriptor
- rejected status maps exact typed code/detail and no accepted mutation
- no adapter path draws RNG, mutates a tracker, or calls the mediator

### Trace

- accepted attempt emits one complete trace after accepted mutation
- accepted `NONE` override emits one complete trace with the actual outer wire
- checked rejection plus retry emits two complete traces in attempt order; first has proposed wire
  plus `ENGINE_REJECTED` and no final response, second has `ENGINE_ACCEPTED`
- typed pre-engine rejection emits one complete rejection trace
- post-result attempt failure emits a disposition trace without a fabricated accepted response
- actual tracker mutation failure emits one accepted but incomplete trace and closes the session
- every path leaves `TraceSession.isActive()` false
- trace-disabled and trace-enabled wire results are identical

## 10. One Verification Pass

After all edits are complete, run one focused Maven pass containing:

- existing `SwccgGameMediatorAiRetryTest`
- new mediator lifecycle tests
- Rando/Chosen lifecycle and trace-hook tests
- Curator lifecycle tests
- existing `ResponseFinalizerContractTest`
- finalizer-to-result adapter tests
- immutable rejection-history tests
- affected tracker trace tests

Use `-Dsurefire.failIfNoSpecifiedTests=false -DskipITs`. Do not run package wildcards, the full
module, VTS, games, sandbox scenarios, deploy, or live reload.

Then run:

- `git diff --check`
- exact changed-path proof against baseline
- normalized Rando/Chosen production and test parity
- search proof that mediator uses only `decideForEngine`
- source and fixture proof that mediator owns loop-local immutable history and passes counts 0 then 1
- source proof that Curator forwards history unchanged and no map, bot field, numeric-id key, or
  `ThreadLocal` stores it
- search proof that each accepted result calls the callback once
- search proof that typed rejection never reaches `decisionMade`
- source-order proof that acceptance callback precedes pending-action continuation and child clock
  scheduling
- source-order proof that engine acceptance latches before the accepted callback
- source-order proof that an accepted-callback fault cannot reach rejection or retry
- call-path-aware close proof: direct calls close inline; every mediator result, including `NONE`,
  closes only in a disposition callback; failure before a result closes via attempt-failed callback
- trace schema proof that rejected and failed attempts can complete without a fabricated final
  response
- search proof that outer Rando/Chosen tracker recording has no mediator-facing pre-accept path
- retry wire-parity proof that moving the outer tracker record post-accept does not alter the second
  attempt; shared-base fallback memory remains the declared pre-accept residual
- search proof that the outer tracker before-snapshot moved with its callback record
- search proof that direct interceptors still carry mutation mode `NONE`
- search proof that Curator forwards disposition exactly once
- fixture or static branch proof for every direct interceptor and chaos route
- search proof that `ResponseFinalizer` still has no phase-owner caller
- `NoOpTraceSink.INSTANCE` production default proof
- unrelated dirty/untracked file exclusion proof

Create one phase commit only after all gates pass. Return the SHA, parent, changed paths, focused
test counts, callback count evidence, trace lifecycle evidence, declared residual, and excluded-file
status to Codex for one independent aggregate gate.

## 11. Hard Stops

Stop the whole phase and return exact evidence if:

- the human mediator path must change
- acceptance or rejection callback would run on a different thread
- trace cannot remain open safely until the synchronous callback
- accepted mutation mode and mediator trace-close ownership cannot remain orthogonal
- a mediator-facing `NONE` route would close before Curator can replace its wire response
- loop-local immutable rejection history cannot reach Rando, ChosenOne, Curator, and the later
  `ResponseFinalizer` caller without a map, bot field, numeric-id key, or `ThreadLocal`
- an engine-rejected attempt cannot emit a complete trace without fabricating an accepted response
- engine acceptance cannot latch before the accepted callback, or callback failure can reach
  rejection or retry
- a result needs mutable pending state keyed only by numeric decision id
- Curator cannot forward the actual accepted override response without consulting the network
- any direct caller loses its current `decide()` behavior
- any wire response, score, route, candidate order, RNG draw, retry count, or phase behavior changes
- a required invariant can be proven only with VTS, a game, sandbox, or live server

## 12. Worker Return

K-2 returns one phase-boundary report:

- commit SHA and parent
- exact changed paths
- focused test command and counts
- accepted/rejected callback count matrix
- retry-history count and exact-wire matrix
- normalized parity result
- trace close/emit/disposition matrix
- Curator compatibility result
- declared HeuristicAiBase residual
- static search proof
- excluded dirty/untracked file status
