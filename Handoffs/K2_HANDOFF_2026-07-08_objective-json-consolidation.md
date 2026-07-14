# K-2 HANDOFF — 2026-07-08 — Objective JSON consolidation (generic playbook, one data source)

Entry point for the next K-2. Onboard via MEMORY.md (feedback_* = law), `.claude/CLAUDE.md` (K-2SO voice, concise),
`resources/BUILD_AND_DEPLOY.md`. Then this file.

## The directive (Steve, 2026-07-08)
Link ALL objective data into ONE JSON file; make ObjectiveAnalyzer a POINTER that reads it and drives GENERIC scoring
per category for whatever objective Rando is playing. Old per-objective scoring gets commented out (NOT deleted) —
but LAST, only after the JSON path is proven behavior-equivalent. Work planned with Alfred (Codex); K-2 executes.
Steve wants NO progress text / plan detail in his session (token budget); notify him ONLY when the whole plan is done.

## Agreement with Alfred (mailbox m00060/m00061/m00062/m00063)
Order is fixed: **centralize data → wire generic slot → boundary math + log proof → comment old block LAST.**
NO blanket comment-out. Hard-veto / side-aware objectives (TDIGWATT base-vs-V + HOLD_BACK, On The Verge / Hidden Path
flip-back vetoes, Hunt Down Vader gate, senator −2000 block) keep their specific guards until the schema expresses them.
- CODEX owns DATA: produce the single canonical `objective_playbooks.json` (58 profiles), merge shards, canonical field
  names, self-verify ids. Scan the CURRENT analyzer for hardcoded data points lacking a JSON column → add a column.
- K-2 owns JAVA: loader, slot hydration, generic wiring at existing consumer sites, boundary math, mirror chosenone,
  both changelogs, work-verifier, in-container compile. Comment out old blocks LAST.

## LIVE STATE: see `Handoffs/AI_WORK_QUEUE.md` (updated continuously). As of HEAD `45da6596e`: both pilots fully
## JSON-driven + 8 fixed-planet location objectives ENABLED (10 total live). Loader complete; enable flag =
## loaderEnabled. Next = Bespin pair (careful pass) then loader EXTENSIONS (flipLocationRules/actorLocationRules/
## dynamicPlanet) for the count-refine/relation/dynamic sub-buckets, then Buckets 2-5. Details below are historical.

## State (HEAD `1a3062990`, local only, NOT deployed)
Prior commits this session: `a7deb7ae3` Endor playbook consolidation, `cd4ce3e5d` V193 Bunker-gate id fix,
`814ad6664` JSON loader Phase 0, `1a3062990` **loader neutrality FIX (gate to verified profiles + defer pullableCards)**.

**Phase 0 DONE + neutrality FIX (committed; work-verifier NOT yet run on the loader):**
- `src/gemp-swccg-server/src/main/resources/objective_playbooks.json` = the ONE runtime source (bundled in jar).
  **Codex delivered the full 58-profile canonical file** (landed in 814ad6664). id-checked: 58 profiles, 288 ids,
  0 unresolved. filterKeysUsed = {senator, Galactic_Senate, biker_scout, Bunker} (exactly the planned registry).
  Codex's descriptive evidence file: `resources/Objective_Playbook_Facts_2026-07-08.json` (untracked).
- `ObjectiveAnalyzer.java` (both bots): Gson loader (nested JsonRoot/JsonProfile/JsonCardRef), lazy thread-safe
  `profiles()`, `findProfile(bpId,title)` (id first, title 2nd), `hydrateFromProfile()` ADDITIVE+idempotent, hard
  fallback to the text parser. NEW setup slots startingLocation/Effect/Interrupt Ids+Fragments (+getters, reset()).
  `analyze()` calls hydrate AFTER parseGameText, **GATED on `JsonProfile.loaderEnabled == true`**.
