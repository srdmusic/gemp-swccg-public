# Minimal Decision Trace Hook

Date: 2026-07-13
Owner: Codex/Alfred
Purpose: upgrade the log harvester into an exact fixture source without rewriting every rule first

## Existing choke points

`EvaluatedAction` already centralizes the score mutations that matter:

- Constructor initial score.
- `addReasoning(reason, scoreDelta)` additive score changes.
- `setScore(newScore)` overwrites.
- `hardVeto(reason)` constraints.
- `mergeFrom(other)` additive evaluator merge and veto OR.

`CombinedEvaluator` already owns evaluator invocation order, action-id merge, bucket/rank selection,
winner selection, and pass/veto epilogues. `RandoCalAi` owns route/fallback/final-safety boundaries.

Instrument these choke points. Do not add log regexes to infer missing semantics and do not create a
second evaluator implementation.

## Trace operation

Each immutable operation records:

- Monotonic sequence within one decision.
- Original candidate ordinal and action id.
- Evaluator id, when bound.
- Rule id, domain id, and output kind when explicitly supplied.
- Operation: `INITIAL`, `ADD`, `SET`, `HARD_VETO`, `MERGE`, `RANK`, `SELECT`, or `FINALIZE`.
- Before, delta, and after values as raw float bits where applicable.
- Veto state/reason and human diagnostic text.

The operation log is append-only. A `SET` records before and after; it cannot masquerade as an
additive contribution. `MERGE` records the boundary but does not add a second synthetic score delta,
because the merged source operations already explain the value.

## Legacy compatibility

- Existing constructor scores record `INITIAL` with rule `LEGACY_UNTAGGED`.
- Existing `addReasoning(reason, delta)` records `ADD` with `LEGACY_UNTAGGED`.
- Existing `setScore` records `SET` with `LEGACY_UNTAGGED`.
- Add an explicit overload for migrated arms that supplies rule id, domain id, and kind.
- Never parse V-tags from prose as authoritative identity. Reason text remains diagnostic.
- `CombinedEvaluator` binds its evaluator name to returned operations before merging.

This yields exact evaluator/action mutation traces immediately, then gains exact arm ownership as
each domain migrates. Untagged operations are visible debt, not silently guessed metadata.

## Candidate order

Build an immutable action-id-to-first-ordinal index from the complete frozen input. Every trace event
uses that ordinal. Synthetic Pass receives an explicit synthetic ordinal and source marker. Replace
the merge `HashMap` only in the approved tie-stability delta; trace collection itself must not change
iteration or winner behavior.

## Trace ownership

- `EvaluatedAction`: score/veto operations only.
- `CombinedEvaluator`: evaluator order, merge order, bucket/rank/pass eligibility, pre-final winner.
- Router/Rando entry point: route id, full frozen input, fallback/emergency path.
- `DecisionSafety`: final response and correction reason.
- State recorder: intended memory/state events. Shadow records but does not apply them.

## Pure harness seam

Add a package-visible constructor or factory that accepts an ordered evaluator list and trace sink.
Production uses the normal evaluator list. Pure JUnit tests inject scripted evaluators and a capture
sink; they do not start a server, parse logs, or read replay files.

The sink receives one complete record only after finalization. A no-op sink remains the production
default until shadow capture is explicitly enabled.

## Comparator requirements

- Compare arrays and event sequence in order.
- Compare raw float bits exactly.
- Compare complete veto action/reason sets, not counts.
- Compare rank/bucket/pass eligibility and both pre-final and final winner.
- Reject missing, extra, reordered, truncated, or untagged-required fields.
- Intentional deltas are explicit fixture waivers. Decimal tolerance is never the default.

## Gate

The hook advances only if a no-op trace run preserves current route, score bits, vetoes, winner,
final response, candidate order, Rando/ChosenOne parity, and V191 output. Unit tests must prove that
candidate reordering, a one-bit float change, veto-reason changes, set-versus-add changes, and final
safety changes all fail comparison.
