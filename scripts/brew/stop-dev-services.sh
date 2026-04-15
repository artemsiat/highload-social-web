#!/usr/bin/env bash
set -euo pipefail

# Stop in reverse dependency order.
SERVICES=(
  "grafana"
  "prometheus"
  "kafka"
  "zookeeper"
#  "postgresql@16"
)

if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew is not installed or not available in PATH." >&2
  exit 1
fi

echo "Stopping tracked dev services..."
for service in "${SERVICES[@]}"; do
  echo "-> $service"
  brew services stop "$service"
done

echo
echo "Tracked services status:"
brew services list | awk '
  NR == 1 || $1 == "postgresql@16" || $1 == "zookeeper" || $1 == "kafka" || $1 == "prometheus" || $1 == "grafana"
'
