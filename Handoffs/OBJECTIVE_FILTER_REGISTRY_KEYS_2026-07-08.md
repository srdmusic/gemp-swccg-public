# Objective Filter Registry Keys, 2026-07-08

Purpose: map the first JSON rule keys from `Handoffs/OBJECTIVE_LOADER_EXTENSION_SCHEMA_2026-07-08.md` to runtime `Filters.*` expressions before K-2 wires the fail-closed registry.

Verification basis:

- Source checked: `src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/filters/Filters.java`
- Schema source: `Handoffs/OBJECTIVE_LOADER_EXTENSION_SCHEMA_2026-07-08.md`
- Rule: direct constants are safe registry entries. Composite entries need one small resolver branch. Dynamic and state entries must not be guessed as static card filters.

## Location Keys

| registryKey | status | Filters expression | Notes |
|---|---|---|---|
| `Endor_location` | DIRECT | `Filters.Endor_location` | Exact constant exists. |
| `Bunker` | DIRECT | `Filters.Bunker` | Exact title filter exists. |
| `Bespin_system` | DIRECT | `Filters.Bespin_system` | Exact system constant exists. |
| `Cloud_City_site` | DIRECT | `Filters.Cloud_City_site` | Exact site constant exists. |
| `Cloud_City_battleground_site` | DIRECT | `Filters.Cloud_City_battleground_site` | Exact battleground-site constant exists. |
| `Tatooine_location` | DIRECT | `Filters.Tatooine_location` | Exact location constant exists. |
| `Alderaan_location` | COMPOSITE | `Filters.partOfSystem(Title.Alderaan)` | `Alderaan_site` and `Alderaan_system` exist, but no exact `Alderaan_location` constant was found. |
| `Yavin_4_location` | DIRECT | `Filters.Yavin_4_location` | Exact location constant exists. |
| `Hoth_location` | DIRECT | `Filters.Hoth_location` | Exact location constant exists. |
| `your_Hoth_location` | CONTEXT_COMPOSITE | `Filters.and(Filters.your(playerId), Filters.Hoth_location)` | Requires player ownership context. Do not register as a static zero-arg filter. |
| `Theed_Palace_Throne_Room` | DIRECT | `Filters.Theed_Palace_Throne_Room` | Exact title filter exists. |
| `Naboo_system` | DIRECT | `Filters.Naboo_system` | Exact system constant exists. |
| `Galactic_Senate` | DIRECT | `Filters.Galactic_Senate` | Exact title filter exists. |
| `Ahch_To_location` | ALIAS | `Filters.AhchTo_location` | Schema spelling has underscore after Ahch. Runtime constant is `AhchTo_location`. Register alias or rename JSON keys consistently. |
| `Rebel_Base_location` | DIRECT | `Filters.Rebel_Base_location` | Runtime definition is Yavin 4 or Hoth system parts. |
| `Wattos_Junkyard` | DIRECT | `Filters.Wattos_Junkyard` | Exact title filter exists. |
| `interior_Naboo_battleground_site` | COMPOSITE | `Filters.and(Filters.interior_Naboo_site, Filters.battleground_site)` | `interior_Naboo_site` and `battleground_site` exist. No exact combined constant was found. |

## Actor Keys

