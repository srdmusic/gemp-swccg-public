# Bucket 1 Fixed vs Dynamic Split

Generated: 2026-07-08 by Codex Alfred

Scope: the 30 Bucket 1 location/count objectives from `Handoffs/OBJECTIVE_EXTENSION_BUCKETS_2026-07-08.md`.

Update 2026-07-08: source re-check moved `204_32` Old Allies, `109_4` Quiet Mining Colony, and `301_2` City In The
Clouds from count-refine to fragment-first pilots. Their exact count/key-site/opponent-control clauses still belong in
`flipLocationRules[]` later, but the active-objective fragment steer is valid. `209_29` THNIWRC also passed as a Scarif
fragment-first enable.

## Verdict

| Class | Meaning | K-2 action |
|---|---|---|
| Fixed fragment-first | A single planet/location fragment is a useful first scoring step. Existing `isObjectiveRelevantLocation` plus V136 `+200` and the section D cap waiver can steer spread without a new count slot. | Enable one at a time after source check and compile. |
| Fixed count-refine | A fixed fragment helps, but the objective has key-site, exact count, actor, opponent-control, or alternate conditions that can mis-score if treated as "all matching locations are equal." | Add `flipLocationRules[]` before broad enable, or use fragment only as a narrow pilot with caveat. |
| Dynamic runtime location | The location group is chosen during play, not fixed by title. A static fragment is wrong or incomplete. | Needs dynamic slot, not fragment-only. |
| Relation/actor first | The decisive condition is actor presence, named character, target absence, or faction count more than location title. | Needs `actorLocationRules[]` / `controlWithCount` shape before enable. |

## Batch List

