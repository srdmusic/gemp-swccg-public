# Trace Stage 4B1: Heuristic Memory Preflight

Date: 2026-07-13
Architecture/review owner: Codex/Alfred
Java implementation owner: K-2/Claude small agent after 4A2b advances
Audit base and required implementation parent: `7098f9b33`
Status: `RELEASED FOR JAVA` after independent Codex and K-2 source/council confirmation
Deployment: held
Push: held

## Scope

This increment observes only retained memory owned directly by `HeuristicAiBase`. Its private
Rando `DecisionTracker` is the separate 4A2b owner and is excluded. Primary evaluator and direct
interceptor routes that do not call `super.decide(...)` must produce no heuristic-memory event.

No behavior repair, owner consolidation, reset invention, capture enablement, or score change is
authorized.

The inherited heuristic memory has no source-level new-game reset. Failed-search sets, the last
action tuple, and last-decision fields remain on a reused controller. State-hash changes clear only
local/recent response memory and repeat count; turn rollback clears only the two reassignment maps.
Normal Hall construction creates a fresh AI controller per game, so this is a controller-reuse
hazard and an observed retention rule, not evidence of Hall cross-game leakage. This packet records
the current retention exactly and does not invent a reset.

## Source-Complete Owner Table

| Boundary | Exact mutations at `7098f9b33` | Required payload and folding |
|---|---|---|
| `STATE_UPDATE` | `currentTurnNumber`; turn-rollback clears of both reassignment maps; pruning of `recentReassignmentTurns` only; `currentStateHash`; state-hash clears of local blocks and recent responses; `lastDecisionRepeatCount`; `blockStateHash` | Exact five state-read values, before/after complete heuristic snapshot, exact pruned rows. The nested shared `decisionTracker.updateState(...)` is 4A2b-owned and remains a separate event. |
| `ACTION_CHOICE_REMEMBER` | `lastActionChoiceText`, `lastActionChoiceCardId`, `lastActionChoiceBlueprintId` | Exact decision type/result/index and prior/new tuple. Sentinel blueprint values `inplay` and `rules` become empty strings exactly as legacy code does. |
| `FAILED_SEARCH_ADD` | membership additions to failed action-text, card-id, and blueprint-id sets | Exact prior action tuple and complete sorted set deltas. There are memberships only, no counters. Repeated additions are `NO_OP`. |
| `SINGLE_RESPONSE_RECORD` | repeat count, last decision key/response/state hash, plus any internally created local-response block | Exact decision/raw/tracking response and complete before/after snapshot. The internal local block is folded into this one external boundary event. It is not a seventh top-level mutator event. |
| `RECENT_RESPONSE_APPEND` | map-entry creation, ordered deque append, and FIFO eviction after six entries | Exact decision key, appended response, evicted rows, and ordered deque before/after. |
| `REASSIGNMENT_RECORD` | `recentReassignmentTurns` write plus `reassignmentCounts` increment | Closed key variant and value, turn, and both map deltas. Key precedence is card, non-sentinel blueprint, then extracted text. |

## Exact Reachability and Order

Within one successful heuristic fallback decision:

1. 4A2b shared phase/update boundaries run first.
2. Response selection and 4A2b explicit cancel block run.
3. Safety validation completes.
4. The action-choice detail helper is invoked.
5. The failed-search helper is invoked.
6. The single-response helper is invoked and may internally add local blocks.
7. The recent-response helper is invoked.
8. The reassignment helper is invoked.
9. 4A2b shared `RECORD_RESPONSE` runs last.

The five post-selection helpers are called unconditionally, but their internal guards determine
whether a write boundary is reached. An early guard return emits no event. Once an assignment,
`add`, `put`, deque append, or folded local-block/count mutation is executed, emit exactly one owner
event even when canonical before/after snapshots are equal; equality is a real `NO_OP`, not event
suppression. The same rule applies to `STATE_UPDATE`: a helper return before any owned write emits
no heuristic-memory event, while an executed write boundary emits one event.

`STATE_UPDATE` returns without any direct memory write when game/player is null or a game-state
getter throws. A successful helper call writes turn/pruning state before the nested 4A2b shared
update and state-hash memory after it. If 4B1 records one owner event at helper exit, trace order is
therefore shared `UPDATE_STATE` followed by the completed heuristic `STATE_UPDATE` summary.

## Matrix Corrections

The 4A0 matrix is corrected alongside this packet with these source findings:

- `STATE_UPDATE` includes direct `currentTurnNumber` and `currentStateHash` writes.
- Normal advance prunes only `recentReassignmentTurns`; `reassignmentCounts` persists until turn
  rollback.
- Failed-search memory has set membership only, not a count.
- Recent-response memory includes map creation and ordered six-item FIFO eviction.
- Local response blocking is internal to `updateSingleDecisionLoop(...)`; it must be folded into
  `SINGLE_RESPONSE_RECORD`, not emitted as a duplicate top-level mutator event.
