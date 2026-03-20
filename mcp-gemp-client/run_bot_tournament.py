#!/usr/bin/env python3
"""
Bot-vs-Bot Tournament Runner for GEMP-SWCCG.

Runs N games between two AI bots sequentially, collecting results and
tracking replay file creation for later analysis.

Uses the same proven polling approach as watch_bot_game.py (new client per request).

Usage:
    python run_bot_tournament.py --games 15
    python run_bot_tournament.py --games 5 --light-deck "MY LS DECK" --dark-deck "MY DS DECK"
"""

import argparse
import asyncio
import json
import os
import re
import sys
import time
from datetime import datetime
from typing import Any, Dict, List, Optional, Set
from xml.etree import ElementTree as ET
from pathlib import Path

# Import the working BotGameWatcher directly
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from watch_bot_game import BotGameWatcher


def strip_html(text: str) -> str:
    text = re.sub(r"<div class='cardHint'[^>]*>(.*?)</div>", r"\1", text)
    text = re.sub(r"<[^>]+>", "", text)
    return text.strip()


def get_replay_files(replay_base: str, player_id: str) -> Set[str]:
    player_dir = os.path.join(replay_base, player_id)
    if not os.path.isdir(player_dir):
        return set()
    return set(os.listdir(player_dir))


def extract_key_events(messages: List[str]) -> List[str]:
    key_events = []
    keywords = [
        "deploys", "starting location", "epic event", "force drain",
        "lost due to", "is the winner", "retrieves", "battle",
        "forfeits", "concede", "place out of play", "lost pile",
        "attacks", "moves to", "Force generation",
    ]
    for msg in messages:
        clean = strip_html(msg)
        lower = clean.lower()
        if any(kw in lower for kw in keywords):
            key_events.append(clean)
    return key_events


async def run_single_game(watcher: BotGameWatcher, game_num: int,
                           format_code: str, light_skill: str, light_deck: str,
                           dark_skill: str, dark_deck: str, deck_owner: str,
                           max_polls: int = 5000) -> Dict[str, Any]:
    """Run a single game using the proven BotGameWatcher polling."""

    # Create the game
    game_id = await watcher.create_bot_game(
        format_code, light_skill, light_deck,
        dark_skill, dark_deck, deck_owner
    )
    if not game_id:
        return {"game_number": game_num, "error": "Failed to create game"}

    print(f"    Game ID: {game_id}")

    # Reset watcher state for new game
    watcher.game_id = game_id
    watcher.channel_number = 0
    watcher.game_over = False
    watcher.messages = []
    watcher.decision_count = 0

    # Sign up as spectator
    await watcher.spectate_signup()

    # Poll loop (same as watch_bot_game.py)
    poll_count = 0
    start_time = time.time()
    last_status_time = start_time
    game_timeout = 600

    while not watcher.game_over and poll_count < max_polls:
        poll_count += 1

        elapsed = time.time() - start_time
        if elapsed > game_timeout:
            print(f"    TIMEOUT after {game_timeout}s")
            break

        # Status heartbeat every 30s
        now = time.time()
        if now - last_status_time > 30:
            print(f"    ... {elapsed:.0f}s elapsed, {len(watcher.messages)} msgs, {watcher.decision_count} decisions")
            last_status_time = now

        update = await watcher.poll()

        if "messages" in update:
            for msg in update["messages"]:
                lower = msg.lower()
                if any(kw in lower for kw in ["deploys", "wins", "loses",
                                                "concedes", "force drain", "forfeits"]):
                    elapsed = time.time() - start_time
                    clean = strip_html(msg)
                    print(f"    [{elapsed:5.1f}s] {clean[:120]}")

        if "game_over" in update:
            elapsed = time.time() - start_time
            print(f"    GAME OVER ({elapsed:.1f}s, {watcher.decision_count} decisions)")
            break

        if update.get("timeout"):
            await asyncio.sleep(0.1)
        else:
            await asyncio.sleep(0.05)

    elapsed = time.time() - start_time

    # Parse winner/loser from messages
    winner = ""
    loser = ""
    win_reason = ""
    for msg in reversed(watcher.messages):
        lower = msg.lower()
        if "is the winner due to:" in msg:
            parts = msg.split(" is the winner due to:")
            winner = strip_html(parts[0]).replace("~", "").strip()
            win_reason = parts[1].strip() if len(parts) > 1 else ""
        elif "lost due to:" in lower:
            parts = msg.split(" lost due to:")
            loser = strip_html(parts[0]).replace("~", "").strip()
        if winner or loser:
            break

    return {
        "game_number": game_num,
        "game_id": game_id,
        "winner": winner,
        "loser": loser,
        "win_reason": win_reason,
        "elapsed_seconds": round(elapsed, 1),
        "total_decisions": watcher.decision_count,
        "total_messages": len(watcher.messages),
        "messages": list(watcher.messages),
    }


