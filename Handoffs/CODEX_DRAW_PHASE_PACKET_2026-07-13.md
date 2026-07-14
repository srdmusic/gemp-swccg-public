# DRAW Behavioral Cutover Packet

Date: 2026-07-13
Architect/gate: Codex/Alfred
Implementer: K-2/Claude small agent
Baseline: `ACTIVATE_CONTROL_COMMIT_TBD`
Status: `PIPELINED: AWAITING RUNTIME AND ACTIVATE+CONTROL GATES`

## Objective

Cut over only the canonical top-level draw action. Preserve all current score operations, Pass
competition, candidate order, RNG position, and unowned DRAW-window behavior. Extract only the
duplicated reserve arithmetic into one shared pure assessment. Retain each mirrored private method as
a thin delegate until the step-10 retirement boundary; this phase removes duplicated arithmetic, not
the predecessor method shells.

## Hard Boundaries

- One coherent edit, one focused verification pass, one commit.
- No tests while editing; no game, VTS, sandbox, browser, deploy, reload, restart, or push.
- Do not migrate optional responses, PULL/search, failed-search verification, selection children,
  destiny draws, opponent-turn prompts, heuristic fallback, or any other phase.
- Do not change score magnitudes, evaluator registration, first-seen tie order, candidate order,
  raw float bits, Pass semantics, or the chaos RNG draw.
- Preserve unrelated dirty and untracked files.

## 1. Engine Semantic Stamp

Decision text is compatibility evidence, not the owner key. Add the smallest closed semantic seam:

- `DecisionActionSemantic` in `gemp-swccg-common`, including `UNKNOWN` and
  `DRAW_CARD_INTO_HAND_FROM_FORCE_PILE`
- default `UNKNOWN` semantic on `logic/timing/Action`
- ordinal-aligned `actionSemantic[]` emission from `CardActionSelectionDecision`
- one override on the canonical Force-Pile draw action in `AbstractSwccgCardBlueprint`

Do not classify other actions in this phase. The semantic array must be ordinal-aligned with the action
array, and every entry must be nonblank and parse as a recognized enum value. Repeated `UNKNOWN` values
are valid and expected; repeated semantic values are not, by themselves, malformed. Missing, blank,
unrecognized, or misaligned semantic data remains legacy-unowned.

## 2. Owned Route

`DRAW_TOP_LEVEL` requires the exact conjunction:

1. `DecisionOrigin.PHASE_ACTION`
2. phase `DRAW`
3. raw `yourTurn == true`
4. decision type `CARD_ACTION_CHOICE`
5. complete parallel candidate metadata
6. at least one selectable `DRAW_CARD_INTO_HAND_FROM_FORCE_PILE` semantic
7. no destiny-only classification

`ACTION_CHOICE` remains legacy. Resolver input uses captured raw fields only and performs no game,
strategy, objective, oracle, planner, or evaluator reads.

Dispatch in both bots after the existing direct interceptors and the one existing chaos-gate draw,
but before generic `tryEvaluators`. `LEGACY_UNOWNED` follows the byte-identical legacy path.

## 3. Owner And Finalizer

For an owned route:

1. build the existing evaluator context once
2. invoke `CombinedEvaluator` once
3. preserve the complete evaluator registration and contribution order: ForceActivation, Deploy,
   Battle, Move, Draw, CardSelection, ActionText, Pass; evaluators that cannot evaluate still emit no
   contribution, exactly as before
4. translate empty Pass to `ResponseIntent.Pass`; translate a selected action to its original
   `ResponseIntent.CandidateOrdinal`
5. reuse one immutable snapshot and call `ResponseFinalizer` once with the runtime's exact immutable
   `RejectionHistory`
6. require status `ACCEPTED`, exact wire, and zero finalizer RNG draws
7. return one `AiDecisionResult` and bypass legacy emergency fallback, both `DecisionSafety` copies,
   heuristic fallback, and a second evaluator lane

`CORRECTED`, `FORCED`, ambiguous translation, missing snapshot, or unknown contract fact is a hard
stop or typed rejection according to the landed runtime contract. No owned rejection falls through.

## 4. Draw Reserve Assessment

Extract the mirrored private reserve calculation into one pure shared `DrawReserveAssessment` while
preserving call and arithmetic order:

`min(10, DTF + FirstStrike + contestedAny + turn4 + 2*IAO + Verge + maintenance)
 + corridorCharacters`

Freeze these details:

- contested contributes once when any relevant location is contested
- the cap of 10 occurs before corridor-character transit reserve
- corridor counts all characters, not only unique or ability-bearing bodies
- repeated soak reads remain repeated where legacy performs them
- a V67z corridor-read exception is swallowed locally and preserves the already computed, capped
  pre-corridor reserve
