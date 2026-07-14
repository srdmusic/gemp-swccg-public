# Objective Facts And Adapter Phase Packet

Status: `FROZEN, PIPELINED, NOT YET RELEASED FOR JAVA`

This packet executes step 5 of `CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`. It may be released only after the accepted-response runtime, V44/V67j pilot, ACTIVATE+CONTROL, DRAW, and PULL/SEARCH commits pass their independent gates.

## Goal

Create one immutable objective-fact snapshot per decision and route objective policy through narrow phase adapters. Correct known front/back and rematch-state defects without changing unrelated phase ownership.

## Owned Production Scope

- Objective fact production and objective playbook lookup.
- A single immutable `DecisionSnapshot` fact view. Facts contain no score, rank, veto, mutable map, or evaluator side effect.
- Narrow adapters for DEPLOY, MOVE, BATTLE, PULL, and SETUP. Because PULL already cut over in step 4,
  this step makes `ObjectivePullAdapter` authoritative after shadow parity and disables only the objective-pull
  emitters it replaces. DEPLOY, MOVE, BATTLE, and SETUP remain shadow until their later owner boundaries.
- Rando and ChosenOne parity.
- The smallest loader/profile changes needed for blueprint-first lookup.

Do not absorb DRAW or ACTIVATE+CONTROL policy here. Do not redesign the JSON schema. Do not move general evaluator rules merely because they mention an objective.

## Required Corrections

1. Objective identity uses the physical objective's blueprint. The opposite side comes from `getOtherSideBlueprint()`. Never parse a `[Back Side]` block from front-side text.
2. Mutable objective state resets when the `SwccgGame` object reference changes. Opponent name and side are not a game identity.
3. Runtime profiles resolve by blueprint id first. Title lookup remains compatibility-only. A profile field with no typed runtime consumer remains design-time data.
4. The facts producer emits exactly once per mediated decision regardless of trace state. Trace capture,
   `CombinedEvaluator`, and every objective adapter consume that exact immutable object. No evaluator or
   adapter may build, mutate, or refresh another snapshot or `ObjectiveAnalyzer` state.
5. Unknown facts remain `UNKNOWN`. They do not silently become false, free, safe, battleground, or legal.
6. `DecisionSnapshot` contains objective facts only. It must not carry `ObjectivePlaybook.weights`,
   `ScoreNote`, score, rank, veto, or any other objective policy output.

## Adapter Boundaries

### DEPLOY

- Produce the exact V193 parent contribution `400.0f` and child contribution `2000.0f` in shadow only.
- Assign My Lord V83/V88/V108/V110, the objective-site `200.0f` contribution, and V193 to
  `ObjectiveDeployAdapter`. Step 6 makes that adapter the exclusive live emitter for this closed set and
  disables only the replaced call sites. No decision may sum old and new emitters.
- Preserve V99 as a separate generic rule. Keep V86 and V121 deck-owned, and keep formation,
  affordability, destination safety, and sequencing DEPLOY-owned.
- The adapter translates typed facts into ordered contributions. The facts snapshot itself carries no
  `ObjectivePlaybook.weights` or `ScoreNote` policy.

### MOVE

- Hidden Path requires two Jedi at non-Mapuzo sites.
- Underground Corridor targets a Jedi Survivor and uses deploy/move cost default `1`, not free.
- Emit objective movement intent only. Exact mover, destination, legality, and sequence remain MOVE-owner facts.

### BATTLE

- Hunt-style intent uses typed Inquisitor identity and stacked Hatred truth.
- Emit objective battle intent only. Initiation legality, weapon truth, reserve, damage, and forfeit remain BATTLE-owned.

### PULL

- Consume the typed PULL transaction and canonical failed-search truth established by the preceding PULL phase.
- Keep parent action contribution separate from child candidate rank.
- Do not recreate a title-text failed-pull map.

### SETUP

- Consume only typed starting-process references.
- Do not infer SETUP from turn number, prompt text, or objective title.

