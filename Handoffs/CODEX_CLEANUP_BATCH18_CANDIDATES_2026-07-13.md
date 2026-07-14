# Cleanup Batch 1.8 Candidate Packet

Date: 2026-07-13
Owner: Codex/Alfred
Implementer: K-2/Claude
Status: source-audited comment-only candidates; implementation not started

## Scope

Remove one small invalid comment-only predecessor from the mirrored `ActionTextEvaluator` files. No
condition, score, mutation, log statement, dispatch call, or import changes.

| File per bot | Corpse lines | Surviving comment update |
|---|---:|---|
| `evaluators/ActionTextEvaluator.java` | 12 | None. |

Apply identically to Rando and ChosenOne. Total removal: 12 lines per bot, 24 physical corpse
lines.

## Stable Anchors

### V35.4 ownership-inverted undercover detection

- Current mirrored lines: `3813-3824`.
- Delete the complete comment-only predecessor from `// OLD ownership-inverted detection...`
  through its commented closing brace.
- Keep the live owner/opponent scan at current lines `3799-3812`, the blocked-drain condition, the
  `+150/+250` scoring, logging, and `break` byte-for-byte unchanged.

## Source Proof

- Every deletion line begins with `//`; there are no executable tokens inside either range.
- The two source ranges are line-for-line identical between Rando and ChosenOne.
- The live replacement is immediately adjacent and source-reachable.
- The V35.4 live scan checks the opponent owner before `card.isUndercover()`. The deleted predecessor
  inverted ownership and is not a valid rollback implementation.
- No import can become unused because both predecessors are already comments.

## Required Gate

1. One dedicated commit containing only the two mirrored Java files plus changelog/history entries.
2. Automated changed-line scan proves every Java delta is comment or blank only.
3. `git diff --check` clean and normalized Rando/ChosenOne Java hunks identical.
4. Same-JDK detached parent/candidate affected-module builds.
5. Normalized `javap -p -c -s -constants` equality for both affected classes.
6. Frozen V35.4 movement decisions preserve candidate order, contribution sequence, raw score
   bits, winner, and final response.

This packet authorizes no executable owner retirement, phase cutover, capture enablement, or
deployment. K-2 may implement it independently of Trace 2b because the Java delta is comment-only.

The previously proposed V192 old take-dispatch line is explicitly withdrawn from this packet after
the PULL audit. All absorbed V192 predecessors remain held until the full parent/child/destination
transaction fixtures pass.
