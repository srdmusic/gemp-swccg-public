#!/usr/bin/env python3
"""
Bot-vs-Bot Game Watcher for GEMP-SWCCG.

Creates a game between two AI bots (e.g., The Chosen One vs Rando Cal),
then spectates the game by polling for updates and logging all events.

Usage:
    python watch_bot_game.py [--format open] [--light-skill CHOSENONE] [--light-deck "LS deck"]
                              [--dark-skill RANDO] [--dark-deck "DS deck"]
                              [--base-url http://localhost:17001]
                              [--user admin] [--password admin]

The watcher logs all game events (phase changes, decisions, card plays, messages)
to both console and a JSONL log file for later analysis.
"""

import argparse
import asyncio
import json
import os
import sys
import time
from datetime import datetime
from typing import Any, Dict, List, Optional
from xml.etree import ElementTree as ET

import httpx


# ---------------------------------------------------------------------------
# Simple HTTP Client for Spectating
# ---------------------------------------------------------------------------

class BotGameWatcher:
    """Creates and watches a bot-vs-bot game on GEMP."""

    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.cookies: Dict[str, str] = {}
        self.username: Optional[str] = None
        self.game_id: Optional[str] = None
        self.channel_number: int = 0
        self.game_over = False
        self.turn_count = 0
        self.phase = "Unknown"
        self.messages: List[str] = []
        self.decision_count = 0
        self.log_path: Optional[str] = None

    def _headers(self, referer_page: str = "hall.html") -> Dict[str, str]:
        headers = {"Referer": f"{self.base_url}/gemp-swccg/{referer_page}"}
        if self.cookies:
            headers["Cookie"] = "; ".join(f"{k}={v}" for k, v in self.cookies.items())
        return headers

    def _store_cookies(self, response: httpx.Response) -> None:
        for cookie_header in response.headers.get_list("set-cookie"):
            parts = cookie_header.split(";")[0]
            if "=" in parts:
                k, v = parts.split("=", 1)
                self.cookies[k.strip()] = v.strip()

    async def login(self, username: str, password: str) -> bool:
        """Login as admin to access the bot game endpoint."""
        async with httpx.AsyncClient(timeout=15.0) as client:
            await client.head(self.base_url)
            resp = await client.post(
                f"{self.base_url}/gemp-swccg-server/login",
                data={"login": username, "password": password},
                headers={"Referer": f"{self.base_url}/gemp-swccg/"},
                follow_redirects=True,
            )
            self._store_cookies(resp)
            if resp.status_code == 200 and "set-cookie" in resp.headers:
                self._store_cookies(resp)
                self.username = username
                print(f"  Logged in as: {username}")
                return True
            print(f"  Login failed: HTTP {resp.status_code}")
            return False

    async def ensure_server_running(self) -> None:
        """Disable shutdown mode if needed."""
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                f"{self.base_url}/gemp-swccg-server/admin/shutdown",
                data={"enabled": "false"},
                headers=self._headers(),
            )
            if resp.is_success:
                print("  Server is running")
            else:
                print(f"  Warning: Could not verify server status (HTTP {resp.status_code})")

    async def enable_ai_tables(self) -> None:
        """Enable AI/bot tables."""
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                f"{self.base_url}/gemp-swccg-server/admin/settings/aitables",
                data={"enabled": "true"},
                headers=self._headers(),
            )
            if resp.is_success:
                print("  AI tables enabled")

    async def create_bot_game(self, format_code: str, light_skill: str, light_deck: str,
                               dark_skill: str, dark_deck: str,
                               deck_owner: str = "") -> Optional[str]:
        """Create a bot-vs-bot game via the admin endpoint."""
        async with httpx.AsyncClient(timeout=30.0) as client:
            data = {
                "format": format_code,
                "lightSkill": light_skill,
                "lightDeck": light_deck,
                "darkSkill": dark_skill,
                "darkDeck": dark_deck,
            }
            if deck_owner:
                data["deckOwner"] = deck_owner
            resp = await client.post(
                f"{self.base_url}/gemp-swccg-server/admin/botgame",
                data=data,
                headers=self._headers(),
            )
            if not resp.is_success:
                print(f"  Failed to create bot game: HTTP {resp.status_code}")
                print(f"  Response: {resp.text[:500]}")
                return None

            body = resp.text.strip()
            if body.startswith("OK gameId="):
                game_id = body.replace("OK gameId=", "").strip()
                self.game_id = game_id
                print(f"  Game created! ID: {game_id}")
                return game_id
            else:
                print(f"  Unexpected response: {body}")
                return None

    async def spectate_signup(self) -> bool:
        """Sign up as a spectator for the game."""
        if not self.game_id:
            return False
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.get(
                f"{self.base_url}/gemp-swccg-server/game/{self.game_id}",
                params={"participantId": self.username},
                headers=self._headers("game.html"),
            )
            if resp.is_success:
                self._parse_update(resp.text)
                print(f"  Signed up as spectator (channel: {self.channel_number})")
                return True
            else:
                print(f"  Spectate signup failed: HTTP {resp.status_code}")
                # Try with the admin user instead
                return False

    async def poll(self) -> Dict[str, Any]:
        """Poll for game updates as a spectator."""
        if not self.game_id:
            return {"error": "No game"}

        async with httpx.AsyncClient(timeout=12.0) as client:
            try:
                resp = await client.post(
                    f"{self.base_url}/gemp-swccg-server/game/{self.game_id}",
                    data={
                        "participantId": self.username,
                        "channelNumber": str(self.channel_number),
                    },
                    headers=self._headers("game.html"),
                )
            except httpx.TimeoutException:
                return {"timeout": True}

            if resp.status_code == 410:
                # Re-signup
                await self.spectate_signup()
                return {"resubscribed": True}
            if not resp.is_success:
                return {"error": f"HTTP {resp.status_code}"}

            return self._parse_update(resp.text)

    def _parse_update(self, xml_text: str) -> Dict[str, Any]:
        """Parse game update XML."""
        try:
            root = ET.fromstring(xml_text)
        except ET.ParseError:
            return {"error": "XML parse error"}

        cn = root.attrib.get("cn")
        if cn:
            self.channel_number = int(cn)

        result: Dict[str, Any] = {}
        new_messages = []
        new_warnings = []
        decisions = []

        for ge in root.findall(".//ge"):
            etype = ge.attrib.get("type", "")

            if etype == "M":
                msg = ge.attrib.get("message", "")
                if msg:
                    new_messages.append(msg)
                    self.messages.append(msg)

            elif etype == "W":
                msg = ge.attrib.get("message", "")
                if msg:
                    new_warnings.append(msg)

            elif etype == "GPC":
                phase = ge.attrib.get("phase", "")
                if phase:
                    self.phase = phase
                    result["phase_change"] = phase

            elif etype == "D":
                # A decision event (AI is being asked to decide)
                self.decision_count += 1
                dec_type = ge.attrib.get("decisionType", "")
                dec_text = ge.attrib.get("text", "")
                decisions.append({"type": dec_type, "text": dec_text[:100]})

            elif etype == "GS":
                # Game stats
                stats = {}
                for pz in ge.findall("playerZones"):
                    player = pz.attrib.get("name", "unknown")
                    stats[player] = {k: v for k, v in pz.attrib.items() if k != "name"}
                result["stats"] = stats

        # Check for game over
        for msg in new_messages:
            lower = msg.lower()
            if "is the winner" in lower or "lost due to" in lower or "conceded" in lower:
                self.game_over = True
                result["game_over"] = True
                result["result"] = msg

        if new_messages:
            result["messages"] = new_messages
        if new_warnings:
            result["warnings"] = new_warnings
        if decisions:
            result["decisions"] = decisions

        return result

    def init_log(self, light_skill: str, light_deck: str, dark_skill: str, dark_deck: str) -> None:
        """Initialize a JSONL log file for this game."""
        log_dir = os.path.join(os.path.dirname(__file__), "game_logs")
        os.makedirs(log_dir, exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        game_short = (self.game_id or "unknown")[:8]
        self.log_path = os.path.join(log_dir, f"botgame_{timestamp}_{game_short}.jsonl")
        self._log_event({
            "event": "game_start",
            "game_id": self.game_id,
            "light_skill": light_skill,
            "light_deck": light_deck,
            "dark_skill": dark_skill,
            "dark_deck": dark_deck,
        })

    def _log_event(self, data: Dict[str, Any]) -> None:
        if not self.log_path:
            return
        data["timestamp"] = datetime.now().isoformat()
        with open(self.log_path, "a") as f:
            f.write(json.dumps(data) + "\n")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

async def main():
    parser = argparse.ArgumentParser(description="Watch a bot-vs-bot SWCCG game")
    parser.add_argument("--base-url", default="http://localhost:17001", help="GEMP server URL")
    parser.add_argument("--user", default="test1", help="Admin username (default: test1)")
    parser.add_argument("--password", default="test", help="Admin password (default: test)")
    parser.add_argument("--format", default="open", help="Game format (default: open)")
    parser.add_argument("--light-skill", default="CHOSENONE", help="Light side AI skill")
    parser.add_argument("--light-deck", default="LUKE SAGA TATOOINE", help="Light side deck name")
    parser.add_argument("--dark-skill", default="RANDO", help="Dark side AI skill")
    parser.add_argument("--dark-deck", default="DARK DEAL", help="Dark side deck name")
    parser.add_argument("--deck-owner", default="test1", help="Player account that owns the decks")
    parser.add_argument("--max-polls", type=int, default=5000, help="Max poll iterations")
    args = parser.parse_args()

    watcher = BotGameWatcher(args.base_url)

    print("=" * 60)
    print("  GEMP-SWCCG Bot vs Bot Game Watcher")
    print("=" * 60)

    # Step 1: Login
    print("\n[1/4] Logging in...")
    if not await watcher.login(args.user, args.password):
        print("FATAL: Could not login")
        sys.exit(1)

    # Step 2: Ensure server is ready
    print("\n[2/4] Checking server...")
    await watcher.ensure_server_running()
    await watcher.enable_ai_tables()

    # Step 3: Create the bot game
    print(f"\n[3/4] Creating bot game:")
    print(f"  Light: {args.light_skill} with deck '{args.light_deck}'")
    print(f"  Dark:  {args.dark_skill} with deck '{args.dark_deck}'")
    print(f"  Deck owner: {args.deck_owner}")
    print(f"  Format: {args.format}")

    game_id = await watcher.create_bot_game(
        args.format, args.light_skill, args.light_deck,
        args.dark_skill, args.dark_deck, args.deck_owner
    )
    if not game_id:
        print("FATAL: Could not create bot game")
        sys.exit(1)

    # Initialize logging
    watcher.init_log(args.light_skill, args.light_deck, args.dark_skill, args.dark_deck)

    # Step 4: Spectate
    print(f"\n[4/4] Watching game {game_id}...")
    print("-" * 60)

    # Sign up as spectator
    await watcher.spectate_signup()

    # Poll loop
    poll_count = 0
    last_phase = None
    start_time = time.time()

    while not watcher.game_over and poll_count < args.max_polls:
        poll_count += 1
        update = await watcher.poll()

        # Log raw update
        if update and not update.get("timeout"):
            watcher._log_event({"event": "poll", "poll_number": poll_count, "data": update})

        # Print interesting events
        if "phase_change" in update:
            phase = update["phase_change"]
            if phase != last_phase:
                elapsed = time.time() - start_time
                print(f"  [{elapsed:6.1f}s] Phase: {phase}  (decisions so far: {watcher.decision_count})")
                last_phase = phase

        if "messages" in update:
            for msg in update["messages"]:
                # Only print interesting messages (skip routine ones)
                lower = msg.lower()
                if any(kw in lower for kw in ["deploys", "moves", "attacks", "wins", "loses",
                                                "concedes", "force drain", "battle", "forfeits",
                                                "retrieves", "draws", "lost"]):
                    elapsed = time.time() - start_time
                    print(f"  [{elapsed:6.1f}s] {msg[:120]}")

        if "game_over" in update:
            elapsed = time.time() - start_time
            print(f"\n{'=' * 60}")
            print(f"  GAME OVER after {elapsed:.1f}s and {watcher.decision_count} decisions")
            print(f"  Result: {update.get('result', 'Unknown')}")
            print(f"{'=' * 60}")
            watcher._log_event({
                "event": "game_end",
                "result": update.get("result", ""),
                "elapsed_seconds": elapsed,
                "total_decisions": watcher.decision_count,
                "total_messages": len(watcher.messages),
            })
            break

        if "stats" in update:
            watcher._log_event({"event": "stats", "data": update["stats"]})

        # Small delay between polls to avoid hammering the server
        if update.get("timeout"):
            await asyncio.sleep(0.1)
        else:
            await asyncio.sleep(0.05)

    if not watcher.game_over:
        print(f"\nMax polls ({args.max_polls}) reached without game ending.")

    print(f"\nLog saved to: {watcher.log_path}")
    print(f"Total messages: {len(watcher.messages)}")
    print(f"Total decisions: {watcher.decision_count}")


if __name__ == "__main__":
    asyncio.run(main())
