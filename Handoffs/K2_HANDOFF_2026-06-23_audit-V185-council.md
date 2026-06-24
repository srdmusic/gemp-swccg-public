# K-2 Handoff — 2026-06-23 (Audit + V185 + Council angle)

**Author:** K-2 session `c5d15b23` · **For:** the other two K-2s working this install · **Goal:** consolidate so we don't collide or duplicate.

My angle these past two days: the A Good Friend / Leia's Lightsaber "dumb move" → a full backup + breadcrumb audit → sorting and running the local council. Below is what I touched, what I found, and what's open.

---

## ⚠️ READ FIRST — coordination / hot files

Everything is **UNCOMMITTED on the `55c22cf49` working tree.** There are no commits, so git won't protect us from each other — we're all editing one dirty tree. **Coordinate before touching these hot files:**

| File | Who/what is in it | Conflict risk |
|---|---|---|
| `…/rando/evaluators/DeployEvaluator.java` | **My V185** (V67h `WILL_SUCCEED` branch, ~line 798) **+ another K2's V186** | HIGH |
| `…/rando/evaluators/CardSelectionEvaluator.java` | **V186** (`evaluateDeployLocation` ~813, `evaluateUnknown` ~7960) | HIGH |
| `…/rando/strategy/DeckOracle.java` (untracked) | **My V185** helpers (~line 402) | MED |
| `…/rando/evaluators/MoveEvaluator.java`, `ActionTextEvaluator.java` | top open fixes (V96/V67al, V53b/V60) live near here | MED |
| `AI_CHANGELOG.md` | shared log — **append, never overwrite** (V185 + V186 entries already coexist) | HIGH |

Other concurrent work I'm aware of (did **not** touch): **V186 "I Want That Map"** (another K2; `CardSelectionEvaluator` + `ObjectiveAnalyzer.parseFlipCondition`), and a brand-new **`…/ai/models/curator/`** model dir (a third angle).

---

## What I CHANGED (files + anchors)

1. **V185 — weapon-deployability gate** (the lightsaber fix):
   - `DeckOracle.java` ~402: added `reserveTargetsAreAllUnattachableWeapons(game, playerId, targets)` + private `hasInPlayCharacterAccepting(...)`. The crude first-pass (`hasCharacterInPlay` / `reserveTargetsAreAllWeapons`, "any character") is **commented out** right above per the comment-out-superseded rule.
   - `DeployEvaluator.java` ~798 (V67h `WILL_SUCCEED` branch): calls the new method; blocks the pull `-2000` when every Reserve target left is a weapon with no in-play character its OWN `getMatchingCharacterFilter()` accepts.
   - **Status: compiles clean; NOT deployed** (running jar = V184). **chosenone mirror pending.**
2. `AI_CHANGELOG.md` — added the V185 entry (2026-06-23).
3. `bridge/council.py` (in `~/Documents/Claude/Projects/LOCAL LLM MASTER AGENT`) — re-pointed all 5 roles to the only pulled model `deepseek-r1:70b-llama-distill-q8_0`. Restore multi-family with `pull_models.sh` then revert per-role tags (comment in file).
4. Memory dir (`~/.claude/projects/-Users-steve-gemp-swccg-public/memory/`) — added `feedback_weapon_needs_character_deploy`; **corrected stale "queued" notes** (when-deployed = BUILT V184, spy = BUILT V170).

## What I CREATED (read these)

