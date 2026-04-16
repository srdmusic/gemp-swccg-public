#!/usr/bin/env python3
"""
MCP Server for GEMP-SWCCG Game Client.

Provides tools to interact with a running GEMP-SWCCG server: login, create games
against the Rando AI bot, poll game state, submit decisions, and log game outcomes
for cross-session analysis.

Requires a running GEMP-SWCCG instance (typically via Docker at localhost:17001).
"""

import json
import os
import re
import time
from contextlib import asynccontextmanager
from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional
from xml.etree import ElementTree as ET

import httpx
from mcp.server.fastmcp import FastMCP
from pydantic import BaseModel, ConfigDict, Field, field_validator


# ---------------------------------------------------------------------------
# Local card cache — avoids API/file lookups during gameplay
# ---------------------------------------------------------------------------
_CARD_CACHE: Dict[str, Dict[str, str]] = {}

def _load_card_cache():
    """Load the local card blueprint cache from card_cache.json."""
    global _CARD_CACHE
    cache_path = os.path.join(os.path.dirname(__file__), "card_cache.json")
    if os.path.exists(cache_path):
        with open(cache_path, "r") as f:
            _CARD_CACHE = json.load(f)

def card_title(blueprint_id: str) -> str:
    """Look up a card title from its blueprint ID (e.g. '222_29' -> 'Young Skywalker')."""
    if not _CARD_CACHE:
        _load_card_cache()
    info = _CARD_CACHE.get(blueprint_id, {})
    return info.get("title", f"Unknown({blueprint_id})")


# ---------------------------------------------------------------------------
# GEMP API Client (persistent session with cookie management)
# ---------------------------------------------------------------------------

