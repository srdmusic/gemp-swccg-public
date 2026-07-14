# Codex Independent Root-Cause Audit: Solo, Ability, Move, and Battle Safety

Date: 2026-07-11 PDT  
Build under test: `79498fb5b5e8bc42ada7d35b1615b212beceb319`  
Ownership: Codex read-only verification for K-2 message `m00174`  
Code changes: none

## Verdict

This is a route-coverage and dominance failure, not four unrelated card bugs.

| Hypothesis | Verdict | Evidence |
|---|---|---|
| H1: route gaps | **CONFIRMED** | The final move destination route has no generic origin-cohesion, destination-ability, or destination-winnability veto. Location-sourced moves have only a drain guard. Battle actions without a location fall through to generic scoring. |
| H2: additive penalties lose to bonuses | **CONFIRMED** | `-150`, `-300`, and `-500` safety penalties are routinely defeated by the MoveEvaluator `R2 +6000` band and CardSelection bonuses up to `+700`. |
| H3: V171/V172 opened a bypass | **PARTIAL, CONFIRMED FOR V171** | V172 SOLO DOMINANCE did not fire on the bad incidents. V171 did fire on the non-character starship First Light because V171 has no deploying-card category gate. It borrowed hand-character wave power and added `+600`. |
| H4: destiny eligibility modeled incorrectly | **CONFIRMED** | The engine threshold is total ability `>=4`, subject to modifiers. BattleEvaluator V164a instead accepts `ourAbility >= theirAbility`, and its predictor forces at least one draw even when actual ability cannot draw. |

## Incident Evidence

### 1. Greedo deployed solo while two affordable buddies were in hand

Replay: `replays/asdf/jc20n39malc6komb.xml.gz`, final segment.

| Evidence | Result |
|---|---|
| Replay event `2011` | Rando deploys Greedo (V) alone to Tatooine: Desert Heart on turn 2. |
| `logs/gemp-swccg.log:38063` | Hand contains two Alien Mobs plus Greedo. |
| `logs/gemp-swccg.log:38327-38362` | Site selection scores Desert Heart `1420`: planned `+200`, objective `+150`, shared V136 `+800`, icons `+30`, own-site `+40`, battleground `+80`. |
| Missing guard | Greedo ability 1 does not qualify for V113, and shared V156 skips objective-relevant sites. |
| Replay events `2055-2104` | Steve deploys Anakin and Yoda, hits Greedo, wins `10-3`, forfeits Greedo, then inflicts 7 additional damage. |

The buddy-in-hand fact was known. It did not constrain the selected site or require the second deploy.

### 2. First Light was deployed and then battled without battle-destiny eligibility

Replay: `replays/asdf/jc20n39malc6komb.xml.gz`, turn 2.

| Evidence | Result |
|---|---|
| Replay event `2007` | First Light deploys alone to Nal Hutta against Falcon plus Han. |
| `logs/gemp-swccg.log:38184-38205` | V171 incorrectly applies to a STARSHIP and gives `+600`; the `-80` space disadvantage loses. Final site score is `650`. |
| `logs/gemp-swccg.log:38945-38972` | BattleEvaluator sees power `5-6`, ability `3-3`, awards V164a `+40`, merges ActionTextEvaluator fallback `+30`, starts from BattleEvaluator base `100`, and retains only a `-40` force warning. Final score is `130`. |
| Replay events `2017-2033` | Rando initiates. Rando draws no battle destiny. Steve draws 1, wins `7-5`, and inflicts 2 damage. |

V171's missing character gate is a direct regression. The battle decision is a separate threshold-model bug.

### 3. Chiraneau moved out of a group, leaving two weak solos

Replay: `replays/asdf/95s10zqy7sl0c177.xml.gz`, turn 3.

