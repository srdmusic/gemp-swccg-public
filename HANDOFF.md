# GEMP-SWCCG AI Bot — Claude Code Handoff Document

**Created:** March 19, 2026
**Purpose:** Complete context transfer for continuing AI bot development in Claude Code
**Owner:** Steve (steve@srdmusic.com, in-game: ASDF)

---

## 1. STAR WARS CCG GAME MECHANICS REFERENCE

### Core Concepts

**Force Economy:** Each turn, players activate Force from their Reserve Deck into their Force Pile. The amount equals the total number of their Force icons at locations they occupy. Force Pile is spent to deploy cards, initiate battles, and fire weapons. Unspent Force recirculates at end of turn.

**Locations:** The game board is built from Location cards. There are two types:
- **Systems** — space locations (e.g., Bespin, Tatooine, Coruscant). Starships deploy here.
- **Sites** — ground locations (e.g., Cloud City: Dining Room, Tatooine: Cantina). Characters deploy here. Sites are related to systems (Cloud City sites are "Bespin" sites).

**Force Icons:** Locations have Light Side and Dark Side Force icons. Controlling a location gives you Force generation equal to your icons there.

**Deployment:** Cards have a deploy cost paid from Force Pile. Characters deploy to sites. Starships/vehicles deploy to systems. Pilots can deploy aboard starships. Weapons deploy on characters.

**Occupying vs Controlling:**
- **Occupy** = you have presence (any character/ship) at a location where opponent has NO presence
- **Control** = you occupy AND your total ability at that location exceeds opponent's

**Presence:** A character or ship at a location gives you "presence" there.

### Battle System

**Initiating Battle:** During your Battle phase, you can initiate a battle at any location where BOTH players have presence, by spending 1 Force.

**CRITICAL — Battle Destiny:**
- You may draw battle destiny ONLY if your total ability at the battle location is >= 4
- Battle destiny is drawn from the top of your Reserve Deck — the card's destiny number (printed on card) is added to your total power
- Without battle destiny draws, you fight with just your raw power, which is a massive disadvantage
- This is why **ability >= 4 at every site is essential** — it's the single most important tactical threshold

**Battle Resolution:**
1. Both players' total power + battle destiny draws are compared
2. The player with LOWER total must lose cards with total forfeit value >= the difference (called "battle damage")
3. If you have higher total, opponent must forfeit cards; if lower, you must forfeit

**Attrition:** Some cards cause additional losses regardless of who wins.

### Matching Pilot + Starship System

Many pilot characters have a "matching starship" defined in their card blueprint:
- `setMatchingStarshipFilter(Filters.Executor)` — means this pilot matches Executor
- When a matching pilot is aboard their matching starship, the ship gets bonus power (typically +2 or +3)
- AMSD (Alert My Star Destroyer V) effect: reveal a pilot/ship from hand, pull the matching counterpart from Reserve Deck, deploy both simultaneously at -1 Force each

Key matching pairs:
- Admiral Piett → Executor
- Han Solo → Millennium Falcon
- Wedge Antilles → Red Squadron 1
- Lando Calrissian → Lady Luck
- Admiral Chiraneau → any Star Destroyer (has special game text)

### Objective System

**Objectives** are double-sided cards that define your deck's strategy:
- **Front side:** Lists conditions to "flip" the objective (e.g., "occupy 3 Bespin locations")
- **Back side:** Provides powerful ongoing effects BUT lists conditions that flip it BACK (e.g., "opponent occupies 2 Bespin locations")
- Flipping an objective is a major strategic milestone — it enables your deck's engine

**TDIGWATT (This Deal Is Getting Worse All The Time V):**
- Dark Side objective centered on Bespin/Cloud City
- **Flip condition:** Occupy 3 Bespin locations (Bespin system counts + any Cloud City sites)
- **Flip-back condition:** Opponent controls 2 Bespin locations
- **Strategy:** Deploy Bespin system turn 1 → Executor to Bespin (via AMSD or hand) → characters to Cloud City sites → flip → consolidate to 2 locations

