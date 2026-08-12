# Rando Batch 5D exact MWYHL flip implementation report

Date: 2026-08-12 PT

Status: **LEAD-APPROVED SOURCE CANDIDATE; LOCAL COHERENT COMMIT AUTHORIZED; ARTIFACT AND LIVE BEHAVIOR PENDING**

## 1. Lineage and scope

| Item | Exact evidence |
|---|---|
| Worktree | `/Users/steve/gemp-rando-batch5-mwyhl-flip-2026-08-12` |
| Branch | `codex/rando-batch5-mwyhl-flip` |
| Exact integration base and pre-commit HEAD | `fa66550815f17e1670cc7946b6ba3354d486038a` |
| Research input | `/Users/steve/gemp-swccg-public/outputs/rando-batch5-light-doctrine-2026-08-12/research_packet.md` |
| Implemented slice | Batch 5D only, exact Mind What You Have Learned (V) flip |

Production allowlist:

- new `ai/models/common/phase/ObjectiveFlipActionPolicy.java`
- mirrored `ai/models/rando/evaluators/ActionTextEvaluator.java`
- mirrored `ai/models/chosenone/evaluators/ActionTextEvaluator.java`

Tests:

- `ObjectiveFlipActionPolicyTest.java`
- `MindWhatYouHaveLearnedActionTextParityTest.java`
- `ObjectiveFlipActionSourceOwnershipTest.java`

Documentation:

- `resources/AI_CHANGELOG.md`
- `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`
- `Handoffs/AI_MAILBOX.md`
- this report

No engine Java, card Java, objective data, deck, database, client, build artifact, package, deployment, restart, push, or game state was changed.

## 2. Source law and evidence boundary

Actual card Java `Card225_053.java` offers `Flip` only when `canBeFlipped` is true, Luke is on Dagobah, and it is the owner's turn. `Card225_053_BACK.java` performs the required return of Luke and cards on him. The back-side `deploy -3` modifier applies only to an engine-offered react. It is not an ordinary own-turn deployment discount.

The front alone offers the Wise Advice or Yoda's Hope pull and the Dagobah-location pull. The Bespin system or Cloud City site pull is present on both source faces. All three use the three-argument `DeployCardFromReserveDeckEffect` constructor, whose boolean is `reshuffle`; the constructor delegates with `forFree=false`. These are therefore paid deployments. The parent action's `canDeployCardFromReserveDeck` checks phase, search permission, prohibition, and applicable uniqueness limits. It does not prove that a matching card is in Reserve Deck or currently affordable.

Primary replay anchors are DB `72166`, `72159`, `72202`, `72233`, and `72234`. They support taking the available flip and exploiting the returned own card through ordinary generic doctrine. They do not authorize a Luke-title target bonus, a fixed site, an invented react, or a guessed deploy cost.

## 3. Exact behavior

The pure shared policy requires every fact below:

- source front blueprint is exact normalized `225_53`;
- source is owned by the deciding player;
- source is in play;
- source is not flipped;
- candidate text trimmed is exactly `Flip`; and
- the same physical source does not offer a useful front-only setup action.

For this policy, a useful setup action means all of the following are proved:

- its same-source offered text is exactly `Deploy Effect from Reserve Deck` or `Deploy Dagobah location from Reserve Deck`;
- the owner's Reserve Deck contains Wise Advice or Yoda's Hope for the Effect action, or an exact Dagobah location for the location action; and
- the matching card passes the engine's current paid `Filters.deployable(source, null, false, 0)` check, including legality and affordability.

When all facts hold, it emits exactly one operation:

| Rule | Domain | Kind | Delta |
|---|---|---|---:|
| `OBJECTIVE.MWYHL.FLIP` | `OBJECTIVE_INTENT` | `BANDED` | `+600` |

The adapters resolve the physical source from the engine-offered `cardId`. They read Reserve feasibility only after the exact normalized blueprint, ownership, in-play, front-face, and trimmed-Flip guard succeeds. Unrelated actions never scan Reserve. Unknown or unreadable setup feasibility does not suppress Flip.

