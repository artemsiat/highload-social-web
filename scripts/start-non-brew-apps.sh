#!/usr/bin/env bash
set -euo pipefail

# Tracked non-Homebrew desktop apps used with this project.
APPS=(
  "Docker"
)

for app in "${APPS[@]}"; do
  echo "Starting $app..."
  open -a "$app"
done

echo
echo "Tip: Docker Desktop may need some extra time before the engine is ready."
echo "Check readiness with: docker info"
