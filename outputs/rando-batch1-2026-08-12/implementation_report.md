# Rando Batch 1 implementation report

Date: 2026-08-12 PT

Status: **PRE-COMMIT SOURCE CANDIDATE GO; PACKAGE, DEPLOYMENT, AND LIVE FIRING PENDING**

## 1. Lineage and rollback seal

| Item | Exact evidence |
|---|---|
| Worktree | `/Users/steve/gemp-rando-batch1-2026-08-12` |
| Branch | `codex/rando-batch1-persistent-response-2026-08-12` |
| Base and current HEAD | `e877d4b6eb68b02fd25f6bdf90bf9f766662f4f7` |
| Parent | `955962555a94094663c0b58d7177ffef9bae46b5` |
| Canonical baseline tag | `rando-batch1-baseline-e877d4b6-2026-08-12`, peeled to exact e877 |
| Additional preserved tag | `codex/rando-batch1-baseline-e877d4b6-2026-08-12`, peeled to exact e877 |
| Immutable rollback packet | `/Users/steve/gemp-deploy-backups/rando-batch1-2026-08-12/predeploy-e877d4b6-5fcfe59e` |
| Original e877 `web.jar` | size `46,002,727`; MD5 `5fcfe59e32c16262f8637dbc0fcd503e`; SHA-256 `68721e70243b918b4b9ab881d25d626a713a4a9d5a4b2554250bc6daaf32f3f7`; SHA-512 `babd868c223e2e232a52d233d065c307a422eb3a43ae698280d595fed106b55d8519327d73a099e3639201930109d574d6962c76efa692ee0a74a032f89c2a2f` |
| Future safe-start override | `compose.no-boot-flip.yml`, SHA-256 `5e47bcfb2ea9961ba5bf7bcb80b15f11562c7fc81931042b1505411402561b67` |

The canonical breadcrumb was completed after the initial uncommitted shared-policy draft and before bot-mirror edits, commits, builds, or deployment. The original live rollback bytes were not replaced. `web.jar.pre-k2-passivity-e877d4b6e` is an older WMAOP predecessor and is not the Batch 1 rollback.

## 2. Implemented behavior and exact owners

### Paid-drain observation

`trackGameState` is the first behavior-bearing call inside every bot `decide()` after optional trace setup. It invokes `StrategyController.observePersistentResponse` on every decision for Rando and Chosen One. Anchors:

- `RandoCalAi.java:588`, observation at `:2850`
- `TheChosenOneAi.java:588`, observation at `:2699`
- mirrored `StrategyController` owns the per-game `PersistentResponsePolicy.State`

The ledger retains the actual mutable `ForceDrainState` while the event is active. When the state changes or becomes null, it reads final `ForceDrainState.getForcePaid()` (`PersistentResponsePolicy.java:171`). It never records announced total, unpaid remaining liability, or `paid + remaining`. Null finalizes one token but does not close the opponent turn, so multiple drains in one turn aggregate without prematurely expiring another lane.

Completed-through is derived on every observation as opponent latest turn minus one while the opponent is current, otherwise opponent latest turn, clamped to zero. Histories qualify only when positive paid damage occurred at the same permanent location on the two most recently completed opponent turns. GameState identity replacement, counter regression, or an impossible active token across a completed boundary resets the ledger and logs fail closed rather than crashing the game.

### Typed target and plan selection

`PersistentResponsePlanAdapter` converts already-generated planner candidates into typed facts. A candidate must be:

- legal through the existing direct paid-deploy filter;
- available as the exact permanent/current physical cards in hand;
- affordable using exact modifier-aware deploy costs for the full same-target response group;
- timely in the current player's Deploy phase; and
- viable through an existing shared formation route.

Critical roles are self-objective facts only. The adapter reads the current bot's own analyzed objective and recognizes, in priority order, `OBJECTIVE_HARD_LOSS_DEFENSE`, `POST_FLIP_PROTECTION`, `ACTIVE_FLIP_GATE`, and `MISSING_REQUIRED_LOCATION`. Opponent effects, weapons, undercover or otherwise nonparticipating cards do not fabricate a squatter. `ObjectiveAnalyzer.hasOpponentBattleParticipantAt` requires a character, vehicle, or starship at the same physical location that can participate in battle.

