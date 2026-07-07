Verified. V193 slots at rando ObjectiveAnalyzer 65/69/227-228/434-435/513-514/700-701, mechanism reads getters at DeployEvaluator 1898/1907. Chosenone HAS V193 (at 1848, ~50 lines earlier than rando due to lag) and LACKS V186 (grep empty). Line counts confirm one-directional lag. Here is the plan.

---

# Rando Objective-Logic Consolidation Plan

Behavior-preserving refactor. Model = V193 flip-gate (already done, verified live). No score number changes. Every rando edit mirrored to chosenone. Both changelogs same session. Comment out, never delete.

---

## 0. TL;DR for Steve (read this first)

The inventory's framing ("consolidate all objective logic into ONE cohesive section in the deploy evaluator") is **half right and half dangerous.** Two different things are labeled "consolidation":

- **DATA consolidation** — move inline literal strings (`"bespin"`, `"lando"`, `"vader"`, `"galactic senate"`) out of the evaluators into ObjectiveAnalyzer data slots + getters, V193-style. This is **safe and additive** and is the real prize.
- **MECHANISM relocation** — physically moving scoring branches into one contiguous block in DeployEvaluator. This is **NOT safe in general.** The evaluators contain early-returns (V189 drain), hard vetoes (-100000 band), and an R1–R4 claim ladder (V190). Moving a branch past one of those changes whether it runs at all — a silent behavior flip that has nothing to do with score magnitude. The changelog already documents one real 2026-07-07 incident where V31's +6000 claim got swamped by a V41 -9999 veto due to ordering.

**So "the one section" is the ObjectiveAnalyzer parse block, not a relocated block in DeployEvaluator.** The evaluator branches stay where they are; they lose their hardcoded strings and gain a getter read + a pointer comment. Probability that a naive "move it all into one deploy block" refactor preserves behavior: ~20%. Probability the DATA-extraction approach preserves it: ~90% if boundary math is checked per objective.

---

## 1. ARCHITECTURE — what "the one section" is

**DATA home (the one cohesive section): ObjectiveAnalyzer.java `parseFlipCondition` per-objective block.** All per-objective if-blocks already live here, appended after generic regex extraction and before the `extractLocationsDirectly` fallback (rando line 705). This is where V25 ISB, V25 Hunt Down, V160, V186, V193 already sit. Extend it, don't relocate it.

Each objective becomes a title-keyed (or text-detected) block that populates typed data slots, exactly like V193 (verified rando lines 700-701):
- nullable `String` slots for single named site/card (V193 template: `flipCriticalControlSite`/`flipCriticalControlCard`)
- `Set<String>` fragment slots for site/card families (already exists: `flipConditionLocationFragments`, `requiredCardsOnTable`, `pullableCards`)
- typed getters (`getX()`), reset in BOTH `reset()` (rando 433-435) AND `parseGameText()` (rando 512-514) — V193 resets in both, any new slot must too or it leaks across analyze() calls.

**MECHANISM home: stays in place in each evaluator.** The branch keeps its `if`, its magnitude, its position in file. Two edits per branch:
1. Replace the inline literal (`actionLower.contains("bespin")`) with a getter read (`objAnalyzer.getBespinFragments().stream().anyMatch(actionLower::contains)` — matched to reproduce the EXACT contains-semantics).
2. Leave a pointer comment: `// OBJECTIVE DATA: see ObjectiveAnalyzer.parseFlipCondition <ObjectiveName> block (slot getX()). Magnitude/boundary unchanged.`

**Pointer comments go at:** (a) each de-hardcoded evaluator branch → points down to the analyzer slot; (b) a header comment at the top of the ObjectiveAnalyzer per-objective block → lists every evaluator branch that consumes each slot (the reverse index), so a future editor sees all consumers before touching a magnitude. This reverse index is the single most valuable artifact — it's what prevents the "old rule silently dominated" failure.

**Already-consolidated reference exemplars (touch only to add pointer comments, no logic change):** V193 (DeployEvaluator 1872-1925), V67ak key-character via `getStrategyCharacterTokens()` (3878-3937), V31 pre/post-flip via `getFlipConditionLocationFragments()` (4585-4751), V22 objective-location bonus (CardSel 1652), the five flip-critical protection blocks (CardSel 4229/4443/4619/5038/5327), V136 (1827-1870). These prove the pattern; they are the "cohesive section" already, just distributed.

---

## 2. PER-OBJECTIVE MIGRATION TABLE