| Evidence | Result |
|---|---|
| Replay event `6677` | Chiraneau leaves Ozzel at Upper Walkway and moves alone to empty Guest Quarters. |
| `logs/gemp-swccg.log:25210-25250` | MoveEvaluator knows this leaves Ozzel alone and below destiny ability: V27 `-150`, V32 `-300`, V22.2 `-80`. V31 adds `+200`, then the ladder adds `R2 +6000`; final move-action score `5680`. |
| `logs/gemp-swccg.log:25259-25276` | Destination route adds `+327.5` for icons, drain, battleground, and V24.9. It does not evaluate Chiraneau's ability or origin consequences. |
| Replay events `6717-6770` | Steve deploys Rey plus a lightsaber, attacks Guest Quarters, wins `10-2`, forfeits Chiraneau, and inflicts 2 additional damage. |

The safety rules fired and lost. This is conclusive H2 evidence.

### 4. Tarkin moved solo into Rey and left Boba destiny-ineligible

Replay: `replays/asdf/95s10zqy7sl0c177.xml.gz`, turn 4.

| Evidence | Result |
|---|---|
| Replay event `6855` | Tarkin leaves Boba at Upper Walkway and moves alone into Rey at Guest Quarters. |
| `logs/gemp-swccg.log:28120-28172` | MoveEvaluator applies V32 `-300` and V22.2 `-120`, but V31 `+200` and `R2 +6000` dominate. Final move-action score `5863.5`. |
| `logs/gemp-swccg.log:28179-28193` | Destination route gives V41 `+700`; `Enemy too strong` is only `-30`. Final destination score `777.5`. |
| Replay events `6910-6954` | Steve hits Tarkin, wins `16-7`, Tarkin is lost, and Rando loses 3 additional Force. |

Tarkin can draw destiny, but the actual moved team still fails destination winnability. Destiny eligibility alone is not permission to charge.

### 5. Hondo moved alone into Anakin plus Yoda

Replay: `replays/asdf/jc20n39malc6komb.xml.gz`, turn 3.

| Evidence | Result |
|---|---|
| Replay event `2160` | Hondo leaves the Audience Chamber group and moves alone to Desert Heart. |
| `logs/gemp-swccg.log:42135-42168` | MoveEvaluator gives every offered mover the same global `ATTACK 3 enemies with 15 power (+12 advantage)` score, despite the selected mover being Hondo. Move action wins at `101.5`. Destination then scores `1045`: V166 `+300`, V41 `+700`, and only `-40` for enemy power 8. |
| Replay events `2288-2471` | Steve attacks, hits Hondo, and wins `23-8`. |

The top-level move score is destination-blind and mover-insensitive. The destination route knows the enemy stack but has no generic winnability veto.

## Engine Truth: Battle Destiny Requires Ability 4

Primary source: `src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/logic/modifiers/querying/BattleDestiny.java:31-91`.

The engine sets `abilityBasicThreshold = 4` and grants the normal draw only when `abilityForBattle >= abilityBasicThreshold`, after applying threshold and hard-requirement modifiers. Therefore:

- Ability 3 versus ability 3 does not mean both sides draw.
- Relative ability difference cannot substitute for eligibility.
- A predictor must not force `myDraws >= 1`.
- The safest pre-battle model is an engine-aligned eligibility query or a shared helper that mirrors the engine threshold and exception modifiers.

## Coverage Matrix

Legend: `LIVE` means the rule executes on the route; `INEFFECTIVE` means it executed but was additive or scoped incorrectly; `ROUTE-DEAD` means code exists but cannot see the needed data on the reproduced action shape; `NONE` means no enforcing rule.

