# Bottom-up objective intel (inventory rows 18-28, light side) — DRAFT, verify against source

Generated 2026-07-07 by K-2 fast agents. Bottom-up coverage now spans rows 18-57. Candidate counts DB-limited (no set601/Legacy in JSON DB). Verify ids before wiring.

---

# Bottom-up objective intel (inventory rows 18-28) — DRAFT, verify against source

Generated 2026-07-07 by K-2. INTEL to help Codex + cross-check. Verify every id/title against source before wiring. All id/count claims are DB-limited (card_blueprint_database JSON omits set601/Legacy) and flagged where noted. All 11 records report `found=true`.

---

# Bottom-up objective intel (rows 18-28)

Eleven LIGHT-side objectives, bottom-up from card blueprints. Per section: CardRef, Filters-used-in-Java, NamedCardRefs, LocationRequirements, CharacterRequirements, DYNAMIC state, pull chain, flip / flip-back / hard-lose, existing Rando V-tags, gaps.

---

## 208_25 — He Is The Chosen One / He Will Bring Balance (LIGHT)

- **CardRef:** 208_25 (He Is The Chosen One), 208_25_BACK (He Will Bring Balance)
- **Java:** `set208/light/Card208_025.java` (back: `Card208_025_BACK.java`)
- **Filters used in Java:** title(Anakins_Funeral_Pyre), Prophecy_Of_The_Force, Ewok_Village, I_Feel_The_Conflict, or(Icon.EPISODE_I, Icon.EPISODE_VII), character, location, ObiWan, Yoda, Lars_Moisture_Farm, except, Emperors_Power, battleground, any, Luke, Jedi, at(battleground_site), opponents(self), abilityMoreThan(4), occupies(playerId), countTopLocationsOnTable
- **NamedCardRefs:**
	- Anakin's Funeral Pyre → 217_34 (Endor: Anakin's Funeral Pyre, SITE, light)
	- Prophecy Of The Force → 208_14 (Effect, light); deployed to the Pyre
	- Ewok Village → 208_23, 8_73 (Endor: Ewok Village, SITE, light); dark 8_163 is opponent-side, not pulled
	- I Feel The Conflict → 9_34 (Effect, light)
	- Obi-Wan / Yoda / Lars' Moisture Farm → Filters (deploy-restriction exceptions, not pulled)
	- Emperor's Power → Filters.Emperors_Power (deploy-cost-immunity target, not pulled)
- **LocationRequirements:** battleground_site (flip/re-flip spotting both sides). Back peek X = battlegrounds occupied = and(battleground, occupies(playerId)). Emperor's Power cost immunity scoped to Filters.battleground. Ewok Village filter matches both 208_23 and 8_73 (light); physical copy pulled is deck-dependent. Battleground counts DB-limited (set601/Legacy absent).
- **CharacterRequirements:** Luke = Filters.Luke (persona). Jedi = Filters.Jedi (Keyword.JEDI). Opponent threat = opponents + character + abilityMoreThan(4) at battleground_site. Vader crossover check = Persona.VADER (GameConditions.isCrossedOver), gates back re-flip.
- **DYNAMIC state:** None. All pull targets are fixed named ids; no runtime-chosen planet/system/species. Back X (battlegrounds occupied) is a live count, not a chosen id.
- **Pull chain:** Front deploy (required, from Reserve): Anakin's Funeral Pyre (217_34) → Prophecy Of The Force (208_14) to the Pyre → Ewok Village (208_23/8_73) → I Feel The Conflict (9_34), then shuffle. Optional after Luke wins a battle: recirculate + shuffle both Reserve Decks.
- **Flip:** Front→Back (isTableChanged): canSpot Luke or Jedi at a battleground_site AND NOT canSpot opponent character ability>4 at a battleground_site.
- **Flip-back:** Back→Front (unless Vader crossed over, Persona.VADER): if canSpot opponent character ability>4 at a battleground_site OR cannot spot Luke/Jedi at a battleground_site.
- **Hard-lose:** None. Back side: on draw-phase retrieve, opponent MAY stack a card from hand on I Feel The Conflict to place the RETRIEVED card out of play instead (opponent option, affects retrieved card only).
- **Existing Rando V-tags:** No LIVE V-tag targets this objective. Dead ObjectiveHandler.java L110 maps 208_25 → [217_34, 8_163 (WRONG dark side), 9_34], omits Prophecy Of The Force. Adjacent live logic shares only the Funeral Pyre card: V54.1/V29.14 (DeployEvaluator.java ~L5882, Luke Saga deck detection via 217_34 starting-location signature) and CardSelectionEvaluator.java ~L7407 (+1000 hard-prefer Funeral Pyre starting location) — these serve the Anger/Fear/Aggression Epic-Event deck, not this objective.
- **Gaps:** No live Rando handling of this objective's flip discipline or back peek/retrieve. Which Ewok Village copy (208_23 vs 8_73) is pulled is deck-dependent. Battleground counts DB-limited.

---

## 208_26 — Yavin 4 Base Operations / The Time To Fight Is Now (LIGHT)

- **CardRef:** 208_26 (Yavin 4 Base Operations), 208_26_BACK (The Time To Fight Is Now)
- **Java:** `set208/light/Card208_026.java` (back: `Card208_026_BACK.java`)
- **Filters used in Java:** Yavin_4_system, Massassi_War_Room, Restore_Freedom_To_The_Galaxy, system, battleground, battleground_system, Rebel, battleLocation, relatedSiteTo, occupies, snub_fighter, unique, piloted, liberated_system; Icon.VIRTUAL_SET_8
- **NamedCardRefs:**
	- Yavin 4 (system) → 1_135, 211_32 (both light); dark counterpart 1_296 separate per-side
	- Yavin 4: Massassi War Room → 1_139, 208_24 (both light)
	- Restore Freedom To The Galaxy → 208_17 (Epic Event, light)
