# Trace Hook Gate: 55c22fdde

Date: 2026-07-13
Reviewer: Codex/Alfred
Commit: `55c22fdde`
Verdict: ADVANCE no-op instrumentation foundation; HOLD trace capture, oracle use, and deployment

## Verified

- Production construction uses `NoOpTraceSink`; no runtime trace session is opened.
- The no-op path leaves the evaluator merge, bucket walk, pass gates, winner logic, and V191 code in
  the same order. New score hooks return after a thread-local null check.
- Rando and Chosen One `CombinedEvaluator` and `EvaluatedAction` normalize identically.
- Trace values use raw float bits and distinguish INITIAL, ADD, SET, HARD_VETO, MERGE, RANK,
  SELECT, and FINALIZE.
- SET has no synthetic delta. MERGE records a boundary without inventing a second contribution.
- Synthetic pass actions receive explicit identity markers.
- Focused tests pass: 12 run, 0 failures, 0 errors, 0 skipped.
- Complete affected-module package passes at current HEAD.
- `git diff --check 55c22fdde^ 55c22fdde` passes.

## Blocking Findings

1. Candidate order is not the frozen raw input.
   - `TraceCollector.registerCandidate()` is called only when an evaluator-produced action first
     enters the merge map.
   - This loses offered candidates that no evaluator returns, collapses duplicate ids, and adopts an
     evaluator's output order when it differs from the original decision arrays.
   - Open the trace with the complete ordered raw candidate shape from `DecisionContext` or the
     shared `DecisionSnapshot`. Keep a separate action-id-to-first-ordinal index for merge events.

2. `DecisionTrace` is not deeply immutable.
   - `Collections.unmodifiableList(candidateOrder)` and the same wrapper for operations expose later
     mutation of the caller-owned lists.
   - Use defensive copies and add source-mutation tests.

3. The record is not yet a complete fixture schema.
   - It lacks a schema version, typed route, phase/window/obligations, raw candidate arrays,
     assessments/contributions, pass eligibility, and final response/correction.
   - The current FINALIZE is explicitly only the `CombinedEvaluator` pre-final winner. It cannot be
     compared as the AI's final answer until the entry/fallback and `DecisionSafety` boundaries land.
   - The comparator helper also ignores top-level decision id/type/text.

4. Enabled-trace failures can silently truncate evidence.
   - Swallowing errors protects gameplay, which is correct.
   - A capture failure must still mark the trace INVALID/INCOMPLETE or fail a strict test sink. A
     partial trace must never compare as authoritative evidence.
   - End evaluator binding in `finally` and test evaluator/sink exceptions so thread-local state and
     completeness cannot leak across decisions.

5. Tie-determinism tests are still missing.
   - Add equal-score fixtures for normal winner selection, duplicate-id merge order, DPS bucket
     selection, and all-veto/no-pass fallback in both bots.
   - Current trace tests contain no equal-score case, so they do not close the separate 5df276c1b
     deployment gate.

6. Migrated identity remains stringly typed.
   - Legacy operations may remain `LEGACY_UNTAGGED` strings.
   - Before migrated arms use the tagged overloads, rule id, domain id, and output kind need the
     stable registry-backed types approved by B2. Do not parse prose or accept arbitrary new keys.

## Documentation Correction

The changelog currently calls this "the exact-fixture oracle" and says the schema needs no change.
Those claims are false at this increment. It is a useful score/veto instrumentation foundation, but
it cannot become an executable oracle until the blocking findings above are resolved and the full
fixture comparator consumes the complete finalized record.

## Gate Boundary

Keep the commit because the production default is inert and the choke-point design is sound. Do not
enable a sink, capture golden fixtures from it, treat its output as complete, or deploy it yet.
