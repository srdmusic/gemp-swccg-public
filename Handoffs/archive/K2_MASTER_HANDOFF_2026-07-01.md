# K-2 MASTER HANDOFF — 2026-07-01 — THE entry point for a fresh Fable 5 session

You are **K-2** (Claude, Fable 5 model), Steve's AI on GEMP-SWCCG. Steve = Steven Richard Davis, GEMP `asdf`, SWCCG expert, ADHD + dyslexia: concise replies, single-layer bullets, tables over prose, no em-dashes inline. Persona + comm rules: `.claude/CLAUDE.md`.

This file REPLACES `K2_MASTER_HANDOFF_2026-06-23.md` as the master. It is a ROUTER: current state + where the real docs live. It does not duplicate them.

---

## Read order (non-negotiable)

1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — auto-loads. The `feedback_*` entries are law: one-change-at-a-time, breadcrumbs-every-fix, check-rule-is-live-before-editing, update-old-rule-not-new-version, card-search-by-type-not-text, verify-before-done.
2. THIS FILE.
3. The handoff for YOUR workstream (§3 below) — only that one.
4. `resources/BUILD_AND_DEPLOY.md` — before any edit (§1: is the rule live?) and any deploy (§2 + §3).
5. Only if needed: `resources/k2-resources/distilled/00-START-HERE.md` (project hub, stale past ~V186).

The one discipline: **Rando scoring is ADDITIVE. Old rules get DOMINATED, not deleted. Do the boundary math BEFORE writing code.** Four incidents so far.

## 1. Current state (verified by 5-agent audit, 2026-07-01)

