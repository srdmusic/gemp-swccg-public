# Alfred Handoff - 2026-07-07 - Claude Bridge + Onboarding State

Audience: next Codex session, a.k.a. Alfred. Steve does not want context loss. Understandable. We were carrying rather a lot of explosives.

Repo: `/Users/steve/gemp-swccg-public`
Branch: `rando-consolidation-2026-06-23`
Timestamp: `2026-07-07 10:37 PDT`
Current HEAD at handoff: `95d4dc172` (`Mailbox: K-2 -> Alfred sync to HEAD (today's Rando fixes)`)

## 1. Mandatory onboarding order

Read these before touching anything:

| Order | File | Why |
|---|---|---|
| 1 | `AGENTS.md` | Project persona/rules. User explicitly renamed this Codex persona Alfred; K-2 is Claude. Still use K-2SO snark, concise, no em dashes in prose. |
| 2 | `Handoffs/K2_CODEX_HANDOFF_2026-07-07_audit-solo-pull.md` | Still the base entry point, but stale against Git. |
| 3 | `resources/BUILD_AND_DEPLOY.md` | Build/deploy truth. Four gates: compiles, in jar, loaded, fired. |
| 4 | `Handoffs/AI_MAILBOX.md` | K-2 has already used the mailbox to sync Alfred to newer HEAD. |
| 5 | `resources/AI_CHANGELOG.md` by V-tag | Do not read all of it. Grep by tag/task. |

First commands for new Alfred:

```bash
git log --oneline -15
git status --short --branch
tail -n 120 logs/gemp-swccg.log
```

Git is truth. The old handoff said HEAD was `692fec3cf`; that is stale. Current HEAD is `95d4dc172`.

## 2. Standing rules that matter most

| Rule | Meaning |
|---|---|
| Local commits only | Never push unless Steve explicitly asks. |
| Rando scoring discipline | Old rules get dominated, not deleted. Boundary math before any score magnitude change. |
| Actual card source | Read the real card Java file or `mcp-gemp-client/card_cache.json` before card-text scans. Set8 vs set207 virtual bit us. |
| Comment out, do not delete | Superseded code should remain as the revert path. |
| Adjust V-tags in place | Do not mint a new tag for a tweak to an existing rule. |
| Mirror rando to chosenone | If structure exists. If not, say so. |
| Both changelogs same session | `resources/AI_CHANGELOG.md` and `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`. |
| Compile in container | Host has no JRE. Check real Maven exit code. |
| Byte-verify jar | Presence in source is not proof of deployment. |
| Do not deploy over live game | Check logs/table state first. |

## 3. Git state at handoff

Recent commits:

```text
95d4dc172 Mailbox: K-2 -> Alfred sync to HEAD (today's Rando fixes)
e97003fa2 Rando V193 (in place): move flip-gate CARD into the objective logic - general flip-gate steer
f866d98e7 Rando: six Endor-game fixes - maintenance floor, thin-reserve, effective-drain, Endor Bunker plan
718228cf9 Add paste-ready Codex onboarding prompt (persona + rules + points at current handoff)
fdeae7c39 Remove redundant nested AGENTS.md clones (root is the single Codex authority)
27bfd1b59 Fix Codex memory: AGENTS.md points at the current handoff, drops fabricated .Codex paths
5878a264b Handoff (K2 + Codex): reorg audit + solo stack-math fix + Endor objective-pull parser fix
692fec3cf Rando V177/V82.1 parser fix: anchor objective pull-targets on the pull verb (Endor Operations)
```

Dirty/untracked state when this file was created:

```text
 M .claude/skills/work-verifier/history.md
 M AGENTS.md
 M Handoffs/AI_MAILBOX.md
?? .agents/
?? Handoffs/AI_PROTOCOL.md
?? mcp-gemp-client/gemp_mcp.py
?? resources/Rando_Consolidation_Plan_2026-06-29.xlsx
?? resources/Rando_Issues_2026-06-29.xlsx
?? tools/
```

Do not revert unrelated dirty state. Some of it is K-2/Claude activity. Some is Alfred's bridge work. Treat it like live ordnance, because apparently it is.

## 4. What happened this Alfred session

| Topic | Result |
|---|---|
| Onboarded | Read root `AGENTS.md`, current K2/Codex handoff, build/deploy doc, and relevant changelog V-tags. |
| Verified handoff staleness | Git HEAD was already beyond `692fec3cf`; K-2 later advanced/synced to `95d4dc172`. |
| Checked live logs | Before `f866d98e7`/`e97003fa2` live validation, V177 parser was partly confirmed: Rando successfully took Ominous Rumors from Endor Operations after the parser fix. No post-HEAD live decisions were available in the current log at that time. |
| Added mailbox protocol | `Handoffs/AI_PROTOCOL.md` created and `AGENTS.md` updated with AI-to-AI coordination rules. |
| K-2 used mailbox | Commit `95d4dc172` added a K-2 -> Alfred sync entry in `Handoffs/AI_MAILBOX.md`. Read it. |
| Built Claude Bridge MCP | Added a narrow MCP server in `tools/claude-bridge-mcp/` so Codex can call Claude Code from future sessions. |

## 5. Claude Bridge MCP

Steve asked whether building a GPT would be better than MCP. Answer given: no, MCP is the right layer for local Alfred <-> K-2 comms. A custom GPT would be a human-facing layer above tools, not a replacement for local tool access.

Bridge files:

| File | Purpose |
|---|---|
| `tools/claude-bridge-mcp/claude_bridge_mcp.py` | Dependency-free stdio MCP server. |
| `tools/claude-bridge-mcp/README.md` | Setup and usage notes. |
| `/Users/steve/.codex/config.toml` | Modified outside repo to register the MCP server. |

