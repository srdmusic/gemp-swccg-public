# Rando Objective Score Ceiling Migration

Date: 2026-08-15

Branch: `codex/objective-score-cap-2026-08-15`

Exact base: `efbb36723cd0ce9e33efb42764c2217126f6ea26`

Current source state: local source commit `2d143e1695dc63eca695c11de411788048b6e233`

Highest proof gate: `SOURCE_TESTED`

## Outcome

The locally tested source implements Steve's two scoring directives:

• The sole executable positive AI `+6000` literal is now `+1000`.

• Ordinary objective logic contributes at most `+300` to one action in one decision.

The migration is deliberately broad because the defect was architectural. Objective rules were not merely nudging decisions. Their magnitudes and control-flow exits often prevented later tactical logic from competing at all.

## Boundary math

| Boundary | Before | Candidate | Intended result |
|---|---:|---:|---|
| Shared MoveLadder R2 rank | `+6000` | `+1000` | R2 is important but tactically defeasible |
| Worst modeled R2 floor | `6000 - 2800 - 550 = 2650` | `1000 - 2800 - 550 = -2350` | Fine and cross-evaluator penalties may override R2 |
| R1 ceiling | `1920` | `1920` | Unchanged |
| One positive objective request | varied, sometimes thousands | exactly `+300` | Objective intent is a preference |
| Net objective contribution per action and decision | could stack | `[-300,+300]` | Repeated matches cannot rebuild a score cannon |
| Tactical override example | objective often unbeatable | `+300 - 350 = -50` | A tactical `-350` defeats objective preference |

The old R2 floor of `2650` exceeded the complete R1 ceiling of `1920`. Even the modeled worst penalties could not demote an R2 action. That was a rank lock, not a preference. The new `-2350` floor intentionally allows ordinary tactical evidence to win.

Categorical MoveLadder R3/R4 ranks and the `-100000` ladder veto remain. They represent source-proven terminal or safety classifications, not ordinary objective preference.

## Implementation boundary

Shared `ObjectivePreferencePolicy` now owns the objective score ceiling:

• Positive `OBJECTIVE_INTENT` requests normalize to exactly `+300`, even when a legacy caller requests a smaller or larger positive value.

• Negative objective requests retain their smaller signed value and bottom at `-300`.

• Repeated objective contributions for the same action are accumulated inside a signed `[-300,+300]` ceiling. A later positive signal is suppressed instead of being reconstructed as a partial positive and normalized twice.

• Common `PolicyOperation` and `PolicyResult` paths enforce the contract before adapters consume operations.

• Battle aggregation, deploy-plan ranking, and mirrored Rando and Chosen One `EvaluatedAction` accumulation enforce the same contract across merged producers.

• Runtime objective playbook weights are bounded to `300` in magnitude, including the My Lord senator route.

• Shared MoveLadder R2 changes from `+6000` to `+1000` for both bots.

## Preemption retirement

The audit found objective influence expressed through more than score additions. Reversible future plans were also using hard vetoes, defer results, dedicated deploy buckets, early `continue`, or early return. Those paths could prevent tactical negatives from being evaluated.

The candidate demotes audited ordinary cases to typed bounded preference and fallthrough. The affected behavior includes objective route retention, future objective Force reserves, preferred objective destination or card selection, objective deploy ordering, and reversible post-flip holds.

Verge and Hidden Path draw reserves retain their original marginal Force-economy arithmetic. Their contribution is now separated from general reserve facts and emitted through `OBJECTIVE_INTENT`, so the shared signed ceiling owns the combined effect.

The migration preserves categorical blocks for:

• Illegal actions or a proved absence of a legal candidate.

• Actual terminal objective destruction, out-of-play loss, or source-proven hard-loss state.

• Genuine `FormationSafety` rejection or an equivalent proved all-routes-unsafe state.

• Source-proven terminal rank claims that are not ordinary objective preference.

• Exact mechanically self-defeating choices, such as an engine deploy proven to cancel immediately or a destiny adjustment in the harmful direction.

This is the important distinction. Wanting to complete an objective later is worth at most 300 points. Proving an action illegal or terminally losing is still a block.

## Replay diagnosis

### Endor Operations overgarrison

The EOPS replay showed Rando adding more characters to the Bunker than the objective position required while leaving damage opportunities undeveloped. The audited route could accumulate an unsaturated `+900` objective-retention preference. Because objective contributions now share one `+300` ceiling, multiple matching retention reasons cannot keep increasing the same deploy action.

This change reduces the incentive to overgarrison. It does not establish a universal one-character or two-character Bunker maximum, because a real fight, card text, or tactical reinforcement can justify more.

### First Order fleet chase

