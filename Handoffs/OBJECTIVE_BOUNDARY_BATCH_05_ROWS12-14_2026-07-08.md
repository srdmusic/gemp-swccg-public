# Objective Boundary Batch 05: Rows 12-14

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 12 | `109_4` | QMC | Quiet Mining Colony / Independent Operation | No QMC-specific live score found. Several Bespin/Cloud City generic rules can fire through `needsBespinSystemPresence()`, including V24.15, V29, V47, and V51. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs QMC-specific Bespin/Cloud City slots and a side-aware split from TDIGWATT's Executor/Bespin guard logic. |
| 13 | `110_4` | YCEPBT | You Can Either Profit By This... / Or Be Destroyed | No Profit-specific live score found. Generic Jabba's Palace text-named-site logic can overlap, especially V88. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs frozen-captive Han setup, rescue state, and post-flip force-loss action schema. |
| 14 | `111_4` | MBO | Massassi Base Operations / One In A Million | No live MBO-specific score found. Dead `ObjectiveHandler` has Yavin 4 setup refs, but it is not a source. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Yavin 4 control-count slots and post-flip Death Star / Attack Run package semantics. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`, `resources/AI_CHANGELOG.md`, `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`.

The QMC trap is obvious after reading source: it is a Light Cloud City objective, but the old Bespin generic rules are mostly TDIGWATT-shaped. If K-2 blindly feeds QMC into those rules, Rando may wait for Executor like it is cosplaying Dark Deal. That is not strategy. That is a spreadsheet with delusions.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `rolloutEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `109_4` | false | empty | empty | empty | null | empty | empty | empty | present: `Filters.Bespin_system`, `Filters.Cloud_City_battleground_site`, `Filters.Bespin_Cloud_City`, `Filters.Cloud_City_site`, `Filters.Bespin_cloud_sector`, `Filters.docking_bay` |
| `110_4` | false | empty | empty | empty | null | empty | empty | empty | present: `Filters.Jabbas_Palace`, `Filters.Audience_Chamber`, `Filters.Tatooine_location`, `Filters.Jabbas_Palace_site`, `Filters.exterior_Tatooine_site` |
| `111_4` | false | empty | empty | empty | null | empty | empty | empty | present: `Filters.Yavin_4_system`, `Filters.Yavin_4_Docking_Bay`, `Filters.Yavin_4_site`, `Filters.Yavin_4_sector`, `Filters.Death_Star_site`, `Filters.battleground_system`, plus unlabeled Death Star system candidates `2_143`, `7_117`, `216_7` |

