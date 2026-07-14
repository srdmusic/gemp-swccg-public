# Codex Gate: Trace Stage 4B1 Heuristic Memory

Date: 2026-07-13
Observed HEAD: `2eb105a4a`
Implementation commit: `ec886934b`
Implementation parent: `9a84d3a5f`
Packet: `Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md`
Verdict: `ADVANCE-INERT`
Deployment: held
Push: held

## Scope

The implementation is confined to observation of memory already owned by `HeuristicAiBase`, six
closed trace-event families, the canonical heuristic snapshot, `TraceSession` recording choke
points, focused tests, mirrored bot fixtures, and required changelog/handoff bookkeeping. It does
not change scoring, evaluators, strategy, objectives, deck logic, deploy logic, finalizers, engine
behavior, routing authority, schema version, capture defaults, or production enablement.

## Contract Review

- Exactly six external heuristic-memory families exist: `STATE_UPDATE`,
  `ACTION_CHOICE_REMEMBER`, `FAILED_SEARCH_ADD`, `SINGLE_RESPONSE_RECORD`,
  `RECENT_RESPONSE_APPEND`, and `REASSIGNMENT_RECORD`.
- Local response blocking remains folded into `SINGLE_RESPONSE_RECORD`; reassignment count remains
  folded into `REASSIGNMENT_RECORD`. There is no seventh family for either nested mutation.
- Every before-snapshot is captured after the real helper guards and before its first owned write.
  Every after-snapshot and event append occurs after the legacy writes. Snapshot construction and
  append failures are contained as `INCOMPLETE/STATE_EVENT` capture failures.
- The legacy mutators are not duplicated or reordered. The `HeuristicAiBase` production diff has
  206 insertions and zero deletions: one import, guarded trace observation blocks, and the pure
  private snapshot accessor.
- A successful fallback preserves the required order: shared phase/update, heuristic state
  summary, action memory, failed-search memory when reached, single response, recent response,
  reassignment, then shared record response.
- A guard return emits no owner event. An executed write emits one event even when canonical
  snapshots are equal and the outcome is `NO_OP`.
- The reserve-deck getter throw remains outside capture and propagates exactly as legacy behavior.
- Turn advance prunes only expired `recentReassignmentTurns`; turn rollback clears both
  reassignment maps. State-hash change clears local/recent response memory and repeat count only.
  No reset was invented for retained failed-search, action-choice, or last-decision memory.
- `HeuristicMemorySnapshot` freezes every collection, sorts failed-search sets and all map keys,
  sorts set-backed values, and preserves deque insertion order.
- Both bot controllers still initialize `decisionTraceSink` to `NoOpTraceSink.INSTANCE`. Capture
  remains disabled by default.

## Hook Evidence

- `RECENT_RESPONSE_APPEND`: `HeuristicAiBase.java:765-793`.
- `REASSIGNMENT_RECORD`: `HeuristicAiBase.java:825-846`.
- `SINGLE_RESPONSE_RECORD`: `HeuristicAiBase.java:951-1035`.
- `STATE_UPDATE`: `HeuristicAiBase.java:1242-1309`.
- `FAILED_SEARCH_ADD`: `HeuristicAiBase.java:1340-1374`.
- `ACTION_CHOICE_REMEMBER`: `HeuristicAiBase.java:1440-1467`.

## Independent Evidence

- Detached worktree at exact commit `ec886934b`.
- `mvn -q -pl gemp-swccg-server -am test -DskipITs`: exit 0.
- Surefire: 116 reports, 842 tests, 0 failures, 0 errors, 26 expected skips.
- `mvn -q -pl gemp-swccg-server -am package -DskipTests -DskipITs`: exit 0.
- Normalized `RandoCalAiTraceHookTest` and `TheChosenOneAiTraceHookTest`: byte-for-byte identical
  after package, class, controller, and owner substitutions.
- Required mirrored cardinalities are pinned in both files: 10-event fallback pairs, 9 attempted
  append failures, 8-event same-game follow-ups, and the 7-event ThrowingHand stream.
- `git diff --check 9a84d3a5f..ec886934b`: clean.
- Production Java scope contains no deletions outside the sealed `TraceStateEvent` permit/comment
  extension. The owner class itself contains no legacy deletion or replacement.
- Source review found no behavior delta outside active-session trace guards.

## Gate Result

Commit `ec886934b` advances as inert trace infrastructure. This verdict does not authorize capture
enablement, behavior repair, owner consolidation, strategy/controller observation, cutover,
deployment, or push. Stage 4B2 remains a separate packet and gate.
