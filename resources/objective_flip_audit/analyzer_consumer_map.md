# ObjectiveAnalyzer Runtime Consumer Map

Date: 2026-07-24. Branch `rando-consolidation-2026-06-23`, HEAD `192abf72d`.
Scope: what the LIVE AI brain actually consumes from `src/gemp-swccg-server/src/main/resources/objective_playbooks.json`, so a per-objective gap-diff can classify each profile field ACTIVE / INFORMATIONAL / DEAD.

All paths below are relative to `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/` unless absolute. "OA" = `models/common/strategy/ObjectiveAnalyzer.java` (the shared live analyzer, 2,253 lines). "DE" = `models/rando/evaluators/DeployEvaluator.java`, "CSE" = `models/rando/evaluators/CardSelectionEvaluator.java`, "DPP" = `models/rando/strategy/DeployPhasePlanner.java`, "ME" = `models/rando/evaluators/MoveEvaluator.java`. Chosenone copies are identical (see §6).

---

## 1. Live analyzer vs dead code

| Class | Status | Evidence |
|---|---|---|
| `models/common/strategy/ObjectiveAnalyzer.java` | LIVE (SVC-INTEL, "the LIVE objective brain") | header OA:19-33 |
| `models/rando/strategy/ObjectiveAnalyzer.java`, `models/chosenone/strategy/ObjectiveAnalyzer.java` | LIVE 12-line facades: `extends common ObjectiveAnalyzer`, pass bot logger, separate mutable instances | rando facade lines 6-12; OA:28-29 (V207 note) |
| `models/rando/strategy/ObjectiveHandler.java`, `models/chosenone/strategy/ObjectiveHandler.java` | DEAD — DO-NOT-WIRE header still stands: "DEAD CODE — DO NOT WIRE (reorg 2026-07-06) ... V295 retired its inert RandoCalAi and DecisionContext wiring ... retired 2026-07-19" | rando ObjectiveHandler:8-15; wiring removal confirmed at `models/rando/RandoCalAi.java:1507` (`// V295 RETIRED: evalContext.setObjectiveHandler(objectiveHandler);`) |

Nothing in this map is sourced from ObjectiveHandler.

## 2. Profile loader and `loaderEnabled` runtime semantics

- Load: lazy, thread-safe singleton `PROFILES` (OA:906-918); `loadProfiles()` reads classpath `/objective_playbooks.json` via Gson binding of `JsonRoot`/`JsonProfile` (OA:920-936). Hard fallback: missing/malformed file → empty registry → text-parser output stands (OA:924, 932-935).
- Match: `findProfile(bpId, title)` (OA:939-955) matches by `blueprintIds` first, then `titleFragments` substring. **It does NOT filter on loaderEnabled** — a disabled profile is still returned.
- Gate: in `analyze()` (OA:227-232), `loaderOn = prof != null && Boolean.TRUE.equals(prof.loaderEnabled)`. When true, `activePlaybook = buildPlaybookFromProfile(prof)` and `hydrateFromProfile(prof)` runs (OA:257). When false/absent: NO hydration, and `activePlaybook` falls back to the compiled statics (`MY_LORD_PLAYBOOK` / `ENDOR_PLAYBOOK`, OA:794-815) or null.

