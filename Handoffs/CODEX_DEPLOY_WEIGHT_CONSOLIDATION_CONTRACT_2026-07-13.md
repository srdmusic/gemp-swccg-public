# Deploy Weight Consolidation Contract

Date: 2026-07-13
Owner: Codex/Alfred architecture and gate
Implementer: K-2/Claude
Status: corrected preflight `HOLD`; final follow-on after authoritative DEPLOY owner

Current-baseline refresh: Trace/finalizer foundation is complete and independently gated through
`35dea5c5a56fb9fbb1dabdae74107341f1c676ba`. The contract remains `HOLD` behind the active
behavioral lanes and its parent/child transaction fixtures. Cleanup commits since this contract was
written removed inert commented predecessors but did not consolidate the live deploy owners below;
refresh shifted source line numbers before implementation dispatch.

## Decision

Do not make solo deploys safer by adding a larger negative score. The current failure is split
ownership: the parent deploy action, child destination prompt, formation guard, objective steer,
rescue rule, and later movement logic each answer a different version of the same question.

`DeployPhasePlanner` and its existing `DeploymentPlan` become the single cross-prompt owner of a
typed `DeployRouteAssessment`. `FormationSafety` remains a pure shared rule engine. Evaluators
consume the assessment and cannot independently infer a buddy, rescue, or escape plan.

This extends the parent/child lifecycle contract in
`Handoffs/CODEX_PARENT_CHILD_DEPLOY_PLAN_AUDIT_2026-07-13.md`. It does not create another mutable
mediator.

## Current contradictions

| Concern | Current owner(s) | Failure |
|---|---|---|
| Endangered ally | `DeployEvaluator` V169 at 945-984; `CardSelectionEvaluator` V169/V172 at 934-986 | Parent gives every deploy source `+500` when any site is endangered. An unrelated child destination can spend retreat Force. |
| Buddy sequence | `DeployEvaluator` V38/V32; `FormationSafety`; child V136 route | A card in hand is treated as a plan without proving same-site legality, exact order, total cost, or completion. Parent and child can contradict and reopen nine times. |
| Weak solo | Child L3 `-800`; V29.5/V113 penalties; V136 and objective bonuses | Additive penalties can be outvoted by `+800`, `+2000`, or other destination stacks. A stronger penalty only moves the arithmetic accident. |
| Contact/overpower | Child V171/V172 at 1022-1156 | `+600` is mixed into unrelated scores. The intent is valid, but it is not a typed exemption or route rank. |
| Escape or retreat | CardSelection move V169/V156; MoveEvaluator ladder | Deploy selection cannot prove a later legal move, yet may spend the Force that the later move needs. Generic "can move later" is not a plan. |
| Plan score | `DeployPhasePlanner.scorePlan()` at 1413-1523 | The planner aggregates power, ability, and objective bonuses, but does not classify the completed formation or reserve a concrete buddy/move sequence. |

The Court 4-LOM incident is the decisive counterexample to a solo-only fix. The action was not
solo. Global V169 blessed an unrelated deploy, the child rerouted it away from the endangered
site, and the action spent the Force needed to retreat.

## Typed assessment

Produce one assessment per exact physical parent deploy candidate. Preserve raw candidate order.

### Identity

- Parent action ordinal and id.
- Exact physical card id plus blueprint id.
- Snapshot version and producer provenance.
- Ordered child destination assessments.

Blueprint id alone is not sequence identity because duplicate physical copies can coexist.

### Route result

Use the already frozen result states:

- `SAFE_SOLO`: at least one destination is safe without a follow-up.
- `SAFE_SEQUENCE`: an exact buddy or movement sequence completes a safe formation.
- `ALL_DESTINATIONS_BLOCKED`: no safe solo or complete sequence exists.
- `UNKNOWN`: required legality or state facts could not be resolved.

### Formation outcome

Each destination has exactly one typed outcome:

- `SAFE_SOLO`
- `SAFE_BUDDY_SEQUENCE`
- `SAFE_MOVE_SEQUENCE`
- `UNSAFE_NO_PLAN`
- `UNSAFE_PAIR_BUDGET`
- `UNSAFE_CONTESTED`
- `UNKNOWN`

The outcome-to-constraint mapping is closed:

| Formation outcome | Constraint |
|---|---|
| `SAFE_SOLO`, `SAFE_BUDDY_SEQUENCE`, `SAFE_MOVE_SEQUENCE` | `ADMISSIBLE` |
| `UNSAFE_NO_PLAN` | `DEFER` |
| `UNSAFE_PAIR_BUDGET`, `UNSAFE_CONTESTED` | `HARD_BLOCK` |
| `UNKNOWN` | The exact registry-declared route policy; never infer `ADMISSIBLE` |

Legal Pass and every `ADMISSIBLE` candidate must beat `UNSAFE_NO_PLAN` regardless of retained
positive operations. A mandatory route may use an explicit forced-choice policy, but no additive
score may revive `DEFER`, `HARD_BLOCK`, or `ILLEGAL`.

`UNSAFE_NO_PLAN` means all of the following are known:

- The exact physical character lands as the only non-undercover friendly character; total ability
  is not a body count.
- Its post-deploy formation cannot draw normal battle destiny.
- No explicit exemption applies.
- No complete same-phase, same-destination buddy sequence is legal and affordable after obligations.
- No exact legal movement sequence with reserved Force is proven. `UNKNOWN` movement is not a plan.

### Buddy sequence

A buddy plan is real only when it records:

- Exact first and second physical card ids.
- One shared legal destination.
- Ordered deploy costs.
- Force available before the first deploy.
- Maintenance, battle, movement, and objective obligations reserved first.
- Proof that both deploys remain legal in that order in the same deploy phase.

"Another character is in hand" is evidence, not a sequence.

### Movement sequence

A movement plan is real only when it records:

- Exact deployed mover and exact destination.
- Engine-backed legal movement mode and cost.
- Force reserved through the move phase.
- Post-move origin and destination formations.
- The actual mover, not every card at the origin.
- Invalidation conditions for battle, board, Force, or phase changes.

If the parent route cannot preflight these facts without mutation, the movement plan is `UNKNOWN`.
Mere landspeed or adjacency is not an exemption from `UNSAFE_NO_PLAN`.

### Strategic intent

One candidate may expose several observations, but one adapter selects one primary intent:

- `OBJECTIVE_REQUIRED`: narrow engine or objective requirement with an explicit exemption.
- `TARGETED_RESCUE`: this candidate has a legal destination at the endangered site and produces a
  survivable post-sequence formation.
- `OVERPOWER_OPPORTUNITY`: this exact character plus existing allies reaches at least 2x the
  opponent's typed weapon-adjusted power at an underpowered solo or low-power site.
- `DIRECT_CONTACT`: an affordable, legal wave can form at the occupied site this phase.
- `DRAIN_DENIAL`, `SAFE_SEQUENCE`, `SAFE_SOLO`, `SAFE_ESTABLISH`, or `NONE`.

The complete intent order is:
`OBJECTIVE_REQUIRED > TARGETED_RESCUE > OVERPOWER_OPPORTUNITY > DIRECT_CONTACT > DRAIN_DENIAL >
SAFE_SEQUENCE > SAFE_SOLO > SAFE_ESTABLISH > NONE`.

Survival-retreat preservation is a Force-obligation constraint, not an intent. A deploy that would
consume its reserved Force becomes `DEFER` or `HARD_BLOCK` according to the closed comparator below.

`OVERPOWER_OPPORTUNITY` is an exemption from the generic weak-solo deploy penalty. It is not a
blanket exemption from battle-destiny safety. The later battle assessment still owns whether a
voluntary battle is legal and worthwhile.

## Non-additive ordering

The deploy adapter emits three separate channels. They are not summed into one unrestricted pile.

1. `Constraint`: `ADMISSIBLE`, `DEFER`, `HARD_BLOCK`, or `ILLEGAL`.
2. `IntentRank`: objective-required, targeted rescue, overpower/contact, safe sequence, safe solo.
3. `BoundedFine`: drain, icons, card value, and other tie-breaking preferences.

Selection order is lexicographic:

1. `ILLEGAL` never participates.
2. `HARD_BLOCK` participates only under an explicit mandatory forced-choice policy.
3. Legal Pass and every `ADMISSIBLE` candidate beat `DEFER`; a deferred candidate participates only
   when no admissible candidate or legal Pass exists.
4. Preserve required Force obligations, including a known survival retreat.
5. Apply only named, typed safety exemptions.
6. Compare primary intent rank.
7. Compare bounded fine within the same rank.
8. Preserve original candidate order on exact ties.