- Reassignment recording mutates two maps and uses card, blueprint, then text key precedence.
- There is no reachable heuristic-memory clear/reset owner in this class.
- New-game handling clears only the separate outer tracker. Reused-controller heuristic memory
  retains failed-search sets, action tuple, and last-decision fields; record this current behavior
  without claiming normal Hall leakage or adding a reset.

This changes the provisional seven-row matrix into six external boundary events. K-2 source review
and one narrow council check must confirm that folding before implementation.

## Snapshot Boundary

Use one immutable, canonical `HeuristicMemorySnapshot` for this owner. It must contain:

- `currentStateHash`, `blockStateHash`, `lastDecisionStateHash`, `lastDecisionKey`,
  `lastDecisionResponse`, `lastDecisionRepeatCount`, and `currentTurnNumber`;
- `lastActionChoiceText`, `lastActionChoiceCardId`, and `lastActionChoiceBlueprintId`;
- `failedSearchActionTexts`, `failedSearchCardIds`, and `failedSearchBlueprintIds`;
- `localBlockedResponses` with sorted map keys and sorted values inside every set;
- `recentDecisionResponses` with sorted map keys and insertion-ordered deque values;
- `recentReassignmentTurns` and `reassignmentCounts` with sorted keys.

All failed-search sets are sorted. Every outer map is canonicalized by sorted key. Values from
`HashSet` owners are sorted; deque values preserve insertion order. Reassignment maps use sorted
keys. No snapshot order may depend on `HashMap` or `HashSet` iteration.

Do not include the private `DecisionTracker`, static keyword tables, transient method locals, or
game/service references. Do not add a public field or reflection seam. A pure protected or
package-visible snapshot accessor used only under the active trace guard is sufficient.

## Highest-Risk Fixtures

- Fallback emits exact heuristic event order; primary evaluator and direct interceptor routes emit
  none.
- Turn advance prunes only expired recent-turn rows; turn rollback clears both reassignment maps.
- State-hash change clears local/recent response memory and repeat count but not failed-search sets.
- Exact unsuccessful reserve verification adds all available prior-action identities; repeat is
  `NO_OP`; mismatch adds nothing. A throwing `getReserveDeck(...)` read is uncaught legacy behavior:
  the fixture must assert exception propagation plus no failed-search event/state change, not an
  unchanged completed response.
- Two identical non-pass decisions exercise repeat and folded local-block transitions. Seven recent
  responses prove six-entry FIFO order.
- Reassignment fixtures prove card over blueprint over text precedence and every guard/no-op path.
- A prepared test `TraceCollector` whose `recordStateEvent(...)` throws proves active-session
  append failure marks `INCOMPLETE/STATE_EVENT` without changing response or legacy state. A
  throwing final `TraceSink` is a different failure boundary. Capture remains disabled by default.
- Update both eight-event fixture pairs per bot. `sharedTrackerEventsFollowTheFallbackRouteInExactSourceOrder`
  and `emptyCardSelectionFallbackRecordsTheSharedBlockResponseInOrder` each gain heuristic
  `STATE_UPDATE` immediately after shared `UPDATE_STATE` and `SINGLE_RESPONSE_RECORD` immediately
  before shared `RECORD_RESPONSE`, becoming ten. The two injected-append fixtures attempt two new
  appends and become nine; their same-game follow-up streams become eight. The
  `failedGameStateReadSuppressesOnlyTheSharedUpdateStateEvent` ThrowingHand fixture remains seven
  because the failed state read suppresses `STATE_UPDATE` and the empty state hash makes the
  single-response helper return before any owned write. Other fixture cardinalities must change
  only for owner boundaries their source path actually reaches.

## Independent Codex Checks

- A read-only source audit found six external owners and no omitted or duplicate write owner after
  folding the local block and reassignment count into their enclosing helper boundaries. It also
  confirmed the helper call order, guard suppression, reserve-deck throw behavior, and the current
  reused-controller retention boundary.
- A direct local council review using `deepseek-r1:70b-llama-distill-q8_0` with four threads returned
  `PASS`: it found distinct ownership, deterministic snapshots, intentional retention, and no
  concrete reason to hold the corrected model.
- These checks do not replace K-2's independently dispatched source agent and council confirmation.
- K-2 reported `AGREE` in `m00473`: its read-only source agent verified the six-owner bijection,
  all folds, full-state canonicalization list, reserve-read exception boundary, empty-key executed
  writes, and controller-reuse retention; its independent council raised no factual objection.
- Current HEAD `9a84d3a5f` is an allowed documentation-only descendant: the packet's base-gate Java
  and test diff against audited parent `7098f9b33` exits zero; the only committed descendant path is
  K-2's continuity handoff.

## Release Review

Stage 4A2b independently advanced through exact parent `7098f9b33` in
`Handoffs/CODEX_TRACE_STAGE4_4A2B_GATE_7098F9B33_2026-07-13.md` (`m00469`). Codex's source/council
checks and K-2's independently dispatched source/council checks now confirm the corrected
six-boundary model and folding. Java implementation is released under the verbatim brief below.
Strategy, objective, deck, shield, deploy-plan, evaluator, finalizer, engine, cutover, deployment,
and push remain out of scope.

