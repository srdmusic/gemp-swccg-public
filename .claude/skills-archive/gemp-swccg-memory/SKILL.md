---
name: gemp-swccg-memory
description: "Persistent memory and quick-reference for GEMP-SWCCG development, Rando Cal AI improvement, and game client interaction. ALWAYS use this skill when working on anything related to GEMP, SWCCG, Rando, the game server, AI bots, card game mechanics, or the MCP game client. This skill contains accumulated project knowledge, file locations, API contracts, AI architecture details, and improvement observations that persist across sessions. Use it even for simple questions about the codebase — it prevents re-exploring files that have already been cataloged."
---

# GEMP-SWCCG Project Memory

This skill is the persistent knowledge base for the GEMP-SWCCG project. It contains everything needed to jump into a new session and be productive immediately.

## How to Use This Skill

1. **Starting a new session**: Read this SKILL.md first for quick orientation
2. **Working on Rando AI**: Read `references/rando-ai-catalog.md` for full AI architecture
3. **Building the MCP client**: Read `references/api-reference.md` for all API endpoints
4. **Understanding game mechanics**: Read `references/game-mechanics.md` for phases, cards, zones
5. **Reviewing past observations**: Read `references/improvement-log.md` for accumulated insights

## Quick Orientation

### What Is This Project?
GEMP-SWCCG is a Java-based web server for playing Star Wars Customizable Card Game online with automated rules enforcement. The project goal is improving **Rando Cal** — the elite AI bot player.

### Project Location
```
/sessions/clever-hopeful-dirac/mnt/gemp-swccg-public/src/
```

### Key Directories (Most Frequently Needed)

**Rando AI (primary work area):**
```
src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/
├── AiRegistry.java                    # AI player registry
├── SwccgAiController.java             # Interface all AIs implement
├── models/
│   ├── HeuristicAiBase.java           # Base class for all AIs
│   ├── BeginnerAi.java                # Simple keyword AI
│   ├── AdvancedAi.java                # Enhanced heuristic AI
│   └── rando/
│       ├── RandoCalAi.java            # Main elite AI class
│       ├── DecisionSafety.java        # Emergency response generator
│       ├── DecisionTracker.java        # Loop detection (CRITICAL)
│       ├── RandoLogger.java            # Centralized logging
│       ├── RandoConfig.java            # Tunable constants
│       ├── evaluators/
│       │   ├── CombinedEvaluator.java  # Merges all evaluator scores
│       │   ├── DecisionContext.java    # All decision info packaged
│       │   ├── EvaluatedAction.java    # Scored action with reasoning
│       │   ├── ActionType.java         # Action categories enum
│       │   ├── DeployEvaluator.java    # Card deployment scoring
│       │   ├── BattleEvaluator.java    # Battle decision scoring
│       │   ├── BattlePredictor.java    # Monte Carlo battle simulation
│       │   ├── MoveEvaluator.java      # Movement scoring
│       │   ├── DrawEvaluator.java      # Card draw scoring
│       │   ├── ForceActivationEvaluator.java  # Force activation amounts
│       │   ├── CardSelectionEvaluator.java    # Card targeting
│       │   ├── ActionTextEvaluator.java       # General action scoring
│       │   └── PassEvaluator.java             # Pass/cancel fallback
│       └── strategy/
│           ├── StrategyController.java   # Overall game strategy
│           ├── DeckOracle.java           # Full deck knowledge
│           ├── OpponentDeckTracker.java  # Opponent intel from peeks
│           ├── DeployPhasePlanner.java   # Deploy phase strategy
│           ├── ObjectiveHandler.java     # Objective card requirements
│           ├── ObjectiveAnalyzer.java    # Objective value analysis
│           └── ShieldStrategy.java       # Defensive shield mgmt
└── common/
    ├── AiBoardAnalyzer.java            # Board state analysis
    ├── AiPriorityCards.java            # High-value card registry
    ├── AiCardHelper.java               # Card property helpers
    ├── AiDestinyCalculator.java        # Destiny calculations
    └── AiChatManager.java              # Chat message generation
```

**Game Logic:**
```
src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/
├── decisions/          # Decision types (AwaitingDecision subclasses)
├── effects/            # 519+ game effects
├── modifiers/          # 564+ card modifiers
├── actions/            # Game actions (35+ dirs)
├── conditions/         # Card conditions
└── timing/             # Phase processes, TriggerConditions (287KB!)
```

**Web Server / API:**
```
src/gemp-swccg-async/src/main/java/com/gempukku/swccgo/async/handler/
├── LoginRequestHandler.java    # POST /login
├── HallRequestHandler.java     # Hall/lobby management
├── GameRequestHandler.java     # Game play (decisions, state)
└── DeckRequestHandler.java     # Deck CRUD
```

