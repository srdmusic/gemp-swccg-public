# Shared Finalizer Fixture and Mediator Retry Packet

Date: 2026-07-13
Architecture/review owner: Codex/Alfred
Java implementation owner: K-2/Claude
Required baseline: `dde6488e0`
Deployment status: `HOLD`

## Purpose

Open the shared response-finalizer lane without moving any runtime owner. First freeze the engine's
actual response contracts. Then repair the two response paths that cannot currently participate in a
bounded finalizer corpus: unchecked multiple-choice ordinals and stranded AI rejections. Only after
those gates pass may K-2 add a pure shadow finalizer.

This packet does not authorize interceptor convergence, phase-owner cutover, legacy retirement,
enabled trace capture, deployment, or changes to evaluator scoring.

## Frozen increment order

1. `F0`: reusable real-engine decision fixtures.
2. `F1`: checked multiple-choice bounds.
3. `F2`: bounded mediator retry, AI clock repair, and visible terminal failure.
4. `F3`: pure response finalizer in shadow mode using the same fixtures.

Each increment is a separate commit and gate. A failed increment pauses only that increment.
Cleanup and other independent work continue.

## F0: engine-truth fixture home

Use `gemp-swccg-server` as the shared fixture home. It already has JUnit 4 and Mockito and can reach
the logic decision classes through the existing module graph. Do not put the reusable corpus only in
`gemp-swccg-logic`; server finalizer tests would then need duplication or a test artifact.

Add:

- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/finalization/EngineDecisionFixtures.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/finalization/EngineAwaitingDecisionContractTest.java`

`EngineDecisionFixtures` owns fresh-decision suppliers, recording subclasses, and the minimum
Mockito `Action` and `PhysicalCard` helpers. Every assertion must use a fresh decision instance.
Run each accepted/rejected contract twice to expose retained fixture state.

Required real-engine contracts:

| Fixture | Required engine result |
|---|---|
| `fcActionChoiceEmptyRejected` | `ACTION_CHOICE` rejects `""` with `DecisionResultInvalidException`. |
| `fcCardActionNoPassEmptyAccepted` | `CARD_ACTION_CHOICE` accepts `""` and calls back with `null`, even when raw `noPass=true`. Record this contradiction; do not normalize it away. |
| `fcCardSelectionMin0Empty` | `CARD_SELECTION` with minimum zero accepts empty and returns an empty list. |
| `fcCardSelectionMin2Exact` | Two distinct offered ids pass. Empty, one, three, duplicate, and unknown ids fail. |
| `fcArbitraryReturnAnyChange` | Locked preselection plus `returnAnyChange=true` accepts one selectable `tempN` delta despite minimum two. The wire response must not resend locked preselected ids. |
| `fcMultipleChoiceBounds` | A valid ordinal maps to the result at that ordinal. Negative and `size` ordinals must become checked invalid results after F1. |
| `fcIntegerBounds` | Inclusive minimum/maximum pass. Outside and non-numeric values fail. Raw `defaultValue` remains available. |

Source anchors:

- `ActionSelectionDecision.decisionMade`
- `CardActionSelectionDecision.decisionMade`
- `CardsSelectionDecision.decisionMade`
- `ArbitraryCardsSelectionDecision.getSelectedCardsByResponse`
- `MultipleChoiceAwaitingDecision.decisionMade`
- `IntegerAwaitingDecision.getValidatedResult`

Do not use `VirtualTableScenario` for these seven pure contracts. It requires a full game and hides
the small response boundary under unrelated state.

F0 gate:

- All contracts that describe current checked behavior pass.
- The two known red contracts, multiple-choice bounds and arbitrary preselection handling, are
  isolated and named. Do not weaken their expected engine behavior to make the suite green.
- Fixtures create no production code and no global state.

## F1: checked multiple-choice bounds

Current fault: `MultipleChoiceAwaitingDecision.decisionMade` parses an integer and immediately reads
`_possibleResults[index]`. Negative or oversized AI output throws unchecked
`ArrayIndexOutOfBoundsException`, bypassing the mediator's checked retry path.

Minimal production change:

- After parsing, reject `index < 0 || index >= _possibleResults.length` with
  `DecisionResultInvalidException("Unknown response number")`.
- Index only after the check.
- Do not add a generic runtime-exception catch in the mediator. A programming fault is not an
  invalid player response.

Add the narrow logic-module test:

- `src/gemp-swccg-logic/src/test/java/com/gempukku/swccgo/logic/decisions/MultipleChoiceAwaitingDecisionTest.java`

Required cases: valid first and last ordinal, negative, exactly `size`, non-numeric, and label mapping
from a deliberately permuted result array.

Promote the shared F0 contract in the same F1 commit: remove the `@Ignore` from
`fcMultipleChoiceBounds_checkedAfterF1` and retire or convert
`fcMultipleChoiceBounds_todayUncheckedOrdinalPinned`. The old pin explicitly expects
`ArrayIndexOutOfBoundsException`; leaving it active after the checked guard lands is a false red
test. Do not keep both contradictory contracts active.

F1 gate:

- Logic focused test passes.
- Shared `fcMultipleChoiceBounds` passes against the real class.
- Affected-module package passes.
- No unrelated decision behavior changes.

## F2: mediator retry, clock, and terminal visibility

Current source faults in `SwccgGameMediator.maybeLetAiPlay`:

- AI success never calls `addTimeSpentOnDecisionToUserClock`, so its pending timer remains active and
  can charge the bot while another participant thinks.
- A checked invalid answer is requeued, but no retry and no scheduling call follows.
- `MAX_AI_CHAIN` exhaustion returns silently.
- `DefaultUserFeedback.sendWarning` is not a terminal channel for a bot without a browser connection.
- Numeric decision id cannot identify retry ownership. Several decision classes hardcode id `1`.

Required behavior:

- Add `MAX_AI_RESPONSE_ATTEMPTS = 2`, meaning one initial answer plus exactly one retry.
- Retry iteratively inside one `maybeLetAiPlay` call. Do not recurse.
- The retry does not increment `aiChainCounter`.
- On each checked rejection, requeue the same `AwaitingDecision` immediately.
- On acceptance, call `addTimeSpentOnDecisionToUserClock(playerId)` exactly once, before chat,
  pending-action continuation, and new clock scheduling.
- After the second checked rejection, retain the same pending decision and its original timer. Do not
  carry pending actions.
- Report terminal retry exhaustion once through `GameState.sendMessage` and the server logger.
- Route `MAX_AI_CHAIN` exhaustion through the same visible terminal reporter.
- Suppress repeated calls for the same terminal decision object without emitting duplicate messages.
- Key terminal ownership by player plus `AwaitingDecision` object identity. Keep numeric id only for
  diagnostics. A new decision object receives a fresh budget even when its numeric id is also `1`.
- Preserve remove-before-callback ordering because accepted callbacks can synchronously create child
  decisions.

Declared residual: `participantDecided` updates feedback history before validation. An invalid
attempt therefore appears in feedback history. Do not broaden F2 into a feedback redesign; freeze
this behavior in the test and revisit only with a separate engine transaction contract.

Narrow test seam:

- Add a package-private constructor accepting `gameId`, `SwccgGame`, `DefaultUserFeedback`, and the
  player-clock map.
- Make `maybeLetAiPlay` package-private for the terminal re-entry assertion.
- Use public `startGame()` for normal scheduling.
- Do not add a clock abstraction. Assert timer-map membership and lifecycle, not elapsed milliseconds.

Add:

- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/game/SwccgGameMediatorAiRetryTest.java`

