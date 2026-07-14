# Rando Cleanup Inventory

## Snapshot

Counts and ranges were reconciled through local commit `15b776301`. The ranges below are
marker-pinned to that commit. Re-anchor by marker after every edit; never apply an old literal
line span to a newer tree.

| Class | Before Batch 1.5 | Current |
|---|---:|---:|
| Comment-only corpse pairs | 70 pairs, 3,276 physical lines | 24 pairs, 840 lines |
| Literal-false regions | 28 regions | 6 mirrored regions, 750 lines |
| Source-tree artifacts | 5 files, 12,941 lines | 0 |

## A: Comment-only candidates

Every listed range currently contains comments or blank lines only and has a mirrored range in both
bots.

- `evaluators/ActionTextEvaluator.java`: V169 `175-176`; V61c `236-237`; V116 `420-423`;
  V95 `486-551`; V134 `591-594`; V97 `1105-1166`; V100 `1168-1268`; V192 old take
  dispatch `3595`; V60 transit `4467`; V192 old pull trigger `4515-4518`; V82 pre-guard
  `4550-4582`; V60 reserve risk `4599-4601`; V60 reserve-pull baseline `4918-4922`;
  V82 standalone grant `4956-4962`; V131 downgrade `5152-5155`; V67ai old magnitudes
  `5198-5204`; V67ai old emit `5214-5220`; V67am weapon grant `5361-5367`; V67am device
  grant `5469-5475`; V29.7 old generic pull grant `6331-6333`.
- `evaluators/CardSelectionEvaluator.java`: V190 ground-site starship `1562-1567`; V112
  occupation `7916-7942`; V51 unknown-card occupation `8679-8704`; V51 shield occupation
  `8977-9007`.
- `evaluators/DeployEvaluator.java`: none. Cleanup 2.6 consumed the final two packeted groups.
- `evaluators/MoveEvaluator.java`: none. The stale pre-1.9 hints were consumed by Cleanup 1.9,
  2.0, and 2.1; current source has no remaining commented executable predecessor from that list.

These are inventory candidates, not blanket deletion authority. The independent post-2.8 audit
found no unreserved group with a literal-equivalent owner. Every remaining group is explicitly
held, has a different policy boundary, or preserves PULL/DEPLOY/MOVE/ACTIVATE transaction evidence.
Stop the comment-deletion chain at 24 pairs and 840 physical lines until the owning route fixtures
and cutovers make a new group provably redundant.

The prior inventory contained 20 stale line hints; 13 stale literal spans intersected executable
Java. The corrected `15b776301` ranges above total 420 lines per bot, 840 physical lines. Do not
recover or reuse a superseded range from an older version of this file.

PULL-audit exception: do not yet select ActionText predecessors owned by V116, V95, V97, V100,
V192, V82, V60, V131, V67l/V67ai, V67m/V67am, or V29.7. They remain comment-only, but are held as
transaction rollback/evidence until the parent/child/destination PULL fixtures pass. Re-anchor by
marker, not by the stale line hints above. See `CODEX_PULL_ROUTE_AUDIT_2026-07-13.md`.

Cleanup 1.6 removed the DeckOracle V185 first pass, DeployPhasePlanner V22.3 scan, and both
ShieldStrategy ranges: 194 physical corpse lines across four mirrored pairs. Cleanup 1.7 removed
the DrawEvaluator scan and two PassEvaluator scans: 226 physical corpse lines across three mirrored
pairs. Both commits passed detached normalized-bytecode gates.

Cleanup 1.8 removed the invalid V35.4 ActionText predecessor: 24 physical corpse lines across one
mirrored pair. The earlier V192 dispatch candidate remains held with the PULL rollback evidence.
Gate: `Handoffs/CODEX_CLEANUP18_GATE_21DDA1A67_2026-07-13.md`.

Cleanup 1.9 (`9c9ea3a1c`) removed the three packeted MoveEvaluator predecessor pairs, net 28
physical comment lines. Detached parent/candidate packages, normalized bytecode, mirrored streams,
60 focused tests, and diff-check pass. Gate:
`Handoffs/CODEX_CLEANUP19_GATE_9C9EA3A1C_2026-07-13.md`.

Cleanup 2.0 (`82e4bc6ec`) removed only the mirrored V29 DTF/grabber scan and V27 maintenance
deploy-cost scan from `MoveEvaluator.java`, net 90 comment lines. Exact streams, detached packages,
normalized bytecode, 60 focused tests, and diff-check pass. Gate:
`Handoffs/CODEX_CLEANUP20_GATE_82E4BC6EC_2026-07-13.md`.

Cleanup 2.1 (`c6695168b`) removed 12 mirrored T4.1 predecessor regions from
`MoveEvaluator.java`, net 52 comment lines. Live ladder claims, vetoes, weights, and explanatory
ownership comments remain. Exact streams, detached packages, raw bytecode, 60 focused tests, and
diff-check pass. Gate: `Handoffs/CODEX_CLEANUP21_GATE_C6695168B_2026-07-13.md`.

