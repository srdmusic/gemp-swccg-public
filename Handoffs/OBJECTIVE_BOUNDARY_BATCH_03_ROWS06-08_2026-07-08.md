# Objective Boundary Batch 03: Rows 06-08

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 6 | `9_61` | TIGIH | There Is Good In Him / I Can Save Him | None found in live Rando or Chosen One evaluators/analyzer. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs captive Luke/Vader presence and crossover mechanics before meaningful enable. |
| 7 | `10_26` | WYS | Watch Your Step / This Place Can Be A Little Rough | None found in live Rando or Chosen One evaluators/analyzer. Generic smuggler/location scoring may still apply. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs smuggler battleground count, Kessel Run alternative, and Lost Pile interrupt support. |
| 8 | `12_88` | PMCTTS | Plead My Case To The Senate / Sanity And Compassion | My Lord senator weights are gated to My Lord, but V99 Senate Guard is deliberately ungated whenever Galactic Senate is on table. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs a PMCTTS Senate playbook separate from My Lord, plus boundary math against ungated V99. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`, `resources/AI_CHANGELOG.md`, `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`.

The PMCTTS search intentionally hits the My Lord Senate family. That is relevant because V99 is ungated and will still act at Galactic Senate, while V83/V88/V108/V110 are My Lord-only. Do not blindly reuse the dark-side My Lord playbook for the light-side Senate objective. That would be very efficient, in the way stepping on a rake is efficient.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `loaderEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `9_61` | null | empty | empty | empty | null | empty | empty | empty | present: Chief Chirpa's Hut, Endor: Landing Platform, Luke's site |
| `10_26` | null | empty | empty | empty | null | empty | empty | empty | present: Cantina, Docking Bay 94, Tatooine, Corellia, Kessel, battlegrounds |
| `12_88` | null | empty | empty | empty | null | empty | empty | empty | present: Galactic Senate, other Episode I location |

