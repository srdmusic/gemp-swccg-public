# B2 Consistency Gate: `d558248cf`

Date: 2026-07-13
Reviewer: Codex/Alfred
Verdict: inert facts/snapshot model `ADVANCE`; trace consumer/cutover `HOLD`

## Verified

- The commit is limited to `DecisionFacts`, `DecisionSnapshot`, the shared `TraceSnapshots`
  adapter, focused tests, and the two required history documents. It makes no bot-specific policy
  change and remains undeployed.
- The four `fa0f254ac` consistency gaps are closed at the inert-model boundary:
  - an omitted builder turn fails instead of fabricating turn 0; explicit turn 0 remains legal;
  - `RouteSelectionEvidence` carries `selectedRoute`, and construction rejects disagreement;
  - known obligation flags are checked in both directions against known `noPass` and `minimum`,
    while partial unknowns remain lawful;
  - candidate counts cannot exceed frozen rows, and a row cannot carry a raw action/card id beyond
    its claimed count. Id-free surplus rows remain lawful so malformed parallel arrays can be
    represented honestly and marked incomplete by the trace layer.
- Detached-commit focused run passes 97/97 tests: 49 decision-model, 24 combined-trace, 8 tie,
  10 envelope, and 6 bot-entry tests. Zero failures, errors, or skips.
- Detached affected-module package passes:
  `mvn -q -pl gemp-swccg-server -am package -DskipTests`.
- `git diff --check d558248cf^..d558248cf` is clean.

## Boundary

- This advances only the immutable facts/snapshot foundation and its one shared trace adaptation.
- It does not make `CandidateShape` a complete raw-array schema. Presence and length for every raw
  parallel array remain part of the Trace 2b gate in
  `CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md`.
- It does not authorize capture enablement, fixture-oracle status, a phase-owner cutover, deployment,
  or removal of legacy policy. Those remain held until Trace 2b and real decision fixtures pass.
