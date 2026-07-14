# Codex Phase Cutover Order

Date: 2026-07-13
Owner: Codex/Alfred
Status: refreshed authoritative master order after mailbox `m00582`; supersedes the earlier order

## Evidence sample

The existing log harvester found 7,407 Rando decisions across:

- `logs/2026-07/app-07-12-2026-1.log.gz`
- `logs/2026-07/app-07-12-2026-2.log.gz`
- `logs/gemp-swccg.stuck-173119.log`

This sample is suitable for route frequency and prompt-shape evidence only. It is not the executable
parity oracle because the harvester has the limitations recorded in
`CODEX_FIXTURE_HARNESS_REVIEW_E5B393955_2026-07-13.md`.

## Critical routing boundary

A phase is a window, not a sufficient route key. Decisions from several semantic engines occur
inside the same phase.

### DRAW window

- Total: 540 decisions.
- Actual `Choose Draw action or Pass`: 289, all `CARD_ACTION_CHOICE`.
- Other decisions inside DRAW: 251.
- The other 251 include optional responses, failed-search verification, card selection, effect
  choices, and revert approval.

The DRAW phase adapter may own only the typed draw-action route. The remaining decisions must route
to RESPONSE, PULL/SEARCH, CARD-SELECTION, or an interceptor. A router that selects DRAW from
`phase == DRAW` alone is rejected.

The behavior-neutral DRAW route predicate is the conjunction of:

1. Current player's turn.
2. Phase `DRAW`.
3. Decision type `CARD_ACTION_CHOICE`. `ACTION_CHOICE` remains legacy because the stamped
   `PHASE_ACTION` origin and all observed canonical draw prompts use `CARD_ACTION_CHOICE`.
4. Complete ordinal-aligned candidate semantics containing at least one selectable action stamped
   `DRAW_CARD_INTO_HAND_FROM_FORCE_PILE`.
5. No action classified only as a destiny draw. Missing or misaligned semantics remain legacy.

The legacy decision-text check remains compatibility evidence, not the target semantic classifier.
The DRAW packet must add the smallest engine semantic stamp: one closed action-semantic enum, default
`UNKNOWN` on `Action`, ordinal-aligned emission from `CardActionSelectionDecision`, and an override
only on the canonical Force-Pile draw action.

The DRAW adapter also cannot recompute its own facts during shadow construction. The live reserve
target is local `DrawEvaluator` arithmetic with early-return precedence, cache side effects, a cap
before V67z, and an all-character corridor count. Freeze that route before extracting it. Detailed
source audit and fixture contract:
`Handoffs/CODEX_DRAW_ROUTE_AUDIT_2026-07-13.md`.

### SETUP window

- Total: 81 decisions across 11 prompt shapes.
- Types: 54 `ARBITRARY_CARDS`, 23 `MULTIPLE_CHOICE`, 4 `CARD_SELECTION`.
- Twenty-five decisions used heuristic fallback with no V191 candidate list.
- The window includes starting-location, starting-interrupt, Reserve selection, side selection, saga,
  and start-game acknowledgement routes.

SETUP is not the first phase cutover. It needs explicit subroutes and interceptor/fallback capture
before the current CardSelection branches can move.

The required subroutes are acknowledgement fallback, starting-effect children, objective
bootstrap, non-objective starting location, starting interrupt, side/location conversion, direct
saga interception, and unknown-card setup selection. Complete raw arbitrary-card arrays must be
captured before the legacy context drops testing/selectable metadata. Detailed source audit and
fixture contract: `Handoffs/CODEX_SETUP_ROUTE_AUDIT_2026-07-13.md`.

### END_OF_TURN window

- Total: 167 decisions.
- 150 are optional-response pass decisions.
- Seven maintenance choices currently use heuristic fallback.
- One required-response decision exposed two equal-score actions.

This is a useful finalizer/fallback canary, but not a strategic phase migration. It must preserve
mandatory maintenance choices and cannot collapse the window to automatic Pass.

## Ordered cutover

The immutable snapshot, Trace V2, response contract, pure finalizer, and bounded mediator retry
scaffolding already exist. The remaining production sequence is frozen as follows:

1. Accepted-response runtime prerequisite. Add the typed mediator result, truthful loop-local retry
   history, disposition callbacks, post-accept mutation owner, Curator forwarding, and complete
   accepted/rejected trace lifecycle. This is the current phase.
1b. V44/V67j finalizer-owner pilot (immediately after step 1, before ACTIVATE+CONTROL; Codex decision
   m00594 on K-2 recommendation m00593). The lowest-risk end-to-end proof of the runtime finalizer
   submission seam (decideForEngine -> CandidateOrdinal -> ResponseFinalizer.finalize(...) ->
   FinalizedResponse -> AiDecisionResult adapter -> post-accept mutation): the always-accept "approve
   revert" MULTIPLE_CHOICE route reads no board state and cannot regress strategy. Convert BOTH bots
   together, preserve the always-accept semantic (positive-label scan, default ordinal 0), and delete the
   old direct V44/V67j interceptor only in the SAME coherent pilot after owner proof. Gated on the step-1
   runtime commit + the exact reordered-label fixture RR_V44_REVERT_REORDERED. No dependency on any of the
   8 strategic lanes. No game/browser testing.
