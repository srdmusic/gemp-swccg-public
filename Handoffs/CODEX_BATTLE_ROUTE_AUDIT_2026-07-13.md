# BATTLE Route And Ownership Audit

Date: 2026-07-13
Owner: Codex/Alfred
Audited commit: `97d2cb65a`
Scope: static source, replay, and log audit. No Java edit and no deploy.
Verdict: BATTLE cutover `HOLD`; typed battle route facts and initiation assessment may advance.

## Boundary correction

`Phase.BATTLE` is an engine window, not a semantic router and not one evaluator owner. All seven
engine decision shapes enter the shared outer pipeline. Four shapes appear in current reasoning
logs; `ACTION_CHOICE`, `INTEGER`, and `EMPTY` are source-reachable but not represented in those logs.

Production constructs Rando only. Rando and ChosenOne remain normalized source mirrors except the
declared Rando-only V79b parsec interceptor. Chosen parity is a protected shadow invariant, not a
second live factory path.

Sources:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/hall/HallServer.java:385`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/trace/TraceRoute.java:15`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java:525`

## Runtime route matrix

Outer pre-routing is phase-independent: V45 optional forfeit, V44 revert, V170 Undercover, V61
saga, then Rando-only V79b. Each returns before common safety, final decision tracking, and strategic
event recording.

| Shape | BATTLE contributors | Pass, cancel, and fallback |
|---|---|---|
| `CARD_ACTION_CHOICE` | Battle, Move on own-turn move text, ActionText, Pass | Combined, then Heuristic, emergency, Safety |
| `ACTION_CHOICE` | Move, ActionText, Pass | Pass may select an explicit Cancel action |
| `CARD_SELECTION` | CardSelection, Pass | Heuristic incorrectly selects at least one on some optional fallback paths |
| `ARBITRARY_CARDS` | CardSelection, Pass | Combined multi-select formatter; heuristic may return empty |
| `INTEGER` | ForceActivation, Pass | Every integer is treated as Force activation, regardless of prompt/phase |
| `MULTIPLE_CHOICE` | Direct interceptors; narrow ActionText; Pass | ActionText iterates `actionId`, not `results`; most choices fall to Heuristic |
| `EMPTY` | Pass only | Otherwise Heuristic can return literal `pass` |

Deploy and Draw are registered in CombinedEvaluator but phase-gated out. Chaos is explicitly
disabled in BATTLE. Combined applies typed-veto filtering, optional all-veto Pass, forced least-bad
selection, and V148 all-bad Pass before selecting a pre-safety winner.

Null Combined output enters the seven-shape heuristic router. Heuristic owns its own emergency,
safety, and tracker, then the outer bot runs safety/tracking again. The outer emergency reads raw
`noPass`; semantic Done/Cancel exceptions live separately in DecisionSafety. A valid empty Done can
therefore be replaced before semantic safety sees it.

The mediator is the sole engine commit point. An invalid response re-presents the decision but does
not roll back AI-side mutations.

Sources:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CombinedEvaluator.java:63`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/HeuristicAiBase.java:67`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/DecisionSafety.java:61`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/game/SwccgGameMediator.java:1278`

## Current semantic owners

### Battle initiation

BattleEvaluator currently owns engine power/ability modifiers, ObjectiveAnalyzer, FormationSafety,
BattlePredictor, reserve/DTF checks, and fallback hit economics. ActionText V25 separately scores
the same initiation action. Their contributions intentionally add today.

Sources:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/BattleEvaluator.java:141`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/ActionTextEvaluator.java:4292`

### Active-battle tactics

- BattleEvaluator owns selected fire/cancel/destiny branches.
- ActionText also owns fire, destiny, weapons, interrupts, cancel, targeting, Houjix/Ghhhk, and
  Force Push branches.
- CardSelection owns target selection.
- Force loss and forfeit span three handlers, with V159 unified scoring as another owner.
- Movement and pull/search are delegated BATTLE routes. Parent actions use Move or ActionText;
  child destination, selection, and verification use CardSelection.
- Pass uses ForceReserveService except the generic battle-action branch, which returns early at
  score `-5`.

These delegated routes must remain explicit. They cannot be absorbed into a generic BATTLE score
table without changing engine behavior.

## P0 ownership faults

### V45 bypass is text-only

V45 concludes that all cards are immune from prompt text alone. It inspects no candidate cards and
returns before CardSelection's optional-forfeit handler, making the latter unreachable on that
prompt route.

### Universal INTEGER overcapture

`ForceActivationEvaluator.canEvaluate()` accepts every INTEGER. A BATTLE integer unrelated to Force
activation can receive activation math and return the wrong scalar.

Source:
`src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/ForceActivationEvaluator.java:45`

### Wrong target can waive reserve protection

V61b waives reserve protection when any contested location has margin `>=8`, not necessarily the
candidate battle location. The typed replacement must use the actual target location.

### Predictor and veto semantics are mixed

V76 derives destiny draws from character counts, clamps to `1..4`, invokes a randomized 50-run
predictor, and implements its block as additive `-800/-500`, not a typed veto. Shadow comparison
must consume one frozen predictor result rather than rerun it.

### Permanent-weapon facts are conflated

Permanent-weapon text conflates card ownership, ability to hit, and zero forfeit. Keep these as
separate facts. Existing source audit:
`Handoffs/CODEX_PERMANENT_WEAPON_AUDIT_2026-07-11.md`.

### Finalization can reverse legal choices

The generic cancel-opponent branch precedes cancel-and-redraw valuation. Force Push exchange remains
a soft `-500`, despite replay evidence that the action can still win the merged sum. Optional
standalone forfeit calls V159; required standalone forfeit does not. Combined damage intentionally
omits V67y. These are separate routes, not one incomplete handler.

## P1 duplicated facts and score costumes

- BattleEvaluator duplicates ForceReserveService reserve/DTF semantics instead of consuming one
  ForceBudgetAssessment.
- Only FormationSafety calls create typed `hardVeto`. Large negative scores elsewhere remain
  outvotable unless terminal control flow prevents a later sum.
- The imported solo/destiny prerequisite remains exactly
  `Handoffs/CODEX_SOLO_ABILITY_ROOT_CAUSE_AUDIT_2026-07-11.md`. Do not create a second version of
  that rule.
- Preserve Steve's explicit overpower exception: target-specific evidence that an opposing solo or
  underpowered position can be profitably overpowered must remain an allowed initiation route.

## Mutation and RNG ledger

Before evaluation, ObjectiveAnalyzer and DeckOracle analyze/refresh, and DeployPhasePlanner receives
the analyzer. During scoring or fallback, mutable effects include:

- BattlePredictor RNG and DecisionSafety RNG;
- ForceReserve soak counters/recomputation;
- V169 retry budgets, Barrier target memory, and AMSD failure memory;
- OpponentDeckTracker peek recording and MoveEvaluator turn state;
- heuristic and outer tracker/loop memories;
- pending concede and BATTLE-exit `playerLost`.

Sources:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java:1429`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java:1871`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/ActionTextEvaluator.java:37`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java:319`

The trace must observe the one legacy mutation/RNG result. It must never call these producers a
second time.

## Smallest typed boundary

Add one `BattleWindowRoute` and one `BattleCandidateRole` per frozen raw ordinal:

- `INITIATE`
- `FIRE`
- `ADD_DESTINY`
- `TACTIC`
- `DELEGATED_MOVE`
- `DELEGATED_PULL`
- `GENERIC`
- `PASS`

The first owner move is intentionally narrow:

1. Build one immutable `BattleInitiationAssessment` per initiation candidate ordinal.
2. Consume typed target identity, engine power/ability, actual destiny eligibility, separate weapon
   ownership/hit-capability facts, FormationAssessment, ForceBudgetAssessment, objective facts,
   target-specific overpower evidence, and exactly one frozen predictor result.
3. `LegacyBattleInitiationAdapter` emits the current BattleEvaluator plus ActionText V25 operation
   order and sums unchanged.
4. Movement, pull/verify, target selection, active tactics, and damage remain separate adapters.
5. Consolidate ownership only after exact trace parity, then retire the old contributing arm in the
   same gated increment.

## Required fixtures

1. `BATTLE_01_AllShapesAndRawOrder`: all seven shapes, complete raw arrays, original ordinals,
   omitted candidates, duplicate ids, and source-only ACTION_CHOICE/INTEGER/EMPTY coverage.
2. `BATTLE_02_InitiationMatrix`: `B0_V25_UpperWalkway_170`, locationless fallback,
   `B0_MergedAction_VetoOR`, all-veto boundaries, and target-specific V61b overpower.
3. `B0_L2_FirstLight_NoDestiny`: import unchanged from the existing solo/destiny audit.
4. `BATTLE_03_PredictorAndWeapons`: V76 called once; attached weapon, true permanent weapon, false
   owner, and a permanent weapon that cannot hit.
5. `BATTLE_04_ActiveTactics`: fire, add destiny, throw, own/opponent cancel, cancel-redraw
   precedence, Houjix/Ghhhk, and both Force Push modes.
6. `BATTLE_05_DamageAndForfeit`: optional/required standalone forfeit, combined damage, standalone
   force loss, damage `1/2/3+`, attrition, hit/dead, immune, armed, crewed ship, and
   objective-critical card.
7. `BATTLE_06_DelegatedRoutes`: target selection, movement destination, pull/verify, and each
   mutation path.
8. `BATTLE_07_Finalization`: explicit Cancel, synthetic Pass, heuristic fallback, raw-`noPass`
   emergency versus semantic Done, and every DecisionSafety correction.
9. `BATTLE_08_BotParity`: normalized Rando/Chosen operation order and raw float bits, with only
   V79b declared as a personality divergence.

Existing named cases remain catalogued in
`Handoffs/CODEX_RANDO_DECISION_FIXTURE_SPEC_2026-07-12.md`.

## Replay and log evidence

The reasoning logs contain 815 `CARD_ACTION_CHOICE`, 98 `CARD_SELECTION`, 15 `MULTIPLE_CHOICE`,
and 7 `ARBITRARY_CARDS` decisions during BATTLE. Representative evidence includes generic draw
winning, Pass, merged fire score `490`, empty verify fallback, MULTIPLE_CHOICE heuristic fallback,
combined damage, and V45 bypass in:

- `logs/gemp-swccg.stuck-173119.log`
- `Handoffs/CODEX_COURT_EVACUATION_FAILURE_2026-07-11.md`
- `Handoffs/CODEX_REY_REPLAY_AMN_ANALYSIS_2026-07-11.md`

Court replay ids are `jmul5k9gge86f9c8` and Rando copy `bk29gs3twy175tsl`.

## Intentional deltas to preserve

- Rando-only V79b and production Rando construction.
- First-seen exact-tie retention.
- Current BattleEvaluator plus V25 additive sum until an approved consolidation delta.
- Target-specific overpower opportunity.
- Per-arm unknown policy.
- Combined-handler V67y omission.

Everything else above is duplicated ownership, route cross-talk, or missing trace coverage.

## Gate

BATTLE cutover and retirement remain `HOLD` until the complete raw route snapshot, route-complete
trace, frozen mutation/RNG ledger, and eight fixture groups pass in both bot mirrors. The typed
BattleWindowRoute and read-only BattleInitiationAssessment may proceed in shadow.
