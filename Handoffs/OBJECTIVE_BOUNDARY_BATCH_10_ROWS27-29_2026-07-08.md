# Objective Boundary Batch 10: Rows 27-29

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 27 | `225_53` | MWYHL | Mind What You Have Learned / Save You It Can | No live title-specific scoring found outside dead `ObjectiveHandler`. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Dagobah/Bespin setup, Luke-on-Dagobah flip, Jedi Test/Lost Pile action, and completed-test state. |
| 28 | `226_28` | THP | The Hidden Path / Gather Allies And Train | Many live Hidden Path V-tags exist, including V52b, V53b, V60, V62, V67aa, and V67z. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs source fields plus ported V-tag weights. This is high-risk and should not be loader-enabled casually. Obviously. |
| 29 | `301_2` | CITC | City In The Clouds / You Truly Belong Here With Us | No live title-specific scoring found outside dead `ObjectiveHandler`. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Bespin/Cloud City setup, Cloud City control count, Bespin occupancy, and opponent-control exclusion. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `rolloutEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `225_53` | false | empty | empty | empty | null | empty | empty | empty | present: Beldon's Corridor, Yoda's Hut, Bespin system or Cloud City site, Dagobah location |
| `226_28` | false | empty | empty | empty | null | empty | empty | empty | present: Mapuzo setup sites, Jabiim location, non-Mapuzo site, Jabiim site or battleground site |
| `301_2` | false | empty | empty | empty | null | empty | empty | empty | present: Bespin system, Cloud City battleground site, Cloud City location, Cloud City sites |

This is behavior-neutral while disabled. It is not source-equivalent, because the loader does not consume these named-location facts for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `225_53` | Front deploys `Filters.Beldons_Corridor`, `Filters.Yodas_Hut`, `Filters.and(Icon.DAGOBAH, Filters.Yoda)` to Yoda's Hut, `Filters.and(Icon.CLOUD_CITY, Filters.No_Disintegrations)`, and `Filters.Patience`. It prevents Sense targeting characters at non-battlegrounds, prevents LS Force drains on Dagobah, makes non-Dagobah LS ability > 4 characters except Ahsoka lost, once per turn deploys Bespin system or Cloud City site, can deploy Wise Advice or Yoda's Hope without a once-per-turn gate in Java, once per turn deploys a Dagobah location, and flips during LS turn if Luke is on Dagobah. Back immediately returns Luke and cards on him to owner's hand, lets LS deploy Luke and weapon-on-Luke as react, places completed Jedi Test out of play to take a Cloud City Rebel from Lost Pile, keeps Bespin location deploy, and forces Cloud City Rebel battleground-site Force drain losses from Reserve unless a captive is on table. | Captures setup, Bespin/Dagobah pulls, Luke-on-Dagobah flip, no flip-back, Luke return, Jedi Test action, and Force-drain loss rule. | Runtime consumed slots omit setup, Yoda-at-Hut destination, No Disintegrations/Patience, Bespin/Cloud City pull, Dagobah pull, Wise Advice/Yoda's Hope pull, Luke-on-Dagobah flip, post-flip Luke return, Jedi Test cost action, Cloud City Rebel Lost Pile upload, and Reserve-loss condition. | Hold. Needs pull-chain and post-flip action schema before meaningful enable. |
| `226_28` | Front deploys `Filters.Mining_Village`, `Filters.Safehouse`, `Filters.Underground_Corridor`, and `Filters.Fallen_Order`. It bans generic locations, Anakin, non-Jedi-Survivor Jedi, and A Jedi's Resilience, modifies Weapon Levitation, prevents Nabrun Leids while front-side-up, gives Mapuzo Force drains -1, once per turn deploys `Filters.Jabiim_location`, once per turn deploys `Filters.holocron`, and flips when LS occupies two non-Mapuzo sites with Jedi, including excluded from battle. Back keeps the front restrictions, makes Jedi Survivors deploy -1, protects LS holocrons from leaving table by putting them in Used Pile, gives opponent total battle destiny -1 where they have ability > 4 character, relocates a Jedi during move phase between Jabiim site and battleground site for 2 Force, makes opponent lose 1 Force at end of opponent turn if Jedi occupy two battleground sites, deploys Jabiim locations, and flips back if LS does not occupy two non-Mapuzo sites with Jedi. | Captures setup, Fallen Order, Jabiim pull, holocron pull, exact non-Mapuzo/Jedi flip and flip-back filters, post-flip relocation, holocron protection, and V-tag mapping. | Runtime consumed slots omit setup, Jabiim pull, holocron pull, non-Mapuzo Jedi relation, INCLUDE_EXCLUDED_FROM_BATTLE nuance, deploy/play bans, Mapuzo drain penalty, post-flip relocation, holocron protection, battle destiny penalty, battleground-site end-turn loss, and every existing V-tag weight. | Hold. Needs careful consolidation of existing V52b/V53b/V60/V62/V67aa/V67z scoring. Empty JSON would erase the behavior Steve actually cares about. Probability of Steve enjoying that: 0%. |
| `301_2` | Front deploys `Filters.Bespin_system`, a `Filters.Cloud_City_site` that is a battleground, and optionally `Filters.Weather_Vane`. Once per turn, LS may use 1 Force to deploy a Cloud City battleground from Reserve. It flips if LS controls two Cloud City battleground sites, occupies Bespin system, and opponent controls no Cloud City sites. Back once per game deploys Cloud City Celebration, once during control uses 2 Force to take an Interrupt from Reserve, once per turn moves a character as react to opponent battle or Force drain at a Cloud City site, and flips back if opponent controls more Cloud City sites than LS. | Captures setup, optional Weather Vane, Cloud City battleground pull with 1 Force cost, flip count, Bespin occupancy, opponent-control exclusion, post-flip Cloud City Celebration, Interrupt upload, react move, and flip-back. | Runtime consumed slots omit setup, optional Weather Vane, Cloud City battleground pull/cost, Cloud City control count, Bespin occupancy, opponent-control exclusion, Cloud City Celebration, Interrupt upload, react move, and flip-back. | Hold. Needs count/occupy/control relation schema before meaningful enable. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| MWYHL V | Dead `ObjectiveHandler` has setup refs only. No live title-specific evaluator scoring found. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs Luke-on-Dagobah and post-flip Jedi Test/Cloud City Rebel semantics. |
| THP | Existing live scoring includes V52b deploy weights (+800 Jedi, +800 Fallen Order Jedi, +700 lightsaber, +600 holocron), V53b move weighting (+800 after previous +9999 was dominated), V60 Underground Corridor transit (+20000), V62 split-site selection (+200 and -500), V67aa suicide block (-9999), and V67z transit Force reserve (-1500). | Empty consumed slots add nothing and would not replace current evaluator behavior. | Hold. This is not safe until JSON has all source fields and K-2 ports existing V-tags with boundary math, not vibes. |
| CITC | Dead `ObjectiveHandler` has setup refs only. No live title-specific evaluator scoring found. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs Bespin/Cloud City control scoring and post-flip interrupt/celebration action scoring. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `225_53` | `startingLocations` | `Filters.Beldons_Corridor`, DB candidate `225_40`; `Filters.Yodas_Hut`, DB candidates `216_26`, `4_89`. |
| `225_53` | `startingEffects` | `Filters.and(Icon.CLOUD_CITY, Filters.No_Disintegrations)`, DB candidate `225_55`; `Filters.Patience`, DB candidate `225_57`; `Filters.and(Icon.DAGOBAH, Filters.Yoda)` to Yoda's Hut, runtime filter. |
| `225_53` | `pullableCards` | `Filters.or(Filters.Bespin_system, Filters.Cloud_City_site)`, once per turn; `Filters.or(Filters.Wise_Advice, Filters.Yodas_Hope)`, no once-per-turn gate in Java; `Filters.Dagobah_location`, once per turn. |
| `225_53` | new schema likely needed | `flip=Luke on Dagobah during LS turn`; `postFlipReturnLukeToHandOnFlip`; `postFlipCompletedJediTestCostUploadCloudCityRebelFromLostPile`; `postFlipCloudCityRebelBattlegroundDrainLostFromReserveUnlessCaptive`; `frontAbilityGreaterThan4LossExceptAhsoka`; `DagobahForceDrainBan`. |
| `226_28` | `startingLocations` | `Filters.Mining_Village`, DB candidate `226_21`; `Filters.Safehouse`, DB candidate `226_22`; `Filters.Underground_Corridor`, DB candidate `226_23`. |
| `226_28` | `startingEffects` | `Filters.Fallen_Order`, DB candidate `226_14`. |
| `226_28` | `pullableCards` | `Filters.Jabiim_location`, once per turn; `Filters.holocron`, once per turn on front. |
| `226_28` | `weights` | Port existing V52b, V53b, V60, V62, V67aa, and V67z numbers exactly first. Do not invent new magnitudes. |
| `226_28` | new schema likely needed | `flip=Jedi occupy two non-Mapuzo sites, include excluded from battle`; `flipBack=not Jedi occupy two non-Mapuzo sites`; `postFlipRelocateJediBetweenJabiimAndBattlegroundFor2Force`; `postFlipOpponentLoseIfJediOccupyTwoBattlegroundSites`; `postFlipHolocronToUsedPileInsteadOfLeaving`; `postFlipOpponentBattleDestinyMinus1WhereAbilityMoreThan4`; `frontMapuzoDrainMinus1`; `frontBans`. |
| `301_2` | `startingLocations` | `Filters.Bespin_system`, DB candidates `223_8`, `5_164`, `5_76`; `Filters.and(Filters.Cloud_City_site, Filters.battleground)`, runtime filter only. |
| `301_2` | `startingEffects` | Optional `Filters.Weather_Vane`, DB candidates `219_47`, `5_30`, `5_127`. |
| `301_2` | `pullableCards` | Front `Filters.Cloud_City_location` constrained by battleground, once per turn, costs 1 Force; back `Filters.Cloud_City_Celebration`, once per game; back `Filters.Interrupt`, once during control, costs 2 Force. |
| `301_2` | new schema likely needed | `flip=control two Cloud City battleground sites AND occupy Bespin system AND opponent controls no Cloud City sites`; `flipBack=opponent controls more Cloud City sites than LS`; `postFlipReactMoveToCloudCityBattleOrForceDrain`. |

## Source Files Read

| BP | Files |
|---|---|
| `225_53` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set225/light/Card225_053.java`, `Card225_053_BACK.java` |
| `226_28` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set226/light/Card226_028.java`, `Card226_028_BACK.java` |
| `301_2` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set301/light/Card301_002.java`, `Card301_002_BACK.java` |

## One-Line Verdict

Rows 27-29 are disabled no-ops today, but not safe as meaningful JSON-driven profiles. MWYHL needs Dagobah/Bespin and Jedi Test semantics, THP needs the existing V-tag behavior ported with boundary math, and CITC needs Bespin/Cloud City count and control relations.
