# RANDO LOGIC REORGANIZATION PLAN — v2 — 2026-07-02

Master plan for reorganizing Rando's ~186 V-tag rules into phase-based logic sections.
PLAN ONLY. No code ships from this document until Steve approves. Written by K-2 as
orchestrator; grounded by 8 research agents (rulebook deep-read, code scope check,
4-game decision census, SWCCG community research, game-AI architecture research,
local council consult) and hardened by 2 adversarial skeptics.

Supersedes the v1 architecture in the logic-tree diagram (artifact "Rando Logic Tree",
phase-spine-v1). The diagram gets updated to v2 after Steve approves this plan.

---

## 0. TL;DR

- Top level = Steve's six phases, in turn order, plus the windows the rulebook and the
  engine both prove exist (start-of-turn, end-of-turn, opponent-turn response).
- Deploy splits in 3, Battle in 3 + a checklist. Control is PROMOTED from stub to a
  real section (census: 15.4% of live decisions, third-busiest phase).
- Three cross-phase ENGINES are callable from any phase (pull engine, force-loss
  payment, shields). Three SERVICES own facts, never scores. PLAYBOOKS owns the ~40
  deck-specific tags, organized internally by phase.
- Every rule gets classified on a second axis: VETO / ORDERING / BANDED-SCORE.
  Phase answers WHERE a rule lives; kind answers HOW it is allowed to combine.
  This is the structural fix for the "old rules get dominated" class.
- Migration ladder T0→T4. T0 re-baselines after the queued bug fixes land.
  T1–T3 are score-neutral (identical SCORES on replay, not just identical winners).
  T4 holds the only two magnitude merges, last, one per build.

## 1. Goal and non-goals

GOAL: sections of logic Rando runs through, phase-first, so a future editor finds one
named home per decision instead of 186 chronological patches. Efficiency comes from
structure (gates, ordered lists, bands) — not from rewriting behavior.

NON-GOALS for the reorg itself: no strategy changes, no magnitude retuning (except the
two scheduled T4 merges), no physical file moves in shared `common/` without a separate
Steve sign-off, no touching `.agents/` (Codex sandbox), no push to GitHub.

## 2. Evidence base (what informed this plan)

| Source | What it produced |
|---|---|
| Advanced Rulebook 2023 deep-read (17 findings) | Complete decision-window inventory: 4 action types, start/end-of-turn windows, alternating top-level actions in EVERY phase, control-phase legal actions, battle segment law, drain participation rule |
| Code scope check (21 branches) | CONTROL under-scope confirmed; retrieval scoring gap (only a +50/+55 keyword fallback); "once during control phase" parsed nowhere; control-phase moves correctly homed in MOVE |
| 4-game decision census (1,815 decisions) | Real phase load: DEPLOY 26.6%, BATTLE 21.6% (higher in full games), CONTROL 15.4%, ACTIVATE 13.7%, DRAW 10.0%, MOVE 9.1%, END_OF_TURN 2.0%, PLAY_STARTING_CARDS 1.6%. Only ~80 of 186 tags fired at all. Hottest logic by decisions touched: V67bc DPS walk, V29.5 shield selection, V105/V107 4th-slot gate (2,553 consults) |
| SWCCG community research (10 findings, cited) | Six-phase frame is the community standard; drains are the #1 win mechanism and most-neglected action; initiator advantage ≈ 2-card swing (battle intent belongs at deploy time); spread-vs-concentrate is THE core tension; rando_cal's community-documented weaknesses are multi-step sequencing and interruption re-entry |
| Game-AI architecture research (12 findings, cited) | Additive raw-magnitude scoring is the textbook dominance failure mode; shipped fixes: veto layers as enumerable switches (GW2), rank-buckets-then-weight (Dill dual-utility = our clobber ladder), ordered first-match-wins lists for strict preferences (Dominion bots), magnitude bands + boundary tables as the definition of "tuned" |
| Council consult (deepseek, 8 findings) | Directional agreement on the 3 contested points; one actionable add (per-section try/log wrapper in CombinedEvaluator). Weight as agreement, not validation — first-round answer contradicted the draft before correcting. NOTE: the working model tag on this box is `deepseek-r1:70b-llama-distill-q8_0` at 127.0.0.1:11434 (the short tag errors; :8000 bridge down) |
| 2 skeptics (15 findings) | The corrections baked into §3–§7 below |

