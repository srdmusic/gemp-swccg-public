# Objective Flip Audit — Phase A+B Report

K-2, 2026-07-24. Baseline HEAD `192abf72d` on `rando-consolidation-2026-06-23`. Mission: `Handoffs/OBJECTIVE_FLIP_AUDIT_HANDOFF_2026-07-22.md`, executed under the schema and proof gates of `Handoffs/CODEX_OBJECTIVE_FLIP_BEHAVIOR_DEEP_TEST_HANDOFF_2026-07-24.md`.

Method: 6 extraction agents read all 132 objective card java files (66 front/back pairs) and wrote one SOURCE_VERIFIED record per objective to `records/<frontBp>.json` (structured allOf/anyOf flip law, exact Filters, file:line citations, seed diff, profile diff). A 7th agent mapped every objective_playbooks.json field to its runtime consumer (`analyzer_consumer_map.md`). This report merges both into the Phase B gap classification (`gap_matrix.json`).

## Headline numbers

- 66 objectives audited, 66 SOURCE_VERIFIED records written, zero extraction failures.
- COMPLETE: **0**. PARTIAL: **14**. WRONG: **1**. ABSENT: **51** (43 disabled profiles + 8 with no profile).
- Not one objective has its full flip law reaching runtime. Invasion (14_113) is the closest and it is still losing games (replay `4sec2izfk7qq3hlb`).
- Seed facts errata: 21 mismatches across 15 objectives (list below). The facts list was not just untested, parts of it are false.

## Cross-cutting runtime defects (these dominate any per-objective fix)

1. **The V201 non-additive DEFER**: `DeployPlanPolicy.evaluateDestinationTarget` (`models/common/phase/DeployPlanPolicy.java:124-131`, enforced `CombinedEvaluator.java:557-575`) defers every non-planned destination whenever the exact planned target is offered. A deferred action loses to ANY admissible one regardless of score, so even a +2000 objective steer cannot rescue an empty flip-gate site once the plan named somewhere else. This is the proven Batch Zero failure (Sidious to Swamp, log lines 3299/3460).
2. **keyCharacterFilter / keySiteFilter are never read.** Any actor or site knowledge stored there is inert. Several profiles conceptually rely on them.
3. **objectiveNamedLocations is not even Gson-bound.** All curation in that field is dead weight at runtime.
4. **pullableCards hydration is commented out** (ObjectiveAnalyzer ~970-974); the runtime pull set is parser-only.
5. **flipGateSite is a single string.** It cannot express counts (3 Yavin sites), location classes, opponent constraints (opponent controls zero), anyOf routes, or control-vs-occupy splits. This single schema limit is why 14 of 15 enabled profiles are stuck at PARTIAL.
6. **flipLocationRules / actorLocationRules exist for Invasion only**, and only scoreRole `actorToSite` is ever read. The general mechanism the other 65 objectives need exists but is single-tenant.
7. **No profile field expresses the back-side hold law.** Post-flip play is unguided for every objective, including Invasion.
8. **43 of 58 profiles are loaderEnabled=false**, which means fully inert: no hydration, nothing computed.

## The 15 enabled profiles, classified