**`loaderEnabled=false` (or absent) means the profile is loaded into the registry and matched, but fully inert at runtime — none of its data reaches any analyzer slot, playbook, or score; the V21/V22.2/V25/V160/V186 text parsers and compiled statics stand unchanged.** 15 of 58 profiles carry `loaderEnabled: true` (Dantooine Base Operations, Rebel Strike Team, Quiet Mining Colony, Massassi Base Operations, Diplomatic Mission To Alderaan, Old Allies, They Have No Idea We're Coming, Zero Hour, City In The Clouds, Ralltiir Operations, Endor Operations, My Lord Is That Legal, Invasion, Imperial Entanglements, Twin Suns Of Tatooine); the other 43 omit the key.

Hydration behavior (OA:959-1005): additive/idempotent for most slots, EXCEPT `requiredCardsOnTable` which is AUTHORITATIVE clear-then-set (OA:966-969), and `pullableCards` whose hydration is commented out (OA:970-974).

## 3. Field classification

Legend: **ACTIVE** = reaches a decision score/veto/ordering. **INFORMATIONAL** = loaded into a runtime fact, but no decision consumer reads it. **DEAD** = never deserialized or never read by live code.

| JSON profile field | Loader binding | Analyzer fact it feeds | Decision consumers (file:line) | Class |
|---|---|---|---|---|
| `loaderEnabled` | `JsonProfile.loaderEnabled` OA:835 | master per-profile switch (OA:227-232, 257) | gates everything below | **ACTIVE** |
| `blueprintIds` | OA:836 | profile selection (`findProfile` OA:942-945); also `ObjectivePlaybook.identity` (OA:1107, 1117) — identity itself has no downstream reader | selects which profile hydrates/builds the playbook | **ACTIVE** (as selection key) |
| `titleFragments` | OA:837 | profile selection fallback (OA:947-953); playbook identity (unused downstream) | same as above | **ACTIVE** (as selection key) |
| `locationFragments` | OA:838 | `flipConditionLocationFragments` via `addLocationFragment` (OA:961-962) → `isObjectiveRelevantLocation(title)` OA:268-279, `getLocationObjectiveBonus` OA:343-362 (+150 pre-flip / +200 flip-back-protect) | DPP:642-643/655-656 (V22 location-deploy ordering), DPP:1184-1191 (+50 establish sort), DPP:1613-1617 (V22 plan score via `DeployPlanRankingPolicy` `models/common/phase/DeployPlanRankingPolicy.java:46-49`); DE:1227 (V136 +200 obj site), DE:1683, DE:2807; CSE:2141-2142, 2701, 2875, 2948, 3148, 3184, 3295, 3856, 4025, 5342, 5772, 6691-6698; ME:642, 1043; `getFlipConditionLocationFragments` → DPP:972 (V22 Bespin capital plan), DE:3278/3718/3803, ME:1175, CSE:7120 (SetupPolicy.startingEffectObjective) | **ACTIVE** |
| `requiredCardsOnTable` | OA:839 | `requiredCardsOnTable` set, AUTHORITATIVE clear-then-set (OA:966-969) → `isRequiredCardForFlip` OA:321-330, `getRequiredCardsOnTable` OA:518, `assessDeployChild` OA:592-655 | CSE:4178/4187 (V21 objective-critical force-loss protection), CSE:4299, CSE:4452; CSE:7123 (SetupPolicy.startingEffectObjective); `assessDeployChild` consumer CSE:1116-1120 is **log-only** (`logger.debug`, no score) | **ACTIVE** |
| `pullableCards` | OA:840 (bound) | hydration COMMENTED OUT — "DEFERRED ... Kept off for now" (OA:970-974). Runtime `pullableCards` set is text-parser-only (OA:1845-1881) | none from JSON | **DEAD** (profile field never reaches runtime) |
| `flipGateSite` | OA:841 | `flipCriticalControlSite` (OA:975-976) → `getFlipCriticalControlSite` OA:375, `isActiveFlipGateLocationTitle` OA:388-393 | DE:1270-1272 (V193 flip-gate steer, weight at DE:1359); CSE:2758-2814 (V193-CS steer, weight + 1600 offset at CSE:2809); DPP:764 (V297 formation plan target); CSE:282 → `PULL.OBJECTIVE.FLIP_GATE_SITE` +300 (`models/common/phase/PullSelectionCandidatePolicy.java:71-75`) — note this pull rule requires an ACTOR rule too (OA:389-390 `findFlipGateActorRule() != null`), so it fires for Invasion, not Endor | **ACTIVE** |
| `flipGateCardName` (Endor only) | OA:842 | `flipCriticalControlCard` (OA:977-978) → `getFlipCriticalControlCard` OA:376 | DE:1316, 1338-1340 (holds-gate-card fallback detection); CSE:2773, 2788-2791 | **ACTIVE** |
| `flipGateCardIds` | OA:843 | `flipCriticalControlCardIds` (OA:979) → OA:378 | DE:1327-1337 (id-scoped gate-card detection); CSE:2772, 2780-2787 | **ACTIVE** |
| `startingLocations` | OA:844 | `startingLocationIds/Fragments` (OA:980) → getters OA:511-512 | **NONE** — zero call sites for the six getters or `isHydratedFromJson()` anywhere in `src/` (repo-wide grep 2026-07-24). OA:63-66 comment says "consumers (CardSelection starting-chooser) wire per objective" — never wired | **INFORMATIONAL** |
| `startingEffects` | OA:845 | `startingEffectIds/Fragments` (OA:981) → OA:513-514 | NONE (same grep). The live IWTM starting-effect pick uses the hardcoded V186 slots (OA:238-243 → CSE:7094-7097), not these | **INFORMATIONAL** |
| `startingInterrupts` | OA:846 | `startingInterruptIds/Fragments` (OA:982) → OA:515-516 | NONE | **INFORMATIONAL** |
| `keyCharacterFilter` | OA:847 | `resolveFilter` (OA:1041-1053) → `ObjectivePlaybook.keyCharacter` (OA:1118) | NONE — no live code reads `playbook.keyCharacter` (grep `\.keyCharacter` clean; My Lord arms use `Filters.senator` directly, OA:1149, 1170, 1238; Endor biker_scout "not yet scored" per OA:805-807) | **INFORMATIONAL** |
| `keySiteFilter` | OA:848 | `ObjectivePlaybook.keySite` (OA:1119) | NONE — no reader of `playbook.keySite` (My Lord uses `Filters.Galactic_Senate` directly OA:1157, 1175) | **INFORMATIONAL** |
| `weights.rewardKeyCharAtKeySite` / `.penalizeKeyCharOffKeySite` / `.prioritizeKeyCharDeploy` / `.holdNonKeyCharNoSite` | OA:849 → `buildPlaybookFromProfile` OA:1109-1113 (absent keys default 0, OA:1098-1102) | `ObjectiveWeights` (OA:752-774) read as `mlPb.weights.*` in `getDeployObjectiveAdjustments` (OA:1146, 1160, 1183, 1193, 1241 — V83/-2000, V110/-2000, V108/+500, V88/+1500 magnitudes) | ScoreNotes applied verbatim at DE:1118-1129 (`action.addReasoning`) | **ACTIVE** (My Lord carrier) |
| `weights.deployFlipGateSite` | OA:1114 | `playbook.weights.deployFlipGateSite` | DE:1356-1359 (V193 bonus); CSE:2800-2809 (V193-CS: weight + 1600 offset ≈ dominate); DPP:1646-1653 (V297 formation-plan ranking via `DeployPlanRankingPolicy.evaluateFlipGateFormation`, `models/common/phase/DeployPlanRankingPolicy.java:124-131`) | **ACTIVE** (Endor 400, Invasion 1600) |
| `flipLocationRules` (Invasion only) | OA:853 (loader-extension DTO OA:878-884) | coarse: alternatives' `locationFragments` → `addLocationFragment` (OA:988-997); stored `activeFlipLocationRules` (OA:999) → filter-based `isObjectiveRelevantLocation(PhysicalCard,...)` overload (OA:301-310, fail-closed registry OA:1061-1096) | DE:1227 and CSE:2701 use the filter overload (V136 +200 objective-site path) | **ACTIVE** |
| `actorLocationRules` (Invasion only) | OA:854 (DTO OA:885-897) | `activeActorLocationRules` (OA:1000) → `findFlipGateActorRule` (OA:1017-1028; requires phase=preFlip, purpose=flip, scoreRole=actorToSite) → `hasFlipGateActorRequirement` OA:379-381, `matchesFlipGateActorRequirement` OA:396-416/442-447, `isFlipGateLocation` OA:419-439, `countFlipGateActorsAtLocation` OA:450-471, `hasFlipGateActorAtLocation` OA:474-477, V276 `advancesUnfilledFlipGateActorRequirement` OA:484-509, plus relevance overload OA:311-317 | V297 planner formation plan DPP:682-691 + 753-862; V297 move gate-hold hard veto ME:753-819 (`MoveObjectiveGateHoldPolicy`); DE:1273-1276, 1343-1349 (actor-gating of V193); CSE:2649-2669, 2760-2764, 2796-2799; CSE:1670-1680; pull gate CSE:280-282 | **ACTIVE** |
| `dynamicLocationRules` | OA:855 (DTO OA:898-904) | bound, stored nowhere; no profile carries the key; no consumer | none | **DEAD** (parse-only schema placeholder, per OA:850-855 "step 3+" note) |
| `label` | OA:834 | hydrate log line (OA:1002-1004), playbook display label (OA:1116-1117) | log/reasoning strings only | **INFORMATIONAL** |
| `frontBp` | not a `JsonProfile` field (OA:833-856) | never deserialized (Gson ignores unknown keys, OA header comment 826-827) | none | **DEAD** |
| `backBp` | not bound | — | none | **DEAD** |
| `abbreviation` | not bound | — | none | **DEAD** |
| `side` | not bound | — | none | **DEAD** |
| `objectiveNamedLocations` | not bound | — | none | **DEAD** |
| `sourceCanonicalFile` | not bound | — | none | **DEAD** |

Top-level JSON keys `_comment`, `schemaVersion`, `generatedAt`, `generatedBy`, `validation`: `JsonRoot` binds only `profiles` (OA:857-859) → **DEAD** metadata.

### 3a. `scoreRole` sub-field note

Of the schema's scoreRole vocabulary (`setupLocation | flipProgress | flipGate | stayFlipped` on FlipLocationAlternative OA:875; `keyActor | actorToSite | denyOpponentActor` on ActorLocationRule OA:895), only **`actorToSite`** is ever read at runtime (OA:1022-1023, combined with `phase=="preFlip"` and `purpose=="flip"`). Every other scoreRole / phase / purpose / mode / relation / count / opponentConstraint value is carried but unread, EXCEPT `alternatives[].locationFragments` (coarse hydration OA:991-995) and `locationFilterKey`/`actorFilterKey`/`count` on the matched actorToSite rule (OA:409, 432, 502-504). `sourceText` is explicitly audit-only (OA:876, 896).

## 4. Fact-to-decision summary (what a gap-diff should treat as the live surface)

| Analyzer fact | Fed by (JSON when loaderEnabled, else parser/static) | Decision surface |
|---|---|---|
| `flipConditionLocationFragments` | locationFragments + flipLocationRules coarse + V21 parser | +150/+200 location bonus, V22 plan ordering/score, V136 +200 site bonus, Setup objective match |
| `requiredCardsOnTable` | requiredCardsOnTable (authoritative) + parser | V21 never-pitch protection (CSE:4178+), setup match |
| `pullableCards` | parser ONLY (JSON path disabled) | V21 protection (CSE:4181/4190/4304/4454) |
| `flipCriticalControlSite/Card/CardIds` | flipGateSite/CardName/CardIds (Endor JSON; null elsewhere) | V193 deploy steers (DE + CSE), V297 formation target, PULL.OBJECTIVE.FLIP_GATE_SITE |
| actor-gate rule facts | actorLocationRules (Invasion JSON only) | V297 formation plan, V297 move-hold veto, V276 actor-candidate gating, +300 gate-site pull |
| `activePlaybook.weights` | weights (JSON) or compiled statics | V83/V88/V108/V110 magnitudes, V193/V297 flip-gate bonus |
| identity flags `isInvasion/isMyLord/isEndor/isTdigwatt/isWantThatMap` | title substring in `analyze()` (OA:217-243) — **NOT JSON** | V86/V121 Invasion arms, V83..V110 My Lord arms, TDIGWATT sequencing (DE:690/892/1451/4496), V186 IWTM picks (CSE:1062-1098, 7094-7097) |
| `assessDeployChild` (V214, Endor-only modeling) | requiredCardsOnTable | **log-only** at CSE:1116-1120 — no score consumer yet |

## 5. Bot parity

Both bots share one implementation and identical wiring:

- Facades: `models/rando/strategy/ObjectiveAnalyzer.java` and `models/chosenone/strategy/ObjectiveAnalyzer.java` differ only in package/logger/javadoc (3-hunk diff).
- Top-level wiring is line-for-line identical: instance created at `models/rando/RandoCalAi.java:268` / `models/chosenone/TheChosenOneAi.java:268`; per-decision `setObjectiveAnalyzer` + `analyze()`/`refreshFlipStatus()` + `deployPhasePlanner.setObjectiveAnalyzer` at RandoCalAi:1508-1517 / TheChosenOneAi:1362-1372 (same code, different offsets).
- Consumers: after normalizing the `rando`/`chosenone` package token, `diff` residue is ZERO lines for DeployPhasePlanner, DeployEvaluator, CardSelectionEvaluator, MoveEvaluator, ActionTextEvaluator, PullPolicyAdapter (verified 2026-07-24). No asymmetry found.

## 6. V201 deploy-plan vs objective gate (the Invasion failure surface)

Two stacked surfaces let a plan target (e.g. Swamp) beat an empty flip-gate site (e.g. Theed Palace Throne Room):

**Surface A — the plan itself is chosen on drain/power economics, with only small objective biases.**
- Establish targets are sorted by `theirForceIcons` with just a +50 objective-relevance bias (DPP:1176-1194).
- Whole-plan scoring adds only the +150/+200 `getLocationObjectiveBonus` per target location (DPP:1608-1628 via `DeployPlanRankingPolicy` `models/common/phase/DeployPlanRankingPolicy.java:46-49`).
- The objective's real lever is the V297 flip-gate FORMATION plan (DPP:682-691), scored `scorePlan + weights.deployFlipGateSite` (DPP:1641-1654). But `generateFlipGateFormationPlan` (DPP:753-862) returns null unless ALL of: analyzer has an actorToSite rule (DPP:757-758), the gate site is already a ground location ON the board (DPP:767-772), a matching actor is deployable and an actor+buddy pair is fundable (DPP:787-817), and the V171/V172 contact-viability + ability>=4 check passes when the opponent is present (DPP:820-846). Any failure → no gate plan in the candidate list → `selectBestPlan` (DPP:1664-1711) picks the hottest economic plan (Swamp).

**Surface B — once a plan exists, its target dominates all other destinations at child-selection time.**
- `DeployPlanPolicy.evaluateDestinationTarget` (`models/common/phase/DeployPlanPolicy.java:111-134`): planned target +200 (`deploy-plan-target-match`), non-planned -100 (`deploy-plan-target-other`), and — decisive — a **non-additive DEFER** (`deploy-plan-target-defer`, lines 124-131) on every non-planned destination whenever the exact planned target is offered. Applied at CSE:1491-1512 (only released if the planned target is spy-blocked, the V297.1 fallback at CSE:1496-1508).
- V201 defer semantics: `EvaluatedAction.java:38-42` ("An admissible action or legal Pass always beats DEFER") enforced in `CombinedEvaluator.java:557-575`. Consequence: the ~+2000 V193-CS flip-gate steer (CSE:2804-2812) is score-irrelevant on a deferred action — **the objective-gate offer can only win if the gate is the planned target, or the plan is stale/complete/spy-blocked.**
- Inside plan scoring, the objective formation's only tie-lever is `DEPLOY.FORMATION.OBJECTIVE_TIE_BREAK` +25 (`models/common/phase/DeployPlanPolicy.java:45-49`), and its budget is protected off-plan only by `DEPLOY.BUDGET.OBJECTIVE_FORMATION_RESERVE` -500 (`models/common/phase/DeployBudgetPolicy.java:107-112`), which arms ONLY when the active plan already IS the V297 formation plan (fact computed at DE:1452-1463).

Net: objective facts reach the V201 planner only as ordering/plan-score nudges (+50 / +150-200 / +25) unless the V297 formation plan survives its preconditions; when it doesn't, the plan-target defer at DeployPlanPolicy.java:124-131 (applied CSE:1499-1504) is the exact line where a plan destination beats the flip-gate offer.

## 7. Evergreen gotchas re-confirmed

- `ObjectiveHandler` DO-NOT-WIRE header intact (rando ObjectiveHandler:8-15); V295 retirement comment present in both bot AIs.
- The 43 loaderEnabled-absent profiles are pure ballast at runtime today: their ONLY runtime effect would be via `findProfile` returning them with `loaderOn=false`, which is a no-op (OA:228-232, 257).
- `resolveFilter` registry covers only 5 keys (OA:1043-1052) and `resolveLocationFilter` ~17 keys (OA:1061-1096); unknown keys warn + fail closed (no score), which is the intended no-fabrication behavior for future profile enablement.
