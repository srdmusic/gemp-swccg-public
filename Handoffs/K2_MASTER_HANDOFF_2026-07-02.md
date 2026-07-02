# K-2 MASTER HANDOFF — 2026-07-02 — THE entry point for a fresh Fable 5 session

Written late 2026-07-01 by the Master K-2 that consolidated the four session handoffs (K2-1 through K2-4). Dated 07-02 to avoid colliding with the router it replaces. This file REPLACES `K2_MASTER_HANDOFF_2026-07-01.md` and every older master. It is a ROUTER: verified state + work queues + where truth lives. It does not duplicate the changelogs.

You are **K-2** (Claude, Fable 5), Steve's AI on GEMP-SWCCG. Steve = Steven Richard Davis, GEMP `asdf`, SWCCG expert, ADHD + dyslexia: concise, single-layer bullets, tables over prose, no em-dashes in inline prose. Persona + comm rules: `.claude/CLAUDE.md`.

---

## Read order (non-negotiable)

1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — auto-loads. The `feedback_*` entries are LAW: one-change-at-a-time, breadcrumbs-every-fix, check-rule-is-live-before-editing, update-old-rule-not-new-version, card-search-by-type-not-text, verify-before-done.
2. THIS FILE.
3. The handoff for YOUR workstream (§3 below), and only that one.
4. `resources/BUILD_AND_DEPLOY.md` — before any edit (§1: is the rule live?) and any deploy (§2 + §3).
5. Only if needed: `resources/k2-resources/distilled/00-START-HERE.md` (project hub, stale past ~V186).

The one discipline: **Rando scoring is ADDITIVE. Old rules get DOMINATED, not deleted. Do the boundary math BEFORE writing code.** Four incidents so far. Corollary from the V61c bug: when a behavior is enforced at multiple code sites (ForceActivationEvaluator amount cap + ActionTextEvaluator V168 carve-out + V38.3 confirm), all sites must share ONE predicate or they undo each other.

## 1. Current state (machine-verified 2026-07-01: K2-4's 5-agent audit + Master re-check)

