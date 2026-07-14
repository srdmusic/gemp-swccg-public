# Objective Boundary Batch 06: Rows 15-17

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 15 | `112_1` | AITC | Agents In The Court / No Love For The Empire | No live title-specific scoring found outside dead `ObjectiveHandler`. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs dynamic Rep species schema and battleground occupancy count rules. |
| 16 | `203_19` | DMTA | Diplomatic Mission To Alderaan / A Weakness Can Be Found | No live title-specific scoring found. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Stolen Data Tapes delivered state, Rebel-control relations, and Tatooine/Alderaan pull slots. |
| 17 | `204_32` | OA | Old Allies / We Need Your Help | No live title-specific scoring found. Dead `ObjectiveHandler` has Jakku setup refs only. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Jakku control/occupy split and post-flip prevention tools. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `rolloutEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `112_1` | false | empty | empty | empty | null | empty | empty | empty | present: Hutt Trade Route, Jabba's Palace site, battleground site, Tatooine location, Rancor Pit |
| `203_19` | false | empty | empty | empty | null | empty | empty | empty | present: Tatooine system, Dune Sea, Alderaan system, Tatooine battleground site, battleground site/system, Tantive IV, R2-D2, Stolen Data Tapes |
| `204_32` | false | empty | empty | empty | null | empty | empty | empty | present: Jakku system, Niima Outpost Shipyard, Jakku location, Jakku battleground site/system, Episode VII Falcon |

This is behavior-neutral while disabled. It is not source-equivalent, because the loader does not consume these named-location facts for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `112_1` | Front deploys `Filters.Hutt_Trade_Route`, a `Filters.Jabbas_Palace_site`, reveals a unique alien with species as Rep, and optionally deploys `Filters.Yarna_dal_Gargan`. It makes Rep a leader, protects Yarna from Alter, forbids insert cards and operatives, protects Trap Door while a rancor is at Rancor Pit, and lets stacked Rep copies deploy as if from hand. Front flips with two occupied battleground sites, but if a non-Tatooine location is on table it needs three occupied battleground sites and one must be occupied with a non-unique alien of Rep's species. Back can stack a Rep copy from hand to cancel a Force drain or battle destiny, retrieves a non-unique alien of Rep's species once during control, adds destiny to total power when two LS aliens battle at a Tatooine location, and flips back if LS does not occupy two battleground sites. | Captures setup, Rep selection, species-dependent flip, Rep-copy stack actions, species retrieval, and flip-back. | Runtime consumed slots omit starting refs, dynamic Rep, species filters, battleground count logic, post-flip stack/cancel, retrieval, Tatooine alien battle modifier, and flip-back. | Hold. Needs a dynamic chosen-card profile field, not a static named-card list. |
| `203_19` | Front deploys `Filters.Tatooine_system`, `Filters.Tantive_IV` to Tatooine, non-Reflections II `Filters.R2D2` to Tatooine, `Filters.Stolen_Data_Tapes` to Tatooine, and `Filters.Dune_Sea`. It forbids Sandwhirl, Strike Planning, Admiral's Orders, and Episode I Jedi. Front drains at Tatooine system are -2. Once per turn may deploy `Filters.or(Filters.Alderaan_system, Filters.Tatooine_site)` with the second filter `Filters.or(Filters.Alderaan_system, Filters.battleground)`, which means Alderaan or a Tatooine battleground site. Tantive IV may be forfeited before LS turn 1 to satisfy all battle damage. Front flips when Stolen Data Tapes are delivered and Rebels control a battleground site and a battleground system. Back gives total battle destiny +X for battlegrounds occupied by Rebels ability < 4, keeps the same location deploy action, retrieves 1 Force after winning battle, and flips back if LS lacks a battleground site, battleground system, or any controlled location. | Captures setup, pull action, delivered tapes flip, Rebel control relations, battle-damage shield, post-flip destiny/retrieval, and flip-back. | Runtime consumed slots omit all setup refs, delivered-data state, Rebel-control relations, pullable Alderaan/Tatooine locations, front deploy bans/drain penalty, Tantive IV battle-damage shield, post-flip destiny/retrieval, and flip-back. | Hold. Needs delivered-card state and two-relation flip requirements before useful enable. |
| `204_32` | Front deploys `Filters.Jakku_system`, `Filters.Niima_Outpost_Shipyard`, an Episode VII Falcon to Niima Outpost Shipyard, and optionally `Filters.Graveyard_Of_Giants`. It forbids Combined Fleet Action, Harc Seff, Luke, and Jedi, suspends Your Destiny, redirects opponent Reflections II objective to Rey, and suspends Visage Of The Emperor while Rey is at a battleground site. Once per turn may deploy `Filters.Jakku_location`. Front flips if LS controls Jakku system and occupies two Jakku battleground sites, or occupies Jakku system and controls two Jakku battleground sites. Back keeps the Jakku location deploy action, can reduce certain opponent-control-phase Force loss to 1 if LS controls two Jakku battlegrounds, can subtract 2 from destiny during battle with an LS Resistance character, gives Episode VII characters/starships with Han defense value +2, and flips back if LS does not occupy two battlegrounds. | Captures setup, Jakku pull, control/occupy alternatives, post-flip prevention tools, and flip-back. | Runtime consumed slots omit setup refs, Jakku location pull, control/occupy alternatives, front bans/suspensions, Force-loss reduction, destiny subtraction, Han-related defense bonus, and flip-back. | Hold. Needs relation-aware Jakku control/occupy schema and post-flip prevention slots. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| AITC | No live title-specific score found. Dead `ObjectiveHandler` lists Hutt Trade Route and Jabba's Palace site but is not consumed. | Empty consumed slots add nothing. | Neutral no-op today. Future enable must not pretend Rep species is static. |
| DMTA | No live title-specific score found. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs delivered-card and Rebel-control logic, not just Tatooine location preference. |
| OA | No live title-specific score found. Dead `ObjectiveHandler` lists Jakku and Niima Outpost Shipyard but is not consumed. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs Jakku relation alternatives plus prevention actions. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `112_1` | `startingLocations` | `Filters.Hutt_Trade_Route`, DB candidates `112_9`, `223_48`; `Filters.Jabbas_Palace_site`, broad runtime filter. |
| `112_1` | `startingEffects` | Optional `Filters.Yarna_dal_Gargan`, DB candidates `6_59`, `208_16`. |
| `112_1` | new schema likely needed | `chosenRep=unique alien with species`; `repCopiesCanDeployFromStack`; `repSpeciesNonUniqueAlien`; `flip=occupy 2 battleground sites, or 3 with one Rep-species alien if non-Tatooine location on table`; `flipBack=not occupy 2 battleground sites`; `postFlipRepCopyCancelDrainOrDestiny`; `postFlipRetrieveRepSpeciesAlien`. |
| `203_19` | `startingLocations` | `Filters.Tatooine_system`, DB candidates `1_127`, `1_289`, `12_84`, `12_175`, `203_33`; `Filters.Dune_Sea`, DB candidates `1_130`, `224_22`. |
| `203_19` | `startingCards` | `Filters.Tantive_IV`, DB candidates `2_73`, `201_19`; non-Reflections II `Filters.R2D2`; `Filters.Stolen_Data_Tapes`, DB candidate `203_14`. |
| `203_19` | `pullableCards` | `Filters.or(Filters.Alderaan_system, Filters.Tatooine_site)` constrained by `Filters.or(Filters.Alderaan_system, Filters.battleground)`, once per turn on both sides. Alderaan DB candidates `1_121`, `1_281`. |
| `203_19` | new schema likely needed | `delivered_Stolen_Data_Tapes`; `flip=Rebel controls battleground site and battleground system`; `flipBack=missing occupied battleground site or system or no controlled location`; `frontTatooineDrainMinus2`; `preTurn1TantiveSatisfyBattleDamage`; `postFlipBattleDestinyByLowAbilityRebelBattlegrounds`; `postFlipWinBattleRetrieve1`. |
| `204_32` | `startingLocations` | `Filters.Jakku_system`, DB candidates `204_26`, `204_51`; `Filters.Niima_Outpost_Shipyard`, DB candidate `204_27`. |
| `204_32` | `startingEffects` | Optional `Filters.Graveyard_Of_Giants`, DB candidate `204_14`. |
| `204_32` | `startingCards` | `Filters.and(Icon.EPISODE_VII, Filters.Falcon)` deployed landed to Niima Outpost Shipyard; canonical facts flag dual-faced candidate verification. |
| `204_32` | `pullableCards` | `Filters.Jakku_location`, once per turn on both sides. |
| `204_32` | new schema likely needed | `flip=control Jakku system plus occupy 2 Jakku battleground sites OR occupy Jakku system plus control 2 Jakku battleground sites`; `flipBack=not occupy 2 battlegrounds`; `frontBansAndSuspensions`; `postFlipForceLossReduceTo1ExceptJakkuDrain`; `postFlipDestinyMinus2WithResistanceBattle`; `postFlipEpisodeVIIWithHanDefensePlus2`. |

## Source Files Read

| BP | Files |
|---|---|
| `112_1` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set112/light/Card112_001.java`, `Card112_001_BACK.java` |
| `203_19` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set203/light/Card203_019.java`, `Card203_019_BACK.java` |
| `204_32` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set204/light/Card204_032.java`, `Card204_032_BACK.java` |

## One-Line Verdict

Rows 15-17 are disabled no-ops today, but not safe as meaningful JSON-driven profiles. AITC needs dynamic Rep species, DMTA needs delivered tapes plus Rebel-control relations, and OA needs Jakku control/occupy alternatives plus post-flip prevention actions.
