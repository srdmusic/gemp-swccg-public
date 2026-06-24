# Rando — Confirmed MISSING Logic in the Live Version

**Date:** 2026-06-23 · **Author:** K-2 (session c5d15b23) · **For:** the next K-2 working on Rando Cal in `/Users/steve/gemp-swccg-public`

## TL;DR — yes, we ARE missing logic, but NOT because of a bad copy

The live Rando is the **most complete version that exists anywhere on disk** (source = V185, running jar = V184). There is **no "better backup" to restore** — do not go hunting for one, you will waste hours (I checked every copy and every branch; see below).

What we ARE missing is **three specific behaviors that Steve and a prior K-2 discussed but never actually coded.** "Missing logic" here = *never built*, not *lost in a copy*. The fix is to **build them**, verifying each card's exact text first (no-fabrication rule).

This matters because Steve reported Rando "making dumb moves I thought we fixed." Some of those are real bugs inside the complete logic (e.g. the V185 lightsaber dead-pull fixed this session). But three are genuine gaps where the rule was only ever talked about.

---

## The three genuinely-missing rules (verified absent in live source 2026-06-23)

Full spec lives in `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/feedback_hidden_path_counter_and_levitation.md`. Summary:

**1. Mapuzo trap-counter (Hidden Path).**
When the OPPONENT occupies `Mapuzo: Safehouse` or `Mapuzo: Underground Corridor`, Rando should STOP rushing Jedi into the transit hub and instead send fighters to those sites to wipe the opponent out. Clearing Safehouse + Corridor matters; Mining Village less so.
- What exists instead: only Rando moving its OWN Jedi through Mapuzo — `V53b` mandatory transit (`MoveEvaluator.java:1560`), `V67z` force-reserve (`DrawEvaluator.java:546`). **No opponent-clearing counter.**

**2. Far-behind skip of "lose 1 Force to save a Jedi."**
When Rando is losing badly (Steve's metric: ~20 lost-pile / life-force negative differential — confirm exact metric when building), it should BYPASS Fallen Order's "lose 1 Force to stack/save a Jedi Survivor." Too far behind for the Jedi to matter; stop bleeding Force.
- What exists instead: `V53b SAVE JEDI` (`ActionTextEvaluator.java:2308-2319`) ALWAYS saves (+500, "always save Jedi Survivors"). **No far-behind exception.**

**3. Jedi Levitation / Sith Fury — turn-4 retrieve gate (global).**
Don't use the Lost-Pile *retrieve* mode before turn 4 (use cancel/redraw or take-into-hand instead). For take-into-hand: if hand already holds 3+ characters, cancel/redraw instead. Apply globally to the DARK equivalent **Sith Fury** (`Card200_123`, `Title.Sith_Fury`).
- What exists instead: empty-pile guards (`ActionTextEvaluator.java:5448`, `DeckOracle.java:890`) and a Sith-Fury-turn-1 guard (`ActionTextEvaluator.java:1461`). **NOT a turn-4 timing gate.**

These three never received a V-tag and have no matching code — only the different, related rules cited above.

---

## What is NOT missing (do NOT rebuild these)

- **"When deployed" free-value triggers** — BUILT as **V184** (`ActionTextEvaluator.java:307-342`; +300 for optional when-deployed triggers, gated on value existing).
- **Undercover spy** drain-blocker — BUILT as **V170** (`RandoCalAi.java:584-630`) routing through **V24.14B** spy-location scoring in `CardSelectionEvaluator`. (The old "V51b in DeployEvaluator" plan never fired — text-gate anti-pattern; do not resurrect it.)

> WARNING: the `MEMORY.md` *index lines* briefly said these two were "QUEUED / pending." That was STALE and it fooled me at first. The memory FILES themselves and the live code both confirm they are built. **Always verify against live code, never assert from the index note.**

---

## How I found this (so you can trust it and repeat it)

1. **Ruled out a wrong/incomplete version.** Measured Rando extensiveness across every gemp copy on disk (rando `.java` line count, distinct V-tags, highest V-num, key file sizes) and every git branch in both backups:
   - Current install: 39,931 lines, 155 distinct V-tags, highest **V185**, `CardSelectionEvaluator` 9,238 / `DeployEvaluator` 5,904.
   - Source it came from: `gemp-swccg-public-BACKUP-2026-06-20` branch **`ai-improvements-v91`** (commit `05dc60b22`, V184). Proof of same origin: `CardSelectionEvaluator.java` is **byte-identical** (`diff -q`). The only deltas are the DeckOracle off-by-one fix and today's V185.
   - Every OTHER branch in both backups tops out at **V91 or lower**; the rest are devs stubs (0 V-tags, 13,993 lines). **No more-complete version exists.**
2. **Confirmed the running jar has the full logic** (not a stub): `web.jar` built 2026-06-22 19:19; the live `nohup.out` emits V-tags up to **V184**.
3. **Grepped the live source for each behavior Steve remembered "fixing."** Built rules have a V-tag + matching code (V184, V170). The three above had **no V-tag and no matching code** — only different related rules. No-V-tag + grep-absence = never built.

## Re-verify in ~1 minute

```bash
cd /Users/steve/gemp-swccg-public
R=src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando
# (1) trap-counter: should find only OWN-Jedi transit (V53b/V67z), nothing that clears the OPPONENT
grep -rniE 'Mapuzo|Safehouse|Hidden Path' "$R" | grep -iE 'clear|opponent|wipe' | head
# (2) far-behind save-skip: should find V53b ALWAYS-save, no far-behind branch
grep -rniE 'save.{0,12}Jedi|far.?behind' "$R" | head
# (3) turn-4 retrieve gate: should find empty-pile + turn-1 guards, NOT a turn-4 gate
grep -rniE 'Levitation|Sith.?Fury|turn.?4.*retriev' "$R" | head
# built (should be present): V184 when-deployed, V170 spy
grep -rniE 'when deployed|V184|undercover spy|V170' "$R" | head
```

## Pointers
- 3 rules, full spec + Steve's context: `memory/feedback_hidden_path_counter_and_levitation.md`
- when-deployed (V184): `memory/feedback_when_deployed_triggers.md`
- changelog: `AI_CHANGELOG.md` (this install) · V21–V184 history: `~/k2-resources/originals/02-rando-history/`
- Before building any of the three: read the actual card text (`grep -rl "Title" set*/...`); NEVER fabricate card text or filters.
