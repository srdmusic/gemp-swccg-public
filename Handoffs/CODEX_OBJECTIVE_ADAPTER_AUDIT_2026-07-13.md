# Objective Adapter Audit

Date: 2026-07-13
Reviewer: Codex/Alfred orchestration lane
Source point: `e447d306d1db60318dba459ae215d07d6cdb22ed`
Verdict: objective adapter cutover `HOLD`; facts producer and fixtures may advance

Current-baseline refresh: compared through gated HEAD
`35dea5c5a56fb9fbb1dabdae74107341f1c676ba`. `ObjectiveAnalyzer`,
`DeployPhasePlanner`, `DeployPhaseScript`, `DeckOracle`, `CharacterDeploySiteEvaluator`, the
objective-specific battle/draw consumers, and the cited engine card sources have no intervening
semantic change. Changes in the mirrored bot/evaluator paths are trace observation plus deletion of
inert commented predecessors. The ownership findings below remain valid; refresh shifted line
numbers from current source before releasing an implementation packet.

## Current Runtime Shape

- `ObjectiveAnalyzer` produces objective identity, flip state, parsed locations/cards, compiled
  flags, JSON hydration, strategy tokens, and deploy `ScoreNote` contributions.
- DeployPhasePlanner and DeployPhaseScript consume objective locations, plan scores, and dynamic
  strategy tokens.
- DeployEvaluator consumes ObjectiveAnalyzer scores plus evaluator-local objective rules.
- CardSelectionEvaluator owns setup choices, child deploy destinations, objective-card protection,
  movement picks, and pull-target ranking.
- Move, Battle, Draw, and ActionText consume objective state for phase-local overlays.
- CharacterDeploySiteEvaluator adds a shared objective-location `+200` contribution and applies
  formation exemptions.
- DeckOracle owns pull catalogue, zone, viability, and failed-search facts. It correctly owns no
  score contribution.
- ObjectiveHandler is transported through context but has no behavioral caller. ActionAudit is a
  stub.

Objective deploy behavior therefore has at least four additive layers: planner ordering/plan
scores, ObjectiveAnalyzer ScoreNotes, evaluator-local rules, and shared objective-location `+200`.
They cannot be collapsed into one anonymous objective bonus without changing ordering.

## Blocking Findings

### 1. ObjectiveAnalyzer mixes facts and contributions

The approved facts/assessment boundary requires one immutable objective fact snapshot with no
score. ObjectiveAnalyzer currently emits mutable facts and deploy contributions from one object.
Phase-local consumers also recalculate objective predicates, so changing one owner can leave a
shadow owner active and create cross-talk.

### 2. Back-side truth is read from the wrong source

`ObjectiveAnalyzer.java:171-220` analyzes the current blueprint once and thereafter refreshes only
the flipped boolean when the blueprint ID matches. `ObjectiveAnalyzer.java:1651-1668` searches the
front-side game text for a literal `[Back Side]` marker instead of reading
`PhysicalCard.getOtherSideBlueprint()` (`PhysicalCard.java:83`, `PhysicalCardImpl.java:502`).

The objective fact snapshot must read both actual blueprints and retain front/back identity and
text separately. It must not infer the opposite side from formatting inside one game-text string.

### 3. Same-opponent rematches retain old mutable state

`RandoCalAi.java:1652-1682` resets ObjectiveAnalyzer, DeployPhasePlanner, DeckOracle, and other
strategy state only when the opponent name changes or `mySide` is null. A same-opponent rematch on
the same side can retain analyzer, planner, and oracle state from the previous game.

Reset must key on actual game identity, not opponent identity.

The stable identity already available at the AI boundary is the `SwccgGame` reference returned by
`gameState.getGame()`. A revert may replace the `GameState` object, while snapshot generation copies
the same `_game` reference into the replacement state. Therefore, compare the game reference by
identity; do not use `GameState` object identity and do not synthesize an id from opponent/time.

### 4. JSON is partially live and partially design-only

Current bundled data contains:

- 58 profiles;
- 15 `loaderEnabled` profiles;
- 14 enabled profiles with location fragments;
- 43 disabled design-time profiles.

The loader hydrates location fragments, required table cards, flip-gate fields, starting refs, and
some extension storage. Pullable-card hydration is deliberately commented out
(`ObjectiveAnalyzer.java:682-686`). Starting refs are stored but have no runtime consumer. Extension
arrays are empty in disabled profiles and remain design-time scaffolding.

DTO `JsonCardRef` binds blueprint IDs, title fragments, and source V-tag only. Data fields such as
runtime filter, source evidence, and enables-flip are not runtime truth. Any cutover must preserve
the explicit `loaderEnabled` boundary and prove compiled fallback behavior when the resource is
missing or malformed.

