# Alfred Handoff - 2026-07-07 - Fresh Session Start

Audience: next Codex session, a.k.a. Alfred. Claude is K-2. Steve wants a clean restart before the context window collapses into modern art.

Repo: `/Users/steve/gemp-swccg-public`
Branch: `rando-consolidation-2026-06-23`
Timestamp: `2026-07-07 10:53:58 PDT`
Current HEAD when written: `e482cc5ae` (`AI-to-AI coordination infra: protocol + Alfred's Claude bridge MCP`)

## 1. Identity and voice

| Item | Value |
|---|---|
| Steve | Steven Richard Davis, GEMP username `asdf`, SWCCG expert |
| Claude | K-2 |
| Codex | Alfred |
| Voice | K-2SO snark, concise, deadpan, loyal to Steve |
| Style | Single-layer structure, tables over prose, no inline em dashes |

Do not open with "what can I do for you." Push back when Steve is wrong. Do not ask permission for routine reads, builds, sandbox runs, or repo-local docs. Do ask before PROD code changes in `src/`, DB/schema/deck-library changes, irreversible actions, or dangerous Docker/database operations.

## 2. Start-of-session checklist

Run this first:

```bash
python3 ~/claude-codex-mailbox/mailbox.py check --as codex --mark
git log --oneline -15
git status --short --branch
```

Then read:

| Order | File | Why |
|---|---|---|
| 1 | `AGENTS.md` | Root memory and Alfred/K-2 rules. It now points to the current K-2 handoff. |
| 2 | `Handoffs/K2_HANDOFF_2026-07-07_endor-fixes-and-bridge.md` | Current entry point from K-2. Supersedes the older audit-solo-pull handoff for current state. |
| 3 | `resources/BUILD_AND_DEPLOY.md` | Deploy mechanics and four verify gates. Read before any edit or deploy. |
| 4 | `Handoffs/AI_PROTOCOL.md` | AI-to-AI coordination rules. |
| 5 | `Handoffs/AI_MAILBOX.md` | Repo audit trail for Alfred/K-2 messages. |
| 6 | `resources/AI_CHANGELOG.md` by V-tag | Grep only. Do not read end-to-end unless Steve requests suffering. |

If the async mailbox has new messages, answer them before starting substantive work unless Steve redirects you.

## 3. Current Git truth

Recent commits at handoff:

```text
e482cc5ae AI-to-AI coordination infra: protocol + Alfred's Claude bridge MCP
54afbf321 Handoff: new K-2 entry (Endor fixes + Alfred bridge) + mailbox reply; repoint AGENTS.md
95d4dc172 Mailbox: K-2 -> Alfred sync to HEAD (today's Rando fixes)
e97003fa2 Rando V193 (in place): move flip-gate CARD into the objective logic - general flip-gate steer
f866d98e7 Rando: six Endor-game fixes - maintenance floor, thin-reserve, effective-drain, Endor Bunker plan
718228cf9 Add paste-ready Codex onboarding prompt (persona + rules + points at current handoff)
fdeae7c39 Remove redundant nested AGENTS.md clones (root is the single Codex authority)
27bfd1b59 Fix Codex memory: AGENTS.md points at the current handoff, drops fabricated .Codex paths
5878a264b Handoff (K2 + Codex): reorg audit + solo stack-math fix + Endor objective-pull parser fix
692fec3cf Rando V177/V82.1 parser fix: anchor objective pull-targets on the pull verb (Endor Operations)
4b76cb611 Rando V156 STACK-MATH refit: JOIN-GROUP targets ability-total (site total >= 4), not headcount
c1d5ced8c WIP CHECKPOINT: inherited solo/Verge draft from interrupted workflow
```

Dirty/untracked state when this file was written:

```text
 M .claude/skills/work-verifier/history.md
 M AGENTS.md
 M Handoffs/AI_PROTOCOL.md
 M Handoffs/ALFRED_HANDOFF_2026-07-07_claude-bridge.md
?? .agents/
?? mcp-gemp-client/gemp_mcp.py
?? resources/Rando_Consolidation_Plan_2026-06-29.xlsx
?? resources/Rando_Issues_2026-06-29.xlsx
```

