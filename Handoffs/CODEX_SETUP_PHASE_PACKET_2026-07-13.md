# SETUP Behavioral Cutover Packet

Date: 2026-07-13
Architect/gate: Codex/Alfred
Implementer: K-2/Claude small agent
Authority: `CODEX_SETUP_ROUTE_AUDIT_2026-07-13.md` and
`CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`
Pipeline position: 9, after MOVE; direct-interceptor retirement is step 10
Baseline: `MOVE_COMMIT_TBD`
Status: `FROZEN / PIPELINED: ROUTE CONTRACT ADVANCE, OWNER MOVEMENT HOLD`

## Objective

Cut over each SETUP subroute by exact starting-process provenance and exact wire shape. Never infer
SETUP ownership from turn zero, phase alone, prompt text, candidate text, or card-name heuristics.
Preserve wire response, candidate order, scoring operations, terminal behavior, mutations, fallback,
and direct-interceptor behavior until the exact replacement owner is proven.

## Pipeline Gate

- Steps 1 through 8 in the authoritative cutover order must have clean aggregate gates.
- PULL, objective, DEPLOY, BATTLE, and MOVE ownership must already be settled before SETUP touches
  their shared `CardSelectionEvaluator` surface.
- Freeze every route fixture below before moving one owner.
- Shadow classification comes first. Owner movement is route-by-route. Retirement is a later tranche.
- No game, browser, VTS, sandbox, deployment, reload, restart, or push belongs in this phase packet.

## Route Key

Add the smallest closed provenance seam emitted by the pregame process that creates the decision and
propagated through starting-effect, starting-interrupt, and objective child actions. Resolve a shared
pure `SetupRoute` from that provenance plus a lossless immutable `DecisionSnapshot`.

The resolver may use only:

- exact pregame process and parent action/effect provenance
- decision type and original candidate ordinals
- raw `min`, `max`, and tri-state `noPass` presence/value
- exact array presence, lengths, alignment, and wire identifiers

Labels may retain their legacy meaning only after provenance has established the owned route. Missing,
unknown, duplicated, or misaligned provenance/metadata returns `LEGACY_UNOWNED`. Do not recompute a
route from game state or silently repair input.

## Frozen Subroutes

| Route | Required provenance | Exact wire boundary | Frozen legacy behavior |
|---|---|---|---|
| `SETUP_ACK` | direct acknowledgement from `PlayStartingEffectsGameProcess` | `MULTIPLE_CHOICE` | `HeuristicAiBase` returns ordinal `0` |
| `STARTING_EFFECT_CHILD` | child of the auto-stacked Starting Effect play action | original child wire plus parent/source provenance | Existing evaluator, fallback, and terminal order |
| `OBJECTIVE_BOOTSTRAP_CHILD` | child of the Objective play action stacked by `PlayStartingLocationsAndObjectivesGameProcess` | original child wire plus objective parent/source provenance | Objective discovery and `GameState` writes occur before child execution |
| `STARTING_LOCATION` | direct choice from `PlayStartingLocationsAndObjectivesGameProcess` | `ARBITRARY_CARDS`, `min=1`, `max=1` | Exactly one temporary id is returned |
| `LOCATION_CONVERSION` | direct same-location conversion choice from that process | `MULTIPLE_CHOICE` with original ordinals | Preserve the current response, including the frozen `No` path |
| `STARTING_INTERRUPT` | direct choice from `PlayStartingInterruptsGameProcess` | `ARBITRARY_CARDS`, `min=0`, `max=1`, `noPass` absent | No Pass candidate; current forced selection remains |
| `SAGA_CHILD_V61` | exact `PlayStartingInterruptsGameProcess -> Card217_051 -> Card217_050 -> saga decision` provenance | `MULTIPLE_CHOICE` with the original result array | Preserve V61 selection and its current direct-return lifecycle |
| `UNKNOWN_SETUP` | valid setup provenance with no exact owned arm | any unmatched or incomplete shape | Byte-identical legacy path |

