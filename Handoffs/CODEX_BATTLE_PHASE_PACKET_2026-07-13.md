# BATTLE Frozen Cutover Packet

Date: 2026-07-13
Owner: K-2/Claude implements; Codex/Alfred independently gates
Status: `FROZEN, PIPELINED, NOT RELEASED FOR JAVA`
Order: step 7 of `CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`

This packet may be released only after the accepted-response runtime, V44/V67j pilot,
ACTIVATE+CONTROL, DRAW, PULL/SEARCH, objective facts/adapters, and DEPLOY commits pass their
independent gates. BATTLE must finish before MOVE consumes shared battle-feasibility ownership.

## Goal

Install typed BATTLE routes and one immutable initiation assessment while preserving current battle
behavior first. The behavior-neutral owner must reproduce the existing BattleEvaluator plus
ActionText additive result, operation order, raw float bits, final response, and state lifecycle
before any defect repair or legacy retirement.

## Hard Boundaries

- `Phase.BATTLE` is a window, not an owner key. Route from frozen typed facts, never phase or prompt
  text alone.
- Cover all seven wire shapes: `CARD_ACTION_CHOICE`, `ACTION_CHOICE`, `CARD_SELECTION`,
  `ARBITRARY_CARDS`, `INTEGER`, `MULTIPLE_CHOICE`, and `EMPTY`.
- Preserve evaluator registration, candidate order, original ordinals, strict-greater replacement,
  first-seen exact ties, Pass competition, fallback behavior, RNG position, and mutation timing.
- Preserve Combined's typed-veto filtering, optional all-veto Pass, forced least-bad selection, and
  V148 all-bad Pass in their current order. Preserve every existing FormationSafety typed veto,
  Rando-only V79b, per-arm unknown policy, and the combined-damage V67y omission.
- Do not merge movement, pull/search, target selection, active tactics, damage, or forfeit into one
  generic battle score.
- Do not run games, VTS, sandbox, browser, deploy, reload, restart, or push in this lane. Deployment
  remains step 12 after aggregate offline gates and independent review.
- Preserve unrelated dirty and untracked files.

## 1. Frozen Route Contract

Add one `BattleWindowRoute` and one `BattleCandidateRole` for every captured raw ordinal. The route
enum is closed to:

| Route | First-cutover owner |
|---|---|
| `INITIATE` | legacy BattleEvaluator plus ActionText V25 adapter |
| `FIRE` | existing BattleEvaluator and ActionText arms |
| `ADD_DESTINY` | existing BattleEvaluator and ActionText arms |
| `TACTIC` | existing tactic, cancel, weapon, interrupt, and response arms |
| `DELEGATED_MOVE` | current Move or ActionText parent plus CardSelection child |
| `DELEGATED_PULL` | current ActionText parent plus CardSelection selection/verify child |
| `GENERIC` | unchanged generic evaluator and fallback path |
| `PASS` | unchanged explicit Cancel, Pass, Done, and synthetic fallback semantics |

The snapshot retains complete raw arrays, testing/selectable state, duplicate ids, omitted
candidates, original ordinals, defaults, and pass eligibility. Missing, misaligned, or unknown facts
remain legacy-unowned. Classification is read-only and must not invoke an evaluator or finalizer.

## 2. Behavior-Neutral Initiation Owner

Build one immutable `BattleInitiationAssessment` per `INITIATE` candidate ordinal. It consumes the
actual target identity, engine power and ability, destiny eligibility, separate weapon ownership and
hit-capability facts, FormationAssessment, ForceBudgetAssessment, objective intent, the exact optional
accepted DEPLOY overpower/rescue intent, target-specific overpower evidence, and exactly one frozen
BattlePredictor result. The deploy intent is immutable evidence only: BATTLE rechecks current legality
and opportunity, owns the initiation decision, and never re-emits the DEPLOY score.

