# Controlled Chosen One versus Rando bot-game implementation report

Date: 2026-08-12 PT

Status: **UNCOMMITTED SOURCE REVIEW PACKET; NO PACKAGE, DEPLOYMENT, OR GAME**

## 1. Frozen lineage

| Item | Exact value |
|---|---|
| Worktree | `/Users/steve/gemp-chosen-rando-botgame-2026-08-12` |
| Branch | `codex/chosen-rando-botgame-2026-08-12` |
| Exact base | `970851a7c853c073bebe4d3164428dca661a740e` |
| Matchup | `~The_Chosen_One` / `TheChosenOneAi` / Light versus `~Rando_Cal` / `RandoCalAi` / Dark |
| Endpoint | hidden admin-only `POST /admin/botgame` |
| Test runtime | pinned image `sha256:3db65087c1a663b264017845ae5f67eef27b6a6aa4259f1c1efdb2dbda649a80`, Corretto Java 21.0.11 |
| Test network | disabled |

This packet does not claim a commit, packaged class, loaded JVM, completed game, database row, replay, deployment, push, or pull request.

## 2. Endpoint contract

The route accepts six form fields:

| Field | Contract |
|---|---|
| `format` | nonblank Hall format code |
| `lightSkill` | exactly `CHOSENONE` |
| `lightDeck` | nonblank deck name |
| `darkSkill` | exactly `RANDO` |
| `darkDeck` | nonblank deck name |
| `deckOwner` | existing player whose decks are used for both sides |

Status behavior is explicit: missing authentication is `401`, authenticated non-admin access is `403`, blank or wrong matchup input is `400`, a missing deck owner is `404`, invalid format or deck input is `400`, and Hall operational conflicts are `409`. Success is written only after synchronous natural completion and has the form `OK gameId=<id>`. No route was added to a client page.

## 3. Hall ownership and lifecycle

`HallServer.createChosenOneVsRandoGame` is a dedicated exact path. It does not extend `normalizeAiSkill`, `getAiPlayerIdForSkill`, or `createAiForSkill`, so generic Hall requests still have no Chosen One fallback.

The path rejects:

1. shutdown mode;
2. a non-operational Hall;
3. disabled AI tables;
4. any awaiting table;
5. any unfinished running table;
6. a missing deck owner;
7. an unsupported Hall format;
8. a missing, invalid, unowned, or wrong-side deck.

Both decks pass the existing `validateUserAndDeck` path, including format validation, collection ownership filtering, and playtesting access. Side is then checked explicitly. The exact controller objects are registered under exact, distinct IDs and identity-checked. Two INFO records include game ID, side, player ID, controller class, deck owner, and deck name.

The normal `SwccgoServer.createNewGame` call remains responsible for game ID, chat, `SwccgGameMediator`, recorder listeners, game-history lifecycle, and the server running-game map. The controlled path adds only cleanup and Hall-notification result listeners. It does not add a `BotStatsGameResultListener` or game-state listener. It does not disable any timer or set automatic phase behavior.

Before start, the path attaches both result listeners, registers exact controllers, attaches the existing chat room if present, publishes the `RunningTable`, and notifies Hall listeners. It then releases the Hall write lock and invokes `startGame` synchronously. Natural finish, cancellation, setup failure, start failure, or an unfinished return removes the table and both registry entries. A start failure from an already-aborted mediator is not aborted a second time.

## 4. All-AI scheduler boundary

The ordinary mixed human/AI route remains the default. A separate iterative driver is selected only when the mediator has exactly two participants and both have registered AI controllers. The legacy `maybeLetAiPlay` helper and the legacy portion of `startClocksForUsersPendingDecision` have the same SHA-256 as the exact base:

| Legacy region | Base and candidate SHA-256 |
|---|---|
| `maybeLetAiPlay` | `0f7fd5db731e4f2c2d0cb051e1d2b1a305628c677684bbaf23d169a05bbcf298` |
| mixed entry body | `7c21b28306a2bf83625e0c3d00de7202f0852050c9d28c4d70ace26cd13e3562` |

The iterative path resolves one pending AI decision at a time, preserves the ordinary order of `setGame`, `decide`, `participantDecided`, `decisionMade`, clock credit, optional chat, and engine carry-out, then repeats until the game is finished. It accepts more than the legacy 50-decision chain and fails at 10,000 accepted decisions.

These states abort and throw instead of returning an unfinished success:

- no pending AI decision while unfinished;
- a pending player without an awaiting decision;
- a controller disappearing after pending-player discovery;
- `DecisionResultInvalidException`;
- runtime failure anywhere in the iterative discovery or resolution step;
- guard exhaustion;
- synchronous `startGame` returning unfinished.

Failure logs at ERROR, publishes a game-state message when possible, invokes ordinary game cancellation, and preserves any abort exception as a suppressed cause. The endpoint never reports a failed or guard-aborted game as success.

## 5. Synchronous execution decision

Source inspection found no existing bounded game executor in the async server or Hall lifecycle that could be reused without adding new ownership, cancellation, shutdown, and duplicate-request races. Creating an ad hoc thread was prohibited. The minimum frozen implementation therefore uses the explicitly accepted synchronous path and releases the Hall write lock first.

