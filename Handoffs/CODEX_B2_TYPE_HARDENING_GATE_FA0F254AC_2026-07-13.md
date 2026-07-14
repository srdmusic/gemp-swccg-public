# B2 Type Hardening Gate: `fa0f254ac`

Date: 2026-07-13
Reviewer: Codex/Alfred
Verdict: inert types `ADVANCE`; snapshot builder/consumer `HOLD`

## Verified

- Frozen commit changes eight files: the four shared decision types, two pure test classes, and two
  history documents.
- No production source outside `models/common/decision` references `DecisionFacts`, `ActionFacts`,
  `DecisionSnapshot`, or `FactValue`.
- Detached-commit focused run passes 43/43 tests: 12 `FactValueTest` plus 31
  `DecisionSnapshotTest`, with zero failures/errors/skips.
- Detached affected-module package passes:
  `mvn -q -pl gemp-swccg-server -am package -DskipTests`.
- `git diff --check 5240f36c6..fa0f254ac` is clean.
- The commit corrects the original string/default problems: engine `AwaitingDecisionType`, typed
  obligation and route enums, distinct reference types, unknown-capable response/selectability
  facts, exact count/power names, component separation, defensive collection copies, and nonblank/
  range/version validation.
- Changelog and history correctly state zero consumers, no builder parity, and not deployed.

## Corrections Before Any Builder Or Consumer

### 1. An unset builder turn silently becomes a known pregame turn

`DecisionFacts.Builder.turn` is primitive `int`, so omission fabricates `0`. This contradicts the
builder comment that every engine fact is unset until supplied. Use boxed `Integer` in the builder
and reject null before constructing the record, or make turn an explicit fact if it can be unknown.
Add an omitted-turn test as well as the existing negative-turn test.

Source: `DecisionFacts.java:263-280,319-323`.

### 2. Selected route is not part of the checked evidence

The constructor validates evidence type/phase/window/obligations but never validates
`selectedRoute`. A `CARD_ACTION_CHOICE` decision can claim `DecisionRoute.INTEGER` and still pass.
Include the selected route in `RouteSelectionEvidence` and compare it, or enforce the current exact
structural mapping before the semantic route taxonomy expands. Add mismatch tests.

Source: `DecisionFacts.java:136-154,186-233`.

### 3. Derived obligations can contradict their source facts

`obligationFlags`, `noPass`, and `minimum` store the same semantics independently. The record accepts
known `noPass=false` plus `NO_PASS`, or known `minimum=0` plus `MANDATORY_SELECTION`, as long as the
evidence repeats the inconsistent set. Use one typed `DecisionObligations` record over the source
facts or validate the derived flags whenever their inputs are known. Add both contradiction tests
and partial-unknown tests.

Source: `DecisionFacts.java:55-61,107-113,171-180`.

### 4. Candidate-shape evidence is not tied to the snapshot

`CandidateShape` records two counts, but `DecisionSnapshot` validates only ordinals. Existing test
helper `snapshot(1)` combines one `ActionFacts` entry with evidence claiming three action and three
card candidates. This means the evidence is typed but not yet machine-checkable against the frozen
input.

The production-ready shape must retain the presence and length of every raw parallel array needed by
SETUP and other routes, then validate the normalized candidate rows against that shape. At minimum,
reject impossible count/list combinations and add a deliberate mismatch test.

Source: `DecisionFacts.java:205-233`, `DecisionSnapshot.java:36-52`,
`DecisionSnapshotTest.java:39-45,126-135,155-159`.

## Boundary

The commit is safe to retain because it has no runtime consumer. K-2 may continue trace-schema,
registry, fixture, and route work in parallel. Do not construct these snapshots beside either bot,
serialize them as the fixture oracle, or cite route evidence as complete until the four consistency
gaps above and the original Rando/ChosenOne builder parity gate pass.
