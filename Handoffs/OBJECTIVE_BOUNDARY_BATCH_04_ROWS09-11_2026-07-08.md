# Objective Boundary Batch 04: Rows 09-11

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 9 | `12_89` | THGSG | The Hyperdrive Generator's Gone / We'll Need A New One | None found in live Rando or Chosen One evaluators/analyzer. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Credits stacking, system pull despite front-side system deploy lock, and senator battleground loss modeling. |
| 10 | `13_46` | WLHT | We'll Handle This / Duel Of The Fates | None found in live Rando or Chosen One evaluators/analyzer. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Jedi/Dark Jedi present-at-Naboo logic and lightsaber combat schema. |
| 11 | `14_52` | WHAP | We Have A Plan / They Will Be Lost And Confused | None found for WHAP. Dead `ObjectiveHandler` has setup refs. Do not confuse with title-gated `isInvasion()`. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Theed setup, Amidala/Panaka site control, flip-back, and force-loss action schema. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`, `resources/AI_CHANGELOG.md`, `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`.

The search hits around Invasion/Naboo are not WHAP authority. `ObjectiveAnalyzer.isInvasion()` is title-derived from objectives containing "invasion"; WHAP does not. That trap is labeled now, because apparently Theed has banana peels.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `loaderEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `12_89` | null | empty | empty | empty | null | empty | empty | empty | present: Watto's Junkyard, City Outskirts, Coruscant/Tatooine systems, battleground site with senator |
| `13_46` | null | empty | empty | empty | null | empty | empty | empty | present: Theed Generator Core, Theed Generator, interior Naboo battleground, Naboo site with Jedi, interior Theed Palace site |
| `14_52` | null | empty | empty | empty | null | empty | empty | empty | present: Theed Palace Throne Room, Hallway, Courtyard, interior Naboo site |

