# Bottom-up objective intel (inventory rows 40-57) — DRAFT, verify against source

Generated 2026-07-07 by K-2 fast agents (Filters-from-Java extraction). This is INTEL to help Codex fill the inventory + for K-2 to cross-check. NOT gospel — every id/title must be verified against real card source before wiring. Known caveat: Invasion row lists 14_114 as both the back-side id AND Blockade Flagship — an agent collision to resolve.

---

# Bottom-up objective intel (rows 40-57)

Compiled from 18 bottom-up objective records for Codex to fold into the inventory and for K-2 to cross-check against Codex's top-down pass. All 18 records are `found=true`. Data below is verbatim from the source scans; nothing invented. Uncertain/approximate items are flagged in each Gaps line and summarized at the end.

---

## 1. `13_73` — Let Them Make The First Move / At Last We Will Have Revenge
- **CardRef:** ids `13_73` — "Let Them Make The First Move" / "At Last We Will Have Revenge"
- **Java:** set13/dark/Card13_073.java (front) + Card13_073_BACK.java (back)
- **Filters-used-in-Java:** Theed_Palace_Generator_Core, Theed_Palace_Generator, Deep_Hatred, sameLocationAs, opponents, Jedi, Naboo_site, your, Dark_Jedi, character, presentWith, at, interior_Theed_Palace_site, undercover_spy, R2D2, presentAt, interior_Naboo_site, battleground_site, wherePresent, canBeTargetedBy, present
- **NamedCardRefs:** Deep Hatred → 13_65 (EPIC_EVENT, DARK); Theed Palace Generator → 13_76 (SITE DARK; LS 13_31/220_8 exist but objective deploys own DARK copy); Theed Palace Generator Core → 13_77 (SITE DARK; LS 13_32 exists)
- **LocationRequirements:** interior Theed Palace site (target needs your Dark Jedi there) ~12 candidates e.g. 13_76, 13_77, 12_173, 12_174, 14_112; interior Naboo battleground site (flip gate on opp Jedi present) ~12 candidates e.g. 13_76, 13_77, 12_173, 12_174; Naboo site = uncancelable-drain scope
- **CharacterRequirements:** your Dark Jedi (drain-uncancelable + target-to-lose + back lightsaber combat); opponent's Jedi (blocks their drain, is combat target, drives both flips); target-to-lose = opp character present with your Dark Jedi at interior Theed Palace site (game-text can restrict to undercover_spy / R2D2)
- **Pull chain:** On deploy (required, in order): Theed Palace Generator Core (13_77), Theed Palace Generator (13_76), Deep Hatred (13_65). All forced deploy. No further Reserve pulls.
- **Flip:** FRONT→BACK required on any table change when opp Jedi present at an interior Naboo battleground site
- **Flip-back:** BACK→FRONT when opp has NO Jedi at any interior Naboo battleground site; also retrieve 1 Force on flip-back
- **Hard-lose:** None. Only card-loss effects are offensive (front control-phase target-is-lost; back lightsaber combat)
- **Rando V-tags:** None. Single grep hit (CardSelectionEvaluator.java:1014) is an unrelated comment.
- **Gaps:** Battleground count approximated via both-force-icons; ~12 interior Naboo battleground + ~12 interior Theed Palace sites in DB. interior_Theed_Palace_site / Naboo_site are engine-named filters, not re-derived from icons. **[APPROX]**

---

## 2. `14_113` / `14_114` — Invasion / In Complete Control
- **CardRef:** ids `14_113`, `14_114` — "Invasion" / "In Complete Control"
- **Java:** set14/dark/Card14_113.java + Card14_113_BACK.java
- **Filters-used-in-Java:** FRONT: Naboo_system, Blockade_Flagship, Naboo_Swamp, Droid_Racks, Naboo_site, Theed_Palace_Throne_Room, Neimoidian, hasAbilityOrHasPermanentPilotWithAbility, hasAbilityWhenUsingDejarikRules, Icon.TRADE_FEDERATION+starship, character+loreContains("Trade Federation"), Civil_Disorder, CancelForceIconsModifier(Naboo_system, opponent). BACK: battle_droid, Icon.PRESENCE+droid, Neimoidian, droid_starfighter, site, Interrupt, Naboo_system, Theed_Palace_Throne_Room
- **NamedCardRefs:** Naboo system → 12_169 (D), 12_78 (L), 216_10 (D); Blockade Flagship → 14_114 (D); Naboo: Swamp → 12_171 (D), 12_80 (L); Droid Racks → 14_96 (D), 208_39 (D); Naboo: Theed Palace Throne Room → 12_174 (D), 12_83 (L); Civil Disorder → 5_20 (D, canceled)
- **LocationRequirements:** Naboo system (12_169); Naboo: Swamp (12_171); Droid Racks (14_96); Theed Palace Throne Room (12_174, flip control target); "a Naboo site" = 8 dark Naboo sites e.g. 12_170, 12_172, 12_173
- **CharacterRequirements:** Neimoidian at Throne Room (flip req); [Presence] droids (back draw-2-BD); battle droids (back deploy -1); droid starfighters (back attrition immunity); Trade Federation-lore chars (deploy-restriction exception)
- **Pull chain:** On deploy (required, in order): Naboo system → Blockade Flagship aboard it → Naboo: Swamp → Droid Racks. While front: once/deploy phase deploy one Naboo site from Reserve.
- **Flip:** FRONT→BACK when you control Theed Palace Throne Room with a Neimoidian there AND control Naboo system
- **Flip-back:** BACK→FRONT when opponent controls Naboo system OR Theed Palace Throne Room
- **Hard-lose:** None (only PlaceDestinyCardOutOfPlay targets opp battle-destiny Interrupt)
- **Rando V-tags:** isInvasion flag in ObjectiveAnalyzer (~line 171). V86 INVASION live in ObjectiveAnalyzer (~406); DeployEvaluator copy (~1581-1648) COMMENTED OUT/superseded. V142 (ActionTextEvaluator ~674-737) Blockade Flagship site deploy-mode (replaces V29.7). V177 (DeckOracle ~1320) flags passing "Deploy a Blockade Flagship site" while Bridge in Reserve. DeckOracle ~1142 handles the once/deploy-phase Naboo-site pull. ActionTextEvaluator ~4464 notes Invasion pull-parser produced no firing. ObjectiveHandler entry exists but is DEAD code.
- **Gaps:** **[FLAG]** ObjectiveHandler (dead) lists Blockade Flagship as `14_111`, but real DB id is `14_114` (14_111 is a different card) — harmless since dead path. Blockade_Flagship / Droid_Racks / Naboo_system filter defs not opened (assumed persona/title); may match unenumerated virtual reprints. **[UNCERTAIN]**

---