The First Order replay showed three large ships concentrating at one system and following one small ship between systems. The opponent could move away each turn while Rando abandoned independent drains. An objective-labeled R2 chase started at `+6000` and remained above every R1 alternative even after the strongest modeled fines. Objective route holds also limited the alternatives allowed to compete.

R2 now starts at `+1000`, and audited reversible route holds fall through. Existing split-to-drain, battle, Force-economy, and safety logic can now override the chase when their combined evidence is stronger.

## Expected behavioral effect

The migration should make objective play directional rather than compulsory:

• A good objective step receives a meaningful `+300` nudge.

• A tactical problem larger than 300 points can override it.

• Multiple objective reasons do not stack into thousands of points.

• R2 movement no longer guarantees victory over all R1 routes.

• Ordinary objective planning no longer skips the later rules that can identify bad deployment, wasteful movement, or a missed damage opportunity.

This should reduce passive objective hoarding, overgarrison, and fleet chasing. It does not by itself prove that every drain, battle, pairing, or movement candidate is generated correctly.

## Non-goals

This candidate does not implement:

• Emperor Palpatine deployment onto Emperor's Shuttle.

• A specific repair for the AT-AT that deployed and then moved to an unsafe or unproductive destination.

• A battle-predictor correction.

• A new battle-initiation or Force-drain candidate generator.

• A guaranteed maximum garrison size at the Bunker.

Those may be separate defects. This migration removes the scoring dominance that could hide them.

## Scope

Included:

• Shared AI policy, phase, and strategy code.

• Mirrored Rando and Chosen One adapters.

• Approved `objective_playbooks.json` weights.

• AI tests and required AI history records.

• The post-snapshot domain-registry amendment for the current score owners.

Excluded:

• Engine Java and card Java.

• Client code.

• Deck library and database schema.

• Objective workbook changes.

• Packaging, deployment, server restart, or live game creation.

## Verification ledger

| Gate | Status | Evidence |
|---|---|---|
| Source migration | PASS | Exact local source commit `2d143e1695dc63eca695c11de411788048b6e233`; exact parent base `efbb36723cd0ce9e33efb42764c2217126f6ea26` |
| Sole executable positive `+6000` audit | PASS | Zero executable positive hits; historical comments and intentional `-6000` activation logic remain |
| Positive objective exact `+300` contract | PASS | Final focused ring `126/0/0/0` |
| Signed per-action objective ceiling | PASS | Focused policy, battle, plan-ranking, adapter, and merge contracts included in `126/0/0/0` |
| Rando and Chosen One parity | PASS | Exact normalized full-file parity across all 10 changed mirror pairs |
| Changed-test ring | BASELINE FAILURES ONLY | 132 classes, `1507/4/0/0`; only the exact four Endor failures reproduced on the untouched base |
| Full reactor comparison | BASELINE FAILURES ONLY | `3400/4/0/26`; the same four Endor failures, with no migration-specific failure or error |
| Independent compile | PASS | Pinned offline Corretto Java 21, network disabled, exit `0` |
| Static scope and data | PASS | 216 intended paths, zero prohibited paths; valid JSON with 164 numbers and zero outside `[-300,+300]` |
| Package and byte identity | NOT RUN | No candidate jar exists |
| Loaded JVM | NOT RUN | Deployed server remains on the prior artifact |
| Semantic branch firing | NOT RUN | No candidate log evidence exists |
| Replay behavior | NOT RUN | No candidate replay exists |

The prior deployed `web.jar` SHA-256 is `917f080f863bf26a6574a693bbccff1d6d8c7855e3bbde9fdc611bf2cfb1c8cf`. It is baseline evidence only and does not contain this migration.

The four baseline failures are `classicActorsMoveToOpenThirdSiteWithTypedProgress`, `classicMoveDoesNotScoreWhenItOnlyRelocatesSoleActor`, `duplicateBlueprintMoverResolutionBindsTheChosenPhysicalCopy`, and `botMoveLatchCarriesExactPhysicalCopyIntoChildScoring` in `EndorOperationsCombinedEvaluatorDecisionTest`. They remain failures. This report classifies them as unchanged baseline defects rather than turning a red reactor into a false PASS.

## Required next gates

• Keep the local source commit and append-only mailbox closure together as the exact review boundary. Do not push.

• Package and deploy only under a separately verified zero-active-game gate.

• Use a fresh Chosen One versus Rando game, then Steve's replay, to prove actual damage decisions. Jar presence is not branch-firing proof.

## Revert

The candidate is locally committed and undeployed. Revert local source commit `2d143e1695dc63eca695c11de411788048b6e233` to remove the migration. A runtime rollback is unnecessary unless a later package is deployed.
