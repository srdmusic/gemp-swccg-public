# Codex cleanup 2.7 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Frozen source: `CardSelectionEvaluator.java` blobs at `13db1dfde`
Scope: mirrored V173/V174 maintenance-basis predecessors

## Verdict

`ADVANCE` this exact six-line-per-bot comments-only packet after the DeployEvaluator cleanup chain
is cleanly committed and gated. V190 and V51/V112 rollback branches, executable wave projection,
deployment, and push remain `HOLD`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/CardSelectionEvaluator.java`

Apply the identical edit to both files. Re-anchor by the exact statements below.

## Exact allowed deletions

Delete only these six comment lines from each bot:

1. The one-line `thisIsMaint = maint` predecessor beside `thisMaintCost = maintCost`.
2. The one-line `buddies.add(... maint ? 1f : 0f)` predecessor beside the live maintain-cost tuple.
3. The two-line `mc = getDeployCost()` / `tableMaint += mc` predecessor beside the live
   `MaintenanceFacts.maintainCost(...)` call.
4. The one-line old `reserved` expression that charges `thisCost` as upkeep.
5. The one-line old `spend` expression that charges `2 * deployCost` for a maintenance buddy.

Frozen line hints are 5543, 5547, 5569-5570, 5580, and 5601. Each bot's byte-identical six-line
stream has SHA-256:

```text
8bf5b897d67d7aa69e3ac673fab2c2895417b45722e09b592900699777d328c8
```

The Rando stream followed by the ChosenOne stream has SHA-256:

```text
a3cead56289b112e05e6276b3b98fb6477da68628a25c08182d0c2c36c5fc10c
```

Individual SHA-256 values are:

- Self maintenance flag: `0696dd1fea9506e4a9a0a7ccb74e16cc73aa114a62bcfbd05df179ac8b7514dc`.
- Buddy maintenance flag: `976b6403672e60b3d0e3533dfcfdcaf7990102929927d6d536bcd3025a80ae27`.
- Table deploy-cost upkeep: `25d5142a3afa2b3ad95c2c334ad27b16b0e901aba2aca085612865fbef99600f`.
- Candidate deploy-cost reserve: `1c514ffc8680c782a4416ef5b89987d3fb1610ec230c98b2aca6bbc919a83442`.
- Buddy double-spend: `cfb6df97ab528bb2cdf1faecba42f54b0bf901844d6fb7e84e00ee0db5c2f497`.

Do not rewrite any preface. Expected source diff per bot: 0 insertions, 6 deletions. Total source
reduction: 12 lines.

## Replacement-owner proof

V173/V174 ownership and rationale remain in the live method header. Every replacement uses
`MaintenanceFacts.maintainCost(...)`, preserving the engine's card-specific upkeep rather than the
stale boolean/deploy-cost approximations:

- The deploying card stores exact `thisMaintCost`.
- Buddy tuples store exact maintain cost in `ch[2]`.
- Table upkeep sums exact maintain cost under owner and in-play gates.
- `reserved` adds `thisMaintCost`.
- Buddy spend is deploy cost plus exact maintain cost.

All eight maintenance card-source costs match `MaintenanceFacts`: `1,2,1,2,2,3,1,1`. This packet
changes no executable projection, reserve cap, score, or action order.

## Explicit exclusions

- Do not include the pre-V190 ground-site predecessor. Live-game verification and rollback evidence
  remain pending.
- Do not include the V112 or V51 occupation loops. Battle Plan self-play and waiver evidence remain
  pending.
- Do not repair adjacent documentation defects in this cleanup commit.
- Do not alter `MaintenanceFacts`, wave-projection code, scores, or fixtures.
- Do not deploy or push.

## Required gate

1. Exact diff contains only these two mirrored source edits plus required changelog/history
   bookkeeping.
2. Each source file reports exactly 0 insertions and 6 deletions.
3. The normalized six-line deletion stream matches the pinned SHA-256 in both bots.
4. Complete source edit streams are mirror-identical.
5. Every Java source change is a comment; all live projection code remains unchanged.
6. `git diff --check` passes.
7. Parent and candidate affected-module packages succeed in isolated clean worktrees.
8. Parent and candidate `javap -p -c -s -constants` output is identical for both
   `CardSelectionEvaluator.class` files.
9. The expanded focused trace/tie/V191 suite passes.
10. No deployment and no push.
