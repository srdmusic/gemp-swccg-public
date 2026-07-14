# ACTIVATE + CONTROL Phase B Behavioral Cutover Packet

Date: 2026-07-13
Architect/gate: Codex/Alfred
Implementer: K-2/Claude small agents
Baseline: `PILOT_COMMIT_TBD` (must descend from the independently gated runtime commit)
Status: `PIPELINED: AWAITING V44/V67J PILOT COMMIT AND CODEX AGGREGATE GATE`

Blocking prerequisite:
1. `Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md`
2. `Handoffs/K2_V44V67J_FINALIZER_PILOT_PACKET_DRAFT_2026-07-13.md`
   SHA256 `7b7bc814e343884efad643342d206c7d78384da62f6aa0b3514c14447edf8c98`

The prerequisite release entered the technical correction hold in `m00579`; K-2 confirmed zero code
was written in `m00581`, and the corrected packet hash was sent in `m00584`. Do not release this
packet for Java until the corrected runtime and the pilot are each committed and independently gated.
The pilot establishes the explicit adapter mutation-mode contract: preserve its `NONE` route unchanged;
this phase must request `OUTER_COMMON` explicitly for accepted owned responses. Drafting and source/council
review proceed in parallel so this packet can release immediately after the pilot gate.

## Objective

Cut over the six stamped ACTIVATE and CONTROL routes as one coherent behavioral phase. Replace the
legacy ownership that creates INTEGER and confirmation cross-talk, consolidate the duplicated
CONTROL force-drain policy, and retire only the branches proven replaced by the frozen harness.

This is one implementation tranche, one verification pass, and one phase commit. Do not split it
into intermediate commits or request a gate between internal edits.

## Success Criteria

The phase is complete only when all of these are true:

- both bots consume `ActivateControlRouteResolver` at the same live decision-loop position
- each owned route is evaluated exactly once
- ACTIVATE amount, allowance, zero-confirmation, and acknowledgement are origin-scoped
- normal zero-activation Pass is declined by selecting `No`; the existing V61c keep-three state
  intentionally confirms Pass by selecting `Yes`
- CONTROL force-drain policy has one shared ordered assessment and two thin mirrored adapters
- on the mediator-facing `decideForEngine` path, every `OWNED_INTENT` calls `ResponseFinalizer` once
  and returns one runtime `AiDecisionResult`; every `OWNED_REJECTED` calls it zero times and returns
  one typed pre-engine rejection
- response finalization has one explicit post-engine mutation owner and no silent second evaluation
  or legacy safety path
- migrated routes emit a typed route in the trace while preserving the legacy response and raw
  operation stream except for the documented zero-confirmation correction
- replaced legacy branches are deleted, not commented out
- Rando and ChosenOne remain normalized mirrors
- one focused Maven verification pass and the static gates pass after all edits are complete

## Hard Boundaries

- Do not run tests while editing. Finish the whole phase, then run section 10 once.
- Do not run a game, `VirtualTableScenario`, sandbox scenario, replay, or live-server check.
- Do not deploy, reload, restart, push, or touch a live game.
- Do not change score magnitudes, evaluator registration order, candidate order, exact-tie policy,
  RNG draw count, phase transitions, or unrelated fallback behavior.
- Do not migrate PULL, DEPLOY, BATTLE, MOVE, DRAW, SETUP, objective adapters, or heuristic fallback.
- Do not build a general `DecisionFacts` framework in this phase. Use the smallest immutable types
  required for the six routes and the drain assessment.
- Preserve unrelated dirty and untracked files. Stage only the phase-owned paths and exact
  changelog hunks.
- Stop on a documented hard stop. Do not improvise around an engine contract.

## 1. Live Dispatch Position

Add one mirrored bot adapter per bot and one shared route input builder. The resolver remains pure.
The live dispatch belongs in the existing non-chaos branch:

1. open the trace session exactly where legacy does
2. run the existing direct interceptors unchanged
3. call `shouldApplyChaos()` exactly once in its current position
4. when chaos is false, resolve the stamped ACTIVATE/CONTROL route
5. dispatch an owned route before the normal `tryEvaluators` call
6. when the route is `LEGACY_UNOWNED`, execute the existing `tryEvaluators` and heuristic fallback
   path unchanged except for the explicitly documented removal of universal INTEGER capture
7. preserve the existing emergency, safety, tracker, strategic-event, and trace-finalization order
   unless section 7 explicitly replaces one of those operations for an owned route

Do not resolve before the chaos gate. Even with `CHAOS_PERCENT == 0`, the gate consumes the existing
RNG draw. Moving dispatch ahead of it changes the RNG sequence.

Construct the route input from the already-captured raw decision fields. The thin adapter may read
`gameState.getCurrentPlayerId()` exactly once because `ActivateControlRouteInput` requires a nonblank
current-turn player and that value is not present in the raw decision fields. Route selection must not
perform any other game-state read, build an evaluator context, or call a strategy service. The resolver
remains pure and receives the current-turn player as immutable input.

The closed live matrix is unchanged:

| Phase | Origin | Route |
|---|---|---|
| ACTIVATE | `PHASE_ACTION` | `ACTIVATE_TOP_LEVEL` |
| ACTIVATE | `ACTIVATE_AMOUNT` | `ACTIVATE_AMOUNT` |
| ACTIVATE | `ACTIVATE_ALLOWANCE` | `ACTIVATE_ALLOWANCE` |
| ACTIVATE | `ACTIVATE_ZERO_CONFIRM` | `ACTIVATE_ZERO_CONFIRM` |
| ACTIVATE | `ACTIVATE_INTERRUPTION_ACK` | `ACTIVATE_ACK` |
| CONTROL | `PHASE_ACTION` | `CONTROL_TOP_LEVEL` |
| any incompatible or unstamped decision | any | `LEGACY_UNOWNED` |

## 2. Owner Result Contract

Use one small shared pre-finalizer owner result for the live adapter. It must distinguish:

- `UNOWNED`: continue through the exact legacy path
- `OWNED_INTENT`: carry exactly one `ResponseIntent` to section 7 without evaluating again
- `OWNED_REJECTED`: carry a nonblank typed reason/detail to section 7; never fall through to legacy
  evaluation and never guess a response

An owned route cannot return an empty optional that is later mistaken for unowned. No owned route
may call `CombinedEvaluator` more than once. The pre-finalizer owner result does not carry a wire
response, tracker callback, mutable engine object, or pending state.

The typed owner contract applies directly to `decideForEngine`. The compatibility `decide()` API still
returns a raw `String`. For legal `OWNED_INTENT` results it must return the same finalized wire and apply
the accepted outer mutation once inline. For `OWNED_REJECTED`, it must use the frozen pre-cutover direct
compatibility computation exactly once, with no fabricated typed result and no behavior change. Fixtures
must name and prove that direct mapping separately from the mediator-facing typed rejection path.

## 3. ACTIVATE Owner

Create one visible ACTIVATE owner with five route methods. Keep bot-specific context construction in
thin mirrored adapters and put only genuinely shared pure policy in shared classes.

### Top-Level Action

For this phase, delegate once to the existing `CombinedEvaluator` and translate its selected action
id or Pass into a typed response intent. Preserve candidate order and all existing contributions:

- V167 activation soft block `-200`
- V168 activation `+5000`
- V38.3 activation `+500`
- V61c activation stand-down `-6000`
- V67ak pull contribution `+800`
- V192 contribution and all of its current availability, destination, once-per-turn,
  failed-search, and V61c gates

Do not add deterministic pull-before-activation ordering in this phase. PULL has not migrated to an
owned route, and silently changing the current V192/activation exact-tie result would exceed this
cutover boundary. Freeze the existing first-seen tie behavior until the PULL phase owns the order.

