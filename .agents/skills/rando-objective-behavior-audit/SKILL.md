---
name: rando-objective-behavior-audit
description: This skill should be used when auditing, implementing, testing, reviewing, packaging, deploying, or replay-checking Rando and Chosen One behavior for a SWCCG Objective. It applies to objective flip sequences, flip-back preservation, objective-family batches, disconnected objective data, live replay failures, cross-phase Force or movement conflicts, and claims that objective facts influence gameplay.
---

# Rando Objective Behavior Audit

## Purpose

Convert source-verified objective facts into observable Rando and Chosen One decisions. Treat each
objective as a state machine whose plan must survive setup, pull, activation, deploy, movement,
battle, Force loss, forfeit, the unchanged engine flip trigger, and post-flip play.

Reject the following as behavioral proof:

- A complete workbook row.
- A loader-enabled profile with no active decision consumer.
- A helper returning a nonzero score without a candidate comparison.
- A passing compile with no packaged-byte verification.
- A marker in `web.jar` with no fresh-JVM or gameplay evidence.

## Required Reads

Read these before editing:

1. `AGENTS.md`
2. `resources/BUILD_AND_DEPLOY.md`
3. The latest objective takeover handoff under `Handoffs/`
4. `references/methodology.md`
5. `references/batch-checklist.md`

Read the actual front and back objective card Java for every objective in the batch. Read supporting
card Java when the objective's trigger depends on another card's action or state. Never edit engine
or card Java.

## Core Decision Rule

Count an objective fact as gameplay influence only when this chain is intact:

```text
card source
  -> exact typed fact
  -> current missing requirement
  -> live phase owner
  -> bounded score, ordering rule, or veto
  -> winning legal candidate
  -> persistent board progress
  -> unchanged engine flip or stable back side
```

Stop calling the objective complete at the first missing link.

## Workflow

### 1. Establish a clean boundary

- Record branch, HEAD, worktree status, server state, and active-game state.
- Work from a clean isolated worktree when the primary tree is dirty.
- Preserve unrelated files. Never reset, clean, or overwrite another lane.
- Keep one writer/integrator. Delegate only read-only source, test, or review packets.

### 2. Extract the complete source law

- Record front and back trigger timing.
- Preserve exact `allOf` and `anyOf` structure.
- Distinguish control, occupy, presence, control-with, count, captive, blown-away, counter, and
  chosen-state semantics.
- Record exact blueprint IDs, typed `Filters`, `SpotOverride`, persona, species, and physical-card
  requirements.
- Trace one level upstream from a passive trigger to its playable driver. For example, a blown-away
  state may be the trigger while controlling the host system is the action Rando must pursue.
- Separate flip law, strategic enabler, and post-flip payoff.

### 3. Build the objective state machine

- Compute what is satisfied now.
- Compute every missing leg.
- Identify the next legal advancing action for each missing leg.
- Identify Force, card, actor, formation, movement, and battle obligations created for later phases.
- Identify sole enablers that must survive Force loss, forfeit, movement, and battle.
- Close front-side bonuses immediately after the flip condition is complete.
- Open only source-supported post-flip hold, payoff, flip-back, or terminal-hazard behavior.

### 4. Trace existing owners before adding code

- Search live evaluator and policy paths plus the generated rulebook.
- Reject dead owners behind `if (false)` or retired wiring.
- Keep `ObjectiveAnalyzer` responsible for typed facts and current objective state.
- Keep phase-specific scoring and vetoes in the existing phase policy or evaluator ordering point.
- Prefer shared common policies for identical bot behavior.
- Keep Rando and Chosen One adapters mirrored and thin.
- Use semantic rule IDs. Amend an existing owner when it already owns the behavior.

### 5. Write behavior-first tests

Write the cheapest test that can fail for the observed defect, then climb only as high as needed:

1. Native engine contract for the actual front and back law.
2. Fact-reader or analyzer state test.
3. Policy boundary test with a positive candidate and a near miss.
4. Actual candidate-selection test against Pass and a plausible distraction.
5. Persistent multi-phase chain test.
6. Rando and Chosen One parity test.
7. Replay or controlled-game evidence.

Require the unchanged objective card to perform the actual flip in deterministic scenarios whenever
the test framework supports it.

### 6. Make the smallest bounded fix

- Gate the rule on exact objective identity, correct side, missing state, legal action family,
  exact candidate identity, affordability, and tactical viability.
- Make an advancing viable candidate beat the strongest observed distraction and Pass.
- Keep existing hard legality and genuine suicide vetoes dominant.
- Do not require the destination to be controlled before selecting the action that establishes
  control.
- Do not reject a strategic enabler merely because it is not the literal flip trigger.
- Do not build a generic schema before one real objective proves the need.
- Preserve old rules unless an explicit, tested ownership migration dominates them.

### 7. Prove the whole chain

- Test pull identity and physical-card provenance.
- Test deploy order, destination, formation quality, and downstream Force reserve.
- Test legal movement or transport toward the missing state.
- Test required safe battle initiation and unsafe-battle rejection.
- Test sole-enabler retention during Force loss and battle forfeit.
- Test the actual native flip.
- Test post-flip hold, payoff, flip-back, and hard-loss boundaries.
- Test negative cross-talk against unrelated objectives and shared-title printings.

### 8. Seal one objective-family batch

- Complete one objective family, including true twin printings, before deploying.
- Run focused tests, impacted shared tests, mirror parity, and the clean package gate.
- Byte-verify changed shared and per-bot classes plus objective data in both server jar and
  `web.jar`.
- Confirm no game is active before restart.
- Restart the JVM, verify hash/load time, HTTP 200, gameplay switches, and clean startup.
- Test automated games as Rando versus Chosen One. Do not use `asdf` as the automated opponent.
- Tell Steve when the sealed batch is ready for his live test.
- Publish only the sealed feature branch to Steve's fork when the current handoff authorizes it.
  Never push or merge PlayersCommittee branches.

## Strictness Boundary

Block or stop for:

- Card-source uncertainty.
- Illegal action or false typed identity.
- A real hard tactical loss or terminal objective hazard.
- Scope violation into engine, card, database, deck-library, or client code.
- Contaminated build or deploy evidence.

Do not block the batch for:

- A missing universal abstraction when a typed local policy is sufficient.
- Lack of a live replay after deterministic native-flip and decision tests pass.
- An unrelated audit concern that does not alter the current candidate comparison.
- A desire to prove every theoretical board state.
- Documentation cleanup unrelated to the objective's decision chain.

Record non-blocking concerns and continue. Strictness protects truth, legality, tactical safety, and
artifact integrity. It must not turn into objective paralysis.

## Delegation

Use lower agents only for bounded read-only packets:

- Front and back card-law extraction.
- Existing-owner and dead-code search.
- Replay timeline reconstruction.
- Independent test and boundary review.

Require file paths, card citations, rule IDs, and explicit PASS/FAIL. Keep all production editing,
integration, committing, packaging, deployment, and remote publication under one owner.

## Required Output

For each sealed family, report:

- Objective names and printings.
- Exact flip and flip-back law.
- What Rando and Chosen One now do in each relevant phase.
- Strongest competing rule and boundary math.
- Positive, negative, sequence, parity, package, and gameplay evidence.
- Highest honestly completed proof gate.
- Known limits and exact next replay observation.
