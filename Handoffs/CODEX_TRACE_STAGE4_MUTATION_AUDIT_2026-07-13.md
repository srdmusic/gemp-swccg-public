# Trace Stage 4 mutation audit

Date: 2026-07-13
Audit baseline: `dde6488e0`
Current integration baseline: `46e62f4dc`
Owner: Codex architecture and gate; K-2 production Java
Status: superseded by the reviewed source-cardinality contract in
`CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md`

## Verdict

The current `TraceIntendedStateEvent(Kind, detail)` stream is not Stage-4 complete.
It observes only three outer sites per bot. Two of those observations run before their mutators and
therefore record intent, not an actual mutation. The current stream cannot distinguish lifecycle
operations without parsing prose.

This audit also found one active dual-owner inconsistency plus latent reuse hazards. Normal Hall
games construct a fresh Rando controller per game, so prior-game leakage is not yet established on
that runtime path. Any behavior repair must remain separate from instrumentation.

## Current observation coverage

Both bots currently have the same three call sites:

| Event | Current placement | Actual evidence |
|---|---|---|
| `PENDING_CONCEDE` | after the pending fields are written | valid post-write observation for set only |
| `DECISION_TRACKER_RECORD` | before `decisionTracker.recordDecision(...)` | intent only; the call can still fail or diverge |
| `STRATEGIC_EVENT_RECORD` | before `trackStrategicEvents(...)` | intent only; false-positive when the method makes no write |

The pending-concede fire and clear paths have no events. No existing event records the heuristic
tracker, strategy/objective/deck refreshes, deploy-plan lifecycle, retry/barrier/move state, or
opponent intelligence.

## Session boundary findings

- `context = RandoContext.build(...)` runs before the trace session opens in both bots.
- `context = null` runs while the session is still open.
- The trace snapshot reads outer tracker blocked responses before `trackGameState(...)` performs
  its new-game clear. That ordering can expose stale evidence only if a controller instance is
  reused.
- Direct interceptors return through `finally`, but skip evaluator-context construction, chaos,
  outer safety, final tracker recording, and strategic-event recording. Their absent mutation
  events are expected and must remain visibly absent.
- `buildEvaluatorContext(...)` mutates objective, deck, and planner state before evaluator
  `canHandle` results are known. A declined evaluator lane can still have state events.

## Mandatory Stage-4 mutation families

The frozen Trace V2 contract requires post-write observation at the existing owner, never a replay.
Anchors below are from `dde6488e0`; re-anchor by method and statement before implementation.

| Family | Existing owner or call boundary | Required event distinction |
|---|---|---|
| Outer tracker | both bot `DecisionTracker` instances and final record call | update, clear, accepted-response record |
| Heuristic tracker | `HeuristicAiBase` private tracker | update, block/add, clear, response record |
| Strategy lifecycle | `StrategyController` public mutators | new game, start turn, refresh, reset, strategic record |
| Objective lifecycle | `ObjectiveAnalyzer` analyze/refresh/reset/profile cache | analyze, refresh, reset, profile replace |
| Deck lifecycle | `DeckOracle` analyze/refresh/reset/catalog growth/AMSD retry | analyze, refresh, reset, catalog add, retry set/clear |
| Deploy plan | `DeployPhasePlanner`, `DeploymentPlan`, `DeploymentInstruction` | begin, advance, record deployment, cancel, clear, flag set |
| Retry and barrier | `ActionTextEvaluator` retained retry/barrier state | increment, set, clear |
| Move state | `MoveEvaluator` retained turn/ladder/assertion state | turn reset, set, clear |
| Opponent intelligence | `OpponentDeckTracker` and shield seen-card state | record, set, clear, reset |
| Strategic memory | branch writes inside bot `trackStrategicEvents` | actual record only, no pre-call false-positive |
| Concede lifecycle | pending-concede fields and `playerLost` call | set, fire, clear, engine loss action |

Every hook belongs immediately after the existing write or successful mutator call. Never call
`refresh`, `recordDecision`, `createPlan`, evaluator scoring, `decisionMade`, or an RNG method again
to manufacture evidence.

## Active inconsistency and latent lifecycle hazards

These are not instrumentation work. Runtime reachability must be proven before changing behavior:

1. Two independent decision trackers exist. Each outer bot owns one tracker, while
   `HeuristicAiBase` owns another Rando tracker. ChosenOne heuristic fallback therefore mutates the
   inherited Rando tracker, not its package-local outer tracker. This divergence is active inside a
   game whenever heuristic fallback runs, even if both trackers are otherwise fresh.
2. New-game reset clears only the outer tracker. If a controller is reused, the inherited tracker
   can leak loop/blocked state across games.
3. New-game reset leaves evaluator retry/barrier/move state, planner retained side/analyzer state,
   and `lastPendingDeployType` untouched. These are latent reuse hazards.
4. The trace snapshot can freeze stale outer tracker state before the new-game clear, but only on a
   reused controller.
5. The normal Hall path constructs a new `RandoCalAi` in `HallServer.createAiForSkill(...)`, stores
   it under a game-specific `AiRegistry` key, and unregisters that game later. Current source does
   not prove cross-game controller reuse on this path.

Required handling: first add a fixture proving whether the dual trackers produce conflicting loop
state within one game. Audit every non-Hall controller lifecycle before proposing any cross-game
reset repair. Do not mix a behavior repair with Stage-4 hooks, finalizer F3, or cleanup commits.

## Scope exclusions

- `EvaluatedAction` candidate score/veto changes remain in the operation trace, not state events.
- Local scratch values and newly constructed transient assessment objects are not retained state.
- Chat, personality overlays, process-wide blueprint caches, and RNG draws are outside the current
  Stage-4 enum. They require an explicit scope decision before the trace can claim complete replay
  of those effects. Do not silently fold them into free-form `detail`.
- Engine `playerLost` is not AI memory. If observed, it needs a distinct engine-action event.

## Landing order

1. Freeze a machine-comparable event schema. Diagnostic prose cannot define owner, operation, or
   subject identity.
2. Move the two existing pre-call observations to successful post-write boundaries and add
   pending-concede fire/clear coverage.
3. Add tracker and strategy/objective/deck lifecycle events at public mutator exits.
4. Add deploy-plan, retry/barrier/move, and opponent-intelligence events at their existing writes.
5. Add exact route fixtures through both bot entry paths. Compare event count, order, typed owner,
   operation, subject, and values. Missing, extra, reordered, or prose-only identity fails.
6. Keep the sink disabled and apply no shadow mutation.

## Gate corpus

- A controller-reuse fixture, if that lifecycle exists, cannot freeze a stale prior-game
  blocked-response snapshot.
- Outer and heuristic trackers are separately visible until one owner is deliberately cut over.
- A strategic-event no-op emits no mutation event.
- Pending concede set, fire, and clear are distinct and ordered.
- Deploy-plan begin, advance, cancel, and clear cannot compare equal.
- Retry increment differs from retry clear; barrier set differs from barrier clear.
- Objective/deck refresh differs from reset and from an observed no-op.
- Direct interceptors contain no fabricated downstream tracker or strategic events.
- Rando and ChosenOne normalized event streams match for shared routes; explicit personality routes
  require named waivers.
- A state-event capture failure marks the trace `INCOMPLETE` and never changes gameplay.
- No hook invokes an existing mutator or RNG source a second time.

This audit does not authorize implementation. The reviewed 4A0 matrix replaces its provisional
families and is the only current Stage 4 authority. It opens the narrow capture-disabled 4A1 slice;
enabled capture, later families, behavior repair, deployment, and cutover remain held.
