# Codex Gate: Tie Determinism `5df276c1b`

Date: 2026-07-13
Reviewer: Codex/Alfred
Verdict: tie delta and focused fixtures `ADVANCE`; aggregate deployment remains `HOLD`

## Verified

- The commit changes only both `CombinedEvaluator` mirrors plus the two history documents.
- Rando and ChosenOne sources are identical after package normalization.
- The current branch compiles with
  `mvn -q -pl gemp-swccg-server -am package -DskipTests`.
- `LinkedHashMap` preserves the first insertion for an action id across later score merges.
- Evaluator registration order is explicit and stable; each evaluator's offered-action order supplies
  the second ordering key.
- The normal final winner and DPS bucket winner replace the incumbent only when
  `Float.compare(candidate, best) > 0`, so exact ties retain the first candidate.
- The V67bc non-bucket epilogue already uses strict `>` and therefore retains first-seen order.
- Non-tied score selection, hard-veto filtering, bucket precedence, and pass thresholds are unchanged.

## Focused fixture completion

- Commit `5240f36c6` adds four focused tests per bot through the package-visible scripted-evaluator
  seam.
- Normal final selection, duplicate-id merge, DPS bucket selection, and all-vetoed forced choice
  each preserve the first offered candidate on an exact tie.
- Every case includes a one-ULP control proving that a later strictly higher score still replaces
  the incumbent.
- Detached-commit verification ran all eight tests: 8 passed, 0 failures/errors/skips.
- Rando and ChosenOne test sources are identical after package normalization.
- `git diff --check 55c22fdde..5240f36c6` is clean.

The intentional winner delta for historical exact ties should be captured as an explicit fixture
contract change. The corrected log harvester is not sufficient proof because it captures only the
logged top five and has no executable candidate-order oracle.

Boundary: this gate clears the tie-determinism increment only. It does not clear trace capture,
phase cutover, aggregate build, live-game validation, or deployment.
