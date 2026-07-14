# Codex source-comment fact repair packet

Date: 2026-07-13
Owner: K-2 edits production Java; Codex independently gates the clean commit
Frozen source: `15b776301`
Scope: three source-proven comment corrections, no executable change

## Verdict

`ADVANCE` only after the numbered cleanup chain is cleanly committed and gated. This packet repairs
comments that currently contradict their own authoritative card Java sources. It does not authorize
score, predicate, route, card-data, deployment, or push changes.

## Files

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/MaintenanceFacts.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/CardSelectionEvaluator.java`

## Exact correction A: maintenance card IDs

In the `MaintenanceFacts` class header, currently lines 22-23, correct only the two list lines that
attach the wrong titles to `Card13_056` and `Card13_087`.

Replace:

```java
 *   (Card13_059) — Stormtrooper Garrison 1 (Card13_056) — Thok And Thug 2
 *   (Card13_092) — Blizzard 4 1 (Card13_087) — Ap'lek 1 (Card222_002, "Use 1 or
```

with:

```java
 *   (Card13_059) — Blizzard 4 1 (Card13_056) — Thok And Thug 2
 *   (Card13_092) — Stormtrooper Garrison 1 (Card13_087) — Ap'lek 1 (Card222_002, "Use 1 or
```

The frozen two-line predecessor SHA-256 is:

```text
d5888d5ece914c3fd3a2b88f3168ffba60d052714b8e3db61af7c7ea0855e648
```

Authoritative source proof:

- `cards/set13/dark/Card13_056.java` declares `Title: Blizzard 4` and `Use 1 Force to maintain`.
- `cards/set13/dark/Card13_087.java` declares `Title: Stormtrooper Garrison` and `Use 1 Force to
  maintain`.

The parsed costs are already correct because both cards cost 1. Only their names and IDs are crossed
in the documentation.

## Exact correction B: Battle Order drain cost

In both mirrored `CardSelectionEvaluator.java` files, currently line 7905, replace only:

```java
// Battle Order also costs Rando 1 Force per drain — net negative.
```

with:

```java
// Battle Order/Plan normally requires Rando to use 3 Force per drain; occupation
// waives that cost, and Battle Plan also suppresses the Battle Order modifier.
```

The frozen predecessor-line SHA-256, identical in both bots, is:

```text
9ab1ccc5aa2ba0f1c892116596af690fe73c34a8e2d70f7d848d4944ec5bf378
```

Authoritative source proof:

- `cards/set8/dark/Card8_118.java` requires `use 3 Force` to initiate a Force drain unless the
  player occupies both battleground theaters; Battle Plan on table also suppresses this modifier.
- `cards/set13/dark/Card13_054.java` uses the same `use 3 Force` requirement and both exceptions.

The source proves the cost and exceptions, not a universal "net negative" strategic assessment.

## Required gate

1. Exact diff contains only these three comment corrections plus required changelog/history
   bookkeeping.
2. Both mirrored `CardSelectionEvaluator` edit streams are identical after package normalization.
3. No executable Java token changes.
4. `git diff --check` passes.
5. Parent and candidate affected-module packages pass in isolated clean worktrees.
6. Parent and candidate `javap -p -c -s -constants` output is identical for `MaintenanceFacts` and
   both `CardSelectionEvaluator` classes.
7. The expanded focused trace/tie/V191 suite passes.
8. No deployment and no push.
