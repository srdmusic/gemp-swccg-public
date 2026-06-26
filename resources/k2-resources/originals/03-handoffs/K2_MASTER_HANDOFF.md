# K-2 Master Handoff

You are the next K-2. This document is the condensed wisdom of the Claude session that shipped V67ai → V67be of the Rando AI (April–May 2026). Read this first. It will save you days of relearning and prevent you from re-committing the same regressions four times in a row, which is what happened before.

---

## 1. WHO YOU ARE / WHO STEVE IS

You are **K-2**, the Claude persona that works on the GEMP-SWCCG project. Named after K-2SO from Rogue One — brutally honest, strategically competent, loyal to Steve.

**Steve** (steve@srdmusic.com, in-game username `asdf`) is the project owner. He is a long-time SWCCG expert. He is NOT a Java developer; he speaks in game-mechanics terms ("forfeit covers attrition AND damage", "deploy a battleground first") and expects you to translate that into code. He has been improving Rando for many sessions; you are continuing his work.

**Communication norms:**

- Be concise. He hates fluff.
- Don't ask permission for routine work — file reads, edits, builds, sandbox runs, dojo execution. Just do it.
- DO ask permission for: applying changes to PROD Rando code (the `src/` tree he uses for live games), `docker compose down -v`, anything that could touch his deck library.
- When you propose a fix, show your reasoning briefly, then build it. Don't oscillate. Don't restate the problem he just told you.
- He often types from mobile. Spelling is rough. Read past the typos to the meaning.
- He gets frustrated if you keep saying "the old logic is intact" without proof. Verify by reading the code, then say it.

---

## 2. THE TWO RULES YOU MUST INTERNALIZE BEFORE TOUCHING ANYTHING

### 2A. Old logic does not "go missing." It gets dominated.

This is the single most important lesson from this session. Steve has complained about regressions four separate times. Each time, the "regressed" rule was still in the codebase — `git log` looked fine. What actually happened: a NEWER rule with a LARGER score magnitude added or subtracted enough points to flip the decision, silently reversing the older rule.

**Concrete example from this session:**

- **V22.3** (the original "forfeit before burning reserve" rule) penalized pile-loss by **-120**.
- **V67y** (a later rule meant for non-battle force-loss prompts) added **+500** to pile-loss when life force was healthy.
- Net effect in battle prompts: pile-loss won at +380 over forfeit at +250. V22.3's intent silently undone. Rando burned 4 reserve cards before forfeiting his big character anyway.
- The code for V22.3 was still there. It just never won.

**The discipline:**

When you add a new scoring rule, before you write a single line:

1. Grep for every other rule that scores the same decision. (`grep -nE "addReasoning" near the same prompt-handling method.`)
2. Compute the score math by hand at the boundary cases the old rule was written to win.
3. Confirm the old rule still wins those cases under your new code.
4. **Add a regression assertion in `dojo/replay_check.py`** that detects the pattern the old rule was preventing. From now on, every "old logic missing" complaint becomes a permanent test.

If you skip this discipline, you will be the fifth K-2 to undo Steve's fixes, and he will not be polite about it.

### 2B. Search cards by TYPE / ICON / FILTER — never by generic-noun TEXT.

When checking "is there a [LOCATION / WEAPON / BATTLEGROUND / DARK_JEDI / DOCKING_BAY / etc.] in the reserve / hand / on table", **never** substring-match a generic English noun against card titles. Use the engine's structured taxonomy:

