# BATTLE Phase Owner Cutover Gate

Date: 2026-07-14 PDT

Status: `PASS, READY TO COMMIT AND DEPLOY`

## Identity

| Item | Value |
|---|---|
| Worktree | `/Users/steve/gemp-swccg-battle-phase-codex` |
| Branch | `codex/battle-owner-cutover` |
| Parent | `494d6f4bc` |
| Frozen packet | `/Users/steve/gemp-swccg-public/Handoffs/CODEX_BATTLE_PHASE_PACKET_2026-07-13.md` |
| Packet SHA-256 | `85498b28364acfaf8994813cffc1c26fd5f0d0fa16cbf32fbf32eab1459b778e` |
| Route audit | `/Users/steve/gemp-swccg-public/Handoffs/CODEX_BATTLE_ROUTE_AUDIT_2026-07-13.md` |
| Route audit SHA-256 | `8f61dd92be7d59e8f0c5677f6aa72c77e91b8d7051a25cd4dce83d267e43ac4f` |

## Boundary Results

| Gate | Result |
|---|---|
| Focused BATTLE/objective/snapshot/finalizer/formation/tie corpus | `130 tests, 0 failures, 0 errors` |
| Lifecycle/mediator/PULL/DEPLOY/trace/parity/route regressions | `185 tests, 0 failures, 0 errors` |
| Affected reactor package | `PASS` |
| Compiled owner markers | `PASS`, `BattlePhaseOwner` present three times in each bot AI class |
| Rando/ChosenOne changed-hunk normalization | `PASS`, AI entry, BattleEvaluator, ActionTextEvaluator, and DecisionContext |
| Introduced forbidden type/title patterns | `NONE` |
| Source whitespace audit | `git diff --check` clean |
| Game, browser, VTS, or sandbox execution | `NONE` |
| Push or deployment during gate | `NONE` |

The mediator regression output intentionally includes ERROR diagnostics from fixtures that exercise rejected answers,
chain limits, acceptance-callback faults, and injected game-state read failures. Maven exited 0 and all assertions passed.

## Ownership Matrix

| Wire | Typed origin/semantic | Owner result |
|---|---|---|
| Initiate battle | `BATTLE_ACTION` + `BATTLE_INITIATE` | Exact target assessment and one finalized compatibility wire |
| Fire weapon | `BATTLE_ACTION` + `BATTLE_FIRE` | Exact firing action and target facts |
| Play next battle action | `BATTLE_ACTION` | Typed battle-action choice |
| Battle power | `BATTLE_POWER` | Typed power-segment choice |
| Cancel previous destiny draws | `BATTLE_DESTINY_SELECTION` | Typed redraw choice |
| Lose or forfeit card | `BATTLE_FORFEIT` | Required/optional physical-card selection |
| Unknown or malformed shape | none | Legacy-unowned; no inferred ownership from prompt or phase |

Only one battle `CardsSelectionDecision` producer exists under the battle timing package. Its lose/forfeit decision is
stamped `BATTLE_FORFEIT`, including the optional-forfeit flag. Damage resolution remains with the engine.

## Frozen Assessment

One `BattleAssessment` is captured per initiation candidate and reused by both bots. It carries:

- Exact candidate and target identity.
- Friendly and opponent power and ability.
- Permanent-weapon facts, including the legacy opponent lightsaber `+5` and other weapon `+3` distinction.
- Formation and target-overpower facts.
- Same-turn completed DEPLOY intent as evidence, without re-emitting DEPLOY scores.
- Objective-relevant battle facts.
- Exactly one battle-predictor result.

Typed evaluation never reruns the predictor when the frozen result is UNKNOWN. A direct bot-level regression fails if
the DeckOracle predictor is touched after capture. Legacy-untyped fallback retains its predecessor behavior.

## Preserved Boundaries

- V76 opportunity penalties remain additive `-800/-500`; they are not hard vetoes.
- Force Push exchange remains additive `-500`; it is not a hard veto.
- Force Push battle mode no longer also receives the unrelated Stunning Leader exclusion penalty.
- Existing candidate order, tie behavior, and compatibility response wires remain unchanged.
- Objective battle contributions moved to `ObjectiveBattleAdapter`; generic battle policy remains outside the adapter.
- MOVE and PULL battle-window actions remain delegated to their own phase owners.
- Damage resolution and nonchooser engine effects remain engine-owned.
- Source-only, INTEGER, and EMPTY shapes without a BATTLE origin remain legacy-unowned.

## Intentionally Retained

- Legacy fallback code for wire shapes not yet proven by an origin stamp.
- Live-board reads used by unrelated retained battle rules such as barrier, Force, and reserve policy.
- Physical predecessor bodies needed for the later interceptor-retirement proof.
- Score tuning, including deploy-weight consolidation and solo deploy penalties.

This is a narrow BATTLE ownership cutover, not a claim that every historical battle rule has been physically deleted.

## Commands

```bash
mvn -q -pl gemp-swccg-server -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=BattleAssessmentCaptureTest,BattleRouteAndFactsTest,BattleDeployIntentTest,BattlePhaseOwnerTest,BattleWeaponFactsTest,RandoBattleActionTextParityTest,TheChosenOneBattleActionTextParityTest,RandoBattlePredictorConsumptionTest,TheChosenOneBattlePredictorConsumptionTest,RandoBattleCutoverIntegrationTest,TheChosenOneBattleCutoverIntegrationTest,ObjectiveAdapterContractTest,DecisionActionSemanticContractTest,DecisionSnapshotTest,ResponseFinalizerContractTest,FormationSafetyCountTest,CombinedEvaluatorTieTest test

mvn -q -pl gemp-swccg-server -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=RandoCalAiLifecycleTest,TheChosenOneAiLifecycleTest,SwccgGameMediatorAiLifecycleTest,RandoCalAiTraceHookTest,TheChosenOneAiTraceHookTest,CombinedEvaluatorTraceTest,EngineAwaitingDecisionContractTest,RandoPullCutoverIntegrationTest,TheChosenOnePullCutoverIntegrationTest,RandoDeployCutoverIntegrationTest,TheChosenOneDeployCutoverIntegrationTest,ActivateControlRouteResolverTest,DeployRouteAndFactsTest,DeployPhaseOwnerTest,PullRouteAndFactsTest,PullPhaseOwnerTest,ObjectiveProfileResolutionParityTest test

mvn -q -pl gemp-swccg-server -am package -DskipTests
```

All Maven commands ran against the isolated BATTLE worktree. They did not modify or restart the live server.
