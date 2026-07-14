# ACTIVATE + CONTROL Deferred Phase B Design Reference

Date: 2026-07-13
Owner: K-2/Claude implements; Codex/Alfred independently gates
Baseline: gated Trace 4B2 commit `35dea5c5a56fb9fbb1dabdae74107341f1c676ba`
Scope: Option 2 shadow seam, one coherent edit phase, one verification pass, one commit
Release status: `DEFERRED: NOT RELEASED FOR JAVA`

This preserves the full-cutover analysis that Option 2 deliberately deferred. The active, compact
worker packet is `Handoffs/CODEX_ACTIVATE_CONTROL_PHASE_PACKET_2026-07-13.md`. Do not execute this
reference until the decide-equivalent harness exists and Codex releases a later phase.

## Objective

Land the engine-owned decision-origin seam and a pure ACTIVATE/CONTROL route resolver without
connecting either to the live AI decision loop. This is a shadow foundation phase. It must not
change Rando or Chosen gameplay behavior.

The completed phase contains:

- the closed `DecisionOrigin` enum in the lowest shared module
- origin stamps at the five exact engine creation sites
- one immutable phase-routing input that keeps decision recipient and turn player distinct
- one pure phase/origin/wire-shape resolver
- reasonable fixtures for the five stamps and the route/bypass matrix

Testing is held until every edit in this phase is complete. Then K-2 runs one verification pass and
creates one commit. There are no games, sandbox runs, deploys, or pushes in this phase.

## Corrected Current Truth

- The zero-activation confirmation does not loop forever at current HEAD. Legacy evaluator routing
  emits no candidates, then `HeuristicAiBase.pickMultipleChoice` reads raw `results` and selects
  `Yes`. The defect is unconditional confirmation of Pass, except when V61c intentionally keeps
  three Reserve cards.
- `SwccgGameMediator` now permits one initial response plus one retry, then reports a visible
  terminal failure. AI-side mutations can still execute twice if a response reaches that retry.
- Complete raw parameters, including `results`, defaults, and present-empty arrays, are already in
  `DecisionSnapshot.RawDecision`. Semantic decision origin is still absent.
- `ForceActivationEvaluator.canEvaluate()` still captures every `INTEGER`. That cross-talk is real.
- ACTIVATE Pass can score `2`, `3.5`, `5`, or `8`. CONTROL Pass can reach `2..116` early and
  `5..123` later.
- A viable V192 pull and Activate Force can currently tie at `5500`; first-seen candidate order then
  decides. Candidate array order must not remain the phase policy.

## Non-Negotiable Boundaries

- Do not call the new resolver from either bot entry point or any live `decide()` path.
- Do not implement ACTIVATE owners, CONTROL owners, pull ordering, drain extraction, finalizer live
  use, or legacy deletion.
- Do not modify score magnitudes, evaluator order, fallback behavior, RNG, trackers, or trace
  capture.
- Do not classify by prompt text. Origins come only from the engine creation sites listed below.
- Keep `DecisionFacts.currentPlayer` recipient-valued. Carry the true turn player through the new
  phase-specific immutable input; do not expand the trace schema for this phase.
- Do not run tests while editing. Finish the coherent phase first, then run the single gate in
  section 4.
- Do not run games or sandbox scenarios. Do not deploy, push, or touch a live game.
- Preserve unrelated dirty edits. Stage only the exact files owned by this phase.

## 1. Shared Decision-Origin Seam

Add one small serialized enum in the lowest module already shared by logic, cards, and server. Its
wire parameter and allowed values are closed:

