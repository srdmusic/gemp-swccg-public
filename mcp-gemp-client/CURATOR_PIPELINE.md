# Offline Curator-Review Pipeline

Rando and Chosen One play each other; the Q8 curator reviews each finished game
offline (no live-play blocking); K-2 + the council turn the reviews into Rando /
Chosen One logic changes. This sidesteps the live-consult speed wall — the 70B
model reasons at its own pace on completed games.

## Why offline
A 70B reasoning model is too slow to sit inside the live game decision loop
(each consult = minutes; it stalls the game and crashes the spectator). Reviewing
FINISHED replays removes all timeout pressure and gives full reasoning depth.

## Stages
1. **Tournament** — `run_bot_tournament.py` runs N Rando-vs-ChosenOne games
   (alternating sides) and records each result + replay path. Games self-complete
   via the server's iterative AI drive; no live spectator needed.
2. **Curator review** — `curator_review.py` summarizes each replay into a compact
   strategic timeline and asks the Q8 model: why won/lost + 1-3 concrete logic
   changes for the loser. Writes a JSON + a readable `.md` digest.
3. **Synthesis** — K-2 reads the digest, runs the council (localhost:8000) for
   a second opinion, and proposes V-tag changes to Rando / Chosen One.

## Run it
```bash
cd /Users/steve/gemp-swccg-public/mcp-gemp-client

# Stage 1 — run 10 games (CPU only, ~minutes; no GPU/model). HEAT: moderate.
python3 run_bot_tournament.py --games 10
#   -> tournament_results/tournament_<ts>.json

# Stage 2 — curator reviews each game (loads the 74GB Q8 model). HEAT: high.
#   Pre-warm optional; each review ~1-5 min. Unload after with keep_alive=0.
python3 curator_review.py tournament_results/tournament_<ts>.json
#   -> tournament_results/review_<ts>.md   (the digest K-2 reads)

# Stage 3 — K-2 + council synthesize changes from review_<ts>.md
```

## Heat note
Stage 1 is CPU-only (cool-ish). Stage 2 loads the Q8 model (hot). To free RAM/GPU
after stage 2:
```bash
curl -s http://127.0.0.1:11434/api/generate \
  -d '{"model":"deepseek-r1:70b-llama-distill-q8_0","keep_alive":0}' >/dev/null
```

## Notes
- Decks: `DECK_PAIRS` in run_bot_tournament.py (test1 owns LUKE SAGA TATOOINE /
  DARK DEAL). Add pairs to widen coverage.
- A game counts as "completed" only if the replay has a winner AND >=2 turns
  (filters out early stalls).
- Replays: `/Users/steve/gemp-swccg-public/replays/<playerId>/<id>.xml.gz` (zlib).
