# Codex cleanup 1.9 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Baseline: `21dda1a67367da689e6f610f7111b6dbdfee0c2e`
Scope: comments-only residue in mirrored `MoveEvaluator.java` files

## Verdict

`ADVANCE` this exact comments-only packet for implementation. MOVE policy, scoring, route
ownership, and runtime cutover remain `HOLD` under `CODEX_MOVE_ROUTE_AUDIT_2026-07-13.md`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/MoveEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/MoveEvaluator.java`

Neither file was dirty when this packet was prepared. Recheck before editing.

## Exact allowed edits

Apply the same change in both bot files.

| Marker | Current clean-baseline lines | Allowed edit | Net per bot |
|---|---:|---|---:|
| V169 old local soft-block implementation | `408-412` | Delete the five commented Java statements beginning `EvaluatedAction softBlockedMove` and ending `continue`. Keep the live warning and the current owner explanation. | `-5` |
| V160 old `-9999` cancel-loop implementation | `417-422` | Delete the three commented Java statements. Condense the preceding live explanation to two comment lines stating that T4.1 raised the cancel-loop veto from `-9999` to ladder class `-100000`, above all score bands including R4 transit. | `-4` |
| V79 broken mobile-system location lookup | `506-510` | Delete the five commented Java statements using `cardToMove.getAtLocation()`. Keep the explanation and live `getSystemOrbited()` implementation. | `-5` |

Expected net deletion: 14 physical lines per bot, 28 total.

Re-anchor by the exact markers and statements above. Line numbers are hints only after either file
moves.

## Mirror proof

The combined baseline chunks `408-412`, `417-422`, and `506-510` have identical SHA-256 in both
bot files:

```text
9381e9f08350f9d75f08ce4175a21fb55b2e083b40a2f532c5639fb4f36b2c52
```

## Why this packet is behavior-neutral

| Removed predecessor | Live replacement retained |
|---|---|
| V169 local `-400` plus duplicate reasoning and early `continue` | The endangered mover falls through; ActionText owns the retry-budgeted `-250`; live warning remains. |
| V160 local `-9999` block | Live `EvaluatedAction` receives the `-100000` ladder-veto reasoning and continues to block. |
| V79 `getAtLocation()` test | Live `getSystemOrbited()` detects Scarif for the mobile-system location card. |

The historical implementation remains recoverable from Git and the verified local backup at
`/Users/steve/gemp-swccg-public-backup-20260712-221311`.

## Explicit exclusions

- Do not alter live Java statements.
- Do not retune `-250`, `-100000`, V79 bonuses, or any pass threshold.
- Do not change comments outside the three named regions.
- Do not fold other MoveEvaluator corpses into this commit.
- Do not edit Rando without the identical chosenone change.
- Do not update or deploy a live server from a dirty tree.

## Required gate

1. Exact diff contains only the two mirrored files plus changelog/history bookkeeping.
2. Exact source diff matches this packet and reports 14 net lines per bot.
3. Rando/chosenone deletion streams are identical.
4. Affected-module package succeeds in a clean detached worktree.
5. Parent and candidate normalized `javap -p -c -s -constants` are identical for both
   `MoveEvaluator.class` files.
6. Existing focused MOVE/formation fixtures pass in both bots.
7. `git diff --check` passes.
8. No deployment.

Normalized bytecode equality is the behavior-neutral proof. Raw class hashes are not sufficient
because source-line metadata changes.