The policy runs after the existing loop-prevention vetoes and before generic action dispatch. A non-empty result is applied once, added to the action list, and returned through `continue`, so no later generic text score can stack. When a proved useful front-only setup action suppresses the contribution, existing generic pull handling scores that setup above the silent Flip. An offered action with no exact target, an illegal target, or an unaffordable target cannot strand Flip because it does not suppress the `+600` contribution.

`Deploy Bespin location from Reserve Deck` is deliberately not a suppressor. The same once-per-turn action exists on the back face, so flipping does not sacrifice it, and its recurring offer must not become a reason to postpone the flip indefinitely. It retains ordinary pull scoring independently of this exact policy.

## 4. Dominance and exclusion math

Shared Pass base is `+5`. Exact MWYHL flip is `+600`, for a minimum margin of `595`. The value matches the existing post-flip payoff-start magnitude and remains far below mandatory objective and setup actions at `+10000` or `+12000`. In the useful-setup cases, deterministic mirrored-adapter tests prove the existing setup score is at least `+100` and beats the policy-silent Flip.

The rule cannot override a loop hard veto because loop safety executes first and returns. The exact source and early-return boundary prevent text leakage and duplicate scoring. This slice changes no deployment price. It adds no Luke, destination, formation, deploy-target, cost, react, `-3`, or profile behavior.

## 5. Test-first and deterministic verification

Failure-first evidence: the three requested tests were written before production. Their first compile failed because `ObjectiveFlipActionPolicy` did not exist.

Focused result: **13 passed, 0 failures, 0 errors, 0 skipped**.

- policy exact positive, negative, front-setup classification, and Bespin exclusion cases: 5
- live mirrored ActionText adapter decisions, including useful, absent, unaffordable, wrong-source, and Bespin boundaries: 6
- source ownership, ordering, purity, and normalized full-file parity: 2

Impacted green ring:

```text
*ActionText*Test,
*Objective*PolicyTest,
*SourceOwnershipTest,
excluding the known baseline DeployActionTextSourceParityTest failure
```

Result: **311 passed, 0 failures, 0 errors, 0 skipped**.

The first inclusive impacted run exposed null blueprint handling in unrelated action-source mocks. Both adapters were corrected before normalization. Parent review then exposed two further risks: offered text did not prove a usable setup target, and the first adapter shape scanned Reserve for every action. The final reader requires an exact matching currently deployable paid card and is called only for an exact qualified Flip. The final inclusive run passed 315 of 316 tests. Its sole failure was `DeployActionTextSourceParityTest.deployParentAndDestinationAdaptersHaveNoHiddenScores`. Running that exact five-test suite in the untouched integration worktree at `fa6655081` produced the same one failure, proving it is a pre-existing baseline condition.

Additional checks:

- `mvn -q -pl gemp-swccg-server -am compile`: passed
- `git diff --check`: passed
- normalized complete Rando and Chosen One `ActionTextEvaluator` source: byte-equivalent after package and bot-name normalization, asserted by the ownership test
- production scope scan: only the three allowlisted AI production files changed

Host Maven used Homebrew JDK 25 for this source/test gate. The lead's final integrated artifact gate must rerun under the sealed pinned JDK 21 image and dedicated cache before packaging or deployment.

## 6. Proof ceiling and rollback

This candidate proves pure policy output and mirrored adapter selection. It does not yet prove native card execution, packaged class bytes, a loaded JVM, semantic-tag firing, or final-segment replay behavior.

The lead reviewed the exact diff and authorized one coherent local Batch 5D commit containing this report. Its hash is reported separately after creation so the report does not attempt to embed its own changing object ID. Revert that one commit to remove this slice. If it reaches a future combined deployment, the lead must use the already-sealed e877 rollback packet and no-boot-flip procedure documented by Gate 0.

Current verdict: **Batch 5D source and tests passed Alfred review. Local commit is authorized. Package, deployment, and live behavior remain pending.**
