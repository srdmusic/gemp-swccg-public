# Objective Flip Schema Extension Design (for Codex review)

K-2, 2026-07-25. Branch `rando-consolidation-2026-06-23`, HEAD `000cbcf1b` (confirm `git log -1`).
DESIGN PROPOSAL, not an implementation. Consolidates every schema/primitive gap the flip audit found
(`dossiers/*.md`, `analyzer_consumer_map.md` + both addenda, `gap_matrix.json`, `records/`) into one ranked
plan, DEDUPED against the CURRENT engine, which has grown well past the audit baseline.

All `OA:` = `models/common/strategy/ObjectiveAnalyzer.java`, line numbers at HEAD `000cbcf1b`. Dossiers cite
`ee64e6f3b`; their lines have drifted and several are one-to-two commits STALE on what is already built (see
baseline). Cite methods, not just lines, when packeting.

## 1. Principles

- Profile data over title-gated statics: one shared assessor, N JSON profiles. The `isShieldWillBeDown` boolean
  (OA:170, title-gated OA:10288) and the identity flags (OA:138-170) are the anti-pattern to retire.
- Update-in-place per the rulebook plan: extend existing DTOs/consumers, never mint a parallel system.
- Every new arm ships with boundary math: additive scoring means a bigger new magnitude silently dominates an old
  rule. Sandwich the boundary cases before writing code (the discipline that burned Steve 4x).
- Fail-closed registries: an unknown `resolveFilter`/`resolveLocationFilter` key warns and returns null (no score).
  Every new key is a real `Filters.*`, no fabrication.
- Bot parity by construction: rando and chosenone share OA + all `models/common/phase` policies. Land facts once;
  end every packet with a `...DecisionsAreIdenticalAcrossRandoAndChosenone` test.

### CURRENT ENGINE BASELINE (already general at HEAD `000cbcf1b` — do NOT rebuild)

The `flipLocationRules`/`actorLocationRules` engine is no longer Invasion/Endor-only. It already reaches:
- Relations `control|occupy|controlWith|occupyWith|presentAt|at|onTable` (`relationSatisfiedAt` OA:4231, `onTable`
  OA:529/3988). All comparators (`compareCounts` OA:4489). `opponentConstraint` (OA:4277). `referenceController`
  relative counts (OA:3959).
- Back-hold law: `purpose:"flipBack"` and `"stayFlipped"` fully evaluated (`assessPostFlipLocationRisk` OA:3682).
  Audit §7 "no profile expresses the back-side hold law" is SOLVED.
- Opponent-actor absence/blocker leg: `scoreRole:"globalBlockerAbsent"`/`"globalBlockerPresent"`, consumers
  `isPreFlipGlobalBlockerAt` (OA:3031) + battle-removable variant (OA:3042), wired into Deploy/Move/CardSelection/
  Battle, live in the Hunt Down profiles (playbook ~3364). This SUPERSEDES the dormant `ActorLocationRule.absentFrom /
  opponentActorFilterKey / denyOpponentActor` slots (OA:7844-7854), now redundant dead code.
- Required-card gates: `requiredCardDeployRules`, `requiredCardRetentionRules`, and scoreRoles `requiredOnTableCard`,
  `requiredOnTableCardMissing`, `requiredActorMissing`, `requiredCardDeployEnabler`, `requiredCardRetention`.
- Blow-away HARD-LOSS (back-lock only): `HardLossLocationRule` `blownAwayLastStep` -> `objectiveOutOfPlay`
  (`isObjectiveHardLossLocation` OA:6053).
- 24 profiles now `loaderEnabled` (audit measured 15). What is STILL missing is everything in section 2.

## 2. Proposed extensions (ranked by unlock count)