- **`/Users/steve/k2-resources/`** — the **ONBOARDING ARCHIVE. I (this K-2 lineage) built it** on 2026-06-22, consolidating every scattered `.md` from `BACKUP-2026-06-20` into one place OUTSIDE any install (so re-clones can't wipe it). **Start at `k2-resources/distilled/00-START-HERE.md`** — the current hub (refreshed 2026-06-23; routes to all three handoffs). ⚠️ The *deep* distilled docs lag the code (`K2_ONBOARDING.md` ≤ V138, `ai-instructions.md` ≤ V126) — trust the **hub + the live install** for anything V185+. Verbatim sources live under `originals/` (the 5 handoffs, full `AI_CHANGELOG` 2336 L, `AI_VERSION_HISTORY` 5358 L, the 2 prior audit `.xlsx`, ~41 SWCCG rules docs).
- **`RANDO_BACKUP_AUDIT_2026-06-23.xlsx`** — 5 sheets. The **Breadcrumb Findings** tab has file:line evidence for every item below.
- **`RANDO_MISSING_LOGIC.md`** — the 3 never-coded rules + how I verified.
- This handoff.

## 📍 Where to look for stuff (orientation map)

| You need… | Look here |
|---|---|
| **Onboarding / orientation** | `k2-resources/distilled/00-START-HERE.md` (the hub I built); `MEMORY.md` auto-loads a pointer to it each session |
| **Standing rules + project state** | `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` (auto-loaded) — the `feedback_*` rules are non-negotiable |
| **The 3 K2 angles / current work** | the two handoffs in the install root (this one + `K2_HANDOFF_2026-06-23.md`). The **curator** angle has NO handoff yet — a gap to fill |
| **Live code changes (V185, V186, proxy, auto-pass)** | `AI_CHANGELOG.md` (each entry has Why + Revert) |
| **Full V-tag history V21–V184** | `k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md` (NOT present in the live install) |
| **SWCCG rules / glossary / expansions** | `k2-resources/originals/05-swccg-reference/` (~41 docs) |
| **The backup audit + every finding's file:line** | `RANDO_BACKUP_AUDIT_2026-06-23.xlsx` → Breadcrumb Findings tab |
| **The 3 never-coded rules** | `RANDO_MISSING_LOGIC.md` |
| **Prior-K2 hard-won lessons (silent regressions, dojo design)** | `BACKUP-2026-06-20/K2_MASTER_HANDOFF.md` (also in `k2-resources/originals/03-handoffs/`) |

---

## FINDINGS the others need (the consolidation core)

### A. Confirmed code gaps, verified against live code — PRIORITY ORDER
1. **V96 / V67al magnitude inversion (DOMINATION-RISK — TOP).** V96 "concentrate at contested sites" = flat **+500** (`DeployEvaluator:1832`); V67al "spread penalty" = **power-scaled** (`:3804`); they sum. At high stacked power V67al ≥ +500, so Rando declines to pile on. **This is the "spreads out instead of piling on" complaint.** Fix (council UNANIMOUS): gate V67al OFF when V96 fires (contested).
2. **3 dead V136 stubs** in `…/ai/models/common/strategy/CharacterDeploySiteEvaluator.java` (affect BOTH bots): `deckShipCount` passed literal `0` (`:653` ship-heavy override never fires), `perSiteEffectActive` literal `false` (`:468` per-site/TDIGWATT override dead), `isAboard` hardcoded `false` (`:137`, pilot-aboard body-count wrong). Low/med severity (each gates one override).
3. **V53b/V60 Hidden-Path precedence** (`MoveEvaluator:1560` +9999 vs `:1579` -9999, implicit order; risk: stuck Jedi). Lower likelihood, high blast.
4. **No dojo regression harness** (designed in `K2_MASTER_HANDOFF.md §6`, never built). The systemic guard against silent score-domination. Council (2/3) says build this **#2**, before more scoring tuning.

### B. The 3 never-coded rules (full spec in `RANDO_MISSING_LOGIC.md`)
Mapuzo **trap-counter** (clear opponent off Safehouse/Corridor) · **far-behind skip** of "lose 1 Force to save a Jedi" · Jedi Levitation / Sith Fury **turn-4 retrieve gate**. None are in any version — BUILD, not merge.

### C. Do NOT chase (disproved by adversarial cross-check)
V67y "duplicate of V29.8" (V153 superseded both) · V112+V51 "double -9999" (V51 is a positive bonus) · "V136 §D disabled in chosenone" (both bots pass the live flag). And BattleEvaluator "missing ability check" is resolved by V164a.

### D. Docs-only drifts (code correct, the backup `AI_VERSION_HISTORY.md` is stale)
V52 (SPEND FORCE +300 removed), V106 (re-enabled 2026-06-17, changelog says removed), V29.13 (ghost V21 ref).

---

## KEY FACTS everyone should know

- **Rando reads ZERO external files at runtime** — all knowledge is compiled into `.java`. No data/config files to merge or restore. (Verified: 0 `getResource`/`new File`/`Files.read` across the whole AI tree.)
- **Current install = the most complete version on disk** = `BACKUP-2026-06-20/ai-improvements-v91` (V184) + the DeckOracle off-by-one fix + V185(source). All V-tags V3–V185 present. **Nothing to merge from any backup.**
- **Running jar = V184, NOT V185** (`web.jar` built 2026-06-22 19:19). V185 + V186 are source-only until a rebuild.
- **The "dumb moves I thought we fixed" root cause is silent score-magnitude domination** (a present rule out-scored by a bigger newer rule), per `K2_MASTER_HANDOFF.md:28`. Not missing code. The dojo is the fix for the whole class.
- **Council:** only `deepseek-r1:70b-llama-distill-q8_0` is pulled; all roles re-pointed to it (perspective diversity via prompts only, not multi-family). Its review CONFIRMED the V96/V67al fix.
- This install **lacks `AI_VERSION_HISTORY.md`** (the 5,358-line V-tag bible); it lives in `BACKUP-2026-06-20/` and `~/k2-resources/`. Worth restoring.

---

## OPEN decisions (for Steve / whoever picks them up)

- [ ] Build **V96/V67al** first (gate V67al off when contested) — verified top finding, council's #1.
- [ ] Then **dojo harness** (council's lean, prevents the regression class) **or** the **3 V136 stubs** (quick, isolated)?
- [ ] **Deploy V185** (rebuild the jar so it goes live).
- [ ] **Mirror V185** into the `chosenone/` copy.
- [ ] **Restore `AI_VERSION_HISTORY.md`** into the install.
- [ ] Build the **3 never-coded rules** (RANDO_MISSING_LOGIC.md).

## Where the detail lives
`AI_CHANGELOG.md` (V185 + V186) · `RANDO_BACKUP_AUDIT_2026-06-23.xlsx` → **Breadcrumb Findings** tab (file:line for every finding) · `RANDO_MISSING_LOGIC.md` · `BACKUP-2026-06-20/` (`AI_VERSION_HISTORY.md`, `K2_MASTER_HANDOFF.md`, `Rando_AI_Rule_Audit.xlsx`) · memory dir.