## Required Fixtures

Run the same fixture corpus through Rando and ChosenOne.

1. Run the same objective before and after flip. Assert canonical front/back blueprint ids and texts,
   plus the current/opposite blueprint ids and texts in both orientations; `getBlueprint()` and
   `getOtherSideBlueprint()` swap with physical flip state without changing canonical identity.
2. One bot reused across two distinct `SwccgGame` references with the same opponent resets all objective mutable state.
3. A same-game snapshot/revert retaining the same `SwccgGame` reference does not reset objective state.
4. Blueprint-first runtime profile wins; title compatibility fallback works; missing or malformed JSON preserves the compiled legacy profile.
5. Facts producer emits exactly once per decision with tracing enabled and disabled; trace,
   `CombinedEvaluator`, and every adapter see the same snapshot identity. Separately prove at most one
   objective contribution per action id.
6. My Lord V83/V88/V108/V110, objective-site `200.0f`, and V193 parent `400.0f`/child `2000.0f`
   retain their current separate ordered values in shadow before DEPLOY cutover.
7. Hidden Path, Underground Corridor, Hunt, PULL parent/child, failed-search, and starting-process fixtures
   use exact typed facts. PULL proves `ObjectivePullAdapter` is the sole objective-pull emitter after cutover.
8. My Lord uses typed Senator truth, and Invasion uses the exact Throne Room plus Neimoidian plus Naboo-system flip law.
9. Unknown and zero-contribution cases fall through to legacy behavior with no response delta.
10. Candidate order, ordered operation stream, exact float bits, veto/rank, winning response, trace disposition, and mutation mode match in both bots except documented bot identity fields.

## Retirement Boundary

Delete nothing in this phase. After PULL shadow parity, make `ObjectivePullAdapter` authoritative and disable
only its replaced objective-pull emitters before the single phase-boundary verification. Keep the retained
predecessors physically present for step-10 caller proof. DEPLOY selects `ObjectiveDeployAdapter` as the
exclusive live emitter for the closed My Lord/objective-site/V193 set at step 6; BATTLE, MOVE, and SETUP do
the same at their later owner boundaries. Physical deletion waits for step 10.

## Verification And Commit

No tests while editing. At the phase boundary:

1. Run focused objective facts/adapter fixtures in both bots.
2. Run the accepted-response lifecycle and trace regression set.
3. Run the affected-module compile/package gate.
4. Run `git diff --check` and inspect the exact production/test/doc path list.
5. Capture the command/tool transcript for the phase, attest explicitly that no network, DB, deck-library,
   game, browser, VTS, sandbox, or deployment action was invoked, and verify the changed-path list contains
   only packet-approved files.
6. Make one coherent phase commit. Return commit SHA, parent SHA, exact paths, test counts, parity evidence,
   reset evidence, both-orientation front/back evidence, one-snapshot identity proof, authoritative PULL
   adapter proof, and the list of legacy branches intentionally retained.

## Hard Stops

Stop before commit if any of these occurs:

- Opposite-side truth still depends on front-side text parsing.
- Canonical front/back identity changes when the physical objective flips, or current/opposite orientation
  is wrong in either state.
- Same-opponent rematch state survives a new `SwccgGame` reference.
- More than one objective snapshot is built for a mediated decision, including trace-disabled execution.
- `ObjectivePlaybook.weights`, `ScoreNote`, or any score/rank/veto policy leaks into `DecisionSnapshot`.
- An adapter contains mutable state or computes general phase legality.
- A JSON field becomes runtime policy without a typed consumer and fixture.
- PULL failure truth diverges from the canonical PULL phase contract.
- `ObjectivePullAdapter` is not the sole objective-pull emitter in the final phase state.
- DEPLOY shadow coverage omits any member of the closed My Lord/objective-site/V193 set or absorbs V99,
  V86, V121, or formation policy.
- Rando and ChosenOne produce different strategic operations or wire responses.
- Any unrelated production file changes.
