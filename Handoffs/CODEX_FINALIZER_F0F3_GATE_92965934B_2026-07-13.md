# Finalizer F0 and F3 gate for 92965934b

Date: 2026-07-13
Commit: `92965934b794c31b1211ba0025d732e8227bf583`
Parent: `9c9ea3a1c`
Verdict: `F0 ADVANCE`; `F3 HOLD`

## Independent proof

- Commit scope: 10 files, 2,030 insertions. Five new pure finalization value/service files, three
  fixture files, and changelog/history bookkeeping.
- `git diff --check 92965934b^ 92965934b`: pass.
- Detached affected-module package under Homebrew OpenJDK 25.0.2: pass.
- Detached focused suite: 137 tests, 0 failures, 0 errors, 1 skipped.
  - Prior facts/trace/tie corpus: 112.
  - `EngineAwaitingDecisionContractTest`: 8 reported, with the one named post-F1 checked-bounds
    target intentionally skipped and the current unchecked defect pinned executable.
  - `ResponseFinalizerContractTest`: 17.
- No production source outside `ai/models/common/finalization/` references the new F3 types.
  Runtime ownership, engine callback order, trackers, and RNG remain unchanged.

## F0 verdict

`ADVANCE` the reusable real-engine corpus. It uses fresh concrete decision subclasses, runs the
actual validation methods, preserves the CARD_ACTION_CHOICE empty-under-noPass contradiction,
pins current multiple-choice unchecked bounds, and covers card cardinality, arbitrary-card delta,
and integer bounds. The proxy fixture deviation is acceptable because `Action`, `PhysicalCard`,
and `SwccgCardBlueprint` are interfaces and the helpers preserve identity equality.

## F3 blocking findings

### P0: the advertised V148 policy owner is inert

`ResponseContract.from()` computes `policyPassAllowed`, but no production or test call reads the
accessor. `ResponseFinalizer.finalizePass()` accepts a `Pass` solely when
`emptyWireAccepted` is true and explicitly labels the policy fact advisory. Therefore:

- strategy `Pass` and engine-shape `Acknowledge` collapse to the same empty response;
- a policy-prohibited strategy decline is accepted whenever the engine can parse empty;
- the changelog claim that F3 owns one V148 pass-legality semantic is false.

Required correction: `Pass` must consume `policyPassAllowed`. Preserve the engine contradiction by
using `Acknowledge` for the declared EMPTY/CARD_ACTION_CHOICE acknowledgement shapes. Do not allow
generic `Acknowledge` to bypass pass policy for CARD_SELECTION or ARBITRARY_CARDS.

### P0: FORCED results have no typed forced-choice reason

The frozen finalizer contract requires a forced-choice reason. `FinalizedResponse` carries status,
corrections, rejection, and optional RNG metadata, but no typed force reason or detail. Deterministic
INTEGER defaults and first-N card fills can therefore return `FORCED` with no correction, rejection,
random draw, or explanation.

Required correction: add a typed forced-choice reason/detail record, require it for every `FORCED`
result, prohibit it for other statuses, and assert exact reasons in the real-engine corpus.

## Order hold

The frozen packet places F3 after F0 through F2. F1 checked bounds and F2 mediator retry/clock
repair have not landed. The inert F3 files may remain on the branch while corrected, but they do not
authorize interceptor migration, runtime shadow wiring, owner retirement, or deployment.

## Required re-gate

1. Correct the two P0 contract gaps in one narrow F3 commit.
2. Update the changelog/history claims.
3. Add tests proving policy-prohibited `Pass` differs from legal `Acknowledge` and every FORCED
   response carries a typed reason.
4. Re-run the 137-test corpus and affected-module package in a detached worktree.
5. Keep zero production consumers and no deployment.