**Hunt Down And Destroy The Jedi (V):**
- Dark Side aggro objective
- Requires Vader at battleground locations, Luke eventually
- Vader must actively hunt — not sit at safe locations

**ISB Operations:**
- Dark Side control/drain objective
- Requires ISB agents at Rebel Base locations

### Force Drain

During your Control phase, at each battleground location you control alone, you Force drain your opponent. They must move cards from their Reserve Deck to Lost Pile equal to your Force drain amount (typically 1 per Force icon, plus modifiers). This is how you whittle down the opponent's "life force" (Reserve Deck).

### Card Synergy Examples (Steve's Philosophy)

1. **Amidala vs Vader:** Amidala's game text cancels Vader's game text when at same location. Vader has a permanent weapon in his game text — deploying Amidala effectively disarms him.

2. **Piett → Gherant → Executor sites:** Deploying Piett aboard Executor allows pulling Commander Gherant from Reserve. Gherant once per deploy phase can pull an Executor site from Reserve. This creates a deployment pipeline that generates free locations (Force generation).

3. **Admiral Chiraneau aboard Star Destroyer:** Chiraneau's game text: when piloting a Star Destroyer at a battleground system, Force drain +1 at that location AND at all battleground systems within 2 parsecs you control with a Star Destroyer. This creates a Force drain amplification network.

4. **AMSD deployment pipeline:** Use Alert My Star Destroyer V to pull matching pilot+ship, deploy both at -1 each, immediately enabling space control.

---

## 2. COMPLETE CHANGELOG: V30 through V32

### V30 — Universal Matching Pilot + Starship Deploy Rule
**Files:** Rando DeployEvaluator.java, ChosenOne DeployEvaluator.java
**Date:** March 2026

Replaced the old Piett-specific hard block (-9999) with a universal rule that works for ALL matching pilot/ship combos:

**For CHARACTER deployment:**
- Checks `card.getBlueprint().getMatchingStarshipFilter()` to find if character has a matching ship
- **CASE 1 — Both in hand (+1000):** Matching pilot and starship both in hand → deploy them together NOW. Additional +1000 if deploying to system mentioned in objective.
- **CASE 2 — Ship in play (+300):** Matching ship already deployed → deploy pilot aboard it.
- **CASE 3 — Ship in reserve with AMSD (-500):** Soft penalty to prefer AMSD route, but allows manual fallback if AMSD fails.

**For STARSHIP/VEHICLE deployment:**
- Reverse check: scans hand for matching pilot when deploying a starship
- Same scoring tiers as above

### V31 — Pre-Flip vs Post-Flip Objective Strategy
**Files:** Rando DeployEvaluator.java, ChosenOne DeployEvaluator.java, Rando MoveEvaluator.java, ChosenOne MoveEvaluator.java
**Date:** March 19, 2026

**Deploy — Pre-flip:**
- Counts occupied vs unoccupied objective-relevant locations
- +250 for deploying to an UNOCCUPIED objective location (spread to meet flip condition)
- -50 for stacking on already-occupied objective location when unoccupied ones still need presence
- Solo deploys to objective locations are OK pre-flip — speed matters

**Deploy — Post-flip:**
- Finds the 2 strongest held objective locations (by power)
- +200 for reinforcing one of those 2 hold locations
- -100 for spreading to a 3rd+ objective location (only need 2 to prevent flip-back)

**Move — Post-flip consolidation:**
- If occupying 3+ objective locations but only need 2, identifies the weakest
- +200 bonus for characters at the weakest location to move and reinforce a stronger one

### V32 — Ability >= 4 Rule
**Files:** Rando DeployEvaluator.java, ChosenOne DeployEvaluator.java, Rando MoveEvaluator.java, ChosenOne MoveEvaluator.java, RandoConfig.java, ChosenOneConfig.java, Rando DeployPhasePlanner.java, ChosenOne DeployPhasePlanner.java
**Date:** March 19, 2026

**Config fix:**
- `ABILITY_THRESHOLD` changed from 2 → 4 in both RandoConfig and ChosenOneConfig

