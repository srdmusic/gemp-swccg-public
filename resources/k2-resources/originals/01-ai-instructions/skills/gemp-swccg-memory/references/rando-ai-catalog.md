---
title: Rando Cal AI - Complete Architecture Catalog
updated: 2026-03-03
---

# GEMP-SWCCG RANDO CAL AI SYSTEM - COMPLETE CATALOG

**Date Generated:** 2026-03-03  
**Version:** V24.7+ (Latest)  
**Base Path:** `/sessions/clever-hopeful-dirac/mnt/gemp-swccg-public/src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/`

---

## QUICK INDEX

### Core Architecture
- **AiRegistry** - AI player registry and lookup
- **SwccgAiController** - Interface all AIs implement
- **HeuristicAiBase** - Base class for all AI implementations
- **RandoCalAi** - Main advanced AI with personality

### AI Implementations
- **BeginnerAi** - Light keyword-based heuristics
- **AdvancedAi** - Enhanced heuristics with board awareness
- **RandoCalAi** - Expert AI with full strategy system

### Decision Processing
- **DecisionContext** - Encapsulates all decision information
- **EvaluatedAction** - Scored action with reasoning
- **ActionType** - Enum of possible action types
- **CombinedEvaluator** - Merges all evaluator scores

### Evaluators (Decision Scoring)
- **ActionEvaluator** - Base class for all evaluators
- **DeployEvaluator** - Deployment phase card placement
- **BattleEvaluator** - Battle initiation and tactics
- **BattlePredictor** - Monte Carlo battle outcome simulation
- **ForceActivationEvaluator** - Force activation amounts
- **DrawEvaluator** - Card drawing decisions
- **CardSelectionEvaluator** - Card targeting/selection
- **MoveEvaluator** - Character movement
- **PassEvaluator** - Pass/cancel decisions
- **ActionTextEvaluator** - General action text scoring

### Strategy Components
- **StrategyController** - Overall game strategy state
- **DeckOracle** - Full deck knowledge (V22.6+)
- **OpponentDeckTracker** - Opponent destiny intelligence (V24.7+)
- **DeployPhasePlanner** - Deploy phase strategy
- **ObjectiveHandler** - Objective card requirements
- **ShieldStrategy** - Defensive shield management
- **ObjectiveAnalyzer** - Analyze objective value
- **DeployStrategy** - Deployment strategy selection

### Safety & Tracking
- **DecisionSafety** - Emergency response generation
- **DecisionTracker** - Loop detection (CRITICAL)
- **RandoLogger** - Centralized logging system
- **RandoConfig** - Configuration constants

### Supporting Utilities
- **AiBoardAnalyzer** - Board state analysis
- **AiPriorityCards** - High-value card registry
- **AiCardHelper** - Card property helpers
- **AiDestinyCalculator** - Destiny calculations
- **AiChatManager** - Chat message generation

---

## 1. CORE ARCHITECTURE FILES

### 1.1 AiRegistry.java
**Path:** `/ai/AiRegistry.java`  
**Purpose:** Registry for AI player instances per game

**Key Methods:**
- `register(String gameId, String playerId, SwccgAiController ai)` - Register AI for a game
- `get(String gameId, String playerId)` - Get AI for specific player
- `isAi(String gameId, String playerId)` - Check if player is AI
- `unregisterGame(String gameId)` - Clean up game

**Implementation:**
- Uses `ConcurrentHashMap<String, Map<String, SwccgAiController>>` (gameId → playerId → AI)
- Thread-safe for concurrent game management
- Automatically cleans up on game end

---

### 1.2 SwccgAiController.java
**Path:** `/ai/SwccgAiController.java`  
**Purpose:** Interface all AI implementations must follow

**Key Methods:**
- `String decide(String playerId, AwaitingDecision decision, GameState gameState)` - **REQUIRED** - Make decision
- `void setGame(SwccgGame game)` - **Optional** - Set game reference (default: no-op)
- `String getChatMessage()` - **Optional** - Get chat message (default: null)

**Decision Response Format:**
- Empty string "" = Pass (when allowed)
- Action ID or card ID = Make choice
- Response validated by DecisionSafety

---

### 1.3 HeuristicAiBase.java
**Path:** `/models/HeuristicAiBase.java`  
**Extends:** SwccgAiController  
**Abstract Base for:** BeginnerAi, AdvancedAi, RandoCalAi

**Key Methods:**
- `String decide(String playerId, AwaitingDecision decision, GameState gameState)` - Main decision entry point
- `scoreAction(String action, String text, GameState gameState)` - Keyword weight scoring
- `getActionWeights()` - Return keyword weights (abstract - subclass defined)
- `getActionPenalties()` - Return penalty keywords
- `getChoiceWeights()` - Return choice weights
- `getChoicePenalties()` - Return choice penalties

**Key Fields:**
- `BLOCKED_RESPONSE_PENALTY = 500` - Penalty for blocked response
- `FAILED_SEARCH_PENALTY = 650` - Penalty for failed search
- `MISSING_RESERVE_DECK_TITLE_PENALTY = 900` - Penalty for missing reserve deck card
- `SINGLE_DECISION_LOOP_THRESHOLD = 2` - Loop detection trigger
- `RECENT_DECISION_RESPONSE_WINDOW = 6` - Recent decisions to track
- `REASSIGNMENT_REPEAT_PENALTY = 800` - Penalty for repeated reassignment

**Decision Processing:**
1. Parse decision parameters (min/max, results, etc.)
2. Score available options using keyword weights
3. Apply loop prevention penalties
4. Return best-scored option or pass if allowed

---

## 2. MAIN AI IMPLEMENTATION

### 2.1 RandoCalAi.java
**Path:** `/models/rando/RandoCalAi.java`  
**Extends:** HeuristicAiBase  
**Purpose:** Expert-level AI with full strategic system

**Key Components:**
- **Evaluators** (CombinedEvaluator)
- **Strategy System** (StrategyController, DeployPhasePlanner, etc.)
- **Deck Knowledge** (DeckOracle)
- **Opponent Tracking** (OpponentDeckTracker)
- **Loop Detection** (DecisionTracker)
- **Decision Safety** (DecisionSafety)

**Initialization:**
```java
public RandoCalAi(String gameName, Side side)
  - Creates DeckOracle, OpponentDeckTracker, StrategyController, DecisionTracker
  - Initializes CombinedEvaluator with all evaluators
  - Sets up RandoLogger and RandoConfig
```

**Main Decision Flow:**
1. `decide()` → Check for game state, validate playerId
2. `buildEvaluatorContext()` → Create DecisionContext with all strategy components
3. `evaluateDecision()` → Run CombinedEvaluator (all evaluators)
4. `DecisionTracker.recordDecision()` → Track for loop detection
5. `DecisionSafety.ensureValidResponse()` → Validate response
6. `recordDecision()` → Log and return response

