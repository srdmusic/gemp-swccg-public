# Cleanup 2.4 gate for 13db1dfde

Date: 2026-07-13
Commit: `13db1dfde8f4e33602607df494ddf23dcfbd2207`
Parent: `46e62f4dcc21100fcce7059fbe637d2a4c9b0180`
Verdict: `ADVANCE` as behavior-neutral comments-only cleanup

## Independent proof

- Exact scope: mirrored Rando and ChosenOne `DeployEvaluator.java` files plus required
  changelog/history bookkeeping.
- Per bot source diff: 5 insertions, 57 deletions, net 52 physical lines removed. Net source
  reduction: 104 lines.
- Each bot's normalized 52-line V22.3/V59, V24.5, and V29.13 predecessor stream matches SHA-256
  `9ceb4248600af21123eef3028b4acd47de01b951ee50328c403a37317d4c69f7`.
- The actual Rando-then-ChosenOne concatenation matches SHA-256
  `ef4f13d65f267a0490162b49ed296297cc82cfa83ad404ecbce58c3b6a2e72f1`.
  The candidate packet was corrected to distinguish the per-bot pin from the concatenated pin.
- All four individual deletion ranges independently match their packet pins:
  `5176097c345f0e2bf58062263607c3ab8624a6b07c8d8a0b4eec5a9da363bcd3`,
  `eadacd8f9e8ca4c3049f34629ce4d56e9b18875c368a5f4cb8a183344728fa77`,
  `fa40d93b3a93ad83571853fc1325c4841218c1a080743820e3fe648ac1527210`, and
  `771e218444bb9e82b61aa5909215f2490f637881da4fea3a3494b557fe38276c`.
- Complete source edit streams are mirror-identical. Their SHA-256 is
  `17b2926724527dda21c524cb8f8d52ad811a175272054da9810289c47791ef48`.
- Every evaluator source change is a line comment. No Java statement, score, guard, predicate,
  action order, or log call changed.
- Live owners remain intact: `MaintenanceFacts.maintainCost(...)`,
  `maintenanceObligation`, `dtfActive`, `grabberUnused`, the maintenance icon gates, and the live
  force-reserve conditions.
- Live weights remain intact: V59/V64 `-2000/-1500/-400`, V24.5 `-50/-50`, V29.13
  `-50/-500/-30`.
- `git diff --check 46e62f4dc 13db1dfde`: pass.
- Detached parent and candidate affected-module packages pass under Homebrew OpenJDK 25.0.2 from
  `src/`: `mvn -q -pl gemp-swccg-server -am -DskipTests package`.
- Parent/candidate `javap -p -c -s -constants` output is byte-for-byte identical:
  - Rando: `0e0794df649076445a0a52c6cf2dcb2c707d33730e7209931eac35fbe86a47e5`.
  - ChosenOne: `d4e91d1142b2b469cacdbc098e111591dd9c63a915053833e15a953d4d612fbd`.
- Candidate expanded focused suite: 168 tests, 0 failures, 0 errors, 1 expected skip.
  The skip remains `EngineAwaitingDecisionContractTest.fcMultipleChoiceBounds_checkedAfterF1`.
- Changelog/history accurately describe the exact source reduction and no-deploy state. K-2's
  narrower pre-commit fixture run was 60/60.
- Temporary gate worktrees were removed. No push and no deployment.

## Boundary

Cleanup 2.4 may remain on the branch. It does not authorize Cleanup 2.5 or later Java by itself,
Stage 4 capture enablement, finalizer or engine work, phase cutover, aggregate deployment, or push.
