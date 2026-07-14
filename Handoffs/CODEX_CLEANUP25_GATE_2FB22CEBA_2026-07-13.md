# Cleanup 2.5 gate for 2fb22ceba

Date: 2026-07-13
Commit: `2fb22ceba2d4ef3592ed91231c1dc6d4ba41d7c1`
Parent: `13db1dfde8f4e33602607df494ddf23dcfbd2207`
Verdict: `ADVANCE` as behavior-neutral comments-only cleanup

## Independent proof

- Exact scope: mirrored Rando and ChosenOne `DeployEvaluator.java` files plus required
  changelog/history bookkeeping.
- Per bot source diff: 11 insertions, 55 deletions, net 44 physical lines removed. Net source
  reduction: 88 lines.
- Each bot's normalized 44-line V38/V53 predecessor stream matches SHA-256
  `65a1987dfeebd6b6639648f2cdbc28d5fcd8eed09aae6e17c2440d742a0b87d9`.
- The Rando-then-ChosenOne concatenation matches SHA-256
  `5bb4259f71064ee52ca66ef39dca636a50440f875444cbd42583a07270c0daef`.
- All four individual deletion ranges independently match their packet pins:
  `246f34e6566d02fc30c51de6227b0b91af27011f305b74cf90c7dcedf3a0ebe3`,
  `26470e9d5ed81e47c4571b010ce8a9b6e982dbe2523d3e1e719dc4031ce6898b`,
  `f5d181688f3316d32143643461bce3135aa3ae41d013826801b1e8d1ab30a817`, and
  `9dcdb7b8f75da38923d2527100ad558785648c4ad3eec9c8da74717d55960f97`.
- Complete source edit streams are mirror-identical. Their SHA-256 is
  `814d4ebcd5c69a8a42e87f41e9c9efd78ea116f0dd191586d79224eb3c3b5bc0`.
- Every evaluator source change is a line comment. No Java statement, score, guard, predicate,
  action order, or log call changed.
- Live successor and diagnostic boundary remains intact:
  `maintenanceObligation`, `dtfActive`, `undercoverSpyCount`,
  `MaintenanceFacts.maintainCost(...)`, `forceReserveNeeded`, `pairedDeployPossible`, and the live
  V53 diagnostic log. `pairedDeployPossible` remains executable but has no score consumer.
- `git diff --check 13db1dfde 2fb22ceba`: pass.
- Detached parent and candidate affected-module packages pass under Homebrew OpenJDK 25.0.2 from
  `src/`: `mvn -q -pl gemp-swccg-server -am -DskipTests package`.
- Parent/candidate `javap -p -c -s -constants` output is byte-for-byte identical at 13,704 lines
  per bot:
  - Rando: `0e0794df649076445a0a52c6cf2dcb2c707d33730e7209931eac35fbe86a47e5`.
  - ChosenOne: `d4e91d1142b2b469cacdbc098e111591dd9c63a915053833e15a953d4d612fbd`.
- Candidate expanded focused suite: 168 tests, 0 failures, 0 errors, 1 expected skip.
  The skip remains `EngineAwaitingDecisionContractTest.fcMultipleChoiceBounds_checkedAfterF1`.
- Changelog/history accurately describe the exact source reduction and no-deploy state. K-2's
  narrower pre-commit fixture run was 60/60.
- Temporary gate worktrees were removed. No push and no deployment.

## Boundary

Cleanup 2.5 may remain on the branch. It does not authorize Cleanup 2.6 or later Java by itself,
Stage 4 capture enablement, finalizer or engine work, phase cutover, aggregate deployment, or push.
