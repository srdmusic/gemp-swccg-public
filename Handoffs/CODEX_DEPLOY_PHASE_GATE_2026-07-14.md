# DEPLOY Phase Owner Cutover Gate

Date: 2026-07-14 PDT

Status: `READY, REPLACEMENT COMMIT PENDING`

This gate supersedes the premature `119dcc035` result. The replacement repairs
rotated current-card identity, deployed-location relations, blocked-attempt replay,
deferred PULL formation binding, child assessment ownership, and the complete V170
compatibility matrix before integration.

## Identity

| Item | Value |
|---|---|
| Worktree | `/Users/steve/gemp-swccg-deploy-phase-codex` |
| Branch | `codex/deploy-owner-cutover` |
| Parent | `3a4214866` |
| Supersedes | `8fcb9fa9a` (`HOLD`; never integrated or deployed) |
| Frozen packet | `/Users/steve/gemp-swccg-public/Handoffs/CODEX_DEPLOY_PHASE_PACKET_2026-07-13.md` |
| Packet SHA-256 | `6ba53824b27c09e97284654e69c68d734781d37a2e7292d9f9b0db012b86fe09` |

## Boundary Results

| Gate | Result |
|---|---|
| Focused DEPLOY, objective, PULL, snapshot, safety, intent, and parity corpus | `185 tests, 0 failures, 0 errors, 0 skipped` |
| Lifecycle, mediator, cutover, trace, parity, and route regressions | `100 tests, 0 failures, 0 errors, 0 skipped` |
| Affected reactor package | `PASS` |
| Rando/ChosenOne changed-hunk normalization | `PASS`, all six repaired mirrored file pairs identical; original nine-pair candidate gate retained |
| Source whitespace audit | `git diff --check` clean |
| Game, browser, VTS, or sandbox execution | `NONE` |
| Push or deployment | `NONE` |

The mediator regression output intentionally includes ERROR diagnostics from fixtures that exercise rejected answers,
chain limits, and acceptance-callback faults. Maven exited 0 and all corresponding assertions passed.

## Transaction Matrix

| Route | Owner | Binding | Accepted transition |
|---|---|---|---|
| Parent deploy action | DEPLOY | Opaque attempt plus action identity and ordinal | `SNAPSHOT -> PARENT_PENDING` |
| Destination child | DEPLOY | Exact attempt plus ordered legal destination refs | `CHILD_PENDING -> COMMITTED` |
| Card-selection buddy | DEPLOY | Exact current/permanent physical ids | `CHILD_PENDING -> COMMITTED` |
| Arbitrary-card buddy | DEPLOY | Temporary wire id kept separate from physical ids | `CHILD_PENDING -> COMMITTED` |
| V170 Undercover | DEPLOY | Exact `PlayCharacterAction` provenance and scanned result ordinal | Finalizer acceptance advances once |
| Capacity/confirmation | DEPLOY | Parent identity refreshed after parent acceptance | Finalizer acceptance advances once |
| Forced destination | DEPLOY | One engine-auto-selected, known legal destination | Parent safety assessment retained |
| Expected deployed-zone change | DEPLOY | Exact physical source and expected zone | `COMMITTED -> COMPLETED` once |

Unexpected zone drift, Force invalidation, phase/game change, cancellation, and rejection clear only the bound opaque
attempt. Reused numeric decision ids and duplicate blueprint copies cannot advance one another.

Direct `decide()` calls record planner acceptance after selecting a wire. Mediator calls defer that same transition to
the engine-acceptance callback. Neither route advances twice. PULL child acceptance follows the same split.

## Choice Matrix

| Choice | Result |
|---|---|
| Optional, all candidates structurally vetoed | Legal Pass/cancel preserved |
| Mandatory, all candidates structurally vetoed | No illegal Pass; exact compatibility wire preserved |
| Exact tie | Original candidate insertion order and first-seen winner preserved |
| Unknown legality | Legacy-unowned or UNKNOWN; never promoted to safe or blocked |

## Required Evidence

- Physical identity: transaction and route fixtures use permanent plus current card ids. The permanent id remains the
  lifecycle identity when the engine rotates a current id; duplicate blueprint copies remain distinct, including
  `tempN` arbitrary-selection wires.
