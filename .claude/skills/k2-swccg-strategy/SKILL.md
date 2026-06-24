---
name: k2-swccg-strategy
description: "K-2's SWCCG gameplay strategy skill. Use this skill whenever K-2 (Claude) is playing a live SWCCG game via the GEMP MCP tools. Contains Rando's proven AI logic as the mechanical foundation, Steve's strategic wisdom from 232 replays, and K-2's own learned instincts. This is K-2's brain for making in-game decisions."
---

# K-2 SWCCG Strategy Guide

## DECISION FRAMEWORK (Read This First)

For every non-auto-pass decision during gameplay, follow this process:

1. **Refer to Rando's Logic section FIRST** (bottom of this document). Rando knows the game mechanics cold — ability thresholds, destiny predictions, card values, deploy priorities. Understand what Rando would choose to do.

2. **Then refer to K-2's Strategy section** (middle of this document). This contains Steve's higher-level strategic wisdom — Cantina shuttle drains, Skywalker retrieval, force economy, spy deployment, overflow damage awareness.

3. **Check for contradictions.** If Rando's logic and K-2's strategy agree, follow them confidently. If they disagree, weigh which is more likely to produce a winning result:
   - **Rando is usually right about:** Ability thresholds, battle math, destiny predictions, deploy order priorities, force reserve calculations, card-specific rules
   - **K-2 is usually right about:** Higher-level strategy (multi-turn plans), location combos (Cantina/Mos Eisley shuttle), when to be aggressive vs conservative, force economy (deploy everything, don't hoard)

4. **Log the outcome.** When you choose Rando's logic over your own instinct (or vice versa) and it produces a bad outcome, log it as an observation. Over time, we build a weight system for when Rando is right vs when K-2 is right. This data will be used to improve Rando's code.

---

# SECTION 1: K-2's Strategy (Steve's Wisdom + Game Experience)

Built from 232 analyzed replays of Steve (ASDF) playing on GEMP-SWCCG, plus lessons learned from K-2's own games.

## How to Use This Section

1. **Before a game**: Read the deck-specific playbook for the deck you're piloting
2. **During decisions**: Cross-reference with Rando's Logic below
3. **During battles**: Reference the battle doctrine
4. **Post-game**: Log observations for the weight system

## Core Strategic Principles (All Decks)

These apply regardless of which side or deck K-2 is playing:

### 1. Activate Aggressively Early
- Turn 1: Activate 100% of force generation
- Turns 2-3: Activate ~100%
- Turns 5+: Reserve 15-20% for battle interrupts
- Never leave force unactivated in early game — tempo is everything

### 2. Stack Then Spread
- Deploy heavy at one primary location first (ability >= 4 always)
- Expand to secondary locations only after primary is secure
- Never spread so thin that any location drops below ability 4

### 3. Fight Early, Fight Favorable
- First battle should happen by turn 3 or 4. I can sometimes happen sooner if you find an opportunity to over power an opponent in turn 1-2
- Only initiate battles with power advantage (+5 or more preferred)
- Steve wins 82% of battles he initiates (avg power differential +6.7)
- If you see that the opponent has left himself weak at a battle ground attack with every possible character, vehicle or starship you have so that you cause as much overflow damage to your opponent by making him loose force at the end of a battle.
- Never attack into a stronger board — drain instead

### 4. Force Drains Win Games
- Steve executes 2.4x more drains than opponents (5.3/game vs 2.2)
- 40% of games are won without any battles — pure drain pressure
- Maintain presence at 3-5 drain locations by mid-game
- Bespin system is a drain engine when playing with the objective This Deal Is Getting Worse all the time. (237 drains across replays)

### 5. Life Force = Reserve + Force Pile + Used Pile + Hand
- This is your total remaining life. When it hits 0, you LOSE.
- Hand counts as life force but NOT as reserve (can't activate from hand).
- Cards on Table are NOT life force — they're deployed assets.
- Lost Pile cards are PERMANENTLY gone — they reduce your total life force forever.
- To check who's winning: compare total life force (Reserve + Force Pile + Used Pile), NOT just reserve.
- A bad battle that sends 5+ cards to Lost Pile is catastrophic — it permanently shrinks your life.
- Force drains send cards from Reserve to Lost Pile — that's permanent damage.
- Forfeiting characters in battle sends them to Lost Pile — permanent life force loss.
- NEVER initiate a battle you might lose badly — the overflow damage to Lost Pile can be game-ending.

### 6. Ability >= 4 Is Non-Negotiable
- Must have total ability >= 4 at every battleground site you occupy
- Without this, you cannot draw battle destiny and lose every fight leaving your self open to overflow damage
- When evaluating deploys, always check: "Does this keep me at ability >= 4?"

---

## Light Side Playbooks

### DECK: Like My Father Before Me (Luke Saga / Luke Saga Tatooine)

**Objective**: Like My Father Before Me (with Anger, Fear, Aggression V)

**Starting Setup (100% consistent across 100 games)**:
- Anger, Fear, Aggression (V) + The Force Is Strong In My Family + Like My Father Before Me
- Starting location: Endor: Anakin's Funeral Pyre
- Starting Interrupt: The Rise Of Skywalker (deploys TFISMF + 2 effects)
- TFISMF choice (choose your adventure): Pick based on which Skywalker persona has MULTIPLE copies in the deck:
  - "I Have It" = powers up Luke personas (choose if deck has multiple Lukes)
  - "My Father Has It" = powers up Anakin personas (choose if deck has multiple Anakins)
  - "You Have That Power, Too" = powers up Rey AND makes her a Skywalker (choose if deck has multiple Reys)
  - For LUKE SAGA TATOOINE: choose "I Have It" (deck is Luke-heavy)
- Effects priority: Wokling (V) > I Must Be Allowed To Speak (V)

**Turn 1 Script (CRITICAL — follow exactly)**:
1. Deploy Tatooine locations FIRST: Mos Eisley, Lars' Moisture Farm (V), Cantina — force gen ramp is priority #1
2. Deploy Young Skywalker to a Tatooine battleground site (NOT Endor — Endor is NOT a battleground)
3. Use Gift Of The Mentor or similar puller to grab Luke's Lightsaber from Reserve Deck, attach to Young Skywalker
4. This sets up a big force drain at the Tatooine site
5. NEVER deploy characters to Endor: Anakin's Funeral Pyre — it has 0 drain icons, no battleground
6. Force generation ramps from ~6 to ~11 by turn 2

**Turn 2-3 Priorities**:
- Deploy Tatooine system for ship presence
- Deploy Han/Chewie/Falcon (V) or Wild Karrde to Tatooine system
- Expand to opponent sites and systems to pressure opponent's objective locations or stop them from openly draining
- Deploy Skywalkers retrieve force when they battle Leia, Luke and Anakin are sky walkers. When choosing "You also have it" for Skywalker saga, This makes Rey a Skywalker.  Fight battles with Skywalkers as often as possible to retrieve force.
- Deploy Yoda Master Of The Force is a helpful character because he Jedi's add one to force generation when on table, Jedi's are hard to kill and they can carry a lightsabers.

**Primary Drain Locations**: Cantina (138 drains), Mos Eisley (100), Lars' Moisture Farm (44) Mos Eisley give you the ability to move from Cantina to or from Los Eisley durning control phase.  This is a good drain combo.  Stack characters on either Mos Eisley or Cantina, during control phase you can move one character over to which ever location you do not currently occupy.  Drain at that location then drain again and the other location, Then move all of your character back together.  This keeps them safe by stacking but also lets your drain at two locations every turn.

**Key Card Combos**:
- Young Skywalker + Luke's Lightsaber (power combo, always together)
- Yarna d'al Gargan (V) (location puller — thins deck)
- Smoke Screen (172 plays — #1 battle interrupt)
- Help Me Obi-Wan Kenobi (V), Fall Of The Legend (V) — secondary battle interrupts
- Sorry About The Mess & Blaster Proficiency, Blaster Deflection — weapons/defense
- I Don't Like Sand — limits destiny draws to 1 each (massive battle swing)

**Movement Pattern**:
- Peak movement turns 3-4 (repositioning for battles)
- Young Skywalker is most-moved character
- Move characters from safe Tatooine sites TO contested Cloud City sites

**Win Condition**: Steady 2-force drains at 3+ Tatooine sites + battle victories at opponent location  = opponent's reserve deck empties

---

### DECK: The Hidden Path

**Objective**: The Hidden Path (with Anger, Fear, Aggression V)

**Starting Setup**:
- Anger, Fear, Aggression (V) + The Hidden Path
- Starting locations: Mapuzo: Mining Village, Mapuzo: Safehouse, Mapuzo: Underground Corridor
- Starting Interrupt: Heading For The Medical Frigate
- Effects: Fallen Order (core engine), Wokling (V), Sai'torr Kal Fas (V), Draw Their Fire

**Turn 1 and 2 Script**:
1. Deploy Fallen Order tokens massively (deck's core engine, 650 deploys in data)
2. Deploy Obi-Wan Kenobi, Jedi In Exile + Obi-Wan's Lightsaber to Mapuzo: Safehouse
3. Deploy Quinlan Vos and/or Kelleran Beq as secondary Jedi
4. Attach Jedi Holocron to characters
5. Move all available Jedi to Underground Corridor. This location lets you transport to any opponent battle ground or Jabiim site.  When two Jedi are on battle grounds outside Mapuzo the objective flips and you get bonuses for each Jedi

**Deploy Philosophy — EXTREME STACKING**:
- Mapuzo: Safehouse receives overwhelming character presence (1,258 deploys in data)
- Stack lightsabers on every Jedi: Obi-Wan's Lightsaber, Jedi Lightsaber (V), Ahsoka's Shoto

- Expand to Jabiim locations (Path Operations Center, Starship Hangar)

**Battle Behavior — Most Aggressive Deck**:
- Battles start TURN 1 (42 battles on turn 1 across 97 games)
- Fights at own locations AND opponent locations equally
- Top battle sites: Mapuzo Safehouse (56), Cloud City Dining Room (56)

**Movement — Heaviest of All Decks**:
- 1,073 moves recorded; massive turn 1-2 movement
- Characters shuttle through Mapuzo: Underground Corridor constantly
- Obi-Wan, Kelleran, Quinlan are the most-moved characters

**Force Drains**: Multi-location pressure at Underground Corridor (143), Jabiim Starship Hangar (108), Malachor Sith Temple Entrance (86)

---

## Dark Side Playbooks

### DECK: This Deal Is Getting Worse All The Time (V) (TDIGWATT / Dark Deal)

**Objective**: This Deal Is Getting Worse All The Time (V) + Knowledge And Defense (V) + I'm Sorry (V)

**Starting Setup**:
- Starting Interrupt: Slip Sliding Away (V) — deploys 1 CC battleground + effects
- Starting locations: Cloud City: Dining Room (V) (from objective) + Cloud City: Upper Walkway (from SSA)
- Effects from SSA: Alert My Star Destroyer! (V) > Endor Shield (V) > Fighters Straight Ahead

**Turn 1 Script (FULLY SCRIPTED — follow exactly)**:
1. Use TDIGWATT game text to pull Bespin (V) into hand (always Bespin first — force gen ramp is priority #1)
2. Play A Real Hero (Used Interrupt) to pull Lobot, Lando's Broker into hand
3. Use Endor Shield (V) to pull Admiral Piett into hand
4. Use Endor Shield (V) AGAIN to pull Admiral Chiraneau into hand (can use twice!)
5. Play defensive shields from Knowledge And Defense (V): Allegations Of Corruption, Secret Plans
6. Activate ALL 5 force
7. Deploy Bespin (V) system
8. Use I'm Sorry (V) to deploy Cloud City: Security Tower (V) from Reserve Deck (free CC site!)
9. Draw ALL remaining force into hand (empty force pile completely)
10. Force generation jumps from ~5 to ~9-10

**NOTE on AMSD (V) Bug**: Alert My Star Destroyer! sometimes fails to find Executor's matching pilot. If AMSD fails, deploy Executor manually from hand + Chiraneau as pilot separately. Steve proved this works — won his game deploying Executor + Chiraneau manually on Turn 2.

**Turn 2 Priorities** (Force gen ~10):
1. Use TDIGWATT to pull Cloud City Occupation (or Dark Deal if already have Occupation)
2. Activate ALL 10 force
3. Deploy Executor from hand to Bespin (V) — costs 7 force
4. Deploy Admiral Chiraneau on Executor as pilot
5. Use I'm Sorry (V) to deploy Cloud City: Carbonite Chamber from Reserve (another free CC site!)
6. Draw remaining force into hand

**Turn 3 — The Explosive Turn** (Force gen 17):
1. Use TDIGWATT to pull whatever you still need (Dark Deal, Occupation, etc.)
2. Activate ALL force
3. Force drain 2 at Bespin FIRST (Executor enables drain)
4. Deploy Lando from Reserve via Cloud City: Dining Room game text (Dining Room lets you pull Lando!)
5. Use Lando's game text to PLACE opponent's non-immune Effect in Used Pile (kill their Yarna, Wokling, etc.)
6. Mass deploy to Cloud City: Upper Walkway — Blizzard 4 + Mara Jade (passenger) + Lobot + Dr. Evazan & Ponda Baba + Admiral Piett (passenger)
7. Deploy Cloud City Occupation on Bespin
8. INITIATE BATTLE at Upper Walkway — you should have massive power advantage
9. Winning this battle FLIPS THE OBJECTIVE to Pray I Don't Alter It Any Further (V)
10. After battle: disembark Mara Jade, move Dr. E&PB to Dining Room for presence spread
11. Play Battle Order from stacked shields
12. Use Lando to draw from Reserve Deck

**Turn 4+ — The Drain Engine (CRITICAL PHASE ORDER)**:
Every turn from now on follows this EXACT sequence:
1. Activate ALL force
2. **Trigger Cloud City Occupation FIRST** — opponent loses 2-3 force (this MUST happen before drains)
3. **Lando Shuttle Move** — use Pray I Don't Alter It objective to move Lando to an adjacent CC site
4. **Trigger Dark Deal (V)** — if deployed, this adds force loss when draining
5. **NOW drain at ALL locations** — Carbonite Chamber, Bespin, Dining Room, Upper Walkway
6. Deploy more characters / draw remaining force into hand

**WHY THIS ORDER MATTERS**: Steve discovered that if you drain before triggering Dark Deal and CC Occupation, the phase-skip bug pushes you past your drain phase. Trigger effects FIRST, then drain. Also give a brief pause between drains at different locations — the server gets confused with rapid-fire drains.

**The Lando Shuttle Pattern (Core Engine — Non-Negotiable)**:
- Move Lando Calrissian, Vader's Broker between Cloud City sites every turn using Pray I Don't Alter It objective
- Pattern: Dining Room → Carbonite Chamber → back next turn → Security Tower → back
- This enables drains at the location Lando moves TO, plus drains at all other occupied locations
- Lando moving = 2+ drain locations from one safe character stack
- NEVER move Lando to a location where opponent has overwhelming force — Lando is the engine, protect him
- Lando is the most-moved character (24 recorded moves in 9 games)

**Deploy Philosophy — Aggressive Cloud City Control**:
- Turn 3: Mass deploy characters to Upper Walkway for the flip battle
- After flip: Spread 1+ characters per Cloud City site (maximize drain locations)
- Stack power on Executor at Bespin for space control (drain 2 every turn)
- Use Lando's effect removal to disrupt opponent's board (remove their Yarna, Wokling, etc.)
- Late game: Deploy bounty hunters (Boba Fett + Dr. E&PB) to opponent's weak sites for overflow kills

**Battle Behavior — Flip Battle Turn 3, Then Opportunistic**:
- First battle Turn 3 at Upper Walkway to flip the objective (this is scripted, not optional)
- After flip: Battle only with overwhelming advantage (+10 power or more)
- Look for opponent's weak locations — ships landed at sites with 0 power are free kills
- Lord Maul + Sniper & Dark Strike combo: Maul's lightsaber "hits" a character (forfeit→0), then Sniper makes them lost. Two-card assassination.
- Send bounty hunter death squads to opponent sites with weak characters

**Force Drains (Primary Win Condition)**:
- Bespin (V) system: drain 2 with Executor (35 drains in data)
- Cloud City: Carbonite Chamber: drain 1-2 (Lord Maul adds +1 when present)
- Cloud City: Dining Room (V): drain 1
- Cloud City: Upper Walkway: drain 1-2
- Cloud City: Security Tower (V): drain 1
- By turn 4-5: draining at 4-5 locations for 7+ drain + 2-3 CC Occupation = **9-10 force loss/turn**

**Key Card Combos**:
- AMSD (V) + Executor + Piett = the deploy engine (but deploy manually if AMSD bugs)
- Dining Room game text = pulls Lando from Reserve Deck (free deploy!)
- Lando Broker game text = removes opponent's non-immune Effects AND draws from Reserve
- Lando Broker + Lobot Broker = always deploy together for control phase movement
- Cloud City Occupation + Dark Deal (V) = force loss engine post-flip
- Lord Maul + Sniper & Dark Strike = character assassination combo
- Boba Fett + Dr. Evazan & Ponda Baba = overflow damage squad for weak locations
- No Escape (V) = retrieve lost characters from Lost Pile (recycle Dr. E&PB)
- Blizzard 4 = transport vehicle for Mara Jade + Piett as passengers to CC sites

**Objective Flip Strategy**: Flip on Turn 3, not later. Mass deploy to Upper Walkway, win the battle, flip immediately. The flipped objective enables the Lando shuttle pattern which is your entire win condition. Don't wait — flip fast, drain hard.

**Exploiting Opponent Mistakes**:
- If opponent lands a starship at a site (power 0), send bounty hunters for a free overflow kill
- Wild Karrde at Mos Eisley = Boba Fett + Dr. E&PB for 21 vs 0, game-ending overflow damage
- If opponent deploys to your CC sites, battle them with your full stack — you have home field advantage

---

### DECK: Hunt Down And Destroy The Jedi (V)

**Starting Setup**:
- Prepared Defenses deploying: Crush The Rebellion, I Am Your Father (V), There Are Many Hunting You Now, Visage Of The Emperor (V)

**Turn 1 Script**:
1. Pull I Have You Now + Darth Vader's Lightsaber (V) from Reserve Deck
2. Deploy Darth Vader, Emperor's Enforcer + lightsaber immediately
3. Deploy Blaster Rack

**Strategy — Pure Vader Aggression**:
- Vader deploys Turn 1 with lightsaber, moves to enemy locations immediately
- Battles from Turn 2 onward, every turn
- Reinforcements: Ninth Sister, Eighth Brother, The Grand Inquisitor, Mara Jade With Lightsaber
- Lord Vader deploys Turn 5 with Tarkin's Bounty for additional pressure
- This deck fights constantly — no waiting, no draining, just violence

---

## Battle Doctrine

### Spy Deployment
- **Jyn Erso** should ALWAYS be deployed as an "undercover spy" to an opponent's battleground site where opponent has force drained.  An "undercover spy" block force drains.
- An "undercover spy" block opponent force drains at the location they occupy
- Deploy Jyn to whichever Cloud City site Rando is draining from most (Dining Room, Carbonite Chamber)
- This is a defensive priority — reducing Rando's drain pressure is critical

### When to Initiate Battle
- Power advantage >= +5 at the location
- Total ability >= 4 at the location (mandatory for battle destiny)
- **CHECK ABILITY COUNT** — ability determines how many battle destiny draws each side gets. Even power does NOT mean even battle if opponent has more ability (draws more destiny).
- Example: 2 Jedi (ability ~12) draw 1 destiny. 3 bounty hunters (ability ~16) draw 4 destiny. Even at equal power, the bounty hunters will crush you with destiny draws.
- You have battle interrupts in hand (Smoke Screen, I Don't Like Sand, etc.)
- Opponent has key characters you can eliminate
- **NEVER battle at even power unless you have equal or more ability (destiny draws)**

### When NOT to Battle
- Power disadvantage at the location
- Ability < 4 (no battle destiny = you lose)
- No interrupts in hand and opponent likely has some
- You're winning on drains alone (don't risk what you don't need to)

### Battle Interrupt Priority
1. Smoke Screen (most played — 172 times)
2. I Don't Like Sand (limits destiny to 1 each — huge swing)
3. Help Me Obi-Wan Kenobi (V)
4. Fall Of The Legend (V)
5. Sorry About The Mess & Blaster Proficiency
6. Blaster Deflection

### Attrition Sponges (Expendable Characters)
Steve sacrifices these to absorb battle losses while protecting key characters:
- Lando Calrissian, Scoundrel (67 forfeitures)
- Rey With Lightsaber (52)
- Sabine, Padawan Learner (46)
- Young Skywalker (35)

---

## Decision Heuristics

### CRITICAL: Never Search Reserve Deck for a Card Already in Hand
- Before using ANY puller effect (Yarna, I Must Be Allowed To Speak, Gift Of The Mentor, LMFBM, etc.), CHECK YOUR HAND FIRST
- If the card you want is already in your hand, the search will FAIL
- When a search fails, your opponent gets to VIEW YOUR ENTIRE RESERVE DECK
- This gives them complete intelligence on every card you haven't drawn yet — devastating advantage
- ALWAYS verify the target card is NOT in your hand before searching

### Deploy Phase
1. Can I deploy a location that increases force generation? -> DO IT
2. Can I deploy a character with lightsaber to a key site? -> DO IT
3. Does this deploy maintain ability >= 4 at all my sites? -> REQUIRED
4. Am I spreading too thin? -> Stack primary location first
5. Is there a scripted combo available (Piett->Gherant, AMSD->Executor)? -> FOLLOW THE SCRIPT

### Move Phase
1. Can I move to initiate a favorable battle? -> DO IT
2. Can I move to a location where I'll force drain? -> DO IT
3. Does moving leave my origin below ability 4? -> DON'T MOVE
4. Is this the Lando Shuttle Pattern? -> MOVE (TDIGWATT only)

### Battle Phase
1. Do I have power advantage >= +5? -> INITIATE
2. Do I have ability >= 4? -> REQUIRED to fight
3. Do I have battle interrupts? -> FACTOR into decision
4. Is opponent's key character here and vulnerable? -> INITIATE

### Control Phase Movement Shuffle (UNIVERSAL PRINCIPLE)
Any card or location that allows movement during Control phase enables a drain shuttle:
1. Stack characters safely at Location A
2. Control phase: move 1 character to adjacent Location B (using Control phase movement ability)
3. Force drain at Location B
4. Force drain at Location A
5. Move everyone back together for safety
This gives 2+ drains per turn from one safe stack. ALWAYS look for this pattern.

**Luke Saga example**: Mos Eisley game text allows Control phase movement to/from Cantina
**Dark Deal example**: Lando Calrissian, Vader's Broker + Lobot enable Control phase movement between Cloud City sites via the TDIGWATT objective. This is the Lando Shuttle Pattern — it satisfies occupy conditions AND enables multi-site drains.

**Rule**: Any time you see a card or location that says "move during Control phase" — exploit it for the drain shuffle.

### Skywalker Force Retrieval (Critical Combat Mechanic)
- Skywalkers (Luke, Leia, Anakin, Rey if "You Have That Power Too" chosen) retrieve 1 force each when INITIATING a battle
- Leia's own game text ALSO retrieves 1 force when initiating battles (stacks with Skywalker retrieval)
- Luke + Leia initiating a battle = 2 force retrieved before battle even resolves
- Luke + Leia + Rey = 3 force retrieved per battle
- This means: ALWAYS battle when Skywalkers are present, even if the battle itself is a wash — retrieval alone is worth it
- Deploy Luke and Leia together whenever possible for the retrieval combo

### GEMP Phase-Skip Bug and Drain Timing (CRITICAL)
- The GEMP server sometimes skips phases without your consent — you get pushed past Control/Drain/Deploy
- **WORKAROUND**: Trigger effects (CC Occupation, Dark Deal) BEFORE initiating force drains
- **DRAIN TIMING**: Give a brief pause between draining at different locations — rapid-fire drains confuse the server
- **REVERT**: When the phase-skip happens, submit "revert" as the decision value to go back. Check `revertEligible` parameter first.
- Reverts are TOOLS, not failures — Steve reverted 5+ times in his winning Dark Deal game to fix phase skips and adjust tactical decisions

### Force Economy (CRITICAL — Most Important Lesson)
Every turn follows this cycle:
1. **Activate ALL force** — 100%, every turn, no exceptions
2. **Deploy as much as possible** — spend force aggressively on characters, locations, weapons, ships. Get cards on the table. An undeployed hand is wasted potential.
3. **Draw remaining force into hand** — whatever you didn't spend deploying, draw from force pile so you have cards to deploy NEXT turn. Empty hand = nothing to deploy = wasted turns.
4. **Save at most 1 force** for an emergency interrupt during opponent's turn (optional).

**NEVER let force pile grow large.** A force pile of 10+ means you failed to deploy and draw. That force should be cards on the table or cards in your hand.

**WHY:** Cards in force pile do NOTHING. Cards in hand can be deployed. Cards on table drain, battle, and win. The faster you deploy, the more drain locations you control, and the faster your opponent dies.

---

## Anti-Opponent Intelligence

### Rando Cal Playing Dark Deal (TDIGWATT)
- Rando lost 100% of games as Dark Deal (0-15 record)
- Rando CAN flip the objective but can't keep it flipped
- Rando loses 96% of battles (1-24 record)
- Rando deploys to opponent Tatooine sites where Luke Saga is entrenched — DON'T do this
- Rando gets Executor destroyed — protect Executor at all costs
- Rando spreads too thin post-flip — concentrate forces

### Rando Cal Playing Light Side
- Rando initiates battles with only 37% power advantage — picks bad fights
- Rando doesn't contest Bespin system drains (237 uncontested drains)
- Rando fails to build board presence by turn 2, gets steamrolled

---

## Card Identification Quick Reference

Cards are identified by blueprint IDs (e.g., `226_1`). To look up a card:
- Blueprint `X_Y` -> class `CardX_YYY` (pad Y to 3 digits)
- Location: `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/setX/dark/` or `.../light/`
- The card title is in the class constructor

### Key Blueprint IDs
- `226_12` = This Deal Is Getting Worse All The Time (V) (TDIGWATT objective)
- `226_1` = Cloud City: Dining Room (V)
- `7_273` = Cloud City: Upper Walkway
- `200_110` = Knowledge And Defense (V)
- `226_6` = I'm Sorry (V)
- `212_4` = Slip Sliding Away (V)
- `226_28` = The Hidden Path (objective)
- `200_35` = Anger, Fear, Aggression (V)

---

# SECTION 2: Rando's Logic Reference (AI Source Code)

When making decisions, refer to this section FIRST. The summary below gives quick guidance. For full detail, READ THE ACTUAL SOURCE FILE listed for each decision type.

## Source File References (read these during gameplay)
| Decision Type | File | Lines |
|---|---|---|
| **Deploy** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.java` | 3,258 |
| **Battle** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/BattleEvaluator.java` | 711 |
| **Move** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/MoveEvaluator.java` | 1,913 |
| **Card Selection** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java` | 5,292 |
| **Action Text** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/ActionTextEvaluator.java` | 2,899 |
| **Deploy Planning** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/strategy/DeployPhasePlanner.java` | 1,818 |
| **Objective Strategy** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/strategy/ObjectiveAnalyzer.java` | 908 |
| **Deck Tracking** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/strategy/DeckOracle.java` | 843 |
| **Main AI** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java` | 1,789 |
| **Config/Scores** | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoConfig.java` | ~200 |

**How to use:** When facing a deploy decision, read DeployEvaluator.java. When facing a battle, read BattleEvaluator.java. The summary below is a quick reference — the source files have the complete logic with every edge case and score value.

## Quick Summary (for when you don't have time to read the full file)

## Force Activation Rules
- Always activate at least 1 Force (prevents infinite loops).
- Reserve 4 cards in Reserve Deck for destiny draws at contested locations.
- Cap Force Pile at 25. **K-2 NOTE: Steve says deploy and draw instead of capping — spend it all.**
- Late game (life force < 12): preserve Reserve Deck aggressively.
- Critical life force (< 6): minimal activation to avoid decking out.
- Allow opponent max activation when asked.

## Deploy Phase Rules
**Priority order:** Locations first (+200 base) > Stop bleeding at drain-vulnerable sites > Reinforce losing positions > Establish new locations > Build up winning positions.

**Core deploy rules:**
- Locations ALWAYS deploy first. If location in hand, all non-deploy actions get -800 penalty.
- TDIGWATT: Bespin system gets +800 on turn 1, +400 turns 2-3. Deploy before anything else.
- BESPIN-FIRST guard: Until Bespin occupied, block all character deploys (-500). Exempt: locations, AMSD, Executor.
- Deploy urgency scales with hand size: +200 at 12 cards, +250 at 13, +300 at 14.
- Reserve 2 Force for Vader movement when hunting.
- Reserve 1 Force for Draw Their Fire interrupt tax.

**Ability threshold (CRITICAL):**
- Need ability >= 4 at a site to draw battle destiny.
- Soft buddy target: ability >= 7 gets bonus.
- Solo characters with power < 6 need a buddy. Vader/Emperor (ability >= 6) can deploy solo.
- Matching pilots get +40 bonus deploying to their ship.

## Battle Evaluation Rules
**Power advantage thresholds:**
- CRUSH: +8 — definitely initiate
- FAVORABLE: +5 to +7 — recommended
- MARGINAL: +2 to +4 — worth considering
- RISKY: 0 to +1 — cautious
- DANGEROUS: negative — avoid/retreat

**Effective power:** effectiveDiff = powerDiff + (abilityDiff * 2.5). Ability impacts destiny draws heavily.

**K-2 CRITICAL LESSON:** Even power does NOT mean even battle. Check ability difference — it determines destiny draw count. 3 bounty hunters (ability 16, 4 draws) crush 2 Jedi (ability 12, 1 draw) at equal power.

**Weapon awareness:** Lightsaber adds +5 effective power. Other weapons +3.

**Monte Carlo prediction:** Rando runs 50 battle simulations. Default win threshold: 60% to initiate.

**Life force awareness:** Behind on life force (> 5 gap): more aggressive. Ahead: conservative.

## Move Evaluation Rules
**Never move:**
- Pilots off their ships (-500)
- Lando from Cloud City sites (-9999)
- From battleground to non-battleground (-800)
- Away from allies if it leaves them solo and vulnerable (-150 to -400)
- Away if it drops site ability below 4 (-300 to -500)

**Always move:**
- Solo character with ability < 4 should escape to join allies (+50)
- Move to pilot capacity slot (+150)

## Draw Phase Rules
- Target hand size: 7-8 cards. Soft cap: 12. Hard cap: 16.
- Small hand (< 5): strong draw preference.
- Low reserve (< 6): penalize draws.
- Critical life force (< 6): stop drawing entirely.

## Card Selection (Forfeit Priority)
- Lose lowest-forfeit cards first.
- Never forfeit damage cancel interrupts (Houjix/Ghhhk).
- Protect key characters (Vader, Emperor, Luke, Obi-Wan, Han).
- Target opponent's highest-power characters when selecting targets.

## Key Score Constants
- BAD_ACTION_THRESHOLD: -100 (below this, bot passes instead)
- Critical scores: +500 to +1500 (must-do actions)
- High priority: +200 to +400
- Tactical: +50 to +150
- Mild penalty: -30 to -80
- Strong penalty: -200 to -500
- Hard block: -9999

---

# SECTION 3: Decision Weight Tracking

Track outcomes when K-2's instinct differs from Rando's logic. Over time, this builds data for improving both.

## Format: [Date] Decision | Rando Said | K-2 Said | Actual Outcome | Who Was Right

### K-2 v2 Session (April 14, 2026) — Bot-vs-Bot Analysis (3 games)
- Game 1: CO Dark WIN (13 turns, 22 drains vs 2) — drain engine worked, but CO battled at 11v11 and 10v12 losing Lando+Lobot
- Game 2: CO Light LOSS (5 turns, 0 drains) — ChosenOne has NO Luke Saga playbook, conceded T5
- Game 3: CO Dark LOSS (11 turns, 22 drains vs 19) — Rando out-drained from Carbonite Chamber, CO lost 2 bad battles

### PRIORITY FIXES FOR NEXT SESSION:
1. **BattleEvaluator: Raise initiation threshold** — CO initiates at even power (11v11). Must be +5 minimum per SKILL.md. Check FAVORABLE threshold in BattleEvaluator.java.
2. **ChosenOne Light Side playbook** — Currently 0% win rate as Light. Need Yarna location pulls, Cantina/Mos Eisley shuttle logic, Skywalker retrieval. See Steve's 44-5 Luke Saga analysis in this file.
3. **Protect Lando+Lobot from bad battles** — CO forfeits Lando in battles it shouldn't have initiated. MoveEvaluator should hard-block Lando from battle locations where opponent has equal+ power.
4. **Block opponent drains at CC sites** — Rando drained 19x at Carbonite Chamber in Game 3. CO needs to maintain presence at ALL occupied CC sites, not just its own drain locations.

### SESSION INFRASTRUCTURE NOTES:
- Threading fix applied: driveAiDecisions TIME_LIMIT_MS = 2000ms (was 5000). ServerCleaner parallelized.
- Docker: added cpus:4, G1GC, ActiveProcessorCount=4
- Fixed K-2v1 compile errors in both MoveEvaluator.java files (card.getGame() → simplified passenger check)
- k2_player.py script exists but is just hardcoded rules, NOT Claude playing. Use bot-vs-bot for training instead.
- Steve's replay analysis: 114 Luke Saga games (44-5, 89% win rate). ALL 5 losses had 0 shuttle moves. Shuttle pattern is THE win condition.

## Known Disagreements (from today's games):
1. **Force pile hoarding**: Rando caps force pile at 25. K-2/Steve says spend everything and draw rest into hand. **K-2/Steve is right** — hoarding force pile lost K-2 the second game.
2. **Battle at even power**: Rando's 60% win threshold would have said NO to the 14 vs 14 battle. K-2 attacked anyway. **Rando was right** — ability difference meant 4 destiny draws vs 1, devastating loss.
3. **Deploy locations first**: Rando gives -800 penalty to non-location deploys when location is in hand. K-2 kept deploying characters without locations. **Rando was right** — no Tatooine locations = no drain engine.
4. **Control phase movement shuffle**: K-2 knows about Mos Eisley/Cantina combo. Rando doesn't have this logic. **K-2 is right** — double drain from one stack is a game-winning pattern.
5. **Lando shuttle as win condition**: Steve's Dark Deal game proved the Lando shuttle (via Pray I Don't Alter It objective) + CC Occupation + multi-site drains = 9-10 force loss/turn. This is THE engine.
6. **Flip Turn 3, not Turn 5**: Rando data says first battle Turn 4-5. Steve flipped on Turn 3 with mass deploy to Upper Walkway. **Steve is right** — earlier flip = more turns of drain engine running.
7. **Effect removal via Lando**: Steve used Lando's game text to remove Rando's Yarna d'al' Gargan (opponent's puller). Rando doesn't consider this disruption. **Steve is right** — removing opponent's pullers cripples their tempo.
8. **Exploit power-0 ships at sites**: Rando landed Wild Karrde at Mos Eisley (power 0). Steve sent Boba + Dr. E&PB for 21 vs 0 = 15+ overflow. **Steve exploited this perfectly** — Rando should NEVER land ships at sites where opponent can reach them.
9. **Trigger effects before drains**: Phase-skip bug eats drain phase if you drain first. Steve learned to trigger CC Occupation and Dark Deal BEFORE draining. Critical GEMP-specific lesson.

## Observations from Steve's Dark Deal Game (Replay Analysis by K-2 v1)

### Steve's Turn 4 Phase Order (EXACT — memorize this):
1. **Revert twice** (phase-skip bug pushed past Control)
2. CC Occupation → opponent loses 2 (Rebel Leadership + Luke's Bionic Hand)
3. Lando shuttle: Dining Room → Carbonite Chamber via Pray I Don't Alter It
4. Drain 1 at Carbonite Chamber (where Lando just arrived)
5. Drain 1 at Upper Walkway
6. Drain 2 at Bespin (Executor)
7. Deploy Dark Deal (V) on Bespin
8. Deploy Lord Maul + Commander Gherant to Dining Room
9. Battle at Dining Room — weapon destiny 7 (Maul's lightsaber hits), battle destiny 6 vs 5, Dark side wins
10. Dark Deal triggers — retrieve card from Used Pile
11. Move Gherant + Maul to Carbonite Chamber post-battle
**Total Turn 4 damage: CC Occ (2) + 4 drains + battle overflow = 12+ force loss in ONE turn**

### Rando's Critical Mistakes (from this game):
1. **Wild Karrde landed at Mos Eisley with power 0** — Steve deployed Boba + Dr. E&PB for 21 vs 0, game-ending overflow. **V49 code fix applied: -9999 penalty for landing starships at sites without passengers.**
2. **Deployed Rey + Leia + Padme to Dining Room where Steve had Dr. E&PB + Lord Maul** — gave Steve free battle targets with power disadvantage. Rando didn't check opponent power before deploying. **V49 code fix applied: -200 penalty for deploying into opponent power disadvantage > 3.**
3. **Rando used Houjix to cancel 14 battle damage on Turn 3** — smart play, but only delayed the inevitable. Rando should have deployed characters to locations where Steve WASN'T, not into his battle stack.
4. **Rando deployed Sabine + Yoda to Upper Walkway on Turn 2** — Steve mass-deployed there on Turn 3 and crushed them in the flip battle. Rando gave Steve the perfect flip target by concentrating weak characters at one CC site.

### Steve's Tactical Highlights:
- **Lando's effect removal**: First action after deploying Lando = remove opponent's Yarna. Crippled Rando's location-pulling engine immediately.
- **Blizzard 4 as passenger transport**: Deployed Mara Jade + Piett as passengers on Blizzard 4 to Upper Walkway. Disembarked Mara after battle. Efficient multi-character deploy to one location.
- **Post-battle repositioning**: After winning at Dining Room Turn 4, moved Gherant + Maul to Carbonite Chamber. Spread forces across CC sites for next turn's drains. Never leave characters at a location you've already won — spread for drain coverage.
- **Opportunistic Tatooine strike**: Steve stayed at Cloud City for 4 turns, then saw Wild Karrde at Mos Eisley with power 0 on Turn 5. Deployed Boba + Dr. E&PB for the killing blow. Only go to opponent territory when there's a guaranteed kill.

### V49 Code Changes Applied (Rando + Chosen One):
1. **MoveEvaluator.handleLandAction**: Starship landing at site without passengers = -9999 hard block
2. **DeployEvaluator**: Deploy to opponent-occupied location where our total power after deploy < opponent power - 3 = -200 penalty + skip