No objective preference, urgency bonus, or movement ladder score may revive a blocked candidate.
An objective may override formation safety only through a named exemption such as the V193
flip-gate one-body steer, never through a large scalar.

The V193 exemption is narrow. It requires all of these typed facts: exact flip-gate site, objective
still requires the gate, no current control, gate card reachable, exact physical candidate has
presence-producing ability, current policy deploy cost at most 4, legal deploy, and every Force
obligation preserved. The card text requires control of Bunker. A named site alone is never an
exemption.

If a temporary compatibility float encodes intent ranks while `BoundedFine` is limited to
`[-B,+B]`, adjacent rank stride must be greater than `2B`. Derive `B` from frozen traces; do not
guess it.

Freeze these current domination counterexamples before cutover:

- `50 base - 800 L3 + 800 V67bn = 50`, which can beat the deploy-location cancel bar.
- `50 - 800 + 500 V136 + 600 V171/V172 = 350`, which revives an unsafe solo.
- V193 contributes roughly `+2000`, so an ordinary negative cannot safely dominate it.

## Ownership moves

| Legacy logic | Target owner | Cutover rule |
|---|---|---|
| Parent V169 global `+500` | Deploy route assessment | Remove once targeted-rescue and retreat-obligation fixtures pass. It must never bless unrelated source actions. |
| Child V169 `+800..1100` | `TARGETED_RESCUE` intent | Requires exact viable rescue destination and post-sequence survivability. |
| V171 direct-contact `+600` | `DIRECT_CONTACT` intent | Requires character category, affordable legal wave, obligations, typed weapon power, and hit model. |
| V172 solo-dominance `+600` | `OVERPOWER_OPPORTUNITY` exemption/intent | Preserve Steve's positive ruling, character-only, exact 2x typed effective-power proof. |
| V166 contact pressure `+250..400` | `DRAIN_DENIAL` intent | Fold into the one primary-intent selection and prove no duplicate emission with V171/V172. |
| L3 child `-800` | Formation outcome and constraint policy | Replace only after parent and child consume the same assessment. |
| V67bj wave-affordability `-400` | `UNSAFE_CONTESTED` constraint | Consume the shared exact sequence and obligation facts; do not recompute affordability. |
| V67bu title-prefix escape gates | Exact MOVE sequence evidence | Retire both heuristic gates after exact movement proof; unresolved movement is `UNKNOWN`. |
| V29.5, V113, V32, V38 solo/buddy fragments | Bounded fine or retired duplicate | Remove only arm-by-arm after trace parity proves the typed assessment owns the semantic contribution. |
| V136 deploy-site result | Bounded fine plus objective adapter inputs | It cannot own formation safety, rescue, or cross-prompt lifecycle. |

Also inventory and either migrate or explicitly retain every rescue and obligation owner before
scalar deletion: V67bn `+800`, V29 rescue `+100..250`, V176 `-800`, and V67z `-1500`. V67bn may
become a typed `DAMAGE_MITIGATION` intent only when no legal retreat exists and the exact deploy
reduces projected loss; it does not satisfy `TARGETED_RESCUE` without survivability.

The exact deploy transaction cursor is separate from strategy-learning memory. `DeploymentPlan`
must bind exact physical copies and remove only the completed cursor. `lastPendingDeployType` remains
text-derived advisory learning state; it cannot prove child completion, zone transition, or sequence
progress. Accepted parent, child cancel, child completion, and successful zone transition update the
transaction owner explicitly. Strategy credit occurs once only after successful completion.

This is one deploy-phase working tranche and one aggregate commit. Inside the uncommitted tranche,
wire shadow capture, move every closed owner, and disable each exact duplicate live call site without
running tests. Retain predecessor source and use its hash-pinned fixtures or frozen traces during the
one final gate. Do not split types, shadow wiring, parent, child, owner moves, callsite disablement,
tests, or changelogs into micro-commits. A failed final gate is repaired in the working tranche and the
same focused phase command is rerun; the aggregate commit is created only after the final state passes.

## Required fixtures

### Parent and child lifecycle

- `DEPLOY_L3_ValidBuddySequence_FirstBodyAllowed_ThenCompleted`
- `DEPLOY_L3_AllDestinationsBlocked_ParentDoesNotOpenChild`
- `DEPLOY_L3_ChildCancel_InvalidatesSequence_NoReopenLoop`
- `DEPLOY_L3_DuplicateBlueprints_ExactPhysicalCursor`
- `DEPLOY_ChildCancel_NoStrategyCredit_NoReopen`
- `DEPLOY_ChildSuccess_OneZoneTransition_OneStrategyCredit`