**Key Methods:**
- `setGame(SwccgGame game)` - Set game reference for advanced features
- `getChatMessage()` - Generate contextual chat (when enabled)
- `buildEvaluatorContext(DecisionContext)` - Build full strategy context
- `evaluateDecision(DecisionContext)` - Run CombinedEvaluator
- `checkForInfiniteLoop()` - Detect and escalate loop response

**Key Fields:**
- `combEvaluator: CombinedEvaluator` - All evaluators combined
- `strategyController: StrategyController` - Overall game strategy
- `deckOracle: DeckOracle` - Deck knowledge
- `opponentDeckTracker: OpponentDeckTracker` - Opponent destiny intel
- `deployPhasePlanner: DeployPhasePlanner` - Deploy strategy
- `decisionTracker: DecisionTracker` - Loop detection
- `shieldStrategy: ShieldStrategy` - Defensive shields
- `objectiveHandler: ObjectiveHandler` - Objective requirements

---

## 3. AI VARIANTS

### 3.1 BeginnerAi.java
**Path:** `/models/BeginnerAi.java`  
**Extends:** HeuristicAiBase  
**Purpose:** Light keyword-based heuristics for casual play

**Scoring Constants (ACTION_WEIGHTS):**
- "force drain" = 120
- "initiate battle" = 110
- "battle" = 70
- "deploy" = 60
- "activate" = 50
- "weapon" = 40
- "play" = 35
- "draw" = 25
- "steal/capture" = 25
- "react" = 20

**Penalties (ACTION_PENALTIES):**
- "pass" = -120
- "forfeit" = -80
- "place in lost pile" = -70
- "sacrifice" = -80
- "lose" = -45

**CHOICE_WEIGHTS/PENALTIES:** Similar pattern for card choices

**No specialized strategy** - Pure keyword matching

---

### 3.2 AdvancedAi.java
**Path:** `/models/AdvancedAi.java`  
**Extends:** HeuristicAiBase  
**Purpose:** Enhanced heuristics with board/hand awareness

**Scoring Constants (higher than Beginner):**
- "force drain" = 160
- "initiate battle" = 150
- "battle" = 100
- "deploy" = 90
- "activate" = 70

**Additional Features:**
- Board state awareness (hand size, force pile)
- Card type matching (title bonus +80, type bonus +20)
- Phase-aware strategy
- Max 3 type matches per decision

**Key Method Override:**
- `AiContext.build()` - Builds board context for evaluation
- Phase-specific penalties/bonuses

---

## 4. DECISION CONTEXT & EVALUATION

### 4.1 DecisionContext.java
**Path:** `/models/rando/evaluators/DecisionContext.java`  
**Purpose:** Encapsulate all information needed to evaluate a decision

**Game State Fields:**
- `gameState: GameState` - Core game state
- `playerId: String` - AI player ID
- `side: Side` - Light or Dark
- `game: SwccgGame` - Full game reference
- `phase: Phase` - Current game phase
- `turnNumber: int` - Current turn
- `isMyTurn: boolean` - Whose turn is it?

**Decision Info:**
- `decisionType: String` - CARD_ACTION_CHOICE, CARD_SELECTION, INTEGER, etc.
- `decisionText: String` - Human-readable prompt
- `decisionId: String` - Unique decision ID

**Available Actions:**
- `actionIds: List<String>` - Possible action IDs
- `actionTexts: List<String>` - Readable action descriptions
- `cardIds: List<String>` - Card IDs to select from
- `blueprints: List<String>` - Card blueprint IDs
- `selectable: List<Boolean>` - Which cards are selectable
- `testingTexts: List<String>` - Card titles from GEMP

**Selection Constraints:**
- `noPass: boolean` - Must make a choice?
- `min: int` - Minimum selections
- `max: int` - Maximum selections

**Strategy Components (Injected):**
- `strategyController: StrategyController`
- `deployPhasePlanner: DeployPhasePlanner`
- `objectiveHandler: ObjectiveHandler`
- `objectiveAnalyzer: ObjectiveAnalyzer`
- `shieldStrategy: ShieldStrategy`
- `deckOracle: DeckOracle` (V22.6+)
- `opponentDeckTracker: OpponentDeckTracker` (V24.7+)

**Key Methods:**
- `getCardTitleAt(int index)` - Get card name (handles "•" prefix and "(V)" virtual marker)
- `getForceAvailable()` - Force pile size
- `getReserveDeckSize()` - Reserve deck size
- `getLifeForce()` - Total life force (reserve + used + force pile)
- `getHand()` - Cards in hand
- `getHandSize()` - Hand size

---

### 4.2 EvaluatedAction.java
**Path:** `/models/rando/evaluators/EvaluatedAction.java`  
**Purpose:** Represent a scored action with reasoning

**Key Fields:**
- `actionId: String` - The action/card ID to return
- `actionType: ActionType` - Category of action
- `score: float` - Total score (higher = better)
- `displayText: String` - Readable description
- `reasoning: List<String>` - Reasoning breakdown

**Optional Metadata:**
- `cardName: String` - Card title
- `blueprintId: String` - Card blueprint ID
- `deployCost: int` - Deploy cost (if relevant)
- `expectedValue: float` - Expected value calculation

**Key Methods:**
- `addReasoning(String reason)` - Add reasoning (no score change)
- `addReasoning(String reason, float scoreDelta)` - Add reasoning with score adjustment
- `mergeFrom(EvaluatedAction other)` - Merge another action's score/reasoning
- `getReasoningString()` - Format all reasoning with pipe separators
- `toString()` - Format as "EvaluatedAction(id=..., score=..., text...)"

**Merging Logic:**
- Scores are ADDED (allows multiple evaluators to contribute)
- Reasoning lists are concatenated
- More specific ActionType is preferred
- More descriptive display text is used

---

### 4.3 ActionType.java
**Path:** `/models/rando/evaluators/ActionType.java`  
**Purpose:** Categorize types of actions the AI can take

**Core Actions:**
- `DEPLOY` - Deploy card
- `PASS` - Pass/cancel
- `ACTIVATE_FORCE` - Activate force
- `BATTLE` - Initiate/conduct battle
- `MOVE` - Move character
- `DRAW` - Draw card
- `DRAW_DESTINY` - Draw destiny random number

**Card Selection:**
- `SELECT_CARD` - Select a card
- `ARBITRARY` - Arbitrary choice

**Combat Related:**
- `FIRE_WEAPON` - Fire weapon
- `BATTLE_DESTINY` - Battle destiny draw
- `SUBSTITUTE_DESTINY` - Substitute destiny draw
- `CANCEL_DAMAGE` - Cancel battle damage

**Special Actions:**
- `FORCE_DRAIN` - Force drain action
- `RACE_DESTINY` - Race destiny
- `REACT` - Respond to action
- `STEAL` - Steal card
- `SABACC` - Sabacc value selection
- `CANCEL` - Cancel something
- `EMBARK` - Embark on starship

**Fallback:**
- `UNKNOWN` - Unknown action type

---

