# Codex cleanup 2.3 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Baseline: `2a6241edf`
Scope: mirrored inline objective predecessors in `DeployEvaluator.java`

## Verdict

`ADVANCE` this exact comments-only packet. Objective policy, adapter cutover, live-owner changes,
deployment, and push remain `HOLD`.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/DeployEvaluator.java`

Apply the identical edit to both files. Both files are clean at the frozen baseline.

## Exact allowed deletion

Delete the complete comment-only region that begins with:

```java
// ----- OLD INLINE OBJECTIVE BLOCKS (V83/V110/V108/V86/V88/V99) ...
```

and ends with the commented closing brace immediately before the live V89 Dr. Evazan block.
Current baseline hints are lines `1425-1795`, inclusive. Re-anchor by both markers, not by line
number.

The source uses Unicode box drawing and punctuation in the opening marker. Match the exact source
rather than the ASCII rendering above.

The region is exactly 371 physical lines per bot and contains no executable line. Its normalized
Rando/ChosenOne SHA-256 is:

```text
b93315428a880f8c1dcc5583ebb48084a5be746a8d1b4c76f04914b3dd6b846c
```

At the retained live owner preface immediately above the deletion, replace only the stale final two
lines:

```java
// is unchanged. The old inline blocks are commented out just below (comment-out-superseded
// rule); a clean build can strip them. Logic transcribed verbatim into the analyzer.
```

with:

```java
// is unchanged. The superseded inline blocks were removed 2026-07-13 after source parity
// proof; ObjectiveAnalyzer is the sole owner of these six arms.
```

Expected source diff per bot: 2 insertions, 373 deletions, net 371 physical lines removed. Total
source reduction: 742 lines.

## Replacement-owner matrix

| Arm | Deleted predecessor | Live owner | Boundary retained |
|---|---|---|---|
| V83 | senator off Senate, `-2000` | `ObjectiveAnalyzer.java` 838-857 | My Lord, typed senator, first explicit top-location match, generic deploy skips |
| V110 | non-senator hold, `-2000` | `ObjectiveAnalyzer.java` 859-879 | My Lord, character, keyword-or-lore senator exclusion, no non-Senate site |
| V108 | senator deploy, `+500` | `ObjectiveAnalyzer.java` 881-886 | My Lord, character, keyword-or-lore senator |
| V86 | Neimoidian pilot, `-1500/+300` | `ObjectiveAnalyzer.java` 888-925 | Invasion, typed Neimoidian pilot, first friendly capital, explicit target only |
| V88 | senator to Senate, `+1500` | `ObjectiveAnalyzer.java` 927-935 | My Lord, typed senator, Galactic Senate action text |
| V99 | non-senator Senate guard, `-1500` | `ObjectiveAnalyzer.java` 937-980 | deliberately objective-ungated, printed friendly senator power vs opponent total power, `opponent <= friendly` |

The live call remains in `DeployEvaluator.java` immediately above the deletion. It invokes
`getDeployObjectiveAdjustments(...)` and applies every returned `ScoreNote` at the same additive
position as the predecessor blocks.

## Source and data proof

- Rando and ChosenOne `ObjectiveAnalyzer.java` implementations are normalized mirrors.
- The normalized 165-line live `getDeployObjectiveAdjustments(...)` method SHA-256 is
  `a6de654ba8521673520334728e846a6f7ebe49088d9e91f37b435c5f56f2e345` in both bots.
- The normalized 21-line retained `DeployEvaluator` owner-call region SHA-256 is
  `c287fae1ba2637660e6ee862dcff51bd3df91e245b54853a124a6fae667ce562` in both bots.
- Compiled `MY_LORD_PLAYBOOK` weights remain `+1500/-2000/+500/-2000`.
- Enabled runtime JSON for `12_179` and `12_179_BACK` carries the same four weights.
- V99 remains outside the objective-analyzed gate and has only an added null-title guard. A real
  Galactic Senate card has a title, so the guard only prevents malformed-state failure.
- The broader keyword-or-lore senator helper used by V108, V110, and V99 is inherited from the
  predecessor exactly. Do not replace it with `Filters.senator` in this cleanup.
- V86 is strategy for Invasion and Blockade Flagship placement, not a claim that the objective's
  flip condition requires a capital ship. Do not alter its semantics in this cleanup.
- Old V99 prose claimed non-senators lacked the Senate weapon-destiny protection. Card source applies
  the modifier without a senator filter. That unsupported prose is inside the deleted corpse and is
  not emitted by the live owner.

## Explicit exclusions

- Do not alter the live `getDeployObjectiveAdjustments(...)` call or any `ObjectiveAnalyzer` source.
- Do not alter objective JSON, weights, filters, predicates, scores, logs, action order, or V89.
- Do not combine this with Stage 4 trace work or any other cleanup region.
- Do not infer authority to retire the objective adapter or any compiled owner.
- Do not deploy or push.

## Required gate

1. Exact diff contains only these two mirrored source edits plus required changelog/history
   bookkeeping.
2. Each source file reports 2 insertions and 373 deletions, net 371 lines removed.
3. The 371-line normalized deletion stream matches the pinned SHA-256 in both bots.
4. Rando and ChosenOne complete source edit streams are identical after package normalization.
5. The live adapter call and all six live ObjectiveAnalyzer arms remain unchanged.
6. `git diff --check` passes.
7. Parent and candidate affected-module packages succeed in isolated clean worktrees.
8. Parent and candidate `javap -p -c -s -constants` output is identical for both
   `DeployEvaluator.class` files.
9. The complete current focused AI contract suite passes for the candidate.
10. No deployment and no push.
