# Rando Cal AI - Java Port

This package contains the **Rando Cal** AI for GEMP SWCCG, ported from the Python bot at `new_rando/`.

Rando Cal is an advanced AI with personality that plays Star Wars CCG with strategic decision-making, chat messages, and holiday overlays.

## Package Structure

```
ai/rando/
├── RandoCalAi.java          # Main AI class (extends HeuristicAiBase)
├── RandoConfig.java         # Configuration constants
├── RandoLogger.java         # Centralized logging (configurable by category)
├── RandoContext.java        # Per-decision game context (internal to RandoCalAi)
│
├── DecisionTracker.java     # Loop detection and blocked response tracking
├── DecisionSafety.java      # Emergency response handling (never hang)
│
├── AstrogatorPersonality.java  # Chat personality and messages
├── HolidayOverlay.java         # Date-based holiday message variants
│
├── evaluators/              # Scoring-based decision evaluators
│   ├── ActionEvaluator.java        # Base interface for all evaluators
│   ├── CombinedEvaluator.java      # Orchestrates multiple evaluators
│   ├── DecisionContext.java        # Context passed to evaluators
│   ├── EvaluatedAction.java        # Scored action with reasoning
│   │
│   ├── ForceActivationEvaluator.java  # INTEGER decisions (force activation)
│   ├── DeployEvaluator.java           # Deploy phase decisions
│   ├── BattleEvaluator.java           # Battle initiation/tactics
│   ├── MoveEvaluator.java             # Movement decisions
│   └── CardSelectionEvaluator.java    # Card selection/targeting
│
└── strategy/                # Strategic planning components
    ├── DeployStrategy.java       # Deployment strategy enum
    ├── DeploymentPlan.java       # Complete deployment plan
    ├── DeploymentInstruction.java # Single deployment instruction
    ├── DeployPhasePlanner.java   # Holistic deploy phase planning
    ├── LocationAnalysis.java     # Location analysis for targeting
    │
    ├── ShieldStrategy.java       # Defensive shield selection
    ├── ObjectiveHandler.java     # Objective card requirements
    └── StrategyController.java   # Game-wide strategy coordinator
```

## Architecture Overview

### Decision Flow

```
AwaitingDecision from GEMP
         │
         ▼
┌─────────────────────────────────────┐
│         RandoCalAi.decide()         │
│                                     │
│  1. Build RandoContext              │
│  2. Update DecisionTracker state    │
│  3. Check for loops                 │
│  4. Try CombinedEvaluator           │
│  5. Fall back to keyword heuristics │
│  6. Record decision for tracking    │
└─────────────────────────────────────┘
         │
         ▼
    Response string
```

### Evaluator System

Each evaluator implements `ActionEvaluator`:

```java
public interface ActionEvaluator {
    boolean canEvaluate(DecisionContext context);
    List<EvaluatedAction> evaluate(DecisionContext context);
}
```

The `CombinedEvaluator` tries each registered evaluator and picks the highest-scored action:

```java
CombinedEvaluator
├── ForceActivationEvaluator  (INTEGER decisions)
├── DeployEvaluator           (CARD_ACTION_CHOICE in Deploy phase)
├── BattleEvaluator           (CARD_ACTION_CHOICE in Battle phase)
├── MoveEvaluator             (CARD_ACTION_CHOICE with move keywords)
└── CardSelectionEvaluator    (CARD_SELECTION, ARBITRARY_CARDS)
```

**Scoring scale:**
- 0-20: Low priority
- 20-50: Moderate priority
- 50-80: High priority
- 80+: Critical priority
- Negative: Avoid/penalize

### Loop Detection

`DecisionTracker` detects multi-decision loops (e.g., A→B→A→B cycles):

1. Tracks sequence of (decision_key, response, state_hash) tuples
2. Detects repeating sequences of length 2-4
3. Blocks responses that caused loops
4. Escalating thresholds:
   - 2 repeats: Add randomness
   - 6 repeats: Force different choice
   - 12 repeats: Consider conceding

State changes (hand size, force pile, etc.) reset loop detection since they indicate progress.

### Strategy Components

**DeployPhasePlanner** creates holistic deployment plans:
1. Deploy locations first (opens options)
2. Reinforce losing locations (reduce harm)
3. Establish at opponent-only locations (gain ground)
4. Build up winning locations (avoid overkill)

**StrategyController** tracks game-wide state:
- Game phase (EARLY/MID/LATE)
- Force economy and targets
- Strategy focus (GROUND/SPACE/BALANCED)
- Battle Order rules detection

**ShieldStrategy** scores defensive shields:
- AUTO_PLAY_IMMEDIATE: Play turns 1-2
- AUTO_PLAY_EARLY: Play before opponent drains
- SITUATIONAL_HIGH/MEDIUM: Based on opponent deck
- Shield pacing to reserve slots for reactions

## Logging Configuration

All logging uses `RandoLogger` with configurable categories:

```
com.gempukku.swccgo.ai.models.rando          (base)
com.gempukku.swccgo.ai.models.rando.decision (decision processing)
com.gempukku.swccgo.ai.models.rando.evaluator (evaluator scoring)
com.gempukku.swccgo.ai.models.rando.strategy (strategy planning)
com.gempukku.swccgo.ai.models.rando.safety   (loop detection, critical)
```

