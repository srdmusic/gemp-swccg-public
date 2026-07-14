# V82 pull-validation reproductions

Date: 2026-07-11
Owner: Codex/Alfred, read-only verification
Tested artifact: deployed `web.jar` at HEAD `326895c77`

## Result

The current source-text validator does not preserve selector semantics. It can
both reject a legal pull and approve an illegal pull.

| Source | Actual selector | Test Reserve Deck | Actual verdict | Expected |
|---|---|---|---|---|
| `224_20` Son Of Skywalker (V) | `Persona.ANAKINS_LIGHTSABER` | Anakin's Lightsaber | `WILL_FAIL` | `WILL_SUCCEED` |
| `601_129` Cell 2187 (V) | `Filters.spy AND Persona.R2D2` | non-spy Artoo, Brave Little Droid | `WILL_SUCCEED` | `WILL_FAIL` |
| `224_8` A Cunning Warrior | `Icon.CLOUD_CITY AND titleContains("Corridor")` | Hoth: Echo Corridor | `WILL_SUCCEED` | `WILL_FAIL` |
| `501_41` I'm Sending My Apprentice | `Icon.CORUSCANT AND Filters.Naboo_site` | Hoth: Echo Corridor | `WILL_SUCCEED` | `WILL_FAIL` |

All four tests used the real card blueprint in a one-card Reserve Deck and
called `DeckOracle.validatePullFromSourceCard` from the deployed jar.

## Evidence

### Multiword persona false failure

- Source: `Card224_020.java:52,91-98`
- Parsed target: `anakin's lightsaber here`
- Result: `WILL_FAIL`
- Cause: `DeckOracle.java:1363-1371` splits the target into words, then compares
  each word with the complete `Persona.getHumanReadable()` value. A multiword
  persona such as `Anakin's Lightsaber` can never match.

### Persona qualifier false success

- Source: `Card601_129.java:46,75-83`
- Parsed target: `spy r2-d2 for free`
- Candidate: `Card14_003`, which has `Persona.R2D2` and typed
  `hasKeyword(Keyword.SPY) == false`
- Result: `WILL_SUCCEED`
- Cause: `spy` is absent from `wordHasPredicate` and `blueprintMatchesWord` at
  `DeckOracle.java:1463-1522`. Persona rescue ignores unrecognized qualifiers.

### Qualified corridor false success

- Source: `Card224_008.java:40,68-81`
- Parsed target: `cloud city corridor`
- Candidate: `Card3_058`, typed `hasIcon(Icon.CLOUD_CITY) == false`
- Result: `WILL_SUCCEED`
- Cause: `hasTargetInZone` reduces the phrase to last word `corridor` before
  validating the Cloud City qualifier.

### Qualified site false success

- Source: `Card501_041.java:39,68-76`
- Parsed target: `coruscant naboo site`
- Candidate: `Card3_058`, typed `hasIcon(Icon.CORUSCANT) == false`
- Result: `WILL_SUCCEED`
- Cause: V82.1 maps final word `site` to `CardCategory.LOCATION` and returns
  success before validating the Coruscant and Naboo qualifiers.

## Repair boundary

`WILL_SUCCEED` is valid only when one candidate in the requested zone satisfies
the complete selector:

1. Match a full multiword persona phrase, not one token.
2. Treat the category as the base type, not a complete match.
3. Enforce every recognized icon, keyword, species, subtype, and persona
   qualifier against the same candidate.
4. Return `UNKNOWN`, never `WILL_SUCCEED`, when semantic qualifier words remain
   unresolved.
5. Prefer the selected action's text/filter. Use full source game text only as a
   fallback because multi-mode cards describe selectors for several actions.

No Java files were edited by Codex.