**Deploy rule:**
- Only checks SITE deployments (not systems)
- +150 for deploys that FIX an ability deficit (bringing site from < 4 to >= 4)
- -200 for solo deploy with ability < 4 AND no follow-up character in hand that could reach threshold
- -30 mild caution for solo deploy < 4 BUT follow-up character exists in hand
- -100 for deploys where total ability still < 4 even with existing friendlies

**Move rule:**
- -300 penalty for moving away from a site if remaining friendly ability drops below 4
- -500 if enemy also present at that site (no destiny + enemy = disaster)
- +50 bonus for a solo character with ability < 4 to MOVE AWAY and join allies (escape to consolidate)

**DeployPhasePlanner fix:**
- Line ~1299: Was estimating ability from power using `MIN(power, 4)` — completely wrong
- Now uses `inst.getAbilityContribution()` (the actual ability stat from the card)
- Fallback: `MIN(power, 3)` if ability wasn't set (conservative estimate)

---

## 3. ALL MODIFIED FILES (V30-V32)

```
# Rando Cal bot
src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/
├── RandoConfig.java                          # ABILITY_THRESHOLD 2→4
├── evaluators/DeployEvaluator.java           # V30 + V31 + V32 (largest changes)
├── evaluators/MoveEvaluator.java             # V31 post-flip consolidation + V32 ability check
└── strategy/DeployPhasePlanner.java          # V32 ability estimation fix

# The Chosen One bot (parallel changes)
src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/
├── ChosenOneConfig.java                      # ABILITY_THRESHOLD 2→4
├── evaluators/DeployEvaluator.java           # V30 + V31 + V32
├── evaluators/MoveEvaluator.java             # V31 + V32
└── strategy/DeployPhasePlanner.java          # V32 ability estimation fix

# Documentation
CLAUDE.md                                     # Project overview for Claude Code
HANDOFF.md                                    # This document
context.md                                    # Full changelog V21-V29.13
```

---

## 4. PENDING WORK & KNOWN ISSUES

