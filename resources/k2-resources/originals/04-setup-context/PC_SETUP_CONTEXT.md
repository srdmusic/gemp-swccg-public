# GEMP-SWCCG Windows PC Setup — Full Context from Mac Claude Session

**Date:** March 14, 2026
**Owner:** Steve (steve@srdmusic.com)
**Project:** GEMP-SWCCG (online Star Wars CCG platform with AI bot "Rando Cal")

---

## WHAT IS THIS DOCUMENT

This is a full context dump from a Claude session on Steve's Mac. The goal is to get GEMP-SWCCG running in Docker on Steve's Windows PC at `C:\Users\Steve\Documents\gemp-swccg-public`. The project was copied from the Mac. A code change (V29.13) was also made to `MoveEvaluator.java` during this session and needs to be compiled after Docker is running.

---

## PROJECT STRUCTURE (Key Files)

```
gemp-swccg-public/
├── src/
│   ├── docker/
│   │   ├── docker-compose.yml          ← NEEDS PATH EDITS FOR WINDOWS
│   │   ├── docker-compose-windows.yml  ← PRE-EDITED VERSION (if it was copied from Mac)
│   │   ├── .env                        ← NO changes needed
│   │   ├── gemp_app.Dockerfile         ← NO changes needed
│   │   └── gemp_db.Dockerfile          ← NO changes needed
│   ├── db-scripts/
│   │   ├── database_creation_script.sql
│   │   ├── initial_user_setup.sql
│   │   ├── sample_decks.sql
│   │   ├── update_add_bot_stats.sql    ← NOT used in Dockerfile
│   │   └── utinni_sample_decks.sql     ← THIS FILE CAUSED BUILD FAILURES
│   └── gemp-swccg-server/
│       └── src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/
│           └── MoveEvaluator.java       ← V29.13 code change (already in files)
├── database/                            ← MariaDB persistent data (bind-mounted)
└── logs/
    └── nohup.out                        ← App log file
```

---

## TASK 1: FIX docker-compose.yml PATHS

The `docker-compose.yml` has 5 hardcoded Mac paths that must be changed to Windows paths.

**File:** `C:\Users\Steve\Documents\gemp-swccg-public\src\docker\docker-compose.yml`

**Find and replace ALL instances of:**
```
/Users/steve/gemp-swccg-public
```
**Replace with:**
```
C:/Users/Steve/Documents/gemp-swccg-public
```

**Specific lines (5 occurrences):**

| Line | Original (Mac) | Replacement (Windows) |
|------|----------------|----------------------|
| 27 | `source: /Users/steve/gemp-swccg-public/src/gemp-swccg-async/src/main/web` | `source: C:/Users/Steve/Documents/gemp-swccg-public/src/gemp-swccg-async/src/main/web` |
| 30 | `source: /Users/steve/gemp-swccg-public` | `source: C:/Users/Steve/Documents/gemp-swccg-public` |
| 33 | `source: /Users/steve/gemp-swccg-public/logs` | `source: C:/Users/Steve/Documents/gemp-swccg-public/logs` |
| 36 | `source: /Users/steve/gemp-swccg-public/logs/nohup.out` | `source: C:/Users/Steve/Documents/gemp-swccg-public/logs/nohup.out` |
| 70 | `source: /Users/steve/gemp-swccg-public/database` | `source: C:/Users/Steve/Documents/gemp-swccg-public/database` |

**ALTERNATIVELY:** If `docker-compose-windows.yml` was copied from the Mac, just rename it to replace `docker-compose.yml`. It already has all the correct Windows paths.

**The .env file does NOT need any changes.** It only has container-internal paths and ports.

---

## TASK 2: FIX THE utinni_sample_decks.sql BUILD FAILURE

### The Error
Docker build fails at this step in `gemp_db.Dockerfile`:
```
COPY ./db-scripts/utinni_sample_decks.sql /docker-entrypoint-initdb.d/40.sql
```

The first 3 SQL COPY steps show "CACHED" but the 4th fails. This is likely a Docker build cache issue OR the file didn't get copied to the PC.

### How the DB Dockerfile works
`gemp_db.Dockerfile` has `context: ..` in docker-compose.yml, meaning Docker's build context is `src/` (one level up from `docker/`). The COPY paths are relative to `src/`:
```dockerfile
FROM public.ecr.aws/docker/library/mariadb:11 AS MariaDB
COPY ./db-scripts/database_creation_script.sql /docker-entrypoint-initdb.d/10.sql
COPY ./db-scripts/initial_user_setup.sql /docker-entrypoint-initdb.d/20.sql
COPY ./db-scripts/sample_decks.sql /docker-entrypoint-initdb.d/30.sql
COPY ./db-scripts/utinni_sample_decks.sql /docker-entrypoint-initdb.d/40.sql
```

