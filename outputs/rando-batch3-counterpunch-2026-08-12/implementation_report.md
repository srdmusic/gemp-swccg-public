# Batch 3 Battle-Retention Implementation Report

Date: 2026-08-12
Branch: `codex/rando-batch3-exact-retention-telemetry`
Exact base: `6426d6deb463ee19190bcdead5b3bfe2d2d5dff7`
Worktree: `/Users/steve/gemp-rando-batch3-2026-08-12`

## Result

The Batch 3 candidate implements telemetry plumbing and an executable zero-score boundary. It does not change battle selection.

Both public bots now expose expected friendly and opponent battle destiny from values the predictor already generated. The shared coordinator reuses the one V76 prediction for a named target, reads typed retention telemetry once after V61 and before V27, and applies an empty policy result. Every public retention route is `RAW_PREDICTOR_ONLY` or `UNKNOWN`, with contribution `0`.

No exact-retention penalty, expected-collapse heuristic, positive bonus, engine facts reader, or card-specific inference shipped.

## Simplicity checkpoint

Two designs were evaluated:

| Design | Production shape | Reachable behavior | Decision |
|---|---|---|---|
| Exact resolver experiment | Several hundred lines of unit, side, dependency, simultaneous-obligation, search, suppression, and scoring logic | None. Only tests could inject `EXACT` facts | Removed |
| Telemetry-only boundary | Two predictor fields, two adapter copies, one small typed policy, one narrow reader, and one coordinator application | Named-target telemetry only, always score `0` | Kept |

The exact resolver experiment passed focused tests, including parallel damage and attrition, fixed-total immunity, mandatory forfeits, noncharacter ordering, remains-in-play reduction, recursive loss closure, pilot status, and exact-only collapse scoring. It was still speculative production infrastructure because no public or engine caller could provide its required exact snapshot. AGENTS.md simplicity-first therefore requires the smaller design.

## Current contract

- Monte Carlo routes store and average existing friendly and opponent destiny draws. They make no additional random calls and retain friendly-then-opponent call order.
- The opponent-intel route averages its existing friendly random draws and exposes its existing rounded opponent term.
- The full-intel route exposes the two rounded terms already used in outcome calculation.
- Legacy three-field outcomes preserve their original values and set new fields to `Float.NaN`.
- Nonfinite or negative telemetry downgrades to `UNKNOWN`.
- The locationless fallback does not call the retention reader.
- The physical-opponent, power-zero route does not call the retention reader.
- The named contested route reuses the cached `PredictionGate`; it never calls the predictor again.
- `BattleRetentionPolicy.evaluate` always returns an empty operation list.
- The Phase 3 block remains byte-identical, SHA-256 `8b97976005012b75aacff7afc109165b20c6414c98625194d2a22db71181dca8`.

## Behavioral HOLD

An exact `UNAVOIDABLE_COLLAPSE_BEHIND` decision remains blocked on an engine-owned read-only facts seam. Before scoring can be reconsidered, that seam must establish:

- fixed total attrition and current battle-damage and attrition obligations;
- independent simultaneous satisfaction of both tracks;
- modifier-aware forfeit, cannot-satisfy, satisfies-all, and remains-in-play behavior;
- all-present attrition immunity, including nonforfeitable participants;
- mandatory forfeits and must-before-other-character ordering;
- exact off-table battle-damage payment capacity;
- recursive attachment and aboard-card loss closure;
- pilot-dependent operational power and ability after each loss;
- response fulfillment, weapon legality and results, and conservative lethal proof.

Expected predictor means cannot substitute for those facts. Replays DB72184, DB72186, DB72143, DB72232, DB72271, DB72274, and DB72251 remain evidence fixtures, not hindsight inputs. Later realized destiny, forfeits, reacts, weapons, hidden deployments, and movement do not enter prebattle facts.

## Verification

- Focused Batch 3 and coordinator ring: 36 tests, 0 failures, 0 errors, 0 skipped.
- Broader battle, objective, Formation Safety, forfeit, and bot-parity ring: 163 tests, 0 failures, 0 errors, 0 skipped.
- Rando and Chosen One predictor sources are identical after package normalization.
- Both adapters copy all five prediction fields.
- Predictor source guards prove the original random-call count and friendly-then-opponent order.
- Source guards prove retention placement after V61 and before V27.
- Source guards prove the reader has no predictor call, no engine attrition query, and no `EXACT` state.
- Replay-shaped fixtures for DB72184, DB72186, DB72232, DB72271, DB72274, and DB72251 remain `UNKNOWN` with zero operations.
- Server and dependencies compile with tests skipped.
- `git diff --check` passes.

No package, artifact, deployment, restart, live game, push, PR, engine Java, card Java, objective data, deck, or database change was made. The verification listed above preceded the local commit.
