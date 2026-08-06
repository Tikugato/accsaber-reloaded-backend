#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_USER="${SUDO_USER:-$(id -un)}"
OUT_DIR="${OUT_DIR:-$(getent passwd "$RUN_USER" | cut -d: -f6)/accsaber-backups}"
AT="${AT:-03:00:00}"
SCRIPT_PATH="${SCRIPT_PATH:-$REPO_DIR/scripts/backup-db.sh}"
COMPOSE_PROJECT="${COMPOSE_PROJECT:-}"
CONTAINER="${CONTAINER:-}"

if [[ ! -x "$SCRIPT_PATH" ]]; then
  echo "error: $SCRIPT_PATH is not executable" >&2
  echo "run: chmod +x $SCRIPT_PATH" >&2
  exit 1
fi

if [[ -z "$COMPOSE_PROJECT" && -z "$CONTAINER" ]]; then
  echo "error: set COMPOSE_PROJECT (compose project label) or CONTAINER (explicit name)" >&2
  echo "find it with: docker ps --format '{{.Label \"com.docker.compose.project\"}}'" >&2
  exit 1
fi

if [[ "$RUN_USER" != "root" ]] && ! id -nG "$RUN_USER" | tr ' ' '\n' | grep -qx docker; then
  echo "error: user '$RUN_USER' is not in the docker group; the timer would fail" >&2
  exit 1
fi

ENV_LINES="Environment=OUT_DIR=$OUT_DIR"

add_env() {
  local key="$1" value="$2"
  if [[ -n "$value" ]]; then
    ENV_LINES="$ENV_LINES"$'\n'"Environment=$key=$value"
  fi
}

add_env COMPOSE_PROJECT "$COMPOSE_PROJECT"
add_env CONTAINER "$CONTAINER"
add_env POSTGRES_DB "${POSTGRES_DB:-}"
add_env POSTGRES_USER "${POSTGRES_USER:-}"

echo "installing timer:"
echo "  user    : $RUN_USER"
echo "  script  : $SCRIPT_PATH"
echo "  output  : $OUT_DIR"
echo "  target  : ${CONTAINER:-project $COMPOSE_PROJECT}"
echo "  runs    : daily at $AT UTC"
echo

sudo tee /etc/systemd/system/accsaber-backup.service > /dev/null <<EOF
[Unit]
Description=AccSaber production database backup
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
User=$RUN_USER
$ENV_LINES
ExecStart=$SCRIPT_PATH
Nice=10
IOSchedulingClass=idle
TimeoutStartSec=3600
EOF

sudo tee /etc/systemd/system/accsaber-backup.timer > /dev/null <<EOF
[Unit]
Description=Daily AccSaber production database backup

[Timer]
OnCalendar=*-*-* $AT UTC
Persistent=true
RandomizedDelaySec=300

[Install]
WantedBy=timers.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now accsaber-backup.timer

echo
sudo systemctl list-timers accsaber-backup.timer --no-pager
echo
echo "run:"
echo "  sudo systemctl start --no-block accsaber-backup.service && journalctl -u accsaber-backup -f"
