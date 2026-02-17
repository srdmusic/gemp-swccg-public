# WS/Auth Migration Notes

This branch is intentionally scoped to transport/auth foundation changes only.

## Scope

- Add JWT auth endpoints (`/api/auth/*`).
- Add websocket endpoint (`/gemp-swccg-server/ws`) for `game`, `hall`, and `chat`.
- Keep existing legacy handlers and UI behavior intact where possible.

## Why `channelRead0` Handles Close/Ping/Text

`SwccgoWebSocketFrameHandler` is the transport boundary:

- `CloseWebSocketFrame`: complete websocket close handshake and clean up session.
- `PingWebSocketFrame`: reply with `PongWebSocketFrame` for keepalive.
- `TextWebSocketFrame`: pass payload to channel session (`game`, `hall`, `chat`).

Keeping this split avoids leaking Netty frame logic into gameplay/hall/chat code.

## Why Game WS Payloads Stay XML

The existing game UI expects XML (`gameState`, `update`) and has deep logic tied to
those structures.

To keep the migration small:

- game websocket sends legacy XML directly,
- client consumes that XML directly (no JSON-to-XML rebuild for game state),
- websocket control messages (ack/error) remain small JSON envelopes.

This keeps websocket transport and JWT auth in scope without rewriting game UI parsing.

## WS-Only Fallback Policy (This Branch)

On websocket-capable browsers, the client stays websocket-first and does not re-enter
HTTP long polling for game/hall/chat updates. If websocket fails, UI gets the existing
error handling path.

## AI/Bot Behavior

No AI logic changes are intended as part of the WS/auth foundation scope.
Any AI/bot changes should be handled in separate PRs.
