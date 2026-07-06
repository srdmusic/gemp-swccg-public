# K-2 REORG EXECUTION HANDOFF — 2026-07-06 — for Opus-model sub-agent sessions

You are **K-2** running on Claude Opus (Fable 5 is unavailable to Steve until Anthropic
re-releases it). This file is your COMPLETE brief for executing the Rando logic-tree
reorganization. It was written by the Fable-5 K-2 that produced the plan, after four
multi-agent research runs (~9M subagent tokens: code map 286 branches line-verified,
rulebook decision-window inventory, 4-game census, 54-finding overlap audit with
adversarial verification). You do not need to re-derive any of it. You DO need to
re-verify anchors before editing — the tree moves between sessions.

**Operating stance for Opus sessions:** offload heavy lifting. Use the Workflow tool /
Agent tool for anything that means reading more than ~2 files end-to-end (searches,
audits, boundary sweeps, verification passes). Keep your own context for strategy,
synthesis, and the edits themselves. Consult the council for second opinions on
magnitudes (see §10). Invoke the `work-verifier` skill after every deploy.

---

## 1. Read order (non-negotiable)

1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — auto-loads; every `feedback_*` rule is LAW. The big five: one-change-at-a-time, breadcrumbs-every-fix (BOTH changelogs, same session), check-rule-is-live-before-editing (grep for `if (false`), update-old-rule-not-new-version, verify-before-done.
2. THIS FILE, end to end.
3. `resources/RANDO_REORG_PLAN_2026-07-02.md` — THE plan: architecture §3, rule-KIND doctrine §4, ladder §5, traps §6, resolved decisions §7. This handoff operationalizes it; the plan is the design authority.
4. `resources/BUILD_AND_DEPLOY.md` — before any edit or deploy (the 4 verify gates).
5. As needed: `Handoffs/K2_MASTER_HANDOFF_2026-07-02.md` (repo-wide state, landmines, doc map — its "current state" §1 is now stale below HEAD `8392f3868`; git is truth).

## 2. Ground truth at handoff time (HEAD `ee0a1b435`, 2026-07-06)

- Branch `rando-consolidation-2026-06-23`, local only, NOTHING pushed to GitHub (standing order).
- Shipped since the plan was written (2026-07-02 → 07-06), all with changelog entries:
  - `fcec408b9`+`d58db0275` — **TDIGWATT bug A DONE**: V177 category rescue (consults V67h validator before declaring a search DEAD), LIVE-VERIFIED via self-play.
  - `88b5170de`+`cb98f0075` — **V189**: net-value drain gate (net −1 drains budget-gated on turn spend forecast) + V140 false Battle-Plan waiver repaired.
  - `41651dab8` — **V190**: starships deploy to systems, not docking bays (both bots + DeckOracle).
  - `ee0a1b435` — **V51/V105/V112/V117 updated in place**: Battle Order 4th-slot deadlock fix + occupation-predicate unification.
- ALL commits above are IN HEAD `ee0a1b435` — do NOT re-implement them. The only remaining T0.1 code work is TDIGWATT bug B and the V61c battle-intent bypass.
- STILL QUEUED from the old bug workstream: **TDIGWATT bug B** (V29 BESPIN-FIRST demanding a forbidden Executor — see `Handoffs/K2_HANDOFF_2026-07-02_fable5-onboarding.md`) and the **V61c battle-intent bypass** (Steve-approved plan, same file, § "ALSO QUEUED").
- Untracked working artifacts (do not lose): `resources/RANDO_REORG_PLAN_2026-07-02.md`, `resources/Rando_Overlap_Audit_2026-07-04.xlsx`, `resources/Rando_Version_Table_2026-07-01.xlsx`, `resources/Rando_Consolidation_Plan_2026-06-29.xlsx`, `resources/Rando_Issues_2026-06-29.xlsx`, `mcp-gemp-client/gemp_mcp.py`. Ask Steve about committing docs when convenient.
- `.agents/` + `AGENTS.md` = Codex sandbox with known bugs. NEVER merge into `.claude/`.

## 3. The artifact inventory you inherit

