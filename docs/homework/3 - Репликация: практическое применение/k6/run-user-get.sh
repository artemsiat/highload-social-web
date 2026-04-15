#!/usr/bin/env bash
set -euo pipefail

HOMEWORK_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$HOMEWORK_DIR"

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is not installed." >&2
  echo "Install it on macOS with: brew install k6" >&2
  exit 1
fi

VUS="${1:-10}"
DURATION="${2:-120s}"
BASE_URL="${BASE_URL:-http://localhost:8075}"
USER_ID_MIN="${USER_ID_MIN:-1}"
USER_ID_MAX="${USER_ID_MAX:-999999}"
RESULTS_DIR="${RESULTS_DIR:-${HOMEWORK_DIR}/results}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
SUMMARY_FILE="${RESULTS_DIR}/user-get-${VUS}vus-${DURATION}-${TIMESTAMP}.json"

mkdir -p "$RESULTS_DIR"

cat <<INFO
Running k6 user get test with:
  BASE_URL=${BASE_URL}
  USER_ID_MIN=${USER_ID_MIN}
  USER_ID_MAX=${USER_ID_MAX}
  K6_VUS=${VUS}
  K6_DURATION=${DURATION}
  SUMMARY_FILE=${SUMMARY_FILE}
INFO

K6_VUS="$VUS" \
K6_DURATION="$DURATION" \
BASE_URL="$BASE_URL" \
USER_ID_MIN="$USER_ID_MIN" \
USER_ID_MAX="$USER_ID_MAX" \
SUMMARY_FILE="$SUMMARY_FILE" \
k6 run ./user-get.js