A funded mandatory objective candidate wins the internal lexicographic selection unless a response at that exact target also advances the same mandatory need. For each threat, avoided two-turn damage must strictly exceed the best executable race alternative; equality preserves the alternative. Strategic income is only a tiebreak among already-qualified response candidates. There is no standalone top-income admission, cancellation branch, relocation mode, or arbitrary rank score exported into `CombinedEvaluator`.

### Formation boundary

Ground response feasibility calls shared `DeployTacticalPolicy.assessPersistentResponseFormation`, which reuses the shipped V171/V172 contact assessment and adds existing friendly power/ability to the planned formation evidence. Planned ground power and ability are public printed attributes only. Unsupported categories or modifier-dependent packages fail closed. There is no three-unit cap; viable formations with four or more bodies remain eligible.

Space admission is deliberately narrow: exactly one ship in an existing `space_bleed` or `space_reinforce` plan, independently operational through a permanent pilot and positive exact operational ability, and satisfying the shipped V296 nonlosing power contact. Mixed ship-plus-pilot plans, multiple ships, unpiloted ships, inferred crew elsewhere in hand, pilot printed power, objective-capital, establish, combined, and repilot plans fail closed.

### Exact wave and action provenance

The obligation stores the deterministic ordered same-target response group as exact `(permanentCardId, currentCardId)` keys. Unrelated instructions elsewhere in the source plan are not members. Order is instruction priority, then permanent ID, then current ID.

The outer `PERSISTENT_RESPONSE` bucket is inserted only after normal deploy evaluation and only for the next pending member. The offered source row's current card ID is resolved back to a physical card and must match both the planned permanent and current IDs (`PersistentResponsePlanAdapter.java:215-238`). The row must also be selectable, legal, affordable, timely, and neither hard-vetoed nor deferred. Missing or misaligned decision metadata, duplicate mappings, empty legacy buckets, stale target facts, or any failed proof leave the original DPS hierarchy untouched and never force Pass.

The child destination decision separately requires the same exact physical instruction, the exact planned target row, and current selectability before `scoreSelectedResponseAction` can fire. Opponent participation and the typed persistent or critical fact are rechecked against current state. An exact accepted deployment advances to the next original same-target member. Automatic absence from hand advances only when that exact card is proven in play at the planned target; discard, loss, another effect, or wrong-target presence clears the obligation.

### Additive boundary math

| Rule | Exact contribution | Boundary |
|---|---:|---|
| `deploy-persistent-response-selected` | `+300` | Each exact remaining proved wave member on its own destination decision; suppressed when existing V166 already scored that action |
| `deploy-objective-critical-eviction-selected` | `+250` | Each exact remaining proved non-spy member while the self-objective typed critical role remains current |
| V170 spy | `+0` Batch 1 | Existing V170 score remains sole owner |

The contributions never enter `scorePlan`. They do not release a hard block or defer. `V171 + persistent = 600 + 300 = 900`, which can tie V170's maximum. `V171 + persistent + critical = 600 + 300 + 250 = 1150`, intentionally above V169's `1100` maximum for a persistent objective-critical eviction. Existing V166 suppresses the overlapping persistent addition so the same drain-contest fact is not stacked twice.

Every operation reason includes `target=<title>#<permanentId>` for location-scoped validation. A two-member proved wave may therefore emit the same bounded semantic tag twice, once for each exact member's own destination decision. It is not a one-grant-per-plan score.

## 3. Exact source and test scope

Production files, 16 total:

- `ai/models/common/phase/PersistentResponsePolicy.java`
- `ai/models/common/phase/PersistentResponsePlanAdapter.java`
- `ai/models/common/phase/DeployTacticalPolicy.java`
- `ai/models/common/strategy/ObjectiveAnalyzer.java`
- Rando and Chosen One mirrors of the outer bot, `StrategyController`, `DeployPhasePlanner`, `DeploymentPlan`, `DeployEvaluator`, and `CardSelectionEvaluator`, 12 files total

Test files, 8 total:

- `PersistentResponsePolicyTest.java`
- `DeployPlanRankingAdapterParityTest.java`
- `DeployPhaseSourceParityTest.java`
- `DeployTacticalPolicyTest.java`
- `DeploymentPlanAssessmentCopyPurityTest.java`
- `PersistentResponseStrategyControllerParityTest.java`
- `ObjectiveAnalyzerBattleParticipantFactTest.java`
- `GreatTacticianObjectiveEngineContractTest.java`

Documentation files, 4 total:

- `resources/AI_CHANGELOG.md`
- `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`
- this report
- `Handoffs/AI_MAILBOX.md`

