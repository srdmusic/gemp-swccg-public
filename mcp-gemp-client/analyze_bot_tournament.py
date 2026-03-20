#!/usr/bin/env python3
"""
Bot Tournament Analyzer for GEMP-SWCCG.

Reads tournament results JSON (from run_bot_tournament.py) and the associated
replay files to identify patterns in wins and losses.

Usage:
    python analyze_bot_tournament.py tournament_results/tournament_XXXXXXXX_XXXXXX.json
    python analyze_bot_tournament.py --latest
    python analyze_bot_tournament.py --replay-dir /Users/steve/gemp-swccg-public/replays --latest

Outputs a detailed markdown analysis report.
"""

import argparse
import json
import os
import re
import sys
import zlib
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


# ---------------------------------------------------------------------------
# HTML Stripping
# ---------------------------------------------------------------------------

def strip_html(text: str) -> str:
    """Remove HTML tags from game messages, preserving card names from value attrs."""
    # Extract card names from cardHint divs: <div class='cardHint' value='XXX'>CardName</div>
    text = re.sub(r"<div class='cardHint'[^>]*>(.*?)</div>", r"\1", text)
    # Remove any remaining HTML tags
    text = re.sub(r"<[^>]+>", "", text)
    return text.strip()


def extract_card_name(msg: str) -> Optional[str]:
    """Extract a card name from a message containing cardHint HTML."""
    match = re.search(r"<div class='cardHint'[^>]*>(.*?)</div>", msg)
    return match.group(1) if match else None


def extract_all_card_names(msg: str) -> List[str]:
    """Extract all card names from a message."""
    return re.findall(r"<div class='cardHint'[^>]*>(.*?)</div>", msg)


# ---------------------------------------------------------------------------
# Replay Parser
# ---------------------------------------------------------------------------

