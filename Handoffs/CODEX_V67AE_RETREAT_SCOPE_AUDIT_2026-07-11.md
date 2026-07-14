# V67ae retreat-scope audit

Date: 2026-07-11
Owner: Codex/Alfred, read-only verification
Tested source: HEAD `326895c77`

## Finding

V67ae now scans only locations in the destination's system when deciding
whether a doomed friendly card justifies retreat to a zero-drain destination.
That scope is valid for some location-text moves, but not for all actions the
branch accepts.

Current branch:

- Trigger: `ActionTextEvaluator.java:3643-3645`
- Same-system restriction: `ActionTextEvaluator.java:3675-3684`
- Doomed-gap test: `ActionTextEvaluator.java:3685-3715`

## Source boundaries

### Broad-origin actions currently caught by V67ae

| Source | Displayed action | Actual origin filter |
|---|---|---|
| `Card209_050.java:49-65` | `Move from other battleground site to here` | any other battleground site |
| `Card208_054.java:50-65` | `Move Kylo from a battleground to here` | any other battleground location |
| `Card221_048.java:64,118-135` | `Move from a site you occupy to here` | any other occupied site |

These filters are not restricted to the destination's system. A doomed legal
mover at another system must be considered by the retreat exemption.

### Theater-scoped action

`Card5_084.java:56-67` displays `Move from other Cloud City site to here` and
uses an `otherCloudCitySite` origin filter. Destination-system/theater scoping
is appropriate for this action.

### Trigger coverage gap

`Card601_151.java:127-133` provides a global occupied-site relocation, but its
displayed text is `Relocate characters to here`. The current test for literal
`relocate to here` does not match because `characters` appears between those
words. This action therefore bypasses V67ae entirely.

## Why the evaluator cannot inspect legal origins

The engine calculates the exact legal origin set in
`MoveUsingLocationTextAction`:

- Constructor filters candidate origins and builds `validFromCards`.
- The list is a constructor local, then captured by a private targeting effect.
- There is no public getter for the legal origins.

The AI decision layer removes the typed action before evaluation:

- `CardActionSelectionDecision.java:12-35` keeps `List<Action> _actions`
  private and publishes only flattened parameters.
- `DecisionContext.java:44-52` stores action IDs/text, card IDs, blueprint IDs,
  and selection metadata. It does not retain the `Action` object or origin set.

Therefore `ActionTextEvaluator` cannot implement an exact legal-origin scan
without an API change.

## Repair boundary

### Minimum AI-only repair

Classify scope from the selected action text:

- Use theater/system scope only for explicit phrases such as `related`,
  `Cloud City`, `Jabba's Palace`, `Death Star`, or a named origin site.
- Use global friendly-location scope for broad phrases such as `other
  battleground`, `a battleground`, `a site you occupy`, `another site`, and
  unqualified `move ... to here`.
- Widen relocation detection to word order, not the exact substring
  `relocate to here`.

### Exact generic repair

Expose the already-computed legal origin IDs from
`MoveUsingLocationTextAction`, carry them through the awaiting-decision
parameters and `DecisionContext`, then run the doomed-gap test only on those
origins. This avoids card names and text parsing, but crosses the cards, logic,
and AI module boundary.

No Java files were edited by Codex.
