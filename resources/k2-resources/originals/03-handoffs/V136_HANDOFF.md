# V136 — Master Deploy Rule (PAUSED 2026-05-26)

**Status:** Spec v2 reviewed, 9 specific fixes identified, NOT YET CODED.

## Resume here

Two artifacts in `/tmp/` (will not survive reboot — back them up if needed):

- `/tmp/V136_SPEC_V2.md` — full v2 spec
- `/tmp/V136_RULE_CATALOG.md` — catalog of all 15+ character→site scoring rules with KEPT vs SUPERSEDED classification

If `/tmp/` is wiped, regenerate the catalog by running the Explore subagent with the same prompt against `rando/evaluators/{DeployEvaluator, CardSelectionEvaluator, MoveEvaluator}.java`.

## v2 verdict

- **Council engineer** (qwen3-coder:30b via deliberate endpoint): APPROVE-WITH-CHANGES
- **Subagent** (Claude general-purpose with full code-read access): APPROVE-WITH-CHANGES

Architecture is sound. 9 specific edits required before code:

### Real bugs in v2 spec

1. `getDestinyValue()` on spec line 79 should be `getPower()`. Destiny ≠ power.
2. Side-symmetry FAIL: class under `ai/models/common/strategy/` cannot import `rando.strategy.ObjectiveAnalyzer` or `rando.evaluators.DecisionContext`. Fix: pass primitives into method signature (`boolean isObjectiveRelevant`, `List<PhysicalCard> hand`) instead of side-specific objects.

### Design flaw

3. **Buddy-lookahead is symmetric → neither character deploys.** When Phasma + Trooper both in hand, both score -200 (because the OTHER one is in hand). Net: Rando passes deploy phase entirely. Fix: lower-ability character treats itself as "I am the buddy that the higher-ability character needs" and scores 0 instead of -200, so it deploys first. Then higher-ability character benefits from Case 4 (+600).

### Arithmetic errors in dominance table

4. Case 5: V96 fires +500, not +100 (diff is -1, well within ±10 window). Total: -910 not -1310.
5. Case 9: spec didn't specify opp power so V96 magnitude is ambiguous. Specify.

### Missing dominance rows

6. Ability exactly = 4 boundary (decide: `>= 4` or `> 4`?).
7. Aboard-ship deploy case (TIE Pilot aboard Vader's TIE in contested system).
8. V67bj + V136 stacking on Stormtrooper Patrol scenario (-400 + -1500 = -1900, both block but redundancy worth noting).

### Missing scoring row

9. No row covers `abilityPass OK + powerPass FAILS`. Vader (A=6 P=6) vs site with opp P=10 currently falls through to default 0 or worst-case -2000. Spec gap.

## When you resume

1. Read `/tmp/V136_SPEC_V2.md` (or regen via Explore subagent)
2. Apply the 9 fixes above
3. Either send to one more review round OR have Steve sanity-check v3 directly
4. Code: new file `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/CharacterDeploySiteEvaluator.java`
5. Comment out V90/V122/V67aj/V67as/V67al with pointer comments
6. Build clean
7. Subagent reviews implementation against spec
8. Steve test-plays 2-3 games before push

## Caveat

Steve paused V136 to investigate a real-game bug instead (2026-05-26):
- Stormtrooper left alone vs Jedi
- Rando didn't force drain turn 2
- Kylo Ren ship moved to system with Falcon + Han turn 4

Whatever bugs that investigation surfaces should feed back into V136's dominance table before code.
