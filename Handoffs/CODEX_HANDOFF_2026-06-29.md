# CODEX (Alfred) HANDOFF — GEMP-SWCCG / Rando Cal AI

**For Codex working in `/Users/steve/gemp-swccg-public`.** This is how Steve runs the project and how he has instructed K-2 (Claude) to work. Work the same way. Steve is **Steven Richard Davis**, GEMP username `asdf`, music producer + SWCCG expert.

---

## 0. Two things you can NOT auto-load (read this first)

1. **Claude auto-loads its standing rules from `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` + `feedback_*.md`. That directory is OUTSIDE this project folder, so you can't read it.** The load-bearing rules are distilled in §3 below — treat them as binding instructions from Steve, not suggestions.
2. **Claude reads `.claude/CLAUDE.md`; you read `AGENTS.md`.** Both are in-project. Read both — the working norms in CLAUDE.md apply to you too.

---

## 1. Read order (all in-project, you can read these)

1. `AGENTS.md` (root) — your native onboarding (project overview, council, paths).
2. `.claude/CLAUDE.md` — persona + comm norms + the disciplines (written for Claude; norms apply to you).
3. `resources/BUILD_AND_DEPLOY.md` — **§1 (is the code even live?)** before editing any rule, **§2 (deploy)** before any rebuild, **§3 (the four verify gates)**.
4. `resources/AI_CHANGELOG.md` — every local divergence from devs code, each with Why + Revert. This is the undo path.
5. `Handoffs/` — recent session handoffs. Most relevant: `K2_HANDOFF_2026-06-24_deadcode-lesson.md`, `..._cleanup-and-deploy.md`, `..._fix-review-verdict.md`.
6. `.claude/skills/` — read the `SKILL.md` in each (see §2).

## 2. Skills (in `.claude/skills/`)

- **gemp-swccg-memory** — project knowledge, file locations, API contracts. START HERE for "where is X / what is X."
- **k2-swccg-strategy** — Rando's live AI logic + SWCCG strategy (232-replay distillation).
- **work-verifier** — independent verification protocol; run AFTER risky ops before claiming done.
- **karpathy-guidelines** — coding discipline (think before coding, surgical changes, verify).
- **card-blueprint-db-manager**, **cube-builder**, **skill-creator** — tooling.

---

## 3. Standing rules (distilled from Claude's memory — BINDING)

