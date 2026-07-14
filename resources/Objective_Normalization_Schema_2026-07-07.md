# Objective Normalization Schema (machine-usable playbook facts)

Date: 2026-07-07. Author: K-2, reviewing Alfred's `Objective_Blueprint_Inventory_2026-07-07.{md,json}`.
Status: proposal for review (Steve + Alfred). No Java yet.

## Core principle

Two different artifacts, do not conflate:
- **Inventory (human + authoring):** raw text, fragments, and a RESOLVED CANDIDATE SNAPSHOT. Lives in the MD (summary) + JSON sidecar (full).
- **Runtime playbook (Rando):** stores the semantic REQUIREMENT as a Filter RECIPE and resolves via the engine's own `Filters` at decision time. It NEVER hardcodes a candidate id list. New virtual cards auto-qualify — this is the broad-requirement analog of the id+title `NamedCardRef` for named cards. Both evergreen.

The static candidate snapshot is for human authoring + a regression check ("does the live resolve still return these 7?"), not the runtime source of truth.

## The types

### NamedCardRef  (specific named cards: the objective itself, Establish Secret Base, Ominous Rumors…)
- `blueprintIds: [ "8_124", "207_25", "601_260" ]`  (base + every virtual reprint)
- `titleFragments: [ "establish secret base" ]`  (lowercased, both flip-side titles)
- match = `blueprintIds.contains(id) OR titleFragments.anyMatch(title.contains)`

### LocationRequirement  (broad location text: "an Endor battleground site", "any planet system", "◇ site")
- `planetOrSystemGroup`: e.g. `ENDOR`, `BESPIN`, `HOTH` (matched by the location's PLANET icon / location-group), or `ANY`, or `DYNAMIC` (e.g. "the Subjugated planet" chosen at deploy)
- `subtype`: `SITE | SYSTEM | SECTOR | ANY_LOCATION`
- `battlegroundRequired`: bool
- `interiorExterior`: `INTERIOR | EXTERIOR | ANY` (optional)
- `iconsRequired`: [icon…] (optional, e.g. light-side ◇ marker for "◇ site")
- `titleContains`: [str] (optional, only when text truly names a title token)
- `sideOwnership`: `SELF | OPPONENT | ANY` (optional, for "opponent controls no…")
- `exceptions`: [LocationRequirement | NamedCardRef]  (e.g. "except a Kamino, Tatooine, or [Reflections III] location")
- RUNTIME resolve = `Filters.and( planetGroupFilter, subtypeFilter, battlegroundFilter, … )`

### CharacterRequirement  (senator, Neimoidian pilot, Vader, "matching operatives") — NEVER a title fragment
- `persona`: e.g. `VADER` (optional)
- `keyword`: e.g. `SENATOR` (optional)
- `filterType`: composed Filter, e.g. `Neimoidian + pilot`, `Jedi`, `Rebel_scout` (optional)
- `loreContains`: [str] (optional — e.g. senator lore, per the 29-of-35 keyword gap)
- `abilityConstraint`: e.g. `> 4` (optional)
- RUNTIME resolve = the composed `Filters`

### PullOrDeployAction  (the pull chain, per link)
- `phase`: `SETUP | PRE_FLIP | POST_FLIP`
- `verb`: `deploy | take | download | upload | reveal | play | stack | retrieve`
- `sourceZone`: `RESERVE | LOST | USED | FORCE_PILE | OUTSIDE_DECK | TABLE`
- `target`: NamedCardRef | LocationRequirement | CharacterRequirement
- `cadence`: `ON_DEPLOY | ONCE_PER_TURN | ONCE_PER_GAME | REPEATABLE`
- `cost`: Force (optional)
- `enablesFlip`: bool  (marks a link as a flip GATEWAY so it's scored as such, e.g. Establish Secret Base)

### FlipRequirement
- `relation`: `CONTROL | OCCUPY | PRESENT | ON_TABLE | DELIVERED | CAPTURED | BLOWN_AWAY | MOVED_TO | COUNT_BENEATH`
- `count`: int
- `target`: LocationRequirement | NamedCardRef | CharacterRequirement
- `byWhom`: CharacterRequirement (optional, e.g. "your matching operatives", "Jedi", "3 senators")
- `opponentConstraint`: e.g. `{relation: CONTROL, count: 0, target: LocationRequirement(planet=X)}` ("opponent controls no X")
- `exceptions`: […]
- `alternatives`: [FlipRequirement]  (for "X OR Y" flip conditions — very common)

### FlipBackRequirement — same shape as FlipRequirement (usually the negation of the flip)

### HardLose  ("Place out of play if…")
- `condition`: e.g. `BLOWN_AWAY`
- `target`: LocationRequirement

### ResolvedCandidates  (INVENTORY ONLY — build-time snapshot of a LocationRequirement/CharacterRequirement)
- per card: `{ cardId, title, side, cardCategory, cardSubtype, icons, reasonMatched }`
- `reasonMatched` e.g. "title/group ENDOR + subtype SITE + battleground=true"
- MD carries a one-line summary ("Resolved: 7 Endor battleground sites"); JSON carries the full list.

## Which inventory columns are human-only (unsafe to wire)
- `Analyzer Facts` (locFragments / pullable / flip="…") — raw substrings; authoring hint only.
- `Starts With` / `Pulls / Deploys` free text — decompose into PullOrDeployAction[] before wiring.
- `Flip To Back` free text — decompose into FlipRequirement before wiring.
Safe-to-wire today: `frontBp/backBp`, `side`, `setIcons`, `frontAbbrev`, and (once Java-sourced) the verbatim game text.

## Data-quality flag
The card DB (`card_blueprint_database_*.json`) can carry STALE/draft location text — confirmed on Hidden Path `226_28` where the DB text disagrees with `Card226_028.java setGameText` (the authoritative, compiled one). The sidecar's `frontGameText/backGameText` must be the Java `setGameText`, not the DB text. Alfred's "Java wins" rule is correct; verify it actually held in the JSON.

## Rows that most need semantic parsing (fragments will fail hardest)
- "any planet system and one ◇ site to that system" (Local Uprising 7_137, Imperial Occupation 7_298) — DYNAMIC planet + ◇ subtype.
- "three battleground sites related to the Subjugated/Renegade planet" — count + relation + DYNAMIC planet.
- "an Endor / Cloud City / Tatooine battleground site", "a marker site", "a Jakku location" — planet-group + subtype + battleground.
- "◇ site", "[Episode VII] location/battleground", "a holocron", "a corvette" — category/subtype/keyword, not titles.
- "3 senators at Galactic Senate", "matching operatives", "two Rebel scouts at each" — CharacterRequirement + count.

## Pilot order
1. **My Lord (12_179)** — safest first. Named site (Galactic Senate) + CharacterRequirement(senator) + FlipRequirement(present, 3, byWhom senator, at Galactic Senate). Authored + Codex/work-verifier PASS. Validates the plumbing end-to-end, near-zero risk.
2. **Endor Operations (8_167)** — second. The LocationRequirement centerpiece: proves "Endor site"/"Endor battleground" resolution + the pull chain (PullOrDeployAction with enablesFlip) + flip-gate. Already fully traced.
3. Then generic profile for the long tail using the same LocationRequirement/FlipRequirement resolve.