- **LocationRequirements:** Deploy Yavin 4 system (required) + Massassi War Room (required) + Restore Freedom To The Galaxy (optional). Once/turn download: LOCATION, subtype system, Filters.battleground (any battleground system, either planet-icon, both force icons); ~30-40 battleground systems, samples Yavin 4 1_135, Hoth, Tatooine, Coruscant systems — DB-limited. Front flip: Rebels control two battleground_system. Back flip-back: occupy two battleground LOCATIONs. Back power bonus at battleLocation that is relatedSiteTo a system you occupy.
- **CharacterRequirements:** Rebel = Filters.Rebel (REBEL affiliation/icon), NOT a title fragment. Used two ways: front alt-flip "four Rebels on table" (canSpot 4 Rebel) and "Rebels control two battleground systems" (controlsWith Rebel). Back power modifier keys on piloted + unique + snub_fighter present at the related system (starship subtype + uniqueness + piloted state, not affiliation).
- **DYNAMIC state:** Back "liberated" system is runtime location state (canSpotLocation(liberated_system)), not a fixed id. Once/turn download picks ANY battleground system at deploy time (runtime choice). No chosen-planet/species WhileInPlayData binding.
- **Pull chain:** On deploy: required Yavin 4 system → required Massassi War Room → optional Restore Freedom To The Galaxy (0-1). Ongoing front top-level: once/turn pay 1 Force → DeployCardFromReserveDeck any battleground system.
- **Flip:** Front→Back (isTableChanged + canBeFlipped) when EITHER four Rebels on table OR Rebels control two battleground systems.
- **Flip-back:** Back→Front (isTableChanged + canBeFlipped) when NOT four Rebels on table AND you do NOT occupy two battleground locations.
- **Hard-lose:** None.
- **Existing Rando V-tags:** No live V-tags. Only reference is dead ObjectiveHandler.java L65-68/121 (title comment + Massassi War Room 1_139). ObjectiveAnalyzer (live brain) has zero references. No evaluator/strategy live handling.
- **Gaps:** Battleground-system download count not exactly enumerated, DB-limited (set601/Legacy absent).

---

## 209_29 — They Have No Idea We're Coming / Until We Win, Or The Chances Are Spent (LIGHT)

