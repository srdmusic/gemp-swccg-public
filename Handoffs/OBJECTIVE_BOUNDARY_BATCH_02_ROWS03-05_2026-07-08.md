# Objective Boundary Batch 02: Rows 03-05

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled low-risk Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 3 | `7_138` | MWYHL | Mind What You Have Learned / Save You It Can | None found in live Rando or Chosen One evaluators/analyzer. Dead `ObjectiveHandler` has `225_53` virtual setup only. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Jedi Test completion, retargeted Leia branch, Dagobah hard-lose, and source-derived pull slots before useful enable. |
| 4 | `7_139` | RTP | Rescue The Princess / Sometimes I Amaze Even Myself | None found in live Rando or Chosen One evaluators/analyzer. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs captive setup, Leia movement/capture, hard-lose suppression, and back-side source oddity review before useful enable. |
| 5 | `8_78` | RST | Rebel Strike Team / Garrison Destroyed | No RST-specific live scoring found. Endor V193 hits are Endor Operations only, not this objective. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Rebel scout count logic, Bunker blown-away alternative, Endor flip-back, and pull slots before useful enable. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`, `resources/AI_CHANGELOG.md`, `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`.

The only related non-live hit was dead `ObjectiveHandler.java` for virtual MWYHL `225_53`. Do not source from it. The other hits were generic Endor/Scout infrastructure or Endor Operations V193, not Rebel Strike Team.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `loaderEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `7_138` | null | empty | empty | empty | null | empty | empty | empty | present: Dagobah system/location |
| `7_139` | null | empty | empty | empty | null | empty | empty | empty | present: Detention Block Corridor, DB 327, Yavin 4 DB, War Room, Death Star location |
| `8_78` | null | empty | empty | empty | null | empty | empty | empty | present: Endor system, Rebel Landing Site, exterior Endor sites, Bunker |

