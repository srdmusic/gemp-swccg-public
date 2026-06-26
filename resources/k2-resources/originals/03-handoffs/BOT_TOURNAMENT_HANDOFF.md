# Bot Tournament Handoff

How to run bot-vs-bot games on GEMP-SWCCG, what works, what doesn't, and every workaround we've tried.

---

## 1. Architecture Overview

GEMP runs in Docker on Steve's Mac. Three containers matter:

- **gemp_swccg_app_1** (port 17001): Java app server, runs the game engine and AI bots
- **gemp_swccg_db_1** (port 35001): MariaDB, stores decks, players, game history
- **build** container: Used for in-container Maven builds

The bots (`~The_Chosen_One` and `~Rando_Cal`) run server-side inside the JVM. They don't need a client. When a bot-vs-bot game is created, the server plays both sides automatically with no human input.

The challenge is *observing* the game from outside to know when it finishes and to collect results.

---

## 2. Creating a Bot Game

### Admin API Endpoint

```
POST http://localhost:17001/gemp-swccg-server/admin/botgame
```

Required parameters (form-encoded):

| Parameter | Example | Notes |
|-----------|---------|-------|
| `format` | `open` | Game format code |
| `lightSkill` | `CHOSENONE` | AI skill: `CHOSENONE`, `RANDO`, `ADVANCED`, or `BEGINNER` |
| `lightDeck` | `LUKE SAGA TATOOINE` | Exact deck name as stored in DB |
| `darkSkill` | `RANDO` | Same skill options |
| `darkDeck` | `DARK DEAL` | Exact deck name |
| `deckOwner` | `test1` | Player account that owns the decks (REQUIRED) |

Returns: `OK gameId=<uuid-style-id>` on success.

### Prerequisites

Before creating a game, the admin must:

1. **Login** as an admin user (e.g., `test1` with password `test`)
2. **Disable shutdown mode**: `POST /gemp-swccg-server/admin/shutdown` with `enabled=false`
3. **Enable AI tables**: `POST /gemp-swccg-server/admin/settings/aitables` with `enabled=true`

### Bot Player IDs

- `~The_Chosen_One` maps to `TheChosenOneAi` (skill string: `CHOSENONE`)
- `~Rando_Cal` maps to `RandoCalAi` (skill string: `RANDO`)

### Deck Ownership

Decks must exist under the `deckOwner` account. The deck name must match exactly (case-sensitive). If the deck doesn't exist or the side is wrong, the server returns an error.

---

## 3. The Working Single-Game Watcher

File: `mcp-gemp-client/watch_bot_game.py`

This is the **proven, reliable** script for watching a single bot game. It uses the `BotGameWatcher` class.

### Usage

```bash
cd /Users/steve/gemp-swccg-public/mcp-gemp-client

python3 watch_bot_game.py \
  --base-url http://localhost:17001 \
  --user test1 --password test \
  --format open \
  --light-skill CHOSENONE --light-deck "LUKE SAGA TATOOINE" \
  --dark-skill RANDO --dark-deck "DARK DEAL" \
  --deck-owner test1
```

### How It Works

1. Logs in via `POST /gemp-swccg-server/login`
2. Ensures server is running and AI tables are enabled
3. Creates the bot game via `POST /gemp-swccg-server/admin/botgame`
4. Signs up as spectator via `GET /gemp-swccg-server/game/{gameId}?participantId={username}`
5. Polls for updates via `POST /gemp-swccg-server/game/{gameId}` with `participantId` and `channelNumber`
6. Parses XML responses for game events (messages, phase changes, decisions, game over)
7. Detects game over when a message contains "is the winner", "lost due to", or "conceded"

### Critical Technical Detail: New Client Per Request

The single most important thing about GEMP's spectator API:

**You MUST create a new `httpx.AsyncClient` for every single HTTP request.**

This is non-negotiable. Using a persistent/session client (e.g., `async with httpx.AsyncClient() as client:` wrapping the whole poll loop) results in **zero messages received**. We tested this. The spectator endpoint stops responding.

The working pattern:

```python
async def poll(self):
    async with httpx.AsyncClient(timeout=12.0) as client:  # NEW client each call
        resp = await client.post(
            f"{self.base_url}/gemp-swccg-server/game/{self.game_id}",
            data={
                "participantId": self.username,
                "channelNumber": str(self.channel_number),
            },
            headers=self._headers("game.html"),
        )
    # ... parse response
```

Cookie management is also manual (stored in a dict, sent via Cookie header). Do not rely on httpx's built-in cookie jar.

### Channel Number Tracking

The server returns a `cn` attribute in the XML root element. This is the channel number for the next poll. You must update `self.channel_number` from this value each time, or the server will re-send old events or return empty responses.

### HTTP 410 Handling

If the spectator endpoint returns 410, the spectator session has expired. Re-call `spectate_signup()` and continue polling.

---

## 4. The Tournament Runner Problem

We need to run N games sequentially, collect results, and analyze patterns. The single-game watcher works fine for one game but getting it to run multiple games in sequence has been the main source of pain.

### Approach 1: Game History API Polling (FAILED)

**Idea:** After creating a game, poll `GET /gemp-swccg-server/gameHistory?participantId=~The_Chosen_One&start=0&count=1` and wait for the game count to increment, indicating the game finished.

**Result:** The count never incremented from the script's perspective. Likely an auth/cookie issue with the gameHistory endpoint. The endpoint works fine in a browser but the script's cookies don't carry over properly.

**Verdict:** Abandoned. Don't use this approach.

### Approach 2: Persistent httpx Client (FAILED)

**Idea:** Create one `httpx.AsyncClient` and reuse it for all requests within a game.

**Result:** Received 0 messages. The spectator polling returned empty XML every time.

**Root cause:** GEMP's spectator API does not work with persistent HTTP clients. Reason unknown (possibly server-side session management, connection pooling, or cookie behavior).

**Verdict:** Abandoned. Always create a new client per request.

### Approach 3: BotGameWatcher Spectator Loop (PARTIALLY WORKS)

**Idea:** Import and reuse the proven `BotGameWatcher` class from `watch_bot_game.py` for each game in the tournament. Create a new `BotGameWatcher` instance per game. Wait for game_over, then start the next game.

**Result:** Got through 6-12 games reliably, then got stuck. The spectator polling would stop receiving messages even though the game had finished on the server.

**Symptoms when stuck:**
- Spectating channel 0, receiving 0 messages
- The game actually finished on the server (you can see it in game history via the web UI)
- The script's poll just returns empty updates forever

**Observations:**
- The server sometimes runs 2 bot games simultaneously. We saw 12 games complete but the server was running them 2 at a time (6 batches of 2). This was unexpected.
- After approximately 6 games, the spectator channel goes stale
- The stuck condition happens more reliably after many games in sequence

### Mitigations That Helped (But Didn't Fully Solve)

1. **Error streak detection:** If 15+ consecutive polls return errors or empty data, assume the game finished and move on
2. **404 detection:** If the game endpoint returns 404, the game is definitely over
3. **Heartbeat timeout:** If no new messages for 30+ seconds, assume stuck and move on
4. **Inter-game pause:** Wait 5-10 seconds between games to let the server clean up
5. **Fresh BotGameWatcher per game:** Create a completely new instance (new cookies, new login) for each game

---

## 5. Tournament Runner Script (NEEDS RECREATION)

The `run_bot_tournament.py` script was lost between sessions. It needs to be recreated. Here's the design that worked best (V5):

### Architecture

```
run_bot_tournament.py
  |
  +-- imports BotGameWatcher from watch_bot_game.py
  |
  +-- for each game 1..N:
  |     +-- create new BotGameWatcher instance
  |     +-- login
  |     +-- create bot game
  |     +-- spectate and poll until game_over or timeout
  |     +-- save game result to results dict
  |     +-- pause 5-10 seconds
  |
  +-- save tournament_results/tournament_TIMESTAMP.json
  +-- save per-game message logs to tournament_results/game_messages/
```

### Key Parameters

