#!/usr/bin/env python3
"""
K-2 Autonomous SWCCG Player

Plays SWCCG games against Rando via the GEMP HTTP API, following Steve's
Dark Deal and Luke Saga playbooks from SKILL.md. No MCP overhead — runs
locally and reports results when done.

Usage:
    python3 k2_player.py --deck "DARK DEAL" --games 5
    python3 k2_player.py --deck "LUKE SAGA TATOOINE" --games 5
    python3 k2_player.py --all  # 5 Dark + 5 Light
"""

import argparse
import asyncio
import json
import os
import re
import sys
import time
from datetime import datetime
from typing import Any, Dict, List, Optional

import httpx

# Import GempSession and card_title from the MCP server module
sys.path.insert(0, os.path.dirname(__file__))
from gemp_mcp import GempSession, GameLogger, card_title, _load_card_cache


# ---------------------------------------------------------------------------
# Strategy constants
# ---------------------------------------------------------------------------

# Cards we want to pull during Activate phase
TDIGWATT_PRIORITY = ["Bespin", "Cloud City Occupation", "Dark Deal"]
ENDOR_SHIELD_TARGET = ["Admiral Piett", "Chiraneau"]

# K&D shield priority
KD_SHIELDS = ["Allegations Of Corruption", "Secret Plans", "Battle Order"]

# Cards to protect (never lose from hand if avoidable)
PROTECT_CARDS = {"Ghhhk", "Lando Calrissian, Vader's Broker", "Cloud City Occupation"}

# Blueprint IDs for key cards
BP_BESPIN = "223_8"
BP_CC_OCCUPATION = "7_223"
BP_DARK_DEAL = "223_9"
BP_DINING_ROOM = "226_1"
BP_UPPER_WALKWAY = "7_273"
BP_SECURITY_TOWER = "200_126"
BP_CARBONITE_CHAMBER = "5_166"
BP_LANDO_BROKER = "217_13"
BP_LOBOT = "223_16"
BP_PIETT = "9_98"
BP_CHIRANEAU = "9_97"
BP_EXECUTOR = "4_167"


