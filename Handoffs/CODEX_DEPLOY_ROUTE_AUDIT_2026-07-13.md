# Codex DEPLOY route audit

Date: 2026-07-13
Owner: Codex audits and gates; K-2 implements production Java
Baseline: clean commit `21dda1a67367da689e6f610f7111b6dbdfee0c2e`
Scope: all seven wire decision shapes during the DEPLOY phase, parent/child deployment
transactions, formation safety, planner lifecycle, Force obligations, objective adaptation,
Rando/chosenone parity, fixtures, and replay evidence. Concurrent dirty Trace 2b work was
excluded.

## Verdict

| Boundary | Verdict | Reason |
|---|---|---|
| Inert B2 fact extensions and corrected registry arms | ADVANCE | Typed transport can expose current ownership without changing response selection. |
| Shadow `DeployWindowRoute` capture | ADVANCE | Parent/child identity is currently absent and must be observed before policy moves. |
| Pure deploy assessments and deterministic fixtures | ADVANCE | These can freeze existing behavior while isolating contradictions. |
| Runtime DEPLOY cutover | HOLD | There is no atomic parent/child owner, Force obligations have split owners, and several hard rules remain additive scores. |
| Legacy owner retirement | HOLD | Planner, evaluator, objective, and fallback behavior are not fixture-equivalent yet. |
| Build or deployment authorization | HOLD | This audit is read-only and authorizes no deployment. |

## P0 findings

### 1. Parent and child deployment have no atomic owner

The parent `CARD_ACTION_CHOICE` exposes an action ordinal and physical card id, but the concrete
action remains private. `PlayCharacterAction` then opens a destination `CARD_SELECTION`, optional
undercover `MULTIPLE_CHOICE`, and optional capacity-slot `MULTIPLE_CHOICE` through a private target
filter closure. No typed parent identity survives into those child decisions.

Evidence:

| Source | Evidence |
|---|---|
| `CardActionSelectionDecision.java:26-40` | Parent wire parameters expose action/card data only. |
| `PlayCharacterAction.java:58` onward | Concrete deployment action owns the private target sequence. |
| `ChooseCardsOnTableEffect.java:216` onward | Destination selection becomes a separate awaiting decision. |
| `Handoffs/CODEX_PARENT_CHILD_DEPLOY_PLAN_AUDIT_2026-07-13.md` | July 12 parent allowed buddy plans while child FormationSafety canceled every first body. |

Result: parent scoring cannot prove the child destination, undercover, capacity, or buddy
sequence that will actually be accepted. The parent plus all deployment children must cut over
as one transaction.

### 2. The current hard-veto contract is not uniformly absolute

`EvaluatedAction` hard-veto flags OR-merge, but `CombinedEvaluator` deliberately selects the
least-bad vetoed action when every candidate is vetoed and pass is unavailable. That forced-choice
policy is valid only when it is explicit. It contradicts comments that describe every Formation
Safety veto as unselectable.

Other advertised hard rules remain score costumes:

| Rule | Current mechanism | Consequence |
|---|---|---|
| V163 blocked action | `-100000` | A larger additive stack can still revive it. |
| Deploy loop block | `-9999` | Not a typed constraint. |
| V67z | `-1500` | Not a typed constraint. |
| L3 no-plan deployment | `-800` | Not a typed constraint and can be outscored by rescue/objective bonuses. |

Clean-baseline anchors: `EvaluatedAction.java:96`, `CombinedEvaluator.java:462` onward,
`ActionTextEvaluator.java:107` onward, and `DeployEvaluator.java:529` onward.

Required contract: each assessment returns `ALLOW`, `DEFER`, or `BLOCK`. If every candidate is
blocked, a separate typed forced-choice policy decides whether legal pass or deterministic
least-bad response is required. Scores do not make that decision.

### 3. Force and strategic intent have split ownership

| Owner | Current obligation model | Conflict |
|---|---|---|
| `DeployPhasePlanner` | Reserves battle plus maintenance. | Omits several parent/child obligations. |
| Parent V38 | Separately reserves maintenance, Draw Their Fire, and spies. | Misprices a maintenance buddy as its deploy cost. |
| Child V173 | Computes another reserve, then caps it to preserve a three-Force deployment wave. | Child can disagree with parent affordability. |
| Parent V169 | Adds `+500` to every deploy source when any location is endangered. | Bonus is not tied to a concrete rescuer or legal destination. |

No producer represents a concrete retreat or movement obligation. Source anchors:
`DeployPhasePlanner.java:210`, `ForceReserveService.java:69`, `DeployEvaluator.java:950`, and
`CardSelectionEvaluator.java:5463`.

Required contract: one immutable `ForceObligationVector` owned by the planner and consumed
unchanged by parent, child, pass, and movement-preservation assessments.

## P1 findings

