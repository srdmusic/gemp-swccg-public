# Rando Cal AI — Fixes V177–V182 (2026-06-12 to 2026-06-14)

Greppable breadcrumb for the 2026-06-12→14 fix batch. Converted from
`resources/Rando_AI_Fixes_2026-06.numbers` (a prior K-2 built that Numbers doc as
a per-fix "what/why/how" reference). The `.numbers` is the pretty source; this
`.md` is the searchable copy so you can `grep` a V-tag, diff it, and read it
without QuickLook.

Each fix's full rationale is also in `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`.
NOTE: these V-tags are **missing from `AI_CHANGELOG.md`** (which currently jumps
V160 → V184); backfilling V161–V183 into the master changelog is a separate
breadcrumb-hygiene task. See [[feedback_changelog_on_push]].

---

## ⚠️ VERSION-TAG COLLISION — read before reverting

There are **two V177s and two V178s** in this batch. They are *different fixes*.
Disambiguate by the **commit hash**, never by the V-tag alone.

| V-tag | Date | Area | Commit | One-line |
|-------|------|------|--------|----------|
| V177 (a) | 06-12 | Deck Oracle | `ddbe8eadc` | Block dead Reserve-Deck game-text searches |
| V178 (a) | 06-12 | Force fodder | `392d4d1c1` | Prefer forfeiting the *unarmed* body (tiebreaker) |
| V177 (b) | 06-13 | Deploy / winnability | `71ea3b3dc` | Count gear in winnability + survivability gate |
| V178 (b) | 06-13 | Force fodder | `523e7c789` | Protect a wielded weapon from the fodder pile |
| V179 | 06-13 | Deploy order | `f5868737e` | "farm" + planet names recognized as LOCATIONS |
| V180 | 06-13 | Wielder detection | `83dfe01d9` | Scan persona set, not just printed title |
| V181 | 06-14 | Deploy / winnability | `c0022e5d5` | Coin-flip commit into close, worthwhile fights |
| V182 | 06-14 | Draw / force mgmt | `0b5ad63d5` | Bank force for an army instead of drawing it away |

---

## V177 (a) — Deck Oracle — `ddbe8eadc` (2026-06-12)
- **Symptom:** Rando searched his Reserve Deck for cards not in his deck (e.g. Force Projection) 4+ times a game. Each try wasted the action, revealed his Reserve to the opponent, and reshuffled.
- **Root cause:** The oracle catalogs the whole deck and tracks each card's zone, but the action-scoring layer never consulted it for game-text searches. V116 even gave dead searches a +100 floor.
- **Fix:** Before scoring any action whose text says "from Reserve Deck" or "[download]": read the source card's text, parse its pull targets, classify each — ALIVE (matches a card now in Reserve; strict match or any 6+ letter word matches a reserve title), JUNK (parser garbage: >25 chars or has digits), DEAD (clean title-like string matching nothing). Block only when there is no ALIVE, at least one DEAD, and no JUNK. Multi-target pulls stay alive if ANY target is still in Reserve.
- **Key thresholds:** Block = **-2000** (skips even the V116 +100 floor). Word-rescue = 6+ letters. Junk = >25 chars or contains a digit.
- **Before → After:** Force Projection searches 4+/game → 0. 176 dead-search evaluations blocked; legit pulls stayed alive.
- **Where:** `ActionTextEvaluator.java` (rando + chosenone), gate before the V116 reserve floor.

## V178 (a) — Force fodder (forfeit choice) — `392d4d1c1` (2026-06-12)
- **Symptom:** Lightsabers kept dying with their carriers. Every lost saber costs the drain bonus plus a hit until it's re-pulled.
- **Root cause:** When choosing who to forfeit, an armed character was as likely to be picked as an unarmed one. (The drain-add itself was already perfect: 914/914 taken when offered.)
- **Fix:** In the forfeit-choice scorer, a character with a WEAPON attached gets a small penalty in the two normal paths (attrition-coverage and pure-damage). Pure tiebreaker: when two bodies are otherwise equal, give up the unarmed one first. The "already hit" and "about to die anyway" branches are untouched.
- **Key thresholds:** Weight = **-10** (deliberately tiny). Real factors it must never override (forfeit value, hit, immunity) = 60 to 1500.
- **Before → After:** Armed characters are no longer forfeited ahead of an equal unarmed body.
- **Where:** `CardSelectionEvaluator.java` `v159ForfeitScore` (rando + chosenone).

