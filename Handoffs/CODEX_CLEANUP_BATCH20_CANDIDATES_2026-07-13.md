# Codex cleanup 2.0 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Baseline: `9c9ea3a1c`
Scope: two comments-only predecessor scans in mirrored `MoveEvaluator.java` files

## Verdict

`ADVANCE` this exact comments-only packet for implementation. MOVE policy, scoring, route
ownership, and runtime cutover remain `HOLD` under `CODEX_MOVE_ROUTE_AUDIT_2026-07-13.md`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/MoveEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/MoveEvaluator.java`

Apply the same edit to both bot files. Recheck that neither file is dirty before editing.

## Exact allowed edits

| Marker | Current baseline lines | Allowed edit | Net per bot |
|---|---:|---|---:|
| V29 old DTF/grabber permanent-card scan | `731-756` | Delete all 26 commented lines. Replace the six-line T2 preface at `723-728` with four comment lines stating that the shared cache preserves in-play-gated DTF detection and any-unused-grabber semantics, and that V29 weights remain unchanged. | `-28` |
| V27 old maintenance deploy-cost scan | `1964-1977` | Delete all 14 commented lines. Replace the seven-line T2 preface at `1956-1962` with four comment lines stating that the shared cache is authoritative, this site formerly used deploy cost, the consolidated `MaintenanceFacts` engine basis is intentional, and the `-80` weight remains unchanged. | `-17` |

Expected net deletion: 45 physical lines per bot, 90 total.

Re-anchor by exact markers and commented statements. Line numbers are baseline hints only.

## Mirror proof

The two deletion streams have identical SHA-256 in Rando and ChosenOne:

```text
V29 DTF/grabber scan: 0b26005c8a6733aa9e2d5e59afa283674b4602e5f80cb4eb2955416f0831c64a
V27 maintenance scan: f6a3da797c1f67cf7e213764de0d119781f4047756c2994df0f03709b5ca7577
```

## Replacement-owner proof

`ForceReserveService.compute()` is the live owner for both scans:

- It gates permanent cards to in-play zones before deriving facts.
- `dtfActive` requires exact opponent ownership and a Draw Their Fire title match.
- `grabberUnused` is true when any friendly in-play grabber has no stacked card.
- `maintenanceObligation` sums `MaintenanceFacts.maintainCost()` for friendly in-play
  maintenance cards.
- `DecisionContext.getForceReserveFacts()` caches those facts once per decision for both bots.

The old V27 scan used deploy cost. That predecessor is not the intended behavior authority. The T2
consolidation deliberately standardized this site on `MaintenanceFacts.maintainCost()`, as recorded
in `ForceReserveService` and the retained local preface. Git and the verified local backup preserve
the historical implementation.

## Explicit exclusions

- Do not alter live Java statements.
- Do not retune `-100`, `-150`, `-60`, `-80`, or pass thresholds.
- Do not alter critical-interrupt detection.
- Do not change `ForceReserveService`, `DecisionContext`, or MOVE route ownership.
- Do not fold other MoveEvaluator residue into this commit.
- Do not edit Rando without the identical ChosenOne change.
- Do not deploy.

## Required gate

1. Exact diff contains only the two mirrored files plus changelog/history bookkeeping.
2. Exact source diff matches this packet and reports 45 net lines per bot.
3. Rando/ChosenOne deletion and replacement-comment streams are identical.
4. `git diff --check` passes.
5. Affected-module package succeeds in a clean detached worktree.
6. Parent and candidate normalized `javap -p -c -s -constants` are identical for both
   `MoveEvaluator.class` files.
7. Existing focused MOVE, formation, force-reserve, and trace fixtures pass in both bots.
8. No deployment.

Normalized bytecode equality is the behavior-neutral proof. Raw class hashes alone are not
sufficient because source-line metadata changes.
