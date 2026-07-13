# Decision-Fixture Capture Harness (BATCH 0 prototype)

Captures every AI `decide()` call from GEMP server logs as JSONL fixtures, so a shadow
implementation can later be compared decision-for-decision (fixture parity gate).

Status per `Handoffs/CODEX_FIXTURE_HARNESS_REVIEW_E5B393955_2026-07-13.md`: this is the
EVIDENCE-HARVESTING layer, not the authoritative behavior oracle. The frozen fixture
contract lives in `Handoffs/CODEX_RANDO_DECISION_FIXTURE_SPEC_2026-07-12.md`.

## Usage
	•	Extract: `python3 extract_fixtures.py logs/gemp-swccg.log logs/2026-07/app-*.log.gz -o baseline.jsonl`
	•	Replay context (optional): `--replay replays/asdf/<id>.xml.gz` (raw zlib XML, despite the .gz name)
	•	Compare: `python3 compare_fixtures.py baseline.jsonl shadow.jsonl` (exit 0 parity, 1 divergent, 2 error)
	•	Tests: `python3 -m unittest discover tools/fixture-harness` (encodes the three Codex false-parity cases + parity/divergence coverage)

Comparison is EXACT by default: top-5 is compared as an ordered actionId sequence with
per-position scores at zero tolerance, `decisionText` is compared, and veto reasons are
compared as a full ordered list (not just a count). `--tolerance N` is the only way to
allow score drift, and only for a reviewed intentional delta (spec: waivers, not tolerance).

## Record schema (source="log")
`seq`, `game` (room id or `untracked-N`), `bot` (RandoCalAi / TheChosenOneAi), `ts`,
`decisionType`, `phase`, `decisionText` (trunc 200), `top5` [[actionId, score]...] from V191 TOPN
in logged order (actionId may be "" = pass), `fallback` (V191 fallback-heuristic pick, when no
top-5 exists), `bestAction`/`bestScore`, `reasoning` (trunc 500), `evaluatorDecision`/`evaluatorScore`,
`chosen` (decide() result), `chosenScore`, `vetoes` (FULL ordered list of FORMATION SAFETY /
HARD VETO lines, each trunc 300) / `vetoCount`, `clamps` (SAFETY CLAMP lines), `unterminated`
(decide() never resolved: crash/hang tail).
Records with source="replay" carry the engine-side D elements (decisionType/text/actionIds/actionTexts).

## Known limitations (gaps vs the fixture contract)

The authoritative field list is `Handoffs/CODEX_RANDO_DECISION_FIXTURE_SPEC_2026-07-12.md`.
Today's logs cannot provide these spec-required fields; they need the batch-2 production
trace hooks or the pure scripted-evaluator harness the spec describes:
	•	Ordered pre-merge contributions (evaluator, rule id/V-tag, domain, kind, per-rule delta) — log scores are POST-MERGE only.
	•	Raw IEEE-754 float bits — logs carry formatted decimals; the spec treats raw bits as authoritative, decimals as diagnostic.
	•	Complete candidate arrays and original ordinals — V191 TOPN logs at most 5 actions.
	•	Selected route and evaluator invocation order.
	•	Decision obligation flags (`noPass`, min/max) for log-sourced records (replay D elements carry `noPass` only).
	•	Facts/derived assessments with producer + provenance; winner before vs after safety correction; stable `fixtureId`/`baselineCommit` identity.

Harvester-level heuristics (also disclosed in the Codex review):
	•	Replays record only the HUMAN client's decisions (participantId=asdf); Rando's server-side decide() calls never appear as `ge type="D"` elements, so replays are context/cross-check only.
	•	Game id comes from "Welcome to room: Game..." TRACE lines; concurrently running games share one log with no per-line game tag, so the latest room wins (heuristic). Untagged segments get `untracked-N`.
	•	Evaluator/strategy lines carry no bot name; they attach to the most recently opened decide() (safe for 1 bot per game, heuristic if both bots interleave in one process).
	•	MULTIPLE_CHOICE picks routed through HeuristicAiBase log only the fallback pick, no scores.
	•	`decisionText` (200), `reasoning` (500), and veto lines (300) are truncated; changes past the truncation point are invisible to the comparator.
