# PULL Route Audit

Date: 2026-07-13
Owner: Codex/Alfred
Source snapshot: `f2bb32e95`; audited pull-policy sources are unchanged through `d558248cf`
Verdict: typed facts and pure assessment `ADVANCE`; PULL owner cutover/retirement `HOLD`

## Route Matrix

PULL is a cross-phase transaction, not one evaluator branch.

| Stage | Wire decision | Current route and owners |
|---|---|---|
| Parent action | `CARD_ACTION_CHOICE` | Parallel action/card/blueprint arrays. Deploy parents merge `DeployEvaluator` and `ActionTextEvaluator`; most other pull parents use `ActionTextEvaluator`. |
| Deploy-from-pile child | `ARBITRARY_CARDS` | Temporary card ids, blueprints, selectable flags. Standard deploy searches currently fall through `CardSelectionEvaluator.evaluateUnknown()`, not `evaluateReserveDeckSelection()`. |
| Take-into-hand child | `ARBITRARY_CARDS` | Same wire shape, routed to `CardSelectionEvaluator.evaluateTakeIntoHand()`. That route sorts candidates before merge. |
| Destination child | `CARD_SELECTION` | Real card ids, routed to `evaluateDeployLocation()`. A single legal destination is auto-selected by the engine, so no AI child decision exists. |

The parent, selected card, and destination therefore form one mediated transaction. A phase-only
controller cannot own it without losing child decisions and forced-destination behavior.

## Blocking Findings

### P1: failed-search memory is disconnected

- The engine installs a same-turn `CantSearchCardPileModifier` after a failed search in
  `ChooseCardsFromPileEffect`.
- `DeckOracle.recordFailedPull()` has no caller.
- V192 and child selection use incompatible failure keys.
- `HeuristicAiBase` owns separate fallback-only failed-pull sets that do not clear.
- The evaluator retry guards are therefore inert and are not a valid transaction lifecycle.

Canonical attempt state belongs at decision mediation: record the selected parent, observe the
engine result, then mark failure or success/reset using one stable transaction key. Do not put
mutable attempt history in `DeckOracle`.

### P1: broad category heuristics can contradict the actual source filter

`DeckOracle` collapses `site`, `battleground`, `docking bay`, and `location` to the broad LOCATION
category for some availability checks. The real card filter can be subtype- and modifier-aware,
including `Filters.battleground`. A generic location in Reserve cannot prove that a battleground
pull will succeed.

Availability facts must preserve the source card's actual Java filter or an exact typed equivalent.
Card text/category heuristics may remain evidence for `UNKNOWN`; they cannot certify `KNOWN true`.

### P1: V192's clamp is not a route-total clamp

The single V192 positive emission is capped at `1750` for deploy-grade pulls or `7100` for the old
activate scope. Separate contributions still merge outside it:

- V67ak objective steering can add `+800`.
- formation can later add `-800`.
- `DeployEvaluator` contributes independently to deploy parents.

A deploy-parent pull can therefore exceed `2550` before other deploy scoring, so the documented
`1950` held-location anchor is not protected. Consolidation must first freeze the complete ordered
contribution ledger. Do not replace the current numbers with one guessed total.

### P2: several advertised blocks are additive scores

`-2000` and `-9999` lines are not structural vetoes unless the action's merged result carries
`hardVeto`. `CombinedEvaluator` sums every applicable evaluator for the action. A large positive
from another owner can still reverse an additive "block."

The new assessment may emit `ALLOW`, `DEFER`, or `BLOCK`, but the compatibility adapter must
preserve the current additive-versus-hard-veto distinction until fixtures prove each intentional
conversion.

### P2: formation protection is partial by engine necessity

Forced-`here` pulls need a parent guard because one legal destination is auto-selected without a
child prompt. `FormationSafety` supplies true hard vetoes for known unsafe cases, but unresolved
text/card identity fails open and "weak solo, no plan" remains an additive `-800`. Parent and child
formation checks must consume one typed deploy sequence; removing the parent guard would reopen the
forced-destination failure.

### P2: tie behavior depends on route order

The live evaluator order is ForceActivation, Deploy, Battle, Move, Draw, CardSelection, ActionText,
then Pass. Strict `>` preserves first insertion on exact ties. `evaluateTakeIntoHand()` also sorts
candidates before merge. A consolidation can change the winner with identical numeric scores if it
changes insertion or candidate order.

## Current Owners