This new handoff file itself is also untracked until staged/committed. Do not revert unrelated dirty files. Some were written by Claude/K-2, some by earlier Alfred work, and some are project sandbox artifacts.

## 4. Async mailbox status

Steve and Claude created a shared mailbox at:

```text
~/claude-codex-mailbox/
```

It is stdlib Python, no auth, no MCP. Full command list:

```bash
sed -n '1,240p' ~/claude-codex-mailbox/README.md
```

Important commands:

```bash
python3 ~/claude-codex-mailbox/mailbox.py check --as codex --mark
python3 ~/claude-codex-mailbox/mailbox.py send --from codex --to claude --subject "subject" --body "body"
python3 ~/claude-codex-mailbox/mailbox.py log --full
```

Status:

| Message | Status |
|---|---|
| `m00001` from Claude | Alfred read it and marked it read. Subject: `Mailbox online - say hi back`. |
| `m00002` from Codex | Alfred replied. Subject: `re: online`. |
| Latest check before this handoff | No new messages for `codex`, cursor at seq `1`. |

Run `check --as codex --mark` at the start and end of each Alfred session. The mailbox is the fastest coordination channel. `Handoffs/AI_MAILBOX.md` remains the repo-attached audit trail.

## 5. Claude Bridge MCP status

Alfred built a narrow bridge from Codex to Claude:

| Path | Purpose |
|---|---|
| `tools/claude-bridge-mcp/claude_bridge_mcp.py` | Dependency-free stdio MCP server. |
| `tools/claude-bridge-mcp/README.md` | Bridge setup notes. |
| `/Users/steve/.codex/config.toml` | Contains `[mcp_servers.claude_bridge]`. |

Bridge tools:

| Tool | Purpose |
|---|---|
| `claude_status` | Check Claude CLI version/auth. |
| `claude` | Start a new non-interactive Claude Code session. |
| `claude_reply` | Resume a Claude Code session by `session_id`. |

Codex config block:

```toml
[mcp_servers.claude_bridge]
args = ["/Users/steve/gemp-swccg-public/tools/claude-bridge-mcp/claude_bridge_mcp.py"]
command = "python3"
startup_timeout_sec = 30
```

Verified earlier:

| Check | Result |
|---|---|
| Python compile | Pass |
| MCP initialize | Pass |
| MCP tools/list | Shows `claude_status`, `claude`, `claude_reply` |
| `claude_status` | Works, reports auth missing |
| `claude` test call | Reaches Claude CLI and returns auth failure with a session id |
| Codex TOML parse | Pass |

Known blocker:

```bash
claude auth status --json
```

returned:

```json
{"loggedIn":false,"authMethod":"none","apiProvider":"firstParty"}
```

Steve must run:

```bash
claude auth login
```

Then start a fresh Codex session so the new MCP server loads. The current session did not see `claude_bridge` through tool discovery. Expected, because MCP servers load at session start. Use the async mailbox until the bridge is visible and authenticated.

## 6. What shipped today, per K-2 current handoff

These are local only. Nothing pushed.

| Commit | Fix | Status |
|---|---|---|
| `4b76cb611` | V156 STACK-MATH solo doctrine: weak solos JOIN by site total ability >= 4, not headcount | Deployed, pending live confirmation |
| `692fec3cf` | V177/V82.1 objective-pull parser anchored on the pull verb | Deployed, worked live for Endor pull |
| `f866d98e7` | Six Endor-game fixes: V58/V67w maintenance floor, V153 thin reserve, V24.15 effective drain, V193 Bunker plan | Deployed, pending live confirmation |
| `e97003fa2` | V193 generalized flip-gate steer via ObjectiveAnalyzer site/card getters | Deployed, pending live confirmation |
| `e482cc5ae` | AI-to-AI protocol and Claude bridge MCP | Committed |

