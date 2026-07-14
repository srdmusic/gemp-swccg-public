# Codex verification: V148 deploy-site cancel loop

Date: 2026-07-11
Owner: K-2 implements; Codex verifies
Scope: read-only replay, log, and source review. No Java edits.

## Verdict

The current Rando-only V148 adjustment prevents the bad deploy, but it creates a
three-cycle parent/child retry before the generic cancel-loop breaker suppresses
the parent action.

| Stage | Evidence | Result |
|---|---|---|
| Parent deploy choice | `logs/gemp-swccg.log:29953` | Ap'lek parent action scores `80`; Pass scores `16`, so Rando enters deploy targeting. |
| Child site choice | `logs/gemp-swccg.log:29960-30011` | All sites are negative. V177 gates the contested sites; best remaining site scores `-375`. |
| V148 result | `logs/gemp-swccg.log:30011-30251` | New zero pass bar returns Done, but the same parent action is selected again twice. |
| Loop breaker | `logs/gemp-swccg.log:30255` | After three consecutive Dones, `DecisionTracker` blocks the parent action. |
| Final parent state | `logs/gemp-swccg.log:30300-30302` | Blocked parent falls to `-119998`; Pass wins at `16`. |
| Replay result | `replays/asdf/f27ws5lgy0g58k5p.xml.gz`, last segment starts at event `1998` | No Ap'lek deployment occurs. The bad deploy was prevented. |

## Source boundary

| Check | Result |
|---|---|
| Rando V148 implementation | `rando/evaluators/CombinedEvaluator.java:283-315` |
| Chosenone mirror | Missing. Chosenone still uses only `BAD_ACTION_THRESHOLD` at `chosenone/evaluators/CombinedEvaluator.java:283`. |
| Generic three-strike breaker | `rando/DecisionTracker.java:195-225` |
| Existing parent blocker | `rando/DecisionTracker.java:431-443` |
| Forced selections | Still protected. V148 only returns empty when `min == 0` and the decision is passable or explicitly offers Done/Cancel/optional. |

## Adjust-in-place recommendation

Keep the V148 zero pass bar, but when it cancels an all-negative `Choose where
to deploy ... Done to cancel` child decision, block the immediately preceding
parent deploy action on the first cancel. Reuse `blockLastActionOnCancel`; do not
create another scoring string. The generic three-strike breaker remains the
fallback for ambiguous cancel paths.

Before commit:

1. Mirror the V148 `CombinedEvaluator` block to chosenone.
2. Re-run `f27ws5lgy0g58k5p` behavior or a fresh equivalent.
3. Required log shape: one negative child selection, one immediate parent block,
   then Pass or a different deploy. No three-Done sequence.
4. Compile in-container and byte-verify both `CombinedEvaluator.class` files.

