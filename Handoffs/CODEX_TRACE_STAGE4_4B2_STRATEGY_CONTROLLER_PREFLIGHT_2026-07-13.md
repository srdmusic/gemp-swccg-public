# Trace Stage 4B2: StrategyController Preflight

Date: 2026-07-13
Architecture/review owner: Codex/Alfred
Java implementation owner: K-2/Claude small agent after release
Audit base and required implementation parent: `85eb0452a`
Status: `RELEASED FOR JAVA` after independent Codex and K-2 source/council confirmation
Deployment: held
Push: held

## Continuity Gate

This is the next Fable-defined trace increment after independently accepted commits:

- 4B1 `ec886934b`: `ADVANCE`
- F1 `5bd89ac68`: `ADVANCE`
- F2 `a095db834`: `ADVANCE`

The model change to Opus does not reopen, reinterpret, or broaden those increments. Opus must
dispatch the released brief below verbatim as one background agent. Java ownership is limited to
that brief. Deployment, push, capture enablement, behavior repair, and adjacent cleanup remain
unauthorized.

## Scope

Observe only the mirrored Rando and ChosenOne `StrategyController` owners at six operation kinds
across seven real lexical call sites per bot. Win and loss are separate lexical calls to the same
battle-result operation. This increment records retained state transitions. It does not repair or
activate strategy behavior.

The outer `lastPendingDeployType` field and its 4A1 pending-deploy events remain a separate owner.
The inherited and outer decision trackers remain 4A2 owners. Heuristic memory remains the 4B1
owner. No existing event is folded into the controller owner.

The mirrored controller sources are behaviorally identical after package and logger/config
normalization. The outer call paths are also normalized mirrors.

## Source-Complete Owner Table

| Event family | Exact reachable call | Retained effect and exact no-op behavior |
|---|---|---|
| `SIDE_SET` | `setSide(Side side)` in the new-game branch | Unconditionally assigns `mySide`. A same-value assignment executes and emits `NO_OP`. |
| `RESET` | `reset()` immediately after `setSide(...)` | Resets every retained field except `mySide` and `lastDecisionReason`. A fresh controller after side assignment may be `NO_OP`. |
| `START_TURN` | `startNewTurn(int turnNumber)` when `currentTurn > lastTurn` | Writes turn, phase, target, and per-turn reserve count. Clears reserve-card memory only when `turnNumber - lastReserveCheckTurn > 2`. It does not recompute `forceDeficit`. |
| `FOCUS_DEPLOY_RECORD` | `onSuccessfulDeploy(String cardType)` only when outer `lastPendingDeployType != null` | Mutates only when focus is `ground` or `space` and the card type matches. Current production never calls `setFocus(...)`, so normal production calls are presently `NO_OP`; fixtures may seed focus through the existing public mutator. |
| `BATTLE_ORDER_REFRESH` | `updateBattleOrderFromGameState(GameState gameState)` once on every non-null `trackGameState(...)` path | Scans permanent cards in `Zone.SIDE_OF_TABLE` for the exact six Battle Order/Plan blueprint ids, catches `Exception`, then assigns `underBattleOrderRules`. The game/service reference is never stored in an event. Snapshot equality supplies `CHANGED` or `NO_OP`. |
| `BATTLE_RESULT_RECORD` | `onBattleResult(boolean won)` after exact battle-text recognition | `true` increments wins. `false` increments losses, reduces confidence by `0.3f`, and resets focus below `0.3f`. There is no dedupe; repeated matching text repeats the mutation. |

Preserve Fable's two conceptual families: `SIDE_SET`, `RESET`, and `START_TURN` are lifecycle;
`BATTLE_ORDER_REFRESH`, `FOCUS_DEPLOY_RECORD`, and `BATTLE_RESULT_RECORD` are battle-coupled.
Implement six operation-specific records rather than one nullable Cartesian record or a generic
operation/detail record. Each record carries `StrategyControllerOwner`, the serializable exact
call argument where one exists, complete before/after snapshots, and a constructor-validated
`MutationOutcome` derived from snapshot equality:

