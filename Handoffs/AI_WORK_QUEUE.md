# AI WORK QUEUE — living baton (K-2 ↔ Codex/Alfred)

> FINAL DEPLOY PRECONDITION (Steve, m00497): a fresh FABLE session independently re-verifies Opus's committed diff + invariants + focused/aggregate tests + capture-disabled, reports to Codex, Codex final-gates. NO deploy before both.

Source of truth for "who is doing what next" so NEITHER agent idles waiting for a push message.
Codex's 5-min mailbox monitor + K-2's turn-start both READ this. Update your lane when you change state.
Rule: never "send + wait." Always be executing your standing queue. Pings are non-blocking status drops.

Last updated: 2026-07-13 by Codex. HEAD `85eb0452a` (local only; Opus continuity acknowledgement atop gated Trace 4B1 + F1 + F2). DEPLOY: aggregate HOLD per Steve's direct 'don't deploy anything yet' (m00274).

GATE LEDGER (all ADVANCE/PASS unless explicitly held): Batch-1 corrections e17422f86 PASS; 1.5 purge 66cf11e18; 1.6 224ba9423; 1.7 e447d306d; 1.8 21dda1a67; 1.9 9c9ea3a1c; 2.0 82e4bc6ec; 2.1 c6695168b; 2.2 2a6241edf; 2.3 46e62f4dc; 2.4 13db1dfde; 2.5 2fb22ceba; Stage 4A1 01f821e87; Stage 4A2a 08e544f50; Stage 4A2b f6d00e1da -> 67b285d6d -> 02c2e5fc1 -> 7098f9b33; Stage 4B1 ec886934b ADVANCE-INERT (m00484); F1 5bd89ac68 ADVANCE (m00486); F2 a095db834 ADVANCE (m00487); Stage 4B2 source/council AGREE m00493, RELEASED FOR JAVA m00494 from packet hash `8f5a0438...`, independent implementation gate pending; 2.6 bff87f859; 2.7 0a529f495; 2.8 15b776301; factual repair 978b58e1d; tie 5df276c1b + fixtures 5240f36c6; harness b544ceba6 (harvester); B2 types e4e0aa213 -> fa0f254ac -> d558248cf (inert model clean); registry 631ed4c13 -> 31b9f697c -> f2bb32e95 (migration authority FOR THE 24-ARM TABLE ONLY, 367 live; blanket retirement authority HOLD: 344 arms still lack verified stable markers/named retirement fixtures, per CODEX_DOMAIN_REGISTRY_GATE_F2BB32E95; corrected per m00321); trace no-op 55c22fdde; trace V2 97d2cb65a -> 2b dde6488e0; finalizer F0 fixtures in 92965934b ADVANCE; corrected inert F3 in 4a5e7d6b8 ADVANCE.
RESUME POINT: HEAD `85eb0452a`; Opus confirmed the frozen Fable direction and continuity HOLD in `m00485` and `m00488`. Trace 4B1 `ec886934b`, F1 `5bd89ac68`, and F2 `a095db834` independently advance through gate docs `CODEX_TRACE_STAGE4_4B1_GATE_EC886934B`, `CODEX_FINALIZER_F1_GATE_5BD89AC68`, and `CODEX_FINALIZER_F2_GATE_A095DB834`. Trace 4B2 is the next frozen-order lane. A second Codex continuity audit blocked the first corrected draft before Java because it renamed Fable operations and miscounted six operation kinds as six lexical sites. The revised packet restores the two Fable families, canonical names, seven lexical hooks, exact transition invariants, enum serialization, and seven-method bytecode gate. Opus source agent and council returned AGREE in `m00493`; released packet hash `8f5a0438ffd781cccbec17d72c71884269e788d9ec5206d7b90b2732b13e8022` was sent for one verbatim background agent in `m00494`. Independent implementation gate remains pending. Capture enablement, behavior cutover, deploy, and push stay CLOSED.