2. Combined ACTIVATE + CONTROL cutover. These routes share INTEGER and ActionText ownership, so they
   move as one coherent phase rather than the old interleaved sequence.
3. DRAW. Cut over only the typed draw-action route. Preserve local reserve arithmetic, candidate
   order, Pass competition, and the existing chaos-gate RNG position.
4. PULL/SEARCH. Install the canonical parent, child, destination, and outcome transaction. Keep
   `CantSearchCardPileModifier` plus `GameConditions.canSearch*` authoritative for failed-search
   suppression and natural end-of-turn reset. Do not reconnect the dead `DeckOracle` failed-pull
   map. Preserve the forced-`here` Krennic guard until DEPLOY owns the coupled destination route.
5. Objective facts and adapters. This follows PULL because `ObjectivePullAdapter` consumes the pull
   transaction key. Land facts plus compatibility adapters first; enable no profile without a live
   typed consumer and validation.
6. DEPLOY. Consume PULL's immutable search/deploy transaction reference without taking ownership of
   search target or outcome. DEPLOY exclusively owns formation, destination safety, deploy sequence,
   and Force obligations for deploy-from-pile children. Move the parent/child deploy transaction
   atomically, bind physical source/destination, and install the one Force-obligation vector.
7. BATTLE. Consume objective and deploy intent, then own battle legality and opportunity. Preserve
   Steve's explicit rule to overpower underpowered solo or low-power sites when the battle remains
   legal and worthwhile.
8. MOVE. This follows DEPLOY and BATTLE because of V169's parent/ActionText/child arms and shared
   battle feasibility. Move rescue/retreat ownership together and convert pseudo-vetoes only inside
   their proven owner arms.
9. SETUP. Keep it near-last because it shares `CardSelectionEvaluator` with DEPLOY, PULL, objective,
   and MOVE routes. Cut it only after all setup subroutes and terminal-return fixtures are frozen.
10. Direct-interceptor retirement. Retire only after each replacement owner is live: V45 after
    BATTLE, V170 after DEPLOY, V61 after SETUP. (V44/V67j is already converted AND retired in step 1b's
    pilot.) Keep V79b as the documented Rando-only waiver until ChosenOne parity has an owner.
11. Deploy-weight and solo-plan tuning. This is the final behavior pass after structural parity. Put
    solo deploy penalties and buddy/move-plan evidence only through the authoritative `DeployPlan`
    owner's typed ranking channels, with boundary math against every retained positive stack.
12. Aggregate offline gate, fresh Fable review, Codex final gate, then deployment. No game or live
    deployment occurs before all three gates pass.

This order supersedes the earlier CONTROL/objective/ACTIVATE interleave and the earlier placement of
interceptor retirement. Interceptor lifecycle migration is part of accepted-response/finalizer
ownership; deletion of replaced direct branches is a separate last-lane retirement.

## Per-Lane Gate Guards

- Do not cut SETUP before DEPLOY, PULL, and MOVE finish their shared card-selection ownership.
- Do not retire an interceptor before its exact replacement owner and permuted-result fixtures pass.
- Do not enable an objective profile without a live typed consumer and validation.
- Keep the forced-`here` pull-parent guard unchanged until DEPLOY owns the destination transaction.
- Do not cut MOVE until all three current V169 arms are frozen and preserved. MOVE then replaces the
  parent physical-card guess, ActionText retry map, and child blueprint guess atomically through its
  authoritative transaction owner.
- Convert a score-costume veto to typed `hardVeto` only inside its owner lane and only after
  max-positive-stack boundary math plus Done/Pass fixtures.
- Keep deploy-weight and solo tuning last so score magnitudes cannot mask structural contradictions.
- Preserve evaluator registration and first-seen tie order across every shared
  `CardSelectionEvaluator` migration.
- Behavioral phases may disable a fixture-proven predecessor after the new owner is selected, but
  physical deletion waits for step 10. This preserves rollback evidence and keeps one-test-per-phase
  cadence compatible with proof-before-deletion.

Retire a legacy owner only after its exact registry arm has observed coverage plus clean fixture
parity. Comment cleanup may continue independently; compiled-out owners may not.

## DRAW gate corpus

At minimum:

- Empty hand emergency draw (`V42-draw`).
- Force surplus draw-down (`V58`).
- Maintenance floor (`V67w`).
- IAO/Secret Plans reserve adjustment (`V78`).
- Offensive banking block (`V182`).
- Hidden Path transit reserve (`V67z`) with its Deploy-side twin unchanged.
- Loop-soft-blocked draw (`V167`) that remains selectable.
- Pass wins over every bad draw action.
- Draw wins over Pass by the smallest observed margin.
- Non-draw optional response in DRAW window does not enter the DRAW adapter.
- Failed-search verification in DRAW window does not enter the DRAW adapter.
- Blocked draw preserves both the Draw `-200` and ActionText `-100000`, with `hardVeto=false`.
- Reserve target preserves cap-before-V67z and all-character corridor counting.
- Cross-talk fixtures cover Battle, Move, ForceActivation, CardSelection, and heuristic fallback
  decisions inside the DRAW window.

No phase route advances on winner-only parity. Candidate order, contribution operations, raw float
bits, vetoes, pass eligibility, pre-final winner, final response, and intended state events must all
match.
