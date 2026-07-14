# Domain Registry Ambiguity Resolution

Date: 2026-07-13
Source snapshot: `66cf11e18`
Status: corrective input for `resources/DOMAIN_REGISTRY_2026-07-12.md`

## Ruling

The 13 ambiguous registry rows are not 13 single rules. They expand to 21 exact arms. A reused
base V-tag cannot receive one owner, kind, or migration status. Every row below needs a separate
registry entry with mirrored Rando/ChosenOne anchors.

`Kind` is the target contract. Parenthetical scores describe the current implementation costume,
not the future ownership model.

| Exact arm | Current Rando anchor | Fact or assessment producer | Contribution owner | Kind | Correction |
|---|---|---|---|---|---|
| V37.4 pass | `PassEvaluator.evaluate:163` | decision hand/force/phase facts -> pass policy | pass-cancel / SVC-SAFETY | SCORE | Passing is the decision; DEPLOY-1 does not own it. |
| V37.4 empty check | `DeployEvaluator.evaluate:3270` | none; local guessed eligibility | none | FACT, unconsumed | `canDeployToOpponents` is never read and V40 hardcodes `emptyPenalty=0`; retire after a no-consumer fixture. |
| V156 deploy hold | `CharacterDeploySiteEvaluator.computeTeamViability:440` | action facts + objective plan -> FormationAssessment | solo-formation / DEPLOY-2 | SCORE `-600/+250` | Formation policy, not generic siting. |
| V169 urgency | `DeployEvaluator.evaluate:945` | board facts -> endangered FormationAssessment | solo-formation / DEPLOY-1 | SCORE `+500` | Opens deploy because a formation needs rescue. |
| V169 destination | `CardSelectionEvaluator.evaluateDeployLocation:934` | FormationAssessment + rescue feasibility | solo-formation / DEPLOY-2 | SCORE `+800..1100` | Chooses the endangered formation. |
| V172 protect gate | `CardSelectionEvaluator.evaluateDeployLocation:947` | BattleFeasibility + ForceBudgetAssessment | solo-formation / DEPLOY-2 | CONSTRAINT | A rescue wave must be able to close the gap. |
| V172 contact gate | `CardSelectionEvaluator.evaluateDeployLocation:1077` | BattleFeasibility + ForceBudgetAssessment | solo-formation / DEPLOY-2 | CONSTRAINT | Controls whether direct contact is supportable. |
| V172 solo dominance | `CardSelectionEvaluator.evaluateDeployLocation:1127` | BattleFeasibility -> FormationAssessment | solo-formation / DEPLOY-2 | SCORE `+600` | Registry claim of no own points is stale. |
| V174 wave budget | `CardSelectionEvaluator.v173WaveProjection:5521` | maintenance + action facts -> ForceBudgetAssessment | force-budget / DEPLOY-2 | CONSTRAINT | Reserved force is policy, not an observational fact. |
| V29.5 buddy | `CardSelectionEvaluator.evaluateDeployLocation:3089` | presence/ownership facts -> FormationAssessment | solo-formation / DEPLOY-2 | SCORE | Scores group topology; unrelated to the V29.5 shield arm. |
| V27 battle reserve | `BattleEvaluator.evaluate:801` | decision facts -> ForceBudgetAssessment | force-budget / BATTLE-1 | SCORE `-15/-40` | Soft interrupt-readiness cost. |
| V27.1 battle DTF | `BattleEvaluator.evaluate:813` | ForceReserveService DTF fact -> ForceBudgetAssessment | force-budget / BATTLE-1 | SCORE `-60/-100` | Collapse the duplicate inline DTF scan into the shared fact service. |
| V27.1 pass DTF | `PassEvaluator.evaluate:184` | ForceReserveService DTF fact -> ForceBudgetAssessment | force-budget / pass | SCORE `+20/+40/+60` | Same fact, separate phase contribution. |
| V24.2 pull | `CardSelectionEvaluator.evaluateTakeIntoHand:7630` | ObjectivePlan + PullViability | pull-search / Pull Engine | SCORE `+250/+200` | Objective supplies plan facts; Pull Engine owns the candidate result. |
| V24.2 drain | `ActionTextEvaluator.evaluate:3062` | optional `+1 drain` ActionFact | drain-control / CONTROL | RANK, live `+80` | Free-drain acceptance is unrelated to pulls or battle. |
| V193 Deploy route | `DeployEvaluator.evaluate:1905` | ObjectiveAnalyzer + DeckOracle -> ObjectivePlan | deploy-siting / objective adapter | RANK, live `+400` | Separate parent route; currently lacks the child body gate. |
| V193 child route | `CardSelectionEvaluator.evaluateDeployLocation:2211` | same ObjectivePlan | deploy-siting / objective adapter | RANK, live `weight+1600` | Separate destination route with ability/cost gate. |
| Formation enforcement | `EvaluatedAction.mergeFrom:70`; `CombinedEvaluator.evaluateDecision:274` | domain constraints | loop-safety / SVC-SAFETY finalizer | ROUTING consuming CONSTRAINT | Formation owns laws; generic finalizer owns veto OR-merge and enforcement. |
| V141 transport floor | `ActionTextEvaluator.evaluate:608` | action + force/reserve facts -> ForceBudgetAssessment | force-budget / MOVE-RESPONSE | CONSTRAINT, live `-2000` | Unaffordable transport is ineligible, not merely undesirable. |
| V67z DRAW reserve | `DrawEvaluator.calculateForceToReserve:633` | ObjectivePlan + corridor occupants -> ForceBudgetAssessment | force-budget / DRAW | CONSTRAINT | Hidden Path scopes the trigger; budget service owns reserve mechanics. Current code counts all characters despite Jedi wording. |
| V67z DEPLOY reserve | `DeployEvaluator.evaluate:301,2359` | same ForceBudgetAssessment | force-budget / DEPLOY-1 | CONSTRAINT, live `-1500` | Keep separate from DRAW because the deploy copy caps reserve at `3`. |

All anchors above have corresponding ChosenOne mirrors at the same current line or method location.
Re-anchor after any cleanup commit.

## Cross-phase facts versus contributions

Shared services may produce the same fact for several phases without owning every contribution.
Examples:

- ForceReserveService owns the DTF/maintenance observation. Battle and Pass own separate phase
  contributions derived from it.
- ObjectiveAnalyzer owns objective identity and plan facts. Pull Engine and deploy-siting adapters
  own their route results.
- FormationAssessment owns formation laws. CombinedEvaluator owns generic constraint merge and
  final enforcement.

Do not classify these services as SCORE merely because current callers immediately add a number.

## Multi-arm warning

The V27 buddy-protect arm in `MoveEvaluator` is a solo-formation/MOVE rule. Its Pass/Move
maintenance siblings are force-budget rules. Likewise, V24.2 pull and drain, V193 parent and child,
V37.4 pass and empty check, and V67z DRAW and DEPLOY require separate entries.

## Registry completion gate

For each new arm row, K-2 must add:

- Exact Rando and ChosenOne method anchors.
- Current score band and target kind.
- Fact producer, assessment producer, and contribution owner as separate columns.
- Current live/dead/unconsumed status.
- Replacement owner and cutover batch.
- Named parity and retirement fixtures.

The registry becomes migration authority only when these 21 arms replace the 13 ambiguous rows and
the previously misclassified dead/no-score rows are corrected.
