# V44/V67J Revert-Approval Finalizer-Owner Pilot

Date: 2026-07-13
Drafted by: K-2/Claude (per Codex decision m00594 on K-2 recommendation m00593)
Gate/architect: Codex/Alfred
Status: `FROZEN BY CODEX; AWAITING GATED RUNTIME COMMIT BEFORE RELEASE`
Baseline: the GATED accepted-response runtime commit (step 1). Do NOT start until the runtime phase
commits AND aggregate-gates. Fill the exact baseline SHA at release.
Master order: `CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md` step 1b (after runtime, before ACTIVATE+CONTROL).

## Why This Pilot Exists

It is the lowest-risk end-to-end proof of the runtime finalizer SUBMISSION seam before the higher-stakes
ACTIVATE+CONTROL cutover bets on it. The runtime phase (step 1) builds the seam (`decideForEngine`, typed
`AiDecisionResult`, `FinalizedResponse -> AiDecisionResult` adapter, disposition callbacks, post-accept
mutation owner) but creates NO finalizer-owned route. This pilot makes the FIRST real finalizer-owned
route, chosen deliberately as the safest one: the V44/V67j "approve opponent's revert" route reads NO board
state and its always-accept semantic cannot regress strategy.

## Dependency + Boundary

- Gated ONLY on: the step-1 runtime commit (the seam types must exist) + the exact reordered-label fixture
  `RR_V44_REVERT_REORDERED`. NO dependency on any of the 8 strategic lanes.
- Convert BOTH bots (RandoCalAi + TheChosenOneAi) together; full parity, NO Rando-only waiver.
- Delete the old direct V44/V67j interceptor ONLY in the SAME coherent pilot, AFTER owner proof.
- One coherent phase edit, one verification pass, one commit. No tests during edits.
- No game, VirtualTableScenario, sandbox, replay, browser, live-server, deploy, or reload.
- Engine/AI files (Steve's blanket all-phase authorization 2026-07-13 covers implementation; deploy still
  triple-locked).

## Current Route (source, for the implementer)

`RandoCalAi.java:655-683` (mirror `TheChosenOneAi.java:655-683`, byte-identical), route enum
`TraceRoute.V44_V67J_REVERT_APPROVAL`:
- Guard: `decisionType == MULTIPLE_CHOICE && decisionText.toLowerCase().contains("revert")` (:655-656).
- Always accepts (Steve's rule :649-651): scans the `results` param for the positive label
  (`yes`/`allow`/`accept`/`ok`/`revert`), returns that ordinal; defaults `yesIndex = 0` when the array is
  absent or no positive label found (:657-670).
- Returns `String.valueOf(yesIndex)` at :683 BEFORE the common finalizer tail (outer
  `decisionTracker.recordDecision` :1060, `trackStrategicEvents` :1076, common `recordFinalResponse` :1083);
  self-records route + `recordEvaluatorLaneNotApplicable` + `recordFinalResponse(..., skippedCommonFinalizer=true)`
  at :676-681.

## Behavior-Neutrality Contract

The old interceptor applies NO outer tracker / strategic mutation (skippedCommonFinalizer=true). To stay
behavior-neutral, the finalizer-owned route MUST also apply NO outer mutation. Therefore:

- When `results` is non-empty, the pilot route computes the SAME positive ordinal via the SAME label scan
  (identical predicate, same default 0). The wire response is byte-identical to today, including reordered
  labels and the no-positive-label fallback to the first offered result.
- On the mediator-facing path it produces `ResponseIntent.CandidateOrdinal(positiveOrdinal)` and calls the
  public `ResponseFinalizer.finalize(...)` entry point with the frozen snapshot, derived response contract,
  fixed RNG, and the exact mediator `RejectionHistory`. The private `finalizeCandidateOrdinal` helper is not a
  production API and must not be exposed for this pilot.
- A valid revert ordinal must finalize as `ACCEPTED`, with the exact result label's original ordinal wire and
  zero RNG draws. A typed rejection remains typed and does not fall through to legacy safety or fallback.
- Absent or empty `results` is a malformed MULTIPLE_CHOICE contract with zero candidates. The legacy route
  returned wire `"0"`, which the engine cannot accept because candidate ordinal zero does not exist. The pilot
  intentionally replaces that invalid submission with typed `ORDINAL_OUT_OF_BOUNDS`, no engine submission,
  no retry, and no legacy fallthrough. Do NOT weaken `ResponseContract` bounds, synthesize a fake candidate,
  or add a raw-wire escape intent to preserve the invalid legacy wire. This is the pilot's one explicit,
  bounded safety correction. Valid non-empty inputs remain behavior-neutral.
- Map the `FinalizedResponse` through the runtime adapter using explicit accepted-mutation mode `NONE`, not
  `OUTER_COMMON`. The trace still defers close to the disposition callback. The accepted callback records the
  exact accepted wire, `ENGINE_ACCEPTED`, and a completed no-mutation outcome, then closes without applying the
  outer tracker or strategic mutation.
- The DIRECT `decide()` path preserves today's exact behavior (same ordinal, same skippedCommonFinalizer
  trace, inline close).