## V177 (b) — Deploy / winnability — `71ea3b3dc` (2026-06-13)
- **Symptom:** E2 — Rando left Luke + Bionic Hand + 3-PO + a saber in hand instead of overpowering Kylo at his site. E4 — a ship deployed to "contest a drain" then moved away next phase, wasting 1 Force.
- **Root cause:** Two layers vetoed the Luke attack: (a) the winnability gate over-counted reserved force (reserved 12 > force 10 → wave budget 0); (b) the team-viability score counted only character power, never the Bionic Hand or saber, so it stalled at 6 vs Kylo's 10 and hit its -2000 "can't win" cap. E4: the contest-drain bonus had no survivability check.
- **Fix:** (1) **Reserve cap** — cap reserved force at `forcePile - thisCost - 3` so upkeep can't starve the wave to zero (maintenance is already handled at deploy-score time, so it was double-counted). (2) **Gear projection** — after projecting affordable character reinforcements, also project affordable weapons/devices so an armed group reads as winnable and returns +400 instead of -2000. (3) **Survivability gate** — award the contest-drain bonus only when `ourPower + thisCard + affordable wave >= theirPower - 2`.
- **Key thresholds:** Reserve cap = `forcePile - cost - 3`. Gear: device +2, lightsaber +3, other weapon +2. Survivability tolerance = `theirPower - 2`. Win = +400, lose cap = -2000.
- **Before → After:** Luke 6 + 3-PO 1 + Bionic Hand 2 + saber 3 = 12 ≥ Kylo 10 → commits (was stuck reading 6 vs 10).
- **Where:** `CardSelectionEvaluator.java` (`v173WaveProjection`, V166) + `CharacterDeploySiteEvaluator.java` (V151).

## V178 (b) — Force fodder (weapon loss-order) — `523e7c789` (2026-06-13)
- **Symptom:** E1a — Luke's Lightsaber was thrown away as force fodder (it scored 650, the top pick) while Luke was on the table. Luke fought bare-handed all game.
- **Root cause:** The force-loss order protected battle interrupts from the fodder pile but not weapons, even when a wielder was present.
- **Fix:** Each card in hand gets a "loss-zone score" (higher = lost first); a weapon normally scores ~600 (lost early). V178 checks: does this weapon have a wielder (any non-undercover friendly character on the table, OR a character in hand to deploy it onto)? If yes, subtract 450 in the protect tier (life force ≥ 4): 600 → 150, so it's lost near-last like a character. Turn-gated to turn 4+ (turns 1-3 the deck is dense, so a known weapon is safer to lose than a blind reserve card). Survival tier (<4) and duplicates unchanged.
- **Key thresholds:** Weapon junk score ~600. Protection = **-450** (→ ~150). Protect tier = life force ≥ 4. Turn gate = turn > 3.
- **Before → After:** Luke's Lightsaber 650 (top fodder) → 150 (near-last). Fired 10x in self-play.
- **Where:** `CardSelectionEvaluator.java` (rando + chosenone), after the V175 interrupt-protect block.

## V179 — Deploy order — `f5868737e` (2026-06-13)
- **Symptom:** E3 — Rando had "I Must Be Allowed To Speak" out (it deploys a free Tatooine farm from Reserve) but never used it. The farm rotted in Reserve while he deployed a character every turn.
- **Root cause:** The card scorer ranked the farm-deploy 2050, but the deploy-priority WALK that overrides the scorer didn't recognize "farm" as a location, so the farm never entered the LOCATIONS bucket and a character always won.
- **Fix:** The deploy-priority walk sorts actions into buckets (LOCATIONS, then KEY CHARACTERS, then others) and takes the first non-empty bucket. V179 gives the walk the SAME location-keyword list the scorer already uses, so the farm classifies as LOCATIONS (step 1) and deploys first.
- **Key thresholds:** LOCATIONS = step 1 of 5. Keywords added: farm, planet names, docking bay, cantina, spaceport, city, palace, temple, village, outpost…
- **Before → After:** Farm scored 2050 but never deployed → "picking Deploy a farm 4100" → Lars' Moisture Farm enters play turn 1.
- **Where:** `DeployPhaseScript.java` (rando + chosenone), `namesLocation()`.