### Decision Origin In Context

Add a typed `DecisionOrigin` field to both mirrored `DecisionContext` classes and builders. Unknown
or absent origin remains unowned. Do not add another `Map<String,Object>` key.

Change `ForceActivationEvaluator.canEvaluate()` so INTEGER ownership requires one of:

- `ACTIVATE_AMOUNT`
- `ACTIVATE_ALLOWANCE`

It must return false for every other INTEGER decision. This is the cross-talk repair.

### Amount Policy

Extract the duplicated amount arithmetic into one small shared pure policy. The mirrored evaluator
adapters remain responsible for obtaining the already-existing facts. Preserve exact operation
order:

1. V57 starts at the raw maximum
2. V61c caps activation to keep three Reserve cards when the existing
   `DecisionContext.isBattlePlausibleThisTurn()` predicate is true
3. V67at caps activation to keep two when three-pile life is `<= 10`
4. V43 raises a legal positive-range choice to at least one
5. clamp to the raw engine minimum and maximum

Use `DecisionContext.getLifeForce`. Do not substitute `GameState.getPlayerLifeForce`.

The owner may build the evaluator context for amount because the legacy evaluator path already did
so. It then delegates once through `CombinedEvaluator`, preserving evaluator lifecycle, trace
operations, and the existing amount score operations. Do not call the evaluator directly outside
`CombinedEvaluator`.

### Allowance

`ACTIVATE_ALLOWANCE` returns the raw maximum through the origin-gated evaluator path. Preserve the
existing `+50` amount score operation and full-activation bonus behavior. Keep decision recipient
and current turn player distinct.

### Zero Confirmation

Build the evaluator context because the legacy ActionText path already did so and the V61c
battle-plausibility predicate is context-owned. Find `Yes` and `No` by exact ordered result labels,
never by assumed ordinals.

- V61c keep-three state: select the original ordinal of `Yes`
- every other legal zero-activation skip: select the original ordinal of `No`
- duplicate, missing, or malformed labels: `OWNED_REJECTED`

The V61c keep-three state is the exact legacy conjunction:
`reserveDeckSize <= 3 && isBattlePlausibleThisTurn()`. Neither fact alone is sufficient. Freeze
reserve sizes 3 and 4 crossed with battle-plausible true and false.

The result must be invariant under `Yes,No` and `No,Yes` permutations.

### Interruption Acknowledgement

Run the existing evaluator-context preparation exactly once for legacy-equivalent side effects, then
do not call `CombinedEvaluator`. The current legacy route enters `tryEvaluators`, whose context build
analyzes or refreshes `ObjectiveAnalyzer`, binds `DeployPhasePlanner`, and analyzes or refreshes
`DeckOracle` before evaluator applicability is checked. Removing those writes during this cutover is
an unrelated behavior delta. Keep this compatibility preparation local and explicit so the later
objective and PULL owners can retire it with their own fixtures.

Find exactly one `OK` result and select its original ordinal. Missing, duplicate, or extra result
labels produce `OWNED_REJECTED`.

## 4. CONTROL Owner

`CONTROL_TOP_LEVEL` delegates once to the existing `CombinedEvaluator`. Preserve the frozen
registration and contribution order:

1. ForceActivation
2. Deploy
3. Battle
4. Move
5. Draw
6. CardSelection
7. ActionText
8. Pass

The origin gate makes ForceActivation contribute nothing to a CONTROL top-level chooser, but leave
its registered position unchanged. Exact raw-float ties continue to retain the first-seen
candidate. Do not evaluate a candidate twice and do not create a second fallback lane for an owned
CONTROL route.

Translate the selected action id to its original candidate ordinal. Translate the empty top-level
winner to the offered Pass response. Missing or ambiguous action-id translation is
`OWNED_REJECTED`, not a guessed index.

