#!/usr/bin/env bash
# backup_db.sh — dump the gemp-swccg MariaDB to a timestamped .sql file.
# ALWAYS run before any rebuild that might touch the DB image.
#
# Usage: ./scripts/backup_db.sh
# Output: ~/gemp_db_backups/gemp_db_YYYYMMDD_HHMMSS.sql
#
# Restore: docker exec -i gemp_swccg_db_1 mariadb -uroot -pgempukku < backup.sql

set -euo pipefail

BACKUP_DIR="${HOME}/gemp_db_backups"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/gemp_db_${TIMESTAMP}.sql"

mkdir -p "${BACKUP_DIR}"

if ! docker ps --format '{{.Names}}' | grep -q '^gemp_swccg_db_1$'; then
    echo "ERROR: gemp_swccg_db_1 container is not running. Start it with 'docker compose up -d' first."
    exit 1
fi

echo "Dumping gemp-swccg DB to ${BACKUP_FILE}..."
docker exec gemp_swccg_db_1 mariadb-dump \
    -uroot -pgempukku \
    --all-databases \
    --single-transaction \
    --quick \
    > "${BACKUP_FILE}"

SIZE="$(du -h "${BACKUP_FILE}" | cut -f1)"
DECK_COUNT="$(grep -c '^INSERT INTO `deck`' "${BACKUP_FILE}" || true)"

echo "✓ Backup complete: ${BACKUP_FILE} (${SIZE}, ${DECK_COUNT} deck INSERT statements)"
echo ""
echo "Keeping last 10 backups; older ones removed:"
ls -t "${BACKUP_DIR}"/gemp_db_*.sql 2>/dev/null | tail -n +11 | xargs -I {} sh -c 'echo "  removed: {}"; rm "{}"' || true
