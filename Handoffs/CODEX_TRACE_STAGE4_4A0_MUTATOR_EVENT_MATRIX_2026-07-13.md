# Trace Stage 4A0 mutator-to-event matrix

Date: 2026-07-13
Audit baseline: `46e62f4dc`
Owner: Codex architecture and gate; K-2 production Java
Status: `ADVANCE` for the narrow, capture-disabled 4A1 slice; later families remain `HOLD`

## Verdict

The earlier 13-family event algebra is superseded. It named operations and identities that do not
exist in source, merged unrelated owners, omitted live writes, and promoted candidate-local MOVE
scratch into cross-decision state.

K-2 source verification and council review accepted the structure in `m00363` after the exact
corrections incorporated below. `ADVANCE` only the narrow 4A1 type-and-hook slice. F1/F2 still gate
runtime finalizer/interceptor work, but they do not gate this inert, capture-disabled observation
slice.

## Incorporated review decisions

- Lifetime and ownership jointly decide the trace lane. State owned across decisions is a state
  event. State confined to one candidate evaluation is an operation trace.
- One reachable owner mutator produces at most one owner event. Internal field and collection deltas
  are payload, not extra top-level events.
- Owner-specific before/after snapshots define `CHANGED` and `NO_OP`. There is no universal rule that
  a clear must produce null or absence; real owners clear to empty collections, zero, empty strings,
  and sentinel values.
- `ShieldStrategy` has its own family. Outer dedupe sets, shield state, and opponent destiny state are
  three different owners.
- `StrategyController` lifecycle operations and battle-coupled operations are separate families.
- `pendingMoveCardIds` and `pendingDeployCardIds` are documented tombstones. Their per-turn
  `lastTurnNumber` latches are live but decision-inert. Any future population is a schema-review
  trigger.
- Engine call outcome is a distinct closed type. It is not a value in `MutationOutcome`; call
  success does not prove that engine state changed.
- When a public-call summary and a direct field write could both claim one mutation, the direct
  write wins. The summary payload must not include the same delta.
- Families land hooks-first beside their first real source owner. No complete type algebra lands
  ahead of reachable hooks.
- No detail string supplies owner, operation, subject identity, or transition value.
- The phrase `three current outer sites` counts the three superseded prose hooks. The completed
  direct-write mapping uses ten lexical hook locations per bot.
- `RECORD_RESPONSE` lands in 4A1 only with complete before/after outer-tracker snapshots. A partial
  repeat-count event is forbidden. `history`, the unpopulated permanent block map, and fields owned
  by `updateState`/`onPhaseChange` remain outside this event.

## Bijection rule

Every reachable mutation of retained, decision-affecting legacy state must map exactly once to one
row below or to an explicit exclusion. A public owner call summarizes its internal changes except
for any direct write already mapped to its own row. Direct-write ownership wins that tiebreaker. A
hook records already-computed values after the legacy write or call; it never invokes the mutator
again.

## Outer bot owner

| Source operation | Retained state | Event operation | Required evidence |
|---|---|---|---|
| new-game direct writes | `lastTurn`, `lastPhase`, `mySide`, `opponentName` | `OUTER_GAME_RESET` | old/new side and opponent, turn and phase before/after |
| turn transition write | `lastTurn` | `TURN_SET` | turn before/after |
| phase tail write | `lastPhase` | `PHASE_SET` | phase before/after |
| lost-pile threshold write | `pendingConcede`, reason | `SET_PENDING` | player, closed cause, lost-pile inputs, before/after |
| new-game or post-call clear | `pendingConcede`, reason | `CLEAR_PENDING` | closed clear cause, before/after |
| `playerLost(...)` call | engine-call attempt | `PLAYER_LOST` | player, `GameEndReason`, distinct `EngineCallOutcome.SUCCESS` or `.THREW` |
| new-game seen-set clears | outer opponent-card and own-shield dedupe sets | `SEEN_GATE_CLEAR` | owner-specific canonical set before/after |
| newly observed opponent card | outer opponent-card dedupe set | `OPPONENT_CARD_ADD` | physical-card/card key and membership before/after |
| newly observed own shield | outer own-shield dedupe set | `OWN_SHIELD_ADD` | physical-card/blueprint key and membership before/after |
| deploy branch direct write | `lastPendingDeployType` | `PENDING_DEPLOY_SET` | exact legacy value before/after |
| next-turn direct clear | `lastPendingDeployType` | `PENDING_DEPLOY_CLEAR` | before/after |

