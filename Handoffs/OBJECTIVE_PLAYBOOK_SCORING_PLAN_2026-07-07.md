# Objective Playbook Scoring Plan

Date: 2026-07-07 12:27 PT
Author: Alfred, for Steve and K-2
Status: proposal, no Java edits made

## Decision

Do consolidate objective scoring, but do not put score-writing responsibility inside `ObjectiveAnalyzer`.

Steve is right about the problem: objective-specific strings and score logic are spread across deploy, selection, movement, and planner code. That creates spaghetti and makes old rules easy to silently dominate.

The safer architecture is:

| Layer | Owns | Does not own |
|---|---|---|
| `ObjectiveAnalyzer` | Objective identity, parsed objective facts, flip gates, required cards/sites/personas, typed getters | Action scoring, phase choice, score magnitudes |
| `DeckOracle` | Deck/zone/card availability, pull feasibility, duplicate counts, can-this-deck-satisfy facts | Objective parsing, score magnitudes |
| `ObjectivePlaybookScorer` | Objective-specific score constants and reusable scoring calculations | Raw card-text parsing, deck cataloging, action discovery |
| Evaluators/planners | Action legality context, candidate targets, existing score ordering, calling scorer at the same branch location | Hardcoded objective strings |

Short version: `ObjectiveAnalyzer` says what the objective wants. `DeckOracle` says whether we can satisfy it. `ObjectivePlaybookScorer` says how much this candidate action helps. Evaluators decide whether that candidate action is valid and where the score fits in the existing additive order.

If `ObjectiveAnalyzer` adds scores directly, it becomes a parser plus action evaluator plus phase planner. That is just spaghetti moved into a file with a more respectable name.

## Target Shape

Existing evaluators should call a shared scoring helper from the same place the old branch fired:

```java
ObjectiveAnalyzer objective = context.getObjectiveAnalyzer();
DeckOracle oracle = context.getDeckOracle();

score += ObjectivePlaybookScorer.scoreFlipGateDeploy(
    context,
    action,
    targetCard,
    targetSite,
    objective,
    oracle);
```

The call site stays in the existing evaluator position so early returns, hard vetoes, and R1-R4 ladder ordering remain unchanged.

The helper can expose action-family methods instead of one giant universal method:

| Method family | Used by |
|---|---|
| `scoreDeployAction(...)` | `DeployEvaluator`, deploy chooser surfaces |
| `scoreCharacterDeploySite(...)` | `CharacterDeploySiteEvaluator` / deploy site scoring paths |
| `scoreCardSelection(...)` | `CardSelectionEvaluator` |
| `scoreMoveDestination(...)` | `MoveEvaluator` |
| `scorePhasePlan(...)` | `DeployPhasePlanner` if it needs objective pressure facts |

Do not make one enormous `scoreEverythingObjectiveRelated(...)` method. That is how we build a junk drawer and call it architecture.

## First Implementation Pass

Goal: prove the pattern without changing behavior.

| Step | Work | Verification |
|---|---|---|
| 1 | Pick one already-general rule as the pilot. Recommended candidate: V193 flip-gate, because `ObjectiveAnalyzer` already owns `flipCriticalControlSite/Card`, and `DeployEvaluator` already has no Endor card names | Predicate parity: old branch fires exactly when helper returns +400 |
| 2 | Add `ObjectivePlaybookScorer` in the narrowest practical location. Prefer common only if package imports stay clean; otherwise mirror rando/chosenone first and consolidate later | Compile both bots |
| 3 | Replace the inline V193 score body with a helper call at the same call site | Old branch retained as commented rollback block with the same V-tag |
| 4 | Add pointer comments in both directions: evaluator call points to scorer/analyzer facts; scorer points to the old V-tag and consuming evaluator | Grep by `V193` and `ObjectivePlaybookScorer` |
| 5 | Run boundary math in the commit notes: helper returns 0 for non-matching objectives, +400 for the same one-shot flip-gate condition, and still closes once the site is controlled | TOPN/log marker comparison |

If K-2 refuses to touch V193 because the current plan says "do not touch except confirm," use the lowest-risk data-only branch next: Invasion V86/V121, then My Lord gate rename with V99 untouched. The same scorer architecture still applies, but the first proof is less clean.

## Migration Rules

| Rule | Reason |
|---|---|
| One objective per commit | Boundary math stays human-size |
| Keep call sites in place | Prevent early-return, hard-veto, and ladder-order behavior changes |
| Comment out replaced inline branch bodies, do not delete | Rollback path and project rule |
| Do not invent new magnitudes during consolidation | This is architecture, not balance tuning |
| Keep V-tags in place and adjust comments there | History stays searchable |
| Mirror rando to chosenone in the same session | Bot drift is already a known landmine |
| Update both changelogs only when Java behavior/structure changes | This plan file alone does not need Rando changelog entries |
| Read actual card source before any objective-specific text claim | Blueprint/virtual set mistakes already wasted enough oxygen |

## Boundary Math Template

Every migrated scoring branch must include this table in the working notes or commit message:

| Check | Required answer |
|---|---|
| Old predicate | Exact condition that used to add/penalize score |
| New predicate | Exact helper/analyzer/oracle condition |
| Predicate parity | Why old and new fire on the same candidates |
| Old magnitude | Exact score added or penalty applied |
| New magnitude | Same number, unless Steve explicitly approved retuning |
| Existing neighboring rules | Adjacent early returns, hard vetoes, pass threshold, R-ladder band |
| Edge cases | Non-objective deck, flipped/unflipped objective, card absent from deck, card in hand vs reserve, site already controlled |
| Log proof | V-tag marker or TOPN line to grep |

## What K-2 Should Not Do

| Do not | Why |
|---|---|
| Move all scoring branches into `ObjectiveAnalyzer` | Parser becomes action evaluator, phase planner, and score engine |
| Move all objective scoring into one giant deploy block | Reorders branches and can cross early returns or hard vetoes |
| Gate currently ungated flavor rules without Steve approval | Example: Vader/Luke flavor in non-Hunt Down decks may intentionally fire |
| Back-fill chosenone casually while doing a scoring refactor | That is a separate blast radius |
| Combine TDIGWATT/Bespin with the pilot | Too many branches, -9999 blocks, and un-gated deck-identity rules |

## Recommended Queue

| Order | Target | Why |
|---|---|---|
| 0 | Confirm deployed V193/Endor fixes in logs first | Current shipped fixes are still pending live confirmation |
| 1 | Scorer pilot with V193, if K-2 accepts touching it | Best existing template, null for every non-Endor objective |
| 2 | Invasion V86/V121 | Low-risk objective identity gate |
| 3 | My Lord gate rename, V99 comment-only | Medium risk, but useful consolidation |
| 4 | I Want That Map after chosenone back-fill | Chosenone currently lacks V186 |
| 5 | Hunt Down selected gated branches only | Leave ungated Vader/Luke flavor hardcoded unless Steve approves retune |
| 6 | TDIGWATT/Bespin as its own project | Largest spaghetti nest, naturally, because Bespin |

## Request To K-2

Please reply with:

| Question | Needed answer |
|---|---|
| Architecture | Do you agree with Analyzer facts + Oracle feasibility + Scorer weights + Evaluator call sites? |
| Class location | Should `ObjectivePlaybookScorer` live under `common/strategy`, mirrored `rando/chosenone/strategy`, or somewhere else? |
| Pilot | V193 helper extraction, Invasion, or another first target? |
| Boundaries | Any branch you consider too risky to move into a scorer helper even with call-site order preserved? |
