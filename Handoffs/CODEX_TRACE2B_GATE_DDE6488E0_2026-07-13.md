# Trace Oracle V2 Increment 2b Gate: dde6488e0

Date: 2026-07-13
Reviewer: Codex/Alfred
Implementation owner: K-2/Claude
Reviewed commit: `dde6488e0`

## Verdict

`ADVANCE` as inert trace-schema and capture-completeness infrastructure.

`HOLD` remains on enabled capture, executable parity-oracle claims, mutation observation,
semantic-route authority, owner cutover, deployment, and legacy retirement.

This increment closes the five repair gaps from
`Handoffs/CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md` closely enough to open the independent
engine-contract fixture and pure shared-finalizer lane. It does not complete Trace V2 stages 4 or 5.

## Independent verification

- Detached worktree at exact commit `dde6488e0`.
- Scope: 23 files, 1,266 insertions, 79 deletions.
- `git diff --check dde6488e0^ dde6488e0`: pass.
- Focused tests: 112 passed, 0 failures, 0 errors, 0 skipped.
- `DecisionSnapshotTest`: 41.
- `DecisionTraceEnvelopeTest`: 19.
- Rando and ChosenOne `CombinedEvaluatorTraceTest`: 13 each.
- Rando and ChosenOne bot trace-hook tests: 3 each.
- `FactValueTest`: 12.
- Rando and ChosenOne `CombinedEvaluatorTieTest`: 4 each.
- Affected-module package: `mvn -q -pl gemp-swccg-server -am package -DskipTests`, pass.
- No push and no deployment.

## Five repaired gaps

### 1. Complete raw decision evidence

The bot boundary now copies the complete `AwaitingDecision.getDecisionParameters()` map into
`DecisionSnapshot.RawDecision`. Keys remain separate, arrays preserve order, duplicates, blanks,
null elements, and present-empty versus absent. The copy is deeply immutable. The pure evaluator
seam labels its already-parsed reconstruction `CONTEXT_EFFECTIVE` instead of claiming engine-raw
provenance.

Accepted boundary: a theoretically present key with a null array is represented as present-empty.
No `setParam(..., null)` producer exists in the current logic source. This normalization is declared
and does not authorize future producer changes without a fixture.

### 2. Typed failure channel

An `open()` construction failure installs a degraded collector carrying a typed `OPEN` failure.
A `finish()` failure returns an `INCOMPLETE` fallback envelope carrying a typed `CLOSE` failure.
`closeAndEmit()` re-offers a sink rejection once with a typed `SINK` failure on the immutable trace.
All bot and seam owners use this emission path.

Accepted boundary: if degraded construction itself fails, or a sink rejects both the original and
typed retry, no third evidence channel exists. Those structurally terminal paths remain best effort
so capture cannot harm gameplay.

### 3. Route-complete status

`TraceCollector.finish()` now requires a selected route, a final response at the bot boundary, and
pass/cancel evidence or an explicit not-applicable reason. `COMBINED_EVALUATOR` additionally requires
a recorded pre-safety winner and at least one operation. Direct, chaos, heuristic, and emergency
routes mark skipped evaluator facts explicitly rather than leaving ambiguous nulls.

### 4. Mandatory operation identity

`TraceOperation` rejects null operation, producer, rule, domain, and output-kind identity.
Framework operations use typed `COMBINED_EVALUATOR` sentinels. Unmigrated arms use explicit
`LEGACY_UNTAGGED` sentinels. Recording choke points fill the sentinels before construction.

### 5. Frozen-shape validation

The selected runtime `TraceRoute` is checked against the frozen wire-decision shape. The four
interceptors whose legacy guards require `MULTIPLE_CHOICE` fail as typed `ROUTE` evidence when the
snapshot disagrees. V45 remains unconstrained because its legacy guard is text-only. Phase remains
an allowed window, not a semantic route key.

This is deliberately wire-shape validation, not final semantic-route authority. The legacy
`DecisionFacts.selectedRoute` name still represents shape, while `TraceRoute` represents observed
runtime execution. Router cutover remains held until typed semantic route facts replace that naming
ambiguity and pass the full route corpus.

## Remaining holds

- No production trace sink is enabled. `NoOpTraceSink` remains the default.
- No complete real-game `decide()` fixture corpus exists.
- Inner mutation observation is not complete at tracker, strategy-refresh, and deploy-plan lifecycle
  choke points.
- Cross-bot normalized-envelope parity and the declared Rando-only V79b personality normalization
  are not executable yet.
- Chaos fallback remains unexercised while `CHAOS_PERCENT == 0`.
- The shared response finalizer, bounded mediator retry, direct-interceptor convergence, phase
  adapters, and owner retirement remain separately gated.

## Next authorized work

1. Build engine-contract response fixtures against the concrete `decisionMade` implementations.
2. Build the pure shared finalizer in shadow mode with fixed RNG injection.
3. Add one bounded mediator retry with visible terminal failure.
4. Finish Trace V2 mutation events and normalized real-decision parity before enabling capture or
   moving any owner.

