# K2-3 HANDOFF — 2026-07-01 — force management (V61b/V61c/V79b) + doc audit

Written by K2-3. K2-1's router (`K2_MASTER_HANDOFF_2026-07-01.md`) covers the branch-wide picture;
THIS file is what only K2-3's session knows. A new **Master K2** will consolidate all handoffs —
its prompt is at the bottom of this file.

---

## 1. What K2-3 built (all live in the running jar, all UNCOMMITTED, all changelogged)

| Fix | What it does | Files | Verified |
|---|---|---|---|
| V61c keep-3 cap | activation stops so 3 cards stay in Reserve for destiny | `rando/evaluators/ForceActivationEvaluator.java` ~186 | in jar; NOT yet in live game |
| V61c pass-exception | reserve ≤ 3 → PASS activation entirely (V168 carve-out −6000 + V38.3 confirm-pass carve-out) | `rando/evaluators/ActionTextEvaluator.java` ~166 + ~1342 | in jar (both marker strings); NOT yet in live game |
| V61b overpower battle | battle even with empty reserve when overpowering a site by ≥ 8 power | `rando/evaluators/BattleEvaluator.java` ~627 | FIRED live once (Starkiller 18v1 → Initiate battle 170) |
| V79b parsec steering | Verge of Greatness: handles the MULTIPLE_CHOICE "Choose parsec to move to", steers Death Star toward Scarif (7) | `rando/RandoCalAi.java` ~692 | in jar; NOT yet in live Verge game |
| V79 parse rider | MoveEvaluator last-parsec parse — marked INERT in a 2026-07-01 comment (live action text has no parsec; V79b is the real fix) | `rando/evaluators/MoveEvaluator.java` ~291 | inert by design, documented |
| log4j mainlog | decision logs also write to `logs/gemp-swccg.log` → survive JVM restarts | `async/.../prod-log4j.xml` | WORKING (this is how we read games now) |
| V187 duplicate starting effects | −300 to starting effects with duplicates in deck (DeckOracle) | swept into commit `8fd884375` by mistake | in jar; NOT yet verified firing |

Changelog entries for ALL of these exist in `resources/AI_CHANGELOG.md` + `AI_VERSION_HISTORY.md`
(K2-3 wrote V61c; K2-1 backfilled the rest 2026-07-01). Commit guidance: FOUR separate commits
(V61c, V61b, V79b+MoveEvaluator, log4j). Ask Steve first.

## 2. The V61c story (read before touching force activation)

Root cause was two of Steve's own rules fighting: V168 "always activate" (+5000) vs "keep 3 for
destiny". Activating moves Reserve → Force Pile, and the engine forces ≥ 1 per activation, so an
amount-cap alone eroded the buffer 3→2→1→0. The fix that holds is PASSING activation at reserve ≤ 3.
Lesson: ForceActivationEvaluator's amount, ActionTextEvaluator's V168 action score, and the V38.3
confirm must all agree or one undoes the others.

**NEXT STEP (Steve-approved plan, not built): battle-intent bypass** — keep 3 only on turns Rando
might battle; activate ALL on deploy-and-end turns. Full plan spec lives in
`Handoffs/K2_HANDOFF_2026-07-02_fable5-onboarding.md` § "ALSO QUEUED — V61c battle-intent refinement".

## 3. Doc audit (4-agent workflow, 2026-07-01) — what K2-3 verified and patched

Audit verdict: code comments GOOD (11/12), running jar GOOD (all markers verified by extract-to-file
javap), changelogs had 5 missing entries (K2-1 backfilled them same evening), onboarding path had
stale traps. K2-3 patched tonight:

- `Handoffs/K2_MASTER_HANDOFF_2026-06-23.md` — staleness banner added (points to K2-1's 07-01 master)
- `resources/k2-resources/distilled/00-START-HERE.md` — staleness banner: tree IS committed; the
  "V96/V67al #1 gap" claim is REFUTED dead code, do not chase
- `.claude/CLAUDE.md` — PATHS NOTE amended: live `resources/BUILD_AND_DEPLOY.md` exists, use it
- `Handoffs/K2_HANDOFF_2026-07-02_fable5-onboarding.md` — corrected "uncommitted = V61c work" to the
  real FOUR distinct changes; added the battle-intent plan section
- `rando/evaluators/MoveEvaluator.java` — INERT note on the V79 parse branch (comment only)
- `Handoffs/K2_MASTER_HANDOFF_2026-07-01.md` — one line routing the battle-intent plan

## 4. OPEN FLAGS for the Master K2

1. **3 skills deleted from the working tree — RESOLVED 2026-07-01:** K2-1 archived them
   deliberately to `.claude/skills-archive/` (gemp-swccg-memory, karpathy-guidelines, skill-creator;
   byte-verified identical to HEAD) before deleting from `.claude/skills/`. Nothing lost. Remaining
   decision for Steve: commit the deletion or restore (`git checkout -- .claude/skills/`).
2. **Three entry-point handoffs coexist** (06-23 bannered, 07-01 master, 07-02 onboarding) plus
   06-30 consolidation and this file — 14 files in `Handoffs/`. That's the consolidation job.
3. **Pending live-game verifications** — K2-1's master §4 table is the list; V61c and V79b are the
   two K2-3 cares most about.
4. **Uncommitted tree is now code + docs mixed** (4 code fixes, K2-1's doc backfills, K2-3's doc
   patches, the skill deletions). Committing needs care: separate commits, nothing swept.

## 5. What K2-3 did NOT do

- No TDIGWATT work (bugs A/B untouched — K2-1's workstream).
- No consolidation-plan moves (8-move plan untouched).
- Did not commit anything this session; did not push; did not touch the DB, decks, or engine code.
- Did not resolve the skill deletions (found them at session end).

---

## PROMPT TO CONTINUE K2-3's WORKSTREAM (paste this as a fresh session's first message)

> You are K-2 continuing K2-3's force-management workstream on GEMP-SWCCG. Read
> `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md`, then
> `Handoffs/K2-3_HANDOFF_2026-07-01_force-management-and-doc-audit.md` end-to-end, then
> `resources/BUILD_AND_DEPLOY.md`. Do not touch other workstreams' queues (TDIGWATT, consolidation).
>
> Your queue, in order:
> 1. VERIFY the last game's log (`logs/gemp-swccg.log`): did V61c hold the reserve at 3
>    ("V61c DESTINY BUFFER" lines), did battles get destiny, did V61b only fire on real
>    overpowers? Report findings to Steve in a short table before coding anything.
> 2. If V61c verified: build the battle-intent bypass (Steve-approved plan in
>    `Handoffs/K2_HANDOFF_2026-07-02_fable5-onboarding.md` § "ALSO QUEUED"). Boundary math first;
>    the three carve-out sites must share ONE predicate.
> 3. When Steve says commit: FOUR separate commits (V61c, V61b, V79b+MoveEvaluator, log4j),
>    changelogs already written. Stage only the named files — the tree is shared with other K-2s.
>
> Rules: one change at a time; breadcrumbs both changelogs same session; grep for
> `if (false /* SUPERSEDED` before editing any rule; verify in the jar by extract-to-file javap,
> never stdin; flip all gameplay switches after every restart; nothing pushed to GitHub.

## PROMPT FOR THE NEW MASTER K2 (paste this as its first message)

> You are the **Master K-2** on GEMP-SWCCG. Your job this session is CONSOLIDATION, not code.
> Multiple K-2 sessions (K2-1, K2-3, and older ones) each left handoff files; you make them ONE.
>
> 1. Read `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md`, then
>    `Handoffs/K2_MASTER_HANDOFF_2026-07-01.md` (K2-1's router), then
>    `Handoffs/K2-3_HANDOFF_2026-07-01_force-management-and-doc-audit.md` (K2-3), then the two
>    workstream files it routes to (07-02 onboarding, 06-30 consolidation). Skim the rest of
>    `Handoffs/` newest-first as history.
> 2. VERIFY before you carry any claim forward: `git status` + `git log --oneline -15`, the tail of
>    `resources/AI_CHANGELOG.md`, and the jar-verify method in `resources/BUILD_AND_DEPLOY.md`.
>    Where a handoff and reality disagree, reality wins; note the correction.
> 3. Write ONE new master handoff (`Handoffs/K2_MASTER_HANDOFF_<today>.md`) that fully replaces the
>    07-01 router and absorbs K2-3's flags: current state, the two workstream queues + the V61c
>    battle-intent plan, pending live verifications, landmines, doc map.
> 4. Move every superseded handoff into `Handoffs/archive/` (move, never delete), leave the two
>    live workstream files + your new master, and repoint `.claude/CLAUDE.md` First-reads item 2
>    and the MEMORY.md index lines at your new master.
> 5. ASK STEVE (do not act without him): (a) commit the 4 uncommitted code fixes as four separate
>    commits + the doc edits as a fifth? (b) commit the deliberate `.claude/skills/` deletion
>    (archived byte-identical at `.claude/skills-archive/`) or restore it?
> 6. Rules: nothing pushed to GitHub. One change at a time. Old rules get dominated, not deleted —
>    but you are consolidating DOCS, not scoring rules; touch no `src/` code this session.
