# Objective Extension Buckets

Generated: 2026-07-08 by Codex Alfred

Purpose: replace the per-objective grind with shared loader/evaluator extension buckets. K-2 can build one extension type, then enable every objective in that bucket after boundary math.

Source basis:

| Source | Use |
|---|---|
| `resources/Objective_Playbook_Facts_2026-07-08.json` | Canonical source-derived facts, ids, runtime filters, pull chains, flip and flip-back data |
| `src/gemp-swccg-server/src/main/resources/objective_playbooks.json` | Runtime profile state, especially enabled pilots and empty consumed slots on disabled rows |
| `Handoffs/OBJECTIVE_BOUNDARY_BATCH_01_ROWS00-02_2026-07-08.md` through batch 11 | Boundary notes for rows 00-32 |
| Actual `Card*.java` files | Primary rules source. Spot-check before flipping each objective on |

Exclusions: `8_167` Endor Operations and `12_179` My Lord are already enabled pilots. The 56 rows below remain disabled.

## Scoring Categories

These should be generic scoring categories, with objective-specific values supplied by JSON:

| Category | Generic consumer | JSON data needed |
|---|---|---|
| Setup location | Deploy and card-selection evaluators | `startingLocations[]`, `startingEffects[]`, `startingInterrupts[]`, ids, title fragments, source zone |
| Pull chain | Deploy, action-text, and card-selection evaluators | `pullOrDeployActions[]`, action cadence, source zone, destination, enablesFlip |
| Key actor | Deploy and move evaluators | `keyActors[]`, runtime filter key, target location rule, score weights |
| Flip progress | Deploy and move evaluators | `flipLocationRules[]`, count, relation, comparator, actor filter, location filter |
| Stay flipped | Move and deploy evaluators | `flipBackRules[]`, maintenance count, actor filter, location filter |
| Hard veto | Action-text and pass safety | `hardVetoActions[]`, action text or event filter, objective consequence, weight or absolute block |
| Variant split | Loader | `profileFamily`, `variantKey`, exact front/back ids, side-aware ids, per-variant weights |

## Build Order

| Order | Bucket | Why first |
|---:|---|---|
| 1 | Location/count and actor-at-location | Biggest bucket. One extension can unlock the most objectives after boundary math. |
| 2 | Captive-state | Common but needs runtime captive and escort predicates. |
| 3 | Stacked or attached card count | Clean shared shape, but needs objective/card attachment semantics. |
| 4 | Side-aware variant split | Must not share old TDIGWATT or Shield logic across base and virtual variants. |
| 5 | Hard-veto and complex triggers | High-risk. Do after shared slots exist. |

## Bucket 1: Location/Count And Actor-At-Location

Loader field to add:

| Field | Shape |
|---|---|
| `flipLocationRules[]` | `{ relation, count, comparator, locationFilterKey, actorFilterKey, opponentConstraint, phaseGate, alternatives[] }` |
| `flipBackLocationRules[]` | Same shape, used for stay-flipped and flip-back scoring |
| `locationCountWeights` | Reuses existing V-tag numbers when present; otherwise defaults to conservative setup and movement nudges |

Objectives:

