# Objective Boundary Batch 09: Rows 24-26

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 24 | `219_48` | ZH | Zero Hour / Liberation of Lothal | No live title-specific scoring found outside dead `ObjectiveHandler`. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Lothal location setup/pull, Rebel/Phoenix control alternatives, opponent-control exclusion, and post-flip Phoenix battleground count. |
| 25 | `221_67` | HFTDG | Hunt For The Droid General / He's A Coward | No live title-specific scoring found outside dead `ObjectiveHandler`. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Clone Army setup chain, Grievous Will Run And Hide attached-state flip, Grievous-alone exception, and post-flip Clone Army battleground X actions. |
| 26 | `222_27` | TEKWRH | The Empire Knows We're Here / Prepare For Ground Assault | No direct title-specific scoring found. Hoth V160 hits are dark Shield Will Be Down, not this Light Hoth objective. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Hoth setup/pull, opponent-occupies-your-Hoth flip, marker-site retrieve, and post-flip Used Pile peek X action. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `rolloutEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `219_48` | false | empty | empty | empty | null | empty | empty | empty | present: Lothal system, Lothal site, Lothal location, battleground occupied with Phoenix Squadron characters |
| `221_67` | false | empty | empty | empty | null | empty | empty | empty | present: Clone Army battleground, Clone Command Center at same planet |
| `222_27` | false | empty | empty | empty | null | empty | empty | empty | present: Hoth system, Main Power Generators, Echo Command Center or marker site, Hoth battleground marker site |

