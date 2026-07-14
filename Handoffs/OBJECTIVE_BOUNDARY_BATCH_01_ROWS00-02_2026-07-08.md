# Objective Boundary Batch 01: Rows 00-02

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled low-risk location/count objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 0 | `7_135` | DBO | Dantooine Base Operations | None found outside generic parser/evaluators | Empty dormant profile | Safe only as no-op. Needs profile-fill before useful loader enable. |
| 1 | `7_136` | HB | Hidden Base | None found outside generic parser/evaluators | Empty dormant profile | Hold. Stateful flip tracker/probe logic needs explicit profile fields before useful enable. |
| 2 | `7_137` | LU | Local Uprising | None found outside generic parser/evaluators | Empty dormant profile | Hold. Runtime-selected Subjugated planet must be represented before useful enable. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando`, `resources/AI_CHANGELOG.md`, `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`.

Only hit was `ObjectiveType.java` using Hidden Base as an archetype comment example. No title-specific V-tag or magnitude found for these three.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `loaderEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights |
|---|---:|---|---|---|---|---|---|---|
| `7_135` | null | empty | empty | empty | null | empty | empty | empty |
| `7_136` | null | empty | empty | empty | null | empty | empty | empty |
| `7_137` | null | empty | empty | empty | null | empty | empty | empty |

This is neutral because loader is disabled and slots are empty. It is not a source-equivalent profile.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `7_135` | Front deploys `Filters.Dantooine_system`; once per deploy phase deploys `Filters.or(Filters.site, Filters.and(Filters.non_unique, Filters.Rebel))` to `Title.Dantooine`; flips when Rebels control 3 `Filters.Dantooine_site` and opponent controls no `Filters.Dantooine_location`; OOP if Dantooine blown away. Back flips if opponent controls 2 Dantooine locations, or 3 under `LEGACY__MORE_DANGEROUS_THAN_YOU_REALIZE__REQUIRES_THREE_SITES_TO_FLIP_BACK`. | Descriptive JSON captures Dantooine system, Dantooine site/location, non-unique Rebel pull, squadron back-side facts, and legacy flip-back count caveat. | Runtime profile has none of the starting location, pull action, location fragments, flip, flip-back, or OOP facts. | Do not treat as complete. If enabled now it is a no-op. Before useful enable, add Dantooine system/site/location fields and legacy flip-back count caveat. |
| `7_136` | Front deploys `Filters.Rendezvous_Point`, stacks outside-deck `Filters.and(Filters.planet_system, Filters.planetSystemInParsecRange(1, 8))`, sets while-in-play data, once per deploy phase deploys `Filters.system`, tracks deployed system titles, and exposes top-level Flip only after tracker clears. Back forbids Light deploying systems, cancels opponent drains up to battleground system count limit 2, lets Dark probe occupied systems, and OOPs if Hidden Base system is probed. | Descriptive JSON captures Rendezvous Point, Hidden Base indicator, deployable system, battleground-system tracker, probe Droid notes, and says flip is stateful tracker, not direct trigger. | Runtime profile has no startingLocations and cannot represent while-in-play data tracker, probe, or OOP facts. | Hold for schema. Needs `statefulFlipTracker`, `probeLoseCondition`, and `mayNotDeployAfterFlip` style fields. |
| `7_137` | Front deploys `Filters.planet_system`, stores `gameState.setSubjugatedPlanet(systemName)`, deploys one `Filters.and(Filters.generic, Filters.site)` to that system, then once per deploy phase deploys another generic site to the stored planet. Flips when matching operatives control 3 battleground sites related to Subjugated planet. Back retrieves 1 on matching operative deploy, buffs matching operatives, adds battle destiny, and flips back if not occupying 2 battleground Subjugated-planet sites. | Descriptive JSON captures Subjugated planet, generic site, matching operative, runtime selected planet, flip, and flip-back. | Runtime profile has no way to hydrate runtime-selected planet state, matching operative requirement, or generic-site pull chain. | Hold for schema. Needs `runtimeSelectedPlanet`, `relatedGenericSite`, `matchingOperative`, and count-based stay-flipped fields. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| DBO | No title-specific V-tag score. Generic parser may still infer locations from game text. | Empty profile adds nothing. | Neutral no-op. Filling `locationFragments` later would add generic objective-location scoring and must be boundary-mathed. |
| HB | No title-specific V-tag score. Generic parser may not understand the while-in-play tracker. | Empty profile adds nothing. | Neutral no-op. Needs schema before meaningful scoring. |
| LU | No title-specific V-tag score. Generic parser may not understand selected Subjugated planet state. | Empty profile adds nothing. | Neutral no-op. Needs schema before meaningful scoring. |

## K-2 Implementation Notes

| Need | Why |
|---|---|
| Keep all three `loaderEnabled` absent/false for now. | Enabling current empty profiles is behavior-neutral but does not advance Steve's "JSON is the data source" goal. |
| Add source-derived slots before enabling. | These objectives are location/count driven, but the runtime resource currently dropped the useful data from `resources/Objective_Playbook_Facts_2026-07-08.json`. |
| Do not hydrate broad pullables yet. | DBO and LU pull broad runtime filters. Pull hydration is already deferred because it can change behavior. Correct. |
| Add schema for stateful objectives before HB/LU. | Hidden Base depends on while-in-play data and probing. Local Uprising depends on `gameState.getSubjugatedPlanet()`. A static title fragment is not enough. |

## Source Files Read

| BP | Files |
|---|---|
| `7_135` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/light/Card7_135.java`, `Card7_135_BACK.java` |
| `7_136` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/light/Card7_136.java`, `Card7_136_BACK.java` |
| `7_137` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set7/light/Card7_137.java`, `Card7_137_BACK.java` |

## One-Line Verdict

Rows 00-02 are safe only in the boring sense: current runtime profiles are empty and disabled, so they cannot change behavior. They are not safe to flip as meaningful JSON-driven playbooks until K-2 restores the source-derived facts into runtime fields and adds stateful schema for Hidden Base and Local Uprising.
