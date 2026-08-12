# Exact Chosen One vs Rando validation runner

`run_exact_chosen_rando.py` runs one controlled game only:

* Light: `~The_Chosen_One`, direct `CHOSENONE` controller
* Dark: `~Rando_Cal`, direct `RANDO` controller
* Decks: exact existing decks owned by the configured deck owner
* Result accepted: natural Life Force depletion only

It does not enable settings, change shutdown state, write to the database, edit
decks, select replays by modification time, retry the game request, or start a
second game.

## Offline test

Run this before using the live server:

```bash
cd /Users/steve/gemp-chosen-rando-botgame-2026-08-12/outputs/chosen-rando-botgame-2026-08-12
python3 -m unittest -v test_run_exact_chosen_rando.py
```

The tests use temporary replay and log fixtures. They do not contact Hall,
MariaDB, or the game server.

## Required environment

Set these values in the shell that will run the validator:

```bash
export GEMP_ROOT=/Users/steve/gemp-swccg-public
export GEMP_BASE_URL=http://localhost:17001
export GEMP_ADMIN_USERNAME='<admin username>'
read -r -s GEMP_ADMIN_PASSWORD
export GEMP_ADMIN_PASSWORD
export GEMP_BOTGAME_FORMAT='<format code>'
export GEMP_BOTGAME_LIGHT_DECK='<exact Light deck name>'
export GEMP_BOTGAME_DARK_DECK='<exact Dark deck name>'
export GEMP_BOTGAME_DECK_OWNER='<exact deck owner>'
export GEMP_BOTGAME_ARM=CHOSENONE_LIGHT_VS_RANDO_DARK_ONCE
```

Optional values:

```bash
export GEMP_DB_CONTAINER=gemp_swccg_db_1
export GEMP_BOTGAME_TIMEOUT_SECONDS=1800
```

The database client reads `MYSQL_USER`, `MYSQL_PASSWORD`, and `MYSQL_DATABASE`
inside the configured MariaDB container. Database credentials are not copied
into the runner process, command line, output, or error text.

## Run once

```bash
python3 /Users/steve/gemp-chosen-rando-botgame-2026-08-12/outputs/chosen-rando-botgame-2026-08-12/run_exact_chosen_rando.py
```

The runner performs this fixed sequence:

1. Log in and require an authenticated session cookie.
2. Require zero `WAITING` and zero `PLAYING` Hall tables.
3. Wait 15 seconds and require zero active tables again.
4. Record a SELECT-only `game_history` high-water mark.
5. Send exactly one synchronous `POST /admin/botgame` request.
6. Require Hall to be empty after the response.
7. Resolve exactly one new participant, deck, and time-matched DB row.
8. Reject cancellation, concession, decision timeout, and game timeout.
9. Resolve both replay paths from DB recording IDs and participant directories.
10. Inflate raw zlib XML, use only the final history segment, and require equal
    public-message fingerprints and terminal records.
11. Require exact Light and Dark controller registration lines for the returned
    runtime `gameId`, with no exact-game abort or error log.

Every run writes a JSON packet under `evidence_reports/`. Exit code `0` means the
entire evidence contract passed. Exit code `2` means it failed closed.

If the POST times out or the result is ambiguous, do not rerun it automatically.
Inspect Hall, `game_history`, replays, and logs first. The server may have
completed the game after the client connection ended.
