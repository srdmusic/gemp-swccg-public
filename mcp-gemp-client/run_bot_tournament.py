#!/usr/bin/env python3
"""
run_bot_tournament.py — Stage 1 of the offline curator-review pipeline.

Runs N Rando-vs-ChosenOne bot games and records each game's result + replay path.
Each game is created via the admin /botgame endpoint; the server's ITERATIVE AI
drive plays it to completion (no live spectator needed — that watcher was flaky).
We detect completion by watching the host replay directory for a new file, then
read the winner from the replay.

Output: tournament_results/tournament_<timestamp>.json

Usage:
    cd mcp-gemp-client
    python3 run_bot_tournament.py --games 10
"""
from __future__ import annotations
import argparse
import asyncio
import json
import re
import sys
import time
import zlib
import html
from pathlib import Path
from typing import Optional, Set

import httpx

sys.path.insert(0, str(Path(__file__).parent))
from watch_bot_game import BotGameWatcher  # reuse login / create_bot_game

REPLAY_ROOT = Path('/Users/steve/gemp-swccg-public/replays')
RESULTS_DIR = Path(__file__).parent / 'tournament_results'

# Decks owned by test1 (light, dark). Add more pairs as desired.
DECK_PAIRS = [
    ('LUKE SAGA TATOOINE', 'DARK DEAL'),
]


def snapshot_replays() -> Set[str]:
    out = set()
    if REPLAY_ROOT.exists():
        for player_dir in REPLAY_ROOT.iterdir():
            if player_dir.is_dir():
                for f in player_dir.iterdir():
                    if f.suffix == '.gz':
                        out.add(str(f.resolve()))
    return out


def detect_new_replays(before: Set[str]) -> list[Path]:
    after = snapshot_replays()
    return [Path(p) for p in sorted(after - before, key=lambda x: Path(x).stat().st_mtime)]


def strip_tags(s: str) -> str:
    return re.sub(r'<[^>]+>', '', s)


def read_replay_result(path: Path) -> dict:
    """Decompress a replay and pull winner / loser / reason + a size hint."""
    try:
        data = path.read_bytes()
        try:
            xml = zlib.decompress(data, 16 + zlib.MAX_WBITS).decode('utf-8', 'replace')
        except zlib.error:
            xml = zlib.decompress(data).decode('utf-8', 'replace')
    except Exception as e:
        return {'error': f'decompress failed: {e}'}

    events = re.findall(r'<ge\s', xml)
    turns = re.findall(r'Start of [^<]*turn', xml)
    winner = loser = reason = ''
    for m in re.finditer(r'message="([^"]*)"', xml):
        msg = html.unescape(m.group(1))
        low = msg.lower()
        if 'is the winner due to:' in msg:
            parts = msg.split(' is the winner due to:')
            winner = strip_tags(parts[0]).replace('~', '').strip()
            reason = parts[1].strip() if len(parts) > 1 else ''
        elif 'lost due to:' in low:
            loser = strip_tags(msg.split(' lost due to:')[0]).replace('~', '').strip()
    return {
        'events': len(events),
        'turn_count': len(turns),
        'winner': winner,
        'loser': loser,
        'win_reason': reason,
        'complete': bool(winner) and len(turns) >= 2,
    }


async def create_game_long(watcher: BotGameWatcher, fmt: str, light_skill: str, light_deck: str,
                           dark_skill: str, dark_deck: str, deck_owner: str,
                           timeout: int) -> Optional[str]:
    """Create a bot game with a LONG timeout. The server's iterative AI drive plays
    the whole game synchronously inside this request, so it can block for minutes;
    watch_bot_game's built-in create uses a 30s timeout which crashes. We tolerate
    a ReadTimeout here (the game still runs server-side and produces a replay)."""
    data = {'format': fmt, 'lightSkill': light_skill, 'lightDeck': light_deck,
            'darkSkill': dark_skill, 'darkDeck': dark_deck, 'deckOwner': deck_owner}
    try:
        async with httpx.AsyncClient(timeout=timeout) as client:
            resp = await client.post(
                f'{watcher.base_url}/gemp-swccg-server/admin/botgame',
                data=data, headers=watcher._headers('hall.html'))
        body = resp.text.strip()
        if body.startswith('OK gameId='):
            return body.replace('OK gameId=', '').strip()
        print(f'   unexpected create response: {body[:200]}')
        return None
    except httpx.ReadTimeout:
        # Game is running server-side; we'll detect it via the replay dir.
        print('   create timed out (game running server-side) — watching for replay')
        return 'TIMEOUT'


