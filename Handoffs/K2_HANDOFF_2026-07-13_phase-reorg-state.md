# K-2 HANDOFF — Phase-Reorg Program State (2026-07-13, written at 82% weekly usage)

**For any fresh session (Opus or otherwise) continuing this work.** Read this + `Handoffs/AI_WORK_QUEUE.md`
(Codex co-maintains it) before touching anything. The standing rules in `~/.claude/projects/.../memory/MEMORY.md`
all apply — especially: check-replays-not-logs, one-change-at-a-time, changelog-on-push, engine-files-need-Steve,
comment-out-not-delete (EXCEPT inside Codex-sanctioned cleanup packets), verify-before-done.

## Steve's operating directives (2026-07-13, latest first — these override older habits)
1. **Token conservation**: hand off maximal load to SMALL AGENTS; K-2 main loop = coordination, commits, gates only.
2. **Utilize Codex MORE** — he has far more tokens. He authors packets, runs all independent gates, does source
   audits and preflights. Propose he take even more (see "role split" below).
3. F1/F2 engine fixes: **STEVE APPROVED** ("Both fixes sound great! Ship them", 2026-07-13). Ship = commit into
   the gated stack. DEPLOY of everything remains HOLD until the aggregate gate (Steve's m00274 directive).
4. Never deploy over a live game; deploy-gate script (last Evaluator decision >5 min) + work-verifier after deploys.

## The program (plan file: ~/.claude/plans/no-need-to-overflow-warm-donut.md)
Wide-scope phase reorg of Rando's decision logic. Codex = architect/coordinator/gater (mailbox
`python3 ~/claude-codex-mailbox/mailbox.py check --as claude --mark` / `send --from claude --to codex`;
raw store `~/claude-codex-mailbox/mailbox.jsonl` — read raw when monitors lapse). K-2 = implementer via
scoped background agents. EVERY commit gets an independent Codex gate; his HOLD blocks deploy/cutover only,
never independent prep (m00234). Frozen execution order (CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md):
trace stages → shared finalizer → interceptors → DRAW → PULL → CONTROL → objective adapters → ACTIVATE →
atomic DEPLOY parent+child → BATTLE → MOVE → SETUP.

## Gate ledger — ALL ADVANCE/PASS through 978b58e1d (HEAD at handoff time)
- Batch-1 Krennic corrections e17422f86 PASS (side-aware DeckOracle.getSourceCardFullGameText owner,
  case-sensitive title-grammar strip, persona flip exemption, friendly-count gate; 28 fixtures).
- Cleanup program COMPLETE + CLOSED by Codex ruling (m00408/m00415): 18 gated passes, ~3,700 comment lines,
  24 pairs/840 lines HELD as revert evidence — DO NOT DELETE MORE RESIDUE.
- Tie determinism 5df276c1b + fixtures 5240f36c6; harness b544ceba6 (evidence harvester only, NOT oracle).
- Trace oracle: 55c22fdde (no-op) → 97d2cb65a (V2 envelope) → dde6488e0 (2b: RawDecision verbatim capture,
  typed-INCOMPLETE everywhere, route-complete matrix). Capture DISABLED everywhere (NoOpTraceSink default).
- B2 snapshot types: e4e0aa213 → fa0f254ac → d558248cf (inert model clean; first shadow BUILDER still未 built).
- Registry f2bb32e95 + 24-arm authority table (31b9f697c/f2bb32e95): migration authority FOR THOSE 24 ARMS ONLY;
  344-arm marker sweep outstanding.
- Finalizer: F0 real-engine fixture corpus + F3 pure shadow (92965934b, corrected 4a5e7d6b8) ADVANCE-inert.
  ResponseFinalizer = the ONE V148 pass semantic; typed ForceReason invariant.
- Stage 4A1 01f821e87 ADVANCE: sealed state events (tracker RECORD_RESPONSE w/ complete DecisionTrackerSnapshot
  via pure traceSnapshot()/traceDecisionKey() seams; concede; PLAYER_LOST w/ distinct EngineCallOutcome;
  pending-deploy), envelope SCHEMA_VERSION 3, 10 observation hooks/bot.
- Fact repair 978b58e1d ADVANCE (MaintenanceFacts card-id swap + Battle Order comment truth).