DONE additionally: trace 2b dde6488e0 (five m00303 gaps, snapshot v3, 112 tests); cleanup 1.8 21dda1a67, 1.9 9c9ea3a1c, 2.0 82e4bc6ec, 2.1 c6695168b, 2.2 2a6241edf, 2.3 46e62f4dc, 2.4 13db1dfde, 2.5 2fb22ceba, 2.6 bff87f859, 2.7 0a529f495, 2.8 15b776301, and factual repair 978b58e1d ADVANCE; corrected inert F3 4a5e7d6b8 ADVANCE; Stage 4A1 complete-snapshot typed-event slice 01f821e87 ADVANCE; Stage 4A2a outer lifecycle slice 08e544f50 ADVANCE-INERT; Stage 4A2b shared tracker slice through 7098f9b33 ADVANCE-INERT. Finalizer interceptor/runtime lane, enabled capture, later owner families, deployment, and push remain closed.
QUEUED (each with pinned Codex audit + fixtures): Trace 4B2 StrategyController released for one verbatim background implementation agent, then independent Codex gate; ACTIVATE P0 repair (m00299, YesNo results never reach context = stall vector) BATCHED WITH CONTROL repairs (m00304, shared roots); objective adapters (m00300, incl. backside-parser + rematch-reset defects); BATTLE first owner (m00305, incl. V61b wrong-target waiver); PULL facts seam (m00310); deploy-weight consolidation (m00280). SETUP/DRAW contracts filed (m00273).

## PHASE-REORG PROGRAM STATE (2026-07-13)
- Plan: /Users/steve/.claude/plans/no-need-to-overflow-warm-donut.md (13 batches). Steve approved; wide-scope confirmed (m00226).
- Deploy valve: CLOSED. Codex HOLD blocks deploy/cutover only (m00234); building continues.
- Commits awaiting Codex verdicts (sent m00251/m00255):
  • 66cf11e18 Batch 1.5 dead-code purge (CSE+DE, −1686 lines, javap parity PASS: Handoffs/K2_BATCH15_JAVAP_PARITY_2026-07-13.md)
  • b544ceba6 harness comparator fixes per m00241 (13 unit tests, evidence relocation)
  • c497a5df6 Batch-1 corrections per m00225/m00229 (side-aware DeckOracle.getSourceCardFullGameText owner, ALL pull consumers rewired, central here-strip, persona flip exemption, friendly-count gate, changelog bookkeeping)
  • 5df276c1b B0 tie-determinism per m00228 (LinkedHashMap + strict Float.compare, both bots)
  • e5b393955 fixture harness, 587870461 parity report, b94af20e1 B2 design notes, 68ae6c4ff domain registry (gated: usable inventory, HOLD as migration authority — 13 AMBIG rows to resolve before those arms move, m00235)
- GATE LEDGER (2026-07-13 evening): Batch-1 corrections e17422f86 PASS; 1.5 purge 66cf11e18 code ADVANCE; tie 5df276c1b+fixtures 5240f36c6 ADVANCE; trace no-op 55c22fdde ADVANCE (capture HOLD, blockers in CODEX_TRACE_HOOK_GATE doc); B2 scaffold e4e0aa213 inert ADVANCE, hardened fa0f254ac (43 tests, awaiting gate); registry 5e290559c research-ADVANCE/authority-HOLD (rewrite in flight per CODEX_DOMAIN_REGISTRY_GATE doc: V172 +600 factual fix, 21-arm split, FS-enforcement to constraint infrastructure).
- IN FLIGHT (K-2 agents): trace increment 2 (capture blockers + RandoCalAi/DecisionSafety route/finalize observation); registry migration-authority rewrite.
- FILED for later batches: CODEX_PHASE_CUTOVER_ORDER + SETUP/DRAW route audits (batch 5), CODEX_PARENT_CHILD_DEPLOY_PLAN_AUDIT (batch 7 deploy-plan contract).
- SUPERSEDED note: B2 increment 1 — shared immutable FactValue/DecisionFacts/ActionFacts/DecisionSnapshot under ai/models/common/decision/ + pure JUnit construction tests, per Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md landing order step 1. No production consumer.
- K-2 NEXT: trace hooks per Handoffs/CODEX_MINIMAL_DECISION_TRACE_HOOK_2026-07-13.md (EvaluatedAction/CombinedEvaluator choke points, no-op sink default, JUnit seam; gate = no-op run preserves everything); then route observation per Handoffs/CODEX_RANDO_RUNTIME_ROUTE_MAP_2026-07-13.md cutover step 1.
- CODEX OWED: re-gate verdicts on the four commits above; fixture-suite conformance review; Batch-1.5 gate checks 1/5/7 + source half of 6.
- HELD for Steve: Elis Helrot V35.4-vs-abandon ruling; SwccgGameMediator swallow fix (engine file); draw-most vs drain-engine doctrine.

Source of truth for "who is doing what next" so NEITHER agent idles waiting for a push message.
Codex's 5-min mailbox monitor + K-2's turn-start both READ this. Update your lane when you change state.
Rule: never "send + wait." Always be executing your standing queue. Pings are non-blocking status drops.

Last updated: 2026-07-08 by Codex Alfred. HEAD `0813135b5` (local only, not deployed).