## 3. `109_12` — This Deal Is Getting Worse All The Time / Pray I Don't Alter It Any Further (base TDIGWATT)
- **CardRef:** ids `109_12` (base, Card109_012) and note `226_12` (virtual "(V)", different upload targets/clauses) — "This Deal Is Getting Worse All The Time"
- **Java:** set109/dark/Card109_012.java + Card109_012_BACK.java
- **Filters-used-in-Java:** FRONT deploy: Cloud_City_battleground_site (required); Secret_Plans (0..1); All_Wrapped_Up (0..1); upload once/deploy phase: or(Bespin_system, Bespin_Cloud_City, Dark_Deal, Cloud_City_Occupation); immunity: your + or(CLOUD_CITY, JABBAS_PALACE, SPECIAL_EDITION) + character immune to Goo_Nee_Tay at Bespin_location; flip: canSpot Dark_Deal + occupies Bespin_system + occupies Bespin_Cloud_City. BACK: ImmuneToTitle Dark_Deal vs Surreptitious_Glance; SuspendsCard The_Planet_That_Its_Farthest_From targeting Bespin_system; ForceDrainsMayNotBeModified at Bespin_location controlsWith Imperial; TotalBattleDestiny your alien with your Imperial in battle + ConditionEvaluator(2,4, Ugnaught); before-trigger isPlayingCard All_Too_Easy → opp loses 8; flip: isBlownAwayLastStep SYSTEM titled Bespin OR justCanceled Dark_Deal OR opp controls Bespin_system
- **NamedCardRefs:** Dark Deal → 5_115, 223_9; Cloud City Occupation → 7_223; Bespin system → 5_164/223_8 (D), 5_76 (L); Bespin: Cloud City → 5_165 (D), 5_77 (L); Secret Plans → 7_240, 13_86; All Wrapped Up → 6_141, 224_1; All Too Easy → 5_112; The Planet That It's Farthest From / Surreptitious Glance / Goo Nee Tay referenced by Filter/Title only
- **LocationRequirements:** required deploy = 1 Cloud City battleground site (SITE+CLOUD_CITY+both force icons), ~8 candidates e.g. 221_11, 226_1, 5_166; flip gate needs occupation of Bespin system (5_164/223_8) + Bespin: Cloud City (5_165) + Dark Deal on table; back drain-protection at any Bespin_location controlled with Imperial
- **CharacterRequirements:** no named-character upload; immunity keys on CLOUD_CITY/JABBAS_PALACE/SPECIAL_EDITION + character; back BD bonus keys on alien+Imperial pair in battle, +2 more if alien is Ugnaught (Keyword/Species); back drain protection needs Imperial at Bespin location
- **Pull chain:** FRONT deploy: required Cloud City battleground site + optional Secret Plans + optional All Wrapped Up from Reserve. Once/deploy phase upload one of Bespin system / Bespin: Cloud City / Dark Deal / Cloud City Occupation into hand.
- **Flip:** FRONT→BACK when Dark Deal on table AND you occupy Bespin system AND Bespin: Cloud City
- **Flip-back:** if Dark Deal canceled, OR opp controls Bespin system, OR Bespin blown away
- **Hard-lose:** None (only flip-back)
- **Rando V-tags:** Heavy coverage (mostly written around virtual 226_012). ActionTextEvaluator: V24 EXHAUSTED SEARCH GUARD (~1871, -400); V177 CATEGORY RESCUE (~325); V26/V29.6 DINING ROOM deploy-Lando (~3934); Bespin-system occupation priority (~4179); admiral→Executor chain (~3083/3140). ObjectiveAnalyzer: V29 UPDATED 2026-07-06 "TDIGWATT bug B" forbids Executor (~1364, applies to 226 not base 109). DeployPhasePlanner: Executor-to-Bespin priority (~1296) + HOLD_BACK plan (~1725). Also DrawEvaluator, MoveEvaluator, DeployEvaluator, CardSelectionEvaluator. (HOLD_BACK reserved for TDIGWATT per feedback_deploy_philosophy.)
- **Gaps:** **[FLAG]** base 109 vs virtual 226 have DIFFERENT upload sets; 226 adds "may not deploy Admiral's Orders/Executor" absent from base; most V-tags target 226. Bespin_system filter matches both D and L copies (Rando uploads its own dark). 8-site count is **[APPROX]** from DB icon scan.

---

## 4. `110_6` — Court Of The Vile Gangster / I Shall Enjoy Watching You Die
- **CardRef:** ids `110_6` — "Court Of The Vile Gangster" / "I Shall Enjoy Watching You Die"
- **Java:** set110/dark/Card110_006.java + Card110_006_BACK.java
- **Filters-used-in-Java:** **[NOT ENUMERATED in source record — field = "see array"; treat as unresolved]**. Referenced filters inferable from other fields: Tatooine_battleground_site, Jabbas_Palace_site, Tatooine_site, bounty_hunter, non_droid_character, captive, character, docking bay / Independent starship
- **NamedCardRefs:** Audience Chamber → 6_162 (D; L 6_81 is opp copy); Great Pit Of Carkoon → 6_170, 221_34; Dungeon → 6_164; Sarlacc → 107_5, 7_214; Rancor → 6_139; Rancor Pit → 6_166; Trap Door → 6_159; Goo Nee Tay / Scanning Crew referenced as forfeit/may-not-play (opp cards, not pulled)
- **LocationRequirements:** FRONT when-deployed pulls 3 specific sites: Audience Chamber (6_162), Great Pit Of Carkoon (6_170/221_34), Dungeon (6_164); Tatooine battleground site (force-loss avoidance) subset of ~59 Tatooine SITE cards, samples 106_18, 112_20, 11_92; Jabba's Palace site (flip captive loc) ~9 dark, samples 6_162, 6_163, 112_12; Tatooine site (back flip); docking bay OR Independent starship (repeatable front pull); Sarlacc/Rancor/Rancor Pit (repeatable back pull)
- **CharacterRequirements:** bounty_hunter (forfeit +2, immune to Goo Nee Tay on deploy); non_droid_character at Tatooine battleground site (presence avoids 1 Force loss end of deploy); captive at Jabba's Palace site (front flip) / at Tatooine site (back flip); opp character eaten by Rancor/Sarlacc (back force loss + place OOP)
- **Pull chain:** FRONT when deployed: required Audience Chamber + Great Pit Of Carkoon + Dungeon. Then once/deploy phase docking bay OR [Independent] starship (repeatable). BACK: once/deploy phase Sarlacc/Rancor/Rancor Pit (repeatable).
- **Flip:** FRONT→BACK when you have two captives (or a captive of ability >2) at any Jabba's Palace site(s)
- **Flip-back:** BACK→FRONT when NO captives at Tatooine sites AND opp has NO character at same site as a Rancor
- **Hard-lose:** None (back place-OOP is offensive — opp characters eaten)
- **Rando V-tags:** DEAD code only. ObjectiveHandler maps "110_6" → 6_162, 6_170, 6_164 (dead). ObjectiveType lists it as COMBO example (comment). DeployEvaluator V190 (2026-07-04) uses this objective's game as origin case for "starships deploy to systems not docking bays" gate. No dedicated live V-tag.
- **Gaps:** **[FLAG]** filtersUsedInJava not enumerated in source ("see array"). Back side in separate BACK.java (no separate DB blueprint; DB only has 110_6). Multi-id cards (Carkoon/Sarlacc/Audience Chamber); ObjectiveHandler used 6_170/6_162 specifically. Live logic has no 110_6-specific entry. **[UNCERTAIN]**

---

## 5. `111_6` — Set Your Course For Alderaan / The Ultimate Power In The Universe
- **CardRef:** ids `111_6` — "Set Your Course For Alderaan" / "The Ultimate Power In The Universe"
- **Java:** set111/dark/Card111_006.java + Card111_006_BACK.java. Filter def: gemp-swccg-logic Filters.java:18144 (Deployable_By_SYCFA)
- **Filters-used-in-Java:** Death_Star_system, Deployable_By_SYCFA = and(DARK, or(Alderaan_system, title(Jedha City))), Docking_Bay_327, Revolution, planet_system, Alderaan_system, Yavin_4_system, Hoth_system, Subjugated_system, Commence_Primary_Ignition, Death_Star_site, SYSTEM+title(Alderaan), Star_Destroyer, Victory_class_Star_Destroyer, battleground_system+sameSystemAs(Star_Destroyer), Imperial_class/Super_class_Star_Destroyer, system+battleground (back upload), SYSTEM+title(Yavin_4), SYSTEM+title(Death_Star), Yavin_4_site+opponents
- **NamedCardRefs:** Death Star system → 2_143 (D), 216_7 (V); Alderaan system → 1_281 (D); Jedha City → alt deploy target (resolve by Title.Jedha_City, dark loc text ~line 26632); Docking Bay 327 → 1_285 (D); Yavin 4 system → 1_296 (D); Hoth system → 3_143 (D); Revolution → 17146 (L, canceled); Commence Primary Ignition → 2_130, 209_45 (D, target-restricted)
- **LocationRequirements:** Death Star system exact ~2 dark (2_143 base, 216_7 [V]); Deployable_By_SYCFA = DARK Alderaan (1_281) OR Jedha City ~2; Docking Bay 327 dark 1_285; BACK upload any battleground SYSTEM ~40-60 candidates, samples 1_281, 1_296, 3_143; Commence Primary Ignition legal targets only Alderaan/Yavin4/Hoth/Subjugated
- **CharacterRequirements:** none (keyword/species/persona). Back keys off Star Destroyers (Star_Destroyer, Victory/Imperial/Super class) for deploy discount + drain bonus
- **Pull chain:** On deploy (required, in order): Death_Star_system → Deployable_By_SYCFA (Alderaan system OR Jedha City) → Docking_Bay_327. After deploy: proxy cancels Revolution + restricts Commence Primary Ignition. Front top-level: once/deploy phase take a "Death Star"-title card into hand from Reserve. Back top-level: once/deploy phase deploy a battleground system from Reserve.
- **Flip:** FRONT→BACK when Alderaan (SYSTEM titled Alderaan) blown away
- **Flip-back:** None. Front→back only; back terminal.
- **Hard-lose:** Back places objective OOP if Death Star (SYSTEM titled Death Star) blown away
- **Rando V-tags:** None dedicated. Incidental only: AstrogatorPersonality.java:87 flavor quip; ObjectiveType.java lines 11+23 doc-comment category examples ("SYCFA"). No evaluator/strategy V-tag for pull chain/flip/SD drain math.
- **Gaps:** No Rando code targets SYCFA. **[UNCERTAIN]** Jedha City numeric id not resolved (Title only). Battleground-system count rough **[APPROX]**. Docking_Bay_327 / Subjugated_system filter defs not opened (assumed standard).

