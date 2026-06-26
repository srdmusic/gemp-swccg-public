# Rando Cal AI — Complete Change Log & Context

**Project:** GEMP-SWCCG (online Star Wars CCG platform)
**AI Bot:** Rando Cal (autonomous AI opponent)
**Version Range:** V21 through V29.13
**Last Updated:** March 14, 2026

---

## Architecture Overview

Rando Cal's decision-making is spread across several evaluator and strategy files. Each evaluator scores possible actions with floating-point values. The CombinedEvaluator aggregates scores and picks the highest. A `BAD_ACTION_THRESHOLD` of -100.0f causes the bot to pass instead of taking a terrible action.

### Key Files

| File | Role |
|------|------|
| `RandoCalAi.java` | Main entry point, orchestrates phases, auto-concede logic |
| `CombinedEvaluator.java` | Aggregates scores from all evaluators, picks best action |
| `ActionTextEvaluator.java` | Evaluates actions by parsing their display text (card names, keywords) |
| `DeployEvaluator.java` | Scores deploy actions (characters, ships, locations, weapons) |
| `MoveEvaluator.java` | Scores movement actions (character relocation between locations) |
| `CardSelectionEvaluator.java` | Scores card choices (deploy targets, location picks, forfeit order) |
| `BattleEvaluator.java` | Evaluates whether to initiate or avoid battles |
| `DrawEvaluator.java` | Evaluates Force draw/activation decisions |
| `PassEvaluator.java` | Evaluates passing vs acting |
| `ObjectiveAnalyzer.java` | Parses the active objective to determine deck strategy (Hunt Down, TDIGWATT, ISB Ops) |
| `DeckOracle.java` | Tracks what's in reserve/force/lost piles for informed decisions |
| `ShieldStrategy.java` | Manages defensive shield deployment pacing |
| `DeployPhasePlanner.java` | Plans multi-card deploy sequences |
| `OpponentDeckTracker.java` | Tracks opponent's revealed cards and destiny values |

### Supported Deck Archetypes

