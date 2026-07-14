# Codex gate: domain registry rewrite `631ed4c13`

Date: 2026-07-13  
Commit: `631ed4c135f50a15034b5895e9501ad59af720f3`  
Parent: `224ba9423`  
Scope: read-only registry/source verification. No Java edit and no deploy.

## Verdict

- **ADVANCE** the corrected 21-arm ownership facts.
- **HOLD** the document's claim to be a complete migration authority.
- This hold blocks only migration or retirement that relies on the aggregate inventory. It does not block K2's cleanup, trace, B2, fixture, or deploy-contract work.

The rewrite resolves the prior gate findings. V172 solo-dominance is correctly recorded as a live `+600` score, FS enforcement is correctly owned by loop-safety/SVC-SAFETY rather than FormationSafety, and the stale section 5 text is gone. The remaining defect is narrower: three known live V27 arms are acknowledged only in prose and have no first-class registry rows.

## Passed checks

1. Commit scope
   - Exactly one file changed: `resources/DOMAIN_REGISTRY_2026-07-12.md`.
   - Diff size: `+95/-64`.
   - `git diff --check 224ba9423..631ed4c13` passed.

2. Corrected exact-arm table
   - Section 5 contains exactly 21 data rows: 20 LIVE and 1 INERT.
   - Every row carries route, anchor, producers, target owner, kind, magnitude, marker, status, and fixture state.
   - All listed stable markers were counted at commit `631ed4c13` in both bot mirrors. Each listed marker is `1` per mirror except the explicitly shared V156 marker (`1` total), the four FS markers (`1` in their named mirror file), and markers whose row explicitly states multiple literals.
   - The V172 solo-dominance marker is `1` per mirror and the source arm scores `+600`.
   - The four FS-enforcement markers are each `1` per mirror in `EvaluatedAction` or `CombinedEvaluator` as documented.

3. Internal arithmetic
   - The 22 section-2 domain headers sum to 364.
   - The section-7 per-domain list also sums to 364.
   - Section 5 correctly distinguishes its 20 LIVE rows from the one INERT V37.4 sibling.

4. Stale contradiction removal
   - No surviving statement says all V172 arms award no siting points.
   - No surviving statement owns FS-enforcement under solo-formation.
   - The old 13-row/unchanged-table language is gone.

## Blocking finding

The registry defines one rule as one V-tag arm and calls itself the authoritative LIVE inventory. Lines 644 and 649 nevertheless acknowledge three live V27 arms that remain prose-only:

| Missing arm | Live source at `631ed4c13` | Contribution | Marker count | Required owner |
|---|---|---|---|---|
| `V27-maintenance-pass` | `PassEvaluator.java:223-240` | `+25/+50` on Pass | `"V27 MAINTENANCE RESERVE"` x1/bot | force-budget / pass route |
| `V27-maintenance-move` | `MoveEvaluator.java:1968-2004` | `-80` on Move | `"V27 MAINTENANCE MOVE BLOCK"` x1/bot | force-budget / move route |
| `V27-buddy-protect` | `MoveEvaluator.java:880-925` | `-150/-250/-400` on Move | `"V27 BUDDY PROTECT"` x2/bot | solo-formation / move route |

All three blocks are live, have action-score contributions, and are mirrored. None has a matching first-class row in section 2 or section 5. A prose pointer is not sufficient for arm-by-arm migration, fixture gating, or retirement.

## Required amendment

Mint the three rows with the same completion fields as the 21-arm table, then regenerate:

- live total: `364 -> 367`, unless K2 identifies and documents an existing row each arm replaces;
- force-budget: `9 -> 11`;
- solo-formation: `14 -> 15`;
- per-file counts: `PE +1`, `ME +2`;
- solo-formation owner files: add `ME`;
- multi-domain base tags: add V27 (`force-budget`, `solo-formation`) and update the count;
- stable-marker wording: 23 live exact arms will then be verified, while the remaining unverified-marker count stays 344;
- fixtures: name and freeze one parity fixture per new arm before migration or retirement.

After that amendment, rerun the table-row count, domain sum, per-file sum, marker count, and stale-text scans. Aggregate deployment remains separately held.

## Coordination

Finding sent to K2 as mailbox message `m00288`. K2 was explicitly told to continue all independent work lanes while this narrow amendment is gated.