### Not Yet Done
1. **BattleEvaluator ability check** — BattleEvaluator tracks relative power advantage but does NOT check if total ability >= 4 before recommending battle initiation. Should add: if our ability < 4 at a location, heavily penalize initiating battle there (we can't draw destiny).

2. **CombinedEvaluator additive scoring problem** — Root cause of many bugs. Soft penalties from one evaluator (-100) get overridden by generic bonuses from other evaluators (+150 for "contested location"). Potential fix: multiplicative scoring for hard constraints, or priority-based evaluation.

3. **Generic deck support** — Most tuning is for TDIGWATT and Hunt Down V. Other Dark Side objectives and ALL Light Side objectives use basic fallback heuristics.

4. **Multi-turn planning** — Bot evaluates one action at a time. No lookahead for sequences like "deploy character now → move next turn → battle the turn after."

5. **Battle destiny prediction** — Could factor in destiny draw probabilities from DeckOracle (knows what's left in Reserve Deck).

6. **Card synergy awareness** — Bot has no concept of combo exploitation (Amidala cancels Vader's game text), force drain amplification networks (Chiraneau), or deployment pipelines (AMSD → Piett+Executor → Gherant → Executor sites). These are currently handled as one-off hardcoded rules per card — a more general system would be better.

### Known Bugs
1. **V29.13 compile status unclear** — MoveEvaluator V29.13 changes were written but compile wasn't verified
2. **DeployPhasePlanner plans not enforced** — Plans are created but individual action evaluations don't check plan compliance. A planned "deploy Lando + backup together" can result in just Lando deploying solo if the evaluator scores him high enough independently.

### Steve's Priorities
- Aggressive deployment style — deploy as much as possible each turn
- Card combo awareness — understand WHY cards are deployed together
- Matching pilot+ship ALWAYS deployed together
- Ability >= 4 at every site — non-negotiable
- Objective-aware location priority — deploy to locations that matter for your objective
- Pre-flip: spread for presence. Post-flip: consolidate for defense.

---

## 5. KEY TECHNICAL NOTES FOR DEVELOPMENT

### API Patterns Used in Evaluators
```java
// Get game objects from context
SwccgGame game = context.getGame();
GameState gameState = context.getGameState();
String playerId = context.getPlayerId();
String opponentId = game.getOpponent(playerId);

// Get objective analyzer
ObjectiveAnalyzer analyzer = context.getObjectiveAnalyzer();
analyzer.isFlipped();
analyzer.getFlipConditionLocationFragments();  // Returns Set<String>
analyzer.needsBespinSystemPresence();
analyzer.isFlipBackProtectionLocation(locationTitle);

// Get cards in hand
List<PhysicalCard> hand = gameState.getHand(playerId);

// Get cards at a location
List<PhysicalCard> cardsHere = gameState.getCardsAtLocation(location);

// Get all locations on table
Collection<PhysicalCard> locations = gameState.getTopLocations();

// Check card properties
card.getBlueprint().getCardCategory()     // CHARACTER, STARSHIP, VEHICLE, LOCATION, etc.
card.getBlueprint().getCardSubtype()      // SITE, SYSTEM, CAPITAL, STARFIGHTER, etc.
card.getBlueprint().getPower()            // Float
card.getBlueprint().getAbility()          // Float
card.getBlueprint().hasAbilityAttribute() // boolean
card.getBlueprint().getMatchingStarshipFilter()  // Filter or null
card.getTitle()                           // String
card.getOwner()                           // String (player ID)
card.isPilotOf()                          // boolean
card.getAttachedTo()                      // PhysicalCard (ship if piloting)

// Get power at location
float power = game.getModifiersQuerying().getTotalPowerAtLocation(
    gameState, location, playerId, false, false);

// Score an action
action.addReasoning("V32 explanation text", 150.0f);  // positive = good, negative = bad
```

### Rando vs Chosen One Differences
The two bots have identical logic but live in separate packages. When modifying code:
- Rando: `com.gempukku.swccgo.ai.models.rando.*`
- Chosen One: `com.gempukku.swccgo.ai.models.chosenone.*`
- Always change BOTH when adding new rules
- Watch for import differences: `RandoConfig` vs `ChosenOneConfig`, `rando.strategy.ObjectiveAnalyzer` vs `chosenone.strategy.ObjectiveAnalyzer`

### Score Magnitudes Reference
| Range | Meaning | Examples |
|-------|---------|---------|
| +1000 to +1500 | Critical engine action | Matching pilot+ship both in hand |
| +500 to +800 | Must-do strategic action | Bespin system deploy turn 1, Executor with Bespin on table |
| +200 to +400 | High priority | Reinforce hold location post-flip, Lando with backup |
| +100 to +150 | Good tactical | Ability fix at site, Gherant deploy, spy blocking drains |
| +15 to +50 | Small preference | Icon bonuses, generic starship deploy, take-off |
| -30 to -80 | Mild discouragement | Suboptimal but survivable |
| -100 to -200 | Strong penalty | Ability < 4 solo deploy, spreading thin post-flip |
| -300 to -500 | Severe penalty | Stranding ally, AMSD soft block, ability danger on move |
| -9999 | Hard block | Executor without Bespin on table |

### Replay Format
Replay files in `replays/` are **zlib-compressed** (NOT gzip) XML:
```python
import zlib
data = zlib.decompress(open('replay.bin', 'rb').read())
# XML with <ge> elements: type="M" (messages), type="GS" (game state), type="D" (decisions)
```

---

## 6. HOW TO CONTINUE DEVELOPMENT

1. **Compile:** `cd src && mvn clean package -DskipTests`
2. **Test:** Run 20-game bot tournaments (Rando vs Rando) and analyze replays
3. **Add new rules:** Follow the V## pattern, add to both bots, include LOG.warn() for debugging
4. **Update context.md:** After adding rules, append to the version history
5. **Key next steps:** BattleEvaluator ability check, CombinedEvaluator scoring redesign, Light Side support
