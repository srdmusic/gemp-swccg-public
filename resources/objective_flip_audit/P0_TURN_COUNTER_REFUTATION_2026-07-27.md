# P0 Turn-Counter Defect: REFUTED (2026-07-27)

Verdict: NOT A BUG. Closed with no code change.

## The claim (CODEX_TO_K2_OBJECTIVE_BEHAVIOR_TAKEOVER_2026-07-26.md §9 P0)

"DecisionContext.getTurnNumber() reported 1 across turns two through seven in the Shield
replay" (93r5wrrnbo3q91j0), 37 log hits; suspected source
gameState.getPlayersLatestTurnNumber(playerId); V102 therefore capped shields at 2/game.

## The evidence (logs/2026-07/app-07-26-2026-1.log.gz, UTC timestamps)

The Shield replay game is the 20:43:13 NEW-GAME session, ending 21:05:20. The replay file
mtime 14:05 PDT equals 21:05 UTC. Findings:

- rawTurn (trackGameState, same getPlayersLatestTurnNumber(playerId) call) advanced
  0(x6) 1(x118) 2(x125) 3(x214) 4(x237) 5(x184) 6(x119) 7(x21) across the game window.
  The counter was never stuck.
- All 37 "V102 K&D ACTIVATION CAP: turn 1 count 2" lines fall between 20:44:00 and
  20:45:38. The first rawTurn=2 sighting is 20:45:39,793. Every cap hit occurred during
  REAL turn 1. That is designed V102 behavior: 2 activations on turn 1, then hold.
- On turns 2-7 there are ZERO V102 cap lines, zero pacing lines, and zero
  recordKnDActivation lines in the game window. K&D was held by a silent rule, not a cap.

Cross-checks: four other games that day (05:06, 05:39, 15:07, 15:45, 21:26 UTC) all show
turn-advancing activation lines (e.g. "ACTIVATION turn 2 -> count 1", "turn 3 -> count 4",
"turn 4 -> count 4"). Engine flow verified in source: BetweenTurnsProcess ->
GameState.startPlayerTurn -> incrementAndGetCurrentTurnNumber, keyed per side; snapshot
copy preserves both counters (GameState.java:259-260).

## The real mechanism behind "2 shields all game"

ShieldPolicy.stackedPileParent's V112 3RD SLOT HOLD (-3000 ORDERING, "reserve the third
shield for Battle Order") fires whenever shieldsOnTable==2 and Rando does not occupy both
theaters. That branch emits NO warn log, which is why the log goes silent after turn 1.
The reserved slot waits for Battle Order, and the Battle Order gate never opened because
it inspects Rando's own theater coverage instead of the draining opponent's (Hoth live
regression #1, HOTH_REPAIR_DESIGN_2026-07-27.txt). The observable "capped at 2 for the
entire game" is fully explained by V112 + the broken Battle Order gate. That repair is
queued as the Shield adjacent-repair batch (takeover §9 Priority 2), including re-keying
the third-slot reserve so it cannot block Battle Order itself.

## Consequences

- No AI or engine change made. DecisionContext.java:97 is correct as written.
- Knowledge And Defense per-turn cap already resets on later turns (proven by the other
  games' turn-2+ activation lines).
- Codex's Battle Order repair no longer has a turn-number prerequisite. Proceed directly.
- Takeover §9 Priority 0 success criteria are moot: the "wrong turn value" cannot be
  proven because it does not occur.