| registryKey | status | Filters expression | Notes |
|---|---|---|---|
| `smuggler` | DIRECT | `Filters.smuggler` | Exact keyword filter exists. |
| `senator` | DIRECT | `Filters.senator` | Exact keyword filter exists. Prefer this over lore text search. Lore search can overmatch. |
| `agenda_character` | COMPOSITE | `Filters.and(CardCategory.CHARACTER, Filters.or(Filters.ambition_agenda, Filters.blockade_agenda, Filters.justice_agenda, Filters.order_agenda, Filters.peace_agenda, Filters.rebellion_agenda, Filters.taxation_agenda, Filters.trade_agenda, Filters.wealth_agenda))` | Agenda filters are runtime modifier-aware. Use all agenda constants, not title or lore text. |
| `Dark_Jedi` | DIRECT | `Filters.Dark_Jedi` | Exact runtime definition is dark side character with ability at least 6. |
| `Jedi` | DIRECT | `Filters.Jedi` | Exact runtime definition is light side character with ability at least 6. |
| `Luke` | DIRECT | `Filters.Luke` | Exact persona filter exists. |
| `Amidala` | DIRECT | `Filters.Amidala` | Exact persona filter exists. |
| `Panaka` | DIRECT | `Filters.Panaka` | Exact persona filter exists. |
| `Rebel` | DIRECT | `Filters.Rebel` | Exact icon filter exists. |
| `Resistance_character` | DIRECT | `Filters.Resistance_character` | Exact character plus Resistance icon filter exists. |
| `First_Order_character` | DIRECT | `Filters.First_Order_character` | Exact character plus First Order icon filter exists. |
| `ISB_agent` | DIRECT | `Filters.ISB_agent` | Exact keyword filter exists. |
| `Xizor` | DIRECT | `Filters.Xizor` | Exact title filter exists. |
| `Shada` | COMPOSITE | `Filters.title("Shada")` | No `Filters.Shada` constant was found. This matches existing Agents Of Black Sun legacy handling in card source. |
| `Watto` | DIRECT | `Filters.Watto` | Exact title filter exists. |
| `presence_droid` | CONTEXT_STATE | `Filters.droid` plus location-presence check | No static `hasPresence` filter exists. Evaluate droid plus actual presence at the objective location using the evaluator/game context. |
| `unique_snub_fighter` | COMPOSITE | `Filters.and(Filters.unique, Filters.snub_fighter)` | Both component constants exist. |

## State And Dynamic Keys

| registryKey | status | Filters expression | Notes |
|---|---|---|---|
| `Bunker_blownAway` | STATE_COMPOSITE | `Filters.and(Filters.Bunker, Filters.blown_away)` | Static filter can identify blown-away Bunker, but evaluator still needs objective-specific state semantics. |
| `Kessel_Run_complete` | STATE | `GameConditions.hasCompletedUtinniEffect(game, playerId, Filters.Kessel_Run)` | Existing card source uses this condition. Do not model as a card-presence filter. |
| `Stolen_Data_Tapes_delivered` | DIRECT_STATE_FILTER | `Filters.delivered_Stolen_Data_Tapes` | Exact filter exists, but it represents delivered-state, not generic on-table presence. |
| `BB8_or_map_state` | STATE | existing I Will Finish What You Started BB-8 or map logic | Existing V186-style logic should remain the source. The registry should call that helper, not invent a filter. |
| `dynamic_subjugated_planet` | DYNAMIC | `Filters.Subjugated_planet_location` plus `Filters.matchingOperativeToSubjugatedPlanet` | Runtime depends on `gameState.getSubjugatedPlanet()`. |
| `dynamic_renegade_planet` | DYNAMIC | `Filters.Renegade_planet_location` plus `Filters.matchingOperativeToRenegadePlanet` | Runtime depends on `gameState.getRenegadePlanet()`. |
| `dynamic_rep_species` | DYNAMIC | `Filters.species(rep.getBlueprint().getSpecies())` plus objective Rep lookup | Requires the chosen Rep card. Cannot be a static registry key. |

## Wiring Notes

| Topic | Recommendation |
|---|---|
| Unknown key behavior | Fail closed: log the key, return no score, do not guess. |
| Registry shape | Use separate resolver paths for `location`, `actor`, `state`, and `dynamic`. One giant map will lie to you. Shocking development. |
| Player context | Keys like `your_Hoth_location` and actor ownership constraints need player/source context in the resolver signature. |
| Dynamic objectives | Subjugated planet, Renegade planet, Rep species, and presence-droid checks must stay dynamic. Static snapshots will silently score the wrong locations. |
| Alias cleanup | Decide whether JSON keeps `Ahch_To_location` as a human-readable key or changes to `AhchTo_location`. The registry must accept whatever the JSON actually emits. |

## Current Pass Verdict

Use the direct keys immediately. Add composite keys with explicit branches. Treat dynamic and state keys as separate evaluator hooks. If any unknown key appears in JSON after this table, the correct score is zero plus a warning, not "close enough." Probability a guessed fallback causes a bad Rando action: offensive.