- Branch `rando-consolidation-2026-06-23`, HEAD `37c352d87`, base devs `55c22cf49`. **Local only. NOTHING pushed to GitHub (Steve's standing order).**
- Committed + live: V188 (Alderaan Death-Star block), V120 fix (Vader pull), V179 fix (held-location download), V67h junk pass-through, V22 Silence Is Golden, V187 (duplicate starting effects, swept into `8fd884375`), V156 smart-solo update (swept into `d72ced949`), Fix #2 §A contested gate (`4f8ec16d3`), cancel-loop fix (`d1e2aa890`), auto-flip on boot (`b89268344`).
- **UNCOMMITTED but live in the running jar** (6 files, all one K-2's finished force-management work): V61b (battle when overpowering, empty reserve), V61c (keep-3 destiny buffer, both halves), V79b (Death Star parsec choice), MoveEvaluator V79 rider, log4j mainlog. Files: `rando/RandoCalAi.java`, `rando/evaluators/{ActionTextEvaluator,BattleEvaluator,ForceActivationEvaluator,MoveEvaluator}.java`, `async/.../prod-log4j.xml`. Ask Steve before committing or discarding. `git add` stages whole files; two prior commits accidentally swept cross-K2 work — stage only what YOU changed.
- Every V-tag above has a changelog entry as of 2026-07-01 (backfill done). `git status`/`git diff` remains the source of truth for in-flight work — check it before editing anything.

## 2. Where documentation truth lives

| Doc | Role | State |
|---|---|---|
| `resources/AI_CHANGELOG.md` | THE live changelog. Every divergence from devs code: Why + Boundary + Revert. Update SAME session as any fix. | Current through 2026-07-01 |
| `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md` | The V-tag archive (`════ Vxxx ════` blocks, newest ~line 3219). Update with the changelog. | Current through 2026-07-01 |
| `resources/BUILD_AND_DEPLOY.md` | Deploy + 4 verify gates (compiles ≠ in jar ≠ loaded ≠ fired) | Current |
| `logs/gemp-swccg.log` | Live decision log (V-tag lines). Rotates 10MB → `logs/YYYY-MM/*.log.gz` (`gunzip -c`, macOS zcat broken). Replays: `replays/asdf/*.xml.gz`. | Live |
| `resources/k2-resources/originals/02-rando-history/Rando_AI_Rule_Audit.xlsx` | Rule-contradiction audit. Consult before ANY new V-tag. | Stops at V115 (code is at V188) |
| `.claude/skills/k2-swccg-strategy` (live) + `.claude/skills-archive/gemp-swccg-memory` (archived 2026-07-01 with karpathy-guidelines + skill-creator; byte-identical to HEAD, untracked) | Architecture, file map, gameplay strategy | Rule coverage stale (V24–V49 era). Use for structure/strategy, NEVER as current-rule truth — the changelog is truth. |

## 3. The two workstreams — pick yours, don't collide

- **Bug fixes / gameplay** → `Handoffs/K2_HANDOFF_2026-07-02_fable5-onboarding.md`. Queued: the two TDIGWATT bugs (V177 dead-search false negative starving I'm Sorry's site downloads; V29 BESPIN-FIRST demanding a forbidden Executor), diagnosed with file:line evidence, ready to build. Plus the V61c battle-intent refinement (Steve-approved plan in that same file: activate ALL force on turns Rando won't battle; keep 3 only when battle is plausible).
- **Rule consolidation** → `Handoffs/K2_HANDOFF_2026-06-30_fable5-consolidation.md`. The 8-move plan (`resources/Rando_Consolidation_Plan_2026-06-29.xlsx`) + 3 council traps. Move 6 touches V120, which the bug workstream just fixed — sync first.
- Multiple K-2s share ONE working tree. Coordinate via `Handoffs/` + `git status`. If two sessions run at once, ask Steve for a worktree.
- Steve's issue tracker: `resources/Rando_Issues_2026-06-29.xlsx`. Add new bugs there; Steve reads tables.

## 4. Pending live-game verifications (check `logs/gemp-swccg.log` after Steve plays)

| Tag | Confirm |
|---|---|
| V61c | reserve holds at 3; `V61c DESTINY BUFFER` pass lines when reserve ≤ 3 |
| V61b | already fired once (18v1 battled); watch it doesn't over-trigger close battles |
| V79b | Verge game: Death Star 4→6 turn 1, 6→7 turn 2, orbits Scarif |
| V187 | `V187 DUPLICATE` on a deck with a doubled effect |
| V188 / V120 / V179 | Alderaan / Hunt Down (Vader deploys) / Saga (Be With Me pulls) |
| TDIGWATT A+B | after they're built |

## 5. Codex ("Alfred") status

- Codex worked in the repo 2026-06-29/30: its sandbox is `.agents/` + `AGENTS.md` + the two xlsx files. **It touched ZERO Rando code** (verified).
- `.agents/skills/` is a blanket Claude→Codex find-replace of `.claude/skills/` with real bugs: broken `.Codex` paths, falsified incident history (it claims Codex pushed PR #3260 — false). **Never merge `.agents/` back into `.claude/`.**
- Codex MCP (`mcp__codex__codex`) is usage-capped until ~Jul 29 2026. Its onboarding doc: `Handoffs/CODEX_HANDOFF_2026-06-29.md`.
- Local council: deepseek-r1:70b direct at `http://127.0.0.1:11434/api/generate` (FastAPI bridge on :8000 was down last check). Council hallucinates card text — verify against code or `mcp-gemp-client/card_cache.json`.

## 6. Landmines (the short list; full versions in memory + BUILD_AND_DEPLOY)

- NEVER: `docker compose down -v`, `rm -rf database/`, `bin/gemp reset-db`, unpin `mariadb:11.8.6`, push to GitHub without Steve's explicit ask.
- Host has NO JRE — build in-container; the `-am` flag is load-bearing. Deploy = `bin/gemp reload-ai`; `rebuild`/`rebuild-fast` do NOT restart the JVM.
- After every restart: flip the gameplay switches (login `asdf`; shutdown=false, then aitables/privategames/stattracking/newaccounts=true). Operational alone loads no AI tables.
- Before editing ANY rule: grep the enclosing `if (...)` for `if (false /* SUPERSEDED */)`. V67aj/V67al/V90 in DeployEvaluator are dead; live spread logic is V136 + V96.
- The scratchpad is wiped between turns; re-extract log slices each time.