| Row | BP | Objective | Source-derived data to encode | Also needs |
|---:|---|---|---|---|
| 00 | `7_135` | `DBO` Dantooine Base Operations | Control 3 Dantooine sites with Rebels. Flip back if opponent controls 2 Dantooine locations. Starts Dantooine system. Pulls Dantooine site or non-unique Rebel to Dantooine. | Blown-away hardLose for Dantooine system. |
| 02 | `7_137` | `LU` Local Uprising | Matching operatives control 3 Subjugated planet battleground sites. Flip back if occupying fewer than 2. Starts planet system and generic site. Pulls generic site. | Dynamic planet like `IO`, but Light side. |
| 05 | `8_78` | `RST` Rebel Strike Team | Control 3 Endor locations or blow away Bunker. Flip back on opponent move phase unless Bunker is blown away. Starts Endor and Rebel Landing Site. | Blown-away Bunker alternative. |
| 07 | `10_26` | `WYS` Watch Your Step | Occupy 2 battlegrounds with smugglers or complete Kessel Run. Flip back if neither condition remains. Starts Cantina, Docking Bay 94, Tatooine. | Utinni/Kessel Run alternative. |
| 08 | `12_88` | `PMCTTS` Plead My Case To The Senate | Senator or agenda-character count at Galactic Senate. Flip back when present count drops below source threshold. Starts Galactic Senate and another Episode I location. | Existing Senate weights V83/V88/V99 family must stay scoped. |
| 10 | `13_46` | `WLHT` We'll Handle This | Opponent Dark Jedi present at interior Naboo battleground site flips. Absence flips back. | Present-at relation only. |
| 11 | `14_52` | `WHAP` We Have A Plan | Control Theed Palace Throne Room with Amidala or source actor. Opponent control flips back. | Actor filter must not borrow Invasion V86. |
| 12 | `109_4` | `QMC` Quiet Mining Colony | Bespin/Cloud City control-count gate with opponent-control constraints. Back flips if opponent controls Bespin system or enough Cloud City sites. | Variant guard from TDIGWATT Bespin logic. |
| 14 | `111_4` | `MBO` Massassi Base Operations | Control at least 3 Yavin 4 sites while opponent controls fewer than 3. Starts and post-flip Death Star package are source-specific. | Blown-away Yavin 4 hardLose. |
| 15 | `112_1` | `AITC` Agents In The Court | Occupy 2 battleground sites, with Rep/species and Tatooine alternatives. Flip back if occupying fewer than 2. | Dynamic Rep species actor filter. |
| 16 | `203_19` | `DMTA` Diplomatic Mission To Alderaan | Delivered Stolen Data Tapes plus Rebel-control relations, or occupy required Tatooine/Alderaan/battleground set. | Delivered-card state. |
| 17 | `204_32` | `OA` Old Allies | Jakku system or Jakku battleground control/occupy alternatives with Rey/Resistance/Episode VII actors. Flip back if occupying fewer than 2 battlegrounds. | Post-flip prevention actions. |
| 18 | `208_25` | `HITCO` He Is The Chosen One | Luke or Jedi present at battleground site while opponent high-ability character condition is false. Flip back when opponent gate appears or key actor absent. | Luke/Jedi relation gate. |
| 19 | `208_26` | `Y4BO` Yavin 4 Base Operations | Four Rebels on table or control/occupy battleground system relations with unique snub fighter. Flip back if fewer than 2 occupied unless 4 Rebels remain. | Rebel count alternative. |
| 20 | `209_29` | `THNIWRC` They Have No Idea We're Coming | Control 2 Scarif locations, or Scarif conditions with Baze/Chirrut/trooper/spy. Flip back if occupying fewer than 2 Scarif locations. | Keep isolated from dark Scarif `OTVOG`. |
| 22 | `211_36` | `TGMNAL` The Galaxy May Need A Legend | Luke on Ahch-To and Resistance count/occupy alternatives. Starts Saddle and Episode VII battleground. | Luke-on-planet actor filter. |
| 24 | `219_48` | `ZH` Zero Hour | Control 3 Lothal locations or Phoenix Squadron battleground condition. Flip back if opponent controls more Lothal locations. | Phoenix Squadron actor filter. |
| 26 | `222_27` | `TEKWRH` The Empire Knows We're Here | Opponent occupies your Hoth location. Flip back if opponent no longer occupies your Hoth location. | Light Hoth scoring split from dark Shield. |
| 29 | `301_2` | `CITC` City In The Clouds | Control 2 Cloud City battleground sites and occupy Bespin/Cloud City relation. Flip back if opponent controls more Cloud City sites. | Weather Vane and Cloud City Celebration pull chain. |
| 32 | `7_298` | `IO` Imperial Occupation | Matching operatives control 3 Renegade planet battleground sites. Flip back if occupying fewer than 2. | Dynamic Renegade planet and matching-operative filters. |
| 33 | `7_299` | `ISBO` ISB Operations | Four ISB agents on table or ISB agents control 2 Rebel Base locations. Flip back if no ISB agents. | Existing V25/V29.7 ISB logic. |
| 34 | `7_300` | `RO` Ralltiir Operations | Imperials control 3 Ralltiir sites. Flip back if opponent controls 2 Ralltiir sites. | Blown-away Ralltiir hardLose. |
| 37 | `10_29` | `AOBS` Agents Of Black Sun | Xizor or Legacy Shada at battleground while target character is not at battleground. Flip back if target reaches battleground or Xizor absent. | Retargeted Luke/Leia/Kanan/Anakin profile data. |
| 39 | `12_180` | `NMNPND` No Money, No Parts, No Deal! | Watto present at and occupying Watto's Junkyard. Flip back if Watto not present or not occupying. | Simple actor-at-location. |
| 40 | `13_73` | `LTMTFM` Let Them Make The First Move | Your Dark Jedi present with opponent Jedi at interior Naboo battleground. Flip back when opponent Jedi absent. | Dark Jedi relation filter. |
| 41 | `14_113` | `INV` Invasion | Control Throne Room with presence droid plus control Naboo system. Flip back if opponent controls Naboo system or Throne Room. | Existing V86 Neimoidian deploy scoring must be profiled, not left hardcoded. |
| 45 | `112_15` | `MKOS` My Kind Of Scum | Occupy 2 battleground sites with Jabba/Bib/Rep species alternatives. Flip back if occupying fewer than 2. | Dynamic Rep species actor filter. |
| 46 | `201_39` | `IE` Imperial Entanglements | Control 3 Tatooine sites while opponent controls fewer than 3. Flip back if opponent controls more Tatooine sites. | Trooper and Devastator pull chain. |
| 47 | `208_57` | `IWTM` I Want That Map | First Order characters control 2 battlegrounds. Flip back if occupying fewer than 2 or BB-8/key map condition changes. | Existing V186 map and Resistance Agent handling. |
| 57 | `301_4` | `TSOT` Twin Suns Of Tatooine | Control and occupy 2 Tatooine battleground sites with Dark Jedi. Flip back if opponent controls more Tatooine sites. | Tatooine Occupation pull chain. |

