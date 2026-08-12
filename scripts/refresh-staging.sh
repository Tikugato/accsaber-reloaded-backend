#!/usr/bin/env bash
set -euo pipefail

STAGING_PROJECT="${STAGING_PROJECT:-}"
STAGING_DB_CONTAINER="${STAGING_DB_CONTAINER:-}"
STAGING_DB_SERVICE="${STAGING_DB_SERVICE:-postgres}"
STAGING_BACKEND_SERVICE="${STAGING_BACKEND_SERVICE:-backend}"
DB="${POSTGRES_DB:-accsaber}"
DB_USER="${POSTGRES_USER:-accsaber}"
OUT_DIR="${OUT_DIR:-$HOME/accsaber-backups}"
DUMP="${DUMP:-}"
JOBS="${JOBS:-4}"

log() { echo "[$(date -u +%H:%M:%S)] $*"; }

resolve() {
  docker ps \
    --filter "label=com.docker.compose.project=$STAGING_PROJECT" \
    --filter "label=com.docker.compose.service=$1" \
    --format '{{.Names}}'
}

resolve_all() {
  docker ps --all \
    --filter "label=com.docker.compose.project=$STAGING_PROJECT" \
    --filter "label=com.docker.compose.service=$1" \
    --format '{{.Names}}'
}

if [[ -z "$STAGING_DB_CONTAINER" ]]; then
  if [[ -z "$STAGING_PROJECT" ]]; then
    log "error: set STAGING_DB_CONTAINER or STAGING_PROJECT"
    exit 1
  fi
  STAGING_DB_CONTAINER="$(resolve "$STAGING_DB_SERVICE")"
  if [[ -z "$STAGING_DB_CONTAINER" ]]; then
    log "error: no running container for project '$STAGING_PROJECT' service '$STAGING_DB_SERVICE'"
    exit 1
  fi
fi

PROD_DB_NAME="${PROD_DB_NAME:-accsaber-postgres}"
PROD_PROJECT="${PROD_PROJECT:-}"

RESOLVED_NAME="$(docker inspect -f '{{.Name}}' "$STAGING_DB_CONTAINER" 2>/dev/null | sed 's|^/||')"
RESOLVED_PROJECT="$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' \
  "$STAGING_DB_CONTAINER" 2>/dev/null)"

if [[ "$RESOLVED_NAME" == "$PROD_DB_NAME" ]]; then
  log "error: '$STAGING_DB_CONTAINER' is the production database container, refusing"
  exit 1
fi

if [[ -n "$PROD_PROJECT" && "$RESOLVED_PROJECT" == "$PROD_PROJECT" ]]; then
  log "error: '$STAGING_DB_CONTAINER' belongs to production project '$PROD_PROJECT', refusing"
  exit 1
fi

log "target: $RESOLVED_NAME (project ${RESOLVED_PROJECT:-none})"

if [[ -z "$DUMP" ]]; then
  DUMP="$(find "$OUT_DIR/daily" -maxdepth 1 -type f -name 'accsaber-2*.dump' -printf '%T@ %p\n' 2>/dev/null \
    | sort -rn | head -n 1 | cut -d' ' -f2-)"
fi

if [[ -z "$DUMP" || ! -f "$DUMP" ]]; then
  log "error: no dump found in $OUT_DIR/daily, and DUMP was not set"
  exit 1
fi

log "restoring $(basename "$DUMP") ($(du -h "$DUMP" | cut -f1)) into $STAGING_DB_CONTAINER"

BACKENDS="$(resolve_all "$STAGING_BACKEND_SERVICE" | tr '\n' ' ')"
BACKENDS="${BACKENDS% }"
if [[ -n "$BACKENDS" ]]; then
  log "stopping staging backend: $BACKENDS"
  docker stop $BACKENDS > /dev/null
else
  log "warning: no staging backend container found, nothing to stop or restart afterwards"
fi

log "dropping and recreating $DB"
docker exec "$STAGING_DB_CONTAINER" psql -U "$DB_USER" -d postgres \
  -c "DROP DATABASE IF EXISTS \"$DB\" WITH (FORCE);"
docker exec "$STAGING_DB_CONTAINER" psql -U "$DB_USER" -d postgres \
  -c "CREATE DATABASE \"$DB\" OWNER \"$DB_USER\";"

REMOTE_DUMP="/tmp/accsaber-staging-restore.dump"
cleanup_remote() {
  docker exec "$STAGING_DB_CONTAINER" rm -f "$REMOTE_DUMP" 2>/dev/null || true
}
trap cleanup_remote EXIT

log "copying dump into $STAGING_DB_CONTAINER"
docker cp "$DUMP" "$STAGING_DB_CONTAINER:$REMOTE_DUMP"

log "restoring with $JOBS workers"
docker exec "$STAGING_DB_CONTAINER" pg_restore \
  -U "$DB_USER" -d "$DB" --no-owner --jobs "$JOBS" --exit-on-error "$REMOTE_DUMP"

if [[ -n "$BACKENDS" ]]; then
  log "starting staging backend"
  docker start $BACKENDS > /dev/null
fi

log "staging refreshed from $(basename "$DUMP")"
log "the staging app will now run any migrations production has not seen yet"
