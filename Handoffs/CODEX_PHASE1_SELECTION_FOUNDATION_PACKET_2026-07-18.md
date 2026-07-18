# Phase 1 Packet: AI-Only Selection Foundation

Date: 2026-07-18

Baseline: `017bba424`

Coordinator and worker: Codex

Historical reviewer: K-2 / Claude

Status: FROZEN FOR IMPLEMENTATION

## Goal

Add the minimum AI-only contracts needed to migrate one rule arm at a time without changing current selection behavior.

## Production scope

Allowed production paths:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/policy/**`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/playbook/**`
- mirrored `PolicyOperationAdapter.java` files under Rando and ChosenOne evaluators
- the typed `defer` overload in both mirrored `EvaluatedAction.java` files

No other production path is authorized.

## Contracts

1. `PolicyOperation` preserves action id, rule-arm id, domain, manifest kind, operation kind, raw float delta, reason, and order.
2. `PolicyResult` names the one producer and owns an immutable ordered operation list.
3. `PolicyContributionLedger` requires each `(decision, actionId, ruleArmId)` contribution exactly once, including rejecting a repeated arm from the same producer.
4. Mirrored adapters apply `ADD`, `HARD_VETO`, and `DEFER` operations to existing `EvaluatedAction` choke points in order.
5. The existing AI-only `DecisionSnapshot` remains the immutable stock-decision snapshot. No duplicate snapshot type is added.
6. `ObjectiveProgressAssessment` is factual and emits no score.
7. `TurnResourcePlan` exposes named later-turn Force obligations and emits no score.
8. `PendingAiIntent` lives only in AI memory, stores exact action or physical-card constraints, and never stores a score or engine object.
9. `PendingAiIntentStore` clears on completion, Pass/No, failed search, game reset, turn change, phase change, child-shape mismatch, missing candidate, or ambiguous match.

## Explicit non-goals

- no live route or policy cutover
- no evaluator recognition change
- no score or rank retuning
- no conversion of legacy pseudo-veto magnitudes
- no `CombinedEvaluator` selection change
- no engine, decision parameter, mediator, action, card, client, deck, or database edit
- no deployment until the complete Phase 1 gate passes

## Required tests

- ordered raw-float operation application
- one-producer uniqueness and duplicate-producer rejection
- Rando/ChosenOne normalized adapter parity
- existing optional all-veto, mandatory all-veto, Pass, DEFER, DPS bucket, and first-seen tie fixtures
- objective assessment immutability and outcome invariants
- Force-plan obligation, spendable, and shortfall boundaries
- pending-intent exact match, ambiguity, missing child, lifecycle expiry, and explicit clear reasons
- existing `DecisionSnapshot` contract

## Gate

Phase 1 passes only when focused tests, affected server tests, package, mirror parity, source boundary, forbidden-symbol scan, K-2 historical review, and an independent verifier all pass. It then receives one commit and one local AI deployment after the no-live-game hall gate.