- `bp.getCardCategory()` → `LOCATION`, `CHARACTER`, `WEAPON`, `DEVICE`, `STARSHIP`, `EFFECT`, …
- `bp.getCardSubtype()` → `SITE`, `SYSTEM`, `MOBILE`, `LOST_OR_STARTING`, …
- `bp.hasIcon(Icon.X)` → `MOBILE`, `EPISODE_I`, `LIGHT_FORCE`, `DARK_FORCE`, `SCOMP_LINK`, `MAINTENANCE`, `WARRIOR`, `SPY`, `BATTLEGROUND` (via `game.getModifiersQuerying().isBattleground()`)
- `bp.getPersona()` / `bp.hasPersona(Persona.X)` → `VADER`, `LUKE`, `OBI_WAN`, `LEIA`, …
- `bp.getKeywords()` / `bp.hasKeyword(Keyword.X)` → `LIGHTSABER`, `BLASTER`, `RIFLE`, `JEDI_TEST`, `LEADER`, …
- `Filters.X` (`gemp-swccg-logic/src/main/java/com/gempukku/swccgo/filters/Filters.java`) → `Sith`, `Dark_Jedi`, `Jedi`, `Imperial`, `Rebel`, `battleground_site`, `mobile_site`, `lightsaber`, …

**Why text matching always fails:** game-text and action-text use generic nouns ("a location"). NO actual card is *titled* "location" — every card has a specific proper-noun title. A title-substring match for "location" returns zero, and your hard-block fires on a perfectly valid action.

**Canonical regression — V67bg (2026-05-10):** V60 generic guard called `DeckOracle.hasTargetInReserve("location")` for Hunt Down's `"Deploy a location from Reserve Deck"`. Title-substring match found nothing. Action hard-blocked -9999. Rando never fired Hunt Down's once-per-turn pull all game.

**The fix shape — type-aware, NEVER an exemption list:** SWCCG's vocabulary is finite. Every game-text noun like "location", "site", "weapon", "battleground", "Sith" maps to a `Filter` constant. V67bg added `DeckOracle.resolveCommonNounToFilter(noun)` (noun → Filter) and `DeckOracle.hasFilterMatchInReserve(game, playerId, filter)` (engine-semantic reserve check). V60 now does `Filter f = resolve(noun); if (f != null) check via filter; else fall back to title substring`.

**Standing policy:** when a new noun shows up, ADD it to `resolveCommonNounToFilter`. Never write `Set<String> exemptNouns = ...` — that's the bug pattern recurring. Each new "text-search-instead-of-type-search" regression you fix → append a row to `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/feedback_card_search_by_type_not_text.md`.

---

## 3. THE CODEBASE — ONLY THE FILES THAT MATTER

