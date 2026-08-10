#!/usr/bin/env bash
set -euo pipefail

DUMP="${1:-}"
CONTAINER="${CONTAINER:-accsaber-postgres}"
BACKEND="${BACKEND:-}"
COMPOSE_PROJECT="${COMPOSE_PROJECT:-}"
COMPOSE_SERVICE="${COMPOSE_SERVICE:-backend}"
DB="${POSTGRES_DB:-accsaber}"
DB_USER="${POSTGRES_USER:-accsaber}"
OUT_DIR="${OUT_DIR:-$HOME/accsaber-backups}"

if [[ -z "$DUMP" || ! -f "$DUMP" ]]; then
  [[ -n "$DUMP" ]] && echo "error: no such file: $DUMP" >&2
  echo "usage: $0 <path-to.dump>" >&2
  echo >&2
  echo "available in $OUT_DIR:" >&2
  find "$OUT_DIR" -name 'accsaber-2*.dump' -printf '  %TY-%Tm-%Td %TH:%TM  %10s  %p\n' 2>/dev/null \
    | sort -r >&2 || echo "  (none)" >&2
  exit 1
fi

echo "This DESTROYS the current '$DB' database and replaces it with:"
echo "  $DUMP  ($(du -h "$DUMP" | cut -f1), modified $(date -r "$DUMP" -u +%Y-%m-%dT%H:%M:%SZ))"
echo
read -r -p "Type the database name '$DB' to confirm: " CONFIRM
if [[ "$CONFIRM" != "$DB" ]]; then
  echo "aborted" >&2
  exit 1
fi

if [[ -z "$BACKEND" ]]; then
  if [[ -z "$COMPOSE_PROJECT" ]]; then
    echo "error: set BACKEND (explicit container name) or COMPOSE_PROJECT (compose project label)" >&2
    echo "find it with: docker ps --format '{{.Label \"com.docker.compose.project\"}}'" >&2
    exit 1
  fi
  BACKEND="$(docker ps \
    --filter "label=com.docker.compose.project=$COMPOSE_PROJECT" \
    --filter "label=com.docker.compose.service=$COMPOSE_SERVICE" \
    --format '{{.Names}}' | tr '\n' ' ')"
  BACKEND="${BACKEND% }"
  if [[ -z "$BACKEND" ]]; then
    echo "error: no running container for project '$COMPOSE_PROJECT' service '$COMPOSE_SERVICE'" >&2
    exit 1
  fi
  echo "resolved backend: $BACKEND"
fi

echo "==> stopping $BACKEND"
docker stop $BACKEND > /dev/null

echo "==> dropping and recreating $DB"
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS \"$DB\" WITH (FORCE);"
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE \"$DB\" OWNER \"$DB_USER\";"

echo "==> restoring"
docker exec -i "$CONTAINER" pg_restore -U "$DB_USER" -d "$DB" --no-owner --exit-on-error < "$DUMP"

echo "==> starting $BACKEND"
docker start $BACKEND > /dev/null

echo
echo "restore ok — tail the backend until Flyway reports the schema is current:"
echo "  docker logs -f $BACKEND"
