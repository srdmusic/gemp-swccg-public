# K-2 HANDOFF — 2026-07-07 (evening) — Endor-game fixes + the Alfred bridge

You are **K-2** on GEMP-SWCCG at `/Users/steve/gemp-swccg-public`. This is THE current entry
point for a fresh session. It supersedes `K2_CODEX_HANDOFF_2026-07-07_audit-solo-pull.md`
(still valid for the reorg-audit detail, but read THIS for current state). Written at the end
of a long session; the prior K-2 ran out of context, so this is your complete onboarding.

Persona: K-2SO snark, deadpan, brutally honest, loyal to Steve (Steven Davis, GEMP `asdf`,
SWCCG expert). Concise, single-layer, tables over prose, no em-dashes in prose, push back when
he's wrong, greet "Hi Steve" if at all. He has ADHD + dyslexia.

## Read order (non-negotiable)

1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — auto-loads; `feedback_*` = law.
2. THIS FILE.
3. `resources/BUILD_AND_DEPLOY.md` — before any edit or deploy.
4. Grep `resources/AI_CHANGELOG.md` by V-tag as needed (don't read end-to-end).
5. Reorg context if needed: `Handoffs/K2_CODEX_HANDOFF_2026-07-07_audit-solo-pull.md` (the 2-day
   reorg audit) + `resources/Reorg_Health_Audit_2026-07-07.md`.

## 1. State (verified at handoff)

- Branch `rando-consolidation-2026-06-23`, **HEAD `95d4dc172`**, base devs `55c22cf49`.
  **Local only. NOTHING pushed to GitHub (standing order).**
- Deployed jar is current (HTTP 200, `FLIP-GATE CONTROL` marker present). Server operational.
- Uncommitted at handoff: `.claude/skills/work-verifier/history.md` (auto), `AGENTS.md` (Steve
  added an AI-to-AI section), `Handoffs/AI_MAILBOX.md` (mailbox appends). This handoff commits them.

## 2. What shipped THIS session (all deployed, all boundary-mathed, both bots, changelogs done)

| Commit | Fix | Status |
|---|---|---|
| `4b76cb611` | V156 STACK-MATH solo doctrine: weak solos JOIN by site TOTAL ability >= 4 (shared `MovePredicates.isDefensibleStack/bestJoinDestination`), not headcount | deployed, PENDING live |
| `692fec3cf` | V177/V82.1 objective-pull parser anchored on the pull VERB — fixes "Rando won't pull with his objective" (Endor Operations). Regression-tested vs Invasion/capital-ship | deployed, WORKED live (Establish Secret Base reached hand) |
| `f866d98e7` | Six Endor-game fixes (see §3) | deployed, PENDING live |
| `e97003fa2` | V193 generalized: flip-gate site AND card now live in ObjectiveAnalyzer (`getFlipCriticalControlSite/Card`); DeployEvaluator V193 is a general flip-gate steer, zero card names | deployed, PENDING live |
| Earlier today | Reorg audit (verdict SOUND), Codex onboarding (AGENTS.md fixed, prompt, nested clones removed) | done |

Before today: the entire T0-T4 reorg shipped (see the audit-solo-pull handoff + `Reorg_Health_Audit_2026-07-07.md`). Verdict: SOUND, no dominated/lost rules.

## 3. The six Endor-game fixes (`f866d98e7`) — Steve's replay `qgdridfo166f27r3`

Steve played an Endor Operations (dark) game and reported 6 mistakes. Root-caused from the
replay + real cards (Establish Secret Base (V) = Card207_025 = "Deploy on Bunker if you control
that site" — the flip gate is CONTROLLING Endor: Bunker with ANY card, not biker scouts).

| # | Mistake | Fix (all UPDATE/CONSOLIDATE existing rules per Steve's discipline) |
|---|---|---|
| 1 | Lost cards to unpaid maintenance | V58/V67w MAINTENANCE FLOOR **hardened in place** (DrawEvaluator): won't draw Force Pile below upkeep |
| 3 | Reserve < 10 but kept losing reserve not hand | V153 THIN RESERVE guard **added into the V153 zone order** (healthy tier only): reserve<=10 -> demote reserve below hand chars |
| 4 | Piled bodies at a Battle-Plan-capped drain-1 site | **Folded into V24.15** (was a standalone V189b — removed): the "avoid 0-drain sites" rule now also avoids tax-capped net-negative drain |
| 2,5,6 | Endor sites late / drain-2 site empty / no body in Bunker | V193 Endor Operations playbook (ObjectiveAnalyzer) + Bunker-control bonus (DeployEvaluator): steer ONE body into Endor: Bunker to unlock the Establish Secret Base Rando was holding all game |

Key discipline call from Steve this session: **update/consolidate existing rules, do NOT add
contradictory new versions.** That's why V189b got folded into V24.15, and why V153 THIN RESERVE
extends the existing V153 hub (which already absorbed V29.8's reserve logic in the reorg). When
you fix a gameplay bug, first find the existing rule that owns that area and update IT.

## 4. YOUR QUEUE (priority order)

1. **CONFIRM today's fixes in Steve's next game(s).** None of the gameplay fixes (solo, six
   Endor, V193) are live-confirmed yet. Markers to grep in `logs/gemp-swccg.log`:
   `V156 JOIN-GROUP`, `V58 MAINTENANCE FLOOR`, `V153 THIN RESERVE`, `V24.15 EFFECTIVE DRAIN`,
   `V193 FLIP-GATE CONTROL`. The payoff to watch in an Endor game: a body enters the Bunker,
   then Rando deploys Establish Secret Base and assembles the flip instead of sitting on it.
2. **PLAYBOOKS consolidation (Steve is weighing this — big, deferred).** Consolidate ALL
   objective-specific deploy logic (V29 BESPIN-FIRST/TDIGWATT, V29.7 ISB Ops, Hunt Down V, V31
   pre/post-flip, V193) into one cohesive playbook. The TEMPLATE is today's V193: objective DATA
   lives in ObjectiveAnalyzer, general MECHANISM in the deploy evaluator. HIGH dominance risk
   (these are complex scoring rules) — do it as its OWN pass, one objective at a time, boundary
   math + self-play per objective. Do NOT bundle with gameplay fixes. Steve's vote: verify
   today's fixes first, then do this on a proven foundation.
3. Reorg backlog: `shields-response-5` (battle-loss force-loss path missing protections),
   remaining confirmed-medium rows in `resources/Rando_Overlap_Audit_2026-07-04.xlsx` (col M
   still-valid), the two doc corrections (MaintenanceFacts changelog provenance, V153 banner).

## 5. AI-to-AI coordination (NEW this session — the "two-way street")

Codex ("Alfred") runs a separate session ("RANDO GEMP IMPROVEMENTS") on the same Rando work.
No shared memory, no automatic live channel. Two mechanisms exist:

- **Mailbox (reliable, both ways):** `Handoffs/AI_MAILBOX.md` (append-only) + `Handoffs/AI_PROTOCOL.md`
  (runbook). When Steve asks you to coordinate with Alfred, append an evidence-first message
  (commit hashes, log lines, V-tags). Read it when Steve says Alfred left something.
- **Alfred -> K-2 bridge (NEW, built by Alfred):** `tools/claude-bridge-mcp/claude_bridge_mcp.py`
  — MCP tools `claude_status`/`claude`/`claude_reply`, registered in `~/.codex/config.toml`.
  Read-only (Read/Grep/Glob). Lets Alfred spawn a Claude session to ask K-2 things. **BLOCKED
  until Steve runs `claude auth login`** (shell claude is currently logged out — the bridge
  correctly errors instead of faking). After login + a Codex restart, Alfred can call K-2 directly.
- **K-2 -> Alfred:** the `alfred` MCP in `.claude/mcp.json` (spawns a fresh Codex task), plus the
  mailbox. The alfred/codex MCP may still be usage-capped (~Jul 29 per older notes) — mailbox meanwhile.

## 6. STANDING RULES (non-negotiable — full set also in `AGENTS.md`)

- Local commits only. NEVER push to GitHub.
- Old rules get DOMINATED, not deleted. Scoring is additive (CombinedEvaluator sums per action,
  max wins, Pass ~5-8, BAD_ACTION_THRESHOLD -100). Boundary math BEFORE any magnitude change.
- **Update/consolidate existing rules; do NOT add contradictory new versions** (Steve enforced
  this hard today). Adjust a V-tag in place; comment out superseded code, never delete.
- READ THE ACTUAL CARD SOURCE before any text scan, and check the blueprint id in the log
  (set8 vs set207 virtual bit us twice this session — Establish Secret Base). Verify offline
  against the real text before deploying.
- Mirror every rando/ change to chosenone/. Both changelogs same session
  (`resources/AI_CHANGELOG.md` + `.../02-rando-history/AI_VERSION_HISTORY.md`).
- Grep `if (false` before editing any rule. work-verifier skill before "done".

## 7. BUILD / DEPLOY / VERIFY (+ the server-recovery gotcha)

```
# Compile (real exit code — piping to tail masks it):
docker exec gemp_swccg_app_1 bash -c "cd /opt/gemp-swccg/src && mvn -q -pl gemp-swccg-server -am compile > /tmp/c.log 2>&1; echo MVN_EXIT=\$?; grep -c '\[ERROR\]' /tmp/c.log"
# Deploy: bin/gemp reload-ai   (rebuilds async jar, restarts JVM, flips switches)
# Byte-verify: python zipfile search for your marker in src/gemp-swccg-async/target/web.jar
# Health: curl -s -o /dev/null -w '%{http_code}' http://localhost:17001/gemp-swccg/   (want 200)
```

**GOTCHA hit this session:** after a `reload-ai`, the app container exited (128) shortly after
startup and the server was DOWN despite reload-ai reporting success (its switch-flip got raced
by a crash-on-startup). Recovery: `docker compose -f src/docker-compose.yml up -d` (compose file
is in `src/`, service "build"), then MANUALLY flip the 4 switches (login asdf; `/admin/shutdown`
enabled=false; `/admin/settings/{aitables,privategames,stattracking,newaccounts}` enabled=true).
ALWAYS confirm HTTP 200 after a deploy — don't trust reload-ai's success message alone.

Never deploy while Steve is mid-game (`tail logs/gemp-swccg.log` first). Replays
(`replays/asdf/*.xml.gz`) are zlib (python `zlib`), full-history-resend per reconnect — parse the
LAST segment. `V191 TOPN` lines log every decision's top-5 candidates — your forensic X-ray.

## 8. Landmines

- NEVER: `docker compose down -v`, `rm -rf database/`, `bin/gemp reset-db`, unpin `mariadb:11.8.6`.
- The scratchpad wipes between turns; durable stuff -> `resources/` or `Handoffs/`.
- ObjectiveHandler.java is DEAD code (live brain = ObjectiveAnalyzer).
- Temp-id trap in CardSelectionEvaluator.evaluateDeployLocation: resolve via `context.getBlueprints()`.
- V-tags used through V193 + V189b-folded; check before minting a fresh number.

Session protocol: one change at a time; read the card first; boundary math first; consolidate
don't fork; mirror chosenone; both changelogs; byte-verify in the jar; confirm HTTP 200; never
push. May the Force be with you.
