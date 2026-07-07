#!/usr/bin/env python3
"""Minimal MCP server that lets Codex call Claude Code safely.

Protocol framing: newline-delimited JSON-RPC, matching the current MCP stdio SDK.
No third-party dependencies. This is deliberate; dependency installs are where small
bridges go to become tiny empires.
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
from typing import Any


SERVER_NAME = "claude-bridge"
SERVER_VERSION = "0.1.0"
DEFAULT_CWD = "/Users/steve/gemp-swccg-public"
DEFAULT_ALLOWED_TOOLS = ["Read", "Grep", "Glob"]
DEFAULT_TIMEOUT_MS = 600_000
MAX_TIMEOUT_MS = 900_000


def respond(message_id: Any, result: dict[str, Any]) -> None:
    write({"jsonrpc": "2.0", "id": message_id, "result": result})


def error(message_id: Any, code: int, message: str, data: Any = None) -> None:
    payload: dict[str, Any] = {"jsonrpc": "2.0", "id": message_id, "error": {"code": code, "message": message}}
    if data is not None:
        payload["error"]["data"] = data
    write(payload)


def write(payload: dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(payload, separators=(",", ":")) + "\n")
    sys.stdout.flush()


def text_result(text: str, structured: dict[str, Any] | None = None, is_error: bool = False) -> dict[str, Any]:
    result: dict[str, Any] = {
        "content": [{"type": "text", "text": text}],
        "isError": is_error,
    }
    if structured is not None:
        result["structuredContent"] = structured
    return result


def tool_schema() -> list[dict[str, Any]]:
    claude_common_props = {
        "prompt": {
            "type": "string",
            "description": "Prompt to send to Claude Code.",
        },
        "cwd": {
            "type": "string",
            "description": f"Working directory for Claude. Defaults to {DEFAULT_CWD}.",
        },
        "model": {
            "type": "string",
            "description": "Optional Claude model alias or full model id, for example 'sonnet' or 'opus'.",
        },
        "allowed_tools": {
            "type": "array",
            "items": {"type": "string"},
            "description": "Claude Code tools to expose. Defaults to read-only Read, Grep, Glob. Use [] for no tools.",
        },
        "max_budget_usd": {
            "type": "number",
            "minimum": 0,
            "maximum": 10,
            "description": "Optional Claude CLI budget cap. Defaults to 0.25.",
        },
        "timeout_ms": {
            "type": "integer",
            "minimum": 1_000,
            "maximum": MAX_TIMEOUT_MS,
            "description": f"Subprocess timeout in milliseconds. Defaults to {DEFAULT_TIMEOUT_MS}.",
        },
    }

    return [
        {
            "name": "claude_status",
            "description": "Check whether the local Claude CLI is installed and authenticated.",
            "inputSchema": {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "type": "object",
                "properties": {},
                "additionalProperties": False,
            },
            "annotations": {"readOnlyHint": True, "destructiveHint": False, "idempotentHint": True},
        },
        {
            "name": "claude",
            "description": "Start a new read-only Claude Code session and return Claude's result plus session_id.",
            "inputSchema": {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "type": "object",
                "properties": claude_common_props,
                "required": ["prompt"],
                "additionalProperties": False,
            },
            "annotations": {"readOnlyHint": False, "destructiveHint": False, "idempotentHint": False, "openWorldHint": True},
        },
        {
            "name": "claude_reply",
            "description": "Resume an existing Claude Code session by session_id and return Claude's result.",
            "inputSchema": {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "type": "object",
                "properties": {
                    "session_id": {
                        "type": "string",
                        "description": "Claude Code session id returned by a previous claude call or shared by K-2.",
                    },
                    **claude_common_props,
                },
                "required": ["session_id", "prompt"],
                "additionalProperties": False,
            },
            "annotations": {"readOnlyHint": False, "destructiveHint": False, "idempotentHint": False, "openWorldHint": True},
        },
    ]


def run_process(args: list[str], cwd: str | None = None, timeout_ms: int = DEFAULT_TIMEOUT_MS) -> dict[str, Any]:
    timeout_ms = min(max(int(timeout_ms), 1_000), MAX_TIMEOUT_MS)
    try:
        completed = subprocess.run(
            args,
            cwd=cwd or DEFAULT_CWD,
            text=True,
            capture_output=True,
            timeout=timeout_ms / 1000,
            env=os.environ.copy(),
        )
    except FileNotFoundError as exc:
        return {
            "ok": False,
            "returncode": 127,
            "stdout": "",
            "stderr": str(exc),
            "json": None,
        }
    except subprocess.TimeoutExpired as exc:
        return {
            "ok": False,
            "returncode": 124,
            "stdout": exc.stdout or "",
            "stderr": f"Timed out after {timeout_ms} ms\n{exc.stderr or ''}",
            "json": None,
        }

    parsed = None
    stdout = completed.stdout.strip()
    if stdout:
        try:
            parsed = json.loads(stdout)
        except json.JSONDecodeError:
            parsed = None

    return {
        "ok": completed.returncode == 0,
        "returncode": completed.returncode,
        "stdout": completed.stdout,
        "stderr": completed.stderr,
        "json": parsed,
    }


def claude_status() -> dict[str, Any]:
    version = run_process(["claude", "--version"], timeout_ms=15_000)
    auth = run_process(["claude", "auth", "status", "--json"], timeout_ms=15_000)
    structured = {
        "version": version.get("stdout", "").strip(),
        "auth": auth.get("json"),
        "auth_returncode": auth.get("returncode"),
        "auth_stderr": auth.get("stderr", "").strip(),
    }
    lines = [
        f"Claude CLI: {structured['version'] or 'not found'}",
        f"Auth: {json.dumps(structured['auth'], separators=(',', ':')) if structured['auth'] else 'unknown'}",
    ]
    if auth.get("returncode") != 0:
        lines.append("Fix: run `claude auth login` in a terminal, then restart Codex if needed.")
    return text_result("\n".join(lines), structured=structured, is_error=auth.get("returncode") != 0)


def build_claude_command(params: dict[str, Any], resume: bool = False) -> tuple[list[str], str, int]:
    prompt = params.get("prompt")
    if not isinstance(prompt, str) or not prompt.strip():
        raise ValueError("prompt must be a non-empty string")

    cwd = params.get("cwd") or DEFAULT_CWD
    if not isinstance(cwd, str):
        raise ValueError("cwd must be a string")

    timeout_ms = int(params.get("timeout_ms") or DEFAULT_TIMEOUT_MS)
    max_budget = params.get("max_budget_usd", 0.25)
    allowed_tools = params.get("allowed_tools", DEFAULT_ALLOWED_TOOLS)
    if allowed_tools is None:
        allowed_tools = DEFAULT_ALLOWED_TOOLS
    if not isinstance(allowed_tools, list) or not all(isinstance(tool, str) for tool in allowed_tools):
        raise ValueError("allowed_tools must be an array of strings")

    cmd = [
        "claude",
        "-p",
        "--output-format",
        "json",
        "--permission-mode",
        "dontAsk",
        "--max-budget-usd",
        str(max_budget),
    ]

    model = params.get("model")
    if model:
        if not isinstance(model, str):
            raise ValueError("model must be a string")
        cmd.extend(["--model", model])

    if resume:
        session_id = params.get("session_id")
        if not isinstance(session_id, str) or not session_id.strip():
            raise ValueError("session_id must be a non-empty string")
        cmd.extend(["--resume", session_id])

    cmd.extend(["--tools", ",".join(allowed_tools), "--"])
    cmd.append(prompt)
    return cmd, cwd, timeout_ms


def call_claude(params: dict[str, Any], resume: bool = False) -> dict[str, Any]:
    cmd, cwd, timeout_ms = build_claude_command(params, resume=resume)
    result = run_process(cmd, cwd=cwd, timeout_ms=timeout_ms)
    parsed = result.get("json")
    is_error = result["returncode"] != 0 or bool(parsed and parsed.get("is_error"))
    structured = {
        "command": redact_command(cmd),
        "cwd": cwd,
        "returncode": result["returncode"],
        "stderr": result.get("stderr", ""),
        "claude": parsed,
        "stdout": None if parsed else result.get("stdout", ""),
    }

    if parsed:
        text = json.dumps(parsed, indent=2)
    else:
        text = (result.get("stdout", "") + "\n" + result.get("stderr", "")).strip()
    if is_error and "Not logged in" in text:
        text += "\n\nFix: run `claude auth login` in a terminal. The bridge is installed; Claude CLI auth is the missing motivator."

    return text_result(text, structured=structured, is_error=is_error)


def redact_command(cmd: list[str]) -> list[str]:
    redacted = list(cmd)
    if redacted:
        redacted[-1] = "<prompt>"
    return redacted


def handle_tool_call(message_id: Any, params: dict[str, Any]) -> None:
    name = params.get("name")
    args = params.get("arguments") or {}
    if not isinstance(args, dict):
        error(message_id, -32602, "arguments must be an object")
        return

    try:
        if name == "claude_status":
            respond(message_id, claude_status())
        elif name == "claude":
            respond(message_id, call_claude(args, resume=False))
        elif name == "claude_reply":
            respond(message_id, call_claude(args, resume=True))
        else:
            error(message_id, -32601, f"Unknown tool: {name}")
    except Exception as exc:  # noqa: BLE001 - MCP should return errors, not crash.
        error(message_id, -32603, str(exc))


def handle(message: dict[str, Any]) -> None:
    method = message.get("method")
    message_id = message.get("id")

    if method == "initialize":
        respond(
            message_id,
            {
                "protocolVersion": "2024-11-05",
                "capabilities": {"tools": {}},
                "serverInfo": {"name": SERVER_NAME, "version": SERVER_VERSION},
            },
        )
    elif method == "tools/list":
        respond(message_id, {"tools": tool_schema()})
    elif method == "tools/call":
        params = message.get("params") or {}
        if not isinstance(params, dict):
            error(message_id, -32602, "params must be an object")
        else:
            handle_tool_call(message_id, params)
    elif method == "ping":
        respond(message_id, {})
    elif method and method.startswith("notifications/"):
        return
    else:
        error(message_id, -32601, f"Unknown method: {method}")


def main() -> None:
    for line in sys.stdin:
        if not line.strip():
            continue
        try:
            message = json.loads(line)
        except json.JSONDecodeError as exc:
            error(None, -32700, f"Invalid JSON: {exc}")
            continue
        handle(message)


if __name__ == "__main__":
    main()
