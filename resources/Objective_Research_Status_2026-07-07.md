# Objective Research Status

Generated: 2026-07-07

Purpose: durable checkpoint for the objective inventory and normalized playbook research. No Java or Rando code changes were made.

## Current Shape

| Item | Status |
|---|---|
| Human inventory | `resources/Objective_Blueprint_Inventory_2026-07-07.md` exists |
| Authoring JSON sidecar | `resources/Objective_Blueprint_Inventory_2026-07-07.json` exists |
| Normalization schema | `resources/Objective_Normalization_Schema_2026-07-07.md` exists |
| Claude bottom-up intel | `resources/Objective_Intel_BottomUp_rows29-39_2026-07-07.md` and `resources/Objective_Intel_BottomUp_rows40-57_2026-07-07.md` read |
| Mailbox sync | Alfred sent Claude row-28/top-half spot check as mailbox message `m00037` |
| Playbook facts JSON | `resources/Objective_Playbook_Facts_2026-07-07.json` started with pilot batch: `12_179` My Lord and `8_167` Endor Operations |

## Source Ownership

| Rows | Owner | Status | Notes |
|---:|---|---|---|
| 0-28 | Alfred/Codex | Source-read from actual front/back Java | Ready to fold into normalized `NamedCardRef`, `LocationRequirement`, `CharacterRequirement`, `PullOrDeployAction`, `FlipRequirement`, `HardLose` shape |
| 29-39 | Claude/K-2 bottom-up agents | Draft intel read by Alfred | Useful, but must still be verified against source before wiring |
| 40-57 | Claude/K-2 bottom-up agents | Draft intel read by Alfred | Useful, but rows flagged below needed source confirmation |

## Schema Rules Confirmed

| Rule | Decision |
|---|---|
| Runtime truth | Read each objective's Java filters and effects. Do not parse English as rules truth |
| Candidate lists | Store as inventory snapshots only, with provenance: `DB`, `SOURCE_SUPPLEMENT`, or `DB_PLUS_SOURCE_SUPPLEMENT` |
| Runtime matching | Use filter recipes, not frozen candidate ID lists |
| Selection vs destination | Keep target selection filters separate from deploy destination or attachment constraints |
| Flip-back | Reuse `FlipRequirement` vocabulary with comparator or negation, do not invent a second object shape |
| Dynamic state | Represent chosen objective state explicitly, e.g. Hidden Base hidden system, Local Uprising subjugated planet, AITC Rep species |
| Side-aware IDs | Named systems and sites can have Light and Dark IDs. Card refs must carry side |
| Dead code | `ObjectiveHandler.java` is stale/dead for this work. Do not trust it |

## Trait Detection

| Need | Rules Source |
|---|---|
| Rebel, Imperial, Republic, Alien | Card icon/type fields |
| Species | `species` on blueprint/source |
| Spy, smuggler, senator, leader, scout | Keyword/filter when present |
| Persona | Persona/filter/title constants |
| Lore fallback | Only when Java uses `Filters.loreContains(...)` or keyword coverage is known incomplete |
| Senator | `Filters.senator` is rules truth. Lore can widen candidate snapshots only |

## K-2 Caveats Resolved From Source

| Row | Objective | K-2 Caveat | Alfred Source Result |
|---:|---|---|---|
| 40 | `14_113` Invasion | Intel header implied `14_114` collision | Resolved. `14_113_BACK` is In Complete Control. `14_114` is Blockade Flagship starship |
| 43 | `110_6` Court Of The Vile Gangster | `filtersUsedInJava = see array` placeholder | Resolved from Java. Setup pulls Audience Chamber, Great Pit Of Carkoon, Dungeon. Front once per deploy pulls docking bay or Independent starship. Flip needs two captives, or captive ability >2, at Jabba's Palace sites. Back pulls Sarlacc, Rancor, or Rancor Pit and flips back if no captives at Tatooine sites and no opponent character at same site as Rancor |
| 45 | `112_15` My Kind Of Scum | `filtersUsedInJava = placeholder`; pull chain uncertain | Resolved from Java. Setup pulls Desert Heart and a Jabba's Palace site, chooses unique alien with species as Rep, optionally deploys Well Guarded. Flip requires two battleground sites, or if any non-Tatooine location exists, three battleground sites with one occupied by a non-unique alien of Rep's species. Back retrieves non-unique alien of Rep species and flips if not occupying two battleground sites |

## Top-Half Source Caveats

| Row | Objective | Caveat |
|---:|---|---|
| 1 | Hidden Base | Hidden planet is dynamic while-in-play data. Probe and hard-loss logic must not be flattened to a named location |
| 2 | Local Uprising | Subjugated planet is chosen at deploy. Matching operative uses matching-system data |
| 12 | Quiet Mining Colony | Selection filter and destination are separate: site or cloud sector, to Bespin |
| 15 | Agents In The Court | Rep species is dynamic. Non-unique alien requirement depends on chosen Rep species |
| 16 | Diplomatic Mission To Alderaan | Alderaan system or Tatooine battleground site is an intersection of selection and extra filter, not a plain title scan |
| 23 | Rescue The Princess (V) | Conditional Leia deploy ordering is suspicious because source checks Reserve for Detention Block Corridor after deploying it |
| 25 | Hunt For The Droid General | Clone Command Center destination is based on the system chosen by the Clone Army battleground location |
| 26 | The Empire Knows We're Here | Java deploys Main Power Generators even though text says 1st Marker. Java wins |
| 27 | Mind What You Have Learned (V) | Front flip is Luke on Dagobah. Back has no explicit flip-back |
| 28 | The Hidden Path | Jedi occupy two non-Mapuzo sites, not two generic battleground sites |

## Next Work

| Priority | Work |
|---:|---|
| 1 | K-2 verifies pilot batch in `resources/Objective_Playbook_Facts_2026-07-07.json`: `12_179` and `8_167` |
| 2 | K-2 implements ObjectiveAnalyzer-owned playbook pilot from verified facts |
| 3 | Alfred continues adding normalized JSON batches, about 10 rows at a time |
| 4 | Source-verify Claude rows 29-57 before marking them complete, starting with rows that have hard-lose or stale dead-code warnings |
| 5 | Produce final human MD summary plus machine JSON with resolved candidate snapshots and filter recipes for all 58 rows |

## Batch Log

| Batch | Front BPs | Status | Notes |
|---|---|---|---|
| Pilot 1 | `12_179`, `8_167` | JSON written, K-2 verification pending | My Lord and Endor Operations. Both high confidence. `Political Effect` and `piloted AT-ST` are represented with schema caveats because the current schema has no EffectRequirement or VehicleRequirement type |