### Fix Steps
1. **Verify the file exists on PC:**
   ```powershell
   dir C:\Users\Steve\Documents\gemp-swccg-public\src\db-scripts\
   ```
   You should see all 5 .sql files. If `utinni_sample_decks.sql` is missing, it needs to be copied from the Mac.

2. **Clear Docker build cache:**
   ```powershell
   docker builder prune -f
   ```

3. **Rebuild with no cache:**
   ```powershell
   cd C:\Users\Steve\Documents\gemp-swccg-public\src\docker
   docker compose build --no-cache
   ```

---

## TASK 3: CREATE REQUIRED DIRECTORIES AND FILES

Before running Docker, make sure these exist:

```powershell
# Create logs directory and nohup.out if they don't exist
mkdir C:\Users\Steve\Documents\gemp-swccg-public\logs
echo. > C:\Users\Steve\Documents\gemp-swccg-public\logs\nohup.out

# Create database directory if it doesn't exist
mkdir C:\Users\Steve\Documents\gemp-swccg-public\database
```

**nohup.out note:** In a previous attempt, this file was read-only/locked and caused an "access denied" error. If that happens:
```powershell
del C:\Users\Steve\Documents\gemp-swccg-public\logs\nohup.out
echo. > C:\Users\Steve\Documents\gemp-swccg-public\logs\nohup.out
```

---

## TASK 4: START DOCKER

```powershell
cd C:\Users\Steve\Documents\gemp-swccg-public\src\docker
docker compose up -d
```

**IMPORTANT:** You MUST run this from the `src\docker\` directory, or you'll get "no configuration file provided: not found". Alternatively:
```powershell
cd C:\Users\Steve\Documents\gemp-swccg-public\src
docker compose -f docker/docker-compose.yml up -d
```

---

## TASK 5: BUILD THE PROJECT (After Docker is Running)

```powershell
docker compose exec build bash -c "cd /opt/gemp-swccg/src && mvn install -DskipTests -rf :gemp-swccg-server"
docker compose restart build
```

This compiles the Java code (including the V29.13 MoveEvaluator changes) and restarts the app container.

---

## TASK 6: ACCESS GEMP

Once running, access GEMP at: `http://localhost:17001`
(Port is `1700${SERVID}` where SERVID=1, so 17001)

---

## ERRORS WE ENCOUNTERED (Full History)

