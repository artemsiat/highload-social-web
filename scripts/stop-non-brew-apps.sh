#!/usr/bin/env bash
set -euo pipefail

# Stop tracked desktop apps gracefully.
APPS=(
  "Docker"
)

for app in "${APPS[@]}"; do
  echo "Stopping $app..."
  osascript -e "quit app \"$app\"" >/dev/null
done

echo
echo "Tracked non-Homebrew apps were asked to quit."
