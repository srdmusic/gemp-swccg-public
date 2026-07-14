# Objective Boundary Batch 07: Rows 18-20

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 18 | `208_25` | HITCO | He Is The Chosen One / He Will Bring Balance | No live title-specific scoring found outside dead `ObjectiveHandler`. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Luke/Jedi battleground relation, opponent ability gate, and crossed-over Vader exception. |
| 19 | `208_26` | Y4BO | Yavin 4 Base Operations / The Time To Fight Is Now | No live title-specific scoring found outside dead `ObjectiveHandler`. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Rebel-count and Rebel-controlled battleground-system alternatives. |
| 20 | `209_29` | THNIWRC | They Have No Idea We're Coming / Until We Win, Or The Chances Are Spent | No live title-specific scoring found. Scarif hits in evaluators are mostly dark Verge/Death Star steering, not this objective. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Scarif control/occupy profile plus Rogue One exception and Rebel spy post-flip actions. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `rolloutEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `208_25` | false | empty | empty | empty | null | empty | empty | empty | present: Anakin's Funeral Pyre, Prophecy Of The Force, Ewok Village, battleground site |
| `208_26` | false | empty | empty | empty | null | empty | empty | empty | present: Yavin 4 system, Massassi War Room, battleground system, battleground, liberated system |
| `209_29` | false | empty | empty | empty | null | empty | empty | empty | present: Scarif system, Scarif site/location, Data Vault, Massassi War Room |