### Error 1: "no configuration file provided: not found"
**Cause:** Running `docker compose up -d` from the wrong directory.
**Fix:** Must be in `src\docker\` directory, or use `-f` flag to specify the compose file path.

### Error 2: utinni_sample_decks.sql COPY failed
**Cause:** Docker build cache corruption or file missing on PC. The first 3 SQL files showed CACHED but the 4th failed.
**Fix:** `docker builder prune -f` then `docker compose build --no-cache`

### Error 3: docker-compose-windows.yml not found
**Cause:** The Windows-path version was created on the Mac's Cowork sandbox. It may or may not have been copied to the PC.
**Fix:** Either copy it from Mac, or just edit the regular `docker-compose.yml` directly (see Task 1).

### Error 4: PowerShell parsing errors (hundreds of red errors)
**Cause:** Steve accidentally pasted Docker error output back into PowerShell as commands. PowerShell tried to execute the error text.
**Fix:** Don't paste error output into the terminal. Type commands fresh.

### Error 5: nohup.out access denied
**Cause:** File was read-only or locked by a previous container run.
**Fix:** `del` the file and recreate it: `echo. > C:\...\logs\nohup.out`

---

## V29.13 CODE CHANGE (Already in MoveEvaluator.java)

The V29.13 Vader Movement Grouping code is already written in `MoveEvaluator.java` at lines 433-578. It was written during this Mac session and should already be in the files that were copied to the PC.

**What it does:** Prevents Rando's characters from scattering during the Move phase when playing Hunt Down And Destroy The Jedi (V). Characters get bonuses for moving toward Vader and penalties for moving away.

**Score values:**
- Vader moving TOWARD allies: +200 (+250 if ally power >= 8)
- Vader moving AWAY from allies to empty location: -200
- Vader moving toward opponents (hunting): no penalty (this is good)
- Non-Vader WITH Vader, moving away: -250
- Non-Vader NOT with Vader, moving TOWARD Vader: +250
- Non-Vader NOT with Vader, moving elsewhere: -100

**The code has been verified:** 332/332 braces balanced. But it has NOT been compiled yet — that happens in Task 5 above.

---

## DOCKER CONFIGURATION DETAILS

### .env file (NO changes needed)
```
SERVID=1
COMPOSE_PROJECT_NAME=gemp_swccg_${SERVID}
APP_CONTAINER_NAME=gemp_swccg_app_${SERVID}
APP_IP=172.29.${SERVID}.2
APP_PORT=1700${SERVID}
INTERNAL_PORT=80
DEBUGGER_PORT=80${SERVID}0
DB_CONTAINER_NAME=gemp_swccg_db_${SERVID}
DB_IP=172.29.${SERVID}.3
DB_PORT=3500${SERVID}
MYSQL_ROOT_PASSWORD=gempukku
MYSQL_DATABASE=gemp-swccg
MYSQL_USER=gemp
MYSQL_PASSWORD=Four_mason8pirate
APPLICATION_ROOT=/opt/gemp-swccg
WEB_PATH=/opt/gemp-swccg/web/
PLAYTESTING_NO_DECK_VALIDATION=false
```

### docker-compose.yml (AFTER Windows path edits)
```yaml
services:
  build:
    env_file:
      - .env
    container_name: ${APP_CONTAINER_NAME}
    build:
      context: .
      dockerfile: gemp_app.Dockerfile
    image: gemp_app
    depends_on:
      - db
    expose:
      - "80"
      - "8080"
    restart: unless-stopped
    ports:
      - target: 80
        published: "${APP_PORT}"
      - target: 8000
        published: "${DEBUGGER_PORT}"
    volumes:
       - type: bind
         source: C:/Users/Steve/Documents/gemp-swccg-public/src/gemp-swccg-async/src/main/web
         target: /opt/gemp-swccg/web
       - type: bind
         source: C:/Users/Steve/Documents/gemp-swccg-public
         target: /opt/gemp-swccg
       - type: bind
         source: C:/Users/Steve/Documents/gemp-swccg-public/logs
         target: /logs
       - type: bind
         source: C:/Users/Steve/Documents/gemp-swccg-public/logs/nohup.out
         target: /opt/gemp-swccg/src/nohup.out
    networks:
      gemp_net_1:
        ipv4_address: ${APP_IP}
    tty: true
    command: >
      nohup
      java -Xmx4g
      -Dlog4j.debug
      -Dlog4j.configurationFile=/opt/gemp-swccg/src/gemp-swccg-async/src/main/resources/prod-log4j.xml
      -jar /opt/gemp-swccg/src/gemp-swccg-async/target/web.jar
      com.gempukku.swccgo.async.SwccgoAsyncServer &"

  db:
    env_file:
      - .env
    container_name: ${DB_CONTAINER_NAME}
    build:
      context: ..
      dockerfile: docker/gemp_db.Dockerfile
    image: gemp_db
    ports:
      - target: 3306
        published: "${DB_PORT}"
    restart: unless-stopped
    volumes:
       - type: bind
         source: C:/Users/Steve/Documents/gemp-swccg-public/database
         target: /var/lib/mysql
    networks:
      gemp_net_1:
        ipv4_address: ${DB_IP}

networks:
  gemp_net_1:
    ipam:
      driver: default
      config:
        - subnet: 172.29.${SERVID}.0/24
    attachable: true
```

### gemp_db.Dockerfile (NO changes needed)
```dockerfile
FROM public.ecr.aws/docker/library/mariadb:11 AS MariaDB
COPY ./db-scripts/database_creation_script.sql /docker-entrypoint-initdb.d/10.sql
COPY ./db-scripts/initial_user_setup.sql /docker-entrypoint-initdb.d/20.sql
COPY ./db-scripts/sample_decks.sql /docker-entrypoint-initdb.d/30.sql
COPY ./db-scripts/utinni_sample_decks.sql /docker-entrypoint-initdb.d/40.sql
```

### gemp_app.Dockerfile (NO changes needed)
Based on `amazoncorretto:21-alpine-jdk`, installs Maven 3.9.6. Working directory is `/opt/gemp-swccg/src`.

---

## QUICK START CHECKLIST

1. [ ] Verify `utinni_sample_decks.sql` exists: `dir C:\Users\Steve\Documents\gemp-swccg-public\src\db-scripts\`
2. [ ] Edit `docker-compose.yml` — replace all `/Users/steve/gemp-swccg-public` with `C:/Users/Steve/Documents/gemp-swccg-public` (5 places)
3. [ ] Create `logs\` dir and `logs\nohup.out` if missing
4. [ ] Create `database\` dir if missing
5. [ ] Clear Docker cache: `docker builder prune -f`
6. [ ] Start Docker: `cd C:\Users\Steve\Documents\gemp-swccg-public\src\docker && docker compose up -d`
7. [ ] Build project: `docker compose exec build bash -c "cd /opt/gemp-swccg/src && mvn install -DskipTests -rf :gemp-swccg-server" && docker compose restart build`
8. [ ] Test at http://localhost:17001
9. [ ] Play Hunt Down V game to test V29.13 Vader grouping

---

## NOTE ON DUAL GEMP FOLDERS

During the Mac session, it appeared the PC might have TWO gemp folders:
- `C:\Users\Steve\gemp-swccg-public`
- `C:\Users\Steve\Documents\gemp-swccg-public`

The **Documents** one is the intended target. Make sure Docker paths point there.