| Row | BP | Objective | Class | Fragment / field | Source-derived reason |
|---:|---|---|---|---|---|
| 00 | `7_135` | `DBO` Dantooine Base Operations | Fixed fragment-first | `locationFragments=["dantooine"]` | Control 3 Dantooine sites with Rebels. Verified live pilot. |
| 02 | `7_137` | `LU` Local Uprising | Dynamic runtime location | `dynamicPlanet=subjugatedPlanet` | Objective chooses the Subjugated planet at setup. Static fragment cannot know which planet. |
| 05 | `8_78` | `RST` Rebel Strike Team | Fixed count-refine | `locationFragments=["endor"]`, plus Bunker alt | Control 3 Endor locations or blow away Bunker. Needs Bunker alternative and Rebel scout context. |
| 07 | `10_26` | `WYS` Watch Your Step | Fixed count-refine | `locationFragments=["tatooine","kessel","corellia"]` plus smuggler/Utinni alt | Flip can be 2 battlegrounds with smugglers or Kessel Run. Fragment alone is too blunt. |
| 08 | `12_88` | `PMCTTS` Plead My Case To The Senate | Relation/actor first | `actorLocationRules` for Senate | Needs senator/agenda-character count at Galactic Senate, not generic Coruscant or Senate-site spread. |
| 10 | `13_46` | `WLHT` We'll Handle This | Relation/actor first | `actorLocationRules` | Opponent Dark Jedi present at interior Naboo battleground. Actor relation is the gate. |
| 11 | `14_52` | `WHAP` We Have A Plan | Relation/actor first | `keySite=Theed_Palace_Throne_Room`, `keyActor=Amidala` | Throne Room control with Amidala/Panaka logic, not broad Naboo spread. |
| 12 | `109_4` | `QMC` Quiet Mining Colony | Fixed fragment-first | `locationFragments=["bespin","cloud city"]` | Bespin/Cloud City control gate. Fragment steers required locations; exact Cloud City count, Lando/Lobot shortcut, and opponent-control constraints are later refinement. |
| 14 | `111_4` | `MBO` Massassi Base Operations | Fixed fragment-first | `locationFragments=["yavin 4"]` | Control 3 Yavin 4 sites. Similar fixed-planet spread pattern, plus blown-away hardLose later. |
| 15 | `112_1` | `AITC` Agents In The Court | Dynamic runtime location | `repSpecies`, `battleground_site`, Tatooine/non-Tatooine alt | Rep species and two-alien condition dominate. Static site fragment is not enough. |
| 16 | `203_19` | `DMTA` Diplomatic Mission To Alderaan | Fixed count-refine | `locationFragments=["tatooine","alderaan"]`, plus delivered tapes | Delivered Stolen Data Tapes and multi-location occupy set. Fragment alone misses the state machine. |
| 17 | `204_32` | `OA` Old Allies | Fixed fragment-first | `locationFragments=["jakku"]` | Control/occupy Jakku system plus two Jakku battleground sites. Fragment steers all required Jakku locations; exact system/site relation is later refinement. |
| 18 | `208_25` | `HITCO` He Is The Chosen One | Relation/actor first | `actorLocationRules` | Luke/Jedi at battleground and opponent high-ability condition. Not a planet-fragment problem. |
| 19 | `208_26` | `Y4BO` Yavin 4 Base Operations | Fixed count-refine | `locationFragments=["yavin 4"]`, plus Rebel count/system alt | Four-Rebel alternative and battleground system relations need explicit rules. |
| 20 | `209_29` | `THNIWRC` They Have No Idea We're Coming | Fixed fragment-first | `locationFragments=["scarif"]` | Control/occupy Scarif locations. Good fixed-fragment candidate, but keep separate from dark Scarif Verge. |
| 22 | `211_36` | `TGMNAL` The Galaxy May Need A Legend | Relation/actor first | `actorLocationRules` for Luke on Ahch-To and Resistance | Luke/Resistance conditions dominate over broad Ahch-To/E7 fragments. |
| 24 | `219_48` | `ZH` Zero Hour | Fixed fragment-first | `locationFragments=["lothal"]` | Control 3 Lothal locations or Phoenix Squadron battleground condition. Fragment is useful first pass. |
| 26 | `222_27` | `TEKWRH` The Empire Knows We're Here | Fixed count-refine | `locationFragments=["hoth"]`, plus opponent-occupies-your-Hoth | It is fixed Hoth, but the gate is opponent occupying your Hoth location. |
| 29 | `301_2` | `CITC` City In The Clouds | Fixed fragment-first | `locationFragments=["bespin","cloud city"]` | Control 2 Cloud City battleground sites plus occupy Bespin system. Fragment steers required locations; exact system/site relation and opponent-control constraint are later refinement. |
| 32 | `7_298` | `IO` Imperial Occupation | Dynamic runtime location | `dynamicPlanet=renegadePlanet` | Objective chooses Renegade planet at setup. Static fragment cannot know it. |
| 33 | `7_299` | `ISBO` ISB Operations | Relation/actor first | `controlWithCount` with ISB agent / Rebel Base | Four ISB agents or ISB-controlled Rebel Base locations. Actor count is the gate. |
| 34 | `7_300` | `RO` Ralltiir Operations | Fixed fragment-first | `locationFragments=["ralltiir"]` | Control 3 Ralltiir sites with Imperials. Verified live pilot. |
| 37 | `10_29` | `AOBS` Agents Of Black Sun | Relation/actor first | `actorLocationRules` for Xizor/Shada and target absence | Battleground target absence, not broad Coruscant spread. |
| 39 | `12_180` | `NMNPND` No Money, No Parts, No Deal! | Relation/actor first | `keySite=Watto's Junkyard`, `keyActor=Watto` | Watto present and occupying Watto's Junkyard. One key site. |
| 40 | `13_73` | `LTMTFM` Let Them Make The First Move | Relation/actor first | `actorLocationRules` | Dark Jedi present with opponent Jedi at interior Naboo battleground. |
| 41 | `14_113` | `INV` Invasion | Fixed count-refine | `locationFragments=["naboo"]`, plus Throne Room and system split | Needs Throne Room with presence droid plus Naboo system control. Fragment alone is too broad. |
| 45 | `112_15` | `MKOS` My Kind Of Scum | Dynamic runtime location | `repSpecies`, `battleground_site`, Tatooine/non-Tatooine alt | Rep/species and Jabba/Bib alternatives dominate. |
| 46 | `201_39` | `IE` Imperial Entanglements | Fixed fragment-first | `locationFragments=["tatooine"]` | Control 3 Tatooine sites while opponent controls fewer. Good fixed-fragment candidate. |
| 47 | `208_57` | `IWTM` I Want That Map | Fixed count-refine | `locationFragments=["starkiller base","tuanul","episode vii"]`, plus First Order/BB-8 | Fixed locations help, but BB-8/map and First Order actor rules are required. |
| 57 | `301_4` | `TSOT` Twin Suns Of Tatooine | Fixed fragment-first | `locationFragments=["tatooine"]` | Control and occupy 2 Tatooine battleground sites with Dark Jedi. Fragment first pass is useful. |

## Fixed-Fragment Enables Completed

| BP | Objective | Fragment |
|---|---|---|
| `7_135` | DBO | `dantooine` |
| `7_300` | RO | `ralltiir` |
| `111_4` | MBO | `yavin 4` |
| `219_48` | ZH | `lothal` |
| `201_39` | IE | `tatooine` |
| `301_4` | TSOT | `tatooine` |
| `204_32` | OA | `jakku` |
| `209_29` | THNIWRC | `scarif` |
| `109_4` | QMC | `bespin`, `cloud city` |
| `301_2` | CITC | `bespin`, `cloud city` |

## Scoring Sufficiency Finding

Dantooine and Ralltiir are correct as first fixed-fragment pilots:

| Check | Result |
|---|---|
| Source truth | `Card7_135` and `Card7_300` both deploy the named system, pull a site or non-unique matching affiliation to that planet, and flip on controlling 3 named-planet sites with the matching side. |
| Fragment match | `dantooine` and `ralltiir` match the system plus titled sites, and no unrelated card titles in the objective location set. |
| Score path | `isObjectiveRelevantLocation` makes the location true, V136 section B adds `+200`, and V136 section D does not apply the third-ground-battleground or third-system cap when `isObjectiveRelevantSite` is true. |
| Spread behavior | Empty relevant sites get the objective bonus and cap waiver. Existing per-site over-stack penalties still apply, so the path favors spreading once a site is already loaded. |
| Caveat | This does not count "exactly 3 sites" or require Rebel/Imperial actor filters yet. It is sufficient to turn on location relevance, not sufficient as final flip-completion logic. |