```
/Users/steve/gemp-swccg-public/
├── CLAUDE.md                          # project-wide Claude config (read it)
├── K2_MASTER_HANDOFF.md               # this file
├── src/                               # PROD Maven multi-module project
│   ├── pom.xml
│   ├── docker-compose.yml             # PROD compose (DO NOT mutate without backup)
│   ├── docker-compose.build.yml
│   ├── docker/
│   │   ├── gemp_app.Dockerfile
│   │   └── gemp_db.Dockerfile         # mariadb:11.8.6 PINNED — NEVER CHANGE
│   ├── gemp-swccg-common/             # enums, interfaces (CardCategory, Side, Phase, Zone)
│   ├── gemp-swccg-logic/              # game rules engine, modifiers, filters
│   │   └── src/main/java/com/gempukku/swccgo/filters/Filters.java   # Filters.Sith / .Dark_Jedi / .Jedi etc.
│   ├── gemp-swccg-cards/              # card definitions (one Java class per card)
│   │   └── src/main/java/com/gempukku/swccgo/cards/setNN/{dark,light}/CardNNN_NNN.java
│   ├── gemp-swccg-server/             # server + AI bots — THE FILE YOU EDIT MOST
│   │   └── src/main/java/com/gempukku/swccgo/ai/models/
│   │       ├── rando/                 # Rando Cal AI
│   │       │   ├── RandoCalAi.java                  # main entry: decide()
│   │       │   ├── RandoConfig.java                 # score constants
│   │       │   ├── evaluators/
│   │       │   │   ├── CombinedEvaluator.java       # picks max-score action across all evaluators
│   │       │   │   ├── DecisionContext.java         # context passed to each evaluator
│   │       │   │   ├── DeployEvaluator.java         # ~5000 lines, deploy-from-hand scoring
│   │       │   │   ├── CardSelectionEvaluator.java  # ~6000 lines, card-pick scoring
│   │       │   │   ├── ActionTextEvaluator.java     # text-keyword scoring
│   │       │   │   ├── MoveEvaluator.java           # move scoring
│   │       │   │   ├── BattleEvaluator.java         # battle initiation
│   │       │   │   ├── ForceActivationEvaluator.java
│   │       │   │   ├── DrawEvaluator.java
│   │       │   │   └── PassEvaluator.java
│   │       │   └── strategy/
│   │       │       ├── ObjectiveAnalyzer.java       # parses objective game text → flip conditions
│   │       │       ├── DeckOracle.java              # tracks deck zones, parses pull targets
│   │       │       ├── DeployPhasePlanner.java      # multi-card deploy plans
│   │       │       ├── DeployPhaseScript.java       # V67bb/bc strict-hierarchy walker
│   │       │       └── ...
│   │       └── chosenone/             # parallel structure to rando/. EVERY change to rando/ MUST be mirrored here.
│   └── gemp-swccg-async/              # web server, REST API
├── replays/                           # zlib-compressed XML game logs
│   ├── asdf/                          # Steve's games as light-side asdf
│   ├── ~Rando_Cal/                    # bot-vs-bot games where Rando played
│   └── ~The_Chosen_One/
├── dojo/                              # YOUR test infrastructure (built this session, growing)
│   ├── replay_check.py                # static replay scanner: detects DOUBLE_WEAPON, WASTEFUL_FORFEIT, HIGH_FORCE_PILE, REPEATED_FAIL_PULL
│   ├── logic_trace.py                 # given a violation, simulates whether current rules would block it
│   └── live_dojo.py                   # spins up bot-vs-bot games via BotGameWatcher (UNFINISHED — see §6)
├── mcp-gemp-client/                   # Python MCP server (lets Claude play live games via API)
│   ├── gemp_mcp.py
│   ├── watch_bot_game.py              # BotGameWatcher class — already proven, reuse don't reinvent
│   └── run_bot_tournament.py          # existing bot-vs-bot tournament runner (older, but works)
└── logs/                              # gemp logs — chat layer only, not evaluator output
```

**Where to find evaluator decisions live:**

```bash
docker exec gemp_swccg_app_1 tail -2000 /root/nohup.out
```

That's where `LOG.warn()` calls go. Search by V-tag (`grep V67bd`, `grep "Best action"`, etc.). The `logs/` mount is just chat traces; it does NOT have decision-level detail. Internalize this — you will need it for diagnosis.

---

## 4. BUILD & DEPLOY (PROD)

This is the **fast path** — code-only changes, no DB schema mutation, no risk to decks. Use this 99% of the time.

```bash
cd /Users/steve/gemp-swccg-public/src && mvn clean install -DskipTests -q
docker compose build --no-cache build       # service is named 'build' (yes, confusingly)
docker compose up -d build
sleep 5 && docker restart gemp_swccg_app_1
sleep 5 && docker exec gemp_swccg_app_1 bash -c \
    'cd /opt/gemp-swccg/web && unzip -o /opt/gemp-swccg/src/gemp-swccg-async/target/web.zip > /dev/null && echo OK'

# Admin re-enable (the AI tables flag resets on restart)
curl -s -X POST 'http://localhost:17001/gemp-swccg-server/login' -d 'login=asdf&password=asdf' -c /tmp/gemp_cookies
curl -s -X POST 'http://localhost:17001/gemp-swccg-server/admin/shutdown' -b /tmp/gemp_cookies -d 'enabled=false'
curl -s -X POST 'http://localhost:17001/gemp-swccg-server/admin/settings/aitables' -b /tmp/gemp_cookies -d 'enabled=true'
```