| Law | DeployEvaluator | CardSelection deploy | CardSelection move destination | MoveEvaluator | ActionTextEvaluator | BattleEvaluator | Shared CharacterDeploySiteEvaluator |
|---|---|---|---|---|---|---|---|
| L1: do not abandon a remaining solo ally | N/A | N/A | **NONE**: destination-only; V156 checks whether the mover started solo, not who remains | **INEFFECTIVE**: V27 `-150/-250/-400` at `866-924`; V32 `-300/-500` at `930-991`; both lost to R2 `+6000` | **NONE** for location-sourced moves; V67ae only checks drain value | N/A | N/A |
| L2: do not voluntarily battle with zero actual battle-destiny draws | V32 ground adds `0` when no follow-up and V35 ship is only `-50`; no enforcement | Shared V136 penalizes contested ability `<4`, but bonuses remain additive | **NONE**: no destination eligibility guard | V137 requires ability 4 only when it can resolve a destination; generic landspeed action text cannot, so **ROUTE-DEAD** here | Battle branch uses continuous ability difference and locationless fallback `+30`; **NONE** | **WRONG MODEL**: V164a accepts equal ability at `644-657`; predictor forces at least 1 draw at `434-446` | `abilityPass >=4` at `253-255`; contested weak team `-1500/-200` at `428-439`, numeric only |
| L3: weak solo deploy with affordable buddy must hold or complete the group | **INEFFECTIVE**: V29 paired only detects; V38 is `-150`; V32 says follow-up in hand means deploy freely with `0` | **GAPPED**: V29.5 own-site `+40`; V113 only ability `>=3`; V156 is skipped at objective-relevant sites; Greedo ability 1 escapes | N/A | N/A | N/A | N/A | **PARTIAL**: V156 `-600` at `563-582`, but objective-relevant carve-out at `565` and numeric scoring permit bypasses |
| L4: do not move a solo body into an unwinnable enemy stack | N/A | V136/V171 are deploy-only | **NONE GENERIC**: objective-specific V67aa exists, while V41 gives any mover `+500/+700` at `6907-6935` | **ROUTE-DEAD on generic landspeed**: V137 searches action display text for destination at `1417-1428`; display text is only `Move using landspeed`. It also projects the whole origin group, not the single moved body | **NONE** for location-sourced moves; no formation or can-win guard | Can discourage a later battle, but cannot undo the bad move | N/A for moves; shared `MovePredicates.canWinAt` is reusable |

## Hypothesis Details

### H1: route gaps

Confirmed gaps:

1. `CardSelectionEvaluator.evaluateMoveDestination` has the mover and destination but no shared formation or winnability veto.
2. `MoveEvaluator` has V137, but generic `Move using landspeed` actions do not name the destination. V137 cannot resolve one and does nothing.
3. `ActionTextEvaluator` location-sourced movement bypasses MoveEvaluator's formation logic.
4. Locationless battle actions make ActionTextEvaluator add a default `+30`; BattleEvaluator scans all contested locations and can approve the wrong eligibility model.

### H2: additive penalties outvoted

Confirmed. Chiraneau and Tarkin show the exact failure mode. Safety penalties total hundreds; R2 adds 6000. CardSelection then independently rewards the unsafe destination. A larger negative number is another temporary point patch, not a class fix.

### H3: V171/V172

- V172 SOLO DOMINANCE did not fire on Greedo, Chiraneau, Tarkin, Hondo, or the First Light battle decision.
- V171 did fire on First Light's deployment. `CardSelectionEvaluator.java:1038-1146` lacks a character-category gate. `v171ThisPower` remains zero for the starship, but `v173WaveProjection` supplies power from characters in hand. The `theirEff - 2` tolerance then awards `+600` to a ship that still has ability 3 and cannot draw normal battle destiny.
- Other V171/V172 log hits in `95s10zqy7sl0c177` concern character reinforcement and are not the cause of the reproduced bad moves.

### H4: ability and destiny

Confirmed in three places:

1. BattleEvaluator V164a compares relative ability instead of first checking actual draw eligibility.
2. BattlePredictor estimates draws by character count and clamps each side to at least one draw.
3. ActionTextEvaluator's battle fallback adds `+30` when location text is absent, with no ability gate.

## Structural Recommendation