## 5. CONTROL Drain Consolidation

Extract the duplicated `ActionTextEvaluator.evaluateForceDrain` policy into one shared ordered
assessment. Keep game-object reads in thin mirrored fact collectors. The shared assessment accepts
immutable facts and emits ordered immutable score operations with rule id, detail, raw float delta,
and terminal state.

Do not eagerly collect every drain fact. The current helper terminates before later engine queries.
Use staged fact slices, or an equivalently small lazy interface, so later queries execute only when
the preceding assessment step is nonterminal. Preserve query order, exception boundaries, and
fail-open/fail-conservative behavior.

The required operation order is:

1. V24.15 nonpositive drain: `-9999`, terminal
2. V189 cost gap of at least two or net-minus-one budget failure: `-2000`, terminal; query failure
   opens only this gate
3. V25 dynamic non-battleground with opponent Simple Tricks: `-9999`, terminal
4. Battle Order affordability: insufficient Force or unaffordable next deploy is `-50`, terminal;
   no deployable body is `+70`, terminal
5. V140 zero-cost drain: `+60`, terminal
6. V104 drain of one or less: `-2000`, nonterminal; suppress only V52/V48 turn logic
7. Battle Order turn logic when not suppressed: turn three or later `+50`, turns one and two `-50`
8. non-Battle Order baseline: deployable body `+50`, otherwise `+70`
9. V52 multi-site contribution: `+300`, `+200`, or `+100` at the existing thresholds
10. Hunt Down contribution: conditional icon `+40`, then `+30`

The mirrored adapter applies emitted operations through the existing reasoning API in exact order.
Preserve raw float bits and existing detail strings unless a shared constant is required to make
the normalized sources identical.

Preserve all surrounding ActionText contributions, including V24.2, V52 cancel, V29.14, V23,
V184, V192, and their current prelude order. A legacy comment saying "veto" does not become a typed
hard veto unless the source already returned terminally or set one.

## 6. Trace Ownership

Add explicit trace routes for the six owned routes, using the same names as
`ActivateControlRoute`. Do not overload `COMBINED_EVALUATOR` after ownership moves.

For amount, allowance, and both top-level routes, preserve the existing candidate, merge, and score
operation streams produced by `CombinedEvaluator`. Only the outer selected route changes.

For zero-confirmation and acknowledgement, record the owner-selected original ordinal and final
response without fabricating evaluator score operations.

`LEGACY_UNOWNED` decisions retain their existing `COMBINED_EVALUATOR` or `HEURISTIC_FALLBACK`
routes. Trace capture remains disabled by default through `NoOpTraceSink.INSTANCE`.

## 7. Response Finalizer And Mutation Boundary

This section consumes the accepted-response runtime API from the blocking prerequisite. Reconcile
the exact committed type/method names before release; the lifecycle and ordering below are frozen.

### One Snapshot And One Finalizer Call

For an owned route only:

1. obtain the route's `OWNED_INTENT` or `OWNED_REJECTED` result
2. reuse one immutable `DecisionSnapshot` built from the complete raw engine parameters and the
   existing trace snapshot inputs
3. derive `ResponseContract.from(snapshot)` once
4. call `ResponseFinalizer.finalize(snapshot, contract, intent, random, history)` exactly once
5. convert the `FinalizedResponse` through the post-pilot pure adapter into one
   `AiDecisionResult`, explicitly selecting mutation mode `OUTER_COMMON`; never alter or reuse the
   pilot route's `NONE` mode
6. return that result without entering outer emergency fallback, either mirrored `DecisionSafety`,
   heuristic fallback, or a second evaluator lane

Refactor the mirrored bot-local trace snapshot helper only as needed to retain the built
`TraceSnapshots.Result` in the current decision computation. When tracing is enabled, the same
snapshot opens the trace and feeds the finalizer. When tracing is disabled, build it lazily only
after the resolver returns an owned route. Never build the snapshot twice and never add game,
tracker, objective, oracle, or planner reads beyond the existing snapshot builder.

