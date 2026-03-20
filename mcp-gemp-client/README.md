# GEMP-SWCCG MCP Game Client

MCP server that enables Claude to play SWCCG against the Rando AI bot via the GEMP game server API.

## Prerequisites

- GEMP-SWCCG running locally (`./bin/gemp initialize` or `docker compose up`)
- Python 3.10+
- A user account and at least one deck on the server

## Install

```bash
pip install -r requirements.txt
```

## Configure for Claude Code / Cowork

Add to your Claude MCP config (e.g., `~/.claude/mcp_servers.json` or project `.claude/mcp.json`):

```json
{
  "mcpServers": {
    "gemp-swccg": {
      "command": "python3",
      "args": ["/path/to/mcp-gemp-client/gemp_mcp.py"],
      "env": {
        "GEMP_BASE_URL": "http://localhost:17001",
        "GEMP_LOG_DIR": "/path/to/mcp-gemp-client/game_logs"
      }
    }
  }
}
```

## Tools (12)

| Tool | Purpose |
|------|---------|
| `gemp_login` | Login to GEMP server |
| `gemp_list_decks` | List available decks |
| `gemp_get_hall` | View game lobby |
| `gemp_create_game` | Create game vs AI (Rando/Advanced/Beginner) |
| `gemp_find_game` | Find your active game |
| `gemp_join_game` | Join game and get initial state |
| `gemp_poll` | Poll for game updates and decisions |
| `gemp_get_current_decision` | View current pending decision details |
| `gemp_submit_decision` | Submit response to a decision |
| `gemp_concede` | Concede current game |
| `gemp_game_messages` | View game message history |
| `gemp_log_observation` | Record observation for cross-session learning |

## Game Flow

```
gemp_login → gemp_create_game → gemp_find_game → gemp_join_game
→ loop: gemp_poll → gemp_get_current_decision → gemp_submit_decision
→ gemp_log_observation (as needed)
```

## Game Logs

Structured JSONL logs are written to `game_logs/` for each game, tracking every decision and observation for cross-session analysis.
