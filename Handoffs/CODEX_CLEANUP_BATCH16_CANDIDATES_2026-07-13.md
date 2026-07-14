# Cleanup Batch 1.6 Candidate Packet

Date: 2026-07-13
Owner: Codex/Alfred
Implementer: K-2/Claude
Status: source-audited comment-only candidates; implementation not started

## Scope

This is a deliberately small mirrored cleanup slice after `66cf11e18`. It removes 194 physical
comment-only corpse lines across six files while preserving the surviving rationale next to the
live owner.

| File per bot | Delete | Surviving comment update |
|---|---:|---|
| `strategy/DeckOracle.java` | 43 lines | Remove the section header claim that the dead first pass must remain. |
| `strategy/DeployPhasePlanner.java` | 26 lines | Describe `ForceReserveService` as the live sole scan without referring to a block below. |
| `strategy/ShieldStrategy.java` | 28 lines | Describe the live unified predicates without referring to commented variables above. |

Apply identically to Rando and ChosenOne. No imports should change from these deletions.

## Stable anchors

### DeckOracle V185 first pass

- Delete from `// === V185 first pass ... SUPERSEDED` through the commented
  `reserveTargetsAreAllWeapons` closing brace, immediately before live
  `reserveTargetsAreAllUnattachableWeapons`.
- Current Rando lines: `422-464`.
- Update the section banner at current lines `24-25` so it no longer says the dead revert block is
  retained in source.
- The live owner and rationale remain in the Javadoc beginning at current line `466`.

### DeployPhasePlanner V22.3 inline maintenance scan

- Delete the fully commented `int maintenanceReserve = 0` through its commented catch block,
  immediately after the live `ForceReserveService.compute` catch.
- Current Rando lines: `232-257`.
- Rewrite the surviving current lines `216-221` to state that the shared service replaces the
  incorrect allCards guard and deploy-cost basis, without saying the old loop remains below.
- Live `maintenanceReserve` and `effectiveForce` statements remain unchanged.

### ShieldStrategy old power-based occupation scan

- Delete from `// --- SUPERSEDED ... old power-based theater scan` through the commented
  `triggerA` assignment.
- Delete the single commented `weOccupyAnyBg = ...` line after the live
  `ShieldFacts.occupiesAnyBattleground` call.
- Current Rando lines: `485-511` and `525`.
- Update the surviving current lines `635-636` to refer to the unified predicates directly, without
  saying old variables are commented above.
- Keep the divergence note at current lines `526-529`; it documents live intentional power-based
  behavior for the other counts.

## Source proof

- Every deletion range contains only `//` comments and blank lines.
- Rando and ChosenOne ranges are structurally mirrored. DeckOracle has one unrelated later comment
  wording difference outside this slice; do not normalize it as part of cleanup.
- All live calls, variables, conditions, returns, logs, and service ownership remain outside the
  deletion ranges.

## Required gate

1. One dedicated commit containing only the six Java files plus changelog/history entries.
2. `git diff --check` clean and normalized bot diffs identical for the deletion hunks.
3. Same-JDK isolated pre/post compile.
4. Normalized `javap -p -c -s -constants` equality for all six classes.
5. Existing DeckOracle pull parser, maintenance reserve, and shield-strategy focused tests pass.
6. No V191 candidate/contribution/raw-bit/veto/winner/final-response delta on covered fixtures.

This packet does not authorize deleting the V122, V67as, or ObjectiveAnalyzer V193 constant-false
regions. Those remain semantic retirement work.