### Solo and movement plan

- `DEPLOY_Greedo_TwoAffordableBuddies_CannotEndSolo`
- `DEPLOY_WeakSolo_NoBuddy_NoLegalMove_UnsafeNoPlan`
- `DEPLOY_WeakSolo_NoBuddy_NoLegalMove_PassAndEveryAdmissibleCandidateWinsDespitePositiveLegacyScores`
- `DEPLOY_WeakSolo_NoBuddy_ConcreteMoveSequence_AllowedAndReserved`
- `DEPLOY_WeakSolo_MovementUnknown_NoExemption`
- `MOVE_Chiraneau_LeavesOzzelAndLandsSolo_Blocked`

### Rescue and obligations

- `DEPLOY_Court4LOM_UnrelatedDestination_DoesNotReceiveRescueIntent`
- `DEPLOY_Court4LOM_RetreatForcePreserved`
- `DEPLOY_TargetedRescue_ViableWave_ReceivesRescueIntent`
- `DEPLOY_TargetedRescue_UnwinnableWave_DefersToRetreat`

### Positive controls

- `DEPLOY_Tyranus_OverpowersLoneLeia_OpportunityPreserved`
- `DEPLOY_FirstLight_CharacterContactIntentDoesNotFire`
- `DEPLOY_V193_FlipGateCheapAbilityBody_ExplicitExemption`
- `DEPLOY_V193_AbilityZeroBody_NoExemption`
- `DEPLOY_V193_CostFiveBody_NoExemption`
- `DEPLOY_V193_GateCardMissing_NoExemption`
- `DEPLOY_V193_BunkerAlreadyControlled_NoExemption`
- `DEPLOY_V193_UnrelatedSite_NoExemption`
- `DEPLOY_UndercoverSpy_DrainBlock_ExplicitExemption`
- `BATTLE_OverpowerDeploy_ZeroDestiny_DoesNotBypassBattleSafety`

The scalar-retirement inventory must cover V169 parent/child, L3, V113, V156, V166, V171/V172,
V193, V170, V176, V67z, V67bn, V67bj, and both V67bu gates. Retire only the solo arms of V29.5 and V38. Split V136: formation,
speculative buddy, and mover arms retire; combat, drain, icon, and over-stack preferences remain
bounded fine until their own owners move. Preserve V32's live `+150` ability-fix preference or
delete its stale penalty diagnostic; there is no live V32 `-200` to migrate.

## Execution Order Inside One Tranche

1. Extend the inert B2 types with the typed formation, sequence, obligation, and intent values.
2. Build shadow assessments from the immutable snapshot. No score or response changes.
3. Bind exact physical parent and child candidates and trace sequence invalidation.
4. Add the named frozen replay fixtures and Rando/ChosenOne parity checks without running them.
5. Move the global V169 parent grant to targeted rescue plus retreat-obligation ownership.
6. Move L3 buddy/no-plan ownership across parent and child as one atomic behavioral cutover.
7. Move V166 and V171/V172 to one-primary-intent drain/contact/overpower ownership. Move V67bj to
   `UNSAFE_CONTESTED` and replace both V67bu gates with exact movement evidence or `UNKNOWN`.
8. Disable every exact duplicate live call site in the same working tranche while retaining its
   source for the retirement phase. Do not condition disablement on an intermediate test run.
9. Run one focused phase verification pass against the final disabled state using the retained
   predecessor fixtures or frozen traces. Include package, static-owner/deletion, frozen fixture corpus,
   and mirrored parity gate. Game, browser, live-server, jar-load, and fired-trace validation remain
   outside this edit phase and require the final deployment gate.

## Gate

The deploy cutover remains `HOLD` until:

- The inert B2 consistency prerequisite is cleared by `d558248cf`; the complete Trace 2b raw-input
  and executable consumer gate must still pass before these assessments become authoritative.
- Parent and child consume one exact `DeployRouteAssessment`.
- A legal buddy or move sequence is proven, not inferred.
- Retreat Force is an obligation visible before parent deploy selection.
- V172 overpower remains a positive control.
- Every migrated arm has one owner in the domain registry and one ordered trace contribution.
- Focused fixtures, mirrored-source checks, affected-module package, and aggregate build pass.

No deployment is authorized by this contract.
