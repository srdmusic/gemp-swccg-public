# Objective Loader Extension Schema

Generated: 2026-07-08 by Codex Alfred

Purpose: give K-2 one generic loader/evaluator extension to build next. The 12 live profiles use `locationFragments`
or the two JSON-built pilots. Everything below is for the remaining Bucket 1 objectives that cannot be safely solved
by a bare fragment.

## Current Live Baseline

| BP | Objective | Live slot |
|---|---|---|
| `7_135` | DBO | `locationFragments=["dantooine"]` |
| `7_300` | RO | `locationFragments=["ralltiir"]` |
| `109_4` | QMC | `locationFragments=["bespin","cloud city"]` |
| `111_4` | MBO | `locationFragments=["yavin 4"]` |
| `219_48` | ZH | `locationFragments=["lothal"]` |
| `301_2` | CITC | `locationFragments=["bespin","cloud city"]` |
| `201_39` | IE | `locationFragments=["tatooine"]` |
| `301_4` | TSOT | `locationFragments=["tatooine"]` |
| `204_32` | OA | `locationFragments=["jakku"]` |
| `209_29` | THNIWRC | `locationFragments=["scarif"]` |
| `8_167` | Endor | JSON-built pilot |
| `12_179` | My Lord | JSON-built pilot |

No bulk-enable from this document. Each row still needs K-2 boundary math before `loaderEnabled:true`.

## Rule Shape

Use one generic `objectiveRules` model, with named alternatives. Do not make separate one-off Java fields for each
objective. That would merely move the spaghetti and give it a JSON costume.

### `flipLocationRules[]`

```json
{
  "id": "string stable id",
  "phase": "preFlip | postFlip",
  "purpose": "flip | stayFlipped | flipBack",
  "mode": "allOf | anyOf",
  "alternatives": [
    {
      "relation": "control | occupy | presentAt | controlWith | occupyWith",
      "controller": "self | opponent",
      "locationFilterKey": "registry key or dynamic ref",
      "locationFragments": ["optional title fragments for current scorer"],
      "count": { "comparator": ">=", "value": 1 },
      "actorFilterKey": "optional registry key",
      "requiredSide": "LIGHT | DARK | optional",
      "opponentConstraint": {
        "relation": "control | occupy | presentAt",
        "locationFilterKey": "optional",
        "count": { "comparator": "< | == | >", "value": 1 }
      },
      "scoreRole": "setupLocation | flipProgress | flipGate | stayFlipped",
      "sourceText": "short source-derived clause"
    }
  ]
}
```

### `actorLocationRules[]`

```json
{
  "id": "string stable id",
  "phase": "preFlip | postFlip",
  "purpose": "flip | stayFlipped | flipBack",
  "relation": "presentAt | absentFrom | controlsWith | occupiesWith | sameSiteAs",
  "actorFilterKey": "registry key",
  "locationFilterKey": "registry key or dynamic ref",
  "coActorFilterKey": "optional second actor",
  "opponentActorFilterKey": "optional opponent actor",
  "count": { "comparator": ">=", "value": 1 },
  "scoreRole": "keyActor | actorToSite | denyOpponentActor",
  "sourceText": "short source-derived clause"
}
```

### `dynamicLocationRules[]`

```json
{
  "id": "string stable id",
  "source": "subjugatedPlanet | renegadePlanet | repSpecies | setupChoice",
  "derivedLocationFilterKey": "dynamicPlanet.site | dynamicPlanet.battlegroundSite | dynamicPlanet.location",
  "matchingActorFilterKey": "optional dynamic matching operative or species",
  "sourceText": "short source-derived clause"
}
```

## Loader Requirements

| Requirement | Reason |
|---|---|
| Registry keys must resolve to real `Filters.*` or dynamic runtime filters. | No lore-token guessing in evaluator code. |
| Alternatives must be first-class. | WYS, RST, Y4BO, INV, AITC, and MKOS have real OR conditions. |
| `phase` must distinguish front and back. | Flip and flip-back often use related but not identical predicates. |
| `sourceText` is audit only. | Runtime must use typed fields, not parse this string. |
| Existing `locationFragments` can remain as coarse relevance. | The new rules refine count, actor, and relation without removing current spread pressure. |

## `flipLocationRules[]` Rows

| BP | Objective | Rules to encode |
|---|---|---|
| `8_78` | RST | AnyOf: control 3 Endor locations, or Bunker blown away. Flip-back/stay: opponent move phase unless Bunker blown away. Needs `Endor_location`, `Bunker`, `blownAway(Bunker)`. |
| `10_26` | WYS | AnyOf: occupy 2 battlegrounds with smugglers, or complete Kessel Run. Needs `battleground`, `smuggler`, `Kessel_Run_complete`. |
| `203_19` | DMTA | Delivered Stolen Data Tapes plus Rebel control/occupy relations across Tatooine and Alderaan. Needs `Stolen_Data_Tapes_delivered`, `Tatooine_location`, `Alderaan_location`, `Rebel`. |
| `208_26` | Y4BO | AnyOf: four Rebels on table, or Yavin 4 system/site relation with Rebel presence and unique snub fighter relation. Needs `Yavin_4_location`, `Rebel`, `unique_snub_fighter`. |
| `222_27` | TEKWRH | Opponent occupies your Hoth location. Flip-back when opponent no longer occupies your Hoth location. Needs ownership-aware `your_Hoth_location`, not plain `Hoth_location`. |
| `14_113` | INV | Control Theed Palace Throne Room with presence droid plus control Naboo system. Flip-back if opponent controls Naboo system or Throne Room. Needs `Theed_Palace_Throne_Room`, `presence_droid`, `Naboo_system`. |
| `208_57` | IWTM | First Order characters control 2 battlegrounds, plus BB-8/map constraints. Needs `First_Order_character`, `battleground`, `BB8`, map state from existing V186 logic. |