class ReplayParser:
    """Parses a GEMP replay XML file and extracts game analytics."""

    def __init__(self, xml_content: str):
        self.root = ET.fromstring(xml_content)
        self.messages: List[str] = []
        self.raw_messages: List[str] = []
        self.stats_snapshots: List[Dict] = []
        self._parse()

    def _parse(self):
        for ge in self.root.findall("ge"):
            etype = ge.attrib.get("type", "")
            if etype == "M":
                msg = ge.attrib.get("message", "")
                if msg:
                    self.raw_messages.append(msg)
                    self.messages.append(strip_html(msg))
            elif etype == "GS":
                snapshot = {
                    "lightForceGen": float(ge.attrib.get("lightForceGeneration", "0")),
                    "darkForceGen": float(ge.attrib.get("darkForceGeneration", "0")),
                }
                for pz in ge.findall("playerZones"):
                    name = pz.attrib.get("name", "")
                    snapshot[name] = {
                        "hand": int(pz.attrib.get("HAND", "0")),
                        "reserve": int(pz.attrib.get("RESERVE_DECK", "0")),
                        "lost": int(pz.attrib.get("LOST_PILE", "0")),
                        "force": int(pz.attrib.get("FORCE_PILE", "0")),
                        "used": int(pz.attrib.get("USED_PILE", "0")),
                        "oop": int(pz.attrib.get("OUT_OF_PLAY", "0")),
                    }
                self.stats_snapshots.append(snapshot)

    def get_winner(self) -> Optional[str]:
        for msg in self.messages:
            if "is the winner" in msg:
                return msg.split(" is the winner")[0].replace("~", "").strip()
        return None

    def get_loser(self) -> Optional[str]:
        for msg in self.messages:
            if "lost due to" in msg:
                return msg.split(" lost due to")[0].replace("~", "").strip()
        return None

    def get_win_reason(self) -> str:
        for msg in self.messages:
            if "is the winner due to:" in msg:
                return msg.split("is the winner due to:")[-1].strip()
        return "Unknown"

    def get_starting_locations(self) -> Dict[str, str]:
        """Extract what location each player deployed as their starting location."""
        locations = {}
        for msg in self.raw_messages:
            # Starting locations are deployed from Reserve Deck early, and are horizontal
            if "deploys" in msg and "data-horizontal='true'" in msg and "Reserve Deck" in msg:
                clean = strip_html(msg)
                card_name = extract_card_name(msg)
                if "~The_Chosen_One" in msg or "The_Chosen_One" in msg:
                    locations["light"] = card_name or clean
                elif "~Rando_Cal" in msg or "Rando_Cal" in msg:
                    if "light" not in locations or "dark" not in locations:
                        locations.setdefault("dark", card_name or clean)
            # Also catch location deploys without horizontal flag
            if "from Reserve Deck" in msg and "deploys" in msg:
                card_name = extract_card_name(msg)
                if card_name and (":" in card_name or "Site" in card_name or "System" in card_name):
                    if "~The_Chosen_One" in msg:
                        locations.setdefault("light_first_loc", card_name)
                    elif "~Rando_Cal" in msg:
                        locations.setdefault("dark_first_loc", card_name)
        return locations

    def get_cards_deployed(self, player_substr: str) -> List[str]:
        """Get all cards a player deployed."""
        deployed = []
        for msg in self.raw_messages:
            if player_substr in msg and "deploys" in msg:
                names = extract_all_card_names(msg)
                deployed.extend(names)
        return deployed

    def get_force_drains(self, player_substr: str) -> int:
        """Count force drains initiated by a player."""
        count = 0
        for msg in self.messages:
            if player_substr.replace("~", "") in msg and "force drain" in msg.lower():
                count += 1
        return count

    def get_battles(self) -> List[str]:
        """Get battle descriptions."""
        battles = []
        for msg in self.messages:
            if "battle" in msg.lower() and ("initiates" in msg.lower() or "attacks" in msg.lower()):
                battles.append(msg)
        return battles

    def get_force_losses(self, player_substr: str) -> int:
        """Count total force loss events for a player."""
        count = 0
        clean_name = player_substr.replace("~", "")
        for msg in self.messages:
            if clean_name in msg and ("loses a force" in msg.lower() or "lost pile" in msg.lower()):
                count += 1
        return count

    def get_retrievals(self, player_substr: str) -> List[str]:
        """Get retrieval events for a player."""
        retrievals = []
        clean_name = player_substr.replace("~", "")
        for msg in self.messages:
            if clean_name in msg and "retriev" in msg.lower():
                retrievals.append(msg)
        return retrievals

    def get_cards_placed_out_of_play(self, player_substr: str) -> List[str]:
        """Get cards placed out of play by a player."""
        oop = []
        for msg in self.raw_messages:
            if player_substr in msg and "out of play" in msg.lower():
                names = extract_all_card_names(msg)
                oop.extend(names)
        return oop

    def get_final_stats(self) -> Optional[Dict]:
        """Get the last game stats snapshot."""
        return self.stats_snapshots[-1] if self.stats_snapshots else None

    def get_peak_force_generation(self) -> Dict[str, float]:
        """Get peak force generation for each side."""
        peak = {"light": 0.0, "dark": 0.0}
        for snap in self.stats_snapshots:
            peak["light"] = max(peak["light"], snap.get("lightForceGen", 0))
            peak["dark"] = max(peak["dark"], snap.get("darkForceGen", 0))
        return peak

    def get_game_length_turns(self) -> int:
        """Estimate game length in turns from message count."""
        turn_count = 0
        for msg in self.messages:
            if "turn #" in msg.lower() or "activate phase" in msg.lower():
                turn_count += 1
        return turn_count // 2  # Two players per turn


# ---------------------------------------------------------------------------
# Replay File Reader
# ---------------------------------------------------------------------------

def read_replay_file(filepath: str) -> Optional[str]:
    """Read a replay file (zlib compressed XML)."""
    try:
        with open(filepath, "rb") as f:
            raw = f.read()
        return zlib.decompress(raw).decode("utf-8")
    except Exception as e:
        print(f"  WARNING: Could not read replay {filepath}: {e}")
        return None


# ---------------------------------------------------------------------------
# Tournament Analyzer
# ---------------------------------------------------------------------------