| Artifact | Role |
|---|---|
| `resources/RANDO_REORG_PLAN_2026-07-02.md` | The design authority: architecture, rule-KIND axis, ladder, traps |
| `resources/Rando_Version_Table_2026-07-01.xlsx` | 301 rules: issue / fix / dead-status. Freeze + regenerate at T0.4 |
| `resources/Rando_Overlap_Audit_2026-07-04.xlsx` | 54 combine/overlap/contradict findings, adversarially verified (30 confirmed, 20 high). Sorted confirmed-high first; gray rows refuted |
| `resources/Rando_Consolidation_Plan_2026-06-29.xlsx` | The original 8 moves + 3 council traps (absorbed into the ladder; kept for detail) |
| Artifact "Rando Logic Tree" (claude.ai/code/artifact/307bbf11-4339-4f6e-be7e-adbfa1e3deb3) | The v1 diagram. Needs a v2 update after T0 (see §12 backlog) |
| `resources/AI_CHANGELOG.md` + `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md` | THE changelogs. Every step updates BOTH, same session |

## 4. Architecture (compact — full detail in plan §3)

**PATHS:** every evaluator path in this file is relative to
`src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/`.
`rando/` = the primary brain you edit; `chosenone/` = the mirror (every rando fix is
mirrored there); `common/` = shared by BOTH bots (banners safe, code motion needs Steve).
**ANCHORS:** all `~line` numbers were verified 2026-07-06 but the tree moves — re-anchor
by grepping the V-tag or the quoted log string, never trust a bare line number.

Spine: SETUP · START-OF-TURN · ACTIVATE · CONTROL (promoted: drain-order / retrieval-gap / card-text dispatch) · DEPLOY-1 sequencing+budget · DEPLOY-2 siting (hub V136) · DEPLOY-3 weapons/pilots (hub V158) · BATTLE-1 initiation · BATTLE-2 weapons-segment window · BATTLE-power checklist · BATTLE-3 damage+forfeit (hub V159) · MOVE · DRAW · END-OF-TURN.
Router: RESPONSE (thin dispatcher; deep sections get callable entry points).
Engines (cross-phase): PULL-ENGINE · FORCE-LOSS (hub V153) · SHIELDS.
Overlay: PLAYBOOKS (primary home for ~40 deck tags, phase-organized internally, back-pointers in phase sections).
Services (facts only, never scores): SVC-ORACLE · SVC-INTEL · SVC-SAFETY (+obligations lane, +no-pass flag, +keyword-fallback ownership).
Rule-KIND axis on every rule: VETO / ORDERING / BANDED-SCORE (plan §4 — this is the anti-dominance mechanism).

## 5. THE LADDER — expanded into executable steps

One ladder step per session. Every step ends with: code comment breadcrumbs, BOTH
changelogs updated, one commit staging ONLY your files, `work-verifier` invoked,
and the §11 status table in this file updated. Never batch steps.

### T0 — RECONCILE + REBASELINE (do this before anything else)

| Step | What | Done when |
|---|---|---|
| T0.0 | RECONCILE the audit vs HEAD: the 07-04/07-06 commits (V189, V190, V51/V105/V112/V117, V177) may have addressed audit findings. Re-verify at minimum: control-drain-1, control-drain-2 (vs `cb98f0075`/`88b5170de`), shields-response-1/-2/-3 (vs `ee0a1b435`), deploy-sequencing findings touching V177. Mark addressed rows in the audit xlsx (new column "Status @HEAD"). Offload: one verification agent per finding | Audit xlsx annotated; summary table to Steve |
| T0.1 | Ship remaining queued bug work FIRST (same files the reorg banners touch): TDIGWATT bug B (V29 BESPIN-FIRST release when objective forbids Executor — mirror chosenone), then the V61c battle-intent bypass (shared predicate across ForceActivationEvaluator ~186 + ActionTextEvaluator V168 ~166 + V38.3 confirm arm ~1374 (grep "MUST ACTIVATE"; second arm ~5040) — all three sites ONE predicate, bias toward keeping 3 when unsure). Boundary math first for both, committed as an artifact (in the changelog entry or a resources/ file) so work-verifier can confirm it exists | Both live-verified in self-play; changelogs; boundary tables committed |
| T0.2 | Ask Steve: queue the five confirmed audit bugs (§7) into the bug workstream now or after T1? Add chosen ones to `resources/Rando_Issues` tracker (new dated xlsx) | Steve's pick recorded |
| T0.3 | Single-owner manifest: every LIVE tag → exactly ONE destination section (~186 live tags; 301 = version-table total incl. ~25 dead — dead tags stay in the version table only). The section clusters ARE the §4 spine; the §8 multi-arm table is the authoritative arm→section source — seed the manifest from §8 first, then sweep the remaining single-home tags. Record the grep hit count per tag per file and assert every hit maps to a labeled arm; ANY unassigned hit blocks code motion for that tag. Offload: one agent per §4 section; you merge. Deliverable: `resources/Rando_Section_Manifest_<date>.xlsx` | All ~186 live tags assigned; hit-count assertion passes; Steve eyeballs it |
| T0.4 | Rule-KIND classification pass (VETO/ORDERING/BANDED per rule) — add column to the manifest. Then regenerate the version table from post-fix HEAD; mark the 07-01 xlsx frozen | Manifest has KIND column |
| T0.5 | Top-N candidate logging: each decision logs its top surviving candidates with scores (instrumentation only; the dominance-regression detector every later gate uses). Small code change in CombinedEvaluator max-pick + RandoCalAi fallback path. CRITICAL: log from a COPY of the candidate collection — never sort or mutate the live structure (reordering can flip tie-breaking in the max-pick). Then run the §6.5 identical-scores gate on T0.5 ITSELF (2 games) before T1 starts — every later gate trusts this code | Lines in `logs/gemp-swccg.log`; T0.5's own neutrality gate passed |
| T0.6 | Jar↔tree parity check: byte-verify HEAD classes in the running `web.jar` (BUILD_AND_DEPLOY §3 extract-to-file method) | Parity confirmed or reconciled |