This is behavior-neutral while disabled. It is not source-equivalent, because the current loader does not consume `objectiveNamedLocations` for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `9_61` | Front deploys `Filters.Chief_Chirpas_Hut`, `[Death Star II]` `Filters.Luke`, `Filters.Lukes_Lightsaber`, `Filters.Landing_Platform`, and `Filters.I_Feel_The_Conflict`; prevents Alter, Strangle, and Captive Fury; Luke is captured by an Imperial at Luke's site; flips when Luke is captive. Back makes opponent lose 2 unless Vader escorts Luke, lets an Imperial transfer Luke to Vader, lets LS once per turn shuffle and draw destiny when Vader is present with Luke, crosses Vader if crossover total > 14, and flips back if Luke is neither captive nor present with Vader. | Captures Chief Chirpa's Hut, Landing Platform, DSII Luke, Luke's Lightsaber, I Feel The Conflict, Luke captive flip, Vader escort/presence, crossover > 14, and hard lose/deplete outcome. | Runtime consumed slots omit starting locations/cards, Luke/Vader character requirements, captive flip relation, flip-back relation, and crossover action. | Hold. Needs schema for captive state, present-with-Vader state, opponent-owned transfer action, and crossover destiny. |
| `10_26` | Front deploys Cantina, Docking Bay 94, and Tatooine system; if a Cantina exists and cannot be converted, a Reserve Cantina is placed out of play instead of deployed; once during each deploy phase takes Corellia or Kessel into hand; flips if LS occupies two battlegrounds with smugglers or has completed two Kessel Runs. Back gives smugglers/system and interrupt-from-Lost mechanics; flips back if LS does not occupy two battlegrounds unless two Kessel Runs are completed. | Captures Cantina special setup, Docking Bay 94, Tatooine, Corellia/Kessel pull, smuggler occupation, Kessel Run alternative, Lost Pile interrupt play, and flip-back. | Runtime consumed slots omit starting locations, pullable systems, smuggler requirements, Kessel Run alternative, flip-back, and Lost Pile interrupt action. | Hold. Needs count-aware smuggler battleground fields and an Utinni/Kessel Run alternative before useful enable. |
| `12_88` | Front deploys `Filters.Galactic_Senate` and another Episode I non-Senate location; boosts politics for low-ability Rebel/Imperial leaders; cancels Counter Assault and Surprise Assault; deploys cards stacked on Political Effects; at Galactic Senate weapon destinies are -6, creatures are lost, and non-Republic character game text is canceled; flips with 3 senators at Senate or 2 senators with at least one peace agenda. Back may use 3 Force during control to place up to two random opponent hand cards in Used Pile, once per turn takes a Political Effect from Reserve, boosts senator/order-agenda destiny, may use 2 Force at end of turn to place stacked Political Effect cards in Used Pile, and flips back if fewer than two senators at Galactic Senate. | Captures Galactic Senate, other Episode I location, senator/peace-agenda flip, Political Effect pulls/actions, random hand action, and flip-back. | Runtime consumed slots omit starting locations, senator key character, Galactic Senate key site, peace-agenda requirement, Political Effect pull, random hand action, and flip-back. | Hold. Needs PMCTTS-specific Senate playbook and boundary against existing V99, not My Lord copy-paste archaeology. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| TIGIH | No live title-specific score found. Generic text parsers may see Endor/Luke/Vader terms, but no TIGIH V-tag. | Empty consumed slots add nothing. | Neutral no-op. Filling source slots later needs behavior math because captive/crossover actions are not plain deploy scoring. |
| WYS | No live title-specific score found. Generic objective/location scoring may reward Tatooine or battlegrounds only if analyzer parses them from game text. | Empty consumed slots add nothing. | Neutral no-op. Filling Cantina/Tatooine/battleground/smuggler slots later needs count and Kessel Run boundary math. |
| PMCTTS | My Lord V83 -2000, V88 +1500, V108 +500, V110 -2000 are My Lord-gated. V99 Senate Guard is deliberately ungated and penalizes non-senators at Galactic Senate when Senate is on table. | Empty consumed slots add nothing. | Neutral no-op, except existing ungated V99 may already affect PMCTTS games once Galactic Senate is present. PMCTTS scoring must account for that instead of duplicating My Lord rules blindly. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `9_61` | `startingLocations` | `Filters.Chief_Chirpas_Hut`, ids `214_19`, `8_71`; `Filters.Landing_Platform`, ids `8_166`, `8_76` |
| `9_61` | `pullableCards` | DSII Luke via `Filters.and(Icon.DEATH_STAR_II, Filters.Luke)`, `luke's lightsaber`, `i feel the conflict`, plus setup locations |
| `9_61` | new schema likely needed | `flip=Luke captive`; `flipBack=Luke neither captive nor presentWithVader`; `keyCharacters=Luke,Vader,Imperial escort`; `crossoverDestinyThreshold=14` |
| `10_26` | `startingLocations` | `Filters.Cantina`, `Filters.Docking_Bay_94`, `Filters.Tatooine_system` |
| `10_26` | `pullableCards` | `Filters.Corellia_system`, `Filters.Kessel_system` |
| `10_26` | new schema likely needed | `flip=occupy 2 battlegrounds with smugglers OR completed 2 Kessel Runs`; `flipBack=not occupy 2 battlegrounds unless 2 Kessel Runs`; `postFlipLostPileInterrupt=true` |
| `12_88` | `startingLocations` | `Filters.Galactic_Senate`, plus `Filters.and(Icon.EPISODE_I, Filters.location, Filters.not(Filters.Galactic_Senate))` |
| `12_88` | `keyCharacterFilter` | `Filters.senator`, but remember Steve's lore rule: senator detection may need keyword OR lore for candidate prep, while runtime `Filters.senator` is source truth |
| `12_88` | `keySiteFilter` | `Filters.Galactic_Senate` |
| `12_88` | `pullableCards` | `Filters.Political_Effect` post-flip |
| `12_88` | new schema likely needed | `flip=3 senators at Senate OR 2 senators where at least one has peace agenda`; `flipBack=fewer than 2 senators at Senate`; `startingEffectLike=PoliticalEffect stacked-card deployment`; `postFlipRandomHandUsedPile` |

## K-2 Implementation Notes

| Need | Why |
|---|---|
| Keep all three `loaderEnabled` absent/false for now. | Current consumed profiles are empty. Enabling them is either a no-op or a future regression seed. |
| Do not promote `objectiveNamedLocations` alone. | TIGIH and WYS have action/state conditions that are not equivalent to location relevance. PMCTTS needs Senate-specific character logic. |
| PMCTTS should probably get its own light-side Senate playbook. | My Lord's Senate weights are dark-side blockade logic. PMCTTS is peace-agenda/senator logic. Shared primitives, different objective. Shocking, the Senate is complicated. |
| Boundary PMCTTS against V99 explicitly. | V99 is ungated by design. If PMCTTS adds a Senate playbook, non-senator penalties must not double-fire or block valid Political Effect/leader lines. |
| Treat WYS as a count-and-alternative objective. | "Two battlegrounds with smugglers" and "two Kessel Runs" are alternatives, not a flat location fragment. |
| Treat TIGIH as captive/crossover state work. | The objective is not about controlling Endor sites. It is about getting Luke captured, present with Vader, and making a crossover attempt. Parser strings will not save us. They rarely do. |

## Source Files Read

| BP | Files |
|---|---|
| `9_61` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set9/light/Card9_061.java`, `Card9_061_BACK.java` |
| `10_26` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set10/light/Card10_026.java`, `Card10_026_BACK.java` |
| `12_88` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set12/light/Card12_088.java`, `Card12_088_BACK.java` |

## One-Line Verdict

Rows 06-08 are disabled no-ops today, but not safe as meaningful JSON-driven profiles. TIGIH needs captive/crossover schema, WYS needs smuggler/Kessel Run alternatives, and PMCTTS needs a light-side Senate playbook that does not accidentally inherit My Lord's dark-side math.