---

## 6. `112_15` — My Kind Of Scum / Fearless And Inventive
- **CardRef:** ids `112_15` — "My Kind Of Scum" / "Fearless And Inventive" (virtual reprint 601_010 shares title, not fully diffed)
- **Java:** set112/dark/Card112_015.java + Card112_015_BACK.java
- **Filters-used-in-Java:** **[NOT ENUMERATED in source — field = "placeholder"; unresolved]**. Inferable from other fields: Desert_Heart, Well_Guarded, Jabbas_Palace_site, battleground_site, non_Tatooine_location, unique+alien+hasSpecies (Rep), non_unique+alien+species(Rep), operative, frozenCaptive+Han, Jabba, Bib
- **NamedCardRefs:** Desert Heart → Tatooine: Desert Heart 112_20, 225_30 (V); Well Guarded → 6_150, 221_2 (V); Jabba's Palace site → broad; Han/Jabba/Bib/Wounded Wookiee/No Bargain/Bad Feeling Have I/Alter referenced in modifier conditions only, NOT pulled
- **LocationRequirements:** Desert Heart = 112_20 (base), 225_30 (V) exact; Jabba's Palace site ~7 dark by title e.g. 112_12, 6_162, 6_165; battleground_site occupancy (flip needs 2 or 3 occupied); non_Tatooine_location gates harder 3-site flip + forfeit+2
- **CharacterRequirements:** Rep = unique alien with a Species (chosen from Reserve at deploy, stored); 3rd-site flip helper + BACK retrieval = non_unique alien of Rep's exact species; MayNotDeploy operatives; refs frozen captive Han, Jabba, Bib (modifier triggers only)
- **Pull chain:** On deploy: choose Rep (unique alien w/ species) from Reserve. Pulls of Desert Heart / Well Guarded implied by named refs (source pullChain field self-referential). **[UNCERTAIN — exact on-deploy pull sequence not spelled out in record]**
- **Flip:** FRONT→BACK if you occupy 2 battleground sites; BUT if a non-Tatooine location on table, need 3 battleground sites AND one occupied with a non-unique alien of Rep's species
- **Flip-back:** BACK→FRONT if you do NOT occupy 2 battleground sites
- **Hard-lose:** None
- **Rando V-tags:** Only ObjectiveHandler (DEAD) lines 104-109 map 112_15 → [112_20, "title:Jabba's Palace"]. ObjectiveAnalyzer has NO 112_15 entry. No live V-tag. CardSelectionEvaluator:1729 only mentions "Tatooine: Desert Heart" in a V88/V136 tie-break comment, unrelated.
- **Gaps:** **[FLAG]** filtersUsedInJava = "placeholder" (not enumerated). Jabbas_Palace_site exact membership not enumerated (~7 dark/3 light title-prefix **[APPROX]**). Rep species dynamic → species-filtered counts unknowable. 601_010 virtual reprint not diffed. Pull chain not fully specified. **[UNCERTAIN]**

---