### 4.4 CombinedEvaluator.java
**Path:** `/models/rando/evaluators/CombinedEvaluator.java`  
**Purpose:** Run all evaluators and combine their scores

**Evaluator Initialization Order:**
1. `ForceActivationEvaluator` - INTEGER decisions
2. `DeployEvaluator` - Deploy actions
3. `BattleEvaluator` - Battle actions
4. `MoveEvaluator` - Movement
5. `DrawEvaluator` - Card draws (draw phase only)
6. `CardSelectionEvaluator` - Card selections
7. `ActionTextEvaluator` - General text-based actions
8. `PassEvaluator` - Pass/cancel actions

**Key Methods:**
- `evaluateDecision(DecisionContext)` - Run all evaluators, merge scores, return best
- `canHandle(DecisionContext)` - Check if any evaluator can handle this decision
- `getEvaluators()` - Return list for testing

**Scoring Logic:**
1. Map stores `(actionId → EvaluatedAction)`
2. Run each applicable evaluator in order
3. For each scored action:
   - If actionId exists: merge scores + reasoning
   - If new: add to map
4. Select best-scored action
5. If best score < BAD_ACTION_THRESHOLD (-100) and can pass → recommend passing
6. Log final decision with reasoning

**Special Cases:**
- **Bad Action Threshold:** If all actions score < -100, prefer to pass (if allowed)
- **V24.5 Change:** No randomness on bad actions anymore - always pass when possible
- **Must-Choose Logic:** If noPass=true or min>=1, even bad actions must be selected

---

## 5. ACTION EVALUATORS

### 5.1 ActionEvaluator.java (Base Class)
**Path:** `/models/rando/evaluators/ActionEvaluator.java`  
**Abstract Base for:** All evaluator implementations

**Key Methods:**
- `abstract boolean canEvaluate(DecisionContext context)` - Can this evaluator handle this decision?
- `abstract List<EvaluatedAction> evaluate(DecisionContext context)` - Score all possible actions
- `logEvaluation(EvaluatedAction action)` - Debug logging
- `getName()`, `isEnabled()`, `setEnabled(boolean)` - State management

**Standard Pattern:**
1. Check `canEvaluate()` to see if evaluator applies
2. If yes, `evaluate()` returns list of all possible actions with scores
3. Return actions even if score is 0 or negative
4. CombinedEvaluator merges results from multiple evaluators

---

### 5.2 DeployEvaluator.java
**Path:** `/models/rando/evaluators/DeployEvaluator.java`  
**Extends:** ActionEvaluator  
**Purpose:** Score deployment actions

