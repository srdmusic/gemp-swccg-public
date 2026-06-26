# K-2 HANDOFF — 2026-06-24: cleanup + the real deploy story

**For the new K-2.** This is a reply to `K2_HANDOFF_2026-06-24_deadcode-lesson.md`. A prior K-2 lost a day editing dead code, wrote it up honestly, and asked for help. I verified its findings against the real source AND the live `web.jar`, fixed what it left misleading, and wrote you a deploy guide. Here is exactly what happened and what is now true.

---

## The prior K-2 was RIGHT about the dead code (bytecode-verified)
- V67aj / V67al / V90 in `DeployEvaluator.java` live inside `if (false /* SUPERSEDED V136 */)` blocks. The Java compiler strips them. Byte-search of the LIVE `web.jar`: `V67aj` = 0, `V67al` = 0, `V90 NO SUICIDE` = 0, `POWER-STACK` = 0 occurrences. They do NOT run.
- V96 CONCENTRATE (+500) IS live: `DeployEvaluator.java:1849-1894`, no false guard, 4 occurrences in the jar.
- The live replacement is V136: `common/strategy/CharacterDeploySiteEvaluator.java` `evaluateSite`, called un-gated from `DeployEvaluator.java:1759` and `CardSelectionEvaluator.java:2056`. Its §B (uncontested over-stack penalty, no cap on contested sites) is the real spread-vs-pile-on logic now.
- So the master handoff's "V96/V67al magnitude inversion" was a phantom. V67al cannot dominate V96 because it never runs. I corrected master-handoff queue item 2.
- The deploy was never broken. The prior K-2 edited a switch that was taped off.

## What I cleaned up (the tree is honest again)
- Reverted the 3 dead-code edits in `DeployEvaluator.java` (they sat inside the `if(false)` block, zero runtime effect, pure clutter). `git checkout`.
- Reverted the misleading 2026-06-24 entry in `resources/AI_CHANGELOG.md`. This was the dangerous artifact: it documented a behavior fix that physically cannot run. A future K-2 would have trusted it and burned a session "verifying" nothing.
- Replaced the matching misleading note in `AI_VERSION_HISTORY.md` (in k2-resources, untracked) with an accurate NOTE: V67al/aj are dead, V96 stands alone, the edit was reverted.
- Left `feedback_docker_rebuild.md` mostly intact. On inspection it was accurate: the real "changes didn't take" cause is a missing `-am` flag (async pulls a stale server jar from `~/.m2`), NOT a Docker cache. I added a one-line `--force-recreate` fallback for consistency.

## The deploy reality (verified against the live jar)
- Run artifact: `src/gemp-swccg-async/target/web.jar` (44 MB fat jar, bundles all 70 rando AI classes; bind-mounted, so host == container).
- Fast path for an AI edit: `./bin/gemp reload-ai`. It runs `mvn -q -pl gemp-swccg-async -am package -DskipTests` in-container, restarts the JVM, sets operational. The `-am` is load-bearing (it recompiles the AI module into the shaded jar).
- THE TRAP: `rebuild` / `rebuild-fast` rebuild `web.jar` but DO NOT restart the JVM (they just print "Run restart"). The jar on disk is fresh, the running process still serves the OLD classes. Never use them alone.
- After ANY restart, flip the switches manually: login `asdf`, `shutdown=false`, then `aitables`/`privategames`/`stattracking`/`newaccounts` `enabled=true`. `bin/gemp` only flips `shutdown`.
- `--force-recreate` over a bare restart is belt-and-suspenders, NOT proven mandatory. Staling could not be reproduced.
- Full detail: `resources/BUILD_AND_DEPLOY.md`. Read it before your first deploy.

## Verify a fix actually shipped (four gates, each one earned)
compiles  ≠  bundled into web.jar  ≠  the JVM loaded it  ≠  the rule fired in a game.
- Gate 2: host-side python byte-search of the class inside `web.jar` (beats container `strings`/`javap`, which lie on big classes).
- Gate 4: play a real game, grep the container `nohup.out` for the V-tag's log string.

## The #1 lesson (now a standing memory)
Before editing ANY Rando rule, grep its enclosing `if (...)` for `if (false /* SUPERSEDED Vxxx */)`. Lots of old V-tags are taped off and compiled out. One grep saves a day. See `feedback_check_rule_is_live_before_editing` in memory and `BUILD_AND_DEPLOY.md` §1.

## What is actually open (NOT the dead V67al)
- Is "Rando spreads instead of piling on" even real in the LIVE code? Reproduce it from a replay FIRST. The live levers are V136 §B + V96. Likely suspect (UNCONFIRMED): a contested site reading as uncontested when opponent power = 0 (spy/undercover, or an undercounting power query), which fires the -700 over-stack penalty. Do not edit before you reproduce.
- V185 + V186 are deployed and bytecode-confirmed live. Still pending: confirm each FIRES in a real game, and the `chosenone` mirror (master-handoff queue item 7).
- The 3 dead V136 stubs in `CharacterDeploySiteEvaluator.java` (queue item 4) are a SEPARATE issue (literal-value stubs like `deckShipCount = 0`, not `if(false)`). Still real, still open.

You inherit a clean, honest tree. Start at `K2_MASTER_HANDOFF_2026-06-23.md`, read `resources/BUILD_AND_DEPLOY.md` before your first deploy, and grep for `if(false)` before your first edit.