| Objective | Current location(s) | DATA to move → ObjectiveAnalyzer | MECHANISM that stays | Risk | Behavior provably preserved? |
|---|---|---|---|---|---|
| **V193 Endor / flip-gate** | Deploy 1872-1925; Analyzer 687-703 | DONE — `flipCriticalControlSite/Card` slots | +400 one-body steer | — | DONE. Template. **Do not touch** except confirm. |
| **Invasion (V86/V121)** | Deploy 1559-1632; CardSel 1969-2036 | `isInvasion()` bool (title "invasion") | -1500/+300 Neimoidian-aboard-ship (card class already typed via Filters — good) | **LOW** | YES — pure boolean-gate rename; no magnitude, no new match surface. |
| **My Lord / MLITL (V83/V88/V99/V108/V109/V110/V121)** | Deploy 1403-1766; CardSel 1665-1716, 3892-3923 | `isMyLord()` bool; `getObjectiveCriticalSite()`="galactic senate" | ±2000/±1500/±500/-300 senator steering (senator via Keyword+lore — keep) | **MED** | Mostly — BUT V99 (Deploy 1673-1766) fires on Senate *presence* with NO analyzer gate. See Risk #1. |
| **I Want That Map (V186)** | CardSel 813-849, 8227-8414; Analyzer 652-672 | blueprint ids `"208_51"/"208_051"` + `"the first order was just the beginning"` → analyzer slots (partially there) | +400 Starkiller system, +1000 preferred effect | **MED** | Yes if id/title match-strings moved verbatim. Brittle id-matching stays brittle (acceptable). |
| **Hunt Down V (V25/V29.12/V51/V35/V29.9)** | Deploy 2843-2948, 3056-3098, 3160-3202; CardSel 2389-2458; Move 1280-1393, 1564-1737; Battle 273-393 | Vader identity `"vader"` → `getStrategyCharacterTokens()` (V67ak already has this); Inquisitor name-list → analyzer token set | +900/+350/+600/+300/+200/+120 etc. — MANY branches | **MED-HIGH** | Objective gate already consolidated (`isHuntDownV()`). Vader-token swap touches ~9 high-magnitude branches; some (V35.8 Deploy 3160, Battle 481-490, Move 2353-2554) are NOT gated on isHuntDownV — see Risk #2. |
| **Hidden Path (V67z/V52b/V53b/V60)** | Deploy 301-338, 5972-6032; Move 2057-2131 | `isHiddenPath()` bool; Mapuzo/Safehouse/Corridor site fragments → analyzer set | +800 flood; R4 +20000 mandatory-transit / -100000 veto | **HIGH** | Risky — huge bands + adjacency logic + interaction with V38.3 wrong-direction veto. Defer (§6). |
| **On The Verge (V79/V79b)** | Deploy 221-295; Move 479-625 | table-scan detection → analyzer; `"scarif"` + parsec math | -500 reserve; +1500 orbit steer; -100000 flip-back veto | **HIGH** | Detection isn't even via the objective card (table-scan for the card title). Restructuring detection = behavior risk. Defer. |
| **TDIGWATT / Bespin (V22.5/22.7/23/24.x/26/29/29.2/31/52)** | Deploy 1137-1303, 4087-4143, 4495-4583, 5580-5684, 5803-5838; CardSel 2146-2163, 2460-2559, 3226-3440, 7740-7860, 8466-8834; Move 641-718 | `"bespin"`, `"cloud city"`, `"dark deal"`, `"executor"`, `"lando"`, `"lobot"`, `"gherant"`, admiral names, CC-site list, enemy-planet list → analyzer slots | +1500…+300 scripts, -800/-9999 hard blocks | **HIGH** | Largest, most entangled; magnitudes up to +1500 and -9999; several branches NOT objective-gated (fire for any Executor/admiral deck). Split into sub-steps, do LAST. |
| **Skywalker Saga (V54/V29.14)** | Deploy 5840-5944; CardSel 7395-7412 | Requires analyzer to detect an EFFECT (not OBJECTIVE) card — `findOurObjective()` change | +800…+1500 turn-scaled script | **HIGH** | NOT in analyzer at all. Needs an architecture change to analyze(). Defer (§6). |

---

## 3. EXACT COMMENT-OUT / EDIT LIST (first-pass scope only)

