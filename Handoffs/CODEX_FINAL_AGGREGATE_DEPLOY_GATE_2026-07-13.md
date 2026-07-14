# Final Aggregate Review And Deploy Gate

Status: `FROZEN, PIPELINED, NOT YET RELEASED`

This packet executes step 12 only after steps 1 through 11 have committed and passed independent gates.

## Triple Lock

Deployment requires all three:

1. Aggregate offline gate passes.
2. Fresh Fable review of the final aggregate diff returns `AGREE` with no unresolved blocker.
3. Independent Codex plus `work-verifier` review returns `PASS`.

Any code change after one review invalidates all three and restarts the lock on the new HEAD.

## Aggregate Offline Gate

- Run the union of every phase's focused fixture set once, not a history of repeated micro-runs.
- Run accepted-response lifecycle, finalizer, rejection history, trace, both-bot normalized parity, raw snapshot, objective, PULL, DEPLOY, BATTLE, MOVE, SETUP, cleanup, and deploy-weight suites.
- Run the affected-module package/build.
- Run `git diff --check`, exact changed-path review, static caller/deletion proof, and compiled-bytecode presence/absence checks.
- Verify every migrated semantic has one live owner and every retained exception is named.
- Verify no DB, deck library, schema, frontend, card image, replay, or unrelated user file changed.

The aggregate report records commit chain, test command/counts, failures/errors/skips, route-owner matrix, retained waivers, deleted V-tags, operation-stream parity, final responses, and mutation/trace counts.

## Fresh Fable Review

Give Fable only:

- final HEAD and baseline SHA;
- this packet and `CODEX_PHASE_EXECUTION_MANIFEST_2026-07-13.md`;
- aggregate report;
- exact production diff and retained-waiver table.

Ask for blocking contradictions only: route ownership, behavior drift, duplicate mutation, missing finalizer, dead-code edits, score domination, bot parity, engine lifecycle, and deployment risk. Do not ask Fable to reread old mailbox history.

## Independent Codex/Verifier Gate

Use the independent `work-verifier` agent, but do not execute
`.agents/skills/work-verifier/references/verify-evaluator-edit.md` verbatim. Its host-Java,
`target/classes`, tolerant mirror-count, and legacy changelog assumptions do not match this checkout.
The verifier must instead:

- run Maven directly inside `gemp_swccg_app_1` with no output pipe that can mask its exit status;
- inspect the affected module's fat `src/gemp-swccg-async/target/web.jar`, not `target/classes`;
- require exact mirrored counts unless the manifest names a deliberate waiver; and
- use `resources/AI_CHANGELOG.md` plus the matching `AI_VERSION_HISTORY.md` entry.

Then independently inspect:

- one owner per route and contribution;
- exact physical transaction identity;
- no mutation before engine acceptance;
- no retry/fallback double-finalization;
- unknown handling;
- candidate and operation order;
- scalar boundary math and typed constraints;
- Rando/ChosenOne parity plus explicit V79b waiver;
- proven legacy deletion only.

## Build And Deploy

Follow `resources/BUILD_AND_DEPLOY.md` exactly.

1. Before any restart, inspect `logs/gemp-swccg.log` and server state for an active game. If Steve is
   mid-game or idle state is inconclusive, do not restart. Wait and recheck.
2. Build in `gemp_swccg_app_1` with `mvn -q -pl gemp-swccg-async -am package -DskipTests`.
3. Prove changed classes and live marker strings are present in host `web.jar`; prove retired dead markers are absent where expected.
4. Record jar hash and class-byte evidence.
5. Recreate only the app service with `docker compose up -d --force-recreate build` from `src/`.
6. Restore operational state and verify required AI/private/stat/account settings. Do not touch DB volumes.
7. Verify HTTP health, container start time, loaded jar identity, and clean startup logs.
8. Run the smallest controlled post-deploy smoke that reaches representative accepted/rejected lifecycle and one migrated route per strategic lane. Read `logs/gemp-swccg.log` and the generated replay. Do not edit decks or DB state.

## Four Deployment Proofs

Report these separately:

| Gate | Required evidence |
|---|---|
| 1. Compiles | Maven exit and test/build counts |
| 2. Bundled | `web.jar` hash plus class/marker byte proof |
| 3. Loaded | new container start time, health, and loaded artifact identity |
| 4. Fired | post-deploy decision-log and replay evidence for migrated owners |

## Hard Stops

- Any triple-lock reviewer disagrees.
- The aggregate diff changes after review.
- A required route has no fired/fixture evidence.
- A retired marker remains live or a new owner is absent from bytecode.
- Startup has AI, finalizer, trace, or decision lifecycle errors.
- Deployment requires DB/schema/deck/frontend destruction or a push.
- Steve is mid-game or the server's idle state cannot be established before restart.
