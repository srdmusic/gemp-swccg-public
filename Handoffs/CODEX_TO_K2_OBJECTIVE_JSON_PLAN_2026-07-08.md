# Codex -> K-2 Temporary Execution Plan: Objective JSON Runtime Link

Date: 2026-07-08
Owner split: Codex owns canonical objective data. K-2 owns Java loader, scoring, mirrors, changelogs, compile.

## Files

- Runtime contract file K-2 requested: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`
- Canonical 58-entry data file: `resources/Objective_Playbook_Facts_2026-07-08.json`
- Source WIP merged by Codex: `resources/Objective_Playbook_Facts_Codex_WIP.json`
- Analyzer scan source: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/strategy/ObjectiveAnalyzer.java`

## Data Status

| Check | Result |
|---|---|
| Entries | 58 |
| Inventory rows | 0-57 complete |
| Duplicate front BPs | 0 |
| Unresolved blueprint id errors | 0 |
| Source-only ids | `601_150` verified by `Card601_150.java` |
| Canonical aliases merged | `sites -> locationRequirements`, `characters -> characterRequirements`, `pullChain -> pullOrDeployActions` |
| Setup fields added | `startingLocations`, `startingEffects`, `startingInterrupts`, `objectiveNamedLocations` |
| Analyzer hardcodes captured | IWTM, ISB, Hunt Down, Shield, My Lord, Endor, Invasion, TDIGWATT V Executor restriction, Bespin/Cloud City link |
| Runtime profiles | 58 in `objective_playbooks.json` |
| Runtime filter keys used | `senator`, `Galactic_Senate`, `biker_scout`, `Bunker` |
| Runtime rollout flags | `rolloutEnabled=true` only for My Lord `12_179` and Endor `8_167`; all other profiles are `data_only` until promoted |

## Execution Contract

1. Load one canonical JSON resource from the server jar.
2. Select active objective profile by front/back blueprint id first, title fragments second.
3. Hydrate existing `ObjectiveAnalyzer` slots from JSON only when `rolloutEnabled=true`:
   - `flipConditionLocationFragments`
   - `requiredCardsOnTable`
   - `pullableCards`
   - `flipCriticalControlSite`
   - `flipCriticalControlCardIds`
   - `keyCharacter` / `keySite` only where real runtime filters exist.
4. Use new setup fields for starting-card logic:
   - `startingLocations`
   - `startingEffects`
   - `startingInterrupts`
   - `objectiveNamedLocations`
5. Keep parser fallback for parse misses, unlisted objectives, and `rolloutEnabled=false` profiles.
6. Do not comment old objective-specific blocks until JSON hydration plus boundary math proves equivalent or intentionally dominant behavior.

## Pilot Order

| Phase | Objective | Reason |
|---|---|---|
| 1 | My Lord `12_179` | Existing pilot, keyCharacter/keySite/weights |
| 1 | Endor Operations `8_167` | Existing pilot, flip-gate control and exact gated ids |
| 2 | TDIGWATT V `226_12` | Starting location, pull chain, Bespin/Cloud City, Executor restriction |
| 3 | Shield `222_14` / `222_30` | Setup deploys, engine cards, hard-lose guard |
| 4 | Low-risk location/count objectives | Batch after pilots pass |

## Boundary Rules

- Reuse existing V-tag magnitudes from `weights`.
- No invented scoring numbers from JSON.
- Old rules are dominated or commented in place, never deleted.
- Mirror rando to chosenone.
- Update both changelogs in the same session as Java changes.
- Compile in-container, check real Maven exit code, byte-verify jar, never deploy over a live game.

## Codex Notes For K-2

- `requiredCardsOnTable` was corrected after generation so setup locations do not leak into required flip cards. Endor now lists only `establish secret base` and `ominous rumors`.
- Runtime padded aliases like `207_025` live under `runtimeBlueprintIdAliases`, not canonical `blueprintIds`.
- `resolvedSample` and `resolvedCount` are evidence only. Runtime should use `runtimeFilter`.
- `ObjectiveHandler.java` remains dead. Do not source from it.

Ping Codex after loader/hydration shape is known. Codex can continue as a data/verifier agent while K-2 implements.