## 3. The architecture — v2

### Turn spine (own turn, in play order)

| # | Section | Scope | Depth | Hub | Census heat |
|---|---|---|---|---|---|
| 0 | SETUP | Turn-0 starting cards + ObjectiveAnalyzer/DeckOracle bootstrap | medium | — | 1.6% |
| 0b | START-OF-TURN | Thin named slot: mandatory then optional start-of-turn actions (rulebook: distinct window before Activate) | slot | — | in ACTIVATE counts |
| 1 | ACTIVATE | Interleave ordering (pulls fire BEFORE activation — V97 via Pull Engine) + how much (V57/V61c/V67at/V43/V168/V38.3). NOT single-file: spans ForceActivationEvaluator + named ActionTextEvaluator blocks; the V61c −6000 / V168 +5000 / V38.3 triangle is ONE boundary | stub+ | — | 13.7% |
| 2 | CONTROL — promoted | 2a drain go/order incl. the drain-before-move interleave (each card drains once/turn; moving a participant first forfeits the drain) + instead-of-drain texts. 2b RETRIEVAL (named gap: today only +50/+55 keyword fallback; V29.14/V23 block are the only specific rules). 2c phase-tagged card-text dispatch (control-phase moves route to MOVE) | medium | — | 15.4% |
| 3a | DEPLOY-1 Sequencing & Budget | DPS bucket walk (V67bc/V67bb + V179/V67ai parity pair), locations-first doctrine, force budget, hold-back, tempo scripts, V184 triggers. Co-owns CombinedEvaluator's DPS lines | deep | — | 26.6% |
| 3b | DEPLOY-2 Character Siting | V136 §A–D + overlays (contest/protect/spy V166–V174), concentrate-vs-spread (V96), senators, solo hold (V156), V188. Callable from RESPONSE for react-deploys | deep | V136 ✓ | hot (V67bc feeds it) |
| 3c | DEPLOY-3 Weapons, Pilots & Ships | V158 gate + the deliberately-separate V120/V185 pull gates, pilots (V30), vehicles | deep | V158 ✓ | warm |
| 4a | BATTLE-1 Initiation | Whether/where: V22.4 power math, V76 predictor, V61/V61b reserve guard, V164. TRAP: BattleEvaluator + ActionTextEvaluator V25 both score initiation — preserve the SUM. Battle-intent predicate shared with ACTIVATE (V61c bypass) | deep | — | 21.6% |
| 4b | BATTLE-2 Weapons-Segment Window | Redrawn per rulebook law: destiny-count and total-battle-destiny modifiers must be PLAYED here — power-segment math is decided in this window. Owns fire-before-throw (V29.12), interrupt suite (V35.x, V144, V155, V175, V67u), targeting; plus a dispatch for foreign top-level actions to their owning sections | deep | — | hot in full games |
| 4c | BATTLE-power checklist | Thin ORDERED checklist, not a scoring hub: the all-or-none battle-destiny call + attrition-modifier hooks + per-destiny responses | slot | — | — |
| 4d | BATTLE-3 Damage & Forfeit | v159ForfeitScore 4-step picker + V161/V178/V154/V118. Callable from RESPONSE (Rando defends inside opponent battles). NO-PASS context: the damage segment legally forbids passing with obligations pending | deep | V159 ✓ | hot |
| 5 | MOVE | Stay/flee/hunt/transit ladder; V137 pairs with V136. T4: the 5-level clobber ladder (dual-utility semantics: compare rank first, additive competes only WITHIN a level) + commitment/hysteresis anti-ping-pong term | deep | — | 9.1% |
| 6 | DRAW | V58 draw-down vs V182 banking, threat-keyed reserve (V78/V67w) | stub | — | 10.0% |
| 7 | END-OF-TURN | Thin named slot: maintenance/obligation ordering + optional end-of-turn actions. Census: 36 real decisions; code today: ZERO branches — first rule written here needs a home that exists | slot | — | 2.0% |