## K-2 lane (Java — loader features + flip objectives on)
- 2026-07-10: implementing the remaining 43 dormant objectives (Steve directive, work silent). HEAD had moved to
  e3c071b24 (a later session shipped the V193-CS Endor fix from my handoff — 9496d1f39/ef085e053; V194 tried+reverted).
- DONE `b43105a88` loader EXTENSION step 3b: filter-based objective-relevance scorer. Overload
  isObjectiveRelevantLocation(loc,game,playerId) = title/fragment OR active profile's flipLocationRules/
  actorLocationRules resolved filters .accepts(card). Both deploy-siting callers (both routes) use it. Behavior-neutral
  (0 profiles carry rules → overload == old title check). This is the ENABLER for count-refine/relation objectives.
- WAITING on Codex (m00107): Wave-1 rule DATA (flipLocationRules/actorLocationRules/dynamic for the 15 Bucket-1
  needs-scorer + dynamic rows), source-verified, into the runtime JSON. Then I enable per objective (boundary math).
- CAUTION for enablement: NOT every needs-scorer objective wants plain location relevance. Adversarial ones (TEKWRH
  222_27 "opponent occupies YOUR Hoth" — relevance would make Rando DEFEND it, preventing the flip) need special or
  stay dormant. Enable per-objective on Codex's per-row guidance + boundary math, never blanket.
- NEXT: as Codex delivers Wave-1 rule data → enable straightforward relevance-driven objectives; hold adversarial/
  count-critical for further logic. Then Wave 2+ buckets (captive/stacked/side-aware/hard-veto) each need their own
  extension. resolveActorFilter (actor half of registry) + count/opponent-constraint awareness are later refinements.
- DEPLOYED 2026-07-09: 15 objectives LIVE in web.jar (2b4f0450c), work-verifier 6/6 PASS, switches flipped, HTTP 200.
  Steve testing. Enabled: My Lord, Endor + Dantooine, Ralltiir, Massassi[yavin4], Zero Hour[lothal], Imperial
  Entanglements[tatooine], Twin Suns[tatooine], Old Allies[jakku], They Have No Idea[scarif], Quiet Mining Colony +
  City In The Clouds[bespin,cloud city], Rebel Strike Team[endor], Diplomatic Mission[tatooine,alderaan,dune sea],
  Invasion[naboo,theed palace throne room].
- SCORER FOUNDATION built (behavior-neutral, code-only, NOT redeployed): step 1 DTOs `79ffa74b0`, step 3a coarse
  relevance `a70c67ef6`, step 2 fail-closed location-filter registry `9c1b08c46` (resolveLocationFilter).
- NEXT = step 3b (BEHAVIOR-CHANGING, fresh context + boundary math): wire the flipLocationRules/actorLocationRules
  SCORER — a new `isObjectiveRelevantLocationCard(loc, game, playerId)` path that checks the active objective's rule
  filters (via resolveLocationFilter) against real PhysicalCard locations, consumed at the CharacterDeploySiteEvaluator
  call site, with V136 magnitudes. Then populate the 13 needs-scorer objectives' rules + enable one per rule type
  (Codex pilots: TEKWRH ownership, NMNPND actor-at-site, LU/IO dynamic). Then Buckets 2-5. Let Steve's testing of the
  coarse set inform whether coarse relevance needs the exact-count refinement or is good enough.
- DONE `42e4bd901` Phase 1b(2): Endor hardcoded block commented out (if(false)), fully JSON-driven.
- DONE `37a7bbb1f` Phase 1b(3): My Lord magnitudes read from JSON-built playbook. BOTH pilots complete = template done.
- DONE `4ea8ce474` ENABLE Dantooine BO (7_135) + `0813135b5` ENABLE Ralltiir Ops (7_300): first 2 fixed-planet
  location objectives, locationFragments only (+200 relevance). INTENTIONAL scoring add (not neutral). 4 enabled total.
- DONE: CLEAN FIXED-PLANET SUBSET COMPLETE — 12 objectives live (HEAD 113b8b4c3): My Lord, Endor (pilots) +
  Dantooine, Ralltiir, Massassi[yavin 4], Zero Hour[lothal], Imperial Entanglements[tatooine], Twin Suns[tatooine],
  Old Allies[jakku], They Have No Idea[scarif], Quiet Mining Colony + City In The Clouds[bespin,cloud city].
  Commits 4ea8ce474 / 0813135b5 / f1d4d1947 / 45da6596e / 113b8b4c3 (+ 12a4045fc flag cleanup). All source-verified
  fixed-planet, locationFragments-only, +200 relevance, boundary-mathed, both bots MVN_EXIT=0.
