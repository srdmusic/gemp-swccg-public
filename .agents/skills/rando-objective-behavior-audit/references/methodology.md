# Objective Behavior Methodology

## 1. Define the unit of work

Use one objective family as the default unit. Include another printing only when source proves it is
a true behavioral twin. Include another title only when both share the same typed mechanism and the
first implementation already establishes the reusable policy.

Do not treat a data row, card printing, objective title, and behavior family as interchangeable:

- A printing is one physical front/back implementation.
- A profile is one runtime data entry and may cover multiple printings.
- A family is the smallest group that can honestly share a behavior engine.
- A sealed batch is one family with all required proof layers green.

The audit currently distinguishes 58 runtime profiles and 66 front-side printings. Report both
numbers when measuring progress.

## 2. Read source as executable law

Read the front and back objective card Java. Follow every typed filter and condition that changes
the meaning of:

- Control versus occupy.
- Presence versus control-with an actor.
- Exact site versus system versus region.
- Excluded-from-battle spotting.
- Persona, species, icon, uniqueness, or ownership.
- Count threshold and strict greater-than versus greater-than-or-equal.
- Table-change, phase-start, end-turn, battle-result, or persistent-state timing.
- Flip-back, out-of-play, hard-loss, or one-way back-side behavior.

Then inspect supporting cards that create the playable route. This is the "one level upstream"
rule. The objective may flip on a passive engine state that Rando cannot choose directly.

Examples:

- Invasion flips on Throne Room control with a Neimoidian plus Naboo system control. The playable
  route includes pulling Throne Room, allocating the Neimoidian, funding a viable formation, and
  preserving both locations.
- First Order Reigns flips when Tracked Fleet is blown away. The actionable driver is controlling
  its current host system at the start of the owner's turn, usually through Supremacy's route.
- Shield twins flip after the Epic Event resolves. The route includes the exact Event, Cannon,
  piloted AT-AT, marker range, weapon destiny threshold, and firing action.
- TDIGWATT classic flips on Dark Deal plus two occupation legs. The virtual printing instead needs
  relative control of three Bespin locations. Shared title does not imply shared law.

## 3. Normalize the law without flattening it

Represent the source as explicit branches:

```text
front:
  allOf:
    - requirement A
    - anyOf:
        - route B1
        - route B2
back:
  anyOf:
    - flip-back hazard C
    - flip-back hazard D
```

Never convert `anyOf` to `allOf`. Never replace a typed actor-at-location condition with a title
fragment. Never treat control as occupation or vice versa. Preserve strict comparisons and timing.

For every leaf, record:

- Exact source citation.
- Exact typed identity.
- Current truth value.
- Legal actions capable of changing it.
- Required later-phase resources.
- Cards or formations whose loss would break the route.

## 4. Build a state machine, not a static profile

Use these states:

| State | Meaning |
|---|---|
| `UNAVAILABLE` | No legal or affordable route exists now |
| `MISSING_SETUP` | Required location, Effect, Epic Event, or route card is absent |
| `MISSING_ACTOR` | Required persona, species, vehicle, pilot, or formation piece is absent |
| `MISSING_CONTROL` | Required site/system control or occupation is absent |
| `ROUTE_OPEN` | A legal sequence exists and downstream obligations are fundable |
| `READY_TO_TRIGGER` | Native engine conditions are complete, awaiting its timing event |
| `FLIPPED_STABLE` | Back-side state is safe |
| `FLIPPED_AT_RISK` | Flip-back or terminal hazard is armed |

Recompute after every state-changing action. Front-side bonuses must close when their obligation is
complete. Post-flip rules must open only when source supports them.

## 5. Map the full decision chain

Audit every applicable phase:

| Phase | Required question |
|---|---|
| Setup | Did the unchanged objective deploy the exact cards and printings? |
| Pull | Does the exact parent action fire, and does the correct physical child beat distractors? |
| Activate | Is enough Force activated and retained for the remaining route? |
| Deploy | Are cards sequenced, funded, grouped, and sent to the exact destination? |
| Move | Does legal movement advance the missing leg without evacuating a completed leg? |
| Battle | Does Rando initiate a viable required battle and decline a doomed one? |
| Force loss | Are sole route enablers retained while duplicates remain expendable? |
| Forfeit | Are critical actors and carriers preserved when another legal loss exists? |
| Trigger | Does unchanged card Java perform the actual flip? |
| Back side | Does Rando hold required state, exploit payoff, and avoid flip-back or hard loss? |

Mark phases that do not apply. Do not invent behavior merely to fill the table.

## 6. Separate facts, policy, and adapters

Use this ownership split:

```text
ObjectiveAnalyzer or typed facts reader
  owns identity, source-derived facts, current state, and missing requirements

Shared phase policy
  owns score, ordering, reserve, release, and veto decisions for one action family

Rando and Chosen One adapters
  own engine-context extraction and application at the established evaluator ordering point
```