This is behavior-neutral while disabled. It is not source-equivalent, because the current loader does not consume `objectiveNamedLocations` for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `7_138` | Front deploys `Filters.Dagobah_system`; once during deploy phase deploys `Luke`, `Daughter_Of_Skywalker`, `Yoda`, `Yodas_Hope`, `At_Peace`, or `Lukes_Backpack` to `Title.Dagobah` from Reserve; `Daughter_Of_Skywalker` retargets the objective to Leia; flips when `Filters.Jedi_Test_5` is completed by Luke or retargeted Leia. Back retrieves 10 on flip, may use 3 Force to take Luke/Leia into hand, and places itself out of play if own Force drain at `Filters.Dagobah_location` or apprentice is placed out of play. | Captures Dagobah system/location, the pull chain, Jedi Test #5 flip, retargeted Leia branch, and hard-lose. | Runtime consumed slots omit starting location, pullables, required Jedi Test #5, flip relation, retargeted apprentice, and hard-lose. | Do not enable as meaningful profile. Needs schema or explicit fields for `JEDI_TEST_COMPLETED_BY`, retargeted Leia, and hard-lose. |
| `7_139` | Front deploys `Filters.Detention_Block_Corridor`, imprisoned captive `Filters.and(Filters.Leia, Filters.abilityLessThan(4))`, `Filters.Docking_Bay_327`, `Filters.Yavin_4_Docking_Bay`, and `Filters.Yavin_4_War_Room`; restricts spies/8D8/Revolution/Death Star Plans from Death Star, blocks Detention Block Control Room and Nabrun Leids; flips when Leia moves to `Filters.Yavin_4_War_Room`; places itself out of play if Leia is lost, unless `RESCUE_THE_PRINCESS__CANNOT_BE_PLACED_OUT_OF_PLAY` applies. Back flips if Leia is captured and has a copied Local Uprising-style battle destiny action using Subjugated planet filters. | Captures starting locations, captive Leia, move-to-War-Room flip, capture flip-back, hard-lose suppression, and flags the back-side copied action for K-2 review. | Runtime consumed slots omit all starting deploys, captive setup, Leia movement/capture, hard-lose, and restrictions. | Hold. Needs source-side confirmation on the copied back-side action before enabling anything beyond no-op location relevance. |
| `8_78` | Front deploys `Filters.Endor_system` and `Filters.Rebel_Landing_Site`; once per deploy phase may use 2 Force to take `Filters.Bunker` or `Filters.Deactivate_The_Shield_Generator` into hand; flips if Bunker blown away or during own move phase controls 3 `Filters.exterior_Endor_site` where each has two `Filters.Rebel_scout`; OOP if Endor system blown away. Back retrieves `Filters.and(Filters.Rebel_scout, Filters.abilityLessThan(3))`; flips back during opponent move phase if Bunker not blown away and opponent controls `Filters.Endor_system` or 3 exterior Endor sites; OOP if Endor system blown away. | Captures Endor system, Rebel Landing Site, Bunker, Deactivate, exterior Endor sites, Rebel scout requirement, flip alternatives, flip-back, and hard-lose. | Runtime consumed slots omit starting locations, pullables, Rebel scout count, flip alternatives, flip-back, and hard-lose. | Hold. Needs count-aware `LocationRequirement` and `CharacterRequirement` hydration before this can drive scoring. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| MWYHL | No live title-specific V-tag score. Dead `ObjectiveHandler` only lists virtual `225_53` setup cards and is explicitly non-authoritative. | Empty consumed slots add nothing. | Neutral no-op. Filling Dagobah/Jedi Test slots later would add objective-location or pull behavior and needs boundary math. |
| RTP | No live title-specific V-tag score. | Empty consumed slots add nothing. | Neutral no-op. Captive setup and hard-lose fields are prerequisite, otherwise scoring would be blind to the actual objective. |
| RST | No RST-specific live score. Endor Operations V193 uses Endor/Bunker terms but is gated by Endor Operations identity. | Empty consumed slots add nothing. | Neutral no-op. Filling Endor/exterior/Rebel scout slots would likely activate generic objective-location scoring and must be bounded. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `7_138` | `startingLocations` | `Filters.Dagobah_system`, ids `4_84`, `7_279`, `217_33` |
| `7_138` | `locationFragments` | `dagobah`, `dagobah system`, `dagobah location` |
| `7_138` | `requiredCardsOnTable` | `jedi test #5`, but only if backed by a `JEDI_TEST_COMPLETED_BY` relation, not a dumb title check |
| `7_138` | `pullableCards` | `luke`, `daughter of skywalker`, `yoda`, `yoda's hope`, `at peace`, `luke's backpack`, plus `dagobah system` for setup |
| `7_138` | new schema likely needed | `flip.relation=JEDI_TEST_COMPLETED_BY`, `apprentice=Luke|LeiaIfRetargeted`, `hardLose=DagobahDrainOrApprenticeOOP` |
| `7_139` | `startingLocations` | `Detention_Block_Corridor`, `Docking_Bay_327`, `Yavin_4_Docking_Bay`, `Yavin_4_War_Room` |
| `7_139` | new schema likely needed | `startingCaptive=Leia ability<4 at Detention Block Corridor`, `flip=Leia moved to War Room`, `flipBack=Leia captured`, `hardLose=LeiaLostUnlessSuppressed` |
| `8_78` | `startingLocations` | `Endor_system`, `Rebel_Landing_Site` |
| `8_78` | `locationFragments` | `endor`, `endor system`, `endor location`, `exterior endor sites`, `rebel landing site`, `bunker` |
| `8_78` | `pullableCards` | `bunker`, `deactivate the shield generator` |
| `8_78` | new schema likely needed | `flip.alternatives=BunkerBlownAway OR control 3 exterior Endor sites with 2 Rebel scouts at each`; `flipBack=opponentControlsEndorSystemOr3ExteriorEndorSitesUnlessBunkerBlownAway`; `hardLose=EndorSystemBlownAway` |

## K-2 Implementation Notes

| Need | Why |
|---|---|
| Keep all three `loaderEnabled` absent/false for now. | Current consumed profiles are empty. Enabling them does not advance Steve's JSON-driven scoring goal. |
| Do not promote `objectiveNamedLocations` blindly into consumed `locationFragments`. | It would create generic scoring without pull, flip, and hard-lose awareness. That is how silent regressions breed. Very inspiring, for mold. |
| Treat MWYHL as schema work, not just location work. | The flip condition is Jedi Test completion by a retargetable apprentice, not control of a site. |
| Treat RTP as schema and source-review work. | Captive setup and Leia movement/capture are the actual mechanics, and the back-side copied action needs a human ruling before migration. |
| Treat RST as count-aware objective work. | "Three exterior Endor sites with two Rebel scouts at each" cannot be represented by a flat string fragment without losing the real condition. |

## Source Files Read

| BP | Files |
|---|---|
| `7_138` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/light/Card7_138.java`, `Card7_138_BACK.java` |
| `7_139` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/light/Card7_139.java`, `Card7_139_BACK.java` |
| `8_78` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set8/light/Card8_078.java`, `Card8_078_BACK.java` |

## One-Line Verdict

Rows 03-05 are currently safe only as disabled no-ops. They are not safe to flip as meaningful JSON-driven playbooks until K-2 hydrates source-derived consumed slots and adds schema for Jedi Tests, captive Leia, hard-lose, and Rebel scout count logic.