This is behavior-neutral while disabled. It is not source-equivalent, because the current loader does not consume `objectiveNamedLocations` for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `109_4` | Front deploys `Filters.Bespin_system` and `Filters.Cloud_City_battleground_site`. Once per deploy phase, may use 1 Force to deploy `Filters.or(Filters.site, Filters.cloud_sector)` to Bespin from Reserve Deck. Front limits opponent Force loss from LS Bespin drains to 1, flips when opponent controls no Bespin locations, LS controls `Filters.Bespin_Cloud_City`, and LS controls either two Cloud City sites or one Cloud City site with Lando/Lobot on Cloud City. Back deploys `Filters.docking_bay` once per deploy phase, buffs aliens/cloud cars/Independent starships, protects drains at Bespin locations controlled with an alien, and flips back if opponent controls Bespin system or three Cloud City sites/Bespin cloud sectors. Both sides place out of play if Bespin is blown away. | Captures setup deploys, front site/sector deploy, back docking bay deploy, flip, flip-back, and Bespin blown-away hard lose. | Runtime consumed slots omit setup locations, site/sector deploy action, docking bay deploy action, Lando/Lobot reduced site-count alternative, flip-back, drain cap/protection, and hard lose. | Hold. Needs QMC-specific profile fill and a Bespin-rule split so TDIGWATT Executor logic does not contaminate QMC. |
| `110_4` | Front deploys `Filters.Jabbas_Palace`, `Filters.Audience_Chamber`, and usually `Filters.Han` as unattended frozen captive at Audience Chamber unless the objective text is modified. Opponent may deploy 0-2 aliens to Audience Chamber. Front forbids opponent drains at Audience Chamber, forbids LS drains at Tatooine locations, forbids LS playing Frozen Assets, gives Luke/C-3PO/R2-D2 deploy -2 to Jabba's Palace sites, and Master Luke deploys free there. Front flips when Han is on Tatooine and not captive. Back retrieves 5 Force on flip, or 10 if Han power < 4, cancels Bad Feeling Have I, lets unpiloted starfighters deploy landed to exterior Tatooine sites, causes opponent Force loss for each battleground occupied by Han/Luke/Leia/Chewie/Lando, and flips back when Han cannot be spotted as not captive. Both sides place out of play if Tatooine is blown away. | Captures setup deploys, frozen Han, opponent optional alien setup, flip, flip-back, retrieval, control-phase force loss, Tatooine hard lose, and modifier caveats. | Runtime consumed slots omit fixed setup, frozen-captive relation, opponent optional setup, Han rescue state, key-character battleground count, deploy-cost modifiers, drain restrictions, force retrieval, force-loss modifiers, and hard lose. | Hold. Needs captive-state and key-character occupancy schema before meaningful enable. |
| `111_4` | Front deploys `Filters.Yavin_4_system` and `Filters.Yavin_4_Docking_Bay`; for remainder of game, forbids Revolution and LS drains at Yavin 4 sites/sectors. While front side up, LS generates no more than 1 Force from each Yavin 4 site, Imperials deploy +2 to Yavin 4, and LS may deploy one `Filters.Yavin_4_site` from Reserve Deck once per deploy phase. Front flips when LS controls three Yavin 4 sites and opponent controls fewer than three Yavin 4 sites. Front places out of play if Yavin 4 is blown away. Back lets LS deploy Death Star system without Death Star Plans, once per deploy phase may take `Filters.or(Filters.Rebel_Tech, Filters.Death_Star_system, Filters.Attack_Run, Filters.Proton_Torpedoes)` into hand, gives LS total power +3 in battles at systems, adds 3 to Death Star blown-away Force loss per opponent Death Star site, and after Death Star is blown away gives LS drains +2 at battleground systems with a piloted starfighter present. No back-side flip-back trigger in Java. | Captures Yavin setup, Yavin site deploy, Yavin control-count flip, back take-into-hand package, Death Star modifiers, no flip-back, and front-only Yavin hard lose. | Runtime consumed slots omit fixed setup, Yavin site deploy, Yavin 4 control-count flip, Revolution/drain restrictions, force-generation cap, Imperial deploy penalty, back take-into-hand package, Death Star Plans bypass, Death Star blown-away modifiers, and front-only hard lose. | Hold. Needs count-based Yavin slots plus post-flip action package before useful enable. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| QMC | No title-specific QMC rule. Existing Bespin generic rules include V24.15 Bespin deploy +800 on turn 1 or +400 on turns 2-3, V29 non-location/non-ship character deploy gate -500 until Bespin space is occupied, V51 Cloud City army +500 pre-flip, V51 objective-first +300, V47 Lando/Lobot hard solo blocks -9999, and Lando stay-lock hard vetoes. | Empty consumed slots add nothing. If K-2 later fills Bespin/Cloud City slots without splitting logic, QMC may inherit TDIGWATT assumptions, especially Executor. | Neutral no-op today. Future enable is not safe until Bespin rules are objective/side aware. QMC wants Cloud City sites and aliens, not Executor theater. |
| YCEPBT | No title-specific Profit rule. Generic V88 text-named site bonus is +500 when character text/lore names the candidate site. Generic objective-first location scoring can also apply once relevant locations are represented. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs captive-state and Han-rescue semantics; do not reduce it to "Jabba's Palace location good." |
| MBO | No live title-specific MBO rule. Dead `ObjectiveHandler` has `111_4` Yavin 4 system and Yavin 4: Docking Bay, but it is not consumed. Generic objective-first location scoring may apply once Yavin 4 fragments are represented. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs count-based Yavin control and back-side Death Star package modeling. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `109_4` | `startingLocations` | `Filters.Bespin_system`, DB candidates include `5_76` Light Bespin and `5_164` Dark Bespin plus virtual `223_8` Dark Bespin; `Filters.Cloud_City_battleground_site`, candidate set must stay filter-driven and side-aware. |
| `109_4` | `pullableCards` | Front deploy action: `Filters.or(Filters.site, Filters.cloud_sector)` to Bespin from Reserve Deck, cost 1 Force, once per deploy phase. Back deploy action: `Filters.docking_bay` from Reserve Deck, once per deploy phase. |
| `109_4` | `flipRequirements` | Opponent controls 0 Bespin locations; LS controls `Filters.Bespin_Cloud_City`; LS controls two Cloud City sites, or one Cloud City site plus Lando/Lobot on Cloud City. |
| `109_4` | `flipBackRequirements` | Opponent controls `Filters.Bespin_system`, or opponent controls three total `Filters.or(Filters.Cloud_City_site, Filters.Bespin_cloud_sector)`. |
| `109_4` | new schema likely needed | `hardLose=Bespin blown away`; `bespinDrainLossCap=1 while front`; `postFlipDrainProtection=control Bespin location with alien`; `postFlipIndependentDeployCost=-1`; `bespinRuleFamily=QMC`, not TDIGWATT. |
| `110_4` | `startingLocations` | `Filters.Jabbas_Palace`, DB candidates `7_131` Light and `6_171` Dark; `Filters.Audience_Chamber`, DB candidates `6_81` Light and `6_162` Dark. |
| `110_4` | new setup refs | `startingCaptive=Filters.Han at Filters.Audience_Chamber as unattended frozen captive`, unless `YOU_CAN_EITHER_PROFIT_BY_THIS__DO_NOT_DEPLOY_HAN_AT_START_OF_GAME`; `opponentOptionalDeploy=0-2 Filters.alien to Audience Chamber`. |
| `110_4` | `flipRequirements` | `Filters.Han` on Tatooine and not captive. |
| `110_4` | `flipBackRequirements` | Cannot spot `Filters.and(Filters.Han, Filters.not(Filters.captive))`. |
| `110_4` | `keyCharacterFilter` | `Filters.or(Filters.Han, Filters.Luke, Filters.Leia, Filters.Chewie, Filters.Lando)` for back-side battleground Force loss. Add Luke/C-3PO/R2-D2/Master Luke deploy-cost facts separately. |
| `110_4` | new schema likely needed | `hardLose=Tatooine blown away`; `forbiddenPlay=Frozen Assets`; `frontDrainRestrictions`; `onFlipRetrieve=5 or 10 if Han power <4`; `postFlipBadFeelingHaveICancel`; `postFlipUnpilotedStarfighterLandedDeploy`; `postFlipForceLossModifierCaveats`. |
| `111_4` | `startingLocations` | `Filters.Yavin_4_system`, DB candidates include `1_135` Light, `1_296` Dark, `211_32` Light virtual; `Filters.Yavin_4_Docking_Bay`, DB candidates `1_136` Light and `1_297` Dark. |
| `111_4` | `pullableCards` | Front deploy action: `Filters.Yavin_4_site` from Reserve Deck, once per deploy phase. Back take-into-hand action: `Filters.or(Filters.Rebel_Tech, Filters.Death_Star_system, Filters.Attack_Run, Filters.Proton_Torpedoes)`. |
| `111_4` | `pullableCards` candidate ids | `Rebel Tech`: `2_19`; `Death Star system`: `7_117` Light, `2_143` Dark, `216_7` Dark virtual; `Attack Run`: `2_42`; `Proton Torpedoes`: `1_158`, `14_66`. |
| `111_4` | `flipRequirements` | LS controls at least three `Filters.Yavin_4_site`; opponent controls fewer than three `Filters.Yavin_4_site`. |
| `111_4` | `flipBackRequirements` | None in `Card111_004_BACK.java`. |
| `111_4` | new schema likely needed | `hardLose=Yavin 4 blown away front only`; `frontMayNotPlayRevolution`; `frontNoYavinDrains`; `frontYavinForceGenerationCap=1`; `frontImperialDeployPlus2ToYavin4`; `postFlipDeathStarPlansBypass`; `postFlipSystemBattlePowerPlus3`; `postDeathStarBlownAwayForceLossPlus3PerOpponentDeathStarSite`; `postDeathStarBlownAwayBattlegroundSystemDrainPlus2WithPilotedStarfighter`. |

