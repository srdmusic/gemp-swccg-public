# Codex Gate: Finalizer F2 Mediator Retry and Clock

Date: 2026-07-13
Implementation commit: `a095db834`
Implementation parent: `5bd89ac68`
Packet: `Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md`
Verdict: `ADVANCE`
Deployment: held
Push: held

## Scope

F2 changes only the AI branch of `SwccgGameMediator`, adds its narrow package-visible test seam,
adds one focused mediator test class, and records the required changelog entries. The human
`playerAnswered(...)` route is unchanged. No evaluator, score, strategy, objective, finalizer,
trace-capture, card, database, or deployment behavior is changed.

## Contract Review

- `MAX_AI_RESPONSE_ATTEMPTS = 2` means one initial answer plus exactly one iterative retry.
- Only `DecisionResultInvalidException` enters the retry path. Arbitrary runtime failures are not
  converted into invalid-response retries.
- The retry stays inside one `maybeLetAiPlay(...)` invocation and does not increment
  `aiChainCounter`.
- Each checked rejection requeues the same `AwaitingDecision` after the required
  remove-before-callback ordering.
- Acceptance credits the AI decision clock exactly once before chat, pending-action continuation,
  and scheduling the next decision clocks.
- Second rejection preserves the same pending decision and its original timer, does not carry
  pending actions, and reports one visible failure through both the logger and
  `GameState.sendMessage(...)`.
- `MAX_AI_CHAIN` exhaustion uses the same visible terminal reporter and preserves the pending
  decision and timer.
- Terminal suppression is keyed by player plus `AwaitingDecision` object identity. Re-entry with
  the same object neither calls the AI again nor duplicates the message. A new object with the
  same numeric id receives a fresh budget.
- The declared feedback-history residual remains unchanged: `participantDecided(...)` still runs
  before validation on every attempted answer.
- No generic runtime catch was added and the human decision route has no diff.

## Independent Evidence

- Detached review at exact commit `a095db834` against parent `5bd89ac68`.
- Focused F2/finalizer/engine run: 33 tests, 0 failures, 0 errors, 0 skips.
- F1 logic control: 6 tests, 0 failures, 0 errors, 0 skips.
- Reconstructed K-2 combined suite: 122 server tests plus 6 logic tests, exactly 128 total, with
  0 failures, 0 errors, and 0 skips. This resolves the verifier's initial documentation warning.
- Expanded Codex server corpus: 172 tests, 0 failures, 0 errors, 0 skips.
- `mvn -q -pl gemp-swccg-server -am package -DskipTests`: exit 0.
- `git diff --check 5bd89ac68..a095db834`: clean.
- Diff is limited to four expected paths: one production mediator, one focused test class, and two
  changelog files.
- Independent verifier result after evidence reconciliation: functional PASS, documentation count
  independently reproduced.

## Gate Result

Commit `a095db834` advances as the bounded F2 engine repair. This verdict does not authorize
interceptor/finalizer cutover, other mediator changes, deployment, or push.