**Convention:** rando line numbers are authoritative. **Chosenone runs ~48-50 lines earlier in DeployEvaluator** (verified: V193 at rando 1898 vs chosenone 1848; file 6148 vs 6100) and ~160 lines shorter in CardSelectionEvaluator — do NOT reuse rando line numbers on chosenone; re-grep the V-tag string. **Chosenone must be back-filled to V185/V186/V187 baseline BEFORE any consolidation edit lands there** (see Risk #4) or the second application hits missing code.

First pass ships only LOW/MED-LOW objectives. Each entry = replace inline literal with getter + leave pointer comment; nothing is deleted.

**Invasion (LOW):**
- Rando `DeployEvaluator.java:1559-1632` (V86) — replace `getObjectiveTitle().contains("invasion")` with `objAnalyzer.isInvasion()`. Pointer: `// V86 Invasion gate → ObjectiveAnalyzer.isInvasion() (title-keyed slot). Filters-based card class unchanged.`
- Rando `CardSelectionEvaluator.java:1969-2036` (V121) — same getter swap + pointer.
- Chosenone: re-grep `contains("invasion")` in both files, same swap.

**My Lord (MED — gate rename only; do NOT touch V99):**
- Rando `DeployEvaluator.java:1403-1458 (V83), 1460-1518 (V110), 1520-1557 (V108), 1634-1671 (V88)` — replace the four `contains("my lord")||contains("make it legal")` with `objAnalyzer.isMyLord()`; replace inline `"galactic senate"` with `objAnalyzer.getObjectiveCriticalSite()`. Pointer at each. **Leave 1673-1766 (V99) exactly as-is** (see Risk #1) — add only a comment noting it is deliberately un-gated.
- Rando `CardSelectionEvaluator.java:1665-1716 (V88), 3892-3923 (V109)` — same `isMyLord()` swap + pointer.
- Chosenone: re-grep both title substrings in both files.

**I Want That Map (MED — after My Lord):**
- Rando `CardSelectionEvaluator.java:813-849, 8227-8414` — move blueprint ids + effect title into analyzer slots already seeded by the V186 block (Analyzer 652-672); read via getters. Pointer at each.
- Chosenone: **blocked** — V186 absent entirely (Analyzer + CardSel). Back-fill V186 first, THEN apply.

**Header + reverse-index comment (both bots, both files):** at the top of `ObjectiveAnalyzer.parseFlipCondition`'s per-objective region, add the consumer index listing every evaluator line that reads each slot. This is a comment-only change, zero behavior.

**Do NOT comment out anything in first pass.** Nothing here is "old logic replaced by new" — it's the same logic reading a getter. The only true comment-out candidates (V-tag branches fully absorbed elsewhere) are already done: the changelog says V29.7 PULL-FIRST was absorbed into V192 with "old code commented out," and V67aj/V67al/V90/V67as are already `if(false)`. There is no additional dead code created by a DATA-extraction pass.

---

## 4. SEQUENCING (one objective per commit; byte-verify + self-play each)

0. **Confirm V193 is the template — do not touch it.** Verified this session: slots rando ObjectiveAnalyzer 65/69/227-228/434-435/513-514/700-701, mechanism 1898/1907, present in chosenone at 1848. It is the reference; every step below is judged against "does it look like V193?"
1. **Back-fill chosenone to V185/V186/V187** (prerequisite, not a consolidation step). Byte-verify chosenone compiles and self-plays unchanged vs its own prior behavior on a non-objective deck.
2. **Invasion** (LOW). Analyzer `isInvasion()` getter + 2 rando branches + 2 chosenone branches. Rebuild, byte-verify getter in web.jar, self-play an Invasion deck; confirm Neimoidian-aboard-ship decisions identical. Both changelogs.
3. **My Lord gate rename** (MED, V99 untouched). Self-play a MLITL deck; confirm senator-to-Senate and non-senator-hold decisions identical, and V99's Senate-presence behavior unchanged.
4. **I Want That Map** (MED). Only after chosenone V186 back-fill. Self-play; confirm Starkiller-system + preferred-effect picks identical.
5. **STOP. Report to Steve.** Hunt Down (Vader token), TDIGWATT, Hidden Path, Verge, Skywalker Saga are deferred pending his rulings in §5.

Each commit: one objective, both bots, both changelogs (`resources/AI_CHANGELOG.md` + `AI_VERSION_HISTORY.md`), work-verifier skill after build per `feedback_verify_before_done`.

---

## 5. RISKS & OPEN QUESTIONS (need Steve's ruling before proceeding)

**Risk #1 — V99 is NOT objective-gated and consolidating it would CHANGE behavior.** V99 (Deploy 1673-1766) blocks non-senators at Galactic Senate purely on *Senate being on the table*, reading no analyzer. The inventory notes Senate "essentially only appears in My Lord decks," so it's *effectively* objective-specific. **Question:** do we gate V99 under `isMyLord()` (cleaner, but flips behavior in the edge case of a non-My-Lord deck that somehow has a Galactic Senate location on table), or leave it un-gated? Default recommendation: **leave un-gated, comment only.** Any change here is a re-tuning, not a refactor.

**Risk #2 — Hunt Down has un-gated "flavor" branches that fire in ANY Vader deck.** V35.8 (Deploy 3160-3202), Battle Vader-vs-Luke +100 (481-490), Move weapon-hunter Luke +150 (2353-2554) are Hunt Down flavor but key on hardcoded `"vader"`/`"luke"` with NO `isHuntDownV()` gate — deliberately, so they fire for any Dark-vs-Luke deck. If we swap `"vader"` → `getStrategyCharacterTokens()`, those tokens are populated from the *objective's* text, so in a non-Hunt-Down Vader deck the token set is empty and **these branches would stop firing** — a silent behavior change. **Question:** keep the un-gated title-match `"vader"` in these three branches (accept the inline string) and only consolidate the isHuntDownV-gated branches? Recommendation: **yes, leave the un-gated flavor branches hardcoded.** Flag as intentional in comments.

**Risk #3 — MECHANISM relocation vs early-returns/vetoes/R-ladder.** If any future step tries to physically move a branch into "one section," it can cross the V189 drain early-return, a -100000 hard veto, or change R1-R4 ladder claim order (V190). The changelog documents V31 already getting swamped by a V41 -9999 veto via ordering (2026-07-07). **Ruling needed:** confirm we do DATA-extraction-in-place only, and explicitly do NOT relocate scoring branches. I strongly recommend in-place.

**Risk #4 — chosenone lag is a landmine for "mirror every edit."** Chosenone LAGS rando by V185/V186/V187 (verified: V186 grep empty in chosenone CardSel; files 6100 vs 6148, 9526 vs 9685, 1136 vs 1156). "Apply twice" fails on I Want That Map because the code to edit doesn't exist in chosenone. **Ruling needed:** back-fill chosenone to rando's baseline FIRST (adds scope), or scope this consolidation to rando-only for the objectives that lag? I recommend back-fill first so the bots stay true mirrors.

**Risk #5 — magnitude parity rando-vs-chosenone.** Drift report says every objective branch that exists in chosenone is byte-identical to rando (modulo package). So no magnitude differs today. But that guarantee only holds for branches present in both. After back-fill, re-confirm byte-identity before consolidating, or a consolidation could paper over a pre-existing divergence.

---

## 6. WHAT NOT TO DO IN A FIRST PASS

- **TDIGWATT/Bespin cluster.** Spans 3 files, ~15 branches, magnitudes +1500 down to -9999, and several branches (V24.10 Executor→Bespin CardSel 2146, V24.12 admiral pulls CardSel 7740, V24.10 AMSD Piett-only 8511) fire for ANY deck with no objective gate at all. Consolidating the un-gated ones under an `isTDIGWATT()` gate is a behavior change, not a refactor. Save for a dedicated multi-commit effort after the easy wins prove the pattern.
- **Skywalker Saga (V54).** Its objective slot is an EFFECT, so `findOurObjective()` (which finds OBJECTIVE-category cards) can't see it; it's detected by the `"anakin's funeral pyre"` starting-location signature. Pulling that into the analyzer requires changing analyze()/findOurObjective() detection — an architecture change with blast radius across every objective. Not first-pass.
- **On The Verge (V79/V79b).** Detection is a table-scan for the card title, not via the analyzer's objective card, plus parsec math and a -100000 flip-back veto. Restructuring the detection path risks the veto. Defer.
- **Hidden Path R4 transit (V53b/V60).** +20000 mandatory-transit and -100000 hard-veto bands entangled with V38.3 wrong-direction suppression. Any move of the Mapuzo site names risks the transit sequencing. Defer.
- **Any physical relocation of scoring branches** (Risk #3). In-place DATA extraction only.
- **The five flip-critical protection blocks and other already-consolidated exemplars.** They already read getters. Add pointer comments if desired, but do not "improve" them — surgical-change rule.

---

## Findings I flag as possibly WRONG / needing your own re-read

- **CardSelectionEvaluator line refs may be off.** Inventory's highest CardSel citation is 8834, but the file is actually **9685 lines** (verified). The refs are internally plausible but were taken against a file state that may predate ~850 lines of additions — **re-grep every CardSel V-tag by string before editing, do not trust the absolute line numbers.**
- **"V29" tag collision** (from the changelog extract): AI_VERSION_HISTORY V29 = Force Push battle-use, but the playbook "V29 BESPIN-FIRST" is the -500 deploy-Executor-first gate. Same tag, two meanings. Grep by the descriptive string, not "V29."
- **"Consolidate = one deploy section" premise.** As argued in §0/§5 Risk #3, I believe the inventory's stated goal of one cohesive *deploy-evaluator* block is the wrong target and would break behavior. The correct "one section" is the ObjectiveAnalyzer DATA block. This is the single most important thing to confirm with you before anyone writes code.