This is behavior-neutral while disabled. It is not source-equivalent, because the loader does not consume these named-location facts for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `208_25` | Front deploys `Filters.title(Title.Anakins_Funeral_Pyre)`, `Filters.Prophecy_Of_The_Force` to that site, `Filters.Ewok_Village`, and `Filters.I_Feel_The_Conflict`. It forbids Episode I or Episode VII character/location deploys except Obi-Wan, Yoda, and Lars' Moisture Farm, makes Emperor's Power not increase deploy costs at battlegrounds, lets LS initiate battles for free, and lets Luke battle wins recirculate and shuffle. Front flips when Luke or a Jedi is at a battleground site and no opponent character ability > 4 is at a battleground site. Back peeks at top Reserve cards up to occupied battleground count, retrieves any one card during draw phase with opponent I Feel The Conflict stack-to-out-of-play response, repeats Luke battle recirculate/shuffle, and flips back unless Vader crossed over if an opponent ability > 4 character is at a battleground site or LS lacks Luke/Jedi at a battleground site. | Captures setup, Luke/Jedi battleground flip, opponent ability gate, crossed-over Vader exception, Reserve peek, card retrieval, and flip-back. | Runtime consumed slots omit setup, deploy bans, battleground relation filters, opponent ability exclusion, crossed-over Vader state, post-flip peek/retrieval, and I Feel The Conflict response. | Hold. Needs relation and state exceptions before meaningful enable. |
| `208_26` | Front deploys `Filters.Yavin_4_system`, `Filters.Massassi_War_Room`, and optionally `Filters.Restore_Freedom_To_The_Galaxy`. Once per turn, LS may use 1 Force to deploy a battleground system from Reserve Deck. Front flips if four Rebels are on table or LS controls two battleground systems with Rebels. Back gives total power +2 at related sites for each piloted unique snub fighter present at a system LS occupies, may place a Lost Pile card out of play during draw phase to retrieve 1 Force if a system is liberated, and flips back if LS has fewer than four Rebels on table and does not occupy two battlegrounds. | Captures setup, battleground system pull, four-Rebel alternative, Rebel-controlled system alternative, liberated-system retrieval, and flip-back. | Runtime consumed slots omit setup, optional Restore Freedom, battleground system pull/cost, Rebel count, Rebel-controlled system relation, liberated-system draw action, related-site snub fighter modifier, and flip-back. | Hold. Needs count/alternative modeling, not a flat Yavin location string. |
| `209_29` | Front deploys `Filters.Scarif_system`, `Filters.DataVault`, `Filters.Stardust`, and `Filters.Massassi_War_Room`. It makes Baze, Chirrut, and Rebel troopers spies, forbids Taking Them With Us and Jedi, once per turn may deploy `Filters.or(Filters.Rogue_One, Filters.corvette, Filters.Scarif_site)` from Reserve Deck, and flips when LS controls two Scarif locations. Back gives LS spies defense +2, LS spies with Stardust power +1, LS spies immunity to Undercover, prevents opponent canceling LS Force drains at battlegrounds while Stardust is on an LS spy, can place a Rebel character from Lost Pile out of play to move an LS Rebel spy during control or cancel weapon targeting of a non-undercover LS Rebel spy, and flips back if LS does not occupy two Scarif locations unless Rogue One is at a Scarif site LS occupies. | Captures setup, Scarif pull, control-2 flip, Rogue One exception, spy modifiers, and Lost Pile cost actions. | Runtime consumed slots omit setup, Scarif pull, control/occupy Scarif counts, Rogue One exception, spy keyword grant, deploy bans, Stardust attachment condition, and post-flip movement/weapon-cancel actions. | Hold. Needs Scarif relation schema and must not reuse dark Verge Death Star scoring. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| HITCO | No live title-specific score found. Dead `ObjectiveHandler` has setup refs, but it is not consumed. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs Luke/Jedi relation scoring plus the opponent ability gate. |
| Y4BO | No live title-specific score found. Dead `ObjectiveHandler` has setup refs, but it is not consumed. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs four-Rebel and Rebel-controlled-system alternatives. |
| THNIWRC | No live title-specific score found. Scarif evaluator hits are dark Verge/Death Star steering, including V79/V103 parsec and orbit logic, not this Light Scarif objective. | Empty consumed slots add nothing. | Neutral no-op today. Future enable must keep Light Scarif objective scoring separated from dark Verge Death Star orbit logic. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `208_25` | `startingLocations` | `Filters.title(Title.Anakins_Funeral_Pyre)`, DB candidate `217_34`; `Filters.Ewok_Village`, DB candidates `8_73`, `8_163`, `208_23`. |
| `208_25` | `startingEffects` | `Filters.Prophecy_Of_The_Force`, DB candidate `208_14`, deployed to Anakin's Funeral Pyre; `Filters.I_Feel_The_Conflict`, DB candidate `9_34`. |
| `208_25` | new schema likely needed | `flip=Luke or Jedi at battleground site and no opponent ability >4 character at battleground site`; `flipBack=opponent ability >4 at battleground site OR no Luke/Jedi at battleground site unless Vader crossed over`; `postFlipReservePeekByOccupiedBattlegroundCount`; `postFlipRetrieveAnyCardWithIFeelTheConflictResponse`; `frontFreeBattles`; `frontDeployBans`. |
| `208_26` | `startingLocations` | `Filters.Yavin_4_system`, DB candidates `1_135`, `1_296`, `211_32`; `Filters.Massassi_War_Room`, DB candidates `1_139`, `208_24`. |
| `208_26` | `startingEffects` | Optional `Filters.Restore_Freedom_To_The_Galaxy`, DB candidate `208_17`. |
| `208_26` | `pullableCards` | `Filters.system` constrained by `Filters.battleground`, cost 1 Force, once per turn. |
| `208_26` | new schema likely needed | `flip=four Rebels on table OR control two battleground systems with Rebels`; `flipBack=not four Rebels and not occupy two battlegrounds`; `postFlipRelatedSitePowerByPilotedUniqueSnubAtOccupiedSystem`; `postFlipLiberatedSystemDrawRetrieve`. |
| `209_29` | `startingLocations` | `Filters.Scarif_system`, DB candidates `209_23`, `216_13`; `Filters.DataVault`, DB candidate `209_25`; `Filters.Massassi_War_Room`, DB candidates `1_139`, `208_24`. |
| `209_29` | `startingEffects` | `Filters.Stardust`, DB candidate `209_18`. Java deploys it as a separate Reserve deploy, not explicitly attached to Data Vault. |
| `209_29` | `pullableCards` | `Filters.or(Filters.Rogue_One, Filters.corvette, Filters.Scarif_site)`, once per turn. Known candidates include Rogue One `206_7`, Scarif sites `209_24` to `209_27` and `216_14` to `216_17`; corvette stays runtime-filter truth. |
| `209_29` | new schema likely needed | `flip=control two Scarif locations`; `flipBack=not occupy two Scarif locations unless Rogue One at Scarif site you occupy`; `frontSpyGrant=Baze/Chirrut/Rebel troopers`; `frontDeployBans=Taking Them With Us,Jedi`; `postFlipSpyDefensePlus2`; `postFlipSpyWithStardustPowerPlus1`; `postFlipForceDrainCancelProtection`; `postFlipLostPileRebelCostMoveSpyOrCancelWeaponTarget`. |

## Source Files Read

| BP | Files |
|---|---|
| `208_25` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set208/light/Card208_025.java`, `Card208_025_BACK.java` |
| `208_26` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set208/light/Card208_026.java`, `Card208_026_BACK.java` |
| `209_29` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set209/light/Card209_029.java`, `Card209_029_BACK.java` |

## One-Line Verdict

Rows 18-20 are disabled no-ops today, but not safe as meaningful JSON-driven profiles. HITCO needs Luke/Jedi relation gates, Y4BO needs Rebel count/control alternatives, and THNIWRC needs Light Scarif scoring isolated from dark Verge Death Star rules.
