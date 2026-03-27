#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

echo "== Homebrew Services =="
if command -v brew >/dev/null 2>&1; then
  brew services list | awk '
    NR == 1 || $1 == "postgresql@16" || $1 == "zookeeper" || $1 == "kafka" || $1 == "prometheus" || $1 == "grafana" || $2 == "started"
  '
else
  echo "brew not found"
fi

echo
echo "== Tracked Scripts View =="
bash ./scripts/brew/list-dev-services.sh || true
echo
bash ./scripts/apps/list-non-brew-apps.sh || true

echo
echo "== Common Dev Processes =="
if command -v pgrep >/dev/null 2>&1; then
  if ! pgrep -fal 'Docker|Docker Desktop|Code|Cursor|IntelliJ|Idea|PyCharm|WebStorm|GoLand|DataGrip|PhpStorm|Rider|Android Studio|Postman|TablePlus|DBeaver|OrbStack|Colima|podman|ngrok|redis-server|mongod|elasticsearch|mysql|mysqld|java|node|npm|pnpm|yarn|gradle|mvn|spring-boot|k6' 2>/dev/null; then
    echo "No matching common dev processes found, or process listing is restricted."
  fi
else
  echo "pgrep not found"
fi

echo
echo "== Listening Local Ports =="
if command -v lsof >/dev/null 2>&1; then
  if ! lsof -nP -iTCP -sTCP:LISTEN 2>/dev/null | awk 'NR==1 || /127\.0\.0\.1|localhost|\*/'; then
    echo "No listening TCP ports found, or listing is restricted."
  fi
else
  echo "lsof not found"
fi