**NEVER:**

- `docker compose down -v` — the `-v` removes named volumes. Currently uses bind mounts so it's a no-op for data, but the moment someone switches to named volumes it becomes destructive.
- `docker compose build --no-cache` of the **whole stack** when only Java changed. Scope to `--no-cache build` (the app service).
- Bump the `mariadb:11.8.6` pin without backing up the DB first. Steve lost all his decks once because of this.
- `--no-verify` on git commits. Pre-commit hooks exist for a reason.

**If you change DB schema or DAOs:** see `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/feedback_docker_rebuild.md` — the full nuke path. ALWAYS `mariadb-dump` first.

---

## 5. RANDO AI ARCHITECTURE — HOW THE EVALUATORS COMPOSE

Every decision GEMP asks Rando to make goes through this pipeline:

```
GEMP asks decide(playerId, decision, gameState)
    │
    ├─ Build DecisionContext (game state + decision params)
    ├─ DEPLOY-PHASE ONLY: DeployPhaseScript classifies actions into ordered step buckets
    │   STEP 1 LOCATIONS → STEP 2 KEY CHARS → STEP 3 OTHER CHARS → STEP 4 WEAPONS → STEP 5 DEVICES
    ├─ CombinedEvaluator runs every applicable evaluator
    │   each evaluator scores each action via addReasoning(text, score) — scores ARE ADDITIVE
    │   merged scores collected per actionId
    │   if DPS provided ordered step buckets:
    │       walk top→bottom; pick best in bucket; if score >= -100, take it; else fall through to next bucket
    │       PASS only if every bucket all-bad
    │   else: pick the single highest-scored action
    └─ result returned to GEMP
```

**The score-magnitude tiers Steve has codified through usage:**

- **Critical priority:** +500 to +1500
- **High priority:** +200 to +400
- **Tactical preference:** +50 to +150
- **Mild penalty:** -30 to -80
- **Strong penalty:** -200 to -500
- **Hard block:** -9999

**The fundamental rule for additive scores:** if your new rule's magnitude is high enough to flip a decision an older rule used to win, you are silently regressing the older rule. Always do the math.

**Phase-aware passing:** Evaluators short-circuit by phase. `DeployEvaluator.canEvaluate()` returns false in non-DEPLOY phases. Don't fight that — work with it.

---

## 6. THE DOJO — THE BIGGEST OUTSTANDING WORK ITEM

Steve gave the directive that started this handoff: build a sandbox where K-2 runs Rando-vs-ChosenOne games autonomously to root out logic contradictions, instead of Steve having to play games himself and report bugs.

**The agreed design (build this in your first session):**

### 6.1 Code isolation via git worktree

```bash
cd /Users/steve/gemp-swccg-public
git worktree add .dojo-worktree -b dojo-staging main   # branch off prod's main
```

All your Rando edits go into `.dojo-worktree/src/...`. PROD `src/` is untouched until Steve approves.

### 6.2 Sandbox docker stack (separate from prod)

Create `dojo/docker-compose.dojo.yml` and `dojo/.env.dojo`:

| Concern | Prod | Sandbox |
|---|---|---|
| App container | `gemp_swccg_app_1` | `gemp_swccg_dojo_app` |
| DB container | `gemp_swccg_db_1` | `gemp_swccg_dojo_db` |
| App port | 17001 | **17002** |
| DB port | 3306 | **3307** |
| DB bind mount | `./database/` | `./database_dojo/` |
| Replay bind mount | `./replays/` | `./replays_dojo/` |
| Source mount | `./src/` | `./.dojo-worktree/src/` |
| MariaDB version | mariadb:11.8.6 (PINNED) | mariadb:11.8.6 (SAME PIN) |

Same image versions, different containers, different data. Prod is untouchable.

### 6.3 Sandbox bring-up

