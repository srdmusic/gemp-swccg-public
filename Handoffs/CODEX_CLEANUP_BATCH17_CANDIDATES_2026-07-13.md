# Cleanup Batch 1.7 Candidate Packet

Date: 2026-07-13
Owner: Codex/Alfred
Implementer: K-2/Claude
Status: source-audited comment-only candidates; implementation not started

## Scope

Remove 226 physical comment-only corpse lines from the mirrored DRAW and PASS evaluators. The live
`ForceReserveService` calls and every scoring statement remain unchanged.

| File per bot | Delete | Surviving comment update |
|---|---:|---|
| `evaluators/DrawEvaluator.java` | 78 lines | Describe the shared service as the live owner without referring to a block below. |
| `evaluators/PassEvaluator.java` | 35 lines | Remove two stale references to old inline scans below. |

Apply identically to Rando and ChosenOne. Total: 113 lines per bot, 226 physical lines.

## Stable anchors

### DrawEvaluator shared reserve scan

- Delete from the commented `// Scan ALL permanent cards for opponent's DTF / First Strike` through
  the commented closing brace immediately before live `if (opponentHasDTF)`.
- Current Rando and ChosenOne lines: `541-618`.
- Rewrite the live-owner comment at current lines `525-533` to state that
  `context.getForceReserveFacts()` supplies DTF, First Strike, IAO, maintenance, and Verge facts.
  Remove only the claim that the old inline scan remains below.
- Keep the live assignments at current lines `534-540` and all reserve arithmetic beginning at
  current line `620` byte-for-byte unchanged.

### PassEvaluator DTF scan

- Delete the commented `String opponentIdDtf` through the commented closing brace immediately
  before live `if (dtfActive)`.
- Current Rando and ChosenOne lines: `197-213`.
- Rewrite current lines `191-195` to state that DTF comes from the cached shared service with the
  opponent-owner and in-play gates. Remove the stale `commented out below` sentence.
- Keep the live `dtfActive` assignment and V27.1 weights unchanged.

### PassEvaluator maintenance scan

- Delete the commented `int maintenanceCostTotal = 0` through the commented closing brace
  immediately before live `if (maintenanceCostTotal > 0 ...)`.
- Current Rando and ChosenOne lines: `248-265`.
- Rewrite current lines `242-246` to state that the shared service supplies the typed
  `MaintenanceFacts` obligation. Remove the stale `inline scan below` sentence.
- Keep the live `maintenanceCostTotal` assignment and V27 weights unchanged.

## Source proof

- Every deletion range contains only `//` comments and blank lines.
- The Rando and ChosenOne blocks are line-for-line mirrors apart from package names outside the
  deletion ranges.
- Live service calls, boolean assignments, reserve arithmetic, caps, Hidden Path transit reserve,
  pass weights, conditions, returns, and logs are outside the deletion ranges.
- No import should become unused because the old bodies are already comments.

## Required gate

1. One dedicated commit containing only four Java files plus changelog/history entries.
2. `git diff --check` clean and normalized bot deletion hunks identical.
3. Same-JDK isolated pre/post affected-module compile.
4. Normalized `javap -p -c -s -constants` equality for all four classes.
5. V191 trace parity on frozen DRAW and PASS decisions: candidate order, contribution sequence,
   raw score bits, vetoes, winner, and final response unchanged.
6. Rando/ChosenOne fixture parity and aggregate affected-module package pass.

This packet authorizes no semantic owner retirement and no deployment. It may land while another
phase migration is held because it contains no executable Java.