- `StrategySideSetEvent`: exact nullable `Side side` argument, because the legacy call accepts it;
- `StrategyResetEvent`: no call argument;
- `StrategyStartTurnEvent`: exact `int turnNumber` argument;
- `StrategyFocusDeployRecordEvent`: exact non-null `String cardType` argument;
- `StrategyBattleOrderRefreshEvent`: no `GameState` or service reference;
- `StrategyBattleResultRecordEvent`: exact `boolean won` argument.

`StrategyControllerOwner` is closed to `RANDO` and `CHOSENONE`. Do not reuse `TrackerOwner`.

## Operation-Specific Invariants

Every record rejects an unrelated snapshot delta. Snapshot inequality alone is insufficient:

- `SIDE_SET`: `after` must equal `before` with only nullable side replaced by the exact argument.
- `RESET`: `after` must be the exact reset projection: preserved side and decision reason; flags
  `false/true/false`; phase `early`; turn/generation zero; target and deficit 8; focus `balanced`;
  confidence exactly `0.5f` raw bits; focus counters zero; location/reserve collections empty;
  reserve counters/turn zero; battle counters zero.
- `START_TURN`: side, flags, generation, deficit, focus, locations, last reserve-check turn,
  battle counters, and decision reason are frozen. Turn equals the argument; reserve checks become
  zero; phase/target are exactly `early/8` through turn 3, `mid/6` through turn 8, otherwise
  `late/5`; seen-reserve cards clear iff `turnNumber - lastReserveCheckTurn > 2`.
- `FOCUS_DEPLOY_RECORD`: a balanced or nonmatching focus requires identical snapshots. A matching
  ground/space card increments deployments once and raises confidence by exactly `0.2f`, capped at
  `1.0f`, only from the second matching deployment onward. Every other field is frozen.
- `BATTLE_ORDER_REFRESH`: only `underBattleOrderRules` may differ.
- `BATTLE_RESULT_RECORD`: win increments only wins. Loss increments only losses, subtracts exactly
  `0.3f` with a zero floor, and changes only focus to `balanced` when resulting confidence is below
  `0.3f`. Focus counters are not reset. Every other field is frozen.

## Exact Reachability, Guards, and Order

For both bots:

1. `decide()` opens the decision trace when capture is enabled, then calls
   `trackGameState(...)` before route evaluation.
2. Null `gameState` returns before every controller call and therefore emits no controller event.
3. The exact existing new-game guard is
   `mySide == null || !newOpponent.equals(opponentName)`. It is opponent-based. It does not detect
   a side-only change once `mySide` is non-null, and `newOpponent.equals(...)` retains its existing
   null risk. Instrumentation must not normalize or repair either behavior.
4. Inside that branch, the existing pending-concede clear and outer tracker clear run before
   controller `SIDE_SET`, then controller `RESET`. Each controller call gets its own event.
5. When `currentTurn > lastTurn`, controller `START_TURN` runs. Optional controller
   `FOCUS_DEPLOY_RECORD` runs later in the same branch, before the outer pending-deploy `CLEAR`.
6. Opponent-card and own-shield tracking run next. Controller `BATTLE_ORDER_REFRESH` then runs once.
7. After response recording, `trackStrategicEvents(...)` returns immediately when decision is
   null or result is null/empty. The result value otherwise does not determine battle outcome.
8. That helper classifies and writes the outer pending deploy first. Its 4A1 `SET` event therefore
   precedes any controller battle-result event.
9. Controller `BATTLE_RESULT_RECORD(true)` runs only when lowercased decision text contains
   `battle` and either `you won` or `you have won`. `BATTLE_RESULT_RECORD(false)` runs only when it
   contains `battle` and either `you lost` or `you have lost`. The branches are `if/else if`, so at
   most one runs. These are two lexical hooks for one operation kind.

An early guard that prevents the legacy controller call emits no controller event. Once a legacy
call executes, emit exactly one corresponding event even when before and after are equal.

## Internal and Production-Unreachable Mutators

`setUnderBattleOrderRules(boolean)` is called internally by
`updateBattleOrderFromGameState(...)`. Its write remains folded into the single external
`BATTLE_ORDER_REFRESH` event. It is not a seventh operation kind.

These public mutators have no `src/main` caller at the audit base:

- `updateForceGeneration(int generation)`
- `setFocus(StrategyFocus focus)`
- `recordReserveCheck(List<String> cardsSeen)`

They emit no top-level event in 4B2. Direct fixtures may use them only to establish retained state
before testing a reachable lifecycle call. Preserve their current behavior: `setFocus(null)` can
throw before mutation, and `recordReserveCheck(...)` does not enforce `shouldCheckReserve()`.

## Snapshot Boundary

Add one common immutable `StrategyControllerSnapshot` containing all 21 retained instance fields:

- nullable side;
- `underBattleOrderRules`, `hasShieldsToPlay`, and `offeredConcedeThisGame`;
- phase and turn number;
- force generation, target, and deficit;
- focus, raw float confidence bits, focus-turn count, and focus-deployment count;
- contested and dangerous location lists;
- reserve-check count, seen-reserve cards, and last reserve-check turn;
- battle win and loss counts;
- last decision reason.

This is complete retained legacy state, not a claim that every field currently affects a
decision. `hasShieldsToPlay`, `offeredConcedeThisGame`, `contestedLocations`,
`dangerousLocations`, `turnsWithFocus`, and `lastDecisionReason` are currently inert or
effectively write-only. They remain in the snapshot because lifecycle calls preserve or clear
them and future source drift must be visible.

Serialize phase and focus as their exact lowercase `.getValue()` strings so the snapshot does not
depend on either bot package or leave `.name()` versus `.getValue()` to the implementer. Preserve
raw float bits. Freeze all collections defensively.
Preserve the `ArrayList` order of both location lists. Copy `cardsSeenInReserve` from its
`HashSet` into a sorted immutable list. No snapshot equality may depend on hash iteration.

Do not use `getStatus()`: it omits retained fields. Do not use reflection. Add one pure
package-local `traceSnapshot()` seam to each mirrored controller and one tiny public read-only
`StrategyControllerTraceAccess` bridge in each controller package. Each bridge delegates to its
package-local seam and exposes no mutator or raw field.

## Authorized Instrumentation Shape

- Instrument the seven outer lexical call sites per bot, not the legacy controller mutator bodies.
- Before each legacy call, capture the before snapshot only under `TraceSession.isActive()`.
- Run the legacy call exactly once at its unchanged source position.
- After normal return, capture the after snapshot and record the matching typed event.
- A before-snapshot failure marks `INCOMPLETE/STATE_EVENT` and still runs the legacy call once.
- An after-snapshot, event-construction, or append failure marks `INCOMPLETE/STATE_EVENT`; the
  already-completed legacy call and response remain unchanged.
- Never catch or change an exception thrown by the legacy controller call itself.
- With no active session, do not call either trace-access bridge and do not allocate a snapshot or
  event. Both bots retain `NoOpTraceSink.INSTANCE` as their production default.
- Do not move, repeat, combine, suppress, or add a controller call.

Because hooks land at outer call sites, normalized `javap` output for the six external controller
mutators plus internal folded `setUnderBattleOrderRules(...)` must have method-level bytecode
identity against the audited parent after instrumentation. The added pure snapshot seam is
outside those seven methods.

## Required Fixtures

- Common state tests prove the snapshot is defensively immutable, canonical, and equal across
  normalized Rando/ChosenOne state. Deliberately shuffled reserve-set input must serialize in
  stable order.
- Event tests prove all six records, closed owner identity, exact serializable arguments,
  required-null/impossible-input rejection, the intentionally nullable side argument, and derived
  `CHANGED`/`NO_OP` consistency. Every operation-specific frozen-remainder and transition
  invariant above gets a negative constructor fixture.
- Direct controller fixtures prove each listed retained delta, reset preservation, stale force
  deficit on turn start, reserve cooldown clearing, focus transition behavior, repeated battle
  mutation, and direct-null/unreachable-mutator behavior without inventing owner events.
- New-game bot fixtures prove exact event order: existing pending-concede clear, existing outer
  tracker clear, `SIDE_SET`, then `RESET`. Preserve the opponent-based guard and its null risk.
- Turn fixtures prove `START_TURN`, optional `FOCUS_DEPLOY_RECORD`, then existing pending-deploy
  `CLEAR`. Balanced focus produces a real `NO_OP` successful-deploy event.