- Force parity: one immutable `ForceObligationVector` is attached to the shared assessment consumed by parent, child,
  Pass, and movement-preservation checks.
- Formation parity: one immutable formation assessment is produced by the planner. Parent and child do not recompute it.
  PULL defers formation until the first exact destination child, then binds that same accepted assessment once.
  Friendly physical-body presence is tri-state; an ability-zero body is PRESENT, known empty is ABSENT, and missing or
  unresolved location facts remain UNKNOWN.
- Forced-destination safety: one common assessment preserves the unflipped named first-pull exemption, exact post-flip
  unsupported-repeat and weak-solo penalties, UNKNOWN identity, and true hard blocks for both bots.
- Copy purity: assessment copies deep-copy deployment instructions; evaluating either bot cannot mutate planner-owned
  instruction state.
- Destination integrity: completion validates attached, at-location, related-starship/vehicle, part-of-system,
  system-orbited, and converted-location relations against the accepted destination, and terminates exactly once on drift.
- Replay integrity: cancellation blocks only the exact opaque attempt. Rejection clears it without poisoning replay;
  a blocked replay remains `ALL_DESTINATIONS_BLOCKED` and cannot reopen an optional child.
- Strategic intents: targeted rescue, Tyranus/direct-contact, safe solo, safe establish, drain denial, and legal
  overpower remain explicit in both planners; deploy weights and solo penalties are unchanged.
- V170 compatibility: positive and zero drain, both Yes/No orders, direct and mediator acceptance/rejection, unknown
  drain, malformed labels, exact ordinal response, one mutation stream, and trace closure are pinned for both bots.
- Mutation boundary: pending state begins only after engine acceptance. Repeated planning returns an equal detached
  assessment and does not mutate transaction state.
- Objective authority: `ObjectiveDeployAdapter` is the live owner of V83/V88/V108/V110, objective-site `200.0f`,
  V193 parent `400.0f`, and V193 child `2000.0f`. Exact predecessors remain present but disabled.
- Route authority: prompt text and phase never establish ownership. Typed origin, action semantic, opaque attempt,
  physical identity, and route-specific metadata must all validate.
- Mirror parity: Rando and ChosenOne use the same planner lifecycle and route logic. The documented Rando-only V79b
  waiver remains outside this cutover.

## Intentionally Retained

- Disabled objective DEPLOY predecessor call sites for interceptor-retirement proof.
- Disabled V170 prompt-text interceptors for the same retirement boundary.
- V99 generic policy, V86/V121 deck-owned policy, formation policy, affordability, and destination safety.
- Scarif/Krennic and V193 guards until their exact typed owners are retired in the frozen sequence.
- All deploy weights and solo-plan penalties. Tuning is a separate later phase.

## Commands

```bash
mvn -q -pl gemp-swccg-server -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=DeployRouteAndFactsTest,DeployTransactionTest,DeployPhaseOwnerTest,DeployPlannerLifecycleParityTest,RandoDeployCutoverIntegrationTest,TheChosenOneDeployCutoverIntegrationTest,ObjectiveAdapterContractTest,PullRouteAndFactsTest,PullPhaseOwnerTest,DecisionSnapshotTest,FormationSafetyCountTest,RandoForceObligationParityTest,TheChosenOneForceObligationParityTest,RandoForcedDestinationDeploySafetyMatrixTest,TheChosenOneForcedDestinationDeploySafetyMatrixTest,DeploymentPlanAssessmentCopyPurityTest,CombinedEvaluatorTieTest test

mvn -q -pl gemp-swccg-server -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=RandoCalAiLifecycleTest,TheChosenOneAiLifecycleTest,SwccgGameMediatorAiLifecycleTest,RandoPullCutoverIntegrationTest,TheChosenOnePullCutoverIntegrationTest,CombinedEvaluatorTraceTest,ObjectiveProfileResolutionParityTest,PullMetadataValueContractTest,DecisionActionSemanticContractTest,ActivateControlRouteResolverTest,RandoDrawEvaluatorScoreParityTest,TheChosenOneDrawEvaluatorScoreParityTest test

mvn -q -pl gemp-swccg-server -am package -DskipTests
```

The Maven commands ran in a separate `gemp_app` container mounted to this worktree. They did not modify or restart the
live server. No game simulation, browser run, VTS run, sandbox scenario, deployment, or push occurred during this gate.
