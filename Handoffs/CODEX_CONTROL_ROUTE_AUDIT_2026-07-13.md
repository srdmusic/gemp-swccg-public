# CONTROL Route And Ownership Audit

Date: 2026-07-13
Owner: Codex/Alfred
Audited commit: `97d2cb65a`
Scope: static source audit only. No Java edit and no deploy.
Verdict: CONTROL cutover `HOLD`; typed raw-route facts and fixtures may advance.

## Boundary correction

CONTROL is an engine phase window, not one closed AI route. Its canonical chooser is
`CARD_ACTION_CHOICE`, but start/end triggers and selected actions may suspend on any of the seven
`AwaitingDecisionType` values while `Phase.CONTROL` remains active. Registry ownership cannot be
derived from evaluator file alone. Applicability depends on decision shape, prompt and candidate
text, turn ownership, phase/window, and exact raw parameters.

Primary sources:

- `src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/timing/processes/turn/ControlPhaseGameProcess.java:17`
- `src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/timing/processes/turn/general/PlayersPlayPhaseActionsInOrderGameProcess.java:38`
- `src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/timing/processes/turn/general/EndOfPhaseGameProcess.java:28`
- `src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/decisions/AwaitingDecisionType.java:6`

## Engine route

| Stage | Legacy behavior |
|---|---|
| Enter | Set CONTROL, snapshot, enqueue the start-of-phase trigger. |
| Choose | Rotate through both players and offer `getTopLevelActions` as `Choose Control action or Pass`. Raw flags are `yourTurn`, `autoPassEligible=true`, `noPass=false`, `noLongDelay=false`, and `revertEligible=true`. |
| Action | A successful action resets consecutive passes. An aborted action re-prompts the same player without resetting the prior pass count. |
| Pass | Empty response is Pass. Consecutive passes by every player end CONTROL. |
| Exit | Enqueue the end-of-phase trigger, then transition to DEPLOY. Either trigger may open child decisions. |

## Raw decision matrix

| Decision type | Required frozen input and response shape |
|---|---|
| `CARD_ACTION_CHOICE` | Ordinal `actionId`; card, blueprint, testing, backside, horizontal, and action-text arrays; empty is Pass. |
| `ACTION_CHOICE` | Ordinal action plus blueprint, testing, backside, horizontal, and text arrays; empty is invalid. |
| `MULTIPLE_CHOICE` | Numeric index into `results`, plus `defaultIndex`. |
| `INTEGER` | Bounded scalar with optional `min`, `max`, and `defaultValue`. |
| `CARD_SELECTION` | Real card ids, comma-separated, constrained by `min/max`; no `selectable` array. |
| `ARBITRARY_CARDS` | `tempN` ids plus blueprint, testing, backside, horizontal, `preselected`, `selectable`, `cardText`, and `returnAnyChange`. |
| `EMPTY` | `timeoutValue`; response ignored. |

Sources are the seven concrete decision classes under
`src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/decisions/`.

## AI precedence

Rando's outer order is:

1. Build context and track game state.
2. V25/V67aw concede mutation and outer loop tracking.
3. Direct V45, V44/V67j, V170, V61, and Rando-only V79b interceptors.
4. Chaos, CombinedEvaluator, then heuristic fallback.
5. Raw-`noPass` emergency, DecisionSafety, outer tracker record, and strategic record.

Direct interceptors return before common safety and final tracker/strategic recording, but after
earlier state mutations. V79b also refreshes objective flip state.

CombinedEvaluator order is ForceActivation, Deploy, Battle, Move, Draw, CardSelection, ActionText,
then Pass. Scores merge additively by first-seen action id; exact ties retain the earlier candidate.

Primary sources:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java:524`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CombinedEvaluator.java:67`

## P0 cross-talk

### Universal INTEGER capture

`ForceActivationEvaluator.canEvaluate()` accepts every `INTEGER`, without prompt or phase typing.
An unrelated CONTROL integer can therefore receive activation arithmetic and return an
activation-derived value.

Source:
`src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/ForceActivationEvaluator.java:45`

### MULTIPLE_CHOICE routes are advertised but not populated

ActionText advertises selected MULTIPLE_CHOICE handling, but context construction does not map
`results` into action ids/text. Those branches emit no candidates and fall through to fallback.

Sources:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java:1268`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DecisionContext.java:44`

### Legal selection can be replaced or rejected

- V148 may synthesize a legal empty response for `min=0` cancel semantics.
- The outer raw-`noPass` emergency can replace that empty response before V148-aware
  DecisionSafety runs.
- `CardsSelectionDecision(min>1)` has no `selectable` array. Current evaluator formatting and
  Safety's selectable clamp cannot fill the required count. A single id is rejected and the
  mediator re-sends the same decision while prior AI-side mutations remain applied.

Sources:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CombinedEvaluator.java:520`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java:962`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/DecisionSafety.java:177`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/game/SwccgGameMediator.java:1327`

### Fallback duplicates ownership

`HeuristicAiBase` owns its own tracker, safety, failed-search, and reassignment memory. The outer bot
then repeats safety and tracking after fallback. A replacement route must observe exact intended
events and RNG use before retiring either owner.

## CONTROL drain precedence

The current drain path is ordered and contains terminal returns. It cannot be flattened into one
unordered additive table.