## 7. `201_39` — Imperial Entanglements / No One To Stop Us This Time
- **CardRef:** ids `201_39`, `201_39_BACK` — "Imperial Entanglements" / "No One To Stop Us This Time"
- **Java:** set201/dark/Card201_039.java + Card201_039_BACK.java. [Virtual Set 1], A New Hope icon. Front deploys 0, back 7.
- **Filters-used-in-Java:** Tatooine_system, Devastator, Tatooine_site, battleground, system, Imperial, Imperial_starship, Admirals_Order, Tatooine_Occupation, Tatooine_location, trooper
- **NamedCardRefs:** Tatooine system → 1_289, 12_175, 203_33 (D); LS 1_127, 12_84 (filter matches any); Devastator → 1_301, 216_8; Tatooine Occupation (block target, not deployed) → resolve by title
- **LocationRequirements:** Tatooine system 3 dark candidates (1_289, 12_175, 203_33; +LS 1_127, 12_84); Tatooine battleground site = Tatooine_site+battleground, ~27 dark "Tatooine:" sites / ~35 light, battleground subset smaller, samples 1_290, 1_291, + Mos Eisley (Jabba's Palace sites excluded); any planet system blocked from deploy rest of game
- **CharacterRequirements:** only Imperial characters (non-Imperial blocked); only Imperial starships; back retrieve trooper (Keyword); no named-character pull
- **Pull chain:** On deploy (required, in order): Tatooine system → Devastator onto it → Tatooine battleground site. Recurring once/turn [download] Tatooine battleground site (both sides). Back: retrieve trooper (draw phase) + Reserve peek (control phase).
- **Flip:** control 3 Tatooine sites AND opp controls <3
- **Flip-back:** opp controls more Tatooine sites than you
- **Hard-lose:** None
- **Rando V-tags:** None specific. Only generic Tatooine heuristics (V54 LMFBM, V73 shuttle); nothing references this objective.
- **Gaps:** Exact Tatooine-battleground-site count not enumerated (~27 dark "Tatooine:" upper bound) **[APPROX]**. Note Tatooine_site+battleground (front/download) vs Tatooine_location (back peek counts occupied).

---

## 8. `208_57` — I Want That Map / And Now You'll Give It To Me
- **CardRef:** ids `208_57` — "I Want That Map" (front) / "And Now You'll Give It To Me" (back)
- **Java:** set208/dark/Card208_057.java + Card208_057_BACK.java
- **Filters-used-in-Java:** FRONT deploy pulls: Tuanul_Village; EPISODE_VII+location+not(Tuanul_Village); I_Will_Finish_What_You_Started. FRONT Resistance-Agent reveal: or(unique+hasAbility+character+except(Luke or Jedi), mayBeRevealedAsResistanceAgent); ongoing lost: Dark_Jedi+not(EPISODE_VII); Luke; sameTitle(revealedResistanceAgent); Resistance_Agent+Set_For_Stun; CancelImmunityToAttrition on Luke; flip gate: controlsWith 2× battleground by First_Order_character, NOT canSpot(Resistance_Agent+presentAt(battleground_site)). BACK: isDuringBattleWithParticipant(your+First_Order_leader); force-retrieval cancel unless BB8_or_has_BB8_as_permanent_astromech; Kylo; I_Will_Finish_What_You_Started; hasStacked(Interrupt/playable); flip-back: NOT occupies 2× battleground OR canSpot(Resistance_Agent+presentAt(battleground_site))
- **NamedCardRefs:** Tuanul Village → Jakku: Tuanul Village 204_53 (D), 204_31 (L); I Will Finish What You Started → 208_40 (D); The First Order Was Just The Beginning → 214_12 (D) [rando-named starting effect, NOT in objective Java]; Starkiller Base system → 208_51 (D) [rando-named EpVII pick, NOT hardcoded]
- **LocationRequirements:** deploy 1 = Tuanul Village (204_53); deploy 2 = any other EpVII location ~40-50 candidates, samples 208_51, 208_52, 208_53; flip gate control 2 battlegrounds with First Order chars + Resistance Agent NOT present at battleground_site
- **CharacterRequirements:** First_Order_character (control 2 bg for flip); First_Order_leader (back BD cost); Kylo persona (back interrupt engine); Resistance Agent = opp's chosen unique+hasAbility+character except Luke/Jedi OR mayBeRevealedAsResistanceAgent, else Luke (loses attrition immunity); BB-8 on opp stops force-retrieval cancel; non-EpVII Dark Jedi made lost
- **Pull chain:** On deploy (required, in order): Tuanul Village → other EpVII location → I Will Finish What You Started. Then optional opp reveal of Resistance Agent (else Luke). Ongoing: non-EpVII Dark Jedi lost; Resistance Agents immune to Set For Stun. Back (Kylo present): stack Interrupts from Lost Pile onto I Will Finish What You Started, once/turn play one then place OOP.
- **Flip:** FRONT→BACK when First Order chars control 2 battlegrounds AND no Resistance Agent present at a battleground site
- **Flip-back:** BACK→FRONT when you don't occupy 2 battlegrounds OR a Resistance Agent IS present at a battleground site
- **Hard-lose:** None (back places played stacked Interrupts OOP — card disposal, not loss)
- **Rando V-tags:** V186 (consolidated 2026-07-07) only. ObjectiveAnalyzer (~924, isWantThatMap): title "i want that map" → addLocationFragment("starkiller base"); marks "the first order was just the beginning" required+pullable. CardSelectionEvaluator V186: Starkiller Base SYSTEM (208_51) +400 as EpVII pick (~802/817); prefers 214_12 as starting effect (~8228). ObjectiveHandler DEAD. No rando handling of Resistance-Agent reveal, Kylo engine, or flip gating beyond location/effect steering.
- **Gaps:** Objective Java refs named cards by Filter, never title fragment. Starkiller Base (208_51) + The First Order Was Just The Beginning (214_12) are rando strategy picks, not hardcoded. EpVII location count **[APPROX]**.

---

## 9. `211_26` — A Stunning Move / A Valuable Hostage
- **CardRef:** ids `211_26`, `211_26_BACK` — "A Stunning Move" / "A Valuable Hostage". DARK, Virtual Set 11, icons THEED_PALACE + EPISODE_I.
- **Java:** set211/dark/Card211_026.java + Card211_026_BACK.java
- **Filters-used-in-Java:** _500_Republica, Insidious_Prisoner, Private_Platform, or(Sidious, First_Order_character, Imperial), Grievous, siteOfStarshipOrVehicle(INVISIBLE_HAND, true), SEPARATIST+droid, Invisible_Hand_site, opponents(owner)+or(Jedi, starship, vehicle), immunityToAttritionMoreThan(5), your+character+SEPARATIST at hasAttached(Insidious_Prisoner)
- **NamedCardRefs:** Insidious Prisoner → 211_11 (EPIC_EVENT, DARK); 500 Republica → 211_17 (Coruscant); Private Platform → 211_18 (Coruscant Docking Bay); note 216_9 Mustafar: Private Platform is a DIFFERENT persona; Grievous → persona; Sidious → persona; Invisible Hand → Persona.INVISIBLE_HAND (objective targets its sites)
- **LocationRequirements:** 500 Republica exact (211_17) unique site; Private Platform (211_18 Coruscant; 216_9 Mustafar separate persona); Invisible Hand site = siteOfStarshipOrVehicle(INVISIBLE_HAND) small set (interior sites), downloaded from Reserve
- **CharacterRequirements:** Grievous (+2 immunity to attrition); Separatist character holding Insidious Prisoner (back Force loss); download target = SEPARATIST+droid non-unique; deploy ban (not requirements): Sidious, First_Order_character, Imperial
- **Pull chain:** On deploy (required, in order): 500 Republica → Insidious Prisoner (to 500 Republica) → Private Platform. Recurring per Deploy phase: Invisible Hand site OR non-unique Separatist droid. Flip is a state trigger (prisoner location), no search.
- **Flip:** FRONT→BACK when Insidious Prisoner isAttachedTo an Invisible Hand site
- **Flip-back:** BACK→FRONT when Insidious Prisoner NOT at Invisible Hand site AND insidiousPrisoner != null
- **Hard-lose:** None (both files explicitly comment no place-OOP)
- **Rando V-tags:** NONE. No rando code references this objective / Insidious Prisoner / 500 Republica. Grep hits for grievous/invisible hand/stunning leader are unrelated (Stunning Leader interrupt V37.2, Grievous bounce-hand guard, Invisible Hand in starship list).
- **Gaps:** **[FLAG]** _500_Republica / Private_Platform resolve by persona/title; confirm Private_Platform filter is persona-scoped (Mustafar 216_9 shares name fragment) before using downstream. Invisible Hand site count not enumerated (small set). **[UNCERTAIN]**

---

## 10. `213_31` — Hunt Down And Destroy The Jedi / Their Fire Has Gone Out Of The Universe (V)
- **CardRef:** ids `213_31`, `213_31_BACK` (base non-V = 7_297 / 7_297_BACK) — "Hunt Down And Destroy The Jedi" / "Their Fire Has Gone Out Of The Universe"
- **Java:** set213/dark/Card213_031.java + Card213_031_BACK.java
- **Filters-used-in-Java:** title(Vaders_Castle), Visage_Of_The_Emperor+icon(VIRTUAL_SET_13), CLOUD_CITY+site+iconCount(DARK_FORCE,1), battleground_site+or(partOfSystem(Malachor), icon(CLOUD_CITY)), character+not(or(droid, Imperial, bounty_hunter)), inquisitor, sameLocationAs(inquisitor), hasStacked(hatredCard), Vader, at(battleground_site), or(Jedi, padawan, Luke), opponents, site+controlsWith(Vader), Visage_Of_The_Emperor (back NoForceLoss), Vader+armedWith(lightsaber) (back)
- **NamedCardRefs:** Vader's Castle → 209_50 (Mustafar: Vader's Castle, only DB match for Title.Vaders_Castle); Visage Of The Emperor [Set 13] → 213_16 (base non-V 4_135); Vader → persona (targeted, not pulled)
- **LocationRequirements:** Vader's Castle 209_50; [Set 13] Visage 213_16; [Cloud City]-icon site with exactly one [DS] icon ~40 CC-titled sites narrowed to single-DS interior, samples 5_166, 5_167, 5_168; download target = battleground site Malachor system OR [Cloud City] icon; Malachor sites (4): 213_28, 213_29, 217_15, 219_36
- **CharacterRequirements:** deploy restriction only droid/Imperial/bounty_hunter; inquisitor → destiny +1, BD +1 (+2 with Hatred) where present; Vader + or(Jedi, padawan, Luke) drive flip; hasStacked(hatredCard) enables +2
- **Pull chain:** On deploy (required, in order): Vader's Castle → [Set 13] Visage Of The Emperor → a [Cloud City] site w/ exactly one [DS] icon. Ongoing top-level (once/deploy phase): download battleground site = Malachor system OR [Cloud City]. Once/game after front flips: take Vader into hand from a site you control. Back once/game: take Vader + cards on him into hand.
- **Flip:** FRONT→BACK when Vader at a battleground_site AND NOT (opp Jedi/Padawan/Luke at a battleground_site)
- **Flip-back:** BACK→FRONT when Vader NOT on table OR opp Jedi/Padawan/Luke at a battleground_site
- **Hard-lose:** None in this objective's code. (Maul card 6683 externally protects this objective from place-OOP during a Maul-initiated duel — a protection, not a lose clause.)
- **Rando V-tags:** Substantial. BattleEvaluator: V29.9 HUNT DOWN (armed Vader FIGHT, aggro); V35 VADER EXPENDABILITY + V35 INQUISITOR BATTLE DESTINY (+150 / +250 with Hatred) + Vader-vs-Luke +100. V27/V27.1 Draw Their Fire interrupt-tax (name-collision only, different card). ActionEvaluator: isInquisitor() helper. ActionTextEvaluator ~2028/2063 Hunt Down replay notes on IAYF mis-blocked weapon pull. Header BattleEvaluator.java:18 lists V29.9.
- **Gaps:** **[UNCERTAIN]** Vader's Castle via Title.Vaders_Castle; only DB match 209_50 (Title constant not independently verified vs DB title). [Cloud City] one-DS-icon site count **[APPROX]** (~40 CC total, single-DS subset not enumerated). No hard-lose in objective code.

