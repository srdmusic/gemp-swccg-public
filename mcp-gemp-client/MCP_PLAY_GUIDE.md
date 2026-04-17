# GEMP-SWCCG MCP Play Guide

## What This MCP Does
Connects Claude Code to a running GEMP-SWCCG game server so Claude can play SWCCG as a human player. Claude logs in as a user account (e.g. `asdf`), creates or joins a game against an AI bot (Rando Cal or The Chosen One), and makes all gameplay decisions via HTTP API.

## Setup (Every Session)
1. GEMP must be running on `localhost:17001` (Docker: `cd src && docker compose up -d`)
2. **Boot GEMP server** — MUST do these two steps every session or GEMP won't accept games:
   - `gemp_login` with username `asdf`, password `asdf`
   - `gemp_admin_setup` — this disables shutdown mode, enables bot tables, clears cache
3. Steve creates the game from the browser, then closes the browser
4. Find game: `gemp_find_game` → `gemp_join_game` with the game ID

**If the app container crashed** (DB not ready on boot), run `docker restart gemp_swccg_app_1` then wait ~30 seconds before login.

## MCP Tools
| Tool | Purpose |
|------|---------|
| `gemp_login` | Authenticate with GEMP server |
| `gemp_admin_setup` | Run all admin initialization in one shot |
| `gemp_list_decks` | List available decks for the logged-in user |
| `gemp_create_game` | Create a game vs AI (specify deck, AI skill level) |
| `gemp_create_bot_vs_bot` | Create AI vs AI game to spectate |
| `gemp_find_game` | Find active game for logged-in user |
| `gemp_join_game` | Join a game by ID, get initial state |
| `gemp_poll` | Poll for game updates and pending decisions |
| `gemp_submit_decision` | Submit a decision response |
| `gemp_advance` | **KEY TOOL** — auto-passes non-critical decisions, returns only when a real strategic decision is needed |
| `gemp_get_current_decision` | View current pending decision details |
| `gemp_concede` | Concede the current game |
| `gemp_game_messages` | Get recent game messages/narration |
| `gemp_log_observation` | Log observations for analysis |

## How to Play Efficiently
- Use `gemp_advance` as the primary loop — it auto-passes:
  - Empty "Optional responses" (no choices available)
  - Opponent force activation allowance (allows max)
  - Verification dialogs
- It returns when there's a **real decision**: deploy choices, battle decisions, card selections, etc.
- After making a decision with `gemp_submit_decision`, call `gemp_advance` again to skip to the next real decision.

## Decision Response Formats
- **MULTIPLE_CHOICE**: index string (`"0"`, `"1"`)
- **INTEGER**: number string (`"7"`)
- **CARD_ACTION_CHOICE**: action index string (`"0"`, `"1"`, `"2"`)
- **CARD_SELECTION**: comma-separated card IDs (`"230,240"`)
- **ARBITRARY_CARDS**: comma-separated temp IDs (`"temp0,temp2"`)
- **Pass/Done**: empty string `""`

## Card Lookup
Cards are identified by blueprint IDs (e.g. `226_1` = Cloud City: Dining Room (V)). To identify a card:
- Blueprint `X_Y` maps to class `CardX_YYY` (pad Y to 3 digits) in `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/setX/dark/` or `.../light/`
- The card title is in the class constructor

## Important: Browser Conflict
Only ONE client can be connected at a time. If Steve has the game open in a browser AND Claude is connected via MCP, you get "Subscription conflict" errors. Steve must close the browser before Claude takes control.

## Steve's Available Decks
**Dark Side:** DARK DEAL, EOPS, Hunt Down V, IBS, IBS tornement, ISB_v2, Imperial Entanglements, Shadow Collective, WALKER NEW
**Light Side:** HIDDEN PATH CHARGE, LUKE SAGA, LUKE SAGA TATOOINE, Luke Jedi Knight, REY SAGA

## TDIGWATT Strategy (Dark Deal deck)
- Objective: This Deal Is Getting Worse All The Time (V)
- Deploy Cloud City sites, get Lando + backup characters at CC
- Use AMSD (Alert My Star Destroyer) to deploy Executor at Bespin system
- Deploy Piett (pulls Gherant) aboard Executor
- Occupy 3 CC locations to flip objective
- Post-flip: consolidate to 2 locations, maintain ability >= 4 everywhere
- Key rules: Never deploy Lando alone, never deploy Piett/Gherant to ground, keep ability >= 4 at sites
