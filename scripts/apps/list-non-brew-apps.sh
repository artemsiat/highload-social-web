#!/usr/bin/env bash
set -euo pipefail

printf "%-18s %-10s %-s\n" "App" "Running" "Notes"
printf "%-18s %-10s %-s\n" "---" "-------" "-----"

if pgrep -f "/Applications/Docker.app" >/dev/null 2>&1 || pgrep -f "Docker Desktop" >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    printf "%-18s %-10s %-s\n" "Docker" "yes" "Desktop app is running and engine responds"
  else
    printf "%-18s %-10s %-s\n" "Docker" "yes" "Desktop app is running, engine is still starting"
  fi
else
  printf "%-18s %-10s %-s\n" "Docker" "no" "Desktop app is not running"
fi