- Battle Order fixtures use one of the exact six ids in `Zone.SIDE_OF_TABLE`; wrong zone,
  title-only, absent card, and caught scan-exception controls prove current behavior.
- Strategic-event fixtures prove outer pending-deploy `SET` precedes `BATTLE_RESULT_RECORD`, the exact
  four text fragments, result-value irrelevance after the non-empty guard, and no dedupe.
- Disabled-capture fixtures prove zero bridge/snapshot/event calls and unchanged response.
- A prepared collector whose `recordStateEvent(...)` throws proves one legacy call, unchanged
  response/state, no surviving event, and typed `INCOMPLETE/STATE_EVENT` evidence.
- Normalize mirrored production hooks, controller seams/bridges, and tests after package, class,
  logger/config, and owner substitutions. Prove parity.
- Compare normalized method-level `javap` output for the six external legacy controller mutators
  plus internal `setUnderBattleOrderRules(...)` against `85eb0452a` in both mirrors.
- Run every existing trace contract and mirrored bot trace-hook fixture, every new 4B2 test, the
  module test suite, package, and `git diff --check`.

## Independent Codex Audit

A read-only current-HEAD source agent returned `HOLD` on the previous packet. It confirmed the six
reachable external boundaries and mirror parity, but found these required corrections:

- the new-game guard is opponent-based, not a generic side/opponent change detector;
- `setUnderBattleOrderRules(...)` is internal and folded;
- six retained fields cannot be called decision-affecting in current source;
- `cardsSeenInReserve` requires sorting;
- exact source guards and pending-deploy/battle-result ordering were missing;
- the previous packet had no embedded Agent-Ready Implementation Brief.

Those corrections are incorporated here. K-2 independently dispatched one small read-only source
agent and narrow council review against the corrected packet.

A second Codex continuity agent then caught five architecture-level drifts in the first corrected
draft before release: provisional operation renames, six-versus-seven lexical-hook cardinality,
missing operation-specific transition invariants, unspecified enum serialization, and omission of
the folded internal setter from the bytecode gate. Codex invalidated that packet hash in `m00490`.
This revision restores the Fable-defined names and family split and incorporates all five holds.
K-2 then returned `AGREE` in `m00493` against exact packet hash `1631df9e...`: its source agent
confirmed the six-operation/six-mutator bijection, seven lexical hooks per bot, lowercase
`.getValue()` serialization, every frozen-remainder transition invariant, and the seven-method
bytecode gate. Its council found no factual source mismatch and independently highlighted the
direct battle-loss focus assignment that preserves both focus counters, already pinned here.

## Release Review

K-2 confirmed all of the following in `m00493`:

- current HEAD is a Java/test-identical descendant of `85eb0452a`, or report exact drift;
- six reachable operation kinds across seven lexical sites per bot are complete and
  non-duplicative;
- snapshot fields and canonicalization exactly match current source;
- event folding and source order are exact;
- the bridge/call-site shape preserves all seven relevant legacy mutator bodies and disabled
  capture across seven lexical hooks per bot;
- council finds no concrete ownership, ordering, or determinism defect.

Java implementation is released under the verbatim brief below. Capture enablement, behavior
repair, owner consolidation, cutover, deployment, and push remain held.

## Agent-Ready Implementation Brief

This prompt is `AUTHORIZED FOR VERBATIM DISPATCH` as one background agent. No other Java packet or
adjacent cleanup is opened by this release.

