#!/usr/bin/env python3
"""
Auto-play script: Plays a full SWCCG game against Rando via the HTTP API.

Usage:
  python3 ~/gemp-swccg-public/mcp-gemp-client/play_game.py

Handles all decision types, logs the full game, and reports results.
"""

import asyncio
import json
import os
import re
import sys
import time
from datetime import datetime
from typing import Any, Dict, List, Optional, Tuple
from xml.etree import ElementTree as ET

sys.path.insert(0, os.path.dirname(__file__))
from gemp_mcp import GempSession


# ---------------------------------------------------------------------------
# Game state tracker
# ---------------------------------------------------------------------------

class GameState:
    """Tracks the full game state across polls."""

    def __init__(self):
        self.turn: int = 0
        self.phase: str = ""
        self.my_force: int = 0
        self.opp_force: int = 0
        self.messages: List[str] = []
        self.decision_count: int = 0
        self.game_over: bool = False
        self.winner: str = ""
        self.cards_in_play: Dict[str, Dict] = {}  # cardId -> info
        self.my_hand_count: int = 0
        self.opp_hand_count: int = 0


# ---------------------------------------------------------------------------
# Decision engine — makes choices for each decision type
# ---------------------------------------------------------------------------

class DecisionEngine:
    """Simple decision-making engine for playing SWCCG."""

    def __init__(self, verbose: bool = True):
        self.verbose = verbose
        self.turn_actions: List[str] = []

    def decide(self, decision: Dict[str, Any], game: GameState) -> Tuple[str, str]:
        """
        Given a decision dict (from XML parsing), return (decision_value, reasoning).
        """
        dec_type = decision.get("type", "")
        dec_text = decision.get("text", "")
        dec_id = decision.get("id", "")

        if dec_type == "MULTIPLE_CHOICE":
            return self._handle_multiple_choice(decision, game)
        elif dec_type == "INTEGER":
            return self._handle_integer(decision, game)
        elif dec_type in ("CARD_ACTION_CHOICE", "ACTION_CHOICE"):
            return self._handle_action_choice(decision, game)
        elif dec_type in ("CARD_SELECTION", "ARBITRARY_CARDS"):
            return self._handle_card_selection(decision, game)
        else:
            # Unknown type — try empty string (pass/done)
            return "", f"Unknown decision type '{dec_type}', defaulting to empty"

    def _handle_multiple_choice(self, dec: Dict, game: GameState) -> Tuple[str, str]:
        """Handle MULTIPLE_CHOICE decisions."""
        options = dec.get("options", [])
        text = dec.get("text", "").lower()

        if not options:
            return "0", "No options, default to 0"

        # Check for Pass option
        pass_idx = None
        for opt in options:
            opt_text = opt.get("text", "").lower()
            if opt_text in ("pass", "done", "no"):
                pass_idx = opt.get("index", "0")

        # Specific decision heuristics
        if "choose activate action" in text or "activate action or pass" in text:
            # During activate phase, pass unless something useful
            if pass_idx is not None:
                return pass_idx, "Passing activate phase"
            return str(options[-1].get("index", "0")), "Pass activate"

        if "choose control action" in text or "control action or pass" in text:
            if pass_idx is not None:
                return pass_idx, "Passing control phase"
            return str(options[-1].get("index", "0")), "Pass control"

        if "choose move action" in text or "move action or pass" in text:
            if pass_idx is not None:
                return pass_idx, "Passing move phase"
            return str(options[-1].get("index", "0")), "Pass move"

        if "choose battle action" in text or "battle action or pass" in text:
            if pass_idx is not None:
                return pass_idx, "Passing battle phase"
            return str(options[-1].get("index", "0")), "Pass battle"

        if "choose draw" in text or "draw action or pass" in text:
            if pass_idx is not None:
                return pass_idx, "Passing draw phase"
            return str(options[-1].get("index", "0")), "Pass draw"

        # For Yes/No questions, default to Yes (index 0) for game actions
        for opt in options:
            if opt.get("text", "").lower() == "yes":
                return opt["index"], "Defaulting to Yes"

        # Default: pick the first non-pass option, or the default option
        for opt in options:
            if opt.get("is_default"):
                return opt["index"], f"Picking default: {opt.get('text', '?')}"

        # Just pick first option
        return options[0].get("index", "0"), f"Picking first option: {options[0].get('text', '?')}"

    def _handle_integer(self, dec: Dict, game: GameState) -> Tuple[str, str]:
        """Handle INTEGER decisions (e.g., Force activation amount)."""
        text = dec.get("text", "").lower()
        min_val = int(dec.get("min", "0"))
        max_val = int(dec.get("max", "0"))
        default_val = int(dec.get("default", str(max_val)))

        if "force to allow opponent to activate" in text:
            # Let opponent activate all their Force (don't interrupt)
            return str(max_val), f"Allowing opponent full activation ({max_val})"

        if "activate" in text and "force" in text:
            # Activate maximum Force
            return str(max_val), f"Activating max Force ({max_val})"

        if "use" in text and "force" in text:
            # Use Force — be conservative, use minimum unless it's deploying
            return str(min_val), f"Using minimum Force ({min_val})"

        # Default to max for most integer decisions
        return str(default_val), f"Integer default: {default_val} (range {min_val}-{max_val})"

    def _handle_action_choice(self, dec: Dict, game: GameState) -> Tuple[str, str]:
        """Handle CARD_ACTION_CHOICE and ACTION_CHOICE decisions."""
        options = dec.get("options", [])
        text = dec.get("text", "").lower()

        if not options:
            return "", "No actions available"

        # Find the Pass option if present
        pass_opt = None
        for opt in options:
            opt_text = (opt.get("text", "") or opt.get("cardTitle", "") or "").lower()
            if opt_text in ("pass", "done", "pass - end phase"):
                pass_opt = opt
                break
            if "pass" in opt_text:
                pass_opt = opt

        # During routine phase prompts ("Choose X action or Pass"), prefer Pass
        # to avoid infinite loops where we keep picking actions with no effect.
        phase_prompts = [
            "activate action or pass", "control action or pass",
            "move action or pass", "battle action or pass",
            "draw action or pass",
        ]
        is_phase_prompt = any(phrase in text for phrase in phase_prompts)

        if is_phase_prompt:
            # For now, always pass during phase prompts.
            # A smarter bot would evaluate whether any action is worth taking.
            if pass_opt:
                return pass_opt["actionId"], "Passing phase (action choice)"
            # If no explicit pass found, the last option is typically Pass
            last_opt = options[-1]
            return last_opt["actionId"], "Passing phase (last option)"

        # Look for deploy actions (prioritize deploying)
        for opt in options:
            action_text = (opt.get("text", "") or "").lower()
            if "deploy" in action_text:
                card_title = opt.get("cardTitle", opt.get("text", ""))
                return opt["actionId"], f"Deploying: {card_title}"

        # Look for play/use actions
        for opt in options:
            action_text = (opt.get("text", "") or "").lower()
            if "play" in action_text or "use" in action_text:
                card_title = opt.get("cardTitle", opt.get("text", ""))
                return opt["actionId"], f"Playing: {card_title}"

        # Look for move actions
        for opt in options:
            action_text = (opt.get("text", "") or "").lower()
            if "move" in action_text:
                card_title = opt.get("cardTitle", opt.get("text", ""))
                return opt["actionId"], f"Moving: {card_title}"

        # Default: pick first action (but prefer pass if nothing useful found)
        if pass_opt:
            return pass_opt["actionId"], "No useful action found, passing"
        card_title = options[0].get("cardTitle", options[0].get("text", "?"))
        return options[0]["actionId"], f"First action: {card_title}"

    def _handle_card_selection(self, dec: Dict, game: GameState) -> Tuple[str, str]:
        """Handle CARD_SELECTION and ARBITRARY_CARDS decisions."""
        cards = dec.get("cards", [])
        min_sel = int(dec.get("min", "0"))
        max_sel = int(dec.get("max", str(len(cards))))
        text = dec.get("text", "").lower()

        # Filter to selectable cards only
        selectable = [c for c in cards if c.get("selectable", True)]

        if not selectable:
            return "", "No selectable cards"

        if min_sel == 0:
            # Optional selection — skip unless it seems beneficial
            if "choose card" in text and ("deploy" in text or "play" in text):
                # Select up to max
                selected = selectable[:max_sel]
                ids = ",".join(c["cardId"] for c in selected)
                titles = ", ".join(c.get("title", c["cardId"]) for c in selected)
                return ids, f"Selected {len(selected)} cards: {titles}"
            # Otherwise skip
            return "", "Optional selection — skipping"

        # Must select cards — pick the required number
        count = min(min_sel, len(selectable))
        selected = selectable[:count]
        ids = ",".join(c["cardId"] for c in selected)
        titles = ", ".join(c.get("title", c["cardId"]) for c in selected)
        return ids, f"Selected {count} cards: {titles}"


