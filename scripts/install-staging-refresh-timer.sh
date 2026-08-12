#!/usr/bin/env bash
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_USER="${SUDO_USER:-$(id -un)}"
OUT_DIR="${OUT_DIR:-$(getent passwd "$RUN_USER" | cut -d: -f6)/accsaber-backups}"
AT="${AT:-05:00:00}"
SCRIPT_PATH="${SCRIPT_PATH:-$HERE/refresh-staging.sh}"
STAGING_PROJECT="${STAGING_PROJECT:-}"
STAGING_DB_CONTAINER="${STAGING_DB_CONTAINER:-}"
JOBS="${JOBS:-4}"

if [[ ! -x "$SCRIPT_PATH" ]]; then
  echo "error: $SCRIPT_PATH is not executable" >&2
  echo "run: chmod +x $SCRIPT_PATH" >&2
  exit 1
fi

if [[ -z "$STAGING_PROJECT" && -z "$STAGING_DB_CONTAINER" ]]; then
  echo "error: set STAGING_PROJECT (compose project label) or STAGING_DB_CONTAINER (explicit name)" >&2
  echo "coolify uses the resource uuid as the project name, visible in the deploy log" >&2
  echo "find it with: docker ps --format '{{.Names}}  {{.Label \"com.docker.compose.project\"}}'" >&2
  exit 1
fi

if [[ "$RUN_USER" != "root" ]] && ! id -nG "$RUN_USER" | tr ' ' '\n' | grep -qx docker; then
  echo "error: user '$RUN_USER' is not in the docker group; the timer would fail" >&2
  exit 1
fi

ENV_LINES="Environment=OUT_DIR=$OUT_DIR"$'\n'"Environment=JOBS=$JOBS"

add_env() {
  local key="$1" value="$2"
  if [[ -n "$value" ]]; then
    ENV_LINES="$ENV_LINES"$'\n'"Environment=$key=$value"
  fi
}

add_env STAGING_PROJECT "$STAGING_PROJECT"
add_env STAGING_DB_CONTAINER "$STAGING_DB_CONTAINER"
add_env PROD_PROJECT "${PROD_PROJECT:-}"
add_env POSTGRES_DB "${POSTGRES_DB:-}"
add_env POSTGRES_USER "${POSTGRES_USER:-}"

echo "installing timer:"
echo "  user    : $RUN_USER"
echo "  script  : $SCRIPT_PATH"
echo "  dumps   : $OUT_DIR/daily"
echo "  target  : ${STAGING_DB_CONTAINER:-project $STAGING_PROJECT}"
echo "  runs    : daily at $AT UTC"
echo

sudo tee /etc/systemd/system/accsaber-staging-refresh.service > /dev/null <<EOF
[Unit]
Description=Refresh the AccSaber staging database from the latest production dump
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
User=$RUN_USER
$ENV_LINES
ExecStart=$SCRIPT_PATH
Nice=10
IOSchedulingClass=idle
TimeoutStartSec=7200

[Install]
WantedBy=multi-user.target
EOF

sudo tee /etc/systemd/system/accsaber-staging-refresh.timer > /dev/null <<EOF
[Unit]
Description=Daily AccSaber staging database refresh

[Timer]
OnCalendar=*-*-* $AT UTC
Persistent=true
RandomizedDelaySec=300

[Install]
WantedBy=timers.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now accsaber-staging-refresh.timer

echo
sudo systemctl list-timers accsaber-staging-refresh.timer --no-pager
echo
echo "run now:"
echo "  sudo systemctl start --no-block accsaber-staging-refresh.service && journalctl -u accsaber-staging-refresh -f"
