# Codex Card-Search Plan — resolving objective requirements to card ids/titles

Date: 2026-07-07. For: Codex (Alfred), the heavy lifting on the objective inventory normalization.
Pairs with: `resources/Objective_Normalization_Schema_2026-07-07.md` (the target schema).
Rule: verify every recipe against KNOWN cards before trusting it. Do not fabricate a field or a filter — check it.

## Data sources, in priority order

| Source | Path | Use it for | Trust |
|---|---|---|---|
| Objective Java effect code | `src/gemp-swccg-cards/.../cards/setN/{dark,light}/Card*.java` | The Filters the objective ALREADY uses in its deploy/pull/flip effects — the machine-usable requirement, pre-parsed | HIGHEST |
| Engine Filters vocabulary | `src/gemp-swccg-logic/.../filters/Filters.java` | The named Filters (`battleground`, `Endor_site`, `senator`, `battleground_site`…) + their criteria | HIGHEST |
| Card blueprint DB | `src/gemp-swccg-cards/src/main/resources/card_blueprint_database_{dark,light}.json` | Resolving a Filter into a candidate card LIST; card metadata (id, title, side, category, subtype, icons, keywords, expansionSet) | HIGH (structured); text fields can be STALE |
| Card `setGameText()` | the same `Card*.java` | Authoritative game text (DB game text can be a stale draft — confirmed on Hidden Path 226_28) | HIGHEST for text |

**Golden rule:** read the requirement as a FILTER from the objective's Java first. Only parse English when no Filter is exposed.

## Search technique 1 — read the objective's own Filters (primary)

For each objective `Card*.java`, extract the Filters passed to its effects. Example, Endor Operations `Card8_167.java`:
- `new DeployCardFromReserveDeckEffect(action, Filters.Endor_system, ...)` -> deploys an `Endor_system`
- Bunker / Landing Platform deploys -> their Filters
- `TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.Ominous_Rumors, Filters.Establish_Secret_Base), ...)` -> pull target = those two named cards
- flip condition `GameConditions.canSpot(..., Filters.Ominous_Rumors)` + `Filters.Establish_Secret_Base` -> flip requirement = both on table

Those `Filters.X` names ARE the requirement. Map each to the schema:
- `Filters.Endor_system` -> LocationRequirement{planet=ENDOR, subtype=SYSTEM}
- `Filters.Ominous_Rumors` -> NamedCardRef (resolve its ids/titles, below)
- `Filters.battleground_site` -> LocationRequirement{subtype=SITE, battlegroundRequired=true}

Grep helper: `grep -nE 'Filters\.[A-Za-z_]+|CardSubtype\.|Icon\.|Keyword\.' Card8_167.java`.

## Search technique 2 — map a Filter to its DB criterion, then resolve candidates

When you need the candidate LIST for a Filter (for ResolvedCandidates), find what the Filter checks in `Filters.java`, then query the DB.

Verified examples (checked against real cards):
| Requirement | Filter | DB criterion to resolve candidates |
|---|---|---|
| Endor site | `Filters.Endor_site` | `cardCategory==LOCATION && cardSubtype==SITE && icons has ENDOR` |
| Endor battleground site | `Filters.and(Endor_site, battleground)` | above + battleground (see note) |
| any planet system | — | `cardCategory==LOCATION && cardSubtype==SYSTEM && icons has PLANET` |
| senator | `Filters.senator` | `cardTypes has CHARACTER && (keywords has SENATOR || lore contains "senator")` |
| Neimoidian pilot | `Filters.and(Neimoidian, pilot)` | `CHARACTER && species==NEIMOIDIAN && icons has PILOT` |
| interior vs exterior site | — | icons has `INTERIOR_SITE` / `EXTERIOR_SITE` |

Battleground NOTE: `Filters.battleground` = `modifiersQuerying.isBattleground(...)` — a RUNTIME value (modifiers can change it). For the static snapshot, approximate battleground as "location generates force for BOTH sides" (has both a dark-force and light-force icon) and MARK it approximate. Rando resolves the real thing via `Filters.battleground` at decision time; the snapshot is a human/regression aid, not runtime truth. Validate your approximation against a known battleground set before trusting it.

## Search technique 3 — named cards: collect ALL ids + both titles (the virtual-reprint rule)

For any NAMED card (objective, Establish Secret Base, Ominous Rumors…):
1. Find every DB row whose title matches (base + all virtual [V] reprints share the title). Collect their `cardId`s.
2. Collect title fragments for BOTH flip sides (front + `_BACK`), lowercased.
3. Output `NamedCardRef{ blueprintIds:[…all ids…], titleFragments:[…] }`.
Verified example: Establish Secret Base -> ids `8_124` (base), `207_25` (V), `601_260`; Ominous Rumors -> `8_127`, `223_19`, `601_261`.
Match at runtime = id-in-set OR title-contains. Evergreen: a new V reprint is caught by title until its id is added.

## Gotchas (each has burned someone)
- DB game text can be STALE draft; `Card*.java setGameText` wins. Spot-check every row you resolve text from.
- `battleground` / `occupies` / `controls` / `present` are RUNTIME (modifier-aware). Snapshot = approximation; runtime = Filter.
- DYNAMIC planets: "the Subjugated/Renegade planet", "the planet where your Hidden Base is" — chosen in-game. Mark `planet=DYNAMIC`, resolved at runtime from `SetWhileInPlayData`. Do NOT pre-resolve.
- Exceptions/negations: "except a Kamino, Tatooine, or [Reflections III] location" -> `exceptions:[…]`.
- ◇ = light-side location icon, □ = dark-side. Confirm how the DB encodes side-of-location before resolving "◇ site".
- Set/era brackets ("[Episode VII]", "[Clone Army]") -> match via `cardTypes`/`icons`/`expansionSet`, not the literal bracket text.

## Output (per Steve)
- MD row: short summary per broad requirement, e.g. "Resolved: 7 Endor battleground sites".
- JSON sidecar: full `ResolvedCandidates[]` = `{cardId, title, side, cardCategory, cardSubtype, icons, reasonMatched}`, attached to the objective's normalized requirement, plus the requirement RECIPE (LocationRequirement/CharacterRequirement) so Rando resolves live.

## Verification (before marking a row done)
- Re-resolve the requirement's Filter against the DB and confirm the candidate count/titles look sane (e.g. "Endor site" returns only Endor: … SITE cards).
- Confirm named-card ids: every id you list actually shares the title in the DB.
- Confirm game text came from Java, not DB.

## Sequencing (align with the pilot order)
1. My Lord (12_179) — 1 named site (Galactic Senate) + senator CharacterRequirement + flip(present,3). Smallest; proves the schema end-to-end.
2. Endor Operations (8_167) — LocationRequirement + pull-chain + flip-gate centerpiece (already traced).
3. The rest, one objective per row, broad requirements resolved to candidates.
4. Generic long-tail: the parser already emits requiredCardsOnTable / pullableCards / flipConditionLocationFragments for every objective; feed those through the same Filter resolution.

Reply in the mailbox with the first enriched row (My Lord) so K-2 can sanity-check the shape before you run all 58.