class TournamentAnalyzer:
    """Analyzes a set of tournament games to find win/loss patterns."""

    def __init__(self, tournament_data: Dict[str, Any], replay_base: str):
        self.data = tournament_data
        self.replay_base = replay_base
        self.config = tournament_data.get("config", {})
        self.games = tournament_data.get("games", [])
        self.replays: Dict[str, ReplayParser] = {}  # game_number -> parser

    def load_replays(self):
        """Load all replay files referenced in the tournament."""
        replay_info = self.data.get("replay_files", {})

        for side in ["light", "dark"]:
            player = replay_info.get(f"{side}_player", "")
            new_replays = replay_info.get(f"{side}_new_replays", [])
            player_dir = os.path.join(self.replay_base, player)

            if not os.path.isdir(player_dir):
                print(f"  WARNING: Replay dir not found: {player_dir}")
                continue

            for replay_file in new_replays:
                filepath = os.path.join(player_dir, replay_file)
                content = read_replay_file(filepath)
                if content:
                    parser = ReplayParser(content)
                    # Try to match to a game by winner/loser
                    key = f"{side}_{replay_file}"
                    self.replays[key] = parser

        print(f"  Loaded {len(self.replays)} replay files")

    def load_all_player_replays(self, max_per_player: int = 30):
        """Load the most recent replay files for each player (fallback if no tournament tracking)."""
        for player_id in [self.config.get("light_player", "~The_Chosen_One"),
                          self.config.get("dark_player", "~Rando_Cal")]:
            player_dir = os.path.join(self.replay_base, player_id)
            if not os.path.isdir(player_dir):
                continue

            files = sorted(Path(player_dir).glob("*.xml.gz"),
                           key=lambda p: p.stat().st_mtime, reverse=True)

            for f in files[:max_per_player]:
                content = read_replay_file(str(f))
                if content:
                    parser = ReplayParser(content)
                    key = f"{player_id}_{f.name}"
                    self.replays[key] = parser

        print(f"  Loaded {len(self.replays)} replay files from player directories")

    def analyze(self) -> str:
        """Run full analysis, return markdown report."""
        lines = []
        lines.append("# Bot Tournament Analysis Report")
        lines.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

        # Config
        lines.append("## Tournament Configuration")
        lines.append(f"- Light: **{self.config.get('light_skill', '?')}** — {self.config.get('light_deck', '?')}")
        lines.append(f"- Dark: **{self.config.get('dark_skill', '?')}** — {self.config.get('dark_deck', '?')}")
        lines.append(f"- Format: {self.config.get('format', '?')}")
        lines.append(f"- Games: {self.config.get('num_games', len(self.games))}")
        lines.append("")

        # Overall record
        summary = self.data.get("summary", {})
        lines.append("## Overall Record")
        lines.append(f"- **Light wins: {summary.get('light_wins', '?')}** ({summary.get('light_win_pct', '?')}%)")
        lines.append(f"- **Dark wins: {summary.get('dark_wins', '?')}** ({summary.get('dark_win_pct', '?')}%)")
        lines.append(f"- Errors/DNF: {summary.get('errors', 0)}")
        lines.append("")

        # Per-game results table
        lines.append("## Game-by-Game Results")
        lines.append("| Game | Winner | Win Reason | Time | Decisions |")
        lines.append("|------|--------|-----------|------|-----------|")
        for g in self.games:
            gn = g.get("game_number", "?")
            ws = g.get("winning_side", "?")
            wr = g.get("win_reason", "?")
            t = g.get("elapsed_seconds", "?")
            d = g.get("total_decisions", "?")
            lines.append(f"| {gn} | {ws} | {wr} | {t}s | {d} |")
        lines.append("")

        # Win reason breakdown
        lines.append("## Win Reason Breakdown")
        reason_counter = Counter()
        reason_by_side = {"Light": Counter(), "Dark": Counter()}
        for g in self.games:
            reason = g.get("win_reason", "Unknown")
            side = g.get("winning_side", "Unknown")
            reason_counter[reason] += 1
            reason_by_side[side][reason] += 1
        for reason, count in reason_counter.most_common():
            lines.append(f"- **{reason}**: {count} games")
        lines.append("")

        # Replay-based deep analysis
        if self.replays:
            lines.append("## Deep Replay Analysis")
            lines.append("")
            lines.extend(self._analyze_replays())

        # Loss pattern analysis
        lines.append("## Loss Pattern Analysis")
        lines.append("")
        lines.extend(self._analyze_loss_patterns())

        # Key events per game
        lines.append("## Key Events Per Game")
        for g in self.games:
            gn = g.get("game_number", "?")
            ws = g.get("winning_side", "?")
            events = g.get("key_events", [])
            lines.append(f"\n### Game {gn} — {ws} wins")
            if events:
                # Show first 10 and last 10 key events
                show_events = events[:10]
                if len(events) > 20:
                    show_events.append(f"... ({len(events) - 20} more events) ...")
                    show_events.extend(events[-10:])
                elif len(events) > 10:
                    show_events.extend(events[10:])
                for evt in show_events:
                    lines.append(f"  - {evt}")
            else:
                lines.append("  (No key events captured — run with replay tracking)")
        lines.append("")

        # Recommendations
        lines.append("## Strategy Recommendations")
        lines.append("")
        lines.extend(self._generate_recommendations())

        return "\n".join(lines)

    def _analyze_replays(self) -> List[str]:
        """Deep analysis from replay file data."""
        lines = []

        # Aggregate stats across replays
        light_deploys = Counter()
        dark_deploys = Counter()
        light_starting_locs = Counter()
        dark_starting_locs = Counter()
        light_force_drains = []
        dark_force_drains = []
        light_oop_cards = Counter()
        dark_oop_cards = Counter()
        peak_force_gen = {"light": [], "dark": []}
        light_retrievals = []
        dark_retrievals = []

        for key, parser in self.replays.items():
            # Deployments
            for card in parser.get_cards_deployed("The_Chosen_One"):
                light_deploys[card] += 1
            for card in parser.get_cards_deployed("Rando_Cal"):
                dark_deploys[card] += 1

            # Starting locations
            locs = parser.get_starting_locations()
            if "light" in locs:
                light_starting_locs[locs["light"]] += 1
            elif "light_first_loc" in locs:
                light_starting_locs[locs["light_first_loc"]] += 1
            if "dark" in locs:
                dark_starting_locs[locs["dark"]] += 1
            elif "dark_first_loc" in locs:
                dark_starting_locs[locs["dark_first_loc"]] += 1

            # Force drains
            light_force_drains.append(parser.get_force_drains("The_Chosen_One"))
            dark_force_drains.append(parser.get_force_drains("Rando_Cal"))

            # Out of play
            for card in parser.get_cards_placed_out_of_play("The_Chosen_One"):
                light_oop_cards[card] += 1
            for card in parser.get_cards_placed_out_of_play("Rando_Cal"):
                dark_oop_cards[card] += 1

            # Peak force gen
            peaks = parser.get_peak_force_generation()
            peak_force_gen["light"].append(peaks["light"])
            peak_force_gen["dark"].append(peaks["dark"])

            # Retrievals
            light_retrievals.append(len(parser.get_retrievals("The_Chosen_One")))
            dark_retrievals.append(len(parser.get_retrievals("Rando_Cal")))

        # Report starting locations
        lines.append("### Starting Locations")
        lines.append("**Light (The Chosen One):**")
        for loc, count in light_starting_locs.most_common(5):
            lines.append(f"  - {loc}: {count} games")
        lines.append("**Dark (Rando Cal):**")
        for loc, count in dark_starting_locs.most_common(5):
            lines.append(f"  - {loc}: {count} games")
        lines.append("")

        # Most deployed cards
        lines.append("### Most Deployed Cards")
        lines.append("**Light (The Chosen One) — Top 15:**")
        for card, count in light_deploys.most_common(15):
            lines.append(f"  - {card}: {count}")
        lines.append("**Dark (Rando Cal) — Top 15:**")
        for card, count in dark_deploys.most_common(15):
            lines.append(f"  - {card}: {count}")
        lines.append("")

        # Force generation
        if peak_force_gen["light"]:
            avg_l = sum(peak_force_gen["light"]) / len(peak_force_gen["light"])
            avg_d = sum(peak_force_gen["dark"]) / len(peak_force_gen["dark"])
            max_l = max(peak_force_gen["light"])
            max_d = max(peak_force_gen["dark"])
            lines.append("### Force Generation")
            lines.append(f"  - Light avg peak: {avg_l:.1f} (max: {max_l:.0f})")
            lines.append(f"  - Dark avg peak: {avg_d:.1f} (max: {max_d:.0f})")
            lines.append("")

        # Force drains
        if light_force_drains:
            lines.append("### Force Drains")
            lines.append(f"  - Light avg: {sum(light_force_drains)/len(light_force_drains):.1f} per game")
            lines.append(f"  - Dark avg: {sum(dark_force_drains)/len(dark_force_drains):.1f} per game")
            lines.append("")

        # Retrievals
        if light_retrievals:
            lines.append("### Force Retrievals")
            lines.append(f"  - Light avg: {sum(light_retrievals)/len(light_retrievals):.1f} per game")
            lines.append(f"  - Dark avg: {sum(dark_retrievals)/len(dark_retrievals):.1f} per game")
            lines.append("")

        # Out of play tracking
        if light_oop_cards or dark_oop_cards:
            lines.append("### Cards Placed Out of Play")
            if light_oop_cards:
                lines.append("**Light:**")
                for card, count in light_oop_cards.most_common(10):
                    lines.append(f"  - {card}: {count}")
            if dark_oop_cards:
                lines.append("**Dark:**")
                for card, count in dark_oop_cards.most_common(10):
                    lines.append(f"  - {card}: {count}")
            lines.append("")

        return lines

    def _analyze_loss_patterns(self) -> List[str]:
        """Identify common loss patterns from game key events."""
        lines = []

        light_loss_games = [g for g in self.games if g.get("winning_side") == "Dark"]
        dark_loss_games = [g for g in self.games if g.get("winning_side") == "Light"]

        if light_loss_games:
            lines.append(f"### Light Side Losses ({len(light_loss_games)} games)")
            lines.append("")

            # Analyze key events in losses
            loss_patterns = Counter()
            for g in light_loss_games:
                events = g.get("key_events", [])
                events_text = " ".join(events).lower()

                # Check various patterns
                if "concede" in events_text:
                    loss_patterns["Conceded (ran out of cards)"] += 1
                if "battle damage" in events_text:
                    battle_damage_count = events_text.count("battle damage")
                    if battle_damage_count > 5:
                        loss_patterns["Heavy battle damage"] += 1
                if "force drain" in events_text:
                    # Count opponent force drains
                    drain_count = sum(1 for e in events if "rando" in e.lower() and "force drain" in e.lower())
                    if drain_count > 3:
                        loss_patterns["Overwhelmed by opponent force drains"] += 1
                if not any("deploys" in e.lower() and "chosen" in e.lower() for e in events[:30]):
                    loss_patterns["Slow start (few early deployments)"] += 1

                # Check win reason
                wr = g.get("win_reason", "")
                loss_patterns[f"Loss reason: {wr}"] += 1

            for pattern, count in loss_patterns.most_common():
                lines.append(f"  - {pattern}: {count} games")
            lines.append("")

        if dark_loss_games:
            lines.append(f"### Dark Side Losses ({len(dark_loss_games)} games)")
            lines.append("")

            loss_patterns = Counter()
            for g in dark_loss_games:
                events = g.get("key_events", [])
                events_text = " ".join(events).lower()

                if "concede" in events_text:
                    loss_patterns["Conceded (ran out of cards)"] += 1
                if "battle damage" in events_text:
                    battle_damage_count = events_text.count("battle damage")
                    if battle_damage_count > 5:
                        loss_patterns["Heavy battle damage"] += 1

                wr = g.get("win_reason", "")
                loss_patterns[f"Loss reason: {wr}"] += 1

            for pattern, count in loss_patterns.most_common():
                lines.append(f"  - {pattern}: {count} games")
            lines.append("")

        return lines

    def _generate_recommendations(self) -> List[str]:
        """Generate strategy recommendations based on analysis."""
        lines = []
        summary = self.data.get("summary", {})
        light_wins = summary.get("light_wins", 0)
        dark_wins = summary.get("dark_wins", 0)

        if light_wins > dark_wins:
            weaker = "Dark (Rando Cal)"
            stronger = "Light (The Chosen One)"
            weaker_pct = summary.get("dark_win_pct", 0)
        elif dark_wins > light_wins:
            weaker = "Light (The Chosen One)"
            stronger = "Dark (Rando Cal)"
            weaker_pct = summary.get("light_win_pct", 0)
        else:
            lines.append("The matchup is currently even. Review individual game replays for specific improvement areas.")
            return lines

        lines.append(f"**{weaker}** is the weaker side, winning only {weaker_pct}% of games.")
        lines.append(f"**{stronger}** is dominant in this matchup.\n")
        lines.append("### Areas to investigate for improving " + weaker + ":")
        lines.append("")
        lines.append("1. **Starting Location Choice** — Is the bot picking the optimal starting location?")
        lines.append("2. **Force Generation** — Is the bot building force generation quickly enough?")
        lines.append("3. **Card Deployment Priority** — Are key cards being deployed at the right time?")
        lines.append("4. **Battle Decisions** — Is the bot engaging in battles it can win?")
        lines.append("5. **Force Drain Strategy** — Is the bot draining when it has board advantage?")
        lines.append("6. **Resource Management** — Is the bot placing cards out of play too early?")
        lines.append("7. **Retrieval Utilization** — Is the bot using retrieval effects effectively?")
        lines.append("")
        lines.append("Review the Key Events section above for each lost game to identify specific decision points where the bot made suboptimal choices.")

        return lines


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def find_latest_tournament(results_dir: str) -> Optional[str]:
    """Find the most recent tournament results file."""
    if not os.path.isdir(results_dir):
        return None
    files = sorted(Path(results_dir).glob("tournament_*.json"), key=lambda p: p.stat().st_mtime, reverse=True)
    return str(files[0]) if files else None


