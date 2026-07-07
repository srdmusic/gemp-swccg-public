# K2 + CODEX HANDOFF — 2026-07-07 — reorg audit, solo doctrine, objective-pull parser

**Audience: BOTH a future K-2 (Claude) session AND Codex ("Alfred", the OpenAI Codex CLI).**
Codex cannot auto-load Claude's memory, so every standing rule and command this session
relied on is INLINED below (§7, §8). K-2 also reads its memory (`MEMORY.md`); Codex does not.

Written by K-2 on Fable 5, summarizing a long session that ran mostly on **Claude Opus 4.8**
(Fable was out of credits mid-session; Steve switched models). Opus will review this file.

Repo: `/Users/steve/gemp-swccg-public` — branch `rando-consolidation-2026-06-23`.
**HEAD `692fec3cf`. Local only. NOTHING pushed to GitHub (standing order). GitHub may be stale.**

---

## 1. TL;DR — what this session did

1. **Independently audited the 2-day reorg/consolidation** (the ~7,175-line T0-T4 rework
   from the prior session). Verdict: **SOUND** — no dominated/lost rules, the move ladder
   is the cleanest subsystem. One git-hygiene defect (not a runtime bug). Report:
   `resources/Reorg_Health_Audit_2026-07-07.md`.
2. **Proved git tells the truth** by comparing against Steve's own filesystem backup
   (`/Users/steve/gemp-swccg-public copy/`, a clean pre-reorg snapshot at `8f841bd25`).