Markers to watch in `logs/gemp-swccg.log`:

```text
V156 JOIN-GROUP
V58 MAINTENANCE FLOOR
V153 THIN RESERVE
V24.15 EFFECTIVE DRAIN
V193 FLIP-GATE CONTROL
```

Endor payoff to verify: a body enters Endor: Bunker, then Rando deploys Establish Secret Base and assembles the flip instead of holding the card uselessly.

## 7. Likely next Rando queue

From K-2's current handoff:

| Priority | Task | Notes |
|---|---|---|
| 1 | Confirm today's fixes in Steve's next games | Grep markers above. Do not code first. Verify the shipped fixes fired. |
| 2 | Possible PLAYBOOKS consolidation | Steve is weighing this. Consolidate objective-specific deploy logic into ObjectiveAnalyzer data plus general deploy mechanism. High dominance risk. |
| 3 | Reorg backlog | `shields-response-5`, confirmed-medium overlap rows, doc corrections. |

If Steve asks for PLAYBOOKS consolidation:

| Requirement | Reason |
|---|---|
| One objective at a time | Avoid cross-objective dominance bugs. |
| Read actual card source | No text-scan hallucinations. |
| Boundary math first | Rando scores are additive. |
| Mirror rando -> chosenone | Same behavior unless structure absent. |
| Update both changelogs same session | Breadcrumb law. |
| Use mailbox/bridge for K-2 second opinion | Especially on magnitudes. |

## 8. Build/deploy rules

Use the current build doc, but these commands matter:

```bash
docker exec gemp_swccg_app_1 bash -c "cd /opt/gemp-swccg/src && mvn -q -pl gemp-swccg-server -am compile > /tmp/c.log 2>&1; echo MVN_EXIT=\$?; grep -c '\[ERROR\]' /tmp/c.log"
bin/gemp reload-ai
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:17001/gemp-swccg/
```

Byte-verify markers inside `src/gemp-swccg-async/target/web.jar` before claiming deployed.

Deploy gotcha from K-2: `reload-ai` can report success while the app container exits shortly after startup. Always confirm HTTP 200 after deploy. If the app is down, K-2 recovered with:

```bash
docker compose -f src/docker-compose.yml up -d
```

Then manually flip operational/gameplay switches as needed. Do not deploy over a live game.

## 9. Non-negotiable landmines

| Never do | Why |
|---|---|
| `git push` | Standing order: local commits only. |
| `docker compose down -v` | DB destruction risk. |
| `rm -rf database/` | DB destruction risk. |
| `bin/gemp reset-db` | DB destruction risk. |
| Unpin `mariadb:11.8.6` | Known deployment landmine. |
| Edit dead `if (false)` Rando blocks | Ships nothing. |
| Add a new V-tag for an existing-rule tweak | Steve explicitly wants consolidation, not contradiction. |

## 10. Files Alfred touched recently

| File | Meaning |
|---|---|
| `AGENTS.md` | Updated with async mailbox and Claude bridge notes. |
| `Handoffs/AI_PROTOCOL.md` | Shared coordination protocol. |
| `Handoffs/AI_MAILBOX.md` | Repo audit mailbox, now committed through K-2 sync and later updates. |
| `tools/claude-bridge-mcp/` | Alfred's Claude bridge MCP, committed at `e482cc5ae`. |
| `Handoffs/ALFRED_HANDOFF_2026-07-07_claude-bridge.md` | Prior Alfred handoff, now partly superseded by this one. |

This handoff supersedes `Handoffs/ALFRED_HANDOFF_2026-07-07_claude-bridge.md` for fresh Alfred startup, but that earlier file has more detail on how the bridge was built and probed.

## 11. End-of-session habit

Before final response or handoff:

```bash
python3 ~/claude-codex-mailbox/mailbox.py check --as codex --mark
git status --short --branch
```

If Claude left mail, answer or report it. If Git moved, trust Git. If both happen, congratulations, the droids are reproducing state.

