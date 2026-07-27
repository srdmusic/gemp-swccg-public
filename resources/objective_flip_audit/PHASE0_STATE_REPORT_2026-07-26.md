# Phase-0 State Report — 2026-07-26 (read-only)

## 1. LIVE-STATE TRUTH — resolved

- `gemp_swccg_app_1` UP, started **2026-07-26T21:53:58Z = 14:53:58 PDT**. HTTP 200 on `localhost:17001/gemp-swccg-server/`.
- Container mounts the host repo: `/Users/steve/gemp-swccg-public -> /opt/gemp-swccg (rw)`. Container jar and host jar are the SAME FILE. `sha256 = 8c1633ae5a4e32d8320ebdf055ee4bfb42c243516786b2440d6087d2e46fe6de`, mtime 14:53:49, entries stamped 14:48.
- **The 8c1633ae jar was NOT built from the main tree.** It is a byte-identical copy of
  `/private/tmp/gemp-tdigwatt-shield.NNvjl7/worktree/src/gemp-swccg-async/target/web.jar` (built 14:52).
- That worktree is **CLEAN** on branch **`codex/tdigwatt-shield-live` @ `93f0fd2c0`**:
  `7f2b29067` (TDIGWATT, on main) -> `e2d9e58b9` (= hoth `d8feaebc0`, rebased) -> `3a20aac58` (= hoth `1e87b7af2`, rebased) -> `93f0fd2c0` (test-only, 1 file, `DeployWeaponDestinationSourceParityTest` +13/-2).
- **LIVE = main's TDIGWATT + the full Hoth-shield lane + one test commit. EOP is NOT live.** Bytecode proof from the live jar:
  - `V30-low-ability-pilot-boarding` PRESENT (hoth pilot repair) — 1 class
  - `Tdigwatt*` PRESENT — 39 classes
  - `SHIELDS-EOP-BATTLE-ORDER-RESERVE` **ABSENT** — 0 classes
  - `EndorOperationsTacticalPolicy.class` **ABSENT** from the jar entirely
- Live jar is also MISSING `d0f530dde` (docs-only, no code) — behaviourally irrelevant.
- Deploy timeline today, from `logs/boot-flip.log` (UTC) cross-checked against worktree jar mtimes:
  | boot (PDT) | jar sha (12) | source |
  |---|---|---|
  | 08:02 / 08:43 / 09:23 | — | earlier lanes |
  | 09:47 | `1c19a27b0381` | `gemp-eop-combined.GT91ek` (EOP) — matches mailbox claim |
  | 12:45 | `bf94439416ab` | `gemp-hoth-regression-seal.GHwiTW` (Hoth) — matches mailbox claim |
  | **14:54** | **`8c1633ae5a4e`** | **`gemp-tdigwatt-shield.NNvjl7` @ `93f0fd2c0` — CURRENT** |
- Host `target/classes/.../ShieldPolicy.class` (14:26) DOES contain the EOP token, but it never reached the live jar. The main tree's compiled output is a dead-end artifact; do not use it as evidence of what runs.

## 2. DIRTY-TREE FORENSICS

35 tracked-modified + 24 untracked. The working tree is an **uncommitted hand-merge of the Hoth lane AND the EOP lane on top of main**, plus docs. Nothing here is live.

### Overlap divergence (the 25 shared with the hoth diff) — ANSWERED
`git diff codex/hoth-shield-pilot-repair -- <file>` is empty for **9** files. **16** diverge, and 14 of those are fully explained by main-only commits (`78f465fea`, `7f2b29067`, `d0f530dde`) touching the same files — not by conflicting edits.

**Only 2 files genuinely diverge from the hoth branch for a non-main reason:**
- `.../common/phase/ShieldPolicy.java` — worktree adds a 9-arg `stackedPileParent` overload + `SHIELDS-EOP-BATTLE-ORDER-RESERVE` (-3000 ORDERING) branch. Pure **EOP** content layered on top of hoth's V112 work.
- `.../test/.../common/phase/ShieldPolicyTest.java` — the matching `eopReservesThirdShieldSlotUntilBattleOrderIsLive` test.