class K2Player:
    """Autonomous SWCCG player following Steve's playbooks."""

    def __init__(self, base_url: str = "http://localhost:17001"):
        self.session = GempSession(base_url)
        self.logger = GameLogger(os.path.join(os.path.dirname(__file__), "game_logs"))
        self.log_path: Optional[str] = None
        self.decisions_made = 0
        self.turn = 0
        self.my_drains = 0
        self.opp_drains = 0
        self.game_over = False
        self.winner: Optional[str] = None
        self.hand_blueprints: Dict[str, str] = {}  # cardId -> blueprintId
        # Phase tracking to prevent loops
        self.activated_this_turn = False
        self.activate_pulls_done = 0  # How many "Take card" used during Activate
        self.kd_shields_played = 0
        self.deploys_this_turn = 0
        self.draws_this_turn = 0
        self.last_decision_text = ""
        self.same_decision_count = 0

    def _clean_html(self, text: str) -> str:
        """Strip HTML card hints from message text."""
        text = re.sub(r"<div class='cardHint'[^>]*>([^<]*)</div>", r'\1', text)
        return text

    def _track_hand(self, result: Dict[str, Any]):
        """Track cards entering/leaving hand from game events."""
        for card in result.get("cards_entered_play", []):
            if card.get("zone") == "HAND" and card.get("owner") == self.session.username:
                self.hand_blueprints[card["cardId"]] = card.get("blueprintId", "")

    def _track_messages(self, result: Dict[str, Any]):
        """Track game state from messages."""
        for msg in result.get("messages", []):
            clean = self._clean_html(msg)
            if "Start of" in clean and "turn" in clean:
                m = re.search(r"turn #(\d+)", clean)
                if m:
                    self.turn = int(m.group(1))
                    # Reset per-turn state
                    self.activated_this_turn = False
                    self.activate_pulls_done = 0
                    self.kd_shields_played = 0
                    self.deploys_this_turn = 0
                    self.draws_this_turn = 0
            if "activated" in clean and "Force" in clean and self.session.username in clean:
                self.activated_this_turn = True
            if "Force drain" in clean and "initiates" in clean:
                if self.session.username and self.session.username in clean:
                    self.my_drains += 1
                else:
                    self.opp_drains += 1
            if "is the winner" in clean:
                self.game_over = True
                if self.session.username and self.session.username in clean:
                    self.winner = "K2"
                else:
                    self.winner = "RANDO"
            if "conceded" in clean.lower():
                self.game_over = True
                if self.session.username and self.session.username in clean:
                    self.winner = "RANDO"
                else:
                    self.winner = "K2"
            if "lost due to" in clean.lower():
                self.game_over = True
                if self.session.username and self.session.username in clean:
                    self.winner = "RANDO"
                else:
                    self.winner = "K2"

    def _check_game_over(self, result: Dict[str, Any]) -> bool:
        """Check if game ended."""
        if result.get("game_over"):
            self.game_over = True
            return True
        self._track_messages(result)
        return self.game_over

    # -------------------------------------------------------------------
    # Decision-making engine
    # -------------------------------------------------------------------

    def decide(self, decision: Dict[str, Any]) -> str:
        """Make a strategic decision based on the current game state."""
        dtype = decision.get("type", "")
        text = self._clean_html(decision.get("text", ""))

        # Game start
        if "Select OK to start game" in text:
            return "0"

        # Force activation prompt
        if "You have not activated Force" in text:
            return "1"  # No, don't pass without activating

        # Choose amount of force — activate all, allow opponent max
        if dtype == "INTEGER":
            if "Force to activate" in text:
                return decision.get("max", "0")  # Activate all
            if "Force to allow" in text or "opponent to activate" in text:
                return decision.get("max", "0")  # Allow opponent max (auto-pass)
            # Default for any INTEGER: use max
            return decision.get("default", decision.get("max", "0"))

        # Draw battle destiny
        if "draw" in text.lower() and "battle destiny" in text.lower():
            return "0"  # Yes, always draw

        # Starting card selection: Cloud City site
        if "Cloud City battleground site" in text:
            # Deploy Upper Walkway
            for card in decision.get("cards", []):
                if "Upper Walkway" in card.get("title", ""):
                    return card["cardId"]
            return decision["cards"][0]["cardId"]

        # Starting interrupt
        if "starting interrupt" in text.lower():
            for card in decision.get("cards", []):
                if "Slip Sliding Away" in card.get("title", ""):
                    return card["cardId"]
            if decision.get("cards"):
                return decision["cards"][0]["cardId"]
            return ""

        # SSA effect deployment from Reserve
        if dtype == "ARBITRARY_CARDS" and "deploy from Reserve Deck" in text:
            return self._pick_ssa_or_reserve_card(decision)

        # Verify Reserve Deck (failed search) — just accept
        if dtype == "ARBITRARY_CARDS" and "Verify Reserve Deck" in text:
            return ""

        # TDIGWATT / Endor Shield search
        if dtype == "ARBITRARY_CARDS" and "Choose card to take into hand" in text:
            return self._pick_search_card(decision)

        # K&D shield selection
        if dtype == "ARBITRARY_CARDS" and "click 'Done' to cancel" in text:
            return self._pick_kd_shield(decision)

        # Location placement (Left/Right)
        if dtype == "MULTIPLE_CHOICE" and ("Left" in str(decision.get("options", [])) or
                                            any("Left" in o.get("text", "") for o in decision.get("options", []))):
            return "0"  # Always left

        # Capacity slot (Pilot/Passenger)
        if dtype == "MULTIPLE_CHOICE" and "capacity slot" in text.lower():
            return "0"  # Pilot

        # Choose effect (Ezra Bridger etc)
        if dtype == "MULTIPLE_CHOICE" and "Choose effect" in text:
            # Take force gen reduction over giving opponent activation
            return "0"

        # CARD_ACTION_CHOICE — the main phase decisions
        if dtype in ("CARD_ACTION_CHOICE", "ACTION_CHOICE"):
            return self._pick_action(decision)

        # CARD_SELECTION — choosing where to deploy or what to forfeit
        if dtype == "CARD_SELECTION":
            return self._pick_card_selection(decision)

        # ARBITRARY_CARDS with selectable cards
        if dtype == "ARBITRARY_CARDS":
            return self._pick_arbitrary(decision)

        # Default: pass
        return ""

    def _pick_ssa_or_reserve_card(self, decision: Dict[str, Any]) -> str:
        """Pick card to deploy from Reserve (SSA effects or I'm Sorry)."""
        cards = decision.get("cards", [])
        selectable = [c for c in cards if c.get("selectable")]

        # Priority: Dining Room > AMSD > Endor Shield > Fighters > Security Tower > Carbonite
        priority = ["Dining Room", "Alert My Star Destroyer", "Endor Shield",
                     "Fighters", "Security Tower", "Carbonite Chamber"]
        for target in priority:
            for card in selectable:
                title = card.get("title", "")
                if target.lower() in title.lower():
                    return card["cardId"]

        # Take first selectable
        if selectable:
            return selectable[0]["cardId"]
        return ""

    def _pick_search_card(self, decision: Dict[str, Any]) -> str:
        """Pick card from Reserve Deck search (TDIGWATT, Endor Shield, etc)."""
        cards = decision.get("cards", [])
        selectable = [c for c in cards if c.get("selectable")]

        # Priority for TDIGWATT: Bespin > CC Occupation > Dark Deal
        for target in TDIGWATT_PRIORITY + ENDOR_SHIELD_TARGET:
            for card in selectable:
                title = card.get("title", "")
                if target.lower() in title.lower():
                    return card["cardId"]

        # Take first selectable
        if selectable:
            return selectable[0]["cardId"]
        return ""

    def _pick_kd_shield(self, decision: Dict[str, Any]) -> str:
        """Pick K&D defensive shield. Stop after 2 to avoid looping."""
        if self.kd_shields_played > 2:
            return ""  # Done

        cards = decision.get("cards", [])
        selectable = [c for c in cards if c.get("selectable")]

        for target in KD_SHIELDS:
            for card in selectable:
                if target.lower() in card.get("title", "").lower():
                    return card["cardId"]

        # No priority shields left — done
        return ""

    def _pick_action(self, decision: Dict[str, Any]) -> str:
        """Pick action during a game phase (Activate/Control/Deploy/Battle/Move/Draw)."""
        text = self._clean_html(decision.get("text", ""))
        options = decision.get("options", [])

        if not options:
            return ""  # Auto-pass empty optional responses

        # Loop detection: if we see the same decision text 5+ times, pass
        if text == self.last_decision_text:
            self.same_decision_count += 1
            if self.same_decision_count > 4:
                self.same_decision_count = 0
                return ""
        else:
            self.last_decision_text = text
            self.same_decision_count = 0

        # Build action map
        actions = {}
        for opt in options:
            actions[opt.get("text", "")] = opt.get("index", opt.get("actionId", ""))

        # ACTIVATE PHASE
        if "Activate action" in text:
            # If already activated, just pass
            if self.activated_this_turn:
                return ""

            # First: Activate Force (do this before pulls if we haven't yet)
            # But allow 2-3 pulls first for TDIGWATT/Endor Shield/K&D
            if self.activate_pulls_done < 3 and not self.activated_this_turn:
                # Try TDIGWATT / Endor Shield (once-per-turn effects)
                if self.activate_pulls_done < 2:
                    for label in ["Take card into hand from Reserve Deck"]:
                        if label in actions:
                            self.activate_pulls_done += 1
                            return actions[label]
                # Play K&D shield
                if self.kd_shields_played < 2 and "Play a card" in actions:
                    self.kd_shields_played += 1
                    self.activate_pulls_done += 1
                    return actions["Play a card"]

            # Now activate force
            if "Activate Force" in actions:
                return actions["Activate Force"]
            return ""

        # CONTROL PHASE
        if "Control action" in text:
            # Steve's order: CC Occupation > Lando shuttle > drain all > pass
            for label, idx in actions.items():
                if "Make opponent lose" in label:
                    return idx
            if "Have your Lando make a regular move" in actions:
                return actions["Have your Lando make a regular move"]
            for label, idx in actions.items():
                if "Force drain" in label:
                    return idx
            return ""

        # DEPLOY PHASE
        if "Deploy action" in text:
            # Limit deploys to prevent infinite loops
            if self.deploys_this_turn > 8:
                return ""
            self.deploys_this_turn += 1

            # Priority: I'm Sorry > Deploy Lando from Reserve > Deploy from hand > Pass
            if "Deploy site from Reserve Deck" in actions and self.deploys_this_turn <= 2:
                return actions["Deploy site from Reserve Deck"]
            if "Deploy Lando from Reserve Deck" in actions and self.deploys_this_turn <= 3:
                return actions["Deploy Lando from Reserve Deck"]
            # Deploy cards from hand (try each Deploy option)
            for label, idx in actions.items():
                if label == "Deploy":
                    return idx
            return ""

        # BATTLE PHASE
        if "Battle action" in text:
            if "Initiate battle" in actions:
                return actions["Initiate battle"]
            return ""

        # DRAW PHASE
        if "Draw action" in text:
            if self.draws_this_turn > 15:
                return ""
            self.draws_this_turn += 1
            if "Draw card into hand from Force Pile" in actions:
                return actions["Draw card into hand from Force Pile"]
            return ""

        # MOVE PHASE — usually pass
        if "Move action" in text:
            return ""

        # Force drain add-ons (like Maul's lightsaber)
        if "Force drain" in text and ("add" in text.lower() or "Add" in str(actions)):
            for label, idx in actions.items():
                if "Add" in label or "add" in label:
                    return idx
            return ""

        # Optional responses
        if "Optional responses" in text:
            return ""

        # Deploying card responses — use triggered abilities
        if "Deploying" in text or "just deployed" in text:
            if options:
                return options[0].get("index", options[0].get("actionId", "0"))
            return ""

        # Default: pass
        return ""

    def _pick_card_selection(self, decision: Dict[str, Any]) -> str:
        """Pick card for CARD_SELECTION (deploy location, forfeit, etc)."""
        text = self._clean_html(decision.get("text", ""))
        cards = decision.get("cards", [])

        if not cards:
            return ""

        # Deploying to a location — pick first available
        if "Choose where to deploy" in text:
            # For characters, prefer Dining Room (231) or first CC site
            return cards[0]["cardId"]

        # Location placement adjacency
        if "deploy" in text.lower() and "next to" in text.lower():
            return cards[0]["cardId"]

        # Lando shuttle movement
        if "Choose where to move" in text:
            # Move to a different CC site than current
            return cards[0]["cardId"]

        # Force loss / forfeit
        if "Force to lose" in text or "forfeit" in text:
            # If we can forfeit a character from battle, do that first
            # Otherwise lose from force pile/reserve (not hand)
            for card in cards:
                cid = card["cardId"]
                # Prefer losing from force pile/reserve (not hand cards we need)
                if cid not in self.hand_blueprints:
                    return cid
            # If all are hand cards, lose the first one
            return cards[0]["cardId"]

        # Default: first card
        return cards[0]["cardId"]

    def _pick_arbitrary(self, decision: Dict[str, Any]) -> str:
        """Pick from ARBITRARY_CARDS selection."""
        cards = decision.get("cards", [])
        selectable = [c for c in cards if c.get("selectable")]
        min_sel = int(decision.get("min", "0"))

        if not selectable and min_sel == 0:
            return ""  # Done/cancel

        if selectable:
            return selectable[0]["cardId"]

        return ""

    # -------------------------------------------------------------------
    # Game loop
    # -------------------------------------------------------------------

    async def play_game(self, deck_name: str, ai_deck: str, ai_skill: str = "RANDO") -> Dict[str, Any]:
        """Play a complete game and return results."""
        self.decisions_made = 0
        self.turn = 0
        self.my_drains = 0
        self.opp_drains = 0
        self.game_over = False
        self.winner = None
        self.hand_blueprints = {}

        # Create game
        result = await self.session.create_game_vs_ai(
            game_format="open",
            deck_name=deck_name,
            ai_skill=ai_skill,
            ai_deck_name=ai_deck,
            ai_deck_sample=False,
        )
        if result.get("status") != "ok":
            return {"error": f"Failed to create game: {result}"}

        # Find game
        for _ in range(5):
            result = await self.session.find_my_game()
            if result.get("status") == "ok" and result.get("gameId"):
                break
            await asyncio.sleep(1)
        else:
            return {"error": "Could not find game"}

        game_id = result["gameId"]
        self.log_path = self.logger.start_game(game_id, deck_name, ai_skill, ai_deck)

        # Join game
        result = await self.session.signup_for_game(game_id)
        if result.get("status") != "ok":
            return {"error": f"Failed to join game: {result}"}

        self._track_hand(result)
        self._track_messages(result)

        # Main game loop
        max_decisions = 5000  # Safety limit — games typically need 1000-2000 decisions
        stall_count = 0
        start_time = time.time()

        while not self.game_over and self.decisions_made < max_decisions and (time.time() - start_time) < 600:
            decision = result.get("decision") or self.session.current_decision_full

            if not decision:
                # Poll for new events
                result = await self.session.poll_game()
                if self._check_game_over(result):
                    break
                self._track_hand(result)
                decision = result.get("decision") or self.session.current_decision_full
                if not decision:
                    stall_count += 1
                    if stall_count > 10:
                        break  # Stuck
                    await asyncio.sleep(0.5)
                    continue

            stall_count = 0
            decision_id = decision.get("id")
            if not decision_id:
                result = await self.session.poll_game()
                continue

            # Make decision
            answer = self.decide(decision)
            self.decisions_made += 1

            # Debug: log every 100th decision and first 20
            if self.decisions_made <= 20 or self.decisions_made % 100 == 0:
                dtext = self._clean_html(decision.get("text", ""))[:80]
                print(f"  [{self.decisions_made}] T{self.turn} {decision.get('type','?')}: {dtext} -> '{answer}'")

            # Submit
            result = await self.session.poll_game(
                decision_id=decision_id,
                decision_value=answer,
            )

            if self._check_game_over(result):
                break
            self._track_hand(result)

            # Small delay to avoid hammering the server
            if self.decisions_made % 50 == 0:
                await asyncio.sleep(0.1)

        # Concede if game didn't finish naturally
        if not self.game_over:
            try:
                await self.session.concede()
                self.winner = "RANDO"
                self.game_over = True
            except Exception:
                pass

        # Get final stats
        stats = result.get("game_stats", {})
        my_stats = stats.get(self.session.username, {})
        opp_stats = {}
        for k, v in stats.items():
            if k != self.session.username and isinstance(v, dict) and "LOST_PILE" in v:
                opp_stats = v

        game_result = {
            "game_id": game_id,
            "deck": deck_name,
            "turns": self.turn,
            "decisions": self.decisions_made,
            "my_drains": self.my_drains,
            "opp_drains": self.opp_drains,
            "winner": self.winner or "UNKNOWN",
            "my_lost": my_stats.get("LOST_PILE", "?"),
            "opp_lost": opp_stats.get("LOST_PILE", "?"),
        }

        if self.log_path:
            self.logger.log_game_end(self.log_path, json.dumps(game_result), self.session.game_messages[-20:])

        return game_result


