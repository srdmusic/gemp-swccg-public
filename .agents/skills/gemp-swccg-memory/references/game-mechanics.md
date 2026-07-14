---
title: GEMP-SWCCG Game Mechanics Reference
updated: 2026-03-03
purpose: Game phases, card types, zones, force mechanics, battle mechanics, build system, database
---

# GEMP-SWCCG Game Mechanics Reference

## 1. Game Phases (in order)

1. **PLAY_STARTING_CARDS** — Initial setup, deploy starting cards
2. **ACTIVATE** — Activate Force (move cards from Reserve Deck to Force Pile)
3. **CONTROL** — Control phase (special card mechanics)
4. **DEPLOY** — Deploy cards into play (characters, starships, locations, etc.)
5. **BATTLE** — Initiate and resolve battles at locations
6. **MOVE** — Move cards between locations
7. **DRAW** — Draw cards from Reserve Deck to hand
8. **END_OF_TURN** — Cleanup and state reset
9. **BETWEEN_TURNS** — Transition between player turns

Each phase has: StartOfPhaseGameProcess → PlayersPlayPhaseActionsInOrderGameProcess → EndOfPhaseGameProcess

## 2. Card Types (26 types)

**Characters:** ALIEN, CREATURE, DARK_JEDI_MASTER, DROID, IMPERIAL, JEDI_MASTER, REBEL, REPUBLIC, RESISTANCE, SITH
**Locations:** LOCATION
**Vehicles:** STARSHIP, VEHICLE, PODRACER
**Events/Orders:** ADMIRALS_ORDER, OBJECTIVE, EPIC_EVENT, FIRST_ORDER, JEDI_TEST, NEW_REPUBLIC
**Items:** DEVICE, WEAPON
**Modifiers:** DEFENSIVE_SHIELD, EFFECT, INTERRUPT

### Character Stats
- **destiny** — Drawn randomly for various effects
- **deployCost** — Force cost to put into play
- **power** — Combat strength
- **ability** — General capability (affects combat, some effects)
- **politics** — Political influence
- **landspeed** — Movement range
- **forfeit** — Force opponent loses if this card is lost in battle
- **armor** — Damage reduction
- **maneuver** — Ship handling ability

## 3. Card Zones

### In-Play (Public)
- `AT_LOCATION` — Cards at a location
- `SIDE_OF_TABLE` — Cards on table in play
- `LOCATIONS` — Location cards
- `ATTACHED` — Attached to other cards

### Piles (Face Down)
- `RESERVE_DECK` — Main draw deck
- `FORCE_PILE` — Activated force (life force)
- `USED_PILE` — Used cards
- `LOST_PILE` — Lost cards

### Private
- `HAND` — Player's hand
- `SABACC_HAND` / `REVEALED_SABACC_HAND`

### Special
- `STACKED` / `STACKED_FACE_DOWN`
- `OUT_OF_PLAY`
- `VOID` — Completely hidden

**Zone Properties:** isPublic(), isVisibleByOwner(), isInPlay(), isFaceDown(), isLifeForce(), isCardPile()

## 4. Force Mechanics

**Force Activation:** Move cards from Reserve Deck → Force Pile. Each location provides force icons (activation value).

**Force Draining:** At locations you control, force opponent to lose Force (move from their Life Force to Lost Pile). Force drain = number of your force icons at that location.

**Force Retrieval:** Move cards from Lost/Used Piles back to Reserve Deck.

**Life Force = Reserve Deck + Force Pile + Used Pile.** When Life Force is depleted (all three empty), player loses.

## 5. Battle Mechanics

### Initiation
- Must have presence at a location where opponent also has presence
- Costs Force (may be modified by cards like Battle Order)

### Resolution
1. **Battle Start Responses** — Play interrupts/reactions
2. **Weapons Segment** — Fire weapons
3. **Power Segment** — Calculate total power at location
4. **Battle Destiny Draws** — Draw destiny cards, add to power
5. **Attrition** — Loser's attrition = winner's total - loser's total
6. **Forfeit** — Loser must forfeit cards to satisfy attrition

### Power Calculation
Total power = Sum of character/ship power + battle destiny draws + modifiers

## 6. Card Blueprint System

Cards defined as Java classes extending abstract bases:
- `AbstractCharacter`, `AbstractAlien`, `AbstractAlienRebel`, `AbstractStarship`, `AbstractLocation`, etc.

**Blueprint ID format:** `[setNumber]_[cardNumber]` (e.g., "7_299")

**Expansion Sets:**
- 1-9: Original Decipher sets
- 10-13: Reflections series
- 14: Theed Palace
- 101-112: Virtual sets
- 200-226: Episode VII+ cards
- 301, 401, 501, 601: Premium, Dream, Playtesting, Legacy

**Cards location:** `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set[N]/`

## 7. Build System

### Maven Multi-Module
```
gemp-swccg (parent)
├── gemp-swccg-common       # Shared enums
├── gemp-swccg-logic        # Game rules engine
├── gemp-swccg-cards        # Card definitions
├── gemp-swccg-server       # Backend + AI
└── gemp-swccg-async        # Web server + frontend
```
Java 21, Maven 3.9.6

### Docker
- **gemp_app** — Java app (Amazon Corretto + Maven, 4GB heap)
- **gemp_db** — MariaDB

### Environment (.env)
```
APP_PORT=17001
DB_PORT=35001
MYSQL_DATABASE=gemp-swccg
MYSQL_USER=gemp
MYSQL_PASSWORD=Four_mason8pirate
MYSQL_ROOT_PASSWORD=gempukku
```

### Commands
```bash
./bin/gemp initialize      # First-time
./bin/gemp rebuild-fast    # Compile (no tests)
./bin/gemp reload-fast     # Rebuild + restart
./bin/gemp logs / status / shell / db-shell
```

## 8. Database Schema

| Table | Purpose |
|-------|---------|
| player | Users, bots, admins (id, name, password, type) |
| deck | Player decks (id, player_id, name, type, contents) |
| collection | Player card collections |
| game_history | Completed games (winner, loser, decks, format, recordings) |
| league | League definitions |
| league_participation | League sign-ups |
| league_match | League game results |
| merchant_data | Card marketplace tracking |
| gemp_settings | Global settings (privateGamesEnabled, aiTablesEnabled, etc.) |

**DB scripts:** `src/db-scripts/` — schema, sample decks, initial users

## 9. Sample Decks

Stored in `src/db-scripts/sample_decks.sql` and `utinni_sample_decks.sql`.
AI uses decks from the "Librarian" player account.

## 10. Game Formats

Defined in `src/gemp-swccg-server/src/main/resources/swccgFormats.json`

Key formats: open, classic, legacy, jawa, plus 30+ others.
Each format specifies: allowed card sets, banned cards, deck size, game timer.

## 11. Test Infrastructure

JUnit 5.7.2, Mockito 4.7.0

Test helpers in: `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/framework/`
- GameProcedures, Battles, Actions, Decisions, GameProperties, Assertions, etc.

## 12. Logic Subsystem Scale

| Component | Count |
|-----------|-------|
| Effects | 519+ files |
| Modifiers | 564+ files |
| Actions | 35+ directories |
| Card definitions | 2000+ |
| TriggerConditions.java | 287KB |
