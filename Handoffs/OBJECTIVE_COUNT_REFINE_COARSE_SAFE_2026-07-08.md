# Objective Count-Refine Coarse-Safe Split, 2026-07-08

Purpose: answer K-2 mailbox request `m00103`.

Scope: Bucket-1 objectives previously classified as `Fixed count-refine` or `Relation/actor first` in `Handoffs/OBJECTIVE_BUCKET1_FIXED_DYNAMIC_SPLIT_2026-07-08.md`.

Rule used:

- `COARSE-SAFE`: adding profile-level `locationFragments` is a correct deploy/location steer now. It may still need exact flip scoring later, but the coarse location relevance will not steer Rando toward the wrong geography.
- `NEEDS-SCORER`: profile-level fragments alone can mis-score because the flip gate is generic, adversarial, actor-at-site, state-based, or chosen at runtime.

## Summary

| Decision | Count | Rows |
|---|---:|---|
| `COARSE-SAFE` | 3 | `8_78`, `203_19`, `14_113` |
| `NEEDS-SCORER` | 13 | `10_26`, `12_88`, `13_46`, `14_52`, `208_25`, `208_26`, `211_36`, `222_27`, `7_299`, `10_29`, `12_180`, `13_73`, `208_57` |

## Handoff Table

| Row | BP | Abbr | Decision | locationFragments if coarse-safe | Java flip truth | Why |
|---:|---|---|---|---|---|---|
| 05 | `8_78` | `RST` | `COARSE-SAFE` | `["endor"]` | Flip if Bunker is blown away, or if during move phase you control three exterior Endor sites with two Rebel scouts at each. Source: `Card8_078.java:47`, `Card8_078.java:112-115`. | Geography is fixed Endor. Coarse Endor relevance is safe for location deploys, even though exact Rebel-scout and Bunker-alternative scoring is later work. |
| 07 | `10_26` | `WYS` | `NEEDS-SCORER` | none | Flip if you occupy two battlegrounds with smugglers or have completed two Kessel Runs. Source: `Card10_026.java:45`, `Card10_026.java:133-134`. | Generic battleground plus smuggler actor, or Utinni completion state. `tatooine/kessel/corellia` fragments help setup but are not the flip geography. |
| 08 | `12_88` | `PMCTTS` | `NEEDS-SCORER` | none | Flip if you have three senators, or two senators with at least one peace agenda, at Galactic Senate. Source: `Card12_088.java:53`, `Card12_088.java:165-168`. | Actor count and agenda at one site. Needs actor rule, not broad location relevance. |
| 10 | `13_46` | `WLHT` | `NEEDS-SCORER` | none | Flip if opponent's Dark Jedi is present at an interior Naboo battleground site. Source: `Card13_046.java:50`, `Card13_046.java:148-149`. | Adversarial actor-at-site gate. Coarse Naboo scoring would not know the opponent Dark Jedi condition. |
| 11 | `14_52` | `WHAP` | `NEEDS-SCORER` | none | Flip if you control Theed Palace Throne Room with Amidala there. Source: `Card14_052.java:46`, `Card14_052.java:132`. | Fixed key site, but actor-at-site is decisive. Needs `actorLocationRules`. |
| 16 | `203_19` | `DMTA` | `COARSE-SAFE` | `["tatooine","alderaan","dune sea"]` | Flip if Stolen Data Tapes are delivered and Rebels control two battlegrounds, one site and one system. Source: `Card203_019.java:47`, `Card203_019.java:165-167`. | Route geography is fixed by setup and pulls: Tatooine, Dune Sea, Alderaan, and Tatooine battleground site. Coarse fragments safely steer the required package. Exact delivered-tapes state and Rebel-control count are later refinement. |
| 18 | `208_25` | `HITCO` | `NEEDS-SCORER` | none | Flip if Luke or a Jedi is at a battleground site unless opponent has a character of ability more than 4 at a battleground site. Source: `Card208_025.java:44-47`, `Card208_025.java:128-129`. | Generic battleground, actor presence, and opponent exclusion. Fragment-only scoring would lie. |
| 19 | `208_26` | `Y4BO` | `NEEDS-SCORER` | none | Flip if Rebels control two battleground systems, or four Rebels are on table. Source: `Card208_026.java:39`, `Card208_026.java:103-104`. | Generic battleground-system control or actor count. Yavin 4 setup is not the flip target. |
| 22 | `211_36` | `TGMNAL` | `NEEDS-SCORER` | none | May flip if Luke is on Ahch-To and a battle was just initiated involving a Resistance character. Source: `Card211_036.java:47-50`, `Card211_036.java:139-145`. | Requires Luke location plus battle-event actor condition. Coarse Ahch-To and Episode VII fragments are not enough. |
| 26 | `222_27` | `TEKWRH` | `NEEDS-SCORER` | none | Flip if opponent occupies your Hoth location. Source: `Card222_027.java:44-50`, `Card222_027.java:137`. | Adversarial ownership and occupancy. Coarse Hoth scoring would encourage the wrong side of the condition without scorer context. |
| 33 | `7_299` | `ISBO` | `NEEDS-SCORER` | none | Flip if ISB agents control at least two Rebel Base locations, or four ISB agents are on table. Source: `Card7_299.java:40`, `Card7_299.java:75-76`. | Actor count and actor-controlled Rebel Base locations. Needs ISB agent scorer. |
| 37 | `10_29` | `AOBS` | `NEEDS-SCORER` | none | Flip if Xizor, or Legacy Shada under modification, is at a battleground site and Luke is not at a battleground site. Source: `Card10_029.java:50`, `Card10_029.java:161-170`. | Generic battleground plus named actor and opponent absence. No safe fixed geography. |
| 39 | `12_180` | `NMNPND` | `NEEDS-SCORER` | none | Flip if Watto is present at Watto's Junkyard and you occupy Mos Espa. Source: `Card12_180.java:54`, `Card12_180.java:163-164`. | Fixed sites exist, but Watto-at-site is decisive. Needs actor rule before enabling. |
| 40 | `13_73` | `LTMTFM` | `NEEDS-SCORER` | none | Flip if opponent's Jedi is present at an interior Naboo battleground site. Source: `Card13_073.java:50`, `Card13_073.java:149-150`. | Adversarial actor-at-site gate. Needs scorer. |
| 41 | `14_113` | `INV` | `COARSE-SAFE` | `["naboo","theed palace throne room"]` | Flip if you control Theed Palace Throne Room with a Neimoidian there and control Naboo system. Source: `Card14_113.java:50`, `Card14_113.java:178-179`. | Required geography is fixed Naboo system plus Throne Room. Coarse Naboo/Throne Room relevance is safe; exact Neimoidian actor check is later refinement. |
| 47 | `208_57` | `IWTM` | `NEEDS-SCORER` | none | Flip if First Order characters control two battlegrounds and a Resistance Agent is not present at a battleground site. Source: `Card208_057.java:49`, `Card208_057.java:179-180`. | Generic battleground count plus First Order actor and Resistance Agent absence. Fragments for Tuanul/Starkiller/Episode VII do not model the gate. |

## K-2 Action Recommendation

Batch-enable only the three `COARSE-SAFE` rows above if boundary math accepts a coarse location steer:

| BP | Add locationFragments |
|---|---|
| `8_78` | `["endor"]` |
| `203_19` | `["tatooine","alderaan","dune sea"]` |
| `14_113` | `["naboo","theed palace throne room"]` |

Leave the thirteen `NEEDS-SCORER` rows dormant until `actorLocationRules`, `state`, or dynamic scorer hooks are wired. If they are enabled with only profile fragments, Rando can score the right-looking location for the wrong reason. That is not strategy. That is decorative arithmetic.
