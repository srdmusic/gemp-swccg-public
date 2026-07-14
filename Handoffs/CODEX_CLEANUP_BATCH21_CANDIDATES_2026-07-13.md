# Codex cleanup 2.1 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Baseline: `82e4bc6ec`
Scope: obsolete T4.1 scoring predecessors in mirrored `MoveEvaluator.java` files

## Verdict

`ADVANCE` this exact comments-only packet after Cleanup 2.0 is committed or declined. MOVE policy,
scoring, route ownership, and runtime cutover remain `HOLD`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/MoveEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/MoveEvaluator.java`

Apply the same edit to both bot files. Recheck that neither file is dirty before editing.

## Exact allowed deletions

| Marker | Current baseline lines | Commented predecessor removed | Lines per bot |
|---|---:|---|---:|
| V47 Lando stay | `697-698` | `-9999` reasoning | 2 |
| V135 self-move alone | `1227-1229` | `-2000` reasoning | 3 |
| V137 winnability | `1460-1461` | inline clean-win boolean | 2 |
| V53b Safehouse transit | `2043-2044` | `setScore(9999)` plus duplicate reasoning | 2 |
| V60 Corridor landspeed | `2057-2058` | `setScore(-9999)` plus duplicate reasoning | 2 |
| V37.1 CRUSH | `2191-2192` | `-9999` reasoning plus early return | 2 |
| V37.1 FAVORABLE | `2201-2202` | `-9999` reasoning plus early return | 2 |
| V85 uncontested drain | `2262` | `-2000` reasoning plus early return | 1 |
| V38.3 wrong direction | `2892-2895` | local `-9999` reasoning | 4 |
| V38.3 Castle retreat | `2926-2927` | local `-9999` reasoning | 2 |
| default MOVE tax | `2942` | local `-50` reasoning | 1 |
| V49 unprotected landing | `3268-3270` | local `-9999` reasoning | 3 |

Expected net reduction: 26 physical lines per bot, 52 total. Git diff should report 28 deletions
and 2 insertions per bot because the two retained prefaces are rewritten in place.

Also make these two line-count-neutral comment corrections in both files:

- V47 preface: remove the dangling `Old line:` phrase and end with `magnitude now band-proof.`
- V135 preface: remove the dangling `Old lines kept for revert:` phrase and end with
  `that lands ALONE is absolutely blocked.`

Re-anchor by marker and statement. Line numbers are baseline hints only, especially if Cleanup 2.0
lands first.

## Mirror proof

The combined 26-line deletion stream has identical SHA-256 in Rando and ChosenOne:

```text
08324eddfeb9013a05d9a453d1afe71ded2baa2b1907f131c08e681d7fa98a6d
```

## Replacement-owner proof

Each deleted statement has an adjacent live owner:

- V47, V135, V60, V38.3 Castle, and V49 set ladder hard-veto state.
- V137 uses `MovePredicates.canWinAt()` and retains its R1 deterrent.
- V53b claims R4 mandatory transit and retains its `+800` fine.
- V37.1 CRUSH/FAVORABLE retain `-1500` R1 weights without early returns.
- V85 retains its `-800` R1 weight without early return.
- V38.3 wrong direction sets the deferred veto with the transit carve-out.
- The default `-50` tax is applied by `ladderFinalize()` only for R1.

The adjacent comments retain trigger, rank, magnitude, and carve-out rationale. Git and the verified
local backup preserve the superseded statements.

## Explicit exclusions

- Do not alter live Java statements, score magnitudes, thresholds, claims, vetoes, or route order.
- Do not delete explanatory comments that describe the live replacement.
- Do not fold force-reserve scans or unrelated MOVE residue into this commit.
- Do not edit Rando without the identical ChosenOne change.
- Do not deploy.

## Required gate

1. Exact diff contains only the two mirrored files plus changelog/history bookkeeping.
2. Exact source diff matches this packet and reports 28 deletions, 2 insertions, and net 26 lines
   removed per bot.
3. Rando/ChosenOne deletion and replacement-comment streams are identical.
4. `git diff --check` passes.
5. Affected-module package succeeds in a clean detached worktree.
6. Parent and candidate normalized `javap -p -c -s -constants` are identical for both
   `MoveEvaluator.class` files.
7. Existing focused MOVE, formation, force-reserve, route, and trace fixtures pass in both bots.
8. No deployment.
