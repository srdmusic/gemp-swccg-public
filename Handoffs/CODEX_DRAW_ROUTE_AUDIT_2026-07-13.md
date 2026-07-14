# DRAW Route Audit

Date: 2026-07-13
Owner: Codex/Alfred
Read-only source baseline: `55c22fdde`
Verdict: route contract `ADVANCE`; owner movement `HOLD`

## Canonical route

The engine sets `Phase.DRAW`, runs start-of-phase actions, then alternates player actions. The
canonical prompt is `CARD_ACTION_CHOICE`, text `Choose Draw action or Pass`, with
`autoPassEligible=true` and `noPass=false`. The draw action text is exactly
`Draw card into hand from Force Pile`; an empty response is Pass, and consecutive passes end DRAW.

Primary anchors:

- `DrawPhaseGameProcess.java:17`
- `PlayersPlayPhaseActionsInOrderGameProcess.java:38-109`
- `CardActionSelectionDecision.java:26`
- `AbstractSwccgCardBlueprint.java:2294`
- `DrawCardsIntoHandFromPileEffect.java:114`
- `RandoCalAi.java:505-1219`

Only Rando is created by the live factory. ChosenOne is a source-parity target.

## Additive evaluator route

The canonical own-turn prompt is not Draw-only. The ordered route is:

1. `DrawEvaluator`
2. `ActionTextEvaluator`
3. `PassEvaluator`

Other decision shapes in the DRAW window can also activate:

- `ForceActivationEvaluator` for any INTEGER child prompt.
- `BattleEvaluator` when action or prompt text contains battle/initiate.
- `MoveEvaluator` on the AI turn when any action matches a movement keyword.
- `CardSelectionEvaluator` for selection child prompts.
- `HeuristicAiBase` for unhandled child prompts, including its second tracker.

Opponent-turn DRAW normally routes through ActionText plus Pass. A router keyed only by
`phase == DRAW` is therefore invalid.

## Contribution and precedence contract

Draw scoring contains early returns whose order must remain exact:

- Blocked action `-200`.
- Maintenance floor `-150` and return.
- V42 emergency hand score `+200/+400/+600`.
- Critical-life `-120`, with conditional return.
- Late-life scaled penalty.
- Effective hand cap `-150` and return.
- V182 `-300` and return; this is not `hardVeto=true`.
- Hold-back, affordability, expensive-card, no-affordable-card, and force-starved policies.
- Piett, reserve shortfall/surplus, reserve preservation, overflow, and last-Force terms.

Blocked canonical draw also receives ActionText `-100000`. That is a score-costume veto, not a
typed hard veto. Pass is not a stable `~5-8` baseline in DRAW; it can be 2 early and exceed 100 when
DTF and maintenance bonuses stack.

## Reserve arithmetic that must freeze before extraction

The live target is computed locally in `DrawEvaluator`, not owned by `ForceReserveService`:

`min(10, DTF + FirstStrike + contestedAny + turn4Buffer + 2*IAO + VergeMove + maintenance) + corridorCharacters`

Behavioral details:

- `contestedAny` uses ownership only and collapses every contested location to one point.
- V67z is added after the cap and counts every owned character, not only Jedi.
- Force generation uses base one plus friendly-side icons and does not test presence despite its
  comment.
- Shared force facts map some missing inputs to false/zero.
- Every twentieth decision can cause repeated recomputation on subsequent reads.
- Maintenance parsing and the engine agree on the eight current blueprint overrides.

Changing this arithmetic while extracting it is a policy change, not consolidation.

## Registry corrections

- V24.10 requires Piett present in Force or Used Pile; absence contributes zero.
- V42 precedence with maintenance and hand-size boundaries is omitted.
- V58/V78 rows omit DTF, First Strike, turn buffer, contested-any, V79, maintenance, and post-cap
  V67z composition.
- V67w's historical cap 8 is superseded by the final cap 10.
- V67z counts all owned characters, not staged Jedi.
- V79's DRAW reserve arm is missing.
- V182 is a `-300` early return, not an implementation hard veto.
- V167's effective blocked score also includes ActionText `-100000`.
- Untagged hold-back, affordability, force-starved, life-force, hand-cap, and baseline policies are
  absent from the six-row DRAW registry summary.

## Smallest neutral seam

Create the shadow snapshot immediately after legacy context construction and before
`CombinedEvaluator.canHandle`. Read already-parsed fields only. Do not call services, evaluators, or
`getForceReserveFacts()` from the snapshot builder.

The first typed extraction is a pure `DrawReserveAssessment` preserving the current local operation
order. Keep maintenance-floor and reserve-target reads in their current sequence, including soak
recomputations, cap-before-V67z, ownership-only contested detection, all-character corridor count,
and error fallback `1`.

## Required frozen fixtures

1. `DRAW_TopLevel_Canonical`: route/evaluator order, candidate order, additive contributions,
   first-seen tie, Pass eligibility, and final response.
2. `DRAW_RuleArm_Matrix`: every score delta and early-return boundary, including Piett absent/present
   and hand one versus hand two under maintenance/V42.
3. `DRAW_Reserve_CapThenTransit`: all reserve components, cap 10, then ordinary corridor characters
   pushing the target above 10.
4. `DRAW_Blocked_DoublePenalty`: `-200` plus `-100000`, `hardVeto=false`, Pass winner.
5. `DRAW_Cancel_RawNoPass`: standard Pass, two-pass phase completion, and child-prompt emergency
   behavior.
6. `DRAW_CrossTalk_Fallback`: Battle/Move activation, INTEGER ForceActivation, and fallback with its
   second tracker.

Run every fixture through both bot entry paths and compare route, candidate/evaluator order, raw
float bits, veto state, pre-safety winner, final response, and intended mutations.
