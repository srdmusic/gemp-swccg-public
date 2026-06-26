# BUILD_AND_DEPLOY — GEMP-SWCCG

**Self-contained build/deploy reference. No Claude-memory dependencies. The local K2 can read this directly.**

---

## TL;DR — 99% of changes use this

```bash
cd /Users/steve/gemp-swccg-public/src && mvn -q -pl gemp-swccg-async -am package -DskipTests
/Users/steve/gemp-swccg-public/bin/gemp restart
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:17001/gemp-swccg/   # expect 200
```

If you see `HTTP 200`, the server is up with your new code. **`./bin/gemp restart` automatically flips operational mode** (logs in as admin, posts `enabled=false` to `/admin/shutdown`). You don't need to do it manually anymore.

---

## When to use which path

The decision tree is **only about one thing: did you touch the DB?**

| What you changed | Risk to decks | Use |
|------------------|---------------|-----|
| Java in `gemp-swccg-server/`, `-cards/`, `-logic/`, `-async/` (AI rules, evaluators, card logic) | **None** | **Fast path** (TL;DR above) |
| `src/docker/gemp_db.Dockerfile`, `db-scripts/`, anything that touches DB image or schema | **High** | **Full nuke path** with backup (below) |
| Just edited a `.md` file | None | Nothing |

**99% of AI work is fast path.** The full nuke path has only been needed twice in the project's history.

---

## Fast path (code-only changes — what you'll use all the time)

```bash
# 1. Compile (~30-60 sec)
cd /Users/steve/gemp-swccg-public/src
mvn -q -pl gemp-swccg-async -am package -DskipTests

# 2. Restart container (auto-flips operational mode)
/Users/steve/gemp-swccg-public/bin/gemp restart

# 3. Verify server is responding
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:17001/gemp-swccg/
# Expected: 200
```

**Why this works:** The repo uses a bind-mount — `/Users/steve/gemp-swccg-public/src` is mapped into the container as `/opt/gemp-swccg/src`. The `web.jar` artifact from `mvn package` is already inside the container after the build. Restarting the JVM picks up the new bytecode.

**Time:** ~60 seconds total (build + restart + ready).

---

## Full nuke path (DB image or schema change)

Only use this if you changed something in `src/docker/`, `db-scripts/`, or a DB-touching DAO. Always backup the DB first.

```bash
# 0. BACKUP THE DECKS — non-negotiable
docker exec gemp_swccg_db_1 mariadb-dump -uroot -pgempukku --all-databases > ~/gemp_db_backup_$(date +%Y%m%d_%H%M%S).sql

# 1. Build locally
cd /Users/steve/gemp-swccg-public/src && mvn clean install -DskipTests -q

# 2. Full rebuild (WITHOUT -v — that would wipe the bind mount)
docker compose down
docker compose build --no-cache
docker compose up -d

# 3. Wait for DB to come up, then restart app to clear init race condition
sleep 15 && docker restart gemp_swccg_app_1

# 4. Wait for app to start, then unzip web
sleep 5 && docker exec gemp_swccg_app_1 bash -c \
    'cd /opt/gemp-swccg/web && unzip -o /opt/gemp-swccg/src/gemp-swccg-async/target/web.zip > /dev/null && echo OK'

# 5. Flip operational mode + AI tables on
/Users/steve/gemp-swccg-public/bin/gemp operational
```

**Time:** ~5-10 minutes.

---

## NEVER (data-loss landmines)

| Command | What it does to you |
|---------|---------------------|
| `docker compose down -v` | The `-v` removes named volumes. Currently the repo uses bind mounts so it's mostly safe, but the moment someone switches to named volumes this becomes destructive. Just don't use `-v`. |
| Bump `mariadb:11.8.6` to a floating tag like `mariadb:11` | This is exactly how Steve lost all his decks once. The pin is in `src/docker/gemp_db.Dockerfile`. Don't touch it without a backup first. |
| `git commit --no-verify` | Pre-commit hooks exist for a reason. Don't bypass them. |
| `docker compose build --no-cache` of the WHOLE stack when only Java changed | Wastes 10+ minutes. Scope to `--no-cache build` (the app service) if you're using the full nuke path. |

---

## Verifying a fix actually fires (critical discipline)

The single biggest source of "ship and pray" bugs in this codebase: code that compiles but never runs at runtime. After ANY rebuild, immediately verify your new V-tag's log line appears:

```bash
# Replace V128 with your tag
docker exec gemp_swccg_app_1 grep "V128" /root/nohup.out | tail -5
```

If grep returns nothing after a few in-game decisions that should trigger your rule, **the rule isn't firing.** The code is loaded but the condition is wrong, the code path is wrong, or the rule is being short-circuited by another rule. Don't tell Steve "shipped" until you see your tag in the log.

Confirm bytecode contains your tag if you want absolute proof:

```bash
docker cp gemp_swccg_app_1:/opt/gemp-swccg/src/gemp-swccg-async/target/web.jar /tmp/web.jar
unzip -p /tmp/web.jar com/gempukku/swccgo/ai/models/rando/evaluators/DeployEvaluator.class \
  | strings | grep V128
```

---

## Push to remote

```bash
cd /Users/steve/gemp-swccg-public
git add <files>
git commit -m "..."
git push dev-fork ai-improvements-v91
```

- **`dev-fork`** = `https://github.com/srdmusic/gemp-swccg.git` (where AI changes go)
- **`origin`** = `https://github.com/PlayersCommittee/gemp-swccg-public.git` (read-only for us)

**ALWAYS update both `AI_CHANGELOG.md` and `AI_VERSION_HISTORY.md` in the same commit as AI code changes.** No exceptions. The changelogs are the only way future K2 sessions reconstruct rule intent.

---

## Logs — where to find them

| What | Where |
|------|-------|
| AI decision-level logs (V-tag fires, score breakdowns) | `docker exec gemp_swccg_app_1 tail -2000 /root/nohup.out` |
| Server chat logs only | `/Users/steve/gemp-swccg-public/logs/` (NOT decision-level — don't waste time here) |
| Replay files (zlib-compressed XML) | `/Users/steve/gemp-swccg-public/replays/{username}/{id}.xml.gz` |
| Database files (bind-mounted) | `/Users/steve/gemp-swccg-public/database/` |

Read a replay:

```python
import zlib
with open('/Users/steve/gemp-swccg-public/replays/~Rando_Cal/abc123.xml.gz', 'rb') as f:
    text = zlib.decompress(f.read()).decode('utf-8', errors='replace')
```

---

## Service / container reference

| Container name | What it is | Host port |
|----------------|------------|-----------|
| `gemp_swccg_app_1` | Java/Netty app server | 17001 |
| `gemp_swccg_db_1` | MariaDB 11.8.6 (pinned) | 3306 |

```bash
docker ps                                              # status
docker logs --tail 50 gemp_swccg_app_1                 # container logs (small — most output is inside container)
docker exec gemp_swccg_app_1 tail -200 /root/nohup.out # the real logs
```

---

## `bin/gemp` commands (already-wrapped sequences)

```bash
./bin/gemp restart       # restart app container + auto-flip operational mode + AI tables on
./bin/gemp reload-fast   # mvn package + restart (no full clean) — fastest iteration
./bin/gemp reload-ai     # targeted async-module build + restart + operational flip
./bin/gemp operational   # standalone: just flip operational mode (use if a restart didn't auto-flip)
./bin/gemp logs          # tail container logs
./bin/gemp initialize    # one-time setup from scratch
```

If `./bin/gemp restart` ever stops auto-flipping operational (you'll see "Server is not yet in operational mode" in the chat hall), run `./bin/gemp operational` to force the flip.

---

## When something doesn't work

| Symptom | Diagnostic |
|---------|------------|
| `mvn` fails | Read the error. Usually a syntax mistake in your Java change. |
| Container won't start | `docker logs --tail 50 gemp_swccg_app_1` |
| HTTP 200 but bot behaves oddly | `docker exec gemp_swccg_app_1 grep "V12X" /root/nohup.out` to see if your rule fired |
| HTTP 000 (no response) | Container is up but JVM is wedged. Try `./bin/gemp restart`. If still wedged, `docker logs --tail 200 gemp_swccg_app_1` for the stack trace. |
| "Server is not yet in operational mode" in the hall | Operational flip didn't fire. Run `./bin/gemp operational` |
| Your V-tag doesn't appear in `/root/nohup.out` | Either the code wasn't loaded (rebuild?), the condition is wrong, or the rule is being short-circuited |

---

*Updated 2026-05-22 as part of the K2 handoff. If you change build/deploy steps, update this file in the same commit.*