- **CardRef:** 209_29 (They Have No Idea We're Coming), 209_29_BACK (Until We Win, Or The Chances Are Spent). Set 9 [V] Premium.
- **Java:** `set209/light/Card209_029.java` (back: `Card209_029_BACK.java`)
- **Filters used in Java:** Scarif_system, DataVault, Stardust, Massassi_War_Room, Scarif_site, Scarif_location, Rogue_One, corvette, Chirrut, Baze, Rebel, trooper, Jedi, title(Taking_Them_With_Us), spy, undercover_spy, battleground, character, site, occupies, hasAttached(Stardust), with(self,Stardust); Keyword.SPY, Title.Undercover, Icon.PREMIUM, Icon.VIRTUAL_SET_9
- **NamedCardRefs:**
	- Scarif system → 209_23 (light); dark counterpart 216_13 — objective deploys the LIGHT 209_23
	- Scarif: Data Vault → 209_25 (light site)
	- Stardust → 209_18 (light Effect)
	- Yavin 4: Massassi War Room → 208_24, 1_139 (light site)
	- Rogue One → 206_7 (light starfighter); corvette sample Corellian Corvette 1_140 (light capital)
	- Baze Malbus With Cannon → 207_1; Chirrut Imwe → 207_2 (light characters)
	- Taking Them With Us → 9_5 (light Admiral's Order, ban target)
	- Scarif sites (light): 209_24 Beach, 209_25 Data Vault, 209_26 Landing Pad Nine, 209_27 Turbolift Complex; dark Scarif sites 216_14..216_17 separate ids
- **LocationRequirements:** Scarif_location = Scarif system + all Scarif sites. Light ids: system 209_23; sites 209_24/25/26/27. Flip gate counts CONTROL of 2; flip-back counts OCCUPY of 2. Scarif_site (SITE) is a once/turn download target alongside Rogue One / corvette. Massassi War Room pulled on deploy. battleground used on back for the drain-not-cancelable modifier. Scarif-location counts DB-limited.
- **CharacterRequirements:** Baze (Filters.Baze) and Chirrut (Filters.Chirrut) — persona, made SPY for rest of game. Rebel troopers = and(Rebel, trooper): affiliation via Rebel ICON + trooper KEYWORD, made SPY. Back bonuses apply to and(your, spy) via granted Keyword.SPY. Once/turn move targets and(your, spy, Rebel) movableAsRegularMove. Weapon-cancel targets and(Rebel, spy, not undercover_spy). Cost pays and(Rebel, character) from Lost Pile. Deploy bans: Jedi (classification) + Taking Them With Us (title).
- **DYNAMIC state:** None. Every requirement is a fixed named card or static subtype/keyword/icon filter. No runtime-chosen planet/system/species; nothing in WhileInPlayData.
- **Pull chain:** On-deploy required x4: Scarif system, Scarif: Data Vault, Stardust, Massassi War Room. Front once/turn top-level: DeployCardFromReserveDeck of or(Rogue_One, corvette, Scarif_site). All Reserve pulls.
- **Flip:** Front→Back when you control two Scarif locations.
- **Flip-back:** Back→Front (isTableChanged) if you do NOT occupy two Scarif locations AND cannot spot Rogue One at a Scarif site you occupy.
- **Hard-lose:** None. Only "place out of play" is a COST from your own Lost Pile (a Rebel character) to fuel back-side move / weapon-cancel.
- **Existing Rando V-tags:** No dedicated live logic / no V-tag. (1) Dead ObjectiveHandler.java:117 entry [216_13 Scarif (DARK, bug), 209_25 Data Vault, 1_139 Massassi War Room]. (2) ActionTextEvaluator.java:3606 penalizes revealing any card whose text contains "stardust" (generic reveal-danger heuristic). (3) Scarif hits in MoveEvaluator/DrawEvaluator are V79 = the DARK "Verge of Greatness" objective, unrelated. (4) CardSelectionEvaluator:76 lists "rogue one" in a generic unique-starship list. ObjectiveType.java:23 names this objective only as a doc example.
- **Gaps:** Scarif-location and corvette counts DB-limited. Corvette subtype not enumerated beyond sample 1_140. Massassi War Room resolves to two live ids (208_24 + classic 1_139); pull accepts either via Filters.Massassi_War_Room.

---

## 210_25 — The Hyperdrive Generator's Gone / We'll Need A New One (LIGHT)

- **CardRef:** 210_25 (The Hyperdrive Generator's Gone), 210_25_BACK (We'll Need A New One). VIRTUAL_SET_10, CORUSCANT, EPISODE_I icons.
- **Java:** `set210/light/Card210_025.java` (back: `Card210_025_BACK.java`)
- **Filters used in Java:** Wattos_Junkyard, City_Outskirts, Credits_Will_Do_Fine, hasAbilityOrHasPermanentPilotWithAbility, unique, alien, Republic_character, Republic_starship, Jedi, hasAbilityWhenUsingDejarikRules, Your_Destiny, Objective, system, Maul, QuiGon; BACK: Queens_Royal_Starship, Tatooine_location, battleground, Jar_Jar, Amidala, participatingInBattle; Icon.EPISODE_I, Icon.REFLECTIONS_II
- **NamedCardRefs:**
	- Tatooine: Watto's Junkyard → 12_87 (light), 12_178 (dark) [separate per-side ids, same title]
	- Tatooine: City Outskirts → 11_42 (light only in DB)
	- Credits Will Do Fine → 12_42 (base), 221_56 (V) (light)
	- Queen's Royal Starship → 12_91 (light) [BACK text only]
- **LocationRequirements:** Deploys 3 named cards from Reserve (Watto's Junkyard = Tatooine site, City Outskirts = Tatooine site, Credits Will Do Fine = Effect that stacks). Once/game upload target = any system with Icon.EPISODE_I (LOCATION/SYSTEM). Back references Tatooine_location (alien deploy-cost immunity) and battlegrounds occupied by Amidala/Jar Jar for the drain. EP I system + Republic counts DB-limited, not enumerated.
- **CharacterRequirements:** Ability-deploy restriction (front): allowed = unique aliens (unique+alien), Republic characters (Republic ICON), Republic starships ([Republic] icon), [Episode I] Jedi (Icon.EPISODE_I + Jedi keyword). Icon/keyword-based, not title. Maul immune to attrition unless present with Qui-Gon (Filters.Maul, Filters.QuiGon persona). Back: unique Republic characters power +1 / forfeit +2; control-phase drain scales with battlegrounds occupied by Amidala or Jar Jar (persona filters).
- **DYNAMIC state:** Once/game upload target is dynamic: any and(system, Icon.EPISODE_I) chosen at runtime, not a fixed id. Flip trigger keys off the live stack count under in-play Credits Will Do Fine (stacked cards), resolved at runtime.
- **Pull chain:** On deploy (required): DeployCardFromReserveDeck x3 → Watto's Junkyard, City Outskirts, Credits Will Do Fine. Top-level once/game: TakeCardIntoHandFromReserveDeck of an [Episode I] system. Back side pulls nothing from Reserve.
- **Flip:** Front→Back (isTableChanged) when 4+ cards stacked beneath Credits Will Do Fine (hasStackedCards(creditsWillDoFine, 4)).
- **Flip-back:** None. Back has no flip logic; one-way flip.
- **Hard-lose:** None on either side.
- **Existing Rando V-tags:** None. Grep for Hyperdrive Generator / Well_Need_A_New_One / Credits_Will_Do_Fine / Wattos_Junkyard / Queens_Royal_Starship in rando evaluators/ + strategy/ returned zero hits.
- **Gaps:** EP I system and Republic counts DB-limited, not enumerated. Watto's Junkyard has separate light (12_87) + dark (12_178) ids. Filters.Wattos_Junkyard / City_Outskirts resolve by title at runtime and will also match [V] reprints not in this DB snapshot.

---

## 211_36 — The Galaxy May Need A Legend / We Need Luke Skywalker (LIGHT)

- **CardRef:** 211_36 (The Galaxy May Need A Legend), 211_36_BACK (We Need Luke Skywalker). Set 11 / Episode VII.
- **Java:** `set211/light/Card211_036.java` (back: `Card211_036_BACK.java`)
- **Filters used in Java:** AhchTo_Saddle, and(battleground, Icon.EPISODE_VII), on(Title.Ahch_To), or(and(location, Icon.EPISODE_I), and(not(Icon.EPISODE_VII), character, abilityMoreThan(4))), Luke, not(AhchTo_location), AhchTo_location, and(Luke, on(Ahch_To)), Resistance_character, findFirstActive(self, Luke), any, and(unique, Resistance_character, your(self)), opponents(self), sameLocationAs, and(unique, Resistance_character, participatingInBattle)
- **NamedCardRefs:**
	- Ahch-To: Saddle → 210_1 (SITE, light, Set 10 / EP VII) — forced deploy target
	- Ahch-To → 211_48 (SYSTEM, light, Set 11) — only reachable via download, NOT the forced deploy
	- Luke → dynamic Filters.Luke, no fixed id
- **LocationRequirements:** Ahch-To: Saddle 210_1 (fixed forced deploy) + any [Episode VII] battleground (forced 2nd deploy): LOCATION + Icon.EPISODE_VII + battleground; DB-approx ~28 ids, samples 204_26 Jakku, 204_27 Jakku: Niima Outpost Shipyard, 204_28 Jakku: Ravager Crash Site. Ahch-To locations (download set): 6 in DB — 210_1 Saddle, 211_44 Luke's Hut, 211_45 Jedi Village, 211_46 Jedi Temple, 211_47 Cliffs, 211_48 system. EP VII counts DB-limited/undercounts.
- **CharacterRequirements:** Luke = Filters.Luke (persona), not title fragment — restricted to Ahch-To, placed out of play on flip. Resistance character = Filters.Resistance_character (Rebel/Resistance affiliation-icon based) — needed as battle participant to flip; two UNIQUE ones on back enable +1 drains / cancel-redraw. Deploy restriction targets non-[Episode VII] characters ability>4 (Icon.EPISODE_VII + abilityMoreThan(4)).
- **DYNAMIC state:** No deploy-time chosen planet/species/system. "Any EP VII battleground" forced deploy and the once/turn download are runtime player picks, not fixed ids. Luke resolved live via Filters.Luke. WhileInPlayData used only as a once/game latch for the Force-Pile take.
- **Pull chain:** On deploy (required, front): DeployCardFromReserveDeck Ahch-To: Saddle (210_1), then any [EP VII] battleground. Top-level front: once/game TakeCardIntoHandFromForcePile (reshuffle); once/turn (deploy phase) DeployCardFromReserveDeck an Ahch-To location OR [EP VII] battleground. Back top-level: once/game TakeCardIntoHandFromForcePile (only if front's unused); once/turn peek top Force Pile + Reserve Deck.
- **Flip:** Front→Back (optional): Luke on Ahch-To AND a battle just initiated involving a Resistance character (battleInitiated + isDuringBattleWithParticipant(Resistance_character) + canSpot and(Luke, on(Ahch_To))).
- **Flip-back:** None. Back is terminal.
- **Hard-lose:** No "you lose" text. On flip the back REQUIRES placing Luke out of play (PlaceCardOutOfPlayFromTableEffect, ignoring [Death Star II] restrictions) and blocks weapon firing for remainder of battle — a cost, not a game-loss.
- **Existing Rando V-tags:** No live V-tags. Dead ObjectiveHandler.java maps 211_36 → [211_48 Ahch-To system] — imprecise: forced deploy is Ahch-To: Saddle (210_1) + an EP VII battleground; 211_48 only via download. ObjectiveAnalyzer has NO entry. DeployPhaseScript.java:309-310 mentions Ahch-To only in a comment about download-ordering (Be With Me), not this objective.
- **Gaps:** EP VII battleground/location counts DB-limited undercounts. Dead ObjectiveHandler single-id mapping doesn't match the actual forced-deploy pair.

---

## 215_17 — Rescue The Princess / Sometimes I Amaze Even Myself (LIGHT)

- **CardRef:** 215_17 (Rescue The Princess), 215_17_BACK (Sometimes I Amaze Even Myself). Set 15 virtual.
- **Java:** `set215/light/Card215_017.java` (back: `Card215_017_BACK.java`)
- **Filters used in Java:** Death_Star_Central_Core, A_Power_Loss, Trash_Compactor, Detention_Block_Corridor, and(Icon.A_NEW_HOPE, Leia), your(playerId), Death_Star_site, Luke, abilityMoreThan(4), Jedi, icon(EPISODE_I), icon(EPISODE_VII), Leia; BACK: Set_Your_Course_For_Alderaan, I_Cant_Believe_Hes_Gone, ObiWan, at(Death_Star_site), on(Title.Death_Star), character, and(your, blaster)
- **NamedCardRefs:**
	- Death Star: Central Core → 215_6 (L, SITE), 1_283 (D, SITE)
	- A Power Loss → 215_2 (L, Effect)
	- Death Star: Trash Compactor → 1_125 (L), 215_9 (L); dark separate id, DB-limited
	- Death Star: Detention Block Corridor → 215_7 (L), 7_118 (L), 1_284 (D)
	- [A New Hope] Leia → runtime via Icon.A_NEW_HOPE + Filters.Leia; base A New Hope Leia ids NOT in this DB subset (DB-limited)
	- Rescue The Princess base → 7_139 (L); back base → 7_139_BACK (L)
	- Set Your Course For Alderaan, I Can't Believe He's Gone, Obi-Wan — back-side text/filter refs, not deployed
- **LocationRequirements:** Death Star sites — subtype SITE, title "Death Star:". Your DS sites get +1 Force gen; once/turn download any DS site. Named sites deployed: Central Core, Trash Compactor, Detention Block Corridor. DB-limited count: 12 light, 9 dark (set601/Legacy absent → real pool larger).
- **CharacterRequirements:** [A New Hope] Leia = Icon.A_NEW_HOPE + Filters.Leia (deployed imprisoned; base ANH Leia ids DB-limited absent). Luke ability>4 — may-not-deploy restriction. Jedi with [Episode I]/[Episode VII] icon — may-not-deploy restriction. Obi-Wan (back) = Filters.ObiWan at a DS site, placed out of play as cancel-battle cost. Your character with a blaster (back) — blaster "hit" trigger.
- **DYNAMIC state:** No fixed dynamic planet/species selection. State is board-derived: Leia's imprisonment location = the Detention Block Corridor on table (findFirstFromTopLocationsOnTable at runtime); flip gating keys off live spot of Leia at a DS site and A Power Loss "shut down" state. No WhileInPlayData-chosen system/species.
- **Pull chain:** When deployed (front), required from Reserve in order: (1) Central Core, (2) A Power Loss, (3) Trash Compactor, (4) Detention Block Corridor; then if Detention Block Corridor spottable in Reserve, (5) [A New Hope] Leia deployed TO it as an imprisoned captive (ignoreLocationDeploymentRestrictions, deployAsImprisonedCaptive). Ongoing: once/turn download any DS site (both sides). Both sides: if Leia about-to-leave-table, re-imprison her in Detention Block Corridor (cards on her → Used Pile).
- **Flip:** Front→Back (required): Leia occupies a Death Star site (occupiesWith self/owner, Death_Star_site + Leia) AND A Power Loss is "shut down" (isDeathStarPowerShutDown).
- **Flip-back:** Back→Front (required): Leia NOT at a Death Star site (!canSpot and(Leia, at(Death_Star_site)), INCLUDE_EXCLUDED_FROM_BATTLE).
- **Hard-lose:** None. Only place-out-of-play is a back-side COST: place Obi-Wan out of play (from a DS site) to cancel a battle initiated on the Death Star.
- **Existing Rando V-tags:** None. Grep for "Rescue The Princess", "Sometimes I Amaze", "A Power Loss", "Central Core", "Trash Compactor", "215_17" in rando evaluators/ + strategy/ returned zero hits.
- **Gaps:** DB (1874-entry subset) omits base-set [A New Hope] Leia and set601/Legacy, so named-card id lists and DS site counts are DB-limited, not exhaustive.

---

## 219_48 — Zero Hour / Liberation of Lothal (LIGHT)

- **CardRef:** 219_48 (Zero Hour), 219_48_BACK (Liberation of Lothal). Set 19 [V].
- **Java:** `set219/light/Card219_048.java` (back: `Card219_048_BACK.java`)
- **Filters used in Java:** Lothal_system, Lothal_site, Lothal_location, Rebel, Phoenix_Squadron_character, Jedi, Harc, Ahsoka, Kanan, Chopper, Ezra, Hera, Sabine, Zeb, battleground, hasAbilityOrHasPermanentPilotWithAbility, title(Menace_Fades), title(Projection_Of_A_Skywalker), your(playerId), controls, occupiesWith; Icon.EPISODE_I, Icon.EPISODE_VII, Keyword.PHOENIX_SQUADRON
- **NamedCardRefs:**
	- Lothal system → light 219_38 ("Lothal", SYSTEM); dark counterpart 219_10
	- Lothal sites LIGHT → 219_39 Capital City, 219_40 Comm Tower E-272 (Ezra's Roost), 219_41 Jedi Temple, 219_42 Tarkintown
	- Lothal sites DARK → 219_11 Advanced Projects Laboratory, 219_12 Capital City, 219_13 Imperial Complex, 219_14 Imperial Strip Mines
	- Menace Fades / Projection Of A Skywalker — cancel targets by title
	- Harc, Ahsoka, Kanan, Chopper, Ezra, Hera, Sabine, Zeb — persona Filters (deploy-cost/keyword), not pulled
- **LocationRequirements:** Lothal_location = title-family (Lothal system + all Lothal sites), spans BOTH sides. System = SYSTEM subtype; sites = SITE subtype. Lothal system + most sites carry BOTH DARK_FORCE + LIGHT_FORCE icons (battleground-capable); exception dark 219_13 Imperial Complex has DARK_FORCE only. DB count: 5 light (219_38..42) + 5 dark (219_10..14) = 10; samples 219_38, 219_39, 219_41. Legacy Lothal locations NOT counted — DB-limited.
- **CharacterRequirements:** Flip/back logic keys on Filters.Rebel (ICON) controlling Lothal locations, and Filters.Phoenix_Squadron_character (Keyword.PHOENIX_SQUADRON). Phoenix Squadron partly GRANTED by this objective at deploy: KeywordModifier adds PHOENIX_SQUADRON to Chopper, Ezra, Hera, Kanan, Sabine, Zeb (persona Filters). Deploy +2 targets: Harc; Jedi (Keyword.JEDI, except Ahsoka & Kanan personas); your cards with Icon.EPISODE_I or EPISODE_VII that have ability (or permanent pilot with ability). Detection by Keyword/Icon/persona, not title fragments.
- **DYNAMIC state:** WhileInPlayData used only as a per-turn boolean flag, NOT a chosen planet/species. FRONT: set true on completing any Force drain, cleared each turn (currently unused by front modifiers). BACK: set true on Force drain at a battleground; drives InPlayDataEqualsCondition on the +1 ForceDrainModifier for your other battleground drains; cleared end of turn. X for the destiny mod computed live = battlegrounds you occupy with Phoenix Squadron characters.
- **Pull chain:** On-deploy (required): DeployCardFromReserveDeck Lothal_system, then Lothal_site. Recurring top-level (both sides): once/turn deploy a Lothal_site from Reserve (GameTextActionId.ZERO_HOUR__DEPLOY_LOCATION). All pulls target Lothal locations only; no character/weapon pulls.
- **Flip:** Front→Back (required, isTableChanged, canBeFlipped): ( controlsWith(3, Lothal_location, Rebel) OR occupiesWith(3, Lothal_location, Phoenix_Squadron_character) ) AND opponent controls NO Lothal_location.
- **Flip-back:** Back→Front (required, isTableChanged, canBeFlipped): countTopLocations(Lothal_location controlled by opponent, INCLUDE_EXCLUDED_FROM_BATTLE) > count controlled by you.
- **Hard-lose:** None on either side.
- **Existing Rando V-tags:** None live. Only reference is dead ObjectiveHandler.java: put("219_48", ["219_10" (comment "Lothal (system)"), "title:Lothal:"]) — 219_10 is actually the DARK Lothal system; the LIGHT system deployed is 219_38. ObjectiveAnalyzer has no 219_48/Lothal/Phoenix handling. No V-tags in evaluators/*.java or other strategy/*.java.
- **Gaps:** Lothal filters are title-family spanning both sides, so the LIGHT objective can deploy/download either side's Lothal sites from Reserve (whatever is in deck). DB counts (10) exclude set601/Legacy → real pool may be larger. Dead ObjectiveHandler entry has stale/wrong system id (219_10 vs correct light 219_38) but is not executed.

---

## 221_67 — Hunt For The Droid General / He's A Coward (LIGHT)

- **CardRef:** 221_67 (Hunt For The Droid General), 221_67_BACK (He's A Coward). Set 21 [V], [Clone Army]/[Episode I].
- **Java:** `set221/light/Card221_067.java` (back: `Card221_067_BACK.java`)
- **Filters used in Java:** Icon.CLONE_ARMY, battleground, location, Clone_Command_Center, Cloning_Cylinders, Grievous_Will_Run_And_Hide, Grievous, alone, at(battleground), not(Icon.EPISODE_I), hasAbilityOrHasPermanentPilotWithAbility, Your_Destiny, Icon.REFLECTIONS_II, Objective, Jedi, Icon.PILOT, Icon.EPISODE_I, site, Title.No_Escape, occupies, clone, padawan, icon(CLONE_ARMY), battleLocation, movableAsRegularMoveUsingLandspeed; Zone.RESERVE_DECK, Zone.USED_PILE
- **NamedCardRefs:**
	- Clone Command Center → 221_54 (light/site)
	- Cloning Cylinders → 211_53 (light)
	- Grievous Will Run And Hide → 221_65 (light)
	- Grievous (flip check) → General Grievous 203_27 (DARK) persona; Filters.Grievous matches opponent's Grievous, a dark-side character referenced by this light objective
- **LocationRequirements:** Deploy target: a [Clone Army] battleground LOCATION (Icon.CLONE_ARMY + battleground + location). Battleground computed at runtime, not a DB icon; DB shows 7 LIGHT [Clone Army] locations: 221_48 Assembly Area (site), 221_52 Christophsis (system), 221_53 Christophsis: Chaleydonia (site), 221_54 Clone Command Center (site), 221_62 Geonosis (system), 221_63 Geonosis: Badlands Of N'g'zi (site), 221_74 Supply Route (site) — DB-limited superset (not all qualify as battlegrounds). Back X-count: battlegrounds occupied by your [Clone Army] cards (icon(CLONE_ARMY)). Both sides: end-of-opp-turn Force loss uses battleground + occupies.
- **CharacterRequirements:** Grievous = Filters.Grievous (persona/lore) — flip gate only (opponent's General Grievous), NOT a friendly requirement. Jedi = Filters.Jedi (Keyword.JEDI) → your Jedi gain [Pilot]. Padawan = Filters.padawan (back drain bonus needs a clone WITH a Jedi or Padawan at same location). Clone = Filters.clone (species/keyword) → back drain bonus + X>2 landspeed move target. No affiliation-icon character requirement to deploy; deploy requirement is location + named cards only.
- **DYNAMIC state:** The deployed [Clone Army] battleground is chosen at deploy time; Clone Command Center then keys off that card's getSystemName() to deploy to the same planet. So the target planet/system is dynamic (resolved at runtime from the chosen battleground). Back X is dynamic (count of battlegrounds your [Clone Army] cards occupy).
- **Pull chain:** On deploy (front), required chain from Reserve in order: (1) a [Clone Army] battleground location; (2) Clone Command Center to that location's system/planet; (3) Cloning Cylinders; (4) Grievous Will Run And Hide. All via DeployCardFromReserveDeckEffect (reshuffle=true).
- **Flip:** Front→Back (any table change): Grievous Will Run And Hide attached here AND NOT (Grievous alone at a battleground, INCLUDE_EXCLUDED_FROM_BATTLE). I.e. flip when Grievous is no longer alone at a battleground while GWRAH is here.
- **Flip-back:** Back→Front (any table change): Grievous Will Run And Hide NOT attached, OR Grievous IS alone at a battleground (INCLUDE_EXCLUDED_FROM_BATTLE).
- **Hard-lose:** None on either side.
- **Existing Rando V-tags:** None. No live evaluator/strategy logic. Only reference is dead ObjectiveHandler.java: put("221_67", [211_42 Kamino: Clone Birthing Center, 221_54 Clone Command Center, 211_53 Cloning Cylinders, 221_65 Grievous Will Run And Hide]) — dead map lists Kamino: Clone Birthing Center (211_42) which is NOT in the actual Java deploy chain.
- **Gaps:** Filters.battleground computed at runtime (no DB BATTLEGROUND icon), so the 7-location list is a superset; not all are battlegrounds. DB omits set601/Legacy → [Clone Army] counts DB-limited. Grievous flip check references opponent's General Grievous (dark 203_27); Filters.Grievous is persona/lore-based, not verified against every Grievous variant.

---

## 222_27 — The Empire Knows We're Here / Prepare For Ground Assault (LIGHT)

- **CardRef:** 222_27 (The Empire Knows We're Here), 222_27_BACK (Prepare For Ground Assault). Set 22, Rarity V, Icon.HOTH + VIRTUAL_SET_22.
- **Java:** `set222/light/Card222_027.java` (back: `Card222_027_BACK.java`)
- **Filters used in Java:** Hoth_system, Main_Power_Generators, Second_Marker, Third_Marker, Ice_Storm, system, Leia, character, abilityMoreThan(4), Echo_Command_Center, marker_site, battleground, Hoth_location, Admirals_Order, unique, trooper, gunner, pilot, Hoth_Sentry, Sunsdown, vehicle_weapon, artillery_weapon; Icon.SPECIAL_EDITION, Title.Alter
- **NamedCardRefs:**
	- Hoth system → 3_55 (light SYSTEM); dark same title 3_143
	- Main Power Generators / 1st Marker → 3_61, 210_15 (light), 222_9 (dark); pulled from own Reserve so light 3_61/210_15
	- Echo Command Center → 3_57 (light), 3_145 (dark) (Hoth: Echo Command Center (War Room))
	- Ice Storm (banned) → 3_37 (light), 3_104 (dark)
	- [Special Edition] Leia (banned) → Icon.SPECIAL_EDITION + Filters.Leia, not a fixed id
	- Echo Base Garrison (immune to Alter) → 111_3 (light)
	- Hoth Sentry (cancelled by back) → 7_64 (light); Sunsdown (cancelled by back) → 1_230 (dark)
- **LocationRequirements:** marker_site (subtype/keyword filter) — Hoth marker sites: 1st (Main Power Generators) 3_61/210_15 L, 222_9 D; 2nd (Snow Trench) 3_63/222_22 L, 3_63; 3rd (Defensive Perimeter) 3_56/223_38 L, 3_144 D; 4th (North Ridge) 3_62 L, 3_149/217_12 D; 5th (Ice Plains) 3_148 D/208_49 D. Retrieve/peek keyed on and(battleground, marker_site) and and(Hoth_location, battleground) you occupy. Hoth system (3_55 L) — force-drain +1 site + flip anchor via Hoth_location. Echo Command Center 3_57 L — download target. Marker-site id inventory DB-limited.
- **CharacterRequirements:** No affiliation/species character REQUIREMENT to deploy. Constraints only: Front MayNotPlay characters ability>4 (abilityMoreThan(4)); bans [Special Edition] Leia (Icon.SPECIAL_EDITION + Leia). Back CancelsGameText on your unique characters EXCEPT gunners/pilots/troopers — role via Filters.trooper/gunner/pilot (Keyword-based), plus Filters.Admirals_Order.
- **DYNAMIC state:** None runtime-chosen. Objective fixed to Hoth (Hoth system, Hoth marker sites, Hoth_location). Only "dynamic" value is back-side X = count of Hoth battlegrounds you occupy (Variable.X via countTopLocationsOnTable), not a chosen planet/species.
- **Pull chain:** On-deploy required: DeployCardFromReserveDeck Hoth system, then Main Power Generators / 1st Marker. Recurring once/turn top-level: DeployCardFromReserveDeck or(Echo_Command_Center, marker_site) — present on BOTH sides.
- **Flip:** Front→Back (required, table change): opponent occupies your Hoth location (INCLUDE_EXCLUDED_FROM_BATTLE, and(your, Hoth_location)).
- **Flip-back:** Back→Front (required, table change): opponent does NOT occupy your Hoth location.
- **Hard-lose:** None on either side.
- **Existing Rando V-tags:** None. No rando evaluator/strategy logic targets this LIGHT objective. Grep hits for "222_9"/"marker" in dead ObjectiveHandler.java and ObjectiveAnalyzer.java refer to the DARK Hoth/AT-AT theme, not this card. No V-tags handle 222_27.
- **Gaps:** DB omits set601/Legacy → marker-site id inventory DB-limited. Filters.marker_site is a subtype/keyword match resolved at runtime, so it also picks up marker sites not in the DB snapshot.

---

## 225_53 — Mind What You Have Learned / Save You It Can (LIGHT)

- **CardRef:** 225_53 (Mind What You Have Learned), 225_53_BACK (Save You It Can). Set 25 virtual, Rarity V. Icons: SPECIAL_EDITION, DAGOBAH, VIRTUAL_SET_25.
- **Java:** `set225/light/Card225_053.java` (back: `Card225_053_BACK.java`)
- **Filters used in Java:** Beldons_Corridor, Yodas_Hut, and(Icon.DAGOBAH, Yoda), and(Icon.CLOUD_CITY, No_Disintegrations), Patience, or(Bespin_system, Cloud_City_site), or(Wise_Advice, Yodas_Hope), Dagobah_location, and(Luke, On_Dagobah), and(character, at(non_battleground_location)), Sense, and(your(playerId), not(Icon.DAGOBAH), abilityMoreThan(4), not(Ahsoka)); BACK: Luke, weapon, completed_Jedi_Test, and(Icon.CLOUD_CITY, Rebel), battleground, captive, sameSiteAs(self, and(Icon.CLOUD_CITY, Rebel))
- **NamedCardRefs:**
	- Beldon's Corridor → 225_40 (Cloud City: Beldon's Corridor, light site)
	- Yoda's Hut → 216_26, 4_89 (Dagobah: Yoda's Hut, light site)
	- Yoda [Dagobah] → 207_10, 4_2 (light char) deployed to Hut
	- No Disintegrations! [Cloud City] → 225_55 (CC-icon Effect). Non-CC 4_28 EXCLUDED by Icon.CLOUD_CITY
	- Patience! → 225_57 (Epic Event, light)
	- Wise Advice → 7_81 (Effect). NOTE 13_47 is a DEFENSIVE_SHIELD, not the download target
	- Yoda's Hope → 225_58, 4_44 (Effect, light)
	- Bespin system → 5_76 (light SYSTEM)
	- Ahsoka (exempt), Luke (flip trigger + back react) — Filters
- **LocationRequirements:** On-deploy pulls: Cloud City site (Beldon's Corridor 225_40) + Dagobah site (Yoda's Hut 216_26/4_89). OPT download: Bespin system (5_76) OR a Cloud City site (Filters.Cloud_City_site, Icon.CLOUD_CITY + SITE, ~11 light in DB, e.g. 224_13, 225_40). OPT download: a Dagobah location (Filters.Dagobah_location, Icon.DAGOBAH on LOCATION, ~14 light in DB incl. Anoat 201_16/4_80, Dagobah 217_33, Yoda's Hut 216_26). Back drain rule keys on battleground sites where a [Cloud City] Rebel is present. Counts DB-limited.
- **CharacterRequirements:** Yoda [Dagobah] required at Yoda's Hut on deploy — Icon.DAGOBAH + Filters.Yoda (persona). Deck must be [Dagobah]-icon characters: your non-[Dagobah] chars ability>4 are lost, EXCEPT Ahsoka — your(playerId) + not(Icon.DAGOBAH) + abilityMoreThan(4) + not(Ahsoka). Luke = Filters.Luke (persona) flip enabler + back react target. Back Lost-Pile pull targets [Cloud City] Rebel — Icon.CLOUD_CITY + Filters.Rebel (Rebel = affiliation icon).
- **DYNAMIC state:** None. All targets are fixed named cards / icon+subtype filters; no runtime-chosen planet/system/species. WhileInPlayData not used for target selection.
- **Pull chain:** Deploy trigger fires 5 required Reserve pulls in order: (1) Beldon's Corridor, (2) Yoda's Hut, (3) [Dagobah] Yoda to the Hut, (4) [Cloud City] No Disintegrations!, (5) Patience!. Ongoing OPT top-level pulls from Reserve: Bespin system / Cloud City site (OPT), Dagobah location (OPT), Wise Advice / Yoda's Hope (while front up, no per-turn limit shown beyond canDeploy). Back also has OPT Bespin/CC-site pull.
- **Flip:** Front→Back: canBeFlipped(self) AND canTarget and(Luke, On_Dagobah) AND isDuringYourTurn (top-level OTHER_CARD_ACTION_1 with FlipCardEffect).
- **Flip-back:** None. Back defines no flip-to-front; one-way flip.
- **Hard-lose:** None. Only place-out-of-play is a back COST (place a completed Jedi Test out of play to pull a [Cloud City] Rebel). Front's non-Dagobah ability>4 characters are made LOST (LoseCardsFromTableEffect), not out-of-play, and it's a persistent required trigger, not a hard-lose.
- **Existing Rando V-tags:** No V-tag / ObjectiveAnalyzer (live brain) logic for 225_53. Only reference is dead ObjectiveHandler.java L83-89: put("225_53", [225_40, 4_89, 4_28, "title:Patience"]) — stale/wrong: lists 4_28 (non-Cloud-City No Disintegrations) but the objective's actual filter is and(Icon.CLOUD_CITY, No_Disintegrations) = 225_55. MoveEvaluator/CardSelectionEvaluator "Beldon" hits are unrelated generic solo-move/deploy comments.
- **Gaps:** DB lacks set601/Legacy → Dagobah-location (~14) and Cloud-City-site (~11) counts are DB-limited lower bounds. Back blueprint id assumed "225_53_BACK" per convention (front file is Card225_053_BACK.java); not separately verified in blueprint DB. Two ids each for Yoda's Hut (216_26, 4_89) and Yoda (207_10, 4_2) — filter accepts any matching title/icon, engine picks at pull time.

---

## 226_28 — The Hidden Path / Gather Allies And Train (LIGHT)

- **CardRef:** 226_28 (The Hidden Path), 226_28_BACK (Gather Allies And Train). SET_26, Rarity V.
- **Java:** `set226/light/Card226_028.java` (back: `Card226_028_BACK.java`)
- **Filters used in Java:** Mining_Village, Safehouse, Underground_Corridor, Fallen_Order, Jabiim_location, Jabiim_site, holocron, Mapuzo_site, Mapuzo_location, Jedi, Jedi_Survivor, Anakin, A_Jedis_Resilience, Weapon_Levitation, Nabrun_Leids, generic, location, site, battleground_site, character, abilityMoreThan(4), opponents(self)
- **NamedCardRefs:**
	- Mapuzo: Mining Village → 226_21 (light, SITE)
	- Mapuzo: Safehouse → 226_22 (light, SITE)
	- Mapuzo: Underground Corridor → 226_23 (light, SITE)
	- Fallen Order → 226_14 (light, EPIC_EVENT)
- **LocationRequirements:** Named Mapuzo sites (226_21/22/23) + Fallen Order (226_14) auto-deploy from Reserve. Jabiim locations (Filters.Jabiim_location): SITE subtype, Jabiim system icon — DB shows 2 light: 226_15 Path Operations Center, 226_16 Starship Hangar (plus any set601/Legacy not in DB). Mapuzo sites (Filters.Mapuzo_site/Mapuzo_location) get -1 drain and are excluded from the flip's "non-Mapuzo site" count. battleground_site used by back-side relocate + end-of-turn Force loss trigger. Counts DB-limited.
- **CharacterRequirements:** Jedi (Filters.Jedi = Keyword.JEDI) is the core actor for occupation/flip/relocate triggers. Jedi_Survivor (Filters.Jedi_Survivor) is the exception carve-out: survivors CAN deploy and get -1 deploy on back; non-survivor Jedi + Anakin are locked out. abilityMoreThan(4): opponent characters ability>4 drive the -1 total battle destiny penalty (back). No affiliation/species/senator detection; role gating is purely Jedi vs Jedi-survivor keyword.
- **DYNAMIC state:** None. No runtime-chosen planet/system/species. All targets are fixed Filters (Mapuzo/Jabiim named systems, holocron keyword, Jedi/Jedi-survivor). Jabiim site chosen at relocate time is a normal target choice, not persisted WhileInPlayData.
- **Pull chain:** On-deploy (front, REQUIRED): DeployCardFromReserveDeck x4 → Mining Village, Safehouse, Underground Corridor, Fallen Order (all forced=true). Ongoing OPT [download]: (front+back) a Jabiim location (226_15 Path Operations Center, 226_16 Starship Hangar, plus any Legacy not in DB); (front only) a holocron (Filters.holocron). Both sides: relocate/deploy tools do not pull further named cards.
- **Flip:** Front→Back (isTableChanged, canBeFlipped): Jedi occupy 2 non-Mapuzo sites (occupiesWith count 2, and(not(Mapuzo_location), site), INCLUDE_EXCLUDED_FROM_BATTLE, Filters.Jedi). Singleton trigger.
- **Flip-back:** Back→Front (isTableChanged, canBeFlipped): NOT(Jedi occupy 2 non-Mapuzo sites) — same predicate negated. Singleton trigger.
- **Hard-lose:** None. (Back-side holocron "about to leave table" → PlaceCardInUsedPileFromTable is a save, not a loss.)
- **Existing Rando V-tags:** None objective-specific. Grep of rando evaluators/ + strategy/ found only GENERIC references: ObjectiveAnalyzer.java:555 comment lists Fallen Order as an example epic-event; DeckOracle.java:1088 has a generic "holocron" keyword branch in the pull-target parser. No V-tag scores or handles The Hidden Path, Jabiim, Mapuzo, or Jedi-survivor strategy. Light objective, unhandled by Rando as expected.
- **Gaps:** Holocron count DB-limited (keyword not populated in DB JSON; only title-match found 1 light). Jabiim/Mapuzo/Legacy set601 cards absent from DB so location counts are floors, not totals.

---

### Cross-cutting notes
- **All id/count claims are DB-limited:** card_blueprint_database JSON omits set601/Legacy cards, so every enumeration is a lower-bound approximation.
- **ObjectiveHandler.java is DEAD code** (project memory): live brain = ObjectiveAnalyzer. Multiple stale/wrong entries surfaced here: 208_25 (uses DARK Ewok Village 8_163, omits Prophecy), 209_29 (DARK Scarif 216_13), 219_48 (DARK Lothal 219_10 vs correct light 219_38), 221_67 (lists Kamino 211_42 not in the actual chain), 225_53 (lists non-CC No Disintegrations 4_28 vs correct 225_55).
- **Zero dedicated Rando handling** for all 11 objectives: 208_25, 208_26, 209_29, 210_25, 211_36, 215_17, 219_48, 221_67, 222_27, 225_53, 226_28. All fall through to GENERIC weighting or ObjectiveType-comment classification only. No live V-tag targets any of these LIGHT objectives.
- **Adjacent/generic live logic exists but is NOT objective-specific:** 208_25 shares the Funeral Pyre card with V54.1/V29.14 (Luke Saga Epic-Event deck); 209_29 touches ActionTextEvaluator:3606 "stardust" reveal-penalty + V79 (DARK Verge of Greatness); 226_28 touches ObjectiveAnalyzer:555 (Fallen Order example) + DeckOracle:1088 (holocron parser).
- **One-way flips (no flip-back):** 210_25, 211_36, 225_53. **Terminal back with a place-Luke-out-of-play cost:** 211_36. **`found=false`:** none — all 11 report `found=true`.