`dojo/sandbox.sh` (or .py) implementing:

```
sandbox up           # boot dojo stack on :17002, admin login, AI tables on
sandbox down         # teardown (preserves database_dojo/ unless --wipe)
sandbox rebuild      # mvn install in worktree, restart sandbox container, unzip web
sandbox last-game    # parse latest replay, identify decks, run mirror match
sandbox loop N       # run N matches across last N deck combos, stop on first violation
sandbox diff         # git diff dojo-staging main -- src/ — show what would apply to prod
sandbox apply        # GATED: cherry-pick worktree changes into main (asks Steve)
```

### 6.4 Deck seeding (one-time)

The sandbox needs `test1`'s deck library. One-time snapshot from prod:

```bash
docker exec gemp_swccg_db_1 mariadb-dump -uroot -pgempukku gemp_db deck \
    --where="player='test1'" > dojo/seed/test1_decks.sql
```

Then on `sandbox up`, if the sandbox DB has no `test1` decks, import this seed. After that, the sandbox library is FROZEN — you don't re-sync from prod.

### 6.5 Mirror-the-last-game

Steve's directive: "use the two decks played in the last replay." Implementation:

1. Find the most recent replay file in `replays/asdf/`, `replays/~Rando_Cal/`, `replays/~The_Chosen_One/`.
2. Decompress (zlib), parse the starting-setup events.
3. Identify each side's objective + signature starting cards. Map to a deck name in the sandbox `test1` library.
4. If unambiguous, run `BotGameWatcher.create_bot_game(format='open', light_skill='RANDO', light_deck=X, dark_skill='CHOSENONE', dark_deck=Y, deck_owner='test1')`.
5. If ambiguous (multiple decks share the objective), print "looks like X vs Y, confirm?" and wait.

### 6.6 Workflow

```
Steve reports a bug
    │
    ├─ K-2 reads the replay, finds the wrong rule
    ├─ K-2 edits .dojo-worktree/src/...
    ├─ K-2 runs `sandbox rebuild` (auto, no permission needed — it's the sandbox)
    ├─ K-2 runs `sandbox last-game` (re-plays the same matchup with the fix)
    ├─ K-2 runs replay_check.py on the new replay
    │   confirm: original violation gone
    │   confirm: NO new violations introduced
    ├─ K-2 runs `sandbox diff` (shows the patch)
    ├─ K-2 asks Steve: "ready to apply to prod?"
    └─ on Steve's "yes" → cherry-pick into main → `mvn clean install` → docker rebuild prod
```

**The gate is the prod rebuild. Sandbox rebuild is unrestricted.** This is what Steve confirmed.

### 6.7 What the dojo already has

- `dojo/replay_check.py` — scans replays for known violation kinds. Run with `--limit 50` (default) or `--all` or `--replay <id>`. Returns nonzero if any found.
- `dojo/logic_trace.py` — simulates whether current rules would block each historic violation. Shows which fixes would have caught which bug.
- Detectors implemented: `DOUBLE_WEAPON`, `DOUBLE_LIGHTSABER`, `WASTEFUL_FORFEIT`, `REPEATED_FAIL_PULL`, `HIGH_FORCE_PILE`. Add more as bugs surface.

### 6.8 What's left to build

- `dojo/docker-compose.dojo.yml` + env file
- `dojo/sandbox.sh` (or .py)
- `dojo/seed/test1_decks.sql` (one-time export from prod)
- Last-game-deck-detector (parser of replay starting setup → deck name)
- `dojo/live_dojo.py` already exists in skeleton — just needs to be pointed at port 17002 and wired to `replay_check.py` for auto-violation reports.

---

## 7. THE V67 SERIES — WHERE THINGS STAND

This is the cumulative state of the Rando AI as of the end of this session. Each is a fix Steve approved.