Use a real `DefaultUserFeedback`, mocked `SwccgGame` and `GameState`, a real `AiRegistry` registration
with `unregisterGame` cleanup, a scripted `SwccgAiController`, and a real anonymous
`IntegerAwaitingDecision` constrained to `1..1`.

Required tests:

1. `invalidOnceRetriesOnceThenAccepts`: answers `"0"`, then `"1"`; two AI calls, one accepted
   callback, one pending-action continuation, no pending decision, AI timer removed.
2. `invalidTwiceReportsTerminalAndPreservesDecision`: answers `"0"`, then `"2"`; two AI calls, no
   continuation, same decision pending, AI timer retained, one visible game-state message.
3. Invoke `maybeLetAiPlay` again after terminal exhaustion; no third AI call and no duplicate message.
4. `chainLimitReportsTerminalAndPreservesDecision`: AI is not called, pending decision and timer
   remain, and the failure is visible.
5. A different decision object with the same numeric id receives a fresh attempt budget.

F2 gate:

- Focused mediator tests pass.
- Existing human `playerAnswered` clock and rejection behavior remains unchanged.
- No arbitrary `RuntimeException` is converted into a retry.
- Affected-module package and diff-check pass.

## F3: pure shadow finalizer

Only after F0 through F2 pass, add:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/finalization/ResponseFinalizer.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/finalization/ResponseFinalizerContractTest.java`

Minimum pure seam:

`finalize(DecisionSnapshot, ResponseContract, ResponseIntent, RandomGenerator, RejectionHistory)`
returns `FinalizedResponse`.

Rules:

- No game, tracker, strategy, planner, cache, chat, or mediator mutation.
- Fixed RNG is injected. Neither mirrored `DecisionSafety` static `Random` may be called.
- `ResponseContract` is derived from raw engine parameters and concrete decision shape. It preserves
  the `CARD_ACTION_CHOICE` empty-response contradiction as observed engine truth.
- ARBITRARY output contains only selectable delta ids. It never resends locked preselected ids.
- The output is submitted to a fresh real `decisionMade` instance from `EngineDecisionFixtures`.
- Rejection remains typed data. F3 does not perform retries itself.
- Run each fixture twice from fresh state and compare exact output plus rejection facts.
- Keep both legacy safety copies authoritative. F3 is shadow-only until exact parity and engine
  acceptance pass for both bots.

F3 gate:

- Shared fixture corpus passes through both legacy and shadow paths with declared contradictions
  visible.
- Fixed RNG makes every fallback deterministic.
- No state-event count changes and no decision callback occurs twice.
- Rando and ChosenOne results compare equal except explicitly declared personality routes.
- Trace capture remains disabled after the commit.

## Cross-lane hold

The base heuristic path finalizes and records a decision before the outer bot repeats safety and
tracking. Trace 2b observes only the outer tracker event. No finalizer or interceptor owner may cut
over until Trace stage 4 records the base mutation and proves exactly one callback, one tracker
record, and one strategic-state mutation per accepted response.
