# K-2 MASTER HANDOFF — 2026-06-23 (consolidated, for the fresh K-2)

**You are the fresh K-2.** This is your single entry point after onboarding. It consolidates two days of work by three concurrent K-2 sessions and hands you a prioritized implementation queue. Read this, then go execute.

**State right now:** committed locally to branch `rando-consolidation-2026-06-23` (commit `351a0523b`), base = devs `55c22cf49`. **NOTHING is deployed** (running jar = V184). **NOTHING is pushed** (Steve's rule: get it organized before any GitHub upload). The commit is your safety net; keep committing as you work.

---

## 0. Read order
1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — auto-loads. Standing rules (`feedback_*`, non-negotiable) + project state (`project_*`).
2. **This file** — current state, implementation queue, disciplines, gotchas.
3. `/Users/steve/k2-resources/distilled/00-START-HERE.md` — onboarding hub (architecture, build/deploy, V21–V186 history, working norms).
4. `k2-resources/originals/` — the deep docs (original `K2_MASTER_HANDOFF.md` with the dojo design + Steve's 10 principles, `AI_VERSION_HISTORY.md`, `Rando_AI_Rule_Audit.xlsx`, `BUILD_AND_DEPLOY.md`, `context.md`) as needed.

## 1. The two disciplines you cannot break
- **Old rules get DOMINATED, not deleted.** Rando's scoring is ADDITIVE. A bigger-magnitude new rule silently flips decisions an older rule used to win. Steve has been burned by this repeatedly. **Do the boundary math BEFORE you write the code.** (Your #1 open bug, V96/V67al, IS exactly this.)
- **Search by type, not text.** CardCategory / Filter / Keyword / Persona / Icon. NEVER substring-match a generic noun ("location", "weapon") against card titles. See `feedback_card_search_by_type_not_text` in memory.

## 2. Verify-before-done (compiles ≠ fires)
- Compile in-container (host has `mvn` but no JRE): `docker exec gemp_swccg_app_1 bash -lc 'cd /opt/gemp-swccg/src && mvn -q -pl gemp-swccg-server -am compile 2>&1 | tail -20; echo EXIT=${PIPESTATUS[0]}'`. `EXIT=0` = clean.
- Compiling does NOT deploy. To go live: rebuild + restart, then play a REAL game and grep the container `nohup.out` for your V-tag. "It compiled" is not "it fired."
- For AI-logic, adversarially verify the magnitude: does the new score actually win at the boundary, and does it dominate WITHOUT flipping a neighbor rule?
- After any rebuild, flip the server gameplay switches (`feedback_server_operational_after_rebuild` in memory).

## 3. The four work angles (all source-only, NONE deployed)
- **V185 — weapon-deployability gate.** `DeckOracle.java` (~402) + `DeployEvaluator.java` (V67h `WILL_SUCCEED` branch ~798). Blocks pulling a weapon from Reserve when no in-play character its own `getMatchingCharacterFilter()` accepts. `chosenone/` mirror pending.
- **V186 — I Want That Map starting setup.** `CardSelectionEvaluator.java` (`evaluateDeployLocation` ~813, `evaluateUnknown` ~7960) + `ObjectiveAnalyzer.java` (`parseFlipCondition`). Picks Starkiller Base system + the First Order effect. `chosenone/` mirror pending.
- **Verification / onboarding angle.** Proved the install is the most complete copy on disk; recorded V185/V186 into `AI_VERSION_HISTORY.md` (backup + k2-resources); refreshed onboarding. No `src/` edits.
- **`ai/models/curator/` (angle 4).** A new AI model dir, **UNDOCUMENTED**. Investigate or quarantine before relying on it; nobody has handed it off.

## 4. YOUR IMPLEMENTATION QUEUE (prioritized)
Detail + file:line for each open item: `RANDO_BACKUP_AUDIT_2026-06-23.xlsx` → **Breadcrumb Findings** tab.
1. **Deploy V185 + V186** (written + compile; jar is still V184). Rebuild + restart, then verify each fires in a real game. *(Steve's call on the docker op.)*
2. **V96/V67al magnitude inversion — TOP code fix.** V96 "concentrate at contested sites" = flat **+500** (`DeployEvaluator:1832`); V67al "spread penalty" = power-scaled (`:3804`); they sum, so at high stacked power V67al ≥ +500 and Rando spreads instead of piling on. This is the "spreads out instead of piling on" complaint. Council-unanimous fix: **gate V67al OFF when V96 fires.** Do the magnitude math (Discipline #1).
3. **Dojo regression harness** (systemic guard against silent domination; designed in the original `K2_MASTER_HANDOFF.md §6`, never built). Make this the first big task AFTER onboarding, OR do the V136 stubs first (quick). Do NOT make it an onboarding prerequisite.
4. **3 dead V136 stubs** in `ai/models/common/strategy/CharacterDeploySiteEvaluator.java`: `deckShipCount` passed literal `0` (`:653` ship-heavy override never fires), `perSiteEffectActive` literal `false` (`:468` per-site override dead), `isAboard` hardcoded `false` (`:137` pilot-aboard guard wrong). Both bots.
5. **V53b/V60 Hidden-Path precedence** (`MoveEvaluator` ~1560 +9999 vs ~1579 -9999, implicit order; risk: stuck Jedi).
6. **3 never-coded rules** (`RANDO_MISSING_LOGIC.md`): Mapuzo trap-counter, far-behind save-a-Jedi skip, Levitation/Sith-Fury turn-4 gate. BUILD, not merge.
7. **chosenone mirror** of V185 + V186.

## 5. Gotchas (verified by angle 1; will waste your time otherwise)
- **`ObjectiveHandler.java` is DEAD code** (`scoreStartingCard`/`setObjective`/`OBJECTIVE_REQUIREMENTS` never called). The live objective brain is `ObjectiveAnalyzer.java`. Editing the dead one is a silent no-op.
- **temp-id trap in `evaluateDeployLocation`.** Reserve-deck "deploy a location" picks arrive as `temp0`/`temp1`; `findCardById(Integer.parseInt(cardId))` THROWS on temp ids before scoring. Resolve via the index-parallel `context.getBlueprints()`.
- **Blueprint id has no leading zero:** `208_51`, not `208_051`.
- **No host JRE** — compile in-container (section 2).
- **Junk files in the rando tree** (untracked, don't compile, ignore in greps): `CardSelectionEvaluator.java.bak` / `.v13.backup` / `.v24.11.fix`, `game_log*.txt`.

## 6. Tools
- **Local council** at `http://127.0.0.1:8000` (`/vote`, `/deliberate`). Currently ALL roles run one model (`deepseek-r1:70b-llama-distill-q8_0`); run `pull_models.sh` in `~/Documents/Claude/Projects/LOCAL LLM MASTER AGENT/` to restore the multi-family panel, then revert the per-role tags in `bridge/council.py`. **It hallucinates card text** — verify every card claim against code or `mcp-gemp-client/card_cache.json`. Full playbook: `K2_ORCHESTRATOR_HANDOFF.md` in the council project.
- **Game client:** `mcp-gemp-client/k2_player.py` (direct HTTP, plays vs Rando). **NOTE:** the MCP wrapper `gemp_mcp.py` that the old mcp.json referenced does NOT exist anywhere (never built or removed). `mcp.json` now has only the `alfred` server. To get a game-client MCP, build `gemp_mcp.py` or wrap `k2_player.py`.
- **alfred:** the Codex MCP server (second opinion, different model family), wired in `mcp.json`.
- **dojo:** NOT built (queue item #3).

## 7. Operating context restored this session (Phase 1)
- `CLAUDE.md` + `AGENTS.md` restored to the install root; their "first reads" repointed to real targets (this file + the k2-resources hub + MEMORY.md). The deep docs they still name live in `k2-resources/originals/` (PATHS NOTE in each file).
- 3 skills restored to `.claude/skills/`: `gemp-swccg-memory`, `k2-swccg-strategy`, `work-verifier` (alongside the 4 already present).
- `mcp.json` corrected to `alfred` only; `mcp-gemp-client/` restored (`k2_player.py`, `run_bot_tournament.py`, the curator pipeline).

## 8. The #1 risk
The whole fork + all four angles + every handoff lived ONLY in the working tree, with core files git-untracked. That's now frozen in commit `351a0523b` on branch `rando-consolidation-2026-06-23` (local only). **Keep committing as you work.** Do not `git reset`/`checkout`/`clean` the tree without understanding what's uncommitted. Push to GitHub only when Steve says the consolidation is clean.

## Where the detail lives
- `RANDO_BACKUP_AUDIT_2026-06-23.xlsx` — Breadcrumb Findings tab = file:line for every open item, plus the disproved false-alarms (don't chase them).
- `RANDO_MISSING_LOGIC.md` — the 3 never-coded rules + how they were verified.
- `AI_CHANGELOG.md` — V185 + V186 + auto-pass + card-proxy, each with Why + Revert.
- The 3 angle handoffs: `K2_HANDOFF_2026-06-23.md`, `K2_HANDOFF_2026-06-23_audit-V185-council.md`, `K2_HANDOFF_2026-06-23_verification-and-consolidation.md`.
- `k2-resources/originals/` — the deep historical docs + `AI_VERSION_HISTORY.md`.