## K-2 Implementation Notes

| Need | Why |
|---|---|
| Keep all three `rolloutEnabled=false` for now. | Current consumed profiles are empty. Enabling them gives no source-grounded scoring. |
| Split QMC from TDIGWATT before filling Bespin weights. | `needsBespinSystemPresence()` is too blunt. It would catch QMC and can drag in Executor-first assumptions that do not belong. |
| Add captive-state setup before Profit. | Profit is not just a location objective. It starts with frozen Han and flips on rescue state. |
| Add count-comparison requirements for MBO. | MBO flips on LS Yavin 4 site count versus opponent Yavin 4 site count, not a single gate site. |
| Keep broad filters as filters, not fake exact IDs. | Cloud City battleground site, Yavin 4 site, Han persona, and Death Star system all have side/virtual/card-pool variance. Runtime `Filter` truth beats a pretty but wrong list. As usual, disappointment is cheaper in Markdown than in Java. |

## Source Files Read

| BP | Files |
|---|---|
| `109_4` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set109/light/Card109_004.java`, `Card109_004_BACK.java` |
| `110_4` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set110/light/Card110_004.java`, `Card110_004_BACK.java` |
| `111_4` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set111/light/Card111_004.java`, `Card111_004_BACK.java` |

## One-Line Verdict

Rows 12-14 are disabled no-ops today, but not safe as meaningful JSON-driven profiles. QMC needs a QMC-specific Bespin family split, Profit needs frozen-Han rescue state, and MBO needs Yavin count plus post-flip Death Star package semantics.