This is behavior-neutral while disabled. It is not source-equivalent, because the current loader does not consume `objectiveNamedLocations` for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `12_89` | Front deploys `Filters.Wattos_Junkyard`, `Filters.City_Outskirts`, and `Filters.Credits_Will_Do_Fine`; once per game may take `Filters.Coruscant_system` and/or `Filters.Tatooine_system` into hand; front side separately prevents deploying any systems; flips when `Credits Will Do Fine` has 4 stacked cards. Back gives unique Republic characters power/forfeit bonuses, battle-destiny retrieval, Queen's Royal Starship activation, and control-phase opponent Force loss equal to battleground sites occupied with senators. No back-side flip-back trigger. | Captures Watto's Junkyard, City Outskirts, Credits Will Do Fine, Coruscant/Tatooine pull, stacked-card flip, Queen's Royal Starship action, senator battleground loss, and no flip-back. | Runtime consumed slots omit setup locations/cards, Credits stack requirement, pullable systems, front system deploy lock, Queen's Royal Starship action, senator battleground count, and back-side force-loss action. | Hold. Needs stack-count and "pull but cannot deploy now" semantics before useful enable. |
| `13_46` | Front deploys `Filters.Theed_Palace_Generator_Core`, `Filters.Theed_Palace_Generator`, and `Filters.Inner_Strength`; prevents opponent Force drains where opponent has a Dark Jedi; protects LS Naboo drains where LS has a Jedi; once during control may target an opponent character present with LS Jedi at an interior Theed Palace site and lose it, modified by legacy flags to narrower target classes; flips when opponent Dark Jedi is present at an interior Naboo battleground. Back prevents LS Force drains/battles where LS has a Jedi, once during move phase initiates lightsaber combat between LS Jedi and present opponent Dark Jedi, and flips back plus retrieves 1 when opponent has no Dark Jedi present at any interior Naboo battleground. | Captures Theed locations, Inner Strength, Jedi/Dark Jedi requirements, target-to-lose action, legacy target narrowing, lightsaber combat, and flip-back. | Runtime consumed slots omit setup cards, Jedi/Dark Jedi key filters, interior Naboo battleground relation, target-to-lose action, legacy caveats, lightsaber combat, and flip-back. | Hold. Needs relation-aware Jedi/Dark Jedi location schema, not a flat Naboo string. |
| `14_52` | Front deploys `Filters.Theed_Palace_Throne_Room`, `Filters.Theed_Palace_Hallway`, and `Filters.Theed_Palace_Courtyard`; restricts deployable card classes; suspends Your Destiny; protects from Vengeance Of The Dark Prince; front side forbids LS character deploys to interior Naboo sites; once per turn activates 1 Force; flips when LS controls Theed Palace Throne Room with `Filters.Amidala` there. Back limits drain loss to 2, buffs Republic starships, cancels Trade Federation starship immunity, may cancel opponent battle destiny > 3 using 1 Force, causes opponent Force loss during control phase for each battleground site controlled with Amidala or Panaka, and flips back if opponent controls Theed Palace Throne Room. | Captures Theed setup, Amidala Throne Room flip, Panaka/Amidala battleground loss, destiny cancel, flip-back, and deploy restriction caveats. | Runtime consumed slots omit setup locations, key character filters, Throne Room flip gate, force activation, control-phase force loss, battle-destiny cancel, and flip-back. | Hold. Needs key-character site-control schema and post-flip action schema before useful enable. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| THGSG | No live title-specific score found. | Empty consumed slots add nothing. | Neutral no-op. Filling Watto/Tatooine/Coruscant/senator slots later needs boundary math because the objective can pull systems but cannot deploy systems while front side up. |
| WLHT | No live title-specific score found. | Empty consumed slots add nothing. | Neutral no-op. Filling Naboo/Jedi/Dark Jedi slots later must be bounded against generic Jedi deploy and battle logic. |
| WHAP | No live WHAP-specific score found. The old Invasion V86/V121 family is title-gated to objectives containing "invasion", not WHAP. | Empty consumed slots add nothing. | Neutral no-op. Do not reuse Invasion weights unless K-2 deliberately proves the boundary. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `12_89` | `startingLocations` | `Filters.Wattos_Junkyard`, ids `12_178`, `12_87`; `Filters.City_Outskirts`, id `11_42` |
| `12_89` | `pullableCards` | `credits will do fine`; once-per-game `Filters.Coruscant_system` and/or `Filters.Tatooine_system` |
| `12_89` | new schema likely needed | `flip=Credits Will Do Fine has 4 stacked cards`; `frontMayNotDeploySystems=true`; `postFlipQueenRoyalStarshipActivation`; `postFlipSenatorBattlegroundLoss` |
| `13_46` | `startingLocations` | `Filters.Theed_Palace_Generator_Core`, `Filters.Theed_Palace_Generator` |
| `13_46` | `pullableCards` | `inner strength` |
| `13_46` | `keyCharacterFilter` | `Filters.Jedi`, `Filters.Dark_Jedi` |
| `13_46` | `locationFragments` | `theed palace generator core`, `theed palace generator`, `interior naboo battleground`, `interior theed palace site` |
| `13_46` | new schema likely needed | `flip=opponent Dark Jedi present at interior Naboo battleground`; `flipBack=no opponent Dark Jedi present at interior Naboo battleground`; `postFlipLightsaberCombat=true`; `legacyTargetNarrowing` |
| `14_52` | `startingLocations` | `Filters.Theed_Palace_Throne_Room`, `Filters.Theed_Palace_Hallway`, `Filters.Theed_Palace_Courtyard` |
| `14_52` | `keyCharacterFilter` | `Filters.Amidala`, `Filters.Panaka` |
| `14_52` | `flipGateSite` | `Filters.Theed_Palace_Throne_Room`, but the relation is `controlsWith(..., Amidala)`, not just control |
| `14_52` | new schema likely needed | `flip=control Throne Room with Amidala`; `flipBack=opponent controls Throne Room`; `postFlipBattlegroundLossWithAmidalaOrPanaka`; `postFlipCancelBattleDestinyGt3` |

## K-2 Implementation Notes

| Need | Why |
|---|---|
| Keep all three `loaderEnabled` absent/false for now. | Current consumed profiles are empty. Enabling them does not add source-grounded scoring. |
| Add relation types before enabling WLHT and WHAP. | Both depend on "present at" or "controls with" relations, not mere location relevance. |
| Treat THGSG system pull carefully. | The objective lets LS take Coruscant/Tatooine into hand while the front side forbids deploying systems. Scoring a pulled system as immediately deployable would be wrong. Steve dislikes wrong. A niche preference. |
| Do not map WHAP to Invasion by theme. | Existing Invasion scoring is title-gated. WHAP is a Theed Palace objective but not the Invasion objective. |
| Model post-flip actions separately from flip requirements. | THGSG, WLHT, and WHAP all have meaningful post-flip actions that are not just "stay flipped" data. |

## Source Files Read

| BP | Files |
|---|---|
| `12_89` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set12/light/Card12_089.java`, `Card12_089_BACK.java` |
| `13_46` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set13/light/Card13_046.java`, `Card13_046_BACK.java` |
| `14_52` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set14/light/Card14_052.java`, `Card14_052_BACK.java` |

## One-Line Verdict

Rows 09-11 are disabled no-ops today, but not safe as meaningful JSON-driven profiles. THGSG needs stacked-card and system-lock semantics, WLHT needs Jedi/Dark-Jedi relation schema, and WHAP needs Amidala/Panaka site-control logic that is not confused with Invasion.
