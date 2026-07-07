# K-2 HANDOFF — 2026-07-07 (late) — ObjectivePlaybook build

You are **K-2** on GEMP-SWCCG at `/Users/steve/gemp-swccg-public`. Prior session ran low on context
mid-build. This is your entry point.

## Onboarding (do first, in order)
1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — auto-loads; `feedback_*` = law.
2. THIS FILE (state + the build job).
3. `.claude/CLAUDE.md` — persona (K-2SO from Rogue One; Steve = Steven Davis, GEMP `asdf`, SWCCG expert,
   ADHD+dyslexia). **Voice: concise, deadpan, single-layer, tables > prose, no em-dashes, push back when
   he's wrong, no preamble. Do NOT over-explain — a prior session bloated context with verbose status reports.**
4. `resources/BUILD_AND_DEPLOY.md` before any edit/deploy.
5. Context: Rando is the SWCCG AI bot (`ai/models/rando/`, mirror `chosenone/`). Scoring is ADDITIVE
   (CombinedEvaluator sums per action, max wins, Pass ~5-8, BAD_ACTION_THRESHOLD -100). Old rules get
   DOMINATED not deleted. `AI_CHANGELOG.md` = grep by V-tag, don't read whole.

What we're building here: the objective-playbook consolidation. Detail below.

## The job
Steve's ruling: **ObjectiveAnalyzer owns objective identity + typed facts + scoring WEIGHTS**;
DeckOracle = feasibility only; evaluators consume the playbook at EXISTING call sites (ordering unchanged);
ObjectiveHandler.java is DEAD/stale — never revive. Wire the 58 objectives into typed ObjectivePlaybooks
one at a time, behavior-preserving, boundary math before any magnitude change, mirror chosenone, both changelogs.

## State (HEAD `699b45876`, local only, NOT deployed)
- **Pilot #1 My Lord (12_179): DONE + work-verifier PASS.** THE TEMPLATE. See ObjectiveAnalyzer.java:
  nested types `NamedCardRef` / `ObjectiveWeights` / `ObjectivePlaybook`, static `MY_LORD_PLAYBOOK`,
  `activePlaybook` field (set in analyze() ~L177, reset in reset() ~L774), `getActivePlaybook()`.
  The 4 My Lord deploy magnitudes in `getDeployObjectiveAdjustments` read `MY_LORD_PLAYBOOK.weights.*`.
  Copy this shape for every objective.
- **Normalized facts (machine-readable, id-verified, zero id errors):**
  - Pilots (My Lord + Endor): `resources/Objective_Playbook_Facts_2026-07-07.json` (Codex, verified GO).
  - K-2 rows 29-57: `resources/Objective_Playbook_Facts_K2_rows{29-39,40-57}_WIP.json`.
  - Codex rows 0-24: `resources/Objective_Playbook_Facts_Codex_rows*.json` (+ `_Codex_WIP.json`). Codex doing 0-39.
- Schema: `resources/Objective_Normalization_Schema_2026-07-07.md`. Plan: `Handoffs/OBJECTIVE_ANALYZER_PLAYBOOK_PLAN_2026-07-07.md`.

## Next pilots (Steve's order): Endor 8_167 → TDIGWATT V 226_12 → Shield Will Be Down 222_14/222_30
- **Endor 8_167** (facts verified GO_WITH_FIXES): consolidation = build `ENDOR_PLAYBOOK` (identity 8_167/_BACK;
  flip cards Establish Secret Base {8_124,207_25,601_260} + Ominous Rumors {8_127,223_19,601_261}; Bunker flip-gate).
  V193 already lives analyzer-owned (getFlipCriticalControlSite/Card + DeployEvaluator +400 Bunker steer ~L1900) —
  move the +400 into ENDOR_PLAYBOOK.weights, behavior-preserving. **FIX:** keep the Bunker steer scoped to the
  Establish Secret Base (V) 207_25 path — base 8_124 gates on 3 Endor sites, NOT Bunker; do NOT universalize.
  **NEW (needs Steve's boundary math first, NOT consolidation):** back-side (Imperial Outpost) drain protection —
  hold a biker_scout / piloted AT_ST at drained Endor sites; nothing scores the back side today.
- TDIGWATT V 226_12: side-aware Bespin/Cloud City, base(109_12) vs V(226_12) split — do NOT merge. High risk.
- Shield Will Be Down 222_14/222_30: add missing back-side OOP-risk guard (V160 only covers front flip).

## Coordination (both auto-wake, no Steve relay)
- Mailbox: `python3 ~/claude-codex-mailbox/mailbox.py {send,check --as claude --mark,wait --as claude}`.
  `wait` (I added it) blocks until mail — run `wait --as claude --mark` as a BACKGROUND bash so the harness
  re-invokes you on Codex mail. Codex has a 5-min heartbeat monitor for your mail.
- Model: Codex = data (normalize → WIP files, zero id errors, self-verify). K-2 = verify + write Java.
  Do NOT re-verify every objective 1-by-1 — spot-check chunks with a deterministic id-check (every id must
  resolve in card_blueprint_database_{dark,light}.json OR a Card*.java file) + occasional agent verify.

## Build/deploy: compile in-container (`gemp_swccg_app_1`, `mvn -q -pl gemp-swccg-server -am compile`, real
MVN_EXIT), mirror rando→chosenone (clone ObjectiveAnalyzer, `models.rando`→`models.chosenone`), both changelogs
(`resources/AI_CHANGELOG.md` + `.../02-rando-history/AI_VERSION_HISTORY.md`), work-verifier before "done",
never push, never deploy mid-game. Weights REUSE existing V-tag numbers — never invent.

## Standing rules (non-negotiable, from feedback_* memory)
- LOCAL commits only. NEVER push to GitHub. NEVER `docker compose down -v` / `rm database/` / unpin mariadb.
- Behavior-preserving unless Steve OKs a change; boundary math (edge cases) BEFORE any magnitude change.
- Comment out superseded code (never delete); every fix leaves breadcrumbs in BOTH changelogs same session.
- READ the actual card SOURCE (Card*.java setGameText) before any card-text claim — never fabricate ids/names.
  DB (`card_blueprint_database_*.json`) MISSES set601/Legacy + carries stale draft text; Java source wins.
- Search by type/Filter/Keyword/Icon/Species, NOT substring on generic nouns. `Filters.senator`=keyword truth.
- Mirror every rando/ change to chosenone/ (clone ObjectiveAnalyzer, `models.rando`→`models.chosenone`) same session.
- ZERO id errors (Steve's hard bar): every id must resolve in the DB OR a Card*.java file. Deterministic-check chunks.
- After ANY rebuild/restart: confirm HTTP 200 (`curl localhost:17001/gemp-swccg/`) + flip 4 switches
  (reload-ai usually auto-flips; if server exits 128 → `docker compose -f src/docker-compose.yml up -d` + manual flip).

## Reference docs
- `resources/Objective_Normalization_Schema_2026-07-07.md` — the typed-fact schema (CardRef/LocationRequirement/etc).
- `Handoffs/CODEX_CARD_SEARCH_PLAN_2026-07-07.md` — how to resolve requirements to card ids (Filters-from-Java first).
- `resources/Objective_Intel_BottomUp_rows*.md` — draft intel (verify before trusting).
- `Handoffs/AI_MAILBOX.md` + `Handoffs/AI_PROTOCOL.md` — repo mailbox (durable); the live channel is `~/claude-codex-mailbox/`.
- Prior current-state handoff (session before this build): `Handoffs/K2_HANDOFF_2026-07-07_endor-fixes-and-bridge.md`.