### 5. Card-source facts disagree with several AI predicates

- Hidden Path flips when two Jedi occupy non-Mapuzo sites. It does not require Jedi Survivors or
  battlegrounds (`Card226_028.java:145-160`).
- Underground Corridor moves a Jedi Survivor during MOVE and passes `forFree=false`; the shared
  action constructor therefore uses the default cost of 1 (`Card226_023.java:43-52`,
  `MoveUsingLocationTextAction.java:62-65`). MoveEvaluator's V53b comment says the move is free at
  `MoveEvaluator.java:2062-2066`.
- Hunt V uses typed `Filters.inquisitor` and specifically stacked Hatred. BattleEvaluator uses
  title helpers and treats any stacked card as Hatred.
- My Lord uses typed `Filters.senator`; ObjectiveAnalyzer's lore fallback is not equivalent.
- Invasion's actual flip law is Throne Room control with a Neimoidian plus Naboo system control.
  Pilot-aboard-capital-ship rules are deck playbook behavior, not objective truth.

Engine card Java and typed Filters remain the source of truth. The migration must not promote these
shadow predicates into canonical facts.

## Required Ownership Split

### `ObjectiveFactsProducer`

One immutable per-decision snapshot from current and opposite-side blueprints, enabled JSON, and
typed engine filters. It emits identity, front/back state, setup refs, flip requirements, role refs,
and typed filter facts. It emits no score and exposes no mutable ObjectiveAnalyzer reference.

### `ObjectiveDeployAdapter`

Own ObjectiveAnalyzer deploy notes, the shared objective-location contribution, and the V193
parent/child route pair. Preserve V193 parent `+400` versus child approximately `+2000`. Keep V99
under generic deploy-siting and V86/V121 under deck-playbook.

### `ObjectiveMoveAdapter`

Own pre/post-flip posture plus exact Hidden Path and Verge predicates. V67z remains a
ForceBudgetAssessment arm, not objective movement logic.

### `ObjectiveBattleAdapter`

Own only objective-specific battle overlays using typed Inquisitor/Hatred facts. Preserve the
intentional sum between generic BattleEvaluator behavior and ActionText V25.

### `ObjectivePullAdapter`

Combine objective target role with DeckOracle viability. Emit one V192 parent contribution and a
separate child target rank. Failed-search state must dominate stale objective desire.

### `ObjectiveSetupAdapter`

Consume typed starting refs and normalize temporary IDs, blueprint IDs, and real card IDs. Generic
CardSelection setup heuristics remain separate.

## Required Fixtures Before Retirement

1. `B0_ObjectiveFacts_FrontAndOtherSideBlueprint`
2. `B0_ObjectiveFacts_SameOpponentRematchReset`
3. `B0_ObjectiveCompiledFallback_ClasspathMissing`
4. `B0_ObjectiveDeploy_OneEmitPerActionId`
5. `B0_FlipGate_V193_ParentDeploy` plus existing `B0_FlipGate_V193_Bunker`
6. `B0_ObjectiveMove_HiddenPath_TwoNonMapuzoJedi`
7. `B0_ObjectiveMove_HiddenPath_JediSurvivor_Cost1`
8. `B0_ObjectiveBattle_Hunt_TypedInquisitorHatred`
9. `B0_ObjectivePull_V192_SingleEmit`
10. `B0_ObjectivePull_DeadSearchDominates`
11. `B0_ObjectiveSetup_RouteShapes`
12. `B0_ObjectiveLegacyArms_ZeroContribution`
13. Rando/ChosenOne parity for every route fixture

### Smallest First Fixture Slice

Seed only the first two facts fixtures before production work:

- front/other-side blueprint truth, using an existing double-sided objective scenario and asserting
  both blueprint identities/texts before and after flip
- same-opponent rematch reset, reusing one bot across two distinct `SwccgGame` references with the
  same player names/side and proving reset through the existing typed `StrategyResetEvent`, plus a
  snapshot/revert control that retains the same game reference and must not reset. Do not add a
  private-state getter or reflection-only production seam.

Do not assert the current wrong front-text parser or opponent-name reset behavior as a baseline.
The remaining ten route/contribution fixtures wait for the immutable facts API so they do not bind
to mutable `ObjectiveAnalyzer` internals.

## Cleanup Boundary

The old commented V83/V110/V108/V86/V88/V99 deploy block and the unreachable Endor `if(false)`
arm are inert and must never be re-enabled. Physical deletion may occur only after the
zero-contribution fixture proves no active owner depends on them. Live parser, title detectors,
compiled My Lord/Endor playbooks, setup logic, and phase-local playbooks cannot retire before their
adapter fixtures pass.

This audit authorizes no Java change, score change, owner retirement, or deployment.
