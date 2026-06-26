# K-2 HANDOFF — 2026-06-25: make Rando flip the Invasion objective

**For the new K-2.** Steve played a TF Battle-Droid Invasion deck; Rando deployed well (Fix #1 landed) but never flipped Invasion because it never controlled the Theed Palace Throne Room. Verified read-only against source + the post-fix game log. Reflects Steve's design call. Nothing applied.

---

## The card
`Card14_113` (Invasion / In Complete Control). Flips when BOTH:
- `controlsWith(Theed Palace Throne Room, Filters.Neimoidian)` — control the Throne Room WITH a Neimoidian there (`:178`).
- `controls(Naboo system)` (`:179`).

The Naboo-system half is already handled: the objective auto-deploys the Blockade Flagship to Naboo system on turn 0 (`:58-70`), and Rando held it all game (`Naboo [SPACE] CONTROLLED us:12 them:0`). **All the work is the ground Throne Room.**

## What we already have, and what blocks it
- V22 steering works: the parser marks the Throne Room objective-relevant and adds +150 to deploy there (`CardSelectionEvaluator.java:1648-1651`, value `ObjectiveAnalyzer.java:187`). The parser captures both flip locations.
- The blocker is the §A -1500 ability gate (`CharacterDeploySiteEvaluator.java:363-365`). The empty Throne Room returns -1500 for any low-ability body BEFORE the +150 matters, so the room stays empty. Log: `V136 ... Throne Room: §A=-1500 §B=300 total=-1200`. This is the not-yet-applied Fix #2.

## Steve's design call (how to steer the flip)
- **Control the Throne Room with ANY characters, a droid + buddy stack.** It is a contested, hard-to-hold site, and Neimoidians are weak (low power/ability), so a lone Neimoidian gets out-powered and loses control. Pile bodies to win and hold the power.
- **At least one body in that stack must be a Neimoidian.** The flip hard-requires it (`controlsWith(..., Neimoidian)`). The Neimoidian rides along with the droid/buddy stack; it does not carry the site alone.
- So the rule is: heavily prioritize CONTROLLING the flip-site with a stack, AND guarantee a Neimoidian is among the deployed bodies.

## The fix, in order
1. **Fix #2 first** (`CharacterDeploySiteEvaluator.java:363-365`): gate the -1500 on `oppPower` so low-ability bodies fall through to the +500 path at an uncontested site. ~95% this unblocks the room (proven: the same log shows §A=+500 once any friendly was present). Helps every deck seed uncontested objective sites. Do the boundary math first (additive-domination discipline), and mind V156 (`:429-434`) and the +500 reward (`:436`).
2. **Verify Fix #2:** a body now deploys to the empty Throne Room. No flip yet is the EXPECTED intermediate state.
3. **Parser enhancement** (`ObjectiveAnalyzer.parseFlipCondition`, `:522-589`): the qualifier "with a `<type>` there" is currently dropped (the location string captures `theed palace throne room (with a neimoidian there)` but nothing reads the type). Add a generic parse: a `Map<String,String>` of `location -> requiredType`, filled by one regex for the parenthetical form `\(with (?:a |an )?([A-Za-z' ]+?) there\)` and a second branch for the inline form `control X with <type>`. Expose `getRequiredTypeForLocation(title)`. This is GENERIC, not an Invasion hardcode, and also fixes ~10 other objectives (the Theed light-side mirror with Amidala `Card14_052:132`, Corellia/Rebel `Card601_146`, Dantooine/Rebel `Card7_135`, Ralltiir/Imperial `Card7_300`, and more). Per global-over-specific.
4. **Steering** (V22 at `CardSelectionEvaluator.java:1647-1652`, and deploy-side V29 at `DeployEvaluator.java:2417`): when a flip-site has a required type, keep the strong "control this site with a stack" push for all bodies (Steve's hold-it intent), AND add a large bonus to deploy a matching-type body there so the stack is guaranteed to include one. Withhold nothing from the droids, they are the muscle; just make sure a Neimoidian is steered into the same stack.

## Type resolution (Neimoidian)
- `Filters.Neimoidian = species(Species.NEIMOIDIAN)` (`Filters.java:18855`). There is NO `Keyword.NEIMOIDIAN`.
- Resolve in this order, belt-and-suspenders like the senator code (V88/V99, `CardSelectionEvaluator.java:1674-1678`): `getSpecies() == Species.NEIMOIDIAN` (authoritative; V121 already uses it at `:1986`), then any keyword, then `getLore().toLowerCase().contains("neimoidian")`. CONFIRMED: all three Neimoidians carry the species AND "Neimoidian" in lore (Dofine `Card12_103:41/45`, Tey How `Card12_121:41/45`, Rune Haako `Card14_087:38/43`). Either check catches them.

## Gotchas
- **V121 routes the Neimoidian PILOTS to the ship.** `CardSelectionEvaluator.java:1959-2026` forces Dofine + Tey How aboard the Blockade Flagship. So the Throne-Room Neimoidian should be a NON-pilot (e.g. Rune Haako) or the steering will fight V121 and lose. Prefer a non-pilot Neimoidian for the ground flip-site.
- **V51 OBJ FIRST (+300) does not fire here** (0 runtime hits; the Invasion deploys route through the CardSelectionEvaluator `V136 CS` path, not the DeployEvaluator path where V51 lives). Don't count on it.
- The room is contested in real games (the opponent out-powered Rando there last game), which is exactly why Steve wants the droid/buddy stack, not a lone Neimoidian.

## Verify end-to-end
Real TF Invasion-deck game (after `reload-ai`): confirm a Neimoidian plus droid/buddies deploy to and HOLD the Throne Room, Naboo stays controlled, and the card flips. Grep the container `nohup.out` for the flip. Compiled is not fired. See `resources/BUILD_AND_DEPLOY.md`.
