# Codex Gate: Trace Stage 4A2a Outer Tracker Lifecycle

Date: 2026-07-13
Commit: `08e544f5050d6a85b2dadcb25e5cb73436ace2b6`
Parent: `0bad33598f6b32322b35e58b70140c2d7d76f133`
Packet: `Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md`
Verdict: `ADVANCE-INERT`
Mailbox result: `m00434`
Deployment: held
Push: held

## Scope

The 18-file commit is confined to the two outer `DecisionTracker` lifecycle observations,
their common typed trace model, mirrored tests, and required handoff/changelog bookkeeping.
It contains no F1/F2 engine, evaluator, score, objective, deploy, routing, or capture-enablement
change.

## Contract Review

- `DecisionTrackerLifecycleSnapshot` contains the complete decision snapshot plus `lastTurn` and
  `lastStateHash`. It does not contain `lastPhase`.
- `TrackerUpdateStateEvent` and `TrackerClearEvent` are separate closed records. They reject the
  shared heuristic owner and inconsistent mutation outcomes.
- Each bot has exactly one outer `updateState(...)` hook and one new-game `clear()` hook.
- The inherited `HeuristicAiBase` tracker remains untouched and reserved for 4A2b.
- The default sink remains `NoOpTraceSink`; production capture remains disabled.
- The legacy mutator executes once outside the instrumentation failure path. Snapshot and append
  failures cannot skip or repeat it.

## Independent Evidence

- Focused new-fixture run: 36 tests, 0 failures, 0 errors, 0 skips.
- Expanded trace/tie/finalizer run: 171 tests, 0 failures, 0 errors, 1 expected F1 skip.
- Independent affected-module run: 787 tests, 0 failures, 0 errors, 26 skips.
- `mvn -pl gemp-swccg-server -am package -DskipTests -q`: exit 0.
- `git diff --check 0bad33598..08e544f50`: clean.
- Rando/ChosenOne normalized production-hook hash:
  `7a51cb926a9a16a5f1ca5c9f57d9541ccce9d54104daad9a63d1603c1ded74a8`.
- Rando/ChosenOne normalized lifecycle-fixture hash:
  `24a91d5ece93a1263c73dda6aa082cfb323d378ab3b0e0dc568374b5990ae77d`.
- Rando/ChosenOne normalized bot-boundary-fixture hash:
  `59902923eba130168758ff67826543e898da6ddaa33a5989190db80e7346f42a`.
- Parent/current mirrored `DecisionTracker.updateState` bytecode hash:
  `f26e1bcfc200a4f0883ad3e13e81aa8f4455bcfc8e4d51cb300068b269a933c4`.
- Parent/current mirrored `DecisionTracker.clear` bytecode hash:
  `2dad60bb8b81016102569461db93d124ae6c403f4ccc3623514c853cc0bdcf7e`.

## Required Debt

The independent reviewer found no injected snapshot or state-event append failure fixture. This is
nonblocking for the inert slice because capture is disabled and source structure proves the legacy
mutator remains outside every instrumentation `try/catch`. The fault-injection fixture is required
before any capture-enablement gate may advance.

## Gate Result

`08e544f50` advances as inert trace infrastructure. Stage 4A2b may open from
`Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md`. This does not authorize
F1/F2, interceptor, behavior-cutover, deployment, or push work. K-2 must update the live continuity
handoff with this verdict, the actual F1/F2 lane state, exact HEAD, mailbox watermark, and next
command before the next implementation milestone. Codex sent this result and the handoff-update
requirement to K-2 in `m00434`.