```text
You own Trace Stage 4B2, mirrored StrategyController observation only. You are not alone in the
repository. Preserve all existing user, K-2, and Codex edits. Do not revert, reformat, or clean
unrelated work. Read this entire packet before editing:
Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md

BASE GATE
1. The audited production baseline is 85eb0452a. A later checkout is allowed only when
   `git diff --exit-code 85eb0452a..HEAD -- src/gemp-swccg-server/src/main/java src/gemp-swccg-server/src/test/java`
   is empty. If it is not empty, stop and report exact paths. Do not merge competing Java.
2. Capture remains disabled by default. Do not deploy, push, or commit. K-2 owns verification and
   the isolated commit.

WRITE OWNERSHIP
- src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java
- src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/TheChosenOneAi.java
- both mirrored strategy/StrategyController.java files, pure package-local traceSnapshot() only
- one mirrored strategy/StrategyControllerTraceAccess.java read-only bridge per bot
- src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/trace/TraceSession.java
- only StrategyControllerSnapshot, StrategyControllerOwner, the six 4B2 event records, and the
  sealed permits under common/trace/state/
- focused 4B2 tests and existing mirrored bot trace-hook fixtures
Do not edit scoring, evaluators, objectives, deck logic, shields, deploy planning, finalizers,
engine behavior, card Java, other trace families, or unrelated comments.

IMPLEMENTATION CONTRACT
1. Add the exact immutable canonical 21-field StrategyControllerSnapshot from Snapshot Boundary.
   Use exact lowercase `.getValue()` strings for phase/focus, raw float bits, immutable ordered
   location lists, and a sorted immutable reserve-card list.
2. Add StrategyControllerOwner with only RANDO and CHOSENONE. Preserve the lifecycle versus
   battle-coupled family split and add exactly six operation-specific records:
   StrategySideSetEvent, StrategyResetEvent, StrategyStartTurnEvent,
   StrategyFocusDeployRecordEvent, StrategyBattleOrderRefreshEvent, and
   StrategyBattleResultRecordEvent.
   Use only the serializable exact arguments listed in Source-Complete Owner Table. Preserve the
   legacy-nullable SIDE_SET side argument; require every other event field specified as non-null.
   Enforce every frozen-remainder and exact transition invariant in Operation-Specific Invariants;
   snapshot inequality alone is not sufficient. Never store a GameState, service, callback,
   timestamp, random value, or prose identity.
3. Add one package-local pure traceSnapshot() seam and one public read-only delegating bridge in
   each mirrored strategy package. Do not expose a field, add reflection, or alter a legacy
   mutator body.
4. Add six narrow TraceSession record methods following the existing STATE_EVENT error law. With
   no active session they immediately return. Event construction/append failure marks
   INCOMPLETE/STATE_EVENT and never throws into gameplay.
5. Instrument only the seven existing outer lexical call sites per bot. Snapshot only under the
   active-session guard. Invoke each legacy controller method exactly once in unchanged source
   order. An early guard before the call emits no event; an executed call emits exactly one event,
   including NO_OP. Never catch or alter a legacy-call exception.
6. Preserve all folding and ordering in this packet. setUnderBattleOrderRules remains internal to
   BATTLE_ORDER_REFRESH. Outer pending-deploy events remain separate and keep their positions.
7. Do not repair the opponent-based new-game guard, null risk, stale force deficit, inactive focus,
   repeated battle counting, exception handling, or production-unreachable mutators.

REQUIRED TESTS
1. Cover snapshot canonicalization/immutability and every event constructor/invariant.
2. Cover each changed path, each executed-call NO_OP, and each early-guard suppression path.
3. Cover exact mixed-owner order for new game, new turn/pending deploy, and strategic events in
   both bots. Disabled capture must produce no 4B2 bridge/snapshot/event work.
4. Use a prepared throwing state-event collector to prove typed failure without a skipped,
   repeated, or altered legacy call/response.
5. Normalize and prove Rando/ChosenOne parity. Compare normalized method-level `javap` output for
   setSide, reset, startNewTurn, onSuccessfulDeploy, updateBattleOrderFromGameState,
   onBattleResult, and internal setUnderBattleOrderRules against 85eb0452a.
6. Run every existing trace contract and mirrored bot hook fixture, every new 4B2 test, then
   `mvn -q -pl gemp-swccg-server -am test -DskipITs` and
   `mvn -q -pl gemp-swccg-server -am package -DskipTests -DskipITs`.

REPORT TO K-2
- exact changed paths and seven lexical hook line numbers per bot
- focused and full test totals, failures, errors, and skips
- package exit code
- normalized Rando/ChosenOne parity result
- seven-method normalized javap identity result
- git diff --check result
- proof both bots still default to NoOpTraceSink.INSTANCE
- any behavior or bytecode delta outside active trace guards, which is an automatic HOLD
```
