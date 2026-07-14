# PULL And Search Phase Packet

Status: `FROZEN, PIPELINED, NOT YET RELEASED FOR JAVA`

This packet executes step 4 of `CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`. Release only after the DRAW commit passes its independent gate.

## Goal

Give parent pull actions, deploy/take children, destination children, and forced destinations one typed transaction. PULL owns search identity, target selection, and outcome. DEPLOY later owns formation, destination safety, and deploy sequencing through an immutable handoff. Preserve the existing engine's failed-search rule instead of creating a second AI memory system.

## Frozen Routes

| Route | Wire shape |
|---|---|
| `PULL_PARENT` | `CARD_ACTION_CHOICE` |
| `PULL_DEPLOY_CHILD` | `ARBITRARY_CARDS` |
| `PULL_TAKE_CHILD` | `ARBITRARY_CARDS` |
| `PULL_DESTINATION` | `CARD_SELECTION` |
| `PULL_FAILED_VERIFY` | `ARBITRARY_CARDS`, `min=0`, `max=0` |

Route selection uses typed metadata and raw wire shape. It never uses action text as an identity key.

## Failed Search Authority

Do not add an engine-to-AI failed-search callback. Do not reconnect `DeckOracle.recordFailedPull()`, `clearFailedPulls()`, or the fallback title/action-text sets.

The inherited `HeuristicAiBase` failed-search sets are currently live predecessor behavior, not
disconnected code: the fallback decision path writes text/card/blueprint identities, applies repeat
penalties, and emits `FAILED_SEARCH_ADD`. In this coherent phase edit, disable those writes, penalty
reads, and trace event for Rando and ChosenOne before the single boundary verification. Use the
smallest bot-scoped seam so Beginner and Advanced AI behavior is unchanged. Keep the shared fields
and helper bodies physically present until step 10 proves that no remaining subclass owns them.

The engine remains authoritative:

- Standard failed searches install `CantSearchCardPileModifier` until end of turn.
- `GameConditions.canSearch*` suppresses the prohibited action during action generation.
- Exact rule query is `ModifiersQuerying.isSearchingCardPileProhibited(gameState, sourceCard, playerId, zone, zoneOwner, gameTextActionId)`.
- Modifier expiry provides the reset. No AI success/reset lifecycle is needed.

If a custom card action bypasses `GameConditions.canSearch*`, record it as a separate engine defect. Do not compensate with an AI heuristic map.

## Typed Transaction

The transaction exists for parent/child/destination scoring consistency, not failed-search suppression. It carries:

- game, turn, player, parent decision id, and accepted parent ordinal;
- exact physical action source and `GameTextActionId` when the engine exposes them;
- exact search zone/owner and source filter or typed equivalent when exposed by the standard search effect;
- selected child physical card, including duplicate-copy identity;
- deploy versus take-into-hand intent;
- forced destination or ordered destination candidates;
- accepted, canceled, failed-verify, successful, and invalidated outcome.

For `PULL_DEPLOY_CHILD` and `PULL_DESTINATION`, publish an immutable `PullDeployRef`. It contains physical search/source/child identity and forced or ordered destination evidence. PULL remains the owner of search target and outcome. Until step 6, existing deploy and forced-`here` formation guards remain authoritative. At step 6, DEPLOY consumes `PullDeployRef` and exclusively owns formation, destination safety, Force obligations, and deploy-sequence policy. Neither phase emits the other's contribution.

Add the smallest explicit semantic metadata at the standard decision/effect construction boundary. Do not expose mutable `Action` or `Effect` objects to the AI and do not reconstruct missing fields from prompt text. Missing metadata is `UNKNOWN` and remains on the legacy route.

## Compatibility Owner

