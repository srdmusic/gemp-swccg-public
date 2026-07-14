# TDIGWATT V 226_12 Boundary Math and Loader Verification

Created: 2026-07-08
Author: Codex Alfred
Audience: K-2 Claude

## 1. Loader Fix Verification For Commit 1a3062990

| Check | Result | Evidence |
|---|---:|---|
| Only pilot profiles hydrate | PASS | `objective_playbooks.json` has exactly two `loaderEnabled=true` profiles: `8_167` and `12_179`. |
| Non-pilot profiles inert | PASS | `109_12` and `226_12` both have `rolloutEnabled=false`, `rolloutStage=data_only`, and no `loaderEnabled=true`. |
| `pullableCards` hydration deferred | PASS | `ObjectiveAnalyzer.hydrateFromProfile()` has the pullable hydration block commented out in both rando and chosenone. |
| Rando to chosenone mirror | PASS | Normalized diff is empty after replacing only package/import namespaces. |
| Compile | PASS | In-container Maven compile returned `MVN_EXIT=0`, `[ERROR]` count `0`. |
| My Lord no-op scoring slots | PASS | `12_179` hydrates empty `locationFragments`, `requiredCardsOnTable`, `pullableCards`, `flipGateSite`, and `flipGateCardIds`. Starting locations and weights are present but not consumed by current scoring yet. |
| Endor byte-identical to hardcoded block | WARN | Endor JSON hydrates `locationFragments`, `requiredCardsOnTable`, `flipGateSite`, and scoped `flipGateCardIds`, but it does not hydrate `flipCriticalControlCard = "establish secret base"`. Current behavior remains safe only because the old hardcoded Endor block still runs after JSON hydration. |

K-2 action before commenting out the Endor hardcoded block:

| Required fix | Why |
|---|---|
| Add a JSON field for the flip-gate card display/name, or derive it from canonical data, then hydrate `flipCriticalControlCard`. | `DeployEvaluator` uses IDs for detection when present, but still reads `getFlipCriticalControlCard()` for fallback and reasoning text. Without the old hardcoded block, Endor V193 reasoning becomes `null`, and future no-id gate profiles lose fallback detection. |

## 2. Actual Card Source, Classic Vs Virtual

| Field | Classic TDIGWATT `109_12` | Virtual TDIGWATT `226_12` | Boundary |
|---|---|---|---|
| Source files read | `Card109_012.java`, `Card109_012_BACK.java` | `Card226_012.java`, `Card226_012_BACK.java` | Java source wins over DB text and old playbook assumptions. |
| Front setup | Deploy one `Filters.Cloud_City_battleground_site`; optional `Filters.Secret_Plans`; optional `Filters.All_Wrapped_Up`. | Deploy `Filters.Cloud_City_battleground_site`; deploy `Filters.and(Icon.CLOUD_CITY, Filters.Im_Sorry)`. | Do not share starting-effect list. |
| Front pull | Once during deploy phase, take `Filters.or(Filters.Bespin_system, Filters.Bespin_Cloud_City, Filters.Dark_Deal, Filters.Cloud_City_Occupation)`. | Once during turn, take `Filters.or(Filters.Dark_Deal, Filters.Vaders_Bounty, Filters.and(Icon.SPECIAL_EDITION, Filters.Bespin_system))`. | V does not pull Cloud City Occupation. |
| Executor path | No deploy-forbid clause. Classic can use Executor at Bespin. | Objective text and modifiers forbid `[Death Star II] Executor` and Admiral's Orders. | All Executor/AMSD positives must be gated off for V. |
| Flip condition | `Dark Deal` on table, occupy `Filters.Bespin_system`, occupy `Filters.Bespin_Cloud_City`. | Control 3 `Filters.Bespin_location`; opponent controls fewer than 3. | Classic wants Bespin system plus Cloud City sector. V wants location-count control. |
| Flip-back condition | `Dark Deal` canceled, opponent controls `Filters.Bespin_system`, or Bespin blown away. | Opponent controls more `Filters.Bespin_location` than Rando. | Classic is system/Dark Deal fragile. V is location-count fragile. |
| Post-flip character facts | Imperial at controlled Bespin locations protects drains; alien/Imperial pair gets destiny bonus. | Lando/Lobot protect drain bonuses; Vader at Bespin cancels Admiral's Orders; alien/Imperial pair gets destiny bonus; Lando destiny tweak. | Character categories overlap, but details are not identical. |

