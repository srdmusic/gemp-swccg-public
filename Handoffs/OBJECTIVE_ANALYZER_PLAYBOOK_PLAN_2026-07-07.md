# Objective Analyzer Playbook Plan

Date: 2026-07-07
Author: Alfred, after K-2 consultation
Status: Steve-approved direction, no Java edits by Alfred

## Ruling

Steve wants objective playbooks, categories, and weights to live under `ObjectiveAnalyzer`.

That is acceptable if we keep one hard boundary:

| Boundary | Rule |
|---|---|
| Ownership | `ObjectiveAnalyzer` owns objective profiles, requirements, and objective-specific weights |
| Application | Evaluators still apply score notes at the original call sites |
| Ordering | Do not move scoring across early returns, hard vetoes, or R-ladder bands |
| Rollback | Old inline branches stay commented out in place |

Translation: everything objective-specific lives in the analyzer, but the deploy, selection, move, and planner surfaces still decide when to ask the analyzer. This gives Steve the single objective brain without creating one global action hook that quietly breaks ordering.

## K-2 Consultation

K-2 replied through the async mailbox.

| Point | K-2 answer |
|---|---|
| Disclosure | K-2 already shipped deploy consolidation in commit `e8f1eaac3` |
| Current implementation | `ObjectiveAnalyzer.getDeployObjectiveAdjustments(...)` returns `List<ScoreNote>` |
| Deploy evaluator | Calls analyzer once at the old V83 objective region and applies notes there |
| Covered rules | V83, V110, V108, V86, V88, and ungated V99 |
| Safety fixes | Codex review caught V99 narrowing and flag-before-analyzed edge, both fixed |
| K-2 preference | External scorer is cleaner long-term, but current analyzer-owned version matches Steve's wording |
| Alfred ruling after Steve's follow-up | Keep analyzer-owned playbooks, but formalize them so `ObjectiveAnalyzer` does not become a junk drawer |

## Target Shape

`ObjectiveAnalyzer` should contain an explicit objective playbook model.

| Analyzer component | Purpose |
|---|---|
| `ObjectivePlaybook` | One active profile for the current objective |
| `ObjectiveWeights` | Per-objective and generic score constants |
| `ScoreNote` | Score plus reason string to apply at evaluator call site |
| Objective fact getters | Required cards, required sites, flip-gate site/card, objective-relevant fragments |
| Adjustment methods | Action-family methods that return score notes for one candidate |

Recommended internal structure:

```java
ObjectiveAnalyzer
  ObjectivePlaybook activePlaybook
  GenericObjectivePlaybook genericPlaybook

  static final class ObjectivePlaybook
    id
    objectiveTitleFragments
    objectiveBlueprintIds
    requiredSites
    requiredCards
    pullableCards
    protectedSites
    keyCharacters
    forbiddenCards
    flipGateSite
    flipGateCard
    ObjectiveWeights weights

  static final class ObjectiveWeights
    deployFlipGateSite
    pullRequiredLocation
    pullRequiredFlipCard
    deployObjectiveSite
    protectFlipBackSite
    holdWrongObjectiveDeploy

  static final class ScoreNote
    score
    reason
```

This can be nested in `ObjectiveAnalyzer` at first. If the file becomes too large, split into files in the same `strategy` package later, but keep ownership conceptually under the analyzer. Do not create a broad common scorer until there is a proven need.

## Generic Objective Playbook

Add a generic fallback profile that applies to objectives not yet hand-authored.

| Generic category | Behavior |
|---|---|
| Pull required locations | Score pulls/deploys for locations named by the objective parser |
| Pull required flip cards | Score cards in `requiredCardsOnTable` and `pullableCards` |
| Deploy objective sites | Bonus objective-relevant sites while pre-flip |
| Protect flip-back sites | Bonus required hold sites after flip |
| Flip-gate deploy | Bonus one body to the site that unlocks a required deploy card |

Generic weights should be conservative and must reuse existing magnitudes where possible. No fresh number just because the table has an empty cell. That is how arithmetic commits crimes.

## Specific Objective Profiles

Specific objectives override or add facts and weights.