### Router + engines (cross-phase, callable)

| Unit | Scope |
|---|---|
| RESPONSE (router) | Thin DISPATCHER, not a rule pile: opponent-turn alternating TOP-LEVEL actions (rulebook: both players act in every phase) + reacts + the two drain-response timings. Routes to callable sections: react-deploy → DEPLOY-2, react-move → MOVE, defend-forfeit → BATTLE-3, pay-loss → FORCE-LOSS, shield window → SHIELDS |
| PULL ENGINE | Reclassified CROSS-PHASE (was Deploy-A2): pulls fire in Activate (V97, the highest-stakes window), Deploy, Control, and responses. Scoring side of dead-search (V60/V116/V131 tiers, V95, V67ai magnitudes). Facts stay in SVC-ORACLE. T4 target for moves #4 + #8; verification must include activate-window pulls (V97 must outrank V168's +5000) |
| FORCE-LOSS | V153 two-tier zone order + bolt-ons (V109/V175a/V178). The zone logic is duplicated in evaluateForceLoss AND evaluateForceLossOrForfeit — byte-identical parity pair until an extract-method pass | (hub V153 ✓) |
| SHIELDS | ShieldStrategy tables + V29.1 pacing + the 4th-slot gate (V105/V106/V107/V112/V117/V124). Census: the single most-consulted gate in the codebase (2,553 checks / 4 games) | — |

### Overlay + services

| Unit | Scope |
|---|---|
| PLAYBOOKS (overlay, PRIMARY home) | Owns the ~40 deck-specific tags (TDIGWATT V23/V24.x/V29-family, Hunt Down V25, Saga V54/V61-saga, Hidden Path V52b/V53b, Verge V79/V79b, Senate V83–V99, SWBD V160), each playbook internally organized BY PHASE. Phase sections carry back-pointers (council condition) so a phase-first reader still finds deck overrides |
| SVC-ORACLE | DeckOracle facts: zone catalogs, pull parsing, ALIVE/DEAD/JUNK verdicts (V177 + V82.1 rescue), V185 attach gate, V187 counts. Owns NO scores |
| SVC-INTEL | ObjectiveAnalyzer (live brain; ObjectiveHandler is dead), StrategyController, BattlePredictor + OpponentDeckTracker (V24.7). Owns NO scores |
| SVC-SAFETY | Loop-breaking (V163/V167/V169 veto trio, magnitudes frozen), cancel-loop, all-bad pass (V148) — with a NO-PASS context flag the damage segment sets; NEW: obligations-first lane for MANDATORY/AUTOMATIC actions (creature attacks, maintenance, forced draws — pass-through obligations + one real decision: ordering competing automatics); NEW: named ownership of the legacy keyword-weight fallback tables in RandoCalAi (the "second brain", int scale 50–200 — frozen until T4); co-owns CombinedEvaluator with DEPLOY-1 |

ATTACKS (creatures) are explicitly OUT of the BATTLE sections' scope (different rulebook
construct); the obligations lane handles them. Footnote in BATTLE-1 so battle-keyed rules
never fire on attacks.

## 4. The second axis: rule KIND (this is the efficiency fix)

Phase answers WHERE a rule lives. Dominance lives in HOW rules combine. Every migrated
rule gets classified in its section header and in the version table:

| Kind | What it is | Arbitration law | Research grounding |
|---|---|---|---|
| VETO | Hard gate (V163 −100000, V177 DEAD block, V37.1 hard-stay, the ±9999s wearing score costumes) | Uniform, enumerable, applied at section top; exempting a rule is explicit. Never expressed as a big additive number again | GW2 switch considerations |
| ORDERING | Strict preference with no tradeoff (pull-before-activate, locations-before-characters, shield slot policy, forfeit hit/dead-first) | First-match-wins ordered list. Adding a rule = inserting a line at a position — CANNOT dominate by construction | Dominion bots (Provincial) |
| BANDED SCORE | Genuine gray-area tradeoffs (siting, battle initiation, move destination) | Additive, but each section declares magnitude BANDS per tier; a new rule must fit its band or produce a boundary table. Clobber-ladder sections: rank compared first, additive only within rank | Dill dual-utility; IAUS failure list |