def main():
    parser = argparse.ArgumentParser(description="Analyze bot-vs-bot tournament results")
    parser.add_argument("results_file", nargs="?", help="Path to tournament results JSON")
    parser.add_argument("--latest", action="store_true", help="Use the most recent tournament results")
    parser.add_argument("--replay-dir", default="", help="Path to replays/ folder")
    parser.add_argument("--output", default="", help="Output markdown file path")
    parser.add_argument("--load-all-replays", action="store_true",
                        help="Load all recent replays (not just tournament-tracked ones)")
    parser.add_argument("--max-replays", type=int, default=30,
                        help="Max replays to load per player when using --load-all-replays")
    args = parser.parse_args()

    # Find results file
    results_file = args.results_file
    if args.latest or not results_file:
        results_dir = os.path.join(os.path.dirname(__file__), "tournament_results")
        results_file = find_latest_tournament(results_dir)
        if not results_file:
            print("ERROR: No tournament results found. Run run_bot_tournament.py first.")
            sys.exit(1)

    print(f"Loading tournament results: {results_file}")
    with open(results_file) as f:
        tournament_data = json.load(f)

    # Find replay directory
    replay_base = args.replay_dir
    if not replay_base:
        candidates = [
            os.path.join(os.path.dirname(os.path.dirname(__file__)), "replays"),
            "/Users/steve/gemp-swccg-public/replays",
            os.path.expanduser("~/gemp-swccg-public/replays"),
        ]
        for c in candidates:
            if os.path.isdir(c):
                replay_base = c
                break

    if not replay_base or not os.path.isdir(replay_base):
        print(f"WARNING: Replay directory not found. Analysis will be limited.")
        replay_base = "/tmp/no-replays"

    print(f"Replay directory: {replay_base}")

    # Run analysis
    analyzer = TournamentAnalyzer(tournament_data, replay_base)

    if args.load_all_replays:
        analyzer.load_all_player_replays(args.max_replays)
    else:
        analyzer.load_replays()
        # Fallback: if no tournament-tracked replays, load recent ones
        if not analyzer.replays:
            print("  No tournament-tracked replays found, loading recent player replays...")
            analyzer.load_all_player_replays(args.max_replays)

    report = analyzer.analyze()

    # Save report
    output_path = args.output
    if not output_path:
        results_dir = os.path.join(os.path.dirname(__file__), "tournament_results")
        os.makedirs(results_dir, exist_ok=True)
        base_name = Path(results_file).stem
        output_path = os.path.join(results_dir, f"{base_name}_analysis.md")

    with open(output_path, "w") as f:
        f.write(report)

    print(f"\nAnalysis report saved to: {output_path}")
    print(f"\nQuick summary:")
    summary = tournament_data.get("summary", {})
    print(f"  Light: {summary.get('light_wins', '?')} wins ({summary.get('light_win_pct', '?')}%)")
    print(f"  Dark:  {summary.get('dark_wins', '?')} wins ({summary.get('dark_win_pct', '?')}%)")


if __name__ == "__main__":
    main()
