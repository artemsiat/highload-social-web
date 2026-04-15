#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

load_env_file() {
  if [[ -f .env ]]; then
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
  fi
}

wait_for_docker() {
  echo "Checking Docker daemon..."
  for _ in {1..60}; do
    if docker info >/dev/null 2>&1; then
      echo "Docker is ready."
      return 0
    fi
    sleep 2
  done
  echo "Docker is not ready. Start Docker Desktop and retry." >&2
  return 1
}

start_docker_desktop() {
  if command -v open >/dev/null 2>&1; then
    open -a Docker >/dev/null 2>&1 || true
  fi
}

usage() {
  cat <<'USAGE'
Usage: ./scripts/dev.sh <command>

Commands:
  up     Start Docker DB and run Spring Boot locally
  db     Start only Docker DB
  full   Run both DB and app in Docker
  down   Stop Docker services
  reset  Stop Docker services and remove DB volume
USAGE
}

cmd="${1:-}"
load_env_file
case "$cmd" in
  up)
    start_docker_desktop
    wait_for_docker
    # Spring Boot Docker Compose support auto-starts the DB container;
    # lifecycle-management=start-only keeps it alive across app restarts.
    ./mvnw spring-boot:run
    ;;
  db)
    start_docker_desktop
    wait_for_docker
    docker compose up -d
    ;;
  full)
    start_docker_desktop
    wait_for_docker
    docker compose --profile full up --build
    ;;
  down)
    docker compose down
    ;;
  reset)
    docker compose down -v
    ;;
  *)
    usage
    exit 1
    ;;
esac
