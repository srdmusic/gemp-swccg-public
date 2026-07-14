# DEPLOY Phase Owner Cutover Packet

Status: `FROZEN, PIPELINED, NOT YET RELEASED FOR JAVA`

This packet executes step 6 of `CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`. It is released only after the objective facts/adapters commit passes its independent gate.

## Goal

Make the existing `DeployPhasePlanner` and `DeploymentPlan` the single owner of a deploy transaction across parent action, card child, destination, undercover, capacity, confirmation, and forced-destination paths. Preserve current strategic weights in this phase. Deploy-weight and solo-plan tuning remains step 11.

For deploy-from-pile routes, DEPLOY consumes PULL's immutable `PullDeployRef`. PULL keeps search identity, target selection, and outcome ownership. DEPLOY exclusively owns formation, destination safety, Force obligations, and deploy sequencing. This is a handoff, not two owners scoring the same semantic.

## Existing Owners To Reuse

- Extend the existing `DeployPhasePlanner` and `DeploymentPlan`. Do not create a second planner.
- Keep `FormationSafety` pure. `DeployPhasePlanner` invokes it from typed facts, then produces one
  immutable formation assessment. Parent and child evaluators consume that assessment; neither
  `FormationSafety` nor an evaluator owns transaction state or recomputes it.
- Use the shared immutable decision/objective facts from step 5.
- Preserve the accepted-response lifecycle from step 1. Sequence state changes only after an accepted engine response.

## Transaction Contract

One transaction binds:

- game identity, turn, phase, player, an opaque parent-attempt identity, and the diagnostic numeric
  parent decision id; the reusable numeric id is never the lifecycle key;
- exact physical source card, not only blueprint id;
- parent action ordinal and action identity;
- ordered legal destinations and child candidates;
- exact selected destination, undercover/capacity choice, and buddy sequence;
- one immutable formation assessment and one `ForceObligationVector`;
- accepted response, cancel, invalidation reason, and sequence cursor.

Lifecycle is `SNAPSHOT -> PARENT_PENDING -> CHILD_PENDING -> COMMITTED -> COMPLETED`. The expected
accepted source-to-deployed zone transition advances or completes the exact opaque parent attempt once;
it is not an invalidation. Invalidate on child cancel, unexpected zone drift, Force change that breaks
affordability, phase change, game change, engine rejection, or invalid response. Duplicate blueprint
copies and reused numeric decision ids must never advance or remove each other. PULL retains ownership
of search outcome; DEPLOY records only the accepted deployment transition.

Ordered legal destinations require a narrow, non-mutating engine legality preflight. If that preflight cannot expose exact destination legality without changing game state, the parent assessment is `UNKNOWN`; it must not invent `ALL_DESTINATIONS_BLOCKED` or a safe sequence.

## Ownership Rules

1. Parent and child consume the same physical-card assessment. A parent cannot open an optional child that every destination will reject.
2. `SAFE_SEQUENCE` identifies the exact first card, exact buddy, shared legal destination, ordered costs, obligations, and same-phase availability. "Buddy in hand" is insufficient.
3. A present ability-zero character counts as a physical body. Do not substitute total ability for presence.
4. One forced legal destination still receives the parent safety assessment because the engine auto-selects it without an AI child prompt.
5. Targeted rescue and overpower opportunity remain explicit intents. The AI must continue taking legal opportunities to overpower underpowered solo or low-power enemy positions.
6. Keep Scarif/Krennic and V193 guards until their exact typed owner emits the same result. The unflipped first Krennic pull may proceed; unsupported post-flip repeat pull remains guarded.
7. Keep parent and child objective contributions distinct until this phase proves and moves their exact owner.
8. Make `ObjectiveDeployAdapter` the exclusive live emitter for the closed objective-deploy set:
   My Lord V83/V88/V108/V110, objective-site `200.0f`, and V193 parent `400.0f`/child `2000.0f`.
   Disable every exact predecessor call site before the final phase gate without physically deleting
   its source; step 10 removes it after this commit's gate. Never sum old and new emissions. Preserve
   V99 as generic, V86/V121 as deck-owned, and formation policy as DEPLOY-owned.
9. Own V170 only through exact engine provenance from the bound `PlayCharacterAction` undercover
   choice inside the current deploy transaction. The owned wire is `MULTIPLE_CHOICE` with the original
   `results` array. Prompt text and phase are never ownership evidence. Scan the ordered results for
   the actual Yes and No ordinals. Choose Yes when source-proven opponent active Force drain is greater
   than zero and No when the known amount is zero. Missing provenance, malformed results, or unknown
   drain facts remains legacy-unowned. The replacement must use the shared finalizer and accepted-
   response lifecycle for both direct and mediator entry paths.

## Constraint And Score Compatibility

This phase preserves existing ordered contribution streams and exact float bits before deleting any source arm. Do not use a larger score to overpower an old pseudo-veto.