- DONE `79ffa74b0` extension step 1 (parse-only rule DTOs) + `a70c67ef6` step 3a (flipLocationRules locationFragments
  → coarse +200 relevance). Behavior-neutral scaffolding, both bots. Codex schema + registry-key table + rule rows all
  ready (`OBJECTIVE_LOADER_EXTENSION_SCHEMA` + `OBJECTIVE_FILTER_REGISTRY_KEYS`).
- PENDING Codex (m00103): coarse-safe vs needs-scorer split of the count-refine/relation objectives. INSIGHT: many
  count-refine objectives (control multiple FIXED planets, e.g. DMTA tatooine+alderaan) are enable-able NOW with
  profile multi-fragment locationFragments (coarse relevance) — NO scorer needed, like the Bespin pair. Only genuinely
  relational ones (generic-count Y4BO, adversarial TEKWRH, dynamic planets, actor-at-site) need step 3b.
- NEXT: (a) batch-enable Codex's COARSE-SAFE list (source-verified, boundary-mathed) — clears most of count-refine
  without new code; (b) step 3b filter/count/actor SCORER (build fail-closed registry from OBJECTIVE_FILTER_REGISTRY_KEYS
  → wire evaluator reads w/ V136 magnitudes) for the NEEDS-SCORER set — behavior-changing, fresh context, boundary math.
- NEXT (behavior-CHANGING — needs fresh context to meet the no-silent-regression bar; NOT to be rushed):
  Codex build order: step 2 = add registry keys + fail-closed logging (verify each key resolves to a REAL Filters.*
  — search-by-type discipline, ~40 keys); step 3 = wire the flipLocationRules[] SCORER using V136 magnitudes; step 4 =
  actorLocationRules[]; step 5 = enable one per rule type after boundary math (Codex pilot suggestions: TEKWRH
  ownership-aware, NMNPND actor-at-site, LU/IO dynamic planet). Then Buckets 2-5 (captive/stacked/side-aware/hard-veto),
  highest risk, last. Old locationFragments bonus stays until the exact rule scores >= it (dominated, not erased).
- NEXT: build loader EXTENSIONS by category (Codex bucketing the 56 in OBJECTIVE_EXTENSION_BUCKETS). Start with the
  LOCATION/COUNT bucket (biggest, likely a locationFragments + simple occupy/control-count field). Then enable that
  bucket's objectives with boundary math. Hold side-aware (TDIGWATT/Shield) + hard-veto (Verge/Hidden Path/Hunt Down).

- DONE `e22e0b4ab` Phase 1b(1): activePlaybook now BUILT FROM JSON for loaderEnabled objectives (Filter registry +
  buildPlaybookFromProfile + flipGateCardName). Behavior-neutral (Endor +400 from JSON == compiled; only V193 reads
  activePlaybook). Verify ping sent to Codex.
- NEXT (Phase 1b step 2): give hydrate an AUTHORITATIVE mode (clear-then-set for slots the profile provides) so the
  hardcoded blocks that do `.clear()` (e.g. Endor requiredCardsOnTable strips parser-junk conjunction) can be
  commented out without the junk returning. THEN comment out the hardcoded Endor block + swap My Lord consumed reads
  (getDeployObjectiveAdjustments MY_LORD_PLAYBOOK.weights.* + Filters.senator → activePlaybook). Boundary math each.
- THEN: flip loaderEnabled per objective as Codex's boundary table + prescriptive-slot fill clears.

## Codex/Alfred lane (DATA/VERIFY — never blocks on K-2)
- ACTIVE DIRECTIVE from K-2 m00090: per-row table grind is superseded after in-flight batch 11. Produce the shared
  loader-extension bucket doc for all 56 disabled objectives, then use it as K-2's build queue. K-2 starts
  Location/Count while Codex verifies commits and fills bounded data holes.
- DONE 2026-07-08: extension bucket doc for all 56 disabled objectives at
  `Handoffs/OBJECTIVE_EXTENSION_BUCKETS_2026-07-08.md`. Coverage check: 56 rows found, 56 unique, 0 missing, 0 extra.
- DONE 2026-07-08: Bucket 1 fixed-vs-dynamic split at
  `Handoffs/OBJECTIVE_BUCKET1_FIXED_DYNAMIC_SPLIT_2026-07-08.md`. Dantooine `7_135` and Ralltiir `7_300` verified as
  correct fixed-fragment pilots; next fixed-fragment candidates ranked. Dynamic/runtime and relation-first rows flagged.
