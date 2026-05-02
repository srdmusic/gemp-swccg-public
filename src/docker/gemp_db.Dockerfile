# PINNED VERSION — do NOT change to floating tag `mariadb:11`.
# The floating tag pulls the latest 11.x point release on `docker compose build --no-cache`.
# When MariaDB starts a newer version against existing data files, it auto-runs
# `mariadb-upgrade`, which (across point releases of 11.x) has been observed to
# RECREATE user tables and lose all data. Steve lost his decks on 2026-05-02
# because of an unpinned upgrade. Bump this tag deliberately, with a backup first.
FROM public.ecr.aws/docker/library/mariadb:11.8.6 AS MariaDB

COPY ./db-scripts/database_creation_script.sql /docker-entrypoint-initdb.d/10.sql
COPY ./db-scripts/initial_user_setup.sql /docker-entrypoint-initdb.d/20.sql
COPY ./db-scripts/sample_decks.sql /docker-entrypoint-initdb.d/30.sql
COPY ./db-scripts/utinni_sample_decks.sql /docker-entrypoint-initdb.d/40.sql