# ---------------------------------------------------------------------------
# XML Parser (simplified from gemp_mcp.py)
# ---------------------------------------------------------------------------

def parse_game_update(xml_text: str, channel: int) -> Dict[str, Any]:
    """Parse game update XML into structured data."""
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return {"status": "error", "message": f"XML parse error: {xml_text[:300]}"}

    # Update channel
    cn = root.attrib.get("cn")
    new_channel = int(cn) if cn else channel

    result: Dict[str, Any] = {"status": "ok", "channel": new_channel}

    messages = []
    decision = None
    phase_change = None
    game_over = False
    winner = None

    for ge in root.findall(".//ge"):
        event_type = ge.attrib.get("type", "")

        if event_type == "M":
            msg = ge.attrib.get("message", "")
            if msg:
                messages.append(msg)

        elif event_type == "W":
            msg = ge.attrib.get("message", "")
            if msg:
                messages.append(f"[WARNING] {msg}")

        elif event_type == "GPC":
            phase = ge.attrib.get("phase", "")
            if phase:
                phase_change = phase

        elif event_type == "D":
            decision = _parse_decision(ge)

    # Check for game over
    for msg in messages:
        ml = msg.lower()
        if "wins" in ml or "loses" in ml or "concedes" in ml or "game over" in ml:
            game_over = True
            winner = msg

    if messages:
        result["messages"] = messages
    if phase_change:
        result["phase"] = phase_change
    if decision:
        result["decision"] = decision
    if game_over:
        result["game_over"] = True
        result["winner"] = winner

    return result


