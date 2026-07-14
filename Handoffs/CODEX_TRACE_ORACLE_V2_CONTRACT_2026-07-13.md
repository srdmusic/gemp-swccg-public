# Exact Decision Trace Oracle V2 Contract

Date: 2026-07-13
Owner: Codex/Alfred
Implementer: K-2/Claude
Status: required before trace capture or shadow comparison

## Purpose

The trace is an executable parity oracle for one legacy decision execution. It is not a production
log and not a second evaluator run. It must prove that route, complete input, ordered candidates,
score operations, vetoes, pass eligibility, winner, final response, and intended state events did
not drift.

The current `55c22fdde` hook remains useful inert instrumentation. It is not the oracle because its
candidate order starts at evaluator merge output, it misses direct/fallback routes and post-safety
responses, and swallowed capture failures can produce a plausible truncated record.

## One immutable envelope

Use one versioned immutable trace envelope. Names may follow local style, but these fields are
required and cannot be replaced with free-form strings:

1. `schemaVersion`: exact supported trace schema version.
2. `botModel`: Rando or ChosenOne source path/personality identifier.
3. `snapshot`: the corrected immutable `DecisionSnapshot`, built once from complete raw input.
4. `status`: `COMPLETE` or `INCOMPLETE`, with ordered typed capture failures when incomplete.
5. `route`: one selected route id plus ordered route evidence and bypass/fallback reason.
6. `operations`: append-only typed contribution/merge/rank/select events with raw float bits.
7. `finalization`: pre-safety winner, pass eligibility, every correction, and final engine response.
8. `intendedStateEvents`: ordered typed AI-side mutation events observed during the one legacy run.

Use `List.copyOf` or equivalent defensive copies at every envelope boundary. Wrapping a caller-owned
list with `Collections.unmodifiableList` is insufficient.

## Frozen input and candidate order

Candidate ordinals come only from the complete raw decision arrays, never from evaluator output or a
merge map. Preserve array presence and each original length for:

- action ids and action text;
- card ids, blueprint ids, and testing text;
- selectable and preselected flags;
- multiple-choice results;
- minimum, maximum, noPass, auto-pass eligibility, and blocked responses;
- any source/destination arrays present on the concrete decision type.

Normalize one ordered candidate row per original ordinal without sorting. If the engine supplies
parallel arrays of different lengths, retain the mismatch in the snapshot and mark the trace
incomplete; do not pad with false, zero, or empty text.

Synthetic candidates use an explicit typed source and a synthetic ordinal. A synthetic Pass with
action id `""` cannot reuse or replace an offered candidate's ordinal.

## Route record

Route selection occurs once and records one stable typed id. The minimum set includes:

- direct optional-forfeit, revert, undercover, saga, and Rando-only parsec interceptors;
- chaos fallback;
- normal CombinedEvaluator route;
- heuristic fallback;
- raw-noPass emergency;
- phase/window subroutes as they are introduced.

Route evidence contains only frozen input facts. It cannot contain a score, assessment, winner, or
mutable service result. The selected route is part of the evidence record and must be validated
against it.

Record every bypass explicitly. A direct interceptor is not allowed to disappear merely because it
returned before the current common finalizer/tracker path.

## Operation record

Keep the useful current distinctions:

- `INITIAL`
- `ADD`
- `SET`
- `HARD_VETO`
- `MERGE`
- `RANK`
- `SELECT`

`SET` has before/after bits and no delta. `MERGE` is a boundary, not a fabricated contribution.
Every score is stored with `Float.floatToRawIntBits`.

Replace free-form `ruleId`, `domainId`, and `outputKind` with validated stable-id value types and a
closed output-kind enum for migrated arms. Unmigrated code uses one explicit legacy-untagged value;
it does not infer a V-tag from reasoning prose.

Each operation references a raw candidate ordinal or an explicit synthetic candidate. Evaluator id
and operation sequence are mandatory. Details/reasoning text may be retained as evidence, but they
do not define identity.