`LegacyBattleInitiationAdapter` must emit the generic BattleEvaluator and ActionText V25 operations
once, in their current order. At the BATTLE cutover, move the objective-specific `V29.9 HUNT DOWN`,
`V35 VADER EXPENDABLE`, and `V35 HUNT DESTINY` operations to `ObjectiveBattleAdapter`, then exclude
exactly those operations from the live legacy adapter. Preserve the generic barrier-risk base and all
other BattleEvaluator operations. Require one objective emission per action. Preserve each operation
and its IEEE-754 float bits; do not reassociate, pre-sum, normalize, or rerun either arm. The merged
sum, hard-veto state, V148 result, pre-final winner, final wire response, and intended state events
must match exactly.

The trace observes the single legacy fact, RNG, and mutation result. It must not refresh
ObjectiveAnalyzer or DeckOracle, rerun BattlePredictor or DecisionSafety RNG, consume reserve soak
twice, or duplicate retry, Barrier, failed-pull, opponent-peek, move-turn, tracker, concede, or
`playerLost` mutations.

## 3. Pipelined Execution

1. **Coherent edit:** add typed route facts, raw snapshots, immutable assessments, route-complete trace,
   owner cutover, named repair assertion groups, and predecessor disablement without running tests.
2. **Final production state:** route owned candidates through the legacy/objective adapters and the
   accepted-response finalizer exactly once. Unknown routes stay legacy. Disable only replaced live
   call sites before verification, while retaining predecessor source for direct parity fixtures.
3. **Single verification:** run every baseline-parity and repair assertion group below once against the
   final disabled production state. Repairs are explicit expected deltas inside this one BATTLE gate;
   there are no intermediate commits or independent repair gates.
4. **Deferred deletion:** delete no legacy source in this phase. Record zero-live-caller proof and keep
   predecessor source/comments intact for step 10. Keep all unrelated interceptors and contributors live.

## 4. Required Repair Gates

- **V45 text-only forfeit:** replace prompt-only immunity with candidate-card facts. Keep optional,
  required, standalone, and combined damage routes distinct. Disable the mirrored direct interceptor
  call sites during the coherent edit; the single gate compares retained predecessor behavior with the
  new owner through permuted-result fixtures. Retain source/comments for physical deletion in step 10.
- **INTEGER overcapture:** consume the typed origin contract from ACTIVATE+CONTROL. An unrelated
  BATTLE integer must not enter ForceActivationEvaluator; absent or unknown origin stays legacy.
- **Wrong-target reserve waiver:** calculate V61b for the candidate battle location only. Preserve
  Steve's target-specific opportunity to overpower an underpowered solo or low-power position when
  the battle is legal and worthwhile.
- **DEPLOY intent handoff:** consume the exact optional accepted deploy overpower/rescue intent with
  physical target identity. Do not emit its score again and do not treat absence as a veto. BATTLE owns
  current legality, predictor, destiny safety, and the final initiate/pass choice.
- **Predictor and pseudo-veto:** freeze one predictor result. Preserve V76 `-800/-500` additive
  behavior for baseline parity, then convert only an owner-proven arm to `hardVeto` after maximum
  positive-stack boundary math plus Done/Pass fixtures. Do not weaken or duplicate FormationSafety.
- **Weapon truth:** keep permanent-weapon ownership, ability to hit, and zero-forfeit truth as three
  independent facts.
- **Finalization reversal:** preserve legal Cancel/Done choices before raw-`noPass` emergency logic,
  evaluate cancel-and-redraw before generic opponent-cancel ownership can mask it, and finalize once.
  Force Push, optional/required forfeit, combined damage, and standalone force loss remain separate
  route repairs.

## 5. Both-Bot Fixture Gate

Run every group through one shared abstract harness with thin Rando and ChosenOne adapters:

1. `BATTLE_01_AllShapesAndRawOrder`: all seven wire shapes, complete arrays, ordinals, omitted
   candidates, duplicate ids, and source-only `ACTION_CHOICE`/`INTEGER`/`EMPTY` coverage.