def _parse_decision(ge: ET.Element) -> Dict[str, Any]:
    """Parse a decision event."""
    decision_id = ge.attrib.get("id", "")
    decision_type = ge.attrib.get("decisionType", "")
    decision_text = ge.attrib.get("text", "")

    decision: Dict[str, Any] = {
        "id": decision_id,
        "type": decision_type,
        "text": decision_text,
    }

    # Collect parameters
    params: Dict[str, List[str]] = {}
    for param in ge.findall("parameter"):
        name = param.attrib.get("name", "")
        value = param.attrib.get("value", "")
        if name not in params:
            params[name] = []
        params[name].append(value)

    if decision_type == "MULTIPLE_CHOICE":
        options = []
        results = params.get("results", [])
        default_idx = params.get("defaultIndex", ["0"])[0]
        for i, opt in enumerate(results):
            options.append({"index": str(i), "text": opt, "is_default": str(i) == default_idx})
        decision["options"] = options

    elif decision_type == "INTEGER":
        decision["min"] = params.get("min", ["0"])[0]
        decision["max"] = params.get("max", ["0"])[0]
        decision["default"] = params.get("defaultValue", ["0"])[0]

    elif decision_type in ("CARD_ACTION_CHOICE", "ACTION_CHOICE"):
        options = []
        action_ids = params.get("actionId", [])
        action_texts = params.get("actionText", [])
        blueprint_ids = params.get("blueprintId", [])
        testing_texts = params.get("testingText", [])
        for i in range(len(action_ids)):
            opt: Dict[str, Any] = {"index": str(i), "actionId": action_ids[i]}
            if i < len(action_texts):
                opt["text"] = action_texts[i]
            if i < len(blueprint_ids):
                opt["blueprintId"] = blueprint_ids[i]
            if i < len(testing_texts):
                opt["cardTitle"] = testing_texts[i]
            options.append(opt)
        decision["options"] = options

    elif decision_type in ("CARD_SELECTION", "ARBITRARY_CARDS"):
        cards = []
        card_ids = params.get("cardId", [])
        blueprint_ids = params.get("blueprintId", [])
        testing_texts = params.get("testingText", [])
        selectables = params.get("selectable", [])
        for i in range(len(card_ids)):
            card: Dict[str, Any] = {"cardId": card_ids[i]}
            if i < len(blueprint_ids):
                card["blueprintId"] = blueprint_ids[i]
            if i < len(testing_texts):
                card["title"] = testing_texts[i]
            if i < len(selectables):
                card["selectable"] = selectables[i] == "true"
            cards.append(card)
        decision["cards"] = cards
        decision["min"] = params.get("min", ["0"])[0]
        decision["max"] = params.get("max", [str(len(card_ids))])[0]

    else:
        decision["raw_params"] = {k: v for k, v in params.items()}

    return decision