class GempSession:
    """Manages HTTP session state for a single GEMP server connection."""

    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.cookies: Dict[str, str] = {}
        self.username: Optional[str] = None
        self.game_id: Optional[str] = None
        self.channel_number: Optional[int] = None
        self.logged_in: bool = False
        # Game state tracking
        self.current_decision_id: Optional[str] = None
        self.current_decision_type: Optional[str] = None
        self.current_decision_text: Optional[str] = None
        self.current_options: List[Dict[str, Any]] = []
        self.game_messages: List[str] = []
        self.game_phase: Optional[str] = None
        self.game_events: List[Dict[str, Any]] = []
        self.current_decision_full: Optional[Dict[str, Any]] = None
        self.is_my_turn: bool = False  # Track whose turn it is

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

    async def login(self, username: str, password: str) -> Dict[str, Any]:
        async with httpx.AsyncClient(timeout=15.0) as client:
            # Get initial session cookie
            await client.head(self.base_url)

            resp = await client.post(
                f"{self.base_url}/gemp-swccg-server/login",
                data={"login": username, "password": password},
                headers={"Referer": f"{self.base_url}/gemp-swccg/"},
                follow_redirects=True,
            )
            self._store_cookies(resp)

            if resp.status_code == 200 and "set-cookie" in resp.headers:
                self.username = username
                self.logged_in = True
                self._store_cookies(resp)
                return {"status": "ok", "username": username}
            elif resp.status_code == 401:
                return {"status": "error", "message": "Invalid credentials"}
            elif resp.status_code == 403:
                return {"status": "error", "message": "Account banned"}
            else:
                return {"status": "error", "message": f"Login failed: HTTP {resp.status_code}"}

    async def start_server(self) -> Dict[str, Any]:
        """Ensure the game server is running (disable shutdown mode)."""
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                f"{self.base_url}/gemp-swccg-server/admin/shutdown",
                data={"enabled": "false"},
                headers=self._headers(),
            )
            if resp.is_success:
                return {"status": "ok", "message": "Game server is running"}
            return {"status": "error", "message": f"Could not start server: HTTP {resp.status_code}"}

    async def admin_setup(self) -> Dict[str, Any]:
        """Run full admin initialization: startup mode + enable all settings."""
        results = {}
        admin_calls = [
            ("Enter Startup Mode", "/gemp-swccg-server/admin/shutdown", {"enabled": "false"}),
            ("Clear Server Cache", "/gemp-swccg-server/admin/clearcache", {}),
            ("Enable Private Tables", "/gemp-swccg-server/admin/settings/privategames", {"enabled": "true"}),
            ("Enable Bot Tables", "/gemp-swccg-server/admin/settings/aitables", {"enabled": "true"}),
            ("Enable New Player Registration", "/gemp-swccg-server/admin/settings/newaccounts", {"enabled": "true"}),
            ("Enable In-Game Stat Tracking", "/gemp-swccg-server/admin/settings/stattracking", {"enabled": "true"}),
            ("Enable Bonus Abilities", "/gemp-swccg-server/admin/settings/bonusabilities", {"enabled": "true"}),
        ]
        async with httpx.AsyncClient(timeout=15.0) as client:
            for label, path, data in admin_calls:
                try:
                    resp = await client.post(
                        f"{self.base_url}{path}",
                        data=data,
                        headers=self._headers(),
                    )
                    results[label] = "OK" if resp.is_success else f"HTTP {resp.status_code}"
                except Exception as e:
                    results[label] = f"Error: {e}"
        return {"status": "ok", "results": results}

    async def list_decks(self) -> Dict[str, Any]:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.get(
                f"{self.base_url}/gemp-swccg-server/deck/list",
                params={"participantId": self.username},
                headers=self._headers(),
            )
            if not resp.is_success:
                return {"status": "error", "message": f"HTTP {resp.status_code}"}
            try:
                root = ET.fromstring(resp.text)
                decks = []
                for deck_el in root.findall(".//deck"):
                    decks.append(deck_el.attrib.get("name", deck_el.text or ""))
                if not decks:
                    for deck_el in root.iter():
                        if deck_el.text and deck_el.text.strip():
                            decks.append(deck_el.text.strip())
                return {"status": "ok", "decks": decks}
            except ET.ParseError:
                return {"status": "ok", "raw": resp.text[:2000]}

    async def get_hall(self) -> Dict[str, Any]:
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.get(
                f"{self.base_url}/gemp-swccg-server/hall",
                params={"participantId": self.username},
                headers=self._headers(),
            )
            if not resp.is_success:
                return {"status": "error", "message": f"HTTP {resp.status_code}"}
            try:
                root = ET.fromstring(resp.text)
                tables = []
                for t in root.findall(".//table"):
                    tables.append({
                        "id": t.attrib.get("id"),
                        "gameId": t.attrib.get("gameId"),
                        "format": t.attrib.get("format"),
                        "players": t.attrib.get("players", ""),
                        "status": t.attrib.get("status"),
                        "statusDescription": t.attrib.get("statusDescription"),
                    })
                return {"status": "ok", "tables": tables}
            except ET.ParseError:
                return {"status": "ok", "raw": resp.text[:2000]}

    async def create_game_vs_ai(
        self,
        game_format: str,
        deck_name: str,
        ai_skill: str = "RANDO",
        ai_deck_name: str = "",
        ai_deck_sample: bool = True,
        sample_deck: bool = False,
    ) -> Dict[str, Any]:
        async with httpx.AsyncClient(timeout=30.0) as client:
            data = {
                "participantId": self.username,
                "format": game_format,
                "deckName": deck_name,
                "sampleDeck": str(sample_deck).lower(),
                "tableDesc": f"Claude vs {ai_skill}",
                "isPrivate": "false",
                "playVsAi": "true",
                "aiSkill": ai_skill,
                "aiDeckName": ai_deck_name,
                "aiDeckSample": str(ai_deck_sample).lower(),
            }
            resp = await client.post(
                f"{self.base_url}/gemp-swccg-server/hall",
                data=data,
                headers=self._headers(),
            )
            if not resp.is_success:
                return {"status": "error", "message": f"HTTP {resp.status_code}: {resp.text[:500]}"}
            return {"status": "ok", "message": "Table created. Poll hall to find gameId."}

    async def find_my_game(self) -> Dict[str, Any]:
        """Search the hall for an active game belonging to this user."""
        hall = await self.get_hall()
        if hall.get("status") != "ok":
            return hall
        for table in hall.get("tables", []):
            players = table.get("players", "")
            if self.username and self.username in players:
                status = table.get("status", "")
                game_id = table.get("gameId")
                if status == "PLAYING" and game_id:
                    self.game_id = game_id
                    return {"status": "ok", "gameId": game_id, "table": table}
                elif status == "WAITING":
                    return {"status": "waiting", "message": "Game waiting for opponent", "table": table}
        return {"status": "not_found", "message": "No active game found for this user"}

    async def signup_for_game(self, game_id: str) -> Dict[str, Any]:
        """Initial game state fetch — assigns channel number."""
        self.game_id = game_id
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.get(
                f"{self.base_url}/gemp-swccg-server/game/{game_id}",
                params={"participantId": self.username},
                headers=self._headers("game.html"),
            )
            if not resp.is_success:
                return {"status": "error", "message": f"HTTP {resp.status_code}"}
            return self._parse_game_update(resp.text)

    async def poll_game(self, decision_id: Optional[str] = None, decision_value: Optional[str] = None) -> Dict[str, Any]:
        """Poll for game updates, optionally submitting a decision."""
        if not self.game_id:
            return {"status": "error", "message": "No active game. Use signup_for_game first."}

        data: Dict[str, Any] = {
            "participantId": self.username,
            "channelNumber": str(self.channel_number or 0),
        }
        if decision_id is not None and decision_value is not None:
            data["decisionId"] = decision_id
            data["decisionValue"] = decision_value

        async with httpx.AsyncClient(timeout=10.0) as client:
            try:
                resp = await client.post(
                    f"{self.base_url}/gemp-swccg-server/game/{self.game_id}",
                    data=data,
                    headers=self._headers("game.html"),
                )
            except httpx.TimeoutException:
                # Long-poll timeout is normal — just means no new events yet
                return {"status": "ok", "message": "No new events (poll timeout)", "awaiting_decision": self.current_decision_id is not None}

            if resp.status_code == 410:
                # Subscription expired — re-signup
                return await self.signup_for_game(self.game_id)
            if resp.status_code == 409:
                # Auto-retry: re-signup to get a fresh channel, then retry the operation
                import asyncio
                await asyncio.sleep(0.5)
                signup_result = await self.signup_for_game(self.game_id)
                if signup_result.get("status") != "ok":
                    return {"status": "error", "message": "Subscription conflict — re-signup failed"}
                # If we had a decision to submit, retry with new channel
                if decision_id is not None and decision_value is not None:
                    data["channelNumber"] = str(self.channel_number or 0)
                    try:
                        resp = await client.post(
                            f"{self.base_url}/gemp-swccg-server/game/{self.game_id}",
                            data=data,
                            headers=self._headers("game.html"),
                        )
                        if resp.is_success:
                            return self._parse_game_update(resp.text)
                    except httpx.TimeoutException:
                        pass
                    return {"status": "error", "message": "Subscription conflict — retry after re-signup failed"}
                return signup_result
            if not resp.is_success:
                return {"status": "error", "message": f"HTTP {resp.status_code}: {resp.text[:500]}"}

            return self._parse_game_update(resp.text)

    async def create_bot_vs_bot_game(
        self,
        game_format: str,
        light_skill: str,
        light_deck: str,
        dark_skill: str,
        dark_deck: str,
        deck_owner: str = "",
    ) -> Dict[str, Any]:
        """Create a bot-vs-bot game via the admin endpoint."""
        async with httpx.AsyncClient(timeout=30.0) as client:
            data = {
                "format": game_format,
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
                return {"status": "error", "message": f"HTTP {resp.status_code}: {resp.text[:500]}"}

            body = resp.text.strip()
            if body.startswith("OK gameId="):
                game_id = body.replace("OK gameId=", "").strip()
                return {"status": "ok", "gameId": game_id, "message": body}
            elif body.startswith("ERROR:"):
                return {"status": "error", "message": body}
            else:
                return {"status": "ok", "raw": body}

    async def concede(self) -> Dict[str, Any]:
        if not self.game_id:
            return {"status": "error", "message": "No active game"}
        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(
                f"{self.base_url}/gemp-swccg-server/game/{self.game_id}/concede",
                data={"participantId": self.username},
                headers=self._headers("game.html"),
            )
            return {"status": "ok" if resp.is_success else "error", "message": resp.text[:500]}

    # -----------------------------------------------------------------------
    # XML Parsing — converts raw game XML into structured, readable data
    # -----------------------------------------------------------------------

    def _parse_game_update(self, xml_text: str) -> Dict[str, Any]:
        """Parse game update XML into structured data with readable decisions."""
        try:
            root = ET.fromstring(xml_text)
        except ET.ParseError:
            return {"status": "error", "message": f"XML parse error: {xml_text[:500]}"}

        # Update channel number
        cn = root.attrib.get("cn")
        if cn:
            self.channel_number = int(cn)

        result: Dict[str, Any] = {"status": "ok", "channel": self.channel_number}

        # Parse game events
        messages = []
        warnings = []
        cards_in_play = []
        game_stats = {}
        decision = None
        phase_change = None
        game_over = False
        winner = None

        for ge in root.findall(".//ge"):
            event_type = ge.attrib.get("type", "")

            if event_type == "M":  # Message
                msg = ge.attrib.get("message", "")
                if msg:
                    # Track whose turn it is
                    if "Start of " in msg and "'s turn" in msg:
                        if self.username and self.username in msg:
                            self.is_my_turn = True
                        else:
                            self.is_my_turn = False
                    messages.append(msg)
                    self.game_messages.append(msg)

            elif event_type == "W":  # Warning
                msg = ge.attrib.get("message", "")
                if msg:
                    warnings.append(msg)

            elif event_type == "GPC":  # Game phase change
                phase = ge.attrib.get("phase", "")
                if phase:
                    phase_change = phase
                    self.game_phase = phase

            elif event_type == "D":  # Decision awaiting
                decision = self._parse_decision(ge)

            elif event_type == "GS":  # Game stats
                game_stats = self._parse_game_stats(ge)

            elif event_type in ("PCIP", "PCIPAR"):  # Card in play
                card_info = {
                    "cardId": ge.attrib.get("cardId"),
                    "blueprintId": ge.attrib.get("blueprintId"),
                    "zone": ge.attrib.get("zone"),
                    "owner": ge.attrib.get("participantId"),
                    "locationIndex": ge.attrib.get("locationIndex"),
                    "testingText": ge.attrib.get("testingText", ""),
                }
                cards_in_play.append(card_info)

            elif event_type == "RCFP":  # Card removed from play
                pass  # Could track removals

            # Check for game over conditions in messages
            # IMPORTANT: "loses a Force" is a force drain, NOT game over.
            # Only trigger on actual win/concede messages.
            for msg in messages:
                msg_lower = msg.lower()
                if "concedes" in msg_lower:
                    game_over = True
                    winner = msg
                elif "wins" in msg_lower and ("wins the game" in msg_lower or "has won" in msg_lower):
                    game_over = True
                    winner = msg

        if messages:
            result["messages"] = messages[-10:]  # Last 10 messages
        if warnings:
            result["warnings"] = warnings
        if phase_change:
            result["phase"] = phase_change
        if cards_in_play:
            result["cards_entered_play"] = cards_in_play[:20]  # Cap for readability
        if game_stats:
            result["game_stats"] = game_stats
        if game_over:
            result["game_over"] = True
            result["result"] = winner

        if decision:
            self.current_decision_id = decision.get("id")
            self.current_decision_type = decision.get("type")
            self.current_decision_text = decision.get("text")
            self.current_options = decision.get("options", [])
            self.current_decision_full = decision  # Store full parsed decision
            result["decision"] = decision
        else:
            result["awaiting_decision"] = self.current_decision_id is not None

        return result

    def _parse_decision(self, ge: ET.Element) -> Dict[str, Any]:
        """Parse a decision event into a readable structure."""
        decision_id = ge.attrib.get("id", "")
        decision_type = ge.attrib.get("decisionType", "")
        decision_text = ge.attrib.get("text", "")

        decision: Dict[str, Any] = {
            "id": decision_id,
            "type": decision_type,
            "text": decision_text,
        }

        # Extract turn/revert metadata from decision parameters
        raw_params = {}
        for param in ge.findall("parameter"):
            name = param.attrib.get("name", "")
            value = param.attrib.get("value", "")
            if name in ("yourTurn", "revertEligible", "autoPassEligible"):
                raw_params[name] = value

        if raw_params.get("yourTurn") == "true":
            self.is_my_turn = True
        elif raw_params.get("yourTurn") == "false":
            self.is_my_turn = False

        if raw_params.get("revertEligible") == "true":
            decision["revert_eligible"] = True

        # Collect parameters
        params: Dict[str, List[str]] = {}
        for param in ge.findall("parameter"):
            name = param.attrib.get("name", "")
            value = param.attrib.get("value", "")
            if name not in params:
                params[name] = []
            params[name].append(value)

        # Build readable options based on decision type
        if decision_type == "MULTIPLE_CHOICE":
            options = []
            results = params.get("results", [])
            default_idx = params.get("defaultIndex", ["0"])[0]
            for i, opt in enumerate(results):
                options.append({
                    "index": str(i),
                    "text": opt,
                    "is_default": str(i) == default_idx,
                })
            decision["options"] = options
            decision["respond_with"] = "index number as string (e.g. '0', '1')"

        elif decision_type == "INTEGER":
            decision["min"] = params.get("min", ["0"])[0]
            decision["max"] = params.get("max", ["0"])[0]
            decision["default"] = params.get("defaultValue", ["0"])[0]
            decision["respond_with"] = f"number between {decision['min']} and {decision['max']}"

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
            decision["respond_with"] = "action index as string (e.g. '0', '1', '2')"

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
                    # Enrich with local card cache title
                    card["title"] = card_title(blueprint_ids[i])
                if i < len(testing_texts) and testing_texts[i] and testing_texts[i] != "null":
                    card["title"] = testing_texts[i]  # Server title overrides cache
                if i < len(selectables):
                    card["selectable"] = selectables[i] == "true"
                cards.append(card)
            decision["cards"] = cards
            min_sel = params.get("min", ["0"])[0]
            max_sel = params.get("max", [str(len(card_ids))])[0]
            decision["min"] = min_sel
            decision["max"] = max_sel
            decision["respond_with"] = f"comma-separated card IDs (select {min_sel}-{max_sel})"

        else:
            # Unknown type — dump raw params
            decision["raw_params"] = {k: v for k, v in params.items()}
            decision["respond_with"] = "raw value string"

        return decision

    def _parse_game_stats(self, ge: ET.Element) -> Dict[str, Any]:
        """Parse game stats event."""
        stats: Dict[str, Any] = {}
        for pz in ge.findall("playerZones"):
            player = pz.attrib.get("name", "unknown")
            stats[player] = {k: v for k, v in pz.attrib.items() if k != "name"}

        # Power at locations
        for tag in ("darkPowerAtLocations", "lightPowerAtLocations"):
            el = ge.find(tag)
            if el is not None:
                stats[tag] = dict(el.attrib)

        return stats


# ---------------------------------------------------------------------------
# Game Logger — writes structured game logs for cross-session learning
# ---------------------------------------------------------------------------

class GameLogger:
    """Writes game events to persistent log files."""

    def __init__(self, log_dir: str):
        self.log_dir = log_dir
        os.makedirs(log_dir, exist_ok=True)

    def start_game(self, game_id: str, player_deck: str, ai_skill: str, ai_deck: str) -> str:
        """Create a new game log file. Returns the log path."""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"game_{timestamp}_{game_id[:8]}.jsonl"
        path = os.path.join(self.log_dir, filename)
        self._append(path, {
            "event": "game_start",
            "timestamp": datetime.now().isoformat(),
            "game_id": game_id,
            "player_deck": player_deck,
            "ai_skill": ai_skill,
            "ai_deck": ai_deck,
        })
        return path

    def log_decision(self, log_path: str, decision: Dict[str, Any], response: str, reasoning: str = "") -> None:
        self._append(log_path, {
            "event": "decision",
            "timestamp": datetime.now().isoformat(),
            "decision_id": decision.get("id"),
            "decision_type": decision.get("type"),
            "decision_text": decision.get("text", "")[:200],
            "options_count": len(decision.get("options", decision.get("cards", []))),
            "response": response,
            "reasoning": reasoning,
        })

    def log_game_end(self, log_path: str, result: str, messages: List[str]) -> None:
        self._append(log_path, {
            "event": "game_end",
            "timestamp": datetime.now().isoformat(),
            "result": result,
            "final_messages": messages[-20:],
        })

    def log_observation(self, log_path: str, observation: str) -> None:
        self._append(log_path, {
            "event": "observation",
            "timestamp": datetime.now().isoformat(),
            "text": observation,
        })

    def _append(self, path: str, data: Dict[str, Any]) -> None:
        with open(path, "a") as f:
            f.write(json.dumps(data) + "\n")


# ---------------------------------------------------------------------------
# Global session state (persists across tool calls within one MCP session)
# ---------------------------------------------------------------------------

_session: Optional[GempSession] = None
_logger: Optional[GameLogger] = None
_current_log_path: Optional[str] = None


def _get_session() -> GempSession:
    global _session
    if _session is None:
        base_url = os.environ.get("GEMP_BASE_URL", "http://localhost:17001")
        _session = GempSession(base_url)
    return _session


def _get_logger() -> GameLogger:
    global _logger
    if _logger is None:
        log_dir = os.environ.get("GEMP_LOG_DIR", os.path.join(os.path.dirname(__file__), "game_logs"))
        _logger = GameLogger(log_dir)
    return _logger


# ---------------------------------------------------------------------------
# MCP Server and Tools
# ---------------------------------------------------------------------------

mcp = FastMCP("gemp_swccg_mcp")


# --- Input Models ---

class LoginInput(BaseModel):
    """Login credentials for the GEMP server."""
    model_config = ConfigDict(str_strip_whitespace=True)

    username: str = Field(..., description="GEMP username (e.g. 'test1')", min_length=1, max_length=50)
    password: str = Field(..., description="GEMP password (e.g. 'test')", min_length=1, max_length=100)
    base_url: Optional[str] = Field(
        default=None,
        description="GEMP server URL (default: http://localhost:17001)"
    )


class CreateGameInput(BaseModel):
    """Parameters for creating a game against an AI opponent."""
    model_config = ConfigDict(str_strip_whitespace=True)

    deck_name: str = Field(..., description="Your deck name (must exist on the server)", min_length=1)
    game_format: str = Field(default="open", description="Game format code (e.g. 'open', 'classic')")
    ai_skill: str = Field(default="RANDO", description="AI level: 'BEGINNER', 'ADVANCED', or 'RANDO'")
    ai_deck_name: str = Field(default="", description="AI deck name (empty = library default)")
    sample_deck: bool = Field(default=False, description="Use a library/sample deck for yourself")
    ai_deck_sample: bool = Field(default=True, description="Use a library/sample deck for the AI")


class SubmitDecisionInput(BaseModel):
    """Submit a response to a pending game decision."""
    model_config = ConfigDict(str_strip_whitespace=True)

    decision_value: str = Field(..., description="Your decision response (index, card IDs, or number)")
    reasoning: Optional[str] = Field(default="", description="Why you chose this (logged for analysis)")


class GameIdInput(BaseModel):
    """Input requiring a specific game ID."""
    model_config = ConfigDict(str_strip_whitespace=True)

    game_id: str = Field(..., description="The GEMP game ID string", min_length=1)


class ObservationInput(BaseModel):
    """Record a game observation for cross-session learning."""
    model_config = ConfigDict(str_strip_whitespace=True)

    observation: str = Field(..., description="What you observed about the game or Rando's behavior", min_length=5)


class BotVsBotInput(BaseModel):
    """Parameters for creating a bot-vs-bot game."""
    model_config = ConfigDict(str_strip_whitespace=True)

    game_format: str = Field(default="open", description="Game format code (e.g. 'open', 'classic')")
    light_skill: str = Field(default="CHOSENONE", description="AI skill for Light Side: 'BEGINNER', 'ADVANCED', 'RANDO', 'CHOSENONE'")
    light_deck: str = Field(default="LUKE SAGA TATOOINE", description="Light Side deck name")
    dark_skill: str = Field(default="RANDO", description="AI skill for Dark Side: 'BEGINNER', 'ADVANCED', 'RANDO', 'CHOSENONE'")
    dark_deck: str = Field(default="DARK DEAL", description="Dark Side deck name")
    deck_owner: str = Field(default="test1", description="Player account that owns the decks (default: test1)")


# --- Tools ---

@mcp.tool(
    name="gemp_admin_setup",
    annotations={
        "title": "Initialize GEMP Admin Settings",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def gemp_admin_setup() -> str:
    """Run full GEMP server admin initialization.

    Calls all admin panel endpoints in one shot:
    - Enter Startup Mode (disable shutdown)
    - Clear Server Cache
    - Enable Private Tables
    - Enable Bot Tables (required for AI games)
    - Enable New Player Registration
    - Enable In-Game Stat Tracking
    - Enable Bonus Abilities

    Must be logged in as an admin user first.

    Returns:
        JSON with status of each admin action.
    """
    session = _get_session()
    if not session.logged_in:
        return json.dumps({"status": "error", "message": "Not logged in. Call gemp_login first."})
    result = await session.admin_setup()
    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_login",
    annotations={
        "title": "Login to GEMP Server",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def gemp_login(params: LoginInput) -> str:
    """Login to a running GEMP-SWCCG server and establish a session.

    This must be called before any other GEMP tools. The session persists
    across subsequent tool calls. Default server is localhost:17001.

    Returns:
        JSON with login status and username.
    """
    global _session
    base_url = params.base_url or os.environ.get("GEMP_BASE_URL", "http://localhost:17001")
    _session = GempSession(base_url)
    result = await _session.login(params.username, params.password)

    if result["status"] == "ok":
        # Also start the game server
        await _session.start_server()

    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_list_decks",
    annotations={
        "title": "List Available Decks",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def gemp_list_decks() -> str:
    """List all decks available to the logged-in user.

    Returns:
        JSON with list of deck names.
    """
    session = _get_session()
    if not session.logged_in:
        return json.dumps({"status": "error", "message": "Not logged in. Call gemp_login first."})
    result = await session.list_decks()
    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_get_hall",
    annotations={
        "title": "View Game Hall",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def gemp_get_hall() -> str:
    """View the game hall (lobby) showing all active and waiting tables.

    Returns:
        JSON with list of tables including gameId, players, status, format.
    """
    session = _get_session()
    if not session.logged_in:
        return json.dumps({"status": "error", "message": "Not logged in. Call gemp_login first."})
    result = await session.get_hall()
    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_create_game",
    annotations={
        "title": "Create Game vs AI",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": False,
        "openWorldHint": True,
    },
)
async def gemp_create_game(params: CreateGameInput) -> str:
    """Create a new game table against an AI opponent (Rando, Advanced, or Beginner).

    After creation, call gemp_find_game to locate the game, then gemp_join_game
    to get the initial state and start playing.

    Returns:
        JSON with creation status. Follow up with gemp_find_game.
    """
    session = _get_session()
    if not session.logged_in:
        return json.dumps({"status": "error", "message": "Not logged in. Call gemp_login first."})

    result = await session.create_game_vs_ai(
        game_format=params.game_format,
        deck_name=params.deck_name,
        ai_skill=params.ai_skill,
        ai_deck_name=params.ai_deck_name,
        ai_deck_sample=params.ai_deck_sample,
        sample_deck=params.sample_deck,
    )
    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_create_bot_vs_bot",
    annotations={
        "title": "Create Bot vs Bot Game",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": False,
        "openWorldHint": True,
    },
)
async def gemp_create_bot_vs_bot(params: BotVsBotInput) -> str:
    """Create a bot-vs-bot game where two AI players play against each other.

    Requires admin login. Decks are loaded from the specified deck owner account.
    Defaults: The Chosen One (Light, LUKE SAGA TATOOINE) vs Rando (Dark, DARK DEAL) from test1.
    After creation, use gemp_get_hall to find the game, then spectate it.

    Returns:
        JSON with the game ID of the newly created bot-vs-bot game.
    """
    session = _get_session()
    if not session.logged_in:
        return json.dumps({"status": "error", "message": "Not logged in. Call gemp_login first."})

    result = await session.create_bot_vs_bot_game(
        game_format=params.game_format,
        light_skill=params.light_skill,
        light_deck=params.light_deck,
        dark_skill=params.dark_skill,
        dark_deck=params.dark_deck,
        deck_owner=params.deck_owner,
    )
    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_find_game",
    annotations={
        "title": "Find My Active Game",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def gemp_find_game() -> str:
    """Search the hall for an active game belonging to the logged-in user.

    Returns the gameId if found. Call gemp_join_game next with that ID.

    Returns:
        JSON with gameId if found, or waiting/not_found status.
    """
    session = _get_session()
    if not session.logged_in:
        return json.dumps({"status": "error", "message": "Not logged in. Call gemp_login first."})
    result = await session.find_my_game()
    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_join_game",
    annotations={
        "title": "Join/Signup for Game",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def gemp_join_game(params: GameIdInput) -> str:
    """Join a game by ID and get the initial game state.

    This assigns a channel number and returns the first game events,
    which may include an initial decision to make (e.g., choosing starting cards).

    Returns:
        JSON with game state, current phase, and any pending decision.
    """
    global _current_log_path
    session = _get_session()
    if not session.logged_in:
        return json.dumps({"status": "error", "message": "Not logged in. Call gemp_login first."})

    result = await session.signup_for_game(params.game_id)

    # Start game log
    logger = _get_logger()
    _current_log_path = logger.start_game(
        game_id=params.game_id,
        player_deck="(unknown)",
        ai_skill="RANDO",
        ai_deck="(unknown)",
    )

    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_poll",
    annotations={
        "title": "Poll Game State",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    },
)
async def gemp_poll() -> str:
    """Poll the game for new events and pending decisions.

    Call this repeatedly to get game updates. Returns new messages,
    phase changes, and any decision awaiting your response.

    Returns:
        JSON with game events, messages, and current decision (if any).
    """
    session = _get_session()
    if not session.game_id:
        return json.dumps({"status": "error", "message": "No active game. Join a game first."})
    result = await session.poll_game()
    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_submit_decision",
    annotations={
        "title": "Submit Game Decision",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": False,
        "openWorldHint": True,
    },
)
async def gemp_submit_decision(params: SubmitDecisionInput) -> str:
    """Submit a response to the current pending decision.

    The decision_value format depends on the decision type:
    - MULTIPLE_CHOICE: index string ("0", "1", "2")
    - INTEGER: number string ("5")
    - CARD_ACTION_CHOICE: action index string ("0", "1")
    - CARD_SELECTION: comma-separated card IDs ("1,3,5")
    - ARBITRARY_CARDS: comma-separated temp IDs ("temp0,temp2")
    - Pass/Done: empty string ""

    Returns:
        JSON with updated game state after the decision.
    """
    session = _get_session()
    if not session.game_id:
        return json.dumps({"status": "error", "message": "No active game."})
    if not session.current_decision_id:
        return json.dumps({"status": "error", "message": "No pending decision. Call gemp_poll first."})

    # Log the decision
    logger = _get_logger()
    if _current_log_path and session.current_decision_text:
        logger.log_decision(
            _current_log_path,
            {"id": session.current_decision_id, "type": session.current_decision_type, "text": session.current_decision_text},
            params.decision_value,
            params.reasoning or "",
        )

    decision_id = session.current_decision_id
    # Clear current decision before submitting (it will be replaced by the response)
    session.current_decision_id = None
    session.current_decision_type = None
    session.current_decision_text = None
    session.current_options = []

    result = await session.poll_game(decision_id=decision_id, decision_value=params.decision_value)

    # Check for game over
    if result.get("game_over") and _current_log_path:
        logger.log_game_end(_current_log_path, result.get("result", ""), session.game_messages)

    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_get_current_decision",
    annotations={
        "title": "Get Current Decision",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": False,
    },
)
async def gemp_get_current_decision() -> str:
    """Get details of the currently pending decision (if any).

    Returns the decision type, text prompt, and all available options
    in a readable format. Use this to understand what you need to decide.

    Returns:
        JSON with decision details, options, and response format guidance.
    """
    session = _get_session()
    if not session.current_decision_id:
        return json.dumps({"status": "no_decision", "message": "No decision pending. Try gemp_poll first."})

    return json.dumps({
        "status": "ok",
        "decision": {
            "id": session.current_decision_id,
            "type": session.current_decision_type,
            "text": session.current_decision_text,
            "options": session.current_options,
        },
        "game_phase": session.game_phase,
    }, indent=2)


@mcp.tool(
    name="gemp_concede",
    annotations={
        "title": "Concede Game",
        "readOnlyHint": False,
        "destructiveHint": True,
        "idempotentHint": False,
        "openWorldHint": True,
    },
)
async def gemp_concede() -> str:
    """Concede the current game. This ends the game immediately.

    Returns:
        JSON with concession status.
    """
    session = _get_session()
    if not session.game_id:
        return json.dumps({"status": "error", "message": "No active game."})

    if _current_log_path:
        logger = _get_logger()
        logger.log_game_end(_current_log_path, "conceded", session.game_messages)

    result = await session.concede()
    return json.dumps(result, indent=2)


@mcp.tool(
    name="gemp_log_observation",
    annotations={
        "title": "Log Game Observation",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": False,
        "openWorldHint": False,
    },
)
async def gemp_log_observation(params: ObservationInput) -> str:
    """Record an observation about the game or Rando's behavior for future analysis.

    Observations are saved to the game log and can be reviewed in later sessions
    to identify patterns and improvement areas.

    Returns:
        JSON confirmation.
    """
    logger = _get_logger()
    if _current_log_path:
        logger.log_observation(_current_log_path, params.observation)
        return json.dumps({"status": "ok", "logged_to": _current_log_path})
    else:
        # Log to a general observations file
        obs_path = os.path.join(logger.log_dir, "observations.jsonl")
        logger.log_observation(obs_path, params.observation)
        return json.dumps({"status": "ok", "logged_to": obs_path})


@mcp.tool(
    name="gemp_game_messages",
    annotations={
        "title": "Get Game Messages",
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": False,
    },
)
async def gemp_game_messages() -> str:
    """Get all game messages received so far in the current game.

    Messages include game narration, card plays, battle results, etc.

    Returns:
        JSON with list of game messages (most recent last).
    """
    session = _get_session()
    messages = session.game_messages
    return json.dumps({
        "status": "ok",
        "total": len(messages),
        "messages": messages[-50:],  # Last 50
        "game_phase": session.game_phase,
    }, indent=2)


@mcp.tool(
    name="gemp_advance",
    annotations={
        "title": "Auto-Advance Game",
        "readOnlyHint": False,
        "destructiveHint": False,
        "idempotentHint": False,
        "openWorldHint": True,
    },
)
async def gemp_advance() -> str:
    """Auto-pass through non-critical game decisions until a real decision appears.

    Automatically handles:
    - Empty "Optional responses" (passes with "")
    - Opponent force activation allowance (allows max)
    - Verification dialogs (passes with "")
    - Phase transitions with no meaningful choices

    Returns when it hits a decision that requires strategic input:
    - Deploy/Battle/Move choices with real options
    - Card selections
    - Any decision with multiple meaningful choices

    Returns:
        JSON with the pending strategic decision, plus a summary of
        auto-passed decisions and key game messages since last call.
    """
    import asyncio

    session = _get_session()
    if not session.game_id:
        return json.dumps({"status": "error", "message": "No active game."})

    auto_passed = 0
    max_auto = 200  # Safety limit
    key_messages: list = []
    phases_seen: list = []

    while auto_passed < max_auto:
        # If no decision pending, poll for one
        if not session.current_decision_id:
            result = await session.poll_game()
            if result.get("game_over"):
                return json.dumps({
                    "status": "game_over",
                    "result": result.get("result", ""),
                    "auto_passed": auto_passed,
                    "messages": key_messages[-20:],
                }, indent=2)
            if result.get("phase"):
                phases_seen.append(result["phase"])
            if result.get("messages"):
                key_messages.extend(result["messages"])
            if not session.current_decision_id:
                # No decision yet, wait and poll again
                await asyncio.sleep(0.5)
                continue

        # We have a decision — check if it's auto-passable
        d_type = session.current_decision_type
        d_text = session.current_decision_text or ""
        d_options = session.current_options
        d_id = session.current_decision_id

        # 1. Empty "Optional responses" — no options available
        if d_type in ("CARD_ACTION_CHOICE", "ACTION_CHOICE") and len(d_options) == 0:
            session.current_decision_id = None
            session.current_decision_type = None
            session.current_decision_text = None
            session.current_options = []
            result = await session.poll_game(decision_id=d_id, decision_value="")
            auto_passed += 1
            if result.get("phase"):
                phases_seen.append(result["phase"])
            if result.get("messages"):
                key_messages.extend(result["messages"])
            if result.get("game_over"):
                return json.dumps({
                    "status": "game_over",
                    "result": result.get("result", ""),
                    "auto_passed": auto_passed,
                    "messages": key_messages[-20:],
                }, indent=2)
            continue

        # 2. "Choose amount of Force to allow opponent to activate" — allow max
        if d_type == "INTEGER" and "allow opponent to activate" in d_text.lower():
            full = session.current_decision_full or {}
            max_val = full.get("max", "10")
            session.current_decision_id = None
            session.current_decision_type = None
            session.current_decision_text = None
            session.current_options = []
            result = await session.poll_game(decision_id=d_id, decision_value=max_val)
            auto_passed += 1
            if result.get("phase"):
                phases_seen.append(result["phase"])
            if result.get("messages"):
                key_messages.extend(result["messages"])
            if result.get("game_over"):
                return json.dumps({
                    "status": "game_over",
                    "result": result.get("result", ""),
                    "auto_passed": auto_passed,
                    "messages": key_messages[-20:],
                }, indent=2)
            continue

        # 3. Verification dialogs (min=0, max=0, no selectable cards)
        if d_type in ("CARD_SELECTION", "ARBITRARY_CARDS"):
            full = session.current_decision_full or {}
            min_val = full.get("min", "1")
            max_val = full.get("max", "1")
            if str(min_val) == "0" and str(max_val) == "0":
                session.current_decision_id = None
                session.current_decision_type = None
                session.current_decision_text = None
                session.current_options = []
                result = await session.poll_game(decision_id=d_id, decision_value="")
                auto_passed += 1
                if result.get("phase"):
                    phases_seen.append(result["phase"])
                if result.get("messages"):
                    key_messages.extend(result["messages"])
                if result.get("game_over"):
                    return json.dumps({
                        "status": "game_over",
                        "result": result.get("result", ""),
                        "auto_passed": auto_passed,
                        "messages": key_messages[-20:],
                    }, indent=2)
                continue

        # 4. Phase-aware auto-pass.
        #    OPPONENT'S TURN: Auto-pass Activate, Control, Move, Draw, End of turn
        #    OPPONENT'S TURN: STOP during Deploy and Battle (so we can play interrupts!)
        #    MY TURN: Auto-pass Activate, Move, Draw, End of turn
        #    MY TURN Control: DO NOT auto-pass (need to force drain!)
        #    MY TURN Deploy/Battle: DO NOT auto-pass (need to deploy/battle!)
        current_phase = (session.game_phase or "").lower()
        if session.is_my_turn:
            auto_pass_phases = ["activate", "move", "draw", "end of turn"]
        else:
            auto_pass_phases = ["activate", "control", "move", "draw", "end of turn"]
            # During opponent's Deploy and Battle, do NOT auto-pass generic options
            # so we can respond with interrupts (Sense, Barrier, battle cards)
        in_auto_pass_phase = any(p in current_phase for p in auto_pass_phases)

        # During opponent's Deploy/Battle, only stop for real interrupt opportunities
        # (decisions with non-empty options that contain deploy/battle-specific responses)
        # For training efficiency, auto-pass generic decisions during opponent's phases
        opponent_action_phase = not session.is_my_turn and any(p in current_phase for p in ["deploy", "battle"])
        # opponent_action_phase flag is available but we don't block auto-pass for generic options
        # TODO: Add smarter interrupt detection that checks for Sense, Barrier, etc. in hand

        if in_auto_pass_phase and d_type in ("CARD_ACTION_CHOICE", "ACTION_CHOICE") and len(d_options) >= 0:
            session.current_decision_id = None
            session.current_decision_type = None
            session.current_decision_text = None
            session.current_options = []
            result = await session.poll_game(decision_id=d_id, decision_value="")
            auto_passed += 1
            if result.get("phase"):
                phases_seen.append(result["phase"])
            if result.get("messages"):
                key_messages.extend(result["messages"])
            if result.get("game_over"):
                return json.dumps({
                    "status": "game_over",
                    "result": result.get("result", ""),
                    "auto_passed": auto_passed,
                    "messages": key_messages[-20:],
                }, indent=2)
            continue

        # 5. "Choose ... action or Pass" — auto-pass UNLESS a real action is present.
        #    Uses a blocklist approach: if ANY option is a real strategic action, stop.
        if d_type in ("CARD_ACTION_CHOICE", "ACTION_CHOICE") and len(d_options) > 0:
            real_action_patterns = [
                "force drain", "initiate a battle", "move using",
            ]
            has_real_action = False
            for opt in d_options:
                opt_text = (opt.get("text") or opt.get("actionText") or "").lower()
                # Bare "Deploy" with no other context = real deploy action
                if opt_text.strip() == "deploy":
                    has_real_action = True
                    break
                # Check for strategic actions
                if any(pat in opt_text for pat in real_action_patterns):
                    has_real_action = True
                    break
                # "Deploy Luke's Lightsaber from Reserve Deck" = real puller action
                if "deploy" in opt_text and "from reserve deck" in opt_text:
                    has_real_action = True
                    break
                # "Deploy card from Reserve Deck" = real Yarna-type pull
                if "deploy card from reserve deck" in opt_text:
                    has_real_action = True
                    break
                # "Deploy a farm from Reserve Deck" = real location pull
                if "deploy a farm" in opt_text:
                    has_real_action = True
                    break
            if not has_real_action:
                session.current_decision_id = None
                session.current_decision_type = None
                session.current_decision_text = None
                session.current_options = []
                result = await session.poll_game(decision_id=d_id, decision_value="")
                auto_passed += 1
                if result.get("phase"):
                    phases_seen.append(result["phase"])
                if result.get("messages"):
                    key_messages.extend(result["messages"])
                if result.get("game_over"):
                    return json.dumps({
                        "status": "game_over",
                        "result": result.get("result", ""),
                        "auto_passed": auto_passed,
                        "messages": key_messages[-20:],
                    }, indent=2)
                continue

        # If we got here, this is a real decision — return it
        decision_summary = session.current_decision_full or {
            "id": d_id,
            "type": d_type,
            "text": d_text,
            "options": d_options,
        }

        # Compact ARBITRARY_CARDS: only return selectable cards to save tokens
        if decision_summary.get("type") in ("ARBITRARY_CARDS", "CARD_SELECTION"):
            cards = decision_summary.get("cards", [])
            selectable = [c for c in cards if c.get("selectable", True)]
            non_selectable_count = len(cards) - len(selectable)
            if selectable or decision_summary.get("min") == "0":
                decision_summary["cards"] = selectable
                if non_selectable_count > 0:
                    decision_summary["hidden_cards"] = non_selectable_count

        return json.dumps({
            "status": "decision",
            "auto_passed": auto_passed,
            "phase": session.game_phase,
            "messages": key_messages[-15:],  # Recent context
            "phases_seen": list(set(phases_seen)),
            "decision": decision_summary,
            "game_stats": _get_latest_stats(session),
        }, indent=2)

    return json.dumps({
        "status": "timeout",
        "message": f"Hit auto-pass limit ({max_auto}). Something may be stuck.",
        "auto_passed": auto_passed,
        "messages": key_messages[-20:],
    }, indent=2)


def _get_latest_stats(session: 'GempSession') -> dict:
    """Get a compact version of game stats."""
    # Return whatever stats we have cached
    return {
        "game_phase": session.game_phase,
        "game_id": session.game_id,
    }


if __name__ == "__main__":
    mcp.run()
