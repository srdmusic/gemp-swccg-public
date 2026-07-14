# Objective Boundary Batch 08: Rows 21-23

Created: 2026-07-08
Author: Codex Alfred
Scope: disabled Light objectives, no Java edits

## Summary

| Row | BP | Abbr | Objective | Old title-specific scoring | Runtime profile state | Verdict |
|---:|---|---|---|---|---|---|
| 21 | `210_25` | THGG | The Hyperdrive Generator's Gone / We'll Need A New One | No live title-specific scoring found. Generic Episode I hits are unrelated to this objective. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs setup refs, Credits Will Do Fine stack-count flip schema, Episode I system pull, and post-flip battleground loss count. |
| 22 | `211_36` | TGMNAL | The Galaxy May Need A Legend / We Need Luke Skywalker | No live title-specific scoring found. Dead `ObjectiveHandler` points to Ahch-To system, but Java deploys Ahch-To: Saddle. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Saddle setup, Episode VII battleground pull, Luke-on-Ahch-To plus Resistance-battle flip schema, and post-flip Luke out-of-play caveat. |
| 23 | `215_17` | RTP | Rescue The Princess / Sometimes I Amaze Even Myself | No live title-specific scoring found for the virtual row. Classic RTP must not be copied without a source split. | Disabled. Consumed hydrated slots empty. `objectiveNamedLocations` present but not consumed by current loader. | Hold. Needs Death Star setup package, A Power Loss shutdown state, captive Leia relation, and Leia-at-Death-Star flip-back. |

Grep scope: `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/{rando,chosenone}`.

## Runtime JSON Hydrated Slots

Current source: `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`

| BP | `rolloutEnabled` | `locationFragments` | `requiredCardsOnTable` | `pullableCards` | `flipGateSite` | `flipGateCardIds` | starting refs | weights | non-consumed named locations |
|---|---:|---|---|---|---|---|---|---|---|
| `210_25` | false | empty | empty | empty | null | empty | empty | empty | present: Episode I system, Tatooine location, battleground occupied by Amidala/Jar Jar, City Outskirts, Watto's Junkyard |
| `211_36` | false | empty | empty | empty | null | empty | empty | empty | present: Ahch-To: Saddle, Ahch-To location, Episode VII battleground |
| `215_17` | false | empty | empty | empty | null | empty | empty | empty | present: Death Star site, Detention Block Corridor, Death Star: Central Core, Trash Compactor |

This is behavior-neutral while disabled. It is not source-equivalent, because the loader does not consume these named-location facts for scoring.

## Source Audit Table

