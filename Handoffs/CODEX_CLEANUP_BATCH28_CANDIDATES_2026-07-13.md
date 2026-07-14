# Codex cleanup 2.8 candidate packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Frozen source: `ActionTextEvaluator.java` blobs at `13db1dfde`
Scope: mirrored V140 wrong Battle Plan waiver predecessor

## Verdict

`ADVANCE` this exact comments-only packet after earlier cleanup packets are cleanly committed and
gated. Other MOVE, ACTIVATE, CONTROL, and PULL predecessors remain `HOLD` because their live policy
is not literally equivalent or they remain transaction rollback evidence.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/ActionTextEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/ActionTextEvaluator.java`

Apply the identical edit to both files. Re-anchor by the V140 marker and live engine query.

## Exact allowed deletion

Delete the complete 64-line commented V140 hand-rolled detection block at frozen lines 6069-6132.
It begins with:

```java
// OLD detection commented out 2026-07-04 (feedback_comment_out_old_rules):
```

and ends with the commented V140 catch/log. Each bot's byte-identical deletion stream has SHA-256:

```text
7942e58c3a0acd66d060d83653544afad7a2c77be549c3f4877cab205a57008d
```

The Rando stream followed by the ChosenOne stream has SHA-256:

```text
624d14422aa68b7112e0197f6655badd847861322e8d62c2b000225446f4676b
```

Replace only the seven-line V140 update preface immediately above the live query with:

```java
// V140 UPDATED 2026-07-04: engine aggregate initiate-cost is the sole
// waiver authority. Card8_118 stands down while Battle Plan is on table,
// but Card8_035 imposes its own 3-Force tax unless the player occupies
// a battleground site and a battleground system.
// A queried cost of 0 receives +60 and returns; positive cost falls
// through to V104/V52/V48. Query failure also falls through.
// The wrong hand-rolled predecessor was removed 2026-07-13 after source proof.
```

The normalized replacement-preface SHA-256 is:

```text
38eb6a4a6ebbcffe38333270a1843260cbaeca9670db580806165e3234b76ab8
```

Expected source diff per bot: 7 insertions, 71 deletions, net 64 physical lines removed. Total
source reduction: 128 lines.

## Replacement-owner proof

The deleted predecessor incorrectly treated Battle Plan as a universal waiver. Card source and
engine ownership show why the current query is authoritative:

- Battle Plan installs its own 3-Force initiate-drain modifier.
- Battle Order suppresses its modifier while Battle Plan is present.
- The engine aggregates all active initiate-drain modifiers.
- Live V140 calls `getInitiateForceDrainCost(...)`, grants `+60`, and returns only when aggregate
  cost is zero. Positive cost and query failure fall through to the existing drain policy.

This is a previously landed factual correction, not a new behavior change. Commit history and
`AI_CHANGELOG` preserve the wrong predecessor and revert evidence.

## Explicit exclusions

- Do not include V169, V61c, V134, or V60 transit predecessors. Their scores or route predicates
  differ from the live owners and remain policy-bearing rollback evidence.
- Do not include V116/V95/V97/V100/V192/V82/V60/V131/V67l/V67ai/V67m/V67am/V29.7 PULL
  predecessors. They remain held until parent/child/destination PULL fixtures pass.
- Do not alter the live engine query, `+60` grant, return, V104/V52/V48 fallthrough, or any score.
- Do not deploy or push.

## Required gate

1. Exact diff contains only these two mirrored source edits plus required changelog/history
   bookkeeping.
2. Each source file reports 7 insertions and 71 deletions, net 64 lines removed.
3. The normalized 64-line deletion stream and seven-line replacement preface match their pinned
   SHA-256 values in both bots.
4. Complete source edit streams are identical after package normalization.
5. Every Java source change is a comment; the live V140 engine query and fallthrough remain
   unchanged.
6. `git diff --check` passes.
7. Parent and candidate affected-module packages succeed in isolated clean worktrees.
8. Parent and candidate `javap -p -c -s -constants` output is identical for both
   `ActionTextEvaluator.class` files.
9. The expanded focused trace/tie/V191 suite passes.
10. No deployment and no push.