If the snapshot is unavailable or its issues make the response contract unknown, return one typed
pre-engine rejection. Do not call the finalizer with fabricated facts.

### Intent Mapping

The mapping is closed:

- empty top-level ACTIVATE or CONTROL winner -> `ResponseIntent.Pass`
- selected top-level action id -> its original `ResponseIntent.CandidateOrdinal`
- ACTIVATE amount or allowance -> `ResponseIntent.IntegerValue`
- zero-confirmation and interruption acknowledgement -> original
  `ResponseIntent.CandidateOrdinal`

On `decideForEngine`, an `OWNED_REJECTED` owner result bypasses finalization and becomes one typed
pre-engine rejection with the exact owner reason/detail. It calls `ResponseFinalizer` zero times and
never enters legacy safety or fallback. Missing, duplicate, ambiguous, or malformed labels never
become Pass, ordinal zero, or a default value on that mediator-facing path. The direct `decide()`
compatibility mapping remains the separately frozen behavior described in section 2.

### Accepted Status And RNG Rule

All six owned-route intents are required to be legal as proposed. Their finalized status must be
`ACCEPTED`, their exact wire must match the owner intent, and the finalizer must consume zero RNG
draws. `CORRECTED` or `FORCED` on any owned route is a hard stop because it would introduce another
behavior or RNG delta beyond this packet. `REJECTED` becomes the runtime typed rejection and never
falls through to legacy safety.

Pass the exact immutable `RejectionHistory` received by the history-aware runtime method into every
owned `ResponseFinalizer` call. The prerequisite mediator is the sole append owner: first attempt
count is 0; after checked engine rejection it appends the exact rejected wire,
`ENGINE_DECISION_INVALID`, and nonblank detail; the single retry receives count 1. Curator forwards
the same immutable history unchanged. Do not substitute `RejectionHistory.empty()` inside a bot or
wrapper, and do not add a numeric-id map, bot field, wrapper-local pending value, or `ThreadLocal`.

A checked engine rejection after an `ACCEPTED` finalization remains a hard-stop fixture signal that
the proposed intent did not match the engine contract. The existing single retry still executes so
the runtime contract is proven: the first attempt commits no owner mutation and emits a rejection
trace, while the retry finalizer stamps `priorRejectionCount == 1`.

### Exact Mutation Owner

The adapted accepted result explicitly carries mutation mode `OUTER_COMMON` plus the finalizer's immutable
`TrackerMutationRequest`. For mediator calls, the prerequisite acceptance callback is the sole
owner of:

1. applying that exact outer tracker response once
2. running the existing `trackStrategicEvents` once
3. recording accepted finalizer and trace evidence
4. closing and emitting the trace once

The generic legacy outer tracker path must not run in addition to the descriptor. Rejection applies
no outer tracker or strategic mutation and closes one rejection trace. Direct `decide()` callers
retain the prerequisite's compatibility behavior: the same accepted descriptor and outer common
mutation apply once inline before the wire response returns.

`LEGACY_UNOWNED` remains on the prerequisite's legacy raw-string result path, including its existing
emergency, safety, tracker, strategic-event, and trace behavior, except that universal INTEGER capture
by `ForceActivationEvaluator` is intentionally removed. No phase owner calls the finalizer for an
unowned route. Before release, inventory every stamped and unstamped unowned INTEGER family reachable
from source or the frozen fixture corpus. For each family, record the pre-cutover route/wire and the
post-cutover route/wire, and prove that only removal of ForceActivation ownership explains any delta.
Do not claim byte-identical output for this intentionally changed family.

## 8. Legacy Retirement

Delete only the branches replaced and proven by this phase:

