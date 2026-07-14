# Domain Registry Gate: 5e290559c

Date: 2026-07-13
Reviewer: Codex/Alfred
Commit: `5e290559c`
Verdict: ADVANCE as research notes; HOLD as migration authority

## Verified Improvements

- All 13 former AMBIG rows now state a proposed owner and a stable source marker.
- V37.4's DeployEvaluator arm is correctly identified as inert: `canDeployToOpponents` is
  computed but not consumed, and V40 neutralized the related penalty.
- V24.2 correctly separates pull-search and drain-control ownership.
- V27 explicitly flags the separate buddy-protect arm instead of silently assigning it to the
  reservation group.
- V193 preserves objective-intel versus deploy-siting ownership.
- No production code changed.

## Blocking Corrections

1. V172's no-score claim is false.
   - The registry says V172 "awards no siting points of its own."
   - `CardSelectionEvaluator.evaluateDeployLocation` contains the live V172 SOLO DOMINANCE arm and
     calls `action.addReasoning(..., 600.0f)` before logging `-> +600`.
   - Record three exact V172 arms: protect gate, contact gate, and solo-dominance score. They share
     a formation owner but do not share kind or magnitude.

2. Thirteen rows still hide 21 separate arms.
   - V37.4 pass versus inert deploy check.
   - V169 parent urgency versus child destination.
   - V172 protect, contact, and solo-dominance.
   - V27 battle/pass/move reservation arms plus the separately flagged buddy arm.
   - V27.1 battle versus pass contributions.
   - V24.2 pull versus drain.
   - V193 parent versus child siting.
   - V67z draw versus deploy reservation.
   - The rows may share a semantic owner, but each requires its own route, kind, magnitude,
     producer, marker, parity fixture, and retirement fixture.

3. Formation enforcement needs an ownership boundary note.
   - Current hard-veto call sites are formation laws, so the current row may remain under
     solo-formation.
   - `EvaluatedAction.hardVeto` and `CombinedEvaluator` veto merge/final enforcement are generic
     constraint infrastructure. Do not migrate that mechanism into `FormationSafety` or make
     future domains depend on a formation-specific finalizer.

4. The registry is internally non-authoritative by its own addendum.
   - Section 2 placements and Section 7 counts were not regenerated.
   - The complete 357-arm stable-marker sweep is still open.
   - Therefore this commit resolves discussion labels but cannot yet gate owner moves or legacy-arm
     retirement.

## Required Completion

- Replace the 13 summary rows with the 21 exact-arm rows specified in
  `Handoffs/CODEX_DOMAIN_REGISTRY_AMBIGUITY_RESOLUTION_2026-07-13.md`.
- Correct V172's live +600 score and kind.
- Regenerate domain tables/counts from the exact arms.
- Finish the 357-arm stable-marker uniqueness sweep.
- Add the named migration and retirement fixtures before any arm changes owner.

## Post-Amendment Consistency Check

The amendment added the corrected 21-arm authority rows, but the old section 5 resolution table was
not replaced. The same file therefore still contains two incompatible authorities:

- `resources/DOMAIN_REGISTRY_2026-07-12.md:613` says V172 "awards no siting points of its own",
  while the corrected exact row at line 225 records the live V172 solo-dominance `+600`.
- `resources/DOMAIN_REGISTRY_2026-07-12.md:620` assigns generic hard-veto merge/enforcement to
  `solo-formation`, while the corrected exact row at line 462 assigns it to `loop-safety`.
- Lines 624-630 still describe the old 13-row resolution as current and say the tables/counts were
  unchanged, while the document header says the 21-arm inventory regenerated them.

Remove or rewrite the stale section 5 rows in the registry correction commit. A migration registry
cannot retain an obsolete recommendation table that contradicts its authoritative rows.
