# K-2 HANDOFF — Phase-Reorg Program State (2026-07-13, written at 82% weekly usage)

## ★ CURRENT TRUE STATE (2026-07-14 ~04:40Z — FRESH SESSION START HERE, everything below is older context) ★
- **Steve's model NOW (overrides all older cadence):** phases are INDEPENDENT (each = one decision
  domain's consolidated logic, no cross-over except minor force allocation; NOT whack-a-mole, NOT a
  coupled migration). DEPLOY EACH PHASE AS IT COMPLETES: implement → ONE gate → deploy (fast path) →
  test in game → next. ABC, always be closing. NO batch aggregate-gate/Fable wait. **NEVER PUSH**
  (Steve's standing rule; local deploy only). Cut ceremony hard. See memory feedback_deploy_each_phase +
  feedback_drive_pipeline_no_stopping.
- **DEPLOYED + LIVE so far (Codex drove these directly while the prior K-2 session stalled):**
  runtime/finalizer aa122eae8 (216/0/0/0, reload-ai, HTTP 200, byte-verified); DRAW ff04a4b69 (220/0/0/0)
  = current HEAD. PULL entering package/commit/deploy. Confirm with `git log --oneline` + docker.
- **DIVISION NOW:** Codex is DRIVING ALL PHASES SOLO (implement + gate + deploy) per m00626 — including the
  V44/V67j pilot (step 1b, frozen packet 7b7bc814) and ACTIVATE+CONTROL. The prior K-2 session degraded
  (dead background worker, missed m00616-m00624, stale mental model) and unblocked Codex to proceed without
  it. A FRESH K-2 rejoins as INDEPENDENT REVIEWER (not the bottleneck-driver), or takes a specific phase
  Codex hands to the mailbox.
- **FRESH SESSION FIRST ACTIONS:** (1) `git log --oneline -6` + `docker ps` for true HEAD/live state;
  (2) `python3 ~/claude-codex-mailbox/mailbox.py check --as claude --mark`, then read the raw mailbox
  tail past the last id for anything unread; (3) send Codex a short RESUMED with current HEAD + "reviewer,
  not blocker"; (4) arm a DIRECT any-new-mail watcher (background bash polling mailbox.jsonl; the passive
  Monitor FAILED and dropped 9 action mails — do NOT trust it); (5) do NOT re-impose heavy ceremony;
  (6) `caffeinate -dimsu` is running (prior session started it) so the Mac won't sleep — battery pmset
  sleep is 1 min, a landmine if unplugged.
- **The stall root cause (fixed):** passive mailbox Monitor mis-filtered Codex action mail as FYI →
  K-2 went deaf, not idle. Always poll the mailbox directly + arm the direct watcher.

### K-2 REVIEWER STATUS (2026-07-14, fresh session) — A+C phase
- Baseline FROZEN at 8789a74fe (Codex m00633): V44/V67j pilot API already committed here
  (AiDecisionResult.MutationMode{NONE,OUTER_COMMON}, FinalizedResponseAdapter, RevertApprovalPhaseOwner).
  Codex owns the full A+C Java cutover in his worktree /Users/steve/gemp-swccg-ac-phase-codex
  (branch codex/activate-control-cutover); K-2 does NO Java edits — independent reviewer only.
- K-2 reviewer worktree: scratchpad/ac-worktree @8789a74fe (READ-ONLY reference). Evidence docs in
  scratchpad/: AC_PHASE_B_PREFLIGHT_2026-07-14.md, AC_UNOWNED_INTEGER_INVENTORY_2026-07-14.md.
- A+C PHASE COMMITTED by Codex: **7205159f0** (base 8789a74fe; 186 tests 0F/0E, mirror-parity 8 pairs PASS,
  resolver/ActivateAmountPolicy/ControlDrainAssessment each exactly 2 consumers). NOT yet deployed.
- Deliverable #1 (unowned-INTEGER inventory) DONE → m00634. CASE A regression (passable min==0 unstamped
  INTEGER → PassEvaluator empty decline) ACCEPTED by Codex (m00635) + FIXED in 7205159f0: both
  PassEvaluator.canEvaluate now return false for INTEGER → all unstamped INTEGER route to
  HeuristicAiBase.pickInteger (gain→MAX/lose→MIN). Verified in diff.
- Deliverable #2 (direct decide() rejected-route compat) — **PASS** (m00637). decideOwnedActivateControl:
  TYPED_REJECTION → mediatorFacing returns typed rejection; direct path → completeDirectActivateControlCompatibility
  reproduces legacy raw-string (tryEvaluators/super.decide + emergency + ensureValidResponse + outer mutation),
  never hits baseline decide() throw. Minor note: invoked-then-rejected defensive branch uses super.decide()
  vs legacy evaluator — no real-input delta.
- Deliverable #3 (A+C vs PULL_PARENT overlap) — **PASS** (m00637). Dispatch DRAW→A+C→PULL, disjoint origin
  claim sets + wire-shape guard = order-independent; 1 authorized game-state read; snapshot reused.
- ALL THREE A+C REVIEW DELIVERABLES CLOSED. No hard stop.
- **A+C DEPLOYED + INDEPENDENTLY VERIFIED (2026-07-14 ~07:28Z, m00642).** HEAD 7205159f0 (now 822202e1f w/
  Codex doc-fix). Bytecode-confirmed in live web.jar (SHA 9fcbea25...8e89, exact match): PassEvaluator
  INTEGER guard live both bots (CASE A fix), ForceActivation origin gate live (getDecisionOrigin→ACTIVATE_
  AMOUNT/ALLOWANCE), new A+C classes present (genuine rebuild), container restartCount 0, HTTP up.
  NOTE: javap needs the CONTAINER (no JDK on host).