## V180 — Wielder detection — `83dfe01d9` (2026-06-13)
- **Symptom:** E1b — Rando never armed Luke. The "does this saber have a wielder?" guard blocked Luke's own lightsaber 12 times in one game.
- **Root cause:** The guard pulled the wielder word out of the weapon name ("luke" from Luke's Lightsaber) and looked for a character whose printed TITLE contains it. Young Skywalker's title has no "luke", so the guard decided Luke wasn't on the table. Same class as the senator-keyword bug.
- **Fix:** After the title check, the guard now also scans the character's PERSONA set. Young Skywalker carries `Persona.LUKE`; the guard lowercases each persona name and, if any matches the wielder word, treats the wielder as present. Identity lives in the persona set, not always the printed name.
- **Key thresholds:** Block score = -9999 (was firing 12x a game on a valid wielder).
- **Before → After:** NO-WIELDER blocks 12 → 0. Luke's Lightsaber (and Rey's) enter play in self-play.
- **Where:** `ActionTextEvaluator.java` (rando + chosenone), V158 NO-WIELDER branch.

## V181 — Deploy / winnability (coin-flip commit) — `c0022e5d5` (2026-06-14)
- **Symptom:** Rando passed on close contested fights and ceded the drain, even when a coin-flip battle would have been a good trade.
- **Root cause:** Raw power decided everything. But a small power gap is actually low-attrition (battle destiny decides it) and the extra body is forfeit + weapon fodder, so the power gate over-vetoed a fair fight.
- **Fix:** Inside the team-viability projection, AFTER the +400 clean-win check, if the group is still a little short, consider a coin-flip commit. Fires only when ALL hold: gap (their power - our projected power) is 1 to 3; the opponent's force drain at that site is ≥ 2; ability ≥ 4 (we can draw battle destiny); forfeit trade favorable-or-even (`our forfeit <= their forfeit × 1.25`, a one-sided cap). If all hold, return `min(300, drain × 100)`. Drain-1 sites fall through.
- **Key thresholds:** gap ≤ 3; drain ≥ 2; ability ≥ 4. Forfeit cap: ours ≤ theirs × 1.25. Bonus = `min(300, drain × 100)` → drain 2 = +200, drain 3+ = +300 (below the +400 clean win, above PASS).
- **Before → After:** Armed Maul, gap 3, drain 2, forfeit 7 vs 16 → commits +200 (was -2000, passed). A symmetric parity-window bug was caught in test and fixed to the one-sided cap.
- **Where:** `CharacterDeploySiteEvaluator.java` (shared, both bots), `computeTeamViability`.

## V182 — Draw / force management (offensive bank) — `0b5ad63d5` (2026-06-14)
- **Symptom:** Rando almost never left force in the pile, so "save force for a bigger army next turn" never actually happened.
- **Root cause:** The draw phase pays +80 per surplus card (cap +400) to draw the pile down, and the reserve target only counted DEFENSIVE needs (Draw Their Fire, First Strike, maintenance…). There was no offensive "I'm assembling an army" term, so saved force became hand cards.
- **Fix (Steve's bottleneck rule):** Before the draw-down bonus, `computeOffensiveBank` scans every site where the opponent out-powers us and is worth fighting (a battleground, or the opponent drains there). For each it covers the power gap with our strongest hand characters and sums their deploy cost. If the hand CAN cover the gap but we can't afford the deploy this turn AND could within ~2 turns of banking, it returns that army's cost. When the force pile is below that (and hand ≥ 4 so we don't starve), suppress the draw so PASS wins and the force stays in the pile. Self-resolving: once the pile reaches the cost, the army deploys and the trigger clears. If the hand does NOT have enough characters, it returns 0 and Rando draws normally.
- **Key thresholds:** Suppress draw = -300 (early return). Reachability = `forcePile + 2 × generation >= cost`. Hand guard = `handSize >= 4`.
- **Before → After:** Held force ("need 10, have 2") → accumulated → army deployed → battle. Fired 24x and resolved; 9 battles vs 3 before.
- **Where:** `DrawEvaluator.java` (rando + chosenone), `computeOffensiveBank`.
