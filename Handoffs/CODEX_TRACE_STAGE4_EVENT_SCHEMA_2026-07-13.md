# Trace Stage 4 Typed State-Event Schema

Date: 2026-07-13
Architecture owner: Codex/Alfred
Java implementer: K-2/Claude
Status: `SUPERSEDED` by `CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md`; no Java
implementation, runtime capture, or deployment authorized

## Correction notice

The first source-cardinality pass found three blocking mismatches after this draft was sent:

- `StrategyLifecycleEvent` cannot identify live `onSuccessfulDeploy`, `onBattleResult`, or Battle
  Order state writes.
- The proposed Move payload includes per-candidate ladder scratch already represented by operation
  events, while the retained `pendingMoveCardIds` set is only cleared and never populated.
- `ShieldStrategy` carries decision-affecting retained state but has no event family here.

Therefore the type list below is historical design evidence, not implementation authority. The
source-derived 4A0 matrix supersedes it and is awaiting K-2/council bijection review.

## Decision

Retire `TraceIntendedStateEvent(Kind, detail)`. Stage 4 observes completed legacy state operations,
not intentions, and prose cannot define identity. Add a sealed `TraceStateEvent` hierarchy, rename
the envelope field/getter to `stateEvents`, and bump `DecisionTrace.SCHEMA_VERSION` from 2 to 3 when
the envelope changes.

Do not use one Cartesian owner/operation/detail record. Each family gets its own record and closed
operation enum so impossible combinations cannot construct. Records contain immutable data only.
They have no callback, `apply`, game/service reference, timestamp, or RNG value. List position is the
authoritative event order.

## Common Laws

- `MutationOutcome` has only `CHANGED` and `NO_OP`. Capture failure remains
  `TraceCaptureFailure.Stage.STATE_EVENT`; it is not a fake failed mutation.
- `TrackerScope` distinguishes `OUTER_RANDO`, `OUTER_CHOSENONE`, and `HEURISTIC_SHARED` until owner
  cutover deliberately removes one.
- Use typed wrappers where String domains can be confused: decision key/id, blueprint id, physical
  card id, location id, plan/instruction id, and stable state key.
- Store float values as raw integer bits. Canonicalize unordered sets/maps into sorted immutable rows.
- `CHANGED` requires different typed before/after values where both exist. `NO_OP` requires equality
  and means the owner method actually ran. Event absence means it did not run.
- `SET` requires an after value; `CLEAR` requires an absent after value; `INCREMENT` requires the
  declared arithmetic transition.
- No detail string may supply owner, operation, subject, cause, or value. Existing logs remain the
  human narrative.
- Defensively copy every collection at construction.

## Sealed Families

Field names may follow local style, but these semantic components are mandatory.

| Event record | Closed operations | Required typed payload |
|---|---|---|
| `TrackerEvent` | `UPDATE_STATE`, `RECORD_RESPONSE`, `BLOCK_RESPONSE`, `PHASE_CHANGE`, `RESET_REPEAT`, `CLEAR` | scope; decision type/id/key and response when applicable; turn/state hash; history and sequence sizes; repeat count; detected loop length; cancel count; canonical blocked-response delta; outcome |
| `HeuristicMemoryEvent` | `FAILED_SEARCH_ADD`, `LOCAL_RESPONSE_RECORD`, `LOCAL_RESPONSE_BLOCK`, `REASSIGNMENT_RECORD`, `STATE_CHANGE_CLEAR`, `TURN_CHANGE_CLEAR` | stable memory key; typed response/card/blueprint subject; exact counter or canonical value before/after; turn/state hash; outcome |
| `StrategyLifecycleEvent` | `NEW_GAME`, `START_TURN`, `REFRESH`, `RESET` | turn; strategy phase/focus; focus-confidence raw bits; force generation/target/deficit; reserve-check count; outcome |
| `ObjectiveLifecycleEvent` | `ANALYZE`, `REFRESH`, `RESET`, `PROFILE_REPLACE` | objective blueprint identity; flipped and hydrated state; active profile/playbook identity; canonical changed flip-location/required-card rows; outcome |
| `DeckLifecycleEvent` | `ANALYZE`, `REFRESH`, `RESET`, `CATALOG_ADD`, `FAILED_PULL_INCREMENT`, `FAILED_PULL_CLEAR`, `AMSD_RETRY_SET`, `AMSD_RETRY_CLEAR` | blueprint/title key as appropriate; zone before/after; failed-pull count; AMSD turn; catalog/all-card count; outcome |
| `DeployPlanEvent` | `BEGIN`, `ADVANCE`, `RECORD_DEPLOYMENT`, `CANCEL`, `CLEAR`, `FLAG_SET`, `FLAG_CLEAR` | deploy strategy; stable instruction identity; card blueprint/physical identity; target/backup destination; cursor and deployment count before/after; waiting/allow-extra flags; closed cancel/clear cause |
| `RetryBudgetEvent` | `INCREMENT`, `CLEAR` | closed budget key; decision/action subject; integer before/after; turn; outcome |
| `BarrierMemoryEvent` | `SET`, `CLEAR` | closed barrier key; typed card/location subject before/after; turn; outcome |
| `MoveStateEvent` | `TURN_RESET`, `SET`, `CLEAR`, `INCREMENT` | closed enum for the actual retained MoveEvaluator field; typed card/location/ladder value before/after; turn; outcome |
| `OpponentIntelEvent` | `DESTINY_RECORD`, `CARD_SEEN`, `SHIELD_SEEN`, `CLEAR`, `RESET` | typed card/blueprint subject; destiny raw bits; total destiny sum/count; peek count; has-intel; canonical seen-card delta; outcome |
| `StrategicMemoryEvent` | closed enum matching each actual branch write | typed card/location/battle subject; exact counter/focus value before/after; no event for a wrapper call whose branches make no write |
| `ConcedeEvent` | `SET_PENDING`, `FIRE_PENDING`, `CLEAR_PENDING` | player id; closed cause; loss-pile inputs; pending state before/after |
| `EngineActionEvent` | `PLAYER_LOST` | player id and typed `GameEndReason` |

