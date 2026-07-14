---
name: work-verifier
description: Independent verification agent for GEMP-SWCCG project. Invoked AFTER risky operations to catch the "trust without verify" class of mistakes Codex makes when pattern-matching from prior sessions. Reads project state with fresh eyes, runs structured verification protocols, and reports PASS/FAIL with actionable errors. Maintains an append-only history of past verifications so it gets smarter over time.
---

# Work Verifier

## What this is

A sophisticated, project-aware verification agent. Codex invokes it via
the `Agent` tool with `subagent_type: general-purpose` after performing
any of the 4 trigger operations. The agent gets a fresh context window
and reads project state without Codex's biases.

The agent's job is to **catch mistakes BEFORE they reach Steve**.

## Why this exists

Two real failures from May 19-20 2026 sessions:

1. **PR #3260 base mismatch** — Codex pushed an "AI-only" PR but based
   the branch on `origin/master` (public mirror) instead of
   `pc-private/master` (correct private base). Result: 3000+ files,
   933 commits in the diff. Maintainer flagged it within an hour.
2. **Card images missing after rebuild** — Codex ran `unzip -oq`
   (quiet mode) after a docker nuke. The quiet flag suppressed the
   warning that `StreamingAssets/` already existed as an empty dir.
   `cardImages.json` (379KB, 4345 card URLs) silently failed to extract.
   Codex declared "fixed" without running `ls` on the destination.

Both failures share the same root cause: Codex trusted the action
without verifying the result. This skill exists so a fresh agent
verifies independently.

## When Codex invokes this

After any of these 4 trigger operations, BEFORE telling Steve "done":

| # | Operation | Reference file |
|---|---|---|
| 1 | `git push`, `gh pr create`, force-push | `references/verify-git-push.md` |
| 2 | Full Docker nuke + rebuild | `references/verify-docker-rebuild.md` |
| 3 | `unzip`, `tar -x`, `cp -r` (mass file ops) | `references/verify-extract.md` |
| 4 | Multi-file edit to AI evaluators | `references/verify-evaluator-edit.md` |

## How Codex invokes it

```
Agent({
  subagent_type: "general-purpose",
  description: "Verify <operation>",
  prompt: <prompt from the matching reference file>
})
```

Codex reads the matching reference file, copies the prompt template,
fills in the context (branch name, commit hash, paths, expected
results), and sends it to the agent.

The agent:
1. Reads this SKILL.md plus the matching reference file
2. Runs the verification commands listed
3. Cross-references against `history.md` for past failures
4. Returns a structured report: PASS, WARN, or FAIL with specifics
5. Appends its findings to `history.md`

## The verification report format

The agent returns ONE of:

- **PASS** — all checks succeeded. Codex can report success to Steve.
- **WARN** — minor issues that should be flagged but don't block.
  Codex tells Steve about them.
- **FAIL** — at least one check failed. Codex MUST fix the
  underlying problem and re-invoke the verifier before declaring done.

Every report ends with the actual numbers (file counts, byte sizes,
HTTP codes, log line counts) so Codex can confirm independently.

## Project context (preloaded for the agent)

- Working dir: `/Users/steve/gemp-swccg-public`
- Source root: `src/`
- GEMP server: `localhost:17001`
- Container names: `gemp_swccg_app_1`, `gemp_swccg_db_1`
- Git remotes:
  - `origin` = PlayersCommittee/gemp-swccg-public (PUBLIC mirror)
  - `dev-fork` = srdmusic/gemp-swccg (Steve's fork)
  - `pc-private` = PlayersCommittee/gemp-swccg (PRIVATE dev repo)
- Key paths:
  - AI evaluators: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/`
  - Web bundle source: `src/gemp-swccg-async/src/main/web/`
  - Web bundle deployed: `/opt/gemp-swccg/web/` (in container)
  - Built zip: `/opt/gemp-swccg/src/gemp-swccg-async/target/web.zip` (in container)

## History

The agent appends every verification run to
`/Users/steve/gemp-swccg-public/.Codex/skills/work-verifier/history.md`
with timestamp, operation type, PASS/FAIL outcome, and any issues caught.

When the agent is re-invoked, it reads recent history to learn
patterns of past failures and check for them in the current state.

## Self-maintenance contract

This skill is expected to get smarter over time. Codex (the main
session, not the agent) maintains it. After these events, Codex
must update the skill files:

1. **A bug slipped past the verifier** — Steve found a problem that
   PASS missed. Add a new check to the matching `references/verify-*.md`.
   Append the miss to `history.md` with `→ MISSED` so the agent can
   cross-check next time.

2. **A new type of risky operation** — create a 5th (or 6th, etc.)
   protocol file in `references/`, register it in the trigger table
   above, and update the memory rule at
   `/Users/steve/.Codex/projects/-Users-steve-gemp-swccg-public/memory/feedback_verify_before_done.md`.

3. **Steve gives verification feedback** — encode the feedback as a
   concrete step in the matching protocol. Don't trust memory across
   sessions; write it into the file.

4. **A check fires often** — promote it from spot-check to "always run
   first". Reorder steps so the highest-value checks run early.

The agent itself only appends to `history.md`. Modifying the
SKILL.md or `references/` is Codex's responsibility, not the
agent's, because those files shape the agent's behavior on every
subsequent run and need human-loop oversight.
