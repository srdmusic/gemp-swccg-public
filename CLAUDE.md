# GEMP-SWCCG AI Bot Project

## Project Overview
GEMP-SWCCG is a Java-based online platform for playing the Star Wars Customizable Card Game (SWCCG). This repository contains the game engine, card definitions, and AI bots. The primary development focus is improving the AI bots: **Rando Cal** and **The Chosen One**.

## Owner
Steve (steve@srdmusic.com) — ASDF is Steve's in-game username.

## Build System
- Java 21, Maven multi-module project
- Build from `src/`: `mvn clean package -DskipTests`
- Modules: gemp-swccg-common, gemp-swccg-logic, gemp-swccg-cards, gemp-swccg-server, gemp-swccg-async
- Docker Compose available for deployment

## Repository Structure
```
src/
├── gemp-swccg-common/     # Shared enums, interfaces (CardCategory, Side, Phase, Zone, etc.)
├── gemp-swccg-logic/      # Game rules engine, modifiers, filters
├── gemp-swccg-cards/      # Card definitions (Card1_001.java through Card601_xxx.java)
├── gemp-swccg-server/     # Server + AI bots
│   └── .../ai/models/
│       ├── rando/          # Rando Cal AI bot
│       │   ├── RandoCalAi.java          # Main entry point
│       │   ├── RandoConfig.java         # Configuration constants
│       │   ├── evaluators/              # 9 evaluator files (Deploy, Move, Battle, etc.)
│       │   └── strategy/               # ObjectiveAnalyzer, DeckOracle, DeployPhasePlanner, etc.
│       └── chosenone/      # The Chosen One AI bot (parallel structure to rando/)
│           ├── TheChosenOneAi.java
│           ├── ChosenOneConfig.java
│           ├── evaluators/
│           └── strategy/
└── gemp-swccg-async/      # Web server, REST API
```

## AI Architecture
Both bots use the same architecture:
1. **CombinedEvaluator** — Aggregates floating-point scores from all evaluators, picks highest-scoring action
2. **Evaluators** — Each evaluator (Deploy, Move, Battle, CardSelection, ActionText, Draw, ForceActivation, Pass) scores each possible action
3. **Strategy layer** — ObjectiveAnalyzer (parses objective text), DeckOracle (tracks card zones), DeployPhasePlanner (multi-card deploy plans)
4. Actions scoring below `BAD_ACTION_THRESHOLD` (-100.0f) are rejected (bot passes instead)

## Current AI Version
**V32** — Latest changes include:
- V30: Universal matching pilot+starship deploy rule
- V31: Pre-flip vs post-flip objective deployment strategy
- V32: Ability >= 4 enforcement for deploy and move decisions
- See `context.md` for full version history V21-V32

## Key Game Mechanics for AI Development
- **Ability >= 4 threshold**: Must have total ability >= 4 at a site to draw battle destiny. Without this, you lose almost every battle.
- **Matching pilot+ship**: Card blueprints define matching via `setMatchingStarshipFilter`/`getMatchingStarshipFilter`. Matching pilot aboard matching ship gets power bonus.
- **Objective flip conditions**: Objectives have front (to flip) and back (to flip back) conditions. Usually require occupying/controlling specific locations.
- **Force economy**: Force pile = currency for deploying cards. Force generation = number of location icons you control.
- **TDIGWATT strategy**: Deploy Bespin system → Executor (via AMSD or hand) → Piett (pulls Gherant) → occupy 3 Bespin locs to flip → consolidate to 2 locs post-flip.

## Important Files to Know
- `context.md` — Full changelog V21-V29.13 with score values and reasoning
- `HANDOFF.md` — Comprehensive handoff document with all V30-V32 changes, SWCCG mechanics, and pending work
- Card definitions: `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set*/dark/` and `.../light/`
- Replay files: `replays/` directory (zlib-compressed XML)

## Development Guidelines
- All changes to Rando MUST also be applied to The Chosen One (parallel files with `chosenone` package references)
- Use `chosenone.strategy.ObjectiveAnalyzer` not `rando.strategy.ObjectiveAnalyzer` in Chosen One files
- Use `ChosenOneConfig` not `RandoConfig` in Chosen One files
- Score magnitudes: +500-1500 critical, +200-400 high priority, +50-150 tactical, -30 to -80 mild penalty, -200 to -500 strong penalty, -9999 hard block
- Always add LOG.warn() statements for new rules to aid debugging
- Version tag all new rules (V33, V34, etc.) in comments
- Steve's philosophy: aggressive deployment, understand card combos, always maintain ability >= 4 at sites, match pilots to ships, objective-aware location priority
