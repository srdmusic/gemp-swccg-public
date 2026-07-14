# SETUP Route Audit

Date: 2026-07-13
Owner: Codex/Alfred
Read-only source baseline: `55c22fdde`
Verdict: route contract `ADVANCE`; owner movement `HOLD`

## Route boundary

`PLAY_STARTING_CARDS` is a window containing several routes, not one evaluator lane:

1. Start-game acknowledgement is `MULTIPLE_CHOICE` and falls through to
   `HeuristicAiBase`, which currently returns index zero.
2. The Starting Effect itself is auto-selected, but its play action can open child decisions.
3. Objectives are auto-discovered and written to `GameState` before their stacked play actions run.
4. A non-objective starting location uses `ARBITRARY_CARDS`, `min=1`, `max=1`.
5. A starting interrupt uses `ARBITRARY_CARDS`, `min=0`, `max=1`, but canonical setup input omits
   `noPass`; the legacy default is true, so the AI cannot pass that prompt.
6. Saga choice is a direct outer interceptor and bypasses the common finalizer and tracker path.

Primary engine anchors:

- `PlayStartingEffectsGameProcess.java:34-70`
- `PlayStartingLocationsAndObjectivesGameProcess.java:68,234-330`
- `PlayStartingInterruptsGameProcess.java:53`
- `RandoCalAi.java:505-828`
- `SwccgGameMediator.java:1278-1327`

Only Rando is created by the live factory. ChosenOne remains a source-parity target, not proof of a
second live path.

## Cross-evaluator surface

- `ForceActivationEvaluator` accepts every `INTEGER` prompt without a setup phase boundary.
- `BattleEvaluator` can join setup action choices when prompt or action text says battle/initiate.
- `MoveEvaluator` can join own-turn setup action choices when movement keywords match.
- `CardSelectionEvaluator` accepts every `CARD_SELECTION` and `ARBITRARY_CARDS` prompt.
- `ActionTextEvaluator` accepts action choices and selected multiple-choice prompts.
- `PassEvaluator` requires `noPass=false` and `min=0`; canonical starting interrupts therefore do
  not get a Pass candidate.
- `DeployEvaluator` and `DrawEvaluator` are phase-gated and do not own setup.

The CardSelection branch order is behavior. V21 terminal guards run before setup-specific scoring,
and several later branches return or continue differently. A cleanup that merely groups V-tags by
name can change which contributions are suppressed.

## Raw-input loss and state hazards

- `ArbitraryCardsSelectionDecision` emits temporary ids, blueprints, testing text, selectable flags,
  and preselected flags. Legacy context construction copies only a subset and never installs the
  testing-text array.
- Temporary ids bypass real-card-id parsing. A rule proven on a real id is not necessarily live for
  the canonical setup prompt.
- Normal setup decisions can refresh or mutate `DecisionTracker`, `ObjectiveAnalyzer`, `DeckOracle`,
  and `DeployPhasePlanner` before evaluator eligibility is known.
- Fallback uses a second private tracker in `HeuristicAiBase`.
- Direct saga returns bypass final validation, decision recording, and strategic tracking.
- Objective JSON starting-location/effect/interrupt sets currently have no runtime consumers.

## Registry corrections

- V21 is a real-id turn-zero score guard, not a hard veto. Temporary ids bypass it.
- V22 reserve-card mirror is incomplete because legacy context drops testing text.
- V29.14 can stack two `+1000` arms; it is not first-match scoring.
- V29.15 conflates the actual direct V61 saga route with unreachable/labeled evaluator blocks.
- V43 is the live starting-interrupt route with base and additive preference scores.
- V67p does not protect the canonical temporary-id starting-interrupt prompt.
- V186 has two distinct setup arms and temporary deploy-location candidates can retain only the
  base score after real-id parsing aborts the generic tail.
- Setup ownership is missing for V24.10, V25, V26, the real V61 route, untagged base scores,
  generic card-type tails, and fallback behavior.

The domain registry remains research evidence, not migration authority, until these routes are
represented as exact arms.

## Smallest neutral seam

Do not add another Combined evaluator. Add a shared pure `SetupRoute` and build a shadow
`DecisionSnapshot` from the complete raw arrays before service mutation. Do not populate a missing
legacy array as a side effect.

A `SetupStartingSection` may later emit ordered typed contributions through compatibility calls at
the existing CardSelection sites. It must return explicit `terminalDecision` and
`terminalCandidate` flags so early returns and later suppression remain exact. Keep evaluator
eligibility, dispatch order, candidate ids, generic tails, fallback, and direct saga behavior
unchanged until each route has its own owner-move gate.

## Required frozen fixtures

1. `MC_SETUP_FALLBACK`: acknowledgement and same-location conversion, including current `No` choice.
2. `START_INTERRUPT_TEMP`: Prepared Defenses, Surface Defense, and Tentacle with temporary ids;
   freeze V43, V67p non-fire, and forced selection despite `min=0`.
3. `START_LOCATION_STACK`: Funeral Pyre stacking, battleground status, and side-specific Sith markers.
4. `SETUP_ROUTE_MATRIX`: every setup text/type plus INTEGER and action-choice evaluator collisions.
5. `REAL_ID_V21_TERMINAL`: banned and allowed candidates, exact `-500/+100`, no downstream scores.
6. `UNKNOWN_SETUP_MATRIX`: V22/V25/V80/V126/V186/V187, universal guards, tails, and terminal bans.
7. `SAGA_DIRECT`: shuffled saga choices and unknown deck name; freeze skipped finalizer/tracker events.
8. `BOOTSTRAP_RESERVE`: no-objective first prompt, objective-revealed child, analyzer/oracle writes,
   dead JSON slots, and inert blueprint-only mirrors.

No current AI setup fixture proves route, contribution, winner, response, or mutation parity. Engine
startup tests are not a substitute.
