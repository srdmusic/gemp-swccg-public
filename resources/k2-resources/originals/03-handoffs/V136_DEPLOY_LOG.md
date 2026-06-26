# V136 Deploy Log + Revert Instructions

**Deploy date:** 2026-05-26
**Branch:** ai-improvements-v91
**Pre-deploy commit:** (capture before push)

## Files modified

### NEW file
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/CharacterDeploySiteEvaluator.java`

### Modified (commenting out old rules + wiring V136)
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.java`
  - V90 (~line 1552) — armed-enemy solo block → SUPERSEDED, replaced by V136 §A
  - V67aj (~line 3398) — spread destination tiered scoring → SUPERSEDED, replaced by V136 §B
  - V67al (~line 3488) — power-stack penalty → SUPERSEDED, replaced by V136 §B (now correctly gated by isUncontested)
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java`
  - V122 (~line 1653) — V90 mirror in hand-deploy route → SUPERSEDED, replaced by V136 §A
  - V67as (~line 2926) — V67aj+V67al mirror for hand-deploy route → SUPERSEDED, replaced by V136 §B
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/DeployEvaluator.java`
  - Same V90/V67aj/V67al mirror locations
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/CardSelectionEvaluator.java`
  - Same V122/V67as mirror locations

### Reference docs updated
- `AI_CHANGELOG.md` — V136 entry in "V130-V135 ACTIVE RULES" section
- `AI_VERSION_HISTORY.md` — V136 family block

## Revert plan if V136 causes regressions

**Symptoms to watch for in test games:**
- Rando passes deploy phase entirely (V136 over-penalizing everything)
- Rando never deploys characters (likely §A misfire on team ability check)
- Rando deploys low-ability characters solo (V136 §A buddy-lookahead misfire)
- Rando deploys to NBG sites turn 1-2 when BGs available (NBG penalty not firing)
- Rando deploys to 3rd BG turn 1-2 with no objective benefit (§D not firing)
- Build / startup errors after the rebuild (compile or runtime issue with the new class)

**Quick revert (without losing other work):**

```bash
cd /Users/steve/gemp-swccg-public
git revert <V136-commit-hash> --no-edit
mvn -q -f src/pom.xml -pl gemp-swccg-async -am package -DskipTests
./bin/gemp restart
```

This restores all five superseded rules (V90/V122/V67aj/V67as/V67al) and uncomments their bodies. The CharacterDeploySiteEvaluator.java file remains in the tree but is no longer called from the evaluators — harmless.

**Hard revert (back to pre-V136 state, drop the new file too):**

```bash
cd /Users/steve/gemp-swccg-public
git reset --hard <commit-before-V136>
mvn -q -f src/pom.xml -pl gemp-swccg-async -am package -DskipTests
./bin/gemp restart
```

Use only if soft revert isn't enough. CAUTION: `--hard` discards uncommitted work.

## What's stubbed (TODO follow-ups)

These passes-through-default values mean V136 is somewhat conservative on day 1. Expect to wire them up after observing real-game behavior:

1. **`deckShipCount = 0`** in callers — means §D2 ship-heavy override is OFF. A TIE-rush deck would have its 3rd system blocked. Wire by adding `DeckOracle.countShipsInDeck()` method and resolving at call time.

2. **`perSiteEffectActive = false`** in callers — means TDIGWATT-style "for each site" decks won't get the NBG-penalty override. Wire by scanning active permanent cards' game text for the 7 patterns Steve approved (for each location / for each battleground / per site you control / for each site / for each system / for each docking bay / for each battleground you occupy).

3. **`isAboard = false`** in §A — aboard-ship detection stub. TIE Pilot solo aboard a ship into an empty system currently scores -1500. Wire by detecting when deployingCard goes aboard a friendly ship at the candidate system.

4. **Sibling V137** (MoveEvaluator V34 power-comparison gate, Kylo→D'Qar bug from 2026-05-26 replay) is a SEPARATE V-tag, not part of V136.

5. **Sibling V138** (ship/vehicle/pilot deploy logic, IE Objective speeder-without-pilot bug) is a SEPARATE V-tag. Notes at /tmp/V138_SHIP_VEHICLE_PILOT_NOTES.md.

## Spec reference

Full spec: /tmp/V136_SPEC_V3.md
Rule catalog: /tmp/V136_RULE_CATALOG.md
Original v2 review notes: /Users/steve/gemp-swccg-public/V136_HANDOFF.md

## Game-replay validation cases V136 should fix

From 2026-05-26 replay `5bognj14thaf44kn.xml.gz`:

- Turn 2: Rando under-reinforced Salt Plateau, deployed ships instead. V136 §A should now score reinforcement +600 vs ships+0, prefer reinforcement.
- Turn 3: FN-2199 deployed solo at new Crait: Outpost Entrance Cavern. V136 §A should now block at -1500.

Validate by watching `V136` debug logs and confirming deploys match these expectations.