There is no `FIRE_PENDING` state mutation. The real sequence is engine-call attempt followed by
pending-state clear when `playerLost(...)` returns or throws a caught `Exception`. The event order
must therefore be `PLAYER_LOST(SUCCESS|THREW)`, then `CLEAR_PENDING`. The catch is not `Throwable`;
an escaping `Error` skips the clear. Engine `playerLost(...)` is internally idempotent, so
`SUCCESS` records call outcome and does not prove an engine-state transition. A null `currentGame`
does not clear pending state; it can persist until a later battle-end attempt.

`currentGame` is bound before the trace opens and is a frozen decision input, not a state event.
The chaos `Random` state is cross-decision and decision-affecting through `shouldApplyChaos()`.
4A1 does not mutate or serialize it; exact replay must freeze its seed and draw position or record a
closed RNG input at each chaos draw. Chat flags, message pacing, `currentGameId`, personality state,
DAO state, and timestamps remain out of the decision-state corpus.

## Decision tracker owners

The outer Rando tracker, outer ChosenOne tracker, and inherited heuristic tracker remain distinct
scopes until a separate behavior change deliberately consolidates them.

| Reachable owner call | Event operation | Decision-affecting payload |
|---|---|---|
| `updateState(...)` | `UPDATE_STATE` | state hash, turn, ordered sequence, repeat/loop counts, sorted turn blocks before/after |
| `recordDecision(...)` | `RECORD_RESPONSE` | call subject, ordered sequence rows, last-action pair, cancel key/count, repeat/loop counts, turn blocks before/after |
| heuristic `blockLastActionOnCancel(...)` path | `BLOCK_RESPONSE` | decision key/response and sorted turn-block delta |
| heuristic phase update | `PHASE_CHANGE` | phase plus affected sequence/block state before/after |
| `clear()` | `CLEAR` | complete decision-affecting tracker snapshot before/after |

Sequence contents are mandatory; size alone cannot replay loop detection. `history` is diagnostic
only and is excluded from the decision-state payload. The permanent `blockedResponses` map has no
active population path. `RESET_REPEAT` is unreachable and is excluded unless a future caller makes
it live.

## Heuristic owner memory

| Reachable mutation | Event operation | Required evidence |
|---|---|---|
| state/turn update | `STATE_UPDATE` | exact five state reads plus complete heuristic snapshot before/after; turn-rollback clears of both reassignment maps; normal-advance pruning of `recentReassignmentTurns` only; state-hash-change clears of `localBlockedResponses`, `recentDecisionResponses`, and `lastDecisionRepeatCount`; `currentTurnNumber`, `currentStateHash`, and `blockStateHash` deltas |
| chosen action memory | `ACTION_CHOICE_REMEMBER` | exact prior/new action tuple |
| failed search | `FAILED_SEARCH_ADD` | exact prior action tuple plus sorted membership deltas for action text, card id, and blueprint id; no counter exists |
| single-decision response | `SINGLE_RESPONSE_RECORD` | raw/tracking response and complete heuristic snapshot before/after; any internally created local-response block is folded into this event |
| recent response deque | `RECENT_RESPONSE_APPEND` | appended response, evicted response if any, ordered deque before/after |
| reassignment memory | `REASSIGNMENT_RECORD` | typed key variant and value, turn, `recentReassignmentTurns` delta, and folded `reassignmentCounts` delta |

The full reassignment clear occurs when turn decreases, not on every turn change. Normal advance
prunes only `recentReassignmentTurns`; `reassignmentCounts` persists until rollback. Pruning executes
inside `updateDecisionTrackerState(...)`, so it belongs to `STATE_UPDATE`, not
`REASSIGNMENT_RECORD`. The local-response block is internal to the single-response helper and is
folded into `SINGLE_RESPONSE_RECORD`; it is not a seventh top-level owner event. There is no reachable
heuristic-memory reset owner. Last-action memory, state-hash clears, append/eviction order, deterministic
collection canonicalization, and pruning cannot be omitted. The source-complete six-boundary contract is
`Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md`.

## Strategy controller owner

| Reachable call | Family and operation | Retained state |
|---|---|---|
| `setSide(...)` | lifecycle `SIDE_SET` | `mySide` |
| `reset()` | lifecycle `RESET` | phase, force, focus, reserve, location, and battle state |
| `startNewTurn(...)` | lifecycle `START_TURN` | turn, phase, force target, reserve state |
| `updateBattleOrderFromGameState(...)` | battle `BATTLE_ORDER_REFRESH` | `underBattleOrderRules` |
| `onSuccessfulDeploy(...)` | battle `FOCUS_DEPLOY_RECORD` | focus deployment/confidence |
| `onBattleResult(...)` | battle `BATTLE_RESULT_RECORD` | win/loss counters, confidence, possible focus |