| Objective | Analyzer profile owns | First scoring surface |
|---|---|---|
| Endor Operations | `Endor: Bunker`, `Establish Secret Base`, Endor site relevance, V193 one-shot gate | Deploy |
| My Lord | Galactic Senate, senator rules, non-senator hold, V83/V88/V108/V110 | Deploy, then CardSelection |
| Invasion | Neimoidian pilot to capital ship, V86/V121 | Deploy, then CardSelection |
| I Want That Map | Starkiller system ids, preferred starting effect, V186 | CardSelection |
| Hunt Down | Vader-required profile only where objective-gated | Deploy, Move, Battle later |
| TDIGWATT/Bespin | Bespin, Cloud City sites, Dark Deal, Executor exceptions | Separate project |
| Hidden Path | Mapuzo/Safehouse transit facts | Defer |
| On The Verge | Death Star, Scarif, parsec path, flip-back veto facts | Defer |

## Evaluator Contract

Evaluators call the analyzer from the same location where old logic fired.

| Surface | Allowed pattern | Forbidden pattern |
|---|---|---|
| Deploy | One analyzer call in the old objective deploy region, as `e8f1eaac3` does | Moving deploy scoring earlier or later |
| CardSelection | Per-handler analyzer calls at the exact old branch sites | One global CardSelection hook |
| Move | Per-ladder-rung or exact old branch calls only | One global move hook crossing R1-R4 claims |
| Battle | Exact old branch calls only | Hiding hard vetoes inside a broad analyzer call |
| Planner | Read analyzer playbook facts and weights, but do not duplicate evaluator scoring | Planner and evaluator both adding the same score |

## Sequencing

| Order | Work | Why |
|---|---|---|
| 0 | Confirm whether `e8f1eaac3` is deployed and live-tested | Do not stack refactors on unverified gameplay fixes |
| 1 | Accept `getDeployObjectiveAdjustments(...)` as the pilot, with Steve's analyzer-owned ruling | Already built and reviewed |
| 2 | Refactor the deploy method internally into playbook-shaped helpers only if needed | Keeps current behavior while making future objectives cleaner |
| 3 | Add the generic objective playbook for required locations and required flip cards | Highest leverage, lowest objective-specific risk |
| 4 | Move CardSelection objective logic one handler at a time | CardSelection has continues and router returns, so broad hooks are unsafe |
| 5 | Add I Want That Map as first CardSelection profile | Known objective facts, bounded surface |
| 6 | Add My Lord and Invasion CardSelection profiles | Completes rules already partly consolidated |
| 7 | Stop and report before Hunt Down, Verge, Hidden Path, or TDIGWATT | High dominance and hard-veto risk |

## Implementation Rules

| Rule | Requirement |
|---|---|
| Existing scores | No magnitude changes during consolidation |
| Old logic | Comment out old branches in place, never delete |
| V-tags | Keep V-tags searchable and adjust comments in place |
| Boundary math | Required before every moved branch |
| Cards | Read actual card source before any card-text claim |
| Rando and chosenone | Mirror same session |
| Changelogs | Update `resources/AI_CHANGELOG.md` and `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md` for Java changes |
| Build | Compile in-container and check the real `mvn` exit code |
| Deploy | Byte-verify jar markers, never deploy over a live game |

## Boundary Math Template

Each migrated branch gets this table in notes or commit message:

| Check | Required answer |
|---|---|
| Old predicate | Exact old condition |
| New predicate | Exact analyzer/playbook condition |
| Predicate parity | Why both fire on the same candidates |
| Old magnitude | Exact score |
| New magnitude | Same score |
| Neighboring rules | Early returns, hard vetoes, pass threshold, R-ladder band |
| Edge cases | No analyzed objective, non-matching objective, card absent, site already controlled, flipped vs unflipped |
| Proof | Log marker or TOPN evidence |

## Immediate Message To K-2

Steve likes the analyzer-owned version. Proceed with this interpretation:

| Instruction | Meaning |
|---|---|
| Keep objective profiles and weights inside `ObjectiveAnalyzer` | Do not migrate `getDeployObjectiveAdjustments(...)` to an external scorer now |
| Preserve evaluator call sites | The analyzer returns notes, evaluators apply them exactly where old branches fired |
| Formalize the analyzer internals | Make objective playbooks explicit so the analyzer remains organized |
| Generic objective profile is allowed | Add conservative generic weights for required locations and required flip cards |
| Do not rush high-risk objectives | TDIGWATT, Verge, Hidden Path, and Move ladder work wait |