| Finding | Evidence and impact |
|---|---|
| Planner evaluation mutates state | `DeployEvaluator.evaluate()` performs cleanup and stale-flag changes. Evaluation cannot be replayed as a pure function. |
| Plan cache is too coarse | `DeploymentPlan` is keyed by turn; instructions are blueprint-only; `recordDeployment()` removes every duplicate printing. Physical-card identity is lost. |
| Dead planner output | `DeployPhasePlanner.getCardScore()` has zero callers. It is theory, not runtime ownership. |
| Buddy legality is optimistic | FormationSafety accepts the cheapest character in hand without proving a shared legal destination. |
| Mover legality is optimistic | V156 treats any table character as a possible mover; V38 treats matching planet-title prefixes as future movement plans. |
| Existing body fact is wrong | FormationSafety's `landsSolo` uses total ability, so a present ability-zero character is invisible. |
| Objective deploy policy is mixed into facts | `ObjectiveAnalyzer` emits `ScoreNote` contributions, parses a backside marker from front blueprint text, and only resets on opponent identity change. |
| V193 parent/child gates differ | Parent uses roughly `+400`; child uses roughly `+2000`, and only child applies the ability/cost gate. |
| Response finalization is unsafe | MULTIPLE_CHOICE can throw unchecked on an out-of-range ordinal; invalid AI responses are silently requeued; recursion stops silently at 50; INTEGER emergency always returns `0`. |
| Registry is inventory, not retirement authority | The DEPLOY registry has 23 live arms plus one inert arm, but 344 of 367 global live arms still lack stable markers and many fixtures remain unresolved. |

Source anchors: `DeployEvaluator.java:343`, `DeploymentPlan.java:81`,
`DeployPhasePlanner.java:1732`, `FormationSafety.java:205`,
`CharacterDeploySiteEvaluator.java:474`, `DeployEvaluator.java:2695`,
`ObjectiveAnalyzer.java:171`, `:408`, `:1651`, and `RandoCalAi.java:1916`.

## Frozen route precedence

All DEPLOY shapes first encounter V45. Applicable `MULTIPLE_CHOICE` decisions then encounter
V44/V67j, V170, V61, and Rando-only V79b in that order. DEPLOY disables chaos. Direct
interceptors return before emergency handling, `DecisionSafety`, tracker mutation, and strategic
event mutation.

After interceptors, the current lanes are:

| Wire shape | Current DEPLOY lane |
|---|---|
| `EMPTY` | No normal evaluator under defaults; heuristic returns `pass`. |
| `INTEGER` | `ForceActivationEvaluator` catches every integer decision. |
| `MULTIPLE_CHOICE` | Narrow ActionText capacity/option handlers; otherwise heuristic fallback. |
| `ARBITRARY_CARDS` | CardSelection catch-all, multi-select formatter, then selectable clamp. |
| `CARD_ACTION_CHOICE` | Deploy, ActionText, and legal Pass contributions merge by action id; DPS hierarchy then selects. |
| `ACTION_CHOICE` | ActionText plus Pass only when raw `noPass=false`; otherwise heuristic if no actions emerge. |
| `CARD_SELECTION` | CardSelection text router handles destination, source, pilot, and target; unknown text creates neutral actions and suppresses heuristic fallback. |

`CombinedEvaluator` registration is ForceActivation, Deploy, Battle, Move, Draw, CardSelection,
ActionText, Pass. Every applicable evaluator runs. Registration order controls first-seen
insertion and exact ties, not exclusive ownership. Selection then applies DPS buckets at `-100`,
non-bucket epilogue at `+50`, legacy filtering, veto handling, deploy-location cancel bar `0`,
and final winner.

Clean-baseline anchors: `RandoCalAi.java:614`, `CombinedEvaluator.java:63`, and
`CombinedEvaluator.java:186` onward.

## Imported contracts

These existing documents remain normative and must not be redefined:

| Contract | Frozen ownership |
|---|---|
| `Handoffs/CODEX_PARENT_CHILD_DEPLOY_PLAN_AUDIT_2026-07-13.md` | Parent/child topology, cancellation evidence, and atomic cutover requirement. |
| `Handoffs/CODEX_DEPLOY_WEIGHT_CONSOLIDATION_CONTRACT_2026-07-13.md` | Formation outcomes, sequence proof, obligations, intent order, and score-boundary fixtures. |
| `Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md` | Shared response contract, pass legality, bounded mediator retry, and single tracker mutation. |

## Smallest transport seam

```java
record DeployWindowRoute(
    WindowKey window,
    RouteKind kind,
    int ordinal,
    String responseId,
    PhysicalCardRef source,
    Optional<PhysicalCardRef> destination,
    Optional<ParentRouteRef> parent
) {}

record DeployRouteAssessment(
    DeployWindowRoute route,
    Constraint constraint,
    RouteResult result,
    FormationOutcome formation,
    Optional<SequenceRef> sequence,
    ForceObligationVector obligations,
    IntentRank intent,
    float boundedFine,
    Provenance provenance
) {}
```