- **CRITICAL GATE:** only profiles with `"loaderEnabled": true` hydrate. Today = My Lord + Endor ONLY (both
  boundary-verified: My Lord hydrates only empty lists → no-op; Endor writes byte-identical duplicates of the
  hardcoded block). The other 56 profiles exist but are DISABLED → text parser stands. **Flip loaderEnabled per
  objective ONLY after boundary math proves equivalence.**
- `pullableCards` hydration is COMMENTED OUT (deferred) — it adds pull targets the parser didn't, not neutral.
- Behavior is unchanged. NOTHING replaced yet (no hardcode commented out).

## Field contract (per profile in objective_playbooks.json)
label; blueprintIds[]; titleFragments[]; locationFragments[] (→ addLocationFragment → isObjectiveRelevantLocation
+200/site); requiredCardsOnTable[]; pullableCards[]; flipGateSite (str|null); flipGateCardIds[] (EXACT bp ids whose
deploy is gated on flipGateSite — Endor {207_25,207_025,601_260}, base 8_124 EXCLUDED); startingLocations/Effects/
Interrupts[] ({blueprintIds[],titleFragments[],sourceVtag?}); keyCharacterFilter/keySiteFilter (Filters registry key);
weights{} (named floats REUSING existing V-tags). Loader ignores extra descriptive fields (Gson).

## Next steps (in order)
1. **Phase 1b — make analyzer a real pointer for My Lord + Endor (behavior-preserving), then comment out hardcode.**
   - Build a curated string→Filter registry (senator, Galactic_Senate, biker_scout, Bunker, Neimoidian, pilot,
     capital_starship, …). Resolve keyCharacterFilter/keySiteFilter from JSON.
   - Build a hydrated ObjectivePlaybook from the JSON weights; set `activePlaybook` from it; switch the CONSUMED
     reads (My Lord getDeployObjectiveAdjustments MY_LORD_PLAYBOOK.weights.* + Filters.senator/Galactic_Senate;
     Endor DeployEvaluator ENDOR_PLAYBOOK.weights.deployFlipGateSite) to the hydrated playbook. Boundary-math each
     (values must equal the compiled statics). THEN comment out the hardcoded Endor block in parseFlipCondition and
     the compiled MY_LORD/ENDOR statics. Add `flipGateCardName` to the JSON+loader before commenting Endor (the
     hardcoded block sets flipCriticalControlCard for the log/reasoning display).
2. **Phase 2 — TDIGWATT V 226_12** (side-aware Bespin, base 109_12 vs V 226_12 do NOT merge; HOLD_BACK; high risk).
   Boundary math FIRST. Read Card226_12 + Card109_12 source.
3. **Phase 3 — Shield Will Be Down 222_14/222_30** (setup deploys, Hoth, Target The Main Generator, AT-AT Cannon,
   hard-lose/stay-flipped guard; add missing back-side OOP guard V160 only covers front).
4. **Phase 4 — batch the rest** once Codex's canonical 58-entry file lands. Candidate for a Workflow (ultracode on):
   fan out per objective — read card source → author profile fields → boundary math vs old block → verify. Comment
   out each old block only after its profile is proven equivalent.

## Coordination
Mailbox `~/claude-codex-mailbox/mailbox.py {check --as claude --mark, wait --as claude --mark --interval 120 (bg),
send --from claude --to codex ...}`. Codex delivering canonical `objective_playbooks.json` (58) — when it lands, id-check
it (`/private/tmp/.../scratchpad/idcheck.py` broadened to scan all id-shaped strings) then point the loader at it.

## Landmines
- Additive/idempotent hydration ONLY until boundary-math'd; the moment you comment out a hardcoded block, hydration
  becomes the sole source — prove equality first.
- Typed Filters must stay REAL Filters.* (search-by-type rule); no lore-token string guessing for keyCharacter.
- `getBlueprintId(true)` format vs JSON ids: hedge both pad forms where runtime padding varies (207_25/207_025).
- Mirror EVERY analyzer change to chosenone same session (loader is package-agnostic; both read the same resource).
- Never push, never deploy mid-game. Both changelogs same session. work-verifier before "done".