| Origin | Required wire type | Meaning |
|---|---|---|
| `PHASE_ACTION` | `CARD_ACTION_CHOICE` | Canonical alternating top-level action-or-Pass chooser. |
| `ACTIVATE_AMOUNT` | `INTEGER` | Active player chooses own activation amount. |
| `ACTIVATE_ALLOWANCE` | `INTEGER` | Opponent chooses uninterrupted allowance. |
| `ACTIVATE_ZERO_CONFIRM` | `MULTIPLE_CHOICE` | Yes/No confirmation after passing with zero activation. |
| `ACTIVATE_INTERRUPTION_ACK` | `MULTIPLE_CHOICE` | Single-result `OK` acknowledgement. |

Use one parameter name, for example `decisionOrigin`. Add a protected final helper on
`AbstractAwaitingDecision` rather than exposing arbitrary parameter mutation publicly.

Stamp only the known creation sites:

- `PlayersPlayPhaseActionsInOrderGameProcess`: `PHASE_ACTION` and
  `ACTIVATE_ZERO_CONFIRM`.
- `AbstractSwccgCardBlueprint.getCardPilePhaseActions`: `ACTIVATE_AMOUNT`,
  `ACTIVATE_ALLOWANCE`, and `ACTIVATE_INTERRUPTION_ACK`.

Do not add adapter, trace, or bot-entry consumption in this phase. Serialization and parsing helpers
may be added only where the pure resolver fixtures require them. Unknown values must parse as
unowned/legacy, never through prompt inference.

## 2. Pure Shadow Router

Add one pure resolver with no production consumer. Its closed route matrix is:

| Phase | Origin | Route |
|---|---|---|
| ACTIVATE | `PHASE_ACTION` | `ACTIVATE_TOP_LEVEL` |
| ACTIVATE | `ACTIVATE_AMOUNT` | `ACTIVATE_AMOUNT` |
| ACTIVATE | `ACTIVATE_ALLOWANCE` | `ACTIVATE_ALLOWANCE` |
| ACTIVATE | `ACTIVATE_ZERO_CONFIRM` | `ACTIVATE_ZERO_CONFIRM` |
| ACTIVATE | `ACTIVATE_INTERRUPTION_ACK` | `ACTIVATE_ACK` |
| CONTROL | `PHASE_ACTION` | `CONTROL_TOP_LEVEL` |
| any | absent/unknown/incompatible | `LEGACY_UNOWNED` |

Resolve only from an immutable input containing:

- phase
- typed origin or unowned origin state
- wire decision type
- decision recipient
- current turn player
- ordered `results`
- `defaultIndex` and `defaultValue`

The resolver must be deterministic and side-effect free. It must not read game state, call an
evaluator, mutate a tracker, finalize a response, emit trace data, or alter fallback behavior.
Absent, unknown, wrong-phase, and wrong-shape origins return `LEGACY_UNOWNED`.

There is deliberately no live call site in this release. The resolver exists in shadow form so a
later phase can prove decide-equivalent ordering before cutover.

## Deferred Phase B Design Reference (Not Released)

Everything in this section is reference for a later behavioral phase. The Option 2 worker must not
implement, edit, delete, test, or stage any item below in the current shadow phase.

Before any live cutover, the later phase must first add a unit or `VirtualTableScenario` harness for
decide-equivalent candidate ordering. Games and sandbox runs remain excluded until Steve changes
that boundary.

Known seams to preserve for that later phase:

- drain helper: `ActionTextEvaluator.evaluateForceDrain`, approximately lines 5810-6207 in both
  bot trees
- amount policy: `ForceActivationEvaluator.calculateActivationAmount`
- `CombinedEvaluator` exposes only the winner; deterministic pull-before-activate ordering will
  require a candidate-list accessor or another proven ordering seam

### ACTIVATE Owner

Create one ACTIVATE owner for the five routes above. It may call existing pure helpers, but there
must be one visible phase entry point.

#### Top-Level Ordering

Use ordered phase intent, not a larger additive number:

1. viable V192 pull before activation
2. Activate Force
3. Pass

The V192 candidate must pass its existing source-card, availability, destination, once-per-turn,
failed-search, and V61c stand-down gates. This phase does not rewrite the PULL engine. It only makes
the already-documented pull-before-activate order deterministic across candidate permutations.