## IN FLIGHT at handoff
1. **F1/F2 agent running** (engine files, Steve-approved): F1 = MultipleChoice checked bounds (flip the
   @Ignore'd red test fcMultipleChoiceBounds_checkedAfterF1 in EngineAwaitingDecisionContractTest);
   F2 = SwccgGameMediator initial+one retry for AI rejections, addTimeSpentOnDecisionToUserClock credit on AI
   success, VISIBLE terminal exhaustion keyed by decision OBJECT IDENTITY, AI-path only. Packet:
   CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md. COMMIT AS TWO SEPARATE COMMITS, each gated by Codex.
2. **Trace 4A2a ready to implement** (packet CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md,
   work order m00417): outer tracker updateState/clear observation, one isolated commit. PREFLIGHT DONE:
   call sites verified 4/4 (RandoCalAi 1208/1959, TheChosenOneAi 1063/1814); council YES on the bijection
   question; boundary test = consecutive calls yield one event each; PRECISION NOTE: UPDATE_STATE snapshot
   must NOT claim lastPhase (onPhaseChange-owned, heuristic increment). Preflight report to Codex was DRAFTED
  but NOT yet sent when Steve interrupted — send it (content above) before implementing.

## QUEUED (each with pinned Codex audit + fixtures; sequence per frozen order)
Route repairs: ACTIVATE (m00299, YesNo-results-never-reach-context stall vector) BATCHED with CONTROL (m00304);
objective adapters (m00300: backside parser reads wrong side, rematch reset); BATTLE first owner (m00305:
V61b wrong-target waiver); PULL facts seam (m00310); MOVE subroutes (m00316: destination-before-mover);
deploy-weight consolidation (m00280); lifecycle/reset packet (m00340/m00342 — cross-game leak NOT confirmed,
fresh bot per game; same-game dual-tracker IS confirmed); trace stages 4A2b+ per Codex packets.

## Commit/process discipline (hard-won this session — follow exactly)
- Shared changelog files (resources/AI_CHANGELOG.md + resources/k2-resources/originals/02-rando-history/
  AI_VERSION_HISTORY.md): construct content PER COMMIT; never wholesale `git add` them (two boundary
  contaminations happened; fixed via recreation, m00387/m00392). Changelog is newest-at-top; version history
  append-at-bottom.
- Agents NEVER commit; K-2 verifies (spot checks + focused suite) then commits; Codex gates the SHA.
- Cleanup-style packets: SHA-pin verification BEFORE deleting, comment-only dual-layer assertions,
  detached-worktree builds (main tree often carries other lanes' dirty edits), RAW javap no-delta.
- Focused test suite (never the full engine suite): FactValueTest, DecisionSnapshotTest,
  DecisionTraceEnvelopeTest, CombinedEvaluatorTraceTest×2, CombinedEvaluatorTieTest×2, RandoCalAiTraceHookTest,
  TheChosenOneAiTraceHookTest, ResponseFinalizerContractTest, EngineAwaitingDecisionContractTest,
  TraceStateEventTest (+ per-lane new classes). In-container:
  `docker exec gemp_swccg_app_1 bash -lc 'cd /opt/gemp-swccg/src && mvn -pl gemp-swccg-server -am test
  -Dtest="..." -Dsurefire.failIfNoSpecifiedTests=false -DskipITs -q'` with maven wait-guard
  (`while docker exec gemp_swccg_app_1 pgrep -f maven >/dev/null 2>&1; do sleep 15; done`).
- Mirror discipline: scoped sed `s/models\.rando/models.chosenone/` (+ class names) — NEVER bare s/rando/
  (corrupts 'random'); V79b block is the known rando-only divergence.
- Council: Ollama localhost:11434, deepseek-r1:70b-llama-distill-q8_0; num_predict ≥2000 (thinking eats budget).

## Proposed role split under the new token directive (put to Codex, m-number TBD)
- Codex: continues packets/gates/audits + takes MORE: preflights, boundary tables, fixture authorship,
  and (if he agrees to relax his read-only stance) small mechanical Java implementations that K-2 gates.
- K-2 main loop: mailbox triage, agent dispatch, commit gate, Steve comms. Everything else = background agents.

## LIVE STATUS (refreshed through m00470; K-2 at 90% weekly tokens, MAXIMUM-OFFLOAD PROTOCOL active)
- HEAD 7098f9b33 (Codex's own em-dash doc commit) atop 02c2e5fc1/67b285d6d/f6d00e1da. 4A2b ADVANCE-INERT
  through 7098f9b33 (m00469). Stage-4 observation arc COMPLETE and gated: 4A1 + 4A2a + 4A2b.
- ACTIVE: 4B1 heuristic-memory preflight per m00470 — corrected packet at
  Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md (parent 7098f9b33; six
  source-audit corrections applied by Codex). K-2 dispatched: one SMALL read-only source agent verifying the
  packet's claims + one narrow council check. Java HOLD until Codex release.
- F1/F2: PAUSED pre-edit; Steve's approval STANDS; relaunch brief = CODEX_FINALIZER_FIXTURE_RETRY_PACKET +
  m00431 pin retirement; needs only the dispatch.
- MAXIMUM-OFFLOAD PROTOCOL (Steve, 90% budget): Codex authors agent-ready briefs INSIDE packets (K-2
  dispatches verbatim); mid-flight corrections go INTO the packet file + one generic re-read ping; Codex
  prefixes non-action mail "FYI:" (K-2's monitor drops those); handoff refreshed at COMMITS and Codex
  requests only. K-2 main loop = dispatch, commit, gate handback, Steve comms. NOTHING else.
- MODEL-SWITCH RESUME: read this file + AI_WORK_QUEUE.md, check `git log --oneline -5`, read raw mailbox
  tail (~/claude-codex-mailbox/mailbox.jsonl) beyond the watermark, then continue the ACTIVE lane exactly.
  All discipline sections below remain binding.
- 4B1 COMMITTED (agent report reconciled, 842 module tests green). F1/F2 agent RUNNING (relaunched on Steve reconfirmed blanket go-ahead). Next: Codex 4B1 clean gate; F1/F2 report, then two separate commits, then his gates.
- SESSION END STATE (K-2 at 4pct tokens; OPUS SWITCH-OVER): 4B1 committed ec886934b; F1 committed
  5bd89ac68; F2 committed a095db834. Both engine fixes Steve-approved. NO agent lanes running. Tree CLEAN.
- OPUS FIRST ACTIONS: (1) read this file fully; (2) mailbox check --as claude --mark + raw store past
  m00481; (3) tell Codex "K-2 resumed on Opus, same protocol"; (4) his gate queue: ec886934b, 5bd89ac68,
  a095db834; on ADVANCE dispatch his next packet's embedded Agent-Ready Brief VERBATIM as a background
  agent; (5) every discipline section in this file binds; agents never commit; changelogs constructed
  per-commit; engine files beyond F1/F2 still need Steve.
- Remaining released queue: Codex packet stream (4B2+, route repairs per frozen order; K-2 tasks #13-#24).
  Deploy of EVERYTHING stays HOLD until the aggregate gate.
- Mailbox watermark: m00481.

## PRIOR LIVE STATUS (m00427, superseded)
- HEAD 0bad33598. Role split ACCEPTED (m00425): Codex = preflights end-to-end, boundary math/tables, fixture
  authoring in test/support surfaces, council/source audits, independent gates, 4A2b research; production Java
  stays with K-2 agents unless Steve transfers exact file ownership.
- Agent lane 1 (running): F1/F2 engine fixes per CODEX_FINALIZER_FIXTURE_RETRY_PACKET, Steve-approved;
  commits as TWO separate gated commits when it reports.
- Agent lane 2 (running): 4A2a per the m00426-UPDATED packet — DecisionTrackerLifecycleSnapshot wraps
  DecisionTrackerSnapshot + lastTurn + lastStateHash ONLY; lastPhase EXCLUDED (gate rejects it; onPhaseChange
  is heuristic-path, 4A2b); exactly one event per direct outer updateState/clear call. One isolated commit.
- Council CPU note (Steve 2026-07-13): pass "num_thread": 4 in ollama options on council calls; consider
  taskpolicy -b on the ollama pid; q4_K_M quant recommended over q8 for the 70B.
- Next commands after agent reports: verify focused suite in-container -> construct per-commit changelog
  content -> commit (F1, then F2, then 4A2a separately) -> send each SHA to Codex -> he gates.
- Mailbox watermark at this update: m00427.

## Landmines
- 24 held comment pairs are REVERT EVIDENCE — no more deletion.
- Capture stays disabled until Codex's enablement gate; V192/PULL predecessors held as rollback evidence.
- objective_playbooks.json = single objective data source; ObjectiveHandler is DEAD code (live = ObjectiveAnalyzer).
- Two backups: gemp-swccg-public-backup-20260712-221311 (byte-verified) + git history = undo paths.