### T1 — HUB BANNERS (zero score motion)

| Step | What |
|---|---|
| T1.1 | Section banner comments + tag ledger at the four live hubs: V136 → DEPLOY-2 (`common/strategy/CharacterDeploySiteEvaluator.java` — banners only, file is shared with chosenone), V158 → DEPLOY-3 (`rando/evaluators/DeployEvaluator.java` ~3947, grep "UNIFIED WEAPON DEPLOY GATE"), V159 → BATTLE-3 (`rando/evaluators/CardSelectionEvaluator.java` ~9103, grep "v159ForfeitScore"), V153 → FORCE-LOSS (same file ~3970 + ~4857, grep "V153"; note the byte-identical-mirror constraint) |
| T1.2 | Fix the lying V67al comments (DeployEvaluator ~1868 + ~1918, inside the V96 CONCENTRATE block — they describe dead V67al as "currently penalizes"/"stays in effect"; the actual V67al code at ~3833 is nested inside the dead `if (false /* V67aj SUPERSEDED V136 */)` at ~3764) — comment text only |
| T1.3 | Split-ownership comments in `CombinedEvaluator.java` (DPS walk lines → DEPLOY-1; V148/all-bad + threshold → SVC-SAFETY) and in `RandoCalAi.java` keyword tables (→ SVC-SAFETY ownership, FROZEN until T4) |
| Gate | Self-play 2 full games: decision log diff shows IDENTICAL SCORES on comparable decisions (not just identical winners — the −100 threshold makes drift matter); jar byte-verify |

### T2 — SINGLE-HOME SECTIONS (still score-neutral; order within tier is free)