The Starting Effect itself remains auto-selected. Objective JSON starting-location, effect, and
interrupt sets remain inert because they still have no runtime consumer.

`ObjectiveSetupAdapter` is the exclusive owner only for the exact V22 objective-relevant
starting-location operation currently emitted by `CardSelectionEvaluator`. Preserve its candidate
order, objective bonus operation, and float bits, then disable that one predecessor call site before
the final SETUP gate. V22's mentioned-in-starting-interrupt bonus, reserve mirrors, generic setup
tails, V24.10, and every other setup score remain legacy. Do not activate broad JSON effect,
interrupt, or location sets merely because those fields exist.

## Lossless Snapshot

Capture the snapshot before `DecisionTracker`, `ObjectiveAnalyzer`, `DeckOracle`,
`DeployPhasePlanner`, evaluator eligibility, or any fallback can mutate state.

Preserve exactly:

- temporary ids, blueprint ids, testing-text values, selectable flags, and preselected flags
- every raw array's order, length, null entries, and state as `ABSENT`, `PRESENT_EMPTY`, or
  `PRESENT_VALUES`
- raw result/action/card arrays, original ordinals, `min`, `max`, and `noPass` presence/value
- candidate wire ids and the final response string

Do not parse temporary ids as real card ids. Do not install a missing testing array, synthesize
defaults, collapse present-empty into absent, or mutate the legacy parameter map.

## Compatibility And Terminal Order

- Keep evaluator registration, dispatch order, strict-greater replacement, and first-seen tie order.
- Preserve the existing `CardSelectionEvaluator` branch order. V21 terminal guards run before setup
  scoring and must retain exact `-500/+100` results with no downstream contribution.
- Model early exits explicitly as `terminalDecision` and `terminalCandidate`. Preserve every current
  `return`, `continue`, generic tail, untagged base score, and downstream suppression.
- Freeze V29.14's two independently additive `+1000` arms, V43's base/additive interrupt scores,
  V67p's non-fire on canonical temporary ids, and V186's temporary-id base-only path.
- Preserve ForceActivation, Battle, Move, CardSelection, ActionText, Pass, and heuristic-fallback
  collisions. DEPLOY and DRAW remain phase-gated non-owners.
- Preserve canonical starting-interrupt Pass absence even though `min=0`; absent `noPass` must not be
  normalized to an explicit value.

Shadow construction must not duplicate or move side effects. Preserve tracker refreshes, analyzer,
oracle, and planner writes, the second private fallback tracker, starting-location/battleground and
side-marker effects, objective reveal ordering, and all terminal suppression exactly where legacy
performs them.

## Execution Tranches

1. Before releasing the production edit, create a hash-pinned, test-only legacy fixture artifact on
   the already gated MOVE baseline. It must record all eight routes, candidates, contribution
   operations, raw float bits, winners, responses, mutations, and lifecycle events for both bots.
   This is prerequisite evidence, not an intermediate SETUP production commit.
2. In one coherent uncommitted SETUP edit, install lossless snapshot capture and pure route
   shadowing. Legacy alone returns the response while the replacement owners are wired.
3. In that same edit, move acknowledgement/conversion, starting location, and starting interrupt one
   exact route at a time. An owned rejection must not fall through to legacy.
4. Move Starting Effect and objective children only through ordered compatibility calls at their
   existing CardSelection sites. Move only V22's objective-relevant starting-location operation to
   `ObjectiveSetupAdapter` and disable its exact predecessor call site.
5. Move the exact saga owner last using the starting-interrupt descendant provenance chain. Disable
   both live V61 interceptor call sites only after the replacement owner is complete in both bots.
6. Run no tests and make no commit between steps 2 through 5. Keep every disabled predecessor source
   block until step 10, then run one final SETUP suite/package gate and make one phase commit.