- **NEXT PHASE = Objective facts/adapters** (packet afda65ec, step 5). K-2 bounded preflight DONE (m00645,
  evidence scratchpad/OBJECTIVE_PHASE_PREFLIGHT_2026-07-14.md). 3 corrections + dead-ObjectiveHandler
  landmine CONFIRMED. MISMATCHES: (A) ACCEPTED — NO ObjectivePullAdapter/any
  Objective*Adapter exists; all 5 net-new; Codex builds ObjectivePullAdapter fresh, only its objective-source
  parent/child authoritative. (B) WITHDRAWN (verified m00647) — V193 child 2000f DOES exist, in
  CardSelectionEvaluator:2288 (v193csWeight 400 + 1600 = 2000f), distinct from parent 400f (DeployEvaluator
  :1600-1602); my preflight agent missed it by searching only the deploy path. Refinements: correction #3 half-done (findProfile
  already blueprint-first); correction #2 leak unreachable in hall path (fresh bot/game); front/back parse
  currently DEAD. Landmines: production builds NO snapshot (trace-gated, must hoist); double snapshot builder;
  3x/decision analyzer refresh; objective truth summed across many evaluators (emit-once fan-out); temp-id trap.
  Codex to reconcile mismatches, then implement; K-2 reviews his commit + verifies deploy.
- OBJECTIVE PHASE pre-commit GATE (worktree /Users/steve/gemp-swccg-objective-phase, branch
  codex/objective-facts-adapters, base 822202e1f, uncommitted): K-2 independent verdict = **PASS WITH NOTES**
  (m00654, evidence scratchpad/OBJECTIVE_PHASE_GATE_VERDICT_2026-07-14.md). All 10 checklist items PASS
  (purity/one-refresh/front-back/reset-ref/one-snapshot/facts-only/shadow-adapters/parity; ObjectiveHandler
  untouched; engine changes minimal+behavior-neutral). 2 CLOSE-BEFORE-DEPLOY: (1) CONCERN — authoritative
  ObjectivePullAdapter yields 0 on UNKNOWN objective identity vs legacy 1500/500 (likely unreachable; prove via
  fixture or add legacy fallback); (2) DISCIPLINE — legacy parent tier arm DELETED not commented (restore comment).
  Codex to close both, then commit → his gate → deploy. K-2 will bytecode-verify deploy like A+C.
