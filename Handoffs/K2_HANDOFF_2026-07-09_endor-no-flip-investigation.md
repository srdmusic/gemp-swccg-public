# K-2 HANDOFF — 2026-07-09 — Investigate: Endor Operations did NOT flip in 5 unmolested turns

**For the next K-2.** Steve played a game (2026-07-09 ~05:59–06:12) where Rando ran **Endor Operations** (dark, 8_167),
Steve did NOT attack Rando's Endor sites for ~5 turns, and the objective **still never flipped**. Figure out why and
propose a fix. Do NOT ship a fix without boundary math + Steve's OK — this is a diagnosis handoff.

Onboard first: MEMORY.md (`feedback_*` = law), `resources/BUILD_AND_DEPLOY.md` §1 (confirm code is LIVE before editing),
`Handoffs/AI_WORK_QUEUE.md` (current objective-playbook state; 15 objectives live incl. Endor).

## Replay + log
- Replay: `replays/asdf/somykkwjy449xul4.xml.gz` (zlib XML; `gunzip -c | ...`). Confirm it's the right game first.
- Live log: `logs/gemp-swccg.log` (in container `/opt/gemp-swccg/logs/gemp-swccg.log`). The game is the 05:59–06:12 block.
- Deployed build at time of game: HEAD `2b4f0450c` (15 objectives), web.jar rebuilt + JVM restarted 05:59.

## What the Endor Operations objective needs to FLIP (rules truth — verified vs Card8_167.java / Card207_025.java)
Flip requires **BOTH** flip cards on table: **Ominous Rumors** AND **Establish Secret Base**. The deck runs the V
versions. **Establish Secret Base (V) 207_25 "Deploy on Bunker if you control that site"** — so ESB can only reach the
table once Rando **controls Endor: Bunker** (a character with presence, not just an Effect). That's the whole reason
V193 exists: steer one body onto Bunker to seize control so the cost-0 ESB can deploy.

## TL;DR ROOT CAUSE (evidence-backed)
Rando got **one** flip card down (Ominous Rumors (V) deployed onto Bunker) but the **second never deployed**:
**Establish Secret Base (V) 207_25 sat in Rando's hand the ENTIRE game and never deployed, because Rando NEVER
controlled Endor: Bunker** — Bunker stayed `us:0 them:0 EMPTY` every single turn despite scoring +700. One of two
flip cards on table → objective cannot flip. No amount of "not being attacked" helps: the blocker is self-inflicted,
Rando won't commit+hold a controlling body on Bunker.

## Evidence (log, with timestamps)
1. **Loader + hydrate worked** (my JSON playbook is live and correct):
   `05:59:03 [ObjectiveAnalyzer] loaded 58 objective playbook profile(s)` and
   `05:59:03 JSON hydrate 'Endor Operations': ... reqCards=[ominous rumors, establish secret base], flipGateSite=endor: bunker, flipGateIds=[601_260,207_25,207_025], startLoc=[endor system, bunker, landing platform]`.
   Deployed Endor profile weights `{deployFlipGateSite:400}`, flipGateSite `endor: bunker` — all correct in the jar.