`FOCUS_DEPLOY_RECORD` is currently a reachable no-op because no live call sets focus away from
`BALANCED`. If recorded, its owner-specific outcome is `NO_OP`. The outer strategic wrapper emits no
event; its battle calls are recorded at `StrategyController` and its direct deploy-memory writes are
recorded at the outer owner.

## Shield strategy owner

| Reachable call | Event operation | Retained state |
|---|---|---|
| `setSide(...)` | `SIDE_SET` | side |
| `reset()` | `RESET` | own/opponent shield sets, objective, limits, auto-play and K&D counters |
| `recordOpponentCard(...)` | `OPPONENT_CARD_SEEN` | shield-owned seen-card set |
| `setOpponentObjective(...)` | `OPPONENT_OBJECTIVE_SET` | opponent objective |
| `recordOpponentShield(...)` | `OPPONENT_SHIELD_SEEN` | blueprint and title keys |
| `recordShieldPlayed(...)` | `OWN_SHIELD_RECORDED` | played set and possible auto-play count |
| `recordKnDActivation(...)` | `KND_ACTIVATION_RECORD` | turn and activation count |

These operations never share an event family with the outer dedupe sets or
`OpponentDeckTracker`.

## Objective owners

| Owner call | Event operation | Retained state |
|---|---|---|
| `ObjectiveHandler.reset()` | handler `RESET` | objective and requirement state |
| `ObjectiveAnalyzer.analyze(...)` | analyzer `ANALYZE` | objective identity, parser result, active playbook/profile, setup and flip state |
| `ObjectiveAnalyzer.refreshFlipStatus(...)` | analyzer `REFRESH_FLIP` | flipped state |
| `ObjectiveAnalyzer.reset()` | analyzer `RESET` | parser, profile, setup, flip, and identity caches |

The static `PROFILES` lazy initialization is process configuration, not session-owned state. It must
be represented by a frozen configuration/catalog fingerprint before full replay claims, not by a
session event. `PROFILE_REPLACE` does not exist. Assignment of `activePlaybook` is an internal delta
of `ANALYZE`. The extra Rando-only `refreshFlipStatus(...)` call requires an explicit route waiver or
parity correction; it must not be silently normalized away.

## Deck and opponent-deck owners

| Owner call | Event operation | Retained state |
|---|---|---|
| `DeckOracle.analyze(...)` | `ANALYZE` | rebuilt catalogs, totals, analyzed state |
| `DeckOracle.refresh(...)` | `REFRESH` | exact catalog zone rows and any catalog growth |
| `DeckOracle.reset()` | `RESET` | catalogs, pull failures, AMSD sentinel, analyzed state |
| `recordAmsdFailedOnTurn(...)` | `AMSD_RETRY_SET` | turn before/after plus candidate/action subject |
| `OpponentDeckTracker.recordPeek(...)` | `DESTINY_RECORD` | supplied destiny values, sum/count/average, peek count, intel flag |
| `OpponentDeckTracker.reset()` | `RESET` | all destiny intel |

Catalog additions are internal deltas of `REFRESH`, not separate public operations. Failed-pull
increment/clear methods have no callers. `AMSD_RETRY_CLEAR` does not exist outside full deck reset.
`recordPeek(...)` receives no card identity, so a destiny event must not fabricate one.
`setDeckName(...)` is production-unreachable through the dead `CuratorAi` construction chain and is
an explicit exclusion. A new production caller reopens the inventory.

## Deploy-plan owner

| Reachable mutation | Event operation | Required evidence |
|---|---|---|
| `currentPlan` replacement and `lastPlanTurn` write | `BEGIN` | turn, strategy, exact reason, reserve-for-battle, deployment count, flags, ordered instruction rows |
| `DeploymentPlan.recordDeployment(...)` | `RECORD_DEPLOYMENT` | blueprint, removed row occurrences, remaining rows, count before/after |
| live setters to true | `FLAG_SET` | closed flag key and before/after |
| planner reset | `CLEAR` | plan and turn before/after |

Instruction rows contain only source values: blueprint id, target-location id, priority, and deploy
cost. There is no plan id, instruction id, cursor, advance, cancel, physical-card identity, backup
use, or flag-clear path. Do not invent them. Construction-only setters and transient planner
`currentGame/currentPlayerId/currentSide` are excluded. The per-decision
`DeployPhasePlanner.setObjectiveAnalyzer(...)` rebind is also an excluded dependency pointer, not
planner decision state. Unread plan/instruction fields remain documented tombstones, not trace
payload.

