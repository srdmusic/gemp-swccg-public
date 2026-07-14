# Codex Review: Fixture Harness `e5b393955`

Date: 2026-07-13
Reviewer: Codex/Alfred
Verdict: evidence harvester `PASS`; executable decision fixture gate `HOLD`

## What passed

- Both Python files compile as source.
- The extractor recognizes the existing decide/result/V191 log shapes.
- It reads plain and compressed logs and emits valid JSONL records.
- The comparator detects coarse winner, top-five membership, score, count, and missing-record changes
  when those fields are present.
- The README accurately discloses several major limitations.

This makes the tool useful for discovering candidate regression cases and measuring observed runtime
coverage. It is not the authoritative behavior oracle.

## Conclusive false-parity tests

Each synthetic pair below returned exit code `0` and `PARITY: no divergences`:

1. Same top-five set and winner, but candidate order reversed.
2. Same action and winner, but score changed from `10.0` to `10.005` under default tolerance.
3. Same type/phase/winner/count, but decision text and veto reason both changed.

All three are divergences under the frozen fixture contract. Candidate order is behavioral, score
bits are exact, and the veto reason/set is more informative than a log-line count.

## Missing authoritative fields

- Stable fixture and decision identity.
- Complete candidate arrays and original ordinals.
- Full untruncated decision, action, testing, and card input.
- Obligation flags and blocked responses.
- Selected route and ordered evaluator invocation.
- Ordered pre-merge contributions with rule/domain/kind and raw float bits.
- Complete merged candidate list, rank/bucket, pass eligibility, and per-action veto set.
- Facts and derived assessments with producer/provenance.
- Winner before and after final safety correction.
- Intended state events without applying shadow mutations.

## Heuristic risks

- Top-five ids are compared as sets, which discards order.
- Decimal tolerance permits unapproved score changes.
- Veto counts can match while the vetoed action or reason changes.
- Positional alignment by heuristic game id cascades after a missing/reordered decision.
- Concurrent games and concurrent bots can attach evaluator lines to the wrong pending decision.
- Tracker state resets per input file, so rotated-file boundaries can create duplicate synthetic game
  ids and ordering dependence.
- Input text and replay arrays are truncated.
- Replay records describe the human client, not server-side Rando decisions.
- No committed automated tests reproduce the claimed divergence checks.

## Commit isolation

The harness commit also deletes three source snapshots and two historical game logs. Those files do
not compile, but they are separate cleanup work. `game_log_latest.txt` is cited by current replay and
AMN audits. Restore it or relocate it to durable evidence and update citations before accepting its
deletion. Future tool, artifact cleanup, production Java, and test changes need separate commits.

## Advance requirements

1. Keep this extractor as a clearly named evidence-harvesting layer.
2. Add production trace hooks or a pure scripted evaluator harness that emits the full frozen record.
3. Make exact comparison the default; intentional deltas use explicit waivers, not tolerance.
4. Compare ordered arrays and contributions, exact raw float bits, complete veto sets, and final
   responses.
5. Add committed unit tests for every comparator field and the three false-parity cases above.
6. Add the named corpus from `CODEX_RANDO_DECISION_FIXTURE_SPEC_2026-07-12.md`.
7. Run Rando and ChosenOne route fixtures plus the focused and complete affected-module tests.

Mailbox gate: `m00241`.

## Correction re-gate: `b544ceba6`

Verdict: corrected evidence harvester `ADVANCE`; executable frozen-fixture oracle `HOLD`.

Verified improvements:

- All 13 committed comparator tests pass.
- Top-five order, decision text, veto count, and ordered veto reasons are compared.
- Score comparison is exact by default; tolerance now requires an explicit argument.
- The veto-list count cap is removed.
- `resources/evidence/game_log_latest.txt` is restored and current citations point to it.

Remaining oracle gaps:

- A direct mutation test changed `clamps`, `fallback`, `bestAction`, `bestScore`, `reasoning`,
  `evaluatorDecision`, `evaluatorScore`, `chosenScore`, `unterminated`, and `topnLines`; the
  comparator returned no divergence.
- The extractor still truncates decision text, reasoning, and individual veto lines.
- Only the logged top five are captured, not the complete ordered candidate list and ordinals.
- Evaluator route order, pre-merge contributions, raw Java float bits, facts, assessment provenance,
  obligations, blocked responses, and pre/post-safety winners are absent.
- Records are still aligned by heuristic game position; there is no stable decision id.
- `load()` ignores records whose source is not `log`, so the comparator cannot consume the planned
  executable fixture source without another schema path.

Boundary: keep `b544ceba6` as the runtime evidence discovery/regression layer. Do not use its parity
result to authorize router cutover or deployment. That gate opens only after the production trace
and executable fixture contract in `CODEX_RANDO_DECISION_FIXTURE_SPEC_2026-07-12.md` are implemented.