2. **Ominous Rumors (V) 223_19 deployed to Bunker** (flip card #1 down): `06:08:50 V88 TEXT-NAMED SITE: 'Ominous Rumors' text mentions 'bunker' → +500` → `Best action: Deploy to Endor: Bunker (700.0)`.
3. **Establish Secret Base (V) 207_25 STUCK IN HAND all game** (flip card #2 never down): repeated
   `🔍 Hand cards: ... Establish Secret Base (id=257, bp=207_25) ...` from 06:08 through 06:12+.
   Early `V59 DIAGNOSTIC NO-DEPLOYS: ... hand: [... Establish Secret Base(EFFECT,c=0)] | offered: [Take Endor Shield from Force Pile, Activate Force]` — ESB was **not an offered deploy action** (deploy requirement `controls(Filters.Bunker)` was FALSE → engine didn't offer it).
4. **Bunker EMPTY the whole game**: `Endor: Bunker [GROUND] EMPTY - us:0 them:0` at 05:59, 06:02, 06:05, 06:08, 06:12. Rando never had a controlling body there.
5. **V193 FLIP-GATE CONTROL (+400 Bunker steer) fired 0 times** this game — `grep -c "V193 FLIP-GATE CONTROL"` = 0. Yet the bytecode IS in the deployed jar (`V193 FLIP-GATE CONTROL` string present in DeployEvaluator.class). So the code shipped but its branch was never reached.
6. **Zero-drain penalties fight the objective bonus, and MOVE logic flees Bunker**:
   `06:05:23 V67g MOVE-FROM-DRAIN: leaving Endor: Landing Platform (drain 1) for Endor: Bunker (drain 0) → -250`;
   final deploy reasoning includes `V24.15 ZERO DRAIN ... -80` and `V67ah NON-BG (no drain): truly useless -350` on Bunker. Bunker has 0 force drain, so multiple rules penalize deploying/holding/moving there.
7. **The deploys route through CardSelectionEvaluator** (`V136 CS [~Rando_Cal]`), NOT DeployEvaluator. **V193 lives in `DeployEvaluator` only** — so on the CardSelection deploy route actually used here, the +400 Bunker steer is absent.

## Ranked hypotheses (for K-2 to confirm/refute against the replay + source)
1. **V193 is on the wrong route.** V193's +400 flip-gate steer is in `rando/evaluators/DeployEvaluator.java` (V136
   character-siting block). These Endor deploys resolve through `CardSelectionEvaluator` (`V136 CS`). If the CS route
   has no equivalent flip-gate steer, the one mechanism designed to force a Bunker body never applies. **Check:** does
   CardSelectionEvaluator read `getFlipCriticalControlSite()` / apply a Bunker steer? Grep both evaluators for
   `getFlipCriticalControlSite` / `V193`. This is the most likely single cause.
2. **Zero-drain penalties dominate the objective bonus on the CS route.** Even with +700 CS + V22 obj +150 + V88 +500,
   the `-350 (V67ah NON-BG)` + `-80 (V24.15 ZERO DRAIN)` + competing DRAIN sites (Landing Platform drain 1, Cloud City,
   Dagobah) mean a draining site wins the turn's single deploy, every turn. Bunker is a 0-drain site so it's always
   "wasted." **Check:** per-turn, what site actually won vs Bunker's net score? Was Bunker ever the top pick that
   resulted in a landed body?
3. **MOVE-FROM-DRAIN actively empties Bunker.** Even if a body lands on Bunker, `V67g MOVE-FROM-DRAIN` (-250 to move
   ONTO a lower-drain site, i.e. it discourages moving to Bunker; verify direction) and the general "leave 0-drain
   sites" pressure may pull any body back off Bunker toward drain sites. **Check:** did a body ever occupy Bunker for
   even one turn, then leave?
4. **Chicken-and-egg / ESB never offered.** ESB (V) needs Bunker control to be a legal deploy; Bunker control needs a
   prioritized body; the body is deprioritized by zero-drain penalties and the missing V193-on-CS steer. So ESB is
   never offered (confirmed at 06:08:50 V59 diagnostic) and stays in hand. Breaking any one link (esp. #1) likely fixes it.

## Candidate fixes (DO NOT implement yet — evaluate + boundary-math + get Steve's OK)
- **Most promising:** make the V193 flip-gate steer fire on the CardSelection deploy route too (mirror the
  DeployEvaluator V193 into CardSelectionEvaluator's siting, or move the flip-gate steer into the shared
  `CharacterDeploySiteEvaluator.evaluateSite` so BOTH routes get it). The analyzer already exposes
  `getFlipCriticalControlSite()/getFlipCriticalControlCardIds()` — the data is there; only the CS consumer is missing.
- **Tuning:** when a site is the objective flip-gate control site AND Rando holds the gated card, the flip-gate bonus
  must out-weigh the zero-drain/non-BG penalties (+400 was sized in DeployEvaluator vs its magnitudes; re-check the net
  on the CS route where V67ah -350 / V24.15 -80 apply). Boundary-math the net so Bunker actually wins the turn a
  controlling body is available — but ONLY while ESB is un-deployed and Bunker uncontrolled (self-limiting, like V193).
- **Do NOT** just inflate objective relevance globally — that would over-commit every location objective to 0-drain
  sites. Keep it scoped to the flip-gate-control case (holds gate card + doesn't control site yet).

## Landmines
- Confirm the code path is LIVE before editing (BUILD_AND_DEPLOY §1). V193 shipped but never fired — classic "code
  present, branch unreached." Prove which evaluator/route the Endor deploys actually take (log says `V136 CS` =
  CardSelectionEvaluator) before touching DeployEvaluator.
- Mirror any evaluator change rando→chosenone. Both changelogs. work-verifier before "done". Never deploy mid-game.
- Do the boundary math at Bunker: net score with the new steer vs the winning drain-site score, per turn, so you don't
  over-steer OR under-steer.
