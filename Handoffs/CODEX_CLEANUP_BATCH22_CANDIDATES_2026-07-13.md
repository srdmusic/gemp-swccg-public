# Codex cleanup 2.2 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Baseline: `4a5e7d6b8`
Scope: obsolete mirrored V60 pull-baseline comments in `DeployEvaluator.java`

## Verdict

`ADVANCE` this exact comments-only packet. Deploy policy, PULL-engine restructuring, owner
retirement, runtime cutover, deployment, and push remain `HOLD`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/DeployEvaluator.java`

Apply the identical edit to both files. Both files are clean at the frozen baseline.

## Exact allowed deletion

At the `Passed all guards` / `V60 RESERVE PULL guards passed` anchor, delete only these two
commented statements in each bot:

```java
// action.addReasoning("V60 RESERVE PULL: try every turn - free value", 100.0f);
// LOG.warn("V60 RESERVE PULL: '{}' passed guards - +100 baseline", actionText);
```

The source uses typographic dashes in those two comments. The ASCII rendering above identifies the
statements; copy the exact source lines when checking the deletion stream.

Rewrite only the final sentence of the retained preface:

```text
duplicate -9999s are harmless. Superseded +100 baseline removed 2026-07-13; see git.
```

Current baseline hints are lines `893-895`. Re-anchor by the surrounding live `LOG.info`, not by
line number.

Expected source diff per bot: one comment line rewritten and two comment lines deleted. Net
reduction: two physical lines per bot, four total.

## Mirror proof

The exact two-line deletion streams are identical in Rando and ChosenOne. SHA-256:

```text
bbd106adee060b3982877cea169f332cb565209a236510bdf11d6a834fd8e6b0
```

## Replacement-owner proof

- V192 is live in `ActionTextEvaluator` under the `PULL-ENGINE` region.
- Its single emit owns the deploy-grade pull base and explicitly records that it absorbs V60-pull.
- The live `DeployEvaluator` `LOG.info` immediately after this deletion states that V192 owns the
  baseline.
- All V60 fail-stop, risk, miss, V67bg, V66, V67h, V185, and V190 guards above the anchor remain
  executable and unchanged.
- The deleted lines cannot compile or score because they are line comments.
- Git plus the verified local backup preserves the predecessor.

## Explicit exclusions

- Do not alter any live Java statement, score, guard, veto, pull predicate, action order, or log.
- Do not touch the large inline objective predecessor block or the V67ai/V67am pull predecessors.
- Do not touch held ActionText PULL rollback evidence.
- Do not combine this with Stage 4 trace work or changelog prose cleanup.
- Do not deploy or push.

## Required gate

1. Exact diff contains only these two mirrored source edits plus required changelog/history
   bookkeeping.
2. Each source file reports one insertion and three deletions, net two physical lines removed.
3. Rando and ChosenOne source edit streams are identical after package-name normalization.
4. `git diff --check` passes.
5. Parent and candidate affected-module packages succeed in isolated clean worktrees.
6. Parent and candidate normalized and raw `javap -p -c -s -constants` output are identical for
   both `DeployEvaluator.class` files.
7. The complete current focused AI contract suite passes for the candidate.
8. No deployment and no push.