Configure in `log4j2.xml`:

```xml
<!-- Production: Only warnings/errors -->
<Logger name="com.gempukku.swccgo.ai.models.rando" level="WARN" additivity="false">
    <AppenderRef ref="Console"/>
</Logger>

<!-- Debug: Verbose logging -->
<Logger name="com.gempukku.swccgo.ai.models.rando" level="DEBUG" additivity="false">
    <AppenderRef ref="Console"/>
</Logger>

<!-- Only safety/critical issues -->
<Logger name="com.gempukku.swccgo.ai.models.rando.safety" level="INFO" additivity="false">
    <AppenderRef ref="Console"/>
</Logger>
```

## Key Configuration Constants (RandoConfig.java)

| Constant | Default | Description |
|----------|---------|-------------|
| `CHAOS_PERCENT` | 5 | % chance of random action |
| `BATTLE_FAVORABLE_THRESHOLD` | 4 | Power advantage for favorable battle |
| `BATTLE_DANGER_THRESHOLD` | -6 | Power deficit for dangerous battle |
| `DEPLOY_THRESHOLD` | 4 | Min deployable power before committing |
| `FORCE_GEN_TARGET` | 6 | Target force icon count |
| `HAND_SOFT_CAP` | 12 | Soft limit on hand size |
| `MAX_HAND_SIZE` | 16 | Hard limit on hand size |
| `CRITICAL_LIFE_FORCE` | 6 | Life force for desperate play |
| `CHAT_ENABLED` | true | Enable personality chat |

## Adding a New Evaluator

1. Create class in `evaluators/` implementing `ActionEvaluator`:

```java
public class MyEvaluator implements ActionEvaluator {
    private static final Logger LOG = RandoLogger.getEvaluatorLogger();

    @Override
    public boolean canEvaluate(DecisionContext context) {
        // Return true if this evaluator handles this decision type
        return "CARD_ACTION_CHOICE".equals(context.getDecisionType())
            && context.getDecisionText().contains("my keyword");
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        // Score each available action
        for (String actionId : context.getActionIds()) {
            float score = calculateScore(actionId, context);
            EvaluatedAction action = new EvaluatedAction(actionId, score);
            action.addReasoning("My reason for this score");
            actions.add(action);
        }
        return actions;
    }
}
```

2. Register in `CombinedEvaluator.initializeEvaluators()`:

```java
private void initializeEvaluators() {
    evaluators.add(new ForceActivationEvaluator());
    evaluators.add(new DeployEvaluator());
    // ... existing evaluators ...
    evaluators.add(new MyEvaluator());  // Add here
}
```

## Common Patterns

### Using Board Analysis

```java
// Get locations where we're losing
List<LocationAnalysis> losing = AiBoardAnalyzer.getLosingLocations(
    game, playerId, opponentId, mySide);

// Calculate overall board advantage
float advantage = AiBoardAnalyzer.calculateBoardAdvantage(
    game, playerId, opponentId, mySide);
```

### Checking Priority Cards

```java
// Is this a high-value card?
if (AiPriorityCards.isPriorityCard(blueprintId)) {
    score += 50;
}

// Should we use Sense on this target?
SenseTargetResult result = AiPriorityCards.getSenseTargetValue(targetText);
if (result.isHighValue) {
    score += result.score;
}
```

### Adding Reasoning to Actions

Always add reasoning for debugging:

```java
EvaluatedAction action = new EvaluatedAction(actionId, score);
action.addReasoning("Base score: " + baseScore);
action.addReasoning("Location bonus: +" + locationBonus);
action.addReasoning("Priority card: +" + priorityBonus);
```

## Relationship to Python Bot

This is a port of `new_rando/` Python bot. Key mappings:

| Python | Java |
|--------|------|
| `engine/evaluators/` | `evaluators/` |
| `engine/deploy_planner.py` | `strategy/DeployPhasePlanner.java` |
| `engine/strategy_controller.py` | `strategy/StrategyController.java` |
| `engine/shield_strategy.py` | `strategy/ShieldStrategy.java` |
| `engine/objective_handler.py` | `strategy/ObjectiveHandler.java` |
| `engine/priority_cards.py` | `ai/common/AiPriorityCards.java` |
| `engine/decision_safety.py` | `DecisionSafety.java`, `DecisionTracker.java` |
| `brain/astrogator_brain.py` | `AstrogatorPersonality.java` |
| `brain/holiday_overlay.py` | `HolidayOverlay.java` |
| `config.py` | `RandoConfig.java` |

## Testing

Run from project root:

```bash
mvn test -pl gemp-swccg-server -Dtest=RandoCalAiTest
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Bot hangs | Check `DecisionSafety` - should never happen with emergency responses |
| Loop detected | Check `DecisionTracker` logs, verify state changes reset detection |
| Wrong action chosen | Enable DEBUG logging for `evaluator` category, check reasoning |
| Evaluator not firing | Verify `canEvaluate()` matches decision type/text patterns |
| Missing chat messages | Check `RandoConfig.CHAT_ENABLED` and `AiChatManager` rate limits |

## Files NOT Ported (per design)

- SQLite persistence / stats tracking
- Achievement system
- Neural deploy planner
- Monte Carlo simulations
- Multi-turn game plan
- Deck analyzer / archetype detector
- Chat command handler