Preserve these contributions and guards exactly:

- V167 activation soft block `-200`, not a veto
- V168 `+5000`
- V38.3 activation `+500`
- V61c activation stand-down `-6000`
- V67ak pull contribution `+800` outside V192's local clamp

#### Amount

Move the amount policy behind `ACTIVATE_AMOUNT`; delete universal INTEGER ownership. Preserve this
operation order:

1. V57 full-activation default
2. V61c keep three Reserve cards when the existing battle-plausibility predicate is true
3. V67at keep two when three-pile life is `<=10`
4. V43 minimum one when activation remains legal
5. clamp to the engine's raw minimum and maximum

Use `DecisionContext.getLifeForce` three-pile life. Do not substitute
`GameState.getPlayerLifeForce`, which includes unresolved destiny and sabacc cards.

`ACTIVATE_ALLOWANCE` returns the raw maximum. `ACTIVATE_ACK` returns the ordinal of the sole `OK`
result.

#### Zero Confirmation

Find `Yes` and `No` by ordered result labels, never assumed ordinal:

- normal skip with activation still available: return `No` so the same player is prompted again
- V61c intentional keep-three state: return `Yes` to confirm Pass

Malformed or missing labels produce a typed finalizer rejection, not a guessed index.

After the new owner passes its fixtures, remove the replaced ACTIVATE amount/confirmation branches
from `ForceActivationEvaluator` and `ActionTextEvaluator`. Do not leave commented copies.

### CONTROL Owner

Create one CONTROL top-level owner around the canonical `CARD_ACTION_CHOICE`. Preserve the existing
candidate contribution order and additive first-seen merge:

1. ForceActivation
2. Deploy
3. Battle
4. Move
5. Draw
6. CardSelection
7. ActionText
8. Pass

With INTEGER routing fixed, ForceActivation contributes nothing to a CONTROL top-level chooser but
remains in the frozen order for parity. Exact ties retain the earlier candidate. Battle remains its
`+100` base plus ActionText contributions. The MOVE rank ladder and its deferred veto/fine-clamp
order remain unchanged. V67ak remains additive before V192.

The owner may delegate to existing domain evaluators. It must not duplicate their queries or score
them twice. Unhandled CONTROL candidates retain the existing heuristic fallback and its current
ordering until their owning phase/domain is migrated.

#### Drain Consolidation

Extract the current drain helper into one ordered CONTROL assessment, then delete the replaced
inline helper. Preserve exact float operations and terminal behavior:

1. V24.15 nonpositive drain: `-9999`, terminal
2. V189 cost gap/budget failure: `-2000`, terminal; query failure opens the gate
3. V25 dynamic non-battleground plus Simple Tricks: `-9999`, terminal
4. Battle Order affordability: `-50` terminal, or no deployable body `+70` terminal
5. V140 zero-cost drain: `+60`, terminal
6. V104 small drain: `-2000`, nonterminal and suppresses only V52/V48 turn logic
7. V52/V48: turn 3+ `+50`; turns 1-2 `-50`
8. non-Battle Order baseline: deployable body `+50`, otherwise `+70`
9. V52 multi-site: `+300`, `+200`, or `+100` at the existing thresholds
10. Hunt Down: `+40` when its icon condition holds, then `+30`

Preserve V24.2, V52 cancel, V29.14, V23, V184, V192, and all prelude contributions surrounding
the helper. A score labelled "veto" remains additive unless the current source emits a typed
`hardVeto` or returns terminally.

### Typed Response Boundary

Every migrated route produces `ResponseIntent` and passes through the existing
`ResponseContract`/`ResponseFinalizer`. Add the minimum adapter needed to translate a legacy domain
winner to an original candidate ordinal. Do not rewrite all domain evaluators in this phase.