## Finalization record

`CombinedEvaluator`'s selected action is `preSafetyWinner`, not the final response. Record these
separately:

1. pre-safety candidate/winner and score bits;
2. semantic pass/cancel eligibility and the exact facts used;
3. multi-select formatting result;
4. raw-noPass emergency action, if any;
5. every `DecisionSafety` correction with typed reason and before/after response;
6. final response returned by the bot after safety;
7. whether the route skipped the common finalizer under legacy behavior.

The comparator includes all top-level decision fields, the snapshot, status/errors, route,
operations, finalization, and intended state events. Winner-only comparison is rejected.

## State-event observation

Shadow execution remains pure, but legacy AI-side mutations must be visible so the replacement does
not update a tracker twice or skip a planner event. Record typed intended events at the existing
mutation choke points for:

- outer and heuristic decision trackers;
- owner-scoped objective, deck, strategy, shield, and opponent-deck operations;
- only the real deploy-plan operations: begin/replace, deployment record, live flag set, and clear;
- retry budgets, scoring-time barrier target memory, and AMSD retry state;
- actual outer memory writes, pending-concede clear/set, and the engine loss call.

The source-derived cardinality and explicit exclusions live in
`CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md`. In particular, MOVE ladder fields are
candidate-local operation-trace data; there is no current MOVE state-event family. Invented
plan advance/cancel identities and wrapper-level strategic-intent events are forbidden.

The trace observes the one legacy mutation. It never applies a second event and never calls
`decisionMade`.

## Error and lifecycle rules

Instrumentation must not throw into the game decision path. It also must not silently claim
completion.

- Every swallowed capture error marks the current trace `INCOMPLETE` with stage and error class.
- A strict fixture sink fails on any incomplete trace.
- `beginEvaluator` is paired with `endEvaluator` in `finally`.
- Closing always clears the thread-local in `finally`, even when record construction or sink accept
  fails.
- Nested opens, evaluator exceptions, sink exceptions, interrupted finalization, and thread reuse
  have focused tests proving no stale evaluator/session leaks.
- The no-op production sink stays allocation-free and behavior-free.

## Landing increments

1. Add the V2 immutable envelope, typed ids/status/finalization records, comparator, and pure
   construction/deep-copy tests. No runtime consumer.
2. Correct `DecisionSnapshot` consistency, then bind the collector to raw snapshot ordinals. Keep
   the sink disabled.
3. Add mirrored outer route and post-safety response hooks without changing return order, calling
   safety twice, or consuming randomness twice.
4. Add typed mutation observation at existing legacy choke points.
5. Run exact fixtures through both bot entry paths. Require raw-bit equality and complete traces.
6. Enable local capture only after no-op behavior, exception lifecycle, and fixture parity pass.

## Minimum gate corpus

- offered candidate omitted by every evaluator still appears at its raw ordinal;
- duplicate action id across evaluators retains one raw candidate identity plus separate operations;
- parallel raw arrays with different lengths mark the trace incomplete;
- candidate reorder and one-bit float drift fail comparison;
- SET versus ADD and veto-reason drift fail comparison;
- direct interceptor, CombinedEvaluator, heuristic fallback, and raw-noPass emergency each record a
  distinct route;
- synthetic pass never steals an offered empty-id ordinal;
- pre-safety winner changed by final safety records both values;
- evaluator/sink exception marks incomplete and leaves no thread-local/evaluator leak;
- Rando and ChosenOne produce equal normalized envelopes for the same frozen input, except an
  explicitly declared personality route such as Rando-only V79b.

## Gate

Capture, executable oracle claims, shadow comparison, and deployment remain `HOLD` until every
required field is populated from one legacy run, incomplete traces fail strict fixtures, and both
bot entry paths pass the minimum corpus. The current no-op hook may remain installed meanwhile.
