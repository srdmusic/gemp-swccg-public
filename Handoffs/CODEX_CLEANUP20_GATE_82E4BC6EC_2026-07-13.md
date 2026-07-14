# Cleanup 2.0 gate for 82e4bc6ec

Date: 2026-07-13
Commit: `82e4bc6ecba31ac989beb6224496eaa980590583`
Parent: `92965934b`
Verdict: `ADVANCE` as behavior-neutral comments-only cleanup

## Independent proof

- Exact scope: mirrored Rando and ChosenOne `MoveEvaluator.java` files plus changelog/history.
- Per bot source diff: 8 insertions, 53 deletions, net 45 lines removed. Net 90 lines total.
- The two parent deletion streams match the packet and are mirror-identical:
  - V29 DTF/grabber scan:
    `0b26005c8a6733aa9e2d5e59afa283674b4602e5f80cb4eb2955416f0831c64a`
  - V27 maintenance scan:
    `f6a3da797c1f67cf7e213764de0d119781f4047756c2994df0f03709b5ca7577`
- Replacement comment streams are mirror-identical:
  `5e89847bd4a4b175a74adb223728f2c1981efe834d36d92a144d34cf725aa04c`.
- `git diff --check 92965934b 82e4bc6ec`: pass.
- Detached parent and candidate affected-module packages pass under Homebrew OpenJDK 25.0.2.
- Parent/candidate normalized `javap -p -c -s -constants` hashes are identical:
  - Rando: `d61ff31fffad20525647f4a0713dfff998e569c6f8cb02a8acc1cc3bb42ab93a`.
  - ChosenOne: `18894a2fa1a883602ebc781cbb1da82b7d7af89708072b101beb35a8bd9bfc6f`.
- Candidate focused suite: 60 tests, 0 failures, 0 errors, 0 skipped.
  - `FormationSafetyCountTest`: 6.
  - `DockingBayTransitTests`: 5.
  - Rando and ChosenOne `DeckOraclePullTargetParseTest`: 11 each.
  - Rando and ChosenOne `CombinedEvaluatorTieTest`: 4 each.
  - `DecisionTraceEnvelopeTest`: 19.
- No push and no deployment.

## Live-owner proof

`DecisionContext.getForceReserveFacts()` remains the live read in both blocks.
`ForceReserveService.compute()` preserves in-play-gated DTF detection and any-unused-grabber
semantics. Its `maintenanceObligation` intentionally uses the consolidated
`MaintenanceFacts.maintainCost()` basis. V29 `-100/-150/-60` and V27 `-80` weights are unchanged.

## Boundary

Cleanup 2.0 may remain on the branch. It does not authorize MOVE policy changes, finalizer or phase
cutover, further residue deletion, trace capture, or aggregate deployment.