| BP | Java source truth | Descriptive facts file | Runtime profile mismatch | Enable verdict |
|---|---|---|---|---|
| `210_25` | Front deploys `Filters.Wattos_Junkyard`, `Filters.City_Outskirts`, and `Filters.Credits_Will_Do_Fine` from Reserve Deck. It bans most ability-card deploys except unique aliens, Republic characters, Republic starships, and Episode I Jedi, suspends Your Destiny, blocks Force loss from Reflections II objectives, lets LS once per game take an Episode I system from Reserve, gives Maul immunity unless present with Qui-Gon, and flips when Credits Will Do Fine has at least four stacked cards. Back gives unique Republic characters power +1 and forfeit +2, prevents alien deploy-cost modifiers to Tatooine locations, retrieves 1 Force after non-substituted battle destiny, activates up to 2 Force if Queen's Royal Starship is at a system, and makes opponent lose 1 Force per battleground occupied by Amidala or Jar Jar during control. | Captures setup, Episode I system pull, Credits Will Do Fine stack-count flip, no flip-back, Queen's Royal Starship activation, and Amidala/Jar Jar battleground count. | Runtime consumed slots omit setup, Credits Will Do Fine as a flip-critical card, the stack-count relation, Episode I system pull, deploy bans, Your Destiny suspension, Maul/Qui-Gon condition, post-flip activation, and battleground-count loss. | Hold. Needs a stack-count flip schema and post-flip count action before meaningful enable. |
| `211_36` | Front deploys `Filters.AhchTo_Saddle` and `Filters.and(Filters.battleground, Icon.EPISODE_VII)`. It restricts Luke to Ahch-To, forbids Force drains on Ahch-To, forbids Episode I locations and non-Episode VII characters of ability > 4, lets LS once per game take any one card from Force Pile, and once per deploy phase deploys `Filters.or(Filters.AhchTo_location, Filters.and(Filters.battleground, Icon.EPISODE_VII))` from Reserve. It flips after a battle is initiated involving a Resistance character while Luke is on Ahch-To. Back immediately places Luke out of play when flipped, prevents weapons for the battle, lets LS take the Force Pile card if not already used, peeks at top Force and Reserve once per turn, limits opponent immunity, gives drains +1 where LS has two unique Resistance characters, and can cancel opponent destiny for redraw during battle with two participating Resistance characters. | Captures setup, Force Pile upload, Ahch-To/Episode VII location pull, Luke-on-Ahch-To plus Resistance-battle flip, and post-flip actions. | Runtime consumed slots omit setup, Ahch-To/Luke relation, Episode VII battleground pull, Force Pile upload, deploy bans, Force drain ban, Luke out-of-play flip trigger, post-flip peek, two-Resistance drain modifier, and destiny redraw. | Hold. Needs relation filters and post-flip effect fields. Also fix stale Ahch-To system assumption from dead `ObjectiveHandler`. |
| `215_17` | Front deploys `Filters.Death_Star_Central_Core`, `Filters.A_Power_Loss`, `Filters.Trash_Compactor`, `Filters.Detention_Block_Corridor`, and if Detention Block Corridor is spotted in Reserve at that moment, deploys `Filters.and(Icon.A_NEW_HOPE, Filters.Leia)` there as imprisoned captive. It gives LS Death Star sites +1 Force generation, forbids Luke ability > 4 and Episode I/VII Jedi, re-imprisons Leia if she leaves table, once per turn deploys a Death Star site from Reserve, and flips when Leia occupies a Death Star site and `GameConditions.isDeathStarPowerShutDown(game)` is true. Back keeps Death Star site deploy, adds opponent Force drain cost +1, makes LS Death Star sites immune to Set Your Course For Alderaan text, cancels I Can't Believe He's Gone, can place Obi-Wan out of play from a Death Star site to cancel a Death Star battle, retrieves during opponent draw if opponent did not battle, re-imprisons Leia, causes opponent loss after LS blaster hit, and flips back if Leia is not at a Death Star site. | Captures setup, captive Leia deploy, Death Star site pull, A Power Loss shutdown flip truth, Leia flip-back, Set Your Course modifier, and post-flip actions. | Runtime consumed slots omit setup, A Power Loss, captive Leia destination, Death Star site pull, shutdown state, Leia-at-site flip relation, Set Your Course modifier, Obi-Wan battle cancel, I Can't Believe He's Gone cancel, draw-phase retrieve, blaster-hit loss, and re-imprison trigger. | Hold. Needs a virtual-RTP profile split from classic RTP and a power-shutdown state, not just A Power Loss on table. |

## Existing Score Boundary

| Objective | Old score/magnitude found | New profile if enabled now | Boundary result |
|---|---|---|---|
| THGG | No live title-specific score found. Generic Episode I comments and filters in Rando/ChosenOne do not target this objective. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs new slot consumption for stack-count and post-flip battleground-count scoring. |
| TGMNAL | Dead `ObjectiveHandler` lists `211_48` Ahch-To system, but current Java setup deploys `210_1` Ahch-To: Saddle. DeployPhaseScript Ahch-To comments are for another card interaction, not objective scoring. | Empty consumed slots add nothing. | Neutral no-op today. Future enable must correct the system-vs-Saddle trap and avoid resurrecting dead ObjectiveHandler data. |
| RTP V | No live virtual-RTP title-specific score found. Existing generic Leia/CardKnowledge and Death Star mentions are not objective flip scoring. | Empty consumed slots add nothing. | Neutral no-op today. Future enable needs source-derived virtual RTP fields. Do not merge blindly with classic `7_139`. |