## Agent-Ready Implementation Brief

K-2 may dispatch the following prompt verbatim now.

```text
You own Trace Stage 4B1, heuristic-memory observation only. You are not alone in the repository.
Preserve all existing user, K-2, and Codex edits. Do not revert, reformat, or clean unrelated work.
Read this entire packet before editing:
Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md

BASE GATE
1. The audited production baseline is 7098f9b33. A later checkout is allowed only when
   `git diff --exit-code 7098f9b33..HEAD -- src/gemp-swccg-server/src/main/java src/gemp-swccg-server/src/test/java`
   is empty. If it is not empty, stop and report the exact paths. Do not merge competing Java.
2. Capture must remain disabled by default. Do not deploy, push, or commit. K-2 owns verification
   and the isolated commit.

WRITE OWNERSHIP
- src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/HeuristicAiBase.java
- src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/trace/TraceSession.java
- only the 4B1 snapshot/event records plus the sealed permits change under
  src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/trace/state/
- focused 4B1 tests and existing mirrored bot trace-hook fixtures under
  src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/
Do not edit scoring, evaluators, strategy, objectives, deck logic, deploy logic, finalizers,
engine behavior, card Java, or unrelated trace families.

IMPLEMENTATION CONTRACT
1. Add one immutable canonical HeuristicMemorySnapshot containing exactly the fields listed in
   Snapshot Boundary. Freeze every collection defensively. Sort failed-search sets, outer map
   keys, HashSet-backed map values, and both reassignment maps. Preserve deque insertion order.
2. Add exactly six closed typed TraceStateEvent families from Source-Complete Owner Table:
   STATE_UPDATE, ACTION_CHOICE_REMEMBER, FAILED_SEARCH_ADD, SINGLE_RESPONSE_RECORD,
   RECENT_RESPONSE_APPEND, and REASSIGNMENT_RECORD. No seventh local-block or nested-count event.
3. Add narrow TraceSession record methods following the existing 4A2b error law. With no active
   session they immediately return. Snapshot/event construction failure must mark
   INCOMPLETE/STATE_EVENT and must never alter or throw into the legacy decision path.
4. Instrument the six real HeuristicAiBase owner boundaries only. Capture before/after snapshots
   only when TraceSession.isActive(). Never invoke a legacy mutator twice. Never reorder a legacy
   read, write, helper call, or return. A guard return before any owned write emits no event. Once
   an assignment, add, put, deque append, or folded local-block/count mutation executes, emit one
   event even if canonical snapshots compare equal. That event has NO_OP outcome.
5. Preserve the exact event order in Exact Reachability and Order. Shared 4A2b tracker events stay
   separate. Local response blocking stays folded into SINGLE_RESPONSE_RECORD. Reassignment count
   stays folded into REASSIGNMENT_RECORD.
6. Do not invent a reset. Preserve the reused-controller retention behavior exactly. Preserve the
   uncaught reserve-deck getter exception exactly.

REQUIRED TESTS
1. Common state tests: all six event constructors, closed identities, defensive immutability,
   canonical equality, CHANGED/NO_OP outcomes, rejected null/impossible inputs, and deterministic
   ordering from deliberately shuffled HashMap/HashSet inputs.
2. Focused owner fixtures: every changed path, every early-guard suppression path, executed-write
   NO_OP paths, turn advance pruning, turn rollback, state-hash reset scope, reserve verification
   mismatch/repeat/throw behavior, local-block folding, six-entry recent-response FIFO, and
   reassignment key precedence plus both map deltas.
3. Mirrored bot fixtures: in both Rando and ChosenOne,
   sharedTrackerEventsFollowTheFallbackRouteInExactSourceOrder and
   emptyCardSelectionFallbackRecordsTheSharedBlockResponseInOrder change 8 to 10;
   injectedAppendFailureNeverSkipsOrRepeatsTheLegacyMutators and
   injectedAppendFailureCoversTheDirectBlockResponseBoundary change 7 attempted appends to 9 and
   their same-game streams change 6 to 8; failedGameStateReadSuppressesOnlyTheSharedUpdateStateEvent
   stays 7. Primary evaluator and direct interceptor routes must show no heuristic-memory events.
   Normalize the mirrored files and prove parity after package/class/owner substitutions.
4. Failure injection: a prepared collector whose state-event append throws must produce
   INCOMPLETE/STATE_EVENT while returning the same response and leaving legacy state identical.
5. Re-run the focused suite from the handoff plus every new 4B1 test, then run
   `mvn -pl gemp-swccg-server -am package -DskipTests -DskipITs -q`.

REPORT TO K-2
- exact changed paths
- source line numbers for all six hooks
- focused test classes, total tests, failures, errors, skips
- package exit code
- normalized Rando/ChosenOne parity result
- `git diff --check` result
- proof that capture remains NoOpTraceSink-disabled
- any behavior or bytecode delta outside trace guards, which is an automatic HOLD
```