There is no proven wall-clock maximum. The deterministic bound is 10,000 accepted decisions, while the time for one AI evaluation or engine carry-out is not source-bounded. The operational caller should use a 30-minute request deadline as a generous observation envelope, not as proof that the game must finish within 30 minutes. A timeout is an indeterminate transport result. It is never permission to issue a second POST.

The watcher contract is therefore:

1. verify the controlled Hall gates before the POST;
2. issue exactly one authenticated POST with a 30-minute client deadline;
3. do not change auto-pass phases, timer settings, Hall settings, decks, or database state;
4. do not retry automatically on disconnect, timeout, `409`, or any other response;
5. if the client loses the response, inspect the exact Hall table, identity INFO lines, cancellation or natural-finish log, game-history row, and recorder output before any human decides what to do next;
6. accept only a natural `gameFinished` result with both exact controller identities as a completed validation game.

The synchronous call occupies one Netty worker event-loop thread for the match. This is acceptable only for the isolated, one-at-a-time controlled campaign. It is not a general public tournament service design.

## 6. Completion-listener timing

`DefaultSwccgGame.gameWon` sends the terminal winner message and then invokes result listeners before it sets `_finished=true`; its listener collection is a `HashSet`, so listener order is not a contract. The custom cleanup listener can therefore unregister both AI controllers before a later recorder or Hall listener runs and before `_finished` becomes true.

That ordering is safe for this path:

- the controller's evaluator reasoning and chosen-action logging occur inside `ai.decide`, before `decisionMade` and engine carry-out;
- mediator clock credit and optional AI chat also happen before engine carry-out;
- the terminal engine message is sent before result listeners;
- recorder and Hall result listeners use their captured game, participant, winner, loser, and recording state, not `AiRegistry`;
- after the terminal callback, the iterative driver performs no further AI lookup or decision and exits when carry-out returns and `isFinished` becomes true.

Registry removal may therefore precede later terminal listeners, but it cannot truncate final AI decision reasoning. The exact behavior still needs one natural end-to-end game to confirm live log and recording evidence.

## 7. Verification

Pinned offline Java 21 focused result: **34 passed, 0 failures, 0 errors, 0 skipped**.

| Suite | Tests | Principal coverage |
|---|---:|---|
| `SwccgGameMediatorAllAiDriveTest` | 11 | 120 decisions, exact-two boundary, mixed path, repeated decision object, checked invalid, runtime discovery and resolution, no pending decision, vanished controller, 10,001-decision guard, abort failure preservation |
| `HallServerChosenOneRandoGameTest` | 13 | exact IDs, sides, classes, standard creation call, full deck validation, listener publication, no BotStats, no state listener, unlocked start, cleanup, unfinished return, start failure, no double abort, invalid format/decks/sides, all Hall gates |
| `AdminRequestHandlerBotGameTest` | 10 | `401`, `403`, `400`, `404`, `409`, exact skill values, exact Hall call, exact success body, POST-only routing |

Additional checks passed:

- `git diff --check`;
- forbidden new-path scan for `new Thread`, `new AwaitingTable`, `BotStatsGameResultListener`, timer-disable calls, and generic Chosen One skill fallback;
- exact production allowlist;
- exact mixed-path SHA-256 comparison against base.

The first pinned offline run exposed a missing Surefire provider in the normalized dependency cache. The exact provider already present in the host Maven cache was copied into the established writable normalized cache, then the authoritative run completed with network disabled. This was dependency-cache normalization, not a source or online test change.

## 8. Exact allowlist

Production Java, exactly three:

1. `src/gemp-swccg-async/src/main/java/com/gempukku/swccgo/async/handler/AdminRequestHandler.java`
2. `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/hall/HallServer.java`
3. `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/game/SwccgGameMediator.java`

Test and test-build support:

1. `src/gemp-swccg-async/pom.xml`
2. `src/gemp-swccg-async/src/test/java/com/gempukku/swccgo/async/handler/AdminRequestHandlerBotGameTest.java`
3. `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/hall/HallServerChosenOneRandoGameTest.java`
4. `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/game/SwccgGameMediatorAllAiDriveTest.java`
5. `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/finalization/EngineAwaitingDecisionContractTest.java`

Append-only documentation:

1. `resources/AI_CHANGELOG.md`
2. `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`
3. `Handoffs/AI_MAILBOX.md`
4. `outputs/chosen-rando-botgame-2026-08-12/implementation_report.md`

Every production line outside the three named Java files is forbidden for this candidate. AI evaluators, AI strategy controllers, objective data, engine internals outside the mediator, card Java, client/UI, decks, database state, build/package artifacts, and deployment state are unchanged.

## 9. Proof ceiling and next gate

Unit tests and source contracts prove the intended isolated lifecycle. They do not prove package inclusion, JVM loading, a natural controller-versus-controller finish, result-listener behavior under the real engine, recorder completion, database row truth, replay stream pairing, or strategy behavior.

The next gate is lead review of this exact uncommitted packet. If approved, create one coherent local commit and stop. Packaging, deployment, restart, endpoint invocation, game creation, database writes, push, and pull request remain separate later gates.

Rollback before commit is to discard only this allowlist from the isolated worktree. After commit, revert the one coherent implementation commit.
