# Permanent-weapon AI audit

Date: 2026-07-11
Owner: Codex/Alfred, read-only verification
Tested artifact: deployed `web.jar` at HEAD `326895c77`

## Finding

The AI currently asks two different questions with one text search:

1. Does this character own a permanent weapon?
2. Can that permanent weapon hit a character and set forfeit to zero?

Those questions require different evidence. The permanent-weapon icon answers
the first. It does not answer the second.

## Current exposure

Each bot has nine logical checks using
`getGameText().toLowerCase().contains("permanent weapon")`:

- `CardSelectionEvaluator.java:1564,1594,5897,6774`
- `ActionTextEvaluator.java:3706`
- `BattleEvaluator.java:261,599`
- `MoveEvaluator.java:2209,3030`

The chosenone files mirror the same line numbers, for 18 total checks.

## False ownership matches

The following real character blueprints mention permanent weapons but do not
own one. Deployed-jar checks confirmed
`bp.hasIcon(Icon.PERMANENT_WEAPON) == false` for all ten:

| Blueprint | Card |
|---|---|
| `201_25` | Jango Fett |
| `205_11` | Dathcha |
| `210_12` | Dash Rendar |
| `211_59` | Ahsoka Tano |
| `216_36` | Master Kenobi |
| `216_38` | Master Yoda |
| `219_3` | Daultay Dofine |
| `221_8` | Asajj Ventress |
| `223_46` | Sabine, Padawan Learner |
| `601_173` | Maris Brood, Fallen Jedi |

Their text refers to another card, an opponent, a restriction, or immunity.
The ownership replacement is the typed predicate:

```java
bp.hasIcon(Icon.PERMANENT_WEAPON)
```

## True owners that do not hit

These six true permanent-weapon characters can target a character, but their
weapon action never marks that target hit:

| Blueprint | Permanent weapon result |
|---|---|
| `109_6` | 4-LOM's Concussion Rifle cancels game text |
| `200_71` | 4-LOM's Concussion Rifle makes target power -1 |
| `601_177` | 4-LOM's Concussion Rifle makes target power -1 |
| `208_2` | C1-10P's Electroshock Prod cancels game text |
| `211_7` | Narthax's E-web changes immunity or adds attrition |
| `213_7` | Hylobon Enforcer's cannon cancels game text |

Source evidence:

- `Card109_006.java:41,62,66`
- `Card200_071.java:43,57,61`
- `Card601_177.java:43,58,62`
- `Card208_002.java:42,68,72`
- `Card211_007.java:33,46,50`
- `FireWeaponActionBuilder.java:1827-1879`
- `Card213_007.java:40,69,73`

## V76 boundary

`BattleEvaluator.java:599-614` sets `fArmed=true` for every permanent-weapon
text match, then prices each armed opponent as one of our characters hit with
forfeit zero. Two such opponents against average forfeit 5 fabricate expected
loss 10 and trigger the `-500` pyrrhic veto.

Replacing the text test with `Icon.PERMANENT_WEAPON` fixes ownership detection,
but it does not fix this hit-loss assumption. V76 hit economics needs separate,
narrow evidence that the weapon can actually hit a character. Until that
evidence exists, true non-hit permanent weapons must not increment the hit-loss
counter.

No Java files were edited by Codex.