| Front BP | Objective | Side | Class | Rationale |
|---|---|---|---|---|
| 7_135 | Dantooine Base Operations | LIGHT | **PARTIAL** | fragments-only; law (3 Rebel-controlled Dantooine sites + opponent zero) unrepresented |
| 7_300 | Ralltiir Operations | DARK | **PARTIAL** | fragments-only; Imperial actor, count 3, opponent-zero constraint all missing |
| 8_78 | Rebel Strike Team | LIGHT | **PARTIAL** | dual-route law (Bunker blown away OR 3 exterior sites with Rebel-scout pairs) unrepresented; move-phase windows missing |
| 8_167 | Endor Operations | DARK | **WRONG** | flipGateSite "endor: bunker" misstates a pure card-on-table law; Ominous Rumors missing from flipGateCardIds; biker_scout implies a phantom actor gate |
| 12_179 | My Lord, Is That Legal? | DARK | **PARTIAL** | unambiguous gate site (Galactic Senate) left null; counts and blockade-agenda alternative missing; keySiteFilter is a dead field so it compensates for nothing |
| 14_113 | Invasion | DARK | **PARTIAL** | front law fully modeled (only profile using flipLocationRules/actorLocationRules); back hold law absent; behaviorally still failing per replay 4sec2izfk7qq3hlb |
| 109_4 | Quiet Mining Colony | LIGHT | **PARTIAL** | three-leg law + Lando/Lobot count-reduction alternative unrepresented |
| 111_4 | Massassi Base Operations | LIGHT | **PARTIAL** | count-3 Yavin sites + opponent-fewer-than-3 constraint not representable in current schema |
| 201_39 | Imperial Entanglements | DARK | **PARTIAL** | counts + opponent constraint missing; Devastator not in requiredCardsOnTable |
| 203_19 | Diplomatic Mission To Alderaan | LIGHT | **PARTIAL** | central gate card-state (delivered Stolen Data Tapes) unencoded; Rebel actor legs missing |
| 204_32 | Old Allies | LIGHT | **PARTIAL** | control/occupy either-way pairing and count-2 unencoded |
| 209_29 | They Have No Idea We're Coming | LIGHT | **PARTIAL** | count-2 Scarif + Rogue One back-hold exception unencoded |
| 219_48 | Zero Hour | LIGHT | **PARTIAL** | two legs with distinct actors, opponent-zero blocker, Phoenix Squadron membership all unencoded |
| 301_2 | City In The Clouds | LIGHT | **PARTIAL** | three-leg law, opponent-zero constraint, strict back comparator unencoded |
| 301_4 | Twin Suns Of Tatooine | DARK | **PARTIAL** | control-vs-occupy split, Dark Jedi actor, opponent-zero constraint unencoded |

## Full 66-objective matrix