**Activation Conditions:**
- Decision type = CARD_ACTION_CHOICE
- Action text contains "Deploy" (case-insensitive)
- NOT "Deploy from..." (that's a different context)

**Scoring Factors:**
1. **Card Value** = (Power + Ability) - DeployCost
2. **Location Priority**
   - Contested locations (both sides present) = +20
   - Losing locations = +40 (reinforcement bonus)
   - Uncontested locations = +10
3. **Strategic Value**
   - Locations = +100 (highest priority)
   - Matching pilots = +40
   - Characters with weapons = +30
4. **Affordability Check**
   - Can we pay the cost? If not, -200 penalty

**Key Decision Logic:**
- **V21 Starting Effect Ban**: Certain effects banned as starting cards
  - "No Escape", "Coarse And Rough And Irritating"
  - Only applies on turn 0 (starting interrupt phase)
- **V22.4 Suicide Block**: Prevents deploying alone to opponent-icon locations when severely outmatched
- **Location Deployment**: Always prefer locations first (V24.4 LOCATIONS FIRST rule)

**Integration with Strategy:**
- Uses `DeployPhasePlanner` for deployment plan
- Respects deploy strategy from `StrategyController`
- Avoids cards already tried this turn (loop prevention)

---

### 5.3 BattleEvaluator.java
**Path:** `/models/rando/evaluators/BattleEvaluator.java`  
**Extends:** ActionEvaluator  
**Purpose:** Score battle-related actions

**Activation Conditions:**
- Decision type = CARD_ACTION_CHOICE
- Phase = BATTLE, OR
- Decision/action text contains "battle", "initiate", or "fire"

**Battle Thresholds:**
- `CRUSH_THRESHOLD = 8` - Overwhelming advantage
- `FAVORABLE_THRESHOLD = 5` - Strong advantage
- `MARGINAL_THRESHOLD = 2` - Worth initiating
- `RISKY_THRESHOLD = 0` - Even/slight advantage
- `MIN_RESERVE_FOR_BATTLE = 3` - Reserve deck cards for destiny

**Scoring Factors:**

**Initiate Battle:**
- V22.4 **LOCATION-SPECIFIC EVALUATION**
  - Tries to extract specific target location from action text
  - Evaluates ONLY that location (not all locations)
  - Fallback: if location can't be identified, checks all locations but more conservatively
  - **SUICIDE BLOCK**: If opponent power > 2x ours AND > 6 → -500 penalty
  - If favorable (effectiveDiff >= MARGINAL_THRESHOLD) → +40
  - If even → -30
  - If unfavorable → -60 to -250 (scaled by disadvantage)
- **Reserve Deck**: If < MIN_RESERVE_FOR_BATTLE → -50
- **Board Position**:
  - Behind on life force → +15 (slight encouragement)
  - Ahead on life force → -20 (be conservative)
  - Critical life force (≤6) → +30 (need to act)

**Weapon Firing:**
- Base +40
- Target character → +10
- Target unique card → +20

**Cancel Battle (It's A Trap):**
- If we initiated → -150 (NEVER cancel our own battle)
- If opponent initiated:
  - Badly losing (powerDiff < -5) → +60
  - Slightly losing → +20
  - Not losing → -60 (don't waste interrupt)

**Battle Phase Actions:**
- Fire weapons → +50
- Draw battle destiny → +30

---

### 5.4 BattlePredictor.java
**Path:** `/models/rando/evaluators/BattlePredictor.java`  
**Static Utility for:** Predicting battle outcomes

**Key Constants:**
- `SIMULATIONS = 50` - Number of Monte Carlo simulations
- `DESTINY_MAX = 6` - Destiny range per draw (0-6)

**Core Methods:**

**`predictBattle(int myPower, int myDestinyDraws, int oppPower, int oppDestinyDraws)`**
- Runs 50 battle simulations with random destiny
- Returns `BattleOutcome` with:
  - `winProbability` (0.0-1.0)
  - `expectedDamageDealt` (avg damage if we win)
  - `expectedDamageTaken` (avg damage if we lose)

**V24.7 Enhancements:**

**`predictBattleWithIntel(int myPower, int myDestinyDraws, int oppPower, int oppDestinyDraws, float knownOppDestinyAvg)`**
- Uses KNOWN opponent destiny average instead of random
- Still simulates our draws (we don't know our order)
- Much more accurate for opponent intelligence

**`predictBattleFullIntel(int myPower, int myDestinyDraws, float myDestinyAvg, int oppPower, int oppDestinyDraws, float oppDestinyAvg)`**
- Both sides' averages known (from DeckOracle and OpponentDeckTracker)
- Deterministic: no randomness needed
- Most accurate prediction

**`predictBattle(int myPower, int myDestinyDraws, int oppPower, int oppDestinyDraws, DeckOracle deckOracle, OpponentDeckTracker tracker)`**
- Auto-selects best method based on available intel
- Falls back to random if no intel available
- Recommended method

**BattleOutcome Methods:**
- `isFavorable()` - winProbability >= 60%
- `isFavorable(float threshold)` - Custom threshold
- `isRisky()` - 40-60% win rate
- `isDangerous()` - < 40% win rate
- `getExpectedNetDamage()` - Positive = we deal more

**Usage in Strategy:**
- Battle predictions influence force drain decisions
- Combat decisions use outcome probability
- Combined with board state for final verdict

---

### 5.5 ForceActivationEvaluator.java
**Path:** `/models/rando/evaluators/ForceActivationEvaluator.java`  
**Extends:** ActionEvaluator  
**Purpose:** Determine optimal force activation amount

**Activation Conditions:**
- Decision type = INTEGER

**Scoring Logic:**

**Early Game (Turns 1-3):**
- Activate MAXIMUM available force
- Reason: Build resources aggressively

**Standard Force Activation:**
- Calculate optimal amount considering:

**Rule 1: Reserve Cards for Destiny Draws**
- Always reserve ~4 cards (or more if contested)
- Prevents deck-out during battles

**Rule 2: Cap Force Pile at MAX (25)**
- Don't generate wasted force
- Limited value of huge force pile

**Rule 3: Late-Game Preservation**
- If life force < CRITICAL (6), only activate enough for one action
- Preserve rest for destiny draws

**Additional Considerations:**
- **Expensive Cards**: If we have 6+ cost cards in hand and not enough force, need to activate more
- **Already High Force**: If force > 12, only activate 2 more (diminishing returns)
- **Small Hand**: If hand <= 4 cards and force >= 8, limit to 2 (not worth more)

**Config Constants (RandoConfig):**
- `MAX_FORCE_PILE = 25`
- `RESERVE_FOR_DESTINY_CONTESTED = 4`
- `LATE_GAME_LIFE_FORCE = 12`
- `CRITICAL_LIFE_FORCE = 6`

---

### 5.6 DrawEvaluator.java
**Path:** `/models/rando/evaluators/DrawEvaluator.java`  
**Extends:** ActionEvaluator  
**Purpose:** Score card draw decisions

**Activation Conditions:**
- Decision type = CARD_ACTION_CHOICE or ACTION_CHOICE
- Phase = DRAW (draw phase only!)
- Our turn (`isMyTurn` = true)
- Action text contains "draw" but NOT "destiny"

**Scoring Factors:**

**Hand Size Target:**
- Target = 7 cards (optimal play)
- Soft cap = 12 (start penalizing draws above)
- Hard cap = 16 (strong avoidance)

**Low Reserve Threshold (<6):**
- Risk of deck-out
- Limit activation

**Draw Encouragement:**
- Small hand (<5) → +8 bonus
- Below target (5-7) → +4 bonus

**Force-Starved Strategy:**
- If force activation limited (≤8) and power low:
  - Draw to find more force-generating locations
  - Can draw even with decent hand

**Hold-Back Draw Logic:**
- If we couldn't deploy anything and force is low:
  - Draw aggressively to find playable cards

**Late-Game Preservation:**
- If life force critical (<6) and force high:
  - May skip drawing

**Blocked Response:**
- If response is blocked (loop prevention) → -200 penalty

---

### 5.7 CardSelectionEvaluator.java
**Path:** `/models/rando/evaluators/CardSelectionEvaluator.java`  
**Extends:** ActionEvaluator  
**Purpose:** Score card selection and targeting decisions

**Activation Conditions:**
- Decision type = CARD_SELECTION or ARBITRARY_CARDS

**Handled Decision Patterns:**
- "choose card to set sabacc value" → Random selection
- "choose where to deploy" → Pick best location
- "choose force to lose" → Pick best card to lose
- "choose a card from battle to forfeit" → Pick lowest forfeit value
- "choose a pilot" → Pick best pilot
- "choose card to cancel" → Cancel opponent's cards, not ours
- "choose target" → Weapon/ability targeting
- "choose card to put on ship" → Pilot/capacity placement

**V21: Starting Effect Ban:**
- Turn 0 (starting interrupt phase):
  - Ban certain effects from being deployed as starting cards
  - List: "No Escape", "Coarse And Rough And Irritating"
  - Penalties = -500 for banned, +100 for allowed
  - Applied before any other scoring

**Scoring Deltas:**
- `VERY_GOOD_DELTA = 150` - Excellent choice
- `GOOD_DELTA = 10` - Good choice
- `BAD_DELTA = -10` - Bad choice
- `VERY_BAD_DELTA = -150` - Terrible choice

**Card Priority Evaluation:**
- Uses `AiPriorityCards` to identify high-value cards
- NEVER forfeit or lose priority cards (barrier, damage cancel, destiny manipulation)
- Prefer losing low-value cards

**Blueprint Lookup:**
- Tries to match cards by blueprintId
- Falls back to title matching if needed

---

### 5.8 MoveEvaluator.java
**Path:** `/models/rando/evaluators/MoveEvaluator.java`  
**Extends:** ActionEvaluator  
**Purpose:** Score character movement decisions

**Activation Conditions:**
- Decision type = CARD_ACTION_CHOICE
- Action text contains "move"

**Movement Strategy:**
- Move to contested locations (where we're losing)
- Move to aid reinforcement
- Move away from danger
- Don't move to locations about to be contested unfavorably

**Scoring Factors:**
- Power balance at target location
- Ability advantage
- Risk of counter-deployment
- Force cost of movement

---

### 5.9 PassEvaluator.java
**Path:** `/models/rando/evaluators/PassEvaluator.java`  
**Extends:** ActionEvaluator  
**Purpose:** Create PASS action as fallback

**Activation Conditions:**
- `noPass` = false (passing allowed)
- `min` = 0 (no minimum selection required)
- For "Required responses", only pass if explicit cancel action exists

**Base Score:** 5.0 (very low - only used if other actions are bad)

**Score Adjustments:**

**Early Game (Turns 1-3):**
- Multiply pass bonus by 0.5 (encourage action)

**Resource-Based Adjustments:**
- Low force (<3) → +2 bonus (save force)
- Low reserve deck (<10) → +3 bonus (conserve cards)
- Small hand (<5) → +8 bonus (save force for drawing)
- Hand below target (5-7) → +4 bonus

**Phase-Specific:**
- Move phase + low force + small hand → +10 bonus (draw instead)

**Action Type Penalties:**
- Battle initiation → -10 (should fight, not pass)
- Already committed action → -15 (follow through)

**Pass Action Selection:**
- For CARD_ACTION_CHOICE: finds explicit "Cancel"/"Done" action
- For CARD_SELECTION: uses empty string ""

---

### 5.10 ActionTextEvaluator.java
**Path:** `/models/rando/evaluators/ActionTextEvaluator.java`  
**Extends:** ActionEvaluator  
**Purpose:** General action text pattern matching

**Activation Conditions:**
- Decision type = CARD_ACTION_CHOICE, ACTION_CHOICE, or MULTIPLE_CHOICE (capacity slot)

**Scoring Deltas:**
- `VERY_GOOD_DELTA = 50` - Excellent action
- `GOOD_DELTA = 30` - Good action
- `BAD_DELTA = -30` - Bad action
- `VERY_BAD_DELTA = -50` - Terrible action

**Key Protections & Penalties:**

**V24.4 LOCATIONS FIRST:**
- If deploy phase and hand contains locations
- Non-deploy actions get -800 penalty (deploy locations first!)
- Exception: Location search actions (TDIGWATT, I'm Sorry)
- Exception: AMSD exempt (deploys Star Destroyer effectively)

**V24.15 AMSD Exempt:**
- "Alert My Star Destroyer" excluded from LOCATIONS FIRST
- Can fire immediately even with locations in hand

**V23 EMPTY PILE GUARD:**
- Block searches of empty Lost Pile / Used Pile
- -300 penalty for searching empty Lost Pile
- -100 penalty for searching pile with ≤2 cards

**Search Failure Prevention:**
- Track failed searches to avoid retrying

**Special Action Handling:**
- Barrier cards: track targets to prevent double-barriring
- Damage cancellation: preserve for critical battles
- Destiny manipulation: high-priority use
- Force drain: high-priority at controlled locations

**Blocked Response Tracking:**
- -200 penalty for blocked responses (loop prevention)

---

## 6. STRATEGY COMPONENTS

### 6.1 StrategyController.java
**Path:** `/models/rando/strategy/StrategyController.java`  
**Purpose:** Manage overall game strategy state

**Game Phase Enum:**
- `EARLY` (Turns 1-3) - Establishing force generation
- `MID` (Turns 4-8) - Building board presence
- `LATE` (Turns 9+) - Consolidating and finishing

**Strategy Focus Enum:**
- `GROUND` - Prioritize characters, vehicles, sites
- `SPACE` - Prioritize starships, pilots, systems
- `BALANCED` - No preference

**Threat Level Enum:**
- `SAFE` - We control, no enemies
- `CRUSH` - Overwhelming advantage (6+)
- `FAVORABLE` - Good odds (2-5)
- `RISKY` - Could go either way (-2 to +2)
- `DANGEROUS` - Bad odds (-6 to -2)
- `RETREAT` - Should retreat (<-6)

**Key Fields:**
- `underBattleOrderRules: boolean` - Battle Order/Plan card in play
- `hasShieldsToPlay: boolean` - Defensive shields available
- `phase: GamePhase` - Current game phase
- `myForceGeneration: int` - Current force generation
- `forceGenerationTarget: int` - Target based on game phase
- `currentFocus: StrategyFocus` - Current deck strategy

**Battle Order Cards:**
- Dark: "8_118", "13_54", "12_129"
- Light: "8_35", "13_8", "12_41"
- Effect: +3 force drain cost unless draining player occupies both battleground site AND system

**Force Generation Targets:**
- EARLY: 8 icons (aggressive)
- MID: 6 icons (balanced)
- LATE: 5 icons (conservative)

---

### 6.2 DeckOracle.java
**Path:** `/models/rando/strategy/DeckOracle.java`  
**Purpose:** V22.6+ Track full deck knowledge for intelligent decisions

**Inner Class: DeckCard**
```java
public static class DeckCard {
    String blueprintId, title
    float destiny, deployCost, power, forfeit, ability
    CardCategory category
    CardSubtype subtype
    String gameText
    Zone currentZone  // Hand, Reserve, Force, Used, Lost, InPlay
}
```

**Key Functionality:**

**Deck Cataloging:**
- Scans entire deck at game start
- Maps cards by blueprintId and title
- Tracks full deck size

**Zone Tracking:**
- Every decision: refreshes card locations from GameState
- Always reads ground truth, never infers
- Supports: Hand, Reserve, Force Pile, Used Pile, Lost Pile, In Play

**Failed Pull Tracking:**
- `failedPulls: Map<String, Integer>` - Count consecutive failed pulls
- Prevents infinite retries of missing cards
- V24.10: AMSD failed attempt tracker (turn-based retry)

**V24.10 AMSD Tracking:**
- `amsdFailedOnTurn: int` - Last failed AMSD attempt turn
- Don't retry same turn (need recirculation)
- Reset on next turn

**Key Methods:**
- `analyze()` - Initial deck scan (called once per game)
- `refresh(GameState)` - Update zones (called every decision)
- `isCardInZone(String blueprintId, Zone zone)` - Check if card exists in zone
- `getAverageDestinyInReserve()` - Calculate average destiny value
- `getFailedPullCount(String blueprintId)` - How many failed pulls?
- `recordFailedPull(String blueprintId)` - Mark pull as failed

**Integration:**
- Initialized in RandoCalAi constructor
- analyze() called on first decision
- refresh() called before evaluators
- Injected into DecisionContext for evaluator access

---

### 6.3 OpponentDeckTracker.java
**Path:** `/models/rando/strategy/OpponentDeckTracker.java`  
**Purpose:** V24.7+ Track opponent deck intelligence from verification peeks

**Deck Peek Mechanic:**
- When opponent searches reserve and fails, bot gets to verify
- Sees all cards in opponent's reserve
- Records destiny values for intelligence

**Key Fields:**
- `opponentDestinyAverage: float` - Calculated average destiny value (default 3.0)
- `totalDestinySum, totalDestinyCards: int` - Aggregated stats
- `peekCount: int` - How many peeks we've had
- `hasIntel: boolean` - Have we gathered real data?

**Key Methods:**

**`recordPeek(float[] destinyValues, int cardCount)`**
- Called when we peek at opponent's reserve
- Records all destiny values seen
- Recalculates average
- Marks hasIntel = true

**`getOpponentDestinyAverage()`**
- Returns calculated average (3.0 if no peeks)
- Used by BattlePredictor for accurate battle simulation

**`hasIntel()`**
- Whether we have real data (vs default estimate)

**`getPeekCount()`**
- How many times we've peeked

**`reset()`**
- Clear all intel (for new game)

**Integration with BattlePredictor:**
- BattlePredictor.predictBattle() checks for tracker intel
- Uses real average instead of random 0-6 simulation
- Dramatically improves battle prediction accuracy

---

### 6.4 DeployPhasePlanner.java
**Path:** `/models/rando/strategy/DeployPhasePlanner.java`  
**Purpose:** Plan deployment strategy for the phase

**Key Classes:**
- `DeploymentPlan` - List of deployment instructions
- `DeploymentInstruction` - Deploy card X to location Y
- `DeployStrategy` - Enum: ESTABLISH_PRESENCE, REINFORCE_LOSING, GAIN_GROUND, DEFEND, etc.

**Planning Process:**
1. Analyze board state (power at each location)
2. Identify strategy (which locations to focus on)
3. Generate deployment plan
4. Score deployments based on plan

**Integration with DeployEvaluator:**
- DeployEvaluator uses plan to score card deployments
- Respects location priority from plan
- Follows established strategy

---

### 6.5 ObjectiveHandler.java
**Path:** `/models/rando/strategy/ObjectiveHandler.java`  
**Purpose:** Handle objective card starting requirements

**Objective Database:**
- Maps objective blueprintId → required card patterns
- Patterns: exact IDs, title matching, characteristics

**Starting Cards Decision:**
- When playing starting cards (ARBITRARY_CARDS decision)
- ObjectiveHandler identifies which cards are required
- Ensures all objective requirements are met
- Prevents starting with incomplete objective

**Example (Clone Army Objective):**
- Objective: "Hunt For The Droid General" (221_67)
- Required: Kamino system, Clone Command Center, Cloning Cylinders, Grievous Will Run And Hide
- All must be selected as starting cards

---

### 6.6 ShieldStrategy.java
**Path:** `/models/rando/strategy/ShieldStrategy.java`  
**Purpose:** Manage defensive shield deployment strategy

**Shield Types:**
- Defensive shields (SWCCG-specific)
- Special objectives requiring shield deployment

**Strategy:**
- When to deploy shields
- Which shields to prioritize
- When shields have been used up

---

### 6.7 ObjectiveAnalyzer.java
**Path:** `/models/rando/strategy/ObjectiveAnalyzer.java`  
**Purpose:** Analyze objective card value and requirements

**Analysis:**
- Card combo requirements
- Deployment cost vs benefit
- Strategic value of objective

---

## 7. SAFETY & TRACKING

### 7.1 DecisionSafety.java
**Path:** `/models/rando/DecisionSafety.java`  
**Purpose:** CRITICAL - Guarantee valid response in all cases

**Core Philosophy:**
- EVERY decision MUST get a valid response
- Never return without posting decision
- Bad decision > no decision (game hangs)

**Known Decision Types:**
- MULTIPLE_CHOICE
- CARD_SELECTION
- CARD_ACTION_CHOICE
- ACTION_CHOICE
- INTEGER
- ARBITRARY_CARDS

**Key Methods:**

**`mustChoose(AwaitingDecision decision): boolean`**
- Check if must select something (cannot pass)
- Checks `noPass` parameter first
- Falls back to decision text analysis
- Looks for "must", "required" language

**`canPass(AwaitingDecision decision): boolean`**
- Can we pass/cancel?
- Checks for "may", "optional", "done", "cancel"
- Defaults to true

**`ensureValidResponse(AwaitingDecision, String response, String[] availableOptions): String[]`**
- CRITICAL VALIDATION
- If response empty but must choose:
  - Forces random selection from available
  - Returns [forcedResponse, "SAFETY FORCED"]
- Otherwise returns [response, ""]

**`getEmergencyResponse(AwaitingDecision, String[] actionIds, String[] cardIds): SafetyDecision`**
- LAST RESORT - called when all evaluators fail
- ALWAYS returns valid response
- Logs emergency with severity

**Emergency Response Logic:**
- `INTEGER` → return "0" (preserve resources)
- `MULTIPLE_CHOICE` → return "0" (first option) or "1" (avoid concede)
- `ACTION_CHOICE/CARD_ACTION_CHOICE` → random from available
- `CARD_SELECTION/ARBITRARY_CARDS` → random from available cards
- `UNKNOWN` → try actions, then cards, else "0"
- FINAL SAFETY: If must choose and response empty → force random

---

### 7.2 DecisionTracker.java
**Path:** `/models/rando/DecisionTracker.java`  
**Purpose:** CRITICAL - Detect and prevent infinite loops

**Loop Detection:**
- Tracks sequence of (decision_key, response) pairs
- Detects when 2-4 decision sequence repeats
- Escalates response when loop detected

**Key Concept: Multi-Decision Loops**
- NOT just same decision repeating
- Example: Decision A → B → A → B (2-decision loop)
- Or: A → B → C → A → B → C (3-decision loop)

**Thresholds (Escalating Response):**
- `LOOP_RANDOMIZE_THRESHOLD = 2` - After 2 repeats: add randomness
- `LOOP_FORCE_DIFFERENT = 6` - After 6 repeats: force different choice
- `LOOP_CRITICAL = 12` - After 12 repeats: consider conceding

**Key Methods:**

**`recordDecision(String decisionType, String decisionText, String decisionId, String response)`**
- Record every decision and response
- CRITICAL: Only non-pass responses tracked (passing can't cause loops)
- Triggers checkSequenceLoop()

**`updateState(int handSize, int forcePile, int reserveDeck, int turn, int cardsInPlay)`**
- Track game state
- Reset loop detection if state changes (not a real loop)
- Clear turn-specific blocked actions on turn change

**`checkSequenceLoop(): void` (private)**
- Check last 2, 3, or 4 entries for repeating pattern
- Count how many times sequence repeats
- Block the responses causing the loop

**`getBlockedResponses(String decisionType, String decisionText): Set<String>`**
- Return responses to block/penalize for this decision
- Used by evaluators for loop prevention
- Never blocks empty response (can't cause loop)

**`getLoopSeverity(): String`**
- "none", "mild", "moderate", "severe", "critical"
- Indicates how bad loop is

**`shouldForceDifferentChoice(): boolean`**
- sequenceRepeatCount >= 6

**`shouldConsiderConcede(): boolean`**
- sequenceRepeatCount >= 12

**`onPhaseChange(String newPhase): void`**
- Reset loop tracking on phase change
- Phase changes break loops

**`blockLastActionOnCancel(String decisionType, String decisionText): boolean`**
- Special case: block last CARD_ACTION_CHOICE when cancelling target selection
- Breaks pattern: Pick action → Cancel target → Back to action (loop!)

---

### 7.3 RandoLogger.java
**Path:** `/models/rando/RandoLogger.java`  
**Purpose:** Centralized logging for all Rando AI

**Logger Hierarchy:**
- `com.gempukku.swccgo.ai.models.rando` (base - WARN in production)
  - `.decision` - Decision processing
  - `.evaluator` - Evaluator scoring
  - `.strategy` - Strategy planning
  - `.safety` - Loop detection, critical issues

**Configuration:**
```xml
<!-- Add to log4j2.xml for debug: -->
<Logger name="com.gempukku.swccgo.ai.models.rando" level="DEBUG"/>
<Logger name="com.gempukku.swccgo.ai.models.rando.safety" level="WARN"/>
```

**Key Logging Methods:**
- `debug(String, Object...)` - Base logger debug
- `info(String, Object...)` - Base logger info
- `warn(String, Object...)` - Base logger warn
- `error(String, Object...)` - Base logger error
- `critical(String, Object...)` - Safety error (always logged)
- `loopDetected(String, Object...)` - Loop detection warning
- Plus emoji-prefixed convenience methods:
  - `success()`, `failure()`, `caution()`
  - `blocked()`, `target()`, `search()`
  - `shield()`, `battle()`, `deploy()`, `drain()`
  - `emergency()`, `barrier()`, `note()`

**Utility Methods:**
- `isDebugEnabled()` - Check before expensive operations
- `isEvaluatorDebugEnabled()` - Check evaluator logging

---

### 7.4 RandoConfig.java
**Path:** `/models/rando/RandoConfig.java`  
**Purpose:** Configuration constants (hardcoded, no external config files)

**Core Settings:**
- `MAX_HAND_SIZE = 16` - Hard cap for hand
- `HAND_SOFT_CAP = 12` - Start penalizing above
- `CHAOS_PERCENT = 0` - Random action chance (was 25%, now 0)

**Deploy Strategy:**
- `DEPLOY_THRESHOLD = 4` - Minimum power before committing
- `DEPLOY_OVERKILL_THRESHOLD = 8` - Stop reinforcing at this advantage
- `DEPLOY_COMFORTABLE_THRESHOLD = 4` - Reinforcement low priority threshold
- `DEPLOY_EARLY_GAME_THRESHOLD = 110` - Minimum score for weak plays early
- `DEPLOY_EARLY_GAME_TURNS = 3` - How many turns = early game

**Character Deployment:**
- `MIN_SOLO_DEPLOY_POWER = 6` - Minimum power to deploy alone
- `WEAK_CHARACTER_POWER = 3` - Character power threshold for needing buddy
- `MIN_ESTABLISH_POWER = 2` - Minimum for establish/early game

**Battle Strategy:**
- `BATTLE_FAVORABLE_THRESHOLD = 4` - "Good odds" for initiation
- `BATTLE_DANGER_THRESHOLD = -6` - Power diff to avoid battle
- `CRITICAL_LIFE_FORCE = 6` - Life force threshold for desperate play

**Scoring Weights:**
- `SCORE_REINFORCE_LOSING = 80` - Base bonus for reinforcing
- `SCORE_GAIN_GROUND = 60` - Bonus for gaining ground
- `SCORE_DEPLOY_LOCATION = 100` - Bonus for location deployment
- `SCORE_FORCE_DRAIN = 120` - Bonus for force drain
- `SCORE_INITIATE_BATTLE = 80` - Bonus for favorable battle
- `SCORE_MATCHING_PILOT = 40` - Bonus for pilot/ship match
- `SCORE_DAMAGE_CANCEL = 100` - Bonus for cancel interrupts
- `SCORE_BARRIER_USE = 80` - Bonus for barrier effects
- `SCORE_SENSE_USE = 70` - Bonus for Sense on high value

**Chat Settings:**
- `CHAT_ENABLED = true` - Send chat messages?
- `CHAT_MIN_INTERVAL_SECONDS = 3` - Cooldown between messages
- `CHAT_LIMIT_ONE_PER_TURN = true` - Max one message per turn

---

## 8. SUPPORTING UTILITIES

### 8.1 AiBoardAnalyzer.java
**Path:** `/common/AiBoardAnalyzer.java`  
**Purpose:** Analyze board state for strategic decisions

**Inner Class: ContestStatus**
- `WINNING` - More power/presence
- `LOSING` - Less power/presence
- `TIED` - Equal
- `UNCONTESTED` - Only one player present
- `EMPTY` - Nobody present

**Inner Class: LocationAnalysis**
```java
LocationAnalysis {
    PhysicalCard location
    float ourPower, theirPower, ourAbility, theirAbility
    int ourForceIcons, theirForceIcons
    int ourCardCount, theirCardCount
    ContestStatus status
    boolean isBattleground, isInterior, isExterior, isSite, isSystem
    boolean shouldFlee, isBattleOpportunity
    int locationIndex
}
```

**Key Methods:**
- `getPowerAdvantage()` - ourPower - theirPower
- `isContested()` - Both players have presence
- `weControl()` - ourAbility > 0 && theirAbility == 0
- `theyControl()` - theirAbility > 0 && ourAbility == 0

---

### 8.2 AiPriorityCards.java
**Path:** `/common/AiPriorityCards.java`  
**Purpose:** Register high-value cards that should be protected

**Priority Card Categories:**
- `DEFENSIVE` - Barrier cards (Imperial/Rebel Barrier)
- `DAMAGE_CANCEL` - Houjix/Ghhhk (cancel battle damage)
- `DESTINY` - Jedi Levitation/Sith Fury
- `DESTINY_BONUS` - +X to battle destiny
- `PROTECTION` - Character protection (Blaster Deflection)
- `UTILITY` - Cancel/retrieve cards
- `RETRIEVAL` - Card retrieval
- `STARTING` - Starting effect cards

**Key Data:**
- blueprintId, title, side (LIGHT/DARK)
- protectionScore (0-100, higher = more protected)
- usageNotes (strategic tips)

**Example Priorities:**
- Houjix/Ghhhk → Score 100 (CRITICAL)
- Jedi Levitation/Sith Fury → Score 90 (Very high value)
- Barrier cards → Score 80 (Important defense)
- Destiny bonus (+1) → Score 65 (Useful)

---

### 8.3 AiCardHelper.java
**Path:** `/common/AiCardHelper.java`  
**Purpose:** Helper methods for card properties

**Key Functionality:**
- Get card properties safely (handle null/exceptions)
- Match cards by blueprint/title
- Parse card text for keywords
- Calculate card value scores

---

### 8.4 AiDestinyCalculator.java
**Path:** `/common/AiDestinyCalculator.java`  
**Purpose:** Destiny calculation and analysis

**Functionality:**
- Calculate average destiny in deck/pile
- Simulate destiny outcomes
- Predict destiny draw distributions

---

### 8.5 AiChatManager.java
**Path:** `/common/AiChatManager.java`  
**Purpose:** Generate contextual chat messages

**Integration with RandoCalAi:**
- Implements `getChatMessage()` interface method
- Called between decisions for flavor messages
- Respects chat cooldown and per-turn limit
- Messages based on game state and events

---

## 9. COMPARISON: AI VARIANTS

| Feature | BeginnerAi | AdvancedAi | RandoCalAi |
|---------|-----------|-----------|-----------|
| **Base Class** | HeuristicAiBase | HeuristicAiBase | HeuristicAiBase |
| **Scoring Method** | Keyword weights | Keyword + board analysis | Full evaluator system |
| **Strategy** | None | Phase-aware | Full StrategyController |
| **Deck Knowledge** | None | None | DeckOracle (V22.6+) |
| **Battle Prediction** | Power comparison | Power + modifiers | Monte Carlo simulation |
| **Opponent Intel** | None | None | OpponentDeckTracker (V24.7+) |
| **Loop Detection** | None | None | DecisionTracker |
| **Deploy Planning** | None | None | DeployPhasePlanner |
| **Force Economy** | Simple | Better | Sophisticated |
| **Personality** | Generic | Generic | Named "Rando Cal" |
| **Chat** | No | No | Yes (configurable) |

---

## 10. DECISION FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────────────┐
│ RandoCalAi.decide(playerId, decision, gameState)               │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ├─ Validate playerId
                         ├─ Check if game initialized
                         │
                         ▼
            ┌──────────────────────────────┐
            │ buildEvaluatorContext()      │
            │  - Create DecisionContext    │
            │  - Parse decision params     │
            │  - Inject strategy comps     │
            │  - Refresh DeckOracle        │
            │  - Update DecisionTracker    │
            └──────────────┬───────────────┘
                           │
                           ▼
            ┌──────────────────────────────┐
            │ CombinedEvaluator.evaluate() │
            │  ┌────────────────────────┐  │
            │  │ ForceActivationEval    │  │
            │  ├────────────────────────┤  │
            │  │ DeployEvaluator        │  │
            │  ├────────────────────────┤  │
            │  │ BattleEvaluator        │  │
            │  ├────────────────────────┤  │
            │  │ MoveEvaluator          │  │
            │  ├────────────────────────┤  │
            │  │ DrawEvaluator          │  │
            │  ├────────────────────────┤  │
            │  │ CardSelectionEval      │  │
            │  ├────────────────────────┤  │
            │  │ ActionTextEvaluator    │  │
            │  ├────────────────────────┤  │
            │  │ PassEvaluator          │  │
            │  └────────────────────────┘  │
            │  - Merge scores             │
            │  - Pick best (highest score)│
            │  - Check if bad (< -100)    │
            └──────────────┬───────────────┘
                           │
                           ▼
            ┌──────────────────────────────┐
            │ DecisionTracker.recordDecision │
            │  - Track sequence            │
            │  - Detect loops             │
            │  - Update blocked responses │
            └──────────────┬───────────────┘
                           │
                           ▼
            ┌──────────────────────────────┐
            │ DecisionSafety.ensureValid() │
            │  - Validate response        │
            │  - Emergency fallback       │
            └──────────────┬───────────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │ Return Response │
                  └─────────────────┘
```

---

## 11. KEY PARAMETERS & HARDCODED VALUES

### Tunable Constants in RandoConfig

**Hand Size:**
- Target: 7 cards
- Soft cap: 12 cards
- Hard cap: 16 cards

**Force Economy:**
- Force pile cap: 25 (max_force_pile)
- Reserve for destiny: 4 cards (contested) or 1 card (safe)
- Force generation targets:
  - Early (T1-3): 8 icons
  - Mid (T4-8): 6 icons
  - Late (T9+): 5 icons

**Battle Thresholds:**
- Crush: +8 power advantage
- Favorable: +5 power advantage
- Marginal: +2 power advantage
- Risky: 0 power advantage
- Dangerous: -6 power disadvantage

**Life Force Critical:** 6 (desperate play threshold)

**Deploy Strategy:**
- Min solo power: 6 (don't deploy weak chars alone)
- Weak char power: 3 (needs buddy)
- Overkill threshold: 8 (stop reinforcing)

---

## 12. TODO/FIXME COMMENTS IN CODE

**RandoCalAi.java:**
- Decision safety needs stress testing
- Loop detection thresholds may need tuning
- Chat rate limiting could be configurable

**BattleEvaluator.java:**
- V22.4 Location-specific evaluation still has edge cases
- Suicide block threshold (2x power) may be too harsh
- Ability weighting (2.5x multiplier) could be refined

**DeployEvaluator.java:**
- V21 starting effect ban list may be incomplete
- Deploy threshold tuning based on deck analysis

**CardSelectionEvaluator.java:**
- V21 ban system for starting effects
- Blueprint lookup fallback needs improvement
- Target selection scoring could be more sophisticated

**ActionTextEvaluator.java:**
- V24.4 LOCATIONS FIRST rule may have false positives
- V24.15 AMSD exemption list needs expansion

---

## 13. VERSION HISTORY MARKERS

**V21:** Starting effect bans, card selection improvements  
**V22.4:** Location-specific battle evaluation, suicide block protection  
**V22.6:** DeckOracle introduction for full deck knowledge  
**V23:** Empty pile guard to block wasteful searches  
**V24.4:** LOCATIONS FIRST rule (deploy locations before effects)  
**V24.5:** No randomness on bad actions (always pass if possible)  
**V24.7:** OpponentDeckTracker, BattlePredictor with intel, destiny average tracking  
**V24.9:** "I'm Sorry" location search exception added  
**V24.10:** AMSD failed attempt tracking (turn-based retry)  
**V24.15:** AMSD exempt from LOCATIONS FIRST rule  

---

## 14. INTEGRATION POINTS

**Game Initialization:**
- `AiRegistry.register(gameId, playerId, RandoCalAi)` - Register AI

**Per-Game Initialization:**
- `ai.setGame(game)` - Provide full game reference

**Decision Loop:**
- `AiRegistry.get(gameId, playerId).decide(playerId, decision, gameState)` - Make decision

**Chat Integration:**
- `ai.getChatMessage()` - Get optional chat message

**Game End:**
- `AiRegistry.unregisterGame(gameId)` - Clean up

---

## 15. CRITICAL SAFETY SYSTEMS

### DecisionSafety Emergency Fallback
- **Trigger:** All evaluators fail or return null
- **Guarantee:** Always return valid response
- **Methods:** Random selection from available, preferring safe defaults
- **Logging:** Marked as EMERGENCY in logs

### DecisionTracker Loop Prevention
- **Mechanism:** Detect 2-4 decision repeating sequences
- **Escalation:** Randomize → Force different → Concede consideration
- **Blocked Responses:** Stored per decision, penalized during evaluation
- **Phase Reset:** Loop tracking resets on phase change

### Response Validation
- **Checks:** Empty string when must choose, unknown response IDs
- **Corrections:** Forces random selection if invalid
- **Logging:** All corrections logged as safety violations

---

## SUMMARY

The Rando Cal AI system is a sophisticated multi-layered decision-making engine with:

1. **Core Architecture:** Registry-based AI system with interface contract
2. **Decision Evaluation:** 9 specialized evaluators scored and merged
3. **Strategic Planning:** Full game state tracking and objective analysis
4. **Deck Intelligence:** Complete deck knowledge + opponent peek intel
5. **Loop Prevention:** Multi-decision loop detection with escalating response
6. **Safety Guarantees:** Emergency fallback ensures every decision gets response
7. **Configurability:** Hardcoded config constants for easy tuning
8. **Logging:** Centralized multi-tier logging for debugging

This is a reference document for understanding RandoCalAi's complete architecture, decision flow, and implementation details.