This is behavior-neutral while disabled. It is not source-equivalent, because the loader does not consume these named-location facts for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `219_48` | Front deploys `Filters.Lothal_system` and `Filters.Lothal_site` from Reserve. It cancels Menace Fades and Projection Of A Skywalker, increases deploy cost for Harc, Jedi except Ahsoka/Kanan, and LS Episode I/VII ability cards, gives Phoenix Squadron keyword to Chopper, Ezra, Hera, Kanan, Sabine, and Zeb, once per turn deploys a Lothal site, tracks Force-drain state, and flips if LS controls three Lothal locations with Rebels or occupies three Lothal locations with Phoenix Squadron characters while opponent controls no Lothal locations. Back keeps Lothal-site deploy, gives other battleground drains +1 after a battleground drain that turn, adds or subtracts X from battle or opponent weapon destiny where X is battlegrounds occupied with Phoenix Squadron characters, prevents battle destiny draw limits at Lothal system, and flips back if opponent controls more Lothal locations than LS. | Captures setup, Lothal-site pull, Rebel/Phoenix flip alternatives, opponent-control exclusion, flip-back, and Phoenix count post-flip destiny action. | Runtime consumed slots omit setup, Lothal pull, Rebel/Phoenix control alternatives, opponent-control exclusion, keyword grants, cancel effects, post-flip force-drain state, destiny X action, Lothal battle destiny rule, and flip-back. | Hold. Needs count/alternative semantics plus post-flip stateful drain/destiny scoring before meaningful enable. |
| `221_67` | Front deploys `Filters.and(Icon.CLONE_ARMY, Filters.battleground, Filters.location)`, then deploys `Filters.Clone_Command_Center` to that location's system, then deploys `Filters.Cloning_Cylinders` and `Filters.Grievous_Will_Run_And_Hide`. It bans non-Episode I ability-card deploys, suspends Your Destiny, redirects Reflections II objectives to Anakin, gives Jedi pilot skill, makes LS Episode I sites immune to No Escape, causes opponent loss at end of opponent turn if LS occupies more battlegrounds, and flips if Grievous Will Run And Hide is attached to the objective unless Grievous is truly alone at a battleground. Back keeps front modifiers, gives drains +1 where a clone is with a Jedi or Padawan, if LS initiated battle peeks at Reserve or Used top card, then if X > 1 retrieves a Clone Army card, then if X > 2 may move a clone to the battle location, where X is battlegrounds occupied with LS Clone Army cards. Back flips if Grievous Will Run And Hide is no longer attached or Grievous is truly alone at a battleground. | Captures setup chain, attached Grievous effect flip, Grievous-alone exception, end-turn battleground comparison, post-flip X actions, and flip-back. | Runtime consumed slots omit setup, same-planet Clone Command Center relation, attached-card state, Grievous-alone exception using INCLUDE_EXCLUDED_FROM_BATTLE, battleground comparison, drain relation, battle-init X actions, and front modifiers. | Hold. Needs attached-card and X-count action schemas. A flat required-card list will be wrong. |
| `222_27` | Front deploys `Filters.Hoth_system` and `Filters.Main_Power_Generators` from Reserve, even though game text says 1st Marker. It limits Hoth Energy Shield beyond the 1st Marker, prevents playing Ice Storm, systems, Special Edition Leia, and ability > 4 characters, makes Echo Base Garrison immune to Alter, once per turn deploys `Filters.or(Filters.Echo_Command_Center, Filters.marker_site)`, gives Force drains +1 at Hoth system, retrieves 1 Force during control if LS occupies two battleground marker sites, and flips if opponent occupies an LS Hoth location. Back keeps location deploy, cancels Hoth Sentry and Sunsdown, cancels LS Admiral's Orders and unique non-gunner/non-pilot/non-trooper character game text, prevents cards hit by LS artillery/vehicle weapons from satisfying attrition, if LS initiated battle peeks at top X Used Pile cards and takes one where X is Hoth battlegrounds LS occupies, and flips back if opponent does not occupy an LS Hoth location. | Captures Hoth setup, Main Power Generators Java truth, Echo/marker pull, marker retrieve, opponent-occupies-your-Hoth flip, post-flip Used Pile X action, and flip-back. | Runtime consumed slots omit setup, Echo/marker pull, Hoth system force drain, marker-site retrieve, opponent-occupation relation, post-flip X action, cancels, deploy/play restrictions, hit attrition prevention, and flip-back. | Hold. Needs relation-based Hoth schema and must not reuse dark Shield Will Be Down V160 scoring. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| ZH | Dead `ObjectiveHandler` has Lothal setup refs only. No live title-specific evaluator scoring found. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs Rebel/Phoenix count alternatives and opponent-control exclusion. |
| HFTDG | Dead `ObjectiveHandler` has Clone Army setup refs. No live title-specific evaluator scoring found. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs attached Grievous effect state and Clone Army X actions. |
| TEKWRH | No direct title-specific evaluator score found. Existing Hoth analyzer V160 is dark Shield Will Be Down and should stay separate. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs Light Hoth relation scoring isolated from dark Hoth invasion logic. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `219_48` | `startingLocations` | `Filters.Lothal_system`, DB candidates `219_38`, `219_10`; `Filters.Lothal_site`, runtime filter with DB site candidates `219_39` to `219_42` plus dark-side Lothal sites where legal. |
| `219_48` | `pullableCards` | `Filters.Lothal_site`, once per turn from Reserve on front and back. |
| `219_48` | new schema likely needed | `flip=control three Lothal locations with Rebels OR occupy three Lothal locations with Phoenix Squadron characters, and opponent controls no Lothal locations`; `flipBack=opponent controls more Lothal locations than LS`; `keywordGrant=Phoenix Squadron to Chopper/Ezra/Hera/Kanan/Sabine/Zeb`; `postFlipBattlegroundDrainAfterDrain`; `postFlipDestinyDeltaByPhoenixBattlegroundCount`; `cancelMenaceFadesProjection`. |
| `221_67` | `startingLocations` | `Filters.and(Icon.CLONE_ARMY, Filters.battleground, Filters.location)`, runtime filter only; then `Filters.Clone_Command_Center`, DB candidate `221_54`, to same planet. |
| `221_67` | `startingEffects` | `Filters.Cloning_Cylinders`, DB candidate `211_53`; `Filters.Grievous_Will_Run_And_Hide`, DB candidate `221_65`. |
| `221_67` | new schema likely needed | `flip=Grievous Will Run And Hide attached to objective unless Grievous truly alone at a battleground`; `flipBack=not attached OR Grievous truly alone at a battleground`; `frontOpponentLoseIfYouOccupyMoreBattlegrounds`; `postFlipX=count battlegrounds occupied with LS Clone Army cards`; `postFlipPeekReserveOrUsed`; `postFlipRetrieveCloneArmyIfXGreaterThan1`; `postFlipMoveCloneIfXGreaterThan2`; `frontDeployBans`; `YourDestinySuspended`. |
| `222_27` | `startingLocations` | `Filters.Hoth_system`, DB candidates `3_55`, `3_143`; `Filters.Main_Power_Generators`, DB candidates `210_15`, `222_9`, `3_61`. Java setup deploys Main Power Generators, not a generic 1st Marker string. |
| `222_27` | `pullableCards` | `Filters.or(Filters.Echo_Command_Center, Filters.marker_site)`, once per turn from Reserve on front and back. Echo Command Center DB candidates `3_57`, `3_145`; marker-site candidates include `3_56`, `3_61`, `3_62`, `3_63`, `3_144`, `3_148`, `3_149`, `208_49`, `217_12`, `222_22`, `222_9`, `223_38`. Runtime filter is truth. |
| `222_27` | new schema likely needed | `flip=opponent occupies your Hoth location`; `flipBack=opponent does not occupy your Hoth location`; `frontRetrieveIfOccupyTwoBattlegroundMarkerSites`; `frontHothSystemDrainPlus1`; `postFlipPeekUsedPileXByOccupiedHothBattlegroundCount`; `postFlipCancelHothSentrySunsdown`; `postFlipCancelOwnAOAndUniqueNonTrooperGunnerPilotText`; `hitByArtilleryVehicleWeaponAttritionPrevention`; `frontPlayBans`. |

## Source Files Read

| BP | Files |
|---|---|
| `219_48` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set219/light/Card219_048.java`, `Card219_048_BACK.java` |
| `221_67` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set221/light/Card221_067.java`, `Card221_067_BACK.java` |
| `222_27` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set222/light/Card222_027.java`, `Card222_027_BACK.java` |

## One-Line Verdict

Rows 24-26 are disabled no-ops today, but not safe as meaningful JSON-driven profiles. ZH needs Lothal count alternatives, HFTDG needs attached-card and Clone Army X semantics, and TEKWRH needs Light Hoth relation scoring isolated from dark Shield Will Be Down.
