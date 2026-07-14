# ACTIVATE + CONTROL Decide-Equivalent Harness Packet

Date: 2026-07-13
Architect/gate: Codex/Alfred
Implementer: K-2/Claude small agent
Baseline: `443248a65fd5f3de248e7c777af6ddda53a5a98a`
Status: `RELEASED FOR TEST-ONLY JAVA`

## Purpose

Freeze the current ACTIVATE and CONTROL decision boundary before either shadow route is connected
to a bot entry point. This phase adds executable evidence only. It does not move ownership, change
a score, add a production seam, or cut over a route.

The existing trace surface is sufficient. `DecisionTrace` already exposes raw candidate order,
merge order, ordered operations with raw float bits, the pre-safety winner, and the final response.
Do not add the candidate-list accessor proposed in the older deferred design note.

## Phase Boundary

This is one coherent test-only phase:

1. Add the shared decide-level contract and two thin bot adapters.
2. Finish every edit before running tests.
3. Run one focused verification pass.
4. Create one phase commit and return its SHA to Codex.

No production file under `src/**/main/` may change. No owner, resolver consumer, finalizer consumer,
score, evaluator order, trace schema, capture default, RNG path, tracker, or game behavior may
change.

## Owned Files

New test files only:

- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/AbstractActivateControlDecisionHarnessTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/rando/RandoActivateControlDecisionHarnessTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/chosenone/TheChosenOneActivateControlDecisionHarnessTest.java`

The normal per-commit changelog entries may be added after verification. Stage their exact hunks,
not the whole dirty files.

Explicitly excluded from this phase:

- `ActivateControlLegacyBehaviorTest.java`
- `ActivateControlOwnerResponseTest.java`

Those two deferred seed fixtures use `VirtualTableScenario` and remain untracked. Do not stage,
modify, delete, or count them in this phase.

## Harness Shape

Use one abstract JUnit 4 contract in the common test package and one minimal concrete adapter in
each bot package. The adapters exist only because `setDecisionTraceSinkForTesting` is intentionally
package-visible.

The abstract contract owns:

- a scripted immutable `AwaitingDecision` helper
- a configurable minimal `GameState` subclass
- the six inherited `@Test` methods below
- structured trace assertions

Each bot adapter owns only this operation:

1. Run the decision on a fresh bot with the production-default no-op sink.
2. Run the identical decision on a second fresh bot with `TraceTestSupport.CaptureSink`.
3. Assert both wire responses are identical.
4. Return the traced response and the sink's single `DecisionTrace` to the shared contract.

Do not widen the existing package-visible setter. Do not use reflection. Do not copy the six tests
into both bot packages.

## Pure Fixtures

Use scripted engine-shaped decisions plus the minimal `GameState` stub. Do not instantiate
`VirtualTableScenario`, start a game, run a sandbox scenario, parse a replay, or read a log.

Every scripted decision carries the appropriate `decisionOrigin` wire parameter. The fixture must
assert that the full raw parameter map in the captured snapshot retains that stamp.

| Fixture | Phase | Type and inputs | Frozen current response | Frozen route |
|---|---|---|---|---|
| `activateTopLevel` | ACTIVATE | `CARD_ACTION_CHOICE`; one `Activate Force` action plus optional Pass | offered action id | `COMBINED_EVALUATOR` |
| `controlTopLevel` | CONTROL | `CARD_ACTION_CHOICE`; one `Force drain` action with one aligned source `cardId` plus optional Pass | offered action id | `COMBINED_EVALUATOR` |
| `activateAmount` | ACTIVATE | `INTEGER`; real engine min `0`, max/default `3` | `3` | `COMBINED_EVALUATOR` |
| `activateAllowance` | ACTIVATE | `INTEGER`; min `1`, max/default `3`; recipient differs from turn player in the fixture facts | `3` | `COMBINED_EVALUATOR` |
| `activateZeroConfirmLegacy` | ACTIVATE | `MULTIPLE_CHOICE`; results `Yes`, `No` | `0` (`Yes`) | `HEURISTIC_FALLBACK` |
| `activateInterruptionAck` | ACTIVATE | `MULTIPLE_CHOICE`; sole result `OK` | `0` | `HEURISTIC_FALLBACK` |

The zero-confirm result is evidence of the current defect, not desired policy. Name the assertion
and comment accordingly. A later ACTIVATE owner is expected to create an explicit intentional delta
for normal skip behavior.

If a listed response or route does not match current source when the fixture is constructed, stop
and return the actual trace. Do not edit production code or weaken the fixture to force this table.

The CONTROL fixture is a top-level routing/merge smoke fixture, not the future drain-policy oracle.
Give it one aligned nonblank source `cardId` because the real engine always serializes the attached
action card. The pure stub may resolve that id to null so this fixture does not fabricate a
location or claim to exercise location-dependent drain guards. Those guards remain deferred until
their immutable facts/assessment phase can be verified without a game or sandbox run.

## Required Assertions

For every fixture and both bots, assert:

- traced and untraced wire responses are identical
- `TraceSession.isActive()` is false after each call
- decision id, type, text, phase, origin stamp, obligation flags, and ordered raw arrays are exact
- selected `TraceRoute` is exact
- raw candidate order and merge order are exact and unsorted
- every operation is checked in order, including ordinal, action id, evaluator id, rule/domain/kind,
  raw before/delta/after bits, veto state/reason, and detail
- pre-safety winner, pass eligibility, corrections, and final response are exact
- the final response matches the frozen table above
- no capture failure is ignored; a COMPLETE fixture must use `StrictFixtureSink`

Use structured expected records or explicit field assertions. Do not compare formatted decimals,
hashes, unordered sets, or winner-only projections. Bot identity and bot-specific tracker-owner
labels may differ, but no candidate, score, veto, route, or response field may be normalized away.

The top-level fixture stub may override only getters actually read by these decisions. Its fixed
facts are: side DARK, turn 1, phase supplied by the fixture, configurable current-turn player,
empty hand/permanent/used/lost piles, Force pile 4, Reserve Deck 20, life force 40, and opponent id
`opponent`. The allowance fixture must set current-turn player to an id other than the decision
recipient and assert that distinction directly against the stub. The trace correctly remains
recipient-valued and does not claim to carry a separate turn-player fact. Add another override only
when a captured failure identifies the exact missing read.

## Hard Stops

Stop and report to Codex if any of these occurs:

- a production accessor, setter, adapter, or visibility change appears necessary
- a fixture needs prompt-text routing beyond the existing legacy path
- traced and untraced responses differ
- Rando and ChosenOne differ in candidate, score, veto, route, or response behavior
- a fixture outcome depends on RNG; the existing chaos-gate draw with `CHAOS_PERCENT=0` is allowed
  and must remain behaviorally inert
- a pure stub cannot represent CONTROL without a `VirtualTableScenario`
- any capture is INCOMPLETE after adding only the exact stub getter it reports missing

Do not repair a hard stop inside this phase.

## One Verification Pass

After all edits are complete, run exactly one focused Maven pass containing:

- both new concrete harness classes
- both existing `CombinedEvaluatorTraceTest` classes
- `RandoCalAiTraceHookTest`
- `TheChosenOneAiTraceHookTest`

Use `-Dsurefire.failIfNoSpecifiedTests=false -DskipITs`. Do not run the full module, package,
`VirtualTableScenario`, games, sandbox scenarios, deploy, push, or live reload in this phase.

Then run static checks only:

- `git diff --check`
- changed-path proof that no `src/**/main/` file changed
- normalized bot-adapter parity proof
- production defaults remain `NoOpTraceSink.INSTANCE`
- the two excluded untracked seed fixtures remain excluded

Commit only after all gates pass. Report the commit SHA, focused test counts, changed paths, and the
exact excluded-file status to Codex. Codex independently gates the commit before any next phase.