# ---------------------------------------------------------------------------
# Main game loop
# ---------------------------------------------------------------------------

async def play_game(
    base_url: str = "http://localhost:17001",
    username: str = "test1",
    password: str = "test",
    player_deck: str = "LUKE SAGA TATOOINE",
    ai_deck: str = "DARK DEAL",
    max_decisions: int = 500,
    verbose: bool = True,
):
    """Play a complete game against Rando and return the result."""

    session = GempSession(base_url)
    engine = DecisionEngine(verbose=verbose)
    game = GameState()
    log_lines: List[str] = []

    def log(msg: str):
        timestamp = datetime.now().strftime("%H:%M:%S")
        line = f"[{timestamp}] {msg}"
        log_lines.append(line)
        if verbose:
            print(line)

    # --- Setup ---
    log("=" * 60)
    log("SWCCG Auto-Play: test1 (LIGHT) vs Rando (DARK DEAL)")
    log("=" * 60)

    # Login
    result = await session.login(username, password)
    if result["status"] != "ok":
        log(f"LOGIN FAILED: {result}")
        return {"status": "error", "message": "Login failed", "log": log_lines}
    log(f"Logged in as {username}")

    # Admin setup
    result = await session.admin_setup()
    log(f"Admin setup: {result.get('results', {})}")

    # Create game
    result = await session.create_game_vs_ai(
        game_format="open",
        deck_name=player_deck,
        ai_skill="RANDO",
        ai_deck_name=ai_deck,
        ai_deck_sample=False,
        sample_deck=False,
    )
    log(f"Create game: {result.get('message', result)}")

    await asyncio.sleep(2)

    # Find game
    result = await session.find_my_game()
    if not result.get("gameId"):
        log(f"GAME NOT FOUND: {result}")
        return {"status": "error", "message": "Game not found", "log": log_lines}

    game_id = result["gameId"]
    players = result.get("table", {}).get("players", "")
    log(f"Game found: {game_id}")
    log(f"Players: {players}")

    # Join game
    log("Joining game...")
    join_result = await session.signup_for_game(game_id)
    channel = join_result.get("channel", 0)
    log(f"Joined. Channel: {channel}")

    # Process initial events
    if join_result.get("messages"):
        for msg in join_result["messages"][-5:]:
            log(f"  > {msg}")

    # Handle initial decision if present
    pending_decision = join_result.get("decision")

    # --- Main game loop ---
    log("\n" + "=" * 60)
    log("GAME START")
    log("=" * 60)

    decision_num = 0
    consecutive_errors = 0
    last_phase = ""

    while decision_num < max_decisions and not game.game_over:
        # If we have a pending decision from the last poll, handle it
        if pending_decision:
            decision_num += 1
            dec_type = pending_decision.get("type", "?")
            dec_text = pending_decision.get("text", "")[:80]
            dec_id = pending_decision.get("id", "")

            # Make decision
            value, reasoning = engine.decide(pending_decision, game)

            # Log important decisions (skip routine passes)
            is_routine = any(phrase in dec_text.lower() for phrase in [
                "activate action or pass", "control action or pass",
                "move action or pass", "battle action or pass",
                "draw action or pass", "force to allow opponent",
            ])

            if not is_routine or verbose:
                log(f"  [{decision_num}] {dec_type}: {dec_text}")
                log(f"         -> {value} ({reasoning})")

            # Submit decision via poll
            try:
                import httpx
                async with httpx.AsyncClient(timeout=10.0) as client:
                    data = {
                        "participantId": session.username,
                        "channelNumber": str(channel),
                        "decisionId": dec_id,
                        "decisionValue": value,
                    }
                    resp = await client.post(
                        f"{base_url}/gemp-swccg-server/game/{game_id}",
                        data=data,
                        headers=session._headers("game.html"),
                    )

                    if resp.status_code == 410:
                        # Re-signup
                        log("  (channel expired, re-joining)")
                        join_result = await session.signup_for_game(game_id)
                        channel = join_result.get("channel", 0)
                        pending_decision = join_result.get("decision")
                        consecutive_errors = 0
                        continue

                    if not resp.is_success:
                        consecutive_errors += 1
                        log(f"  ERROR: HTTP {resp.status_code}: {resp.text[:200]}")
                        if consecutive_errors > 5:
                            log("Too many consecutive errors, stopping.")
                            break
                        pending_decision = None
                        await asyncio.sleep(1)
                        continue

                    consecutive_errors = 0
                    update = parse_game_update(resp.text, channel)
                    channel = update.get("channel", channel)

            except Exception as e:
                log(f"  EXCEPTION: {e}")
                consecutive_errors += 1
                if consecutive_errors > 5:
                    break
                pending_decision = None
                await asyncio.sleep(1)
                continue

        else:
            # No pending decision — poll for updates
            try:
                import httpx
                async with httpx.AsyncClient(timeout=10.0) as client:
                    data = {
                        "participantId": session.username,
                        "channelNumber": str(channel),
                    }
                    resp = await client.post(
                        f"{base_url}/gemp-swccg-server/game/{game_id}",
                        data=data,
                        headers=session._headers("game.html"),
                    )

                    if resp.status_code == 410:
                        join_result = await session.signup_for_game(game_id)
                        channel = join_result.get("channel", 0)
                        pending_decision = join_result.get("decision")
                        continue

                    if not resp.is_success:
                        consecutive_errors += 1
                        if consecutive_errors > 10:
                            log("Too many poll errors, stopping.")
                            break
                        await asyncio.sleep(1)
                        continue

                    consecutive_errors = 0
                    update = parse_game_update(resp.text, channel)
                    channel = update.get("channel", channel)

            except httpx.TimeoutException:
                # Normal long-poll timeout
                update = {"status": "ok"}
                continue
            except Exception as e:
                log(f"  POLL ERROR: {e}")
                consecutive_errors += 1
                if consecutive_errors > 10:
                    break
                await asyncio.sleep(1)
                continue

        # Process update
        if update.get("phase") and update["phase"] != last_phase:
            last_phase = update["phase"]
            log(f"\n--- Phase: {last_phase} ---")

        if update.get("messages"):
            for msg in update["messages"]:
                # Log important messages (skip routine activation)
                if not msg.startswith("~Rando_Cal activated 1 Force"):
                    log(f"  > {msg}")
                # Check for game over
                ml = msg.lower()
                if "wins" in ml or "loses" in ml or "concedes" in ml:
                    game.game_over = True
                    game.winner = msg

        if update.get("game_over"):
            game.game_over = True
            game.winner = update.get("winner", "unknown")
            log(f"\n{'=' * 60}")
            log(f"GAME OVER: {game.winner}")
            log(f"Total decisions: {decision_num}")
            log(f"{'=' * 60}")
            break

        pending_decision = update.get("decision")

    # --- Save log ---
    log_dir = os.path.join(os.path.dirname(__file__), "game_logs")
    os.makedirs(log_dir, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_path = os.path.join(log_dir, f"autoplay_{timestamp}.txt")
    with open(log_path, "w") as f:
        f.write("\n".join(log_lines))
    print(f"\nGame log saved to: {log_path}")

    return {
        "status": "complete" if game.game_over else "stopped",
        "winner": game.winner,
        "decisions": decision_num,
        "log_path": log_path,
    }


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Play SWCCG against Rando")
    parser.add_argument("--player-deck", default="LUKE SAGA TATOOINE", help="Your Light Side deck")
    parser.add_argument("--ai-deck", default="DARK DEAL", help="Rando's Dark Side deck")
    parser.add_argument("--quiet", action="store_true", help="Less verbose output")
    parser.add_argument("--max-decisions", type=int, default=500, help="Max decisions before stopping")
    args = parser.parse_args()

    result = asyncio.run(play_game(
        player_deck=args.player_deck,
        ai_deck=args.ai_deck,
        max_decisions=args.max_decisions,
        verbose=not args.quiet,
    ))

    print(f"\nResult: {json.dumps(result, indent=2)}")