- an exception anywhere outside that local V67z boundary returns the outer fallback value 1
- every early return from maintenance through V182 and force-starved handling remains in place

Do not substitute `ForceReserveService`; it does not own this arithmetic.

## 5. Frozen Scoring And Trace

- Preserve evaluator registration and additive merge order.
- Preserve strict-greater winner replacement and first-seen exact ties.
- Preserve dynamic Pass scoring and normal Pass competition.
- A blocked canonical draw remains Draw `-200` plus ActionText `-100000`, with
  `hardVeto == false`.
- Preserve raw operations, raw float bits, pre-safety winner, exact wire, and original ordinal.
- Emit one typed DRAW route while retaining all contribution operations.
- Trace-disabled and trace-enabled wire responses remain identical.

## 6. Surgical Retirement

In this phase:

- replace both private `calculateForceToReserve` bodies with thin delegates to the shared assessment
- prove there is one arithmetic owner and zero duplicated reserve arithmetic after the edit
- defer physical deletion of the two private delegate shells to step 10, after this phase's single
  post-edit verification proves the shared owner
- remove only imports and comments orphaned by extracting the arithmetic

Keep `DrawEvaluator.canEvaluate`, the exact draw arm in `ActionTextEvaluator`, DRAW behavior in
`PassEvaluator`, generic `tryEvaluators`, legacy safety, and fallback. Unowned routes still require
them. Do not leave commented replacement copies.

## 7. Fixture Matrix

- semantic stamp: canonical draw, default unknown, ordinal alignment, repeated `UNKNOWN`, repeated
  recognized values, and missing/blank/unrecognized/misaligned data
- resolver: canonical owner plus wrong phase, opponent turn, `ACTION_CHOICE`, destiny-only, optional
  response, failed-search verification, and selection-child exclusions
- reserve: each component, contested-any collapse, cap 10, post-cap corridor characters, repeated
  reads, local V67z fail-open preserving the capped pre-corridor value, outer exception fallback to 1,
  and call order
- score ledger: all early-return/rule arms, blocked double penalty, exact ties, Pass/noPass,
  smallest draw-over-Pass margin, raw bits, candidate and operation order
- mixed candidates: canonical Force-Pile draw plus a battleground deploy action, proving Battle's
  contribution and the complete eight-evaluator merge order remain present
- cross-talk: Battle, Move, INTEGER, CardSelection, PULL/search, and heuristic fallback remain legacy
- lifecycle: accepted, checked rejection then retry with history counts 0 then 1, typed rejection,
  zero duplicate mutation, and no active trace leak
- exact normalized Rando/ChosenOne parity

Use one shared abstract harness with thin mirrored adapters. Reuse existing trace, tie, snapshot,
runtime, and finalizer contract tests.

## 8. Changed-Path Boundary

Allowed production scope:

- semantic enum, `Action`, `CardActionSelectionDecision`, `AbstractSwccgCardBlueprint`
- new shared DRAW route/input/resolver/assessment/owner types
- both bot entry adapters and both `DrawEvaluator` files
- trace route enum and exact changelog/version-history hunks

No `ForceReserveService`, PULL, objective, DEPLOY, BATTLE, MOVE, SETUP, database, deck library, or
unrelated scoring file changes.

## 9. Verification And Return

After all edits finish, run one focused Maven pass containing the new semantic, route, assessment,
shared harness, mirrored adapter, runtime lifecycle, finalizer, trace, and tie tests. Use
`-Dsurefire.failIfNoSpecifiedTests=false -DskipITs`.

Then run `git diff --check`, exact changed-path proof, one-consumer and one-finalizer-call searches,
normalized mirrored parity, zero-duplicated-arithmetic proof, thin-delegate retention proof, semantic
alignment proof, complete eight-evaluator order proof, RNG-position proof, no-owned-fallback proof,
and unrelated-file exclusion proof.

Create one commit only after all gates pass. Return SHA and parent, changed paths, focused test
command/counts, score/operation parity, lifecycle/history matrix, shared-arithmetic/thin-delegate
proof, intentional trace-route delta, and excluded-file status for Codex's independent aggregate gate.

## 10. Hard Stops

- predecessor runtime or ACTIVATE+CONTROL gate has not passed
- canonical ownership requires decision text instead of the typed semantic stamp
- action semantics cannot remain ordinal-aligned and backward compatible
- any owned path calls an evaluator or finalizer twice
- reserve extraction changes read or arithmetic order
- local V67z fail-open and outer fallback-to-1 exception boundaries cannot remain distinct
- the complete eight-evaluator order or mixed draw-plus-battleground ledger changes
- either private reserve delegate must be physically deleted before the step-10 retirement gate
- any score, tie, candidate, operation, Pass, RNG, response, or mutation delta appears
- an invariant requires a game, VTS, sandbox, browser, or live server
