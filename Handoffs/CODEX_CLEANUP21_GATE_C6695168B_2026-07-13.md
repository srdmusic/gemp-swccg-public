# Cleanup 2.1 gate for c6695168b

Date: 2026-07-13
Commit: `c6695168be29225e14f13eab36bc88314741d141`
Parent: `82e4bc6ecba31ac989beb6224496eaa980590583`
Verdict: `ADVANCE` as behavior-neutral comments-only cleanup

## Independent proof

- Exact scope: mirrored Rando and ChosenOne `MoveEvaluator.java` files plus changelog/history.
- Per bot source diff: 2 insertions, 28 deletions, net 26 lines removed. Net 52 lines total.
- The 26-line packet deletion stream is mirror-identical and matches the packet SHA-256:
  `08324eddfeb9013a05d9a453d1afe71ded2baa2b1907f131c08e681d7fa98a6d`.
- The complete 28-line Git deletion stream, including the two rewritten prefaces, is
  mirror-identical:
  `d9c9a7003047e5192f92ab4f53f124adf24d88e67c324ab6376548bc4e0929c0`.
- The two replacement preface lines are mirror-identical:
  `dbbca54b89df4d4a562af8f55a6f97e84b2ac36d9a8aa00709323878b9eee6e2`.
- Every evaluator source change is a comment or blank line. No Java statement changed.
- `git diff --check c6695168b^ c6695168b`: pass.
- Detached parent and candidate affected-module packages pass under Homebrew OpenJDK 25.0.2.
- Parent/candidate raw `javap -p -c -s -constants` hashes are identical:
  - Rando: `d61ff31fffad20525647f4a0713dfff998e569c6f8cb02a8acc1cc3bb42ab93a`.
  - ChosenOne: `18894a2fa1a883602ebc781cbb1da82b7d7af89708072b101beb35a8bd9bfc6f`.
- Candidate focused suite: 60 tests, 0 failures, 0 errors, 0 skipped.
  - `FormationSafetyCountTest`: 6.
  - `DockingBayTransitTests`: 5.
  - Rando and ChosenOne `DeckOraclePullTargetParseTest`: 11 each.
  - Rando and ChosenOne `CombinedEvaluatorTieTest`: 4 each.
  - `DecisionTraceEnvelopeTest`: 19.
- Changelog/history accurately describe the exact arithmetic, gate, and no-deploy state.
- No push and no deployment.

## Live-owner proof

All 12 deleted predecessor regions retain adjacent live owners:

- V47, V135, V60, V38.3 Castle, and V49 retain ladder hard vetoes.
- V137 retains `MovePredicates.canWinAt(...)`, its veto flag, and R1 deterrent.
- V53b retains its R4 transit claim plus `+800` fine.
- V37.1 CRUSH/FAVORABLE retain `-1500` R1 weights and fall-through.
- V85 retains its `-800` R1 weight and fall-through.
- V38.3 wrong-direction retains the deferred veto and transit carve-out.
- The default `-50` MOVE tax remains in `ladderFinalize()` for R1 only.

## Boundary

Cleanup 2.1 may remain on the branch. It does not authorize MOVE policy changes, finalizer or phase
cutover, trace capture, further residue deletion, aggregate deployment, or push.
