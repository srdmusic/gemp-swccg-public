# Batch 4 V182 Exact Response-Bank Implementation Report

Date: 2026-08-12

Status: source committed and integrated as `468fb6224`; artifact and runtime proof pending

Branch: `codex/rando-batch4-response-bank`

Exact implementation base: `6426d6deb463ee19190bcdead5b3bfe2d2d5dff7`

## Outcome

The narrow V182 extension is implemented for both Rando and Chosen One. It suppresses only an ordinary stock Force-pile draw that would consume Force reserved for a current, exact, fully revalidated Batch 1 response obligation while the bot is materially behind on board.

No generic draw plan, combo taxonomy, burst counter, title exception, or new StrategyController cache was added. Existing V182 code, magnitudes, and ordering remain unchanged.

## Minimum Viable Design

| Question | Broader research design | Implemented minimum |
|---|---|---|
| Where is proof stored? | A separate typed current response-bank snapshot, potentially coordinator-owned | Optional `ResponseBankDetails` on Batch 1's existing `PersistentResponsePolicy.Obligation`, already carried by the current deployment plan |
| How many cached facts? | Candidate, plan, target, and preservation metadata | Five facts only: selection turn, threat revision, whole Force cost, formation route, and the one plan domain required to re-prove the narrow space route |
| How is target identity retained? | Duplicate target snapshot data | Existing obligation permanent-location identity; Draw resolves that exact permanent card live |
| How are cards retained? | Duplicate plan snapshot | Existing obligation's ordered permanent/current response-action identities |
| How are legality and affordability retained? | Cached flags | They are not trusted at Draw. Every exact card, destination, direct deployability, and current total cost is recomputed |
| What combo protection exists? | Potential generic combo classification | None. CandidateFacts does not prove generic combo intent. Piett is the only explicit, source-owned dig bypass |

This reduced the production design to eight touched AI files. The bot coordinators lose their duplicated board-unit counters, so those changes are net deletions.

## Publication Boundary

A bank proof is attached only when all of these are true at Batch 1 selection:

- The selected candidate is a response target.
- The response won typed co-ranking with reason `selected-executable-response`.
- Batch 1's `ExecutionProof` is legal, available, affordable, and timely.
- The exact whole response cost is positive.
- The complete response formation is viable under an existing route.
- The selection is not a funded mandatory objective.
- The current player turn is known and positive.

Unknown, unsupported, and mandatory-selected cases publish nothing.

## Reachable Deploy-to-Draw Lifecycle

Root review correctly challenged whether an exact obligation could survive a
deploy phase. A response member that actually deploys does not survive, and it
should not. The reachable case is an untouched response whose outer deploy
action remains below the deploy bucket's viability floor.

The exact current source path is:

1. Batch 1 proves and selects the whole response, then the deploy adapter
   prepends its exact offered action when it is selectable, legal, affordable,
   timely, and neither hard-vetoed nor deferred. Bucket admission does not
   require a score of at least `-100`.
2. A current concrete score path is the ordinary deploy parent envelope `+50`
   plus additive V59 maintenance-holistic `-1500`, producing `-1450`. The V59
   operation is additive bookkeeping, not a hard veto or defer.
3. The V67bc bucket walker sees the response first but falls through because
   `-1450 < -100`. A later free location prelude at `+50` wins.
4. `DeploymentPlan.recordDeployment` removes that non-response prelude. Its
   persistent-response advance sees no matching response action and preserves
   the untouched obligation and bank proof.
5. On the next deploy prompt, the still-all-bad response loses to legal Pass.
   Its exact cards remain in hand, the target and cost remain current, and no
   response action has advanced.
6. Draw independently revalidates the full proof and applies the new `-300`
   boundary.

Current source anchors are `DeployEvaluator.java:5588`,
`CombinedEvaluator.java:557-606`, `DeploymentPlan.java:219-271`,
`PersistentResponsePlanAdapter.java:145-214`, and
`DrawEvaluator.java:193-213`.

The below-threshold deploy result can be transient across turns. Declining the
draw preserves the exact `C` Force as sufficient input for a fresh next-turn
selection if the target, cards, cost, route, and response reason remain current.
Later activation can then clear the V59 holistic shortfall. In the deterministic
fixture, retained `C = 4` produces a fresh turn-4 bank proof, and increasing
available Force to `9` changes the same envelope plus V59 calculation from
`-1450` to `+50`. This is preservation, not a claim that the response can
execute during the current turn or a guarantee that it will execute next turn.

`ResponseBankReachabilityTest` executes this path through the actual response
selection, bucket prepend, CombinedEvaluator threshold walk, non-response
`recordDeployment`, live response revalidation, and real Rando Draw evaluator.
It ends with the exact `V182 RESPONSE BANK` contribution at `-300`.

There is no movement seam in this implementation. The proof is deploy-plan
owned and makes no claim about planned movement.

## Draw Revalidation

The Draw adapter treats the current deployment plan only as an identity carrier. Before exposing its bank proof, it independently proves:

- Same current player and Draw phase.
- Same player turn as selection.
- Same persistent-threat ledger revision.
- Force pile still funds the whole response cost.
- Exact permanent target still resolves.
- The opponent participant and persistent or objective-critical reason are still current.
- Every ordered permanent/current response card is still in hand.
- Every exact card is still directly deployable to the exact target.
- Recomputed whole deploy cost exactly equals the selected cost.
- Current whole-formation analysis remains viable through the same V170, V171, V172, V296, or existing-formation route.

