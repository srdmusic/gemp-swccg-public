# Bottom-up objective intel (inventory rows 29-39) — DRAFT, verify against source

Generated 2026-07-07 by K-2 fast agents. INTEL to help Codex + cross-check. Verify every id/title against source before wiring. Candidate counts are DB-limited (JSON DB is missing set601/Legacy). Note surfaced: dead ObjectiveHandler.java carries a WRONG Bespin id (5_164) — real light Bespin = 5_76.

---

# Bottom-up objective intel (rows 29-39)

Eleven objectives, bottom-up from card blueprints. Per section: CardRef, Filters-used-in-Java, NamedCardRefs, LocationRequirements, CharacterRequirements, pull chain, flip / flip-back / hard-lose, existing Rando V-tags, gaps. All id/count claims are DB-limited (card_blueprint_database JSON omits set601/Legacy) and flagged where noted.

---

## 301_2 — City In The Clouds / You Truly Belong Here With Us (LIGHT)

- **CardRef:** 301_2 (City In The Clouds), 301_2_BACK (You Truly Belong Here With Us)
- **Java:** `set301/light/Card301_002.java` (back: `Card301_002_BACK.java`)
- **Filters used in Java:** Bespin_system, Cloud_City_site, battleground, Weather_Vane, Cloud_City_location, Cloud_City_battleground_site, Cloud_City_Celebration, Interrupt, character, your, controls; Icon.CLOUD_CITY, Icon.VIRTUAL_SET_P
- **NamedCardRefs:**
	- Bespin system → 5_76 (Bespin, light). NOTE: dead ObjectiveHandler mapping cites 5_164, which is NOT the light Bespin id — stale/wrong.
	- Weather Vane → light 219_47, 5_30 (base + virtual); dark 5_127
	- Cloud City Celebration → 7_55 (light; back-side OPG download)
