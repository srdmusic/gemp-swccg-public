# Post-Cleanup 2.8 stop boundary

Date: 2026-07-13
Audit base: `15b776301`
Reserved commits: Cleanup 2.7 `0a529f495` and Cleanup 2.8 `15b776301`, both gated `ADVANCE`
Final factual repair: `978b58e1d`, gated `ADVANCE`
Verdict after reserved packets: `HOLD` further comment deletion

## Result

An independent marker-anchored audit found no additional unreserved comment group with a
literal-equivalent live owner. At Cleanup 2.8, the remaining inventory is 24 mirrored
pairs and 840 physical lines. Those lines are comments, but they are not all behavior-neutral
retirement candidates.

The safe numbered cleanup chain stops after Cleanup 2.8 plus the separate factual-comment repair.
Further deletion requires the owning route fixture, shadow comparison, and cutover proof named in
the phase audits.

## Smallest held examples

### V169 endangered mover

- Old predecessor: indefinite `-400` soft block.
- Live owner: `-250` for three retries in an action/turn, then `-100000`.
- Verdict: `HOLD`. The old and live policies are not equivalent.
- Required owner proof: MOVE retry/abandon fixtures and the MOVE retirement boundary.

### V61c activation reserve

- Old predecessor: preserve Reserve whenever the pile is at most 3.
- Live owners: preserve it only when battle is plausible; otherwise V168 can prefer activation.
- Verdict: `HOLD`. ActionText, confirmation, and ForceActivation consumers share unresolved policy.
- Required owner proof: ACTIVATE route fixtures and one shared battle-plausibility owner.

## Held families

- PULL transaction evidence: V116, V95, V97, V100, V192, V82, V60, V131, V67ai,
  V67am, and V29.7 predecessors.
- MOVE policy evidence: V169 and V60 transit predecessors.
- ACTIVATE policy evidence: V61c and related activation predecessors.
- DEPLOY/occupation rollback evidence: V190, V112, and V51 CardSelection predecessors.
- Any predecessor whose score, retry boundary, source classifier, or route gate differs from the
  current owner.

## Reopen rule

Reopen one group at a time only when all of these are true:

1. A named live owner covers the same typed trigger and candidate scope.
2. Boundary math proves equal or intentionally dominated behavior at edge cases.
3. Parent/child/destination or phase-window fixtures pass for the owning route.
4. The old block is no longer needed as rollback evidence for an uncut transaction.
5. Both bots receive one mirrored, hash-pinned, comments-only commit.
6. Detached package, normalized bytecode, focused fixtures, and `git diff --check` pass.

This stop is not an argument for keeping old residue forever. It is the proof boundary between
comment cleanup and executable owner cutover.
