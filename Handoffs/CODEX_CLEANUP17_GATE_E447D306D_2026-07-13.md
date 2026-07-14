# Cleanup 1.7 Gate: `e447d306d`

Date: 2026-07-13
Reviewer: Codex/Alfred
Baseline: `31b9f697c`
Verdict: source cleanup `ADVANCE`; aggregate deployment `HOLD`

## Scope

The commit changes exactly the four mirrored Java files named by
`Handoffs/CODEX_CLEANUP_BATCH17_CANDIDATES_2026-07-13.md` plus the two required history files.
No production class outside DrawEvaluator and PassEvaluator changed.

Per bot, the deletion anchors contain exactly:

- DrawEvaluator superseded ForceReserveService inline scan: 79 removed lines, with one surviving
  banner line rewritten.
- PassEvaluator superseded DTF and maintenance scans: 39 removed lines, with four surviving banner
  lines rewritten.

The four-file Java diff is `+10/-236`. All 246 changed Java lines are blank or comment tokens.

## Independent Proof

- `git diff --check 31b9f697c..e447d306d`: clean.
- Automated changed-line scan: 246 added/deleted Java lines, zero executable lines.
- Rando/ChosenOne normalized full-source hashes match at the parent and candidate for both
  DrawEvaluator and PassEvaluator.
- Isolated detached parent and candidate builds both pass under the same Maven/JDK runtime using
  `mvn -q -pl gemp-swccg-async -am package -DskipTests` from each `src/` root.
- Explicit Homebrew OpenJDK 25.0.2 `javap -p -c -s -constants` output is exactly equal pre/post for
  all four affected classes. Hashes:
  - Rando DrawEvaluator: `6906993e9a16b0e49e0f494e3ade49455c918621638c705df31f82d5db60c57d`.
  - Rando PassEvaluator: `bed6a7a2435d5d89d79518aca6c49006b80c874f5fb7aadd2107ba2376afd902`.
  - ChosenOne DrawEvaluator: `e1173d064fa86a2968b6a51b19ccf4d9fbcf4f17276d75168b015e9a4999a1ea`.
  - ChosenOne PassEvaluator: `485ce247709856f0e56958baafefa9ba0725454c92a576adb4578cbd403833c3`.
- Changelog and version history record the scope, ForceReserveService ownership, git-history revert
  path, and not-deployed state.

## Boundary

The cleanup commit is safe to retain and does not need to wait for the trace, registry, B2,
objective-adapter, or phase-cutover lanes. It does not authorize behavioral owner retirement,
deployment, or removal of any held objective, setup, ActionAudit, ObjectiveHandler, or V-tag arm.