**Cards:** `src/gemp-swccg-cards/` (2000+ card definitions as Java classes)
**Common Enums:** `src/gemp-swccg-common/` (Phase, Zone, CardType, Side, etc.)
**Tests:** `src/gemp-swccg-server/src/test/`
**DB Scripts:** `src/db-scripts/` (schema, sample decks, initial users)
**Docker:** `src/docker-compose.yml`, `src/docker/`, `src/.env`
**Build Helper:** `src/bin/gemp` (initialize, rebuild, reload, etc.)

### Build & Run Cheatsheet
```bash
./bin/gemp initialize      # First-time setup
./bin/gemp rebuild-fast    # Recompile (skip tests)
./bin/gemp reload-fast     # Rebuild + restart
./bin/gemp logs            # Tail logs
./bin/gemp shell           # Bash into app container
./bin/gemp db-shell        # MySQL shell
./bin/gemp status          # Check health
```
Server runs at: `http://localhost:17001/gemp-swccg/`
Default creds: `test1` / `test`

### Rando AI Decision Flow (Quick Reference)
```
decide() → buildEvaluatorContext() → CombinedEvaluator.evaluate()
  ├── ForceActivationEvaluator (INTEGER decisions)
  ├── DeployEvaluator (deploy actions)
  ├── BattleEvaluator (battle actions + BattlePredictor)
  ├── MoveEvaluator (movement)
  ├── DrawEvaluator (card draws, draw phase only)
  ├── CardSelectionEvaluator (card targeting)
  ├── ActionTextEvaluator (general text patterns)
  └── PassEvaluator (pass/cancel fallback)
→ DecisionTracker.recordDecision() (loop detection)
→ DecisionSafety.ensureValidResponse() (guarantee valid response)
→ return response
```

### Key Tunable Constants (RandoConfig.java)
| Constant | Value | Purpose |
|----------|-------|---------|
| HAND_TARGET | 7 | Optimal hand size |
| MAX_FORCE_PILE | 25 | Cap on force activation |
| CRITICAL_LIFE_FORCE | 6 | Desperate play threshold |
| BATTLE_FAVORABLE_THRESHOLD | 4 | Power diff for "good odds" |
| BATTLE_DANGER_THRESHOLD | -6 | Power diff to avoid battle |
| MIN_SOLO_DEPLOY_POWER | 6 | Min power to deploy alone |
| SCORE_DEPLOY_LOCATION | 100 | Location deploy bonus |
| SCORE_FORCE_DRAIN | 120 | Force drain bonus |
| CHAOS_PERCENT | 0 | Random action chance (was 25%) |

### API Quick Reference (for MCP Client)
| Action | Method | Endpoint |
|--------|--------|----------|
| Login | POST | `/gemp-swccg-server/login` (login, password) |
| List tables | GET | `/gemp-swccg-server/hall` (participantId) |
| Create vs AI | POST | `/gemp-swccg-server/hall` (format, deckName, playVsAi=true, aiSkill=RANDO, aiDeckName) |
| Get game state | GET | `/gemp-swccg-server/game/{gameId}` (participantId) |
| Submit decision | POST | `/gemp-swccg-server/game/{gameId}` (participantId, channelNumber, decisionId, decisionValue) |
| Concede | POST | `/gemp-swccg-server/game/{gameId}/concede` |
| List decks | GET | `/gemp-swccg-server/deck/list` |
| Save deck | POST | `/gemp-swccg-server/deck` (deckName, deckContents) |

### Decision Types Quick Reference
| Type | Response Format |
|------|----------------|
| MULTIPLE_CHOICE | Index string: "0", "1", "2" |
| INTEGER | Number string: "5" |
| CARD_SELECTION | Comma-separated card IDs: "1,3,5" |
| ARBITRARY_CARDS | Comma-separated temp IDs: "temp0,temp2" |
| CARD_ACTION_CHOICE | Action index: "2" |
| ACTION_CHOICE | Action index: "2" |

### Version History of Rando Improvements
- V21: Starting effect bans, card selection improvements
- V22.4: Location-specific battle eval, suicide block
- V22.6: DeckOracle (full deck knowledge)
- V23: Empty pile guard (block wasteful searches)
- V24.4: LOCATIONS FIRST rule
- V24.5: No randomness on bad actions (always pass if possible)
- V24.7: OpponentDeckTracker, BattlePredictor with intel
- V24.10: AMSD failed attempt tracking
- V24.15: AMSD exempt from LOCATIONS FIRST

## Reference Files

For deeper dives, read these reference files:

- `references/rando-ai-catalog.md` — Complete catalog of every AI file, class, method, field, and tunable parameter. Read this when modifying Rando's behavior.
- `references/api-reference.md` — Full HTTP API contracts for building game clients. All endpoints, parameters, response formats, error codes.
- `references/game-mechanics.md` — Game phases, card types, zones, force mechanics, battle mechanics, build system, database schema.
- `references/improvement-log.md` — Accumulated observations from game analysis sessions. Append new findings here.
