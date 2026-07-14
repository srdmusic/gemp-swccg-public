# Codex factual-comment repair gate: 978b58e1d

Date: 2026-07-13
Commit: `978b58e1d5892590ce25e4a8ba7647455f4e34f0`
Parent: `15b77630101d764d22be2701a27a0ea5ff468eba`
Verdict: `ADVANCE`
Deployment: not performed
Push: not performed

## Scope

- Only shared `MaintenanceFacts.java`, the two mirrored `CardSelectionEvaluator.java` files,
  and the two required changelog/history files changed.
- `MaintenanceFacts.java` is 2 insertions and 2 deletions; each evaluator is 2 insertions and
  1 deletion.
- Every changed Java line is a comment.
- The committed Java blobs exactly match the independently tested corrected snapshot.
- Both history files add only the factual-repair section and delete no prior content.
- `git diff --check 15b776301 978b58e1d` passes.

## Source truth

- `Card13_056.java` declares Blizzard 4 and a 1-Force maintenance cost.
- `Card13_087.java` declares Stormtrooper Garrison and a 1-Force maintenance cost.
- `Card8_118.java` and `Card13_054.java` require 3 Force to initiate a drain unless the
  player occupies both battleground theaters; Battle Plan also suppresses Battle Order's copy.
- The final two-line evaluator comment states those limits and does not retain the earlier
  unsupported universal "net negative" assessment.
- The mirrored evaluator sources remain identical after bot package/name normalization.

## Build and tests

- Detached affected-module package on the exact committed Java blobs: pass.
- Parent/candidate `MaintenanceFacts` javap SHA:
  `7a3b9a1ece5347d4681ee363ab90e0e9fd7db9fce795eda5899e2c5e841d989c`.
- Parent/candidate Rando `CardSelectionEvaluator` javap SHA:
  `4c62e0d22f5c9bba210ece81193fd89487acacafd466ab861ceb3fc82eb5bb38`.
- Parent/candidate ChosenOne `CardSelectionEvaluator` javap SHA:
  `fc5b3bf958ae560820511fc45ec71b6fb072bf761ab74519f4dfa520d55f050d`.
- Expanded focused suite: 181 tests, 0 failures, 0 errors, 1 expected F1 skip.

## Boundary

The factual-comment repair and numbered cleanup sequence are complete. Further comment deletion
remains held at 24 mirrored pairs and 840 physical lines until route fixtures and cutovers prove a
literal replacement owner. This gate does not authorize behavior changes, deployment, push, F1,
or F2.