---

## 11. `213_32` — Shadow Collective / You Know Who I Answer To
- **CardRef:** ids `213_32`, `213_32_BACK` — "Shadow Collective" / "You Know Who I Answer To"
- **Java:** set213/dark/Card213_032.java + Card213_032_BACK.java
- **Filters-used-in-Java:** title(Dathomir_Mauls_Chambers), title(Massassi_Throne_Room), Maul, or(non_unique+blaster, titleContains("First Light")), MayNotDeploy your+cardsThatMayNotDeploy, cardsThatMayNotDeploy = or(EPISODE_I+droid, hasAbilityOrHasPermanentPilotWithAbility+not(or(independentStarships, episode1BountyHunters, assassin, gangster, loreCharacters))), independentStarships = INDEPENDENT+starship, episode1BountyHunters = EPISODE_I+bounty_hunter, loreCharacters = or(loreContains("Crimson Dawn"/"Black Sun"/"Hutt")), occupies(3, battleground), controlsWith(2, battleground, gangster), justHitBy(character, your), BACK: your+leader+gangster+at(site), your+non_unique+blaster, Maul+alone
- **NamedCardRefs:** Dathomir: Maul's Chambers → 213_23 (required pull + deploy target for Maul); Yavin 4: Massassi Throne Room → 1_138 (spot gate); [Set 13] Maul → 213_10 (code uses Filters.Maul persona = any Maul)
- **LocationRequirements:** front flip (opp -1): occupies 3 battlegrounds; front auto-flip: gangsters control 2 battlegrounds during your battle phase; back: leader gangster at a site; battleground keyword ~300+ ids, samples 1_138, 213_23. No broad deploy-location pull; only specific named locations pulled.
- **CharacterRequirements:** Maul persona (deploy to Maul's Chambers if Massassi Throne Room spotted); gangster (control 2 bg to flip; leader in battle for back destiny); leader (back, gangster leader w/ non-unique blaster); assassin / EpI bounty hunter / Black Sun/Crimson Dawn/Hutt lore chars exempt from MayNotDeploy; your character "hit" by your card triggers front flip
- **Pull chain:** On deploy (required): Dathomir: Maul's Chambers (213_23). If Massassi Throne Room (1_138) on table, optional deploy a Maul to Maul's Chambers. Once/turn (both sides): deploy non-unique blaster OR "First Light"-title card from Reserve; reshuffle. Back flip: recirculate + reshuffle Reserve.
- **Flip:** FRONT→BACK when EITHER (a) you just "hit" a character with your card, OR (b) during your battle phase your gangsters control 2 battlegrounds. If flips while occupying 3 battlegrounds, opp loses 1 Force.
- **Flip-back:** BACK→FRONT at end of each turn. If about to flip while occupying 3 battlegrounds, opp loses 1 Force.
- **Hard-lose:** None
- **Rando V-tags:** V22 (CardSelectionEvaluator ~8218-8225, ~8768-8774): "Shadow Collective payoff" — Rando prefers these starting effects at +500 (both deal-pick and reserve-pick). CardSelectionEvaluator ~79: "first light" in preferred capital-starship title list. ObjectiveHandler 208-210: 213_32 → 213_23 (DEAD code).
- **Gaps:** No dedicated ObjectiveAnalyzer/strategy handling for flip-race or once/turn blaster download. Filters.Maul persona-wide (any Maul qualifies though text specifies [Set 13] 213_10). Battleground count not enumerated (~hundreds) **[APPROX]**.

---

## 12. `216_11` — On The Verge Of Greatness / Taking Control Of The Weapon
- **CardRef:** ids `216_11` — "On The Verge Of Greatness" (front, deploy 0) / "Taking Control Of The Weapon" (back, deploy 7). Krennic/Scarif Death Star Imperial.
- **Java:** set216/dark/Card216_011.java + Card216_011_BACK.java
- **Filters-used-in-Java:** FRONT: VIRTUAL_SET_16+Death_Star_system; Scarif_system; title(Scarif_Citadel_Tower); Shield_Gate; OPT Scarif_location+battleground; MayNotDeploy or(Endor_Shield, character+not(Imperial), starship+not(Imperial_starship)); DeployCost your+non_unique+or(Imperial, vehicle, capital_starship) w/ CardMatchesEvaluator(-1,-2, Star_Destroyer); Forfeit+1 non_unique+Imperial; FLIP spot at(Scarif_battleground_site)+or(Krennic, Tarkin) AND Death_Star_system isOrbiting(Scarif). BACK: A_Bright_Center_To_The_Universe; BD loc or(Death_Star_system, isOrbitedBy(Death_Star_system)); Vader movableAsRegularMove; retrieve non_unique+hasAbilityOrHasPermanentPilotWithAbility; flip-back(neg) your+leader+at(Scarif_battleground_site); place-oop !Death_Star_system || !Shield_Gate
- **NamedCardRefs:** Death Star [Set 16] system → 216_7; Scarif system → 216_13 (D), 209_23 (L); Scarif: Citadel Tower → 216_15; Shield Gate → 216_18; Endor Shield (may-not-deploy) → filter, id not resolved (restriction); A Bright Center To The Universe (back ModifyGameText target)
- **LocationRequirements:** Scarif battleground site (flip gate): DARK Beach/Command Center/Landing Pad Five/Citadel Tower; LIGHT Beach/Data Vault/Landing Pad Nine/Turbolift Complex; ~8 Scarif site ids (216_x dark / 209_x light); OPT once/turn deploy any Scarif_location+battleground; BACK BD bonus = Death Star system (216_7/2_143) + orbited system + related sites
- **CharacterRequirements:** flip = Krennic OR Tarkin at a Scarif battleground site; flip-back = NO your leader at a Scarif battleground site; back Vader move; deploy restriction only Imperial chars / Imperial starships
- **Pull chain:** Required on-deploy (in order): Set-16 Death Star system 216_7 → Scarif system → Scarif: Citadel Tower 216_15 → Shield Gate 216_18. Once/turn either side: download a Scarif battleground. Back draw phase: retrieve non-unique card with ability.
- **Flip:** Krennic or Tarkin at a Scarif battleground site AND Death Star orbiting Scarif
- **Flip-back:** back→front if you do NOT have a leader at a Scarif battleground site
- **Hard-lose:** Back places OOP if Death Star system OR Shield Gate not on table
- **Rando V-tags:** V79 (MoveEvaluator ~469-550, Steve 2026-05-15): "VERGE OF GREATNESS — MOVE DEATH STAR TOWARD SCARIF". Rando-as-Krennic shepherds Set-16 DS parsec 4→7, takes orbit-Scarif move (+1500), post-flip/orbiting never re-initiates DS hyperspeed. Uses engine isOrbiting title check (matches flip). DrawEvaluator ~579-605 has COMMENTED-OUT (dead) V79 detection block. No strategy/ObjectiveAnalyzer logic.
- **Gaps:** DS [Set 16] system id confirmed 216_7. Endor Shield left as filter (restriction). Full Scarif battleground id list not dumped (~8) **[APPROX]**. Krennic/Tarkin/Vader are persona Filters.

---

## 13. `219_1` — A Great Tactician Creates Plans / The Result Is Often Resentment (V)
- **CardRef:** ids `219_1` — "A Great Tactician Creates Plans" / "The Result Is Often Resentment". Set 19 [V], Thrawn objective.
- **Java:** set219/dark/Card219_001.java + Card219_001_BACK.java
- **Filters-used-in-Java:** Lothal_system, title(Lothal_Advanced_Projects_Laboratory), title(Lothal_Imperial_Complex), Thrawns_Art_Collection, Chiraneau, or(EPISODE_I, EPISODE_VII), hasAbilityOrHasPermanentPilotWithAbility/PRESENCE, Imperial_starship+Star_Destroyer, not(Lothal_location)/Lothal_location, Chimaera, battleground_system, site+deployableToSystem(Lothal), partOfSystem(Lothal), Thrawn+at(battleground), Thrawn, Imperial_leader, TIE_Defender (piloted/hasPermanentPilot/hasAboard character), hasStacked(any)
- **NamedCardRefs:** Lothal system → 219_10; Lothal: Advanced Projects Laboratory → 219_11; Lothal: Imperial Complex → 219_13; Thrawn's Art Collection → 219_20; Chiraneau → 9_97 (base only found); Chimaera → 219_2 (Set19 V) and 9_154 (base); Thrawn → persona (ids 10_40, 207_21, 207_30)
- **LocationRequirements:** fixed named pulls on flip-up: Lothal system 219_10, Advanced Projects Laboratory 219_11, Imperial Complex 219_13; once/turn download battleground_system (~40-60, e.g. 219_10, 1_286, 3_106) OR a site deployableToSystem Lothal; "artwork" = cards stacked on Thrawn's Art Collection (219_20), not a location filter
- **CharacterRequirements:** Thrawn at a battleground = flip-up gate; Thrawn not on table = flip-back; back "study artwork" trigger enabled by Imperial_leader OR piloted/permanent-pilot/crewed TIE_Defender; MayNotPlay Chiraneau + your EpI/EpVII cards with ability or [Presence]
- **Pull chain:** Flip-up (getGameTextWhenDeployedAction) fires 4 REQUIRED pulls in order: Lothal system (219_10) → Advanced Projects Laboratory (219_11) → Imperial Complex (219_13) → Thrawn's Art Collection (219_20). Thereafter once/turn top-level download of battleground system or Lothal-deployable site.
- **Flip:** FRONT→BACK during either player's DEPLOY phase when can spot Thrawn at a battleground AND Thrawn's Art Collection has ≥2 stacked cards
- **Flip-back:** BACK→FRONT two triggers: (1) Thrawn's Art Collection has 0 stacked cards (not during battle), or (2) cannot spot Thrawn
- **Hard-lose:** None (back puts studied artwork in owner's Lost Pile — not a loss)
- **Rando V-tags:** NONE. No live logic. ObjectiveHandler (DEAD) maps only 219_48 (Zero Hour), not 219_1. MoveEvaluator hit was unrelated comment ("Thrawn's 4" ability). No V-tag.
- **Gaps:** **[UNCERTAIN]** Filters.Thrawn = Grand Admiral Thrawn persona; DB ids 10_40, 207_21, 207_30 (verify no other variants). Chiraneau confirmed 9_97 base only (no virtual found in quick grep). battleground_system count **[APPROX]**.

---

## 14. `222_14` — The Shield Will Be Down In Moments / Imperial Troops Have Entered The Base!
- **CardRef:** ids `222_14` — "The Shield Will Be Down In Moments" / "Imperial Troops Have Entered The Base!". DARK, HOTH + VIRTUAL_SET_22.
- **Java:** set222/dark/Card222_014.java + Card222_014_BACK.java
- **Filters-used-in-Java:** FRONT: Fifth_Marker; VIRTUAL_SET_17+Fourth_Marker; First_Marker; VIRTUAL_SET_9+Prepare_For_A_Surface_Attack; MayNotBeFired your+AT_AT_Cannon [CONTROL]; MayNotDeploy or(Rebel_Base_Occupation, Sunsdown, Dark_Jedi+not(Vader)); CancelsGameText title("Echo Base Sensors"); LimitForceLoss=1 You_May_Start_Your_Landing; once/turn Hoth_location; flip isBlownAwayLastStep title(Main_Power_Generators). BACK: LostInterrupt or(title("Rebel Leadership"), Were_Doomed); Attrition InBattleCondition(Imperial_leader)+OnTableEvaluator(participatingInBattle, Imperial_leader); deploy snowtrooper; retrieve bottomOfLostPile; peek X = Hoth_location+controls; place-oop Hoth_site+occupiesWith(or(AT_AT+piloted, Imperial_leader, snowtrooper))
- **NamedCardRefs:** 5th Marker (Ice Plains) → 3_148, 208_49 (D); [Set 17] 4th Marker (North Ridge) → 217_12 (D; base 3_149); 1st Marker (Main Power Generators) → 222_9 (D [Set 22]); Prepare For A Surface Attack [Set 9] → 209_42 (base 13_82); Echo Base Sensors → 13_17 (L, canceled); You May Start Your Landing → 11_77 (D)
- **LocationRequirements:** Hoth location (once/turn download) all Hoth systems + sites ~40+, samples 3_148, 217_12, 222_9; Hoth site (place-oop occupation) ~25+, samples 222_9, 3_149; markers = specific named Hoth locations (1st-7th)
- **CharacterRequirements:** Imperial leader (attrition bonus + occupation-satisfier); snowtrooper (deployable + occupation-satisfier); piloted AT-AT (occupation-satisfier); Dark Jedi except Vader (deploy lockout)
- **Pull chain:** FRONT on deploy (required, 4 pulls): 5th Marker, [Set 17] 4th Marker, 1st Marker, [Set 9] Prepare For A Surface Attack. Ongoing once/turn: download any Hoth location. BACK once/turn: deploy snowtrooper from Reserve (reshuffle); control-phase retrieve bottom Lost Pile.
- **Flip:** FRONT→BACK when Main Power Generators blown away
- **Flip-back:** None. Instead back placed OOP.
- **Hard-lose:** BACK placed OOP when you cannot spot a Hoth site you occupy with a piloted AT-AT / Imperial leader / snowtrooper. Front has none.
- **Rando V-tags:** V160 (Steve 2026-05-29) in ObjectiveAnalyzer (~112, 712, 887-909): detects "shield will be down", sets isShieldWillBeDown, adds "target the main generator" to required+pullable ("target the main generator", "at-at cannon", "prepare for a surface attack"), addLocationFragment "hoth: defensive perimeter"/"hoth: ice plains"/"hoth: main power generators". Recognizes flip path (Target The Main Generator → AT-AT Cannon fire → MPG blown away). ObjectiveHandler 219-225 maps 222_14 (+AI variant 222_30) to [3_148, 3_149, 222_9, 13_82] — DEAD code. No logic for BACK-side occupation or place-OOP.
- **Gaps:** **[FLAG]** V160 covers FRONT flip path only; no guard for BACK place-OOP (must keep piloted AT-AT / Imperial leader / snowtrooper at a Hoth site). Marker ids verified 3_148, 3_149, 222_9 + virtual 208_49/217_12; full Hoth-location enumeration not done **[APPROX]**. Prepare For A Surface Attack has base 13_82 + [Set 9] 209_42; objective pulls the [Set 9] copy.

---

## 15. `222_30` — The Shield Will Be Down In Moments / Imperial Troops Have Entered The Base! (AI variant)
- **CardRef:** ids `222_30` (AI variant) and `222_14` (non-AI variant, same set) — same titles as row 14
- **Java:** set222/dark/Card222_030.java + Card222_030_BACK.java
- **Filters-used-in-Java:** FRONT: Fifth_Marker; VIRTUAL_SET_17+Fourth_Marker; First_Marker; VIRTUAL_SET_9+Prepare_For_A_Surface_Attack; your+AT_AT_Cannon [MayNotBeFired, CONTROL]; MayNotDeploy or(Rebel_Base_Occupation, Sunsdown, Dark_Jedi+not(Vader)); CancelsGameText title("Echo Base Sensors"); Hoth_location [once/turn]; You_May_Start_Your_Landing [LimitForceLoss 1]; title(Main_Power_Generators) [flip, blownAway]. BACK: or(title("Rebel Leadership"), Were_Doomed) [LostInterrupt]; Imperial_leader [Attrition + participatingInBattle]; snowtrooper [once/turn deploy]; Hoth_location+controls [countTopLocationsOnTable for X]; bottomOfLostPile [retrieve]; place-oop Hoth_site+occupiesWith(or(AT_AT+piloted, Imperial_leader, snowtrooper))
- **NamedCardRefs:** 5th Marker (Ice Plains) → 208_49 (D); [Set 17] 4th Marker (North Ridge) → 217_12 (D); 1st Marker (Main Power Generators) → 222_9 (D; base 3_148/3_149 exist); Prepare For A Surface Attack → 13_82 (base), 209_42 (Set 9 V) — objective pulls 209_42; Main Power Generators (flip) → 222_9; Echo Base Sensors (title-canceled, L); You May Start Your Landing (cap 1); Rebel Leadership / We're Doomed → made Lost Interrupts (opp cards)
- **LocationRequirements:** Hoth location (once/turn download) ~15-25 blueprints, samples 208_49, 217_12, 222_9; Hoth SITE (hard-lose occupy + X counter); marker sites pulled on deploy = 5th (208_49), [Set17] 4th (217_12), 1st (222_9)
- **CharacterRequirements:** Imperial leader (BACK attrition +1 each in battle + hard-lose occupier); snowtrooper (once/turn Reserve deploy + occupier); AT-AT piloted (occupier); Dark Jedi except Vader (deploy lockout); AT-AT Cannon (can't fire control phase)
- **Pull chain:** On deploy (required, 4 in order): 5th Marker (208_49) → [Set17] 4th Marker (217_12) → 1st Marker (222_9) → [Set9] Prepare For A Surface Attack (209_42). Install persistent modifiers. Ongoing FRONT once/turn download any Hoth location. BACK once/turn deploy snowtrooper (reshuffle).
- **Flip:** FRONT→BACK when Main Power Generators (1st Marker) blown away
- **Flip-back:** None (one-way)
- **Hard-lose:** BACK placed OOP whenever table changes and you do NOT occupy any Hoth site with piloted AT-AT / Imperial leader / snowtrooper
- **Rando V-tags:** V160 (Steve 2026-05-29). ObjectiveAnalyzer (~887-909): detects "shield will be down"; adds required/pullable "target the main generator", "at-at cannon", "prepare for a surface attack"; addLocationFragment "hoth: defensive perimeter"/"hoth: ice plains"/"hoth: main power generators". ActionTextEvaluator (~921-934): V160 pushes Target The Main Generator action (+800). ObjectiveHandler references it (DEAD).
- **Gaps:** **[FLAG]** V160 does NOT model: the 4 on-deploy required pulls, once/turn Hoth download, BACK snowtrooper Reserve deploy, or hard-lose occupy condition; no place-OOP awareness; no attrition-per-Imperial-leader modeling. Pulled Prepare For A Surface Attack is specifically [Set 9] 209_42; V160 pullable list uses bare title (may match Reflections III 13_82 too). **[UNCERTAIN]**

---

## 16. `225_32` — The First Order Reigns / The Resistance Is Doomed
- **CardRef:** ids `225_32` — "The First Order Reigns" / "The Resistance Is Doomed". DARK, Episode VII, Virtual Set 25.
- **Java:** set225/dark/Card225_032.java + Card225_032_BACK.java
- **Filters-used-in-Java:** Dqar_system, Crait_system, Crait_Salt_Plateau, Tracked_Fleet, your, hasAbilityOrHasPermanentPilotWithAbility, not(EPISODE_VII), Bow_To_The_First_Order, titleContains("Supremacy"), EPISODE_VII+battleground, system, Supremacy, EPISODE_VII+system, non_unique, trooper, FIRST_ORDER, vehicle, Han, Leia, Luke, Kylo, wherePresent, Crait_location, First_Order_character, characterOrPermanentPilotAlone, sameLocationAs, occupiesWith, opponents
- **NamedCardRefs:** D'Qar system → 211_19; Crait system → 225_15; Crait: Salt Plateau → 225_17; Tracked Fleet → 225_34; Bow To The First Order → 204_47; Supremacy → 225_27 (Bridge 225_28, Throne Room 225_29 also match titleContains); Kylo persona → 204_43, 209_37, 222_10 etc.; Han → 102_2, 200_14…; Leia → 102_3, 200_18…; Luke → 101_2…
- **LocationRequirements:** 4 specific deploy targets: D'Qar (211_19), Crait (225_15), Crait: Salt Plateau (225_17), Tracked Fleet (225_34); once/turn download "Supremacy"-title OR [Episode VII] battleground (~6 dark EpVII systems: 204_51, 208_51, 211_19 + Crait/Ahch-To/Batuu, plus EpVII bg sites); Supremacy deploy-cost reset applies at EPISODE_VII+system (~6 dark)
- **CharacterRequirements:** Kylo persona (occupy a Crait location for drain +1; control Salt Plateau; being forfeited at Salt Plateau triggers hard-lose); two First_Order_character at a battleground (+1 drain); opp Han/Leia/Luke present at Salt Plateau enables place-OOP; downloadable from Lost Pile: non-unique trooper or non-unique [First Order] vehicle
- **Pull chain:** On deploy (required): D'Qar, Crait, Crait: Salt Plateau, Tracked Fleet. After deploy: may-not-deploy lock on Bow To The First Order + all your cards-with-ability except EpVII. Each turn (both): download "Supremacy" title or EpVII battleground. Back adds once/turn deploy non-unique trooper or non-unique [First Order] vehicle from Lost Pile.
- **Flip:** FRONT→BACK when Tracked Fleet annihilated (blown away)
- **Flip-back:** None. Leaves BACK only via hard-lose.
- **Hard-lose:** BACK placed OOP if Kylo just forfeited to Lost Pile from Crait: Salt Plateau during a battle you lost there while opp's Han, Leia, or Luke present
- **Rando V-tags:** No dedicated V-tag/ObjectiveAnalyzer logic. ObjectiveHandler (DEAD) maps "225_32" → [225_15, 211_19, 225_28, 225_34]. **[FLAG]** that list uses 225_28 (Supremacy: Bridge) and OMITS Salt Plateau 225_17, so it does NOT match the actual Java deploy set (225_15, 211_19, 225_17, 225_34). CardSelectionEvaluator "supremacy" (line 67) is incidental capital-ship name list.
- **Gaps:** EpVII battleground count **[APPROX]** (~6 dark systems + bg sites; Filters.battleground engine-computed). Separate reprint objective at set501/dark/Card501_060.java (not the 225_32 target). Persona filters resolve to many ids (samples only). ObjectiveHandler requirement list stale/dead and mismatches real deploy set.

---

## 17. `226_12` — This Deal Is Getting Worse All The Time (V) / Pray I Don't Alter It Any Further (V)
- **CardRef:** ids `226_12`, `226_12_BACK` — "This Deal Is Getting Worse All The Time" / "Pray I Don't Alter It Any Further" (virtual TDIGWATT)
- **Java:** set226/dark/Card226_012.java + Card226_012_BACK.java
- **Filters-used-in-Java:** FRONT deploy: Cloud_City_battleground_site; CLOUD_CITY+Im_Sorry. FRONT+BACK MayNotDeploy or(Admirals_Order, DEATH_STAR_II+Executor); your+Lando+movableAsRegularMove; upload or(Dark_Deal, Vaders_Bounty, SPECIAL_EDITION+Bespin_system); flip controls(player,3,Bespin_location) && !controls(opp,3,Bespin_location). BACK: AtCondition(Vader, Bespin_location); MayNotPlay or(Sense, Alter); ForceDrainBonusesMayNotBeCanceled sameSiteAs your or(Lando, Lobot); CancelsGameText Admirals_Order when Vader at Bespin; TotalBattleDestiny +2 your alien with your Imperial in battle; NumTimesPerBattle destiny +/-1 gated on your Lando in battle (x2 if Lobot in battle); flip countTopLocations Bespin_location+controls(opp) > controls(player)
- **NamedCardRefs:** I'm Sorry [Cloud City] → 11_72, 226_6 (effect requires CLOUD_CITY, so 226_6 is the [CC] copy); Dark Deal → 5_115, 223_9 (V); Vader's Bounty → 5_125; Bespin system [SE] → 5_164, 223_8 (SPECIAL_EDITION → 5_164); Admiral's Order → filter class (subtype); Executor [DS II] → DEATH_STAR_II+Executor
- **LocationRequirements:** Cloud City battleground site (LOCATION+site+[CC] icon+battleground) subset of 32 "Cloud City:" sites, samples 226_1, 5_166, 221_11; Bespin_location (any location on Bespin) ~34 candidates, samples 5_164/5_76, 5_165/5_77
- **CharacterRequirements:** Lando persona (once/turn regular move + back destiny trigger); Lobot persona (doubles destiny-mod uses; force-drain-bonus protection); Vader at a Bespin location cancels Admiral's Orders (back); alien+Imperial pair in same battle (+2 total BD)
- **Pull chain:** Deploy phase (required trigger): Cloud City battleground site + [Cloud City] I'm Sorry from Reserve. Once/turn upload: Dark Deal | Vader's Bounty | [SE] Bespin system into hand.
- **Flip:** FRONT→BACK when you control 3 Bespin locations AND opp controls fewer than 3
- **Flip-back:** BACK→FRONT when opp controls more Bespin locations than you
- **Hard-lose:** None (only flip/flip-back)
- **Rando V-tags:** V22 (DeployPhasePlanner ~827/1295-1352): objectiveWantsBespin → plan capital ship to Bespin system, +200. V22.5 (ObjectiveAnalyzer.needsBespinSystemPresence ~1355): Bespin/CC objectives want a ship at Bespin system to enable Dark Deal/CC Occupation. V29 UPDATED 2026-07-06 (ObjectiveAnalyzer ~1364-1370, TDIGWATT bug B): reads objective's OWN text "may not deploy … Executor" → forbids Executor; notes classic 109_12 lacks this clause. ObjectiveAnalyzer L1073-1284 auto-links Cloud City ↔ Bespin fragments. (ObjectiveHandler DEAD.)
- **Gaps:** Location counts **[APPROX]** (title-prefix "Cloud City:" scan = 32 both sides incl virtuals, not filtered to battleground-only). Bespin_location resolved conceptually, not enumerated. Dark Deal/Bespin/I'm Sorry each have base+V ids; Java uses Filters/Icons so any matching copy qualifies.

---

## 18. `301_4` — Twin Suns Of Tatooine / Well Trained In The Jedi Arts
- **CardRef:** ids `301_4` — "Twin Suns Of Tatooine" (front, deploy 0) / "Well Trained In The Jedi Arts" (back, deploy 7). DARK, set301/DEMO_DECK, Rarity V.
- **Java:** set301/dark/Card301_004.java + Card301_004_BACK.java
- **Filters-used-in-Java:** FRONT deploy: Tatooine_system; Tatooine_site+not(Jabbas_Palace_site); MayNotDeploy or(Jabbas_Palace_site, Sandwhirl); once/turn download Tatooine_site+battleground; flip Tatooine_battleground_site (controls 2), Dark_Jedi (controlsWith), Tatooine_system (occupies), Tatooine_site (opp controls none). BACK download Tatooine_Occupation; move-cost mod opponents+or(character, vehicle) sameSiteAs your Dark_Jedi; flip Tatooine_site+controls(opp/player)
- **NamedCardRefs:** Tatooine Occupation → 221_32, 7_244 (BACK once-per-game download); Sandwhirl → named in may-not-deploy (front); Jabba's Palace sites → excluded (front deploy + may-not-deploy)
- **LocationRequirements:** Tatooine system (~5 ids): dark 12_175, 1_289, 203_33; light 12_84, 1_127; non-Jabba's-Palace Tatooine site ~50+ ids, samples 11_43, 12_85, 106_8; Tatooine battleground site (OPT download + flip-control) ~20-30 candidates, samples 11_43, 12_85
- **CharacterRequirements:** Dark Jedi (flip requires one of two controlled Tatooine battleground sites to hold a Dark Jedi; BACK move-cost bonus keys off your Dark Jedi at a site). Detect by Keyword/subtype, NOT title.
- **Pull chain:** On deploy (required): download Tatooine system + one non-Jabba's-Palace Tatooine site. FRONT: once/turn use 1 Force to download a Tatooine battleground site. BACK: once/game download Tatooine Occupation (221_32/7_244); once/control phase peek top 2 of Reserve, take 1, reshuffle.
- **Flip:** FRONT→BACK when you control two Tatooine battleground sites (at least one WITH a Dark Jedi), occupy Tatooine system, AND opp controls no Tatooine site
- **Flip-back:** BACK→FRONT if opp controls MORE Tatooine sites than you
- **Hard-lose:** None
- **Rando V-tags:** Single DEAD-code ref only. ObjectiveHandler entry uses title-fragment ("title:Tatooine:") not the Filter-based requirements; system ref hardcoded to light 1_127 only (real deploy is Filters.Tatooine_system, any side). No live ObjectiveAnalyzer/evaluator support.
- **Gaps:** **[FLAG]** ObjectiveHandler dead + title-fragment mismatch + hardcoded light 1_127. No live support. Battleground-Tatooine-site count not enumerated (~20-30) **[APPROX]**.

---

## Cross-cutting flags for Codex/K-2

**Filters not enumerated in source (unresolved — do NOT treat as complete):**
- `110_6` filtersUsedInJava = "see array" (placeholder)
- `112_15` filtersUsedInJava = "placeholder"; also its exact on-deploy pull sequence is not spelled out
Both need a re-scan of the actual Java before Codex relies on their filter lists.

**Dead-code / stale ObjectiveHandler entries (live brain = ObjectiveAnalyzer):**
- `14_113` — lists Blockade Flagship id as `14_111`; real is `14_114`
- `110_6`, `112_15`, `213_32`, `219_1` (maps 219_48 not 219_1), `225_32`, `301_4` — all DEAD; `225_32` requirement list actively MISMATCHES the real Java deploy set (has 225_28, omits 225_17); `301_4` uses title-fragment + hardcoded light system id

**Objectives with NO live Rando handling at all:** `13_73`, `111_6`, `112_15`, `201_39`, `211_26`, `219_1`, `225_32`, `301_4`

**Hard-lose / place-OOP present (matters for Rando risk-modeling):** `111_6` (Death Star blown away → OOP), `216_11` (Death Star system OR Shield Gate gone → OOP), `222_14` + `222_30` (lose Hoth-site occupation → OOP), `225_32` (Kylo forfeited at Salt Plateau w/ opp hero present → OOP). Of these, only V160 touches 222_14/222_30 and it does NOT guard the place-OOP condition.

**One-way flips (no flip-back):** `111_6`, `222_14`, `222_30`, `225_32`.

**Persona/title-filter id caveats to verify downstream:** `211_26` (Private_Platform — Coruscant 211_18 vs Mustafar 216_9 share name fragment; confirm persona-scoped), `213_31` (Vaders_Castle Title constant vs DB title 209_50), `219_1` (Thrawn persona ids 10_40/207_21/207_30; Chiraneau 9_97 base-only in quick grep).

**Approximate counts (never enumerated per-blueprint):** battleground/Hoth/Tatooine/Cloud City/Scarif/Bespin/EpVII location counts across rows 1, 3, 4, 7, 10, 12, 13, 14, 15, 16, 17, 18. All marked **[APPROX]** inline.

**Base vs virtual pairs sharing a title (Codex should not merge):** `109_12` (base) vs `226_12` (V) — different upload sets, 226 adds Executor ban; `222_14` vs `222_30` (AI variant, same set); `225_32` vs `501_060` reprint; `112_15` vs `601_010` reprint; `213_31` (V) base non-V `7_297`.