## Frozen Fixtures

- `MC_SETUP_FALLBACK`: acknowledgement and same-location conversion, including current `No` choice.
- `START_INTERRUPT_TEMP`: Prepared Defenses, Surface Defense, and Tentacle temporary ids; freeze V43,
  V67p non-fire, `min=0`, absent `noPass`, and forced selection.
- `START_LOCATION_STACK`: Funeral Pyre stacking, battleground state, and side-specific Sith markers.
- `SETUP_ROUTE_MATRIX`: every setup provenance/wire shape plus INTEGER and action-choice collisions.
- `REAL_ID_V21_TERMINAL`: banned and allowed candidates, exact `-500/+100`, no downstream scores.
- `UNKNOWN_SETUP_MATRIX`: V22/V25/V80/V126/V186/V187, universal guards, tails, and terminal bans.
- `SAGA_DIRECT`: shuffled saga results and unknown deck name; freeze the legacy skipped
  finalizer/tracker events, assert the full `PlayStartingInterruptsGameProcess -> Card217_051 ->
  Card217_050 -> saga decision` lineage, and prove the replacement owner's intentional lifecycle.
- `BOOTSTRAP_RESERVE`: no-objective first prompt, objective-revealed child, analyzer/oracle writes,
  dead JSON slots, and inert blueprint-only mirrors.

Winner-only parity is insufficient. Each fixture must prove route, candidate order, contribution
order and operations, raw bits, terminal flags, Pass eligibility, pre-final winner, exact wire,
state events, and trace events.

## Both-Bot Lifecycle And Trace

- Use one immutable snapshot, one owner evaluation, one finalizer call, and one `AiDecisionResult`
  per owned attempt.
- Accepted responses mutate strategic state exactly once. Rejected attempts mutate it zero times and
  carry truthful loop-local rejection history into the retry.
- Preserve exact response wire and original ordinal across trace-disabled and trace-enabled runs.
- Close every accepted, rejected, corrected, or forced trace attempt. Leave no active trace session.
- Record the V61 legacy bypass as baseline evidence. The replacement owner must have one explicit,
  reviewed lifecycle delta and no duplicate tracker/finalizer events before V61 can retire.
- Require normalized Rando/ChosenOne parity for every SETUP-owned route. For `LEGACY_UNOWNED`, compare
  each bot against its own frozen baseline rather than inventing cross-bot equality. Preserve the
  documented V79b Rando-only waiver outside SETUP. Only Rando is live-factory proof; ChosenOne remains
  a source-parity target.

## Retirement Gate

Delete no legacy source in SETUP. Disable only V22's exact objective-relevant starting-location
emitter and the two V61 live interceptor call sites after their exclusive replacement owners,
fixtures, accepted/rejected lifecycle proof, and both-bot parity are complete. Keep generic
CardSelection tails, fallback, all other V22 arms, and every disabled source block until step 10.
Compiled-out cleanup is unrelated and remains out of scope.

## Hard Stops

- Any predecessor gate is incomplete.
- Ownership depends on turn zero, phase alone, decision text, candidate text, or card-name guessing.
- Any raw array, temporary id, or absent/present-empty distinction is lost or repaired.
- CardSelection branch order, terminal suppression, candidate order, score operation, raw bit, Pass,
  tie, response, mutation, or fallback behavior changes without an explicit approved fixture delta.
- An owned route evaluates, finalizes, records, or mutates twice, or falls through after rejection.
- Rando and ChosenOne differ after normalization.
- V61 retirement is proposed before the exact setup-owner and lifecycle fixture passes.
- SETUP production work begins without the hash-pinned legacy fixture artifact, or tests/commits are
  run between internal production-edit stages.
- Any V22 arm beyond the exact objective-relevant starting-location operation moves to
  `ObjectiveSetupAdapter`, or any inert objective JSON slot is treated as a live owner.
