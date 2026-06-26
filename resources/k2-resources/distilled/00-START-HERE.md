# K2 Resources — START HERE

Orientation hub for any future **K-2** (Claude Code) session on the **GEMP-SWCCG** project — a web Star Wars CCG game server plus the custom **Rando Cal** AI bot — for Steve (`asdf` / steve@srdmusic.com).

Built 2026-06-22 by K-2, consolidated from the scattered `.md` files in `GEMP ARCHIVE/gemp-swccg-public-BACKUP-2026-06-20`.

> **This is an ARCHIVE, not the live project.** The live install is `/Users/steve/gemp-swccg-public`. Use THIS folder for depth.
>
> **READ YOUR AUTO-MEMORY FIRST:** `/Users/steve/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` is the index (loaded each session) of the standing rules and project state you must work by. It links:
> - `feedback_*` — the standing rules (one-change-at-a-time, breadcrumbs on every fix, verify-before-done, search-by-type-not-text, no fabrication, and more). These are non-negotiable.
> - `project_*` — current work state (install provenance, the devs-Rando bug, the changelog location, etc.).
> - `reference_*` — pointers including this k2-resources archive.
> - `user_*` — who Steve is and how he works.
>
> Open `MEMORY.md`, then read the linked files relevant to your task. The handoffs and changelog below are the *delta* on top of those rules.

## Latest live state (2026-06-23) — read this after the fast path
This archive's Rando history stops at V184. **THREE K-2s are working the live install concurrently on one uncommitted tree** (no commits on `55c22cf49` → no git merge protection). To consolidate, read **all three** handoffs below — they are different angles, not duplicates:
- **`/Users/steve/gemp-swccg-public/Handoffs/K2_HANDOFF_2026-06-23.md`** — angle 1: collision map for the 3 K2s, V185 + V186, and two gotchas every objective edit needs (`ObjectiveHandler` is DEAD code; the temp-id trap in `evaluateDeployLocation`).
- **`/Users/steve/gemp-swccg-public/Handoffs/K2_HANDOFF_2026-06-23_audit-V185-council.md`** — angle 2 (audit / findings / council): the backup + breadcrumb audit (nothing to merge — current install is the most complete version; Rando reads zero runtime files), the 5 verified code gaps (**V96/V67al silent score-domination = #1**, the 3 dead V136 stubs, V53b/V60 precedence, the missing dojo harness), the 3 never-coded rules, the disproved false-alarms (don't chase them), and the council's confirmation. Detail: `RANDO_BACKUP_AUDIT_2026-06-23.xlsx` (Breadcrumb Findings tab) + `RANDO_MISSING_LOGIC.md`.
- **`/Users/steve/gemp-swccg-public/Handoffs/K2_HANDOFF_2026-06-23_verification-and-consolidation.md`** — angle 3 (verification / version-history / onboarding): proved the live install is the most complete copy (the `OURS` `run-devs-pure` stub is the comparison trap), the content archaeology audit (the 3 `RANDO_MISSING_LOGIC` behaviors are genuinely absent by content; V186 IS built), V185+V186 recorded into `AI_VERSION_HISTORY.md`, and this onboarding refresh. Verification + docs only, no `src/` edits.
- **`/Users/steve/gemp-swccg-public/resources/AI_CHANGELOG.md`** — V185 + V186 + auto-pass + card-proxy, each with Why + Revert.
- **Key shared insight for consolidation:** the recurring "dumb moves I thought we fixed" are mostly **silent score-magnitude domination** (a present rule out-scored by a bigger newer rule), NOT missing code — building the dojo regression harness is the systemic fix.

## Two folders
- **`distilled/`** — canonical, organized. Start here.
- **`originals/`** — every source doc kept verbatim (source of truth if a distilled doc is ever unclear).

## Fast path (under 5 minutes)
1. **`distilled/K2_ONBOARDING.md`** — project overview, setup/build/deploy commands, the Rando AI + V-tag system, working norms. *(It flags a few stale conflicts — e.g. V136 status, DB port, build path, branch — verify those against `git` at session start.)*
2. **`distilled/ai-instructions.md`** — operating rules for working here + an index of the available skills.
3. **`distilled/rando-history-highlights.md`** — the arc of the Rando work (V21–V186), key V-tags, and superseded rules you must not re-break.
4. **`distilled/swccg-reference.md`** — index into the ~41 SWCCG rules/glossary/expansion docs.

## Where the raw material lives (`originals/`)
| Folder | Contents |
|---|---|
| `01-ai-instructions/` | CLAUDE.md, AGENTS.md, full skills tree |
| `02-rando-history/` | full AI_CHANGELOG.md (2336 L), AI_VERSION_HISTORY.md (5358 L), 2 audit `.xlsx` |
| `03-handoffs/` | the 5 original session handoffs |
| `04-setup-context/` | context.md, PC_SETUP_CONTEXT.md, BUILD_AND_DEPLOY.md, README.md |
| `05-swccg-reference/` | ~41 SWCCG rules/glossary/expansion/cardlist docs (+ images) |

## Important
Rando does **not** read any of these files at runtime (verified — every doc-name mention in the AI code is a comment). They are documentation only: safe to read, reorganize, or extend without affecting the game.