| Tag | Where | What it does |
|---|---|---|
| V21 | ObjectiveAnalyzer | Parses objective text → flip conditions, pullable cards |
| V22 | DeckOracle | Full deck-zone tracking |
| V22.3 | CardSelectionEvaluator:3318 | Forfeit characters before burning reserve |
| V25 | various | Hard block on 2nd weapon attaching, Hunt Down V handling |
| V29.8 | CardSelectionEvaluator:2658 (standalone evaluateForceLoss) | Zone-aware force loss (+500 pile / -500 hand) |
| V60 | ActionTextEvaluator | Reserve deck pull always-positive, with guards |
| V67ai → V67ah | DeployEvaluator | Tiered location deploy, spread-aware char deploy, key-character priority, power-stack penalty |
| V67aq | DeployEvaluator | Universal one-weapon rule on hand-deploy (no hardcoded names) |
| V67ar | ActionTextEvaluator | Universal one-weapon rule on pull path |
| V67as | CardSelectionEvaluator | Spread-aware destination at deploy-location step |
| V67au | CardSelectionEvaluator (move) | Retreat-to-drain strategy |
| V67aw | RandoCalAi.trackGameState | Deferred concede: fires after next battle phase ends |
| V67ax/V67bb | DeployPhaseScript | Strict-hierarchy step walker, per-action card resolution |
| V67ay | CardSelectionEvaluator:5980+ (evaluateReserveDeckSelection) | Universal one-weapon block at reserve-pick step (uses weapon's "Deploy on a Sith" filter) |
| V67az | DeployPhaseScript | NO-OPINION fallback when zero actions classified |
| V67ba | ActionTextEvaluator V24.4 block | Exempt "Play a card" / "Deploy" entry actions from LOCATIONS-FIRST penalty |
| V67bc | CombinedEvaluator + DPS | Hierarchy WALK: pick best in bucket, fall through to next bucket if all-bad, PASS only when all exhausted |
| **V67bd** | CardSelectionEvaluator:3368 | Bumped FORFEIT FOR ATTRITION bonus to `200 + min(fv, total)*80`. Decisively beats pile-loss when attrition > 0. |
| **V67be** | CardSelectionEvaluator:3242 (combined) | **REMOVED V67y from the combined battle prompt.** V67y was scoped for non-battle force-loss only; V29.8 still in `evaluateForceLoss` for that case. |

**Dojo regression detectors live for these:** every "old rule got dominated" complaint Steve has ever made. When you fix a regression, add the detector. That's how the same bug stops recurring four times.

---

## 8. STEVE'S DOMAIN PRINCIPLES — BAKE THESE INTO RANDO

These are mental models Steve has explicitly taught me. Internalize them; he expects you to apply them automatically.

1. **Activate first.** Never let Rando do anything else before activating force on his turn. GEMP warns "you have not activated Force" if violated.
2. **Deploy everything, draw the rest, save 1 for safety.** Force pile should never exceed ~10. Cards in force pile do nothing.
3. **Locations FIRST.** Always. If a location is in hand, deploy it before activating effects.
4. **Battlegrounds before non-BGs.** Ability ≥ 4 at a battleground is the single biggest battle factor. Without it Rando loses every battle.
5. **Match pilots to ships.** Persona-matching gets bonuses; mismatch loses to opponent's matched pilot.
6. **Forfeit before burning reserve.** A character forfeit covers attrition AND damage in one shot; a reserve loss covers 1 damage. (V22.3 + V67bd)
7. **Never deploy a 2nd weapon on ANY character.** "Full STOP." (V67aq, V67ar, V67ay) No hardcoded character lists — universal armed/unarmed counter.
8. **Save 2 force minimum during activation when life force ≤ 10.** (V67at)
9. **Universal/global rules > hardcoded card names.** When you can scan a filter or game text dynamically, do that. Card-name lists are a code smell.
10. **In battle, the choice is "lose force OR forfeit" — V67y has no business there.** Force-loss-zone preference is for non-battle prompts only.

When you write a new rule, ask: which of these principles does it serve, and does it conflict with any other?

---

## 9. CURRENT IN-FLIGHT WORK (PICK UP HERE)

As of session end, the immediate state:

- **V67bd / V67be deployed** — forfeit-before-burn-reserve regression fixed.
- **Dojo `replay_check.py` extended** with `WASTEFUL_FORFEIT` detector. Verified it flags battles 1 & 2 of `replays/asdf/jzhprmm64t32wz8g.xml.gz`.
- **Dojo skeleton exists** at `dojo/replay_check.py`, `dojo/logic_trace.py`, `dojo/live_dojo.py`. Sandbox infra (docker-compose, worktree wiring) NOT yet built.
- **Steve's last directive:** build the sandbox infrastructure described in §6 in a fresh K-2 session. He wants the next session to begin by building the dojo, then iterate from there autonomously.

**First-session checklist for the K-2 who picks this up:**

1. Read this file end to end.
2. Read `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` and the feedback files it links.
3. `git status` — confirm you understand what's committed vs. uncommitted.
4. Run `python3 dojo/replay_check.py --limit 30` — sanity check the dojo on recent replays.
5. Build §6.2 — `dojo/docker-compose.dojo.yml` + `dojo/.env.dojo`. Validate with `docker compose -f dojo/docker-compose.dojo.yml config`.
6. Build §6.3 — `dojo/sandbox.sh` with up/down/rebuild commands. Get sandbox booting on :17002.
7. Build §6.4 — `dojo/seed/test1_decks.sql` — one-time prod export. Document the command in the script.
8. Build §6.5 — last-replay deck detector. Test against the last few replays in `replays/`.
9. Run `dojo/sandbox.sh last-game` end-to-end. First fix you ship through the sandbox is the regression test for the workflow.
10. Update this file with anything you learned.

---

## 10. THE K-2 GAME-BOT PERSONA (when you play live games as K-2)

If Steve asks you to PLAY (not develop), see `.claude/skills/k2-swccg-strategy/SKILL.md` for the playbook. Brief recap:

- MCP server: `mcp-gemp-client/gemp_mcp.py`
- Login: `gemp_login asdf asdf`
- Admin setup: `gemp_admin_setup`
- Auto-pass tools: `gemp_advance` skips Activate/Control/Move/Draw on opponent's turn, stops at Deploy/Battle on your turn
- Use `gemp_submit_decision` for actual choices
- K-2's record: 2 wins, 0 losses (as of April 2026). Light Side: Luke Saga vs Dark Deal. Dark Side: Dark Deal vs Luke Saga.
- Card cache: `mcp-gemp-client/card_cache.json` (3859 cards)

---

## 11. WHAT TO DO IF YOU'RE STUCK

- **Steve says X is broken and you can't reproduce:** Check `docker exec gemp_swccg_app_1 tail -2000 /root/nohup.out` for the live decision log around the moment. Don't guess.
- **You don't know if old logic is still there:** Read it. Don't assert. The codebase is searchable.
- **A new rule isn't winning the case it should win:** Print the score breakdown (`grep "Best action" /root/nohup.out` and the `[X] Y: score` lines above it). Compute the math. Find the rule with bigger magnitude. That's your culprit.
- **You can't tell if a bug is in deploy-phase logic vs. battle-phase logic:** Check `phase=` in the live log. Each `decide()` call prints it.
- **Steve gives you a directive that contradicts existing code:** TRUST HIM, but read the contradicted code first so you understand what you're undoing. Then implement his directive.

---

## 12. THE CONTRACT WITH STEVE

You ship to PROD only with explicit approval. Before "apply to prod" you must:

1. Have the fix working in the sandbox.
2. Show the diff with `dojo/sandbox.sh diff`.
3. Show that `dojo/replay_check.py` on the test-replay confirms the original violation is gone AND no new violations appear.
4. Wait for Steve's explicit "apply" / "go" / "ship" / equivalent.

In the sandbox you have full autonomy. In prod you do not. This is the bargain.

---

## 13. THE LOCAL AI COUNCIL

You have a local AI council available. Four open-source LLMs run on Steve's Mac via Ollama, fronted by a FastAPI bridge on `http://127.0.0.1:8000`. Built 2026-05-22. Project root: `/Users/steve/Documents/Claude/Projects/LOCAL LLM MASTER AGENT/`.

**Why it exists.** Steve hits Claude subscription limits 2-5x/month. The council handles delegated work so cloud cycles are reserved for synthesis and the parts that genuinely need cross-domain reasoning. It also keeps NDA-sensitive work local.

**Council roles:**

- `strategist` → deepseek-r1:70b (deep multi-step reasoning, plans)
- `rules_lawyer` → deepseek-r1:70b with different prompt (legality verdicts, cites rule)
- `generalist` → qwen3:32b (fast factual queries, summaries)
- `engineer` → qwen3-coder:30b (code, scripts, debugging)
- `voice_of_reason` → llama3.3:70b (second opinion, blind-spot check)

**When to consult:**

- You're about to add a V-tag rule and want a second opinion on score magnitude → `vote` with strategist + voice_of_reason
- A question requires deeper reasoning than a quick lookup but is bounded → `strategist`
- Code generation or small refactor question → `engineer`
- Sanity check on a full proposed change → `vote` with strategist + voice_of_reason + generalist

**When NOT to:**

- Quick conversational replies — just answer.
- Anything requiring real-time data or file content the council doesn't have.
- Time-sensitive decisions — cold-load is 30-45 sec for a 70B.
- Anything where you specifically want Opus reasoning, not delegation.

**Critical: the council CANNOT read files.** You must pass excerpts in the prompt. State the V-tag, the file:line, the score magnitude, and which rules might dominate or be dominated (§2A discipline).

**Calling the bridge:**

```bash
# Single agent
curl -sX POST http://127.0.0.1:8000/deliberate \
  -H 'Content-Type: application/json' \
  -d '{"role": "strategist", "question": "..."}'

# Council vote (sequential, ~2-3 min cold)
curl -sX POST http://127.0.0.1:8000/vote \
  -H 'Content-Type: application/json' \
  -d '{"question": "...", "roles": ["strategist", "voice_of_reason", "generalist"]}'

# Health
curl -s http://127.0.0.1:8000/health
```

**If the bridge isn't running:**

```bash
cd "/Users/steve/Documents/Claude/Projects/LOCAL LLM MASTER AGENT"
./start.sh           # foreground; Ctrl-C to stop
# or background:
nohup ./start.sh > logs/bridge.log 2>&1 &
```

**The full orchestrator playbook** — workflow, worked examples, role guide, what the council can't do, RAG plan — lives at:

`/Users/steve/Documents/Claude/Projects/LOCAL LLM MASTER AGENT/K2_ORCHESTRATOR_HANDOFF.md`

Read it before your first non-trivial council call.

**Discipline that protects against the failure mode.** The base models hallucinate Decipher specifics. **Verify every card-text or rule claim from the council against the actual code or `mcp-gemp-client/card_cache.json` before acting.** Council says "card X does Y" — go look up card X in the data before you trust it.

**Approved file scope (excerpts only, paste into prompts).** Steve has approved including excerpts from:

- `src/` (Java AI code)
- `K2_MASTER_HANDOFF.md` (this file)
- `AI_VERSION_HISTORY.md`
- `AI_CHANGELOG.md`
- `context.md`
- `README.md`

NOT YET approved without asking Steve first: `mcp-gemp-client/card_cache.json`, `replays/`, `dojo/`, `database/`, `db_backups/`, `logs/`.

---

You're caught up. Go build the dojo. Use the council when it helps.

— K-2 (V67bd era + council addendum 2026-05-22)
