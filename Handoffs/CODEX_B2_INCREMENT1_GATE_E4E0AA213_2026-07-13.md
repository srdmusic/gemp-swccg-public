# B2 Increment 1 Gate: e4e0aa213

Date: 2026-07-13
Reviewer: Codex/Alfred
Commit: `e4e0aa213`
Verdict: ADVANCE as an inert shared scaffold; HOLD all shadow or production consumers

## Verified

- The commit adds six files under the shared `models/common/decision` package.
- There is no production consumer. Repository search finds only the four model classes and two tests.
- `FactValue` distinguishes known false/zero from unknown and preserves producer, provenance, and reason.
- Snapshot and collection values are immutable after construction.
- Candidate ordinal order is enforced by `ordinal == list index`.
- Focused tests pass: 22 run, 0 failures, 0 errors, 0 skipped.
- `git diff --check e4e0aa213^ e4e0aa213` passes.

## Required Before Increment 2

1. Replace string categories with actual types.
   - Use `AwaitingDecisionType` for decision type.
   - Add an obligation enum before the shadow builder emits obligation flags.
   - Add a route enum before any route consumer exists.
   - Replace `FactValue<String>` action/card/source/destination references with distinct stable-id value types. A string labeled "typed" is still a string.

2. Make route evidence machine-checkable.
   - A free-form `routeSelectionEvidence` string cannot prove that route selection used only decision type, phase/window, obligations, and candidate shape.
   - Store a typed immutable route selection record. Human-readable trace text should be derived from it.

3. Remove false/zero defaults for missing response constraints.
   - Builder defaults currently fabricate `noPass=false`, `minimum=0`, `maximum=0`, and `selectable=false` when the raw decision did not provide those facts.
   - Represent known values explicitly and preserve unknown/not-applicable state. Add tests for known false, known zero, absent input, and malformed ranges.

4. Rename observations to their exact engine meaning.
   - `forceAvailable` is ambiguous. Name the exact observed zone/count, such as force-pile size, and record its producer.
   - Define whether `lifeForce` means total life force and which zones contribute.
   - Rename `friendlyPresenceCount` and `opposingPresenceCount` to the actual measurement. If they count non-undercover characters, use that name. Engine presence is a rule concept, not a synonym for character count.

5. Keep power components separate.
   - Base power plus weapon bonus is the correct fact boundary.
   - The snapshot builder must emit UNKNOWN if component resolution fails. It must not convert an exception into a known zero.

6. Reject malformed metadata at construction.
   - Producer id, provenance, and unknown reason must be nonblank, not merely non-null.
   - Decision id, player id, and stable reference ids must be nonblank or use their native numeric/type form.
   - Validate turn, response ranges when applicable, and positive snapshot versions.

## Gate Boundary

The current commit is safe to retain because no runtime path can observe it. Increment 2 may add types and pure tests, but it may not build snapshots beside Rando or Chosen One until the required deltas above pass focused construction tests.

The current `equalSnapshotsFromEqualInputsAreEqual` test proves record equality only. The actual parity gate remains open until the same frozen raw decision is independently built through both bot entry paths and produces byte-identical serialized snapshots with unchanged candidate order.