1. **TDIGWATT** (That's It, The Rebels Are There / Dark Deal / Cloud City Occupation) — Dark Side control deck centered on Bespin system, Executor, Piett, Cloud City sites, Lando/Lobot
2. **Hunt Down And Destroy The Jedi (V)** — Dark Side aggro deck centered on Vader hunting Luke with lightsabers
3. **ISB Operations** — Dark Side drain/control deck
4. **Generic** — Fallback heuristics for unrecognized objectives

---

## Version History (Chronological)

### V21 — Starting Effect Bans
**File:** CardSelectionEvaluator.java
**Why:** Certain effects are terrible as starting effects and should never be chosen.

---

### V22 — Core Objective Awareness
**Files:** ObjectiveAnalyzer.java, CardSelectionEvaluator.java, BattleEvaluator.java

The foundational version that gave Rando awareness of its own objective card.

- **V22: Objective Location Bonus** — Locations matching the objective get deploy priority
- **V22: Strategic Must-Fight Override** — Sometimes Rando MUST fight even at a disadvantage (e.g., to prevent opponent from occupying a critical location)
- **V22.2: Flip-Back Protection** — After flipping an objective, protect the conditions that keep it flipped. Parses the back side text to identify which locations must stay occupied
- **V22.3: Maintenance Cost Satisfaction** — When a maintenance card's upkeep is due, ALWAYS pay rather than sacrifice. The card was deployed for a reason
  - Pay maintenance: +200, Sacrifice: -300
- **V22.4: Location-Specific Battle Evaluation** — Evaluate battles based on actual power at the specific location, not globally
- **V22.5: Pre-Flip Consolidation** — Don't leave characters alone to die. If a lone character is badly outgunned, move them to join allies. Also: Alert My Star Destroyer ship deployment priority (+300 when no ship at Bespin)
  - Bespin/Cloud City presence requirement tracking for TDIGWATT
- **V22.6: Deck Oracle & Pull Priority** — Universal location priority for objective pulls. Failed pull avoidance using DeckOracle to check if targets exist in reserve before searching
- **V22.7: Cloud City Occupation Guard** — Cloud City Occupation self-cancels if we don't occupy Bespin system. Block deployment until Bespin is occupied
  - Block penalty: -800, Engine boost after Bespin occupied: +300
- **V22.7: AMSD Safety Net** — Basic guard against wasting AMSD

---

### V23 — Bespin System & Empty Pile Guard
**Files:** ActionTextEvaluator.java, DeployEvaluator.java

- **V23: Bespin System Early Deploy Priority** — For TDIGWATT, Bespin is THE foundation. Deploy it turn 1 at +800 priority, turns 2-3 at +400
- **V23: Empty Pile Guard** — Block interrupts that search empty piles. Sith Fury on turn 1 wastes 4 Force searching an empty Lost Pile
  - Empty pile: -300, Low pile (≤2 cards): -100

---

### V24 — AMSD Engine, Cloud City Strategy, Weapon Combos
**Files:** All evaluators, DeployPhasePlanner.java

The major TDIGWATT refinement version.

- **V24: AMSD Bespin Gate** — AMSD needs Bespin on table to have a deploy target. Hard block (-9999) if Bespin missing
- **V24: Mega Location Priority** — Locations are the foundation of everything. Turns 1-3: +200, Turn 4+: +50
- **V24: TDIGWATT Exhausted Search Guard** — Stop searching when all targets already pulled (-400)
- **V24.1: Card-Specific Pull Preferences** — Specific cards get pull priority for TDIGWATT
- **V24.2: Force Drain Modifier** — Always accept +1 force drain responses (+80)
- **V24.2B: Lando/Lobot Pull Priority** — Key TDIGWATT pieces get pull preference
- **V24.2D: Lando Movement Strategy** — Lando moves toward Cloud City sites where he's needed
- **V24.2E: Undercover Spy Deploy Priority** — U-3PO and Keder deploy to block opponent, NEVER at Cloud City (+100)
- **V24.3A: Dr. Evazan Weapon Combo Deploy** — Evazan converts weapon "hits" into "lost". Boost when weapon partners are in play (+150/+100)
- **V24.3B: Dr. Evazan Deploy Location Preference** — Deploy Evazan to locations with weapon characters
- **V24.3C: Dr. Evazan Movement Preference** — Move Evazan toward weapon characters
- **V24.4: Locations First** — Locations MUST deploy before activating effects like AMSD. Penalty for non-location actions when location in hand: -800
- **V24.5: Reserve Force for Maintenance** — Don't deploy if it leaves no Force for existing maintenance cards
  - Insufficient force: -400, Tight force: -100
- **V24.5: No Randomness** — CombinedEvaluator uses deterministic passing when all actions are bad
- **V24.6A: Executor Deploy Priority** — Executor is THE key ship. Must come out turns 1-2. Bespin must be on table first
  - Hard block (no Bespin): -9999, Turns 1-2 with Bespin: +600, Turn 3+: +350
- **V24.6B: I'm Sorry Location Pull** — Deploy interior CC sites from reserve every turn until exhausted
  - Sites in reserve: +250, All pulled: -300
- **V24.7: Opponent Deck Intel** — Scan destiny values of opponent's revealed cards for battle predictions
- **V24.9: CC Site Unoccupied Preference** — Move to unoccupied Cloud City sites to escape spy-blocked locations (+150)
- **V24.9: Masterful Move Guard** — Don't search for Ghhhk too early. No characters on table: -500, Turns 1-2: -300
- **V24.9: Piett Deploy Priority** — Piett is THE matching Executor pilot (+200)
- **V24.10: AMSD — Piett + Executor Only** — AMSD should ONLY fire with Piett as pilot AND Executor in reserve. Wrong pilot: -9999. Early turns with correct combo: +1500
- **V24.10: Save Piett for AMSD** — Block manual Piett deploy if AMSD is on table and Executor in reserve (-9999)
- **V24.10: Dig for Piett** — DrawEvaluator boosts Force pile digging when Piett is buried
- **V24.10: Lando Deploy Location** — Prefer Dining Room for Lando deployment
- **V24.10: Executor Must Deploy to Bespin** — Hard location constraint
- **V24.10: CC Site Selection — Context-Aware** — Choose CC sites based on game state
- **V24.11: AMSD Routing** — Route AMSD decisions through proper evaluation before target selection
- **V24.13: Lando Alone Detection** — Detect when Lando is alone and needs support, steer movement accordingly
- **V24.14B: Weapon Characters to Space — Movement Penalty** — Don't move weapon characters to space locations where weapons don't work
- **V24.15: Never Force Drain at 0** — Draining for 0 does nothing but opens us to traps (-9999)
- **V24.15: Avoid Deploying Characters to 0-Drain Locations** — Characters at zero-drain sites contribute nothing

---

### V25 — Hunt Down V, Battle Initiation, Pilot Protection
**Files:** MoveEvaluator.java, ActionTextEvaluator.java, CardSelectionEvaluator.java, DeployEvaluator.java, RandoCalAi.java, ObjectiveAnalyzer.java

The version that added Hunt Down V support and fixed battle initiation.

- **V25: Never Move a Pilot Off Their Ship** — Pilots aboard ships should NEVER shuttle off. Removing the pilot unpilots the ship. In testing, Piett shuttled off Executor, got killed alone, and Rando lost 16 Force
  - Penalty: -500
- **V25: Hunt Down V — Vader Castle Deploy Action** — Deploying Vader from Reserve via Castle is THE most important Hunt Down action
  - Not enough Force: -500, Enough Force: +550
- **V25: Hunt Down V — Lightsaber Deploy Priority** — Arm Vader with his lightsaber
- **V25: Hunt Down V — Vader Priority Deployment** — Vader must be on table for the deck to function
- **V25: Initiate Battle** — Battle initiation was previously unhandled (scored 0.0f), meaning Rando NEVER chose to fight. Now evaluates power differential
  - Crushing advantage (diff ≥ 8): +200
  - Strong advantage (diff ≥ 5): +120
  - Marginal advantage (diff ≥ 2): +60
  - Even battle: +20
  - Unfavorable: -60 to -250
  - Suicidal (opponent 2x power): -500
- **V25: Auto-Concede** — Concede when losing by 20+ in Lost Pile differential
- **V25: Cloud City Ability-Based Spread** — TDIGWATT spreads characters by ability across CC sites
- **V25: ISB Operations Awareness** — ObjectiveAnalyzer detects ISB Operations objective
- **V25: Hard Block Second Weapon** — Don't deploy a second weapon on a character who already has one

---

### V26 — Dining Room Lando Deploy
**File:** ActionTextEvaluator.java

- **V26: Dining Room — Deploy Lando** — Dining Room's game text deploys Lando from Reserve. Check if Lando would be alone (suicide) or has backup
  - Friendlies present: +150, Lando alone: -30

---

### V27 — Buddy Protection, Force Reservation, Maintenance
**Files:** MoveEvaluator.java, BattleEvaluator.java, DrawEvaluator.java, PassEvaluator.java

The defensive awareness version.

- **V27: Buddy Protection — Never Leave Vulnerable Ally Solo** — Moving a character away can leave their buddy alone and vulnerable. Check if removing this character would leave any remaining ally below thresholds (power < 6 AND ability < 4)
  - Base penalty: -150
  - Enemy present: -250
  - Enemy overpowers ally: -400
- **V27: Maintenance Force Conservation** — Penalize non-critical moves when Force is needed for maintenance payment (-80)
- **V27: Contested Location Force Reservation** — Keep Force available when locations are contested
- **V27.1: Draw Their Fire — Force Reservation** — Reserve 1 Force for DTF interrupt tax when Draw Their Fire is on the table
- **V27.1: Battle Interrupt Force Reservation** — Reserve Force for battle interrupts (Ghhhk, Houjix, etc.)

---

### V29 — Major Overhaul: Force Management, Smart Deploy, Weapons
**Files:** All files

The largest version series, touching nearly every evaluator.

- **V29: Force Reserve Check for Moves** — Moving costs Force. Save Force for DTF interrupt tax, grabber shield activation, critical interrupts in hand
  - Critical interrupt in hand: -150
  - Normal low Force: -100
  - Mild reserve concern: -60
- **V29: Force Push — Battle Use Only** — Force Push battle mode is good (+80), Force Pile exchange mode is bad (-300)
- **V29: Smart Solo Deploy Check** — Characters with power < 6 deploying solo get penalized UNLESS a second character can deploy right after, or the solo deploy helps flip the objective
- **V29: Buddy-Seek Bonus** — Deploying a strong character (power ≥ 6) to a location with a vulnerable solo ally gets +200
- **V29: TDIGWATT Bespin-First Guard** — Executor MUST deploy before characters. Guard stays active until Bespin is occupied. Non-exempt cards: -9999
- **V29: Character Aboard Ships** — Logic for deploying characters aboard capital ships vs ground locations

---

### V29.1 — Shield Pacing
**File:** ShieldStrategy.java, ActionTextEvaluator.java

- **V29.1: Shield Pacing** — Don't burn all 4 shield slots immediately. Play 2 on turn 1, then wait to see what opponent runs before committing remaining slots (-40 at pacing cap)

---

### V29.2 — Lando/Lobot Deploy Fix
**File:** DeployEvaluator.java

- **V29.2: Lando/Lobot Deploy Priority Fix** — Check BOTH card title AND action text for "lando"/"lobot" to catch all deploy paths

---

### V29.3 — Blueprint Card Type Detection
**File:** CardSelectionEvaluator.java

- **V29.3: Blueprint-Based Card Type Detection** — Use card blueprints to reliably detect card types instead of fragile text matching

---

### V29.4 — Force Pile Cataloging
**File:** DeckOracle.java

- **V29.4: Force Pile Catalog Scanning** — DeckOracle now scans the Force pile to know what cards are buried there (helps find Piett, etc.)

---

### V29.5 — General Buddy System
**File:** CardSelectionEvaluator.java

- **V29.5: General Buddy System** — When deploying characters, prefer locations where we already have characters. Reinforcing is better than scattering

---

### V29.6 — Hand Bloat, Battleground Bonus, Blaster Rack
**Files:** DeployEvaluator.java, ActionTextEvaluator.java, PassEvaluator.java, CardSelectionEvaluator.java

- **V29.6: Hand Bloat — Deploy Aggressively** — If Rando has 15+ cards in hand, boost all deploy actions. Cards in hand do nothing; cards on the table drain/battle/occupy
  - Base: +50 at 15 cards, +20 per additional card
- **V29.6: Battleground Bonus** — Prefer deploying locations that are battlegrounds
- **V29.6: Dining Room Lando Fix** — Check specific CC site for Lando deploys via Dining Room
- **V29.6: Blaster Rack — Only Rack to Save Weapons** — Racking weapons outside of battle damage resolution strips characters of weapons before they can fire
  - During battle damage: +80, Proactive racking: -500

---

### V29.7 — Weapon Hunter, Docking Bay Strategy, ISB Operations, Reserve Validation
**Files:** MoveEvaluator.java, ActionTextEvaluator.java, CardSelectionEvaluator.java, BattleEvaluator.java, DeckOracle.java

The combat effectiveness version.

- **V29.7: Weapon Hunter — Armed Characters Seek Battle** — Vader with lightsaber (or any armed high-power character) alone at an uncontested location should move to engage opponents. Weapon-equipped characters are worth far more than their base power
  - Base attack: +60
  - Power advantage ≥ 6: +40 additional
  - Power advantage ≥ 3: +20 additional
  - Luke as Hunt Down target: +150
- **V29.7: Weapon Combat Awareness** — BattleEvaluator accounts for weapon damage output when evaluating battle outcomes
- **V29.7: Vader's Castle Retreat Penalty** — Mustafar has 0 drain value. Don't teleport Vader back there when he's draining at a good location (-300)
- **V29.7: Deploy Docking Bay — Smart Strategy** — Docking bays are SHARED. Don't deploy more when we have empty ones
  - Already have empty bay: -200, First docking bay: +200
- **V29.7: We Must Accelerate Our Plans** — Only use for deploying Blockade Flagship site. Effect/interrupt pulls are wasteful (-400)
- **V29.7: Universal Reserve Deck Pull Validation** — Check DeckOracle before searching reserve for specific cards. Covers: Crush The Rebellion, IAYF, You Are Beaten, Blast Points, Hunt Down, Imperial Command, Endor Shield, Visage, Kir Kanos
- **V29.7: ISB Operations Deployment Strategy** — Enhanced ISB Ops deployment with ability-based character scoring
- **V29.7: Ability-Based Character Scoring** — Score characters by ability value, not just power
- **V29.7: Battleground Preference for Character Deployment** — Characters prefer battleground locations
- **V29.7: Refresh Flip Status** — ObjectiveAnalyzer can refresh flip status without full re-analysis
- **V29.7: Pull First Rule** — Detect RETURN-TO-HAND (bouncing own card, BAD) vs RETRIEVE from deck (GOOD)
  - Bounce from table: -300, Pull from reserve: +250

---

### V29.8 — IAYF Vader Check, Sense Block, Zone-Aware Force Loss
**Files:** ActionTextEvaluator.java, CardSelectionEvaluator.java

- **V29.8: IAYF Vader-On-Table Check** — I Am Your Father can deploy Vader's Lightsaber from reserve or lost pile. Vader MUST be on table first (-500)
- **V29.8: Sense & Uncertain — Block Redraw Hand** — Sense/Uncertain has two functions. "Redraw hand" helps opponent. NEVER use that function (-600)
- **V29.8: Zone-Aware Force Loss** — Track which zone cards are lost from (hand vs table vs reserve) for smarter forfeit decisions

---

### V29.9 — Hunt Down Battle Aggression, Rebel Barrier, IHYN, Lightsaber Priority
**Files:** MoveEvaluator.java, ActionTextEvaluator.java, BattleEvaluator.java, DeployEvaluator.java

- **V29.9: Unarmed Vader Penalty** — Vader WITHOUT lightsaber should NOT be sent to fight. He needs to get armed first
  - Lightsaber in hand but not equipped: -250
  - No weapon at all: -100
- **V29.9: Rebel Barrier Risk Assessment** — Factor in the risk that opponent plays Rebel Barrier to cancel an attack
- **V29.9: Hunt Down Vader Battle Aggressiveness** — Vader in Hunt Down gets extra battle aggression bonus
- **V29.9: I Have You Now — Play During Battle** — IHYN adds extra battle destiny draws. Devastating with Vader
  - Vader in battle: +300, Not in battle: -200
- **V29.9: Lightsaber Deploy Priority (Hunt Down V)** — Deploying lightsaber ON unarmed Vader is CRITICAL
  - Unarmed Vader: +400, Vader's Lightsaber specifically: +500
  - Already armed: -300
- **V29.9: Hunt Down Force Drain Priority** — Drains are extra valuable with Visage Of The Emperor on table (+30 to +40)

---

### V29.10 — Lightsaber Throw, Hatred Card
**File:** ActionTextEvaluator.java

- **V29.10: Lightsaber Throw** — After firing lightsaber, Vader can throw it for extra attrition. Throw MUST score LOWER than Fire (throw places saber in Lost Pile — if Rando throws first, he can never fire it)
  - Fire weapon: +300, Battle throw: +200, Non-battle throw: +150
- **V29.10: Hatred Card** — Stacking Hatred on opponent's character cancels their game text (removes attrition immunity!)
  - Deploy phase: +300, Battle phase: +250

---

### V29.11 — Lightsaber Targeting Refinement, Blaster Rack Fix
**Files:** DeployEvaluator.java, ActionTextEvaluator.java

- **V29.11: Lightsaber Targeting** — Refined targeting logic for different saber types (Vader's Lightsaber vs generic)
- **V29.11: Blaster Rack Fix** — Refined rack timing to only save weapons during battle damage resolution

---

### V29.12 — Hunt Down Deploy Grouping, Vader Must Hunt
**Files:** DeployEvaluator.java, MoveEvaluator.java, ActionTextEvaluator.java

- **V29.12: Hunt Down — Deploy Characters WITH Vader** — Characters should deploy at Vader's location to create overwhelming force. Scattered characters get picked off individually
  - Deploy WITH Vader: +250
  - High power (≥5) extra bonus: +50
  - Deploy AWAY from Vader: -150
- **V29.12: Hunt Down — Vader Must Leave Castle and Hunt** — Armed Vader at an uncontested location (like Castle) is wasting turns. He must go fight
  - Bonus to move armed Vader: +200
- **V29.12: Lightsaber Throw — Add Destiny to Attrition** — Refined lightsaber throw scoring

---

### V29.13 — Move Grouping, Deploy Efficiency, Force Drain Awareness (LATEST)
**Files:** MoveEvaluator.java, DeployEvaluator.java

The latest version, focused on fixing the "scatter" problem and improving deploy/move efficiency.

- **V29.13: Hunt Down — Move Characters With Vader (Grouping)** — Mirror of V29.12 deploy grouping but for the MOVE phase. Prevents characters from scattering (e.g., Vader moves to Cantina while brothers move FROM Cantina to Mos Eisley)
  - **Vader moving TOWARD allies:** +200 (+250 if ally power ≥ 8)
  - **Vader moving AWAY from allies (no opponents):** -200
  - **Vader moving toward opponents (hunting):** no penalty — this is good
  - **Non-Vader WITH Vader, moving away:** -250
  - **Non-Vader NOT with Vader, moving TOWARD Vader:** +250
  - **Non-Vader NOT with Vader, moving elsewhere:** -100
- **V29.13: Deploy Directly to Opponents** — Rando was deploying to empty locations then wasting Force to move. Deploy directly where opponents are
  - Deploy to opponents: +100 (+50 if opponent power ≥ 6)
  - Deploy to empty location when opponents elsewhere: -75
- **V29.13: Force Reservation (Deploy-Aggressive)** — Philosophy: ALWAYS deploy as much as possible. Board presence wins games. Maintenance-free cards: zero penalty. Maintenance cards: small tiebreaker (-40). DTF/grabber reserve: tiny soft penalty (-30)
- **V29.13: Force Drain Modifier Check** — Avoid moving to locations with bad drain modifiers
  - Moving to worse drain location: -80
  - Moving to drain ≤ 0: -120
  - Moving to better drain (≥2): +40
  - Moving to much better drain (≥3): +80
- **V29.13: Safe Default for Activate Force** — Never skip activation due to exceptions (+50)
- **V29.13: Reduced Reserve-for-Destiny** — Reduced from 2-3 to 1. Having Force to deploy is MORE important than saving cards for destiny draws
- **V29.13: No Battle Damage Remaining — Optional Forfeit** — When no battle damage remains to satisfy, don't forfeit unnecessarily

---

## Score Reference Guide

### Critical Thresholds
- **BAD_ACTION_THRESHOLD:** -100.0f (below this, Rando passes instead of acting)
- **Hard Block:** -9999.0f (absolutely never do this)

### Score Magnitudes
- **+500 to +1500:** Critical engine actions (AMSD with Piett+Executor early, Vader's Lightsaber deploy)
- **+200 to +400:** High-priority strategic actions (location deploys, weapon arming, buddy-seek, grouping)
- **+50 to +150:** Good tactical bonuses (battleground preference, drain modifiers, pull priorities)
- **-30 to -80:** Mild discouragement (suboptimal but not terrible)
- **-100 to -300:** Strong penalties (scattering, bad battles, wasted searches)
- **-400 to -600:** Severe penalties (leaving ally to die, redraw hand, proactive weapon racking)
- **-9999:** Absolute block (would break game engine or self-cancel objective)

---

### V30 — Universal Matching Pilot + Starship Deploy Rule (March 2026)
**Files:** DeployEvaluator.java (both bots)

Replaced Piett-specific hard block with universal rule for ALL matching pilot/ship combos. Uses `card.getBlueprint().getMatchingStarshipFilter()` to detect matching relationships.

- Both in hand: +1000 (deploy together NOW) + another +1000 if to objective system
- Ship in play: +300 (deploy pilot aboard)
- Ship in reserve + AMSD on table: -500 (prefer AMSD, allow fallback)
- Reverse rule for starship deployment: scan hand for matching pilot

---

### V31 — Pre-Flip vs Post-Flip Objective Strategy (March 19, 2026)
**Files:** DeployEvaluator.java, MoveEvaluator.java (both bots)

**Deploy pre-flip:** +250 deploy to unoccupied objective location, -50 stacking on occupied when unoccupied exist
**Deploy post-flip:** +200 reinforce 2 strongest hold locations, -100 spreading to 3rd+ location
**Move post-flip:** +200 move from weakest 3rd objective location to reinforce stronger positions

---

### V32 — Ability >= 4 Enforcement (March 19, 2026)
**Files:** DeployEvaluator.java, MoveEvaluator.java, Config.java, DeployPhasePlanner.java (both bots)

**Config:** ABILITY_THRESHOLD 2→4
**Deploy:** +150 fixing ability deficit, -200 solo ability<4 with no follow-up, -30 if follow-up exists, -100 total still <4
**Move:** -300 to -500 for moves that drop ability below 4, +50 for solo escape to join allies
**Planner fix:** ability estimation now uses `getAbilityContribution()` instead of `MIN(power, 4)`

---

## Score Reference Guide

### Critical Thresholds
- **BAD_ACTION_THRESHOLD:** -100.0f (below this, Rando passes instead of acting)
- **Hard Block:** -9999.0f (absolutely never do this)

### Score Magnitudes
- **+1000 to +1500:** Critical engine actions (matching pilot+ship both in hand, objective system deploy)
- **+500 to +800:** Must-do strategic (Bespin system turn 1, Executor + Bespin on table)
- **+200 to +400:** High-priority strategic actions (location deploys, reinforce hold locs, weapon arming, buddy-seek)
- **+50 to +150:** Good tactical bonuses (ability fix, battleground preference, drain modifiers, pull priorities)
- **-30 to -80:** Mild discouragement (suboptimal but not terrible)
- **-100 to -200:** Strong penalties (ability < 4 solo, scattering post-flip, bad battles)
- **-300 to -500:** Severe penalties (ability danger on move, leaving ally to die, AMSD soft block)
- **-9999:** Absolute block (Executor without Bespin on table)

---

## Known Issues & Future Work

1. **BattleEvaluator needs ability check** — Does not verify ability >= 4 before recommending battle initiation
2. **CombinedEvaluator additive scoring** — Soft penalties get overridden by generic bonuses from other evaluators; consider multiplicative or priority-based scoring
3. **DeployPhasePlanner plans not enforced** — Plans created but individual evaluations don't check plan compliance
4. **Generic deck support is limited** — Most tuning has been for TDIGWATT and Hunt Down V
5. **Light Side support** — Rando primarily plays Dark Side. Light Side heuristics are minimal
6. **Battle destiny prediction** — Could factor in destiny draw probabilities from DeckOracle
7. **Multi-turn planning** — Rando evaluates one action at a time. No lookahead
8. **Card synergy system** — Currently one-off hardcoded rules per card; needs general combo-awareness system
