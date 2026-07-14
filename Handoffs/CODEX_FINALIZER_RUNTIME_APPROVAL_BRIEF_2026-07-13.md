# Approval Brief: Accepted AI Response Lifecycle

Date: 2026-07-13
Baseline: `ad8f593857c443aeecce37b9e397a792e68dc914`
Status: `APPROVED BY STEVE: 2026-07-13. JAVA PHASE RELEASED.`
Technical packet: `Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md`

## Decision Requested

Approve one coherent production-code phase that changes when Rando, ChosenOne, and Curator commit
their outer decision history. The commit will happen only after the game engine accepts the AI's
response, instead of before validation.

Approval permits the production files and focused tests listed below. It does not authorize a
deployment, live reload, game, replay, sandbox, or broader phase-logic rewrite.

Suggested approval text:

> I approve the accepted AI response lifecycle phase described in this brief, including the
> SwccgAiController and SwccgGameMediator changes. Complete it as one edit phase, one offline
> verification pass, and one commit. Do not deploy or run games yet.

## Why It Is Needed

| Current problem | Result |
|---|---|
| The AI returns only a raw response string. | The engine cannot carry acceptance work with the response. |
| Rando records some tracker and strategy state before engine validation. | A rejected response can be remembered as though it happened. |
| The mediator may reject and retry after that early mutation. | Retry history and decision traces can become inaccurate. |
| The new response finalizer has no live acceptance boundary. | It cannot safely become the owner of ACTIVATE, CONTROL, or later phases. |

## What Changes

| Area | Plain-language change |
|---|---|
| AI result | Add a small typed envelope containing either an exact wire response or a typed rejection. |
| Engine mediator | Validate the response first, then send exactly one accepted or rejected callback. |
| Rando and ChosenOne | For mediator calls only, delay the outer tracker and strategy commit until acceptance. |
| Curator | Forward the engine result to wrapped Rando and record the actual Curator override if accepted. |
| Trace | Keep one decision trace open until acceptance or rejection, then close it exactly once. |
| Finalizer | Add a pure adapter into the typed envelope. No phase uses it yet. |
| Existing callers | Keep the current `String decide(...)` method and its direct-call behavior. |

## Expected Production Scope

| File or class | Purpose | Guard |
|---|---|---|
| `SwccgAiController.java` | Add the backward-compatible mediator result and lifecycle methods. | Existing `decide()` remains available. |
| `SwccgGameMediator.java` | Add AI-only validation and disposition ordering. | Human decision path stays unchanged. |
| New `AiDecisionResult.java` | Carry a closed typed response or rejection. | No callback closures or mutable engine state. |
| `RandoCalAi.java` | Defer only its outer common mutation for mediator calls. | Direct calls and response choice stay unchanged. |
| `TheChosenOneAi.java` | Mirror Rando's lifecycle boundary. | Normalized parity with Rando is required. |
| `CuratorAi.java` | Forward lifecycle and preserve the accepted override response. | Tests use no Ollama or network calls. |
| Finalizer and trace support | Adapt finalized responses and record disposition evidence. | No phase-owner cutover in this work. |
| Focused tests and changelogs | Freeze lifecycle counts, ordering, parity, and residual behavior. | Exact owned paths only. |

## What Must Not Change

| Protected behavior | Required result |
|---|---|
| AI wire responses | Identical |
| Evaluator scores and deploy weights | Identical |
| Phase routes and candidate ordering | Identical |
| RNG draws | Identical |
| Existing one-retry policy | Identical |
| Human mediator path | Identical |
| Direct `decide()` callers | Identical |
| Legacy safety copies | Remain in place |
| ACTIVATE, CONTROL, and other phase ownership | No cutover |
| Live environment | No deploy, reload, restart, or game execution |

## Main Risks And Gates

| Risk | Required gate |
|---|---|
| Accepted or rejected callback fires twice. | Count fixtures and static source proof require exactly one disposition per attempt. |
| A rejected action still changes outer tracker state. | Rejection fixtures require zero accepted mutation. |
| A trace leaks into another game or retry. | Mode-aware close plus mediator attempt `finally`; every fixture ends with no active trace. |
| Curator records Rando's suggestion instead of its override. | Pure override test requires the actual engine-accepted Curator response. |
| Curator consult times out or throws while a trace is open. | Timeout fallback and unchecked-failure fixtures prove correct close and no leaked mutation. |
| Shared heuristic fallback state is mistaken for migrated state. | Its existing pre-accept mutations remain an explicit tested residual. |
| Scope expands into gameplay logic. | Any score, route, RNG, response, phase, or retry change is a hard stop. |

## Verification After Editing

| Step | Limit |
|---|---|
| Edit | One coherent phase. No tests during the edit. |
| Automated check | One focused Maven pass for mediator, lifecycle, Curator, finalizer, and trace tests. |
| Static check | Diff, changed paths, callback ordering, close ownership, parity, and no-finalizer-consumer proofs. |
| Commit | One phase commit only after every gate passes. |
| Independent gate | Codex reviews the complete commit and evidence before any later phase starts. |
| Runtime testing | Not part of this approval. No games, VTS, sandbox, replay, or live server. |

## Review Result

K-2 completed a source review. Its council agreed with the design after adding three lifecycle guards
and the Curator no-network test seam. Mailbox evidence: `m00568` and `m00571`.

Steve approved this phase and the remaining planned phase sequence on 2026-07-13. Production Java
implementation is released. Deployment remains gated until the complete code and verification
sequence passes.