Do not add four more score penalties. Add one non-additive safety layer and call it from every route that has enough facts.

### 1. Add a real veto state to `EvaluatedAction`

Add `hardVeto` plus `vetoReason`; merge it with logical OR in `mergeFrom`. `CombinedEvaluator` must never select a vetoed action, regardless of additive score or ladder rank. This replaces magnitude warfare with an actual invariant.

### 2. Add a shared formation safety helper

One common helper should answer:

- `canDeployCharacterTo(card, destination, hand, force, objectiveFacts)`
- `canMoveCharacterTo(mover, origin, destination, movementMode)`
- `canInitiateBattleAt(location)`

Use engine totals and actual moved bodies. Do not assume the whole origin group will follow a one-card landspeed move.

### 3. Enforce at the route where all facts exist

| Route | Required enforcement point |
|---|---|
| Character deploy | `CardSelectionEvaluator` final destination selection, with a pre-check in `DeployEvaluator` only to avoid offering actions whose every destination is vetoed |
| Generic landspeed/shuttle | `CardSelectionEvaluator` final destination selection; `MoveEvaluator` should rank only destinations that pass the same helper |
| Location-sourced move-to-here | `ActionTextEvaluator` knows the destination; the subsequent mover selection must call the same helper before choosing a card |
| Battle initiation | Both `BattleEvaluator` and ActionTextEvaluator must use the shared eligibility result; a veto from either route survives merge |

### 4. Invariants and narrow exemptions

Apply these as vetoes:

1. Origin: moving this card must not leave one vulnerable friendly behind, unless the origin is doomed and the move is a real survival retreat.
2. Destination: a contested destination must pass `MovePredicates.canWinAt` using destination allies plus the actual mover only.
3. Solo destination: an uncontested battleground solo with ability `<4` is vetoed when a legal group/join alternative exists.
4. Battle: voluntary initiation is vetoed when expected normal battle-destiny draws are zero and no explicit added-draw exception is active.
5. Deploy sequencing: an ability-`<4` first body with an affordable, legal buddy must either complete the pair this phase or wait.

Allowed exemptions must be explicit and testable:

- V172 solo dominance at true `>=2x` effective power, character-only.
- V193 flip-gate control steer.
- Undercover spy drain-block deployment or movement.
- Destiny-eligible solo at an uncontested location.
- Forced movement or R4 mandatory transit.
- Survival retreat from a doomed origin.

### 5. Immediate corrections inside the structural change

1. Gate V171/V172 to deploying `CardCategory.CHARACTER`.
2. Replace BattlePredictor's minimum-one-draw assumption with actual eligibility.
3. Remove V41's universal `Jedi -> +700` application to non-Vader movers, or make it subordinate to the shared destination veto.
4. Stop projecting every character at the origin as if all will move with a single landspeed action.

## Verification Scenarios for K-2's Fix

1. Greedo plus two Alien Mobs: Greedo cannot end the deploy phase alone at Desert Heart unless a buddy follows.
2. First Light ability 3 at Nal Hutta: V171 does not fire; battle is vetoed because normal battle-destiny draws are zero.
3. Chiraneau move: leaving Ozzel ability 2 alone is vetoed; destination solo ability 2 is also vetoed.
4. Tarkin move into Rey: actual mover plus destination allies fails winnability and is vetoed, even though Tarkin can draw destiny.
5. Hondo into Anakin plus Yoda: V41/V166 cannot override the destination veto.
6. Tyranus power 8 into lone Leia power 3: V172 character-only dominance remains allowed.
7. V193 cheap ability character to the flip-gate site remains allowed.
8. Undercover spy deployment to block a drain remains allowed.

## Bottom Line

The existing rules frequently detect the danger. The architecture lets unrelated bonuses overrule them or sends the decision through a route that never sees the rule. A merged hard-veto state plus one shared formation-safety helper is the smallest class-level repair.
