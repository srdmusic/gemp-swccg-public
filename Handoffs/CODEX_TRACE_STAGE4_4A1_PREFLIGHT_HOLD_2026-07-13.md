# Trace Stage 4A1 pre-commit hold

Date: 2026-07-13
Baseline: `13db1dfde`
Owner: Codex architecture and independent gate; K-2 production Java
Verdict: partial implementation `REJECTED`; amended complete-snapshot 4A1 `ADVANCE`
Resolution: K-2 source proof `m00373`, council objection reconciled in `m00374`, Codex acceptance
`m00375`/`m00376`

## Blocking finding 1: partial tracker payload

The accepted 4A0 matrix requires `RECORD_RESPONSE` to carry the call subject and complete
decision-affecting tracker state before and after:

- ordered sequence rows;
- last-action pair;
- cancel key and count;
- repeat and loop counts;
- sorted turn-block rows;
- `MutationOutcome.CHANGED` or `MutationOutcome.NO_OP`.

The in-flight `TrackerRecordResponseEvent` intentionally carries only the subject and sequence
repeat counts, omits `MutationOutcome`, and defers the remaining owner state to 4A2. That is not a
mechanical implementation of the accepted row. It cannot prove replay or owner mutation.

Both outer `DecisionTracker` classes currently expose no pure immutable snapshot API. A complete
4A1 tracker event therefore requires new snapshot seams in both package-local tracker classes. That
would expand 4A1 into the tracker-owner increment that the landing order assigns to 4A2.

## Blocking finding 2: hook cardinality

The phrase `exactly three current outer sites per bot` counts the three superseded prose trace hooks.
It is not the cardinality of the new direct-write instrumentation.

The 4A1 direct-write mapping requires ten lexical hook locations per bot:

| Owner method | Required lexical hooks per bot |
|---|---:|
| `decide(...)` pending-concede set | 1 |
| `decide(...)` outer tracker record | 1 if retained in 4A1 |
| `trackGameState(...)` new-game pending clear | 1 |
| `trackGameState(...)` pending-deploy clear | 1 |
| `trackGameState(...)` `playerLost(...)` outcome | 1 |
| `trackGameState(...)` post-call pending clear | 1 |
| `trackStrategicEvents(...)` mutually exclusive deploy-type sets | 4 |

The player-lost event must precede the pending clear after both success and caught `Exception`.
An escaping `Error` skips both because the legacy clear is not reached. Null `currentGame` emits
neither event and leaves pending state intact.

## Reconciled amendment

The original Option B recommendation deferred `RECORD_RESPONSE` to 4A2. K-2 correctly identified
that this would remove the only tracker observation during the increments intended to expose
dual-tracker cross-talk. The council preferred B only under the original partial-payload framing;
its false-replay objection is eliminated by the complete snapshot below.

The accepted 4A1 boundary is:

1. Both package-local outer `DecisionTracker` classes add only pure package-local
   `traceSnapshot()` and `traceDecisionKey(...)` accessors. No mutator changes.
2. `DecisionTrackerSnapshot` contains ordered `TrackerSequenceRow(decisionKey,response,stateHash)`
   rows, repeat count, detected loop length, last-action key/response, consecutive-cancel key/count,
   and canonical sorted `TrackerTurnBlockRow(decisionKey,sortedResponses)` rows.
3. `TrackerRecordResponseEvent` contains `TrackerOwner.OUTER_RANDO` or `.OUTER_CHOSENONE`, exact
   decision type/id/key/response, complete before/after snapshots, and constructor-validated
   `MutationOutcome` derived from snapshot equality.
4. `history` remains excluded as diagnostic. `blockedResponses` remains excluded because it has no
   live population path. `lastTurn`, `lastPhase`, and scalar `lastStateHash` remain excluded because
   `updateState`/`onPhaseChange` own them; the state hash read by `recordDecision` is retained inside
   each appended sequence row.
5. 4A1 also replaces the pending-concede, `PLAYER_LOST`, and pending-deploy prose observations with
   completed typed events at all ten lexical hook locations per bot.
6. 4A2 adds the remaining outer and inherited tracker operations using this complete snapshot model.

`PendingConcedeEvent.Cause.POST_PLAYER_LOST` is the required clear cause. `POST_FIRE_CLEAR` is
rejected because no `FIRE_PENDING` state operation exists.

## Capture-disabled invariants

- Production defaults remain `NoOpTraceSink.INSTANCE`; no runtime enablement is added.
- Disabled capture constructs no state event, snapshot, canonical row, or session.
- Every legacy write and engine call executes exactly once and in unchanged order.
- Hooks consume already-computed values and never invoke a mutator, strategy call, analyzer, or RNG
  method again.
- Active capture failure marks `INCOMPLETE/STATE_EVENT` without changing legacy state.
- Wrapper-level `trackStrategicEvents(...)`, `onSuccessfulDeploy(...)`, and
  `onBattleResult(...)` observations are absent from 4A1.

## Gate boundary

Cleanup 2.5 remains independent and may proceed. F1/F2, enabled capture, tracker consolidation,
behavior repair, owner retirement, phase cutover, deployment, and push remain held.
