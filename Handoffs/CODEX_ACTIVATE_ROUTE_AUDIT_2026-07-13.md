# ACTIVATE Route Audit

Date: 2026-07-13
Reviewer: Codex/Alfred orchestration lane
Source point: `e447d306d1db60318dba459ae215d07d6cdb22ed`
Verdict: ACTIVATE cutover `HOLD`; typed snapshot enrichment may continue

## Blocking Findings

### 1. P0: zero-activation confirmation can stall both bots

The engine asks a `MULTIPLE_CHOICE` Yes/No question after a player passes ACTIVATE without
activating Force:

- `PlayersPlayPhaseActionsInOrderGameProcess.java:76-93` creates
  `YesNoDecision("You have not activated Force. Do you want to Pass?")`.
- `ActionTextEvaluator.java:63-71` claims this prompt.
- `ActionTextEvaluator.java:88-103` creates candidates only by iterating `actionIds`.
- `RandoCalAi.java:1038-1177` copies `actionId`, `actionText`, card, blueprint, and selection
  parameters into `DecisionContext`, but never copies the ordered `results` array.

The Yes/No route therefore gives ActionTextEvaluator zero candidates. PassEvaluator remains
eligible because `noPass` is false and `min` is zero, and emits the empty-string Pass candidate at
`PassEvaluator.java:69-96`.

The boundary does not repair it:

- `RandoCalAi.java:876-883` passes only `actionIds` or `cardIds` to DecisionSafety, never `results`.
- `DecisionSafety.java:85-119` does not mark a two-result Yes/No prompt as must-choose.
- `MultipleChoiceAwaitingDecision.java:59-70` requires an integer result index, so empty is invalid.
- `SwccgGameMediator.java:1299-1329` catches that invalid response and requeues the same decision.

The V38.3 Yes/No scoring block at `ActionTextEvaluator.java:1496-1530` is unreachable because it is
inside the empty `actionIds` loop. This registry arm is labeled LIVE but does not execute on its
intended route.

### 2. P1: every INTEGER decision is misclassified as Force activation

`ForceActivationEvaluator.java:45-49` accepts every `INTEGER` decision without checking phase,
prompt origin, or engine action. Descendant card effects in ACTIVATE and other phases can also ask
INTEGER questions. The evaluator can therefore apply force-reserve arithmetic to a legal but
unrelated numeric choice.

The typed snapshot currently cannot separate these routes. It needs a typed origin/window plus
INTEGER default and recipient facts before any activation-amount assessment can replace legacy
routing.

### 3. P1: V192 does not strictly dominate Activate Force as documented

The ordinary Activate Force action receives V168 `+5000` plus V38.3 `+500`, for `+5500` before
other contributions (`ActionTextEvaluator.java:212-252`, `5799-5808`). A generic ACTIVATE pull can
also receive exactly `+5500` (`ActionTextEvaluator.java:5588-5629`). CombinedEvaluator preserves
first-seen order on equal scores, so this is a tie rather than the documented strict pull-first
boundary.

V67ak `+800` is added outside V192's local `7100` clamp (`ActionTextEvaluator.java:5493-5558`,
`5664-5679`). The reserve-three stand-down drops a viable pull to the deploy-grade base `+150`,
which still beats ordinary Pass. The complete additive boundary must be frozen before ACTIVATE or
Pull Engine ownership moves.

## Current Route Ownership

- Top-level ACTIVATE choice: `CARD_ACTION_CHOICE`; ActionText plus Pass, with possible Move/Battle
  text overlap.
- Own activation amount: `INTEGER`; currently captured by ForceActivationEvaluator.
- Opponent activation allowance: also `INTEGER`; intentionally uses the maximum.
- Zero-activation confirmation: `MULTIPLE_CHOICE [Yes, No]`; currently broken as described above.
- Interrupted acknowledgement: one-result `MULTIPLE_CHOICE`; DecisionSafety forces the sole index.
- Descendant card effects: may produce any engine decision shape and must not inherit ACTIVATE
  amount logic merely because the phase is ACTIVATE.

Normal evaluator order is ForceActivation, Deploy, Battle, Move, Draw, CardSelection, ActionText,
Pass (`CombinedEvaluator.java:60-74`). Contributions merge by action ID, and equal-score selection
retains first-seen order.

## Smallest Cutover Seam

Keep the current bot-boundary trace snapshot and add only the facts required to discriminate the
routes:

1. Decision recipient distinct from turn player.
2. Ordered `results` plus MULTIPLE_CHOICE candidate count.
3. INTEGER `defaultValue` and MULTIPLE_CHOICE `defaultIndex`.
4. Typed ACTIVATE origin/window distinguishing own activation amount, opponent allowance,
   zero-activation confirmation, and unrelated descendant prompts.
5. Legacy three-pile life count (`reserve + used + force`) as a separately named fact. Do not
   substitute the broader life-force count that includes unresolved piles.

The first pure producer should be `ActivationAmountAssessment`, and it must accept only the two
typed activation-amount origins. Preserve operation order: V57/V61c, V67at, V43, then bounds clamp.
All other INTEGER decisions stay on legacy routing until they have their own typed owner.

## Required Fixtures

1. `ACTIVATE_TopLevel_RotationAndPass`
2. `ACTIVATE_Amount_V57_V61c_V67at_V43`
3. `ACTIVATE_OpponentAllowance_AndOneChoiceAck`
4. `ACTIVATE_ZeroConfirm_YesNo_NoStall`
5. `ACTIVATE_Interleave_V167_V168_V38c_V192_Pass`
6. `ACTIVATE_DescendantInteger_NotForceActivation`
7. Rando/ChosenOne parity for every fixture above

## Boundary

This audit authorizes no scoring change, route change, owner retirement, or deployment. It blocks
only ACTIVATE cutover and any claim that V38.3 confirmation is currently live. Registry totals are
unchanged; ACTIVATE row metadata and anchors need a focused correction after K2 chooses the repair
lane.
