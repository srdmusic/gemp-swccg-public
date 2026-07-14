# Parent/Child Deploy Plan Audit

## Scope

Read-only review of the July 12 application log around the turn-1 deploy decision:

`logs/2026-07/app-07-12-2026-1.log.gz`

The line numbers below are decompressed log line numbers.

## Finding

The L3 formation guard correctly blocks an unsafe completed solo formation, but the parent and
child prompts disagree about whether a formation plan exists. The result is nine canceled deploy
attempts in about 0.21 seconds. This is cross-prompt policy contradiction, not a successful final
decision.

At the parent `CARD_ACTION_CHOICE` prompt, V29 identifies affordable pairs:

- FN-2003 then Cardo: lines 62869-62880.
- Cardo then FN-2003: lines 62884-62895.
- Captain Phasma then FN-2003: lines 62899-62910.

At the child `CARD_SELECTION` prompt, FormationSafety treats the same buddy-in-hand fact as a
reason to veto every destination. The first FN-2003 attempt is explicit at lines 62942-62980:

- Parent selects FN-2003 at score `195`: lines 62933-62937.
- All four destinations receive `L3 WEAK SOLO WITH BUDDY IN HAND`: lines 62952-62976.
- The optional child prompt returns empty Pass: lines 62977-62980.
- The engine immediately reopens the parent prompt: line 62981.

The same pattern repeats exactly nine times:

| Source card | Parent selections | Child all-veto results |
|---|---|---|
| FN-2003 | 62933, 63104, 63275 | 62977, 63148, 63319 |
| Cardo | 63435, 63597, 63759 | 63483, 63645, 63807 |
| Captain Phasma | 63910, 64054, 64198 | 63953, 64097, 64241 |

Only after those cancellations disappear from the available parent actions does the route choose
the Reserve Deck Snoke pull at lines 64331-64335.

## Required ownership

One deploy-formation producer must own both prompts. It must return a typed parent assessment and,
when applicable, an explicit sequence plan:

- `SAFE_SOLO`: at least one destination is safe without a follow-up.
- `SAFE_SEQUENCE`: first card, buddy card, shared destination, total force cost, reserved
  obligations, and legal sequence are all known.
- `ALL_DESTINATIONS_BLOCKED`: no solo destination or complete sequence is safe.
- `UNKNOWN`: required facts could not be resolved, with provenance and an explicit unknown policy.

Parent and child must consume the same assessment. A parent deploy action with
`ALL_DESTINATIONS_BLOCKED` cannot open the child prompt. A `SAFE_SEQUENCE` cannot be rejected merely
because its buddy is still in hand; that is the planned first step.

## Existing lifecycle and smallest owner change

No separate mutable `DecisionMediator` is needed. One AI instance already persists for a game and
owns one `DeployPhasePlanner`, while each parent and child prompt receives a fresh
`DecisionContext`. Keep `CombinedEvaluator` stateless and use the existing objects as follows:

- `DeployPhasePlanner` remains the only producer and mutator of deploy sequence assessments.
- `DeploymentPlan` owns the ordered cross-prompt sequence, reserved obligations, provenance, and
  one active sequence cursor.
- `DeploymentInstruction` identifies the exact physical card, not only its blueprint. Duplicate
  copies otherwise remove or advance the wrong instruction.
- Each bot's validated `decide()` response boundary binds the chosen parent action, reconciles
  child success or cancel, and advances the exact sequence cursor.
- Each bot clears active formation state on phase transition. Board change and cancellation must
  invalidate or reconcile the current plan; turn number alone is not a sufficient cache key.
- Parent `DeployEvaluator` and child `CardSelectionEvaluator` consume the same typed assessment.
  Neither evaluator owns lifecycle state or infers a buddy independently.

Current `DeploymentPlan` already has ordered instructions and completion/stale state, but its
budget fields are mostly unused. Current instructions discard physical card identity and known
ability, and `recordDeployment()` removes all instructions with the same blueprint. These are the
specific extension points. Do not create a parallel plan framework.

An exact parent-side `ALL_DESTINATIONS_BLOCKED` result also requires a narrow, non-mutating legal
destination preflight. `CardActionSelectionDecision` does not expose its actions and
`PlayCharacterAction` hides the authoritative target filter. Until that engine truth is available,
the parent assessment must be `UNKNOWN`, not an invented block.

## Non-fixes

- Do not add another deploy score to overpower the child veto.
- Do not broadly suppress a parent deploy when any destination is unsafe. A safe destination or
  complete safe sequence must remain available.
- Do not infer a plan from card title text at the child prompt.
- Do not let evaluator-local caches become the cross-prompt plan owner.
- Do not add a second mutable mediator beside `DeployPhasePlanner` and `DeploymentPlan`.
- Do not use blueprint id as sequence identity when duplicate physical copies can exist.

## Required fixtures

- `B0_L3_ParentDeploy_AllDestinationsVeto_NoReopen`
- `B0_L3_ParentDeploy_ValidBuddySequence_Allowed`

Both fixtures must cover the parent and child prompts as one mediated sequence, not as unrelated
single-prompt score snapshots.