Codex config added:

```toml
[mcp_servers.claude_bridge]
args = ["/Users/steve/gemp-swccg-public/tools/claude-bridge-mcp/claude_bridge_mcp.py"]
command = "python3"
startup_timeout_sec = 30
```

Bridge tools:

| Tool | Purpose |
|---|---|
| `claude_status` | Check Claude CLI version/auth. |
| `claude` | Start a new non-interactive Claude Code session. |
| `claude_reply` | Resume an existing Claude Code session by `session_id`. |

Default Claude calls are read-only: `Read`, `Grep`, `Glob`. The wrapper intentionally does not expose raw `claude mcp serve`, because that would hand Codex Claude's whole toolbelt. That is called "unnecessary blast radius" by people who enjoy surviving.

Verified:

| Check | Result |
|---|---|
| `python3 -m py_compile tools/claude-bridge-mcp/claude_bridge_mcp.py` | Pass |
| MCP `initialize` probe | Pass |
| MCP `tools/list` probe | Returns `claude_status`, `claude`, `claude_reply` |
| `claude_status` probe | Works and reports auth missing |
| `claude` probe | Correctly reaches Claude CLI and returns auth failure with a `session_id` |
| `/Users/steve/.codex/config.toml` parse via `tomllib` | Pass |

Current bridge blocker:

```bash
claude auth status --json
```

returns:

```json
{"loggedIn":false,"authMethod":"none","apiProvider":"firstParty"}
```

Steve needs to run:

```bash
claude auth login
```

Then restart Codex or start a fresh Codex session so MCP config reloads. This current Alfred session did not see `claude_bridge` through dynamic tool discovery, which is expected because MCP servers load at session start.

## 6. Mailbox protocol

Files:

| File | Purpose |
|---|---|
| `Handoffs/AI_PROTOCOL.md` | Shared Alfred/K-2 coordination runbook. |
| `Handoffs/AI_MAILBOX.md` | Append-only shared message log. |

Important mailbox state:

- Initial Alfred -> K-2 marker exists.
- K-2 -> Alfred sync exists at `2026-07-07 17:25 PT`, committed as `95d4dc172`.
- Alfred -> K-2 bridge announcement exists at `2026-07-07 10:35 PT`, currently dirty/uncommitted in `Handoffs/AI_MAILBOX.md`.

Protocol summary:

```md
## YYYY-MM-DD HH:MM PT - Sender -> Recipient - STATUS

Topic:
Request:
Evidence:
Files:
Risks:
Needed:
```

Use the mailbox even after the bridge works. The bridge is a commlink. The mailbox is the audit trail. The Empire lost at least one battle by confusing those categories.

## 7. Current Rando queue from K-2 sync

K-2's mailbox update says the following are shipped/deployed today, local only:

| Fix | Commit | Note |
|---|---|---|
| V156 STACK-MATH solo doctrine | `4b76cb611` | Join by site total ability >= 4. |
| V177/V82.1 Endor objective-pull parser | `692fec3cf` | Anchor objective pull targets on pull verb. |
| Six Endor-game fixes | `f866d98e7` | V58/V67w maintenance floor, V153 thin reserve, V24.15 effective drain, V193 Bunker plan. |
| V193 generalized flip-gate steer | `e97003fa2` | Flip-gate card moved into ObjectiveAnalyzer; no card names hardcoded in DeployEvaluator. |

K-2 notes a possible next task: consolidate objective-specific deploy logic into one cohesive playbook section. This has high dominance risk. If Steve assigns it:

- Read the real objective/card sources.
- Grep all current V-tags involved, likely V29, V29.7, V31, V193, V160/V186 playbook arms, V24.15 related objective exemptions.
- Do boundary math before touching scores.
- Ask K-2/Claude for second opinion through `claude_bridge` only after `claude auth login` is fixed, or use mailbox if not.

## 8. Exact next actions for future Alfred

1. Read `AGENTS.md`, `Handoffs/K2_CODEX_HANDOFF_2026-07-07_audit-solo-pull.md`, `resources/BUILD_AND_DEPLOY.md`, and `Handoffs/AI_MAILBOX.md`.
2. Run `git log --oneline -15` and `git status --short --branch`. Trust those over this handoff if they differ.
3. Check whether `claude_bridge` is available in tool discovery. If absent, Codex needs restart/fresh session.
4. If bridge is available, run `claude_status`. If auth is missing, tell Steve to run `claude auth login`.
5. If working on Rando, do not start from the stale handoff queue. Start from K-2 mailbox + current changelog.
6. Do not push. Do not deploy over a live game. Do not touch DB/deck library/schema without Steve.

## 9. Files Alfred intentionally touched

| Path | Status |
|---|---|
| `AGENTS.md` | Modified, added AI-to-AI coordination and Claude Bridge note. |
| `Handoffs/AI_PROTOCOL.md` | New/untracked at last status, protocol/runbook. |
| `Handoffs/AI_MAILBOX.md` | Modified, added bridge announcement. |
| `tools/claude-bridge-mcp/README.md` | New/untracked, bridge docs. |
| `tools/claude-bridge-mcp/claude_bridge_mcp.py` | New/untracked, bridge server. |
| `/Users/steve/.codex/config.toml` | Modified outside repo, registered `claude_bridge`. |

Alfred did not touch Rando Java in this session. If a future diff shows Java from this session, assume timeline contamination or another droid. Both are plausible. Check Git before blaming anyone with a face.

