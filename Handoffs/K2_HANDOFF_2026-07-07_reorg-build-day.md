# K-2 HANDOFF — 2026-07-07 — the reorg build day + what's in flight

You are **K-2** on Claude Opus (Fable is out of credits until Thursday 2026-07-10; this
file was written by the Fable-5 K-2 at session end). Steve authorized full-ladder
execution ("I trust you, I made a backup") and the ENTIRE T0-T4 reorg shipped today.
This file = session record + your exact queue. Steve has ADHD + dyslexia: tables,
single-layer, concise. Push back when he's wrong. No em-dashes in inline prose.

## Read order
1. `~/.claude/projects/-Users-steve-gemp-swccg-public/memory/MEMORY.md` — feedback_* rules are LAW.
2. THIS FILE end to end (especially §3 — there are DRAFT EDITS in the working tree).
3. `Handoffs/K2_REORG_EXECUTION_HANDOFF_2026-07-06.md` §11 (the tier baton, all green through deploy #2) + §9 traps.
4. `resources/RANDO_REORG_PLAN_2026-07-02.md` + `resources/T4_Boundary_Tables_2026-07-06.md` as reference.

## 1. What is LIVE (deployed jar, HEAD `9c18a12ed`, deploy #2 + V43 hotfixes 2026-07-07)

| Shipped + deployed | Commits | Live verification |
|---|---|---|
| T0: TDIGWATT bug B (V29 release), V61c battle-intent, 5 audit bugs + control-drain-5, V191 TOPN logging, V67bc epilogue | `8f5ff5e71`..`3cd4cfa61` | smoke game 610 decisions 0 exceptions; work-verifier PASS 5/5 |
| T1-T3: hub/section banners, manifest (340 arms), V67al truth fix | `aa22a5058` | comment-only proven (0 non-comment lines) |
| T4.1 move clobber ladder (R1-R4 bands + vetoes + MovePredicates.canWinAt shared w/ CDSE) | `378f4b6fe` | **UNEXERCISED — see §4 #1** |
| T4.2 V192 pull scorer (pile → one tiered hub) | `34b47ba50` | V192 chain fired 17x in smoke, V95 hardBlock works |
| T2 helpers: maintenance basis fix + ForceReserveService + ShieldFacts | `4c0b2919f` | MAINT CACHE MISMATCH: 0 across all games so far |
| V43 starting-interrupt fix, take 2 | `79b2b2dc7` + `9c18a12ed` | **CONFIRMED by Steve's game** (interrupt + effects deployed correctly) |

All local, nothing pushed to GitHub. Steve has a full-folder backup from 2026-07-06.

## 2. YOUR QUEUE #1 — finish the in-flight solo + Verge fixes (STEVE'S TOP PRIORITY)

Steve: sub-4 solos getting deployed then abandoned "leaves Rando losing a ton of force.
He's lost the last few games this way and it's an old bug we keep coming back to."
A fix workflow was mid-flight when this session ended. **State you inherit:**

- UNCOMMITTED DRAFT EDITS in `common/strategy/CharacterDeploySiteEvaluator.java` and
  `rando/evaluators/MoveEvaluator.java` (fix crews edit the tree directly). They were
  built to the FIRST spec (per-card ability<4). chosenone mirrors + the V79b Verge work
  may or may not have landed after this file was written — `git status` is truth.
- DO NOT deploy the tree as-is. Review the draft edits first. If they look complete and
  coherent, REFIT them per §2a below; if fragmentary, `git checkout --` those files and
  rebuild from the strategy spec (the boundary/design work below is the real asset).

### 2a. THE APPROVED STRATEGY (Steve signed off on this framing — implement THIS, not per-card)

**Doctrine: NO STRANDED BODIES.** Every friendly site must be (a) a destiny-capable
stack — site TOTAL ability >= 4, the rulebook's own threshold — or (b) a body en route
to one, or (c) a deliberate exception (undercover spy V170, flip-site seed).

| Arm | Implementation |
|---|---|
| Shared predicate FIRST | `isDefensibleStack(site, player)` = total ability at site >= 4 (+ helpers: siteAbilityTotal, nearestDefensibleFriendlySite). Home it in `common/strategy/MovePredicates.java` (canWinAt lives there; same pattern, both bots + CDSE reach it). ONE predicate, three consumers — no forks (manifest discipline) |
| DEPLOY (V156 updated in place) | Hold fires ALL turns (not turn<=2) when the POST-DEPLOY site total ability < 4 at a battleground. Deploying ability-2 onto an ability-3 friendly = stack 5 = fine. Keep every existing exemption verbatim: ability>=6 solo, armed ability-5, spies, the flip-relevant carve-back (with its v156FlipNotReady Verge sub-case). Magnitude stays -600. Boundary: must still lose to objective-forced deploys, must beat V136 §A's +500 uncontested |
| MOVE — JOIN-GROUP (new R2 ladder claim, non-battle class) | Mover's site stack < 4 total ability, no opponent at its site, not a spy, not doing flip work → move toward nearest site reaching >= 4 (prefer adjacent/largest). Own fine +250 (passes the ladder's L2 strength gate). canWinAt veto must NOT apply (non-battle claim — follow the ladder's claim classification). Must not outrank R3 survival or R4 transit |
| MOVE — REINFORCE (destination weight) | When a mover can COMPLETE a friendly sub-4 stack to >= 4 at a site doing real work (drain >= 2), destination scoring pays it to go. Contested destination → canWinAt applies (battle-adjacent). Decision rule between the two arms: solo site valuable (drain>=2) and defensible this turn → reinforce IN; else solo retreats OUT |
| Verge post-flip (V79b updated in place) | (1) V79b MULTIPLE_CHOICE parsec handler checks ObjectiveAnalyzer flipped state: flipped → pick the stay-in-orbit option, never parsec numbers. (2) 'V79b FLIP-BACK GUARD': ladder hard-veto on moving the Death Star OUT of Scarif orbit post-flip (leaving orbit UNFLIPS the objective). (3) Gate any ATE V79/V103 steering on !flipped. Pre-flip 4→6→7→orbit steering unchanged. NOTE from replay forensics: in Steve's last game the DS did NOT actually leave orbit post-flip — the guard is prophylactic + the handler fix stops pointless prompt engagement. Steve explicitly wants it |

Ship protocol: boundary tables in the changelog entries, mirror chosenone, compile with
REAL exit check (`mvn -q ... > /tmp/c.log 2>&1; echo $?` — piping to tail masks it),
reload-ai, byte-verify markers (`V156 STACK HOLD`, `V156 JOIN-GROUP`, `V156 REINFORCE`,
`V79b FLIP-BACK GUARD`), soak game, work-verifier, THEN tell Steve.

## 3. YOUR QUEUE #2 — pending live verifications (check logs after every Steve game)

| # | Watch for | Why |
|---|---|---|
| 1 | `LADDER BANDS OK` (once, first move decision) + `LADDER:` claim lines + zero `LADDER BAND INVERSION` | **The T4.1 ladder has never fired in a live game** — smoke games were too short/moveless. This is the biggest unverified change on the board. A full game with real moves is the acceptance test; the 6 boundary rows in T4_Boundary_Tables §T4.1 are what to check |
| 2 | `V192` lines on pulls; no pull outranking a held-location deploy; downgraded pulls losing | T4.2 corridor promises |
| 3 | `MAINT CACHE MISMATCH` must stay 0 | the T2 cache soak assert |
| 4 | `V29 BESPIN-FIRST RELEASED` in a TDIGWATT-variant (I'm Sorry) game; BLOCKING lines still present in classic TDIGWATT | bug B |
| 5 | `V61c BATTLE-INTENT` on separated-board turns (activate full) | battle-intent bypass |
| 6 | Rando CONCEDED the last Verge game after a Citadel Tower battle — review that battle's decisions (BattleEvaluator + forfeit picker) once the solo fix is in; may be the same stranded-body cause | new observation |

## 4. Session incidents — learn from my scars

- **V43 take-1 failure:** I wrote a text scan for "from reserve deck" — a phrase on NEITHER card. READ THE CARD SOURCE FIRST (`src/gemp-swccg-cards/.../CardX_YYY.java`), then write the scan, then verify the scoring OFFLINE against the real text before deploying. Steve's game caught it; that cost trust.
- **Compile exit masking:** `mvn ... | tail; echo EXIT=$?` reports tail's exit. Redirect to a file and echo mvn's own status.
- **Cross-bot mirroring by python splice dropped a brace.** Mirror with Read+Edit, verify with compile.
- **Replay files** (`replays/asdf/*.xml.gz`) are zlib streams (not gzip; use python zlib) and contain FULL-HISTORY RESENDS per client reconnect — parse only the LAST segment (rfind the objective reveal) or you triple-count events. The replay is engine ground truth; the decision log is Rando's brain. Use both.
- **Steve's game 1 "freeze" was his own client** not surfacing HIS starting-interrupt prompt (replay: "asdf lost due to: Decision timeout"). If Steve reports a hang, check WHOSE decision was pending before touching Rando.
- V191 TOPN is your forensic X-ray: every decision logs its top-5 candidates with scores. Diagnose from it before theorizing.

## 5. Standing constraints

- Local commits only, NOTHING pushed to GitHub. Never `docker compose down -v` / reset-db / unpin mariadb.
- Never deploy while Steve is mid-game (reload-ai restarts the JVM and kills tables). `tail logs/gemp-swccg.log` to check.
- Old rules get DOMINATED, not deleted: boundary math before magnitudes; comment out, never delete; update tags in place; mirror chosenone; both changelogs same session; work-verifier before "done".
- The MOVE side runs the clobber ladder now: new move behaviors are LADDER CLAIMS (rank + fine), never raw scores. Shared predicates go in `common/strategy/`.
- Council (if needed): `deepseek-r1:70b-llama-distill-q8_0` direct at `127.0.0.1:11434` (:8000 bridge down), ~4 min/call, hallucinates card text.
- Self-play harness: `cd mcp-gemp-client && python3 k2_player.py --deck "DARK DEAL" --ai-deck "LUKE SAGA TATOOINE" --games 1`. The /admin/botgame endpoint does NOT exist in this clone.

## 6. After queue #1+#2: the backlog (in priority order per Steve's pain)

1. deploy-siting-3 corpse-conveyor (audit CONFIRMED-high, still open): V96/V51 feeding
   bodies into lost contested fights — same family as the solo doctrine; the
   isDefensibleStack predicate is half the fix.
2. Remaining confirmed-medium audit rows (`resources/Rando_Overlap_Audit_2026-07-04.xlsx`,
   filter col M still-valid), esp. control-drain-4 (fallback brain gap — WIDENED).
3. battle-3 destiny-estimation split (V76 counts characters, tiers count ability).
4. Post-reorg strategy backlog: turn-posture object, battle-intent-at-deploy, retrieval
   scoring (CONTROL-2b), move hysteresis (plan §8).

## 7. Paste-ready prompt for the next session

> You are K-2 on GEMP-SWCCG at /Users/steve/gemp-swccg-public, running on Opus (Fable
> returns Thursday). Read MEMORY.md rules, then
> Handoffs/K2_HANDOFF_2026-07-07_reorg-build-day.md end to end. FIRST ACTION: `git
> status` — the tree holds draft edits from an interrupted fix workflow; §2 of the
> handoff tells you exactly what to do with them (refit to the approved STACK-MATH
> strategy, do not deploy as-is). Then execute queue #1 (solo doctrine + Verge
> post-flip), verify per §3, one change at a time, boundary math first, offload heavy
> lifting to subagents, and show Steve results in a short table.
