# BUILD_AND_DEPLOY.md — Rando AI deploy guide for a fresh K-2

A prior K-2 lost a full day "deploying" a fix into code that was compiled out (`if (false)`), then blamed the deploy. This guide teaches you to confirm the code is even live BEFORE you edit, then how to actually land a fix in `web.jar` and prove it fired.

Read section 1 first. Every time. No exceptions.

---

## 1. Before you edit any Rando rule: is the code even live?

The #1 lesson. Rando is full of V-tags that are taped off behind `if (false /* ... SUPERSEDED Vxxx */ ...)`. The compiler dead-code-eliminates them. An edit to one can NEVER compile in, can NEVER run, and will burn your whole session while you "verify a fix that does nothing."

Before touching ANY rule, grep its enclosing `if (...)` for a `false` guard:

```
grep -nE 'if \(false|&& false|false &&|SUPERSEDED V' \
  /Users/steve/gemp-swccg-public/src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.java
```

If your target line number falls between a dead block's `if (false` and its matching close brace, STOP. Editing it is wasted motion.

Known dead blocks in `DeployEvaluator.java` (confirmed, brace-balanced):

- V90 NO-SUICIDE-DEPLOY — `if (false /* V90 SUPERSEDED V136 */ ...)` opens line 1784, closes line 1830. Whole block dead.
- V67aj DEPLOY-DEST tiers — `if (false /* V67aj SUPERSEDED V136 */ ...)` opens line 3735, closes line 3886. Whole block dead.
- V67al POWER-STACK spread penalty — lives at lines 3829-3879, NESTED inside the V67aj dead block. Also dead.
- The 2026-06-24 "contested-site gate" edit (the `v67Contested` logic ~3758-3776, the "CONTESTED PILE-ON" branch ~3807-3811, the `&& !v67Contested` gate at 3852) also sits inside the V67aj `if (false)`. Inert. Never executes.

Those are the ONLY two `if (false)` guards in the file. Line 235 (`boolean v79DeathStarAtScarif = false;`) is an ordinary local that gets reassigned, NOT a guard. The `, false, false)` you'll see in V96/V67 are `getTotalPowerAtLocation(...)` args, not guards.

LIVE in `DeployEvaluator.java` (genuinely runs):

- V96 CONCENTRATE — lines 1849-1894, no `false` guard. Fires `+500` when a target site is contested and power diff is within ±10, `+100` when already crushing.

