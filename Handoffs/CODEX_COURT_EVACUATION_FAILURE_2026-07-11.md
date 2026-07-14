# Codex audit: Court evacuation failure (latest replay)

Date: 2026-07-11 PDT

## Scope

- Replay: `replays/asdf/jmul5k9gge86f9c8.xml.gz`
- Rando copy: `replays/~Rando_Cal/bk29gs3twy175tsl.xml.gz`
- Runtime log: `logs/gemp-swccg.log`
- Java card truth: `Card110_006.java`, `Card200_131.java`
- AI source was read only. Codex made no Java edits.

## Verdict

CONFIRMED cross-phase evacuation-budget failure, compounded by a mixed-target V192 misclassification.

Rando correctly recognized that Tatooine: Great Pit Of Carkoon was unsalvageable, but spent 7 of 8 available Force on unrelated deploys. It then had enough Force to retreat only one of three endangered characters. Steve attacked on the next turn for 39 power against 3, dealing 36 battle damage and depleting Rando's Life Force.

## Replay evidence

Final-state event indexes in `jmul5k9gge86f9c8.xml.gz`:

- 04871: Rando deploys Jabba The Hutt to Great Pit.
- 04873: Rando deploys Velken Tezeri to Great Pit.
- 04882: Rando deploys Cad Bane to Great Pit.
- 05101: Steve transports C-3PO, Captain Han, Leia, and Chewbacca to Great Pit.
- 05165: Steve deploys Son Of Skywalker to Great Pit.
- 05334: Rando spends 4 Force to deploy Bossk In Hound's Tooth at Nal Hutta.
- 05354: Rando spends 3 Force to deploy 4-LOM at Beldon's Corridor.
- 05368: Rando spends its final Force moving only Cad Bane from Great Pit to Dungeon.
- 05468: Steve initiates battle at Great Pit.
- 05702-05704: dark power 3, light power 39, light wins.
- 05706-05773: 36 battle damage remains after attrition; Rando's Life Force is depleted.

## Log evidence

### The planner knew the site was losing

- `logs/gemp-swccg.log:60051`: Rando has 8 Force and 11 Life Force.
- `logs/gemp-swccg.log:60056`: Great Pit is LOSING, 9 power versus 23.
- `logs/gemp-swccg.log:60077`: planner wants 4-LOM at Great Pit to cover a 14-power deficit.
- `logs/gemp-swccg.log:60401`: V172 then correctly declares the site unsavable by deploys and says to retreat instead.

### V192 overvalued Court's mixed target action

- `logs/gemp-swccg.log:60149-60152`: Court's pull scores 2955 and wins.
- `logs/gemp-swccg.log:60164-60179`: Bossk In Hound's Tooth is the only selectable target and is chosen.
- `logs/gemp-swccg.log:60190`: the pull costs 4 Force.
- `logs/gemp-swccg.log:60154-60170`: the selection step proves this execution is a starship pull, not a location pull.

The 2955 score includes a location tier derived from the other half of Court's OR text:

- `logs/gemp-swccg.log:60142-60145`: V82/V192 sees `docking bay` in source text and grants LOCATION Tier 1 (+1500).
- `logs/gemp-swccg.log:60146`: V67ak also grants +800 for parsed token `independent`.

Actual card source confirms one action uses a union filter:

- `Card110_006.java:85-96`: `docking_bay OR (INDEPENDENT AND starship)`.
- `Card200_131.java:36-43`: Bossk In Hound's Tooth is an Independent starship, not a location.

The AI source classifies the whole source action from any location-like parsed target:

- `ActionTextEvaluator.java:5009-5027`: one source target containing a location keyword sets `v67lAddsLocation`.
- `ActionTextEvaluator.java:5161-5215`: that boolean grants the objective LOCATION tier.

Therefore a mixed `location OR non-location` action receives location priority even when only the non-location branch can execute.

## Confirmed structural and parser blast radius

Court is not an isolated card-text shape. The following Java sources each implement one Reserve Deck action with a union filter containing both a location and a non-location target. V192 grades the source action before the later card-selection decision reveals which branch will execute.