`WindowKey` contains game id, turn, phase, and parent decision id. `RouteKind` contains only
parent action, destination, source selection, capacity, confirmation, and pass. Candidate arrays
remain in the B2 snapshot and must not be duplicated here.

Do not invent a fine-score clamp yet. First freeze legacy ordered traces, then derive one bounded
fine range per intent rank from observed contribution boundaries.

## Cutover order

1. Shadow-capture all seven wire shapes plus typed deploy parent references. No response change.
2. Make `DeployPhasePlanner` a pure producer of physical-card assessments and one obligation vector.
3. Commit or invalidate sequence state only after an engine-validated response, child cancel, zone change, phase change, or game change.
4. Cut L3 parent and child ownership atomically.
5. Move V169 rescue and retreat preservation together.
6. Move V171/V172 positive controls, then V193 through `ObjectiveDeployAdapter`.
7. Retire score costumes one exact registry arm at a time after mirrored trace parity.

## Required fixtures

Retain every named fixture in the deploy consolidation contract, especially all `DEPLOY_L3_*`,
`DEPLOY_Greedo_*`, `DEPLOY_WeakSolo_*`, `DEPLOY_Court4LOM_*`,
`DEPLOY_TargetedRescue_*`, `DEPLOY_Tyranus_*`, `DEPLOY_V193_*`, and
`BATTLE_OverpowerDeploy_ZeroDestiny_DoesNotBypassBattleSafety` cases.

Add these route and lifecycle fixtures:

| Fixture | Required assertion |
|---|---|
| `DEPLOY_ROUTE_EMPTY_HeuristicNoOp` | EMPTY behavior is captured without inventing an evaluator owner. |
| `DEPLOY_ROUTE_INTEGER_BoundsPreserved` | Concrete integer bounds survive route capture and finalization. |
| `DEPLOY_ROUTE_MULTIPLE_CHOICE_OrdinalRange` | Ordinal is in range and engine-valid. |
| `DEPLOY_ROUTE_ARBITRARY_CARDS_SelectableMulti` | Preselected/selectable state and iterative selection are preserved. |
| `DEPLOY_ROUTE_CARD_ACTION_ParentPhysicalBinding` | Parent binds exact physical source card. |
| `DEPLOY_ROUTE_ACTION_CHOICE_ExplicitAction` | Explicit action identity survives routing. |
| `DEPLOY_ROUTE_CARD_SELECTION_ChildDestinationBinding` | Child binds destination and exact parent. |
| `DEPLOY_CONSTRAINT_AllVetoMandatory_ExplicitPolicy` | Forced all-veto outcome uses typed policy, not score magnitude. |
| `DEPLOY_PLAN_Evaluate_IsPure` | Repeated assessment has no planner mutation. |
| `DEPLOY_PLAN_SameOpponentRematch_Invalidates` | New game invalidates cached objective/deploy state even with same opponent. |
| `DEPLOY_FORMATION_AbilityZeroBodyCountsPresent` | Existing ability-zero friendly character counts as a body. |
| `DEPLOY_MEDIATOR_InvalidResponse_ReinvokedOrTerminal` | Invalid response gets one bounded retry or visible terminal state. |

Each fixture freezes raw candidate order, exact float bits, pass eligibility, selected route,
winner, cursor delta, and invalidation. No unseeded RNG.

## Replay and log grounding

| Evidence | Proven behavior |
|---|---|
| `logs/2026-07/app-07-12-2026-1.log.gz`, decompressed lines `62869-64335` | Nine parent/child cancellation cycles expose the cross-prompt topology. Current HEAD may now allow a first body when pair budget fits, but still does not bind or complete that buddy sequence. |
| `replays/asdf/rbujmoc90br3uu4c.xml.gz` | Negative child destination and deploy-cancel behavior. |
| `replays/asdf/f27ws5lgy0g58k5p.xml.gz` | V148 child cancel repeats parent three times before loop blocking. |
| `replays/asdf/somykkwjy449xul4.xml.gz` | Deploy route evidence retained by the fixture corpus. |
| `replays/asdf/vugpape5lw1bc7rq.xml.gz` | Deploy route evidence retained by the fixture corpus. |
| `replays/asdf/qgdridfo166f27r3.xml.gz` | Deploy route evidence retained by the fixture corpus. |

Committed normalized parity exists for Combined, Deploy, CardSelection, Pass, ActionText,
ObjectiveAnalyzer, DeployPhasePlanner, DeploymentPlan, DeploymentInstruction, and DecisionSafety.
Text parity proves mirrored code only. It does not prove one semantic owner or runtime correctness.

No deployment is authorized by this audit.