V136 is the live replacement for the dead V67aj / V67al / V90 spread+suicide logic. It lives in the shared engine:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/CharacterDeploySiteEvaluator.java`
- One method, `evaluateSite(...)`, returns a single additive float per (character, site). Side-symmetric (in `common/`, both bots call it).
- Two LIVE call sites in the rando bot, both un-gated: `DeployEvaluator.java:1759` (from-hand / action-text deploy route) and `CardSelectionEvaluator.java:2056` (CardSelection deploy route).
- `MoveEvaluator.java:970,980` are COMMENTS inside the live V137 move-side gate, NOT calls into V136.

What V136 actually does (the spread-vs-concentrate core lives here now):
- §A team viability (±2000): winnable deploy +500, out-powered -200/-500, ability<4 -1500, worst case -2000.
- §B strategic position (±700): uncontested over-stack penalty (-700/-400/-200 by power), but CONTESTED sites get NO over-stack cap (V157 "never cap a fight") plus a +200 overwhelm nudge.
- §C weapon modifiers (±50), §D turn-1/2 site-count gate (-700 until turn 3+).

So if you want to change spread/pile-on behavior, the live levers are V136 §A/§B in `CharacterDeploySiteEvaluator.java` and V96 in `DeployEvaluator.java:1849`. NOT the dead V67aj/V67al code.

Note: the master-handoff premise "V96/V67al magnitude inversion (+500 - 300 - 700 = -500)" is REFUTED. V67al is dead, so it cannot dominate V96. Do not chase that math.

### Unconfirmed — verify live
- The actual in-game "Rando spreads instead of piling on" complaint was NOT reproduced from a game log in these reports. The most likely live cause (per report B) is V136 §B misclassifying a contested site as uncontested when opponent power reads 0 (spy/undercover, or an under-counting power query), which fires the -700 over-stack penalty. Reproduce from a replay before editing. Do not guess.

---

## 2. The deploy procedure that lands in web.jar

The run artifact is the fat jar:

- `/Users/steve/gemp-swccg-public/src/gemp-swccg-async/target/web.jar` (44 MB, ~26k entries, maven-assembly fat jar, deps shaded flat at top level).
- It bundles the AI classes. All 70 rando `.class` files are inside, including `com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.class`.
- The host has no JRE, so build IN-CONTAINER via `docker exec`. The JVM reads `web.jar` once at process start, so a fresh jar only takes effect after a JVM restart.
- Host-side `web.jar` == container `web.jar` (same bind mount), so host-side reads are authoritative for verification.

### Reliable AI-only deploy sequence (exact commands)

```
docker exec gemp_swccg_app_1 bash -lc 'cd /opt/gemp-swccg/src && mvn -q -pl gemp-swccg-async -am package -DskipTests'
```
Rebuilds the server module (`-am` also-makes upstream deps) and re-shades it into `web.jar`.

```
cd /Users/steve/gemp-swccg-public/src && docker compose up -d --force-recreate build
```
Restarts the JVM so it reloads bytecode at boot. Prefer `--force-recreate` over a bare `docker compose restart build`: the bind is a macOS virtiofs / gRPC-FUSE host mount, and `--force-recreate` gives a new container with a fresh mount, reliably flushing any host-write → container-read staleness. A bare restart usually flushes too (the file's page cache drops on process exit) but is belt-without-suspenders.

```
./bin/gemp operational
```
Or wait for the inline `flip_operational`. Sets `shutdown=false`. This is the ONLY switch it flips.

Then MANUALLY flip the rest (login `asdf`/`asdf` first, then POST `enabled=true`):
- `/admin/settings/aitables`
- `/admin/settings/privategames`
- `/admin/settings/stattracking`
- `/admin/settings/newaccounts`

`flip_operational` does NOT touch those four. Operational alone won't load AI tables.

### bin/gemp reconciliation: which subcommand is safe, which is a trap

- `reload-ai` (line 280) — SAFE, the correct fast path. Runs `mvn -q -pl gemp-swccg-async -am package -DskipTests` in-container, then `docker compose restart build` + flip. Collapses the build+restart+operational steps.
- `reload` / `reload-fast` (236 / 258) — SAFE. Full `mvn install` (`-fast` skips tests), then restart + flip.
- `restart` (207) — SAFE for a JVM-only restart (no rebuild).
- `rebuild` / `rebuild-fast` (218 / 227) — TRAP. They run `mvn install` (which DOES rewrite `web.jar`) but then only print "Run restart" and DO NOT restart the JVM. The jar on disk is fresh; the running JVM still serves the OLD classes loaded at boot. If you stop here you've "deployed" nothing. Always follow with `restart`, or just use `reload-ai`.

None of the bin/gemp subcommands runs `mvn clean`, and all use bare `docker compose restart build`, not `--force-recreate`. For an AI-only edit that's normally fine; if you hit stale reads, fall back to the manual `--force-recreate` sequence above.

### Frontend 404 fix (web.zip)

If the client 404s on static assets after a deploy, the static `web` dir needs re-syncing. In THIS install `/opt/gemp-swccg/web` is a LIVE host bind of `gemp-swccg-async/src/main/web`, NOT a baked `web.zip` extract (both `/opt/gemp-swccg` and `/opt/gemp-swccg/web` show as `fakeowner` virtiofs binds). So the generic "unzip web.zip into /opt/gemp-swccg/web" step is install-specific and is NOT required here. Overwriting `newgui.html` wipes the epic-duel image shim, so don't blindly clobber it.

### Unconfirmed — verify live
- "A bare `docker compose restart build` stales the virtiofs bind-mount → 404s / stale reads." The mount IS confirmed virtiofs-class, but staling on a bare restart could NOT be reproduced (read-only pass). Treat `--force-recreate` as the safer default, not as proven-mandatory. There is an open contradiction: the handoff says `--force-recreate` is load-bearing, but the `feedback_docker_rebuild.md` memory now says a bare restart is fine. One is wrong. Resolve with a live test before trusting either; flag to Steve.

---

## 3. Verify it actually landed and fired

Four distinct gates, and passing one does NOT imply the next:

1. compiles  ≠  2. bundled into web.jar  ≠  3. the JVM loaded it  ≠  4. the rule fired in a game

Two cheap checks cover gates 2 and 4.

### Check A — host-side python byte-search of the class inside web.jar (gate 2)

This beats `strings` / `javap` on big classes, which truncate or lie on the 5933-line `DeployEvaluator`.

```
python3 - <<'PY'
import zipfile
JAR = "/Users/steve/gemp-swccg-public/src/gemp-swccg-async/target/web.jar"
CLS = "com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.class"
NEEDLE = "V96 CONCENTRATE"
data = zipfile.ZipFile(JAR).read(CLS)
print(NEEDLE, "PRESENT" if NEEDLE.encode() in data else "ABSENT")
PY
```

Swap `CLS` and `NEEDLE` for whatever you shipped. A dead `if (false)` V-tag will be ABSENT (compiler stripped it). That is your bytecode-level proof a fix is inert. Confirmed-present live tags: V96 CONCENTRATE, V136, V185 (in `rando/strategy/DeckOracle.class`), V186, the auto-pass server fix (`getAutoPassPhases` in `async/handler/GameRequestHandler.class`). Confirmed-absent (dead): V67aj, V67al, V90 in `DeployEvaluator.class`.

### Check B — play a real game and grep the V-tag log line (gates 3 and 4)

Bytecode present in the jar still doesn't prove the loaded JVM is running it, nor that the rule's branch was reached. Play a game, then grep the container log for the V-tag's `addReasoning` string:

```
docker exec gemp_swccg_app_1 bash -lc 'grep -n "V96 CONCENTRATE" /opt/gemp-swccg/src/nohup.out | tail'
```

If the line appears with a real score in a real decision, the rule fired. If the jar has the string but the log never shows it across games where it should fire, you loaded a stale JVM (gate 3 failed: re-restart with `--force-recreate`) or the branch's condition is never true.

---

## 4. Hard NOs for a code-only change

A Rando/AI edit is code-only. NEVER run these for it — they are for schema/DB work ONLY, and require Steve's explicit OK:

- `docker compose down -v` — wipes volumes / the database.
- `rm -rf database/` (or any delete of the DB dir).
- Floating / bumping the `mariadb` image tag.

Code-only deploys touch `web.jar` and the JVM, nothing else. If you reach for any DB-destructive command to "fix" a code deploy, you are on the wrong path — revert and re-read section 2.

---

## 5. chosenone caveat

There are two bots. `RandoCalAi` is the LIVE one. A fix mirrored into the `chosenone` bot ships ONLY if `chosenone` is also rebuilt into `web.jar` in the same package step.

- Editing `chosenone` source alone, or editing `rando` and assuming `chosenone` inherits it, ships nothing for the other bot.
- Verify per-bot with Check A: e.g. V185's `reserveTargetsAreAllUnattachableWeapons` is PRESENT in the rando `DeckOracle.class` but ABSENT in the `chosenone` copy — they are separate classes, separately compiled.
- If a behavior must hold for both bots, edit both and confirm the byte-search passes in BOTH classes before claiming done.
