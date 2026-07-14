# Codex Review: a25026c9c Formation Safety

Status: `a25026c9c` was deployed before this review reached K-2. The app restarted at
2026-07-12 17:18, the new classes are present in `web.jar`, and HTTP health is 200.

## Required Before Replacement Deploy

### P0: DPS non-bucket epilogue bypasses hard veto

Both `CombinedEvaluator.java` copies filter hard-vetoed actions inside ordered DPS buckets,
but the non-bucket epilogue at lines 206-224 scans every non-pass action without checking
`isHardVetoed()`. It returns early, before the final veto-aware selector.

Result: a hard-vetoed non-bucket action scoring at least `NON_BUCKET_EPILOGUE_FLOOR` can still
be selected.

Fix boundary: exclude hard-vetoed actions from the epilogue candidate scan.

### P0: all-veto branch misclassifies optional card selections as forced

The all-veto branch at line 291 uses:

```java
context.getMin() == 0 && !context.isNoPass()
```

The existing V148 logic at lines 324-342 already documents that optional deploy-location
prompts commonly use `min=0`, `noPass=true`, and expose cancellation through prompt text such
as `Done` or `Cancel`.

Result: when all destinations are vetoed, an optional deploy can be forced into a vetoed
destination even though Done is legal.

Fix boundary: use one shared cancellability predicate for the hard-veto branch and V148.

### P0: L3 condition is opposite Steve's no-plan requirement and can deadlock a pair

`FormationSafety.vetoCharacterDeploy()` lines 218-224 veto a weak first body only when
`affordableBuddyInHand` is true. The value comes from `v173WaveProjection()`, which proves only
global hand affordability. It does not prove a same-site follow-up or a committed deploy plan.

Steve's requested rule is stronger suppression for a weak solo when there is no concrete buddy
or movement plan. The current condition can also stop two affordable weak characters from ever
forming a pair because one must legally deploy first.

Fix boundary:

- Do not use global buddy-in-hand presence as a hard veto against the first body.
- A hard veto needs a real no-plan signal from the deploy plan, or it should remain additive.
- A same-site committed buddy sequence may exempt the first body.
- Preserve objective flip-gate, undercover, destiny-eligible, and dominance exemptions.

Minor: line 215 formats the destination title where the deployed card name should appear.

## Court Replay: Separate Root Cause

The Court 4-LOM action was not a solo deploy. It joined a power-1 friendly character at
Beldon's Corridor. Therefore the L3 solo veto does not fix the replay that prompted Steve's
deploy-weight concern.

Source action arithmetic from `logs/gemp-swccg.log` around lines 60320-60465:

| Component | Score |
|---|---:|
| Base deploy | +200 |
| V38.4 urgency | +50 |
| V169 global endangered-site urgency | +500 |
| V38 solo caution | -150 |
| V52 momentum | +100 |
| Total | +700 |
| Pass | +16 |

Destination scoring then rejected the intended Great Pit rescue at `-430` and rerouted 4-LOM
to Beldon's Corridor at `+930`. That spent the Force needed to retreat the endangered group.

Required consolidation boundary:

- A global endangered-site bonus must not bless every deploy source action.
- Gate rescue urgency on whether this candidate can produce a viable rescue destination.
- Preserve retreat Force before selecting an unrelated deploy.
- Do not solve this by increasing the solo penalty alone. That would miss the causal action.

Full replay evidence: `Handoffs/CODEX_COURT_EVACUATION_FAILURE_2026-07-11.md`.

Mailbox trail: Codex sent K-2 `m00194` with the defects and `m00195` with live-deploy proof.
