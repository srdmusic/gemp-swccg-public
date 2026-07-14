# Codex cleanup 2.4 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Baseline: `46e62f4dc`
Scope: mirrored force-economy predecessors in `DeployEvaluator.java`

## Verdict

`ADVANCE` this exact comments-only packet. Executable force-economy logic, weights, Stage 4 Java,
deployment, and push remain `HOLD`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/DeployEvaluator.java`

Apply the identical edit to both files. Both files are clean at the frozen baseline.

## Exact allowed deletions

Delete only these four comment-only predecessor ranges. Line numbers are baseline hints; re-anchor
by the quoted statements and surrounding live owner.

1. The single commented V22.3/V59 statement at current line 1811:

```java
// int maintenanceCost = cost;  // superseded T2 COMMIT-1 2026-07-06 ("maintenance cost = deploy cost" refuted)
```

2. The complete 19-line commented V24.5 inline maintenance scan at current lines 1896-1914,
   beginning with `// int existingMaintenanceCost = 0;` and ending with its commented closing brace.
3. The single commented V29.13 condition at current line 2038:

```java
// if (thisCardHasMaint && forceAfterThisDeploy < cost) {  // superseded T2 COMMIT-1 2026-07-06 (deploy-cost basis)
```

4. The complete 31-line commented V29.13 DTF/grabber scans at current lines 2066-2096,
   beginning with `// String dtfOpponentId =` and ending with the grabber scan's commented closing
   brace.

The four normalized deletion streams total 52 lines per bot. Each bot's byte-identical 52-line
stream has SHA-256:

```text
9ceb4248600af21123eef3028b4acd47de01b951ee50328c403a37317d4c69f7
```

The Rando stream followed by the ChosenOne stream has SHA-256:

```text
ef4f13d65f267a0490162b49ed296297cc82cfa83ad404ecbce58c3b6a2e72f1
```

Individual normalized SHA-256 values are:

- V22.3/V59 one-line predecessor: `5176097c345f0e2bf58062263607c3ab8624a6b07c8d8a0b4eec5a9da363bcd3`.
- V24.5 inline maintenance scan: `eadacd8f9e8ca4c3049f34629ce4d56e9b18875c368a5f4cb8a183344728fa77`.
- V29.13 one-line maintenance threshold: `fa40d93b3a93ad83571853fc1325c4841218c1a080743820e3fe648ac1527210`.
- V29.13 DTF/grabber scans: `771e218444bb9e82b61aa5909215f2490f637881da4fea3a3494b557fe38276c`.

## Exact preface rewrites

Keep the first two live-owner preface lines above `existingMaintenanceCost`. Replace only the
current final three lines with:

```java
// owner, in-play, maintenance-icon, and MaintenanceFacts guards).
// V24.5 weights (-50/-50) are unchanged. Superseded inline scan
// removed 2026-07-13; git preserves it.
```

Keep the first five live-owner preface lines above `dtfOnTable`. Replace only the current final two
lines with:

```java
// fields 2+ grabbers with mixed state. Superseded inline scans were
// removed 2026-07-13; the -30 weight is unchanged; git preserves them.
```

Expected source diff per bot: 5 insertions, 57 deletions, net 52 physical lines removed. Total
source reduction: 104 lines.

## Replacement-owner proof

| Predecessor | Live owner | Boundary retained |
|---|---|---|
| V22.3/V59 `maintenanceCost = cost` | `MaintenanceFacts.maintainCost(blueprint)` immediately below | Maintenance icon gate and `-2000/-1500/-400` tiers remain; corrected engine maintain cost stays authoritative |
| V24.5 inline maintenance sum | `context.getForceReserveFacts().maintenanceObligation` | Same owner, in-play, maintenance-icon, and `MaintenanceFacts` guards; `-50/-50` unchanged |
| V29.13 compare to deploy cost | `MaintenanceFacts.maintainCost(blueprint)` and the live condition immediately below | Maintenance-only gate and `-50/-500` unchanged; corrected threshold stays authoritative |
| V29.13 DTF/grabber scans | `dtfActive` and `grabberUnused` from `ForceReserveService` | DTF owner/in-play/title detection retained; grabber deliberately means any unused copy; `-30` unchanged |

The commented predecessors contain stale or narrower fact derivation only. This cleanup does not
change any live condition, fact, score, or action order.

## Explicit exclusions

- Do not edit `MaintenanceFacts`, `ForceReserveService`, their callers, or any score.
- Do not include the adjacent V38/V53 diagnostic predecessors. They are reserved for Cleanup 2.5.
- Do not include the V67ai/V67am pull predecessors. They are reserved for Cleanup 2.6.
- Do not combine this with Stage 4, finalizer, objective, or engine work.
- Do not deploy or push.

## Required gate

1. Exact diff contains only these two mirrored source edits plus required changelog/history
   bookkeeping.
2. Each source file reports 5 insertions and 57 deletions, net 52 lines removed.
3. The normalized 52-line deletion stream matches the pinned SHA-256 in both bots.
4. Complete Rando and ChosenOne source edit streams are identical after package normalization.
5. Every Java source change is a comment; all live force facts and weights remain unchanged.
6. `git diff --check` passes.
7. Parent and candidate affected-module packages succeed in isolated clean worktrees.
8. Parent and candidate `javap -p -c -s -constants` output is identical for both
   `DeployEvaluator.class` files.
9. The complete current expanded focused AI contract suite passes for the candidate.
10. No deployment and no push.