## `actorLocationRules[]` Rows

| BP | Objective | Rules to encode |
|---|---|---|
| `12_88` | PMCTTS | Senator or agenda character present at Galactic Senate count gate. Needs `senator`, `agenda_character`, `Galactic_Senate`. |
| `13_46` | WLHT | Opponent Dark Jedi present at interior Naboo battleground. Needs `opponent Dark_Jedi`, `interior_Naboo_battleground_site`. |
| `14_52` | WHAP | Control Theed Palace Throne Room with Amidala or source actor package. Needs `Theed_Palace_Throne_Room`, `Amidala`, likely `Panaka` after source re-check. |
| `208_25` | HITCO | Luke or Jedi present at battleground while opponent high-ability gate is false. Needs `Luke`, `Jedi`, `battleground`, `opponent ability threshold`. |
| `211_36` | TGMNAL | Luke on Ahch-To plus Resistance count/occupy alternatives. Needs `Luke`, `Ahch-To`, `Resistance_character`. |
| `7_299` | ISBO | Four ISB agents on table or ISB agents control 2 Rebel Base locations. Needs `ISB_agent`, `Rebel_Base_location`. |
| `10_29` | AOBS | Xizor or Legacy Shada at battleground while target character is not at battleground. Needs `Xizor`, `Shada`, target profile actor, `battleground`. |
| `12_180` | NMNPND | Watto present at and occupying Watto's Junkyard. Needs `Watto`, `Wattos_Junkyard`. |
| `13_73` | LTMTFM | Your Dark Jedi present with opponent Jedi at interior Naboo battleground. Needs `your Dark_Jedi`, `opponent Jedi`, `interior_Naboo_battleground_site`. |

## `dynamicLocationRules[]` Rows

| BP | Objective | Dynamic source-derived values |
|---|---|---|
| `7_137` | LU | `source=subjugatedPlanet`; matching operatives control 3 battleground sites at that planet; flip-back if occupying fewer than 2. |
| `7_298` | IO | `source=renegadePlanet`; matching operatives control 3 battleground sites at that planet; flip-back if occupying fewer than 2. |
| `112_1` | AITC | `source=repSpecies`; occupy 2 battleground sites with Rep/species and Tatooine or non-Tatooine alternatives. Needs dynamic actor, not a static planet fragment. |
| `112_15` | MKOS | `source=repSpecies`; occupy 2 battleground sites with Jabba/Bib/Rep species alternatives. Needs dynamic actor plus Tatooine/non-Tatooine relation. |

## Registry Keys To Add First

| Key family | Keys |
|---|---|
| Locations | `Endor_location`, `Bunker`, `Bespin_system`, `Cloud_City_site`, `Cloud_City_battleground_site`, `Tatooine_location`, `Alderaan_location`, `Yavin_4_location`, `Hoth_location`, `your_Hoth_location`, `Theed_Palace_Throne_Room`, `Naboo_system`, `Galactic_Senate`, `Ahch_To_location`, `Rebel_Base_location`, `Wattos_Junkyard`, `interior_Naboo_battleground_site` |
| Actors | `smuggler`, `senator`, `agenda_character`, `Dark_Jedi`, `Jedi`, `Luke`, `Amidala`, `Panaka`, `Rebel`, `Resistance_character`, `First_Order_character`, `ISB_agent`, `Xizor`, `Shada`, `Watto`, `presence_droid`, `unique_snub_fighter` |
| State | `Bunker_blownAway`, `Kessel_Run_complete`, `Stolen_Data_Tapes_delivered`, `BB8_or_map_state`, `dynamic_subjugated_planet`, `dynamic_renegade_planet`, `dynamic_rep_species` |

## Execution Order For K-2

1. Add the JSON fields and Gson DTOs with no consumers. Verify parse only.
2. Add registry keys and fail-closed logging for unknown keys. Unknown key means no score, not a guessed score.
3. Wire evaluator reads for `flipLocationRules[]` using existing V136 magnitudes first.
4. Wire `actorLocationRules[]` only after location rules are stable.
5. Enable one objective per rule type after boundary math. Suggested pilots: `TEKWRH` for ownership-aware location,
   `NMNPND` for simple actor-at-site, then `LU` or `IO` for dynamic planet.

## Boundary Reminder

The old fragment pilot gives spread pressure. These rules add exactness. They should not replace the current
`locationFragments` bonus until the exact rule has an equal or better score path for setup locations, deploy targets,
and movement. Old rules get dominated, not erased. This remains annoying and still true.