| Order | Arm | Actual operation |
|---:|---|---|
| 1 | V24.15 | Drain `<=0`: `-9999`, terminal. |
| 2 | V189 | Cost gap `>=2`, or net `-1` budget failure: `-2000`, terminal; query failure opens the gate. |
| 3 | V25 | Dynamic non-battleground plus opponent Simple Tricks: `-9999`, terminal. |
| 4 | Untagged Battle Order | Cannot afford or would starve cheapest deploy: `-50`, terminal; no deployable body: `+70`, terminal. |
| 5 | V140 | Engine drain cost `0`: `+60`, terminal; skips later drain arms. |
| 6 | V104 | Drain `<=1`: `-2000`, nonterminal; suppresses only V48/V52 turn logic. |
| 7 | V52/V48 | Without V104, turn `>=3`: `+50`; turns 1-2: `-50`. |
| 8 | Untagged non-Battle Order | Deployable body: `+50`; none: `+70`. |
| 9 | V52 multi-site | Drain `>=3`: `+300`; drain `>=2`: `+200`; otherwise two-plus drain sites: `+100`. |
| 10 | V29.9 | Hunt Down: `+40` at two-plus Light icons, then `+30` always. |

Primary source:
`src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/ActionTextEvaluator.java:5829`

Other live CONTROL contributions include V24.2 `+80`, V52 cancel `-9999/+30`, V29.14 `+200`,
V23 `-300/-100`, V184 `+300`, V192 `+150` plus location/weapon/device/download bonuses, and
separate V67ak `+800`.

## P1 dominance and ownership

- Only FormationSafety calls produce typed `hardVeto`. `-100000`, `-9999`, and `-2000` labels are
  additive score costumes and remain outvotable unless terminal control flow prevents later sums.
- Battle candidates merge BattleEvaluator's `+100` base with the ActionText battle stack.
  V76 `-800/-500` is not a typed veto.
- Move candidates expose all 49 MOVE arms during CONTROL. The Move finalizer applies score-costume
  veto `-100000`, fine clamp `+/-2800`, then rank bases R4 `+20000`, R3 `+12000`, R2 `+6000`,
  and R1 `0/-50`.
- Canonical CONTROL Pass starts at `5`, can become `2..116` early and up to `123` later after hand,
  V27.1, and V27 adjustments. Registry prose saying Pass is approximately `5-8` is not a safe
  dominance boundary.

## Registry corrections required

- V48 is turns 1-2; V52's later branch starts at turn 3.
- V104, V24.15, V25, V189, V76, and MOVE "vetoes" are generally scores, not typed vetoes.
- V140 must record its real `+60` and terminal precedence.
- Untagged Battle Order and non-Battle Order drain baselines need first-class rows.
- V79/V103 parsec ActionText scoring is ineffective because MULTIPLE_CHOICE `results` never enter
  the evaluator context. Rando's direct V79b route remains the effective handler.
- Universal INTEGER routing, heuristic CONTROL drain `+440`, fallback precedence, merge order, and
  mutation ownership need explicit registry entries.

## Parity and mutation boundary

The eight evaluator pairs, DecisionSafety, and EvaluatedAction are normalized source mirrors.
Top-level bots differ by Rando's V79b block. Chosen's shared fallback imports Rando DecisionSafety
and DecisionTracker. Only Rando is instantiated by the live hall factory; Chosen parity remains a
shadow/source invariant.

Persistent observations include tracker resets/records, shield and opponent-card memory, Battle
Order state, deferred loss, objective/deck refreshes, ForceReserve cache/soak counters, move-state
reset, retries, barrier memory, fallback memories, strategic events, and chaos/Safety/BattlePredictor
RNG. No shadow route may call these producers a second time.

## Smallest typed seam

1. Build the complete raw snapshot inside `decide`, after reading type/text/phase and before
   `trackGameState`.
2. Model CONTROL as a phase window containing one typed decision-shape route, not as an evaluator
   file route.
3. Immediately before legacy `evaluateForceDrain`, capture immutable `ControlDrainFacts` once.
4. Run pure `ControlDrainAssessment` over those facts. Emit ordered raw-float operations plus an
   explicit terminal/nonterminal status.
5. Legacy remains authoritative. Do not rerun modifier queries, BattlePredictor, analyzers,
   trackers, caches, or RNG during comparison.

## Required fixtures

1. `CONTROL_01_WindowAndRawOrder`: start/chooser/end routing, both players, action reset, aborted
   action, two-pass completion, all raw arrays, and merge order.
2. `CONTROL_02_DrainPrecedenceMatrix`: every drain row above plus V24.2, V52 cancel, V23, V29.14,
   V184, and V192; exact float bits and terminal returns.
3. `CONTROL_03_MixedEvaluatorMerge`: interleaved move/drain/battle/pull candidates, evaluator
   insertion order, duplicate-id sums, and exact ties.
4. `CONTROL_04_VetoPassSafety`: score-costume versus typed veto, real and synthetic Pass,
   all-veto optional/forced, V148, raw-`noPass`, and pre/post-safety response.
5. `CONTROL_05_DecisionShapeRouteMatrix`: all seven shapes and every raw field, direct/evaluator/
   fallback/emergency routes, malformed arrays, and `CARD_SELECTION min=2` rejection/re-prompt.
6. `CONTROL_06_MutationAndBotParity`: ordered intended-mutation ledger and RNG count for normal,
   direct, fallback, and rejected responses; normalized bot equality with only V79b declared.

## Gate

CONTROL cutover and retirement remain `HOLD` until the complete raw snapshot, route-complete trace,
six fixtures, exact drain terminal precedence, and mutation/RNG ledger pass in both bot mirrors.
