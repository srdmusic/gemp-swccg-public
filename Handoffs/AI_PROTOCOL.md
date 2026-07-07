# AI Protocol

Purpose: provide a low-friction way for Claude Code K-2 and Codex Alfred to coordinate through the repo when no direct tool bridge exists.

## When To Use

- Use this protocol when Steve asks one agent to coordinate with the other agent.
- Use it when a handoff, task, or bug investigation needs review from the other agent.
- Do not use it for routine solo work. A mailbox nobody needed is just a smaller inbox, which is still bad.

## Files

| File | Owner | Purpose |
|---|---|---|
| `Handoffs/AI_PROTOCOL.md` | Shared | This runbook |
| `Handoffs/AI_MAILBOX.md` | Shared | Append-only message thread |
| `tools/claude-bridge-mcp/` | Alfred | Narrow MCP bridge from Codex to Claude Code |

## Rules

- Append new messages to `Handoffs/AI_MAILBOX.md`. Do not rewrite prior messages except to fix obvious typos in your own most recent entry.
- Start each message with date, local time, sender, recipient, and status.
- Keep messages evidence-first. Include commit hashes, log line numbers, replay ids, file paths, and exact V-tags when relevant.
- Mark what you need from the other agent. Do not make them infer the task from archaeology.
- If a question is resolved, append a `RESOLVED` message with the answer and evidence.
- If the other agent gives a claim about card text, verify it against the actual card source or `mcp-gemp-client/card_cache.json` before coding. Trust, but verify. Mostly verify.
- For Rando code work, the standing rules still win: read the handoff and build docs, read real card source, boundary math before score changes, mirror rando to chosenone, update both changelogs, never push.

## Claude Bridge MCP

Alfred can use a narrow MCP bridge to call Claude Code from Codex once the shell Claude CLI is authenticated.

| Tool | Purpose |
|---|---|
| `claude_status` | Check Claude CLI version and auth |
| `claude` | Start a new Claude Code session |
| `claude_reply` | Resume a Claude Code session by `session_id` |

Codex config:

```toml
[mcp_servers.claude_bridge]
args = ["/Users/steve/gemp-swccg-public/tools/claude-bridge-mcp/claude_bridge_mcp.py"]
command = "python3"
startup_timeout_sec = 30
```

Auth requirement:

```bash
claude auth status --text
claude auth login
```

Default bridge calls are read-only: `Read`, `Grep`, and `Glob`. The mailbox remains the durable fallback and audit trail.

## Message Template

```md
## YYYY-MM-DD HH:MM PT - Sender -> Recipient - STATUS

Topic:
Request:
Evidence:
Files:
Risks:
Needed:
```

## Status Values

| Status | Meaning |
|---|---|
| `OPEN` | Needs a reply or action |
| `INFO` | Shared context only |
| `BLOCKED` | Cannot proceed without the recipient or Steve |
| `RESOLVED` | The thread is closed |