async def main():
    parser = argparse.ArgumentParser(description="Run a bot-vs-bot SWCCG tournament")
    parser.add_argument("--games", type=int, default=15, help="Number of games (default: 15)")
    parser.add_argument("--base-url", default="http://localhost:17001")
    parser.add_argument("--user", default="test1")
    parser.add_argument("--password", default="test")
    parser.add_argument("--format", default="open")
    parser.add_argument("--light-skill", default="CHOSENONE")
    parser.add_argument("--light-deck", default="LUKE SAGA TATOOINE")
    parser.add_argument("--dark-skill", default="RANDO")
    parser.add_argument("--dark-deck", default="DARK DEAL")
    parser.add_argument("--deck-owner", default="test1")
    parser.add_argument("--replay-base", default="")
    args = parser.parse_args()

    # Auto-detect replay base
    replay_base = args.replay_base
    if not replay_base:
        for c in [
            os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "replays"),
            "/Users/steve/gemp-swccg-public/replays",
            os.path.expanduser("~/gemp-swccg-public/replays"),
        ]:
            if os.path.isdir(c):
                replay_base = c
                break
        if not replay_base:
            replay_base = "/tmp/no-replays"

    light_player = "~The_Chosen_One" if args.light_skill == "CHOSENONE" else "~Rando_Cal"
    dark_player = "~Rando_Cal" if args.dark_skill == "RANDO" else "~The_Chosen_One"

    print("=" * 60)
    print("  GEMP-SWCCG Bot vs Bot Tournament")
    print("=" * 60)
    print(f"  Games:  {args.games}")
    print(f"  Light:  {args.light_skill} — '{args.light_deck}'")
    print(f"  Dark:   {args.dark_skill} — '{args.dark_deck}'")
    print(f"  Replays: {replay_base}")

    # Create watcher and login ONCE
    watcher = BotGameWatcher(args.base_url)

    print("\n[1/3] Logging in...")
    if not await watcher.login(args.user, args.password):
        print("FATAL: Could not login")
        sys.exit(1)

    print("[2/3] Checking server...")
    await watcher.ensure_server_running()
    await watcher.enable_ai_tables()
    print("  Server ready")

    print(f"\n[3/3] Starting {args.games}-game tournament...\n")

    # Snapshot replays before
    light_replays_before = get_replay_files(replay_base, light_player)
    dark_replays_before = get_replay_files(replay_base, dark_player)

    results = []
    light_wins = 0
    dark_wins = 0
    errors = 0

    for i in range(1, args.games + 1):
        print(f"\n{'='*60}")
        print(f"  GAME {i} of {args.games}")
        print(f"{'='*60}")

        result = await run_single_game(
            watcher, i, args.format,
            args.light_skill, args.light_deck,
            args.dark_skill, args.dark_deck,
            args.deck_owner
        )

        if result.get("error"):
            errors += 1
            results.append(result)
            await asyncio.sleep(2)
            continue

        # Determine side
        if "The_Chosen_One" in result.get("winner", "") or "Chosen" in result.get("winner", ""):
            result["winning_side"] = "Light"
            light_wins += 1
        elif "Rando_Cal" in result.get("winner", "") or "Rando" in result.get("winner", ""):
            result["winning_side"] = "Dark"
            dark_wins += 1
        else:
            result["winning_side"] = "Unknown"

        results.append(result)
        print(f"    Result: {result['winning_side']} wins — {result.get('win_reason', '?')}")
        print(f"    Score: Light {light_wins} - Dark {dark_wins} (of {i} games)")

        # Pause between games
        if i < args.games:
            if i % 5 == 0:
                print(f"    (Pausing 8s after {i} games...)")
                await asyncio.sleep(8)
            else:
                await asyncio.sleep(4)

    # Snapshot replays after
    light_replays_after = get_replay_files(replay_base, light_player)
    dark_replays_after = get_replay_files(replay_base, dark_player)

    # Build tournament JSON
    tournament = {
        "timestamp": datetime.now().isoformat(),
        "config": {
            "num_games": args.games, "format": args.format,
            "light_skill": args.light_skill, "light_deck": args.light_deck, "light_player": light_player,
            "dark_skill": args.dark_skill, "dark_deck": args.dark_deck, "dark_player": dark_player,
        },
        "summary": {
            "light_wins": light_wins, "dark_wins": dark_wins, "errors": errors,
            "light_win_pct": round(light_wins / max(light_wins + dark_wins, 1) * 100, 1),
            "dark_win_pct": round(dark_wins / max(light_wins + dark_wins, 1) * 100, 1),
        },
        "replay_files": {
            "light_player": light_player,
            "light_new_replays": sorted(light_replays_after - light_replays_before),
            "dark_player": dark_player,
            "dark_new_replays": sorted(dark_replays_after - dark_replays_before),
        },
        "games": [],
    }

    for r in results:
        game_summary = {k: v for k, v in r.items() if k != "messages"}
        game_summary["key_events"] = extract_key_events(r.get("messages", []))
        tournament["games"].append(game_summary)

        # Save per-game message log
        if r.get("messages") and r.get("game_id"):
            msg_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tournament_results", "game_messages")
            os.makedirs(msg_dir, exist_ok=True)
            gid_short = r["game_id"][:8]
            msg_file = os.path.join(msg_dir, f"game{r.get('game_number', 0):02d}_{gid_short}.txt")
            with open(msg_file, "w") as mf:
                for msg in r["messages"]:
                    mf.write(strip_html(msg) + "\n")

    # Save results
    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tournament_results")
    os.makedirs(results_dir, exist_ok=True)
    results_file = os.path.join(results_dir, f"tournament_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    with open(results_file, "w") as f:
        json.dump(tournament, f, indent=2)

    s = tournament["summary"]
    print(f"\n{'='*60}")
    print(f"  TOURNAMENT COMPLETE")
    print(f"{'='*60}")
    print(f"  Light ({args.light_skill}): {s['light_wins']} wins ({s['light_win_pct']}%)")
    print(f"  Dark  ({args.dark_skill}):  {s['dark_wins']} wins ({s['dark_win_pct']}%)")
    print(f"  Results saved to: {results_file}")
    print(f"  To analyze: python3 analyze_bot_tournament.py --latest --load-all-replays")


if __name__ == "__main__":
    asyncio.run(main())