- `DeckOracle`: refreshed zone/catalog facts, side-aware source text, common-filter checks, live
  AMSD failure turn, plus a currently dead failed-pull map.
- `ObjectiveAnalyzer`: mutable objective state and cached playbook profiles. V67ak contributes
  outside V192; V193 contributes at parent and destination stages.
- `ActionTextEvaluator`: V192 parent scorer, source-text/category predicates, objective additions,
  several additive guards, and forced-destination formation handling.
- `CardSelectionEvaluator`: deploy/take child choice, destination choice, and additional formation
  and objective contributions.
- `DeployEvaluator`: independent deploy-parent contributions that merge with V192.
- `HeuristicAiBase`: fallback-only failure sets disconnected from the evaluator guards.

Rando and ChosenOne PULL evaluators are executable-parity mirrors after package/name normalization.
Rando's separate V79b Death Star parsec behavior is unrelated.

## Replay And Decision-Log Grounding

The latest relevant replay is `replays/asdf/ocffe8duo7yxh7fh.xml.gz`, final full-history segment:

- Events `3436-3442`: Command Center pulls Krennic and the objective flips. This is the legitimate
  first-pull exemption and forced-`here` transaction.
- Events `3607-3624`: after Krennic was lost and the objective was already flipped, Command Center
  pulls Krennic again. This is the unsafe re-pull that needs parent-stage formation handling because
  the forced destination produces no AI child prompt.
- The corresponding pre-fix decision log records V192 deploy-grade base `150`, then merged score
  `350` clearing the non-bucket floor (`CODEX_VERIFY_D92BC3A3C_2026-07-12.md`, original
  `logs/gemp-swccg.log:78083,78092-78093`). It proves that V192's local emission was only part of the
  route total.

These anchors protect the two different intended outcomes. The unflipped first pull may win; the
post-flip unsupported re-pull must retain its current formation penalty/veto behavior. A winner-only
fixture cannot distinguish a correct route from a score that happened to cross the same floor.

## Source-Grounded Objective Checks

- Endor pull claims were checked against the actual card Java filters for `Card8_167`, `Card8_124`,
  `Card207_025`, and `Card601_260`.
- Scarif's Krennic pull is specifically `Filters.Krennic` plus `Filters.here(self)` in
  `Card216_016`; broad title/category substitution is not equivalent.
- `ObjectiveHandler` paths and `pendingDeployCardIds` are not live PULL authority merely because
  similarly named state exists.

## Smallest Safe Seam

1. Build one immutable decision snapshot at `buildEvaluatorContext`, preserving ordinal, raw ids,
   raw array presence/length, selectable flags, and semantic subroute.
2. Add shared immutable `PullFacts` and pure `PullAssessment`. The assessment contains evidence and
   `ALLOW/DEFER/BLOCK`, never score magnitudes.
3. Keep thin compatibility contribution adapters in the existing parent, child, and destination
   slots. Initially preserve every magnitude, operation order, candidate order, and hard-veto bit.
4. Add one canonical transaction lifecycle at decision mediation for parent selection, engine
   failure observation, and success/reset.
5. Shadow old and new assessments. Remove one contribution owner at a time only after exact parity.

Moving V192 wholesale into `DeckOracle` or a phase controller is explicitly rejected. `DeckOracle`
is a facts service, not policy or transaction ownership.

## Required Fixtures

1. Endor successful pull and confirmed dead search.
2. Exact battleground subtype missing while a generic location remains in Reserve.
3. Scarif Krennic exemption, unresolved identity, weak solo/no-plan, and true hard-veto cases.
4. Deploy-from-pile child versus take-into-hand child with the same `ARBITRARY_CARDS` wire shape.
5. Destination selection with V136/V193 and the one-destination auto-select path.
6. Failed-search lifecycle: parent selected, engine failure, same-turn suppression, turn/reset,
   successful later search.
7. Full parent total containing V192 clamp, V67ak, formation, and `DeployEvaluator` contributions.
8. Exact-score ties preserving raw candidate ordinal and first insertion.

Run every fixture against both bots with real card blueprints and engine filters. The golden record
must include raw candidate order, ordered contribution operations with exact float bits, hard-veto
state, pre-safety winner, final response, and winning ordinal.

## Retirement Boundary

Do not yet delete absorbed V192 comments, disabled Endor fallbacks, constant-false pull branches,
dead handlers, unused caches, or current parent/child guards. Retirement requires shadow parity,
one-owner-at-a-time removal, and zero-live-caller proof. Comment-only cleanup outside these held
PULL blocks may continue independently.