| Front BP | Objective | Side | Class | Flip-to-back law (verified from source) | Gaps | Seed errs |
|---|---|---|---|---|---|---|
| 7_135 | Dantooine Base Operations | LIGHT | PARTIAL | Flip if owner controls at least 3 Dantooine sites each with a Rebel contributing to control, AND opponent controls zero Dantooine locations. | 5 | 0 |
| 7_136 | Hidden Base | LIGHT | ABSENT (DISABLED) | Owner may flip any time once the deployed-system tracker is satisfied: the 'Hidden Base' indicator system has been deployed AND (6+ tracked  | 5 | 0 |
| 7_137 | Local Uprising | LIGHT | ABSENT (DISABLED) | Flip if your matching operatives control at least 3 battleground sites related to the Subjugated planet (control-with: you control the site  | 5 | 0 |
| 7_138 | Mind What You Have Learned | LIGHT | ABSENT (DISABLED) | Flip when Jedi Test #5 is completed by Luke (or by Leia if the Daughter Of Skywalker retarget game-text modification is active) and the obje | 5 | 0 |
| 7_139 | Rescue The Princess | LIGHT | ABSENT (DISABLED) | Flip when the objective owner moves Leia to Yavin 4: War Room (move performed by playerId; any Leia, no ability constraint on the move trigg | 5 | 0 |
| 7_296 | Carbon Chamber Testing | DARK | ABSENT (DISABLED) | Flips if (A) after start-of-game deployment no card is imprisoned in Security Tower (no Rebel was in opponent's Reserve Deck; also sets Whil | 5 | 0 |
| 7_297 | Hunt Down And Destroy The Jedi | DARK | ABSENT (DISABLED) | Flip if Vader can be spotted at a battleground site AND no opponent Jedi or Luke can be spotted at a battleground site. Both spots use SpotO | 6 | 2 |
| 7_298 | Imperial Occupation | DARK | ABSENT (DISABLED) | Flips when your matching operatives CONTROL at least 3 battleground sites related to the Renegade planet (controlsWith: you control the site | 4 | 0 |
| 7_299 | ISB Operations | DARK | ABSENT (DISABLED) | Flip if 4 ISB agents are on table OR ISB agents control at least 2 Rebel Base locations (control-with). | 5 | 0 |
| 7_300 | Ralltiir Operations | DARK | PARTIAL | Flip if you control at least 3 Ralltiir sites each with an Imperial there, and opponent controls no Ralltiir locations. | 5 | 0 |
| 8_78 | Rebel Strike Team | LIGHT | PARTIAL | Flip if Bunker is 'blown away', or if during your move phase you control three exterior Endor sites each of which has two of your Rebel scou | 5 | 0 |
| 8_167 | Endor Operations | DARK | WRONG | Flips when both Effects Ominous Rumors AND Establish Secret Base can be spotted on table. Pure card-on-table check; no location control/occu | 4 | 0 |
| 9_61 | There Is Good In Him | LIGHT | ABSENT (DISABLED) | Flip if Luke can be spotted as a captive (anywhere; spot includes captives and battle-excluded cards). The capture itself is engineered by a | 6 | 2 |
| 9_151 | Bring Him Before Me | DARK | ABSENT (DISABLED) | Flip if the target character (Luke by default; Leia or Kanan if retargeted) is a CAPTIVE - spotted anywhere with INCLUDE_CAPTIVE_AND_EXCLUDE | 4 | 1 |
| 10_26 | Watch Your Step | LIGHT | ABSENT (DISABLED) | Flip if you have completed two Kessel Runs OR you occupy two battlegrounds with smugglers (occupy-with: your smuggler at each of 2 battlegro | 5 | 0 |
| 10_29 | Agents Of Black Sun | DARK | ABSENT (DISABLED) | Flip if Xizor (or legacy Shada under LEGACY__TREAT_XIZOR_AS_SHADA) is at a battleground site AND Luke (or Rey/Anakin under Reflections-II re | 5 | 2 |
| 12_88 | Plead My Case To The Senate | LIGHT | ABSENT (DISABLED) | Flip if you have 3+ of your senators AT Galactic Senate, or 2+ of your senators AT Galactic Senate at least one of which has a peace agenda. | 4 | 1 |
| 12_89 | The Hyperdrive Generator's Gone | LIGHT | ABSENT (DISABLED) | Flips when Credits Will Do Fine is active on table and has 4 or more cards stacked beneath it. Pure counter-on-card check; no control/occupy | 5 | 0 |
| 12_179 | My Lord, Is That Legal? | DARK | PARTIAL | Collect your senators AT Galactic Senate (Filters.filterActive with SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE). Flip if that collection has  | 5 | 1 |
| 12_180 | No Money, No Parts, No Deal! | DARK | ABSENT (DISABLED) | Flip if Watto is PRESENT AT Watto's Junkyard AND you OCCUPY Mos Espa. | 4 | 0 |
| 13_46 | We'll Handle This | LIGHT | ABSENT (DISABLED) | Flip if an opponent's Dark Jedi is PRESENT at an interior Naboo battleground site (presentAt = actual presence at the site, spotting include | 5 | 0 |
| 13_73 | Let Them Make The First Move | DARK | ABSENT (DISABLED) | Flip if an opponent's Jedi is PRESENT at an interior Naboo battleground site. Opponent-driven: their Jedi arriving flips you. | 5 | 0 |
| 14_52 | We Have A Plan | LIGHT | ABSENT (DISABLED) | Flip if you CONTROL Theed Palace Throne Room WITH Amidala there (controlsWith: you control the location and Amidala is spotted there, spotti | 4 | 1 |
| 14_113 | Invasion | DARK | PARTIAL | Flips when owner CONTROLS Theed Palace Throne Room with a Neimoidian there AND CONTROLS Naboo system. | 3 | 0 |
| 109_4 | Quiet Mining Colony | LIGHT | PARTIAL | Flip if opponent controls NO Bespin locations, AND you control Bespin: Cloud City (the site), AND (you control >= 2 Cloud City sites, OR (La | 5 | 1 |
| 109_12 | This Deal Is Getting Worse All The Time | DARK | ABSENT (DISABLED) | Flip if Dark Deal on table AND you occupy Bespin system AND you occupy Bespin: Cloud City. All three required. | 5 | 0 |
| 110_4 | You Can Either Profit By This... | LIGHT | ABSENT (DISABLED) | Flip if Han is on Tatooine and not a captive (spot Han with on(Tatooine) and not(captive); includes excluded-from-battle but NOT captives, s | 6 | 0 |
| 110_6 | Court Of The Vile Gangster | DARK | ABSENT (DISABLED) | Flip if you have one captive of ability > 2 at a Jabba's Palace site, OR two captives at Jabba's Palace site(s). | 6 | 0 |
| 111_4 | Massassi Base Operations | LIGHT | PARTIAL | Flip if you CONTROL three Yavin 4 sites AND opponent does NOT control three Yavin 4 sites. | 4 | 0 |
| 111_6 | Set Your Course For Alderaan | DARK | ABSENT (DISABLED) | Flips when Alderaan (system) is 'blown away' - i.e., the Commence Primary Ignition epic event completes against Alderaan. Nothing else flips | 4 | 0 |
| 112_1 | Agents In The Court | LIGHT | ABSENT (DISABLED) | If any non-Tatooine location is on table: flip requires you OCCUPY >= 3 battleground sites AND at least one battleground site is occupied wi | 6 | 2 |
| 112_15 | My Kind Of Scum | DARK | ABSENT (DISABLED) | If a non-Tatooine location is on table: flip requires you OCCUPY 3 battleground sites AND you occupy at least one battleground site with a n | 4 | 1 |
| 201_39 | Imperial Entanglements | DARK | PARTIAL | Flip if you control 3 Tatooine sites AND opponent controls fewer than 3 Tatooine sites. Plain control (no actor filter); sites only, system  | 5 | 0 |
| 203_19 | Diplomatic Mission To Alderaan | LIGHT | PARTIAL | Flip if Stolen Data Tapes are 'delivered' AND you control a battleground site with a Rebel AND you control a battleground system with a Rebe | 4 | 0 |
| 204_32 | Old Allies | LIGHT | PARTIAL | Flip if (you CONTROL Jakku system AND OCCUPY two Jakku battleground sites) OR (you OCCUPY Jakku system AND CONTROL two Jakku battleground si | 5 | 0 |
| 208_25 | He Is The Chosen One | LIGHT | ABSENT (DISABLED) | Flips when Luke or a Jedi is at a battleground site, UNLESS an opponent's character of ability > 4 is at (any) battleground site. | 4 | 0 |
| 208_26 | Yavin 4 Base Operations | LIGHT | ABSENT (DISABLED) | Flip if 4 Rebels are on table (anywhere), OR owner controls at least 2 battleground systems with a Rebel among the controlling presence. Spo | 5 | 2 |
| 208_57 | I Want That Map | DARK | ABSENT (DISABLED) | Flip if your First Order characters CONTROL two battlegrounds (controlsWith: control + at least one First Order character there) AND no Resi | 4 | 0 |
| 209_29 | They Have No Idea We're Coming | LIGHT | PARTIAL | Flip if you CONTROL two Scarif locations (any Scarif locations - system or sites; plain control, no actor filter). | 6 | 0 |
| 210_25 | The Hyperdrive Generators Gone | LIGHT | ABSENT (DISABLED) | Flip when there are 4 or more cards stacked beneath Credits Will Do Fine (which must be active on table). | 4 | 0 |
| 211_26 | A Stunning Move | DARK | ABSENT (DISABLED) | Flip if the Insidious Prisoner card (found via Filters.findFirstActive) is attached to an Invisible Hand site. Card-attached-to-location sem | 5 | 1 |
| 211_36 | The Galaxy May Need A Legend | LIGHT | ABSENT (DISABLED) | MAY flip when a battle was just initiated involving a Resistance character, while Luke is on Ahch-To. Flipping immediately places Luke out o | 4 | 0 |
| 213_31 | Hunt Down And Destroy The Jedi (V) | DARK | ABSENT (DISABLED) | Flip if Vader can be spotted at a battleground site AND no opponent Jedi, Padawan, or Luke can be spotted at a battleground site. Both spots | 6 | 1 |
| 213_32 | Shadow Collective | DARK | ABSENT (DISABLED) | Route A: flip when a character was just 'hit' by your card (any character, any location; no canBeFlipped guard in code). Route B: flip durin | 5 | 0 |
| 215_17 | Rescue The Princess (V) | LIGHT | ABSENT (DISABLED) | Flip if Leia occupies a Death Star site (occupy-with: you occupy a Death Star site with Leia there - imprisoned/captive Leia does NOT occupy | 6 | 0 |
| 216_11 | On The Verge Of Greatness | DARK | ABSENT (DISABLED) | Flip if Krennic or Tarkin is at a Scarif battleground site AND a Death Star system is orbiting Scarif. | 5 | 0 |
| 219_1 | A Great Tactician Creates Plans | DARK | ABSENT (DISABLED) | Flip during any deploy phase if Thrawn is at a battleground (any battleground, site or system) AND Thrawn's Art Collection is on table with  | 5 | 0 |
| 219_48 | Zero Hour | LIGHT | PARTIAL | Flips when (Rebels control three Lothal locations OR you occupy three Lothal locations with Phoenix Squadron characters) AND opponent contro | 4 | 0 |
| 221_67 | Hunt For The Droid General | LIGHT | ABSENT (DISABLED) | Flip if the Effect 'Grievous Will Run And Hide' is attached to this Objective ('here'), UNLESS Grievous is alone at a battleground. The alon | 5 | 2 |
| 222_14 | The Shield Will Be Down In Moments | DARK | ABSENT (DISABLED) | Flip when Main Power Generators is 'blown away' (last step of the blown-away process). Single event condition; no board-count threshold. | 5 | 0 |
| 222_27 | The Empire Knows We're Here | LIGHT | ABSENT (DISABLED) | Flip if opponent OCCUPIES a Hoth location that YOU own (ownership-qualified: Filters.your(playerId) on the location card). | 5 | 0 |
| 222_30 | The Shield Will Be Down In Moments | DARK | ABSENT (DISABLED) | Flip when Main Power Generators is 'blown away' (your own 1st Marker site; the win path is Target The Main Generator + AT-AT Cannon). | 5 | 0 |
| 225_32 | The First Order Reigns | DARK | ABSENT (DISABLED) | Flip if Tracked Fleet is 'annihilated' - coded as the blown-away state of Filters.Tracked_Fleet. | 5 | 0 |
| 225_53 | Mind What You Have Learned (V) | LIGHT | ABSENT (DISABLED) | MAY flip during your turn if Luke is on Dagobah. Implemented as a top-level game-text action, not a table-changed trigger - Rando must activ | 5 | 0 |
| 226_12 | This Deal Is Getting Worse All The Time (V) | DARK | ABSENT (DISABLED) | Flip if you control at least 3 Bespin locations AND opponent controls fewer than 3 Bespin locations. SpotOverride.INCLUDE_EXCLUDED_FROM_BATT | 5 | 0 |
| 226_28 | The Hidden Path | LIGHT | ABSENT (DISABLED) | Flip if Jedi OCCUPY two non-Mapuzo sites (occupiesWith count 2: two sites, each occupied by owner and having a Jedi there). | 5 | 0 |
| 301_2 | City In The Clouds | LIGHT | PARTIAL | Flip if (a) you control two Cloud City battleground sites, (b) you occupy Bespin system, and (c) opponent controls NO Cloud City site. Three | 4 | 0 |
| 301_4 | Twin Suns Of Tatooine | DARK | PARTIAL | Flip if you control two Tatooine battleground sites (at least one controlled with a Dark Jedi), occupy Tatooine system, and opponent control | 4 | 0 |
| 501_14 | I Can Bring You In Warm | LIGHT | ABSENT (NO_PROFILE) | When a battle is just initiated involving both Din and 'The Asset', a required singleton action makes the OPPONENT choose: (0) lose The Asse | 2 | 0 |
| 501_19 | More Systems Will Rally To Our Cause | DARK | ABSENT (NO_PROFILE) | Flips when two [Separatist] systems can be spotted on table. Pure card-on-table count; no control/occupy requirement and no ownership restri | 2 | 0 |
| 501_60 | The First Order Reigns | DARK | ABSENT (NO_PROFILE) | Flip if Tracked Fleet is 'blown away' ('annihilated' in card text; engine models it as blown-away state). Playtest card (ExpansionSet.PLAYTE | 2 | 0 |
| 501_91 | Massassi Base Operations (V) | LIGHT | ABSENT (NO_PROFILE) | Card501_091 is an empty AbstractObjective shell: constructor only (side/title/expansion, setVirtualSuffix, setTestingText("Massassi Base Ope | 1 | 1 |
| 501_94 | Rebel Strike Team (V) | LIGHT | ABSENT (NO_PROFILE) | Flip when the Bunker is 'blown away' (blown-away last step for a card titled Bunker). No control/occupy component at all. | 1 | 0 |
| 601_29 | Wookiee Slaving Operation | DARK | ABSENT (NO_PROFILE) | Flip if your slavers control two Kashyyyk battlegrounds (each controlled with a slaver there) and opponent controls no Kashyyyk locations. | 1 | 0 |
| 601_87 | Hunt Down And Destroy The Jedi (V) | DARK | ABSENT (NO_PROFILE) | Flip if Galen or Vader is AT a battleground site AND opponent has NO unique character of ability > 3 AT a battleground site. Coded as AT (Fi | 2 | 0 |
| 601_146 | Watch Your Step (V) | LIGHT | ABSENT (NO_PROFILE) | Flips when you OCCUPY Corellia system AND CONTROL two Corellia battleground sites with Corellians. | 2 | 0 |

## Seed facts errata (source beats seed in every case)

- `7_297` Hunt Down And Destroy The Jedi: Seed flipRequirements labels the relation PRESENT with byWhom Vader at battleground_site; source is GameConditions.canSpot of Filters.and(Filters.Vader, Filters.at(Filters.battleground_site))). Semantically 'at', not 'present at' in the SWCCG presence sense; minor terminology drift, same practical meaning here.
- `7_297` Hunt Down And Destroy The Jedi: Seed omits the SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE detail on both flip directions (the K2 seed records it only for 213_31).
- `9_61` There Is Good In Him: Seed labels the back-side crossover route 'hardLose' with condition CROSSOVER_DESTINY_GT_14; in source it is a WIN condition for the objective owner (Vader crosses to Light Side and OPPONENT's Life Force is depleted), a TopLevelGameTextAction the owner chooses, not a loss or a required trigger. Category error in the seed.
- `9_61` There Is Good In Him: Seed does not record that the crossover destiny total is modifier-adjusted via modifiersQuerying.getCrossoverAttemptTotal(vader, totalDestiny) before the > 14 comparison (Card9_061_BACK.java:145-147).
- `9_151` Bring Him Before Me: Seed flip claims byWhom=Filters.Vader ('CAPTURED ... byWhom Vader'). Source flip law has NO Vader requirement: it is canSpot(target AND Filters.captive) regardless of who captured or whether Vader escorts (Card9_151.java:343/359/375). Vader is only the auto-capture MECHANISM, not part of the flip condition.
- `10_29` Agents Of Black Sun: Seed flipRequirements say the retargeted Luke alternatives are 'Luke/Leia/Kanan/Anakin'; source Card10_029.java:167-169 uses ModifyGameTextType.REFLECTIONS_II_OBJECTIVE__TARGETS_REY_INSTEAD_OF_LUKE -> Filters.Rey and ...TARGETS_ANAKIN_INSTEAD_OF_LUKE -> Filters.Anakin. Alternatives are Rey and Anakin, never Leia or Kanan.
- `10_29` Agents Of Black Sun: Seed flipBackRequirements repeat the same wrong list ('retargeted Luke/Leia/Kanan/Anakin'); source Card10_029_BACK.java:216-218 is Rey | Anakin | Luke.
- `12_88` Plead My Case To The Senate: Seed (Objective_Playbook_Facts_2026-07-08.json entry 12_88) labels both flip relations as 'PRESENT'; source checks Filters.at(Filters.Galactic_Senate) - the AT relation, which is broader than 'present at' in SWCCG semantics. Filters are otherwise quoted correctly by the seed, so this is a relation-label inaccuracy only.
- `12_179` My Lord, Is That Legal?: Terminology only: seed calls both directions' relation PRESENT; source uses Filters.at(Filters.Galactic_Senate) inside filterActive/canSpot ('at' semantics, spot-based counting). Thresholds, actors, and site all match source.
- `14_52` We Have A Plan: Seed (Objective_Playbook_Facts_2026-07-08.json entry 14_52) cites sourceEvidence 'front Java getGameTextWhileActiveInPlay' / 'back Java getGameTextWhileActiveInPlay' for both flip laws; the flip triggers actually live in getGameTextRequiredAfterTriggers on both sides. Relations and filters in the seed are otherwise correct.
- `109_4` Quiet Mining Colony: Seed's Lando/Lobot leg says relation ON where Filters.Bespin_Cloud_City (the named Bespin: Cloud City site); source uses GameConditions.isOnCloudCity -> Filters.on_Cloud_City = locationAndCardsAtLocation(Filters.Cloud_City_site), i.e. Lando/Lobot at ANY Cloud City site satisfies the leg, not only Bespin: Cloud City.
- `112_1` Agents In The Court: Seed does not record the Rep-null guard: on the front, if no Rep was chosen (WhileInPlayData empty) the flip trigger method returns null and the objective can NEVER flip (Card112_001.java:134-137). Behavioral fact missing from seed.
- `112_1` Agents In The Court: Seed structure nests the 2-site and 3-site branches as 'alternatives' of one requirement without stating the branch selector's exclusivity: source is a strict if/else-if on canSpotLocation(non_Tatooine_location); the 2-battleground branch is ONLY evaluated when no non-Tatooine location exists on table.
- `112_15` My Kind Of Scum: Seed structures the 3-site branch as OCCUPIES count 3 'byWhom' rep-species alien, conflating two separate checks. Source is: occupies(3, battleground_site) AND separately occupiesWith(battleground_site, rep-species non-unique alien) with no count - the species alien needs to be at only ONE occupied battleground site, and the other two legs need no particular occupier (Card112_015.java:156-157).
- `208_26` Yavin 4 Base Operations: Seed omits SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE on all legs (both directions).
- `208_26` Yavin 4 Base Operations: Seed flipBack comparator 'LESS_THAN_UNLESS_FOUR_REBELS_ON_TABLE' compresses the law correctly but does not state the occupy filter is Filters.battleground (any battleground, sites included) while the FRONT leg is Filters.battleground_system only; the asymmetry (control systems to flip vs occupy any battlegrounds to hold) is a strategic fact absent from the seed.
- `211_26` A Stunning Move: Seed K2 rows40-57 WIP (entry 211_26, 'characters' item) claims the [Separatist] droid pull is 'non-unique enforced at deploy'; the actual filter in both front and back java is Filters.and(Icon.SEPARATIST, Filters.droid) with NO Filters.non_unique term (Card211_026.java:76, Card211_026_BACK.java:79). Card text and setActionMsg say non-unique, but the coded filter does not enforce it.
- `213_31` Hunt Down And Destroy The Jedi (V): 2026-07-08 seed omits the once-per-game take-Vader-into-hand rider (front class, cardFlipped trigger, requires a site you control with Vader); the K2 rows40-57 WIP seed does record it. No substantive mismatches in either seed's flip/flipBack laws.
- `221_67` Hunt For The Droid General: Seed flipBack leg 1 claims relation PRESENT 'Grievous Will Run And Hide at a battleground'; source checks !GameConditions.hasAttached(game, self, Filters.Grievous_Will_Run_And_Hide) (Card221_067_BACK.java:175): the back flips to front whenever GWRAH is no longer attached to the Objective, regardless of whether it is at a battleground. Code is strictly broader than the seed/card-text phrasing.
- `221_67` Hunt For The Droid General: Seed omits the alone re-verification guard on both directions: Grievous only counts as 'alone' if modifiersQuerying.isAlone(..., SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE) also holds (companions excluded from battle still break 'alone').
- `501_91` Massassi Base Operations (V): No seed row in resources/Objective_Playbook_Facts_2026-07-08.json or resources/Objective_Playbook_Facts_K2_rows40-57_WIP.json (frontBp 501_91 absent from both) - nothing to compare.

## Engine / data defects found in passing (report-only, we do not touch these)

- `card_cache.json` entry `222_27` is stale: cache says "Alter" (light); source Card222_027.java is the objective The Empire Knows We're Here.
- `Card501_094_BACK` (Rebel Strike Team V, playtest): Endor Rebel draw-phase retrieval is NOT gated on Bunker blown away despite printed text.
- `Card601_146_BACK` (Watch Your Step V): printed "unless two Kessel Runs" flip-back exception is not coded; broad text-vs-code divergence.
- `Card601_087` (Hunt Down V, legacy): back destiny+2 vs vehicles/starships is an uncoded TODO; front Rogue Shadow upload lacks a once-per-game limit.
- `Card7_139_BACK.java:68-90`: dead subjugated-planet/operative add-destiny action matching no clause of the printed card.
- `Card14_113_BACK` missing post-flip modifier: previously documented for Steve, still open.

## Supersession and next steps

- The records in `records/` supersede `resources/Objective_Playbook_Facts_2026-07-08.json` and the rows 40-57 WIP file as the audit truth source. Both seed files left untouched (WIP file is uncommitted parallel work; preserved).
- Phase C priority 1 (Batch Zero): sequence test reproducing the V201 DEFER beating the empty Throne Room offer, per Codex's deep-test handoff section 10.
- Phase C priority 2: characterization tests for the 15 enabled profiles asserting hydrated facts vs the truth records.
- Phase D shape, subject to dual gate: fix the DEFER/objective interaction first; then a schema extension generalizing flipLocationRules/actorLocationRules (counts, opponent constraints, anyOf, control-vs-occupy, postFlip roles) before enabling more profiles. Enabling profiles into dead fields would ship nothing.