No engine, card, objective-profile JSON, client, database, deck, shield, WMAOP, `601_87`, or unrelated objective source changed. The separate WMAOP boundary commit `30b1709661a9bd0bd0ce80b3cc87ce440b9d8901` has not been cherry-picked into this dirty worktree.

## 4. Deterministic verification

### Impacted ring

Result: **201 passed, 0 failures, 0 errors, 0 skipped** across 25 suites.

This ring covers the new ledger/policy, real planner adapter, formation routes, exact wave lifecycle, both deployment-plan mirrors, both `CombinedEvaluator` bucket walks, objective participant and post-flip facts, Invasion, native `219_1`, planner characterization, destination characterization, source ownership, and Rando/Chosen One action-text parity.

### Preserved Gate 0 compatibility ring

Result: **225 passed, 0 failures, 0 errors, 0 skipped**.

- preserved Phase 1 through 3 focused set: `184/0/0/0`
- `DeployPlanRankingAdapterParityTest`: `27/0/0/0`
- `FirstOrderReignsActionDecisionParityTest`: `6/0/0/0`
- `ObjectiveAnalyzerSharedGoldenTest`: `8/0/0/0`

These two reported rings overlap and must not be summed as unique tests. The final source also passed `mvn ... -DskipTests -DskipITs compile` and `git diff --check`. Normalized Rando/Chosen One source parity passed. The live response routes contain no new `if(false)` or `SUPERSEDED` wrapper.

### `219_1` regression precondition

`GreatTacticianObjectiveEngineContractTest` adds four unchanged-card deterministic contracts:

- exact Thrawn-at-battleground plus two-artwork native front flip;
- one-artwork and missing-Thrawn front near misses;
- back stability with Thrawn plus one artwork, then flip-back at zero artwork outside battle; and
- independent missing-Thrawn flip-back.

No `219_1` card, engine, objective analyzer, or playbook behavior was changed.

## 5. Proof ceiling and residual risks

- No candidate `web.jar` has been packaged. Candidate-versus-normalized-e877 byte allowlisting remains pending.
- No jar has been replaced, no JVM restarted, and no deployment occurred.
- No live log or replay proves `deploy-persistent-response-selected`, `deploy-objective-critical-eviction-selected`, or the outer `PERSISTENT_RESPONSE` bucket fired.
- Repeated cancellation remains unknowable through the current safe public AI source and is intentionally absent from production facts. Zero paid damage is not labeled cancellation.
- Independent strategic-income admission and relocation are intentionally absent. Strategic income only breaks ties among already-qualified responses; real executable race alternatives remain supported.
- Generic mixed or multi-ship response packages remain fail closed. Only the narrow permanently piloted V296 ship route is admitted.
- Minimum whole-formation feasibility is implemented. Batch 2 site-loss memory, general wave banking, anti-alpha teleport modeling, remote overconcentration, and Batch 3 counterpunch prediction remain outside scope.
- Each exact proved wave member receives its own bounded destination addition. Live validation must count per-member tags, not assume one tag per plan.

## 6. Rollback and next gates

Before deployment, rollback is simply the exact e877 source lineage plus the untouched original live jar in the external packet. After any future deployment, rollback must use the hashed `compose.no-boot-flip.yml` override so the server remains non-operational until jar, JVM, log, database, and gameplay-switch checks pass; `shutdown=false` must be last. An ordinary compose restart or `up` is unsafe because the base `boot-flip.sh` automatically reopens gameplay after HTTP becomes available.

The next authorized sequence is:

1. Alfred reviews this stabilized diff and documentation.
2. Create the coherent local Batch 1 commit, without pushing.
3. Cherry-pick the separate test-only WMAOP boundary commit and rerun its impacted suites.
4. Build normalized e877 and Batch 1 artifacts with the same pinned image `sha256:3db65087c1a663b264017845ae5f67eef27b6a6aa4259f1c1efdb2dbda649a80`, dedicated cache, read-only root, and final offline package.
5. Byte-verify candidate changes against the normalized e877 build, and record normalized-e877 versus original-live compiler drift separately.
6. Complete Alfred review and independent work-verifier PASS before any deployment.
7. If deployed, collect fresh location-scoped logs and controlled replay evidence for both semantic rule IDs and the exact target identity.

Current verdict: **source/test candidate GO for commit review; artifact and live behavior remain HOLD**. No live firing is claimed.