The current concede cause is `LOST_PILE_DEFICIT`. The ordered source sequence is
`FIRE_PENDING`, successful `PLAYER_LOST`, then `CLEAR_PENDING`. `playerLost` is observed once, never
called for evidence.

One tracker event may summarize one public tracker mutator and its internal loop/block updates. The
outer tracker, the `HeuristicAiBase` private tracker, and heuristic local memory remain separately
visible. `DeployPlanEvent.BEGIN`, `CANCEL`, and `CLEAR` must never compare equal. Move state keys must
be enumerated from actual retained fields, never field-name strings or V-tags.

## Recording API

Do not construct an event when no trace session exists. `TraceSession` exposes one typed recording
method per family. Each method checks `CURRENT` first, then constructs/appends the record inside the
existing try/catch. Construction or append failure marks the envelope `INCOMPLETE/STATE_EVENT`.

Hooks pass only already-computed arguments and values read immediately after the existing write or
successful mutator. No hook may call a mutator, analyzer, scorer, planner, `decisionMade`,
`playerLost`, cache loader, or RNG source again. Capture stays disabled by default.

## Landing Increments

1. **Stage 4A, type algebra only:** add the sealed hierarchy, enums, value records, and pure
   constructor/deep-copy tests. No envelope consumer, bot hook, owner method, or schema bump.
2. **Stage 4B, envelope and current outer hooks:** bump schema to 3; replace the old event list; move
   outer tracker observation after `recordDecision`; replace wrapper-level strategic intent with
   branch-write events; add concede set/fire/engine-action/clear. Keep both bots mirrored.
3. **Stage 4C, tracker and lifecycle owners:** add outer/private tracker, heuristic local memory,
   strategy, objective, and deck events at owner exits. Prove no mutator runs twice.
4. **Stage 4D, planner and evaluator state:** add deploy-plan, retry, barrier, move,
   opponent-intelligence, and remaining strategic events. Do not mix behavior repairs with hooks.
5. **Stage 4E, exact route corpus:** compare subtype, operation, subject, value, outcome, count, and
   order through both bot entry paths.

## Gate Corpus

- illegal constructor matrix and defensive-copy tests for every subtype;
- comparator fails on subtype, operation, payload, outcome, order, omission, and duplicate drift;
- `REFRESH + NO_OP` differs from `RESET + NO_OP` and event absence;
- no active session means no event construction and no capture failure;
- active-session event failure produces `INCOMPLETE/STATE_EVENT` without changing legacy state;
- enabled versus disabled trace returns the same response and owner state, with one mutator call;
- outer and heuristic tracker records are separately ordered;
- strategic no-op emits no event;
- concede set/fire/engine-action/clear order is exact;
- deploy begin/advance/cancel/clear and retry increment/clear remain distinct;
- direct interceptors contain no fabricated downstream events;
- normalized Rando/ChosenOne streams match except named personality waivers.

## Hold

Stage 4A through 4E are held until the source-cardinality correction replaces this draft. Enabled
capture, behavior repair, tracker consolidation, finalizer/interceptor cutover, owner retirement,
deployment, and push remain held.