## Retained evaluator state

| Owner | Reachable mutation | Event operation | Required evidence |
|---|---|---|---|
| `ActionTextEvaluator` retry budget | turn change/reset | `TURN_CLEAR` | retry turn and complete sorted retry-map delta |
| `ActionTextEvaluator` retry budget | candidate retry write | `INCREMENT` | exact legacy key, candidate reference, count before/after, turn |
| `ActionTextEvaluator` barrier memory | turn change/reset | `TURN_CLEAR` | barrier turn and cleared normalized-text set |
| `ActionTextEvaluator` barrier memory | scoring-time target add | `CANDIDATE_TARGET_ADD` | normalized target-text key, candidate reference, membership before/after |

The barrier key is candidate action text, not a typed card/location, and it is added while scoring
before the candidate wins. Calling it a played barrier is false.

`MoveEvaluator` gets no state-event family. Its ladder fields reset per candidate and already affect
the operation trace through score/veto operations. `pendingMoveCardIds` is never populated or read;
its `lastTurnNumber` is only a live-but-inert clear latch. `ladderBandsChecked` is itself the static
assertion flag and is diagnostic only.

`DeployEvaluator` likewise gets no event for `pendingDeployCardIds`: all population statements are
commented, so its `lastTurnNumber` only clears an always-empty set. Future code that populates either
pending set or makes a MOVE/DEPLOY evaluator field cross-decision and decision-affecting reopens this
inventory.

## First Java slice after review

Stage 4A1 is intentionally smaller than the full matrix:

1. Add `TraceStateEvent`, `TrackerOwner`, `MutationOutcome`, distinct `EngineCallOutcome`, complete
   immutable outer-tracker snapshots, and only the concrete records required by the current outer
   hooks: final outer `RECORD_RESPONSE`, pending concede `SET/CLEAR`, engine
   `PLAYER_LOST(SUCCESS|THREW)`, and outer pending-deploy `SET/CLEAR`.
2. Replace the envelope's prose event list, bump schema to 3, and capture the complete tracker state
   before and after the one legacy `recordDecision(...)` call. The event itself is emitted only after
   that call. Instrument all ten real lexical hook locations per bot; the old count of three referred
   only to the prose hooks being replaced.
3. Remove the wrapper-level strategic-intent event. Emit only at actual direct writes. Leave
   `StrategyController` calls unobserved until their owner increment. The wrapper's
   `onBattleResult(...)` observation is the only intentionally deferred half.
4. Preserve the real concede order, including caught-Exception
   `PLAYER_LOST(THREW)` followed by `CLEAR_PENDING`.
5. Keep capture disabled and prove one legacy call with or without tracing. Disabled capture calls
   neither pure tracker accessor and constructs no snapshot or event.

No other family is declared type-first. Each later family lands beside its first owner hook and
constructor tests.

## Corrected landing order

1. **4A0:** K-2 and council review complete in `m00363`; corrections are incorporated here.
2. **4A1:** outer atomic slice above. Capture remains disabled. F1/F2 are not required for this
   inert observation slice, but still gate finalizer/interceptor work.
3. **4A2:** remaining outer and inherited tracker operations, reusing the complete ordered-state
   snapshot model established for outer `RECORD_RESPONSE`.
4. **4B:** one owner per increment: heuristic memory, strategy lifecycle, strategy battle state,
   objective handler/analyzer, deck, shield, then opponent deck.
5. **4C:** deploy plan, retry budget, and barrier target memory. No MOVE family.
6. **4D:** exact route corpus and normalized Rando/ChosenOne comparison with named waivers.

Enabled capture, behavior repair, tracker consolidation, finalizer/interceptor cutover, owner
retirement, deployment, and push remain held throughout this inventory review.

## Review result

K-2 source verification and council answered all five questions `YES` in `m00363`, subject to the
corrections now incorporated:

1. Does every reachable retained decision-state mutation map exactly once or to an explicit
   exclusion?
2. Does any row cross an ownership boundary or duplicate an internal delta?
3. Is every operation named after a real source call or direct write?
4. Is the 4A1 type set the minimum required to replace only the existing outer hooks?
5. Do the engine-call outcomes and pending clear order match both success and exception paths?

4A1 is now authorized only at the narrow boundary above. Enabled capture, later owner families,
behavior repair, tracker consolidation, finalizer/interceptor cutover, deployment, and push remain
held.
