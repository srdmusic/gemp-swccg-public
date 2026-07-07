# Claude Bridge MCP

Small stdio MCP server that gives Codex a narrow commlink to Claude Code.

It intentionally exposes a tiny surface instead of the raw `claude mcp serve` toolbelt.

## Tools

| Tool | Purpose |
|---|---|
| `claude_status` | Check Claude CLI auth and version |
| `claude` | Start a new non-interactive Claude Code session |
| `claude_reply` | Resume an existing Claude Code session by `session_id` |

## Auth Requirement

The bridge uses the local `claude` CLI. The shell CLI must be logged in:

```bash
claude auth status --text
claude auth login
```

If auth is missing, the MCP tool returns Claude's JSON error instead of pretending the commlink works. A very advanced feature called "not lying."

## Codex Config

Add this to `~/.codex/config.toml`:

```toml
[mcp_servers.claude_bridge]
command = "python3"
args = ["/Users/steve/gemp-swccg-public/tools/claude-bridge-mcp/claude_bridge_mcp.py"]
startup_timeout_sec = 30
```

Restart the Codex session after editing config so the tool list reloads.

## Defaults

`claude` and `claude_reply` run Claude with:

- `--print`
- `--output-format json`
- `--permission-mode dontAsk`
- read-only tools: `Read`, `Grep`, `Glob`

Pass `allowed_tools: []` for no tools, or a narrower read-only list if needed.

