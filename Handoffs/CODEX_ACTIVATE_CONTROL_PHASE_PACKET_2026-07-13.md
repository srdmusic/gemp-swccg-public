# ACTIVATE + CONTROL Option 2 Shadow Phase Packet

Date: 2026-07-13
Owner: K-2/Claude implements; Codex/Alfred independently gates
Baseline: gated Trace 4B2 commit `35dea5c5a56fb9fbb1dabdae74107341f1c676ba`
Scope: one coherent edit phase, one verification pass, one commit
Release status: `RELEASED FOR JAVA: OPTION 2 SHADOW PHASE ONLY`

## Objective

Add an engine-owned decision-origin seam and a pure ACTIVATE/CONTROL route resolver without
connecting either to the live AI decision loop. This phase must not change gameplay behavior.

Success requires all of the following:

- a closed `DecisionOrigin` enum in `gemp-swccg-common`
- origin stamps at the five exact engine decision creation sites
- one immutable phase-routing input with separate recipient and turn-player fields
- one deterministic phase/origin/wire-shape resolver
- reasonable fixtures for the five stamps and the route/bypass matrix

## Hard Boundaries

- Do not call the resolver from Rando, Chosen, or any live `decide()` path.
- Do not implement ACTIVATE owners, CONTROL owners, pull ordering, drain extraction, response
  finalizer wiring, or legacy deletion.
- Do not modify evaluator scores/order, fallback behavior, RNG, trackers, or trace capture.
- Do not infer origin from prompt text.
- Keep `DecisionFacts.currentPlayer` recipient-valued. Carry true turn player only in the new
  phase-specific input; do not expand the trace schema.
- Do not run tests while editing. Complete the entire phase, then run section 4 once.
- Do not run games or sandbox scenarios. Do not deploy or push.
- Preserve unrelated dirty files. Stage only the exact files reported for this phase.

## 1. Decision Origin

Add a serialized enum in `src/gemp-swccg-common` with exactly these values:

| Origin | Required wire type |
|---|---|
| `PHASE_ACTION` | `CARD_ACTION_CHOICE` |
| `ACTIVATE_AMOUNT` | `INTEGER` |
| `ACTIVATE_ALLOWANCE` | `INTEGER` |
| `ACTIVATE_ZERO_CONFIRM` | `MULTIPLE_CHOICE` |
| `ACTIVATE_INTERRUPTION_ACK` | `MULTIPLE_CHOICE` |

Use one wire parameter, `decisionOrigin`. Add the minimum protected final helper to
`AbstractAwaitingDecision`; do not expose arbitrary public parameter mutation.

Stamp only these creation sites:

1. `PlayersPlayPhaseActionsInOrderGameProcess`: top-level `CardActionSelectionDecision` gets
   `PHASE_ACTION`.
2. `PlayersPlayPhaseActionsInOrderGameProcess`: zero-activation `YesNoDecision` gets
   `ACTIVATE_ZERO_CONFIRM`.
3. `AbstractSwccgCardBlueprint.getCardPilePhaseActions`: activation amount decision gets
   `ACTIVATE_AMOUNT`.
4. `AbstractSwccgCardBlueprint.getCardPilePhaseActions`: opponent allowance decision gets
   `ACTIVATE_ALLOWANCE`.
5. `AbstractSwccgCardBlueprint.getCardPilePhaseActions`: one-result `OK` acknowledgement gets
   `ACTIVATE_INTERRUPTION_ACK`.

No adapter, bot entry, or trace consumer is in scope.

## 2. Pure Shadow Resolver

Add a phase-specific immutable input containing:

- phase
- typed origin or unowned origin state
- wire decision type
- decision recipient
- current turn player
- ordered `results`
- `defaultIndex` and `defaultValue`

The resolver returns only this closed matrix:

| Phase | Origin | Route |
|---|---|---|
| ACTIVATE | `PHASE_ACTION` | `ACTIVATE_TOP_LEVEL` |
| ACTIVATE | `ACTIVATE_AMOUNT` | `ACTIVATE_AMOUNT` |
| ACTIVATE | `ACTIVATE_ALLOWANCE` | `ACTIVATE_ALLOWANCE` |
| ACTIVATE | `ACTIVATE_ZERO_CONFIRM` | `ACTIVATE_ZERO_CONFIRM` |
| ACTIVATE | `ACTIVATE_INTERRUPTION_ACK` | `ACTIVATE_ACK` |
| CONTROL | `PHASE_ACTION` | `CONTROL_TOP_LEVEL` |
| any | absent, unknown, wrong phase, or wrong shape | `LEGACY_UNOWNED` |

The resolver must be deterministic and side-effect free. It must not read game state, call an
evaluator, mutate trackers, finalize a response, emit trace data, or alter fallback behavior. There
is deliberately no production call site in this phase.

## 3. Fixture Scope

Keep fixture volume proportional to this seam:

- extend `ActivateControlEngineContractTest` to prove the five exact stamps and wire types
- add one resolver test class for the six owned rows plus absent, unknown, wrong-phase, and
  wrong-shape bypass
- include one case where recipient differs from current turn player
- prove resolver determinism and that the input preserves ordered results/defaults

Do not edit or stage these deferred Phase B seed fixtures:

- `ActivateControlLegacyBehaviorTest.java`
- `ActivateControlOwnerResponseTest.java`

They remain untracked for the later behavioral phase. Use exact-path staging so they cannot enter
this commit accidentally.

## 4. Single Phase Gate

Run nothing until all production and fixture edits above are complete. Then run one planned gate:

1. inspect the complete diff and exact five stamp sites
2. run the focused engine-origin and resolver fixtures
3. run the affected common, logic, cards, and server module test/package checks once
4. prove no Rando/Chosen entry point consumes the resolver and trace capture remains disabled
5. run mirror/diff checks and work-verifier

If the gate finds a material defect, correct the coherent phase and repeat the full gate. Do not
fall back to test-per-line or micro-commit churn.

The one commit contains production seam code, the two reasonable fixture surfaces, both changelog
updates, and the durable K-2 handoff update. K-2 sends Codex the SHA, exact file list, commands, and
counts. Codex verifies that exact commit before any later phase is released.

## Deferred Phase B

Not in this release:

- live `decide()` cutover and all five ACTIVATE route owners
- deterministic pull-before-activate behavior
- CONTROL owner and drain assessment extraction
- response-finalizer live use
- universal INTEGER owner removal or any other legacy deletion

Before Phase B, add a unit or `VirtualTableScenario` harness for decide-equivalent candidate
ordering. Do not substitute games or sandbox runs without Steve changing the boundary.

Preserve these known seams for that work:

- drain helper: `ActionTextEvaluator.evaluateForceDrain`, approximately lines 5810-6207 in both
  bot trees
- amount policy: `ForceActivationEvaluator.calculateActivationAmount`
- `CombinedEvaluator` exposes only the winner, so deterministic candidate ordering needs a proven
  candidate-list seam before live cutover

The full deferred design is preserved in
`Handoffs/CODEX_ACTIVATE_CONTROL_DEFERRED_PHASE_B_DESIGN_2026-07-13.md`.

## Worker Report

Return one report to K-2 containing:

- every changed file
- the five engine stamps and resolver matrix implemented
- focused and affected-module test/package counts from the single gate
- proof of no production consumer, no gameplay behavior change, and disabled capture
- exact staging list and commit SHA
- every unresolved mismatch

Do not deploy, push, run games, or run sandbox scenarios.