DRAW (+ move #1 DTF/maintenance cache — TWO commits, in order: FIRST the audit force-economy-5 fix as its own BUG commit with boundary math (the V58 DTF copy lacks the `isInPlay()` gate the other four have — a behavior change, NOT score-neutral, and the assert-equal soak would fail against the un-fixed copy), THEN the neutral cache consolidation with the assert-equal soak) · CONTROL dispatch docs (name the retrieval gap; do NOT build retrieval scoring yet — that's post-reorg backlog) · SHIELDS (+ move #3 `fourthShieldBlocked()` helper — reconcile with `ee0a1b435` first, it may already exist) · SETUP · SVC-INTEL docs (mark ObjectiveHandler dead, ActionAudit + shouldInitiateBattle dormant) · BATTLE-power checklist doc · START/END-OF-TURN slot naming · SVC-SAFETY docs + obligations lane + no-pass flag. Move #2 `canWinAt()` helper lands in DEPLOY-2 (see audit move-4: port V181's gap/drain/parity tolerance into V137 via the SAME shared predicate — that one IS a scored change, so it graduates to T4 if the boundary table shows decision flips).
Gate per section: identical-scores diff + section's tags observed firing in 2 full self-play games.

### T3 — SCATTERED SECTIONS (score-neutral, FIXED order)

1. PLAYBOOKS first (its ~40 tags are cuts through other sections' methods — second movers otherwise find code already moved). 2. BATTLE-1 + BATTLE-2 (preserve the BattleEvaluator + ActionTextEvaluator V25 initiation SUM). 3. ACTIVATE (post-T0.1 bypass, triangle re-baselined). 4. DEPLOY-1 orchestration. 5. MOVE formalize (labels only, no ladder). 6. RESPONSE router.
Mechanical rules per extraction: carry every early-return guard above the extracted rule (a bare `return` today suppresses everything downstream — extraction converts "suppressed" into "summed"; see audit control-drain-1 for the live example); actionId strings FROZEN; canEvaluate signatures FROZEN; log strings FROZEN (add lines, never alter — census tooling parses them); routing-stability check (evaluator-vs-keyword-fallback decision counts unchanged before/after).

### T4 — MAGNITUDE MERGES (last, one per build, boundary table to Steve first)

1. MOVE clobber ladder (dual-utility semantics: rank compared first, additive only within rank; HIDDEN_PATH_MANDATORY above STAY_AND_CRUSH). The audit's six confirmed-high MOVE contradictions (move-1/2/3/4/7/8) are the ladder's acceptance tests — each must resolve correctly.
2. PULL-ENGINE merge (old moves #4+#8): keep the V29.9/V29.11 nested guards; ONE pull scorer must stay above V168's +5000 in the activate window (feedback_pull_before_activate); fix audit deploy-sequencing-1 (V82 +2500 sits ABOVE the V60 reserve≤2 hard-block) and deploy-sequencing-2 (V67ai tier table implemented twice, sums to +3600) INSIDE this merge — they are the same pile.

## 6. Verification protocol (honest version — there is NO deterministic replay harness)

1. Fast syntax check in-container: `docker exec gemp_swccg_app_1 bash -c "cd /opt/gemp-swccg/src && mvn -q -pl gemp-swccg-server -am compile"` (compile-only, server module). The DEPLOY rebuild is different: `bin/gemp reload-ai` runs `mvn -pl gemp-swccg-async -am package -DskipTests` — both are correct, for different jobs.
2. Deploy = `bin/gemp reload-ai` (rebuild/rebuild-fast do NOT restart the JVM). Flip ALL gameplay switches after every restart (login `asdf`; shutdown=false; aitables/privategames/stattracking/newaccounts=true).
3. Byte-verify your new log string in `src/gemp-swccg-async/target/web.jar` (python zipfile extract-to-file, BUILD_AND_DEPLOY §3).
4. Self-play soak: 2 full-length games minimum (short games have near-zero BATTLE decisions — census fact). Read `logs/gemp-swccg.log`; rotated: `logs/YYYY-MM/*.log.gz` via `gunzip -c` (macOS zcat broken).
5. Score-neutrality gate, concrete recipe (there is no deterministic replay, so matching is by signature): key each decision by phase + actionId + the T0.5 top-N candidate signature (candidate set + scores); compare the MULTISET of per-signature score vectors before vs after; identical multisets = pass. Save the diff artifact to `resources/` so work-verifier can audit it. IDENTICAL SCORES, not identical winners (the −100 threshold makes drift matter). Before T0.5 lands, the gate is winner-score + reasoning-string only (the log's `Evaluator decision` / `Best action` lines) — best-effort, say so in the gate report.
6. Cold-tag gate (mechanical, not honor-system): each section's gate report ENUMERATES the section's tags with fired/cold status from the soak logs. A section with cold tags is NOT done until EITHER a targeted-deck self-play run exercises them OR Steve's explicit sign-off is recorded in the §11 status row.
7. Invoke the `work-verifier` skill (Agent tool) before telling Steve "done".

## 7. The five confirmed audit bugs (independent of the reorg — Steve decides queue order at T0.2)

| Audit ID | Bug | Anchor | Fix sketch |
|---|---|---|---|
| move-7 | V35.4 spy detection ownership-INVERTED: our own undercover spy at an opponent drain site gets +250 on every move action — pays our spy to abandon its post | ActionTextEvaluator ~3510-3550 (grep `Move using`; +250 at ~3538; the inverted owner test at ~3524) | Test owner==opponentId && isUndercover; scope to the MOVER's location |
| move-8 | V47 locks ANY "lando" title at ANY site whose title contains "platform" (any planet) at −9999; no objective/danger gate | MoveEvaluator ~348-382 | Gate on CC-objective active + survivability; drop generic "platform" substring |
| cross-brain-1 | V169: blocked-move soft penalties stack across evaluators and the `continue` skips its own retreat bonuses — blocked retreats can never be re-attempted (recreates the Asajj incident it was built to fix). CAUTION from QA fact-check: the −800 ctor+addReasoning double-add detail did not reproduce at HEAD (visible code shows a single −400 addReasoning) — re-verify the arithmetic before fixing; the cross-evaluator stacking + `continue` skip ARE confirmed | MoveEvaluator ~165-192 + ActionTextEvaluator ~118-150 (grep V169) | One shared blocked-gate predicate; recompute the stack at HEAD first |
| deploy-sequencing-1 | V82 SITE PULL +2500 lands even when V60 Guard-1 (reserve ≤ 2) hardBlocks — Rando reveals his last 2 reserve cards | ActionTextEvaluator pull branch (V82 block above the `if(!hardBlocked)` region) | Move V82 inside the guard; align Guard-1 magnitude with the DeployEvaluator copy |
| deploy-sequencing-4 | Into-hand pulls are unpickable on any bucketed deploy decision: DPS excludes them from buckets, bucket exhaustion returns PASS | CombinedEvaluator ~97-180 + DeployPhaseScript ~224 | Epilogue to the bucket walk: consider positively-scored non-bucket actions before returning PASS |

(All five were adversarially confirmed with recomputed arithmetic on 07-04 code; re-verify anchors vs HEAD per T0.0 before editing. Mirror every fix to chosenone.)

## 8. Multi-arm tags — sub-label BEFORE any code motion (move ARMS, never grep-and-move a tag)

| Tag | Arms (→ section) |
|---|---|
| V61 | saga pick RandoCalAi:647 (→SETUP/PLAYBOOKS) vs battle reserve guard BattleEvaluator:622 (→BATTLE-1) |
| V22 | starting-effect (→SETUP), objective-location bonus (→DEPLOY-2), must-fight (→BATTLE-1), post-flip protect (→PLAYBOOKS) |
| V25 | Hunt Down detector (→SVC-INTEL), power-tier initiate (→BATTLE-1), pilot lock (→MOVE), Simple-Tricks drain arm (→CONTROL), lightsaber loss-protect (→FORCE-LOSS), concede arm (→SVC-SAFETY) |
| V29.x family | V29 BESPIN gate (→PLAYBOOKS), V29 move force-reserve (→MOVE), V29.7 pull/weapon/retreat/flip arms, V29.9 Barrier-risk vs Hunt-Down-aggro, V29.12 hunter, V29.13 drain-delta + grouping |
| V33 | live named-weapon priority (→DEPLOY-3) + live move buddy-break (→MOVE); weapon hard-block arm is DEAD in V158 |
| V35.x | Hatred lifecycle (→BATTLE-2), V35.4 spy rules ATE ~3510-3550 (→MOVE, buggy — §7), Inquisitor destiny (→BATTLE-1) |
| V37 / V37.1 | high-value protect (→BATTLE-3), BG-to-nonBG block (→MOVE), hatred timing (→BATTLE-2), hard-stay (→MOVE) |
| V38.3 | activation confirm ATE ~1374 + ~5040 (→ACTIVATE) vs wrong-direction move block (→MOVE) |
| V42 / V43 | activation amount (→ACTIVATE) vs emergency draw (→DRAW) / min-1 floor (→ACTIVATE) vs redundant-shield skip (→SHIELDS) |
| V48 / V51 / V52 / V53 | drain-amount vs move-budget / drain-contest vs targeting vs Battle-Order-gate / momentum vs drain-priority vs self-cancel / spy-follow vs shield-priority |
| V60 | Hidden Path transit (→MOVE) vs pull baseline + guards (→PULL-ENGINE; SAME-TAG drift: different magnitudes in ATE vs DeployEvaluator — audit deploy-sequencing-3) |
| V169 / V170 / V178 / V24.10 / V64 / V79 | deploy-protect vs move-retreat vs loop-soft-block / spy siting vs cover decision / forfeit vs force-loss arms / AMSD script vs Piett-dig / protect vs urgency / V79 INERT parse vs V79b live steering |

## 9. Traps ledger (the ones that have burned prior sessions, plus audit additions)

- Additive dominance: CombinedEvaluator SUMS per actionId; "score-neutral" = identical scores. Do boundary math BEFORE code. Four historical burns + audit control-drain-1 as the live specimen.
- Check rule liveness first: ~25 tags are compiled out or //-commented (version table column D). Editing dead code ships nothing (the V67al day-loss).
- Parity pairs, same commit: V179↔V67ai keyword lists; V136↔V137 winnability; V153's two zone-order copies; V61c buffer constant ↔ V61 battle guard.
- Dead code travels WITH its hub (it is the revert path). Comment out, never delete.
- `common/` files serve BOTH bots; `chosenone/` mirrors every rando fix.
- Early-return extraction hazard; actionId/canEvaluate/log-string freezes (§5 T3).
- Dual-brain: RandoCalAi keyword tables are a second brain on an int scale — frozen until T4.
- Temp-id trap in CardSelectionEvaluator.evaluateDeployLocation: resolve blueprints via `context.getBlueprints()`.
- The scratchpad wipes between turns; task outputs under `/private/tmp/claude-501/.../tasks/` are ephemeral — anything durable goes in `resources/` or `Handoffs/`.
- NEVER: `docker compose down -v`, `rm -rf database/`, `bin/gemp reset-db`, unpin `mariadb:11.8.6`, push to GitHub, edit the deck library/DB schema without Steve.

## 10. Tooling for an Opus session

- **Council** (second opinions on magnitudes/architecture; hallucinates card text — verify vs code or `mcp-gemp-client/card_cache.json`): the :8000 FastAPI bridge was DOWN at last check; go direct to Ollama. Model tag on this box is `deepseek-r1:70b-llama-distill-q8_0` (the short `deepseek-r1:70b` errors). `curl -s --max-time 570 http://127.0.0.1:11434/api/generate -d '{"model":"deepseek-r1:70b-llama-distill-q8_0","prompt":"...","stream":false}'`. ~4 min per call. Council CANNOT read files — paste excerpts.
- **Workflows/agents**: fan out per-cluster (the research runs used 6-8 hunters + adversarial verifiers; that pattern works). Verify agents' file:line claims before acting — agents drift.
- **Codex/"Alfred"** (`mcp__codex__codex`): third-family opinion; was usage-capped until ~Jul 29 2026 — check before relying on it.
- **Census method** (re-runnable): every `decide() called` log line carries `phase=X`; V-tag fires via `grep -oE 'V[0-9]+[a-z.0-9]*'`. Beware co-occurring tags on one line (V105/V107) and per-candidate log inflation (V29.5).
- **xlsx**: openpyxl (no pandas on this box). House style: header fill 1F3864, Arial 10, freeze A2, autofilter, wrap.

## 11. Status table — update at the end of EVERY session (this file is the baton)

| Step | Status | Session note |
|---|---|---|
| T0.0 reconcile audit vs HEAD | ✅ 2026-07-06 | 9 findings re-verified at HEAD `8f841bd25` (audit xlsx col M): control-drain-1 PARTIALLY ADDRESSED by V189 (net-0 fall-through survives, sev→low); shields-response-2 PARTIALLY ADDRESSED by ee0a1b435 (ShieldStrategy trigger-A copy remains, sev→low); 7 STILL VALID — control-drain-5 + deploy-sequencing-1 stay HIGH; control-drain-4 WIDENED (V189 protections are evaluator-brain only, fallback brain unprotected). Other 41 non-refuted rows: cited code untouched since audit (diff-checked). §7 five-bug list unchanged and still valid |
| T0.1 TDIGWATT B + battle-intent bypass | ☐ | bug A already DONE (`fcec408b9`) |
| T0.2 queue the 5 audit bugs (Steve) | ☐ | |
| T0.3 single-owner manifest | ☐ | |
| T0.4 KIND column + version-table regen | ☐ | |
| T0.5 top-N logging | ☐ | |
| T0.6 jar↔tree parity | ☐ | |
| T1.1–T1.3 hub banners | ☐ | |
| T2 sections (8) | ☐ | |
| T3 sections (6, fixed order) | ☐ | |
| T4.1 move ladder | ☐ | |
| T4.2 pull-engine merge | ☐ | |

## 12. Backlog after the ladder (do NOT bundle into reorg steps)

Diagram v2 update (artifact URL in §3) · retrieval scoring rules (CONTROL-2b) · turn-posture object · battle-intent-at-deploy scoring · move hysteresis · re-entrant handlers + persistent plan object · remaining audit confirmed-medium findings (work the xlsx top-down).

---

Session protocol: one ladder step per session. Open with `git log --oneline -5` +
`git status` + read §11. Close with commits, changelogs, §11 updated, and a 3-line
summary to Steve in table form. He has ADHD + dyslexia: concise, single-layer, tables.
Push back when he's wrong. No em-dashes in inline prose. May the Force be with you —
statistically, it favors whoever did the boundary math.