- Resolve current all-veto behavior explicitly. Mandatory choices must use a typed forced-choice policy. Optional choices may pass or cancel according to the existing wire contract.
- Preserve current additive guards as additive unless a named fixture proves an intentional conversion to a structural constraint.
- Keep V171/V172, V193, formation, targeted rescue, DeployEvaluator, ActionText, and CardSelection contributions distinguishable in the trace.
- Preserve candidate insertion order and strict first-seen tie behavior.

## Required Fixtures

Run every fixture through Rando and ChosenOne with exact operation-stream and response parity.

1. Every typed deploy route among the seven wire shapes has exact opaque parent-attempt binding and
   child route identity. Unrelated `EMPTY`, `INTEGER`, and other non-deploy shapes retain their frozen
   legacy owner and response; phase alone never binds them to a DEPLOY transaction.
2. Parent plus child cancellation replay: no repeated parent reopen for a known `ALL_DESTINATIONS_BLOCKED` plan.
3. `SAFE_SEQUENCE` with exact physical first card and buddy, including duplicate blueprint copies.
4. Child cancel, engine rejection, phase change, game change, unexpected zone drift, and Force
   invalidation clear the cursor once; the expected accepted source-to-deployed transition advances or
   completes once without self-invalidation.
5. Ability-zero body is present; unknown body/legality remains `UNKNOWN`.
6. Forced-destination Scarif/Krennic: unflipped first pull, post-flip unsupported repeat pull, unresolved identity, weak solo/no-plan, and true hard block.
7. My Lord V83/V88/V108/V110, objective-site `200.0f`, and V193 parent `400.0f`/child `2000.0f`
   retain exact operations, float bits, and gates; every predecessor is disabled before the final
   verification, while V99, V86/V121, and formation ownership remain unchanged.
8. Targeted rescue, Tyranus, safe solo, safe establish, direct contact, drain denial, and legal overpower opportunity.
9. Full Force obligation vector is consumed unchanged by parent, child, legal Pass, and movement-preservation assessments.
10. Optional all-veto, mandatory all-veto, exact ties, one legal destination auto-select, finalizer acceptance/rejection, trace close, and mutation mode.
11. Repeated `DeployPhasePlanner` assessment is pure and produces identical output with no state mutation.
12. `DEPLOY_V170_UNDERCOVER`: exact `PlayCharacterAction` provenance, Yes/No results in both orders,
    positive opponent drain, known zero drain, unknown drain facts, malformed results, direct entry,
    mediator entry, accepted response, rejection, one mutation stream, and one closed trace. The exact
    response ordinal and wire must match the frozen legacy behavior for every owned case.

Golden evidence includes raw candidate order, physical ids, route, transaction state, ordered operations with exact float bits, hard-veto bits, pre-safety winner, final response, winning ordinal, rejection history, and accepted mutation count.

## Retirement Boundary

Delete nothing in this phase. After replacement shadow parity is frozen, disable only the old
contribution call sites with an exclusive replacement owner, then run the single phase-boundary gate
against that final disabled state. Disable both live V170 interceptor call sites only after the exact
typed Undercover owner and fixture are installed in both bots. Queue all physical source removal for
step 10. Retain the disabled V170 source, V79b as the documented Rando-only waiver, and unrelated
commented/constant-false blocks. Keep a named retained list in the commit evidence.

## Verification And Commit

No tests while editing. At the phase boundary:

1. Run the focused DEPLOY transaction and compatibility fixtures once.
2. Run accepted-response lifecycle, trace, objective adapter, PULL forced-destination, and mirrored-bot regressions.
3. Run the affected-module compile/package gate and the repeated-planner-purity fixture.
4. Run `git diff --check`, inspect exact files, and prove no unrelated production changes.
5. Make one coherent DEPLOY phase commit.
6. Return commit/parent SHA, exact paths, test counts, transaction matrix, all-veto matrix, physical-identity proof, Force-obligation parity, retained legacy list, and zero game/browser/deploy proof.

## Hard Stops

- Parent and child compute formation or Force facts independently.
- `FormationSafety` or an evaluator owns transaction state, or parent/child recompute its assessment.
- A blueprint id substitutes for exact physical sequence identity.
- The numeric parent decision id substitutes for opaque parent-attempt identity.
- The expected accepted source-to-deployed transition invalidates instead of advancing/completing once.
- Sequence state mutates before engine acceptance.
- Unknown legality is converted to safe or blocked without an explicit route policy.
- A mandatory choice produces an illegal Pass.
- A score magnitude is used as a structural constraint without fixture approval.
- Overpower opportunities are globally suppressed.
- A non-deploy wire shape is captured because it occurs during the DEPLOY phase.
- Any predecessor in the closed objective-deploy set remains live beside `ObjectiveDeployAdapter`, or
  V99, V86/V121, or formation policy is absorbed into that adapter.
- V170 ownership depends on prompt text or phase, Yes/No ordinals are assumed instead of scanned, an
  unknown drain fact becomes Yes or No, or either direct interceptor remains live beside the typed
  owner at the final gate.
- Rando and ChosenOne differ outside the documented V79b waiver.
- Deploy weights or solo-plan penalties are tuned in this phase.