## 3. Side-Aware Bespin And Cloud City ID Notes

| Requirement | Correct handling |
|---|---|
| `[Special Edition] Bespin` for `226_12` | Specific upload target is `Filters.and(Icon.SPECIAL_EDITION, Filters.Bespin_system)`. Current resolved id is `223_8`. Do not use broad `5_164` for this specific pull. |
| Broad `Filters.Bespin_system` | Classic `109_12` can reference all Bespin system printings/candidates, including `5_164`, `223_8`, `5_76` in current DB snapshots. |
| `Filters.Bespin_location` | Broad runtime filter, includes system, Cloud City sector/site, and relevant Bespin locations. Do not collapse into a single card id list without runtime filter support. |
| `Filters.Cloud_City_battleground_site` | Runtime battleground truth is modifiers/filter logic. Static DB snapshots are advisory only. |
| `Bespin: Cloud City` | Classic `109_12` specifically needs `Filters.Bespin_Cloud_City`, not just any Cloud City site. |

## 4. Existing TDIGWATT Score Surfaces And Magnitudes

| V-tag | File surface | Current magnitude | Applies cleanly to classic? | Applies cleanly to V? | Notes |
|---|---|---:|---:|---:|---|
| V22.5 | `ObjectiveAnalyzer.needsBespinSystemPresence()` | boolean gate | YES | PARTIAL | Detects Bespin/Cloud City objective text. Too broad to mean "needs Executor at Bespin" for `226_12`. |
| V29 bug-B | `DeployEvaluator` plus `ObjectiveAnalyzer.objectiveForbidsDeployingExecutor()` | releases `-500` gate | YES | YES | Correctly releases Bespin-first character hostage gate when objective forbids Executor or no capital path exists. |
| V29 BESPIN-FIRST | `DeployEvaluator` | `-500` for non-exempt deploys before Bespin space occupied | YES | NO, unless released | Classic should keep it pre-Executor. V must not wait for a banned Executor. |
| V24.10 Executor target | `CardSelectionEvaluator` | `+500` Bespin, `-9999` non-Bespin | YES | NO | V forbids DSII Executor, so this should be classic-only or capital-path-only. |
| V24.10 AMSD action | `ActionTextEvaluator` | `+300`, `+1500`, or `-9999` depending path | YES | NO | V forbids DSII Executor and Admiral's Orders, so AMSD chain should not be treated as objective setup for V. |
| V24.12 admiral pull | `CardSelectionEvaluator` | Piett `+300`, Chiraneau `+150`, Ozzel `+100` | YES | NO as objective setup | V does not want Executor pipeline. These can remain generic only if not tied to TDIGWATT V objective weight. |
| V24.1 Gherant commander pull | `CardSelectionEvaluator` | `+400` | YES | NO as objective setup | "Gherant deploys Executor site" is classic engine logic, not V flip logic. |
| V24.2 Lando/Lobot pull | `CardSelectionEvaluator` | Lando `+250`, Lobot `+200`, Lando unsafe `-9999` | YES | YES with caveat | V explicitly uses Lando movement and Lobot protection. Classic uses Lando/Lobot too. Keep, but source from character requirements, not broad Bespin system gate. |
| V24.6 I'm Sorry action | `ActionTextEvaluator` | `+250` if CC sites remain, `-300` if exhausted | NO for classic base | YES | `226_12` starts `[Cloud City] I'm Sorry`; classic `109_12` does not. |
| V24.10 starting CC exterior | `CardSelectionEvaluator` | exterior `+500`, interior `-500` | PARTIAL | YES | V starts a CC battleground site and I'm Sorry can pull interiors, so exterior-first logic remains relevant. |
| V26 objective CC site pick | `CardSelectionEvaluator` | Upper Walkway `+500`, Dining Room `-400`, other interior `-200` | PARTIAL | YES | Current text distinguishes objective pick from Slip Sliding. Good candidate for JSON-driven starting location preferences. |
| V25 CC ability spread | `CardSelectionEvaluator` | reinforce about `+100` plus deficit, Lando support `+250`, spread `+120`, overstack `-40` | YES | YES | Should consume "Bespin location count" and "Cloud City site" objective facts, not `needsBespinSystemPresence()` alone. |
| V47 Lando stay | `MoveEvaluator` and card-selection movement | hard veto class, old `-9999`; support `+250`; pull block `-9999` | YES | YES with caveat | V has Lando regular move. Keep objective/site relevance and survivability gates. |
| V40/V46 HOLD_BACK | `DeployEvaluator`, `DrawEvaluator` | no score, strategy gate | PARTIAL | PARTIAL | Current code treats any analyzed non-Hunt Down objective as TDIGWATT-ish. Needs playbook category, not broad "not Hunt Down". |
| V52 TDIGWATT T1 script | `DeployEvaluator` | Bespin system `+1500`, CC site `+1200`, Lando Broker `+1000`, Executor/Flagship `+900`, Chiraneau `+850` | YES | MIXED, dangerous | CC site is useful to V. Executor and Chiraneau are wrong for V. Bespin system is only `[SE] Bespin` upload target, not required by V flip text. |
| V29.6 Dining Room Lando | `ActionTextEvaluator` | Lando with friendlies `+150`, alone `-30` | YES | YES with caveat | Works if tied to Lando/Dining Room package, not to broad Bespin-space need. |

## 5. Boundary Math For Enabling `226_12`

Do not enable runtime hydration for `226_12` until the score consumers can distinguish these objective categories:

| Category | Classic `109_12` | Virtual `226_12` | Consumer impact |
|---|---|---|---|
| `requiresBespinSpacePresence` | true | false | Gates V29 BESPIN-FIRST, V24.10 Executor target, AMSD, admiral/Gherant pipeline, V52 Executor/Chiraneau scores. |
| `requiresBespinLocationControlCount` | false | true, count 3 | Feeds generic location relevance, deploy site priority, hold/contest/flip-back protection. |
| `startsImSorry` | false | true | Feeds I'm Sorry action boost and CC interior pull ordering. |
| `startsOptionalSecretPlansAllWrappedUp` | true | false | Classic-only setup fields. |
| `pullsCloudCityOccupation` | true | false | Classic-only engine card. Do not give V this pull. |
| `pullsVadersBounty` | false | true | V-only upload target. |
| `forbidsExecutor` | false | true | Releases Executor wait gates and prevents objective setup scoring for DSII Executor/AMSD. |

Minimum safe rule before flipping `loaderEnabled=true` for `226_12`:

| Old broad gate | Replacement playbook field |
|---|---|
| `needsBespinSystemPresence()` used as "TDIGWATT wants Executor at Bespin" | `requiresBespinSpacePresence == true` |
| `needsBespinSystemPresence()` used as "Bespin/Cloud City locations matter" | `locationRequirements` with `Filters.Bespin_location`, `Filters.Cloud_City_battleground_site`, or `Filters.Bespin_Cloud_City` |
| hardcoded "TDIGWATT T1" script | Per-profile setup priorities with excluded cards when `forbidsExecutor == true` |
| broad Bespin id samples | runtime filters plus specific named card refs, especially `[Special Edition] Bespin` id `223_8` |

## 6. Recommended K-2 Execution Order

| Step | Action | Verification |
|---|---|---|
| 1 | Add `flipGateCardLabel` or equivalent to runtime JSON schema and hydrate `flipCriticalControlCard`. | Endor JSON-only profile can reproduce hardcoded Endor slots before hardcoded block is commented. |
| 2 | Add playbook booleans/categories for `requiresBespinSpacePresence`, `requiresBespinLocationControlCount`, `startsImSorry`, `forbidsExecutor`, and classic-only/V-only pull flags. | `109_12` true for space pipeline, `226_12` false for space pipeline and true for location-count. |
| 3 | Change V24.10/V29/V52/AMSD/admiral consumers to read those booleans, not `needsBespinSystemPresence()` where the meaning is "Executor pipeline". Comment old condition lines in place. | Classic keeps existing Executor/AMSD scores. V gets zero Executor/AMSD objective setup bonuses and no Bespin-first wait. |
| 4 | Keep location relevance and Lando/Lobot logic shared through generic location and character requirements. | Both TDIGWATT rows still value Cloud City/Bespin locations and Lando/Lobot where source says so. |
| 5 | Only then consider `loaderEnabled=true` for `226_12`. | Run targeted log check for no V29 BESPIN-FIRST blocks in V game, no V52 Executor/Chiraneau objective boosts in V game, and normal classic behavior in `109_12`. |

## 7. One-Line Verdict

`1a3062990` is behavior-safe as committed because only My Lord and Endor hydrate, pullables are deferred, and Endor still has the old hardcoded block. It is not yet safe to comment out the Endor block or enable TDIGWATT V without adding the missing flip-gate card label and splitting "Bespin locations matter" from "Executor at Bespin matters."
