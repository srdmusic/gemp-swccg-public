# Codex cleanup 2.6 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Frozen source: `DeployEvaluator.java` blobs at `2fb22ceba`
Scope: mirrored DeployEvaluator copies already absorbed by the V192 pull scorer

## Verdict

`ADVANCE` this exact comments-only packet after Cleanup 2.5 is cleanly committed and gated.
Executable pull detection, pull vetoes, held ActionText rollback evidence, Stage 4 Java, deployment,
and push remain `HOLD`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/DeployEvaluator.java`

Apply the identical edit to both files. Re-anchor by the markers and live owners below.

## Exact allowed deletions

Delete only these two comment-only predecessor ranges from each bot:

1. The 32-line V67ai tier block at frozen lines 3173-3204, beginning with
   `// Commented out per feedback_comment_out_old_rules:` and ending with its commented closing
   brace.
2. The 6-line V67am `+600` grant at frozen lines 3355-3360, beginning with
   `// Commented out per feedback_comment_out_old_rules:` and ending with the commented log call.

The normalized deletion streams total 38 lines per bot. Each bot's byte-identical stream has
SHA-256:

```text
ab6710a24ec9c24866d0a51249e5c6324db132cda872662f99c32b6fc347de79
```

The Rando stream followed by the ChosenOne stream has SHA-256:

```text
8f510b1b820969136ced5cbf9ee362eb500ef1499e3d36a1a3000b1d4bad12a8
```

Individual normalized SHA-256 values are:

- V67ai tier block: `91d10cf43729e487bee2ce8722292839a74a48f99e419b267dd9bea305740e8b`.
- V67am `+600` grant: `4ac26ecadd2e2638af1392827ccdad85189691ff86ce281b0613aa536aa0a244`.

## Exact preface rewrites

Replace only the first four lines of the V67ai absorption preface with:

```java
// V67ai Tier 1-3 DE scorer was removed 2026-07-13. V192 in
// ActionTextEvaluator is the sole live owner, with V131 gating and
// one resized tier emit. Git preserves the superseded
// +2000/+1800/+1600/+1500 DeployEvaluator copy.
```

Retain the following live explanation that V67i detection and the hand-deploy anchor remain active.

Replace only the five-line V67am absorption preface with:

```java
// V67am +600 pull grant is owned by V192 in ActionTextEvaluator.
// The former DeployEvaluator grant double-counted weapon pulls and was
// removed 2026-07-13; git preserves it. Live V67ar/V67ao/V149
// vetoes above remain unchanged, and V192 repeats them structurally
// before emitting the same +600 weapon tier.
```

Expected source diff per bot: 9 insertions, 47 deletions, net 38 physical lines removed. Total
source reduction: 76 lines.

## Replacement-owner proof

| Predecessor | Live owner | Boundary retained |
|---|---|---|
| V67ai DeployEvaluator tier `+2000/+1800/+1600/+1500` | V192 in `ActionTextEvaluator` | One tier emit after V131; intentionally resized to `+1500/+1400/+1300/+1200` plus base/context instead of double-counting |
| V67am DeployEvaluator weapon grant `+600` | V192 in `ActionTextEvaluator` | Same weapon tier `+600`, after structural V67ar/V67ao/V149 vetoes |

The live `v67iAddsLocation` and `v67mAddsWeapon` detection remains because downstream veto routing
still consumes it. The V162/V67ai hand-deploy block remains. This packet removes only the already
commented duplicate scorers.

## Explicit exclusions

- Do not delete or alter live V67i/V67m detection, V67ar/V67ao/V149 vetoes, V162, or the V67ai
  hand-deploy anchor.
- Do not delete held V192/V82/V60/V131/V67l/V67ai/V67m/V67am ActionText predecessors. They remain
  transaction rollback evidence until the PULL fixtures pass.
- Do not change any pull score, tier, source classifier, action order, or owner.
- Do not combine with Stage 4, finalizer, objective, or engine work.
- Do not deploy or push.

## Required gate

1. Exact diff contains only these two mirrored source edits plus required changelog/history
   bookkeeping.
2. Each source file reports 9 insertions and 47 deletions, net 38 lines removed.
3. The normalized 38-line deletion stream matches the pinned SHA-256 in both bots.
4. Complete source edit streams are identical after package normalization.
5. Every Java source change is a comment; all live detection, vetoes, scores, and order remain
   unchanged.
6. `git diff --check` passes.
7. Parent and candidate affected-module packages succeed in isolated clean worktrees.
8. Parent and candidate `javap -p -c -s -constants` output is identical for both
   `DeployEvaluator.class` files.
9. The complete current expanded focused AI contract suite passes for the candidate.
10. No deployment and no push.