## Bucket 2: Captive-State

Loader field to add:

| Field | Shape |
|---|---|
| `captiveRules[]` | `{ captiveFilterKey, captiveNameIds, requiredLocationFilterKey, escortFilterKey, state: captive/frozen/free, sourceZone, setupChoice, flipRelation, flipBackRelation }` |
| `captiveWeights` | setup captive, rescue route, escort route, preserve captive, avoid self-kill |

Objectives:

| Row | BP | Objective | Source-derived data to encode | Also needs |
|---:|---|---|---|---|
| 04 | `7_139` | `RTP` Rescue The Princess | Leia ability < 4 is captive path. Starts Detention Block Corridor, Docking Bay 327, Yavin 4 Docking Bay, War Room. Flip by moving/rescuing Leia to Yavin 4 War Room. Flip back if Leia captured. Hard lose if Leia lost from table. | Leia-specific hardLose. |
| 06 | `9_61` | `TIGIH` There Is Good In Him | Luke captive relation, Imperial escort, Vader crossover destiny. Starts Chief Chirpa's Hut, Landing Platform, I Feel The Conflict. Flip when Luke captive. Flip back if Luke neither present with Vader nor captive. | Crossover hardLose for Vader. |
| 13 | `110_4` | `YCEPBT` You Can Either Profit By This... | Han captive/free state on Tatooine. Flip when Han on Tatooine and not captive. Flip back when no free Han spotted. | Blown-away Tatooine hardLose. |
| 23 | `215_17` | `RTP` Rescue The Princess (V) | A New Hope Leia rescue route through Death Star sites. Starts Central Core, Trash Compactor, Detention Block Corridor. Flip/flip-back depends on Leia/Obi-Wan/Luke/Jedi presence. | A Power Loss shutdown. |
| 30 | `7_296` | `CCT` Carbon Chamber Testing | Jabba's Prize or opponent Rebel deployed imprisoned at Security Tower. Flip by moving escorting frozen captive to Audience Chamber, or no initial captive. Back requires frozen captive unless setup flag says none existed. | Frozen-captive hardLose and setup flag. |
| 36 | `9_151` | `BHBM` Bring Him Before Me | Luke, or retargeted Leia/Kanan, captive relation with Vader/Emperor package. Flip when target captive. Flip back if target not present with Vader and not captive. | Retarget data and Vader/Emperor pull chain. |
| 43 | `110_6` | `COTVG` Court Of The Vile Gangster | Captives at Jabba's Palace/Tatooine site, with bounty hunter and Rancor/Sarlacc support. Flip requires captive count or captive relation. Flip back when none of the captive requirements remain. | Rancor/Sarlacc related hard-state. |

