# Decision-Fixture Capture Harness (BATCH 0 prototype)

Captures every AI `decide()` call from GEMP server logs as JSONL fixtures, so a shadow
implementation can later be compared decision-for-decision (fixture parity gate).

## Usage
	•	Extract: `python3 extract_fixtures.py logs/gemp-swccg.log logs/2026-07/app-*.log.gz -o baseline.jsonl`
	•	Replay context (optional): `--replay replays/asdf/<id>.xml.gz` (raw zlib XML, despite the .gz name)
	•	Compare: `python3 compare_fixtures.py baseline.jsonl shadow.jsonl --tolerance 0.01` (exit 0 parity, 1 divergent, 2 error)

## Record schema (source="log")
`seq`, `game` (room id or `untracked-N`), `bot` (RandoCalAi / TheChosenOneAi), `ts`,
`decisionType`, `phase`, `decisionText` (trunc 200), `top5` [[actionId, score]...] from V191 TOPN
(actionId may be "" = pass), `fallback` (V191 fallback-heuristic pick, when no top-5 exists),
`bestAction`/`bestScore`, `reasoning` (trunc 500), `evaluatorDecision`/`evaluatorScore`,
`chosen` (decide() result), `chosenScore`, `vetoes`/`vetoCount` (FORMATION SAFETY / HARD VETO lines),
`clamps` (SAFETY CLAMP lines), `unterminated` (decide() never resolved: crash/hang tail).
Records with source="replay" carry the engine-side D elements (decisionType/text/actionIds/actionTexts).

## Known limitations
	•	Log-derived scores are POST-MERGE only; per-rule contributions need the batch-2 trace hooks.
	•	V191 TOPN logs at most 5 actions; full candidate sets are not recoverable from today's logs.
	•	Replays record only the HUMAN client's decisions (participantId=asdf); Rando's server-side decide() calls never appear as `ge type="D"` elements, so replays are context/cross-check only.
	•	Game id comes from "Welcome to room: Game..." TRACE lines; concurrently running games share one log with no per-line game tag, so the latest room wins (heuristic). Untagged segments get `untracked-N`.
	•	Evaluator/strategy lines carry no bot name; they attach to the most recently opened decide() (safe for 1 bot per game, heuristic if both bots interleave in one process).
	•	MULTIPLE_CHOICE picks routed through HeuristicAiBase log only the fallback pick, no scores.
