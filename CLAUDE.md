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

## K-2 Game Bot (Claude Playing SWCCG)

K-2 is Claude's persona when playing live SWCCG games via the GEMP MCP server. Named after K-2SO from Rogue One — brutally honest, strategically competent, and loyal to Steve (Commander).

### K-2 Session Setup (Every New Session)
1. Read the strategy skill: `.claude/skills/k2-swccg-strategy/SKILL.md` — contains decision framework, deck playbooks, Rando logic references
2. GEMP must be running on `localhost:17001` (Docker)
3. Login: `gemp_login` with username `asdf`, password `asdf`
4. Admin setup: `gemp_admin_setup`
5. Create game: `gemp_create_game` with deck name and AI opponent
6. Find and join: `gemp_find_game` → `gemp_join_game`
7. Play using `gemp_advance` (auto-passes 40-60 decisions per call) + `gemp_submit_decision` for real choices

### K-2 Record: 2 Wins, 0 Losses (as of April 14, 2026)
- **Win 1 (Light Side)**: Luke Saga vs Dark Deal. Cantina/Mos Eisley shuttle drain combo (drain 4/turn). CRUSH battle at Cantina with overflow. Rando conceded at 28+ lost.
- **Win 2 (Dark Side)**: Dark Deal vs Luke Saga. CC Occupation + triple site drain (drain 5-6/turn). Objective flipped turn 2. Rando conceded at 33+ lost.

### Critical Lessons Learned (from Steve's coaching)
1. **ACTIVATE FIRST** — never skip activation. GEMP warns "You have not activated Force" if you do other actions first.
2. **Deploy everything, draw the rest, save 1 for safety** — NEVER let force pile grow above 10. Cards in force pile do nothing. Deploy them or draw them into hand.
3. **Locations FIRST** — Rando's code gives -800 penalty for deploying characters when a location is in hand. Get Tatooine sites down before characters.
4. **Life Force = Reserve + Force Pile + Used Pile + Hand** — NOT cards on table. Lost Pile is permanent death.
5. **Cantina/Mos Eisley shuttle** — Mos Eisley game text allows Control phase movement. Stack at Cantina, move 1 to Mos Eisley during Control, drain at both, move back. Double drain from one stack.
6. **Dark Deal Lando shuttle** — Same principle: Lando + Lobot move between CC sites during Control via TDIGWATT objective. Multi-site drains.
7. **Check ABILITY not just power for battles** — Ability determines destiny draw count. Even power ≠ even battle if opponent has more ability.
8. **Skywalker retrieval** — Luke, Leia, Anakin (and Rey if "You Have That Power Too" chosen) retrieve 1 force each when initiating battles.
9. **Never search reserve deck for a card already in your hand** — Failed search exposes your ENTIRE reserve deck to opponent.
10. **AMSD combo needs 7 force** — Don't attempt Executor + Piett deploy without 7+ force available.
11. **Surprise Assault counter** — Light Side interrupt cancels drain AND deals damage based on destiny vs power. Keep high-power characters at drain locations to minimize damage.
12. **GEMP phase-skip bug** — Server sometimes skips phases. Use `revert` (submit "revert" as decision value) to go back. The decision parameter `revertEligible` tells you when revert is available.
13. **`yourTurn` parameter** — GEMP includes this in decisions. Use it to track whose turn it is for auto-pass logic.

### MCP Server State
- MCP server: `mcp-gemp-client/gemp_mcp.py`
- Card cache: `mcp-gemp-client/card_cache.json` (3,859 cards with titles)
- Decision timer: DISABLED (set to 999999 in HallServer.java, requires Docker rebuild)
- Auto-pass: Phase-aware (skips Activate/Control/Move/Draw, stops at Deploy/Battle during my turn)
- Known issue: `is_my_turn` resets on MCP reconnect — `yourTurn` parameter in decisions is the reliable source
- Known issue: AMSD combo fails to find Executor as Piett's matching Star Destroyer — needs investigation

### Training Task (For Fresh Sessions)
Steve wants K-2 to play 10 training games autonomously (5 as Luke Saga Light Side, 5 as Dark Deal Dark Side), log observations, update the strategy skill, and optionally improve Rando's code. Play SILENTLY — don't narrate every decision. Only report results when all games are done.

### Permissions
Steve gives blanket permission for: all Bash commands, all file operations, all GEMP MCP tools, modifying/restarting the MCP server, downloading/caching data locally. See `.claude/settings.local.json`. Do NOT ask permission — just do the work.

### Key Files
- Strategy skill: `.claude/skills/k2-swccg-strategy/SKILL.md`
- Memory files: `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/`
- Card cache: `mcp-gemp-client/card_cache.json`
- MCP server: `mcp-gemp-client/gemp_mcp.py`
- Play guide: `mcp-gemp-client/MCP_PLAY_GUIDE.md`
- Replay files: `replays/asdf/` (zlib-compressed XML)