async def run_one(watcher: BotGameWatcher, label: str, light_skill: str, light_deck: str,
                  dark_skill: str, dark_deck: str, deck_owner: str, fmt: str,
                  finish_timeout: int) -> dict:
    print(f'\n[{label}] {light_skill}({light_deck}) vs {dark_skill}({dark_deck})')
    before = snapshot_replays()
    game_id = await create_game_long(watcher, fmt, light_skill, light_deck,
                                     dark_skill, dark_deck, deck_owner, finish_timeout)
    if not game_id:
        return {'label': label, 'error': 'create failed'}

    # Wait for the game to finish: a new replay file appears AND parses as complete.
    start = time.time()
    result_replay = None
    while time.time() - start < finish_timeout:
        new = detect_new_replays(before)
        for rp in new:
            res = read_replay_result(rp)
            if res.get('complete'):
                result_replay = (rp, res)
                break
        if result_replay:
            break
        await asyncio.sleep(3)

    elapsed = time.time() - start
    if not result_replay:
        # Grab whatever replay appeared (even if incomplete) for diagnostics.
        new = detect_new_replays(before)
        if new:
            rp = new[-1]
            res = read_replay_result(rp)
            print(f'[{label}]   timed out after {elapsed:.0f}s — replay {rp.name} '
                  f'events={res.get("events")} turns={res.get("turn_count")} (INCOMPLETE)')
            return {'label': label, 'game_id': game_id, 'replay': str(rp),
                    'elapsed_s': round(elapsed, 1), 'status': 'incomplete', **res}
        print(f'[{label}]   timed out after {elapsed:.0f}s — no replay')
        return {'label': label, 'game_id': game_id, 'elapsed_s': round(elapsed, 1),
                'status': 'no_replay'}

    rp, res = result_replay
    print(f'[{label}]   done in {elapsed:.0f}s — winner={res["winner"]!r} '
          f'reason={res["win_reason"]!r} turns={res["turn_count"]} replay={rp.name}')
    return {'label': label, 'game_id': game_id, 'replay': str(rp),
            'light_skill': light_skill, 'light_deck': light_deck,
            'dark_skill': dark_skill, 'dark_deck': dark_deck,
            'elapsed_s': round(elapsed, 1), 'status': 'completed', **res}


async def amain():
    ap = argparse.ArgumentParser()
    ap.add_argument('--games', type=int, default=10)
    ap.add_argument('--base-url', default='http://localhost:17001')
    ap.add_argument('--user', default='asdf')
    ap.add_argument('--password', default='asdf')
    ap.add_argument('--deck-owner', default='test1')
    ap.add_argument('--light-deck', default=None,
                    help='override light deck name (use with --dark-deck)')
    ap.add_argument('--dark-deck', default=None,
                    help='override dark deck name (use with --light-deck)')
    ap.add_argument('--format', default='open')
    ap.add_argument('--light-skill', default='RANDO')
    ap.add_argument('--dark-skill', default='CHOSENONE')
    ap.add_argument('--finish-timeout', type=int, default=600,
                    help='max seconds to wait for one game to finish')
    ap.add_argument('--pause', type=float, default=4.0, help='seconds between games')
    args = ap.parse_args()

    watcher = BotGameWatcher(args.base_url)
    watcher.username = args.user
    if not await watcher.login(args.user, args.password):
        print('login failed'); return
    await watcher.ensure_server_running()
    await watcher.enable_ai_tables()

    # CLI override of the deck pair (else fall back to hardcoded DECK_PAIRS).
    deck_pairs = DECK_PAIRS
    if args.light_deck and args.dark_deck:
        deck_pairs = [(args.light_deck, args.dark_deck)]
    print(f'deck pairs (light, dark): {deck_pairs}  owner={args.deck_owner}')

    results = []
    for i in range(args.games):
        # Alternate which skill plays Light, so each side is tested both ways.
        light_deck, dark_deck = deck_pairs[i % len(deck_pairs)]
        if i % 2 == 0:
            ls, ld, ds, dd = args.light_skill, light_deck, args.dark_skill, dark_deck
        else:
            ls, ld, ds, dd = args.dark_skill, light_deck, args.light_skill, dark_deck
        label = f'G{i+1}/{args.games}'
        res = await run_one(watcher, label, ls, ld, ds, dd,
                            args.deck_owner, args.format, args.finish_timeout)
        results.append(res)
        await asyncio.sleep(args.pause)

    # Summary
    completed = [r for r in results if r.get('status') == 'completed']
    wins = {}
    for r in completed:
        wins[r['winner']] = wins.get(r['winner'], 0) + 1
    summary = {
        'games_requested': args.games,
        'completed': len(completed),
        'incomplete': len([r for r in results if r.get('status') != 'completed']),
        'win_counts': wins,
    }

    RESULTS_DIR.mkdir(exist_ok=True)
    ts = time.strftime('%Y%m%d_%H%M%S')
    out = RESULTS_DIR / f'tournament_{ts}.json'
    out.write_text(json.dumps({'summary': summary, 'results': results}, indent=2))
    print(f'\n===== TOURNAMENT DONE =====')
    print(f'completed {len(completed)}/{args.games}  wins={wins}')
    print(f'results -> {out}')
    print(f'next: python3 curator_review.py {out}')


def main():
    asyncio.run(amain())


if __name__ == '__main__':
    main()