async def main():
    parser = argparse.ArgumentParser(description="K-2 Autonomous SWCCG Player")
    parser.add_argument("--deck", default="DARK DEAL", help="Deck to play")
    parser.add_argument("--ai-deck", default="LUKE SAGA TATOOINE", help="AI opponent deck")
    parser.add_argument("--games", type=int, default=5, help="Number of games")
    parser.add_argument("--all", action="store_true", help="Play 5 Dark + 5 Light")
    parser.add_argument("--url", default="http://localhost:17001", help="GEMP server URL")
    args = parser.parse_args()

    _load_card_cache()

    player = K2Player(args.url)

    # Login and setup
    print("K-2 logging in...")
    result = await player.session.login("asdf", "asdf")
    if result.get("status") != "ok":
        print(f"Login failed: {result}")
        return

    result = await player.session.admin_setup()
    print(f"Admin setup: {result.get('status')}")

    # Determine game schedule
    if args.all:
        schedule = [
            ("DARK DEAL", "LUKE SAGA TATOOINE", 5),
            ("LUKE SAGA TATOOINE", "DARK DEAL", 5),
        ]
    else:
        schedule = [(args.deck, args.ai_deck, args.games)]

    all_results = []
    game_num = 0

    for deck, ai_deck, count in schedule:
        for i in range(count):
            game_num += 1
            print(f"\n{'='*50}")
            print(f"Game {game_num}: {deck} vs {ai_deck} (Rando)")
            print(f"{'='*50}")

            try:
                result = await player.play_game(deck, ai_deck)
                all_results.append(result)

                if "error" in result:
                    print(f"  ERROR: {result['error']}")
                else:
                    w = "WIN" if result["winner"] == "K2" else "LOSS"
                    print(f"  {w} | Turns: {result['turns']} | Decisions: {result['decisions']}")
                    print(f"  Drains: {result['my_drains']} vs {result['opp_drains']}")
                    print(f"  Lost Pile: {result['my_lost']} vs {result['opp_lost']}")
            except Exception as e:
                print(f"  CRASH: {e}")
                all_results.append({"error": str(e), "deck": deck})

            # Brief pause between games
            await asyncio.sleep(2)

    # Final summary
    print(f"\n{'='*60}")
    print(f"K-2 SESSION RESULTS")
    print(f"{'='*60}")

    wins = sum(1 for r in all_results if r.get("winner") == "K2")
    losses = sum(1 for r in all_results if r.get("winner") == "RANDO")
    errors = sum(1 for r in all_results if "error" in r)

    print(f"Record: {wins}-{losses} ({errors} errors)")
    print(f"Win rate: {wins*100//(wins+losses) if (wins+losses) > 0 else 0}%")

    for r in all_results:
        if "error" in r:
            print(f"  {r.get('deck','?'):20s} | ERROR: {r['error'][:50]}")
        else:
            w = "WIN " if r["winner"] == "K2" else "LOSS"
            print(f"  {r['deck']:20s} | {w} | T:{r['turns']:2d} | D:{r['my_drains']:2d}v{r['opp_drains']:2d} | Lost:{r['my_lost']}v{r['opp_lost']}")

    # Save summary
    summary_path = os.path.join(os.path.dirname(__file__), "game_logs", f"session_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    with open(summary_path, "w") as f:
        json.dump({"results": all_results, "record": f"{wins}-{losses}"}, f, indent=2)
    print(f"\nSession log: {summary_path}")


if __name__ == "__main__":
    asyncio.run(main())
