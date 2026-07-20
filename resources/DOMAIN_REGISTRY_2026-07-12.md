# RANDO DOMAIN REGISTRY — 2026-07-12 (phase-reorg BATCH 0)

Authoritative LIVE V-tag inventory grouped into semantic domains. **One owner per domain** is the migration law this document encodes; every later batch keys on it. Objective-XOR is domain-scoped: a rule is owned by EITHER the objective brain (objective-intent, via `ObjectiveAnalyzer.getDeployObjectiveAdjustments()`) OR its phase pipeline — never both.

- Built at HEAD `5ab16f8ac` (2026-07-12, batch-1 hotfixes included). Branch `rando-consolidation-2026-06-23`.
- **AMENDED 2026-07-13** (gate `5e290559c`; evidence `Handoffs/CODEX_DOMAIN_REGISTRY_GATE_5E290559C_2026-07-13.md` + arm enumeration `Handoffs/CODEX_DOMAIN_REGISTRY_AMBIGUITY_RESOLUTION_2026-07-13.md`): the 13 ex-AMBIG summary rows are split into **21 exact arms** (authority table §5); V172 solo-dominance corrected to its live **+600** score; FS-enforcement re-homed solo-formation → loop-safety (generic constraint infrastructure, not formation-owned); §1/§2/§4/§7 tables and counts regenerated from the post-split inventory. All 21 arms re-verified against the live tree at HEAD `5240f36c6`: markers `grep -cF` counted per bot file, magnitudes read from the live `addReasoning`/`hardVeto` calls, no enclosing `if (false` on any arm, both bots checked.
- **AMENDED 2026-07-13 (2)** (gate finding m00288 on 631ed4c13): the three live V27 sibling arms are minted as first-class rows — V27-maintenance-pass (PE, +25/+50), V27-maintenance-move (ME, -80) under force-budget; V27-buddy-protect (ME, -150/-250/-400) under solo-formation. Authority table = 24 rows (23 LIVE + the 1 INERT V37.4-empty-check); live total 364 → **367**.
- Scope scanned: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/*.java`, `.../rando/strategy/*` (rules live in DPP, DPS, SS, DO, AA; OA is now a facade), `.../models/common/strategy/*` (including shared cOA), plus `RandoCalAi`/`DecisionSafety`/`DecisionContext` where manifest arms live there. `.bak` files and `if (false /* SUPERSEDED */)` blocks excluded.
- Backbone: `resources/Rando_Section_Manifest_2026-07-06.xlsx` (340 single-owner arms, T0.3/T0.4), re-verified against the live tree; 30 rows added (manifest gaps + everything shipped 2026-07-07 → 2026-07-12: V192 pull hub, V193, FORMATION SAFETY, batch-1 hotfixes).
- A *rule* = one V-tag arm (multi-arm tags appear once per arm, `Arm of` set). KIND per plan §4: VETO / ORDERING / BANDED.
- **Anchor semantics**: `FILE:line` = first live occurrence of the base tag in that file at HEAD (block may start at a nearby comment); multi-arm tags share the base-tag anchor — grep the arm's log string for the exact block. Manifest 07-06 line refs inside Trigger text have drifted; re-grep before moving code.
- File abbrevs: ATE=ActionTextEvaluator CSE=CardSelectionEvaluator DE=DeployEvaluator ME=MoveEvaluator BE=BattleEvaluator DrE=DrawEvaluator PE=PassEvaluator FAE=ForceActivationEvaluator CE=CombinedEvaluator DPP=DeployPhasePlanner DPS=DeployPhaseScript OA=bot-local ObjectiveAnalyzer facade SS=ShieldStrategy DO=DeckOracle SC=StrategyController AA=ActionAudit RCA=RandoCalAi DC=DecisionContext DSf=DecisionSafety / shared common: cOA=ObjectiveAnalyzer CDSE=CharacterDeploySiteEvaluator FS=FormationSafety FRS=ForceReserveService MF=MaintenanceFacts MP=MovePredicates ShF=ShieldFacts. The file IS the decision route (ATE=text-ranked top-level, CSE=card-selection prompts, DE=deploy scoring, ME=move destinations, BE=initiation, CE=merge/select).
- Every rando evaluator has a **chosenone mirror**; `common/strategy` files are SHARED (no mirror drift). "Both bots" applies to every row unless noted.

## 1. Domain overview

| Domain | Live rules | Current owner files (count) | Target single owner (phase plan) |
|---|---|---|---|
| setup-starting | 15 | ATE CSE DC DE DO OA RCA (7) | SETUP slot (turn spine); code today = CSE turn-0 blocks |
| activation-amount | 7 | ATE DC FAE (3) | ACTIVATE (FAE + named ATE blocks); the V61c -6000 / V168 +5000 / V38.3 +500 triangle is ONE boundary across 3 sites |
| force-budget | 11 | ATE BE CSE DE DPP DrE ME PE (8) | ForceReserveService (shared svc; one cached computation) + MaintenanceFacts basis |
| drain-control | 11 | ATE CSE (2) | CONTROL slot (turn spine 2a/2b) |
| deploy-sequencing | 23 | ATE CE CSE DC DE DO DPS ObjectiveType.java RCA (9) | DEPLOY pipeline / DEPLOY-1 (DPP + DPS bucket walk + CE epilogue) |
| deploy-siting | 38 | AA CDSE CSE DE ME (5) | DEPLOY pipeline / DEPLOY-2 (hub = shared CDSE.evaluateSite; V136) |
| deploy-attach | 25 | ATE CSE DE DO (4) | DEPLOY pipeline / DEPLOY-3 (gate = V158; weapon pulls also gated by V185/V120) |
| solo-formation | 15 | ATE BE CDSE CSE DE FS ME (7) | FormationSafety (shared common/, both bots) — LAWS only; veto enforcement plumbing = SVC-SAFETY, see loop-safety FS-enforcement row (gate 5e290559c #3) |
| battle-initiation | 12 | ATE BE CSE DC DE FAE ME (7) | BATTLE pipeline / BATTLE-1 (TRAP: ATE V25 tier block + BE both score initiation — preserve the SUM) |
| battle-weapons | 21 | ATE CSE DE (3) | BATTLE pipeline / BATTLE-2 |
| battle-forfeit | 13 | CSE RCA (2) | BATTLE pipeline / BATTLE-3 (hub = v159ForfeitScore, CSE) |
| move | 49 | AA ATE CSE DE DPP ME RCA RandoConfig.java (8) | MOVE pipeline (T4 clobber ladder; dual-utility semantics) |
| draw-count | 6 | DrE (1) | DRAW (DrE) |
| force-loss-payment | 8 | CSE FLF FLP (3) | FORCE-LOSS policy (hub = shared ForceLossPolicy; CSE is the stock-choice adapter) |
| shields | 13 | ATE CSE RCA SS (4) | SHIELDS engine (ShieldStrategy + ShieldFacts) |
| pull-search | 33 | AA ATE CE CSE DC DE DO RCA (8) | PULL ENGINE (hub = V192 in ATE since T4.2); facts stay SVC-ORACLE (DeckOracle) |
| objective-intent | 7 | AA ATE BP cOA CSE DC DE DPP ODT RCA (10) | SVC-INTEL (shared ObjectiveAnalyzer is the LIVE brain; bot OA files are compatibility facades; ObjectiveHandler.java is DEAD, do not wire) |
| loop-safety | 10 | AA ATE CE CSE DSf DrE RCA EvaluatedAction.java (8) | SVC-SAFETY (DecisionSafety + ATE loop guards + CE finalizer incl. the generic hardVeto OR-merge/enforcement) |
| pass-cancel | 2 | DE PE (2) | SVC-SAFETY (PassEvaluator; V148 cancellability semantics) |
| response-routing | 6 | ATE CSE (2) | RESPONSE router (thin dispatcher; routes to callable sections) |
| deck-playbook | 37 | ATE BE cOA CSE DE DO DrE ME RCA (9) | PLAYBOOKS overlay (shared cOA owns objective data; phase back-pointers retain score ownership) |
| fact-services | 5 | BP CSE DC ODT RCA (5) | common/strategy services (MovePredicates, ShieldFacts, MaintenanceFacts, FRS) + SVC-ORACLE |

## 2. Registry by domain

### setup-starting — 15 rules
*Turn-0 starting cards: effects, locations, epic-event picks.* Target owner: SETUP slot (turn spine); code today = CSE turn-0 blocks.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V21-starting-ban | V21 | SETUP | CSE:214 DE:149 | VETO | — | Ban list for effects that must never be starting-effect picks (CSE ~218-278; banned -500 vs +100 allowed). 1 CSE hit (line 214) and the 1 DeployEvaluator hit (line 149)… | LIVE |
| V22-starting | V22 | SETUP | CSE:1730 | BANDED | starting-pick tier: effects +25..+500 (ban -600); locations +25..+75 base | Two sub-blocks, both SETUP: starting-EFFECT block (~7929-8144 + reserve-pick mirror 8505-8511; ban -600, preferred +200, Shadow Collective +500, objective-synergy… | LIVE |
| V29.14-epic-start | V29.14 | SETUP | CSE:394 | ORDERING | — | Epic-mention starting location +1000 hard-prefer (7137-7144) + Funeral Pyre title check +1000 (7148-7153); depends on V71 text concat; lines 394/397 are the V67v routing… | LIVE |
| V29.15 |  | SETUP | RCA:120 DC:86 ATE:2472 CSE:8733 | ORDERING | — | The Force Is Strong In My Family saga choice by deck name: correct pick +1000 / wrong -500 / no-deck-name default I Have It +500 (ATE 2388-2420); RandoCalAi hits =… | LIVE |
| V43-starting-interrupt | V43 | SETUP | CSE:501 | ORDERING | — | FOUND ARM not in handoff sect8's V43 row: starting-interrupt selection routing (CSE 494) + evaluateStartingInterrupt (6967-7013) + Epic-Event hard-prefer +1500… | LIVE |
| V67o |  | SETUP | CSE:394 | BANDED | +300 battleground / -150 non-battleground (below +1000/+500 overrides, above +25..+75 base) | Battleground starting-location preference (7156-7196). 2 hits (394, 502) are the V67v/V67r routing | LIVE |
| V67p |  | SETUP | CSE:225 | VETO | — | Tentacle excluded as starting-interrupt pick (comment at CSE 225 inside the starting-ban region) - hard exclusion, no | LIVE |
| V67q |  | SETUP | CSE:395 | BANDED | +600 non-Palace BG / -350 Palace / -300 non-BG (additional to V67o; nets ~+900/~-50/~-450) | Sith-deck starting-location tightening when Rise/Revenge Of The Sith is in Rando's own pool (~7203+). Pool ownership guarded by | LIVE |
| V67r |  | SETUP | CSE:393 | ORDERING | — | Routing rule: turn-0 'Choose where to deploy' prompt routed to evaluateStartingLocation so V67o/p/q fire. Pure dispatch, | LIVE |
| V67v |  | SETUP | CSE:392 | ORDERING | — | Branch-precedence fix (CSE 392-399) so V67r's routing runs before the generic turn-0 branch; restored V67o/p/q/r + V29.14 + V67q on Luke Saga starts. Pure | LIVE |
| V67x |  | SETUP | CSE:7815 | VETO | — | Ownership guard on V67q's pool check (CSE 7233, 7247): getAllPermanentCards() returns both players' cards; filter to Rando's own so opponent's Rise/Revenge never… | LIVE |
| V80 |  | SETUP | CSE:8555 | ORDERING | — | Skywalker Epic Event required Effects (A Cunning Warrior / A Good Friend) must-pick: +1000 in evaluateUnknown (8068-8082) + reserve-pick mirror (8492-8502). +1000 is the… | LIVE |
| V126x |  | SETUP | CSE | BANDED | starting-effect tier +400..+600 | EXPANDED STARTING-EFFECT BONUSES family inside the V22 turn-0 block: V126 header comment x1 (7985), V126a First Strike / free-battle-initiation +500 x3, V126b… | LIVE |
| V186 |  | SETUP | CSE:809 cOA:21 | ORDERING | — | I Want That Map turn-0 script: temp-ID-safe Starkiller Base SYSTEM +400 in evaluateDeployLocation (CSE 802-847, resolves blueprints via context.getBlueprints() - the… | LIVE |
| V187 |  | SETUP | CSE:8520 DO:21 | BANDED | -300 demotion within starting-effect pool (below +1000/+500 tiers by design) | Duplicate starting-effect penalty (CSE 7938-7948): -300 to any candidate Effect duplicated in the decklist; only re-orders inside the pool, never blocks. DeckOracle hit… | LIVE |

### activation-amount — 7 rules
*How much Force to activate + the activate/interleave boundary.* Target owner: ACTIVATE (FAE + named ATE blocks); the V61c -6000 / V168 +5000 / V38.3 +500 triangle is ONE boundary across 3 sites.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V38.3-confirm | V38.3 | ACTIVATE | ATE:204 FAE:13 DC:319 | VETO | — | 'Not activated Force' confirm bounce-back: choose No/go-activate +9999, refuse skip -9999 (ATE 1418-1455, with the V61c carve-out honoring Yes when reserve <= 3 AND… | LIVE |
| V42-activation | V42 | ACTIVATE | FAE:11 | ORDERING | — | calculateActivationAmount ALWAYS reserves cards for destiny draws (FAE 85-89) - replaced the old V38.2 threshold. Amount-ladder rule. OTHER V42 ARMS: emergency draw ->… | LIVE |
| V43-floor | V43 | ACTIVATE | FAE:12 | VETO | — | Always activate >= 1 when asked - engine re-asks on 0 (infinite loop), so this is a hard floor. OTHER V43 ARMS: redundant Battle Order/Plan shield skip ShieldStrategy 2… | LIVE |
| V57 |  | ACTIVATE | FAE:11 | ORDERING | — | ACTIVATE FULL default (FIX 19 removed all throttling) - the bottom rung of the amount ladder in calculateActivationAmount: V43 floor >= 1, then V61c keep-3 cap, then… | LIVE |
| V61c |  | ACTIVATE | ATE:203 FAE:12 DC:309 | VETO | — | Destiny-buffer triangle, three sites sharing ONE DecisionContext battle-intent predicate (DC 309-320): FAE keep-3 activation cap (179-208), ATE -6000 Activate carve-out… | LIVE |
| V67at |  | ACTIVATE | FAE:11 | ORDERING | — | End-game force preservation (FAE 167-208): lifeForce <= 10 caps activation at maxAvailable-2 (takes the more conservative vs V61c); V43 floor still applies. A cap rung… | LIVE |
| V168 |  | ACTIVATE | ATE:203 FAE:13 DC:319 | VETO | — | +5000 ALWAYS ACTIVATE (ATE 193-231) - a +/-9999-class score costume; V61c destiny-buffer is its only carve-out. FAE/DC hits = shared-predicate comments. Any PULL-ENGINE… | LIVE |

### force-budget — 11 rules
*"Reserve N Force for X" rules: maintenance, DTF, interrupts, transit.* Target owner: ForceReserveService (shared svc; one cached computation) + MaintenanceFacts basis.

**V269 owner note:** ForceReserveService still owns the DTF and maintenance observations. Shared AI-only `PassPolicy` now owns the separate V27.1 and V27 Pass contributions; both bot adapters only obtain the cached facts and apply the policy result.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V22.3-maintenance | V22.3 | DEPLOY-1 | DPP:210 | ORDERING (reserve calc) | reserves maintenance upkeep before location deploys | Deploy-phase planner reserves total maintenance upkeep Force (MaintenanceFacts basis via ForceReserveService) before planning deploys | LIVE (manifest gap — added batch 0) |
| V24.5 |  | DEPLOY-1 | DE:2306 ATE:4235 | BANDED | -50/-50 warnings; small ATE use/lose-force aversions | Penalize deploys that leave less Force than existing maintenance cards need at end of turn | LIVE (manifest gap — added batch 0) |
| V27-battle-reserve | V27 | BATTLE-1 | BE:858 (#evaluate, block 801-866) | BANDED (SCORE) | -40 pile<2 / -15 pile<4 (no-DTF branch) | Battle-interrupt Force readiness cost on the initiation score. Marker `"V27 BATTLE FORCE WARNING"` ×1/bot. SIBLING V27 ARMS minted as first-class rows 2026-07-13 (gate m00288): V27-maintenance-pass + V27-maintenance-move below (force-budget) and V27-buddy-protect under solo-formation. Fixture: TODO `B0_V27_BattleForce_LowPile` | LIVE (split 2026-07-13) |
| V27.1-battle-DTF | V27.1 | BATTLE-1 | BE:846 (#evaluate, block 813-854) | BANDED (SCORE) | -60 pile<3 / -100 pile=0 / 0.0f informational above | DTF on opponent's table taxes every battle interrupt; initiation penalized when the pile can't fund tax+loss+interrupt. Still an INLINE table scan — cutover target: consume ForceReserveService.dtfActive (the PE arm already does). Marker `"V27.1 DTF ACTIVE"` ×1/bot. Fixture: TODO `B0_V271_DTF_BattleBlocked` | LIVE (split 2026-07-13) |
| V27.1-pass-DTF | V27.1 | pass | PE:222 (#evaluate, block 184-229) | BANDED (SCORE) | +20 pile<=3 / +40 pile<=1 / +60 pile=0 on Pass | Same DTF fact, separate phase contribution: prefer Pass to conserve the interrupt money. Already consumes ForceReserveService.dtfActive (T2 move-1 commit-2; old inline scan //-commented). Marker `"V27.1 DTF RESERVE"` ×1/bot. Fixture: `PassEvaluatorCharacterizationTest#fullEarlyResourceStackMatchesLegacyScoreAndReasonOrder` | CONSOLIDATED V269 |
| V27-maintenance-pass | V27 | pass | PE:223 (#evaluate, block 223-240) | BANDED (SCORE) | +25 on Pass (pile <= obligation+1) / +50 (pile < obligation) | Maintenance obligation from the shared ForceReserveService cache (MaintenanceFacts basis; old inline scan removed, cleanup 1.7): pile at/below the obligation → prefer Pass, conserve. Marker `"V27 MAINTENANCE RESERVE"` ×1/bot. Fixture: `PassEvaluatorCharacterizationTest#fullEarlyResourceStackMatchesLegacyScoreAndReasonOrder` | CONSOLIDATED V269 |
| V27-maintenance-move | V27 | MOVE | ME:1968 (#evaluate, block 1968-2004) | BANDED (SCORE) | -80 on moves that dip the pile below the maintenance obligation | Same ForceReserveService obligation, move-phase contribution (COMMIT-2 engine basis; old inline scan //-commented in place). Marker `"V27 MAINTENANCE MOVE BLOCK"` ×1/bot. Fixture: TODO `B0_V27_MaintMove_Dip` | LIVE (minted 2026-07-13, gate m00288) |
| V67z-draw-reserve | V67z | DRAW | DrE:661 (#calculateForceToReserve) | ORDERING reserve calc (CONSTRAINT) | +1 reserve per friendly body at Underground Corridor, UNCAPPED | Hidden Path (unflipped) move-phase transit money inside the V58 reserve target. DEFECT NOTE (gate-confirmed, do NOT silently fix): counts every friendly CHARACTER, not Jedi, despite the wording. Marker `"V67z TRANSIT RESERVE: {} Jedi at Underground Corridor"` ×1/bot. Fixture: TODO `B0_V67z_DrawReserve_CorridorCount` | LIVE (split 2026-07-13) |
| V67z-deploy-reserve | V67z | DEPLOY-1 | DE:333 + DE:2372 (#evaluate) | BANDED -1500 (CONSTRAINT) | reserve CAPPED at 3 (min(bodies,3)); -1500 per force-costing deploy dipping below it; cost-0 [download]s exempt | Deploy-phase twin (deploy runs before move and was starving the transit). Same all-CHARACTER counting defect as the draw twin. KEEP SEPARATE from the draw arm: cap-3 semantics differ (gate ruling). Markers ×1/bot each: budget calc `"hold {} Force in deploy for the move-phase transit off Mapuzo"`, penalty `"V67z TRANSIT RESERVE: Deploy costs %d"`. Fixture: TODO `B0_V67z_DeployReserve_Cap3` | LIVE (split 2026-07-13) |
| V141 |  | MOVE-RESPONSE | ATE:670 (#evaluate) | VETO (CONSTRAINT) | -2000 hold | Transport interrupts ("draw destiny, use that much Force") INELIGIBLE — not merely undesirable — when forcePile < 4 OR reserve deck empty (cannot draw destiny). Marker `"V141 TRANSPORT INTERRUPT BLOCK"` ×1/bot. Fixture: TODO `B0_V141_Transport_ForceFloor` | LIVE (verified 2026-07-13) |
| V174-wave-budget | V174 | DEPLOY-2 | CSE:5581 (#v173WaveProjection) | ORDERING reserve calc (CONSTRAINT) | no own action points — reserved = MaintenanceFacts table upkeep + interrupt reserve (max 2) + this card's maint + 1 battle-initiation fee (V176); V177 cap keeps wave budget >= 3 | Reserved Force is POLICY, not an observational fact (gate ruling). The projection is consumed by the V172 gates / V171 / V151; the arm lives inside the V173 fact-helper, so migration moves OWNERSHIP, not the helper. Marker `"tableMaint + interruptReserve + thisMaintCost"` ×1/bot. Fixture: TODO `B0_V174_WaveReserve_V177Cap` | LIVE (verified 2026-07-13) |

### drain-control — 11 rules
*Drain go/no-go, drain ordering, drain-value gates, retrieval (named gap).* Target owner: CONTROL slot (turn spine 2a/2b).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V24.15-drain | V24.15 | CONTROL | ATE:1595 | VETO | — | NEVER drain at 0: -9999 hard block (Surprise Assault trap avoidance), ATE 5207-5304 + the 5534 note-line; V189 net-value gate now sits upstream at this check. OTHER… | UPDATED 2026-07-07 (EFFECTIVE DRAIN; ex-V189b folded in) |
| V24.2-drain | V24.2 | CONTROL | ATE:3068 (#evaluate) | BANDED (RANK) | +80 live (VERY_GOOD_DELTA 50 + 30) | Optional "+1 to force drain" response always accepted — free damage, sets FORCE_DRAIN type. RE-HOMED from battle-weapons 2026-07-13 (gate: neither V24.2 arm is battle; free-drain acceptance is unrelated to pulls or battle). Marker `"V24.2 FORCE DRAIN BONUS"` ×1/bot. Fixture: TODO `B0_V242_DrainPlus1_Accept` | LIVE (split 2026-07-13) |
| V25-SimpleTricks | V25 | CONTROL | ATE:2191 | VETO | — | Simple Tricks And Nonsense drain block: -9999 on non-battleground drains when opponent has it on table (ATE 5308-5364; occurrence lines 5308, 5318, 5356, 5357, 5364).… | LIVE |
| V29.9-HuntDown-drain | V29.9 | CONTROL | ATE:2217 | BANDED | +30 base / +40 high-icon site (Hunt Down drain-priority tier) | Hunt Down force-drain priority: +30 always (Visage +1 pressure), +40 at high opponent-icon sites (ATE 5635-5658). OTHER V29.9 ARMS: Barrier-risk assessment… | LIVE |
| V29.14-noescape-retrieval | V29.14 | CONTROL | ATE:1646 | BANDED | +200 (retrieval/lost-pile tier) | FOUND ARM not in my task list: 'NO ESCAPE - take top card of Lost Pile into hand' +200 (ATE 1572-1579). Plan sect3 CONTROL row 2b explicitly names 'V29.14/V23 block' as… | LIVE |
| V48-drain | V48 | CONTROL | ATE:5799 | BANDED | -50 (VERY_BAD_DELTA) early-turn drain deferral | Battle Order early-turn drain skip: turns <= 3 defer drains to save Force for deploys, -50 (ATE 5569-5570); hits 5436, 5439, 5527 are the shared V140/V104 waiver-cluster… | LIVE |
| V52-drain | V52 | CONTROL | ATE:4183 | BANDED | +50 (VERY_GOOD_DELTA) drain-anyway; +100/+200/+300 multi-site drain tier | Drain-priority arm: after turn 3 drain anyway under Battle Order +50 unless V104 hard-blocked (5560-5566; 5253 = the V189 comment restoring V52's net -1 stance), plus… | LIVE |
| V52-self-cancel | V52 | CONTROL | ATE:4183 | VETO | — | NEVER cancel own force drain: -9999 at both cancel sites (ATE 3993-3994 and 5734-5737, Surprise Assault self-sabotage case). Handoff sect8 names this a distinct V52 arm;… | LIVE |
| V104 |  | CONTROL | ATE:5800 | VETO | — | Under Battle Order/Plan, hard-block (-2000) drain <= 1 when the +3 cost is actually due (ATE 5532-5557); now fronted by the V140 engine-true cost waiver (hits 5436,… | LIVE |
| V140 |  | CONTROL | ATE:5801 CSE:8400 | ORDERING | — | Battle Order cost-waiver check firing BEFORE V104/V48: engine initiate-cost 0 => drain is free, positive score + skip penalties (ATE 5430-5533; UPDATED 2026-07-04: old… | LIVE |
| V189 |  | CONTROL | ATE:5800 | VETO | — | Net-value drain gate (ATE 5226-5298, at the V24.15 check): initiate cost > drain => net <= -2, -2000 block; net -1 allowed ONLY if force pile covers planned deploys +… | LIVE |

### deploy-sequencing — 23 rules
*What to deploy this turn, order, hold-back, budget walk.* Target owner: DEPLOY pipeline / DEPLOY-1 (DPP + DPS bucket walk + CE epilogue).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V24-location-first | V24 | DEPLOY-1 | DE:3538 CSE:8206 | ORDERING | — | MEGA LOCATION PRIORITY, DE ~3350-3426: first-3-turns location deploys dominate (+200 tier). Arm split NOT in handoff §8 (discovered): V24 tag has 4 live arms. V24 total… | LIVE |
| V24-tdigwatt-engine-boost | V24 | DEPLOY-1 | DE:3538 | BANDED | +300 tier | DE 3972-3986: Dark Deal / CC Occupation +300 deploy-ASAP boost, TDIGWATT-only. Discovered arm, not in §8. Plausibly PLAYBOOKS (deck-specific); homed DEPLOY-1 pending… | LIVE |
| V24.4 |  | DEPLOY-1 | ATE:1573 | ORDERING | — | LOCATIONS FIRST: -800 on non-deploy actions while a location sits in hand (ATE ~1499-1554). Sequencing enforcement implemented as a large penalty. Carve-outs: V67ba… | LIVE |
| V38.4 |  | DEPLOY-1 | DE:559 ATE:5859 | BANDED | urgency scaling, hand-size + force-pile driven; PERSONA REPLACE -500 | Three DE arms: DEPLOY URGENCY block 877-918 (with V56), PERSONA REPLACE -500 block 547-552, FORCE SURPLUS off-plan allowance 2024-2035. ATE hit is a V189-interplay… | LIVE |
| V40 |  | DEPLOY-1 | DE:1034 ObjectiveType.java | VETO | — | HOLD_BACK gate: applies to TDIGWATT only, all other decks deploy freely; also gates the turn-1 DEPLOY_LOCATIONS plan block to TDIGWATT. Heaviest single-tag presence in… | LIVE |
| V46 |  | DEPLOY-1 | DE:1056 | VETO | — | HOLD_BACK off from turn 3+: hard time-gate on the same hold-back policy V40 scopes by | LIVE |
| V50 |  | DEPLOY-1 | DE:3197 | BANDED | power-disadvantage penalty, turns 1-3 only, below-even threshold | DE 3009-3027: penalty (variable disadvantagePenalty) when deploy would end below even power, turns 1-3; from turn 4 explicit 0.0f LATE DEPLOY pass-through. Exact… | LIVE |
| V52-momentum | V52 | DEPLOY-1 | DE:2267 | BANDED | +100 base momentum tier | FIX 11 DEPLOY MOMENTUM, DE 5603-5630 (4 hits) + 3 legacy comment refs to the removed V52 SPEND FORCE +300 (2129, 2152, 5588 under V67bk). Other V52 arms NOT claimed: ATE… | LIVE |
| V52-tdigwatt-t1 | V52 | DEPLOY-1 | DE:2267 | ORDERING | +850..+1500 scripted tiers | FIX 12 TDIGWATT TURN 1 SCRIPT, DE 5635-5673: Bespin +1500 > CC site +1200 > Lando +1000 > Executor +900 > Chiraneau +850. NOT in the §8 V52 arm list… | LIVE |
| V53c |  | DEPLOY-1 | ATE:1534 | VETO | — | ATE 1460-1485: -9999 hard block on Wokling (V) Effect search by blueprint id 200_47, turns 1-3. Homed DEPLOY-1 per slice; content is | LIVE |
| V55 |  | DEPLOY-1 | DE:6004 | BANDED | positive urgency tier, scaled up early game (computed v55Bonus) | FIX 17 HIGH-ABILITY DEPLOY URGENCY, DE 5778-5799: ability >= 6 characters in hand get a steady deploy bonus, | LIVE |
| V56 |  | DEPLOY-1 | DE:896 | BANDED | +50/+80 baseline tier (+80 idle-force adder) | Mid-hand baseline urgency inside the V38.4 block, DE 877-911: mid-hand +80, small-hand +50, +80 more when force sits unused. Effectively an arm of V38.4 kept as its own… | LIVE |
| V60-v24skip | V60 | DEPLOY-1 | DE:576 | ORDERING | — | DE 3353-3416: V60-tagged refinement of V24 — only genuine 'Deploy <location>' actions get the location-first bonus; game-text pulls fall through to V67i/V60/V67bg. Third… | LIVE |
| V67ax |  | DEPLOY-1 | RCA:76 CE:137 DC:65 | ORDERING | — | Deploy Phase Script activation plumbing: walk steps 1-5, restrict evaluator pipeline to first non-empty step, CARD_ACTION_CHOICE during DEPLOY only. Original DPS;… | LIVE |
| V67ba |  | DEPLOY-1 | ATE:1613 | ORDERING | — | Exempts generic 'Play a card'/'Deploy' entry-point actions from the V24.4 -800 so the deploy-from-hand gateway is never penalized (ATE 1539-1560). Functionally an arm of… | LIVE |
| V67bb |  | DEPLOY-1 | DPS:18 | ORDERING | — | DPS REDESIGNED header + no-opinion fall-through (0 actions classified -> normal scoring) + battleground scan. The DPS file itself is the V67bb/V67bc/V179 bucket-walk | LIVE |
| V67bc |  | DEPLOY-1 | RCA:1234 CE:22 DC:71 DPS:18 | ORDERING | — | DPS hierarchy walk: ordered buckets top-to-bottom, pick highest above bad-threshold, PASS only when ALL buckets exhausted. UPDATED-adjacent code (CombinedEvaluator) —… | UPDATED 2026-07-12 (epilogue skips hard-vetoed actions) |
| V67i |  | DEPLOY-1 | DE:3560 ATE:4978 | ORDERING | — | GLOBAL LOCATION-FIRST PRIORITY, DE 3420-3490: source-card game-text parse for location keywords; any action putting a location on table beats character deploys. ATE hit… | LIVE |
| V162 |  | DEPLOY-1 | DE:3565 | ORDERING | +500 / -200, life-force<=10 flip | DE 3377-3411: life force > 10 adds +500 on top of V67ai +1400 (locations first, +1900 total); life force <= 10 gives -200 HOLD (location kept as force-loss fodder). Uses… | LIVE |
| V179 |  | DEPLOY-1 | DPS:18 | ORDERING | — | Shared namesLocation() so scored location pulls classify into the LOCATIONS bucket; 2026-06-29 revision: named-location target already in hand does NOT classify as… | LIVE |
| V184 |  | DEPLOY-1 | ATE:422 | BANDED | +300, existence-gated | ATE 390-427: optional when-deployed free-value triggers +300 (beats Pass), gated on non-empty Reserve (reveal/look) or non-empty Lost Pile (retrieve). Watch note in… | LIVE |
| V190 |  | DEPLOY-1 | DE:846 CSE:1481 DO:22 | VETO | -12000 hard block (DE) / -1500 site penalty (CSE) | Steve 2026-07-04: only deploy starships to systems. DE 834-858: -12000 blocks a starship-only Reserve fetch when no space location (SYSTEM/SECTOR) on table; CSE… | LIVE |
| vehicle-pilot+docking-bay |  | DEPLOY-1 | CSE | BANDED | +50 aboard-ship / +80 empty-bay; starship-site block now V190 | No dedicated V-tag; count 0 own-tag hits — the code carries other tags: starship-never-to-docking-bay block CSE 1363-1500 (widened into V190 on 2026-07-04, dead… | LIVE |

### deploy-siting — 38 rules
*WHERE a character/unit goes: site scoring, contest/protect/spread.* Target owner: DEPLOY pipeline / DEPLOY-2 (hub = shared CDSE.evaluateSite; V136).

**V271 owner note:** `DeploySitingPolicy` now also owns the `CardSelectionEvaluator` destination-compatibility stream: V29 ship-reference ground, the CSE arm of V190, starship space tiers, vehicle compatibility, V24.14B permanent-weapon siting, V29.7 empty-bay protection, and V29.6 battleground value. Adapters retain every board and card read, branch, catch, break, and log. V190's CSE `-1500` remains additive score costume, not hard-veto control.

**V272 owner note:** three scalar CSE destination arms now use their existing shared AI-only owners. `DeployPilotShipPolicy` owns V24.10 Executor/Flagship destination scoring (`+500` Bespin, additive `-9999` elsewhere); `DeploySitingPolicy` owns V23 opponent Force-icon value (`icons * 30` above zero); and `DeployCardValuePolicy` owns the V29.7 destination-ability ladder (`+50/+25/+5`, silent `1..<3`, `-30` below `1`). Adapters retain all observations, guards, catches, logs, and contribution positions. No engine metadata or candidate control flow is involved.

**V273 owner note:** `DeployPilotShipPolicy` now owns the V29 character-boarding and ship-cargo destination scores. The two `CardSelectionEvaluator` adapters still discover the first referenced ship name, perform the generic-capital subtype match, retain all blueprint/game-text/title reads and diagnostics, and own the existing cargo `actions.add` plus candidate `continue`. The shared owner contributes only the unchanged additive `+600/+650/+50/+100/+50/-300` outcomes; it does not read GEMP state or introduce engine metadata.

**V274 owner note:** `DeployWeaponPolicy` now also owns the V25 `CardSelectionEvaluator` destination-slot and Hunt Down lightsaber outcomes. Adapters retain both declaration-order attachment scans, first-match breaks, the deploying-card blueprint/title read, lazy ObjectiveAnalyzer access, catches, and diagnostics. The two separate additive `-9999` arms intentionally remain independently reachable for a second lightsaber; no engine metadata or candidate control moved.

**V275 owner note:** `DeploySitingPolicy` now owns the V64 Mapuzo destination outcome after the adapters gather opponent power and literal Jedi Survivor text, while `DeployPlanPolicy` owns the physical planned-target `+200/-100` choice after the adapters compare IDs. All board, blueprint, objective, plan, catch, and log behavior remains in the mirrored adapters; no engine metadata or candidate control moved.

**V276 behavior-fix note:** the existing V193 owner now consumes an ObjectiveAnalyzer-proven unfilled actor-at-site gate. For Invasion only, the pre-flip Neimoidian-to-Throne-Room candidate receives the playbook's `+1600` direct arm or `+3200` destination arm and is exempt from V201 at that exact gate. The requirement self-closes when filled. Shared AI-only `ShieldFacts` separately excludes Naboo system from Battle Order/Plan battleground accounting while front blueprint `14_113` is on table on either face. No new engine metadata or candidate control flow was introduced.

**V277 owner note:** `DeployFormationSitingPolicy` now also owns generic empty-destination concentration and committed reinforcement. This includes contested-solo `-200` scaling, solo `-100` scaling, empty establishment `+20`, V67bn/V67bu's inclusive `4..5` deficit and escape precedence, weak-solo `+150/+250`, and strict-above-`1.5x` pair `+100`. The adapters retain all board scans, lazy escape reads, catches, scan continues, first-found breaks, and logs. Existing V29.5 buddy and V113 operations remain later and independently additive.

**V278 owner note:** `DeployTacticalPolicy` now also owns V24.15 zero/effective-drain siting, V59 universal spy siting, V22.3 contest scoring, V24.14B fallback-spy siting, and V24.3B destination partner scoring. The mirrored adapters retain all GEMP reads, lazy exemptions, nested catches, scans, first-found breaks, fallback state, logs, and contribution positions. TDIGWATT and the V22 objective tail remain adapter-owned. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V279 owner note:** `DeployObjectiveSitingPolicy` now also owns V22.7 objective-system contesting, V29.7 ISB agent priority, Hunt Down character priority, Cloud City ability spread, Lando destination/safety, and the final objective/TDIGWATT siting tail. Both mirrored adapters retain every GameState, blueprint, ObjectiveAnalyzer, battleground, hand, character, power, opponent, and objective read plus all catches, scans, breaks, diagnostics, and contribution positions. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V280 owner note:** `MoveVergePolicy` now also owns the mirrored V79 parsec and Scarif-destination option weights plus V103 parsec fallback arithmetic. Both `ActionTextEvaluator` adapters retain all prompt detection, permanent-card/owner/zone/orbit observations, integer/regex parsing, catches, logs, action insertion, and control flow. Rando-only V79b interception remains untouched. This is an AI-only structural extraction; no engine metadata or player-choice code moved.

**V281 owner note:** `DeployFormationSitingPolicy` now also owns the mirrored V29.7 battleground preference and V67ah non-battleground penalty ladder. Both `CardSelectionEvaluator` adapters retain battleground, top-location, blueprint, side, and force-icon observations plus all catches and contribution placement. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V282 owner note:** `ControlDrainAssessment` now also owns the mirrored top-level CONTROL fallback arithmetic: the adapter-supplied force-drain base plus `+20` per controlled battleground. Both bot coordinators retain phase/action recognition, guarded board observation, counting, exception behavior, and contribution placement. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V289 owner note:** `ControlActionPolicy` now also owns the residual Monnok hand-size, make-opponent-lose, generic retrieve, and used-pile peek action-text arithmetic. Both action-text adapters retain exact case-sensitive recognition, opponent-hand and Lost Pile reads, V184 stacking position, and first-match control flow. Opponent force-drain cancellation remains exclusively `ResponsePolicy`-owned; CONTROL still owns only the two delegated self-drain veto sites.

**V283 owner note:** `BattleActionTextPolicy` and `BattleWeaponsPolicy` now also own the mirrored top-level BATTLE initiation, board-fallback, and fire-weapon score bands. Both bot coordinators retain phase/action recognition, guarded location and board observations, title matching, first-match control, fallback gating, and contribution placement. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V284 owner note:** `ResponsePolicy` now also owns the mirrored fixed-score V184 when-deployed, V29.8 Sense redraw, V53b save-Jedi, react, cancel-own, and Houjix/Ghhhk action-text operations. Both `ActionTextEvaluator` adapters retain text recognition, all game observations, catches, action types, diagnostics, branch order, and contribution placement. The two Sense classifiers remain separate additive arms. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V285 owner note:** `DeployActionTextPolicy` now also owns V160 Target The Main Generator priority, late generic Deploy-on/projection/unique scoring, and the generic Play-a-card Force baseline. The mirrored adapters retain all text and objective recognition, shield routing, action-type mutation, Force reads, logs, and the existing ordinary-Deploy skip gate. V184 remains solely `ResponsePolicy`-owned. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V286 owner note:** `ResponsePolicy` now also owns the mirrored Sense/Alter cancel bands, shadowed late force-drain twin, Barrier scoring ladder, Grab ownership ladder, and cancel-target selection operations. Both action-text and card-selection adapters retain all route recognition, AiPriorityCards and GEMP observations, catches, logs, action construction, Barrier turn state, Grab `setScore` behavior, early returns, and contribution/state-mutation positions. V52 self-drain remains CONTROL-owned. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V287 owner note:** `CoordinatorPosturePolicy` owns the mirrored top-level life-force, board-posture, and hand-title fallback arithmetic, while `DeployActionTextPolicy` owns the mirrored top-level DEPLOY fallback arithmetic. Both bot coordinators retain phase/action recognition, every context and board observation, null behavior, independent scans, first-match control, and contribution order. This is an AI-only structural extraction; no engine metadata or candidate control moved.

**V288 owner note:** `BattleWeaponsPolicy` now also owns V67bi Force Lightning `-9999` and Blaster Rack `+80/-500/-500`; `BattleActionTextPolicy` owns race destiny `+50`; and `BattleForfeitPolicy.StandaloneResidualFacts` carries dead-card/pilot flags for the policy-owned `+140/+50` standalone priorities. Both mirrored adapters retain all recognition, GEMP/card/battle/attachment reads, catches, logs, action types, early returns, and exact contribution placement before V48/V139/V21. This is an AI-only structural extraction; no engine metadata or player-choice code moved.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V24.14B-weapon-space | V24.14B | DEPLOY-2 | CSE:1601 | BANDED | -300 | Armed character (permanent weapon) deploying to a space system: weapons cannot fire at systems | LIVE (manifest gap — added batch 0) |
| V29b |  | DEPLOY-2 | CSE:2936 | BANDED | -200 (from -80) / -100 (from -40) spread-thin penalty bumps | Comment-only hits (CSE 2675/2682) documenting magnitude bumps inside the V29 buddy block; the live constants themselves carry no tag. Travels with the V29.5-buddy | LIVE |
| V31-deploy | V31 | DEPLOY-2 | DE:4643 | BANDED | pre-flip spread across objective locations; post-flip +200 reinforce hold locations, penalize 3rd obj location | DE 4417-4596 PRE/POST-FLIP OBJECTIVE DEPLOYMENT (TDIGWATT posture — PLAYBOOKS-adjacent, slice assigns DEPLOY-2). Interleaved with V36 DEFEND TERRITORY in the same | LIVE |
| V36-siting | V36 | DEPLOY-2 | DE:3101 | BANDED | context-dependent empty-deploy penalty (0 when can't challenge); DEFEND TERRITORY +800 Malachor override / lesser defend bonuses; contest-drain deploy urgency | Three sub-sites in DE: 2913-2915 CONTEST DRAIN (inside the V51 drain-contest block), 3093-3158 SMART EMPTY DEPLOY, 4472-4500 DEFEND TERRITORY (inside the V31… | LIVE |
| V38 |  | DEPLOY-2 | DE:2530 | BANDED | -150 SOLO CAUTION / -80 STAGING / +300 REINFORCE ALLY / +400 REINFORCE VADER; ability>=6 passes free | DE 2368-2651 reworked solo-deploy (replaces old V29 power<6 hard block). V67bl's exception-removal is folded in here (solo-OK waiver deleted, buddy credit earned via… | LIVE |
| V51-contest | V51 | DEPLOY-2 | DE:3008 CDSE:47 | BANDED | +150..+1000: drain-contest +500/+600; fortify/establish/reinforce +300..+500; buddy-destiny +200..+500; armed +150; Vader flip +900; CC ARMY/OBJ FIRST +300/+500; spy +1000/-300 | §8 arm 'drain-contest' = all DeployEvaluator V51. Sub-arms worth flagging for the merge: VADER FLIP +900 (DE 2926-2962, Hunt Down) and CC ARMY/OBJ FIRST (DE 3671-3711,… | LIVE |
| V67ab |  | DEPLOY-2 | DE:2871 | VETO | — | Gate, no own points: V33 buddy bonus and ability-stacking only awarded at BATTLEGROUNDS (skip at non-BG). DE 2683-2693 (BUDDY-SEEK SKIP) + 4716-4755 (BUDDY SKIP; V67ag… | LIVE |
| V67ag |  | DEPLOY-2 | DE:4077 CSE:3239 AA:23 | BANDED | -300 additional character at a non-BG that already has a friendly | Live at DE 4729-4752. DE 3867 is a comment INSIDE the dead V67aj if(false) block; CSE 2978 is a comment inside the V67ah | LIVE |
| V67ah |  | DEPLOY-2 | CSE:2231 DE:4013 | BANDED | -100 non-BG with opponent drain icons / -350 non-BG with zero icons | Live at CSE 2973-2999 (tiered non-BG deploy penalty). DE hits (3803/3867) are 'handled by V67ah in CardSelectionEvaluator' comments; 3803 sits in dead-V67aj | LIVE |
| V67bj |  | DEPLOY-2 | CSE:2967 CDSE:48 | BANDED | -400 DON'T BAIT: deficit >= 4 vs total available power, UNCOMMITTED sites only, holds 2 force battle-reserve | CSE 3603-3730 live; V67bu carved it back to uncommitted destinations (committed -> V67bn owns the call). CDSE hit = header comment listing caller-side | LIVE |
| V67bl |  | DEPLOY-2 | DE:2712 | BANDED | comment-only (behavior folded into V38: SOLO CAUTION -150 applies regardless of hand contents) | DE 2524: marker documenting removal of the V29 PAIRED 'solo OK' exception. No own code path — travels with | LIVE |
| V67bn |  | DEPLOY-2 | CSE:1853 ME:1096 CDSE:48 | BANDED | +800 REINFORCE OUTGUNNED (Braveheart), deficit 4..5 cap, no-escape only | CSE 2694-2809 live (extended by V67bu to any committed count). MoveEvaluator 751 + CSE 1776 reference 'the V67bn cap of 5 with one-point hysteresis' (move-side parity… | LIVE |
| V67br |  | DEPLOY-2 | CSE:3772 CDSE:48 | BANDED | -800 turn 1 / -300 turn 2 non-concentration SITE destinations; systems exempt; turn 3+ free | CSE 3511-3600 TURN-BASED SPREAD DISCIPLINE. V75 kill-box override nests inside it. Aboard-ship friendlies don't anchor | LIVE |
| V67bt |  | DEPLOY-2 | CSE:3483 | BANDED | comment-only (spy detection = typed checks only: Methods 1 and 3) | CSE 3222: marker documenting permanent removal of spy-detection Method 2 (both-sides-offered heuristic, false-positived Nevar/Kyneugh). Travels with the spy siting logic… | LIVE |
| V67bu |  | DEPLOY-2 | CSE:3008 CDSE:48 | BANDED | gate on V67bn +800: extends to ANY committed count, skips when an escape route exists (adjacent friendly site or friendly ship at parent system) | CSE 2747-2813 (inside the V67bn block) + 3697-3700 (the V67bj uncommitted carve-back). If escape exists, Move-phase retreat (V67au) owns | LIVE |
| V75 |  | DEPLOY-2 | CSE:3819 | BANDED | +200 KILL-BOX spread override (suppresses V67br concentration penalty when concentration site overwhelmed by >4 power) | CSE 3558-3586, nested inside the V67br | LIVE |
| V83 |  | DEPLOY-2 | DE:1405 | VETO | — | MY LORD: senators blocked from deploying anywhere but Galactic Senate (dominant penalty, intent = never). DE 1384-1433 + companion mention at 1616 (V88 header). Typed… | LIVE |
| V83.1 | V83 | DEPLOY-2 | DE:1405 | VETO | — | Applicability guard on V83's veto: only penalize when the target location is identifiable from action text (generic 'Deploy' actions defer to the CardSelection step). DE… | LIVE |
| V88 |  | DEPLOY-2 | DE:1405 | BANDED | +1500 senator -> Galactic Senate (dominant tier, deliberately overrides solo penalties) | DE 1615-1647 DeployEvaluator arm; fires only when actionText names the Senate. Senator detection = Keyword.SENATOR OR lore contains 'senator' (only 29/35 senators carry… | LIVE |
| V88-CS | V88 | DEPLOY-2 | CSE:1743 | BANDED | +1500 Senate / -2000 senator at wrong site (CardSelection route) | CSE 1665-1713 (8 hits) + 1819 (comment in the V99 block: 'V99 must live where V88 lives'). V88-CS-LORE detection at 1683. This is the arm the slice calls out alongside… | LIVE |
| V88-text-named | V88 | DEPLOY-2 | CSE:1743 | BANDED | +500 text-named home site / -500 text-avoided site; +500 skipped if site hopelessly outgunned | CSE 1718-1811, the 2026-06-03 council-verified GENERALIZED universal rule (no card-name lists). In-place edit of V88 per the update-old-rule-not-new-version standing… | LIVE |
| V89 |  | DEPLOY-2 | DE:1796 | VETO | — | DR. EVAZAN NEEDS ARMED PARTNER, DeployEvaluator route (DE 1749-1801). Title-prefix catches both 'Dr. Evazan' and 'Dr. Evazan & Ponda Baba';… | LIVE |
| V89-CS | V89 | DEPLOY-2 | CSE:1995 | VETO | — | Same block via the CardSelection location-pick route (CSE 1915-1965), labeled 'V89-CS' in code — same pattern as V99-CS. Original V89 only fires when actionText names… | LIVE |
| V96 |  | DEPLOY-2 | DE:2038 CDSE:18 | BANDED | +500 contested within ±10 power / +100 already winning by >10 | LIVE (bytecode-verified per version table; V67al interplay is dead). DE 1908-1968 CONCENTRATE AT CONTESTED SITES; the V67al mentions at DE 1915-1965 are comments INSIDE… | LIVE |
| V99 |  | DEPLOY-2 | DE:1406 CSE:1892 | VETO | — | SENATE GUARD: -1500 non-senator -> Galactic Senate, ALLOW branch when opponent power there > friendly senator power (defensive reinforcement). Both routes: DE 1654-1738… | LIVE |
| V108 |  | DEPLOY-2 | DE:1405 | BANDED | +500 senator deploy-action priority when MLITL/Make It Legal active | DE 1501-1533. Queue-preference intent (drafts senators to the front) implemented additively — kept BANDED, but ORDERING-flavored; merger may | LIVE |
| V110 |  | DEPLOY-2 | DE:1405 | VETO | — | -2000 HOLD non-senator deploy actions while MLITL active and no non-Senate SITE exists on table (stops the deploy before V99's destination pick can be bypassed by… | LIVE |
| V136 |  | DEPLOY-2 | CDSE:17 DE:1855 CSE:1027 ME:26 | BANDED | ±2000 (§A team viability) / ±700 (§B strategic position) — hub | LIVE hub = shared common/strategy/CharacterDeploySiteEvaluator.evaluateSite() (serves BOTH bots). DE hits = call site 1808-1849 + V90/V67aj superseded markers; CSE hits… | LIVE |
| V136-Fix#2 | V136 | DEPLOY-2 | CDSE:17 | BANDED | gate on §A ability penalty: contested-only | Literal 'Fix #2' at CDSE:389 (2026-06-25): ability penalty only when oppPower>0; uncontested solo falls through to V156 then the +500 reward. Counted within V136's 8… | LIVE |
| V151 |  | DEPLOY-2 | CDSE:286 | BANDED | +400 co-deploy power lookahead | Contested site, ability passes, solo power short: project affordable hand reinforcements; +400 if combined wins. V177 gear-counting feeds this projection. Zero hits in… | LIVE |
| V157 |  | DEPLOY-2 | CDSE:640 | BANDED | +200 OVERWHELM; scopes V136 §B over-stack/ability cap to UNCONTESTED sites only | CDSE 577/636. +200 nudge (lowered from +300 after council review) when opp total ability <= 4 at contested | LIVE |
| V166 |  | DEPLOY-2 | CSE:871 | BANDED | +250..+400 deploy-path contest of opponent's softest drain site | Deploy arm CSE 861-909 (6 hits) + boundary-math comments 920/975/1005 (3) + shared helper 5604 (1). Fires when opponent out-drains net >= 2 | LIVE |
| V170-siting | V170 | DEPLOY-2 | CSE:989 | BANDED | +600..+900 (600 + min(300, drain*75)) spy to opponent's biggest drain site | §8 arm 'spy siting'. CSE 967-997, Keyword.SPY card to opponent-occupied drain>=1 site. Pairs with the V51 spy-deploy sub-arm in | LIVE |
| V171 |  | DEPLOY-2 | CSE:1022 | BANDED | +600 DEPLOY TO CONTACT | CSE 1000-1078: destination opponent-occupied + affordable wave coming this phase -> deploy directly instead of adjacent-then-march. Gated by V172; flat force>=4 check… | UPDATED 2026-07-11 (hit-aware contact; character-gated — fired on starship First Light) |
| V181 |  | DEPLOY-2 | CDSE:350 | BANDED | +200..+300 (min(300, drain*100)) fair-fight commit | Inside the V151/V177 projection gate: gap 1..3, drain >= 2, forfeit-OK (ourForfeit <= theirForfeit*1.25). Sits strictly below +400 coordinated-attack and +500 tiers.… | LIVE |
| V188 |  | DEPLOY-2 | CDSE:19 | VETO | — | Early gate at TOP of evaluateSite: returns -900 when deploying ability>=1 character to a Death Star site with Set Your Course For Alderaan front-side in play. Ability-0… | LIVE |
| V193-deploy-route | V193 | DEPLOY-2 | DE:1976 (#evaluate) | BANDED (RANK) | +400 (playbook weights.deployFlipGateSite; 400 default) | Parent deploy-action route: steer one body to the analyzer-named flip-gate site (Rando doesn't control it, flip card waits in Reserve). GATE NOTE (07-13 resolution): parent route currently LACKS the CS route's ability/cost body gate. Objective-XOR intact: the intel stays V193-intel (OA). Marker `"V193 FLIP-GATE CONTROL: steer one body to"` ×1/bot. Fixture: TODO `B0_FlipGate_V193_ParentDeploy` | LIVE (split 2026-07-13) |
| V193-cs-route | V193 | DEPLOY-2 | CSE:2288 (#evaluateDeployLocation) | BANDED (RANK) | weight+1600 (~2000 total) — deliberate offset to dominate the CS anti-hold stack (V67ah -350 + V113 -300 + V24.15 -80 + V29 GROUND -200 + CONCENTRATE -100) plus a reinforced hot-drain competitor; boundary math in code comment, replay vugpape5lw1bc7rq | Child destination route WITH ability/cost gate; FS steer-exempt; added 2026-07-09. NOT magnitude-lockstep with the DE arm BY DESIGN (see §3). Marker `"V193 (CS) FLIP-GATE CONTROL: steer one ability body"` ×1/bot. Fixture: `B0_FlipGate_V193_Bunker` (named, fixture spec 07-12) | LIVE (split 2026-07-13) |

### deploy-attach — 25 rules
*Weapons, devices, pilots, ships: attach/aboard gates.* Target owner: DEPLOY pipeline / DEPLOY-3 (gate = V158; weapon pulls also gated by V185/V120).

**V270 owner note:** `DeployPilotShipPolicy` now also owns the untagged generic pilot-candidate quality ladder and simultaneous-pilot Star Destroyer guard, plan, and matching ladder. Both `CardSelectionEvaluator` mirrors remain fact and control adapters; AMSD remains independently PULL-owned. The legacy Star Destroyer block is still an additive set-then-add score costume, not a hard-veto operation.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V30 |  | DEPLOY-3 | DE:4206 | BANDED | +/-1000 tier (+1000 pair deploy, +300 pilot-onto-ship, -500 AMSD soft-prefer) | Matching pilot+ship deploy-together | LIVE |
| V33-block | V33 | DEPLOY-3 | DE:4209 | VETO | — | DEAD LEDGER (version table col D: CONSOLIDATED into V158). Commented-out one-weapon hard block at DeployEvaluator 4103-4140 plus consolidation banner comments… | LIVE |
| V33-named-weapon | V33 | DEPLOY-3 | DE:4209 | ORDERING | — | LIVE arm at DeployEvaluator 4260-4318: character-specific (named) weapons deploy before generic ones. Other V33 hits NOT this arm: MoveEvaluator 5 = buddy-break arm… | LIVE |
| V35.5 |  | DEPLOY-3 | DE:4207 | BANDED | weak-starship deploy penalty (magnitude at DeployEvaluator ~5243) | Don't deploy weak starships to systems where opponent ship power is | LIVE |
| V35.6 |  | DEPLOY-3 | DE:4207 | BANDED | +300 named-pilot / ability>=4-at-system tier | FOUND IN SCOPE, NOT IN VERSION TABLE (no V35.6 row). Ship ability check: need >=4 ability at system; +300 named pilot bonus. DeployEvaluator ~5138-5196. Plausibly… | LIVE |
| V40.1 | V40 | DEPLOY-3 | DE:5758 | BANDED | +300 | Pilot deploys aboard a ship (PILOT ABOARD bonus) | LIVE (manifest gap — added batch 0) |
| V67am |  | DEPLOY-3 | ATE:4496 DE:3791 | ORDERING | — | Bump of V67m +200->+600 so reserve weapon pulls outscore hand-deploy of same class. Adjustment of V67m; same | detection LIVE (DE), +600 grant ABSORBED→V192 weapon tier |
| V67an |  | DEPLOY-3 | ATE:3414 DE:4388 | ORDERING | — | Weapon-swap-to-free-matching-slot: +400 transfer wrong weapon to buddy, +150 ambiguous | LIVE |
| V67ao |  | DEPLOY-3 | ATE:4499 DE:3685 | ORDERING | — | No-soft-penalty ordering policy: locations outscore character pulls naturally via V67ai tiers; hard-block only where action would FAIL. Mostly ladder documentation +… | LIVE |
| V67aq |  | DEPLOY-3 | ATE:2019 CSE:9566 DE:3786 | VETO | — | DEAD LEDGER (col D: DEAD, commented out -> V158). DeployEvaluator 4144-4256 is the //-commented block (10 hits) + 4 live banner/mirror comments (3584, 3995, 3996, 4104).… | LIVE |
| V67ar |  | DEPLOY-3 | ATE:2029 CSE:8927 DE:3685 | VETO | — | One-weapon rule on reserve-pull path (ActionTextEvaluator, mirrors dead V67aq): -9999 when zero unarmed | LIVE |
| V67ay |  | DEPLOY-3 | CSE:8925 | VETO | — | One-weapon rule at reserve-deck SELECT step: -9999 to weapon-category candidates when all friendlies armed; location candidates | LIVE |
| V67m |  | DEPLOY-3 | ATE:4496 DE:3726 | ORDERING | — | Weapon-pull priority in the deploy ladder; original +200, live value +600 via V67am | LIVE |
| V70 |  | DEPLOY-3 | ATE:2029 CSE:8362 DE:4402 | VETO | — | One-weapon-per-character block on CardSelectionEvaluator evaluateUnknown path + 'Deploys on X' criteria helpers. Lowercase helper v70CheckWeaponDeviceBlock NOT counted… | LIVE |
| V72 |  | DEPLOY-3 | ATE:3517 CSE:9706 | ORDERING | — | WEAPON REDISTRIBUTION: transfer from double-armed character to unarmed friendly preferred over | LIVE |
| V86 |  | DEPLOY-3 | CSE:2049 DE:1405 | BANDED | -1500 block / +300 tier | Neimoidian pilots deploy only to capital ship while one is on table (Invasion objective). Species/Icon via Filters, no name | LIVE |
| V86.1 |  | DEPLOY-3 | CSE:2051 DE:1621 | BANDED | n/a — scoping guard, no own score | Identifiable-target guard on V86: only act when action text names the target (aboard/to/on). Travels with | LIVE |
| V115 |  | DEPLOY-3 | ATE:2019 CSE:9566 DE:4212 | VETO | — | DEAD LEDGER (col D: DEAD, commented out inside V67aq block -> V158). CSE 9027/9046 are LIVE: V115 widened v70 helper visibility so DeployEvaluator can call them — that… | LIVE |
| V120 |  | DEPLOY-3 | ATE:2015 | VETO | — | 'Deploy <NAME> from reserve' weapon-pull hard block -9999 when no criteria-matching unarmed friendly. Revised | LIVE |
| V121 |  | DEPLOY-3 | CSE:2049 | BANDED | -1500 block / +300 tier | Location-pick mirror of V86: non-capital-ship candidate -1500, capital ship | LIVE |
| V125 |  | DEPLOY-3 | ATE:2016 | VETO | — | Bidirectional title-contains fix for V120's match; adjustment tag, travels with | LIVE |
| V149 |  | DEPLOY-3 | ATE:4499 DE:3685 | VETO | — | Lightsaber pull requires unarmed [Warrior] ability>=4; else -2000 instead of | LIVE |
| V158 |  | DEPLOY-3 | ATE:945 DE:4205 | VETO | — | LIVE unified one-weapon deploy gate (-9999), replaced V33-block/V67aq/V115. Includes NO-WIELDER lightsaber guard (V180 fixed persona scan). Hub for the DEPLOY-3 dead… | LIVE |
| V180 |  | DEPLOY-3 | ATE:1025 | VETO | — | Persona-scan fix inside V158's NO-WIELDER guard (Young Skywalker IS Luke). Single anchor; travels with | LIVE |
| V185 |  | DEPLOY-3 | DO:21 DE:810 | VETO | — | Weapon-pull -2000 block when no in-play character passes the weapon's own matching-character filter (V67h WILL_SUCCEED branch). Version table col D: first-pass oracle… | UPDATED 2026-07-11 (return-value bug fix — gate verdict now actually returned) |

### solo-formation — 15 rules
*Steve's four laws + solo/buddy discipline (VETO-class).* Target owner: FormationSafety (shared common/, both bots) — the LAWS (veto/score PRODUCERS). The veto ENFORCEMENT plumbing (EvaluatedAction hardVeto OR-merge + CE finalizer) is SVC-SAFETY infrastructure, NOT owned here — see the FS-enforcement row under loop-safety (gate 5e290559c correction 3, 2026-07-13).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V29.5-buddy | V29.5 | DEPLOY-2 | CSE:3089 (#evaluateDeployLocation; drifted from 2828) | BANDED (SCORE) | +40 own location / -150 solo to opponent's EMPTY location / -100 opp location with enemies, no friendlies / +10 opp location with friendlies | GENERAL BUDDY SYSTEM: group-topology scoring, all decks; only penalizes solo deploys to OPPONENT locations (V113 covers the rest, implemented inside this try-block). Unrelated to the V29.5 shield-plumbing arm. Marker `"V29.5: GENERAL BUDDY SYSTEM"` ×1/bot. Fixture: TODO `B0_V295_Buddy_TopologyTiers` | LIVE (verified 2026-07-13) |
| V27-buddy-protect | V27 | MOVE | ME:880 (#evaluate, block 880-925) | BANDED (SCORE) | -150 leaves ally alone / -250 enemy present, ally can hold / -400 enemy overpowers ally | Move-away penalty when the move strands a buddy; V59 DOOMED escape overrides (+200) when the location is already lost. Minted here 2026-07-13 (gate m00288) — formation policy, not budget; the explicit re-home the 07-13 addendum queued. Marker `"V27 BUDDY PROTECT"` ×2/bot (score + warn log lines; count stated). Fixture: TODO `B0_V27_BuddyProtect_StrandTiers` | LIVE (minted 2026-07-13) |
| V113 |  | DEPLOY-2 | CSE:998 | BANDED | -300 solo ability>=3 character at ANY location | CSE 2913-2933 live (implemented inside V29.5's try-block); hits at 976/1004 are boundary-math comments in the V169/V171 blocks (stays under V169 PROTECT | LIVE |
| V156 |  | DEPLOY-2 | CDSE:580 (#computeTeamViability) | BANDED (SCORE) | -600 SOLO HOLD (ability<6 unarmed solo at BG, turns 1-2) / +250 SOLO OK PREFER BUDDY (can solo but buddy affordable) | Formation policy, not generic siting (gate ruling): the arm stays PHYSICALLY in the shared CDSE hub, the RULE is owned by solo-formation. JOIN-GROUP twins (ME + CSE dest, shared isV156FlipNotReady) travel with it. Rewritten in place 2026-06-25; old flat -300 guard //-commented. Marker `"V156 SOLO HOLD"` ×1 (shared common/ — serves both bots, no mirror). Fixture: TODO `B0_V156_SoloHold_WeakBody` (boundary interplay: `B0_L3_NoBuddy_Raw350_Soft800`) | UPDATED 2026-07-07 (solo stack-math 4b76cb611); verified 2026-07-13 |
| V169-urgency | V169 | DEPLOY-1 | DE:976 (#evaluate, block 943-984) | BANDED (SCORE) | +500 on the top-level Deploy action | OPENS deploy because a formation needs rescue: any friendly-occupied location outpowered → boost "Deploy" itself; the destination is picked downstream by V169-destination. Marker `"V169 PROTECT URGENT: our characters at"` ×1/bot. Fixture: TODO `B0_V169_Urgency_DeployOpens` | LIVE (split 2026-07-13) |
| V169-destination | V169 | DEPLOY-2 | CSE:975 (#evaluateDeployLocation, block 934-988) | BANDED (SCORE) | +800 + min(300, excess*30) = +800..+1100 | CHOOSES the endangered formation: deploy-buddies bonus at the outpowered site; fires ONLY when the V172 protect gate passes. Shared outpowered-helper (CSE ~5611) also feeds the retreat arm. Marker `"V169 PROTECT (deploy)"` ×1/bot. Fixture: TODO `B0_V169_Destination_OutpoweredSite` | LIVE (split 2026-07-13) |
| V172-protect-gate | V172 | DEPLOY-2 | CSE:947 (#evaluateDeployLocation, block 947-988) | VETO-class gate (CONSTRAINT) | no own points — withholds V169-destination +800..+1100 | REINFORCEABILITY BRAKE: a rescue wave must be able to close the gap (this card + v173 affordable wave >= deficit - 4), else the site is unsavable by deploys and the move-phase RETREAT path owns it (the 416x corpse-conveyor incident). Marker `"V172 PROTECT GATED"` ×1/bot. Fixture: TODO `B0_V172_ProtectGate_UnsavableRetreat` | LIVE (split 2026-07-13) |
| V172-contact-gate | V172 | DEPLOY-2 | CSE:1077 (#evaluateDeployLocation, block 1047-1155) | VETO-class gate (CONSTRAINT) | no own points — withholds V171 +600 | WINNABILITY GATE on direct contact: hit-aware projection (projected - hitDiscount) must reach theirEff - 2 with the wave affordable after reserves, else assemble adjacent. Marker `"V172 CONTACT GATED"` ×1/bot. Fixture: TODO `B0_V172_ContactGate_WaveShort` | UPDATED 2026-07-11 (hit-aware, character-gated); split 2026-07-13 |
| V172-solo-dominance | V172 | DEPLOY-2 | CSE:1138 (#evaluateDeployLocation, block 1127-1142) | BANDED (SCORE) | **+600** — `action.addReasoning(..., 600.0f)`, verified live in BOTH mirrors 2026-07-13 | SOLO DOMINANCE (Steve ruling 2026-07-11): (our power here + this body) >= 2x their weapon-adjusted effective power → buddy/wave gate WAIVED and the arm itself SCORES +600 (fires INSTEAD of V171 in the else-if chain). CORRECTED 2026-07-13 (gate 5e290559c #1): the prior "awards no siting points of its own" claim was FALSE for this arm — true only of the two gates. Marker `"buddy gate waived, Steve 2026-07-11"` ×1/bot (`"V172 SOLO DOMINANCE"` rejected: 3 hits/bot). Fixture: `B0_Dominance_Tyranus8_Leia3` (named, fixture spec 07-12) | LIVE (split + score corrected 2026-07-13) |
| BATCH1b-two-weak-solos | FORMATION | SVC-SAFETY | CSE:6282 | BANDED | -800 (penalty not veto — repositioning stays possible) | Weak mover relocating SOLO to an uncontested EMPTY site while leaving a lone weak buddy at an uncontested origin (Chiraneau/Ozzel escape; boundary 327.5 → -472.5 loses to Pass) | NEW since manifest (post-2026-07-06) |
| FS-L1-abandon | FORMATION | SVC-SAFETY | FS:150 CSE:6276 | VETO (hardVeto) | un-outvotable; OR-merged on EvaluatedAction | L1: never move the last buddy away leaving ONE weak (ability<4 weapon-adj) body behind at a contested origin; exempt if leftover ability>=4, origin doomed (gap>=6), or origin uncontested | NEW since manifest (post-2026-07-06) |
| FS-L2-no-destiny-battle | FORMATION | SVC-SAFETY | FS:107 BE:426 BE:700 | VETO (hardVeto) | un-outvotable | L2: never voluntarily initiate battle where total ability < 4 (engine BattleDestiny truth — zero normal destiny draws); fallback arm vetoes battle phase when NO contested location reaches 4 | NEW since manifest (post-2026-07-06) |
| FS-L3-solo-deploy | FORMATION | SVC-SAFETY | FS:199 CSE:2161 | VETO + BANDED | hardVeto when pair formable this phase is budget-starved; -800 NO-PLAN when no buddy plan exists (07-12 rewrite, deadlock-free) | L3: never deploy a weak solo when a buddy is in hand/affordable; exemptions: spies, 2x dominance (V172 law), flip-gate steer (V193-class) | NEW since manifest (post-2026-07-06) |
| FS-L4-solo-charge | FORMATION | SVC-SAFETY | FS:122 CSE:6272 | VETO (hardVeto) | un-outvotable | L4: never move a weak (ability<4 weapon-adj) solo character into an enemy-held location; exempt: spies, uncontested destination, >=2x effective dominance | NEW since manifest (post-2026-07-06) |
| FS-pull-route | FORMATION | SVC-SAFETY | ATE:5672 ATE:5742 | VETO + BANDED | hardVeto (L4/L3) / -800 NO-PLAN | Location game-text pulls ("[download] X here") force the destination and bypass CS siting: resolve the pulled character from Reserve, run FS deploy checks with dest=source location; flip-plan exemption (first-name token, batch 1a/1c fixed the inert matcher) | NEW since manifest (post-2026-07-06) |

**FS-enforcement RE-HOMED 2026-07-13** (gate 5e290559c correction 3): the hardVeto OR-merge (EvaluatedAction.mergeFrom) + CombinedEvaluator bucket-walk filter / V67bc epilogue skip / final-selection enforcement are GENERIC constraint infrastructure serving every producer domain, not a formation-owned rule — the row now lives under loop-safety. The laws above PRODUCE vetoes; the plumbing that enforces them belongs to SVC-SAFETY.

### battle-initiation — 12 rules
*Whether/where to initiate battle.* Target owner: BATTLE pipeline / BATTLE-1 (TRAP: ATE V25 tier block + BE both score initiation — preserve the SUM).

**V208 owner note:** `BattleDecisionPolicy` is the canonical owner of the former `BattleEvaluator` stream for both bots. The V25 `ActionTextEvaluator` ladder intentionally remains a separate contribution, so the two streams still sum. Row anchors below describe the historical source locations; the shared policy is the live score owner for the BE arms.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V22.4-initiation-guard | V22.4 | BATTLE-1 | BE:16 | BANDED | -800 danger / location-tier scoring | Location-specific battle evaluation + suicidal-location catastrophic-power guard (BattleEvaluator 146-551). Referenced by V164a boundary | LIVE |
| V25-power-tier | V25 | BATTLE-1 | ATE:2191 | BANDED | tiered initiation scoring (SUICIDE/CRUSH/FAVORABLE/MARGINAL/EVEN/UNFAVORABLE) | ATE 4098-4189 INITIATE BATTLE tier ladder incl. low-reserve caveat. Other V25 arms (other owners, per §8): concede RandoCalAi 495 (1, SVC-SAFETY); pilot lock ME 360-371… | LIVE |
| V29.9-barrier | V29.9 | BATTLE-1 | BE:18 | BANDED | Barrier-risk initiation penalty (risk-scaled) | BattleEvaluator 259-305: Rebel Barrier risk when strength concentrated in Vader. Interacts with V35 Vader-expendable reduction. Remaining V29.9 hits mapped in the… | LIVE |
| V29.9-huntdown-aggro | V29.9 | BATTLE-1 | BE:18 | BANDED | armed-Vader battle aggressiveness bonus | EXTRA ARM found in scope (handoff §8 names 'Barrier-risk vs Hunt-Down-aggro'): BattleEvaluator 311-324, armed Vader should FIGHT. Plausibly BATTLE-1 alongside the… | LIVE |
| V34 |  | BATTLE-1 | BE:17 DE:3218 ME:1414 | BANDED | +100 base / +150-200 must-fight raises | HOMED arm = BattleEvaluator (base-score raise, weapon-adjusted advantage, MUST-FIGHT +200 vs drains). Other arms: MoveEvaluator 2269-2348 destination-aware contest bonus… | LIVE |
| V35-inquisitor-destiny | V35 | BATTLE-1 | BE:326 | BANDED | Inquisitor battle-destiny bonus stack | BattleEvaluator 329-374 HUNT DESTINY. §8 explicitly routes Inquisitor destiny to BATTLE-1. Remaining V35 hits mapped: BE 289-296 (2) = Vader-expendable arm (next entry);… | LIVE |
| V35-vader-expendable | V35 | BATTLE-1 | BE:326 | BANDED | Barrier-risk reduction for expendable Vader | EXTRA ARM found in scope: BattleEvaluator 289-296, reduces V29.9 barrier risk in Hunt Down (Vader expendable). Plausibly BATTLE-1, must travel with | LIVE |
| V61-reserve | V61 | BATTLE-1 | BE:17 | BANDED | -800/-400/-200 low-reserve destiny penalties | BattleEvaluator 550-657 RESERVE DECK GUARD (empty reserve = near-veto -800). V61b bypass sits inside this chain. Other V61 hits: RandoCalAi 636/684 (2) = saga epic-event… | LIVE |
| V61b |  | BATTLE-1 | ATE:231 BE:17 DC:313 FAE:18 | VETO | — | Overpower bypass predicate: margin >= 8 skips the entire V61 penalty chain (BattleEvaluator 627/644). Uses lowercase v61BestOverpow local (not grep-counted).… | LIVE |
| V76 |  | BATTLE-1 | BE:16 | VETO | — | BattlePredictor gate: winRate < 35% or avgDamageTaken >= 10 hard-blocks initiation; else falls through to V22.4/V29.7 tiers. BattleEvaluator | UPDATED 2026-07-11 (relative pyrrhic threshold) |
| V164a |  | BATTLE-1 | BE:17 CSE:874 | BANDED | +40 favorable-battle bonus, guard-gated (ABILITY_BATTLE_MAX_POWER_DEFICIT 2.0) | Ability-parity initiation at BattleEvaluator 545-553; deliberately +40 so V61 reserve guards and V22.4 danger still dominate. CSE hits 864/869/5887/5891 are comment… | LIVE |
| V176 |  | BATTLE-1 | CSE:6064 DE:983 | BANDED | -800 deploy brake / +1 initiation-fee reserve | Two parts one tag: (a) CSE 5695 wave-projection +1 Force reserve for initiation fee, (b) DeployEvaluator 964-1006 umbrella brake -800 when winnable battle waiting and… | LIVE |

### battle-weapons — 19 rules
*Weapons-segment window: fire, interrupts, targeting, destiny mods.* Target owner: BATTLE pipeline / BATTLE-2.

**V208 owner note:** `BattleWeaponsPolicy` now owns Force Push exchange/exclusion, fire-before-throw, redraw, generic fire/cancel/draw, V51 already-hit, V36 targeting, and final V38.3 self-target scoring. The mirrored evaluators retain stock fact recognition and the other card-specific battle-interrupt arms. Row anchors below are historical for the migrated slice.

**V290 owner note:** `TargetSelectionPolicy` now owns the mirrored generic target base and target-value arithmetic: beneficial own/opponent ownership, beneficial power/unique value, harmful opponent ownership, V51 undercover-spy priority, outside-battle power value, and harmful unique value. `BattleWeaponsPolicy` remains the unchanged owner of V51 already-hit, V36 destiny bands/priorities, and V38.3 harmful self-targeting. Both adapters retain routing and recognition, all card/blueprint/game/destiny/title observations, catches, logs, candidate order, and one final per-candidate application. The `TARGET-base` score is tagged initial state, not an additive reasoning operation. No engine metadata or player-choice code moved.

**V268 owner note:** `BattleActionTextPolicy` retains only V144's battle-freeze arm. The IAYF-search arm of V144, the V147 Lost-Pile search gate, and the V155 mode-1 pull-save gate are now owned by `PullSpecificActionPolicy` under pull-search.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V29.6-blaster-rack | V29.6 | BATTLE-2 | ATE:4006 | VETO | — | ATE 4193-4262: rack weapons ONLY at end of battle to save from hit/forfeit characters; proactive racking blocked. Header co-tagged V29.6/V29.11. OTHER V29.6 arms under… | LIVE |
| V29.9-IHYN | V29.9 | BATTLE-2 | ATE:2217 | BANDED | +300 in-battle / -200 save-for-battle | EXTRA ARM found in scope: ATE 2720-2768, I Have You Now played during battle (Vader mega boost). Battle-interrupt timing rule, plausibly | LIVE |
| V29.10 |  | BATTLE-2 | ATE:2676 | ORDERING | — | Lightsaber throw scores below fire (shared 2614 header with V29.12; throw at 2631) + a Hatred-card cancel-gametext sub-behavior at 2635. Same tag, both battle-card | LIVE |
| V29.11 |  | BATTLE-2 | ATE:4383 DE:4372 | VETO | — | NOT in version table (absorbed: its hardcoded 2nd-weapon stack was replaced by V67aq then V158). Live remnants: ATE 4193 shared Blaster Rack header (live, co-owner with… | LIVE |
| V29.12-fire | V29.12 | BATTLE-2 | ATE:2675 | ORDERING | — | ATE 2585-2629: Fire (300) MUST outscore Throw (200) so the saber isn't lost before firing. Other V29.12 arms: MoveEvaluator 930/1171 (2) = Vader-leaves-Castle hunter +… | LIVE |
| V35-hatred-lifecycle | V35 | BATTLE-2 | ATE:2676 | BANDED | +250 in-battle / +150-200 hatred placement / -100 save | ATE 2773-2830 FMFTD USED (place hatred) / LOST (add destiny) mode scoring. §8 homes hatred lifecycle in BATTLE-2. Uses RandoConfig SCORE_FMFTD_* constants (RandoConfig 1… | LIVE |
| V35.1-self-barrier | V35.1 | BATTLE-2 | ATE:6467 | VETO | -9999 | Never barrier our OWN character (You Are Beaten can target any character) | LIVE (manifest gap — added batch 0) |
| V35.2-rack | V35.2 | BATTLE-2 | ATE:4409 | BANDED | +80 save (char in battle) / -500 rack (char not at battle) | Weapon racking during battle only from characters actually AT the battle | LIVE (manifest gap — added batch 0) |
| V35.4-you-are-beaten | V35.4 | BATTLE-2 | ATE:3037 | BANDED | +150 in-battle / -200 save | You Are Beaten used for attrition during battle, not wasted outside battle or on undercover spies | LIVE (manifest gap — added batch 0) |
| V36-weapon-targeting | V36 | TARGETING | CSE:7420 | BANDED | hit-probability tiers (EASY/MARGINAL/LIKELY MISS) + priority targets +80..+300 (Padme +300, destiny-adder +100, Jedi hunt +80) | CSE 6860-6923 DESTINY-BASED WEAPON TARGETING. NOT DEPLOY-2 — same home as the V51-targeting arm (§8 calls that arm 'targeting'); exact section name for the… | LIVE |
| V37-redraw | V37 | BATTLE-2 | ATE:2251 | VETO | — | EXTRA ARM found in scope: ATE 3196-3231 cancel/redraw destiny gate — keep destiny >= 3, redraw only when low. Battle-destiny manipulation, plausibly BATTLE-2. Tag reuse… | LIVE |
| V37.1-hatred | V37.1 | BATTLE-2 | ATE:2744 | BANDED | -600 opponent-turn hatred block | ATE 2645-2717: place hatred only on OUR turn. Other V37.1 arm: MoveEvaluator 1762-1779 (8 hits) = STAY AND CRUSH/FIGHT -9999 hard-stay (MOVE per | LIVE |
| V37.2 |  | BATTLE-2 | ATE:2989 | VETO | — | Stunning Leader defensive-only: HARD BLOCK when we initiated; -200 save outside battle; bonus when defending outnumbered. ATE | LIVE |
| V37.3 |  | BATTLE-2 | ATE:6302 | VETO | — | Sense self-cancel block: NEVER cancel our own interrupts. ATE | LIVE |
| V51-targeting | V51 | TARGETING | CSE:7407 | BANDED | -500 ALREADY HIT / +500 KILL SPY (Force Lightning / Trample) | §8 arm 'targeting'. CSE 6847-6856, weapon/interrupt target choice. Not | LIVE |
| V67bi |  | BATTLE-2 | ATE:1444 | VETO | — | Force Lightning self-target hard block when no opponent character in play (ATE 1370-1405). Card-title list grows per replay (documented exception to the no-name-lists… | LIVE |
| V67u |  | BATTLE-2 | ATE:1962 | VETO | — | Force Push exchange-with-Force-Pile waste block, incl. source-detected defense-in-depth catch (ATE 1888-1935, shared header with | LIVE |
| V144-freeze | V144 | BATTLE-2 | ATE:1057 | BANDED | +500 | You Are Beaten battle-freeze mode during the battle phase. The separate mode-2 IAYF-search veto is owned by pull-search. | CONSOLIDATED V268 |
| V175 |  | BATTLE-2 | ATE:2677 CSE:4342 | BANDED | kill shot +400+power*40 cap 900; substitute delta*60; our-char -100 | ATE 3060-3150 = kill-shot + substitute-destiny arms (BATTLE-2). CSE 4039-4066 (3 hits) = protect-battle-interrupts-from-force-loss-picker arm — force-loss territory;… | LIVE |

### battle-forfeit — 13 rules
*Damage segment: forfeit picker + loss-vs-forfeit.* Target owner: BATTLE pipeline / BATTLE-3.

**V208 owner note:** `BattleForfeitFacts` and `BattleForfeitPolicy` now own optional V22.4/V29.13, V154, V118, V150, V22.3, and the V159/V161/V178 ladder. The obsolete bot-local `v159ForfeitScore` helpers are deleted. V206 FORCE-LOSS remains between the policy's before-route and after-route streams. Standalone mandatory-forfeit nudges, V67be's explanatory no-op, and V45 remain with their existing owners; row anchors below are historical for the migrated slice.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V22.3-damage | V22.3 | BATTLE-3 | CSE:2847 | ORDERING | — | Forfeit-characters-before-pile/hand-loss (CSE 4275-4992 incl. the -40/-80/-120 forfeit-first penalties and the V67be restoration comments). Other V22.3 arms under same… | LIVE |
| V22.4-forfeit | V22.4 | BATTLE-3 | CSE:2955 | BANDED | optional-forfeit rework (forfeit good while damage remains) | 7 hits at CSE 4196-4679 = the version-table V22.4 behavior (optional forfeit rework, now dominated by V159 ladder). 1 hit at CSE 2694 is a V22.4+V67bn… | LIVE |
| V37-protect | V37 | BATTLE-3 | CSE:4649 | VETO | — | High-value character forfeit protection, applied in both optional and non-optional paths via V67t/V139 (CSE 4346-4480, 'never forfeit' semantics). Other V37 arms: ME… | LIVE |
| V45 |  | BATTLE-3 | RCA:574 | VETO | — | Never forfeit when ALL cards immune to attrition — pass instead (RandoCalAi 547-551). SAME-TAG REUSE: ATE 1696-1784 (6 hits) 'V45 AMSD UNAFFORDABLE' is an unrelated… | LIVE |
| V67be |  | BATTLE-3 | CSE:5045 | ORDERING | — | Removal of V67y from the combined battle prompt so V22.3 forfeit-first dominates again (CSE 4739 no-op block; only 1 line carries the literal V67be). KIND =… | LIVE |
| V67y |  | BATTLE-3 | CSE:5045 | BANDED | +500 pile-loss / -500 hand-loss (standalone force-loss prompts only) | WARNING: all 6 tag occurrences sit inside the V67be no-op explanation block (CSE 4739-4759) documenting V67y's REMOVAL from the combined battle prompt. The surviving… | LIVE |
| V118 |  | BATTLE-3 | CSE:5008 | BANDED | +200 pile-loss / -500 non-hit forfeit when damage 1-2 and attrition 0 | Small-damage save-the-character rule (CSE 4702-4729). Genuine gray-area | LIVE |
| V139-live-nudges | V139 | BATTLE-3 | CSE:4768 | BANDED | -100 high-power / -300 valuable-unique | Residual protect nudges inside forfeit scoring (V37/V139 PROTECT prints); heavy-protect branches dead → V159 | LIVE (manifest gap — added batch 0) |
| V150 |  | BATTLE-3 | CSE:5181 | ORDERING | — | While attrition > 0, pile-loss penalty deepens -150 -> -500 so forfeits win until attrition satisfied (CSE 4959-4975 + comments 4875/5055). Ordering intent implemented… | LIVE |
| V154 |  | BATTLE-3 | CSE:4958 | ORDERING | — | Lose attached WEAPON first in lose-or-forfeit: +2000 (+2200 if host hit), engineered above V146 hit-forfeit +1500 (CSE 4652-4672). Global CardCategory.WEAPON | LIVE |
| V159 |  | BATTLE-3 | CSE:4560 | ORDERING | — | LIVE unified 4-step forfeit picker: shared helper v159ForfeitScore() (lowercase, NOT grep-counted — must travel with tag) at CSE ~9182+, called from both forfeit… | LIVE |
| V161 |  | BATTLE-3 | CSE:9722 | BANDED | immune-forfeit: 1500+savings*80-waste*30 vs cautious -500; solo-immune power-gap scaling | Branch inside the V159 ladder (CSE 9288-9299), boundary math documented in version table. Revised | LIVE |
| V178-forfeit | V178 | BATTLE-3 | CSE:4013 | BANDED | -10 pure tiebreaker on armed-character forfeit score | CSE 9217 inside the V159 picker's normal-path returns. Other V178 arm: CSE 4071-4103 (3 hits) protect-attached-weapon-holders in force-loss picks (FORCE-LOSS per §8… | LIVE |

### move — 49 rules
*Stay/flee/hunt/transit ladder, retreat, consolidation.* Target owner: MOVE pipeline (T4 clobber ladder; dual-utility semantics).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V25-pilot-lock | V25 | MOVE | ME:627 | VETO | — | §8 arm: NEVER move a pilot off their ship (-500 block, ME | LIVE |
| V25-shuttle-defense | V25 | MOVE | ME:627 | BANDED | defensive bonus tier | FOUND IN REGION (not in §8's V25 arm list): defensive shuttle bonus only when opponent has 2x our power at destination (ME 1497-1529). Small MOVE arm of V25; labeled so… | LIVE |
| V27.2 |  | MOVE | ME:866 | BANDED | threshold tweak, no own magnitude | Comment-only anchor (ME:580): 'more permissive buddy protection for MOVES' — threshold relaxation on the V33/buddy family. No own score | LIVE |
| V29-move-reserve | V29 | MOVE | ME:725 | BANDED | -60..-150 tier | §8 arm: FORCE RESERVE CHECK FOR MOVES — low Force = -100 (-150 with critical interrupt in hand), mild -60 (ME 447-523). DTF/grabber | LIVE |
| V29.12-hunt | V29.12 | MOVE | ME:1285 DE:2901 | BANDED | hunt grouping tier | Hunter arm per handoff §8: Vader must leave Castle and hunt (ME 930, 1171 mirror-comment) + deploy-characters-with-Vader grouping (DE 2713-2816; functionally deploy-side… | LIVE |
| V29.13 |  | MOVE | ME:23 | BANDED | drain-delta -40/pt, group +/-100..250 | MOVE home per §8 (drain-delta + grouping): BAD/GOOD DRAIN SITE delta scoring (~-40*delta/+80/+40, ME 2075-2130) + HUNT GROUP move with Vader (+X/-100..-250 scatter, ME… | LIVE |
| V31-consolidation | V31 | MOVE | ME:1128 | BANDED | +200 tier | POST-FLIP MOVE CONSOLIDATION: leave weakest objective location to reinforce stronger (+200). Unclaimed: DE 6 = pre-flip spread / post-flip reinforce deploy arm -> DEPLOY… | LIVE |
| V31-move | V31 | MOVE | ME:1128 | BANDED | post-flip move consolidation away from weakest objective location | MoveEvaluator 783-841. Not in §8; labeled so V31's 10 hits all | LIVE |
| V32-move | V32 | MOVE | ME:24 | VETO | — | ABILITY >= 4 move protection: move that drops site ability under 4 (no battle destiny) = -500 block; SOLO ESCAPE +50 when alone under 4. ME:376 documents an… | LIVE |
| V33-buddy-break | V33 | MOVE | ME:1058 RandoConfig.java | BANDED | -150 tier | Moving away drops site ability below buddy target (7) = -150 penalty, SKIPPED when site hopelessly outgunned (retreat allowed). RandoConfig hit = shared… | LIVE |
| V35.4-spy-flee | V35.4 | MOVE | ATE:3625 | BANDED | +250 flee / +150 enemy-presence | Spy-flee arms in the ATE Movement Actions dispatch (undercover spy avoids capture) | LIVE (manifest gap — added batch 0) |
| V36-move | V36 | MOVE | ME:2843 | BANDED | extra urgency moving to contest uncontested drains | MoveEvaluator | LIVE |
| V38.3-direction | V38.3 | MOVE | ME:44 | VETO | — | WRONG DIRECTION -9999 (move to empty loc while opponents exist; raised from -400) + CASTLE RETREAT block (ME 2376-2424; V111 BG-advance is the carved exception). §8 arm… | LIVE |
| V41 |  | MOVE | ATE:119 CSE:3417 ME:376 | VETO | — | WRONG DIRECTION -9999 + CASTLE RETREAT -9999 move-dest vetoes, plus CONTEST DEST +300/+500 banded sub-arm. Counts include 2 'V41-wrong-direction' prose tokens (ATE:111,… | LIVE |
| V47 |  | MOVE | CSE:3427 DE:1665 ME:109 | VETO | — | UPDATED 2026-07-06 (audit move-8): LANDO STAY -9999 move lock now gated on (a) objective-wants-here and (b) survivability; generic 'platform' substring removed. Arms… | UPDATED 2026-07-06/07-10 (CC-title list, objective + survivability gates); reserve-solo arm DELETED batch 1d |
| V49 |  | MOVE | DE:3188 ME:24 | VETO | — | NEVER land a starship at a site with no passengers (power 0 death trap) -9999; with passengers +10 allowed. V67f1 replaced the old assumed-passenger check (ME:2672).… | LIVE |
| V53-spy-follow | V53 | MOVE | ME:25 | BANDED | +/-300..500 tier | Undercover spy follows opponent: FOLLOW +500 / STAY -300 / REPOSITION +400 (ME 1586-1631; ME:382 is a V47-gate comment citing the +500). §8 arm split: spy-follow (here)… | LIVE |
| V53b |  | MOVE | ATE:2529 ME:44 | VETO | — | HIDDEN PATH MANDATORY JEDI TRANSIT: Safehouse->Corridor landspeed +9999 (a +9999 wearing a score costume = VETO-class mandatory), leaving Mapuzo +800; SAVE JEDI… | LIVE |
| V59 |  | MOVE | ATE:6766 CSE:2779 DE:100 ME:91 DPP:23 | VETO | — | Multi-arm, single-homed per slice. Arms: (a) MOVE arm = DOOMED LOCATION escape (ME 593-614, disables buddy-protect, flee valuable char); (b) MAINTENANCE HARD/HOLISTIC… | LIVE |
| V60-transit | V60 | MOVE | ATE:263 ME:90 | VETO | — | §8 arm split honored (SAME-TAG drift warning): Hidden Path transit arm = game-text 'Move Jedi here' +9999 (ATE 4266-4281) + landspeed-from-Corridor block -9999 (ME… | LIVE |
| V63 |  | MOVE | ATE:6398 CSE:435 | VETO | — | Two arms: LOST PILE EMPTY hard block on 'take character from Lost Pile' searches (ATE 5784-5814) + move-routing fix comment (CSE:428 routes 'Choose card to move to'… | LIVE |
| V64 |  | MOVE | CSE:1159 DE:944 | VETO | — | MOVE arm = SUICIDE MOVE hard-block tiers -1500/-1800/-2500 + SAFE DRAIN +150 / FAVORABLE +80 (CSE 6158-6209, 7 hits incl 5705 comment). Other arms: MAPUZO JEDI-ONLY… | LIVE |
| V65 |  | MOVE | CSE:6730 | BANDED | -1500 tier | Magnitude/threshold tuning of move-dest rules: threshold 8→7 (CSE:6179), -400→-1500 strengthening (CSE:6315), SMART WRONG-DIRECTION skip conditions (CSE:6523). All… | LIVE |
| V65a |  | MOVE | CSE:7092 | VETO | — | Explicit exemption arm of the V41 wrong-direction veto: our spy neutralizes the drain at the 'abandoned' location, so destination is not wrong-direction (CSE | LIVE |
| V65b |  | MOVE | CSE:7111 | VETO | — | Exemption arm of the V41 wrong-direction veto: suicide destination (opponent too strong for single Jedi) is excluded from wrong-direction marking (CSE | LIVE |
| V67aa |  | MOVE | CSE:7008 | VETO | — | HIDDEN PATH JEDI SUICIDE BLOCK -9999: pre-flip power-3 Jedi never transit into opponent power (CSE | LIVE |
| V67ae |  | MOVE | ATE:3625 AA:23 | BANDED | -300 tier | Game-text 'move to here' drain guard: destination 0 opp icons = -300 (free-retreat penalty). ActionAudit hits are dormant consolidation | LIVE |
| V67au |  | MOVE | CSE:3017 | BANDED | +400 tier | RETREAT-TO-DRAIN +400: over-contested source, move to safe adjacent with friendly icons and drain there (CSE 6024-6093 + 2756 | LIVE |
| V67d |  | MOVE | CSE:442 | ORDERING | — | Routing addition: 'Choose where to move <X> using landspeed' recognized as move-destination decision (CSE:435). Comment-only anchor, routing not | LIVE |
| V67e |  | MOVE | CSE:6441 | BANDED | +24..48 tie-breaker tier | Drain-potential tie-breaker on move destinations (~+24..48, drain*X). Explicitly designed to stay under safety rails (V67n | LIVE |
| V67f |  | MOVE | CSE:7068 | BANDED | -100 tier | SPY-ONLY destination penalty -100 (destination holds only opponent spies, no real | LIVE |
| V67g |  | MOVE | ATE:3636 CSE:6405 AA:23 | BANDED | -200..-450 tier | ZERO DRAIN dest -200 (strengthened from -25) + MOVE-FROM-DRAIN scaled penalty (was blocking at -432 pre-V67k exemption). ActionAudit hits are dormant-consolidation notes… | LIVE |
| V67k |  | MOVE | CSE:6480 | ORDERING | — | Transit-staging-site exemption to the V67g zero-drain/move-from-drain penalties (first-match | LIVE |
| V67n |  | MOVE | CSE:6441 | BANDED | +1500 safety-rail tier | TRANSIT STAGING DEST +1500 — deliberately dominates other Mapuzo destinations and V67e/ICON bonuses; V41 wrong-direction is skipped so this wins (CSE 6601-6606).… | LIVE |
| V73 |  | MOVE | ME:23 | BANDED | +400 tier (must beat V29.13 penalty) | Cantina<->Mos Eisley multi-drain shuttle +400 (only when source keeps chars); overrides V29.13 drain-delta penalty by design. Also dropped old <1/>=2 thresholds… | LIVE |
| V79b |  | MOVE | RCA:719 ME:28 | ORDERING | — | LIVE Death Star steering at the RandoCalAi MULTIPLE_CHOICE layer: pick orbit-Scarif else parsec closest to 7 — deterministic first-match choice, not additive scoring. ME… | LIVE |
| V85 |  | MOVE | ME:24 | VETO | — | Uncontested + lower-drain move = -2000 hard block (checks BEST adjacent drain vs current). Live, MoveEvaluator | LIVE |
| V87 |  | MOVE | ATE:121 | VETO | — | Pilot<->passenger capacity-slot swap hard block -3000. ATE:113 cites it as the loop-breaker dominance | LIVE |
| V91 |  | MOVE | ME:24 | BANDED | +600..800 tier | ESCAPE LANDED-SHIP TRAP: +600/+800 bonus to re-take-off or disembark from a landed-ship | LIVE |
| V103 |  | MOVE | ATE:1273 | BANDED | distance-scaled fallback tier | Verge parsec-detection repair + PARSEC FALLBACK distance-scaled bonus when Verge/DS scan fails but engine offers parsec prompt (ATE 1216-1361). Support logic for the V79… | LIVE |
| V111 |  | MOVE | ME:2923 | BANDED | +400 tier | Non-battleground -> battleground advance +400 (establish drain | LIVE |
| V134 |  | MOVE | ATE:549 | VETO | — | Odin Nesloor 5-Force floor: hard block (-9999) the destiny-draw transport in MOVE phase when forcePile < 5. Lives in ATE but is a move-phase | LIVE |
| V135 |  | MOVE | ME:109 | VETO | — | SELF-MOVE-TO-FRIEND requires companion: landing alone at 'friend' destination = | LIVE |
| V137 |  | MOVE | ME:24 | VETO | — | Move-side winnability gate: contested dest where even full group loses = -800/-1500; ANTI-SOLO BG sub-arm -500 (uncontested BG, opp can reinforce). Parity pair with V136… | LIVE |
| V137b | V137 | MOVE | ME:24 | BANDED | predicate widening, no own magnitude | Predicate extension of the hunt/grouping logic: 'hunter' = ANY Dark Jedi (Vader, Dooku/Tyranus...), not just Vader. No own magnitude; widens V137/V29.13 | LIVE |
| V166-move | V166 | MOVE | CSE:871 | BANDED | +200..+350 move-path contest | CSE 5884-5911 (move-destination route). NOT in §8's multi-arm table — flagging: V166 splits deploy vs move exactly like V169; labeled here so all 14 CSE hits | LIVE |
| V169-loop-soft-block | V169 | MOVE | ATE:41 ME:24 | VETO | — | §8 arm 'loop-soft-block'. UPDATED 2026-07-06 (audit cross-brain-1): single-owner -250 soft penalty in ATE with V169_SOFT_RETRY_BUDGET=3 per turn, then -100000 hard veto… | LIVE |
| V169-move-retreat | V169 | MOVE | CSE:934 | BANDED | +600 safe retreat destination; gates off V41's -9999 wrong-direction for endangered movers | §8 arm 'move-retreat'. CSE 5761-5791 (retreat mode detect), 5872-5881 (+600), 6613-6618 (V41 exempt). Not DEPLOY-2; labeled for hit | LIVE |
| V169-retreat | V169 | MOVE | CSE:934 ME:24 | BANDED | +600 retreat tier | UPDATED 2026-07-06 (audit cross-brain-1). Retreat mode: endangered mover -> safe destinations +600, V41 wrong-direction gated off (CSE 5761-5791, 5872-5881, 6613-6618 +… | LIVE |

### draw-count — 6 rules
*End-of-turn draw count + reserve targets.* Target owner: DRAW (DrE).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V24.10-dig | V24.10 | DRAW | DrE:24 | BANDED | +200 turns 1-2 / +150 turns 3-4 / +80 later (Piett dig) | DIG FOR PIETT: when Piett is in neither hand/reserve/play/lost he must be in the force pile - draw aggressively to find him (267-289). OTHER V24.10 ARM per sect8 ('AMSD… | LIVE |
| V42-draw | V42 | DRAW | DrE:24 | BANDED | +200/+400/+600 emergency by hand size (2/1/0 cards) | Emergency draw: hand <= 2 with force >= 1 and reserve >= 2 => (3-handSize)*200 bonus (185-193). Sibling arms listed on the V42-activation | LIVE |
| V58 |  | DRAW | DrE:22 | BANDED | +80 per surplus card, cap +400 (draw-down); reserve target is computed, not scored | Core draw policy: force pile above the computed reserve target => draw the surplus aggressively (316-331); calculateForceToReserve (FIX 20, 426+) is the threat-keyed… | UPDATED 2026-07-07 (maintenance floor with V67w) |
| V67w |  | DRAW | DrE:23 | BANDED | maintenance reserve via Icon.MAINTENANCE; reserve cap bumped 4 -> 8 | Maintenance detection switched from title matching to the engine's Icon.MAINTENANCE blueprint icon (491, 541) - feeds the same V58 reserve calc as V78.… | UPDATED 2026-07-07 (maint floor; Icon.MAINTENANCE basis) |
| V78 |  | DRAW | DrE:23 | BANDED | reserve +2 (cap raised to 10) inside V58's reserve calc | Imperial Arrest Order & Secret Plans retrieval-tax buffer: opponent has IAO => +2 Force to the reserve target (452, 484, 537, 543). An amount adjustment inside… | LIVE |
| V182 |  | DRAW | DrE:22 | VETO | — | Offensive force-banking: computeOffensiveBank (591-667) finds contested sites winnable with hand characters affordable within ~2 turns of banking; when armed and hand >=… | LIVE |

### force-loss-payment — 8 rules
*Which zone pays Force loss.* Owner: shared `ForceLossPolicy` (hub = V153) over one immutable `ForceLossFacts` snapshot; CSE retains stock routing and battle-only ordering.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V21-loss-protect | V21 | FORCE-LOSS | CSE:214 | VETO | — | FOUND IN REGION arm of V21: HARD BAN losing/forfeiting flip-required or objective-pullable cards (CSE 3756,3976,4145,4535-4544,5228-5237). UNCLAIMED V21 ARMS:… | UPDATED 2026-07-12 (cleanCardName strips icon tokens so [Special Edition] titles arm protection) |
| V25-loss-protect | V25 | FORCE-LOSS | CSE:1377 | BANDED | -300..-500 protect tier | §8 arm (lightsaber loss-protect): protect Hunt Down lightsabers in hand/combined-loss/unknown-loss (-500/-400/-300; +200 unknown-gain) — CSE… | LIVE |
| V28-DTF | V28 | FORCE-LOSS | CSE:4013 | ORDERING | — | Draw Their Fire arm: Force pile = interrupt ability, lose from reserve instead (heavy scaled penalty; live copy CSE 4115-4135, old copy 3890-3910 commented, + 3976… | LIVE |
| V28-dtf-force-pile | V28 | FORCE-LOSS | CSE:4013 | BANDED | heavy penalty protecting Force pile when Draw Their Fire active | Live copy CSE 4115-4135 + priority comment 3976; older copy 3890-3910 is //-commented. Different rule sharing the V28 tag — belongs to the force-loss cluster, not… | LIVE |
| V109 |  | FORCE-LOSS | CSE:4012 | BANDED | -300 tier | MY LORD: protect senators from loss/cost picks (-300) inside the loss-selection order (CSE 3825-3856, 3975 preserved-protections | LIVE |
| V153 |  | FORCE-LOSS | FLP | ORDERING | — | FORCE-LOSS hub: shared route-aware loss order, char/life-force tiers (protect chars when lifeForce>=4, survival mode <4), HAND FLOOR -700, PRIORITY CARD -100, THIN RESERVE -335. | CONSOLIDATED V206 |
| V175a |  | FORCE-LOSS | FLP | ORDERING | — | Turn-gate on battle-interrupt protection inside the standalone V153 order: protection starts turn 4; turns 1-3 lose the known interrupt before a blind reserve hit. V178-loss separately protects weapons. | CONSOLIDATED V206 |
| V178-loss | V178 | FORCE-LOSS | CSE:4013 | ORDERING | — | §8 arm split (forfeit vs force-loss): loss arm reranks wielded weapons zone 600 -> 150 (CSE 4071-4103). UNCLAIMED ARM: forfeit arm (CSE:9217, armed chars slightly… | LIVE |

**V289 owner note:** `ForceLossPolicy` now also owns the residual action-text loss/cost choices and the unknown-selection loss-category stream, followed by the V25 Hunt Down lightsaber `-300` arm; unmatched categories emit no operation and preserve the legacy neutral base. `PullActionPolicy` owns the parent action-text take-into-hand residuals, `PullSpecificActionPolicy` remains card-specific, and `PullTakeCandidatePolicy` remains the child candidate owner. Both bot adapters retain recognition, every GEMP read and scan, logs, catches, return/continue behavior, and exact first-match position. All moved operations remain additive.

### shields — 13 rules
*Shield pick tables, pacing, 4th-slot gate.* Target owner: SHIELDS engine (ShieldStrategy + ShieldFacts).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V29-shield-mix | V29 | SHIELDS | SS:12 | ORDERING | — | FOUND IN REGION arm of V29 (§8 lists only BESPIN + move-reserve): auto-play shield allocation cap + situational-shield preference (situational 250/80 vs auto-play,… | LIVE |
| V29.1 |  | SHIELDS | ATE:2582 SS:13 | BANDED | -40 pacing tier | Shield pacing: hold slots early to scout opponent (-40 per early | LIVE |
| V29.5-shield-plumbing | V29.5 | SHIELDS | CSE:1027 | ORDERING | — | CSE 8667-8974: isShieldSelectionByContent + DEFENSIVE SHIELD selection scoring path (ShieldStrategy lookup, fallback 100) + ARBITRARY_CARDS temp-ID handling. Same tag,… | LIVE |
| V51-battle-order-gate | V51 | SHIELDS | CSE:7407 | VETO | — | §8 arm 'Battle-Order-gate'. -9999 unless Rando occupies BOTH a BG site and BG system; +50/+200 EARLY-DEPLOY when met. UPDATED 2026-07-06 (occupiesBothTheaters predicate… | LIVE |
| V53-shield-priority | V53 | SHIELDS | SS:12 | ORDERING | — | §8 arm: shield priority order — grabber first (A Tragedy/Allegations +100), retrieval tax second (Aim High/Secret Plans +50); Battle Order/Plan downgraded IMMEDIATE ->… | LIVE |
| V102 |  | SHIELDS | RCA:1899 ATE:2589 SS:13 | VETO | — | K&D per-turn ACTIVATION CAP hard block (separate counter from shieldsPlayed; RandoCalAi tracks activations, ShieldStrategy holds counter, ATE | LIVE |
| V105 |  | SHIELDS | ATE:2612 CSE:8440 SS:14 | ORDERING | — | 4th-slot Trigger A (Battle Order/Plan when we occupy system+site BGs). UPDATED 2026-07-06 (Verge game deadlock fix): only pursue when preferred card is actually… | LIVE |
| V106 |  | SHIELDS | SS:14 CSE:9393 | ORDERING | — | 4th-slot Trigger B (CHYBC/Simple Tricks: opp drains non-BG + opp bg<2 + we occupy BG). CSE:8854 records the CSE-side copy was dropped per Steve 2026-05-20;… | LIVE |
| V107 |  | SHIELDS | ATE:2612 CSE:8440 SS:14 | ORDERING | — | 4th-slot Trigger C (Resistance/Ultimatum when opp can drain 3+). Shares the V105 policy plumbing (prefers4thSlot). One 'V107-preferred' variant token counted in | LIVE |
| V112 |  | SHIELDS | CSE:8388 | VETO | — | Battle Order/Plan occupation gate -9999 on evaluateUnknown path. UPDATED 2026-07-06: engine occupiesBothTheaters predicate (V51/V112 unified, helper at CSE ~8720); old… | LIVE |
| V117 |  | SHIELDS | CSE:8435 | ORDERING | — | Universal 4th-shield policy on evaluateUnknown: 3 shields on table => HOLD unless V105/V107 trigger names this exact shield (+2000 match / block otherwise). UPDATED… | LIVE |
| V124 |  | SHIELDS | ATE:2589 | ORDERING | — | K&D PARENT-action enforcement of the 4th-slot policy: 3+ shields on table and no V105/V107 trigger => don't activate K&D at all (ATE 2521-2560). Applies to both bots per… | LIVE |
| V129 |  | SHIELDS | ATE:1124 | ORDERING | — | Exclusion predicate: Anger/Fear/Aggression (light-side stacked-pile mirror) excluded from K&D-style shield logic; isKnDShieldPlay renamed | LIVE |

### pull-search — 36 rules
*Reserve-deck pulls/searches: scoring + dead-pull verdicts.* Target owner: PULL ENGINE (hub = V192 in ATE since T4.2); facts stay SVC-ORACLE (DeckOracle).

**V268 owner note:** `PullSpecificActionPolicy` owns the extracted card-specific PULL actions. `PullSelectionCandidatePolicy` is the shared route-specific candidate owner, but each operation keeps its registry domain: V186 is SETUP_STARTING; generic gain/search value is PULL_SEARCH; loss protection is FORCE_LOSS_PAYMENT; Hunt Down, Cloud City, and AMSD are DECK_PLAYBOOK; and deployment-plan matching is DEPLOY_SEQUENCING. GEMP state, DeckOracle, card, zone, action, and candidate reads remain in the bot adapters.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V22.6 |  | PULL-ENGINE | RCA:79 DC:84 CSE:8036 DO:29 | ORDERING | +500 location priority / -500 failed-pull | Three arms, all PULL-ENGINE: DeckOracle plumbing (RandoCalAi/DecisionContext/DeckOracle hits = wiring + reset, the DeckOracle file header itself is V22.6); CSE 7454-7467… | predicate/choice arm LIVE (RCA); location-priority magnitudes interact with V192 |
| V24-amsd-bespin-gate | V24 | PULL-ENGINE | ATE:1686 | VETO | — | ATE 1612-1636: -9999 hard block on AMSD when no Bespin system on table. Discovered arm, not in §8; belongs with the V29.4 AMSD gate family. ATE V24 hits split 4+4 with… | LIVE |
| V24-tdigwatt-exhausted-search | V24 | PULL-ENGINE | ATE:1686 | VETO | -400 soft-veto | ATE 1804-1822: -400 block when all 4 TDIGWATT search targets already pulled (dead-search guard). Discovered arm, not in | LIVE |
| V24.2-pull | V24.2 | PULL-ENGINE | CSE:7687 (#evaluateTakeIntoHand) | BANDED (SCORE) | +250 Lando (friendlies at CC; interleaved V47 guard: buddy-in-hand + force>=5 alt +250 / else -9999 block) / +200 Lobot | Objective supplies the plan facts; Pull Engine owns the candidate result (gate ruling). RE-HOMED from battle-weapons 2026-07-13 (MISFIT resolved). Any migration must co-freeze the interleaved V47 not-alone guard. Marker `"V24.2 PULL: Lando gets +250"` ×1/bot. Fixtures: spec boundary "V47 Lando stay/pull remain" + TODO `B0_V242_LandoLobot_PullPriority` | LIVE (split 2026-07-13) |
| V29.4 |  | PULL-ENGINE | ATE:1757 DO:22 | VETO | -9999 AMSD gates | AMSD allowed from HAND or RESERVE (unblocks the old Executor-in-hand false block); -9999 only when Piett in hand but Executor in neither zone; fail-open when oracle… | LIVE |
| V29.7-pull-validation | V29.7 | PULL-ENGINE | ATE:684 DO:22 | VETO | -400 per-card dead-pull blocks; +250 PULL FIRST | FLAGGED, not in my list: §8's V29.x family row names 'V29.7 pull/weapon/retreat/flip arms'. ATE ~2107-2400 UNIVERSAL RESERVE DECK PULL VALIDATION: card-specific -400… | guards LIVE; generic PULL FIRST +250 ABSORBED→V192 |
| V29.8-IAYF | V29.8 | PULL-ENGINE | ATE:2150 | VETO | -500 | ATE 2068-2087: lightsaber deploy from ANY source blocked when Vader not on table (IAYF). LIVE despite the version-table V29.8 row being DEAD - the dead part is the… | LIVE |
| V37-IAYF | V37 | PULL-ENGINE | ATE:2251 | ORDERING | -600/-400 wrong-zone blocks, +600 retrieve priority | ATE 2169-2225: DeckOracle zone check for the IAYF lightsaber — block pulling from the zone the saber is not in (reserve vs lost), +600 top priority to retrieve when… | LIVE |
| V37-search | V37 | PULL-ENGINE | ATE:2251 | BANDED | -200 intel-risk / caution tier | ATE 2314-2328 UNIVERSAL RESERVE SEARCH SAFETY NET: small reserve -> searching gives the opponent too much intel (-200), larger reserve -> caution note. NOT in the §8 V37… | LIVE |
| V60-pull | V60 | PULL-ENGINE | DE:576 ATE:263 CSE:8206 | VETO | guards -9999; baseline +100 (DE) vs +150 (ATE) drift | §8 authoritative arm 'pull baseline + guards'. Guards: FAIL-STOP after 2 failures, RESERVE RISK reserve<=2 (-9999 both copies as of 2026-07-06), RESERVE MISS… | ABSORBED→V192 2026-07-06 (guards live as veto chain; +100/+150 baseline now V192) |
| V66 |  | PULL-ENGINE | DE:705 ATE:4498 AA:23 DO:22 | VETO | — | MEMORY AUDIT: unified pull validation via DeckOracle — blocks unfindable targets and wasteful pulls (target already in hand/play); runs after older specific | LIVE |
| V67ac |  | PULL-ENGINE | ATE:4498 AA:23 | VETO | — | Force-cost guard for card-action reserve pulls: cheapest matching reserve card costed against force pile; hard-block if unaffordable (Vader's Castle incident).… | LIVE |
| V67b |  | PULL-ENGINE | CSE:1179 | VETO | — | CSE 1101: true Jedi Survivor predicate = game text contains literal 'Jedi Survivor' (persona fallback dropped). Gates Hidden Path Mapuzo siting (Ahsoka-stuck incident).… | LIVE |
| V67bg |  | PULL-ENGINE | DE:652 DO:696 | VETO | — | Type-aware pull validation: generic nouns (location/site/weapon/bay) resolved to typed Filters via DeckOracle.resolveCommonNounToFilter instead of title… | LIVE |
| V67h |  | PULL-ENGINE | DE:778 ATE:336 AA:23 DO:22 | VETO | — | Source-card game-text pull-target parser -> WILL_FAIL -9999 veto. 2026-06-28 revision in validatePullFromSourceCard: junk parse targets (len>25 or digit) return UNKNOWN… | LIVE |
| V67l |  | PULL-ENGINE | DE:3733 ATE:1179 CSE:6509 | ORDERING | +1500 dominance | Universal location-pull priority in ATE (~4650): +1500 when action/source text names a location in its target list — location pulls fire | predicate LIVE (keyword set), magnitude ABSORBED→V192 |
| V67s |  | PULL-ENGINE | DO:964 | VETO | — | Comment-only hits (DeckOracle 931/940): icon-prefix stripping + last-word fallback in the named-target keyword matcher. Parser support inside the veto machinery; no own… | LIVE |
| V82 |  | PULL-ENGINE | ATE:263 | ORDERING | +2500 dominance | Source-card blueprint pattern '(site/location/battleground) ... from reserve' -> +2500, dominating V60+V67l and competing deploys (Invasion Naboo-site pull | ABSORBED→V192 (predicate feeds LOCATION tier; +2500 grant retired) |
| V82.1 |  | PULL-ENGINE | ATE:337 DO:21 | VETO | — | Parser arm: dropped the 'deploy' anchor — capture the clause before 'from Reserve Deck', strip verb/article noise. Feeds the WILL_FAIL veto machinery; no own | LIVE |
| V82.2 |  | PULL-ENGINE | ATE:337 DO:21 | VETO | — | WILL_FAIL authority rules: authoritative when a target had a recognized type-word, or fully proper-noun target failed substring match. Decides when the dead-search veto… | LIVE |
| V82.2b | V82.2b | PULL-ENGINE | DO:1364 | VETO (rescue) | persona-match rescue before DEAD verdict | Pull filters: persona match rescues a pull verdict (Rey replay bonus defect, 2026-07-10) | NEW since manifest (post-2026-07-06) |
| V82.3 |  | PULL-ENGINE | DO:21 | VETO | — | Paren/bracket stripping after the or->comma split so the category/predicate fallback sees clean words (Begin Landing 'coruscant docking bay' case). Parser support for… | LIVE |
| V95 |  | PULL-ENGINE | ATE:277 DO:1189 | VETO | -2000 hold, reserves>=15 gate | SAVE DEAD INTERRUPTS: all parsed pull/upload targets already on table AND reserve force >= 15 -> -2000 (keep as force-loss fodder). Additive prohibition, not an early… | LIVE as hardBlock inside V192 veto chain (standalone block absorbed) |
| V97 |  | PULL-ENGINE | ATE:209 | ORDERING | — | Pull-from-Reserve-before-activating +1500 (ATE 1056-1108; Knowledge And Defense excluded; symmetric with chosenone). Task slice lists this under ACTIVATE as 'ordering… | ABSORBED→V192 (+5500 activate-grade base carries the pull-before-activate law) |
| V100 |  | PULL-ENGINE | ATE:1164 DE:860 | ORDERING | +1500 dominance | Location pull/deploy from Reserve fires BEFORE character/vehicle deploys (+1500 when characters still in hand). Distinct from V97 (Activate-phase). Excludes Knowledge… | ABSORBED→V192 (+25 chars-in-hand context term) |
| V116 |  | PULL-ENGINE | CE:25 ATE:254 | BANDED | +100 floor | Unconditional +100 baseline at the very top of ATE.evaluate() for any 'from reserve deck'/[download] action; V60/V67ai/V82 stack on | ABSORBED→V192 (+150 deploy-grade base) |
| V123 |  | PULL-ENGINE | DE:731 ATE:4671 | VETO | — | Stopword list of generic category nouns: when V66's captured keyword is a stopword, defer to criteria-aware V67ai/V82 instead of false WILL_FAIL veto (Hunt Down… | LIVE |
| V130 |  | PULL-ENGINE | DO:22 | VETO | — | Pure query helpers, NO scoring: countMatchingInDeck + countMatchingInHandOrTable. Classified VETO only because it is the backbone of V131's Tier-1 hard block;… | LIVE |
| V131 |  | PULL-ENGINE | ATE:4498 DO:22 | VETO | Tier1 -9999 / Tier2 -2000 / Tier3 unchanged | Three-tier deck-state gate wrapping V67ai: not-in-deck -9999, already-satisfied -2000, genuinely-needed unchanged; also fixed V67l substring misfire. Fail-open on… | LIVE |
| V144-search | V144 | PULL-ENGINE | ATE:1057 | VETO | -2000 | You Are Beaten mode-2 IAYF search is always blocked so the card remains available for battle freeze or Cancel Uncontrollable Fury. The battle-freeze arm remains BATTLE-2. | CONSOLIDATED V268 |
| V147 |  | PULL-ENGINE | ATE:800 | VETO | -2000 | IAYF Lost-Pile mode is blocked when Vader's Lightsaber is not actually in the Lost Pile. | CONSOLIDATED V268 |
| V155 |  | PULL-ENGINE | ATE:842 | VETO | -2000 | Welcome Home Lord Tyranus mode-1 pull is blocked when the pull is already satisfied, preserving the card's premium battle mode. | CONSOLIDATED V268 |
| V177 |  | PULL-ENGINE | DE:828 ATE:267 CSE:889 DO:21 | VETO | -2000 block, skips all further scoring incl. V116 | Dead-search gate: classify parsed pull targets ALIVE/JUNK/DEAD; block only when no ALIVE, >=1 DEAD, no JUNK. Supersedes fire-every-turn heuristic for parseable pulls.… | UPDATED 2026-07-07 (pull-parser 692fec3cf) |
| V177a | V177 | PULL-ENGINE | ATE:267 | VETO | — | Same-session amendment: added the JUNK class + >=6-char loose word-rescue after false-blocks. Documented under | LIVE |
| V183 |  | PULL-ENGINE | ATE:366 DO:615 | VETO | — | Deck Oracle retool: no verb parsing — scan source game text for catalogued deck titles (>=6 chars) and judge by real ZONE; block when every named target is out of… | LIVE |
| V192 |  | PULL-ENGINE | ATE:4484 | ORDERING (hub) + VETO chain | base +150 deploy-grade / +5500 activate-grade (P1 stand-down when V61c holds destiny buffer); location tier 1500/1400/1300/1200 by source cat, weapon 600, device 400; +50 [download], +25 chars-in-hand; clamp 1750 deploy / 7100 activate | THE merged pull scorer (T4.2, 2026-07-06): ONE emit per reserve-deck pull; vetoes (V60 guards, V66, V67h, V67ac, V95, V131, V67ar/V67ao/V149) run first and short-circuit | NEW since manifest (post-2026-07-06) |

### objective-intent — 7 rules
*Objective detection, flip intel, objective deploy adjustments.* Owner: SVC-INTEL (shared cOA is the LIVE brain; bot OA files are compatibility facades; ObjectiveHandler.java is DEAD, do not wire).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V21-analyzer | V21 | SVC-INTEL | cOA:32 DPP:23 AA:23 | ORDERING | — | §-arm per slice: shared ObjectiveAnalyzer runtime objective-text parser (V21 is its founding tag) + planner objective-awareness wiring; ActionAudit hit is a dormant… | CONSOLIDATED V207 |
| V24.7-intel |  | SVC-INTEL | RCA:82 BP:61 CSE:334 DC:85 ODT:6 | BANDED | n/a — intel service, no direct score | OpponentDeckTracker destiny-intel service (deck-peek scanning, average destiny) + BattlePredictor intel-aware prediction methods. NOTE:… | LIVE |
| V25-detector | V25 | SVC-INTEL | cOA:20 | ORDERING | — | §8 arm: Hunt Down V + ISB Operations objective detection (flip conditions, Vader-on-table checks, ISB agent counting, back-side flip-back detection). Detection service… | CONSOLIDATED V207 |
| V29-objtext-intel | V29 | SVC-INTEL | cOA:21 | ORDERING | — | FOUND IN REGION arm of V29, UPDATED 2026-07-06 (TDIGWATT bug B): stores raw objective game text + objectiveForbidsDeployingExecutor() predicate consumed by the… | CONSOLIDATED V207 |
| V67ak |  | SVC-INTEL | ATE:1955 DE:3936 cOA:21 | BANDED | +800 flip-critical tier | Universal KEY-CHARACTER token extractor (ObjectiveAnalyzer service, 235/323) with two scoring consumers: +800 pull priority (ATE 5102-5162) and +800 deploy priority (DE… | CONSOLIDATED V207 |
| V170-cover | V170 | SVC-INTEL | RCA:611 ATE:3754 | ORDERING | — | §8 arm 'cover decision': yes/no intercept in RandoCalAi 584-630 answering the engine's 'deploy as Undercover spy?' prompt (YES when opp total drain >= 1, NO early game).… | LIVE |
| V193-intel | V193 | SVC-INTEL | cOA:79 | ORDERING | no score — names the flip-gate control site | Objective flip-gate control-site intel (Endor: Bunker etc.); reset+reparse per detection; JSON-hydrated profiles supersede the hardcoded Endor block | CONSOLIDATED V207 |

### loop-safety — 10 rules
*Loop breaking, retry budgets, concede, dormant checks.* Target owner: SVC-SAFETY (DecisionSafety + ATE loop guards + CE).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V25-concede | V25 | SVC-SAFETY | RCA:494 | ORDERING | — | §8 arm: AUTO-CONCEDE when losing by 30+ in Lost Pile (RandoCalAi:495); V67aw defers the actual | LIVE |
| V44 |  | SVC-SAFETY | RCA:583 | VETO | — | ALWAYS accept opponent revert requests — hard policy, never blocks. Shares its two code lines with V67j (both tags on RandoCalAi:556,579); counts overlap by | LIVE |
| V67aw |  | SVC-SAFETY | RCA:100 CSE:951 | ORDERING | — | Concede DEFER: pendingConcede flag set when losing threshold hit, fires only after next battle phase ends (RandoCalAi 100-1705). CSE hits are strategy comments… | LIVE |
| V67j |  | SVC-SAFETY | RCA:583 | ORDERING | — | Revert-accept mechanics: never assume index 0 = Yes; scan the results array for the actual Yes/No indexes (RandoCalAi 556-594). Two lines shared with V44. NOTE: V67j has… | LIVE |
| V68-dormant | V68 | SVC-SAFETY | AA:20 | VETO | — | ActionAudit unified pre-flight validation framework — DORMANT, ZERO CALLERS (plan traps ledger: label, don't wire, don't clean up). Consolidation targets documented… | LIVE |
| V148 |  | SVC-SAFETY | DSf:62 CE:66 | VETO | — | Done/Cancel must remain reachable: decisions explicitly offering Done/Cancel are exempt from random-pick correction so deploy-abort stays | UPDATED 2026-07-10 (deploy-abort) + 2026-07-12 (cancellability semantics reused by FS all-veto fallback) |
| FS-enforcement | FORMATION | SVC-SAFETY | EvaluatedAction.java:99 (#mergeFrom) + CE #evaluateDecision (bucket-walk filter :249, V67bc epilogue skip :301, final selection + all-veto fallback :378-427) | VETO plumbing (ROUTING consuming CONSTRAINT) | no own score — vetoed actions unselectable regardless of score; all-veto fallback: synthetic Pass if V148-cancellable, else least-bad WITHOUT clearing veto facts | RE-HOMED from solo-formation 2026-07-13 (gate 5e290559c #3): GENERIC CONSTRAINT INFRASTRUCTURE. The hardVeto OR-merge + merge/final enforcement serve ANY producer domain — FormationSafety owns the LAWS (solo-formation); CombinedEvaluator owns THIS plumbing. Do NOT migrate the mechanism into FormationSafety; future domains must not depend on a formation-specific finalizer. V148 supplies cancellability semantics (dependency, not co-ownership). Markers ×1/bot each: `"vetoes are OR-merged — no bonus stack can wash one out"` (EvaluatedAction), `"FORMATION SAFETY 2026-07-11c"`, `"epilogue must not resurrect vetoed actions"`, `"FORMATION SAFETY: ALL actions vetoed and pass is legal"` (CE). Fixtures (all named, spec 07-12): `B0_MergedAction_VetoOR`, `B0_170_HardVeto_Epilogue`, `B0_AllVeto_OptionalDone`, `B0_AllVeto_ForcedLeastBad` | RE-HOMED + rewritten 2026-07-13 |
| V163 |  | SVC-SAFETY | ATE:44 DrE:27 | VETO | — | Cancel-loop HARD VETO -100000 (replaced old additive -200 that got dominated). The uniform veto every loop-breaker falls back to; V167/V169 carve the explicit | LIVE |
| V167 |  | SVC-SAFETY | ATE:107 DrE:27 | VETO | — | Veto-exemption policy: phase-fundamental actions (Activate Force, draws) are NEVER hard-vetoed — soft -200 instead (pre-V163 value). Explicit exemption to the V163 law,… | LIVE |
| V191 |  | SVC-SAFETY | RCA:845 CE:72 | BANDED | n/a — log-only instrumentation | NEW 2026-07-06 (post-version-table; no xlsx row — add at T0.4 regen): TOP-N candidate logging, the dominance-regression detector (plan T0.5). CombinedEvaluator logs… | LIVE |

### pass-cancel — 2 live rules (+1 inert V37.4 arm pending retirement)
*Pass baseline + pass/cancel gates.* Target owner: shared AI-only `PassPolicy`; PassEvaluator remains the fact adapter and V148 cancellability semantics remain separate.

**V269 owner note:** `PassPolicy` owns the complete additive Pass ladder, exact early-return outcomes, baseline constants, canonical V37.4/V27.1/V27 arms, and typed trace metadata. The Rando and ChosenOne adapters retain decision-text classification, GameState and ForceReserveService reads, diagnostics, action construction, and action-list control.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V37.4-pass | V37.4 | pass-cancel | PE:170 (#evaluate) | BANDED (SCORE) | -50 - (hand-10)*20 on Pass, extra -100 when forcePile >= 8 (DEPLOY phase, hand >= 10 only) | HAND BLOAT: passing IS the decision being scored — pass-cancel owns it, DEPLOY-1 does not (gate ruling). Marker `"V37.4 HAND BLOAT: %d cards in hand"` ×1/bot. Fixture: `PassPolicyTest#deployHandBloatPreservesForceSurchargeAndBoundary` | CONSOLIDATED V269 |
| V37.4-empty-check | V37.4 | — | DE:3272-3299 (#evaluate) | FACT, unconsumed | none in effect — `canDeployToOpponents` computed but NEVER read (declare+assign only, verified 2026-07-13); the V36 EMPTY DEPLOY reasoning it was meant to modulate fires with `emptyPenalty` hardcoded 0.0f (V40 neutralization) | INERT arm, owner NONE (gate-confirmed): local guessed eligibility with no consumer. NOT counted in live totals. Retire (delete) only after the no-consumer fixture proves score-neutrality. Marker `"V37.4: Check if we CAN actually deploy"` ×1/bot. Fixture: TODO `B0_V374_EmptyCheck_NoConsumer` | INERT (split 2026-07-13; retirement pending fixture) |
| PASS-baseline |  | SVC-SAFETY | PE:29 | BANDED | 5 baseline | The Pass action baseline other sections deliberately score against (V61c lands below it by design); now emitted as a typed INITIAL trace without changing visible score or reasoning. | CONSOLIDATED V269 |

### response-routing — 6 rules
*Opponent-turn top-level actions, reacts, choice prompts.* Target owner: RESPONSE router (thin dispatcher; routes to callable sections).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V29.8-ate-guards | V29.8 | RESPONSE | ATE:2150 | BANDED | -500..-600 tier | IMPORTANT: version table marks V29.8 'DEAD -> V153' but that is ONLY the CSE zone-scoring arm (all 46 CSE hits verified commented). This ATE arm is LIVE: IAYF… | LIVE |
| V38.3-self-target | V38.3 | RESPONSE | CSE:7500 | VETO | — | FOUND IN REGION, not on slice list: hard block targeting OWN cards with harmful effects -9999 (CSE 6940-6944, target-selection path). Not in the §8 V38.3 arm list (which… | LIVE |
| V53-grab-guard | V53 | RESPONSE | ATE:2543 | VETO | — | FOUND-IN-REGION arm bundle of V53 (beyond §8's two named arms): (a) grabber shields must only grab OPPONENT interrupts — own-card grab -9999, opponent grab +GOOD,… | LIVE |
| V67af |  | RESPONSE | ATE:3374 | VETO | — | RETURN-OWN-CHARACTER-TO-HAND bounce block -9999 (unclassifiable return -150). DE:4162 commented pointer. Overlaps V29.7 BOUNCE -300 (older, weaker copy) — consolidation… | LIVE |
| V74 |  | RESPONSE | ATE:4190 | ORDERING | — | Maintenance-cost decision order (replaces V22.3): PAY +400 > lose to Used pile -200 > place out of play -800. Strict preference among the offered maintenance | LIVE |
| V79-parsec-choice | V79 | RESPONSE | ATE:73 | BANDED | +/-200..1500 choice tier | FOUND IN REGION: ATE MULTIPLE_CHOICE parsec/orbit scoring (+1500/+1200/+800/-800/-200, ATE 1209-1330) with V103 detection. Response-layer arm of V79; in practice V79b… | LIVE |

### deck-playbook — 37 rules
*Deck-specific scripts: TDIGWATT, Hunt Down, Hidden Path, Saga, Verge, Senate, SWBD.* Target owner: PLAYBOOKS overlay (phase back-pointers per council condition).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V22.2-flip | V22.2 | PLAYBOOKS | CSE:3447 ME:594 cOA:20 | BANDED | +/-30..160 tier | Post-flip protection arm (slice-homed): deploy-to-protect +60, pre-flip fortify scaled penalties -30..-160, post-flip STAY/CONSOLIDATE move logic +/-30..160 (ME… | LIVE |
| V22.5 |  | PLAYBOOKS | ATE:4249 DE:5686 ME:649 cOA:1884 | BANDED | +100..300 tier | AMSD/Bespin ship priority (+300 no-ship-at-Bespin, +100 after; +120 pilot+ship combo) + PRE-FLIP CONSOLIDATION lone-outgunned move +100..160 (ME 1339-1399) +… | LIVE |
| V22.7 |  | PLAYBOOKS | CSE:419 DE:4145 | VETO | — | CLOUD CITY OCCUPATION GUARD: block Dark Deal-style cards that self-cancel when we don't occupy Bespin (DE 3935-3988, hard block; +50 when safe) + MUST CONTEST… | LIVE |
| V23 |  | PLAYBOOKS | ATE:1640 CSE:2409 DE:1137 | BANDED | +250..300 contest / -100..-300 guard | TDIGWATT/Bespin playbook: BESPIN CONTEST +300 / ship +250 (DE 1118, 5485-5513) + opponent-force-icon deploy preference (CSE 2192-2207). ATE arm = EMPTY PILE GUARD… | LIVE |
| V24.1 |  | PLAYBOOKS | CSE:8064 DE:4561 | ORDERING | — | TDIGWATT pull-preference ladder: Endor Shield admiral pull Piett-first (V24.1A), Piett's commander pull Gherant-first +400 (V24.1B), Gherant deploy +150 (V24.1C in | LIVE |
| V24.3 |  | PLAYBOOKS | CSE:3320 DE:5621 | BANDED | +100..200 combo tier | Dr. Evazan weapon-kill combo: deploy-location +200 (B), move preference +200 (C), deploy priority +150/+100 | LIVE |
| V24.6 |  | PLAYBOOKS | ATE:1927 DE:5679 | BANDED | +250..800 / -300 guard | I'M SORRY location pull until CC interior sites exhausted (+250 / -300 fail-guard) + EXECUTOR deploy priority +800 (V24.6A with | LIVE |
| V24.9 |  | PLAYBOOKS | ATE:1591 CSE:3456 DE:5638 | BANDED | +/-200..800 tier | TDIGWATT engine pacing: Masterful Move early-game guard -500/-300, EXECUTOR CRITICAL +800 turn 1-2, unoccupied-CC move dest +200, spy-detect method 3 (blueprint… | LIVE |
| V24.10-amsd-script | V24.10 | PLAYBOOKS | ATE:1716 CSE:395 DE:1028 DO:153 | VETO | — | §8 arm: AMSD script — Piett-only -9999 family (retry block after per-turn failure, Piett-not-in-hand gate, non-Piett pilot blocks, Executor-must-go-Bespin -9999/+500,… | UPDATED 2026-07-11 (Piett-only safety net: +500 Piett / -9999 non-Piett on AMSD) |
| V24.10-piett-dig | V24.10 | PLAYBOOKS | DrE:24 DE:1028 | BANDED | +150..piettBonus tier | §8 arm: DIG FOR PIETT — aggressive draw bonus when Piett not in hand/reserve (DrawEvaluator 267-289) + extra location-deploy priority +150 to power draws (DE 1103-1114).… | LIVE |
| V24.11 |  | PLAYBOOKS | CSE:459 | ORDERING | — | AMSD routing fix: check before evaluateTargetSelection, route 'click done to cancel' + AMSD active to evaluatePilotSelection. Routing, no | LIVE |
| V24.12 |  | PLAYBOOKS | CSE:5619 | ORDERING | — | Admiral pull pick ladder by title (generic GEMP text): Piett +300 > Chiraneau +150 > Ozzel +100; AMSD-on-table detection forcing pilot | LIVE |
| V24.13 |  | PLAYBOOKS | CSE:2696 | BANDED | -30..+250 tier | Lando-alone reinforcement +250 (deploy + move-to-support) + I'M SORRY site pull order (Carbonite Chamber +150 first, Security Tower -30 | LIVE |
| V25-huntdown | V25 | PLAYBOOKS | ATE:2191 CSE:1377 | BANDED | +/-150..500 script tier (embedded -9999 vetoes) | Hunt Down V + CC deck-script body (slice-homed here; §8 splits the other five V25 arms out): Vader Castle deploy VERY_GOOD+500 / -500 force gate (ATE 3782-3806 + 2109… | LIVE |
| V26 |  | PLAYBOOKS | ATE:4006 CSE:515 DE:5806 | ORDERING | — | TDIGWATT objective site deploy order: Upper Walkway EXTERIOR +500 first (I'm Sorry can't pull it) > other interiors -200 > Dining Room -400 (save for Slip Sliding). Plus… | LIVE |
| V29-BESPIN | V29 | PLAYBOOKS | DE:852 | VETO | — | §8 arm: TDIGWATT BESPIN-FIRST guard (DE 1159-1284): -500 gate on character deploys until Bespin occupied. UPDATED 2026-07-06 (TDIGWATT bug B): gate releases when… | LIVE |
| V29.2 |  | PLAYBOOKS | DE:4566 | BANDED | +150..200 tier | Lando +200 / Lobot +150 deploy priority when backup present (title AND action-text | LIVE |
| V29.3 |  | PLAYBOOKS | CSE:639 | ORDERING | — | Card-type detection fallback chain (decision text -> gameState -> FALLBACK_LIBRARY -> last-resort assume character). Detection service, no scoring; candidate to relabel… | LIVE |
| V29.7-deck-scoring | V29.7 | PLAYBOOKS | ATE:684 CSE:908 DE:2139 | BANDED | +/-30..200 tier | Residual V29.7 arms beyond §8's four names, labeled so every hit maps: docking-bay strategy (first bay +200, extra empty bays -200/-50; ATE 3719-3778 + CSE 1591-1614),… | LIVE |
| V29.7-flip | V29.7 | PLAYBOOKS | RCA:1201 cOA:374 | ORDERING | — | §8 arm (flip): per-evaluation flip-status refresh (RandoCalAi:1115 -> ObjectiveAnalyzer.refreshFlipStatus). Service plumbing, no | LIVE |
| V29.7-pull | V29.7 | PLAYBOOKS | ATE:684 DO:22 | ORDERING | — | §8 arm (pull): PULL FIRST +250 (pulls fire before deploys) + universal reserve-target validation -300..-500 fail guards per named source (Crush/IAYF/You Are Beaten/Blast… | LIVE |
| V29.7-retreat | V29.7 | PLAYBOOKS | ATE:684 | BANDED | -300 tier | §8 arm (retreat): Vader's Castle retreat penalty -300 while Vader is draining (ATE | LIVE |
| V29.7-weapon | V29.7 | PLAYBOOKS | BE:205 ME:100 | BANDED | weapon-adjusted diff tier | §8 arm (weapon): weapon-adjusted effective power diff for battle decisions (BattleEvaluator 191-474) + WEAPON HUNTER armed-character seek-battle move bonus (ME… | LIVE |
| V35.1-recall-block | V35.1 | PLAYBOOKS | ATE:2962 | BANDED | -400 | Hunt Down: do not recall an Inquisitor (Eighth Brother return-to-hand) while opponents are on the board | LIVE (manifest gap — added batch 0) |
| V35.3-hatred-siting | V35.3 | PLAYBOOKS | ATE:2758 | BANDED | -300 | Hunt Down: only place hatred when Vader/Inquisitor is co-located with opponents | LIVE (manifest gap — added batch 0) |
| V35.7-hatred-inquisitor | V35.7 | PLAYBOOKS | ATE:2774 | BANDED | -500 block / bonus when co-located | Hunt Down: hatred requires an Inquisitor on table (not Vader alone) | LIVE (manifest gap — added batch 0) |
| V35.8-IAYF-spare | V35.8 | PLAYBOOKS | ATE:2239 | BANDED | +50 | I Am Your Father: spare-lightsaber retrieval preference when Vader already armed (Reserve free vs Lost Pile -1) | LIVE (manifest gap — added batch 0) |
| V35.8-hunt-jedi | V35.8 | PLAYBOOKS | DE:3239 | BANDED | +600 | Hunt Down: Vader deploys to a site holding a JEDI (raised from +350 — killing Jedi is THE objective) | LIVE (manifest gap — added batch 0) |
| V41.2 |  | PLAYBOOKS | DE:5750 | ORDERING | — | PIETT DEPLOY — HOLD FOR AMSD (hold-until script marker, DE | LIVE |
| V52b |  | PLAYBOOKS | DE:6030 | BANDED | +600..800 opening-script tier | HIDDEN PATH JEDI FLOOD turns 1-2: Jedi chars +800, Fallen Order Jedi deploy +800, lightsaber +700, holocron +600 (DE | LIVE |
| V54 |  | PLAYBOOKS | DE:5898 | BANDED | turn-scaled script tier | Skywalker Saga (LMFBM) T1-3 deploy script: Cantina/Mos Eisley/Lars/Tatooine sites/system, Young Skywalker/Luke persona, Luke's Lightsaber, Jedi buddy — turn-scaled… | LIVE |
| V54.1 |  | PLAYBOOKS | DE:5907 | ORDERING | — | Skywalker Saga deck-detection predicate (Epic Event deck detection, DE 5681). Comment-anchored detection, gates the V54 | LIVE |
| V61-saga | V61 | PLAYBOOKS | RCA:663 | ORDERING | — | §8 arm: 'The Force Is Strong In My Family' Epic Event saga CHOICE pick at RandoCalAi:636-684 (deterministic deck-based index pick). UNCLAIMED ARM: V61 battle… | LIVE |
| V62 |  | PLAYBOOKS | CSE:440 | BANDED | +200 / -500 / -1500 tiers | Hidden Path siting: SPLIT SITE +200 ideal / -500 duplicate-Jedi dest; SPY DILUTION -1500 (don't move onto our own undercover spy's site). V41 -9999 interplay documented… | LIVE |
| V83/V88/V99-Senate |  | PLAYBOOKS |  | VETO | — | CROSS-REF STUB ONLY — per slice instruction PLAYBOOKS carries a pointer to the Senate rule family; the tags themselves are homed by the DEPLOY-cluster slice (do not… | LIVE |
| V142 |  | PLAYBOOKS | ATE:677 | VETO | -2000 | We Must Accelerate Our Plans: deck-aware mode preconditions before playing (WMAOP misfire class) | LIVE (manifest gap — added batch 0) |
| V160 |  | PLAYBOOKS | ATE:928 cOA:21 | BANDED | +800 tier | Shield Will Be Down In Moments (Hoth invasion): push Target The Main Generator +800; ObjectiveAnalyzer detection | LIVE |

### fact-services — 5 rules
*No-score helpers: detection, predicates, wave projection, intel plumbing.* Target owner: common/strategy services (MovePredicates, ShieldFacts, MaintenanceFacts, FRS) + SVC-ORACLE.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V24.7 |  | BATTLE-1 | BP:61 CSE:334 DC:85 ODT:6 RCA:82 | BANDED | n/a — intel plumbing, no direct scoring | Destiny-value scan of visible cards feeding BattlePredictor real numbers. Pure data plumbing across 5 files; arguably SVC-INTEL rather than BATTLE-1 — flag for | LIVE |
| V24.14B-spy-detect | V24.14B | DEPLOY-2 | CSE:1236 | ORDERING (detection) | no own score — feeds V170/V24.14 spy handling | Universal early spy detection: blueprint game text "undercover" + decision-text keywords | LIVE (manifest gap — added batch 0) |
| V28-arbitrary-plumbing | V28 | INFRA | CSE:4013 | ORDERING | — | KIND not really applicable — pure infrastructure: ARBITRARY_CARDS temp-ID -> blueprint resolution (CSE 7037-7286, the temp-id trap). Plan skips pure-infrastructure rows;… | LIVE |
| V71 |  | SETUP | CSE:7691 | BANDED | n/a - no score (fact helper) | Not a scorer: concatenates base + Light + Dark location game text so keyword scans (V29.14 epic, V67o battleground, reserve/force-gen) work (CSE 7109, 7141; 7618 is a… | LIVE |
| V173 |  | DEPLOY-2 | CSE:956 | BANDED | helper, no own points — v173WaveProjection (whole-hand wave, weapon weights +5 lightsaber/+3 other, max 2) | Helper at CSE 5633 + usage comments 934/1050. Feeds both V172 | LIVE |

## 3. Parity pairs & mirror surfaces (change together or regress)

| Pair | Sides | Law |
|---|---|---|
| V136 <-> V137 | CDSE.evaluateSite winnability (deploy) <-> ME no-abandon veto (move) | Bot must not deploy to spots it immediately flees; both route winnability through MovePredicates.canWinAt (graded) — V181 is the deploy-side port of V137 |
| V179 <-> V67ai | DPS location-keyword set <-> DE/ATE deploy-from-reserve keyword list | Lists MUST stay in sync or the DPS walk and pull scoring disagree on what counts as a location pull. NOTE: V67ai DE-copy magnitudes ABSORBED into V192 (2026-07-06); parity is now over the PREDICATE set (V179 <-> V67ai list <-> V192 location tier) |
| V153 routes | evaluateForceLoss <-> evaluateForceLossOrForfeit (CSE adapters) | One shared ForceLossPolicy owns the common zone table; preserve the deliberate standalone-only and combined-battle scope differences pinned by V206 tests |
| V159 x2 call sites | v159ForfeitScore() called from both forfeit paths (CSE) | Shared helper (lowercase, not grep-counted) must travel with the tag |
| V88/V89/V193 DE<->CS | DeployEvaluator arm <-> CardSelectionEvaluator (CS) twin | Same rule on two decision routes. V88/V89: keep magnitudes in lockstep. V193: deliberately NOT lockstep — CS route = weight+1600 offset to beat the CS-route penalty stack (see V193-cs-route row); preserve the OFFSET, not equality (corrected 2026-07-13) |
| V67z x2 | DrE transit reserve <-> DE deploy-phase twin | Same reservation on two phases |
| V61c/V168/V38.3 triangle | FAE keep-3 cap <-> ATE activate block <-> ATE confirm carve-out | THREE sites share DecisionContext.isBattlePlausibleThisTurn(); V192 activate-grade base (+5500) must outrank V168 (+5000) |
| BATTLE-1 SUM | BE initiation scoring + ATE V25 power-tier block | The SUM is the behavior — preserve it when changing either side |
| V156 <-> V136 §A/B | CDSE solo-hold arm <-> CDSE team-viability/over-stack | Solo stack math (2026-07-07) tuned against §A/§B bands — re-check boundary cases together |
| rando <-> chosenone | every evaluator file | Mirrored edits required except shared common/strategy classes (FS, CDSE, MP, MF, ShF, FRS) which CANNOT drift |

## 4. Multi-arm / multi-domain tags (the offenders)

41 base tags have live arms in 2+ domains (V24.2 added by the exact-arm split; V27 added 2026-07-13 when its siblings were minted, gate m00288). Every arm is a separate registry row above; migrate ARMS, not tags.

| Base tag | Domains its arms live in |
|---|---|
| V25 | battle-initiation, deck-playbook, drain-control, force-loss-payment, loop-safety, move, objective-intent |
| V27 | force-budget (battle-reserve, maintenance-pass, maintenance-move), solo-formation (buddy-protect) |
| V29 | deck-playbook, move, objective-intent, shields |
| V21 | force-loss-payment, objective-intent, setup-starting |
| V29.9 | battle-initiation, battle-weapons, drain-control |
| V36 | battle-weapons, deploy-siting, move |
| V37 | battle-forfeit, battle-weapons, pull-search |
| V38.3 | activation-amount, move, response-routing |
| V51 | battle-weapons, deploy-siting, shields |
| V53 | move, response-routing, shields |
| V60 | deploy-sequencing, move, pull-search |
| FS | solo-formation (laws), loop-safety (enforcement plumbing, re-homed 2026-07-13) |
| V24.2 | drain-control, pull-search |
| V166 | deploy-siting, move |
| V169 | move, solo-formation |
| V170 | deploy-siting, objective-intent |
| V178 | battle-forfeit, force-loss-payment |
| V193 | deploy-siting, objective-intent |
| V22.3 | battle-forfeit, force-budget |
| V22.4 | battle-forfeit, battle-initiation |
| V24 | deploy-sequencing, pull-search |
| V24.10 | deck-playbook, draw-count |
| V24.14B | deploy-siting, fact-services |
| V24.7 | fact-services, objective-intent |
| V28 | fact-services, force-loss-payment |
| V29.12 | battle-weapons, move |
| V29.14 | drain-control, setup-starting |
| V29.5 | shields, solo-formation |
| V29.7 | deck-playbook, pull-search |
| V29.8 | pull-search, response-routing |
| V31 | deploy-siting, move |
| V33 | deploy-attach, move |
| V35 | battle-initiation, battle-weapons |
| V35.1 | battle-weapons, deck-playbook |
| V35.4 | battle-weapons, move |
| V40 | deploy-attach, deploy-sequencing |
| V42 | activation-amount, draw-count |
| V43 | activation-amount, setup-starting |
| V52 | deploy-sequencing, drain-control |
| V61 | battle-initiation, deck-playbook |
| V83 | deck-playbook, deploy-siting |

Heavyweights: **V169** (6 registry rows after the 07-13 urgency/destination split; magnitudes frozen per SVC-SAFETY), **V47** (3 arms: LANDO STAY ME ladder, LANDO PULL TDIGWATT CSE ~8112, reserve-solo DELETED batch 1d), **V32** (ME ability>=4 move protection ME:930 + DPP ability-contribution calc DPP:1430), **V25** (7 domains — Hunt Down playbook cuts through everything), **V24.15** (TDIGWATT AMSD mega-priority ATE:1595+ vs EFFECTIVE-DRAIN control arm ATE:5207+), **V21** (starting-ban / loss-protect / analyzer-intel), **V35 family** (8 sub-tags across battle-weapons + playbook), **V37** (5 arms), **V52/V53/V60/V61** (3-4 arms each). Manifest merge-duplicates to watch: V97, V177, V67an, V37.3, V24.2, V169-loop-soft-block.

## 5. Exact-arm registry — 24 arms (21 ex-AMBIG per gate 5e290559c + the 3 V27 siblings minted per gate m00288; 23 LIVE + 1 INERT)

The 13 ambiguity-resolution summary rows concealed **21 independently routed arms** (enumeration per `Handoffs/CODEX_DOMAIN_REGISTRY_AMBIGUITY_RESOLUTION_2026-07-13.md` — followed exactly, no arms invented here). Each arm below is ALSO a §2 row in its owning domain table; THIS table is the migration authority carrying the full completion-gate fields. KIND: registry taxonomy first, target contract in parentheses (parenthetical scores describe the current implementation costume, not the future ownership model). Markers: `grep -cF` hit count per bot file, rando + chosenone mirrors at identical lines unless marked shared common/. Fixture ids follow `Handoffs/CODEX_RANDO_DECISION_FIXTURE_SPEC_2026-07-12.md`; **TODO = the fixture that WOULD be needed does not exist yet and MUST be written and frozen before that arm is retired or changes owner.** Every route fixture runs against BOTH bots (spec rule); shared helpers additionally need one shared unit fixture plus both-bot route fixtures proving the helper is reached. **Tiebreaker: if a §2 row and this table ever disagree, THIS table wins** — §2 rows are the domain inventory, this table is the per-arm migration authority.

| Arm | Route (decision path) | Anchor (rando = chosenone) | Producer(s) | Owner (target) | KIND | Magnitude | Marker (hits/bot) | Status | Fixtures (parity + retirement) |
|---|---|---|---|---|---|---|---|---|---|
| V37.4-pass | Pass action, DEPLOY phase (PE) | PE:170 #evaluate | decision hand/force/phase facts → pass policy | pass-cancel / SVC-SAFETY | BANDED (SCORE) | -50-(hand-10)*20; extra -100 if forcePile>=8 | `"V37.4 HAND BLOAT: %d cards in hand"` ×1 | LIVE | TODO `B0_V374_HandBloat_PassPenalty` |
| V37.4-empty-check | DE evaluate, V36 empty-deploy branch | DE:3272 #evaluate | none — local guessed eligibility | NONE | FACT, unconsumed | none: `canDeployToOpponents` never read; `emptyPenalty` hardcoded 0.0f (V40) | `"V37.4: Check if we CAN actually deploy"` ×1 | INERT | TODO `B0_V374_EmptyCheck_NoConsumer` — retire (delete) only after it passes |
| V156-deploy-hold | shared CDSE evaluateSite → computeTeamViability | CDSE:580 #computeTeamViability (shared common/, no mirror) | action facts + objective plan → FormationAssessment | solo-formation / DEPLOY-2 | BANDED (SCORE) | -600 SOLO HOLD / +250 PREFER BUDDY | `"V156 SOLO HOLD"` ×1 (shared) | LIVE | TODO `B0_V156_SoloHold_WeakBody` (+ named `B0_L3_NoBuddy_Raw350_Soft800` boundary interplay) |
| V169-urgency | top-level Deploy action (DE) | DE:976 #evaluate | board facts → endangered FormationAssessment | solo-formation / DEPLOY-1 | BANDED (SCORE) | +500 | `"V169 PROTECT URGENT: our characters at"` ×1 | LIVE | TODO `B0_V169_Urgency_DeployOpens` |
| V169-destination | deploy-location pick (CSE) | CSE:975 #evaluateDeployLocation | FormationAssessment + rescue feasibility (v173 wave) | solo-formation / DEPLOY-2 | BANDED (SCORE) | +800 + min(300, excess*30) | `"V169 PROTECT (deploy)"` ×1 | LIVE | TODO `B0_V169_Destination_OutpoweredSite` |
| V172-protect-gate | same CSE route; guards V169-destination | CSE:947 #evaluateDeployLocation | BattleFeasibility + ForceBudgetAssessment (v173) | solo-formation / DEPLOY-2 | gate (CONSTRAINT) | no own points — withholds +800..+1100 | `"V172 PROTECT GATED"` ×1 | LIVE | TODO `B0_V172_ProtectGate_UnsavableRetreat` |
| V172-contact-gate | same CSE route; guards V171 | CSE:1077 #evaluateDeployLocation | BattleFeasibility + ForceBudgetAssessment | solo-formation / DEPLOY-2 | gate (CONSTRAINT) | no own points — withholds V171 +600 | `"V172 CONTACT GATED"` ×1 | LIVE (upd 07-11 hit-aware) | TODO `B0_V172_ContactGate_WaveShort` |
| V172-solo-dominance | same CSE route; else-if ahead of V171 | CSE:1138 #evaluateDeployLocation | BattleFeasibility → FormationAssessment | solo-formation / DEPLOY-2 | BANDED (SCORE) | **+600** (`addReasoning(..., 600.0f)`) — prior no-score claim FALSE, corrected | `"buddy gate waived, Steve 2026-07-11"` ×1 (`"V172 SOLO DOMINANCE"` ×3, rejected) | LIVE | `B0_Dominance_Tyranus8_Leia3` (named) |
| V174-wave-budget | inside v173WaveProjection; consumed by V172 gates/V171/V151 | CSE:5581 #v173WaveProjection | MaintenanceFacts + hand facts → ForceBudgetAssessment | force-budget / DEPLOY-2 | ORDERING calc (CONSTRAINT) | reserved = tableMaint + interruptReserve(≤2) + thisMaintCost + 1 (V176 fee); V177 cap budget>=3 | `"tableMaint + interruptReserve + thisMaintCost"` ×1 | LIVE | TODO `B0_V174_WaveReserve_V177Cap` |
| V29.5-buddy | deploy-location pick (CSE) | CSE:3089 #evaluateDeployLocation (drift from 2828) | presence/ownership facts → FormationAssessment | solo-formation / DEPLOY-2 | BANDED (SCORE) | +40 / -150 / -100 / +10 | `"V29.5: GENERAL BUDDY SYSTEM"` ×1 | LIVE | TODO `B0_V295_Buddy_TopologyTiers` |
| V27-battle-reserve | battle-initiation score (BE) | BE:858 #evaluate | decision facts → ForceBudgetAssessment | force-budget / BATTLE-1 | BANDED (SCORE) | -40 pile<2 / -15 pile<4 | `"V27 BATTLE FORCE WARNING"` ×1 | LIVE | TODO `B0_V27_BattleForce_LowPile` |
| V27.1-battle-DTF | battle-initiation score (BE) | BE:846 #evaluate | inline DTF scan (cutover: ForceReserveService.dtfActive) → ForceBudgetAssessment | force-budget / BATTLE-1 | BANDED (SCORE) | -60 pile<3 / -100 pile=0 / 0 info | `"V27.1 DTF ACTIVE"` ×1 | LIVE | TODO `B0_V271_DTF_BattleBlocked` |
| V27.1-pass-DTF | Pass action (PE) | PE:222 #evaluate | ForceReserveService.dtfActive → ForceBudgetAssessment | force-budget / pass | BANDED (SCORE) | +20 / +40 / +60 on Pass | `"V27.1 DTF RESERVE"` ×1 | LIVE | TODO `B0_V271_DTF_PassConserve` |
| V27-maintenance-pass | Pass action (PE) | PE:223 #evaluate | ForceReserveService.maintenanceObligation → ForceBudgetAssessment | force-budget / pass | BANDED (SCORE) | +25 pile<=obligation+1 / +50 pile<obligation, on Pass | `"V27 MAINTENANCE RESERVE"` ×1 | LIVE (minted m00288) | TODO `B0_V27_MaintPass_Conserve` |
| V27-maintenance-move | Move action (ME) | ME:1968 #evaluate | ForceReserveService.maintenanceObligation → ForceBudgetAssessment | force-budget / MOVE | BANDED (SCORE) | -80 when the move dips the pile below the obligation | `"V27 MAINTENANCE MOVE BLOCK"` ×1 | LIVE (minted m00288) | TODO `B0_V27_MaintMove_Dip` |
| V27-buddy-protect | Move action (ME) | ME:880 #evaluate | board facts → FormationAssessment (move-origin strand) | solo-formation / MOVE | BANDED (SCORE) | -150 / -250 enemy present / -400 enemy overpowers ally | `"V27 BUDDY PROTECT"` ×2 (score + warn log; stated) | LIVE (minted m00288) | TODO `B0_V27_BuddyProtect_StrandTiers` |
| V24.2-pull | take-into-hand pick (CSE) | CSE:7687 #evaluateTakeIntoHand | ObjectivePlan + PullViability | pull-search / PULL-ENGINE | BANDED (SCORE) | +250 Lando / +200 Lobot (V47 guard interleaved) | `"V24.2 PULL: Lando gets +250"` ×1 | LIVE | spec boundary "V47 Lando stay/pull remain" + TODO `B0_V242_LandoLobot_PullPriority` |
| V24.2-drain | top-level optional response (ATE) | ATE:3068 #evaluate | optional "+1 drain" ActionFact | drain-control / CONTROL | BANDED (RANK) | +80 live (VERY_GOOD_DELTA 50 + 30) | `"V24.2 FORCE DRAIN BONUS"` ×1 | LIVE | TODO `B0_V242_DrainPlus1_Accept` |
| V193-deploy-route | parent deploy action (DE) | DE:1976 #evaluate | ObjectiveAnalyzer + DeckOracle → ObjectivePlan | deploy-siting / objective adapter | BANDED (RANK) | +400 (playbook weight; 400 default); LACKS child body gate | `"V193 FLIP-GATE CONTROL: steer one body to"` ×1 | LIVE | TODO `B0_FlipGate_V193_ParentDeploy` |
| V193-cs-route | destination pick (CSE) | CSE:2288 #evaluateDeployLocation | same ObjectivePlan | deploy-siting / objective adapter | BANDED (RANK) | weight+1600 (~2000); ability/cost gate; FS-exempt | `"V193 (CS) FLIP-GATE CONTROL: steer one ability body"` ×1 | LIVE | `B0_FlipGate_V193_Bunker` (named) |
| FS-enforcement | every decision: merge + final selection | EvaluatedAction.java:99 #mergeFrom + CE #evaluateDecision (:249 / :301 / :378-427) | domain constraints (ANY producer) | loop-safety / SVC-SAFETY finalizer | VETO plumbing (ROUTING consuming CONSTRAINT) | no own score — veto OR-merge; all-veto: Pass if V148-cancellable else least-bad | 4 markers ×1 each (see loop-safety §2 row) | LIVE | named: `B0_MergedAction_VetoOR`, `B0_170_HardVeto_Epilogue`, `B0_AllVeto_OptionalDone`, `B0_AllVeto_ForcedLeastBad` |
| V141-transport-floor | interrupt play decision (ATE) | ATE:670 #evaluate | action + force/reserve facts → ForceBudgetAssessment | force-budget / MOVE-RESPONSE | VETO (CONSTRAINT) | -2000 (forcePile<4 OR reserve empty) | `"V141 TRANSPORT INTERRUPT BLOCK"` ×1 | LIVE | TODO `B0_V141_Transport_ForceFloor` |
| V67z-draw-reserve | draw-count reserve calc (DrE) | DrE:661 #calculateForceToReserve | ObjectivePlan (Hidden Path unflipped) + corridor occupants → ForceBudgetAssessment | force-budget / DRAW | ORDERING calc (CONSTRAINT) | +1/body UNCAPPED; counts ALL characters despite Jedi wording (defect note, do not silently fix) | `"V67z TRANSIT RESERVE: {} Jedi at Underground Corridor"` ×1 | LIVE | TODO `B0_V67z_DrawReserve_CorridorCount` |
| V67z-deploy-reserve | deploy scoring (DE: budget calc + per-deploy penalty) | DE:333 + DE:2372 #evaluate | same assessment, deploy copy | force-budget / DEPLOY-1 | BANDED (CONSTRAINT) | cap min(bodies,3); -1500 per dipping force-costing deploy; cost-0 exempt — cap-3 semantics keep it SEPARATE from the draw arm | `"hold {} Force in deploy for the move-phase transit off Mapuzo"` ×1 + `"V67z TRANSIT RESERVE: Deploy costs %d"` ×1 | LIVE | TODO `B0_V67z_DeployReserve_Cap3` |

**Ownership boundary (gate correction 3).** FormationSafety/solo-formation own the LAWS — the producers that set hardVeto or score formations. `EvaluatedAction.hardVeto`/mergeFrom OR-merge and the CombinedEvaluator merge/final enforcement are GENERIC constraint infrastructure (SVC-SAFETY). Current hard-veto call sites happen to be formation laws, so the producers stay under solo-formation; the mechanism must NOT migrate into FormationSafety, and no future domain may depend on a formation-specific finalizer.

**V172 factual correction (gate correction 1).** The prior resolution claimed V172 "awards no siting points of its own". Live code in BOTH CardSelectionEvaluator mirrors calls `action.addReasoning(..., 600.0f)` on the SOLO DOMINANCE path before logging `-> +600` (verified 2026-07-13, CSE 1127-1142, identical in rando and chosenone, no enclosing `if (false`). The no-own-points claim is true ONLY of the protect and contact gates. The three V172 arms share a formation owner but do NOT share kind or magnitude.

**Cross-phase facts vs contributions (carried from the resolution).** Shared services may produce one fact for several phases without owning every contribution: ForceReserveService owns the DTF/maintenance observation (Battle and Pass own separate phase contributions); ObjectiveAnalyzer owns objective identity/plan facts (Pull Engine and deploy-siting adapters own their route results); FormationSafety owns formation laws (CombinedEvaluator owns generic merge + enforcement). Do not classify a service as SCORE merely because current callers immediately add a number.

### AMBIG RESOLUTION addendum — regenerated 2026-07-13 (supersedes the m00235 addendum)

- Gate `5e290559c` corrections APPLIED: (1) V172 solo-dominance live **+600** recorded and kind corrected; (2) the 13 summary rows replaced by the 21 exact arms — §2 rows physically SPLIT and RE-HOMED (V24.2-pull → pull-search, V24.2-drain → drain-control, FS-enforcement → loop-safety, V37.4-pass → pass-cancel with the inert DE arm tracked beside it); (3) formation-enforcement ownership boundary noted (§5 above + §1 + §2 section intros); (4) §1 placements, §2 headers, §3 V193 parity note, §4 (40 multi-domain tags), §7 counts regenerated from the post-split inventory; (5) the full stable-marker sweep remains OPEN — tracked below.
- Post-split live total: **367 arms** (was 357; +10: V172 +2, V27 +3 [siblings minted per gate m00288], V169 +1, V27.1 +1, V67z +1, V24.2 +1, V193 +1) plus 1 INERT arm tracked in place (V37.4-empty-check, pass-cancel table — excluded from live counts).
- **V27 scope note — CLOSED 2026-07-13** (gate m00288): the three sibling arms are minted as first-class rows (§2 force-budget ×2, §2 solo-formation ×1, §5 authority table). Markers verified ×1/×1/×2 per bot at minting.
- Verification discipline used (2026-07-13, HEAD `5240f36c6`): every marker `grep -cF`-counted in BOTH bot files; every magnitude read from the live `addReasoning`/`hardVeto`/return call; liveness checked against enclosing `if (false` (CSE's taped-off blocks at 2310/3694 and DE's single comment ref all fall OUTSIDE the 21 arm blocks; BE/PE/ATE/DrE contain zero `if (false`).

### 367-arm stable-marker sweep — REMAINING (open batch, do NOT attempt in one pass)

Of the 367 live arms, only the 23 live arms in the §5 table above (+1 inert sibling) carry verified-unique stable markers. **344 arms still carry base-tag first-hit `FILE:line` anchors** and need the marker treatment (unique code literal, `grep -c` = 1 per bot file, or an explicitly stated count). Remaining per domain: move 49, deck-playbook 37, deploy-siting 36 (V193 pair done), pull-search 32 (V24.2-pull done), deploy-attach 25, deploy-sequencing 23, battle-weapons 21, setup-starting 15, battle-forfeit 13, shields 13, battle-initiation 12, drain-control 10 (V24.2-drain done), loop-safety 9 (FS-enforcement done), force-loss-payment 8, activation-amount 7, objective-intent 7, solo-formation 7 (V113, BATCH1b, FS-L1..L4, FS-pull-route — the FS-law rows still anchor `FS:line`), draw-count 6, response-routing 6, fact-services 5, force-budget 2 (V22.3-maintenance, V24.5), pass-cancel 1 (PASS-baseline).

## 6. Dead / deleted / absorbed / unresolved ledger (NOT live rules — revert paths)

| Tag | Status |
|---|---|
| V122 | DEAD (taped off `if(false)` in V136 hub ledger) — anchor CSE:2117 DE:1856 CDSE:25 |
| V133 | DEAD (never shipped, dropped pre-V136) — anchor CSE:2354 |
| V28-reserve-solo | DELETED 2026-07-12 batch 1d (wrong-facts audit m00206; replacement = FS pull-route guard) — anchor CSE:4013 DE:1182 |
| V67aj | DEAD (taped off `if(false)` in V136 hub ledger) — anchor DE:1856 CSE:3669 CDSE:26 |
| V67al | DEAD (taped off `if(false)` in V136 hub ledger) — anchor DE:1856 CSE:3669 CDSE:26 |
| V67as | DEAD (taped off `if(false)` in V136 hub ledger) — anchor CSE:2117 DE:1857 CDSE:26 |
| V90 | DEAD (taped off `if(false)` in V136 hub ledger) — anchor DE:1856 CSE:2308 CDSE:25 |
| V164 | DOC-ONLY (zero code hits; the live arm is V164a) |
| V165 | NOT FOUND in tree (zero hits src-wide; version-table row exists — treat as unimplemented/lost, verify before relying on the turn-cap) |
| V152 | TOMBSTONE (no own logic; idea implemented as V155) — anchor ATE:843 |
| V159-superseded-branches | DEAD ledger (`if(false /* V159 SUPERSEDED */)` branches) — anchor CSE:4560 |
| V79-INERT | INERT (parse only; live arm = V79b in RCA + V103 fallback) — anchor ME:28 RCA:722 |
| V13 | DEAD (pre-fork baseline; restored by V153) — anchor CSE:4098 |
| V67t, V67bd, V67bh, V143, V145, V146 | DEAD, compiled out `if(false)` -> V159 (forfeit hub ledger, CSE) |
| V101, V119, V127 | DEAD, deleted/commented -> V153 (force-loss hub ledger) |
| V24.8 | DEAD, replaced by V24.10 AMSD safety net (CSE ~8839) |
| V38.2 | DEAD, replaced by V42 always-reserve (FAE ~97 comment) |
| V114 | DEAD, deleted generic deploy catch-all (ATE ~4080 comment) |
| V132 | DEAD, reverted (FAE ~70 comment) |
| V67ad, V67ap | DEAD, replaced weapon-stack rules (comment refs only) |
| V67bl, V67bt | Comment-only markers (behavior folded into V38 / spy-detection method notes) |
| V47-reserve-solo (with V28 arm) | DELETED 2026-07-12 batch 1d (CSE ~8861; git + backup = undo) |
| ObjectiveHandler.java | DEAD CODE — the live objective brain is ObjectiveAnalyzer; do not wire |
| V193 hardcoded Endor block | SUPERSEDED 2026-07-08 `if(false)` — Endor now JSON-hydrated (cOA:1447) |

## 7. Summary stats

- **367 live rules** (arms) across **22 domains** after the 2026-07-13 exact-arm split + V27 sibling minting (was 357; +10: 13 ex-AMBIG rows split into 21 arms, then the 3 live V27 siblings minted per gate m00288 — §5 authority table = 24 arms); plus 1 INERT arm tracked in place (V37.4-empty-check, pass-cancel table — excluded from live counts, retirement pending its no-consumer fixture); 13 dead/deleted/inert/unresolved rows tracked in §6.
- Rules per domain: move 49, deploy-siting 38, deck-playbook 37, pull-search 33, deploy-attach 25, deploy-sequencing 23, battle-weapons 21, setup-starting 15, solo-formation 15, battle-forfeit 13, shields 13, battle-initiation 12, drain-control 11, force-budget 11, loop-safety 10, force-loss-payment 8, activation-amount 7, objective-intent 7, draw-count 6, response-routing 6, fact-services 5, pass-cancel 2. (Sums to 367.)
- Rules per file (live; a rule counts once per owning file): CSE 138, ATE 127, DE 90, ME 37, DO 21, RCA 17, BE 14, CDSE 13, OA 10, DC 10, DrE 10, FAE 8, AA 8, SS 7, CE 6, FS 4, PE 4, DPS 3, DPP 3, BP 2, ODT 2, ObjectiveType.java 1, RandoConfig.java 1, DSf 1, EvaluatedAction.java 1. (Delta-adjusted 2026-07-13 from the batch-0 baseline for the 13→21 split: CSE +2 V172 arms; CE -2 comment-only V27/V27.1 anchor hits dropped;  DE -1 V37.4 inert arm excluded from live counts; EvaluatedAction.java +1 FS-enforcement; ME +2 and PE +1 V27 siblings minted 2026-07-13 per gate m00288.)
- **41 multi-domain base tags** (§4) — the true migration workload: each needs arm-by-arm surgery.
- Worst multi-owner domains (distinct owner files today): **objective-intent** (10), **deploy-sequencing** (9), **deck-playbook** (9), **force-budget** (8), **move** (8), **pull-search** (8).
- KIND mix at manifest T0.4: 113 VETO / 78 ORDERING / 149 BANDED; batch-0 additions skew VETO (FORMATION SAFETY) plus the V192 ORDERING hub. The 2026-07-13 exact-arm split, over its 13→20 live rows: BANDED 10→14, VETO-class 2→4 (V172 gates split out as no-score gates), ORDERING 1→2 (V67z draw calc; V174 re-classed BANDED→ORDERING reserve calc).

*Batch-0 artifact — gates every later batch. Re-grep anchors before moving code; consult `Rando_AI_Rule_Audit.xlsx` + `resources/T4_Boundary_Tables_2026-07-06.md` before touching magnitudes; old rules get DOMINATED, not deleted — do the boundary math.*
