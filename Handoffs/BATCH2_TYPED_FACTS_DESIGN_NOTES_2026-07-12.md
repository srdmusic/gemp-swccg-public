# Batch 2 — Typed DecisionFacts/ActionFacts design notes (pre-spec)
Inputs: Codex target runtime (m00214 §1), council review (deepseek-r1, 2026-07-12), K-2 deltas.

## Council verdicts (adopted unless noted)
1. ADD to ActionFacts beyond the base set: threat level at destination, survivability estimate,
   economic impact (deploy/maintenance cost vs budget), objective-alignment flag. K-2 delta: these are
   DERIVED facts — live in the facts object but computed by the shared services (FormationSafety weapon
   math, BattleFacts, TurnResourcePlan, ObjectivePlan) at normalization, so one implementation feeds all.
2. UNKNOWN handling: Optional<T> per field (no tri-states, no bitmasks). Fail-open convention stays:
   a rule that needs an absent Optional contributes nothing (never guesses).
3. Derived facts computed EAGERLY at normalization (board is small; immutability > laziness).
   K-2 delta: cap eager computation to the candidate set actually offered (not all 30 locations).
4. Shadow comparison: FREEZE the facts object into the fixture record at capture time (no re-derivation
   at compare time — re-derivation hides normalization drift, which is exactly what we're testing).

## Consumes
- DOMAIN_REGISTRY_2026-07-12.md (field needs per domain), Codex fixture-suite spec (pending),
  fixture harness (tools/fixture-harness, in flight).