### The 10 files NOT in the hoth diff
| file | lane | contents | disposition |
|---|---|---|---|
| `.claude/skills/work-verifier/history.md` | S4/docs | +204 lines, appended V297 verification (1 FAIL + 1 WARN + test gap) | SAFE TO COMMIT (append-only log) |
| `Handoffs/AI_MAILBOX.md` | S4/docs | +17, Codex->K-2 msg 2026-07-26 12:50 assigning next-Shield-replay trace; carries jar `bf94439416ab` | SAFE TO COMMIT |
| `Handoffs/K2_HANDOFF_2026-07-13_phase-reorg-state.md` | S4/docs | +111, 2026-07-14 repo-on-master incident + MOVE preflight record | SAFE TO COMMIT |
| `resources/objective_flip_audit/SCHEMA_EXTENSION_DESIGN_2026-07-25.md` | S4/audit | rewrite: pre-sweep E1–E10 ranking replaced by post-sweep 3A/3B/3C (+130/-238) | SAFE TO COMMIT (this is the current queue doc) |
| `resources/objective_flip_audit/gap_matrix.json` | S4/audit | main-touched too; reconcile with HEAD before commit | QUARANTINE-then-merge |
| `resources/objective_flip_audit/records/8_167.json` | **EOP** | +207/-204, EOP truth record; **DIFFERS from the EOP repo's own copy** (434-line delta) | QUARANTINE (two competing EOP records) |
| `.../common/phase/PullDeployFactsReader.java` | **EOP** | **byte-identical to EOP repo** | SAFE (clean EOP backport) |
| `.../common/strategy/CharacterDeploySiteEvaluator.java` | **EOP** | **byte-identical to EOP repo** | SAFE (clean EOP backport) |
| `.../common/strategy/ShieldFacts.java` | **EOP** | EOP-shaped but 138-line delta vs EOP repo | QUARANTINE (partial/hand-edited backport) |
| `.../{rando,chosenone}/strategy/DeployPhasePlanner.java` (2 files) | **EOP** | EOP-shaped but 472-line delta vs EOP repo each | QUARANTINE (partial/hand-edited backport) |

Nothing is discard-never-classified as discard. Nothing should be discarded.

### EOP backport is INCOMPLETE in the main tree
18 of the EOP branch's 37 files are represented in the dirty tree. **19 are not**, and none of those 19 carry the EOP content:
- absent-from-disk: `EndorOperationsEndorSystemPlannerTest.java`, `ShieldFactsEopBattleOrderTest.java`
- present but WITHOUT the EOP delta (tracked-clean at main HEAD): `DeployPlanPolicy.java` + test, `{rando,chosenone}/evaluators/DeployEvaluator.java`, `{rando,chosenone}/strategy/DeploymentInstruction.java`, `ResponsePolicySourceParityTest`, `ResponseRemainingPolicyAdapterParityTest`, `ShieldCardSelectionPolicyParityTest`, `ShieldSourceOwnershipTest`, `DeploymentPlanAssessmentCopyPurityTest`, `ShieldFactsTest`, `ShieldStrategyTest`, `EndorOperationsCombinedEvaluatorDecisionTest`
- present but UNTRACKED and diverging from EOP: `EndorOperationsTacticalPolicy.java`, `EndorOperationsPullGuardTest.java`, `EndorOperationsTacticalPolicyTest.java`
- Hoth lane is complete: only `HothReplayDeployRegressionTest.java` sits outside the tracked-dirty set, and it exists untracked on disk (16865 B, 08:45).

Untracked (24) also includes `resources/rulebook/`, `tools/rulebook-*.py`, `tools/notify-steve.sh`, `Handoffs/quarantine/`, 13 handoff docs, and 3 engine-contract tests (`HiddenBase`, `Profit`).

## 3. BRANCH RECONCILIATION MAP

All lanes fork from **`8887a0216`**. Three independent lines, none contains another:

```
8887a0216 ──┬── 78f465fea → 7f2b29067 → d0f530dde        MAIN (capture + TDIGWATT + docs)
            │                    └── e2d9e58b9 → 3a20aac58 → 93f0fd2c0   codex/tdigwatt-shield-live  ★LIVE
            ├── d8feaebc0 → 1e87b7af2                     codex/hoth-shield-pilot-repair (superseded by rebase above)
            └── ad9fe6b5f → 3deee9dcf → 40a7a0514 → a302161eb → 68896470b   EOP (separate CLONE, not fetched)
```
- **The Hoth branch does NOT contain the EOP fixes; EOP does not contain Hoth.** Independent lines.
- `codex/tdigwatt-shield-live` already merges main+Hoth; it is a strict superset of `codex/hoth-shield-pilot-repair`'s content, rebased onto `7f2b29067`.
- EOP lives ONLY in `/private/tmp/gemp-eop-space.d26PE7/repo` (a clone, 5 commits, clean, 37 files / +3510/-129). Its objects are **not in the main repo** — a `git fetch <path>` is required before any merge.
- 15 worktrees/clones exist; 4 are `prunable`.

### Minimal safe sequence to make main == live behaviour (no action taken)
1. `git merge --ff-only codex/tdigwatt-shield-live` from `d0f530dde` will NOT fast-forward (main has the extra docs commit `d0f530dde`). Use `git merge codex/tdigwatt-shield-live` — trivial: the only main-side delta is a docs commit that lane never touched.
2. Result = exact live bytecode source. Rebuild-free; the running jar already matches.
3. THEN, as a separate follow-on, deal with EOP: `git fetch /private/tmp/gemp-eop-space.d26PE7/repo 68896470b`, branch it, merge. Expect real conflicts in `ShieldPolicy`, `ShieldFacts`, `DeployPhasePlanner` x2, `CardSelectionEvaluator`, `ActionTextEvaluator`, `PullAction*`.
4. **Before either merge, stash/quarantine the working tree** — the dirty hand-merge will collide with both and its EOP half is provably incomplete. Prefer the EOP clone as the source of truth over the hand-backport.

