# RANDO DOMAIN REGISTRY — 2026-07-12 (phase-reorg BATCH 0)

Authoritative LIVE V-tag inventory grouped into semantic domains. **One owner per domain** is the migration law this document encodes; every later batch keys on it. Objective-XOR is domain-scoped: a rule is owned by EITHER the objective brain (objective-intent, via `ObjectiveAnalyzer.getDeployObjectiveAdjustments()`) OR its phase pipeline — never both.

- Built at HEAD `5ab16f8ac` (2026-07-12, batch-1 hotfixes included). Branch `rando-consolidation-2026-06-23`.
- Scope scanned: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/*.java`, `.../rando/strategy/*` (rules live in DPP, DPS, OA, SS, DO, AA), `.../models/common/strategy/*`, plus `RandoCalAi`/`DecisionSafety`/`DecisionContext` where manifest arms live there. `.bak` files and `if (false /* SUPERSEDED */)` blocks excluded.
- Backbone: `resources/Rando_Section_Manifest_2026-07-06.xlsx` (340 single-owner arms, T0.3/T0.4), re-verified against the live tree; 30 rows added (manifest gaps + everything shipped 2026-07-07 → 2026-07-12: V192 pull hub, V193, FORMATION SAFETY, batch-1 hotfixes).
- A *rule* = one V-tag arm (multi-arm tags appear once per arm, `Arm of` set). KIND per plan §4: VETO / ORDERING / BANDED.
- **Anchor semantics**: `FILE:line` = first live occurrence of the base tag in that file at HEAD (block may start at a nearby comment); multi-arm tags share the base-tag anchor — grep the arm's log string for the exact block. Manifest 07-06 line refs inside Trigger text have drifted; re-grep before moving code.
- File abbrevs: ATE=ActionTextEvaluator CSE=CardSelectionEvaluator DE=DeployEvaluator ME=MoveEvaluator BE=BattleEvaluator DrE=DrawEvaluator PE=PassEvaluator FAE=ForceActivationEvaluator CE=CombinedEvaluator DPP=DeployPhasePlanner DPS=DeployPhaseScript OA=ObjectiveAnalyzer SS=ShieldStrategy DO=DeckOracle SC=StrategyController AA=ActionAudit RCA=RandoCalAi DC=DecisionContext DSf=DecisionSafety / shared common: CDSE=CharacterDeploySiteEvaluator FS=FormationSafety FRS=ForceReserveService MF=MaintenanceFacts MP=MovePredicates ShF=ShieldFacts. The file IS the decision route (ATE=text-ranked top-level, CSE=card-selection prompts, DE=deploy scoring, ME=move destinations, BE=initiation, CE=merge/select).
- Every rando evaluator has a **chosenone mirror**; `common/strategy` files are SHARED (no mirror drift). "Both bots" applies to every row unless noted.

## 1. Domain overview

| Domain | Live rules | Current owner files (count) | Target single owner (phase plan) |
|---|---|---|---|
| setup-starting | 15 | ATE CSE DC DE DO OA RCA (7) | SETUP slot (turn spine); code today = CSE turn-0 blocks |
| activation-amount | 7 | ATE DC FAE (3) | ACTIVATE (FAE + named ATE blocks); the V61c -6000 / V168 +5000 / V38.3 +500 triangle is ONE boundary across 3 sites |
| force-budget | 7 | ATE BE CE CSE DE DPP DrE ME PE (9) | ForceReserveService (shared svc; one cached computation) + MaintenanceFacts basis |
| drain-control | 10 | ATE CSE (2) | CONTROL slot (turn spine 2a/2b) |
| deploy-sequencing | 23 | ATE CE CSE DC DE DO DPS ObjectiveType.java RCA (9) | DEPLOY pipeline / DEPLOY-1 (DPP + DPS bucket walk + CE epilogue) |
| deploy-siting | 37 | AA CDSE CSE DE ME (5) | DEPLOY pipeline / DEPLOY-2 (hub = shared CDSE.evaluateSite; V136) |
| deploy-attach | 25 | ATE CSE DE DO (4) | DEPLOY pipeline / DEPLOY-3 (gate = V158; weapon pulls also gated by V185/V120) |
| solo-formation | 12 | ATE BE CDSE CE CSE DE FS (7) | FormationSafety (shared common/, both bots) + CE enforcement |
| battle-initiation | 12 | ATE BE CSE DC DE FAE ME (7) | BATTLE pipeline / BATTLE-1 (TRAP: ATE V25 tier block + BE both score initiation — preserve the SUM) |
| battle-weapons | 22 | ATE CSE DE (3) | BATTLE pipeline / BATTLE-2 |
| battle-forfeit | 13 | CSE RCA (2) | BATTLE pipeline / BATTLE-3 (hub = v159ForfeitScore, CSE) |
| move | 49 | AA ATE CSE DE DPP ME RCA RandoConfig.java (8) | MOVE pipeline (T4 clobber ladder; dual-utility semantics) |
| draw-count | 6 | DrE (1) | DRAW (DrE) |
| force-loss-payment | 8 | CSE (1) | FORCE-LOSS engine (hub = V153, two byte-identical copies in CSE) |
| shields | 13 | ATE CSE RCA SS (4) | SHIELDS engine (ShieldStrategy + ShieldFacts) |
| pull-search | 32 | AA ATE CE CSE DC DE DO RCA (8) | PULL ENGINE (hub = V192 in ATE since T4.2); facts stay SVC-ORACLE (DeckOracle) |
| objective-intent | 7 | AA ATE BP CSE DC DE DPP OA ODT RCA (10) | SVC-INTEL (ObjectiveAnalyzer — the LIVE brain; ObjectiveHandler.java is DEAD, do not wire) |
| loop-safety | 9 | AA ATE CE CSE DSf DrE RCA (7) | SVC-SAFETY (DecisionSafety + ATE loop guards + CE) |
| pass-cancel | 2 | DE PE (2) | SVC-SAFETY (PassEvaluator; V148 cancellability semantics) |
| response-routing | 6 | ATE CSE (2) | RESPONSE router (thin dispatcher; routes to callable sections) |
| deck-playbook | 37 | ATE BE CSE DE DO DrE ME OA RCA (9) | PLAYBOOKS overlay (phase back-pointers per council condition) |
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
| V186 |  | SETUP | CSE:809 OA:21 | ORDERING | — | I Want That Map turn-0 script: temp-ID-safe Starkiller Base SYSTEM +400 in evaluateDeployLocation (CSE 802-847, resolves blueprints via context.getBlueprints() - the… | LIVE |
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

### force-budget — 7 rules
*"Reserve N Force for X" rules: maintenance, DTF, interrupts, transit.* Target owner: ForceReserveService (shared svc; one cached computation) + MaintenanceFacts basis.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V22.3-maintenance | V22.3 | DEPLOY-1 | DPP:210 | ORDERING (reserve calc) | reserves maintenance upkeep before location deploys | Deploy-phase planner reserves total maintenance upkeep Force (MaintenanceFacts basis via ForceReserveService) before planning deploys | LIVE (manifest gap — added batch 0) |
| V24.5 |  | DEPLOY-1 | DE:2306 ATE:4235 | BANDED | -50/-50 warnings; small ATE use/lose-force aversions | Penalize deploys that leave less Force than existing maintenance cards need at end of turn | LIVE (manifest gap — added batch 0) |
| V27 |  | BATTLE-1 | BE:18 CE:210 ME:822 PE:12 | BANDED | battle-interrupt Force reservation warnings/penalties | HOMED arm = BattleEvaluator 665-727 battle-interrupt Force reservation (4 hits). MULTI-ARM not in handoff §8: MoveEvaluator has TWO other arms — buddy-protect 536-644 (5… | LIVE |
| V27.1 |  | BATTLE-1 | BE:18 CE:210 PE:13 | BANDED | Draw Their Fire tax reservation | PassEvaluator keeps Force in reserve for battle interrupts when opponent has Draw Their Fire; BattleEvaluator side accounts for it at | LIVE |
| V67z |  | DRAW | DrE:633 DE:301 | ORDERING (reserve calc) | +1 Force per staged Jedi | Hidden Path (unflipped): reserve Force for the move-phase Mapuzo transit per Jedi staged at Underground Corridor — draw-side + deploy-side twins | LIVE (manifest gap — added batch 0) |
| V141 |  | PLAYBOOKS | ATE:604 | VETO | -2000 | Transport interrupts (Elis Helrot/Nabrun Leids: "draw destiny, use that much Force") held unless 4+ Force available (floor incl. destiny risk) | LIVE (manifest gap — added batch 0) |
| V174 |  | DEPLOY-2 | CSE:1084 | BANDED | budget reserves inside the wave projection, no own points — upkeep for MAINTENANCE cards + 1 force per battle interrupt in hand (max 2) | CSE 1052 + 5645 (inside v173WaveProjection). Also see V177 RESERVE CAP comment at 5700 (separate | LIVE |

### drain-control — 10 rules
*Drain go/no-go, drain ordering, drain-value gates, retrieval (named gap).* Target owner: CONTROL slot (turn spine 2a/2b).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V24.15-drain | V24.15 | CONTROL | ATE:1595 | VETO | — | NEVER drain at 0: -9999 hard block (Surprise Assault trap avoidance), ATE 5207-5304 + the 5534 note-line; V189 net-value gate now sits upstream at this check. OTHER… | UPDATED 2026-07-07 (EFFECTIVE DRAIN; ex-V189b folded in) |
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

### deploy-siting — 37 rules
*WHERE a character/unit goes: site scoring, contest/protect/spread.* Target owner: DEPLOY pipeline / DEPLOY-2 (hub = shared CDSE.evaluateSite; V136).

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
| V193-steer | V193 | DEPLOY-2 | DE:1902 CSE:2211 | BANDED | +400 one body | Steer ONE ability-body to the objective flip-gate site when analyzer named it, Rando does not control it, and the flip card waits in Reserve; CS-route copy added 2026-07-09; FS exempts the steer | NEW since manifest (post-2026-07-06) |

### deploy-attach — 25 rules
*Weapons, devices, pilots, ships: attach/aboard gates.* Target owner: DEPLOY pipeline / DEPLOY-3 (gate = V158; weapon pulls also gated by V185/V120).

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

### solo-formation — 12 rules
*Steve's four laws + solo/buddy discipline (VETO-class).* Target owner: FormationSafety (shared common/, both bots) + CE enforcement.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V29.5-buddy | V29.5 | DEPLOY-2 | CSE:1027 | BANDED | +40 own location / -150 solo to empty opponent location / -100 solo to enemy-occupied / +10 with friendlies | CSE 2828-2939 GENERAL BUDDY SYSTEM + boundary comment 1005. V113 was implemented inside this try-block. Only penalizes solo deploys to OPPONENT locations (that gap is… | LIVE |
| V113 |  | DEPLOY-2 | CSE:998 | BANDED | -300 solo ability>=3 character at ANY location | CSE 2913-2933 live (implemented inside V29.5's try-block); hits at 976/1004 are boundary-math comments in the V169/V171 blocks (stays under V169 PROTECT | LIVE |
| V156 |  | DEPLOY-2 | CDSE:18 | BANDED | -600 SOLO HOLD / +250 SOLO OK PREFER BUDDY | Rewritten in place 2026-06-25 (LIVE); old flat -300 guard commented out at CDSE ~463-472 (version-table col D confirms). Turns 1-2, character at BG site, not… | UPDATED 2026-07-07 (solo stack-math 4b76cb611) — LIVE hub arm in shared CDSE |
| V169-deploy | V169 | DEPLOY-2 | DE:942 CSE:934 | BANDED | +800..+1100 (800 + min(300, excess*30)) deploy-buddies; +500 PROTECT URGENT umbrella on the Deploy action | §8 arm 'deploy-protect'. DE 923-962 umbrella; CSE 912-976 deploy-buddies (6) + shared outpowered-helper at CSE 5611 (1, also used by retreat arm). V169 tag UPDATED… | LIVE |
| V172 |  | DEPLOY-2 | CSE:907 | BANDED | gate, no own points — enables/disables V171 +600 and V169-deploy +800..+1100 | Projected-power winnability test (projected >= theirPower - 2). CSE 925/960 (PROTECT brake) + 1045/1073 (CONTACT gate). Uses V173 wave | UPDATED 2026-07-11 (SOLO DOMINANCE Steve ruling: >=2x weapon-adjusted dominance deploys+battles buddy-less; character-gated) |
| BATCH1b-two-weak-solos | FORMATION | SVC-SAFETY | CSE:6282 | BANDED | -800 (penalty not veto — repositioning stays possible) | Weak mover relocating SOLO to an uncontested EMPTY site while leaving a lone weak buddy at an uncontested origin (Chiraneau/Ozzel escape; boundary 327.5 → -472.5 loses to Pass) | NEW since manifest (post-2026-07-06) |
| FS-L1-abandon | FORMATION | SVC-SAFETY | FS:150 CSE:6276 | VETO (hardVeto) | un-outvotable; OR-merged on EvaluatedAction | L1: never move the last buddy away leaving ONE weak (ability<4 weapon-adj) body behind at a contested origin; exempt if leftover ability>=4, origin doomed (gap>=6), or origin uncontested | NEW since manifest (post-2026-07-06) |
| FS-L2-no-destiny-battle | FORMATION | SVC-SAFETY | FS:107 BE:426 BE:700 | VETO (hardVeto) | un-outvotable | L2: never voluntarily initiate battle where total ability < 4 (engine BattleDestiny truth — zero normal destiny draws); fallback arm vetoes battle phase when NO contested location reaches 4 | NEW since manifest (post-2026-07-06) |
| FS-L3-solo-deploy | FORMATION | SVC-SAFETY | FS:199 CSE:2161 | VETO + BANDED | hardVeto when pair formable this phase is budget-starved; -800 NO-PLAN when no buddy plan exists (07-12 rewrite, deadlock-free) | L3: never deploy a weak solo when a buddy is in hand/affordable; exemptions: spies, 2x dominance (V172 law), flip-gate steer (V193-class) | NEW since manifest (post-2026-07-06) |
| FS-L4-solo-charge | FORMATION | SVC-SAFETY | FS:122 CSE:6272 | VETO (hardVeto) | un-outvotable | L4: never move a weak (ability<4 weapon-adj) solo character into an enemy-held location; exempt: spies, uncontested destination, >=2x effective dominance | NEW since manifest (post-2026-07-06) |
| FS-enforcement | FORMATION | SVC-SAFETY | CE:214 CE:275 | VETO plumbing | vetoed actions unselectable regardless of score; all-veto fallback: pass if V148-cancellable else least-bad; V67bc epilogue skips vetoed actions | CombinedEvaluator enforcement of hardVeto (2026-07-11c + 07-12 P0 patch): the anti-dilution mechanism — no bonus stack can wash a veto out | NEW since manifest (post-2026-07-06) |
| FS-pull-route | FORMATION | SVC-SAFETY | ATE:5672 ATE:5742 | VETO + BANDED | hardVeto (L4/L3) / -800 NO-PLAN | Location game-text pulls ("[download] X here") force the destination and bypass CS siting: resolve the pulled character from Reserve, run FS deploy checks with dest=source location; flip-plan exemption (first-name token, batch 1a/1c fixed the inert matcher) | NEW since manifest (post-2026-07-06) |

### battle-initiation — 12 rules
*Whether/where to initiate battle.* Target owner: BATTLE pipeline / BATTLE-1 (TRAP: ATE V25 tier block + BE both score initiation — preserve the SUM).

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

### battle-weapons — 22 rules
*Weapons-segment window: fire, interrupts, targeting, destiny mods.* Target owner: BATTLE pipeline / BATTLE-2.

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V24.2 |  | BATTLE-2 | ATE:3058 CSE:8170 | BANDED | +250 Lando / +200 Lobot pull; drain +1 acceptance | MISFIT FLAG: slice assigns BATTLE-2 but neither arm is battle. CSE ~7588-7600 = Lando/Lobot reserve-pull priority w/ V47 not-alone amendment (PULL-ENGINE/PLAYBOOK); ATE… | LIVE |
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
| V144 |  | BATTLE-2 | ATE:1057 | VETO | — | You Are Beaten mode gating: mode-2 IAYF search hard-blocked -2000 universally; +500 battle-freeze in battle phase (ATE | LIVE |
| V147 |  | BATTLE-2 | ATE:800 | VETO | — | IAYF Lost-Pile mode -2000 when saber not actually in Lost Pile (ATE | LIVE |
| V155 |  | BATTLE-2 | ATE:842 | VETO | — | Welcome Home Lord Tyranus: oracle-gated -2000 on mode-1 pull (save premium battle mode). ATE 797-876. Implements parked | LIVE |
| V175 |  | BATTLE-2 | ATE:2677 CSE:4342 | BANDED | kill shot +400+power*40 cap 900; substitute delta*60; our-char -100 | ATE 3060-3150 = kill-shot + substitute-destiny arms (BATTLE-2). CSE 4039-4066 (3 hits) = protect-battle-interrupts-from-force-loss-picker arm — force-loss territory;… | LIVE |

### battle-forfeit — 13 rules
*Damage segment: forfeit picker + loss-vs-forfeit.* Target owner: BATTLE pipeline / BATTLE-3 (hub = v159ForfeitScore, CSE).

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
*Which zone pays Force loss.* Target owner: FORCE-LOSS engine (hub = V153, two byte-identical copies in CSE).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V21-loss-protect | V21 | FORCE-LOSS | CSE:214 | VETO | — | FOUND IN REGION arm of V21: HARD BAN losing/forfeiting flip-required or objective-pullable cards (CSE 3756,3976,4145,4535-4544,5228-5237). UNCLAIMED V21 ARMS:… | UPDATED 2026-07-12 (cleanCardName strips icon tokens so [Special Edition] titles arm protection) |
| V25-loss-protect | V25 | FORCE-LOSS | CSE:1377 | BANDED | -300..-500 protect tier | §8 arm (lightsaber loss-protect): protect Hunt Down lightsabers in hand/combined-loss/unknown-loss (-500/-400/-300; +200 unknown-gain) — CSE… | LIVE |
| V28-DTF | V28 | FORCE-LOSS | CSE:4013 | ORDERING | — | Draw Their Fire arm: Force pile = interrupt ability, lose from reserve instead (heavy scaled penalty; live copy CSE 4115-4135, old copy 3890-3910 commented, + 3976… | LIVE |
| V28-dtf-force-pile | V28 | FORCE-LOSS | CSE:4013 | BANDED | heavy penalty protecting Force pile when Draw Their Fire active | Live copy CSE 4115-4135 + priority comment 3976; older copy 3890-3910 is //-commented. Different rule sharing the V28 tag — belongs to the force-loss cluster, not… | LIVE |
| V109 |  | FORCE-LOSS | CSE:4012 | BANDED | -300 tier | MY LORD: protect senators from loss/cost picks (-300) inside the loss-selection order (CSE 3825-3856, 3975 preserved-protections | LIVE |
| V153 |  | FORCE-LOSS | CSE:4011 | ORDERING | — | FORCE-LOSS hub: unified loss order, char/life-force tiers (protect chars when lifeForce>=4, survival mode <4), HAND FLOOR -700, PRIORITY CARD -100. TWO COPIES (regular… | UPDATED 2026-07-07 (THIN RESERVE tier) |
| V175a |  | FORCE-LOSS | CSE:4012 | ORDERING | — | Turn-gate on weapon protection inside the V153 order: protection starts turn 4; turns 1-3 the deck is dense, lose the known weapon (CSE 4049, 4079). Reorders, does not… | LIVE |
| V178-loss | V178 | FORCE-LOSS | CSE:4013 | ORDERING | — | §8 arm split (forfeit vs force-loss): loss arm reranks wielded weapons zone 600 -> 150 (CSE 4071-4103). UNCLAIMED ARM: forfeit arm (CSE:9217, armed chars slightly… | LIVE |

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

### pull-search — 32 rules
*Reserve-deck pulls/searches: scoring + dead-pull verdicts.* Target owner: PULL ENGINE (hub = V192 in ATE since T4.2); facts stay SVC-ORACLE (DeckOracle).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V22.6 |  | PULL-ENGINE | RCA:79 DC:84 CSE:8036 DO:29 | ORDERING | +500 location priority / -500 failed-pull | Three arms, all PULL-ENGINE: DeckOracle plumbing (RandoCalAi/DecisionContext/DeckOracle hits = wiring + reset, the DeckOracle file header itself is V22.6); CSE 7454-7467… | predicate/choice arm LIVE (RCA); location-priority magnitudes interact with V192 |
| V24-amsd-bespin-gate | V24 | PULL-ENGINE | ATE:1686 | VETO | — | ATE 1612-1636: -9999 hard block on AMSD when no Bespin system on table. Discovered arm, not in §8; belongs with the V29.4 AMSD gate family. ATE V24 hits split 4+4 with… | LIVE |
| V24-tdigwatt-exhausted-search | V24 | PULL-ENGINE | ATE:1686 | VETO | -400 soft-veto | ATE 1804-1822: -400 block when all 4 TDIGWATT search targets already pulled (dead-search guard). Discovered arm, not in | LIVE |
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
| V177 |  | PULL-ENGINE | DE:828 ATE:267 CSE:889 DO:21 | VETO | -2000 block, skips all further scoring incl. V116 | Dead-search gate: classify parsed pull targets ALIVE/JUNK/DEAD; block only when no ALIVE, >=1 DEAD, no JUNK. Supersedes fire-every-turn heuristic for parseable pulls.… | UPDATED 2026-07-07 (pull-parser 692fec3cf) |
| V177a | V177 | PULL-ENGINE | ATE:267 | VETO | — | Same-session amendment: added the JUNK class + >=6-char loose word-rescue after false-blocks. Documented under | LIVE |
| V183 |  | PULL-ENGINE | ATE:366 DO:615 | VETO | — | Deck Oracle retool: no verb parsing — scan source game text for catalogued deck titles (>=6 chars) and judge by real ZONE; block when every named target is out of… | LIVE |
| V192 |  | PULL-ENGINE | ATE:4484 | ORDERING (hub) + VETO chain | base +150 deploy-grade / +5500 activate-grade (P1 stand-down when V61c holds destiny buffer); location tier 1500/1400/1300/1200 by source cat, weapon 600, device 400; +50 [download], +25 chars-in-hand; clamp 1750 deploy / 7100 activate | THE merged pull scorer (T4.2, 2026-07-06): ONE emit per reserve-deck pull; vetoes (V60 guards, V66, V67h, V67ac, V95, V131, V67ar/V67ao/V149) run first and short-circuit | NEW since manifest (post-2026-07-06) |

### objective-intent — 7 rules
*Objective detection, flip intel, objective deploy adjustments.* Target owner: SVC-INTEL (ObjectiveAnalyzer — the LIVE brain; ObjectiveHandler.java is DEAD, do not wire).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V21-analyzer | V21 | SVC-INTEL | OA:32 DPP:23 AA:23 | ORDERING | — | §-arm per slice: ObjectiveAnalyzer runtime objective-text parser (V21 is its founding tag) + planner objective-awareness wiring; ActionAudit hit is a dormant… | LIVE |
| V24.7-intel |  | SVC-INTEL | RCA:82 BP:61 CSE:334 DC:85 ODT:6 | BANDED | n/a — intel service, no direct score | OpponentDeckTracker destiny-intel service (deck-peek scanning, average destiny) + BattlePredictor intel-aware prediction methods. NOTE:… | LIVE |
| V25-detector | V25 | SVC-INTEL | OA:20 | ORDERING | — | §8 arm: Hunt Down V + ISB Operations objective detection (flip conditions, Vader-on-table checks, ISB agent counting, back-side flip-back detection). Detection service… | LIVE |
| V29-objtext-intel | V29 | SVC-INTEL | OA:21 | ORDERING | — | FOUND IN REGION arm of V29, UPDATED 2026-07-06 (TDIGWATT bug B): stores raw objective game text + objectiveForbidsDeployingExecutor() predicate consumed by the… | LIVE |
| V67ak |  | SVC-INTEL | ATE:1955 DE:3936 OA:21 | BANDED | +800 flip-critical tier | Universal KEY-CHARACTER token extractor (ObjectiveAnalyzer service, 235/323) with two scoring consumers: +800 pull priority (ATE 5102-5162) and +800 deploy priority (DE… | LIVE |
| V170-cover | V170 | SVC-INTEL | RCA:611 ATE:3754 | ORDERING | — | §8 arm 'cover decision': yes/no intercept in RandoCalAi 584-630 answering the engine's 'deploy as Undercover spy?' prompt (YES when opp total drain >= 1, NO early game).… | LIVE |
| V193-intel | V193 | SVC-INTEL | OA:79 | ORDERING | no score — names the flip-gate control site | Objective flip-gate control-site intel (Endor: Bunker etc.); reset+reparse per detection; JSON-hydrated profiles supersede the hardcoded Endor block | NEW since manifest (post-2026-07-06) |

### loop-safety — 9 rules
*Loop breaking, retry budgets, concede, dormant checks.* Target owner: SVC-SAFETY (DecisionSafety + ATE loop guards + CE).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V25-concede | V25 | SVC-SAFETY | RCA:494 | ORDERING | — | §8 arm: AUTO-CONCEDE when losing by 30+ in Lost Pile (RandoCalAi:495); V67aw defers the actual | LIVE |
| V44 |  | SVC-SAFETY | RCA:583 | VETO | — | ALWAYS accept opponent revert requests — hard policy, never blocks. Shares its two code lines with V67j (both tags on RandoCalAi:556,579); counts overlap by | LIVE |
| V67aw |  | SVC-SAFETY | RCA:100 CSE:951 | ORDERING | — | Concede DEFER: pendingConcede flag set when losing threshold hit, fires only after next battle phase ends (RandoCalAi 100-1705). CSE hits are strategy comments… | LIVE |
| V67j |  | SVC-SAFETY | RCA:583 | ORDERING | — | Revert-accept mechanics: never assume index 0 = Yes; scan the results array for the actual Yes/No indexes (RandoCalAi 556-594). Two lines shared with V44. NOTE: V67j has… | LIVE |
| V68-dormant | V68 | SVC-SAFETY | AA:20 | VETO | — | ActionAudit unified pre-flight validation framework — DORMANT, ZERO CALLERS (plan traps ledger: label, don't wire, don't clean up). Consolidation targets documented… | LIVE |
| V148 |  | SVC-SAFETY | DSf:62 CE:66 | VETO | — | Done/Cancel must remain reachable: decisions explicitly offering Done/Cancel are exempt from random-pick correction so deploy-abort stays | UPDATED 2026-07-10 (deploy-abort) + 2026-07-12 (cancellability semantics reused by FS all-veto fallback) |
| V163 |  | SVC-SAFETY | ATE:44 DrE:27 | VETO | — | Cancel-loop HARD VETO -100000 (replaced old additive -200 that got dominated). The uniform veto every loop-breaker falls back to; V167/V169 carve the explicit | LIVE |
| V167 |  | SVC-SAFETY | ATE:107 DrE:27 | VETO | — | Veto-exemption policy: phase-fundamental actions (Activate Force, draws) are NEVER hard-vetoed — soft -200 instead (pre-V163 value). Explicit exemption to the V163 law,… | LIVE |
| V191 |  | SVC-SAFETY | RCA:845 CE:72 | BANDED | n/a — log-only instrumentation | NEW 2026-07-06 (post-version-table; no xlsx row — add at T0.4 regen): TOP-N candidate logging, the dominance-regression detector (plan T0.5). CombinedEvaluator logs… | LIVE |

### pass-cancel — 2 rules
*Pass baseline + pass/cancel gates.* Target owner: SVC-SAFETY (PassEvaluator; V148 cancellability semantics).

| Tag/arm | Arm of | Sect | Anchor | KIND | Magnitude / verdict | Trigger | Status |
|---|---|---|---|---|---|---|---|
| V37.4 |  | DEPLOY-1 | PE:13 DE:3318 | BANDED | pass-penalty / penalty-reduction tier | Two arms, both deploy-urgency: DE 3130 reduces the empty-site penalty when no opponent location is deployable; PassEvaluator 153-162 HAND BLOAT (10+ cards in hand… | LIVE |
| PASS-baseline |  | SVC-SAFETY | PE:29 | BANDED | ~5-8 baseline | The Pass action baseline other sections deliberately score against (V61c lands below it by design) | LIVE (manifest gap — added batch 0) |

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
| V22.2-flip | V22.2 | PLAYBOOKS | CSE:3447 ME:594 OA:20 | BANDED | +/-30..160 tier | Post-flip protection arm (slice-homed): deploy-to-protect +60, pre-flip fortify scaled penalties -30..-160, post-flip STAY/CONSOLIDATE move logic +/-30..160 (ME… | LIVE |
| V22.5 |  | PLAYBOOKS | ATE:4249 DE:5686 ME:649 OA:1884 | BANDED | +100..300 tier | AMSD/Bespin ship priority (+300 no-ship-at-Bespin, +100 after; +120 pilot+ship combo) + PRE-FLIP CONSOLIDATION lone-outgunned move +100..160 (ME 1339-1399) +… | LIVE |
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
| V29.7-flip | V29.7 | PLAYBOOKS | RCA:1201 OA:374 | ORDERING | — | §8 arm (flip): per-evaluation flip-status refresh (RandoCalAi:1115 -> ObjectiveAnalyzer.refreshFlipStatus). Service plumbing, no | LIVE |
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
| V160 |  | PLAYBOOKS | ATE:928 OA:21 | BANDED | +800 tier | Shield Will Be Down In Moments (Hoth invasion): push Target The Main Generator +800; ObjectiveAnalyzer detection | LIVE |

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
| V153 x2 | evaluateForceLoss <-> evaluateForceLossOrForfeit (CSE) | Byte-identical zone-order copies until an extract-method pass — edit BOTH |
| V159 x2 call sites | v159ForfeitScore() called from both forfeit paths (CSE) | Shared helper (lowercase, not grep-counted) must travel with the tag |
| V88/V89/V193 DE<->CS | DeployEvaluator arm <-> CardSelectionEvaluator (CS) twin | Same rule on two decision routes; keep magnitudes in lockstep |
| V67z x2 | DrE transit reserve <-> DE deploy-phase twin | Same reservation on two phases |
| V61c/V168/V38.3 triangle | FAE keep-3 cap <-> ATE activate block <-> ATE confirm carve-out | THREE sites share DecisionContext.isBattlePlausibleThisTurn(); V192 activate-grade base (+5500) must outrank V168 (+5000) |
| BATTLE-1 SUM | BE initiation scoring + ATE V25 power-tier block | The SUM is the behavior — preserve it when changing either side |
| V156 <-> V136 §A/B | CDSE solo-hold arm <-> CDSE team-viability/over-stack | Solo stack math (2026-07-07) tuned against §A/§B bands — re-check boundary cases together |
| rando <-> chosenone | every evaluator file | Mirrored edits required except shared common/strategy classes (FS, CDSE, MP, MF, ShF, FRS) which CANNOT drift |

## 4. Multi-arm / multi-domain tags (the offenders)

39 base tags have live arms in 2+ domains. Every arm is a separate registry row above; migrate ARMS, not tags.

| Base tag | Domains its arms live in |
|---|---|
| V25 | battle-initiation, deck-playbook, drain-control, force-loss-payment, loop-safety, move, objective-intent |
| V29 | deck-playbook, move, objective-intent, shields |
| V21 | force-loss-payment, objective-intent, setup-starting |
| V29.9 | battle-initiation, battle-weapons, drain-control |
| V36 | battle-weapons, deploy-siting, move |
| V37 | battle-forfeit, battle-weapons, pull-search |
| V38.3 | activation-amount, move, response-routing |
| V51 | battle-weapons, deploy-siting, shields |
| V53 | move, response-routing, shields |
| V60 | deploy-sequencing, move, pull-search |
| FS | pass-cancel, solo-formation |
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

Heavyweights: **V169** (5 arms; magnitudes frozen per SVC-SAFETY), **V47** (3 arms: LANDO STAY ME ladder, LANDO PULL TDIGWATT CSE ~8112, reserve-solo DELETED batch 1d), **V32** (ME ability>=4 move protection ME:930 + DPP ability-contribution calc DPP:1430), **V25** (7 domains — Hunt Down playbook cuts through everything), **V24.15** (TDIGWATT AMSD mega-priority ATE:1595+ vs EFFECTIVE-DRAIN control arm ATE:5207+), **V21** (starting-ban / loss-protect / analyzer-intel), **V35 family** (8 sub-tags across battle-weapons + playbook), **V37** (5 arms), **V52/V53/V60/V61** (3-4 arms each). Manifest merge-duplicates to watch: V97, V177, V67an, V37.3, V24.2, V169-loop-soft-block.

## 5. Ambiguous-domain rows (flagged, with recommendation)

| Tag/arm | Flag |
|---|---|
| V37.4 | AMBIG: deploy-sequencing (deploy-phase pass penalty) — REC pass-cancel |
| V156 | AMBIG: deploy-siting (fires in CDSE siting hub) vs solo-formation — REC solo-formation |
| V169-deploy | AMBIG: deploy-siting — REC solo-formation (protect-endangered-solo intent) |
| V172 | AMBIG: deploy-siting (gates V171/V169-deploy siting bonuses) — REC solo-formation (it IS the dominance exemption law) |
| V174 | AMBIG: deploy-siting (lives inside V173 wave projection) vs force-budget — REC force-budget |
| V29.5-buddy | AMBIG: deploy-siting — REC solo-formation |
| V27 | AMBIG: battle-initiation (fires on initiation scoring) vs force-budget (it is a reservation) — REC force-budget |
| V27.1 | AMBIG: battle-initiation vs force-budget — REC force-budget |
| V24.2 | AMBIG (manifest MISFIT note): owned BATTLE-2 but arms are Lando/Lobot reserve-pull priority + drain acceptance — REC pull-search (CSE arm) + drain-control (ATE arm) at migration time |
| V193-steer | AMBIG: objective-intent (intent) vs deploy-siting (it is a siting bonus) — REC deploy-siting |
| FS-enforcement | AMBIG: loop-safety (CE is SVC-SAFETY co-owned) — REC solo-formation (it exists to enforce the four laws) |
| V141 | AMBIG: move (transport = movement) vs force-budget — REC force-budget |
| V67z | AMBIG: deck-playbook (Hidden Path specific) — REC force-budget (reservation mechanics) |

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
| V193 hardcoded Endor block | SUPERSEDED 2026-07-08 `if(false)` — Endor now JSON-hydrated (OA:1447) |

## 7. Summary stats

- **357 live rules** (arms) across **22 domains**; 13 dead/deleted/inert/unresolved rows tracked in §6.
- Rules per domain: move 49, deploy-siting 37, deck-playbook 37, pull-search 32, deploy-attach 25, deploy-sequencing 23, battle-weapons 22, setup-starting 15, battle-forfeit 13, shields 13, solo-formation 12, battle-initiation 12, drain-control 10, loop-safety 9, force-loss-payment 8, activation-amount 7, force-budget 7, objective-intent 7, draw-count 6, response-routing 6, fact-services 5, pass-cancel 2.
- Rules per file (live; a rule counts once per owning file): CSE 136, ATE 127, DE 91, ME 36, DO 21, RCA 17, BE 14, CDSE 13, OA 10, DC 10, DrE 10, FAE 8, CE 8, AA 8, SS 7, PE 4, FS 4, DPS 3, DPP 3, BP 2, ODT 2, ObjectiveType.java 1, RandoConfig.java 1, DSf 1.
- **39 multi-domain base tags** (§4) — the true migration workload: each needs arm-by-arm surgery.
- Worst multi-owner domains (distinct owner files today): **objective-intent** (10), **force-budget** (9), **deploy-sequencing** (9), **deck-playbook** (9), **move** (8), **pull-search** (8).
- KIND mix at manifest T0.4: 113 VETO / 78 ORDERING / 149 BANDED; batch-0 additions skew VETO (FORMATION SAFETY) plus the V192 ORDERING hub.

*Batch-0 artifact — gates every later batch. Re-grep anchors before moving code; consult `Rando_AI_Rule_Audit.xlsx` + `resources/T4_Boundary_Tables_2026-07-06.md` before touching magnitudes; old rules get DOMINATED, not deleted — do the boundary math.*
