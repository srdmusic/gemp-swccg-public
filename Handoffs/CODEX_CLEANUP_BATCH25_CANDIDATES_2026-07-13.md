# Codex cleanup 2.5 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Frozen source: `DeployEvaluator.java` blobs at `13db1dfde`
Scope: mirrored V38/V53 diagnostic force-reserve predecessors

## Verdict

`ADVANCE` this exact comments-only packet after Cleanup 2.4 is cleanly committed and gated.
Executable solo-deploy policy, `pairedDeployPossible`, Stage 4 Java, deployment, and push remain
`HOLD`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/DeployEvaluator.java`

Apply the identical edit to both files. Re-anchor by the markers and live owners below.

## Exact allowed deletions

Delete only these four comment-only predecessor ranges from each bot:

1. The 15-line V38 inline maintenance scan at frozen lines 2099-2113, from
   `// int maintObligation = 0;` through its commented closing brace.
2. The single commented V38 statement at frozen line 2116:

```java
// maintObligation += cost;  // superseded T2 COMMIT-1 2026-07-06 (deploy-cost basis)
```

3. The 15-line V38 DTF scan at frozen lines 2126-2140, from
   `// int interruptReserve = 0;` through its commented closing brace.
4. The 13-line V53 undercover-spy scan at frozen lines 2153-2165, from
   `// int spyMoveReserve = 0;` through the commented catch.

The normalized deletion streams total 44 lines per bot. Each bot's byte-identical stream has
SHA-256:

```text
65a1987dfeebd6b6639648f2cdbc28d5fcd8eed09aae6e17c2440d742a0b87d9
```

The Rando stream followed by the ChosenOne stream has SHA-256:

```text
5bb4259f71064ee52ca66ef39dca636a50440f875444cbd42583a07270c0daef
```

Individual normalized SHA-256 values are:

- V38 on-table maintenance sum: `246f34e6566d02fc30c51de6227b0b91af27011f305b74cf90c7dcedf3a0ebe3`.
- V38 one-line candidate maintain cost: `26470e9d5ed81e47c4571b010ce8a9b6e982dbe2523d3e1e719dc4031ce6898b`.
- V38 DTF scan: `f5d181688f3316d32143643461bce3135aa3ae41d013826801b1e8d1ab30a817`.
- V53 undercover-spy scan: `9dcdb7b8f75da38923d2527100ad558785648c4ad3eec9c8da74717d55960f97`.

## Exact preface rewrites

Keep the first three lines above `maintObligation`. Replace only the current final two lines with:

```java
// below. V67bl removed the paired solo exception, so these facts now
// affect diagnostics only. Superseded inline scan removed; see git.
```

Replace the complete four-line preface above `interruptReserve` with:

```java
// T2 MOVE #1 COMMIT-2 (2026-07-06): ForceReserveService preserves
// exact-opponent, in-play detection and the 1 Force fact. V67bl means
// pairedDeployPossible has no score consumer. Superseded scan removed;
// git preserves it.
```

Replace the complete five-line preface above `spyMoveReserve` with:

```java
// T2 MOVE #1 COMMIT-2 (2026-07-06): ForceReserveService owns the
// count. Its in-play gate is behavior-neutral because GameState clears
// the undercover flag off table. V67bl leaves no score consumer.
// Superseded inline scan removed 2026-07-13; git preserves it.
// The diagnostic V53 log remains live.
```

Expected source diff per bot: 11 insertions, 55 deletions, net 44 physical lines removed. Total
source reduction: 88 lines.

## Replacement-owner proof

| Predecessor | Live owner | Boundary retained |
|---|---|---|
| V38 inline maintenance sum | `ForceReserveFacts.maintenanceObligation` | Same owner, in-play, maintenance-icon, and `MaintenanceFacts` guards |
| V38 candidate deploy-cost basis | `MaintenanceFacts.maintainCost(blueprint)` | Correct card-specific maintain cost under the same maintenance-icon gate |
| V38 DTF scan | `ForceReserveFacts.dtfActive` | Exact opponent, in-play, Draw Their Fire title detection and 1 Force fact |
| V53 undercover-spy scan | `ForceReserveFacts.undercoverSpyCount` | Same owner and undercover state; added in-play gate is neutral because `GameState` clears undercover off table |

V67bl removed the paired-deploy exception. These computed facts now feed diagnostic calculations and
logs only; `pairedDeployPossible` has no score consumer. This packet deletes only comments and does
not authorize removal of those executable diagnostics.

## Explicit exclusions

- Do not delete or rewrite executable `maintObligation`, `interruptReserve`, `spyMoveReserve`,
  `forceReserveNeeded`, `pairedDeployPossible`, or their logs.
- Do not alter V38 solo penalties, objective-flip exceptions, or buddy/reinforcement scoring.
- Do not include V67ai/V67am pull predecessors reserved for Cleanup 2.6.
- Do not combine with Stage 4, finalizer, objective, or engine work.
- Do not deploy or push.

## Required gate

1. Exact diff contains only these two mirrored source edits plus required changelog/history
   bookkeeping.
2. Each source file reports 11 insertions and 55 deletions, net 44 lines removed.
3. The normalized 44-line deletion stream matches the pinned SHA-256 in both bots.
4. Complete source edit streams are identical after package normalization.
5. Every Java source change is a comment; all live facts, scores, and action order remain unchanged.
6. `git diff --check` passes.
7. Parent and candidate affected-module packages succeed in isolated clean worktrees.
8. Parent and candidate `javap -p -c -s -constants` output is identical for both
   `DeployEvaluator.class` files.
9. The complete current expanded focused AI contract suite passes for the candidate.
10. No deployment and no push.
