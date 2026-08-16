# DB72288 Pressure and Docking-Transit Correction

Status: source-tested, independently verified, not packaged, not deployed.

## Identity

- Branch: `codex/rando-pressure-transit-2026-08-16`
- Exact parent: `77c5ac17fcadccf033da7ec46d6cca620b21a6d7`
- Implementation: the local commit containing this report
- Replay anchor: DB `72288`, Light `LIKE MY SAGA` versus Dark `ISB_v2`

## Replay Findings

DB72288 does not support a global conclusion that Rando refuses all battles. Dark had two own-turn Battle phases with an opposing card present:

- Turn 4: V76 predicted only 10 to 19 percent win probability. Both battle candidates scored `-237.5` versus Pass `-5`, so Rando passed.
- Turn 5: V76 predicted 100 percent win probability. The battle scored `+400`, Rando initiated it, and Rando won it.
- Later Battle phases had no initiation action because no opponent card was present at Rando's location.

The damage-pressure defect was upstream. The deploy planner treated every point of opposing ability above 4 as another average destiny draw. It therefore rejected an affordable 9-power, 7-ability two-character contest against an 11-power, 10-ability stack even though both sides receive one normal battle-destiny draw. Rando deployed elsewhere, which left no legal battle to initiate.

The docking-bay move was a separate scoring defect. The engine correctly attaches a docking-bay transit parent action to the source location. The AI therefore applied `-10` for a missing mover location, then added a generic `+15` for the words "docking bay transit." That `+5` narrowly beat Pass `+2`. Rando then spent 1 Force to move Palpatine to an adjacent docking bay with the same icon value and no new drain or battle payoff.

## Production Changes

### Site pressure packages

Both mirrored deploy planners now search affordable site packages against the existing shared V181 predicate:

- ability at least 4 for one normal battle-destiny draw;
- clean power win, or raw power gap no greater than 3;
- opponent drain at least 2 for the tolerant arm;
- projected forfeit within the existing 1.25 parity factor.

The search is bounded. Its state key is deploy cost, power capped at the clean-win threshold, and ability capped at 4. For equivalent states it retains the lowest-risk package. Final selection prefers fewer bodies, then lower cost. This prevents both the old greater-than-eight greedy false negative and unnecessary larger packages.

The tolerant branch is site-only. Systems retain the legacy planner because the current `CardInfo` model does not represent permanent-pilot ability or starship forfeit adequately for the character-based V181 predicate.

### Docking-bay transit

- Generic docking-bay text contributes zero at the parent.
- A child route that is merely safe contributes zero.
- Actual objective actor advance or blocker chase still receives bounded `+300`.
- Ordinary objective retention remains `-300`.
- FormationSafety remains categorical.

### ISB destination progress

The fourth qualifying ISB agent remains parent-level on-table progress under the actual front-side card law. That identity no longer makes every arbitrary child destination count as progress toward the separate two-Rebel-Base-location route. Existing exact route completion remains intact.

### Battle prediction

No battle production code changed. The active-opponent-lightsaber proxy remains exactly `+5` once. A focused test now pins raw opposing power 5 plus one active lightsaber as predictor input 10 with one predictor call.

## Verification

- Expected red gate: the old planner returned no package for the exact 9/7 versus 11/10 drain-2 fixture.
- Independent focused ring: 12 classes, `228/0/0/0`.
- Full server reactor: `3403/4/0/26`.
- Baseline comparison: the four failures are the same `EndorOperationsCombinedEvaluatorDecisionTest` producer-predicate failures reproduced `4/4` on exact parent `77c5ac17fcadccf033da7ec46d6cca620b21a6d7`.
- Independent pinned offline Corretto Java 21 compile: pass.
- Rando and Chosen One changed-patch parity: pass for planner and card-selection mirrors.
- Compiled-marker checks: pass.
- Generic docking `+15` and safe-completion objective marker absence: pass.
- Forbidden title-typing scan: pass.
- `git diff --check`: pass.

## Scope

- Production: six AI files.
- Tests: five AI files.
- Records: `AI_CHANGELOG.md`, `AI_VERSION_HISTORY.md`, append-only `AI_MAILBOX.md`, and this report.
- Excluded: card Java, engine Java, client, deck library, database, build configuration, package, deployment, container, server settings, and live games.

## Proof Ceiling

`SOURCE_TESTED` only. The local live server still runs the previously deployed objective-score candidate and does not contain this repair. A live claim requires a later package, byte verification, fresh zero-table deployment gate, fresh JVM, and replay/log evidence.

## Suggested Live Check

Use an ISB or EOPS Dark Rando game that presents a repeated drain-2 site and an affordable two-character contest package. Confirm that Rando creates contact, then distinguish the Battle candidate from the Battle decision. For docking transit, confirm that a paid adjacent equal-value route loses to Pass unless it produces a concrete objective, drain, battle, escape, or formation payoff.