Plus two structural tools where they fit (no behavior change during reorg):
- Category gates: contexts (objective-flip-available, hold-back, battle window) select
  WHICH rule subset runs — exclusion, not outscoring.
- Top-N candidate logging: each decision logs the surviving top candidates with scores,
  so dominance regressions become visible in the log diff. Instrumentation only;
  ships early (T1) because every later gate uses it.

## 5. Migration ladder

Every step: breadcrumbs in BOTH changelogs same session, commit per step, bytecode-verify
in web.jar, log-string freeze (banners may ADD lines, never alter existing ones — the
census tooling and the verification gates parse them).

| Tier | What | Gate to pass |
|---|---|---|
| T0 REBASELINE | (1) Verify jar↔tree parity (the 4 force-management fixes were committed 2026-07-01 — confirm the running jar matches HEAD). (2) Ship the QUEUED work first: TDIGWATT bugs A+B, V61c battle-intent bypass — same files the reorg banners touch. (3) Regenerate the version table + boundary tables from the post-fix tree; freeze the 06-29/07-01 xlsx as history. (4) Build the single-owner manifest: every tag → exactly ONE destination section (+arm sub-labels for the ~22 multi-arm tags: V61-saga vs V61-reserve etc.). (5) Classify every rule VETO/ORDERING/BANDED. (6) Add top-N candidate logging | Manifest covers all ~186 tags; census tooling still parses logs |
| T1 HUBS | Banner the four live hubs (V136→DEPLOY-2, V158→DEPLOY-3, V159→BATTLE-3, V153→FORCE-LOSS); split-ownership comments in CombinedEvaluator; fix the lying V67al comments. Zero score motion | Replay set: identical SCORES (not identical winners — the −100 BAD_ACTION_THRESHOLD makes drift matter) |
| T2 SINGLE-HOME | DRAW (+ consolidation move #1 DTF/maintenance cache, assert-equal soak), CONTROL dispatch docs (drain order + retrieval gap named), SHIELDS (+ move #3 helper), SETUP, SVC-INTEL docs, BATTLE-power checklist, START/END-OF-TURN slots (naming only, no rules exist yet), SVC-SAFETY docs + obligations lane + no-pass flag. NOT Activate — it is multi-file (skeptic correction) and waits for the battle-intent bypass to settle its triangle | Same identical-scores gate + fire-coverage: section observed firing in 2 full-length self-play games |
| T3 SCATTERED (ordered) | (1) PLAYBOOKS first — its ~40 tags are cuts through other sections' methods; second movers otherwise find code already moved. (2) BATTLE-1/BATTLE-2 (preserve the initiation SUM). (3) ACTIVATE (post-bypass, triangle re-baselined). (4) DEPLOY-1 orchestration. (5) MOVE formalize (no ladder yet). (6) RESPONSE router. Mechanical rules per extraction: carry every early-return guard above the extracted rule (a bare `return` today suppresses everything downstream — extraction converts "suppressed" into "summed"); actionId strings frozen; canEvaluate signatures frozen until T4; routing-stability check (evaluator-vs-keyword-fallback decision counts unchanged) | Identical scores + fire-coverage + routing stability |
| T4 MAGNITUDE (one per build, LAST) | (1) MOVE clobber ladder — dual-utility semantics, HIDDEN_PATH_MANDATORY above STAY_AND_CRUSH, ships only with the replay regression set. (2) PULL ENGINE merge (moves #4 + #8): keep the V29.9/V29.11 nested guards; the ONE pull scorer must stay above V168's +5000 in the activate window; full before/after boundary table; verification matrix includes activate-window pulls | Boundary table reviewed by Steve + soak games |

## 6. Traps ledger (carried + new)

- Additive dominance: CombinedEvaluator SUMS per actionId; "score-neutral" = identical scores.
- Multi-arm tags (~22): sub-label BEFORE moving; migrations move ARMS, never grep-and-move.
- Parity pairs edited together, same commit: V179↔V67ai keyword lists; V136↔V137
  deploy/move winnability; V153's duplicated zone block in the two force-loss methods.
- Dead code travels WITH its hub — it is the revert path. Comment out, never delete.
- CharacterDeploySiteEvaluator is shared with chosenone: banners safe, code motion needs Steve.
- Early-return extraction hazard (NEW): carried guards checklist per extraction.
- Dual-brain routing (NEW): the RandoCalAi keyword tables are a second brain on an int
  scale; canEvaluate edits silently move decisions between brains. Frozen until T4.
- Verification blind spots (NEW): ~106 tags never fired in the census sample; cold-tag
  sections need targeted self-play decks before their T3 step, or they migrate on faith.
- ObjectiveHandler.java is dead; ActionAudit (V68) and BattlePredictor.shouldInitiateBattle
  are DORMANT (zero callers) — label, don't wire, don't "clean up".

## 7. Decisions this plan resolves (with grounds)

| Decision | Ruling | Grounds |
|---|---|---|
| PLAYBOOKS ownership | Primary home for deck tags, phase-organized internally, back-pointers in phase sections | Council + maintainability; T3-first because its tags cut through other sections |
| Deploy A1/A2 split | A1 stays under DEPLOY; A2 reclassified as the cross-phase PULL ENGINE | Skeptic: pulls fire in Activate/Control/responses; feedback_pull_before_activate lives in the Activate window |
| Move clobber ladder | Approved for T4, dual-utility semantics | Shipped pattern (Zoo Tycoon 2); council concurs; kills cross-rank dominance by construction |
| CONTROL scope | Promoted to 3-part dispatch section | Census 15.4% + four documented code gaps (retrieval etc.) |
| BATTLE internal cut | Weapons-segment window / power checklist / damage picker | Rulebook timing law: power-segment math must be played in the weapons segment |
| RESPONSE shape | Thin router; deep sections get callable entry points | Rulebook: top-level actions alternate in every phase; code already routes phase-agnostically |
| Spine slots | START-OF-TURN + END-OF-TURN added | Rulebook windows + census (36 + 29 real decisions); zero code exists — cheap to name now |

## 8. Post-reorg backlog (strategy work UNLOCKED by the structure — explicitly not part of it)

- Turn-posture object computed once per turn (spread-vs-concentrate stance, own/opponent
  force-pile + hand signals, race position) read by DEPLOY/MOVE/DRAW — the community's
  core-tension variable and the fix for Forge-style per-rule isolation.
- Battle intent computed at deploy time (initiator ≈ 2-card swing) — extends the V61c
  battle-intent predicate into DEPLOY-1 scoring.
- A real RETRIEVAL rule set in CONTROL-2b (today: +50 keyword fallback).
- Move hysteresis (commitment bonus + cooldown) against ping-pong shuffling.
- Re-entrant phase handlers + a persistent multi-step plan object — the two
  community-documented rando_cal weakness classes.
- Normalization bands / gradual IAUS-style bounded contributions inside the hottest
  BANDED sections, if dominance incidents continue after the reorg.

## 9. Appendix

- Census method + per-game numbers: workflow task wc5gvswrl (4 games, 1,815 decisions,
  phase attribution from `decide() called ... phase=X` lines).
- Research transcripts: workflow runs wf_854a5088 (research+skeptics), wf_e56ffe16
  (code map, 286 branches line-verified, routing 29/29), wf_9a5b1f0a (version table).
- Version table: `resources/Rando_Version_Table_2026-07-01.xlsx` (freeze at T0 rebaseline).
- Diagram: artifact "Rando Logic Tree" (update to v2 on plan approval).
- Council: use model tag `deepseek-r1:70b-llama-distill-q8_0` direct at 127.0.0.1:11434.
