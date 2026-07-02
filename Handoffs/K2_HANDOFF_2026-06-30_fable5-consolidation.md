# K-2 HANDOFF → Fable 5 — Rando consolidation + current state (2026-06-30)

You are **K-2** (now running Fable 5), Steve's AI on GEMP-SWCCG. Steve = Steven Davis, GEMP `asdf`, SWCCG expert, dyslexia + ADHD (keep replies SIMPLE, single-layer, tables over prose, no em-dashes inline). This file is your catch-up for the last week of work that is NOT yet in the older onboarding docs.

---

## 0. Read in this order before touching anything
1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — standing rules (`feedback_*`) + project state. Auto-loads.
2. The project `CLAUDE.md` (your K-2 persona/voice). (The old `fable5` discipline skill is retired — its habits are native to the Fable 5 model. Archived copy: `~/.claude/skills-archive/fable5/` if ever needed.)
3. `Handoffs/K2_MASTER_HANDOFF_2026-07-02.md` — THE master router for overall state (Master-K2 consolidation of all four sessions; the 07-01 router it replaced was K2-4's and is archived).
4. **This file** — the CONSOLIDATION workstream (K2-2). For the bug workstream read `K2_HANDOFF_2026-07-02_fable5-onboarding.md` instead.
5. Only if needed: `resources/k2-resources/distilled/00-START-HERE.md` (hub, current through ~V186 — now one week stale).

**Skills cleanup (2026-07-01):** active project skills are now only `work-verifier`, `k2-swccg-strategy`, `cube-builder`, `card-blueprint-db-manager`. The `karpathy-guidelines`, `gemp-swccg-memory` (stale, V24-era), and `skill-creator` skills are archived at `.claude/skills-archive/` — do NOT go looking for them; MEMORY.md + the handoffs replaced them.

The older `Handoffs/K2_*2026-06-23/24/25*.md` are history. Do not treat them as current.

---

## 1. Current state (verified 2026-06-30)
- **Branch:** `rando-consolidation-2026-06-23`. **HEAD:** `37c352d87`. **Local only — nothing pushed to GitHub** (Mac-only for now).
- **Committed this week (newest first):**
  - `37c352d87` V179 — A Good Friend / A Cunning Warrior: don't rank a held-location download above deploying it (**bug #4, other K-2**)
  - `f664cc2ba` V120 — weapon-pull block no longer mis-fires on a character pull (**Vader, bug #1, other K-2**)
  - `d72ced949` V188 — Set Your Course For Alderaan: no ability characters to Death Star sites (this K-2; deployed + verified live)
  - `8fd884375` V67h junk pass-through + V22 Silence Is Golden
  - `b89268344` auto-flip server operational on container boot
- **Uncommitted (the other K-2's in-flight work — DO NOT sweep into your commits):** `RandoCalAi.java`, `evaluators/ActionTextEvaluator.java`, `evaluators/BattleEvaluator.java`, `evaluators/ForceActivationEvaluator.java`, `evaluators/MoveEvaluator.java`, `src/.../prod-log4j.xml`.
- **Two K-2s share ONE working tree.** `git add <file>` stages the whole file. Commit only files you changed; name-and-leave the rest. (This already caused one bundling incident — commit `8fd884375` swept other-K-2 work.)

---

## 2. Your job: the Rando rule-consolidation (vetted, ready to execute)

**Goal (from Steve):** deep-dive Rando's logic structure, consolidate the ~100+ overlapping scoring rules, comment out (never delete) redundancies. Rando scoring is ADDITIVE — one decision gets hit by 4-6 rules that all reward the same fact (a reserve pull piles up to **+6250**). The plan collapses each pile into ONE scorer per decision, keeps every GUARD, uses the clean Filter/Category path instead of hand-synced keyword lists.

**Deliverables already produced:**
- **`resources/Rando_Consolidation_Plan_2026-06-29.xlsx`** — the plan: 8 moves, ordered, with the 3 council fixes baked in + a review note. THIS IS THE PLAN. Read it first (openpyxl or just open it).
- **`resources/k2-resources/originals/02-rando-history/Rando_AI_Rule_Audit.xlsx`** — the FOUNDATION: 205 rules catalogued (2026-05-22), 9 categories of audit findings (contradictions / dead rules / score inflation / hard-block stacking / detection-path mismatches / card-name hardcoding / side-symmetry / turn-window / overfitting). **It STOPS at V115** — code now runs through V188, so it under-covers.

**The 8 moves (see the Excel for detail), ordered best payoff-to-risk:**
| # | Consolidate | Risk | Note |
|---|---|---|---|
| Now | Fix lying V67al comments (DeployEvaluator:1839, 1889 say dead V67al is "active") | none | pending Steve's OK |
| Now | Delete 5 untracked junk files (see §6) | none | pending Steve's OK |
| 1 | Cache DTF + maintenance once (5 rules recompute) | LOW | ship first |
| 2 | One `canWinAt()` helper (3 copies) | LOW | ship first |
| 3 | One `fourthShieldBlocked()` helper (4 copies) | LOW | ship first |
| 4 | Comment out ~10 hardcoded dead-search checks (generic V177 gate covers) | LOW* | *KEEP V29.9/V29.11 (see traps) |
| 5 | Move-blocks → 5 fixed CLOBBER-not-add levels | MED | precedence table first |
| 6 | One weapon-holder gate (6 rules) | MED | **collides with V120 — see traps** |
| 7 | Fold deploy-occupancy into site scorer, cap +400 | MED | boundary table first |
| 8 | ONE reserve-pull scorer (+6250 pile → ~+2350) | HIGH | LAST, alone, 1 build |

**The 3 council traps you MUST guard (found by a code-cited adversarial review + deepseek, 2026-06-30):**
1. **Move 4:** `V29.9`/`V29.11` "already-in-hand" duplicate-guards are NESTED inside the V29.7 block. The generic gate does NOT do that dedup. Comment out only the dead-search `-400` lines; keep the duplicate-guards. (ActionTextEvaluator ~2028-2045.)
2. **Move 6:** `V120` got its Vader fix committed TODAY (`f664cc2ba`). Its bespoke last-word noun-match is the fix. A careless gate-merge resurrects the game-losing Vader-as-weapon bug. **Keep V120's detection; share only the wielder-COUNT math; keep the 3 separate gates** (V158 hand-deploy, V120 effect/interrupt, V67ar/ao/V149 reserve-pull, V185 oracle path — different action surfaces). **Coordinate with the other K-2 before touching V120.**
3. **Move 7:** the `+400` cap is NOT a no-op. A drain-emergency deploy: §A −2000 + V51 +600 = −1400 (blocked) today; capped → −1600. That's a magnitude change at the boundary. Build a per-rung table against §A's terminal magnitudes (CharacterDeploySiteEvaluator: −2000 @536, −1500 @398) first. The "3.3x route split" is a CONSISTENCY issue (different sequential decisions), not domination — lower urgency than first rated.

**The known headline trap (handled):** move 8's reserve pull in the ACTIVATE phase must out-score `V168` "+5000 always-activate" (`ActionTextEvaluator:186`). ACTIVATE phase has no step-buckets (global-max), so a naive +2350 pull LOSES to +5000 and V97 "pull-before-activate" goes missing. Size the ACTIVATE-phase pull above +5000; the DEPLOY-phase nudge (V100, competitor ~+500) can be small.

**Confidence:** ~85% clean if the 3 traps are guarded; ~45% if shipped literally. **Sequence:** dead-code/comments + moves 1-3 first (safe), regression-test, #8 last and alone, one cluster per build.

---

## 3. The other workstream — coordinate, don't collide
The other K-2 owns the 4 gameplay bugs (`resources/Rando_Issues_2026-06-29.xlsx`): #1 Vader (V120, DONE `f664cc2ba`), #4 A Good Friend (V179, DONE `37c352d87`), #2 weapon-before-character pull-first, #3 force/card hoarding. **Your Move 6 touches V120 — the exact rule they just fixed.** Sync before either of you edits `ActionTextEvaluator` V120.

---

## 4. Disciplines (non-negotiable — these have burned prior K-2s)
- **Additive domination:** a consolidation must NOT make an old rule "go missing." Do the boundary math at edge cases BEFORE writing code.
- **Comment out, don't delete** superseded code (`//` per line). Before calling a rule dead, grep its enclosing `if (...)` for `if (false /* SUPERSEDED */)`.
- **Breadcrumbs same session:** every fix = code comment + BOTH `resources/AI_CHANGELOG.md` and `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md` + commit msg.
- **Verify before "done":** after any reload-ai / evaluator edit, invoke the `work-verifier` skill (Agent tool) or an adversarial pass. Confirm live, don't claim it.
- **Search by CardCategory/Filter/Keyword/Persona** — never substring-match generic nouns. (Matching a specific card TITLE is OK.)
- **One change at a time.** Don't bundle. Ask before PROD `src/` edits Steve hasn't approved, `docker compose down -v`, DB/schema, anything irreversible.
- **A fix that ADJUSTS an existing V-tag updates it in place** — no new V-tag for an adjustment.

---

## 5. Deploy + verify (the mechanics)
- **Deploy = `bin/gemp reload-ai`**: in-container `mvn -pl gemp-swccg-async -am package -DskipTests` → `docker compose restart build` → flips switches operational. This rebuilds `web.jar` from the WHOLE working tree and restarts the JVM. `rebuild`/`rebuild-fast` rebuild but do NOT restart (trap).
- **"It compiled" ≠ "it's live."** Verify: python `zipfile` byte-search the class inside `src/gemp-swccg-async/target/web.jar` for your log string; confirm the JVM `etime` is small (restarted onto the new jar); `curl -s -o /dev/null -w "%{http_code}" http://localhost:17001/gemp-swccg/` = 200; `git log -1`.
- Container: `gemp_swccg_app_1`. It has `mvn` + bash. Host has no JRE — compile in-container.

---

## 6. Env gotchas learned this week (will save you hours)
- **Logs rotate + gzip.** Live decision log = `logs/gemp-swccg.log` (RollingFile, TRUNCATES on container restart). History is gzipped in `logs/2026-06/app-06-DD-2026-N.log.gz`. **macOS `zcat` is broken — use `gunzip -c file.gz > out`.** The LIGHT-Saga and Hunt-Down replays analyzed this week are in `app-06-29-2026-1.log.gz`.
- **The scratchpad gets cleaned between turns** — re-extract game slices from the archives each time; don't assume `scratchpad/*.log` persists.
- **The deploy "structure"** = the DPS hierarchy walk in `evaluators/CombinedEvaluator.java:97` (walks LOCATIONS → CHARACTERS → WEAPONS buckets, ordered by `strategy/DeployPhasePlanner.java`, first good bucket wins). ~100+ point-rules live OUTSIDE it and can override a step (that's the sprawl you're consolidating).
- **Council status:** local `deepseek-r1:70b` is UP — query direct at `http://127.0.0.1:11434/api/generate` (the FastAPI bridge on `:8000` is DOWN, only deepseek is loaded). **Alfred/Codex** (`mcp__codex__codex`, read-only sandbox, approval never) is a strong different-family voice but was **usage-capped until ~Jul 29 2026** — retry then.
- **5 untracked junk files to delete** (confirmed untracked, none compiled): `evaluators/CardSelectionEvaluator.java.bak`, `.v13.backup`, `.v24.11.fix`, `rando/game_log2.txt`, `rando/game_log_latest.txt`. They pollute greps. Under `src/` so get Steve's OK first.

---

## 7. Next actions, in order
1. Get Steve's OK on the two do-now items (§2): fix the V67al lying comments; delete the 5 junk files.
2. Ship the SAFE batch: moves 1, 2, 3 (pure dedup / extract-method, no magnitude merge). Reload-ai + verify each.
3. Sync with the other K-2 on V120 ownership before Move 6.
4. Then moves 4→5→6→7 one cluster per build, boundary table + regression check each. Move 8 LAST and alone.
5. Loop Alfred in after his reset for a 3rd-family check on move 8's boundary math.

---

## 8. The one thing most likely to be wrong
The consolidation plan is verified against current code for the load-bearing facts (V168 +5000, the reserve stack, the dead `if(false)` rules, the junk files, the V96 lying comments — all confirmed). What is NOT yet verified: the boundary math for moves 5 and 7 at every edge case, and whether move 8's ~15 "guards survive verbatim" each still early-return after the merge. Build the boundary table and re-verify per cluster; do not trust the plan's magnitudes without re-reading the code at the moment you touch each rule.
