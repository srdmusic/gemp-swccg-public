# Cleanup 1.9 Gate: `9c9ea3a1c`

Date: 2026-07-13
Reviewer: Codex/Alfred
Parent: `dde6488e0`
Verdict: source cleanup `ADVANCE`; aggregate deployment `HOLD`

## Scope

The commit changes exactly the two mirrored `MoveEvaluator` files named by
`CODEX_CLEANUP_BATCH19_CANDIDATES_2026-07-13.md` plus the two required history files.

- Rando: 16 comment lines removed, 2 replacement explanation lines added, net 14 lines removed.
- ChosenOne: the identical change.
- Total Java reduction: 28 net physical lines.
- Removed predecessor blocks: V169 local `-400` soft block, V160 old `-9999` cancel-loop block, and
  V79 broken `getAtLocation()` mobile-system lookup.
- Retained live owners: V169 fallthrough warning, V160 `-100000` ladder-veto action/reason/log, and
  V79 `getSystemOrbited()` Scarif detection.

## Independent proof

- `git diff --check 9c9ea3a1c^ 9c9ea3a1c`: pass.
- Commit scope: 4 files, 14 insertions, 32 deletions.
- Rando and ChosenOne deleted Java streams are byte-identical and match the packet's pinned baseline
  SHA-256: `9381e9f08350f9d75f08ce4175a21fb55b2e083b40a2f532c5639fb4f36b2c52`.
- Rando and ChosenOne replacement comment streams are byte-identical:
  `e4bb64aa63c8045a14e77878ad6df705de5baa01dd8e5aca6f02adc07a0257c2`.
- Detached parent and candidate affected-module packages pass under Homebrew OpenJDK 25.0.2:
  `mvn -q -pl gemp-swccg-server -am package -DskipTests`.
- Normalized `javap -p -c -s -constants` hashes are equal parent/candidate:
  - Rando: `d61ff31fffad20525647f4a0713dfff998e569c6f8cb02a8acc1cc3bb42ab93a`.
  - ChosenOne: `18894a2fa1a883602ebc781cbb1da82b7d7af89708072b101beb35a8bd9bfc6f`.
- Independent current-HEAD focused suite: 60 passed, 0 failures, 0 errors, 0 skipped.
  - `FormationSafetyCountTest`: 6.
  - `DockingBayTransitTests`: 5.
  - Rando and ChosenOne `DeckOraclePullTargetParseTest`: 11 each.
  - Rando and ChosenOne `CombinedEvaluatorTieTest`: 4 each.
  - `DecisionTraceEnvelopeTest`: 19.
- No push and no deployment.

## Boundary

Cleanup 1.9 may remain on the branch as behavior-neutral source reduction. It does not authorize
MOVE policy changes, phase-owner cutover, trace enablement, removal of any remaining MOVE
predecessor, aggregate deployment, or legacy retirement. Runtime behavior is unchanged because both
affected compiled instruction and constant surfaces are identical to the parent.