Keep scoring out of raw research data. Keep game-strategy scores out of card-law extraction.
Avoid adding another general evaluator when a live phase owner already exists.

## 7. Diagnose the actual losing comparison

Start with the replay or deterministic failing scenario. Reconstruct:

- Offered legal candidates.
- Board state and physical identities.
- Current Force and later obligations.
- Contributions from every live evaluator.
- Pass score and any categorical veto.
- The exact winner and why it won.

Fix the smallest wrong comparison. Do not retune a global score because one objective lost a tie.

Invasion Batch Zero illustrates the pattern:

1. The Throne Room pull eventually worked.
2. The next deploy plan still targeted Swamp.
3. The needed correction was not more card facts. It was a viable Throne formation outranking the
   generic plan while the site was empty.
4. Once the opponent built an overwhelming Throne stack, existing safety correctly rejected a
   suicidal recovery.

The policy therefore needed two tested boundaries: decisive when the route was viable, subordinate
when the route was doomed.

## 8. Do boundary math before choosing a magnitude

For an objective contribution `O`, strongest safe distraction `D`, Pass `P`, and safety veto `V`:

```text
viable objective route: O + existing_route_scores > max(D, P)
doomed objective route: O + V < safe_alternative_or_Pass
closed objective state: O = 0
wrong printing or objective: O = 0
```

Use actual numbers from the failing decision. Include all additive contributions, not only the new
policy. A hard veto should remain categorical when the engine says the action is illegal or the
board projection is genuinely terminal.

Avoid two opposite errors:

- Too weak: the objective score documents intent but never wins.
- Too broad: the objective score resurrects illegal, unaffordable, or suicidal actions.

## 9. Use the minimum sufficient test ladder

### Native engine contract

Arrange the exact source-defined state, emit the real timing event, and assert the unchanged
objective card flips or stays stable. Add near misses for every conjunction, alternative, strict
count, or actor condition.

### State and facts

Assert exact identities, current missing legs, route-open state, reserves, and sole-enabler roles.
Reject wrong printings and loose title matches.

### Policy boundary

Test positive, negative, release, and close conditions. Include the strongest known safety veto.

### Candidate decision

Offer:

- The advancing action.
- Pass.
- One plausible tactical distraction.
- One near-miss objective-looking action.

Assert the selected action and semantic rule ID through the real public bot adapter or
`CombinedEvaluator` surface.

### Persistent chain

Run the sequence across phases. Confirm that an earlier choice does not consume the Force, actor,
card, movement path, or formation required by the next choice. Require the real native flip when
possible.

### Parity

Run the same decision contract through Rando and Chosen One. Source-text parity is useful but does
not replace winner parity.

### Replay or controlled game

Use Rando versus Chosen One for automated games. Use Steve's `asdf` replay as authoritative manual
evidence. Inspect the replay board state first, then use logs to explain the choice.

## 10. Keep K2 strict in the useful places

Use strict gates for:

- Source truth.
- Typed legality.
- Tactical viability.
- Scope.
- Build and artifact integrity.
- Honest proof claims.

Use a recorded concern, not a work stoppage, for:

- A possible future generic abstraction.
- An unrelated evaluator overlap without evidence in the target decision.
- Missing live replay after deterministic native-flip proof.
- A low-probability board state outside the source-defined route.
- Cleanup or documentation debt outside the batch.

One unknown should produce one targeted negative test. It should not produce a week-long universal
model unless the test proves the mechanism is shared.

## 11. Avoid the recurring snag patterns

- Do not enable data and call the objective improved without a decision consumer.
- Do not demand a universal schema before fixing a typed objective route.
- Do not create many micro-commits and gates for one family. Seal one coherent batch.
- Do not let independent reviewers rewrite production code. Request findings and integrate once.
- Do not use a live replay as the only way to test an actual flip when the engine harness can do it.
- Do not wait for live replay proof before starting the next family after deterministic, package,
  and deployment gates are honestly recorded.
- Do not build or package from a shared dirty tree.
- Do not deploy over an active game.
- Do not count `target/classes` from another worktree as proof.
- Do not let a safety heuristic forbid every playable route. Require viability, then allow the best
  viable route to beat ordinary tactics.

## 12. Seal and report

Use one local commit per complete objective family. Update:

- `resources/AI_CHANGELOG.md`
- `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`
- The objective audit record and gap matrix proof classification
- Rulebook sources and generated output when the extractor accurately represents the new rules

Record separate claims for:

- Test pass.
- Package pass.
- Jar contents.
- Fresh JVM load.
- Rule firing.
- Replay observation.
- Native flip observation.

Never promote one proof layer into another.