1. Build one immutable `PullFacts` and pure `PullAssessment` per transaction stage.
2. Keep parent, child, and destination compatibility adapters in their current merge slots.
3. Preserve every current magnitude, operation order, hard-veto bit, raw candidate order, and first-seen tie before deleting an owner.
4. Keep V192, V67ak, FormationSafety, DeployEvaluator, and objective contributions individually visible in the ordered trace.
5. Preserve deploy-from-pile and take-into-hand as different routes even when the wire shape matches.
6. Keep exact card Java filters authoritative. A generic LOCATION fact cannot certify a battleground, site, docking bay, or other narrower filter.
7. Preserve Scarif's exact `Filters.Krennic` plus `Filters.here(self)` behavior and the forced-destination parent guard.

## Required Fixtures

Run all fixtures through Rando and ChosenOne.

1. Endor successful pull and confirmed dead search.
2. Failed standard search installs the engine modifier, suppresses the same search for the turn, expires at turn end, and requires no AI map mutation.
   For both Rando and ChosenOne, assert no failed-search set write, no failed-search penalty read, and
   no `FAILED_SEARCH_ADD` trace event against the final disabled production state.
3. Generic location present but exact battleground subtype missing remains unavailable/unknown.
4. Deploy-from-pile child and take-into-hand child share wire shape but select different typed routes.
5. Destination child with V136/V193 and one-destination auto-select path.
6. Scarif Krennic: legitimate unflipped first pull, unsupported post-flip repeat pull, unresolved source identity, weak solo/no-plan, and true hard veto.
7. Full parent contribution ledger with V192 clamp, V67ak, formation, and DeployEvaluator.
8. Duplicate physical child cards, child cancel, failed verify, success, phase/game invalidation, and exact ties.
9. Missing semantic metadata remains legacy with exact response parity.
10. Raw candidate order, ordered operations with exact float bits, veto bits, pre-safety winner, final response, winning ordinal, accepted mutation, rejection history, and trace disposition.

## Retirement Boundary

Delete nothing in this phase. Disable old contribution call sites now owned by PULL, plus the
Rando/Chosen failed-search writes, penalty reads, and trace event, as part of the coherent edit BEFORE
the single phase-boundary verification. Run all fixtures against that final disabled state, then list
the retained predecessors for step 10. The `DeckOracle` failed-pull map/helpers and inherited fallback
failed-search fields/helpers wait for step-10 static caller proof. If Beginner or Advanced AI still
uses the inherited memory, step 10 must retain it or migrate those callers separately; it may not
delete shared storage on Rando/Chosen proof alone. Retain V192 source, Endor fallbacks, constant-false
pull branches, Krennic/formation guards, and any custom-search exception until their exact replacement
owner passes a named fixture.

## Verification And Commit

No tests while editing. At the phase boundary:

1. Run focused PULL transaction, modifier-authority, route, and compatibility fixtures once.
2. Run accepted-response lifecycle, DRAW semantic, trace, and both-bot parity regressions.
3. Run static caller proof for every failed-pull helper/set queued for step 10, including subclass
   reachability; prove Rando and Chosen are disabled while Beginner and Advanced remain unchanged.
4. Run the affected-module compile/package gate.
5. Run `git diff --check` and inspect exact production/test/doc paths.
6. Make one coherent PULL phase commit.
7. Return commit/parent SHA, paths, test counts, route matrix, modifier lifecycle proof, transaction matrix, operation parity, retained exceptions, and zero game/browser/deploy proof.

## Hard Stops

- An AI-side failure map duplicates `CantSearchCardPileModifier`.
- Any Rando/Chosen failed-search set write, penalty read, or `FAILED_SEARCH_ADD` event remains reachable
  after the coherent edit.
- Disabling the predecessor changes Beginner or Advanced AI behavior.
- Action text, blueprint title, or broad category becomes the transaction key.
- Mutable engine `Action` or `Effect` leaks into the public AI fact contract.
- A generic LOCATION certifies a narrower source filter.
- Parent and child lose physical identity or disagree about forced destination.
- Candidate order or exact tie behavior changes.
- Rando and ChosenOne diverge.