- empty top-level phase choice -> `Pass`
- selected action id -> original `CandidateOrdinal`
- activation integer -> `IntegerValue`
- Yes/No/OK -> original `CandidateOrdinal`

Accepted finalized wire output replaces the two mirrored raw safety/final-format paths only for
these migrated routes. Unmigrated routes retain legacy safety. The finalizer must not draw RNG for
an already-valid migrated intent.

If a migrated response is rejected before reaching the engine, return the typed visible failure
path. Do not silently invoke a second evaluator or mutate trackers again.

### Deletion Boundary

Delete in this commit only when a fixture proves the new owner has the same or intentionally
changed result:

- universal INTEGER `ForceActivationEvaluator.canEvaluate` behavior
- legacy ACTIVATE amount branch now owned by `ACTIVATE_AMOUNT`
- unreachable V38.3 zero-confirm branch now owned by `ACTIVATE_ZERO_CONFIRM`
- old inline CONTROL drain helper after operation-by-operation parity
- comments that describe any deleted owner as live

Do not delete unrelated dormant code, fallback rules, objective adapters, PULL transaction logic,
DEPLOY/BATTLE/MOVE owners, or shared historical changelog entries.

## 7. Single Phase Verification Matrix

All tests land with the implementation and run once as the phase gate.

### Engine And Route

- all five origins serialized from the exact engine call sites
- absent, unknown, wrong-phase, and wrong-shape origins bypass safely
- recipient differs from current turn player
- ACTIVATE and CONTROL start/end transitions unchanged
- action success resets passes; abort retains prior pass count; all-player pass exits

### ACTIVATE

- pull/activate exact tie under both candidate permutations chooses the viable pull
- blocked/nonviable pull leaves Activate Force first
- V61c stand-down and keep-three predicate remain identical across action, amount, confirmation,
  and pull order
- full activation, keep-three, critical-life keep-two, zero-to-one, min/max clamps
- opponent allowance maximum and one-result acknowledgement
- normal zero-confirm chooses No; V61c chooses Yes; reordered result arrays remain correct
- unrelated INTEGER and MULTIPLE_CHOICE decisions match legacy output and never enter ACTIVATE

### CONTROL

- exact evaluator order, first-seen merge, tie behavior, Battle co-sum, MOVE ladder, V67ak/V192,
  and actual Pass bounds
- every drain row above, including terminal/nonterminal interaction and query-failure behavior
- mixed pull, drain, battle, move, draw, and Pass candidates
- start/end trigger child decisions remain on their own route

### Response And Parity

- every migrated intent produces an engine-valid wire response without mediator retry
- valid migrated responses consume no finalizer RNG draw
- Rando and Chosen normalized source/output parity
- trace capture remains disabled
- exact source scan proves deleted branches have one remaining owner

### Commands

Run focused tests for the new route/origin/ACTIVATE/CONTROL/finalizer fixtures, then the complete
affected server and logic module suites, package, mirror parity, diff-check, and work-verifier.
Do not run GEMP games yet.

### Fixture Ownership

Codex seeds the current-compiling baseline in exactly these three new test files:

- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/ActivateControlLegacyBehaviorTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/ActivateControlEngineContractTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/finalization/ActivateControlOwnerResponseTest.java`

The production worker may extend only these three files with target assertions after the real
origin, router, owner, viable-pull, drain-assessment, and response-dispatch APIs exist. Preserve
every compiling baseline assertion. Do not weaken or delete one to make the new implementation
pass. Do not add disabled placeholders or tests against imaginary signatures; API-dependent rows
remain requirements in the verification matrix above until their production seam exists.

Codex baseline gate at `35dea5c5a56fb9fbb1dabdae74107341f1c676ba`:

- focused three-class gate: 11 tests, 0 failures, 0 errors, 0 skips
- full server-module gate with these fixtures present: 894 tests, 0 failures, 0 errors, 25 skips
- preserved baselines: evaluator order, ACTIVATE score/pass/amount behavior, representative CONTROL
  drain score, real engine decision shapes and phase transitions, and deterministic typed finalizer
  acceptance without RNG draws
- intentionally not frozen as baselines: universal INTEGER claiming, zero-confirm returning no
  evaluator candidate, and the legacy normal-zero fallback to Yes. Those are replacement targets,
  not behavior to preserve.
- API-dependent rows still owned by the production tranche: decision-origin route matrix,
  recipient/current-turn separation, viable-pull permutation order, full CONTROL drain terminal
  matrix, pass-counter lifecycle, owner response dispatch, and deleted-owner scans
- omitted rather than fabricated: isolated V43 floor setup, private pass-counter mutation setup,
  and high-end CONTROL Pass fixtures that lack a stable public seam at this baseline

## Commit And Gate

The single commit contains production code, tests, both changelogs, and the durable K-2 handoff
update. K-2 sends Codex the SHA and exact test counts. Codex verifies the exact detached commit,
including deleted-owner scans and capture-disabled proof. Any material failure reopens the whole
phase commit; it does not create a chain of documentation-only correction commits.

## Agent-Ready Brief

Use this brief verbatim for the one ACTIVATE+CONTROL production worker after Codex marks this
packet `RELEASED FOR JAVA` and supplies the gated 4B2 baseline plus exact fixture paths.

### Ownership

You own the complete ACTIVATE+CONTROL production tranche described above across the shared
decision-origin seam, both bot entry points, both mirrored evaluator trees, the extracted phase
owners, response-finalizer adapter, and removal of only the replaced legacy branches. You do not
own the active Trace 4B2 files except where a minimal route enum/recording addition is explicitly
required by this packet. Codex owns the new baseline/target fixture files and will hand their exact
paths to you before dispatch.

You are not alone in this repository. Preserve every unrelated dirty edit. Do not revert, rewrite,
stage, or commit anyone else's work. Do not commit your own work; K-2 verifies and creates the one
phase commit after your report.

### Required Inputs

1. Read this packet once.
2. Read only the exact source files named in its source/behavior sections.
3. Read the Codex fixture files handed off with the release.
4. Use the gated 4B2 SHA as the implementation baseline.

Do not reread the full session history, all handoffs, or the entire changelog.

### Execution Order

1. Add the closed engine-stamped decision-origin seam and raw-input parsing.
2. Add the strict phase/origin/shape resolver with legacy bypass for absent or incompatible origin.
3. Implement all five ACTIVATE routes and their typed intents.
4. Implement the CONTROL top-level owner and extract the ordered drain assessment.
5. Route migrated intents through `ResponseContract`/`ResponseFinalizer` without adding RNG draws
   to valid responses.
6. Run the focused fixtures before deleting anything.
7. Delete only the now-covered legacy ACTIVATE and drain owners, then rerun the full phase matrix.
8. Normalize Rando/Chosen source parity and run the complete affected-module gate.

This remains one tranche. Do not stop after metadata, routing, shadow comparison, or one subroute.

### Hard Stop Conditions

Stop and report to K-2 before proceeding if implementation would require any of these:

- prompt-text inference for route identity
- moving an unrelated DEPLOY/BATTLE/MOVE/PULL/objective owner
- changing a direct interceptor or unmigrated fallback
- changing RNG call count or tracker mutation order on a valid migrated response
- changing raw score magnitudes outside the documented pull-before-activate ordering
- enabling trace capture
- deleting a branch before its replacement fixture passes

### Report To K-2

Return one report containing:

- every changed and deleted file
- the exact origin-to-route table implemented
- intentional behavior deltas versus legacy
- deleted-owner search evidence
- Rando/Chosen parity evidence
- focused and complete module test counts
- package/diff-check results
- proof that `NoOpTraceSink` remains the default
- any unresolved mismatch, even if tests are green

Do not deploy, push, or run GEMP games.
