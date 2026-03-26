#!/usr/bin/env bash
set -euo pipefail

if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew is not installed or not available in PATH." >&2
  exit 1
fi

echo "Tracked Homebrew services for this project:"
brew services list | awk '
  NR == 1 || $1 == "postgresql@16" || $1 == "zookeeper" || $1 == "kafka" || $1 == "prometheus" || $1 == "grafana"
'