## Bucket 3: Stacked, Attached, Or Hidden State

Loader field to add:

| Field | Shape |
|---|---|
| `stackRules[]` | `{ hostCardIds, hostTitleFragments, count, comparator, cardFilterKey, destination, sourceZone }` |
| `attachedRules[]` | `{ attachedCardIds, attachedToFilterKey, requiredLocationFilterKey, count, comparator }` |
| `hiddenStateRules[]` | `{ markerCardIds, probeState, whileInPlayDataKey, consequence }` |

Objectives:

| Row | BP | Objective | Source-derived data to encode | Also needs |
|---:|---|---|---|---|
| 01 | `7_136` | `HB` Hidden Base | Hidden Base indicator and Rendezvous Point setup. Flip when deployment tracker clears. Hard consequence when probed. | Hidden/probed state, not English scan. |
| 09 | `12_89` | `THGSG` The Hyperdrive Generator's Gone | Credits Will Do Fine stack count reaches 4. Starts Watto's Junkyard, City Outskirts, Credits Will Do Fine. | Stack-count route to flip. |
| 21 | `210_25` | `THGG` The Hyperdrive Generator's Gone (V) | Credits Will Do Fine count beneath/stacked reaches 4, with Amidala/Jar Jar battleground conditions. | Shared stack-count with variant fields. |
| 25 | `221_67` | `HFTDG` Hunt For The Droid General | Grievous Will Run And Hide attached to objective, Clone Army X semantics, Jedi/Clone relations. Flip-back depends on attachment and Grievous/Jedi conditions. | Attached-card count and Clone Army X. |
| 48 | `211_26` | `ASM` A Stunning Move | Insidious Prisoner attached to your Separatist character at Invisible Hand site. Flip back when not attached. | Attached-card target location. |
| 52 | `219_1` | `AGTCP` A Great Tactician Creates Plans | Thrawn at battleground and Thrawn's Art Collection has at least 2 stacked artwork cards. Flip back if Thrawn absent or no artwork stacked except during battle. | Stack count plus battle-phase exception. |

## Bucket 4: Side-Aware Variant Split

Loader field to add:

| Field | Shape |
|---|---|
| `profileFamily` | Shared family key, for example `TDIGWATT` or `SHIELD_WILL_BE_DOWN` |
| `variantKey` | Exact variant, for example `classic`, `virtual_226_12`, `classic_222_14`, `virtual_222_30` |
| `variantScopedRules` | Same fields as other buckets, but never shared unless blueprint ids and source filters match |

Objectives:

| Row | BP | Objective | Source-derived data to encode | Hold reason |
|---:|---|---|---|---|
| 42 | `109_12` | `TDIGWATT` classic | Starts Cloud City battleground site, Secret Plans, All Wrapped Up. Flip requires Dark Deal on table, occupying Bespin system, occupying Bespin: Cloud City, and occupying a Cloud City battleground site. | Do not share with `226_12`; classic uses space-presence and Dark Deal package. |
| 56 | `226_12` | `TDIGWATT` V | Starts Cloud City battleground site and I'm Sorry. Flip requires control count 3 at Bespin locations, with opponent control constraint. | `TDIGWATT_V_226_12_BOUNDARY_MATH_2026-07-08.md` says hold until Bespin logic is split. |
| 53 | `222_14` | `TSWBDIM` classic | Starts Hoth markers and Prepare For A Surface Attack. Flip when Main Power Generators is blown away. Back out-of-play if no Hoth site occupied with required durable actor. | Classic and V have different source ids and gating. |
| 54 | `222_30` | `TSWBDIM` V | Same family, virtual profile. Flip on Main Power Generators blown away. HardLose if not occupying required Hoth site. Existing V160 logic must stay exact. | Keep disabled until variant-scoped Shield profile proves equivalent. |