- OBJECTIVE PHASE COMMITTED 1fa153eaf (parent 822202e1f). K-2 gate: SHA == reviewed state + ObjectiveFactsSource
  comment tweak = valid CHECKPOINT (commit PASS). **CLOSE-ITEM 1 ESCALATED (m00657) to CONFIRMED PRE-DEPLOY
  BLOCKER:** traced ActionTextEvaluator (rando) 5137-5176 — v192ObjectiveSource is FORCED true by engine
  srcCat==OBJECTIVE (:5146), DECOUPLED from ObjectiveFacts identity; on identity UNKNOWN/mismatch the
  authoritative adapter returns null → v192Tier=0.0f (:5175, "Unknown facts contribute zero") where legacy gave
  1500. Real reachable 1500→0 regression (Steve #1 discipline). FIX: when v192ObjectiveSource && adapter==null,
  fall back to legacy 1500 (both bots), OR fixture proving identity always ID-matches. DO NOT DEPLOY 1fa153eaf
  as-is. Close-item 2 (parent-arm comment breadcrumb) minor, still open. Codex to fix → re-commit → K-2 re-gates delta.
- OBJECTIVE PHASE both notes CLOSED in replacement commit **978527306** (supersedes 1fa153eaf; parent
  822202e1f). K-2 re-gate = **GATE PASS** (m00659, verified delta). Item1: ObjectivePullAdapter.adaptParent/
  adaptChildAtOrdinal legacy 1500/500 fallback guarded to (physicalObjectiveSource && identity unknown &&
  eligibleNonFailure && correct route && real ordinal && predecessor-only for child); NOT on BLOCK/net-new/known.
  Item2: +1500 arm comment retained. Parity identical, 172 tests. CLEAR TO DEPLOY after Codex gate + no-live-game.
  K-2 will bytecode-verify live jar (javap in-container; host has no JDK).
- OBJECTIVE FINAL SHA **3a4214866** (supersedes 978527306+1fa153eaf; parent 822202e1f). K-2 re-gate = **GATE
  PASS** (m00662). Broadened the pull fallback: dropped the `!identity().isUnknown()` guard in adaptParent+
  adaptChildAtOrdinal so legacy 1500/500 fires whenever parentContributions-empty (UNKNOWN OR known-id-mismatch)
  + physicalObjectiveSource + eligibleNonFailure + route + ordinal (+predecessor-child). Faithful to legacy;
  no double (gated on normal-path-empty); BLOCK/failed/net-new still 0. Parity identical, 173 tests.
  NOTE (honesty): my 978527306 PASS was INCOMPLETE — its isUnknown()-only guard left known-id-mismatch at 0;
  I flagged both in m00657 but didn't catch the half-cover on re-gate. Codex broadened it; 3a4214866 covers both.
  CLEAR TO DEPLOY after Codex gate + no-live-game. K-2 bytecode-verifies live jar (javap in-container).
- **OBJECTIVE PHASE DEPLOYED + INDEPENDENTLY VERIFIED (2026-07-14 ~10:01Z, m00665).** HEAD 3a4214866,
  host jar SHA ea1fac050...eafec (exact match), container restartCount 0 startedAt 10:01:52Z, HTTP 200.
  Bytecode (javap in-container): ObjectiveFactsProducer/ObjectivePullAdapter/ObjectiveSetupAdapter/
  ObjectiveDeployAdapter present (rebuild); adaptParent 5-arg + adaptChildAtOrdinal 7-arg fallback overloads
  compiled in; 'Unknown facts contribute zero' string = 0 (regression GONE both bots); 'OBJECTIVE-ADAPTER' = 2.
  4 PHASES NOW LIVE+VERIFIED: DRAW, PULL, ACTIVATE+CONTROL, OBJECTIVE.
- NEXT = DEPLOY owner (manifest step 6, packet CODEX_DEPLOY_PHASE_PACKET_2026-07-13.md, SHA
  6ba53824...). K-2 to start bounded preflight when Codex releases it.
- DEPLOY-owner phase (step 6, packet 6ba53824) bounded preflight DONE (m00668, evidence
  scratchpad/DEPLOY_PHASE_PREFLIGHT_2026-07-14.md). Shadow ObjectiveDeployAdapter deployed but DISCARDED
  (zero delta today); all risk at exclusive-owner flip. MATERIAL FLAGS: (1) VERIFIED obj-site VALUE MISMATCH —
  adapter OBJECTIVE_SITE=200f flat vs legacy 3-way split 200 post-flip-protect/150 PRE-FLIP/0; flip shifts
  pre-flip +50 (silent regression unless adapter models flip state); (2) My Lord disable OVER-REACH —
  getDeployObjectiveAdjustments loop (DeployEvaluator:1425) mixes My Lord (owned) + V86/V99 (keep); suppress
  per-rule not call site; (3) V193 child same value 2000 but DIFFERENT gate (ability>=1&&cost<=4 vs
  !controls&&holdsGate) — parity risk; (4) obj-site TWO consumers (CardSelectionEvaluator:1742+:7191). V170:
  prompt-text gated + unknown-vs-zero drain conflated + malformed→0/1 default (all packet-forbidden). PullDeployRef
  exists; DeployPhasePlanner needs substantial lifecycle extension. My Lord/V193-parent values MATCH.
  Codex to reconcile before implementing; K-2 gates his commit.
- DEPLOY split into sidecars. First: OBJECTIVE-EMITTER sidecar 07758a7c1 (parent 3a4214866, branch
  codex/deploy-objective-emitter-sidecar) makes ObjectiveDeployAdapter the EXCLUSIVE live emitter for the
  objective-deploy set. K-2 gate = **PASS WITH NOTES** (m00671): no double-count, byte-parity, all 4 preflight
  flags closed (obj-site 150/200/0 split, My Lord per-rule w/ V86/V99 preserved, V193 child = legacy-branch
  truth, both CS consumers). if(false) comment-out honored. HEADLINE DECISION (Steve's domain): legacy Deploy
  §B obj-site was FLAT +200 (flip-unaware, CharacterDeploySiteEvaluator verified); consolidation moves it to
  flip-aware 150/200/0 → EVERY objective deck: pre-flip 200->150, post-flip-nonprotect 200->0. Strategically
  correct (CS already used 150/200/0) but a real scoring change; deviates from packet's literal 'exact 200f'.
  Narrower notes: CS filter-rules obj-site gap (loader-rules objectives only); V193 child hardcoded 2000 vs
  weight+1600 (step-11); V88 Senate substring->filter (theoretical). Author didn't run tests — recommend
  runtime smoke (Endor + My Lord decks) before live.
- Obj-site semantics RESOLVED: Codex made the objective-emitter sidecar BEHAVIOR-PRESERVING (Deploy flat+200
  + CS 150/200/0 both stay EXACT; flip-aware unification deferred to step-11). Steve dismissed the canonical-
  semantics question → default stands (behavior-preserving now).
- DEPLOY owner commit 8fcb9fa9a = HOLD (Codex self-caught direct-entry lifecycle defect + found 6 open frozen
  fixtures 2/5/6/9/10/11). m00678 was stale/retracted (m00679) — ignore. Do NOT gate/deploy 8fcb.
- K-2 took DISJOINT SLICE **fixture 11** (DeploymentPlan.assessmentCopy purity) and CLOSED it (m00685):
  DeploymentInstruction is mutable + DeployPhasePlanner binds ids during assessment → shallow copy leaked into
  live plan (rule 11 break). Fix = deep-copy ctor + per-instruction copy in assessmentCopy (both bots, parity-
  verified). Fixture DeploymentPlanAssessmentCopyPurityTest 2/2 PASS, BUILD SUCCESS. Patch /tmp/fixture11_deepcopy.patch
  (base 8fcb9fa9a) for Codex to fold into amended SHA. K-2 worktree scratchpad/deploy-fix11. NOT committed/pushed.
  Codex owns 2/5/6/9/10 + V170 + amended DEPLOY SHA; K-2 gates it + offered to take fixture 2 or 10 next.
- K-2 fixture-11 patch ACKed by Codex (applying it); fixtures 2+10 already in his worktree (don't dup).
- K-2 did DISJOINT REVIEW = optional/mandatory all-veto CombinedEvaluator coverage (m00690, both bots).
  PARITY ok. 5 findings: (1) TWO veto notions — hard-veto isHardVetoed→fsAllVetoed path vs score-pseudo-veto
  -9999→argmax+v148PassBar path; coverage needs both × optional/mandatory = 4 cases/bot (score-pseudo-veto is
  the common untested one); (2) mandatory all-veto returns least-bad bestAction with NO typed forced-choice
  marker (fixture-10 core, SAFE not a crash); (3) fsCanPass + v148 canPass DUPLICATE identical cancellability
  logic (agree, redundant); (4) DEAD log branch `else if(canPass)` ~558 unreachable (log-only, no behavior);
  (5) no dedicated all-veto fixture in baseline. Review-only, no edits.
- MAILBOX GOTCHA: bodies with { } ( ) break `mailbox.py --body "..."` via zsh (parse error, send FAILS
  silently). Send via python subprocess arg-list (read body from a file) — see the m00690 send pattern.
- Watcher armed at seq-690 for Codex's amended DEPLOY SHA (fixtures 2/9/10/11 + V170) to gate.

---


**For any fresh session (Opus or otherwise) continuing this work.** Read this + `Handoffs/AI_WORK_QUEUE.md`
(Codex co-maintains it) before touching anything. The standing rules in `~/.claude/projects/.../memory/MEMORY.md`
all apply — especially: check-replays-not-logs, one-change-at-a-time, changelog-on-push, engine-files-need-Steve,
comment-out-not-delete (EXCEPT inside Codex-sanctioned cleanup packets), verify-before-done.

## Steve's operating directives (2026-07-13, latest first — these override older habits)
-2. **ANTI-STALL CHECK-IN (Steve 2026-07-13): never let a long worker/wait masquerade as a stall.** When a
    background worker runs long: (a) send Codex a one-line heartbeat (worker at step X, ball position); (b)
    run a stall-detector Monitor on the worker output mtime that pings K-2 if idle >5min, so K-2 never
    blind-waits; (c) if Codex/Steve perceives a stall, immediately reply with the true state (worker mtime +
    tree footprint + mailbox tail prove who holds the ball). The harness auto-notifies on worker completion +
    Codex action mail (monitor bui/b2464...); the gap was silent worker runtime — the heartbeat + stall
    detector close it. This REPLACES the deleted 30-min commlink (m00501) with a lighter self-managed check.
-1. **PROCESS RESET (Steve m00501, effective next behavioral phase; 4B2 finishes old-style):** NO more
    micro-commit/micro-gate. Each remaining behavioral phase = ONE coherent implementation tranche -> ONE
    phase commit -> ONE mandatory independent phase verification. Do NOT reread/restate full session history
    per edit; resume from THIS handoff + AI_WORK_QUEUE + the active phase packet + exact source files only.
    Update ONE durable ledger at PHASE boundaries (this file), not overlapping docs. Mailbox Codex ONLY at a
    phase boundary, a real blocker, or a material finding. The 30-min heartbeat is deleted; Codex checks
    manually at meaningful boundaries.
0. **FINAL FABLE GATE before ANY deploy** (Steve via Codex m00497): after Opus completes this consolidation
   tranche, a FRESH FABLE session must independently re-verify Opus's exact committed diff, phase/trace
   invariants, focused + aggregate tests, and capture-disabled state, then report findings to Codex for the
   FINAL gate. NO deployment until that Fable review AND Codex's final gate are both complete. This is a hard
   precondition on the aggregate deploy, on top of every per-commit gate.
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
- ACTIVATE+CONTROL Option-2 shadow 443248a65 PASS (m00543): DecisionOrigin seam + pure ActivateControlRouteResolver,
  zero production consumer. THEN decide-equivalent HARNESS ad8f59385 PASS (m00560): test-only baseline freeze,
  6 pure fixtures, full raw-decision fidelity, 64/0/0/0 — the verifiable baseline that unblocks Phase B.

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
- OPUS SESSION LIVE (resumed on claude-opus-4-8; continuity confirmed with Codex m00483/m00485).
- ALL THREE INHERITED COMMITS GATE-CLEAN: 4B1 ec886934b ADVANCE-INERT (m00484, 842 tests), F1 5bd89ac68
  ADVANCE (m00486), F2 a095db834 ADVANCE (m00487, 128 green). The F1/F2 engine-fix lane is DONE + gated.
- HEAD 2eb105a4a (this doc commit will advance it). Tree clean, no agents running.
- 4B2 RELEASED FOR JAVA (m00494, released packet SHA 8f5a0438, parent 85eb0452a). Preflight AGREE against
  revised SHA 1631df9e; release-SHA delta is the status-flip only, embedded brief confirmed to encode the
  verified contract before dispatch. ONE background agent RUNNING (K-2 lane trace-4b2, owns: RandoCalAi/
  TheChosenOneAi hook sites, both StrategyController traceSnapshot seams + StrategyControllerTraceAccess
  bridges, TraceSession record methods, StrategyControllerSnapshot/Owner + six event records + sealed
  permits, focused 4B2 tests + mirrored bot fixtures). Agent will NOT commit; K-2 verifies + isolated commit
  + gate handback. On its report: focused suite + full module + package exit + parity + seven-method javap +
  diff-check + NoOpTraceSink-default proof -> constructed changelogs -> commit -> SHA to Codex.
- OPUS FIRST ACTIONS (if this session also ends): (1) read this file; (2) mailbox check --as claude --mark
  + raw store past the watermark below; (3) tell Codex K-2 resumed, same protocol; (4) act on his latest
  released brief only. Discipline sections below all bind; agents never commit; changelogs per-commit;
  engine files beyond F1/F2 need Steve.
- PROGRAM SCOPE — ACCURATE FRAMING (Codex m00499, do NOT describe as almost done): what is COMPLETE is the
  ARCHITECTURAL / TRACE / FINALIZER FOUNDATION (trace observation 4A1-4B2, snapshot/fact types, pure
  finalizer F0/F3, F1/F2 engine fixes, cleanup program, domain registry). What REMAINS is the BEHAVIORAL
  MIGRATION, the part that actually changes how Rando plays:
    1. 4B2 (StrategyController observation) COMPLETE — committed + Codex-gated (last FOUNDATION piece).
    2. FIVE major behavior lanes remain, ACTIVATE+CONTROL Option-2 groundwork DONE+gated; NEXT behavior lanes: objective adapters,
       BATTLE owner, PULL facts, deploy-weight/plan consolidation.
    3. THEN: phase cutover / shadow integration, aggregate + Fable + Codex gates, controlled GEMP game
       validation, and finally the deploy decision.
  Foundation != behavioral migration. The program is mid-flight, not near-done.

- PHASE CLOSED — GATE PASS m00560 (2026-07-13): decide-equivalent HARNESS phase, test-only, gated SHA ad8f59385
  (was 385373457; amended once for Codex gate HOLD m00556 [raw-param fidelity assertion had been decisionOrigin-only
  + doc precision] + wording precision m00557; re-gated PASS m00560 on clean detached worktree). Focused pass 64/0/0/0
  DUMP=false; FULL raw-decision fidelity asserted (source==ENGINE_PARAMETERS + verbatim param map, key order,
  present-empty); both bots candidate/score/veto/route/response parity, operation streams byte-identical (botModel
  intentionally bot-specific); no src/**/main change; deferred seed fixtures untracked; NoOpTraceSink default intact.
  UNBLOCKS deferred Phase B (the ACTIVATE/CONTROL live cutover) — now has its verifiable baseline. NEXT packet is
  Codex's to release; K-2 starts NO Java without a released frozen packet.
- PHASE B PREREQUISITE DISCOVERED (Codex m00563 preflight + K-2 verify m00564): Phase B section 7 (route consumer)
  cannot freeze until the finalizer has a PRODUCTION CONSUMER and tracker mutation moves to POST-ACCEPTANCE. Verified
  source: ResponseFinalizer = NO PRODUCTION CONSUMER (fixture-only); AI.decide() returns raw String + does
  DecisionSafety+recordDecision BEFORE engine acceptance (RandoCalAi:1060 / TheChosenOneAi:915); SwccgGameMediator
  :1351-1378 = decide->decisionMade with only the F2 retry, no typed acceptance callback. K-2 RULING to Codex
  (m00564): this must be a SEPARATE major prerequisite phase, NOT bundled into ACTIVATE/CONTROL — it's GLOBAL (every
  route/AI), ENGINE-TOUCHING (SwccgGameMediator + SwccgAiController interface — needs STEVE approval like F1/F2),
  CROSS-AI (3 impls incl CuratorAi), needs its own global parity gate, and must stay independently revertable.
  Suggested shape: P-a SHADOW (route decide through finalizer, produce trackerMutation request, legacy authoritative,
  zero-divergence gate, test-only) -> P-b CUTOVER (typed acceptance callback in mediator+controller [Steve-approved
  engine change], move recordDecision to post-acceptance in HeuristicAiBase/both bots/CuratorAi, flip finalizer
  authoritative). Only after P-b gates can Phase B route through the consumer.
- CODEX DECISION (m00565): AGREED separate major prerequisite. He is drafting ONE global finalizer-runtime
  prerequisite packet (NOT the P-a/P-b split — one coherent phase per Steve's cadence: one edit, one verify, one
  commit), covering engine/API blast radius, Curator compatibility, accepted/rejected callback semantics, focused
  no-game test matrix. STAYS UNRELEASED until STEVE explicitly approves the SwccgAiController + SwccgGameMediator
  PRODUCTION changes (engine gate, like F1/F2). After the draft hash Codex asks K-2 for ONE council/source check
  before release. K-2 holds read-only until then; the Phase B Java packet is also withheld until this lands.
  ORIGINAL IN-FLIGHT NOTES (retained for context): Packet
  CODEX_ACTIVATE_CONTROL_DECIDE_HARNESS_PACKET_2026-07-13.md (sha e65b3878...), released m00545 at baseline
  443248a65. Codex m00544 preflight: harness is TEST-ONLY — existing DecisionTrace already exposes candidate/
  merge/op/winner/response, so ZERO production accessor/cutover/schema change needed. Scope = 3 new test files
  (AbstractActivateControlDecisionHarnessTest + Rando/ChosenOne adapters), 6 pure fixtures freezing current
  ACTIVATE/CONTROL boundary (activateZeroConfirmLegacy=0/Yes is the KNOWN DEFECT frozen as evidence, not policy).
  Agent implementing; K-2 runs ONE focused verify pass then creates the single phase commit for Codex's gate.
  PRE-GATE CONDITIONS (Codex concurrent review m00549/m00550, RELAYED to worker): harness was mid-freeze
  (DUMP/discovery mode captured but not frozen). Do NOT gate/commit until (1) DUMP=false in committed form;
  (2) assertOps no longer early-returns AND all four COMBINED_EVALUATOR op-lists (activateTopLevel,
  controlTopLevel, activateAmount, activateAllowance) frozen with exact captured ops (ordinal/action id/
  evaluator id/rule-domain-kind/raw before-delta-after bits/veto state+reason/detail, ordered, unsorted);
  (3) activateAllowance overrides getCurrentPlayerId so recipient!=turn player AND asserts it. Hard stop
  re-armed: captured response/route diverging from the frozen table => stop + return actual trace, no production edit.
  FIDELITY CORRECTIONS (Codex source audit m00552; packet corrected in place, NEW sha 40fff3d10e77...): (A)
  activateAmount real engine min=0 NOT 1 (AbstractSwccgCardBlueprint:2243) — response stays 3, obligationFlags
  UNKNOWN (no noPass on the INTEGER amount decision => obligationsKnown=false; min=0 gave a byte-identical trace to
  min=1), re-freeze ACTIVATE_AMOUNT_OPS from real min=0 trace; (B) controlTopLevel action must carry one
  aligned nonblank source cardId (CardActionSelectionDecision:69; stub may resolve to null; CONTROL stays a
  routing/merge smoke, drain guards deferred) — re-freeze CONTROL_TOP_LEVEL_OPS. Allowance keeps DIRECT
  assertNotEquals(recipient, stub getCurrentPlayerId); do NOT prove turn player via recipient-valued trace.
  Fallback routes = HEURISTIC_FALLBACK. Worker (agent a7655a03) re-capturing + re-freezing; K-2 reviews + commits.
  FIRST agent run (pre-correction) had passed 64/0/0/0 with DUMP=false but on the OLD min=1/no-cardId fixtures.
  Deferred Phase B (the actual behavior cutover: kill wrong-Yes-to-pass, INTEGER cross-talk, drain consolidation)
  still unreleased until AFTER this harness gates.
- HARNESS PHASE COMMITTED: HEAD = amended harness commit ad8f59385 (was 385373457; test-only, 6 files, 64/0/0/0,
  no src/**/main change). Codex gate HOLD m00556 + wording m00557 both applied via one-phase amend; re-gate
  requested m00558 (full evidence). Awaiting his PASS; task #26 stays open until then.
- FINALIZER-RUNTIME PREREQUISITE PACKET REVIEWED (draft CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md,
  sha e9e4d8a95fdf). K-2 review = 4 parallel read-only source agents + council (num_thread=4): VERDICT AGREE — design
  sound + source-verified. ThreadLocal-through-callback question ANSWERED SAFE (GEMP pull model: decisionMade does
  NOT re-enter ai.decide; child re-entry at SwccgGameMediator:1398 is AFTER onDecisionAccepted per §3 step5;
  TraceSession single-slot, refuses nested opens :77-79, clears in finally :133-135). Returned to Codex (m00568):
  ONE correction (§9 Curator override fixture unbuildable — override gated behind static-final USE_MODEL+private
  shouldConsult, no seam, no CuratorAi test exists; Codex must authorize a minimal override test seam or relax the
  fixture) + THREE gate-guards (onDecisionAccepted must fire before :1397/:1398; trace-clearing finally must be
  MEDIATOR-side spanning the whole attempt not just the callback, else a RuntimeException leaks the session across
  pooled-thread games; item-4 closeAndEmit must become MODE-AWARE so it is not double-closed/leaked, and the tracker
  before-snapshot moves with recordDecision). PACKET STILL AWAITS: (a) Codex's Curator-fixture fix, (b) STEVE's
  explicit engine approval of SwccgAiController + SwccgGameMediator production changes before ANY Java.
- PACKET FROZEN FOR STEVE APPROVAL: Codex incorporated all 3 gate-guards + the Curator seam (package-private
  injected-Rando ctor + pure applyOverride helper) into CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md,
  new frozen sha 68939339cd2a... (m00570). ONE addendum outstanding (K-2 m00571, sent AFTER his freeze): the
  mediator trace-clearing finally must wrap the decideForEngine CALL ITSELF (not just 'successful decideForEngine
  through disposition'), because Curator's SYNCHRONOUS 300s callOllama consult runs INSIDE decideForEngine after the
  wrapped Rando trace is already open (CuratorAi:69 CONSULT_TIMEOUT_S=300, :318 http.send; USE_MODEL default TRUE) —
  an unchecked throw there must still clear the session; + add a Curator-consult-throw/timeout -> no-leak fixture.
  NEXT: once Codex reconciles m00571 and locks the FINAL hash, K-2 presents Steve the explicit engine-approval
  decision (SwccgAiController + SwccgGameMediator) on that exact hash. NO Java until Steve approves.
- FINAL PACKET LOCKED (Codex m00573): CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md sha
  9b3d1afc84a4dd2b78a9ec5c98518b1be97fce2e97c59fe851057e72f2a07ca0 — all K-2 corrections + gate-guards + the
  Curator 300s-consult window + mediator last-resort leak guard + caught-timeout fallback fixture + unchecked-consult
  no-leak/no-mutation fixture incorporated; git diff --check PASS. Codex also wrote a plain brief at
  CODEX_FINALIZER_RUNTIME_APPROVAL_BRIEF_2026-07-13.md (sha a3419d0d...). K-2 delivered Steve a readable approval
  ARTIFACT (tables) at claude.ai/code/artifact/4c0cf125-b0ac-4171-8aff-5e1d3da8c607. AWAITING STEVE'S EXPLICIT
  APPROVAL of the engine changes (SwccgAiController + SwccgGameMediator). On 'approve': K-2 implements ONE phase ->
  ONE verify -> Codex independent gate -> commit; still NO deploy (aggregate gate + Fable + Codex final still apply).
- STEVE APPROVED + FULL AUTONOMY GRANTED (2026-07-13): 'I've said I approve ... Pull the trigger on all the
  phases and let me know when it's finished. No need to ask any more permissions until the job is done. We have
  backups.' => Execute ALL remaining phases autonomously to DEPLOY; NO per-phase permission asks. KEEP the
  verification gates (Codex per-phase + final Fable review + aggregate) as SAFETY, not permission (they are what
  makes backups a fallback not the plan). Deploy at the end after the triple-lock passes; report at phase
  boundaries + when finished. Only surface to Steve on a genuine unrecoverable blocker or completion.
- PHASE IN FLIGHT: finalizer-runtime prerequisite RELEASED (Codex m00574) sha 413b1658 (= approved 9b3d1afc
  substance + RELEASED status flip; verified on disk, baseline ad8f59385). Implementation agent ace3f514 running
  the full phase (packet sections 1-9, one coherent tranche). K-2 will run the §10 focused pass, review, commit,
  return §12 evidence for Codex's aggregate gate. Asked Codex (m00575) to PIPELINE the next packet (ACTIVATE/CONTROL
  behavioral cutover) while this implements, then objective adapters/BATTLE/PULL/MOVE per frozen order.
- HARD STOP on released 413b1658 (Codex independent source gate m00579): FOUR load-bearing contradictions —
  (a) trace ownership must be ORTHOGONAL to mutation mode: even mode NONE must DEFER trace close on the
  mediator-facing path (Curator can override a NONE/V45 result, inline close records the wrong accepted response);
  (b) F2 retry/rejection history must reach the AI via a backward-compatible OVERLOAD (+ ENGINE_DECISION_INVALID
  reason + Curator forwarding, no maps/fields/threadlocals) so Phase B RejectionHistory is truthful not .empty();
  (c) rejected attempts close COMPLETE on proposed-wire + engine-disposition evidence, NO fabricated accepted
  finalResponse; (d) accepted disposition LATCHES before the callback, callback fault takes runtime-fault path,
  never rejection/retry (no double-submit). Worker had written ZERO code (still studying) -> clean tree ad8f59385,
  no reconciliation debt; its planned design would have tripped (a)+(c) — gate caught it pre-code. Codex correcting
  the packet + will send new hash; K-2 resumes the SAME worker (source already loaded) on the corrected packet.
  Council inspecting the 4 points async; ordering-tail workflow (w3rr9x640) still running.
- FULL-TAIL ORDER delivered to Codex (m00582) for master-order freeze — dep-map agent (over 8 lane audits + plan +
  FACTS_CONTRACT + old CUTOVER_ORDER) + council: AUTHORITATIVE dependency-safe tail AFTER combined ACTIVATE+CONTROL =
  DRAW -> PULL -> objective-adapters -> DEPLOY -> BATTLE -> MOVE -> SETUP -> interceptors(retirement); then
  deploy-weight/solo tuning as final follow-on (through DeployPlan owner only) -> deploy. Key edges: PULL before
  objective (ObjectivePullAdapter needs pull txn key); objective before deploy/battle/move; SETUP near-LAST (NOT
  low-risk: shares CardSelectionEvaluator + objective bootstrap); interceptors LAST (retire after each owner:
  V45->BATTLE V170->DEPLOY V61->SETUP V79b->MOVE waiver V44/V67j->finalizer); MOVE after DEPLOY (V169 3-arm). 8
  per-lane gate-guards attached (SETUP-early trap, interceptor early-retire, forced-here pull guard till DEPLOY,
  V169, pseudo-veto conversion per-arm only, deploy-weight last, shared CardSelectionEvaluator). Council (m00583)
  CONFIRMED all 4 runtime corrections + amplified (4): harden accepted-callback partial-failure (trace closes in
  finally, callback throw = runtime fault, no half-tracker corrupting next decision).
- CORRECTED finalizer packet ac62724a REVIEWED -> AGREE (m00585): verified all 4 contradiction fixes
  (trace-orthogonal-to-mode §1/§4/§7; backward-compat RejectionHistory overload §2; disposition-aware completeness
  no-fake-finalResponse §7; acceptance-latch-before-callback §3) + council 5th amplification (partial-mutation:
  §4 240-243 + fixture) + new ATTEMPT_FAILED replacing abandon() + Curator 300s consult handled. No new contradiction;
  baseline ad8f59385. Worker ace3f514 had ZERO edits -> clean-from-scratch impl. Flagged ONE gate watch-point
  (outer-tracker-read-shapes-retry, inert per source, covered by no-wire-change hard-stop + fixtures). Did NOT re-run
  council (already confirmed the 4 points m00583; faithful impl). AWAITING Codex RELEASE (status flip + hash) to
  resume worker ace3f514 on that exact hash with the 4 fixes+amplification+ATTEMPT_FAILED baked into its brief.
- MASTER ORDER FROZEN (Codex m00586, CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md sha f390d1a3, was 4e3c5a1f, m00592 corrected: DRAW owns CARD_ACTION_CHOICE only + typed DRAW_CARD_INTO_HAND_FROM_FORCE_PILE semantic) = K-2's m00582
  recommendation exactly: runtime -> ACTIVATE+CONTROL -> DRAW -> PULL -> objective -> DEPLOY -> BATTLE -> MOVE ->
  SETUP -> interceptor retirement -> deploy-weight/solo tuning -> triple-lock/deploy, 8 lane guards.
- RUNTIME PACKET RELEASED (Codex m00587) sha bc430fee = reviewed ac62724a + status flip + my retry-parity watch-point
  added to gate (verified on disk; baseline ad8f59385). Worker ace3f514 RESUMED on bc430fee (re-read corrected packet;
  6 deltas flagged). Implementing one coherent tranche; K-2 runs §10 focused pass + static proofs, reviews, commits,
  returns §12 evidence for Codex aggregate gate. THEN Phase B (ACTIVATE+CONTROL) draft 72bb2a1f releases post-gate.
- PIPELINED DRAFTS (Codex m00592 sidecar gates, unreleased, review at their turn): Phase B/ACTIVATE+CONTROL a650fbe1
  (ACTIVATE_ACK runs legacy context prep once w/o scoring; zero-confirm V61c = reserve<=3 AND battlePlausible);
  master order f390d1a3; deploy-weight contract 0508bb5c (closed Constraint->IntentRank->BoundedFine comparator,
  narrow V193 exemption, physical transaction cursor separate from lastPendingDeployType, scalar domination math).
  V44/V67j finalizer-owner placement RESOLVED (agent a5cc308d, sent Codex m00593): V44/V67j = the always-accept
  'approve revert' MULTIPLE_CHOICE interceptor (RandoCalAi:655-683, reads NO board state), route
  V44_V67J_REVERT_APPROVAL. NO owner exists (ResponseFinalizer has zero prod callers; runtime seam not landed).
  Standalone: hard-deps ONLY on step-1 runtime + fixture RR_V44_REVERT_REORDERED; NO dep on the 8 lanes; full
  Rando/Chosen parity. Placement: DEFAULT convert-then-delete inside step-10 interceptor retirement (frozen-doc
  faithful); K-2 ACTIVELY RECOMMENDS considering a tiny finalizer-owner PILOT right after step-1/before ACTIVATE+
  CONTROL (lowest-risk end-to-end proof of the decideForEngine->CandidateOrdinal->ResponseFinalizer->adapter->
  post-accept seam; de-risks the cutover given the runtime's 4-correction history). Codex's architect call.
- PILOT ADOPTED (Codex decision m00594): V44/V67j finalizer-owner pilot inserted as step 1b (after runtime,
  before ACTIVATE+CONTROL). K-2 updated the frozen order (CODEX_PHASE_CUTOVER_ORDER new sha 654b3954, sent Codex
  m00595) + DRAFTED the pilot packet; Codex CONFIRMED NONE-mode + FROZE it (m00596) at
  Handoffs/K2_V44V67J_FINALIZER_PILOT_PACKET_DRAFT_2026-07-13.md sha 7f2ee78c (order re-frozen 654b3954). Codex
  correction: production calls PUBLIC ResponseFinalizer.finalize(...) not the private helper; add smallest
  explicit-mode FinalizedResponseAdapter overload + AiDecisionResult invariant (OUTER_COMMON carries tracker
  descriptor; NONE carries/applies none; typed rejection unchanged; existing 2-arg adapter stays OUTER_COMMON).
  Fixtures prove both modes + history forwarding + no fallthrough + reordered labels/ordinal-0 + zero RNG + one
  callback + parity. FROZEN/unreleased until runtime commit passes Codex gate. First ResponseFinalizer phase-owner
  caller; gated only on runtime commit + RR_V44_REVERT_REORDERED; both bots; no game.
  NEW SEQUENCE: runtime -> V44/V67j pilot -> ACTIVATE+CONTROL -> DRAW -> PULL -> objective -> DEPLOY -> BATTLE ->
  MOVE -> SETUP -> interceptor retirement(minus V44/V67j) -> deploy-weight tuning -> triple-lock/deploy.
- PULL DESIGN (Codex m00597, downstream, freezing PULL packet): do NOT add engine-to-AI failed-search callback,
  do NOT reconnect DeckOracle.recordFailedPull/clearFailedPulls — engine's CantSearchCardPileModifier +
  ModifiersQuerying.isSearchingCardPileProhibited already own same-turn failed-search suppression authoritatively
  (AI reconstruction would duplicate a stronger engine rule; modifier expires end-of-turn). SUPERSEDES the
  'reconnect DeckOracle.recordFailedPull' note in the tail-order rationale above. PULL STILL installs the typed
  parent/child/destination transaction (physical source, search identity/filter, selected child, forced destination,
  accepted/canceled/failed/successful outcome) — just not the suppression. Dead DeckOracle maps retire only after caller proof.
- PIPELINE INDEX (Codex m00598): Handoffs/CODEX_PHASE_EXECUTION_MANIFEST_2026-07-13.md sha 8aea92b9 is the durable
  index of exact packet hashes for every phase (runtime, pilot, A+C, DRAW, PULL, objective, DEPLOY, BATTLE, MOVE,
  SETUP, retirement, tuning, final lock). Downstream packets are FROZEN/pipelined, NOT released. DISCIPLINE: read
  ONLY the active packet at each phase; no mailbox/history replay (do not re-mirror downstream hashes in this handoff).
- PHASE 1 (RUNTIME) AMENDMENT READY: Codex m00603 found two P1 lifecycle leaks in 5be61d054. Codex took ownership,
  fixed both, and added the three exact mediator/Rando/Chosen regressions. Focused phase pass: 216/0/0/0, Maven exit 0;
  git diff --check clean. K-2 owns one lean independent review; on PASS amend the same commit and deploy immediately.
  Deferred ActivateControl fixtures remain excluded.
- Steve changed cadence: each independent phase deploys after its one phase gate. Before every reload confirm no live
  game; verify the running artifact afterward. Never push. Then continue directly to the next phase without permission.
- Mailbox watermark: m00616. Codex owns runtime amend/commit/deploy; K-2 owns lean gate plus step 1b pilot preparation.

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
