# Objective Boundary Batch 11, Rows 30-32

Generated: 2026-07-08 by Codex Alfred

Scope: disabled runtime profiles only. No Java edits. Source evidence read from the actual card classes named below, plus the canonical data file `resources/Objective_Playbook_Facts_2026-07-08.json`.

## Summary

| Row | Side | Front BP | Objective | Runtime consumed slots | Verdict |
|---:|---|---|---|---|---|
| 30 | DARK | `7_296` | `CCT` Carbon Chamber Testing | Empty consumed slots; named locations only | HOLD. Needs captive-state, frozen-captive, setup-captive, and hard-lose slots. |
| 31 | DARK | `7_297` | `HDADTJ` Hunt Down And Destroy The Jedi | Empty consumed slots; named locations only | HOLD. Existing Hunt Down weights and hard-veto rules must be ported as a high-risk objective. |
| 32 | DARK | `7_298` | `IO` Imperial Occupation | Empty consumed slots; named locations only | HOLD. Needs dynamic Renegade planet plus matching-operative count slots. |

## Runtime JSON Hydrated Slots

| Front BP | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | Enabled |
|---|---|---|---|---|---|---|
| `7_296` | Empty | Empty | Empty | Empty | Empty | `rolloutEnabled=false` |
| `7_297` | Empty | Empty | Empty | Empty | Empty | `rolloutEnabled=false` |
| `7_298` | Empty | Empty | Empty | Empty | Empty | `rolloutEnabled=false` |

## Source Audit

| Front BP | Source-derived data | Existing Rando scoring boundary | Required extension before enable |
|---|---|---|---|
| `7_296` | Front deploys Carbonite Chamber, Carbonite Chamber Console, Security Tower, then either Jabba's Prize from own Reserve or an opponent-chosen Rebel to Security Tower as captive. Front pulls Audience Chamber, Docking Bay 94, or East Platform once per deploy phase. Flip is moving an escorting frozen captive to Audience Chamber, or no Rebel imprisoned at setup. Back retrieves 1 Force, can deploy Scum And Villainy while a frozen captive is at Audience Chamber, and places the objective out of play if no frozen captive exists unless the setup no-Rebel flag is set. | Only indirect `V24.13` Cloud City pull-package handling exists: Carbonite Chamber +150 and Security Tower -30 for I'm Sorry. No CCT-specific live objective playbook outside dead `ObjectiveHandler`. | Add `captiveSetup`, `captiveState`, `objectiveFlag`, `hardLose`, and post-flip `pullOrDeployActions` consumed slots. Do not enable from current empty profile. |
| `7_297` | Front deploys Holotheatre and Visage, may deploy Meditation Chamber and/or Epic Duel, may use 4 Force to take Vader from a controlled location into hand. Flip when Vader is at a battleground site and no opponent Luke or Jedi is at a battleground site. Both sides place the objective out of play if the player plays Scanning Crew, initiates a non-Epic duel, or Force drains at an Executor site. Back flips if opponent Luke/Jedi is at a battleground site or Vader is not on table. | Live Hunt Down logic exists in `ObjectiveAnalyzer`, `DeployEvaluator`, `MoveEvaluator`, and card selection: Vader priority, battleground/Vader move steer, and no-Vader flip-back awareness. Current JSON profile consumes none of it. | Add `keyCharacter`, `keyLocationRule`, `hardVetoActions`, and classic-vs-virtual Hunt Down split. Port existing V25/V29.12/V35.1/V40/V51/V137b magnitudes exactly. Hold until high-risk branch is verified. |
| `7_298` | Setup chooses any planet system as the Renegade planet and deploys one generic site to that system. Front pulls one generic site to the Renegade planet once per deploy phase. Flip when matching operatives control at least three battleground sites related to the Renegade planet. Back flips if the player does not occupy at least two Renegade planet battleground sites. | No dedicated live objective scoring found outside dead `ObjectiveHandler`. Generic location/control heuristics may help but do not know the dynamic planet or matching-operative requirement. | Add `dynamicPlanet`, `siteToDynamicPlanet`, `controlWithCount`, and `occupyCountFlipBack` slots keyed to `Filters.Renegade_planet_location` and `Filters.matchingOperativeToRenegadePlanet`. |

## Source Files Read

| Objective | Files |
|---|---|
| `7_296` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/dark/Card7_296.java`; `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/dark/Card7_296_BACK.java` |
| `7_297` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/dark/Card7_297.java`; `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/dark/Card7_297_BACK.java` |
| `7_298` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/dark/Card7_298.java`; `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/dark/Card7_298_BACK.java` |

## One-Line Verdict

Rows 30-32 are all data-only. They should stay disabled until the loader gains shared extension slots, especially captive-state, hard-veto, and dynamic-planet count rules.
