# Trace Oracle V2 Semantic Gate: 97d2cb65a

Date: 2026-07-13
Reviewer: Codex/Alfred
Implementation owner: K-2/Claude
Reviewed commit: `97d2cb65a`

## Verdict

`HOLD` for fixture capture, executable parity-oracle claims, shadow comparison, and deployment.

The typed envelope, mirrored hooks, raw-versus-merge order, exact float bits, direct-route hooks,
and strict comparator are useful infrastructure and may remain inert on the branch. The commit is
not yet fully conformant with `CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md`.

This gate pauses only the Trace V2 lane. K-2 can continue the independent Batch 2 consistency lane.

## Independent verification

- Commit scope: 32 files, 3,150 insertions, 310 deletions.
- Detached review worktree: commit `97d2cb65a`, not the shared dirty checkout.
- Targeted Maven run passed 40 tests:
  - `DecisionTraceEnvelopeTest`: 10/10.
  - Rando `CombinedEvaluatorTraceTest`: 12/12.
  - ChosenOne `CombinedEvaluatorTraceTest`: 12/12.
  - Rando and ChosenOne bot hook tests: 3/3 each.
- Command:
  `mvn -q -pl gemp-swccg-server -am -Dtest=DecisionTraceEnvelopeTest,RandoCalAiTraceHookTest,TheChosenOneAiTraceHookTest,CombinedEvaluatorTraceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Blocking findings

### P0: the snapshot does not preserve the complete raw decision

The contract requires raw presence, each original length, and each value. The current snapshot
cannot distinguish several inputs that can drive different legacy behavior.

- `TraceSnapshots.Input` omits `preselected`, `autoPassEligible`, `defaultIndex`, `defaultValue`,
  `backSideTestingText`, `horizontal`, `cardText`, `returnAnyChange`, `yourTurn`, `noLongDelay`,
  `revertEligible`, and `timeoutValue`.
- `TraceSnapshots.java:130-132` folds `results` into `actionText`, losing which raw array supplied
  the value.
- `TraceSnapshots.java:128,133-134,258-263` maps blank raw ids to `null`, so the snapshot does not
  preserve the offered value.
- `TraceSnapshots.java:294-299` ignores present empty arrays. A present empty array paired with a
  present non-empty parallel array is not reported as a mismatch.
- `DecisionFacts.CandidateShape` records only action-id and card-id counts. It does not record
  presence or lengths for the other arrays, including `results`.

Required repair: add one immutable raw-decision component to `DecisionSnapshot` that preserves
typed scalar presence and every raw array separately, including present-empty versus absent. Build
normalized action rows from it without replacing the raw evidence.

### P0: record-construction and sink failures still disappear

The contract says every swallowed capture failure becomes a typed failure on an `INCOMPLETE`
envelope.

- `TraceSession.java:61-69` catches a `finish()` failure and returns `null`; no envelope or typed
  failure survives.
- Both bot entry points call `TraceSession.close()` before `decisionTraceSink.accept(trace)`. If
  `accept` throws, the immutable trace is already finalized and the thread-local is gone. The catch
  only calls `abandon()`, so the sink failure cannot be represented.
- `CombinedEvaluatorTraceTest.throwingSinkLeavesWinnerAndThreadStateIntact` proves only gameplay
  continuity and thread cleanup. It does not prove a typed `INCOMPLETE` record.
- `TraceSession.open()` returns `false` after construction failure and emits no failure record.

Required repair: define a safe emission result/failure channel that preserves a fallback envelope
when construction or sink delivery fails. Add focused tests that inspect the typed failure, not only
the winner and thread-local state.

### P0: `COMPLETE` is not route-complete

- `DecisionTrace.java:83-94` requires only status/failure consistency plus non-null snapshot and
  route.
- `TraceCollector.java:250-266` adds failures only for missing route and a missing bot-boundary
  final response.
- `DecisionTraceEnvelopeTest.scriptedRouteAndFinalResponseFlowIsCaptured` declares `COMPLETE`
  without pass eligibility, a pre-safety winner, or any operation.
- Pass eligibility is recorded only inside `CombinedEvaluator`. Direct interceptors and a pure
  heuristic route can therefore be `COMPLETE` with this required finalization fact absent.

Required repair: enforce a route-specific completeness matrix in `finish()`. At minimum, every
route requires pass/cancel facts and a final response; CombinedEvaluator requires a pre-safety
winner and the expected operation/finalization boundary; direct routes explicitly require the
fields they skip to be marked not-applicable rather than silently `null`.

### P1: operation identity remains nullable

- `TraceOperation` accepts null `op`, `evaluatorId`, `ruleId`, `domainId`, and `outputKind` without
  validation.
- Rank/select operations and synthetic Pass construction occur outside an evaluator binding, so
  their `evaluatorId` is null.
- Legacy operations use `TraceRuleId.LEGACY_UNTAGGED` but null domain and output kind, despite the
  contract requiring one explicit legacy-untagged identity rather than null inference.

Required repair: use a mandatory producer id for every operation, including a typed
`COMBINED_EVALUATOR` producer for merge/rank/select. Add explicit legacy sentinels for every identity
dimension or narrow the contract and constructor consistently. Reject incomplete operation identity
at construction.

### P1: route authority is duplicated and not validated

- `DecisionFacts.selectedRoute` is assigned from the engine decision type in
  `TraceSnapshots.java:168-197`.
- `TraceRouteRecord.selected` is independently chosen as the final runtime route observation in
  `TraceCollector.java:237-249`.
- No validator checks the selected execution route against frozen route-selection facts.
- Ordered evidence is stored as free prose. Some routes, notably V79b, depend on game-state/service
  observations that are not frozen in the snapshot before route selection.

Required repair: rename the decision-type value to a shape if it is only a shape. Select one typed
execution route from frozen typed evidence, then validate that route once. Keep fallback history as
ordered evidence, not as competing route selections.

### P1: minimum-corpus stages 4 and 5 remain open

K-2 correctly disclosed these remaining gaps:

- inner mutation observation is not wired at heuristic tracker, strategy refresh, and deploy-plan
  lifecycle choke points;
- there are no `COMPLETE` real `decide()` fixtures over real game state;
- there is no direct cross-bot normalized-envelope fixture;
- chaos fallback has no executable fixture while `CHAOS_PERCENT == 0`.

The comparator also compares `botModel` literally, so cross-bot parity needs an explicit normalized
comparison rule before that gate can pass.

## Required repair fixtures

1. Present-empty versus present-nonempty parallel arrays becomes `INCOMPLETE`.
2. Every engine decision parameter is preserved separately with absent/present-empty distinction.
3. A CombinedEvaluator trace missing pass eligibility or pre-safety winner is rejected.
4. A direct route records pass eligibility and explicit not-applicable finalization fields.
5. Record construction failure produces an inspectable typed failure envelope.
6. Sink acceptance failure produces an inspectable typed failure through the chosen failure channel.
7. Null producer/rule/domain/output identity is rejected or represented by explicit sentinels.
8. A selected route inconsistent with frozen evidence is rejected.
9. Rando and ChosenOne compare equal after only declared personality normalization.
10. Inner tracker/refresh/deploy-plan events are observed once and never applied twice.

## Safe claim after this commit

`97d2cb65a` is a substantial inert Trace V2 infrastructure increment with 40 independently green
focused tests. It is not yet a complete parity oracle and must not authorize owner cutover or deploy.