```python
MAX_POLLS_PER_GAME = 5000        # Safety limit per game
STUCK_THRESHOLD_SECONDS = 120    # If no progress for 2 min, move on
ERROR_STREAK_LIMIT = 15          # 15 consecutive errors = give up on this game
INTER_GAME_PAUSE = 8             # Seconds between games
```

### Output Format

The tournament results JSON should contain:

```json
{
  "tournament_id": "tournament_20260415_120000",
  "config": {
    "num_games": 15,
    "format": "open",
    "light_skill": "CHOSENONE",
    "light_deck": "LUKE SAGA TATOOINE",
    "dark_skill": "RANDO",
    "dark_deck": "DARK DEAL"
  },
  "results": [
    {
      "game_number": 1,
      "game_id": "abc123...",
      "winner": "~The_Chosen_One",
      "result_message": "~The_Chosen_One is the winner...",
      "elapsed_seconds": 45.2,
      "decision_count": 312,
      "message_count": 89,
      "status": "completed"
    }
  ],
  "summary": {
    "total_games": 15,
    "completed": 12,
    "stuck": 3,
    "light_wins": 3,
    "dark_wins": 9
  }
}
```

---

## 6. Replay Files and Analysis

### Replay File Location

```
/Users/steve/gemp-swccg-public/replays/{playerId}/{gameRecordingId}.xml.gz
```

Player IDs in the path use tilde: `~The_Chosen_One`, `~Rando_Cal`

### Reading Replays

Replay files are **zlib-compressed** (NOT gzip). Using `gzip.decompress()` will fail.

```python
import zlib

with open(f'replays/~Rando_Cal/{game_id}.xml.gz', 'rb') as f:
    xml_text = zlib.decompress(f.read()).decode('utf-8', errors='replace')
```

### Replay API

You can also fetch replays via HTTP:

```
GET /gemp-swccg-server/replay/{playerId}${gameRecordingId}
```

Note the `$` separator between playerId and gameRecordingId.

### Game History API

```
GET /gemp-swccg-server/gameHistory?participantId={playerId}&start=0&count=50
```

Returns XML with `<historyEntry>` elements containing game IDs, winners, timestamps.

### What to Extract from Replays

The replay XML contains `<ge>` (game event) elements with types:

- `type="M"` (message): `message` attribute has the game log text (deploys, moves, battles, force drains, etc.)
- `type="GPC"` (game phase change): `phase` attribute
- `type="D"` (decision): AI decision events
- `type="GS"` (game stats): Player zone counts (hand, reserve, used, lost, force pile)

Key things to look for in messages:
- Starting locations: "deploys ... from Reserve Deck" early in the game
- Force drains: "Force drain of X at ..."
- Retrievals: "retrieves X Force"
- Battles: "initiates a battle at ...", "forfeits ..."
- Winner: "is the winner", "lost due to"

---

## 7. Analysis Script (NEEDS RECREATION)

The `analyze_bot_tournament.py` script was also lost. It should:

1. Read the tournament results JSON
2. For each game, fetch the replay (either from disk or API)
3. Parse all messages from the replay
4. Extract stats per game:
   - Starting location for each side
   - Starting effect/interrupt for each side
   - Cards deployed (with timestamps/turn numbers)
   - Force drains executed (location, amount)
   - Retrievals (source, amount)
   - Battle outcomes
   - Cards placed out of play
   - Final result and win reason
