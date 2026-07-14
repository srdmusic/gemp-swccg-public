# Objective Facts And Adapter Phase Gate

Date: 2026-07-14 PDT

Status: `FINAL PRE-DEPLOY GATE PASS`

## Identity

| Item | Value |
|---|---|
| Worktree | `/Users/steve/gemp-swccg-public` |
| Branch | `rando-consolidation-2026-06-23` |
| Parent | `822202e1f11484ce5a62019240e8ac8b2fa65525` |
| Frozen packet | `/Users/steve/gemp-swccg-public/Handoffs/CODEX_OBJECTIVE_FACTS_ADAPTER_PHASE_PACKET_2026-07-13.md` |
| Packet SHA-256 | `afda65ec75911aa6274bd49a9d9deeb6e9275e832977179eb6bec2a0eda03720` |

## Boundary Results

| Gate | Result |
|---|---|
| Focused objective, PULL, lifecycle, finalizer, trace, DRAW, and setup corpus | `173 tests, 0 failures, 0 errors, 0 skipped` |
| Affected reactor package | `PASS` |
| `git diff --check` | `PASS` |
| Added inline em dash scan | `PASS`, none added |
| Mirrored evaluator/analyzer normalization | `PASS`, six file pairs identical |
| Objective policy leak scan in `DecisionSnapshot` and `ObjectiveFacts` | `PASS`, no playbook weights, ScoreNote, score, rank, or veto field |
| Explicit network, DB, deck-library, game, browser, VTS, or sandbox action | `NONE` |
| Push or deployment | `NONE` |

The lifecycle test output intentionally includes diagnostic ERROR logs from fixtures that exercise rejected answers,
chain limits, and acceptance-callback faults. Maven exited 0 and the corresponding assertions passed.

## Commands

```bash
cd /Users/steve/gemp-swccg-public/src
mvn -q -pl gemp-swccg-server -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=ObjectiveFactsContractTest,ObjectiveAdapterContractTest,ObjectiveProfileResolutionParityTest,RandoObjectiveGameReferenceLifecycleTest,TheChosenOneObjectiveGameReferenceLifecycleTest,StartingObjectiveDecisionOriginTest,PullMetadataSeamTest,DecisionSnapshotTest,CombinedEvaluatorTraceTest,RandoPullCutoverIntegrationTest,TheChosenOnePullCutoverIntegrationTest,PullPhaseOwnerTest,SwccgGameMediatorAiLifecycleTest,RandoCalAiLifecycleTest,TheChosenOneAiLifecycleTest,CuratorAiLifecycleTest,RandoDrawEvaluatorScoreParityTest,TheChosenOneDrawEvaluatorScoreParityTest,DrawPhaseOwnerTest test
mvn -q -pl gemp-swccg-server -am package -DskipTests

cd /Users/steve/gemp-swccg-public
git diff --check
git diff --unified=0 | rg --pcre2 '^\+.*\x{2014}'
```

## Required Evidence

- Physical orientation: `ObjectiveFactsContractTest` proves canonical front/back identity remains fixed while
  current/opposite ids, titles, texts, and blueprints swap in both physical orientations.
- Reset boundary: both bot lifecycle fixtures prove the same `SwccgGame` reference retains state and a new object
  reference resets it, even for a same-opponent rematch.
- Profile order: the shared corpus passes through both analyzers and proves blueprint id, title compatibility, and
  compiled fallback order.
- One snapshot: each bot has one `TraceSnapshots.build` and one `ObjectiveFactsProducer.produce` at its mediated
  boundary. `CombinedEvaluatorTraceTest` proves trace and evaluator retain the exact snapshot and ObjectiveFacts
  object identities.
- PULL authority: the live `+1500` and `+500` constants exist only in `ObjectivePullAdapter`. The old parent arm and
  child emitter remain commented for step-10 caller proof. When identity is unavailable or its ids disagree, explicit
  physical objective proof preserves only the exact predecessor value; blocked assessments and non-legacy child routes do not fall back.
  `recordFailedPull` has no production caller.
- PULL temporary wire seam: `PullMetadataSeamTest` proves real `ArbitraryCardsSelectionDecision` output keeps
  `tempN` wire ids separate from ordered physical current/permanent ids. `ObjectiveAdapterContractTest` proves ordinal
  1 resolves the second typed physical candidate and produces exactly one `+500` rank. Both bot call sites consume
  `adaptChildAtOrdinal`.
- DEPLOY shadow: exact ordered V83/V88/V108/V110, objective-site `+200`, V193 parent `+400`, and V193 child `+2000`
  contributions are fixture-pinned. V99, V86, V121, formation, affordability, legality, and sequencing remain outside
  the adapter.
- MOVE, BATTLE, and SETUP shadow: exact Hidden Path, Jedi Survivor cost 1, Inquisitor/Hatred, and typed starting-origin
  fixtures pass. These adapters emit only intent or contributions and hold no mutable state.
- Mirror parity: normalized Rando and ChosenOne `ActionTextEvaluator`, `CardSelectionEvaluator`, `CombinedEvaluator`,
  `DecisionContext`, `DeployEvaluator`, and `ObjectiveAnalyzer` files are byte-identical. The bot-entry classes retain
  their pre-existing Rando-only V79b branch; objective boundary blocks are mirrored.

## Gate Corrections

1. The first compile attempt exposed a JUnit 4/5 mismatch in the new logic fixture. The fixture now uses the module's
   JUnit 5 API.
2. The first test attempt ran 144 tests and exposed six null-side legacy lifecycle fixtures. Both test fixtures now
   initialize their known bot side, preserving the production non-null snapshot contract.
3. Independent read-only audit found that ARBITRARY child wire ids are `tempN`. The first adapter call-site draft tried
   `GameState.findCardById`, which would suppress the objective child rank. Both bots now resolve by ordered typed
   `PullFacts.candidateCards`, and the final 173-test corpus covers both halves of that seam.
4. Final review corrected the `ObjectiveFactsSource` class comment: the source owns the one-time boundary refresh,
   while `ObjectiveFactsProducer` performs the read-only projection. No runtime behavior changed.
5. K2 gates `m00654` and `m00657` found that unavailable or mismatched objective identity would suppress the
   authoritative parent and child values, and that the old parent tier arm had been deleted instead of retained. The adapter now owns an explicit physical-type
   fallback with exact legacy values, blocked-assessment guards, and a child-route guard. Both parent and child
   predecessors remain commented, and focused fixtures cover every fallback boundary.

## Intentionally Retained

- Disabled objective PULL parent and child predecessors for step-10 caller proof.
- Live DEPLOY, MOVE, BATTLE, and SETUP predecessor policy because their adapters remain shadow in this phase.
- Generic V99, deck-owned V86/V121, formation, affordability, destination safety, and sequencing policy.
- DeckOracle failed-pull storage methods, which currently have no production writer and remain for step-10 proof.

No game simulation, browser run, VTS run, sandbox scenario, deployment, or push occurred during this phase gate.