## Draft Prescriptive Slots For K-2

| BP | Slot | Source-derived value |
|---|---|---|
| `210_25` | `startingLocations` | `Filters.Wattos_Junkyard`, DB candidates `12_87`, `12_178`; `Filters.City_Outskirts`, DB candidate `11_42`. |
| `210_25` | `startingEffects` | `Filters.Credits_Will_Do_Fine`, DB candidates `12_42`, `221_56`. Runtime title filter decides legal copy. |
| `210_25` | `pullableCards` | `Filters.and(Filters.system, Icon.EPISODE_I)`, once per game take into hand from Reserve. DB snapshot has 17 Episode I systems, but runtime filter is truth. |
| `210_25` | new schema likely needed | `flip=Credits Will Do Fine has >=4 stacked cards`; `postFlipOpponentLoseByBattlegroundOccupiedByAmidalaOrJarJar`; `postFlipActivateIfQueensRoyalStarshipAtSystem`; `frontDeployBans`; `YourDestinySuspended`; `MaulImmuneUnlessWithQuiGon`. |
| `211_36` | `startingLocations` | `Filters.AhchTo_Saddle`, DB candidate `210_1`; `Filters.and(Filters.battleground, Icon.EPISODE_VII)`, runtime filter only. |
| `211_36` | `pullableCards` | `Filters.any` from Force Pile, once per game; `Filters.or(Filters.AhchTo_location, Filters.and(Filters.battleground, Icon.EPISODE_VII))` from Reserve during deploy, once per turn. |
| `211_36` | new schema likely needed | `flip=Luke on Ahch-To AND battle just initiated involving Resistance character`; `frontLukeDeployOnlyToAhchTo`; `frontAhchToForceDrainBan`; `frontDeployBans=Episode I locations and non-Episode VII characters ability>4`; `onFlipPlaceLukeOutOfPlay`; `postFlipTwoResistanceDrainPlus1`; `postFlipPeekForceReserve`; `postFlipDestinyCancelRedraw`. |
| `215_17` | `startingLocations` | `Filters.Death_Star_Central_Core`, DB candidates `215_6`, `1_283`; `Filters.Trash_Compactor`, DB candidates `215_9`, `1_125`; `Filters.Detention_Block_Corridor`, DB candidates `215_7`, `7_118`, `1_284`. |
| `215_17` | `startingEffects` | `Filters.A_Power_Loss`, DB candidate `215_2`. It is an epic event in DB, not an effect. |
| `215_17` | `startingCharacters` | `Filters.and(Icon.A_NEW_HOPE, Filters.Leia)` deployed as imprisoned captive to Detention Block Corridor. Candidate source is runtime filter, not a single title. |
| `215_17` | `pullableCards` | `Filters.Death_Star_site`, once per turn from Reserve on front and back. |
| `215_17` | new schema likely needed | `flip=Leia occupies Death Star site AND Death Star power shut down`; `flipBack=no Leia at Death Star site`; `frontReimprisonLeiaOnLeaveTable`; `postFlipModifySetYourCourseForAlderaan`; `postFlipCancelICantBelieveHesGone`; `postFlipObiWanOutOfPlayCancelDeathStarBattle`; `postFlipRetrieveIfOpponentDidNotBattle`; `postFlipBlasterHitOpponentLose1`. |

## Source Files Read

| BP | Files |
|---|---|
| `210_25` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set210/light/Card210_025.java`, `Card210_025_BACK.java` |
| `211_36` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set211/light/Card211_036.java`, `Card211_036_BACK.java` |
| `215_17` | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set215/light/Card215_017.java`, `Card215_017_BACK.java` |

## One-Line Verdict

Rows 21-23 are disabled no-ops today, but not safe as meaningful JSON-driven profiles. THGG needs stack-count and post-flip count semantics, TGMNAL needs the Saddle/Luke/Resistance relation split, and virtual RTP needs source-derived power-shutdown plus captive-Leia handling separate from classic RTP.
