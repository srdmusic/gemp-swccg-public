# Interceptor And Proven Legacy Retirement Packet

Status: `FROZEN, PIPELINED, NOT YET RELEASED FOR JAVA`

This packet executes step 10 of `CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`. Release only after SETUP and every earlier strategic phase pass independent gates.

## Goal

Remove only old interceptors, duplicate contribution arms, disabled blocks, and commented code whose replacement owner and exact fixture are already proven. This phase is behavior-neutral cleanup, not a new strategy phase. Apply the stricter `CODEX_POST28_CLEANUP_STOP_BOUNDARY_2026-07-13.md` whenever it covers a candidate.

## Frozen Retirement Matrix

| Legacy item | Required owner proof | Action |
|---|---|---|
| V44/V67j revert interceptor | Completed in step 1b with reordered-label fixture | Already removed. Verify absent only. |
| V45 optional forfeit interceptor | BATTLE owner plus exact forfeit/immunity fixtures | Remove both-bot copies only if the gate record exists. |
| V170 Undercover interceptor | Hash-pinned amended DEPLOY packet and commit proving exact typed `PlayCharacterAction` Undercover ownership, both-bot fixtures/lifecycle, and both live call sites disabled | Remove both-bot source blocks only when every proof exists; otherwise retain. |
| V61 saga/setup interceptor | SETUP owner plus exact starting-process fixture | Remove both-bot copies only if the gate record exists. |
| V79b parsec interceptor | Documented intentional Rando-only waiver | Retain. Do not manufacture parity. |
| V122, V67as, V193 constant-false or held blocks | Exact replacement owner plus named fixture and zero-live-caller proof | Remove individually or retain with reason. |

The post-2.8 audit found 24 mirrored comment pairs spanning roughly 840 lines. That inventory is a review list, not deletion authority.

## Group Boundary

Retire one proven group at a time. A direct interceptor or executable predecessor may enter this
phase only after an earlier behavioral phase disabled its live call site and recorded replacement
fixtures. A held post-2.8 comment family may enter only when it is no longer transaction evidence.
Do not combine direct interceptors, held comments, and constant-false blocks into one deletion group.

Before releasing a group, freeze a hash-pinned addendum naming the exact source paths and marker
boundaries, replacement owner, fixture selectors, parent SHA, expected bytecode classes/callers, and
retained positive controls. After that group is committed and its evidence returned, stop until an
independent Codex `ADVANCE` releases the next group. Aggregate suites remain step 12; do not rerun
them between retirement groups.

## Proof Required Per Deletion

Every removed block needs all seven:

1. One named production owner in the new phase architecture.
2. A named fixture that covers the old block's exact positive, negative, tie, and fallback boundary.
3. Source and bytecode proof that the new owner is live and the old owner is not called.
4. Rando/ChosenOne parity proof, unless the block is an explicit documented waiver.
5. A changelog entry with Why, Boundary, Revert, and the replacing V-tag/owner.
6. Released rollback evidence and a hash-pinned before-state for the exact group.
7. Detached packaging plus normalized bytecode proof for executable predecessors, or a mirrored
   comments-only diff for a held comment group.

If any proof is absent, retain the block and record the missing proof. Do not infer replacement from similar names or comments.

## Cleanup Rules

- Keep executable retirement and held-comment cleanup in separate groups. For executable
  predecessors, delete the disabled source and its directly attached comments together after proof.
  For held comment families, use one mirrored, hash-pinned, comments-only group as required by the
  post-2.8 boundary.
- Remove imports, fields, helpers, tests, and trace labels only when this phase's deletions make them unused.
- Preserve history in `resources/AI_CHANGELOG.md` and `AI_VERSION_HISTORY.md`; do not keep dead production comments as history storage.
- Do not reformat surrounding evaluators or rename unrelated classes.
- Do not alter scores, route precedence, candidate order, mutation mode, or final responses.

## Verification And Commit

No tests while editing. At each coherent group boundary:

0. Capture `git status --porcelain=v1 -z` as the pre-status artifact, require an empty index, and
   require every exact target path to be clean before editing. Preserve the NUL-delimited bytes.
1. Run each retired block's named owner fixture in both bots.
2. Run only the group's focused strategic route, accepted-response lifecycle, trace, and mirrored-
   source regression selectors. Full aggregate suites remain step 12.
3. Run marker-scoped searches inside `RandoCalAi.java`, `TheChosenOneAi.java`, and the exact predecessor
   files. Prove the retired marker block and caller are absent while unrelated same-tag owners and the
   V79b waiver remain present. Tag-wide absence is not a valid deletion proof.
4. Inspect bytecode/callers where source-only grep cannot prove retirement.
5. Run `git diff --check`, stage only the hash-pinned allowlist, and prove the staged path list exactly
   equals that allowlist before committing.
6. Make one coherent group commit. Do not batch unrelated retirement classes to reduce commit count.
7. Capture the post-commit NUL-delimited status and prove it is byte-identical to pre-status except
   that the allowlisted retirement diff is now committed. Return commit/parent SHA, deleted-line count
   by V-tag, replacement owner/fixture table, retained-list reasons, test counts, bytecode proof, and
   zero wire/operation delta proof. Then stop for independent `ADVANCE`.

## Hard Stops

- A deletion is justified only by a comment, title, or approximate behavior.
- A fixture proves only the winner but not ordered operations and fallback behavior.
- Rando and ChosenOne cleanup differs without an approved waiver.
- Cleanup changes any score, route, response, mutation, or candidate ordering.
- The diff expands into unrelated formatting or refactoring.
- A target path was dirty before the group, the index was non-empty, staged paths differ from the
  allowlist, or unrelated worktree bytes change.
- A held post-2.8 family lacks any reopen proof required by
  `CODEX_POST28_CLEANUP_STOP_BOUNDARY_2026-07-13.md`.