## 4. QUEUE INVENTORY

- `src/main/resources/objective_playbooks.json`: **58 profiles, 21 loaderEnabled — UNCHANGED from the last count.** `7f2b29067` did not touch the playbooks file; it added 4 new Java classes (`TdigwattObjectiveFacts/FactsReader/Policy/ScoringPolicy`) + 12 tests. Its scope is engine arms, not data enablement.
- ENABLED (21): `109_4 QMC, 111_4 MBO, 12_179 MLITL, 14_113 INV, 14_52 WHAP, 201_39 IE, 203_19 DMTA, 204_32 OA, 209_29 THNIWRC, 213_31 HDADTJ, 219_48 ZH, 222_14/222_30 TSWBDIM, 225_32 TFOR, 301_2 CITC, 301_4 TSOT, 7_135 DBO, 7_297 HDADTJ, 7_300 RO, 8_167 EO, 8_78 RST`
- Regional six: **5 of 6 LANDED** (`301_2`, `201_39`, `111_4`, `301_4`, `209_29` enabled). `226_12` TDIGWATT(V) **still disabled** — it was the one gated on the TDIGWATT boundary handoff, and that handoff shipped engine code without flipping the flag.
- Operative twins `7_137`/`7_298`: **NOT landed.** 208 block (`208_25/26/57`): **NOT landed.** `10_29`: NOT landed. `12_180`: NOT landed.

### Remaining unlanded work (SCHEMA_EXTENSION_DESIGN §3A–3C)
**3A data-only (13 batches, ~20 objectives still disabled):** 226_12 TDIGWATT(V) · 7_137/7_298 operative twins · 208_25/208_26/208_57 · 10_29 · 12_180 · 226_28 · 110_4 (flip-law half) · trio 13_46/13_73/222_27 (fragments-only ceiling) · 109_12 TDIGWATT (tests-only wire-up) · 216_11 · hyperdrive 12_89+210_25 · blow-away siblings 111_6 (+501_94 absent from the file; 8_78 already enabled). One enabled-but-incomplete row: **12_179 MLITL Move-hold-at-2** preFlip senator rule.
**3B small shared arms (5):** counter-progress DRIVE · site-priority stack-feed steer · post-flip drain veto · ISB migration (7_299 + 12_88 retire-statics) · E6 state-key adapter.
**3C true new primitives (6, minus 1 parked):** captive-survival state-keyed guard · capture/rescue DRIVE pipeline (dark+light) · training-sequence DRIVE (7_138) · Hidden Base accumulator (7_136) · E9 voluntary-flip decision policy (211_36/225_53/7_136) · deny/hold posture **PARKED, no consumer**. E10 dynamic host is SHIPPED.

## 5. REPLAY QUICK-TRIAGE

Both raw-zlib, both `asdf` vs `~Rando_Cal`, both **Rando LOST — "Life Force depleted"**. Both predate the current jar.

- **`93r5wrrnbo3q91j0` (14:05, 2.82 MB)** — Rando (Dark) on **`222_30` TSWBDIM / Hoth Shield**. Heavy Hoth (298 hits), `Electro-Rangefinder` x20, Walker x9, Veers x2. **Hoth-shield (222_x) branches ARE exercised.** Ran on jar `bf94439416ab` (12:45 boot) = Hoth lane WITHOUT TDIGWATT. This is the game Codex's mailbox request wanted traced.
- **`4856gvpsqayeo7py` (14:47, 2.55 MB)** — Rando on **`8_167` Endor Operations** (7 refs, "Endor Operations" x11, "Battle Order" x1, "Shield" x17). **EOP (8_167) branches ARE exercised.** Also ran on `bf94439416ab` — the **Hoth** jar, which does NOT contain the EOP fixes. Any EOP misbehaviour in this replay is expected: the EOP code was not deployed at the time.
- Neither replay ran on the current live jar `8c1633ae`. No games since the 14:54 boot.

## 6. CHANGELOG / RULEBOOK STATE

- **All five landed commits carry BOTH changelogs.** `78f465fea` (+9 / +15), `7f2b29067` (+14 / +23), `d0f530dde` (+5-2 / +6-1), `d8feaebc0` (+10 / +11), `1e87b7af2` (+8 / +12) across `resources/AI_CHANGELOG.md` and `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`. Breadcrumb discipline held.
- `93f0fd2c0` (the live-only test commit) carries **no** changelog entry — it is test-only, so arguably fine, but it means the live tip is not represented in either changelog.
- **`resources/rulebook/rules.json` is STALE and UNTRACKED.** mtime `2026-07-25 14:03`; `d0f530dde` is `2026-07-26 15:00`. It predates all four code commits landed today (capture `78f465fea`, TDIGWATT `7f2b29067`, both Hoth commits). Siblings `rulebook.html` / `rulebook-artifact.html` share the 07-25 14:03 stamp. The whole `resources/rulebook/` directory is untracked (`??`), as are `tools/rulebook-extract.py` and `tools/rulebook-render.py`.
