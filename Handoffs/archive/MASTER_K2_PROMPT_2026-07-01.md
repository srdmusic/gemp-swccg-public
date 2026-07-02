# Paste-ready prompt for the Master K2 (written by K2-4, 2026-07-01)

You are the **Master K-2** (Fable 5) on GEMP-SWCCG at `/Users/steve/gemp-swccg-public`. Your job this session is **CONSOLIDATION of documentation, not code**. Four K-2 sessions (K2-1 through K2-4) left handoff files; you merge them into ONE master. Touch nothing in `src/`.

1. Read in this order:
   - `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — the `feedback_*` standing rules are law.
   - `Handoffs/K2_MASTER_HANDOFF_2026-07-01.md` — K2-4's router, the current entry point you are replacing.
   - `Handoffs/K2-4_HANDOFF_2026-07-01_consolidation-inputs-for-master.md` — machine-verified ground truth, the artifact→session map (note: K2-3's file mislabels K2-4's work as "K2-1's"), and the residual punch list.
   - `Handoffs/K2-3_HANDOFF_2026-07-01_force-management-and-doc-audit.md` — the force-management fixes (V61b/V61c/V79b/log4j/V187) and doc-audit patches.
   - `Handoffs/K2_HANDOFF_2026-07-02_fable5-onboarding.md` — K2-2's bug workstream: TDIGWATT bugs A+B (diagnosed, unbuilt) + the V61c battle-intent plan.
   - `Handoffs/K2_HANDOFF_2026-06-30_fable5-consolidation.md` — K2-1's rule-consolidation workstream: the 8-move plan + 3 council traps.
   Skim the rest of `Handoffs/` newest-first as history only.

2. Verify before carrying ANY claim forward: `git status --short`, `git log --oneline -15`, the tail of `resources/AI_CHANGELOG.md`, and the jar-verify method in `resources/BUILD_AND_DEPLOY.md` §3. Where a handoff and reality disagree, reality wins — record the correction. K2-4's §3 ground truth (verified 2026-07-01) may be trusted as-is unless git state has moved since.

3. Write ONE new master: `Handoffs/K2_MASTER_HANDOFF_<today>.md`. It fully replaces the 07-01 router and must absorb: current verified state (committed vs uncommitted, what's live in the jar), the two live workstream queues (TDIGWATT A+B; the 8-move plan + its traps), the V61c battle-intent plan, the pending live-game verification table, the doc map, Codex status (`.agents/` = sandbox with known bugs, never merge; Codex MCP capped until ~Jul 29), the landmines, and K2-4's punch-list items (stale pointers in the 07-02 and 06-30 files).

4. Archive: move every superseded handoff into `Handoffs/archive/` (move, never delete). Keep in `Handoffs/`: your new master, the two live workstream files, `CODEX_HANDOFF_2026-06-29.md`, and the K2-3/K2-4 session records only if you judge them still load-bearing.

5. Repoint: `.claude/CLAUDE.md` First-reads item 2 and the MEMORY.md Project lines at your new master.

6. ASK STEVE before acting (do not act on these without him):
   (a) Commit the 4 uncommitted code fixes as four separate commits (V61c, V61b, V79b+MoveEvaluator, log4j) plus the doc work as a fifth?
   (b) The 3 skills moved to `.claude/skills-archive/` (verified byte-identical to HEAD) — keep the move or restore?
   (c) Confirm the archive step in 4 before moving files.

7. Guardrails: nothing gets pushed to GitHub. No `src/` edits this session. One change at a time. NEVER: `docker compose down -v`, `rm -rf database/`, `bin/gemp reset-db`, unpin `mariadb:11.8.6`. If another K-2 session is active, coordinate via `git status` before touching shared files.
