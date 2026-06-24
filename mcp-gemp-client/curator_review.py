#!/usr/bin/env python3
"""
curator_review.py — Stages 2+3 of the offline curator-review pipeline.

Takes a tournament_results JSON (from run_bot_tournament.py), and for each
completed game:
  1. Summarizes the replay into a compact strategic timeline (deploys, drains,
     battles, forfeits, result).
  2. Sends that summary to the local Q8 reasoning model (DeepSeek-R1 70B via
     Ollama) and asks WHY the game was won/lost and what 1-3 concrete logic
     changes would help the loser play better next time.
Runs OFFLINE on finished games — no timeout pressure, full reasoning depth.

Output: tournament_results/review_<tournament>.json  (+ a readable .md digest)

Usage:
    cd mcp-gemp-client
    python3 curator_review.py tournament_results/tournament_<ts>.json
    # then K-2 + council synthesize proposed V-tag changes from the digest.
"""
from __future__ import annotations
import argparse
import html
import json
import re
import sys
import time
import zlib
from pathlib import Path

import httpx

OLLAMA_URL = 'http://127.0.0.1:11434'
MODEL = 'deepseek-r1:70b-llama-distill-q8_0'
CONSULT_TIMEOUT = 900  # offline — let it think


def decompress(path: Path) -> str:
    data = path.read_bytes()
    try:
        return zlib.decompress(data, 16 + zlib.MAX_WBITS).decode('utf-8', 'replace')
    except zlib.error:
        return zlib.decompress(data).decode('utf-8', 'replace')


def strip_tags(s: str) -> str:
    return re.sub(r'<[^>]+>', '', s).strip()


def summarize_replay(path: Path, max_lines: int = 120) -> str:
    """Compact strategic timeline: the messages that actually decide games."""
    xml = decompress(path)
    msgs = [html.unescape(m.group(1)) for m in re.finditer(r'message="([^"]*)"', xml)]
    keep_kw = (
        'deploys', 'Force drain', 'force drain', 'retrieves', 'initiates battle',
        'is the winner', 'lost due to', 'forfeits', 'choose to forfeit',
        'loses a Force', 'Start of', 'attrition', 'battle damage', 'is \'hit\'',
        'places out of play', 'activates', 're-circulates',
    )
    out = []
    turn = 0
    for raw in msgs:
        c = strip_tags(raw)
        if not c:
            continue
        if c.startswith('Start of'):
            turn += 1
            out.append(f'--- {c} ---')
            continue
        if any(k in c for k in keep_kw):
            out.append(c)
    # Trim: keep first 30 (setup) + last (max_lines-30) (endgame, where games are decided)
    if len(out) > max_lines:
        out = out[:30] + ['... (mid-game trimmed) ...'] + out[-(max_lines - 31):]
    return '\n'.join(out)


def ask_curator(game_label: str, light_skill: str, dark_skill: str,
                winner: str, loser: str, win_reason: str, summary: str) -> str:
    system = (
        "You are a world-class Star Wars CCG (SWCCG) strategist and AI-bot coach. "
        "You are reviewing a finished game between two AI bots to improve their logic. "
        "Be concrete and actionable: name specific decisions, turns, and SWCCG mechanics."
    )
    user = f"""Game: {game_label}  ({light_skill} = Light, {dark_skill} = Dark)
Winner: {winner}   Loser: {loser}   Win reason: {win_reason}

Strategic timeline:
{summary}

Analyze this game:
1. WHY did {winner} win and {loser} lose? (The 1-2 pivotal moments or patterns.)
2. What did {loser} do WRONG that a stronger player would have done differently?
3. Propose 1-3 SPECIFIC, implementable logic changes that would help {loser}'s bot
   play better next time (e.g. "forfeit the cheaper character first", "don't deploy
   solo into a contested site", "drain at the 2-icon site before the 1-icon site").
Keep it tight. End with a 'CHANGES:' list of the concrete rule proposals."""

    body = {
        'model': MODEL,
        'messages': [{'role': 'system', 'content': system},
                     {'role': 'user', 'content': user}],
        'stream': False,
        'options': {'temperature': 0.4, 'num_predict': 3000},
    }
    try:
        with httpx.Client(timeout=CONSULT_TIMEOUT) as client:
            r = client.post(f'{OLLAMA_URL}/v1/chat/completions', json=body)
        r.raise_for_status()
        content = r.json()['choices'][0]['message']['content']
        # strip <think> blocks
        return re.sub(r'<think>.*?</think>', '', content, flags=re.DOTALL).strip()
    except Exception as e:
        return f'(curator review failed: {e})'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('tournament_json', help='tournament_results/tournament_<ts>.json')
    ap.add_argument('--limit', type=int, default=0, help='review only first N games (0 = all)')
    args = ap.parse_args()

    tj = Path(args.tournament_json)
    data = json.loads(tj.read_text())
    games = [g for g in data['results'] if g.get('status') == 'completed' and g.get('replay')]
    if args.limit:
        games = games[:args.limit]

    print(f'Reviewing {len(games)} completed games with {MODEL} (offline, slow)...')
    reviews = []
    for i, g in enumerate(games):
        label = g['label']
        print(f'\n[{i+1}/{len(games)}] {label} — summarizing + asking curator...')
        try:
            summary = summarize_replay(Path(g['replay']))
        except Exception as e:
            print(f'   summarize failed: {e}'); continue
        t0 = time.time()
        analysis = ask_curator(label, g.get('light_skill', '?'), g.get('dark_skill', '?'),
                               g.get('winner', '?'), g.get('loser', '?'),
                               g.get('win_reason', '?'), summary)
        print(f'   curator answered in {time.time()-t0:.0f}s')
        reviews.append({'label': label, 'winner': g.get('winner'), 'loser': g.get('loser'),
                        'win_reason': g.get('win_reason'), 'analysis': analysis})

    # Write JSON + a readable markdown digest for K-2 + council synthesis.
    stem = tj.stem.replace('tournament_', 'review_')
    out_json = tj.parent / f'{stem}.json'
    out_md = tj.parent / f'{stem}.md'
    out_json.write_text(json.dumps({'source': str(tj), 'reviews': reviews}, indent=2))

    md = [f'# Curator Review — {tj.name}\n',
          f'Model: {MODEL}  |  Games reviewed: {len(reviews)}\n',
          f'Win counts: {data["summary"].get("win_counts")}\n']
    for r in reviews:
        md.append(f'\n## {r["label"]} — winner {r["winner"]} / loser {r["loser"]}')
        md.append(f'_{r["win_reason"]}_\n')
        md.append(r['analysis'])
    out_md.write_text('\n'.join(md))

    print(f'\n===== REVIEW DONE =====')
    print(f'json  -> {out_json}')
    print(f'digest -> {out_md}')
    print('next: K-2 reads the digest, runs the council, and proposes V-tag changes.')


if __name__ == '__main__':
    main()
