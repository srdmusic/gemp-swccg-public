# Batch 2A implementation report

Date: 2026-08-12
Base: `9687ad56e27bed4b82c929f718d7bea9b0b6f10d`
Branch: `codex/rando-batch2-exact-formation-react`
Status: implemented and verified in an isolated worktree; this report records the pre-commit review snapshot

## Implemented scope

- Both bot planners now rank the full current plus planned friendly formation at each exact target. Planned cards alone retain the per-instruction base-power score.
- `DeploymentInstruction.abilityContribution` is a `float`. Generic and repilot instructions use current engine ability, including intrinsic/permanent-pilot ability for starships and vehicles and character ability without permanent-pilot inclusion. Fractional ability remains fractional.
- EOP package selection uses ship ability plus external-pilot ability. Emitted instructions own those values separately. A permanent-piloted ship plus external pilot is `[1.0, 2.0]`, total `3.0`; an unpiloted ship plus that pilot is `[0.0, 2.0]`.
- The previously approved but absent `deploy-plan-ranking-isolated-packet` semantic owner is NEW behavior. It emits one ADD `-150` operation per proved exposed target.
- The common public reader proves only the strongest single active, public, in-play opponent character, starship, or vehicle with a live move-as-react permission and an executable exact-target route.

## Exact E2 boundary and score math

E2 requires all of the following:

```text
theirCardCount == 0
triggerKnowable
exposureProven
!formationPenaltyExempt
strongestReactEffectivePower > 0
strongestReactEffectivePower >= 2.0 * postOurPower
```

`postOurPower` and `postOurAbility` are unrounded current plus planned values. Equality fires. The characterized boundary is silent at `9.999` against post power `5.0` and fires at `10.0`.

The frozen examples remain:

- Small exposed packet: `-77`; larger safe packet: `83`.
- Consolidated equal total power: `85`; two exposed split packets: `-200`.
- Existing contested example: `123`, with no E2 operation.

Exact V297 formation plans, exact EOP Bunker garrison plans, and a planned Spy at the exact target are the only waivers. A current opponent card, including power zero, makes the target contested and keeps E2 silent.

## Public route proof

- The trigger requires opponent absence, a positive bot Force drain at the future occupied target, and no Force-drain prohibition.
- Movers are active opponent-owned public characters, starships, and vehicles. Permission sources are all active public cards because an opponent-owned location can grant the reacting player permission.
- Live `MAY_NOT_REACT`, `MAY_NOT_REACT_TO_LOCATION`, intrinsic permissions, source-granted player/mover/target filters, current Force affordability, piloting, reach, and exact target are enforced.
- The reader probes the eight movement factories used by `AbstractDeployable`: landspeed, hyperspeed, without hyperspeed, sector movement, land, take off, enter, and exit. All are called as reacts. Their shipped movement checks enforce `MAY_NOT_REACT_FROM_LOCATION`. Only landspeed receives `changeInCost`, matching the engine's current react construction.
- Non-finite effective power fails closed. Hidden cards, ordinary movement, passengers, joined reacts, multiple-mover sums, remote power, and speculative future Force never qualify.

The helper is intentionally not replaced by the shorter live `getMoveAsReactOption` or `getMoveAsReactAction` paths. Those bind to `GameState.getBattleOrForceDrainLocation()` and cannot prove a planned future drain target. Raw topology or target-filter checks would omit blueprint legality, affordability, piloting, and movement restrictions.

## Held residuals

No generic topology or adjacency model, counterdeploy envelope, ordinary next-turn movement, passenger or multi-mover projection, wave staging, site-loss memory, escort sequencing, anti-alpha logic, or broad movement policy was added. `FormationSafety`, per-action evaluators, `StrategyController`, card Java, engine Java, objective data, decks, and database state are unchanged.

## Verification

- Focused policy, adapter, source-ownership, public-react, and EOP ring: `58/0/0/0`.
- Expanded deploy, formation, Endor, plan-copy, and mirror ring: `113/0/0/0`.
- Clean `gemp-swccg-server` reactor compile: passed.
- Normalized Rando and Chosen One planner equality: passed.
- Normalized Rando and Chosen One instruction-object equality: passed.
- Allowlist, whitespace, and diff checks: passed.

These checks precede the eventual local implementation commit. No package, deployment, restart, runtime load, game, log, replay firing, push, or pull request proof exists.

## Changed paths

Production:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/DeployPlanRankingPolicy.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/PublicImmediateReactAnalyzer.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/strategy/DeployPhasePlanner.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/strategy/DeployPhasePlanner.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/strategy/DeploymentInstruction.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/strategy/DeploymentInstruction.java`

Tests:

- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/DeployPlanRankingPolicyTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/DeployPlanRankingAdapterParityTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/DeployPlanRankingSourceOwnershipTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/strategy/PublicImmediateReactAnalyzerTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/strategy/EndorOperationsEndorSystemPlannerTest.java`

History and report:

- `resources/AI_CHANGELOG.md`
- `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`
- `outputs/rando-batch2-2026-08-12/implementation_report.md`