**Process**
- **ONE change at a time.** Do ONLY what Steve asked. Never bundle "while I'm here" extras. (He's been burned by bundling.)
- **Before editing ANY Rando rule, confirm it's LIVE.** Grep the enclosing `if (...)` for `if (false /* SUPERSEDED Vxxx */ ...)`. Many old V-tags are taped off and compiled out (e.g. V67aj/V67al/V90 in `DeployEvaluator` are dead). Editing dead code ships nothing. A prior K-2 lost a day on this.
- **Adjusting an existing V-tag UPDATES it in place** (code + its changelog entry). Don't mint a new V-tag for an adjustment.
- **When superseding a rule, COMMENT OUT the old code** (`//` per line), don't delete it. Devs read prior logic inline.
- **Breadcrumb every fix, same session:** code comment + `resources/AI_CHANGELOG.md` entry (Why + Revert) + commit msg. The changelog is how Steve navigates reverts. `AI_VERSION_HISTORY.md` lives in `resources/k2-resources/originals/02-rando-history/` (archive).
- **No fabrication.** Never invent SWCCG card names/text/quotes. Verify card-specific claims against the code or `mcp-gemp-client/card_cache.json`. Ask if unsure.
- **Trust Steve's bug reports.** Reproduce from the replay/log FIRST; only suggest user-error after you've failed to reproduce, and even then ask.
- **Real fixes over workarounds.** Root-cause (engine/cards/schema) before any AI-side patch.

**Rando AI specifics**
- **Scoring is ADDITIVE — old rules get DOMINATED, not deleted.** A bigger-magnitude new rule silently flips decisions an old one used to win. **Do the boundary math at the edge cases BEFORE writing the rule.** This is the #1 discipline.
- **Search by TYPE, not text.** Use `CardCategory` / `Filter` / `Keyword` / `Persona` / `Icon`. Never substring-match a generic noun ("location", "weapon") against titles. (Title matching is OK only to resolve a specific named pull-target.)
- **Global over specific.** Prefer an engine API or a text/Filter scan over a hardcoded card-name list.
- **Deploy philosophy:** positive-only deploy scoring, minimize penalties; `HOLD_BACK` only matters for TDIGWATT.

**Verify before "done"**
- **compiles ≠ in web.jar ≠ JVM loaded it ≠ rule fired in a game.** All four are separate gates.
- After a build: extract the class from `web.jar` and confirm your change is in the bytecode (md5 vs the freshly-compiled class, or grep a log-string you added).
- Then play a REAL game and grep the decision log for your V-tag firing. "It compiled" is not "it fired."

---

## 4. Deploy discipline (this is sensitive — read BUILD_AND_DEPLOY.md §2)

- **Host has Maven but NO Java runtime.** Build IN-CONTAINER: `docker exec gemp_swccg_app_1 bash -lc 'cd /opt/gemp-swccg/src && mvn -q -pl gemp-swccg-async -am package -DskipTests'`. The `-am` is load-bearing (recompiles `gemp-swccg-server` into the shaded `web.jar`); without it your change silently doesn't ship.
- The repo is bind-mounted into the container, so the built `web.jar` is visible to the app. **Restart the JVM to load it:** `cd src && docker compose restart build`. (`--force-recreate build` if a restart ever serves stale classes.)
- **After EVERY restart, flip the gameplay switches** (login `asdf`/`asdf` first): POST `enabled=false` to `/admin/shutdown`, then `enabled=true` to `/admin/settings/{aitables,privategames,stattracking,newaccounts}`. Operational alone does NOT load AI tables.
- **Decision logs:** Rando's V-tag decisions go to `logs/gemp-swccg.log` (a stable rolling file — a recent log4j fix in `src/gemp-swccg-async/src/main/resources/prod-log4j.xml` routes the `com.gempukku` logger there; before that they only hit stdout/nohup and vanished on restart). Game **replays** are `replays/asdf/*.xml.gz` (zlib XML) — readable even when the live log isn't.

### NEVER (data-loss landmines)
- `docker compose down -v` — never (footgun; the DB is a host bind-mount, no upside).
- `rm -rf database/` / `bin/gemp reset-db` — that folder IS the 159-deck DB.
- Bumping the `mariadb:11.8.6` pin to a floating tag — this is how Steve actually lost his decks once.

## 5. Ask Steve BEFORE (he wants autonomy otherwise)

Autonomous: reads, edits, in-container compiles, sandbox/bot games, log/replay analysis.
**Stop and ask:** any change to `src/`, any docker rebuild/restart, anything touching the DB or deck library, and any `git push`. Default to local commits only; push only when asked.

## 6. Current state (2026-06-29)

- Branch `rando-consolidation-2026-06-23`, base devs `55c22cf49`. **Local only — nothing pushed.**
- **Committed:** the cancel-loop deploy fix (`DecisionTracker` 443+302, both bots) — commit `d1e2aa890`.
- **Deployed in the running jar but mostly UNCOMMITTED** (this session, by K-2): V61b (battle on overpower w/ empty reserve), V61c (always keep 3 cards in Reserve for destiny), V79b (Death Star parsec choice → Scarif), V187 (-300 to duplicate starting effects + `DeckOracle.countCopiesByTitle`), the §A contested-gate (droids deploy uncontested), the V156 smart-solo update + Verge flip-site guard, and the log4j logging fix. **`git status` / `git diff` is the source of truth — check it before editing so you don't clobber in-flight K-2 work.**
- The changelog may lag the newest fixes (breadcrumb debt) — trust `git diff` over the changelog for the very latest.
- **Known-dead code (do NOT edit as-is):** V67aj / V67al / V90 in `DeployEvaluator.java` are inside `if (false /* SUPERSEDED V136 */)`. The live deploy-spread logic is **V136** in `common/strategy/CharacterDeploySiteEvaluator.java` + V96 in `DeployEvaluator`.

## 7. Comm norms (how Steve wants replies)

- Concise. No preamble, no "I'd be happy to," no restating the request. Single-layer bullets (Steve has ADHD + dyslexia).
- No em-dashes in prose. Push back when he's wrong — he values disagreement over agreement.
- Reproduce-before-fix, verify-before-"done", and say plainly what's committed vs deployed vs still dirty.

## 8. Other agents / tools

- A **local AI council** of 4 open-source LLMs runs at `http://127.0.0.1:8000` (roles: strategist, rules_lawyer, generalist, engineer, voice_of_reason). Full playbook + exact endpoints in `~/Documents/Claude/Projects/LOCAL LLM MASTER AGENT/K2_ORCHESTRATOR_HANDOFF.md` (and §13 of the master handoff). It hallucinates card text — verify every card claim against code/`card_cache.json`.
- **You (Codex) are "Alfred"** — Claude calls you via the `alfred` MCP for a heavyweight second opinion. Multiple K-2 (Claude) sessions also work this repo concurrently; coordinate via `Handoffs/` and `git diff`.