Cleanup 2.2 (`2a6241edf`) removed only the mirrored two-line commented V60 `+100`
`DeployEvaluator` pull baseline, whose live owner is V192. The source tree shrank by four lines.
The corpse inventory shrank by six because the retained preface line in each bot was rewritten as
current owner documentation and is no longer classified as corpse. Detached packages, mirror
streams, raw bytecode listings, 168 focused tests with one expected F1 skip, and diff-check pass.
Gate: `Handoffs/CODEX_CLEANUP22_GATE_2A6241EDF_2026-07-13.md`.

Cleanup 2.3 (`46e62f4dc`) removed only the mirrored 371-line `DeployEvaluator`
V83/V110/V108/V86/V88/V99 predecessor region, net 742 comment lines. Exact streams, live-owner
hashes, runtime weights, detached packages, raw bytecode listings, 168 focused tests with one
expected F1 skip, and diff-check pass. Gate:
`Handoffs/CODEX_CLEANUP23_GATE_46E62F4DC_2026-07-13.md`.

Cleanup 2.4 (`13db1dfde`) removed four mirrored force-economy predecessor groups, net 104 comment
lines. Exact streams, live owners and weights, detached packages, raw bytecode listings, 168 focused
tests with one expected F1 skip, and diff-check pass. Gate:
`Handoffs/CODEX_CLEANUP24_GATE_13DB1DFDE_2026-07-13.md`.

Cleanup 2.5 (`2fb22ceba`) removed four mirrored V38/V53 diagnostic predecessor groups, net 88
comment lines. Exact streams, live diagnostic owners, detached packages, raw bytecode listings, 168
focused tests with one expected F1 skip, and diff-check pass. Gate:
`Handoffs/CODEX_CLEANUP25_GATE_2FB22CEBA_2026-07-13.md`.

Cleanup 2.6 (`bff87f859`) removed the final two mirrored `DeployEvaluator` predecessor groups,
net 76 comment lines. Exact streams, mirrored sources, detached packages, raw bytecode, 181 focused
tests with one expected F1 skip, and diff-check pass. `DeployEvaluator` now has no remaining
inventory candidate. Gate: `Handoffs/CODEX_CLEANUP26_GATE_BFF87F859_2026-07-13.md`.

Cleanup 2.7 (`0a529f495`) removed the five mirrored V173/V174 maintenance-basis predecessor groups
from `CardSelectionEvaluator`, net 12 comment lines. Exact streams, mirrored sources, detached
packages, raw bytecode, 181 focused tests with one expected F1 skip, and diff-check pass. Gate:
`Handoffs/CODEX_CLEANUP27_GATE_0A529F495_2026-07-13.md`.

Cleanup 2.8 (`15b776301`) removed the mirrored 64-line V140 hand-rolled Battle Plan/Battle Order
waiver predecessor and replaced its stale preface with the current engine-query owner description,
net 128 comment lines. Exact streams, mirrored sources, detached package, raw bytecode, 181 focused
tests with one expected F1 skip, and diff-check pass. The safe deletion chain stops here. Gate:
`Handoffs/CODEX_CLEANUP28_GATE_15B776301_2026-07-13.md`.

Factual repair (`978b58e1d`) corrected source comments only and does not change the inventory count.
Gate: `Handoffs/CODEX_COMMENT_FACT_REPAIR_GATE_978B58E1D_2026-07-13.md`.

## B: Constant-false candidates

These stay held until their replacement owner and route fixtures are explicit:

- `evaluators/CardSelectionEvaluator.java`: 2310-2352, V122
- `evaluators/CardSelectionEvaluator.java`: 3694-3996, V67as
- `strategy/ObjectiveAnalyzer.java`: 1447-1475, V193 hardcoded predecessor

There are two bot copies of each range, producing six physical regions.

## C: Artifacts

Commit `e5b393955` removed five tracked files from the Rando source tree:

- `CardSelectionEvaluator.java.bak`: 2,770 lines
- `CardSelectionEvaluator.java.v13.backup`: 2,451 lines
- `CardSelectionEvaluator.java.v24.11.fix`: 3,437 lines
- `game_log2.txt`: 270 lines
- `game_log_latest.txt`: 4,013 lines

The Java snapshots are valid artifact deletions. The two logs were evidence inputs and their removal
must remain separate from fixture-harness claims. Restore or durably relocate any log still cited by
an active handoff before declaring the evidence chain complete.

## D: Excluded compiled code

Do not delete these from source-call counts alone:

- `strategy/ActionAudit.java`: no non-mirror Java caller found, but external/runtime use is unproven.
- `strategy/ObjectiveType.java`: no non-mirror Java reference found, but serialization or reflection
  use is unproven.
- `chosenone/TheChosenOneAi.java`: no Java constructor caller found; external model loading remains
  unproven.
- `strategy/ObjectiveHandler.java`: instantiated, stored in context, and reset. It is not source-dead.
- `evaluators/MoveEvaluator.java` V79 destination parser: source-reachable alternate-text fallback.
- Rando and ChosenOne fallback routes: source-reachable.

Compiled method or class removal requires source, bytecode, reflection, service/config,
serialization, and route-fixture proof under `CODEX_RANDO_CLEANUP_GATE_2026-07-13.md`.