2. `BATTLE_02_InitiationMatrix`: `B0_V25_UpperWalkway_170`, locationless fallback,
   `B0_MergedAction_VetoOR`, all-veto/V148 boundaries, and target-specific V61b overpower. Consume
   accepted DEPLOY intent without score duplication and prove actual `INITIATE` outcomes for a legal
   underpowered lone-solo target and a legal low-power enemy site; separately prove objective-required
   holding and unchanged `B0_L2_FirstLight_NoDestiny` safety can still prevent an unsafe battle.
3. `BATTLE_03_PredictorAndWeapons`: predictor called once; attached weapon, true permanent weapon,
   false owner, zero-forfeit separation, and a permanent weapon that cannot hit.
4. `BATTLE_04_ActiveTactics`: fire, add destiny, throw, own/opponent cancel, cancel-redraw
   precedence, Houjix/Ghhhk, and both Force Push modes.
5. `BATTLE_05_DamageAndForfeit`: optional/required standalone forfeit, combined damage, standalone
   force loss, damage `1/2/3+`, attrition, hit/dead, immune, armed, crewed ship, and
   objective-critical card.
6. `BATTLE_06_DelegatedRoutes`: target selection, movement destination, pull/verify, parent-child
   ownership, and every associated mutation path.
7. `BATTLE_07_Finalization`: explicit Cancel, synthetic Pass, heuristic fallback, semantic Done
   versus raw-`noPass` emergency, every DecisionSafety correction, checked rejection, bounded retry
   history, accepted disposition, post-accept mutation, and trace on/off wire parity.
8. `BATTLE_08_BotParity`: normalized operation order and raw float bits for every route, with only
   documented bot identity fields and Rando-only V79b permitted to differ.
9. `BATTLE_09_ObjectiveOwnership`: retained predecessor versus `ObjectiveBattleAdapter` parity for
   `V29.9 HUNT DOWN`, `V35 VADER EXPENDABLE`, and `V35 HUNT DESTINY`; generic barrier risk and
   ActionText V25 remain in the legacy adapter, with exactly one objective emission per action.

## 6. Release Gate

BATTLE remains `HOLD` until the complete raw-route snapshot, route-complete trace, frozen mutation
and RNG ledger, and all nine fixture groups pass in both bot mirrors. The gate must prove exact
candidate and operation order, raw float bits, veto state, Pass eligibility, pre-final winner, final
response, rejection lifecycle, and intended state events.

Return one report with commit and parent SHAs, exact paths, the single focused BATTLE test
command/counts, both-bot parity evidence, predictor-once proof, lifecycle/trace matrix, every
intentional repair delta, every retained legacy owner, mirrored call-site disablement and
zero-live-caller proof. Physical deletion proof and aggregate offline testing belong to steps 10 and
12 respectively, not this phase.

## Hard Stops

- any predecessor gate is missing
- routing needs phase or prompt text alone
- BattleEvaluator plus ActionText operation order or raw bits drift before an approved repair
- an objective-specific V29.9/V35 operation is emitted by both the legacy and objective adapters, or
  generic barrier risk/ActionText V25 is removed from the legacy adapter
- accepted DEPLOY intent is rescored, loses physical target identity, bypasses current BATTLE legality,
  or globally suppresses legal overpower opportunities when absent
- V148 or FormationSafety is bypassed, weakened, duplicated, or made outvotable
- BattlePredictor, finalizer, safety, or a mutable producer runs twice
- delegated MOVE/PULL or damage/forfeit routes lose separate ownership
- Rando and ChosenOne differ outside declared identity fields and V79b
- any legacy deletion lacks exact owner proof and before-and-after fixtures
- V45 predecessor source is physically deleted before step 10, or its mirrored live call sites remain
  reachable after this phase
- an intermediate repair gate, extra phase commit, or premature aggregate test is required
- any game, live server, deployment, database, deck-library, or unrelated-file change is required

Sources: `Handoffs/CODEX_BATTLE_ROUTE_AUDIT_2026-07-13.md` and
`Handoffs/CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`.