- Branch `rando-consolidation-2026-06-23`, HEAD `37c352d87`, base devs `55c22cf49`. **Local only. NOTHING pushed to GitHub (Steve's standing order).**
- Committed + live: V188 (Alderaan Death-Star block, `d72ced949`), V120 fix (Vader pull, `f664cc2ba`), V179 fix (held-location download, `37c352d87`), V67h junk pass-through + V22 Silence Is Golden (`8fd884375`), V187 (swept into `8fd884375`), V156 smart-solo update (swept into `d72ced949`), Fix #2 §A contested gate (`4f8ec16d3`), cancel-loop fix (`d1e2aa890`), auto-flip on boot (`b89268344`).
- **UNCOMMITTED but live in the running jar** — 4 distinct code changes across 6 files (all K2-3's finished force-management work):
  - V61c destiny buffer (keep 3 in reserve, both halves) — `rando/evaluators/ForceActivationEvaluator.java` + `rando/evaluators/ActionTextEvaluator.java`
  - V61b overpower battle (fight with empty reserve when overpowering by ≥8) — `rando/evaluators/BattleEvaluator.java`
  - V79b Death Star parsec steering (Verge → Scarif) — `rando/RandoCalAi.java` + `rando/evaluators/MoveEvaluator.java` (MoveEvaluator half is marked INERT, kept as robustness)
  - log4j mainlog appender (decision log survives restarts) — `async/.../prod-log4j.xml`
- Also uncommitted: the changelog backfills + doc-audit patches + this consolidation (docs), and 11 `.claude/skills/` deletions (all byte-identical in untracked `.claude/skills-archive/` — archive move, not loss).
- Every shipped fix has entries in BOTH changelogs as of 2026-07-01. `git status` + `git diff` remain the source of truth for in-flight work — check before editing anything.
- Commit plan (waiting on Steve): FOUR separate code commits (V61c / V61b / V79b+MoveEvaluator / log4j) + docs as a fifth. `git add` stages whole files; two prior commits accidentally swept cross-session work — stage only what YOU changed.

## 2. Who wrote what (labels corrected by K2-4; older files in `Handoffs/archive/`)

| Session | Work |
|---|---|
| K2-1 | Rule-consolidation workstream (`K2_HANDOFF_2026-06-30_fable5-consolidation.md`), V188, skills cleanup |
| K2-2 | Bug workstream (`K2_HANDOFF_2026-07-02_fable5-onboarding.md`): TDIGWATT A+B diagnosis, V120 + V179 fixes |
| K2-3 | Force-management fixes (V61b, V61c, V79b, V79 rider, log4j, V187), doc audit, staleness banners |
| K2-4 | The 07-01 router, changelog backfills, Codex investigation, ground-truth audit |
| Codex | `.agents/` + `AGENTS.md` + the two `resources/Rando_*_2026-06-29.xlsx` (sandbox — see §6) |

## 3. The two work queues — pick yours, don't collide

- **A. Bug fixes / gameplay** → `Handoffs/K2_HANDOFF_2026-07-02_fable5-onboarding.md`. Queued in order: TDIGWATT Bug A (V177 dead-search false negative starving I'm Sorry's interior Cloud City site downloads — fix by running V67h's V82.1 category fallback before declaring DEAD), then Bug B (V29 BESPIN-FIRST demanding an Executor the objective forbids). Both adjust old V-tags in place, mirror to chosenone. THEN the V61c battle-intent refinement (Steve-approved plan in that same file, § "ALSO QUEUED": keep 3 only on turns Rando might battle; activate ALL on deploy-and-end turns; one shared predicate for all three carve-out sites).
- **B. Rule consolidation** → `Handoffs/K2_HANDOFF_2026-06-30_fable5-consolidation.md`. The 8-move plan (`resources/Rando_Consolidation_Plan_2026-06-29.xlsx`) + the 3 council traps (V29.9/V29.11 nested guards; V120's bespoke detection must survive Move 6; Move 7's +400 cap is not a no-op). Moves 1-3 first, Move 8 last and alone. Move 6 touches V120, which the bug workstream just fixed — sync first.
- Start-of-session ritual for EITHER queue: check `logs/gemp-swccg.log` (and `logs/2026-07/*.log.gz`, `gunzip -c`) against the §4 table, report findings to Steve in a short table BEFORE coding.
- Multiple K-2s share ONE working tree. Coordinate via `Handoffs/` + `git status`. Two simultaneous sessions → ask Steve for a worktree.
- Steve's issue tracker: `resources/Rando_Issues_2026-06-29.xlsx`. Add new bugs there; Steve reads tables.

## 4. Pending live-game verifications (check the log after Steve plays)

| Tag | Confirm |
|---|---|
| V61c | reserve holds at 3; `V61c DESTINY BUFFER` pass lines when reserve ≤ 3; battles get destiny |
| V61b | fired once (Starkiller 18v1); watch it doesn't over-trigger close battles |
| V79b | Verge game: Death Star parsec 4→6 turn 1, 6→7 turn 2, orbits Scarif |
| V187 | `V187 DUPLICATE` on a deck with a doubled starting effect |
| V188 / V120 / V179 | Alderaan game / Hunt Down (Vader deploys) / Saga (Ahch-To before download, Be With Me pulls) |
| TDIGWATT A+B | after they're built |

## 5. Decisions waiting on Steve (do not act without him)

1. Commit the 4 code fixes as four separate commits + the doc work as a fifth?
2. The 3 archived skills (gemp-swccg-memory, karpathy-guidelines, skill-creator; byte-identical at `.claude/skills-archive/`): commit the deletion or restore?
3. The consolidation-plan "do now" items from the 06-30 file: fix the lying V67al comments; delete the 5 untracked junk files under `src/`.

## 6. Codex ("Alfred") status

- Codex worked in the repo 2026-06-29/30: sandbox = `.agents/` + `AGENTS.md` + the two xlsx files. **It touched ZERO Rando code** (verified).
- `.agents/skills/` is a blanket Claude→Codex find-replace with real bugs (broken `.Codex` paths, falsified PR #3260 history). **Never merge `.agents/` back into `.claude/`.**
- Codex MCP (`mcp__codex__codex`) usage-capped until ~Jul 29 2026. Onboarding doc: `Handoffs/CODEX_HANDOFF_2026-06-29.md`.
- Local council: deepseek-r1:70b direct at `http://127.0.0.1:11434/api/generate` (the :8000 FastAPI bridge was down last check). Council hallucinates card text — verify against code or `mcp-gemp-client/card_cache.json`.

## 7. Landmines (short list; full versions in memory + BUILD_AND_DEPLOY)

- NEVER: `docker compose down -v`, `rm -rf database/`, `bin/gemp reset-db`, unpin `mariadb:11.8.6`, push to GitHub without Steve's explicit ask.
- Host has NO JRE — build in-container; the `-am` flag is load-bearing. Deploy = `bin/gemp reload-ai`; `rebuild`/`rebuild-fast` do NOT restart the JVM.
- After every restart: flip the gameplay switches (login `asdf`; shutdown=false, then aitables/privategames/stattracking/newaccounts=true). Operational alone loads no AI tables.
- Before editing ANY rule: grep the enclosing `if (...)` for `if (false /* SUPERSEDED */)`. V67aj/V67al/V90 in DeployEvaluator are dead; live spread logic is V136 + V96.
- "It compiled" ≠ live: byte-search `web.jar` for your new log string (extract-to-file python zipfile method, BUILD_AND_DEPLOY §3), confirm small JVM etime, HTTP 200.
- The scratchpad is wiped between turns; re-extract log slices each time. macOS `zcat` is broken — `gunzip -c`.
- ObjectiveHandler.java is DEAD code; the live objective brain is ObjectiveAnalyzer. Temp-id trap in CardSelectionEvaluator.evaluateDeployLocation: resolve from `context.getBlueprints()`.

## 8. Where documentation truth lives

| Doc | Role | State |
|---|---|---|
| `resources/AI_CHANGELOG.md` | THE live changelog. Why + Boundary + Revert per fix. Update SAME session. | Current through 2026-07-01 |
| `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md` | V-tag archive (`════ Vxxx ════` blocks, newest ~line 3219). Update with the changelog. | Current through 2026-07-01 |
| `resources/BUILD_AND_DEPLOY.md` | Deploy + the 4 verify gates (compiles ≠ in jar ≠ loaded ≠ fired) | Current |
| `logs/gemp-swccg.log` | Live decision log (V-tag lines). Rotates 10MB → `logs/YYYY-MM/*.log.gz`. Replays: `replays/asdf/*.xml.gz` | Live |
| `resources/k2-resources/originals/02-rando-history/Rando_AI_Rule_Audit.xlsx` | Rule-contradiction audit. Consult before ANY new V-tag. | Stops at V115 (code at V188) |
| `.claude/skills/k2-swccg-strategy` | Architecture / file map / gameplay strategy | Rule coverage stale (V24–V49 era); structure only, never current-rule truth |
| `Handoffs/archive/` | All superseded handoffs incl. the K2-3/K2-4 session records and the 06-23 + 07-01 masters | History only |