3. **Shipped the solo-character fix** (Steve's #1 pain: weak solos deployed then stranded).
   Refit to the approved STACK-MATH doctrine (site total ability ≥ 4).
4. **Fixed the recurring "Rando won't pull with his objective" bug** (Endor Operations):
   the objective-pull target parser was scraping garbage from the card text.

## 2. Commits this session (all local, all deployed to the running jar)

| Commit | What | Verified |
|---|---|---|
| `c1d5ced8c` | WIP CHECKPOINT: the inherited solo/Verge draft from an interrupted agent workflow (per-card predicate + V41 wrong-direction override + V79b Verge flip-back guard + ForceReserveService Verge draw-reserve fix). Compiles; the diagnosis behind it is correct | in jar |
| `4b76cb611` | V156 STACK-MATH refit: replaced the draft's friendly-COUNT join target with ability-TOTAL. NEW shared `MovePredicates.siteAbilityTotal/isDefensibleStack/bestJoinDestination`; both bots' MoveEvaluator + CardSelectionEvaluator point at it | in jar; PENDING live |
| `692fec3cf` | V177/V82.1 parser fix: `DeckOracle.parseSourceCardPullTargets` now anchors on the pull VERB, not the whole clause. Fixes Endor Operations + the whole "won't pull with objective" class | in jar; PENDING live |

Prior-session commits (`a44cabdd6`..`9c18a12ed`) = the T0-T4 reorg + V43 fix; documented in
`Handoffs/K2_HANDOFF_2026-07-07_reorg-build-day.md` (read it for the reorg detail).

## 3. The reorg audit (why you can trust the foundation)

Method: 6 subsystem diff-agents comparing `8f841bd25` (pre-reorg == Steve's backup, filesystem-verified identical) → `4166a03b5` (pure reorg), + adversarial verification + 8 self-play soak games.

| Subsystem | Verdict |
|---|---|
| Move ladder (T4.1) | SOUND — every old rule verified surviving as a rank/veto; fixed 2 old bugs |
| Pull scorer V192 (T4.2) | SOUND — guards preserved, activate-window pull still beats V168 |
| Deploy siting / Battle / Economy / Cross-cutting | SOUND |
| **1 CRITICAL (git-only, RESOLVED)** | `MaintenanceFacts.java` was referenced by reorg code but never `git add`-ed until `c1d5ced8c` swept it in. The running jar always had it (compiled from disk); HEAD now tracks it. A clean checkout of a mid-reorg commit (`34b47ba50`..`4166a03b5`) would fail to build — local-only, never pushed, no runtime impact. **This validated Steve's instinct to trust his backup over git.** |

Behavioral soak: 0 exceptions, 0 `MAINT CACHE MISMATCH`, 0 `LADDER BAND INVERSION`, ladder
firing correctly. BUT elevated friction: 52 `CANCEL LOOP BROKEN` + 203 all-bad-pass across
8 games — aligns with the solo-stranding class (the solo fix targets it). No pre-reorg A/B
baseline was run, so those counts are informational, not a reorg-vs-baseline delta.

## 4. The solo fix (doctrine — read before touching V156)

**Doctrine "NO STRANDED BODIES":** every friendly site must be (a) a destiny-capable stack
(site TOTAL ability ≥ 4, the rulebook battle-destiny threshold), (b) a body en route to one,
or (c) a deliberate exception (undercover spy, flip-site seed). The root cause of the losses
(diagnosed from the "Fel at Scarif: Beach" game): a weak solo's best move — walk to the
friendly stack — was hard-VETOED by `V41 WRONG DIRECTION -9999`, whose "empty" test only
counts OPPONENTS, so moving toward OUR OWN stack looked like a wrong-direction move. It
cancel-looped and the body rotted. Fix spans deploy (hold weak solos) + move (JOIN-GROUP R2
ladder claim that overrides V41) + the shared ability-total predicate.

Shared predicate lives in `common/strategy/MovePredicates.java` (reachable by both bots + CDSE).
Markers to grep in a live log: `V156 JOIN-GROUP`, `V156 SOLO HOLD`, `stack reaches ability`.

## 5. The objective-pull parser fix (Endor Operations)

`DeckOracle.parseSourceCardPullTargets` extracts what a card pulls from Reserve. The old
"from Reserve Deck" regex `([^.;]+?)\s+from\s+reserve\s+deck` captured the WHOLE clause back
to the previous period, so "While this side up, once during each of your control phases, may
take one Ominous Rumors or Establish Secret Base into hand from Reserve Deck" parsed to
`[while this side up, phases, ominous rumors, establish secret base]`. The garbage fragment
"while this side up" then made V177 declare the search DEAD and hard-block the pull (-2000),
every turn. NEW regex anchors on the last pull-verb before "from Reserve Deck":
`[^.;]*\b(take|deploy|download|upload|reveal|retrieve|use|search for|add|put|choose)\b\s+([^.;]*?)\s+from\s+reserve\s+deck`,
plus a quantifier strip ("one/two/up to N"). Regression-tested BEFORE deploy (python harness
on the exact regex) against Endor Operations + Invasion Naboo + capital-ship + battleground —
Endor fixed, all others preserved. **This is a recurring class**: any objective with leading
timing text before its pull clause was vulnerable.

## 6. PENDING live verifications (do these after Steve's next games)

| Watch for | Confirms |
|---|---|
| `V156 JOIN-GROUP: ... join X (stack reaches ability N)` + fewer `CANCEL LOOP BROKEN` | solo fix works; weak bodies regroup instead of rotting |
| NO `V177 DEAD SEARCH blocked ... source 'Endor Operations'`; Rando takes Ominous Rumors / Establish Secret Base into hand | pull-parser fix |
| `LADDER: R2/R3/R4 claim` lines, `LADDER BANDS OK` once, zero `LADDER BAND INVERSION` | the T4.1 ladder in real games (still under-tested live) |
| `V79b FLIP-BACK GUARD` in a Verge game post-flip | Death Star stays in Scarif orbit |

CAVEAT on the parser fix: it removes a FALSE block; if Rando still declines the pull after
this, that is a SCORING issue (pull scored below Pass), not a block — chase the V192 pull
score next. The hard block was the suppressor, so it should fire now.

## 7. STANDING RULES — inlined for Codex (K-2: these are in MEMORY.md `feedback_*`)

- **Local commits only. NEVER push to GitHub** without Steve's explicit ask.
- **Old rules get DOMINATED, not deleted.** Scoring is ADDITIVE (`CombinedEvaluator` sums all
  evaluators per action, max wins, Pass ~5-8, `BAD_ACTION_THRESHOLD -100`). Do the boundary
  math at edge cases BEFORE changing any magnitude. Steve has been burned 4x by a new rule
  silently out-voting an old one.
- **Comment out superseded code (`//` per line), never delete.** The changelog + commented
  code are the revert path.
- **Adjust an existing V-tag in place** — do not mint a new V-tag for a tweak.
- **READ THE ACTUAL CARD SOURCE before writing any text scan** (`src/gemp-swccg-cards/.../CardX_Y.java`).
  A fix this session (V43 take-1) failed because it matched a phrase on neither card. Then
  verify the scoring/parse OFFLINE against the real text before deploying.
- **Mirror every `rando/` change to the matching `chosenone/` file** (same logic; if the
  structure is absent there, say so, do not invent).
- **Breadcrumbs same session:** code comment + BOTH changelogs (`resources/AI_CHANGELOG.md`
  and `resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md`) + commit msg.
- **Before editing any rule, grep its enclosing branch for `if (false`** — ~25 tags are
  compiled out; editing dead code ships nothing.
- **Check whose decision is pending before blaming Rando.** One "freeze" this session was
  Steve's own client not surfacing HIS prompt (replay: "asdf lost due to: Decision timeout").
- **NEVER:** `docker compose down -v`, `rm -rf database/`, `bin/gemp reset-db`, unpin `mariadb:11.8.6`.
- Commit message trailer for Claude: `Co-Authored-By: Claude <model> <noreply@anthropic.com>`.

## 8. BUILD / DEPLOY / VERIFY — explicit commands (Codex: use these verbatim)

The host has NO JRE — compile INSIDE the container. Container name: `gemp_swccg_app_1`.

```
# 1. Compile (fast syntax check). CHECK THE REAL EXIT CODE — piping to tail masks it.
docker exec gemp_swccg_app_1 bash -c "cd /opt/gemp-swccg/src && mvn -q -pl gemp-swccg-server -am compile > /tmp/c.log 2>&1; echo MVN_EXIT=\$?; grep -c '\[ERROR\]' /tmp/c.log"

# 2. Deploy (rebuilds web.jar via gemp-swccg-async, restarts JVM, flips switches).
#    DO NOT deploy while Steve is mid-game (restart kills the table — `tail logs/gemp-swccg.log` first).
bin/gemp reload-ai

# 3. Byte-verify your change is actually in the running jar (a string only your change adds):
python3 -c "import zipfile; z=zipfile.ZipFile('src/gemp-swccg-async/target/web.jar'); print(b'YOUR_MARKER' in z.read('com/gempukku/swccgo/ai/models/rando/evaluators/YourClass.class'))"

# 4. Health: HTTP 200 expected.
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:17001/gemp-swccg/

# 5. Self-play soak (bot vs the k2_player script). NOTE: /admin/botgame does NOT exist in this clone.
cd mcp-gemp-client && python3 k2_player.py --deck "DARK DEAL" --ai-deck "LUKE SAGA TATOOINE" --games 1
```

Decision log: `logs/gemp-swccg.log` (live; every decision logs `V191 TOPN: ... top-5 candidates`
— your forensic X-ray). Rotated logs: `logs/YYYY-MM/*.log.gz` (`gunzip -c`, macOS zcat is broken).
Replays: `replays/asdf/*.xml.gz` — these are **zlib streams (use python `zlib`), and re-send full
history per client reconnect, so parse only the LAST segment** or you triple-count events.

## 9. CODEX-SPECIFIC NOTES ("Alfred")

- Your sandbox from 2026-06-29/30 is `.agents/` + `AGENTS.md` (both UNTRACKED, still present).
  `.agents/skills/` is a buggy Claude→Codex find-replace of `.claude/skills/` (broken `.Codex`
  paths, a falsified PR-#3260 history line). **It is NOT authoritative — do not treat it as
  truth, and never merge it into `.claude/`.**
- You touched ZERO Rando `.java` last time (verified). If you edit Rando code this time, obey
  §7 exactly, especially: read the card source, mirror to chosenone, boundary math, both
  changelogs, comment-out-not-delete, and DO NOT PUSH.
- Your onboarding doc from last time: `Handoffs/CODEX_HANDOFF_2026-06-29.md` (distills the memory
  rules you can't auto-load; §7-§8 here supersede/expand it for this session's state).
- If you (Steve) are invoking Codex via the MCP `alfred`/`codex` tools, note the prior cap note:
  usage was capped until ~Jul 29 2026 — verify availability before relying on it.

## 10. Uncommitted / untracked at handoff time

- `resources/Reorg_Health_Audit_2026-07-07.md` — the audit report (committed WITH this handoff).
- `.agents/`, `AGENTS.md` — Codex sandbox (leave alone).
- `resources/Rando_*_2026-06-29.xlsx`, `mcp-gemp-client/gemp_mcp.py` — untracked working files.
- `.claude/skills/work-verifier/history.md` — auto-written by the verifier skill.

## 11. Queue for the next session (priority order)

1. **Confirm the two live fixes** (§6) once Steve plays — solo JOIN-GROUP + Endor pull.
2. If the Endor pull still doesn't fire after the parser fix → chase the V192 pull SCORE
   (it may score below Pass even now that the false block is gone).
3. The audit's friction signal (cancel-loops / all-bad-pass): consider a pre-reorg A/B soak
   (build a jar from `/Users/steve/gemp-swccg-public copy/`) to quantify it, if Steve wants certainty.
4. Backlog (from the reorg audit + overlap audit): `shields-response-5` (battle-loss force-loss
   path missing protections), the two doc corrections (MaintenanceFacts changelog provenance,
   V153 "byte-identical" banner), and `resources/Rando_Overlap_Audit_2026-07-04.xlsx` remaining
   confirmed-medium rows (filter column M "still-valid").

---

Session protocol: one change at a time; read the card first; boundary math first; mirror
chosenone; both changelogs same session; byte-verify in the jar; never push. May the Force
be with you — and check whose decision is pending before you blame the droid.