- **LocationRequirements:** Cloud City battleground SITES = LOCATION, subtype site, Bespin/Cloud City system, both force icons. ~15 "Cloud City:" sites in light DB (battleground subset smaller). Samples: 5_78 (Carbonite Chamber), 224_13 (Lower Corridor), 225_40 (Beldon's Corridor). Bespin system (5_76) must be occupied for the flip. DB-limited (no set601/Legacy).
- **CharacterRequirements:** Front: none. Back react trigger: any "your character" (Filters.character + Filters.your), no affiliation/species/keyword restriction — detection by CardCategory.character only.
- **Pull chain:** Front deploy (required): pull Bespin system, then a Cloud City battleground site. Optional: pull Weather Vane (0-1). Front OPT (once/turn, 1 Force): download a Cloud City battleground from Reserve. Back: OPG download Cloud City Celebration; once/control-phase pay 2 Force to upload an Interrupt to hand from Reserve.
- **Flip:** Front→Back: control 2 Cloud City battleground sites AND occupy Bespin system AND opponent controls no Cloud City sites.
- **Flip-back:** Back→Front: opponent controls MORE Cloud City sites than you (Card301_002_BACK L147-150).
- **Hard-lose:** None on either side.
- **Existing Rando V-tags:** No dedicated V-tag. ObjectiveHandler.java (DEAD code; live brain = ObjectiveAnalyzer) maps "301_2" → [5_164 Bespin, "title:Cloud City"] L71-75 (5_164 disagrees with real light Bespin 5_76). ObjectiveAnalyzer has only generic "Cloud City battleground site" text detection (~L783), no 301_2 branch. ActionTextEvaluator has generic Bespin/AMSD gating (V24, V24.15, ~L1590-1780) and Weather Vane pull-parser notes (V177, ~L360-373), none keyed to this objective.
- **Gaps:** Bespin id ambiguity (real light 5_76 vs dead-code 5_164; 5_164 not found in light DB, may be dark/stale). Weather Vane in both light + dark DBs; deploy is by Filters.Weather_Vane so all matching ids qualify. Battleground-site count DB-limited.

---

## 7_296 — Carbon Chamber Testing / My Favorite Decoration (DARK)

- **CardRef:** 7_296 (Carbon Chamber Testing), 7_296_BACK (My Favorite Decoration). Both AbstractObjective, Side.DARK, ExpansionSet.SPECIAL_EDITION, Rarity.R. Front cost 0; back generates 7.
- **Java:** `set7/dark/Card7_296.java` (front); `set7/dark/Card7_296_BACK.java` (back)
- **Filters used in Java:** Carbonite_Chamber, Carbonite_Chamber_Console, Security_Tower, Jabbas_Prize, Rebel, Han, Leia, Luke, imprisonedIn(Security_Tower), Audience_Chamber, Docking_Bay_94, East_Platform, Dark_Deal, escorting(frozenCaptive), frozenCaptive, Scum_And_Villainy, alien, starship + Icon.INDEPENDENT, at(Audience_Chamber); Title.You_Can_Either_Profit_By_This, Title.Rescue_The_Princess, Title.There_Is_Good_In_Him, Title.Alter
- **NamedCardRefs:**
	- Carbonite Chamber → 5_166 (dark); light 5_78, 225_41
	- Carbonite Chamber Console → 5_107, 211_8 (dark)
	- Security Tower → 5_172, 200_126 (dark)
	- Jabba's Prize → 10_42 (dark)
	- Audience Chamber → 6_162 (dark); light 6_81
	- Docking Bay 94 → 1_291 (dark); light 1_129
	- East Platform → 5_169 (dark)
	- Scum And Villainy → 6_149 (dark)
	- Dark Deal → 5_115, 223_9 (dark) [may-not-play]
- **LocationRequirements:** Carbonite Chamber, Carbonite Chamber Console, Security Tower (Cloud City sites), Audience Chamber (Jabba's Palace), Docking Bay 94 (Tatooine), East Platform (Cloud City Docking Bay) — each by its named Filter.
- **CharacterRequirements:** Imprisoned captive = Filters.Rebel (Rebel affiliation icon), opponent chooses from their Reserve. Conditional persona exclusions by opponent's objective: exclude Han if opp plays You Can Either Profit By This; Leia if Rescue The Princess; Luke if There Is Good In Him. Jabba's Prize (10_42) may substitute if in Rando's own Reserve. Back immunity applies to Filters.alien + Icon.INDEPENDENT starships.
- **Pull chain:** On-deploy (required): pull Carbonite Chamber, Carbonite Chamber Console, Security Tower from own Reserve; then imprison a Rebel from OPPONENT's Reserve into Security Tower (opp choice) OR deploy Jabba's Prize from own Reserve as imprisoned captive if present. Front repeatable: 1/deploy phase pull Audience Chamber OR Docking Bay 94 OR East Platform (reshuffle). Back repeatable: 1/deploy phase pull Scum And Villainy while a frozen captive is at Audience Chamber (reshuffle).
- **Flip:** Front flips when Rando moves a frozen captive to Audience Chamber (movedToLocationBy escorting(frozenCaptive) → Audience_Chamber). Alt immediate flip: if no imprisonedIn(Security_Tower) captive spottable after deploy (no Rebel was in opp Reserve at game start), set WhileInPlayData flag and flip.
- **Flip-back:** None. Single flip front→back. Back is placed out of play if no frozen captives remain on table (unless the no-Rebel-at-start flag is set).
- **Hard-lose:** None (field empty in record).
- **Existing Rando V-tags:** No 7_296-specific brain; handled via generic ObjectiveAnalyzer text parsing (Cloud City↔Bespin auto-link, flip-fragment parsing). Touches: ObjectiveHandler.java L157-161 lists 7_296 start reqs 5_166/5_107/5_172 (DEAD code). ActionTextEvaluator V24.6 (I'm Sorry: pull Cloud City interior sites incl. Carbonite Chamber/Security Tower, +250). CardSelectionEvaluator V24.13 (Carbonite Chamber priority battleground +150; Security Tower force-gen deploy LAST -30) ~L8489-8494, 8821-8826, plus carbonite/security-tower substring checks L3119/3174/7804. MoveEvaluator L662-663 (carbonite/security-tower move targeting; platform substring commented out V47 2026-07-06). DeployEvaluator L4557 (Carbonite Chamber/Dining Room Lando comment). CardSelectionEvaluator V88 text-scan (~L1720-1745) bonuses Audience Chamber / Scum And Villainy but anchored to Jabba The Hutt / Court Of The Vile Gangster, not this objective.
- **Gaps:** Location filters are persona/title filters matching by name regardless of side, so light ids (5_78/225_41, 6_81, 1_129) also match; front deploys from own dark Reserve so dark ids are practical targets. No dedicated 7_296 evaluator/strategy V-tag; handling emergent from generic parsing + I'm Sorry V24.x heuristics. Rebel-imprison mechanic (pull from OPPONENT Reserve) has no specific Rando awareness found. DB-limited ids.

---

## 7_297 — Hunt Down And Destroy The Jedi / Their Fire Has Gone Out Of The Universe (DARK)

- **CardRef:** 7_297, 7_297_BACK; virtual 213_31, 213_31_BACK (Hunt Down And Destroy The Jedi / Their Fire Has Gone Out Of The Universe)
- **Java:** `set7/dark/Card7_297.java`
- **Filters used in Java:** Holotheatre, Visage_Of_The_Emperor, Meditation_Chamber, Epic_Duel, Vader, Sense, Alter, Scanning_Crew, Maul, Executor_site, Jedi, Luke, Skywalker, battleground_site, controls, opponents
- **NamedCardRefs:**
	- Holotheatre → 4_161 (Executor: Holotheatre, dark). Only DB match, no [V].
	- Visage Of The Emperor → 4_135, 213_16 (dark base + virtual)
	- Meditation Chamber → 4_163 (Executor: Meditation Chamber, dark)
	- Epic Duel → 5_129 (dark)
	- Scanning Crew → 1_266 (dark) [place-out-of-play trigger]
	- Vader → persona filter Filters.Vader (many ids, not enumerated)
	- Executor site → Filters.Executor_site (site-subtype filter, e.g. 4_161/4_163)
- **LocationRequirements:** Executor: Holotheatre (4_161) required deploy; Visage Of The Emperor (4_135/213_16) required deploy (Executor site); Executor: Meditation Chamber (4_163) optional. Filters.Executor_site = any Executor site (hard-lose if you initiate non-Epic duel OR Force drain there). Filters.battleground_site: Vader must be at one (front flip); opponent Jedi/Luke at one (flip gate).
- **CharacterRequirements:** Vader — persona (Filters.Vader). Opponent Jedi — Keyword.JEDI (Filters.Jedi). Opponent Luke — persona (Filters.Luke). Opponent Skywalker — Filters.Skywalker (BACK battle/drain restriction).
- **Pull chain:** On deploy: REQUIRED deploy Holotheatre, then REQUIRED Visage Of The Emperor; then OPTIONAL up to 1 Meditation Chamber and OPTIONAL up to 1 Epic Duel. All via DeployCardFromReserveDeckEffect / DeployCardsFromReserveDeckEffect (reshuffle=true).
- **Flip:** Front→Back: Vader at a battleground site AND no opponent Jedi/Luke at a battleground site (+ isTableChanged + canBeFlipped).
- **Flip-back:** Back→Front: table changed AND (opponent has Luke or a Jedi at a battleground site OR Vader not on table).
- **Hard-lose:** Place out of play (both sides) if: you play Scanning Crew (before-trigger); you initiate a non-Epic duel (after-trigger; Maul-duel exception when HUNT_DOWN__DO_NOT_PLACE_OUT_OF_PLAY_IF_MAUL_DUELS active); or you Force drain at an Executor site. "Place out of play" = removed permanently.
- **Existing Rando V-tags:** V25 (ObjectiveAnalyzer: detects Hunt Down via Vader-at-battleground flip text; sets isHuntDownV, huntDownNeedsVader, huntDownFlipBackNoVader ~L107-110, 879-884, 1169-1172). V29.12 (MoveEvaluator ~L1280: armed Vader must leave Castle and hunt). V29.13 (MoveEvaluator ~L1564: group/move characters WITH Vader). MoveEvaluator ~L2482-2508: bonus for moving toward Luke. No dedicated deploy/pull evaluator beyond generic objective-deploy logic.
- **Gaps:** Vader persona ids not enumerated (many); Holotheatre only one DB id (4_161), no [V]. Rando has strategy/move awareness (V25/V29.12/V29.13) but NO dedicated deploy-side evaluator forcing the Holotheatre/Visage pull or guarding the Executor-site Force-drain hard-lose. DB-limited.

---

## 7_298 — Imperial Occupation / Imperial Control (DARK)

- **CardRef:** 7_298 (Imperial Occupation), 7_298_BACK (Imperial Control). Dark, Special Edition.
- **Java:** `set7/dark/Card7_298.java` (back: `Card7_298_BACK.java`)
- **Filters used in Java:** planet_system (CardSubtype.SYSTEM + Icon.PLANET), and(generic, site), matchingOperativeToRenegadePlanet (Keyword.OPERATIVE + blueprint.getMatchingSystem()==RenegadePlanet), battleground_site, Renegade_planet_location, Rebel, sameSiteAs, any
- **NamedCardRefs:** None (Renegade planet is chosen dynamically at deploy).
- **LocationRequirements:** Renegade planet = any planet SYSTEM (CardSubtype.SYSTEM + Icon.PLANET) from Reserve; ~163 planet-system/PLANET-icon entries in dark DB (DB-limited). A ◇ (generic) site deployed to that system. Flip/hold gauge counts BATTLEGROUND SITES related to the Renegade planet (battleground_site AND Renegade_planet_location). Sample battleground sites (Tatooine): 1_291 (Docking Bay), 7_231 (Jabba's Palace). Counts vary by chosen planet.
- **CharacterRequirements:** Matching operative = Keyword.OPERATIVE whose blueprint.getMatchingSystem() equals the Renegade planet system name (detection is KEYWORD OPERATIVE + matching-system field, NOT title/lore). Rebel detection (power +2 clause) = Filters.Rebel.
- **Pull chain:** On deploy (required): DeployCardFromReserveDeckEffect planet_system → sets systemName as Renegade planet → DeployCardToSystemFromReserveDeckEffect (generic site) to that system. Front top-level (1/deploy phase): DeployCardToSystemFromReserveDeckEffect generic ◇ site to Renegade planet, reshuffle.
- **Flip:** Front→Control: controlsWith(3, battleground_site AND Renegade_planet_location, your + matchingOperativeToRenegadePlanet).
- **Flip-back:** Control→Occupation: required trigger when table changes and you do NOT occupy 2+ battleground sites related to the Renegade planet.
- **Hard-lose:** None.
- **Existing Rando V-tags:** NONE. Grep of rando/evaluators/*.java and rando/strategy/*.java for "Renegade", "Imperial Occupation", "Imperial Control", "operative", 7_298 → zero matches.
- **Gaps:** "Renegade planet" is dynamic (chosen at deploy), so location/battleground reqs depend on chosen system; counts are per-planet approximations. DB omits set601/Legacy → operative and battleground-site inventories undercounted. matchingSystem field on operatives (not title) determines "matching" — verify per operative blueprint.

---

## 7_299 — ISB Operations / Empire's Sinister Agents (DARK)

- **CardRef:** 7_299 (ISB Operations), 7_299_BACK (Empire's Sinister Agents). Dark, Special Edition.
- **Java:** `set7/dark/Card7_299.java` (front); `Card7_299_BACK.java` (back)
- **Filters used in Java:** Coruscant_location (front deploy), your(self), character, loreContains(ISB/Rebel/Rebels/Rebellion), Keyword.ISB_AGENT (granted), Keyword.SPY (granted), ISB_agent (flip/retrieve/drain), Rebel_Base_location (flip via controlsWith 2), battleground_site (back drain), sameLocationAs, not(undercover_spy), sameOrRelatedLocationAs; Icon.SPECIAL_EDITION
- **NamedCardRefs:** ISB Operations → 7_299 (front), 7_299_BACK (back). No virtual [V] reprints found in DB.
- **LocationRequirements:** Coruscant_location — deploy any one from Reserve on deploy (~22 Coruscant LOCATIONs across dark+light by CORUSCANT icon/title; samples 12_165 Coruscant, 12_166 Coruscant: Docking Bay, 12_167 Coruscant: Galactic Senate; DB-limited lower bound). Rebel_Base_location (flip gate: control ≥2 with ISB agents) = partOfSystem(Yavin 4) OR partOfSystem(Hoth). battleground_site (back side: non-Undercover ISB agent there gives drain bonuses).
- **CharacterRequirements:** ISB agents = YOUR CHARACTERs whose LORE contains ISB, Rebel, Rebels, or Rebellion (Filters.loreContains, OR'd — a LORE scan, NOT keyword/title). Such characters granted Keyword.ISB_AGENT + Keyword.SPY + IgnoresLocationDeploymentRestrictionsInGameText at runtime. Flip + back-side effects key off Filters.ISB_agent (granted keyword): lore-driven up front, keyword-driven thereafter. Back drain bonus additionally requires non-Undercover: not(undercover_spy).
- **Pull chain:** On deploy: DeployCardFromReserveDeckEffect(Coruscant_location) — any one Coruscant location (reshuffle). Back side: RetrieveCardEffect(ISB_agent) once per draw phase (retrieve from Lost Pile, NOT a Reserve pull).
- **Flip:** Front→Back: canBeFlipped AND (canSpot 4 ISB_agent on table [INCLUDE_EXCLUDED_FROM_BATTLE] OR controlsWith 2 Rebel_Base_location using ISB_agent).
- **Flip-back:** Back→Front: canBeFlipped AND NOT canSpot any ISB_agent on table.
- **Hard-lose:** None on either side.
- **Existing Rando V-tags:** None. No evaluator/strategy code references ISB Operations, Empire's Sinister Agents, ISB_agent, or this flip/drain logic. Grep hits (isBehindOnLifeForce, isBattleground, DeployPhasePlanner Coruscant-restriction parser L1887) are generic/unrelated.
- **Gaps:** Coruscant_location and battleground_site runtime/DB-limited; counts approximate. Rebel_Base_location is precisely Yavin 4 + Hoth (partOfSystem). ISB-agent detection is LORE-based — cannot be enumerated by keyword/title; resolve by lore scan at runtime. DB misses set601/Legacy.

---

## 7_300 — Ralltiir Operations / In The Hands Of The Empire (DARK)

- **CardRef:** 7_300 (Ralltiir Operations), 7_300_BACK (In The Hands Of The Empire)
- **Java:** `set7/dark/Card7_300.java` (front) + `Card7_300_BACK.java` (back)
- **Filters used in Java:** Ralltiir_system, site, non_unique, Imperial, Rebel, Ralltiir_location, Ralltiir_site, CardSubtype.SYSTEM, title(Title.Ralltiir), any
- **NamedCardRefs:** Ralltiir (system) → 2_65 (light, A New Hope), 2_148 (dark, A New Hope), 220_3 (dark, Ralltiir [V], Set 20)
- **LocationRequirements:** Ralltiir system (Filters.Ralltiir_system / SYSTEM + title Ralltiir) — deployed on-deploy. Ralltiir sites (Filters.Ralltiir_site) — need Imperials controlling ≥3 to flip. Ralltiir locations (Filters.Ralltiir_location) — system + sites; battle-destiny bonus = # occupied by Imperials.
- **CharacterRequirements:** Imperial = ICON detection (Filters.Imperial); non-unique Imperial is a deploy target from Reserve. Rebel = ICON detection (Filters.Rebel); opponent Rebels are deploy +2 at Ralltiir locations (a modifier vs opponent, not a Rando deploy req).
- **Pull chain:** On-deploy (required): deploy Ralltiir system from Reserve. Front top-level (1/deploy phase): deploy from Reserve to Ralltiir one site OR non-unique Imperial (or(site, and(non_unique, Imperial)), Title.Ralltiir); reshuffle. Back top-level (1/control phase): pay 2 Force, take ANY one card into hand from Reserve (reshuffle).
- **Flip:** Front→Back: table changed AND canBeFlipped AND Imperials control ≥3 Ralltiir sites (controlsWith 3, Ralltiir_site, INCLUDE_EXCLUDED_FROM_BATTLE, Imperial) AND opponent controls NO Ralltiir locations.
- **Flip-back:** Back→Front: table changed AND canBeFlipped AND opponent controls ≥2 Ralltiir locations.
- **Hard-lose:** Both sides: place out of play if Ralltiir is "blown away" (isBlownAwayLastStep on SYSTEM + title Ralltiir → PlaceCardOutOfPlayFromTableEffect).
- **Existing Rando V-tags:** None (only a comment example in ObjectiveType.java).
- **Gaps:** Ralltiir system ids confirmed: 2_65 (L), 2_148 (D), 220_3 (D, [V]). Ralltiir-site/location and non-unique-Imperial pull-pool counts DB-limited. No Rando handling — playable only via generic objective logic, no dedicated V-tag.

---

## 8_167 — Endor Operations / Imperial Outpost (DARK)

- **CardRef:** 8_167 (Endor Operations), 8_167_BACK (Imperial Outpost). ExpansionSet.ENDOR. Front deploy 0; back deploy 7.
- **Java:** `set8/dark/Card8_167.java` (front) + `Card8_167_BACK.java` (back)
- **Filters used in Java:** Endor_system, Bunker, Landing_Platform, Ominous_Rumors, Establish_Secret_Base, Endor_location, Endor_site, biker_scout, piloted, AT_ST, sameSiteAs, your
- **NamedCardRefs:**
	- Endor system → 8_157 (dark); light 204_24, 8_68
	- Bunker → 8_160 (dark Endor: Bunker); light 8_70, 204_25
	- Landing Platform → 8_166 (dark Endor: Landing Platform (Docking Bay)); light 8_76
	- Ominous Rumors → 8_127 (base), 223_19 (V) [both DARK]
	- Establish Secret Base → 8_124 (base), 207_25 (V) [both DARK]
- **LocationRequirements:** Deploys Endor system, an Endor: Bunker site (Filters.Bunker), an Endor: Landing Platform (Filters.Landing_Platform). Back benefits scoped to Endor sites (Filters.Endor_site) with a friendly biker scout or piloted AT-ST. Place-out-of-play trigger keyed to any Endor location (Filters.Endor_location). Endor-location counts DB-limited.
- **CharacterRequirements:** biker scout → Keyword.BIKER_SCOUT (Filters.biker_scout) — role type; back drain protection + retrieve target. AT-ST → Filters.AT_ST (vehicle, must be piloted for drain protection; immune to attrition <3). piloted → Filters.piloted state, not a character type.
- **Pull chain:** On-deploy required from Reserve: (1) Endor system, (2) Bunker, (3) Landing Platform. Repeatable control-phase pull: take one Ominous Rumors OR Establish Secret Base into hand (reshuffle). Back draw phase: retrieve one biker scout (RetrieveCardEffect, not a Reserve pull).
- **Flip:** Front→Back: both Ominous Rumors AND Establish Secret Base on table.
- **Flip-back:** Back→Front: when Ominous Rumors and Establish Secret Base are NOT both on table (either missing).
- **Hard-lose:** Not a lose condition. PlaceCardOutOfPlayFromTableEffect on self when an Endor location is "blown away" (isBlownAwayLastStep, Filters.Endor_location) — same trigger both sides.
- **Existing Rando V-tags:** V193 (Steve 2026-07-07) is the ONLY tag, across 3 files: (a) ObjectiveAnalyzer.java ~L63-70, L932-961 — title-keyed "endor operations" block: makes all Endor sites objective-relevant (addLocationFragment "endor"), requiredCardsOnTable=[ominous rumors, establish secret base], flipCriticalControlSite="endor: bunker", flipCriticalControlCard="establish secret base" (fixes two generic-parser bugs). (b) DeployEvaluator.java ~L1900-1919 — BUNKER-CONTROL BONUS: steer a body onto Endor: Bunker so cost-0 Establish Secret Base (V) 207_25 "Deploy on Bunker if you control that site" becomes legal, enabling the flip. (c) DeckOracle.java ~L1163-1226 — ANCHORED parse + quantifier-strip fix so "take one Ominous Rumors/Establish Secret Base" resolves as pull targets. No biker-scout/AT-ST back-side strategy logic.
- **Gaps:** V193 handles only the FRONT flip-gate. No logic exploits the BACK side (Imperial Outpost): no steering toward biker scouts / piloted AT-STs at Endor sites for drain protection, no valuing the biker-scout retrieve, no awareness of flip-back risk when Ominous Rumors or Establish Secret Base leaves the table. DB-limited.

---

## 9_151 — Bring Him Before Me / Take Your Father's Place (DARK)

- **CardRef:** 9_151 (Bring Him Before Me), 9_151_BACK (Take Your Father's Place)
- **Java:** `set9/dark/Card9_151.java` (front); `set9/dark/Card9_151_BACK.java` (back)
- **Filters used in Java:** Throne_Room, Insignificant_Rebellion, Your_Destiny, Scanning_Crew, Emperor, Luke, Leia, Kanan, Vader, Objective, captive, frozenCaptive, canBeTargetedBy, presentWith, presentAt, escorting, escortedBy, canEscortCaptive, aboard, open_vehicle, grantedMayBeTargetedBy, isLeavingTable, opponents, title(Title.We_Need_Luke_Skywalker); Icon.DEATH_STAR_II; Persona.SIDIOUS, Persona.LUKE, Persona.LEIA, Persona.KANAN
- **NamedCardRefs:**
	- Throne Room → 9_147 (Death Star II: Throne Room, DARK) [Filters.Throne_Room pull]
	- Insignificant Rebellion → 9_127, 210_47
	- Your Destiny → 9_134
	- Emperor → Filters.Emperor / Persona.SIDIOUS (broad, not enumerated) — Rando pull deploy -2 from Reserve
	- Luke → Filters.Luke / Persona.LUKE (broad; opponent deploy -2 from Reserve or Lost Pile)
	- Self: 9_151 / 9_151_BACK
- **LocationRequirements:** Death Star II: Throne Room (Filters.Throne_Room) — the single named site Vader/Emperor/Luke must be present at for the duel; deployed from Reserve. DB id 9_147. Not a broad battleground-count req.
- **CharacterRequirements:** Vader (Filters.Vader persona) must be present with Luke, not escorting a captive, to capture/seize; present at Throne Room with Emperor+Luke to duel. Emperor (Filters.Emperor / Persona.SIDIOUS) — must be present at Throne Room. Luke (Filters.Luke / Persona.LUKE) — opponent's card, captured then dueled; alt targets Leia (Persona.LEIA) or Kanan (Persona.KANAN) if "targets instead of Luke" mod set. All PERSONA/Filter-based, not title fragments — not enumerated.
- **Pull chain:** On deploy (required, all reshuffle): Throne Room, then Insignificant Rebellion, then Your Destiny. Top-level (Rando's): deploy Emperor (-2) from Reserve, reshuffle. Opponent-card actions: opponent may deploy Luke (-2) from Reserve OR from Lost Pile. Kanan, Rebel Infiltrator via the Luke slot sets BRING_HIM_BEFORE_ME__TARGETS_KANAN_INSTEAD_OF_LUKE for rest of game (Leia variant analogous). Back can re-download Emperor from Reserve.
- **Flip:** Front→Back when Luke (or Leia/Kanan per mod) is captured: an until-end-of-game proxy makes Vader capture+seize Luke when Luke present with Vader, Vader not escorting a captive, Luke targetable (TO_BE_CAPTURED). Flip fires when self canBeFlipped and a captive Luke/Leia/Kanan spotted (SpotOverride.INCLUDE_CAPTIVE_AND_EXCLUDED_FROM_BATTLE). Scanning Crew may-not-play + Luke may-not-be-placed-out-of-play + Vader may-not-transfer-Luke set on deploy.
- **Flip-back:** Back→Front if Luke (or Leia/Kanan) is neither a captive NOR present with Vader. Back also: lose 1 Force at end of each of your turns; once/turn when Vader+Luke(even non-frozen captive)+Emperor all present at Throne Room, initiate Luke/Vader duel (2 destiny each + ability; Vader win = opp -3 Force; Luke win = shuffle Reserve, draw destiny, if crossover total >12 Luke crosses to Dark Side and DEPLETES opponent's Life Force = win).
- **Hard-lose:** None on the objective itself. (Linked card Luke Skywalker, The Emperor's Prize / 205_14 has place-out-of-play text, but that is a separate card.) The back's crossover branch DEPLETES the OPPONENT's Life Force (dark win), not a self-lose.
- **Existing Rando V-tags:** NONE. Grep of rando evaluators/ + strategy/ finds no dedicated logic and no V-tag for 9_151. Only appearance: "Bring Him Before Me" cited as a doc-comment EXAMPLE of ObjectiveType.COMBO (ObjectiveType.java:27) and "Throne Room" example under MAINS_HEAVY. No cardId→COMBO mapping in ObjectiveAnalyzer (grep 9_151 / BRING_HIM / COMBO-with-id = empty). ObjectiveHandler.java is dead code.
- **Gaps:** Zero objective-specific handling: 9_151 not mapped to any ObjectiveType (COMBO reference is only enum javadoc, no id binding), falls through to GENERIC weighting. No logic for the capture-Luke-with-Vader pull chain, flip gate, once/turn Luke/Vader duel, or crossover life-force-depletion win. Emperor/Luke pulls resolve via broad persona filters, not enumerated ids. DB-limited.

---

## 10_29 — Agents Of Black Sun / Vengeance Of The Dark Prince (DARK)

- **CardRef:** 10_29 (Agents Of Black Sun), 10_29_BACK (Vengeance Of The Dark Prince)
- **Java:** `set10/dark/Card10_029.java` (back: `Card10_029_BACK.java`)
- **Filters used in Java:** Imperial_City, Xizor, Coruscant_system, No_Bargain, title("Shada"), alien + loreContains("Black Sun"), bounty_hunter, information_broker, Black_Sun_agent (Keyword.BLACK_SUN_AGENT), Emperor, Icon.INDEPENDENT + starship, Scanning_Crew, hasAbilityOrHasPermanentPilotWithAbility, site / adjacentSite / any_bounty, battleground_site / battleground, Luke (variant Rey / Anakin), occupies / occupiesWith; Icon.LEGACY_BLOCK_4
- **NamedCardRefs:**
	- Prince Xizor → dark 10_45
	- Coruscant: Imperial City → dark 201_38, 7_277
	- Coruscant system → dark 7_275, 12_165, 203_31, 226_2
	- No Bargain → dark 7_233, 225_4 (LEGACY_BLOCK_4 V-variant used)
	- Shada → NOT in card_blueprint_database_dark.json (Legacy/set601; DB omits it); resolved in Java only via Filters.title("Shada") + Icon.LEGACY_BLOCK_4
- **LocationRequirements:** Coruscant: Imperial City (SITE) → dark 201_38, 7_277. Coruscant (SYSTEM) → dark 7_275, 12_165, 203_31, 226_2. Bounty-hunter move ability targets any SITE adjacent to a bounty hunter that has a bounty (Filters.any_bounty). Force drain / flip checks count battleground_site + battleground occupied by Xizor/Emperor (DB-limited).
- **CharacterRequirements:** Prince Xizor (Filters.Xizor persona → dark 10_45) must deploy to Imperial City. Black Sun Agents defined by: aliens with "Black Sun" in LORE (alien + loreContains), bounty hunters (Filters.bounty_hunter), information brokers (Filters.information_broker) — all gain Keyword.BLACK_SUN_AGENT. Emperor (Filters.Emperor) exempt from may-not-deploy lock and counts for Vengeance Force drain. Flip pivots on Luke (or Rey/Anakin variant) at a battleground site.
- **Pull chain:** On deploy, required from Reserve in order: (1) Coruscant: Imperial City; (2) Xizor to Imperial City [LEGACY branch: if Reserve holds both a LEGACY_BLOCK_4 No Bargain (V) and a LEGACY_BLOCK_4 Shada, deploy Xizor-or-Shada to Imperial City, and if Shada chosen also deploy No Bargain (V)]; (3) Coruscant system.
- **Flip:** Front→Back (isTableChanged trigger) when Xizor is at a battleground site AND Luke is NOT at a battleground site. Variant mods retarget Luke→Rey or Anakin; LEGACY__TREAT_XIZOR_AS_SHADA retargets Xizor→Shada.
- **Flip-back:** Back→Front when Luke (or Rey/Anakin variant) is at a battleground site OR Xizor (or Shada) is not on table.
- **Hard-lose:** None. Flip back and forth only, no hard-lose gate.
- **Existing Rando V-tags:** No live rando strategy/evaluator/ObjectiveAnalyzer logic. Only a DEAD-code entry: ObjectiveHandler.java L152 OBJECTIVE_REQUIREMENTS.put("10_29", ["7_277" Imperial City, "200_144" Coruscant]) — ObjectiveHandler is dead code (live brain = ObjectiveAnalyzer, zero mention). No V-tags. CardSelectionEvaluator L122 lists "vengeance of the dark prince" only as a ship-name false-positive exclusion (unrelated). MoveEvaluator/CardSelectionEvaluator "Xizor" hits are Xizor's Palace comments, unrelated.
- **Gaps:** Shada missing from DB (Legacy/set601). No dedicated handling beyond dead ObjectiveHandler map; no ObjectiveAnalyzer branch or strategy playbook. ObjectiveHandler's Coruscant id 200_144 matches no live DB Coruscant-system id (7_275/12_165/203_31/226_2), likely stale. DB-limited.

---

## 12_179 — My Lord, Is That Legal? / I Will Make It Legal (DARK)

- **CardRef:** 12_179 (My Lord, Is That Legal?), 12_179_BACK (I Will Make It Legal). Coruscant expansion.
- **Java:** `set12/dark/Card12_179.java` (front); `Card12_179_BACK.java` (back)
- **Filters used in Java:** Galactic_Senate, location, not(Galactic_Senate), or(Rebel, Imperial), leader, abilityLessThan(4), Counter_Assault, Surprise_Assault, Republic_character, Goo_Nee_Tay, Political_Effect, stackedOn(self,...), creature, character, senator, blockade_agenda, ambition_agenda, at(Galactic_Senate); Icon.EPISODE_I
- **NamedCardRefs:**
	- Coruscant: Galactic Senate → 12_75 (light), 12_167 (dark)
	- Counter Assault → canceled via Filters.Counter_Assault (not pulled)
	- Surprise Assault → canceled via Filters.Surprise_Assault (not pulled)
	- Goo Nee Tay → Filters.Goo_Nee_Tay immunity target (not pulled)
- **LocationRequirements:** Coruscant: Galactic Senate — 12_75 (LIGHT), 12_167 (DARK) — deployed from Reserve. Any other [Episode I] LOCATION (Icon.EPISODE_I, not Galactic Senate) — deployed from Reserve. DB-limited ~86 Episode I locations across light+dark (samples 11_42 Tatooine: City Outskirts, 11_43 Tatooine: Mos Espa, 11_44 Tatooine: Podrace Arena).
- **CharacterRequirements:** Senators at Galactic Senate (flip driver) — Filters.senator = Keyword.SENATOR OR lore contains "senator". Your Republic characters — Filters.Republic_character. Rebel/Imperial LEADERS ability<4 — or(Rebel,Imperial)+leader+abilityLessThan(4) get politics +2. Blockade agenda senator (alt flip) — Filters.blockade_agenda. Ambition agenda senator (back destiny +3) — Filters.ambition_agenda.
- **Pull chain:** On deploy: DeployCardFromReserveDeckEffect ×2 (required) — (1) Galactic_Senate, (2) and(Icon.EPISODE_I, location, not Galactic_Senate). Back upload: TakeCardIntoHandFromReserveDeckEffect Political_Effect (once/turn, reshuffle).
- **Flip:** Front flips when your senators at Galactic Senate ≥ 3, OR ≥ 2 with at least one having a blockade agenda.
- **Flip-back:** Back flips to front when you can NOT spot 2 of your senators at Galactic Senate.
- **Hard-lose:** None.
- **Existing Rando V-tags:** Extensive. ObjectiveAnalyzer.java isMyLord flag (title contains "my lord" OR "make it legal"), ~L90/172. Gated rules: V83 (senator must deploy to Galactic Senate, penalize senator→non-Senate), V88 (BOOST senator→Galactic Senate +1500, flip+weapon-destiny protection), V108 (BOOST deploy senators from hand +500, flip target), V110 (HOLD non-senator until a non-Senate SITE exists). V99 SENATE GUARD (DELIBERATELY ungated — keys on Galactic Senate on table, not the objective: block non-senator→Senate unless defensive reinforcement). isSenatorCard helper = Keyword.SENATOR OR lore "senator". Consumers in DeployEvaluator + CardSelectionEvaluator.
- **Gaps:** ~86 Episode I location count DB-limited. Galactic Senate exists as both LIGHT (12_75) and DARK (12_167); pull is from deploying (dark) player's Reserve so relevant id is 12_167, but both share the title. Counter/Surprise Assault + Goo Nee Tay are cancel/immunity filter targets, not pulled cards.

---

## 12_180 — No Money, No Parts, No Deal! / You're A Slave? (DARK)

- **CardRef:** 12_180 (No Money, No Parts, No Deal!, deploy 0), 12_180_BACK (You're A Slave?, deploy 7). Coruscant, Side DARK.
- **Java:** `set12/dark/Card12_180.java` (back: `Card12_180_BACK.java`, same dir)
- **Filters used in Java:** Wattos_Junkyard, Mos_Espa, Watto, QuiGon, Tatooine_site, at, presentAt, and, opponents, non_unique, alien, deployable, any
- **NamedCardRefs:**
	- Watto's Junkyard → 12_178 (dark: Tatooine: Watto's Junkyard), 12_87 (light) [Filters.Wattos_Junkyard]
	- Mos Espa → 11_93 (dark), 208_56 (dark), 11_43 (light), 221_75 (light) all Tatooine: Mos Espa [excludes Mos Espa Docking Bay 12_177/12_86]
	- Watto → 11_65, 11_66 (dark CHARACTER)
	- Qui-Gon → Filters.QuiGon persona: 11_10/11_11, 12_16/12_17, 13_39, 14_27, 200_22/200_146, 213_40, 216_37, 218_3 (all light CHARACTER; opponent's cards buffed +3 power)
- **LocationRequirements:** Deploy Tatooine: Watto's Junkyard (Filters.Wattos_Junkyard) from Reserve. Deploy Tatooine: Mos Espa (Filters.Mos_Espa) from Reserve; must OCCUPY Mos Espa to flip/hold back. LimitForceLossFromForceDrainModifier on Filters.Tatooine_site caps opponent drain loss at 1 while front up. Sample Tatooine-site ids: 11_93 (Mos Espa), 12_178 (Watto's Junkyard), 12_177 (Mos Espa Docking Bay). DB-limited.
- **CharacterRequirements:** Watto (Filters.Watto → dark 11_65/11_66) must be present at Watto's Junkyard to flip front→back and to keep back up; the 8-Force place-in-Used-Pile ability targets Watto at Watto's Junkyard. Qui-Gon (Filters.QuiGon persona) only receives +3 power while front up — opponent's card, no requirement to control.
- **Pull chain:** On deployment: two required DeployCardFromReserveDeckEffect pulls — Filters.Wattos_Junkyard then Filters.Mos_Espa (reshuffle). After deploy: AddUntilEndOfGame ImmuneToTitleModifier(Watto's Junkyard, Revolution). No further Reserve pulls on front. Back side: DrawCardIntoHandFromReserveDeckEffect only if YOURE_A_SLAVE__DRAW_TOP_CARD... game-text mod present (not native to this card).
- **Flip:** Front→Back: table changed AND canBeFlipped AND Watto present at Watto's Junkyard (SpotOverride INCLUDE_EXCLUDED_FROM_BATTLE) AND you occupy Tatooine: Mos Espa.
- **Flip-back:** Back→Front: table changed AND canBeFlipped AND (Watto NOT present at Watto's Junkyard OR you do NOT occupy Tatooine: Mos Espa).
- **Hard-lose:** None. No place-out-of-play / LoseGame on either side. Worst case on back: if opponent uses 2 Force and you cannot deploy the placed card, you lose 2 Force and that card is lost (LoseCardsFromOffTableSimultaneously) — not a game loss.
- **Existing Rando V-tags:** None. Only reference is a doc comment in strategy/ObjectiveType.java L19 citing "No Money No Parts No Deal" as a DRAIN_FOCUSED example. No V-tags.
- **Gaps:** No card-specific handling (no pull-order gating for the two location deploys, no flip-condition awareness of Watto-at-Junkyard + occupy-Mos-Espa, no use of the drain-limit shield). Classification only, via ObjectiveType.DRAIN_FOCUSED comment. DB-limited.

---

### Cross-cutting notes
- **All id/count claims are DB-limited:** card_blueprint_database JSON omits set601/Legacy cards, so every enumeration is a lower-bound approximation. Explicitly flagged: Shada (10_29) is absent from the dark DB entirely.
- **ObjectiveHandler.java is DEAD code** (project memory): live brain = ObjectiveAnalyzer. Its entries for 301_2 (Bespin 5_164 vs real 5_76), 7_296, 10_29 (Coruscant 200_144 matches no live id) are stale and internally inconsistent.
- **Zero dedicated Rando handling** for: 7_298, 7_299, 7_300, 9_151, 10_29, 12_180 (all fall through to GENERIC weighting or ObjectiveType-comment classification only).
- **Partial handling:** 8_167 (V193, front flip-gate only), 7_297 (V25/V29.12/V29.13, strategy/move only, no deploy-pull enforcement).
- **Extensive handling:** 12_179 (V83/V88/V99/V108/V110 senator suite).
- **`found=false`:** none — all 11 records report `found=true`.