5. Generate an aggregate analysis showing:
   - Win rate by side
   - Most common starting locations
   - Force drain frequency (the #1 predictor of win/loss per our analysis)
   - Common card plays
   - Patterns in wins vs losses

### Previous Analysis Finding

From our 31-game analysis, the key finding was: **Light Side (Chosen One) wins only 23% of games.** The primary predictor of win/loss was force drain frequency. In losses, Chosen One had near-zero force drains. Improving force drain strategy is the #1 priority.

---

## 8. Server Rebuild After Code Changes

### Fast Path (most common, code-only changes)

```bash
cd /Users/steve/gemp-swccg-public/src
mvn -q -pl gemp-swccg-async -am package -DskipTests

# Restart via helper script
/Users/steve/gemp-swccg-public/bin/gemp restart

# Verify
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:17001/gemp-swccg/
# Should return 200
```

Takes about 60 seconds. The repo uses bind mounts so `mvn package` output is already visible inside the container.

### In-Container Build (alternative)

```bash
docker compose exec -T build bash -c "cd /opt/gemp-swccg/src && mvn install -DskipTests"
docker compose restart build
```

### Full Nuke (only if Docker/DB changes)

```bash
# BACKUP FIRST
docker exec gemp_swccg_db_1 mariadb-dump -uroot -pgempukku --all-databases > ~/gemp_db_backup_$(date +%Y%m%d_%H%M%S).sql

cd /Users/steve/gemp-swccg-public/src && mvn clean install -DskipTests -q
docker compose down          # NO -v flag!
docker compose build --no-cache
docker compose up -d
sleep 15 && docker restart gemp_swccg_app_1
sleep 5 && docker exec gemp_swccg_app_1 bash -c \
    'cd /opt/gemp-swccg/web && unzip -o /opt/gemp-swccg/src/gemp-swccg-async/target/web.zip > /dev/null && echo OK'
/Users/steve/gemp-swccg-public/bin/gemp operational
```

---

## 9. Database Reference

- **Host:** localhost:35001
- **Root password:** `gempukku`
- **GEMP user password:** `Four_mason8pirate`
- **Admin flag:** Character `a` in the player `type` field

---

## 10. AI Bot Configuration

### Bot Skill Strings to AI Classes

| Skill String | AI Class | Player ID |
|-------------|----------|-----------|
| `CHOSENONE` | `TheChosenOneAi` | `~The_Chosen_One` |
| `RANDO` | `RandoCalAi` | `~Rando_Cal` |
| `ADVANCED` | `AdvancedAi` | `~Advanced_AI` |
| `BEGINNER` | `BeginnerAi` | (default) |

### AI Source Locations

```
src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/
  +-- SwccgAiController.java          (interface)
  +-- AiRegistry.java                 (game-to-AI mapping)
  +-- models/
       +-- chosenone/
       |    +-- TheChosenOneAi.java
       |    +-- evaluators/
       |         +-- ActionTextEvaluator.java
       |         +-- CardSelectionEvaluator.java
       |         +-- DecisionContext.java
       |         +-- ...
       +-- rando/
            +-- RandoCalAi.java
            +-- evaluators/
                 +-- (same structure as chosenone)
```

### Decision Flow

1. Game engine sends an `AwaitingDecision` to the AI
2. `SwccgGameMediator` looks up the AI from `AiRegistry`
3. Calls `ai.setGame(game)` then `ai.decide(playerId, decision, gameState)`
4. The AI builds a `DecisionContext` and routes to the appropriate evaluator
5. The evaluator scores all options and returns the highest-scored choice

### Decision Types

| Type | What It Is | Example |
|------|-----------|---------|
| `MULTIPLE_CHOICE` | Pick one text option | "Select OK to start game", "Choose an option" |
| `ARBITRARY_CARDS` | Pick card(s) from a set | "Choose starting location", "Choose cards to deploy" |
| `CARD_ACTION_CHOICE` | Pick an action for a card | "Activate Force", "Deploy", "Fire" |
| `ACTION_CHOICE` | General action choice | Same as above |
| `INTEGER` | Pick a number | "Choose amount of Force to activate" |

### Decision Routing in CardSelectionEvaluator

The `evaluate()` method uses an else-if chain matching keywords in the decision text. **Order matters.** A previous bug was caused by "choose...location" matching before "starting location" because it appeared first in the chain:

```java
// CORRECT ORDER (specific before general):
} else if (textLower.contains("starting location")) {
    return evaluateStartingLocation(context);
} else if (textLower.contains("choose") && textLower.contains("location")) {
    return evaluateLocationSelection(context);
```

---

## 11. Recent V29.15 Changes (This Session)

### Epic Event Saga Choice

The Skywalker Epic Event ("The Force Is Strong In My Family") presents three saga choices. The correct choice depends on the deck name:

| Deck contains | Correct choice |
|--------------|---------------|
| "Luke" | "I Have It" |
| "Anakin" | "My Father Has It" |
| "Rey" | "You Have That Power, Too" |

Implementation required threading the deck name through the system:

1. `SwccgAiController.java`: Added `default void setDeckName(String)` to the interface
2. `HallServer.java`: Calls `ai.setDeckName(deckName)` when creating bot games (both bot-vs-bot and human-vs-bot paths)
3. `TheChosenOneAi.java` / `RandoCalAi.java`: Store the deck name, pass to DecisionContext
4. Both `DecisionContext.java` classes: Added `deckName` field with getter/setter
5. Both `ActionTextEvaluator.java` classes: Added saga choice scoring (+1000 correct, -500 wrong, +500 default to "I Have It" if no deck name)

Also expanded `canEvaluate` in both ActionTextEvaluators to handle `"choose an option"` MULTIPLE_CHOICE decisions (previously only handled `"capacity slot"`).

### Epic Starting Effect (V29.15)

Both bots now give +1000 bonus to any starting effect/interrupt whose game text contains "epic". This ensures bots pick starting cards that synergize with their Epic Events (e.g., "Rise of Skywalker" over "The Signal").

### Starting Location Routing Fix

Swapped the else-if order in both CardSelectionEvaluators so `"starting location"` is checked before `"choose" + "location"`. Without this fix, the `evaluateStartingLocation()` method (which has the Funeral Pyre +1000 bonus) was never called.

---

## 12. Unsolved Problems

### Tournament Runner Reliability

The spectator-based approach stalls after ~6-12 games. Possible causes:

- Server-side spectator session cleanup
- Cookie/session expiration after extended use
- The server running 2 games simultaneously when we expected 1
- Channel number going stale

### Potential Solutions Not Yet Tried

1. **Skip spectating entirely.** Create all N games up front, wait a fixed time (bot games typically take 30-60 seconds), then just query game history to get results. Avoids all spectator API issues.

2. **Query the database directly.** After creating a game, poll the MariaDB `game_history` table to detect completion. Bypasses the HTTP API entirely.

3. **Use the replay file system.** After creating a game, watch the `replays/` directory for the game's replay file to appear. When it exists, the game is done.

4. **Shorter timeout with retry.** Instead of waiting forever for a stuck game, timeout after 90 seconds and create a new game. Accept that some games will be "lost" and just run extra games to compensate.

5. **Run games one at a time with full cleanup.** After each game, wait 15 seconds, then verify no running games exist via the hall API before starting the next one.

---

## 13. File Inventory

| File | Status | Purpose |
|------|--------|---------|
| `mcp-gemp-client/watch_bot_game.py` | EXISTS, WORKING | Single-game watcher with BotGameWatcher class |
| `mcp-gemp-client/run_bot_tournament.py` | MISSING (needs recreation) | Multi-game tournament runner |
| `mcp-gemp-client/analyze_bot_tournament.py` | MISSING (needs recreation) | Replay parser and analysis report generator |
| `mcp-gemp-client/game_logs/` | EXISTS | 60 JSONL game log files from previous runs |
| `mcp-gemp-client/tournament_results/` | EXISTS (empty) | Target directory for tournament output |
| `mcp-gemp-client/card_cache.json` | EXISTS | Card data cache for analysis |

---

## 14. Quick Start Checklist

For any session picking up this work:

1. Read `BUILD_AND_DEPLOY.md` for build/restart commands
2. Verify Docker is running: `docker ps` should show `gemp_swccg_app_1` and `gemp_swccg_db_1`
3. Verify server responds: `curl -s http://localhost:17001/gemp-swccg/` should return HTML
4. Test a single game: `python3 mcp-gemp-client/watch_bot_game.py` with appropriate args
5. If rebuilding after code changes, use the fast path (mvn package + gemp restart)
6. Check AI logs after a game: `docker exec gemp_swccg_app_1 tail -500 /root/nohup.out | grep V29`
