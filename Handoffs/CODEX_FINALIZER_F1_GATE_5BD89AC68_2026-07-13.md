# Codex Gate: Finalizer F1 Multiple-Choice Bounds

Date: 2026-07-13
Implementation commit: `5bd89ac68`
Implementation parent: `ec886934b`
Packet: `Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md`
Verdict: `ADVANCE`
Deployment: held
Push: held

## Scope

F1 adds one ordinal range guard to the real `MultipleChoiceAwaitingDecision` validator, focused
logic tests, activation of the previously ignored server contract, retirement of its contradictory
unchecked-exception pin, and required changelog entries. It does not change AI scoring,
evaluators, strategy, objectives, finalizer mapping, mediator retry behavior, deployment, or trace
capture.

## Contract Review

- Parsed ordinals are range-checked before `_possibleResults[index]` is evaluated.
- Negative and exactly-size ordinals now throw the checked `DecisionResultInvalidException`.
- The existing warning text remains exactly `Unknown response number`.
- Valid first and last ordinals still map to the result at that exact array position.
- Non-numeric input remains checked-rejected.
- The formerly ignored post-F1 contract is active. The old unchecked
  `ArrayIndexOutOfBoundsException` expectation was converted rather than left contradictory.
- Rejected inputs never invoke `validDecisionMade(...)`.

## Independent Evidence

- Detached review at exact commit `5bd89ac68` against parent `ec886934b`.
- `MultipleChoiceAwaitingDecisionTest`: 6 tests, 0 failures, 0 errors, 0 skips.
- `EngineAwaitingDecisionContractTest`: 8 tests, 0 failures, 0 errors, 0 skips.
- `mvn -pl gemp-swccg-server -am -DskipTests package`: exit 0.
- `git diff --check ec886934b..5bd89ac68`: clean.
- Diff is limited to the expected five paths: one production validator, two focused contract test
  files, and two changelog files.
- Independent verifier: PASS.

## Gate Result

Commit `5bd89ac68` advances as the narrow F1 engine repair. This verdict does not authorize F2,
other engine behavior, deployment, or push.
