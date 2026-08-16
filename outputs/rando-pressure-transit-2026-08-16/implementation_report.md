# DB72288 Pressure and Docking-Transit Correction

Status: packaged, byte-verified, and loaded in a fresh local JVM. Live gameplay remains pending.

## Identity

- Branch: `codex/rando-pressure-transit-2026-08-16`
- Exact parent: `77c5ac17fcadccf033da7ec46d6cca620b21a6d7`
- Implementation: exact local commit `7310ea95fe72d5831927f86090c2afc0db61d936`
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
- Pinned offline Corretto Java 21 package: pass.
- Packaged `web.jar`: 46,102,860 bytes, 27,017 ZIP entries, SHA-256 `102ca01b5da7365a64ef669ae6398f013350dd87f2ae3d3bb9f3a315891c121d`.
- Artifact allowlist: 54 changed and four added class entries, all owned by the six changed production sources, with zero removals or unaccounted entries.
- Byte identity: all 85 class outputs from those sources match `target/classes`, the server jar, and `web.jar`.
- Independent K2 post-deploy verification: pass, mailbox message `m01739`.

## Scope

- Production: six AI files.
- Tests: five AI files.
- Records: `AI_CHANGELOG.md`, `AI_VERSION_HISTORY.md`, append-only `AI_MAILBOX.md`, and this report.
- Excluded: card Java, engine Java, client, deck library, database code or schema, build configuration, and live games.

## Proof Ceiling

`RUNTIME_LOADED`. Exact source, packaged bytes, fresh JVM loading, settings, and service health are proved. Semantic branch firing, improved pressure, and replay behavior are not yet proved.

## Deployment Record

- Authenticated Hall checks at `2026-08-16T19:23:16.446Z`, `19:23:32.459Z`, and `19:26:03.150Z` each showed zero total, WAITING, and PLAYING tables.
- The server was placed in shutdown mode before the final empty-Hall check and jar replacement.
- Candidate artifact: `/Users/steve/gemp-deploy-artifacts/rando-pressure-transit-2026-08-16/7310ea95-102ca01b/web.jar`.
- Prior live jar backup: `/Users/steve/gemp-deploy-backups/rando-pressure-transit-2026-08-16/predeploy-7310ea95-b06764dd/web.jar`, SHA-256 `b06764dd88f97209c0910929ed48c9dbe0d4a300999aef9fcc725d1a38e082a3`.
- The live jar was replaced by atomic rename. Only `gemp_swccg_app_1` was recreated with the copied no-boot-flip override and `--no-deps --no-build`.
- Fresh app container: `d7d52391302e9488d530f6aa8b1d73cd6d5cc9ada17826fd3499a494292306bc`, started `2026-08-16T19:27:50.335016345Z`, restart count `0`, OOM false, direct Java PID 1.
- Database container remained `46a8397072d34ed7927676aa8eaf870e6ead6d3ee9332fa1ca602526de84faa6`, with unchanged `2026-08-16T04:21:38.537361708Z` start time and restart count `0`.
- Sealed artifact, host jar, and container jar hashes all equal `102ca01b5da7365a64ef669ae6398f013350dd87f2ae3d3bb9f3a315891c121d`.
- HTTP returned `200`; operational mode, AI tables, private games, stat tracking, and new accounts were restored; authenticated Hall remained empty.
- Startup completed with one known pre-start multi-release-JAR warning, zero warnings after startup, and zero material ERROR, FATAL, exception, or OOM lines.
- Packaged byte checks found the site-wave classes in both bots and found neither the generic docking-bay reward nor the safe-completion objective marker.

## Runtime Rollback

Atomically restore the new `b06764dd...` backup above, recreate only the app with the copied no-boot-flip override, restore the five operational/settings switches, and repeat the hash, HTTP, Hall, database-identity, and startup checks. Do not use the older `917f080f...` jar as the immediate rollback for this deployment.

## Suggested Live Check

Use an ISB or EOPS Dark Rando game that presents a repeated drain-2 site and an affordable two-character contest package. Confirm that Rando creates contact, then distinguish the Battle candidate from the Battle decision. For docking transit, confirm that a paid adjacent equal-value route loses to Pass unless it produces a concrete objective, drain, battle, escape, or formation payoff.
