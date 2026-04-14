#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
HOMEWORK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is not installed." >&2
  echo "Install it on macOS with: brew install k6" >&2
  exit 1
fi

VUS="${1:-10}"
DURATION="${2:-120s}"
FIRST_NAME="${FIRST_NAME:-}"
LAST_NAME="${LAST_NAME:-}"
BASE_URL="${BASE_URL:-http://localhost:8075}"
SEARCH_DATA_FILE="${SEARCH_DATA_FILE:-./user-search-data.json}"
RESULTS_DIR="${RESULTS_DIR:-${HOMEWORK_DIR}/results}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
SUMMARY_FILE="${RESULTS_DIR}/user-search-${VUS}vus-${DURATION}-${TIMESTAMP}.json"

mkdir -p "$RESULTS_DIR"

if [[ -n "$FIRST_NAME" || -n "$LAST_NAME" ]]; then
  if [[ -z "$FIRST_NAME" || -z "$LAST_NAME" ]]; then
    echo "Set both FIRST_NAME and LAST_NAME, or leave both empty to use the dataset file." >&2
    exit 1
  fi
fi

cat <<INFO
Running k6 search test with:
  BASE_URL=${BASE_URL}
  FIRST_NAME=${FIRST_NAME:-<dataset mode>}
  LAST_NAME=${LAST_NAME:-<dataset mode>}
  SEARCH_DATA_FILE=${SEARCH_DATA_FILE}
  K6_VUS=${VUS}
  K6_DURATION=${DURATION}
  SUMMARY_FILE=${SUMMARY_FILE}
INFO

K6_VUS="$VUS" \
K6_DURATION="$DURATION" \
BASE_URL="$BASE_URL" \
FIRST_NAME="$FIRST_NAME" \
LAST_NAME="$LAST_NAME" \
SEARCH_DATA_FILE="$SEARCH_DATA_FILE" \
SUMMARY_FILE="$SUMMARY_FILE" \
k6 run "${HOMEWORK_DIR}/user-search.js"