Any mismatch is inert and leaves the legacy Draw policy unchanged.

Advancing even one exact response action clears the bank proof. A partially
executed or changed wave must earn a fresh typed selection rather than inherit
the old whole-cost claim.

## Exact Draw Boundary Math

Let `H` be hand size before the candidate draw, `F` current Force pile, and `C` the recomputed whole response cost.

The arm first requires the exact action text `Draw card into hand from Force Pile`, `ourUnits + 1 < opponentUnits`, a current proof, and `F >= C`.

| Hand | Force boundary | Result |
|---:|---:|---|
| `H <= 2` | Any funded response | No response-bank penalty. V42 emergency hand repair retains ownership. |
| `3 <= H <= 5` | `F = C` | Add exactly `-300`, then return. Drawing would produce `F' = C - 1`, breaking the exact response budget. |
| `3 <= H <= 5` | `F >= C + 1` | No response-bank penalty. One draw leaves `F' >= C`, so the response stays funded. |
| `H >= 6` | `F >= C` | Add exactly `-300`, then return. The hand is repaired, so bank the selected response. |

If Piett's existing V24.10 dig is current, the new arm does not fire. The cached Piett read is reused later so its existing score and order are preserved.

Earlier owners remain earlier: V58 maintenance, V42 repair, critical life, hand limit, and original V182. The new arm sits immediately after old V182 and before holdback and Piett. No old rule moved or changed magnitude.

## Shared Fact Ownership

`DrawPhaseFactsReader` now owns:

- Exact case-insensitive recognition of the ordinary stock Force-pile draw.
- The board-unit count of owned, in-play characters, starships, and vehicles.
- The historical deficit definition `ourUnits + 1 < opponentUnits`.

Both top-level bot coordinators now call that same reader and no longer maintain duplicate counters.

## Source Mismatch and Approved Scope Reduction

The research packet contemplated a broader typed snapshot and combo-preservation seam. Current Batch 1 source provides stronger existing ownership through `Obligation`, `ExecutionProof`, and current deployment-plan identity, so a second coordinator cache would duplicate stale state and violate simplicity-first.

CandidateFacts proves funded mandatory objective status, exact response actions, execution, and formation. It does not prove a generic combo taxonomy. The implementation therefore:

- Makes funded mandatory objective selections fail closed with no bank proof.
- Preserves Piett explicitly through its existing source fact.
- Does not claim or infer generic combo preservation.
- Does not cache a stale plan or rejected alternatives.
- Does not add card-title exceptions.

## Changed Paths

Production:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/PersistentResponsePolicy.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/PersistentResponsePlanAdapter.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/DrawPhaseFactsReader.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/DrawPhasePolicy.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DrawEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/DrawEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/RandoCalAi.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/TheChosenOneAi.java`

Tests:

- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/DrawPhasePolicyTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/DrawPhaseFactsReaderTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/DeployPlanRankingAdapterParityTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/common/phase/DeployPhaseSourceParityTest.java`
- `src/gemp-swccg-server/src/test/java/com/gempukku/swccgo/ai/models/rando/evaluators/ResponseBankReachabilityTest.java`

Session records:

- `resources/AI_CHANGELOG.md`
- `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`
- `Handoffs/AI_MAILBOX.md`
- `outputs/rando-batch4-draw-economy-2026-08-12/implementation_report.md`

## Deterministic Tests

Eight new behavior tests cover:

1. Exact ordinary stock-action recognition and false near-matches.
2. Shared board deficit with owned in-play unit filtering.
3. Repaired-hand bank at hand 6.
4. Sequential hand 0 through 2 repair and hand 3 through 5 Force-surplus edges.
5. Missing current response proof remaining inert.
6. Explicit Piett bypass.
7. Maintenance, critical-life, and hand-limit earlier ownership.
8. Exact card, target, whole-cost, turn, threat-revision, and formation-route invalidation.

Additional parity and source-ownership tests prove normalized Draw evaluators, one shared rule owner, no coordinator response-bank cache, and shared board-count ownership.

A ninth lifecycle test proves the banking state is reachable through the live
decision architecture rather than only through isolated policy facts. It also
asserts fresh next-turn selection from the retained exact response cost and the
source-owned V59 score clearing after additional activation.

## Verification

- Focused six-class ring: `65/0/0/0`.
- Impacted thirteen-class ring: `117/0/0/0`.
- Parity and source-ownership five-class ring: `45/0/0/0`.
- Compile: passed with tests and integration tests skipped.
- Rando and Chosen One normalized Draw parity: passed.
- Shared source-ownership checks: passed.
- `git diff --check`: passed.
- New inline prose em-dash check: passed.

## Scope and Delivery State

No engine Java, card Java, objective data, decks, or database state was touched by this slice. Source commit `468fb6224` is integrated. The combined integration now has an offline JDK 21 candidate package; final artifact sealing, fresh-JVM, live-log, and replay proof remain campaign-level gates outside this slice. No deployment, server restart, push, or PR was performed here.

Revert `468fb6224` to remove this slice and return its behavior to exact base `6426d6deb463ee19190bcdead5b3bfe2d2d5dff7`.