### E1. SCHEMA.COUNTER.PROGRESS_FACT — objective progress counter primitive
- Unlocks (~8): 12_89, 210_25 (stacked on Credits Will Do Fine, need 4); 219_1 (stacked on Thrawn's Art
  Collection, need 2); 10_26 (completed Kessel Runs, need 2); 208_26 (4 Rebels on table); 7_299 (4 ISB agents on
  table, generalizes the existing `countISBAgentsOnTable`); 7_136 (deployed-battleground-system count, need 5+1);
  7_138 (Jedi Test #5 milestone). Also the count-route half of Family A count objectives.
- Blocker: no counter concept in OA. `hasStackedCards` / `hasCompletedUtinniEffect` appear only in card java and
  V35 Hatred, never in OA. resolveLocationFilter reserves this at OA:8170-8176 ("STATE keys ... step 3b").
- Schema (new `FlipLocationAlternative.relation` values + counter descriptor): `{relation:"stackedCount",
  counterHostFilterKey:"Credits_Will_Do_Fine", count:{">=",4}}`; `{relation:"cardCount", actorFilterKey:"Rebel",
  count:{">=",4}}`; `{relation:"completedUtinni", actorFilterKey:"Kessel_Run", count:{">=",2}}` (scoreRole flipProgress).
- Consumer wiring: `isFlipLocationAlternativeSatisfied` (OA:3933), add the three `relation` branches reading
  `game.getModifiersQuerying()` (`getStackedCards(host).size()`, `Filters.countActive(cardFilter)`,
  `hasCompletedUtinniEffect`). `countAlternativeMatches` (OA:3984) already dispatches per relation. Counter-host
  bp into `requiredCardsOnTable` (OA:966) for free V21 never-pitch preservation (the cheapest correctness win).
- Dominance/boundary: pre-flip progress bias must be a location/pull nudge on the +150/+200 (`locationFragments`)
  and +300 (`PULL.OBJECTIVE.FLIP_GATE_SITE`) tier, NOT a veto. It fights nothing existing (net-new relation).
- Test contract: `stackingFourthCardOnCreditsWillDoFineFlipsToBack`, `threeStackedIsNearMiss`, `forceLossNeverPitchesTheCounterHost`.
- Effort: M (one shared fact, three event sources; twins 12_89/210_25 build once).

### E2. SCHEMA.CAPTURE.STATE_FAMILY — capture / captive / escort primitives
- Unlocks (6): 9_151, 9_61, 7_296, 110_6, 215_17, 7_139.
- Blocker: no capture/captive/escort/`movedToLocationBy`/`isDeathStarPowerShutDown` reader anywhere in `models/**`.
  Only lever is blind `KeywordWeight("capture",45)` (RandoCalAi:202). `actorToSite` models control-occupancy, cannot
  express capture-STATE.
- Schema (new relations on `FlipLocationAlternative`): `captiveAt` (`{actorFilterKey:"Luke",
  locationFilterKey:"Jabbas_Palace_site", count, includeCaptive:true}`), `captiveCount`, `escortMovedTo`
  (`{actorFilterKey:"frozenCaptive", locationFilterKey:"Audience_Chamber"}`), plus a `globalFlag`
  (`isDeathStarPowerShutDown`) boolean leg. Add profile `invertPreservation:true` for 9_61 (owner WANTS Luke captured).
- Consumer wiring: `isFlipLocationAlternativeSatisfied` + a MoveEvaluator branch for the escort-move flip; feed the
  survival persona into `ObjectiveHardLossPolicy` (7_139: Leia loss = objective out of play) reusing
  `RequiredCardRetentionRisk`. The move-event and out-of-play terminals reuse existing hard-loss shapes.
- Dominance/boundary: the 9_61 inversion is the trap, a generic "protect my character" penalty must be SUPPRESSED
  (sign-flipped) when the objective wants Luke captured; prove the preservation rule does not veto the intended
  capture. Capture-drive bonus tuned above `KeywordWeight("capture")+45` but below hard vetoes.
- Test contract: `vaderPresentWithFreeLukeAutoCapturesAndFlipsToBack`, `capturedByAnyMeansFlipsWithoutVader`,
  `ownLukeCaptureIsFlipProgressNotAThreat` (9_61), `losingLeiaPlacesObjectiveOutOfPlay` (7_139), `imprisonedLeiaDoesNotOccupy` (215_17).
- Effort: L (five sub-shapes; canonical-heavy, high value).

### E3. SCHEMA.STATE.BLOWAWAY_FLIP — proxy-route generification (blow-away FLIP direction)
- Unlocks (~6): 8_78 (Bunker blown away, anyOf with the 3-site route), 111_6 (Alderaan system blown away), 501_94
  (Bunker; needs a new profile), 225_32 (Tracked Fleet annihilated); plus REFACTORS 222_14/222_30 off the title gate.
- Blocker: the FLIP direction of blow-away is bespoke and title-gated: `isShieldWillBeDown` (OA:170/10288) plus the
  hardcoded rule-id `"shield-will-be-down-virtual-in-range-route"` (OA:4072) and bp checks 222_13/209_42. Hard-loss
  already reads `blownAwayLastStep` (OA:6053) but there is no blow-away FLIP relation.
- Schema: new relation `{relation:"blownAway", targetFilterKey:"Bunker", mode:"lastStep|state", scoreRole:"flipGate"}`.
  `mode:"state"` = `GameConditions.isBlownAway` (225_32 polls persisted state); `mode:"lastStep"` = the event. Migrate
  the Shield in-range pursuit rule onto this relation + the existing Shield `requiredCardDeployRules`, dropping the
  `isShieldWillBeDown` boolean.
- Consumer wiring: `isFlipLocationAlternativeSatisfied` (OA:3933) add the `blownAway` branch; expose `isBlownAway`
  as a readable analyzer fact (mirrors `isObjectiveHardLossLocation`). Delete the `isShieldWillBeDown` title gate
  once its data lives in the Shield profile.
- Dominance/boundary: the pursuit steer (deploy Target The Main Generator, position the Cannon in range) must keep
  the current Shield magnitudes; regression-test 222 before/after the de-title-gating so behavior is identical.
- Test contract: `bunkerBlownAwayFlipsRebelStrikeTeam`, `alderaanBlownAwayFlipsSetYourCourse`,
  `shieldRouteBehavesIdenticallyAfterDeTitleGating` (222 golden), `trackedFleetAnnihilatedStateFlipsFirstOrderReigns`.
- Effort: M (one relation + a title-gate retirement + one golden regression).

### E4. SCHEMA.FLIP_ALT.SUB_LEGS — nested allOf inside an anyOf alternative
- Unlocks (4 directly, plus Family D count legs): 204_32 (control system + occupy 2 sites) OR (occupy system +
  control 2 sites); 109_4 (Lando/Lobot-on-any-CC-site + control 1 CC site reduced branch); 112_1 / 112_15 (3-site +
  species-alien branch vs 2-site branch).
- Blocker: a flat `alternatives` list under one `mode` cannot express anyOf-of-(2-leg allOf) when the branches pair
  DIFFERENT relations on DIFFERENT location classes. Not flattenable.
- Schema: `FlipLocationAlternative.subLegs : List<FlipLocationAlternative>` (one level), evaluated as an inner allOf.
  Precedent-shaped: mirrors how `opponentConstraint` already nests a sub-condition on an alternative.
  `{ "mode":"anyOf", "alternatives":[ {"subLegs":[control Jakku_system, occupy Jakku_battleground_site >=2]},
  {"subLegs":[occupy Jakku_system, control Jakku_battleground_site >=2]} ] }`.
- Consumer wiring: `isFlipLocationAlternativeSatisfied` (OA:3933): if `subLegs != null`, return allOf over subLegs
  (recurse). `countAlternativeMatches` (OA:3984) unaffected for flat legs. Add registry keys `Jakku_system`,
  `Jakku_battleground_site`, `battleground` (loc), `Lando`, `Lobot`, `Bespin_location`, `Bespin_Cloud_City`,
  `Bespin_cloud_sector` (resolveFilter/resolveLocationFilter OA:8048/8177).
- Dominance/boundary: additive-safe; a satisfied nested branch feeds one flip-progress fact, same magnitude as a
  flat leg. No existing arm fights it.
- Test contract: `flipsViaControlSystemPlusOccupyTwoSites`, `flipsViaOccupySystemPlusControlTwoSites`,
  `doesNotFlipWithOnlyOneBranchLegSatisfied`, `landoOnAnyCloudCitySiteReducesRequiredCountToOne`.
- Effort: S (one DTO field + one recursion + registry keys).

### E5. SCHEMA.CHOSEN_STATE.DERIVED_ACTOR — chosen-state / dynamic-actor fact
- Unlocks (4): 112_1 / 112_15 (Rep species read from the setup-chosen Rep's blueprint), 7_138 (apprentice = Luke or
  Leia under Daughter Of Skywalker retarget), 7_136 (stacked hidden-base indicator identity). (10_29 Rey/Anakin
  retarget resolves inline at flip-eval, no field.)
- Blocker: `DynamicLocationRule` DTO exists (OA:7857, source `subjugatedPlanet|renegadePlanet|repSpecies|setupChoice`)
  but is Gson-bound and DEAD, no consumer. No code reads objective `WhileInPlayData` for a chosen card.
- Schema: wire the existing `DynamicLocationRule` (do not add a parallel): `{ "source":"repSpecies",
  "matchingActorFilterKey":"non_unique_alien_of_rep_species" }` where the analyzer resolves the filter at flip-eval
  from `self.getWhileInPlayData().getPhysicalCard().getBlueprint().getSpecies()`. Apprentice = `source:"setupChoice"`
  reading the game-text-modification retarget.
- Consumer wiring: build the `DynamicLocationRule`/derived-actor resolver as the step-3b STATE hook the registry
  comment reserves (OA:8170-8176); the resolved `Filters.*` then flows into the normal `occupyWith`/`at` legs.
- Dominance/boundary: none new, the derived filter feeds an existing relation. Guard the null-Rep case
  (112_1 returns null and NEVER flips when no Rep chosen, the fact must fail closed, not default-true).
- Test contract: `chosenRepSpeciesIsClassifiedAsDynamicFlipActor`, `repUnsetMeansObjectiveNeverFlips`,
  `daughterOfSkywalkerRetargetsApprenticeToLeia`.
- Effort: M (one resolver on a reserved DTO; 112 twins build once).

### E6. SCHEMA.STATE.CARD_STATE_HOOKS — step-3b delivered / attached / hit
- Unlocks (4): 203_19 (Stolen Data Tapes `delivered` keyword state), 221_67 (Effect attached to the objective),
  211_26 (Insidious Prisoner attached to an Invisible Hand site), 213_32 (character just `hit` by your card, event).
- Blocker: `delivered`/`hasAttached`/`isAttachedTo`/`justHitBy` appear only in card java, never in `models/**`.
  resolveLocationFilter reserves `delivered` for step 3b (OA:8170-8176).
- Schema: new relations `{ "relation":"cardStateActive", "targetFilterKey":"delivered_Stolen_Data_Tapes" }`,
  `{ "relation":"attachedTo", "trackedCardFilterKey":"Insidious_Prisoner", "targetFilterKey":"Invisible_Hand_site" }`
  (targetFilterKey = the objective self for 221_67). 213_32 route A is a hit-EVENT signal, not a standing state.
- Consumer wiring: `isFlipLocationAlternativeSatisfied` for the two state relations; the tracked card into
  `requiredCardsOnTable` (retention). The hit-event (213_32 route A) belongs in ActionText/BattleDecision fire logic
  (V36/V51), objective-identity-gated so "hitting a character = flip progress" never leaks.
- Dominance/boundary: 211_26 null-guard (prisoner gone = back frozen, NOT flip-back) means retention-critical, not
  flip-back exposure. 213_32 end-of-turn oscillation nudge must never dominate real battle economics, hard boundary.
- Test contract: `deliveredTapesStateIsFlipBottleneck`, `prisonerDetachedFromInvisibleHandSiteFlipsBack`,
  `prisonerLeavingTableFreezesTheBack`, `attachedGWRAHIsFlipCriticalState`, `firingBlasterAtCharacterIsValuedAsFlipProgressRouteA`.
- Effort: M-L (delivered + attached share a hook; hit-event/oscillation is the highest-uncertainty piece, may split).

### E7. SCHEMA.FLIP_RULE.PHASE_WINDOW — phase qualifier on flip / flip-back rules
- Unlocks (3-4) and TIGHTENS shipped profiles: 8_78 (flip only during your move phase), 219_1 (flip only during a
  deploy phase, either player), 213_32 route B (your battle phase); tightens 7_297/213_31 where windows exist.
- Blocker: `FlipLocationRule` has no phase-window field; `isActivePreFlipRule` ignores game phase.
- Schema: `FlipLocationRule.phaseWindow : { "phase":"MOVE|DEPLOY|BATTLE", "player":"self|either" }` (optional; absent
  = any phase, current behavior).
- Consumer wiring: gate `isActivePreFlipRule` and the post-flip risk pass on `game.getGameState().getCurrentPhase()`.
- Dominance/boundary: purely restrictive, narrows when a rule counts as satisfiable, cannot inflate a score. Verify
  it does not suppress the standing deploy-steer (steer toward the gate is fine outside the window; only the
  flip-imminent fact is windowed).
- Test contract: `doesNotFlipOutsideDeployPhase` (219_1), `rebelStrikeTeamFlipsOnlyDuringYourMovePhase`.
- Effort: S (one optional field + one phase check).

### E8. SCHEMA.ABSENCE_LEG — opponent-blocker profiles (DATA ONLY; consumer already built)
- Unlocks (3): 208_25 (no opponent character ability>4 at any battleground), 10_29 (no opponent Luke at a
  battleground), 208_57 (no Resistance Agent present at a battleground site).
- Blocker: NOT a schema gap anymore. The `globalBlockerAbsent` consumer exists (OA:3031, playbook ~3364, Hunt Down).
  Missing is only: `loaderEnabled` + author the `globalBlockerAbsent` alternative + a few registry keys
  (`Resistance_Agent`, `Xizor`, and an attribute-threshold filter `opponent_character_ability_over_4` =
  `Filters.and(character, abilityMoreThan(4))`).
- Schema: mirror the Hunt Down block, `{ "relation":"at", "controller":"opponent", "actorFilterKey":"...",
  "count":{"==",0}, "scoreRole":"globalBlockerAbsent" }`.
- Consumer wiring: none new. `resolveFilter` (OA:8048) gains the actor keys.
- Dominance/boundary: the ability>4 predicate is the only real add; confirm `abilityMoreThan` reads modified ability.
- Test contract: `jediAtBattlegroundButOpponentAbilityFiveDoesNotFlip`, `xizorAtBattlegroundButOpponentLukeBlocksFlip`.
- Effort: S. NOTE: the `ActorLocationRule.denyOpponentActor / opponentActorFilterKey / absentFrom` dead slots
  (OA:7844-7854) are now REDUNDANT with `globalBlockerAbsent`, recommend deleting them, not wiring them.

### E9. SCHEMA.FLIP.VOLUNTARY_DECISION — elective-flip decision policy
- Unlocks (3): 211_36 (high-stakes: flip trades Luke out of play for the Resistance engine, battle-timed optional),
  225_53 (low-stakes: voluntary top-level flip, Luke returns to hand, no flip-back, no hard-lose), 7_136 (degenerate
  zero-cost optional flip once its counter clears).
- Blocker: no voluntary/optional-flip decision consumer exists (`grep voluntary|mayFlip|flipDecision` clean). All
  current flip rules assume automatic/standing-state flips.
- Schema: profile `voluntaryFlip : { "trigger":"topLevel|battleInitiated", "costActorFilterKey":"Luke",
  "costOutcome":"placeOutOfPlay|returnToHand|none" }` + objective-identity gate.
- Consumer wiring: a new `models/common/phase` policy (co-designed for both stakes) that scores WHETHER to take an
  offered optional flip from cost-vs-benefit; objective-identity-gated like the isTdigwatt/isInvasion arms so it
  never leaks. Reads back-engine readiness (2+ unique Resistance chars for 211_36; Cloud City drain set for 225_53).
- Dominance/boundary: the one-way permanent cost (211_36 Luke out of play) means the policy must value Luke on board
  vs back readiness; DO NOT flip while Luke is the best board piece. 225_53 leans flip-positive (reversible-ish).
- Test contract: `declinesFlipWhileLukeIsStillHighValueOnBoard`, `takesFlipOnceBackEngineAssembled`,
  `voluntaryFlipReturnsLukeToHand` (225_53).
- Effort: M (one policy, two stakes profiles).

### E10. SCHEMA.DYNAMIC.HOST_SYSTEM — moving flip target
- Unlocks (1-2): 225_32 (Tracked Fleet's flip driver is "annihilate at start of your turn IF you CONTROL the
  [EpVII] system the fleet currently sits on"; the fleet moves between systems). Future movable-gate objectives.
- Blocker: `flipGateSite` is a single static string; it cannot follow a card that relocates. No dynamic-host resolver.
- Schema: reuse `DynamicLocationRule` (OA:7857) with `source:"hostOfCard"`, `derivedLocationFilterKey` resolved at
  runtime to the current top-location of `Filters.Tracked_Fleet`. Pairs with E3 (the blow-away/annihilate flip).
- Consumer wiring: same derived resolver as E5; the resolved location flows into the normal `control` leg + the
  `actorToSite` flip-gate machinery.
- Dominance/boundary: 225_32 is on the inversion list (E3/NON-GOALS). The fleet MUST be annihilated to flip; never
  protect it. The control leg targets the fleet's host, the annihilation is the flip event.
- Test contract: `controlOfTrackedFleetHostSystemEnablesAnnihilationFlip`, `fleetRelocationMovesTheGateTarget`.
- Effort: M (one dynamic resolver, shared with E5).

## 3. Sequencing proposal (waves)

Steve's library skews canonical (Invasion done); prioritize canonical objectives (Hoth/Shield, Bespin, Tatooine,
Yavin, Death Star rescue, Jabba capture, Dagobah training) over obscure playtest. Each wave rides the cheap
data-only profiles that need no new engine.

- Wave 1 (data-only, zero new engine): E8 absence-leg (208_25/10_29/208_57). Ride-alongs on the shipped Family A
  count engine: 226_12 (Bespin count+opponent-fewer), 201_39 (Tatooine count), 111_4 (Yavin count), 601_29 (Wookiee
  slavers), 301_2/301_4 finish. Retire the dead ActorLocationRule slots (E8 note).
- Wave 2 (E4 subLegs, S, then E5 chosen-state, M, paired): 204_32, 109_4 first (subLegs only); then 112_1/112_15
  and partial 7_138 (subLegs + derived-actor). Family A count ride-alongs continue.
- Wave 3 (E1 counter, M + E7 phase-window, S + E9 voluntary, M): 12_89/210_25 (twins), 219_1 (counter + phase-window),
  208_26, 7_299, 10_26, 7_136, 7_138 (finish); 211_36/225_53 voluntary; phase-window tightens shipped Hunt Down/8_78.
- Wave 4 (E3 blow-away + E10 host, M, paired): 8_78, 111_6, 501_94, refactor 222_14/222_30 off the title gate;
  225_32 (blow-away + moving host).
- Wave 5 (E6 card-state, M-L): 203_19 (delivered), 221_67 + 211_26 (attached); 213_32 hit/oscillation LAST or split.
- Wave 6 (E2 capture, L): 9_151, 9_61, 110_6, 215_17, 7_139, 7_296. Canonical-heavy, highest value; most boundary
  care (the 9_61 preservation inversion).

## 4. Explicit non-goals

- No engine or card-code edits. Every fix is AI-side (OA + `models/**` + `objective_playbooks.json`).
- Do NOT encode printed-text exceptions the CARD CODE does not implement (the 601_146 lesson: its printed "unless
  two Kessel Runs" flip-back exception is uncoded; same for 501_94 draw-phase retrieval and 601_087). Model the CODE.
- Do NOT protect flip-targets that must DIE to flip (inversion list): Tracked Fleet (225_32/501_60), Alderaan system
  (111_6), Bunker (8_78/501_94), Main Power Generators (222_14/222_30). Wire them as flip drivers, never never-pitch.
- Do NOT wire the dormant `ActorLocationRule` absence slots; `globalBlockerAbsent` already owns that job.

## 5. Open questions for Codex

1. subLegs (E4): is one nesting level enough, or does any objective need anyOf-inside-allOf-inside-anyOf? Audit found only one level.
2. Blow-away de-title-gating (E3): retire `isShieldWillBeDown` in the same packet as the new relation, or ship the relation first and cut the title gate once 222 golden parity is proven?
3. Counter primitive (E1): three relations (`stackedCount`/`cardCount`/`completedUtinni`) or one `progressCount` with a `source` discriminator (mirroring `DynamicLocationRule.source`)?
4. Chosen-state (E5) + host-system (E10): one shared runtime resolver on `DynamicLocationRule` or two? Both derive a filter from runtime card state; one resolver is DRY but couples species/apprentice/host logic.
5. Voluntary-flip (E9): objective-identity-gated policy (like the Invasion/My Lord arms) or a general `voluntaryFlip` field? Identity-gating is safer but does not scale to 501-set cards.
6. Hit-event / end-of-turn oscillation (213_32, E6): model at all, or leave to the engine and only preserve the enablers? Highest design-uncertainty item; propose deferring to a spike.
7. E8 dead slots: delete `ActorLocationRule.absentFrom/opponentActorFilterKey/denyOpponentActor`, or leave a `// SUPERSEDED by globalBlockerAbsent` comment per the comment-out-old-rules norm?