- universal INTEGER ownership in both `ForceActivationEvaluator` classes
- duplicated amount arithmetic after the shared policy is active
- legacy V38.3 zero-confirmation branch in both `ActionTextEvaluator` classes
- duplicated inline CONTROL drain helper after operation-by-operation parity is proven
- comments and imports made stale by those deletions

Do not leave commented copies. Do not delete unrelated dormant code or historical changelog
entries. The top-level ACTIVATE and CONTROL scoring remains in existing evaluators because the new
owners deliberately delegate to `CombinedEvaluator`.

## 9. Frozen Fixture Matrix

Extend the existing shared decide-equivalent harness and two thin bot adapters. Keep tests pure and
stub-backed. Explicitly exclude these two untracked files and record their hashes in the release-time
dirty baseline:

- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/finalization/ActivateControlOwnerResponseTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/ActivateControlLegacyBehaviorTest.java`

Only the second file imports `VirtualTableScenario`; do not describe both as scenario fixtures.

Required cases:

- each of the six stamped routes is owned; absent, unknown, wrong-phase, and wrong-shape origins
  remain legacy; every route fixture supplies a nonblank current-turn player and proves the adapter
  reads it once without any other route-selection game-state query
- dispatch occurs after the chaos-gate draw and does not change RNG count
- ACTIVATE and CONTROL top-level response, candidate order, merge order, raw score operations,
  pre-safety winner, and final response remain exact; only trace route changes intentionally
- amount boundaries cover raw min/max, keep-three, low-life keep-two, minimum-one, and clamp order
- allowance uses raw maximum when recipient differs from current turn player
- zero confirmation covers normal `Yes,No`, normal `No,Yes`, V61c in both permutations, and
  malformed/duplicate labels
- acknowledgement covers sole `OK`, missing `OK`, duplicate `OK`, and extra labels
- acknowledgement performs the legacy context-preparation mutation stream exactly once and performs
  zero evaluator scoring
- zero confirmation crosses reserve sizes 3 and 4 with battle-plausible true and false for both bots
- every inventoried unrelated stamped and unstamped INTEGER family bypasses ForceActivation, with its
  exact pre/post route and wire recorded as an intentional-delta fixture
- CONTROL drain covers every terminal step, every nonterminal continuation, query failure at V189,
  V104 suppression boundary, multi-site thresholds, Hunt Down ordering, and raw float bits
- Rando and ChosenOne traces remain equal after identity normalization
- `TraceSession.isActive()` is false after every decision
- trace-disabled and trace-enabled wire responses remain identical
- no owned rejected result falls through to a second evaluator
- every legal owned intent finalizes as `ACCEPTED` with zero corrections, no forced choice, and no
  RNG draw
- a missing owned snapshot or unknown contract fact produces one typed pre-engine rejection
- checked engine rejection followed by retry produces two traces and zero accepted mutation for the
  rejected attempt; owned finalizer `priorRejectionCount` is 0 then 1, and retry history contains the
  exact first wire plus `ENGINE_DECISION_INVALID`
- Curator forwarding preserves the same immutable history and finalizer count

Document intentional deltas explicitly:

- zero-confirmation normal skip changes from legacy ordinal `0`/`Yes` to the original ordinal of
  `No`
- owned route trace labels change from generic legacy routes to typed ACTIVATE/CONTROL routes
- universal INTEGER capture is removed

No other response, score, operation, candidate-order, RNG, tracker, or fallback delta is allowed. Any
unowned INTEGER delta not captured in the release-time inventory is a hard stop.

## 10. One Verification Pass

After every edit and fixture is complete, run one focused Maven pass containing:

- both ACTIVATE/CONTROL decide harness adapters
- all new owner, amount-policy, drain-assessment, route, finalizer-adapter, and trace-route tests
- both `CombinedEvaluatorTraceTest` classes
- `RandoCalAiTraceHookTest`
- `TheChosenOneAiTraceHookTest`
- existing `ResponseFinalizer` tests
- existing mediator retry tests if production finalizer wiring touches mediator behavior

Use `-Dsurefire.failIfNoSpecifiedTests=false -DskipITs`. Do not run a package wildcard, full module,
`VirtualTableScenario`, game, sandbox, deploy, or live reload.

Then run these static gates:

- `git diff --check`
- exact changed-path proof against baseline
- normalized Rando/ChosenOne parity for every mirrored production and test adapter
- search proof that replaced amount, zero-confirmation, and drain policy bodies have one live owner
- search proof that unrelated INTEGER decisions cannot reach ForceActivation
- search proof that the resolver has exactly the two mirrored production consumers
- source/fixture proof that mediator-facing `OWNED_INTENT` calls `ResponseFinalizer` exactly once and
  `OWNED_REJECTED` calls it zero times, while the direct compatibility path preserves its frozen mapping
- search proof that no mediator-facing owned route enters emergency fallback, mirrored `DecisionSafety`,
  or heuristic fallback after producing its owner result
- search proof that no owner-side tracker mutation precedes engine acceptance
- search proof that the finalizer tracker descriptor is the one accepted outer tracker record, not a
  second record beside the generic path
- source proof that one `TraceSnapshots.Result` instance feeds both trace opening and finalization
  when capture is enabled, and is built once lazily for an owned route when capture is disabled
- fixture proof that all legal owned finalizations are `ACCEPTED` and consume zero RNG draws
- source and fixture proof that owned finalization receives the runtime history, stamps counts 0 then
  1 across rejection/retry, and creates no map, bot field, wrapper-local value, or `ThreadLocal`
- production trace default remains `NoOpTraceSink.INSTANCE`
- exact hash/status proof that both named excluded untracked test files remain unstaged and unchanged
- source proof that the post-pilot adapter uses explicit `OUTER_COMMON` for this phase, preserves the
  pilot's `NONE` route, and supersedes the pilot's one-phase-owner proof without changing that route
- changelog and version-history entries identify the intentional deltas and exact revert boundary

Create one phase commit only after every gate passes. Return the SHA, changed paths, focused test
counts, static proof, excluded-file status, and documented intentional deltas to Codex for one
independent aggregate gate.

## 11. Hard Stops

Stop the whole phase and report exact evidence if any of these occurs:

- finalizer rejection cannot cross the raw-string bot boundary visibly and without duplicate
  mutation
- the committed runtime or V44/V67j pilot prerequisite differs from section 7's typed result,
  explicit mutation-mode, or disposition contract
- an owned route cannot reuse/build exactly one immutable snapshot without adding behavior reads
- a legal owned intent finalizes as `CORRECTED` or `FORCED`, or consumes an RNG draw
- an owned finalizer cannot receive the runtime's immutable rejection history unchanged
- route dispatch requires moving or adding the chaos RNG draw
- an owned route needs legacy heuristic fallback to produce a valid response
- an unowned INTEGER family is missing from the frozen pre/post route-and-wire inventory
- a top-level owner must evaluate CombinedEvaluator twice
- amount or zero-confirmation requires a new battle-plausibility predicate
- acknowledgement cannot preserve the existing context-preparation mutations without evaluating a
  candidate or running them more than once
- drain consolidation changes engine query order or executes a query skipped by a legacy terminal
  return
- Rando and ChosenOne differ after identity normalization
- a listed invariant can be tested only with `VirtualTableScenario`, a game, or sandbox run
- an unrelated production file must change

## 12. Worker Return

K-2 returns one report after the coherent phase, not progress micro-reports:

- final commit SHA and parent
- exact changed paths
- focused test command and counts
- normalized parity result
- legacy-retirement search result
- finalizer and tracker ownership proof
- owned-route finalizer status and zero-RNG matrix
- snapshot single-build/reuse proof
- retry-history count and exact-wire proof
- intentional delta list
- excluded dirty/untracked file status
- council or Fable aggregate review result, if available