| Source card | Java evidence | Mixed branches |
|---|---|---|
| Court Of The Vile Gangster | `set110/dark/Card110_006.java:85-96` | docking bay OR Independent starship |
| I'm Sending My Apprentice | `set501/dark/Card501_041.java:69-76` | Sidious' Lightsaber OR Coruscant Naboo site |
| Ket Maliss (V) | `set601/dark/Card601_159.java:66-77` | five named characters OR docking bay |
| Mobilization Points | `set9/dark/Card9_129.java:59-66` | five systems OR Executor |
| Moff Jerjerrod | `set9/dark/Card9_117.java:60-67` | Death Star II sector OR Superlaser Mark II |
| Firin Morett | `set7/light/Card7_017.java:58-65` | two non-location cards OR docking bay |
| Massassi Base Operations back | `set111/light/Card111_004_BACK.java:73-80` | Rebel Tech, Death Star system, Attack Run, OR Proton Torpedoes |
| This Deal Is Getting Worse... | `set109/dark/Card109_012.java:81-88` | two Bespin locations OR Dark Deal / Cloud City Occupation |
| You Cannot Hide Forever & Mobilization Points | `set12/dark/Card12_144.java:112-119` | five systems OR Executor |

The deployed `web.jar` was then probed through each bot's real
`DeckOracle.parseSourceCardPullTargets`. Rando and Chosen One returned identical
mixed lists for all nine sources:

| Source | Deployed parser output |
|---|---|
| Court | `docking bay`; `independent starship starship` |
| I'm Sending My Apprentice | `sidious' lightsaber`; `coruscant naboo site` |
| Ket Maliss (V) | five named characters; `docking bay` |
| Mobilization Points | five named systems; `executor` |
| Moff Jerjerrod | `death star ii sector`; `superlaser mark ii` |
| Firin Morett | `advance preparation`; `rebel planners`; `docking bay` |
| Massassi Base Operations back | `rebel tech`; `death star system`; `attack run`; `proton torpedoes` |
| This Deal Is Getting Worse... | `bespin system`; `bespin: cloud city`; `dark deal`; `cloud city occupation` |
| You Cannot Hide Forever & Mobilization Points | six location-family targets; `executor`; `death star ii effect that deploys for free` |

Only Court was runtime-reproduced in this audit. The other eight are now both
source-confirmed and deployed-parser-confirmed instances of the vulnerable
mixed-target shape, not claims that their actions misfired in this replay.

This argues against a Court title exception. The correction belongs at the branch-resolution boundary: do not award a category tier for a union branch until the executable/selectable target set proves that category.

#### Existing V190 resolver is the nearest generic fix boundary

At the source-action decision, the AI receives action text and source card ID,
not the `DeployCardFromReserveDeckEffect` filter or the later selectable-card
array. The exact selectable set appears only after the source action wins
(`logs/gemp-swccg.log:60164-60179`). Therefore V192 cannot literally grade the
later selection without new engine-to-AI decision metadata.

There is already a generic current-Reserve approximation in
`DeckOracle.reservePullFetchesOnlyStarships()` (`DeckOracle.java:548-592`). It
parses every union branch, resolves those targets against cards currently in
Reserve, handles `docking bay` as an exact title class instead of generic
LOCATION, and returns true only when every resolved current target is a
starship. V190 consumes it in `DeployEvaluator.java:848-873`. Court's later
selection confirms that this resolver's intended state is exactly the failing
case: Bossk is selectable; the other shown cards are not.

V192 does not use that resolver. Its V131 path calls
`resolveCommonNounToFilter()` (`DeckOracle.java:709-755`), which has no
`docking bay` mapping, while the V67h fallback maps `docking bay` to the broad
LOCATION category (`DeckOracle.java:1543-1581`). V192 consequently receives no
branch-specific signal that the docking-bay branch is empty; its parser-level
location flag remains open.

Lowest-risk generic implementation shape: factor V190's current-zone
resolution loop into a method returning the categories/cards actually matched
by the parsed branches, then let both V190 and V192 consume it. V192 awards a
LOCATION tier only when that resolved current set contains a location. If no
target resolves, fail open to the existing score. This remains an
approximation because source-text parsing cannot see dynamic game-text
modifications; exact parity requires exposing eligible target metadata on the
source action decision.

#### V67ak key-character bonus is also type-blind

Court's score contains a separate false +800:
`logs/gemp-swccg.log:60143` calls parsed token `independent` a key character.
Here `Independent` is the starship icon branch, and Bossk In Hound's Tooth is
the only selectable card.

The root cause is generic. `ObjectiveAnalyzer.getStrategyCharacterTokens()`
extracts capitalized phrases from the objective plus every friendly persistent
Effect (`ObjectiveAnalyzer.java:992-1045`) and filters them only through a
stopword list (`:1048-1076`). It never proves that a token names a character or
persona in the deck. `ActionTextEvaluator` then grants +800 when any parsed pull
target contains any such token (`ActionTextEvaluator.java:5493-5545`), again
without checking the target category. Non-character tokens can never be
"filled" by its later in-play character scan, so the bonus repeats.

Current-log corpus: 79 V67ak pull bonuses, grouped exactly as follows:

| Count | Source | Matched token | Target type |
|---:|---|---|---|
| 37 | Power Of The Hutt | `hutt influence` | Effect |
| 19 | Court Of The Vile Gangster | `independent` | Starship icon branch |
| 13 | Jabba's Haven | `nal hutta` | Location |
| 5 | Endor Shield | `admiral` | Generic character class, not a named persona |
| 3 | I'm Sorry | `cloud city` | Location phrase |
| 2 | This Deal Is Getting Worse All The Time | `dark deal` | Effect |

Thus 74 of 79 fires are definitively non-character, and the remaining five are
a generic class rather than the named-persona use documented by V67ak. Minimum
guard: a pull target may receive V67ak only when the resolved current target is
a CHARACTER and the token is grounded to an actual character title/persona.
The resolved-target helper proposed for V192 can supply the category. Grounding
the token against player-owned character blueprints/personas prevents the
capitalized-noun scanner from manufacturing strategy characters.

### Deploy consumed the evacuation reserve

- `logs/gemp-swccg.log:60401-60404`: Great Pit is unsavable and 4-LOM's planned destination scores -430.
- `logs/gemp-swccg.log:60440-60444`: Beldon's Corridor scores 930, so Rando spends 3 Force there.
- `logs/gemp-swccg.log:60580-60605`: three Great Pit retreat actions receive the same R3 score, 12260.
- `logs/gemp-swccg.log:60612-60630`: the tie resolves to Cad Bane, with only Dungeon available as destination.

The issue is not that retreat failed to trigger. Retreat triggered correctly, too late to evacuate the group.

#### Retreat tie evidence grade

Replay decision parameters map the three equal R3 actions precisely:

- action 8, card 332: Jabba The Hutt (V), base power 4, ability 3, forfeit 7 (`Card200_084.java:46`).
- action 9, card 333: Velken Tezeri (V), base power 2, ability 2, forfeit 4 (`Card205_016.java:36`).
- action 11, card 337: Cad Bane, base power 4, ability 4, forfeit 5, armor 5, and a battle-destiny ability (`Card203_024.java:40-46`).

All three scored 12260, so action order selected Cad. That is nondeterministic from a strategy perspective, but this replay does not prove Cad was the wrong survivor. A simple power + ability + forfeit comparison is Jabba 14, Cad 13, Velken 8, and Cad's armor/battle-destiny text can reasonably close the one-point gap. In the next battle Velken was directly lost to Leia's Blaster Rifle, while hit Jabba was forfeited for 7.

Evidence grade: value-aware tie-breaking is a quality improvement, not a confirmed cause of this loss. The confirmed cause is that only one of three retreat actions could be paid for.

### V61c withheld three Force; evacuation was short by two

- `logs/gemp-swccg.log:59847-59853`: with Reserve Deck 10 and Force Pile 1, V61c activates only 7 of 10, preserving 3 cards for battle destiny. Rando begins the action phases with 8 Force instead of 11.
- `logs/gemp-swccg.log:60056-60067`: the planner sees exactly one contested location, Great Pit, and classifies it LOSING at 9 versus 23. It finds zero winning and zero attack locations.
- `logs/gemp-swccg.log:60525-60532`: BattleEvaluator later reaches the same 9-versus-23 state, blocks MUST-FIGHT as suicide, and scores battle -810 versus Pass -5.

`DecisionContext.isBattlePlausibleThisTurn()` currently returns true for any location where both sides have power (`DecisionContext.java:309-335`). It does not ask whether any battle is viable. ForceActivationEvaluator then keeps three cards whenever that coarse predicate is true (`ForceActivationEvaluator.java:197-214`).

In this replay the only contested site was one Rando correctly refused to battle at. The coarse predicate therefore preserved destiny for a battle that the battle evaluator would not take. The move phase needed three Force and still had one, so the actual evacuation shortfall was two Force:

- Full activation: 10 Reserve + 1 Force Pile = 11 usable Force.
- Actual unrelated spending: Bossk 4 + 4-LOM 3 = 7 Force.
- Remaining after full activation: 4 Force, enough for all three one-Force retreats.
- Remaining after V61c activation: 1 Force, enough for Cad Bane only.

This is a direct contradiction of V61c's stated rule: save destiny when Rando intends to battle, activate fully when it will deploy and end without battling. `contested` is not a sufficient proxy for `intends to battle`.

#### Causal sufficiency matrix

Three independent corrections each prevent the stranded group in this replay:

| Counterfactual | Force entering move | Retreat capacity | Evidence |
|---|---:|---:|---|
| Actual | 1 | 1 of 3 | 8 activated, Bossk 4, 4-LOM 3 |
| V61c viability fix only | 4 | 3 of 3 | Full activation gives 11; both deploys still cost 7 |
| V192 branch-grade fix only | 5 | 3 of 3 | Removing the false +1500 location tier drops Court from 2955 to 1455, below the zero-cost Passenger Deck at 2730; Bossk is not pulled |
| Deploy reserve fix only | 4 | 3 of 3 | After Bossk costs 4, skip 4-LOM's unrelated 3-Force deployment and preserve the move budget |

This replay does not require all three fixes to change its outcome. V192 and V61c are independently incorrect decision boundaries. Cross-phase retreat reservation is additional defense against the same failure when activation or pull scoring is otherwise valid.

#### Cross-stage retreat-budget contract

The source currently discovers the retreat requirement after the deploy action
has already won:

- `DeployPhasePlanner.java:982-1020`: `generateReinforcePlan` accepts any
  non-empty affordable combination; it does not require the combination to
  make the losing location viable. This produced the `REINFORCE` plan.
- `DeployEvaluator.java:942-980`: V169 gives every deploy umbrella action +500
  whenever any friendly location is outpowered. It does not share V172's
  reinforceability brake.
- `CardSelectionEvaluator.java:947-984`: only the later destination decision
  computes that Great Pit is unsavable and logs `retreat instead`.
- `DeployEvaluator.java:985-1025`: V176 already establishes the phase-level
  resource-reservation pattern, but only for a one-Force battle fee when the
  current pile is already `<= 2`. It does not compare post-deploy Force against
  a retreat budget.

Court demonstrates the contract gap: 4-LOM's source deploy scored 700, including
V169 +500. The Great Pit destination then scored -430 as unsavable, so the
chooser diverted 4-LOM to Beldon's Corridor at 930 and spent 3 Force anyway.

Minimal acceptance boundary for a phase-level guard:

- Great Pit unsavable, three legally movable friendlies, 4 Force available,
  4-LOM cost 3: the deploy must not spend below the three-Force retreat budget.
- With at least 6 Force available, the same 3-Force deployment may proceed if
  it still leaves all three one-Force retreats payable.
- Do not reserve Force for bodies with no legal escape, and do not block a
  reinforcement that actually makes the site viable.

The comparison must use projected post-action Force (`available - deployCost`),
not V176's current-pile threshold. Prefer a shared unsavable/retreatable
predicate so planner, deploy source scoring, destination scoring, and move
scoring do not disagree again.

#### Current-log V61c corpus

The current runtime log contains exactly three `V61c DESTINY BUFFER (keep 3 in reserve)` decisions:

- `logs/gemp-swccg.log:14683`: correct hold. Rando later identified favorable contested sites (`:15545-15548`) and initiated battle (`:15555-15557`).
- `logs/gemp-swccg.log:32975`: correct hold. Rando later saw Dining Room at 11 versus 5 (`:33293`) and initiated battle (`:33302-33304`).
- `logs/gemp-swccg.log:59847`: false hold. Great Pit was 9 versus 23, BattleEvaluator blocked the suicide attack, and Pass beat battle -810 (`:60526-60532`).

Result: two true positives and one false positive. The buffer should be refined, not removed. Acceptance requires preserving the first two battle-intent cases while fully activating when every contested location is non-viable, as in the Court replay.

#### V61c viability acceptance matrix

Do not replace `any contested location` with a current-board-only favorable
battle check. One valid hold depended on a deploy-created battle:

| V61c event | State after the keep-3 activation | Why the hold is correct or wrong |
|---|---|---|
| `:14683` | Force 12; Beldon's 6 vs 5; Guest Quarters 8 vs 12; hand includes Boba P7/C5 (`:14885-14894`) | Correct. Boba is affordable and projects Guest Quarters to 15 vs 12; Rando then initiates (`:15545-15557`). |
| `:32975` | Force 8; Dining Room already 11 vs 5; no characters in hand (`:33125-33143`) | Correct. A favorable current battle already exists and Rando initiates (`:33293-33304`). |
| `:59847` | Force 8; Great Pit 9 vs 23; affordable reinforcement only +2 (`:60056-60067`, `:60401-60404`) | Wrong. No current or affordable projected battle is viable; Rando refuses battle (`:60525-60532`). |

Required predicate shape:

`battleIntent = currentViableBattle || affordableDeployWaveCreatesViableBattle`

For the activation decision, affordability must use projected Force after the
keep-3 candidate amount:
`currentForce + min(maxAvailable, max(0, reserve - 3))`. If that budget cannot
create a viable battle, activate fully. Reuse one shared
battle-viability calculation, including ability, opponent weapons, and pyrrhic
hit economics. A raw-power-only copy will disagree with BattleEvaluator again.

#### V61c helper-boundary audit

There is no existing callable helper that implements the required predicate:

- `CardSelectionEvaluator.v173WaveProjection()` (`:5954-6046`) is private,
  subtracts one nominated deploy card from the current Force pile, counts every
  hand character without checking whether it can legally deploy to the target,
  and does not apply BattleEvaluator's hit-economics veto. It cannot be reused
  directly for activation intent.
- `DeployPhasePlanner.generateStopBleedingPlan()` and
  `generateAttackPlan()` (`:914-973`, `:1250-1292`) do filter legal deploys and
  require a power/ability goal, but they are private. The public `createPlan()`
  reads the current pre-activation Force pile and caches/mutates the turn plan
  (`:113-138`, `:304-308`), so calling it speculatively during activation would
  use the wrong budget and poison the later deploy plan.
- `BattleEvaluator` keeps weapon, ability, and pyrrhic checks inline inside
  action scoring (`BattleEvaluator.java:187-538`, `:545-664`).
  `BattlePredictor.shouldInitiateBattle()` is not a substitute: it models only
  power and destiny (`BattlePredictor.java:197-245`), and omits weapon-hit loss.

The shared predicate is called from decisions that do not all carry the
activation integer's `max` parameter: the V168 action choice, V38.3 pass
confirmation, V192 pull stand-down, and ForceActivationEvaluator amount choice.
Therefore it must not derive projected Force from `DecisionContext.getMax()`.
The engine's own remaining-activation formula is available from game state:

`min(floor(totalForceGeneration - forceActivatedThisTurn(fromGeneration)), reserveDeckSize)`

That is the exact `maxToActivate` calculation used to construct the integer
decision (`AbstractSwccgCardBlueprint.java:2237-2243`). Use this state-derived
amount inside the one shared predicate so all four callers receive the same
answer. Then evaluate current battles plus location-specific, legally
deployable combinations against an explicit projected Force budget, preserving
the battle-initiation fee and maintenance obligations. Do not call
`DeployPhasePlanner.createPlan()` or use the global-hand V173 projection as a
shortcut.

## Required behavior boundary

1. Mixed-target pull grading must reflect the currently executable target branch. Court must not receive a location tier when the only selectable card is an Independent starship.
2. V61c must not preserve battle destiny solely because a hopelessly outmatched site is contested. Battle plausibility must agree with battle viability.
3. When a friendly occupied site is marked unsavable, deploy-phase spending must preserve enough Force to execute the available retreat plan before unrelated deploys.
4. If equal R3 retreat candidates are re-ranked, use a deterministic value-aware tiebreak. Do not encode this replay as proof that Cad was the wrong survivor.

## Suggested acceptance fixture

State from this replay:

- Force: 8.
- Great Pit: Rando 9, opponent 23.
- Endangered Rando characters: Jabba, Velken, Cad Bane.
- Hand: 4-LOM, None Shall Pass, Passenger Deck.
- Court pull can select Bossk In Hound's Tooth only.

Expected:

- Activation does not keep three cards for the 9-versus-23 non-battle.
- Court does not receive LOCATION Tier 1 for the Bossk-only execution.
- At least 3 Force remains for three one-Force retreats.
- All movable Great Pit characters retreat before Rando passes move phase.