## Bucket 5: Hard-Veto And Complex Trigger

Loader field to add:

| Field | Shape |
|---|---|
| `hardVetoActions[]` | `{ actionType, actionTextFragments, cardFilterKey, locationFilterKey, consequence, score, absoluteBlock }` |
| `specialFlipRules[]` | `{ triggerType, eventFilterKey, actorFilterKey, locationFilterKey, timing, alternatives[] }` |
| `missionRules[]` | Objective-specific state machines for Jedi Tests, blowing away, orbiting, tracked fleets, and end-of-turn flip-backs |

Objectives:

| Row | BP | Objective | Source-derived data to encode | Hold reason |
|---:|---|---|---|---|
| 03 | `7_138` | `MWYHL` Mind What You Have Learned | Jedi Test #5 completion by Luke/Leia apprentice. HardLose on Dagobah Force drain or apprentice placed out of play. | Jedi Test state machine. |
| 27 | `225_53` | `MWYHL` Mind What You Have Learned (V) | Luke on Dagobah, Beldon's Corridor/Yoda's Hut setup, Cloud City No Disintegrations, Patience. Flip during your turn with Luke on Dagobah. | Variant Jedi/Dagobah/Bespin split. |
| 28 | `226_28` | `THP` The Hidden Path | Occupy 2 non-Mapuzo sites with Jedi Survivor/Jedi, Jabiim and Fallen Order package, Anakin/opponent ability relation. | Existing V52b/V53b/V60/V62/V67aa/V67z weights must be ported exactly. |
| 31 | `7_297` | `HDADTJ` Hunt Down And Destroy The Jedi | Vader at battleground and no opponent Luke/Jedi at battleground. Veto Scanning Crew, non-Epic duel, Executor-site Force drain. Flip back if Vader absent or opponent Luke/Jedi appears. | High-risk hard veto and live V25/V29.12/V35.1/V40/V51/V137b scoring. |
| 44 | `111_6` | `SYCFA` Set Your Course For Alderaan | Death Star system, Alderaan or Jedha City, Docking Bay 327. Flip when Alderaan blown away. Pulls Death Star title cards and battleground systems. | Blown-away mission sequence. |
| 49 | `213_31` | `HDADTJ` Hunt Down And Destroy The Jedi (V) | Vader's Castle, Cloud City one-icon site, Visage set 13. Vader at battleground and opponent target absence. Flip back if Vader absent or opponent target appears. | Different source filters from classic Hunt Down. |
| 50 | `213_32` | `SC` Shadow Collective | Flip when you just hit a character, or when gangsters control two battlegrounds during battle phase. Back flips at end of each turn. | Event-trigger plus automatic end-turn flip-back. |
| 51 | `216_11` | `OTVOG` On The Verge Of Greatness | Krennic or Tarkin at Scarif battleground site and Death Star system orbiting Scarif. Back out-of-play if no Death Star system or Shield Gate. | Scarif and Death Star orbiting state, hardLose. |
| 55 | `225_32` | `TFOR` The First Order Reigns | Flip when Tracked Fleet blown away. HardLose when Kylo forfeited at Salt Plateau from lost battle with Han, Leia, or Luke present. | Battle-loss event and named-character hardLose. |

## Coverage Check

| Count | Meaning |
|---:|---|
| 58 | Total objective front/back pairs in canonical data |
| 2 | Enabled pilots excluded here: `8_167` Endor, `12_179` My Lord |
| 56 | Disabled objectives bucketed in this document |
| 30 | Bucket 1 location/count |
| 7 | Bucket 2 captive-state |
| 6 | Bucket 3 stacked/attached/hidden |
| 4 | Bucket 4 side-aware variant split |
| 9 | Bucket 5 hard-veto and complex trigger |

## K-2 Directive

Start with Bucket 1. Add the generic `flipLocationRules[]` and `flipBackLocationRules[]` loader contract, wire conservative scoring once, then enable one or two low-risk rows only after boundary math. Do not turn on the whole bucket in one commit. Probability that would create a 30-objective surprise party: 100 percent, and the party would be bad.