- This is the FIRST production caller of `ResponseFinalizer`. The runtime packet's "ResponseFinalizer has no
  phase-owner caller" proof is intentionally SUPERSEDED for this one route by this pilot (update that proof to
  "exactly one phase-owner caller: the V44/V67j revert route").

This packet intentionally separates typed-finalizer origin from accepted-mutation ownership. The runtime's
existing two-argument adapter remains backward-compatible and defaults to `OUTER_COMMON`. Add the smallest
explicit-mode overload plus result invariant:

- `OUTER_COMMON` typed-finalizer wire: non-null tracker mutation descriptor required and copied unchanged.
- `NONE` typed-finalizer wire: tracker mutation descriptor is not carried or applied; typed-finalizer origin,
  exact decision id, and exact wire remain recorded.
- typed rejection: no mutation mode or tracker descriptor, unchanged.

The overload is pure. It draws no RNG, mutates no tracker, and never calls the mediator. If this separation
cannot be expressed without a wire, score, retry, or direct-call change, hard stop. Do not begin recording an
outer mutation the interceptor never made.

## Scope / Files

- `RandoCalAi.java` + `TheChosenOneAi.java`: replace the V44/V67j interceptor block (:655-683) so the revert
  route, on the mediator-facing path, is finalizer-owned per the contract above; DELETE the old direct
  interceptor return after the owner is proven (same commit). Direct `decide()` path unchanged.
- Reuse existing runtime types (`AiDecisionResult`, `decideForEngine`, `ResponseFinalizer`,
  `ResponseIntent.CandidateOrdinal`, `DecisionRejectionKind`). Extend only `AiDecisionResult` and
  `FinalizedResponseAdapter` with the explicit `NONE` typed-finalizer result described above. Do not add a
  network, HTTP, executor, route registry, or generic policy abstraction.
- Fixtures: `RR_V44_REVERT_REORDERED` (positive option found by label after permutation, both result orders,
  zero RNG); non-empty/no-positive-label fallback to ordinal zero; absent and empty `results` typed
  `ORDINAL_OUT_OF_BOUNDS` with no submission/retry/fallthrough; exact bounds; history forwarding; typed
  rejection without legacy fallthrough; a seam-proof fixture (finalizer, explicit-mode adapter, mediator submit,
  one disposition callback, `NONE` trace close with exact accepted wire); existing adapter default remains
  `OUTER_COMMON`; and normalized Rando/ChosenOne parity.

## Hard Stops

- The revert route would need any board/GameState read to reproduce today's wire.
- A reordered-label input changes the chosen ordinal vs the legacy scan.
- Preserving no-outer-mutation is impossible through the finalizer path without weakening unrelated
  `OUTER_COMMON` invariants.
- The runtime seam types are absent (runtime not committed) — do not start.
- Any valid non-empty-input wire response, score, route, candidate order, RNG draw, retry count, or phase
  behavior changes. The documented absent/empty-results typed rejection is the only permitted divergence.
- The two bots would diverge (parity break).
- The direct `decide()` path loses today's behavior.

## Verification (one focused pass, no game)

Focused Maven, in-container: the new revert-route lifecycle tests, `RR_V44_REVERT_REORDERED`, adapter-mode
invariants, parity, and the existing ResponseFinalizer/adapter/trace-hook tests.
`-Dsurefire.failIfNoSpecifiedTests=false -DskipITs`.
Static proofs: git diff --check; `ResponseFinalizer` now has EXACTLY ONE phase-owner caller (this route) and
the runtime "no caller" proof updated accordingly; the direct V44/V67j interceptor block DELETED in BOTH
bots; normalized Rando/ChosenOne parity; wire response byte-identical to legacy on every valid non-empty
fixture; absent/empty `results` rejected before submission with typed `ORDINAL_OUT_OF_BOUNDS`; NONE-mode
(no outer tracker/strategic mutation) for the route; NoOpTraceSink default; dirty/untracked exclusion.

## Worker Return

commit SHA + parent; changed paths; focused test counts; the seam proof (finalizer -> adapter -> submit ->
disposition close); behavior-neutrality proof for valid non-empty inputs (byte-identical wire + NONE mutation),
malformed-empty safety proof (typed rejection, no submission/retry/fallthrough); interceptor-deletion
proof (both bots); parity; excluded-file status. K-2 reviews + commits; Codex independent aggregate gate.