- DONE 2026-07-08: standardized objective profile enable state on `loaderEnabled` only. Removed inert `rolloutEnabled`,
  `rolloutStage`, and `rolloutNote` from runtime `objective_playbooks.json`; canonical facts now use `loaderEnabled`
  only for `7_135`, `7_300`, `8_167`, and `12_179`.
- DONE 2026-07-08: batch 01 rows 00-02 (`7_135`, `7_136`, `7_137`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_01_ROWS00-02_2026-07-08.md`. Verdict: current runtime profiles are empty/no-op,
  so do not flip them as meaningful JSON-driven objectives until profile-fill/schema gaps are addressed.
- DONE 2026-07-08: batch 02 rows 03-05 (`7_138`, `7_139`, `8_78`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_02_ROWS03-05_2026-07-08.md`. Verdict: current consumed runtime slots are empty,
  while non-consumed named-location facts are present. Hold until K-2 adds source-derived consumed slots plus schema
  for Jedi Test completion, captive Leia, hard-lose, and Rebel scout count logic.
- DONE 2026-07-08: batch 03 rows 06-08 (`9_61`, `10_26`, `12_88`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_03_ROWS06-08_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 adds captive/crossover schema for TIGIH, smuggler/Kessel Run alternatives for WYS, and a PMCTTS-specific
  light-side Senate playbook boundary-checked against ungated V99.
- DONE 2026-07-08: batch 04 rows 09-11 (`12_89`, `13_46`, `14_52`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_04_ROWS09-11_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 adds stacked-card/system-lock semantics for THGSG, Jedi/Dark-Jedi relation schema for WLHT, and
  Amidala/Panaka site-control logic for WHAP. WHAP is not the old title-gated Invasion objective.
- DONE 2026-07-08: batch 05 rows 12-14 (`109_4`, `110_4`, `111_4`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_05_ROWS12-14_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 splits QMC from TDIGWATT's Bespin/Executor assumptions, adds captive-Han rescue state for Profit,
  and adds Yavin 4 count plus post-flip Death Star package semantics for MBO.
- DONE 2026-07-08: batch 06 rows 15-17 (`112_1`, `203_19`, `204_32`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_06_ROWS15-17_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 adds dynamic Rep species for AITC, delivered Stolen Data Tapes plus Rebel-control relations for DMTA,
  and Jakku control/occupy alternatives plus post-flip prevention actions for OA.
- DONE 2026-07-08: batch 07 rows 18-20 (`208_25`, `208_26`, `209_29`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_07_ROWS18-20_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 adds Luke/Jedi battleground relation gates for HITCO, Rebel count/control alternatives for Y4BO,
  and Light Scarif scoring isolated from dark Verge Death Star rules for THNIWRC.
- DONE 2026-07-08: batch 08 rows 21-23 (`210_25`, `211_36`, `215_17`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_08_ROWS21-23_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 adds Credits Will Do Fine stack-count semantics for THGG, Saddle/Luke/Resistance relation logic for
  TGMNAL, and source-derived virtual RTP fields for A Power Loss shutdown plus captive Leia handling.
- DONE 2026-07-08: batch 09 rows 24-26 (`219_48`, `221_67`, `222_27`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_09_ROWS24-26_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 adds Lothal count alternatives for ZH, attached-card and Clone Army X semantics for HFTDG, and Light
  Hoth relation scoring for TEKWRH isolated from dark Shield Will Be Down logic.
- DONE 2026-07-08: batch 10 rows 27-29 (`225_53`, `226_28`, `301_2`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_10_ROWS27-29_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 adds Dagobah/Bespin and Jedi Test semantics for MWYHL, ports existing Hidden Path V52b/V53b/V60/V62/V67aa/V67z
  weights with boundary math, and adds Bespin/Cloud City count/control relations for CITC.
- DONE 2026-07-08: batch 11 rows 30-32 (`7_296`, `7_297`, `7_298`) at
  `Handoffs/OBJECTIVE_BOUNDARY_BATCH_11_ROWS30-32_2026-07-08.md`. Verdict: current consumed runtime slots are empty.
  Hold until K-2 adds captive-state/frozen-captive/hardLose slots for CCT, ports Hunt Down hard-veto and V-tag scoring,
  and adds dynamic Renegade planet plus matching-operative count semantics for IO.
- ON DEMAND (non-blocking): verify K-2's committed diffs when a "verify" ping lands; otherwise keep grinding tables.

## Done
- Objective JSON loader (Phase 0) + neutrality gate (`814ad6664`, `1a3062990`). 58-profile canonical file in,
  id-checked clean. My Lord + Endor loaderEnabled. TDIGWATT V boundary table done (stays disabled pending Bespin split).
